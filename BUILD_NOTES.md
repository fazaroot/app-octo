# BUILD_NOTES — CloneX

Catatan jujur tentang status kompilasi proyek ini. Dibuat pada 2026-11-23
di VM Linux (Ubuntu 24.04) tempat proyek ini ditulis.

## Perubahan 2026-11-23 — Perbaikan perintah adb di README (setelah zip 09:42)

- Perintah `dpm set-profile-owner` dan `dpm remove-active-admin` di
  README memakai nama class relatif
  `com.clonex.app.debug/.provisioning.CloneXDeviceAdminReceiver`.
  Bentuk relatif itu di-resolve terhadap applicationId, jadi pada
  build debug ia menjadi
  `com.clonex.app.debug.provisioning.CloneXDeviceAdminReceiver`,
  yang tidak ada (class sebenarnya
  `com.clonex.app.provisioning.CloneXDeviceAdminReceiver`), dan
  `dpm` akan gagal. Pada build rilis bentuk relatif itu kebetulan
  benar, karena applicationId-nya `com.clonex.app`.
- Diperbaiki menjadi nama class lengkap (fully qualified) di kedua
  perintah, yang benar untuk debug maupun rilis. Tidak ada perubahan
  kode Kotlin; hanya README. Zip di `workspace/your_files/CloneX.zip`
  diperbarui dengan README yang sudah diperbaiki.
- Upaya build ulang di VM ini (2026-11-23 sore, `./gradlew
  --no-daemon assembleDebug` memakai Gradle 8.7 di `/opt/gradle`)
  tetap gagal dengan error yang sama seperti sebelumnya
  ("Could not receive a message from the daemon"), jadi APK debug
  tetap belum dihasilkan di VM ini dan tetap harus di-build di
  Android Studio.

## Perubahan 2026-11-23 — Diagnostik Xiaomi POCO M6

- Baru: `device/DeviceDiagnostics.kt` mendeteksi perangkat, model,
  versi Android aktual (release dan API level), patch keamanan,
  dan versi HyperOS secara best-effort dari build.prop sistem
  (`ro.hyperos.version`, `ro.miui.ui.version.name`) dan Build.DISPLAY.
  Jika tidak terdeteksi, UI menampilkan "Tidak terdeteksi".
  Tidak ada refleksi ke API tersembunyi.
- Diperbarui: `CompatibilityChecker` kini menyertakan DeviceInfo dan
  status apakah pengguna saat ini adalah managed profile, plus
  penjelasan khusus Xiaomi. `canProvision` tetap hanya berdasarkan
  FEATURE_MANAGED_USERS dan tidak adanya work profile, bukan dari
  versi Android saja.
- Diperbarui: `CompatibilityScreen` kini menjadi diagnostic screen
  dengan bagian Diagnostik Perangkat, Pemeriksaan Provisioning,
  kartu peringatan "Tidak kompatibel untuk provisioning" bila
  FEATURE_MANAGED_USERS tidak tersedia, dan tombol "Salin Hasil
  Diagnostik" yang menyalin ringkasan teks ke clipboard.
- Target pengujian utama: Xiaomi POCO M6 (model 2404APC5FG) dengan
  Android dan HyperOS terbaru. Perubahan ini sudah diperiksa manual
  (tidak ada pemakaian CompatibilityStatus lain yang rusak), tetapi
  belum dikompilasi penuh di VM ini karena keterbatasan Gradle daemon
  yang sama seperti di bawah. Perlu build di Android Studio.

## Ringkasan status

**Kompilasi penuh (`./gradlew assembleDebug`) BELUM BERHASIL dijalankan di
VM ini.** Penyebabnya adalah keterbatasan lingkungan VM, bukan kesalahan
kode yang ditemukan: komunikasi TCP loopback yang dipakai Gradle daemon
tidak berfungsi di VM ini (bukti di bawah). **APK debug belum dihasilkan.**

Proyek ini lengkap dan siap dibuka di Android Studio di komputer lain.
Langkah build dan pengujiannya ada di `README.md`.

Yang sudah diverifikasi di VM ini (lihat bagian Verifikasi):

