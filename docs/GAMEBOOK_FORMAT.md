# Gamebook Files

StoryBoy local adventures are JSON files with the `.gamebook` extension.

The Android app stores downloaded gamebooks in its app-specific internal storage:

`context.filesDir/gamebooks`

This keeps adventures available offline without requesting broad storage permissions.

## Minimum File Shape

```json
{
  "format": "storyboy.gamebook",
  "formatVersion": 1,
  "id": "first-adventure",
  "title": "First Adventure",
  "author": "StoryBoy",
  "version": "1.0.0",
  "description": "A small test adventure.",
  "startNodeId": "start",
  "nodes": {
    "start": {
      "type": "lore",
      "text": "The story begins."
    }
  }
}
```

StoryBoy rejects files that:

- do not use the `.gamebook` extension
- do not declare `format` as `storyboy.gamebook`
- do not have `formatVersion` 1 or higher
- do not contain a `nodes` object
- reference a `startNodeId` that is missing from `nodes`

## Store Index

The future free-adventure repository should publish a `store-index.json` file.

```json
{
  "gamebooks": [
    {
      "id": "first-adventure",
      "title": "First Adventure",
      "author": "StoryBoy",
      "version": "1.0.0",
      "description": "A small test adventure.",
      "downloadUrl": "https://example.com/first-adventure.gamebook"
    }
  ]
}
```
