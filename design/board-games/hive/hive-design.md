# Hive — Design Document

## Overview
Hive is a two-player abstract strategy game played without a fixed board.
Players place insect tiles next to one another to build a single connected structure called the hive.
The goal is to surround the opposing Queen Bee on all six sides before the AI surrounds yours.
Each player has 11 pieces: Queen Bee ×1, Beetles ×2, Grasshoppers ×3, Spiders ×2, and Ants ×3.
A player's Queen Bee must be placed by that player's fourth turn.
After placement, pieces may move according to their insect rules as long as the One Hive rule keeps the structure connected.
This version is human versus AI and fully offline.

## Visual Style
Use Material 3 surfaces with a tactile tabletop feel and the underwater palette.
Background surfaces use `Dark0` and `Dark1`, field hints and hex outlines use `Aqua3`, and selection plus legal destinations use `Aqua2`.
Text and counters use `Aqua0`, while stronger callouts such as turn emphasis or zoom controls use `Aqua4`.
Player pieces remain black and white for rule clarity, but highlights, badges, and motion cues come from the shared token palette.
Stacked beetles should show clear elevation and count badges without clutter.
Victory and defeat messages must stay below the play area, never over the hive.

## Screen Layout
