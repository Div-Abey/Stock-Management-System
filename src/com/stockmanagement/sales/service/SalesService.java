package com.stockmanagement.sales.service;

import com.stockmanagement.common.enums.PaymentStatus;
import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.inventory.dao.ItemDAO;
import com.stockmanagement.inventory.dao.ItemDAO.CustomerSummary;
import com.stockmanagement.inventory.dao.ItemDAO.ItemStockSummary;
import com.stockmanagement.inventory.service.FIFOService;
import com.stockmanagement.inventory.service.FIFOService.Allocation;
import com.stockmanagement.sales.dao.InvoiceDAO;
import com.stockmanagement.sales.dao.PaymentDAO;
import com.stockmanagement.sales.dao.SaleDAO;
import com.stockmanagement.sales.model.Invoice;
import com.stockmanagement.sales.model.Payment;
import com.stockmanagement.sales.model.Sale;
import com.stockmanagement.sales.model.SaleItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point of the sales module used by POSFrame / BillingFrame / InvoiceFrame.
 * completeSale() is ONE database transaction: if anything fails, stock, invoice,
 * payment and customer balance are all rolled back together.
 */
public class SalesService {

    private static final DateTimeFormatter DB_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ItemDAO itemDAO = new ItemDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    private final BillingService billingService = new BillingService(saleDAO);
    private final CreditSalesService creditSalesService = new CreditSalesService(paymentDAO);
    private final FIFOService fifoService = new FIFOService();

    public BillingService getBillingService() {
        return billingService;
    }

    // ---------------- data for the POS screen ----------------

    public List<CustomerSummary> getCustomers() throws SQLException {
        return itemDAO.findAllCustomerSummaries();
    }

    public List<ItemStockSummary> getAvailableItems() throws SQLException {
        return itemDAO.findAvailableStockItems();
    }

    public ItemStockSummary findItemByCodeOrBarcode(String code) throws SQLException {
        return itemDAO.findStockByCodeOrBarcode(code);
    }

    // ---------------- invoice history ----------------

    public List<Invoice> getAllInvoices() throws SQLException {
        return invoiceDAO.findAll();
    }

    public List<SaleItem> getInvoiceItems(int invoiceId) throws SQLException {
        return invoiceDAO.findItems(invoiceId);
    }

    // ---------------- checkout ----------------

    public Sale completeSale(Sale sale) throws SQLException {
        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            throw new SQLException("The cart is empty.");
        }

        List<SaleItem> originalCartLines = sale.getItems();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. FIFO: take stock batch by batch; a cart line may become several invoice lines
                List<SaleItem> invoiceLines = new ArrayList<>();
                for (SaleItem cartLine : sale.getItems()) {
                    invoiceLines.addAll(splitLineByBatch(conn, cartLine));
                }
                sale.setItems(invoiceLines);
                billingService.recalculateSaleTotals(sale);

                // 2. payment rules, amounts and status
                billingService.validatePayment(sale);
                String paymentType = sale.getPaymentType().toUpperCase();
                sale.setPaymentType(paymentType);

                double net = sale.getNetAmount();
                double applied = BillingService.round2(Math.min(Math.max(sale.getAmountPaid(), 0), net));
                double due = BillingService.round2(net - applied);

                sale.setAmountPaid(applied);
                sale.setBalanceAmount(due);
                sale.setStatus((due <= 0 ? PaymentStatus.PAID : applied > 0 ? PaymentStatus.PARTIAL : PaymentStatus.UNPAID).name());
                if (due <= 0) {
                    sale.setDueDate(null);
                }

                if (due > 0) {
                    creditSalesService.assertCreditAllowed(conn, sale.getCustomerId(), due);
                }

                // 3. save invoice + lines
                LocalDateTime now = LocalDateTime.now();
                sale.setInvoiceDate(now);
                sale.setInvoiceNumber(saleDAO.generateInvoiceNumber(conn));
                int invoiceId = saleDAO.insertInvoice(conn, sale, now.format(DB_DATE_TIME));
                sale.setInvoiceId(invoiceId);
                for (SaleItem line : sale.getItems()) {
                    saleDAO.insertInvoiceItem(conn, invoiceId, line);
                }

                // 4. payment record (needs a customer: customer_payments.customer_id is NOT NULL) and cheque
                if (sale.getCustomerId() != null && applied > 0) {
                    paymentDAO.insertCustomerPayment(conn,
                            new Payment(sale.getCustomerId(), invoiceId, applied, toPaymentMethod(paymentType)),
                            now.format(DB_DATE_TIME));
                }
                if ("CHEQUE".equals(paymentType)) {
                    paymentDAO.insertReceivedCheque(conn, sale.getChequeNumber(), sale.getBankName(), applied,
                            sale.getChequeIssueDate(), sale.getChequeRealizationDate(), sale.getCustomerId());
                }

                // 5. customer receivable
                if (due > 0) {
                    creditSalesService.addToCustomerBalance(conn, sale.getCustomerId(), due);
                }

                conn.commit();
                return sale;

            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                sale.setItems(originalCartLines);   // keep the cart intact so the cashier can fix the problem and retry
                throw ex;
            }
        }
    }

    /** Splits one cart line across batches (FIFO) and shares the line discount in proportion to quantity. */
    private List<SaleItem> splitLineByBatch(Connection conn, SaleItem cartLine) throws SQLException {
        List<Allocation> allocations = fifoService.allocateAndDeduct(
                conn, cartLine.getItemId(), cartLine.getItemName(), cartLine.getQuantity());

        List<SaleItem> lines = new ArrayList<>();
        double discountLeft = cartLine.getDiscountApplied();

        for (int i = 0; i < allocations.size(); i++) {
            Allocation a = allocations.get(i);
            boolean last = (i == allocations.size() - 1);
            double share = last ? discountLeft
                    : BillingService.round2(cartLine.getDiscountApplied() * a.getQuantity() / cartLine.getQuantity());
            discountLeft = BillingService.round2(discountLeft - share);

            SaleItem line = new SaleItem(cartLine.getItemId(), cartLine.getItemCode(), cartLine.getItemName(),
                    cartLine.getUnitId(), a.getQuantity(), cartLine.getUnitPrice());
            line.setUnitName(cartLine.getUnitName());
            line.setBatchId(a.getBatchId());
            line.setBatchNumber(a.getBatchNumber());
            line.setDiscountApplied(share);
            lines.add(line);
        }
        return lines;
    }

    /** customer_payments only allows CASH, CHEQUE, BANK_TRANSFER, so a card payment is stored as BANK_TRANSFER. */
    private String toPaymentMethod(String paymentType) {
        switch (paymentType) {
            case "CHEQUE":
                return "CHEQUE";
            case "CARD":
                return "BANK_TRANSFER";
            default:
                return "CASH";   // CASH, and the part paid up-front on a CREDIT sale
        }
    }
}
