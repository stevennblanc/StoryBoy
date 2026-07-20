import 'package:flutter/material.dart';

import '../app_model.dart';
import '../data/store_repository.dart';
import '../models.dart';
import '../theme.dart';
import 'book_detail_screen.dart';
import 'browse_widgets.dart';
import 'library_screen.dart' show BookPoster, BookBanner;

class StoreScreen extends StatefulWidget {
  const StoreScreen({super.key, required this.model});

  final AppModel model;

  @override
  State<StoreScreen> createState() => _StoreScreenState();
}

class _StoreScreenState extends State<StoreScreen> {
  String _query = '';
  String? _genre;

  List<CatalogueBook> get _filtered {
    return widget.model.catalogue
        .where((book) =>
            matchesQuery(_query, [book.title, book.author, book.genre, book.description]))
        .where((book) => _genre == null || book.genre == _genre)
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    final model = widget.model;
    final genres = model.catalogue.map((b) => b.genre).where((g) => g.isNotEmpty).toSet().toList()
      ..sort();
    final books = _filtered;

    return SafeArea(
      child: RefreshIndicator(
        onRefresh: model.refreshStore,
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  children: [
                    BrowseHeader(
                      title: 'Store',
                      subtitle: _genre,
                      actions: [
                        FilterMenuButton(
                          genres: genres,
                          selectedGenre: _genre,
                          onGenre: (genre) => setState(() => _genre = genre),
                        ),
                        ViewMenuButton(model: model),
                        IconButton(
                          onPressed: model.refreshStore,
                          icon: const Icon(Icons.refresh, color: SbColors.text),
                        ),
                      ],
                    ),
                    BrowseSearchField(
                      value: _query,
                      hint: 'Search store',
                      onChanged: (value) => setState(() => _query = value),
                    ),
                    if (model.message != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 10),
                        child: Text(model.message!, style: mutedStyle),
                      ),
                    const SizedBox(height: 12),
                  ],
                ),
              ),
            ),
            if (model.isLoadingStore && model.catalogue.isEmpty)
              const SliverFillRemaining(child: Center(child: CircularProgressIndicator()))
            else if (books.isEmpty)
              const SliverFillRemaining(
                hasScrollBody: false,
                child: Center(child: Text('No matches', style: mutedStyle)),
              )
            else if (model.displayMode == DisplayMode.grid)
              SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                sliver: SliverGrid(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 3,
                    mainAxisSpacing: 12,
                    crossAxisSpacing: 12,
                    childAspectRatio: 0.55,
                  ),
                  delegate: SliverChildBuilderDelegate(
                    childCount: books.length,
                    (context, index) => _StoreTile(book: books[index], model: model),
                  ),
                ),
              )
            else
              SliverList(
                delegate: SliverChildBuilderDelegate(
                  childCount: books.length,
                  (context, index) => _StoreRow(book: books[index], model: model),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _StoreTile extends StatelessWidget {
  const _StoreTile({required this.book, required this.model});

  final CatalogueBook book;
  final AppModel model;

  @override
  Widget build(BuildContext context) {
    final owned = model.ownedBookIds.contains(book.id);
    return GestureDetector(
      onTap: () => _openDetail(context, model, book.id),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(child: BookPoster(posterUrl: resolveStoreUrl(book.posterPath))),
          const SizedBox(height: 6),
          Text(
            book.title,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 13),
          ),
          Text(
            owned ? 'In your library' : book.priceLabel,
            style: mutedStyle.copyWith(
              fontSize: 12,
              color: owned ? SbColors.muted : SbColors.accent,
            ),
          ),
        ],
      ),
    );
  }
}

class _StoreRow extends StatelessWidget {
  const _StoreRow({required this.book, required this.model});

  final CatalogueBook book;
  final AppModel model;

  @override
  Widget build(BuildContext context) {
    final owned = model.ownedBookIds.contains(book.id);
    final installed = model.localBook(book.id) != null;

    return InkWell(
      onTap: () => _openDetail(context, model, book.id),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: SbColors.line)),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 120,
              height: 74,
              child: BookBanner(bannerUrl: resolveStoreUrl(book.bannerPath)),
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
                  const SizedBox(height: 6),
                  Wrap(
                    spacing: 8,
                    children: [
                      if (owned)
                        const StatusBadge(text: 'In your library')
                      else
                        StatusBadge(text: book.priceLabel, accent: true),
                      if (installed) const StatusBadge(text: 'Downloaded'),
                    ],
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

class StatusBadge extends StatelessWidget {
  const StatusBadge({super.key, required this.text, this.accent = false});

  final String text;
  final bool accent;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: SbColors.surface2,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: accent ? SbColors.accent : SbColors.line),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: accent ? SbColors.accent : SbColors.muted,
        ),
      ),
    );
  }
}
