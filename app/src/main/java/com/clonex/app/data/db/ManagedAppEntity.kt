package com.clonex.app.data.db

import androidx.room.Entity

/**
 * Metadata kustom untuk satu entri aplikasi di dashboard CloneX.
 *
 * Kunci utama adalah kombinasi packageName dan userSerial, karena
 * aplikasi yang sama bisa muncul dua kali: satu di profil utama
 * dan satu di profil kerja. Keduanya adalah entri yang berbeda.
 *
 * PENTING: customName dan customIconPath HANYA berlaku di dashboard
 * CloneX. CloneX tidak mengubah nama atau ikon aplikasi di launcher
 * sistem, dan tidak mengubah APK atau tanda tangan aplikasi target.
 */
@Entity(
    tableName = "managed_apps",
    primaryKeys = ["packageName", "userSerial"]
)
data class ManagedAppEntity(
    val packageName: String,
    val userSerial: Long,
    val customName: String? = null,
    val customIconPath: String? = null,
    val isFavorite: Boolean = false,
    val lastLaunched: Long? = null
)
