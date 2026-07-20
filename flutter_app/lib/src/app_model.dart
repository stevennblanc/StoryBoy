import 'package:flutter/foundation.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import 'data/progress_store.dart';
import 'data/store_repository.dart';
import 'models.dart';

enum DisplayMode { grid, list }

enum LibrarySort { title, author }

enum ProgressFilter { all, inProgress, notStarted }

/// App-wide state: catalogue, local library, ownership, and auth session.
class AppModel extends ChangeNotifier {
  AppModel(this.progress) : repository = StoreRepository(progress) {
    displayMode = progress.displayMode == 'list' ? DisplayMode.list : DisplayMode.grid;
    Supabase.instance.client.auth.onAuthStateChange.listen((_) {
      refreshOwnership();
      notifyListeners();
    });
  }

  DisplayMode displayMode = DisplayMode.grid;
  LibrarySort librarySort = LibrarySort.title;

  void setDisplayMode(DisplayMode mode) {
    displayMode = mode;
    progress.saveDisplayMode(mode == DisplayMode.list ? 'list' : 'grid');
    notifyListeners();
  }

  void setLibrarySort(LibrarySort sort) {
    librarySort = sort;
    notifyListeners();
  }

  final ProgressStore progress;
  final StoreRepository repository;

  List<CatalogueBook> catalogue = [];
  List<LocalBook> localBooks = [];
  Set<String> ownedBookIds = {};
  bool isLoadingStore = false;
  bool isLoadingLibrary = false;
  String? message;

  Session? get session => Supabase.instance.client.auth.currentSession;
  User? get user => Supabase.instance.client.auth.currentUser;
  bool get isSignedIn => session != null;

  String get displayName {
    final metadata = user?.userMetadata;
    final name = (metadata?['display_name'] as String?)?.trim() ?? '';
    if (name.isNotEmpty) return name;
    return user?.email?.split('@').first ?? '';
  }

  CatalogueBook? catalogueBook(String bookId) {
    for (final book in catalogue) {
      if (book.id == bookId) return book;
    }
    return null;
  }

  LocalBook? localBook(String bookId) {
    for (final book in localBooks) {
      if (book.id == bookId) return book;
    }
    return null;
  }

  Future<void> refreshAll() async {
    await Future.wait([refreshLibrary(), refreshStore()]);
  }

  Future<void> refreshLibrary() async {
    isLoadingLibrary = true;
    notifyListeners();
    try {
      localBooks = await repository.listLocalBooks();
    } catch (error) {
      message = 'Could not read the library: $error';
    }
    isLoadingLibrary = false;
    notifyListeners();
  }

  Future<void> refreshStore() async {
    isLoadingStore = true;
    message = null;
    notifyListeners();
    try {
      catalogue = await repository.fetchCatalogue();
    } catch (error) {
      message = 'The store is unavailable right now.';
    }
    await refreshOwnership();
    isLoadingStore = false;
    notifyListeners();
  }

  Future<void> refreshOwnership() async {
    try {
      ownedBookIds = await repository.fetchOwnedBookIds();
    } catch (_) {
      ownedBookIds = {};
    }
    notifyListeners();
  }

  Future<void> download(CatalogueBook book) async {
    isLoadingStore = true;
    message = null;
    notifyListeners();
    try {
      await repository.downloadBook(book);
      await refreshLibrary();
    } catch (error) {
      message = 'Download failed. Check your connection and try again.';
    }
    isLoadingStore = false;
    notifyListeners();
  }

  Future<void> deleteBook(String bookId) async {
    await repository.deleteBook(bookId);
    await refreshLibrary();
  }

  Future<void> acquire(String bookId) async {
    try {
      await repository.acquireBook(bookId);
      ownedBookIds = {...ownedBookIds, bookId};
    } catch (error) {
      message = 'Could not add this book to your library.';
    }
    notifyListeners();
  }

  void markProgressChanged() {
    // Reading progress lives in ProgressStore; refresh badges.
    refreshLibrary();
  }
}
