package com.stockmanagement.reports.export;

import com.stockmanagement.config.AppConfig;
import com.stockmanagement.reports.model.ReportTable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a ReportTable to a simple A4-landscape PDF (title, table with repeating header, summary, page numbers).
 * Pure Java - no extra library needed (uses the built-in Helvetica font, so only Latin characters print; others show as '?').
 * Also keeps the static exportText(...) used by the customer statement and purchase order screens.
 */
public class PDFExporter {

    // ---- plain-text mode (customer statement, purchase order): A4 portrait, Courier ----
    private static final int TEXT_PAGE_WIDTH = 595;
    private static final int TEXT_PAGE_HEIGHT = 842;
    private static final int TEXT_MARGIN = 40;
    private static final int TEXT_FONT_SIZE = 8;
    private static final int TEXT_LEADING = 11;

    // ---- table mode (reports): A4 landscape, Helvetica ----
    private static final float PAGE_W = 842f;
    private static final float PAGE_H = 595f;
    private static final float MARGIN = 30f;
    private static final float MAX_FONT = 8f;
    private static final float MIN_FONT = 5.5f;
    private static final float ROW_H = 13f;

    public void export(ReportTable table, File file) throws IOException {
        int cols = table.getColumns().size();
        float[] em = columnEm(table);
        float size = chooseFontSize(em);
        float[] widths = columnWidths(em, size);
        boolean[] numeric = numericColumns(table);

        List<StringBuilder> pages = new ArrayList<>();
        StringBuilder page = null;
        float y = 0;

        String generated = "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        List<Object[]> rows = table.getRows();

        int rowIndex = 0;
        boolean first = true;
        while (first || rowIndex < rows.size()) {
            page = new StringBuilder();
            pages.add(page);
            y = PAGE_H - MARGIN;

            if (pages.size() == 1) {
                text(page, AppConfig.getAppName(), MARGIN, y - 10, 9, false);
                text(page, table.getTitle(), MARGIN, y - 28, 14, true);
                text(page, generated, MARGIN, y - 42, 8, false);
                y -= 58;
            } else {
                text(page, table.getTitle(), MARGIN, y - 8, 9, true);
                y -= 22;
            }
            y = header(page, table, widths, y, size);

            while (rowIndex < rows.size() && y - ROW_H > MARGIN + 30) {
                y = row(page, rows.get(rowIndex), widths, numeric, y, rowIndex % 2 == 1, cols, size);
                rowIndex++;
            }
            first = false;
        }

        // summary after the last row (new page when there is no room)
        List<String> summary = table.getSummaryLines();
        if (!summary.isEmpty()) {
            float needed = 14f * summary.size() + 14;
            if (y - needed < MARGIN + 20) {
                page = new StringBuilder();
                pages.add(page);
                y = PAGE_H - MARGIN - 10;
            }
            y -= 10;
            for (String line : summary) {
                text(page, line, MARGIN, y, 9, line.startsWith("TOTAL") || line.startsWith("GROSS"));
                y -= 14;
            }
        }

        if (table.getRows().isEmpty()) {
            text(pages.get(0), "No data found for the selected report / period.", MARGIN, PAGE_H - 140, 10, false);
        }

        for (int i = 0; i < pages.size(); i++) {
            text(pages.get(i), "Page " + (i + 1) + " of " + pages.size(), PAGE_W - MARGIN - 60, MARGIN - 14, 8, false);
        }
        write(pages, file);
    }

    // ---------- plain text (existing behaviour, unchanged) ----------

    public static void exportText(List<String> lines, Path target) throws IOException {
        int linesPerPage = (TEXT_PAGE_HEIGHT - 2 * TEXT_MARGIN) / TEXT_LEADING;
        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += linesPerPage) {
            pages.add(lines.subList(i, Math.min(lines.size(), i + linesPerPage)));
        }
        if (pages.isEmpty()) {
            pages.add(new ArrayList<>());
        }

        // object numbers: 1 catalog, 2 page tree, 3 font, then (page, content) pairs
        int objectCount = 3 + pages.size() * 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int[] offsets = new int[objectCount + 1];
        writeLatin1(out, "%PDF-1.4\n");

