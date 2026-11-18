
package com.siritel.dao;

import com.siritel.config.DatabaseConnection;
import com.siritel.exception.StokTidakCukupException;
import com.siritel.model.Elektronik;
import com.siritel.model.ItemTransaksi;
import com.siritel.model.TransaksiRingkas;
import com.siritel.model.Makanan;
import com.siritel.model.Minuman;
import com.siritel.model.Produk;
import com.siritel.model.Transaksi;
import com.siritel.model.LaporanPenjualan;
import com.siritel.model.Pengguna;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
    
public class TransaksiDAO {

    // Mengunci batch berdasarkan urutan FEFO
    private static final String SQL_LOCK_BATCH =
        "SELECT id, stok_batch " +
        "FROM batch_produk " +
        "WHERE produk_id = ? AND stok_batch > 0 " +
        "ORDER BY tgl_kadaluarsa ASC NULLS LAST, id ASC " +
        "FOR UPDATE";

    // Menyimpan header transaksi
    private static final String SQL_INSERT_TRANSAKSI =
        "INSERT INTO transaksi " +
        "(id_transaksi, tanggal, kasir_id, subtotal_barang, " +
        "total_pajak, grand_total, bayar, kembalian) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    // Menyimpan detail transaksi
    private static final String SQL_INSERT_ITEM =
        "INSERT INTO item_transaksi " +
        "(transaksi_id, produk_id, kuantitas, harga_satuan, " +
        "pajak_satuan, subtotal) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    // Mengurangi stok batch
    private static final String SQL_UPDATE_STOK =
        "UPDATE batch_produk " +
        "SET stok_batch = stok_batch - ? " +
        "WHERE id = ? AND stok_batch >= ?";

    // Mengambil ringkasan riwayat transaksi dengan pagination
    private static final String SQL_RIWAYAT_TRANSAKSI =
    "SELECT t.id_transaksi, t.tanggal, " +
    "p.nama_lengkap AS nama_kasir, " +
    "COUNT(it.id) AS jumlah_item, " +
    "t.grand_total " +
    "FROM transaksi t " +
    "JOIN pengguna p ON t.kasir_id = p.id " +
    "LEFT JOIN item_transaksi it ON t.id_transaksi = it.transaksi_id " +
    "WHERE t.tanggal >= ? AND t.tanggal < ? " +
    "GROUP BY t.id_transaksi, t.tanggal, p.nama_lengkap, t.grand_total " +
    "ORDER BY t.tanggal DESC, t.id_transaksi DESC " +
    "LIMIT ? OFFSET ?";

    // Mengambil header satu transaksi berdasarkan ID
    private static final String SQL_DETAIL_HEADER =
    "SELECT t.id_transaksi, t.tanggal, t.kasir_id, " +
    "p.username, p.nama_lengkap, p.role, " +
    "t.subtotal_barang, t.total_pajak, t.grand_total, " +
    "t.bayar, t.kembalian " +
    "FROM transaksi t " +
    "JOIN pengguna p ON t.kasir_id = p.id " +
    "WHERE t.id_transaksi = ?";
// Mengambil detail item beserta informasi produk
private static final String SQL_DETAIL_TRANSAKSI =
    "SELECT it.kuantitas, it.harga_satuan, " +
    "it.pajak_satuan, it.subtotal, " +
    "p.id AS produk_id, p.kode, p.nama, p.harga, " +
    "p.jenis_produk, p.berat_gram, p.volume_ml, " +
    "p.is_dingin, p.bulan_garansi " +
    "FROM item_transaksi it " +
    "JOIN produk p ON it.produk_id = p.id " +
    "WHERE it.transaksi_id = ? " +
    "ORDER BY it.id ASC";

    // Menghitung jumlah transaksi berdasarkan rentang tanggal
    private static final String SQL_HITUNG_RIWAYAT =
    "SELECT COUNT(*) " +
    "FROM transaksi " +
    "WHERE tanggal >= ? AND tanggal < ?";

