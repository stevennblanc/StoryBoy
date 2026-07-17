# StoryBoy GPT Story Authoring Spec

Use this spec when generating or updating a StoryBoy gamebook.

A StoryBoy `.gbk` file is a ZIP package containing:

- `story.json`
- `poster.png`
- `banner.png`
- optional story images, such as maps, clues, sketches, diagrams, or scene art

`story.json` must be valid JSON.

## Top-Level Structure

```json
{
  "metadata": {
    "title": "The Long Shadow",
    "author": "Steven Blanc",
    "genre": "Detective Noir",
    "description": "Short book description.",
    "folder": "the_long_shadow",
    "start_node": "start",
    "version": "0.1.1-test",
    "cover_image": "poster.png",
    "title_image": "banner.png"
  },
  "evidence": [],
  "nodes": []
}
```

## Required Metadata

- `title`
- `author`
- `genre`
- `description`
- `folder`
- `start_node`
- `version`

Use `folder` as the stable book id. Example: `the_long_shadow`.

`start_node` must match an existing node id.

## Node Rules

Every node must have:

```json
{
  "id": "unique_node_id",
  "type": "text"
}
```

Node ids must be unique.

All choice targets, return targets, puzzle targets, and future node references must point to existing node ids.

## Supported Runtime Node Types

### 1. Text Node

Use text nodes for normal story, investigation, dialogue, hub, deduction, climax, and ending scenes.

```json
{
  "id": "apartment_search",
  "type": "text",
  "text": "Nathan searched the apartment.",
  "evidence": ["torn_photograph"],
  "choices": [
    {
      "text": "Inspect the desk",
      "target": "inspect_desk"
    },
    {
      "text": "Return to the street",
      "target": "street_hub"
    }
  ]
}
```

If a text node has no `choices`, StoryBoy treats it as an ending.

### 2. Lore Node

Use lore nodes for journals, case files, notes, records, and optional reading.

```json
{
  "id": "read_brother_journal",
  "type": "lore",
  "entries": [
    {
      "title": "October 3",
      "text": "Hart says I have two weeks."
    },
    {
      "title": "October 8",
      "text": "Tommy heard the tape."
    }
  ],
  "return_to": "apartment_deeper_search"
}
```

`return_to` must point to an existing node.

StoryBoy displays a Continue choice automatically.

### 3. Puzzle Node

Use puzzle nodes for typed-answer puzzles.

```json
{
  "id": "safe_puzzle",
  "type": "puzzle",
  "question": "Enter Roy Hart's three-digit safe code:",
  "answers": ["413"],
  "correct_target": "safe_opened",
  "incorrect_target": "safe_locked",
  "default_target": "safe_locked"
}
```

Answers are normalized by trimming, lowercasing, and collapsing spaces.

Example accepted answers:

```json
{
  "answers": ["long shadow", "the long shadow"]
}
```

### 4. Inventory Node

Use inventory nodes when the main purpose of the scene is gaining one or more items.

```json
{
  "id": "take_locker_key",
  "type": "inventory",
  "text": "The brass key was stamped with the number 317.",
  "items": [
    {
      "id": "locker_key",
      "title": "Locker Key",
      "description": "A brass key stamped 317."
    }
  ],
  "return_to": "apartment_search"
}
```

`return_to` is optional. If provided, StoryBoy displays a Continue choice automatically.

Inventory can also be granted from any `text`, `lore`, `puzzle`, `evidence`, or `map` node with `items`, `inventory`, `gain_inventory`, or `gains_inventory`.

### 5. Evidence Node

Use evidence nodes when the main purpose of the scene is gaining or reviewing evidence.

```json
{
  "id": "collect_roy_ledger",
  "type": "evidence",
  "text": "The ledger listed three payments from Callahan Maritime.",
  "evidence": [
    {
      "id": "roy_ledger",
      "title": "Roy's Ledger",
      "description": "A ledger showing suspicious payments tied to the docks."
    }
  ],
  "return_to": "office_hub"
}
```

`return_to` is optional. If provided, StoryBoy displays a Continue choice automatically.

Evidence can also be granted from any node with `evidence`, `gain_evidence`, or `gains_evidence`.

### 6. Map Node

Use map nodes for travel, location selection, and open investigation hubs.

