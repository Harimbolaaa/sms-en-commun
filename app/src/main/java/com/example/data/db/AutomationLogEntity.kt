package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room pour la persistance locale de l'historique de tous les événements, statuts et flux de données.
 */
@Entity(tableName = "automation_logs")
data class AutomationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val type: String,
    val details: String? = null,
    val payloadJson: String? = null,
    val httpCode: Int? = null,
    val source: String? = null,
    val status: String = "INFO",
    val userPhone: String? = null,
    val userId: String? = null
)
