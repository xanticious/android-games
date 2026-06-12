# Splendor — Design Document

## Overview
- Splendor is a fast engine-building board game adapted for offline single-player Android play.
- The app supports one human player against 1, 2, or 3 AI rivals.
- Standard rules are preserved: collect gem tokens, reserve cards, buy development cards, and attract nobles.
- The six token colors are white, blue, green, red, black, and gold as the wild reserve reward.
- The win threshold is 15 prestige points, with end-of-round resolution so all players in the round finish equally.
- The digital version emphasizes readability over table realism.
- Information density is high, so the layout must keep all major card markets visible without hiding the player's reserve.
- The design should feel premium, jewel-like, and calm rather than flashy.
- Material 3 surfaces, rounded cards, and the underwater palette establish the app-wide visual identity.
- All profile data, AI history, win rates, and settings remain local to the device.
- The human turn is always explicit and confirm-driven to prevent accidental gem collection or purchases.
- AI turns should resolve quickly with compact animations and visible card movement.
- Noble visits are automatic and never require a separate confirm step.
- All actionable game information must be accessible in portrait orientation first, with tablet layouts widening rather than reordering the logic.

## Visual Style
- Use Material 3 components with the underwater palette from `ui/theme/Color.kt`.
- Primary background surfaces use `Dark0` and `Dark1` in dark theme, and `Aqua0` for large light-theme surfaces.
- Interactive highlights use `Aqua2` for valid actions and `Aqua3` for confirmed selections.
- Key score and prestige accents use `Aqua4` so point-bearing elements stand out.
- Card frames use elevated Material cards with soft tonal contrast instead of faux wood or velvet textures.
- Gem token chips should be color-coded with labeled icons so color-blind players can read both hue and symbol.
- Reserved cards use a subtle banner treatment with `Aqua1` accents so they are distinct from open market cards.
- Noble tiles use a brighter elevated style and a prestige emblem to communicate rarity.
- Disabled actions dim with lower-emphasis typography rather than disappearing.
- The bank area should look orderly, using evenly spaced circular token wells.
- Motion is restrained: tap pulses, card lifts, token slides, and short score count-ups.
- All animations respect reduced-motion settings by switching to instant transitions.

## Screen Layout
