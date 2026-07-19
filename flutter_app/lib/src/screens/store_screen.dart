import 'package:flutter/material.dart';

import '../app_model.dart';
import '../data/store_repository.dart';
import '../models.dart';
import '../theme.dart';
import 'book_detail_screen.dart';
import 'library_screen.dart' show BookPoster;

class StoreScreen extends StatelessWidget {
  const StoreScreen({super.key, required this.model});

  final AppModel model;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: RefreshIndicator(
        onRefresh: model.refreshStore,
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 20),
          children: [
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 16),
              child: Text('Store', style: TextStyle(fontSize: 30, fontWeight: FontWeight.w600)),
            ),
            if (model.message != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Text(model.message!, style: mutedStyle),
              ),
            if (model.isLoadingStore && model.catalogue.isEmpty)
              const Padding(
                padding: EdgeInsets.only(top: 60),
                child: Center(child: CircularProgressIndicator()),
              )
            else
              for (final book in model.catalogue) _StoreRow(book: book, model: model),
          ],
        ),
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
      onTap: () {
        Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => BookDetailScreen(model: model, bookId: book.id),
          ),
        );
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 92,
              height: 138,
              child: BookPoster(posterUrl: resolveStoreUrl(book.posterPath)),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(book.title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 2),
                  Text(book.author, style: mutedStyle),
                  Text(book.genre, style: mutedStyle),
                  const SizedBox(height: 8),
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
