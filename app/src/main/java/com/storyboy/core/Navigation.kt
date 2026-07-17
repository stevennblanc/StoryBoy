package com.storyboy.core

import android.content.Context
import android.content.Intent
import com.storyboy.MenuLauncherActivity
import com.storyboy.MenuSettingsActivity
import com.storyboy.StoryEngineActivity

object Navigation {
    const val ExtraGameId = "com.storyboy.extra.GAME_ID"
    const val ExtraGamebookPath = "com.storyboy.extra.GAMEBOOK_PATH"

    fun menuLauncherIntent(context: Context): Intent {
        return Intent(context, MenuLauncherActivity::class.java)
    }

    fun menuSettingsIntent(context: Context): Intent {
        return Intent(context, MenuSettingsActivity::class.java)
    }

    fun storyEngineIntent(context: Context, gameId: String, gamebookPath: String): Intent {
        return Intent(context, StoryEngineActivity::class.java)
            .putExtra(ExtraGameId, gameId)
            .putExtra(ExtraGamebookPath, gamebookPath)
    }
}
