package com.stockmanagement.sales.service;

import com.stockmanagement.config.AppConfig;
import com.stockmanagement.sales.dao.SaleDAO;
import com.stockmanagement.sales.model.BulkDiscount;

import java.sql.SQLException;

/**
 * Bulk (quantity) discount for one cart line.
 *
 * Rule order:
 *  1. An item-specific rule in the bulk_discounts table (if one exists for this quantity) wins.
 *  2. Otherwise the DEFAULT rule applies to every item: quantity >= 12  ->  3% off the line.
 *     (the 12 and the 3 come from AppConfig, so they can be changed without touching this class)
 */
public class DiscountService {

    private final SaleDAO saleDAO;

    public DiscountService(SaleDAO saleDAO) {
        this.saleDAO = saleDAO;
    }

    /** Smallest quantity that earns the default discount (12). */
    public static int getDefaultMinQuantity() {
        return AppConfig.getBulkDiscountMinQuantity();
    }

    /** Default discount percentage (3). */
    public static double getDefaultPercentage() {
        return AppConfig.getBulkDiscountPercent();
    }

    /** Text for the POS screen, e.g. "Buy 12 or more of an item and get 3% discount". */
    public static String describeDefaultRule() {
        double pct = getDefaultPercentage();
        String pctText = (pct == Math.floor(pct)) ? String.valueOf((int) pct) : String.valueOf(pct);
        return "Buy " + getDefaultMinQuantity() + " or more of an item and get " + pctText + "% discount";
    }

    /**
     * If the rule has a percentage, it is taken off the line gross.
     * Otherwise discount_amount is treated as Rs. off PER UNIT (amount x quantity).
     * The discount can never be more than the line gross.
     */
    public double calculateBulkDiscount(int itemId, int quantity, double lineGross) throws SQLException {
        BulkDiscount rule = saleDAO.findBestBulkDiscount(itemId, quantity);
        if (rule != null) {
            double discount = rule.getDiscountPercentage() > 0
                    ? lineGross * rule.getDiscountPercentage() / 100.0
                    : rule.getDiscountAmount() * quantity;
            return round2(Math.min(discount, lineGross));
        }

        // default rule: quantity >= 12  ->  3%
        if (quantity >= getDefaultMinQuantity()) {
            return round2(Math.min(lineGross * getDefaultPercentage() / 100.0, lineGross));
        }
        return 0.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
