# Tap Rhythm — Design Document

## Overview
A two-button **hold-and-release** rhythm survival game. Two columns of light descend from the top of the screen — one above a **left** button, one above a **right** button — in a Beat Saber–style "incoming beam" presentation. Each falling **beam** must be *caught* by **pressing and holding** the matching button as the beam reaches the catch line, and *released* the instant the beam ends. The two columns are independent, so the game tests each thumb separately and, increasingly, both at once. There is no song to finish: you play until you **die**, and the game continuously gets faster, longer, and more intricate the longer you survive.

This game uses the shared timing-window/judgment vocabulary from [../../common/rhythm-note-highway.md](../../common/rhythm-note-highway.md) but **diverges from the tap-on-hit-line model**: every note here is a **sustain** (hold) note with a defined start and end, and there are exactly **two lanes**, one per thumb.

## Why Two Buttons / Hold + Release
- Single-tap rhythm is well covered by Melody Master. Tap Rhythm's hook is **independent thumb control**: holding the right beam while the left thumb taps a quick sequence, then swapping.
- A beam has a **start edge** (press here) and an **end edge** (release here). Holding too short, too long, or pressing the wrong column all count against you.

## Layout
Two fixed lanes fill the screen; beams descend toward a **catch line** sitting just above the two large thumb buttons.

```
+-------------------+-------------------+
|       LEFT        |       RIGHT       |
|     |#####|       |                   |   each beam is a vertical bar of light;
|     |#####|       |     |###|         |   its on-screen LENGTH = how long to hold
|     |#####|       |     |###|         |
+--[== CATCH LINE ==]--[== CATCH LINE ==]+   press when the beam's head crosses,
|   (  L button  )  |   (  R button  )  |   release when its tail crosses
+-------------------+-------------------+
|  Score  Streak xMult     Survived 0:42|   <- HUD (below the play area)
+-------------------+-------------------+
```

- Two oversized buttons (≥ 48dp, comfortably thumb-sized) anchor the bottom corners.
- The HUD (score, streak/multiplier, survival time) lives **below** the play area, never over it.

## The Beam (sustain note)
Each beam has:
- a **lane** (LEFT or RIGHT),
- a **start time** (when its head reaches the catch line → press),
- a **duration** (how long to keep holding → its visible length),
- a **color** that *roughly encodes its duration* so the player can read hold length at a glance.

### Color ↔ Duration Encoding
The beam's color is a fast pre-read of how long you'll hold it. Bucketed, using `ui/theme/Color.kt` tokens only (no hex here):

| Beam length | Meaning | Palette role |
|-------------|---------|--------------|
| Short (tap-and-let-go) | brief hold | cool/low token |
| Medium | moderate hold | mid token |
| Long (sustained) | long hold | warm/high token |

(The continuous on-screen length is the exact value; color is the at-a-glance bucket.)

## Judging a Beam
A beam is scored on **two edges**, reusing the shared timing windows (Perfect/Great/Good/Miss) from the common rhythm doc, applied to *each* edge:

1. **Press edge** — how close the button-down is to the beam's start time.
2. **Release edge** — how close the button-up is to the beam's end time.

Rules:
- **Holding through** the beam keeps it "lit"; visual fill drains as you hold so the player sees progress.
- **Releasing too early** breaks the hold → the remainder of the beam is a Miss.
- **Releasing too late** (still holding past the end window) → the release edge is a Miss.
- **Pressing the wrong column** or pressing with no beam present is an **overtap** (no score; optional small penalty per the common doc).
- A beam never caught at all (button never pressed) is a full **Miss**.
- Both columns are judged independently and simultaneously.

## Scoring, Streak & Survival
- Per-edge judgment scores and the **streak → multiplier** ladder follow [../../common/rhythm-note-highway.md](../../common/rhythm-note-highway.md) (streak = consecutive clean edges; a Miss resets it).
- **Health/Life**: the player has a small health pool. Misses (and bad edges) drain health; clean Perfects restore a little. **Health reaching 0 = death = game over.** This is the survival fail condition (the only place a full-screen effect is allowed, shown *after* play stops, per [../../common/victory-defeat.md](../../common/victory-defeat.md)).
- The headline result is **survival time** (and score); there is no "track complete".

## Difficulty Ramp (endless)
Three difficulties are picked on the Settings screen; each sets the **starting point** and **how aggressively the ramp climbs** — the mechanics are identical, only the generation parameters scale over time.

