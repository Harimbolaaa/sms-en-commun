package com.example.data

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.AutomationLogDao
import com.example.data.db.AutomationLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestionnaire centralisé et réactif de l'historique et des statuts avec persistance locale Room.
 */
object AutomationLogRepository {

    private var logDao: AutomationLogDao? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    private val _lastDispatchedPayload = MutableStateFlow<String?>(null)
    val lastDispatchedPayload: StateFlow<String?> = _lastDispatchedPayload.asStateFlow()

    private val _totalTransmissions = MutableStateFlow(0)
    val totalTransmissions: StateFlow<Int> = _totalTransmissions.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun init(context: Context) {
        if (logDao == null) {
            val db = AppDatabase.getDatabase(context)
            logDao = db.logDao()
        }
    }

    /**
     * Retourne le flux réactif de tous les logs ordonnés par date décroissante.
     */
    fun getAllLogs(): Flow<List<AutomationLogEntity>> {
        return logDao?.getAllLogs() ?: MutableStateFlow(emptyList())
    }

    /**
     * Recherche dans l'historique par texte / statut / code / source.
     */
    fun searchLogs(query: String): Flow<List<AutomationLogEntity>> {
        val dao = logDao
        return if (dao != null) {
            if (query.isBlank()) dao.getAllLogs() else dao.searchLogs(query.trim())
        } else {
            MutableStateFlow(emptyList())
        }
    }

    /**
     * Enregistre un événement dans la base de données locale Room de façon asynchrone.
     */
    fun log(
        tag: String,
        message: String,
        type: LogType = LogType.INFO,
        details: String? = null,
        payloadJson: String? = null,
        httpCode: Int? = null,
        source: String? = null
    ) {
        val statusStr = when (type) {
            LogType.HTTP_SUCCESS -> "SUCCESS (200)"
            LogType.HTTP_ERROR -> if (httpCode != null) "ERROR ($httpCode)" else "ERROR"
            LogType.HTTP_REQUEST -> "PENDING"
            LogType.SMS_INTERCEPTED -> "RECEIVED"
            LogType.SERVICE_LIFECYCLE -> "LIFECYCLE"
            LogType.PERMISSION_EVENT -> "PERMISSION"
            LogType.INFO -> "INFO"
        }

        val extractedSource = source ?: when {
            tag == "SMS_RECEIVER" || tag == "SMS_SIMULATOR" -> details?.lines()?.firstOrNull { it.startsWith("Émetteur:") }?.removePrefix("Émetteur:")?.trim()
            else -> null
        }

        val entity = AutomationLogEntity(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            type = type.name,
            details = details,
            payloadJson = payloadJson,
            httpCode = httpCode,
            source = extractedSource,
            status = statusStr
        )

        if (payloadJson != null) {
            _lastDispatchedPayload.value = payloadJson
        }

        scope.launch {
            try {
                logDao?.insertLog(entity)
            } catch (e: Exception) {
                android.util.Log.e("LogRepository", "Erreur lors de l'insertion dans Room DB", e)
            }
        }
    }

    fun incrementTransmissions() {
        _totalTransmissions.value += 1
    }

    fun setServiceStatus(running: Boolean) {
        _serviceRunning.value = running
        log(
            tag = "SERVICE",
            message = if (running) "Foreground Service DÉMARRÉ (Actif)" else "Foreground Service ARRÊTÉ",
            type = LogType.SERVICE_LIFECYCLE
        )
    }

    fun deleteLog(id: Long) {
        scope.launch {
            try {
                logDao?.deleteLogById(id)
            } catch (e: Exception) {
                android.util.Log.e("LogRepository", "Erreur suppression log ID $id", e)
            }
        }
    }

    fun clearLogs() {
        scope.launch {
            try {
                logDao?.clearAllLogs()
            } catch (e: Exception) {
                android.util.Log.e("LogRepository", "Erreur nettoyage logs", e)
            }
        }
    }

    fun formatFullDateTime(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatTimeOnly(timestamp: Long): String {
        return timeOnlyFormat.format(Date(timestamp))
    }
}
