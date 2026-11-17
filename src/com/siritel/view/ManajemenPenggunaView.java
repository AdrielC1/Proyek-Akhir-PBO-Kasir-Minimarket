package com.siritel.view;

import com.siritel.controller.AuthController;
import com.siritel.model.Pengguna;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Halaman Manajemen Pengguna dengan penanganan eksepsi lengkap (Fase 2).
 */
public class ManajemenPenggunaView extends JPanel {
    private static final Color BG_PANEL = new Color(248, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color DANGER_RED = new Color(220, 38, 38);
    private static final Color TEAL_COLOR = new Color(13, 148, 136);
    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    private final AuthController authController = new AuthController();
    private final DefaultTableModel tableModel;
    private final JTable tablePengguna;

    private final JButton btnTambah = new JButton("+ Tambah Pengguna");
    private final JButton btnUbah = new JButton("Ubah");
    private final JButton btnHapus = new JButton("Hapus");
    private final JButton btnSegarkan = new JButton("Segarkan");

    public ManajemenPenggunaView() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        // 1. Toolbar Atas
        JPanel topToolbar = buildTopToolbar();
        add(topToolbar, BorderLayout.NORTH);

        // 2. Tabel Pengguna di Tengah
        String[] columnNames = {"ID", "Username", "Nama Lengkap", "Role"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablePengguna = new JTable(tableModel);
        tablePengguna.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePengguna.setRowHeight(38);
        tablePengguna.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tablePengguna.setForeground(TEXT_DARK);
        tablePengguna.setGridColor(new Color(241, 245, 249));
        tablePengguna.setSelectionBackground(new Color(239, 246, 255));
        tablePengguna.setSelectionForeground(TEXT_DARK);
        tablePengguna.setShowVerticalLines(false);

        // Styling Header Tabel
        tablePengguna.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablePengguna.getTableHeader().setBackground(new Color(248, 250, 252));
        tablePengguna.getTableHeader().setForeground(TEXT_MUTED);
        tablePengguna.getTableHeader().setPreferredSize(new Dimension(0, 38));
        tablePengguna.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        for (int i = 0; i < tablePengguna.getColumnCount(); i++) {
            tablePengguna.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(tablePengguna);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Inisialisasi Event Handlers
        initActions();

        // 4. Muat Data Awal
        muatDataTabel();
    }

    private JPanel buildTopToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        leftPanel.setOpaque(false);
        JLabel lblSection = new JLabel("Daftar Akun Pengguna");
        lblSection.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSection.setForeground(TEXT_DARK);
        leftPanel.add(lblSection);
        toolbar.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        btnTambah.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnTambah.setBackground(PRIMARY_BLUE);
        btnTambah.setForeground(Color.WHITE);
        btnTambah.setFocusPainted(false);
        btnTambah.setBorderPainted(false);
        btnTambah.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTambah.setPreferredSize(new Dimension(160, 36));

        btnUbah.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnUbah.setBackground(TEAL_COLOR);
        btnUbah.setForeground(Color.WHITE);
        btnUbah.setFocusPainted(false);
        btnUbah.setBorderPainted(false);
        btnUbah.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnUbah.setPreferredSize(new Dimension(84, 36));

        btnHapus.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnHapus.setBackground(DANGER_RED);
        btnHapus.setForeground(Color.WHITE);
        btnHapus.setFocusPainted(false);
        btnHapus.setBorderPainted(false);
        btnHapus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnHapus.setPreferredSize(new Dimension(84, 36));

        btnSegarkan.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnSegarkan.setBackground(Color.WHITE);
        btnSegarkan.setForeground(new Color(71, 85, 105));
        btnSegarkan.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnSegarkan.setFocusPainted(false);
        btnSegarkan.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        rightPanel.add(btnTambah);
        rightPanel.add(btnUbah);
        rightPanel.add(btnHapus);
        rightPanel.add(btnSegarkan);

        toolbar.add(rightPanel, BorderLayout.EAST);
        return toolbar;
    }

    private void initActions() {
        // Aksi Tambah Pengguna
        btnTambah.addActionListener(e -> {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            PenggunaFormDialog dialog = new PenggunaFormDialog(parentFrame, "Tambah Pengguna", null);
            dialog.setVisible(true);

            if (dialog.isSaved()) {
                String username = dialog.getUsername();
                String namaLengkap = dialog.getNamaLengkap();
                String password = dialog.getPassword();
                String role = dialog.getRole();

                try {
                    Pengguna p = new Pengguna(0, username, password, namaLengkap, role);
                    authController.tambahPengguna(p);
                    JOptionPane.showMessageDialog(
                            this,
                            "Pengguna berhasil ditambahkan.",
                            "Sukses",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    muatDataTabel();
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(
                            this,
                            ex.getMessage(),
                            "Kesalahan Validasi",
                            JOptionPane.ERROR_MESSAGE
                    );
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(
                            this,
                            AuthController.pesanSqlError(ex),
                            "Kesalahan Database",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        // Aksi Ubah Pengguna
        btnUbah.addActionListener(e -> {
            int selectedRow = tablePengguna.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Pilih baris pengguna yang ingin diubah terlebih dahulu.",
                        "Peringatan",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int id = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());
            String curUsername = tableModel.getValueAt(selectedRow, 1).toString();
            String curNama = tableModel.getValueAt(selectedRow, 2).toString();
            String curRole = tableModel.getValueAt(selectedRow, 3).toString();

            Pengguna existing = new Pengguna(id, curUsername, null, curNama, curRole);
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            PenggunaFormDialog dialog = new PenggunaFormDialog(parentFrame, "Ubah Pengguna", existing);
            dialog.setVisible(true);

            if (dialog.isSaved()) {
                String username = dialog.getUsername();
                String namaLengkap = dialog.getNamaLengkap();
                String password = dialog.getPassword();
                String role = dialog.getRole();
                boolean gantiPassword = !password.isEmpty();

                try {
                    Pengguna p = new Pengguna(id, username, gantiPassword ? password : null, namaLengkap, role);
                    authController.ubahPengguna(p, gantiPassword);
                    JOptionPane.showMessageDialog(
                            this,
                            "Pengguna berhasil diperbarui.",
                            "Sukses",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    muatDataTabel();
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(
                            this,
                            ex.getMessage(),
                            "Kesalahan Validasi",
                            JOptionPane.ERROR_MESSAGE
                    );
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(
                            this,
                            AuthController.pesanSqlError(ex),
                            "Kesalahan Database",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        // Aksi Hapus Pengguna
        btnHapus.addActionListener(e -> {
            int selectedRow = tablePengguna.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Pilih baris pengguna yang ingin dihapus terlebih dahulu.",
                        "Peringatan",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int id = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());
            String username = tableModel.getValueAt(selectedRow, 1).toString();

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Apakah Anda yakin ingin menghapus pengguna '" + username + "' (ID: " + id + ")?",
                    "Konfirmasi Hapus Pengguna",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    authController.hapusPengguna(id);
                    JOptionPane.showMessageDialog(
                            this,
                            "Pengguna berhasil dihapus.",
                            "Sukses",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    muatDataTabel();
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(
                            this,
                            ex.getMessage(),
                            "Aksi Ditolak",
                            JOptionPane.ERROR_MESSAGE
                    );
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(
                            this,
                            AuthController.pesanSqlError(ex),
                            "Kesalahan Database",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        btnSegarkan.addActionListener(e -> muatDataTabel());
    }

    private void muatDataTabel() {
        tableModel.setRowCount(0);
        try {
            List<Pengguna> list = authController.daftarPengguna();
            for (Pengguna p : list) {
                tableModel.addRow(new Object[]{
                        p.getId(),
                        p.getUsername(),
                        p.getNamaLengkap(),
                        p.getRole()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    AuthController.pesanSqlError(ex),
                    "Kesalahan Database",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Form Dialog Modal untuk Tambah dan Ubah Pengguna.
     */
    private static class PenggunaFormDialog extends JDialog {
        private final JTextField txtUsername = new JTextField();
        private final JTextField txtNamaLengkap = new JTextField();
        private final JPasswordField txtPassword = new JPasswordField();
        private final JRadioButton rbAdmin = new JRadioButton("Admin");
        private final JRadioButton rbKasir = new JRadioButton("Kasir");
        private final ButtonGroup bgRole = new ButtonGroup();
        private final char defaultEchoChar;
        private boolean isPasswordVisible = false;
        private boolean saved = false;

        public PenggunaFormDialog(Frame owner, String title, Pengguna pengguna) {
            super(owner, title, true);
            setSize(440, 520);
            setLocationRelativeTo(owner);
            setResizable(false);
            setLayout(new BorderLayout());

            JPanel mainCard = new JPanel(new GridBagLayout());
            mainCard.setBackground(Color.WHITE);
            mainCard.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, 0, 0);

            // 1. Header Judul Dialog & Subtitle
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            JLabel lblDialogTitle = new JLabel(title);
            lblDialogTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
            lblDialogTitle.setForeground(TEXT_DARK);
            mainCard.add(lblDialogTitle, gbc);

            gbc.gridy = 1;
            gbc.insets = new Insets(2, 0, 18, 0);
            String subtitle = (pengguna == null)
                    ? "Buat kredensial akun kasir atau administrator baru"
                    : "Perbarui data kredensial akun pengguna";
            JLabel lblSubtitle = new JLabel(subtitle);
            lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblSubtitle.setForeground(TEXT_MUTED);
            mainCard.add(lblSubtitle, gbc);

            // 2. Username
            gbc.gridy = 2;
            gbc.insets = new Insets(0, 0, 6, 0);
            JLabel lblUser = new JLabel("Username *");
            lblUser.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblUser.setForeground(new Color(51, 65, 85));
            mainCard.add(lblUser, gbc);

            gbc.gridy = 3;
            gbc.insets = new Insets(0, 0, 14, 0);
            formatInputField(txtUsername);
            mainCard.add(txtUsername, gbc);

            // 3. Nama Lengkap
            gbc.gridy = 4;
            gbc.insets = new Insets(0, 0, 6, 0);
            JLabel lblNama = new JLabel("Nama Lengkap *");
            lblNama.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblNama.setForeground(new Color(51, 65, 85));
            mainCard.add(lblNama, gbc);

            gbc.gridy = 5;
            gbc.insets = new Insets(0, 0, 14, 0);
            formatInputField(txtNamaLengkap);
            mainCard.add(txtNamaLengkap, gbc);

            // 4. Role Akses (Adaptasi Wajib 1: JRadioButton dalam ButtonGroup)
            gbc.gridy = 6;
            gbc.insets = new Insets(0, 0, 6, 0);
            JLabel lblRole = new JLabel("Role Akses *");
            lblRole.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblRole.setForeground(new Color(51, 65, 85));
            mainCard.add(lblRole, gbc);

            gbc.gridy = 7;
            gbc.insets = new Insets(0, 0, 14, 0);
            bgRole.add(rbAdmin);
            bgRole.add(rbKasir);
            rbAdmin.setBackground(Color.WHITE);
            rbKasir.setBackground(Color.WHITE);
            rbAdmin.setFont(new Font("SansSerif", Font.PLAIN, 13));
            rbKasir.setFont(new Font("SansSerif", Font.PLAIN, 13));
            rbAdmin.setSelected(true);

            JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
            rolePanel.setBackground(Color.WHITE);
            rolePanel.add(rbAdmin);
            rolePanel.add(rbKasir);
            mainCard.add(rolePanel, gbc);

            // 5. Password
            gbc.gridy = 8;
            gbc.insets = new Insets(0, 0, 6, 0);
            JLabel lblPass = new JLabel("Password *");
            lblPass.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblPass.setForeground(new Color(51, 65, 85));
            mainCard.add(lblPass, gbc);

            gbc.gridy = 9;
            gbc.insets = new Insets(0, 0, 4, 0);

            JPanel passPanel = new JPanel(new BorderLayout(4, 0));
            passPanel.setBackground(Color.WHITE);
            passPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(2, 2, 2, 6)
            ));

            txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
            txtPassword.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 6));
            defaultEchoChar = txtPassword.getEchoChar();
            passPanel.add(txtPassword, BorderLayout.CENTER);

            JButton btnEye = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(100, 116, 139));
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
            btnEye.setPreferredSize(new Dimension(28, 28));
            btnEye.setFocusPainted(false);
            btnEye.setBorderPainted(false);
            btnEye.setContentAreaFilled(false);
            btnEye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEye.addActionListener(e -> {
                isPasswordVisible = !isPasswordVisible;
                if (isPasswordVisible) {
                    txtPassword.setEchoChar((char) 0);
                } else {
                    txtPassword.setEchoChar(defaultEchoChar);
                }
                btnEye.repaint();
            });
            passPanel.add(btnEye, BorderLayout.EAST);
            mainCard.add(passPanel, gbc);

            // Keterangan Password
            gbc.gridy = 10;
            gbc.insets = new Insets(0, 0, 20, 0);
            String noteText = (pengguna == null)
                    ? "Minimal 6 karakter"
                    : "*(Kosongkan jika tidak ingin mengganti password)";
            JLabel lblPassNote = new JLabel(noteText);
            lblPassNote.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lblPassNote.setForeground(new Color(148, 163, 184));
            mainCard.add(lblPassNote, gbc);

            // 6. Tombol Batal & Simpan
            gbc.gridy = 11;
            gbc.insets = new Insets(8, 0, 0, 0);
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnPanel.setBackground(Color.WHITE);

            JButton btnBatal = new JButton("Batal");
            btnBatal.setFont(new Font("SansSerif", Font.PLAIN, 13));
            btnBatal.setForeground(new Color(71, 85, 105));
            btnBatal.setBackground(Color.WHITE);
            btnBatal.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(8, 16, 8, 16)
            ));
            btnBatal.setFocusPainted(false);
            btnBatal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnBatal.addActionListener(e -> dispose());

            JButton btnSimpan = new JButton("Simpan");
            btnSimpan.setFont(new Font("SansSerif", Font.BOLD, 13));
            btnSimpan.setForeground(Color.WHITE);
            btnSimpan.setBackground(PRIMARY_BLUE);
            btnSimpan.setFocusPainted(false);
            btnSimpan.setBorderPainted(false);
            btnSimpan.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
            btnSimpan.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnSimpan.addActionListener(e -> {
                saved = true;
                dispose();
            });

            btnPanel.add(btnBatal);
            btnPanel.add(btnSimpan);
            mainCard.add(btnPanel, gbc);

            add(mainCard, BorderLayout.CENTER);

            // Pre-fill jika mode Ubah
            if (pengguna != null) {
                txtUsername.setText(pengguna.getUsername());
                txtNamaLengkap.setText(pengguna.getNamaLengkap());
                if ("Admin".equalsIgnoreCase(pengguna.getRole())) {
                    rbAdmin.setSelected(true);
                } else {
                    rbKasir.setSelected(true);
                }
            }
        }

        private void formatInputField(JTextField field) {
            field.setFont(new Font("SansSerif", Font.PLAIN, 13));
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            field.setPreferredSize(new Dimension(0, 36));
        }

        public boolean isSaved() {
            return saved;
        }

        public String getUsername() {
            return txtUsername.getText();
        }

        public String getNamaLengkap() {
            return txtNamaLengkap.getText();
        }

        public String getPassword() {
            return new String(txtPassword.getPassword());
        }

        public String getRole() {
            return rbAdmin.isSelected() ? "Admin" : "Kasir";
        }
    }
}
