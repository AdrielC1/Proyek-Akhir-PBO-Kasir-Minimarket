package com.siritel.view;

import javax.swing.JPanel;

/**
 * Halaman Manajemen Produk (placeholder kosong untuk katalog produk dan batch stok).
 */
public class ManajemenProdukView extends JPanel {
    public ManajemenProdukView() {
        PlaceholderSupport.initEmptyPage(
                this,
                "Halaman Manajemen Produk",
                "Halaman kosong — siap diisi CRUD produk (Makanan, Minuman, Elektronik) dan batch stok."
        );
    }
}
