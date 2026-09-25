package com.pims.ui;

import javax.swing.*;
import java.awt.print.PrinterException;
import java.text.MessageFormat;

/**
 * PrintUtility - a small helper that prints a JTable.
 */
public class PrintUtility {

    /**
     * Opens the print dialog and prints the given table.
     */
    public static void printTable(java.awt.Component parent, JTable table, String title) {
        try {
            boolean ok = table.print(JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat(title),
                    new MessageFormat("Page {0}"));
            if (!ok) {
                JOptionPane.showMessageDialog(parent, "Printing was cancelled.");
            }
        } catch (java.awt.print.PrinterAbortException e) {
            // user cancelled - fine
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(parent, "Could not print: " + e.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}