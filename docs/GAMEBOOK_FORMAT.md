# Gamebook Files

StoryBoy local adventures use the `.gbk` extension.

A `.gbk` file is a ZIP package with a validated `story.json` file inside. It may also include images such as `poster.png` and `banner.png`.

`poster.png` is the standard vertical cover image for book-style library display.

`banner.png` is the standard horizontal image for cartridge-style library display.

Additional story images may be packaged and referenced by nodes. These are useful for maps, clue drawings, diagrams, letters, and other meaningful visuals.

## Artwork Standards

StoryBoy uses two standard package artwork files:

| File | Purpose | Aspect Ratio | Recommended Size | Minimum Size |
| --- | --- | --- | --- | --- |
| `poster.png` | Vertical cover for book/grid library display | `2:3` | `1024 x 1536` | `600 x 900` |
| `banner.png` | Horizontal art for cartridge/list display | `3:2` | `1536 x 1024` | `900 x 600` |

Rules:

- `.jpg` is preferred for painted or photographic artwork; `.png` only when hard edges or transparency matter
- Keep story images at or below `1280px` on the long side; a full package should stay under roughly 10 MB
- `python scripts/compress_gamebook.py book.gbk` re-encodes an existing package to these standards and rewrites all image references
- Keep titles, logos, and important faces away from the edges because display modes may crop slightly
- Covers may use color, but should remain readable in grayscale for e-ink compatibility
- Avoid relying on color alone for important information
- Use high contrast text and simple compositions when possible
- `poster.png` and `banner.png` should represent the same gamebook, but they do not need to use identical artwork

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

## Collections

StoryBoy has two built-in collection systems: inventory and evidence. Both are optional, and a book can rename them to fit its genre with a top-level `collections` block.

```json
{
  "collections": {
    "inventory": {
      "label": "Souvenirs",
      "show_count": false
    },
    "evidence": {
      "label": "Memories"
    }
  }
}
```

Rules:

- `label` renames the reader button and panel. Defaults are `Items` and `Evidence`.
- `show_count` hides the collected count on the reader button when `false`. Defaults to `true`.
- `enabled` forces a collection on or off. When omitted, a collection is shown only if the book defines or grants anything in it, so books that never use evidence never show an evidence button.
- `collections.items` is accepted as an alias for `collections.inventory`.

Evidence does not have to mean detective proof. A travel book can call it `Memories`, a horror book `Visions`, a family story `Photographs`. The engine behavior is identical; only the wording changes.

## Reviewing Collected Items

Catalog entries in both `inventory` and `evidence` may include optional `detail` and `image` fields:

```json
{
  "inventory": [
    {
      "id": "baggage_receipt",
      "title": "Baggage Receipt",
      "description": "The printed claim receipt for Anya's suitcase.",
      "detail": "Printed claim: TAG 782461 - OWNER: A. RAMSINGH. The last four digits are 2461.",
      "image": "images/baggage_receipt.png"
    }
  ]
}
```

The reader shows `title` and `description` in the collection panel. Selecting an entry reveals its `image` and `detail` so the player can review the actual contents of a clue — a code, a map, a phone number — instead of only knowing they collected it. `image` must be a path inside the `.gbk` package.

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

## Character Stats

A book may define numeric stats (health, armor, a currency, or anything). Stats are opt-in: a book that defines none shows no stat bar.

```json
{
  "stats": [
    { "id": "hp", "label": "Health", "start": 8, "max": 8, "role": "health" },
    { "id": "ac", "label": "Armor Class", "start": 9 }
  ]
}
```

- `label` is the display name and every default can be overridden — a sci-fi book can label `hp` "Shields" and a currency "Credits". If omitted, the label falls back to a title-cased `id`.
- `role: "health"` marks the stat whose reaching zero ends the run (only one is needed). `max` caps the value.
- `hidden: true` tracks a stat without showing it in the bar (useful for flags).

Any node can change stats when entered:

```json
{ "id": "trap_room", "type": "text", "text": "The dart grazes you.",
  "stat_changes": { "hp": -3, "gold": 10 },
  "set_stats": { "torch": 0 } }
```

