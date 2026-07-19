import 'dart:io';

import 'package:flutter/material.dart';

import '../app_model.dart';
import '../models.dart';
import '../theme.dart';
import 'book_detail_screen.dart';

class LibraryScreen extends StatelessWidget {
  const LibraryScreen({super.key, required this.model});

  final AppModel model;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: RefreshIndicator(
        onRefresh: model.refreshLibrary,
        child: CustomScrollView(
          slivers: [
            const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.fromLTRB(20, 16, 20, 12),
                child: Text('Library', style: TextStyle(fontSize: 30, fontWeight: FontWeight.w600)),
              ),
            ),
            if (model.isLoadingLibrary && model.localBooks.isEmpty)
              const SliverFillRemaining(
                child: Center(child: CircularProgressIndicator()),
              )
            else if (model.localBooks.isEmpty)
              SliverFillRemaining(
                hasScrollBody: false,
                child: Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text('No gamebooks downloaded', style: TextStyle(fontSize: 20)),
                      const SizedBox(height: 8),
                      const Text(
                        'Browse the store to download a free adventure.',
                        style: mutedStyle,
                      ),
                    ],
                  ),
                ),
              )
            else
              SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                sliver: SliverGrid(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 3,
                    mainAxisSpacing: 12,
                    crossAxisSpacing: 12,
                    childAspectRatio: 0.6,
                  ),
                  delegate: SliverChildBuilderDelegate(
                    childCount: model.localBooks.length,
                    (context, index) {
                      final book = model.localBooks[index];
                      return _LibraryTile(book: book, model: model);
                    },
                  ),
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
      onTap: () {
        Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => BookDetailScreen(model: model, bookId: book.id),
          ),
        );
      },
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(child: BookPoster(posterFile: book.posterFile)),
          const SizedBox(height: 6),
          Text(
            book.title,
            maxLines: 2,
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
        errorBuilder: (_, _, _) => const _PosterPlaceholder(),
      );
    } else {
      child = const _PosterPlaceholder();
    }
    return ClipRRect(
      borderRadius: BorderRadius.circular(6),
      child: Container(
        decoration: BoxDecoration(
          color: SbColors.surface,
          border: Border.all(color: SbColors.line),
          borderRadius: BorderRadius.circular(6),
        ),
        width: double.infinity,
        child: child,
      ),
    );
  }
}

class _PosterPlaceholder extends StatelessWidget {
  const _PosterPlaceholder();

  @override
  Widget build(BuildContext context) {
    return const Center(child: Text('gbk', style: mutedStyle));
  }
}
