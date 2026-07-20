import 'dart:io';

import 'package:flutter/material.dart';

import '../app_model.dart';
import '../models.dart';
import '../theme.dart';
import 'book_detail_screen.dart';
import 'browse_widgets.dart';

class LibraryScreen extends StatefulWidget {
  const LibraryScreen({super.key, required this.model});

  final AppModel model;

  @override
  State<LibraryScreen> createState() => _LibraryScreenState();
}

class _LibraryScreenState extends State<LibraryScreen> {
  String _query = '';
  String? _genre;
  ProgressFilter _progressFilter = ProgressFilter.all;

  List<LocalBook> get _filtered {
    final model = widget.model;
    final books = model.localBooks
        .where((book) => matchesQuery(_query, [book.title, book.author, book.genre, book.description]))
        .where((book) => _genre == null || book.genre == _genre)
        .where((book) => switch (_progressFilter) {
              ProgressFilter.all => true,
              ProgressFilter.inProgress => book.hasProgress,
              ProgressFilter.notStarted => !book.hasProgress,
            })
        .toList();
    books.sort((a, b) => switch (model.librarySort) {
          LibrarySort.title => a.title.toLowerCase().compareTo(b.title.toLowerCase()),
          LibrarySort.author => a.author.toLowerCase().compareTo(b.author.toLowerCase()),
        });
    return books;
  }

  @override
  Widget build(BuildContext context) {
    final model = widget.model;
    final genres = model.localBooks.map((b) => b.genre).where((g) => g.isNotEmpty).toSet().toList()
      ..sort();
    final books = _filtered;
    final filterLabel = [
      ?_genre,
      if (_progressFilter == ProgressFilter.inProgress) 'In progress',
      if (_progressFilter == ProgressFilter.notStarted) 'Not started',
    ].join(' - ');

    return SafeArea(
      child: RefreshIndicator(
        onRefresh: model.refreshLibrary,
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  children: [
                    BrowseHeader(
                      title: 'Library',
                      subtitle: filterLabel,
                      actions: [
                        FilterMenuButton(
                          genres: genres,
                          selectedGenre: _genre,
                          onGenre: (genre) => setState(() => _genre = genre),
                          progressFilter: _progressFilter,
                          onProgressFilter: (filter) => setState(() => _progressFilter = filter),
                        ),
                        ViewMenuButton(model: model, sortOptions: true),
                        IconButton(
                          onPressed: model.refreshLibrary,
                          icon: const Icon(Icons.refresh, color: SbColors.text),
                        ),
                      ],
                    ),
                    BrowseSearchField(
                      value: _query,
                      hint: 'Search library',
                      onChanged: (value) => setState(() => _query = value),
                    ),
                    const SizedBox(height: 12),
                  ],
                ),
              ),
            ),
            if (model.isLoadingLibrary && model.localBooks.isEmpty)
              const SliverFillRemaining(child: Center(child: CircularProgressIndicator()))
            else if (books.isEmpty)
              SliverFillRemaining(
                hasScrollBody: false,
                child: Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        model.localBooks.isEmpty ? 'No gamebooks downloaded' : 'No matches',
                        style: const TextStyle(fontSize: 20),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        model.localBooks.isEmpty
                            ? 'Browse the store to download a free adventure.'
                            : 'Try a different search or filter.',
                        style: mutedStyle,
                      ),
                    ],
                  ),
                ),
              )
            else if (model.displayMode == DisplayMode.grid)
              SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                sliver: SliverGrid(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 3,
                    mainAxisSpacing: 12,
                    crossAxisSpacing: 12,
                    childAspectRatio: 0.5,
                  ),
                  delegate: SliverChildBuilderDelegate(
                    childCount: books.length,
                    (context, index) => _LibraryTile(book: books[index], model: model),
                  ),
                ),
              )
            else
              SliverList(
                delegate: SliverChildBuilderDelegate(
                  childCount: books.length,
                  (context, index) => _LibraryRow(book: books[index], model: model),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _LibraryTile extends StatelessWidget {
  const _LibraryTile({required this.book, required this.model});

  final LocalBook book;
  final AppModel model;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => _openDetail(context, model, book.id),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AspectRatio(aspectRatio: 2 / 3, child: BookPoster(posterFile: book.posterFile)),
          const SizedBox(height: 6),
          Text(
            book.title,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 13),
          ),
          Text(
            book.hasProgress ? 'In progress' : book.genre,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: mutedStyle.copyWith(fontSize: 12),
          ),
        ],
      ),
    );
  }
}

class _LibraryRow extends StatelessWidget {
  const _LibraryRow({required this.book, required this.model});

  final LocalBook book;
  final AppModel model;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () => _openDetail(context, model, book.id),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: SbColors.line)),
        ),
        child: Row(
          children: [
            SizedBox(
              width: 120,
              height: 74,
              child: BookBanner(bannerFile: book.bannerFile ?? book.posterFile),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    book.title,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
                  ),
                  Text(book.author, style: mutedStyle, maxLines: 1),
                  Text(
                    '${book.genre} - ${book.hasProgress ? 'In progress' : 'New'}',
                    style: mutedStyle.copyWith(fontSize: 13),
                    maxLines: 1,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

void _openDetail(BuildContext context, AppModel model, String bookId) {
  Navigator.of(context).push(
    MaterialPageRoute(builder: (_) => BookDetailScreen(model: model, bookId: bookId)),
  );
}

/// Poster artwork with the standard 2:3 frame and a fallback placeholder.
class BookPoster extends StatelessWidget {
  const BookPoster({super.key, this.posterFile, this.posterUrl});

  final String? posterFile;
  final String? posterUrl;

  @override
  Widget build(BuildContext context) {
    Widget child;
    if (posterFile != null) {
      child = Image.file(File(posterFile!), fit: BoxFit.cover);
    } else if (posterUrl != null && posterUrl!.isNotEmpty) {
      child = Image.network(
        posterUrl!,
        fit: BoxFit.cover,
        errorBuilder: (_, _, _) => const _ArtPlaceholder(),
      );
    } else {
      child = const _ArtPlaceholder();
    }
    return _ArtFrame(child: child);
  }
}

/// Banner artwork (3:2 cartridge style) with fallback.
class BookBanner extends StatelessWidget {
  const BookBanner({super.key, this.bannerFile, this.bannerUrl});

  final String? bannerFile;
  final String? bannerUrl;

  @override
  Widget build(BuildContext context) {
    Widget child;
    if (bannerFile != null) {
      child = Image.file(File(bannerFile!), fit: BoxFit.cover);
    } else if (bannerUrl != null && bannerUrl!.isNotEmpty) {
      child = Image.network(
        bannerUrl!,
        fit: BoxFit.cover,
        errorBuilder: (_, _, _) => const _ArtPlaceholder(),
      );
    } else {
      child = const _ArtPlaceholder();
    }
    return _ArtFrame(child: child);
  }
}

class _ArtFrame extends StatelessWidget {
  const _ArtFrame({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(6),
      child: Container(
        decoration: BoxDecoration(
          color: SbColors.surface,
          border: Border.all(color: SbColors.line),
          borderRadius: BorderRadius.circular(6),
        ),
        width: double.infinity,
        height: double.infinity,
        child: child,
      ),
    );
  }
}

class _ArtPlaceholder extends StatelessWidget {
  const _ArtPlaceholder();

  @override
  Widget build(BuildContext context) {
    return const Center(child: Text('gbk', style: mutedStyle));
  }
}
