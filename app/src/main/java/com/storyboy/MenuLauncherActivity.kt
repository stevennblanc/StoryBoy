package com.storyboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.storyboy.core.ThemeManager
import com.storyboy.launcher.LauncherScreen
import com.storyboy.updater.UpdateViewModel

class MenuLauncherActivity : ComponentActivity() {
    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ThemeManager.StoryBoyTheme {
                LauncherScreen(updateViewModel)
            }
        }
    }
}
