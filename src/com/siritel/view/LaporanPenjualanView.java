package com.siritel.view;

import javax.swing.JPanel;

/**
 * Halaman Laporan Penjualan (placeholder kosong untuk rekap omzet dan pajak per periode).
 */
public class LaporanPenjualanView extends JPanel {
    public LaporanPenjualanView() {
        PlaceholderSupport.initEmptyPage(
                this,
                "Halaman Laporan Penjualan",
                "Halaman kosong — siap diisi filter periode dan rekap laporan penjualan."
        );
    }
}
