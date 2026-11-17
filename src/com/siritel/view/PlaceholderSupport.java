package com.siritel.view;

import com.siritel.view.ui.UITheme;

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
    private static final Color BG_COLOR = UITheme.BG_PAGE;
    private static final Color TITLE_COLOR = UITheme.TEXT_MAIN;
    private static final Color SUBTITLE_COLOR = UITheme.TEXT_MUTED;

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
        titleLabel.setFont(UITheme.font(Font.BOLD, 22f));
        titleLabel.setForeground(TITLE_COLOR);
        panel.add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);

        JLabel subtitleLabel = new JLabel(subtitle, SwingConstants.CENTER);
        subtitleLabel.setFont(UITheme.font(Font.PLAIN, 13f));
        subtitleLabel.setForeground(SUBTITLE_COLOR);
        panel.add(subtitleLabel, gbc);
    }
}
