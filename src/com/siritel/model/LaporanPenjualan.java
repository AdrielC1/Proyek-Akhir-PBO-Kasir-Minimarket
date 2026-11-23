
package com.siritel.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class LaporanPenjualan implements Printable {

    private static final Locale ID_LOCALE = Locale.forLanguageTag("id-ID");

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

        List<Transaksi> salinan = new ArrayList<>(list.size());
        for (Transaksi transaksi : list) {
            if (transaksi == null) {
                throw new IllegalArgumentException(
                    "Daftar transaksi tidak boleh berisi null."
                );
            }
            LocalDate tgl = transaksi.getTanggal().toLocalDate();
            if (tgl.isBefore(awal) || tgl.isAfter(akhir)) {
                throw new IllegalArgumentException(
                    "Transaksi " + transaksi.getIdTransaksi()
                        + " berada di luar rentang periode laporan."
                );
            }
            salinan.add(transaksi);
        }

        this.periodeAwal = awal;
        this.periodeAkhir = akhir;
        this.daftarTransaksi = salinan;
    }

    // Menghitung total omzet dari seluruh transaksi
    public double hitungTotalOmzet() {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaksi transaksi : daftarTransaksi) {
            total = total.add(toMoneyDecimal(transaksi.getGrandTotal()));
        }
        return total.doubleValue();
    }

    // Menghitung total pajak dari seluruh transaksi
    public double hitungTotalPajakTerkumpul() {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaksi transaksi : daftarTransaksi) {
            total = total.add(toMoneyDecimal(transaksi.getTotalPajak()));
        }
        return total.doubleValue();
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
        BigDecimal total = BigDecimal.ZERO;
        for (Transaksi transaksi : transaksiHarian) {
            total = total.add(toMoneyDecimal(transaksi.getGrandTotal()));
        }
        return total.doubleValue();
    }

    // Menghitung pajak berdasarkan tanggal
    public double hitungPajakHarian(List<Transaksi> transaksiHarian) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaksi transaksi : transaksiHarian) {
            total = total.add(toMoneyDecimal(transaksi.getTotalPajak()));
        }
        return total.doubleValue();
    }

    // Getter
    public LocalDate getPeriodeAwal() {
        return periodeAwal;
    }

    public LocalDate getPeriodeAkhir() {
        return periodeAkhir;
    }

    // Implementasi interface Printable
    @Override
    public String cetak() {

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
            .append(formatRupiah(hitungTotalOmzet()))
            .append("\n");

        laporan.append("Total Pajak      : ")
            .append(formatRupiah(hitungTotalPajakTerkumpul()))
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
                formatRupiah(omzet),
                formatRupiah(pajak)
            ));
        }

        laporan.append(garisTebal);

        return laporan.toString();
    }

    private static BigDecimal toMoneyDecimal(double nominal) {
        return BigDecimal.valueOf(nominal).setScale(2, RoundingMode.HALF_UP);
    }

    private static String formatRupiah(double nominal) {
        BigDecimal bd = toMoneyDecimal(nominal);
        boolean adaSen = bd.stripTrailingZeros().scale() > 0;
        NumberFormat nf = NumberFormat.getNumberInstance(ID_LOCALE);
        nf.setMinimumFractionDigits(adaSen ? 2 : 0);
        nf.setMaximumFractionDigits(adaSen ? 2 : 0);
        return "Rp " + nf.format(bd);
    }
}
