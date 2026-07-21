import 'dart:convert';
import 'dart:io';

import 'package:archive/archive.dart';
import 'package:path_provider/path_provider.dart';

import '../story/story_models.dart';
import '../story/story_parser.dart';

/// Opens a downloaded .gbk package (a ZIP with story.json and images).
class GbkPackage {
  GbkPackage._(this.bookId, this._archive);

  final String bookId;
  final Archive _archive;

  static Future<GbkPackage> open(String filePath, String bookId) async {
    final bytes = await File(filePath).readAsBytes();
    return GbkPackage._(bookId, ZipDecoder().decodeBytes(bytes));
  }

  Map<String, dynamic> readStoryJson() {
    final entry = _archive.findFile('story.json');
    if (entry == null) throw const FormatException('Package is missing story.json');
    return (jsonDecode(utf8.decode(entry.content as List<int>)) as Map).cast<String, dynamic>();
  }

  StoryGamebook readStory() => parseStoryGamebook(readStoryJson());

  /// Extracts a package asset to the cache directory and returns its file,
  /// or null when the path does not exist in the package.
  Future<File?> extractAsset(String assetPath) async {
    final normalized = assetPath.replaceAll('\\', '/');
    final entry = _archive.findFile(normalized);
    if (entry == null) return null;
    final cacheDir = await getApplicationCacheDirectory();
    final target = File('${cacheDir.path}/gbk_assets/$bookId/${normalized.split('/').last}');
    final content = entry.content as List<int>;
    // Re-extract when the packaged asset differs from the cached copy: an
    // updated book often reuses filenames (poster.jpg, map tiles) with new art.
    var shouldWrite = true;
    if (await target.exists()) {
      shouldWrite = await target.length() != content.length;
    }
    if (shouldWrite) {
      await target.create(recursive: true);
      await target.writeAsBytes(content);
    }
    return target;
  }

  /// Extracts the first matching artwork candidate, or null.
  Future<File?> extractArtwork(List<String> candidates) async {
    for (final candidate in candidates) {
      final file = await extractAsset(candidate);
      if (file != null) return file;
    }
    return null;
  }
}
