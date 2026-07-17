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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.storyboy.core.AppConfig
import com.storyboy.core.ThemeManager
import com.storyboy.core.UiConfig
import com.storyboy.models.GamebookMetadata
import com.storyboy.models.LocalGamebook
import com.storyboy.models.StoreGamebook
import com.storyboy.widgets.StoryBoyIcon
import com.storyboy.widgets.StoryBoyIconKind

@Composable
fun LauncherScreen(
    launcherViewModel: LauncherViewModel,
    onOpenSettings: () -> Unit,
    onOpenGamebook: (LocalGamebook) -> Unit,
) {
    val state by launcherViewModel.state.collectAsState()
    val visibleMetadata = if (state.selectedTab == LauncherTab.Library) {
        state.library.map { it.metadata }
    } else {
        state.store.map { it.metadata }
    }
    val genres = visibleMetadata.map { it.genre }.filter { it.isNotBlank() }.distinct().sorted()
    val filteredLibrary = state.library
        .filter { it.metadata.matches(state.searchQuery) }
        .filter { state.selectedGenre == null || it.metadata.genre == state.selectedGenre }
        .filter {
            when (state.progressFilter) {
                ProgressFilter.All -> true
                ProgressFilter.InProgress -> it.hasPlaythroughInProgress
                ProgressFilter.NotStarted -> !it.hasPlaythroughInProgress
            }
        }
        .sortedWith(state.librarySortMode.comparator())
    val filteredStore = state.store
        .filter { it.metadata.matches(state.searchQuery) }
        .filter { state.selectedGenre == null || it.metadata.genre == state.selectedGenre }
    val colors = ThemeManager.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.BackgroundCol)
            .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = UiConfig.Spacing.ScreenPadding,
                        vertical = UiConfig.Spacing.ListBuffer,
                    ),
                verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
            ) {
                LauncherHeader(
                    selectedTab = state.selectedTab,
                    displayMode = state.libraryDisplayMode,
                    sortMode = state.librarySortMode,
                    genres = genres,
                    selectedGenre = state.selectedGenre,
                    progressFilter = state.progressFilter,
                    onRefresh = {
                        if (state.selectedTab == LauncherTab.Library) {
                            launcherViewModel.refreshLibrary()
                        } else {
                            launcherViewModel.refreshStore()
                        }
                    },
                    onDisplayModeChange = launcherViewModel::selectLibraryDisplayMode,
                    onSortModeChange = launcherViewModel::selectLibrarySortMode,
                    onGenreChange = launcherViewModel::selectGenreFilter,
                    onProgressFilterChange = launcherViewModel::selectProgressFilter,
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
                            onBrowseStore = { launcherViewModel.selectTab(LauncherTab.Store) },
                            onSelect = launcherViewModel::selectLocalGamebook,
                        )

                        LauncherTab.Store -> StoreShelf(
                            isLoading = state.isLoadingStore,
                            books = filteredStore,
                            displayMode = state.libraryDisplayMode,
                            onSelect = launcherViewModel::selectStoreGamebook,
                        )
                    }
                }
            }

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
private fun LauncherHeader(
    selectedTab: LauncherTab,
    displayMode: LibraryDisplayMode,
    sortMode: LibrarySortMode,
    genres: List<String>,
    selectedGenre: String?,
    progressFilter: ProgressFilter,
    onRefresh: () -> Unit,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    onSortModeChange: (LibrarySortMode) -> Unit,
    onGenreChange: (String?) -> Unit,
    onProgressFilterChange: (ProgressFilter) -> Unit,
) {
    var filterMenuOpen by remember { mutableStateOf(false) }
    var viewMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = selectedTab.name, style = MaterialTheme.typography.displayMedium)
            if (selectedGenre != null || progressFilter != ProgressFilter.All) {
                Text(
                    text = listOfNotNull(selectedGenre, progressFilter.takeIf { it != ProgressFilter.All }?.label)
                        .joinToString(" - "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                IconButton(onClick = { filterMenuOpen = true }) {
                    StoryBoyIcon(kind = StoryBoyIconKind.Sliders, color = ThemeManager.colors.BodyText)
                }
                DropdownMenu(expanded = filterMenuOpen, onDismissRequest = { filterMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            onGenreChange(null)
                            onProgressFilterChange(ProgressFilter.All)
                            filterMenuOpen = false
                        },
                    )
                    if (selectedTab == LauncherTab.Library) {
                        ProgressFilter.values().filterNot { it == ProgressFilter.All }.forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(filter.label) },
                                onClick = {
                                    onProgressFilterChange(filter)
                                    filterMenuOpen = false
                                },
                            )
                        }
                    }
                    genres.forEach { genre ->
                        DropdownMenuItem(
                            text = { Text(genre) },
                            onClick = {
                                onGenreChange(genre)
                                filterMenuOpen = false
                            },
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { viewMenuOpen = true }) {
                    StoryBoyIcon(kind = StoryBoyIconKind.Sort, color = ThemeManager.colors.BodyText)
                }
                DropdownMenu(expanded = viewMenuOpen, onDismissRequest = { viewMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Book view") },
                        onClick = {
                            onDisplayModeChange(LibraryDisplayMode.Book)
                            viewMenuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("List view") },
                        onClick = {
                            onDisplayModeChange(LibraryDisplayMode.Cartridge)
                            viewMenuOpen = false
                        },
                    )
                    if (selectedTab == LauncherTab.Library) {
                        HorizontalDivider(color = ThemeManager.colors.SubDivider)
                        LibrarySortMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = {
                                    onSortModeChange(mode)
                                    viewMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onRefresh) {
                StoryBoyIcon(kind = StoryBoyIconKind.Refresh, color = ThemeManager.colors.BodyText)
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
        label = { Text("Search") },
    )
}

@Composable
private fun BottomNav(
    selectedTab: LauncherTab,
    onSelectTab: (LauncherTab) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = ThemeManager.colors
    Column {
        HorizontalDivider(color = colors.SubDivider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.SurfaceCol)
                .padding(vertical = UiConfig.Spacing.ItemGap),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomNavItem(
                kind = StoryBoyIconKind.Books,
                selected = selectedTab == LauncherTab.Library,
                onClick = { onSelectTab(LauncherTab.Library) },
            )
            BottomNavItem(
                kind = StoryBoyIconKind.Store,
                selected = selectedTab == LauncherTab.Store,
                onClick = { onSelectTab(LauncherTab.Store) },
            )
            BottomNavItem(
                kind = StoryBoyIconKind.Gear,
                selected = false,
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    kind: StoryBoyIconKind,
    selected: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        StoryBoyIcon(
            kind = kind,
            color = if (selected) ThemeManager.colors.AccentCol else ThemeManager.colors.BodyText,
        )
    }
}

@Composable
private fun LibraryShelf(
    isLoading: Boolean,
    books: List<LocalGamebook>,
    displayMode: LibraryDisplayMode,
    onBrowseStore: () -> Unit,
    onSelect: (LocalGamebook) -> Unit,
) {
    when {
        isLoading -> ProgressText("Loading library")
        books.isEmpty() -> EmptyLibraryState(onBrowseStore = onBrowseStore)
        displayMode == LibraryDisplayMode.Book -> BookGrid(books = books, onSelect = onSelect)
        else -> CartridgeList(books = books, onSelect = onSelect)
    }
}

@Composable
private fun StoreShelf(
    isLoading: Boolean,
    books: List<StoreGamebook>,
    displayMode: LibraryDisplayMode,
    onSelect: (StoreGamebook) -> Unit,
) {
    when {
        isLoading -> ProgressText("Loading store")
        books.isEmpty() -> EmptyState("No adventures available")
        displayMode == LibraryDisplayMode.Book -> StoreGrid(books = books, onSelect = onSelect)
        else -> LazyColumn {
            items(books, key = { it.metadata.id }) { book ->
                StoreRow(book = book, onSelect = { onSelect(book) })
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
        columns = GridCells.Fixed(3),
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
    Box(modifier = Modifier.clickable(onClick = onSelect)) {
        ArtworkFrame(
            artworkPath = book.posterPath,
            displayMode = LibraryDisplayMode.Book,
            modifier = Modifier.fillMaxWidth(),
        )
        if (book.hasPlaythroughInProgress) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-UiConfig.Spacing.ItemGap), y = UiConfig.Spacing.ItemGap)
                    .background(ThemeManager.colors.AccentCol, RoundedCornerShape(UiConfig.Controls.ButtonRadius))
                    .padding(UiConfig.Spacing.ItemGap),
            ) {
                StoryBoyIcon(
                    kind = StoryBoyIconKind.Check,
                    color = ThemeManager.colors.BackgroundCol,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CartridgeList(
    books: List<LocalGamebook>,
    onSelect: (LocalGamebook) -> Unit,
) {
    LazyColumn {
        items(books, key = { it.metadata.id }) { book ->
            LocalBookRow(book = book, onSelect = { onSelect(book) })
            HorizontalDivider(color = ThemeManager.colors.SubDivider)
        }
    }
}

@Composable
private fun LocalBookRow(
    book: LocalGamebook,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = UiConfig.Spacing.ListBuffer),
        horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkFrame(
            artworkPath = book.bannerPath ?: book.posterPath,
            displayMode = LibraryDisplayMode.Cartridge,
            modifier = Modifier.width(UiConfig.ImageSizes.GameBannerListWidth),
        )
        BookListSummary(
            metadata = book.metadata,
            status = if (book.hasPlaythroughInProgress) "In progress" else "New",
        )
    }
}

@Composable
private fun StoreGrid(
    books: List<StoreGamebook>,
    onSelect: (StoreGamebook) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.GridBuffer),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.GridBuffer),
    ) {
        items(books, key = { it.metadata.id }) { book ->
            Box(modifier = Modifier.clickable { onSelect(book) }) {
                ArtworkFrame(
                    artworkPath = book.posterPath,
                    displayMode = LibraryDisplayMode.Book,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BookListSummary(
    metadata: GamebookMetadata,
    status: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
    ) {
        Text(
            text = metadata.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = metadata.author,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${metadata.genre} - $status",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoreRow(
    book: StoreGamebook,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = UiConfig.Spacing.ListBuffer),
        horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkFrame(
            artworkPath = book.bannerPath ?: book.posterPath,
            displayMode = LibraryDisplayMode.Cartridge,
            modifier = Modifier.width(UiConfig.ImageSizes.GameBannerListWidth),
        )
        BookListSummary(metadata = book.metadata, status = book.storeStatusText())
    }
    HorizontalDivider(color = ThemeManager.colors.SubDivider)
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
        FeatureChips(features = gamebook.metadata.featureLabels())
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
            artworkPath = gamebook.posterPath,
            displayMode = LibraryDisplayMode.Book,
            modifier = Modifier.width(UiConfig.ImageSizes.GamePosterGridWidth),
        )
        DetailCopy(metadata = gamebook.metadata)
        FeatureChips(features = gamebook.metadata.featureLabels())
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
@OptIn(ExperimentalLayoutApi::class)
private fun FeatureChips(features: List<String>) {
    if (features.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
    ) {
        features.forEach { feature ->
            Text(
                text = feature,
                modifier = Modifier
                    .background(ThemeManager.colors.SurfaceCol, RoundedCornerShape(UiConfig.Controls.ButtonRadius))
                    .padding(
                        horizontal = UiConfig.Spacing.ListBuffer,
                        vertical = UiConfig.Spacing.ItemGap,
                    ),
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
        Text(text = "${metadata.author} - ${metadata.genre} - ${metadata.version}", style = MaterialTheme.typography.labelLarge)
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
        LibraryDisplayMode.Cartridge -> Modifier
            .width(UiConfig.ImageSizes.GameBannerListWidth)
            .height(UiConfig.ImageSizes.GameBannerListHeight)
            .then(modifier)
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
private fun EmptyLibraryState(onBrowseStore: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
    ) {
        Text(text = "No gamebooks downloaded", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Browse the store to download a free adventure for offline play.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onBrowseStore) {
            Text("Browse Store")
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

private fun LibrarySortMode.comparator(): Comparator<LocalGamebook> {
    return when (this) {
        LibrarySortMode.Title -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.metadata.title }
        LibrarySortMode.Author -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.metadata.author }
    }
}

private fun GamebookMetadata.featureLabels(): List<String> {
    val text = "$title $genre $description".lowercase()
    return buildList {
        if ("detective" in text || "mystery" in text || "noir" in text) add("Evidence")
        if ("inventory" in text) add("Inventory")
        if ("puzzle" in text) add("Puzzles")
        if ("battle" in text) add("Battles")
        if ("map" in text) add("Map")
        if (isEmpty()) add("Interactive story")
        add("Offline")
    }.distinct()
}
