package com.storyboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.storyboy.core.ThemeManager
import com.storyboy.core.UiConfig

class MenuSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ThemeManager.StoryBoyTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(UiConfig.ThemeColors.BackgroundCol)
                        .padding(UiConfig.Spacing.ScreenPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Settings ready",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
