# AGENTS.md — Panduan Pengembangan SI-RITEL

File ini berisi konteks arsitektur, aturan bisnis, standar kode, dan batasan proyek **SI-RITEL** (Tugas Proyek Pemrograman Berorientasi Objek berbasis GUI Java & PostgreSQL). Setiap AI Agent atau kontributor wajib mematuhi seluruh aturan dalam dokumen ini saat membuat atau memodifikasi kode.

---

## 1. Identitas Proyek & Tech Stack

* **Nama Aplikasi:** SI-RITEL (Sistem Informasi Kasir & Inventaris Ritel)
* **Bahasa Pemrograman:** Java (JDK 17+)
* **GUI Framework:** Java Swing (`javax.swing.*`, `java.awt.*`) — dipilih agar berjalan langsung di Eclipse tanpa konfigurasi modul eksternal.
* **Database:** PostgreSQL dengan driver `pgJDBC` (`org.postgresql.Driver`).
* **Arsitektur:** Strict **MVC + DAO** (`model`, `dao`, `controller`, `view`, `config`, `exception`).
* **Referensi Skema:**
  * Skema SQL lengkap ada di `docs/database_schema.md`.
  * Struktur paket dan tanda tangan kelas ada di `docs/java_class_schema.md`.

---

## 2. Aturan Arsitektur & Pemisahan Layer (WAJIB)

1. **Dilarang Menulis SQL di View maupun Controller:** Seluruh query JDBC (`Connection`, `PreparedStatement`, `ResultSet`) hanya boleh berada di dalam package `com.siritel.dao`.
2. **Dilarang Menaruh Logika Bisnis di View:** Class di package `view` hanya bertugas merender komponen Swing, menangkap input pengguna, memanggil method pada `controller`, dan menampilkan hasil atau dialog pesan (`JOptionPane`).
3. **Pengelolaan Koneksi Database:** Selalu gunakan `DatabaseConnection.getConnection()` dengan blok `try-with-resources` pada `PreparedStatement` dan `ResultSet` agar tidak terjadi kebocoran koneksi.
4. **Password Security:** Jangan pernah menyimpan atau membandingkan password plaintext di Java. Gunakan fungsi PostgreSQL `crypt(?, gen_salt('bf'))` saat `INSERT/UPDATE` dan `password = crypt(?, password)` saat `SELECT` login di `PenggunaDAO`.

---

## 3. Aturan Implementasi 4 Pilar OOP & Rubrik Penilaian

Kode yang dihasilkan **wajib** mempertahankan penerapan OOP berikut agar lulus rubrik penilaian akademik:

1. **Enkapsulasi:**
   * Semua atribut model bersifat `private` atau `protected`.
   * Setiap *setter* numerik (`setHarga`, `setStokBatch`, `setBeratGram`, `setVolumeMl`, `setBulanGaransi`, `setKuantitas`) wajib melakukan validasi nilai dan melempar `IllegalArgumentException` jika nilai tidak valid.
2. **Pewarisan & Abstraksi:**
   * `Produk` adalah `abstract class` yang tidak boleh diinstansiasi langsung.
   * Subclass `Makanan`, `Minuman`, dan `Elektronik` wajib memanggil `super(...)` pada konstruktornya.
   * `Printable` adalah `interface` dengan method `String cetak()` yang diimplementasikan oleh **dua** class: `Transaksi` dan `LaporanPenjualan`.
3. **Polimorfisme (Overriding & Overloading):**
   * **Overriding:** Pemanggilan `produk.hitungPajak()` harus dilakukan melalui referensi tipe `Produk` (misalnya di dalam `ItemTransaksi` dan `ManajemenProdukView`), bukan dengan `instanceof` manual untuk menghitung pajak.
     * `Makanan`: $5\%$ dari `harga`.
     * `Minuman`: $11\%$ dari `harga` + Rp `500` jika `isDingin == true`.
     * `Elektronik`: $15\%$ dari `harga` jika `bulanGaransi <= 12`, atau $17\%$ jika `bulanGaransi > 12`.
   * **Overloading:** `ProdukController` wajib memiliki dua versi `cariProduk(String nama)` dan `cariProduk(String nama, String jenis)`.
4. **Exception Handling:**
   * Gunakan custom exception `StokTidakCukupException` saat kuantitas pembelian melebihi total stok tersedia.
   * Tangkap `SQLException`, `IllegalArgumentException`, dan `StokTidakCukupException` di layer View/Controller untuk ditampilkan lewat `JOptionPane.showMessageDialog(..., JOptionPane.ERROR_MESSAGE)`.

---

## 4. Aturan Logika Bisnis Inventaris & Transaksi

1. **Sistem Stok Multi-Batch & FEFO:**
   * Tabel `produk` tidak memiliki kolom `stok`. Total stok sebuah `Produk` dihitung dinamis dari penjumlahan `stokBatch` pada `List<BatchProduk>`.
   * Saat `TransaksiDAO.simpanTransaksi(Transaksi t)` dipanggil, proses penyimpanan header `transaksi`, detail `item_transaksi`, dan pemotongan `batch_produk.stok_batch` wajib berjalan dalam **satu transaksi database atomik** (`conn.setAutoCommit(false)` dengan `commit()` / `rollback()`).
   * Pemotongan stok wajib mengikuti urutan **FEFO (*First Expired, First Out*)**: mengurutkan batch berdasarkan `tgl_kadaluarsa ASC NULLS LAST, id ASC`.
2. **Format ID Transaksi:**
   * Gunakan format unik berbasis waktu, misalnya `"TRX-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())`.

---

## 5. Standar Antarmuka Grafis (Java Swing)

