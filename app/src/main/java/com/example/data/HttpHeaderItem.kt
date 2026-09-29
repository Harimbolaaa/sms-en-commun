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
                value = "6a22c323001a2537c74e",
                isEnabled = true,
                description = "Identifiant de projet Appwrite Cloud"
            ),
            HttpHeaderItem(
                id = "preset_appwrite_key",
                name = "X-Appwrite-Key",
                value = "standard_45db327f59e4d9c86616d4eb92bde54e04bc5175e909d5ac318909357a2e6c26cc72686fa02c562d358c723b37199a29f6e47363e7a139ea5eac2aa4f995f1c8d60fef613c89cdcd7b2d491d966d4d4f0fad1238a2b8c5eb5ba85306f7460fe60d3e9b03a9609a354dff899f495676b06fe31860b3f56731a4c30dee9bdd20aa",
                isEnabled = true,
                description = "Clé secrète API Appwrite (Droits complets)"
            ),
            HttpHeaderItem(
                id = "preset_content_type",
                name = "Content-Type",
                value = "application/json",
                isEnabled = true,
                description = "Type de contenu JSON"
            ),
            HttpHeaderItem(
                id = "preset_appwrite_jwt",
                name = "X-Appwrite-JWT",
                value = "",
                isEnabled = false,
                description = "Token JWT utilisateur Appwrite"
            ),
            HttpHeaderItem(
                id = "preset_bearer",
                name = "Authorization",
                value = "Bearer ",
                isEnabled = false,
                description = "Authentification standard Bearer Token"
            ),
            HttpHeaderItem(
                id = "preset_api_key",
                name = "X-API-Key",
                value = "",
                isEnabled = false,
                description = "Clé d'API personnalisée"
            ),
            HttpHeaderItem(
                id = "preset_gateway_token",
                name = "X-SMS-Gateway-Token",
                value = "",
                isEnabled = false,
                description = "Token secret de sécurité passerelle SMS"
            )
        )
    }
}
