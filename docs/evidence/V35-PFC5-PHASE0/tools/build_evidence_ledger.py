#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate V35-PFC5-PHASE0/evidence-sha256.tsv: per-file SHA-256 ledger.

Covers every file under docs/evidence/V35-PFC5-PHASE0 (reverse-verifiable one by
one). Excludes the ledger itself and this generator. Deterministic ordering.
"""
import hashlib
import os
import sys

PHASE0 = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(PHASE0)))
SELF = os.path.abspath(__file__)
LEDGER = os.path.join(PHASE0, "evidence-sha256.tsv")
# self-referential verification outputs: produced BY the suite that verifies this
# ledger, so they are excluded (like manifestExcludesSelf) and are instead
# re-derived deterministically via tools/run_phase0_tests.py
EXCLUDE_BASENAMES = {"TEST_RESULTS.md", "test-results.json"}


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    entries = []
    for dirpath, dirnames, filenames in os.walk(PHASE0):
        for name in filenames:
            full = os.path.join(dirpath, name)
            if os.path.abspath(full) in (SELF, LEDGER) or name in EXCLUDE_BASENAMES:
                continue
            rel = os.path.relpath(full, REPO).replace("\\", "/")
            entries.append((rel, sha256_file(full), os.path.getsize(full)))
    entries.sort()
    with open(LEDGER, "w", encoding="utf-8", newline="") as f:
        f.write("# V35-PFC5-PHASE0 evidence ledger; per-file sha256; reverse-verify individually; manifestExcludesSelf=true\n")
        f.write("sha256\tpath\tbytes\n")
        for rel, digest, size in entries:
            f.write("%s\t%s\t%d\n" % (digest, rel, size))
    print("ledger entries: %d" % len(entries))
    return 0


if __name__ == "__main__":
    sys.exit(main())
