# Idle Combat Training — Design Document

## Overview
A 2D idle game in which a ninja trains against increasingly tough dummies. The ninja starts barely knowing a single move and improves through repetition — accuracy increases, new moves are unlocked, and combo chains emerge. Destroying a dummy earns coins; the next dummy has more HP and offers a larger reward.

---

## Core Loop
1. Ninja automatically attempts moves against the current dummy.
2. Each move has a hit-chance; a successful hit deals damage.
3. When the dummy's HP reaches 0 it is destroyed, coins are earned, and a tougher dummy replaces it.
4. Accumulated coins are spent on training upgrades.
5. Upgrades increase accuracy, damage, move variety, and combo potential.

---

## Move System

| Move | Base Hit Chance | Base Damage | Unlock Condition |
|------|----------------|-------------|-----------------|
| Clumsy Jab | 30% | 2 | Starter |
| Front Kick | 45% | 4 | 5 dummies destroyed |
| Palm Strike | 55% | 5 | First upgrade tier |
| Spinning Kick | 40% | 8 | 20 dummies destroyed |
| Elbow Drop | 60% | 7 | Second upgrade tier |
| Combo Finisher | 70% | 15 | 3 consecutive hits land |

Each session the ninja cycles through unlocked moves in sequence. Hit chances improve with upgrades.

---

## Upgrades

| Upgrade | Effect | Prerequisite |
|---------|--------|--------------|
| Basic Footwork | +10% hit chance (all moves) | — |
| Stance Training | +5% hit chance, +1 damage | Basic Footwork |
| Strength Conditioning | +3 damage (all moves) | — |
| Speed Drills | +0.5 hits per second | Stance Training |
| Focus Technique | unlocks Combo Finisher | Speed Drills |
| Iron Fist | critical hits deal 2× damage | Strength Conditioning |
| Advanced Kata | unlocks Spinning Kick early | Basic Footwork |
| Master's Discipline | all hit chances +15% | Focus Technique + Iron Fist |

---

## Dummy Progression

| Dummy # | HP | Reward | Notes |
|---------|----|--------|-------|
| 1 | 20 | 10 | Tutorial |
| 2–5 | 30–60 | 15–40 | Easy grind |
| 6–15 | 80–200 | 50–200 | Needs upgrades |
| 16–30 | 250–600 | 250–800 | Mid-game |
| 31+ | scales × 1.4 per dummy | scales × 1.3 | Late-game |

---

## Visuals
- Side-scrolling 2D dojo background.
- Ninja sprite animates a distinct pose per move.
- Dummy shows visible crack/damage states (pristine → cracked → heavily damaged → destroyed).
- Hit/miss indicators: flash green on hit, translucent grey on miss.
- Coins fly out of the dummy on destruction.

---

## State Machine
- `IdleCombatTrainingState`: `Training`, `UpgradeMenuOpen`, `DummyDestroyed`
- Events: `MoveAttempted`, `HitLanded`, `MissFired`, `DummyDefeated`, `UpgradePurchased`

---

## Data Model
```
data class Move(val name: String, val baseHitChance: Float, val baseDamage: Int, val unlocked: Boolean)
data class Dummy(val number: Int, val maxHp: Int, val reward: Long)
data class CombatUpgrade(val id: String, val name: String, val cost: Long, val requires: List<String>, val purchased: Boolean)
```

---

## Out of Scope (v1)
- PvP or head-to-head modes
- Multiple ninja characters
- Sound effects
