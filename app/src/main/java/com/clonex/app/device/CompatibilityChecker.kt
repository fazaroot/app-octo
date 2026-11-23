package com.clonex.app.device

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserManager
import com.clonex.app.provisioning.CloneXDeviceAdminReceiver

/**
 * Hasil pemeriksaan kompatibilitas perangkat.
 *
 * Semua pemeriksaan memakai API publik Android:
 * UserManager, DevicePolicyManager, dan PackageManager.
 * Tidak ada API tersembunyi di sini.
 */
data class CompatibilityStatus(
    val deviceInfo: DeviceInfo,
    val supportsManagedUsers: Boolean,
    val hasWorkProfile: Boolean,
    val workProfileCount: Int,
    val totalProfiles: Int,
    val isCurrentUserManagedProfile: Boolean,
    val isProfileOwner: Boolean,
    val isDeviceOwner: Boolean,
    val isAdminActive: Boolean,
    /** Work profile ada tetapi bukan milik CloneX, misalnya profil kantor. */
    val hasOtherProfileOwner: Boolean,
    /** True jika provisioning masih mungkin dilakukan. */
    val canProvision: Boolean,
    /** True jika provisioning sudah selesai dan CloneX adalah Profile Owner. */
    val provisioningDone: Boolean,
    val explanations: List<String>
)

class CompatibilityChecker(private val context: Context) {

    fun adminComponent(): ComponentName {
        return ComponentName(context, CloneXDeviceAdminReceiver::class.java)
    }

    fun check(): CompatibilityStatus {
        val packageManager = context.packageManager
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        val deviceInfo = try {
            DeviceDiagnostics.getDeviceInfo()
        } catch (_: Exception) {
            DeviceInfo(
                manufacturer = "Tidak diketahui",
                brand = "Tidak diketahui",
                model = "Tidak diketahui",
                device = "Tidak diketahui",
                androidRelease = "Tidak diketahui",
                sdkInt = 0,
                securityPatch = "Tidak diketahui",
                display = "",
                incremental = "",
                hyperOsVersion = null,
                miuiVersion = null,
                isXiaomi = false,
                isPocoM6 = false
            )
        }

        val supportsManagedUsers = try {
            packageManager.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)
        } catch (_: Exception) {
            false
        }

        val isCurrentUserManagedProfile = try {
            userManager.isManagedProfile
        } catch (_: Exception) {
            false
        }

        val profiles = try {
            userManager.userProfiles
        } catch (_: Exception) {
            emptyList()
        }
        val myHandle = Process.myUserHandle()
        val workProfiles = profiles.filter { it != myHandle }
        val hasWorkProfile = workProfiles.isNotEmpty()

        val isProfileOwner = try {
            devicePolicyManager.isProfileOwnerApp(context.packageName)
        } catch (_: Exception) {
            false
        }
        val isDeviceOwner = try {
            devicePolicyManager.isDeviceOwnerApp(context.packageName)
        } catch (_: Exception) {
            false
        }
        val isAdminActive = try {
            devicePolicyManager.isAdminActive(adminComponent())
        } catch (_: Exception) {
            false
        }

        // Heuristik yang jujur: jika ada profil tambahan tetapi CloneX
        // bukan Profile Owner, profil itu hampir pasti milik aplikasi
        // lain, misalnya profil kantor dari aplikasi MDM perusahaan.
        // Android hanya mengizinkan satu managed profile, jadi dalam
        // kondisi ini CloneX tidak bisa membuat profil kedua.
        val hasOtherProfileOwner = hasWorkProfile && !isProfileOwner
        val provisioningDone = isProfileOwner && hasWorkProfile
        val canProvision = supportsManagedUsers && !hasWorkProfile && !isProfileOwner

        val explanations = buildExplanations(
            deviceInfo = deviceInfo,
            supportsManagedUsers = supportsManagedUsers,
            hasWorkProfile = hasWorkProfile,
            isProfileOwner = isProfileOwner,
            hasOtherProfileOwner = hasOtherProfileOwner,
            canProvision = canProvision,
            provisioningDone = provisioningDone
        )

