package com.clonex.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clonex.app.data.model.AppEntry
import com.clonex.app.ui.components.AppRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenProvisioning: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingEntry by remember { mutableStateOf<AppEntry?>(null) }
    var customNameText by remember { mutableStateOf("") }

    val pickIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val entry = editingEntry
        if (uri != null && entry != null) {
            viewModel.saveCustomIcon(entry, uri)
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("CloneX") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Muat ulang daftar aplikasi"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Dashboard aplikasi",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Satu aplikasi bisa muncul dua kali: satu di Profil Utama dan satu di Profil Kerja. " +
                    "Nama dan ikon kustom hanya berlaku di dashboard ini, bukan mengubah launcher sistem.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (!state.hasWorkProfile && !state.isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Profil kerja belum aktif",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Saat ini hanya aplikasi Profil Utama yang ditampilkan. " +
                                "Aktifkan profil kerja resmi untuk mendapatkan instance kedua.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onOpenProvisioning) {
                            Text("Buka Pengaturan Profil Kerja")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.setQuery(it) },
                label = { Text("Cari nama aplikasi atau package") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filter == ProfileFilter.SEMUA,
                    onClick = { viewModel.setFilter(ProfileFilter.SEMUA) },
                    label = { Text("Semua") }
                )
                FilterChip(
                    selected = state.filter == ProfileFilter.UTAMA,
                    onClick = { viewModel.setFilter(ProfileFilter.UTAMA) },
                    label = { Text("Utama") }
                )
                FilterChip(
                    selected = state.filter == ProfileFilter.KERJA,
                    onClick = { viewModel.setFilter(ProfileFilter.KERJA) },
                    label = { Text("Kerja") }
                )
                FilterChip(
                    selected = state.filter == ProfileFilter.FAVORIT,
                    onClick = { viewModel.setFilter(ProfileFilter.FAVORIT) },
                    label = { Text("Favorit") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Menampilkan ${viewModel.filteredApps(state).size} dari ${state.apps.size} aplikasi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Memuat daftar aplikasi…")
                    }
                }
            } else if (state.loadError != null && state.apps.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.loadError,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Muat Ulang")
                    }
                }
            } else {
                val visible = viewModel.filteredApps(state)
                if (visible.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tidak ada aplikasi yang cocok dengan pencarian atau filter.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visible, key = { it.key }) { entry ->
                            AppRow(
                                entry = entry,
                                custom = state.customizations[entry.key],
                                systemIcon = state.icons[entry.key],
                                onLaunch = { viewModel.launch(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onEdit = { selected ->
                                    editingEntry = selected
                                    customNameText = state.customizations[selected.key]?.customName ?: ""
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    val entryToEdit = editingEntry
    if (entryToEdit != null) {
        val currentCustom = state.customizations[entryToEdit.key]
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text("Ubah tampilan di dashboard") },
            text = {
                Column {
                    Text(
                        text = "Aplikasi: ${entryToEdit.label} (${entryToEdit.profileLabel})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entryToEdit.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customNameText,
                        onValueChange = { customNameText = it },
                        label = { Text("Nama tampilan kustom") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kosongkan untuk kembali ke nama asli. Nama dan ikon kustom " +
                            "hanya berlaku di dashboard CloneX dan tidak mengubah " +
                            "launcher sistem atau APK aplikasi target.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (currentCustom?.customIconPath != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ikon kustom sudah tersimpan untuk entri ini.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveCustomName(entryToEdit, customNameText)
                        editingEntry = null
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pickIconLauncher.launch("image/*") }) {
                        Text("Pilih Ikon")
                    }
                    TextButton(
                        onClick = {
                            viewModel.clearCustomization(entryToEdit)
                            editingEntry = null
                        }
                    ) {
                        Text("Hapus Kustom")
                    }
                    TextButton(onClick = { editingEntry = null }) {
                        Text("Batal")
                    }
                }
            }
        )
    }
}
