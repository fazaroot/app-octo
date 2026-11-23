package com.clonex.app.domain

import com.clonex.app.data.model.AppEntry
import com.clonex.app.data.repository.AppRepository
import com.clonex.app.data.repository.CustomizationRepository
import com.clonex.app.data.repository.LaunchResult

/**
 * Use case peluncuran aplikasi.
 *
 * Memisahkan aturan sederhana ini dari UI: luncurkan lewat
 * AppRepository, dan hanya catat waktu peluncuran jika berhasil
 * dan entri sudah punya metadata di Room. Tidak ada logika
 * tersembunyi di sini.
 */
class LaunchAppUseCase(
    private val appRepository: AppRepository,
    private val customizationRepository: CustomizationRepository
) {
    suspend fun launch(entry: AppEntry): LaunchResult {
        val result = appRepository.launch(entry)
        if (result is LaunchResult.Success) {
            customizationRepository.markLaunched(entry.packageName, entry.userSerial)
        }
        return result
    }
}
