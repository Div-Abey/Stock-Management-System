package com.stockmanagement.ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The tab bar shared by the 4 inventory screens (Items, Categories, Units, Stock).
 * It only reports which tab was clicked; InventoryPanel decides what to show in the content area.
 * Nothing here opens or closes a window.
 */
public class InventoryNavBar extends JPanel {

    public enum Screen {
        ITEMS("Items"), CATEGORIES("Categories"), UNITS("Units & Conversions"), STOCK("Stock Entry & Expiry");

        final String label;

        Screen(String label) {
            this.label = label;
        }
    }

    /** Implemented by the four inventory panels so they can reload their data each time their tab is shown. */
    public interface Refreshable {
        void refreshData();
    }

    private final Map<Screen, JToggleButton> buttons = new EnumMap<>(Screen.class);

    public InventoryNavBar(Screen initial, Consumer<Screen> onSelect) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 6));
        ButtonGroup group = new ButtonGroup();   // exactly one tab looks selected at a time
        for (Screen s : Screen.values()) {
            JToggleButton b = new JToggleButton(s.label, s == initial);
            group.add(b);
            b.addActionListener(e -> onSelect.accept(s));
            buttons.put(s, b);
            add(b);
        }
    }

    /** Highlights a tab without triggering the selection callback. */
    public void setSelected(Screen screen) {
        JToggleButton b = buttons.get(screen);
        if (b != null) b.setSelected(true);
    }

    /** Admin, Manager and Store Keeper can change data; other roles (e.g. Cashier) only look. */
    public static boolean canEdit(String role) {
        if (role == null) return false;
        String r = role.trim().toLowerCase();
        return r.equals("admin") || r.equals("manager") || r.equals("store keeper");
    }
}
