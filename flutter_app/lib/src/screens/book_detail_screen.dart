import 'package:flutter/material.dart';

import '../app_model.dart';
import '../data/store_repository.dart';
import '../models.dart';
import '../theme.dart';
import 'library_screen.dart' show BookPoster;
import 'reader_screen.dart';
import 'store_screen.dart' show StatusBadge;

/// Full-page book detail, shared by the store and the library.
class BookDetailScreen extends StatelessWidget {
  const BookDetailScreen({super.key, required this.model, required this.bookId});

  final AppModel model;
  final String bookId;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: model,
      builder: (context, _) {
        final catalogue = model.catalogueBook(bookId);
        final local = model.localBook(bookId);
        final owned = model.ownedBookIds.contains(bookId);
        final title = catalogue?.title ?? local?.title ?? bookId;

        return Scaffold(
          appBar: AppBar(title: const Text('')),
          body: SafeArea(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 32),
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    SizedBox(
                      width: 132,
                      height: 198,
                      child: BookPoster(
                        posterFile: local?.posterFile,
                        posterUrl: catalogue == null ? null : resolveStoreUrl(catalogue.posterPath),
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(title, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w700)),
                          const SizedBox(height: 6),
                          Text(catalogue?.author ?? local?.author ?? '', style: mutedStyle),
                          Text(catalogue?.genre ?? local?.genre ?? '', style: mutedStyle),
                          const SizedBox(height: 10),
                          Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: [
                              if (owned)
                                const StatusBadge(text: 'In your library')
                              else if (catalogue != null)
                                StatusBadge(text: catalogue.priceLabel, accent: true),
                              if (local != null && model.progress.hasProgress(bookId))
                                const StatusBadge(text: 'In progress'),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                _Actions(model: model, catalogue: catalogue, local: local, owned: owned),
                const SizedBox(height: 20),
                Text(
                  catalogue?.description ?? local?.description ?? '',
                  style: readerTextStyle.copyWith(fontSize: 16),
                ),
                if (catalogue != null && catalogue.features.isNotEmpty) ...[
                  const SizedBox(height: 16),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final feature in catalogue.features) StatusBadge(text: feature),
                    ],
                  ),
                ],
                if (catalogue != null) ...[
                  const SizedBox(height: 20),
                  _StatsPanel(book: catalogue, installedVersion: local?.version),
                  if (catalogue.about.isNotEmpty) ...[
                    const SizedBox(height: 16),
                    _SectionPanel(
                      title: 'About this book',
                      child: Text(catalogue.about, style: readerTextStyle.copyWith(fontSize: 16)),
                    ),
                  ],
                ],
                if (local != null) ...[
                  const SizedBox(height: 24),
                  TextButton(
                    onPressed: () async {
                      await model.deleteBook(bookId);
                      if (context.mounted) Navigator.of(context).pop();
                    },
                    child: const Text('Delete from device', style: TextStyle(color: SbColors.danger)),
                  ),
                ],
              ],
            ),
          ),
        );
      },
    );
  }
}

class _Actions extends StatelessWidget {
  const _Actions({required this.model, required this.catalogue, required this.local, required this.owned});

  final AppModel model;
  final CatalogueBook? catalogue;
  final LocalBook? local;
  final bool owned;

  @override
  Widget build(BuildContext context) {
    final updateAvailable =
        local != null && catalogue != null && local!.version != catalogue!.version;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (local != null)
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: SbColors.accent,
              foregroundColor: SbColors.accentText,
            ),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => ReaderScreen(model: model, book: local!),
                ),
              );
            },
            child: Text(model.progress.hasProgress(local!.id) ? 'Continue reading' : 'Read'),
          ),
        if (catalogue != null && (local == null || updateAvailable)) ...[
          if (local != null) const SizedBox(height: 10),
          FilledButton(
            onPressed: model.isLoadingStore ? null : () => model.download(catalogue!),
            child: Text(updateAvailable ? 'Update downloaded copy' : 'Download for offline play'),
          ),
        ],
        if (catalogue != null && !owned) ...[
          const SizedBox(height: 6),
          if (model.isSignedIn)
            TextButton(
              onPressed: () => model.acquire(catalogue!.id),
              child: Text('Add to StoryBoy library — ${catalogue!.priceLabel}'),
            )
          else
            const Padding(
              padding: EdgeInsets.only(top: 4),
              child: Text(
                'Sign in from Settings to keep your library across devices.',
                style: mutedStyle,
                textAlign: TextAlign.center,
              ),
            ),
        ],
      ],
    );
  }
}

class _StatsPanel extends StatelessWidget {
  const _StatsPanel({required this.book, required this.installedVersion});

  final CatalogueBook book;
  final String? installedVersion;

  @override
  Widget build(BuildContext context) {
    final stats = <(String, String)>[
      if (book.nodeCount != null) ('Length', '${book.nodeCount} passages'),
      if (book.endingCount != null) ('Endings', '${book.endingCount}'),
      if (book.language.isNotEmpty) ('Language', book.language),
      if (book.fileSizeLabel != null) ('Size', book.fileSizeLabel!),
      if (book.version.isNotEmpty) ('Version', book.version),
      if (book.publishedOn.isNotEmpty) ('Published', book.publishedOn),
      if (book.publisher.isNotEmpty) ('Publisher', book.publisher),
      if (installedVersion != null) ('Installed', installedVersion!),
    ];
    if (stats.isEmpty) return const SizedBox.shrink();

    return _SectionPanel(
      title: 'Book details',
      child: Wrap(
        spacing: 10,
        runSpacing: 10,
        children: [
          for (final (label, value) in stats)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                border: Border.all(color: SbColors.line),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Column(
                children: [
                  Text(label, style: mutedStyle.copyWith(fontSize: 12)),
                  Text(value, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

class _SectionPanel extends StatelessWidget {
  const _SectionPanel({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: SbColors.surface,
        border: Border.all(color: SbColors.line),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
          const SizedBox(height: 10),
          child,
        ],
      ),
    );
  }
}
