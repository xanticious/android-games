# Where's Waldo — Design Document

## Overview
- Where's Waldo is a seek-and-find casual game inspired by seek-and-find puzzle books.
- Each level presents a large, densely illustrated scene packed with characters, objects, and visual distractions. The player must locate a short list of specific characters (Waldo and companions) hidden somewhere in the crowd.
- Scenes are hand-authored with fixed target locations (not procedurally generated), so targets are carefully placed to be challenging but always fair.
- There are no time limits by default. An optional timer challenge mode is available in settings.
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Scene images: large, high-resolution flat-illustration assets with a colorful, busy crowd aesthetic. Waldo and companions use their iconic visual designs (striped shirts, hats, glasses) as original art inspired by the seek-and-find genre.
- Target characters are not copyrighted assets; they are original characters designed specifically for this game with distinctive but genre-appropriate appearances.
- Zoom circle: a magnifying-glass-style circular view that renders a 2× enlarged portion of the scene at the tap point.
- Correct find: a green confirmation ring appears around the found character for 2 seconds.
- Wrong tap: a brief red X pulse at the tap point; no penalty other than feedback.
- Scene is never obscured by the result panel — the panel appears in a sheet that slides up from the bottom only on level completion.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Where's Waldo   Level 3    ⚙       │  ← Top bar
├─────────────────────────────────────┤
│                                     │
│                                     │
│         LARGE ILLUSTRATED           │  ← Scene (pinch-to-zoom, pan with drag)
│         CROWD SCENE                 │
│                                     │
│                                     │
├─────────────────────────────────────┤
│ Find: [Waldo✓] [Wenda] [Wizard]     │  ← Horizontal find-list (thumbnail + name)
└─────────────────────────────────────┘
```
- Supported zoom range: 1×–4×. Below 2× the scene fits the full screen width.
- The find-list is always visible at the bottom; found characters are dimmed with a checkmark.
- The zoom circle appears when the player long-presses; releasing confirms the tap at the center.

## Settings
- **Timer mode**: Off (default), 3 min, 5 min, 10 min.
- **Zoom circle** (on/off, default on): enables the magnifying zoom circle on long-press.
- **Hint cooldown**: 60 s (default), 120 s, Off — how often the hint button can be used.
- **Wrong tap feedback**: Pulse (default), Silent — whether a wrong tap triggers a visual cue.

## How to Play
- Study the find-list at the bottom of the screen — these are the characters you need to locate.
- Explore the scene by pinching to zoom in and dragging to pan.
- To make a precise selection, long-press to bring up the zoom circle, then lift your finger to tap at that location.
- Tap directly on a listed character to mark them as found. The character's thumbnail in the find-list gains a checkmark.
- A wrong tap produces a brief pulse but no penalty.
- Use the hint button (top bar) to briefly zoom to the general area of an unlocated character; the hint button enters cooldown after use.
- Find all listed characters to complete the level.

## Controls
- **Pinch / spread**: zoom the scene in or out (1×–4×).
- **Drag** (one finger): pan when zoomed in.
- **Tap**: attempt to find a character at the tapped position.
- **Long-press**: opens the zoom circle centered at the tap point; releasing sends the tap.
- **Tap** hint button (top bar): zooms to the approximate neighborhood of the next unfound character; enters cooldown.
- **Tap** find-list thumbnail: zooms to the approximate location of that character if already found (for review); has no effect on unfound characters.

## Scene & Target Design
- Each scene is a static authored asset: a single large image with known bounding boxes for each target character.
- Target characters are placed with their full bodies visible (never cropped by the scene edge) and never fully occluded.
- A minimum visual separation between the target's bounding box and any near-lookalike is enforced during scene authoring (lookalikes must differ on at least two visual attributes: color, pattern, accessory).
- Scenes are authored at 2048×3072 px (portrait) or 3072×2048 px (landscape) for sharpness at max zoom.
- Find targets per level: 3 (easy scenes), 5 (default), 7 (challenge scenes).

## Level Progression
- Levels are unlocked in order; completing a level unlocks the next.
- Each completed level is stored with: found/total, time taken, hints used.
- Replay any completed level at any time.
- Timer mode is a separate challenge track that does not affect the normal completion record.

## State Machine
- A dedicated `WheresWaldoStateMachine` in `state/` exposes `StateFlow<WheresWaldoState>`.
```
Idle
 └─ StartLevel → Playing
Playing
 ├─ TapAtPoint [correct] → Playing (target found, find-list updated)
 ├─ TapAtPoint [wrong] → Playing (error pulse)
 ├─ HintRequested → HintCooldown
 ├─ AllTargetsFound → LevelComplete
 └─ TimerExpired [timer mode] → GameOver
HintCooldown
 └─ CooldownExpired → Playing
LevelComplete
 ├─ NextLevel → Playing
 └─ BackToMenu → Idle
GameOver
 ├─ Retry → Playing (same level, timer reset)
 └─ BackToMenu → Idle
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Levels completed | yes |
| Best time per level | yes |
| Hints used per completion | yes |
| Perfect completions (zero wrong taps) | yes |
| Total characters found (lifetime) | yes |

## HUD
- Top bar: title, current level, hint button with cooldown indicator, settings.
- Scene area: full-bleed illustration; no persistent HUD elements overlay the scene.
- Find-list: horizontal scrollable row at the bottom; found characters are dimmed with a checkmark; remaining characters have full opacity.
- Timer bar (timer mode only): thin countdown bar below the top bar.
- Level complete: bottom sheet slides up over the find-list (scene remains fully visible) with time, hints used, perfect badge if applicable, and next-level / menu buttons.
