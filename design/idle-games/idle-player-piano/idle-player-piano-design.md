# Idle Player Piano — Design Document

## Overview
A musical idle game in which a player piano generates notes autonomously. A target sequence is displayed; when the piano plays that sequence correctly (in order), the player earns coins and a new, potentially longer target is generated. Upgrades gradually shift the piano's note selection from pure randomness toward biased-but-still-stochastic behavior, creating a satisfying arc from cacophony to near-melody.

---

## Core Loop
1. The piano plays one note every tick (e.g., once per second).
2. A target sequence is displayed (initially 2 notes).
3. The piano tracks whether consecutive notes match the target sequence in order.
4. On a full sequence match, coins are awarded and a new target is generated.
5. Spend coins on upgrades that bias the piano's note selection.
6. At milestones, the target sequence length increases (2 → 3 → 4 → … → 8).

---

## Note System
- 8-note octave (C D E F G A B C′).
- At base level, each note is chosen with uniform probability (1/8 per note).
- Upgrades add a "correct note bias": the probability of the next-needed correct note increases by a configurable amount, with the remaining probability distributed evenly among incorrect notes.

### Example Bias Math
Let `b` = bias strength (0.0 = uniform, 1.0 = always correct).
- P(correct next note) = `1/8 + b × (1 − 1/8)`
- P(any other note) = `(1 − P(correct)) / 7`

---

## Upgrades

| Upgrade | Effect | Prerequisite | Cost |
|---------|--------|--------------|------|
| Tuned Strings | bias +0.05 | — | 50 |
| Better Hammers | bias +0.05, +10% tick speed | Tuned Strings | 200 |
| Music Roll Library | bias +0.10 | Tuned Strings | 500 |
| Mechanical Memory | piano "remembers" last note, never repeats immediately | Better Hammers | 1 000 |
| Harmonic Resonance | bias +0.10, tick speed +10% | Music Roll Library | 3 000 |
| Pedal Mechanism | 5% chance to auto-advance sequence by 1 without a matching note | Mechanical Memory | 8 000 |
| Refined Roll | bias +0.15 | Harmonic Resonance | 20 000 |
| Maestro Calibration | bias +0.20, +20% tick speed | Refined Roll + Pedal Mechanism | 100 000 |

Maximum achievable bias is capped at 0.90 (the piano always retains a small random element).

---

## Sequence Milestones

| Sequence Length | Unlock Condition | Coin Reward per Match |
|-----------------|-----------------|----------------------|
| 2 notes | Starter | 10 |
| 3 notes | 20 matches completed | 30 |
| 4 notes | 75 matches completed | 100 |
| 5 notes | 200 matches completed | 300 |
| 6 notes | 500 matches completed | 800 |
| 7 notes | 1 200 matches completed | 2 000 |
| 8 notes | 3 000 matches completed | 5 000 |

After reaching 8-note sequences, rewards continue to scale but sequence length stays at 8.

---

## Visuals
- A top-down or front-facing stylized player piano.
- Piano keys animate (depress) as each note plays.
- Target sequence displayed as highlighted key positions above the keyboard.
- Matched notes light up green; unmatched positions remain neutral.
- On a successful match, a brief celebration animation (keys ripple, coins animate).
- Notes played scroll as a ticker (last 8 notes visible).

---

## Audio
- Each of the 8 notes plays the corresponding MIDI pitch on hit.
- Match completion plays a brief ascending arpeggio.

---

## State Machine
- `IdlePlayerPianoState`: `Playing`, `SequenceMatched`, `UpgradeMenuOpen`
- Events: `NotePlayed`, `SequenceCompleted`, `SequenceLengthIncreased`, `UpgradePurchased`

---

## Data Model
```
data class PianoNote(val pitch: Int)   // 0–7 mapped to C–C′
data class TargetSequence(val notes: List<PianoNote>)
data class PianoState(val bias: Float, val ticksPerSecond: Float, val matchesCompleted: Int, val sequenceLength: Int)
data class PianoUpgrade(val id: String, val name: String, val cost: Long, val requires: List<String>, val purchased: Boolean)
```

---

## Out of Scope (v1)
- Custom target sequences chosen by the player
- Multiple instruments
- Sheet music export
