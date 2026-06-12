# Overcooked Lite — Design Document

## Overview
A high-definition kitchen time-management game where the player controls two cooks. Selection and movement use a simple tap system: tap a cook to select, then tap a destination or object to send them there. The goal is to complete and plate dishes before orders expire, managing both cooks simultaneously under time pressure.

## Visual Style
- Top-down 2D kitchen view with bright, clean cartoon-style art.
- Kitchens are compact and visually readable — every station is immediately identifiable by shape and icon.
- Cooks are distinct in appearance (different colors, hats, or accessories) so the player can instantly tell them apart.
- Selected cook has a glowing ring below them.
- Interactive objects highlight with a faint glow when a cook is selected and nearby.
- Completed dishes animate with a brief sparkle.
- Burnt food turns black and emits cartoon smoke puffs.
- Order tickets appear at the top as visual recipe cards with a countdown bar.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Order 1 ████░] [Order 2 ███░] │  ← Active orders with timers (top)
├─────────────────────────────────┤
│                                 │
│       KITCHEN MAP               │
│  [stations, counters, appliances]│
│         [COOK A] [COOK B]       │
│                                 │
├─────────────────────────────────┤
│  [Score]  [Time Left]  [Stars]  │  ← HUD (bottom)
└─────────────────────────────────┘
```

## Controls
- **Select a cook**: tap directly on Cook A or Cook B.
- **Move cook**: with a cook selected, tap any floor tile to move them to that position.
- **Interact**: with a cook selected, tap a station, ingredient, or counter to have the cook walk to it and interact automatically.
- **Deselect**: tapping the currently selected cook deselects them (or selecting the other cook auto-switches).
- No joystick — all input is tap-based.
- Both cooks can be active simultaneously; tapping Cook B while Cook A is mid-task switches the active selection without interrupting Cook A.

## Gameplay Loop

### Level Structure
- Each level is timed (e.g., 3 minutes).
- A queue of orders appears at the top, each with an expiry timer.
- New orders arrive periodically throughout the level.
- Completing an order scores points and earns stars based on delivery time.
- Letting an order expire loses points and counts against the star rating.
- At the end of the timer, the level ends and a star rating (0–3) is awarded based on final score.

### Kitchen Stations
Each station type serves a specific role:

| Station | Function |
|---------|----------|
| Ingredient shelf | Tap to pick up an ingredient |
| Chopping board | Chop ingredient (cook interacts for 1–2 seconds) |
| Stove | Cook ingredient (takes 3–5 seconds; must be removed before burning) |
| Oven | Bake items (takes 5–8 seconds) |
| Plating counter | Combine prepared ingredients onto a plate |
| Serving hatch | Deliver completed plate to score |
| Bin | Discard burnt or wrong items |
| Sink | Wash dirty plates (required when plates run low) |

### Cook Behavior
- When directed to a station, the cook walks there and performs the interaction automatically (no further input needed).
- If the station is occupied by the other cook, the directed cook waits.
- A cook can carry one item at a time.
- Visual status bubble above each cook shows: current item held (or empty), current action (chopping, waiting, etc.).

### Orders
- Each order card shows required dish ingredients as icons.
- Orders have an expiry timer (countdown bar on the card).
- Completed dishes must be plated (all ingredients combined on the plating counter) and delivered via the serving hatch.
- Serving the wrong dish scores 0 and wastes the plate.

### Burning
- Food left on the stove or in the oven too long burns.
- Burnt food must be thrown in the bin.
- Burning creates cartoon smoke; a distinct audio-visual cue warns the player before it burns.
- A visual timer on the stove/oven shows the cooking progress and the danger zone.

## Difficulty Scaling
- More order types and more complex recipes per level.
- More orders arrive simultaneously at higher levels.
- Kitchen layouts become more complex (obstacles, longer paths, split layouts).
- Order expiry timers decrease at higher levels.

## Scoring
| Event | Points |
|-------|--------|
| Deliver order (before 75% timer) | 200 |
| Deliver order (75–100% timer) | 100 |
| Let order expire | −50 |
| Deliver wrong dish | 0 |
| Level complete star bonus | 100 × stars_earned |

## State Machine
```
Idle
 └─ LevelStart → Playing
Playing
 ├─ OrderDelivered → Playing (score update)
 ├─ OrderExpired → Playing (penalty)
 ├─ FoodBurnt → Playing (discard required)
 └─ TimerExpired → LevelComplete
LevelComplete
 └─ (show results, next level or menu) → Idle
```

## HUD
- Order cards: top of screen, up to 4 simultaneous orders, each with a timer bar.
- Score: bottom-left.
- Level timer: bottom-center (countdown).
- Stars earned so far: bottom-right (fills as score thresholds are met).
- Status bubbles above each cook (always visible): held item icon + action icon.
