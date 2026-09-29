package com.example.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Repository gérant la centralisation de la configuration globale (Webhook URL, Headers de sécurité,
 * filtres d'expéditeurs, règles) sur Appwrite Cloud (collection 'app_config', document 'global_settings').
 *
 * Fonctionnalité clé :
 * - L'ADMIN configure et propage les réglages centraux dans Appwrite Cloud.
 * - TOUS LES CLIENTS synchronisent automatiquement cette configuration au démarrage et à chaque reconnexion.
 */
object AppConfigRepository {

    private const val TAG = "AppConfigRepo"
    private const val CONFIG_COLLECTION_ID = "app_config"
    private const val GLOBAL_CONFIG_DOC_ID = "global_settings"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun postToMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    /**
     * Retourne les en-têtes HTTP de sécurité standards par défaut pour Appwrite Cloud.
     */
    fun getDefaultAppwriteHeaders(): List<HttpHeaderItem> {
        return listOf(
            HttpHeaderItem(
                id = "default_appwrite_project",
                name = "X-Appwrite-Project",
                value = AutomationPreferences.DEFAULT_PROJECT_ID,
                isEnabled = true,
                description = "Identifiant de projet Appwrite"
            ),
            HttpHeaderItem(
                id = "default_appwrite_key",
                name = "X-Appwrite-Key",
                value = AutomationPreferences.DEFAULT_API_KEY,
                isEnabled = true,
                description = "Clé secrète API Appwrite Cloud"
            ),
            HttpHeaderItem(
                id = "default_content_type",
                name = "Content-Type",
                value = "application/json",
                isEnabled = true,
                description = "Format JSON Appwrite"
            )
        )
    }

    /**
     * Télécharge la configuration globale depuis Appwrite Cloud et l'applique localement dans AutomationPreferences.
     * Cette méthode est appelée pour tous les téléphones clients pour s'aligner sur la configuration de l'Admin.
     */
    fun fetchGlobalConfigFromAppwrite(
        context: Context,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val prefs = AutomationPreferences(context)
        val endpoint = AutomationPreferences.DEFAULT_ENDPOINT
        val projectId = AutomationPreferences.DEFAULT_PROJECT_ID
        val databaseId = AutomationPreferences.DEFAULT_DATABASE_ID
        val apiKey = AutomationPreferences.DEFAULT_API_KEY

        val docUrl = "$endpoint/databases/$databaseId/collections/$CONFIG_COLLECTION_ID/documents/$GLOBAL_CONFIG_DOC_ID"

        val request = Request.Builder()
            .url(docUrl)
            .header("X-Appwrite-Project", projectId)
            .header("X-Appwrite-Key", apiKey)
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Impossible de récupérer la config globale Appwrite Cloud (hors-ligne ou réseau lent): ${e.message}")
                postToMain {
                    onComplete?.invoke(false, "Hors-ligne ou réseau indisponible : réglages locaux conservés.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (resp.isSuccessful) {
                        try {
                            val bodyStr = resp.body?.string().orEmpty()
                            val docObj = JSONObject(bodyStr)
                            val configValueRaw = docObj.optString("configValue", "")
                            if (configValueRaw.isNotBlank()) {
                                val configJson = JSONObject(configValueRaw)

                                val targetUrl = configJson.optString("targetUrl", "")
                                if (targetUrl.isNotBlank()) {
                                    prefs.targetUrl = targetUrl
                                }

                                val customHeadersJson = configJson.optString("customHeadersJson", "")
                                if (customHeadersJson.isNotBlank()) {
                                    val headersArray = JSONArray(customHeadersJson)
                                    val parsedHeaders = mutableListOf<HttpHeaderItem>()
                                    for (i in 0 until headersArray.length()) {
                                        parsedHeaders.add(HttpHeaderItem.fromJson(headersArray.getJSONObject(i)))
                                    }
                                    if (parsedHeaders.isNotEmpty()) {
                                        prefs.saveCustomHeaders(parsedHeaders)
                                    }
                                }

                                val allowedSenders = configJson.optString("allowedSenders", "")
                                if (allowedSenders.isNotBlank()) {
                                    prefs.allowedSenders = allowedSenders
                                }

                                if (configJson.has("isSenderFilterEnabled")) {
                                    prefs.isSenderFilterEnabled = configJson.getBoolean("isSenderFilterEnabled")
                                }
                                if (configJson.has("isRegexEnabled")) {
                                    prefs.isRegexEnabled = configJson.getBoolean("isRegexEnabled")
                                }
                                if (configJson.has("sendOnlyExtracted")) {
                                    prefs.sendOnlyExtracted = configJson.getBoolean("sendOnlyExtracted")
                                }
                                if (configJson.has("isAutoForwardEnabled")) {
                                    prefs.isAutoForwardEnabled = configJson.getBoolean("isAutoForwardEnabled")
                                }

                                Log.i(TAG, "Configuration globale synchronisée avec succès depuis Appwrite Cloud.")
                                postToMain {
                                    onComplete?.invoke(true, "Configuration globale synchronisée depuis Appwrite Cloud !")
                                }
                                return
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Erreur parsing de la config globale Appwrite", e)
                        }
                    }
                    postToMain {
                        onComplete?.invoke(false, "Code HTTP ${resp.code} : réglages locaux utilisés.")
                    }
                }
            }
        })
    }

    /**
     * Publie la configuration de l'Admin vers Appwrite Cloud pour qu'elle devienne
     * la configuration par défaut de tous les comptes clients.
     */
    fun pushGlobalConfigToAppwrite(
        context: Context,
        targetUrl: String,
        headers: List<HttpHeaderItem>,
        allowedSenders: String,
        isSenderFilterEnabled: Boolean,
        isRegexEnabled: Boolean,
        sendOnlyExtracted: Boolean,
        adminIdentifier: String = "admin",
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val prefs = AutomationPreferences(context)
        prefs.targetUrl = targetUrl
        prefs.saveCustomHeaders(headers)
        prefs.allowedSenders = allowedSenders
        prefs.isSenderFilterEnabled = isSenderFilterEnabled
        prefs.isRegexEnabled = isRegexEnabled
        prefs.sendOnlyExtracted = sendOnlyExtracted

        val endpoint = AutomationPreferences.DEFAULT_ENDPOINT
        val projectId = AutomationPreferences.DEFAULT_PROJECT_ID
        val databaseId = AutomationPreferences.DEFAULT_DATABASE_ID
        val apiKey = AutomationPreferences.DEFAULT_API_KEY

        val headersJsonArray = JSONArray()
        headers.forEach { headersJsonArray.put(it.toJson()) }

        val configValueObj = JSONObject().apply {
            put("targetUrl", targetUrl.ifBlank { AutomationPreferences.DEFAULT_TARGET_URL })
            put("customHeadersJson", headersJsonArray.toString())
            put("allowedSenders", allowedSenders)
            put("isSenderFilterEnabled", isSenderFilterEnabled)
            put("isRegexEnabled", isRegexEnabled)
            put("sendOnlyExtracted", sendOnlyExtracted)
            put("isAutoForwardEnabled", prefs.isAutoForwardEnabled)
        }

        val payload = JSONObject().apply {
            put("data", JSONObject().apply {
                put("configKey", GLOBAL_CONFIG_DOC_ID)
                put("configValue", configValueObj.toString())
                put("updatedAt", System.currentTimeMillis())
                put("updatedBy", adminIdentifier)
            })
        }

        val docUrl = "$endpoint/databases/$databaseId/collections/$CONFIG_COLLECTION_ID/documents/$GLOBAL_CONFIG_DOC_ID"

        val request = Request.Builder()
            .url(docUrl)
            .header("X-Appwrite-Project", projectId)
            .header("X-Appwrite-Key", apiKey)
            .header("Content-Type", "application/json")
            .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Échec de propagation de la config globale vers Appwrite: ${e.message}")
                postToMain {
                    onComplete?.invoke(false, "Erreur réseau: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val isSuccess = resp.isSuccessful
                    val code = resp.code
                    if (isSuccess) {
                        Log.i(TAG, "Configuration globale publiée avec succès sur Appwrite Cloud.")
                        postToMain {
                            onComplete?.invoke(true, "Configuration propagée avec succès sur Appwrite Cloud ! Tous les clients recevront ces réglages.")
                        }
                    } else if (code == 404) {
                        // Document n'existe pas encore, on le crée avec POST
                        createGlobalConfigDoc(endpoint, projectId, databaseId, apiKey, configValueObj.toString(), adminIdentifier, onComplete)
                    } else {
                        val body = resp.body?.string().orEmpty()
                        Log.e(TAG, "Erreur HTTP $code lors de la publication Appwrite: $body")
                        postToMain {
                            onComplete?.invoke(false, "Erreur HTTP $code: $body")
                        }
                    }
                }
            }
        })
    }

    private fun createGlobalConfigDoc(
        endpoint: String,
        projectId: String,
        databaseId: String,
        apiKey: String,
        configValueStr: String,
        adminIdentifier: String,
        onComplete: ((Boolean, String) -> Unit)?
    ) {
        val createUrl = "$endpoint/databases/$databaseId/collections/$CONFIG_COLLECTION_ID/documents"
        val createPayload = JSONObject().apply {
            put("documentId", GLOBAL_CONFIG_DOC_ID)
            put("data", JSONObject().apply {
                put("configKey", GLOBAL_CONFIG_DOC_ID)
                put("configValue", configValueStr)
                put("updatedAt", System.currentTimeMillis())
                put("updatedBy", adminIdentifier)
            })
        }

        val req = Request.Builder()
            .url(createUrl)
            .header("X-Appwrite-Project", projectId)
            .header("X-Appwrite-Key", apiKey)
            .header("Content-Type", "application/json")
            .post(createPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postToMain { onComplete?.invoke(false, "Erreur réseau création: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { r ->
                    val ok = r.isSuccessful
                    postToMain {
                        if (ok) {
                            onComplete?.invoke(true, "Configuration globale initialisée sur Appwrite Cloud !")
                        } else {
                            onComplete?.invoke(false, "Erreur création HTTP ${r.code}")
                        }
                    }
                }
            }
        })
    }

    /**
     * Effectue un test de connexion direct vers Appwrite Cloud avec rapport détaillé.
     */
    fun testAppwriteCloudConnection(
        context: Context,
        targetUrl: String,
        headers: Map<String, String>,
        adminPhone: String = "admin",
        onResult: (Boolean, Int, String) -> Unit
    ) {
        val testPayload = JSONObject().apply {
            put("documentId", "unique()")
            put("data", JSONObject().apply {
                put("sender", "TEST_ADMIN")
                put("rawMessage", "Test de validation du Webhook & En-têtes de sécurité depuis le Dashboard Admin.")
                put("parsedAmount", 0L)
                put("referenceId", "PING-TEST-${System.currentTimeMillis() % 10000}")
                put("operator", "ADMIN_PING")
                put("createdAtUnix", System.currentTimeMillis())
                put("clientPhone", adminPhone)
                put("clientUserId", "admin_dashboard")
            })
        }

        val requestBody = testPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val cleanUrl = targetUrl.ifBlank { AutomationPreferences.DEFAULT_TARGET_URL }

        val requestBuilder = Request.Builder()
            .url(cleanUrl)
            .post(requestBody)
            .header("User-Agent", "AutoBridge-Admin-Dashboard/1.0")

        headers.forEach { (k, v) ->
            if (k.isNotBlank()) {
                requestBuilder.header(k.trim(), v.trim())
            }
        }

        httpClient.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postToMain {
                    onResult(false, -1, "Échec de connexion réseau: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val code = resp.code
                    val isSuccess = resp.isSuccessful
                    val body = resp.body?.string().orEmpty()
                    postToMain {
                        onResult(isSuccess, code, if (isSuccess) "Connexion réussie ! Document créé dans la collection Appwrite 'sms_logs' (HTTP $code)." else "Erreur Appwrite (HTTP $code) : $body")
                    }
                }
            }
        })
    }
}
