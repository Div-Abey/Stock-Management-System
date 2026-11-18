package com.stockmanagement.ui.reports;

import com.stockmanagement.reports.export.PDFExporter;
import com.stockmanagement.reports.model.ReportTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Shared screen for every report group: choose a report, (optionally) a date range or a number of days,
 * press Generate, see the table + summary, Export PDF (opens the Windows "Save Print Output As" dialog straight away).
 * Sub-classes only say which reports they offer and how to build each one (no SQL here - that lives in the report services).
 */
public abstract class ReportViewPanel extends JPanel {

    private final JComboBox<String> cmbReport;
    private final JSpinner spnFrom = dateSpinner();
    private final JSpinner spnTo = dateSpinner();
    private final JSpinner spnDays = new JSpinner(new SpinnerNumberModel(30, 0, 3650, 5));
    private final JButton btnGenerate = new JButton("Generate");
    private final JButton btnPdf = new JButton("Export PDF");
    private final JTable table = new JTable();
    private final JTextArea txtSummary = new JTextArea(5, 40);

    private final boolean usesDateRange;
    private final boolean usesDays;
    private ReportTable currentReport;

    protected ReportViewPanel(String[] reportNames, boolean usesDateRange, boolean usesDays) {
        this.usesDateRange = usesDateRange;
        this.usesDays = usesDays;
        this.cmbReport = new JComboBox<>(reportNames);

        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        add(buildControls(), BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(table), BorderLayout.CENTER);

        txtSummary.setEditable(false);
        txtSummary.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        JScrollPane summaryScroll = new JScrollPane(txtSummary);
        summaryScroll.setBorder(new TitledBorder("Summary"));
        add(summaryScroll, BorderLayout.SOUTH);

        btnPdf.setEnabled(false);
        btnGenerate.addActionListener(e -> generate());
        btnPdf.addActionListener(e -> exportPdf());

        setThisMonth();
    }

    /** Build the report chosen in the combo box. Runs on a background thread. */
    protected abstract ReportTable buildReport(int reportIndex, LocalDate from, LocalDate to, int days) throws SQLException;

    /** Called when the user opens this tab again, so figures are never stale. */
    public void refresh() {
        generate();
    }

    // ---------------- controls ----------------

    private JPanel buildControls() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setBorder(new TitledBorder("Report options"));

        p.add(new JLabel("Report:"));
        p.add(cmbReport);

        if (usesDateRange) {
            p.add(new JLabel("From:"));
            p.add(spnFrom);
            p.add(new JLabel("To:"));
            p.add(spnTo);
        }
        if (usesDays) {
            p.add(new JLabel("Expiring within (days):"));
            p.add(spnDays);
            cmbReport.addActionListener(e -> spnDays.setEnabled(isDaysReport(cmbReport.getSelectedIndex())));
            spnDays.setEnabled(isDaysReport(cmbReport.getSelectedIndex()));
        }

        // button order: Generate, Today, This Month, Export PDF
        p.add(btnGenerate);
        if (usesDateRange) {
            JButton today = new JButton("Today");
            today.addActionListener(e -> {
                setRange(LocalDate.now(), LocalDate.now());
                generate();
            });
            JButton month = new JButton("This Month");
            month.addActionListener(e -> {
                setThisMonth();
                generate();
            });
            p.add(today);
            p.add(month);
        }
        p.add(btnPdf);
        return p;
    }

    /** Sub-classes that use the "days" box say which report it belongs to (default: all). */
    protected boolean isDaysReport(int reportIndex) {
        return true;
    }

    private static JSpinner dateSpinner() {
        JSpinner s = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd"));
        s.setPreferredSize(new Dimension(110, 26));
        return s;
    }

    private void setThisMonth() {
        LocalDate today = LocalDate.now();
        setRange(today.withDayOfMonth(1), today);
    }

    private void setRange(LocalDate from, LocalDate to) {
        spnFrom.setValue(Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        spnTo.setValue(Date.from(to.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    private static LocalDate toLocalDate(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // ---------------- generate ----------------

    private void generate() {
        final int index = cmbReport.getSelectedIndex();
        final LocalDate from = toLocalDate(spnFrom);
        final LocalDate to = toLocalDate(spnTo);
        final int days = ((Number) spnDays.getValue()).intValue();

        if (usesDateRange && from.isAfter(to)) {
            JOptionPane.showMessageDialog(this, "The From date cannot be after the To date.", "Invalid dates", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnGenerate.setEnabled(false);

        new SwingWorker<ReportTable, Void>() {
            @Override
            protected ReportTable doInBackground() throws Exception {
                return buildReport(index, from, to, days);
            }

            @Override
            protected void done() {
                btnGenerate.setEnabled(true);
                try {
                    show(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(ReportViewPanel.this,
                            "Could not generate the report:\n" + cause.getMessage(), "Report error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void show(ReportTable report) {
        currentReport = report;

        DefaultTableModel model = new DefaultTableModel(report.getColumns().toArray(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                for (Object[] row : report.getRows()) {
                    if (c < row.length && row[c] != null) {
                        return row[c].getClass();
                    }
                }
                return String.class;
            }
        };
        for (Object[] row : report.getRows()) {
            model.addRow(row);
        }
        table.setModel(model);

        DefaultTableCellRenderer numberRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value instanceof Double ? String.format("%,.2f", (Double) value) : String.valueOf(value));
            }
        };
        numberRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c = 0; c < model.getColumnCount(); c++) {
            Class<?> type = model.getColumnClass(c);
            TableColumn col = table.getColumnModel().getColumn(c);
            if (Number.class.isAssignableFrom(type)) {
                col.setCellRenderer(numberRenderer);
            }
            col.setPreferredWidth(Math.max(90, Math.min(260, widthFor(report, c))));
        }

        StringBuilder sb = new StringBuilder();
        for (String line : report.getSummaryLines()) {
            sb.append(line).append('\n');
        }
        txtSummary.setText(sb.toString());
        txtSummary.setCaretPosition(0);

        boolean hasData = !report.isEmpty();
        btnPdf.setEnabled(hasData);
        if (!hasData) {
            txtSummary.setText("No data found for this report / period.");
        }
    }

    private int widthFor(ReportTable report, int col) {
        int longest = report.getColumns().get(col).length();
        for (Object[] row : report.getRows()) {
            if (col < row.length && row[col] != null) {
                longest = Math.max(longest, row[col] instanceof Double ? 12 : row[col].toString().length());
            }
        }
        return longest * 8 + 24;
    }

    // ---------------- export ----------------

    /** Export PDF: the native "Save Print Output As" dialog opens at once; the PDF is written where the user picks. */
    private void exportPdf() {
        if (currentReport == null || currentReport.isEmpty()) {
            return;
        }
        String base = currentReport.getTitle().replaceAll("\\(.*\\)", "").trim().replaceAll("[^A-Za-z0-9]+", "_");

        Window owner = SwingUtilities.getWindowAncestor(this);
        FileDialog dialog = owner instanceof Frame
                ? new FileDialog((Frame) owner, "Save Print Output As", FileDialog.SAVE)
                : new FileDialog((Frame) null, "Save Print Output As", FileDialog.SAVE);
        dialog.setFile(base + "_" + LocalDate.now() + ".pdf");
        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return;   // cancelled
        }
        File file = new File(dialog.getDirectory(), dialog.getFile());
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getParentFile(), file.getName() + ".pdf");
        }
        try {
            new PDFExporter().export(currentReport, file);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not save the file:\n" + ex.getMessage(), "Export error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
