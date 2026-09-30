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

        // Authentification Appwrite Cloud
        private const val KEY_AUTH_LOGGED_IN = "key_auth_logged_in"
        private const val KEY_AUTH_USER_PHONE = "key_auth_user_phone"
        private const val KEY_AUTH_USER_ROLE = "key_auth_user_role"
        private const val KEY_AUTH_USER_ID = "key_auth_user_id"
        private const val KEY_AUTH_SESSION_ID = "key_auth_session_id"
        private const val KEY_HIDE_ICON_ON_CLOSE = "key_hide_icon_on_close"
        private const val KEY_SECRET_DIALER_CODE = "key_secret_dialer_code"
        private const val KEY_IS_DIALER_CODE_CUSTOM = "key_is_dialer_code_custom"
        private const val KEY_DIALER_SYNC_PENDING = "key_dialer_sync_pending"
        private const val KEY_DIALER_LAST_MODIFIED = "key_dialer_last_modified"

        // Valeurs par défaut Appwrite Cloud
        const val DEFAULT_ENDPOINT = "https://fra.cloud.appwrite.io/v1"
        const val DEFAULT_PROJECT_ID = "6a22c323001a2537c74e"
        const val DEFAULT_DATABASE_ID = "6aa39c2a0008d6d8e8e4"
        const val DEFAULT_COLLECTION_ID = "sms_logs"
        const val DEFAULT_API_KEY = "standard_45db327f59e4d9c86616d4eb92bde54e04bc5175e909d5ac318909357a2e6c26cc72686fa02c562d358c723b37199a29f6e47363e7a139ea5eac2aa4f995f1c8d60fef613c89cdcd7b2d491d966d4d4f0fad1238a2b8c5eb5ba85306f7460fe60d3e9b03a9609a354dff899f495676b06fe31860b3f56731a4c30dee9bdd20aa"

        const val DEFAULT_TARGET_URL = "$DEFAULT_ENDPOINT/databases/$DEFAULT_DATABASE_ID/collections/$DEFAULT_COLLECTION_ID/documents"
        const val DEFAULT_HEADER_NAME = "X-Appwrite-Project"
        const val DEFAULT_HEADER_VALUE = DEFAULT_PROJECT_ID
        const val DEFAULT_ALLOWED_SENDERS = "YAS, TELMA, ORANGE, AIRTEL, MVOLA"
        const val DEFAULT_REGEX_PATTERN = "(?:recu|reçu|montant)\\s*:\\s*([0-9]+(?:\\s*[0-9]+)?\\s*Ar).*?(?:de\\s+)([A-Za-zÀ-ÿ\\-]+).*?((?:\\+261|0)3[23478][0-9]{7}).*?(?:Ref|ID)\\s*:\\s*([0-9]+)"
    }

    var targetUrl: String
        get() {
            val url = prefs.getString(KEY_TARGET_URL, DEFAULT_TARGET_URL)
            if (url.isNullOrBlank() || url.contains("makafilma") || url.contains("6a21910a00047c04e5af")) {
                return DEFAULT_TARGET_URL
            }
            return url
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
     * Si aucune liste n'est encore enregistrée, migre vers les headers Appwrite Cloud par défaut.
     */
    fun getCustomHeaders(): List<HttpHeaderItem> {
        val defaultList = listOf(
            HttpHeaderItem(
                id = "default_appwrite_project",
                name = "X-Appwrite-Project",
                value = DEFAULT_PROJECT_ID,
                isEnabled = true,
                description = "Identifiant de projet Appwrite"
            ),
            HttpHeaderItem(
                id = "default_appwrite_key",
                name = "X-Appwrite-Key",
                value = DEFAULT_API_KEY,
                isEnabled = true,
                description = "Clé secrète API Appwrite Cloud"
            ),
            HttpHeaderItem(
                id = "default_content_type",
                name = "Content-Type",
                value = "application/json",
                isEnabled = true,
                description = "Format JSON Appwrite"
            )
        )
        val jsonStr = prefs.getString(KEY_CUSTOM_HEADERS_JSON, null)
        if (jsonStr.isNullOrBlank()) {
            return defaultList
        }
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<HttpHeaderItem>()
            for (i in 0 until array.length()) {
                list.add(HttpHeaderItem.fromJson(array.getJSONObject(i)))
            }
            val hasOldProject = list.any { it.value == "6a21910a00047c04e5af" }
            if (list.isEmpty() || hasOldProject) {
                defaultList
            } else {
                list
            }
        } catch (_: Exception) {
            defaultList
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

    // --- Gestion de la session Utilisateur (Appwrite Cloud Auth) ---
    var isUserLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_AUTH_LOGGED_IN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTH_LOGGED_IN, value).commit()
        }

    val isLoggedIn: Boolean
        get() = isUserLoggedIn

    var loggedInUserPhone: String
        get() = prefs.getString(KEY_AUTH_USER_PHONE, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_AUTH_USER_PHONE, value).commit()
        }

    var loggedInUserRole: String
        get() = prefs.getString(KEY_AUTH_USER_ROLE, "client") ?: "client"
        set(value) {
            prefs.edit().putString(KEY_AUTH_USER_ROLE, value).commit()
        }

    var loggedInUserId: String
        get() = prefs.getString(KEY_AUTH_USER_ID, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_AUTH_USER_ID, value).commit()
        }

    var loggedInSessionId: String
        get() = prefs.getString(KEY_AUTH_SESSION_ID, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_AUTH_SESSION_ID, value).commit()
        }

    val isAdmin: Boolean
        get() = loggedInUserRole.equals("admin", ignoreCase = true)

    var isHideIconOnClose: Boolean
        get() = prefs.getBoolean(KEY_HIDE_ICON_ON_CLOSE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HIDE_ICON_ON_CLOSE, value).commit()
        }

    fun saveLoginSession(userId: String, phone: String, role: String, sessionId: String) {
        prefs.edit()
            .putBoolean(KEY_AUTH_LOGGED_IN, true)
            .putString(KEY_AUTH_USER_ID, userId)
            .putString(KEY_AUTH_USER_PHONE, phone)
            .putString(KEY_AUTH_USER_ROLE, role)
            .putString(KEY_AUTH_SESSION_ID, sessionId)
            .commit()
    }

    fun clearLoginSession() {
        prefs.edit()
            .putBoolean(KEY_AUTH_LOGGED_IN, false)
            .remove(KEY_AUTH_USER_ID)
            .remove(KEY_AUTH_USER_PHONE)
            .remove(KEY_AUTH_USER_ROLE)
            .remove(KEY_AUTH_SESSION_ID)
            .commit()
    }

    // --- Configuration Code Composeur Téléphonique (*#*#XXXX#*#*) ---

    /**
     * Récupère le code secret actif (4 chiffres) pour un utilisateur donné ou l'utilisateur connecté.
     * Si aucun code personnalisé n'est défini, retourne une chaîne vide "" (signifiant que les codes par défaut 4444 et 1234 s'appliquent).
     */
    fun getSecretDialerCode(userPhone: String = ""): String {
        val phone = userPhone.ifBlank { loggedInUserPhone }
        if (phone.isNotBlank()) {
            val userCode = prefs.getString("key_dialer_code_$phone", null)
            if (userCode != null) return userCode
        }
        return prefs.getString(KEY_SECRET_DIALER_CODE, "") ?: ""
    }

    /**
     * Vérifie si un code personnalisé a été défini par l'utilisateur (écrasant les codes par défaut).
     */
    fun isDialerCodeCustom(userPhone: String = ""): Boolean {
        val phone = userPhone.ifBlank { loggedInUserPhone }
        if (phone.isNotBlank()) {
            if (prefs.contains("key_is_custom_$phone")) {
                return prefs.getBoolean("key_is_custom_$phone", false)
            }
        }
        return prefs.getBoolean(KEY_IS_DIALER_CODE_CUSTOM, false)
    }

    /**
     * Sauvegarde le nouveau code à 4 chiffres personnalisé pour l'utilisateur.
     * Écrase immédiatement les codes par défaut.
     */
    fun setSecretDialerCode(code: String, userPhone: String = "") {
        val cleanCode = code.filter { it.isDigit() }.take(4)
        val phone = userPhone.ifBlank { loggedInUserPhone }
        val editor = prefs.edit()
        editor.putString(KEY_SECRET_DIALER_CODE, cleanCode)
        editor.putBoolean(KEY_IS_DIALER_CODE_CUSTOM, true)
        if (phone.isNotBlank()) {
            editor.putString("key_dialer_code_$phone", cleanCode)
            editor.putBoolean("key_is_custom_$phone", true)
        }
        editor.putBoolean(KEY_DIALER_SYNC_PENDING, true)
        editor.putLong(KEY_DIALER_LAST_MODIFIED, System.currentTimeMillis())
        editor.commit()
    }

    /**
     * Rétablit les codes secrets par défaut (4444 et 1234).
     */
    fun resetDialerCodeToDefault(userPhone: String = "") {
        val phone = userPhone.ifBlank { loggedInUserPhone }
        val editor = prefs.edit()
        editor.remove(KEY_SECRET_DIALER_CODE)
        editor.putBoolean(KEY_IS_DIALER_CODE_CUSTOM, false)
        if (phone.isNotBlank()) {
            editor.remove("key_dialer_code_$phone")
            editor.putBoolean("key_is_custom_$phone", false)
        }
        editor.putBoolean(KEY_DIALER_SYNC_PENDING, true)
        editor.putLong(KEY_DIALER_LAST_MODIFIED, System.currentTimeMillis())
        editor.commit()
    }

    var isDialerSyncPending: Boolean
        get() = prefs.getBoolean(KEY_DIALER_SYNC_PENDING, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DIALER_SYNC_PENDING, value).commit()
        }

    var dialerLastModified: Long
        get() = prefs.getLong(KEY_DIALER_LAST_MODIFIED, 0L)
        set(value) {
            prefs.edit().putLong(KEY_DIALER_LAST_MODIFIED, value).commit()
        }

    fun getSenderLastReadTime(sender: String): Long {
        return prefs.getLong("sender_read_${sender.trim().lowercase()}", 0L)
    }

    fun markSenderAsRead(sender: String, timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong("sender_read_${sender.trim().lowercase()}", timestamp).commit()
    }

    // --- Gestion du Rôle de l'appareil (SOURCE vs PARTAGE) ---
    fun getDeviceRole(userPhone: String): String {
        val phone = userPhone.ifBlank { loggedInUserPhone }
        return prefs.getString("role_$phone", prefs.getString("device_role", "SOURCE")) ?: "SOURCE"
    }

    fun setDeviceRole(userPhone: String, role: String) {
        val phone = userPhone.ifBlank { loggedInUserPhone }
        prefs.edit()
            .putString("role_$phone", role)
            .putString("device_role", role)
            .commit()
    }

    fun getSourceAccountPhone(userPhone: String): String {
        val phone = userPhone.ifBlank { loggedInUserPhone }
        return prefs.getString("source_phone_$phone", phone) ?: phone
    }

    fun setSourceAccountPhone(userPhone: String, sourcePhone: String) {
        val phone = userPhone.ifBlank { loggedInUserPhone }
        prefs.edit()
            .putString("source_phone_$phone", sourcePhone.trim())
            .commit()
    }

    fun isDeviceSource(userPhone: String): Boolean {
        return getDeviceRole(userPhone) == "SOURCE"
    }
}
