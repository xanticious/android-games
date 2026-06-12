# Chess Puzzles — Design Document

## Overview
- Chess Puzzles is a 10-round single-player score-attack mode built from curated or generated one-move positions.
- There is no opponent, no AI turn, and no match result beyond the player's run score.
- Each round presents a single chess position and asks the player to find the best move for the side to play.
- The player gets exactly one move per puzzle; after submission, the app compares the choice against engine analysis.
- Base scoring uses evaluation distance from the engine's top line: within 0.1 earns 100, within 0.5 earns 75, within 1.5 earns 50, within 3.0 earns 25, and anything worse earns 0.
- Speed adds a bonus: +20 within 10 seconds, +10 within 30 seconds, and +5 within 60 seconds.
- A perfect round can therefore score 120 points, and a perfect 10-round run can score 1,200 points.
- The running total builds across all 10 puzzles, with best run score and scoring breakdowns stored locally on device.
- Difficulty and variety come from Settings: puzzle source, theme mix, rating band, side to move, hints, and whether review details are shown.
- The board remains fully visible during solving, scoring, review, and run-complete states.
- Victory and defeat wording should be replaced by score-attack language such as Run Complete, New Best, and Try Another Run.

## Visual Style
- Material 3 presentation using the underwater palette from `ui/theme/Color.kt`.
- Background layers use `Dark0` and `Dark1`; the board frame and key dividers use `Aqua4`.
- Light board squares use a tinted `Aqua0` treatment; dark squares use `Dark2`.
- Selected pieces, candidate targets, and legal move dots use `Aqua2`.
- Engine feedback arrows, best-move highlights, and scoring accents use `Aqua3`.
- Text uses `Aqua0` on dark surfaces, with `Aqua1` for secondary labels and timing details.
- Round-complete animations are restrained: score chips count up below the board while the final position remains visible.
- The board must remain fully visible during scoring, review, and session-complete states.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Chess Puzzles   Round 4/10    [⚙]   │  ← Top bar
├─────────────────────────────────────┤
│ Score 315     Time 00:18     Theme   │  ← Run HUD
├─────────────────────────────────────┤
│                                     │
│           8×8 chess board            │  ← Puzzle anchor
│                                     │
├─────────────────────────────────────┤
│ Find the best move for White.        │  ← Prompt/review panel
│ [Hint]                 [Submit move] │
└─────────────────────────────────────┘
```
- Portrait keeps the board centered and as large as possible; tablets may place move notation and score breakdown in a side column.
- Review and run-complete panels appear below the board, never over it.

## Settings
- **Puzzle difficulty**: Beginner, Intermediate, Advanced, Expert, or Mixed.
- **Puzzle themes**: Tactics, endgames, defense, mate threats, positional moves, or Mixed (default Mixed).
- **Side to move**: White, Black, or Mixed (default Mixed).
- **Puzzle source**: Curated pack, generated positions, or Mixed.
- **Hints** (on/off, default on): offer a side-to-move reminder and optional candidate-piece highlight; using a hint records the run as assisted.
- **Show engine review** (on/off, default on): after each move, show the best move, chosen move, evaluation distance bucket, and speed bonus.
- **Timer display** (on/off, default on): hides the visible clock only; speed bonus still uses elapsed time.

## How to Play
- Start a run to receive 10 chess positions in sequence.
- For each puzzle, inspect the board and choose exactly one legal move for the side to play.
- Tap Submit to lock the move; there is no second attempt for that puzzle.
- The app compares the chosen move to the engine's top line and awards base points from the evaluation-distance bucket.
- A speed bonus is added based on how quickly the move was submitted.
- After the score breakdown, continue to the next puzzle until all 10 rounds are complete.
- The final run total is compared against the locally stored best score.

## Controls
- Tap a piece to select it; legal destinations highlight.
- Tap a destination to stage the move.
- Tap Submit to score the staged move.
- Tap Hint, when enabled, for limited assistance before submitting.
- During review, tap Best Move to replay the engine line and tap Next Puzzle to continue.

## Scoring & Stats (local)
| Scoring item | Points |
|--------------|--------|
| Chosen move within 0.1 evaluation of top line | 100 |
| Chosen move within 0.5 evaluation of top line | 75 |
| Chosen move within 1.5 evaluation of top line | 50 |
| Chosen move within 3.0 evaluation of top line | 25 |
| Chosen move worse than 3.0 evaluation from top line | 0 |
| Submitted within 10 seconds | +20 |
| Submitted within 30 seconds | +10 |
| Submitted within 60 seconds | +5 |
| Submitted after 60 seconds | +0 |

| Stat | Stored |
|------|--------|
| Best 10-round score | yes |
| Best unassisted 10-round score | yes |
| Average score per difficulty | yes |
| Perfect-move count by theme | yes |
| Average solve time | yes |
| Hint-assisted runs completed | yes |

## State Machine
- A dedicated `ChessPuzzlesStateMachine` in `state/` exposes `StateFlow<ChessPuzzlesState>`.
```
Idle
 └─ RunStarted → LoadingPuzzle
LoadingPuzzle
 └─ PuzzleLoaded → Solving
Solving
 ├─ PieceSelected → MoveStaged
 ├─ HintRequested → Solving
 └─ RunCancelled → RunComplete
MoveStaged
 ├─ MoveChanged → MoveStaged
 ├─ MoveSubmitted → ScoringPuzzle
 └─ SelectionCleared → Solving
ScoringPuzzle
 └─ ScoreAccepted → LoadingPuzzle / RunComplete
RunComplete
 └─ NewRun / Menu → Idle
```
- A pure `ChessPuzzleScorer` controller computes evaluation buckets, speed bonuses, round totals, and run totals without Android imports; unit tests cover every bucket boundary and timer bonus boundary.

## HUD
- Top bar shows game name, current round, and settings access.
- Run HUD shows total score, elapsed time for the current puzzle, difficulty/theme label, and assisted-run status.
- Prompt/review panel shows side to move, staged move, hint result, scoring breakdown, and run-complete actions below the board.
- Run-complete presentation follows `design/common/victory-defeat.md` placement rules: result panels never cover the board.
