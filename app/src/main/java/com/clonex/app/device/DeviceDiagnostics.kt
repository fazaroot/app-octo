package com.clonex.app.device

import android.os.Build
import java.io.File

/**
 * Informasi perangkat hasil deteksi nyata di perangkat.
 *
 * Semua field berasal dari API publik android.os.Build, ditambah
 * pembacaan file build.prop sistem yang bersifat best-effort.
 * Tidak ada refleksi ke API tersembunyi di sini.
 *
 * HyperOS tidak menyediakan API publik untuk versi sistemnya, jadi
 * deteksi HyperOS dilakukan dengan cara berikut, sesuai urutan:
 * 1. Baca ro.hyperos.version dari build.prop sistem.
 * 2. Jika tidak ada, baca ro.miui.ui.version.name. Pada HyperOS nilai
 *    ini biasanya diawali "OS", misalnya "OS2.0".
 * 3. Jika tidak ada, periksa Build.DISPLAY yang di beberapa perangkat
 *    Xiaomi berisi kata "HyperOS".
 * Jika ketiganya gagal, hyperOsVersion bernilai null dan UI harus
 * menampilkan "Tidak terdeteksi", bukan mengarang versi.
 */
data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val androidRelease: String,
    val sdkInt: Int,
    val securityPatch: String,
    val display: String,
    val incremental: String,
    val hyperOsVersion: String?,
    val miuiVersion: String?,
    val isXiaomi: Boolean,
    val isPocoM6: Boolean
) {
    val hyperOsLabel: String
        get() = hyperOsVersion ?: "Tidak terdeteksi"

    val miuiLabel: String
        get() = miuiVersion ?: "Tidak terdeteksi"

    val androidLabel: String
        get() = "Android $androidRelease (API $sdkInt)"
}

object DeviceDiagnostics {

    private val buildPropPaths = listOf(
        "/system/build.prop",
        "/system_ext/build.prop",
        "/product/build.prop",
        "/vendor/build.prop"
    )

    fun getDeviceInfo(): DeviceInfo {
        val manufacturer = Build.MANUFACTURER ?: "Tidak diketahui"
        val brand = Build.BRAND ?: "Tidak diketahui"
        val model = Build.MODEL ?: "Tidak diketahui"
        val device = Build.DEVICE ?: "Tidak diketahui"
        val androidRelease = Build.VERSION.RELEASE ?: "Tidak diketahui"
        val sdkInt = Build.VERSION.SDK_INT
        val securityPatch = try {
            Build.VERSION.SECURITY_PATCH ?: "Tidak diketahui"
        } catch (_: Exception) {
            "Tidak diketahui"
        }
        val display = Build.DISPLAY ?: ""
        val incremental = Build.VERSION.INCREMENTAL ?: ""

        val hyperOsFromProp = readBuildPropValue("ro.hyperos.version")
            ?: readBuildPropValue("ro.hyperos.version.name")
        val miuiVersion = readBuildPropValue("ro.miui.ui.version.name")

        val hyperOsVersion = when {
            !hyperOsFromProp.isNullOrBlank() -> hyperOsFromProp.trim()
            !miuiVersion.isNullOrBlank() && miuiVersion.trim().startsWith("OS") ->
                miuiVersion.trim()
            display.contains("HyperOS", ignoreCase = true) ->
                "Terdeteksi dari Build.DISPLAY (versi tidak diketahui)"
            else -> null
        }

        val isXiaomi = manufacturer.equals("Xiaomi", ignoreCase = true) ||
            manufacturer.equals("POCO", ignoreCase = true) ||
            brand.equals("Xiaomi", ignoreCase = true) ||
            brand.equals("POCO", ignoreCase = true) ||
            model.contains("POCO", ignoreCase = true) ||
            model.contains("Redmi", ignoreCase = true) ||
            model.contains("Mi ", ignoreCase = true)

        val isPocoM6 = model.equals("2404APC5FG", ignoreCase = true) ||
            model.contains("POCO M6", ignoreCase = true)

        return DeviceInfo(
            manufacturer = manufacturer,
            brand = brand,
            model = model,
            device = device,
            androidRelease = androidRelease,
            sdkInt = sdkInt,
            securityPatch = securityPatch,
            display = display,
            incremental = incremental,
            hyperOsVersion = hyperOsVersion,
            miuiVersion = miuiVersion?.trim(),
            isXiaomi = isXiaomi,
            isPocoM6 = isPocoM6
        )
    }

    /**
     * Baca satu kunci dari build.prop sistem. Mengembalikan null jika
     * file tidak bisa dibaca atau kunci tidak ditemukan. Ini pembacaan
     * file biasa, bukan API tersembunyi, dan selalu dibungkus try-catch
     * agar kegagalan tidak merusak pemeriksaan lain.
     */
    private fun readBuildPropValue(key: String): String? {
        for (path in buildPropPaths) {
            try {
                val file = File(path)
                if (!file.canRead()) continue
                file.useLines { lines ->
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                        if (!trimmed.startsWith(key + "=")) continue
                        val value = trimmed.substringAfter("=", "").trim()
                        if (value.isNotEmpty()) return value
                    }
                }
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * Ringkasan teks diagnostik untuk disalin pengguna. Berguna saat
     * melaporkan hasil pemeriksaan dari perangkat Xiaomi yang berbeda.
     */
    fun buildDiagnosticText(info: DeviceInfo, status: CompatibilityStatus): String {
        return buildString {
            appendLine("Diagnostik CloneX")
            appendLine("Produsen: ${info.manufacturer} (${info.brand})")
            appendLine("Model: ${info.model}")
            appendLine("Perangkat: ${info.device}")
            appendLine("Android: ${info.androidLabel}")
            appendLine("Patch keamanan: ${info.securityPatch}")
            appendLine("HyperOS: ${info.hyperOsLabel}")
            appendLine("MIUI: ${info.miuiLabel}")
            appendLine("POCO M6: ${if (info.isPocoM6) "Ya" else "Tidak"}")
            appendLine("FEATURE_MANAGED_USERS: ${if (status.supportsManagedUsers) "Ya" else "Tidak"}")
            appendLine("Profil kerja terdeteksi: ${if (status.hasWorkProfile) "Ya (${status.workProfileCount})" else "Tidak"}")
            appendLine("Total profil: ${status.totalProfiles}")
            appendLine("Profile Owner CloneX: ${if (status.isProfileOwner) "Ya" else "Tidak"}")
            appendLine("Bisa provisioning: ${if (status.canProvision) "Ya" else "Tidak"}")
            if (status.explanations.isNotEmpty()) {
                appendLine("Penjelasan:")
                for (item in status.explanations) {
                    appendLine("- $item")
                }
            }
        }
    }
}
