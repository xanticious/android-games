# Word Builder Controls — Common Component

Shared interaction model for word games where the player **builds a word from a bank of
letters** by tapping, then submits it. Used by Anagrams, Anagrams (Arcade), and Boggle.
Each game references this file and only describes how its letter bank is generated and
how words are scored.

## Letter Bank
- A row/grid of letter tiles is presented as the only source of letters.
- Tapping a tile appends its letter to the **current entry** shown above the bank.
- A tapped tile is visually "used" (dimmed/lifted) and cannot be tapped again until it is
  returned to the bank (via Backspace or after a submit).
- Boggle additionally constrains taps to **adjacent, not-yet-used** tiles; Anagrams has
  no adjacency rule. The adjacency rule is the only bank difference between the games.

## Current Entry
- The letters tapped so far appear in a single, clearly readable entry line above the
  bank.
- The entry line is the live target of Backspace and Submit; it is never a free-text
  field (these games are tap-driven, not typed).

## Action Buttons
A fixed action row sits below the bank. All word-builder games expose the same four
actions so muscle memory transfers between them:

| Button | Effect |
|--------|--------|
| **Backspace** | Removes the last tapped letter and returns that tile to the bank. |
| **Submit** | Validates the current entry. On success it scores the word and **clears** the entry, returning all tiles to the bank. On failure it shows a brief inline rejection (e.g. "not a word" / "already found") without penalty. |
| **Submit & Keep** | Same as Submit, but the tapped letters stay selected so the player can quickly tweak the end of a long word and submit a related word. The entry is kept rather than cleared. |
| **Give Up** | Ends the round immediately and goes to the results screen. |

- Buttons are disabled when not applicable (e.g. Backspace with an empty entry).
- All buttons meet the 48dp minimum touch target and have light/dark variants.

## Countdown Timer (timed variants only)
- Timed variants (Anagrams Arcade, Boggle) show a countdown in the top bar.
- When the timer reaches zero the round ends exactly as if **Give Up** were pressed.
- Untimed variants (regular Anagrams) hide the timer entirely; the round ends only via
  Give Up or when all target words are found.
- Timer styling and placement follow `hud-elements.md`.

## Target / Found Words Display
- Words already found this round are listed so progress is visible.
- Where a game has a known solution set (Anagrams), unfound targets are shown as
  **blanks**, arranged **alphabetically and grouped by word length** (shortest groups
  first). Each blank reveals its word once found.
- Where the solution set is large or only revealed at the end (Boggle), the in-round
  display shows the running list of the player's found words and the score; the full set
  of possible words is shown on the results screen.

## Results Screen
- Follows `victory-defeat.md`: the board/bank stays visible and the summary appears
  **below** it, never overlaying the play area.
- Shows the final score and, for Boggle, **all possible words** that could have been
  found. For Anagrams, any still-blank targets are revealed.
- Offers New round, Replay, and Back to Lobby actions.

## State Machine Pattern
- Each game owns a dedicated state machine in `state/` exposing `StateFlow<…State>`.
- Typical shape: `Idle → Playing → RoundOver`, with the timer driving an automatic
  `TimeExpired` event in timed variants.
- A pure controller validates entries against the bundled word list
  (see `word-data-sources.md`), generates the solution set, and computes scores with no
  Android imports; unit tests cover validation and scoring.
