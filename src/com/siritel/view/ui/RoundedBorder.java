package com.siritel.view.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.border.AbstractBorder;

/**
 * Border melengkung anti-aliased standar untuk komponen input, kontainer tabel, dan panel.
 */
public class RoundedBorder extends AbstractBorder {
    private final Color color;
    private final int radius;
    private final Insets padding;

    public RoundedBorder(Color color, int radius, Insets padding) {
        this.color = color;
        this.radius = radius;
        this.padding = padding;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(padding.top, padding.left, padding.bottom, padding.right);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(padding.top, padding.left, padding.bottom, padding.right);
        return insets;
    }
}
