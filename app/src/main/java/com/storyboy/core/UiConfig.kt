package com.storyboy.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object UiConfig {
    data class ThemePalette(
        val Heading1Text: Color,
        val Heading2Text: Color,
        val BodyText: Color,
        val TopBarText: Color,
        val BackgroundCol: Color,
        val SurfaceCol: Color,
        val ElevatedSurfaceCol: Color,
        val MainDivider: Color,
        val SubDivider: Color,
        val FocusCol: Color,
        val AccentCol: Color,
        val ReaderPageCol: Color,
        val ReaderChoiceCol: Color,
        val ReaderText: Color,
        val ReaderMutedText: Color,
        val ReaderDivider: Color,
    )

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
        val ReaderLineHeight = 28.sp
    }

    object ThemeColors {
        val Heading1Text = Color(0xFFF5F5F5)
        val Heading2Text = Color(0xFFE2E2E2)
        val BodyText = Color(0xFFD8D8D8)
        val TopBarText = Color(0xFFF0F0F0)
        val BackgroundCol = Color(0xFF101010)
        val SurfaceCol = Color(0xFF1B1B1B)
        val ElevatedSurfaceCol = Color(0xFF242424)
        val MainDivider = Color(0xFF555555)
        val SubDivider = Color(0xFF333333)
        val FocusCol = Color(0xFF7FC7D9)
        val AccentCol = Color(0xFF7FC7D9)
        val ReaderPageCol = Color(0xFFF7F5EF)
        val ReaderChoiceCol = Color(0xFFE9E6DE)
        val ReaderText = Color(0xFF141414)
        val ReaderMutedText = Color(0xFF606060)
        val ReaderDivider = Color(0xFFCFCBC2)

        val Dark = ThemePalette(
            Heading1Text = Color(0xFFF5F5F5),
            Heading2Text = Color(0xFFE2E2E2),
            BodyText = Color(0xFFD8D8D8),
            TopBarText = Color(0xFFF0F0F0),
            BackgroundCol = Color(0xFF101010),
            SurfaceCol = Color(0xFF1B1B1B),
            ElevatedSurfaceCol = Color(0xFF242424),
            MainDivider = Color(0xFF555555),
            SubDivider = Color(0xFF333333),
            FocusCol = Color(0xFF7FC7D9),
            AccentCol = Color(0xFF7FC7D9),
            ReaderPageCol = Color(0xFF111111),
            ReaderChoiceCol = Color(0xFF242424),
            ReaderText = Color(0xFFF2F2F2),
            ReaderMutedText = Color(0xFFB8B8B8),
            ReaderDivider = Color(0xFF575757),
        )

        val Light = ThemePalette(
            Heading1Text = Color(0xFF111111),
            Heading2Text = Color(0xFF1F1F1F),
            BodyText = Color(0xFF191919),
            TopBarText = Color(0xFF111111),
            BackgroundCol = Color(0xFFF4F3EF),
            SurfaceCol = Color(0xFFE7E5DE),
            ElevatedSurfaceCol = Color(0xFFFCFBF7),
            MainDivider = Color(0xFFB8B4AA),
            SubDivider = Color(0xFFD0CCC2),
            FocusCol = Color(0xFF3D7280),
            AccentCol = Color(0xFF3D7280),
            ReaderPageCol = Color(0xFFF7F5EF),
            ReaderChoiceCol = Color(0xFFE1DED6),
            ReaderText = Color(0xFF111111),
            ReaderMutedText = Color(0xFF4F4F4F),
            ReaderDivider = Color(0xFFC7C2B8),
        )
    }

    object ImageSizes {
        val GamePosterListWidth = 72.dp
        val GamePosterListHeight = 104.dp
        val GameBannerListWidth = 148.dp
        val GameBannerListHeight = 84.dp
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
