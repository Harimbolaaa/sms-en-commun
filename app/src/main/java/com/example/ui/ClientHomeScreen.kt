package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AutomationPreferences
import com.example.data.DeviceSyncRepository
import com.example.data.UserDialerRepository
import com.example.data.db.AutomationLogEntity
import com.example.receiver.AppUnhideReceiver
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkGreenBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.NeutralButton
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modèle unitaire d'un SMS pour l'affichage Messenger (Reçu ou Envoyé)
 */
data class SmsConversationMessage(
    val id: String,
    val sender: String,
    val rawMessage: String,
    val timestamp: Long,
    val operator: String? = null,
    val amount: Long? = null,
    val ref: String? = null,
    val isOutgoing: Boolean = false
)

/**
 * Modèle de fil de discussion groupé par numéro de téléphone émetteur
 */
data class SmsThreadSummary(
    val sender: String,
    val lastMessage: SmsConversationMessage,
    val totalCount: Int,
    val unreadCount: Int,
    val isRead: Boolean
)

/**
 * Écran d'accueil du Client (Menu Home) :
 * - UI ultra-compacte, sans marges inutiles ni textes verbeux de persistance.
 * - Gestion directe du composeur secret (*#*#XXXX#*#*) et du masquage d'icône.
 * - Historique Messenger en bas groupé par numéro émetteur avec bulle de conversation,
 *   bouton flèche retour en haut, badge Lu/Non-lu et affichage complet des SMS bruts (rawMessage).
 */
