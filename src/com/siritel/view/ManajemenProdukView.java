package com.siritel.view;

import com.siritel.controller.ProdukController;
import com.siritel.model.BatchProduk;
import com.siritel.model.Elektronik;
import com.siritel.model.Makanan;
import com.siritel.model.Minuman;
import com.siritel.model.Produk;
import com.siritel.view.UiStyleSupport.ButtonVariant;
import com.siritel.view.UiStyleSupport.RoundedBorder;
import com.siritel.view.UiStyleSupport.RoundedButton;
import com.siritel.view.UiStyleSupport.ToastManager;
import com.siritel.view.UiStyleSupport.ToastType;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Halaman Manajemen Produk & Stok (Modul 2).
 * Menampilkan katalog barang dengan kolom jenis dan nominal pajak polimorfik (hitungPajak()),
 * pencarian dengan method overloading cariProduk(), form dinamis kategori, serta manajemen batch FEFO.
 */
public class ManajemenProdukView extends JPanel {
    private static final Color BG_PANEL = new Color(248, 250, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color INPUT_BORDER = new Color(203, 213, 225);
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color PRIMARY_BLUE_SOFT = new Color(239, 246, 255);
    private static final Color DANGER_RED = new Color(220, 38, 38);
    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_SLATE = new Color(51, 65, 85);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    private static final Color BADGE_MAKANAN_BG = new Color(254, 243, 199);
    private static final Color BADGE_MAKANAN_FG = new Color(180, 83, 9);
    private static final Color BADGE_MINUMAN_BG = new Color(219, 234, 254);
    private static final Color BADGE_MINUMAN_FG = new Color(29, 78, 216);
    private static final Color BADGE_ELEKTRONIK_BG = new Color(243, 232, 255);
    private static final Color BADGE_ELEKTRONIK_FG = new Color(126, 34, 206);

    private static final Locale ID_LOCALE = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter DATE_DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", ID_LOCALE);

    private final ProdukController produkController = new ProdukController();
    private final ToastManager toastManager = new ToastManager(this);
    private List<Produk> daftarProdukSaatIni = new ArrayList<>();
    private List<BatchProduk> daftarBatchSaatIni = new ArrayList<>();
    private Produk produkTerpilih;
    private boolean isRefreshingTable = false;
    private boolean isUpdatingFilters = false;

    // Toolbar & Katalog Kiri
    private final JTextField txtCari = createPlaceholderField("Cari kode atau nama produk...");
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
            new JSpinner(new SpinnerNumberModel(Integer.valueOf(100), null, Integer.valueOf(1000000), Integer.valueOf(50)));
    private final JSpinner spnVolumeMl =
            new JSpinner(new SpinnerNumberModel(Integer.valueOf(250), null, Integer.valueOf(1000000), Integer.valueOf(50)));
    private final JCheckBox chkDingin = new JCheckBox("Minuman Dingin");
    private final JSpinner spnBulanGaransi =
            new JSpinner(new SpinnerNumberModel(Integer.valueOf(12), null, Integer.valueOf(240), Integer.valueOf(1)));

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
            new JSpinner(new SpinnerNumberModel(Integer.valueOf(10), null, Integer.valueOf(1000000), Integer.valueOf(1)));
    private final RoundedButton btnTambahBatch = new RoundedButton("+ Tambah Batch", ButtonVariant.PRIMARY, 10);
    private final RoundedButton btnHapusBatch = new RoundedButton("Hapus Batch", ButtonVariant.DANGER_OUTLINE, 10);
    private final JLabel lblTotalStokBatch = new JLabel("Total Stok: 0");
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
        sideCard.setVisible(false);
        add(sideCard, BorderLayout.EAST);

        // 4. Event Listeners & State Awal
        initActions();
        tutupSidePanel();
        muatDataTabel(null);
    }

    private JPanel buildCatalogCard() {
        JPanel card = UiStyleSupport.createRoundedCardPanel(new BorderLayout(0, 10), 14);
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
        lblCatalogTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblCatalogTitle.setForeground(TEXT_DARK);
        lblCatalogTitle.setMinimumSize(new Dimension(50, 20));
        hrGbc.gridx = 0;
        hrGbc.weightx = 1.0;
        hrGbc.fill = GridBagConstraints.HORIZONTAL;
        headerRow.add(lblCatalogTitle, hrGbc);

        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new javax.swing.BoxLayout(actionPanel, javax.swing.BoxLayout.X_AXIS));
        actionPanel.setOpaque(false);

