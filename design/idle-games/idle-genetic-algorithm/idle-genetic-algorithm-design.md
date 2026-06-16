# Idle Genetic Algorithm — Design Document

## Overview
An idle game built around a real-time genetic algorithm simulation. Cars are evolved across generations on a procedurally generated 2D track. The player earns passive income from each generation's survivors and bonus coins when a car crosses a checkpoint. Coins are spent on hardware upgrades that the next evolution run benefits from, creating a satisfying loop of watching AI improve with better parts.

---

## Core Loop
1. A generation of cars (e.g., 10) is spawned on the current track.
2. Each car runs until it stops moving or falls off the track.
3. Fitness = distance traveled. Top performers are selected as parents.
4. Child cars are created via crossover + mutation of parent parameters.
5. Passive income: each surviving car earns a small coin trickle proportional to distance.
6. Checkpoint bonus: flat coin bonus each time any car in a generation reaches a new checkpoint.
7. Between generations, the player may spend coins on upgrades; the next generation inherits the improved part specs.
8. Periodically, a new procedurally generated track is introduced.

---

## Car Parameters (evolved by the GA)
- Wheel radius (front and rear, independent)
- Chassis shape (polygon approximation, up to 8 vertices)
- Motor torque
- Spring stiffness (suspension)
- Weight distribution

---

## Player Upgrades (purchased, not evolved)

| Upgrade | Effect | Category |
|---------|--------|----------|
| Extended Fuel Tank | cars run longer before stopping | Endurance |
| Efficient Engine | more torque per unit weight | Performance |
| Higher Top Speed | raises velocity cap | Performance |
| Reinforced Frame | chassis vertices can't be negative | Stability |
| Shock Absorbers | reduces bounce penalty on rough terrain | Stability |
| Larger Gene Pool | +5 cars per generation | Evolution |
| Mutation Boost | temporarily higher mutation rate (3 gens) | Evolution |
| Elitism Lock | top 2 cars always survive unchanged | Evolution |

---

## Procedural Track Generation
- Tracks are 2D side-scrolling terrain curves (heightmap).
- Seed is stored so the same track can be replayed.
- Terrain hazards: steep ramps, gaps, downhill pitfalls.
- Checkpoints are placed at fixed horizontal intervals (e.g., every 50 m).
- A new track is generated after the current track's furthest checkpoint is reached by any car in any generation.

---

## GA Details
- **Selection**: Tournament selection (top 40% by fitness).
- **Crossover**: Uniform crossover on parameter vectors.
- **Mutation**: Gaussian noise on each parameter, rate configurable via upgrade.
- **Population size**: starts at 10, upgradeable.
- Generation time is real-time but accelerated (simulation speed × 4).

---

## Visuals
- Minimalist vector art: wireframe-style cars, gradient terrain.
- Each car has a distinct color derived from its genome hash.
- Live fitness graph displayed beside the track (distance vs. generation number).
- Coin counters and checkpoint markers always visible.

---

## State Machine
- `IdleGeneticAlgorithmState`: `Simulating`, `GenerationSummary`, `UpgradeMenuOpen`, `NewTrackIntro`
- Events: `GenerationCompleted`, `CheckpointReached`, `UpgradePurchased`, `NewTrackUnlocked`

---

## Data Model
```
data class CarGenome(val wheelRadii: Pair<Float, Float>, val chassisVertices: List<Offset>, val torque: Float, val springStiffness: Float)
data class GenerationResult(val generation: Int, val bestDistance: Float, val coinsEarned: Long)
data class GaUpgrade(val id: String, val name: String, val cost: Long, val purchased: Boolean)
```

---

## Out of Scope (v1)
- 3D physics
- Exporting evolved car designs
- Competitive leaderboards
