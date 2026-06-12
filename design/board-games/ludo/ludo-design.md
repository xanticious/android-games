# Ludo — Design Document

## Overview
- Ludo is a classic race board game for one human player against one, two, or three AI opponents.
- The board uses the familiar four-corner layout with four home yards, a shared clockwise outer track, and a colored home column for each side.
- Each side owns four tokens and attempts to move all four from home, around the board, and into the final home stretch.
- A roll of 6 allows a token to enter play from home if its start square is available.
- A roll of 6 also grants a bonus roll after resolving the selected move.
- Tokens must move the exact remaining count to bear off into the final home slot.
- Landing on an opposing token sends that token back to its home yard.
- If Safe Squares is enabled, designated star spaces protect tokens from capture.
- If Safe Squares is disabled, any shared outer-track square can become a capture square.
- The game is purely offline, single-device, and turn-based with fast readability.
- The experience should feel breezy and readable for casual players while still preserving the tension of blocking, racing, and timing.
- The app ships Ludo as a fully guided digital board game with clear move hints, deterministic turn order, and no hidden rules.

## Visual Style
- Use Material 3 surfaces with the underwater palette from `ui/theme/Color.kt`.
- Main background uses `Dark0` so the board reads as a bright object on a calm deep-water backdrop.
- Board frame and outer chrome use `Dark1` and `Dark2` to separate the play field from HUD regions.
- Primary interactive emphasis uses `Aqua3`; secondary highlights use `Aqua2`; soft selection halos use `Aqua1`.
- Neutral light board surfaces use `Aqua0` to keep tracks legible in both light and dark system themes.
- Because the game requires four player identities, each token set keeps its classic red, green, blue, or yellow identity through piece art and labels, while selection, HUD, and feedback still use theme tokens.
- Home yards are large rounded quadrants with strong shape boundaries, not flat saturated blocks.
- Safe squares use a star badge and an `Aqua2` ring so their rule meaning is obvious even if token colors vary.
- Move indicators are circular pips placed on candidate destination cells using `Aqua3` with a subtle `Aqua0` outline.
- The die appears as a chunky Material card with a raised face and bold pips.
- Motion is snappy and short; token travel animates cell by cell with a clean easing curve so captures and home entry are easy to follow.

## Screen Layout
