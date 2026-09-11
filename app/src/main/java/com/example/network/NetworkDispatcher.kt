package com.example.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.AutomationLogRepository
import com.example.data.LogType
import com.example.data.SmsQueueRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gestionnaire de transmission réseau utilisant OkHttp.
 * Gère l'envoi des requêtes HTTP POST avec :
 * - Support d'en-têtes HTTP multiples (ex: X-Appwrite-Project, Content-Type, Authorization, etc.)
 * - Suivi du statut ("SUCCESS" ou "FAILED")
 * - Mise en file d'attente automatique (Queue) dans SQLite Room en cas d'erreur de transmission
 */
object NetworkDispatcher {

    private const val TAG = "NetworkDispatcher"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun postToMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    /**
     * Construit la structure JSON conforme au format Appwrite : {"documentId": "unique()", "data": {...}}
     */
    fun buildJsonPayload(source: String, content: String): String {
        val result = com.example.data.ExtractionEngine.applyRegex(
            content,
            com.example.data.AutomationPreferences.DEFAULT_REGEX_PATTERN,
            true
        )
        return com.example.data.ExtractionEngine.buildCustomJsonPayload(source, content, result)
    }

    /**
     * Envoie la charge utile JSON vers l'URL spécifiée via HTTP POST de façon asynchrone.
     */
    fun transmitData(
        targetUrl: String,
        source: String,
        content: String,
        headers: Map<String, String>? = null,
        headerName: String? = null,
        headerValue: String? = null,
        onComplete: ((Boolean, Int, String) -> Unit)? = null
    ) {
        val payload = buildJsonPayload(source, content)
        transmitDataWithCustomPayload(
            targetUrl = targetUrl,
            payloadJson = payload,
            source = source,
            contentLength = content.length,
            headers = headers,
            headerName = headerName,
            headerValue = headerValue,
            onComplete = onComplete
        )
    }

    /**
     * Envoie une charge utile JSON avec en-têtes HTTP personnalisés et gestion automatique de la file d'attente.
     */
    fun transmitDataWithCustomPayload(
        targetUrl: String,
        payloadJson: String,
        source: String,
        contentLength: Int,
        headers: Map<String, String>? = null,
        headerName: String? = null,
        headerValue: String? = null,
        isRetry: Boolean = false,
        onComplete: ((Boolean, Int, String) -> Unit)? = null
    ) {
        val cleanUrl = targetUrl.trim()

        if (cleanUrl.isBlank() || (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true))) {
            val errorMsg = "URL invalide (doit commencer par http:// ou https://): '$cleanUrl'"
            Log.e(TAG, errorMsg)
            AutomationLogRepository.log(
                tag = "NETWORK",
                message = "ERREUR: URL invalide",
                type = LogType.HTTP_ERROR,
                details = errorMsg,
                payloadJson = payloadJson,
                source = source
            )
            if (!isRetry) {
                SmsQueueRepository.enqueueFailedTransmission(
                    targetUrl = targetUrl,
                    payloadJson = payloadJson,
                    source = source,
                    contentLength = contentLength,
                    errorReason = errorMsg,
                    headersMap = headers,
                    headerName = headerName,
                    headerValue = headerValue
                )
            }
            postToMainThread {
                onComplete?.invoke(false, -1, errorMsg)
            }
            return
        }

        // Préparation du map d'en-têtes HTTP
        val effectiveHeaders = mutableMapOf<String, String>()
        if (headers != null) {
            effectiveHeaders.putAll(headers.filter { it.key.isNotBlank() })
        }
        if (effectiveHeaders.isEmpty() && !headerName.isNullOrBlank() && !headerValue.isNullOrBlank()) {
            effectiveHeaders[headerName.trim()] = headerValue.trim()
        }

        val headerDetails = if (effectiveHeaders.isNotEmpty()) {
            "Headers (${effectiveHeaders.size}): " + effectiveHeaders.entries.joinToString(", ") { "${it.key}: ${it.value.take(4)}***" }
        } else {
            "Aucun header de sécurité"
        }

