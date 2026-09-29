package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.MainActivity
import com.example.data.AutomationLogRepository
import com.example.data.AutomationPreferences
import com.example.data.LogType
import com.example.util.AppIconManager

/**
 * Receiver appelé pour réafficher l'icône de l'application et ouvrir l'interface.
 * Déclenché soit par :
 * - Le code secret composé sur le composeur téléphonique (Dialer) : *#*#4444#*#* ou *#*#1234#*#*
 * - Un intent explicite "com.example.ACTION_UNHIDE_APP" (depuis notification ou commande ADB).
 */
class AppUnhideReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: ""
        val dialedCode = intent?.data?.host ?: ""
        Log.i("AppUnhideReceiver", "Demande de réactivation de l'icône reçue (action=$action, dialedCode=$dialedCode)")

        val prefs = AutomationPreferences(context)

        // Si l'action provient du composeur téléphonique SECRET_CODE (*#*#XXXX#*#*) :
        if (action == "android.provider.Telephony.SECRET_CODE") {
            val customCode = prefs.getSecretDialerCode()
            val isCustomized = prefs.isDialerCodeCustom()

            val isMatch = if (isCustomized && customCode.isNotBlank()) {
                // Le code personnalisé écrase les codes par défaut
                dialedCode == customCode
            } else {
                // Par défaut, les deux codes 4444 et 1234 sont acceptés
                dialedCode == "4444" || dialedCode == "1234" || (customCode.isNotBlank() && dialedCode == customCode)
            }

            if (!isMatch) {
                Log.d("AppUnhideReceiver", "Code composé '$dialedCode' ne correspond pas au code actif (custom=$customCode)")
                return
            }
        }

        // 1. Réactiver immédiatement l'icône du lanceur
        AppIconManager.setIconVisibility(context, visible = true)

        // 2. Mettre à jour les préférences utilisateur
        prefs.isHideIconOnClose = false

        AutomationLogRepository.log(
            tag = "APP_UNHIDE",
            message = "Icône réactivée (Code composé: ${if (dialedCode.isNotBlank()) "*#*#$dialedCode#*#*" else "Action directe"})",
            type = LogType.INFO,
            source = if (dialedCode.isNotBlank()) "*#*#$dialedCode#*#*" else "DIRECT"
        )

        try {
            Toast.makeText(context, "Icône de l'application réaffichée avec succès !", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {}

        // 3. Ouvrir l'application
        try {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(openIntent)
        } catch (e: Exception) {
            Log.e("AppUnhideReceiver", "Erreur ouverture MainActivity: ${e.message}")
        }
    }
}
