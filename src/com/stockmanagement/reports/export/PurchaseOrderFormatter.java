package com.stockmanagement.reports.export;

import com.stockmanagement.common.util.CurrencyUtil;
import com.stockmanagement.supplier.model.PurchaseOrder;
import com.stockmanagement.supplier.model.PurchaseOrderItem;

import java.util.ArrayList;
import java.util.List;

/** Printable text of a purchase order (PDF export and the body of the supplier e-mail). */
public final class PurchaseOrderFormatter {
    private PurchaseOrderFormatter() { }

    public static List<String> toLines(PurchaseOrder po) {
        List<String> out = new ArrayList<>();
        out.add("PURCHASE ORDER " + po.getNumber());
        out.add("=".repeat(86));
        out.add("Supplier   : " + po.getSupplierName());
        out.add("Order date : " + po.getOrderDate());
        out.add("Status     : " + po.getStatus());
        out.add("");
        out.add(String.format("%-12s %-30s %-8s %8s %12s %12s", "Code", "Item", "Unit", "Qty", "Unit price", "Line total"));
        out.add("-".repeat(86));
        for (PurchaseOrderItem l : po.getLines()) {
            String name = l.getItemName().length() > 30 ? l.getItemName().substring(0, 29) + "." : l.getItemName();
            out.add(String.format("%-12s %-30s %-8s %8d %12s %12s", l.getItemCode(), name, l.getUnitName(),
                    l.getQuantity(), CurrencyUtil.format(l.getUnitPrice()), CurrencyUtil.format(l.getLineTotal())));
        }
        out.add("-".repeat(86));
        out.add(String.format("%-74s %12s", "TOTAL (Rs.)", CurrencyUtil.format(po.getTotal())));
        out.add("");
        out.add("Please confirm receipt of this order and the expected delivery date.");
        return out;
    }

    public static String toText(PurchaseOrder po) {
        return String.join("\n", toLines(po));
    }
}
