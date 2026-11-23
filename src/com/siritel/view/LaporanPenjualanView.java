package com.siritel.view;

import com.siritel.controller.TransaksiController;
import com.siritel.model.LaporanPenjualan;
import com.siritel.model.Transaksi;
import com.siritel.view.ui.ButtonVariant;
import com.siritel.view.ui.LaporanPrinter;
import com.siritel.view.ui.ModernDatePicker;
import com.siritel.view.ui.ModernModal;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Halaman laporan penjualan SI-RITEL.
 * Menggunakan komponen UI terpusat dari {@code com.siritel.view.ui}, pemilih
 * tanggal modern, pemuatan asinkron non-blocking, serta pratinjau/cetak dokumen A4.
 */
public class LaporanPenjualanView extends JPanel implements RefreshableView {

    private static final DateTimeFormatter FORMAT_TANGGAL =
            DateTimeFormatter.ofPattern("dd MMM yyyy", UITheme.ID_LOCALE);

    private final TransaksiController transaksiController = new TransaksiController();
    private final ToastManager toastManager = new ToastManager(this);

    private final ModernDatePicker dpTanggalAwal = new ModernDatePicker(LocalDate.now().withDayOfMonth(1), false);
    private final ModernDatePicker dpTanggalAkhir = new ModernDatePicker(LocalDate.now(), false);
    private final RoundedButton btnTampilkan = new RoundedButton("Tampilkan Laporan", ButtonVariant.PRIMARY, UITheme.BUTTON_RADIUS);
    private final RoundedButton btnSegarkan = new RoundedButton("Segarkan", ButtonVariant.OUTLINE, UITheme.BUTTON_RADIUS);
    private final RoundedButton btnCetak = new RoundedButton("Cetak / Pratinjau PDF", ButtonVariant.TEAL, UITheme.BUTTON_RADIUS);

    private final JLabel lblJumlahTransaksi = new JLabel("0");
    private final JLabel lblTotalOmzet = new JLabel(UITheme.formatRupiah(0));
    private final JLabel lblTotalPajak = new JLabel(UITheme.formatRupiah(0));
    private final JLabel lblStatus = new JLabel("Pilih periode laporan.");

