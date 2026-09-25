package com.pims.ui;

import com.pims.db.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * SalesReport - Administrator module.
 * Displays a report of all sales, with the option to filter by date range,
 * plus a summary of total sales, number of transactions and items sold.
 */
public class SalesReport extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblSummary;

    private JTextField txtFromDate;
    private JTextField txtToDate;

    public SalesReport() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Filter bar
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filter by Date (yyyy-mm-dd)"));

        txtFromDate = new JTextField(10);
        txtToDate = new JTextField(10);
        JButton btnFilter = new JButton("Apply Filter");
        JButton btnAll = new JButton("Show All");

        filterPanel.add(new JLabel("From:"));
        filterPanel.add(txtFromDate);
        filterPanel.add(new JLabel("To:"));
        filterPanel.add(txtToDate);
        filterPanel.add(btnFilter);
        filterPanel.add(btnAll);

        // Summary label
        lblSummary = new JLabel(" ");
        lblSummary.setFont(new Font("Arial", Font.BOLD, 14));
        lblSummary.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Table
        tableModel = new DefaultTableModel(new String[]{
                "Sale ID", "Date", "Cashier", "Items", "Total Amount"
        }, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Sales Transactions"));

        add(filterPanel, BorderLayout.NORTH);
        add(lblSummary, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        btnFilter.addActionListener(e -> loadSales(true));
        btnAll.addActionListener(e -> {
            txtFromDate.setText("");
            txtToDate.setText("");
            loadSales(false);
        });

        loadSales(false);
    }

    /**
     * Loads all sales (or sales within a date range) into the table.
     */
private void loadSales(boolean filterByDate) {
        tableModel.setRowCount(0);

        boolean fromOnly = hasDate("from");
        boolean toOnly = hasDate("to");

        String query = "SELECT s.sale_id, s.sale_date, u.full_name AS cashier, "
                + "       COALESCE(SUM(si.quantity_sold), 0) AS items, s.total_amount "
                + "FROM sales s "
                + "LEFT JOIN users u ON s.user_id = u.user_id "
                + "LEFT JOIN sale_items si ON s.sale_id = si.sale_id ";

        if (filterByDate && (fromOnly || toOnly)) {
            if (fromOnly && toOnly) {
                query += "WHERE DATE(s.sale_date) BETWEEN ? AND ? ";
            } else if (fromOnly) {
                query += "WHERE DATE(s.sale_date) >= ? ";
            } else {
                query += "WHERE DATE(s.sale_date) <= ? ";
            }
        }
        query += "GROUP BY s.sale_id, s.sale_date, u.full_name, s.total_amount "
                + "ORDER BY s.sale_date DESC";

        double totalAll = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (filterByDate && fromOnly && toOnly) {
                stmt.setDate(1, Date.valueOf(txtFromDate.getText().trim()));
                stmt.setDate(2, Date.valueOf(txtToDate.getText().trim()));
            } else if (filterByDate && fromOnly) {
                stmt.setDate(1, Date.valueOf(txtFromDate.getText().trim()));
            } else if (filterByDate && toOnly) {
                stmt.setDate(1, Date.valueOf(txtToDate.getText().trim()));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    double amount = rs.getBigDecimal("total_amount").doubleValue();
                    totalAll += amount;
                    tableModel.addRow(new Object[]{
                            rs.getInt("sale_id"),
                            rs.getTimestamp("sale_date").toString(),
                            rs.getString("cashier"),
                            rs.getInt("items"),
                            String.format("R%.2f", amount)
                    });
                }
            }

            lblSummary.setText("Total sales: R" + String.format("%.2f", totalAll)
                    + "   |   Transactions: " + tableModel.getRowCount());

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Dates must be in format yyyy-mm-dd.",
                    "Invalid Date", JOptionPane.ERROR_MESSAGE);
            loadSales(false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading sales report: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private boolean hasDate(String which) {
        String value = (which.equals("from") ? txtFromDate : txtToDate).getText().trim();
        return !value.isEmpty();
    }

    /**
     * Called by the AdminDashboard refresh so the report updates with new sales.
     */
    public void refresh() {
        loadSales(false);
    }
}