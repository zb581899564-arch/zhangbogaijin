#!/usr/bin/env python3
"""Create the immutable top-level SHA-256 index for V35-A3-D2 evidence."""
from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "evidence-sha256.tsv"


def sha(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def main() -> None:
    rows = []
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file() or "__pycache__" in path.parts or path == OUT:
            continue
        rows.append((sha(path), str(path.stat().st_size), path.relative_to(ROOT).as_posix()))
    OUT.write_text("sha256\tbytes\tpath\n" + "".join("\t".join(row) + "\n" for row in rows),
                   encoding="utf-8")
    print("TOP_LEVEL_MANIFEST_FILES=" + str(len(rows)))


if __name__ == "__main__":
    main()
