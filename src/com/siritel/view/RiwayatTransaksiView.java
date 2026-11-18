package com.siritel.view;

import com.siritel.controller.TransaksiController;
import com.siritel.model.ItemTransaksi;
import com.siritel.model.Pengguna;
import com.siritel.model.Transaksi;
import com.siritel.model.TransaksiRingkas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.print.PrinterException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.ListSelectionModel;

/**
 * Halaman riwayat transaksi SI-RITEL.
 *
 * View hanya menangani tampilan, input pengguna, dan pemanggilan Controller.
 * Seluruh akses database dilakukan melalui TransaksiController dan TransaksiDAO.
 */
public class RiwayatTransaksiView extends JPanel {

    // Palet warna disamakan dengan KasirView.
    private static final Color BG_PANEL = new Color(248, 250, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color PRIMARY_BLUE_LIGHT = new Color(239, 246, 255);
    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color SUCCESS_GREEN = new Color(22, 163, 74);

    private static final int BATAS_PER_HALAMAN = 8;

    private static final DateTimeFormatter FORMAT_TANGGAL =
        DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("id", "ID"));

    private static final DateTimeFormatter FORMAT_TANGGAL_WAKTU =
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", new Locale("id", "ID"));

    private final TransaksiController transaksiController;

    private final JSpinner spTanggalAwal = buatSpinnerTanggal(LocalDate.now().withDayOfMonth(1));
    private final JSpinner spTanggalAkhir = buatSpinnerTanggal(LocalDate.now());

    private final JTextField txtCari = new JTextField();

