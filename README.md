# Android Games Collection (Scaffold)

Offline-first Android app scaffold for a large single-player game collection (100+ games).

## Current Scope

- Kotlin-only Android app scaffold
- MVC-ish separation:
  - Models: `app/src/main/java/com/xanticious/androidgames/model`
  - Controller: `app/src/main/java/com/xanticious/androidgames/controller`
  - Views: `app/src/main/java/com/xanticious/androidgames/view`
  - Navigation state machine: `app/src/main/java/com/xanticious/androidgames/state`
- Navigation powered by **KStateMachine**
- Responsive lobby with:
  - Alphabetical game tiles
  - `Only Favorites` filter
  - `Only Released Games` filter (default enabled)
  - Search filter
- All games are currently **unreleased stubs**
- Light/Dark theme using Open Color-inspired underwater palette
- Local-only/offline intent, no ads/store/social/achievements
- Design placeholders for every game in `/design`

## Game Data

The catalog includes 100+ games across strategy, puzzle, word, card, action, educational, memory, tower defense, RTS, idle, and exploration/creative categories.

For word games, the implementation plan targets Wordnik-based word lists; gameplay integration is pending.

## Development

### Requirements

- JDK 17+
- Android SDK (compileSdk 35)

### Build

```bash
./gradlew :app:assembleDebug
```

### Unit Tests

```bash
./gradlew :app:testDebugUnitTest
```

### Lint

```bash
./gradlew :app:lintDebug
```

## Install on Emulator/Device

1. Start an emulator (Android Studio Device Manager) or connect a device with USB debugging.
2. Build and install:

```bash
./gradlew :app:installDebug
```

3. Launch **Android Games** from the launcher.

## Deploy

This scaffold is for personal/offline use. For release packaging:

```bash
./gradlew :app:assembleRelease
```

Then sign and distribute APK/AAB using standard Android release tooling.