```json
{
  "id": "city_map",
  "type": "map",
  "text": "Choose the next lead.",
  "image": "images/city_map.png",
  "image_caption": "Marlowe Square, Brooker Street, and the south docks.",
  "locations": [
    {
      "title": "Miles' Apartment",
      "description": "A room above a pawn shop on Brooker Street.",
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

Every `locations[].target` must point to an existing node.

## Evidence System

Evidence is optional but recommended for detective stories.

Define evidence globally:

```json
{
  "evidence": [
    {
      "id": "torn_photograph",
      "title": "Torn Photograph",
      "description": "A damaged photo of Miles with an unknown man."
    },
    {
      "id": "roy_ledger",
      "title": "Roy's Ledger",
      "description": "A ledger showing suspicious payments tied to the docks."
    }
  ]
}
```

Grant evidence from any node:

```json
{
  "id": "find_ledger",
  "type": "text",
  "text": "The ledger was hidden beneath a loose floorboard.",
  "evidence": ["roy_ledger"],
  "choices": [
    {
      "text": "Return to the office",
      "target": "office_hub"
    }
  ]
}
```

You can also define inline evidence directly on a node:

```json
{
  "id": "find_photo",
  "type": "text",
  "text": "Half a photograph was tucked behind the mirror.",
  "evidence": [
    {
      "id": "locker_key",
      "title": "Locker Key",
      "description": "A brass key stamped 317."
    }
  ],
  "choices": [
    {
      "text": "Keep searching",
      "target": "apartment_search"
    }
  ]
}
```

## Inventory System

Inventory is optional. Define important items globally when you want stable metadata:

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

Grant inventory from a node:

```json
{
  "id": "find_key",
  "type": "text",
  "text": "The key was taped beneath the drawer.",
  "items": ["locker_key"],
  "choices": [
    {
      "text": "Return to the hallway",
      "target": "hallway"
    }
  ]
}
```

You can also grant inline inventory objects:

```json
{
  "items": [
    {
      "id": "master_tape",
      "title": "Master Tape",
      "description": "The recording Miles hid before he vanished."
    }
  ]
}
```

## Node Images

Nodes may display simple images from inside the `.gbk` package. Use this for maps, drawings, clues, diagrams, symbols, letters, or meaningful story art.

Prefer grayscale or limited-palette images for e-ink compatibility, but color images are allowed.

The image file must be included in the `.gbk` package and referenced by its package path.

Single image:

```json
{
  "id": "view_city_map",
  "type": "text",
  "image": "images/city_map.png",
  "image_caption": "Marlowe Square and the warehouse district.",
  "text": "The map was marked in blue pencil.",
  "choices": [
    {
      "text": "Study the warehouse district",
      "target": "warehouse_lead"
    }
  ]
}
```

Multiple images:

```json
{
  "id": "inspect_evidence_photos",
  "type": "text",
  "images": [
    {
      "path": "images/torn_photo.png",
      "caption": "The torn photograph recovered from Miles' room."
    },
    {
      "path": "images/ledger_page.png",
      "caption": "A ledger page with three payments circled."
    }
  ],
  "text": "The evidence did not agree with Evelyn's story.",
  "choices": [
    {
      "text": "Return to the office",
      "target": "office_hub"
    }
  ]
}
```

Image rules:

- Use `.png` or `.jpg`
- Keep image dimensions reasonable for mobile and e-ink targets
- Use `poster.png` only for the library cover
- Use `banner.png` only for the cartridge/banner display
- Put in-story images in a folder such as `images/`
- Every referenced image path must exist in the `.gbk`
- Images are optional; do not add decorative images that do not help the story

## Story Design Categories

These are authoring concepts, not separate engine node types.

Use `type: "text"` for most of these:

- Introduction
- Investigation
- Dialogue
- Hub
- Deduction
- Climax
- Ending
- Character/stat changes

Use `type: "lore"` for:

- Journals
- Case files
- Police reports
- Optional documents

Use `type: "puzzle"` for:

- Codes
- Passwords
- Riddles
- Deduction answers

Use `type: "inventory"` for:

- Item pickup scenes
- Key item discovery
- Optional tool collection

Use `type: "evidence"` for:

- Evidence discovery
- Clue collection
- Case-board additions

Use `type: "map"` for:

- Travel screens
- Investigation hubs
- Open location selection

## Recommended Detective Structure

For a detective gamebook, use this flow style:

1. Introduction node
2. Case acceptance node
3. Investigation hub
4. Branches:
   - Apartment investigation
   - Jazz club investigation
   - Client investigation
   - Police station
   - Gangster investigation
5. Optional lore nodes
6. Evidence-gain nodes
7. Puzzle nodes
8. Deduction node
9. Final accusation node
10. Multiple endings

## Validation Checklist

Before packaging:

- Every node has an `id`
- Every node id is unique
- `metadata.start_node` exists
- Every `choice.target` exists
- Every `lore.return_to` exists
- Every `puzzle.correct_target` exists
- Every `puzzle.incorrect_target` exists
- Every `map.locations[].target` exists
- Every evidence id is stable and lowercase snake_case
- Every inventory id is stable and lowercase snake_case
- Every node image path exists inside the `.gbk`
- Package contains `story.json`
- Package contains `poster.png`
- Package contains `banner.png`
- Metadata uses `"cover_image": "poster.png"`
- Metadata uses `"title_image": "banner.png"`

## Packaging

Package the files as a ZIP archive and rename the archive to use the `.gbk` extension.

Example package:

```text
the_long_shadow.gbk
  story.json
  poster.png
  banner.png
  images/
    city_map.png
    torn_photo.png
    ledger_page.png
```
