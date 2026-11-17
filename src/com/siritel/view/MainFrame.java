package com.siritel.view;

import com.siritel.controller.AuthController;
import com.siritel.model.Pengguna;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
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
 * Jendela utama aplikasi SI-RITEL dengan restyle sidebar terang modern sesuai referensi visual.
 */
public class MainFrame extends JFrame {
    private static final Color SIDEBAR_BG = Color.WHITE;
    private static final Color SIDEBAR_BORDER = new Color(226, 232, 240);
    private static final Color BTN_TEXT = new Color(71, 85, 105);
    private static final Color BTN_HOVER_BG = new Color(241, 245, 249);
    private static final Color BTN_ACTIVE_BG = new Color(239, 246, 255);
    private static final Color BTN_ACTIVE_TEXT = new Color(37, 99, 235);
    private static final Color HEADER_BG = Color.WHITE;
    private static final Color HEADER_BORDER = new Color(226, 232, 240);
    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

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
        setSize(1180, 720);
        setMinimumSize(new Dimension(960, 600));
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
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, SIDEBAR_BORDER));

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

        return sidebar;
    }

    private JPanel buildBrandHeader() {
        JPanel brandPanel = new JPanel(new BorderLayout(12, 0));
        brandPanel.setBackground(SIDEBAR_BG);
        brandPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, SIDEBAR_BORDER),
                BorderFactory.createEmptyBorder(18, 16, 18, 16)
        ));

        JPanel iconBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(239, 246, 255));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(219, 234, 254));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                g2.setColor(new Color(37, 99, 235));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.drawRoundRect(cx - 8, cy - 5, 16, 13, 3, 3);
                g2.drawArc(cx - 4, cy - 10, 8, 8, 0, 180);
                g2.dispose();
            }
        };
        iconBox.setPreferredSize(new Dimension(38, 38));
        iconBox.setOpaque(false);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);

        JLabel title = new JLabel("SI-RITEL");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(TEXT_DARK);

        JLabel subtitle = new JLabel("Kasir Ritel Modern");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subtitle.setForeground(TEXT_MUTED);

        textPanel.add(title);
        textPanel.add(subtitle);

        brandPanel.add(iconBox, BorderLayout.WEST);
        brandPanel.add(textPanel, BorderLayout.CENTER);
        return brandPanel;
    }

    private JButton createSidebarButton(NavItem item) {
        JButton button = new JButton(item.label()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Indikator titik biru aktif di sisi kanan tombol jika sedang dipilih
                if (item.key().equals(activeKey)) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(BTN_ACTIVE_TEXT);
                    int dy = getHeight() / 2;
                    g2.fillOval(getWidth() - 18, dy - 3, 6, 6);
                    g2.dispose();
                }
            }
        };
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setForeground(BTN_TEXT);
        button.setBackground(SIDEBAR_BG);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 24));
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

    private JPanel buildMainArea(List<NavItem> navItems) {
        JPanel mainArea = new JPanel(new BorderLayout());

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(HEADER_BG);
        headerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, HEADER_BORDER),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)
        ));

        // Area Judul Halaman di Kiri
        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleBox.setOpaque(false);
        pageHeaderLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        pageHeaderLabel.setForeground(TEXT_DARK);
        titleBox.add(pageHeaderLabel);
        headerBar.add(titleBox, BorderLayout.WEST);

        // Area Profil & Tombol Keluar di Kanan
        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightBar.setOpaque(false);

        // Header tetap berformat "nama (role)" sesuai spesifikasi Adaptasi Wajib
        String userDisplay = (penggunaAktif != null)
                ? penggunaAktif.getNamaLengkap() + " (" + penggunaAktif.getRole() + ")"
                : "Tamu";
        userProfileLabel.setText(userDisplay);
        userProfileLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        userProfileLabel.setForeground(new Color(30, 41, 59));
        rightBar.add(userProfileLabel);

        // Tombol Keluar bergaya referensi visual
        JButton btnKeluar = new JButton("Keluar");
        btnKeluar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnKeluar.setForeground(new Color(71, 85, 105));
        btnKeluar.setBackground(Color.WHITE);
        btnKeluar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnKeluar.setFocusPainted(false);
        btnKeluar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnKeluar.addActionListener(e -> {
            AuthController.logout();
            dispose();
            new LoginView().setVisible(true);
        });
        rightBar.add(btnKeluar);

        headerBar.add(rightBar, BorderLayout.EAST);

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
            button.setForeground(isActive ? BTN_ACTIVE_TEXT : BTN_TEXT);
            button.setFont(new Font("SansSerif", isActive ? Font.BOLD : Font.PLAIN, 13));
            button.repaint();
        }
    }
}
