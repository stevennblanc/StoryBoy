# StoryBoy Presentation Mode Standard

StoryBoy treats gameplay as state first and presentation second.

The story engine should decide what happened. The platform renderer should decide how that moment is shown.

This lets the Android app feel polished while keeping the same story playable on e-ink and future ESP32-style devices.

## Core Rule

Every animated presentation must have a static equivalent.

An animation can make Android feel better, but it must never be the only way the player receives important information.

## Motion Modes

| Mode | Purpose | Expected Behavior |
| --- | --- | --- |
| `Full` | Default Android experience | Short transitions, highlights, and reveal effects are allowed when they improve clarity |
| `Reduced` | Accessibility, low-power, or preference-based calmer UI | Minimal motion; prefer static state changes and simple emphasis |
| `None` | E-ink, ESP32, and motion-free Android mode | Direct redraws only; all feedback remains visible until the player advances |

Android may default to `Full`.

E-ink and ESP32 builds must default to `None`.

## Engine Events

The engine should expose semantic presentation events. These describe meaningful gameplay moments without prescribing UI behavior.

Current event vocabulary:

| Event | Meaning |
| --- | --- |
| `NodeEntered` | The reader moved to a new story node |
| `InventoryGained` | One or more inventory items were added |
| `EvidenceGained` | One or more evidence entries were added |
| `BattleResolved` | A battle or risk roll produced a result |
| `PuzzleAnswered` | A puzzle answer was submitted and routed to a target |

These events can be rendered differently per platform.

Example:

- Android `Full`: show a brief highlight when evidence is added.
- Android `Reduced`: show the evidence row immediately with no flourish.
- E-ink `None`: redraw the page with a persistent "Evidence added" line and a Continue choice.

## Static Equivalents

| Moment | Android Full | E-ink / None |
| --- | --- | --- |
| Battle roll | Optional short dice reveal before result | Immediate result panel with rolls, modifiers, outcome, and Continue |
| Evidence gained | Optional highlight on evidence board | Persistent "Evidence added" section |
| Inventory gained | Optional item highlight | Persistent "Item added" section |
| Page transition | Optional short fade or slide | Direct redraw to the next page |
| Button press | Ripple or press feedback | Focus rectangle or selected state |
| Puzzle result | Optional answer feedback transition | Static success/failure message or routed result page |

## Authoring Rule

Gamebook JSON should request story meaning, not animations.

Good:

```json
{
  "id": "find_ledger",
  "type": "text",
  "text": "The ledger was hidden under a loose board.",
  "evidence": ["roy_ledger"],
  "choices": [
    {
      "text": "Return to the office",
      "target": "office_hub"
    }
  ]
}
```

Avoid:

```json
{
  "animation": "flash_evidence_board"
}
```

The renderer can decide whether evidence gain is highlighted, redrawn, or simply listed.

## E-ink Rules

E-ink-compatible presentation must follow these rules:

- No transient toasts or snackbars for important information
- No fade-only or motion-only feedback
- No timed interaction required to understand the result
- Prefer high contrast and stable layouts
- Use focus rectangles, selected states, and persistent labels instead of animation
- Keep feedback visible until the player chooses to continue
- Avoid UI changes that rely on color alone

## Android Rules

Android may add motion, but should stay reader-like:

- Keep motion short and purposeful
- Do not animate long reading text
- Do not animate every page turn by default
- Avoid blocking the player with decorative effects
- Respect `MotionMode.Reduced` and `MotionMode.None`

## Implementation Boundary

The story engine may emit `StoryPresentationEvent` values.

The renderer may convert those events into:

- Compose animations for Android `Full`
- Static emphasis for Android `Reduced`
- direct redraws for e-ink `None`

The engine must not wait for animation timing before changing story state.

The gamebook format must remain platform-neutral.