        btnTambahProduk.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnTambahProduk.setPreferredSize(new Dimension(130, 33));
        btnTambahProduk.setMinimumSize(new Dimension(130, 33));
        btnTambahProduk.setMaximumSize(new Dimension(130, 33));

        btnSegarkan.setFont(new Font("SansSerif", Font.PLAIN, 12));
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

        txtCari.setFont(new Font("SansSerif", Font.PLAIN, 12));
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

        btnCari.setFont(new Font("SansSerif", Font.BOLD, 12));
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

        lblRingkasanTabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblRingkasanTabel.setForeground(TEXT_MUTED);
        footer.add(lblRingkasanTabel, BorderLayout.WEST);

        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildSideEditorCard() {
        JPanel sidePanel = UiStyleSupport.createRoundedCardPanel(new BorderLayout(), 14);
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
        lblFormTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblFormTitle.setForeground(TEXT_DARK);
        lblFormSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblFormSubtitle.setForeground(TEXT_MUTED);
        titleGroup.add(lblFormTitle);
        titleGroup.add(lblFormSubtitle);
        topHeader.add(titleGroup, BorderLayout.CENTER);

        btnTutupPanel.setFont(new Font("SansSerif", Font.BOLD, 16));
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
        form.add(createFieldLabel("Kode Produk *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        styleInputField(txtKode);
        form.add(txtKode, gbc);

        // Nama Produk
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        form.add(createFieldLabel("Nama Produk *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        styleInputField(txtNama);
        form.add(txtNama, gbc);

        // Harga Jual
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        form.add(createFieldLabel("Harga Jual (Rp) *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        styleInputField(txtHarga);
        form.add(txtHarga, gbc);

        // Jenis Produk
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        form.add(createFieldLabel("Jenis Produk *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 4, 0);
        styleComboBox(cmbJenis, false, 8);
        cmbJenis.setPreferredSize(new Dimension(0, 32));
        form.add(cmbJenis, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel lblDynamicHint = new JLabel("Atribut menyesuaikan jenis produk");
        lblDynamicHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
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
        pajakBox.setBorder(new RoundedBorder(new Color(191, 219, 254), 10, new Insets(8, 12, 8, 12)));
        lblEstimasiPajak.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblEstimasiPajak.setForeground(new Color(30, 64, 175));
        pajakBox.add(lblEstimasiPajak, BorderLayout.CENTER);
        form.add(pajakBox, gbc);

        // Tombol Aksi Form
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 0, 0, 0);
        JPanel btnRow = new JPanel(new BorderLayout(6, 0));
        btnRow.setOpaque(false);

        btnHapusForm.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnHapusForm.setPreferredSize(new Dimension(70, 34));

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightBtns.setOpaque(false);
        btnBatal.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnBatal.setPreferredSize(new Dimension(70, 34));
        btnSimpan.setFont(new Font("SansSerif", Font.BOLD, 12));
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
        cardMakanan.add(createFieldLabel("Berat Bersih (gram) *"), g1);

        g1.gridy = 1;
        g1.insets = new Insets(5, 0, 6, 0);
        styleNumericSpinner(spnBeratGram);
        cardMakanan.add(spnBeratGram, g1);

        g1.gridy = 2;
        g1.insets = new Insets(0, 0, 0, 0);
        JLabel noteMkn = new JLabel("Tarif pajak produk makanan: 5% dari harga jual.");
        noteMkn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        noteMkn.setForeground(TEXT_MUTED);
        cardMakanan.add(noteMkn, g1);

        // 2. Card MINUMAN
        JPanel cardMinuman = new JPanel(new GridBagLayout());
        cardMinuman.setOpaque(false);
        GridBagConstraints g2 = createDynamicGbc();
        g2.gridy = 0;
        cardMinuman.add(createFieldLabel("Volume Kemasan (ml) *"), g2);

        g2.gridy = 1;
        g2.insets = new Insets(5, 0, 10, 0);
        styleNumericSpinner(spnVolumeMl);
        cardMinuman.add(spnVolumeMl, g2);

        g2.gridy = 2;
        g2.insets = new Insets(0, 0, 2, 0);
        chkDingin.setOpaque(false);
        chkDingin.setFont(new Font("SansSerif", Font.BOLD, 12));
        chkDingin.setForeground(TEXT_SLATE);
        chkDingin.setFocusPainted(false);
        cardMinuman.add(chkDingin, g2);

        g2.gridy = 3;
        g2.insets = new Insets(0, 22, 0, 0);
        JLabel noteMnm = new JLabel("Layanan chiller (+Rp 500 pajak)");
        noteMnm.setFont(new Font("SansSerif", Font.PLAIN, 11));
        noteMnm.setForeground(TEXT_MUTED);
        cardMinuman.add(noteMnm, g2);

        // 3. Card ELEKTRONIK
        JPanel cardElektronik = new JPanel(new GridBagLayout());
        cardElektronik.setOpaque(false);
        GridBagConstraints g3 = createDynamicGbc();
        g3.gridy = 0;
        cardElektronik.add(createFieldLabel("Lama Garansi (bulan) *"), g3);

        g3.gridy = 1;
        g3.insets = new Insets(5, 0, 6, 0);
        styleNumericSpinner(spnBulanGaransi);
        cardElektronik.add(spnBulanGaransi, g3);

        g3.gridy = 2;
        g3.insets = new Insets(0, 0, 0, 0);
        JLabel noteElk = new JLabel("Pajak: 15% (\u2264 12 bln) atau 17% (> 12 bln).");
        noteElk.setFont(new Font("SansSerif", Font.PLAIN, 11));
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
        lblBatchInfoProduk.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblBatchInfoProduk.setForeground(TEXT_DARK);
        addBatchCard.add(lblBatchInfoProduk, gbc);

        // Tgl Masuk & Stok Batch sejajar
        gbc.gridy = r++;
        gbc.insets = new Insets(0, 0, 8, 0);
        JPanel rowTop = new JPanel(new GridLayout(1, 2, 10, 0));
        rowTop.setOpaque(false);

        JPanel colMasuk = new JPanel(new BorderLayout(0, 4));
        colMasuk.setOpaque(false);
        colMasuk.add(createFieldLabel("Tgl Masuk *"), BorderLayout.NORTH);
        colMasuk.add(dateMasukPicker, BorderLayout.CENTER);

        JPanel colStok = new JPanel(new BorderLayout(0, 4));
        colStok.setOpaque(false);
        colStok.add(createFieldLabel("Stok Masuk *"), BorderLayout.NORTH);
        styleNumericSpinner(spnStokBatch);
        spnStokBatch.setPreferredSize(new Dimension(0, 34));
        colStok.add(spnStokBatch, BorderLayout.CENTER);

        rowTop.add(colMasuk);
        rowTop.add(colStok);
        addBatchCard.add(rowTop, gbc);

        // Tgl Kadaluarsa
        gbc.gridy = r++;
        gbc.insets = new Insets(0, 0, 4, 0);
        addBatchCard.add(createFieldLabel("Tanggal Kedaluwarsa"), gbc);

        gbc.gridy = r++;
        gbc.insets = new Insets(0, 0, 4, 0);
        addBatchCard.add(dateKadaluarsaPicker, gbc);

        gbc.gridy = r++;
        gbc.insets = new Insets(0, 0, 10, 0);
        lblInfoKadaluarsaElektronik.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblInfoKadaluarsaElektronik.setForeground(TEXT_MUTED);
        addBatchCard.add(lblInfoKadaluarsaElektronik, gbc);

        gbc.gridy = r;
        gbc.insets = new Insets(0, 0, 0, 0);
        btnTambahBatch.setFont(new Font("SansSerif", Font.BOLD, 12));
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
        lblTotalStokBatch.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTotalStokBatch.setForeground(TEXT_DARK);
        batchFooter.add(lblTotalStokBatch, BorderLayout.WEST);

        btnHapusBatch.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnHapusBatch.setPreferredSize(new Dimension(112, 32));
        btnHapusBatch.setEnabled(false);
        batchFooter.add(btnHapusBatch, BorderLayout.EAST);

        panel.add(batchFooter, BorderLayout.SOUTH);
        return panel;
    }

    private void konfigurasiTabelProduk() {
        tableProduk.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableProduk.setRowHeight(42);
        tableProduk.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tableProduk.setForeground(TEXT_DARK);
        tableProduk.setGridColor(new Color(241, 245, 249));
        tableProduk.setSelectionBackground(PRIMARY_BLUE_SOFT);
        tableProduk.setSelectionForeground(TEXT_DARK);
        tableProduk.setShowVerticalLines(false);
        tableProduk.getTableHeader().setReorderingAllowed(false);

        tableProduk.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tableProduk.getTableHeader().setBackground(new Color(248, 250, 252));
        tableProduk.getTableHeader().setForeground(TEXT_MUTED);
        tableProduk.getTableHeader().setPreferredSize(new Dimension(0, 38));
        tableProduk.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer();
        textRenderer.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        DefaultTableCellRenderer kodeRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setFont(new Font("SansSerif", Font.BOLD, 11));
                setForeground(isSelected ? PRIMARY_BLUE : TEXT_SLATE);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 4));
                return this;
            }
        };

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        rightRenderer.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 6));

        DefaultTableCellRenderer jenisRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                String jenis = value != null ? value.toString() : "";
                JLabel badge = new JLabel(formatNamaJenis(jenis), SwingConstants.CENTER);
                badge.setFont(new Font("SansSerif", Font.BOLD, 10));
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
                        badge.setBackground(new Color(241, 245, 249));
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
                    setFont(new Font("SansSerif", Font.BOLD, 12));
                } else if (stok <= 5) {
                    setText(stok + " (Tipis)");
                    setForeground(BADGE_MAKANAN_FG);
                    setFont(new Font("SansSerif", Font.BOLD, 12));
                } else {
                    setText(String.valueOf(stok));
                    setForeground(TEXT_DARK);
                    setFont(new Font("SansSerif", Font.BOLD, 12));
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
        tableBatch.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tableBatch.setForeground(TEXT_DARK);
        tableBatch.setGridColor(new Color(241, 245, 249));
        tableBatch.setSelectionBackground(PRIMARY_BLUE_SOFT);
        tableBatch.setSelectionForeground(TEXT_DARK);
        tableBatch.setShowVerticalLines(false);
        tableBatch.getTableHeader().setReorderingAllowed(false);

        tableBatch.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tableBatch.getTableHeader().setBackground(new Color(248, 250, 252));
        tableBatch.getTableHeader().setForeground(TEXT_MUTED);
        tableBatch.getTableHeader().setPreferredSize(new Dimension(0, 32));
        tableBatch.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        DefaultTableCellRenderer cellPad = new DefaultTableCellRenderer();
        cellPad.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        DefaultTableCellRenderer stokCell = new DefaultTableCellRenderer();
        stokCell.setHorizontalAlignment(SwingConstants.CENTER);
        stokCell.setFont(new Font("SansSerif", Font.BOLD, 12));

        DefaultTableCellRenderer statusCell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                String status = value != null ? value.toString() : "";
                setFont(new Font("SansSerif", Font.BOLD, 11));
                switch (status) {
                    case "Kedaluwarsa" -> setForeground(DANGER_RED);
                    case "Hampir kedaluwarsa" -> setForeground(BADGE_MAKANAN_FG);
                    case "Aman" -> setForeground(new Color(22, 163, 74));
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

        cmbJenis.addActionListener(e -> {
            String jenis = (String) cmbJenis.getSelectedItem();
            if (jenis != null) {
                dynamicFieldLayout.show(dynamicFieldPanel, jenis);
            }
            perbaruiEstimasiPajak();
        });

        DocumentListener taxUpdateListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                perbaruiEstimasiPajak();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                perbaruiEstimasiPajak();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                perbaruiEstimasiPajak();
            }
        };
        txtHarga.getDocument().addDocumentListener(taxUpdateListener);
        chkDingin.addActionListener(e -> perbaruiEstimasiPajak());
        spnBulanGaransi.addChangeListener(e -> perbaruiEstimasiPajak());

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
            tableProduk.clearSelection();
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
            muatDataTabel(keepId);
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
    private void jalankanPencarian() {
        String keyword = txtCari.getText().trim();
        String filterJenis = (String) cmbFilterJenis.getSelectedItem();

        try {
            List<Produk> hasil;
            if (filterJenis == null || "SEMUA".equals(filterJenis)) {
                hasil = produkController.cariProduk(keyword);
            } else {
                hasil = produkController.cariProduk(keyword, filterJenis);
            }
            Integer selectedId = (produkTerpilih != null) ? produkTerpilih.getId() : null;
            tampilkanDaftarProduk(hasil, selectedId);
        } catch (IllegalArgumentException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Validasi", ex.getMessage());
        } catch (SQLException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Database", ProdukController.pesanSqlError(ex));
        }
    }

    private void muatDataTabel(Integer selectProductId) {
        try {
            List<Produk> list = produkController.daftarProduk();
            tampilkanDaftarProduk(list, selectProductId);
        } catch (SQLException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Database", ProdukController.pesanSqlError(ex));
        }
    }

    private void tampilkanDaftarProduk(List<Produk> list, Integer selectProductId) {
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
                        formatRupiah(p.getHarga()),
                        formatRupiah(p.hitungPajak()),
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
                masukModeUbah(daftarProdukSaatIni.get(indexToSelect));
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
        lblTotalStokBatch.setText("Total Stok: 0");

        pilihTab("DETAIL");
        perbaruiEstimasiPajak();
    }

    private void masukModeUbah(Produk p) {
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

        btnTabBatch.setText("Batch Stok (" + daftarBatchSaatIni.size() + ")");
        lblBatchInfoProduk.setText(p.getKode() + " \u2014 " + p.getNama());
        lblTotalStokBatch.setText("Total Stok: " + p.getStok() + " unit");
        btnHapusBatch.setEnabled(false);
    }

    private void simpanProduk() {
        try {
            int id = (produkTerpilih != null) ? produkTerpilih.getId() : 0;
            Produk produkBaru = bangunProdukDariForm(id);

            if (produkTerpilih == null) {
                produkController.tambahProduk(produkBaru);
                muatDataTabel(produkBaru.getId());
                if (produkTerpilih != null) {
                    pilihTab("BATCH");
                }
                toastManager.show(
                        "Produk Ditambahkan",
                        "Silakan tambahkan batch stok awal pada tab Batch Stok.",
                        ToastType.SUCCESS
                );
            } else {
                produkController.ubahProduk(produkBaru);
                muatDataTabel(produkBaru.getId());
                toastManager.show(
                        "Produk Diperbarui",
                        "Data produk berhasil disimpan.",
                        ToastType.SUCCESS
                );
            }
        } catch (IllegalArgumentException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Validasi", ex.getMessage());
        } catch (SQLException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Database", ProdukController.pesanSqlError(ex));
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

        boolean disetujui = UiStyleSupport.showConfirmModal(
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
            produkController.hapusProduk(produkTerpilih.getId());
            tutupSidePanel();
            muatDataTabel(null);
            toastManager.show(
                    "Produk Dihapus",
                    "Produk '" + namaDihapus + "' berhasil dihapus.",
                    ToastType.SUCCESS
            );
        } catch (IllegalArgumentException ex) {
            UiStyleSupport.showErrorModal(this, "Aksi Ditolak", ex.getMessage());
        } catch (SQLException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Database", ProdukController.pesanSqlError(ex));
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
            muatDataTabel(produkTerpilih.getId());
            pilihTab("BATCH");
            toastManager.show(
                    "Batch Ditambahkan",
                    "Stok masuk sebanyak " + stokBatch + " unit berhasil dicatat.",
                    ToastType.SUCCESS
            );
        } catch (IllegalArgumentException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Validasi", ex.getMessage());
        } catch (SQLException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Database", ProdukController.pesanSqlError(ex));
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
        boolean disetujui = UiStyleSupport.showConfirmModal(
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
            muatDataTabel(produkTerpilih.getId());
            pilihTab("BATCH");
            toastManager.show("Batch Dihapus", "Batch stok berhasil dihapus.", ToastType.SUCCESS);
        } catch (SQLException ex) {
            UiStyleSupport.showErrorModal(this, "Kesalahan Database", ProdukController.pesanSqlError(ex));
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
                int garansi = Math.max(0, ((Number) spnBulanGaransi.getValue()).intValue());
                sampel = new Elektronik(0, "TMP", "Sampel", harga, garansi);
            }
            lblEstimasiPajak.setText("Estimasi Pajak: " + formatRupiah(sampel.hitungPajak()) + " / unit");
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
        btn.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 13));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0, PRIMARY_BLUE),
                BorderFactory.createEmptyBorder(6, 2, 8, 2)
        ));
    }

    private static double parseHarga(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Harga produk tidak boleh kosong.");
        }
        String cleaned = raw.trim().replace(",", ".");
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Harga produk harus berupa angka yang valid.");
        }
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

    private static void styleNumericSpinner(JSpinner spinner) {
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#");
        spinner.setEditor(editor);
        spinner.setFont(new Font("SansSerif", Font.PLAIN, 12));
        spinner.setPreferredSize(new Dimension(0, 30));
        spinner.setBorder(new RoundedBorder(INPUT_BORDER, 8, new Insets(1, 4, 1, 4)));
    }

    private static void styleInputField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 12));
        field.setPreferredSize(new Dimension(0, 32));
        field.setBorder(new RoundedBorder(INPUT_BORDER, 8, new Insets(4, 10, 4, 10)));
    }

    private static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(TEXT_SLATE);
        return label;
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
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXT_DARK);
        combo.setOpaque(false);
        combo.setUI(new RoundedComboBoxUI(radius));
        combo.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 4));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combo.setRenderer(new JenisProdukRenderer(includeSemua));
    }

    /**
     * UI kustom untuk JComboBox agar sudut membulat, chevron rapi, dan bebas garis fokus kotak Win32.
     */
    private static class RoundedComboBoxUI extends BasicComboBoxUI {
        private final int radius;
        private boolean hovered = false;

        RoundedComboBoxUI(int radius) {
            this.radius = radius;
        }

        @Override
        protected void installListeners() {
            super.installListeners();
            comboBox.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    comboBox.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    comboBox.repaint();
                }
            });
        }

        @Override
        protected JButton createArrowButton() {
            JButton btn = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    boolean focused = comboBox != null && (comboBox.hasFocus() || (popup != null && popup.isVisible()));
                    g2.setColor(focused ? PRIMARY_BLUE : TEXT_MUTED);
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawPolyline(
                            new int[]{cx - 4, cx, cx + 4},
                            new int[]{cy - 2, cy + 2, cy - 2},
                            3
                    );
                    g2.dispose();
                }
            };
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.setPreferredSize(new Dimension(24, 24));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return btn;
        }

        @Override
        protected ComboPopup createPopup() {
            BasicComboPopup comboPopup = (BasicComboPopup) super.createPopup();
            comboPopup.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
            return comboPopup;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = c.getWidth();
            int h = c.getHeight();

            g2.setColor(c.isEnabled() ? Color.WHITE : new Color(248, 250, 252));
            g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);
            g2.dispose();

            super.paint(g, c);

            Graphics2D gBorder = (Graphics2D) g.create();
            gBorder.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean active = c.hasFocus() || (popup != null && popup.isVisible());
            gBorder.setColor(active ? PRIMARY_BLUE : (hovered ? new Color(148, 163, 184) : INPUT_BORDER));
            gBorder.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            gBorder.dispose();
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            // Tidak menggambar latar kotak fokus default agar tampilan melengkung tetap bersih
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            Component c = comboBox.getRenderer().getListCellRendererComponent(
                    listBox, comboBox.getSelectedItem(), -1, false, false);
            c.setFont(comboBox.getFont());
            c.setForeground(comboBox.isEnabled() ? TEXT_DARK : TEXT_MUTED);
            c.setBackground(Color.WHITE);
            boolean shouldValidate = c instanceof JPanel;
            currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, shouldValidate);
        }
    }

    private static JTextField createPlaceholderField(String placeholder) {
        return new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(148, 163, 184));
                    g2.setFont(getFont());
                    Insets insets = getInsets();
                    int y = (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
                    g2.drawString(placeholder, insets.left, y);
                    g2.dispose();
                }
            }
        };
    }

    private static String formatRupiah(double nilai) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"));
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(2);
        return "Rp " + nf.format(nilai);
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
            setFont(new Font("SansSerif", (index >= 0 && isSelected) ? Font.BOLD : Font.PLAIN, 12));
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

    /**
     * Komponen pemilih tanggal modern dengan ikon kalender dan popup kalender bulanan interaktif.
     */
    private static class ModernDatePicker extends JPanel {
        private LocalDate selectedDate;
        private final boolean showExpiryPresets;
        private boolean hovered = false;
        private final JPopupMenu popup = new JPopupMenu();
        private final CalendarPopupPanel calendarPanel;

        ModernDatePicker(LocalDate initialDate, boolean showExpiryPresets) {
            this.selectedDate = (initialDate != null) ? initialDate : LocalDate.now();
            this.showExpiryPresets = showExpiryPresets;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 34));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            calendarPanel = new CalendarPopupPanel();
            popup.setBorder(new RoundedBorder(BORDER_COLOR, 12, new Insets(8, 8, 8, 8)));
            popup.setBackground(CARD_BG);
            popup.add(calendarPanel);
            popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                    repaint();
                }

                @Override
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                    repaint();
                }

                @Override
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                    repaint();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (!isEnabled()) {
                        return;
                    }
                    if (popup.isVisible()) {
                        popup.setVisible(false);
                    } else {
                        calendarPanel.tampilkanBulan(selectedDate);
                        int popupW = calendarPanel.getPreferredSize().width + 16;
                        int x = Math.min(0, getWidth() - popupW);
                        popup.show(ModernDatePicker.this, x, getHeight() + 4);
                    }
                }
            });
        }

        LocalDate getDate() {
            return selectedDate;
        }

        void setDate(LocalDate date) {
            if (date != null) {
                this.selectedDate = date;
                repaint();
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (!enabled && popup.isVisible()) {
                popup.setVisible(false);
            }
            setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            boolean enabled = isEnabled();
            boolean active = popup.isVisible();

            g2.setColor(enabled ? Color.WHITE : new Color(248, 250, 252));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);

            Color border = !enabled ? BORDER_COLOR
                    : (active ? PRIMARY_BLUE : (hovered ? new Color(148, 163, 184) : INPUT_BORDER));
            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

            // Teks tanggal
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(enabled ? TEXT_DARK : TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();
            String text = DATE_DISPLAY_FMT.format(selectedDate);
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, 10, ty);

            // Ikon kalender Java2D di kanan
            int ix = w - 24;
            int iy = (h - 14) / 2;
            g2.setColor(enabled ? (active ? PRIMARY_BLUE : TEXT_MUTED) : new Color(148, 163, 184));
            g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(ix, iy + 2, 13, 11, 3, 3);
            g2.drawLine(ix, iy + 6, ix + 13, iy + 6);
            g2.drawLine(ix + 4, iy, ix + 4, iy + 3);
            g2.drawLine(ix + 9, iy, ix + 9, iy + 3);

            g2.dispose();
        }

        private class CalendarPopupPanel extends JPanel {
            private YearMonth currentMonth = YearMonth.from(selectedDate);
            private final JLabel lblMonthYear = new JLabel("", SwingConstants.CENTER);
            private final JPanel daysGrid = new JPanel(new GridLayout(6, 7, 3, 3));

            CalendarPopupPanel() {
                setLayout(new BorderLayout(0, 8));
                setBackground(CARD_BG);
                setPreferredSize(new Dimension(264, showExpiryPresets ? 292 : 264));

                // Header navigasi bulan & tahun
                JPanel navBar = new JPanel(new BorderLayout(4, 0));
                navBar.setOpaque(false);

                JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
                leftNav.setOpaque(false);
                RoundedButton btnPrevYear = createNavBtn("\u00AB", "Tahun sebelumnya");
                RoundedButton btnPrevMonth = createNavBtn("\u2039", "Bulan sebelumnya");
                btnPrevYear.addActionListener(e -> gantiBulan(currentMonth.minusYears(1)));
                btnPrevMonth.addActionListener(e -> gantiBulan(currentMonth.minusMonths(1)));
                leftNav.add(btnPrevYear);
                leftNav.add(btnPrevMonth);

                JPanel rightNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
                rightNav.setOpaque(false);
                RoundedButton btnNextMonth = createNavBtn("\u203A", "Bulan berikutnya");
                RoundedButton btnNextYear = createNavBtn("\u00BB", "Tahun berikutnya");
                btnNextMonth.addActionListener(e -> gantiBulan(currentMonth.plusMonths(1)));
                btnNextYear.addActionListener(e -> gantiBulan(currentMonth.plusYears(1)));
                rightNav.add(btnNextMonth);
                rightNav.add(btnNextYear);

                lblMonthYear.setFont(new Font("SansSerif", Font.BOLD, 13));
                lblMonthYear.setForeground(TEXT_DARK);

                navBar.add(leftNav, BorderLayout.WEST);
                navBar.add(lblMonthYear, BorderLayout.CENTER);
                navBar.add(rightNav, BorderLayout.EAST);

                // Baris nama hari (Sen - Min)
                JPanel weekHeader = new JPanel(new GridLayout(1, 7, 3, 0));
                weekHeader.setOpaque(false);
                String[] hari = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
                for (String hName : hari) {
                    JLabel lblDay = new JLabel(hName, SwingConstants.CENTER);
                    lblDay.setFont(new Font("SansSerif", Font.BOLD, 11));
                    lblDay.setForeground(TEXT_MUTED);
                    weekHeader.add(lblDay);
                }

                JPanel northWrap = new JPanel(new BorderLayout(0, 8));
                northWrap.setOpaque(false);
                northWrap.add(navBar, BorderLayout.NORTH);
                northWrap.add(weekHeader, BorderLayout.SOUTH);
                add(northWrap, BorderLayout.NORTH);

                daysGrid.setOpaque(false);
                add(daysGrid, BorderLayout.CENTER);

                // Footer shortcut cepat
                JPanel quickBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
                quickBar.setOpaque(false);
                quickBar.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                        BorderFactory.createEmptyBorder(8, 0, 0, 0)
                ));

                quickBar.add(createPresetBtn("Hari Ini", LocalDate::now));
                if (showExpiryPresets) {
                    quickBar.add(createPresetBtn("+1 Bln", () -> LocalDate.now().plusMonths(1)));
                    quickBar.add(createPresetBtn("+3 Bln", () -> LocalDate.now().plusMonths(3)));
                    quickBar.add(createPresetBtn("+6 Bln", () -> LocalDate.now().plusMonths(6)));
                    quickBar.add(createPresetBtn("+1 Thn", () -> LocalDate.now().plusYears(1)));
                }
                add(quickBar, BorderLayout.SOUTH);

                renderGrid();
            }

            void tampilkanBulan(LocalDate target) {
                currentMonth = YearMonth.from(target != null ? target : LocalDate.now());
                renderGrid();
            }

            private void gantiBulan(YearMonth target) {
                currentMonth = target;
                renderGrid();
            }

            private void renderGrid() {
                String namaBulan = currentMonth.getMonth().getDisplayName(TextStyle.FULL, ID_LOCALE);
                String capitalized = namaBulan.substring(0, 1).toUpperCase(ID_LOCALE) + namaBulan.substring(1);
                lblMonthYear.setText(capitalized + " " + currentMonth.getYear());

                daysGrid.removeAll();
                LocalDate firstOfMonth = currentMonth.atDay(1);
                int offset = firstOfMonth.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
                LocalDate cellDate = firstOfMonth.minusDays(offset);
                LocalDate today = LocalDate.now();

                for (int i = 0; i < 42; i++) {
                    LocalDate d = cellDate.plusDays(i);
                    boolean inCurrentMonth = d.getMonth() == currentMonth.getMonth();
                    boolean isSelected = d.equals(selectedDate);
                    boolean isToday = d.equals(today);

                    daysGrid.add(new DayCellButton(d, inCurrentMonth, isSelected, isToday));
                }
                daysGrid.revalidate();
                daysGrid.repaint();
            }

            private RoundedButton createNavBtn(String text, String tip) {
                RoundedButton btn = new RoundedButton(text, ButtonVariant.GHOST, 8);
                btn.setFont(new Font("SansSerif", Font.BOLD, 14));
                btn.setPreferredSize(new Dimension(26, 26));
                btn.setToolTipText(tip);
                return btn;
            }

            private RoundedButton createPresetBtn(String label, java.util.function.Supplier<LocalDate> dateSupplier) {
                RoundedButton btn = new RoundedButton(label, ButtonVariant.OUTLINE, 8);
                btn.setFont(new Font("SansSerif", Font.BOLD, 10));
                btn.setPreferredSize(new Dimension(showExpiryPresets ? 48 : 80, 24));
                btn.addActionListener(e -> {
                    setDate(dateSupplier.get());
                    popup.setVisible(false);
                });
                return btn;
            }

            private class DayCellButton extends JButton {
                private final boolean inCurrentMonth;
                private final boolean isSelected;
                private final boolean isToday;
                private boolean cellHovered = false;

                DayCellButton(LocalDate date, boolean inCurrentMonth, boolean isSelected, boolean isToday) {
                    super(String.valueOf(date.getDayOfMonth()));
                    this.inCurrentMonth = inCurrentMonth;
                    this.isSelected = isSelected;
                    this.isToday = isToday;
                    setContentAreaFilled(false);
                    setBorderPainted(false);
                    setFocusPainted(false);
                    setOpaque(false);
                    setFont(new Font("SansSerif", isSelected || isToday ? Font.BOLD : Font.PLAIN, 12));
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            cellHovered = true;
                            repaint();
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            cellHovered = false;
                            repaint();
                        }
                    });

                    addActionListener(e -> {
                        setDate(date);
                        popup.setVisible(false);
                    });
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();

                    if (isSelected) {
                        g2.setColor(PRIMARY_BLUE);
                        g2.fillRoundRect(1, 1, w - 3, h - 3, 8, 8);
                    } else if (cellHovered) {
                        g2.setColor(PRIMARY_BLUE_SOFT);
                        g2.fillRoundRect(1, 1, w - 3, h - 3, 8, 8);
                    }

                    if (isToday && !isSelected) {
                        g2.setColor(PRIMARY_BLUE);
                        g2.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);
                    }

                    Color fg = isSelected ? Color.WHITE
                            : (isToday ? PRIMARY_BLUE
                            : (inCurrentMonth ? TEXT_DARK : new Color(186, 196, 209)));
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    String txt = getText();
                    int tx = (w - fm.stringWidth(txt)) / 2;
                    int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                    g2.setColor(fg);
                    g2.drawString(txt, tx, ty);

                    g2.dispose();
                }
            }
        }
    }
}
