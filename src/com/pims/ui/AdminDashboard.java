package com.pims.ui;

import com.pims.db.DatabaseConnection;
import com.pims.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * AdminDashboard - the main window for the Administrator role.
 * Contains tabs for managing medicines, suppliers, users, and reports.
 */
public class AdminDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    private User loggedInUser;

    // References to the tab panels so we can refresh them
    private MedicineManager medicineManager;
    private SupplierManager supplierManager;
    private UserManager userManager;
    private SalesReport salesReport;
    private ExpiryReport expiryReport;

    // Card-based navigation (replaces the old tabbed layout)
    private CardLayout cardLayout;
    private JPanel contentPanel;

    // Overview tab components
    private JTable lowStockTable;
    private DefaultTableModel lowStockModel;
    private JLabel lblDashboardInfo;
    private Map<String, JLabel> statValueLabels = new HashMap<>();

    public AdminDashboard(User user) {
        // ACCESS CONTROL: only Administrators may open this dashboard
        if (user == null || !"Admin".equalsIgnoreCase(user.getRole())) {
            JOptionPane.showMessageDialog(null,
                    "Access denied: Administrator privileges required.",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            new LoginFrame();
            dispose();
            return;
        }
        this.loggedInUser = user;

        setTitle("HealthFirst Pharmacy - Administrator Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        medicineManager = new MedicineManager();
        supplierManager = new SupplierManager();
        userManager = new UserManager(loggedInUser);
        salesReport = new SalesReport();
        expiryReport = new ExpiryReport();

        getContentPane().setLayout(new BorderLayout(5, 5));
        getContentPane().add(createSidebar(), BorderLayout.WEST);
        getContentPane().add(createContentPanel(), BorderLayout.CENTER);

        setVisible(true);
    }

    /**
     * Content area switched by the sidebar buttons (CardLayout).
     */
    private JPanel createContentPanel() {
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        contentPanel.add(createOverviewPanel(), "dashboard");
        contentPanel.add(medicineManager, "medicines");
        contentPanel.add(supplierManager, "suppliers");
        contentPanel.add(userManager, "users");

        // The Reporting panel holds the Sales Report and Expiry Report inside it
        JTabbedPane reportingTabs = new JTabbedPane();
        reportingTabs.addTab("Sales Report", salesReport);
        reportingTabs.addTab("Expiry Report", expiryReport);
        contentPanel.add(reportingTabs, "reporting");

        cardLayout.show(contentPanel, "dashboard");
        return contentPanel;
    }

    /**
     * Sidebar navigation with 5 section buttons and a Logout button.
     */
    private JPanel createSidebar() {
        JPanel side = new JPanel(new GridLayout(6, 1, 0, 8));
        side.setPreferredSize(new Dimension(200, 0));
        side.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        side.setBackground(new Color(40, 63, 84));

        side.add(createNavButton("Dashboard", "dashboard"));
        side.add(createNavButton("Manage Medicines", "medicines"));
        side.add(createNavButton("Manage Suppliers", "suppliers"));
        side.add(createNavButton("Manage Users", "users"));
        side.add(createNavButton("Reporting", "reporting"));
        side.add(createNavButton("Logout", "logout"));

        return side;
    }

    private JButton createNavButton(String label, String card) {
        JButton btn = new JButton(label);
        btn.setFocusPainted(false);
        btn.setForeground(Color.BLACK);
        btn.setBackground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.addActionListener(e -> {
            if ("logout".equals(card)) {
                logout();
            } else {
                cardLayout.show(contentPanel, card);
            }
        });
        return btn;
    }

    /**
     * Overview panel of the Admin Dashboard: quick stats + low stock alert table.
     */
    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Stats cards
        JPanel stats = new JPanel(new GridLayout(1, 4, 10, 10));
        stats.add(createStatCard("Medicines"));
        stats.add(createStatCard("Sales Today"));
        stats.add(createStatCard("Total Sales (R)"));
        stats.add(createStatCard("Critical Stock"));
        panel.add(stats, BorderLayout.NORTH);

        // Low stock table
        lowStockModel = new DefaultTableModel(new String[]{
                "Medicine", "Stock Left", "Reorder Level", "Expiry Date"
        }, 0);
        lowStockTable = new JTable(lowStockModel);
        JScrollPane scroll = new JScrollPane(lowStockTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Low Stock / Reorder Alerts"));

        JPanel centre = new JPanel(new BorderLayout());
        lblDashboardInfo = new JLabel(" ");
        lblDashboardInfo.setForeground(Color.BLACK);
        lblDashboardInfo.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        centre.add(lblDashboardInfo, BorderLayout.NORTH);
        centre.add(scroll, BorderLayout.CENTER);

        panel.add(centre, BorderLayout.CENTER);

        loadOverviewStats();
        return panel;
    }

    private JPanel createStatCard(String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.PLAIN, 13));
        lblTitle.setForeground(Color.BLACK);
        JLabel lblValue = new JLabel("...", SwingConstants.CENTER);
        lblValue.setFont(new Font("Arial", Font.BOLD, 24));
        lblValue.setForeground(Color.BLACK);
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        statValueLabels.put(title, lblValue);
        return card;
    }

    /**
     * Loads quick statistics for the overview tab.
     */
    private void loadOverviewStats() {
        String query = "SELECT "
                + "(SELECT COUNT(*) FROM medicines) AS total_medicines, "
                + "(SELECT COUNT(*) FROM sales WHERE DATE(sale_date) = CURDATE()) AS sales_today, "
                + "(SELECT COALESCE(SUM(total_amount), 0) FROM sales) AS total_sales, "
                + "(SELECT COUNT(*) FROM medicines WHERE quantity_in_stock <= reorder_level) AS critical_stock";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                statValueLabels.get("Medicines").setText(String.valueOf(rs.getInt("total_medicines")));
                statValueLabels.get("Sales Today").setText(String.valueOf(rs.getInt("sales_today")));
                statValueLabels.get("Total Sales (R)").setText(String.format("R%.2f", rs.getDouble("total_sales")));
                statValueLabels.get("Critical Stock").setText(String.valueOf(rs.getInt("critical_stock")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Low stock items
        String lowQuery = "SELECT name, quantity_in_stock, reorder_level, expiry_date "
                + "FROM medicines WHERE quantity_in_stock <= reorder_level "
                + "ORDER BY quantity_in_stock ASC";

        lowStockModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(lowQuery)) {

            while (rs.next()) {
                lowStockModel.addRow(new Object[]{
                        rs.getString("name"),
                        rs.getInt("quantity_in_stock"),
                        rs.getInt("reorder_level"),
                        rs.getDate("expiry_date") == null ? "" : rs.getDate("expiry_date").toString()
                });
            }
            lblDashboardInfo.setText("Today's date: " + LocalDate.now()
                    + "   |   Low stock items: " + lowStockModel.getRowCount()
                    + "   |   Logged in as: " + loggedInUser.getRole());
        } catch (Exception e) {
            e.printStackTrace();
        }
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