- Semua file XML valid (AndroidManifest, resource, device_admin).
- Semua pemakaian API framework Android resolve terhadap `android.jar`
  API 34 (DevicePolicyManager, LauncherApps, UserManager,
  PackageManager, DeviceAdminReceiver, dan lain-lain).
- Pemeriksaan sintaks Kotlin dengan kotlinc 1.9.24: **0 syntax error**.
- Tidak ada pemakaian API tersembunyi, refleksi ke layanan sistem,
  repackaging APK, atau perubahan tanda tangan di kode.

Yang belum bisa diverifikasi tanpa build penuh dan perangkat nyata:

- Kompilasi penuh beserta pemrosesan resource (aapt2), kapt Room,
  dan dexing (d8).
- APK debug dan instalasi di perangkat.
- Alur provisioning profil kerja, peluncuran lintas profil,
  dan perilaku di perangkat OEM tertentu.

## Lingkungan build yang disiapkan di VM ini

Awalnya VM tidak memiliki Java, Gradle, maupun Android SDK.

Yang berhasil disiapkan:

- **JDK 17** (Temurin 17.0.21.1) di `/opt/jdk`. Percobaan pertama lewat
  `apt-get install openjdk-17-jdk-headless` gagal karena proses
  instalasi dihentikan sistem; unduhan tarball Temurin lewat curl
  berhasil.
- **Android SDK** di `~/Android/Sdk`: platform android-34
  (`platform-34-ext12_r01.zip`), build-tools 34.0.0, dan platform-tools,
  diunduh langsung lewat curl dan diekstrak manual. File lisensi SDK
  dibuat di `licenses/android-sdk-license`.
- **Gradle 8.7** di `/opt/gradle/gradle-8.7`, diunduh lewat curl
  (distribusi `-bin`). `gradle --version` berjalan normal.
- **Gradle wrapper** proyek (`gradlew`, `gradlew.bat`,
  `gradle-wrapper.jar`, `gradle-wrapper.properties`) lengkap.

Yang gagal di lingkungan ini:

- **`sdkmanager` tidak bisa memuat daftar paket.** Gagal dengan
  `NoSuchElementException` di `HttpURLConnection.doTunneling` saat
  mengambil repositori jarak jauh. curl ke URL yang sama berhasil,
  jadi ini adalah masalah implementasi HTTP di JVM untuk rute jaringan
  VM ini, bukan masalah jaringan umum. Diatasi dengan mengunduh paket
  SDK langsung lewat curl.
- **Gradle daemon tidak bisa berkomunikasi dengan klien Gradle.**
  Setiap perintah Gradle (`assembleDebug`, bahkan `help`) gagal dengan:

  ```
  FAILURE: Build failed with an exception.
  * What went wrong:
  Could not receive a message from the daemon.
  Caused by: java.lang.IllegalArgumentException: Unexpected type tag 109 found.
  ```

  Bukti yang dikumpulkan:

  - Log daemon menunjukkan daemon berhasil start dan listen di
    `127.0.0.1` pada port ephemeral, tetapi **tidak pernah menerima
    koneksi maupun permintaan build** (`The daemon is running a build:
    false`), lalu berhenti sendiri karena klien menghapusnya dari
    registry setelah gagal berkomunikasi.
  - Uji soket minimal Java-ke-Java di loopback mereproduksi gejala
    yang sama: server Java tidak pernah menerima koneksi, sementara
    klien menerima bytes yang tidak diharapkan (byte pertama `0x6D`,
    sama dengan type tag 109 pada error Gradle).
  - Uji soket setara dengan Python (server dan klien di proses
    berbeda) berhasil normal, jadi loopback TCP pada umumnya bekerja;
    yang gagal adalah jalur soket Java yang dipakai Gradle daemon.

  Kesimpulan: Gradle membutuhkan komunikasi daemon lewat TCP loopback
  dan jalur itu tidak berfungsi untuk proses Java di VM ini. Tidak ada
  mode build Gradle yang didukung tanpa daemon, jadi kompilasi penuh
  tidak dapat diselesaikan di VM ini. Ini adalah keterbatasan
  lingkungan, bukan cacat pada proyek CloneX. Di komputer biasa
  (termasuk Android Studio di mesin Astr), Gradle daemon berjalan
  normal.

