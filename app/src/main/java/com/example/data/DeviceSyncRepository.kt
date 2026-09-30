package com.example.data

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.AutomationLogEntity
import com.example.data.db.DeviceSyncConfigEntity
import com.example.network.NetworkDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Repository gérant la configuration et la synchronisation des appareils (SOURCE vs PARTAGE).
 * Règle métier :
 * - Tous les appareils d'un client (Source et Partage) se connectent avec le MÊME COMPTE CLIENT (même identifiant/téléphone).
 * - L'appareil principal reste en mode SOURCE (Maître).
 * - Sur les appareils satellites, l'utilisateur active simplement le bouton "Partage activé" (Mode PARTAGE).
 * - Isolation stricte : Chaque compte client ne voit QUE ses propres SMS dans Appwrite Cloud (filtrage strict par clientPhone / clientUserId).
 * - Bidirectionnel : Les SMS REÇUS et les SMS ENVOYÉS par les appareils Partage sont automatiquement transmis à Appwrite Cloud
 *   pour arriver et être centralisés dans l'appareil Source, exactement comme les reçus.
 */
object DeviceSyncRepository {

    private const val TAG = "DeviceSyncRepository"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
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
     * Observe la configuration locale Room du rôle de l'appareil
     */
    fun observeConfig(context: Context, userPhone: String): Flow<DeviceSyncConfigEntity?> {
        val db = AppDatabase.getDatabase(context)
        return db.deviceSyncConfigDao().observeConfig(userPhone.ifBlank { "default_user" })
    }