    // Mengambil seluruh transaksi untuk laporan penjualan
    private static final String SQL_LAPORAN_PENJUALAN =
    "SELECT t.id_transaksi, t.tanggal, t.kasir_id, " +
    "p.username, p.nama_lengkap, p.role, " +
    "t.subtotal_barang, t.total_pajak, " +
    "t.grand_total, t.bayar, t.kembalian " +
    "FROM transaksi t " +
    "JOIN pengguna p ON t.kasir_id = p.id " +
    "WHERE t.tanggal >= ? AND t.tanggal < ? " +
    "ORDER BY t.tanggal ASC, t.id_transaksi ASC";
    /**
     * Menyimpan transaksi beserta detail dan pengurangan stok.
     * Seluruh operasi dilakukan dalam satu transaksi database.
     */
    public void simpanTransaksi(Transaksi transaksi)
            throws SQLException, StokTidakCukupException {

        validasiTransaksi(transaksi);

        // Mengelompokkan kebutuhan stok berdasarkan ID produk
        Map<Integer, Integer> kebutuhanStok =
            kumpulkanKebutuhanStok(transaksi);

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                // 1. Mengunci dan memeriksa batch menggunakan FEFO
                List<PenguranganBatch> daftarPengurangan =
                    rencanakanPenguranganStok(conn, kebutuhanStok);

                // 2. Menyimpan header transaksi
                simpanHeader(conn, transaksi);

                // 3. Menyimpan seluruh detail transaksi
                simpanDetail(conn, transaksi);

                // 4. Mengurangi stok setiap batch
                kurangiStokBatch(conn, daftarPengurangan);

                // 5. Menyelesaikan seluruh operasi
                conn.commit();

            } catch (
                SQLException |
                StokTidakCukupException |
                RuntimeException e
            ) {

                // Membatalkan seluruh perubahan jika gagal
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;
            }
        }
    }

   /**
 * Mengambil ringkasan riwayat transaksi berdasarkan tanggal
 * dan halaman yang dipilih.
 */
public List<TransaksiRingkas> getRiwayatTransaksi(
        LocalDate tanggalAwal,
        LocalDate tanggalAkhir,
        int halaman,
        int batas) throws SQLException {

    if (tanggalAwal == null || tanggalAkhir == null) {
        throw new IllegalArgumentException(
            "Rentang tanggal tidak boleh null."
        );
    }

    if (tanggalAwal.isAfter(tanggalAkhir)) {
        throw new IllegalArgumentException(
            "Tanggal awal tidak boleh melebihi tanggal akhir."
        );
    }

    if (halaman < 1 || batas < 1) {
        throw new IllegalArgumentException(
            "Halaman dan batas data harus lebih dari 0."
        );
    }

    List<TransaksiRingkas> daftarTransaksi = new ArrayList<>();

    LocalDateTime waktuAwal = tanggalAwal.atStartOfDay();
    LocalDateTime waktuAkhir = tanggalAkhir
        .plusDays(1)
        .atStartOfDay();

    int offset = Math.multiplyExact(halaman - 1, batas);

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(
             SQL_RIWAYAT_TRANSAKSI)) {

        ps.setTimestamp(1, Timestamp.valueOf(waktuAwal));
        ps.setTimestamp(2, Timestamp.valueOf(waktuAkhir));
        ps.setInt(3, batas);
        ps.setInt(4, offset);

        try (ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                TransaksiRingkas transaksi = new TransaksiRingkas(
                    rs.getString("id_transaksi"),
                    rs.getTimestamp("tanggal").toLocalDateTime(),
                    rs.getString("nama_kasir"),
                    rs.getInt("jumlah_item"),
                    rs.getBigDecimal("grand_total").doubleValue()
                );

                daftarTransaksi.add(transaksi);
            }
        }
    }

    return daftarTransaksi;
}
        /**
 * Mengambil satu transaksi beserta seluruh detail itemnya.
 *
 * @param idTransaksi ID transaksi yang dicari
 * @return objek transaksi lengkap, atau null jika tidak ditemukan
 */
