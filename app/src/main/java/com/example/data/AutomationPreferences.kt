package com.example.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestionnaire réactif et persistant de la configuration de l'application via SharedPreferences.
 * Garantit la sauvegarde immédiate et la synchronisation en mémoire et sur disque de tous les réglages :
 * URL webhook, En-têtes HTTP personnalisés (Headers), règles Regex, filtrage d'expéditeurs, état du service d'arrière-plan.
 */
class AutomationPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "automation_bridge_prefs"
        private const val KEY_TARGET_URL = "key_target_url"
        private const val KEY_AUTO_FORWARD = "key_auto_forward"
        private const val KEY_KEEP_ALIVE = "key_keep_alive"
        
        // En-têtes HTTP personnalisés pour la sécurité du webhook (ex: X-SMS-Gateway-Token)
        private const val KEY_CUSTOM_HEADER_ENABLED = "key_custom_header_enabled"
        private const val KEY_CUSTOM_HEADER_NAME = "key_custom_header_name"
        private const val KEY_CUSTOM_HEADER_VALUE = "key_custom_header_value"
        private const val KEY_CUSTOM_HEADERS_JSON = "key_custom_headers_json"

        // Paramètres de filtrage par expéditeur
        private const val KEY_FILTER_SENDER_ENABLED = "key_filter_sender_enabled"
        private const val KEY_ALLOWED_SENDERS = "key_allowed_senders"
        
        // Paramètres d'extraction Regex
        private const val KEY_REGEX_ENABLED = "key_regex_enabled"
        private const val KEY_REGEX_PATTERN = "key_regex_pattern"
        private const val KEY_SEND_ONLY_EXTRACTED = "key_send_only_extracted"
        private const val KEY_ACTIVE_PRESET_ID = "key_active_preset_id"

        // Valeurs par défaut Appwrite
        const val DEFAULT_TARGET_URL = "https://cloud.appwrite.io/v1/databases/makafilma/collections/sms_logs/documents"
        const val DEFAULT_HEADER_NAME = "X-Appwrite-Project"
        const val DEFAULT_HEADER_VALUE = "6a21910a00047c04e5af"
        const val DEFAULT_ALLOWED_SENDERS = "YAS, TELMA, ORANGE, AIRTEL, MVOLA"
        const val DEFAULT_REGEX_PATTERN = "(?:recu|reçu|montant)\\s*:\\s*([0-9]+(?:\\s*[0-9]+)?\\s*Ar).*?(?:de\\s+)([A-Za-zÀ-ÿ\\-]+).*?((?:\\+261|0)3[23478][0-9]{7}).*?(?:Ref|ID)\\s*:\\s*([0-9]+)"
    }

    var targetUrl: String
        get() {
            val url = prefs.getString(KEY_TARGET_URL, DEFAULT_TARGET_URL)
            return if (url.isNullOrBlank()) DEFAULT_TARGET_URL else url
        }
        set(value) {
            val toSave = if (value.isBlank()) DEFAULT_TARGET_URL else value.trim()
            prefs.edit().putString(KEY_TARGET_URL, toSave).commit()
        }

    var isAutoForwardEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_FORWARD, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_FORWARD, value).commit()
        }

    var isKeepAliveEnabled: Boolean
        get() = prefs.getBoolean(KEY_KEEP_ALIVE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_KEEP_ALIVE, value).commit()
        }

    // --- En-têtes HTTP de sécurité (Custom Headers) ---
    var isCustomHeaderEnabled: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_HEADER_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CUSTOM_HEADER_ENABLED, value).commit()
        }

    var customHeaderName: String
        get() {
            val name = prefs.getString(KEY_CUSTOM_HEADER_NAME, DEFAULT_HEADER_NAME)
            return if (name.isNullOrBlank()) DEFAULT_HEADER_NAME else name
        }
        set(value) {
            val toSave = if (value.isBlank()) DEFAULT_HEADER_NAME else value.trim()
            prefs.edit().putString(KEY_CUSTOM_HEADER_NAME, toSave).commit()
        }

    var customHeaderValue: String
        get() {
            val v = prefs.getString(KEY_CUSTOM_HEADER_VALUE, DEFAULT_HEADER_VALUE)
            return if (v.isNullOrBlank()) DEFAULT_HEADER_VALUE else v
        }
        set(value) {
            val toSave = if (value.isBlank()) DEFAULT_HEADER_VALUE else value.trim()
            prefs.edit().putString(KEY_CUSTOM_HEADER_VALUE, toSave).commit()
        }

    /**
     * Récupère la liste complète des en-têtes HTTP personnalisés configurés.
     * Si aucune liste n'est encore enregistrée, migre le header existant par défaut.
     */
    fun getCustomHeaders(): List<HttpHeaderItem> {
        val jsonStr = prefs.getString(KEY_CUSTOM_HEADERS_JSON, null)
        if (jsonStr.isNullOrBlank()) {
            val legacyName = prefs.getString(KEY_CUSTOM_HEADER_NAME, DEFAULT_HEADER_NAME) ?: DEFAULT_HEADER_NAME
            val legacyValue = prefs.getString(KEY_CUSTOM_HEADER_VALUE, DEFAULT_HEADER_VALUE) ?: DEFAULT_HEADER_VALUE
            val legacyEnabled = prefs.getBoolean(KEY_CUSTOM_HEADER_ENABLED, true)
            return listOf(
                HttpHeaderItem(
                    id = "default_appwrite_project",
                    name = legacyName,
                    value = legacyValue,
                    isEnabled = legacyEnabled,
                    description = "En-tête principal"
                )
            )
        }
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<HttpHeaderItem>()
            for (i in 0 until array.length()) {
                list.add(HttpHeaderItem.fromJson(array.getJSONObject(i)))
            }
            if (list.isEmpty()) {
                listOf(HttpHeaderItem(id = "default_appwrite_project", name = DEFAULT_HEADER_NAME, value = DEFAULT_HEADER_VALUE, isEnabled = true))
            } else {
                list
            }
        } catch (_: Exception) {
            listOf(HttpHeaderItem(id = "default_appwrite_project", name = DEFAULT_HEADER_NAME, value = DEFAULT_HEADER_VALUE, isEnabled = true))
        }
    }

    /**
     * Sauvegarde la liste complète des en-têtes HTTP personnalisés et synchronise le header principal de secours.
     */
    fun saveCustomHeaders(headers: List<HttpHeaderItem>) {
        val array = org.json.JSONArray()
        headers.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_CUSTOM_HEADERS_JSON, array.toString()).commit()

        val firstEnabled = headers.firstOrNull { it.isEnabled && it.name.isNotBlank() }
        if (firstEnabled != null) {
            prefs.edit()
                .putBoolean(KEY_CUSTOM_HEADER_ENABLED, true)
                .putString(KEY_CUSTOM_HEADER_NAME, firstEnabled.name)
                .putString(KEY_CUSTOM_HEADER_VALUE, firstEnabled.value)
                .commit()
        } else {
            prefs.edit().putBoolean(KEY_CUSTOM_HEADER_ENABLED, false).commit()
        }
    }

    /**
     * Retourne la Map de tous les en-têtes actuellement actifs (cochés et non-vides).
     */
    fun getActiveHeadersMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        getCustomHeaders().filter { it.isEnabled && it.name.isNotBlank() }.forEach {
            map[it.name.trim()] = it.value.trim()
        }
        return map
    }

    // --- Gestion du filtrage d'expéditeur ---
    var isSenderFilterEnabled: Boolean
        get() = prefs.getBoolean(KEY_FILTER_SENDER_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_FILTER_SENDER_ENABLED, value).commit()
        }

    var allowedSenders: String
        get() {
            val s = prefs.getString(KEY_ALLOWED_SENDERS, DEFAULT_ALLOWED_SENDERS)
            return if (s.isNullOrBlank()) DEFAULT_ALLOWED_SENDERS else s
        }
        set(value) {
            val toSave = if (value.isBlank()) DEFAULT_ALLOWED_SENDERS else value.trim()
            prefs.edit().putString(KEY_ALLOWED_SENDERS, toSave).commit()
        }

    // --- Gestion de l'extraction Regex ---
    var isRegexEnabled: Boolean
        get() = prefs.getBoolean(KEY_REGEX_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_REGEX_ENABLED, value).commit()
        }

    var regexPattern: String
        get() = prefs.getString(KEY_REGEX_PATTERN, DEFAULT_REGEX_PATTERN) ?: DEFAULT_REGEX_PATTERN
        set(value) {
            prefs.edit().putString(KEY_REGEX_PATTERN, value).commit()
        }

    var sendOnlyExtracted: Boolean
        get() = prefs.getBoolean(KEY_SEND_ONLY_EXTRACTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SEND_ONLY_EXTRACTED, value).commit()
        }

    var activePresetId: String
        get() = prefs.getString(KEY_ACTIVE_PRESET_ID, "multi_amount_otp") ?: "multi_amount_otp"
        set(value) {
            prefs.edit().putString(KEY_ACTIVE_PRESET_ID, value).commit()
        }
}
