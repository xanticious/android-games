# Set Collector — Design Document

## Overview
- Set Collector is a single-player (vs AI) pattern-recognition game based on the card game SET.
- A grid of cards is dealt face-up. Each card has four attributes: number (1–3), symbol (oval/squiggle/diamond), shading (solid/striped/open), and color (red/green/purple).
- A valid set is exactly three cards where, for each of the four attributes, the values across the three cards are either all the same or all different.
- The player races against an AI opponent to call valid sets. The player who identifies the most sets when the deck is exhausted wins.
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Table surface: `Dark1` with a subtle card-grid shadow.
- Card backgrounds: white or `Aqua0` with clear, high-contrast symbol rendering.
- The three symbol colors (red/green/purple) use accessible, high-contrast token values from `ui/theme/Color.kt` (mapped to `Coral`, `Aqua2`, and `Teal4` variants).
- Symbols must be distinguishable by shape, not color alone (shapes are labeled on the card in a tiny legend for colorblind support).
- Player-claimed sets slide off to the player's collection tray at the bottom.
- AI-claimed sets slide to an AI tray at the top.
- Invalid set attempt: selected cards shake and briefly flash `Coral`.
- Valid set: selected cards highlight with `Aqua3` glow, then animate into the collection tray.
- No animation should exceed 400 ms.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Set Collector   Medium    ⚙        │  ← Top bar
├─────────────────────────────────────┤
│  AI sets: 3    ████████████         │  ← AI score tray
├─────────────────────────────────────┤
│  [♦1][◯2][~3][♦1]                  │
│  [◯1][~2][♦3][◯2]                  │  ← Card grid (3 cols × 4 rows default; expands if needed)
│  [~1][♦2][◯3][~1]                  │
├─────────────────────────────────────┤
│  Your sets: 5    ████████████████   │  ← Player score tray
├─────────────────────────────────────┤
│  Deck: 42 remaining   [No Set?]     │  ← Deck info + No Set declaration button
└─────────────────────────────────────┘
```
- Selected cards get a thick `Aqua3` border. A third tap submits the set automatically.
- If no set exists in the current grid, the player may tap **No Set?** to deal 3 extra cards.
- Result and end-of-game panels appear below the grid.

## Settings
- **AI difficulty**: Easy (slow reaction, occasional misses), Medium (default), Hard (near-instant reactions, never misses).
- **Grid layout**: 3×4 (default), 4×3 (landscape-friendly).
- **Deck size**: Full 81-card deck (default), Mini 45-card deck (subset mode).
- **Colorblind symbols** (on/off, default off): adds a small letter label (R/G/P) on each card.
- **Show set count hint** (on/off, default off): displays the number of valid sets remaining in the current grid.
- **AI reaction delay**: 0.5 s–3 s slider (default 1.5 s for Medium). Lets the player tune perceived fairness.

## How to Play
- Twelve cards are dealt face-up in a 3×4 grid.
- Study the grid and find three cards that form a valid set (each attribute is either all-same or all-different across the three cards).
- Tap the three cards in any order. When the third card is tapped, the set is evaluated immediately.
- A valid set collects the three cards to your tray and deals three replacements from the deck.
- An invalid set deselects all three cards and awards no penalty.
- The AI independently searches for sets and will claim them if you are too slow.
- If neither player can find a set, tap **No Set?** to deal three extra cards (grid grows to 15, then 18).
- The game ends when the deck is exhausted and no valid sets remain. The player with more sets wins.

## Controls
- **Tap** a card: selects it (highlighted border); tapping again deselects it.
- **Tap** the third card in a selection: immediately submits and evaluates the set.
- **Tap** No Set? button: deals 3 extra cards (available only when the grid has no valid set, or after a 10-second delay to prevent accidental use).
- After game over, use **Rematch** or **Menu** from the below-grid result panel.

## AI Opponents
- **Easy**: scans the grid at most once every 3–5 seconds; randomly misses ~30% of sets it finds.
- **Medium**: scans with a 1.5-second delay after a new card appears; never misses found sets.
- **Hard**: reacts within 0.5 seconds of any card change; finds all sets nearly instantly.
- AI behavior is purely scan-rate and miss-probability; it uses the same legal set rules as the player.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty | yes |
| Most sets in a single game | yes |
| Fastest single set identification | yes |
| Games with no invalid attempts | yes |
| Longest win streak per difficulty | yes |

## State Machine
- A dedicated `SetCollectorStateMachine` in `state/` exposes `StateFlow<SetCollectorState>`.
```
Idle
 └─ StartGame → Dealing
Dealing
 └─ GridReady → Playing
Playing
 ├─ CardSelected [< 3 cards] → Playing (selection updated)
 ├─ CardDeselected → Playing (selection updated)
 ├─ SetSubmitted [valid] → CollectingSet
 ├─ SetSubmitted [invalid] → Playing (selection cleared)
 ├─ NoSetDeclared [valid] → Dealing (3 extra cards added)
 ├─ AiSetFound → CollectingAiSet
 └─ DeckExhausted + NoSetsInGrid → GameOver
CollectingSet / CollectingAiSet
 └─ AnimationComplete → Dealing (replacements dealt) / Playing (if deck empty)
GameOver
 ├─ Rematch → Dealing
 └─ BackToMenu → Idle
```

## HUD
- Top bar: title, difficulty, settings.
- AI score tray: number of sets claimed by the AI; tray grows rightward.
- Player score tray: number of sets claimed by player; tray grows rightward.
- Deck counter: shows cards remaining in the deck.
- No Set? button: visually distinct; disabled for 10 seconds after each card deal to prevent misuse.
- Result panel below grid: sets each player claimed, who won, personal bests; never covers the card grid.
