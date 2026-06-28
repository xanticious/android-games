# Word-Prep Tooling

Offline build-time pipeline that produces the bundled word-list asset shared by
all word games. See `design/common/word-data-sources.md` for the full
specification. This tooling runs on a developer machine, never on the device.

## Files
- `prepare_wordlist.py` — download → normalize → offensive-word filter → bundle.
- `blocklist.txt` — maintained offensive-word blocklist (single source of truth).
- `wordlist-raw.txt` — downloaded raw snapshot (git-ignored; regenerated on demand).

## Regenerate the asset
```sh
python3 tools/word-prep/prepare_wordlist.py            # uses cached raw snapshot
python3 tools/word-prep/prepare_wordlist.py --download # refetch raw snapshot first
```
Output: `app/src/main/assets/words/wordlist.txt` (newline-separated, lowercase,
sorted, A–Z only, offensive words removed).

## Sources & attribution
| Data | Source | Snapshot | License |
|------|--------|----------|---------|
| Word list | https://github.com/wordnik/wordlist (`wordlist-20210729.txt`) | 2021-07-29 | MIT |
| Offensive-word blocklist | https://github.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words (`en`) | downloaded snapshot | CC BY 4.0 |

The blocklist is applied with **exact single-word matching** so ordinary complex
or uncommon vocabulary is preserved (the word games intentionally keep hard
words). Multi-word phrases in the blocklist never match single-word entries.

## Scrabble dictionary & definitions
The Scrabble players dictionary and Wiktionary definition lookups described in
`word-data-sources.md` are not bundled in this pass. `WordData` validates
Scrabble plays against this same list and returns a graceful fallback from
`definitionOf`. Drop a `scrabble.txt` / `definitions` asset here and extend the
pipeline to enable them without changing game code.
