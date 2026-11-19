package com.stockmanagement.ui.sales;

import com.stockmanagement.sales.model.Sale;
import com.stockmanagement.sales.service.SalesService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.time.LocalDate;

// [UPDATED BY KASUN] Dedicated Billing & Payment Checkout frame handling CASH, CARD, CREDIT, and CHEQUE settlements
public class BillingFrame extends JFrame {

    private final Sale draftSale;
    private final SalesService salesService;
    private final Runnable onSaleSuccessCallback;

    /** What the cashier sees in the drop-down; `code` is what the system stores (CASH, CARD, CREDIT, CHEQUE). */
    private static class PaymentOption {
        final String code;
        final String label;

        PaymentOption(String code, String label) {
            this.code = code;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private JComboBox<PaymentOption> cmbPaymentType;
    private JTextField txtAmountPaid;
    private JLabel lblBalance;

    // Credit Terms Panel
    private JPanel creditPanel;
    private JTextField txtDueDate;

    // Cheque Details Panel
    private JPanel chequePanel;
    private JTextField txtChequeNumber;
    private JTextField txtBankName;
    private JTextField txtRealizationDate;

    public BillingFrame(Sale draftSale, SalesService salesService, Runnable onSaleSuccessCallback) {
        this.draftSale = draftSale;
        this.salesService = salesService != null ? salesService : new SalesService();
        this.onSaleSuccessCallback = onSaleSuccessCallback;

        this.salesService.getBillingService().recalculateSaleTotals(this.draftSale);

        setTitle("Billing & Payment Checkout");
        setSize(520, 700);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        add(buildSummaryPanel(), BorderLayout.NORTH);
        add(buildPaymentFormPanel(), BorderLayout.CENTER);
        add(buildActionFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildSummaryPanel() {
        JPanel summary = new JPanel(new GridLayout(4, 2, 8, 6));
        summary.setBorder(new TitledBorder("Invoice Summary"));
        summary.setBackground(new Color(248, 250, 252));

        summary.add(new JLabel("Customer:"));
        JLabel lblCust = new JLabel(draftSale.getCustomerName() != null ? draftSale.getCustomerName() : "Walk-in Customer");
        lblCust.setFont(new Font("SansSerif", Font.BOLD, 13));
        summary.add(lblCust);

        summary.add(new JLabel("Gross Total:"));
        summary.add(new JLabel(String.format("Rs. %.2f", draftSale.getTotalAmount())));

        summary.add(new JLabel("Bulk Discount:"));
        JLabel lblDisc = new JLabel(String.format("-Rs. %.2f", draftSale.getDiscountAmount()));
        summary.add(lblDisc);

        summary.add(new JLabel("NET PAYABLE:"));
        JLabel lblNet = new JLabel(String.format("Rs. %.2f", draftSale.getNetAmount()));
        lblNet.setFont(new Font("SansSerif", Font.BOLD, 16));
        summary.add(lblNet);

        return summary;
    }

    private JPanel buildPaymentFormPanel() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(6, 14, 6, 14));

        JLabel lblMethod = new JLabel("Payment Method:");
        lblMethod.setAlignmentX(Component.LEFT_ALIGNMENT);

        cmbPaymentType = new JComboBox<>(new PaymentOption[]{
                new PaymentOption("CASH", "CASH  -  customer pays in cash"),
                new PaymentOption("CARD", "CARD  -  debit / credit card"),
                new PaymentOption("CREDIT", "CREDIT  -  customer pays later (on account)"),
                new PaymentOption("CHEQUE", "CHEQUE  -  bank cheque, money arrives later")
        });
        cmbPaymentType.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        cmbPaymentType.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbPaymentType.addActionListener(e -> updatePaymentModeFields());

        JLabel lblPaid = new JLabel("Amount Paid / Tendered (Rs.):");
        lblPaid.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtAmountPaid = new JTextField(String.format("%.2f", draftSale.getNetAmount()));
        txtAmountPaid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        txtAmountPaid.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtAmountPaid.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                refreshBalanceLabel();
            }
        });

        lblBalance = new JLabel("Change / Balance: Rs. 0.00");
        lblBalance.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblBalance.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Credit Sub-Panel
        creditPanel = new JPanel(new GridLayout(3, 1, 4, 4));
        creditPanel.setBorder(new TitledBorder("Credit Sale Details"));
        creditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        creditPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtDueDate = new JTextField(LocalDate.now().plusDays(30).toString());
        creditPanel.add(new JLabel("<html><i>Customer takes the goods now and pays by the due date (needs a registered customer).</i></html>"));
        creditPanel.add(new JLabel("Repayment Due Date (YYYY-MM-DD):"));
        creditPanel.add(txtDueDate);
        creditPanel.setVisible(false);

