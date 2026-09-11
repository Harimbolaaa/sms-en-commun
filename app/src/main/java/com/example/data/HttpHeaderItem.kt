package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Modèle de données représentant un en-tête HTTP personnalisé (Custom Header).
 * Permet la configuration de multiples en-têtes (Appwrite Project, Appwrite Key, Bearer Token, API Key, etc.)
 * avec support de l'activation/désactivation individuelle, ajout, modification et suppression.
 */
data class HttpHeaderItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val value: String,
    val isEnabled: Boolean = true,
    val description: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("value", value)
            put("isEnabled", isEnabled)
            put("description", description)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): HttpHeaderItem {
            return HttpHeaderItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", ""),
                value = json.optString("value", ""),
                isEnabled = json.optBoolean("isEnabled", true),
                description = json.optString("description", "")
            )
        }

        /**
         * Modèles d'en-têtes prédéfinis pour faciliter la configuration rapide
         */
        val PRESETS = listOf(
            HttpHeaderItem(
                id = "preset_appwrite_project",
                name = "X-Appwrite-Project",
                value = "6a21910a00047c04e5af",
                isEnabled = true,
                description = "Identifiant de projet Appwrite Cloud"
            ),
            HttpHeaderItem(
                id = "preset_appwrite_key",
                name = "X-Appwrite-Key",
                value = "",
                isEnabled = true,
                description = "Clé secrète API Appwrite (Droits complets)"
            ),
            HttpHeaderItem(
                id = "preset_appwrite_jwt",
                name = "X-Appwrite-JWT",
                value = "",
                isEnabled = true,
                description = "Token JWT utilisateur Appwrite"
            ),
            HttpHeaderItem(
                id = "preset_bearer",
                name = "Authorization",
                value = "Bearer ",
                isEnabled = true,
                description = "Authentification standard Bearer Token"
            ),
            HttpHeaderItem(
                id = "preset_api_key",
                name = "X-API-Key",
                value = "",
                isEnabled = true,
                description = "Clé d'API personnalisée"
            ),
            HttpHeaderItem(
                id = "preset_gateway_token",
                name = "X-SMS-Gateway-Token",
                value = "",
                isEnabled = true,
                description = "Token secret de sécurité passerelle SMS"
            )
        )
    }
}
