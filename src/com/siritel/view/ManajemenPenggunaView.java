package com.siritel.view;

import com.siritel.controller.AuthController;
import com.siritel.model.Pengguna;
import com.siritel.view.UiStyleSupport.ButtonVariant;
import com.siritel.view.UiStyleSupport.RoundedBorder;
import com.siritel.view.UiStyleSupport.RoundedButton;
import com.siritel.view.UiStyleSupport.ToastManager;
import com.siritel.view.UiStyleSupport.ToastType;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Halaman Manajemen Pengguna dengan tata letak dinamis seragam dengan ManajemenProdukView:
 * tabel utama melebar penuh secara default, dan panel kanan Tambah/Ubah muncul saat diperlukan.
 */
public class ManajemenPenggunaView extends JPanel {
    private static final Color BADGE_ADMIN_BG = new Color(219, 234, 254);
    private static final Color BADGE_ADMIN_FG = new Color(29, 78, 216);
    private static final Color BADGE_KASIR_BG = new Color(204, 251, 241);
    private static final Color BADGE_KASIR_FG = new Color(15, 118, 110);

    private final AuthController authController = new AuthController();
    private final ToastManager toastManager = new ToastManager(this);
    private List<Pengguna> daftarPenggunaSaatIni = new ArrayList<>();
    private Pengguna penggunaTerpilih;
    private boolean isRefreshingTable = false;

    // Panel Kiri (Tabel Akun Pengguna)
    private final DefaultTableModel tableModel;
    private final JTable tablePengguna;
    private final JLabel lblRingkasanTabel = new JLabel("Menampilkan 0 pengguna");
    private final RoundedButton btnTambah = new RoundedButton("+ Tambah Pengguna", ButtonVariant.PRIMARY, 10);
    private final RoundedButton btnSegarkan = new RoundedButton("Segarkan", ButtonVariant.OUTLINE, 10);

    // Panel Kanan (Form Dinamis Tambah / Ubah Pengguna)
    private final JPanel sideCard;
    private final JLabel lblFormTitle = new JLabel("Tambah Pengguna");
    private final JLabel lblFormSubtitle = new JLabel("Buat kredensial akun kasir atau admin baru");
    private final RoundedButton btnTutupPanel = new RoundedButton("\u00D7", ButtonVariant.GHOST, 8);
    private final JTextField txtUsername = new JTextField();
    private final JTextField txtNamaLengkap = new JTextField();
    private final JRadioButton rbAdmin = new JRadioButton("Admin");
    private final JRadioButton rbKasir = new JRadioButton("Kasir");
    private final ButtonGroup bgRole = new ButtonGroup();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JLabel lblPassNote = new JLabel("Minimal 6 karakter");
    private final char defaultEchoChar;
    private boolean isPasswordVisible = false;
    private final JButton btnEye = createEyeToggleButton();

    private final RoundedButton btnHapusForm = new RoundedButton("Hapus", ButtonVariant.DANGER, 10);
    private final RoundedButton btnBatal = new RoundedButton("Batal", ButtonVariant.OUTLINE, 10);
    private final RoundedButton btnSimpan = new RoundedButton("Simpan", ButtonVariant.PRIMARY, 10);