`stat_changes` adds/subtracts; `set_stats` assigns an absolute value. Values are clamped to `[0, max]`.

## Check Nodes

A check is a single roll — a saving throw or luck test — that routes on success or failure. Lighter than combat; good for hazards, leaps, and locks.

```json
{ "id": "bridge", "type": "check",
  "text": "The bridge gives way. Keep your balance!",
  "dice": "1d20", "modifier": 0, "target": 11,
  "success_target": "gallery", "failure_target": "slip" }
```

- Roll `dice` + `modifier` (+ optional `stat_modifier` stat value); meet or beat `target` for success.
- `success_label` / `failure_label` customize the result heading.

## Combat Nodes

Round-based combat against a single enemy. The player rolls to hit the enemy; the enemy rolls to hit the player's health stat. Reaching zero enemy HP wins; the health stat reaching zero routes to the lose target (typically a death ending).

```json
{ "id": "fight_gloamworm", "type": "combat", "text": "The gloamworm strikes!",
  "enemy": { "label": "Gloamworm", "hp": 6, "hit_target": 9, "damage": "1d4", "hits_on": 13 },
  "player": { "damage": "1d6", "damage_bonus": 2, "hit_bonus": 0 },
  "health_stat": "hp",
  "win_target": "vault", "lose_target": "death", "flee_target": "flee_out" }
```

- `enemy.hit_target` — the number the player must roll (1d20 + `player.hit_bonus`) to hit.
- `enemy.hits_on` — the number the enemy must roll to hit the player.
- `flee_target` (optional) lets the player run, taking a free enemy attack; `talk_target` adds a talk option.

Combat is heavier than a `battle` node (an opposed one-roll luck check) — use `battle`/`check` for quick chance beats and `combat` for real fights.

## Currency, Equipment, and Shops

**Currency** is just a stat, so it needs no special system — define a `gold` stat (rename it "Credits", "Emberium", anything) and grant it with `stat_changes`. A shop spends from a named stat.

**Equipment** is a third collection alongside inventory and evidence, with its own renamable config (`collections.equipment.label`, default "Equipment"; "Gear", "Loadout", etc.). Equipment items add these fields:

```json
{
  "equipment": [
    { "id": "chain_mail", "title": "Chain Mail", "slot": "armor", "equip_effects": { "ac": 2 } },
    { "id": "short_sword", "title": "Short Sword", "slot": "weapon", "damage": "1d6", "damage_bonus": 1, "hit_bonus": 0 }
  ]
}
```

- `slot` makes an item equippable; only one item per slot is active at a time.
- `equip_effects` are stat deltas applied while equipped — displayed and used stat values are the base value plus everything equipped.
- `damage` / `damage_bonus` / `hit_bonus` on an equipped weapon drive combat.

For equipment to matter in combat, give the defense stat `role: "armor"` and point the combat node at it:

```json
{
  "stats": [
    { "id": "ac", "label": "Armor Class", "start": 9, "role": "armor" }
  ],
  "nodes": [
    { "id": "fight", "type": "combat", "text": "It lunges!",
      "enemy": { "label": "Gloamworm", "hp": 7, "hit_target": 9, "damage": "1d4", "attack_bonus": 2 },
      "player": { "damage": "1d3", "hit_bonus": 0 },
      "armor_stat": "ac",
      "win_target": "won", "lose_target": "died" }
  ]
}
```

With `armor_stat` set, the enemy must roll `1d20 + attack_bonus` >= your effective armor stat, so equipped armor makes you harder to hit and an equipped weapon replaces the node's `player.damage`. Without `armor_stat`, combat uses the fixed `monster_hits_on` from stage 1.

**Shop nodes** sell catalog items for a currency stat:

```json
{ "id": "smithy", "type": "shop", "text": "Buy before you delve.",
  "currency_stat": "gold",
  "items": [
    { "equipment": "chain_mail", "price": 40 },
    { "inventory": "torch", "price": 1 }
  ],
  "return_target": "town", "leave_label": "Head out" }
```

Each item references an `equipment` or `inventory` catalog id plus a `price`; buying deducts the currency stat and grants the item (which the player then equips from the equipment panel). Already-owned items show as owned.

