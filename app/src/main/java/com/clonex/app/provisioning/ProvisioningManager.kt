package com.clonex.app.provisioning

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Hasil alur provisioning dari Activity.
 *
 * Perhatikan: RESULT_OK dari intent provisioning tidak selalu berarti
 * profil sudah siap dipakai. Status akhir yang benar dicek lewat
 * CompatibilityChecker (isProfileOwnerApp dan keberadaan work profile)
 * dan lewat callback ACTION_PROFILE_PROVISIONING_COMPLETE di receiver.
 * UI harus menampilkan kedua lapis pemeriksaan ini dengan jujur.
 */
sealed class ProvisioningActivityResult {
    object Success : ProvisioningActivityResult()
    object Cancelled : ProvisioningActivityResult()
    data class Failed(val reason: String) : ProvisioningActivityResult()
}

class ProvisioningManager(private val context: Context) {

    fun adminComponent(): ComponentName {
        return ComponentName(context, CloneXDeviceAdminReceiver::class.java)
    }

    /**
     * Buat intent provisioning managed profile resmi.
     *
     * Memakai DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE,
     * API publik Android. Intent ini menampilkan alur persetujuan
     * sistem; pengguna bisa membatalkannya kapan saja.
     *
     * @return Intent provisioning, atau null jika perangkat tidak
     * mendukung atau intent tidak bisa dibuat.
     */
    @Suppress("DEPRECATION")
    fun createProvisioningIntent(): Intent? {
        return try {
            val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
            intent.putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                adminComponent()
            )
            // Pastikan ada activity sistem yang bisa menangani intent ini.
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo == null) null else intent
        } catch (_: Exception) {
            null
        }
    }

    fun parseActivityResult(resultCode: Int): ProvisioningActivityResult {
        return when (resultCode) {
            Activity.RESULT_OK -> ProvisioningActivityResult.Success
            Activity.RESULT_CANCELED -> ProvisioningActivityResult.Cancelled
            else -> ProvisioningActivityResult.Failed(
                "Provisioning selesai dengan kode hasil tidak dikenal: $resultCode. " +
                    "Periksa status di layar Kompatibilitas."
            )
        }
    }

    fun wasProvisioningCompleteBroadcastReceived(): Boolean {
        return context.getSharedPreferences(
            CloneXDeviceAdminReceiver.PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(CloneXDeviceAdminReceiver.KEY_PROVISIONING_COMPLETE, false)
    }

    fun provisioningCompleteTime(): Long {
        return context.getSharedPreferences(
            CloneXDeviceAdminReceiver.PREFS_NAME,
            Context.MODE_PRIVATE
        ).getLong(CloneXDeviceAdminReceiver.KEY_PROVISIONING_COMPLETE_TIME, 0L)
    }
}