public Transaksi getDetailTransaksi(String idTransaksi)
        throws SQLException {

    if (idTransaksi == null || idTransaksi.isBlank()) {
        throw new IllegalArgumentException(
            "ID transaksi tidak boleh kosong."
        );
    }

    try (Connection conn = DatabaseConnection.getConnection()) {

        // 1. Mengambil header transaksi
        try (PreparedStatement ps =
                conn.prepareStatement(SQL_DETAIL_HEADER)) {

            ps.setString(1, idTransaksi);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                Pengguna kasir = new Pengguna(
                    rs.getInt("kasir_id"),
                    rs.getString("username"),
                    null,
                    rs.getString("nama_lengkap"),
                    rs.getString("role")
                );

                LocalDateTime tanggal =
                    rs.getTimestamp("tanggal").toLocalDateTime();

                double subtotalBarang =
                    rs.getBigDecimal("subtotal_barang").doubleValue();

                double totalPajak =
                    rs.getBigDecimal("total_pajak").doubleValue();

                double grandTotal =
                    rs.getBigDecimal("grand_total").doubleValue();

                double bayar =
                    rs.getBigDecimal("bayar").doubleValue();

                double kembalian =
                    rs.getBigDecimal("kembalian").doubleValue();

                // 2. Mengambil seluruh item transaksi
                List<ItemTransaksi> daftarItem = new ArrayList<>();

                try (PreparedStatement psDetail =
                        conn.prepareStatement(SQL_DETAIL_TRANSAKSI)) {

                    psDetail.setString(1, idTransaksi);

                    try (ResultSet rsDetail = psDetail.executeQuery()) {

                        while (rsDetail.next()) {

                            // Membentuk objek produk sesuai jenisnya
                            int produkId = rsDetail.getInt("produk_id");
                            String kode = rsDetail.getString("kode");
                            String nama = rsDetail.getString("nama");
                            double harga = rsDetail.getDouble("harga");
                            String jenis = rsDetail.getString("jenis_produk");

                            Produk produk;

                            switch (jenis) {
                                case "MAKANAN":
                                    produk = new Makanan(
                                        produkId,
                                        kode,
                                        nama,
                                        harga,
                                        rsDetail.getInt("berat_gram")
                                    );
                                    break;

                                case "MINUMAN":
                                    produk = new Minuman(
                                        produkId,
                                        kode,
                                        nama,
                                        harga,
                                        rsDetail.getInt("volume_ml"),
                                        rsDetail.getBoolean("is_dingin")
                                    );
                                    break;

                                case "ELEKTRONIK":
                                    produk = new Elektronik(
                                        produkId,
                                        kode,
                                        nama,
                                        harga,
                                        rsDetail.getInt("bulan_garansi")
                                    );
                                    break;

                                default:
                                    throw new SQLException(
                                        "Jenis produk tidak valid: " + jenis
                                    );
                            }

                            // Membentuk item dengan nilai historis
                            ItemTransaksi item = new ItemTransaksi(
                                produk,
                                rsDetail.getInt("kuantitas"),
                                rsDetail.getBigDecimal("harga_satuan").doubleValue(),
                                rsDetail.getBigDecimal("pajak_satuan").doubleValue(),
                                rsDetail.getBigDecimal("subtotal").doubleValue()
                            );

                            daftarItem.add(item);
                        }
                    }
                }

                // 3. Membentuk transaksi lengkap
                return new Transaksi(
                    idTransaksi,
                    tanggal,
                    kasir,
                    daftarItem,
                    subtotalBarang,
                    totalPajak,
                    grandTotal,
                    bayar,
                    kembalian
                );
            }
        }
    }
}


    /**
     * Memvalidasi objek transaksi sebelum disimpan.
     */
    private void validasiTransaksi(Transaksi transaksi) {

        if (transaksi == null) {
            throw new IllegalArgumentException(
                "Transaksi tidak boleh null."
            );
        }

        if (transaksi.getIdTransaksi() == null ||
                transaksi.getIdTransaksi().isBlank() ||
                transaksi.getIdTransaksi().length() > 30) {

            throw new IllegalArgumentException(
                "ID transaksi tidak valid."
            );
        }

        if (transaksi.getTanggal() == null) {
            throw new IllegalArgumentException(
                "Tanggal transaksi tidak boleh null."
            );
        }

        if (transaksi.getKasir() == null ||
                transaksi.getKasir().getId() <= 0) {

            throw new IllegalArgumentException(
                "Data kasir tidak valid."
            );
        }

        if (transaksi.getListKeranjang() == null ||
                transaksi.getListKeranjang().isEmpty()) {

            throw new IllegalArgumentException(
                "Keranjang transaksi tidak boleh kosong."
            );
        }

        double subtotal = transaksi.getSubtotalBarang();
        double pajak = transaksi.getTotalPajak();
        double grandTotal = transaksi.getGrandTotal();
        double bayar = transaksi.getBayar();
        double kembalian = transaksi.getKembalian();

        // Memvalidasi nominal transaksi
        toDatabaseMoney(subtotal);
        toDatabaseMoney(pajak);
        toDatabaseMoney(grandTotal);
        toDatabaseMoney(bayar);
        toDatabaseMoney(kembalian);

        if (!nominalSama(subtotal + pajak, grandTotal)) {
            throw new IllegalArgumentException(
                "Grand total tidak sesuai dengan subtotal dan pajak."
            );
        }

        if (bayar < grandTotal) {
            throw new IllegalArgumentException(
                "Pembayaran belum mencukupi."
            );
        }

        if (!nominalSama(bayar - grandTotal, kembalian)) {
            throw new IllegalArgumentException(
                "Nilai kembalian tidak sesuai."
            );
        }

        double subtotalItem = 0;
        double totalPajakItem = 0;

        for (ItemTransaksi item : transaksi.getListKeranjang()) {

            if (item == null || item.getProduk() == null) {
                throw new IllegalArgumentException(
                    "Item transaksi atau produk tidak valid."
                );
            }

            if (item.getProduk().getId() <= 0) {
                throw new IllegalArgumentException(
                    "Produk harus sudah memiliki ID database."
                );
            }

            if (item.getKuantitas() <= 0) {
                throw new IllegalArgumentException(
                    "Kuantitas produk harus lebih dari 0."
                );
            }

            toDatabaseMoney(item.getHargaSatuan());
            toDatabaseMoney(item.getPajakSatuan());
            toDatabaseMoney(item.getSubtotal());

            subtotalItem += item.getSubtotal();
            totalPajakItem += item.hitungPajakItem();
        }

        if (!nominalSama(subtotalItem, subtotal) ||
                !nominalSama(totalPajakItem, pajak)) {

            throw new IllegalArgumentException(
                "Perhitungan detail transaksi tidak sesuai dengan header."
            );
        }
    }


    /**
     * Mengumpulkan total kebutuhan stok setiap produk.
     * TreeMap memastikan produk diproses berdasarkan ID terkecil dahulu.
     */
    private Map<Integer, Integer> kumpulkanKebutuhanStok(
            Transaksi transaksi) {

        Map<Integer, Integer> kebutuhan = new TreeMap<>();

        for (ItemTransaksi item : transaksi.getListKeranjang()) {

            int produkId = item.getProduk().getId();
            int kuantitas = item.getKuantitas();

            kebutuhan.merge(
                produkId,
                kuantitas,
                Math::addExact
            );
        }

        return kebutuhan;
    }


    /**
     * Mengunci batch dan menyusun rencana pengurangan stok FEFO.
     */
    private List<PenguranganBatch> rencanakanPenguranganStok(
            Connection conn,
            Map<Integer, Integer> kebutuhanStok)
            throws SQLException, StokTidakCukupException {

        List<PenguranganBatch> daftarPengurangan =
            new ArrayList<>();

        try (PreparedStatement ps =
                conn.prepareStatement(SQL_LOCK_BATCH)) {

            for (Map.Entry<Integer, Integer> entry :
                    kebutuhanStok.entrySet()) {

                int produkId = entry.getKey();
                int kebutuhan = entry.getValue();

                int sisaKebutuhan = kebutuhan;
                int totalStokTersedia = 0;

                ps.setInt(1, produkId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        int batchId = rs.getInt("id");
                        int stokBatch = rs.getInt("stok_batch");

                        totalStokTersedia = Math.addExact(
                            totalStokTersedia,
                            stokBatch
                        );

                        if (sisaKebutuhan > 0) {

                            int jumlahDiambil = Math.min(
                                stokBatch,
                                sisaKebutuhan
                            );

                            if (jumlahDiambil > 0) {

                                daftarPengurangan.add(
                                    new PenguranganBatch(
                                        batchId,
                                        jumlahDiambil
                                    )
                                );

                                sisaKebutuhan -= jumlahDiambil;
                            }
                        }
                    }
                }

                // Jika stok semua batch tidak mencukupi
                if (sisaKebutuhan > 0) {

                    throw new StokTidakCukupException(
                        "Stok produk ID " + produkId +
                        " tidak mencukupi. Stok tersedia: " +
                        totalStokTersedia +
                        ", dibutuhkan: " + kebutuhan,
                        totalStokTersedia
                    );
                }
            }
        }

        return daftarPengurangan;
    }


    /**
     * Menyimpan header transaksi ke tabel transaksi.
     */
    private void simpanHeader(
            Connection conn,
            Transaksi transaksi) throws SQLException {

        try (PreparedStatement ps =
                conn.prepareStatement(SQL_INSERT_TRANSAKSI)) {

            ps.setString(1, transaksi.getIdTransaksi());

            ps.setTimestamp(
                2,
                Timestamp.valueOf(transaksi.getTanggal())
            );

            ps.setInt(3, transaksi.getKasir().getId());

            ps.setBigDecimal(
                4, toDatabaseMoney(transaksi.getSubtotalBarang())
            );

            ps.setBigDecimal(
                5, toDatabaseMoney(transaksi.getTotalPajak())
            );

            ps.setBigDecimal(
                6, toDatabaseMoney(transaksi.getGrandTotal())
            );

            ps.setBigDecimal(
                7, toDatabaseMoney(transaksi.getBayar())
            );

            ps.setBigDecimal(
                8, toDatabaseMoney(transaksi.getKembalian())
            );

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                    "Gagal menyimpan header transaksi."
                );
            }
        }
    }


    /**
     * Menyimpan seluruh item transaksi.
     */
    private void simpanDetail(
            Connection conn,
            Transaksi transaksi) throws SQLException {

        try (PreparedStatement ps =
                conn.prepareStatement(SQL_INSERT_ITEM)) {

            for (ItemTransaksi item :
                    transaksi.getListKeranjang()) {

                ps.setString(
                    1, transaksi.getIdTransaksi()
                );

                ps.setInt(
                    2, item.getProduk().getId()
                );

                ps.setInt(
                    3, item.getKuantitas()
                );

                ps.setBigDecimal(
                    4, toDatabaseMoney(item.getHargaSatuan())
                );

                ps.setBigDecimal(
                    5, toDatabaseMoney(item.getPajakSatuan())
                );

                ps.setBigDecimal(
                    6, toDatabaseMoney(item.getSubtotal())
                );

                ps.executeUpdate();
            }
        }
    }


    /**
     * Mengurangi stok berdasarkan rencana FEFO.
     */
    private void kurangiStokBatch(
            Connection conn,
            List<PenguranganBatch> daftarPengurangan)
            throws SQLException {

        try (PreparedStatement ps =
                conn.prepareStatement(SQL_UPDATE_STOK)) {

            for (PenguranganBatch pengurangan :
                    daftarPengurangan) {

                ps.setInt(1, pengurangan.kuantitas());
                ps.setInt(2, pengurangan.batchId());
                ps.setInt(3, pengurangan.kuantitas());

                int affected = ps.executeUpdate();

                if (affected != 1) {
                    throw new SQLException(
                        "Gagal mengurangi stok batch ID " +
                        pengurangan.batchId()
                    );
                }
            }
        }
    }


    /**
     * Mengubah nominal double menjadi BigDecimal
     * dengan dua angka desimal sesuai NUMERIC(12,2).
     */
    private BigDecimal toDatabaseMoney(double nominal) {

        if (!Double.isFinite(nominal) || nominal < 0) {
            throw new IllegalArgumentException(
                "Nominal transaksi tidak valid."
            );
        }

        return BigDecimal.valueOf(nominal)
            .setScale(2, RoundingMode.HALF_UP);
    }


    /**
     * Membandingkan nominal setelah disesuaikan
     * dengan presisi database.
     */
    private boolean nominalSama(double nilai1, double nilai2) {
        return toDatabaseMoney(nilai1)
            .compareTo(toDatabaseMoney(nilai2)) == 0;
    }


    // Menyimpan informasi rencana pengurangan batch
    private record PenguranganBatch(
        int batchId,
        int kuantitas
    ) {
    }
    /**
 * Menghitung seluruh transaksi dalam rentang tanggal.
 */
