package com.siritel.view.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.BorderFactory;
import javax.swing.JTextField;

/**
 * Text field rounded dengan ikon pengguna (user) di sisi kiri yang digambar
 * langsung via Java2D, border abu muda 1 px, dan state fokus/error halus.
 * Disesuaikan presisi dengan dimensi target login.png.
 */
public class RoundedTextField extends JTextField {
    private static final int ICON_X = 12;
    private static final int ICON_SIZE = 18;
    private static final int TEXT_LEFT_MARGIN = 40;

    private boolean isFocused = false;
    private boolean isError = false;

    public RoundedTextField() {
        setFont(UITheme.inputFont());
        setForeground(UITheme.TEXT_MAIN);
        setCaretColor(UITheme.PRIMARY_BLUE);
        setOpaque(false);
        // Teks mulai tepat di 40 px dari tepi kiri sesuai spesifikasi target
        setBorder(BorderFactory.createEmptyBorder(0, TEXT_LEFT_MARGIN, 0, 12));
        setPreferredSize(new Dimension(UITheme.CONTENT_WIDTH, UITheme.FIELD_HEIGHT));

        addFocusListener(new FocusListener() {
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
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Gambar latar belakang putih rounded
        g2.setColor(UITheme.CARD_BG);
        g2.fillRoundRect(1, 1, w - 2, h - 2, UITheme.FIELD_RADIUS, UITheme.FIELD_RADIUS);

        // 2. Gambar glow & border sesuai state
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

        // 3. Gambar ikon user Java2D presisi 18 px pada x=12
        drawUserIcon(g2, ICON_X, h / 2);

        g2.dispose();
        super.paintComponent(g);
    }

    private void drawUserIcon(Graphics2D g2, int startX, int centerY) {
        if (isError) {
            g2.setColor(UITheme.BORDER_ERROR);
        } else if (isFocused) {
            g2.setColor(UITheme.PRIMARY_BLUE);
        } else {
            g2.setColor(UITheme.TEXT_MUTED);
        }

        // Kepala bulat (diameter 8 px)
        int headD = 8;
        int headX = startX + 5;
        int headY = centerY - 8;
        g2.drawOval(headX, headY, headD, headD);

        // Bahu / badan lengkung (lebar 14 px, tinggi 9 px)
        int bodyX = startX + 2;
        int bodyY = centerY + 1;
        g2.drawArc(bodyX, bodyY, 14, 10, 0, 180);
    }
}
