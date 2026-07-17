package com.storyboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import com.storyboy.core.AppConfig
import com.storyboy.core.Navigation
import com.storyboy.core.ThemeManager
import com.storyboy.core.UiConfig
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ThemeManager.StoryBoyTheme {
                StoryBoySplash(
                    onComplete = {
                        startActivity(Navigation.menuLauncherIntent(this))
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun StoryBoySplash(onComplete: () -> Unit) {
    var visibleText by remember { mutableStateOf("") }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(UiConfig.AnimationDurations.SplashInitialDelayMillis)

        AppConfig.AppName.forEachIndexed { index, _ ->
            visibleText = AppConfig.AppName.take(index + 1)
            delay(UiConfig.AnimationDurations.SplashCharacterDelayMillis)
        }

        delay(UiConfig.AnimationDurations.SplashPauseMillis)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(UiConfig.AnimationDurations.SplashFadeOutMillis),
        )
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeManager.colors.BackgroundCol)
            .safeDrawingPadding()
            .alpha(alpha.value),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = visibleText,
            style = TextStyle(
                color = ThemeManager.colors.BodyText,
                fontFamily = UiConfig.Fonts.PrimaryFontFamily,
                fontSize = UiConfig.Fonts.MenuHeading * ThemeManager.fontScale,
            ),
        )
    }
}
