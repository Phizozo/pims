package com.pims.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection - a simple utility class that creates a connection
 * to the MySQL database used by the PIMS application.
 *
 * To use this project:
 *  1. Import database.sql into MySQL (workbench or command line).
 *  2. Change the USER and PASSWORD below to match your MySQL login.
 */
public class DatabaseConnection {

    // Change these to match your own MySQL setup
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/pims_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=10000&socketTimeout=30000";
    private static final String USER = "root";
    private static final String PASSWORD = "Phizozo.1@";

    /**
     * Returns an open connection to the pims_db database.
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC driver not found. Add mysql-connector-j jar to the project.");
            e.printStackTrace();
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}