# StoryBoy Story Design Standard

StoryBoy separates runtime node types from story design categories.

Runtime node types are the small set of structures the engine must execute. Story design categories are authoring concepts that describe why a node exists in the adventure.

## Runtime Node Types

| Node Type | Purpose | Current Status |
| --- | --- | --- |
| `text` | Narrative, branching, endings, dialogue-like prose | Implemented |
| `lore` | Optional readable records with a return target | Implemented |
| `puzzle` | Player answer input with correct and incorrect targets | Implemented |
| `inventory` | Grants one or more items and optionally returns to a node | Implemented |
| `evidence` | Grants one or more evidence items and optionally returns to a node | Implemented |
| `map` | Presents travel or investigation location choices | Implemented |
| `battle` | Dice-based physical, tactical, or pressure conflict | Implemented |

Inventory, stats, evidence, triggers, and flags should usually be properties or systems attached to nodes rather than separate runtime node classes.

## Story Design Categories

These categories help authors structure books. They do not require separate engine classes.

| Category | Purpose |
| --- | --- |
| Introduction | Sets atmosphere, premise, and initial stakes |
| Investigation | Primary exploration and clue discovery |
| Dialogue | Conversations that branch or reveal information |
| Lore | Optional reading and records |
| Inventory | Gain or use items |
| Evidence | Gain or present investigative proof |
| Puzzle | Logical challenge |
| Character | Relationship, reputation, or stat changes |
| Map | Travel and location choice |
| Battle | Physical confrontation |
| Chase | Timed or pressure decisions |
| Hub | Choose the next lead or activity |
| Deduction | Player makes a final theory |
| Climax | Final confrontation |
| Ending | Story resolution |

## Evidence System

Evidence is a first-class story system for investigative adventures.

Books may define evidence globally and grant it from any node. The engine persists collected evidence separately from the current node so future deduction and accusation flows can require specific proof.

Example uses:

- collect a torn photograph
- add a ledger entry to the evidence board
- require the master tape before a true ending
- ask the player which evidence proves an accusation

Genres that do not need evidence can ignore the system entirely.

## Artwork Standards

Gamebooks should include package artwork for both supported library views.

| File | Purpose | Aspect Ratio | Recommended Size | Minimum Size |
| --- | --- | --- | --- | --- |
| `poster.png` | Vertical cover for book/grid display | `2:3` | `1024 x 1536` | `600 x 900` |
| `banner.png` | Horizontal art for cartridge/list display | `3:2` | `1536 x 1024` | `900 x 600` |

Artwork can be in color, but should remain readable in grayscale for e-ink compatibility. Keep important text and faces away from edges because display modes may crop slightly.

See [Gamebook Files](GAMEBOOK_FORMAT.md) for the full package artwork rules.

## Battle System

Battle nodes are a lightweight dice system for games that want risk without requiring a full RPG rules engine.

The engine compares a player roll against an opponent roll. Inventory can add preparation bonuses, so a player who found useful gear has better odds without forcing every story to use character stats.

Good uses:

- a short fight
- forcing a door under pressure
- escaping pursuit
- surviving a hazard
- resolving a risky tactic

Books that do not need chance-based scenes can ignore battle nodes entirely.

## Presentation and Motion

StoryBoy must support both richer Android presentation and motion-free e-ink presentation.

Runtime nodes should produce story state and semantic events, not animation instructions. For example, a battle node can produce a `BattleResolved` event, but the book should not request a dice animation directly.

Every animated Android treatment must have a static equivalent for e-ink and reduced-motion use.

See [StoryBoy Presentation Mode Standard](PRESENTATION_MODE_STANDARD.md).