        return CompatibilityStatus(
            deviceInfo = deviceInfo,
            supportsManagedUsers = supportsManagedUsers,
            hasWorkProfile = hasWorkProfile,
            workProfileCount = workProfiles.size,
            totalProfiles = profiles.size,
            isCurrentUserManagedProfile = isCurrentUserManagedProfile,
            isProfileOwner = isProfileOwner,
            isDeviceOwner = isDeviceOwner,
            isAdminActive = isAdminActive,
            hasOtherProfileOwner = hasOtherProfileOwner,
            canProvision = canProvision,
            provisioningDone = provisioningDone,
            explanations = explanations
        )
    }

    private fun buildExplanations(
        deviceInfo: DeviceInfo,
        supportsManagedUsers: Boolean,
        hasWorkProfile: Boolean,
        isProfileOwner: Boolean,
        hasOtherProfileOwner: Boolean,
        canProvision: Boolean,
        provisioningDone: Boolean
    ): List<String> {
        val list = mutableListOf<String>()
        if (deviceInfo.isPocoM6) {
            list.add(
                "Target pengujian utama adalah POCO M6. Hasil di layar ini adalah " +
                    "hasil pemeriksaan nyata di perangkat ini, bukan asumsi dari " +
                    "model Xiaomi lain."
            )
        }
        if (!supportsManagedUsers) {
            if (deviceInfo.isXiaomi) {
                list.add(
                    "Perangkat Xiaomi ini tidak melaporkan dukungan managed profile " +
                        "(FEATURE_MANAGED_USERS), meskipun versi Androidnya memenuhi " +
                        "syarat. HyperOS pada model ini menonaktifkan fitur tersebut, " +
                        "jadi provisioning tidak bisa dilanjutkan. CloneX tetap bisa " +
                        "dipakai sebagai pengelola aplikasi profil utama, tetapi " +
                        "instance kedua lewat profil kerja tidak bisa dibuat di " +
                        "perangkat ini."
                )
            } else {
                list.add(
                    "Perangkat ini tidak melaporkan dukungan managed profile " +
                        "(FEATURE_MANAGED_USERS). Beberapa perangkat Xiaomi, Oppo, Vivo, " +
                        "Huawei, dan Android Go menonaktifkan fitur ini. CloneX tetap bisa " +
                        "dipakai sebagai pengelola aplikasi profil utama, tetapi instance " +
                        "kedua lewat profil kerja tidak bisa dibuat di perangkat ini."
                )
            }
        }
        if (provisioningDone) {
            list.add(
                "Profil kerja CloneX sudah aktif dan CloneX adalah Profile Owner. " +
                    "Kamu bisa mengaktifkan aplikasi ke profil kerja dan meluncurkannya " +
                    "dari dashboard."
            )
        } else if (hasOtherProfileOwner) {
            list.add(
                "Perangkat ini sudah memiliki profil kerja lain, kemungkinan profil " +
                    "kantor. Android hanya mengizinkan satu managed profile, jadi " +
                    "CloneX tidak bisa membuat profil kedua sebelum profil yang ada " +
                    "dihapus lewat Pengaturan sistem. Menghapus profil kerja akan " +
                    "menghapus seluruh data di dalamnya."
            )
        } else if (canProvision) {
            list.add(
                "Perangkat ini mendukung managed profile dan belum memiliki profil " +
                    "kerja. Kamu bisa lanjut ke layar Profil Kerja untuk memulai " +
                    "provisioning resmi. Proses ini meminta persetujuanmu dan " +
                    "dibuat oleh sistem Android, bukan secara diam-diam."
            )
            if (deviceInfo.isXiaomi) {
                list.add(
                    "Catatan Xiaomi: dukungan yang dilaporkan sistem belum menjamin " +
                        "provisioning berhasil. Sebagian perangkat HyperOS membatasi " +
                        "provisioning meskipun FEATURE_MANAGED_USERS tersedia. " +
                        "Keberhasilan hanya bisa dipastikan setelah mencoba alur " +
                        "resmi di layar Profil Kerja."
                )
            }
        } else if (hasWorkProfile && isProfileOwner) {
            list.add(
                "CloneX adalah Profile Owner, tetapi status profil belum terdeteksi " +
                    "penuh. Coba tekan Periksa Ulang. Jika masih seperti ini, buka " +
                    "Pengaturan sistem dan pastikan profil kerja masih ada."
            )
        }
        if (isProfileOwner) {
            list.add(
                "Status Profile Owner aktif. CloneX dapat mengelola aplikasi di " +
                    "profil kerja lewat API resmi DevicePolicyManager dan LauncherApps."
            )
        }
        return list
    }
}
