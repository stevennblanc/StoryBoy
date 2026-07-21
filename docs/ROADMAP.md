# StoryBoy Roadmap

Planned work, captured so it isn't lost. Nothing here is built yet unless noted.

## Character stats: author-defined ability scores — SHIPPED (0.25.0)

**Goal:** let a gamebook give the player a light layer of tabletop-style control over gear and outcomes, without character creation. The story author defines the character; the engine just has to be flexible enough for many worlds (a warrior, a wizard, a cowboy, a space engineer).

**Built in 0.25.0:** a stat can be flagged `ability: true` (or given a `modifier_table`) to map its value to a tiered modifier; combat `hit_stat`/`damage_stat` and check `stat_modifier` add that modifier. And **character choice** shipped too: a `characters[]` list offers pre-made protagonists that seed stats and gear. See `GAMEBOOK_FORMAT.md` and the demo book *The Ashen Crossroads*. The design that was implemented:

- An optional `modifier` mapping on a stat, so a 1–18-style score yields a small bonus by tier. StoryBoy will ship a **default tier table of its own** (an author can override it); the concept of "ability score → modifier" is a common game mechanic, but our specific numbers are ours, e.g.:

  | Score | Modifier |
  | --- | --- |
  | 1–3 | −3 |
  | 4–5 | −2 |
  | 6–8 | −1 |
  | 9–12 | 0 |
  | 13–15 | +1 |
  | 16–17 | +2 |
  | 18 | +3 |

- Combat and checks gain optional `hit_stat` / `damage_stat` / `check_stat` fields that add the referenced stat's **modifier** (not raw value) to the roll — so a high-Strength warrior hits harder, a high-Dexterity ranger lands more shots, a high-Intelligence wizard's checks succeed more often. The author chooses which stats matter for their world.

- Because the player does not build a character, these scores are **set by the author** to fit the story's protagonist. This only becomes richer if we add a **character-choice** feature (below).

**Character choice (later):** a book could offer a small roster of pre-made characters at the start (e.g. "the Warrior," "the Ranger," "the Hedge-Witch"), each seeding different starting stats, gear, and prose flavor. The engine would pick a starting stat block and equipment set from the chosen character. Still author-authored; no free-form creation.

## Visual gamebook builder (authoring tool)

**Goal:** a tool to author `.gbk` books without hand-writing JSON. To build **only once gameplay feels settled**, so we are not chasing a moving format.

Intended capabilities:

- Pick a node type, enter its text, attach an image.
- Choose which systems/panels the book uses and rename their variables (the opt-in/rename model already in the engine).
- Define stats, equipment, map fragments, and shops through forms.
- A **graph view** to wire nodes together (choice → target, check success/failure, combat win/lose, shop return) with validation that every target exists.
- Export a valid `story.json` and package the `.gbk`.

The engine is already structured for this: the format is declarative, every reference is an id, and the parser validates targets. The builder is a front-end over the same contract documented in `GAMEBOOK_FORMAT.md` and `AUTHORING_REFERENCE.md`.

## Maps

**Decision:** show map **segments as pictures** and let the player draw their own map if they want one — this is more engaging than an auto-assembled map, and simpler to author. The engine already does this (revealed fragments stack in the Map panel). Fragment art should be clean, readable single-room/area sketches, not a pretend-connected whole. A future option, if ever wanted, is a single cohesive revealed map, but it is not planned.

## Before expanding The Sunken Vault to a full-length book

Measured against the 95-node 0.6.0 book. These are cheap now and expensive to retrofit across ~200 nodes.

1. **Consumable items (healing).** No node in the Vault restores Health, and the engine has no way to *use* an item — inventory entries are passive. A long dungeon needs an attrition-and-recovery arc or it is just a pass/fail gauntlet. Needs an engine feature (a `use` action on an inventory item with `stat_changes` and a charge count), then content.
2. **A book validator.** Ad-hoc assertions in throwaway scripts already strain at 95 nodes. Before 200: unreachable nodes, dead ends that are not intended endings, missing targets, `requires` naming unknown items/flags/characters, flags set but never read, and stat ids that do not exist. Cheap to write, pays for itself immediately, and is the same contract the visual builder will need.
3. **Decide the economy before writing it.** Today: 302 gold earnable during play against 92 gold of purchasable goods — a 3.3x oversupply, which is why gold stops being a decision partway through. Set the target ratio and the sinks (selling, consumables, repairs, tolls) *before* authoring rewards, or every reward gets renumbered later.

## Art debt

- **The Sunken Vault cover and banner need regenerating.** The current art shows a lone armored man, but as of 0.6.0 the book lets you play Kell, Sorrel, or Vane — the cover contradicts two of the three. Regenerate with the figure **hooded and seen from behind**, the way The Ashen Crossroads cover was done, so it fits any chosen delver. Same green-and-gold palette and title treatment; only the figure changes.

## Other threads

- Native Google sign-in (needs the provider enabled in the Supabase dashboard).
- Reading-progress sync across devices via Supabase (currently on-device only).
- Selling at shops (only buying exists today).
- Special combat effects (e.g. an enemy that destroys equipment on a hit).
