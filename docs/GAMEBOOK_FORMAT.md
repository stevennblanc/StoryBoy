# Gamebook Files

StoryBoy local adventures use the `.gbk` extension.

A `.gbk` file is a ZIP package with a validated `story.json` file inside. It may also include images such as `poster.png` and `banner.png`.

`poster.png` is the standard vertical cover image for book-style library display.

`banner.png` is the standard horizontal image for cartridge-style library display.

The Android app stores downloaded gamebooks in its app-specific internal storage:

`context.filesDir/gamebooks`

This keeps adventures available offline without requesting broad storage permissions.

## Minimum Package Shape

```text
first-adventure.gbk
  story.json
  poster.png
  banner.png
```

## Minimum `story.json`

```json
{
  "metadata": {
    "title": "First Adventure",
    "author": "StoryBoy",
    "genre": "Fantasy",
    "description": "A small test adventure.",
    "folder": "first_adventure",
    "start_node": "start",
    "version": "1.0.0"
  },
  "nodes": [
    {
      "id": "start",
      "type": "lore",
      "text": "The story begins.",
      "choices": []
    }
  ]
}
```

StoryBoy rejects files that:

- do not use the `.gbk` extension
- are not ZIP packages
- do not contain `story.json`
- do not contain `metadata`
- do not contain a non-empty `nodes` array
- reference a `metadata.start_node` that is missing from `nodes`

Artwork files are optional, but the preferred names are:

- `poster.png`, `poster.jpg`, `cover.png`, or `cover.jpg`
- `banner.png` or `banner.jpg`

## Store Index

The future free-adventure repository should publish a `store-index.json` file.

```json
{
  "gamebooks": [
    {
      "id": "first-adventure",
      "title": "First Adventure",
      "author": "StoryBoy",
      "genre": "Fantasy",
      "version": "1.0.0",
      "description": "A small test adventure.",
      "downloadUrl": "https://example.com/first-adventure.gbk"
    }
  ]
}
```
