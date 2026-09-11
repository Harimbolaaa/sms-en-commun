package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room représentant un modèle / règle Regex configurable par l'utilisateur.
 * Permet la création, modification, suppression et activation multiple (groupement par cases à cocher).
 */
@Entity(tableName = "regex_rules")
data class RegexRuleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val pattern: String,
    val description: String,
    val sampleMessage: String,
    val isChecked: Boolean = false,
    val isCustom: Boolean = false,
    val orderIndex: Int = 0
)
