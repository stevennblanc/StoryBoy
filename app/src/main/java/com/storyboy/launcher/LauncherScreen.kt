package com.storyboy.launcher

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import com.storyboy.core.AppConfig
import com.storyboy.core.ThemeManager
import com.storyboy.core.UiConfig
import com.storyboy.models.GamebookMetadata
import com.storyboy.models.LocalGamebook
import com.storyboy.models.StoreGamebook
import com.storyboy.updater.UpdateStatus
import com.storyboy.updater.UpdateViewModel

@Composable
fun LauncherScreen(
    launcherViewModel: LauncherViewModel,
    updateViewModel: UpdateViewModel,
    onOpenSettings: () -> Unit,
    onOpenGamebook: (LocalGamebook) -> Unit,
) {
    val state by launcherViewModel.state.collectAsState()
    val updateStatus by updateViewModel.status.collectAsState()

    val filteredLibrary = state.library.filter { it.metadata.matches(state.searchQuery) }
    val filteredStore = state.store.filter { it.metadata.matches(state.searchQuery) }

    val colors = ThemeManager.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.BackgroundCol)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = UiConfig.Spacing.ScreenPadding,
                    vertical = UiConfig.Spacing.ListBuffer,
                ),
            verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
        ) {
            LauncherTopBar(
                selectedTab = state.selectedTab,
                onRefresh = {
                    if (state.selectedTab == LauncherTab.Library) {
                        launcherViewModel.refreshLibrary()
                    } else {
                        launcherViewModel.refreshStore()
                    }
                },
            )
            SearchBar(
                query = state.searchQuery,
                onQueryChange = launcherViewModel::updateSearchQuery,
            )

            state.message?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }

            Box(modifier = Modifier.weight(1f)) {
                when (state.selectedTab) {
                    LauncherTab.Library -> LibraryShelf(
                        isLoading = state.isLoadingLibrary,
                        books = filteredLibrary,
                        displayMode = state.libraryDisplayMode,
                        onDisplayModeChange = launcherViewModel::selectLibraryDisplayMode,
                        onSelect = launcherViewModel::selectLocalGamebook,
                    )

                    LauncherTab.Store -> StoreShelf(
                        isLoading = state.isLoadingStore,
                        books = filteredStore,
                        onSelect = launcherViewModel::selectStoreGamebook,
                    )
                }
            }

            HorizontalDivider(color = colors.SubDivider)
            UpdateStrip(
                status = updateStatus,
                onCheck = updateViewModel::checkForUpdates,
                onDownload = updateViewModel::downloadAndInstall,
                onInstall = updateViewModel::installPendingUpdate,
            )
            BottomNav(
                selectedTab = state.selectedTab,
                onSelectTab = launcherViewModel::selectTab,
                onOpenSettings = onOpenSettings,
            )
        }

        state.selectedLocalGamebook?.let { gamebook ->
            LocalGamebookDetail(
                gamebook = gamebook,
                onClose = launcherViewModel::closeDetail,
                onDelete = { launcherViewModel.deleteLocalGamebook(gamebook) },
                onPlay = {
                    launcherViewModel.markStarted(gamebook)
                    launcherViewModel.closeDetail()
                    onOpenGamebook(gamebook)
                },
            )
        }

        state.selectedStoreGamebook?.let { gamebook ->
            StoreGamebookDetail(
                gamebook = gamebook,
                onClose = launcherViewModel::closeDetail,
                onDownload = { launcherViewModel.download(gamebook) },
            )
        }
    }
}

@Composable
private fun LauncherTopBar(
    selectedTab: LauncherTab,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = selectedTab.name, style = MaterialTheme.typography.displayMedium)
            Text(text = AppConfig.AppName, style = MaterialTheme.typography.bodyMedium)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Offline ready", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRefresh) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Search library and store") },
    )
}

@Composable
private fun BottomNav(
    selectedTab: LauncherTab,
    onSelectTab: (LauncherTab) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = ThemeManager.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.SurfaceCol, RoundedCornerShape(UiConfig.Controls.ButtonRadius))
            .padding(UiConfig.Spacing.ItemGap),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomNavItem(
            label = "Library",
            selected = selectedTab == LauncherTab.Library,
            onClick = { onSelectTab(LauncherTab.Library) },
        )
        BottomNavItem(
            label = "Store",
            selected = selectedTab == LauncherTab.Store,
            onClick = { onSelectTab(LauncherTab.Store) },
        )
        BottomNavItem(
            label = "Settings",
            selected = false,
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = if (selected) ThemeManager.colors.AccentCol else ThemeManager.colors.BodyText,
        )
    }
}

