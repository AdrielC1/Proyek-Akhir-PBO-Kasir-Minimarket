package com.siritel.view;

import javax.swing.JPanel;

/**
 * Halaman Riwayat Transaksi (placeholder kosong untuk daftar transaksi & detail nota).
 */
public class RiwayatTransaksiView extends JPanel {
    public RiwayatTransaksiView() {
        PlaceholderSupport.initEmptyPage(
                this,
                "Halaman Riwayat Transaksi",
                "Halaman kosong — siap diisi daftar histori transaksi dan cetak ulang struk."
        );
    }
}