        AutomationLogRepository.log(
            tag = "NETWORK",
            message = "Envoi HTTP POST -> $cleanUrl",
            type = LogType.HTTP_REQUEST,
            payloadJson = payloadJson,
            details = "Source: $source | $headerDetails | Longueur: $contentLength car.",
            source = source
        )

        CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()
            try {
                val requestBody = payloadJson.toRequestBody(JSON_MEDIA_TYPE)
                val requestBuilder = Request.Builder()
                    .url(cleanUrl)
                    .post(requestBody)
                    .header("User-Agent", "AutoBridge-Android-Client/1.0")
                    .header("Accept", "application/json, text/plain, */*")

                // Ajout des en-têtes HTTP configurés
                effectiveHeaders.forEach { (name, value) ->
                    if (name.isNotBlank()) {
                        requestBuilder.header(name.trim(), value.trim())
                    }
                }

                val request = requestBuilder.build()

                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        val duration = System.currentTimeMillis() - startTime
                        val errorMessage = e.localizedMessage ?: "Erreur de connexion réseau / Serveur inaccessible"
                        Log.e(TAG, "Échec de transmission vers $cleanUrl: $errorMessage", e)

                        AutomationLogRepository.log(
                            tag = "NETWORK",
                            message = "ÉCHEC Transmission HTTP ($duration ms) [FAILED]",
                            type = LogType.HTTP_ERROR,
                            details = "Erreur: $errorMessage\nURL: $cleanUrl\n$headerDetails",
                            payloadJson = payloadJson,
                            source = source
                        )

                        // MISE EN ATTENTE (QUEUE) : On ne jette pas le SMS, on le garde dans SQLite Room !
                        if (!isRetry) {
                            SmsQueueRepository.enqueueFailedTransmission(
                                targetUrl = cleanUrl,
                                payloadJson = payloadJson,
                                source = source,
                                contentLength = contentLength,
                                errorReason = "Erreur Réseau ($errorMessage)",
                                headersMap = effectiveHeaders,
                                headerName = headerName,
                                headerValue = headerValue
                            )
                        }

                        postToMainThread {
                            onComplete?.invoke(false, -1, errorMessage)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val duration = System.currentTimeMillis() - startTime
                        val responseCode = response.code
                        val responseBody = try {
                            response.body?.string() ?: ""
                        } catch (e: Exception) {
                            ""
                        }
                        val isSuccess = response.isSuccessful

                        AutomationLogRepository.incrementTransmissions()

                        if (isSuccess) {
                            Log.d(TAG, "Transmission réussie (HTTP $responseCode) en $duration ms [SUCCESS]")
                            val responsePreview = if (responseBody.isNotBlank()) responseBody.take(4000) else "(Corps de réponse vide - HTTP 200 OK)"
                            AutomationLogRepository.log(
                                tag = "NETWORK",
                                message = "SUCCÈS HTTP $responseCode ($duration ms) [SUCCESS]",
                                type = LogType.HTTP_SUCCESS,
                                details = "Code: HTTP $responseCode ($duration ms)\n$headerDetails\n\n=== RÉPONSE DU SERVEUR ===\n$responsePreview",
                                payloadJson = payloadJson,
                                httpCode = responseCode,
                                source = source
                            )
                        } else {
                            Log.w(TAG, "Serveur a retourné un code d'erreur HTTP $responseCode [FAILED]")
                            val errorBodyPreview = if (responseBody.isNotBlank()) responseBody.take(4000) else "(Aucun détail d'erreur renvoyé par le serveur)"
                            AutomationLogRepository.log(
                                tag = "NETWORK",
                                message = "ERREUR HTTP $responseCode [FAILED]",
                                type = LogType.HTTP_ERROR,
                                details = "Code Erreur: HTTP $responseCode ($duration ms)\n$headerDetails\n\n=== RÉPONSE D'ERREUR DU SERVEUR ===\n$errorBodyPreview",
                                payloadJson = payloadJson,
                                httpCode = responseCode,
                                source = source
                            )

                            // MISE EN ATTENTE (QUEUE) : Erreur 500 Appwrite ou serveur -> sauvegardé pour réessai !
                            if (!isRetry) {
                                SmsQueueRepository.enqueueFailedTransmission(
                                    targetUrl = cleanUrl,
                                    payloadJson = payloadJson,
                                    source = source,
                                    contentLength = contentLength,
                                    errorReason = "HTTP $responseCode: ${responseBody.take(150).ifBlank { "Erreur serveur" }}",
                                    headersMap = effectiveHeaders,
                                    headerName = headerName,
                                    headerValue = headerValue
                                )
                            }
                        }

                        postToMainThread {
                            onComplete?.invoke(isSuccess, responseCode, responseBody)
                        }
                    }
                })
            } catch (e: Throwable) {
                val duration = System.currentTimeMillis() - startTime
                val errorDesc = e.localizedMessage ?: e.javaClass.simpleName
                Log.e(TAG, "Exception lors de la préparation de la requête HTTP: $errorDesc", e)
                
                AutomationLogRepository.log(
                    tag = "NETWORK",
                    message = "EXCEPTION Requête: $errorDesc [FAILED]",
                    type = LogType.HTTP_ERROR,
                    details = "URL: $cleanUrl\nException: ${e.stackTraceToString().take(300)}",
                    payloadJson = payloadJson,
                    source = source
                )

                if (!isRetry) {
                    SmsQueueRepository.enqueueFailedTransmission(
                        targetUrl = cleanUrl,
                        payloadJson = payloadJson,
                        source = source,
                        contentLength = contentLength,
                        errorReason = "Exception: $errorDesc",
                        headersMap = effectiveHeaders,
                        headerName = headerName,
                        headerValue = headerValue
                    )
                }

                postToMainThread {
                    onComplete?.invoke(false, -1, errorDesc)
                }
            }
        }
    }

    /**
     * Envoi HTTP synchrone (bloquant dans la coroutine appelante) utilisé pour le traitement séquentiel de la file de Retry.
     */
    fun transmitSynchronous(
        targetUrl: String,
        payloadJson: String,
        source: String,
        contentLength: Int,
        headers: Map<String, String>? = null,
        headerName: String? = null,
        headerValue: String? = null
    ): Triple<Boolean, Int, String> {
        val cleanUrl = targetUrl.trim()
        if (cleanUrl.isBlank() || (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true))) {
            return Triple(false, -1, "URL invalide")
        }

        return try {
            val requestBody = payloadJson.toRequestBody(JSON_MEDIA_TYPE)
            val requestBuilder = Request.Builder()
                .url(cleanUrl)
                .post(requestBody)
                .header("User-Agent", "AutoBridge-Android-Client/1.0")
                .header("Accept", "application/json, text/plain, */*")

            val effectiveHeaders = mutableMapOf<String, String>()
            if (headers != null) {
                effectiveHeaders.putAll(headers.filter { it.key.isNotBlank() && it.value.isNotBlank() })
            }
            if (effectiveHeaders.isEmpty() && !headerName.isNullOrBlank() && !headerValue.isNullOrBlank()) {
                effectiveHeaders[headerName.trim()] = headerValue.trim()
            }

            effectiveHeaders.forEach { (name, value) ->
                if (name.isNotBlank()) {
                    requestBuilder.header(name.trim(), value.trim())
                }
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val code = response.code
            val isSuccess = response.isSuccessful
            val body = response.body?.string() ?: ""
            Triple(isSuccess, code, body)
        } catch (e: Exception) {
            Triple(false, -1, e.localizedMessage ?: "Erreur réseau")
        }
    }
}
