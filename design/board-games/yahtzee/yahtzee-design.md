# Yahtzee — Design Document

## Overview
- Yahtzee is a turn-based dice game in which players try to maximize score across 13 categories.
- The app supports solo score attacks against 0, 1, 2, or 3 AI opponents.
- Each turn allows up to three rolls of five dice.
- Between rolls, the player may lock or unlock any individual die.
- At the end of the turn, one score category must be filled, even if it scores zero.
- Standard upper-section categories are Ones, Twos, Threes, Fours, Fives, and Sixes.
- Lower-section categories are Three-of-a-Kind, Four-of-a-Kind, Full House, Small Straight, Large Straight, Chance, and Yahtzee.
- Upper bonus awards 35 points when the upper subtotal reaches at least 63.
- Additional Yahtzees after the Yahtzee box is already filled award 100-point bonuses and act as wilds for full house and straight interpretation.
- The digital version should feel tactile, readable, and fast, with the scorecard always in view.
- Strategy support comes from score previews, AI difficulty, and clear turn-state communication rather than forced guidance.
- All statistics and preferences remain local to the device.

## Visual Style
- Use Material 3 surfaces with an underwater-casino feel built from the app palette.
- Table background surfaces use `Dark0` and `Dark1` in dark mode, while light mode leans on `Aqua0` with elevated cards.
- Dice bodies use high-contrast surfaces with pips tinted from `Dark2` or `Aqua4` for clarity.
- Locked dice gain an `Aqua2` border and a subtle filled chip underneath.
- Roll and confirm emphasis buttons use `Aqua3`, while score previews use lower-emphasis typography tinted toward `Aqua1`.
- Completed score rows use standard text contrast; suggested high-value rows may receive a faint `Aqua2` background wash.
- The scorecard should resemble a clean ledger, not paper scraps or casino felt.
- Player columns must remain legible on both phones and tablets.
- Pointer hover states on large screens can elevate category rows, but touch interactions remain primary.
- Animations are light: dice bounce, lock snap, row highlight, and total count-up.
- Reduced-motion mode converts rolls to quick face swaps with no bounce.

## Screen Layout
