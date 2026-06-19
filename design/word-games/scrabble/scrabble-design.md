# Scrabble — Design Document

## Overview
- Scrabble is the classic tile-placement word game played by a single human against a local
  **AI opponent** on a standard 15×15 board.
- Players draw from a shared bag to keep a rack of up to seven letter tiles and take turns
  placing words that interlock with tiles already on the board, scoring from tile values
  and premium squares.
- The AI opponent has three difficulties: **Easy**, **Medium**, and **Difficult**.
- Plays are validated against the **Scrabble players dictionary**; **illegal words are
  prevented** (a rejected play is never committed). See
  `design/common/word-data-sources.md`.
- There is **no timer** — turns are untimed and relaxed.
- The board is always the visual anchor; rack, bag, scores, and result messaging stay
  outside the board. Victory/defeat presentation follows
  `design/common/victory-defeat.md`.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- The board well is a rounded `Dark1` card; premium squares use distinct `Aqua` tints
  (double/triple letter and word) labeled for clarity.
- Tiles are light surfaces with `Dark1` glyphs and small value subscripts; the tile placed
  most recently and the active turn's tentative tiles get an `Aqua3` outline.
- No hex values outside `ui/theme/Color.kt`; tile placement uses a short slide/snap.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Scrabble        You 132  AI 118  [⚙ ?]│  ← Top bar / score strip
├─────────────────────────────────────┤
│   ┌───────────── 15×15 ───────────┐  │
│   │  board with premium squares    │  │  ← Board anchor
│   └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│   Rack:  [A][E][R][T][S][N][I]        │
│   Bag: 41   Play  Recall  Shuffle     │
│   Pass   Swap                          │  ← Action row
└─────────────────────────────────────┘
```
- Portrait stacks board, rack, and actions; the board scales to fit without clipping or
  illegible tiles.
- Tablet layouts widen the board and may show the move/score history in a side column.

## Settings
- **Opponent difficulty**: Easy, Medium, Difficult (default Medium).
- **Dictionary variant** (if more than one players dictionary is bundled; default the
  primary one).
- **Show premium-square legend** (on/off, default on).
- **Highlight legal anchor squares** (on/off, default off): assist for new players.
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- Each player keeps a rack of up to seven tiles drawn from the bag.
- On your turn, drag rack tiles onto empty board squares to form a single line of letters
  that connects to existing tiles (the first move crosses the center square).
- All words formed (the main word and any cross-words) must be valid; **Play** validates
  before committing — illegal plays are rejected with an inline reason and tiles return to
  the rack.
- A valid play scores tile values with letter/word premiums; using all seven tiles earns
  the bingo bonus.
- You may **Pass** or **Swap** tiles instead of playing. The game ends when the bag is
  empty and a player empties their rack (or after consecutive passes); final racks adjust
  scores per standard rules.

## Controls
- Drag a rack tile to a board square to place it; tap a tentative tile to pick it back up.
- **Recall** returns all tentative tiles to the rack; **Shuffle** reorders the rack.
- **Play** commits a validated move; **Pass** / **Swap** take the alternative turn actions.
- Settings (⚙) and How to Play (?) from the top bar.

## Legal-Word Enforcement
- A pure controller computes the candidate words from the tentative placement and checks
  each against the Scrabble players dictionary.
- If any word is invalid, the play is rejected and **not** committed; the player sees which
  word failed. This guarantees no illegal word is ever scored.

## AI Opponents
- **Easy**: plays a valid but short/low-scoring word, often the first legal move it finds;
  rarely uses premium squares deliberately.
- **Medium**: searches legal moves and picks a good-scoring play, balancing points with
  keeping a usable rack; uses premiums when convenient.
- **Difficult**: generates the full set of legal moves, evaluates score plus rack leave and
  board safety (denying premiums/openings), and plays near-optimally.
- Difficulty changes only move quality; the dictionary, scoring, and rules are identical.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty | yes |
| Highest game score | yes |
| Highest single-move score | yes |
| Bingos (seven-tile plays) | yes |
| Best win margin | yes |
- Standard Scrabble tile values, premium squares, and the bingo bonus apply.

## State Machine
- A dedicated `ScrabbleStateMachine` in `state/` exposes `StateFlow<ScrabbleState>`.
```
Idle
 └─ GameStarted → Dealing
Dealing
 └─ RacksFilled → PlayerTurn
PlayerTurn
 ├─ TilePlaced / TileRecalled / RackShuffled → PlayerTurn
 ├─ PlayValidated → Scoring        (illegal → rejected, stays PlayerTurn)
 ├─ Passed / Swapped → TurnAdvanced
 └─ Resigned → GameOver
Scoring
 └─ MoveScored → TurnAdvanced
TurnAdvanced
 ├─ GameEndReached → GameOver
 └─ NextPlayer → AiTurn / PlayerTurn
AiTurn
 └─ AiMoveResolved → Scoring / TurnAdvanced
GameOver
 └─ Rematch / Menu → Idle
```
- The pure `ScrabbleRules` controller handles placement legality, cross-word extraction,
  dictionary validation, scoring with premiums/bonus, bag/rack management, legal-move
  generation for the AI, and end-game scoring with no Android imports; unit tests cover
  validation, illegal-play rejection, premium scoring, and the bingo bonus.

## HUD
- Top bar / score strip: game name, both scores, current turn, bag count, settings.
- Rack row shows the player's tiles; action row shows Play/Recall/Shuffle/Pass/Swap.
- Per `hud-elements.md` for shared styling.
