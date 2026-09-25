package com.pims.ui;

import com.pims.db.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * SupplierManager - Administrator module: CRUD for suppliers.
 * Shown as a tab inside the AdminDashboard.
 */
public class SupplierManager extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private DefaultTableModel tableModel;

    private JTextField txtName;
    private JTextField txtContactPerson;
    private JTextField txtPhone;
    private JTextField txtEmail;
    private JTextArea txtAddress;

    private int selectedSupplierId = -1;

    public SupplierManager() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Supplier Details"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        txtName = new JTextField(18);
        txtContactPerson = new JTextField(18);
        txtPhone = new JTextField(18);
        txtEmail = new JTextField(18);
        txtAddress = new JTextArea(3, 18);
        txtAddress.setLineWrap(true);

        addRow(formPanel, gbc, 0, "Supplier Name:", txtName);
        addRow(formPanel, gbc, 1, "Contact Person:", txtContactPerson);
        addRow(formPanel, gbc, 2, "Phone:", txtPhone);
        addRow(formPanel, gbc, 3, "Email:", txtEmail);
        addRow(formPanel, gbc, 4, "Address:", new JScrollPane(txtAddress));

        gbc.gridx = 1; gbc.gridy = 5;
        formPanel.add(createButtonPanel(), gbc);

        // Table panel
        tableModel = new DefaultTableModel(new String[]{
                "ID", "Name", "Contact Person", "Phone", "Email", "Address"
        }, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Supplier List"));

        add(formPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadSuppliers();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                fillFormFromSelectedRow(table.getSelectedRow());
            }
        });
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int y, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = y;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.gridy = y;
        panel.add(field, gbc);
    }

    private JPanel createButtonPanel() {
        JPanel buttons = new JPanel(new FlowLayout());
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear Form");

        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);

        btnAdd.addActionListener(e -> addSupplier());
        btnUpdate.addActionListener(e -> updateSupplier());
        btnDelete.addActionListener(e -> deleteSupplier());
        btnClear.addActionListener(e -> clearForm());

        return buttons;
    }

    public void loadSuppliers() {
        tableModel.setRowCount(0);
        String query = "SELECT * FROM suppliers ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("supplier_id"),
                        rs.getString("name"),
                        rs.getString("contact_person"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("address")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading suppliers: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void fillFormFromSelectedRow(int row) {
        selectedSupplierId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        txtName.setText(tableModel.getValueAt(row, 1).toString());
        Object cp = tableModel.getValueAt(row, 2);
        txtContactPerson.setText(cp == null ? "" : cp.toString());
        Object ph = tableModel.getValueAt(row, 3);
        txtPhone.setText(ph == null ? "" : ph.toString());
        Object em = tableModel.getValueAt(row, 4);
        txtEmail.setText(em == null ? "" : em.toString());
        Object ad = tableModel.getValueAt(row, 5);
        txtAddress.setText(ad == null ? "" : ad.toString());
    }

    private void addSupplier() {
        if (txtName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Supplier name is required.");
            return;
        }

        String sql = "INSERT INTO suppliers (name, contact_person, phone, email, address) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, txtName.getText().trim());
            stmt.setString(2, txtContactPerson.getText().trim());
            stmt.setString(3, txtPhone.getText().trim());
            stmt.setString(4, txtEmail.getText().trim());
            stmt.setString(5, txtAddress.getText().trim());

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Supplier added successfully.");
            clearForm();
            loadSuppliers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding supplier: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateSupplier() {
        if (selectedSupplierId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = table.getSelectedRow();
        if (row != -1) {
            selectedSupplierId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        }

        String sql = "UPDATE suppliers SET name=?, contact_person=?, phone=?, email=?, address=? "
                   + "WHERE supplier_id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, txtName.getText().trim());
            stmt.setString(2, txtContactPerson.getText().trim());
            stmt.setString(3, txtPhone.getText().trim());
            stmt.setString(4, txtEmail.getText().trim());
            stmt.setString(5, txtAddress.getText().trim());
            stmt.setInt(6, selectedSupplierId);

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Supplier updated successfully.");
            loadSuppliers();
            findAndSelectRow(selectedSupplierId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating supplier: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void deleteSupplier() {
        if (selectedSupplierId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this supplier? Medicines linked to them will lose their supplier.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM suppliers WHERE supplier_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Set medicines to NULL supplier first so the FK is not violated
            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE medicines SET supplier_id = NULL WHERE supplier_id = ?")) {
                update.setInt(1, selectedSupplierId);
                update.executeUpdate();
            }

            stmt.setInt(1, selectedSupplierId);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Supplier deleted successfully.");
            clearForm();
            loadSuppliers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error deleting supplier: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void clearForm() {
        selectedSupplierId = -1;
        txtName.setText("");
        txtContactPerson.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        table.clearSelection();
    }

    private void findAndSelectRow(int supplierId) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Integer.parseInt(tableModel.getValueAt(i, 0).toString()) == supplierId) {
                table.setRowSelectionInterval(i, i);
                return;
            }
        }
    }
}