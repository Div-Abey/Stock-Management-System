package com.stockmanagement.sales.model;

/** A saved invoice row (as read back from the invoices table) for the history screen. */
public class Invoice {

    private int invoiceId;
    private String invoiceNumber;
    private String customerName;
    private String invoiceDate;      // "yyyy-MM-dd HH:mm:ss" as stored
    private String paymentType;
    private double totalAmount;
    private double discountAmount;
    private double netAmount;
    private double amountPaid;
    private String dueDate;
    private String status;

    /** "yyyy-MM-dd HH:mm" for screens and receipts */
    public String getFormattedInvoiceDate() {
        if (invoiceDate == null) {
            return "";
        }
        return invoiceDate.length() > 16 ? invoiceDate.substring(0, 16) : invoiceDate;
    }

    public double getBalanceAmount() {
        return Math.max(0, Math.round((netAmount - amountPaid) * 100.0) / 100.0);
    }

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String invoiceDate) { this.invoiceDate = invoiceDate; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    public double getNetAmount() { return netAmount; }
    public void setNetAmount(double netAmount) { this.netAmount = netAmount; }
    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
