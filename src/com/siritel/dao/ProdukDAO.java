package com.siritel.dao;

import com.siritel.config.DatabaseConnection;
import com.siritel.model.BatchProduk;
import com.siritel.model.Elektronik;
import com.siritel.model.Makanan;
import com.siritel.model.Minuman;
import com.siritel.model.Produk;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object untuk entitas Produk (Single Table Inheritance) dan BatchProduk (FEFO).
 * Satu-satunya tempat penulisan query SQL terkait katalog produk dan batch stok.
 */
public class ProdukDAO {

    public void insert(Produk p) throws SQLException {
        String sql = "INSERT INTO produk (kode, nama, harga, jenis_produk, berat_gram, volume_ml, is_dingin, bulan_garansi) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindProdukParameters(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p.setId(rs.getInt("id"));
                }
            }
        }
    }

    public void update(Produk p) throws SQLException {
        String sql = "UPDATE produk " +
                     "SET kode = ?, nama = ?, harga = ?, jenis_produk = ?, " +
                     "berat_gram = ?, volume_ml = ?, is_dingin = ?, bulan_garansi = ? " +
                     "WHERE id = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindProdukParameters(ps, p);
            ps.setInt(9, p.getId());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Produk dengan ID " + p.getId() + " tidak ditemukan atau sudah dihapus.");
            }
        }
    }

    /**
     * Menghapus produk dengan strategi hybrid soft-delete:
     * - Jika sudah tercatat pada item_transaksi, produk dinonaktifkan (is_active = FALSE)
     *   dan sisa batch stoknya dibersihkan agar histori struk tetap utuh.
     * - Jika belum pernah ditransaksikan, baris produk dihapus permanen.
     *
     * @return {@code true} jika produk diarsipkan (soft delete), {@code false} jika dihapus permanen.
     */
    public boolean delete(int id) throws SQLException {
        String sqlLock = "SELECT id FROM produk WHERE id = ? AND is_active = TRUE FOR UPDATE";
        String sqlCekTransaksi = "SELECT 1 FROM item_transaksi WHERE produk_id = ? LIMIT 1";
        String sqlSoftDelete = "UPDATE produk SET is_active = FALSE WHERE id = ? AND is_active = TRUE";
        String sqlDeleteBatches = "DELETE FROM batch_produk WHERE produk_id = ?";
        String sqlHardDelete = "DELETE FROM produk WHERE id = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psLock = conn.prepareStatement(sqlLock)) {
                    psLock.setInt(1, id);
                    try (ResultSet rs = psLock.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Produk tidak ditemukan atau sudah dihapus.");
                        }
                    }
                }

                boolean pernahTransaksi;
                try (PreparedStatement psCek = conn.prepareStatement(sqlCekTransaksi)) {
                    psCek.setInt(1, id);
                    try (ResultSet rs = psCek.executeQuery()) {
                        pernahTransaksi = rs.next();
                    }
                }

                if (pernahTransaksi) {
                    try (PreparedStatement psSoft = conn.prepareStatement(sqlSoftDelete)) {
                        psSoft.setInt(1, id);
                        psSoft.executeUpdate();
                    }
                    try (PreparedStatement psBatch = conn.prepareStatement(sqlDeleteBatches)) {
                        psBatch.setInt(1, id);
                        psBatch.executeUpdate();
                    }
                    conn.commit();
                    return true;
                }

                try (PreparedStatement psHard = conn.prepareStatement(sqlHardDelete)) {
                    psHard.setInt(1, id);
                    psHard.executeUpdate();
                }
                conn.commit();
                return false;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackErr) {
                    e.addSuppressed(rollbackErr);
                }
                throw e;
            }
        }
    }

    public List<Produk> getAll() throws SQLException {
        return search("", null);
    }

    public List<Produk> search(String keyword, String jenisProduk) throws SQLException {
        Map<Integer, Produk> produkById = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, kode, nama, harga, jenis_produk, berat_gram, volume_ml, is_dingin, bulan_garansi " +
                "FROM produk WHERE is_active = TRUE"
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (nama ILIKE ? OR kode ILIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (jenisProduk != null && !jenisProduk.isBlank()) {
            sql.append(" AND jenis_produk = ?");
            params.add(jenisProduk.trim().toUpperCase());
        }

        sql.append(" ORDER BY id ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Produk p = mapRowToProduk(rs);
                    produkById.put(p.getId(), p);
                }
            }

            if (!produkById.isEmpty()) {
                muatBatchUntukProduk(conn, produkById);
            }
        }

        return new ArrayList<>(produkById.values());
    }

    public Produk getById(int id) throws SQLException {
        String sql = "SELECT id, kode, nama, harga, jenis_produk, berat_gram, volume_ml, is_dingin, bulan_garansi " +
                     "FROM produk WHERE id = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Produk p = mapRowToProduk(rs);
                    p.setDaftarBatch(muatBatchByProdukId(conn, p.getId()));
                    return p;
                }
            }
        }
        return null;
    }

    public void tambahBatch(BatchProduk batch) throws SQLException {
        String sql = "INSERT INTO batch_produk (produk_id, tgl_masuk, tgl_kadaluarsa, stok_batch) " +
                     "VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batch.getProdukId());
            ps.setDate(2, Date.valueOf(batch.getTglMasuk()));
            if (batch.getTglKadaluarsa() != null) {
                ps.setDate(3, Date.valueOf(batch.getTglKadaluarsa()));
            } else {
                ps.setNull(3, Types.DATE);
            }
            ps.setInt(4, batch.getStokBatch());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    batch.setId(rs.getInt("id"));
                }
            }
        }
    }

    public void deleteBatch(int batchId) throws SQLException {
        String sql = "DELETE FROM batch_produk WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batchId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Batch stok tidak ditemukan atau sudah dihapus.");
            }
        }
    }

    private void muatBatchUntukProduk(Connection conn, Map<Integer, Produk> produkById) throws SQLException {
        Integer[] ids = produkById.keySet().toArray(new Integer[0]);
        String sql = "SELECT id, produk_id, tgl_masuk, tgl_kadaluarsa, stok_batch " +
                     "FROM batch_produk " +
                     "WHERE produk_id = ANY(?) " +
                     "ORDER BY tgl_kadaluarsa ASC NULLS LAST, id ASC";
        Array sqlIds = conn.createArrayOf("integer", ids);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setArray(1, sqlIds);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BatchProduk batch = mapRowToBatch(rs);
                    Produk pemilik = produkById.get(batch.getProdukId());
                    if (pemilik != null) {
                        pemilik.tambahBatch(batch);
                    }
                }
            }
        } finally {
            sqlIds.free();
        }
    }

    private List<BatchProduk> muatBatchByProdukId(Connection conn, int produkId) throws SQLException {
        List<BatchProduk> list = new ArrayList<>();
        String sql = "SELECT id, produk_id, tgl_masuk, tgl_kadaluarsa, stok_batch " +
                     "FROM batch_produk " +
                     "WHERE produk_id = ? " +
                     "ORDER BY tgl_kadaluarsa ASC NULLS LAST, id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, produkId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBatch(rs));
                }
            }
        }
        return list;
    }

    private void bindProdukParameters(PreparedStatement ps, Produk p) throws SQLException {
        ps.setString(1, p.getKode());
        ps.setString(2, p.getNama());
        ps.setBigDecimal(3, BigDecimal.valueOf(p.getHarga()));

        ps.setNull(5, Types.INTEGER);
        ps.setNull(6, Types.INTEGER);
        ps.setNull(7, Types.BOOLEAN);
        ps.setNull(8, Types.INTEGER);

        if (p instanceof Makanan m) {
            ps.setString(4, "MAKANAN");
            ps.setInt(5, m.getBeratGram());
        } else if (p instanceof Minuman mn) {
            ps.setString(4, "MINUMAN");
            ps.setInt(6, mn.getVolumeMl());
            ps.setBoolean(7, mn.isDingin());
        } else if (p instanceof Elektronik el) {
            ps.setString(4, "ELEKTRONIK");
            ps.setInt(8, el.getBulanGaransi());
        } else {
            throw new IllegalArgumentException("Jenis produk tidak didukung: " + p.getClass().getSimpleName());
        }
    }

    private Produk mapRowToProduk(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String kode = rs.getString("kode");
        String nama = rs.getString("nama");
        double harga = rs.getDouble("harga");
        String jenis = rs.getString("jenis_produk");

        return switch (jenis) {
            case "MAKANAN" -> new Makanan(id, kode, nama, harga, rs.getInt("berat_gram"));
            case "MINUMAN" -> new Minuman(id, kode, nama, harga, rs.getInt("volume_ml"), rs.getBoolean("is_dingin"));
            case "ELEKTRONIK" -> new Elektronik(id, kode, nama, harga, rs.getInt("bulan_garansi"));
            default -> throw new SQLException("Jenis produk tidak valid di database: " + jenis);
        };
    }

    private BatchProduk mapRowToBatch(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int produkId = rs.getInt("produk_id");
        Date sqlMasuk = rs.getDate("tgl_masuk");
        Date sqlKadaluarsa = rs.getDate("tgl_kadaluarsa");
        LocalDate tglMasuk = (sqlMasuk != null) ? sqlMasuk.toLocalDate() : LocalDate.now();
        LocalDate tglKadaluarsa = (sqlKadaluarsa != null) ? sqlKadaluarsa.toLocalDate() : null;
        int stokBatch = rs.getInt("stok_batch");
        return new BatchProduk(id, produkId, tglMasuk, tglKadaluarsa, stokBatch);
    }
}
