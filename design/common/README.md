# Design — Common Components

This folder contains design specifications for UI components, interaction patterns, and systems shared across multiple games.

## Action-game components

| File | Description |
|------|-------------|
| [virtual-joystick.md](virtual-joystick.md) | Left-thumb analog joystick (movement knob) |
| [tap-targeting.md](tap-targeting.md) | Right-hand tap-to-aim / tap-to-move targeting system |
| [hud-elements.md](hud-elements.md) | Score display, lives, timers, health bars |
| [powerup-system.md](powerup-system.md) | Power-up spawn, collect, and effect framework |
| [victory-defeat.md](victory-defeat.md) | Level complete / game over presentation |
| [visual-style.md](visual-style.md) | High-definition art direction and color palette guidelines |

## Puzzle-game components

| File | Description |
|------|-------------|
| [puzzle-grid-board.md](puzzle-grid-board.md) | Shared grid/board rendering, sizing, and color tokens |
| [puzzle-controls.md](puzzle-controls.md) | Standard tap / hold / drag / swipe meanings, undo, hints |
| [puzzle-flow.md](puzzle-flow.md) | Settings → How to Play → Gameplay flow and victory/defeat placement |

## Rhythm-game components

| File | Description |
|------|-------------|
| [rhythm-note-highway.md](rhythm-note-highway.md) | Shared lane highway, timing windows, combo/multiplier, audio sync, and results |
| [morse-code.md](morse-code.md) | International Morse alphabet, dit/dah timing, single-button input, and beep audio |

## Strategy / tower-defense components

| File | Description |
|------|-------------|
| [tower-defense.md](tower-defense.md) | Shared lite tower-defense board, waves, economy, towers, and combat |
| [level-solvability.md](level-solvability.md) | Monte Carlo generate→simulate→accept guarantee for beatable levels |
