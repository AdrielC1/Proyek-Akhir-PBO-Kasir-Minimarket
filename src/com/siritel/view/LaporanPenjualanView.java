package com.siritel.view;

import com.siritel.controller.TransaksiController;
import com.siritel.model.LaporanPenjualan;
import com.siritel.model.Pengguna;
import com.siritel.model.Transaksi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableModel;

/**
 * Halaman laporan penjualan SI-RITEL.
 * View hanya menangani tampilan, input, pemanggilan Controller,
 * dan penyajian hasil. Query database tetap berada di DAO.
 */
public class LaporanPenjualanView extends JPanel {

    private static final Color BG_PANEL = new Color(248, 250, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color PRIMARY_BLUE_LIGHT = new Color(239, 246, 255);
    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color SUCCESS_GREEN = new Color(22, 163, 74);

    private static final DateTimeFormatter FORMAT_TANGGAL =
        DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final TransaksiController transaksiController;
    private final JSpinner spTanggalAwal;
    private final JSpinner spTanggalAkhir;

    private final JLabel lblJumlahTransaksi = new JLabel("0");
    private final JLabel lblTotalOmzet = new JLabel("Rp0");
    private final JLabel lblTotalPajak = new JLabel("Rp0");
    private final JLabel lblStatus = new JLabel("Pilih periode laporan.");

    private final DefaultTableModel modelTabel =
        new DefaultTableModel(
            new Object[]{"Tanggal", "Jumlah Transaksi", "Total Omzet", "Total Pajak"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 1 ? Integer.class : String.class;
            }
        };

    private final JTable tabelLaporan = new JTable(modelTabel);
    private LaporanPenjualan laporanAktif;

    public LaporanPenjualanView() {
        this(null);
    }

    /**
     * Parameter penggunaAktif disediakan agar kompatibel dengan MainFrame.
     */
    public LaporanPenjualanView(Pengguna penggunaAktif) {
        transaksiController = new TransaksiController();

        LocalDate hariIni = LocalDate.now();
        spTanggalAwal = buatPemilihTanggal(hariIni.withDayOfMonth(1));
        spTanggalAkhir = buatPemilihTanggal(hariIni);

        setLayout(new BorderLayout(0, 16));
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        bangunTampilan();
        muatLaporan();
    }

    private JSpinner buatPemilihTanggal(LocalDate tanggal) {
        Date nilai = Date.from(
            tanggal.atStartOfDay(ZoneId.systemDefault()).toInstant()
        );

        JSpinner spinner = new JSpinner(
            new SpinnerDateModel(nilai, null, null,
                java.util.Calendar.DAY_OF_MONTH)
        );
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd-MM-yyyy"));
        spinner.setPreferredSize(new Dimension(145, 38));
        spinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return spinner;
    }

    private void bangunTampilan() {
        add(bangunPanelAtas(), BorderLayout.NORTH);
        add(bangunPanelKonten(), BorderLayout.CENTER);
        add(bangunPanelBawah(), BorderLayout.SOUTH);
    }

    private JPanel bangunPanelAtas() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        JLabel judul = new JLabel("Laporan Penjualan");
        judul.setFont(new Font("SansSerif", Font.BOLD, 20));
        judul.setForeground(TEXT_DARK);

        JLabel deskripsi = new JLabel(
            "Pantau ringkasan dan perkembangan penjualan harian."
        );
        deskripsi.setFont(new Font("SansSerif", Font.PLAIN, 12));
        deskripsi.setForeground(TEXT_MUTED);

        JPanel panelJudul = new JPanel(new GridLayout(2, 1, 0, 4));
        panelJudul.setOpaque(false);
        panelJudul.add(judul);
        panelJudul.add(deskripsi);

        JPanel panelFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panelFilter.setBackground(CARD_BG);
        panelFilter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        panelFilter.add(labelKecil("Tanggal Awal"));
        panelFilter.add(spTanggalAwal);
        panelFilter.add(labelKecil("Tanggal Akhir"));
        panelFilter.add(spTanggalAkhir);

        JButton btnTampilkan = buatTombol("Tampilkan Laporan", PRIMARY_BLUE);
        btnTampilkan.addActionListener(e -> muatLaporan());
        panelFilter.add(btnTampilkan);

        panel.add(panelJudul, BorderLayout.NORTH);
        panel.add(panelFilter, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel bangunPanelKonten() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        JPanel panelRingkasan = new JPanel(new GridLayout(1, 3, 14, 0));
        panelRingkasan.setOpaque(false);
        panelRingkasan.add(buatKartuRingkasan(
            "Jumlah Transaksi", lblJumlahTransaksi,
            "Transaksi pada periode terpilih", PRIMARY_BLUE
        ));
        panelRingkasan.add(buatKartuRingkasan(
            "Total Omzet", lblTotalOmzet,
            "Akumulasi grand total transaksi", SUCCESS_GREEN
        ));
        panelRingkasan.add(buatKartuRingkasan(
            "Total Pajak", lblTotalPajak,
            "Akumulasi pajak transaksi", new Color(217, 119, 6)
        ));

        JPanel panelTabel = new JPanel(new BorderLayout(0, 10));
        panelTabel.setBackground(CARD_BG);
        panelTabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JLabel judulTabel = new JLabel("Rincian Penjualan Harian");
        judulTabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        judulTabel.setForeground(TEXT_DARK);

        aturTampilanTabel();

        JScrollPane scrollPane = new JScrollPane(tabelLaporan);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollPane.getViewport().setBackground(Color.WHITE);

        panelTabel.add(judulTabel, BorderLayout.NORTH);
        panelTabel.add(scrollPane, BorderLayout.CENTER);

        panel.add(panelRingkasan, BorderLayout.NORTH);
        panel.add(panelTabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel bangunPanelBawah() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblStatus.setForeground(TEXT_MUTED);

        JPanel panelTombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelTombol.setOpaque(false);

        JButton btnCetak = buatTombol("Cetak Laporan", SUCCESS_GREEN);
        btnCetak.addActionListener(e -> cetakLaporan());
        panelTombol.add(btnCetak);

        panel.add(lblStatus, BorderLayout.WEST);
        panel.add(panelTombol, BorderLayout.EAST);
        return panel;
    }

    private JPanel buatKartuRingkasan(
            String judul, JLabel nilai, String keterangan, Color warnaAksen) {

        JPanel kartu = new JPanel(new BorderLayout(0, 8));
        kartu.setBackground(CARD_BG);
        kartu.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblJudul = labelKecil(judul);
        nilai.setFont(new Font("SansSerif", Font.BOLD, 21));
        nilai.setForeground(warnaAksen);

        JLabel lblKeterangan = new JLabel(keterangan);
        lblKeterangan.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblKeterangan.setForeground(TEXT_MUTED);

        kartu.add(lblJudul, BorderLayout.NORTH);
        kartu.add(nilai, BorderLayout.CENTER);
        kartu.add(lblKeterangan, BorderLayout.SOUTH);
        return kartu;
    }

    private JLabel labelKecil(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(TEXT_MUTED);
        return label;
    }

    private JButton buatTombol(String teks, Color warna) {
        JButton tombol = new JButton(teks);
        tombol.setFont(new Font("SansSerif", Font.BOLD, 12));
        tombol.setForeground(Color.WHITE);
        tombol.setBackground(warna);
        tombol.setFocusPainted(false);
        tombol.setBorderPainted(false);
        tombol.setOpaque(true);
        tombol.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tombol.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        return tombol;
    }

    private void aturTampilanTabel() {
        tabelLaporan.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tabelLaporan.setForeground(TEXT_DARK);
        tabelLaporan.setRowHeight(34);
        tabelLaporan.setShowVerticalLines(false);
        tabelLaporan.setGridColor(BORDER_COLOR);
        tabelLaporan.setSelectionBackground(PRIMARY_BLUE_LIGHT);
        tabelLaporan.setSelectionForeground(TEXT_DARK);
        tabelLaporan.getTableHeader().setFont(
            new Font("SansSerif", Font.BOLD, 12)
        );
        tabelLaporan.getTableHeader().setForeground(TEXT_DARK);
        tabelLaporan.getTableHeader().setBackground(new Color(241, 245, 249));
        tabelLaporan.getTableHeader().setReorderingAllowed(false);
        tabelLaporan.setFillsViewportHeight(true);
    }

    private LocalDate ambilTanggal(JSpinner spinner) {
        Date tanggal = (Date) spinner.getValue();
        return tanggal.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
    }

    private void muatLaporan() {
        LocalDate tanggalAwal = ambilTanggal(spTanggalAwal);
        LocalDate tanggalAkhir = ambilTanggal(spTanggalAkhir);

        if (tanggalAwal.isAfter(tanggalAkhir)) {
            JOptionPane.showMessageDialog(
                this,
                "Tanggal awal tidak boleh melebihi tanggal akhir.",
                "Periode Tidak Valid",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            laporanAktif = transaksiController.buatLaporan(
                tanggalAwal, tanggalAkhir
            );

            perbaruiRingkasan();
            perbaruiTabel();

            lblStatus.setText(
                "Periode: " + tanggalAwal.format(FORMAT_TANGGAL)
                + " sampai " + tanggalAkhir.format(FORMAT_TANGGAL)
                + " | " + laporanAktif.hitungJumlahTransaksi()
                + " transaksi ditemukan."
            );

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Gagal mengambil laporan penjualan:\n" + ex.getMessage(),
                "Kesalahan Database",
                JOptionPane.ERROR_MESSAGE
            );
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Data Tidak Valid",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void perbaruiRingkasan() {
        NumberFormat rupiah =
            NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        lblJumlahTransaksi.setText(
            String.valueOf(laporanAktif.hitungJumlahTransaksi())
        );
        lblTotalOmzet.setText(
            rupiah.format(laporanAktif.hitungTotalOmzet())
        );
        lblTotalPajak.setText(
            rupiah.format(laporanAktif.hitungTotalPajakTerkumpul())
        );
    }

    private void perbaruiTabel() {
        modelTabel.setRowCount(0);

        NumberFormat rupiah =
            NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        Map<LocalDate, List<Transaksi>> daftarHarian =
            laporanAktif.getDaftarHarian();

        for (Map.Entry<LocalDate, List<Transaksi>> entry
                : daftarHarian.entrySet()) {

            List<Transaksi> transaksiHarian = entry.getValue();

            double omzet =
                laporanAktif.hitungOmzetHarian(transaksiHarian);
            double pajak =
                laporanAktif.hitungPajakHarian(transaksiHarian);

            modelTabel.addRow(new Object[]{
                entry.getKey().format(FORMAT_TANGGAL),
                transaksiHarian.size(),
                rupiah.format(omzet),
                rupiah.format(pajak)
            });
        }
    }

    private void cetakLaporan() {
        if (laporanAktif == null) {
            JOptionPane.showMessageDialog(
                this,
                "Belum ada laporan yang dapat dicetak.",
                "Laporan Kosong",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JTextArea areaCetak = new JTextArea(laporanAktif.cetak());
        areaCetak.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        areaCetak.setEditable(false);
        areaCetak.setCaretPosition(0);

        try {
            areaCetak.print(null, null, true, null, null, true);
        } catch (java.awt.print.PrinterException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Gagal mencetak laporan:\n" + ex.getMessage(),
                "Kesalahan Cetak",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
