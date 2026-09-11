package com.example.ui

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HttpHeaderItem
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.NeutralButton
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Section complète de gestion dynamique des en-têtes HTTP (Headers) pour le Dashboard.
 * Permet d'ajouter, modifier, activer/désactiver et supprimer des en-têtes multiples.
 */
@Composable
fun MultipleHeadersSection(
    headers: List<HttpHeaderItem>,
    onToggleHeader: (String, Boolean) -> Unit,
    onUpdateHeader: (String, String, String) -> Unit,
    onDeleteHeaderClick: (HttpHeaderItem) -> Unit,
    onAddHeaderClick: () -> Unit,
    onPresetsClick: () -> Unit,
    onResetDefaultsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCount = remember(headers) { headers.count { it.isEnabled } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBg)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Barre supérieure avec titre, compteur et boutons d'action
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
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "EN-TÊTES HTTP (HEADERS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPurple,
                    letterSpacing = 0.5.sp
                )
            }

            // Badge du nombre d'en-têtes actifs
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeCount > 0) DeepPurple else NeutralButton)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$activeCount actif${if (activeCount > 1) "s" else ""} / ${headers.size}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeCount > 0) AccentPurple else TextMuted
                )
            }
        }

        // Boutons d'actions rapides : Ajouter / Modèles / Rétablir
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddHeaderClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepPurple,
                    contentColor = AccentPurple
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("btn_add_header")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onPresetsClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("btn_header_presets")
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Modèles", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            IconButton(
                onClick = onResetDefaultsClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeutralButton)
                    .testTag("btn_reset_headers")
            ) {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = "Rétablir en-têtes par défaut",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Liste des cartes d'en-têtes HTTP
        if (headers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Aucun en-tête HTTP configuré",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Cliquez sur '+ Ajouter' pour insérer vos clés d'API (ex: X-Appwrite-Project).",
                        fontSize = 10.sp,
                        color = TextMuted.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                headers.forEach { headerItem ->
                    HeaderItemCard(
                        header = headerItem,
                        onToggle = { isChecked -> onToggleHeader(headerItem.id, isChecked) },
                        onUpdate = { newKey, newVal -> onUpdateHeader(headerItem.id, newKey, newVal) },
                        onDelete = { onDeleteHeaderClick(headerItem) }
                    )
                }
            }
        }
    }
}

/**
 * Carte unitaire pour un En-tête HTTP avec bascule d'activation, édition et suppression.
 */
