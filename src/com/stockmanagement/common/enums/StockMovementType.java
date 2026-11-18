package com.stockmanagement.common.enums;

/** Why the stock of an item changed. Stored as text in stock_movements.movement_type. */
public enum StockMovementType {
    STOCK_IN("Stock In (GRN)"),
    SALE_OUT("Sale"),
    ADJUSTMENT("Adjustment"),
    EXPIRED_WRITE_OFF("Expired Write-off"),
    RETURN_IN("Return");

    private final String label;

    StockMovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static StockMovementType fromDb(String value) {
        try {
            return StockMovementType.valueOf(value);
        } catch (Exception e) {
            return ADJUSTMENT;
        }
    }
}
