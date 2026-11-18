
package com.siritel.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class LaporanPenjualan implements printable {

    private final LocalDate periodeAwal;
    private final LocalDate periodeAkhir;
    private final List<Transaksi> daftarTransaksi;

    // Constructor
    public LaporanPenjualan(
            LocalDate awal,
            LocalDate akhir,
            List<Transaksi> list) {

        if (awal == null || akhir == null) {
            throw new IllegalArgumentException(
                "Periode laporan tidak boleh kosong."
            );
        }

        if (awal.isAfter(akhir)) {
            throw new IllegalArgumentException(
                "Tanggal awal tidak boleh melebihi tanggal akhir."
            );
        }

        if (list == null) {
            throw new IllegalArgumentException(
                "Daftar transaksi tidak boleh null."
            );
        }

        if (list.contains(null)) {
            throw new IllegalArgumentException(
                "Daftar transaksi tidak boleh berisi null."
            );
        }

        this.periodeAwal = awal;
        this.periodeAkhir = akhir;

        // Menyalin daftar transaksi
        this.daftarTransaksi = new ArrayList<>(list);
    }

    // Menghitung total omzet dari seluruh transaksi
    public double hitungTotalOmzet() {

        double totalOmzet = 0;

        for (Transaksi transaksi : daftarTransaksi) {
            totalOmzet += transaksi.getGrandTotal();
        }

        return totalOmzet;
    }

    // Menghitung total pajak dari seluruh transaksi
    public double hitungTotalPajakTerkumpul() {

        double totalPajak = 0;

        for (Transaksi transaksi : daftarTransaksi) {
            totalPajak += transaksi.getTotalPajak();
        }

        return totalPajak;
    }

    // Menghitung jumlah transaksi
    public int hitungJumlahTransaksi() {
        return daftarTransaksi.size();
    }

    // Mengelompokkan transaksi berdasarkan tanggal
    public Map<LocalDate, List<Transaksi>> getDaftarHarian() {

        Map<LocalDate, List<Transaksi>> laporanHarian =
            new TreeMap<>();

        // Membuat kelompok untuk setiap tanggal
        for (LocalDate tanggal = periodeAwal;
                !tanggal.isAfter(periodeAkhir);
                tanggal = tanggal.plusDays(1)) {

            laporanHarian.put(tanggal, new ArrayList<>());
        }

        // Memasukkan transaksi ke tanggal masing-masing
        for (Transaksi transaksi : daftarTransaksi) {

            LocalDate tanggal =
                transaksi.getTanggal().toLocalDate();

            List<Transaksi> transaksiTanggal =
                laporanHarian.get(tanggal);

            if (transaksiTanggal != null) {
                transaksiTanggal.add(transaksi);
            }
        }

        // Mengembalikan data yang tidak dapat dimodifikasi
        Map<LocalDate, List<Transaksi>> hasil =
            new TreeMap<>();

        for (Map.Entry<LocalDate, List<Transaksi>> entry :
                laporanHarian.entrySet()) {

            hasil.put(
                entry.getKey(),
                Collections.unmodifiableList(entry.getValue())
            );
        }

        return Collections.unmodifiableMap(hasil);
    }

    // Menghitung omzet berdasarkan tanggal
    public double hitungOmzetHarian(List<Transaksi> transaksiHarian) {

        double total = 0;

        for (Transaksi transaksi : transaksiHarian) {
            total += transaksi.getGrandTotal();
        }

        return total;
    }

    // Menghitung pajak berdasarkan tanggal
    public double hitungPajakHarian(List<Transaksi> transaksiHarian) {

        double total = 0;

        for (Transaksi transaksi : transaksiHarian) {
            total += transaksi.getTotalPajak();
        }

        return total;
    }

    // Getter
    public LocalDate getPeriodeAwal() {
        return periodeAwal;
    }

    public LocalDate getPeriodeAkhir() {
        return periodeAkhir;
    }

    public List<Transaksi> getDaftarTransaksi() {
        return Collections.unmodifiableList(daftarTransaksi);
    }

    // Implementasi interface printable
    @Override
    public String cetak() {

        NumberFormat rupiah =
            NumberFormat.getCurrencyInstance(
                new Locale("id", "ID")
            );

        DateTimeFormatter formatTanggal =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

        String garisTebal = "====================================================================\n";
        String garisTipis = "--------------------------------------------------------------------\n";

        StringBuilder laporan = new StringBuilder();

        laporan.append(garisTebal);
        laporan.append("                              SI-RITEL\n");
        laporan.append("                         LAPORAN PENJUALAN\n");
        laporan.append(garisTebal);

        laporan.append("Periode Awal     : ")
            .append(periodeAwal.format(formatTanggal))
            .append("\n");

        laporan.append("Periode Akhir    : ")
            .append(periodeAkhir.format(formatTanggal))
            .append("\n");

        laporan.append(garisTipis);

        laporan.append("RINGKASAN PENJUALAN\n");

        laporan.append("Jumlah Transaksi : ")
            .append(hitungJumlahTransaksi())
            .append("\n");

        laporan.append("Total Omzet      : ")
            .append(rupiah.format(hitungTotalOmzet()))
            .append("\n");

        laporan.append("Total Pajak      : ")
            .append(rupiah.format(hitungTotalPajakTerkumpul()))
            .append("\n");

        laporan.append(garisTipis);
        laporan.append("RINCIAN PENJUALAN HARIAN\n");

        laporan.append(String.format(
            "%-12s | %10s | %18s | %18s%n",
            "Tanggal",
            "Transaksi",
            "Total Omzet",
            "Total Pajak"
        ));

        laporan.append(garisTipis);

        Map<LocalDate, List<Transaksi>> laporanHarian =
            getDaftarHarian();

        for (Map.Entry<LocalDate, List<Transaksi>> entry :
                laporanHarian.entrySet()) {

            List<Transaksi> transaksiHarian = entry.getValue();

            int jumlahTransaksi = transaksiHarian.size();

            double omzet = hitungOmzetHarian(transaksiHarian);

            double pajak = hitungPajakHarian(transaksiHarian);

            laporan.append(String.format(
                "%-12s | %10d | %18s | %18s%n",
                entry.getKey().format(formatTanggal),
                jumlahTransaksi,
                rupiah.format(omzet),
                rupiah.format(pajak)
            ));
        }

        laporan.append(garisTebal);

        return laporan.toString();
    }
}
