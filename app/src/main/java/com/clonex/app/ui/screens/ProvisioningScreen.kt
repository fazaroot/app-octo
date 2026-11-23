package com.clonex.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.clonex.app.provisioning.ProvisioningActivityResult
import com.clonex.app.provisioning.ProvisioningManager
import com.clonex.app.ui.components.StatusCard
import com.clonex.app.ui.components.StatusKind
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvisioningScreen() {
    val context = LocalContext.current
    val provisioningManager = remember { ProvisioningManager(context) }
    val compatibilityChecker = remember { CompatibilityChecker(context) }
    var status by remember { mutableStateOf<CompatibilityStatus?>(null) }
    var lastMessage by remember { mutableStateOf<String?>(null) }
    var broadcastReceived by remember { mutableStateOf(false) }
    var broadcastTime by remember { mutableStateOf(0L) }

    fun refresh() {
        status = compatibilityChecker.check()
        broadcastReceived = provisioningManager.wasProvisioningCompleteBroadcastReceived()
        broadcastTime = provisioningManager.provisioningCompleteTime()
    }

    val provisioningLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (val parsed = provisioningManager.parseActivityResult(result.resultCode)) {
            is ProvisioningActivityResult.Success -> {
                lastMessage = "Alur provisioning selesai dengan hasil OK. " +
                    "Status akhir masih perlu diperiksa: sistem perlu mengonfirmasi " +
                    "bahwa CloneX benar-benar menjadi Profile Owner. Tekan Periksa Status."
            }
            is ProvisioningActivityResult.Cancelled -> {
                lastMessage = "Provisioning dibatalkan. Tidak ada perubahan yang dilakukan. " +
                    "Kamu bisa mencoba lagi kapan saja."
            }
            is ProvisioningActivityResult.Failed -> {
                lastMessage = parsed.reason
            }
        }
        refresh()
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profil Kerja") })
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
                text = "Provisioning resmi Android",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Profil kerja dibuat oleh sistem Android setelah kamu menyetujuinya. " +
                    "CloneX tidak bisa membuat profil secara diam-diam, tidak memakai root, " +
                    "dan tidak mengubah APK atau tanda tangan aplikasi lain. " +
                    "Maksimal satu aplikasi mendapat dua instance: satu di Profil Utama " +
                    "dan satu di Profil Kerja.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val current = status
            if (current == null) {
                Text("Memeriksa status…")
            } else {
                StatusCard(
                    title = "Status provisioning",
                    value = when {
                        current.provisioningDone -> "Selesai, CloneX adalah Profile Owner"
                        current.hasOtherProfileOwner -> "Tidak bisa, sudah ada profil kerja lain"
                        !current.supportsManagedUsers -> "Tidak didukung perangkat ini"
                        current.canProvision -> "Siap dimulai"
                        else -> "Belum selesai"
                    },
                    kind = when {
                        current.provisioningDone -> StatusKind.OK
                        current.canProvision -> StatusKind.INFO
                        else -> StatusKind.WARNING
                    },
                    description = when {
                        current.provisioningDone -> "Kamu bisa kembali ke Beranda untuk melihat aplikasi Profil Kerja."
                        current.hasOtherProfileOwner -> "Android hanya mengizinkan satu managed profile. Hapus profil yang ada lewat Pengaturan sistem jika kamu yakin ingin menggantinya. Tindakan itu menghapus seluruh data di profil tersebut."
                        !current.supportsManagedUsers -> "Perangkat ini tidak melaporkan dukungan FEATURE_MANAGED_USERS."
                        current.canProvision -> "Tekan tombol di bawah untuk memulai alur persetujuan sistem."
                        else -> "Periksa kembali status setelah beberapa saat."
                    }
                )
                StatusCard(
                    title = "Callback provisioning complete",
                    value = if (broadcastReceived) {
                        "Diterima" + if (broadcastTime > 0L) {
                            " pada " + DateFormat.getDateTimeInstance().format(Date(broadcastTime))
                        } else {
                            ""
                        }
                    } else {
                        "Belum diterima"
                    },
                    kind = if (broadcastReceived) StatusKind.OK else StatusKind.INFO,
                    description = "Callback sistem ACTION_PROFILE_PROVISIONING_COMPLETE. " +
                        "Ini adalah konfirmasi kedua, selain pemeriksaan Profile Owner."
                )
            }

            if (lastMessage != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Hasil terakhir",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lastMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            val provisioningIntent = remember(current) {
                if (current?.canProvision == true) {
                    provisioningManager.createProvisioningIntent()
                } else {
                    null
                }
            }

            Button(
                onClick = {
                    val intent = provisioningManager.createProvisioningIntent()
                    if (intent == null) {
                        lastMessage = "Tidak ada activity sistem yang bisa menangani intent " +
                            "provisioning di perangkat ini. Perangkat mungkin tidak " +
                            "mendukung managed profile, atau produsen menonaktifkannya."
                    } else {
                        provisioningLauncher.launch(intent)
                    }
                },
                enabled = current?.canProvision == true && provisioningIntent != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mulai Provisioning Profil Kerja")
            }
            if (current?.canProvision == true && provisioningIntent == null) {
                Text(
                    text = "Tombol tidak aktif karena sistem tidak menyediakan activity " +
                        "provisioning untuk intent resmi ini. Ini adalah batasan perangkat, " +
                        "bukan kesalahan CloneX.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedButton(
                onClick = { refresh() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Periksa Status")
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Pengujian lewat adb (hanya untuk pengembang)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Untuk pengujian di perangkat sendiri, provisioning juga bisa " +
                            "diuji lewat perintah adb dpm set-profile-owner. Perintah ini " +
                            "membutuhkan perangkat yang belum memiliki Profile Owner dan " +
                            "akses adb yang sudah diizinkan. Langkah lengkap ada di README. " +
                            "CloneX tidak menyediakan tombol yang menjalankan perintah ini " +
                            "secara otomatis, karena itu akan melewati persetujuan pengguna.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Yang tidak bisa dilakukan CloneX",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Membuat lebih dari satu profil kerja.\n" +
                            "• Membuat instance ketiga, keempat, dan seterusnya untuk satu aplikasi.\n" +
                            "• Mengubah nama atau ikon aplikasi di launcher sistem.\n" +
                            "• Menyalin atau mengubah APK dan tanda tangan aplikasi lain.\n" +
                            "• Membuat profil tanpa persetujuan pengguna.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
