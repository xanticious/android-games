# Splendor Puzzles — Design Document

## Overview
Splendor Puzzles is a 10-round score mode made from complete Splendor board states.
Each puzzle shows the gem bank, three tiers of market cards, noble tiles, four opponent summaries, and the player's own engine, gems, reserved cards, and points.
The player is asked one question: "What is your best move?"
A single legal action is selected using full-game controls and then compared against engine-ranked alternatives.
Scoring bands are fixed: optimal move 100 points, near-optimal within the top 10% of value 75, good within the top 25% 50, below average 25, clearly suboptimal 0.
Speed adds +20 under 15 seconds and +10 under 30 seconds.
Best run score is stored locally on device.

## Visual Style
Use Material 3 containers with a polished jewel-table presentation and the underwater palette.
Overall background uses `Dark0`; market shelves, player trays, and summary panels use `Dark1` and `Dark2`.
Selection rings, action highlights, and confirm states use `Aqua2` and `Aqua3`; progress and best-run callouts use `Aqua4`.
Text uses `Aqua0` for contrast.
Actual gem identities remain true to Splendor, but all surrounding chrome, borders, and emphasis cues come from the shared token palette.
Result animations appear only in the lower tray or side summary panel.
No result element may cover cards, nobles, gems, or player summaries.

## Screen Layout