@Composable
fun HeaderItemCard(
    header: HttpHeaderItem,
    onToggle: (Boolean) -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var keyText by rememberSaveable(header.name) { mutableStateOf(header.name) }
    var valueText by rememberSaveable(header.value) { mutableStateOf(header.value) }

    val isSensitive = remember(header.name) {
        header.name.contains("key", ignoreCase = true) ||
                header.name.contains("token", ignoreCase = true) ||
                header.name.contains("auth", ignoreCase = true) ||
                header.name.contains("secret", ignoreCase = true) ||
                header.name.contains("jwt", ignoreCase = true)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("header_item_${header.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (header.isEnabled) DarkSurface else DarkSurface.copy(alpha = 0.6f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (header.isEnabled) DeepPurple.copy(alpha = 0.8f) else DarkBorder
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Ligne supérieure : Statut, Switch & Bouton Supprimer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Switch(
                        checked = header.isEnabled,
                        onCheckedChange = onToggle,
                        modifier = Modifier.testTag("toggle_header_${header.id}"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentPurple,
                            checkedTrackColor = DeepPurple,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = NeutralButton
                        )
                    )

                    if (header.description.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeutralButton)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = header.description,
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_delete_header_${header.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer cet en-tête",
                        tint = AccentError.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Champ Clé de l'en-tête (ex: X-Appwrite-Project)
            OutlinedTextField(
                value = keyText,
                onValueChange = {
                    keyText = it
                    onUpdate(it, valueText)
                },
                label = { Text("Clé du Header (ex: X-Appwrite-Project)", fontSize = 10.sp, color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (header.isEnabled) TextPrimary else TextMuted
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkBg,
                    unfocusedContainerColor = DarkBg
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            // Champ Valeur de l'en-tête (avec masquage du secret si besoin)
            OutlinedTextField(
                value = valueText,
                onValueChange = {
                    valueText = it
                    onUpdate(keyText, it)
                },
                label = { Text("Valeur (ex: 6a21910a00047c04e5af)", fontSize = 10.sp, color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (header.isEnabled) AccentGreen else TextMuted
                ),
                visualTransformation = if (isSensitive && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = {
                    if (isSensitive) {
                        IconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Masquer la valeur" else "Afficher la valeur",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkBg,
                    unfocusedContainerColor = DarkBg
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
        }
    }
}

/**
 * Dialogue Modal d'Ajout d'un nouvel En-tête HTTP avec suggestions rapides.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddHeaderDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, value: String, isEnabled: Boolean, description: String) -> Unit
) {
    var keyName by rememberSaveable { mutableStateOf("") }
    var keyValue by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var isEnabled by rememberSaveable { mutableStateOf(true) }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                Text(
                    text = "Ajouter un En-tête HTTP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Sélectionnez un modèle rapide ou saisissez un en-tête personnalisé :",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                // Chips de modèles rapides
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("X-Appwrite-Project", "6a21910a00047c04e5af", "Appwrite Project"),
                        Triple("X-Appwrite-Key", "", "Appwrite API Key"),
                        Triple("Authorization", "Bearer ", "Bearer Token"),
                        Triple("X-API-Key", "", "API Key"),
                        Triple("X-SMS-Gateway-Token", "", "Gateway Secret"),
                        Triple("Content-Type", "application/json", "Type Contenu")
                    ).forEach { (presetKey, presetVal, presetDesc) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (keyName == presetKey) DeepPurple else NeutralButton)
                                .clickable {
                                    keyName = presetKey
                                    if (presetVal.isNotBlank() && keyValue.isBlank()) {
                                        keyValue = presetVal
                                    }
                                    if (description.isBlank()) {
                                        description = presetDesc
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = presetKey,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (keyName == presetKey) AccentPurple else TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Champ Nom de la clé
                OutlinedTextField(
                    value = keyName,
                    onValueChange = { keyName = it },
                    label = { Text("Nom du Header (ex: X-Appwrite-Project)", fontSize = 11.sp, color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_header_name"),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Champ Valeur
                OutlinedTextField(
                    value = keyValue,
                    onValueChange = { keyValue = it },
                    label = { Text("Valeur (ex: 6a21910a00047c04e5af)", fontSize = 11.sp, color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_header_value"),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = AccentGreen),
                    visualTransformation = if (!isPasswordVisible && (keyName.contains("key", true) || keyName.contains("token", true) || keyName.contains("auth", true))) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Champ Description optionnelle
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Usage (optionnel)", fontSize = 11.sp, color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextSecondary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Activation directe
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Activer immédiatement cet en-tête", fontSize = 11.sp, color = TextPrimary)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentPurple,
                            checkedTrackColor = DeepPurple
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyName.isNotBlank()) {
                        onAdd(keyName.trim(), keyValue.trim(), isEnabled, description.trim())
                        onDismiss()
                    }
                },
                enabled = keyName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple, contentColor = AccentPurple),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_add_header")
            ) {
                Text("Ajouter", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextMuted)
            ) {
                Text("Annuler")
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Dialogue pour choisir parmi des modèles d'en-têtes préconfigurés (Presets).
 */
@Composable
fun HeaderPresetsDialog(
    onDismiss: () -> Unit,
    onSelectPreset: (HttpHeaderItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                Text(
                    text = "Modèles d'En-têtes Prédéfinis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Touchez un modèle pour l'insérer automatiquement dans vos configurations :",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                HttpHeaderItem.PRESETS.forEach { preset ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectPreset(preset.copy(id = java.util.UUID.randomUUID().toString()))
                                onDismiss()
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkBg),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPurple
                                )
                                Text(
                                    text = preset.description,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                            if (preset.value.isNotBlank()) {
                                Text(
                                    text = "Valeur: ${preset.value}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = AccentGreen
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextMuted)) {
                Text("Fermer")
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Dialogue de confirmation de suppression d'un en-tête.
 */
@Composable
fun DeleteHeaderDialog(
    header: HttpHeaderItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = AccentError, modifier = Modifier.size(20.dp))
                Text("Supprimer l'en-tête", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Text(
                text = "Êtes-vous certain de vouloir supprimer l'en-tête '${header.name}' ? Il ne sera plus inclus lors des transmissions HTTP.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentError, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Supprimer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextMuted)) {
                Text("Annuler")
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
