package com.storyboy.core

import android.content.Context

data class UserProfile(
    val displayName: String = "",
    val email: String = "",
    val accountId: String = "",
)

class UserProfileRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE,
    )

    fun load(): UserProfile {
        return UserProfile(
            displayName = preferences.getString(KeyDisplayName, "").orEmpty(),
            email = preferences.getString(KeyEmail, "").orEmpty(),
            accountId = preferences.getString(KeyAccountId, "").orEmpty(),
        )
    }

    fun save(profile: UserProfile) {
        preferences.edit()
            .putString(KeyDisplayName, profile.displayName.trim())
            .putString(KeyEmail, profile.email.trim())
            .putString(KeyAccountId, profile.accountId.trim())
            .apply()
    }

    companion object {
        private const val PreferencesName = "storyboy_profile"
        private const val KeyDisplayName = "display_name"
        private const val KeyEmail = "email"
        private const val KeyAccountId = "account_id"
    }
}
