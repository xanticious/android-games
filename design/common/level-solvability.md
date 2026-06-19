# Level Solvability (Monte Carlo)

Shared technique for **guaranteeing that a procedurally generated level is beatable** before it is ever shown to the player. Used by games that promise a fair, always-winnable level (Anomaly Defense, Empire Skirmish). Lite games that intentionally allow unwinnable maps (Base Defense, Randomized Dice TD) do **not** use this.

## Problem

Random generation is cheap but can produce impossible levels (e.g. a defense too dense to break through, or a tactical map with no winning sequence of moves). We want random variety **and** a guarantee the player can win.

## Approach: Generate → Simulate → Accept/Reject

1. **Generate a candidate** level from a seed (paths, defenders, budget, enemy placement, terrain — game-specific).
2. **Simulate many AI playthroughs** of that candidate using automated solver policies (random and/or heuristic). This is the **Monte Carlo sampling** step.
3. **Measure** how often the candidate is won and the *quality* of the best solution found (e.g. cheapest winning composition, fewest losses, fastest clear).
4. **Accept or reject**:
   - Reject if **no** simulated playthrough wins within the sample budget → regenerate with a new seed.
   - Accept if at least one (ideally several) playthroughs win → the level is provably beatable, and the best winning run becomes the **reference solution**.

## The Reference Solution

The best winning playthrough found during simulation is retained. It is used to:
- **Confirm beatability** (existence of a winning line).
- **Set difficulty parameters** that are relative to the optimum — e.g. a budget set as *reference cost × a difficulty multiplier* (see *Difficulty* below).
- Optionally power a **hint** or post-loss "a solution existed" reassurance.

The reference solution is **never shown** as the only path; it merely proves one exists.

## Difficulty via Margin to Optimum

Difficulty is expressed as the **leeway** the player gets relative to the reference solution rather than as new mechanics:

| Difficulty | Leeway over reference |
|------------|-----------------------|
| Easy   | Generous margin (e.g. budget well above the cheapest winning solution) |
| Normal | Moderate margin |
| Hard   | Slim margin (e.g. budget only a little above the best solution found) |

The exact knob is game-specific (attacker budget, number of reinforcements, turn limit, etc.), but the pattern is always *acceptable margin = f(reference, difficulty)*.

## Simulation Policies

- **Random rollouts** give an unbiased lower bound on winnability and good coverage of the possibility space.
- **Heuristic/greedy rollouts** find stronger solutions faster, tightening the estimate of the optimum (important for Hard).
- Sample count is a tunable budget: more samples = higher confidence and a tighter optimum estimate, at more generation-time cost. Generation runs off the UI thread.

## Determinism & Testing

- Generation and simulation are **pure controllers** (`controller/`) seeded by a `Long`; the same seed yields the same level and the same accept/reject verdict — no Android imports, fully unit-testable.
- Key tests: *an accepted level has at least one winning rollout*; *a rejected level is regenerated*; *Hard leeway ≤ Normal leeway ≤ Easy leeway for the same reference*; *same seed ⇒ identical level*.

## Data Model (shared shape)

```
data class SolveResult(
    val won: Boolean,
    val cost: Int,          // resource/budget cost of this playthrough's solution
    val score: Int          // game-specific quality (losses, turns, time, ...)
)
data class SolvabilityReport(
    val beatable: Boolean,          // any rollout won
    val referenceCost: Int,         // cheapest winning cost found
    val samples: Int
)
// Controller: solve(level, samples, seed) -> SolvabilityReport
// Generator loops generate→solve until beatable, then sets difficulty margin from referenceCost.
```
