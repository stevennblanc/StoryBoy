# StoryBoy Web Preview

The web preview is a static Vercel-friendly StoryBoy reader in `web/`.

Live preview:

`https://storyboy.vercel.app`

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

Create the Vercel project with `web/` as the project root.

The project has no build command and no output directory because the files are already static.

Recommended Vercel settings:

- Framework Preset: Other
- Root Directory: `web`
- Build Command: empty
- Output Directory: empty

This workspace was first deployed as:

```text
https://storyboy.vercel.app
```

If the Vercel CLI asks for authentication, run:

```powershell
vercel login
```

Then deploy from the repository root:

```powershell
vercel deploy web --prod
```

## Store Index

`web/store/store-index.json` should mirror the public store index.

For production testing, keep `downloadUrl`, `posterUrl`, and `bannerUrl` pointed at the same hosted assets used by Android.
