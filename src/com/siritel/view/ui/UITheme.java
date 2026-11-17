package com.siritel.view.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;

/**
 * Konstanta tema dan helper gaya UI terpusat untuk seluruh aplikasi SI-RITEL.
 * Seluruh halaman (Login, MainFrame, Modul 1, Modul 2, dan Modul 3) wajib mengacu ke kelas ini
 * agar warna, tipografi, radius sudut, dan dimensi komponen seragam.
 */
public final class UITheme {

    private UITheme() {
        // Utility class
    }

    // --- Dimensi Layout Kartu & Komponen ---
    public static final int CARD_WIDTH = 400;
    public static final int CARD_RADIUS = 16;
    public static final int PANEL_RADIUS = 14;
    public static final int CARD_PADDING = 32;
    public static final int CONTENT_WIDTH = CARD_WIDTH - (2 * CARD_PADDING); // 336 px

    public static final int TILE_SIZE = 48;
    public static final int TILE_RADIUS = 12;

    public static final int FIELD_HEIGHT = 40;
    public static final int FIELD_RADIUS = 10;
    public static final int SMALL_RADIUS = 8;
    public static final int BANNER_HEIGHT = 40;
    public static final int BANNER_RADIUS = 8;
    public static final int BUTTON_HEIGHT = 40;
    public static final int TOOLBAR_BUTTON_HEIGHT = 34;
    public static final int BUTTON_RADIUS = 10;

    // --- Jarak Vertikal Login ---
    public static final int GAP_TILE_TO_TITLE = 16;
    public static final int GAP_TITLE_TO_SUBTITLE = 4;
    public static final int GAP_SUBTITLE_TO_LABEL = 28;
    public static final int GAP_LABEL_TO_FIELD = 8;
    public static final int GAP_FIELD_TO_FIELD = 16;
    public static final int GAP_FIELD_TO_BANNER = 12;
    public static final int GAP_BANNER_TO_BUTTON = 16;
    public static final int GAP_BUTTON_TO_HINT = 20;
    public static final int PADDING_BOTTOM = 28;

    // --- Warna Latar Belakang & Kartu ---
    public static final Color BG_PAGE = new Color(248, 250, 252);             // Slate 50
    public static final Color BG_HOVER = new Color(241, 245, 249);            // Slate 100
    public static final Color BG_GRADIENT_TOP = new Color(240, 245, 252);     // Biru sangat muda atas
    public static final Color BG_GRADIENT_BOTTOM = new Color(248, 250, 253);  // Putih keabuan bawah
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(226, 232, 240);         // Slate 200

    // --- Warna Teks & Netral ---
    public static final Color TEXT_MAIN = new Color(15, 23, 42);              // Slate 900
    public static final Color TEXT_LABEL = new Color(51, 65, 85);             // Slate 700
    public static final Color TEXT_MUTED = new Color(100, 116, 139);          // Slate 500
    public static final Color TEXT_HINT = new Color(148, 163, 184);           // Slate 400

    // --- Warna Komponen Input & Border ---
    public static final Color BORDER_DEFAULT = new Color(226, 232, 240);      // Slate 200 (1px)
    public static final Color BORDER_INPUT = new Color(203, 213, 225);        // Slate 300
    public static final Color BORDER_HOVER = new Color(148, 163, 184);        // Slate 400
    public static final Color BORDER_FOCUS = new Color(59, 130, 246);         // Blue 500
    public static final Color GLOW_FOCUS = new Color(59, 130, 246, 35);       // Blue 500 glow
    public static final Color BORDER_ERROR = new Color(248, 113, 113);        // Red 400
    public static final Color GLOW_ERROR = new Color(239, 68, 68, 30);        // Red 500 glow

