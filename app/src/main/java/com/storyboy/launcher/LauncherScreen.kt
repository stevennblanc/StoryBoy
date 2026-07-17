package com.storyboy.launcher

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.storyboy.core.AppConfig
import com.storyboy.core.UiConfig
import com.storyboy.models.LocalGamebook
import com.storyboy.models.StoreGamebook
import com.storyboy.updater.UpdateStatus
import com.storyboy.updater.UpdateViewModel

@Composable
fun LauncherScreen(
    launcherViewModel: LauncherViewModel,
    updateViewModel: UpdateViewModel,
    onOpenGamebook: (LocalGamebook) -> Unit,
) {
    val launcherState by launcherViewModel.state.collectAsState()
    val updateStatus by updateViewModel.status.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UiConfig.ThemeColors.BackgroundCol)
            .padding(UiConfig.Spacing.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
    ) {
        LauncherHeader()
        LauncherTabs(
            selectedTab = launcherState.selectedTab,
            onSelectTab = launcherViewModel::selectTab,
        )

        launcherState.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        when (launcherState.selectedTab) {
            LauncherTab.Library -> LibraryPage(
                isLoading = launcherState.isLoadingLibrary,
                books = launcherState.library,
                displayMode = launcherState.libraryDisplayMode,
                onRefresh = launcherViewModel::refreshLibrary,
                onDisplayModeChange = launcherViewModel::selectLibraryDisplayMode,
                onOpen = { gamebook ->
                    launcherViewModel.markStarted(gamebook)
                    onOpenGamebook(gamebook)
                },
            )

            LauncherTab.Store -> StorePage(
                isLoading = launcherState.isLoadingStore,
                books = launcherState.store,
                onRefresh = launcherViewModel::refreshStore,
                onDownload = launcherViewModel::download,
            )
        }

        HorizontalDivider(color = UiConfig.ThemeColors.SubDivider)
        UpdatePanel(
            status = updateStatus,
            onCheck = updateViewModel::checkForUpdates,
            onDownload = updateViewModel::downloadAndInstall,
            onInstall = updateViewModel::installPendingUpdate,
        )
    }
}

@Composable
private fun LauncherHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
        Text(
            text = AppConfig.AppName,
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = "Library",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LauncherTabs(
    selectedTab: LauncherTab,
    onSelectTab: (LauncherTab) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
        LauncherTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            TextButton(
                onClick = { onSelectTab(tab) },
                modifier = Modifier.border(
                    width = UiConfig.Controls.FocusThickness,
                    color = if (isSelected) UiConfig.ThemeColors.FocusCol else UiConfig.ThemeColors.SubDivider,
                    shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
                ),
            ) {
                Text(tab.name)
            }
        }
    }
}

