package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room représentant un SMS / Payload mis en file d'attente (Queue) suite à un échec HTTP
 * (ex: coupure réseau, timeout, erreur 500 Appwrite/Serveur).
 * Permet la rétention locale sans perte de données et le retry automatique ou manuel.
 */
@Entity(tableName = "sms_queue")
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val targetUrl: String,
    val payloadJson: String,
    val source: String,
    val contentLength: Int,
    val status: String = "FAILED", // "FAILED", "PENDING_RETRY", "SUCCESS"
    val attemptCount: Int = 1,
    val lastError: String? = null,
    val lastAttemptTimestamp: Long = System.currentTimeMillis(),
    val headerName: String? = null,
    val headerValue: String? = null,
    val headersJson: String? = null,
    val userPhone: String? = null,
    val userId: String? = null
)
