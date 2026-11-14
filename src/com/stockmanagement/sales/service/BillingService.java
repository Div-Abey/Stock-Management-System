package com.stockmanagement.sales.service;

import com.stockmanagement.inventory.dao.ItemDAO.CustomerSummary;
import com.stockmanagement.inventory.dao.ItemDAO.ItemStockSummary;
import com.stockmanagement.sales.dao.SaleDAO;
import com.stockmanagement.sales.model.Sale;
import com.stockmanagement.sales.model.SaleItem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Bill maths (prices, discounts, totals) and payment-rule validation. No SQL here. */
public class BillingService {

    private final PricingService pricingService;
    private final DiscountService discountService;

    public BillingService(SaleDAO saleDAO) {
        this.pricingService = new PricingService(saleDAO);
        this.discountService = new DiscountService(saleDAO);
    }

    /** Sets unit price (retail / wholesale / special) and bulk discount on one cart line. */
    public void applyPricingAndDiscount(SaleItem line, ItemStockSummary stockItem, CustomerSummary customer) throws SQLException {
        line.setUnitPrice(pricingService.resolveUnitPrice(stockItem, customer));
        double discount = discountService.calculateBulkDiscount(line.getItemId(), line.getQuantity(), line.getGrossAmount());
        line.setDiscountApplied(discount);   // also refreshes lineTotal
    }

    public double calculateCartGrossTotal(List<SaleItem> items) {
        double sum = 0;
        for (SaleItem i : items) {
            sum += i.getGrossAmount();
        }
        return round2(sum);
    }

    public double calculateCartDiscountTotal(List<SaleItem> items) {
        double sum = 0;
        for (SaleItem i : items) {
            sum += i.getDiscountApplied();
        }
        return round2(sum);
    }

    public double calculateCartNetPayable(List<SaleItem> items) {
        return round2(calculateCartGrossTotal(items) - calculateCartDiscountTotal(items));
    }

    public void recalculateSaleTotals(Sale sale) {
        sale.setTotalAmount(calculateCartGrossTotal(sale.getItems()));
        sale.setDiscountAmount(calculateCartDiscountTotal(sale.getItems()));
        sale.setNetAmount(calculateCartNetPayable(sale.getItems()));
    }

    /** Positive = change to give back, negative = still to pay. */
    public double calculateChangeBalance(double netAmount, double amountPaid) {
        return round2(amountPaid - netAmount);
    }

    /**
     * Payment rules (messages are shown to the cashier by BillingFrame):
     *  CASH / CARD : must cover the full net amount
     *  CREDIT      : needs a registered customer (credit limit is checked later, in the transaction)
     *  CHEQUE      : needs cheque number, bank and a valid realization date
     */
    public void validatePayment(Sale sale) throws SQLException {
        String type = sale.getPaymentType() == null ? "" : sale.getPaymentType().toUpperCase();
        double net = sale.getNetAmount();
        double paid = sale.getAmountPaid();

        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            throw new SQLException("The cart is empty.");
        }
        if (paid < 0) {
            throw new SQLException("Amount paid cannot be negative.");
        }

        switch (type) {
            case "CASH":
            case "CARD":
                if (paid + 0.005 < net) {
                    throw new SQLException(String.format("%s payment must cover the full amount (Rs. %.2f).", type, net));
                }
                break;
            case "CREDIT":
                if (sale.getCustomerId() == null) {
                    throw new SQLException("Credit sales need a registered customer. Select one in the POS screen.");
                }
                break;
            case "CHEQUE":
                if (isBlank(sale.getChequeNumber()) || isBlank(sale.getBankName())) {
                    throw new SQLException("Enter the cheque number and bank name.");
                }
                try {
                    LocalDate.parse(sale.getChequeRealizationDate());
                } catch (Exception e) {
                    throw new SQLException("Enter a valid cheque realization date (YYYY-MM-DD).");
                }
                if (paid <= 0) {
                    throw new SQLException("Enter the cheque amount in 'Amount Paid'.");
                }
                break;
            default:
                throw new SQLException("Unknown payment type: " + sale.getPaymentType());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