@Composable
private fun LibraryPage(
    isLoading: Boolean,
    books: List<LocalGamebook>,
    displayMode: LibraryDisplayMode,
    onRefresh: () -> Unit,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    onOpen: (LocalGamebook) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
            Button(onClick = onRefresh) {
                Text("Refresh")
            }
            LibraryDisplayMode.values().forEach { mode ->
                TextButton(
                    onClick = { onDisplayModeChange(mode) },
                    modifier = Modifier.border(
                        width = UiConfig.Controls.FocusThickness,
                        color = if (displayMode == mode) {
                            UiConfig.ThemeColors.FocusCol
                        } else {
                            UiConfig.ThemeColors.SubDivider
                        },
                        shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
                    ),
                ) {
                    Text(mode.name)
                }
            }
        }

        if (isLoading) {
            ProgressText("Loading library")
        } else if (books.isEmpty()) {
            EmptyState("No gamebooks downloaded")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer)) {
                items(books, key = { it.metadata.id }) { book ->
                    LocalGamebookRow(
                        book = book,
                        displayMode = displayMode,
                        onOpen = { onOpen(book) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StorePage(
    isLoading: Boolean,
    books: List<StoreGamebook>,
    onRefresh: () -> Unit,
    onDownload: (StoreGamebook) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
    ) {
        Button(onClick = onRefresh) {
            Text("Refresh store")
        }

        if (isLoading) {
            ProgressText("Loading store")
        } else if (books.isEmpty()) {
            EmptyState("Store shelf is empty")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer)) {
                items(books, key = { it.metadata.id }) { book ->
                    StoreGamebookRow(book = book, onDownload = { onDownload(book) })
                }
            }
        }
    }
}

@Composable
private fun LocalGamebookRow(
    book: LocalGamebook,
    displayMode: LibraryDisplayMode,
    onOpen: () -> Unit,
) {
    GamebookRow(
        title = book.metadata.title,
        author = book.metadata.author,
        description = book.metadata.description,
        status = if (book.hasPlaythroughInProgress) "In progress" else "New",
        action = "Open",
        artworkPath = when (displayMode) {
            LibraryDisplayMode.Book -> book.posterPath
            LibraryDisplayMode.Cartridge -> book.bannerPath ?: book.posterPath
        },
        displayMode = displayMode,
        onAction = onOpen,
        modifier = Modifier.clickable(onClick = onOpen),
    )
}

@Composable
private fun StoreGamebookRow(
    book: StoreGamebook,
    onDownload: () -> Unit,
) {
    GamebookRow(
        title = book.metadata.title,
        author = book.metadata.author,
        description = book.metadata.description,
        status = if (book.isDownloaded) "Downloaded" else "Available",
        action = if (book.isDownloaded) "Saved" else "Download",
        artworkPath = null,
        displayMode = LibraryDisplayMode.Book,
        onAction = onDownload,
    )
}

@Composable
private fun GamebookRow(
    title: String,
    author: String,
    description: String,
    status: String,
    action: String,
    artworkPath: String?,
    displayMode: LibraryDisplayMode,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(UiConfig.ThemeColors.SurfaceCol, RoundedCornerShape(UiConfig.Controls.ButtonRadius))
            .border(
                width = UiConfig.Controls.FocusThickness,
                color = UiConfig.ThemeColors.SubDivider,
                shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
            )
            .padding(UiConfig.Spacing.ListBuffer),
        horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GamebookArtwork(
            artworkPath = artworkPath,
            displayMode = displayMode,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Text(text = author, style = MaterialTheme.typography.bodyMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            Text(text = status, style = MaterialTheme.typography.labelLarge)
        }
        Button(onClick = onAction) {
            Text(action)
        }
    }
}

@Composable
private fun GamebookArtwork(
    artworkPath: String?,
    displayMode: LibraryDisplayMode,
) {
    val bitmap = remember(artworkPath) {
        artworkPath?.let(BitmapFactory::decodeFile)
    }
    val width = when (displayMode) {
        LibraryDisplayMode.Book -> UiConfig.ImageSizes.GamePosterListWidth
        LibraryDisplayMode.Cartridge -> UiConfig.ImageSizes.GameBannerListWidth
    }
    val height = when (displayMode) {
        LibraryDisplayMode.Book -> UiConfig.ImageSizes.GamePosterListHeight
        LibraryDisplayMode.Cartridge -> UiConfig.ImageSizes.GameBannerListHeight
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(UiConfig.ThemeColors.BackgroundCol, RoundedCornerShape(UiConfig.Controls.PosterCornerRadius))
            .border(
                width = UiConfig.Controls.FocusThickness,
                color = UiConfig.ThemeColors.MainDivider,
                shape = RoundedCornerShape(UiConfig.Controls.PosterCornerRadius),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            Text(
                text = AppConfig.GamebookExtension,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
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
            Text("Check for app updates")
        }

        UpdateStatus.Checking -> ProgressText("Checking for app updates")
        UpdateStatus.Downloading -> ProgressText("Downloading app update")
        UpdateStatus.UpToDate -> Text(
            text = "StoryBoy is up to date",
            style = MaterialTheme.typography.bodyMedium,
        )

        UpdateStatus.ReadyToInstall -> Button(onClick = onInstall) {
            Text("Install app update")
        }

        is UpdateStatus.Available -> Column(
            verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
        ) {
            Text(
                text = "Version ${status.manifest.versionName} available",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onDownload) {
                Text("Download app update")
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
