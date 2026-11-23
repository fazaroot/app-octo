package com.clonex.app.provisioning

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Penerima Device Admin CloneX.
 *
 * Peran ini hanya aktif setelah pengguna menyetujui provisioning
 * resmi lewat sistem Android. Receiver ini tidak melakukan
 * pemantauan, penguncian, atau penghapusan data.
 *
 * Saat provisioning selesai, sistem mengirim
 * ACTION_PROFILE_PROVISIONING_COMPLETE. Receiver mencatat status
 * itu ke SharedPreferences lokal agar layar Provisioning bisa
 * menampilkan hasil yang benar, bukan asumsi.
 */
class CloneXDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "CloneXAdmin"
        const val PREFS_NAME = "clonex_provisioning"
        const val KEY_PROVISIONING_COMPLETE = "provisioning_complete"
        const val KEY_PROVISIONING_COMPLETE_TIME = "provisioning_complete_time"
        const val KEY_DEVICE_ADMIN_ENABLED = "device_admin_enabled"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device Admin diaktifkan")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEVICE_ADMIN_ENABLED, true)
            .apply()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "Device Admin dinonaktifkan")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEVICE_ADMIN_ENABLED, false)
            .putBoolean(KEY_PROVISIONING_COMPLETE, false)
            .apply()
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i(TAG, "Provisioning profil kerja selesai")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PROVISIONING_COMPLETE, true)
            .putLong(KEY_PROVISIONING_COMPLETE_TIME, System.currentTimeMillis())
            .apply()
    }
}
