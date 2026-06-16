# Idle Animal Merge — Design Document

## Overview
A passive idle game in which one new animal arrives in the player's grassland every real-world hour. If the arriving animal matches an existing one, the player can merge the pair into a higher-tier animal. There are 100 animals across 10 tiers. The spawn probability for each tier shifts upward as the player's existing collection reaches higher tiers, creating a satisfying long-term discovery loop.

---

## Core Loop
1. Every hour, one animal is spawned according to the current probability table.
2. If the new animal's type matches any animal already in the field, the player may merge them.
3. Merging two same-type animals produces one animal of the next tier (or the highest known if already Tier 10).
4. Higher-tier animals in the field shift spawn probabilities upward.
5. Coins are earned passively; rate increases with average tier of animals present.
6. Optional: spend coins to buy an extra spawn (once per day).

---

## Animal Tiers & Probability

10 animals per tier, 100 animals total. Naming convention example:

| Tier | Example Animals | Base Spawn Weight |
|------|----------------|-------------------|
| 1 | Mouse, Rabbit, Sparrow, Frog, Beetle, … | 100 |
| 2 | Squirrel, Hedgehog, Duck, Crow, … | 0 (unlocked by Tier 1 presence) |
| 3 | Fox, Raccoon, Goose, … | 0 (unlocked by Tier 2 presence) |
| … | … | … |
| 10 | Dragon, Phoenix, Leviathan, … | 0 (unlocked by Tier 9 presence) |

### Probability Unlock Rules
Let `H` = the highest tier currently present in the player's field.
- Tiers ≤ H are always available.
- Tier H+1 becomes available once the player owns at least 2 animals of tier H.
- Spawn weight for tier T = `max(0, 100 − 15 × (T − 1))`, normalized across available tiers.

This means as the player climbs, lower tiers still spawn occasionally (useful for merging), but higher tiers become increasingly common.

---

## Merge Rules
- Two animals of the same type → one animal of the lowest-numbered animal in the next tier.
- Merging always succeeds; no failure state.
- There is a maximum field capacity of 20 slots. If full, incoming animals are queued (up to 3 in queue); player must merge or release to clear space.
- Releasing an animal earns a small coin bonus proportional to tier.

---

## Coin Economy
- Each animal generates `tier² × 0.5` coins per minute passively.
- Merge bonus: earn `tier × 50` coins per successful merge.
- Daily extra spawn costs `200 × H` coins.

---

## Discovery Log
- A bestiary tracks every animal type ever discovered (blurred silhouette until discovered).
- Discovering a new animal for the first time awards a one-time bonus.
- Progress: "X / 100 animals discovered."

---

## Visuals
- Top-down or slight isometric grassland with individual animal sprites.
- Animals idle-animate (graze, hop, fly in place).
- Merge animation: two animals glow and combine with a sparkle burst.
- Tier indicated by a small crown/star icon overlay (1–10 stars).
- Notification badge on app icon when a new animal has arrived.

---

## State Machine
- `IdleAnimalMergeState`: `Idle`, `AnimalArrived`, `MergePrompt`, `FieldFull`
- Events: `HourlySpawn`, `MergeInitiated`, `AnimalReleased`, `FieldCapacityReached`

---

## Data Model
```
data class Animal(val id: String, val name: String, val tier: Int, val fieldSlot: Int)
data class AnimalDefinition(val id: String, val name: String, val tier: Int, val discovered: Boolean)
data class MergeResult(val consumed: Pair<Animal, Animal>, val produced: Animal, val coinsEarned: Long)
```

---

## Out of Scope (v1)
- Trading animals between players
- Seasonal/limited animals
- Sound effects
