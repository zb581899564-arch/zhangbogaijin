#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Verify the ROADMAP write-back for V35-PFC5-CAL-P0 (section 19)."""
import hashlib
import io
import os
import re
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                    "..", "..", "..", ".."))
ROADMAP = os.path.join(ROOT, "docs", "ROADMAP.md")
lines = open(ROADMAP, encoding="utf-8").read().split("\n")

print("roadmapSha256=%s" % hashlib.sha256(open(ROADMAP, "rb").read()).hexdigest())
print()
print("=== status table ===")
for l in lines:
    m = re.match(r"^\|\s*`(PFC5-[A-Z0-9]+)`\s*\|([^|]*)\|\s*`([^`]*)`\s*\|", l)
    if m:
        print("  %-10s -> %s" % (m.group(1), m.group(3)))

print()
print("=== stale wording residue (must be 0) ===")
n = sum(1 for l in lines if "反而丢失" in l or ("开遥测" in l and "反而" in l))
print("  residueLines=%d" % n)

print()
print("=== frozen boundary (must stay false/PAUSED) ===")
for l in lines:
    if "A2Promoted=false" in l or "FINAL_FROZEN=false" in l or "formalMatrix=PAUSED" in l:
        print("  " + l.strip()[:110])
