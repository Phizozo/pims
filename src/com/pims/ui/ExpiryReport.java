package com.pims.ui;

import com.pims.db.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

/**
 * ExpiryReport - Administrator module.
 * Shows medicines that have already expired as well as those expiring
 * within the next 30 days (one month).
 */
public class ExpiryReport extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblSummary;
    private JComboBox<String> cmbFilter;

    public ExpiryReport() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Filter bar
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Expiry Report"));

        cmbFilter = new JComboBox<>(new String[]{
                "Expiring in the next 30 days",
                "Already Expired",
                "All medicines"
        });
        JButton btnRefresh = new JButton("Refresh");
        JButton btnPrint = new JButton("Print");

        filterPanel.add(new JLabel("Show:"));
        filterPanel.add(cmbFilter);
        filterPanel.add(btnRefresh);
        filterPanel.add(btnPrint);

        lblSummary = new JLabel(" ");
        lblSummary.setFont(new Font("Arial", Font.BOLD, 14));
        lblSummary.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Table
        tableModel = new DefaultTableModel(new String[]{
                "ID", "Medicine", "Company", "Stock", "Expiry Date", "Days Remaining"
        }, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Medicines"));

        add(filterPanel, BorderLayout.NORTH);
        add(lblSummary, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadReport());
        btnPrint.addActionListener(e -> PrintUtility.printTable(this, table, "HealthFirst Pharmacy - Expiry Report"));

        loadReport();
    }

    /**
     * Loads the report based on the selected filter option.
     */
    private void loadReport() {
        tableModel.setRowCount(0);
        int option = cmbFilter.getSelectedIndex();

        LocalDate today = LocalDate.now();
        LocalDate monthLater = today.plusMonths(1);

        String query = "SELECT m.medicine_id, m.name, m.company, m.quantity_in_stock, m.expiry_date "
                     + "FROM medicines m WHERE m.expiry_date IS NOT NULL ";

        if (option == 0) {
            query += "AND m.expiry_date BETWEEN ? AND ? ";
        } else if (option == 1) {
            query += "AND m.expiry_date < ? ";
        }
        query += "ORDER BY m.expiry_date";

        int count = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (option == 0) {
                stmt.setDate(1, Date.valueOf(today));
                stmt.setDate(2, Date.valueOf(monthLater));
            } else if (option == 1) {
                stmt.setDate(1, Date.valueOf(today));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate expiry = rs.getDate("expiry_date").toLocalDate();
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, expiry);

                    tableModel.addRow(new Object[]{
                            rs.getInt("medicine_id"),
                            rs.getString("name"),
                            rs.getString("company"),
                            rs.getInt("quantity_in_stock"),
                            expiry.toString(),
                            (days < 0 ? "EXPIRED " + Math.abs(days) + " days ago" : days + " days")
                    });
                    count++;
                }
            }

            switch (option) {
                case 0:
                    lblSummary.setText(count + " medicine(s) expiring within the next 30 days.");
                    break;
                case 1:
                    lblSummary.setText(count + " medicine(s) already expired.");
                    break;
                default:
                    lblSummary.setText(count + " medicine(s) with an expiry date.");
                    break;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading expiry report: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void refresh() {
        loadReport();
    }
}