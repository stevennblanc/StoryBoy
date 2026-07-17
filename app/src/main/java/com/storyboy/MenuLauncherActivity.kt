package com.storyboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.storyboy.core.Navigation
import com.storyboy.core.ThemeManager
import com.storyboy.launcher.LauncherScreen
import com.storyboy.launcher.LauncherViewModel

class MenuLauncherActivity : ComponentActivity() {
    private val launcherViewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ThemeManager.StoryBoyTheme {
                LauncherScreen(
                    launcherViewModel = launcherViewModel,
                    onOpenSettings = {
                        startActivity(Navigation.menuSettingsIntent(this))
                    },
                    onOpenGamebook = { gamebook ->
                        startActivity(
                            Navigation.storyEngineIntent(
                                context = this,
                                gameId = gamebook.metadata.id,
                                gamebookPath = gamebook.filePath,
                            ),
                        )
                    },
                )
            }
        }
    }
}
