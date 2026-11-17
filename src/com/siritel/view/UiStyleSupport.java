package com.siritel.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;

/**
 * Helper bersama untuk komponen UI modern SI-RITEL: kartu membulat, tombol rounded,
 * border melengkung, toast notifikasi kanan atas, dan modal dialog konfirmasi/error.
 */
final class UiStyleSupport {
    static final Color BG_PANEL = new Color(248, 250, 252);
    static final Color CARD_BG = Color.WHITE;
    static final Color BORDER_COLOR = new Color(226, 232, 240);
    static final Color INPUT_BORDER = new Color(203, 213, 225);
    static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    static final Color PRIMARY_BLUE_SOFT = new Color(239, 246, 255);
    static final Color TEAL_COLOR = new Color(13, 148, 136);
    static final Color DANGER_RED = new Color(220, 38, 38);
    static final Color DANGER_SOFT_BG = new Color(254, 226, 226);
    static final Color SUCCESS_GREEN = new Color(22, 163, 74);
    static final Color SUCCESS_SOFT_BG = new Color(220, 252, 231);
    static final Color TEXT_DARK = new Color(15, 23, 42);
    static final Color TEXT_SLATE = new Color(51, 65, 85);
    static final Color TEXT_MUTED = new Color(100, 116, 139);

    private UiStyleSupport() {
    }

    enum ButtonVariant {
        PRIMARY,
        TEAL,
        DANGER,
        OUTLINE,
        DANGER_OUTLINE,
        GHOST
    }

    enum ToastType {
        SUCCESS,
        INFO,
        ERROR
    }

