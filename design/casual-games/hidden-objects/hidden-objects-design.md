# Hidden Objects — Design Document

## Overview
- Hidden Objects is a seek-and-find game where every scene is generated at runtime from a background asset and a pool of object assets rather than hand-painted illustrations.
- Each background defines a set of hotspot zones where objects may be placed; the generator distributes objects across hotspots with randomized position, size (within bounds), and rotation.
- Objects can overlap each other and partially obscure one another, creating occlusion that the player must reason about.
- The generator verifies solvability: every object on the find-list is guaranteed to have enough visible pixels to be distinguishable before the player touches anything.
- When the player taps a listed object correctly, it is removed from the scene, revealing the layer(s) beneath it and potentially exposing objects that were previously obscured.
- The game is fully offline, single-device, and local-stat only.
- No time limit by default; an optional timer mode is available in settings.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Scene area occupies the full screen width; the find-list scrolls horizontally below the scene.
- Background assets are illustrated in a flat, vibrant style consistent with the underwater color palette.
- Object assets are rendered with a thin `Aqua3` highlight ring when the player long-presses to inspect; the ring disappears immediately on release.
- Found objects animate out with a brief expand-and-fade, leaving the revealed layer visible.
- Incorrectly tapped regions produce a short screen-edge pulse in `Dark2` that respects reduce-motion.
- Scene shadows, depth layers, and object drop-shadows are baked into assets; no runtime compositing beyond layer ordering.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Hidden Objects     3/10    ⚙       │  ← Top bar (found / total)
├─────────────────────────────────────┤
│                                     │
│                                     │
│         GENERATED SCENE             │  ← Scrollable/zoomable scene area
│         (background + objects)      │
│                                     │
│                                     │
├─────────────────────────────────────┤
│ [boot] [fish] [anchor] [shell] ...  │  ← Horizontal find-list (thumbnail + name)
└─────────────────────────────────────┘
```
- The scene area supports pinch-to-zoom (0.8×–2.5×) and two-finger pan.
- On small phones, the scene clips to ~65% of screen height; on tablets the scene uses ~80%.
- Find-list items are strikethrough-dimmed when found; remaining items retain full opacity.
- The found/total counter in the top bar updates in real time.

## Settings
- **Difficulty**: Easy (10 objects, generous hotspot sizes, low overlap), Medium (15 objects, default), Hard (20 objects, tight overlap, smaller hotspot margins).
- **Time limit**: Off (default), 3 min, 5 min, 10 min.
- **Hint cooldown**: 30 s (default), 60 s, Off — determines how often the player may request a hint.
- **Object labels**: On (default) / Off — toggles text labels under each find-list thumbnail.
- **Zoom assist** (on/off, default on): auto-pans and zooms to a found object's neighborhood for 1 second after a correct tap.

## How to Play
- Study the find-list along the bottom of the screen — these are the objects you need to locate.
- Tap an object in the scene to identify it. If it matches a listed item, it is removed and the layer beneath is revealed.
- If the tap does not match any listed item, nothing happens (or a brief error pulse appears, configurable in settings).
- Use the hint button (lightbulb icon, top bar) to pan and zoom to the approximate neighborhood of an unlocated object; the button goes on cooldown after use.
- Find all listed objects to complete the scene.
- In timer mode, the game ends if time expires with objects remaining.

## Controls
- **Tap** anywhere on the scene: attempts to identify an object at that point.
- **Pinch / spread**: zoom the scene in or out.
- **Two-finger drag**: pan the scene.
- **Long-press** on any scene area: shows a magnifying circle at tap point (no object detection; purely visual assist).
- **Tap** hint button: zooms and pans to hint location; enters cooldown.
- After all objects found, tap the result panel to proceed (new scene, or menu).

## Scene Generation

### Assets
- A scene is defined by a `SceneDefinition` data class:
  - `background`: reference to a background drawable asset.
  - `hotspots`: list of `Hotspot(rect, maxObjects)` regions where objects may be placed.
  - `objectPool`: list of available `ObjectAsset` references for this scene.
- An `ObjectAsset` data class contains:
  - `id`, `name`, `drawable`, `baseWidthDp` (natural display size).
  - `sizeRange`: float range (e.g. 0.7f–1.3f) applied to base size at placement.
  - `rotationRange`: degrees range (e.g. -30f–30f) at placement.
  - `minimumVisibleFraction`: fraction of object pixels that must remain unobscured to count as solvable (default 0.25f).

### Placement Algorithm
1. Shuffle the object pool and select the required count for the current difficulty.
2. For each selected object, pick a hotspot weighted by remaining capacity.
3. Sample a random position, size, and rotation within the hotspot and object bounds.
4. Compute occlusion against already-placed objects using bounding-box approximation.
5. If `visibleFraction >= minimumVisibleFraction`, accept the placement; otherwise retry up to 10 times before skipping to the next hotspot.
6. After all objects are placed, run a final solvability pass: any object whose visible fraction falls below threshold (due to later placements) is repositioned or its z-order is raised.
7. The generator records the z-ordered list of placed objects for hit-testing at runtime.

### Hit Testing
- On a tap at `(x, y)`, the hit-test walks the placed objects from top z-order to bottom.
- The first object whose bounds contain the tap and whose pixel at `(x, y)` is non-transparent is the candidate.
- If the candidate matches a listed unlocated object, the find is registered; otherwise no match is reported.
- After a successful find, that object is removed from the z-ordered list, and the scene is redrawn.

## Gameplay Loop
1. Settings are confirmed → scene is generated (generation happens on a background coroutine before the scene is shown).
2. Scene is displayed; the find-list populates.
3. Player taps objects. Correct taps remove objects and update the find-list counter.
4. When the last object is found → `AllObjectsFound` event → scene clears with a brief celebration flash.
5. The result panel appears below the scene (never over it) showing time taken and objects found.
6. Player can tap **Next Scene** (new scene same difficulty) or **Menu**.

### Timer Mode
- A countdown bar appears at the top of the scene area.
- When the timer reaches zero → `TimerExpired` event → game over panel appears below scene showing how many objects were found.
- Found objects are retained visually; remaining objects are revealed with a dim overlay.

## State Machine
- A dedicated `HiddenObjectsStateMachine` in `state/` exposes `StateFlow<HiddenObjectsState>`.
```
Idle
 └─ StartGame → Generating
Generating
 └─ SceneReady → Playing
Playing
 ├─ ObjectFound → Playing (found count incremented, object removed)
 ├─ HintRequested → HintCooldown
 ├─ AllObjectsFound → SceneComplete
 └─ TimerExpired [timer mode] → GameOver
HintCooldown
 └─ CooldownExpired → Playing
SceneComplete
 ├─ NextScene → Generating
 └─ BackToMenu → Idle
GameOver
 ├─ Retry → Generating
 └─ BackToMenu → Idle
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Scenes completed per difficulty | yes |
| Best time per difficulty | yes |
| Total objects found (lifetime) | yes |
| Hints used per scene (fewest in a win) | yes |
| Perfect scenes (no wrong taps) | yes |

## HUD
- Top bar: game title, found/total counter, hint button with cooldown indicator, settings.
- Scene area: no persistent overlay; zoom/pan controls are gesture-only.
- Find-list: horizontally scrollable row of thumbnail + label chips; found items are dimmed and struck through.
- Timer bar (timer mode only): thin progress bar below the top bar, shrinks left-to-right.
- Result panel appears below the scene on completion or game over; it never covers the scene.
