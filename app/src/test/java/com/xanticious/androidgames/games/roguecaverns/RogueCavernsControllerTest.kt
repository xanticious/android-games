package com.xanticious.androidgames.games.roguecaverns

import com.xanticious.androidgames.controller.games.roguecaverns.CombatResolver
import com.xanticious.androidgames.controller.games.roguecaverns.LevelGenerator
import com.xanticious.androidgames.controller.games.roguecaverns.MetaProgression
import com.xanticious.androidgames.controller.games.roguecaverns.MonsterFactory
import com.xanticious.androidgames.controller.games.roguecaverns.XpCalculator
import com.xanticious.androidgames.model.games.roguecaverns.CombatAction
import com.xanticious.androidgames.model.games.roguecaverns.Element
import com.xanticious.androidgames.model.games.roguecaverns.Hero
import com.xanticious.androidgames.model.games.roguecaverns.HeroStats
import com.xanticious.androidgames.model.games.roguecaverns.MetaProfile
import com.xanticious.androidgames.model.games.roguecaverns.Monster
import com.xanticious.androidgames.model.games.roguecaverns.PermanentUpgrades
import com.xanticious.androidgames.model.games.roguecaverns.RunSummary
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RogueCavernsControllerTest {

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private val baseHero = Hero(
        stats = HeroStats(maxHp = 50, attack = 10, defense = 5, element = Element.NONE),
        hp = 50
    )
    private val baseMonster = Monster(
        id = 1, name = "Bat", maxHp = 20, hp = 20,
        attack = 5, defense = 2,
        element = Element.NONE, xpReward = 30, depth = 1
    )

    /**
     * A [Random] whose [nextFloat] always returns 0.0 (nextBits = 0).
     * Used for deterministic flee / variance tests.
     */
    private val zeroRandom = object : Random() {
        override fun nextBits(bitCount: Int) = 0
    }

    /**
     * A [Random] whose [nextFloat] always returns a value ≥ 0.5 (nextBits = max).
     * Guarantees that a flee attempt will fail.
     */
    private val maxRandom = object : Random() {
        override fun nextBits(bitCount: Int) = (1 shl bitCount) - 1
    }

    // ── CombatResolver ────────────────────────────────────────────────────────

    @Test
    fun resolveTurn_attack_dealsDamage() {
        val result = CombatResolver.resolveTurn(
            baseHero, baseMonster, CombatAction.ATTACK, CombatAction.ATTACK, zeroRandom
        )
        assertTrue("Monster should take damage", result.monsterHp < baseMonster.hp)
    }

    @Test
    fun resolveTurn_elementAdvantage_dealsBonus() {
        val heroFire = baseHero.copy(stats = baseHero.stats.copy(element = Element.FIRE))
        val monsterEarth = baseMonster.copy(element = Element.EARTH)

        val noAdvantage = CombatResolver.resolveTurn(
            baseHero, monsterEarth, CombatAction.ATTACK, CombatAction.ATTACK, Random(0)
        )
        val withAdvantage = CombatResolver.resolveTurn(
            heroFire, monsterEarth, CombatAction.ATTACK, CombatAction.ATTACK, Random(0)
        )
        assertTrue(
            "FIRE vs EARTH should deal more damage than NONE vs EARTH",
            withAdvantage.monsterHp < noAdvantage.monsterHp
        )
    }

    @Test
    fun resolveTurn_flee_50PercentChance() {
        // zeroRandom → nextFloat() = 0.0 < 0.5 → flee succeeds
        val success = CombatResolver.resolveTurn(
            baseHero, baseMonster, CombatAction.FLEE, CombatAction.ATTACK, zeroRandom
        )
        assertTrue("Flee should succeed when random returns 0.0", success.fled)

        // maxRandom → nextFloat() ≈ 1.0 ≥ 0.5 → flee fails
        val failure = CombatResolver.resolveTurn(
            baseHero, baseMonster, CombatAction.FLEE, CombatAction.ATTACK, maxRandom
        )
        assertFalse("Flee should fail when random returns ~1.0", failure.fled)
    }

    @Test
    fun resolveTurn_skill_dealsMoreDamage() {
        val attackResult = CombatResolver.resolveTurn(
            baseHero, baseMonster, CombatAction.ATTACK, CombatAction.ATTACK, Random(42)
        )
        val skillResult = CombatResolver.resolveTurn(
            baseHero, baseMonster, CombatAction.SKILL, CombatAction.ATTACK, Random(42)
        )
        assertTrue(
            "SKILL should leave monster with less HP than ATTACK",
            skillResult.monsterHp < attackResult.monsterHp
        )
    }

    // ── XpCalculator ─────────────────────────────────────────────────────────

    @Test
    fun xpCalculator_deeperRun_moreXp() {
        val shallow = RunSummary(depthReached = 1, kills = 0, banked = false, xpEarned = 0)
        val deep    = RunSummary(depthReached = 3, kills = 0, banked = false, xpEarned = 0)
        assertTrue(XpCalculator.award(deep, 0) > XpCalculator.award(shallow, 0))
    }

    @Test
    fun xpCalculator_bankedRun_bonusXp() {
        val regular = RunSummary(depthReached = 2, kills = 5, banked = false, xpEarned = 0)
        val banked  = RunSummary(depthReached = 2, kills = 5, banked = true,  xpEarned = 0)
        assertTrue(XpCalculator.award(banked, 0) > XpCalculator.award(regular, 0))
    }

    @Test
    fun xpCalculator_fortuneRank_increasesXp() {
        val summary = RunSummary(depthReached = 2, kills = 0, banked = false, xpEarned = 0)
        assertTrue(XpCalculator.award(summary, 2) > XpCalculator.award(summary, 0))
    }

    // ── MetaProgression ───────────────────────────────────────────────────────

    @Test
    fun upgradeCost_higherRank_higherCost() {
        assertTrue(MetaProgression.upgradeCost(1) > MetaProgression.upgradeCost(0))
        assertTrue(MetaProgression.upgradeCost(2) > MetaProgression.upgradeCost(1))
    }

    @Test
    fun startingHero_vitalityUpgrade_increasesMaxHp() {
        val hero = MetaProgression.startingHero(PermanentUpgrades(vitality = 1))
        assertEquals(70, hero.stats.maxHp) // 50 + 1×20 = 70
    }

    @Test
    fun startingHero_powerUpgrade_increasesAttack() {
        val hero = MetaProgression.startingHero(PermanentUpgrades(power = 1))
        assertEquals(15, hero.stats.attack) // 10 + 1×5 = 15
    }

    @Test
    fun applyUpgrade_validType_incrementsRank() {
        val profile = MetaProfile(totalXp = 1000L)
        val updated = MetaProgression.applyUpgrade(profile, "vitality")
        assertNotNull(updated)
        assertEquals(1, updated!!.upgrades.vitality)
    }

    @Test
    fun applyUpgrade_insufficientXp_returnsNull() {
        val profile = MetaProfile(totalXp = 0L) // rank-0 cost is 100
        val result = MetaProgression.applyUpgrade(profile, "vitality")
        assertNull(result)
    }

    // ── LevelGenerator ────────────────────────────────────────────────────────

    @Test
    fun generateLevel_sameSeed_sameLevel() {
        val level1 = LevelGenerator.generateLevel(1, 42L)
        val level2 = LevelGenerator.generateLevel(1, 42L)
        assertEquals(level1, level2)
    }

    @Test
    fun generateLevel_depth3_strongerMonsters() {
        // MonsterFactory should produce stronger templates at depth 3
        val d1Max = MonsterFactory.monstersForDepth(1).maxOf { it.attack }
        val d3Max = MonsterFactory.monstersForDepth(3).maxOf { it.attack }
        assertTrue("Depth-3 monsters should have higher attack than depth-1", d3Max > d1Max)

        // Verify level generator uses depth-3 monsters in the generated level
        val level3 = LevelGenerator.generateLevel(3, 42L)
        level3.rooms.mapNotNull { it.monster }.forEach { monster ->
            assertTrue("Monsters in a depth-3 level should have depth=3", monster.depth == 3)
        }
    }
}