    static JPanel createRoundedCardPanel(LayoutManager layout, int radius) {
        JPanel panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    static JTextArea createWrappedMessage(String message, int fontSize, Color color) {
        JTextArea area = new JTextArea(message);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        area.setForeground(color);
        area.setBorder(null);
        return area;
    }

    static JPanel createStatusIconBadge(String kind, Color bg, Color fg, int size) {
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

    static boolean showConfirmModal(Component host, String title, String message, String confirmText) {
        Window owner = SwingUtilities.getWindowAncestor(host);
        if (owner == null) {
            return false;
        }
        final boolean[] confirmed = {false};
        JDialog dialog = createModernModalDialog(owner, title);

        JPanel card = buildModalCardShell(
                title,
                message,
                "WARNING",
                DANGER_SOFT_BG,
                DANGER_RED,
                dialog::dispose
        );

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        RoundedButton btnCancel = new RoundedButton("Batal", ButtonVariant.OUTLINE, 10);
        btnCancel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCancel.setPreferredSize(new Dimension(82, 36));
        btnCancel.addActionListener(e -> dialog.dispose());

        RoundedButton btnOk = new RoundedButton(confirmText, ButtonVariant.DANGER, 10);
        btnOk.setFont(new Font("SansSerif", Font.BOLD, 12));
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
        showDialogWithBackdrop(owner, dialog, 380, 440);
        return confirmed[0];
    }

    static void showErrorModal(Component host, String title, String message) {
        Window owner = SwingUtilities.getWindowAncestor(host);
        if (owner == null) {
            return;
        }
        JDialog dialog = createModernModalDialog(owner, title);

        JPanel card = buildModalCardShell(
                title,
                message,
                "ALERT",
                DANGER_SOFT_BG,
                DANGER_RED,
                dialog::dispose
        );

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);

        RoundedButton btnTutup = new RoundedButton("Mengerti", ButtonVariant.PRIMARY, 10);
        btnTutup.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnTutup.setPreferredSize(new Dimension(96, 36));
        btnTutup.addActionListener(e -> dialog.dispose());
        btnRow.add(btnTutup);
        card.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.getRootPane().setDefaultButton(btnTutup);
        showDialogWithBackdrop(owner, dialog, 380, 440);
    }

    static JDialog createModernModalDialog(Window owner, String title) {
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

    static JPanel buildModalCardShell(String title, String message, String iconKind,
                                      Color iconBg, Color iconFg, Runnable onClose) {
        JPanel card = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
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
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTitle.setForeground(TEXT_DARK);
        titleBar.add(lblTitle, BorderLayout.CENTER);

        RoundedButton btnClose = new RoundedButton("\u00D7", ButtonVariant.GHOST, 8);
        btnClose.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnClose.setPreferredSize(new Dimension(26, 26));
        btnClose.addActionListener(e -> onClose.run());
        titleBar.add(btnClose, BorderLayout.EAST);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        textCol.add(titleBar, gbc);

        JTextArea txtMsg = createWrappedMessage(message, 13, TEXT_SLATE);
        txtMsg.setSize(new Dimension(300, Short.MAX_VALUE));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        textCol.add(txtMsg, gbc);

        body.add(textCol, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    static void showDialogWithBackdrop(Window owner, JDialog dialog, int minWidth, int maxWidth) {
        dialog.pack();
        int width = Math.max(minWidth, Math.min(maxWidth, dialog.getPreferredSize().width));
        dialog.setSize(width, dialog.getPreferredSize().height);
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

    /**
     * Pengelola toast notifikasi melayang di pojok kanan atas jendela utama.
     */
    static final class ToastManager {
        private final Component host;
        private JPanel activeToast;
        private Timer toastTimer;
        private JLayeredPane hookedPane;

        ToastManager(Component host) {
            this.host = host;
        }

        void show(String title, String message, ToastType type) {
            Window win = SwingUtilities.getWindowAncestor(host);
            if (!(win instanceof RootPaneContainer rpc)) {
                return;
            }
            JLayeredPane layeredPane = rpc.getLayeredPane();
            if (layeredPane == null) {
                return;
            }

            dismiss();

            if (hookedPane != layeredPane) {
                hookedPane = layeredPane;
                layeredPane.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        reposition();
                    }
                });
            }

            Color iconBg;
            Color iconFg;
            Color accentBorder;
            if (type == ToastType.SUCCESS) {
                iconBg = SUCCESS_SOFT_BG;
                iconFg = SUCCESS_GREEN;
                accentBorder = new Color(187, 247, 208);
            } else if (type == ToastType.ERROR) {
                iconBg = DANGER_SOFT_BG;
                iconFg = DANGER_RED;
                accentBorder = new Color(254, 202, 202);
            } else {
                iconBg = PRIMARY_BLUE_SOFT;
                iconFg = PRIMARY_BLUE;
                accentBorder = new Color(191, 219, 254);
            }

            JPanel toastCard = new JPanel(new BorderLayout(10, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(15, 23, 42, 18));
                    g2.fillRoundRect(2, 3, getWidth() - 3, getHeight() - 3, 12, 12);
                    g2.setColor(CARD_BG);
                    g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 4, 12, 12);
                    g2.setColor(accentBorder);
                    g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 4, 12, 12);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            toastCard.setOpaque(false);
            toastCard.setBorder(BorderFactory.createEmptyBorder(11, 13, 14, 12));

            JPanel iconBadge = createStatusIconBadge(type == ToastType.SUCCESS ? "CHECK"
                    : (type == ToastType.ERROR ? "ALERT" : "INFO"), iconBg, iconFg, 30);
            JPanel iconWrap = new JPanel(new GridBagLayout());
            iconWrap.setOpaque(false);
            iconWrap.add(iconBadge);
            toastCard.add(iconWrap, BorderLayout.WEST);

            JPanel textCol = new JPanel(new GridBagLayout());
            textCol.setOpaque(false);
            GridBagConstraints tg = new GridBagConstraints();
            tg.gridx = 0;
            tg.weightx = 1.0;
            tg.fill = GridBagConstraints.HORIZONTAL;

            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblTitle.setForeground(TEXT_DARK);
            tg.gridy = 0;
            tg.insets = new Insets(0, 0, 2, 0);
            textCol.add(lblTitle, tg);

            JTextArea txtMsg = createWrappedMessage(message, 12, TEXT_SLATE);
            tg.gridy = 1;
            tg.insets = new Insets(0, 0, 0, 0);
            textCol.add(txtMsg, tg);
            toastCard.add(textCol, BorderLayout.CENTER);

            RoundedButton btnClose = new RoundedButton("\u00D7", ButtonVariant.GHOST, 8);
            btnClose.setFont(new Font("SansSerif", Font.BOLD, 14));
            btnClose.setPreferredSize(new Dimension(24, 24));
            btnClose.addActionListener(e -> dismiss());
            JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            closeWrap.setOpaque(false);
            closeWrap.add(btnClose);
            toastCard.add(closeWrap, BorderLayout.EAST);

            int toastWidth = 345;
            toastCard.setSize(toastWidth, 72);
            txtMsg.setSize(new Dimension(toastWidth - 96, Short.MAX_VALUE));
            int prefH = Math.max(62, toastCard.getPreferredSize().height);
            toastCard.setSize(toastWidth, prefH);

            activeToast = toastCard;
            layeredPane.add(toastCard, JLayeredPane.POPUP_LAYER);
            reposition();
            layeredPane.repaint();

            toastTimer = new Timer(3600, e -> dismiss());
            toastTimer.setRepeats(false);
            toastTimer.start();
        }

        void dismiss() {
            if (toastTimer != null) {
                toastTimer.stop();
                toastTimer = null;
            }
            if (activeToast != null && hookedPane != null) {
                hookedPane.remove(activeToast);
                hookedPane.repaint();
                activeToast = null;
            }
        }

        private void reposition() {
            if (activeToast == null || hookedPane == null) {
                return;
            }
            int paneW = hookedPane.getWidth();
            if (paneW <= 0) {
                return;
            }
            int x = Math.max(16, paneW - activeToast.getWidth() - 20);
            activeToast.setLocation(x, 72);
            activeToast.revalidate();
            activeToast.repaint();
        }
    }

    /**
     * Tombol kustom membulat dengan efek hover, pressed, dan status disabled yang kontras.
     */
    static class RoundedButton extends JButton {
        private final ButtonVariant variant;
        private final int radius;
        private boolean hovered = false;
        private boolean pressed = false;

        RoundedButton(String text, ButtonVariant variant, int radius) {
            super(text);
            this.variant = variant;
            this.radius = radius;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    pressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled()) {
                        pressed = true;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    repaint();
                }
            });
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
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
                bg = new Color(241, 245, 249);
                border = BORDER_COLOR;
                fg = TEXT_MUTED;
            } else {
                switch (variant) {
                    case PRIMARY -> {
                        bg = pressed ? new Color(30, 64, 175)
                                : hovered ? new Color(29, 78, 216) : PRIMARY_BLUE;
                        fg = Color.WHITE;
                    }
                    case TEAL -> {
                        bg = pressed ? new Color(17, 94, 89)
                                : hovered ? new Color(15, 118, 110) : TEAL_COLOR;
                        fg = Color.WHITE;
                    }
                    case DANGER -> {
                        bg = pressed ? new Color(153, 27, 27)
                                : hovered ? new Color(185, 28, 28) : DANGER_RED;
                        fg = Color.WHITE;
                    }
                    case DANGER_OUTLINE -> {
                        bg = pressed ? DANGER_SOFT_BG
                                : hovered ? new Color(254, 242, 242) : Color.WHITE;
                        border = hovered ? new Color(239, 68, 68) : new Color(252, 165, 165);
                        fg = DANGER_RED;
                    }
                    case GHOST -> {
                        bg = pressed ? BORDER_COLOR
                                : hovered ? new Color(241, 245, 249) : new Color(0, 0, 0, 0);
                        fg = hovered ? TEXT_DARK : TEXT_MUTED;
                    }
                    default -> {
                        bg = pressed ? new Color(241, 245, 249)
                                : hovered ? new Color(248, 250, 252) : Color.WHITE;
                        border = hovered ? new Color(148, 163, 184) : INPUT_BORDER;
                        fg = hovered ? TEXT_DARK : TEXT_SLATE;
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

    /**
     * Border melengkung anti-aliased untuk input dan kontainer.
     */
    static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final Insets padding;

        RoundedBorder(Color color, int radius, Insets padding) {
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
}