    public ManajemenPenggunaView() {
        setLayout(new BorderLayout(14, 0));
        setBackground(UiStyleSupport.BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        defaultEchoChar = txtPassword.getEchoChar();

        // 1. Inisialisasi Tabel Pengguna
        String[] columnNames = {"ID", "Username", "Nama Lengkap", "Role"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablePengguna = new JTable(tableModel);
        konfigurasiTabelPengguna();

        // 2. Susun Kartu Kiri (Daftar) & Kartu Kanan (Form Dinamis, tersembunyi di awal)
        add(buildTableCard(), BorderLayout.CENTER);
        sideCard = buildSideEditorCard();
        sideCard.setVisible(false);
        add(sideCard, BorderLayout.EAST);

        // 3. Event Handlers & Muat Data Awal
        initActions();
        tutupSidePanel();
        muatDataTabel();
    }

    private JPanel buildTableCard() {
        JPanel mainCard = UiStyleSupport.createRoundedCardPanel(new BorderLayout(0, 12), 14);
        mainCard.setBorder(BorderFactory.createEmptyBorder(15, 16, 14, 16));

        mainCard.add(buildTopToolbar(), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(tablePengguna);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBorder(new RoundedBorder(UiStyleSupport.BORDER_COLOR, 10, new Insets(1, 1, 1, 1)));
        mainCard.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        lblRingkasanTabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblRingkasanTabel.setForeground(UiStyleSupport.TEXT_MUTED);
        footer.add(lblRingkasanTabel, BorderLayout.WEST);
        mainCard.add(footer, BorderLayout.SOUTH);

        return mainCard;
    }

    private JPanel buildTopToolbar() {
        JPanel toolbar = new JPanel(new GridBagLayout());
        toolbar.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;

        JLabel lblSection = new JLabel("Daftar Akun Pengguna");
        lblSection.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSection.setForeground(UiStyleSupport.TEXT_DARK);
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        toolbar.add(lblSection, gbc);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);

        konfigurasiTombol(btnTambah, 156, 34, Font.BOLD);
        konfigurasiTombol(btnSegarkan, 84, 34, Font.PLAIN);

        rightPanel.add(btnTambah);
        rightPanel.add(Box.createHorizontalStrut(8));
        rightPanel.add(btnSegarkan);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        toolbar.add(rightPanel, gbc);
        return toolbar;
    }

    private JPanel buildSideEditorCard() {
        JPanel sidePanel = UiStyleSupport.createRoundedCardPanel(new BorderLayout(), 14);
        sidePanel.setPreferredSize(new Dimension(320, 0));
        sidePanel.setMinimumSize(new Dimension(300, 0));

        // Header Panel Kanan
        JPanel headerWrapper = new JPanel(new BorderLayout(8, 0));
        headerWrapper.setOpaque(false);
        headerWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiStyleSupport.BORDER_COLOR),
                BorderFactory.createEmptyBorder(14, 16, 12, 12)
        ));

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 2));
        titleGroup.setOpaque(false);
        lblFormTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblFormTitle.setForeground(UiStyleSupport.TEXT_DARK);
        lblFormSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblFormSubtitle.setForeground(UiStyleSupport.TEXT_MUTED);
        titleGroup.add(lblFormTitle);
        titleGroup.add(lblFormSubtitle);
        headerWrapper.add(titleGroup, BorderLayout.CENTER);

        btnTutupPanel.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnTutupPanel.setPreferredSize(new Dimension(28, 28));
        btnTutupPanel.setToolTipText("Tutup panel");
        JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeWrap.setOpaque(false);
        closeWrap.add(btnTutupPanel);
        headerWrapper.add(closeWrap, BorderLayout.EAST);

        sidePanel.add(headerWrapper, BorderLayout.NORTH);

        // Isi Form di ScrollPane Vertikal
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiStyleSupport.CARD_BG);
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;

        // 1. Username
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(createFieldLabel("Username *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 10, 0);
        styleInputField(txtUsername);
        form.add(txtUsername, gbc);

        // 2. Nama Lengkap
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(createFieldLabel("Nama Lengkap *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 10, 0);
        styleInputField(txtNamaLengkap);
        form.add(txtNamaLengkap, gbc);

        // 3. Role Akses (JRadioButton dalam ButtonGroup sesuai AGENTS.md)
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 5, 0);
        form.add(createFieldLabel("Role Akses *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 10, 0);
        bgRole.add(rbAdmin);
        bgRole.add(rbKasir);
        rbAdmin.setOpaque(false);
        rbKasir.setOpaque(false);
        rbAdmin.setFont(new Font("SansSerif", Font.PLAIN, 13));
        rbKasir.setFont(new Font("SansSerif", Font.PLAIN, 13));
        rbAdmin.setForeground(UiStyleSupport.TEXT_DARK);
        rbKasir.setForeground(UiStyleSupport.TEXT_DARK);
        rbAdmin.setFocusPainted(false);
        rbKasir.setFocusPainted(false);
        rbAdmin.setSelected(true);

        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        rolePanel.setOpaque(false);
        rolePanel.add(rbAdmin);
        rolePanel.add(rbKasir);
        form.add(rolePanel, gbc);

        // 4. Password
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(createFieldLabel("Password *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 4, 0);
        JPanel passPanel = new JPanel(new BorderLayout(4, 0));
        passPanel.setBackground(Color.WHITE);
        passPanel.setBorder(new RoundedBorder(UiStyleSupport.INPUT_BORDER, 8, new Insets(1, 4, 1, 6)));

        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtPassword.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 4));
        txtPassword.setOpaque(false);
        txtPassword.setPreferredSize(new Dimension(0, 30));
        passPanel.add(txtPassword, BorderLayout.CENTER);
        passPanel.add(btnEye, BorderLayout.EAST);
        form.add(passPanel, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 16, 0);
        lblPassNote.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblPassNote.setForeground(UiStyleSupport.TEXT_MUTED);
        form.add(lblPassNote, gbc);

        // 5. Tombol Aksi Form (Hapus | Batal | Simpan)
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 0, 0, 0);
        JPanel btnRow = new JPanel(new BorderLayout(6, 0));
        btnRow.setOpaque(false);

        konfigurasiTombol(btnHapusForm, 70, 34, Font.BOLD);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightBtns.setOpaque(false);
        konfigurasiTombol(btnBatal, 70, 34, Font.PLAIN);
        konfigurasiTombol(btnSimpan, 86, 34, Font.BOLD);
        rightBtns.add(btnBatal);
        rightBtns.add(btnSimpan);

        btnRow.add(btnHapusForm, BorderLayout.WEST);
        btnRow.add(rightBtns, BorderLayout.EAST);
        form.add(btnRow, gbc);

        // Filler vertikal agar form menempel ke atas
        gbc.gridy = row;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        form.add(spacer, gbc);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.getViewport().setBackground(UiStyleSupport.CARD_BG);
        sidePanel.add(scroll, BorderLayout.CENTER);

        return sidePanel;
    }

    private JButton createEyeToggleButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UiStyleSupport.TEXT_MUTED);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.drawOval(cx - 7, cy - 4, 14, 8);
                g2.fillOval(cx - 2, cy - 2, 4, 4);
                if (isPasswordVisible) {
                    g2.drawLine(cx - 8, cy + 5, cx + 8, cy - 5);
                }
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(28, 28));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Tampilkan / Sembunyikan Password");
        btn.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible;
            txtPassword.setEchoChar(isPasswordVisible ? (char) 0 : defaultEchoChar);
            btn.repaint();
        });
        return btn;
    }

    private void konfigurasiTabelPengguna() {
        tablePengguna.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePengguna.setRowHeight(42);
        tablePengguna.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tablePengguna.setForeground(UiStyleSupport.TEXT_DARK);
        tablePengguna.setGridColor(new Color(241, 245, 249));
        tablePengguna.setSelectionBackground(UiStyleSupport.PRIMARY_BLUE_SOFT);
        tablePengguna.setSelectionForeground(UiStyleSupport.TEXT_DARK);
        tablePengguna.setShowVerticalLines(false);
        tablePengguna.getTableHeader().setReorderingAllowed(false);

        tablePengguna.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tablePengguna.getTableHeader().setBackground(new Color(248, 250, 252));
        tablePengguna.getTableHeader().setForeground(UiStyleSupport.TEXT_MUTED);
        tablePengguna.getTableHeader().setPreferredSize(new Dimension(0, 38));
        tablePengguna.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiStyleSupport.BORDER_COLOR)
        );

        DefaultTableCellRenderer idRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("SansSerif", Font.BOLD, 12));
                setForeground(UiStyleSupport.TEXT_MUTED);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        };

        DefaultTableCellRenderer usernameRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setFont(new Font("SansSerif", Font.BOLD, 12));
                setForeground(isSelected ? UiStyleSupport.PRIMARY_BLUE : UiStyleSupport.TEXT_DARK);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        };

        DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer();
        textRenderer.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        DefaultTableCellRenderer roleRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                String role = value != null ? value.toString() : "";
                JLabel badge = new JLabel(role, SwingConstants.CENTER);
                badge.setFont(new Font("SansSerif", Font.BOLD, 11));
                badge.setOpaque(true);
                badge.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));

                if ("Admin".equalsIgnoreCase(role)) {
                    badge.setBackground(BADGE_ADMIN_BG);
                    badge.setForeground(BADGE_ADMIN_FG);
                } else {
                    badge.setBackground(BADGE_KASIR_BG);
                    badge.setForeground(BADGE_KASIR_FG);
                }

                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 9));
                wrapper.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                wrapper.add(badge);
                return wrapper;
            }
        };

        tablePengguna.getColumnModel().getColumn(0).setCellRenderer(idRenderer);
        tablePengguna.getColumnModel().getColumn(1).setCellRenderer(usernameRenderer);
        tablePengguna.getColumnModel().getColumn(2).setCellRenderer(textRenderer);
        tablePengguna.getColumnModel().getColumn(3).setCellRenderer(roleRenderer);

        tablePengguna.getColumnModel().getColumn(0).setPreferredWidth(56);
        tablePengguna.getColumnModel().getColumn(0).setMaxWidth(72);
        tablePengguna.getColumnModel().getColumn(1).setPreferredWidth(180);
        tablePengguna.getColumnModel().getColumn(2).setPreferredWidth(250);
        tablePengguna.getColumnModel().getColumn(3).setPreferredWidth(120);
    }

    private void initActions() {
        tablePengguna.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || isRefreshingTable) {
                return;
            }
            int selectedRow = tablePengguna.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < daftarPenggunaSaatIni.size()) {
                masukModeUbah(daftarPenggunaSaatIni.get(selectedRow));
                tampilkanSidePanel();
            }
        });

        btnTambah.addActionListener(e -> {
            isRefreshingTable = true;
            try {
                tablePengguna.clearSelection();
            } finally {
                isRefreshingTable = false;
            }
            masukModeTambah();
            tampilkanSidePanel();
            txtUsername.requestFocusInWindow();
        });

        btnSegarkan.addActionListener(e -> muatDataTabel());
        btnTutupPanel.addActionListener(e -> tutupSidePanel());
        btnBatal.addActionListener(e -> tutupSidePanel());
        btnSimpan.addActionListener(e -> simpanPengguna());
        btnHapusForm.addActionListener(e -> hapusPenggunaTerpilih());
    }

    private void tampilkanSidePanel() {
        if (!sideCard.isVisible()) {
            sideCard.setVisible(true);
            revalidate();
            repaint();
        }
    }

    private void tutupSidePanel() {
        isRefreshingTable = true;
        try {
            tablePengguna.clearSelection();
        } finally {
            isRefreshingTable = false;
        }
        masukModeTambah();
        if (sideCard.isVisible()) {
            sideCard.setVisible(false);
            revalidate();
            repaint();
        }
    }

    private void masukModeTambah() {
        penggunaTerpilih = null;
        lblFormTitle.setText("Tambah Pengguna");
        lblFormSubtitle.setText("Buat kredensial akun kasir atau admin baru");
        btnHapusForm.setVisible(false);

        txtUsername.setText("");
        txtNamaLengkap.setText("");
        txtPassword.setText("");
        resetVisibilityPassword();
        rbAdmin.setSelected(true);
        lblPassNote.setText("Minimal 6 karakter");
    }

    private void masukModeUbah(Pengguna p) {
        penggunaTerpilih = p;
        lblFormTitle.setText("Ubah Pengguna");
        lblFormSubtitle.setText(p.getUsername() + " \u2022 " + p.getNamaLengkap());
        btnHapusForm.setVisible(true);

        txtUsername.setText(p.getUsername());
        txtNamaLengkap.setText(p.getNamaLengkap());
        txtPassword.setText("");
        resetVisibilityPassword();
        if ("Admin".equalsIgnoreCase(p.getRole())) {
            rbAdmin.setSelected(true);
        } else {
            rbKasir.setSelected(true);
        }
        lblPassNote.setText("*(Kosongkan jika tidak ingin mengganti password)");
    }

    private void resetVisibilityPassword() {
        isPasswordVisible = false;
        txtPassword.setEchoChar(defaultEchoChar);
        btnEye.repaint();
    }

    private void simpanPengguna() {
        String username = txtUsername.getText();
        String namaLengkap = txtNamaLengkap.getText();
        String password = new String(txtPassword.getPassword());
        String role = rbAdmin.isSelected() ? "Admin" : "Kasir";

        try {
            if (penggunaTerpilih == null) {
                Pengguna baru = new Pengguna(0, username, password, namaLengkap, role);
                authController.tambahPengguna(baru);
                tutupSidePanel();
                muatDataTabel();
                toastManager.show(
                        "Pengguna Ditambahkan",
                        "Akun '" + baru.getUsername() + "' berhasil dibuat.",
                        ToastType.SUCCESS
                );
            } else {
                boolean gantiPassword = !password.isEmpty();
                Pengguna diperbarui = new Pengguna(
                        penggunaTerpilih.getId(),
                        username,
                        gantiPassword ? password : null,
                        namaLengkap,
                        role
                );
                authController.ubahPengguna(diperbarui, gantiPassword);
                tutupSidePanel();
                muatDataTabel();
                toastManager.show(
                        "Pengguna Diperbarui",
                        "Data akun '" + diperbarui.getUsername() + "' berhasil diperbarui.",
                        ToastType.SUCCESS
                );
            }
        } catch (IllegalArgumentException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Validasi", ex.getMessage());
        } catch (SQLException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Database", AuthController.pesanSqlError(ex));
        }
    }

    private void hapusPenggunaTerpilih() {
        if (penggunaTerpilih == null) {
            toastManager.show(
                    "Pengguna Belum Dipilih",
                    "Pilih baris pengguna yang ingin dihapus terlebih dahulu.",
                    ToastType.INFO
            );
            return;
        }

        int id = penggunaTerpilih.getId();
        String username = penggunaTerpilih.getUsername();

        boolean disetujui = UiStyleSupport.showConfirmModal(
                this,
                "Hapus Pengguna",
                "Yakin ingin menghapus akun '" + username + "' (ID: " + id + ")?",
                "Ya, Hapus"
        );

        if (!disetujui) {
            return;
        }

        try {
            authController.hapusPengguna(id);
            tutupSidePanel();
            muatDataTabel();
            toastManager.show(
                    "Pengguna Dihapus",
                    "Akun '" + username + "' berhasil dihapus.",
                    ToastType.SUCCESS
            );
        } catch (IllegalArgumentException ex) {
            UiStyleSupport.showErrorModal(this, "Aksi Ditolak", ex.getMessage());
        } catch (SQLException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Database", AuthController.pesanSqlError(ex));
        }
    }

    private void muatDataTabel() {
        isRefreshingTable = true;
        try {
            Integer selectedId = (penggunaTerpilih != null && sideCard.isVisible())
                    ? penggunaTerpilih.getId()
                    : null;

            daftarPenggunaSaatIni = new ArrayList<>();
            tableModel.setRowCount(0);

            List<Pengguna> list = authController.daftarPengguna();
            daftarPenggunaSaatIni.addAll(list);

            int indexToSelect = -1;
            for (int i = 0; i < daftarPenggunaSaatIni.size(); i++) {
                Pengguna p = daftarPenggunaSaatIni.get(i);
                tableModel.addRow(new Object[]{
                        p.getId(),
                        p.getUsername(),
                        p.getNamaLengkap(),
                        p.getRole()
                });
                if (selectedId != null && p.getId() == selectedId) {
                    indexToSelect = i;
                }
            }

            lblRingkasanTabel.setText("Menampilkan " + daftarPenggunaSaatIni.size() + " pengguna");

            if (indexToSelect >= 0) {
                tablePengguna.setRowSelectionInterval(indexToSelect, indexToSelect);
                masukModeUbah(daftarPenggunaSaatIni.get(indexToSelect));
            } else if (selectedId != null) {
                masukModeTambah();
                sideCard.setVisible(false);
                revalidate();
                repaint();
            }
        } catch (SQLException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Database", AuthController.pesanSqlError(ex));
        } finally {
            isRefreshingTable = false;
        }
    }

    private static void konfigurasiTombol(RoundedButton btn, int width, int height, int fontStyle) {
        btn.setFont(new Font("SansSerif", fontStyle, 12));
        Dimension size = new Dimension(width, height);
        btn.setPreferredSize(size);
        btn.setMinimumSize(size);
        btn.setMaximumSize(size);
    }

    private static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(UiStyleSupport.TEXT_SLATE);
        return label;
    }

    private static void styleInputField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 12));
        field.setPreferredSize(new Dimension(0, 32));
        field.setBorder(new RoundedBorder(UiStyleSupport.INPUT_BORDER, 8, new Insets(4, 10, 4, 10)));
    }
}
