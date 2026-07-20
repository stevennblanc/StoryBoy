# StoryBoy Roadmap

Planned work, captured so it isn't lost. Nothing here is built yet unless noted.

## Character stats: author-defined ability scores

**Goal:** let a gamebook give the player a light layer of tabletop-style control over gear and outcomes, without character creation. The story author defines the character; the engine just has to be flexible enough for many worlds (a warrior, a wizard, a cowboy, a space engineer).

**Where we are:** the stat system is already fully author-defined — any named numeric stats with overridable labels, a `health` and `armor` role, and `stat_modifier` on checks. A book can already define STR/DEX/CON/INT/WIS/CHA, or Shields/Plating/Focus, etc. What is *not* built is an ability-score→modifier convention and combat reading those modifiers.

**Planned addition (design, not built):**

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

## Other threads

- Native Google sign-in (needs the provider enabled in the Supabase dashboard).
- Reading-progress sync across devices via Supabase (currently on-device only).
- Selling at shops (only buying exists today).
- Special combat effects (e.g. an enemy that destroys equipment on a hit).
