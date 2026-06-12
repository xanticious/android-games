# Battleships — Design Document

## Overview
Battleships is a turn-based hidden-information duel focused on deduction, pressure, and clean feedback.
This version is designed for one player versus AI with a strong split-board presentation.
The player's own fleet is always visible on a compact board at the left.
The primary attack grid occupies the center-right and is the main interaction focus.
The tone is crisp naval strategy with a modern Material 3 presentation rather than military realism.
Every shot should feel decisive through readable markers, ship status updates, and turn pacing.
A special Keep Going mode extends finished games into a post-result analysis challenge.
That mode lets the player and AI continue firing to measure how many extra shots were needed after the official win or loss.
Official result stats and Keep Going stats remain separate.

## Visual Style
Overall surfaces use Dark0 and Dark1 for deep-ocean contrast.
Grid lines, panel dividers, and ship silhouettes use Dark2.
Selection, active targeting, and hover-like emphasis use Aqua3 and Aqua4.
Soft panel fills, counters, and helper labels use Aqua0 and Aqua1.
Hits use a hot red-accent Material 3 error color for clarity, ringed by Aqua0 so they remain visible in both themes.
Misses use a pale white-grey splash marker with subtle Aqua1 ripples.
Sunk ships gain a distinct crushed marker treatment: full ship path tinted dark with repeated hit pips and a small "SUNK" tag chip.
The player's fleet mini-board uses simplified ship blocks rather than decorative art.
The enemy board hides ships completely until sunk if reveal rules are enabled for end-state review only.
Buttons are chunky Material 3 pills with strong contrast and large touch targets.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Battleships     Turn 12      [⚙]    │  ← Top bar
├─────────────────────────────────────┤
│ Your fleet        Enemy waters       │
│ A B C D E         A B C D E F G H I J│
│ 1 ■ ■ · · ·       1 · · ◌ · · · · · │
│ 2 · · · · ·       2 · ✕ · · ◌ · · · │  ← Attack grid anchor
│ 3 · · ■ ■ ■       3 · · · ✕ ✕ ✕SUNK │
│ 4 · · · · ·       4 · · · · · · · · │
├─────────────────────────────────────┤
│ Ships: You 4/5   AI 3/5   Mode: Official │
│ [Fire] [Rotate setup] [Keep Going]   │
└─────────────────────────────────────┘
```
Portrait-first: the enemy attack grid is the main center panel, with the player's fleet compressed beside or above it depending on width. Tablet layouts show both full-size grids side by side with the HUD and controls spanning underneath.

## Settings
- **Opponent difficulty**: Easy, Medium, Hard (default Medium).
- **Grid size**: 10x10 standard (default) or 8x8 quick game with a shortened fleet.
- **Ship placement**: Manual (default) or automatic random placement.
- **Keep Going mode** (on/off, default on): after an official win or loss, offer a non-modal **Keep Going** button that continues firing to measure extra shots needed after the result.
- **End-state reveal** (on/off, default on): reveals remaining hidden ships only after the official result or when leaving Keep Going.
- Settings are presented before setup begins.

## How to Play
Place your fleet secretly, then take turns firing at coordinates on the enemy grid. A shot is a miss, hit, or sunk result. The first side to sink the entire enemy fleet earns the official result.

Keep Going mode begins only after the official result is recorded. In that mode, both sides can continue taking shots until the remaining fleet is fully discovered, producing separate analysis stats such as extra shots needed and post-result accuracy. Keep Going never changes the official win or loss.

## Controls
- During setup, drag ships onto the fleet grid or tap **Rotate setup** before placing the selected ship.
- Tap an enemy coordinate to target it; the cell previews with an Aqua3/Aqua4 focus ring.
- Tap **Fire** to confirm the selected coordinate. Previously fired cells are disabled.
- Tap ship status chips to briefly pulse that ship's known cells on your own grid.
- After the official result, tap **Keep Going** to continue or **Menu** to end; neither appears as a modal over the grids.

## AI Opponents
- **Easy**: fires randomly, then takes simple adjacent follow-up shots after a hit.
- **Medium**: uses parity search, completes likely ship lines, and avoids impossible placements.
- **Hard**: maintains probability heat maps from remaining ship lengths, clusters hits efficiently, and adapts to the player's placement patterns across the current match only.
- AI difficulty changes decision quality only; fleet rules, turn order, official results, and Keep Going separation never change.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Official wins / losses per difficulty | yes |
| Official shots fired and accuracy | yes |
| Official fewest shots to win | yes |
| Ships sunk by type | yes |
| Keep Going sessions started / completed | yes |
| Keep Going extra shots after official result | yes |
| Keep Going post-result accuracy | yes |

Official stats and Keep Going stats are stored separately so extended analysis never inflates win rate, accuracy records, or fastest official clears.

## State Machine
A dedicated `BattleshipsStateMachine` in `state/` exposes `StateFlow<BattleshipsState>` and owns setup, turn order, firing, official result, and Keep Going flow.
```
Idle
 └─ MatchStarted → PlacingFleet
PlacingFleet
 ├─ ShipPlaced → PlacingFleet
 └─ FleetReady → PlayerTargeting
PlayerTargeting
 └─ ShotConfirmed → ResolvingShot
ResolvingShot
 ├─ OfficialResultReached → OfficialGameOver
 ├─ TurnPassed → AiTargeting
 └─ ExtraShotRecorded → KeepGoing
AiTargeting
 └─ AiShotChosen → ResolvingShot
OfficialGameOver
 ├─ KeepGoingChosen → KeepGoing
 └─ Menu / Rematch → Idle
KeepGoing
 ├─ ShotConfirmed → ResolvingShot
 ├─ AiShotChosen → ResolvingShot
 └─ KeepGoingFinished → GameOver
GameOver
 └─ Rematch / Menu → Idle
```
A pure `BattleshipsRules` controller in `controller/` has no Android imports and is unit-tested for placement legality, shot results, sinking, win detection, AI-visible information, and official-versus-Keep-Going stat separation.

## HUD
- Turn number, whose turn, selected coordinate, remaining ships, and hit/miss/sunk counts.
- Mode chip showing **Official** or **Keep Going** so post-result shots are unmistakable.
- Compact fleet health for both sides; enemy ship identities reveal only when sunk or at end-state review.
- Victory/defeat messaging follows `design/common/victory-defeat.md` and lives below the grids, never over either board.
