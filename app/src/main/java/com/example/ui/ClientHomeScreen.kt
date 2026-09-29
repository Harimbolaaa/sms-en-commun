package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AutomationPreferences
import com.example.data.UserDialerRepository
import com.example.receiver.AppUnhideReceiver
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkErrorBg
import com.example.ui.theme.DarkGreenBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.NeutralButton
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppIconManager

/**
 * Écran d'accueil du Client (Page Home) :
 * - Permet à chaque utilisateur de configurer son propre code composeur à 4 chiffres (*#*#XXXX#*#*).
 * - Gère les codes par défaut (4444 et 1234) et leur écrasement une fois personnalisé.
 * - Synchronise la configuration dans Room DB locale et dans Appwrite Cloud.
 * - Propose le test du composeur et la gestion du masquage d'icône.
 */
@Composable
fun ClientHomeScreen(
    userPhone: String,
    userId: String,
    isAdmin: Boolean = false,
    isHideIconOnClose: Boolean,
    onToggleHideIconOnClose: (Boolean) -> Unit,
    onHideIconNow: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember { AutomationPreferences(context) }

    // Observation Room DB en temps réel
    val roomEntity by UserDialerRepository.observeUserDialerCode(context, userPhone)
        .collectAsState(initial = null)

    var inputCode by rememberSaveable { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var isSyncingCloud by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }
    var testDialogMessage by remember { mutableStateOf("") }

    // Initialisation depuis SharedPreferences / Room
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

    // Synchronisation intelligente au chargement (envoi des modifs locales si hors-ligne précédemment)
    LaunchedEffect(userId, userPhone) {
        UserDialerRepository.syncWithAppwrite(context, userId, userPhone)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. BANNIÈRE D'ACCUEIL CLIENT ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isAdmin) DeepPurple else DarkGreenBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isAdmin) "ESPACE ADMINISTRATEUR" else "ESPACE CLIENT SÉCURISÉ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdmin) AccentPurple else AccentGreen,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (userPhone.isNotBlank()) "Bienvenue, $userPhone" else "Bienvenue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Gérez la sécurité, votre composeur et la discrétion de l'application.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // --- 2. CARTE : CODE SECRET DU COMPOSEUR TÉLÉPHONIQUE (*#*#XXXX#*#*) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Titre et Icône
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DeepPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dialpad,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Composeur Téléphonique",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Code secret pour réafficher l'icône masquée",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Statut Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCustomized) DeepPurple else NeutralButton)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isCustomized) "PERSONNALISÉ" else "PAR DÉFAUT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCustomized) AccentPurple else TextMuted
                        )
                    }
                }

                // Explication & état actuel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBg)
                        .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Code secret actif à composer :",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (isCustomized && activeCode.isNotBlank()) {
                            Text(
                                text = "*#*#$activeCode#*#*",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentPurple,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✓ Votre code personnalisé écrase les codes par défaut",
                                fontSize = 10.sp,
                                color = AccentGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "*#*#4444#*#*",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "ou",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "*#*#1234#*#*",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Codes d'usine par défaut actifs (4444 ou 1234)",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Champ de saisie des 4 chiffres
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Changer pour 4 nouveaux chiffres :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { newVal ->
                            val filtered = newVal.filter { it.isDigit() }.take(4)
                            inputCode = filtered
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_dialer_code"),
                        placeholder = { Text("Ex: 5678, 9999, 2026...", fontSize = 12.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (inputCode.isNotEmpty()) {
                                Text(
                                    text = "${inputCode.length}/4",
                                    fontSize = 10.sp,
                                    color = if (inputCode.length == 4) AccentGreen else TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (inputCode.isNotEmpty()) {
                        Text(
                            text = "Aperçu de composition : *#*#${inputCode.padEnd(4, '•')}#*#*",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (inputCode.length == 4) AccentPurple else TextMuted
                        )
                    }
                }

                // Boutons d'action pour enregistrer ou réinitialiser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                    Toast.makeText(
                                        context,
                                        "Code *#*#$inputCode#*#* enregistré et synchronisé !",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    inputCode = ""
                                } else {
                                    Toast.makeText(
                                        context,
                                        errorMsg ?: "Erreur d'enregistrement",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        enabled = inputCode.length == 4 && !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepPurple,
                            contentColor = AccentPurple
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("btn_save_dialer_code")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentPurple, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enregistrement...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enregistrer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            UserDialerRepository.resetToDefault(
                                context = context,
                                userId = userId,
                                userPhone = userPhone
                            ) {
                                inputCode = ""
                                Toast.makeText(
                                    context,
                                    "Codes par défaut rétablis (4444 et 1234) !",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        enabled = isCustomized,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_reset_dialer_code")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Par défaut", fontSize = 11.sp)
                    }
                }

                // Bouton de simulation et test
                OutlinedButton(
                    onClick = {
                        val currentActive = if (isCustomized && activeCode.isNotBlank()) activeCode else "4444"
                        // Simulation du broadcast receiver
                        val intent = Intent("android.provider.Telephony.SECRET_CODE").apply {
                            data = Uri.parse("android_secret_code://$currentActive")
                        }
                        AppUnhideReceiver().onReceive(context, intent)
                        testDialogMessage = "Le code *#*#$currentActive#*#* a été simulé avec succès.\n\nL'icône du lanceur a été réaffichée et les autorisations sont actives."
                        showTestDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("btn_test_dialer_code")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tester la composition du code actif",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- 3. CARTE : DISCRÉTION & MASQUAGE D'ICÔNE ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeutralButton),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isHideIconOnClose) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (isHideIconOnClose) AccentPurple else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Masquage à la fermeture",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Cache l'icône de l'accueil Android",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isHideIconOnClose,
                        onCheckedChange = { onToggleHideIconOnClose(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentPurple,
                            checkedTrackColor = DeepPurple,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBg
                        )
                    )
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Masquer l'icône immédiatement :",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Button(
                        onClick = onHideIconNow,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeutralButton,
                            contentColor = AccentError
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Masquer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 4. CARTE : SYNCHRONISATION CLOUD & STOCKAGE ROOM ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Persistance double : Room DB locale & Cloud Appwrite",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            isSyncingCloud = true
                            UserDialerRepository.syncWithAppwrite(context, userId, userPhone, forcePush = true) { success ->
                                isSyncingCloud = false
                                Toast.makeText(
                                    context,
                                    if (success) "Code synchronisé avec la table Appwrite !" else "Synchronisation effectuée",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Synchroniser",
                            tint = AccentPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "• Room DB : Enregistré dans la table 'user_dialer_codes' pour le compte $userPhone.\n" +
                            "• Cloud Appwrite : Synchronisé dans les préférences utilisateurs distantes.\n" +
                            "• Composeur : Détection automatique des codes secrets via BroadcastReceiver.",
                    fontSize = 10.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }
        }
    }

    // Modal de confirmation du test composeur
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
                    Text("Test Composeur Réussi", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(testDialogMessage, color = TextSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = { showTestDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Compris", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
