package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.AutomationLogRepository
import com.example.data.AutomationPreferences
import com.example.data.LogType
import com.example.service.AutomationForegroundService

/**
 * Récepteur système pour redémarrer automatiquement le service d'arrière-plan
 * dès que le smartphone est allumé ou redémarré (BOOT_COMPLETED).
 * Permet au service de tourner sans interruption même si l'icône est masquée.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "onReceive: action reçue = $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = AutomationPreferences(context)
            if (prefs.isKeepAliveEnabled) {
                Log.i(TAG, "Redémarrage automatique du Foreground Service suite au boot du téléphone")
                val serviceIntent = Intent(context, AutomationForegroundService::class.java).apply {
                    this.action = AutomationForegroundService.ACTION_START_SERVICE
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    AutomationLogRepository.log(
                        tag = "BOOT_AUTOSTART",
                        message = "Service relancé automatiquement au démarrage du téléphone ($action)",
                        type = LogType.SERVICE_LIFECYCLE
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur au lancement du service au boot: ${e.message}", e)
                    AutomationLogRepository.log(
                        tag = "BOOT_ERROR",
                        message = "Échec du démarrage automatique au boot: ${e.message}",
                        type = LogType.HTTP_ERROR
                    )
                }
            }
        }
    }
}
