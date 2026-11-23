package com.clonex.app.ui.screens

import android.app.Application
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clonex.app.CloneXApplication
import com.clonex.app.data.db.ManagedAppEntity
import com.clonex.app.data.model.AppEntry
import com.clonex.app.data.repository.AppRepository
import com.clonex.app.data.repository.CustomizationRepository
import com.clonex.app.data.repository.IconStorage
import com.clonex.app.data.repository.LaunchResult
import com.clonex.app.domain.LaunchAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ProfileFilter {
    SEMUA, UTAMA, KERJA, FAVORIT
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val apps: List<AppEntry> = emptyList(),
    val customizations: Map<String, ManagedAppEntity> = emptyMap(),
    val query: String = "",
    val filter: ProfileFilter = ProfileFilter.SEMUA,
    val hasWorkProfile: Boolean = false,
    val loadError: String? = null,
    val message: String? = null,
    val icons: Map<String, Drawable?> = emptyMap()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application.applicationContext)
    private val iconStorage = IconStorage(application.applicationContext)
    private val customizationRepository = CustomizationRepository(
        (application as CloneXApplication).database.managedAppDao(),
        iconStorage
    )
    private val launchAppUseCase = LaunchAppUseCase(appRepository, customizationRepository)

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            customizationRepository.observeAll().collect { list ->
                _state.update { current ->
                    current.copy(customizations = list.associateBy { it.packageName + "|" + it.userSerial })
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            try {
                val apps = appRepository.loadApps()
                val profiles = appRepository.getProfiles()
                // Memuat ikon di IO agar tidak menghambat thread utama
                // saat perangkat memiliki banyak aplikasi.
                val icons = withContext(Dispatchers.IO) {
                    val map = mutableMapOf<String, Drawable?>()
                    for (app in apps) {
                        map[app.key] = try {
                            appRepository.getBadgedIcon(app)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    map
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        apps = apps,
                        icons = icons,
                        hasWorkProfile = profiles.any { profile -> !profile.isMain },
                        loadError = if (apps.isEmpty()) {
                            "Tidak ada aplikasi launcher yang ditemukan. " +
                                "Jika daftar kosong, periksa izin QUERY_ALL_PACKAGES " +
                                "dan coba tekan Muat Ulang."
                        } else {
                            null
                        }
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadError = "Gagal memuat daftar aplikasi: ${e.message ?: "kesalahan tidak diketahui"}"
                    )
                }
            }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun setFilter(filter: ProfileFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun launch(entry: AppEntry) {
        viewModelScope.launch {
            when (val result = launchAppUseCase.launch(entry)) {
                is LaunchResult.Success -> {
                    _state.update {
                        it.copy(message = "Membuka ${entry.label} di ${entry.profileLabel}.")
                    }
                }
                is LaunchResult.Failed -> {
                    _state.update { it.copy(message = result.reason) }
                }
            }
        }
    }

    fun toggleFavorite(entry: AppEntry) {
        viewModelScope.launch {
            val current = _state.value.customizations[entry.key]
            val newValue = current?.isFavorite != true
            customizationRepository.setFavorite(entry.packageName, entry.userSerial, newValue)
            _state.update {
                it.copy(
                    message = if (newValue) {
                        "${entry.label} ditandai sebagai favorit."
                    } else {
                        "${entry.label} dihapus dari favorit."
                    }
                )
            }
        }
    }

    fun saveCustomName(entry: AppEntry, customName: String?) {
        viewModelScope.launch {
            customizationRepository.setCustomName(entry.packageName, entry.userSerial, customName)
            _state.update {
                it.copy(message = "Nama tampilan untuk ${entry.label} disimpan. Nama ini hanya berlaku di dashboard CloneX.")
            }
        }
    }

    fun saveCustomIcon(entry: AppEntry, uri: Uri) {
        viewModelScope.launch {
            val path = iconStorage.saveIcon(uri, entry.packageName, entry.userSerial)
            if (path != null) {
                customizationRepository.setCustomIconPath(entry.packageName, entry.userSerial, path)
                _state.update {
                    it.copy(message = "Ikon kustom untuk ${entry.label} disimpan. Ikon ini hanya berlaku di dashboard CloneX.")
                }
            } else {
                _state.update {
                    it.copy(message = "Gagal menyimpan ikon kustom. Coba pilih gambar lain.")
                }
            }
        }
    }

    fun clearCustomization(entry: AppEntry) {
        viewModelScope.launch {
            customizationRepository.clearCustomization(entry.packageName, entry.userSerial)
            _state.update {
                it.copy(message = "Nama dan ikon kustom untuk ${entry.label} dihapus.")
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    /** Daftar aplikasi setelah pencarian dan filter diterapkan. */
    fun filteredApps(state: DashboardUiState): List<AppEntry> {
        val query = state.query.trim().lowercase()
        return state.apps.filter { entry ->
            val matchesQuery = query.isEmpty() ||
                entry.label.lowercase().contains(query) ||
                entry.packageName.lowercase().contains(query)
            val custom = state.customizations[entry.key]
            val matchesCustomName = custom?.customName?.lowercase()?.contains(query) == true
            val matchesFilter = when (state.filter) {
                ProfileFilter.SEMUA -> true
                ProfileFilter.UTAMA -> !entry.isWorkProfile
                ProfileFilter.KERJA -> entry.isWorkProfile
                ProfileFilter.FAVORIT -> custom?.isFavorite == true
            }
            (matchesQuery || matchesCustomName) && matchesFilter
        }
    }
}
