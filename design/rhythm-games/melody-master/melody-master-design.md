# Melody Master — Design Document

## Overview
A single-player rhythm game. Colored note gems scroll down a multi-lane fretboard-style highway toward a hit line; the player taps each lane in time with the music. Every track is a **freshly generated melody** rather than a licensed song, which keeps the game original (no copyrighted music) and gives effectively unlimited content. Accuracy drives score, combos build a multiplier, and each run ends with a graded results panel.

Shared rhythm rules (lanes, timing windows, combo/multiplier, audio sync, results, state machine, data model) live in [../../common/rhythm-note-highway.md](../../common/rhythm-note-highway.md). This document covers only what is specific to Melody Master.

## Lanes & Layout
- **5 lanes**, each mapped to a fixed pitch of a chosen scale (low -> high, left -> right) and a distinct color token from `ui/theme/Color.kt`.
- Hit line sits near the bottom; lane buttons are large tap targets (>= 48dp) anchored at the hit line.
- The melody's pitches are quantized onto the 5 lanes (see *Note Generation*), so the lane pattern visually traces the tune's contour.

```
+-------------------------------+
|  L1   L2   L3   L4   L5        |  pitches: do  re  mi  sol  la
|   o              o             |
|         o   o                  |  gems scroll down
|   o                   o        |
+--[ == HIT LINE == ]-----------+
|  (5 lane buttons)             |
+-------------------------------+
|  Score    Combo xMult    Acc% |  <- HUD (below board)
+-------------------------------+
```

## Note Generation (v1 — Random Melodies)
v1 ships **simple, randomly generated melodies**. No external audio files; notes are synthesized/sampled per pitch.

Generation steps for a track:
1. Pick a **key/scale** (major or natural minor) and a **tempo** (BPM) from the difficulty's allowed range.
2. Pick a **track length** in bars (difficulty-dependent).
3. For each beat slot, decide note vs rest using a difficulty-driven **note density**.
4. Choose each note's **scale degree** with a smoothing bias toward small melodic steps (large leaps are rarer) so the result sounds melodic rather than random noise.
5. **Quantize pitch to lane**: map the 5 most-used scale degrees of the run to the 5 lanes so the highway stays readable; out-of-set degrees snap to the nearest lane.
6. Apply **rhythm**: at low difficulty notes land on quarter/half beats only; higher difficulties add eighth notes and occasional two-lane chords.
7. Convert the resulting notes to `Note(lane, hitTimeMs)` scheduled against the song clock.

Determinism: a track is generated from a **seed**. The seed is shown on the results screen so a player can replay or share a memorable track. "Random" picks a new seed each time.

### Tunable Generation Parameters
| Parameter | Effect |
|-----------|--------|
| `bpm` | Tempo; also feeds scroll speed. |
| `noteDensity` | Probability a beat slot has a note vs a rest. |
| `stepBias` | How strongly the next degree prefers a small step over a leap. |
| `subdivision` | Smallest allowed note value (quarter -> eighth -> sixteenth). |
| `chordChance` | Probability a slot spawns a 2-lane chord. |
| `bars` | Track length. |

## Difficulty
Four levels selected on the Settings screen. They adjust generation parameters and the shared timing/scroll levers from the common doc — no new mechanics per level.

| Level | Lanes used | BPM range | Density | Subdivision | Chords | Scroll / windows |
|-------|-----------|-----------|---------|-------------|--------|------------------|
| Easy   | 3 of 5 | 70–90   | low    | quarter        | none       | slow, wide windows |
| Normal | 4 of 5 | 90–110  | medium | + eighth       | rare       | default |
| Hard   | 5 of 5 | 110–140 | high   | + eighth       | occasional | fast |
| Expert | 5 of 5 | 140–170 | high   | + sixteenth    | frequent   | fastest, tight windows |

(Lower-lane difficulties simply leave outer lanes unused so the playfield stays uncluttered.)

## Scoring & Results
- Per-note scoring, combo, and multiplier follow the common doc.
- **Accuracy %** = weighted hit value / max possible value.
- **Closing flourish**: the final phrase of every track is tagged; landing all of its notes without a Miss awards a flat **flourish bonus** and a "Perfect Finish" callout in the results panel.
- Results panel (below the dimmed board) shows: final score, max combo, accuracy %, judgment counts, letter/star grade, the track **seed**, and best-score-for-this-difficulty (highlighted on a new record). Buttons: Replay (same seed), New Track (new seed), Menu.

| Grade | Accuracy |
|-------|----------|
| S | 100% (all Perfect/Great) |
| A | >= 90% |
| B | >= 80% |
| C | >= 65% |
| D | < 65% |

## Audio
- Each lane = one pitch of the active scale, played as a synthesized/sampled tone on hit; a soft backing metronome/pad keeps the pulse.
- Single song clock is the timing source of truth (see common doc).
- A 4-beat count-in precedes the first note; a stinger plays on track completion.

## Screen Flow
Standard per-game flow: **Settings -> How to Play -> Gameplay -> Results**.
- **Settings**: difficulty, scale preference (Major / Minor / Surprise me), and whether to use a Random seed or re-enter a saved seed.
- **How to Play**: explains lanes, tapping on the hit line, combos/multiplier, and the closing flourish.

## State Machine
Reuses the shared rhythm shape (`Idle -> CountIn -> Playing -> Results`) from the common doc.
- `MelodyMasterState` mirrors the shared states; no extra states needed for v1.
- Track generation happens on `TrackLoaded` (seed -> `Track`) before `CountIn`.

## Data Model (game-specific)
Builds on the shared `Note` / `Track` / `Judgment` / `TrackResult` types.
```
enum class Scale { MAJOR, MINOR }
data class GenerationParams(
    val bpm: Int,
    val scale: Scale,
    val noteDensity: Float,
    val stepBias: Float,
    val subdivision: Int,   // beats subdivision: 1=quarter, 2=eighth, 4=sixteenth
    val chordChance: Float,
    val bars: Int
)
data class MelodyTrackRequest(val difficulty: Difficulty, val seed: Long)
// Controller: MelodyGenerator.generate(request) -> Track  (pure, deterministic from seed)
```
The generator is a **pure controller function** (`controller/`): given a `MelodyTrackRequest` it returns a `Track`. No Android imports; fully unit-testable (same seed => identical track; lanes stay within difficulty's used-lane count; all hit times monotonically increase).

## Future Work — Song Generator (post-v1, notes only)
v1 random melodies are intentionally simple. A later **Song Generator** would produce more musical tracks via structured composition rather than per-beat randomness:

1. **Motifs** — build short motifs by pairing a **curated note sequence** (from a hand-authored library of pleasing scale-degree patterns) with a **curated rhythm** (from a library of rhythmic cells). A motif = one note-pattern + one rhythm-pattern.
2. **Phrases** — combine several motifs (with light variation: transposition, inversion, rhythmic augmentation) into a longer phrase.
3. **Form** — arrange phrases into a song using a chosen musical form: **ABA**, **ABAB**, or **ABABCB**, where each letter is a distinct phrase that recurs to give the track structure and memorability.

This is captured here so the v1 generator's seam (the pure `generate(request) -> Track` controller) can later be swapped for the structured generator without touching the highway, scoring, or UI. Curated motif/rhythm libraries and form selection are **out of scope for v1**.

## Out of Scope (v1)
- Curated/structured song generator (see Future Work).
- Sustain/hold notes and slides.
- Imported or licensed music.
- Custom user-composed tracks.
- More than 5 lanes.
