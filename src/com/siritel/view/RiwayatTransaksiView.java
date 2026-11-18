package com.siritel.view;

import com.siritel.controller.TransaksiController;
import com.siritel.model.ItemTransaksi;
import com.siritel.model.Pengguna;
import com.siritel.model.Transaksi;
import com.siritel.model.TransaksiRingkas;
import com.siritel.view.ui.ButtonVariant;
import com.siritel.view.ui.ModernDatePicker;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Halaman riwayat transaksi SI-RITEL.
 * Menggunakan komponen UI terpusat dari {@code com.siritel.view.ui} serta
 * mendukung penyegaran otomatis dan manual dari database.
 */
public class RiwayatTransaksiView extends JPanel implements RefreshableView {

    private static final int BATAS_PER_HALAMAN = 10;
    private static final Locale ID_LOCALE = Locale.forLanguageTag("id-ID");

    private static final DateTimeFormatter FORMAT_TANGGAL_WAKTU =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", ID_LOCALE);

    private final TransaksiController transaksiController = new TransaksiController();
    private final ToastManager toastManager = new ToastManager(this);

    private final ModernDatePicker dpTanggalAwal = new ModernDatePicker(LocalDate.now().withDayOfMonth(1), false);
    private final ModernDatePicker dpTanggalAkhir = new ModernDatePicker(LocalDate.now(), false);
    private final JTextField txtCari = UITheme.createPlaceholderField("Cari ID transaksi atau nama kasir...");

