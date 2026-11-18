package com.stockmanagement.ui.inventory;

import com.stockmanagement.ui.components.InventoryNavBar;
import com.stockmanagement.ui.components.InventoryNavBar.Refreshable;
import com.stockmanagement.ui.components.InventoryNavBar.Screen;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * The whole Inventory module as ONE panel for the Dashboard's content area: a tab bar on top and the
 * selected screen (Items / Categories / Units & Conversions / Stock Entry & Expiry) below it.
 * Switching tabs just swaps a card in this panel, so no extra window is ever opened.
 * Each screen is built the first time its tab is opened and then kept, so half-typed forms survive a tab switch.
 */
public class InventoryPanel extends JPanel {

    private final int userId;
    private final String role;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<Screen, JComponent> screens = new EnumMap<>(Screen.class);
    private final InventoryNavBar navBar;
    private Screen current;

    public InventoryPanel(int userId, String role) {
        super(new BorderLayout());
        this.userId = userId;
        this.role = role;
        navBar = new InventoryNavBar(Screen.ITEMS, this::showScreen);
        add(navBar, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    /**
     * Call when the Inventory menu is clicked (after this panel is on screen): opens the Items tab the first
     * time, and afterwards reloads whichever tab was last used.
     */
    public void refresh() {
        if (current == null) {
            showScreen(Screen.ITEMS);
        } else {
            ((Refreshable) screens.get(current)).refreshData();
        }
    }

    /** Shows one tab inside this panel (building it on first use) and reloads its data. */
    public void showScreen(Screen screen) {
        if (screen == current) return;
        JComponent panel = screens.get(screen);
        if (panel == null) {
            panel = create(screen);
            screens.put(screen, panel);
            content.add(panel, screen.name());
        }
        current = screen;
        navBar.setSelected(screen);
        cards.show(content, screen.name());
        ((Refreshable) panel).refreshData();
    }

    private JComponent create(Screen screen) {
        switch (screen) {
            case CATEGORIES: return new CategoryPanel(role);
            case UNITS: return new UnitManagementPanel(role);
            case STOCK: return new StockManagementPanel(userId, role);
            default: return new ItemManagementPanel(role);
        }
    }
}