## Revealing Map

A book can define a **map** that fills in as the player explores. Fragments live in a top-level `map` catalog (id, title, image); nodes reveal them on entry with `reveal_map`. The reader assembles the revealed fragments, in catalog order, in a Map panel the player can open any time.

```json
{
  "collections": { "map": { "label": "Map" } },
  "map": [
    { "id": "frag_entrance", "title": "Entrance", "image": "images/map_entrance.jpg" },
    { "id": "frag_gallery", "title": "Gallery & Vault", "image": "images/map_gallery.jpg" }
  ],
  "nodes": [
    { "id": "landing", "type": "text", "text": "You step inside.",
      "reveal_map": "frag_entrance",
      "choices": [ { "text": "On", "target": "gallery" } ] },
    { "id": "gallery", "type": "text", "text": "A long gallery.",
      "reveal_map": ["frag_gallery"] }
  ]
}
```

- Fragment art is any package image — hand-drawn map slices work well. Each mapped room reveals its piece, so the map "draws itself" as the player goes.
- `reveal_map` accepts a fragment id or a list. Revealing an id already shown does nothing.
- The map label is renamable (`collections.map.label` — "Star Chart", "Trail"), and the whole system is hidden unless the book defines fragments or reveals them.

This is separate from the `map` **node type** (a location/travel hub with `locations`). One is a picture that assembles as you explore; the other is a branching choice screen. A book can use either, both, or neither.

## Conditional Choices and Story Flags

Any choice (or map location) can carry a `requires` block. If the conditions aren't met the choice is **hidden**; add `locked_text` to show it disabled with a hint instead.

```json
{
  "id": "cell_door", "type": "text", "text": "The barred door will not budge.",
  "choices": [
    { "text": "Unlock the door", "target": "beyond",
      "requires": { "item": "iron_key" } },

    { "text": "Bribe the guard", "target": "bribed",
      "requires": { "stat": { "gold": { "gt": 26 } } },
      "locked_text": "Bribe the guard (needs 27 gold)" },

    { "text": "Speak the binding word", "target": "banish",
      "requires": { "character": "witch" } },

    { "text": "Loot the corpse", "target": "loot",
      "requires": { "not_flag": "worm_looted" } }
  ]
}
```

Supported predicates (all listed must pass):

