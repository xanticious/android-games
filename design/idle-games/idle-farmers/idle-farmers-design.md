# Idle Farmers — Design Document

## Overview
A historical idle/incremental game spanning ~10,000 years of agricultural development. The player starts with one farmer and one crop in the Neolithic era and unlocks technologies in dependency order, simulating real agricultural history through to genetic engineering and robotic farming.

---

## Core Loop
1. Farmers work the land and produce crops each cycle.
2. Excess crops are sold for coins.
3. Spend coins on upgrades.
4. Upgrades unlock new upgrades (dependency tree), increasing output.

---

## Eras & Upgrade Tree (abridged)

### Era 1 — Neolithic (~10,000 BCE)
- **Subsistence Farming** *(starter)* — one farmer, one crop, barely enough to eat
- **Cleared Land** *(requires: Subsistence Farming)* — +1 plot, +10% yield
- **Simple Tools** *(requires: Cleared Land)* — stone hoe, +20% yield per farmer
- **Seed Selection** *(requires: Simple Tools)* — keep best seeds, +15% yield

### Era 2 — Ancient (~5,000 BCE)
- **Discover Barley** *(requires: Seed Selection)* — second crop type, unlocks Beer
- **Irrigation Ditch** *(requires: Cleared Land)* — drought immunity, +30% yield
- **Domesticated Oxen** *(requires: Simple Tools)* — ox-drawn plow, doubles field size
- **Train Another Farmer** *(requires: Cleared Land)* — +1 farmer slot (repeatable)

### Era 3 — Classical (~500 BCE)
- **Crop Rotation** *(requires: Discover Barley + Irrigation Ditch)* — eliminates soil exhaustion debuff
- **Bronze Plowshare** *(requires: Domesticated Oxen)* — +40% tilling speed
- **Grain Storage** *(requires: Crop Rotation)* — buffer against bad seasons

### Era 4 — Medieval (~1000 CE)
- **Three-Field System** *(requires: Crop Rotation)* — 33% more active land
- **Windmill** *(requires: Grain Storage)* — passive processing bonus
- **Selective Breeding** *(requires: Seed Selection + Three-Field System)* — slow but compounding yield increase

### Era 5 — Industrial (~1850 CE)
- **Steam Tractor** *(requires: Bronze Plowshare + Windmill)* — replaces one farmer slot with automation
- **Chemical Fertiliser** *(requires: Selective Breeding)* — +80% yield, introduces pollution risk
- **Rail Distribution** *(requires: Grain Storage + Steam Tractor)* — sell surplus at 2× price

### Era 6 — Modern (~1960 CE)
- **Pesticides** *(requires: Chemical Fertiliser)* — eliminates pest debuff
- **Combine Harvester** *(requires: Steam Tractor)* — harvests 10× faster
- **Refrigeration** *(requires: Rail Distribution)* — allows perishable crops

### Era 7 — Biotech (~2000 CE)
- **Genetic Engineering** *(requires: Selective Breeding + Pesticides)* — create custom high-yield crops
- **Drone Fleet** *(requires: Combine Harvester)* — fully automated planting
- **Vertical Farm** *(requires: Refrigeration + Genetic Engineering)* — stacked plots, no land limit

### Era 8 — Future (~2100+)
- **Robotic Farmers** *(requires: Drone Fleet + Vertical Farm)* — replace all human farmer slots
- **Synthetic Soil** *(requires: Genetic Engineering + Chemical Fertiliser)* — produce anywhere
- **Orbital Greenhouse** *(requires: Robotic Farmers + Synthetic Soil)* — endgame prestige unlock

---

## Upgrade Data Model
```
data class FarmUpgrade(
    val id: String,
    val name: String,
    val era: Era,
    val cost: Long,
    val requires: List<String>,   // IDs of prerequisite upgrades
    val effect: UpgradeEffect,
    val purchased: Boolean
)
```

---

## Random Events
Occasionally a seasonal event modifies output for one cycle:
- **Drought** — −50% yield (mitigated by Irrigation)
- **Pest Swarm** — −30% yield (mitigated by Pesticides)
- **Bumper Harvest** — +100% yield (random bonus)
- **Festival** — coin bonus, no production change

---

## Visuals
- Top-down pixelated farm view that evolves visually per era (dirt paths → paved roads, wooden tools → machinery).
- Farmer sprites animate between field rows.
- Era transitions include a brief flavour text card.

---

## State Machine
- `IdleFarmersState`: `Playing`, `UpgradeMenuOpen`, `EventActive`
- Events: `HarvestCycle`, `UpgradePurchased`, `RandomEventTriggered`, `EventResolved`

---

## Out of Scope (v1)
- Multiplayer trading
- Weather mini-game
- Prestige beyond Orbital Greenhouse
