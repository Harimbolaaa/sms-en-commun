package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) pour les requêtes locales de l'historique et des statuts.
 */
@Dao
interface AutomationLogDao {

    @Query("SELECT * FROM automation_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AutomationLogEntity>>

    @Query("""
        SELECT * FROM automation_logs 
        WHERE message LIKE '%' || :query || '%' 
           OR tag LIKE '%' || :query || '%' 
           OR (details IS NOT NULL AND details LIKE '%' || :query || '%')
           OR (payloadJson IS NOT NULL AND payloadJson LIKE '%' || :query || '%')
           OR (source IS NOT NULL AND source LIKE '%' || :query || '%')
           OR type LIKE '%' || :query || '%'
           OR status LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchLogs(query: String): Flow<List<AutomationLogEntity>>

    @Query("SELECT * FROM automation_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: String): Flow<List<AutomationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AutomationLogEntity): Long

    @Query("DELETE FROM automation_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM automation_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM automation_logs")
    fun getLogsCount(): Flow<Int>
}