public int getJumlahRiwayatTransaksi(
        LocalDate tanggalAwal,
        LocalDate tanggalAkhir) throws SQLException {

    if (tanggalAwal == null || tanggalAkhir == null) {
        throw new IllegalArgumentException(
            "Rentang tanggal tidak boleh null."
        );
    }

    if (tanggalAwal.isAfter(tanggalAkhir)) {
        throw new IllegalArgumentException(
            "Tanggal awal tidak boleh melebihi tanggal akhir."
        );
    }

    LocalDateTime waktuAwal = tanggalAwal.atStartOfDay();
    LocalDateTime waktuAkhir = tanggalAkhir
        .plusDays(1)
        .atStartOfDay();

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(
             SQL_HITUNG_RIWAYAT)) {

        ps.setTimestamp(1, Timestamp.valueOf(waktuAwal));
        ps.setTimestamp(2, Timestamp.valueOf(waktuAkhir));

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
    }

    return 0;
}

/**
 * Mengambil data transaksi untuk laporan penjualan
 * berdasarkan rentang tanggal.
 */
public LaporanPenjualan getLaporanPenjualan(
        LocalDate tanggalAwal,
        LocalDate tanggalAkhir) throws SQLException {

    if (tanggalAwal == null || tanggalAkhir == null) {
        throw new IllegalArgumentException(
            "Rentang tanggal tidak boleh null."
        );
    }

    if (tanggalAwal.isAfter(tanggalAkhir)) {
        throw new IllegalArgumentException(
            "Tanggal awal tidak boleh melebihi tanggal akhir."
        );
    }

    LocalDateTime waktuAwal =
        tanggalAwal.atStartOfDay();

    LocalDateTime waktuAkhir =
        tanggalAkhir.plusDays(1).atStartOfDay();

    List<Transaksi> daftarTransaksi = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(
             SQL_LAPORAN_PENJUALAN)) {

        ps.setTimestamp(1, Timestamp.valueOf(waktuAwal));
        ps.setTimestamp(2, Timestamp.valueOf(waktuAkhir));

        try (ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                // Membentuk objek kasir
                Pengguna kasir = new Pengguna(
                    rs.getInt("kasir_id"),
                    rs.getString("username"),
                    null,
                    rs.getString("nama_lengkap"),
                    rs.getString("role")
                );

                // Mengambil tanggal transaksi
                LocalDateTime tanggal =
                    rs.getTimestamp("tanggal").toLocalDateTime();

                // Membentuk objek transaksi historis
                Transaksi transaksi = new Transaksi(
                    rs.getString("id_transaksi"),
                    tanggal,
                    kasir,
                    new ArrayList<>(),
                    rs.getBigDecimal("subtotal_barang").doubleValue(),
                    rs.getBigDecimal("total_pajak").doubleValue(),
                    rs.getBigDecimal("grand_total").doubleValue(),
                    rs.getBigDecimal("bayar").doubleValue(),
                    rs.getBigDecimal("kembalian").doubleValue()
                );

                daftarTransaksi.add(transaksi);
            }
        }
    }

    // Membentuk objek laporan penjualan
    return new LaporanPenjualan(
        tanggalAwal,
        tanggalAkhir,
        daftarTransaksi
    );
}
}
