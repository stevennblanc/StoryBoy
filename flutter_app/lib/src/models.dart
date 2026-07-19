/// Store catalogue row from the Supabase books table.
class CatalogueBook {
  const CatalogueBook({
    required this.id,
    required this.title,
    required this.author,
    required this.genre,
    required this.description,
    required this.about,
    required this.version,
    required this.priceUsd,
    required this.language,
    required this.publisher,
    required this.publishedOn,
    required this.nodeCount,
    required this.endingCount,
    required this.fileSizeBytes,
    required this.features,
    required this.downloadPath,
    required this.posterPath,
    required this.bannerPath,
  });

  final String id;
  final String title;
  final String author;
  final String genre;
  final String description;
  final String about;
  final String version;
  final double priceUsd;
  final String language;
  final String publisher;
  final String publishedOn;
  final int? nodeCount;
  final int? endingCount;
  final int? fileSizeBytes;
  final List<String> features;
  final String downloadPath;
  final String posterPath;
  final String bannerPath;

  bool get isFree => priceUsd <= 0;

  String get priceLabel => isFree ? 'Free' : '\$${priceUsd.toStringAsFixed(2)}';

  String? get fileSizeLabel {
    final bytes = fileSizeBytes;
    if (bytes == null) return null;
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }

  factory CatalogueBook.fromJson(Map<String, dynamic> json) {
    return CatalogueBook(
      id: json['id'] as String,
      title: (json['title'] as String?) ?? '',
      author: (json['author'] as String?) ?? '',
      genre: (json['genre'] as String?) ?? '',
      description: (json['description'] as String?) ?? '',
      about: (json['about'] as String?) ?? '',
      version: (json['version'] as String?) ?? '',
      priceUsd: ((json['price_usd'] as num?) ?? 0).toDouble(),
      language: (json['language'] as String?) ?? '',
      publisher: (json['publisher'] as String?) ?? '',
      publishedOn: (json['published_on'] as String?) ?? '',
      nodeCount: json['node_count'] as int?,
      endingCount: json['ending_count'] as int?,
      fileSizeBytes: json['file_size_bytes'] as int?,
      features: ((json['features'] as List?) ?? const []).cast<String>(),
      downloadPath: (json['download_path'] as String?) ?? '',
      posterPath: (json['poster_path'] as String?) ?? '',
      bannerPath: (json['banner_path'] as String?) ?? '',
    );
  }

  /// Builds a catalogue entry from a static store-index.json record.
  factory CatalogueBook.fromStoreIndex(Map<String, dynamic> json) {
    return CatalogueBook(
      id: json['id'] as String,
      title: (json['title'] as String?) ?? '',
      author: (json['author'] as String?) ?? '',
      genre: (json['genre'] as String?) ?? '',
      description: (json['description'] as String?) ?? '',
      about: '',
      version: (json['version'] as String?) ?? '',
      priceUsd: 0,
      language: '',
      publisher: '',
      publishedOn: '',
      nodeCount: null,
      endingCount: null,
      fileSizeBytes: null,
      features: const [],
      downloadPath: (json['downloadUrl'] as String?) ?? '',
      posterPath: (json['posterUrl'] as String?) ?? '',
      bannerPath: (json['bannerUrl'] as String?) ?? '',
    );
  }
}

/// A catalogue book combined with its on-device install state.
class StoreEntry {
  const StoreEntry({
    required this.book,
    required this.installedVersion,
    required this.owned,
  });

  final CatalogueBook book;
  final String? installedVersion;
  final bool owned;

  bool get isInstalled => installedVersion != null;
  bool get updateAvailable => installedVersion != null && installedVersion != book.version;
}

/// A downloaded gamebook on this device.
class LocalBook {
  const LocalBook({
    required this.id,
    required this.title,
    required this.author,
    required this.genre,
    required this.description,
    required this.version,
    required this.filePath,
    required this.posterFile,
    required this.hasProgress,
  });

  final String id;
  final String title;
  final String author;
  final String genre;
  final String description;
  final String version;
  final String filePath;
  final String? posterFile;
  final bool hasProgress;
}
