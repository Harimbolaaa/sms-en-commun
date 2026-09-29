package com.example.receiver

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import com.example.data.AutomationLogRepository
import com.example.data.AutomationPreferences
import com.example.data.ExtractionEngine
import com.example.data.LogType
import com.example.data.RegexRuleRepository
import com.example.network.NetworkDispatcher
import com.example.service.AutomationForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 1. BroadcastReceiver standard configuré avec priorité 999.
 * Détecte l'action système SMS_RECEIVED et applique les règles de filtrage d'expéditeur,
 * l'extraction Regex multi-modèles groupés cochés, et la transmission HTTP avec En-tête sécurisé.
 * En cas d'échec de transmission HTTP, le SMS est automatiquement mis en file d'attente (Queue)
 * pour être réessayé plus tard dès le retour de la connexion.
 */
class SmsAutomationReceiver : android.content.BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsAutomationReceiver"
        const val SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        Log.d(TAG, "onReceive déclenché avec l'action: $action")

        if (action == SMS_RECEIVED_ACTION) {
            val bundle: Bundle? = intent.extras
            if (bundle == null) {
                Log.w(TAG, "Intent reçu sans Bundle extras")
                return
            }

            try {
                // Utilisation de goAsync() pour permettre un traitement asynchrone sécurisé
                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        processSmsBundle(context, bundle)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Erreur dans la coroutine de réception SMS", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du traitement asynchrone du SMS", e)
            }
        }
    }

    /**
     * Analyse le Bundle contenant les PDUs SMS avec gestion de la compatibilité API 23 -> API 34+.
     */
    private suspend fun processSmsBundle(context: Context, bundle: Bundle) {
        val pdus = bundle.get("pdus") as? Array<*>
        if (pdus.isNullOrEmpty()) {
            Log.w(TAG, "Aucun PDU trouvé dans le Bundle extras")
            return
        }

        val format = bundle.getString("format")
        val messagesBySender = mutableMapOf<String, StringBuilder>()

        for (pdu in pdus) {
            val pduByteArray = pdu as? ByteArray ?: continue
            val smsMessage: SmsMessage? = createSmsMessageFromPdu(pduByteArray, format)

            if (smsMessage != null) {
                val sender = smsMessage.displayOriginatingAddress 
                    ?: smsMessage.originatingAddress 
                    ?: "Inconnu"
                val body = smsMessage.displayMessageBody 
                    ?: smsMessage.messageBody 
                    ?: ""

                val builder = messagesBySender.getOrPut(sender) { StringBuilder() }
                builder.append(body)
            }
        }

        val prefs = AutomationPreferences(context)
        val isUserLoggedIn = prefs.isLoggedIn
        val loggedInUserPhone = prefs.loggedInUserPhone
        val loggedInUserId = prefs.loggedInUserId

        // RÈGLE STRICTE PASSERELLE SMS :
        // Si et seulement si un compte utilisateur client est connecté sur cet appareil,
        // la passerelle SMS (SMS Gateway) a le droit d'effectuer les actions.
        if (!isUserLoggedIn || loggedInUserPhone.isBlank()) {
            Log.w(TAG, "Passerelle SMS bloquée : aucun compte utilisateur connecté sur cet appareil.")
            AutomationLogRepository.log(
                tag = "GATEWAY_AUTH",
                message = "Passerelle SMS bloquée : aucun compte connecté",
                type = LogType.INFO,
                details = "L'action SMS Gateway nécessite qu'un compte utilisateur client soit authentifié sur cet appareil.",
                source = "AUTH_GATEWAY"
            )
            return
        }

        val targetUrl = prefs.targetUrl
        val isAutoForwardEnabled = prefs.isAutoForwardEnabled
        val isSenderFilterEnabled = prefs.isSenderFilterEnabled
        val allowedSenders = prefs.allowedSenders
        val isRegexEnabled = prefs.isRegexEnabled
        val sendOnlyExtracted = prefs.sendOnlyExtracted
        val customHeaderName = if (prefs.isCustomHeaderEnabled) prefs.customHeaderName else null
        val customHeaderValue = if (prefs.isCustomHeaderEnabled) prefs.customHeaderValue else null

        // Récupération des modèles cochés pour extraction groupée
        val checkedRules = try {
            RegexRuleRepository.getCheckedRulesSync()
        } catch (e: Exception) {
            emptyList()
        }

        messagesBySender.forEach { (sender, bodyBuilder) ->
            val fullBody = bodyBuilder.toString()
            Log.i(TAG, "SMS Intercepté de [$sender] pour le client [$loggedInUserPhone]: $fullBody")

            // 1. Vérification de la règle de filtrage d'expéditeur
            val isAllowed = ExtractionEngine.isSenderAllowed(sender, allowedSenders, isSenderFilterEnabled)
            if (!isAllowed) {
                Log.i(TAG, "SMS de [$sender] ignoré (Expéditeur non autorisé dans la liste de filtrage)")
                AutomationLogRepository.log(
                    tag = "SMS_FILTER",
                    message = "SMS ignoré (Expéditeur '$sender' non autorisé)",
                    type = LogType.INFO,
                    details = "Expéditeur: $sender\nListe autorisée: $allowedSenders\nMessage non transmis à l'API.",
                    source = sender,
                    userPhone = loggedInUserPhone,
                    userId = loggedInUserId
                )
                return@forEach
            }

            // 2. Application de l'extraction groupée de tous les modèles cochés
            val extractionResult = if (checkedRules.isNotEmpty()) {
                ExtractionEngine.applyMultipleRules(fullBody, checkedRules, isRegexEnabled)
            } else {
                ExtractionEngine.applyRegex(fullBody, prefs.regexPattern, isRegexEnabled)
            }

            val finalPayload = ExtractionEngine.buildCustomJsonPayload(
                source = sender,
                originalContent = fullBody,
                extractionResult = extractionResult,
                sendOnlyExtracted = sendOnlyExtracted,
                clientPhone = loggedInUserPhone,
                clientUserId = loggedInUserId
            )

            // Journalisation de l'événement avec détails d'extraction
            val extractedSummary = if (isRegexEnabled && extractionResult.isMatch) {
                if (extractionResult.allExtractedValues.size > 1) {
                    "Multi-extractions (${extractionResult.allExtractedValues.size}): ${extractionResult.allExtractedValues.joinToString(" | ")}"
                } else {
                    "Donnée extraite: ${extractionResult.primaryValue}"
                }
            } else {
                "SMS brut reçu"
            }

            AutomationLogRepository.log(
                tag = "SMS_RECEIVER",
                message = "SMS de $sender ($extractedSummary)",
                type = LogType.SMS_INTERCEPTED,
                details = "Émetteur: $sender\nCompte Passerelle: $loggedInUserPhone\nSMS source: $fullBody\nModèles actifs: ${checkedRules.joinToString { it.title }.ifEmpty { "Modèle unique" }}\nDonnées extraites: ${if (isRegexEnabled) extractionResult.allExtractedValues.joinToString(", ").ifEmpty { "Aucune" } else "Désactivé"}\nFormat: ${format ?: "Standard"}",
                payloadJson = finalPayload,
                source = sender,
                userPhone = loggedInUserPhone,
                userId = loggedInUserId
            )

            // Transmission au Foreground Service pour maintenir la persistance
            val serviceIntent = Intent(context, AutomationForegroundService::class.java).apply {
                action = AutomationForegroundService.ACTION_PROCESS_SMS
                putExtra(AutomationForegroundService.EXTRA_SOURCE, sender)
                putExtra(AutomationForegroundService.EXTRA_CONTENT, fullBody)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Impossible de démarrer le service en arrière-plan: ${e.message}")
            }

            // Transmission automatique vers l'API HTTP avec En-têtes sécurisés multiples et Queue de secours
            if (isAutoForwardEnabled && targetUrl.isNotBlank()) {
                val activeHeaders = prefs.getActiveHeadersMap()
                NetworkDispatcher.transmitDataWithCustomPayload(
                    targetUrl = targetUrl,
                    payloadJson = finalPayload,
                    source = sender,
                    contentLength = fullBody.length,
                    headers = activeHeaders,
                    headerName = customHeaderName,
                    headerValue = customHeaderValue,
                    userPhone = loggedInUserPhone,
                    userId = loggedInUserId
                )
            }
        }
    }

    /**
     * Méthode de compatibilité pour convertir un PDU brut en objet SmsMessage
     * selon les différentes versions d'Android (API 23 à 34+).
     */
    @Suppress("DEPRECATION")
    private fun createSmsMessageFromPdu(pdu: ByteArray, format: String?): SmsMessage? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (format != null) {
                    SmsMessage.createFromPdu(pdu, format)
                } else {
                    SmsMessage.createFromPdu(pdu)
                }
            } else {
                SmsMessage.createFromPdu(pdu)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Échec du parsing du PDU SMS", e)
            null
        }
    }
}
