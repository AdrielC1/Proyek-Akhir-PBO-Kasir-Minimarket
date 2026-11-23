package com.siritel.view.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.WeakHashMap;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Pengelola notifikasi toast non-blocking di pojok kanan atas jendela utama.
 * Berkoordinasi lintas instance per {@code JLayeredPane} agar toast antar tab tidak bertumpuk.
 */
public final class ToastManager {
    private static final WeakHashMap<JLayeredPane, ToastManager> ACTIVE_BY_PANE = new WeakHashMap<>();

    private final Component host;
    private JPanel activeToast;
    private Timer toastTimer;
    private JLayeredPane hookedPane;

    public ToastManager(Component host) {
        this.host = host;
    }

    public void show(String title, String message, ToastType type) {
        Window win = SwingUtilities.getWindowAncestor(host);
        if (!(win instanceof RootPaneContainer rpc)) {
            return;
        }
        JLayeredPane layeredPane = rpc.getLayeredPane();
        if (layeredPane == null) {
            return;
        }

        ToastManager previous = ACTIVE_BY_PANE.get(layeredPane);
        if (previous != null && previous != this) {
            previous.dismiss();
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
            iconBg = UITheme.SUCCESS_SOFT_BG;
            iconFg = UITheme.SUCCESS;
            accentBorder = UITheme.SUCCESS_BORDER;
        } else {
            iconBg = UITheme.PRIMARY_SOFT_BG;
            iconFg = UITheme.PRIMARY_BLUE;
            accentBorder = UITheme.PRIMARY_SOFT_BORDER;
        }

        JPanel toastCard = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 23, 42, 18));
                g2.fillRoundRect(2, 3, getWidth() - 3, getHeight() - 3, 12, 12);
                g2.setColor(UITheme.CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 4, 12, 12);
                g2.setColor(accentBorder);
                g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 4, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        toastCard.setOpaque(false);
        toastCard.setBorder(BorderFactory.createEmptyBorder(11, 13, 14, 12));

        JPanel iconBadge = ModernModal.createStatusIconBadge(
                type == ToastType.SUCCESS ? "CHECK" : "INFO",
                iconBg, iconFg, 30
        );
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
        lblTitle.setFont(UITheme.font(Font.BOLD, 12f));
        lblTitle.setForeground(UITheme.TEXT_MAIN);
        tg.gridy = 0;
        tg.insets = new Insets(0, 0, 2, 0);
        textCol.add(lblTitle, tg);

        JTextArea txtMsg = ModernModal.createWrappedMessage(message, 12f, UITheme.TEXT_LABEL);
        tg.gridy = 1;
        tg.insets = new Insets(0, 0, 0, 0);
        textCol.add(txtMsg, tg);
        toastCard.add(textCol, BorderLayout.CENTER);

        RoundedButton btnClose = new RoundedButton("\u00D7", ButtonVariant.GHOST, UITheme.SMALL_RADIUS);
        btnClose.setFont(UITheme.font(Font.BOLD, 14f));
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
        ACTIVE_BY_PANE.put(layeredPane, this);
        layeredPane.add(toastCard, JLayeredPane.POPUP_LAYER);
        reposition();
        layeredPane.repaint();

        toastTimer = new Timer(3600, e -> dismiss());
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    public void dismiss() {
        if (toastTimer != null) {
            toastTimer.stop();
            toastTimer = null;
        }
        if (activeToast != null && hookedPane != null) {
            hookedPane.remove(activeToast);
            hookedPane.repaint();
            activeToast = null;
            if (ACTIVE_BY_PANE.get(hookedPane) == this) {
                ACTIVE_BY_PANE.remove(hookedPane);
            }
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
