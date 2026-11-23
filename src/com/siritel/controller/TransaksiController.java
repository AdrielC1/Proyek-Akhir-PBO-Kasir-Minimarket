
package com.siritel.controller;

import com.siritel.dao.TransaksiDAO;
import com.siritel.exception.StokTidakCukupException;
import com.siritel.model.LaporanPenjualan;
import com.siritel.model.Pengguna;
import com.siritel.model.Produk;
import com.siritel.model.Transaksi;
import com.siritel.model.TransaksiRingkas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TransaksiController {

    private static final DateTimeFormatter ID_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TransaksiDAO transaksiDAO;

    // Constructor default
    public TransaksiController() {
        this.transaksiDAO = new TransaksiDAO();
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

    // Membuat objek transaksi baru (ID dan waktu final ditetapkan saat checkout)
    public Transaksi buatTransaksi(Pengguna kasir) {

        if (kasir == null) {
            throw new IllegalArgumentException(
                "Kasir tidak boleh null."
            );
        }

        LocalDateTime sekarang = LocalDateTime.now();
        return new Transaksi(generateIdTransaksi(kasir, sekarang), kasir);
    }

    // Memproses checkout transaksi dengan ID unik dan waktu real-time saat bayar
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

        BigDecimal bayarBd = BigDecimal.valueOf(transaksi.getBayar()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBd = BigDecimal.valueOf(transaksi.getGrandTotal()).setScale(2, RoundingMode.HALF_UP);
        if (bayarBd.compareTo(totalBd) < 0) {
            throw new IllegalArgumentException(
                "Pembayaran belum mencukupi."
            );
        }

        LocalDateTime waktuCheckout = LocalDateTime.now();
        transaksi.siapkanCheckout(
            generateIdTransaksi(transaksi.getKasir(), waktuCheckout),
            waktuCheckout
        );

        // Menyerahkan proses penyimpanan kepada DAO
        transaksiDAO.simpanTransaksi(transaksi);
    }

    /**
     * Mengambil riwayat transaksi berdasarkan rentang tanggal dan kata kunci pencarian.
     */
    public List<TransaksiRingkas> getRiwayatTransaksi(
            LocalDate tanggalAwal,
            LocalDate tanggalAkhir,
            String keyword,
            int halaman,
            int batas) throws SQLException {

        return transaksiDAO.getRiwayatTransaksi(
            tanggalAwal,
            tanggalAkhir,
            keyword,
            halaman,
            batas
        );
    }

    /**
     * Menghitung jumlah transaksi berdasarkan rentang tanggal dan kata kunci pencarian.
     */
    public int getJumlahRiwayatTransaksi(
            LocalDate tanggalAwal,
            LocalDate tanggalAkhir,
            String keyword) throws SQLException {

        return transaksiDAO.getJumlahRiwayatTransaksi(
            tanggalAwal,
            tanggalAkhir,
            keyword
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

    /**
     * Memetakan kode SQLState PostgreSQL ke pesan berbahasa Indonesia yang ramah pengguna.
     */
    public static String pesanSqlError(SQLException e) {
        if (e == null) {
            return "Terjadi kesalahan database tidak diketahui.";
        }
        String state = e.getSQLState();
        if (state != null) {
            if ("23505".equals(state)) {
                return "ID transaksi terdeteksi duplikat. Silakan ulangi proses checkout.";
            }
            if ("23503".equals(state)) {
                return "Data produk atau kasir yang direferensikan transaksi sudah tidak tersedia.";
            }
            if ("23514".equals(state)) {
                return "Data transaksi melanggar batasan nilai database (periksa kuantitas atau nominal).";
            }
            if ("22003".equals(state)) {
                return "Total nominal transaksi melebihi batas kapasitas database.";
            }
            if (state.startsWith("08")) {
                return "Tidak dapat terhubung ke database. Periksa koneksi jaringan dan konfigurasi PostgreSQL.";
            }
            if (state.startsWith("28")) {
                return "Autentikasi database gagal. Periksa konfigurasi di db.properties atau environment variable.";
            }
        }
        return "Terjadi kesalahan database: " + e.getMessage();
    }

    private String generateIdTransaksi(Pengguna kasir, LocalDateTime waktu) {
        int acak = ThreadLocalRandom.current().nextInt(1000);
        return "TRX-" + ID_TIME_FMT.format(waktu) + "-" + kasir.getId() + String.format("%03d", acak);
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
