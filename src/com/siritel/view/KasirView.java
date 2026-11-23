package com.siritel.view;

import com.siritel.controller.ProdukController;
import com.siritel.controller.TransaksiController;
import com.siritel.exception.StokTidakCukupException;
import com.siritel.model.ItemTransaksi;
import com.siritel.model.Pengguna;
import com.siritel.model.Produk;
import com.siritel.model.Transaksi;
import com.siritel.view.ui.ButtonVariant;
import com.siritel.view.ui.ModernModal;
import com.siritel.view.ui.ReceiptSupport;
import com.siritel.view.ui.RefreshableView;
import com.siritel.view.ui.RoundedBorder;
import com.siritel.view.ui.RoundedButton;
import com.siritel.view.ui.RoundedCardPanel;
import com.siritel.view.ui.ToastManager;
import com.siritel.view.ui.ToastType;
import com.siritel.view.ui.UITheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Halaman kasir SI-RITEL.
 * Menggunakan komponen UI terpusat dari {@code com.siritel.view.ui}, pemuatan & checkout asinkron,
 * serta sinkronisasi otomatis keranjang terhadap perubahan katalog di database.
 */
public class KasirView extends JPanel implements RefreshableView {

    private static final String SEMUA = "SEMUA";
    private static final String MAKANAN = "MAKANAN";
    private static final String MINUMAN = "MINUMAN";
    private static final String ELEKTRONIK = "ELEKTRONIK";

    private final ProdukController produkController = new ProdukController();
    private final TransaksiController transaksiController = new TransaksiController();
    private final ToastManager toastManager = new ToastManager(this);
    private final Pengguna penggunaAktif;

    private final List<Produk> semuaProduk = new ArrayList<>();
    private Transaksi transaksiAktif;
    private String kategoriAktif = SEMUA;
    private int kolomSaatIni = 3;
    private int loadRequestId = 0;
    private boolean isProcessingCheckout = false;

    // Komponen katalog
    private final JTextField txtCari = UITheme.createPlaceholderField("Cari kode atau nama produk (F2)...");
    private final JPanel panelKartuProduk = new JPanel(new GridBagLayout());
    private final JLabel lblJumlahProduk = new JLabel("(0 produk)");
    private final RoundedButton btnSegarkan = new RoundedButton("Segarkan", ButtonVariant.OUTLINE, UITheme.BUTTON_RADIUS);
    private final RoundedButton btnSemua = buatTombolFilter("Semua", SEMUA);
    private final RoundedButton btnMakanan = buatTombolFilter("Makanan", MAKANAN);
    private final RoundedButton btnMinuman = buatTombolFilter("Minuman", MINUMAN);
    private final RoundedButton btnElektronik = buatTombolFilter("Elektronik", ELEKTRONIK);

    // Komponen keranjang dan pembayaran
    private final JPanel panelDaftarKeranjang = new JPanel();
    private final JLabel lblJumlahItem = new JLabel("0 item");
    private final JLabel lblSubtotal = new JLabel(UITheme.formatRupiah(0));
    private final JLabel lblPajak = new JLabel(UITheme.formatRupiah(0));
    private final JLabel lblGrandTotal = new JLabel(UITheme.formatRupiah(0));
    private final JLabel lblKembalian = new JLabel(UITheme.formatRupiah(0));
    private final JPanel boxKembalian = new JPanel(new BorderLayout());
    private final JTextField txtBayar = UITheme.createPlaceholderField("0");
    private final RoundedButton btnBayar = new RoundedButton("BAYAR (F9)", ButtonVariant.TEAL, UITheme.BUTTON_RADIUS);
    private final RoundedButton btnBatal = new RoundedButton("Batal (Esc)", ButtonVariant.OUTLINE, UITheme.BUTTON_RADIUS);

