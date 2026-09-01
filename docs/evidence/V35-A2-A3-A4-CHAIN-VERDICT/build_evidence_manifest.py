#!/usr/bin/env python3
"""Create the file-level SHA-256 manifest for the A2/A3/A4 chain verdict."""
from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "evidence-sha256.tsv"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def main():
    rows = []
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file() or path == OUT or "__pycache__" in path.parts:
            continue
        rows.append((sha256(path), str(path.stat().st_size), path.relative_to(ROOT).as_posix()))
    OUT.write_text("sha256\tbytes\tpath\n" + "".join("\t".join(row) + "\n" for row in rows), encoding="utf-8")
    print("MANIFEST_FILES=" + str(len(rows)))


if __name__ == "__main__":
    main()
