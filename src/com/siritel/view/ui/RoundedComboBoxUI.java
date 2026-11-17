package com.siritel.view.ui;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

/**
 * UI kustom untuk JComboBox standar SI-RITEL agar sudut membulat, chevron rapi,
 * dan bebas garis fokus kotak Win32.
 */
public class RoundedComboBoxUI extends BasicComboBoxUI {
    private final int radius;
    private boolean hovered = false;

    public RoundedComboBoxUI(int radius) {
        this.radius = radius;
    }

    public static void apply(JComboBox<?> combo, int radius) {
        combo.setFont(UITheme.font(Font.PLAIN, 12f));
        combo.setBackground(UITheme.CARD_BG);
        combo.setForeground(UITheme.TEXT_MAIN);
        combo.setOpaque(false);
        combo.setUI(new RoundedComboBoxUI(radius));
        combo.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 4));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        comboBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                comboBox.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                comboBox.repaint();
            }
        });
    }

    @Override
    protected JButton createArrowButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                boolean focused = comboBox != null && (comboBox.hasFocus() || (popup != null && popup.isVisible()));
                g2.setColor(focused ? UITheme.PRIMARY_BLUE : UITheme.TEXT_MUTED);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(
                        new int[]{cx - 4, cx, cx + 4},
                        new int[]{cy - 2, cy + 2, cy - 2},
                        3
                );
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup comboPopup = (BasicComboPopup) super.createPopup();
        comboPopup.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_DEFAULT, 1));
        return comboPopup;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = c.getWidth();
        int h = c.getHeight();

        g2.setColor(c.isEnabled() ? UITheme.CARD_BG : UITheme.BG_PAGE);
        g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);
        g2.dispose();

        super.paint(g, c);

        Graphics2D gBorder = (Graphics2D) g.create();
        gBorder.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean active = c.hasFocus() || (popup != null && popup.isVisible());
        gBorder.setColor(active ? UITheme.PRIMARY_BLUE : (hovered ? UITheme.BORDER_HOVER : UITheme.BORDER_INPUT));
        gBorder.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
        gBorder.dispose();
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        // Tidak menggambar latar kotak fokus default agar tampilan melengkung tetap bersih
    }

    @Override
    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        Component c = comboBox.getRenderer().getListCellRendererComponent(
                listBox, comboBox.getSelectedItem(), -1, false, false);
        c.setFont(comboBox.getFont());
        c.setForeground(comboBox.isEnabled() ? UITheme.TEXT_MAIN : UITheme.TEXT_MUTED);
        c.setBackground(UITheme.CARD_BG);
        boolean shouldValidate = c instanceof JPanel;
        currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, shouldValidate);
    }
}
