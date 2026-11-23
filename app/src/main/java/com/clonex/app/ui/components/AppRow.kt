package com.clonex.app.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.clonex.app.data.db.ManagedAppEntity
import com.clonex.app.data.model.AppEntry
import java.io.File

/**
 * Satu baris aplikasi di dashboard.
 *
 * Menampilkan nama tampilan (custom jika ada, jika tidak nama asli),
 * package name, badge profil, ikon ber-badge atau ikon kustom,
 * tombol favorit, tombol edit, dan seluruh baris bisa ditekan
 * untuk meluncurkan aplikasi.
 *
 * Ikon kustom hanya tampil di sini, di dashboard CloneX.
 */
@Composable
fun AppRow(
    entry: AppEntry,
    custom: ManagedAppEntity?,
    systemIcon: Drawable?,
    onLaunch: (AppEntry) -> Unit,
    onToggleFavorite: (AppEntry) -> Unit,
    onEdit: (AppEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = custom?.customName?.takeIf { it.isNotBlank() } ?: entry.label
    val customIconFile: File? = remember(custom?.customIconPath) {
        custom?.customIconPath?.let { File(it) }?.takeIf { it.exists() }
    }
    val systemBitmap = remember(systemIcon) {
        try {
            systemIcon?.toBitmap(96, 96)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    Card(
        onClick = { onLaunch(entry) },
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (customIconFile != null) {
                AsyncImage(
                    model = customIconFile,
                    contentDescription = "Ikon kustom $displayName",
                    modifier = Modifier.size(48.dp)
                )
            } else if (systemBitmap != null) {
                Image(
                    bitmap = systemBitmap,
                    contentDescription = "Ikon $displayName",
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = entry.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { onLaunch(entry) },
                        label = { Text(entry.profileLabel) }
                    )
                    if (custom?.customName != null || custom?.customIconPath != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nama asli: ${entry.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = { onToggleFavorite(entry) }) {
                Icon(
                    imageVector = if (custom?.isFavorite == true) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (custom?.isFavorite == true) {
                        "Hapus dari favorit"
                    } else {
                        "Tandai sebagai favorit"
                    }
                )
            }
            IconButton(onClick = { onEdit(entry) }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Ubah nama dan ikon tampilan"
                )
            }
        }
    }
}
