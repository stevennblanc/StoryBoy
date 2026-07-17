package com.storyboy.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

enum class AppearanceMode {
    Light,
    Dark,
}

data class AppearanceSettings(
    val mode: AppearanceMode = AppearanceMode.Dark,
    val fontScale: Float = 1f,
)

class AppearanceSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE,
    )

    fun load(): AppearanceSettings {
        val mode = runCatching {
            AppearanceMode.valueOf(preferences.getString(KeyMode, AppearanceMode.Dark.name) ?: AppearanceMode.Dark.name)
        }.getOrDefault(AppearanceMode.Dark)

        return AppearanceSettings(
            mode = mode,
            fontScale = preferences.getFloat(KeyFontScale, DefaultFontScale).coerceIn(MinFontScale, MaxFontScale),
        )
    }

    fun setMode(mode: AppearanceMode) {
        preferences.edit().putString(KeyMode, mode.name).apply()
    }

    fun setFontScale(fontScale: Float) {
        preferences.edit().putFloat(KeyFontScale, fontScale.coerceIn(MinFontScale, MaxFontScale)).apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val MinFontScale = 0.85f
        const val MaxFontScale = 1.35f
        const val DefaultFontScale = 1f

        private const val PreferencesName = "storyboy_appearance"
        private const val KeyMode = "mode"
        private const val KeyFontScale = "font_scale"
    }
}

@Composable
fun rememberAppearanceSettings(): State<AppearanceSettings> {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { AppearanceSettingsRepository(context) }
    val settings = remember { mutableStateOf(repository.load()) }

    DisposableEffect(repository) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            settings.value = repository.load()
        }
        repository.registerListener(listener)
        onDispose {
            repository.unregisterListener(listener)
        }
    }

    return settings
}
