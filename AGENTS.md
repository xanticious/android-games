# AGENTS Instructions

## Product Scope

Build a personal, offline-only Android collection of single-player games and games vs AI with multiple difficulties.

## Non-Negotiables

- Kotlin only
- Android only
- Use KStateMachine for app/game state transitions
- Keep navigation simple and modal-free
- Victory/defeat UI must never cover game boards
- No ads, no store, no social, no achievements
- All profile/stat data must remain local to device

## Architecture Expectations

- Keep code readable and maintainable
- Separate concerns clearly (model/controller/view/state)
- Add focused unit tests for behavior changes
- Prefer small, surgical changes and avoid unrelated refactors

## UX Expectations

- Flat modern UI
- Responsive on phones and tablets
- Light and dark themes
- Use Open Color palette values for core app theme
- Keep transitions instantaneous; reserve full-screen animations for victory/defeat only

## Content Expectations

- Lobby lists all games alphabetically in one list
- Include filters: only favorites, only released (default true), search
- Each game has a settings page and a how-to-play page before gameplay
- During scaffold phase, gameplay screens are stubs