@Composable
fun ClientHomeScreen(
    userPhone: String,
    userId: String,
    isAdmin: Boolean = false,
    persistedLogs: List<AutomationLogEntity> = emptyList(),
    isHideIconOnClose: Boolean,
    onToggleHideIconOnClose: (Boolean) -> Unit,
    onHideIconNow: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember { AutomationPreferences(context) }

    // Observation Room DB en temps réel pour le rôle appareil (SOURCE vs PARTAGE)
    val roomDeviceConfig by DeviceSyncRepository.observeConfig(context, userPhone)
        .collectAsState(initial = null)
    val currentRole = roomDeviceConfig?.deviceRole ?: prefs.getDeviceRole(userPhone)

    // Observation Room DB en temps réel pour le code composeur
    val roomEntity by UserDialerRepository.observeUserDialerCode(context, userPhone)
        .collectAsState(initial = null)

    var inputCode by rememberSaveable { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }
    var testDialogMessage by remember { mutableStateOf("") }

    // Émetteur sélectionné pour le mode discussion Messenger (null = vue liste, non-null = vue conversation)
    var selectedSender by rememberSaveable { mutableStateOf<String?>(null) }
    var readUpdateTrigger by remember { mutableIntStateOf(0) }

    val activeCode = remember(roomEntity, prefs.getSecretDialerCode(userPhone)) {
        val fromRoom = roomEntity?.secretCode
        if (!fromRoom.isNullOrBlank() && roomEntity?.isCustomized == true) {
            fromRoom
        } else {
            prefs.getSecretDialerCode(userPhone)
        }
    }

    val isCustomized = remember(roomEntity, activeCode) {
        if (roomEntity != null) {
            roomEntity?.isCustomized == true && !roomEntity?.secretCode.isNullOrBlank()
        } else {
            prefs.isDialerCodeCustom(userPhone) && activeCode.isNotBlank()
        }
    }

    // Synchronisation avec Appwrite en arrière-plan
    LaunchedEffect(userId, userPhone) {
        UserDialerRepository.syncWithAppwrite(context, userId, userPhone)
    }

    // Agrégation et extraction de TOUS les SMS reçus
    val allSmsMessages = remember(persistedLogs, readUpdateTrigger) {
        extractAllSmsMessages(context, persistedLogs)
    }

    // Groupement des SMS par numéro de téléphone émetteur
    val conversationThreads = remember(allSmsMessages, readUpdateTrigger) {
        groupMessagesBySender(allSmsMessages, prefs)
    }

    // Si une discussion est ouverte, gestion du bouton retour système
    if (selectedSender != null) {
        BackHandler {
            selectedSender = null
        }

        // --- VUE MESSENGER : CONVERSATION DÉTAILLÉE DU NUMÉRO SÉLECTIONNÉ ---
        val currentSender = selectedSender ?: ""
        val messagesForSender = remember(allSmsMessages, currentSender) {
            allSmsMessages.filter { it.sender.equals(currentSender, ignoreCase = true) }
                .sortedBy { it.timestamp } // Chronologique (plus ancien au plus récent)
        }

        MessengerConversationView(
            sender = currentSender,
            messages = messagesForSender,
            onBackClick = { selectedSender = null }
        )
        return
    }

    // --- VUE PRINCIPALE : ACCUEIL CLIENT COMPACT + LISTE DES DISCUSSIONS ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. BANNIÈRE ULTRA-COMPACTE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurface)
                .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAdmin) DeepPurple else DarkGreenBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isAdmin) AccentPurple else AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = if (userPhone.isNotBlank()) "Client : $userPhone" else "Espace Client",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Passerelle SMS active",
                        fontSize = 10.sp,
                        color = AccentGreen
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (currentRole == "SOURCE") DarkGreenBg else DeepPurple)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (currentRole == "SOURCE") "SOURCE" else "PARTAGE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentRole == "SOURCE") AccentGreen else AccentPurple
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCustomized) DeepPurple else NeutralButton)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isCustomized) "CODE PERSO" else "DÉFAUT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCustomized) AccentPurple else TextMuted
                    )
                }
            }
        }

        // 2. CARTE COMPOSEUR TÉLÉPHONIQUE (COMPACTE, SANS TEXTES SUPERFLUS)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
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
                        Icon(Icons.Default.Dialpad, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Composeur Téléphonique",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Code actif actuel
                    Text(
                        text = if (isCustomized && activeCode.isNotBlank()) "*#*#$activeCode#*#*" else "*#*#4444#*#*",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AccentPurple
                    )
                }

                // Ligne de saisie + Bouton Enregistrer + Par défaut + Tester
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { newVal ->
                            inputCode = newVal.filter { it.isDigit() }.take(4)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("input_custom_dialer_code"),
                        placeholder = { Text("4 chiffres", fontSize = 11.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            isSaving = true
                            UserDialerRepository.saveCustomCode(
                                context = context,
                                userId = userId,
                                userPhone = userPhone,
                                newCode = inputCode
                            ) { success, errorMsg ->
                                isSaving = false
                                if (success) {
                                    Toast.makeText(context, "Code *#*#$inputCode#*#* enregistré !", Toast.LENGTH_SHORT).show()
                                    inputCode = ""
                                } else {
                                    Toast.makeText(context, errorMsg ?: "Erreur", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = inputCode.length == 4 && !isSaving,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("btn_save_dialer_code")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = AccentPurple, strokeWidth = 2.dp)
                        } else {
                            Text("Enregistrer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isCustomized) {
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                UserDialerRepository.resetToDefault(context, userId, userPhone) {
                                    inputCode = ""
                                    Toast.makeText(context, "Codes réinitialisés", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val currentActive = if (isCustomized && activeCode.isNotBlank()) activeCode else "4444"
                            val intent = Intent("android.provider.Telephony.SECRET_CODE").apply {
                                data = Uri.parse("android_secret_code://$currentActive")
                            }
                            AppUnhideReceiver().onReceive(context, intent)
                            testDialogMessage = "Code *#*#$currentActive#*#* testé avec succès !"
                            showTestDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("btn_test_dialer_code")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Tester", tint = AccentPurple, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // 3. CARTE MASQUAGE D'ICÔNE ULTRA-COMPACTE (UNE SEULE LIGNE)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isHideIconOnClose) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = if (isHideIconOnClose) AccentPurple else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Masquer à la fermeture",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = isHideIconOnClose,
                        onCheckedChange = { onToggleHideIconOnClose(it) },
                        modifier = Modifier.height(28.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentPurple,
                            checkedTrackColor = DeepPurple,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBg
                        )
                    )

                    Button(
                        onClick = onHideIconNow,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeutralButton, contentColor = AccentError),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Masquer", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. SECTION HISTORIQUE DE MESSAGES DISCUSSIONS MESSENGER GROUPÉES PAR ÉMETTEUR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                Text(
                    text = "Discussions SMS Reçus",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Badge total de discussions
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DeepPurple)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${conversationThreads.size} émetteur(s)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPurple
                )
            }
        }

        // LISTE DES FILS DE DISCUSSIONS MESSENGER
        if (conversationThreads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Aucun SMS intercepté",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Les SMS reçus s'afficheront ici groupés par numéro d'expéditeur.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(conversationThreads, key = { it.sender }) { thread ->
                    ConversationThreadCard(
                        thread = thread,
                        onClick = {
                            prefs.markSenderAsRead(thread.sender)
                            readUpdateTrigger++
                            selectedSender = thread.sender
                        }
                    )
                }
            }
        }
    }

    // Modal de confirmation du test
    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = { showTestDialog = false },
            containerColor = DarkSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = AccentPurple)
                    Text("Test Composeur", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = { Text(testDialogMessage, color = TextSecondary, fontSize = 12.sp) },
            confirmButton = {
                Button(
                    onClick = { showTestDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Carte d'une discussion Messenger dans la liste
 */
@Composable
fun ConversationThreadCard(
    thread: SmsThreadSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("thread_${thread.sender}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(
            1.dp,
            if (!thread.isRead) AccentPurple.copy(alpha = 0.5f) else DarkBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar de l'émetteur
            val initial = thread.sender.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DeepPurple)
                    .border(BorderStroke(1.dp, AccentPurple), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = AccentPurple,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Informations du fil : Émetteur + Dernier message
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.sender,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatRelativeTime(thread.lastMessage.timestamp),
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Extrait du message complet brut
                Text(
                    text = thread.lastMessage.rawMessage.ifBlank { "Message sans contenu" },
                    fontSize = 11.sp,
                    color = if (!thread.isRead) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Statut Lu / Non-Lu
            if (!thread.isRead) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentPurple)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (thread.unreadCount > 1) "${thread.unreadCount}" else "Nouveau",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Lu",
                        tint = AccentGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Lu",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

/**
 * Vue détaillée d'une conversation Messenger :
 * Affiche l'historique complet des SMS reçus de cet émetteur avec bulles de conversation Messenger,
 * le bouton de retour flèche en haut pour changer d'émetteur, et le message brut complet sans troncature.
 */
@Composable
fun MessengerConversationView(
    sender: String,
    messages: List<SmsConversationMessage>,
    onBackClick: () -> Unit
) {
    val listState = rememberLazyListState()

    // Défilement automatique vers le bas (message le plus récent) à l'ouverture
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // EN-TÊTE MESSENGER AVEC FLÈCHE RETOUR EN HAUT
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bouton retour flèche en haut
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_back_to_discussions")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour aux discussions",
                        tint = AccentPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Avatar du contact
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DeepPurple)
                        .border(BorderStroke(1.dp, AccentPurple), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sender.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                        color = AccentPurple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Nom/Numéro de l'émetteur + Compteur
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sender,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${messages.size} message(s) reçu(s) • Lu",
                        fontSize = 10.sp,
                        color = AccentGreen
                    )
                }
            }
        }

        // CORPS DE LA DISCUSSION (BULLES DE CONVERSATION MESSENGER)
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun message dans cette discussion",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessengerMessageBubble(message = msg)
                }
            }
        }
    }
}

