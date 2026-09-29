package com.example.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.UserDialerCodeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
 * Repository gérant la persistance et la synchronisation du code composeur téléphonique (*#*#XXXX#*#*) :
 * 1. Synchronisation locale immédiate dans SharedPreferences (AutomationPreferences) pour accès instantané par le BroadcastReceiver.
 * 2. Persistance relationnelle locale par utilisateur dans la base de données Room (table 'user_dialer_codes').
 * 3. Synchronisation dans le Cloud Appwrite (User Preferences API) pour chaque utilisateur.
 */
object UserDialerRepository {

    private const val TAG = "UserDialerRepo"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private fun postToMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    /**
     * Observe en temps réel le code de composeur dans Room pour un utilisateur.
     */
    fun observeUserDialerCode(context: Context, phone: String): Flow<UserDialerCodeEntity?> {
        val dao = AppDatabase.getDatabase(context).userDialerCodeDao()
        return dao.observeByUserPhone(phone)
    }

    /**
     * Enregistre un nouveau code secret personnalisé (4 chiffres) :
     * - Écrase immédiatement les codes par défaut (4444 / 1234).
     * - Met à jour SharedPreferences, Room et le Cloud Appwrite.
     */
    fun saveCustomCode(
        context: Context,
        userId: String,
        userPhone: String,
        newCode: String,
        onComplete: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        val cleanCode = newCode.filter { it.isDigit() }.take(4)
        if (cleanCode.length != 4) {
            onComplete(false, "Le code secret doit comporter exactement 4 chiffres (ex: 4444, 1234, 5678)")
            return
        }

        val prefs = AutomationPreferences(context)
        // 1. Sauvegarde instantanée SharedPreferences
        prefs.setSecretDialerCode(cleanCode, userPhone)

        // 2. Sauvegarde Room locale par utilisateur
        scope.launch {
            try {
                val dao = AppDatabase.getDatabase(context).userDialerCodeDao()
                val entity = UserDialerCodeEntity(
                    userId = userId.ifBlank { "user_$userPhone" },
                    userPhone = userPhone,
                    secretCode = cleanCode,
                    isCustomized = true,
                    updatedAt = System.currentTimeMillis()
                )
                dao.insertOrUpdate(entity)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur écriture Room: ${e.message}")
            }
        }

        // Journalisation
        AutomationLogRepository.log(
            tag = "DIALER_CODE",
            message = "Nouveau code composeur configuré : *#*#$cleanCode#*#* pour $userPhone",
            type = LogType.INFO,
            source = userPhone,
            details = "Ce code remplace les codes par défaut (4444 et 1234)"
        )

        // 3. Synchronisation Cloud Appwrite (User Preferences API)
        syncToAppwriteCloud(context, userId, userPhone, cleanCode, isCustomized = true) { cloudSuccess, error ->
            postToMain {
                if (cloudSuccess) {
                    onComplete(true, null)
                } else {
                    // Sauvegardé en local, notification avertissement Cloud
                    onComplete(true, if (error != null) "Sauvegardé localement. (Cloud: $error)" else null)
                }
            }
        }
    }

    /**
     * Rétablit les codes secrets par défaut (4444 et 1234) :
     * Réinitialise SharedPreferences, Room et synchronise avec Appwrite Cloud.
     */
    fun resetToDefault(
        context: Context,
        userId: String,
        userPhone: String,
        onComplete: (success: Boolean) -> Unit
    ) {
        val prefs = AutomationPreferences(context)
        prefs.resetDialerCodeToDefault(userPhone)

        scope.launch {
            try {
                val dao = AppDatabase.getDatabase(context).userDialerCodeDao()
                val entity = UserDialerCodeEntity(
                    userId = userId.ifBlank { "user_$userPhone" },
                    userPhone = userPhone,
                    secretCode = "",
                    isCustomized = false,
                    updatedAt = System.currentTimeMillis()
                )
                dao.insertOrUpdate(entity)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur reset Room: ${e.message}")
            }
        }

        AutomationLogRepository.log(
            tag = "DIALER_CODE",
            message = "Codes composeur rétablis par défaut (4444 et 1234) pour $userPhone",
            type = LogType.INFO,
            source = userPhone
        )

        syncToAppwriteCloud(context, userId, userPhone, "", isCustomized = false) { _, _ ->
            postToMain { onComplete(true) }
        }
    }

    /**
     * Synchronise les réglages vers Appwrite Cloud :
     * 1. Dans la collection (table) de la base de données : 'user_dialer_codes'
     * 2. Dans les préférences utilisateur Appwrite : '/users/{userId}/prefs'
     */
    private fun syncToAppwriteCloud(
        context: Context,
        userId: String,
        userPhone: String,
        secretCode: String,
        isCustomized: Boolean,
        onResult: (Boolean, String?) -> Unit
    ) {
        val effectiveDocId = userId.ifBlank { "user_${userPhone.filter { it.isDigit() }}" }
        if (effectiveDocId.isBlank()) {
            onResult(true, null)
            return
        }

        val endpoint = AutomationPreferences.DEFAULT_ENDPOINT
        val projectId = AutomationPreferences.DEFAULT_PROJECT_ID
        val databaseId = AutomationPreferences.DEFAULT_DATABASE_ID
        val collectionId = "user_dialer_codes"
        val apiKey = AutomationPreferences.DEFAULT_API_KEY

        val docData = JSONObject().apply {
            put("userId", effectiveDocId)
            put("userPhone", userPhone)
            put("secretCode", secretCode)
            put("isCustomized", isCustomized)
            put("updatedAt", System.currentTimeMillis())
        }

        // 1. Sauvegarde dans la table Appwrite Database 'user_dialer_codes'
        val docUrl = "$endpoint/databases/$databaseId/collections/$collectionId/documents/$effectiveDocId"
        val patchBody = JSONObject().apply { put("data", docData) }.toString()

        val patchReq = Request.Builder()
            .url(docUrl)
            .header("X-Appwrite-Project", projectId)
            .header("X-Appwrite-Key", apiKey)
            .header("Content-Type", "application/json")
            .patch(patchBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(patchReq).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                syncToUserPrefsOnly(endpoint, projectId, apiKey, userId, userPhone, secretCode, isCustomized, onResult)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 404) {
                    // Document n'existe pas encore, on le crée
                    val createUrl = "$endpoint/databases/$databaseId/collections/$collectionId/documents"
                    val createBody = JSONObject().apply {
                        put("documentId", effectiveDocId)
                        put("data", docData)
                    }.toString()

                    val postReq = Request.Builder()
                        .url(createUrl)
                        .header("X-Appwrite-Project", projectId)
                        .header("X-Appwrite-Key", apiKey)
                        .header("Content-Type", "application/json")
                        .post(createBody.toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    httpClient.newCall(postReq).enqueue(object : Callback {
                        override fun onFailure(c: Call, ex: IOException) {
                            syncToUserPrefsOnly(endpoint, projectId, apiKey, userId, userPhone, secretCode, isCustomized, onResult)
                        }
                        override fun onResponse(c: Call, r: Response) {
                            val prefs = AutomationPreferences(context)
                            prefs.isDialerSyncPending = false
                            syncToUserPrefsOnly(endpoint, projectId, apiKey, userId, userPhone, secretCode, isCustomized, onResult)
                        }
                    })
                } else if (response.isSuccessful) {
                    val prefs = AutomationPreferences(context)
                    prefs.isDialerSyncPending = false
                    syncToUserPrefsOnly(endpoint, projectId, apiKey, userId, userPhone, secretCode, isCustomized, onResult)
                } else {
                    syncToUserPrefsOnly(endpoint, projectId, apiKey, userId, userPhone, secretCode, isCustomized, onResult)
                }
            }
        })
    }

    private fun syncToUserPrefsOnly(
        endpoint: String,
        projectId: String,
        apiKey: String,
        userId: String,
        userPhone: String,
        secretCode: String,
        isCustomized: Boolean,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (userId.isBlank()) {
            onResult(true, null)
            return
        }

        val url = "$endpoint/users/$userId/prefs"
        val payload = JSONObject().apply {
            put("dialerCode", secretCode)
            put("isCustomized", isCustomized)
            put("userPhone", userPhone)
            put("updatedAt", System.currentTimeMillis())
        }
        val requestBodyJson = JSONObject().apply {
            put("prefs", payload)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .header("X-Appwrite-Project", projectId)
            .header("X-Appwrite-Key", apiKey)
            .header("Content-Type", "application/json")
            .patch(requestBodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(true, null)
            }
            override fun onResponse(call: Call, response: Response) {
                onResult(true, null)
            }
        })
    }

    /**
     * Synchronisation bidirectionnelle intelligente :
     * - Si des modifications locales sont en attente (ex: modifiées hors ligne) ou forcePush = true :
     *   Envoie immédiatement les données locales vers la table Appwrite Database.
     * - Sinon, interroge Appwrite pour vérifier si un code plus récent existe et l'applique sans écraser les modifications locales.
     */
    fun syncWithAppwrite(
        context: Context,
        userId: String,
        userPhone: String,
        forcePush: Boolean = false,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val prefs = AutomationPreferences(context)
        val isPending = prefs.isDialerSyncPending
        val localCode = prefs.getSecretDialerCode(userPhone)
        val isCustom = prefs.isDialerCodeCustom(userPhone)

        if (isPending || forcePush) {
            Log.i(TAG, "Push des données locales vers Appwrite (code=$localCode, isCustom=$isCustom)...")
            syncToAppwriteCloud(context, userId, userPhone, localCode, isCustom) { success, _ ->
                if (success) {
                    prefs.isDialerSyncPending = false
                }
                onComplete?.invoke(success)
            }
        } else {
            fetchFromAppwriteCloud(context, userId, userPhone, onComplete)
        }
    }

    /**
     * Pousse automatiquement les modifications en attente dès que la connexion réseau est rétablie.
     */
    fun pushPendingSync(context: Context) {
        val prefs = AutomationPreferences(context)
        if (!prefs.isDialerSyncPending) return
        val userPhone = prefs.loggedInUserPhone
        val userId = prefs.loggedInUserId
        if (userPhone.isBlank() && userId.isBlank()) return
        Log.i(TAG, "Rétablissement réseau : synchronisation automatique du code composeur en attente...")
        syncWithAppwrite(context, userId, userPhone, forcePush = true)
    }

    /**
     * Récupère la configuration depuis Appwrite Cloud au démarrage ou à la connexion.
     */
    fun fetchFromAppwriteCloud(
        context: Context,
        userId: String,
        userPhone: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val effectiveDocId = userId.ifBlank { "user_${userPhone.filter { it.isDigit() }}" }
        if (effectiveDocId.isBlank()) {
            onComplete?.invoke(true)
            return
        }

        val endpoint = AutomationPreferences.DEFAULT_ENDPOINT
        val projectId = AutomationPreferences.DEFAULT_PROJECT_ID
        val databaseId = AutomationPreferences.DEFAULT_DATABASE_ID
        val collectionId = "user_dialer_codes"
        val apiKey = AutomationPreferences.DEFAULT_API_KEY

        val docUrl = "$endpoint/databases/$databaseId/collections/$collectionId/documents/$effectiveDocId"
        val request = Request.Builder()
            .url(docUrl)
            .header("X-Appwrite-Project", projectId)
            .header("X-Appwrite-Key", apiKey)
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onComplete?.invoke(false)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val cloudCode = json.optString("secretCode", "")
                        val cloudCustom = json.optBoolean("isCustomized", false)
                        val cloudUpdatedAt = json.optLong("updatedAt", 0L)

                        val prefs = AutomationPreferences(context)
                        val localUpdatedAt = prefs.dialerLastModified

                        // Si la modification locale est plus récente que le Cloud, on pousse le local vers le Cloud !
                        if (localUpdatedAt > cloudUpdatedAt && prefs.isDialerSyncPending) {
                            val localCode = prefs.getSecretDialerCode(userPhone)
                            val isCustom = prefs.isDialerCodeCustom(userPhone)
                            syncToAppwriteCloud(context, userId, userPhone, localCode, isCustom) { _, _ ->
                                prefs.isDialerSyncPending = false
                                onComplete?.invoke(true)
                            }
                            return
                        }

                        // Sinon, si le Cloud a un code personnalisé plus récent, on met à jour le local
                        if (cloudCustom && cloudCode.isNotBlank()) {
                            prefs.setSecretDialerCode(cloudCode, userPhone)
                            prefs.isDialerSyncPending = false

                            scope.launch {
                                val dao = AppDatabase.getDatabase(context).userDialerCodeDao()
                                dao.insertOrUpdate(
                                    UserDialerCodeEntity(
                                        userId = effectiveDocId,
                                        userPhone = userPhone,
                                        secretCode = cloudCode,
                                        isCustomized = true,
                                        updatedAt = cloudUpdatedAt
                                    )
                                )
                            }
                        }
                        onComplete?.invoke(true)
                    } catch (_: Exception) {
                        onComplete?.invoke(false)
                    }
                } else {
                    onComplete?.invoke(false)
                }
            }
        })
    }
}