1. **Layout Manager:** Dilarang menggunakan `null` layout (`setLayout(null)`). Gunakan kombinasi `BorderLayout`, `GridBagLayout`, `GridLayout`, dan `FlowLayout` agar tampilan responsif saat jendela di-resize.
2. **Paket UI Terpusat (`com.siritel.view.ui`) — WAJIB:** Seluruh halaman pada paket `com.siritel.view` (`LoginView`, `MainFrame`, Modul 1, Modul 2, dan Modul 3: `KasirView`, `RiwayatTransaksiView`, `LaporanPenjualanView`, dll.) **wajib** menggunakan komponen dan token tema dari `com.siritel.view.ui` (jangan membuat komponen tombol/kartu/modal/datepicker rounded lokal di masing-masing View agar desain konsisten dan kode View tetap ringkas):
   * **Tema & Helper Input (`UITheme`):** Gunakan palet warna (`BG_PAGE`, `CARD_BG`, `TEXT_MAIN`, `TEXT_LABEL`, `TEXT_MUTED`, `BORDER_DEFAULT`, `BORDER_INPUT`, `PRIMARY_BLUE`, `TEAL`, `DANGER`, `BADGE_*`), tipografi (`UITheme.font(...)`), konstanta radius (`CARD_RADIUS`, `PANEL_RADIUS`, `BUTTON_RADIUS`, `SMALL_RADIUS`), serta helper `UITheme.createFieldLabel(...)`, `UITheme.styleInputField(...)`, `UITheme.styleNumericSpinner(...)`, dan `UITheme.createPlaceholderField(...)`.
   * **Tombol (`RoundedButton` + `ButtonVariant`):** Seluruh tombol aksi wajib memakai `RoundedButton` (`PRIMARY`, `TEAL`, `DANGER`, `OUTLINE`, `DANGER_OUTLINE`, `GHOST`) — dilarang memakai `new JButton(...)` polos untuk tombol toolbar, aksi form, atau tombol header (`Keluar`).
   * **Kartu & Border (`RoundedCardPanel` + `RoundedBorder`):** Bungkus panel konten utama dan panel samping form di atas latar `UITheme.BG_PAGE` menggunakan `RoundedCardPanel`, serta gunakan `RoundedBorder` untuk scroll/tabel/pembungkus input.
   * **Dropdown (`RoundedComboBoxUI`):** Gunakan `RoundedComboBoxUI.apply(combo, radius)` untuk setiap `JComboBox` agar sudut membulat dan bebas kotak fokus bawaan Win32.
   * **Pemilih Tanggal (`ModernDatePicker`):** Gunakan `ModernDatePicker` untuk input tanggal (termasuk tanggal batch pada Modul 2 dan filter rentang periode tanggal pada Laporan Penjualan Modul 3).
   * **Notifikasi & Modal (`ToastManager` + `ModernModal`):** Gunakan `ToastManager` (pojok kanan atas, non-blocking) untuk feedback sukses/info ringan, serta `ModernModal.confirm(...)` / `ModernModal.error(...)` untuk dialog konfirmasi dan galat agar tampil seragam dengan kartu membulat modern.
   * **Komponen Khusus Login:** `RoundedTextField`, `RoundedPasswordField`, dan `InlineAlertPanel` pada `com.siritel.view.ui` digunakan untuk layar autentikasi.
3. **Variasi Komponen Input (Wajib Ada):**
   * `JTextField` & `JPasswordField` (kode, nama, harga, username, password, nominal bayar).
   * `JComboBox` (pilihan jenis produk: `MAKANAN`, `MINUMAN`, `ELEKTRONIK` dan filter pencarian).
   * `JRadioButton` (pilihan role `Admin` / `Kasir` pada `ManajemenPenggunaView`).
   * `JCheckBox` (opsi `Minuman Dingin` pada `ManajemenProdukView`).
   * `JSpinner` / `ModernDatePicker` (input angka untuk `beratGram`, `volumeMl`, `bulanGaransi`, `stokBatch`, `kuantitas`, serta `ModernDatePicker` / `SpinnerDateModel` untuk tanggal masuk/kadaluarsa dan filter periode laporan).
4. **Konfirmasi Destruktif:** Setiap aksi hapus (`delete`) pada `ManajemenProdukView`, `ManajemenPenggunaView`, maupun modul lainnya wajib menampilkan dialog konfirmasi (`ModernModal.confirm(...)`) terlebih dahulu.
5. **Kontrol Hak Akses (`MainFrame`):**
   * Role `Admin` dapat mengakses seluruh 6 halaman (`Kasir`, `Manajemen Produk & Batch`, `Manajemen Pengguna`, `Riwayat Transaksi`, `Laporan Penjualan`, plus tombol `Logout`).
   * Role `Kasir` hanya dapat mengakses `Kasir`, `Riwayat Transaksi`, dan `Logout`.

---

## 6. Pembagian Modul Tim (3 Anggota)

Saat mengembangkan fitur, jaga batasan tanggung jawab tiap modul agar tidak terjadi konflik integrasi:

* **Modul 1 (Fondasi, Auth & User):** `DatabaseConnection`, `Main`, `Pengguna`, `PenggunaDAO`, `AuthController`, `LoginView`, `MainFrame`, `ManajemenPenggunaView`.
* **Modul 2 (Katalog Produk, Polimorfisme & Batch):** `Produk`, `Makanan`, `Minuman`, `Elektronik`, `BatchProduk`, `ProdukDAO`, `ProdukController`, `ManajemenProdukView`.
* **Modul 3 (Kasir, Riwayat & Laporan):** `printable`, `StokTidakCukupException`, `ItemTransaksi`, `Transaksi`, `LaporanPenjualan`, `TransaksiDAO`, `TransaksiController`, `KasirView`, `RiwayatTransaksiView`, `LaporanPenjualanView`.