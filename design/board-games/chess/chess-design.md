# Chess — Design Document

## Overview
Chess is a full-featured offline board game adaptation for phone and tablet play against local AI. It supports standard chess and Chess960, preserves all classical rules, and presents information clearly without clutter.
The experience emphasizes readable move feedback, deliberate touch input, and strong post-move explanation hooks for future training features.
All matches are single-player, all stats stay on-device, and all navigation remains inside the app's simple state-machine flow.
The board is always the visual anchor, while move history, promotion choice, and result messaging stay outside the playable grid.
Core rules include castling, en passant, promotion, check, checkmate, stalemate, insufficient material, threefold repetition, and the 50-move rule.
Training Wheels mode can optionally restrict the player's choices to strong candidate moves without changing the underlying game rules.

## Visual Style
- Material 3 presentation using the underwater palette from `ui/theme/Color.kt`.
- Background surfaces use `Dark0` and `Dark1`; board frame accents use `Aqua4`.
- Light squares use a tinted `Aqua0` treatment; dark squares use a muted `Dark2` treatment.
- White pieces use bright `Aqua0` fills with `Dark1` line detail for readability.
- Black pieces use `Dark0` / `Dark1` bodies with `Aqua1` edge highlights so they remain legible in dark theme.
- Valid move indicators use small `Aqua2` dots for quiet moves and `Aqua3` rings for captures.
- Selected piece glow uses `Aqua3`; the last move trail uses a subtle `Aqua1` wash when enabled.
- Check warning highlights the threatened king square with a restrained `Aqua2` pulse plus a stronger border treatment.
- Captured pieces, clocks, and buttons sit on cards with rounded Material 3 surfaces, never on top of the board.
- Animations are short and intentional: piece slides, capture fade-outs, check pulse, and end-state panel motion.

## Screen Layout
