package com.stockmanagement.ui.reports;

import com.stockmanagement.reports.model.ReportTable;
import com.stockmanagement.reports.service.StockReportService;

import java.sql.SQLException;
import java.time.LocalDate;

public class StockReportPanel extends ReportViewPanel {

    private static final int EXPIRY = 2;
    private final StockReportService service = new StockReportService();

    public StockReportPanel() {
        super(new String[]{"Current Stock", "Low Stock", "Expiry (expired / expiring soon)", "Batch-wise Stock"}, false, true);
    }

    @Override
    protected boolean isDaysReport(int reportIndex) {
        return reportIndex == EXPIRY;
    }

    @Override
    protected ReportTable buildReport(int index, LocalDate from, LocalDate to, int days) throws SQLException {
        switch (index) {
            case 1: return service.lowStock();
            case EXPIRY: return service.expiry(days);
            case 3: return service.batchStock();
            default: return service.currentStock();
        }
    }
}
