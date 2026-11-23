# CloneX — Manajer Instance Aplikasi berbasis Work Profile Resmi Android

CloneX adalah aplikasi Android native (Kotlin, Jetpack Compose, Material 3)
yang membantu pengguna menjalankan satu aplikasi sebagai dua instance
terpisah: satu di **Profil Utama** dan satu di **Profil Kerja** (managed
profile) resmi Android.

CloneX **tidak** mengubah APK, **tidak** mengubah package name, **tidak**
mengubah tanda tangan digital aplikasi lain, dan **tidak** memakai root.
Semua pemrosesan dilakukan lokal di perangkat. Tidak ada upload APK atau
data pribadi ke server mana pun.

## Cara kerja singkat

1. Android memisahkan data aplikasi per pengguna/profil di level sistem.
   Aplikasi yang sama di profil berbeda punya UID Linux berbeda dan folder
   data berbeda (`/data/user/<userId>/<package>`).
2. CloneX memakai mekanisme resmi itu: ia membantu pengguna membuat satu
   managed profile lewat alur provisioning Android, lalu menampilkan
   aplikasi dari kedua profil dalam satu dashboard.
3. Peluncuran aplikasi dilakukan lewat `LauncherApps.startMainActivity`
   dengan `UserHandle` yang benar.
4. Nama dan ikon kustom hanya berlaku di dashboard CloneX. CloneX tidak
   mengubah nama atau ikon aplikasi di launcher sistem.

## Keterbatasan yang harus dipahami

- **Maksimal 2 instance per aplikasi**: satu di Profil Utama, satu di
  Profil Kerja. Android hanya mengizinkan satu managed profile, jadi
  tidak ada instance ketiga, keempat, dan seterusnya lewat jalur ini.
- **Satu managed profile saja.** Jika perangkat sudah punya profil kerja
  kantor (misalnya dari aplikasi MDM perusahaan), CloneX tidak bisa
  membuat profil kedua sebelum profil yang ada dihapus lewat Pengaturan
  sistem. Menghapus profil kerja akan menghapus seluruh data di dalamnya.
- **Dukungan OEM berbeda-beda.** Pixel dan Android murni paling konsisten.
  Sebagian perangkat Xiaomi, Oppo, Vivo, Huawei, dan Android Go
  menonaktifkan atau membatasi managed profile. Layar Kompatibilitas di
  aplikasi akan menampilkan status perangkat yang sebenarnya.
- **Nama dan ikon kustom hanya di dashboard CloneX.** Android tidak
  menyediakan API publik untuk mengubah nama atau ikon aplikasi lain di
  launcher sistem.
- **Mengaktifkan aplikasi ke profil kerja.** Untuk aplikasi sistem,
  Profile Owner bisa mengaktifkannya lewat `DevicePolicyManager`.
  Untuk aplikasi pengguna biasa, jalur resmi adalah menginstall aplikasi
  itu di dalam profil kerja lewat Play Store di profil kerja
  (Managed Google Play) atau lewat PackageInstaller di dalam profil
  kerja. CloneX versi ini tidak berpura-pura bisa memindahkan aplikasi
  pengguna secara diam-diam.
- **Aplikasi yang memakai pemeriksaan keamanan ketat** (misalnya Play
  Integrity, deteksi perangkat ter-root, atau kebijakan perusahaan)
  bisa saja menolak berjalan di profil kerja. Itu adalah keputusan
  aplikasi target, bukan kesalahan CloneX.

## Spesifikasi teknis

- Bahasa: Kotlin 1.9.24
- UI: Jetpack Compose (BOM 2024.06.00), Material 3, Navigation Compose
- Build: Gradle Kotlin DSL, Android Gradle Plugin 8.4.2, Gradle 8.7
- `compileSdk 34`, `targetSdk 34`, `minSdk 26` (Android 8.0)
- Java 17
- Database: Room 2.6.1 untuk metadata kustom (nama, ikon, favorit,
  waktu terakhir diluncurkan)
- API utama: `LauncherApps`, `UserManager`, `DevicePolicyManager`,
  `PackageManager`
- Package: `com.clonex.app`

## Struktur proyek

