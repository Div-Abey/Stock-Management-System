package com.stockmanagement.customer.model;

/** A payment (settlement) received from a customer. */
public class CustomerPayment {
    private final int paymentId;
    private final String paymentDate;
    private final double amountPaid;
    private final String paymentMethod;
    private final String invoiceNumber;

    public CustomerPayment(int paymentId, String paymentDate, double amountPaid, String paymentMethod, String invoiceNumber) {
        this.paymentId = paymentId;
        this.paymentDate = paymentDate;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.invoiceNumber = invoiceNumber == null ? "" : invoiceNumber;
    }

    public int getPaymentId() { return paymentId; }
    public String getPaymentDate() { return paymentDate; }
    public double getAmountPaid() { return amountPaid; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getInvoiceNumber() { return invoiceNumber; }
}
