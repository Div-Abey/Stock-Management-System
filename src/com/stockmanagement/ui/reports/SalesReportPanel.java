package com.stockmanagement.ui.reports;

import com.stockmanagement.reports.model.ReportTable;
import com.stockmanagement.reports.service.SalesReportService;

import java.sql.SQLException;
import java.time.LocalDate;

public class SalesReportPanel extends ReportViewPanel {

    private final SalesReportService service = new SalesReportService();

    public SalesReportPanel() {
        super(new String[]{"Daily Sales", "Invoice List", "Sales by Item", "Sales by Payment Type"}, true, false);
    }

    @Override
    protected ReportTable buildReport(int index, LocalDate from, LocalDate to, int days) throws SQLException {
        switch (index) {
            case 1: return service.invoiceList(from, to);
            case 2: return service.salesByItem(from, to);
            case 3: return service.salesByPaymentType(from, to);
            default: return service.dailySales(from, to);
        }
    }
}
