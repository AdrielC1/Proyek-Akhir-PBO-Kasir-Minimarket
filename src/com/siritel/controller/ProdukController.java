package com.siritel.controller;

import com.siritel.dao.ProdukDAO;
import com.siritel.model.BatchProduk;
import com.siritel.model.Produk;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller untuk mengelola katalog produk, pencarian (method overloading), dan batch stok.
 */
public class ProdukController {
    private final ProdukDAO produkDAO;

    public ProdukController() {
        this.produkDAO = new ProdukDAO();
    }

    public ProdukController(ProdukDAO produkDAO) {
        if (produkDAO == null) {
            throw new IllegalArgumentException("ProdukDAO tidak boleh null.");
        }
        this.produkDAO = produkDAO;
    }

    public List<Produk> daftarProduk() throws SQLException {
        return produkDAO.getAll();
    }

    public Produk cariProdukById(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID produk harus lebih dari 0.");
        }
        return produkDAO.getById(id);
    }

    public void tambahProduk(Produk p) throws SQLException {
        if (p == null) {
            throw new IllegalArgumentException("Data produk tidak boleh null.");
        }
        produkDAO.insert(p);
    }

    public void ubahProduk(Produk p) throws SQLException {
        if (p == null) {
            throw new IllegalArgumentException("Data produk tidak boleh null.");
        }
        if (p.getId() <= 0) {
            throw new IllegalArgumentException("ID produk tidak valid untuk pembaruan.");
        }
        produkDAO.update(p);
    }

    public void hapusProduk(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID produk tidak valid.");
        }
        produkDAO.delete(id);
    }

    /**
     * Overloading 1: Pencarian produk berdasarkan kata kunci nama atau kode pada semua kategori.
     */
    public List<Produk> cariProduk(String nama) throws SQLException {
        String keyword = (nama == null) ? "" : nama.trim();
        return produkDAO.search(keyword, null);
    }

    /**
     * Overloading 2: Pencarian produk berdasarkan kata kunci nama/kode serta filter jenis produk.
     */
    public List<Produk> cariProduk(String nama, String jenis) throws SQLException {
        String keyword = (nama == null) ? "" : nama.trim();
        String jenisNormal = normalisasiJenis(jenis);
        return produkDAO.search(keyword, jenisNormal);
    }

    public void tambahStokBatch(BatchProduk batch) throws SQLException {
        if (batch == null) {
            throw new IllegalArgumentException("Data batch stok tidak boleh null.");
        }
        if (batch.getProdukId() <= 0) {
            throw new IllegalArgumentException("Produk harus dipilih sebelum menambah batch stok.");
        }
        if (batch.getStokBatch() <= 0) {
            throw new IllegalArgumentException("Jumlah stok masuk batch harus lebih dari 0.");
        }

        Produk produk = produkDAO.getById(batch.getProdukId());
        if (produk == null) {
            throw new IllegalArgumentException("Produk dengan ID " + batch.getProdukId() + " tidak ditemukan.");
        }

        if ("ELEKTRONIK".equals(produk.getJenisProduk())) {
            batch.setTglKadaluarsa(null);
        } else if (batch.getTglKadaluarsa() == null) {
            throw new IllegalArgumentException("Tanggal kedaluwarsa wajib diisi untuk produk Makanan dan Minuman.");
        }

        produkDAO.tambahBatch(batch);
    }

    public List<BatchProduk> daftarBatchProduk(int produkId) throws SQLException {
        if (produkId <= 0) {
            throw new IllegalArgumentException("ID produk tidak valid.");
        }
        return produkDAO.getBatchByProdukId(produkId);
    }

    public void hapusBatch(int batchId) throws SQLException {
        if (batchId <= 0) {
            throw new IllegalArgumentException("ID batch tidak valid.");
        }
        produkDAO.deleteBatch(batchId);
    }

    private String normalisasiJenis(String jenis) {
        if (jenis == null || jenis.isBlank()
                || "SEMUA".equalsIgnoreCase(jenis.trim())
                || "Semua Jenis".equalsIgnoreCase(jenis.trim())) {
            return null;
        }
        String upper = jenis.trim().toUpperCase();
        if (!upper.equals("MAKANAN") && !upper.equals("MINUMAN") && !upper.equals("ELEKTRONIK")) {
            throw new IllegalArgumentException("Jenis produk harus MAKANAN, MINUMAN, atau ELEKTRONIK.");
        }
        return upper;
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
                return "Kode produk sudah digunakan. Gunakan kode produk lain.";
            }
            if ("23503".equals(state)) {
                return "Produk tidak dapat dihapus karena sudah tercatat dalam riwayat transaksi.";
            }
            if ("23514".equals(state)) {
                return "Data produk melanggar batasan database (periksa harga, stok, atau atribut jenis).";
            }
            if (state.startsWith("08")) {
                return "Tidak dapat terhubung ke database. Pastikan PostgreSQL berjalan dan konfigurasi koneksi benar.";
            }
            if (state.startsWith("28")) {
                return "Autentikasi database gagal. Periksa konfigurasi di DatabaseConnection.";
            }
        }
        return "Terjadi kesalahan database: " + e.getMessage();
    }
}
