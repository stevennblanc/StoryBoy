# The Sunken Vault — story design

The plan for taking the Vault from a 97-node systems demo to a full-length book. Decisions here are settled; this is the spec the prose gets written against. Economy rules live in `ROADMAP.md` and are not repeated.

## The four decisions

| | |
| --- | --- |
| **Premise** | A crew went into the vault a month ago and did not come out. You are who got sent after them. |
| **Structure** | Three descending tiers, mostly one-way. Deeper means worse, and turning back costs something. |
| **Cast** | One companion you can carry out — unreliable, dishonest, and the trader's brother. The rest of the crew are found in pieces on the way down. |
| **Reveal** | The vault-keepers drowned their own vault, deliberately. The Tide-Priest is the last of them, still keeping the post. |

## The lost crew

Five people. You meet them in order of descent, and each one is evidence of the same story. Only one comes out.

1. **Odo Finch** — dead in the upper galleries, early. Face down and tangled, a month in the water. His ledger is the first thing that says these were *people*, and it names the other four. Sets the register: this is a recovery, not a treasure hunt.
2. **Sethe Marlow** — pinned under a fallen lintel in rising water. She is lucid, she is calm, and **she cannot be saved** — no combination of choices frees her. The player can spend resources trying, can stay with her, or can leave. The only real choice is how she dies and whether anyone was there. Nothing rewards this. That is the point.
3. **Cass Harrow** — the companion (below).
4. **Ruen Ash** — turned. He is one of the drowned now, and he is a fight. The fight can be avoided entirely by recognising him first — from Odo's ledger, or because Cass says his name. A player who has been reading gets to not kill him.
5. **Ismene Vale** — the crew's scholar and the reason they came. She is at the bottom, she got further than anyone, and she understood what the keepers did. She is the climax.

## Cass Harrow, the companion

Walking wounded. Moves on his own, flinches from every fight, hides when it starts, and steals from you — small things, denied to your face, funny about it afterwards. He must be **entertaining enough to be worth saving despite himself**, because the whole moral weight rests on the player being tempted to leave him.

**He is an asset and a risk, not a fighter.**

- **Opens routes**: holds the far end of a rope, takes one side of the sluice-wheel, fits through gaps you cannot. Several branches are only reachable with him.
- **Knows the vault**: reveals map fragments, names which of the crew's caches are still stocked, reads glyphs you cannot.
- **Carries the reveal**: he read Ismene's notes. The keepers' story reaches the player as a man talking, not as a lore node.
- **Costs**: he takes things. He is why a tonic is missing later. He will not be talked into a fight and will not be left anywhere he considers worse than where he is.

**The sister.** The trader at the vault mouth — the one who sells the starting kit and says *"buy something, or buy a coffin later"* — is Cass's sister. She has been camped there a month because she cannot go down and cannot leave. **This is not revealed at the start.** Cass mentions her in Tier 2, offhand, in the middle of lying about something else. It reframes the opening scene retroactively, and costs nothing to author because that scene already exists.

**The payoff, both ways:** bring him out and she is at the mouth. Lose him and she is *still* at the mouth. Same scene, played the other way. No stat penalty, no failure screen — just her, waiting, and you with your pockets full. The consequence is the scene, not a number.

## The three tiers

| Tier | Place | What it is for | Crew | Rough nodes |
| --- | --- | --- | --- | --- |
| I | The Flooded Galleries | Arrival, outfitting, the first evidence that people died here. Scavenging tone, cold and procedural. | Odo dead, Sethe pinned | 60–70 — **written, 0.9.0** |
| II | The Drowned Crypt | Cass found and the book gains a voice. The records begin explaining the keepers. Bleaker, stranger. | Cass, Ruen turned | 70–80 — **core written, 0.10.0** |
| III | The Sanctum | The keepers' story completes and Ismene forces the choice. Quiet, not loud. | Ismene | 50–60 |

Mostly one-way: descending is easy, climbing back is a deliberate cost. The way *out* at the end is its own sequence, and is where carrying Cass is felt hardest.

## Endings

Driven by what the player understood and who they carried, not by a boss's hit points. Sketch, to be firmed up while writing Tier III:

- **The keeping** — someone has to hold the post. Ismene will, if you let her. You leave with your life and the coast stays dry.
- **The breaking** — kill the Priest, take everything, and the seal fails behind you. Richest ending, and the coast floods a fourth time. The book does not tell you off for it.
- **The relief** — the Priest has held the post for centuries and wants to be let go. Understanding this requires having read the records.
- **The bad delve** — you die in the dark, like the crew did.
- Each crosses with **Cass out / Cass lost**, which changes the final scene rather than the ending's category.

## The scene at the mouth

The last scene of the book is his sister, and it is the payoff the whole companion arc exists for. It varies on **his fate crossed with what you knew** — six versions, not one with a variable line.

| | Knew she was his sister | Never found out |
| --- | --- | --- |
| **Carried out** | She sees him over your shoulder. The reunion, and she knows you knew — you chose it. | She runs past you to him. You learn what you did only now, from her face. |
| **Left behind** | She asks, and you already know the answer she wants. You say it anyway. | She asks whether you saw anyone. You describe him — the limp, the lying. She stops packing up her stall. |
| **Killed** | You knew, you did it, and she is still standing there. | You find out what you took from her while she is asking after him. |

The bottom-right is the cruellest thing in the book and no player chooses it — they simply were not listening when Cass talked. That case is the reason the reveal is missable.

**Killing him must be genuinely available**, not an accident and not a trap that punishes the player for taking it. He steals and lies; there is a moment where turning on him is a real option, and the book does not editorialise.

**Flags:** `cass_out`, `cass_killed`, `cass_sister_known`. The exit node carries six choices with mutually exclusive and exhaustive conditions, so exactly one is visible and the player only ever sees "Climb out into the light."

> **Guard this.** Both readers evaluate `flag` and `not_flag` in the same `requires` block, so the pattern works today with no engine change. The hazard is not the logic, it is exhaustiveness: a gap in the predicates means **zero visible choices at the final node of the book**. Author the six, then verify all six reachable states resolve to exactly one exit before shipping.

Optional refinement if it earns its place: split *left behind* into abandoned versus died-on-the-way. Same scene, different weight of guilt.

## Authoring rules

- Combat carries less of the pacing than it does now. Fights should be avoidable, optional, or *about* something — Ruen is the model.
- Gold obeys the economy in `ROADMAP.md`: one shop at the mouth, gear discoverable, tolls and bribes inside, paying costs loot.
- Every non-`text` node gets a way out. Run `scripts/validate_gamebook.py` before packaging, every time.
- Existing 0.8.0 content is raw material, not scripture. Most of the upper vault survives as Tier I; the crypt is rebuilt around Cass.
- The three delvers stay. Kell, Sorrel, and Vane each need their own line into the crew — a debt, a rumour, or a name they recognise in Odo's ledger.
