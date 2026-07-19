# StoryBoy Architecture

StoryBoy is structured as a lightweight storybook platform rather than a single-purpose Android app.

As of 2026-07-19 the platform has three clients sharing one backend and one gamebook format:

- `app/` — the Kotlin/Compose Android app (currently shipped; feature-frozen except fixes)
- `flutter_app/` — the Flutter client, which becomes the primary phone app (see `FLUTTER_APP.md`)
- `web/` — the static web reader and store host (also the e-ink-faithful reference)

Shared contracts: the `.gbk` format (`GAMEBOOK_FORMAT.md`), the Supabase store backend (`STORE_BACKEND.md`), and the presentation standard (`PRESENTATION_MODE_STANDARD.md`).

## Current Foundation

- `com.storyboy.MainActivity` is the boot entry point only.
- `com.storyboy.MenuLauncherActivity` owns the library launcher surface only.
- `com.storyboy.StoryEngineActivity` is reserved for game runtime execution.
- `com.storyboy.MenuSettingsActivity` is reserved for configurable settings.
- `com.storyboy.core` contains cross-application configuration, theming, and navigation contracts.

## Module Boundaries

- Launcher code must not execute stories.
- Engine code must not know about launcher browsing UI.
- Node behavior belongs under `nodes`, with node types kept independently extensible.
- Raw data access belongs under `data`.
- Repository contracts belong under `repository`.
- Reusable Compose elements belong under `widgets`.
- Generic helpers only belong under `utils`.

## Early Recommendations

1. Keep `UiConfig`, `AppConfig`, `ThemeManager`, and `Navigation` in `core`, not `utils`.
2. Introduce repository interfaces before adding concrete JSON, file, or database loading.
3. Model story nodes as a typed runtime contract instead of a large conditional inside the engine.
4. Keep Activity classes thin and move real screen UI into feature packages as each feature grows.
5. Treat Android APIs as adapters around portable story/runtime logic so future e-ink or embedded targets remain realistic.
