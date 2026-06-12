# Backgammon — Design Document

## Overview
Backgammon is a classic race-and-contact board game focused on tempo, risk, and efficient checker movement.
This version is tuned for single-player portrait play on Android with clear tap-driven interactions.
The tone is elegant, calm, and premium rather than casino-like.
Matches use standard rules with hitting, entering from the bar, bearing off, gammons, and backgammons.
The doubling cube is intentionally omitted to keep decision flow simple and stats easier to compare.
The player always faces an AI opponent.
Dice roll automatically at the start of every turn.
The main skill expression comes from reading the board, minimizing blots, building primes, and timing the race home.

## Visual Style
The board uses a polished tabletop look with Material 3 spacing and the underwater palette.
Primary background surfaces use Dark0 and Dark1.
Point separators, borders, and deep shadows use Dark2.
Active highlights, move indicators, and dice accents use Aqua3 and Aqua4.
Soft selection fills and legal-move glows use Aqua0 and Aqua1.
Checker colors are White and Black as rules-facing labels, but rendering uses light and dark discs with subtle Aqua reflections so they still fit the app palette.
Triangles are alternating dark and light wood-inspired surfaces expressed through Dark1, Dark2, Aqua0, and muted neutral tints derived from Material 3 surfaces.
The center bar is visually strong and slightly raised so portrait orientation remains readable.
Dice are large rounded Material 3 chips with bold pips.
Hit checkers on the bar stack vertically with a slight offset.
Borne-off checkers appear in neat trays at the top and bottom edges of the screen, never over the board.

## Screen Layout
