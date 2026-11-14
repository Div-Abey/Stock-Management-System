package com.stockmanagement.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Central place for application settings.
 *
 * Works with NO extra file (defaults below). To change a setting, create a file named
 * application.properties in the project root (same folder as stock_management.db), e.g.
 *
 *     db.url=jdbc:sqlite:data/stock_management.db
 *     invoice.prefix=INV
 *     bulk.discount.min.quantity=12
 *     bulk.discount.percent=3
 *     app.name=My Shop - Stock Management
 */
public final class AppConfig {

    private static final String CONFIG_FILE = "application.properties";
    private static final Properties PROPS = new Properties();

    static {
        // defaults
        PROPS.setProperty("app.name", "Stock Management System");
        PROPS.setProperty("db.url", "jdbc:sqlite:stock_management.db");
        PROPS.setProperty("invoice.prefix", "INV");

        // default bulk discount: buy 12 or more of an item -> 3% off that line
        PROPS.setProperty("bulk.discount.min.quantity", "12");
        PROPS.setProperty("bulk.discount.percent", "3");

        // optional overrides from application.properties in the working directory
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            PROPS.load(in);
            System.out.println("Loaded settings from " + CONFIG_FILE);
        } catch (IOException ignored) {
            // file not present -> defaults are used
        }
    }

    private AppConfig() {
    }

    public static String getAppName() {
        return PROPS.getProperty("app.name");
    }

    public static String getDatabaseUrl() {
        return PROPS.getProperty("db.url");
    }

    public static String getInvoicePrefix() {
        return PROPS.getProperty("invoice.prefix");
    }

    /** Smallest quantity of ONE item that earns the default bulk discount (default 12). */
    public static int getBulkDiscountMinQuantity() {
        return readInt("bulk.discount.min.quantity", 12);
    }

    /** Default bulk discount in percent (default 3). */
    public static double getBulkDiscountPercent() {
        return readDouble("bulk.discount.percent", 3.0);
    }

    private static int readInt(String key, int fallback) {
        try {
            return Integer.parseInt(PROPS.getProperty(key).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double readDouble(String key, double fallback) {
        try {
            return Double.parseDouble(PROPS.getProperty(key).trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
