# Puzzle Flow & Completion — Common Component

Shared structure for how a puzzle game is entered, played, completed, and recorded. Individual game docs reference this file and describe only their game-specific rules.

## Screen Order
Every puzzle game follows the same modal-free flow, driven by `AppStateMachine` (KStateMachine):
1. **Settings** screen — choose puzzle parameters (size, difficulty, variant). Pre-filled with the last-used values.
2. **How to Play** screen — concise rules and the control legend for this game.
3. **Gameplay** screen — the board plus a top bar and a bottom action/status row.

Settings and How to Play are reachable again from the gameplay top bar (⚙ and ?). Navigation is instantaneous; there are no modal dialogs over the board.

## Gameplay Screen Anatomy
```
┌─────────────────────────────────────┐
│  Title                  timer/⚙ ?    │  ← Top bar
├─────────────────────────────────────┤
│                                     │
│            GAME BOARD               │  ← Board (see puzzle-grid-board.md)
│                                     │
├─────────────────────────────────────┤
│  Undo  Hint  …      status text     │  ← Bottom action + status row
└─────────────────────────────────────┘
```

## Victory / Defeat Presentation
- Per the project non-negotiable, victory and defeat status **never overlays the board**. It appears in the status area *below* the board.
- On solve: the board stays fully visible; a celebratory full-screen animation (the only place full-screen animation is allowed) may play *behind/around* the board area without covering cells, and a "Solved!" summary with stats appears below.
- On failure (only for games that can be lost, e.g. Minesweeper): a concise "You hit a mine" style message appears below the board; the board remains visible so the player can see what happened.
- The summary offers **New**, **Reset/Retry**, and **Back to Lobby**.

## Local Stats (per game)
- All stats are stored locally on the device only — no network, no accounts.
- Common stats tracked where meaningful: puzzles solved (lifetime), best time, best/fewest moves, current and best streak, no-hint solves.
- Per-game additional stats are listed in each game's design doc.

## State Machine Pattern
- Each game owns a dedicated state machine in `state/` exposing `StateFlow<…State>`.
- Events are a `sealed interface`; states are `sealed class` / `DefaultState` nodes.
- A typical shape: `Idle → Playing → (Paused) → Solved`, with `Failed` only for losable games. Settings/How-to-Play live in `AppStateMachine`, not the per-game machine.
