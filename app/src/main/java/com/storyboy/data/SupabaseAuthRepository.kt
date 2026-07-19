package com.storyboy.data

import android.content.Context
import org.json.JSONObject

data class AuthSession(
    val userId: String,
    val email: String,
    val displayName: String,
)

class SupabaseAuthRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE,
    )

    fun currentSession(): AuthSession? {
        val userId = preferences.getString(KeyUserId, null) ?: return null
        return AuthSession(
            userId = userId,
            email = preferences.getString(KeyEmail, "").orEmpty(),
            displayName = preferences.getString(KeyDisplayName, "").orEmpty(),
        )
    }

    /**
     * Returns a non-expired access token, refreshing it first when needed.
     * Returns null when no one is signed in or the refresh fails.
     */
    fun validAccessToken(): String? {
        preferences.getString(KeyUserId, null) ?: return null
        val expiresAt = preferences.getLong(KeyExpiresAt, 0L)
        val accessToken = preferences.getString(KeyAccessToken, null)
        if (accessToken != null && expiresAt - RefreshMarginSeconds > nowSeconds()) {
            return accessToken
        }
        val refreshToken = preferences.getString(KeyRefreshToken, null) ?: return null
        return runCatching {
            val response = SupabaseApi.post(
                path = "/auth/v1/token?grant_type=refresh_token",
                body = JSONObject().put("refresh_token", refreshToken).toString(),
            )
            storeSession(JSONObject(response))
            preferences.getString(KeyAccessToken, null)
        }.getOrElse {
            if (it is SupabaseHttpException && it.statusCode in 400..499) signOut()
            null
        }
    }

    fun signIn(email: String, password: String): AuthSession {
        val response = SupabaseApi.post(
            path = "/auth/v1/token?grant_type=password",
            body = JSONObject().put("email", email).put("password", password).toString(),
        )
        storeSession(JSONObject(response))
        return currentSession() ?: error("Sign in did not return a session.")
    }

    /**
     * Creates an account. Returns the session when the project allows immediate
     * sign-in, or null when email confirmation is required first.
     */
    fun signUp(email: String, password: String, displayName: String): AuthSession? {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
        if (displayName.isNotBlank()) {
            body.put("data", JSONObject().put("display_name", displayName.trim()))
        }
        val response = JSONObject(SupabaseApi.post(path = "/auth/v1/signup", body = body.toString()))
        return if (response.has("access_token")) {
            storeSession(response)
            currentSession()
        } else {
            null
        }
    }

    fun signOut() {
        preferences.edit().clear().apply()
    }

    fun updateDisplayName(displayName: String) {
        val token = validAccessToken() ?: error("Not signed in.")
        SupabaseApi.put(
            path = "/auth/v1/user",
            body = JSONObject()
                .put("data", JSONObject().put("display_name", displayName.trim()))
                .toString(),
            accessToken = token,
        )
        val userId = preferences.getString(KeyUserId, null)
        if (userId != null) {
            runCatching {
                SupabaseApi.post(
                    path = "/rest/v1/profiles?on_conflict=id",
                    body = JSONObject()
                        .put("id", userId)
                        .put("display_name", displayName.trim())
                        .toString(),
                    accessToken = token,
                    prefer = "resolution=merge-duplicates,return=minimal",
                )
            }
        }
        preferences.edit().putString(KeyDisplayName, displayName.trim()).apply()
    }

    private fun storeSession(response: JSONObject) {
        val user = response.optJSONObject("user") ?: JSONObject()
        val metadata = user.optJSONObject("user_metadata") ?: JSONObject()
        val expiresIn = response.optLong("expires_in", 3600L)
        preferences.edit()
            .putString(KeyAccessToken, response.optString("access_token"))
            .putString(KeyRefreshToken, response.optString("refresh_token"))
            .putLong(KeyExpiresAt, nowSeconds() + expiresIn)
            .putString(KeyUserId, user.optString("id"))
            .putString(KeyEmail, user.optString("email"))
            .putString(
                KeyDisplayName,
                metadata.optString("display_name").ifBlank {
                    user.optString("email").substringBefore('@')
                },
            )
            .apply()
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000L

    companion object {
        private const val PreferencesName = "storyboy_supabase_auth"
        private const val KeyAccessToken = "access_token"
        private const val KeyRefreshToken = "refresh_token"
        private const val KeyExpiresAt = "expires_at"
        private const val KeyUserId = "user_id"
        private const val KeyEmail = "email"
        private const val KeyDisplayName = "display_name"
        private const val RefreshMarginSeconds = 60L
    }
}
