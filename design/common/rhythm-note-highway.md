# Rhythm Note Highway

Shared interaction, timing, scoring, and presentation rules for rhythm games (e.g. Melody Master, Tap Rhythm). Individual games specify lane count, note sources, and theming; everything below is common.

## Core Concept

Notes travel along fixed vertical **lanes** from the top of the highway toward a **hit line** near the bottom. The player taps the lane (or its on-screen button) at the moment the note crosses the hit line. Timing accuracy determines the judgment and score.

```
┌───────────────────────────────┐
│  L1   L2   L3   L4   L5        │  ← lanes (count is per-game)
│   ●         ●                  │
│         ●        ●             │  notes scroll downward
│   ●         ●         ●        │
├───[ ▔▔▔ HIT LINE ▔▔▔ ]────────┤  ← judgment zone
│  (tap targets / lane buttons)  │
└───────────────────────────────┘
```

## Timing Windows

Judged on absolute time difference between the tap and the note's scheduled hit time. Windows are symmetric (early or late):

| Judgment | Window (±) | Score | Combo |
|----------|-----------|-------|-------|
| Perfect  | 30 ms     | 100   | keeps + extends |
| Great    | 60 ms     | 70    | keeps + extends |
| Good     | 100 ms    | 40    | keeps + extends |
| Miss     | outside / not tapped | 0 | breaks combo |

A note that scrolls past the hit line untapped beyond the Good window is a Miss. A tap with no note in any window is an **overtap** (no score, no combo break, optional small accuracy penalty per game).

## Combo & Multiplier

- **Combo** = consecutive non-Miss notes. Shown prominently; resets to 0 on Miss.
- **Multiplier** rises with combo, capped per game:

| Combo | Multiplier |
|-------|-----------|
| 0–9   | ×1 |
| 10–24 | ×2 |
| 25–49 | ×3 |
| 50+   | ×4 |

Final note score = base judgment score × current multiplier.

## Scroll Speed & Lead Time

- Notes are scheduled in song time; scroll speed maps song time to vertical pixels.
- **Lead time** is how long a note is visible before its hit time (default ≈ 1.5 s; higher difficulties scroll faster = shorter lead time).
- Scroll speed is constant within a track so spacing reads as rhythm.

## Difficulty Levers (shared)

Difficulty is expressed through data, not new code paths:
- Faster scroll speed / shorter lead time.
- Tighter timing windows (scale the table above).
- More lanes in play and/or denser note placement.
- More simultaneous (chord) notes.

## Audio Sync

- A single monotonic **song clock** (audio playback position) is the source of truth for all timing; visuals follow the clock, never the reverse.
- Each lane maps to a pitch/sample; hitting a note plays its tone, reinforcing the melody.
- A short count-in (e.g. 4 beats) precedes the first note so the player can lock onto the tempo.

## Input

- Tap the lane column anywhere, or its dedicated lane button at the hit line.
- **Chords**: multiple lanes tapped within the same window count independently.
- **Hold/sustain notes** are optional per game (out of scope unless a game's doc enables them).
- No joystick, no drag.

## Results & Victory/Defeat

- Tracks run to completion; there is no mid-board failure overlay.
- End-of-track results follow [victory-defeat.md](victory-defeat.md): the highway dims and a panel slides up **below** the board with final score, max combo, accuracy %, judgment counts (Perfect/Great/Good/Miss), and a letter/star grade.
- Per-game accuracy thresholds map to the star/grade rating.

## State Machine (shared shape)

```
Idle
 └─ TrackLoaded → CountIn
CountIn
 └─ CountInFinished → Playing
Playing
 ├─ NoteJudged → Playing (score/combo update)
 └─ TrackFinished → Results
Results
 └─ (replay / next / menu) → Idle
```

- States: `Idle`, `CountIn`, `Playing`, `Results`.
- Events: `TrackLoaded`, `CountInFinished`, `NoteJudged`, `TrackFinished`.

## Data Model (shared shape)

```
data class Note(val lane: Int, val hitTimeMs: Long)        // optional: durationMs for sustains
data class Track(val bpm: Int, val notes: List<Note>)
enum class Judgment { PERFECT, GREAT, GOOD, MISS }
data class TrackResult(
    val score: Long,
    val maxCombo: Int,
    val accuracy: Float,
    val counts: Map<Judgment, Int>
)
```
