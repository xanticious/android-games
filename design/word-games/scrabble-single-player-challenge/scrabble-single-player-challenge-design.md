# Scrabble Single Player Challenge — Design Document

## Overview
- A solo, puzzle-style Scrabble variant with **no opponent**: each round presents a
  **random board** (some letters already placed) and a **random rack**, and the player must
  find the **single highest-scoring legal word** they can play.
- A session is **10 rounds in a row**; the round scores sum to a session total, and the
  player tries to **beat their personal-best total**.
- After each round, the player sees the **top 20 words** they could have played (best plays
  for that board/rack), so they learn from each puzzle.
- Plays are validated against the **Scrabble players dictionary**; illegal plays are
  rejected. See `design/common/word-data-sources.md`. There is **no timer**.
- Shares board rendering, tile placement, and legal-word enforcement with regular
  `scrabble`; this doc describes only the single-player challenge differences. The board is
  always the anchor and results never overlay it (`design/common/victory-defeat.md`).

## Difference From Regular Scrabble
| Aspect | Scrabble | Single Player Challenge |
|--------|----------|-------------------------|
| Opponent | AI (3 difficulties) | None |
| Goal | Win the full game | Best single word each round |
| Structure | One full game | 10 fixed rounds → session total |
| Board | Builds over the game | Random pre-seeded board each round |
| Feedback | Final result | Top-20 possible plays after each round |
| Persistence | Win/loss stats | Personal-best session total |

## Screen Layout
```
┌─────────────────────────────────────┐
│ SP Challenge   Round 4/10  Total 287 [⚙ ?]│  ← Top bar / session strip
├─────────────────────────────────────┤
│   ┌───────────── 15×15 ───────────┐  │
│   │  random pre-seeded board       │  │  ← Board anchor
│   └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│   Rack:  [Q][U][A][R][T][Z][E]        │
│   Play   Recall   Shuffle   Skip      │  ← Action row
└─────────────────────────────────────┘
```
- Portrait stacks board, rack, and actions; board scales to fit legibly.
- The round-results panel (player's word + top-20 list) appears **below** the board.

## Settings
- **Board density** (Sparse / Medium / Dense; default Medium): how many letters are
  pre-placed on the random board.
- **Rack difficulty** (Balanced / Spicy; default Balanced): Spicy biases racks toward
  high-value but awkward letters.
- **Show top-N after round** (10 / 20; default 20): size of the best-plays list.
- Pre-filled with the last-used values per `puzzle-flow.md`.

## How to Play
- Each round gives you a random board and a random seven-tile rack.
- Place tiles to form one legal word connecting to the board, then **Play** to lock it in;
  illegal words are rejected before they count.
- You play exactly one word per round (or **Skip** for zero), aiming for the highest score.
- After the round, review the **top 20** best possible plays, then continue to the next.
- After 10 rounds, your session total is compared to your personal best.

## Controls
- Same placement controls as regular Scrabble: drag rack tiles, Recall, Shuffle, Play.
- **Skip** ends the round with no word (zero points) when no good play is found.
- Settings (⚙) and How to Play (?) from the top bar.

## Round & Board Generation
- A pure controller seeds the board with a random set of legal, connected tiles at the
  chosen density and deals a random rack.
- It enumerates **all legal plays** for that board/rack against the players dictionary,
  scoring each, to produce both the player's achievable maximum and the **top-20** list.
- Generation guarantees at least one scoring play exists; otherwise it re-rolls so no round
  is unsolvable.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Personal-best session total (10 rounds) | yes |
| Best single-round score | yes |
| Average round score | yes |
| Sessions completed | yes |
| Rounds where player matched the top play | yes |
- Round score is the standard Scrabble score of the played word (tile values + premiums +
  bingo bonus); the session total is the sum of the 10 rounds.

## State Machine
- A dedicated `ScrabbleChallengeStateMachine` in `state/` exposes
  `StateFlow<ScrabbleChallengeState>`.
```
Idle
 └─ SessionStarted → RoundSetup
RoundSetup
 └─ BoardAndRackReady → Playing
Playing
 ├─ TilePlaced / TileRecalled / RackShuffled → Playing
 ├─ PlayValidated → RoundResult     (illegal → rejected, stays Playing)
 └─ Skipped → RoundResult
RoundResult
 ├─ NextRound → RoundSetup          (rounds 1–9)
 └─ SessionFinished → SessionSummary (after round 10)
SessionSummary
 └─ NewSession / Menu → Idle
```
- The pure controller reuses `ScrabbleRules` for placement legality, dictionary validation,
  and scoring, and adds random board/rack seeding plus full legal-move enumeration for the
  top-20 list; unit tests cover seeding solvability, scoring, and top-N ranking with no
  Android imports.

## Results / Session Summary
- Per `design/common/victory-defeat.md`: the board stays visible; the round-results panel
  below shows the player's word, its score, and the **top 20** best plays.
- The session summary after round 10 shows the total, whether a new personal best was set,
  and offers New Session and Menu.
