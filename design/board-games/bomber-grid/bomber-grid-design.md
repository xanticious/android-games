# Bombers — Design Document

## Overview
- Bombers is a turn-based artillery tactics game in the Board Games catalog, even though play happens on a 2D side-view battlefield instead of a grid.
- One human player faces one AI opponent.
- Each side controls exactly three characters.
- A round is made of alternating team turns.
- On a team's turn, only the currently selected character may act.
- Every turn has two mandatory phases: Move, then Aim & Fire.
- The battlefield is built from destructible dirt and grass tiles.
- Terrain is randomized at the start of every match, with varied ledges, plateaus, bridges, and gaps.
- Empty space below the lowest terrain is the abyss.
- Falling into the abyss immediately eliminates a character.
- Characters survive one bomb hit, becoming stunned and visibly singed.
- A second bomb hit eliminates that same character.
- The core fantasy is winning by direct hits, clever terrain destruction, or forcing the enemy to fall.

## Visual Style
- Material 3 framing uses the underwater palette from `ui/theme/Color.kt`.
- App chrome, cards, and controls lean on Aqua4, Aqua3, Dark1, and Dark2 for a bright but grounded interface.
- Battlefield sky uses a calm Aqua0-to-Aqua1 gradient treatment.
- Grass caps read as Aqua2-tinted green for consistency with the palette.
- Dirt body tiles use warm, desaturated browns derived from themed surfaces rather than raw hex values.
- Shadows and outlines use Dark0 and Dark1 to keep sprites readable over busy terrain.
- Characters are stick-figure-like silhouettes with oversized helmets, hats, or scarves.
- Team identity comes from accent colors on helmets and nameplates, not from realistic anatomy.
- Explosions are comic and puffy, with expanding rings, debris arcs, and a brief "BOOM" text burst.
- Destruction should feel dramatic but never obscure where safe ground still exists.
- Walk animation is tiny and toy-like, with quick leg cycles and a bobbing helmet.
- Stunned characters flash and wobble for a short beat after surviving the first explosion.
- Eliminated characters fall, shrink into the abyss, and disappear with a splashless void effect.

## Screen Layout
