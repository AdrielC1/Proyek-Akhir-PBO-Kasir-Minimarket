package com.siritel.view.ui;

/**
 * Kontrak halaman SI-RITEL yang mendukung pemuatan ulang data dari database
 * saat pengguna berpindah tab di {@code MainFrame} maupun menekan tombol Segarkan.
 */
public interface RefreshableView {
    void refreshData();
}
