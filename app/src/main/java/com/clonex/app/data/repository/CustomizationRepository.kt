package com.clonex.app.data.repository

import com.clonex.app.data.db.ManagedAppDao
import com.clonex.app.data.db.ManagedAppEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository untuk metadata kustom di dashboard.
 *
 * Semua data disimpan lokal di Room. Tidak ada data yang dikirim
 * ke server mana pun.
 */
class CustomizationRepository(
    private val dao: ManagedAppDao,
    private val iconStorage: IconStorage
) {
    fun observeAll(): Flow<List<ManagedAppEntity>> = dao.observeAll()

    suspend fun get(packageName: String, userSerial: Long): ManagedAppEntity? {
        return dao.getByKey(packageName, userSerial)
    }

    suspend fun setCustomName(packageName: String, userSerial: Long, customName: String?) {
        val current = dao.getByKey(packageName, userSerial)
        val cleaned = customName?.trim()?.takeIf { it.isNotEmpty() }
        if (current == null && cleaned == null) return
        dao.upsert(
            (current ?: ManagedAppEntity(packageName, userSerial)).copy(customName = cleaned)
        )
    }

    suspend fun setCustomIconPath(packageName: String, userSerial: Long, path: String?) {
        val current = dao.getByKey(packageName, userSerial)
        val oldPath = current?.customIconPath
        if (current == null && path == null) return
        dao.upsert(
            (current ?: ManagedAppEntity(packageName, userSerial)).copy(customIconPath = path)
        )
        if (oldPath != null && oldPath != path) {
            iconStorage.deleteIcon(oldPath)
        }
    }

    suspend fun setFavorite(packageName: String, userSerial: Long, favorite: Boolean) {
        val current = dao.getByKey(packageName, userSerial)
        dao.upsert(
            (current ?: ManagedAppEntity(packageName, userSerial)).copy(isFavorite = favorite)
        )
    }

    suspend fun markLaunched(packageName: String, userSerial: Long) {
        val current = dao.getByKey(packageName, userSerial) ?: return
        dao.upsert(current.copy(lastLaunched = System.currentTimeMillis()))
    }

    suspend fun clearCustomization(packageName: String, userSerial: Long) {
        val current = dao.getByKey(packageName, userSerial) ?: return
        current.customIconPath?.let { iconStorage.deleteIcon(it) }
        dao.deleteByKey(packageName, userSerial)
    }
}
