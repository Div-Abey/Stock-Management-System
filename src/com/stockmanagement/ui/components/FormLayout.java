package com.stockmanagement.ui.components;

import javax.swing.*;
import java.awt.*;

/** Stacks form fields in several rows so long forms never get cut off on a narrow window. */
public final class FormLayout {
    private FormLayout() { }

    /** One row of components, left to right. */
    public static JPanel row(Component... components) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        for (Component c : components) {
            p.add(c);
        }
        return p;
    }

    /** A titled box holding the given rows, one under the other. */
    public static JPanel form(String title, JPanel... rows) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createTitledBorder(title));
        for (JPanel r : rows) {
            r.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(r);
        }
        return box;
    }
}
