# AGENTS Instructions

## Product Scope

Build a personal, offline-only Android collection of single-player games and games vs AI with multiple difficulties. All profile and stat data stays local to the device.

## Non-Negotiables

- Kotlin only
- Android only
- Use KStateMachine for all state transitions (navigation and per-game state)
- Keep navigation simple and modal-free
- Victory/defeat UI must never cover game boards
- No ads, no store, no social, no achievements
- All profile/stat data must remain local to device

---

## Architecture

### Layer Map

| Layer | Package | Responsibility |
|-------|---------|----------------|
| **model** | `model/` | Immutable data classes and enums. Zero Android or UI imports. |
| **state** | `state/` | KStateMachine state machines. Expose state via `StateFlow`. No UI code. |
| **controller** | `controller/` | Pure functions: take model input, return results. No Android, no UI. |
| **view** | `view/` | `@Composable` functions. Render state, fire callbacks. No logic. |
| **styling** | `ui/theme/` | Color tokens and MaterialTheme config only. |

Do not cross these boundaries. A controller must not import `androidx.*`. A composable must not contain business logic. A state machine must not import composables.

### State Machines

- Every navigation transition belongs in `AppStateMachine` using KStateMachine.
- Every game's own state (turn phases, scores, game-over, etc.) gets its own dedicated state machine in `state/`.
- State machines expose their current state as `StateFlow` so composables can collect it.
- Use `sealed interface` for events and `sealed class` / `DefaultState` for states.

### Navigation

`AppStateMachine` owns all screen transitions. `MainActivity` collects `screen: StateFlow<AppScreen>` and renders the matching composable in a `when` block. No NavController, no back-stack library.

To add a new screen:
1. Add a variant to `AppScreen` (sealed interface in `AppStateMachine.kt`).
2. Add a `NavState` node and the corresponding `NavEvent`(s).
3. Wire the transitions inside `createStateMachineBlocking`.
4. Add the `when` branch in `MainActivity`.

### Adding a New Game

1. Add `GameDefinition` in `GameCatalog.kt` with `released = false`.
2. Add a design doc in `/design/<game-name>.md`.
3. When implementing: state machine in `state/`, controller in `controller/`, composable in `view/`.
4. Set `released = true` when the game is playable end-to-end.

---

## Kotlin Conventions

- Use `data class` for all model types.
- Use `sealed interface` / `sealed class` for event and state hierarchies — enables exhaustive `when` with compiler enforcement.
- Prefer `StateFlow` over `LiveData`.
- No `!!` (force-unwrap). Use `?.`, `?:`, `firstOrNull`, or `require`.
- Avoid nullable types where a sensible default exists.
- Use `asSequence()` when chaining multiple filter/map operations over lists.
- Prefer `rememberSaveable` over `remember` in composables that must survive configuration changes.
- Name functions as verbs: `visibleGames(...)`, `openGameSettings(...)`.
- Name state machine events as past-tense or imperative noun phrases: `SplashFinished`, `OpenProfiles`.

---

## Code Quality

- Code should be self-evident. Write comments only when the *why* is non-obvious.
- Keep files focused: one controller per domain, one state machine per concern.
- Prefer small, surgical changes. Avoid unrelated refactors in the same commit.
- No magic numbers or hard-coded strings outside their owning file.

---

## Testing

- Add a unit test for every new or changed piece of behavior in controllers and state machines.
- Tests live in `app/src/test/`. They use plain JUnit — no Android emulator, no Robolectric.
- One assertion per test case. Name tests `<subject>_<condition>_<expectation>` or `<subject>_<scenario>`.
- Do not delete or weaken existing tests.
- Run tests before marking a task done: `./gradlew :app:testDebugUnitTest`.

---

## UX Expectations

- Flat modern UI using Material 3.
- Responsive on phones and tablets.
- Light and dark themes (system-driven via `isSystemInDarkTheme()`).
- Use the Open Color-inspired underwater palette from `ui/theme/Color.kt`. No hex values outside that file.
- Transitions are instantaneous. Full-screen animations are reserved for victory/defeat only.
- Victory/defeat status never overlays the game board; it lives below it.

---

## Content Expectations

- Lobby lists all games alphabetically.
- Lobby filters: Only Favorites, Only Released (default `true`), Search.
- Each game has a Settings screen and a How to Play screen before gameplay begins.
- During scaffold phase, gameplay screens are stubs with `released = false`.
