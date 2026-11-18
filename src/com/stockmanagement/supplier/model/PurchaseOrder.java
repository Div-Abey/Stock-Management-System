package com.stockmanagement.supplier.model;

import java.util.ArrayList;
import java.util.List;

/** A purchase order sent to a supplier. */
public class PurchaseOrder {
    private final int poId;
    private final int supplierId;
    private final String supplierName;
    private final String orderDate;
    private final String sentDate;
    private final String status;
    private final double total;
    private List<PurchaseOrderItem> lines = new ArrayList<>();

    public PurchaseOrder(int poId, int supplierId, String supplierName, String orderDate, String sentDate,
                         String status, double total) {
        this.poId = poId;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.orderDate = orderDate;
        this.sentDate = sentDate;
        this.status = status;
        this.total = total;
    }

    public int getPoId() { return poId; }
    public int getSupplierId() { return supplierId; }
    public String getSupplierName() { return supplierName; }
    public String getOrderDate() { return orderDate; }
    public String getSentDate() { return sentDate == null ? "" : sentDate; }
    public String getStatus() { return status; }
    public double getTotal() { return total; }
    public List<PurchaseOrderItem> getLines() { return lines; }
    public void setLines(List<PurchaseOrderItem> lines) { this.lines = lines; }
    public String getNumber() { return String.format("PO-%05d", poId); }

    @Override
    public String toString() { return getNumber() + " - " + supplierName + " (" + status + ")"; }
}
