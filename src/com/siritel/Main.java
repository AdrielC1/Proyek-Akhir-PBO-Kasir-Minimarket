package com.siritel;

import com.siritel.view.LoginView;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point aplikasi desktop SI-RITEL.
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Gunakan Look & Feel bawaan Swing jika sistem tidak mendukung.
        }

        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView();
            loginView.setVisible(true);
        });
    }
}