```
CloneX/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── values/strings.xml, themes.xml, colors.xml
│       │   ├── xml/device_admin.xml
│       │   ├── drawable/ic_launcher_foreground.xml
│       │   └── mipmap-anydpi-v26/ic_launcher.xml
│       └── java/com/clonex/app/
│           ├── MainActivity.kt
│           ├── CloneXApplication.kt
│           ├── ui/
│           │   ├── theme/ (Color, Type, Theme)
│           │   ├── navigation/AppNav.kt
│           │   ├── components/ (AppRow, StatusCard)
│           │   └── screens/ (Dashboard, DashboardViewModel,
│           │       Compatibility, Provisioning)
│           ├── data/
│           │   ├── db/ (AppDatabase, ManagedAppDao, ManagedAppEntity)
│           │   ├── model/AppEntry.kt
│           │   └── repository/ (AppRepository,
│           │       CustomizationRepository, IconStorage)
│           ├── domain/LaunchAppUseCase.kt
│           ├── device/CompatibilityChecker.kt
│           ├── profile/ProfileManager.kt
│           └── provisioning/ (CloneXDeviceAdminReceiver,
│               ProvisioningManager)
```

## Cara membuka di Android Studio

1. Buka Android Studio (disarankan versi Hedgehog atau lebih baru,
   dengan Android SDK Platform 34 terinstall).
2. Pilih **Open**, lalu pilih folder `CloneX` ini.
3. Biarkan Gradle Sync berjalan sampai selesai. Android Studio akan
   mengunduh Gradle 8.7, Android Gradle Plugin 8.4.2, dan dependensi
   lain pada sync pertama (butuh koneksi internet).
4. Jika muncul peringatan `QUERY_ALL_PACKAGES`, itu sudah dijelaskan di
   `AndroidManifest.xml`: izin ini dipakai untuk menampilkan daftar
   aplikasi ter-install di dashboard. Untuk distribusi Play Store,
   penggunaan izin ini harus dijelaskan dalam pengajuan aplikasi.

## Cara build

Dari terminal di folder `CloneX`:

```bash
./gradlew assembleDebug
```

APK debug akan ada di:

```
app/build/outputs/apk/debug/app-debug.apk
```

Untuk build rilis (dengan minifikasi dan ProGuard):

```bash
./gradlew assembleRelease
```

Catatan: build rilis membutuhkan konfigurasi signing. Proyek ini belum
menyertakan keystore, jadi `assembleRelease` akan menghasilkan APK
tidak bertanda tangan (unsigned) dan perlu ditandatangani manual
sebelum didistribusikan.

## Cara menguji di perangkat nyata

### Persiapan

- Perangkat Android 8.0 atau lebih baru, tanpa root.
- Aktifkan **Opsi Pengembang** dan **USB Debugging**.
- Install APK debug: `./gradlew installDebug` atau salin APK ke
  perangkat dan install manual.
- Cadangkan data penting. Provisioning membuat profil baru; menghapus
  profil kerja nanti akan menghapus seluruh data di dalamnya.

### Alur pengujian yang disarankan

1. **Buka CloneX, layar Beranda.** Pastikan daftar aplikasi Profil Utama
   muncul, pencarian berfungsi, dan menekan satu aplikasi meluncurkannya.
