package com.example.data

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.RegexRuleDao
import com.example.data.db.RegexRuleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Repository gérant les modèles de règles Regex (CRUD, activation multiple par cases à cocher,
 * persistance Room, réinitialisation par défaut).
 */
object RegexRuleRepository {

    private var dao: RegexRuleDao? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    val DEFAULT_RULES = listOf(
        RegexRuleEntity(
            id = "preset_amount_yas",
            title = "Montant Reçu (Ariary / Ar)",
            pattern = "(?:recu|reçu|montant|crédit|credit)\\s*:\\s*([0-9]+(?:\\s*[0-9]+)?\\s*(?:Ar|Ariary|MGA))",
            description = "Extrait le montant en Ariary reçu (ex: 500 Ar)",
            sampleMessage = "19/08/26 08:16 Credit YAS recu : 500 Ar de Mioranirina 0386135942. Solde YAS actuel : 504 Ar. Bonus de 20 pourcent sur ce rechargement. Ref: 5661921553",
            isChecked = true,
            isCustom = false,
            orderIndex = 1
        ),
        RegexRuleEntity(
            id = "preset_phone_mg",
            title = "Numéro Téléphone Malgache (03x)",
            pattern = "((?:\\+261|0)3[23478][0-9]{7})",
            description = "Extrait les numéros de téléphone Madagascar (032, 033, 034, 037, 038)",
            sampleMessage = "19/08/26 08:16 Credit YAS recu : 500 Ar de Mioranirina 0386135942. Solde YAS actuel : 504 Ar. Bonus de 20 pourcent sur ce rechargement. Ref: 5661921553",
            isChecked = true,
            isCustom = false,
            orderIndex = 2
        ),
        RegexRuleEntity(
            id = "preset_ref_yas",
            title = "Référence / Transaction ID",
            pattern = "(?i)(?:Ref|Reference|Référence|TransID|ID)\\s*[:\\-\\s]\\s*([A-Za-z0-9]+)",
            description = "Extrait la référence de l'opération (ex: 5661921553)",
            sampleMessage = "19/08/26 08:16 Credit YAS recu : 500 Ar de Mioranirina 0386135942. Solde YAS actuel : 504 Ar. Bonus de 20 pourcent sur ce rechargement. Ref: 5661921553",
            isChecked = true,
            isCustom = false,
            orderIndex = 3
        ),
        RegexRuleEntity(
            id = "preset_sender_name",
            title = "Nom Expéditeur / Contact",
            pattern = "(?i)(?:de\\s+)([A-Za-zÀ-ÿ\\-]+)",
            description = "Extrait le nom de l'expéditeur du transfert (ex: Mioranirina)",
            sampleMessage = "19/08/26 08:16 Credit YAS recu : 500 Ar de Mioranirina 0386135942. Solde YAS actuel : 504 Ar. Bonus de 20 pourcent sur ce rechargement. Ref: 5661921553",
            isChecked = true,
            isCustom = false,
            orderIndex = 4
        ),
        RegexRuleEntity(
            id = "preset_balance_yas",
            title = "Solde Actuel (Ariary / Ar)",
            pattern = "(?i)(?:Solde[^:]*:\\s*)([0-9]+(?:\\s*[0-9]+)?\\s*(?:Ar|Ariary|MGA))",
            description = "Extrait le solde restant après opération (ex: 504 Ar)",
            sampleMessage = "19/08/26 08:16 Credit YAS recu : 500 Ar de Mioranirina 0386135942. Solde YAS actuel : 504 Ar. Bonus de 20 pourcent sur ce rechargement. Ref: 5661921553",
            isChecked = true,
            isCustom = false,
            orderIndex = 5
        ),
        RegexRuleEntity(
            id = "preset_otp",
            title = "Code OTP / Sécurité",
            pattern = "(?:code|otp|mdp|validation|secret)[^0-9]*([0-9]{4,8})",
            description = "Extrait les codes de confirmation à 4-8 chiffres",
            sampleMessage = "Votre code secret d'accès bancaire est: 894312.",
            isChecked = false,
            isCustom = false,
            orderIndex = 6
        )
    )

