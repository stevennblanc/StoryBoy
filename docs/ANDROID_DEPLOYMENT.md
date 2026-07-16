# Android Deployment

StoryBoy can be sideloaded directly from the debug APK during early development.

## Current Host Recommendation

Use GitHub Releases under `stevennblanc/StoryBoy`.

The app is configured to check:

`https://github.com/stevennblanc/StoryBoy/releases/latest/download/update.json`

Each release should include:

- `update.json`
- `storyboy-debug.apk`

## Update Manifest

The manifest format is:

```json
{
  "versionCode": 1,
  "versionName": "0.1.0",
  "apkUrl": "https://github.com/stevennblanc/StoryBoy/releases/latest/download/storyboy-debug.apk",
  "releaseNotes": "Initial StoryBoy Android foundation."
}
```

Increment `versionCode` before publishing a new update.

## Publish

Authenticate GitHub CLI as `stevennblanc`, then run:

```powershell
.\scripts\publish-github-release.ps1 -Tag v0.1.0
```

The script refuses to publish unless the GitHub CLI account matches `stevennblanc`.
