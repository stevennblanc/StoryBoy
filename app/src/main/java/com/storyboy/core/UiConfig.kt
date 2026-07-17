package com.storyboy.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object UiConfig {
    object Fonts {
        val PrimaryFontFamily = FontFamily.SansSerif
        val SecondaryFontFamily = FontFamily.Serif

        val MenuHeading = 28.sp
        val TextHeading = 22.sp
        val Text2 = 18.sp
        val Text3 = 14.sp

        val Secondary1 = 20.sp
        val Secondary2 = 16.sp
        val Secondary3 = 12.sp
    }

    object ThemeColors {
        val Heading1Text = Color(0xFFEDEDED)
        val Heading2Text = Color(0xFFD6D6D6)
        val BodyText = Color(0xFFE8E2D2)
        val TopBarText = Color(0xFFF2F2F2)
        val BackgroundCol = Color(0xFF141414)
        val SurfaceCol = Color(0xFF202020)
        val MainDivider = Color(0xFF6C6657)
        val SubDivider = Color(0xFF3D3A33)
        val FocusCol = Color(0xFFD8C26A)
        val AccentCol = Color(0xFF8CCFC1)
    }

    object ImageSizes {
        val GamePosterListWidth = 72.dp
        val GamePosterListHeight = 104.dp
        val GamePosterGridWidth = 128.dp
        val GamePosterGridHeight = 184.dp
        val TitleScreenImageHeight = 220.dp
    }

    object Spacing {
        val ScreenPadding = 24.dp
        val ListBuffer = 12.dp
        val GridBuffer = 16.dp
        val SectionGap = 20.dp
        val ItemGap = 8.dp
    }

    object Controls {
        val ButtonRadius = 6.dp
        val FocusThickness = 2.dp
        val PosterCornerRadius = 4.dp
        val MinimumTouchTarget = 48.dp
    }

    object AnimationDurations {
        const val SplashInitialDelayMillis = 250L
        const val SplashCharacterDelayMillis = 115L
        const val SplashPauseMillis = 650L
        const val SplashFadeOutMillis = 550
    }

    object Scroll {
        val ThumbColor = ThemeColors.MainDivider
        val TrackColor = ThemeColors.SubDivider
    }
}
