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
import com.storyboy.core.Navigation
import com.storyboy.core.ThemeManager
import com.storyboy.core.UiConfig

class StoryEngineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameId = intent.getStringExtra(Navigation.ExtraGameId).orEmpty()
        val gamebookPath = intent.getStringExtra(Navigation.ExtraGamebookPath).orEmpty()

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
                        text = "Story engine ready: $gameId\n$gamebookPath",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
