package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AutomationLogRepository
import com.example.data.AutomationPreferences
import com.example.data.LogType
import com.example.network.NetworkDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 2. Foreground Service adaptatif pour assurer la persistance en tâche de fond.
 * - Gère le canal de notification obligatoire pour Android 8.0+ (API 26+).
 * - Implémente la déclaration foregroundServiceType="dataSync" pour Android 14 (API 34).
 */
class AutomationForegroundService : Service() {

    companion object {
        private const val TAG = "AutoForegroundService"
        const val CHANNEL_ID = "automation_bridge_channel_v1"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "com.example.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val ACTION_PROCESS_SMS = "com.example.action.PROCESS_SMS"
        const val ACTION_TRIGGER_PING = "com.example.action.TRIGGER_PING"

        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_CONTENT = "extra_content"

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var heartbeatJob: Job? = null
    private var startTimeMillis: Long = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Initialisation du Foreground Service")
        createNotificationChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SERVICE
        Log.d(TAG, "onStartCommand avec action: $action")

        when (action) {
            ACTION_STOP_SERVICE -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_PROCESS_SMS -> {
                handleIncomingSmsIntent(intent)
            }
            ACTION_TRIGGER_PING -> {
                handleTestPingIntent()
            }
            ACTION_START_SERVICE -> {
                startForegroundWithNotification()
            }
        }

        return START_STICKY
    }

    /**
     * Démarre le service en mode Foreground avec compatibilité Android 8.0+ à Android 14+.
     */
    private fun startForegroundWithNotification() {
        if (!isRunning) {
            isRunning = true
            startTimeMillis = System.currentTimeMillis()
            AutomationLogRepository.setServiceStatus(true)
            startHeartbeatMonitoring()
        }

        val notification = buildForegroundNotification(
            title = "AutoBridge Actif",
            content = "Surveillance active des événements et dispatch réseau"
        )

        // Gestion de la compatibilité Android 14+ (API 34) avec foregroundServiceType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Traitement d'un SMS transmis par le BroadcastReceiver ou la simulation.
     */
    private fun handleIncomingSmsIntent(intent: Intent?) {
        val source = intent?.getStringExtra(EXTRA_SOURCE) ?: "Inconnu"
        val content = intent?.getStringExtra(EXTRA_CONTENT) ?: ""
        
        Log.i(TAG, "Service: Traitement du SMS de [$source]: $content")
        
        // Mise à jour de la notification avec le dernier événement
        updateNotification("Dernier SMS traité : $source", "Contenu : ${content.take(30)}...")
    }

    /**
     * Exécute un test de ping réseau depuis le service en arrière-plan.
     */
    private fun handleTestPingIntent() {
        val prefs = AutomationPreferences(this)
        NetworkDispatcher.transmitData(
            targetUrl = prefs.targetUrl,
            source = "ForegroundService-Ping",
            content = "Test de persistance et communication réseau active"
        )
    }

    /**
     * Tâche de fond éducative maintenant l'activité du service et journalisant les battements périodiques.
     */
    private fun startHeartbeatMonitoring() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            var cycleCount = 0
            while (isActive) {
                delay(60_000) // Vérification toutes les 60 secondes
                cycleCount++
                val uptimeSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000
                Log.d(TAG, "Heartbeat Service - En ligne depuis $uptimeSeconds s (Cycle #$cycleCount)")
            }
        }
    }

    /**
     * Construction du canal de notification obligatoire pour Android 8.0+ (API 26).
     */
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Construction de la notification persistante de premier plan.
     */
    private fun buildForegroundNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AutomationForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingOpenApp)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Arrêter",
                pendingStop
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildForegroundNotification(title, content))
    }

    private fun stopForegroundService() {
        Log.d(TAG, "Arrêt du Foreground Service demandé")
        heartbeatJob?.cancel()
        isRunning = false
        AutomationLogRepository.setServiceStatus(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Libération des ressources du Foreground Service")
        heartbeatJob?.cancel()
        isRunning = false
        AutomationLogRepository.setServiceStatus(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