    fun init(context: Context) {
        if (dao == null) {
            val db = AppDatabase.getDatabase(context)
            dao = db.regexRuleDao()
            
            // Initialisation des règles par défaut si la table est vide ou mise à jour avec les règles Malgaches
            scope.launch {
                try {
                    val count = dao?.getCount() ?: 0
                    if (count == 0) {
                        dao?.insertAll(DEFAULT_RULES)
                    } else {
                        val existingRules = dao?.getAllRulesSync() ?: emptyList()
                        val hasYasRule = existingRules.any { it.id == "preset_amount_yas" }
                        if (!hasYasRule) {
                            dao?.clearAll()
                            dao?.insertAll(DEFAULT_RULES)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RegexRuleRepo", "Erreur init règles par défaut", e)
                }
            }
        }
    }

    /**
     * Flux réactif de tous les modèles Regex
     */
    fun getAllRules(): Flow<List<RegexRuleEntity>> {
        return dao?.getAllRules() ?: MutableStateFlow(DEFAULT_RULES)
    }

    /**
     * Flux réactif des modèles cochés (actifs dans le groupe)
     */
    fun getCheckedRules(): Flow<List<RegexRuleEntity>> {
        return dao?.getCheckedRules() ?: MutableStateFlow(DEFAULT_RULES.filter { it.isChecked })
    }

    suspend fun getCheckedRulesSync(): List<RegexRuleEntity> {
        return dao?.getCheckedRulesSync() ?: DEFAULT_RULES.filter { it.isChecked }
    }

    /**
     * Coche ou décoche un modèle pour l'inclure/exclure du groupe actif
     */
    fun toggleRuleCheck(id: String, isChecked: Boolean) {
        scope.launch {
            try {
                dao?.setRuleChecked(id, isChecked)
            } catch (e: Exception) {
                android.util.Log.e("RegexRuleRepo", "Erreur toggle check rule $id", e)
            }
        }
    }

    /**
     * Ajoute un nouveau modèle personnalisé créé par l'utilisateur
     */
    fun addRule(
        title: String,
        pattern: String,
        description: String,
        sampleMessage: String,
        isChecked: Boolean = true
    ) {
        val newEntity = RegexRuleEntity(
            id = "custom_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            pattern = pattern.trim(),
            description = description.trim().ifBlank { "Modèle personnalisé" },
            sampleMessage = sampleMessage.trim().ifBlank { "Message test" },
            isChecked = isChecked,
            isCustom = true,
            orderIndex = 100
        )
        scope.launch {
            try {
                dao?.insertRule(newEntity)
            } catch (e: Exception) {
                android.util.Log.e("RegexRuleRepo", "Erreur ajout règle", e)
            }
        }
    }

    /**
     * Modifie un modèle existant (titre, motif, description, exemple)
     */
    fun updateRule(
        id: String,
        title: String,
        pattern: String,
        description: String,
        sampleMessage: String,
        isChecked: Boolean
    ) {
        scope.launch {
            try {
                val existing = dao?.getRuleById(id)
                val updated = RegexRuleEntity(
                    id = id,
                    title = title.trim(),
                    pattern = pattern.trim(),
                    description = description.trim(),
                    sampleMessage = sampleMessage.trim(),
                    isChecked = isChecked,
                    isCustom = existing?.isCustom ?: true,
                    orderIndex = existing?.orderIndex ?: 50
                )
                dao?.updateRule(updated)
            } catch (e: Exception) {
                android.util.Log.e("RegexRuleRepo", "Erreur maj règle $id", e)
            }
        }
    }

    /**
     * Supprime un modèle
     */
    fun deleteRule(id: String) {
        scope.launch {
            try {
                dao?.deleteRuleById(id)
            } catch (e: Exception) {
                android.util.Log.e("RegexRuleRepo", "Erreur suppression règle $id", e)
            }
        }
    }

    /**
     * Réinitialise les règles par défaut
     */
    fun resetToDefaults() {
        scope.launch {
            try {
                dao?.clearAll()
                dao?.insertAll(DEFAULT_RULES)
            } catch (e: Exception) {
                android.util.Log.e("RegexRuleRepo", "Erreur reset règles", e)
            }
        }
    }
}
