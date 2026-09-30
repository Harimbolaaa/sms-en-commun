package com.example.util

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.AutomationPreferences
import com.example.data.DeviceSyncRepository

/**
 * Surveille la boîte d'envoi SMS Android (content://sms/sent).
 * Lorsque l'appareil est configuré avec "Partage activé" (Mode PARTAGE) pour le compte client,
 * tout SMS envoyé par ce matériel est automatiquement capturé et transmis vers Appwrite Cloud
 * sous le compte du client pour apparaître dans son appareil Source.
 */
object SentSmsTracker {

    private const val TAG = "SentSmsTracker"
    private var isObserverRegistered = false
    private var lastObservedSmsId = 0L
    private var contentObserver: ContentObserver? = null

    fun startTracking(context: Context) {
        if (isObserverRegistered) return
        val appContext = context.applicationContext
        val resolver = appContext.contentResolver

        try {
            val cursor = resolver.query(
                Uri.parse("content://sms/sent"),
                arrayOf("_id"),
                null,
                null,
                "_id DESC LIMIT 1"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    lastObservedSmsId = it.getLong(0)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Impossible d'initialiser le dernier id de SMS envoyé : ${e.message}")
        }

        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                checkAndDispatchSentSms(appContext)
            }
        }

        try {
            resolver.registerContentObserver(
                Uri.parse("content://sms"),
                true,
                contentObserver!!
            )
            isObserverRegistered = true
            Log.i(TAG, "Surveillance des SMS envoyés (content://sms) démarrée avec succès.")
        } catch (e: Exception) {
            Log.w(TAG, "Impossible d'enregistrer l'observateur SMS envoyés", e)
        }
    }

    private fun checkAndDispatchSentSms(context: Context) {
        val prefs = AutomationPreferences(context)
        val userPhone = prefs.loggedInUserPhone
        val deviceRole = prefs.getDeviceRole(userPhone)
        val isSharing = deviceRole.equals("PARTAGE", ignoreCase = true)

        // RÈGLE : Ne transférer que si le Partage est activé sur cet appareil pour ce compte client
        if (!isSharing || userPhone.isBlank()) return

        try {
            val cursor = context.contentResolver.query(
                Uri.parse("content://sms/sent"),
                arrayOf("_id", "address", "body", "date"),
                "_id > ?",
                arrayOf(lastObservedSmsId.toString()),
                "_id DESC LIMIT 10"
            )
            cursor?.use {
                val idIdx = it.getColumnIndex("_id")
                val addrIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val address = if (addrIdx != -1) it.getString(addrIdx) ?: "Destinataire" else "Destinataire"
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()

                    if (id > lastObservedSmsId) {
                        lastObservedSmsId = id
                    }

                    if (body.isNotBlank()) {
                        Log.i(TAG, "Nouveau SMS envoyé détecté sur l'appareil Partage vers [$address]: $body")
                        DeviceSyncRepository.uploadSentSmsFromSharingDevice(
                            context = context,
                            recipient = address,
                            messageContent = body,
                            smsTimestamp = date
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erreur lecture des SMS envoyés", e)
        }
    }
}
