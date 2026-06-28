package com.xanticious.androidgames.games.anomalydefense

import com.xanticious.androidgames.controller.games.anomalydefense.AssaultSimulator
import com.xanticious.androidgames.controller.games.anomalydefense.BudgetSetter
import com.xanticious.androidgames.controller.games.anomalydefense.LevelGenerator
import com.xanticious.androidgames.controller.games.anomalydefense.SolvabilityVerifier
import com.xanticious.androidgames.controller.games.anomalydefense.assignUnit
import com.xanticious.androidgames.controller.games.anomalydefense.buyUnit
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.anomalydefense.AnomalyDefenseState
import com.xanticious.androidgames.model.games.anomalydefense.AssaultResult
import com.xanticious.androidgames.model.games.anomalydefense.Assignment
import com.xanticious.androidgames.model.games.anomalydefense.AttackPlan
import com.xanticious.androidgames.model.games.anomalydefense.AttackUnit
import com.xanticious.androidgames.model.games.anomalydefense.Defense
import com.xanticious.androidgames.model.games.anomalydefense.Level
import com.xanticious.androidgames.model.games.anomalydefense.Route
import com.xanticious.androidgames.model.games.anomalydefense.Turret
import com.xanticious.androidgames.model.games.anomalydefense.UnitCost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnomalyDefenseControllerTest {

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private val unitCosts = listOf(
        UnitCost(AttackUnit.RUNNER,   20),
        UnitCost(AttackUnit.TANK,     60),
        UnitCost(AttackUnit.SHIELDED, 40),
        UnitCost(AttackUnit.SABOTEUR, 45)
    )

    /** A straight 10-tile route with no obstacles. */
    private val openRoute = Route(id = 0, tiles = List(10) { x -> GridPos(x, 4) })

    /** Defense with a single open route and no turrets. */
    private val openDefense = Defense(
        routes = listOf(openRoute),
        turrets = emptyList(),
        objective = GridPos(9, 4),
        quota = 1
    )

    /** Defense with a single covering turret that fires fast and does heavy damage. */
    private val heavilyGuardedDefense = Defense(
        routes = listOf(openRoute),
        turrets = listOf(
            Turret(id = 0, pos = GridPos(4, 3), range = 4.0f, damage = 50, fireRate = 5.0f),
            Turret(id = 1, pos = GridPos(5, 5), range = 4.0f, damage = 50, fireRate = 5.0f),
            Turret(id = 2, pos = GridPos(6, 3), range = 4.0f, damage = 50, fireRate = 5.0f)
        ),
        objective = GridPos(9, 4),
        quota = 1
    )

    private fun planWith(vararg units: AttackUnit, routeId: Int = 0) = AttackPlan(
        listOf(Assignment(routeId, units.toList()))
    )

    private fun makeState(budget: Int = 1000): AnomalyDefenseState {
        val level = Level(openDefense, budget, 42L, unitCosts)
        return AnomalyDefenseState(
            level = level,
            purchasedUnits = emptyList(),
            plan = AttackPlan(emptyList()),
            budgetRemaining = budget,
            assaultInProgress = false,
            unitsInTransit = emptyList(),
            unitsThrough = 0,
            result = null
        )
    }

    // ── simulate tests ────────────────────────────────────────────────────────

    @Test
    fun simulate_runnerOnOpenRoute_reachesObjective() {
        val result = AssaultSimulator.simulate(
            defense = openDefense,
            plan = planWith(AttackUnit.RUNNER),
            unitCosts = unitCosts,
            seed = 1L
        )
        assertTrue("Runner should reach objective on open route", result.won)
        assertEquals(1, result.unitsThrough)
    }

    @Test
    fun simulate_tankAbsorbsMoreDamage_thanRunner() {
        // One turret that would kill a Runner but a Tank survives
        val oneTurretDefense = Defense(
            routes = listOf(openRoute),
            turrets = listOf(
                Turret(id = 0, pos = GridPos(5, 3), range = 2.0f, damage = 15, fireRate = 2.0f)
            ),
            objective = GridPos(9, 4),
            quota = 1
        )
        val runnerResult = AssaultSimulator.simulate(oneTurretDefense, planWith(AttackUnit.RUNNER), unitCosts, 1L)
        val tankResult   = AssaultSimulator.simulate(oneTurretDefense, planWith(AttackUnit.TANK),   unitCosts, 1L)

        // Tank (120 hp) survives more shots than Runner (30 hp); at minimum the tank should fare >= runner
        assertTrue("Tank should reach objective when runner may not", tankResult.won)
        assertTrue("Tank through count >= runner through count",
            tankResult.unitsThrough >= runnerResult.unitsThrough)
    }

    @Test
    fun simulate_shieldedUnit_absorbsInitialHits() {
        // A turret that does exactly 1 damage per shot – shielded unit should absorb 3 hits from shield
        val lowDamageTurretDefense = Defense(
            routes = listOf(openRoute),
            turrets = listOf(
                Turret(id = 0, pos = GridPos(5, 3), range = 2.0f, damage = 1, fireRate = 10.0f)
            ),
            objective = GridPos(9, 4),
            quota = 1
        )
        val result = AssaultSimulator.simulate(
            defense = lowDamageTurretDefense,
            plan = planWith(AttackUnit.SHIELDED),
            unitCosts = unitCosts,
            seed = 2L
        )
        // Shielded has 60 HP + 3 shield hits; even with many shots it should survive low damage
        assertTrue("Shielded unit should reach objective against low-damage turret", result.won)
    }

    @Test
    fun simulate_saboteur_disablesTurret() {
        // A turret that would kill a Runner but a Saboteur disables it before it can fire enough
        val turretDefense = Defense(
            routes = listOf(openRoute),
            turrets = listOf(
                Turret(id = 0, pos = GridPos(3, 3), range = 2.0f, damage = 40, fireRate = 2.0f)
            ),
            objective = GridPos(9, 4),
            quota = 1
        )
        // Saboteur (speed=2, hp=40) should disable the turret and pass through
        val result = AssaultSimulator.simulate(
            defense = turretDefense,
            plan = planWith(AttackUnit.SABOTEUR),
            unitCosts = unitCosts,
            seed = 3L
        )
        assertTrue("Saboteur should disable turret and reach objective", result.won)
    }

    @Test
    fun simulate_forceWipedBeforeObjective_notWon() {
        val result = AssaultSimulator.simulate(
            defense = heavilyGuardedDefense,
            plan = planWith(AttackUnit.RUNNER),
            unitCosts = unitCosts,
            seed = 4L
        )
        assertFalse("Runner should be destroyed before reaching heavily guarded objective", result.won)
    }

    // ── buyUnit tests ─────────────────────────────────────────────────────────

    @Test
    fun buyUnit_insufficientBudget_returnsNull() {
        val state = makeState(budget = 10) // RUNNER costs 20
        val result = buyUnit(state, AttackUnit.RUNNER)
        assertNull("buyUnit should return null when budget is insufficient", result)
    }

    @Test
    fun buyUnit_sufficientBudget_deductsCost() {
        val state = makeState(budget = 100)
        val result = buyUnit(state, AttackUnit.RUNNER)
        assertNotNull(result)
        assertEquals(80, result!!.budgetRemaining)
        assertEquals(1, result.purchasedUnits.size)
        assertEquals(AttackUnit.RUNNER, result.purchasedUnits.first())
    }

    // ── setBudget tests ───────────────────────────────────────────────────────

    @Test
    fun setBudget_easyDifficulty_generousMultiplier() {
        val budget = BudgetSetter.setBudget(100, GameDifficulty.EASY)
        assertEquals(200, budget)
    }

    @Test
    fun setBudget_hardDifficulty_tightMultiplier() {
        val budget = BudgetSetter.setBudget(100, GameDifficulty.HARD)
        assertEquals(110, budget)
    }

    @Test
    fun setBudget_hardBudget_lessThanEasyBudget() {
        val easy = BudgetSetter.setBudget(100, GameDifficulty.EASY)
        val hard = BudgetSetter.setBudget(100, GameDifficulty.HARD)
        assertTrue("Hard budget should be less than easy budget", hard < easy)
    }

    // ── Determinism test ──────────────────────────────────────────────────────

    @Test
    fun simulate_sameSeedSamePlan_deterministicResult() {
        val plan = planWith(AttackUnit.RUNNER, AttackUnit.TANK)
        val result1 = AssaultSimulator.simulate(openDefense, plan, unitCosts, seed = 99L)
        val result2 = AssaultSimulator.simulate(openDefense, plan, unitCosts, seed = 99L)
        assertEquals("Simulation should be deterministic", result1, result2)
    }

    // ── Solvability verifier ──────────────────────────────────────────────────

    @Test
    fun verify_beatable_withGenerousBudget() {
        val report = SolvabilityVerifier.verify(
            defense = openDefense,
            unitCosts = unitCosts,
            budget = 200,
            seed = 42L,
            iterations = 20
        )
        assertTrue("Open defense should be beatable", report.beatable)
        assertTrue("Reference cost should be positive", report.referenceCost > 0)
        assertTrue("Reference cost should not exceed budget", report.referenceCost <= 200)
    }
}