        offsets[1] = out.size();
        writeLatin1(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder kids = new StringBuilder();
        for (int p = 0; p < pages.size(); p++) {
            kids.append(4 + p * 2).append(" 0 R ");
        }
        offsets[2] = out.size();
        writeLatin1(out, "2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>\nendobj\n");

        offsets[3] = out.size();
        writeLatin1(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>\nendobj\n");

        for (int p = 0; p < pages.size(); p++) {
            int pageObj = 4 + p * 2;
            int contentObj = pageObj + 1;

            StringBuilder content = new StringBuilder();
            content.append("BT\n/F1 ").append(TEXT_FONT_SIZE).append(" Tf\n").append(TEXT_LEADING).append(" TL\n");
            content.append(TEXT_MARGIN).append(' ').append(TEXT_PAGE_HEIGHT - TEXT_MARGIN).append(" Td\n");
            for (String line : pages.get(p)) {
                content.append('(').append(escapeLatin1(line)).append(") Tj T*\n");
            }
            content.append("ET\n");
            byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);

            offsets[pageObj] = out.size();
            writeLatin1(out, pageObj + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + TEXT_PAGE_WIDTH + " " + TEXT_PAGE_HEIGHT
                    + "] /Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObj + " 0 R >>\nendobj\n");
            offsets[contentObj] = out.size();
            writeLatin1(out, contentObj + " 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
            out.write(stream);
            writeLatin1(out, "\nendstream\nendobj\n");
        }

        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (objectCount + 1) + "\n0000000000 65535 f \n");
        for (int i = 1; i <= objectCount; i++) {
            x.append(String.format("%010d 00000 n \n", offsets[i]));
        }
        x.append("trailer\n<< /Size ").append(objectCount + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        writeLatin1(out, x.toString());

        Files.write(target, out.toByteArray());
    }

    private static void writeLatin1(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String escapeLatin1(String line) {
        StringBuilder sb = new StringBuilder();
        for (char ch : line.toCharArray()) {
            if (ch == '\\' || ch == '(' || ch == ')') {
                sb.append('\\').append(ch);
            } else if (ch < 32 || ch > 255) {
                sb.append('?');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    // ---------- table drawing ----------

    private float header(StringBuilder p, ReportTable t, float[] widths, float y, float size) {
        p.append("0.85 0.88 0.93 rg ").append(MARGIN).append(' ').append(y - ROW_H + 3).append(' ')
                .append(PAGE_W - 2 * MARGIN).append(' ').append(ROW_H).append(" re f 0 g\n");
        float x = MARGIN;
        for (int c = 0; c < widths.length; c++) {
            text(p, fit(t.getColumns().get(c), widths[c], size, true), x + 2, y - ROW_H + 6.5f, size, true);
            x += widths[c];
        }
        return y - ROW_H;
    }

    private float row(StringBuilder p, Object[] cells, float[] widths, boolean[] numeric, float y, boolean shade, int cols, float size) {
        if (shade) {
            p.append("0.96 0.96 0.96 rg ").append(MARGIN).append(' ').append(y - ROW_H + 3).append(' ')
                    .append(PAGE_W - 2 * MARGIN).append(' ').append(ROW_H).append(" re f 0 g\n");
        }
        float x = MARGIN;
        for (int c = 0; c < cols && c < cells.length; c++) {
            String s = format(cells[c]);
            s = fit(s, widths[c], size, false);
            float tx = x + 2;
            if (numeric[c]) {
                tx = x + widths[c] - 3 - textEm(s, false) * size;
            }
            text(p, s, tx, y - ROW_H + 6.5f, size, false);
            x += widths[c];
        }
        return y - ROW_H;
    }

    private String format(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof Double) {
            return String.format("%,.2f", (Double) o);
        }
        return o.toString();
    }

    /** Widest text of every column, in em (1 em = the font size). */
    private float[] columnEm(ReportTable t) {
        int cols = t.getColumns().size();
        float[] em = new float[cols];
        for (int c = 0; c < cols; c++) {
            float widest = textEm(t.getColumns().get(c), true);
            for (Object[] row : t.getRows()) {
                if (c < row.length) {
                    widest = Math.max(widest, textEm(format(row[c]), false));
                }
            }
            em[c] = Math.min(widest, 30f);
        }
        return em;
    }

    /** Biggest font (up to MAX_FONT) at which the whole table fits the page width; never below MIN_FONT. */
    private float chooseFontSize(float[] em) {
        float totalEm = 0;
        for (float e : em) {
            totalEm += e;
        }
        float avail = PAGE_W - 2 * MARGIN - 8 * em.length;
        return Math.max(MIN_FONT, Math.min(MAX_FONT, avail / totalEm));
    }

    private float[] columnWidths(float[] em, float size) {
        float[] w = new float[em.length];
        float total = 0;
        for (int c = 0; c < em.length; c++) {
            w[c] = em[c] * size + 8;
            total += w[c];
        }
        float scale = (PAGE_W - 2 * MARGIN) / total;   // stretch narrow tables so they fill the page; squeeze at MIN_FONT
        for (int c = 0; c < w.length; c++) {
            w[c] *= scale;
        }
        return w;
    }

    private boolean[] numericColumns(ReportTable t) {
        boolean[] numeric = new boolean[t.getColumns().size()];
        for (int c = 0; c < numeric.length; c++) {
            boolean any = false;
            boolean all = true;
            for (Object[] row : t.getRows()) {
                if (c < row.length && row[c] != null) {
                    any = true;
                    all &= row[c] instanceof Number;
                }
            }
            numeric[c] = any && all;
        }
        return numeric;
    }

    private String fit(String s, float width, float size, boolean bold) {
        float room = width - 5;
        if (textEm(s, bold) * size <= room) {
            return s;
        }
        String cut = s;
        while (cut.length() > 1 && textEm(cut + "..", bold) * size > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "..";
    }

    /** Approximate Helvetica width of a string in em (digits .556, capitals ~.68, narrow letters ~.28, others ~.52). */
    private float textEm(String s, boolean bold) {
        float w = 0;
        for (char ch : s.toCharArray()) {
            if (ch >= '0' && ch <= '9') {
                w += 0.556f;
            } else if (ch == ' ' || ch == '.' || ch == ',' || ch == ':' || ch == ';' || ch == '\'' || ch == '|') {
                w += 0.28f;
            } else if (ch == 'i' || ch == 'l' || ch == 'j' || ch == 't' || ch == 'f' || ch == 'r' || ch == 'I') {
                w += 0.30f;
            } else if (ch == 'm' || ch == 'w' || ch == 'M' || ch == 'W') {
                w += 0.85f;
            } else if (Character.isUpperCase(ch)) {
                w += 0.69f;
            } else if (ch == '-' || ch == '(' || ch == ')') {
                w += 0.34f;
            } else {
                w += 0.54f;
            }
        }
        return bold ? w * 1.08f : w;
    }

    // ---------- low-level PDF ----------

    private void text(StringBuilder p, String s, float x, float y, float size, boolean bold) {
        p.append("BT /").append(bold ? "F2" : "F1").append(' ').append(size).append(" Tf ")
                .append(x).append(' ').append(y).append(" Td (").append(escape(s)).append(") Tj ET\n");
    }

    private String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch == '(' || ch == ')' || ch == '\\') {
                sb.append('\\').append(ch);
            } else if (ch >= 32 && ch < 127) {
                sb.append(ch);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private void write(List<StringBuilder> pages, File file) throws IOException {
        // objects: 1 catalog, 2 pages, 3 font regular, 4 font bold, then (page, content) pairs
        int n = pages.size();
        List<Integer> offsets = new ArrayList<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < n; i++) {
            kids.append(5 + i * 2).append(" 0 R ");
        }
        obj(out, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
        obj(out, offsets, "<< /Type /Pages /Kids [" + kids + "] /Count " + n + " >>");
        obj(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        obj(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        for (int i = 0; i < n; i++) {
            int contentId = 6 + i * 2;
            obj(out, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_W + " " + PAGE_H + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentId + " 0 R >>");
            String content = pages.get(i).toString();
            int len = content.getBytes(StandardCharsets.ISO_8859_1).length;
            obj(out, offsets, "<< /Length " + len + " >>\nstream\n" + content + "endstream");
        }

        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for (int off : offsets) {
            x.append(String.format("%010d 00000 n \n", off));
        }
        x.append("trailer\n<< /Size ").append(offsets.size() + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        out.write(x.toString().getBytes(StandardCharsets.ISO_8859_1));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(out.toByteArray());
        }
    }

    private void obj(ByteArrayOutputStream out, List<Integer> offsets, String body) throws IOException {
        offsets.add(out.size());
        int id = offsets.size();
        out.write((id + " 0 obj\n" + body + "\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
    }
}
