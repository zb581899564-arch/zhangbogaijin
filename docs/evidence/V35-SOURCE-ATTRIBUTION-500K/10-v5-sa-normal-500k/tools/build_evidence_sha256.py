#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Package-level evidence manifest for 10-v5-sa-normal-500k.

build  : python build_evidence_sha256.py
verify : python build_evidence_sha256.py --verify

Writes evidence-sha256.tsv (path<TAB>sha256<TAB>bytes) over every file in the package,
plus cold-archive-locations.tsv for the two single files >=100 MB (GitHub hard limit;
AGENTS.md §35.5: archive + manifest hash registration instead of committing them).
"""
import hashlib
import os
import sys

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # 10-v5-sa-normal-500k
COLD = "G:/ResearchArchive/ZhangBo-V35-Paper-Evidence-20260823/packages/v35-source-attribution-v5-sa-normal-500k"
SIZE_LIMIT = 100 * 1024 * 1024


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def walk():
    out = []
    for root, dirs, files in os.walk(HERE):
        dirs[:] = [d for d in dirs if d != "__pycache__"]
        for name in files:
            p = os.path.join(root, name)
            rel = os.path.relpath(p, HERE).replace(os.sep, "/")
            if rel in ("evidence-sha256.tsv", "cold-archive-locations.tsv"):
                continue
            out.append((rel, p))
    return sorted(out)


def main():
    verify = "--verify" in sys.argv
    entries = walk()
    rows = []
    cold = []
    for rel, p in entries:
        size = os.path.getsize(p)
        sha = sha256_file(p)
        rows.append((rel, sha, size))
        if size >= SIZE_LIMIT:
            cold.append((rel, size, sha, os.path.join(COLD, os.path.basename(rel)).replace("\\", "/")))
    if verify:
        missing = mismatch = 0
        with open(os.path.join(HERE, "evidence-sha256.tsv"), encoding="utf-8") as f:
            next(f)
            for line in f:
                rel, sha, size = line.rstrip("\n").split("\t")
                p = os.path.join(HERE, rel)
                if not os.path.exists(p):
                    missing += 1
                    print("MISSING", rel)
                    continue
                if sha256_file(p) != sha or os.path.getsize(p) != int(size):
                    mismatch += 1
                    print("MISMATCH", rel)
        print("verify: entries=%d missing=%d mismatch=%d" % (len(rows), missing, mismatch))
        return 1 if (missing or mismatch) else 0
    with open(os.path.join(HERE, "evidence-sha256.tsv"), "w", encoding="utf-8") as f:
        f.write("path\tsha256\tbytes\n")
        for rel, sha, size in rows:
            f.write("%s\t%s\t%d\n" % (rel, sha, size))
    with open(os.path.join(HERE, "cold-archive-locations.tsv"), "w", encoding="utf-8") as f:
        f.write("path\tbytes\tsha256\tcoldArchivePath\n")
        for rel, size, sha, cp in cold:
            f.write("%s\t%d\t%s\t%s\n" % (rel, size, sha, cp))
    print("entries=%d coldArchived=%d" % (len(rows), len(cold)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
