package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.QueueItemDao
import com.example.data.db.QueueItemEntity
import com.example.network.NetworkDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repository gérant la file d'attente locale (Queue Room DB) en cas d'échec réseau ou serveur (Appwrite 500, pas d'internet, etc.).
 * Assure le mécanisme de Retry automatique dès reconnexion et manuel via l'interface.
 * Garantit que TOUS les callbacks UI (onDone, onFinished) sont postés sur le Main UI Thread (Looper.getMainLooper()).
 */
object SmsQueueRepository {

    private const val TAG = "SmsQueueRepository"
    private var dao: QueueItemDao? = null
    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private var connectivityManager: ConnectivityManager? = null
    private var isMonitoringStarted = false

    private fun postToMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        if (dao == null) {
            val db = AppDatabase.getDatabase(context)
            dao = db.queueItemDao()
        }
        startNetworkMonitoring(context)
    }

    /**
     * Active l'écoute active des changements de connectivité réseau pour déclencher
     * automatiquement le re-jeu de la file d'attente dès que la connexion internet est rétablie.
     */
    private fun startNetworkMonitoring(context: Context) {
        if (isMonitoringStarted) return
        isMonitoringStarted = true

        try {
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager?.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.i(TAG, "Connexion Internet RÉTABLIE. Déclenchement automatique du Retry de la file d'attente...")
                    _isNetworkAvailable.value = true
                    
                    AutomationLogRepository.log(
                        tag = "QUEUE_NETWORK",
                        message = "Connexion internet détectée -> Retry automatique de la file d'attente",
                        type = LogType.INFO
                    )

                    // Déclenche le re-jeu automatique des SMS en attente
                    retryAllPending(context)
                }

                override fun onLost(network: Network) {
                    Log.w(TAG, "Connexion Internet PERDUE.")
                    _isNetworkAvailable.value = false
                    
                    AutomationLogRepository.log(
                        tag = "QUEUE_NETWORK",
                        message = "Perte de connexion Internet détectée",
                        type = LogType.INFO
                    )
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Impossible d'initialiser le NetworkCallback", e)
        }
    }

    /**
     * Flux réactif des éléments de la file d'attente filtré par utilisateur :
     * - Pour l'administrateur : accès à la totalité de la file d'attente.
     * - Pour un utilisateur client : accès STRICTEMENT à ses propres SMS mis en file.
     */
    fun getQueueItemsForUser(userPhone: String, isAdmin: Boolean): Flow<List<QueueItemEntity>> {
        val d = dao ?: return MutableStateFlow(emptyList())
        return if (isAdmin || userPhone.isBlank()) {
            d.getAllQueueItems()
        } else {
            d.getQueueItemsByUserPhone(userPhone)
        }
    }

    /**
     * Nombre d'éléments en attente filtré par utilisateur :
     */
    fun getPendingCountForUser(userPhone: String, isAdmin: Boolean): Flow<Int> {
        val d = dao ?: return MutableStateFlow(0)
        return if (isAdmin || userPhone.isBlank()) {
            d.getPendingCount()
        } else {
            d.getPendingCountByUserPhone(userPhone)
        }
    }

    /**
     * Flux réactif de tous les éléments de la file d'attente
     */
    fun getAllQueueItems(): Flow<List<QueueItemEntity>> {
        return dao?.getAllQueueItems() ?: MutableStateFlow(emptyList())
    }

    /**
     * Flux réactif du nombre d'éléments en attente de réessai
     */
    fun getPendingCount(): Flow<Int> {
        return dao?.getPendingCount() ?: MutableStateFlow(0)
    }

    /**
     * Ajoute un message échoué dans la file d'attente locale permanente Room
     * avec association de l'identité du compte utilisateur connecté.
     */
    fun enqueueFailedTransmission(
        targetUrl: String,
        payloadJson: String,
        source: String,
        contentLength: Int,
        errorReason: String,
        headersMap: Map<String, String>? = null,
        headerName: String? = null,
        headerValue: String? = null,
        userPhone: String? = null,
        userId: String? = null
    ) {
        val effectiveUserPhone = userPhone ?: appContext?.let {
            val p = AutomationPreferences(it)
            if (p.isLoggedIn) p.loggedInUserPhone else null
        }
        val effectiveUserId = userId ?: appContext?.let {
            val p = AutomationPreferences(it)
            if (p.isLoggedIn) p.loggedInUserId else null
        }

        val headersJson = if (!headersMap.isNullOrEmpty()) {
            val obj = org.json.JSONObject()
            headersMap.forEach { (k, v) -> obj.put(k, v) }
            obj.toString()
        } else null

        val item = QueueItemEntity(
            timestamp = System.currentTimeMillis(),
            targetUrl = targetUrl,
            payloadJson = payloadJson,
            source = source,
            contentLength = contentLength,
            status = "FAILED",
            attemptCount = 1,
            lastError = errorReason,
            lastAttemptTimestamp = System.currentTimeMillis(),
            headerName = headerName,
            headerValue = headerValue,
            headersJson = headersJson,
            userPhone = effectiveUserPhone,
            userId = effectiveUserId
        )

        scope.launch {
            try {
                val id = dao?.insertItem(item) ?: -1
                Log.w(TAG, "SMS de [$source] mis en file d'attente Room (Queue ID: $id). Cause: $errorReason")
                
                AutomationLogRepository.log(
                    tag = "QUEUE",
                    message = "SMS mis en file d'attente (#$id) suite à échec",
                    type = LogType.INFO,
                    details = "Émetteur: $source\nCause de l'échec: $errorReason\nStocké dans SQLite pour Retry automatique.",
                    payloadJson = payloadJson,
                    source = source
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erreur mise en file d'attente", e)
            }
        }
    }

    /**
     * Réessaie d'envoyer un élément unitaire de la file d'attente
     */
    fun retryItem(
        context: Context,
        itemId: Long,
        onDone: ((Boolean) -> Unit)? = null
    ) {
        scope.launch {
            val item = dao?.getItemById(itemId)
            if (item == null) {
                postToMainThread {
                    onDone?.invoke(false)
                }
                return@launch
            }

            val prefs = AutomationPreferences(context)
            val effectiveUrl = sanitizeUrl(item.targetUrl.ifBlank { prefs.targetUrl }, AutomationPreferences.DEFAULT_TARGET_URL)
            val effectiveHeaders = mutableMapOf<String, String>()
            if (!item.headersJson.isNullOrBlank()) {
                try {
                    val obj = org.json.JSONObject(item.headersJson)
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        effectiveHeaders[key] = obj.optString(key)
                    }
                } catch (_: Exception) {}
            }
            if (effectiveHeaders.isEmpty()) {
                effectiveHeaders.putAll(prefs.getActiveHeadersMap())
            }
            sanitizeHeaders(effectiveHeaders)

            val cleanPayload = sanitizePayloadForAppwrite(item.payloadJson, item.userPhone ?: prefs.loggedInUserPhone, item.userId ?: prefs.loggedInUserId)
            val headerName = item.headerName ?: (if (prefs.isCustomHeaderEnabled) prefs.customHeaderName else null)
            val headerValue = item.headerValue ?: (if (prefs.isCustomHeaderEnabled) prefs.customHeaderValue else null)

            Log.i(TAG, "Retry manuel de la file d'attente pour l'élément #${item.id}")

            NetworkDispatcher.transmitDataWithCustomPayload(
                targetUrl = effectiveUrl,
                payloadJson = cleanPayload,
                source = item.source,
                contentLength = cleanPayload.length,
                headers = effectiveHeaders,
                headerName = headerName,
                headerValue = headerValue,
                isRetry = true,
                onComplete = { success, code, responseOrError ->
                    scope.launch {
                        if (success) {
                            Log.i(TAG, "Retry #$itemId réussi ! Suppression de la file d'attente.")
                            dao?.deleteById(item.id)
                            AutomationLogRepository.log(
                                tag = "QUEUE_RETRY",
                                message = "Retry Réussi (#$itemId) - Code $code",
                                type = LogType.HTTP_SUCCESS,
                                details = "SMS de ${item.source} transmis avec succès après mise en attente.",
                                payloadJson = cleanPayload,
                                httpCode = code,
                                source = item.source
                            )
                        } else {
                            val updated = item.copy(
                                attemptCount = item.attemptCount + 1,
                                lastAttemptTimestamp = System.currentTimeMillis(),
                                lastError = "Échec (Code $code): $responseOrError",
                                status = "FAILED",
                                payloadJson = cleanPayload
                            )
                            dao?.updateItem(updated)
                            AutomationLogRepository.log(
                                tag = "QUEUE_RETRY",
                                message = "Retry Échoué (#$itemId) - Tentative ${updated.attemptCount}",
                                type = LogType.HTTP_ERROR,
                                details = "Erreur: $responseOrError",
                                payloadJson = cleanPayload,
                                httpCode = code,
                                source = item.source
                            )
                        }
                        postToMainThread {
                            onDone?.invoke(success)
                        }
                    }
                }
            )
        }
    }

    /**
     * Réessaie tous les éléments en attente dans la file d'attente de manière séquentielle
     * - Pour un client : réessaie uniquement ses propres SMS.
     * - Pour l'administrateur : réessaie tous les SMS en attente.
     */
    fun retryAllPending(
        context: Context,
        userPhone: String? = null,
        isAdmin: Boolean = true,
        onFinished: ((Int, Int) -> Unit)? = null // (total, succeeded)
    ) {
        if (_isRetrying.value) {
            Log.w(TAG, "Retry déjà en cours...")
            return
        }

        scope.launch {
            val pendingList = if (isAdmin || userPhone.isNullOrBlank()) {
                dao?.getPendingQueueItemsSync() ?: emptyList()
            } else {
                dao?.getPendingQueueItemsSyncByUserPhone(userPhone) ?: emptyList()
            }

            if (pendingList.isEmpty()) {
                Log.d(TAG, "Aucun élément en attente dans la file d'attente.")
                postToMainThread {
                    onFinished?.invoke(0, 0)
                }
                return@launch
            }

            _isRetrying.value = true
            var successCount = 0
            var failCount = 0

            Log.i(TAG, "Démarrage du Retry pour ${pendingList.size} élément(s)...")

            for (item in pendingList) {
                val prefs = AutomationPreferences(context)
                val effectiveUrl = sanitizeUrl(item.targetUrl.ifBlank { prefs.targetUrl }, AutomationPreferences.DEFAULT_TARGET_URL)
                val effectiveHeaders = mutableMapOf<String, String>()
                if (!item.headersJson.isNullOrBlank()) {
                    try {
                        val obj = org.json.JSONObject(item.headersJson)
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            effectiveHeaders[key] = obj.optString(key)
                        }
                    } catch (_: Exception) {}
                }
                if (effectiveHeaders.isEmpty()) {
                    effectiveHeaders.putAll(prefs.getActiveHeadersMap())
                }
                sanitizeHeaders(effectiveHeaders)

                val cleanPayload = sanitizePayloadForAppwrite(item.payloadJson, item.userPhone ?: prefs.loggedInUserPhone, item.userId ?: prefs.loggedInUserId)
                val headerName = item.headerName ?: (if (prefs.isCustomHeaderEnabled) prefs.customHeaderName else null)
                val headerValue = item.headerValue ?: (if (prefs.isCustomHeaderEnabled) prefs.customHeaderValue else null)

                // Exécution synchrone dans la coroutine IO
                val result = NetworkDispatcher.transmitSynchronous(
                    targetUrl = effectiveUrl,
                    payloadJson = cleanPayload,
                    source = item.source,
                    contentLength = cleanPayload.length,
                    headers = effectiveHeaders,
                    headerName = headerName,
                    headerValue = headerValue
                )

                if (result.first) {
                    successCount++
                    dao?.deleteById(item.id)
                    AutomationLogRepository.log(
                        tag = "QUEUE_RETRY",
                        message = "Retry Réussi (#${item.id}) - SMS ${item.source}",
                        type = LogType.HTTP_SUCCESS,
                        details = "Transmis avec succès après réessai.",
                        payloadJson = cleanPayload,
                        httpCode = result.second,
                        source = item.source
                    )
                } else {
                    failCount++
                    val updated = item.copy(
                        attemptCount = item.attemptCount + 1,
                        lastAttemptTimestamp = System.currentTimeMillis(),
                        lastError = "Échec (Code ${result.second}): ${result.third}",
                        status = "FAILED",
                        payloadJson = cleanPayload
                    )
                    dao?.updateItem(updated)
                }
            }

            _isRetrying.value = false
            Log.i(TAG, "Retry terminé : $successCount réussis, $failCount échoués sur ${pendingList.size}")
            postToMainThread {
                onFinished?.invoke(pendingList.size, successCount)
            }
        }
    }

    /**
     * Nettoie et normalise la charge utile JSON pour se conformer rigoureusement au schéma Appwrite Cloud :
     * 1. Encapsule sous { "documentId": "unique()", "data": { ... } } si non présent.
     * 2. Convertit "parsedAmount" en entier Long (sans virgule/décimale).
     * 3. Assure la présence des attributs valides (referenceId, createdAtUnix, clientPhone, clientUserId).
     */
    fun sanitizePayloadForAppwrite(rawJson: String, defaultPhone: String = "", defaultUserId: String = ""): String {
        return try {
            val fixedFloats = rawJson.replace(Regex("\"parsedAmount\"\\s*:\\s*([0-9]+)\\.[0-9]+"), "\"parsedAmount\": $1")
            val root = org.json.JSONObject(fixedFloats)

            val finalObj = org.json.JSONObject()
            finalObj.put("documentId", "unique()")

            val dataObj = if (root.has("data") && !root.isNull("data")) {
                root.getJSONObject("data")
            } else {
                val clone = org.json.JSONObject(fixedFloats)
                clone.remove("documentId")
                clone
            }

            if (dataObj.has("parsedAmount")) {
                val doubleVal = dataObj.optDouble("parsedAmount", 0.0)
                dataObj.put("parsedAmount", doubleVal.toLong())
            }
            if (!dataObj.has("referenceId") || dataObj.isNull("referenceId") || dataObj.optString("referenceId").isBlank()) {
                dataObj.put("referenceId", "REF-${System.currentTimeMillis() % 100000}")
            }
            if (!dataObj.has("createdAtUnix") || dataObj.isNull("createdAtUnix")) {
                dataObj.put("createdAtUnix", System.currentTimeMillis())
            }
            if ((!dataObj.has("clientPhone") || dataObj.optString("clientPhone").isBlank()) && defaultPhone.isNotBlank()) {
                dataObj.put("clientPhone", defaultPhone)
            }
            if ((!dataObj.has("clientUserId") || dataObj.optString("clientUserId").isBlank()) && defaultUserId.isNotBlank()) {
                dataObj.put("clientUserId", defaultUserId)
            }

            finalObj.put("data", dataObj)
            finalObj.toString()
        } catch (_: Exception) {
            val safeSender = "SMS_RECEIVER"
            org.json.JSONObject().apply {
                put("documentId", "unique()")
                put("data", org.json.JSONObject().apply {
                    put("sender", safeSender)
                    put("rawMessage", rawJson)
                    put("referenceId", "REF-${System.currentTimeMillis() % 100000}")
                    put("createdAtUnix", System.currentTimeMillis())
                    if (defaultPhone.isNotBlank()) put("clientPhone", defaultPhone)
                    if (defaultUserId.isNotBlank()) put("clientUserId", defaultUserId)
                })
            }.toString()
        }
    }

    /**
     * Valide l'URL pour corriger d'anciens endpoints obsolètes.
     */
    fun sanitizeUrl(url: String, defaultUrl: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank() || trimmed.contains("6a21910a00047c04e5af") || trimmed.contains("makafilma")) {
            return defaultUrl
        }
        return trimmed
    }

    /**
     * S'assure que les en-têtes indispensables à Appwrite Cloud sont présents et à jour.
     */
    fun sanitizeHeaders(headers: MutableMap<String, String>) {
        if (!headers.containsKey("X-Appwrite-Project") || headers["X-Appwrite-Project"].isNullOrBlank() || headers["X-Appwrite-Project"] == "6a21910a00047c04e5af") {
            headers["X-Appwrite-Project"] = AutomationPreferences.DEFAULT_PROJECT_ID
        }
        if (!headers.containsKey("X-Appwrite-Key") || headers["X-Appwrite-Key"].isNullOrBlank()) {
            headers["X-Appwrite-Key"] = AutomationPreferences.DEFAULT_API_KEY
        }
        if (!headers.containsKey("Content-Type") || headers["Content-Type"].isNullOrBlank()) {
            headers["Content-Type"] = "application/json"
        }
    }

    /**
     * Supprime un élément de la file
     */
    fun deleteItem(id: Long) {
        scope.launch {
            dao?.deleteById(id)
        }
    }

    /**
     * Vide complètement la file d'attente
     */
    fun clearQueue() {
        scope.launch {
            dao?.clearAll()
        }
    }
}
