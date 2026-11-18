package com.stockmanagement.reports.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A report in a neutral shape (title + columns + rows + summary lines).
 * The services build it, the Swing screens show it in a JTable, and the exporters write it to CSV / PDF.
 * Cell values are String, Integer or Double (Double = money / amounts).
 */
public class ReportTable {

    private final String title;
    private final List<String> columns = new ArrayList<>();
    private final List<Object[]> rows = new ArrayList<>();
    private final List<String> summaryLines = new ArrayList<>();

    public ReportTable(String title, String... columns) {
        this.title = title;
        for (String c : columns) {
            this.columns.add(c);
        }
    }

    public void addRow(Object... cells) {
        rows.add(cells);
    }

    public void addSummary(String line) {
        summaryLines.add(line);
    }

    public String getTitle() { return title; }
    public List<String> getColumns() { return columns; }
    public List<Object[]> getRows() { return rows; }
    public List<String> getSummaryLines() { return summaryLines; }
    public boolean isEmpty() { return rows.isEmpty(); }

    /** Sum of one numeric column (0-based). Non-numeric cells are ignored. */
    public double sumColumn(int index) {
        double sum = 0;
        for (Object[] row : rows) {
            if (index < row.length && row[index] instanceof Number) {
                sum += ((Number) row[index]).doubleValue();
            }
        }
        return sum;
    }
}
