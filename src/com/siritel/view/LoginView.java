package com.siritel.view;

import com.siritel.controller.AuthController;
import com.siritel.model.Pengguna;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Jendela Login SI-RITEL.
 */
public class LoginView extends JFrame {
    private final JTextField txtUsername = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JButton btnLogin = new JButton("Login");
    private final AuthController authController = new AuthController();

    public LoginView() {
        setTitle("SI-RITEL — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 360);
        setMinimumSize(new Dimension(380, 320));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBackground(new Color(241, 245, 249));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(24, 28, 28, 28)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Header Aplikasi
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel lblTitle = new JLabel("SI-RITEL", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitle.setForeground(new Color(30, 41, 59));
        card.add(lblTitle, gbc);

        gbc.gridy = 1;
        JLabel lblSubtitle = new JLabel("Sistem Informasi Kasir & Inventaris", SwingConstants.CENTER);
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(100, 116, 139));
        card.add(lblSubtitle, gbc);

        // Input Username
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(new Font("SansSerif", Font.BOLD, 13));
        card.add(lblUsername, gbc);

        gbc.gridx = 1;
        txtUsername.setFont(new Font("SansSerif", Font.PLAIN, 13));
        card.add(txtUsername, gbc);

        // Input Password
        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(new Font("SansSerif", Font.BOLD, 13));
        card.add(lblPassword, gbc);

        gbc.gridx = 1;
        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
        card.add(txtPassword, gbc);

        // Tombol Login
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 6, 6, 6);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.setBackground(new Color(37, 99, 235));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setPreferredSize(new Dimension(100, 36));
        card.add(btnLogin, gbc);

        rootPanel.add(card);
        add(rootPanel, BorderLayout.CENTER);

        // Jadikan btnLogin sebagai default button sehingga tombol Enter langsung memicu login
        getRootPane().setDefaultButton(btnLogin);

        // Action Listener Tombol Login
        btnLogin.addActionListener(e -> {
            String username = txtUsername.getText();
            String password = new String(txtPassword.getPassword());

            // FASE-1: tanpa penanganan eksepsi
            try {
                Pengguna user = authController.login(username, password);
                // FASE-1: tanpa penanganan eksepsi
                dispose();
                new MainFrame(user).setVisible(true);
            } catch (SQLException ex) {
                // FASE-1: tanpa penanganan eksepsi
                ex.printStackTrace();
            }
        });
    }
}
