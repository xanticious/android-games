# Zilch — Design Document

## Overview
- Zilch is a fast scoring dice game also known as Ten Thousand and closely related to Farkle-style rulesets.
- One human player competes against one to four AI opponents.
- The goal is to finish with the highest score once someone reaches 10,000 points and the final round concludes.
- Matches should feel readable, snappy, and satisfying in short sessions.
- Every turn revolves around deciding whether to bank points or keep rolling for more.
- Unbanked turn points are always at risk.
- A roll with no scoring result is a zilch.
- Zilching immediately ends the turn and wipes all unbanked points from that turn.
- Players must set aside at least one valid scoring die or scoring combination after every successful roll.
- If all six dice score, the player earns hot dice and rolls all six again while keeping the running turn total.
- Standard win flow uses a final round after the first player crosses 10,000.
- Optional Keep Going Mode continues the match after a winner is declared to show long-tail pacing.
- Keep Going Mode does not change the official winner of the completed match.
- The design should support clear combination recognition without requiring players to memorize hidden scoring.
- The full experience stays local and offline, matching the app's no-social, no-store product scope.

## Visual Style
- The table is presented as a polished tabletop tray inside a Material 3 card layout.
- Background surfaces use Dark0 and Dark1 to create a calm, high-contrast play area.
- Primary accents use Aqua3 and Aqua4 on buttons, chips, and score highlights.
- Selected scoring dice glow with Aqua2 rings and soft elevation.
- Neutral dice use light surfaces derived from Aqua0 with Dark1 pips for strong readability.
- Warning states such as zilch risk and final-round urgency use theme error or tertiary treatment instead of custom hex colors.
- Score panels use stacked cards with underwater palette accents rather than casino styling.
- Dice animations are crisp and toy-like, with short bounces and subtle shadows.
- Locked dice slide upward into a set-aside lane instead of shrinking away.
- The current player row is emphasized with a strong container tint and an icon marker.
- Typography favors large score numbers and compact rule reminders.
- The overall tone is modern tabletop, not gritty gambling.
- All color references in implementation must come from `ui/theme/Color.kt` and Material 3 roles built from those tokens.

## Screen Layout
