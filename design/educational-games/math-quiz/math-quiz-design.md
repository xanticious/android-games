# Math Quiz — Design Document

## Overview
A mental arithmetic drill game. The player is presented with a math problem and must type (or tap) the correct answer as quickly as possible. Settings control which operations are in play, how hard the numbers get, and whether the session is timed, countdown-limited, or untimed. Improvement is tracked across sessions so the player can watch their speed and accuracy grow over time.

---

## Settings

### Operations
The player toggles one or more operations to include. At least one must be active.

| Operation | Symbol | Example |
|-----------|--------|---------|
| Addition | + | 47 + 36 |
| Subtraction | − | 81 − 29 |
| Multiplication | × | 7 × 8 |
| Division | ÷ | 56 ÷ 7 |

When multiple operations are selected, questions are drawn at random from across all active operations.

### Difficulty
One or more difficulty levels can be active at the same time. Questions are drawn evenly across all active levels.

| Level | Addition / Subtraction | Multiplication / Division |
|-------|------------------------|--------------------------|
| **Easy** | 1- and 2-digit operands (sums ≤ 20) | Tables 1–5; divisors 1–5 with no remainder |
| **Medium** | 2-digit operands (sums ≤ 200) | Tables 1–12; divisors 1–12 with no remainder |
| **Hard** | 3-digit operands | Tables 1–25; divisors with remainders up to 2 digits |
| **Expert** | 4-digit operands; mixed operations in one expression (e.g., 47 + 83 − 29) | Tables 1–50; long division with 2-digit divisors and remainders |

Subtraction results are never negative at Easy or Medium (minuend ≥ subtrahend). At Hard and Expert, negative results can occur.

### Timing Mode
Exactly one timing mode is active per session.

| Mode | Description |
|------|-------------|
| **Untimed** | No clock. Player answers at their own pace. Session ends when the question count is reached or the player manually exits. |
| **Stopwatch** | A stopwatch counts up from 0. Session ends when the question count is reached. Final score includes total elapsed time. |
| **Countdown** | Player sets a starting time (1–30 minutes). The clock counts down. When it reaches 0, the session ends regardless of how many questions were answered. |

### Question Count (Stopwatch / Untimed)
The number of questions in a session. Range: 5–100. Default: 20.

Not shown when Countdown mode is selected (time is the limit instead).

### Starting Time (Countdown only)
How many minutes the countdown starts from. Range: 1–30 minutes. Default: 5.

---

## Screen Layout

```
┌────────────────────────────────────┐
│  [Q: X/N or —]      [Timer]        │  ← HUD: question progress, clock
├────────────────────────────────────┤
│                                    │
│         47  +  36  =  ?            │  ← Question (large, centered)
│                                    │
│  ┌──────────────────────────────┐  │
│  │           ____               │  │  ← Answer input field
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │  [7] [8] [9] [⌫]            │  │
│  │  [4] [5] [6] [−]            │  │  ← Numeric keypad (built-in, no system keyboard)
│  │  [1] [2] [3] [✓]            │  │
│  │     [0]   [.]               │  │
│  └──────────────────────────────┘  │
│                                    │
└────────────────────────────────────┘
```

- The answer field is read-only text driven by the keypad; the system soft keyboard is never shown.
- The `−` key on the keypad allows negative answers (Hard/Expert only; hidden at Easy/Medium).
- Pressing `✓` or the check key submits the answer.
- The `.` (decimal) key is hidden unless a question can produce a fractional answer.

---

## Gameplay Loop

1. Settings screen loads with the player's last-used preferences.
2. Player taps **Start**.
3. A question is generated and displayed.
4. Player taps digits on the keypad and submits with `✓`.
5. Immediate feedback is shown inline:
   - **Correct**: the answer field briefly turns green, then the next question appears.
   - **Incorrect**: the answer field briefly turns red, displays the correct answer for 1 second, then the next question appears.
6. Loop continues until the session end condition is met (question count reached, or countdown hits 0).
7. Results screen is shown (see Scorecard below).

The player can exit mid-session via a small **✕** button in the top corner. A partial score is recorded.

---

## Scoring

| Event | Points |
|-------|--------|
| Correct answer | 10 |
| Correct on first attempt (no miskey) | +5 bonus |
| Speed bonus (answered within 3 seconds) | +5 |
| Speed bonus (answered within 1.5 seconds) | +10 (replaces the 3-second bonus) |

In **Countdown** mode, the session score equals total points accumulated before time expired.

In **Stopwatch** and **Untimed** modes, time bonuses still apply per question.

---

## Scorecard

Displayed at the end of a session.

Contents:
- **Score**: total points earned
- **Correct / Total**: e.g., `17 / 20`
- **Accuracy %**
- **Average time per question** (Stopwatch and Countdown modes only)
- **Personal best** for the current operation/difficulty combo (highlighted if beaten)
- Per-operation breakdown (if multiple operations were active): correct rate for each
- **Play Again** button — same settings, new questions
- **Adjust Settings** button — returns to Settings screen

---

## Persistent Progress Tracking

Stored locally per player profile:

- Personal best score per (operation set × difficulty set × timing mode × question count or countdown time) combination
- Total questions answered lifetime
- Total correct answers lifetime
- Average accuracy per operation (lifetime rolling)

---

## State Machine

```
Idle
 └─ OpenGame → Settings

Settings
 ├─ StartSession → GeneratingQuestion
 └─ Back → Idle

GeneratingQuestion
 └─ QuestionReady → AwaitingAnswer

AwaitingAnswer
 ├─ AnswerSubmitted(correct) → ShowingFeedback(correct)
 ├─ AnswerSubmitted(incorrect) → ShowingFeedback(incorrect)
 └─ CountdownExpired → Results           ← Countdown mode only

ShowingFeedback
 ├─ FeedbackDone [more questions] → GeneratingQuestion
 └─ FeedbackDone [session complete] → Results

Results
 ├─ PlayAgain → GeneratingQuestion
 └─ AdjustSettings → Settings
```

---

## HUD

- **Question progress** (top-left): `Q: 7 / 20` in Stopwatch/Untimed modes; hidden in Countdown mode.
- **Timer** (top-right):
  - Stopwatch: counts up from `0:00`.
  - Countdown: counts down from the starting time in `M:SS`; turns amber below 1 minute, red below 30 seconds.
  - Untimed: not shown.
- **Operation indicator**: a small icon row beneath the question showing which operations are active this session (greyed out ones are not in play).

---

## Visual Style

- Question text is large (48sp+) and centered on screen — the player should be able to read it at a glance.
- Keypad buttons are large tap targets (minimum 56×56dp), arranged in phone-dialpad order.
- Correct/incorrect flash animations are brief (300ms) and use the app's success/error color tokens — no full-screen overlay.
- The Scorecard appears below the now-empty question area; it never covers active gameplay.
