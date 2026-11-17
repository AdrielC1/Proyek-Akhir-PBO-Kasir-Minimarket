package com.siritel.view.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

/**
 * Tombol rounded modern berwarna biru solid sesuai spesifikasi login.png.
 * Mendukung status hover, pressed, dan disabled.
 */
public class RoundedButton extends JButton {

    private boolean isHovered = false;
    private boolean isPressed = false;

    public RoundedButton(String text) {
        super(text);
        setFont(UITheme.buttonFont());
        setForeground(java.awt.Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(UITheme.CONTENT_WIDTH, UITheme.BUTTON_HEIGHT));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(enabled
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (!isEnabled()) {
            g2.setColor(UITheme.PRIMARY_DISABLED);
        } else if (isPressed) {
            g2.setColor(UITheme.PRIMARY_PRESSED);
        } else if (isHovered) {
            g2.setColor(UITheme.PRIMARY_HOVER);
        } else {
            g2.setColor(UITheme.PRIMARY_BLUE);
        }

        g2.fillRoundRect(0, 0, w, h, UITheme.BUTTON_RADIUS, UITheme.BUTTON_RADIUS);
        g2.dispose();

        super.paintComponent(g);
    }
}
