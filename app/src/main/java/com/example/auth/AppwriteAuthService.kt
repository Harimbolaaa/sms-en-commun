package com.example.auth

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.AutomationLogRepository
import com.example.data.AutomationPreferences
import com.example.data.LogType
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AppwriteUserSummary(
    val id: String,
    val phone: String,
    val fakeEmail: String,
    val name: String,
    val role: String,
    val status: Boolean,
    val createdAt: String
)

/**
 * Service d'authentification Appwrite Cloud basé sur Téléphone + Mot de passe.
 * Les numéros de téléphone sont automatiquement mappés en faux emails valides pour Appwrite :
 * Ex: "0444444444" -> "0444444444@appwrite.local"
 */
object AppwriteAuthService {

    private const val TAG = "AppwriteAuth"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun postToMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    /**
     * Convertit un numéro de téléphone en faux email requis par Appwrite Cloud
     */
    fun phoneToFakeEmail(phone: String): String {
        val cleanDigits = phone.filter { it.isDigit() }
        val effectivePhone = if (cleanDigits.isNotBlank()) cleanDigits else phone.trim()
        return "$effectivePhone@appwrite.local"
    }

    /**
     * Récupère le numéro de téléphone original à partir du faux email Appwrite
     */
    fun fakeEmailToPhone(email: String): String {
        return if (email.endsWith("@appwrite.local")) {
            email.substringBefore("@appwrite.local")
        } else {
            email
        }
    }