    private final RoundedButton btnTampilkan = new RoundedButton("Tampilkan", ButtonVariant.PRIMARY, UITheme.BUTTON_RADIUS);
    private final RoundedButton btnReset = new RoundedButton("Reset", ButtonVariant.OUTLINE, UITheme.BUTTON_RADIUS);
    private final RoundedButton btnSegarkan = new RoundedButton("Segarkan", ButtonVariant.OUTLINE, UITheme.BUTTON_RADIUS);

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
    private final RoundedButton btnSebelumnya = new RoundedButton("\u2039 Sebelumnya", ButtonVariant.OUTLINE, UITheme.SMALL_RADIUS);
    private final RoundedButton btnSelanjutnya = new RoundedButton("Selanjutnya \u203A", ButtonVariant.OUTLINE, UITheme.SMALL_RADIUS);

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
    private final RoundedButton btnCetak = new RoundedButton("Cetak Struk", ButtonVariant.PRIMARY, UITheme.BUTTON_RADIUS);

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
     * Konstruktor yang menerima pengguna aktif dari MainFrame.
     */
    public RiwayatTransaksiView(Pengguna penggunaAktif) {
        setLayout(new BorderLayout(14, 0));
        setBackground(UITheme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        aturTabel();
        bangunTampilan();
        pasangListener();

        muatRiwayat(false);
    }

    @Override
    public void refreshData() {
        muatRiwayat(false);
    }

    private void bangunTampilan() {
        add(bangunPanelDaftar(), BorderLayout.CENTER);
        add(bangunPanelDetail(), BorderLayout.EAST);
    }

    private JPanel bangunPanelDaftar() {
        JPanel card = new RoundedCardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(15, 16, 14, 16));

        // Toolbar atas (Judul + Segarkan, Filter Tanggal, Pencarian)
        JPanel panelAtas = new JPanel(new GridBagLayout());
        panelAtas.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Baris 1: Judul & tombol Segarkan
        JPanel headerRow = new JPanel(new BorderLayout(8, 0));
        headerRow.setOpaque(false);

        JPanel panelJudul = new JPanel(new GridLayout(2, 1, 0, 2));
        panelJudul.setOpaque(false);
        JLabel judul = new JLabel("Riwayat Transaksi");
        judul.setFont(UITheme.font(Font.BOLD, 16f));
        judul.setForeground(UITheme.TEXT_MAIN);
        JLabel deskripsi = new JLabel("Log penjualan, detail struk, dan pencarian transaksi");
        deskripsi.setFont(UITheme.font(Font.PLAIN, 12f));
        deskripsi.setForeground(UITheme.TEXT_MUTED);
        panelJudul.add(judul);
        panelJudul.add(deskripsi);

        konfigurasiTombol(btnSegarkan, 90, UITheme.TOOLBAR_BUTTON_HEIGHT, Font.PLAIN);
        JPanel segarkanWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        segarkanWrap.setOpaque(false);
        segarkanWrap.add(btnSegarkan);

        headerRow.add(panelJudul, BorderLayout.CENTER);
        headerRow.add(segarkanWrap, BorderLayout.EAST);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        panelAtas.add(headerRow, gbc);

        // Baris 2: Filter rentang tanggal memanjang penuh dengan ModernDatePicker
        JPanel barisTanggal = new JPanel(new GridBagLayout());
        barisTanggal.setOpaque(false);

        JLabel lblDari = UITheme.createFieldLabel("Dari");
        dpTanggalAwal.setMinimumSize(new Dimension(112, UITheme.TOOLBAR_BUTTON_HEIGHT));
        dpTanggalAwal.setPreferredSize(new Dimension(130, UITheme.TOOLBAR_BUTTON_HEIGHT));
        JLabel lblSampai = UITheme.createFieldLabel("Sampai");
        dpTanggalAkhir.setMinimumSize(new Dimension(112, UITheme.TOOLBAR_BUTTON_HEIGHT));
        dpTanggalAkhir.setPreferredSize(new Dimension(130, UITheme.TOOLBAR_BUTTON_HEIGHT));

        konfigurasiTombol(btnTampilkan, 92, UITheme.TOOLBAR_BUTTON_HEIGHT, Font.BOLD);
        konfigurasiTombol(btnReset, 72, UITheme.TOOLBAR_BUTTON_HEIGHT, Font.PLAIN);

        GridBagConstraints fg = new GridBagConstraints();
        fg.gridy = 0;
        fg.anchor = GridBagConstraints.WEST;

        fg.gridx = 0;
        fg.weightx = 0.0;
        fg.fill = GridBagConstraints.NONE;
        fg.insets = new Insets(0, 0, 0, 8);
        barisTanggal.add(lblDari, fg);

        fg.gridx = 1;
        fg.weightx = 1.0;
        fg.fill = GridBagConstraints.HORIZONTAL;
        fg.insets = new Insets(0, 0, 0, 12);
        barisTanggal.add(dpTanggalAwal, fg);

        fg.gridx = 2;
        fg.weightx = 0.0;
        fg.fill = GridBagConstraints.NONE;
        fg.insets = new Insets(0, 0, 0, 8);
        barisTanggal.add(lblSampai, fg);

        fg.gridx = 3;
        fg.weightx = 1.0;
        fg.fill = GridBagConstraints.HORIZONTAL;
        fg.insets = new Insets(0, 0, 0, 10);
        barisTanggal.add(dpTanggalAkhir, fg);

        fg.gridx = 4;
        fg.weightx = 0.0;
        fg.fill = GridBagConstraints.NONE;
        fg.insets = new Insets(0, 0, 0, 8);
        barisTanggal.add(btnTampilkan, fg);

        fg.gridx = 5;
        fg.weightx = 0.0;
        fg.fill = GridBagConstraints.NONE;
        fg.insets = new Insets(0, 0, 0, 0);
        barisTanggal.add(btnReset, fg);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 8, 0);
        panelAtas.add(barisTanggal, gbc);

