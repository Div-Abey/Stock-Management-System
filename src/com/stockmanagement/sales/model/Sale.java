package com.stockmanagement.sales.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** A sale in progress (draft from the POS) and, after checkout, the completed sale. */
public class Sale {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private int invoiceId;
    private String invoiceNumber;
    private LocalDateTime invoiceDate;

    private Integer customerId;          // null = walk-in customer
    private String customerName;
    private int cashierId;

    private List<SaleItem> items = new ArrayList<>();

    private String paymentType;          // CASH, CARD, CREDIT, CHEQUE
    private String status;               // PAID, UNPAID, PARTIAL, CANCELLED

    private double totalAmount;          // gross, before discount
    private double discountAmount;
    private double netAmount;
    private double amountPaid;           // amount actually applied to this invoice
    private double balanceAmount;        // still owed (credit)
    private LocalDate dueDate;

    // received cheque details (only when paymentType = CHEQUE)
    private String chequeNumber;
    private String bankName;
    private String chequeIssueDate;
    private String chequeRealizationDate;

    public String getFormattedInvoiceDate() {
        return invoiceDate != null ? invoiceDate.format(DISPLAY_FORMAT) : "";
    }

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public LocalDateTime getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDateTime invoiceDate) { this.invoiceDate = invoiceDate; }
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public int getCashierId() { return cashierId; }
    public void setCashierId(int cashierId) { this.cashierId = cashierId; }
    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    public double getNetAmount() { return netAmount; }
    public void setNetAmount(double netAmount) { this.netAmount = netAmount; }
    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
    public double getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(double balanceAmount) { this.balanceAmount = balanceAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getChequeNumber() { return chequeNumber; }
    public void setChequeNumber(String chequeNumber) { this.chequeNumber = chequeNumber; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getChequeIssueDate() { return chequeIssueDate; }
    public void setChequeIssueDate(String chequeIssueDate) { this.chequeIssueDate = chequeIssueDate; }
    public String getChequeRealizationDate() { return chequeRealizationDate; }
    public void setChequeRealizationDate(String chequeRealizationDate) { this.chequeRealizationDate = chequeRealizationDate; }
}
