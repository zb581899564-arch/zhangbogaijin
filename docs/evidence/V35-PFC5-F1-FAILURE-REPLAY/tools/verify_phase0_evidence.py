#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V35-PFC5-F1 Step 1: independently reverse-verify the Phase 0 evidence ledger.

Reads docs/evidence/V35-PFC5-PHASE0/evidence-sha256.tsv and recomputes SHA-256
for every listed file from disk. Zero FE. No algorithm code touched.

Exit code 0 and a verdict of CLOSED only when every row matches.
"""
import hashlib
import os
import sys

PHASE0 = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "..", "V35-PFC5-PHASE0")
PHASE0 = os.path.normpath(PHASE0)
REPO = os.path.normpath(os.path.join(PHASE0, "..", "..", ".."))
LEDGER = os.path.join(PHASE0, "evidence-sha256.tsv")
OUT = os.path.join(os.path.dirname(PHASE0), "V35-PFC5-F1-FAILURE-REPLAY",
                   "01-prelaunch-audit", "phase0-evidence-reverify.tsv")


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    rows = []
    with open(LEDGER, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            fields = line.split("\t")
            if fields[0] == "sha256":
                continue
            rows.append(fields)

    total = len(rows)
    matched = missing = mismatch = size_mismatch = 0
    detail = []
    for fields in rows:
        expected, rel, expected_bytes = fields[0], fields[1], fields[2]
        path = os.path.join(REPO, rel.replace("/", os.sep))
        if not os.path.isfile(path):
            missing += 1
            detail.append((rel, expected, "MISSING", expected_bytes, "", "MISSING"))
            continue
        actual = sha256_file(path)
        actual_bytes = str(os.path.getsize(path))
        if actual == expected and actual_bytes == expected_bytes:
            matched += 1
            detail.append((rel, expected, actual, expected_bytes, actual_bytes, "MATCH"))
        else:
            if actual != expected:
                mismatch += 1
            if actual_bytes != expected_bytes:
                size_mismatch += 1
            detail.append((rel, expected, actual, expected_bytes, actual_bytes, "MISMATCH"))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="") as f:
        f.write("path\texpectedSha256\tactualSha256\texpectedBytes\tactualBytes\tverdict\n")
        for row in detail:
            f.write("\t".join(row) + "\n")

    closed = (missing == 0 and mismatch == 0 and size_mismatch == 0)
    print("phase0LedgerRows=%d" % total)
    print("matched=%d" % matched)
    print("missing=%d" % missing)
    print("mismatch=%d" % mismatch)
    print("sizeMismatch=%d" % size_mismatch)
    print("phase0EvidenceVerdict=%s" % ("CLOSED" if closed else "NOT_CLOSED"))
    print("f1Verdict=%s" % ("PROCEED" if closed else "BLOCKED_PRELAUNCH"))
    print("detailWritten=%s" % os.path.relpath(OUT, REPO).replace(os.sep, "/"))
    return 0 if closed else 1


if __name__ == "__main__":
    sys.exit(main())
