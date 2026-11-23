package com.clonex.app.data.model

import android.content.ComponentName
import android.os.UserHandle

/**
 * Satu aplikasi yang bisa diluncurkan di satu profil.
 *
 * Aplikasi yang sama di profil utama dan profil kerja adalah dua
 * AppEntry berbeda, dibedakan oleh userHandle dan userSerial.
 * CloneX tidak mengubah identitas aplikasi ini; ia hanya
 * menampilkan dan meluncurkannya lewat LauncherApps resmi.
 */
data class AppEntry(
    val packageName: String,
    val componentName: ComponentName,
    val label: String,
    val userHandle: UserHandle,
    val userSerial: Long,
    val isWorkProfile: Boolean
) {
    /** Kunci stabil untuk metadata Room dan UI. */
    val key: String get() = "$packageName|$userSerial"

    val profileLabel: String get() = if (isWorkProfile) "Profil Kerja" else "Profil Utama"
}
