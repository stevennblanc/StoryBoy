# StoryBoy GPT Story Authoring Spec

Use this spec when generating or updating a StoryBoy gamebook.

A StoryBoy `.gbk` file is a ZIP package containing:

- `story.json`
- `poster.png`
- `banner.png`

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
- Inventory gain/use
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
- Every evidence id is stable and lowercase snake_case
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
```
