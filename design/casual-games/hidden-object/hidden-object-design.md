# Hidden Object — Design Document

## Overview
- Hidden Object is a seek-and-find game where each round presents a single illustrated scene and exactly one hidden object to locate.
- The scene is generated at runtime from a background asset and an object pool (using the same generation system as Hidden Objects), but only one item is selected and placed each round.
- The object shifts position and orientation each round — even the same scene offers a new search every time.
- There is no find-list, no multiple targets. The player must find the single named object.
- The generator guarantees solvability: the object always has enough visible pixels to be distinguishable.
- No time limit by default; an optional timer mode is available in settings.
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Scene area occupies the full screen width. The object name is shown in a single compact banner at the bottom.
- Background assets are illustrated in a flat, vibrant style consistent with the underwater palette.
- Object assets render with a thin `Aqua3` highlight ring on long-press for visual inspection (disappears on release).
- Correct find: the object briefly expands and fades with a golden sparkle, leaving the scene visible beneath.
- Wrong tap: a brief `Coral` screen-edge pulse. No counter, no penalty — just feedback.
- Result panel appears below the scene on completion; it never overlays the scene.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Hidden Object          ⏱ 0:24  ⚙  │  ← Top bar (optional timer, settings)
├─────────────────────────────────────┤
│                                     │
│                                     │
│         GENERATED SCENE             │  ← Scrollable/zoomable scene area
│                                     │
│                                     │
├─────────────────────────────────────┤
│  Find:  🔑  Brass Key               │  ← Object banner (icon + name)
└─────────────────────────────────────┘
```
- The scene supports pinch-to-zoom (0.8×–2.5×) and two-finger pan.
- On small phones the scene clips to ~70% of screen height; on tablets the scene uses ~85%.
- The hint button is in the top bar (lightbulb icon); it pans and zooms to the object's neighborhood and enters cooldown.

## Settings
- **Difficulty**: Easy (large hotspot, generous visible fraction, minimal overlap), Medium (default), Hard (small hotspot, tight overlap, more clutter in scene).
- **Time limit**: Off (default), 30 s, 1 min, 2 min.
- **Hint cooldown**: 30 s (default), 60 s, Off.
- **Object label**: On (default) / Off — toggles the text label in the object banner.
- **Wrong tap feedback**: Pulse (default), Silent.

## How to Play
- Read the object banner at the bottom to know what you are looking for.
- Explore the scene by pinching to zoom and dragging to pan.
- Tap the object in the scene. If your tap lands on the object, it is found and the round ends.
- A wrong tap produces a brief pulse but no penalty — keep searching.
- Use the hint button if you are stuck; it pans to the object's approximate location and enters cooldown.
- Find the object to complete the round. Tap **Next** for a fresh scene, or **Menu** to exit.

## Controls
- **Tap** anywhere on the scene: attempts to find the object at that point.
- **Pinch / spread**: zoom the scene in or out.
- **Two-finger drag**: pan the scene.
- **Long-press** on any scene area: shows a magnifying circle for visual inspection (no object detection; purely visual assist).
- **Tap** hint button (top bar): zooms and pans to hint location; enters cooldown.

## Scene Generation

### Assets
Scene and object assets reuse the same `SceneDefinition` and `ObjectAsset` data classes defined for Hidden Objects. The difference is that only one object is selected per round.

### Placement
1. One object is chosen at random from the scene's object pool.
2. A hotspot, position, size (within object's `sizeRange`), and rotation (within `rotationRange`) are sampled.
3. Occlusion from background details is computed using the bounding-box approximation.
4. If `visibleFraction >= minimumVisibleFraction`, the placement is accepted; otherwise, up to 10 retries are attempted before relaxing the visible fraction threshold.
5. The accepted placement is recorded for hit-testing.

### Hit Testing
- On a tap at `(x, y)`, the hit-test checks whether the tap lands within the placed object's bounds and on a non-transparent pixel.
- If yes, the object is found; otherwise no match is reported.

## Gameplay Loop
1. Settings are confirmed → scene is generated (on a background coroutine before the scene is shown).
2. Scene and object banner are displayed.
3. Player taps the scene. Correct tap → object found → brief celebration animation.
4. Result panel appears below the scene with time taken, hint used, and personal bests.
5. Player taps **Next** (new object, same or different scene) or **Menu**.

### Timer Mode
- A countdown bar appears at the top of the scene area.
- When the timer reaches zero → `TimerExpired` event → game over panel below scene with object revealed and dimmed.

## State Machine
- A dedicated `HiddenObjectStateMachine` in `state/` exposes `StateFlow<HiddenObjectState>`.
```
Idle
 └─ StartGame → Generating
Generating
 └─ SceneReady → Playing
Playing
 ├─ ObjectFound → RoundComplete
 ├─ HintRequested → HintCooldown
 ├─ WrongTap → Playing (error pulse)
 └─ TimerExpired [timer mode] → GameOver
HintCooldown
 └─ CooldownExpired → Playing
RoundComplete
 ├─ NextRound → Generating
 └─ BackToMenu → Idle
GameOver
 ├─ Retry → Generating
 └─ BackToMenu → Idle
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Rounds completed | yes |
| Best time per difficulty | yes |
| Hints used per round (fewest in a win) | yes |
| Perfect rounds (found on first tap, no hint) | yes |
| Total objects found (lifetime) | yes |

## HUD
- Top bar: title, optional timer, hint button with cooldown indicator, settings.
- Scene area: full-bleed illustration with no persistent overlays.
- Object banner: single-row strip at the bottom showing the object icon and name.
- Timer bar (timer mode only): thin countdown bar below the top bar.
- Result panel below the scene on completion or game over; never covers the scene.
