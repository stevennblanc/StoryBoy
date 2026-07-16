package com.storyboy.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.storyboy.core.AppConfig
import com.storyboy.core.UiConfig
import com.storyboy.updater.UpdateStatus
import com.storyboy.updater.UpdateViewModel

@Composable
fun LauncherScreen(updateViewModel: UpdateViewModel) {
    val updateStatus by updateViewModel.status.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UiConfig.ThemeColors.BackgroundCol)
            .padding(UiConfig.Spacing.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
    ) {
        Text(
            text = AppConfig.AppName,
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = "Launcher ready",
            style = MaterialTheme.typography.bodyLarge,
        )
        UpdatePanel(
            status = updateStatus,
            onCheck = updateViewModel::checkForUpdates,
            onDownload = updateViewModel::downloadAndInstall,
            onInstall = updateViewModel::installPendingUpdate,
        )
    }
}

@Composable
private fun UpdatePanel(
    status: UpdateStatus,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    when (status) {
        UpdateStatus.Idle -> Button(onClick = onCheck) {
            Text("Check for updates")
        }

        UpdateStatus.Checking -> ProgressText("Checking for updates")
        UpdateStatus.Downloading -> ProgressText("Downloading update")
        UpdateStatus.UpToDate -> Text(
            text = "StoryBoy is up to date",
            style = MaterialTheme.typography.bodyMedium,
        )

        UpdateStatus.ReadyToInstall -> Button(onClick = onInstall) {
            Text("Install update")
        }

        is UpdateStatus.Available -> Column(
            verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
        ) {
            Text(
                text = "Version ${status.manifest.versionName} available",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onDownload) {
                Text("Download update")
            }
        }

        is UpdateStatus.Failed -> Column(
            verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
        ) {
            Text(
                text = status.message,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onCheck) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun ProgressText(text: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
    ) {
        CircularProgressIndicator()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
