package com.storyboy.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit

object ThemeManager {
    private val LocalColors = staticCompositionLocalOf { UiConfig.ThemeColors.Dark }
    private val LocalFontScale = staticCompositionLocalOf { AppearanceSettingsRepository.DefaultFontScale }

    private val shapes = Shapes(
        small = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
        medium = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
        large = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
    )

    val colors: UiConfig.ThemePalette
        @Composable get() = LocalColors.current

    val fontScale: Float
        @Composable get() = LocalFontScale.current

    @Composable
    fun StoryBoyTheme(content: @Composable () -> Unit) {
        val settings = rememberAppearanceSettings().value
        val colors = if (settings.mode == AppearanceMode.Dark) {
            UiConfig.ThemeColors.Dark
        } else {
            UiConfig.ThemeColors.Light
        }
        val colorScheme = if (settings.mode == AppearanceMode.Dark) {
            darkColorScheme(
                primary = colors.AccentCol,
                onPrimary = colors.BackgroundCol,
                secondary = colors.FocusCol,
                onSecondary = colors.BackgroundCol,
                background = colors.BackgroundCol,
                onBackground = colors.BodyText,
                surface = colors.SurfaceCol,
                onSurface = colors.BodyText,
            )
        } else {
            lightColorScheme(
                primary = colors.AccentCol,
                onPrimary = colors.BackgroundCol,
                secondary = colors.FocusCol,
                onSecondary = colors.BackgroundCol,
                background = colors.BackgroundCol,
                onBackground = colors.BodyText,
                surface = colors.SurfaceCol,
                onSurface = colors.BodyText,
            )
        }

        CompositionLocalProvider(
            LocalColors provides colors,
            LocalFontScale provides settings.fontScale,
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typography(colors, settings.fontScale),
                shapes = shapes,
                content = content,
            )
        }
    }

    private fun typography(colors: UiConfig.ThemePalette, fontScale: Float): Typography {
        return Typography(
            displayMedium = TextStyle(
                fontFamily = UiConfig.Fonts.PrimaryFontFamily,
                fontSize = UiConfig.Fonts.MenuHeading.scaled(fontScale),
                color = colors.Heading1Text,
            ),
            headlineMedium = TextStyle(
                fontFamily = UiConfig.Fonts.PrimaryFontFamily,
                fontSize = UiConfig.Fonts.TextHeading.scaled(fontScale),
                color = colors.Heading2Text,
            ),
            bodyLarge = TextStyle(
                fontFamily = UiConfig.Fonts.PrimaryFontFamily,
                fontSize = UiConfig.Fonts.Text2.scaled(fontScale),
                color = colors.BodyText,
            ),
            bodyMedium = TextStyle(
                fontFamily = UiConfig.Fonts.PrimaryFontFamily,
                fontSize = UiConfig.Fonts.Text3.scaled(fontScale),
                color = colors.BodyText,
            ),
            labelLarge = TextStyle(
                fontFamily = UiConfig.Fonts.SecondaryFontFamily,
                fontSize = UiConfig.Fonts.Secondary2.scaled(fontScale),
                color = colors.TopBarText,
            ),
        )
    }

    private fun TextUnit.scaled(fontScale: Float): TextUnit {
        return this * fontScale
    }
}
