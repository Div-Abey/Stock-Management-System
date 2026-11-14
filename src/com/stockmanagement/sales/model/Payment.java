package com.stockmanagement.sales.model;

/** A payment received from a customer (row of customer_payments). */
public class Payment {

    private Integer customerId;
    private Integer invoiceId;
    private double amountPaid;
    private String paymentMethod;   // CASH, CHEQUE, BANK_TRANSFER

    public Payment(Integer customerId, Integer invoiceId, double amountPaid, String paymentMethod) {
        this.customerId = customerId;
        this.invoiceId = invoiceId;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
    }

    public Integer getCustomerId() { return customerId; }
    public Integer getInvoiceId() { return invoiceId; }
    public double getAmountPaid() { return amountPaid; }
    public String getPaymentMethod() { return paymentMethod; }
}
