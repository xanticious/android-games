# Memory — Design Document

## Overview
- Memory is a face-down card-matching pairs game played against one local AI opponent.
- All cards begin face-down in a grid, with exactly two cards for each symbol.
- On each turn, the active player flips two cards.
- If the two cards match, that player collects the pair and takes another turn.
- If they do not match, both cards flip face-down again and the turn passes.
- The player who collects the most pairs wins when every pair has been claimed.
- Ties are possible when both players collect the same number of pairs.
- The game is fully offline, single-device, and local-stat only.
- The design focuses on calm readability, quick card recognition, and clear turn ownership without hiding the board.
- AI difficulty controls how well the opponent remembers seen cards and chooses unknown cards.
- Victory, defeat, and tie panels appear below the grid so the final claimed-pair state remains visible.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Table background uses `Dark0` and `Dark1` for a calm deep-water play surface.
- Card backs use `Dark2` with `Aqua3` line art so face-down cards read as interactive but quiet.
- Card faces use bright surfaces derived from `Aqua0` with high-contrast icon glyphs.
- Matched pairs lift into player collection trays with an `Aqua2` glow for the human and an `Aqua4` accent for the AI.
- The two currently flipped cards receive a thin `Aqua3` focus ring until they resolve.
- Mismatch feedback is a short shake or dim pulse that respects reduce-motion.
- AI flips should be readable and slightly slower than instant, but never feel like a blocking animation.
- Symbols must be distinguishable by shape and label, not color alone.
- All implementation colors come from `ui/theme/Color.kt` or Material 3 roles built from those tokens.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Memory        4×4      Medium   ⚙   │  ← Top bar
├─────────────────────────────────────┤
│ You pairs: 3      AI pairs: 2        │  ← Score strip
├─────────────────────────────────────┤
│  [🂠] [🂠] [🐚] [🂠]                 │
│  [🂠] [⚓] [🂠] [🂠]                 │  ← Card grid anchor
│  [🐚] [🂠] [🂠] [🂠]                 │
│  [🂠] [🂠] [⚓] [🂠]                 │
├─────────────────────────────────────┤
│ Your turn. Flip a first card.        │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait centers the grid and chooses card sizes from the selected layout so every card remains tappable.
- Tablets can add side collection trays for won pairs while preserving the same grid order.
- Claimed pairs leave the grid as empty wells or dimmed collected slots, according to the selected accessibility preference.
- Prompt, result, and tie panels appear below the grid and never cover cards.

## Settings
- **Grid size**: 4×4 (default, 8 pairs), 4×5 (10 pairs), or 6×6 (18 pairs).
- **Opponent difficulty**: Easy, Medium, Hard.
- **Player order**: First, Second, or Random.
- **Theme symbols**: Sea Icons (default), Shapes, or Numbers.
- **Keep matched cards visible** (on/off, default off): leaves claimed cards dimmed in place instead of moving them to trays.
- **Fast AI turns** (on/off, default on): keeps AI flips readable while reducing pauses.
- **Show turn reminder** (on/off, default on): emphasizes whose turn it is before the first flip.

## How to Play
- Cards start face-down in a shuffled grid.
- Flip two cards on your turn.
- If the cards match, you collect that pair and immediately take another turn.
- If the cards do not match, remember their positions before they flip face-down again.
- Watch the AI's flips, because all revealed cards become useful information.
- The match ends when all pairs have been collected.
- The player with the most collected pairs wins; equal pair counts produce a tie.

## Controls
- Tap a face-down card to flip it.
- Tap a second face-down card to complete the attempt.
- Tap a collected pair tray to review which symbols each player has claimed.
- During AI turns, cards flip automatically with a visible pause between first and second card.
- After game over, use Rematch or Menu from the below-board result panel.

## AI Opponents
- **Easy**: remembers only the current turn and otherwise chooses mostly random unknown cards.
- **Medium**: remembers recently seen cards, takes known pairs, and explores unknown cards when no pair is known.
- **Hard**: remembers all revealed positions, prioritizes known pairs, and chooses information-gathering flips when no match is known.
- AI difficulty changes memory and choice quality only; shuffle, legal flips, scoring, and extra-turn rules remain identical.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses / ties per difficulty | yes |
| Results by grid size | yes |
| Best pair margin | yes |
| Fewest turns to win | yes |
| Longest matching streak | yes |
| Perfect known-pair captures | yes |

## State Machine
- A dedicated `MemoryStateMachine` in `state/` exposes `StateFlow<MemoryState>`.
```
Idle
 └─ MatchStarted → Dealing
Dealing
 └─ CardsDealt → PlayerTurn / AiThinking
PlayerTurn
 ├─ FirstCardFlipped → AwaitingSecondFlip
 └─ Surrendered → GameOver
AwaitingSecondFlip
 └─ SecondCardFlipped → ResolvingPair
ResolvingPair
 ├─ PairMatched → CollectingPair
 └─ PairMissed → HidingCards
CollectingPair
 ├─ AllPairsCollected → GameOver
 └─ ExtraTurnGranted → PlayerTurn / AiThinking
HidingCards
 └─ CardsHidden → PlayerTurn / AiThinking
AiThinking
 └─ AiCardsChosen → ResolvingPair
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `MemoryRules` controller shuffles pairs, validates face-down flips, resolves matches, assigns collected pairs, grants extra turns, detects ties, and determines final winner; unit tests cover every grid size without Android imports.

## HUD
- Top bar shows grid size, difficulty, active settings profile, and settings access.
- Score strip shows collected pairs for the human and AI, remaining pairs, and active player.
- Grid annotations show flipped cards, resolving cards, matched cards, and disabled interaction during AI turns.
- Prompt/result panel explains first flip, second flip, matches, misses, extra turns, final winner, ties, and rematch actions below the grid.
- Victory/defeat/tie presentation follows `design/common/victory-defeat.md`: results never cover the card grid.
