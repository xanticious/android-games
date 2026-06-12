# Shogi Lite — Design Document

## Overview
- Shogi Lite is a streamlined offline implementation of Japanese chess for one human versus one AI opponent.
- The board is 9×9 and follows standard Shogi movement, capture, promotion, and drop rules.
- Captured pieces switch allegiance and enter the captor's hand for possible later drops.
- Promotion becomes available when eligible pieces move into, within, or out of the promotion zone, defined as the farthest three ranks.
- Core pieces included are King, Rook, Bishop, Gold General, Silver General, Knight, Lance, and Pawn, along with their promoted forms where applicable.
- The design should make a traditionally complex ruleset feel approachable without removing authentic decision-making.
- Training support, move hints, and visible captured-piece hands are essential for readability.
- The game remains strictly local, single-player, and fully turn-based.

## Visual Style
- Use Material 3 with the underwater palette from `ui/theme/Color.kt`.
- Background depth uses `Dark0`; side trays, hand panels, and supporting chrome use `Dark1` and `Dark2`.
- Board cells use `Aqua0` as the neutral wood-replacement surface, giving enough contrast for either piece style.
- Valid move hints use `Aqua2`; active selection borders and path emphasis use `Aqua3`.
- Promotion-zone awareness uses a very soft `Aqua1` tint band across the last three ranks on each side.
- Check warning accents use `Aqua3` text and icon treatment rather than a loud overlay.
- Piece style is switchable between International iconography and Traditional kanji, but both sit on the same board and HUD language system.
- Piece art should feel substantial and tactile, with clear facing direction so ownership is always readable.
- Drop targets and illegal-drop reasons should be visually explicit to reduce learning friction.

## Screen Layout
