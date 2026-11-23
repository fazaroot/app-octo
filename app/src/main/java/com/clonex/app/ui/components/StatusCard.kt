package com.clonex.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class StatusKind {
    OK, WARNING, INFO
}

/**
 * Kartu status kecil untuk layar Kompatibilitas dan Profil Kerja.
 * Menampilkan satu fakta dengan label Ya atau Tidak, tanpa
 * mengklaim lebih dari yang diperiksa.
 */
@Composable
fun StatusCard(
    title: String,
    value: String,
    kind: StatusKind,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    val icon = when (kind) {
        StatusKind.OK -> Icons.Filled.CheckCircle
        StatusKind.WARNING -> Icons.Filled.Error
        StatusKind.INFO -> Icons.Filled.Info
    }
    val tint = when (kind) {
        StatusKind.OK -> MaterialTheme.colorScheme.primary
        StatusKind.WARNING -> MaterialTheme.colorScheme.error
        StatusKind.INFO -> MaterialTheme.colorScheme.secondary
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
