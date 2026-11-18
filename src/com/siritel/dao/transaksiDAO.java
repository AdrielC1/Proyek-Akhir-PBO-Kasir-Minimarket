
package com.siritel.dao;

import com.siritel.config.DatabaseConnection;
import com.siritel.exception.StokTidakCukupException;
import com.siritel.model.ItemTransaksi;
import com.siritel.model.Produk;
import com.siritel.model.Transaksi;

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
}
