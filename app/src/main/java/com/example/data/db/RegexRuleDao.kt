package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour les opérations CRUD et la sélection multiple des règles Regex.
 */
@Dao
interface RegexRuleDao {

    @Query("SELECT * FROM regex_rules ORDER BY orderIndex ASC, title ASC")
    fun getAllRules(): Flow<List<RegexRuleEntity>>

    @Query("SELECT * FROM regex_rules ORDER BY orderIndex ASC, title ASC")
    suspend fun getAllRulesSync(): List<RegexRuleEntity>

    @Query("SELECT * FROM regex_rules WHERE isChecked = 1 ORDER BY orderIndex ASC")
    fun getCheckedRules(): Flow<List<RegexRuleEntity>>

    @Query("SELECT * FROM regex_rules WHERE isChecked = 1 ORDER BY orderIndex ASC")
    suspend fun getCheckedRulesSync(): List<RegexRuleEntity>

    @Query("SELECT * FROM regex_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: String): RegexRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RegexRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RegexRuleEntity>)

    @Update
    suspend fun updateRule(rule: RegexRuleEntity)

    @Query("UPDATE regex_rules SET isChecked = :isChecked WHERE id = :id")
    suspend fun setRuleChecked(id: String, isChecked: Boolean)

    @Query("DELETE FROM regex_rules WHERE id = :id")
    suspend fun deleteRuleById(id: String)

    @Query("SELECT COUNT(*) FROM regex_rules")
    suspend fun getCount(): Int

    @Query("DELETE FROM regex_rules")
    suspend fun clearAll()
}