| Difficulty | Start | Ramp |
|------------|-------|------|
| Easy   | one beam at a time, generous durations, slow fall, wide windows | gentle, slow climb |
| Normal | occasional two-hand overlap, mixed durations | moderate climb |
| Hard   | frequent overlaps from the start, faster fall, tighter windows | steep climb |

As **survival time** increases, the generator (within any difficulty) escalates along these axes:
- **Faster fall speed** (shorter lead/read time).
- **Denser beams** (shorter rests between beams in a lane).
- **More overlap** between lanes (both thumbs busy at once: independent press/hold/release on each side).
- **More intricate rhythms** (off-beat starts, staggered release points, alternating short/long beams).
- **Tighter timing windows** (scaling the shared judgment table).

Early Easy might be "one beam at a time, hold for a comfortable beat." Later Easy — or any point in Hard — layers independent left/right sequences with mismatched hold lengths.

## Beam Generation
- Beams are generated **procedurally and endlessly** from a **seed** (shown on the game-over screen so a good run can be replayed). Difficulty + elapsed survival time feed the generation parameters above.
- Generation is a **pure controller** (`controller/`): given `(difficulty, seed, elapsedMs)` it yields the next beams with `(lane, startMs, durationMs, colorBucket)`. No Android imports; deterministic from the seed and fully unit-testable (e.g. *same seed ⇒ identical beam stream*, *density/fall-speed increase monotonically with elapsed time*, *Easy never overlaps lanes before time T*).

### Tunable Generation Parameters
| Parameter | Effect |
|-----------|--------|
| `fallSpeed` | Beam descent speed; shorter read time as it rises. |
| `beamDensity` | How tightly beams pack within a lane. |
| `overlapChance` | Probability both lanes are active simultaneously. |
| `durationMix` | Distribution of short/medium/long holds. |
| `windowScale` | Multiplier applied to the shared timing windows. |

## Audio
- A steady musical pulse plays underneath; each lane maps to a distinct **tone** sounded while a beam is correctly held (reinforcing the hold).
- A single monotonic clock is the timing source of truth (per the common rhythm doc).
- A short count-in precedes the first beam; a distinct cue plays on death.

## Screen Flow
Standard per-game flow: **Settings → How to Play → Gameplay → Results**.
- **Settings**: difficulty (Easy/Normal/Hard), and Random seed vs. a re-entered saved seed.
- **How to Play**: explains the two columns, press-on-head / release-on-tail, the color→duration cue, independent thumbs, and that you play until your health runs out.
- **Results (game over)**: survival time, score, max streak, judgment counts, the run **seed**, and best survival time for the difficulty (highlighted on a new record). Buttons: Replay (same seed), New Run (new seed), Menu.

## State Machine
Adapts the shared rhythm shape (`Idle → CountIn → Playing → Results`) from the common doc; "Results" is reached by **death**, not track end.
```
Idle
 └─ RunStarted → CountIn
CountIn
 └─ CountInFinished → Playing
Playing
 ├─ EdgeJudged → Playing      (press/release edge scored; health & streak update)
 ├─ Tick → Playing            (advance clock; spawn next beams; escalate difficulty)
 └─ HealthDepleted → Results  (death)
Results
 └─ (replay / new run / menu) → Idle
```
- States: `Idle`, `CountIn`, `Playing`, `Results`.
- Events: `RunStarted`, `CountInFinished`, `EdgeJudged`, `Tick`, `HealthDepleted`.

## Data Model (game-specific)
Builds on the shared `Judgment` type; replaces the tap-only `Note` with a sustain **Beam**.
```
enum class Lane { LEFT, RIGHT }
enum class ColorBucket { SHORT, MEDIUM, LONG }     // duration-at-a-glance color
data class Beam(
    val lane: Lane,
    val startMs: Long,
    val durationMs: Long,
    val color: ColorBucket
)
enum class Edge { PRESS, RELEASE }
data class EdgeJudgment(val beam: Beam, val edge: Edge, val judgment: Judgment)
data class RunRequest(val difficulty: Difficulty, val seed: Long)
data class RunResult(
    val survivedMs: Long,
    val score: Long,
    val maxStreak: Int,
    val counts: Map<Judgment, Int>,
    val seed: Long
)
// Controller: BeamGenerator.next(difficulty, seed, elapsedMs) -> List<Beam>   (pure, deterministic)
```

## Out of Scope (v1)
- More than two lanes / more than two buttons.
- Imported or licensed music (procedural pulse only).
- Note types beyond press-hold-release sustains (no slides, no taps-only).
- Online leaderboards (best time is local only).
