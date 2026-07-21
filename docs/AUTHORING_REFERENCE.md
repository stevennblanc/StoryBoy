# StoryBoy Authoring Reference

A single-page map of everything a gamebook author can use and rename. For full examples see `GAMEBOOK_FORMAT.md`; this file is the quick index of node types, systems, and nameable variables, kept current as the engine grows.

## Design principle: opt-in and renamable

Every system is **off until a book uses it**, and every player-facing label has a **default the book can override**. A book that defines no stats and no combat shows none of that UI. Labels are how a book fits a genre: a detective book shows "Evidence," a dungeon shows "Health / Armor Class / Gold / Equipment / Map," a sci-fi book can rename those to "Shields / Plating / Credits / Loadout / Star Chart," and a quiet story shows nothing extra.

The author defines the character and world through the stats, labels, and prose. StoryBoy does not let the player build a character; the story speaks to the player *as* the character (a warrior, a wizard, a cowboy, a space engineer), and the stats/combat/equipment should reflect that world.

## Node types

| Type | Purpose | Key fields |
| --- | --- | --- |
| `text` | Narrative, branching, endings. No `choices` = an ending. | `text`, `choices[]` (`text`, `target`) |
| `lore` | Journals, records, optional reading. | `entries[]` (`title`, `text`), `return_to` |
| `puzzle` | Typed-answer challenge. | `question`, `answers[]`, `correct_target`, `incorrect_target` |
| `inventory` | Scene whose purpose is gaining items. | `items[]`, `return_to` |
| `evidence` | Scene whose purpose is gaining evidence. | `evidence[]`, `return_to` |
| `equipment` | Scene whose purpose is gaining gear. | `equipment[]`, `return_to` |
| `map` | Location/travel hub (branching choice screen). | `locations[]` (`title`, `description`, `target`) |
| `shop` | Buy catalog items with a currency stat. | `currency_stat`, `items[]` (`equipment`/`inventory`, `price`), `return_target` |
| `check` | One-roll saving throw / luck test. | `dice`, `modifier`, `stat_modifier`, `target`, `success_target`, `failure_target` |
| `combat` | Round-based fight. | `enemy{}`, `player{}`, `armor_stat`, `health_stat`, `win_target`, `lose_target`, `flee_target`, `talk_target` |
| `battle` | Lightweight opposed one-roll (older, simpler than `combat`). | `player_dice`, `opponent_dice`, `win_target`, `lose_target`, `item_modifiers[]` |

Any node may also carry: `image`/`images[]`, `stat_changes{}` / `set_stats{}`, `set_flags[]` / `clear_flags[]`, `reveal_map`, and grant `items`/`evidence`/`equipment`.

### Conditional choices

A choice or map location may carry `requires` (hidden until met) plus optional `locked_text` (shown disabled instead). Predicates, all of which must pass: `item` / `not_item`, `equipment`, `equipped`, `evidence`, `character` (chosen class), `flag` / `not_flag`, and `stat` numeric comparisons (`min`/`at_least`, `max`/`at_most`, `gt`, `lt`, `equals`, against the effective value). Story flags are free-form — set them with `set_flags` on any node, no declaration required; they're the standard way to stop repeatable loops.

## Systems and their nameable variables

| System | Default label | Where renamed | Notes |
| --- | --- | --- | --- |
| Inventory | `Items` | `collections.inventory.label` | Also `collections.items`. `show_count`, `enabled`. |
| Evidence | `Evidence` | `collections.evidence.label` | Detective "Evidence," or "Memories," "Clues," etc. |
| Equipment | `Equipment` | `collections.equipment.label` | Also `collections.gear`. Items can be equipped. |
| Map | `Map` | `collections.map.label` | Fragments reveal as you explore. |
| Stats | per-stat | each stat's `label` | Fully author-defined (see below). |
| Currency | (a stat) | that stat's `label` | Any stat a shop's `currency_stat` points at. |

### Stats

Stats are entirely author-defined. Each entry in the top-level `stats[]`:

| Field | Meaning | Default |
| --- | --- | --- |
| `id` | Stable key used by nodes/combat/shops. | — (required) |
| `label` | Display name. | title-cased `id` |
| `start` | Starting value. | `0` |
| `max` | Optional cap; shows as `value/max`. | none |
| `role` | `health` (zero ends the run), `armor` (combat defense), or `normal`. | `normal` |
| `hidden` | Track without showing in the bar. | `false` |
| `ability` | Ability score: value maps to a modifier via the tier table. | `false` |
| `modifier_table` | Custom `{ min, max, mod }` bands (implies `ability`). | default tiers |

Nodes change stats with `stat_changes{ id: delta }` (add/subtract) and `set_stats{ id: value }` (absolute), clamped to `[0, max]`. Checks add a stat's **modifier** to the roll via `stat_modifier` (for a plain stat that's its value; for an ability score, its tiered modifier). Default ability tiers: ≤3 −3, 4–5 −2, 6–8 −1, 9–12 0, 13–15 +1, 16–17 +2, ≥18 +3.

### Characters (optional)

A top-level `characters[]` list offers pre-made protagonists at the start. Each: `id`, `name`, `description`, optional `image`, `stats{}` (starting overrides), `equipment[]` (granted ids), `equipped{}` (slot→id), optional `start_node`. No list = no selection screen.

### Equipment items

Equipment entries (in the `equipment[]` catalog or granted inline) add:

| Field | Meaning |
| --- | --- |
| `slot` | Makes the item equippable; one active per slot (e.g. `weapon`, `armor`, `shield`). |
| `equip_effects{}` | Stat deltas applied while equipped (e.g. `{ "ac": 2 }`). |
| `damage`, `damage_bonus`, `hit_bonus` | Weapon stats used in combat while equipped. |

Displayed and combat-used stat values are the base value **plus** everything equipped.

### Map fragments

Top-level `map[]` entries are `{ id, title, image }`. Nodes reveal them with `reveal_map` (id or list). The reader assembles revealed fragments, in catalog order, in the Map panel. (Distinct from the `map` node *type*, which is a location hub.)

## Combat quick fields

- `enemy`: `label`, `hp`, `hit_target` (player must roll ≥ this to hit), `damage`, `attack_bonus`.
- `player`: `damage`, `damage_bonus`, `hit_bonus` (overridden by an equipped weapon), and optional `hit_stat` / `damage_stat` (add that stat's modifier).
- `armor_stat`: the stat the enemy must beat to hit you (so armor matters); falls back to `monster_hits_on`.
- `health_stat`: the stat damage reduces (defaults to the `health`-role stat).
- Targets: `win_target`, `lose_target` (death), optional `flee_target`, `talk_target`.

## Reserved / known ids

- Stat role `health` is required for any book that uses combat death; `armor` is required for armor to matter in combat.
- Only one stat should carry each role.

_When a node type, field, or system is added or changed, update this file and `GAMEBOOK_FORMAT.md` together._
