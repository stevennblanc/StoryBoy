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

1. **Consumable items (healing).** — SHIPPED (0.29.0). An inventory item with a `use` block is spendable from the bag, with its own verb and charge count, clamped and persisted. The Vault gained a Brine Tonic and a Keeper's Draught, sold at both traders and hidden in the ruin.
2. **A book validator.** — SHIPPED. `scripts/validate_gamebook.py`, documented in `AUTHORING_REFERENCE.md`. It found a live bug on its first run: the Vault's merchant sold a `torch_bundle` that the book never defined.
3. **Decide the economy before writing it.** — SETTLED (Vault 0.8.0). The shape, to author every future reward against:

   - **One shop, at the mouth, before you descend.** A trader camped at a ruin's entrance is believable; a shop three rooms into a drowned crypt is not. The mid-dungeon merchant is gone.
   - **All equipment is discoverable.** The trader sells a starting kit and supplies; every other weapon, shield, and suit of armour is found in the world.
   - **Gold rewards exploration, not killing.** Caches, hoards, and hidden niches pay. Corpses give gear and survival, not coin.
   - **Inside, gold buys passage — tolls and bribes.** Paying is never strictly better than fighting: it buys safety and costs you the loot the fight would have dropped.
   - **Never gate progress on gold alone.** Every toll has a free alternative — a fight, or a skill check — so a broke player is never stuck. Unaffordable tolls show locked with an in-fiction line, so the player sees what their purse cost them.
   - Numbers as of 0.9.0: 90 gold findable across the whole vault (was 302), 43 gold of goods at the mouth, 60 gold of tolls inside.

## The Sunken Vault expansion

Story shape is settled — see `SUNKEN_VAULT_DESIGN.md` for the spec. Premise: a crew went in a month ago and you were sent after them. Three descending tiers. An unreliable companion who is the trader's brother. The vault-keepers drowned their own vault. Target ~180–210 nodes, written against the settled economy above.

## Art debt

- **The Sunken Vault cover and banner need regenerating.** The current art shows a lone armored man, but as of 0.6.0 the book lets you play Kell, Sorrel, or Vane — the cover contradicts two of the three. Regenerate with the figure **hooded and seen from behind**, the way The Ashen Crossroads cover was done, so it fits any chosen delver. Same green-and-gold palette and title treatment; only the figure changes.

## Other threads

- Native Google sign-in (needs the provider enabled in the Supabase dashboard).
- Reading-progress sync across devices via Supabase (currently on-device only).
- Selling at shops (only buying exists today).
- Special combat effects (e.g. an enemy that destroys equipment on a hit).
