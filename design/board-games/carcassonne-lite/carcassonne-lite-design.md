# Carcassonne Lite — Design Document

## Overview
Carcassonne Lite is a streamlined tile-laying strategy game about spatial planning, tactical scoring, and long-term territory control.
The player competes against one to three AI opponents using a shared draw pile from the standard 72-tile base set.
Each turn centers on one clean sequence: draw, rotate, place, optionally deploy a meeple, then score any completed features.
The tone is cozy and thoughtful, with an elegant tabletop presentation optimized for touch and portrait-friendly play.
This Lite version keeps the core base-set feature families: cities, roads, monasteries, and fields.
Fields only score at the end of the game.
Meeples placed on features do not return mid-game in this ruleset, creating sharper scarcity and simpler bookkeeping.
The result is a more strategic long-arc experience with less rules overhead during active turns.

## Visual Style
The board uses a crisp tabletop tile look with soft Material 3 elevation and underwater-palette framing.
Empty board space uses Dark0 with a faint grid fade to help orientation during scrolling.
Placed tiles sit on Dark1 shadows with thin Dark2 outlines.
Active placement hints use Aqua3 borders and Aqua1 glows.
Invalid placement previews use a muted error treatment, never a usable-looking highlight.
The current drawn tile rests in a prominent tray using Aqua0 surface accents.
Meeples use player-distinct silhouettes with palette-safe tints, but all UI chrome still references Aqua and Dark tokens for consistency.
Score pills and turn chips use Aqua2, Aqua3, and Aqua4 for emphasis.
Completed feature flashes should be subtle and classy rather than noisy.
The board may become very large, so visual clutter must stay low as the map expands.

## Screen Layout