@Composable
private fun LibraryTools(
    displayMode: LibraryDisplayMode,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val colors = ThemeManager.colors
        LibraryDisplayMode.values().forEach { mode ->
            val selected = mode == displayMode
            TextButton(
                onClick = { onDisplayModeChange(mode) },
                modifier = Modifier.border(
                    width = UiConfig.Controls.FocusThickness,
                    color = if (selected) colors.FocusCol else colors.SubDivider,
                    shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
                ),
            ) {
                Text(mode.name)
            }
        }
    }
}

@Composable
private fun LibraryShelf(
    isLoading: Boolean,
    books: List<LocalGamebook>,
    displayMode: LibraryDisplayMode,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    onSelect: (LocalGamebook) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
    ) {
        LibraryTools(
            displayMode = displayMode,
            onDisplayModeChange = onDisplayModeChange,
        )

        when {
            isLoading -> ProgressText("Loading library")
            books.isEmpty() -> EmptyState("No gamebooks downloaded")
            displayMode == LibraryDisplayMode.Book -> BookGrid(books = books, onSelect = onSelect)
            else -> CartridgeList(books = books, onSelect = onSelect)
        }
    }
}

@Composable
private fun StoreShelf(
    isLoading: Boolean,
    books: List<StoreGamebook>,
    onSelect: (StoreGamebook) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
    ) {
        when {
            isLoading -> ProgressText("Loading store")
            books.isEmpty() -> EmptyState("No adventures available")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer)) {
                items(books, key = { it.metadata.id }) { book ->
                    StoreRow(book = book, onSelect = { onSelect(book) })
                }
            }
        }
    }
}

@Composable
private fun BookGrid(
    books: List<LocalGamebook>,
    onSelect: (LocalGamebook) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(UiConfig.ImageSizes.GamePosterGridWidth),
        horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.GridBuffer),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.GridBuffer),
    ) {
        items(books, key = { it.metadata.id }) { book ->
            BookTile(book = book, onSelect = { onSelect(book) })
        }
    }
}

@Composable
private fun BookTile(
    book: LocalGamebook,
    onSelect: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onSelect),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
    ) {
        ArtworkFrame(
            artworkPath = book.posterPath,
            displayMode = LibraryDisplayMode.Book,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = book.metadata.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (book.hasPlaythroughInProgress) "In progress" else "New",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun CartridgeList(
    books: List<LocalGamebook>,
    onSelect: (LocalGamebook) -> Unit,
) {
    val colors = ThemeManager.colors
    LazyColumn(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer)) {
        items(books, key = { it.metadata.id }) { book ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(book) }
                    .background(colors.SurfaceCol, RoundedCornerShape(UiConfig.Controls.ButtonRadius))
                    .padding(UiConfig.Spacing.ListBuffer),
                horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkFrame(
                    artworkPath = book.bannerPath ?: book.posterPath,
                    displayMode = LibraryDisplayMode.Cartridge,
                )
                BookSummary(metadata = book.metadata, status = if (book.hasPlaythroughInProgress) "In progress" else "New")
            }
        }
    }
}

@Composable
private fun StoreRow(
    book: StoreGamebook,
    onSelect: () -> Unit,
) {
    val colors = ThemeManager.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .background(colors.SurfaceCol, RoundedCornerShape(UiConfig.Controls.ButtonRadius))
            .padding(UiConfig.Spacing.ListBuffer),
        horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkFrame(
            artworkPath = null,
            displayMode = LibraryDisplayMode.Book,
            modifier = Modifier.width(UiConfig.ImageSizes.StorePosterWidth),
        )
        BookSummary(metadata = book.metadata, status = book.storeStatusText())
    }
}

