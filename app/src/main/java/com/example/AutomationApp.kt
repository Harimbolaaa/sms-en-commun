package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.AutomationLogRepository
import com.example.data.LogType
import com.example.data.RegexRuleRepository
import com.example.data.SmsQueueRepository
import com.example.service.AutomationForegroundService

/**
 * Classe Application principale pour l'initialisation des canaux de notification système,
 * de la base de données locale Room (logs, règles Regex et file d'attente retry)
 * et du listener de reconnexion réseau.
 */
class AutomationApp : Application() {

    companion object {
        private const val TAG = "AutomationApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Démarrage de l'application AutoBridge Edu")
        
        // Initialisation de la persistance locale Room pour les logs, les règles et la file d'attente
        AutomationLogRepository.init(this)
        RegexRuleRepository.init(this)
        SmsQueueRepository.init(this)

        // Initialisation des canaux de notification
        createNotificationChannel()

        AutomationLogRepository.log(
            tag = "APP",
            message = "Application initialisée avec persistance SQLite Room et File d'attente (Queue Retry)",
            type = LogType.INFO,
            details = "Support des en-têtes HTTP de sécurité et rétention locale en cas de panne réseau."
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AutomationForegroundService.CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }
}