    /**
     * Enregistre la configuration dans Room DB locale ET dans SharedPreferences.
     * Les appareils Partage et Source partagent le MÊME compte client utilisateur.
     */
    fun saveDeviceConfig(
        context: Context,
        userPhone: String,
        role: String,
        sourceAccountPhone: String = "",
        onDone: (() -> Unit)? = null
    ) {
        val prefs = AutomationPreferences(context)
        val cleanPhone = userPhone.ifBlank { prefs.loggedInUserPhone }
        val cleanRole = if (role.equals("PARTAGE", ignoreCase = true)) "PARTAGE" else "SOURCE"
        // Le compte Source est TOUJOURS le compte client lui-même (même compte connecté)
        val cleanSourcePhone = if (sourceAccountPhone.isNotBlank()) sourceAccountPhone.trim() else cleanPhone
        val isSource = cleanRole == "SOURCE"

        prefs.setDeviceRole(cleanPhone, cleanRole)
        prefs.setSourceAccountPhone(cleanPhone, cleanSourcePhone)

        val entity = DeviceSyncConfigEntity(
            userPhone = cleanPhone,
            deviceRole = cleanRole,
            sourceAccountPhone = cleanSourcePhone,
            isSource = isSource,
            updatedAt = System.currentTimeMillis()
        )

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.deviceSyncConfigDao().saveConfig(entity)
                Log.i(TAG, "Configuration sauvegardée : Compte=$cleanPhone, Role=$cleanRole, isSource=$isSource")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur enregistrement Room DeviceSyncConfig", e)
            }
            postToMainThread {
                onDone?.invoke()
            }
        }
    }

    /**
     * Synchronise tous les SMS (Reçus ET Envoyés) depuis Appwrite Cloud pour le compte connecté
     * et les insère dans la base SQLite Room locale (automation_logs).
     * ISOLATION STRICTE : Un client ne peut voir QUE ses propres SMS (aucun SMS d'autres clients).
     */
    fun syncSharedSmsFromAppwrite(
        context: Context,
        sourcePhone: String,
        onComplete: ((Boolean, Int, String) -> Unit)? = null
    ) {
        val prefs = AutomationPreferences(context)
        val effectiveSource = sourcePhone.ifBlank { prefs.loggedInUserPhone }
        val loggedInUserId = prefs.loggedInUserId

        val orderQuery = java.net.URLEncoder.encode("""{"method":"orderDesc","attribute":"${'$'}createdAt"}""", "UTF-8")
        val targetUrl = "${AutomationPreferences.DEFAULT_ENDPOINT}/databases/${AutomationPreferences.DEFAULT_DATABASE_ID}/collections/${AutomationPreferences.DEFAULT_COLLECTION_ID}/documents?queries[0]=$orderQuery&limit=100"

        val request = Request.Builder()
            .url(targetUrl)
            .get()
            .header("X-Appwrite-Project", AutomationPreferences.DEFAULT_PROJECT_ID)
            .header("X-Appwrite-Key", AutomationPreferences.DEFAULT_API_KEY)
            .header("Content-Type", "application/json")
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val msg = "Échec connexion Appwrite : ${e.localizedMessage}"
                Log.e(TAG, msg, e)
                postToMainThread {
                    onComplete?.invoke(false, 0, msg)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = "Erreur Appwrite (HTTP ${response.code})"
                    postToMainThread {
                        onComplete?.invoke(false, 0, msg)
                    }
                    return
                }

                scope.launch {
                    var addedCount = 0
                    try {
                        val root = JSONObject(body)
                        val documents = root.optJSONArray("documents") ?: org.json.JSONArray()
                        val db = AppDatabase.getDatabase(context)
                        val logDao = db.logDao()

                        for (i in 0 until documents.length()) {
                            val doc = documents.getJSONObject(i)
                            val docClientPhone = doc.optString("clientPhone", "").trim()
                            val docClientUserId = doc.optString("clientUserId", "").trim()
                            val docSender = doc.optString("sender", "Inconnu")
                            val docRecipient = doc.optString("recipient", "")
                            val docRawMessage = doc.optString("rawMessage", "")
                            val docParsedAmount = doc.optLong("parsedAmount", 0L)
                            val docRef = doc.optString("referenceId", "")
                            val docOperator = doc.optString("operator", "")
                            val docDirection = doc.optString("direction", "")
                            val docCreated = doc.optLong("createdAtUnix", System.currentTimeMillis())

                            // RÈGLE CRITIQUE D'ISOLATION STRICTE :
                            // Ne matcher QUE si le document appartient strictement au compte client connecté !
                            // Aucun autre compte client ne peut voir les SMS des autres.
                            val isMatch = (effectiveSource.isNotBlank() && docClientPhone == effectiveSource) ||
                                          (loggedInUserId.isNotBlank() && docClientUserId == loggedInUserId)

                            if (isMatch && docRawMessage.isNotBlank()) {
                                val searchKey = if (docRef.isNotBlank()) docRef else docRawMessage.take(40)
                                val exists = logDao.countLogsWithQuerySync(searchKey) > 0

                                if (!exists) {
                                    // Détecte si le message a été ENVOYÉ par un appareil Partage ou REÇU
                                    val isSentSms = docDirection.equals("SENT", ignoreCase = true) ||
                                                    docOperator == "PARTAGE_SENT" ||
                                                    docRef.startsWith("SENT-") ||
                                                    docSender.contains("MOI (Partage", ignoreCase = true) ||
                                                    docRawMessage.startsWith("SMS envoyé")

                                    val targetContact = if (isSentSms && docRecipient.isNotBlank()) {
                                        docRecipient
                                    } else {
                                        docSender
                                    }

                                    val logTag = if (isSentSms) "SMS_SENT" else "SMS_RECEIVER"
                                    val logType = if (isSentSms) "SMS_SENT" else "SMS_INTERCEPTED"
                                    val logTitle = if (isSentSms) {
                                        "SMS envoyé (Partage) vers $targetContact"
                                    } else {
                                        "SMS de $docSender (${if (docParsedAmount > 0) "$docParsedAmount Ar" else "SMS brut"})"
                                    }

                                    val logDetails = if (isSentSms) {
                                        "SMS envoyé par votre appareil Partage\nCompte client: $effectiveSource\nDestinataire: $targetContact\nSMS source: $docRawMessage\nOpérateur: $docOperator\nRéf: $docRef"
                                    } else {
                                        "SMS synchronisé depuis Cloud Appwrite\nCompte Source: $effectiveSource\nÉmetteur: $docSender\nSMS source: $docRawMessage\nOpérateur: $docOperator\nRéf: $docRef"
                                    }

                                    val newLog = AutomationLogEntity(
                                        timestamp = docCreated,
                                        tag = logTag,
                                        message = logTitle,
                                        type = logType,
                                        details = logDetails,
                                        payloadJson = doc.toString(),
                                        source = targetContact,
                                        status = if (isSentSms) "SENT" else "SUCCESS",
                                        userPhone = effectiveSource,
                                        userId = loggedInUserId
                                    )
                                    logDao.insertLog(newLog)
                                    addedCount++
                                }
                            }
                        }

                        Log.i(TAG, "Synchronisation terminée : $addedCount SMS importés pour le compte $effectiveSource.")
                        postToMainThread {
                            onComplete?.invoke(true, addedCount, "$addedCount SMS (reçus & envoyés) synchronisés pour votre compte !")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur parsing synchro Cloud", e)
                        postToMainThread {
                            onComplete?.invoke(false, 0, "Erreur traitement : ${e.localizedMessage}")
                        }
                    }
                }
            }
        })
    }

    /**
     * Transmet un SMS ENVOYÉ par cet appareil en mode PARTAGE vers Appwrite Cloud,
     * pour qu'il arrive et soit visible dans l'appareil Source du client.
     */
    fun uploadSentSmsFromSharingDevice(
        context: Context,
        recipient: String,
        messageContent: String,
        smsTimestamp: Long = System.currentTimeMillis(),
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val prefs = AutomationPreferences(context)
        val userPhone = prefs.loggedInUserPhone
        val userId = prefs.loggedInUserId

        val activeHeaders = prefs.getActiveHeadersMap().toMutableMap()
        SmsQueueRepository.sanitizeHeaders(activeHeaders)
        val effectiveTargetUrl = SmsQueueRepository.sanitizeUrl(prefs.targetUrl, AutomationPreferences.DEFAULT_TARGET_URL)

        val ref = "SENT-${System.currentTimeMillis() % 1000000}"
        val formattedRaw = if (messageContent.startsWith("SMS envoyé")) messageContent else "SMS envoyé à $recipient : $messageContent"

        val payload = JSONObject().apply {
            put("documentId", "unique()")
            put("data", JSONObject().apply {
                put("sender", "MOI (Partage $userPhone)")
                put("rawMessage", formattedRaw)
                put("parsedAmount", 0L)
                put("referenceId", ref)
                put("operator", "PARTAGE_SENT")
                put("createdAtUnix", smsTimestamp)
                put("clientPhone", userPhone.trim())
                put("clientUserId", userId)
            })
        }.toString()

        // 1. Enregistrement local Room pour cet appareil
        AutomationLogRepository.log(
            tag = "SMS_SENT",
            message = "SMS envoyé (Partage) vers $recipient",
            type = LogType.SMS_INTERCEPTED,
            details = "SMS envoyé depuis l'appareil Partage\nDestinataire: $recipient\nMessage: $messageContent\nRéf: $ref",
            payloadJson = payload,
            source = recipient,
            userPhone = userPhone,
            userId = userId
        )

        // 2. Transmission vers Appwrite Cloud
        NetworkDispatcher.transmitDataWithCustomPayload(
            targetUrl = effectiveTargetUrl,
            payloadJson = payload,
            source = recipient,
            contentLength = formattedRaw.length,
            headers = activeHeaders,
            userPhone = userPhone,
            userId = userId,
            onComplete = { success, code, msg ->
                val feedback = if (success) {
                    "SMS envoyé synchronisé sur Appwrite Cloud (HTTP $code)"
                } else {
                    "Échec transmission SMS envoyé (Code $code) : $msg"
                }
                postToMainThread {
                    onComplete?.invoke(success, feedback)
                }
            }
        )
    }

    /**
     * Envoie un SMS de test (Reçu ou Envoyé) depuis cet appareil en mode PARTAGE vers Appwrite Cloud
     */
    fun sendTestSharedSms(
        context: Context,
        sharingDevicePhone: String,
        sourceAccountPhone: String,
        isSentDirection: Boolean = false,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val prefs = AutomationPreferences(context)
        val clientPhone = sharingDevicePhone.ifBlank { prefs.loggedInUserPhone }

        if (isSentDirection) {
            uploadSentSmsFromSharingDevice(
                context = context,
                recipient = "0340000000",
                messageContent = "Test SMS Envoyé depuis le matériel Partage vers le client Source $clientPhone. Tout fonctionne !",
                onComplete = onComplete
            )
            return
        }

        val activeHeaders = prefs.getActiveHeadersMap().toMutableMap()
        SmsQueueRepository.sanitizeHeaders(activeHeaders)
        val effectiveTargetUrl = SmsQueueRepository.sanitizeUrl(prefs.targetUrl, AutomationPreferences.DEFAULT_TARGET_URL)

        val ref = "REF-SHARE-${System.currentTimeMillis() % 100000}"
        val rawSms = "Transfert Partage : Credit MVOLA recu : 15000 Ar sur l'appareil Partage pour le compte client $clientPhone. Ref: $ref."

        val payload = JSONObject().apply {
            put("documentId", "unique()")
            put("data", JSONObject().apply {
                put("sender", "MVOLA")
                put("rawMessage", rawSms)
                put("parsedAmount", 15000L)
                put("referenceId", ref)
                put("operator", "MVOLA")
                put("direction", "RECEIVED")
                put("createdAtUnix", System.currentTimeMillis())
                put("clientPhone", clientPhone.trim())
                put("clientUserId", prefs.loggedInUserId)
            })
        }.toString()

        NetworkDispatcher.transmitDataWithCustomPayload(
            targetUrl = effectiveTargetUrl,
            payloadJson = payload,
            source = "MVOLA",
            contentLength = rawSms.length,
            headers = activeHeaders,
            userPhone = clientPhone.trim(),
            userId = prefs.loggedInUserId,
            onComplete = { success, code, msg ->
                val feedback = if (success) "SMS reçu de test envoyé vers Appwrite Cloud pour votre compte (HTTP $code) !"
                else "Échec transmission (Code $code): $msg"
                postToMainThread {
                    onComplete?.invoke(success, feedback)
                }
            }
        )
    }
}