@Composable
private fun BookSummary(
    metadata: GamebookMetadata,
    status: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
    ) {
        Text(text = metadata.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = "${metadata.author} • ${metadata.genre}", style = MaterialTheme.typography.bodyMedium)
        Text(text = metadata.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(text = status, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LocalGamebookDetail(
    gamebook: LocalGamebook,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
) {
    DetailPanel(onClose = onClose) {
        ArtworkFrame(
            artworkPath = gamebook.posterPath,
            displayMode = LibraryDisplayMode.Book,
            modifier = Modifier.width(UiConfig.ImageSizes.GamePosterGridWidth),
        )
        DetailCopy(metadata = gamebook.metadata)
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
            Text(if (gamebook.hasPlaythroughInProgress) "Resume" else "Play")
        }
        TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text("Delete from device")
        }
    }
}

@Composable
private fun StoreGamebookDetail(
    gamebook: StoreGamebook,
    onClose: () -> Unit,
    onDownload: () -> Unit,
) {
    DetailPanel(onClose = onClose) {
        ArtworkFrame(
            artworkPath = null,
            displayMode = LibraryDisplayMode.Book,
            modifier = Modifier.width(UiConfig.ImageSizes.GamePosterGridWidth),
        )
        DetailCopy(metadata = gamebook.metadata)
        Button(
            onClick = onDownload,
            enabled = !gamebook.isDownloaded || gamebook.updateAvailable,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(gamebook.storeActionText())
        }
        if (gamebook.isDownloaded) {
            Text(
                text = "Installed: ${gamebook.localVersion}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DetailPanel(
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ThemeManager.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.BackgroundCol)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UiConfig.Spacing.ScreenPadding)
                .background(colors.ElevatedSurfaceCol, RoundedCornerShape(UiConfig.Controls.ButtonRadius))
                .padding(UiConfig.Spacing.SectionGap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            }
            content()
        }
    }
}

@Composable
private fun DetailCopy(metadata: GamebookMetadata) {
    Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
        Text(text = metadata.title, style = MaterialTheme.typography.displayMedium)
        Text(text = "${metadata.author} • ${metadata.genre} • ${metadata.version}", style = MaterialTheme.typography.labelLarge)
        Text(text = metadata.description, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ArtworkFrame(
    artworkPath: String?,
    displayMode: LibraryDisplayMode,
    modifier: Modifier = Modifier,
) {
    val colors = ThemeManager.colors
    val bitmap = remember(artworkPath) {
        artworkPath?.let(BitmapFactory::decodeFile)
    }
    val frameModifier = when (displayMode) {
        LibraryDisplayMode.Book -> modifier.aspectRatio(0.69f)
        LibraryDisplayMode.Cartridge -> modifier
            .width(UiConfig.ImageSizes.GameBannerListWidth)
            .height(UiConfig.ImageSizes.GameBannerListHeight)
    }

    Box(
        modifier = frameModifier
            .background(colors.BackgroundCol, RoundedCornerShape(UiConfig.Controls.PosterCornerRadius))
            .border(
                width = UiConfig.Controls.FocusThickness,
                color = colors.MainDivider,
                shape = RoundedCornerShape(UiConfig.Controls.PosterCornerRadius),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            Text(text = AppConfig.GamebookExtension, style = MaterialTheme.typography.bodyMedium)
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
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun UpdateStrip(
    status: UpdateStatus,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    when (status) {
        UpdateStatus.Idle -> TextButton(onClick = onCheck) {
            Text("Check for app updates")
        }

        UpdateStatus.Checking -> Text(text = "Checking for app updates", style = MaterialTheme.typography.bodyMedium)
        UpdateStatus.Downloading -> Text(text = "Downloading app update", style = MaterialTheme.typography.bodyMedium)
        UpdateStatus.UpToDate -> Text(text = "StoryBoy is up to date", style = MaterialTheme.typography.bodyMedium)
        UpdateStatus.ReadyToInstall -> Button(onClick = onInstall) {
            Text("Install app update")
        }

        is UpdateStatus.Available -> Button(
            onClick = onDownload,
            colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.colors.AccentCol),
        ) {
            Text("Install StoryBoy ${status.manifest.versionName}")
        }

        is UpdateStatus.Failed -> TextButton(onClick = onCheck) {
            Text("Update check failed")
        }
    }
}

@Composable
private fun ProgressText(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun GamebookMetadata.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val normalized = query.trim()
    return title.contains(normalized, ignoreCase = true) ||
        author.contains(normalized, ignoreCase = true) ||
        genre.contains(normalized, ignoreCase = true) ||
        description.contains(normalized, ignoreCase = true)
}

private fun StoreGamebook.storeStatusText(): String {
    return when {
        updateAvailable -> "Update available"
        isDownloaded -> "Downloaded"
        else -> "Available"
    }
}

private fun StoreGamebook.storeActionText(): String {
    return when {
        updateAvailable -> "Update downloaded copy"
        isDownloaded -> "Downloaded"
        else -> "Download for offline play"
    }
}
