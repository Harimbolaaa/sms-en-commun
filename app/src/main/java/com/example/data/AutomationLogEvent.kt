package com.example.data

/**
 * Modèle de données représentant un événement d'automatisation pour le moniteur en direct.
 */
data class AutomationLogEvent(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val type: LogType,
    val details: String? = null,
    val payloadJson: String? = null,
    val httpCode: Int? = null
)

enum class LogType {
    INFO,
    SMS_INTERCEPTED,
    HTTP_REQUEST,
    HTTP_SUCCESS,
    HTTP_ERROR,
    SERVICE_LIFECYCLE,
    PERMISSION_EVENT
}
