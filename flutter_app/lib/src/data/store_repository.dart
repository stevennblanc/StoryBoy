import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import '../models.dart';
import 'gbk_package.dart';
import 'progress_store.dart';

const storeBaseUrl = 'https://story-boy.vercel.app';
const storeIndexUrl = '$storeBaseUrl/store/store-index.json';

String resolveStoreUrl(String pathOrUrl) {
  if (pathOrUrl.startsWith('http://') || pathOrUrl.startsWith('https://')) {
    return pathOrUrl;
  }
  return '$storeBaseUrl$pathOrUrl';
}

class StoreRepository {
  StoreRepository(this._progress);

  final ProgressStore _progress;

  SupabaseClient get _supabase => Supabase.instance.client;

  /// The store catalogue: Supabase books table first, JSON index fallback.
  Future<List<CatalogueBook>> fetchCatalogue() async {
    try {
      final rows = await _supabase
          .from('books')
          .select()
          .eq('is_published', true)
          .order('published_on', ascending: false);
      final books = rows.map(CatalogueBook.fromJson).toList();
      if (books.isNotEmpty) return books;
    } catch (_) {
      // Fall through to the static index.
    }
    final response = await http.get(Uri.parse(storeIndexUrl));
    if (response.statusCode != 200) {
      throw HttpException('Store index returned ${response.statusCode}');
    }
    final index = (jsonDecode(response.body) as Map).cast<String, dynamic>();
    return ((index['gamebooks'] as List?) ?? const [])
        .map((raw) => CatalogueBook.fromStoreIndex((raw as Map).cast<String, dynamic>()))
        .toList();
  }

  Future<Set<String>> fetchOwnedBookIds() async {
    if (_supabase.auth.currentSession == null) return const {};
    final rows = await _supabase.from('purchases').select('book_id');
    return rows.map((row) => row['book_id'] as String).toSet();
  }

  Future<void> acquireBook(String bookId) async {
    final userId = _supabase.auth.currentUser?.id;
    if (userId == null) throw StateError('Sign in first.');
    try {
      await _supabase.from('purchases').insert({'user_id': userId, 'book_id': bookId});
    } on PostgrestException catch (error) {
      if (error.code != '23505') rethrow; // already owned
    }
  }

  Future<Directory> _booksDir() async {
    final docs = await getApplicationDocumentsDirectory();
    final dir = Directory('${docs.path}/gamebooks');
    await dir.create(recursive: true);
    return dir;
  }

  Future<void> downloadBook(CatalogueBook book) async {
    final response = await http.get(Uri.parse(resolveStoreUrl(book.downloadPath)));
    if (response.statusCode != 200) {
      throw HttpException('Download failed with ${response.statusCode}');
    }
    final dir = await _booksDir();
    await File('${dir.path}/${book.id}.gbk').writeAsBytes(response.bodyBytes);
  }

  Future<void> deleteBook(String bookId) async {
    final dir = await _booksDir();
    final file = File('${dir.path}/$bookId.gbk');
    if (await file.exists()) await file.delete();
    final cacheDir = await getApplicationCacheDirectory();
    final assets = Directory('${cacheDir.path}/gbk_assets/$bookId');
    if (await assets.exists()) await assets.delete(recursive: true);
    await _progress.reset(bookId);
  }

  Future<List<LocalBook>> listLocalBooks() async {
    final dir = await _booksDir();
    final books = <LocalBook>[];
    await for (final entity in dir.list()) {
      if (entity is! File || !entity.path.endsWith('.gbk')) continue;
      final bookId = entity.uri.pathSegments.last.replaceAll('.gbk', '');
      try {
        final package = await GbkPackage.open(entity.path, bookId);
        final story = package.readStoryJson();
        final metadata = (story['metadata'] as Map?)?.cast<String, dynamic>() ?? const {};
        final poster = await package.extractArtwork(
          const ['poster.jpg', 'poster.png', 'cover.jpg', 'cover.png'],
        );
        books.add(LocalBook(
          id: (metadata['folder'] as String?) ?? bookId,
          title: (metadata['title'] as String?) ?? bookId,
          author: (metadata['author'] as String?) ?? '',
          genre: (metadata['genre'] as String?) ?? '',
          description: (metadata['description'] as String?) ?? '',
          version: (metadata['version'] as String?) ?? '',
          filePath: entity.path,
          posterFile: poster?.path,
          hasProgress: _progress.hasProgress((metadata['folder'] as String?) ?? bookId),
        ));
      } catch (_) {
        // Skip unreadable packages.
      }
    }
    books.sort((a, b) => a.title.toLowerCase().compareTo(b.title.toLowerCase()));
    return books;
  }

  Future<Map<String, String>> installedVersions() async {
    final books = await listLocalBooks();
    return {for (final book in books) book.id: book.version};
  }
}
