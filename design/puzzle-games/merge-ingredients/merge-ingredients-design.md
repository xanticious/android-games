# Merge Ingredients — Design Document

## Overview
- Merge Ingredients is a relaxed single-player crafting/discovery game. The player starts with a stocked kitchen of base ingredients and **combines** them to discover new ingredients and recipes.
- Combining valid items yields a new item; the goal is to discover **all** recipes in the cookbook. There is no failure state and no timer.
- Example recipe chains:
  - Yeast + Flour → Dough
  - Dough + Oven → Bread
  - Tomato + Stove → Tomato Sauce
  - Tomato Sauce + Sugar + Salt → Ketchup
  - Bread + Tomato Sauce + Cheese + Salami → Pizza
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Recipe Model
- Recipes are data in `model/`: each recipe is an unordered set of input items → one output item. Some recipes take 2 inputs, some take 3+ (e.g. Ketchup, Pizza).
- "Appliance" items (Oven, Stove) are reusable catalysts: they occupy an input slot in the combine tray during a craft but are **not** used up — they stay in the pantry afterward, unlike consumable ingredients.
- The dependency graph is acyclic; every craftable item has at least one discoverable path from base ingredients.
- Recipe lookup (match a multiset of inputs to an output) is a pure function in `controller/`, unit-tested for the full recipe set including multi-input recipes and order-independence.

## Settings
- **Recipe pack**: Kitchen Basics (default); future packs are data-only.
- **Hints**: on (default) / off — a Hint suggests two items that combine into something undiscovered.
- **Auto-sort pantry**: by category (default) / by discovery order.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Pantry: a scrollable grid of rounded ingredient cards with simple icons and labels on `Dark0`.
- A central **combine tray** (2–4 slots) on `Dark1`; filled slots glow `Aqua2`.
- Successful discovery: the new item card pops in with a brief sparkle and is added to the cookbook; a repeat combo gives a gentle "already known" nudge.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Merge Ingredients  18/42 found ⚙ ? │
├─────────────────────────────────────┤
│   Combine:  [ Dough ][ Oven ][ + ]  │  ← combine tray (drag items in)
│            → Bread!                  │
│   ─── Pantry ───                     │
│   [Flour][Yeast][Tomato][Sugar]      │  ← scrollable pantry grid
│   [Salt][Cheese][Salami][Oven]…      │
├─────────────────────────────────────┤
│  Hint   Cookbook        Combine      │
└─────────────────────────────────────┘
```

## How to Play
- Drag two or more pantry items into the combine tray, then tap Combine.
- A valid set reveals a new ingredient and adds it to your cookbook and pantry.
- Discover every recipe to complete the cookbook.

## Controls
- **Drag** a pantry item into a tray slot (or **tap** to add to the next free slot). See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Tap** a tray slot: remove that item back to the pantry.
- **Combine**: evaluate the tray; on success the output is added.
- **Tap** Cookbook: browse discovered and still-hidden (silhouetted) recipes.

## Gameplay Rules
- A combine succeeds only if the tray's item multiset exactly matches a recipe's inputs (appliances counted as catalysts).
- Discovered items persist in the pantry; base ingredients are unlimited.
- Completion = every recipe discovered.

## State Machine
A dedicated `MergeIngredientsStateMachine` in `state/` exposes `StateFlow<MergeState>`.
```
Idle
 └─ StartGame → Playing
Playing
 ├─ ItemAddedToTray / ItemRemoved → Playing
 ├─ CombineAttempted [match, new] → Playing (item discovered)
 ├─ CombineAttempted [match, known] → Playing (already-known nudge)
 ├─ CombineAttempted [no match] → Playing (no-op feedback)
 └─ AllRecipesFound → Completed
Completed
 └─ (continue free-play / NewPack) → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Recipes discovered / total | yes |
| First-try discoveries (no hint) | yes |
| Hints used | yes |
| Cookbook completion % per pack | yes |

## HUD
- Top bar: title, discovery progress, settings, help.
- Bottom: Hint, Cookbook, Combine.
- Completion message appears below the tray; the pantry stays visible for continued free play.
