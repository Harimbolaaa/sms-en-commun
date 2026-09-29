package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour accéder aux codes secrets de composeur téléphonique par utilisateur.
 */
@Dao
interface UserDialerCodeDao {

    @Query("SELECT * FROM user_dialer_codes WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): UserDialerCodeEntity?

    @Query("SELECT * FROM user_dialer_codes WHERE userPhone = :phone LIMIT 1")
    suspend fun getByUserPhone(phone: String): UserDialerCodeEntity?

    @Query("SELECT * FROM user_dialer_codes WHERE userId = :userId LIMIT 1")
    fun observeByUserId(userId: String): Flow<UserDialerCodeEntity?>

    @Query("SELECT * FROM user_dialer_codes WHERE userPhone = :phone LIMIT 1")
    fun observeByUserPhone(phone: String): Flow<UserDialerCodeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: UserDialerCodeEntity)

    @Query("DELETE FROM user_dialer_codes WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}
