# Battleships — Design Document

## Overview
Battleships is a turn-based hidden-information duel focused on deduction, pressure, and clean feedback.
This version is designed for one player versus AI with a strong split-board presentation.
The player's own fleet is always visible on a compact board at the left.
The primary attack grid occupies the center-right and is the main interaction focus.
The tone is crisp naval strategy with a modern Material 3 presentation rather than military realism.
Every shot should feel decisive through readable markers, ship status updates, and turn pacing.
A special Keep Going mode extends finished games into a post-result analysis challenge.
That mode lets the player and AI continue firing to measure how many extra shots were needed after the official win or loss.
Official result stats and Keep Going stats remain separate.

## Visual Style
Overall surfaces use Dark0 and Dark1 for deep-ocean contrast.
Grid lines, panel dividers, and ship silhouettes use Dark2.
Selection, active targeting, and hover-like emphasis use Aqua3 and Aqua4.
Soft panel fills, counters, and helper labels use Aqua0 and Aqua1.
Hits use a hot red-accent Material 3 error color for clarity, ringed by Aqua0 so they remain visible in both themes.
Misses use a pale white-grey splash marker with subtle Aqua1 ripples.
Sunk ships gain a distinct crushed marker treatment: full ship path tinted dark with repeated hit pips and a small "SUNK" tag chip.
The player's fleet mini-board uses simplified ship blocks rather than decorative art.
The enemy board hides ships completely until sunk if reveal rules are enabled for end-state review only.
Buttons are chunky Material 3 pills with strong contrast and large touch targets.

## Screen Layout
