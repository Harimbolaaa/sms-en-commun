package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.util.AppIconManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.ui.PrettyJsonViewer
import com.example.ui.isJsonFormat
import com.example.ui.formatJsonString
import com.example.data.HttpHeaderItem
import com.example.ui.MultipleHeadersSection
import com.example.ui.AddHeaderDialog
import com.example.ui.HeaderPresetsDialog
import com.example.ui.DeleteHeaderDialog
import com.example.auth.AppwriteAuthService
import com.example.ui.AppwriteLoginScreen
import com.example.ui.ClientManagementDialog
import com.example.ui.ClientHomeScreen
import com.example.data.UserDialerRepository
import com.example.data.AppConfigRepository
import com.example.data.DeviceSyncRepository
import com.example.data.db.DeviceSyncConfigEntity
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.AutomationLogRepository
import com.example.data.AutomationPreferences
import com.example.data.ExtractionEngine
import com.example.data.ExtractionResult
import com.example.data.LogType
import com.example.data.RegexRuleRepository
import com.example.data.SmsQueueRepository
import com.example.data.db.AutomationLogEntity
import com.example.data.db.QueueItemEntity
import com.example.data.db.RegexRuleEntity
import com.example.network.NetworkDispatcher
import com.example.service.AutomationForegroundService
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkErrorBg
import com.example.ui.theme.DarkGreenBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeutralButton
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            com.example.util.SentSmsTracker.startTracking(this)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Impossible d'initialiser SentSmsTracker", e)
        }

        setContent {
            MyApplicationTheme {
                AutomationMainScreen(
                    onRequestBatteryOptimization = { requestIgnoreBatteryOptimizations() }
                )
            }
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                AutomationLogRepository.log(
                    tag = "BATTERY",
                    message = "Demande d'exemption d'optimisation batterie",
                    type = LogType.INFO
                )
            } else {
                Toast.makeText(this, "L'application est déjà exemptée des restrictions de batterie.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(fallbackIntent)
            } catch (fallbackEx: Exception) {
                Toast.makeText(this, "Impossible d'ouvrir les paramètres de batterie: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun AutomationMainScreen(
    onRequestBatteryOptimization: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AutomationPreferences(context) }

    // Authentification Appwrite Cloud
    var isUserLoggedIn by rememberSaveable { mutableStateOf(prefs.isUserLoggedIn) }
    var loggedInUserPhone by rememberSaveable { mutableStateOf(prefs.loggedInUserPhone) }
    var loggedInUserRole by rememberSaveable { mutableStateOf(prefs.loggedInUserRole) }
    var showClientManagementDialog by remember { mutableStateOf(false) }

    val isAdmin = loggedInUserRole.equals("admin", ignoreCase = true)

    val persistedLogs by remember(loggedInUserPhone, isAdmin) {
        AutomationLogRepository.getLogsForUser(loggedInUserPhone, isAdmin)
    }.collectAsState(initial = emptyList())
        
    val isServiceRunning by AutomationLogRepository.serviceRunning.collectAsState()
    val allRegexRules by remember { RegexRuleRepository.getAllRules() }
        .collectAsState(initial = RegexRuleRepository.DEFAULT_RULES)

    // Flux de la file d'attente (Queue) filtré par compte connecté (ou tout pour Admin)
    val queueItems by remember(loggedInUserPhone, isAdmin) {
        SmsQueueRepository.getQueueItemsForUser(loggedInUserPhone, isAdmin)
    }.collectAsState(initial = emptyList())
    val pendingQueueCount by remember(loggedInUserPhone, isAdmin) {
        SmsQueueRepository.getPendingCountForUser(loggedInUserPhone, isAdmin)
    }.collectAsState(initial = 0)
    val isQueueRetrying by SmsQueueRepository.isRetrying.collectAsState()
    val isNetworkAvailable by SmsQueueRepository.isNetworkAvailable.collectAsState()

    // États persistants au niveau global
    var targetUrlText by rememberSaveable { mutableStateOf(prefs.targetUrl.ifBlank { AutomationPreferences.DEFAULT_TARGET_URL }) }
    var customHeadersList by remember { mutableStateOf(prefs.getCustomHeaders()) }
    var showAddHeaderDialog by remember { mutableStateOf(false) }
    var showPresetsHeaderDialog by remember { mutableStateOf(false) }
    var headerToDelete by remember { mutableStateOf<HttpHeaderItem?>(null) }

    var isSenderFilterEnabled by rememberSaveable { mutableStateOf(prefs.isSenderFilterEnabled) }
    var allowedSendersText by rememberSaveable { mutableStateOf(prefs.allowedSenders) }
    var isRegexEnabled by rememberSaveable { mutableStateOf(prefs.isRegexEnabled) }
    var sendOnlyExtracted by rememberSaveable { mutableStateOf(prefs.sendOnlyExtracted) }

    // Synchronisation automatique en arrière-plan dès que la connexion réseau est détectée
    LaunchedEffect(isNetworkAvailable) {
        if (isNetworkAvailable) {
            UserDialerRepository.pushPendingSync(context)
            if (isUserLoggedIn && !isAdmin) {
                AppConfigRepository.fetchGlobalConfigFromAppwrite(context) { success, _ ->
                    if (success) {
                        targetUrlText = prefs.targetUrl
                        customHeadersList = prefs.getCustomHeaders()
                        allowedSendersText = prefs.allowedSenders
                        isSenderFilterEnabled = prefs.isSenderFilterEnabled
                        isRegexEnabled = prefs.isRegexEnabled
                        sendOnlyExtracted = prefs.sendOnlyExtracted
                    }
                }
            }
        }
    }

    // Récupération de la configuration globale définie par l'Admin à la connexion d'un client
    LaunchedEffect(isUserLoggedIn, isAdmin) {
        if (isUserLoggedIn && !isAdmin) {
            AppConfigRepository.fetchGlobalConfigFromAppwrite(context) { success, _ ->
                if (success) {
                    targetUrlText = prefs.targetUrl
                    customHeadersList = prefs.getCustomHeaders()
                    allowedSendersText = prefs.allowedSenders
                    isSenderFilterEnabled = prefs.isSenderFilterEnabled
                    isRegexEnabled = prefs.isRegexEnabled
                    sendOnlyExtracted = prefs.sendOnlyExtracted
                }
            }
        }
    }

    // Gestion Discrétion & Masquage de l'icône de l'application
    var isHideIconOnClose by rememberSaveable { mutableStateOf(prefs.isHideIconOnClose) }
    var isIconHiddenLive by remember { mutableStateOf(AppIconManager.isIconHidden(context)) }
    var showCloseConfirmDialog by remember { mutableStateOf(false) }
    var showUnhideInstructionsDialog by remember { mutableStateOf(false) }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showArchitectureDialog by remember { mutableStateOf(false) }
    var isSendingTestPing by remember { mutableStateOf(false) }

    // Redirection automatique pour le client : accès direct au menu Home (4) par défaut
    LaunchedEffect(isUserLoggedIn, loggedInUserRole) {
        if (isUserLoggedIn && !isAdmin && (selectedTab == 0 || selectedTab == 1)) {
            selectedTab = 4
        }
    }

    val handleCloseApplication: () -> Unit = {
        if (isHideIconOnClose) {
            AppIconManager.setIconVisibility(context, visible = false)
            isIconHiddenLive = true
            val activeCode = prefs.getSecretDialerCode(loggedInUserPhone)
            val codeDisplay = if (activeCode.isNotBlank()) "*#*#$activeCode#*#*" else "*#*#4444#*#* ou *#*#1234#*#*"
            Toast.makeText(context, "Icône masquée. Tapez $codeDisplay dans le composeur téléphonique pour la réafficher.", Toast.LENGTH_LONG).show()
        }
        (context as? Activity)?.finishAffinity()
    }

    var selectedLogForDetails by remember { mutableStateOf<AutomationLogEntity?>(null) }

    // Dialogues CRUD pour modèles Regex
    var ruleToEdit by remember { mutableStateOf<RegexRuleEntity?>(null) }
    var showCreateRuleDialog by remember { mutableStateOf(false) }
    var ruleToDelete by remember { mutableStateOf<RegexRuleEntity?>(null) }

    var hasSmsReceivePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasSmsReadPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var isBatteryExempted by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        hasSmsReceivePermission = permissionsMap[Manifest.permission.RECEIVE_SMS] ?: hasSmsReceivePermission
        hasSmsReadPermission = permissionsMap[Manifest.permission.READ_SMS] ?: hasSmsReadPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissionsMap[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    if (!isUserLoggedIn) {
        AppwriteLoginScreen(
            onLoginSuccess = { phone, role ->
                isUserLoggedIn = true
                loggedInUserPhone = phone
                loggedInUserRole = role
            }
        )
        return
    }

    if (showClientManagementDialog) {
        ClientManagementDialog(
            onDismiss = { showClientManagementDialog = false }
        )
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            SophisticatedHeader(
                onInfoClick = { showArchitectureDialog = true },
                pendingQueueCount = pendingQueueCount,
                userPhone = loggedInUserPhone,
                userRole = loggedInUserRole,
                onClientManagementClick = { showClientManagementDialog = true },
                onLogoutClick = {
                    AppwriteAuthService.logout(context) {
                        isUserLoggedIn = false
                        loggedInUserPhone = ""
                        loggedInUserRole = "client"
                    }
                },
                onCloseAppClick = { showCloseConfirmDialog = true }
            )
        },
        bottomBar = {
            SophisticatedNavBar(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                logCount = persistedLogs.size,
                pendingQueueCount = pendingQueueCount,
                isAdmin = isAdmin
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    if (isAdmin) {
                        SophisticatedDashboardContent(
                            targetUrl = targetUrlText,
                            onTargetUrlChange = { newUrl ->
                                targetUrlText = newUrl
                                prefs.targetUrl = newUrl
                            },
                            headersList = customHeadersList,
                            onToggleHeader = { id, isChecked ->
                                val updated = customHeadersList.map {
                                    if (it.id == id) it.copy(isEnabled = isChecked) else it
                                }
                                customHeadersList = updated
                                prefs.saveCustomHeaders(updated)
                            },
                            onUpdateHeader = { id, newKey, newVal ->
                                val updated = customHeadersList.map {
                                    if (it.id == id) it.copy(name = newKey, value = newVal) else it
                                }
                                customHeadersList = updated
                                prefs.saveCustomHeaders(updated)
                            },
                            onDeleteHeaderClick = { header ->
                                headerToDelete = header
                            },
                            onAddHeaderClick = {
                                showAddHeaderDialog = true
                            },
                            onPresetsClick = {
                                showPresetsHeaderDialog = true
                            },
                            onResetDefaultsClick = {
                                val defaults = AppConfigRepository.getDefaultAppwriteHeaders()
                                targetUrlText = AutomationPreferences.DEFAULT_TARGET_URL
                                prefs.targetUrl = AutomationPreferences.DEFAULT_TARGET_URL
                                customHeadersList = defaults
                                prefs.saveCustomHeaders(defaults)
                                Toast.makeText(context, "Valeurs par défaut Appwrite Cloud rétablies (URL & Clé secrète)", Toast.LENGTH_SHORT).show()
                            },
                            onSaveConfig = {
                                prefs.targetUrl = targetUrlText
                                prefs.saveCustomHeaders(customHeadersList)
                                val activeHeadersCount = customHeadersList.count { it.isEnabled }
                                Toast.makeText(context, "Sauvegarde locale OK. Propagation vers Appwrite Cloud...", Toast.LENGTH_SHORT).show()

                                // Propagation immédiate vers Appwrite Cloud pour tous les téléphones clients
                                AppConfigRepository.pushGlobalConfigToAppwrite(
                                    context = context,
                                    targetUrl = targetUrlText,
                                    headers = customHeadersList,
                                    allowedSenders = allowedSendersText,
                                    isSenderFilterEnabled = isSenderFilterEnabled,
                                    isRegexEnabled = isRegexEnabled,
                                    sendOnlyExtracted = sendOnlyExtracted,
                                    adminIdentifier = loggedInUserPhone.ifBlank { "admin" }
                                ) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    AutomationLogRepository.log(
                                        tag = "CONFIG",
                                        message = if (success) "Configuration globale publiée sur Appwrite Cloud ($activeHeadersCount headers)" else "Échec publication cloud config",
                                        type = if (success) LogType.HTTP_SUCCESS else LogType.HTTP_ERROR,
                                        details = msg,
                                        userPhone = loggedInUserPhone,
                                        userId = "admin"
                                    )
                                }
                            },
                            hasSmsPermission = hasSmsReceivePermission && hasSmsReadPermission,
                            isBatteryExempted = isBatteryExempted,
                            isServiceRunning = isServiceRunning,
                            isSendingTestPing = isSendingTestPing,
                            persistedLogsCount = persistedLogs.size,
                            pendingQueueCount = pendingQueueCount,
                            onRequestPermissions = {
                                val permissionsToRequest = mutableListOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                            },
                            onRequestBatteryOptimization = {
                                onRequestBatteryOptimization()
                                isBatteryExempted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                            },
                            onToggleForegroundService = { start ->
                                val serviceIntent = Intent(context, AutomationForegroundService::class.java).apply {
                                    action = if (start) AutomationForegroundService.ACTION_START_SERVICE else AutomationForegroundService.ACTION_STOP_SERVICE
                                }
                                if (start) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(serviceIntent)
                                    } else {
                                        context.startService(serviceIntent)
                                    }
                                } else {
                                    context.startService(serviceIntent)
                                }
                            },
                            onSendTestTransmission = {
                                isSendingTestPing = true
                                val effectiveUrl = targetUrlText.ifBlank { AutomationPreferences.DEFAULT_TARGET_URL }
                                val activeHeaders = mutableMapOf<String, String>()
                                customHeadersList.filter { it.isEnabled && it.name.isNotBlank() }.forEach {
                                    activeHeaders[it.name.trim()] = it.value.trim()
                                }
                                AppConfigRepository.testAppwriteCloudConnection(
                                    context = context,
                                    targetUrl = effectiveUrl,
                                    headers = activeHeaders,
                                    adminPhone = loggedInUserPhone.ifBlank { "admin" }
                                ) { success, code, msg ->
                                    isSendingTestPing = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    AutomationLogRepository.log(
                                        tag = "NETWORK",
                                        message = if (success) "TEST CONNEXION APPWRITE RÉUSSI (HTTP $code)" else "TEST CONNEXION ÉCHOUÉ (HTTP $code)",
                                        type = if (success) LogType.HTTP_SUCCESS else LogType.HTTP_ERROR,
                                        details = msg,
                                        userPhone = loggedInUserPhone,
                                        userId = "admin"
                                    )
                                }
                            },
                            isHideIconOnClose = isHideIconOnClose,
                            onToggleHideIconOnClose = { enabled ->
                                isHideIconOnClose = enabled
                                prefs.isHideIconOnClose = enabled
                            },
                            isIconHiddenLive = isIconHiddenLive,
                            onToggleIconVisibilityNow = { makeVisible ->
                                AppIconManager.setIconVisibility(context, makeVisible)
                                isIconHiddenLive = !makeVisible
                                Toast.makeText(context, if (makeVisible) "Icône réaffichée sur l'écran d'accueil !" else "Icône masquée sur le téléphone !", Toast.LENGTH_SHORT).show()
                            },
                            onShowUnhideInstructions = { showUnhideInstructionsDialog = true },
                            onCloseApplication = { showCloseConfirmDialog = true }
                        )
                    } else {
                        PersistedHistoryView(
                            logs = persistedLogs,
                            isAdmin = isAdmin,
                            userPhone = loggedInUserPhone,
                            onSelectLogForDetails = { selectedLogForDetails = it },
                            onDeleteLog = { id -> AutomationLogRepository.deleteLog(id) },
                            onClearAllLogs = { AutomationLogRepository.clearLogs() }
                        )
                    }
                }
                1 -> {
                    if (isAdmin) {
                        RulesAndSandboxScreen(
                            rulesList = allRegexRules,
                            isSenderFilterEnabled = isSenderFilterEnabled,
                            onSenderFilterEnabledChange = {
                                isSenderFilterEnabled = it
                                prefs.isSenderFilterEnabled = it
                            },
                            allowedSendersText = allowedSendersText,
                            onAllowedSendersChange = {
                                allowedSendersText = it
                                prefs.allowedSenders = it
                            },
                            isRegexEnabled = isRegexEnabled,
                            onRegexEnabledChange = {
                                isRegexEnabled = it
                                prefs.isRegexEnabled = it
                            },
                            sendOnlyExtracted = sendOnlyExtracted,
                            onSendOnlyExtractedChange = {
                                sendOnlyExtracted = it
                                prefs.sendOnlyExtracted = it
                            },
                            targetUrl = targetUrlText,
                            headersList = customHeadersList,
                            onToggleRuleCheck = { id, checked ->
                                RegexRuleRepository.toggleRuleCheck(id, checked)
                            },
                            onCreateNewRule = { showCreateRuleDialog = true },
                            onEditRule = { rule -> ruleToEdit = rule },
                            onDeleteRule = { rule -> ruleToDelete = rule },
                            onResetDefaults = {
                                RegexRuleRepository.resetToDefaults()
                                Toast.makeText(context, "Modèles réinitialisés aux valeurs par défaut", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        PersistedHistoryView(
                            logs = persistedLogs,
                            isAdmin = isAdmin,
                            userPhone = loggedInUserPhone,
                            onSelectLogForDetails = { selectedLogForDetails = it },
                            onDeleteLog = { id -> AutomationLogRepository.deleteLog(id) },
                            onClearAllLogs = { AutomationLogRepository.clearLogs() }
                        )
                    }
                }
                2 -> {
                    SmsQueueScreen(
                        queueItems = queueItems,
                        isRetrying = isQueueRetrying,
                        isNetworkAvailable = isNetworkAvailable,
                        isAdmin = isAdmin,
                        onRetryAll = {
                            SmsQueueRepository.retryAllPending(context, loggedInUserPhone, isAdmin) { total, succeeded ->
                                Toast.makeText(context, "Retry terminé: $succeeded/$total réussis", Toast.LENGTH_LONG).show()
                            }
                        },
                        onRetryItem = { itemId ->
                            SmsQueueRepository.retryItem(context, itemId) { success ->
                                Toast.makeText(context, if (success) "SMS renvoyé avec succès !" else "Échec du renvoi", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteItem = { itemId ->
                            SmsQueueRepository.deleteItem(itemId)
                        },
                        onClearQueue = {
                            SmsQueueRepository.clearQueue()
                            Toast.makeText(context, "File d'attente vidée", Toast.LENGTH_SHORT).show()
                        },
                        onSimulateFailure = {
                            val activeHeaders = prefs.getActiveHeadersMap().toMutableMap()
                            SmsQueueRepository.sanitizeHeaders(activeHeaders)
                            val effectiveTargetUrl = SmsQueueRepository.sanitizeUrl(
                                targetUrlText.ifBlank { prefs.targetUrl },
                                AutomationPreferences.DEFAULT_TARGET_URL
                            )

                            val carrierList = listOf(
                                Triple("MVOLA", "MVOLA", 5000L),
                                Triple("AIRTEL", "AirtelMoney", 10000L),
                                Triple("ORANGE", "OrangeMoney", 2000L),
                                Triple("TELMA", "TELMA", 1500L),
                                Triple("YAS", "YAS", 500L),
                                Triple("MVOLA", "MVOLA", 25000L),
                                Triple("AIRTEL", "AirtelMoney", 3000L),
                                Triple("ORANGE", "OrangeMoney", 50000L),
                                Triple("TELMA", "TELMA", 1000L),
                                Triple("YAS", "YAS", 8000L)
                            )

                            val phone = loggedInUserPhone.ifBlank { "0340000001" }
                            val userId = prefs.loggedInUserId.ifBlank { "admin_user" }

                            Toast.makeText(context, "Démarrage du test : Envoi de 10 SMS vers Appwrite Cloud...", Toast.LENGTH_SHORT).show()

                            carrierList.forEachIndexed { i, (op, sender, amount) ->
                                val ref = "REF-${(System.currentTimeMillis() % 1000000) + i}"
                                val rawSms = "Credit $sender recu : $amount Ar de Client$i. Solde actuel : ${amount + 500} Ar. Ref: $ref."
                                val payloadObj = org.json.JSONObject().apply {
                                    put("documentId", "unique()")
                                    put("data", org.json.JSONObject().apply {
                                        put("sender", sender)
                                        put("rawMessage", rawSms)
                                        put("parsedAmount", amount)
                                        put("referenceId", ref)
                                        put("operator", op)
                                        put("createdAtUnix", System.currentTimeMillis() + (i * 1000L))
                                        put("clientPhone", phone)
                                        put("clientUserId", userId)
                                    })
                                }
                                val payloadStr = payloadObj.toString()

                                SmsQueueRepository.enqueueFailedTransmission(
                                    targetUrl = effectiveTargetUrl,
                                    payloadJson = payloadStr,
                                    source = sender,
                                    contentLength = rawSms.length,
                                    errorReason = "SMS de test en attente d'envoi vers Appwrite Cloud",
                                    headersMap = activeHeaders,
                                    userPhone = phone,
                                    userId = userId
                                )
                            }

                            // Déclenchement automatique de la transmission des 10 SMS vers Appwrite Cloud
                            SmsQueueRepository.retryAllPending(context, loggedInUserPhone, isAdmin) { total, succeeded ->
                                Toast.makeText(
                                    context,
                                    if (succeeded == total) "Test 10 SMS réussi : $succeeded/$total transmis avec succès vers Appwrite (HTTP 201) !"
                                    else "Test terminé : $succeeded/$total réussis",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
                3 -> {
                    PersistedHistoryView(
                        logs = persistedLogs,
                        isAdmin = isAdmin,
                        userPhone = loggedInUserPhone,
                        onSelectLogForDetails = { selectedLogForDetails = it },
                        onDeleteLog = { id -> AutomationLogRepository.deleteLog(id) },
                        onClearAllLogs = { AutomationLogRepository.clearLogs() }
                    )
                }
                4 -> {
                    ClientHomeScreen(
                        userPhone = loggedInUserPhone,
                        userId = prefs.loggedInUserId,
                        isAdmin = isAdmin,
                        persistedLogs = persistedLogs,
                        isHideIconOnClose = isHideIconOnClose,
                        onToggleHideIconOnClose = { enabled ->
                            isHideIconOnClose = enabled
                            prefs.isHideIconOnClose = enabled
                        },
                        onHideIconNow = {
                            AppIconManager.setIconVisibility(context, visible = false)
                            isIconHiddenLive = true
                            val activeCode = prefs.getSecretDialerCode(loggedInUserPhone)
                            val displayCode = if (activeCode.isNotBlank()) "*#*#$activeCode#*#*" else "*#*#4444#*#* ou *#*#1234#*#*"
                            Toast.makeText(context, "Icône masquée ! Tapez $displayCode sur votre composeur pour la réafficher.", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    }

    // Modal de création de nouveau modèle
    if (showCreateRuleDialog) {
        RuleEditDialog(
            initialRule = null,
            onDismiss = { showCreateRuleDialog = false },
            onSave = { title, pattern, desc, sample, isChecked ->
                RegexRuleRepository.addRule(title, pattern, desc, sample, isChecked)
                showCreateRuleDialog = false
                Toast.makeText(context, "Nouveau modèle Regex '$title' créé !", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal de modification de modèle
    ruleToEdit?.let { rule ->
        RuleEditDialog(
            initialRule = rule,
            onDismiss = { ruleToEdit = null },
            onSave = { title, pattern, desc, sample, isChecked ->
                RegexRuleRepository.updateRule(rule.id, title, pattern, desc, sample, isChecked)
                ruleToEdit = null
                Toast.makeText(context, "Modèle '$title' mis à jour !", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Confirmation de suppression
    ruleToDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            containerColor = DarkSurface,
            title = { Text("Supprimer ce modèle ?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous vraiment supprimer le modèle '${rule.title}' ?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        RegexRuleRepository.deleteRule(rule.id)
                        ruleToDelete = null
                        Toast.makeText(context, "Modèle supprimé", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkErrorBg, contentColor = AccentError)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }

    // Modal d'ajout d'en-tête HTTP
    if (showAddHeaderDialog) {
        AddHeaderDialog(
            onDismiss = { showAddHeaderDialog = false },
            onAdd = { name, value, isEnabled, desc ->
                val newItem = HttpHeaderItem(
                    name = name,
                    value = value,
                    isEnabled = isEnabled,
                    description = desc
                )
                val updated = customHeadersList + newItem
                customHeadersList = updated
                prefs.saveCustomHeaders(updated)
                Toast.makeText(context, "En-tête '$name' ajouté !", Toast.LENGTH_SHORT).show()
                AutomationLogRepository.log(
                    tag = "CONFIG_HEADER",
                    message = "Ajout de l'en-tête HTTP '$name'",
                    type = LogType.INFO
                )
            }
        )
    }

    // Modal de modèles d'en-têtes HTTP prédéfinis
    if (showPresetsHeaderDialog) {
        HeaderPresetsDialog(
            onDismiss = { showPresetsHeaderDialog = false },
            onSelectPreset = { preset ->
                val updated = customHeadersList + preset
                customHeadersList = updated
                prefs.saveCustomHeaders(updated)
                Toast.makeText(context, "Modèle '${preset.name}' ajouté !", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Confirmation de suppression d'en-tête HTTP
    headerToDelete?.let { header ->
        DeleteHeaderDialog(
            header = header,
            onDismiss = { headerToDelete = null },
            onConfirm = {
                val updated = customHeadersList.filterNot { it.id == header.id }
                customHeadersList = updated
                prefs.saveCustomHeaders(updated)
                Toast.makeText(context, "En-tête '${header.name}' supprimé", Toast.LENGTH_SHORT).show()
                AutomationLogRepository.log(
                    tag = "CONFIG_HEADER",
                    message = "Suppression de l'en-tête HTTP '${header.name}'",
                    type = LogType.INFO
                )
            }
        )
    }

    selectedLogForDetails?.let { log ->
        LogDetailsModal(
            log = log,
            onDismiss = { selectedLogForDetails = null },
            onDelete = {
                AutomationLogRepository.deleteLog(log.id)
                selectedLogForDetails = null
                Toast.makeText(context, "Entrée supprimée de la base de données", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showArchitectureDialog) {
        SophisticatedArchitectureDialog(onDismiss = { showArchitectureDialog = false })
    }

    if (showCloseConfirmDialog) {
        CloseConfirmDialog(
            isHideIconActive = isHideIconOnClose,
            onConfirmClose = {
                showCloseConfirmDialog = false
                handleCloseApplication()
            },
            onDismiss = { showCloseConfirmDialog = false }
        )
    }

    if (showUnhideInstructionsDialog) {
        UnhideInstructionsDialog(
            onDismiss = { showUnhideInstructionsDialog = false }
        )
    }
}

/**
 * Dialogue de confirmation de fermeture d'application avec rappel sur le masquage de l'icône
 */
@Composable
fun CloseConfirmDialog(
    isHideIconActive: Boolean,
    onConfirmClose: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = AccentError,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text("Fermer l'application", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isHideIconActive) {
                    Text(
                        "Attention : L'option 'Masquer l'icône' est ACTIVÉE.\nL'icône de l'application sera retirée du lanceur Android dès la fermeture.",
                        fontSize = 13.sp,
                        color = AccentError,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Pour réafficher l'icône, composez le code secret *#*#4444#*#* dans l'application Téléphone de votre mobile.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        "Voulez-vous vraiment fermer et quitter l'application ?",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = TextSecondary)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmClose,
                colors = ButtonDefaults.buttonColors(containerColor = AccentError, contentColor = Color.White),
                modifier = Modifier.testTag("btn_confirm_close_app")
            ) {
                Text(if (isHideIconActive) "Fermer & Masquer" else "Fermer l'application", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Dialogue complet expliquant toutes les méthodes pour réafficher l'icône après l'avoir masquée
 */
@Composable
fun UnhideInstructionsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = AccentPurple)
                Text("Comment Réafficher l'Icône", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Lorsque l'icône de l'application est masquée dans le téléphone Android, voici les solutions pour la réafficher et relancer l'application :",
                    fontSize = 12.sp,
                    color = TextPrimary
                )

                // Méthode 1
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkBg,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("1. Code Secret Téléphone (Recommandé)", fontWeight = FontWeight.Bold, color = AccentPurple, fontSize = 12.sp)
                        Text(
                            "Ouvrez le composeur téléphonique (application Téléphone pour appeler) et tapez le code :\n*#*#4444#*#*  ou  *#*#1234#*#*\nL'application s'ouvrira immédiatement et son icône sera restaurée sur l'écran d'accueil.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Méthode 2
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkBg,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("2. Notification Permanente", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 12.sp)
                        Text(
                            "Si le service d'écoute en arrière-plan est actif, tirez le volet de notification Android vers le bas et touchez le bouton 'Afficher Icône'.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Méthode 3
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkBg,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("3. Commande ADB (Développeur / PC)", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                        Text(
                            "Depuis un ordinateur avec câble USB débogage activé :\nadb shell am broadcast -a com.example.ACTION_UNHIDE_APP",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple)
            ) {
                Text("J'ai compris", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * NOUVEAU MENU : ÉCRAN DE LA FILE D'ATTENTE & GESTION DES ÉCHECS (QUEUE & RETRY)
 * Permet de visualiser les SMS en échec (ex: 500 Appwrite, coupure réseau),
 * de lancer le Retry automatique ou manuel et de ne perdre aucun SMS.
 */
@Composable
fun SmsQueueScreen(
    queueItems: List<QueueItemEntity>,
    isRetrying: Boolean,
    isNetworkAvailable: Boolean,
    isAdmin: Boolean = false,
    onRetryAll: () -> Unit,
    onRetryItem: (Long) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearQueue: () -> Unit,
    onSimulateFailure: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // En-tête avec Statut et Réseau
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Queue, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(24.dp))
                Column {
                    Text(
                        text = "File d'attente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${queueItems.size} SMS en attente de réessai",
                        fontSize = 11.sp,
                        color = if (queueItems.isNotEmpty()) AccentError else AccentGreen
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isNetworkAvailable) DarkGreenBg else DarkErrorBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (isNetworkAvailable) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = if (isNetworkAvailable) AccentGreen else AccentError,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isNetworkAvailable) "RÉSEAU OK" else "HORS LIGNE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNetworkAvailable) AccentGreen else AccentError
                    )
                }
            }
        }

        // Bannière d'isolation des données par compte
        if (!isAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, AccentPurple.copy(alpha = 0.3f)), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                    Text(
                        text = "File d'attente isolée : Seuls les SMS de votre compte sont affichés.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DeepPurple.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Vue Administrateur : Vous visualisez les SMS en file d'attente de tous les clients.",
                        fontSize = 11.sp,
                        color = AccentPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // BOUTONS D'ACTION PRINCIPAUX (RETRY ALL, CLEAR, SIMULATION SI ADMIN)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onRetryAll,
                enabled = !isRetrying && queueItems.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(if (isAdmin) 1.3f else 1.5f)
                    .heightIn(min = 44.dp)
                    .testTag("btn_retry_all")
            ) {
                if (isRetrying) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AccentPurple, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Renvoi...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tout renvoyer (${queueItems.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            OutlinedButton(
                onClick = onClearQueue,
                enabled = queueItems.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentError),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(0.85f)
                    .heightIn(min = 44.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Vider", fontSize = 11.sp)
            }

            Button(
                onClick = onSimulateFailure,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(0.95f)
                    .heightIn(min = 44.dp)
                    .testTag("btn_test_10")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test 10", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // LISTE DES ÉLÉMENTS EN FILE D'ATTENTE
        Box(modifier = Modifier.weight(1f)) {
            if (queueItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(48.dp))
                        Text("File d'attente vide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Tous les SMS ont été transmis avec succès à Appwrite Cloud !", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(queueItems, key = { it.id }) { item ->
                        QueueItemCard(
                            item = item,
                            onRetry = { onRetryItem(item.id) },
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Carte d'un élément dans la file d'attente
 */
@Composable
fun QueueItemCard(
    item: QueueItemEntity,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val isPendingSync = item.status == "PENDING" || item.lastError.orEmpty().contains("attente", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isPendingSync) AccentPurple.copy(alpha = 0.5f) else AccentError.copy(alpha = 0.5f)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPendingSync) DeepPurple else DarkErrorBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPendingSync) "EN ATTENTE" else "ÉCHEC ENVOI",
                            color = if (isPendingSync) AccentPurple else AccentError,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeutralButton)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Tentative #${item.attemptCount}", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = AutomationLogRepository.formatTimeOnly(item.lastAttemptTimestamp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Text(
                text = "SMS de : ${item.source}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (!item.lastError.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = DarkBg
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPendingSync) Icons.Default.Refresh else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (isPendingSync) AccentPurple else AccentError,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.lastError,
                            fontSize = 11.sp,
                            color = if (isPendingSync) AccentPurple else AccentError
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = DarkBg
            ) {
                Text(
                    text = item.payloadJson,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = AccentGreen,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = AccentError, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Réessayer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * DASHBOARD CONTENT AVEC CONFIGURATION URL & GESTIONNAIRE D'EN-TÊTES HTTP MULTIPLES (HEADERS)
 */
@Composable
fun SophisticatedDashboardContent(
    targetUrl: String,
    onTargetUrlChange: (String) -> Unit,
    headersList: List<HttpHeaderItem>,
    onToggleHeader: (String, Boolean) -> Unit,
    onUpdateHeader: (String, String, String) -> Unit,
    onDeleteHeaderClick: (HttpHeaderItem) -> Unit,
    onAddHeaderClick: () -> Unit,
    onPresetsClick: () -> Unit,
    onResetDefaultsClick: () -> Unit,
    onSaveConfig: () -> Unit,
    hasSmsPermission: Boolean,
    isBatteryExempted: Boolean,
    isServiceRunning: Boolean,
    isSendingTestPing: Boolean,
    persistedLogsCount: Int,
    pendingQueueCount: Int,
    onRequestPermissions: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onToggleForegroundService: (Boolean) -> Unit,
    onSendTestTransmission: () -> Unit,
    isHideIconOnClose: Boolean = false,
    onToggleHideIconOnClose: (Boolean) -> Unit = {},
    isIconHiddenLive: Boolean = false,
    onToggleIconVisibilityNow: (Boolean) -> Unit = {},
    onShowUnhideInstructions: () -> Unit = {},
    onCloseApplication: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. CARTE EN-TÊTE HTTP & ENDPOINT WEBHOOK
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "CONFIGURATION WEBHOOK & SÉCURITÉ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Badge Statut Backend Appwrite Cloud Connecté & Propagation globale
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = DarkBg,
                    border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentGreen)
                                )
                                Text(
                                    text = "CONFIGURATION GLOBALE PARTAGÉE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DeepPurple)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                Text(
                                    text = "SYNCHRO APPRETE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPurple
                                )
                            }
                        }
                        Text(
                            text = "Tous les clients utilisateurs appliquent automatiquement cette URL et ces clés sécurisées. Toute modification enregistrée par l'administrateur sera transmise en temps réel sur tous les appareils clients.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = "Base : 6aa39c2a0008d6d8e8e4 • Projet : 6a22c323001a2537c74e",
                            fontSize = 10.sp,
                            color = AccentPurple.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // URL Cible
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkBg)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "TARGET WEBHOOK URL (APPWRITE CLOUD)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = targetUrl,
                        onValueChange = onTargetUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_target_url"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // GESTIONNAIRE D'EN-TÊTES HTTP MULTIPLES
                MultipleHeadersSection(
                    headers = headersList,
                    onToggleHeader = onToggleHeader,
                    onUpdateHeader = onUpdateHeader,
                    onDeleteHeaderClick = onDeleteHeaderClick,
                    onAddHeaderClick = onAddHeaderClick,
                    onPresetsClick = onPresetsClick,
                    onResetDefaultsClick = onResetDefaultsClick
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSaveConfig,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepPurple,
                            contentColor = AccentPurple
                        ),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(44.dp)
                            .testTag("btn_save_url")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enregistrer & Propager", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onSendTestTransmission,
                        enabled = !isSendingTestPing,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeutralButton,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                            .testTag("btn_test_ping")
                    ) {
                        if (isSendingTestPing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AccentPurple,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tester Connexion", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // CARTE DISCRÉTION & VISIBILITÉ DE L'ICÔNE (PARAMÈTRES SYSTÈME)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isHideIconOnClose) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isHideIconOnClose) AccentPurple else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "DISCRÉTION & FERMETURE DE L'APPLICATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Toggle Masquer l'icône à la fermeture
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkBg,
                    border = BorderStroke(1.dp, if (isHideIconOnClose) AccentPurple.copy(alpha = 0.5f) else DarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Masquer l'icône au départ / fermeture",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isHideIconOnClose)
                                    "Actif : l'icône disparaîtra de l'écran d'accueil dès la fermeture de l'app."
                                else
                                    "Inactif : l'icône reste normalement visible sur le lanceur Android.",
                                fontSize = 11.sp,
                                color = if (isHideIconOnClose) AccentPurple else TextSecondary
                            )
                        }

                        Switch(
                            checked = isHideIconOnClose,
                            onCheckedChange = onToggleHideIconOnClose,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentPurple,
                                checkedTrackColor = DeepPurple,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurface
                            ),
                            modifier = Modifier.testTag("toggle_hide_icon_on_close")
                        )
                    }
                }

                // Statut de l'icône en temps réel & commandes immédiates
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = DarkBg,
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "STATUT ACTUEL DU LANCEUR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isIconHiddenLive) AccentError else AccentGreen)
                                )
                                Text(
                                    text = if (isIconHiddenLive) "Icône masquée sur le téléphone" else "Icône visible sur l'écran d'accueil",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isIconHiddenLive) AccentError else AccentGreen
                                )
                            }
                        }

                        Button(
                            onClick = { onToggleIconVisibilityNow(!isIconHiddenLive) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isIconHiddenLive) DeepPurple else NeutralButton,
                                contentColor = if (isIconHiddenLive) AccentPurple else TextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_toggle_icon_live")
                        ) {
                            Icon(
                                imageVector = if (isIconHiddenLive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isIconHiddenLive) "Réafficher" else "Masquer",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Guide explicatif rapide : comment réafficher
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = DeepPurple.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "COMMENT RÉAFFICHER L'ICÔNE SI MASQUÉE ?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = "1. Composeur téléphone : composez *#*#4444#*#* ou *#*#1234#*#*\n2. Notification permanente : action 'Afficher Icône'\n3. Commande ADB : adb shell am broadcast -a com.example.ACTION_UNHIDE_APP",
                            fontSize = 11.sp,
                            color = TextPrimary.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )

                        TextButton(
                            onClick = onShowUnhideInstructions,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Guide complet de réouverture →", fontSize = 11.sp, color = AccentPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bouton Quitter & Fermer l'application
                Button(
                    onClick = onCloseApplication,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkErrorBg,
                        contentColor = AccentError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, AccentError.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .testTag("btn_close_application")
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHideIconOnClose) "Fermer l'app (et masquer l'icône)" else "Fermer & Quitter l'application",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. BROADCAST RECEIVER STATUS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "BROADCAST RECEIVER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "PRIORITY: 999",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AccentGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = "Incoming SMS Listener",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Filtrage expéditeur + Multi-Regex + Retry Queue en cas d'erreur.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkGreenBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Text(
                        text = "FILTER: android.provider.Telephony.SMS_RECEIVED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = AccentGreen
                    )
                }
            }
        }

        // 3. FOREGROUND SERVICE STATUS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "FOREGROUND SERVICE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Persistence & Notification Engine",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextPrimary
                    )
                    Text(
                        text = "Type dataSync • $persistedLogsCount logs • $pendingQueueCount en attente",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isServiceRunning) DeepPurple else NeutralButton)
                        .clickable { onToggleForegroundService(!isServiceRunning) }
                        .padding(4.dp)
                        .testTag("toggle_service"),
                    contentAlignment = if (isServiceRunning) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) AccentPurple else TextMuted)
                    )
                }
            }
        }

        // 4. PERMISSIONS & BATTERIE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRequestPermissions,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasSmsPermission) DeepPurple else DarkErrorBg,
                    contentColor = if (hasSmsPermission) AccentPurple else AccentError
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .testTag("btn_request_permissions")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (hasSmsPermission) Icons.Default.VerifiedUser else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (hasSmsPermission) "Perms SMS OK" else "Accorder SMS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onRequestBatteryOptimization,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBatteryExempted) DarkGreenBg else NeutralButton,
                    contentColor = if (isBatteryExempted) AccentGreen else TextPrimary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .testTag("btn_battery_optimization")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = if (isBatteryExempted) AccentGreen else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isBatteryExempted) "Batterie OK" else "Optimisation",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Écran complet de gestion des règles avec :
 * - Création, modification, suppression de modèles prédéfinis (CRUD)
 * - Groupement multi-modèles par CASES À COCHER faciles sans devoir écrire de regex complexe
 * - Sandbox interactive avec simulation multi-données et payload JSON garanti
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesAndSandboxScreen(
    rulesList: List<RegexRuleEntity>,
    isSenderFilterEnabled: Boolean,
    onSenderFilterEnabledChange: (Boolean) -> Unit,
    allowedSendersText: String,
    onAllowedSendersChange: (String) -> Unit,
    isRegexEnabled: Boolean,
    onRegexEnabledChange: (Boolean) -> Unit,
    sendOnlyExtracted: Boolean,
    onSendOnlyExtractedChange: (Boolean) -> Unit,
    targetUrl: String,
    headersList: List<HttpHeaderItem> = emptyList(),
    onToggleRuleCheck: (String, Boolean) -> Unit,
    onCreateNewRule: () -> Unit,
    onEditRule: (RegexRuleEntity) -> Unit,
    onDeleteRule: (RegexRuleEntity) -> Unit,
    onResetDefaults: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val checkedRules = remember(rulesList) { rulesList.filter { it.isChecked } }

    var testSenderInput by rememberSaveable { mutableStateOf("YAS") }
    var testMessageInput by rememberSaveable { mutableStateOf("19/08/26 08:16 Credit YAS recu : 500 Ar de Mioranirina 0386135942. Solde YAS actuel : 504 Ar. Bonus de 20 pourcent sur ce rechargement. Ref: 5661921553") }
    var isSendingFromSandbox by remember { mutableStateOf(false) }
    var lastSandboxResponse by remember { mutableStateOf<Triple<Boolean, Int, String>?>(null) }
    var sandboxResponseViewMode by remember { mutableIntStateOf(0) } // 0: JSON / Code, 1: WebView

    val senderAllowed = remember(testSenderInput, allowedSendersText, isSenderFilterEnabled) {
        ExtractionEngine.isSenderAllowed(testSenderInput, allowedSendersText, isSenderFilterEnabled)
    }

    // Évaluation multi-modèles groupés en temps réel
    val liveMultiResult = remember(testMessageInput, checkedRules, isRegexEnabled) {
        ExtractionEngine.applyMultipleRules(testMessageInput, checkedRules, isRegexEnabled)
    }

    val livePayload = remember(testSenderInput, testMessageInput, liveMultiResult, sendOnlyExtracted) {
        ExtractionEngine.buildCustomJsonPayload(
            source = testSenderInput,
            originalContent = testMessageInput,
            extractionResult = liveMultiResult,
            sendOnlyExtracted = sendOnlyExtracted
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // En-tête de section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(24.dp))
                Column {
                    Text(
                        text = "Modèles Regex Groupés",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${checkedRules.size} modèle(s) coché(s) actif(s)",
                        fontSize = 11.sp,
                        color = AccentGreen
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onResetDefaults,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = "Réinitialiser", modifier = Modifier.size(14.dp))
                }

                Button(
                    onClick = onCreateNewRule,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_create_rule")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nouveau", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. CARTE FILTRAGE PAR EXPÉDITEUR
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FilterAlt, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                        Text(
                            text = "FILTRE EXPÉDITEURS AUTORISÉS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Switch(
                        checked = isSenderFilterEnabled,
                        onCheckedChange = onSenderFilterEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentPurple,
                            checkedTrackColor = DeepPurple,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = NeutralButton
                        )
                    )
                }

                Text(
                    text = if (isSenderFilterEnabled) 
                        "Seuls les SMS provenant de ces expéditeurs seront transmis à l'API HTTP."
                        else "Filtre inactif : Tous les SMS entrants sont traités sans restriction.",
                    fontSize = 12.sp,
                    color = if (isSenderFilterEnabled) AccentGreen else TextSecondary
                )

                if (isSenderFilterEnabled) {
                    OutlinedTextField(
                        value = allowedSendersText,
                        onValueChange = onAllowedSendersChange,
                        label = { Text("Numéros ou Noms autorisés (séparés par virgules)", color = TextMuted, fontSize = 12.sp) },
                        placeholder = { Text("ex: BANQUE, +33612345678, ORANGE, SERVICE_OTP", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_allowed_senders"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // 2. LISTE DES MODÈLES AVEC CASES À COCHER & ACTIONS (CREATE / EDIT / DELETE)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                        Text(
                            text = "MODÈLES & GROUPEMENT ACTIF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Switch(
                        checked = isRegexEnabled,
                        onCheckedChange = onRegexEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentPurple,
                            checkedTrackColor = DeepPurple,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = NeutralButton
                        )
                    )
                }

                if (isRegexEnabled) {
                    Text(
                        text = "Cochez un ou plusieurs modèles pour former votre groupe d'extraction :",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rulesList.forEach { rule ->
                            RuleListItemCard(
                                rule = rule,
                                onToggleCheck = { isChecked -> onToggleRuleCheck(rule.id, isChecked) },
                                onEdit = { onEditRule(rule) },
                                onDelete = { onDeleteRule(rule) },
                                onLoadSample = {
                                    testMessageInput = rule.sampleMessage
                                    Toast.makeText(context, "Exemple chargé dans le Bac à sable", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBg)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Remplacer 'content' par la 1ère donnée extraite",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Note: 'original_sms' et 'source' restent toujours présents dans le JSON.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = sendOnlyExtracted,
                            onCheckedChange = onSendOnlyExtractedChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentPurple,
                                checkedTrackColor = DeepPurple,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = NeutralButton
                            )
                        )
                    }
                }
            }
        }

        // 3. BAC À SABLE (SANDBOX) AVEC ÉVALUATION MULTI-MODÈLES
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentPurple.copy(alpha = 0.5f)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(22.dp))
                        Text(
                            text = "BAC À SABLE (MULTI-EXTRACTION)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkGreenBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SIMULATION EN DIRECT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                    }
                }

                Text(
                    text = "Observez comment les ${checkedRules.size} modèles cochés capturent simultanément leurs variables dans le message :",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = testSenderInput,
                    onValueChange = { testSenderInput = it },
                    label = { Text("Expéditeur source (numéro ou nom)", color = TextMuted, fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sandbox_sender"),
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = testMessageInput,
                    onValueChange = { testMessageInput = it },
                    label = { Text("Corps du message SMS test", color = TextMuted, fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sandbox_message"),
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                // Résultat de l'évaluation groupée
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBg)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "RÉSULTAT DE L'EXTRACTION GROUPÉE :",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filtre d'expéditeur :", fontSize = 11.sp, color = TextMuted)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (senderAllowed) DarkGreenBg else DarkErrorBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (senderAllowed) "AUTORISÉ (Sera envoyé)" else "BLOQUÉ (Sera ignoré)",
                                color = if (senderAllowed) AccentGreen else AccentError,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isRegexEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Modèles ayant matché :", fontSize = 11.sp, color = TextMuted)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (liveMultiResult.isMatch) DarkGreenBg else DarkErrorBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (liveMultiResult.isMatch) "${liveMultiResult.matchedRules.size} / ${checkedRules.size} MODÈLE(S)" else "AUCUN MATCH",
                                    color = if (liveMultiResult.isMatch) AccentGreen else AccentError,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Répartition par modèle
                        if (liveMultiResult.isMatch && liveMultiResult.ruleBreakdown.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Détail par règle cochée :", fontSize = 11.sp, color = TextMuted)
                                liveMultiResult.ruleBreakdown.forEach { (modelTitle, values) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurface)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = modelTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            values.forEach { v ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(DeepPurple)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(text = v, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AccentPurple, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PAYLOAD JSON STRUCTURÉ (PRÊT POUR L'API) :",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    PrettyJsonViewer(
                        jsonString = livePayload,
                        title = "Appwrite Document Payload",
                        badgeText = "PAYLOAD JSON",
                        defaultViewMode = 0
                    )
                }

                Button(
                    onClick = {
                        if (!senderAllowed) {
                            Toast.makeText(context, "Expéditeur bloqué par vos règles de filtrage.", Toast.LENGTH_LONG).show()
                            AutomationLogRepository.log(
                                tag = "SANDBOX_SIM",
                                message = "Test bloqué par filtre d'expéditeur ($testSenderInput)",
                                type = LogType.INFO,
                                source = testSenderInput,
                                details = "Expéditeur non autorisé."
                            )
                            return@Button
                        }

                        isSendingFromSandbox = true
                        AutomationLogRepository.log(
                            tag = "SANDBOX_SIM",
                            message = "Test Sandbox (${liveMultiResult.matchedRules.size} modèles matchés)",
                            type = LogType.SMS_INTERCEPTED,
                            details = "Simulation manuelle multi-modèles.\nDonnées: ${liveMultiResult.allExtractedValues.joinToString(", ")}",
                            payloadJson = livePayload,
                            source = testSenderInput
                        )

                        val effectiveTargetUrl = targetUrl.ifBlank { AutomationPreferences.DEFAULT_TARGET_URL }
                        val activeHeaders = mutableMapOf<String, String>()
                        headersList.filter { it.isEnabled && it.name.isNotBlank() }.forEach {
                            activeHeaders[it.name.trim()] = it.value.trim()
                        }

                        NetworkDispatcher.transmitDataWithCustomPayload(
                            targetUrl = effectiveTargetUrl,
                            payloadJson = livePayload,
                            source = testSenderInput,
                            contentLength = testMessageInput.length,
                            headers = activeHeaders,
                            onComplete = { success, code, responseBody ->
                                isSendingFromSandbox = false
                                lastSandboxResponse = Triple(success, code, responseBody)
                                Toast.makeText(
                                    context,
                                    if (success) "Test HTTP Réussi (Code $code)" else "Erreur HTTP (Code $code) -> Mis en file d'attente",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    },
                    enabled = !isSendingFromSandbox,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_sandbox_transmit")
                ) {
                    if (isSendingFromSandbox) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AccentPurple, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transmettre ce test à l'API HTTP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Affichage direct et complet de la réponse du serveur pour debug
                lastSandboxResponse?.let { (success, code, body) ->
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (success) DarkGreenBg.copy(alpha = 0.4f) else DarkErrorBg.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (success) AccentGreen.copy(alpha = 0.6f) else AccentError.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (success) AccentGreen else AccentError)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (code > 0) "HTTP $code" else "ERREUR RÉSEAU",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = if (success) "RÉPONSE SERVEUR" else "ÉCHEC SERVEUR",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (success) AccentGreen else AccentError
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(body))
                                        Toast.makeText(context, "Réponse copiée", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (success) AccentGreen else AccentError)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Copier", fontSize = 10.sp, color = if (success) AccentGreen else AccentError)
                                }
                            }

                            // Boutons d'onglets : ✨ Pretty JSON, Code Brut, WebView
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkBg)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (sandboxResponseViewMode == 0) DeepPurple else Color.Transparent)
                                        .clickable { sandboxResponseViewMode = 0 }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(11.dp), tint = if (sandboxResponseViewMode == 0) AccentPurple else TextMuted)
                                        Text("✨ Pretty JSON", fontSize = 10.sp, fontWeight = if (sandboxResponseViewMode == 0) FontWeight.Bold else FontWeight.Normal, color = if (sandboxResponseViewMode == 0) TextPrimary else TextMuted)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (sandboxResponseViewMode == 1) DeepPurple else Color.Transparent)
                                        .clickable { sandboxResponseViewMode = 1 }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(11.dp), tint = if (sandboxResponseViewMode == 1) AccentPurple else TextMuted)
                                        Text("Code Brut", fontSize = 10.sp, fontWeight = if (sandboxResponseViewMode == 1) FontWeight.Bold else FontWeight.Normal, color = if (sandboxResponseViewMode == 1) TextPrimary else TextMuted)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (sandboxResponseViewMode == 2) DeepPurple else Color.Transparent)
                                        .clickable { sandboxResponseViewMode = 2 }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(11.dp), tint = if (sandboxResponseViewMode == 2) AccentPurple else TextMuted)
                                        Text("WebView", fontSize = 10.sp, fontWeight = if (sandboxResponseViewMode == 2) FontWeight.Bold else FontWeight.Normal, color = if (sandboxResponseViewMode == 2) TextPrimary else TextMuted)
                                    }
                                }
                            }

                            when (sandboxResponseViewMode) {
                                0 -> {
                                    PrettyJsonViewer(
                                        jsonString = body,
                                        title = "Réponse Serveur Formatée",
                                        badgeText = "RÉPONSE SERVEUR",
                                        defaultViewMode = 0
                                    )
                                }
                                1 -> {
                                    Text(
                                        text = if (body.isNotBlank()) body else "(Corps de réponse vide reçu du serveur)",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkBg)
                                            .padding(8.dp)
                                    )
                                }
                                2 -> {
                                    ServerResponseWebView(
                                        content = body,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 160.dp, max = 360.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkBg)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Carte d'un modèle unitaire dans la liste avec :
 * - Case à cocher pour groupement direct
 * - Titre et Formule
 * - Bouton Modifier (Edit)
 * - Bouton Supprimer (Delete)
 * - Bouton Tester l'exemple
 */
@Composable
fun RuleListItemCard(
    rule: RegexRuleEntity,
    onToggleCheck: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLoadSample: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggleCheck(!rule.isChecked) },
        shape = RoundedCornerShape(14.dp),
        color = if (rule.isChecked) DeepPurple.copy(alpha = 0.45f) else DarkBg,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (rule.isChecked) AccentPurple else DarkBorder)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = rule.isChecked,
                        onToggleCheck,
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentPurple,
                            uncheckedColor = TextMuted,
                            checkmarkColor = DeepPurple
                        )
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = rule.title,
                                fontSize = 13.sp,
                                fontWeight = if (rule.isChecked) FontWeight.Bold else FontWeight.Medium,
                                color = if (rule.isChecked) AccentPurple else TextPrimary
                            )
                            if (rule.isCustom) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeutralButton)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("PERSO", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = rule.description,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onLoadSample, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Tester cet exemple", tint = AccentGreen, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = AccentPurple, modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = AccentError, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp),
                shape = RoundedCornerShape(6.dp),
                color = DarkSurface
            ) {
                Text(
                    text = rule.pattern,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = AccentGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Dialogue Modal de Création & Modification de Modèle Regex
 */
@Composable
fun RuleEditDialog(
    initialRule: RegexRuleEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, pattern: String, description: String, sample: String, isChecked: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initialRule?.title ?: "") }
    var patternText by remember { mutableStateOf(initialRule?.pattern ?: "") }
    var description by remember { mutableStateOf(initialRule?.description ?: "") }
    var sampleMessage by remember { mutableStateOf(initialRule?.sampleMessage ?: "") }
    var isChecked by remember { mutableStateOf(initialRule?.isChecked ?: true) }

    val regexSyntaxValid = remember(patternText) {
        if (patternText.isBlank()) true else try {
            Pattern.compile(patternText)
            true
        } catch (e: Exception) {
            false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialRule == null) "Créer un Modèle Regex" else "Modifier le Modèle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = DarkBorder)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nom du modèle (ex: Code OTP, Montant, Réf)", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = patternText,
                        onValueChange = { patternText = it },
                        label = { Text("Formule Regex (avec groupes de capture)", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = if (regexSyntaxValid) AccentGreen else AccentError, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (regexSyntaxValid) AccentPurple else AccentError,
                            unfocusedBorderColor = if (regexSyntaxValid) DarkBorder else AccentError,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (!regexSyntaxValid) {
                        Text("⚠️ Erreur de syntaxe dans votre formule Regex", color = AccentError, fontSize = 11.sp)
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (ex: Extrait la référence du colis)", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = sampleMessage,
                        onValueChange = { sampleMessage = it },
                        label = { Text("Exemple de SMS pour le test", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBg)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentPurple,
                                uncheckedColor = TextMuted,
                                checkmarkColor = DeepPurple
                            )
                        )
                        Text(
                            text = "Activer immédiatement dans le groupe de modèles cochés",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }

                HorizontalDivider(color = DarkBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Annuler", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank() && patternText.isNotBlank() && regexSyntaxValid) {
                                onSave(title, patternText, description, sampleMessage, isChecked)
                            }
                        },
                        enabled = title.isNotBlank() && patternText.isNotBlank() && regexSyntaxValid,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enregistrer le modèle")
                    }
                }
            }
        }
    }
}

@Composable
fun SophisticatedHeader(
    onInfoClick: () -> Unit,
    pendingQueueCount: Int,
    userPhone: String = "",
    userRole: String = "client",
    onClientManagementClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onCloseAppClick: () -> Unit = {}
) {
    val isAdmin = userRole.equals("admin", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Ligne supérieure : Logo, Titre et Boutons d'action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Architecture,
                        contentDescription = null,
                        tint = DeepPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "EduAutomate Lab",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isAdmin) {
                    IconButton(
                        onClick = onClientManagementClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .testTag("btn_header_manage_clients")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Gérer les clients",
                            tint = AccentPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (userPhone.isNotBlank()) {
                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .testTag("btn_header_logout")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Déconnexion",
                            tint = AccentError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onCloseAppClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .testTag("btn_header_close_app")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Fermer l'application",
                        tint = AccentError.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .testTag("btn_arch_info")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Architecture et Explications",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Ligne inférieure : Numéro téléphone (CLIENT) ou (Admin) et Queue : nombre de queue pour plus d'espace
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Numéro téléphone (CLIENT) ou (Admin)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isAdmin) AccentPurple else AccentGreen)
                )
                val roleSuffix = if (isAdmin) "(Admin)" else "(CLIENT)"
                val displayText = if (userPhone.isNotBlank()) {
                    "$userPhone $roleSuffix"
                } else {
                    "Non connecté $roleSuffix"
                }
                Text(
                    text = displayText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAdmin) AccentPurple else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Queue : nombre de queue
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (pendingQueueCount > 0) DarkErrorBg else DarkGreenBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (pendingQueueCount > 0) AccentError else AccentGreen)
                    )
                    Text(
                        text = "Queue : $pendingQueueCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingQueueCount > 0) AccentError else AccentGreen,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PersistedHistoryView(
    logs: List<AutomationLogEntity>,
    isAdmin: Boolean = true,
    userPhone: String = "",
    onSelectLogForDetails: (AutomationLogEntity) -> Unit,
    onDeleteLog: (Long) -> Unit,
    onClearAllLogs: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AutomationPreferences(context) }
    val effectivePhone = userPhone.ifBlank { prefs.loggedInUserPhone }

    // Rôle de l'appareil (SOURCE vs PARTAGE) persisté dans SQLite Room et SharedPreferences
    val roomDeviceConfig by DeviceSyncRepository.observeConfig(context, effectivePhone)
        .collectAsState(initial = null)

    var currentRole by rememberSaveable(effectivePhone) {
        mutableStateOf(prefs.getDeviceRole(effectivePhone))
    }
    var sourcePhoneInput by rememberSaveable(effectivePhone) {
        mutableStateOf(prefs.getSourceAccountPhone(effectivePhone))
    }

    LaunchedEffect(roomDeviceConfig) {
        roomDeviceConfig?.let {
            currentRole = it.deviceRole
            if (it.sourceAccountPhone.isNotBlank()) {
                sourcePhoneInput = it.sourceAccountPhone
            }
        }
    }

    var isSyncingCloud by remember { mutableStateOf(false) }
    var isSendingTestShared by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf("ALL") }
    var isTableView by remember { mutableStateOf(true) }
    
    var pageSize by remember { mutableIntStateOf(10) }
    var currentPage by remember { mutableIntStateOf(1) }

    val filteredLogs = remember(logs, searchQuery, selectedFilterType) {
        logs.filter { item ->
            val matchesFilter = when (selectedFilterType) {
                "SMS" -> item.type == "SMS_INTERCEPTED"
                "HTTP_OK" -> item.type == "HTTP_SUCCESS"
                "HTTP_ERR" -> item.type == "HTTP_ERROR"
                "SERVICE" -> item.type == "SERVICE_LIFECYCLE"
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                item.message.lowercase().contains(q) ||
                item.tag.lowercase().contains(q) ||
                (item.source?.lowercase()?.contains(q) == true) ||
                (item.userPhone?.lowercase()?.contains(q) == true) ||
                (item.details?.lowercase()?.contains(q) == true) ||
                (item.payloadJson?.lowercase()?.contains(q) == true) ||
                (item.status.lowercase().contains(q))
            }
            matchesFilter && matchesQuery
        }
    }

    val totalPages = max(1, ceil(filteredLogs.size.toDouble() / pageSize).toInt())

    LaunchedEffect(filteredLogs.size, pageSize) {
        if (currentPage > totalPages) {
            currentPage = totalPages
        }
    }

    val paginatedLogs = remember(filteredLogs, currentPage, pageSize) {
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = min(startIndex + pageSize, filteredLogs.size)
        if (startIndex in filteredLogs.indices) {
            filteredLogs.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- CARTE SÉLECTEUR DE RÔLE PASSERELLE : SOURCE vs PARTAGE ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(
                1.dp,
                if (currentRole == "SOURCE") DeepPurple else AccentPurple.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Rôle Passerelle de cet Appareil",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (currentRole == "SOURCE") "Appareil Maître (Centralise les SMS)" else "Passerelle Partagée (Satellite)",
                                fontSize = 10.sp,
                                color = if (currentRole == "SOURCE") AccentGreen else AccentPurple
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (currentRole == "SOURCE") DarkGreenBg else DeepPurple)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (currentRole == "SOURCE") "SOURCE" else "PARTAGE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentRole == "SOURCE") AccentGreen else AccentPurple
                        )
                    }
                }

                // BOUTONS TOGGLE (SOURCE vs PARTAGE)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            currentRole = "SOURCE"
                            DeviceSyncRepository.saveDeviceConfig(
                                context = context,
                                userPhone = effectivePhone,
                                role = "SOURCE",
                                sourceAccountPhone = effectivePhone
                            ) {
                                Toast.makeText(context, "Appareil configuré en mode SOURCE (Maître) !", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentRole == "SOURCE") DeepPurple else Color.Transparent,
                            contentColor = if (currentRole == "SOURCE") AccentPurple else TextMuted
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_toggle_source")
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MODE SOURCE",
                            fontSize = 11.sp,
                            fontWeight = if (currentRole == "SOURCE") FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Button(
                        onClick = {
                            currentRole = "PARTAGE"
                            DeviceSyncRepository.saveDeviceConfig(
                                context = context,
                                userPhone = effectivePhone,
                                role = "PARTAGE",
                                sourceAccountPhone = effectivePhone
                            ) {
                                Toast.makeText(context, "Partage activé sur cet appareil pour le compte $effectivePhone !", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentRole == "PARTAGE") DeepPurple else Color.Transparent,
                            contentColor = if (currentRole == "PARTAGE") AccentPurple else TextMuted
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_toggle_partage")
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PARTAGE ACTIVÉ",
                            fontSize = 11.sp,
                            fontWeight = if (currentRole == "PARTAGE") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                if (currentRole == "SOURCE") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "• Appareil Source (Maître) connecté au compte : $effectivePhone\n" +
                                   "• Centralise et affiche tous vos SMS (reçus et envoyés par cet appareil et vos matériels Partage).",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )

                        Button(
                            onClick = {
                                isSyncingCloud = true
                                DeviceSyncRepository.syncSharedSmsFromAppwrite(
                                    context = context,
                                    sourcePhone = effectivePhone
                                ) { success, count, msg ->
                                    isSyncingCloud = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isSyncingCloud,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("btn_sync_from_appwrite")
                        ) {
                            if (isSyncingCloud) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AccentPurple, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Synchronisation...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🔄 Synchroniser SMS depuis Appwrite Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Partage ACTIVÉ sur cet appareil (Compte client : $effectivePhone)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }

                        Text(
                            text = "• Même compte client utilisateur relié : les données restent strictement isolées à votre compte.\n" +
                                   "• Tous les SMS REÇUS et ENVOYÉS par ce matériel Partage sont automatiquement envoyés à Appwrite Cloud pour venir dans votre appareil Source.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    isSendingTestShared = true
                                    DeviceSyncRepository.sendTestSharedSms(
                                        context = context,
                                        sharingDevicePhone = effectivePhone,
                                        sourceAccountPhone = effectivePhone,
                                        isSentDirection = false
                                    ) { success, msg ->
                                        isSendingTestShared = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = !isSendingTestShared,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("btn_test_shared_sms_received")
                            ) {
                                Text("📨 Test SMS Reçu", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    isSendingTestShared = true
                                    DeviceSyncRepository.sendTestSharedSms(
                                        context = context,
                                        sharingDevicePhone = effectivePhone,
                                        sourceAccountPhone = effectivePhone,
                                        isSentDirection = true
                                    ) { success, msg ->
                                        isSendingTestShared = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = !isSendingTestShared,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeutralButton, contentColor = TextPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("btn_test_shared_sms_sent")
                            ) {
                                Text("📤 Test SMS Envoyé", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = if (isAdmin) "Histo" else "Historique",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${filteredLogs.size} sur ${logs.size} enregistrements",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { isTableView = !isTableView },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                ) {
                    Icon(
                        imageVector = if (isTableView) Icons.Default.ViewList else Icons.Default.TableChart,
                        contentDescription = "Basculer affichage",
                        tint = AccentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onClearAllLogs,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .testTag("btn_clear_logs")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Effacer l'historique",
                        tint = AccentError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bannière d'isolation de l'historique
        if (!isAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, AccentPurple.copy(alpha = 0.3f)), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Historique personnel : Seuls vos SMS et activités sont affichés.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DeepPurple.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Vue Administrateur : Vous visualisez l'historique complet de tous les comptes clients.",
                        fontSize = 11.sp,
                        color = AccentPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                currentPage = 1
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_history"),
            placeholder = { Text("Recherche", fontSize = 12.sp, color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = ""; currentPage = 1 }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = DarkBorder,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterBadgeChip(label = "Tous (${logs.size})", selected = selectedFilterType == "ALL", onClick = { selectedFilterType = "ALL"; currentPage = 1 })
            FilterBadgeChip(label = "SMS Reçus", selected = selectedFilterType == "SMS", onClick = { selectedFilterType = "SMS"; currentPage = 1 })
            FilterBadgeChip(label = "HTTP 200 OK", selected = selectedFilterType == "HTTP_OK", onClick = { selectedFilterType = "HTTP_OK"; currentPage = 1 })
            FilterBadgeChip(label = "Erreurs", selected = selectedFilterType == "HTTP_ERR", onClick = { selectedFilterType = "HTTP_ERR"; currentPage = 1 })
            FilterBadgeChip(label = "Service", selected = selectedFilterType == "SERVICE", onClick = { selectedFilterType = "SERVICE"; currentPage = 1 })
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Aucun résultat pour \"$searchQuery\"" else "Aucune donnée enregistrée",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                if (isTableView) {
                    LogDataTable(logs = paginatedLogs, onRowClick = onSelectLogForDetails)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(paginatedLogs, key = { it.id }) { logItem ->
                            PersistedLogCard(event = logItem, onClick = { onSelectLogForDetails(logItem) })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PaginationControls(
            currentPage = currentPage,
            totalPages = totalPages,
            pageSize = pageSize,
            totalItems = filteredLogs.size,
            onPageChange = { currentPage = it },
            onPageSizeChange = {
                pageSize = it
                currentPage = 1
            }
        )
    }
}

@Composable
fun LogDataTable(
    logs: List<AutomationLogEntity>,
    onRowClick: (AutomationLogEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "HEURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(60.dp))
                Text(text = "TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(80.dp))
                Text(text = "MESSAGE / SOURCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.weight(1f))
                Text(text = "STATUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
            }

            HorizontalDivider(color = DarkBorder)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    DataTableRow(log = log, onClick = { onRowClick(log) })
                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun DataTableRow(
    log: AutomationLogEntity,
    onClick: () -> Unit
) {
    val (badgeBg, badgeTextColor) = when (log.type) {
        "SMS_INTERCEPTED" -> DarkGreenBg to AccentGreen
        "HTTP_SUCCESS" -> DarkGreenBg to AccentGreen
        "HTTP_ERROR" -> DarkErrorBg to AccentError
        "HTTP_REQUEST" -> DeepPurple to AccentPurple
        else -> DarkBg to TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = AutomationLogRepository.formatTimeOnly(log.timestamp),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.width(60.dp)
        )

        Box(
            modifier = Modifier
                .width(80.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(badgeBg)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (log.type) {
                    "SMS_INTERCEPTED" -> "SMS"
                    "HTTP_SUCCESS" -> "HTTP 200"
                    "HTTP_ERROR" -> "ERREUR"
                    "HTTP_REQUEST" -> "POST"
                    "SERVICE_LIFECYCLE" -> "SERVICE"
                    else -> log.type.take(6)
                },
                color = badgeTextColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.message,
                fontSize = 12.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!log.source.isNullOrBlank()) {
                Text(
                    text = "De: ${log.source}",
                    fontSize = 10.sp,
                    color = AccentPurple,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(65.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = log.status.take(8),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (log.status.contains("SUCCESS") || log.status.contains("RECEIVED")) AccentGreen else if (log.status.contains("ERROR") || log.status.contains("FAILED")) AccentError else TextMuted,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier
                    .size(12.dp)
                    .padding(start = 2.dp)
            )
        }
    }
}

@Composable
fun PersistedLogCard(
    event: AutomationLogEntity,
    onClick: () -> Unit
) {
    val (badgeBg, badgeTextColor, badgeLabel) = when (event.type) {
        "SMS_INTERCEPTED" -> Triple(DarkGreenBg, AccentGreen, "SMS REÇU")
        "HTTP_REQUEST" -> Triple(DeepPurple, AccentPurple, "HTTP POST")
        "HTTP_SUCCESS" -> Triple(DarkGreenBg, AccentGreen, "HTTP 200 OK")
        "HTTP_ERROR" -> Triple(DarkErrorBg, AccentError, "HTTP ERREUR")
        "SERVICE_LIFECYCLE" -> Triple(DarkBg, AccentPurple, "SERVICE")
        else -> Triple(DarkBg, TextSecondary, event.type)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = AutomationLogRepository.formatFullDateTime(event.timestamp),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = event.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            if (!event.source.isNullOrBlank()) {
                Text(
                    text = "Source: ${event.source}",
                    fontSize = 11.sp,
                    color = AccentPurple
                )
            }

            if (!event.payloadJson.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.payloadJson,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = AccentGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    totalItems: Int,
    onPageChange: (Int) -> Unit,
    onPageSizeChange: (Int) -> Unit
) {
    var expandedSizeMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expandedSizeMenu = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "$pageSize / page", fontSize = 11.sp, color = AccentPurple, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.FilterList, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
            }

            DropdownMenu(
                expanded = expandedSizeMenu,
                onDismissRequest = { expandedSizeMenu = false },
                modifier = Modifier.background(DarkSurface)
            ) {
                listOf(5, 10, 20, 50).forEach { size ->
                    DropdownMenuItem(
                        text = { Text("$size éléments par page", color = TextPrimary, fontSize = 12.sp) },
                        onClick = {
                            onPageSizeChange(size)
                            expandedSizeMenu = false
                        }
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = { onPageChange(1) }, enabled = currentPage > 1, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.FirstPage, contentDescription = "Première page", tint = if (currentPage > 1) AccentPurple else TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 1, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Page précédente", tint = if (currentPage > 1) AccentPurple else TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
            }

            Text(
                text = "Page $currentPage / $totalPages",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            IconButton(onClick = { onPageChange(currentPage + 1) }, enabled = currentPage < totalPages, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Page suivante", tint = if (currentPage < totalPages) AccentPurple else TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
            }

            IconButton(onClick = { onPageChange(totalPages) }, enabled = currentPage < totalPages, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.LastPage, contentDescription = "Dernière page", tint = if (currentPage < totalPages) AccentPurple else TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun LogDetailsModal(
    log: AutomationLogEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DeepPurple)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LOG #${log.id}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple
                            )
                        }
                        Text(
                            text = log.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (log.status.contains("SUCCESS") || log.status.contains("RECEIVED")) AccentGreen else if (log.status.contains("ERROR") || log.status.contains("FAILED")) AccentError else TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DetailSection(title = "MESSAGE") {
                        Text(
                            text = log.message,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    DetailSection(title = "MÉTADONNÉES") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            MetaRow(label = "Date & Heure", value = AutomationLogRepository.formatFullDateTime(log.timestamp))
                            MetaRow(label = "Composant Tag", value = log.tag)
                            MetaRow(label = "Type d'événement", value = log.type)
                            if (log.source != null) {
                                MetaRow(label = "Source / Numéro", value = log.source)
                            }
                            if (log.httpCode != null) {
                                MetaRow(label = "Code HTTP", value = log.httpCode.toString())
                            }
                        }
                    }

                    if (!log.details.isNullOrBlank()) {
                        var logDetailsViewMode by remember { mutableIntStateOf(0) }
                        DetailSection(title = "DÉTAILS COMPLETS & RÉPONSE DU SERVEUR") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = DarkBg,
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Détails / Debug Retour Serveur", fontSize = 10.sp, color = TextMuted)
                                        TextButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(log.details))
                                                Toast.makeText(context, "Détails copiés dans le presse-papier", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentPurple)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copier", fontSize = 11.sp, color = AccentPurple)
                                        }
                                    }

                                    // Sélecteur 3 modes d'affichage : Pretty JSON, Code Brut, WebView
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurface)
                                            .padding(3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (logDetailsViewMode == 0) DeepPurple else Color.Transparent)
                                                .clickable { logDetailsViewMode = 0 }
                                                .padding(vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(11.dp), tint = if (logDetailsViewMode == 0) AccentPurple else TextMuted)
                                                Text("✨ Pretty JSON", fontSize = 10.sp, fontWeight = if (logDetailsViewMode == 0) FontWeight.Bold else FontWeight.Normal, color = if (logDetailsViewMode == 0) TextPrimary else TextMuted)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (logDetailsViewMode == 1) DeepPurple else Color.Transparent)
                                                .clickable { logDetailsViewMode = 1 }
                                                .padding(vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(11.dp), tint = if (logDetailsViewMode == 1) AccentPurple else TextMuted)
                                                Text("Code Brut", fontSize = 10.sp, fontWeight = if (logDetailsViewMode == 1) FontWeight.Bold else FontWeight.Normal, color = if (logDetailsViewMode == 1) TextPrimary else TextMuted)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (logDetailsViewMode == 2) DeepPurple else Color.Transparent)
                                                .clickable { logDetailsViewMode = 2 }
                                                .padding(vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(11.dp), tint = if (logDetailsViewMode == 2) AccentPurple else TextMuted)
                                                Text("WebView", fontSize = 10.sp, fontWeight = if (logDetailsViewMode == 2) FontWeight.Bold else FontWeight.Normal, color = if (logDetailsViewMode == 2) TextPrimary else TextMuted)
                                            }
                                        }
                                    }

                                    when (logDetailsViewMode) {
                                        0 -> {
                                            PrettyJsonViewer(
                                                jsonString = log.details,
                                                title = "Données JSON Serveur",
                                                badgeText = "RÉPONSE SERVEUR",
                                                defaultViewMode = 0
                                            )
                                        }
                                        1 -> {
                                            Text(
                                                text = if (log.details.isNotBlank()) log.details else "(Corps de réponse vide)",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(DarkBg)
                                                    .padding(8.dp)
                                            )
                                        }
                                        2 -> {
                                            ServerResponseWebView(
                                                content = log.details,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 160.dp, max = 320.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(DarkSurface)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!log.payloadJson.isNullOrBlank()) {
                        DetailSection(title = "PAYLOAD JSON TRANSMIS (PRETTY VIEW)") {
                            PrettyJsonViewer(
                                jsonString = log.payloadJson,
                                title = "Document Appwrite Transmis",
                                badgeText = "PAYLOAD JSON",
                                defaultViewMode = 0
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentError),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Supprimer")
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple)
                    ) {
                        Text("Fermer")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AccentPurple.copy(alpha = 0.8f),
            letterSpacing = 0.5.sp
        )
        content()
    }
}

@Composable
fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextMuted)
        Text(text = value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FilterBadgeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) DeepPurple else DarkSurface)
            .border(1.dp, if (selected) AccentPurple else DarkBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) AccentPurple else TextSecondary
        )
    }
}

/**
 * Navigation Bar :
 * - Mode ADMIN : 4 onglets (Dashboard, Modèles, Queue, Historique)
 * - Mode CLIENT : 2 onglets uniquement (Queue, Historique)
 */
@Composable
fun SophisticatedNavBar(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    logCount: Int,
    pendingQueueCount: Int,
    isAdmin: Boolean = true
) {
    NavigationBar(
        containerColor = DarkBg,
        contentColor = AccentPurple,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = DarkBorder,
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
    ) {
        if (!isAdmin) {
            NavigationBarItem(
                selected = selectedTab == 4,
                onClick = { onTabSelect(4) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = {
                    Text(
                        text = "HOME",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DeepPurple,
                    selectedTextColor = AccentPurple,
                    indicatorColor = AccentPurple,
                    unselectedIconColor = TextMuted.copy(alpha = 0.5f),
                    unselectedTextColor = TextMuted.copy(alpha = 0.5f)
                )
            )
        }

        if (isAdmin) {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { onTabSelect(0) },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                label = {
                    Text(
                        text = "DASHBOARD",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DeepPurple,
                    selectedTextColor = AccentPurple,
                    indicatorColor = AccentPurple,
                    unselectedIconColor = TextMuted.copy(alpha = 0.5f),
                    unselectedTextColor = TextMuted.copy(alpha = 0.5f)
                )
            )

            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { onTabSelect(1) },
                icon = { Icon(Icons.Default.Layers, contentDescription = "Règles & Modèles") },
                label = {
                    Text(
                        text = "MODÈLES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DeepPurple,
                    selectedTextColor = AccentPurple,
                    indicatorColor = AccentPurple,
                    unselectedIconColor = TextMuted.copy(alpha = 0.5f),
                    unselectedTextColor = TextMuted.copy(alpha = 0.5f)
                )
            )
        }

        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelect(2) },
            icon = {
                if (pendingQueueCount > 0) {
                    BadgedBox(badge = {
                        Badge(containerColor = AccentError, contentColor = Color.White) {
                            Text("$pendingQueueCount", fontSize = 9.sp)
                        }
                    }) {
                        Icon(Icons.Default.Queue, contentDescription = "Queue")
                    }
                } else {
                    Icon(Icons.Default.Queue, contentDescription = "Queue")
                }
            },
            label = {
                Text(
                    text = if (pendingQueueCount > 0) "QUEUE ($pendingQueueCount)" else "QUEUE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = if (pendingQueueCount > 0) AccentError else AccentPurple
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepPurple,
                selectedTextColor = AccentPurple,
                indicatorColor = AccentPurple,
                unselectedIconColor = TextMuted.copy(alpha = 0.5f),
                unselectedTextColor = TextMuted.copy(alpha = 0.5f)
            )
        )

        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelect(3) },
            icon = {
                if (logCount > 0) {
                    BadgedBox(badge = {
                        Badge(containerColor = DeepPurple, contentColor = AccentPurple) {
                            Text(if (logCount > 99) "99+" else "$logCount", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }) {
                        Icon(Icons.Default.Storage, contentDescription = "Historique")
                    }
                } else {
                    Icon(Icons.Default.Storage, contentDescription = "Historique")
                }
            },
            label = {
                val histoLabel = if (isAdmin) "HISTO" else "HISTORIQUE"
                Text(
                    text = if (logCount > 0) "$histoLabel ($logCount)" else histoLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepPurple,
                selectedTextColor = AccentPurple,
                indicatorColor = AccentPurple,
                unselectedIconColor = TextMuted.copy(alpha = 0.5f),
                unselectedTextColor = TextMuted.copy(alpha = 0.5f)
            )
        )

        if (isAdmin) {
            NavigationBarItem(
                selected = selectedTab == 4,
                onClick = { onTabSelect(4) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = {
                    Text(
                        text = "HOME",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DeepPurple,
                    selectedTextColor = AccentPurple,
                    indicatorColor = AccentPurple,
                    unselectedIconColor = TextMuted.copy(alpha = 0.5f),
                    unselectedTextColor = TextMuted.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun SophisticatedArchitectureDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "EduAutomate Architecture Guide",
                fontWeight = FontWeight.Bold,
                color = AccentPurple
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. File d'attente (Queue) & Retry Automatique",
                    fontWeight = FontWeight.Bold,
                    color = AccentPurple
                )
                Text(
                    text = "• En cas d'erreur 500 (Appwrite/Serveur) ou d'absence de réseau, aucun SMS n'est perdu : il est placé dans la table 'sms_queue' de Room.\n" +
                           "• Dès que la connexion Internet est rétablie (NetworkCallback), le Retry automatique se déclenche.\n" +
                           "• Possibilité de déclencher le Retry manuellement depuis l'onglet 'QUEUE'.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                HorizontalDivider(color = DarkBorder)

                Text(
                    text = "2. En-tête HTTP de Sécurité (Header)",
                    fontWeight = FontWeight.Bold,
                    color = AccentPurple
                )
                Text(
                    text = "• Ajoutez vos clés de sécurité personnalisées (ex: 'X-SMS-Gateway-Token: MonSuperSecret123!' ou 'Authorization: Bearer ...') directement injectées dans chaque requête HTTP.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                HorizontalDivider(color = DarkBorder)

                Text(
                    text = "3. Modèles Regex Groupés par Cases à Cocher",
                    fontWeight = FontWeight.Bold,
                    color = AccentPurple
                )
                Text(
                    text = "• Cochez les modèles voulus pour extraire simultanément tous vos champs dans 'extracted_items'.\n" +
                           "• Le SMS source complet ('original_sms') et l'expéditeur ('source') sont toujours garantis.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepPurple,
                    contentColor = AccentPurple
                )
            ) {
                Text("Fermer")
            }
        }
    )
}

/**
 * Composant WebView interactif pour inspecter visuellement
 * le rendu HTML ou texte/JSON complet renvoyé par le serveur.
 */
@Composable
fun ServerResponseWebView(
    content: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { webView ->
            val isHtml = content.trim().startsWith("<") && (
                content.contains("<html", ignoreCase = true) ||
                content.contains("<body", ignoreCase = true) ||
                content.contains("<!DOCTYPE", ignoreCase = true) ||
                content.contains("<div", ignoreCase = true)
            )
            val htmlData = if (isHtml) {
                content
            } else {
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            background-color: #0E1118;
                            color: #00E676;
                            font-family: 'Courier New', Courier, monospace;
                            font-size: 12px;
                            padding: 12px;
                            margin: 0;
                            word-break: break-all;
                            white-space: pre-wrap;
                        }
                        .header {
                            color: #C084FC;
                            font-weight: bold;
                            margin-bottom: 8px;
                            font-size: 13px;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">&#128225; Rendu WebView - Retour Serveur :</div>
                    <div>${android.text.TextUtils.htmlEncode(content)}</div>
                </body>
                </html>
                """.trimIndent()
            }
            webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
        }
    )
}