2. **Buka layar Kompatibilitas.** Layar ini adalah diagnostic screen:
   menampilkan perangkat dan model (dengan penanda khusus jika POCO M6),
   versi Android aktual dan API level, patch keamanan, versi HyperOS
   (best-effort dari build.prop dan Build.DISPLAY, atau "Tidak
   terdeteksi" jika tidak bisa dibaca), status FEATURE_MANAGED_USERS,
   keberadaan work profile, dan status Profile Owner. Tekan **Periksa
   Ulang** untuk memastikan status diperbarui, dan **Salin Hasil
   Diagnostik** untuk menyalin ringkasan teks. Jika perangkat tidak
   mendukung managed profile, layar menampilkan alasan dan tombol
   provisioning tidak ditampilkan. Jangan mengasumsikan dukungan hanya
   dari versi Android, karena sebagian perangkat HyperOS menonaktifkan
   managed profile meskipun Androidnya memenuhi syarat.
3. **Buka layar Profil Kerja.** Baca penjelasan batasan, lalu tekan
   **Mulai Provisioning Profil Kerja**. Ikuti alur persetujuan sistem
   Android. Coba juga menekan Batal untuk memastikan pembatalan
   ditangani dengan pesan yang jelas.
4. **Setelah provisioning selesai**, kembali ke layar Kompatibilitas
   dan pastikan status berubah menjadi Profile Owner aktif.
5. **Kembali ke Beranda.** Aplikasi yang tersedia di profil kerja kini
   muncul dengan badge Profil Kerja. Coba luncurkan satu aplikasi
   dari Profil Kerja.
6. **Uji kustomisasi dashboard.** Tekan ikon pensil pada satu aplikasi,
   ubah nama tampilannya, pilih ikon kustom dari galeri, simpan, dan
   pastikan perubahan hanya tampil di dashboard CloneX.
7. **Uji favorit dan filter.** Tandai satu aplikasi sebagai favorit,
   lalu pakai filter Favorit, Utama, dan Kerja.

### Pengujian provisioning lewat adb (hanya untuk pengembang)

Untuk pengujian di perangkat sendiri tanpa alur UI, provisioning juga
bisa diuji lewat `adb`. Perintah ini membutuhkan perangkat yang belum
memiliki Profile Owner dan akses adb yang sudah diizinkan:

```bash
adb shell dpm set-profile-owner com.clonex.app.debug/com.clonex.app.provisioning.CloneXDeviceAdminReceiver
```

Catatan:

- Untuk build debug, applicationId adalah `com.clonex.app.debug`.
  Untuk build rilis, pakai `com.clonex.app` sebagai package, dengan
  nama class yang sama persis:
  `adb shell dpm set-profile-owner com.clonex.app/com.clonex.app.provisioning.CloneXDeviceAdminReceiver`
- Nama class harus ditulis lengkap (fully qualified), bukan bentuk
  relatif `.provisioning.CloneXDeviceAdminReceiver`. Bentuk relatif
  di-resolve terhadap applicationId, jadi pada build debug ia menjadi
  `com.clonex.app.debug.provisioning.CloneXDeviceAdminReceiver` —
  class itu tidak ada, dan `dpm` akan gagal. Bentuk lengkap di atas
  benar untuk debug maupun rilis.
- Perintah ini hanya untuk pengujian. CloneX tidak menyediakan tombol
  yang menjalankan perintah ini secara otomatis, karena itu akan
  melewati persetujuan pengguna.
- Untuk menghapus Profile Owner lewat adb (pengujian saja):

```bash
adb shell dpm remove-active-admin com.clonex.app.debug/com.clonex.app.provisioning.CloneXDeviceAdminReceiver
```

- Menghapus profil kerja sebaiknya dilakukan lewat Pengaturan sistem:
  **Pengaturan > Akun > Hapus profil kerja** (nama menu berbeda-beda
  di tiap produsen).

## API yang dipakai dan yang sengaja tidak dipakai

- **Dipakai (API publik):** `UserManager.getUserProfiles`,
  `PackageManager.hasSystemFeature(FEATURE_MANAGED_USERS)`,
  `DevicePolicyManager.isProfileOwnerApp`,
  `DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE`,
  `LauncherApps.getActivityList`,
  `LauncherApps.startMainActivity`,
  `PackageManager.getUserBadgedIcon`, Room, Storage Access Framework
  (`GetContent`) untuk memilih ikon kustom.
- **Tidak dipakai:** API tersembunyi (hidden API), refleksi ke layanan
  sistem, hooking ActivityManager/PackageManager, framework virtualisasi
  aplikasi, apktool atau repackaging, dan perubahan tanda tangan.
  Jika suatu fitur tidak didukung API publik, aplikasi menjelaskannya
  di UI dan README ini, bukan berpura-pura berhasil.

## Status build

Lihat `BUILD_NOTES.md` untuk status kompilasi yang sebenarnya di
lingkungan tempat proyek ini dibuat, termasuk apa yang sudah
diverifikasi dan apa yang masih perlu diuji di perangkat nyata.
