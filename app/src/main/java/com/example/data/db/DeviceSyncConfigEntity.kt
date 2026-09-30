package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room stockant la configuration de rôle de l'appareil (SOURCE vs PARTAGE).
 * Permet à un compte client d'être :
 * - SOURCE : Appareil principal qui centralise et visualise tous ses SMS et ceux de ses appareils partagés.
 * - PARTAGE : Appareil satellite qui transfère tous ses SMS interceptés vers le compte Source.
 * Persiste localement dans SQLite Room et survit aux redémarrages.
 */
@Entity(tableName = "device_sync_config")
data class DeviceSyncConfigEntity(
    @PrimaryKey
    val userPhone: String,
    val deviceRole: String = "SOURCE", // "SOURCE" ou "PARTAGE"
    val sourceAccountPhone: String = "",
    val isSource: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
