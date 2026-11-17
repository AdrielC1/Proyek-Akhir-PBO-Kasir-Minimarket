package com.siritel.view;

import com.siritel.controller.AuthController;
import com.siritel.model.Pengguna;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Jendela utama aplikasi SI-RITEL dengan sidebar navigasi dan area konten berbasis CardLayout.
 */
public class MainFrame extends JFrame {
    private static final Color SIDEBAR_BG = new Color(30, 41, 59);
    private static final Color SIDEBAR_DIVIDER = new Color(51, 65, 85);
    private static final Color BTN_HOVER_BG = new Color(51, 65, 85);
    private static final Color BTN_ACTIVE_BG = new Color(37, 99, 235);
    private static final Color BTN_LOGOUT_BG = new Color(220, 38, 38);
    private static final Color TEXT_LIGHT = new Color(226, 232, 240);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);
    private static final Color HEADER_BG = Color.WHITE;
    private static final Color HEADER_BORDER = new Color(226, 232, 240);

    private record NavItem(String key, String label, JPanel panel) {
    }

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(cardLayout);
    private final JLabel pageHeaderLabel = new JLabel();
    private final JLabel userProfileLabel = new JLabel();
    private final Map<String, JButton> sidebarButtons = new LinkedHashMap<>();
    private final Map<String, String> pageTitles = new LinkedHashMap<>();
    private String activeKey;
    private final Pengguna penggunaAktif;

    public MainFrame() {
        this(AuthController.getPenggunaAktif());
    }

    public MainFrame(Pengguna penggunaAktif) {
        this.penggunaAktif = penggunaAktif;

        setTitle("SI-RITEL — Sistem Informasi Kasir & Inventaris Ritel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 680);
        setMinimumSize(new Dimension(880, 560));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        List<NavItem> navItems = buildNavItems();

        add(buildSidebar(navItems), BorderLayout.WEST);
        add(buildMainArea(navItems), BorderLayout.CENTER);

        if (!navItems.isEmpty()) {
            navigateTo(navItems.get(0).key());
        }
    }

    private List<NavItem> buildNavItems() {
        List<NavItem> items = new ArrayList<>();
        String role = (penggunaAktif != null && penggunaAktif.getRole() != null)
                ? penggunaAktif.getRole()
                : "Admin";

        // Kasir dapat diakses oleh Admin dan Kasir
        // TODO INTEGRASI: ganti placeholder dengan new KasirView() dari Modul 3
        items.add(new NavItem("kasir", "Kasir", buatPlaceholder("Kasir")));

        if ("Admin".equalsIgnoreCase(role)) {
            // TODO INTEGRASI: ganti placeholder dengan new ManajemenProdukView() dari Modul 2
            items.add(new NavItem("produk", "Manajemen Produk & Batch", buatPlaceholder("Manajemen Produk & Batch")));

            // Modul 1: Manajemen Pengguna
            items.add(new NavItem("pengguna", "Manajemen Pengguna", new ManajemenPenggunaView()));
        }

        // Riwayat Transaksi dapat diakses oleh Admin dan Kasir
        // TODO INTEGRASI: ganti placeholder dengan new RiwayatTransaksiView() dari Modul 3
        items.add(new NavItem("riwayat", "Riwayat Transaksi", buatPlaceholder("Riwayat Transaksi")));

        if ("Admin".equalsIgnoreCase(role)) {
            // TODO INTEGRASI: ganti placeholder dengan new LaporanPenjualanView() dari Modul 3
            items.add(new NavItem("laporan", "Laporan Penjualan", buatPlaceholder("Laporan Penjualan")));
        }

        return items;
    }

    private JPanel buatPlaceholder(String judul) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel label = new JLabel("Halaman " + judul + ", dikerjakan Anggota 2/3", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        label.setForeground(new Color(100, 116, 139));

        panel.add(label);
        return panel;
    }

    private JPanel buildSidebar(List<NavItem> navItems) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(245, 0));
        sidebar.setBackground(SIDEBAR_BG);

        sidebar.add(buildBrandHeader(), BorderLayout.NORTH);

        JPanel navList = new JPanel(new GridLayout(0, 1, 0, 6));
        navList.setOpaque(false);
        navList.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        for (NavItem item : navItems) {
            JButton button = createSidebarButton(item);
            sidebarButtons.put(item.key(), button);
            navList.add(button);
        }

        JPanel navWrapper = new JPanel(new BorderLayout());
        navWrapper.setOpaque(false);
        navWrapper.add(navList, BorderLayout.NORTH);
        sidebar.add(navWrapper, BorderLayout.CENTER);

        sidebar.add(buildLogoutSection(), BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel buildBrandHeader() {
        JPanel brandPanel = new JPanel(new GridLayout(0, 1, 0, 2));
        brandPanel.setBackground(SIDEBAR_BG);
        brandPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, SIDEBAR_DIVIDER),
                BorderFactory.createEmptyBorder(18, 16, 16, 16)
        ));

        JLabel title = new JLabel("SI-RITEL");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Kasir & Inventaris Ritel");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_MUTED);

        brandPanel.add(title);
        brandPanel.add(subtitle);
        return brandPanel;
    }

    private JButton createSidebarButton(NavItem item) {
        JButton button = new JButton(item.label());
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));
        button.setForeground(TEXT_LIGHT);
        button.setBackground(SIDEBAR_BG);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> navigateTo(item.key()));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!item.key().equals(activeKey)) {
                    button.setBackground(BTN_HOVER_BG);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!item.key().equals(activeKey)) {
                    button.setBackground(SIDEBAR_BG);
                }
            }
        });

        return button;
    }

    private JPanel buildLogoutSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SIDEBAR_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, SIDEBAR_DIVIDER),
                BorderFactory.createEmptyBorder(12, 12, 16, 12)
        ));

        JButton btnLogout = new JButton("Logout");
        btnLogout.setHorizontalAlignment(SwingConstants.CENTER);
        btnLogout.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBackground(BTN_LOGOUT_BG);
        btnLogout.setOpaque(true);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnLogout.addActionListener(e -> {
            AuthController.logout();
            dispose();
            new LoginView().setVisible(true);
        });

        panel.add(btnLogout, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildMainArea(List<NavItem> navItems) {
        JPanel mainArea = new JPanel(new BorderLayout());

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(HEADER_BG);
        headerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, HEADER_BORDER),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)
        ));

        pageHeaderLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        pageHeaderLabel.setForeground(SIDEBAR_BG);
        headerBar.add(pageHeaderLabel, BorderLayout.WEST);

        String userDisplay = (penggunaAktif != null)
                ? penggunaAktif.getNamaLengkap() + " (" + penggunaAktif.getRole() + ")"
                : "Tamu";
        userProfileLabel.setText(userDisplay);
        userProfileLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        userProfileLabel.setForeground(new Color(71, 85, 105));
        headerBar.add(userProfileLabel, BorderLayout.EAST);

        for (NavItem item : navItems) {
            pageTitles.put(item.key(), item.label());
            contentCards.add(item.panel(), item.key());
        }

        mainArea.add(headerBar, BorderLayout.NORTH);
        mainArea.add(contentCards, BorderLayout.CENTER);
        return mainArea;
    }

    /**
     * Berpindah ke halaman target di area utama dan menandai tombol sidebar yang aktif.
     */
    public void navigateTo(String key) {
        if (!pageTitles.containsKey(key)) {
            return;
        }
        activeKey = key;
        cardLayout.show(contentCards, key);
        pageHeaderLabel.setText(pageTitles.get(key));

        for (Map.Entry<String, JButton> entry : sidebarButtons.entrySet()) {
            boolean isActive = entry.getKey().equals(key);
            JButton button = entry.getValue();
            button.setBackground(isActive ? BTN_ACTIVE_BG : SIDEBAR_BG);
            button.setForeground(isActive ? Color.WHITE : TEXT_LIGHT);
            button.setFont(new Font("SansSerif", isActive ? Font.BOLD : Font.PLAIN, 14));
        }
    }
}
