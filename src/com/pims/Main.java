package com.pims;

import com.pims.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main - entry point of the Pharmacy Inventory Management System (PIMS).
 */
public class Main {

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread (good Swing practice)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // Use the standard look and feel of the operating system
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new LoginFrame();
            }
        });
    }
}