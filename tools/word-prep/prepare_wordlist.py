#!/usr/bin/env python3
"""Offline word-list preparation pipeline for the word games.

Implements the build-time pipeline described in
``design/common/word-data-sources.md``: download the raw Wordnik word list,
normalize it, strip offensive entries using a maintained blocklist, and emit the
bundled asset consumed at runtime by ``WordData``.

The script is build tooling; it is never executed on the device. Re-run it to
regenerate ``app/src/main/assets/words/wordlist.txt`` whenever the source list or
the blocklist changes.

Usage:
    python3 tools/word-prep/prepare_wordlist.py [--download]

Without ``--download`` it expects ``wordlist-raw.txt`` to already exist next to
this script (a previously downloaded snapshot). With ``--download`` it fetches a
fresh copy from the recorded source URL.

Sources (snapshot recorded for reproducibility):
    Wordnik word list: https://github.com/wordnik/wordlist
        file: wordlist-20210729.txt  (snapshot date: 2021-07-29)
    Offensive-word blocklist: https://github.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words
        file: en
"""
from __future__ import annotations

import argparse
import re
import sys
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO_ROOT = HERE.parent.parent

WORDNIK_URL = "https://raw.githubusercontent.com/wordnik/wordlist/main/wordlist-20210729.txt"
RAW_PATH = HERE / "wordlist-raw.txt"
BLOCKLIST_PATH = HERE / "blocklist.txt"
OUTPUT_PATH = REPO_ROOT / "app" / "src" / "main" / "assets" / "words" / "wordlist.txt"

WORD_RE = re.compile(r"[a-z]+")


def load_blocklist(path: Path) -> set[str]:
    """Single-token, a-z-only entries from the maintained blocklist.

    Multi-word phrases cannot match a single-word list entry, so only single
    tokens are used and matching is exact to avoid the "Scunthorpe problem"
    (removing innocent words that merely contain an offensive substring).
    """
    block: set[str] = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        token = line.strip().lower()
        if WORD_RE.fullmatch(token):
            block.add(token)
    return block


def normalize(raw_path: Path) -> set[str]:
    """Lowercase, strip quotes/whitespace, drop non-A-Z entries, dedupe."""
    words: set[str] = set()
    for line in raw_path.read_text(encoding="utf-8").splitlines():
        token = line.strip().strip('"').lower()
        if WORD_RE.fullmatch(token):
            words.add(token)
    return words


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--download",
        action="store_true",
        help="Fetch a fresh Wordnik snapshot before processing.",
    )
    args = parser.parse_args()

    if args.download or not RAW_PATH.exists():
        print(f"Downloading Wordnik list from {WORDNIK_URL}")
        urllib.request.urlretrieve(WORDNIK_URL, RAW_PATH)

    blocklist = load_blocklist(BLOCKLIST_PATH)
    words = normalize(RAW_PATH)
    removed = words & blocklist
    words -= blocklist

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text("\n".join(sorted(words)) + "\n", encoding="utf-8")

    print(f"Blocklist single-word entries : {len(blocklist)}")
    print(f"Normalized unique words       : {len(words) + len(removed)}")
    print(f"Removed by blocklist          : {len(removed)}")
    print(f"Final words written           : {len(words)}")
    print(f"Output                        : {OUTPUT_PATH.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
