# Flash Cards — Design Document

## Overview
A single, unified flash card game that works with any card pack selected in Settings. The player is shown one side of a card, thinks of the answer, taps Flip to reveal the other side, then marks whether they got it right or wrong. The game tracks performance per card across sessions and supports four play modes for different learning goals.

---

## Card Pack System

### Card Pack Metadata
Every card pack is a self-contained data file with:
- `id` — unique stable identifier (e.g., `us_state_capitals`)
- `name` — display name (e.g., "US State Capitals")
- `description` — one-sentence summary shown in the pack picker
- `schoolLevel` — tag(s) for filtering: `Elementary`, `Middle School`, `High School`, `College`
- `subject` — subject tag: `Geography`, `Math`, `Science`, `Language`, `History`, `Vocabulary`, `Other`
- `frontLabel` — what the front side represents (e.g., "State")
- `backLabel` — what the back side represents (e.g., "Capital City")
- `frontType` — data type of the front face. Supported: `Text`
- `backType` — data type of the back face. Supported: `Text`
- `cards` — ordered list of `Card` entries

### Card Entry
```
Card(
  id: String,       // unique within the pack
  front: String,    // text shown on the front face
  back: String      // text shown on the back face
)
```

Only `Text` content types are supported in this version. Future versions may add `Image`.

---

## Included Card Packs

### Elementary School
| Pack ID | Name | Front | Back | Cards |
|---------|------|-------|------|-------|
| `us_state_capitals` | US State Capitals | State | Capital City | 50 |
| `multiplication_tables` | Multiplication Tables | Problem (e.g., "7 × 8") | Answer | 144 (2–12 tables) |
| `sight_words_fry_100` | Sight Words — Fry Top 100 | Word | Sentence using the word | 100 |
| `animal_classifications` | Animal Classifications | Animal Name | Class (Mammal, Reptile, etc.) | 60 |
| `us_state_abbreviations` | US State Abbreviations | Abbreviation | State Name | 50 |

### Middle School
| Pack ID | Name | Front | Back | Cards |
|---------|------|-------|------|-------|
| `world_capitals` | World Capitals | Country | Capital City | 195 |
| `periodic_table_symbols` | Periodic Table — Symbols | Chemical Symbol | Element Name | 118 |
| `us_presidents` | US Presidents | Ordinal (e.g., "16th President") | President's Name | 47 |
| `literary_terms` | Literary Terms | Term | Definition | 50 |
| `geometry_formulas` | Geometry Formulas | Shape + Property (e.g., "Area of a Circle") | Formula | 30 |

### High School
| Pack ID | Name | Front | Back | Cards |
|---------|------|-------|------|-------|
| `periodic_table_atomic_numbers` | Periodic Table — Atomic Numbers | Element Name | Atomic Number | 118 |
| `sat_vocabulary` | SAT Vocabulary | Word | Definition | 200 |
| `world_history_events` | World History: Key Events | Event Name | Year & Brief Context | 80 |
| `spanish_vocab_beginner` | Spanish Vocabulary — Beginner | English Word | Spanish Word | 150 |
| `biology_cell_organelles` | Biology: Cell Organelles | Organelle Name | Function | 20 |
| `algebra_identities` | Algebra Identities | Identity Name | Expression | 25 |

### College (1st Year)
| Pack ID | Name | Front | Back | Cards |
|---------|------|-------|------|-------|
| `organic_functional_groups` | Organic Chemistry: Functional Groups | Group Name | Structure & Properties | 20 |
| `amino_acids` | Biology: Amino Acids | Amino Acid Name | 3-Letter Code & Side Chain Type | 20 |
| `psychology_key_terms` | Psychology: Key Terms | Term | Definition | 80 |
| `economics_key_terms` | Economics: Key Terms | Term | Definition | 70 |
| `statistics_formulas` | Statistics: Formulas | Formula Name | Expression & Variables | 30 |

---

## Settings

Settings are scoped per card pack so the player can have different preferences for different packs.

### Card Pack
A scrollable, searchable pack picker. Packs are grouped by school level and filterable by subject. Selecting a pack saves it as the active pack for this game session.

### Show Side
Controls which face of the card is revealed first each round.
- **Front Side** — always shows the front face first (e.g., "Arkansas")
- **Back Side** — always shows the back face first (e.g., "Little Rock")
- **Random Side** — each card randomly decides which face is shown first

### Duration
How long the session runs. One of:
- **Full Deck** — (Quiz mode default) show every card in the pack exactly once; no other cutoff
- **Number of Cards** — stop after showing N cards (player sets N, range 5–500)
- **Number of Minutes** — stop after M minutes have elapsed (player sets M, range 1–60)

Duration of **Full Deck** is only available in **Quiz** mode. All other modes require Number of Cards or Number of Minutes.

### Mode
- **Quiz** — Shuffles the deck, presents all cards (or up to the duration limit) one at a time without repeats, then shows a Scorecard. Missed cards are noted but not re-shown mid-session.
- **Shuffled** — Shuffles the deck and works through it in order. When the last card is reached, shuffles again and starts over. Runs until the duration limit is reached.
- **Random** — Picks a card uniformly at random each time (with no history constraint). Runs until the duration limit is reached.
- **Focused** — Picks the next card using a difficulty-weighted random selection biased toward cards the player has answered incorrectly most often (see Focused Mode Algorithm). Never shows the same card twice in a row. Runs until the duration limit is reached.

---

## Focused Mode Algorithm

Each card maintains a **difficulty weight** across sessions:

- **Never attempted**: default weight = `1.0`
- **After being answered**: weight = `incorrectCount / totalShown` (pure failure rate)
  - Minimum weight = `0.05` (so a mastered card can still occasionally appear)
  - Maximum weight = `1.0`

