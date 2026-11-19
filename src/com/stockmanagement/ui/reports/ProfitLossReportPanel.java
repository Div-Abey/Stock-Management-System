package com.stockmanagement.ui.reports;

import com.stockmanagement.reports.model.ReportTable;
import com.stockmanagement.reports.service.ProfitLossReportService;

import java.sql.SQLException;
import java.time.LocalDate;

public class ProfitLossReportPanel extends ReportViewPanel {

    private final ProfitLossReportService service = new ProfitLossReportService();

    public ProfitLossReportPanel() {
        super(new String[]{"Profit by Item", "Profit by Day"}, true, false);
    }

    @Override
    protected ReportTable buildReport(int index, LocalDate from, LocalDate to, int days) throws SQLException {
        return index == 1 ? service.profitByDay(from, to) : service.profitByItem(from, to);
    }
}
