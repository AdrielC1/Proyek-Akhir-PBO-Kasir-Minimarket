package com.siritel.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Helper internal untuk menyusun tampilan placeholder seragam pada panel halaman yang masih kosong.
 */
final class PlaceholderSupport {
    private static final Color BG_COLOR = new Color(248, 250, 252);
    private static final Color TITLE_COLOR = new Color(30, 41, 59);
    private static final Color SUBTITLE_COLOR = new Color(100, 116, 139);

    private PlaceholderSupport() {
    }

    static void initEmptyPage(JPanel panel, String title, String subtitle) {
        panel.setLayout(new GridBagLayout());
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 8, 0);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(TITLE_COLOR);
        panel.add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);

        JLabel subtitleLabel = new JLabel(subtitle, SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitleLabel.setForeground(SUBTITLE_COLOR);
        panel.add(subtitleLabel, gbc);
    }
}
