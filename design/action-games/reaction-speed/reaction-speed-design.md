# Reaction Speed — Design Document

## Overview
A high-definition reflex-testing mini-game collection that measures and tracks the player's reaction time across multiple test types. All tests record precise timings and display trends over sessions.

## Visual Style
- Clean, minimal design: dark background, bold geometric shapes, high-contrast colors.
- Each test type has its own distinct color theme (from `ui/theme/Color.kt`).
- Visual prompts are unambiguous — shape, color, and text all convey the same signal.
- Feedback is immediate: correct responses produce a flash + expansion animation; incorrect or late responses produce a brief red flash.
- Results display with a smooth counter-roll animation.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Test Name]  [Best: 000ms]     │  ← HUD (top)
├─────────────────────────────────┤
│                                 │
│       TEST AREA                 │
│   (large, uncluttered prompt)   │
│                                 │
├─────────────────────────────────┤
│  [Average: 000ms]  [Last: 000ms]│  ← Session stats (bottom)
└─────────────────────────────────┘
```

## Test Types

### 1. Tap the Flash
- A blank screen. At a random moment (2–6 seconds after "Ready"), the screen flashes a bright color.
- Player taps as fast as possible after the flash.
- Measure: time between flash and tap (milliseconds).
- False start: tapping before the flash shows "Too early!" and records a penalty.

### 2. Tap the Shape
- Multiple shapes appear simultaneously (3–5 shapes of different types).
- One shape type is designated (shown briefly before the test round).
- Player must tap only that shape as quickly as possible.
- Measure: time from shape appearance to correct tap.
- Wrong tap: records an error; adds 200ms penalty.

### 3. Color Match
- A word and a color swatch appear simultaneously.
- Tap "Match" if the word's text color matches the color swatch; tap "Mismatch" if they differ.
- A Stroop-style test for cognitive reaction speed.
- Measure: time from display to correct answer tap.

### 4. Sequence Tap
- A sequence of 3–5 numbered or colored circles appears briefly, then disappears.
- Player must tap them in the original order from memory.
- Measure: time per tap in sequence + accuracy.

### 5. Double Tap Race
- Two targets appear simultaneously on opposite sides of the screen.
- Player must double-tap: one finger on each target simultaneously within a time window.
- Tests two-thumb coordination and speed.

## Session Structure
- Player selects a test type from a menu.
- Each session runs 10 rounds of the selected test.
- After 10 rounds, the session result screen shows:
  - Average reaction time across rounds.
  - Best single reaction time.
  - Worst single reaction time.
  - Trend graph (bar chart) of all 10 rounds.
  - Comparison to personal best session average.
- Personal best session average is stored per test type per profile.

## Statistics Stored (local, per profile)
- Per test type:
  - All-time best single reaction time.
  - Best session average.
  - Last 10 session averages (for trend tracking).
- Overall across all tests: combined average.

## Scoring (for leaderboard display)
- No points as such — the primary metric is reaction time in milliseconds.
- Performance bands:
  - ≤150ms: Elite
  - 151–200ms: Fast
  - 201–250ms: Average
  - 251–350ms: Developing
  - >350ms: Keep Practicing

## State Machine
```
Idle
 └─ TestSelected → PreRound
PreRound
 └─ Ready → Waiting (random delay 2–6s)
Waiting
 ├─ EarlyTap → FalseStart
 └─ PromptAppears → Measuring
FalseStart
 └─ RecordPenalty → PreRound (next round)
Measuring
 ├─ CorrectResponse → RecordTime → PreRound or SessionEnd
 └─ WrongResponse → RecordError → PreRound or SessionEnd
SessionEnd
 └─ (show results) → Idle
```

## HUD
- Test name: top-left.
- Personal best for this test type: top-right.
- Current round number: top-center (e.g., "Round 4/10").
- Last recorded time: bottom-left.
- Running session average: bottom-right.
