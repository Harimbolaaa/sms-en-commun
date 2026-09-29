package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.AppwriteAuthService
import com.example.auth.AppwriteUserSummary

private val DarkBg = Color(0xFF0F0B1E)
private val DarkSurface = Color(0xFF1B1533)
private val DeepPurple = Color(0xFF2A1B4E)
private val AccentPurple = Color(0xFFBB86FC)
private val AccentGreen = Color(0xFF00E676)
private val AccentError = Color(0xFFFF5252)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFD1C4E9)
private val TextMuted = Color(0xFF9E8EBF)
private val DarkBorder = Color(0xFF382A61)

@Composable
fun ClientManagementDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var usersList by remember { mutableStateOf<List<AppwriteUserSummary>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(true) }

    // Formulaire d'ajout
    var newPhone by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }

    fun refreshUsers() {
        isLoadingUsers = true
        AppwriteAuthService.listUsers { list ->
            usersList = list
            isLoadingUsers = false
        }
    }

    LaunchedEffect(Unit) {
        refreshUsers()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DeepPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "GESTION DES CLIENTS APPWRITE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Comptes autorisés à se connecter",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { refreshUsers() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir", tint = AccentPurple)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Bouton toggle formulaire d'ajout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Utilisateurs enregistrés (${usersList.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    Button(
                        onClick = { showCreateForm = !showCreateForm },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showCreateForm) DarkBorder else DeepPurple,
                            contentColor = AccentPurple
                        )
                    ) {
                        Icon(
                            imageVector = if (showCreateForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showCreateForm) "Masquer" else "Nouveau Client",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Formulaire d'ajout rapide
                if (showCreateForm) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = DarkBg,
                        border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Créer un nouveau compte client",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple
                            )

                            if (createError != null) {
                                Text(
                                    text = createError ?: "",
                                    fontSize = 11.sp,
                                    color = AccentError
                                )
                            }

                            OutlinedTextField(
                                value = newPhone,
                                onValueChange = { newPhone = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_new_client_phone"),
                                label = { Text("Numéro Téléphone", fontSize = 11.sp) },
                                placeholder = { Text("Ex: 0340000002") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AccentPurple) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_new_client_password"),
                                label = { Text("Mot de passe", fontSize = 11.sp) },
                                placeholder = { Text("Mot de passe du client") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentPurple) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_new_client_name"),
                                label = { Text("Nom ou Libellé (Optionnel)", fontSize = 11.sp) },
                                placeholder = { Text("Ex: Client Antananarivo") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AccentPurple) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            val generatedFakeEmail = AppwriteAuthService.phoneToFakeEmail(newPhone)
                            Text(
                                text = "Sera enregistré sur Appwrite Cloud comme : $generatedFakeEmail",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )

                            Button(
                                onClick = {
                                    if (newPhone.isBlank() || newPassword.isBlank()) {
                                        createError = "Le numéro et le mot de passe sont obligatoires."
                                        return@Button
                                    }
                                    isCreating = true
                                    createError = null

                                    AppwriteAuthService.createClientUser(
                                        phone = newPhone.trim(),
                                        password = newPassword,
                                        name = newName.trim(),
                                        role = "client",
                                        onResult = { success, errorMsg ->
                                            isCreating = false
                                            if (success) {
                                                Toast.makeText(context, "Client '$newPhone' créé avec succès !", Toast.LENGTH_SHORT).show()
                                                newPhone = ""
                                                newPassword = ""
                                                newName = ""
                                                showCreateForm = false
                                                refreshUsers()
                                            } else {
                                                createError = errorMsg ?: "Erreur lors de la création."
                                            }
                                        }
                                    )
                                },
                                enabled = !isCreating,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = Color.Black),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("btn_save_new_client")
                            ) {
                                if (isCreating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Création sur Appwrite Cloud...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Enregistrer le Client", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Liste des utilisateurs
                if (isLoadingUsers) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentPurple)
                    }
                } else if (usersList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucun utilisateur trouvé.", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(usersList) { user ->
                            UserItemCard(user = user)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserItemCard(user: AppwriteUserSummary) {
    val isAdmin = user.role.equals("admin", ignoreCase = true)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkBg,
        border = BorderStroke(1.dp, if (isAdmin) AccentPurple.copy(alpha = 0.4f) else DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                        .clip(CircleShape)
                        .background(if (isAdmin) DeepPurple else DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.VerifiedUser else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isAdmin) AccentPurple else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user.phone,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isAdmin) DeepPurple else DarkSurface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = user.role.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdmin) AccentPurple else TextMuted
                            )
                        }
                    }

                    if (user.name.isNotBlank()) {
                        Text(
                            text = user.name,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Text(
                        text = "Appwrite ID: ${user.fakeEmail}",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (user.status) AccentGreen else AccentError)
            )
        }
    }
}
