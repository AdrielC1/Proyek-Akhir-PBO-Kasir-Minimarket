package com.siritel.view.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

/**
 * Tombol rounded standar aplikasi SI-RITEL dengan dukungan beberapa varian gaya,
 * efek hover/pressed, dan status disabled yang tetap jelas terbaca.
 */
public class RoundedButton extends JButton {
    private ButtonVariant variant;
    private final int radius;
    private final boolean solidDisabledPrimary;
    private boolean isHovered = false;
    private boolean isPressed = false;

    /**
     * Konstruktor default ukuran tombol utama form login (biru solid lebar penuh kartu).
     */
    public RoundedButton(String text) {
        this(text, ButtonVariant.PRIMARY, UITheme.BUTTON_RADIUS, true);
        setFont(UITheme.buttonFont());
        setPreferredSize(new Dimension(UITheme.CONTENT_WIDTH, UITheme.BUTTON_HEIGHT));
    }

    public RoundedButton(String text, ButtonVariant variant) {
        this(text, variant, UITheme.BUTTON_RADIUS, false);
    }

    public RoundedButton(String text, ButtonVariant variant, int radius) {
        this(text, variant, radius, false);
    }

    private RoundedButton(String text, ButtonVariant variant, int radius, boolean solidDisabledPrimary) {
        super(text);
        this.variant = variant;
        this.radius = radius;
        this.solidDisabledPrimary = solidDisabledPrimary;

        setFont(UITheme.font(variant == ButtonVariant.OUTLINE ? Font.PLAIN : Font.BOLD, 12f));
        setForeground(variant == ButtonVariant.PRIMARY || variant == ButtonVariant.TEAL || variant == ButtonVariant.DANGER
                ? Color.WHITE
                : UITheme.TEXT_LABEL);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
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

    public void setVariant(ButtonVariant variant) {
        if (variant != null && this.variant != variant) {
            this.variant = variant;
            setForeground(variant == ButtonVariant.PRIMARY || variant == ButtonVariant.TEAL || variant == ButtonVariant.DANGER
                    ? Color.WHITE
                    : (variant == ButtonVariant.SOFT_PRIMARY ? UITheme.PRIMARY_BLUE : UITheme.TEXT_LABEL));
            repaint();
        }
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
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color bg;
        Color border = null;
        Color fg;

        if (!isEnabled()) {
            if (solidDisabledPrimary) {
                bg = UITheme.PRIMARY_DISABLED;
                fg = Color.WHITE;
            } else {
                bg = UITheme.BG_HOVER;
                border = UITheme.BORDER_DEFAULT;
                fg = UITheme.TEXT_MUTED;
            }
        } else {
            switch (variant) {
                case PRIMARY -> {
                    bg = isPressed ? UITheme.PRIMARY_PRESSED
                            : isHovered ? UITheme.PRIMARY_HOVER : UITheme.PRIMARY_BLUE;
                    fg = Color.WHITE;
                }
                case SOFT_PRIMARY -> {
                    bg = isPressed ? UITheme.PRIMARY_SOFT_BORDER
                            : isHovered ? UITheme.BADGE_BLUE_BG : UITheme.PRIMARY_SOFT_BG;
                    border = isHovered ? UITheme.PRIMARY_BLUE : UITheme.PRIMARY_SOFT_BORDER;
                    fg = UITheme.PRIMARY_BLUE;
                }
                case TEAL -> {
                    bg = isPressed ? UITheme.TEAL_PRESSED
                            : isHovered ? UITheme.TEAL_HOVER : UITheme.TEAL;
                    fg = Color.WHITE;
                }
                case DANGER -> {
                    bg = isPressed ? UITheme.DANGER_PRESSED
                            : isHovered ? UITheme.DANGER_HOVER : UITheme.DANGER;
                    fg = Color.WHITE;
                }
                case DANGER_OUTLINE -> {
                    bg = isPressed ? UITheme.DANGER_SOFT_BG
                            : isHovered ? UITheme.ALERT_BG : UITheme.CARD_BG;
                    border = isHovered ? UITheme.ALERT_ICON : UITheme.DANGER_BORDER_LIGHT;
                    fg = UITheme.DANGER;
                }
                case GHOST -> {
                    bg = isPressed ? UITheme.BORDER_DEFAULT
                            : isHovered ? UITheme.BG_HOVER : new Color(0, 0, 0, 0);
                    fg = isHovered ? UITheme.TEXT_MAIN : UITheme.TEXT_MUTED;
                }
                default -> {
                    bg = isPressed ? UITheme.BG_HOVER
                            : isHovered ? UITheme.BG_PAGE : UITheme.CARD_BG;
                    border = isHovered ? UITheme.BORDER_HOVER : UITheme.BORDER_INPUT;
                    fg = isHovered ? UITheme.TEXT_MAIN : UITheme.TEXT_LABEL;
                }
            }
        }

        int w = getWidth();
        int h = getHeight();
        if (bg.getAlpha() > 0) {
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);
        }
        if (border != null) {
            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
        }

        String text = getText();
        if (text != null && !text.isEmpty()) {
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(text)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(fg);
            g2.drawString(text, tx, ty);
        }

        g2.dispose();
    }
}
