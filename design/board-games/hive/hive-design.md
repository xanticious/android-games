# Hive — Design Document

## Overview
- Hive is a two-player abstract strategy game played without a fixed board.
- The human player faces one local AI opponent.
- Players place insect tiles next to one another to build a single connected structure called the hive.
- The goal is to surround the opposing Queen Bee on all six sides before the AI surrounds yours.
- Each player has 11 pieces: Queen Bee ×1, Beetles ×2, Grasshoppers ×3, Spiders ×2, and Ants ×3.
- A player's Queen Bee must be placed by that player's fourth turn.
- After a player's Queen Bee is placed, that player's pieces may move according to their insect rules.
- Every move must preserve the One Hive rule: the hive can never be split into separate groups.
- Freedom-to-move and sliding restrictions prevent most pieces from squeezing through closed gaps.
- There is no fixed board, so the play area grows from the placed tiles and supports pan and zoom.
- The entire match is offline, with local-only stats and a state-machine driven flow.
- Victory and defeat messages stay below the play area, never over the hive.

## Visual Style
- Use Material 3 surfaces with a tactile tabletop feel and the underwater palette from `ui/theme/Color.kt`.
- Background surfaces use `Dark0` and `Dark1`, field hints and hex outlines use `Aqua3`, and selection plus legal destinations use `Aqua2`.
- Text and counters use `Aqua0`, while stronger callouts such as turn emphasis or zoom controls use `Aqua4`.
- Player pieces remain black and white for rule clarity, but highlights, badges, and motion cues come from the shared token palette.
- Insect icons are bold, flat glyphs centered on hex tiles; each insect type must remain identifiable without relying on color alone.
- Stacked beetles show clear elevation, vertical offset, and count badges without clutter.
- Legal placement ghosts use translucent hexes with `Aqua1` outlines; legal movement destinations use stronger `Aqua2` rings.
- Invalid moves show an inline explanation below the hive rather than a modal.
- All implementation colors come from `ui/theme/Color.kt` or Material 3 roles built from those tokens.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Hive          Turn 6   Medium   ⚙   │  ← Top bar
├─────────────────────────────────────┤
│ You: Queen placed   AI: Queen due    │  ← Rule/status strip
├─────────────────────────────────────┤
│                                     │
│        ⬡Q  ⬡A  ⬡B                  │
│      ⬡S  ⬡G  ⬡Q  ⬡A                │  ← Pan/zoom hive canvas
│        ⬡B  ⬡S  ◎                   │
│                                     │
├─────────────────────────────────────┤
│ Hand: Q×1 B×1 G×2 S×1 A×2           │  ← Piece tray
│ Place your Queen by turn 4.          │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait gives the hive canvas most of the screen, with the remaining hand tray and prompt panel below it.
- The canvas automatically centers the hive after AI moves but preserves the player's pan/zoom while selecting.
- Tablets may show each player's remaining pieces in side rails while the hive stays centered.
- Victory/defeat panels slide up below the canvas and never cover the final Queen surround.

## Settings
- **Opponent difficulty**: Easy, Medium, Hard.
- **Player order**: First, Second, or Random.
- **Show legal moves** (on/off, default on): highlights placements and movement destinations after selecting a tile.
- **Show rule explanations** (on/off, default on): explains Queen deadline, One Hive, sliding, and insect movement rejections.
- **Auto-center after AI turn** (on/off, default on).
- **Fast AI turns** (on/off, default on): keeps AI movement readable but brief.

## How to Play
- On your turn, either place a new tile from your hand or move one of your placed tiles if your Queen Bee has already been placed.
- New tiles must touch the hive and, after the opening placement, may not touch an opponent tile unless the game state allows only the first contact.
- Your Queen Bee must be placed by the end of your fourth turn.
- The Queen moves one space by sliding.
- Beetles move one space and may climb onto other pieces; the top beetle controls the stack's color for placement adjacency.
- Grasshoppers jump in a straight line over one or more adjacent pieces to the first empty space.
- Spiders move exactly three sliding spaces and cannot backtrack during that move.
- Ants slide any number of spaces around the outside of the hive.
- Every placement and movement must leave all pieces connected as one hive.
- Win by surrounding the opposing Queen Bee on all six sides, regardless of which player owns the surrounding pieces.

## Controls
- Tap a piece in your tray to show legal placement cells, then tap a ghost cell to place it.
- Tap one of your placed pieces to show legal movement destinations, then tap a destination to move.
- Tap a stacked beetle badge to inspect stack order.
- Pan and pinch the hive canvas at any time outside an active drag.
- Tap an invalid destination to show the rule preventing it when rule explanations are enabled.
- After game over, use Rematch or Menu from the below-board result panel.

## AI Opponents
- **Easy**: places Queen on time and seeks simple surrounds, but misses pins, mobility traps, and long ant routes.
- **Medium**: respects tempo, blocks surrounds, uses beetle climbs, and avoids breaking One Hive connectivity.
- **Hard**: evaluates piece mobility, pins, Queen pressure, stack control, forced placements, and multi-turn surround threats.
- AI difficulty changes decision quality only; insect movement, Queen deadline, and One Hive legality remain identical.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty | yes |
| Wins as first / second player | yes |
| Average turns to surround | yes |
| Queen deadline warnings shown | yes |
| Wins by surrounding with beetle on top | yes |
| Invalid One Hive moves prevented | yes |

## State Machine
- A dedicated `HiveStateMachine` in `state/` exposes `StateFlow<HiveState>`.
```
Idle
 └─ MatchStarted → AssigningFirstPlayer
AssigningFirstPlayer
 └─ FirstPlayerChosen → PlayerTurn / AiThinking
PlayerTurn
 ├─ TrayPieceSelected → ChoosingPlacement
 ├─ BoardPieceSelected → ChoosingMove
 └─ Surrendered → GameOver
ChoosingPlacement
 ├─ PlacementChosen → ValidatingAction
 └─ SelectionCleared → PlayerTurn
ChoosingMove
 ├─ DestinationChosen → ValidatingAction
 └─ SelectionCleared → PlayerTurn
ValidatingAction
 ├─ ActionAccepted → ResolvingAction
 └─ RuleRejected → PlayerTurn
ResolvingAction
 └─ ActionResolved → CheckingResult
CheckingResult
 ├─ QueenSurrounded → GameOver
 ├─ TurnAdvanced → PlayerTurn / AiThinking
 └─ QueenDeadlineForced → PlayerTurn / AiThinking
AiThinking
 └─ AiActionChosen → ValidatingAction
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `HiveRules` controller validates legal placement, Queen-by-four deadline, One Hive connectivity, sliding freedom, stack ownership, insect movement, and Queen surround detection; unit tests cover every insect without Android imports.

## HUD
- Top bar shows turn count, difficulty, active player, and settings access.
- Rule/status strip shows Queen placement status, current rule reminder, and selected piece type.
- Piece tray shows remaining insects with counts and disabled pieces when placement is illegal.
- Canvas annotations show legal placements, legal moves, selected stack, last AI move, and surrounded Queen.
- Prompt/result panel explains rule rejections and final outcome below the hive.
- Victory/defeat presentation follows `design/common/victory-defeat.md`: results never cover the play area.
