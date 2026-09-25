package com.pims.ui;

import com.pims.db.DatabaseConnection;
import com.pims.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * BillingWindow - shows the bill for the current cart, lets the cashier
 * enter the cash received, and on confirmation:
 *  1. Inserts the sale header into the sales table
 *  2. Inserts each line item into the sale_items table
 *  3. Reduces the medicine stock
 *  4. Saves a copy of the bill to a text file
 */
public class BillingWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private CashierDashboard parent;
    private DefaultTableModel cartModel;   // cart contents (ID, Name, Price, Qty, Line Total)
    private double totalAmount;
    private User cashier;

    private JTextArea billArea;
    private JTextField txtCashPaid;
    private JLabel lblChange;
    private JButton btnConfirm;
    private JButton btnSaveBill;

    public BillingWindow(CashierDashboard parent, DefaultTableModel cartModel, double totalAmount, User cashier) {
        this.parent = parent;
        this.cartModel = cartModel;
        this.totalAmount = totalAmount;
        this.cashier = cashier;

        setTitle("Bill / Checkout");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(520, 650);
        setLocationRelativeTo(parent);

        buildUI();
        setVisible(true);
    }

    private void buildUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Bill display area
        billArea = new JTextArea();
        billArea.setEditable(false);
        billArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(billArea);

        // Payment panel
        JPanel payment = new JPanel(new GridLayout(2, 3, 10, 5));
        payment.setBorder(BorderFactory.createTitledBorder("Payment"));

        payment.add(new JLabel("Cash Paid (R):"));
        txtCashPaid = new JTextField();
        payment.add(txtCashPaid);

        btnConfirm = new JButton("Confirm Sale");
        payment.add(btnConfirm);

        payment.add(new JLabel("Change (R):"));
        lblChange = new JLabel("0.00");
        lblChange.setFont(new Font("Arial", Font.BOLD, 16));
        payment.add(lblChange);

        btnSaveBill = new JButton("Save Bill (No Sale)");
        payment.add(btnSaveBill);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(payment, BorderLayout.SOUTH);

        getContentPane().add(panel);

        txtCashPaid.addActionListener(e -> updateChange());
        btnConfirm.addActionListener(e -> confirmSale());
        btnSaveBill.addActionListener(e -> saveBillFile());

        buildBillText();
    }

    /**
     * Creates the printable text of the bill from the cart.
     */
    private void buildBillText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=====================================\n");
        sb.append("      HEALTHFIRST PHARMACY\n");
        sb.append("           98 Pharmacy Road\n");
        sb.append("             Durban, SA\n");
        sb.append("        Tel: 031 000 0000\n");
        sb.append("=====================================\n");
        sb.append("Cashier: ").append(cashier.getFullName()).append("\n");
        sb.append("Date: ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("-------------------------------------\n");
        sb.append(String.format("%-30s %6s %4s %9s%n", "Medicine", "Price", "Qty", "Total"));

        for (int i = 0; i < cartModel.getRowCount(); i++) {
            String name = cartModel.getValueAt(i, 1).toString();
            String price = cartModel.getValueAt(i, 2).toString();
            String qty = cartModel.getValueAt(i, 3).toString();
            String lineTotal = cartModel.getValueAt(i, 4).toString();
            // Truncate long names so the columns stay aligned
            if (name.length() > 28) {
                name = name.substring(0, 28);
            }
            sb.append(String.format("%-30s %6s %4s %9s%n", name, price, qty, lineTotal));
        }

        sb.append("-------------------------------------\n");
        sb.append(String.format("TOTAL DUE: R%.2f%n", totalAmount));
        sb.append("=====================================\n");
        sb.append("        THANK YOU, PLEASE CALL\n");
        sb.append("          AGAIN SOON!\n");

        billArea.setText(sb.toString());
        billArea.setCaretPosition(0);
    }

    /**
     * Recalculates change as the cashier types the amount paid.
     */
    private void updateChange() {
        try {
            double paid = Double.parseDouble(txtCashPaid.getText().trim());
            double change = paid - totalAmount;
            if (change < 0) {
                lblChange.setText("Waiting for R" + String.format("%.2f", -change) + " more");
                lblChange.setForeground(Color.RED);
            } else {
                lblChange.setText(String.format("%.2f", change));
                lblChange.setForeground(Color.BLACK);
            }
        } catch (NumberFormatException e) {
            lblChange.setText("0.00");
        }
    }

    /**
     * Confirms the sale: saves to database, reduces stock, then offers to print/save.
     */
    private void confirmSale() {
        double paid;
        try {
            paid = Double.parseDouble(txtCashPaid.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid cash amount.",
                    "Payment Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (paid < totalAmount) {
            JOptionPane.showMessageDialog(this,
                    "Cash paid is less than the total. Please collect more money.",
                    "Insufficient Payment", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Run the whole sale in one transaction so if anything fails,
        // nothing is half-written to the database.
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Insert the sale header
            String insertSale = "INSERT INTO sales (total_amount, user_id) VALUES (?, ?)";
            int saleId;
            try (PreparedStatement stmt = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setBigDecimal(1, BigDecimal.valueOf(totalAmount));
                stmt.setInt(2, cashier.getUserId());
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    saleId = keys.getInt(1);
                }
            }

            // 2. Insert each line item + update stock
            String insertItem = "INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) "
                              + "VALUES (?, ?, ?, ?)";
            String updateStock = "UPDATE medicines SET quantity_in_stock = quantity_in_stock - ? "
                               + "WHERE medicine_id = ?";

            for (int i = 0; i < cartModel.getRowCount(); i++) {
                int medicineId = Integer.parseInt(cartModel.getValueAt(i, 0).toString());
                int qty = Integer.parseInt(cartModel.getValueAt(i, 3).toString());
                double priceAtSale = Double.parseDouble(cartModel.getValueAt(i, 2).toString());

                try (PreparedStatement stmt = conn.prepareStatement(insertItem)) {
                    stmt.setInt(1, saleId);
                    stmt.setInt(2, medicineId);
                    stmt.setInt(3, qty);
                    stmt.setBigDecimal(4, BigDecimal.valueOf(priceAtSale));
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement(updateStock)) {
                    stmt.setInt(1, qty);
                    stmt.setInt(2, medicineId);
                    stmt.executeUpdate();
                }
            }

            conn.commit();

            double change = paid - totalAmount;
            String receipt = billArea.getText()
                    + String.format("%nCash Paid: R%.2f%nChange: R%.2f%n", paid, change)
                    + "Sale No: " + saleId + "\n";

            JOptionPane.showMessageDialog(this,
                    "Sale saved successfully. Sale No: " + saleId
                            + "\nChange to give: R" + String.format("%.2f", change),
                    "Sale Complete", JOptionPane.INFORMATION_MESSAGE);

            // Offer to save the receipt
            int saveOption = JOptionPane.showConfirmDialog(this,
                    "Save the bill to a text file?", "Save Bill",
                    JOptionPane.YES_NO_OPTION);
            if (saveOption == JOptionPane.YES_OPTION) {
                saveTextToFile(receipt);
            }

            parent.saleCompleted();
            dispose();

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            JOptionPane.showMessageDialog(this,
                    "Error saving the sale: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Saves the bill to a text file WITHOUT saving a sale (e.g. a quotation).
     */
    private void saveBillFile() {
        saveTextToFile(billArea.getText());
    }

    private void saveTextToFile(String text) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Bill");
        chooser.setSelectedFile(new File("bill_" + System.currentTimeMillis() + ".txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(text);
                JOptionPane.showMessageDialog(this, "Bill saved to:\n" + file.getAbsolutePath(),
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Could not save file: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}