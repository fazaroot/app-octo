package com.clonex.app.data.repository

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.clonex.app.data.model.AppEntry
import com.clonex.app.profile.ProfileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hasil peluncuran aplikasi, agar UI bisa memberi pesan jujur
 * saat peluncuran gagal, bukan berpura-pura berhasil.
 */
sealed class LaunchResult {
    object Success : LaunchResult()
    data class Failed(val reason: String) : LaunchResult()
}

/**
 * Repository aplikasi berbasis LauncherApps resmi.
 *
 * CloneX hanya menampilkan aplikasi yang memang bisa diluncurkan
 * (launcher activity) di tiap profil, lalu meluncurkannya lewat
 * LauncherApps.startMainActivity dengan UserHandle yang benar.
 *
 * CloneX TIDAK menyalin APK, TIDAK mengubah package name, dan
 * TIDAK mengubah tanda tangan aplikasi lain.
 */
class AppRepository(private val context: Context) {

    private val profileManager = ProfileManager(context)

    private val launcherApps: LauncherApps
        get() = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val packageManager: PackageManager
        get() = context.packageManager

    private val devicePolicyManager: DevicePolicyManager
        get() = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    /**
     * Muat semua aplikasi launcher dari profil utama dan profil kerja.
     * Berjalan di Dispatchers.IO karena query LauncherApps bisa lambat
     * di perangkat dengan banyak aplikasi.
     */
    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val profiles = profileManager.getProfiles()
        val result = mutableListOf<AppEntry>()
        for (profile in profiles) {
            try {
                val activities = launcherApps.getActivityList(null, profile.userHandle)
                for (info in activities) {
                    val label = try {
                        info.label.toString()
                    } catch (_: Exception) {
                        info.applicationInfo.packageName
                    }
                    result.add(
                        AppEntry(
                            packageName = info.applicationInfo.packageName,
                            componentName = info.componentName,
                            label = label,
                            userHandle = profile.userHandle,
                            userSerial = profile.userSerial,
                            isWorkProfile = !profile.isMain
                        )
                    )
                }
            } catch (_: SecurityException) {
                // Profil tidak mengizinkan query dari aplikasi ini.
                // Dilewati dengan jujur; UI akan menunjukkan jumlah
                // aplikasi yang berhasil dimuat.
                continue
            } catch (_: Exception) {
                continue
            }
        }
        result.sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
    }

    /**
     * Ambil ikon aplikasi dengan badge profil (ikon kerja).
     * Mengembalikan null jika ikon tidak bisa dimuat.
     */
    fun getBadgedIcon(entry: AppEntry): Drawable? {
        return try {
            val appInfo = launcherApps.getApplicationInfo(
                entry.packageName,
                0,
                entry.userHandle
            ) ?: return null
            val icon = appInfo.loadIcon(packageManager)
            try {
                packageManager.getUserBadgedIcon(icon, entry.userHandle)
            } catch (_: Exception) {
                icon
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Luncurkan aplikasi di profilnya.
     */
    fun launch(entry: AppEntry): LaunchResult {
        return try {
            launcherApps.startMainActivity(entry.componentName, entry.userHandle, null, null)
            LaunchResult.Success
        } catch (e: SecurityException) {
            LaunchResult.Failed(
                "Tidak diizinkan meluncurkan ${entry.label} di ${entry.profileLabel}. " +
                    "Aplikasi mungkin dibatasi oleh kebijakan profil. (${e.message})"
            )
        } catch (e: IllegalArgumentException) {
            LaunchResult.Failed(
                "Aplikasi ${entry.label} tidak ditemukan di ${entry.profileLabel}. " +
                    "Mungkin aplikasi sudah dihapus atau dinonaktifkan."
            )
        } catch (e: Exception) {
            LaunchResult.Failed(
                "Gagal meluncurkan ${entry.label}: ${e.message ?: "kesalahan tidak diketahui"}"
            )
        }
    }

    /**
     * Aktifkan aplikasi sistem ke profil kerja.
     *
     * Hanya berfungsi jika CloneX adalah Profile Owner dan target
     * adalah aplikasi sistem. Untuk aplikasi pengguna biasa, jalur
     * resmi adalah menginstall aplikasi itu di profil kerja lewat
     * Play Store di profil kerja (Managed Google Play) atau lewat
     * PackageInstaller di dalam profil kerja.
     *
     * Metode ini TIDAK memakai API tersembunyi. Jika perangkat atau
     * kebijakan tidak mengizinkan, metode ini mengembalikan false
     * dan UI harus menjelaskannya, bukan mengklaim berhasil.
     *
     * @return true jika sistem menerima permintaan pengaktifan.
     */
    fun enableSystemAppInWorkProfile(packageName: String): Boolean {
        return try {
            devicePolicyManager.enableSystemApp(
                ComponentName(context, com.clonex.app.provisioning.CloneXDeviceAdminReceiver::class.java),
                packageName
            )
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Cek apakah CloneX saat ini adalah Profile Owner.
     */
    fun isProfileOwner(): Boolean {
        return try {
            devicePolicyManager.isProfileOwnerApp(context.packageName)
        } catch (_: Exception) {
            false
        }
    }

    fun getProfiles() = profileManager.getProfiles()
}
