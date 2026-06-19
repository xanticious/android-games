# Word Data Sources — Common Component

Shared specification for the word lists, the Scrabble dictionary, and the definition
lookups used by every word game. Individual game docs reference this file and only
state which list or dictionary they use; they do not re-describe sourcing, filtering,
or bundling.

## Goals
- Every word game is fully offline. All word data ships inside the APK as bundled
  assets; nothing is fetched at runtime.
- A single shared pipeline produces the asset files so all games agree on spelling,
  casing, and which words are allowed.
- Offensive and slur-type words are filtered out of all player-facing lists.

## Sources
| Source | Used for | Notes |
|--------|----------|-------|
| **Wordnik word list** | Anagrams, Anagrams (Arcade), Boggle, Word Slices, Letter Drop, Typing Sprint, Word Chain, Word Ladder, Wordle | General-purpose English word list. Large and varied, including complex/uncommon words. |
| **Scrabble players dictionary** | Scrabble, Scrabble Single Player Challenge | Official tournament word list used to validate legal plays. Kept separate from the Wordnik list. |
| **Wiktionary definitions** | Word Slices, Wordle | Short definition shown after a round ends. Bundled as a local lookup, not a live request. |

## Build-Time Pipeline (offline assets)
The lists are prepared once at build/prep time, not on the device:
1. **Download** the raw Wordnik list and the Scrabble players dictionary into a
   build-tooling directory (outside the app's runtime path).
2. **Normalize**: lowercase, strip whitespace/punctuation, drop entries with
   non-A–Z characters, deduplicate, and sort.
3. **Offensive-word filter**: remove any entry that matches a maintained blocklist of
   offensive words and slurs. The blocklist lives alongside the build tooling and is
   applied to *every* generated list. See "Offensive-word filtering" below.
4. **Per-game derivation**: emit the specific shapes each game needs, for example:
   - A length-indexed structure (words grouped by length) for Anagrams and Boggle.
   - A fixed-length subset (e.g. 5-letter words) for Wordle, split into a curated
     "answers" pool and a larger "allowed guesses" set.
   - A frequency/complexity-tagged set for Word Slices and Typing Sprint difficulty.
5. **Bundle** the resulting files into `assets/` (or a packaged room/db) so they load
   quickly and read-only at runtime.

## Offensive-word filtering
- A single shared blocklist is the source of truth; no game maintains its own.
- Filtering happens at build time so offensive words never reach a device list, and
  also acts as a guess/validation guard so they are neither shown nor accepted.
- The filter removes slurs and offensive terms while keeping ordinary "complex" or
  uncommon vocabulary (the Wordnik list intentionally keeps hard words for games like
  Word Slices and Wordle).
- The blocklist is documented and reviewable; updating it re-runs the pipeline.

## Runtime Access
- A single `WordData` provider (model/controller layer) loads the bundled assets and
  exposes pure lookups: `isValidWord`, `wordsOfLength(n)`, `randomWord(filter)`,
  `anagramSolutions(letters)`, etc. No Android UI imports.
- Definition lookups (`definitionOf(word)`) read the bundled Wiktionary data and return
  a short text snippet, or a graceful "no definition available" fallback.
- All loading is read-only and cached; no network, no writes, no accounts.

## Licensing & Attribution
- Wordnik data, the Scrabble players dictionary, and Wiktionary content are each
  redistributed under their respective licenses; attribution/license text is bundled
  with the app where required.
- The prep tooling records the source URL and snapshot date for each list so the
  bundled assets are reproducible.