        // Baris 3: Kolom pencarian + jumlah transaksi
        JPanel barisCari = new JPanel(new BorderLayout(10, 0));
        barisCari.setOpaque(false);
        UITheme.styleInputField(txtCari);
        txtCari.setPreferredSize(new Dimension(0, UITheme.TOOLBAR_BUTTON_HEIGHT));
        txtCari.setToolTipText("Cari ID transaksi atau nama kasir");
        lblJumlahTransaksi.setFont(UITheme.font(Font.PLAIN, 12f));
        lblJumlahTransaksi.setForeground(UITheme.TEXT_MUTED);
        barisCari.add(txtCari, BorderLayout.CENTER);
        barisCari.add(lblJumlahTransaksi, BorderLayout.EAST);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        panelAtas.add(barisCari, gbc);

        // Tabel Riwayat
        JScrollPane scrollTabel = new JScrollPane(tabelTransaksi);
        scrollTabel.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, 10, new Insets(1, 1, 1, 1)));
        scrollTabel.getViewport().setBackground(UITheme.CARD_BG);

        // Footer Pagination
        JPanel panelPagination = new JPanel(new BorderLayout());
        panelPagination.setOpaque(false);

        lblInfoHalaman.setFont(UITheme.font(Font.PLAIN, 12f));
        lblInfoHalaman.setForeground(UITheme.TEXT_MUTED);

        JPanel navBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        navBtns.setOpaque(false);
        konfigurasiTombol(btnSebelumnya, 104, 32, Font.PLAIN);
        konfigurasiTombol(btnSelanjutnya, 104, 32, Font.PLAIN);
        navBtns.add(btnSebelumnya);
        navBtns.add(btnSelanjutnya);

        panelPagination.add(lblInfoHalaman, BorderLayout.WEST);
        panelPagination.add(navBtns, BorderLayout.EAST);

        card.add(panelAtas, BorderLayout.NORTH);
        card.add(scrollTabel, BorderLayout.CENTER);
        card.add(panelPagination, BorderLayout.SOUTH);
        return card;
    }

    private JPanel bangunPanelDetail() {
        JPanel card = new RoundedCardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(15, 16, 14, 16));
        card.setPreferredSize(new Dimension(382, 0));
        card.setMinimumSize(new Dimension(350, 0));

        // Header panel kanan
        JPanel panelHeader = new JPanel(new BorderLayout(8, 0));
        panelHeader.setOpaque(false);

        JLabel judul = new JLabel("Detail Transaksi");
        judul.setFont(UITheme.font(Font.BOLD, 16f));
        judul.setForeground(UITheme.TEXT_MAIN);

        lblStatus.setFont(UITheme.font(Font.BOLD, 11f));
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(UITheme.BG_HOVER);
        lblStatus.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, UITheme.SMALL_RADIUS, new Insets(3, 9, 3, 9)));

        panelHeader.add(judul, BorderLayout.WEST);
        panelHeader.add(lblStatus, BorderLayout.EAST);

        // Box identitas transaksi
        JPanel panelIdentitas = new JPanel(new GridLayout(3, 2, 8, 8));
        panelIdentitas.setBackground(UITheme.BG_PAGE);
        panelIdentitas.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, 10, new Insets(10, 12, 10, 12)));

        panelIdentitas.add(labelMuted("ID Transaksi"));
        panelIdentitas.add(lblIdTransaksi);
        panelIdentitas.add(labelMuted("Tanggal"));
        panelIdentitas.add(lblTanggal);
        panelIdentitas.add(labelMuted("Kasir"));
        panelIdentitas.add(lblKasir);

        aturLabelNilai(lblIdTransaksi);
        aturLabelNilai(lblTanggal);
        aturLabelNilai(lblKasir);

        // Tabel item detail
        JScrollPane scrollDetail = new JScrollPane(tabelDetail);
        scrollDetail.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, 10, new Insets(1, 1, 1, 1)));
        scrollDetail.getViewport().setBackground(UITheme.CARD_BG);

        JPanel panelKontenAtas = new JPanel(new BorderLayout(0, 10));
        panelKontenAtas.setOpaque(false);
        panelKontenAtas.add(panelIdentitas, BorderLayout.NORTH);
        panelKontenAtas.add(scrollDetail, BorderLayout.CENTER);

        // Ringkasan nominal + tombol Cetak Struk
        JPanel panelRingkasan = new JPanel();
        panelRingkasan.setOpaque(false);
        panelRingkasan.setLayout(new BoxLayout(panelRingkasan, BoxLayout.Y_AXIS));
        panelRingkasan.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_DEFAULT));
        panelRingkasan.add(Box.createVerticalStrut(10));
        panelRingkasan.add(barisNilai("Subtotal", lblSubtotal, false));
        panelRingkasan.add(Box.createVerticalStrut(6));
        panelRingkasan.add(barisNilai("Total Pajak", lblPajak, false));
        panelRingkasan.add(Box.createVerticalStrut(6));
        panelRingkasan.add(barisNilai("Grand Total", lblGrandTotal, true));
        panelRingkasan.add(Box.createVerticalStrut(6));
        panelRingkasan.add(barisNilai("Bayar", lblBayar, false));
        panelRingkasan.add(Box.createVerticalStrut(6));
        panelRingkasan.add(barisNilai("Kembalian", lblKembalian, false));

        btnCetak.setFont(UITheme.font(Font.BOLD, 13f));
        btnCetak.setPreferredSize(new Dimension(0, 40));
        btnCetak.setEnabled(false);

        JPanel panelBawah = new JPanel(new BorderLayout(0, 10));
        panelBawah.setOpaque(false);
        panelBawah.add(panelRingkasan, BorderLayout.CENTER);
        panelBawah.add(btnCetak, BorderLayout.SOUTH);

        card.add(panelHeader, BorderLayout.NORTH);
        card.add(panelKontenAtas, BorderLayout.CENTER);
        card.add(panelBawah, BorderLayout.SOUTH);
        return card;
    }

    private void aturTabel() {
        tabelTransaksi.setRowHeight(38);
        tabelTransaksi.setFont(UITheme.font(Font.PLAIN, 12f));
        tabelTransaksi.setForeground(UITheme.TEXT_MAIN);
        tabelTransaksi.setSelectionBackground(UITheme.PRIMARY_SOFT_BG);
        tabelTransaksi.setSelectionForeground(UITheme.TEXT_MAIN);
        tabelTransaksi.setGridColor(UITheme.BG_HOVER);
        tabelTransaksi.setShowVerticalLines(false);
        tabelTransaksi.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelTransaksi.setFillsViewportHeight(true);

        tabelTransaksi.getTableHeader().setFont(UITheme.font(Font.BOLD, 11f));
        tabelTransaksi.getTableHeader().setBackground(UITheme.BG_PAGE);
        tabelTransaksi.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        tabelTransaksi.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tabelTransaksi.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_DEFAULT));
        tabelTransaksi.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer leftPad = paddedRenderer(SwingConstants.LEFT, false);
        DefaultTableCellRenderer idBold = paddedRenderer(SwingConstants.LEFT, true);
        DefaultTableCellRenderer centerPad = paddedRenderer(SwingConstants.CENTER, false);
        DefaultTableCellRenderer rightBold = paddedRenderer(SwingConstants.RIGHT, true);

        tabelTransaksi.getColumnModel().getColumn(0).setCellRenderer(idBold);
        tabelTransaksi.getColumnModel().getColumn(1).setCellRenderer(leftPad);
        tabelTransaksi.getColumnModel().getColumn(2).setCellRenderer(leftPad);
        tabelTransaksi.getColumnModel().getColumn(3).setCellRenderer(centerPad);
        tabelTransaksi.getColumnModel().getColumn(4).setCellRenderer(rightBold);

        // Tabel detail kanan
        tabelDetail.setRowHeight(30);
        tabelDetail.setFont(UITheme.font(Font.PLAIN, 12f));
        tabelDetail.setForeground(UITheme.TEXT_MAIN);
        tabelDetail.setGridColor(UITheme.BG_HOVER);
        tabelDetail.setShowVerticalLines(false);
        tabelDetail.setFillsViewportHeight(true);
        tabelDetail.getTableHeader().setFont(UITheme.font(Font.BOLD, 11f));
        tabelDetail.getTableHeader().setBackground(UITheme.BG_PAGE);
        tabelDetail.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        tabelDetail.getTableHeader().setPreferredSize(new Dimension(0, 32));
        tabelDetail.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_DEFAULT));
        tabelDetail.getTableHeader().setReorderingAllowed(false);

        tabelDetail.getColumnModel().getColumn(0).setCellRenderer(leftPad);
        tabelDetail.getColumnModel().getColumn(1).setCellRenderer(centerPad);
        tabelDetail.getColumnModel().getColumn(2).setCellRenderer(paddedRenderer(SwingConstants.RIGHT, false));
        tabelDetail.getColumnModel().getColumn(3).setCellRenderer(rightBold);
        tabelDetail.getColumnModel().getColumn(1).setPreferredWidth(44);
        tabelDetail.getColumnModel().getColumn(1).setMaxWidth(52);
    }

    private DefaultTableCellRenderer paddedRenderer(int alignment, boolean bold) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(alignment);
                setFont(UITheme.font(bold ? Font.BOLD : Font.PLAIN, 12f));
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!isSelected) {
                    setForeground(UITheme.TEXT_MAIN);
                }
                return this;
            }
        };
    }

    private void pasangListener() {
        btnTampilkan.addActionListener(e -> terapkanFilterTanggal());
        btnReset.addActionListener(e -> resetFilter());
        btnSegarkan.addActionListener(e -> muatRiwayat(true));
        btnCetak.addActionListener(e -> ReceiptSupport.printStruk(this, transaksiTerpilih, toastManager));

        tabelTransaksi.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                tampilkanDetailTerpilih();
            }
        });

        txtCari.addActionListener(e -> tampilkanTabelHalaman());
        txtCari.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                tampilkanTabelHalaman();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                tampilkanTabelHalaman();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                tampilkanTabelHalaman();
            }
        });

        btnSebelumnya.addActionListener(e -> {
            if (halamanAktif > 1) {
                halamanAktif--;
                muatRiwayat(false);
            }
        });

        btnSelanjutnya.addActionListener(e -> {
            int totalHalaman = hitungJumlahHalaman();
            if (halamanAktif < totalHalaman) {
                halamanAktif++;
                muatRiwayat(false);
            }
        });
    }

    private void terapkanFilterTanggal() {
        LocalDate awal = dpTanggalAwal.getDate();
        LocalDate akhir = dpTanggalAkhir.getDate();

        if (awal.isAfter(akhir)) {
            ModernModal.error(
                    this,
                    "Filter Tanggal Tidak Valid",
                    "Tanggal awal tidak boleh melebihi tanggal akhir."
            );
            return;
        }

        tanggalAwalAktif = awal;
        tanggalAkhirAktif = akhir;
        halamanAktif = 1;
        txtCari.setText("");
        muatRiwayat(false);
    }

    private void resetFilter() {
        tanggalAwalAktif = LocalDate.now().withDayOfMonth(1);
        tanggalAkhirAktif = LocalDate.now();
        dpTanggalAwal.setDate(tanggalAwalAktif);
        dpTanggalAkhir.setDate(tanggalAkhirAktif);
        txtCari.setText("");
        halamanAktif = 1;
        muatRiwayat(false);
        toastManager.show("Filter Direset", "Menampilkan transaksi bulan berjalan.", ToastType.INFO);
    }

    private void muatRiwayat(boolean showToast) {
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
            } else if (tabelTransaksi.getSelectedRow() < 0 && modelTabel.getRowCount() > 0) {
                tabelTransaksi.setRowSelectionInterval(0, 0);
            }

            if (showToast) {
                toastManager.show(
                        "Riwayat Disegarkan",
                        jumlahSeluruhTransaksi + " transaksi ditemukan pada periode aktif.",
                        ToastType.INFO
                );
            }
        } catch (Exception ex) {
            ModernModal.error(this, "Kesalahan Database", "Gagal memuat riwayat transaksi:\n" + ex.getMessage());
        }
    }

    private void tampilkanTabelHalaman() {
        modelTabel.setRowCount(0);

        String kataKunci = txtCari.getText() == null
                ? ""
                : txtCari.getText().trim().toLowerCase(Locale.ROOT);

        for (TransaksiRingkas transaksi : daftarTransaksi) {
            boolean cocok = transaksi.getIdTransaksi().toLowerCase(Locale.ROOT).contains(kataKunci)
                    || transaksi.getNamaKasir().toLowerCase(Locale.ROOT).contains(kataKunci);

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
            ModernModal.error(this, "Kesalahan Database", "Gagal mengambil detail transaksi:\n" + ex.getMessage());
        }
    }

    private void isiPanelDetail(Transaksi transaksi) {
        lblIdTransaksi.setText(transaksi.getIdTransaksi());
        lblTanggal.setText(transaksi.getTanggal().format(FORMAT_TANGGAL_WAKTU));
        lblKasir.setText(transaksi.getKasir().getNamaLengkap());

        lblStatus.setText("LUNAS");
        lblStatus.setForeground(UITheme.SUCCESS);
        lblStatus.setBackground(UITheme.SUCCESS_SOFT_BG);
        lblStatus.setBorder(new RoundedBorder(UITheme.SUCCESS_BORDER, UITheme.SMALL_RADIUS, new Insets(3, 9, 3, 9)));

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
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        lblStatus.setBackground(UITheme.BG_HOVER);
        lblStatus.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, UITheme.SMALL_RADIUS, new Insets(3, 9, 3, 9)));

        modelDetail.setRowCount(0);

        lblSubtotal.setText(formatRupiah(0));
        lblPajak.setText(formatRupiah(0));
        lblGrandTotal.setText(formatRupiah(0));
        lblBayar.setText(formatRupiah(0));
        lblKembalian.setText(formatRupiah(0));

        btnCetak.setEnabled(false);
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
        return (int) Math.ceil((double) jumlahSeluruhTransaksi / BATAS_PER_HALAMAN);
    }

    private static JLabel labelMuted(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(UITheme.font(Font.PLAIN, 11f));
        label.setForeground(UITheme.TEXT_MUTED);
        return label;
    }

    private static void aturLabelNilai(JLabel label) {
        label.setFont(UITheme.font(Font.BOLD, 12f));
        label.setForeground(UITheme.TEXT_MAIN);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    private static JPanel barisNilai(String teks, JLabel nilai, boolean utama) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel label = new JLabel(teks);
        label.setFont(UITheme.font(utama ? Font.BOLD : Font.PLAIN, 12f));
        label.setForeground(utama ? UITheme.TEXT_MAIN : UITheme.TEXT_MUTED);

        nilai.setFont(UITheme.font(Font.BOLD, utama ? 17f : 12f));
        nilai.setForeground(UITheme.TEXT_MAIN);
        nilai.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(label, BorderLayout.WEST);
        panel.add(nilai, BorderLayout.EAST);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, utama ? 26 : 20));
        return panel;
    }

    private static void konfigurasiTombol(RoundedButton btn, int width, int height, int fontStyle) {
        btn.setFont(UITheme.font(fontStyle, 12f));
        Dimension size = new Dimension(width, height);
        btn.setPreferredSize(size);
        btn.setMinimumSize(size);
        btn.setMaximumSize(size);
    }

    private static String formatRupiah(double nominal) {
        NumberFormat format = NumberFormat.getCurrencyInstance(ID_LOCALE);
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(nominal).replace('\u00A0', ' ');
    }
}
