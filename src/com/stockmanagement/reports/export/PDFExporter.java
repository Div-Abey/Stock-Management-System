package com.stockmanagement.reports.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes plain monospaced text as a multi-page A4 PDF. No library needed (so no pom.xml change).
 * Characters outside Latin-1 are written as '?'.
 */
public final class PDFExporter {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;
    private static final int FONT_SIZE = 8;
    private static final int LEADING = 11;

    private PDFExporter() { }

    public static void exportText(List<String> lines, Path target) throws IOException {
        int linesPerPage = (PAGE_HEIGHT - 2 * MARGIN) / LEADING;
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
        write(out, "%PDF-1.4\n");

        offsets[1] = out.size();
        write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder kids = new StringBuilder();
        for (int p = 0; p < pages.size(); p++) {
            kids.append(4 + p * 2).append(" 0 R ");
        }
        offsets[2] = out.size();
        write(out, "2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>\nendobj\n");

        offsets[3] = out.size();
        write(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>\nendobj\n");

        for (int p = 0; p < pages.size(); p++) {
            int pageObj = 4 + p * 2;
            int contentObj = pageObj + 1;

            StringBuilder content = new StringBuilder();
            content.append("BT\n/F1 ").append(FONT_SIZE).append(" Tf\n").append(LEADING).append(" TL\n");
            content.append(MARGIN).append(' ').append(PAGE_HEIGHT - MARGIN).append(" Td\n");
            for (String line : pages.get(p)) {
                content.append('(').append(escape(line)).append(") Tj T*\n");
            }
            content.append("ET\n");
            byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);

            offsets[pageObj] = out.size();
            write(out, pageObj + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT
                    + "] /Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObj + " 0 R >>\nendobj\n");
            offsets[contentObj] = out.size();
            write(out, contentObj + " 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
            out.write(stream);
            write(out, "\nendstream\nendobj\n");
        }

        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (objectCount + 1) + "\n0000000000 65535 f \n");
        for (int i = 1; i <= objectCount; i++) {
            x.append(String.format("%010d 00000 n \n", offsets[i]));
        }
        x.append("trailer\n<< /Size ").append(objectCount + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        write(out, x.toString());

        Files.write(target, out.toByteArray());
    }

    private static void write(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String escape(String line) {
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
}
