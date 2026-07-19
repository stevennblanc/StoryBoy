# StoryBoy Web Preview

The web preview is a static Vercel-friendly StoryBoy reader in `web/`.

Live preview:

`https://story-boy.vercel.app`

It is intentionally lightweight:

- no React, Next, or build step
- reads `store/store-index.json`
- downloads `.gbk` files from each entry's `downloadUrl`
- parses `story.json` directly from the `.gbk` ZIP package in the browser
- stores progress in browser `localStorage`
- uses static presentation by default so it stays close to the future e-ink renderer

## Local Test

From the `web/` folder, serve the directory with any static server.

Example:

```powershell
cd web
npx serve .
```

Then open the displayed local URL on desktop or Android.

## Vercel

Deploys happen through the GitHub integration: the Vercel project imports `stevennblanc/StoryBoy`, and every push to `main` deploys automatically. No Vercel CLI is needed.

The repo-root `vercel.json` sets `outputDirectory: "web"`, so the deployment serves the `web/` folder regardless of the project's dashboard Root Directory setting. It also enables `cleanUrls` and CORS headers for `/store/*`.

Production domain:

```text
https://story-boy.vercel.app
```

## Store Index

`web/store/store-index.json` is the single store index, used by both the web preview and the Android app. Its `downloadUrl`, `posterUrl`, and `bannerUrl` values are origin-relative paths:

```json
"downloadUrl": "/store/anya-suitcase-scenic-route.gbk"
```

These paths resolve against whatever host serves them — production, or `python -m http.server` during local testing — so local preview always tests the packages in the working tree. Android resolves them against its configured `STORE_INDEX_URL`.

The web store catalogue itself is served from Supabase (see `STORE_BACKEND.md`); this JSON remains as the Android source and the web offline fallback.
