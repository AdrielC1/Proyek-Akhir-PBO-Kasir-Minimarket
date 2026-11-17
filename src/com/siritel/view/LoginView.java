package com.siritel.view;

import com.siritel.controller.AuthController;
import com.siritel.model.Pengguna;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Jendela Login SI-RITEL dengan penanganan eksepsi lengkap (Fase 2).
 */
public class LoginView extends JFrame {
    private static final Color BG_PAGE = new Color(241, 245, 249);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color ICON_BOX_BG = new Color(239, 246, 255);
    private static final Color ICON_BOX_BORDER = new Color(219, 234, 254);

    private final JTextField txtUsername = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JButton btnLogin = new JButton("Masuk");
    private final char defaultEchoChar;
    private boolean isPasswordVisible = false;

    private final AuthController authController = new AuthController();

    public LoginView() {
        setTitle("SI-RITEL — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 520);
        setMinimumSize(new Dimension(420, 480));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        defaultEchoChar = txtPassword.getEchoChar();

        JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBackground(BG_PAGE);

        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(32, 32, 28, 32));
        card.setPreferredSize(new Dimension(380, 430));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);

        // 1. Icon Header Brand
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel iconBadge = createBrandIconBadge();
        card.add(iconBadge, gbc);

        // 2. Title SI-RITEL
        gbc.gridy = 1;
        gbc.insets = new Insets(12, 0, 2, 0);
        JLabel lblTitle = new JLabel("SI-RITEL", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblTitle.setForeground(TEXT_DARK);
        card.add(lblTitle, gbc);

        // 3. Subtitle
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        JLabel lblSubtitle = new JLabel("Sistem Informasi Kasir Ritel", SwingConstants.CENTER);
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSubtitle.setForeground(TEXT_MUTED);
        card.add(lblSubtitle, gbc);

        // 4. Label Username
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblUsername.setForeground(new Color(51, 65, 85));
        card.add(lblUsername, gbc);

        // 5. Input Field Username
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 14, 0);
        txtUsername.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        txtUsername.setPreferredSize(new Dimension(0, 38));
        card.add(txtUsername, gbc);

        // 6. Label Password
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 6, 0);
        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblPassword.setForeground(new Color(51, 65, 85));
        card.add(lblPassword, gbc);

        // 7. Input Field Password dengan Toggle Eye Java2D
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 20, 0);
        JPanel passPanel = new JPanel(new BorderLayout(4, 0));
        passPanel.setOpaque(false);
        passPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(2, 2, 2, 6)
        ));

        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtPassword.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 6));
        passPanel.add(txtPassword, BorderLayout.CENTER);

        JButton btnTogglePassword = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(100, 116, 139));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.drawOval(cx - 7, cy - 4, 14, 8);
                g2.fillOval(cx - 2, cy - 2, 4, 4);
                if (isPasswordVisible) {
                    g2.drawLine(cx - 8, cy + 5, cx + 8, cy - 5);
                }
                g2.dispose();
            }
        };
        btnTogglePassword.setPreferredSize(new Dimension(28, 28));
        btnTogglePassword.setFocusPainted(false);
        btnTogglePassword.setBorderPainted(false);
        btnTogglePassword.setContentAreaFilled(false);
        btnTogglePassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTogglePassword.setToolTipText("Tampilkan / Sembunyikan Password");
        btnTogglePassword.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                txtPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar(defaultEchoChar);
            }
            btnTogglePassword.repaint();
        });
        passPanel.add(btnTogglePassword, BorderLayout.EAST);
        card.add(passPanel, gbc);

        // 8. Tombol Masuk
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 14, 0);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setBackground(PRIMARY_BLUE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setPreferredSize(new Dimension(0, 40));
        card.add(btnLogin, gbc);

        // 9. Hint Footer
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel lblHint = new JLabel("Tekan Enter untuk masuk", SwingConstants.CENTER);
        lblHint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblHint.setForeground(new Color(148, 163, 184));
        card.add(lblHint, gbc);

        rootPanel.add(card);
        add(rootPanel, BorderLayout.CENTER);

        // Set default button ke Masuk agar tombol Enter langsung berfungsi
        getRootPane().setDefaultButton(btnLogin);

        // Action Listener Tombol Login dengan Penanganan Eksepsi Lengkap (Fase 2)
        btnLogin.addActionListener(e -> {
            String username = txtUsername.getText();
            String password = new String(txtPassword.getPassword());

            try {
                Pengguna user = authController.login(username, password);
                dispose();
                new MainFrame(user).setVisible(true);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "Kesalahan Login",
                        JOptionPane.ERROR_MESSAGE
                );
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        AuthController.pesanSqlError(ex),
                        "Kesalahan Database",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private JPanel createBrandIconBadge() {
        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ICON_BOX_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(ICON_BOX_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                // Ikon tas belanja Java2D sederhana
                g2.setColor(PRIMARY_BLUE);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.drawRoundRect(cx - 10, cy - 6, 20, 16, 4, 4);
                g2.drawArc(cx - 5, cy - 12, 10, 10, 0, 180);
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(48, 48));
        badge.setOpaque(false);
        return badge;
    }
}