    /**
     * Authentifie un utilisateur avec son Téléphone et Mot de passe auprès d'Appwrite Cloud.
     */
    fun login(
        context: Context,
        phone: String,
        password: String,
        onResult: (success: Boolean, errorMessage: String?, role: String?) -> Unit
    ) {
        val prefs = AutomationPreferences(context)
        val cleanPhone = phone.trim()
        val fakeEmail = phoneToFakeEmail(cleanPhone)
        val endpoint = AutomationPreferences.DEFAULT_ENDPOINT
        val projectId = AutomationPreferences.DEFAULT_PROJECT_ID
        val apiKey = AutomationPreferences.DEFAULT_API_KEY

        val loginUrl = "$endpoint/account/sessions/email"

        val bodyJson = JSONObject().apply {
            put("email", fakeEmail)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url(loginUrl)
            .header("X-Appwrite-Project", projectId)
            .header("Content-Type", "application/json")
            .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Erreur réseau login: ${e.message}")
                postToMain {
                    onResult(false, "Impossible de joindre le serveur Appwrite : ${e.localizedMessage}", null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                val code = response.code

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val json = JSONObject(responseBody)
                        json.optString("message", "Identifiants invalides ou compte inexistant (HTTP $code)")
                    } catch (_: Exception) {
                        "Erreur d'authentification Appwrite (HTTP $code)"
                    }
                    postToMain {
                        onResult(false, errorMsg, null)
                    }
                    return
                }

                // Succès 200/201 : Session créée
                try {
                    val sessionJson = JSONObject(responseBody)
                    val sessionId = sessionJson.optString("\$id", "")
                    val userId = sessionJson.optString("userId", "")

                    // Récupération des détails de l'utilisateur (rôle/labels) via l'API Users Admin
                    fetchUserRole(endpoint, projectId, apiKey, userId, cleanPhone) { role ->
                        prefs.saveLoginSession(
                            userId = userId,
                            phone = cleanPhone,
                            role = role,
                            sessionId = sessionId
                        )

                        AutomationLogRepository.log(
                            tag = "AUTH_LOGIN",
                            message = "Connexion réussie : $cleanPhone ($role)",
                            type = LogType.INFO,
                            details = "Session: $sessionId | User: $userId",
                            source = cleanPhone
                        )

                        postToMain {
                            onResult(true, null, role)
                        }
                    }
                } catch (e: Exception) {
                    postToMain {
                        onResult(false, "Erreur de traitement de session: ${e.localizedMessage}", null)
                    }
                }
            }
        })
    }

    /**
     * Récupère le rôle (labels) d'un utilisateur
     */
    private fun fetchUserRole(
        endpoint: String,
        projectId: String,
        apiKey: String,
        userId: String,
        phone: String,
        onRoleFound: (String) -> Unit
    ) {
        // Si le numéro est le super-admin par défaut configuré
        if (phone.contains("0444444444")) {
            onRoleFound("admin")
            return
        }

        val userUrl = "$endpoint/users/$userId"
        val request = Request.Builder()
            .url(userUrl)
            .header("X-Appwrite-Project", projectId)
            .header("X-Appwrite-Key", apiKey)
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onRoleFound("client")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val labels = json.optJSONArray("labels") ?: JSONArray()
                        var role = "client"
                        for (i in 0 until labels.length()) {
                            if (labels.getString(i).equals("admin", ignoreCase = true)) {
                                role = "admin"
                                break
                            }
                        }
                        onRoleFound(role)
                        return
                    } catch (_: Exception) {}
                }
                onRoleFound("client")
            }
        })
    }

    /**
     * Déconnecte l'utilisateur actuel
     */
    fun logout(context: Context, onComplete: () -> Unit) {
        val prefs = AutomationPreferences(context)
        val phone = prefs.loggedInUserPhone
        val sessionId = prefs.loggedInSessionId
        val endpoint = AutomationPreferences.DEFAULT_ENDPOINT
        val projectId = AutomationPreferences.DEFAULT_PROJECT_ID

        // Optionnel : Suppression de la session sur Appwrite
        if (sessionId.isNotBlank()) {
            val deleteSessionUrl = "$endpoint/account/sessions/$sessionId"
            val request = Request.Builder()
                .url(deleteSessionUrl)
                .header("X-Appwrite-Project", projectId)
                .delete()
                .build()
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {}
            })
        }

        AutomationLogRepository.log(
            tag = "AUTH_LOGOUT",
            message = "Déconnexion de l'utilisateur $phone",
            type = LogType.INFO,
            source = phone
        )

        prefs.clearLoginSession()
        postToMain {
            onComplete()
        }
    }

    /**
     * Création d'un nouvel utilisateur client par l'administrateur
     */
    fun createClientUser(
        phone: String,
        password: String,
        name: String,
        role: String = "client",
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        val cleanPhone = phone.trim()
        val fakeEmail = phoneToFakeEmail(cleanPhone)
        val endpoint = AutomationPreferences.DEFAULT_ENDPOINT
        val projectId = AutomationPreferences.DEFAULT_PROJECT_ID
        val apiKey = AutomationPreferences.DEFAULT_API_KEY

        val createUrl = "$endpoint/users"
        val bodyJson = JSONObject().apply {
            put("userId", "unique()")
            put("email", fakeEmail)
            put("password", password)
            put("name", name.ifBlank { "Client ($cleanPhone)" })
        }.toString()

        val request = Request.Builder()
            .url(createUrl)
            .header("X-Appwrite-Project", projectId)
            .header("X-Appwrite-Key", apiKey)
            .header("Content-Type", "application/json")
            .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postToMain {
                    onResult(false, "Erreur réseau: ${e.localizedMessage}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val j = JSONObject(responseBody)
                        j.optString("message", "Erreur lors de la création (${response.code})")
                    } catch (_: Exception) {
                        "Erreur Appwrite (${response.code})"
                    }
                    postToMain {
                        onResult(false, errorMsg)
                    }
                    return
                }

                // Si rôle non vide, affecter le label
                try {
                    val userJson = JSONObject(responseBody)
                    val newUserId = userJson.getString("\$id")

                    val updateLabelsUrl = "$endpoint/users/$newUserId/labels"
                    val labelsBody = JSONObject().apply {
                        put("labels", JSONArray().put(role))
                    }.toString()

                    val labelReq = Request.Builder()
                        .url(updateLabelsUrl)
                        .header("X-Appwrite-Project", projectId)
                        .header("X-Appwrite-Key", apiKey)
                        .header("Content-Type", "application/json")
                        .put(labelsBody.toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    httpClient.newCall(labelReq).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            postToMain { onResult(true, null) }
                        }
                        override fun onResponse(call: Call, response: Response) {
                            postToMain { onResult(true, null) }
                        }
                    })
                } catch (e: Exception) {
                    postToMain { onResult(true, null) }
                }
            }
        })
    }

    /**
     * Liste des utilisateurs existants dans Appwrite Cloud (Réservé admin)
     */
    fun listUsers(onResult: (List<AppwriteUserSummary>) -> Unit) {
        val endpoint = AutomationPreferences.DEFAULT_ENDPOINT
        val projectId = AutomationPreferences.DEFAULT_PROJECT_ID
        val apiKey = AutomationPreferences.DEFAULT_API_KEY

        val listUrl = "$endpoint/users"
        val request = Request.Builder()
            .url(listUrl)
            .header("X-Appwrite-Project", projectId)
            .header("X-Appwrite-Key", apiKey)
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postToMain { onResult(emptyList()) }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    postToMain { onResult(emptyList()) }
                    return
                }
                try {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val usersArray = json.optJSONArray("users") ?: JSONArray()
                    val result = mutableListOf<AppwriteUserSummary>()

                    for (i in 0 until usersArray.length()) {
                        val u = usersArray.getJSONObject(i)
                        val email = u.optString("email", "")
                        val labelsArray = u.optJSONArray("labels") ?: JSONArray()
                        val labelsList = mutableListOf<String>()
                        for (j in 0 until labelsArray.length()) {
                            labelsList.add(labelsArray.getString(j))
                        }
                        val role = if (labelsList.any { it.equals("admin", ignoreCase = true) }) "admin" else "client"

                        result.add(
                            AppwriteUserSummary(
                                id = u.optString("\$id", ""),
                                phone = fakeEmailToPhone(email),
                                fakeEmail = email,
                                name = u.optString("name", ""),
                                role = role,
                                status = u.optBoolean("status", true),
                                createdAt = u.optString("\$createdAt", "")
                            )
                        )
                    }
                    postToMain { onResult(result) }
                } catch (_: Exception) {
                    postToMain { onResult(emptyList()) }
                }
            }
        })
    }
}
