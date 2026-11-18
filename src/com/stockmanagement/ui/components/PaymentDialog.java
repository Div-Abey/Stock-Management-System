package com.stockmanagement.ui.components;

import com.stockmanagement.common.enums.PaymentMethod;
import com.stockmanagement.common.util.CurrencyUtil;
import com.stockmanagement.common.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/** Asks for payment details (amount, method and, for a cheque, number / bank / realization date). */
public final class PaymentDialog {

    /** What the user entered. */
    public static class PaymentInput {
        public double amount;
        public PaymentMethod method;
        public String reference;          // transfer reference (not used for cheques)
        public String chequeNumber;
        public String bankName;
        public LocalDate realizationDate;
    }

    private PaymentDialog() { }

    /** Returns null when the user cancels. Throws ValidationException for an unreadable amount / date. */
    public static PaymentInput show(Component parent, String title, double defaultAmount) {
        JTextField amount = new JTextField(String.format("%.2f", defaultAmount), 10);
        JComboBox<PaymentMethod> method = new JComboBox<>(PaymentMethod.values());
        JTextField reference = new JTextField(14);
        JTextField chequeNo = new JTextField(14);
        JTextField bank = new JTextField(14);
        JTextField realization = new JTextField(DateUtil.format(LocalDate.now().plusDays(30)), 10);

        Runnable toggle = () -> {
            boolean cheque = method.getSelectedItem() == PaymentMethod.CHEQUE;
            chequeNo.setEnabled(cheque);
            bank.setEnabled(cheque);
            realization.setEnabled(cheque);
            reference.setEnabled(!cheque);
        };
        method.addActionListener(e -> toggle.run());
        toggle.run();

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Amount (Rs.):")); p.add(amount);
        p.add(new JLabel("Method:")); p.add(method);
        p.add(new JLabel("Reference (cash / transfer):")); p.add(reference);
        p.add(new JLabel("Cheque number:")); p.add(chequeNo);
        p.add(new JLabel("Bank:")); p.add(bank);
        p.add(new JLabel("Realization date (yyyy-MM-dd):")); p.add(realization);

        if (JOptionPane.showConfirmDialog(parent, p, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return null;
        }
        PaymentInput in = new PaymentInput();
        in.amount = CurrencyUtil.parse(amount.getText());
        in.method = (PaymentMethod) method.getSelectedItem();
        in.reference = reference.getText();
        if (in.method == PaymentMethod.CHEQUE) {
            in.chequeNumber = chequeNo.getText();
            in.bankName = bank.getText();
            in.realizationDate = DateUtil.parse(realization.getText());
        }
        return in;
    }
}