    private final DefaultTableModel modelTabel = new DefaultTableModel(
            new Object[]{"Tanggal", "Jumlah Transaksi", "Total Omzet", "Total Pajak"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable tabelLaporan = new JTable(modelTabel);
    private LaporanPenjualan laporanAktif;
    private int loadRequestId = 0;

    public LaporanPenjualanView() {
        setLayout(new BorderLayout(0, 14));
        setBackground(UITheme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        bangunTampilan();
    }

    @Override
    public void refreshData() {
        muatLaporan(false);
    }

    private void bangunTampilan() {
        add(bangunPanelAtas(), BorderLayout.NORTH);
        add(bangunPanelKonten(), BorderLayout.CENTER);
        add(bangunPanelBawah(), BorderLayout.SOUTH);
    }

    private JPanel bangunPanelAtas() {
        JPanel card = new RoundedCardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Baris atas: judul & deskripsi di kiri, tombol aksi & segarkan di kanan
        JPanel headerRow = new JPanel(new BorderLayout(8, 0));
        headerRow.setOpaque(false);

        JPanel panelJudul = new JPanel(new GridLayout(2, 1, 0, 2));
        panelJudul.setOpaque(false);
        JLabel judul = new JLabel("Laporan Penjualan");
        judul.setFont(UITheme.font(Font.BOLD, 16f));
        judul.setForeground(UITheme.TEXT_MAIN);
        JLabel deskripsi = new JLabel("Pantau ringkasan omzet, pajak, dan perkembangan penjualan harian");
        deskripsi.setFont(UITheme.font(Font.PLAIN, 12f));
        deskripsi.setForeground(UITheme.TEXT_MUTED);
        panelJudul.add(judul);
        panelJudul.add(deskripsi);

        konfigurasiTombol(btnSegarkan, 90, UITheme.TOOLBAR_BUTTON_HEIGHT, Font.PLAIN);
        btnSegarkan.addActionListener(e -> muatLaporan(true));

        JPanel segarkanWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        segarkanWrap.setOpaque(false);
        segarkanWrap.add(btnSegarkan);

        headerRow.add(panelJudul, BorderLayout.CENTER);
        headerRow.add(segarkanWrap, BorderLayout.EAST);

        // Baris bawah: filter rentang tanggal memanjang penuh dengan ModernDatePicker
        JPanel panelFilter = new JPanel(new GridBagLayout());
        panelFilter.setOpaque(false);

        dpTanggalAwal.setMinimumSize(new Dimension(130, UITheme.TOOLBAR_BUTTON_HEIGHT));
        dpTanggalAwal.setPreferredSize(new Dimension(150, UITheme.TOOLBAR_BUTTON_HEIGHT));
        dpTanggalAkhir.setMinimumSize(new Dimension(130, UITheme.TOOLBAR_BUTTON_HEIGHT));
        dpTanggalAkhir.setPreferredSize(new Dimension(150, UITheme.TOOLBAR_BUTTON_HEIGHT));
        konfigurasiTombol(btnTampilkan, 148, UITheme.TOOLBAR_BUTTON_HEIGHT, Font.BOLD);
        btnTampilkan.addActionListener(e -> muatLaporan(true));

        GridBagConstraints fg = new GridBagConstraints();
        fg.gridy = 0;
        fg.anchor = GridBagConstraints.WEST;

        fg.gridx = 0;
        fg.weightx = 0.0;
        fg.fill = GridBagConstraints.NONE;
        fg.insets = new Insets(0, 0, 0, 8);
        panelFilter.add(UITheme.createFieldLabel("Tanggal Awal"), fg);

        fg.gridx = 1;
        fg.weightx = 1.0;
        fg.fill = GridBagConstraints.HORIZONTAL;
        fg.insets = new Insets(0, 0, 0, 14);
        panelFilter.add(dpTanggalAwal, fg);

        fg.gridx = 2;
        fg.weightx = 0.0;
        fg.fill = GridBagConstraints.NONE;
        fg.insets = new Insets(0, 0, 0, 8);
        panelFilter.add(UITheme.createFieldLabel("Tanggal Akhir"), fg);

        fg.gridx = 3;
        fg.weightx = 1.0;
        fg.fill = GridBagConstraints.HORIZONTAL;
        fg.insets = new Insets(0, 0, 0, 12);
        panelFilter.add(dpTanggalAkhir, fg);

        fg.gridx = 4;
        fg.weightx = 0.0;
        fg.fill = GridBagConstraints.NONE;
        fg.insets = new Insets(0, 0, 0, 0);
        panelFilter.add(btnTampilkan, fg);

        card.add(headerRow, BorderLayout.NORTH);
        card.add(panelFilter, BorderLayout.SOUTH);
        return card;
    }

    private JPanel bangunPanelKonten() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        JPanel panelRingkasan = new JPanel(new GridLayout(1, 3, 14, 0));
        panelRingkasan.setOpaque(false);
        panelRingkasan.add(buatKartuRingkasan(
                "Jumlah Transaksi", lblJumlahTransaksi,
                "Transaksi pada periode terpilih", UITheme.PRIMARY_BLUE
        ));
        panelRingkasan.add(buatKartuRingkasan(
                "Total Omzet", lblTotalOmzet,
                "Akumulasi grand total transaksi", UITheme.SUCCESS
        ));
        panelRingkasan.add(buatKartuRingkasan(
                "Total Pajak", lblTotalPajak,
                "Akumulasi pajak terkumpul", UITheme.BADGE_AMBER_FG
        ));

        JPanel panelTabel = new RoundedCardPanel(new BorderLayout(0, 10));
        panelTabel.setBorder(BorderFactory.createEmptyBorder(15, 16, 14, 16));

        JLabel judulTabel = new JLabel("Rincian Penjualan Harian");
        judulTabel.setFont(UITheme.font(Font.BOLD, 14f));
        judulTabel.setForeground(UITheme.TEXT_MAIN);

        aturTampilanTabel();

        JScrollPane scrollPane = new JScrollPane(tabelLaporan);
        scrollPane.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, 10, new Insets(1, 1, 1, 1)));
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);

        panelTabel.add(judulTabel, BorderLayout.NORTH);
        panelTabel.add(scrollPane, BorderLayout.CENTER);

