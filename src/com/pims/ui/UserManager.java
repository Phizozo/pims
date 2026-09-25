package com.pims.ui;

import com.pims.db.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

import com.pims.model.User;

/**
 * UserManager - Administrator module: create, delete and manage Cashier accounts.
 * Shown as a tab inside the AdminDashboard.
 */
public class UserManager extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private DefaultTableModel tableModel;

    private JTextField txtFullName;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbRole;

    private int selectedUserId = -1;
    private User loggedInUser;

    public UserManager(User loggedInUser) {
        this.loggedInUser = loggedInUser;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("User Account Details"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        txtFullName = new JTextField(18);
        txtUsername = new JTextField(18);
        txtPassword = new JPasswordField(18);
        cmbRole = new JComboBox<>(new String[]{"Cashier", "Admin"});

        addRow(formPanel, gbc, 0, "Full Name:", txtFullName);
        addRow(formPanel, gbc, 1, "Username:", txtUsername);
        addRow(formPanel, gbc, 2, "Password:", txtPassword);
        addRow(formPanel, gbc, 3, "Role:", cmbRole);

        gbc.gridx = 1; gbc.gridy = 4;
        formPanel.add(createButtonPanel(), gbc);

        // Table panel
        tableModel = new DefaultTableModel(new String[]{"ID", "Username", "Role", "Full Name"}, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("User Accounts"));

        add(formPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadUsers();

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
        JButton btnAdd = new JButton("Add User");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear Form");

        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);

        btnAdd.addActionListener(e -> addUser());
        btnUpdate.addActionListener(e -> updateUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnClear.addActionListener(e -> clearForm());

        return buttons;
    }

    public void loadUsers() {
        tableModel.setRowCount(0);
        String query = "SELECT * FROM users ORDER BY full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("full_name")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading users: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void fillFormFromSelectedRow(int row) {
        selectedUserId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        txtUsername.setText(tableModel.getValueAt(row, 1).toString());
        cmbRole.setSelectedItem(tableModel.getValueAt(row, 2).toString());
        txtFullName.setText(tableModel.getValueAt(row, 3).toString());
        txtPassword.setText(""); // do not show the stored password
    }

    private void addUser() {
        if (txtFullName.getText().trim().isEmpty()
                || txtUsername.getText().trim().isEmpty()
                || new String(txtPassword.getPassword()).isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full name, username and password are required.");
            return;
        }

        String sql = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, txtUsername.getText().trim());
            stmt.setString(2, new String(txtPassword.getPassword()));
            stmt.setString(3, cmbRole.getSelectedItem().toString());
            stmt.setString(4, txtFullName.getText().trim());

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "User added successfully.");
            clearForm();
            loadUsers();
        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this, "That username is already taken.",
                    "Duplicate Username", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding user: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateUser() {
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = table.getSelectedRow();
        if (row != -1) {
            selectedUserId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        }

        // Guard: do not let the last Administrator be demoted/locked out
        String currentRole = getRoleOf(selectedUserId);
        String newRole = cmbRole.getSelectedItem().toString();
        if ("Admin".equalsIgnoreCase(currentRole) && !"Admin".equalsIgnoreCase(newRole)
                && countAdmins() <= 1) {
            JOptionPane.showMessageDialog(this,
                    "Cannot demote the last Administrator account.",
                    "Access Restricted", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "UPDATE users SET username=?, role=?, full_name=? ";
        if (!new String(txtPassword.getPassword()).isEmpty()) {
            sql += ", password=? ";
        }
        sql += "WHERE user_id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, txtUsername.getText().trim());
            stmt.setString(2, cmbRole.getSelectedItem().toString());
            stmt.setString(3, txtFullName.getText().trim());

            String pw = new String(txtPassword.getPassword());
            int fieldCount = 4;
            if (!pw.isEmpty()) {
                stmt.setString(fieldCount++, pw);
            }
            stmt.setInt(fieldCount, selectedUserId);

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "User updated successfully.");
            loadUsers();
            findAndSelectRow(selectedUserId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating user: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void deleteUser() {
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Guard: cannot delete your own logged-in account
        if (loggedInUser != null && selectedUserId == loggedInUser.getUserId()) {
            JOptionPane.showMessageDialog(this,
                    "You cannot delete your own account while logged in.",
                    "Access Restricted", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Guard: cannot delete the last Administrator account
        if ("Admin".equalsIgnoreCase(getRoleOf(selectedUserId)) && countAdmins() <= 1) {
            JOptionPane.showMessageDialog(this,
                    "Cannot delete the last Administrator account.",
                    "Access Restricted", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this user account?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM users WHERE user_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, selectedUserId);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "User deleted successfully.");
            clearForm();
            loadUsers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error deleting user: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void clearForm() {
        selectedUserId = -1;
        txtFullName.setText("");
        txtUsername.setText("");
        txtPassword.setText("");
        cmbRole.setSelectedIndex(0);
        table.clearSelection();
    }

    private void findAndSelectRow(int userId) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Integer.parseInt(tableModel.getValueAt(i, 0).toString()) == userId) {
                table.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    /**
     * Returns the role of the given user id based on the current table data.
     */
    private String getRoleOf(int userId) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Integer.parseInt(tableModel.getValueAt(i, 0).toString()) == userId) {
                return tableModel.getValueAt(i, 2).toString();
            }
        }
        return "";
    }

    /**
     * Counts how many Administrator accounts exist (based on current table data).
     */
    private int countAdmins() {
        int count = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ("Admin".equalsIgnoreCase(tableModel.getValueAt(i, 2).toString())) {
                count++;
            }
        }
        return count;
    }
}