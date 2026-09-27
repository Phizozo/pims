package com.pims.ui;

import com.pims.db.DatabaseConnection;
import com.pims.model.User;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextField txtUsername;
    private JPasswordField txtPassword;

    public LoginFrame() {
        // Basic window settings
        setTitle("HealthFirst Pharmacy - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 300);
        setLocationRelativeTo(null);          // centre the window
        setResizable(false);

        buildUI();
        setVisible(true);
    }

    private void buildUI() {
        // Main panel
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Heading
        JLabel heading = new JLabel("HealthFirst Pharmacy", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 22));

        JPanel titlePanel = new JPanel(new GridLayout(1, 1));
        titlePanel.add(heading);

        // Form fields
        JLabel lblUser = new JLabel("Username:");
        JLabel lblPass = new JLabel("Password:");
        txtUsername = new JTextField(15);
        txtPassword = new JPasswordField(15);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(lblUser, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        formPanel.add(txtUsername, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(lblPass, gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        formPanel.add(txtPassword, gbc);

        // Login button
        JButton btnLogin = new JButton("Login");
        btnLogin.setPreferredSize(new Dimension(120, 35));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnLogin);

        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        getContentPane().add(panel);

        // Button action
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });

        // Enter key in the password field also triggers login
        txtPassword.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });
    }

    /**
     * Validates the login details and opens the correct dashboard.
     */
    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.",
                    "Login Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = authenticate(username, password);

        if (user == null) {
            JOptionPane.showMessageDialog(this, "Invalid username or password.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            txtPassword.setText("");
            txtUsername.requestFocus();
            return;
        }

        // Successful login - redirect based on role
        String welcome = "Admin".equalsIgnoreCase(user.getRole())
                ? "Welcome, Administrator!"
                : "Welcome, Cashier!";
        JOptionPane.showMessageDialog(this, welcome,
                "Login Successful", JOptionPane.INFORMATION_MESSAGE);

        if ("Admin".equalsIgnoreCase(user.getRole())) {
            new AdminDashboard(user);
        } else {
            new CashierDashboard(user);
        }
        dispose(); // close the login window
    }

    /**
     * Queries the database for a matching username + password.
     * Returns a User object or null if not found.
     */
    private User authenticate(String username, String password) {
        String query = "SELECT user_id, username, role, full_name FROM users "
                     + "WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setRole(rs.getString("role"));
                    user.setFullName(rs.getString("full_name"));
                    return user;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Database error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }
}