package com.siritel.view;

import com.siritel.controller.AuthController;
import com.siritel.controller.ProdukController;
import com.siritel.controller.TransaksiController;
import com.siritel.exception.StokTidakCukupException;
import com.siritel.model.ItemTransaksi;
import com.siritel.model.Pengguna;
import com.siritel.model.Produk;
import com.siritel.model.Transaksi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Halaman kasir SI-RITEL.
 *
 * Tanggung jawab View dibatasi pada rendering komponen Swing,
 * penerimaan input pengguna, pemanggilan Controller, dan penyajian pesan.
 * Seluruh aturan transaksi dan akses database tetap berada pada Model,
 * Controller, dan DAO.
 */
public class KasirView extends JPanel {

    // Palet warna
    private static final Color BG_PANEL = new Color(248, 250, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color PRIMARY_BLUE_LIGHT = new Color(239, 246, 255);
    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color SUCCESS_GREEN = new Color(22, 163, 74);
    private static final Color SUCCESS_BG = new Color(240, 253, 244);
    private static final Color DANGER_RED = new Color(220, 38, 38);
    private static final Color WARNING_ORANGE = new Color(217, 119, 6);
    private static final Color WARNING_BG = new Color(255, 251, 235);

    private static final String SEMUA = "SEMUA";
    private static final String MAKANAN = "MAKANAN";
    private static final String MINUMAN = "MINUMAN";
    private static final String ELEKTRONIK = "ELEKTRONIK";

    private final ProdukController produkController;
    private final TransaksiController transaksiController;
    private final Pengguna penggunaAktif;

    private final List<Produk> semuaProduk = new ArrayList<>();

    private Transaksi transaksiAktif;
    private String kategoriAktif = SEMUA;

    // Komponen katalog
    private final JTextField txtCari = new JTextField();
   private final JPanel panelKartuProduk = new JPanel(new GridBagLayout());
    private final JLabel lblJumlahProduk = new JLabel("0 produk");
    private final JLabel lblStatusKatalog = new JLabel("Memuat produk...");
    private final JButton btnSemua = buatTombolFilter("Semua", SEMUA);
    private final JButton btnMakanan = buatTombolFilter("Makanan", MAKANAN);
    private final JButton btnMinuman = buatTombolFilter("Minuman", MINUMAN);
    private final JButton btnElektronik = buatTombolFilter("Elektronik", ELEKTRONIK);

    // Komponen keranjang dan pembayaran
    private final JPanel panelDaftarKeranjang = new JPanel();
    private final JLabel lblJumlahItem = new JLabel("0 item");
    private final JLabel lblSubtotal = new JLabel(formatRupiah(0));
    private final JLabel lblPajak = new JLabel(formatRupiah(0));
    private final JLabel lblGrandTotal = new JLabel(formatRupiah(0));
    private final JLabel lblKembalian = new JLabel(formatRupiah(0));
    private final JTextField txtBayar = new JTextField();
    private final JButton btnBayar = new JButton("BAYAR (F9)");

    /**
     * Konstruktor default menggunakan pengguna yang sedang login.
     */
    public KasirView() {
        this(AuthController.getPenggunaAktif());
    }

    /**
     * Konstruktor yang menerima pengguna aktif dari MainFrame.
     */
    public KasirView(Pengguna penggunaAktif) {
        if (penggunaAktif == null) {
            throw new IllegalArgumentException(
                "Pengguna aktif tidak boleh null."
            );
        }

        this.penggunaAktif = penggunaAktif;
        this.produkController = new ProdukController();
        this.transaksiController = new TransaksiController();

        setLayout(new BorderLayout(16, 0));
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        mulaiTransaksiBaru();
        bangunTampilan();
        pasangShortcutKeyboard();
        pasangListenerPembayaran();
        muatProduk();
        perbaruiKeranjang();
    }

    // =========================================================
    // PEMBENTUKAN TAMPILAN
    // =========================================================

    private void bangunTampilan() {
        JPanel panelKatalog = bangunPanelKatalog();
        JPanel panelKeranjang = bangunPanelKeranjang();

        add(panelKatalog, BorderLayout.CENTER);
        add(panelKeranjang, BorderLayout.EAST);
    }

    private JPanel bangunPanelKatalog() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        // Search bar
        JPanel panelPencarian = new JPanel(new BorderLayout(10, 0));
        panelPencarian.setBackground(CARD_BG);
        panelPencarian.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_BLUE, 1),
            BorderFactory.createEmptyBorder(4, 12, 4, 8)
        ));
        panelPencarian.setPreferredSize(new Dimension(100, 46));

        JLabel ikonCari = new JLabel("⌕");
        ikonCari.setFont(new Font("SansSerif", Font.PLAIN, 25));
        ikonCari.setForeground(TEXT_MUTED);
        panelPencarian.add(ikonCari, BorderLayout.WEST);

        txtCari.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtCari.setForeground(TEXT_DARK);
        txtCari.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        txtCari.setToolTipText("Cari nama atau kode produk (F2)");
        txtCari.setOpaque(false);
        txtCari.addActionListener(e -> tampilkanProduk());
        panelPencarian.add(txtCari, BorderLayout.CENTER);

        JLabel shortcutCari = new JLabel("F2");
        shortcutCari.setFont(new Font("SansSerif", Font.BOLD, 11));
        shortcutCari.setForeground(TEXT_MUTED);
        shortcutCari.setOpaque(true);
        shortcutCari.setBackground(new Color(241, 245, 249));
        shortcutCari.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(4, 9, 4, 9)
        ));
        panelPencarian.add(shortcutCari, BorderLayout.EAST);

        // Kategori
        JPanel panelKategori = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelKategori.setOpaque(false);
        panelKategori.add(btnSemua);
        panelKategori.add(btnMakanan);
        panelKategori.add(btnMinuman);
        panelKategori.add(btnElektronik);

        JPanel panelAtas = new JPanel(new BorderLayout(0, 12));
        panelAtas.setOpaque(false);
        panelAtas.add(panelPencarian, BorderLayout.NORTH);
        panelAtas.add(panelKategori, BorderLayout.SOUTH);

        // Kartu produk
        panelKartuProduk.setBackground(BG_PANEL);
        panelKartuProduk.setBorder(BorderFactory.createEmptyBorder(1, 1, 12, 1));

        JScrollPane scrollProduk = new JScrollPane(panelKartuProduk);
        scrollProduk.setBorder(BorderFactory.createEmptyBorder());
        scrollProduk.setBackground(BG_PANEL);
        scrollProduk.getViewport().setBackground(BG_PANEL);
        scrollProduk.getVerticalScrollBar().setUnitIncrement(16);

        JPanel panelKonten = new JPanel(new BorderLayout(0, 10));
        panelKonten.setOpaque(false);

        JPanel panelInfo = new JPanel(new BorderLayout());
        panelInfo.setOpaque(false);
        JLabel judul = new JLabel("Daftar Produk");
        judul.setFont(new Font("SansSerif", Font.BOLD, 15));
        judul.setForeground(TEXT_DARK);
        lblJumlahProduk.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblJumlahProduk.setForeground(TEXT_MUTED);
        panelInfo.add(judul, BorderLayout.WEST);
        panelInfo.add(lblJumlahProduk, BorderLayout.EAST);

        lblStatusKatalog.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblStatusKatalog.setForeground(TEXT_MUTED);
        lblStatusKatalog.setHorizontalAlignment(SwingConstants.CENTER);

        panelKonten.add(panelInfo, BorderLayout.NORTH);
        panelKonten.add(scrollProduk, BorderLayout.CENTER);

        panel.add(panelAtas, BorderLayout.NORTH);
        panel.add(panelKonten, BorderLayout.CENTER);
        return panel;
    }

    private JPanel bangunPanelKeranjang() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(16, 18, 14, 18)
        ));
        panel.setPreferredSize(new Dimension(410, 100));
        panel.setMinimumSize(new Dimension(365, 100));

        // Header keranjang
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setOpaque(false);

        JPanel judulKeranjang = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        judulKeranjang.setOpaque(false);
        JLabel judul = new JLabel("Keranjang");
        judul.setFont(new Font("SansSerif", Font.BOLD, 19));
        judul.setForeground(TEXT_DARK);
        lblJumlahItem.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblJumlahItem.setForeground(PRIMARY_BLUE);
        lblJumlahItem.setOpaque(true);
        lblJumlahItem.setBackground(PRIMARY_BLUE_LIGHT);
        lblJumlahItem.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(191, 219, 254)),
            BorderFactory.createEmptyBorder(4, 9, 4, 9)
        ));
        judulKeranjang.add(judul);
        judulKeranjang.add(lblJumlahItem);

        JButton btnBersihkan = tombolTeks("Bersihkan", DANGER_RED);
        btnBersihkan.addActionListener(e -> bersihkanKeranjang());
        panelHeader.add(judulKeranjang, BorderLayout.WEST);
        panelHeader.add(btnBersihkan, BorderLayout.EAST);

        // Isi keranjang
        panelDaftarKeranjang.setLayout(new BoxLayout(panelDaftarKeranjang, BoxLayout.Y_AXIS));
        panelDaftarKeranjang.setBackground(CARD_BG);

        JScrollPane scrollKeranjang = new JScrollPane(panelDaftarKeranjang);
        scrollKeranjang.setBorder(BorderFactory.createMatteBorder(
            1, 0, 0, 0, BORDER_COLOR
        ));
        scrollKeranjang.getViewport().setBackground(CARD_BG);
        scrollKeranjang.getVerticalScrollBar().setUnitIncrement(14);

        // Panel ringkasan dan pembayaran
        JPanel panelPembayaran = bangunPanelPembayaran();

        panel.add(panelHeader, BorderLayout.NORTH);
        panel.add(scrollKeranjang, BorderLayout.CENTER);
        panel.add(panelPembayaran, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel bangunPanelPembayaran() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createMatteBorder(
            1, 0, 0, 0, BORDER_COLOR
        ));

        panel.add(Box.createVerticalStrut(12));
        panel.add(barisRingkasan("Subtotal Barang", lblSubtotal, false));
        panel.add(Box.createVerticalStrut(8));
        panel.add(barisRingkasan("Total Pajak", lblPajak, false));
        panel.add(Box.createVerticalStrut(10));
        panel.add(new GarisPemisah());
        panel.add(Box.createVerticalStrut(10));
        panel.add(barisRingkasan("GRAND TOTAL", lblGrandTotal, true));
        panel.add(Box.createVerticalStrut(14));

        JPanel panelInputPembayaran = new JPanel(new GridLayout(1, 2, 10, 0));
        panelInputPembayaran.setOpaque(false);

        JPanel kolomBayar = new JPanel(new BorderLayout(0, 6));
        kolomBayar.setOpaque(false);
        JLabel labelBayar = labelKecil("Uang Dibayar");
        txtBayar.setFont(new Font("SansSerif", Font.BOLD, 15));
        txtBayar.setForeground(TEXT_DARK);
        txtBayar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(9, 10, 9, 10)
        ));
        txtBayar.setToolTipText("Masukkan nominal uang yang diterima");
        kolomBayar.add(labelBayar, BorderLayout.NORTH);
        kolomBayar.add(txtBayar, BorderLayout.CENTER);

        JPanel kolomKembali = new JPanel(new BorderLayout(0, 6));
        kolomKembali.setOpaque(false);
        JLabel labelKembali = labelKecil("Kembalian");
        lblKembalian.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblKembalian.setForeground(TEXT_MUTED);
        lblKembalian.setOpaque(true);
        lblKembalian.setBackground(new Color(248, 250, 252));
        lblKembalian.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        kolomKembali.add(labelKembali, BorderLayout.NORTH);
        kolomKembali.add(lblKembalian, BorderLayout.CENTER);

        panelInputPembayaran.add(kolomBayar);
        panelInputPembayaran.add(kolomKembali);
        panel.add(panelInputPembayaran);
        panel.add(Box.createVerticalStrut(12));

        btnBayar.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnBayar.setForeground(Color.WHITE);
        btnBayar.setBackground(SUCCESS_GREEN);
        btnBayar.setFocusPainted(false);
        btnBayar.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        btnBayar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBayar.setOpaque(true);
        btnBayar.setBorderPainted(false);
        btnBayar.addActionListener(e -> prosesPembayaran());
        btnBayar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnBayar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        panel.add(btnBayar);

        JButton btnBatal = new JButton("Batal (Esc)");
        btnBatal.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnBatal.setForeground(TEXT_MUTED);
        btnBatal.setBorderPainted(false);
        btnBatal.setContentAreaFilled(false);
        btnBatal.setFocusPainted(false);
        btnBatal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBatal.addActionListener(e -> batalkanTransaksi());
        btnBatal.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(Box.createVerticalStrut(6));
        panel.add(btnBatal);
        panel.add(Box.createVerticalStrut(4));

        JLabel petunjuk = new JLabel("F2 Cari   •   F9 Bayar   •   Esc Batal");
        petunjuk.setFont(new Font("SansSerif", Font.PLAIN, 10));
        petunjuk.setForeground(TEXT_MUTED);
        petunjuk.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(petunjuk);
        panel.add(Box.createVerticalStrut(4));

        return panel;
    }

    // =========================================================
    // KATALOG PRODUK
    // =========================================================

    private void muatProduk() {
        try {
            List<Produk> hasil = produkController.daftarProduk();
            semuaProduk.clear();

            if (hasil != null) {
                semuaProduk.addAll(hasil);
            }

            tampilkanProduk();
        } catch (SQLException ex) {
            panelKartuProduk.removeAll();
            lblJumlahProduk.setText("0 produk");
            lblStatusKatalog.setText("Gagal memuat produk. Periksa koneksi database.");
            panelKartuProduk.add(lblStatusKatalog);
            panelKartuProduk.revalidate();
            panelKartuProduk.repaint();
            tampilkanPesanError("Gagal memuat daftar produk", ex);
        }
    }

    private void tampilkanProduk() {

    String keyword = txtCari.getText() == null
        ? ""
        : txtCari.getText().trim().toLowerCase(Locale.ROOT);

    List<Produk> hasilFilter = semuaProduk.stream()
        .filter(p -> cocokKategori(p, kategoriAktif))
        .filter(p -> keyword.isEmpty()
            || p.getNama().toLowerCase(Locale.ROOT).contains(keyword)
            || p.getKode().toLowerCase(Locale.ROOT).contains(keyword))
        .collect(Collectors.toList());

    panelKartuProduk.removeAll();
    panelKartuProduk.setLayout(new GridBagLayout());

    lblJumlahProduk.setText(hasilFilter.size() + " produk");

    if (hasilFilter.isEmpty()) {

        JLabel kosong = new JLabel(
            "Produk tidak ditemukan.",
            SwingConstants.CENTER
        );

        kosong.setFont(new Font("SansSerif", Font.PLAIN, 13));
        kosong.setForeground(TEXT_MUTED);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;

        panelKartuProduk.add(kosong, gbc);

    } else {

        int jumlahKolom = 3;

        for (int i = 0; i < hasilFilter.size(); i++) {

            GridBagConstraints gbc = new GridBagConstraints();

            gbc.gridx = i % jumlahKolom;
            gbc.gridy = i / jumlahKolom;

            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.NORTHWEST;

            gbc.weightx = 0;
            gbc.weighty = 0;

            gbc.insets = new Insets(0, 0, 12, 12);

            panelKartuProduk.add(
                buatKartuProduk(hasilFilter.get(i)),
                gbc
            );
        }

        // Spacer kanan untuk menjaga kartu tetap di kiri
        JPanel spacerKanan = new JPanel();
        spacerKanan.setOpaque(false);

        GridBagConstraints gbcKanan = new GridBagConstraints();
        gbcKanan.gridx = jumlahKolom;
        gbcKanan.gridy = 0;
        gbcKanan.weightx = 1;
        gbcKanan.fill = GridBagConstraints.HORIZONTAL;

        panelKartuProduk.add(spacerKanan, gbcKanan);

        // Spacer bawah untuk menjaga kartu tetap di atas
        JPanel spacerBawah = new JPanel();
        spacerBawah.setOpaque(false);

        GridBagConstraints gbcBawah = new GridBagConstraints();
        gbcBawah.gridx = 0;
        gbcBawah.gridy = (hasilFilter.size() + jumlahKolom - 1)
            / jumlahKolom;
        gbcBawah.gridwidth = jumlahKolom + 1;
        gbcBawah.weighty = 1;
        gbcBawah.fill = GridBagConstraints.VERTICAL;

        panelKartuProduk.add(spacerBawah, gbcBawah);
    }

    panelKartuProduk.revalidate();
    panelKartuProduk.repaint();
}

    private boolean cocokKategori(Produk produk, String kategori) {
        return SEMUA.equals(kategori)
            || kategori.equalsIgnoreCase(produk.getJenisProduk());
    }

    private JPanel buatKartuProduk(Produk produk) {
        int stok = produk.getStok();

        JPanel kartu = new JPanel(new BorderLayout(0, 8));
        kartu.setBackground(CARD_BG);
        kartu.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                stok <= 5 && stok > 0 ? new Color(251, 191, 36) : BORDER_COLOR
            ),
            BorderFactory.createEmptyBorder(12, 12, 10, 12)
        ));
        kartu.setPreferredSize(new Dimension(190, 154));

        // Bagian atas kartu: jenis dan stok
        JPanel panelAtas = new JPanel(new BorderLayout());
        panelAtas.setOpaque(false);

        JLabel labelJenis = new JLabel(labelJenisProduk(produk.getJenisProduk()));
        labelJenis.setFont(new Font("SansSerif", Font.BOLD, 10));
        labelJenis.setForeground(warnaJenisProduk(produk.getJenisProduk()));
        panelAtas.add(labelJenis, BorderLayout.WEST);

        JLabel labelStok = new JLabel("Stok: " + stok);
        labelStok.setFont(new Font("SansSerif", Font.PLAIN, 11));
        labelStok.setForeground(stok <= 5 ? WARNING_ORANGE : TEXT_MUTED);
        panelAtas.add(labelStok, BorderLayout.EAST);

        // Nama dan spesifikasi produk
        JPanel panelDetail = new JPanel();
        panelDetail.setOpaque(false);
        panelDetail.setLayout(new BoxLayout(panelDetail, BoxLayout.Y_AXIS));

        JLabel nama = new JLabel(htmlSingkat(produk.getNama(), 26));
        nama.setFont(new Font("SansSerif", Font.BOLD, 13));
        nama.setForeground(stok <= 0 ? TEXT_MUTED : TEXT_DARK);
        nama.setToolTipText(produk.getNama());
        nama.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel spesifikasi = new JLabel(produk.getInfoSpesifik());
        spesifikasi.setFont(new Font("SansSerif", Font.PLAIN, 11));
        spesifikasi.setForeground(TEXT_MUTED);
        spesifikasi.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelDetail.add(nama);
        panelDetail.add(Box.createVerticalStrut(4));
        panelDetail.add(spesifikasi);

        // Harga dan tombol tambah
        JPanel panelBawah = new JPanel(new BorderLayout(4, 0));
        panelBawah.setOpaque(false);
        panelBawah.setBorder(BorderFactory.createMatteBorder(
            1, 0, 0, 0, new Color(241, 245, 249)
        ));

        JLabel harga = new JLabel(formatRupiah(produk.getHarga()));
        harga.setFont(new Font("SansSerif", Font.BOLD, 13));
        harga.setForeground(TEXT_DARK);

        JButton btnTambah = new JButton(stok > 0 ? "+ Tambah" : "Kosong");
        btnTambah.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnTambah.setForeground(stok > 0 ? PRIMARY_BLUE : TEXT_MUTED);
        btnTambah.setBorderPainted(false);
        btnTambah.setContentAreaFilled(false);
        btnTambah.setFocusPainted(false);
        btnTambah.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTambah.setEnabled(stok > 0);
        btnTambah.addActionListener(e -> tambahKeKeranjang(produk));

        panelBawah.add(harga, BorderLayout.WEST);
        panelBawah.add(btnTambah, BorderLayout.EAST);

        kartu.add(panelAtas, BorderLayout.NORTH);
        kartu.add(panelDetail, BorderLayout.CENTER);
        kartu.add(panelBawah, BorderLayout.SOUTH);
        kartu.setToolTipText(produk.getNama() + " — " + formatRupiah(produk.getHarga()));
        return kartu;
    }

    private void tambahKeKeranjang(Produk produk) {
        try {
            transaksiController.tambahItem(transaksiAktif, produk, 1);
            perbaruiKeranjang();
        } catch (StokTidakCukupException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Tidak dapat menambahkan produk",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void pilihKategori(String kategori) {
        kategoriAktif = kategori;
        aturTampilanTombolFilter();
        tampilkanProduk();
    }

    private JButton buatTombolFilter(String teks, String kategori) {
        JButton tombol = new JButton(teks);
        tombol.setFont(new Font("SansSerif", Font.BOLD, 12));
        tombol.setFocusPainted(false);
        tombol.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tombol.setMargin(new Insets(8, 15, 8, 15));
        tombol.addActionListener(e -> pilihKategori(kategori));
        aturGayaTombolFilter(tombol, SEMUA.equals(kategori));
        return tombol;
    }

    private void aturTampilanTombolFilter() {
        aturGayaTombolFilter(btnSemua, SEMUA.equals(kategoriAktif));
        aturGayaTombolFilter(btnMakanan, MAKANAN.equals(kategoriAktif));
        aturGayaTombolFilter(btnMinuman, MINUMAN.equals(kategoriAktif));
        aturGayaTombolFilter(btnElektronik, ELEKTRONIK.equals(kategoriAktif));
    }

    private void aturGayaTombolFilter(JButton tombol, boolean aktif) {

    // Warna latar belakang tombol
    tombol.setBackground(
        aktif ? PRIMARY_BLUE_LIGHT : CARD_BG
    );

    // Warna teks harus tetap kontras
    tombol.setForeground(
        aktif ? PRIMARY_BLUE : TEXT_DARK
    );

    // Memastikan warna tombol ditampilkan oleh Swing
    tombol.setOpaque(true);
    tombol.setContentAreaFilled(true);
    tombol.setBorderPainted(true);
    tombol.setFocusPainted(false);
    tombol.setRolloverEnabled(false);

    // Border tombol
    tombol.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                aktif ? new Color(191, 219, 254) : BORDER_COLOR
            ),
            BorderFactory.createEmptyBorder(2, 3, 2, 3)
        )
    );
}

    // =========================================================
    // KERANJANG
    // =========================================================

    private void perbaruiKeranjang() {
        panelDaftarKeranjang.removeAll();

        List<ItemTransaksi> daftarItem = transaksiAktif.getListKeranjang();
        lblJumlahItem.setText(daftarItem.size() + " item");

        if (daftarItem.isEmpty()) {
            JPanel panelKosong = new JPanel(new BorderLayout());
            panelKosong.setOpaque(false);
            JLabel pesan = new JLabel(
                "Keranjang masih kosong.\nPilih produk untuk memulai transaksi.",
                SwingConstants.CENTER
            );
            pesan.setFont(new Font("SansSerif", Font.PLAIN, 12));
            pesan.setForeground(TEXT_MUTED);
            panelKosong.add(pesan, BorderLayout.CENTER);
            panelKosong.setPreferredSize(new Dimension(100, 150));
            panelDaftarKeranjang.add(panelKosong);
        } else {
            for (ItemTransaksi item : daftarItem) {
                panelDaftarKeranjang.add(buatBarisKeranjang(item));
            }
        }

        lblSubtotal.setText(formatRupiah(transaksiAktif.getSubtotalBarang()));
        lblPajak.setText(formatRupiah(transaksiAktif.getTotalPajak()));
        lblGrandTotal.setText(formatRupiah(transaksiAktif.getGrandTotal()));

        perbaruiPratinjauKembalian();
        panelDaftarKeranjang.revalidate();
        panelDaftarKeranjang.repaint();
    }

    private JPanel buatBarisKeranjang(ItemTransaksi item) {
        Produk produk = item.getProduk();

        JPanel baris = new JPanel(new BorderLayout(8, 6));
        baris.setBackground(CARD_BG);
        baris.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(10, 2, 10, 2)
        ));
        baris.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));

        JPanel informasi = new JPanel();
        informasi.setOpaque(false);
        informasi.setLayout(new BoxLayout(informasi, BoxLayout.Y_AXIS));

        JLabel nama = new JLabel(htmlSingkat(produk.getNama(), 23));
        nama.setFont(new Font("SansSerif", Font.BOLD, 12));
        nama.setForeground(TEXT_DARK);
        nama.setToolTipText(produk.getNama());

        JLabel hargaSatuan = new JLabel("@ " + formatRupiah(item.getHargaSatuan()));
        hargaSatuan.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hargaSatuan.setForeground(TEXT_MUTED);

        informasi.add(nama);
        informasi.add(Box.createVerticalStrut(3));
        informasi.add(hargaSatuan);

        JPanel kontrol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        kontrol.setOpaque(false);

        JButton btnKurang = tombolKuantitas("−");
        JButton btnTambah = tombolKuantitas("+");
        JLabel kuantitas = new JLabel(String.valueOf(item.getKuantitas()), SwingConstants.CENTER);
        kuantitas.setFont(new Font("SansSerif", Font.BOLD, 12));
        kuantitas.setForeground(TEXT_DARK);
        kuantitas.setPreferredSize(new Dimension(22, 26));

        btnKurang.addActionListener(e -> ubahKuantitas(produk, item.getKuantitas() - 1));
        btnTambah.addActionListener(e -> ubahKuantitas(produk, item.getKuantitas() + 1));

        JLabel subtotalItem = new JLabel(formatRupiah(item.getSubtotal()));
        subtotalItem.setFont(new Font("SansSerif", Font.BOLD, 12));
        subtotalItem.setForeground(TEXT_DARK);

        JButton btnHapus = tombolTeks("×", DANGER_RED);
        btnHapus.setFont(new Font("SansSerif", Font.BOLD, 19));
        btnHapus.setToolTipText("Hapus item");
        btnHapus.addActionListener(e -> hapusItem(produk));

        kontrol.add(btnKurang);
        kontrol.add(kuantitas);
        kontrol.add(btnTambah);
        kontrol.add(Box.createHorizontalStrut(3));
        kontrol.add(subtotalItem);
        kontrol.add(btnHapus);

        baris.add(informasi, BorderLayout.CENTER);
        baris.add(kontrol, BorderLayout.EAST);
        return baris;
    }

    private JButton tombolKuantitas(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(new Font("SansSerif", Font.BOLD, 12));
        tombol.setForeground(TEXT_MUTED);
        tombol.setBackground(Color.WHITE);
        tombol.setPreferredSize(new Dimension(25, 26));
        tombol.setMargin(new Insets(0, 0, 0, 0));
        tombol.setFocusPainted(false);
        tombol.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        tombol.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return tombol;
    }

    private void ubahKuantitas(Produk produk, int kuantitasBaru) {
        try {
            if (kuantitasBaru <= 0) {
                transaksiController.hapusItem(transaksiAktif, produk);
            } else {
                transaksiController.ubahKuantitas(
                    transaksiAktif,
                    produk,
                    kuantitasBaru
                );
            }
            perbaruiKeranjang();
        } catch (StokTidakCukupException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Gagal mengubah kuantitas",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void hapusItem(Produk produk) {
        try {
            transaksiController.hapusItem(transaksiAktif, produk);
            perbaruiKeranjang();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Gagal menghapus item",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void bersihkanKeranjang() {
        if (transaksiAktif.getListKeranjang().isEmpty()) {
            return;
        }

        int pilihan = JOptionPane.showConfirmDialog(
            this,
            "Hapus seluruh item dari keranjang?",
            "Bersihkan Keranjang",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (pilihan == JOptionPane.YES_OPTION) {
            transaksiController.bersihkanKeranjang(transaksiAktif);
            txtBayar.setText("");
            perbaruiKeranjang();
        }
    }

    // =========================================================
    // PEMBAYARAN DAN CHECKOUT
    // =========================================================

    private void pasangListenerPembayaran() {
        txtBayar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                perbaruiPratinjauKembalian();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                perbaruiPratinjauKembalian();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                perbaruiPratinjauKembalian();
            }
        });

        txtBayar.addActionListener(e -> prosesPembayaran());
    }

    private void perbaruiPratinjauKembalian() {
        double nominalBayar = bacaNominalPembayaran();
        double total = transaksiAktif == null ? 0 : transaksiAktif.getGrandTotal();

        if (nominalBayar >= total && total > 0) {
            lblKembalian.setText(formatRupiah(nominalBayar - total));
            lblKembalian.setForeground(SUCCESS_GREEN);
            lblKembalian.setBackground(SUCCESS_BG);
            lblKembalian.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(187, 247, 208)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
        } else {
            lblKembalian.setText(formatRupiah(0));
            lblKembalian.setForeground(
                nominalBayar > 0 ? DANGER_RED : TEXT_MUTED
            );
            lblKembalian.setBackground(new Color(248, 250, 252));
            lblKembalian.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
        }

        btnBayar.setEnabled(
            !transaksiAktif.getListKeranjang().isEmpty()
                && nominalBayar >= total
                && nominalBayar > 0
        );
    }

    private double bacaNominalPembayaran() {
        String teks = txtBayar.getText();
        if (teks == null || teks.isBlank()) {
            return 0;
        }

        String angka = teks.replaceAll("[^0-9]", "");
        if (angka.isEmpty()) {
            return 0;
        }

        try {
            return Long.parseLong(angka);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void prosesPembayaran() {
        if (transaksiAktif.getListKeranjang().isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Keranjang masih kosong.",
                "Pembayaran",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        double nominalBayar = bacaNominalPembayaran();
        if (nominalBayar <= 0) {
            JOptionPane.showMessageDialog(
                this,
                "Masukkan nominal pembayaran terlebih dahulu.",
                "Pembayaran",
                JOptionPane.WARNING_MESSAGE
            );
            txtBayar.requestFocusInWindow();
            return;
        }

        try {
            transaksiController.prosesPembayaran(transaksiAktif, nominalBayar);
            transaksiController.checkout(transaksiAktif);
            tampilkanNotaBerhasil();

            mulaiTransaksiBaru();
            txtBayar.setText("");
            muatProduk();
            perbaruiKeranjang();
        } catch (StokTidakCukupException ex) {
            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Stok Tidak Mencukupi",
                JOptionPane.WARNING_MESSAGE
            );
            muatProduk();
        } catch (SQLException ex) {
            tampilkanPesanError("Transaksi gagal disimpan ke database", ex);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Pembayaran tidak valid",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void tampilkanNotaBerhasil() {
        JTextArea areaNota = new JTextArea(transaksiAktif.cetak(), 20, 38);
        areaNota.setEditable(false);
        areaNota.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaNota.setBackground(Color.WHITE);
        areaNota.setCaretPosition(0);

        JScrollPane scrollNota = new JScrollPane(areaNota);
        JOptionPane.showMessageDialog(
            this,
            scrollNota,
            "Transaksi Berhasil — " + transaksiAktif.getIdTransaksi(),
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void batalkanTransaksi() {
        if (transaksiAktif.getListKeranjang().isEmpty()) {
            return;
        }

        int pilihan = JOptionPane.showConfirmDialog(
            this,
            "Batalkan transaksi yang sedang berjalan?",
            "Batalkan Transaksi",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (pilihan == JOptionPane.YES_OPTION) {
            transaksiController.bersihkanKeranjang(transaksiAktif);
            txtBayar.setText("");
            mulaiTransaksiBaru();
            perbaruiKeranjang();
        }
    }

    private void mulaiTransaksiBaru() {
        transaksiAktif = transaksiController.buatTransaksi(penggunaAktif);
    }

    // =========================================================
    // SHORTCUT DAN KOMPONEN BANTUAN
    // =========================================================

    private void pasangShortcutKeyboard() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("F2"), "fokusPencarian"
        );
        getActionMap().put("fokusPencarian", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtCari.requestFocusInWindow();
                txtCari.selectAll();
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("F9"), "prosesBayar"
        );
        getActionMap().put("prosesBayar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                prosesPembayaran();
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("ESCAPE"), "batalkanTransaksi"
        );
        getActionMap().put("batalkanTransaksi", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                batalkanTransaksi();
            }
        });
    }

    private JPanel barisRingkasan(String teks, JLabel nilai, boolean utama) {
        JPanel baris = new JPanel(new BorderLayout());
        baris.setOpaque(false);

        JLabel label = new JLabel(teks);
        label.setFont(new Font("SansSerif", utama ? Font.BOLD : Font.PLAIN,
            utama ? 13 : 12));
        label.setForeground(utama ? TEXT_DARK : TEXT_MUTED);

        nilai.setFont(new Font("SansSerif", Font.BOLD, utama ? 22 : 12));
        nilai.setForeground(TEXT_DARK);
        nilai.setHorizontalAlignment(SwingConstants.RIGHT);

        baris.add(label, BorderLayout.WEST);
        baris.add(nilai, BorderLayout.EAST);
        baris.setMaximumSize(new Dimension(Integer.MAX_VALUE, utama ? 32 : 22));
        return baris;
    }

    private JLabel labelKecil(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        label.setForeground(TEXT_MUTED);
        return label;
    }

    private JButton tombolTeks(String teks, Color warna) {
        JButton tombol = new JButton(teks);
        tombol.setFont(new Font("SansSerif", Font.BOLD, 11));
        tombol.setForeground(warna);
        tombol.setBorderPainted(false);
        tombol.setContentAreaFilled(false);
        tombol.setFocusPainted(false);
        tombol.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return tombol;
    }

    private String labelJenisProduk(String jenis) {
        if (jenis == null) {
            return "PRODUK";
        }
        return switch (jenis.toUpperCase(Locale.ROOT)) {
            case MAKANAN -> "MAKANAN";
            case MINUMAN -> "MINUMAN";
            case ELEKTRONIK -> "ELEKTRONIK";
            default -> jenis.toUpperCase(Locale.ROOT);
        };
    }

    private Color warnaJenisProduk(String jenis) {
        if (jenis == null) {
            return TEXT_MUTED;
        }
        return switch (jenis.toUpperCase(Locale.ROOT)) {
            case MAKANAN -> new Color(217, 119, 6);
            case MINUMAN -> new Color(5, 150, 105);
            case ELEKTRONIK -> PRIMARY_BLUE;
            default -> TEXT_MUTED;
        };
    }

    private String htmlSingkat(String teks, int batas) {
        if (teks == null) {
            return "-";
        }
        String aman = teks.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
        if (aman.length() > batas) {
            aman = aman.substring(0, Math.max(0, batas - 1)) + "…";
        }
        return "<html>" + aman + "</html>";
    }

    private String formatRupiah(double nominal) {
        NumberFormat format = NumberFormat.getCurrencyInstance(
            new Locale("id", "ID")
        );
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(nominal).replace('\u00A0', ' ');
    }

    private void tampilkanPesanError(String judul, Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            judul + ".\n" + ex.getMessage(),
            "Kesalahan",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private static class GarisPemisah extends JPanel {
        GarisPemisah() {
            setPreferredSize(new Dimension(1, 1));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            setBackground(BORDER_COLOR);
        }
    }
}