| Key | Meaning |
| --- | --- |
| `item` / `not_item` | player holds (or doesn't hold) an inventory item |
| `equipment` | owns a piece of equipment |
| `equipped` | has it equipped right now |
| `evidence` | holds an evidence entry |
| `character` / `class` | the chosen character's id |
| `flag` / `not_flag` | a story flag is raised / not raised |
| `stat` | numeric comparison per stat: `min`/`at_least`, `max`/`at_most`, `gt`, `lt`, `equals` |

**Story flags** are free-form — no declaration needed. Any node raises or clears them on entry:

```json
{ "id": "worm_dead", "type": "text", "text": "The worm sags.",
  "set_flags": ["worm_killed"], "clear_flags": ["worm_stalking"] }
```

Flags are the tool for stopping repeatable loops: gate the entrance to a fight with `"not_flag": "worm_killed"` and the monster can't be farmed. They also record what the player has seen or done, so later scenes can react to it. Stat comparisons use the **effective** value (base plus equipped bonuses).

## Ability Scores and Character Choice

A stat can be an **ability score**: a raw value (typically 1–18) that maps to a small modifier. Combat and checks add the *modifier*, not the raw value, so a strong warrior hits harder and a clever witch reads wards better.

```json
{
  "stats": [
    { "id": "str", "label": "Strength", "start": 10, "ability": true },
    { "id": "int", "label": "Intellect", "start": 10, "ability": true }
  ]
}
```

StoryBoy's default tiers (its own numbers): ≤3 → −3, 4–5 → −2, 6–8 → −1, 9–12 → 0, 13–15 → +1, 16–17 → +2, ≥18 → +3. Override per stat with a `modifier_table` of `{ min, max, mod }` bands. A plain (non-ability) stat's own value is its modifier, so existing `stat_modifier` checks are unchanged.

Feed ability scores into play:

- **Combat**: `player.hit_stat` / `player.damage_stat` add that stat's modifier to the to-hit roll / damage.
- **Checks**: `stat_modifier` adds the referenced stat's modifier.

**Character choice.** A book may offer pre-made protagonists at the start with a top-level `characters` list. Choosing one seeds its starting stats and gear, then the story speaks to the player as that character.

```json
{
  "characters": [
    {
      "id": "warrior",
      "name": "Dain the Warrior",
      "description": "Strong, but no scholar.",
      "image": "images/dain.jpg",
      "stats": { "hp": 14, "str": 16, "dex": 11, "int": 9 },
      "equipment": ["iron_sword", "plate"],
      "equipped": { "weapon": "iron_sword", "armor": "plate" },
      "start_node": "crossroads"
    }
  ]
}
```

- `stats` override the starting values of the named stats (others keep their defaults).
- `equipment` grants catalog item ids; `equipped` sets active items per slot.
- `start_node` is optional (defaults to `metadata.start_node`).
- When a book defines no `characters`, there is no selection screen — the player simply is the story's character, defined by the stats and prose.

## Usable Items (Consumables)

An inventory item with a `use` block can be spent from the collection panel — healing potions, a charge of something, anything that moves a stat on demand.

```json
{
  "inventory": [
    {
      "id": "brine_tonic",
      "title": "Brine Tonic",
      "description": "A stoppered vial of something green.",
      "use": { "hp": 6 },
      "uses": 1,
      "use_label": "Drink",
      "use_text": "It burns going down, and the ache in your ribs dulls."
    },
    {
      "id": "keepers_draught",
      "title": "Keeper's Draught",
      "use": { "hp": 9 },
      "uses": 2,
      "use_label": "Drink"
    }
  ]
}
```

- The presence of `use` is what makes an item usable; without it an item is just something you carry.
- `use` values are stat deltas, clamped to `[0, max]` like every other stat change — drinking a 6-point tonic when 2 short of full heals 2, and the rest is wasted. Negative values work too (a cursed relic).
- `uses` is the number of charges (default `1`). The button shows the count remaining when there is more than one, and the item leaves the bag once spent. Remaining charges are saved with the playthrough.
- `use_label` renames the verb — `Drink`, `Read`, `Burn`, `Deploy` — following the same opt-in/rename model as every other system.
- Prefer defining usable items in the top-level `inventory[]` catalog. The catalog is authoritative for use data, so an item granted anywhere as a bare `"brine_tonic"` string still knows how to be drunk.

Items can be sold in shops with `{ "inventory": "brine_tonic", "price": 12 }`, which is how a book turns gold into staying power.

## Opt-in and renaming

Every system — collections, equipment, stats, currency, checks, combat, shops — is off until a book uses it, and every label has a default the book can override. A simple story that defines no stats and uses no combat shows none of it. This keeps genres from bleeding UI into each other: a detective book shows "Evidence," a dungeon shows "Health / Armor Class / Gold / Equipment," a travel comedy shows nothing extra. A sci-fi book can rename Armor Class to "Shields", gold to "Credits", and equipment to "Loadout" with pure label overrides.

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

## Battle Nodes

Use battle nodes for dice-based conflict or chance checks.

```json
{
  "id": "warehouse_fight",
  "type": "battle",
  "text": "Hart's guard lunged from behind the stacked crates.",
  "player_dice": "1d6",
  "opponent_dice": "1d6",
  "player_bonus": 0,
  "opponent_bonus": 1,
  "win_target": "guard_defeated",
  "lose_target": "guard_overpowers_you",
  "draw_target": "fight_stalemate",
  "item_modifiers": [
    {
      "item": "brass_knuckles",
      "bonus": 2,
      "description": "Brass Knuckles"
    }
  ]
}
```

Rules:

- dice expressions use `XdY`, such as `1d6`, `2d6`, or `1d8`
- `win_target` and `lose_target` are required
- `draw_target` is optional; draws use `win_target` when omitted
- `item_modifiers` only apply when the player has the matching inventory item
- every battle target must exist

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
