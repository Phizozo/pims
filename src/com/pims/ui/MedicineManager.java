package com.pims.ui;

import com.pims.db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * MedicineManager - Administrator module: full CRUD for medicines.
 * Shown as a tab inside the AdminDashboard.
 */
public class MedicineManager extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private DefaultTableModel tableModel;

    // Form fields
    private JTextField txtName;
    private JTextField txtCompany;
    private JComboBox<String> cmbType;
    private JTextField txtPrice;
    private JTextField txtQuantity;
    private JTextField txtReorderLevel;
    private JTextField txtExpiry;
    private JComboBox<String> cmbSupplier;

    // Keeps track of supplier names -> supplier ids
    private Map<String, Integer> supplierMap = new HashMap<>();

    private int selectedMedicineId = -1;

    public MedicineManager() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ----- Top: form for adding/editing -----
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Medicine Details"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        txtName = new JTextField(18);
        txtCompany = new JTextField(18);
        cmbType = new JComboBox<>(new String[]{"Tablet", "Capsule", "Syrup", "Injection", "Cream", "Powder", "Drops", "Ointment"});
        txtPrice = new JTextField(18);
        txtQuantity = new JTextField(18);
        txtReorderLevel = new JTextField(18);
        txtExpiry = new JTextField(18);
        cmbSupplier = new JComboBox<>();

        addRow(formPanel, gbc, 0, "Medicine Name:", txtName);
        addRow(formPanel, gbc, 1, "Company:", txtCompany);
        addRow(formPanel, gbc, 2, "Type:", cmbType);
        addRow(formPanel, gbc, 3, "Price (R):", txtPrice);
        addRow(formPanel, gbc, 4, "Quantity in Stock:", txtQuantity);
        addRow(formPanel, gbc, 5, "Reorder Level:", txtReorderLevel);
        addRow(formPanel, gbc, 6, "Expiry Date (yyyy-mm-dd):", txtExpiry);
        addRow(formPanel, gbc, 7, "Supplier:", cmbSupplier);

        gbc.gridx = 1; gbc.gridy = 8;
        formPanel.add(createButtonPanel(), gbc);

        // ----- Centre: the table -----
        tableModel = new DefaultTableModel(new String[]{
                "ID", "Name", "Company", "Type", "Price", "Stock", "Reorder", "Expiry", "Supplier"
        }, 0);
        table = new JTable(tableModel);

        // Highlight rows whose stock has dropped to/below the reorder level
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(new LowStockRenderer());
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Medicine List"));

        JPanel centre = new JPanel(new BorderLayout());
        centre.add(scrollPane, BorderLayout.CENTER);
        centre.add(createSearchPanel(), BorderLayout.NORTH);

        add(formPanel, BorderLayout.NORTH);
        add(centre, BorderLayout.CENTER);

        loadSuppliersIntoCombo();
        loadMedicines("");

        // Clicking a row fills the form
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

        btnAdd.addActionListener(e -> addMedicine());
        btnUpdate.addActionListener(e -> updateMedicine());
        btnDelete.addActionListener(e -> deleteMedicine());
        btnClear.addActionListener(e -> clearForm());

        return buttons;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtSearch = new JTextField(20);
        JButton btnSearch = new JButton("Search");
        JButton btnRefresh = new JButton("Reset");

        panel.add(new JLabel("Search Medicine:"));
        panel.add(txtSearch);
        panel.add(btnSearch);
        panel.add(btnRefresh);

        btnSearch.addActionListener(e -> loadMedicines(txtSearch.getText().trim()));
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            loadMedicines("");
        });

        return panel;
    }

    /**
     * Fills the supplier combo box with supplier names from the database.
     */
    private void loadSuppliersIntoCombo() {
        supplierMap.clear();
        cmbSupplier.removeAllItems();
        String query = "SELECT supplier_id, name FROM suppliers ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String name = rs.getString("name");
                supplierMap.put(name, rs.getInt("supplier_id"));
                cmbSupplier.addItem(name);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading suppliers: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Loads medicines (optionally filtered by a search keyword) into the table.
     */
    public void loadMedicines(String search) {
        tableModel.setRowCount(0);
        StringBuilder query = new StringBuilder(
                "SELECT m.medicine_id, m.name, m.company, m.medicine_type, m.price, "
              + "m.quantity_in_stock, m.reorder_level, m.expiry_date, s.name AS supplier_name "
              + "FROM medicines m LEFT JOIN suppliers s ON m.supplier_id = s.supplier_id ");

        if (!search.isEmpty()) {
            query.append("WHERE m.name LIKE ? OR m.company LIKE ? ");
        }
        query.append("ORDER BY m.medicine_id");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {

            if (!search.isEmpty()) {
                stmt.setString(1, "%" + search + "%");
                stmt.setString(2, "%" + search + "%");
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getInt("medicine_id"),
                            rs.getString("name"),
                            rs.getString("company"),
                            rs.getString("medicine_type"),
                            String.format("R%.2f", rs.getBigDecimal("price")),
                            rs.getInt("quantity_in_stock"),
                            rs.getInt("reorder_level"),
                            rs.getDate("expiry_date") == null ? "" : rs.getDate("expiry_date").toString(),
                            rs.getString("supplier_name")
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
     * Copies the selected table row into the form fields.
     */
    private void fillFormFromSelectedRow(int row) {
        selectedMedicineId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        txtName.setText(tableModel.getValueAt(row, 1).toString());
        txtCompany.setText(tableModel.getValueAt(row, 2).toString());
        cmbType.setSelectedItem(tableModel.getValueAt(row, 3).toString());
        txtPrice.setText(tableModel.getValueAt(row, 4).toString().replace("R", ""));
        txtQuantity.setText(tableModel.getValueAt(row, 5).toString());
        txtReorderLevel.setText(tableModel.getValueAt(row, 6).toString());
        Object exp = tableModel.getValueAt(row, 7);
        txtExpiry.setText(exp == null || exp.toString().isEmpty() ? "" : exp.toString());

        Object supplierObj = tableModel.getValueAt(row, 8);
        if (supplierObj == null || supplierObj.toString().isEmpty()) {
            cmbSupplier.setSelectedIndex(-1);
        } else {
            cmbSupplier.setSelectedItem(supplierObj.toString());
        }
    }

    /**
     * Inserts a new medicine into the database.
     */
    private void addMedicine() {
        if (!validateForm(false)) return;

        String sql = "INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, "
                   + "reorder_level, expiry_date, supplier_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, txtName.getText().trim());
            stmt.setString(2, txtCompany.getText().trim());
            stmt.setString(3, cmbType.getSelectedItem().toString());
            stmt.setBigDecimal(4, new BigDecimal(txtPrice.getText().trim()));
            stmt.setInt(5, Integer.parseInt(txtQuantity.getText().trim()));
            stmt.setInt(6, Integer.parseInt(txtReorderLevel.getText().trim()));
            // Empty expiry date -> NULL
            if (txtExpiry.getText().trim().isEmpty()) {
                stmt.setNull(7, Types.DATE);
            } else {
                stmt.setDate(7, Date.valueOf(txtExpiry.getText().trim()));
            }
            Object selected = cmbSupplier.getSelectedItem();
            if (selected != null) {
                stmt.setInt(8, supplierMap.get(selected.toString()));
            } else {
                stmt.setNull(8, Types.INTEGER);
            }

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Medicine added successfully.");
            clearForm();
            loadMedicines("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding medicine: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Updates  selected medicine.
     */
    private void updateMedicine() {
        if (selectedMedicineId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a medicine from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // If the user changed the medicine name they still need to keep the ID, so we re-fetch from table row
        int row = table.getSelectedRow();
        if (row != -1) {
            selectedMedicineId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        }
        if (!validateForm(true)) return;

        String sql = "UPDATE medicines SET name=?, company=?, medicine_type=?, price=?, "
                   + "quantity_in_stock=?, reorder_level=?, expiry_date=?, supplier_id=? "
                   + "WHERE medicine_id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, txtName.getText().trim());
            stmt.setString(2, txtCompany.getText().trim());
            stmt.setString(3, cmbType.getSelectedItem().toString());
            stmt.setBigDecimal(4, new BigDecimal(txtPrice.getText().trim()));
            stmt.setInt(5, Integer.parseInt(txtQuantity.getText().trim()));
            stmt.setInt(6, Integer.parseInt(txtReorderLevel.getText().trim()));
            if (txtExpiry.getText().trim().isEmpty()) {
                stmt.setNull(7, Types.DATE);
            } else {
                stmt.setDate(7, Date.valueOf(txtExpiry.getText().trim()));
            }
            Object selected = cmbSupplier.getSelectedItem();
            if (selected != null) {
                stmt.setInt(8, supplierMap.get(selected.toString()));
            } else {
                stmt.setNull(8, Types.INTEGER);
            }
            stmt.setInt(9, selectedMedicineId);

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Medicine updated successfully.");
            loadMedicines("");
            findAndSelectRow(selectedMedicineId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating medicine: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Deletes the currently selected medicine.
     */
    private void deleteMedicine() {
        if (selectedMedicineId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a medicine from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this medicine?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM medicines WHERE medicine_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, selectedMedicineId);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Medicine deleted successfully.");
            clearForm();
            loadMedicines("");
        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this,
                    "This medicine has sale history and cannot be deleted. "
                    + "Only medicines without linked sales can be removed.",
                    "Cannot Delete", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error deleting medicine: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void clearForm() {
        selectedMedicineId = -1;
        txtName.setText("");
        txtCompany.setText("");
        cmbType.setSelectedIndex(0);
        txtPrice.setText("");
        txtQuantity.setText("");
        txtReorderLevel.setText("");
        txtExpiry.setText("");
        cmbSupplier.setSelectedIndex(-1);
        table.clearSelection();
    }

    private boolean validateForm(boolean isUpdate) {
        if (txtName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Medicine name is required.");
            return false;
        }
        try {
            BigDecimal price = new BigDecimal(txtPrice.getText().trim());
            int qty = Integer.parseInt(txtQuantity.getText().trim());
            int reorder = Integer.parseInt(txtReorderLevel.getText().trim());
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                JOptionPane.showMessageDialog(this, "Price cannot be negative.");
                return false;
            }
            if (qty < 0 || reorder < 0) {
                JOptionPane.showMessageDialog(this, "Quantity and Reorder Level cannot be negative.");
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price, Quantity and Reorder Level must be numbers.");
            return false;
        }
        if (!txtExpiry.getText().trim().isEmpty()) {
            try {
                Date.valueOf(txtExpiry.getText().trim());
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, "Expiry date must be in format yyyy-mm-dd.");
                return false;
            }
        }
        return true;
    }


    private void findAndSelectRow(int medicineId) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Integer.parseInt(tableModel.getValueAt(i, 0).toString()) == medicineId) {
                table.setRowSelectionInterval(i, i);
                return;
            }
        }
    }


    private class LowStockRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable jt, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component comp = super.getTableCellRendererComponent(jt, value, isSelected, hasFocus, row, column);
            if (row >= 0) {
                try {
                    int stock = Integer.parseInt(tableModel.getValueAt(row, 5).toString());
                    int reorder = Integer.parseInt(tableModel.getValueAt(row, 6).toString());
                    if (stock <= reorder) {
                        comp.setBackground(new Color(255, 200, 200));
                        comp.setForeground(Color.RED);
                        return comp;
                    }
                } catch (Exception ignored) {
                }
            }
            comp.setBackground(isSelected ? jt.getSelectionBackground() : jt.getBackground());
            comp.setForeground(isSelected ? jt.getSelectionForeground() : jt.getForeground());
            return comp;
        }
    }
}