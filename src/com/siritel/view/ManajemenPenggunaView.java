package com.siritel.view;

import com.siritel.controller.AuthController;
import com.siritel.model.Pengguna;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Halaman Manajemen Pengguna (CRUD akun Admin & Kasir).
 */
public class ManajemenPenggunaView extends JPanel {
    private final AuthController authController = new AuthController();

    private final DefaultTableModel tableModel;
    private final JTable tablePengguna;

    private final JTextField txtId = new JTextField();
    private final JTextField txtUsername = new JTextField(18);
    private final JTextField txtNamaLengkap = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JRadioButton rbAdmin = new JRadioButton("Admin");
    private final JRadioButton rbKasir = new JRadioButton("Kasir");
    private final ButtonGroup bgRole = new ButtonGroup();

    private final JButton btnTambah = new JButton("Tambah");
    private final JButton btnUbah = new JButton("Ubah");
    private final JButton btnHapus = new JButton("Hapus");
    private final JButton btnBersihkan = new JButton("Bersihkan");

    public ManajemenPenggunaView() {
        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        // 1. Inisialisasi Tabel di Bagian Tengah
        String[] columnNames = {"ID", "Username", "Nama Lengkap", "Role"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablePengguna = new JTable(tableModel);
        tablePengguna.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePengguna.setRowHeight(26);
        tablePengguna.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tablePengguna.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JScrollPane scrollPane = new JScrollPane(tablePengguna);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        add(scrollPane, BorderLayout.CENTER);

        // 2. Inisialisasi Panel Form di Bagian Kanan
        JPanel formCard = buildFormPanel();
        add(formCard, BorderLayout.EAST);

        // 3. Event Listener Klik Tabel untuk mengisi Form
        tablePengguna.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = tablePengguna.getSelectedRow();
                if (selectedRow >= 0) {
                    txtId.setText(tableModel.getValueAt(selectedRow, 0).toString());
                    txtUsername.setText(tableModel.getValueAt(selectedRow, 1).toString());
                    txtNamaLengkap.setText(tableModel.getValueAt(selectedRow, 2).toString());
                    txtPassword.setText("");

                    String role = tableModel.getValueAt(selectedRow, 3).toString();
                    if ("Admin".equalsIgnoreCase(role)) {
                        rbAdmin.setSelected(true);
                    } else {
                        rbKasir.setSelected(true);
                    }
                }
            }
        });

        // 4. Action Listener Tombol CRUD
        initActions();

        // 5. Muat Data Awal
        muatDataTabel();
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(340, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Form Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel lblFormTitle = new JLabel("Form Pengguna");
        lblFormTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblFormTitle.setForeground(new Color(30, 41, 59));
        panel.add(lblFormTitle, gbc);

        // Username
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        JLabel lblUser = new JLabel("Username:");
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(lblUser, gbc);

        gbc.gridx = 1;
        panel.add(txtUsername, gbc);

        // Nama Lengkap
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel lblNama = new JLabel("Nama Lengkap:");
        lblNama.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(lblNama, gbc);

        gbc.gridx = 1;
        panel.add(txtNamaLengkap, gbc);

        // Password
        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel lblPass = new JLabel("Password:");
        lblPass.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(lblPass, gbc);

        gbc.gridx = 1;
        panel.add(txtPassword, gbc);

        // Role (JRadioButton dalam ButtonGroup)
        gbc.gridy = 4;
        gbc.gridx = 0;
        JLabel lblRole = new JLabel("Role:");
        lblRole.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(lblRole, gbc);

        gbc.gridx = 1;
        bgRole.add(rbAdmin);
        bgRole.add(rbKasir);
        rbAdmin.setBackground(Color.WHITE);
        rbKasir.setBackground(Color.WHITE);
        rbAdmin.setSelected(true);

        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rolePanel.setBackground(Color.WHITE);
        rolePanel.add(rbAdmin);
        rolePanel.add(rbKasir);
        panel.add(rolePanel, gbc);

        // Keterangan password saat ubah
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JLabel lblNote = new JLabel("*(Saat ubah: kosongkan jika tidak ganti password)");
        lblNote.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblNote.setForeground(new Color(100, 116, 139));
        panel.add(lblNote, gbc);

        // Action Buttons Grid
        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 2, 2, 2);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        buttonPanel.setOpaque(false);

        formatButton(btnTambah, new Color(37, 99, 235), Color.WHITE);
        formatButton(btnUbah, new Color(13, 148, 136), Color.WHITE);
        formatButton(btnHapus, new Color(220, 38, 38), Color.WHITE);
        formatButton(btnBersihkan, new Color(100, 116, 139), Color.WHITE);

        buttonPanel.add(btnTambah);
        buttonPanel.add(btnUbah);
        buttonPanel.add(btnHapus);
        buttonPanel.add(btnBersihkan);
        panel.add(buttonPanel, gbc);

        return panel;
    }

    private void formatButton(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(80, 32));
    }

    private void initActions() {
        btnTambah.addActionListener(e -> {
            String username = txtUsername.getText();
            String namaLengkap = txtNamaLengkap.getText();
            String password = new String(txtPassword.getPassword());
            String role = rbAdmin.isSelected() ? "Admin" : "Kasir";

            // FASE-1: tanpa penanganan eksepsi
            Pengguna p = new Pengguna(0, username, password, namaLengkap, role);
            try {
                authController.tambahPengguna(p);
                JOptionPane.showMessageDialog(this, "Pengguna berhasil ditambahkan.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                muatDataTabel();
                bersihkanForm();
            } catch (SQLException ex) {
                // FASE-1: tanpa penanganan eksepsi
                ex.printStackTrace();
            }
        });

        btnUbah.addActionListener(e -> {
            // FASE-1: tanpa penanganan eksepsi
            int id = Integer.parseInt(txtId.getText());
            String username = txtUsername.getText();
            String namaLengkap = txtNamaLengkap.getText();
            String password = new String(txtPassword.getPassword());
            String role = rbAdmin.isSelected() ? "Admin" : "Kasir";
            boolean gantiPassword = !password.isEmpty();

            // FASE-1: tanpa penanganan eksepsi
            Pengguna p = new Pengguna(id, username, password, namaLengkap, role);
            try {
                authController.ubahPengguna(p, gantiPassword);
                JOptionPane.showMessageDialog(this, "Pengguna berhasil diperbarui.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                muatDataTabel();
                bersihkanForm();
            } catch (SQLException ex) {
                // FASE-1: tanpa penanganan eksepsi
                ex.printStackTrace();
            }
        });

        btnHapus.addActionListener(e -> {
            // FASE-1: tanpa penanganan eksepsi
            int id = Integer.parseInt(txtId.getText());

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Apakah Anda yakin ingin menghapus pengguna dengan ID " + id + "?",
                    "Konfirmasi Hapus Pengguna",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    authController.hapusPengguna(id);
                    JOptionPane.showMessageDialog(this, "Pengguna berhasil dihapus.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    muatDataTabel();
                    bersihkanForm();
                } catch (SQLException ex) {
                    // FASE-1: tanpa penanganan eksepsi
                    ex.printStackTrace();
                }
            }
        });

        btnBersihkan.addActionListener(e -> bersihkanForm());
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
            // FASE-1: tanpa penanganan eksepsi
            ex.printStackTrace();
        }
    }

    private void bersihkanForm() {
        txtId.setText("");
        txtUsername.setText("");
        txtNamaLengkap.setText("");
        txtPassword.setText("");
        rbAdmin.setSelected(true);
        tablePengguna.clearSelection();
    }
}