        // Cheque Sub-Panel
        chequePanel = new JPanel(new GridLayout(7, 1, 3, 3));
        chequePanel.setBorder(new TitledBorder("Received Cheque Details"));
        chequePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        chequePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtChequeNumber = new JTextField();
        txtBankName = new JTextField();
        txtRealizationDate = new JTextField(LocalDate.now().plusDays(7).toString());

        chequePanel.add(new JLabel("<html><i>A cheque is a written bank payment order. The money reaches the shop on the realization date.</i></html>"));
        chequePanel.add(new JLabel("Cheque Number:"));
        chequePanel.add(txtChequeNumber);
        chequePanel.add(new JLabel("Bank Name:"));
        chequePanel.add(txtBankName);
        chequePanel.add(new JLabel("Realization Date (YYYY-MM-DD):"));
        chequePanel.add(txtRealizationDate);
        chequePanel.setVisible(false);

        form.add(lblMethod);
        form.add(Box.createVerticalStrut(4));
        form.add(cmbPaymentType);
        form.add(Box.createVerticalStrut(10));
        form.add(lblPaid);
        form.add(Box.createVerticalStrut(4));
        form.add(txtAmountPaid);
        form.add(Box.createVerticalStrut(8));
        form.add(lblBalance);
        form.add(Box.createVerticalStrut(10));
        form.add(creditPanel);
        form.add(chequePanel);

        return form;
    }

    private JPanel buildActionFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JButton btnConfirm = new JButton("Confirm & Generate Invoice");
        btnConfirm.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnConfirm.addActionListener(e -> processCheckout());

        footer.add(btnCancel);
        footer.add(btnConfirm);
        return footer;
    }

    private String selectedPaymentType() {
        PaymentOption option = (PaymentOption) cmbPaymentType.getSelectedItem();
        return option != null ? option.code : "CASH";
    }

    private void updatePaymentModeFields() {
        String method = selectedPaymentType();
        boolean isCredit = "CREDIT".equalsIgnoreCase(method);
        boolean isCheque = "CHEQUE".equalsIgnoreCase(method);

        creditPanel.setVisible(isCredit);
        chequePanel.setVisible(isCheque);

        if (isCredit) {
            txtAmountPaid.setText("0.00");
        } else {
            txtAmountPaid.setText(String.format("%.2f", draftSale.getNetAmount()));
        }
        refreshBalanceLabel();
        revalidate();
        repaint();
    }

    private void refreshBalanceLabel() {
        try {
            double paid = Double.parseDouble(txtAmountPaid.getText().trim());
            double balance = salesService.getBillingService().calculateChangeBalance(draftSale.getNetAmount(), paid);
            lblBalance.setText(String.format("Change / Balance: Rs. %.2f", balance));
            lblBalance.setForeground(balance >= 0 ? new Color(22, 163, 74) : Color.RED);
        } catch (NumberFormatException e) {
            lblBalance.setText("Change / Balance: Rs. 0.00");
            lblBalance.setForeground(Color.DARK_GRAY);
        }
    }

    private void processCheckout() {
        String paymentType = selectedPaymentType();
        double amountPaid = 0.0;

        String paidText = txtAmountPaid.getText().trim();
        if (!paidText.isEmpty()) {
            try {
                amountPaid = Double.parseDouble(paidText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid numeric Amount Paid.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else if (!"CREDIT".equalsIgnoreCase(paymentType)) {
            JOptionPane.showMessageDialog(this, "Amount Paid is required.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        draftSale.setPaymentType(paymentType);
        draftSale.setAmountPaid(amountPaid);

        if ("CREDIT".equalsIgnoreCase(paymentType)) {
            try {
                draftSale.setDueDate(LocalDate.parse(txtDueDate.getText().trim()));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid Due Date (YYYY-MM-DD).",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        if ("CHEQUE".equalsIgnoreCase(paymentType)) {
            draftSale.setChequeNumber(txtChequeNumber.getText().trim());
            draftSale.setBankName(txtBankName.getText().trim());
            draftSale.setChequeIssueDate(LocalDate.now().toString());
            draftSale.setChequeRealizationDate(txtRealizationDate.getText().trim());
        }

        try {
            Sale completedSale = salesService.completeSale(draftSale);
            dispose();

            if (onSaleSuccessCallback != null) {
                onSaleSuccessCallback.run();
            }

            new InvoiceFrame(completedSale).setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Checkout Failed: " + ex.getMessage(),
                    "Transaction Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
