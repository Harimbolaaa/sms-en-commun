package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceSyncConfigDao {

    @Query("SELECT * FROM device_sync_config WHERE userPhone = :phone LIMIT 1")
    fun observeConfig(phone: String): Flow<DeviceSyncConfigEntity?>

    @Query("SELECT * FROM device_sync_config WHERE userPhone = :phone LIMIT 1")
    suspend fun getConfigSync(phone: String): DeviceSyncConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: DeviceSyncConfigEntity)

    @Query("DELETE FROM device_sync_config WHERE userPhone = :phone")
    suspend fun deleteConfig(phone: String)
}
