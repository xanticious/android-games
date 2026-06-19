# Morse Code

Shared alphabet, timing, single-button input, and audio rules for the Morse-code rhythm games (Morse Code, Morse Decoder). Individual games specify the mode (send vs. decode), scoring, and screen flow; everything below is common.

## International Morse Code (subset used)

Games use the standard International Morse alphabet. v1 covers the 26 letters and the word space; digits and punctuation are out of scope.

| Letter | Code | Letter | Code | Letter | Code |
|--------|------|--------|------|--------|------|
| A | `.-`   | J | `.---` | S | `...`  |
| B | `-...` | K | `-.-`  | T | `-`    |
| C | `-.-.` | L | `.-..` | U | `..-`  |
| D | `-..`  | M | `--`   | V | `...-` |
| E | `.`    | N | `-.`   | W | `.--`  |
| F | `..-.` | O | `---`  | X | `-..-` |
| G | `--.`  | P | `.--.` | Y | `-.--` |
| H | `....` | Q | `--.-` | Z | `--..` |
| I | `..`   | R | `.-.`  |   |        |

`.` is a **dit** (short), `-` is a **dah** (long).

## Timing (in dit units)

Morse timing is defined in multiples of a single base unit, the **dit length** (`U` milliseconds). The standard ratios are:

| Element | Duration |
|---------|----------|
| Dit (`.`) | 1 × U |
| Dah (`-`) | 3 × U |
| Gap between elements of one letter | 1 × U |
| Gap between letters | 3 × U |
| Gap between words (space) | 7 × U |

- `U` is derived from a target **WPM** using the standard PARIS word: `U (ms) = 1200 / wpm`.
- A slower WPM = larger `U` = more forgiving timing; this is the primary difficulty/training lever.
- Games that key input (Morse Code sender) judge a press as a **dit** or **dah** by comparing its hold duration against thresholds derived from `U` (see *Single-Button Input*). Games that only play audio (Morse Decoder) use `U` to schedule beeps.

## Single-Button Input (send mode)

For games where the player taps out code with one button:

- **Press duration** classifies the symbol:
  - hold `< 2 × U` → **dit**
  - hold `>= 2 × U` → **dah**
  (The 2× midpoint sits between the dit (1×) and dah (3×) durations.)
- **Release duration** (silence after a press) classifies the boundary:
  - gap `< 2 × U` → still within the current letter (collect more symbols)
  - gap `2 × U .. 5 × U` → **letter boundary** (commit the collected symbols as one letter)
  - gap `>= 5 × U` → **word boundary** (commit the current word)
- These thresholds are derived from `U` and widen at slower WPM so beginners get larger margins.
- Pure controller (`controller/`): given a sequence of `(pressMs, gapMs)` and the active `U`, it returns the decoded letters/words. Fully unit-testable, no Android imports.

## Audio (decode mode and feedback)

- A dit/dah is a **sine-tone beep** (a single fixed pitch, e.g. ~600 Hz) lasting 1×U / 3×U, with U-length silences between elements per the timing table.
- The tone pitch is constant; only durations and gaps carry information.
- Beeps are scheduled against a single monotonic clock so playback stays evenly spaced.
- A short, distinct **error sound** plays on a wrong input (different timbre from the code tone) so it is never confused with a dit/dah.

## Display Helpers (shared)

- A reusable **code strip** renders a letter's code as filled dot/dash glyphs (e.g. `-.-.`), used by Training Mode (send) and by optional hints (decode).
- The **active token** (current word or current letter) is always visually highlighted; completed tokens are dimmed; upcoming tokens use the default text color. Colors reference `ui/theme/Color.kt` only.

## Data Model (shared shape)

```
enum class Symbol { DIT, DAH }
val MORSE: Map<Char, List<Symbol>>            // 'A' -> [DIT, DAH], ...
data class MorseTiming(val ditUnitMs: Int)    // U; derived from wpm
fun wpmToUnitMs(wpm: Int): Int = 1200 / wpm
```

The `MORSE` table and timing helpers live in a shared controller/model so both Morse games reference one source of truth.
