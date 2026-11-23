package com.clonex.app.data.repository

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Penyimpanan ikon kustom di storage privat aplikasi.
 *
 * Ikon dipilih pengguna lewat Storage Access Framework (GetContent),
 * lalu disalin ke folder privat CloneX. File asli tidak diubah,
 * dan ikon ini hanya dipakai di dashboard CloneX, bukan untuk
 * mengubah ikon aplikasi lain di launcher sistem.
 */
class IconStorage(private val context: Context) {

    private fun iconsDir(): File {
        return File(context.filesDir, "custom_icons").apply { mkdirs() }
    }

    private fun fileNameFor(packageName: String, userSerial: Long, extension: String): String {
        val safePackage = packageName.replace(".", "_")
        return "${safePackage}_${userSerial}.$extension"
    }

    /**
     * Salin gambar dari [uri] ke storage privat.
     * Mengembalikan path absolut file hasil salinan, atau null jika gagal.
     */
    fun saveIcon(uri: Uri, packageName: String, userSerial: Long): String? {
        return try {
            val mime = context.contentResolver.getType(uri) ?: "image/png"
            val extension = when {
                mime.contains("jpeg", ignoreCase = true) || mime.contains("jpg", ignoreCase = true) -> "jpg"
                mime.contains("webp", ignoreCase = true) -> "webp"
                else -> "png"
            }
            val target = File(iconsDir(), fileNameFor(packageName, userSerial, extension))
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            target.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun getIconFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return if (file.exists()) file else null
    }

    fun deleteIcon(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            File(path).delete()
        } catch (_: Exception) {
            false
        }
    }
}
