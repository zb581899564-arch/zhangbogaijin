#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Reverse-verify every PFC5 evidence ledger (skips '#' comments AND the header row)."""
import hashlib
import io
import os
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                    "..", "..", "..", ".."))

TARGETS = [
    ("V35-PFC5-PHASE0", "evidence-sha256.tsv"),
    ("V35-PFC5-F1-FAILURE-REPLAY", "06-f1-decision/evidence-sha256.tsv"),
    ("V35-PFC5-F2-DEPLOYABILITY-AUDIT", "evidence-sha256.tsv"),
    ("V35-PFC5-TEACHER-EXPOSURE-CAL-PREREG", "evidence-sha256.tsv"),
]

overall_ok = True
for package, rel_ledger in TARGETS:
    root = os.path.join(ROOT, "docs", "evidence", package)
    ledger = os.path.join(root, rel_ledger)
    if not os.path.isfile(ledger):
        print("  %-42s LEDGER MISSING" % package)
        overall_ok = False
        continue
    ok = miss = bad = n = 0
    with open(ledger, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t")
            if len(parts) != 3 or parts[0] == "path":
                continue
            rel, size, want = parts
            n += 1
            p = os.path.join(root, rel.replace("/", os.sep))
            if not os.path.isfile(p):
                miss += 1
                print("      MISSING %s" % rel)
                continue
            got = hashlib.sha256(open(p, "rb").read()).hexdigest()
            if got == want and os.path.getsize(p) == int(size):
                ok += 1
            else:
                bad += 1
                print("      MISMATCH %s" % rel)
    verdict = "CLOSED" if miss == 0 and bad == 0 else "NOT_CLOSED"
    if verdict != "CLOSED":
        overall_ok = False
    print("  %-42s entries=%-4d matched=%-4d missing=%d mismatch=%d  %s"
          % (package, n, ok, miss, bad, verdict))

print()
print("ALL_LEDGERS_CLOSED=%s" % str(overall_ok).lower())
sys.exit(0 if overall_ok else 1)