        panel.add(panelRingkasan, BorderLayout.NORTH);
        panel.add(panelTabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel bangunPanelBawah() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        lblStatus.setFont(UITheme.font(Font.PLAIN, 12f));
        lblStatus.setForeground(UITheme.TEXT_MUTED);

        JPanel panelTombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelTombol.setOpaque(false);

        konfigurasiTombol(btnCetak, 166, 36, Font.BOLD);
        btnCetak.addActionListener(e -> cetakLaporan());
        panelTombol.add(btnCetak);

        panel.add(lblStatus, BorderLayout.WEST);
        panel.add(panelTombol, BorderLayout.EAST);
        return panel;
    }

    private JPanel buatKartuRingkasan(String judul, JLabel nilai, String keterangan, Color warnaAksen) {
        JPanel kartu = new RoundedCardPanel(new BorderLayout(0, 6));
        kartu.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel lblJudul = UITheme.createFieldLabel(judul);
        lblJudul.setForeground(UITheme.TEXT_MUTED);

        nilai.setFont(UITheme.font(Font.BOLD, 22f));
        nilai.setForeground(warnaAksen);

        JLabel lblKeterangan = new JLabel(keterangan);
        lblKeterangan.setFont(UITheme.font(Font.PLAIN, 11f));
        lblKeterangan.setForeground(UITheme.TEXT_MUTED);

        kartu.add(lblJudul, BorderLayout.NORTH);
        kartu.add(nilai, BorderLayout.CENTER);
        kartu.add(lblKeterangan, BorderLayout.SOUTH);
        return kartu;
    }

