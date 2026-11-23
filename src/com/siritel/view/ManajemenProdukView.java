package com.siritel.view;

import com.siritel.controller.ProdukController;
import com.siritel.model.BatchProduk;
import com.siritel.model.Elektronik;
import com.siritel.model.Makanan;
import com.siritel.model.Minuman;
import com.siritel.model.Produk;
import com.siritel.view.ui.ButtonVariant;
import com.siritel.view.ui.ModernDatePicker;
import com.siritel.view.ui.ModernModal;
import com.siritel.view.ui.RefreshableView;
import com.siritel.view.ui.RoundedBorder;
import com.siritel.view.ui.RoundedButton;
import com.siritel.view.ui.RoundedCardPanel;
import com.siritel.view.ui.RoundedComboBoxUI;
import com.siritel.view.ui.ToastManager;
import com.siritel.view.ui.ToastType;
import com.siritel.view.ui.UITheme;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Halaman Manajemen Produk & Stok (Modul 2).
 * Menampilkan katalog barang dengan kolom jenis dan nominal pajak polimorfik (hitungPajak()),
 * pencarian dengan method overloading cariProduk(), form dinamis kategori, serta manajemen batch FEFO.
 */
public class ManajemenProdukView extends JPanel implements RefreshableView {
    private static final Color BG_PANEL = UITheme.BG_PAGE;
    private static final Color CARD_BG = UITheme.CARD_BG;
    private static final Color BORDER_COLOR = UITheme.BORDER_DEFAULT;
    private static final Color INPUT_BORDER = UITheme.BORDER_INPUT;
    private static final Color PRIMARY_BLUE = UITheme.PRIMARY_BLUE;
    private static final Color PRIMARY_BLUE_SOFT = UITheme.PRIMARY_SOFT_BG;
    private static final Color DANGER_RED = UITheme.DANGER;
    private static final Color TEXT_DARK = UITheme.TEXT_MAIN;
    private static final Color TEXT_SLATE = UITheme.TEXT_LABEL;
    private static final Color TEXT_MUTED = UITheme.TEXT_MUTED;

    private static final Color BADGE_MAKANAN_BG = UITheme.BADGE_AMBER_BG;
    private static final Color BADGE_MAKANAN_FG = UITheme.BADGE_AMBER_FG;
    private static final Color BADGE_MINUMAN_BG = UITheme.BADGE_BLUE_BG;
    private static final Color BADGE_MINUMAN_FG = UITheme.BADGE_BLUE_FG;
    private static final Color BADGE_ELEKTRONIK_BG = UITheme.BADGE_PURPLE_BG;
    private static final Color BADGE_ELEKTRONIK_FG = UITheme.BADGE_PURPLE_FG;

    private static final DateTimeFormatter DATE_DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", UITheme.ID_LOCALE);

    private final ProdukController produkController = new ProdukController();
    private final ToastManager toastManager = new ToastManager(this);
    private List<Produk> daftarProdukSaatIni = new ArrayList<>();
    private List<BatchProduk> daftarBatchSaatIni = new ArrayList<>();
    private Produk produkTerpilih;
    private boolean isRefreshingTable = false;
    private boolean isUpdatingFilters = false;
    private boolean isPopulatingForm = false;
    private boolean isFormDirty = false;
    private int loadRequestId = 0;

    // Toolbar & Katalog Kiri
    private final JTextField txtCari = UITheme.createPlaceholderField("Cari kode atau nama produk...");
    private final JComboBox<String> cmbFilterJenis =
            new JComboBox<>(new String[]{"SEMUA", "MAKANAN", "MINUMAN", "ELEKTRONIK"});
    private final RoundedButton btnCari = new RoundedButton("Cari", ButtonVariant.OUTLINE, 10);
    private final RoundedButton btnTambahProduk = new RoundedButton("+ Tambah Produk", ButtonVariant.PRIMARY, 10);
    private final RoundedButton btnSegarkan = new RoundedButton("Segarkan", ButtonVariant.OUTLINE, 10);
    private final JLabel lblRingkasanTabel = new JLabel("Menampilkan 0 produk");

    private final DefaultTableModel produkTableModel;
    private final JTable tableProduk;

    // Panel Form Kanan (dinamis: tersembunyi sampai tombol Tambah Produk / baris tabel diklik)
    private final JPanel sideCard;
    private final JLabel lblFormTitle = new JLabel("Tambah Produk");
    private final JLabel lblFormSubtitle = new JLabel("Lengkapi data katalog produk baru");
    private final RoundedButton btnTutupPanel = new RoundedButton("\u00D7", ButtonVariant.GHOST, 8);
    private final JButton btnTabDetail = new JButton("Detail");
    private final JButton btnTabBatch = new JButton("Batch Stok");
    private final CardLayout rightTabLayout = new CardLayout();
    private final JPanel rightTabContent = new JPanel(rightTabLayout);

    // Tab Detail Produk
    private final JTextField txtKode = new JTextField();
    private final JTextField txtNama = new JTextField();
    private final JTextField txtHarga = new JTextField();
    private final JComboBox<String> cmbJenis =
            new JComboBox<>(new String[]{"MAKANAN", "MINUMAN", "ELEKTRONIK"});
    private final CardLayout dynamicFieldLayout = new CardLayout();
    private final JPanel dynamicFieldPanel = new JPanel(dynamicFieldLayout);

    // Komponen Spesifik Dinamis
    private final JSpinner spnBeratGram =
            new JSpinner(new SpinnerNumberModel(100, 1, 1000000, 50));
    private final JSpinner spnVolumeMl =
            new JSpinner(new SpinnerNumberModel(250, 1, 1000000, 50));
    private final JCheckBox chkDingin = new JCheckBox("Minuman Dingin");
    private final JSpinner spnBulanGaransi =
            new JSpinner(new SpinnerNumberModel(12, 0, 240, 1));

    private final JLabel lblEstimasiPajak = new JLabel("Estimasi Pajak: -");
    private final RoundedButton btnBatal = new RoundedButton("Batal", ButtonVariant.OUTLINE, 10);
    private final RoundedButton btnSimpan = new RoundedButton("Simpan", ButtonVariant.PRIMARY, 10);
    private final RoundedButton btnHapusForm = new RoundedButton("Hapus", ButtonVariant.DANGER, 10);

    // Tab Batch Stok
    private final JLabel lblBatchInfoProduk = new JLabel("Pilih produk untuk mengelola batch stok");
    private final JLabel lblInfoKadaluarsaElektronik = new JLabel("Elektronik tidak memiliki kedaluwarsa.");
    private final ModernDatePicker dateMasukPicker = new ModernDatePicker(LocalDate.now(), false);
    private final ModernDatePicker dateKadaluarsaPicker = new ModernDatePicker(LocalDate.now().plusDays(30), true);
    private final JSpinner spnStokBatch =
            new JSpinner(new SpinnerNumberModel(10, 1, 1000000, 1));
    private final RoundedButton btnTambahBatch = new RoundedButton("+ Tambah Batch", ButtonVariant.PRIMARY, 10);
    private final RoundedButton btnHapusBatch = new RoundedButton("Hapus Batch", ButtonVariant.DANGER_OUTLINE, 10);
    private final JLabel lblTotalStokBatch = new JLabel("Total Stok Aktif: 0 unit");
    private final DefaultTableModel batchTableModel;
    private final JTable tableBatch;

