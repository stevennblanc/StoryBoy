# Gamebook Files

StoryBoy local adventures use the `.gbk` extension.

A `.gbk` file is a ZIP package with a validated `story.json` file inside. It may also include images such as `poster.png` and `banner.png`.

`poster.png` is the standard vertical cover image for book-style library display.

`banner.png` is the standard horizontal image for cartridge-style library display.

Additional story images may be packaged and referenced by nodes. These are useful for maps, clue drawings, diagrams, letters, and other meaningful visuals.

The Android app stores downloaded gamebooks in its app-specific internal storage:

`context.filesDir/gamebooks`

This keeps adventures available offline without requesting broad storage permissions.

Downloaded gamebooks can be deleted from the Library detail page. Store entries compare their online `version` against the installed package version and offer an update when they differ.

## Minimum Package Shape

```text
first-adventure.gbk
  story.json
  poster.png
  banner.png
  images/
    map.png
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
  "evidence": [
    {
      "id": "torn_photograph",
      "title": "Torn Photograph",
      "description": "A damaged photo recovered during the investigation."
    }
  ],
  "nodes": [
    {
      "id": "start",
      "type": "lore",
      "image": "images/map.png",
      "image_caption": "A hand-drawn map.",
      "text": "The story begins.",
      "evidence": ["torn_photograph"],
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

## Evidence

Evidence is optional. A book may define a top-level `evidence` catalog and grant evidence from any node.

```json
{
  "evidence": [
    {
      "id": "roy_ledger",
      "title": "Roy's Ledger",
      "description": "A ledger showing payments tied to the docks."
    }
  ],
  "nodes": [
    {
      "id": "find_ledger",
      "type": "text",
      "text": "The ledger was hidden under the loose floorboard.",
      "evidence": ["roy_ledger"],
      "choices": [
        {
          "text": "Return to the office",
          "target": "office"
        }
      ]
    }
  ]
}
```

## Node Images

Nodes may reference a single image:

```json
{
  "id": "view_map",
  "type": "text",
  "image": "images/city_map.png",
  "image_caption": "The south docks and Marlowe Square.",
  "text": "The map had three locations circled.",
  "choices": [
    {
      "text": "Continue",
      "target": "office_hub"
    }
  ]
}
```

Nodes may also reference multiple images:

```json
{
  "id": "view_photos",
  "type": "text",
  "images": [
    {
      "path": "images/torn_photo.png",
      "caption": "The torn photograph."
    },
    {
      "path": "images/ledger_page.png",
      "caption": "Roy's ledger."
    }
  ],
  "text": "The evidence told its own story."
}
```

Referenced image paths must exist inside the `.gbk` package.

## Inventory

Inventory is optional. Define inventory globally:

```json
{
  "inventory": [
    {
      "id": "locker_key",
      "title": "Locker Key",
      "description": "A brass key stamped 317."
    }
  ]
}
```

Grant inventory from any node with `items`, `inventory`, `gain_inventory`, or `gains_inventory`.

```json
{
  "id": "take_key",
  "type": "inventory",
  "text": "The key was hidden under the ashtray.",
  "items": ["locker_key"],
  "return_to": "apartment_search"
}
```

## Map Nodes

Use map nodes for travel or investigation hubs.

```json
{
  "id": "city_map",
  "type": "map",
  "text": "Choose the next lead.",
  "image": "images/city_map.png",
  "locations": [
    {
      "title": "Miles' Apartment",
      "description": "A room above a pawn shop.",
      "target": "apartment_investigation"
    },
    {
      "title": "The Crescent Room",
      "description": "The jazz club where Miles was last seen.",
      "target": "jazz_club"
    }
  ]
}
```

Every map location target must exist.

Nodes may also grant inline evidence objects:

```json
{
  "id": "find_photo",
  "type": "text",
  "text": "Half a photograph was tucked behind the mirror.",
  "evidence": [
    {
      "id": "torn_photo",
      "title": "Torn Photograph",
      "description": "The missing half of Miles' photograph."
    }
  ]
}
```

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