## Verifikasi yang sudah dilakukan

### 1. Validasi XML

Seluruh 8 file XML diparse dengan parser XML Python dan semuanya valid:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/device_admin.xml`
- `app/src/main/res/values/strings.xml`, `themes.xml`, `colors.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`,
  `ic_launcher_round.xml`

### 2. Pemeriksaan sintaks Kotlin dengan kotlinc 1.9.24

Perintah yang dijalankan (classpath hanya `android.jar` API 34,
tanpa library Gradle):

```
kotlinc -cp ~/Android/Sdk/platforms/android-34/android.jar \
  -d ~/tmp/kotlinc-out $(find app/src/main/java -name "*.kt")
```

Hasil:

- **0 syntax error** di seluruh 21 file Kotlin (2.283 baris).
- 690 error `unresolved reference`, semuanya ke library yang
  seharusnya diunduh Gradle (Compose, Material 3, Room, Navigation,
  Coil, lifecycle, kotlinx-coroutines). Ini wajar karena library
  tersebut tidak ada di classpath pemeriksaan ini.
- **0 unresolved reference ke `android.*`.** Semua pemakaian API
  framework Android resolve terhadap `android.jar` API 34.
- 21 error lain semuanya adalah efek lanjutan (cascade) dari
  library yang hilang, dan sudah diperiksa satu per satu, misalnya:
  `viewModelScope.launch` yang tidak dikenali sehingga pemanggilan
  `suspend` di dalamnya dilaporkan di luar korutin; `@OptIn`
  yang tidak bisa di-resolve; dan ambiguitas `Date(Long)` yang muncul
  hanya karena tipe `broadcastTime` tidak bisa di-infer tanpa runtime
  Compose. Tidak ada satu pun yang menunjukkan kesalahan pada kode
  dengan classpath lengkap.

### 3. Tinjauan manual kode

- Seluruh file Kotlin dibaca ulang setelah ditulis. Dua perbaikan
  diterapkan dari tinjauan ini: penghapusan import yang tidak
  terpakai di `AppRepository`, dan pemindahan pemuatan ikon aplikasi
  ke `Dispatchers.IO` di `DashboardViewModel` agar tidak menghambat
  thread utama.
- Perbaikan konfigurasi selama penulisan: versi compiler Compose
  dikoreksi menjadi `1.5.14` (yang cocok dengan Kotlin 1.9.24;
  nilai awal `1.9.24` bukan versi compiler Compose yang valid),
  plugin KSP yang tidak dipakai dihapus dari build root, dan heap
  Gradle dikembalikan ke `-Xmx2048m` untuk mesin pengembangan.

## Yang masih perlu diuji

1. **Di Android Studio (komputer Astr):** buka folder `CloneX`,
   biarkan Gradle Sync selesai, lalu jalankan `./gradlew
   assembleDebug`. Jika muncul error kompilasi, catat pesan lengkapnya;
   berdasarkan pemeriksaan di atas, error kemungkinan besar berasal
   dari perbedaan versi dependency, bukan dari sintaks kode.
2. **Di perangkat nyata:** ikuti alur pengujian di `README.md`
   (bagian "Cara menguji di perangkat nyata"): daftar aplikasi,
   layar Kompatibilitas, provisioning profil kerja, peluncuran lintas
   profil, kustomisasi nama dan ikon dashboard, favorit, dan filter.
3. **Perangkat OEM:** uji minimal di satu perangkat Pixel atau
   Android murni (referensi) dan di perangkat target Astr
   (POCO M6 / Xiaomi), karena dukungan managed profile berbeda-beda
   antar produsen. Layar Kompatibilitas akan menunjukkan status
   perangkat yang sebenarnya.

## File yang sengaja tidak disertakan

- `local.properties` (machine-specific, berisi path SDK lokal) tidak
  disertakan. Android Studio akan membuatnya saat proyek dibuka.
  Selama pengujian di VM ini, file tersebut dibuat sementara dengan
  `sdk.dir=/home/hatch/Android/Sdk`, lalu dihapus sebelum penyerahan.
- Tidak ada keystore. Build rilis perlu signing manual (lihat README).