    // --- Warna Tombol Primer ---
    public static final Color PRIMARY_BLUE = new Color(37, 99, 235);          // Blue 600
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216);         // Blue 700
    public static final Color PRIMARY_PRESSED = new Color(30, 64, 175);       // Blue 800
    public static final Color PRIMARY_DISABLED = new Color(147, 197, 253);    // Blue 300
    public static final Color PRIMARY_SOFT_BG = new Color(239, 246, 255);     // Blue 50
    public static final Color PRIMARY_SOFT_BORDER = new Color(191, 219, 254); // Blue 200

    // --- Warna Tombol Sekunder & Semantik ---
    public static final Color TEAL = new Color(13, 148, 136);
    public static final Color TEAL_HOVER = new Color(15, 118, 110);
    public static final Color TEAL_PRESSED = new Color(17, 94, 89);

    public static final Color DANGER = new Color(220, 38, 38);                // Red 600
    public static final Color DANGER_HOVER = new Color(185, 28, 28);          // Red 700
    public static final Color DANGER_PRESSED = new Color(153, 27, 27);        // Red 800
    public static final Color DANGER_SOFT_BG = new Color(254, 226, 226);      // Red 100
    public static final Color DANGER_BORDER_LIGHT = new Color(252, 165, 165); // Red 300

    public static final Color SUCCESS = new Color(22, 163, 74);               // Green 600
    public static final Color SUCCESS_SOFT_BG = new Color(220, 252, 231);     // Green 100
    public static final Color SUCCESS_BORDER = new Color(187, 247, 208);      // Green 200

    // --- Warna Badge Kategori & Role ---
    public static final Color BADGE_AMBER_BG = new Color(254, 243, 199);
    public static final Color BADGE_AMBER_FG = new Color(180, 83, 9);
    public static final Color BADGE_BLUE_BG = new Color(219, 234, 254);
    public static final Color BADGE_BLUE_FG = new Color(29, 78, 216);
    public static final Color BADGE_PURPLE_BG = new Color(243, 232, 255);
    public static final Color BADGE_PURPLE_FG = new Color(126, 34, 206);
    public static final Color BADGE_TEAL_BG = new Color(204, 251, 241);
    public static final Color BADGE_TEAL_FG = new Color(15, 118, 110);

    // --- Warna Banner Error Inline ---
    public static final Color ALERT_BG = new Color(254, 242, 242);            // Red 50 (#FEF2F2)
    public static final Color ALERT_BORDER = new Color(254, 202, 202);        // Red 200 (#FECACA)
    public static final Color ALERT_TEXT = new Color(220, 38, 38);            // Red 600 (#DC2626)
    public static final Color ALERT_ICON = new Color(239, 68, 68);            // Red 500 (#EF4444)

    // --- Warna Badge Ikon Brand ---
    public static final Color BRAND_ICON_BG = new Color(239, 246, 255);       // Blue 50
    public static final Color BRAND_ICON_BORDER = new Color(219, 234, 254);   // Blue 100

    // --- Warna Keycap Hint ---
    public static final Color KEYCAP_BG = new Color(241, 245, 249);           // Slate 100
    public static final Color KEYCAP_BORDER = new Color(203, 213, 225);       // Slate 300
    public static final Color KEYCAP_TEXT = new Color(71, 85, 105);           // Slate 600

    // --- Tipografi / Font Hierarchy (Inter -> Segoe UI -> SansSerif) ---
    private static final String PREFERRED_FONT_FAMILY;

    static {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
        ));
        if (available.contains("Inter")) {
            PREFERRED_FONT_FAMILY = "Inter";
        } else if (available.contains("Segoe UI")) {
            PREFERRED_FONT_FAMILY = "Segoe UI";
        } else {
            PREFERRED_FONT_FAMILY = Font.SANS_SERIF;
        }
    }

    public static Font font(int style, float size) {
        return new Font(PREFERRED_FONT_FAMILY, style, (int) size).deriveFont(style, size);
    }

    public static Font titleFont() {
        return font(Font.BOLD, 24f);
    }

    public static Font subtitleFont() {
        return font(Font.PLAIN, 13f);
    }

    public static Font labelFont() {
        return font(Font.BOLD, 13f);
    }

    public static Font inputFont() {
        return font(Font.PLAIN, 13f);
    }

    public static Font buttonFont() {
        return font(Font.BOLD, 14f);
    }

    public static Font alertFont() {
        return font(Font.PLAIN, 13f);
    }

    public static Font hintFont() {
        return font(Font.PLAIN, 12f);
    }

    public static Font keycapFont() {
        return font(Font.BOLD, 11f);
    }

    // --- Helper Styling Komponen Umum ---

    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.BOLD, 12f));
        label.setForeground(TEXT_LABEL);
        return label;
    }

    public static void styleInputField(JTextField field) {
        field.setFont(font(Font.PLAIN, 12f));
        field.setForeground(TEXT_MAIN);
        field.setPreferredSize(new Dimension(0, 32));
        field.setBorder(new RoundedBorder(BORDER_INPUT, SMALL_RADIUS, new Insets(4, 10, 4, 10)));
    }

    public static void styleNumericSpinner(JSpinner spinner) {
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#");
        spinner.setEditor(editor);
        spinner.setFont(font(Font.PLAIN, 12f));
        spinner.setPreferredSize(new Dimension(0, 30));
        spinner.setBorder(new RoundedBorder(BORDER_INPUT, SMALL_RADIUS, new Insets(1, 4, 1, 4)));
    }

    public static JTextField createPlaceholderField(String placeholder) {
        return new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(TEXT_HINT);
                    g2.setFont(getFont());
                    Insets insets = getInsets();
                    int y = (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
                    g2.drawString(placeholder, insets.left, y);
                    g2.dispose();
                }
            }
        };
    }
}
