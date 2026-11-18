
package com.siritel.controller;

import com.siritel.dao.TransaksiDAO;
import com.siritel.exception.StokTidakCukupException;
import com.siritel.model.Transaksi;
import com.siritel.model.Pengguna;
import com.siritel.model.Produk;
import com.siritel.model.TransaksiRingkas;
import com.siritel.model.LaporanPenjualan;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.time.format.DateTimeFormatter;

public class TransaksiController {

    private final TransaksiDAO transaksiDAO;

    // Constructor default
    public TransaksiController() {
        this.transaksiDAO = new TransaksiDAO();
    }

    // Constructor untuk dependency injection
    public TransaksiController(TransaksiDAO transaksiDAO) {

        if (transaksiDAO == null) {
            throw new IllegalArgumentException(
                "TransaksiDAO tidak boleh null."
            );
        }

        this.transaksiDAO = transaksiDAO;
    }


    // Menambahkan produk ke keranjang
    public void tambahItem(
            Transaksi transaksi,
            Produk produk,
            int qty
    ) throws StokTidakCukupException {

        validasiTransaksi(transaksi);
        transaksi.tambahItem(produk, qty);
    }

    // Mengubah kuantitas produk
    public void ubahKuantitas(
            Transaksi transaksi,
            Produk produk,
            int qty
    ) throws StokTidakCukupException {

        validasiTransaksi(transaksi);
        transaksi.ubahKuantitas(produk, qty);
    }

    // Menghapus produk dari keranjang
    public void hapusItem(
            Transaksi transaksi,
            Produk produk
    ) {

        validasiTransaksi(transaksi);
        transaksi.hapusItem(produk);
    }

    // Mengosongkan keranjang
    public void bersihkanKeranjang(Transaksi transaksi) {

        validasiTransaksi(transaksi);
        transaksi.bersihkanKeranjang();
    }

    // Memproses pembayaran
    public void prosesPembayaran(
            Transaksi transaksi,
            double nominal
    ) {

        validasiTransaksi(transaksi);
        transaksi.prosesBayar(nominal);
    }


    // Membuat objek transaksi dengan ID otomatis
    public Transaksi buatTransaksi(Pengguna kasir) {

        if (kasir == null) {
            throw new IllegalArgumentException(
                "Kasir tidak boleh null."
            );
        }

        String idTransaksi = "TRX-" +
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(LocalDateTime.now());

        return new Transaksi(idTransaksi, kasir);
    }

    // Memproses checkout transaksi
    public void checkout(Transaksi transaksi)
            throws SQLException, StokTidakCukupException {

        if (transaksi == null) {
            throw new IllegalArgumentException(
                "Transaksi tidak boleh null."
            );
        }

        if (transaksi.getListKeranjang().isEmpty()) {
            throw new IllegalArgumentException(
                "Keranjang transaksi masih kosong."
            );
        }

        if (transaksi.getBayar() < transaksi.getGrandTotal()) {
            throw new IllegalArgumentException(
                "Pembayaran belum mencukupi."
            );
        }


        
        // Menyerahkan proses penyimpanan kepada DAO
        transaksiDAO.simpanTransaksi(transaksi);

    }
    /**
     * Mengambil riwayat transaksi berdasarkan rentang tanggal.
     */
    public List<TransaksiRingkas> getRiwayatTransaksi(
        LocalDate tanggalAwal,
        LocalDate tanggalAkhir,
        int halaman,
        int batas) throws SQLException {

    return transaksiDAO.getRiwayatTransaksi(
        tanggalAwal,
        tanggalAkhir,
        halaman,
        batas
    );
}

/**
 * Menghitung jumlah transaksi berdasarkan rentang tanggal.
 */
public int getJumlahRiwayatTransaksi(
        LocalDate tanggalAwal,
        LocalDate tanggalAkhir) throws SQLException {

    return transaksiDAO.getJumlahRiwayatTransaksi(
        tanggalAwal,
        tanggalAkhir
    );
}

    /**
     * Mengambil detail transaksi berdasarkan ID transaksi.
     */
    public Transaksi getDetailTransaksi(
            String idTransaksi) throws SQLException {

        if (idTransaksi == null || idTransaksi.isBlank()) {
            throw new IllegalArgumentException(
                "ID transaksi tidak boleh kosong."
            );
        }

        return transaksiDAO.getDetailTransaksi(idTransaksi);
    }


    /**
 * Membuat laporan penjualan berdasarkan rentang tanggal.
 */
public LaporanPenjualan buatLaporan(
        LocalDate tanggalAwal,
        LocalDate tanggalAkhir) throws SQLException {

    if (tanggalAwal == null || tanggalAkhir == null) {
        throw new IllegalArgumentException(
            "Rentang tanggal laporan tidak boleh null."
        );
    }

    if (tanggalAwal.isAfter(tanggalAkhir)) {
        throw new IllegalArgumentException(
            "Tanggal awal tidak boleh melebihi tanggal akhir."
        );
    }

    return transaksiDAO.getLaporanPenjualan(
        tanggalAwal,
        tanggalAkhir
    );
}
    // Validasi objek transaksi
    private void validasiTransaksi(Transaksi transaksi) {

        if (transaksi == null) {
            throw new IllegalArgumentException(
                "Transaksi tidak boleh null."
            );
        }
    }
}
