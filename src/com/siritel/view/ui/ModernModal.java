package com.siritel.view.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

/**
 * Helper terpusat untuk dialog modal modern SI-RITEL (sudut membulat, ikon status,
 * tombol aksi standar, serta backdrop gelap redup pada jendela induk).
 */
public final class ModernModal {

    private ModernModal() {
    }

    public static boolean confirm(Component host, String title, String message, String confirmText) {
        Window owner = SwingUtilities.getWindowAncestor(host);
        if (owner == null) {
            return false;
        }
        final boolean[] confirmed = {false};
        JDialog dialog = createDialog(owner, title);

        JPanel card = buildCardShell(
                title,
                message,
                "WARNING",
                UITheme.DANGER_SOFT_BG,
                UITheme.DANGER,
                dialog::dispose
        );

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        RoundedButton btnCancel = new RoundedButton("Batal", ButtonVariant.OUTLINE, UITheme.BUTTON_RADIUS);
        btnCancel.setFont(UITheme.font(Font.PLAIN, 12f));
        btnCancel.setPreferredSize(new Dimension(82, 36));
        btnCancel.addActionListener(e -> dialog.dispose());

        RoundedButton btnOk = new RoundedButton(confirmText, ButtonVariant.DANGER, UITheme.BUTTON_RADIUS);
        btnOk.setFont(UITheme.font(Font.BOLD, 12f));
        btnOk.setPreferredSize(new Dimension(104, 36));
        btnOk.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });

        btnRow.add(btnCancel);
        btnRow.add(btnOk);
        card.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.getRootPane().setDefaultButton(btnOk);
        showWithBackdrop(owner, dialog, 380, 440);
        return confirmed[0];
    }

    public static void error(Component host, String title, String message) {
        Window owner = SwingUtilities.getWindowAncestor(host);
        if (owner == null) {
            return;
        }
        JDialog dialog = createDialog(owner, title);

        JPanel card = buildCardShell(
                title,
                message,
                "ALERT",
                UITheme.DANGER_SOFT_BG,
                UITheme.DANGER,
                dialog::dispose
        );

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);

        RoundedButton btnTutup = new RoundedButton("Mengerti", ButtonVariant.PRIMARY, UITheme.BUTTON_RADIUS);
        btnTutup.setFont(UITheme.font(Font.BOLD, 12f));
        btnTutup.setPreferredSize(new Dimension(96, 36));
        btnTutup.addActionListener(e -> dialog.dispose());
        btnRow.add(btnTutup);
        card.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.getRootPane().setDefaultButton(btnTutup);
        showWithBackdrop(owner, dialog, 380, 440);
    }

    public static JDialog createDialog(Window owner, String title) {
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        try {
            dialog.setBackground(new Color(0, 0, 0, 0));
        } catch (UnsupportedOperationException | IllegalArgumentException ignored) {
        }
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE_MODAL");
        dialog.getRootPane().getActionMap().put("CLOSE_MODAL", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        return dialog;
    }

    public static JPanel buildCardShell(String title, String message, String iconKind,
                                        Color iconBg, Color iconFg, Runnable onClose) {
        JPanel card = new RoundedCardPanel(new BorderLayout(0, 16), UITheme.CARD_RADIUS);
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);

        JPanel iconWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        iconWrap.setOpaque(false);
        iconWrap.add(createStatusIconBadge(iconKind, iconBg, iconFg, 38));
        body.add(iconWrap, BorderLayout.WEST);

        JPanel textCol = new JPanel(new GridBagLayout());
        textCol.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel titleBar = new JPanel(new BorderLayout(8, 0));
        titleBar.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UITheme.font(Font.BOLD, 15f));
        lblTitle.setForeground(UITheme.TEXT_MAIN);
        titleBar.add(lblTitle, BorderLayout.CENTER);

        RoundedButton btnClose = new RoundedButton("\u00D7", ButtonVariant.GHOST, UITheme.SMALL_RADIUS);
        btnClose.setFont(UITheme.font(Font.BOLD, 16f));
        btnClose.setPreferredSize(new Dimension(26, 26));
        btnClose.addActionListener(e -> onClose.run());
        titleBar.add(btnClose, BorderLayout.EAST);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        textCol.add(titleBar, gbc);

        JTextArea txtMsg = createWrappedMessage(message, 13f, UITheme.TEXT_LABEL);
        txtMsg.setSize(new Dimension(300, Short.MAX_VALUE));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        textCol.add(txtMsg, gbc);

        body.add(textCol, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    public static void showWithBackdrop(Window owner, JDialog dialog, int minWidth, int maxWidth) {
        dialog.pack();
        int width = Math.max(minWidth, Math.min(maxWidth, dialog.getPreferredSize().width));
        dialog.setSize(width, dialog.getPreferredSize().height);
        dialog.validate();
        int finalHeight = Math.max(dialog.getHeight(), dialog.getPreferredSize().height);
        dialog.setSize(width, finalHeight);
        dialog.setLocationRelativeTo(owner);

        if (owner instanceof RootPaneContainer rpc) {
            Component prevGlass = rpc.getGlassPane();
            JPanel backdrop = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    g.setColor(new Color(15, 23, 42, 95));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            backdrop.setOpaque(false);
            rpc.setGlassPane(backdrop);
            backdrop.setVisible(true);
            try {
                dialog.setVisible(true);
            } finally {
                backdrop.setVisible(false);
                rpc.setGlassPane(prevGlass);
            }
        } else {
            dialog.setVisible(true);
        }
    }

    public static JTextArea createWrappedMessage(String message, float fontSize, Color color) {
        JTextArea area = new JTextArea(message);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UITheme.font(Font.PLAIN, fontSize));
        area.setForeground(color);
        area.setBorder(null);
        return area;
    }

    public static JPanel createStatusIconBadge(String kind, Color bg, Color fg, int size) {
        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);

                g2.setColor(fg);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                if ("CHECK".equals(kind)) {
                    g2.drawPolyline(
                            new int[]{cx - 5, cx - 1, cx + 6},
                            new int[]{cy, cy + 4, cy - 4},
                            3
                    );
                } else if ("INFO".equals(kind)) {
                    g2.fillOval(cx - 1, cy - 6, 3, 3);
                    g2.drawLine(cx, cy - 1, cx, cy + 5);
                } else if ("WARNING".equals(kind)) {
                    g2.drawPolygon(
                            new int[]{cx, cx - 8, cx + 8},
                            new int[]{cy - 7, cy + 6, cy + 6},
                            3
                    );
                    g2.drawLine(cx, cy - 2, cx, cy + 2);
                    g2.fillOval(cx - 1, cy + 3, 2, 2);
                } else {
                    g2.drawLine(cx, cy - 5, cx, cy + 1);
                    g2.fillOval(cx - 1, cy + 4, 3, 3);
                }
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(size, size));
        badge.setMinimumSize(new Dimension(size, size));
        badge.setOpaque(false);
        return badge;
    }
}
