package com.siritel.view.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/**
 * Password field rounded dengan ikon gembok (lock) 18 px pada x=12,
 * teks mulai pada x=40, dan ikon toggle mata di sisi kanan (jarak 12 px).
 * Disesuaikan presisi dengan dimensi target login.png.
 */
public class RoundedPasswordField extends JPanel {
    private static final int ICON_X = 12;
    private static final int TEXT_LEFT_MARGIN = 40;

    private final JPasswordField passwordField = new JPasswordField();
    private final JButton btnToggleEye = new JButton();

    private final char defaultEchoChar;
    private boolean isPasswordVisible = false;
    private boolean isFocused = false;
    private boolean isError = false;

    public RoundedPasswordField() {
        setLayout(new BorderLayout(4, 0));
        setOpaque(false);
        setPreferredSize(new Dimension(UITheme.CONTENT_WIDTH, UITheme.FIELD_HEIGHT));

        defaultEchoChar = passwordField.getEchoChar();

        // Konfigurasi internal JPasswordField
        passwordField.setFont(UITheme.inputFont());
        passwordField.setForeground(UITheme.TEXT_MAIN);
        passwordField.setCaretColor(UITheme.PRIMARY_BLUE);
        passwordField.setOpaque(false);
        passwordField.setBorder(BorderFactory.createEmptyBorder(0, TEXT_LEFT_MARGIN, 0, 4));

        passwordField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });

        // Tombol mata (Java2D eye icon)
        btnToggleEye.setPreferredSize(new Dimension(36, UITheme.FIELD_HEIGHT));
        btnToggleEye.setFocusable(false);
        btnToggleEye.setContentAreaFilled(false);
        btnToggleEye.setBorderPainted(false);
        btnToggleEye.setFocusPainted(false);
        btnToggleEye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggleEye.setToolTipText("Tampilkan Password");

        btnToggleEye.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                passwordField.setEchoChar((char) 0);
                btnToggleEye.setToolTipText("Sembunyikan Password");
            } else {
                passwordField.setEchoChar(defaultEchoChar);
                btnToggleEye.setToolTipText("Tampilkan Password");
            }
            RoundedPasswordField.this.repaint();
            passwordField.requestFocusInWindow();
        });

        add(passwordField, BorderLayout.CENTER);
        add(btnToggleEye, BorderLayout.EAST);
    }

    public JPasswordField getInternalField() {
        return passwordField;
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }

    public void setError(boolean error) {
        if (this.isError != error) {
            this.isError = error;
            repaint();
        }
    }

    public boolean isError() {
        return isError;
    }

    @Override
    public boolean requestFocusInWindow() {
        return passwordField.requestFocusInWindow();
    }

    public void selectAll() {
        passwordField.selectAll();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Gambar latar belakang putih rounded
        g2.setColor(UITheme.CARD_BG);
        g2.fillRoundRect(1, 1, w - 2, h - 2, UITheme.FIELD_RADIUS, UITheme.FIELD_RADIUS);

        // 2. Gambar border dan glow sesuai state
        if (isError) {
            g2.setColor(UITheme.GLOW_ERROR);
            g2.drawRoundRect(0, 0, w - 1, h - 1, UITheme.FIELD_RADIUS + 2, UITheme.FIELD_RADIUS + 2);
            g2.setColor(UITheme.BORDER_ERROR);
            g2.drawRoundRect(1, 1, w - 3, h - 3, UITheme.FIELD_RADIUS, UITheme.FIELD_RADIUS);
        } else if (isFocused) {
            g2.setColor(UITheme.GLOW_FOCUS);
            g2.drawRoundRect(0, 0, w - 1, h - 1, UITheme.FIELD_RADIUS + 2, UITheme.FIELD_RADIUS + 2);
            g2.setColor(UITheme.BORDER_FOCUS);
            g2.drawRoundRect(1, 1, w - 3, h - 3, UITheme.FIELD_RADIUS, UITheme.FIELD_RADIUS);
        } else {
            g2.setColor(UITheme.BORDER_DEFAULT);
            g2.drawRoundRect(1, 1, w - 3, h - 3, UITheme.FIELD_RADIUS, UITheme.FIELD_RADIUS);
        }

        // 3. Gambar ikon gembok pada x=12
        drawLockIcon(g2, ICON_X, h / 2);

        // 4. Gambar ikon mata pada area tombol kanan (x=w-24)
        drawEyeIcon(g2, w - 22, h / 2);

        g2.dispose();
    }

    private void drawLockIcon(Graphics2D g2, int startX, int centerY) {
        if (isError) {
            g2.setColor(UITheme.BORDER_ERROR);
        } else if (isFocused) {
            g2.setColor(UITheme.PRIMARY_BLUE);
        } else {
            g2.setColor(UITheme.TEXT_MUTED);
        }

        // Badan gembok (kotak 12 x 9 px)
        int bodyW = 12;
        int bodyH = 9;
        int bodyX = startX + 3;
        int bodyY = centerY - 1;
        g2.drawRoundRect(bodyX, bodyY, bodyW, bodyH, 2, 2);

        // Gagang lengkung gembok (lebar 8 px, tinggi 7 px)
        int shackleW = 8;
        int shackleH = 7;
        int shackleX = startX + 5;
        int shackleY = centerY - 7;
        g2.drawArc(shackleX, shackleY, shackleW, shackleH * 2, 0, 180);

        // Titik lubang kunci
        g2.fillOval(startX + 8, centerY + 2, 2, 3);
    }

    private void drawEyeIcon(Graphics2D g2, int centerX, int centerY) {
        g2.setColor(isPasswordVisible ? UITheme.PRIMARY_BLUE : UITheme.TEXT_MUTED);

        // Kontur kelopak mata
        g2.drawOval(centerX - 7, centerY - 4, 14, 8);
        // Pupil mata
        g2.fillOval(centerX - 2, centerY - 2, 4, 4);

        // Garis coret jika password sedang tampil
        if (isPasswordVisible) {
            g2.drawLine(centerX - 8, centerY + 5, centerX + 8, centerY - 5);
        }
    }
}
