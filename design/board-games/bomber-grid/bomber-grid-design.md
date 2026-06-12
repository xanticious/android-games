# Bombers — Design Document

## Overview
- Bombers is a turn-based artillery tactics game in the Board Games catalog, even though play happens on a 2D side-view battlefield instead of a grid.
- One human player faces one AI opponent.
- Each side controls exactly three characters.
- A round is made of alternating team turns.
- On a team's turn, only the currently selected character may act.
- Every turn has two mandatory phases: Move, then Aim & Fire.
- The battlefield is built from destructible dirt and grass tiles.
- Terrain is randomized at the start of every match, with varied ledges, plateaus, bridges, and gaps.
- Empty space below the lowest terrain is the abyss.
- Falling into the abyss immediately eliminates a character.
- Characters survive one bomb hit, becoming stunned and visibly singed.
- A second bomb hit eliminates that same character.
- The core fantasy is winning by direct hits, clever terrain destruction, or forcing the enemy to fall.

## Visual Style
- Material 3 framing uses the underwater palette from `ui/theme/Color.kt`.
- App chrome, cards, and controls lean on Aqua4, Aqua3, Dark1, and Dark2 for a bright but grounded interface.
- Battlefield sky uses a calm Aqua0-to-Aqua1 gradient treatment.
- Grass caps read as Aqua2-tinted green for consistency with the palette.
- Dirt body tiles use warm, desaturated browns derived from themed surfaces rather than raw hex values.
- Shadows and outlines use Dark0 and Dark1 to keep sprites readable over busy terrain.
- Characters are stick-figure-like silhouettes with oversized helmets, hats, or scarves.
- Team identity comes from accent colors on helmets and nameplates, not from realistic anatomy.
- Explosions are comic and puffy, with expanding rings, debris arcs, and a brief "BOOM" text burst.
- Destruction should feel dramatic but never obscure where safe ground still exists.
- Walk animation is tiny and toy-like, with quick leg cycles and a bobbing helmet.
- Stunned characters flash and wobble for a short beat after surviving the first explosion.
- Eliminated characters fall, shrink into the abyss, and disappear with a splashless void effect.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Bombers     Round 4   Wind ← 2 [⚙]  │  ← Top bar
├─────────────────────────────────────┤
│ AI:  Scout ✓  Ace ⚠  Tank ✕         │
│                                     │
│        side-view destructible field │
│   ☁        o                       │
│        ___/‾‾\____        __       │  ← Battlefield anchor
│  You A  B  C     gap     AI X Y Z  │
│ ________/‾‾‾\__________/‾‾\___     │
├─────────────────────────────────────┤
│ Active: You B   Phase: Move         │  ← HUD strip
│ [←] [→] [Jump] [Confirm move]       │
│ Aim: 42° Power: 68% [Fire]          │
└─────────────────────────────────────┘
```
- Portrait-first: the battlefield is a wide side-view canvas that pans horizontally while controls remain fixed below it.
- Tablet layouts show more of the terrain at once and move team rosters into side panels without covering the battlefield.

## Settings
- **Opponent difficulty**: Easy, Medium, Hard (default Medium).
- **Terrain complexity**: Gentle, Standard (default), or Chaotic; affects ledges, bridges, gaps, and destructible dirt density.
- **Wind**: Off, Light (default), or Variable; wind affects projectile arc equally for both sides.
- **Turn timer**: Off (default), 30 seconds, or 60 seconds.
- **Aiming assist** (on/off, default on): shows a short predicted arc segment, never the full landing point.
- Settings are selected on the per-game Settings screen before terrain generation.

## How to Play
- Each side has exactly three characters.
- Teams alternate turns, rotating to the next surviving character on that team.
- Every turn has two mandatory phases: **Move**, then **Aim & Fire**.
- During Move, the active character can walk and jump within a limited range. Falling into the abyss immediately eliminates a character.
- During Aim & Fire, choose angle and power, then launch one bomb. Bombs damage characters and destroy nearby terrain.
- A character survives the first direct bomb hit by becoming stunned and singed. A second bomb hit eliminates that character.
- Win by eliminating all three AI characters through hits, terrain collapse, or forcing falls into the abyss.

## Controls
- Tap a character portrait to inspect status; turn order still controls who may act.
- Use left/right buttons or drag on the battlefield to move the active character during the Move phase.
- Tap **Jump** for a short hop when grounded, then **Confirm move** to enter Aim & Fire.
- Drag the aim handle or use angle/power sliders; the projectile preview uses Aqua1/Aqua3 accents.
- Tap **Fire** once to launch. Controls lock until the bomb, terrain, falling, and elimination resolution finishes.

## AI Opponents
- **Easy**: moves toward safe nearby ground and fires with rough angle guesses; rarely uses terrain destruction intentionally.
- **Medium**: seeks cover, avoids obvious abyss risk, estimates projectile arcs, and targets stunned characters when practical.
- **Hard**: plans movement plus shot together, uses wind, chooses terrain-collapse shots, pressures bridges, and protects its wounded characters.
- AI difficulty changes decision quality only; team size, Move then Aim & Fire phases, stun rules, terrain destruction, and abyss elimination never change.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty | yes |
| Characters remaining at win | yes |
| Direct-hit eliminations | yes |
| Terrain-collapse eliminations | yes |
| Abyss fall eliminations caused | yes |
| Matches won with a stunned survivor | yes |

## State Machine
- A dedicated `BombersStateMachine` in `state/` exposes `StateFlow<BombersState>`.
```
Idle
 └─ MatchStarted → GeneratingTerrain
GeneratingTerrain
 └─ TerrainReady → SelectingActor
SelectingActor
 └─ ActorSelected → MovePhase
MovePhase
 ├─ MovementChanged → MovePhase
 └─ MoveConfirmed → AimFirePhase
AimFirePhase
 ├─ AimChanged → AimFirePhase
 └─ BombFired → ResolvingExplosion
ResolvingExplosion
 ├─ TerrainSettled → ResolvingFalls
 └─ CharacterHit → ResolvingFalls
ResolvingFalls
 ├─ TeamEliminated → GameOver
 └─ TurnAdvanced → SelectingActor
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `BombersRules` controller in `controller/` has no Android imports and is unit-tested for movement limits, projectile results, blast damage, stun-to-elimination progression, destructible terrain, falls, abyss elimination, and win detection.

## HUD
- Active team, active character, phase label, round number, wind, and remaining move budget.
- Team roster with healthy, stunned, and eliminated status icons.
- Aim angle, power, predicted partial arc when enabled, and one clear **Fire** action.
- Victory/defeat messaging follows `design/common/victory-defeat.md` and appears below the battlefield, never over destructible terrain or characters.
