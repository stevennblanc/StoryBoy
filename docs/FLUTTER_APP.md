# StoryBoy Flutter App

`flutter_app/` is the Flutter client, chosen (2026-07-19) to become the primary phone app: Android now, iOS later. The Kotlin app in `app/` remains the currently shipped build until the Flutter app reaches parity and takes over the release channel.

## Identity and versioning

- `applicationId` is `com.storyboy` — the same as the Kotlin app — and the versionCode continues its sequence (`pubspec.yaml` `version: 0.20.0+21` follows Kotlin 0.19.0 = 20). This lets the existing in-app updater ship the Flutter APK as an in-place upgrade when it is ready.
- Debug builds sign with the machine's shared Android debug keystore, so a Flutter debug build installs over the Kotlin debug build directly.

## Structure

```text
lib/
  main.dart                  Supabase init + bottom-nav shell (Library, Store, Settings)
  src/theme.dart             StoryBoy palette and component themes
  src/models.dart            CatalogueBook / StoreEntry / LocalBook
  src/app_model.dart         ChangeNotifier app state (catalogue, library, auth, purchases)
  src/data/
    store_repository.dart    Supabase catalogue + JSON index fallback, downloads, purchases
    gbk_package.dart         .gbk ZIP access and asset extraction
    progress_store.dart      reading progress + preferences (shared_preferences)
  src/story/
    story_models.dart        runtime story types incl. CollectionConfig and CollectionItem
    story_parser.dart        Dart port of the story.json parser (all aliases supported)
  src/screens/
    library_screen.dart      poster grid of downloaded books
    store_screen.dart        catalogue list with price/owned badges
    book_detail_screen.dart  full-page detail (hero, stats grid, about, actions)
    reader_screen.dart       full engine: 7 node types, collections, battles, puzzles
    settings_screen.dart     Supabase account, reader text size, about
```

## Behavior contracts

- The story parser accepts the same `story.json` shape and field aliases as the Kotlin and web engines, including the `collections` block (custom labels, `show_count`, `enabled`) and item `detail`/`image` review fields. `test/story_parser_test.dart` locks the important cases.
- The store reads the Supabase `books` table first and falls back to `/store/store-index.json`; packages and artwork download from `story-boy.vercel.app`.
- Purchases and profiles use the same Supabase tables as the web and Kotlin apps — one account, one library everywhere.
- Reading progress is on-device only (parity with the other clients) — syncing it through Supabase is future work.

## Building

```powershell
cd flutter_app
flutter analyze
flutter test
flutter build apk --debug     # output: build/app/outputs/flutter-apk/app-debug.apk
```

## In-app updater

The Flutter app carries the same self-update flow as the Kotlin app: it reads
`update.json` from the latest GitHub release, compares `versionCode` against its
own build number, downloads the APK to `cacheDir/updates/`, and hands it to the
package installer through a small `storyboy/updater` MethodChannel in
`MainActivity.kt` (FileProvider authority `com.storyboy.apk_provider`, mirroring
the Kotlin app). Because the applicationId and debug signing match, the update
channel can move users from the Kotlin app to the Flutter app with a normal
release: publish a Flutter APK with a higher `versionCode` as `storyboy-debug.apk`.

## Not yet ported

- Appearance settings beyond text size (dark/light mode toggle, motion modes)
- Library filters/sort/search and cartridge list view
- Google/Facebook OAuth (needs provider setup in the Supabase dashboard first)
