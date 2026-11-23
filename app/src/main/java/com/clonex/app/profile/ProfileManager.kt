package com.clonex.app.profile

import android.content.Context
import android.os.Process
import android.os.UserHandle
import android.os.UserManager

/**
 * Informasi satu profil pengguna yang dikenali CloneX.
 */
data class ProfileEntry(
    val userHandle: UserHandle,
    val userSerial: Long,
    val isMain: Boolean,
    val label: String
)

/**
 * Pembungkus UserManager untuk daftar profil.
 *
 * Hanya memakai API publik. Nomor seri pengguna dipakai sebagai
 * identitas stabil untuk metadata Room, karena UserHandle sendiri
 * tidak cocok disimpan sebagai kunci database.
 */
class ProfileManager(private val context: Context) {

    private val userManager: UserManager
        get() = context.getSystemService(Context.USER_SERVICE) as UserManager

    fun getSerialForUser(userHandle: UserHandle): Long {
        return try {
            userManager.getSerialNumberForUser(userHandle)
        } catch (_: Exception) {
            // Fallback yang jujur: jika nomor seri tidak bisa dibaca,
            // pakai hashCode sebagai pembeda. Ini tetap konsisten dalam
            // satu sesi proses, tetapi metadata Room untuk entri ini
            // bisa tidak cocok setelah restart. Kasus ini jarang terjadi
            // di perangkat standar.
            userHandle.hashCode().toLong()
        }
    }

    fun getProfiles(): List<ProfileEntry> {
        val myHandle = Process.myUserHandle()
        val profiles = try {
            userManager.userProfiles
        } catch (_: Exception) {
            listOf(myHandle)
        }
        return profiles.map { handle ->
            val isMain = handle == myHandle
            ProfileEntry(
                userHandle = handle,
                userSerial = getSerialForUser(handle),
                isMain = isMain,
                label = if (isMain) "Profil Utama" else "Profil Kerja"
            )
        }
    }

    fun getWorkProfile(): ProfileEntry? {
        return getProfiles().firstOrNull { !it.isMain }
    }

    fun hasWorkProfile(): Boolean {
        return getWorkProfile() != null
    }
}
