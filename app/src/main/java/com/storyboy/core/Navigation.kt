package com.storyboy.core

import android.content.Context
import android.content.Intent
import com.storyboy.MenuLauncherActivity
import com.storyboy.StoryEngineActivity

object Navigation {
    const val ExtraGameId = "com.storyboy.extra.GAME_ID"

    fun menuLauncherIntent(context: Context): Intent {
        return Intent(context, MenuLauncherActivity::class.java)
    }

    fun storyEngineIntent(context: Context, gameId: String): Intent {
        return Intent(context, StoryEngineActivity::class.java).putExtra(ExtraGameId, gameId)
    }
}