    public ManajemenProdukView() {
        setLayout(new BorderLayout(14, 0));
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 1. Inisialisasi Tabel Katalog Produk
        String[] productColumns = {"Kode", "Nama Produk", "Jenis", "Harga", "Pajak", "Stok", "Info Spesifik"};
        produkTableModel = new DefaultTableModel(productColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableProduk = new JTable(produkTableModel);
        konfigurasiTabelProduk();

        // 2. Inisialisasi Tabel Batch Stok
        String[] batchColumns = {"Tgl Masuk", "Kedaluwarsa", "Stok", "Status"};
        batchTableModel = new DefaultTableModel(batchColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableBatch = new JTable(batchTableModel);
        konfigurasiTabelBatch();

        // 3. Susun Panel Kiri (Katalog) & Panel Kanan (tersembunyi secara default agar tabel luas)
        add(buildCatalogCard(), BorderLayout.CENTER);
        sideCard = buildSideEditorCard();
        add(sideCard, BorderLayout.EAST);

        // 4. Event Listeners & State Awal (data dimuat lazy saat tab ditampilkan)
        initActions();
        tutupSidePanel();
    }

    private JPanel buildCatalogCard() {
        JPanel card = new RoundedCardPanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(15, 16, 14, 16));

        // Toolbar Atas (2 baris agar responsif di 960px maupun 1180px)
        JPanel toolbar = new JPanel(new GridBagLayout());
        toolbar.setOpaque(false);
        GridBagConstraints tbGbc = new GridBagConstraints();
        tbGbc.gridx = 0;
        tbGbc.weightx = 1.0;
        tbGbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel headerRow = new JPanel(new GridBagLayout());
        headerRow.setOpaque(false);
        GridBagConstraints hrGbc = new GridBagConstraints();
        hrGbc.gridy = 0;
        hrGbc.anchor = GridBagConstraints.WEST;

        JLabel lblCatalogTitle = new JLabel("Katalog Produk");
        lblCatalogTitle.setFont(UITheme.font(Font.BOLD, 13f));
        lblCatalogTitle.setForeground(TEXT_DARK);
        lblCatalogTitle.setMinimumSize(new Dimension(50, 20));
        hrGbc.gridx = 0;
        hrGbc.weightx = 1.0;
        hrGbc.fill = GridBagConstraints.HORIZONTAL;
        headerRow.add(lblCatalogTitle, hrGbc);

        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new javax.swing.BoxLayout(actionPanel, javax.swing.BoxLayout.X_AXIS));
        actionPanel.setOpaque(false);

        btnTambahProduk.setFont(UITheme.font(Font.BOLD, 12f));
        btnTambahProduk.setPreferredSize(new Dimension(130, 33));
        btnTambahProduk.setMinimumSize(new Dimension(130, 33));
        btnTambahProduk.setMaximumSize(new Dimension(130, 33));

        btnSegarkan.setFont(UITheme.font(Font.PLAIN, 12f));
        btnSegarkan.setPreferredSize(new Dimension(72, 33));
        btnSegarkan.setMinimumSize(new Dimension(72, 33));
        btnSegarkan.setMaximumSize(new Dimension(72, 33));

        actionPanel.add(btnTambahProduk);
        actionPanel.add(javax.swing.Box.createHorizontalStrut(6));
        actionPanel.add(btnSegarkan);
        hrGbc.gridx = 1;
        hrGbc.weightx = 0.0;
        hrGbc.fill = GridBagConstraints.NONE;
        hrGbc.anchor = GridBagConstraints.EAST;
        headerRow.add(actionPanel, hrGbc);

        tbGbc.gridy = 0;
        tbGbc.insets = new Insets(0, 0, 8, 0);
        toolbar.add(headerRow, tbGbc);

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtCari.setFont(UITheme.font(Font.PLAIN, 12f));
        txtCari.setMinimumSize(new Dimension(80, 34));
        txtCari.setPreferredSize(new Dimension(140, 34));
        txtCari.setBorder(new RoundedBorder(INPUT_BORDER, 10, new Insets(5, 11, 5, 11)));
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        filterPanel.add(txtCari, gbc);

        styleComboBox(cmbFilterJenis, true, 10);
        cmbFilterJenis.setMinimumSize(new Dimension(112, 34));
        cmbFilterJenis.setPreferredSize(new Dimension(132, 34));
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        filterPanel.add(cmbFilterJenis, gbc);

        btnCari.setFont(UITheme.font(Font.BOLD, 12f));
        btnCari.setMinimumSize(new Dimension(54, 34));
        btnCari.setPreferredSize(new Dimension(62, 34));
        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        filterPanel.add(btnCari, gbc);

        tbGbc.gridy = 1;
        tbGbc.insets = new Insets(0, 0, 0, 0);
        toolbar.add(filterPanel, tbGbc);
        card.add(toolbar, BorderLayout.NORTH);

        // Tabel Produk (responsif: stretch penuh di layar normal, scroll horizontal di layar sempit)
        JScrollPane scrollPane = new JScrollPane(tableProduk) {
            @Override
            public void doLayout() {
                super.doLayout();
                int vw = getViewport().getWidth();
                int targetMode = (vw > 0 && vw < 515)
                        ? JTable.AUTO_RESIZE_OFF
                        : JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS;
                if (tableProduk.getAutoResizeMode() != targetMode) {
                    tableProduk.setAutoResizeMode(targetMode);
                }
            }
        };
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBorder(new RoundedBorder(BORDER_COLOR, 10, new Insets(1, 1, 1, 1)));
        card.add(scrollPane, BorderLayout.CENTER);

        // Footer Katalog
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));

        lblRingkasanTabel.setFont(UITheme.font(Font.PLAIN, 12f));
        lblRingkasanTabel.setForeground(TEXT_MUTED);
        footer.add(lblRingkasanTabel, BorderLayout.WEST);

        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildSideEditorCard() {
        JPanel sidePanel = new RoundedCardPanel(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(320, 0));
        sidePanel.setMinimumSize(new Dimension(300, 0));

        // Header + Tab Bar
        JPanel headerWrapper = new JPanel(new BorderLayout(0, 8));
        headerWrapper.setOpaque(false);
        headerWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 16, 0, 12)
        ));

        JPanel topHeader = new JPanel(new BorderLayout(8, 0));
        topHeader.setOpaque(false);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 2));
        titleGroup.setOpaque(false);
        lblFormTitle.setFont(UITheme.font(Font.BOLD, 16f));
        lblFormTitle.setForeground(TEXT_DARK);
        lblFormSubtitle.setFont(UITheme.font(Font.PLAIN, 12f));
        lblFormSubtitle.setForeground(TEXT_MUTED);
        titleGroup.add(lblFormTitle);
        titleGroup.add(lblFormSubtitle);
        topHeader.add(titleGroup, BorderLayout.CENTER);

        btnTutupPanel.setFont(UITheme.font(Font.BOLD, 16f));
        btnTutupPanel.setPreferredSize(new Dimension(28, 28));
        btnTutupPanel.setToolTipText("Tutup panel");
        JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeWrap.setOpaque(false);
        closeWrap.add(btnTutupPanel);
        topHeader.add(closeWrap, BorderLayout.EAST);

        headerWrapper.add(topHeader, BorderLayout.NORTH);

        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        tabBar.setOpaque(false);
        konfigurasiTabButton(btnTabDetail);
        konfigurasiTabButton(btnTabBatch);
        tabBar.add(btnTabDetail);
        tabBar.add(btnTabBatch);
        headerWrapper.add(tabBar, BorderLayout.SOUTH);

        sidePanel.add(headerWrapper, BorderLayout.NORTH);

        // Konten Tab (CardLayout)
        rightTabContent.setOpaque(false);
        rightTabContent.add(buildDetailTabPanel(), "DETAIL");
        rightTabContent.add(buildBatchTabPanel(), "BATCH");
        sidePanel.add(rightTabContent, BorderLayout.CENTER);

        pilihTab("DETAIL");
        return sidePanel;
    }

    private JScrollPane buildDetailTabPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        form.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;

        // Kode Produk
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        form.add(UITheme.createFieldLabel("Kode Produk *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        UITheme.styleInputField(txtKode);
        form.add(txtKode, gbc);

        // Nama Produk
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        form.add(UITheme.createFieldLabel("Nama Produk *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        UITheme.styleInputField(txtNama);
        form.add(txtNama, gbc);

        // Harga Jual
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        form.add(UITheme.createFieldLabel("Harga Jual (Rp) *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        UITheme.styleInputField(txtHarga);
        form.add(txtHarga, gbc);

        // Jenis Produk
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        form.add(UITheme.createFieldLabel("Jenis Produk *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 4, 0);
        styleComboBox(cmbJenis, false, 8);
        cmbJenis.setPreferredSize(new Dimension(0, 32));
        form.add(cmbJenis, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel lblDynamicHint = new JLabel("Atribut menyesuaikan jenis produk");
        lblDynamicHint.setFont(UITheme.font(Font.PLAIN, 11f));
        lblDynamicHint.setForeground(TEXT_MUTED);
        form.add(lblDynamicHint, gbc);

        // Panel Dinamis Spesifik Kategori
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        buildDynamicSpecificCards();
        form.add(dynamicFieldPanel, gbc);

        // Box Estimasi Pajak (Polimorfik)
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 10, 0);
        JPanel pajakBox = new JPanel(new BorderLayout());
        pajakBox.setBackground(PRIMARY_BLUE_SOFT);
        pajakBox.setBorder(new RoundedBorder(UITheme.PRIMARY_SOFT_BORDER, 10, new Insets(8, 12, 8, 12)));
        lblEstimasiPajak.setFont(UITheme.font(Font.BOLD, 12f));
        lblEstimasiPajak.setForeground(UITheme.PRIMARY_PRESSED);
        pajakBox.add(lblEstimasiPajak, BorderLayout.CENTER);
        form.add(pajakBox, gbc);

        // Tombol Aksi Form
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 0, 0, 0);
        JPanel btnRow = new JPanel(new BorderLayout(6, 0));
        btnRow.setOpaque(false);

        btnHapusForm.setFont(UITheme.font(Font.BOLD, 12f));
        btnHapusForm.setPreferredSize(new Dimension(70, 34));

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightBtns.setOpaque(false);
        btnBatal.setFont(UITheme.font(Font.PLAIN, 12f));
        btnBatal.setPreferredSize(new Dimension(70, 34));
        btnSimpan.setFont(UITheme.font(Font.BOLD, 12f));
        btnSimpan.setPreferredSize(new Dimension(86, 34));
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
        scroll.getViewport().setBackground(CARD_BG);
        return scroll;
    }

    private void buildDynamicSpecificCards() {
        dynamicFieldPanel.setBackground(new Color(248, 250, 252));
        dynamicFieldPanel.setBorder(new RoundedBorder(BORDER_COLOR, 10, new Insets(12, 12, 12, 12)));

        // 1. Card MAKANAN
        JPanel cardMakanan = new JPanel(new GridBagLayout());
        cardMakanan.setOpaque(false);
        GridBagConstraints g1 = createDynamicGbc();
        g1.gridy = 0;
        cardMakanan.add(UITheme.createFieldLabel("Berat Bersih (gram) *"), g1);

        g1.gridy = 1;
        g1.insets = new Insets(5, 0, 6, 0);
        UITheme.styleNumericSpinner(spnBeratGram);
        cardMakanan.add(spnBeratGram, g1);

        g1.gridy = 2;
        g1.insets = new Insets(0, 0, 0, 0);
        JLabel noteMkn = new JLabel("Tarif pajak produk makanan: 5% dari harga jual.");
        noteMkn.setFont(UITheme.font(Font.PLAIN, 11f));
        noteMkn.setForeground(TEXT_MUTED);
        cardMakanan.add(noteMkn, g1);

        // 2. Card MINUMAN
        JPanel cardMinuman = new JPanel(new GridBagLayout());
        cardMinuman.setOpaque(false);
        GridBagConstraints g2 = createDynamicGbc();
        g2.gridy = 0;
        cardMinuman.add(UITheme.createFieldLabel("Volume Kemasan (ml) *"), g2);

        g2.gridy = 1;
        g2.insets = new Insets(5, 0, 10, 0);
        UITheme.styleNumericSpinner(spnVolumeMl);
        cardMinuman.add(spnVolumeMl, g2);

        g2.gridy = 2;
        g2.insets = new Insets(0, 0, 2, 0);
        chkDingin.setOpaque(false);
        chkDingin.setFont(UITheme.font(Font.BOLD, 12f));
        chkDingin.setForeground(TEXT_SLATE);
        chkDingin.setFocusPainted(false);
        cardMinuman.add(chkDingin, g2);

        g2.gridy = 3;
        g2.insets = new Insets(0, 22, 0, 0);
        JLabel noteMnm = new JLabel("Layanan chiller (+Rp 500 pajak)");
        noteMnm.setFont(UITheme.font(Font.PLAIN, 11f));
        noteMnm.setForeground(TEXT_MUTED);
        cardMinuman.add(noteMnm, g2);

        // 3. Card ELEKTRONIK
        JPanel cardElektronik = new JPanel(new GridBagLayout());
        cardElektronik.setOpaque(false);
        GridBagConstraints g3 = createDynamicGbc();
        g3.gridy = 0;
        cardElektronik.add(UITheme.createFieldLabel("Lama Garansi (bulan) *"), g3);

        g3.gridy = 1;
        g3.insets = new Insets(5, 0, 6, 0);
        UITheme.styleNumericSpinner(spnBulanGaransi);
        cardElektronik.add(spnBulanGaransi, g3);

        g3.gridy = 2;
        g3.insets = new Insets(0, 0, 0, 0);
        JLabel noteElk = new JLabel("Pajak: 15% (\u2264 12 bln) atau 17% (> 12 bln).");
        noteElk.setFont(UITheme.font(Font.PLAIN, 11f));
        noteElk.setForeground(TEXT_MUTED);
        cardElektronik.add(noteElk, g3);

        dynamicFieldPanel.add(cardMakanan, "MAKANAN");
        dynamicFieldPanel.add(cardMinuman, "MINUMAN");
        dynamicFieldPanel.add(cardElektronik, "ELEKTRONIK");
    }

    private JPanel buildBatchTabPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        // Form Tambah Batch
        JPanel addBatchCard = new JPanel(new GridBagLayout());
        addBatchCard.setBackground(new Color(248, 250, 252));
        addBatchCard.setBorder(new RoundedBorder(BORDER_COLOR, 10, new Insets(12, 12, 12, 12)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int r = 0;

        gbc.gridy = r++;
        gbc.insets = new Insets(0, 0, 8, 0);
        lblBatchInfoProduk.setFont(UITheme.font(Font.BOLD, 12f));
        lblBatchInfoProduk.setForeground(TEXT_DARK);
        addBatchCard.add(lblBatchInfoProduk, gbc);

        // Tgl Masuk & Stok Batch sejajar
        gbc.gridy = r++;
        gbc.insets = new Insets(0, 0, 8, 0);
        JPanel rowTop = new JPanel(new GridLayout(1, 2, 10, 0));
        rowTop.setOpaque(false);

        JPanel colMasuk = new JPanel(new BorderLayout(0, 4));
        colMasuk.setOpaque(false);
        colMasuk.add(UITheme.createFieldLabel("Tgl Masuk *"), BorderLayout.NORTH);
        colMasuk.add(dateMasukPicker, BorderLayout.CENTER);

        JPanel colStok = new JPanel(new BorderLayout(0, 4));
        colStok.setOpaque(false);
        colStok.add(UITheme.createFieldLabel("Stok Masuk *"), BorderLayout.NORTH);
        spnStokBatch.setEditor(new JSpinner.NumberEditor(spnStokBatch, "#"));
        spnStokBatch.setFont(UITheme.font(Font.PLAIN, 12f));
        spnStokBatch.setPreferredSize(new Dimension(0, 34));
        spnStokBatch.setBorder(new RoundedBorder(INPUT_BORDER, UITheme.SMALL_RADIUS, new Insets(1, 4, 1, 4)));
        colStok.add(spnStokBatch, BorderLayout.CENTER);

        rowTop.add(colMasuk);
        rowTop.add(colStok);
        addBatchCard.add(rowTop, gbc);

        // Tgl Kadaluarsa
        gbc.gridy = r++;
        gbc.insets = new Insets(0, 0, 4, 0);
        addBatchCard.add(UITheme.createFieldLabel("Tanggal Kedaluwarsa"), gbc);

        gbc.gridy = r++;
        gbc.insets = new Insets(0, 0, 4, 0);
        addBatchCard.add(dateKadaluarsaPicker, gbc);

        gbc.gridy = r++;
        gbc.insets = new Insets(0, 0, 10, 0);
        lblInfoKadaluarsaElektronik.setFont(UITheme.font(Font.ITALIC, 11f));
        lblInfoKadaluarsaElektronik.setForeground(TEXT_MUTED);
        addBatchCard.add(lblInfoKadaluarsaElektronik, gbc);

        gbc.gridy = r;
        gbc.insets = new Insets(0, 0, 0, 0);
        btnTambahBatch.setFont(UITheme.font(Font.BOLD, 12f));
        btnTambahBatch.setPreferredSize(new Dimension(0, 34));
        addBatchCard.add(btnTambahBatch, gbc);

        panel.add(addBatchCard, BorderLayout.NORTH);

        // Daftar Batch
        JScrollPane batchScroll = new JScrollPane(tableBatch);
        batchScroll.getViewport().setBackground(Color.WHITE);
        batchScroll.setBorder(new RoundedBorder(BORDER_COLOR, 10, new Insets(1, 1, 1, 1)));
        panel.add(batchScroll, BorderLayout.CENTER);

        // Footer Batch
        JPanel batchFooter = new JPanel(new BorderLayout());
        batchFooter.setOpaque(false);
        lblTotalStokBatch.setFont(UITheme.font(Font.BOLD, 12f));
        lblTotalStokBatch.setForeground(TEXT_DARK);
        batchFooter.add(lblTotalStokBatch, BorderLayout.WEST);

        btnHapusBatch.setFont(UITheme.font(Font.BOLD, 12f));
        btnHapusBatch.setPreferredSize(new Dimension(112, 32));
        btnHapusBatch.setEnabled(false);
        batchFooter.add(btnHapusBatch, BorderLayout.EAST);

        panel.add(batchFooter, BorderLayout.SOUTH);
        return panel;
    }

    private void konfigurasiTabelProduk() {
        tableProduk.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableProduk.setRowHeight(42);
        tableProduk.setFont(UITheme.font(Font.PLAIN, 13f));
        tableProduk.setForeground(TEXT_DARK);
        tableProduk.setGridColor(UITheme.BG_HOVER);
        tableProduk.setSelectionBackground(PRIMARY_BLUE_SOFT);
        tableProduk.setSelectionForeground(TEXT_DARK);
        tableProduk.setShowVerticalLines(false);
        tableProduk.getTableHeader().setReorderingAllowed(false);

        tableProduk.getTableHeader().setFont(UITheme.font(Font.BOLD, 11f));
        tableProduk.getTableHeader().setBackground(UITheme.BG_PAGE);
        tableProduk.getTableHeader().setForeground(TEXT_MUTED);
        tableProduk.getTableHeader().setPreferredSize(new Dimension(0, 38));
        tableProduk.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return this;
            }
        };

        DefaultTableCellRenderer kodeRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setFont(UITheme.font(Font.BOLD, 11f));
                setForeground(isSelected ? PRIMARY_BLUE : TEXT_SLATE);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 4));
                return this;
            }
        };

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 6));
                return this;
            }
        };

        DefaultTableCellRenderer jenisRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                String jenis = value != null ? value.toString() : "";
                JLabel badge = new JLabel(formatNamaJenis(jenis), SwingConstants.CENTER);
                badge.setFont(UITheme.font(Font.BOLD, 10f));
                badge.setOpaque(true);
                badge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

                switch (jenis) {
                    case "MAKANAN" -> {
                        badge.setBackground(BADGE_MAKANAN_BG);
                        badge.setForeground(BADGE_MAKANAN_FG);
                    }
                    case "MINUMAN" -> {
                        badge.setBackground(BADGE_MINUMAN_BG);
                        badge.setForeground(BADGE_MINUMAN_FG);
                    }
                    case "ELEKTRONIK" -> {
                        badge.setBackground(BADGE_ELEKTRONIK_BG);
                        badge.setForeground(BADGE_ELEKTRONIK_FG);
                    }
                    default -> {
                        badge.setBackground(UITheme.BG_HOVER);
                        badge.setForeground(TEXT_SLATE);
                    }
                }

                JPanel wrapper = new JPanel(new GridBagLayout());
                wrapper.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                wrapper.add(badge);
                return wrapper;
            }
        };

        DefaultTableCellRenderer stokRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                int stok = (value instanceof Number n) ? n.intValue() : 0;
                if (stok <= 0) {
                    setText("0 (Habis)");
                    setForeground(DANGER_RED);
                    setFont(UITheme.font(Font.BOLD, 12f));
                } else if (stok <= 5) {
                    setText(stok + " (Tipis)");
                    setForeground(BADGE_MAKANAN_FG);
                    setFont(UITheme.font(Font.BOLD, 12f));
                } else {
                    setText(String.valueOf(stok));
                    setForeground(TEXT_DARK);
                    setFont(UITheme.font(Font.BOLD, 12f));
                }
                return this;
            }
        };

        tableProduk.getColumnModel().getColumn(0).setCellRenderer(kodeRenderer);
        tableProduk.getColumnModel().getColumn(1).setCellRenderer(textRenderer);
        tableProduk.getColumnModel().getColumn(2).setCellRenderer(jenisRenderer);
        tableProduk.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        tableProduk.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        tableProduk.getColumnModel().getColumn(5).setCellRenderer(stokRenderer);
        tableProduk.getColumnModel().getColumn(6).setCellRenderer(textRenderer);

        tableProduk.getColumnModel().getColumn(0).setPreferredWidth(64);
        tableProduk.getColumnModel().getColumn(1).setPreferredWidth(132);
        tableProduk.getColumnModel().getColumn(2).setPreferredWidth(74);
        tableProduk.getColumnModel().getColumn(3).setPreferredWidth(70);
        tableProduk.getColumnModel().getColumn(4).setPreferredWidth(60);
        tableProduk.getColumnModel().getColumn(5).setPreferredWidth(44);
        tableProduk.getColumnModel().getColumn(6).setPreferredWidth(86);
    }

    private void konfigurasiTabelBatch() {
        tableBatch.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableBatch.setRowHeight(34);
        tableBatch.setFont(UITheme.font(Font.PLAIN, 12f));
        tableBatch.setForeground(TEXT_DARK);
        tableBatch.setGridColor(UITheme.BG_HOVER);
        tableBatch.setSelectionBackground(PRIMARY_BLUE_SOFT);
        tableBatch.setSelectionForeground(TEXT_DARK);
        tableBatch.setShowVerticalLines(false);
        tableBatch.getTableHeader().setReorderingAllowed(false);

        tableBatch.getTableHeader().setFont(UITheme.font(Font.BOLD, 11f));
        tableBatch.getTableHeader().setBackground(UITheme.BG_PAGE);
        tableBatch.getTableHeader().setForeground(TEXT_MUTED);
        tableBatch.getTableHeader().setPreferredSize(new Dimension(0, 32));
        tableBatch.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        DefaultTableCellRenderer cellPad = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        };

        DefaultTableCellRenderer stokCell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(UITheme.font(Font.BOLD, 12f));
                setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                return this;
            }
        };

        DefaultTableCellRenderer statusCell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                String status = value != null ? value.toString() : "";
                setFont(UITheme.font(Font.BOLD, 11f));
                switch (status) {
                    case "Kedaluwarsa" -> setForeground(DANGER_RED);
                    case "Hampir kedaluwarsa" -> setForeground(BADGE_MAKANAN_FG);
                    case "Aman" -> setForeground(UITheme.SUCCESS);
                    default -> setForeground(TEXT_MUTED);
                }
                return this;
            }
        };

        tableBatch.getColumnModel().getColumn(0).setCellRenderer(cellPad);
        tableBatch.getColumnModel().getColumn(1).setCellRenderer(cellPad);
        tableBatch.getColumnModel().getColumn(2).setCellRenderer(stokCell);
        tableBatch.getColumnModel().getColumn(3).setCellRenderer(statusCell);

        tableBatch.getColumnModel().getColumn(0).setPreferredWidth(78);
        tableBatch.getColumnModel().getColumn(1).setPreferredWidth(96);
        tableBatch.getColumnModel().getColumn(2).setPreferredWidth(40);
        tableBatch.getColumnModel().getColumn(3).setPreferredWidth(78);
    }

    private void initActions() {
        btnTabDetail.addActionListener(e -> pilihTab("DETAIL"));
        btnTabBatch.addActionListener(e -> {
            if (produkTerpilih == null) {
                toastManager.show(
                        "Produk Belum Dipilih",
                        "Simpan atau pilih produk terlebih dahulu untuk mengelola batch stok.",
                        ToastType.INFO
                );
                return;
            }
            pilihTab("BATCH");
        });

        DocumentListener dirtyAndTaxListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!isPopulatingForm) {
                    isFormDirty = true;
                }
                perbaruiEstimasiPajak();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!isPopulatingForm) {
                    isFormDirty = true;
                }
                perbaruiEstimasiPajak();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!isPopulatingForm) {
                    isFormDirty = true;
                }
                perbaruiEstimasiPajak();
            }
        };
        DocumentListener dirtyOnlyListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!isPopulatingForm) {
                    isFormDirty = true;
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!isPopulatingForm) {
                    isFormDirty = true;
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!isPopulatingForm) {
                    isFormDirty = true;
                }
            }
        };

        txtKode.getDocument().addDocumentListener(dirtyOnlyListener);
        txtNama.getDocument().addDocumentListener(dirtyOnlyListener);
        txtHarga.getDocument().addDocumentListener(dirtyAndTaxListener);

        cmbJenis.addActionListener(e -> {
            String jenis = (String) cmbJenis.getSelectedItem();
            if (jenis != null) {
                dynamicFieldLayout.show(dynamicFieldPanel, jenis);
            }
            if (!isPopulatingForm) {
                isFormDirty = true;
            }
            perbaruiEstimasiPajak();
        });

        chkDingin.addActionListener(e -> {
            if (!isPopulatingForm) {
                isFormDirty = true;
            }
            perbaruiEstimasiPajak();
        });
        spnBeratGram.addChangeListener(e -> {
            if (!isPopulatingForm) {
                isFormDirty = true;
            }
        });
        spnVolumeMl.addChangeListener(e -> {
            if (!isPopulatingForm) {
                isFormDirty = true;
            }
        });
        spnBulanGaransi.addChangeListener(e -> {
            if (!isPopulatingForm) {
                isFormDirty = true;
            }
            perbaruiEstimasiPajak();
        });

        tableProduk.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || isRefreshingTable) {
                return;
            }
            int selectedRow = tableProduk.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < daftarProdukSaatIni.size()) {
                masukModeUbah(daftarProdukSaatIni.get(selectedRow));
                tampilkanSidePanel();
            }
        });

        tableBatch.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                btnHapusBatch.setEnabled(tableBatch.getSelectedRow() >= 0 && produkTerpilih != null);
            }
        });

        btnCari.addActionListener(e -> jalankanPencarian());
        txtCari.addActionListener(e -> jalankanPencarian());
        cmbFilterJenis.addActionListener(e -> {
            if (!isUpdatingFilters) {
                jalankanPencarian();
            }
        });

        btnTambahProduk.addActionListener(e -> {
            isRefreshingTable = true;
            try {
                tableProduk.clearSelection();
            } finally {
                isRefreshingTable = false;
            }
            masukModeTambah();
            tampilkanSidePanel();
            txtKode.requestFocusInWindow();
        });

        btnTutupPanel.addActionListener(e -> tutupSidePanel());

        btnSegarkan.addActionListener(e -> {
            isUpdatingFilters = true;
            try {
                txtCari.setText("");
                cmbFilterJenis.setSelectedIndex(0);
            } finally {
                isUpdatingFilters = false;
            }
            Integer keepId = (produkTerpilih != null) ? produkTerpilih.getId() : null;
            jalankanPencarian(keepId, false, null);
        });

        btnBatal.addActionListener(e -> tutupSidePanel());

        btnSimpan.addActionListener(e -> simpanProduk());
        btnHapusForm.addActionListener(e -> hapusProdukTerpilih());
        btnTambahBatch.addActionListener(e -> tambahBatchBaru());
        btnHapusBatch.addActionListener(e -> hapusBatchTerpilih());
    }

    /**
     * Memanggil overloading method cariProduk() pada ProdukController sesuai pilihan filter kategori.
     */
    @Override
    public void refreshData() {
        Integer selectedId = (produkTerpilih != null && sideCard.isVisible()) ? produkTerpilih.getId() : null;
        jalankanPencarian(selectedId, true, null);
    }

    private void jalankanPencarian() {
        Integer selectedId = (produkTerpilih != null && sideCard.isVisible()) ? produkTerpilih.getId() : null;
        jalankanPencarian(selectedId, false, null);
    }

    private void jalankanPencarian(Integer selectProductId, boolean preserveDirtyForm, Runnable onAfterLoad) {
        final int reqId = ++loadRequestId;
        final String keyword = txtCari.getText().trim();
        final String filterJenis = (String) cmbFilterJenis.getSelectedItem();

        lblRingkasanTabel.setText("Memuat katalog produk...");
        btnSegarkan.setEnabled(false);

        SwingWorker<List<Produk>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Produk> doInBackground() throws Exception {
                if (filterJenis == null || "SEMUA".equals(filterJenis)) {
                    return produkController.cariProduk(keyword);
                }
                return produkController.cariProduk(keyword, filterJenis);
            }

            @Override
            protected void done() {
                if (reqId != loadRequestId) {
                    return;
                }
                btnSegarkan.setEnabled(true);
                try {
                    List<Produk> hasil = get();
                    tampilkanDaftarProduk(hasil, selectProductId, preserveDirtyForm);
                    if (onAfterLoad != null) {
                        onAfterLoad.run();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    lblRingkasanTabel.setText("Gagal memuat katalog produk");
                    Throwable cause = ex.getCause();
                    if (cause instanceof IllegalArgumentException argEx) {
                        ModernModal.error(ManajemenProdukView.this, "Kesalahan Validasi", argEx.getMessage());
                    } else if (cause instanceof SQLException sqlEx) {
                        ModernModal.error(ManajemenProdukView.this, "Kesalahan Database",
                                ProdukController.pesanSqlError(sqlEx));
                    } else {
                        ModernModal.error(ManajemenProdukView.this, "Kesalahan Sistem",
                                "Gagal memuat produk:\n" + (cause != null ? cause.getMessage() : ex.getMessage()));
                    }
                }
            }
        };
        worker.execute();
    }

    private void tampilkanDaftarProduk(List<Produk> list, Integer selectProductId, boolean preserveDirtyForm) {
        isRefreshingTable = true;
        try {
            daftarProdukSaatIni = new ArrayList<>(list);
            produkTableModel.setRowCount(0);

            int indexToSelect = -1;
            for (int i = 0; i < daftarProdukSaatIni.size(); i++) {
                Produk p = daftarProdukSaatIni.get(i);
                // Pemanggilan polimorfik hitungPajak(), getJenisProduk(), getInfoSpesifik() via tipe Produk
                produkTableModel.addRow(new Object[]{
                        p.getKode(),
                        p.getNama(),
                        p.getJenisProduk(),
                        UITheme.formatRupiah(p.getHarga()),
                        UITheme.formatRupiah(p.hitungPajak()),
                        p.getStok(),
                        p.getInfoSpesifik()
                });
                if (selectProductId != null && p.getId() == selectProductId) {
                    indexToSelect = i;
                }
            }

            lblRingkasanTabel.setText("Menampilkan " + daftarProdukSaatIni.size() + " produk");

            if (indexToSelect >= 0) {
                tableProduk.setRowSelectionInterval(indexToSelect, indexToSelect);
                Produk terpilihBaru = daftarProdukSaatIni.get(indexToSelect);
                if (preserveDirtyForm && isFormDirty) {
                    produkTerpilih = terpilihBaru;
                    muatDataBatchProduk(terpilihBaru);
                } else {
                    masukModeUbah(terpilihBaru);
                }
                tampilkanSidePanel();
            } else {
                tableProduk.clearSelection();
                if (selectProductId != null) {
                    tutupSidePanel();
                }
            }
        } finally {
            isRefreshingTable = false;
        }
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
            tableProduk.clearSelection();
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
        isPopulatingForm = true;
        try {
            produkTerpilih = null;
            lblFormTitle.setText("Tambah Produk");
            lblFormSubtitle.setText("Lengkapi data katalog produk baru");
            btnHapusForm.setVisible(false);

            txtKode.setText("");
            txtNama.setText("");
            txtHarga.setText("");
            cmbJenis.setSelectedItem("MAKANAN");
            dynamicFieldLayout.show(dynamicFieldPanel, "MAKANAN");

            spnBeratGram.setValue(100);
            spnVolumeMl.setValue(250);
            chkDingin.setSelected(false);
            spnBulanGaransi.setValue(12);

            btnTabBatch.setText("Batch Stok");
            batchTableModel.setRowCount(0);
            daftarBatchSaatIni.clear();
            lblBatchInfoProduk.setText("Pilih produk untuk mengelola batch stok");
            lblTotalStokBatch.setText("Total Stok Aktif: 0 unit");

            pilihTab("DETAIL");
            perbaruiEstimasiPajak();
            isFormDirty = false;
        } finally {
            isPopulatingForm = false;
        }
    }

    private void masukModeUbah(Produk p) {
        isPopulatingForm = true;
        try {
            produkTerpilih = p;
            lblFormTitle.setText("Ubah Produk");
            lblFormSubtitle.setText(p.getKode() + " \u2022 " + p.getNama());
            btnHapusForm.setVisible(true);

            txtKode.setText(p.getKode());
            txtNama.setText(p.getNama());
            txtHarga.setText(formatInputHarga(p.getHarga()));

            cmbJenis.setSelectedItem(p.getJenisProduk());
            dynamicFieldLayout.show(dynamicFieldPanel, p.getJenisProduk());

            if (p instanceof Makanan m) {
                spnBeratGram.setValue(m.getBeratGram());
            } else if (p instanceof Minuman mn) {
                spnVolumeMl.setValue(mn.getVolumeMl());
                chkDingin.setSelected(mn.isDingin());
            } else if (p instanceof Elektronik el) {
                spnBulanGaransi.setValue(el.getBulanGaransi());
            }

            perbaruiEstimasiPajak();
            muatDataBatchProduk(p);
            isFormDirty = false;
        } finally {
            isPopulatingForm = false;
        }
    }

    private void muatDataBatchProduk(Produk p) {
        daftarBatchSaatIni = new ArrayList<>(p.getDaftarBatch());
        batchTableModel.setRowCount(0);

        for (BatchProduk b : daftarBatchSaatIni) {
            String masuk = DATE_DISPLAY_FMT.format(b.getTglMasuk());
            String exp = (b.getTglKadaluarsa() != null)
                    ? DATE_DISPLAY_FMT.format(b.getTglKadaluarsa())
                    : "-";
            String status;
            if (b.getTglKadaluarsa() == null) {
                status = "Aktif";
            } else if (b.isKadaluarsa()) {
                status = "Kedaluwarsa";
            } else if (b.isHampirKadaluarsa(7)) {
                status = "Hampir kedaluwarsa";
            } else {
                status = "Aman";
            }

            batchTableModel.addRow(new Object[]{masuk, exp, b.getStokBatch(), status});
        }

        boolean isElektronik = "ELEKTRONIK".equals(p.getJenisProduk());
        dateKadaluarsaPicker.setEnabled(!isElektronik);
        lblInfoKadaluarsaElektronik.setVisible(isElektronik);

        int stokAktif = p.getStok();
        int stokKadaluarsa = p.getStokKadaluarsa();
        btnTabBatch.setText("Batch Stok (" + daftarBatchSaatIni.size() + ")");
        lblBatchInfoProduk.setText(p.getKode() + " \u2014 " + p.getNama());
        lblTotalStokBatch.setText(stokKadaluarsa > 0
                ? "Stok Aktif: " + stokAktif + " unit (" + stokKadaluarsa + " kedaluwarsa)"
                : "Total Stok Aktif: " + stokAktif + " unit");
        btnHapusBatch.setEnabled(false);
    }

    private boolean cocokDenganFilterAktif(Produk p) {
        String filterJenis = (String) cmbFilterJenis.getSelectedItem();
        if (filterJenis != null && !"SEMUA".equals(filterJenis)
                && !filterJenis.equalsIgnoreCase(p.getJenisProduk())) {
            return false;
        }
        String keyword = txtCari.getText().trim().toLowerCase(Locale.ROOT);
        if (keyword.isEmpty()) {
            return true;
        }
        return p.getNama().toLowerCase(Locale.ROOT).contains(keyword)
                || p.getKode().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private void simpanProduk() {
        try {
            int id = (produkTerpilih != null) ? produkTerpilih.getId() : 0;
            Produk produkBaru = bangunProdukDariForm(id);

            if (produkTerpilih == null) {
                produkController.tambahProduk(produkBaru);
                if (!cocokDenganFilterAktif(produkBaru)) {
                    isUpdatingFilters = true;
                    try {
                        txtCari.setText("");
                        cmbFilterJenis.setSelectedIndex(0);
                    } finally {
                        isUpdatingFilters = false;
                    }
                }
                jalankanPencarian(produkBaru.getId(), false, () -> {
                    if (produkTerpilih != null) {
                        pilihTab("BATCH");
                    }
                });
                toastManager.show(
                        "Produk Ditambahkan",
                        "Silakan tambahkan batch stok awal pada tab Batch Stok.",
                        ToastType.SUCCESS
                );
            } else {
                produkController.ubahProduk(produkBaru);
                jalankanPencarian(produkBaru.getId(), false, null);
                toastManager.show(
                        "Produk Diperbarui",
                        "Data produk berhasil disimpan.",
                        ToastType.SUCCESS
                );
            }
        } catch (IllegalArgumentException ex) {
            ModernModal.error(this, "Kesalahan Validasi", ex.getMessage());
        } catch (SQLException ex) {
            ModernModal.error(this, "Kesalahan Database", ProdukController.pesanSqlError(ex));
        }
    }

    private void hapusProdukTerpilih() {
        if (produkTerpilih == null) {
            toastManager.show(
                    "Produk Belum Dipilih",
                    "Pilih baris produk yang ingin dihapus terlebih dahulu.",
                    ToastType.INFO
            );
            return;
        }

        boolean disetujui = ModernModal.confirm(
                this,
                "Hapus Produk",
                "Yakin ingin menghapus '" + produkTerpilih.getNama()
                        + "' (" + produkTerpilih.getKode() + ")? Seluruh batch stok produk ini juga akan terhapus.",
                "Ya, Hapus"
        );

        if (!disetujui) {
            return;
        }

        try {
            String namaDihapus = produkTerpilih.getNama();
            boolean archived = produkController.hapusProduk(produkTerpilih.getId());
            tutupSidePanel();
            jalankanPencarian(null, false, null);
            toastManager.show(
                    "Produk Dihapus",
                    archived
                            ? "Produk '" + namaDihapus + "' dihapus dari katalog aktif (riwayat transaksi tetap terjaga)."
                            : "Produk '" + namaDihapus + "' berhasil dihapus.",
                    ToastType.SUCCESS
            );
        } catch (IllegalArgumentException ex) {
            ModernModal.error(this, "Aksi Ditolak", ex.getMessage());
        } catch (SQLException ex) {
            ModernModal.error(this, "Kesalahan Database", ProdukController.pesanSqlError(ex));
        }
    }

    private void tambahBatchBaru() {
        if (produkTerpilih == null) {
            return;
        }

        try {
            LocalDate tglMasuk = dateMasukPicker.getDate();
            if (tglMasuk == null) {
                throw new IllegalArgumentException("Tanggal masuk tidak boleh kosong.");
            }
            LocalDate tglKadaluarsa = "ELEKTRONIK".equals(produkTerpilih.getJenisProduk())
                    ? null
                    : dateKadaluarsaPicker.getDate();
            int stokBatch = bacaIntSpinner(spnStokBatch, "Stok batch harus berupa bilangan bulat yang valid.");

            BatchProduk batch = new BatchProduk(produkTerpilih.getId(), tglMasuk, tglKadaluarsa, stokBatch);
            produkController.tambahStokBatch(batch);

            spnStokBatch.setValue(10);
            dateMasukPicker.setDate(LocalDate.now());
            dateKadaluarsaPicker.setDate(LocalDate.now().plusDays(30));
            int targetId = produkTerpilih.getId();
            jalankanPencarian(targetId, false, () -> pilihTab("BATCH"));
            toastManager.show(
                    "Batch Ditambahkan",
                    "Stok masuk sebanyak " + stokBatch + " unit berhasil dicatat.",
                    ToastType.SUCCESS
            );
        } catch (IllegalArgumentException ex) {
            ModernModal.error(this, "Kesalahan Validasi", ex.getMessage());
        } catch (SQLException ex) {
            ModernModal.error(this, "Kesalahan Database", ProdukController.pesanSqlError(ex));
        }
    }

    private void hapusBatchTerpilih() {
        if (produkTerpilih == null) {
            return;
        }
        int row = tableBatch.getSelectedRow();
        if (row < 0 || row >= daftarBatchSaatIni.size()) {
            return;
        }

        BatchProduk target = daftarBatchSaatIni.get(row);
        boolean disetujui = ModernModal.confirm(
                this,
                "Hapus Batch Stok",
                "Hapus batch stok (" + target.getStokBatch() + " unit, masuk "
                        + DATE_DISPLAY_FMT.format(target.getTglMasuk()) + ")?",
                "Ya, Hapus"
        );

        if (!disetujui) {
            return;
        }

        try {
            produkController.hapusBatch(target.getId());
            int targetId = produkTerpilih.getId();
            jalankanPencarian(targetId, false, () -> pilihTab("BATCH"));
            toastManager.show("Batch Dihapus", "Batch stok berhasil dihapus.", ToastType.SUCCESS);
        } catch (IllegalArgumentException ex) {
            ModernModal.error(this, "Aksi Ditolak", ex.getMessage());
        } catch (SQLException ex) {
            ModernModal.error(this, "Kesalahan Database", ProdukController.pesanSqlError(ex));
        }
    }

    private Produk bangunProdukDariForm(int id) {
        String kode = txtKode.getText();
        String nama = txtNama.getText();
        double harga = parseHarga(txtHarga.getText());
        String jenis = (String) cmbJenis.getSelectedItem();

        if ("MAKANAN".equals(jenis)) {
            int beratGram = bacaIntSpinner(spnBeratGram, "Berat makanan (gram) harus berupa bilangan bulat.");
            return new Makanan(id, kode, nama, harga, beratGram);
        }
        if ("MINUMAN".equals(jenis)) {
            int volumeMl = bacaIntSpinner(spnVolumeMl, "Volume minuman (ml) harus berupa bilangan bulat.");
            return new Minuman(id, kode, nama, harga, volumeMl, chkDingin.isSelected());
        }
        if ("ELEKTRONIK".equals(jenis)) {
            int garansi = bacaIntSpinner(spnBulanGaransi, "Lama garansi (bulan) harus berupa bilangan bulat.");
            return new Elektronik(id, kode, nama, harga, garansi);
        }
        throw new IllegalArgumentException("Jenis produk harus dipilih.");
    }

    private void perbaruiEstimasiPajak() {
        String hargaText = txtHarga.getText();
        if (hargaText == null || hargaText.isBlank()) {
            lblEstimasiPajak.setText("Estimasi Pajak: -");
            return;
        }
        try {
            double harga = parseHarga(hargaText);
            String jenis = (String) cmbJenis.getSelectedItem();
            Produk sampel;
            if ("MAKANAN".equals(jenis)) {
                sampel = new Makanan(0, "TMP", "Sampel", harga, 1);
            } else if ("MINUMAN".equals(jenis)) {
                sampel = new Minuman(0, "TMP", "Sampel", harga, 1, chkDingin.isSelected());
            } else {
                Object val = spnBulanGaransi.getValue();
                int garansi = (val instanceof Number n) ? Math.max(0, n.intValue()) : 0;
                sampel = new Elektronik(0, "TMP", "Sampel", harga, garansi);
            }
            lblEstimasiPajak.setText("Estimasi Pajak: " + UITheme.formatRupiah(sampel.hitungPajak()) + " / unit");
        } catch (IllegalArgumentException ex) {
            lblEstimasiPajak.setText("Estimasi Pajak: -");
        }
    }

    private void pilihTab(String tabName) {
        rightTabLayout.show(rightTabContent, tabName);
        boolean detailAktif = "DETAIL".equals(tabName);
        setTabVisual(btnTabDetail, detailAktif);
        setTabVisual(btnTabBatch, !detailAktif);
    }

    private void setTabVisual(JButton btn, boolean active) {
        btn.setForeground(active ? PRIMARY_BLUE : TEXT_MUTED);
        btn.setFont(UITheme.font(active ? Font.BOLD : Font.PLAIN, 13f));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0, PRIMARY_BLUE),
                BorderFactory.createEmptyBorder(6, 2, 8, 2)
        ));
    }

    private static double parseHarga(String raw) {
        return UITheme.parseNominalIndonesia(
                raw,
                "Harga produk tidak boleh kosong.",
                "Harga produk harus berupa angka yang valid (contoh: 15000, 15.000, atau 15.000,50)."
        );
    }

    private static int bacaIntSpinner(JSpinner spinner, String errorMsg) {
        try {
            spinner.commitEdit();
        } catch (ParseException ex) {
            throw new IllegalArgumentException(errorMsg);
        }
        Object val = spinner.getValue();
        if (val instanceof Number n) {
            return n.intValue();
        }
        throw new IllegalArgumentException(errorMsg);
    }

    private static GridBagConstraints createDynamicGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        return gbc;
    }

    private static void konfigurasiTabButton(JButton btn) {
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static void styleComboBox(JComboBox<String> combo, boolean includeSemua, int radius) {
        RoundedComboBoxUI.apply(combo, radius);
        combo.setRenderer(new JenisProdukRenderer(includeSemua));
    }

    private static String formatInputHarga(double harga) {
        if (harga == Math.rint(harga)) {
            return String.valueOf((long) harga);
        }
        return BigDecimal.valueOf(harga).stripTrailingZeros().toPlainString();
    }

    private static String formatNamaJenis(String kodeJenis) {
        return switch (kodeJenis) {
            case "MAKANAN" -> "Makanan";
            case "MINUMAN" -> "Minuman";
            case "ELEKTRONIK" -> "Elektronik";
            default -> kodeJenis;
        };
    }

    private static class JenisProdukRenderer extends DefaultListCellRenderer {
        private final boolean includeSemua;

        JenisProdukRenderer(boolean includeSemua) {
            this.includeSemua = includeSemua;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setFont(UITheme.font((index >= 0 && isSelected) ? Font.BOLD : Font.PLAIN, 12f));
            if (index == -1) {
                setOpaque(false);
                setForeground(TEXT_DARK);
                setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            } else {
                setOpaque(true);
                setBackground(isSelected ? PRIMARY_BLUE_SOFT : Color.WHITE);
                setForeground(isSelected ? PRIMARY_BLUE : TEXT_DARK);
                setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            }
            if (value != null) {
                String code = value.toString();
                if (includeSemua && "SEMUA".equals(code)) {
                    setText("Semua Jenis");
                } else {
                    setText(formatNamaJenis(code));
                }
            }
            return this;
        }
    }
}