When selecting the next card:
1. Compute each card's weight as described above.
2. Remove the most-recently-shown card from the eligible pool for this draw.
3. Perform a weighted random draw from the remaining pool.
4. Return the selected card as the next card to show.

**Example**: In a 3-card deck, if Card A has been answered correctly 4/5 times (1 wrong) and Card B has been answered correctly 1/5 times (4 wrong), Card B's weight = 0.8 and Card A's weight = 0.2, making Card B four times more likely to be drawn.

---

## Screen Layout

```
┌────────────────────────────────────┐
│  [Pack Name]     [X/N or Timer]    │  ← HUD: pack name, progress or countdown
├────────────────────────────────────┤
│                                    │
│  ┌──────────────────────────────┐  │
│  │                              │  │
│  │         FRONT FACE           │  │  ← Card face (text, centered)
│  │                              │  │
│  │       "Arkansas"             │  │
│  │                              │  │
│  └──────────────────────────────┘  │
│         [ Flip ▼ ]                 │  ← Tap to reveal back face
│                                    │
│    (back face hidden until flip)   │
│                                    │
│  [Face label: "State"]             │  ← Subtle label describing what the face is
└────────────────────────────────────┘
```

After flip:

```
┌────────────────────────────────────┐
│  [Pack Name]     [X/N or Timer]    │
├────────────────────────────────────┤
│                                    │
│  ┌──────────────────────────────┐  │
│  │                              │  │
│  │         BACK FACE            │  │
│  │                              │  │
│  │        "Little Rock"         │  │
│  │                              │  │
│  └──────────────────────────────┘  │
│                                    │
│   [ ✓ Got It! ]   [ ✗ Oops ]      │  ← Shown only after flip
│                                    │
└────────────────────────────────────┘
```

- The "Got It!" button is styled in a positive/success color.
- The "Oops" button is styled in a neutral/warning color (not alarming — mistakes are part of learning).
- After the player taps either button, the next card immediately replaces the current one (no animation required).
- Both the front face label and back face label are shown as small secondary text below the respective face.

---

## Gameplay Loop

1. Player opens Flash Cards, selects a card pack and configures settings.
2. Session begins. First card is drawn according to the chosen mode.
3. The chosen side is displayed face-up.
4. Player thinks of the other side's content.
5. Player taps **Flip** to reveal the hidden face.
6. Player taps **Got It!** or **Oops** to record their result.
7. The next card is drawn and displayed. Loop continues from step 3.
8. Session ends when:
   - Duration limit is reached (cards exhausted or time elapsed), **or**
   - Quiz mode has shown every card in the deck (if Full Deck duration selected).
9. Results screen is shown (see Scorecard below).

---

## Scorecard (Quiz Mode)

At the end of a Quiz session, a Scorecard is displayed below the (non-interactive) last card area.

Contents:
- **Score**: `X / N correct` with a percentage
- **Time taken** (if timed)
- **Missed Cards** list: front face of each card answered "Oops", with the correct back face shown alongside
- **Play Again** button — runs a new Quiz session with the same settings
- **Review Missed** button — starts a Focused session containing only the cards that were answered "Oops" this session (no duration limit, runs until all missed cards are seen)
- **Back to Settings** button

For non-Quiz modes, a shorter summary is shown:
- Cards seen, correct count, incorrect count, session duration.
- **Play Again** and **Back to Settings** buttons.

---

## Persistent Progress Tracking

Per card, per pack, stored locally on the device:
- `totalShown` — how many times this card has been presented
- `correctCount` — how many times the player tapped "Got It!"
- `incorrectCount` — how many times the player tapped "Oops"
- `lastShownTimestamp` — epoch millis of most recent presentation

This data drives Focused Mode weights and is visible in a per-pack Stats screen (accessible from the pack picker).

---

## State Machine

```
Idle
 └─ OpenGame → PackPicker

PackPicker
 ├─ PackSelected → Settings
 └─ Back → Idle

Settings
 ├─ StartSession → DrawingCard
 └─ Back → PackPicker

DrawingCard
 └─ CardReady → ShowingFront

ShowingFront
 ├─ FlipTapped → ShowingBack
 └─ SessionEnded → Results        ← only if timer expired mid-card

ShowingBack
 ├─ GotIt → RecordingResult(correct)
 └─ Oops → RecordingResult(incorrect)

RecordingResult
 ├─ MoreCards → DrawingCard
 └─ SessionComplete → Results

Results
 ├─ PlayAgain → DrawingCard       ← re-applies same settings
 ├─ ReviewMissed → DrawingCard    ← Quiz mode only; new Focused session of missed cards
 └─ BackToSettings → Settings
```

---

## HUD

- **Pack name**: top-left, small/secondary style.
- **Progress**: top-right.
  - Quiz / Shuffled / Focused modes with card count duration: `Card X of N`.
  - Number of Minutes duration: countdown timer `M:SS`.
  - Full Deck (Quiz only): `X / N` cards done.
- **Face label**: appears beneath the displayed card face in small secondary text (e.g., "State" or "Capital City").
- No score counter is shown during play — scoring is revealed only at the Scorecard to avoid priming the player.

---

## Visual Style

- Card face: large rounded-rectangle card on a neutral background. Material 3 elevated surface.
- Front face and back face use the same card visual; the label beneath changes.
- Flip button: centered below the card. Uses an animated icon (card flipping) to hint at the action.
- Got It / Oops buttons: full-width pill buttons below the flipped card, side by side. Appear with a subtle fade-in after flip to prevent accidental taps.
- No game board that victory/defeat UI could obstruct — the Scorecard replaces the card area.
