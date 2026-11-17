package com.siritel.view.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Panel notifikasi error inline yang presisi sesuai spesifikasi login.png.
 * Menggunakan JLabel asli untuk keterbacaan, seleksi, dan aksesibilitas screen reader.
 * Animasi transisi tinggi halus (0 -> 40 px) tanpa efek alpha yang mengaburkan teks.
 */
public class InlineAlertPanel extends JPanel {
    private static final int TARGET_HEIGHT = UITheme.BANNER_HEIGHT; // 40 px
    private static final int ANIMATION_STEP_MS = 15;
    private static final int STEP_DELTA = 4; // 10 langkah (~150 ms)

    private final JLabel lblMessage = new JLabel();
    private final JPanel iconPanel;
    private int currentHeight = 0;
    private int targetHeight = 0;
    private Timer heightTimer;

    public InlineAlertPanel() {
        setLayout(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        setPreferredSize(new Dimension(UITheme.CONTENT_WIDTH, 0));
        setVisible(false);

        getAccessibleContext().setAccessibleName("Peringatan Login");

        // Ikon bulat tanda seru (!) 18 px presisi sesuai target login.png
        iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int d = 16;
                int x = (getWidth() - d) / 2;
                int y = (getHeight() - d) / 2;

                // Lingkaran garis merah bulat
                g2.setColor(UITheme.ALERT_TEXT);
                g2.drawOval(x, y, d, d);

                // Garis tanda seru (!)
                g2.fillRect(x + (d / 2) - 1, y + 4, 2, 5);
                // Titik tanda seru
                g2.fillRect(x + (d / 2) - 1, y + 11, 2, 2);

                g2.dispose();
            }
        };
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(18, 18));

        lblMessage.setFont(UITheme.alertFont());
        lblMessage.setForeground(UITheme.ALERT_TEXT);

        add(iconPanel, BorderLayout.WEST);
        add(lblMessage, BorderLayout.CENTER);
    }

    /**
     * Menampilkan pesan error dengan teks lengkap seketika (tidak pernah kosong).
     */
    public void showAlert(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = "Username atau password salah.";
        }

        // 1. Set teks SEBELUM banner membesar, pastikan langsung terbaca
        lblMessage.setText(message);
        getAccessibleContext().setAccessibleDescription(message);

        targetHeight = TARGET_HEIGHT;
        setVisible(true);

        if (heightTimer != null && heightTimer.isRunning()) {
            heightTimer.stop();
        }

        // 2. Animasi tinggi 0 -> 40 px dalam ~150 ms
        heightTimer = new Timer(ANIMATION_STEP_MS, e -> {
            if (currentHeight < targetHeight) {
                currentHeight = Math.min(targetHeight, currentHeight + STEP_DELTA);
                setPreferredSize(new Dimension(UITheme.CONTENT_WIDTH, currentHeight));
                revalidate();
                repaint();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        heightTimer.start();

        // Render awal seketika agar teks langsung muncul
        currentHeight = Math.max(currentHeight, STEP_DELTA * 2);
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

    public String getAlertText() {
        return lblMessage.getText();
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

        // Background merah sangat muda (#FEF2F2)
        g2.setColor(UITheme.ALERT_BG);
        g2.fillRoundRect(0, 0, w - 1, h - 1, UITheme.BANNER_RADIUS, UITheme.BANNER_RADIUS);

        // Border merah muda 1 px (#FECACA)
        g2.setColor(UITheme.ALERT_BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, UITheme.BANNER_RADIUS, UITheme.BANNER_RADIUS);

        g2.dispose();
        super.paintComponent(g);
    }
}
