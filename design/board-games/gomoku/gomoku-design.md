# Gomoku — Design Document

## Overview
Gomoku is a territory-free abstract strategy game where players place stones on intersections to make five in a row.
The default board is 15×15, with a larger 19×19 option for longer, more spacious matches.
This version supports both Standard rules and an optional Renju ruleset that constrains Black to reduce first-player advantage.
The design prioritizes clean intersection targeting, visible last-move feedback, and easy understanding of forbidden moves under Renju.
All play is offline versus local AI, with local-only stats and a straightforward state-machine driven flow.
The board should feel calm and contemplative, with readable stones and minimal visual noise.
Result messaging, warnings, and rematch actions stay below the board so the completed position is always visible.

## Visual Style
- Material 3 interface framed by `Dark0`, `Dark1`, `Aqua0`, and `Aqua1` from `ui/theme/Color.kt`.
- The wooden-board fantasy is translated into the app palette through layered dark surfaces and aqua edge lighting rather than literal wood tones.
- Grid lines use `Aqua1` on a muted `Dark1` board plate for sharp intersection readability.
- Black stones use `Dark0` with `Aqua2` rim highlights; white stones use `Aqua0` with `Dark2` shadowing.
- Star points, if shown, use small `Aqua2` dots that never compete with the stones.
- The last placed stone gets a thin `Aqua3` ring marker.
- Forbidden intersections in Renju can show a subtle crossed indicator using `Aqua2` or a dimmed `Dark2` badge.
- Winning five stones glow and pulse with `Aqua3` while the rest of the board remains static.
- Information cards and buttons below the board keep the same underwater Material 3 identity as the rest of the app.

## Screen Layout