    /**
     * Konstruktor yang menerima pengguna aktif dari MainFrame.
     */
    public KasirView(Pengguna penggunaAktif) {
        if (penggunaAktif == null) {
            throw new IllegalArgumentException("Pengguna aktif tidak boleh null.");
        }
        this.penggunaAktif = penggunaAktif;

        setLayout(new BorderLayout(14, 0));
        setBackground(UITheme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        mulaiTransaksiBaru();
        bangunTampilan();
        pasangShortcutKeyboard();
        pasangListenerPembayaran();
        perbaruiKeranjang();
    }

    @Override
    public void refreshData() {
        muatProduk(false);
    }

    // =========================================================
    // PEMBENTUKAN TAMPILAN
    // =========================================================

    private void bangunTampilan() {
        add(bangunPanelKatalog(), BorderLayout.CENTER);
        add(bangunPanelKeranjang(), BorderLayout.EAST);
    }

    private JPanel bangunPanelKatalog() {
        JPanel card = new RoundedCardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(15, 16, 14, 16));

        // Toolbar atas (Judul + Segarkan, Pencarian, Chip Kategori)
        JPanel toolbar = new JPanel(new GridBagLayout());
        toolbar.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Baris 1: Judul + Status jumlah produk + Segarkan
        JPanel headerRow = new JPanel(new BorderLayout(8, 0));
        headerRow.setOpaque(false);

        JPanel titleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleWrap.setOpaque(false);
        JLabel judul = new JLabel("Katalog Kasir");
        judul.setFont(UITheme.font(Font.BOLD, 15f));
        judul.setForeground(UITheme.TEXT_MAIN);
        lblJumlahProduk.setFont(UITheme.font(Font.PLAIN, 12f));
        lblJumlahProduk.setForeground(UITheme.TEXT_MUTED);
        titleWrap.add(judul);
        titleWrap.add(lblJumlahProduk);

        konfigurasiTombol(btnSegarkan, 90, UITheme.TOOLBAR_BUTTON_HEIGHT, Font.PLAIN);
        btnSegarkan.addActionListener(e -> muatProduk(true));

        headerRow.add(titleWrap, BorderLayout.WEST);
        headerRow.add(btnSegarkan, BorderLayout.EAST);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        toolbar.add(headerRow, gbc);

        // Baris 2: Kolom Pencarian + badge F2
        JPanel searchWrap = new JPanel(new BorderLayout(8, 0));
        searchWrap.setBackground(UITheme.CARD_BG);
        searchWrap.setBorder(new RoundedBorder(UITheme.BORDER_INPUT, UITheme.BUTTON_RADIUS, new Insets(4, 12, 4, 8)));
        searchWrap.setPreferredSize(new Dimension(0, 38));

        txtCari.setFont(UITheme.font(Font.PLAIN, 13f));
        txtCari.setForeground(UITheme.TEXT_MAIN);
        txtCari.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        txtCari.setOpaque(false);
        txtCari.setToolTipText("Cari nama atau kode produk (F2)");
        txtCari.addActionListener(e -> tampilkanProduk());
        txtCari.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                tampilkanProduk();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                tampilkanProduk();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                tampilkanProduk();
            }
        });

        JLabel shortcutCari = new JLabel("F2");
        shortcutCari.setFont(UITheme.keycapFont());
        shortcutCari.setForeground(UITheme.KEYCAP_TEXT);
        shortcutCari.setOpaque(true);
        shortcutCari.setBackground(UITheme.KEYCAP_BG);
        shortcutCari.setBorder(new RoundedBorder(UITheme.KEYCAP_BORDER, 6, new Insets(3, 8, 3, 8)));

        searchWrap.add(txtCari, BorderLayout.CENTER);
        searchWrap.add(shortcutCari, BorderLayout.EAST);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        toolbar.add(searchWrap, gbc);

        // Baris 3: Filter chip kategori memanjang penuh sejajar kotak pencarian
        JPanel panelKategori = new JPanel(new GridLayout(1, 4, 8, 0));
        panelKategori.setOpaque(false);
        panelKategori.add(btnSemua);
        panelKategori.add(btnMakanan);
        panelKategori.add(btnMinuman);
        panelKategori.add(btnElektronik);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        toolbar.add(panelKategori, gbc);

        // Wadah grid kartu produk tanpa scroll horizontal
        panelKartuProduk.setBackground(UITheme.BG_PAGE);
        panelKartuProduk.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollProduk = new JScrollPane(panelKartuProduk);
        scrollProduk.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollProduk.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, 12, new Insets(1, 1, 1, 1)));
        scrollProduk.setBackground(UITheme.BG_PAGE);
        scrollProduk.getViewport().setBackground(UITheme.BG_PAGE);
        scrollProduk.getVerticalScrollBar().setUnitIncrement(16);
        scrollProduk.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int targetCols = scrollProduk.getViewport().getWidth() < 490 ? 2 : 3;
                if (targetCols != kolomSaatIni) {
                    kolomSaatIni = targetCols;
                    tampilkanProduk();
                }
            }
        });

        card.add(toolbar, BorderLayout.NORTH);
        card.add(scrollProduk, BorderLayout.CENTER);
        return card;
    }

    private JPanel bangunPanelKeranjang() {
        JPanel card = new RoundedCardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(15, 16, 14, 16));
        card.setPreferredSize(new Dimension(368, 0));
        card.setMinimumSize(new Dimension(340, 0));

        // Header keranjang
        JPanel panelHeader = new JPanel(new BorderLayout(8, 0));
        panelHeader.setOpaque(false);

        JPanel judulWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        judulWrap.setOpaque(false);
        JLabel judul = new JLabel("Keranjang");
        judul.setFont(UITheme.font(Font.BOLD, 16f));
        judul.setForeground(UITheme.TEXT_MAIN);

        lblJumlahItem.setFont(UITheme.font(Font.BOLD, 11f));
        lblJumlahItem.setForeground(UITheme.PRIMARY_BLUE);
        lblJumlahItem.setOpaque(true);
        lblJumlahItem.setBackground(UITheme.PRIMARY_SOFT_BG);
        lblJumlahItem.setBorder(new RoundedBorder(UITheme.PRIMARY_SOFT_BORDER, UITheme.SMALL_RADIUS, new Insets(3, 8, 3, 8)));

        judulWrap.add(judul);
        judulWrap.add(lblJumlahItem);

        RoundedButton btnBersihkan = new RoundedButton("Bersihkan", ButtonVariant.DANGER_OUTLINE, UITheme.SMALL_RADIUS);
        konfigurasiTombol(btnBersihkan, 84, 30, Font.BOLD);
        btnBersihkan.addActionListener(e -> bersihkanKeranjang());

        panelHeader.add(judulWrap, BorderLayout.WEST);
        panelHeader.add(btnBersihkan, BorderLayout.EAST);

        // Daftar item keranjang
        panelDaftarKeranjang.setLayout(new BoxLayout(panelDaftarKeranjang, BoxLayout.Y_AXIS));
        panelDaftarKeranjang.setBackground(UITheme.CARD_BG);

        JScrollPane scrollKeranjang = new JScrollPane(panelDaftarKeranjang);
        scrollKeranjang.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollKeranjang.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UITheme.BORDER_DEFAULT));
        scrollKeranjang.getViewport().setBackground(UITheme.CARD_BG);
        scrollKeranjang.getVerticalScrollBar().setUnitIncrement(14);

        card.add(panelHeader, BorderLayout.NORTH);
        card.add(scrollKeranjang, BorderLayout.CENTER);
        card.add(bangunPanelPembayaran(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel bangunPanelPembayaran() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(barisRingkasan("Subtotal Barang", lblSubtotal, false));
        panel.add(Box.createVerticalStrut(6));
        panel.add(barisRingkasan("Total Pajak", lblPajak, false));
        panel.add(Box.createVerticalStrut(8));

        JPanel divider = new JPanel();
        divider.setPreferredSize(new Dimension(1, 1));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setBackground(UITheme.BORDER_DEFAULT);
        panel.add(divider);
        panel.add(Box.createVerticalStrut(8));

        panel.add(barisRingkasan("GRAND TOTAL", lblGrandTotal, true));
        panel.add(Box.createVerticalStrut(12));

        // Grid input pembayaran dan kembalian
        JPanel gridPay = new JPanel(new GridLayout(1, 2, 10, 0));
        gridPay.setOpaque(false);

        JPanel colBayar = new JPanel(new BorderLayout(0, 5));
        colBayar.setOpaque(false);
        colBayar.add(UITheme.createFieldLabel("Uang Dibayar"), BorderLayout.NORTH);
        txtBayar.setFont(UITheme.font(Font.BOLD, 14f));
        txtBayar.setForeground(UITheme.TEXT_MAIN);
        txtBayar.setPreferredSize(new Dimension(0, 36));
        txtBayar.setBorder(new RoundedBorder(UITheme.BORDER_INPUT, UITheme.SMALL_RADIUS, new Insets(4, 10, 4, 10)));
        txtBayar.setToolTipText("Masukkan nominal uang yang diterima (mis. 50000 atau 50.000)");
        colBayar.add(txtBayar, BorderLayout.CENTER);

        JPanel colKembali = new JPanel(new BorderLayout(0, 5));
        colKembali.setOpaque(false);
        colKembali.add(UITheme.createFieldLabel("Kembalian"), BorderLayout.NORTH);

        lblKembalian.setFont(UITheme.font(Font.BOLD, 14f));
        lblKembalian.setForeground(UITheme.TEXT_MUTED);
        boxKembalian.setOpaque(true);
        boxKembalian.setBackground(UITheme.BG_PAGE);
        boxKembalian.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, UITheme.SMALL_RADIUS, new Insets(6, 10, 6, 10)));
        boxKembalian.setPreferredSize(new Dimension(0, 36));
        boxKembalian.add(lblKembalian, BorderLayout.CENTER);
        colKembali.add(boxKembalian, BorderLayout.CENTER);

        gridPay.add(colBayar);
        gridPay.add(colKembali);
        gridPay.setAlignmentX(Component.LEFT_ALIGNMENT);
        gridPay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        panel.add(gridPay);
        panel.add(Box.createVerticalStrut(12));

        // Tombol Bayar & Batal
        JPanel actionCols = new JPanel(new BorderLayout(8, 0));
        actionCols.setOpaque(false);
        actionCols.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionCols.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        btnBatal.setFont(UITheme.font(Font.PLAIN, 12f));
        btnBatal.setPreferredSize(new Dimension(102, 40));
        btnBatal.addActionListener(e -> batalkanTransaksi());

        btnBayar.setFont(UITheme.font(Font.BOLD, 13f));
        btnBayar.setPreferredSize(new Dimension(0, 40));
        btnBayar.addActionListener(e -> prosesPembayaran());

        actionCols.add(btnBatal, BorderLayout.WEST);
        actionCols.add(btnBayar, BorderLayout.CENTER);
        panel.add(actionCols);
        panel.add(Box.createVerticalStrut(8));

        JLabel petunjuk = new JLabel("F2 Cari   \u2022   F9 Bayar   \u2022   Esc Batal", SwingConstants.CENTER);
        petunjuk.setFont(UITheme.font(Font.PLAIN, 10f));
        petunjuk.setForeground(UITheme.TEXT_MUTED);
        petunjuk.setAlignmentX(Component.LEFT_ALIGNMENT);
        petunjuk.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        panel.add(petunjuk);

        return panel;
    }

    // =========================================================
    // KATALOG PRODUK
    // =========================================================

    private void muatProduk(boolean showToast) {
        final int reqId = ++loadRequestId;
        btnSegarkan.setEnabled(false);
        lblJumlahProduk.setText("(memuat...)");

        SwingWorker<List<Produk>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Produk> doInBackground() throws Exception {
                return produkController.daftarProduk();
            }

            @Override
            protected void done() {
                if (reqId != loadRequestId) {
                    return;
                }
                btnSegarkan.setEnabled(true);
                try {
                    List<Produk> hasil = get();
                    semuaProduk.clear();
                    if (hasil != null) {
                        semuaProduk.addAll(hasil);
                    }

                    // Sinkronkan keranjang dengan katalog terbaru agar stok & harga tidak basi
                    Map<Integer, Produk> katalogById = new LinkedHashMap<>();
                    for (Produk p : semuaProduk) {
                        katalogById.put(p.getId(), p);
                    }
                    List<String> perubahan = transaksiAktif.sinkronkanDenganKatalog(katalogById);

                    tampilkanProduk();
                    perbaruiKeranjang();

                    if (!perubahan.isEmpty()) {
                        String ringkasan = perubahan.get(0)
                                + (perubahan.size() > 1 ? " (+" + (perubahan.size() - 1) + " penyesuaian lain)" : "");
                        toastManager.show("Keranjang Disinkronkan", ringkasan, ToastType.INFO);
                    } else if (showToast) {
                        toastManager.show(
                                "Katalog Disegarkan",
                                semuaProduk.size() + " produk terbaru dimuat dari database.",
                                ToastType.INFO
                        );
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    panelKartuProduk.removeAll();
                    lblJumlahProduk.setText("(0 produk)");
                    JLabel errLbl = new JLabel("Gagal memuat produk. Periksa koneksi database.", SwingConstants.CENTER);
                    errLbl.setFont(UITheme.font(Font.PLAIN, 13f));
                    errLbl.setForeground(UITheme.TEXT_MUTED);
                    panelKartuProduk.add(errLbl);
                    panelKartuProduk.revalidate();
                    panelKartuProduk.repaint();
                    String msg = (cause instanceof SQLException sqlEx)
                            ? TransaksiController.pesanSqlError(sqlEx)
                            : "Gagal memuat daftar produk:\n" + (cause != null ? cause.getMessage() : ex.getMessage());
                    ModernModal.error(KasirView.this, "Kesalahan Database", msg);
                }
            }
        };
        worker.execute();
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
        lblJumlahProduk.setText("(" + hasilFilter.size() + " produk)");

        if (hasilFilter.isEmpty()) {
            JLabel kosong = new JLabel("Produk tidak ditemukan.", SwingConstants.CENTER);
            kosong.setFont(UITheme.font(Font.PLAIN, 13f));
            kosong.setForeground(UITheme.TEXT_MUTED);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            panelKartuProduk.add(kosong, gbc);
        } else {
            int cols = Math.max(2, kolomSaatIni);
            int total = hasilFilter.size();
            int rows = (total + cols - 1) / cols;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int idx = r * cols + c;
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.gridx = c;
                    gbc.gridy = r;
                    gbc.weightx = 1.0;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    gbc.anchor = GridBagConstraints.NORTH;
                    gbc.insets = new Insets(0, 0, 10, c < cols - 1 ? 10 : 0);

                    if (idx < total) {
                        panelKartuProduk.add(buatKartuProduk(hasilFilter.get(idx)), gbc);
                    } else {
                        JPanel placeholder = new JPanel();
                        placeholder.setOpaque(false);
                        panelKartuProduk.add(placeholder, gbc);
                    }
                }
            }

            // Filler vertikal agar kartu rata atas
            GridBagConstraints gbcBawah = new GridBagConstraints();
            gbcBawah.gridx = 0;
            gbcBawah.gridy = rows;
            gbcBawah.gridwidth = cols;
            gbcBawah.weighty = 1.0;
            gbcBawah.fill = GridBagConstraints.VERTICAL;
            JPanel spacerBawah = new JPanel();
            spacerBawah.setOpaque(false);
            panelKartuProduk.add(spacerBawah, gbcBawah);
        }

        panelKartuProduk.revalidate();
        panelKartuProduk.repaint();
    }

    private boolean cocokKategori(Produk produk, String kategori) {
        return SEMUA.equals(kategori) || kategori.equalsIgnoreCase(produk.getJenisProduk());
    }

    private JPanel buatKartuProduk(Produk produk) {
        int stok = produk.getStok();
        JPanel kartu = new RoundedCardPanel(new BorderLayout(0, 8), 12);
        kartu.setBorder(BorderFactory.createEmptyBorder(11, 12, 10, 12));
        kartu.setPreferredSize(new Dimension(0, 152));

        // Baris atas: badge kategori + stok
        JPanel panelAtas = new JPanel(new BorderLayout(6, 0));
        panelAtas.setOpaque(false);

        JLabel badgeJenis = buatBadgeJenis(produk.getJenisProduk());
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeWrap.setOpaque(false);
        badgeWrap.add(badgeJenis);

        JLabel labelStok = new JLabel("Stok: " + stok);
        labelStok.setFont(UITheme.font(Font.BOLD, 11f));
        labelStok.setForeground(stok <= 0 ? UITheme.DANGER : (stok <= 5 ? UITheme.BADGE_AMBER_FG : UITheme.TEXT_MUTED));

        panelAtas.add(badgeWrap, BorderLayout.WEST);
        panelAtas.add(labelStok, BorderLayout.EAST);

        // Bagian tengah: nama wrap otomatis + spesifikasi
        JPanel panelDetail = new JPanel(new BorderLayout(0, 3));
        panelDetail.setOpaque(false);

        javax.swing.JTextArea namaArea = ModernModal.createWrappedMessage(
                produk.getNama(),
                13f,
                stok <= 0 ? UITheme.TEXT_MUTED : UITheme.TEXT_MAIN
        );
        namaArea.setFont(UITheme.font(Font.BOLD, 13f));
        namaArea.setToolTipText(produk.getNama());

        JLabel spesifikasi = new JLabel(produk.getInfoSpesifik());
        spesifikasi.setFont(UITheme.font(Font.PLAIN, 11f));
        spesifikasi.setForeground(UITheme.TEXT_MUTED);

        JPanel detailTop = new JPanel(new BorderLayout(0, 3));
        detailTop.setOpaque(false);
        detailTop.add(namaArea, BorderLayout.NORTH);
        detailTop.add(spesifikasi, BorderLayout.SOUTH);
        panelDetail.add(detailTop, BorderLayout.NORTH);

        // Baris bawah: harga + tombol tambah
        JPanel panelBawah = new JPanel(new BorderLayout(6, 0));
        panelBawah.setOpaque(false);
        panelBawah.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BG_HOVER),
                BorderFactory.createEmptyBorder(8, 0, 0, 0)
        ));

        JLabel harga = new JLabel(UITheme.formatRupiah(produk.getHarga()));
        harga.setFont(UITheme.font(Font.BOLD, 13f));
        harga.setForeground(UITheme.TEXT_MAIN);

        RoundedButton btnTambah = new RoundedButton(
                stok > 0 ? "+ Tambah" : "Habis",
                stok > 0 ? ButtonVariant.SOFT_PRIMARY : ButtonVariant.OUTLINE,
                UITheme.SMALL_RADIUS
        );
        btnTambah.setFont(UITheme.font(Font.BOLD, 11f));
        Dimension tambahSize = new Dimension(78, 28);
        btnTambah.setPreferredSize(tambahSize);
        btnTambah.setMinimumSize(tambahSize);
        btnTambah.setMaximumSize(tambahSize);
        btnTambah.setEnabled(stok > 0);
        btnTambah.addActionListener(e -> tambahKeKeranjang(produk));

        panelBawah.add(harga, BorderLayout.WEST);
        panelBawah.add(btnTambah, BorderLayout.EAST);

        kartu.add(panelAtas, BorderLayout.NORTH);
        kartu.add(panelDetail, BorderLayout.CENTER);
        kartu.add(panelBawah, BorderLayout.SOUTH);
        kartu.setToolTipText(produk.getKode() + " \u2022 " + produk.getNama() + " (" + UITheme.formatRupiah(produk.getHarga()) + ")");
        return kartu;
    }

    private JLabel buatBadgeJenis(String jenis) {
        String code = jenis == null ? "" : jenis.toUpperCase(Locale.ROOT);
        Color bg;
        Color fg;
        String text;
        switch (code) {
            case MAKANAN -> {
                bg = UITheme.BADGE_AMBER_BG;
                fg = UITheme.BADGE_AMBER_FG;
                text = "Makanan";
            }
            case MINUMAN -> {
                bg = UITheme.BADGE_BLUE_BG;
                fg = UITheme.BADGE_BLUE_FG;
                text = "Minuman";
            }
            case ELEKTRONIK -> {
                bg = UITheme.BADGE_PURPLE_BG;
                fg = UITheme.BADGE_PURPLE_FG;
                text = "Elektronik";
            }
            default -> {
                bg = UITheme.BG_HOVER;
                fg = UITheme.TEXT_MUTED;
                text = code.isEmpty() ? "Produk" : code;
            }
        }
        JLabel badge = new JLabel(text);
        badge.setFont(UITheme.font(Font.BOLD, 10f));
        badge.setForeground(fg);
        badge.setBackground(bg);
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        return badge;
    }

    private void tambahKeKeranjang(Produk produk) {
        if (isProcessingCheckout) {
            return;
        }
        try {
            transaksiController.tambahItem(transaksiAktif, produk, 1);
            perbaruiKeranjang();
        } catch (StokTidakCukupException ex) {
            ModernModal.error(this, "Stok Tidak Mencukupi (Sisa: " + ex.getSisaStok() + ")", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            ModernModal.error(this, "Gagal Menambah Item", ex.getMessage());
        }
    }

    private void pilihKategori(String kategori) {
        kategoriAktif = kategori;
        aturTampilanTombolFilter();
        tampilkanProduk();
    }

    private RoundedButton buatTombolFilter(String teks, String kategori) {
        RoundedButton tombol = new RoundedButton(
                teks,
                SEMUA.equals(kategori) ? ButtonVariant.PRIMARY : ButtonVariant.OUTLINE,
                UITheme.SMALL_RADIUS
        );
        konfigurasiTombol(tombol, teks.length() > 7 ? 94 : 78, 30, Font.BOLD);
        tombol.addActionListener(e -> pilihKategori(kategori));
        return tombol;
    }

    private void aturTampilanTombolFilter() {
        btnSemua.setVariant(SEMUA.equals(kategoriAktif) ? ButtonVariant.PRIMARY : ButtonVariant.OUTLINE);
        btnMakanan.setVariant(MAKANAN.equals(kategoriAktif) ? ButtonVariant.PRIMARY : ButtonVariant.OUTLINE);
        btnMinuman.setVariant(MINUMAN.equals(kategoriAktif) ? ButtonVariant.PRIMARY : ButtonVariant.OUTLINE);
        btnElektronik.setVariant(ELEKTRONIK.equals(kategoriAktif) ? ButtonVariant.PRIMARY : ButtonVariant.OUTLINE);
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
                    "<html><div style='text-align:center;'>Keranjang masih kosong.<br>Pilih produk untuk memulai transaksi.</div></html>",
                    SwingConstants.CENTER
            );
            pesan.setFont(UITheme.font(Font.PLAIN, 12f));
            pesan.setForeground(UITheme.TEXT_MUTED);
            panelKosong.add(pesan, BorderLayout.CENTER);
            panelKosong.setPreferredSize(new Dimension(100, 160));
            panelDaftarKeranjang.add(panelKosong);
        } else {
            for (ItemTransaksi item : daftarItem) {
                panelDaftarKeranjang.add(buatBarisKeranjang(item));
            }
        }

        lblSubtotal.setText(UITheme.formatRupiah(transaksiAktif.getSubtotalBarang()));
        lblPajak.setText(UITheme.formatRupiah(transaksiAktif.getTotalPajak()));
        lblGrandTotal.setText(UITheme.formatRupiah(transaksiAktif.getGrandTotal()));

        perbaruiPratinjauKembalian();
        panelDaftarKeranjang.revalidate();
        panelDaftarKeranjang.repaint();
    }

    private JPanel buatBarisKeranjang(ItemTransaksi item) {
        Produk produk = item.getProduk();

        JPanel baris = new JPanel(new BorderLayout(8, 6));
        baris.setBackground(UITheme.CARD_BG);
        baris.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_DEFAULT),
                BorderFactory.createEmptyBorder(9, 2, 9, 2)
        ));
        baris.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

        JPanel informasi = new JPanel();
        informasi.setOpaque(false);
        informasi.setLayout(new BoxLayout(informasi, BoxLayout.Y_AXIS));

        JLabel nama = new JLabel(htmlSingkat(produk.getNama(), 22));
        nama.setFont(UITheme.font(Font.BOLD, 12f));
        nama.setForeground(UITheme.TEXT_MAIN);
        nama.setToolTipText(produk.getNama());

        JLabel hargaSatuan = new JLabel("@ " + UITheme.formatRupiah(item.getHargaSatuan()));
        hargaSatuan.setFont(UITheme.font(Font.PLAIN, 11f));
        hargaSatuan.setForeground(UITheme.TEXT_MUTED);

        informasi.add(nama);
        informasi.add(Box.createVerticalStrut(2));
        informasi.add(hargaSatuan);

        JPanel kontrol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        kontrol.setOpaque(false);

        RoundedButton btnKurang = tombolKuantitas("\u2212");
        RoundedButton btnTambah = tombolKuantitas("+");
        JLabel kuantitas = new JLabel(String.valueOf(item.getKuantitas()), SwingConstants.CENTER);
        kuantitas.setFont(UITheme.font(Font.BOLD, 12f));
        kuantitas.setForeground(UITheme.TEXT_MAIN);
        kuantitas.setPreferredSize(new Dimension(22, 26));

        btnKurang.addActionListener(e -> ubahKuantitas(produk, item.getKuantitas() - 1));
        btnTambah.addActionListener(e -> ubahKuantitas(produk, item.getKuantitas() + 1));

        JLabel subtotalItem = new JLabel(UITheme.formatRupiah(item.getSubtotal()));
        subtotalItem.setFont(UITheme.font(Font.BOLD, 12f));
        subtotalItem.setForeground(UITheme.TEXT_MAIN);
        subtotalItem.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));

        RoundedButton btnHapus = new RoundedButton("\u00D7", ButtonVariant.GHOST, 6);
        btnHapus.setFont(UITheme.font(Font.BOLD, 15f));
        btnHapus.setPreferredSize(new Dimension(24, 24));
        btnHapus.setToolTipText("Hapus item");
        btnHapus.addActionListener(e -> hapusItem(produk));

        kontrol.add(btnKurang);
        kontrol.add(kuantitas);
        kontrol.add(btnTambah);
        kontrol.add(subtotalItem);
        kontrol.add(btnHapus);

        baris.add(informasi, BorderLayout.CENTER);
        baris.add(kontrol, BorderLayout.EAST);
        return baris;
    }

    private RoundedButton tombolKuantitas(String teks) {
        RoundedButton tombol = new RoundedButton(teks, ButtonVariant.OUTLINE, 6);
        tombol.setFont(UITheme.font(Font.BOLD, 12f));
        tombol.setPreferredSize(new Dimension(26, 26));
        return tombol;
    }

    private void ubahKuantitas(Produk produk, int kuantitasBaru) {
        if (isProcessingCheckout) {
            return;
        }
        try {
            if (kuantitasBaru <= 0) {
                transaksiController.hapusItem(transaksiAktif, produk);
            } else {
                transaksiController.ubahKuantitas(transaksiAktif, produk, kuantitasBaru);
            }
            perbaruiKeranjang();
        } catch (StokTidakCukupException ex) {
            ModernModal.error(this, "Stok Tidak Mencukupi (Sisa: " + ex.getSisaStok() + ")", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            ModernModal.error(this, "Gagal Mengubah Kuantitas", ex.getMessage());
        }
    }

    private void hapusItem(Produk produk) {
        try {
            transaksiController.hapusItem(transaksiAktif, produk);
            perbaruiKeranjang();
        } catch (IllegalArgumentException ex) {
            ModernModal.error(this, "Gagal Menghapus Item", ex.getMessage());
        }
    }

    private void bersihkanKeranjang() {
        if (transaksiAktif.getListKeranjang().isEmpty()) {
            toastManager.show("Keranjang Kosong", "Belum ada item di keranjang untuk dibersihkan.", ToastType.INFO);
            return;
        }

        boolean disetujui = ModernModal.confirm(
                this,
                "Bersihkan Keranjang",
                "Hapus seluruh item dari keranjang transaksi saat ini?",
                "Ya, Kosongkan"
        );
        if (disetujui) {
            transaksiController.bersihkanKeranjang(transaksiAktif);
            txtBayar.setText("");
            perbaruiKeranjang();
            toastManager.show("Keranjang Dikosongkan", "Seluruh item keranjang telah dihapus.", ToastType.INFO);
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
        txtBayar.addActionListener(e -> {
            if (btnBayar.isEnabled()) {
                prosesPembayaran();
            }
        });
    }

    private void perbaruiPratinjauKembalian() {
        boolean keranjangAdaIsi = transaksiAktif != null && !transaksiAktif.getListKeranjang().isEmpty();
        BigDecimal totalBd = BigDecimal.valueOf(transaksiAktif == null ? 0.0 : transaksiAktif.getGrandTotal())
                .setScale(2, RoundingMode.HALF_UP);
        Double nominalObj = bacaNominalPembayaran();
        boolean inputTerisi = txtBayar.getText() != null && !txtBayar.getText().isBlank();

        BigDecimal bayarBd = null;
        if (nominalObj != null) {
            bayarBd = BigDecimal.valueOf(nominalObj).setScale(2, RoundingMode.HALF_UP);
        } else if (!inputTerisi && keranjangAdaIsi && totalBd.compareTo(BigDecimal.ZERO) == 0) {
            bayarBd = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        boolean cukupBayar = keranjangAdaIsi && bayarBd != null && bayarBd.compareTo(totalBd) >= 0;

        if (cukupBayar) {
            BigDecimal kembalianBd = bayarBd.subtract(totalBd);
            lblKembalian.setText(UITheme.formatRupiah(kembalianBd.doubleValue()));
            lblKembalian.setForeground(UITheme.SUCCESS);
            boxKembalian.setBackground(UITheme.SUCCESS_SOFT_BG);
            boxKembalian.setBorder(new RoundedBorder(UITheme.SUCCESS_BORDER, UITheme.SMALL_RADIUS, new Insets(6, 10, 6, 10)));
        } else {
            lblKembalian.setText(UITheme.formatRupiah(0));
            lblKembalian.setForeground(inputTerisi ? UITheme.DANGER : UITheme.TEXT_MUTED);
            boxKembalian.setBackground(UITheme.BG_PAGE);
            boxKembalian.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, UITheme.SMALL_RADIUS, new Insets(6, 10, 6, 10)));
        }

        btnBayar.setEnabled(!isProcessingCheckout && cukupBayar);
    }

    private Double bacaNominalPembayaran() {
        String teks = txtBayar.getText();
        if (teks == null || teks.isBlank()) {
            return null;
        }
        try {
            double nilai = UITheme.parseNominalIndonesia(teks, "Kosong", "Invalid");
            return nilai < 0 ? null : nilai;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void prosesPembayaran() {
        if (isProcessingCheckout) {
            return;
        }
        if (transaksiAktif.getListKeranjang().isEmpty()) {
            ModernModal.error(this, "Keranjang Kosong", "Tambahkan minimal 1 produk sebelum melakukan pembayaran.");
            return;
        }

        Double nominalObj = bacaNominalPembayaran();
        double total = transaksiAktif.getGrandTotal();
        double nominalBayar;
        if (nominalObj == null) {
            boolean kosong = txtBayar.getText() == null || txtBayar.getText().isBlank();
            if (kosong && total == 0.0) {
                nominalBayar = 0.0;
            } else {
                ModernModal.error(this, "Nominal Tidak Valid",
                        "Masukkan nominal uang yang valid (contoh: 50000, 50.000, atau 10505,25).");
                txtBayar.requestFocusInWindow();
                return;
            }
        } else {
            nominalBayar = nominalObj;
        }

        try {
            transaksiController.prosesPembayaran(transaksiAktif, nominalBayar);
        } catch (IllegalArgumentException ex) {
            ModernModal.error(this, "Pembayaran Tidak Valid", ex.getMessage());
            txtBayar.requestFocusInWindow();
            return;
        }

        final Transaksi transaksiDiproses = transaksiAktif;
        isProcessingCheckout = true;
        btnBayar.setEnabled(false);
        btnBayar.setText("Memproses...");
        btnBatal.setEnabled(false);

        SwingWorker<Transaksi, Void> worker = new SwingWorker<>() {
            @Override
            protected Transaksi doInBackground() throws Exception {
                transaksiController.checkout(transaksiDiproses);
                return transaksiDiproses;
            }

            @Override
            protected void done() {
                isProcessingCheckout = false;
                btnBayar.setText("BAYAR (F9)");
                btnBatal.setEnabled(true);
                try {
                    Transaksi selesai = get();
                    mulaiTransaksiBaru();
                    txtBayar.setText("");
                    perbaruiKeranjang();
                    muatProduk(false);
                    toastManager.show(
                            "Transaksi Berhasil",
                            selesai.getIdTransaksi() + " tersimpan (" + UITheme.formatRupiah(selesai.getGrandTotal()) + ").",
                            ToastType.SUCCESS
                    );
                    ReceiptSupport.showSuccessModal(KasirView.this, selesai, toastManager);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    perbaruiPratinjauKembalian();
                } catch (ExecutionException ex) {
                    perbaruiPratinjauKembalian();
                    Throwable cause = ex.getCause();
                    if (cause instanceof StokTidakCukupException stokEx) {
                        ModernModal.error(KasirView.this,
                                "Stok Tidak Mencukupi (Sisa: " + stokEx.getSisaStok() + ")",
                                stokEx.getMessage());
                        muatProduk(false);
                    } else if (cause instanceof SQLException sqlEx) {
                        ModernModal.error(KasirView.this, "Kesalahan Database",
                                TransaksiController.pesanSqlError(sqlEx));
                    } else if (cause instanceof IllegalArgumentException argEx) {
                        ModernModal.error(KasirView.this, "Transaksi Tidak Valid", argEx.getMessage());
                        muatProduk(false);
                    } else {
                        ModernModal.error(KasirView.this, "Kesalahan Sistem",
                                "Transaksi gagal diproses:\n" + (cause != null ? cause.getMessage() : ex.getMessage()));
                    }
                }
            }
        };
        worker.execute();
    }

    private void batalkanTransaksi() {
        if (isProcessingCheckout || transaksiAktif.getListKeranjang().isEmpty()) {
            return;
        }

        boolean disetujui = ModernModal.confirm(
                this,
                "Batalkan Transaksi",
                "Batalkan transaksi yang sedang berjalan dan kosongkan keranjang?",
                "Ya, Batalkan"
        );
        if (disetujui) {
            transaksiController.bersihkanKeranjang(transaksiAktif);
            txtBayar.setText("");
            mulaiTransaksiBaru();
            perbaruiKeranjang();
            toastManager.show("Transaksi Dibatalkan", "Sesi transaksi baru siap digunakan.", ToastType.INFO);
        }
    }

    private void mulaiTransaksiBaru() {
        transaksiAktif = transaksiController.buatTransaksi(penggunaAktif);
    }

    // =========================================================
    // SHORTCUT DAN KOMPONEN BANTUAN
    // =========================================================

    private void pasangShortcutKeyboard() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F2"), "fokusPencarian");
        getActionMap().put("fokusPencarian", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtCari.requestFocusInWindow();
                txtCari.selectAll();
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F9"), "prosesBayar");
        getActionMap().put("prosesBayar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnBayar.isEnabled()) {
                    prosesPembayaran();
                } else if (!transaksiAktif.getListKeranjang().isEmpty()) {
                    txtBayar.requestFocusInWindow();
                    toastManager.show("Nominal Belum Cukup", "Masukkan uang dibayar minimal sebesar grand total.", ToastType.INFO);
                }
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "batalkanTransaksi");
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
        label.setFont(UITheme.font(utama ? Font.BOLD : Font.PLAIN, utama ? 13f : 12f));
        label.setForeground(utama ? UITheme.TEXT_MAIN : UITheme.TEXT_MUTED);

        nilai.setFont(UITheme.font(Font.BOLD, utama ? 20f : 12f));
        nilai.setForeground(UITheme.TEXT_MAIN);
        nilai.setHorizontalAlignment(SwingConstants.RIGHT);

        baris.add(label, BorderLayout.WEST);
        baris.add(nilai, BorderLayout.EAST);
        baris.setAlignmentX(Component.LEFT_ALIGNMENT);
        baris.setMaximumSize(new Dimension(Integer.MAX_VALUE, utama ? 30 : 20));
        return baris;
    }

    private static void konfigurasiTombol(RoundedButton btn, int width, int height, int fontStyle) {
        btn.setFont(UITheme.font(fontStyle, 12f));
        Dimension size = new Dimension(width, height);
        btn.setPreferredSize(size);
        btn.setMinimumSize(size);
        btn.setMaximumSize(size);
    }

    private String htmlSingkat(String teks, int batas) {
        if (teks == null) {
            return "-";
        }
        String dipotong = teks.length() > batas
                ? teks.substring(0, Math.max(0, batas - 1)) + "\u2026"
                : teks;
        String aman = dipotong.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return "<html>" + aman + "</html>";
    }
}
