# Splendor Puzzles — Design Document

## Overview
Splendor Puzzles is a 10-round score mode made from complete Splendor board states.
Each puzzle shows the gem bank, three tiers of market cards, noble tiles, four opponent summaries, and the player's own engine, gems, reserved cards, and points.
The player is asked one question: "What is your best move?"
A single legal action is selected using full-game controls and then compared against engine-ranked alternatives.
Scoring bands are fixed: optimal move 100 points, near-optimal within the top 10% of value 75, good within the top 25% 50, below average 25, clearly suboptimal 0.
Speed adds +20 under 15 seconds and +10 under 30 seconds.
Best run score is stored locally on device.

## Visual Style
Use Material 3 containers with a polished jewel-table presentation and the underwater palette.
Overall background uses `Dark0`; market shelves, player trays, and summary panels use `Dark1` and `Dark2`.
Selection rings, action highlights, and confirm states use `Aqua2` and `Aqua3`; progress and best-run callouts use `Aqua4`.
Text uses `Aqua0` for contrast.
Actual gem identities remain true to Splendor, but all surrounding chrome, borders, and emphasis cues come from the shared token palette.
Result animations appear only in the lower tray or side summary panel.
No result element may cover cards, nobles, gems, or player summaries.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Splendor Puzzles   Round 4/10   ⚙   │  ← Top bar
├─────────────────────────────────────┤
│ Bank: Wh4 Bl3 Gr5 Rd2 Bk4 Gold1     │  ← Gem bank
├─────────────────────────────────────┤
│ Nobles: [3Wh3Bl3Gr] [4Rd4Bk] [mix]  │
│ Tier III: [card] [card] [card] [card]│
│ Tier II : [card] [card] [card] [card]│  ← Market anchor
│ Tier I  : [card] [card] [card] [card]│
├─────────────────────────────────────┤
│ Opponents: A 8pt  B 6pt  C 5pt  D 3pt│ ← Board-state summaries
├─────────────────────────────────────┤
│ You: 7pt  Gems 2/10  Reserve 2/3     │  ← Player engine tray
│ Action: choose the best legal move.  │
│ [Take gems] [Reserve] [Buy] [Confirm]│
├─────────────────────────────────────┤
│ Result appears here after confirming.│  ← Score/result panel
└─────────────────────────────────────┘
```
- Portrait keeps the gem bank, nobles, market, opponent summaries, and player tray visible in a vertical scroll with the market as the main anchor.
- The player tray remains sticky near the bottom when possible so the selected action can be confirmed without losing context.
- Tablet layouts widen the market and place opponent summaries in a side column while preserving the same information order.
- Result scoring, alternative-move explanations, speed bonus, and round navigation appear in the lower result panel and never cover cards, nobles, gems, or summaries.

## Settings
- **Puzzle set**: Daily-style local set, Random 10-round run, or Curated tactics.
- **Difficulty band**: Intro, Standard, Expert, or Mixed; controls board-state complexity and engine ranking depth.
- **Timer display**: On or Off; speed bonuses still follow the selected scoring profile when enabled for the run.
- **Show legal action guide** (on/off, default on): keeps full-game controls readable without revealing the best move.
- **Post-move explanation depth**: Brief, Standard, or Detailed.
- **Card text density**: Compact or Expanded.
- **Reset best run**: clears only the locally stored best score for this puzzle mode after confirmation.

## How to Play
- Each run contains 10 independent Splendor board-state puzzles.
- Study the gem bank, market cards, nobles, opponent summaries, and your own engine.
- Choose exactly one legal Splendor action: take gems, reserve a card, buy a visible card, or buy a reserved card when legal.
- Confirm the move once you believe it is the best option.
- The app compares your move against engine-ranked alternatives for that exact board state.
- Optimal moves score 100 points, near-optimal moves score 75, good moves score 50, below-average moves score 25, and clearly suboptimal moves score 0.
- Speed adds +20 under 15 seconds and +10 under 30 seconds when the scoring profile uses timing.
- Best run score is stored locally on device.

## Controls
- Tap gem tokens to build a take-gems action; illegal token combinations are explained in the action guide.
- Tap a visible card to inspect buy and reserve options.
- Tap a reserved card in the player tray to inspect or buy it when affordable.
- Tap a noble to inspect visit requirements; nobles are informational in puzzle selection and do not require a direct action.
- Tap **Confirm** to submit the selected legal action for scoring.
- After scoring, tap alternative moves in the result panel to compare why they ranked higher or lower.
- Tap **Next puzzle** from the result panel to continue the run.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Best 10-round run score | yes |
| Average puzzle score | yes |
| Optimal moves found | yes |
| Speed bonuses earned | yes |
| Results by difficulty band | yes |
| Most common action type chosen | yes |

## State Machine
- A dedicated `SplendorPuzzlesStateMachine` in `state/` exposes `StateFlow<SplendorPuzzlesState>`.
```
Idle
 └─ RunStarted → LoadingPuzzle
LoadingPuzzle
 └─ PuzzleLoaded → ChoosingAction
ChoosingAction
 ├─ GemsSelected → ChoosingAction
 ├─ CardSelected → ChoosingAction
 ├─ ReserveSelected → ChoosingAction
 ├─ ActionCleared → ChoosingAction
 └─ ActionConfirmed → ValidatingAction
ValidatingAction
 ├─ ActionAccepted → ScoringMove
 └─ ActionRejected → ChoosingAction
ScoringMove
 └─ MoveScored → ShowingResult
ShowingResult
 ├─ AlternativeInspected → ShowingResult
 ├─ NextPuzzle → LoadingPuzzle
 └─ RunFinished → RunComplete
RunComplete
 └─ NewRun / Menu → Idle
```
- A pure `SplendorPuzzleRules` controller validates legal Splendor actions for a static board state, computes submitted action identity, applies ranking bands, applies speed bonuses, advances run score, and records best local run data; unit tests cover scoring thresholds and legal action validation without Android imports.

## HUD
- Top bar shows mode name, round number, scoring profile, timer state, and settings access.
- Bank and market areas show available gems, nobles, visible cards, and affordability indicators.
- Opponent summaries show enough local board state to judge urgency without becoming live opponents.
- Player tray shows engine discounts, current gems, reserved cards, prestige, selected action, and confirm availability.
- Result panel shows score band, speed bonus, best alternative, explanation, run total, and next-puzzle action below the play area.
- Victory/defeat language is not used for individual puzzles; run completion and best-score messaging still follow the non-overlay guidance from `design/common/victory-defeat.md`.
