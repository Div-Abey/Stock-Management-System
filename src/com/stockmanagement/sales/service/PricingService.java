package com.stockmanagement.sales.service;

import com.stockmanagement.inventory.dao.ItemDAO.CustomerSummary;
import com.stockmanagement.inventory.dao.ItemDAO.ItemStockSummary;
import com.stockmanagement.sales.dao.SaleDAO;

import java.sql.SQLException;

/** Decides which unit price applies: retail, wholesale or a customer's special price. */
public class PricingService {

    private final SaleDAO saleDAO;

    public PricingService(SaleDAO saleDAO) {
        this.saleDAO = saleDAO;
    }

    public double resolveUnitPrice(ItemStockSummary item, CustomerSummary customer) throws SQLException {
        if (customer == null || customer.getCustomerId() == null) {
            return item.getRetailPrice();                       // walk-in
        }
        String type = customer.getCustomerType() == null ? "RETAIL" : customer.getCustomerType();
        switch (type) {
            case "WHOLESALE":
                return item.getWholesalePrice();
            case "SPECIAL":
                Double special = saleDAO.findSpecialPrice(customer.getCustomerId(), item.getItemId());
                return special != null ? special : item.getRetailPrice();
            default:
                return item.getRetailPrice();
        }
    }
}