/**
 * Bulle de message style Messenger :
 * - Alignée à GAUCHE pour les SMS reçus
 * - Alignée à DROITE pour les SMS envoyés (notamment par le matériel Partage)
 */
@Composable
fun MessengerMessageBubble(message: SmsConversationMessage) {
    val isOutgoing = message.isOutgoing

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        // Enveloppe de la bulle
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isOutgoing) 14.dp else 4.dp,
                        topEnd = if (isOutgoing) 4.dp else 14.dp,
                        bottomEnd = 14.dp,
                        bottomStart = 14.dp
                    )
                ),
            color = if (isOutgoing) DeepPurple else DarkSurface,
            border = BorderStroke(1.dp, if (isOutgoing) AccentPurple else DeepPurple.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Badges d'extraction ou indicateur de sens (Envoyé / Reçu)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOutgoing) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeutralButton)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "📤 Envoyé (Partage)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple
                            )
                        }
                    } else {
                        if (!message.operator.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DeepPurple)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = message.operator,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPurple
                                )
                            }
                        }

                        if (message.amount != null && message.amount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkGreenBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${message.amount} Ar",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                            }
                        }

                        if (!message.ref.isNullOrBlank()) {
                            Text(
                                text = "Réf: ${message.ref}",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // CORPS DU MESSAGE BRUT COMPLET (RAW MESSAGE SANS TRONCATURE)
                SelectionContainer {
                    Text(
                        text = message.rawMessage,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }

                // Horodatage du message
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFullDateTime(message.timestamp),
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Transmis",
                            tint = AccentPurple,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Extraction de tous les messages SMS (Reçus ET Envoyés) à partir des logs Room et de la boîte de réception Telephony
 */
private fun extractAllSmsMessages(
    context: Context,
    persistedLogs: List<AutomationLogEntity>
): List<SmsConversationMessage> {
    val resultList = mutableListOf<SmsConversationMessage>()

    // 1. Extraction depuis Room automation_logs (Reçus et Envoyés)
    for (log in persistedLogs) {
        val isSent = log.tag == "SMS_SENT" || log.type == "SMS_SENT" || log.status == "SENT"
        val isSms = isSent || log.tag == "SMS_RECEIVER" || log.type == "SMS_INTERCEPTED" || !log.source.isNullOrBlank()

        if (isSms) {
            val contact = log.source?.trim()?.ifBlank { "Passerelle" } ?: "Passerelle"
            val (rawMsg, op, amount, ref) = parseLogDetails(log)
            resultList.add(
                SmsConversationMessage(
                    id = "room_${log.id}",
                    sender = contact,
                    rawMessage = rawMsg,
                    timestamp = log.timestamp,
                    operator = op,
                    amount = amount,
                    ref = ref,
                    isOutgoing = isSent
                )
            )
        }
    }

    // 2. Extraction depuis la boîte de réception Telephony Android (Inbox et Sent) si permission accordée
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
        // A. Boîte de réception (inbox)
        try {
            val uriInbox = Uri.parse("content://sms/inbox")
            val cursor = context.contentResolver.query(
                uriInbox,
                arrayOf("_id", "address", "body", "date"),
                null,
                null,
                "date DESC LIMIT 150"
            )
            cursor?.use {
                val addrIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")
                val idIdx = it.getColumnIndex("_id")
                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val address = if (addrIdx != -1) it.getString(addrIdx) ?: "Inconnu" else "Inconnu"
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()

                    if (body.isNotBlank()) {
                        val isDuplicate = resultList.any { r ->
                            r.sender == address && Math.abs(r.timestamp - date) < 3000
                        }
                        if (!isDuplicate) {
                            resultList.add(
                                SmsConversationMessage(
                                    id = "telephony_in_$id",
                                    sender = address.trim(),
                                    rawMessage = body,
                                    timestamp = date,
                                    isOutgoing = false
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ClientHomeScreen", "Impossible de lire la boîte SMS telephony inbox", e)
        }

        // B. Boîte d'envoi (sent)
        try {
            val uriSent = Uri.parse("content://sms/sent")
            val cursorSent = context.contentResolver.query(
                uriSent,
                arrayOf("_id", "address", "body", "date"),
                null,
                null,
                "date DESC LIMIT 100"
            )
            cursorSent?.use {
                val addrIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")
                val idIdx = it.getColumnIndex("_id")
                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val address = if (addrIdx != -1) it.getString(addrIdx) ?: "Destinataire" else "Destinataire"
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()

                    if (body.isNotBlank()) {
                        val isDuplicate = resultList.any { r ->
                            r.sender == address && Math.abs(r.timestamp - date) < 3000
                        }
                        if (!isDuplicate) {
                            resultList.add(
                                SmsConversationMessage(
                                    id = "telephony_out_$id",
                                    sender = address.trim(),
                                    rawMessage = body,
                                    timestamp = date,
                                    isOutgoing = true
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ClientHomeScreen", "Impossible de lire la boîte SMS telephony sent", e)
        }
    }

    return resultList.sortedByDescending { it.timestamp }
}

/**
 * Analyse le message et les métadonnées depuis un log
 */
private fun parseLogDetails(log: AutomationLogEntity): Quadruple<String, String?, Long?, String?> {
    var raw = ""
    var operator: String? = null
    var amount: Long? = null
    var ref: String? = null

    // 1. Tentative d'extraction depuis payloadJson
    if (!log.payloadJson.isNullOrBlank()) {
        try {
            val json = JSONObject(log.payloadJson)
            if (json.has("data")) {
                val data = json.getJSONObject("data")
                raw = data.optString("rawMessage", "")
                operator = data.optString("operator", null)
                amount = data.optLong("parsedAmount", 0L).takeIf { it > 0 }
                ref = data.optString("referenceId", null)
            } else if (json.has("rawMessage")) {
                raw = json.optString("rawMessage", "")
            }
        } catch (_: Exception) {}
    }

    // 2. Si non trouvé, tentative depuis details
    if (raw.isBlank() && !log.details.isNullOrBlank()) {
        val details = log.details
        if (details.contains("SMS source:")) {
            raw = details.substringAfter("SMS source: ")
                .substringBefore("\nModèles")
                .substringBefore("\nCause")
                .trim()
        }
    }

    // 3. Repli sur le texte du log
    if (raw.isBlank()) {
        raw = log.message
    }

    return Quadruple(raw, operator, amount, ref)
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Regroupe tous les messages par numéro de téléphone émetteur et calcule les statuts de lecture
 */
private fun groupMessagesBySender(
    messages: List<SmsConversationMessage>,
    prefs: AutomationPreferences
): List<SmsThreadSummary> {
    val grouped = messages.groupBy { it.sender.trim() }
    val summaries = mutableListOf<SmsThreadSummary>()

    grouped.forEach { (sender, list) ->
        val sortedList = list.sortedByDescending { it.timestamp }
        val newest = sortedList.first()
        val lastReadTime = prefs.getSenderLastReadTime(sender)
        val isRead = newest.timestamp <= lastReadTime
        val unreadCount = if (isRead) 0 else sortedList.count { it.timestamp > lastReadTime }

        summaries.add(
            SmsThreadSummary(
                sender = sender,
                lastMessage = newest,
                totalCount = list.size,
                unreadCount = unreadCount,
                isRead = isRead
            )
        )
    }

    return summaries.sortedByDescending { it.lastMessage.timestamp }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    return when {
        diff < 60_000L -> "À l'instant"
        diff < 24 * 3600_000L -> timeFormat.format(Date(timestamp))
        diff < 48 * 3600_000L -> "Hier"
        else -> dateFormat.format(Date(timestamp))
    }
}

private fun formatFullDateTime(timestamp: Long): String {
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return format.format(Date(timestamp))
}