    private final DefaultTableModel modelTabel = new DefaultTableModel(
        new Object[]{"ID Transaksi", "Tanggal", "Kasir", "Item", "Grand Total"},
        0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable tabelTransaksi = new JTable(modelTabel);

    private final JLabel lblJumlahTransaksi = new JLabel("0 transaksi");
    private final JLabel lblInfoHalaman = new JLabel("Halaman 1");
    private final JButton btnSebelumnya = new JButton("‹ Sebelumnya");
    private final JButton btnSelanjutnya = new JButton("Selanjutnya");

    private final JLabel lblIdTransaksi = new JLabel("-");
    private final JLabel lblTanggal = new JLabel("-");
    private final JLabel lblKasir = new JLabel("-");
    private final JLabel lblStatus = new JLabel("Pilih transaksi");
    private final JLabel lblSubtotal = new JLabel(formatRupiah(0));
    private final JLabel lblPajak = new JLabel(formatRupiah(0));
    private final JLabel lblGrandTotal = new JLabel(formatRupiah(0));
    private final JLabel lblBayar = new JLabel(formatRupiah(0));
    private final JLabel lblKembalian = new JLabel(formatRupiah(0));

    private final DefaultTableModel modelDetail = new DefaultTableModel(
        new Object[]{"Produk", "Qty", "Harga", "Subtotal"},
        0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable tabelDetail = new JTable(modelDetail);
    private final JButton btnCetak = new JButton("Cetak Struk");

    private List<TransaksiRingkas> daftarTransaksi = new ArrayList<>();
    private LocalDate tanggalAwalAktif = LocalDate.now().withDayOfMonth(1);
    private LocalDate tanggalAkhirAktif = LocalDate.now();
    private int halamanAktif = 1;
    private int jumlahSeluruhTransaksi = 0;
    private Transaksi transaksiTerpilih;

    public RiwayatTransaksiView() {
        this(null);
    }

    /**
     * Konstruktor yang dapat menerima pengguna aktif dari MainFrame.
     * Parameter disediakan agar konsisten dengan pola pemanggilan halaman lain.
     */
    public RiwayatTransaksiView(Pengguna penggunaAktif) {
        this.transaksiController = new TransaksiController();

        setLayout(new BorderLayout(16, 0));
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        aturTabel();
        bangunTampilan();
        pasangListener();

        muatRiwayat();
    }

    private void bangunTampilan() {
        JPanel panelKiri = bangunPanelDaftar();
        JPanel panelKanan = bangunPanelDetail();

        panelKiri.setPreferredSize(new Dimension(650, 600));
        panelKanan.setPreferredSize(new Dimension(430, 600));

        add(panelKiri, BorderLayout.CENTER);
        add(Box.createHorizontalStrut(16), BorderLayout.WEST);
        add(panelKanan, BorderLayout.EAST);
    }

    private JPanel bangunPanelDaftar() {
        JPanel panel = panelKartu();
        panel.setLayout(new BorderLayout(0, 14));

        JLabel judul = new JLabel("Riwayat Transaksi");
        judul.setFont(new Font("SansSerif", Font.BOLD, 21));
        judul.setForeground(TEXT_DARK);

        JLabel deskripsi = new JLabel("Log penjualan dan pencarian transaksi");
        deskripsi.setFont(new Font("SansSerif", Font.PLAIN, 12));
        deskripsi.setForeground(TEXT_MUTED);

        JPanel panelJudul = new JPanel();
        panelJudul.setOpaque(false);
        panelJudul.setLayout(new BoxLayout(panelJudul, BoxLayout.Y_AXIS));
        panelJudul.add(judul);
        panelJudul.add(Box.createVerticalStrut(4));
        panelJudul.add(deskripsi);

        JPanel panelFilter = new JPanel(new GridLayout(2, 1, 0, 8));
        panelFilter.setOpaque(false);

        JPanel barisTanggal = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barisTanggal.setOpaque(false);
        barisTanggal.add(labelKecil("Dari"));
        barisTanggal.add(spTanggalAwal);
        barisTanggal.add(labelKecil("Sampai"));
        barisTanggal.add(spTanggalAkhir);

        JButton btnTampilkan = tombolUtama("Tampilkan");
        btnTampilkan.addActionListener(e -> terapkanFilterTanggal());

        JButton btnReset = tombolSekunder("Reset");
        btnReset.addActionListener(e -> resetFilter());

        barisTanggal.add(btnTampilkan);
        barisTanggal.add(btnReset);

        JPanel barisCari = new JPanel(new BorderLayout(8, 0));
        barisCari.setOpaque(false);
        txtCari.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtCari.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        txtCari.setToolTipText("Cari ID transaksi atau nama kasir");
        barisCari.add(txtCari, BorderLayout.CENTER);
        barisCari.add(lblJumlahTransaksi, BorderLayout.EAST);
        lblJumlahTransaksi.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblJumlahTransaksi.setForeground(TEXT_MUTED);

        panelFilter.add(barisTanggal);
        panelFilter.add(barisCari);

        JPanel panelAtas = new JPanel(new BorderLayout(0, 14));
        panelAtas.setOpaque(false);
        panelAtas.add(panelJudul, BorderLayout.NORTH);
        panelAtas.add(panelFilter, BorderLayout.CENTER);

        JScrollPane scrollTabel = new JScrollPane(tabelTransaksi);
        scrollTabel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollTabel.getViewport().setBackground(Color.WHITE);

        JPanel panelPagination = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelPagination.setOpaque(false);
        lblInfoHalaman.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblInfoHalaman.setForeground(TEXT_MUTED);

        gayaTombolKecil(btnSebelumnya);
        gayaTombolKecil(btnSelanjutnya);
        panelPagination.add(lblInfoHalaman);
        panelPagination.add(btnSebelumnya);
        panelPagination.add(btnSelanjutnya);

        panel.add(panelAtas, BorderLayout.NORTH);
        panel.add(scrollTabel, BorderLayout.CENTER);
        panel.add(panelPagination, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel bangunPanelDetail() {
        JPanel panel = panelKartu();
        panel.setLayout(new BorderLayout(0, 12));

        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setOpaque(false);

        JLabel judul = new JLabel("Detail Transaksi");
        judul.setFont(new Font("SansSerif", Font.BOLD, 18));
        judul.setForeground(TEXT_DARK);

        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblStatus.setForeground(TEXT_MUTED);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(241, 245, 249));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));

        panelHeader.add(judul, BorderLayout.WEST);
        panelHeader.add(lblStatus, BorderLayout.EAST);

        JPanel panelIdentitas = new JPanel(new GridLayout(3, 2, 8, 10));
        panelIdentitas.setOpaque(false);
        panelIdentitas.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        panelIdentitas.add(labelKecil("ID Transaksi"));
        panelIdentitas.add(lblIdTransaksi);
        panelIdentitas.add(labelKecil("Tanggal"));
        panelIdentitas.add(lblTanggal);
        panelIdentitas.add(labelKecil("Kasir"));
        panelIdentitas.add(lblKasir);

        aturLabelNilai(lblIdTransaksi);
        aturLabelNilai(lblTanggal);
        aturLabelNilai(lblKasir);

        tabelDetail.setRowHeight(30);
        tabelDetail.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tabelDetail.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tabelDetail.getTableHeader().setReorderingAllowed(false);
        tabelDetail.setFillsViewportHeight(true);

        JScrollPane scrollDetail = new JScrollPane(tabelDetail);
        scrollDetail.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        JPanel panelRingkasan = new JPanel();
        panelRingkasan.setOpaque(false);
        panelRingkasan.setLayout(new BoxLayout(panelRingkasan, BoxLayout.Y_AXIS));
        panelRingkasan.setBorder(BorderFactory.createMatteBorder(
            1, 0, 0, 0, BORDER_COLOR
        ));
        panelRingkasan.add(Box.createVerticalStrut(10));
        panelRingkasan.add(barisNilai("Subtotal", lblSubtotal, false));
        panelRingkasan.add(Box.createVerticalStrut(7));
        panelRingkasan.add(barisNilai("Total Pajak", lblPajak, false));
        panelRingkasan.add(Box.createVerticalStrut(7));
        panelRingkasan.add(barisNilai("Grand Total", lblGrandTotal, true));
        panelRingkasan.add(Box.createVerticalStrut(7));
        panelRingkasan.add(barisNilai("Bayar", lblBayar, false));
        panelRingkasan.add(Box.createVerticalStrut(7));
        panelRingkasan.add(barisNilai("Kembalian", lblKembalian, false));

        btnCetak.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCetak.setForeground(Color.WHITE);
        btnCetak.setBackground(PRIMARY_BLUE);
        btnCetak.setOpaque(true);
        btnCetak.setBorderPainted(false);
        btnCetak.setFocusPainted(false);
        btnCetak.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCetak.setPreferredSize(new Dimension(120, 42));
        btnCetak.setEnabled(false);
        btnCetak.addActionListener(e -> cetakStruk());

        JPanel panelBawah = new JPanel(new BorderLayout(0, 10));
        panelBawah.setOpaque(false);
        panelBawah.add(panelRingkasan, BorderLayout.CENTER);
        panelBawah.add(btnCetak, BorderLayout.SOUTH);

        JPanel panelKontenAtas = new JPanel(new BorderLayout(0, 10));
        panelKontenAtas.setOpaque(false);
        panelKontenAtas.add(panelIdentitas, BorderLayout.NORTH);
        panelKontenAtas.add(scrollDetail, BorderLayout.CENTER);

        panel.add(panelHeader, BorderLayout.NORTH);
        panel.add(panelKontenAtas, BorderLayout.CENTER);
        panel.add(panelBawah, BorderLayout.SOUTH);

        return panel;
    }

    private void aturTabel() {
        tabelTransaksi.setRowHeight(34);
        tabelTransaksi.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tabelTransaksi.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tabelTransaksi.getTableHeader().setBackground(new Color(241, 245, 249));
        tabelTransaksi.getTableHeader().setForeground(TEXT_DARK);
        tabelTransaksi.getTableHeader().setReorderingAllowed(false);
        tabelTransaksi.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelTransaksi.setFillsViewportHeight(true);
        tabelTransaksi.setShowVerticalLines(false);
        tabelTransaksi.setGridColor(BORDER_COLOR);
    }

    private void pasangListener() {
        tabelTransaksi.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                tampilkanDetailTerpilih();
            }
        });

        txtCari.addActionListener(e -> tampilkanTabelHalaman());

        btnSebelumnya.addActionListener(e -> {
            if (halamanAktif > 1) {
                halamanAktif--;
                muatRiwayat();
            }
        });

        btnSelanjutnya.addActionListener(e -> {
            int totalHalaman = hitungJumlahHalaman();
            if (halamanAktif < totalHalaman) {
                halamanAktif++;
                muatRiwayat();
            }
        });
    }

    private void terapkanFilterTanggal() {
        LocalDate awal = ambilTanggal(spTanggalAwal);
        LocalDate akhir = ambilTanggal(spTanggalAkhir);

        if (awal.isAfter(akhir)) {
            JOptionPane.showMessageDialog(
                this,
                "Tanggal awal tidak boleh melebihi tanggal akhir.",
                "Filter tanggal tidak valid",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        tanggalAwalAktif = awal;
        tanggalAkhirAktif = akhir;
        halamanAktif = 1;
        txtCari.setText("");
        muatRiwayat();
    }

    private void resetFilter() {
        tanggalAwalAktif = LocalDate.now().withDayOfMonth(1);
        tanggalAkhirAktif = LocalDate.now();
        spTanggalAwal.setValue(java.sql.Date.valueOf(tanggalAwalAktif));
        spTanggalAkhir.setValue(java.sql.Date.valueOf(tanggalAkhirAktif));
        txtCari.setText("");
        halamanAktif = 1;
        muatRiwayat();
    }

    private void muatRiwayat() {
        try {
            jumlahSeluruhTransaksi = transaksiController.getJumlahRiwayatTransaksi(
                tanggalAwalAktif,
                tanggalAkhirAktif
            );

            int totalHalaman = hitungJumlahHalaman();
            if (halamanAktif > totalHalaman && totalHalaman > 0) {
                halamanAktif = totalHalaman;
            }

            daftarTransaksi = transaksiController.getRiwayatTransaksi(
                tanggalAwalAktif,
                tanggalAkhirAktif,
                halamanAktif,
                BATAS_PER_HALAMAN
            );

            tampilkanTabelHalaman();
            perbaruiPagination();

            if (daftarTransaksi.isEmpty()) {
                kosongkanDetail();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Gagal memuat riwayat transaksi.\n" + ex.getMessage(),
                "Kesalahan",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void tampilkanTabelHalaman() {
        modelTabel.setRowCount(0);

        String kataKunci = txtCari.getText() == null
            ? ""
            : txtCari.getText().trim().toLowerCase(Locale.ROOT);

        for (TransaksiRingkas transaksi : daftarTransaksi) {
            boolean cocok = transaksi.getIdTransaksi()
                    .toLowerCase(Locale.ROOT).contains(kataKunci)
                || transaksi.getNamaKasir()
                    .toLowerCase(Locale.ROOT).contains(kataKunci);

            if (cocok) {
                modelTabel.addRow(new Object[]{
                    transaksi.getIdTransaksi(),
                    transaksi.getTanggal().format(FORMAT_TANGGAL_WAKTU),
                    transaksi.getNamaKasir(),
                    transaksi.getJumlahItem() + " item",
                    formatRupiah(transaksi.getGrandTotal())
                });
            }
        }

        if (modelTabel.getRowCount() == 0) {
            kosongkanDetail();
        }
    }

    private void tampilkanDetailTerpilih() {
        int baris = tabelTransaksi.getSelectedRow();
        if (baris < 0) {
            return;
        }

        String idTransaksi = modelTabel.getValueAt(baris, 0).toString();

        try {
            transaksiTerpilih = transaksiController.getDetailTransaksi(idTransaksi);

            if (transaksiTerpilih == null) {
                kosongkanDetail();
                return;
            }

            isiPanelDetail(transaksiTerpilih);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Gagal mengambil detail transaksi.\n" + ex.getMessage(),
                "Kesalahan",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void isiPanelDetail(Transaksi transaksi) {
        lblIdTransaksi.setText(transaksi.getIdTransaksi());
        lblTanggal.setText(transaksi.getTanggal().format(FORMAT_TANGGAL_WAKTU));
        lblKasir.setText(transaksi.getKasir().getNamaLengkap());

        lblStatus.setText("LUNAS");
        lblStatus.setForeground(SUCCESS_GREEN);
        lblStatus.setBackground(new Color(240, 253, 244));

        modelDetail.setRowCount(0);
        for (ItemTransaksi item : transaksi.getListKeranjang()) {
            modelDetail.addRow(new Object[]{
                item.getProduk().getNama(),
                item.getKuantitas(),
                formatRupiah(item.getHargaSatuan()),
                formatRupiah(item.getSubtotal())
            });
        }

        lblSubtotal.setText(formatRupiah(transaksi.getSubtotalBarang()));
        lblPajak.setText(formatRupiah(transaksi.getTotalPajak()));
        lblGrandTotal.setText(formatRupiah(transaksi.getGrandTotal()));
        lblBayar.setText(formatRupiah(transaksi.getBayar()));
        lblKembalian.setText(formatRupiah(transaksi.getKembalian()));

        btnCetak.setEnabled(true);
    }

    private void kosongkanDetail() {
        transaksiTerpilih = null;

        lblIdTransaksi.setText("-");
        lblTanggal.setText("-");
        lblKasir.setText("-");
        lblStatus.setText("Pilih transaksi");
        lblStatus.setForeground(TEXT_MUTED);
        lblStatus.setBackground(new Color(241, 245, 249));

        modelDetail.setRowCount(0);

        lblSubtotal.setText(formatRupiah(0));
        lblPajak.setText(formatRupiah(0));
        lblGrandTotal.setText(formatRupiah(0));
        lblBayar.setText(formatRupiah(0));
        lblKembalian.setText(formatRupiah(0));

        btnCetak.setEnabled(false);
    }

    private void cetakStruk() {
        if (transaksiTerpilih == null) {
            return;
        }

        JTextArea areaStruk = new JTextArea(transaksiTerpilih.cetak());
        areaStruk.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        areaStruk.setEditable(false);

        try {
            boolean selesai = areaStruk.print();
            if (selesai) {
                JOptionPane.showMessageDialog(
                    this,
                    "Struk berhasil dikirim ke printer.",
                    "Cetak Struk",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Gagal mencetak struk.\n" + ex.getMessage(),
                "Cetak Struk",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void perbaruiPagination() {
        int totalHalaman = hitungJumlahHalaman();

        lblJumlahTransaksi.setText(jumlahSeluruhTransaksi + " transaksi");
        lblInfoHalaman.setText(
            jumlahSeluruhTransaksi == 0
                ? "Tidak ada data"
                : "Halaman " + halamanAktif + " dari " + totalHalaman
        );

        btnSebelumnya.setEnabled(halamanAktif > 1);
        btnSelanjutnya.setEnabled(halamanAktif < totalHalaman);
    }

    private int hitungJumlahHalaman() {
        return (int) Math.ceil(
            (double) jumlahSeluruhTransaksi / BATAS_PER_HALAMAN
        );
    }

    private static JSpinner buatSpinnerTanggal(LocalDate tanggal) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(
            java.sql.Date.valueOf(tanggal),
            null,
            null,
            java.util.Calendar.DAY_OF_MONTH
        ));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        spinner.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return spinner;
    }

    private static LocalDate ambilTanggal(JSpinner spinner) {
        java.util.Date tanggal = (java.util.Date) spinner.getValue();
        return new java.sql.Date(tanggal.getTime()).toLocalDate();
    }

    private JPanel panelKartu() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        return panel;
    }

    private JLabel labelKecil(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        label.setForeground(TEXT_MUTED);
        return label;
    }

    private void aturLabelNilai(JLabel label) {
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(TEXT_DARK);
    }

    private JPanel barisNilai(String teks, JLabel nilai, boolean utama) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel label = new JLabel(teks);
        label.setFont(new Font("SansSerif", utama ? Font.BOLD : Font.PLAIN, 12));
        label.setForeground(utama ? TEXT_DARK : TEXT_MUTED);

        nilai.setFont(new Font("SansSerif", Font.BOLD, utama ? 17 : 12));
        nilai.setForeground(TEXT_DARK);
        nilai.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(label, BorderLayout.WEST);
        panel.add(nilai, BorderLayout.EAST);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, utama ? 28 : 22));
        return panel;
    }

    private JButton tombolUtama(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(new Font("SansSerif", Font.BOLD, 11));
        tombol.setForeground(Color.WHITE);
        tombol.setBackground(PRIMARY_BLUE);
        tombol.setOpaque(true);
        tombol.setBorderPainted(false);
        tombol.setFocusPainted(false);
        tombol.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tombol.setMargin(new Insets(7, 12, 7, 12));
        return tombol;
    }

    private JButton tombolSekunder(String teks) {
        JButton tombol = new JButton(teks);
        tombol.setFont(new Font("SansSerif", Font.BOLD, 11));
        tombol.setForeground(TEXT_DARK);
        tombol.setBackground(Color.WHITE);
        tombol.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        tombol.setFocusPainted(false);
        tombol.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tombol.setMargin(new Insets(7, 12, 7, 12));
        return tombol;
    }

    private void gayaTombolKecil(JButton tombol) {
        tombol.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tombol.setForeground(TEXT_DARK);
        tombol.setBackground(Color.WHITE);
        tombol.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        tombol.setFocusPainted(false);
        tombol.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tombol.setMargin(new Insets(6, 9, 6, 9));
    }

    private static String formatRupiah(double nominal) {
        NumberFormat format = NumberFormat.getCurrencyInstance(
            new Locale("id", "ID")
        );
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(nominal).replace('\u00A0', ' ');
    }
}
