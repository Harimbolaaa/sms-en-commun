package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour gérer la file d'attente des messages en échec et le mécanisme de réessai.
 */
@Dao
interface QueueItemDao {

    @Query("SELECT * FROM sms_queue ORDER BY timestamp DESC")
    fun getAllQueueItems(): Flow<List<QueueItemEntity>>

    @Query("SELECT * FROM sms_queue WHERE userPhone = :phone OR (:phone != '' AND payloadJson LIKE '%' || :phone || '%') ORDER BY timestamp DESC")
    fun getQueueItemsByUserPhone(phone: String): Flow<List<QueueItemEntity>>

    @Query("SELECT * FROM sms_queue WHERE status != 'SUCCESS' ORDER BY timestamp ASC")
    suspend fun getPendingQueueItemsSync(): List<QueueItemEntity>

    @Query("SELECT * FROM sms_queue WHERE status != 'SUCCESS' AND (userPhone = :phone OR (:phone != '' AND payloadJson LIKE '%' || :phone || '%')) ORDER BY timestamp ASC")
    suspend fun getPendingQueueItemsSyncByUserPhone(phone: String): List<QueueItemEntity>

    @Query("SELECT COUNT(*) FROM sms_queue WHERE status != 'SUCCESS'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_queue WHERE status != 'SUCCESS' AND (userPhone = :phone OR (:phone != '' AND payloadJson LIKE '%' || :phone || '%'))")
    fun getPendingCountByUserPhone(phone: String): Flow<Int>

    @Query("SELECT * FROM sms_queue WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): QueueItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: QueueItemEntity): Long

    @Update
    suspend fun updateItem(item: QueueItemEntity)

    @Query("DELETE FROM sms_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sms_queue WHERE status = 'SUCCESS'")
    suspend fun deleteSuccessful()

    @Query("DELETE FROM sms_queue")
    suspend fun clearAll()
}
