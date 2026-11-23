package com.siritel.view.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;

/**
 * Panel notifikasi error inline dengan pembungkusan teks otomatis agar pesan error panjang
 * tetap terbaca penuh tanpa terpotong elipsis.
 */
public class InlineAlertPanel extends JPanel {
    private static final int MIN_TARGET_HEIGHT = UITheme.BANNER_HEIGHT; // 40 px
    private static final int ANIMATION_STEP_MS = 15;
    private static final int STEP_DELTA = 6;

    private final JTextArea txtMessage = new JTextArea();
    private int currentHeight = 0;
    private int targetHeight = 0;
    private Timer heightTimer;

    public InlineAlertPanel() {
        setLayout(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        setPreferredSize(new Dimension(UITheme.CONTENT_WIDTH, 0));
        setVisible(false);

        getAccessibleContext().setAccessibleName("Peringatan Login");

        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int d = 16;
                int x = (getWidth() - d) / 2;
                int y = (getHeight() - d) / 2;

                g2.setColor(UITheme.ALERT_TEXT);
                g2.drawOval(x, y, d, d);
                g2.fillRect(x + (d / 2) - 1, y + 4, 2, 5);
                g2.fillRect(x + (d / 2) - 1, y + 11, 2, 2);

                g2.dispose();
            }
        };
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(18, 18));

        JPanel iconWrap = new JPanel(new GridBagLayout());
        iconWrap.setOpaque(false);
        iconWrap.add(iconPanel);

        txtMessage.setEditable(false);
        txtMessage.setFocusable(false);
        txtMessage.setOpaque(false);
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);
        txtMessage.setFont(UITheme.alertFont());
        txtMessage.setForeground(UITheme.ALERT_TEXT);
        txtMessage.setBorder(null);

        JPanel msgWrap = new JPanel(new GridBagLayout());
        msgWrap.setOpaque(false);
        java.awt.GridBagConstraints mg = new java.awt.GridBagConstraints();
        mg.gridx = 0;
        mg.gridy = 0;
        mg.weightx = 1.0;
        mg.fill = java.awt.GridBagConstraints.HORIZONTAL;
        msgWrap.add(txtMessage, mg);

        add(iconWrap, BorderLayout.WEST);
        add(msgWrap, BorderLayout.CENTER);
    }

    /**
     * Menampilkan pesan error dengan teks lengkap dan tinggi dinamis mengikuti panjang pesan.
     */
    public void showAlert(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = "Username atau password salah.";
        }

        txtMessage.setText(message);
        int textWidth = Math.max(200, UITheme.CONTENT_WIDTH - 52);
        txtMessage.setSize(new Dimension(textWidth, Short.MAX_VALUE));
        int textPrefH = txtMessage.getPreferredSize().height;
        targetHeight = Math.max(MIN_TARGET_HEIGHT, textPrefH + 18);
        getAccessibleContext().setAccessibleDescription(message);

        setVisible(true);

        if (heightTimer != null && heightTimer.isRunning()) {
            heightTimer.stop();
        }

        heightTimer = new Timer(ANIMATION_STEP_MS, e -> {
            if (currentHeight < targetHeight) {
                currentHeight = Math.min(targetHeight, currentHeight + STEP_DELTA);
                setPreferredSize(new Dimension(UITheme.CONTENT_WIDTH, currentHeight));
                revalidate();
                repaint();
            } else if (currentHeight > targetHeight) {
                currentHeight = Math.max(targetHeight, currentHeight - STEP_DELTA);
                setPreferredSize(new Dimension(UITheme.CONTENT_WIDTH, currentHeight));
                revalidate();
                repaint();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        heightTimer.start();

        currentHeight = Math.min(targetHeight, Math.max(currentHeight, STEP_DELTA * 2));
        setPreferredSize(new Dimension(UITheme.CONTENT_WIDTH, currentHeight));
        revalidate();
        repaint();
    }

    /**
     * Menyembunyikan banner error dengan transisi tinggi ke 0 px.
     */
    public void hideAlert() {
        targetHeight = 0;
        if (heightTimer != null && heightTimer.isRunning()) {
            heightTimer.stop();
        }

        heightTimer = new Timer(ANIMATION_STEP_MS, e -> {
            if (currentHeight > 0) {
                currentHeight = Math.max(0, currentHeight - STEP_DELTA);
                setPreferredSize(new Dimension(UITheme.CONTENT_WIDTH, currentHeight));
                revalidate();
                repaint();
            } else {
                setVisible(false);
                ((Timer) e.getSource()).stop();
            }
        });
        heightTimer.start();
    }

    public boolean isAlertVisible() {
        return targetHeight > 0 || isVisible();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (currentHeight <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        g2.setColor(UITheme.ALERT_BG);
        g2.fillRoundRect(0, 0, w - 1, h - 1, UITheme.BANNER_RADIUS, UITheme.BANNER_RADIUS);

        g2.setColor(UITheme.ALERT_BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, UITheme.BANNER_RADIUS, UITheme.BANNER_RADIUS);

        g2.dispose();
        super.paintComponent(g);
    }
}
