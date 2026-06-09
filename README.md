# Android Games Collection

Offline-first Android app with a growing collection of 100+ single-player games and games vs AI. Built entirely in Kotlin with Jetpack Compose for the UI and KStateMachine for all state transitions.

All data stays on device — no ads, no accounts, no network calls.

---

## Getting Started (New to Kotlin / Android?)

This section walks you through everything needed to build and run the app from scratch.

### 1. Install the Tools

| Tool | Why you need it | Download |
|------|-----------------|---------|
| **JDK 17+** | Kotlin runs on the JVM | [Adoptium](https://adoptium.net/) |
| **Android Studio** | IDE + emulator + Android SDK | [developer.android.com/studio](https://developer.android.com/studio) |

After installing Android Studio, open it once so it can download the Android SDK automatically.

### 2. Clone and Open the Project

```bash
git clone https://github.com/xanticious/android-games.git
cd android-games
```

Open the cloned folder in Android Studio (`File → Open`). Wait for Gradle to sync (bottom status bar).

### 3. Run on an Emulator

1. In Android Studio, open **Device Manager** (right-side panel or `Tools → Device Manager`).
2. Click **Create Virtual Device**, pick a phone (e.g. Pixel 6), choose an API 26+ system image, and finish.
3. Press the green **Run ▶** button (or `Shift+F10`). The app launches in the emulator.

### 4. Run on a Physical Device

1. On your Android phone: **Settings → About Phone → tap "Build number" 7 times** to enable Developer Options.
2. Enable **USB Debugging** in Developer Options.
3. Plug in with USB. Android Studio detects the device automatically.
4. Press **Run ▶**.

---

## Build Commands (Terminal)

All commands run from the project root.

```bash
# Debug build
./gradlew :app:assembleDebug

# Install debug build on a connected device / running emulator
./gradlew :app:installDebug

# Run unit tests
./gradlew :app:testDebugUnitTest

# Lint
./gradlew :app:lintDebug

# Release build (unsigned)
./gradlew :app:assembleRelease
```

> **Windows users:** replace `./gradlew` with `gradlew.bat`.

---

## Project Overview

### What's in the App

- **Lobby** — lists all games alphabetically with search, Favorites filter, and Released filter (default on; all games are currently stubs).
- **Game Settings** — per-game configuration screen (stub).
- **How to Play** — rules screen (stub).
- **Game Board** — game play area (stub).
- **App Settings** — theme, audio (stub).
- **Profiles** — local player profiles (stub).

### Game Catalog

100+ games across: Strategy, Puzzle, Word, Card, Action, Educational, Memory, Tower Defense, RTS, Idle, Exploration/Creative. Design docs for every game live in [`/design`](./design).

---

## Code Organization

```
app/src/main/java/com/xanticious/androidgames/
├── MainActivity.kt              # Entry point — wires state machine → views
│
├── model/                       # Pure data — no Android dependencies
│   ├── GameModels.kt            # GameDefinition, LobbyFilter, GameCategory
│   └── GameCatalog.kt           # Static list of all GameDefinitions
│
├── controller/                  # Business logic — operates on models, returns results
│   └── LobbyController.kt       # Filtering and sorting the game list
│
├── state/                       # App-level state machines (KStateMachine)
│   └── AppStateMachine.kt       # Navigation states, events, and screen StateFlow
│
├── view/                        # Jetpack Compose UI — reads state, fires callbacks
│   └── AppViews.kt              # All screen composables
│
└── ui/theme/                    # Styling only — colors, typography, MaterialTheme
    ├── Color.kt                 # Open Color-inspired underwater palette tokens
    └── Theme.kt                 # Light / Dark MaterialTheme configuration
```

### Layer Responsibilities

| Layer | Files | Rule |
|-------|-------|------|
| **model** | `model/` | Plain Kotlin data classes and enums. Zero Android imports. |
| **state** | `state/` | KStateMachine state machines. Expose `StateFlow`s. No UI code. |
| **controller** | `controller/` | Pure functions that transform model data. No Android, no UI. |
| **view** | `view/` | `@Composable` functions only. Read state, fire callbacks, no logic. |
| **styling** | `ui/theme/` | Color tokens and theme config only. Never referenced by state or controller. |

### Navigation Flow

All screen transitions are owned by `AppStateMachine`. `MainActivity` collects the `screen: StateFlow<AppScreen>` and renders the matching composable via a `when` expression. No NavController, no back-stack library.

```
Splash → Lobby ⇄ Profiles
                ⇄ AppSettings
                ⇄ GameSettings → HowToPlay
                               → GameStub
```

Each arrow corresponds to a `NavEvent` in `AppStateMachine`. To add a new screen:
1. Add a `data object` / `data class` to `AppScreen`.
2. Add a `NavState` and `NavEvent` entry.
3. Add the transition(s) in `createStateMachineBlocking`.
4. Add the matching `when` branch in `MainActivity`.

---

## Architecture Principles

- **State machines for all state.** Navigation uses KStateMachine. Each game's own state (turns, scores, phases) will also use a dedicated state machine.
- **Unidirectional data flow.** State lives in `StateFlow`s. Views never mutate state directly — they call callbacks that trigger events.
- **Thin views.** Composables contain layout and rendering only. All decisions happen in controllers or state machines.
- **Pure controllers.** Controller functions are plain functions (`input → output`). Easy to unit-test without mocks or Android.
- **Separate styling.** Color tokens and theme are isolated in `ui/theme`. No hex values outside that package.

---

## Testing

Unit tests live in `app/src/test/`. They cover controllers and business logic using plain JUnit — no Android emulator needed.

```bash
./gradlew :app:testDebugUnitTest
```

When adding behavior, add a test in the corresponding `*Test.kt` file under `src/test/`. Keep tests focused: one assertion per case, descriptive names like `visibleGames_appliesOnlyReleasedFilter`.

---

## Kotlin Conventions Used Here

- `data class` for model types — gives `equals`, `hashCode`, `copy` for free.
- `sealed interface` / `sealed class` for exhaustive `when` expressions (the compiler will warn if you miss a branch).
- `StateFlow` over `LiveData` — works without Android lifecycle in tests.
- `rememberSaveable` in composables to survive configuration changes.
- `asSequence()` in filters to avoid intermediate list allocations.
- No `!!` (force-unwrap). Use `?.`, `?:`, or `firstOrNull`.

---

## Contributing a New Game

1. Add a `GameDefinition` entry to `GameCatalog.kt` with `released = false`.
2. Add a design doc to `/design/<game-name>.md` following the existing format.
3. When implementing: add a state machine in `state/`, a controller in `controller/`, and a composable in `view/`.
4. Flip `released = true` when the game is playable end-to-end.
