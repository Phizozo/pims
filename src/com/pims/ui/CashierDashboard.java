package com.pims.ui;

import com.pims.db.DatabaseConnection;
import com.pims.model.User;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * CashierDashboard - the Point of Sale (POS) window for Cashiers.
 * Cashiers can search medicines, check stock, add items to a cart,
 * and complete a sale which produces a bill.
 */
public class CashierDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    private User loggedInUser;

    // Medicine search results (keep only cashier-visible data: id, name, price, stock)
    private JTable medicineTable;
    private DefaultTableModel medicineModel;

    // Cart
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel lblTotal;

    public CashierDashboard(User user) {
        // ACCESS CONTROL: only Cashiers may open this dashboard
        if (user == null || !"Cashier".equalsIgnoreCase(user.getRole())) {
            JOptionPane.showMessageDialog(null,
                    "Access denied: Cashier privileges required.",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            new LoginFrame();
            dispose();
            return;
        }
        this.loggedInUser = user;

        setTitle("HealthFirst Pharmacy - Cashier Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(createTopBar(), BorderLayout.NORTH);

        // Left: medicine list, Right: cart
        JPanel main = new JPanel(new GridLayout(1, 2, 10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        main.add(createMedicinePanel());
        main.add(createCartPanel());

        getContentPane().add(main, BorderLayout.CENTER);
        setVisible(true);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(40, 63, 84));
        bar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("Welcome, Cashier - Point of Sale");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 20));

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> logout());

        JPanel right = new JPanel(new FlowLayout());
        right.setOpaque(false);
        right.add(btnLogout);

        bar.add(title, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }


    private JPanel createMedicinePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Medicines"));

        // Search bar
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtSearch = new JTextField(16);
        JButton btnSearch = new JButton("Search");
        JButton btnAll = new JButton("Show All");
        search.add(new JLabel("Search:"));
        search.add(txtSearch);
        search.add(btnSearch);
        search.add(btnAll);

        btnSearch.addActionListener(e -> loadMedicines(txtSearch.getText().trim()));
        btnAll.addActionListener(e -> {
            txtSearch.setText("");
            loadMedicines("");
        });
        txtSearch.addActionListener(e -> loadMedicines(txtSearch.getText().trim()));

        // Medicine table
        medicineModel = new DefaultTableModel(new String[]{"ID", "Name", "Type", "Price", "Stock"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        medicineTable = new JTable(medicineModel);
        JScrollPane scroll = new JScrollPane(medicineTable);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout());
        JButton btnAdd = new JButton("Add to Cart");
        JButton btnStockCheck = new JButton("Stock Check");
        buttons.add(btnAdd);
        buttons.add(btnStockCheck);

        btnAdd.addActionListener(e -> addSelectedToCart());
        btnStockCheck.addActionListener(e -> checkStock());

        panel.add(search, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        loadMedicines("");
        return panel;
    }


    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Cart"));

        cartModel = new DefaultTableModel(new String[]{"ID", "Medicine", "Price (R)", "Qty", "Line Total (R)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(cartModel);
        JScrollPane scroll = new JScrollPane(cartTable);

        // Total
        lblTotal = new JLabel("Total: R0.00");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 18));
        lblTotal.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout());
        JButton btnRemove = new JButton("Remove Selected");
        JButton btnClear = new JButton("Clear Cart");
        JButton btnCheckout = new JButton("Checkout");
        buttons.add(btnCheckout);
        buttons.add(btnRemove);
        buttons.add(btnClear);

        btnCheckout.addActionListener(e -> checkout(loggedInUser.getUserId()));
        btnRemove.addActionListener(e -> removeSelectedFromCart());
        btnClear.addActionListener(e -> clearCart());

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(lblTotal, BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Loads medicines from the database (optionally filtered).
     * Cashiers only see ID, name, type, price and stock - no edit ability.
     */
    public void loadMedicines(String search) {
        medicineModel.setRowCount(0);

        String query = "SELECT medicine_id, name, medicine_type, price, quantity_in_stock "
                     + "FROM medicines ";
        if (!search.isEmpty()) {
            query += "WHERE name LIKE ? ";
        }
        query += "ORDER BY medicine_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (!search.isEmpty()) {
                stmt.setString(1, "%" + search + "%");
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    medicineModel.addRow(new Object[]{
                            rs.getInt("medicine_id"),
                            rs.getString("name"),
                            rs.getString("medicine_type"),
                            String.format("%.2f", rs.getBigDecimal("price")),
                            rs.getInt("quantity_in_stock")
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading medicines: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Adds the selected medicine to the cart.
     */
    private void addSelectedToCart() {
        int row = medicineTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a medicine first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int medicineId = Integer.parseInt(medicineModel.getValueAt(row, 0).toString());
        String medicineName = medicineModel.getValueAt(row, 1).toString();
        double price = Double.parseDouble(medicineModel.getValueAt(row, 3).toString());
        int available = Integer.parseInt(medicineModel.getValueAt(row, 4).toString());

        if (available <= 0) {
            JOptionPane.showMessageDialog(this, medicineName + " is out of stock.",
                    "Out of Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Ask for quantity
        String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity for " + medicineName + ":", "1");
        if (qtyStr == null) return; // cancelled

        int qty;
        try {
            qty = Integer.parseInt(qtyStr.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be a whole number.");
            return;
        }
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.");
            return;
        }
        if (qty > available) {
            JOptionPane.showMessageDialog(this, "Only " + available + " units available in stock.");
            return;
        }

        // If the medicine is already in the cart, increase its quantity
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            if (cartModel.getValueAt(i, 1).toString().equals(medicineName)) {
                int oldQty = Integer.parseInt(cartModel.getValueAt(i, 3).toString());
                int newQty = oldQty + qty;
                if (newQty > available) {
                    JOptionPane.showMessageDialog(this, "Only " + available + " units available in stock.");
                    return;
                }
                cartModel.setValueAt(newQty, i, 3);
                cartModel.setValueAt(String.format("%.2f", price * newQty), i, 4);
                updateTotal();
                return;
            }
        }

        // Not in cart yet - add a new row
        cartModel.addRow(new Object[]{
                medicineId,
                medicineName,
                String.format("%.2f", price),
                qty,
                String.format("%.2f", price * qty)
        });
        updateTotal();
    }

    private void removeSelectedFromCart() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item in the cart first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cartModel.removeRow(row);
        updateTotal();
    }

    private void clearCart() {
        cartModel.setRowCount(0);
        updateTotal();
    }

    private void updateTotal() {
        double total = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            total += Double.parseDouble(cartModel.getValueAt(i, 4).toString());
        }
        lblTotal.setText("Total: R" + String.format("%.2f", total));
    }


    private void checkStock() {
        String name = JOptionPane.showInputDialog(this,
                "Enter the medicine name to check stock and price:");
        if (name == null || name.trim().isEmpty()) return;

        String query = "SELECT name, price, quantity_in_stock, expiry_date FROM medicines WHERE name LIKE ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "%" + name.trim() + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                StringBuilder result = new StringBuilder();
                int count = 0;
                while (rs.next()) {
                    count++;
                    result.append("Medicine: ").append(rs.getString("name")).append("\n");
                    result.append("Price: R").append(rs.getBigDecimal("price").toPlainString()).append("\n");
                    result.append("In Stock: ").append(rs.getInt("quantity_in_stock")).append("\n");
                    result.append("Expiry: ").append(rs.getDate("expiry_date") == null ? "N/A" : rs.getDate("expiry_date").toString()).append("\n");
                    result.append("----------------------------\n");
                }
                if (count == 0) {
                    JOptionPane.showMessageDialog(this, "No medicine found with that name.",
                            "Stock Check", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, result.toString(),
                            "Stock Check", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error checking stock: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Checkout: opens the billing window. The sale is only written to the
     * database when the user confirms the bill.
     */
    private void checkout(int cashierUserId) {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "The cart is empty.",
                    "Checkout", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double total = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            total += Double.parseDouble(cartModel.getValueAt(i, 4).toString());
        }

        new BillingWindow(this, cartModel, total, loggedInUser);
    }


    public void saleCompleted() {
        clearCart();
        loadMedicines("");
    }

    private void logout() {
        int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?",
                "Logout", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame();
        }
    }
}