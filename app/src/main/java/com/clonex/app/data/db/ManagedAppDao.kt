package com.clonex.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedAppDao {

    @Query("SELECT * FROM managed_apps")
    fun observeAll(): Flow<List<ManagedAppEntity>>

    @Query("SELECT * FROM managed_apps WHERE packageName = :packageName AND userSerial = :userSerial LIMIT 1")
    suspend fun getByKey(packageName: String, userSerial: Long): ManagedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ManagedAppEntity)

    @Query("DELETE FROM managed_apps WHERE packageName = :packageName AND userSerial = :userSerial")
    suspend fun deleteByKey(packageName: String, userSerial: Long)

    @Query("DELETE FROM managed_apps")
    suspend fun deleteAll()
}
