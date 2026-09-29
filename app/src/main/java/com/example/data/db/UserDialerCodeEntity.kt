package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room stockant la configuration locale du composeur téléphonique secret par utilisateur.
 * Permet à chaque utilisateur (client ou admin) d'avoir ses propres 4 chiffres secrets (*#*#XXXX#*#*).
 */
@Entity(tableName = "user_dialer_codes")
data class UserDialerCodeEntity(
    @PrimaryKey
    val userId: String,
    val userPhone: String,
    val secretCode: String, // 4 chiffres personnalisés, ou vide si par défaut
    val isCustomized: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