    private void aturTampilanTabel() {
        tabelLaporan.setFont(UITheme.font(Font.PLAIN, 12f));
        tabelLaporan.setForeground(UITheme.TEXT_MAIN);
        tabelLaporan.setRowHeight(36);
        tabelLaporan.setShowVerticalLines(false);
        tabelLaporan.setGridColor(UITheme.BG_HOVER);
        tabelLaporan.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelLaporan.setSelectionBackground(UITheme.PRIMARY_SOFT_BG);
        tabelLaporan.setSelectionForeground(UITheme.TEXT_MAIN);
        tabelLaporan.setFillsViewportHeight(true);

        tabelLaporan.getTableHeader().setFont(UITheme.font(Font.BOLD, 11f));
        tabelLaporan.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        tabelLaporan.getTableHeader().setBackground(UITheme.BG_PAGE);
        tabelLaporan.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tabelLaporan.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_DEFAULT));
        tabelLaporan.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer tanggalRenderer = paddedCellRenderer(SwingConstants.LEFT);
        DefaultTableCellRenderer qtyRenderer = paddedCellRenderer(SwingConstants.CENTER);
        DefaultTableCellRenderer rupiahRenderer = paddedCellRenderer(SwingConstants.RIGHT);

        tabelLaporan.getColumnModel().getColumn(0).setCellRenderer(tanggalRenderer);
        tabelLaporan.getColumnModel().getColumn(1).setCellRenderer(qtyRenderer);
        tabelLaporan.getColumnModel().getColumn(2).setCellRenderer(rupiahRenderer);
        tabelLaporan.getColumnModel().getColumn(3).setCellRenderer(rupiahRenderer);

        // Header alignment mengikuti alignment isi kolom agar rapi
        tabelLaporan.getColumnModel().getColumn(0).setHeaderRenderer(headerRenderer(SwingConstants.LEFT));
        tabelLaporan.getColumnModel().getColumn(1).setHeaderRenderer(headerRenderer(SwingConstants.CENTER));
        tabelLaporan.getColumnModel().getColumn(2).setHeaderRenderer(headerRenderer(SwingConstants.RIGHT));
        tabelLaporan.getColumnModel().getColumn(3).setHeaderRenderer(headerRenderer(SwingConstants.RIGHT));
    }

    private DefaultTableCellRenderer paddedCellRenderer(int alignment) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(alignment);
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));

                int modelRow = table.convertRowIndexToModel(row);
                Object txVal = table.getModel().getValueAt(modelRow, 1);
                boolean hasTx = txVal instanceof Number n && n.intValue() > 0;
                setFont(UITheme.font(hasTx ? Font.BOLD : Font.PLAIN, 12f));
                if (!isSelected) {
                    setForeground(hasTx ? UITheme.TEXT_MAIN : UITheme.TEXT_MUTED);
                }
                return this;
            }
        };
    }

    private DefaultTableCellRenderer headerRenderer(int alignment) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(alignment);
                setFont(UITheme.font(Font.BOLD, 11f));
                setForeground(UITheme.TEXT_MUTED);
                setBackground(UITheme.BG_PAGE);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_DEFAULT),
                        BorderFactory.createEmptyBorder(0, 14, 0, 14)
                ));
                return this;
            }
        };
    }

    private void muatLaporan(boolean showToast) {
        LocalDate tanggalAwal = dpTanggalAwal.getDate();
        LocalDate tanggalAkhir = dpTanggalAkhir.getDate();

        if (tanggalAwal.isAfter(tanggalAkhir)) {
            ModernModal.error(
                    this,
                    "Periode Tidak Valid",
                    "Tanggal awal tidak boleh melebihi tanggal akhir."
            );
            return;
        }

        final int reqId = ++loadRequestId;
        lblStatus.setText("Memuat laporan penjualan...");
        btnTampilkan.setEnabled(false);
        btnSegarkan.setEnabled(false);

        SwingWorker<LaporanPenjualan, Void> worker = new SwingWorker<>() {
            @Override
            protected LaporanPenjualan doInBackground() throws Exception {
                return transaksiController.buatLaporan(tanggalAwal, tanggalAkhir);
            }

            @Override
            protected void done() {
                if (reqId != loadRequestId) {
                    return;
                }
                btnTampilkan.setEnabled(true);
                btnSegarkan.setEnabled(true);
                try {
                    laporanAktif = get();
                    perbaruiRingkasan();
                    perbaruiTabel();

                    lblStatus.setText(
                            "Periode: " + tanggalAwal.format(FORMAT_TANGGAL)
                                    + " s/d " + tanggalAkhir.format(FORMAT_TANGGAL)
                                    + " \u2022 " + laporanAktif.hitungJumlahTransaksi() + " transaksi ditemukan."
                    );

                    if (showToast) {
                        toastManager.show(
                                "Laporan Diperbarui",
                                laporanAktif.hitungJumlahTransaksi() + " transaksi berhasil dimuat.",
                                ToastType.INFO
                        );
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    lblStatus.setText("Gagal memuat laporan.");
                    if (cause instanceof SQLException sqlEx) {
                        ModernModal.error(LaporanPenjualanView.this, "Kesalahan Database",
                                TransaksiController.pesanSqlError(sqlEx));
                    } else if (cause instanceof IllegalArgumentException argEx) {
                        ModernModal.error(LaporanPenjualanView.this, "Data Tidak Valid", argEx.getMessage());
                    } else {
                        ModernModal.error(LaporanPenjualanView.this, "Kesalahan Sistem",
                                "Gagal mengambil laporan penjualan:\n" + (cause != null ? cause.getMessage() : ex.getMessage()));
                    }
                }
            }
        };
        worker.execute();
    }

    private void perbaruiRingkasan() {
        lblJumlahTransaksi.setText(String.valueOf(laporanAktif.hitungJumlahTransaksi()));
        lblTotalOmzet.setText(UITheme.formatRupiah(laporanAktif.hitungTotalOmzet()));
        lblTotalPajak.setText(UITheme.formatRupiah(laporanAktif.hitungTotalPajakTerkumpul()));
    }

    private void perbaruiTabel() {
        modelTabel.setRowCount(0);

        Map<LocalDate, List<Transaksi>> daftarHarian = laporanAktif.getDaftarHarian();
        for (Map.Entry<LocalDate, List<Transaksi>> entry : daftarHarian.entrySet()) {
            List<Transaksi> transaksiHarian = entry.getValue();
            double omzet = laporanAktif.hitungOmzetHarian(transaksiHarian);
            double pajak = laporanAktif.hitungPajakHarian(transaksiHarian);

            modelTabel.addRow(new Object[]{
                    entry.getKey().format(FORMAT_TANGGAL),
                    transaksiHarian.size(),
                    UITheme.formatRupiah(omzet),
                    UITheme.formatRupiah(pajak)
            });
        }
    }

    private void cetakLaporan() {
        LaporanPrinter.showPrintPreviewAndPrint(this, laporanAktif, toastManager);
    }

    private static void konfigurasiTombol(RoundedButton btn, int width, int height, int fontStyle) {
        btn.setFont(UITheme.font(fontStyle, 12f));
        Dimension size = new Dimension(width, height);
        btn.setPreferredSize(size);
        btn.setMinimumSize(size);
        btn.setMaximumSize(size);
    }
}
