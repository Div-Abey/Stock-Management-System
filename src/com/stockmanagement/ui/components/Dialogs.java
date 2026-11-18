package com.stockmanagement.ui.components;

import com.stockmanagement.common.exception.ValidationException;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.sql.SQLException;

/** Small helpers shared by the section 3 screens. */
public final class Dialogs {
    private Dialogs() { }

    /** An action that may fail with a database error. */
    public interface SqlAction {
        void run() throws SQLException;
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Runs an action; validation and database errors are shown in a dialog instead of crashing the screen. */
    public static void run(Component parent, SqlAction action) {
        try {
            action.run();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Check your input", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            error(parent, "Database error: " + ex.getMessage());
        }
    }
}
