package com.storyboy.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

object ThemeManager {
    private val colorScheme = darkColorScheme(
        primary = UiConfig.ThemeColors.AccentCol,
        onPrimary = UiConfig.ThemeColors.BackgroundCol,
        secondary = UiConfig.ThemeColors.FocusCol,
        onSecondary = UiConfig.ThemeColors.BackgroundCol,
        background = UiConfig.ThemeColors.BackgroundCol,
        onBackground = UiConfig.ThemeColors.BodyText,
        surface = UiConfig.ThemeColors.SurfaceCol,
        onSurface = UiConfig.ThemeColors.BodyText,
    )

    private val typography = Typography(
        displayMedium = TextStyle(
            fontFamily = UiConfig.Fonts.PrimaryFontFamily,
            fontSize = UiConfig.Fonts.MenuHeading,
            color = UiConfig.ThemeColors.Heading1Text,
        ),
        headlineMedium = TextStyle(
            fontFamily = UiConfig.Fonts.PrimaryFontFamily,
            fontSize = UiConfig.Fonts.TextHeading,
            color = UiConfig.ThemeColors.Heading2Text,
        ),
        bodyLarge = TextStyle(
            fontFamily = UiConfig.Fonts.PrimaryFontFamily,
            fontSize = UiConfig.Fonts.Text2,
            color = UiConfig.ThemeColors.BodyText,
        ),
        bodyMedium = TextStyle(
            fontFamily = UiConfig.Fonts.PrimaryFontFamily,
            fontSize = UiConfig.Fonts.Text3,
            color = UiConfig.ThemeColors.BodyText,
        ),
        labelLarge = TextStyle(
            fontFamily = UiConfig.Fonts.SecondaryFontFamily,
            fontSize = UiConfig.Fonts.Secondary2,
            color = UiConfig.ThemeColors.TopBarText,
        ),
    )

    private val shapes = Shapes(
        small = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
        medium = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
        large = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
    )

    @Composable
    fun StoryBoyTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}
