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
| `battle` | Physical or tactical conflict | Drafted |

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
