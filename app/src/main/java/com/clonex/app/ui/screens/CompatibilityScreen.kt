package com.clonex.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clonex.app.device.CompatibilityChecker
import com.clonex.app.device.CompatibilityStatus
import com.clonex.app.device.DeviceDiagnostics
import com.clonex.app.ui.components.StatusCard
import com.clonex.app.ui.components.StatusKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityScreen(
    onOpenProvisioning: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<CompatibilityStatus?>(null) }

    fun refresh() {
        status = CompatibilityChecker(context).check()
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Kompatibilitas") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Status perangkat",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Pemeriksaan ini memakai API publik Android. Hasilnya adalah " +
                    "kondisi perangkat yang sebenarnya saat ini, bukan asumsi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val current = status
            if (current == null) {
                Text("Memeriksa perangkat…")
            } else {
                val info = current.deviceInfo
                Text(
                    text = "Diagnostik perangkat",
                    style = MaterialTheme.typography.titleMedium
                )
                StatusCard(
                    title = "Perangkat",
                    value = "${info.manufacturer} ${info.model}",
                    kind = if (info.isPocoM6) StatusKind.OK else StatusKind.INFO,
                    description = if (info.isPocoM6) {
                        "Brand: ${info.brand}. Codename perangkat: ${info.device}. " +
                            "Ini adalah target pengujian utama POCO M6."
                    } else {
                        "Brand: ${info.brand}. Codename perangkat: ${info.device}. " +
                            "Bukan POCO M6, hasil tetap ditampilkan apa adanya."
                    }
                )
                StatusCard(
                    title = "Versi Android aktual",
                    value = info.androidLabel,
                    kind = StatusKind.INFO,
                    description = "Patch keamanan: ${info.securityPatch}. " +
                        "Build: ${info.display.ifBlank { "Tidak diketahui" }}."
                )
                StatusCard(
                    title = "Versi HyperOS",
                    value = info.hyperOsLabel,
                    kind = if (info.hyperOsVersion != null) StatusKind.INFO else StatusKind.WARNING,
                    description = if (info.hyperOsVersion != null) {
                        "MIUI: ${info.miuiLabel}. Deteksi HyperOS bersifat best-effort " +
                            "dari build.prop dan Build.DISPLAY, bukan API resmi."
                    } else {
                        "Versi HyperOS tidak terdeteksi dari build.prop maupun " +
                            "Build.DISPLAY. Ini tidak otomatis berarti perangkat " +
                            "tidak memakai HyperOS, hanya versinya tidak bisa dibaca."
                    }
                )
                Text(
                    text = "Pemeriksaan provisioning",
                    style = MaterialTheme.typography.titleMedium
                )
                StatusCard(
                    title = "Dukungan managed profile",
                    value = if (current.supportsManagedUsers) "Ya" else "Tidak",
                    kind = if (current.supportsManagedUsers) StatusKind.OK else StatusKind.WARNING,
                    description = "Fitur sistem FEATURE_MANAGED_USERS. Jika Tidak, profil kerja " +
                        "tidak bisa dibuat di perangkat ini."
                )
                StatusCard(
                    title = "Profil kerja terdeteksi",
                    value = if (current.hasWorkProfile) {
                        "Ya (${current.workProfileCount} profil tambahan)"
                    } else {
                        "Tidak"
                    },
                    kind = if (current.hasWorkProfile) StatusKind.INFO else StatusKind.INFO,
                    description = "Jumlah total profil yang terlihat: ${current.totalProfiles}."
                )
                StatusCard(
                    title = "Profil saat ini adalah managed profile",
                    value = if (current.isCurrentUserManagedProfile) "Ya" else "Tidak",
                    kind = StatusKind.INFO,
                    description = "Jika Ya, berarti CloneX sedang berjalan di dalam " +
                        "profil kerja, bukan di profil utama."
                )
                StatusCard(
                    title = "CloneX sebagai Profile Owner",
                    value = if (current.isProfileOwner) "Ya" else "Tidak",
                    kind = if (current.isProfileOwner) StatusKind.OK else StatusKind.INFO,
                    description = "Peran ini aktif hanya setelah provisioning resmi disetujui."
                )
                StatusCard(
                    title = "Device Admin aktif",
                    value = if (current.isAdminActive) "Ya" else "Tidak",
                    kind = if (current.isAdminActive) StatusKind.OK else StatusKind.INFO,
                    description = "Menunjukkan apakah penerima Device Admin CloneX sedang aktif."
                )
                StatusCard(
                    title = "Profil kerja milik aplikasi lain",
                    value = if (current.hasOtherProfileOwner) "Ya" else "Tidak",
                    kind = if (current.hasOtherProfileOwner) StatusKind.WARNING else StatusKind.OK,
                    description = if (current.hasOtherProfileOwner) {
                        "Perangkat sudah punya profil kerja, kemungkinan profil kantor. " +
                            "Android hanya mengizinkan satu managed profile."
                    } else {
                        "Tidak ada indikasi profil kerja milik aplikasi lain."
                    }
                )
                StatusCard(
                    title = "Bisa melakukan provisioning",
                    value = if (current.canProvision) {
                        "Ya"
                    } else if (current.provisioningDone) {
                        "Sudah selesai"
                    } else {
                        "Tidak saat ini"
                    },
                    kind = if (current.canProvision || current.provisioningDone) {
                        StatusKind.OK
                    } else {
                        StatusKind.WARNING
                    },
                    description = if (current.provisioningDone) {
                        "Profil kerja CloneX sudah aktif."
                    } else if (!current.canProvision) {
                        "Lihat penjelasan di bawah untuk alasannya."
                    } else {
                        "Perangkat mendukung dan belum memiliki profil kerja."
                    }
                )

                if (current.explanations.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Penjelasan",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            for (explanation in current.explanations) {
                                Text(
                                    text = "• $explanation",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                if (!current.supportsManagedUsers) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Tidak kompatibel untuk provisioning",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Perangkat ini tidak bisa membuat profil kerja " +
                                    "karena FEATURE_MANAGED_USERS tidak tersedia. " +
                                    "CloneX tidak mengklaim provisioning berhasil di " +
                                    "perangkat ini. Kamu tetap bisa memakai dashboard " +
                                    "untuk aplikasi profil utama.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { refresh() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Periksa Ulang")
                }
                OutlinedButton(
                    onClick = {
                        val text = DeviceDiagnostics.buildDiagnosticText(
                            current.deviceInfo,
                            current
                        )
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as? ClipboardManager
                        if (clipboard != null) {
                            val clip = ClipData.newPlainText("Diagnostik CloneX", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(
                                context,
                                "Hasil diagnostik disalin",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Clipboard tidak tersedia",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salin Hasil Diagnostik")
                }
                if (current.canProvision) {
                    Button(
                        onClick = onOpenProvisioning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lanjut ke Provisioning Profil Kerja")
                    }
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buka Pengaturan Sistem")
                }
                Text(
                    text = "Catatan: menghapus profil kerja lewat Pengaturan sistem akan " +
                        "menghapus seluruh data di dalam profil kerja tersebut. " +
                        "CloneX tidak pernah menghapus profil secara diam-diam.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
