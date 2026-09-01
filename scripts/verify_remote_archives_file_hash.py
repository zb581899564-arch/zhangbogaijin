#!/usr/bin/env python3
"""Stream each remote tar archive once and compare every file to the pre-cleanup ledger."""

from __future__ import annotations

import csv
import hashlib
import os
import tarfile
from collections import defaultdict
from pathlib import Path


ROOT = Path("/home/inspur/aicomp")
CATALOG = ROOT / "zhangbo-v35-paper-evidence-catalog-20260823"
ARCHIVES = CATALOG / "archives"


def digest(stream) -> str:
    value = hashlib.sha256()
    for block in iter(lambda: stream.read(8 * 1024 * 1024), b""):
        value.update(block)
    return value.hexdigest().upper()


def main() -> None:
    expected: dict[str, dict[str, str]] = defaultdict(dict)
    with (CATALOG / "remote-artifact-ledger.tsv").open("r", encoding="utf-8", newline="") as stream:
        for row in csv.DictReader(stream, delimiter="\t"):
            campaign = row["campaignId"]
            absolute = Path(row["path"])
            relative = absolute.relative_to(ROOT / campaign).as_posix()
            expected[campaign][campaign + "/" + relative] = row["sha256"].upper()

    rows = []
    for campaign in sorted(expected):
        archive_path = ARCHIVES / f"{campaign}.tar.gz"
        remaining = dict(expected[campaign])
        failures = []
        verified = 0
        with tarfile.open(archive_path, "r|gz") as archive:
            for member in archive:
                expected_sha = remaining.pop(member.name, None)
                if expected_sha is None:
                    continue
                source = archive.extractfile(member)
                if source is None:
                    failures.append(f"{member.name}:not-a-file")
                    continue
                actual = digest(source)
                if actual != expected_sha:
                    failures.append(f"{member.name}:{actual}")
                else:
                    verified += 1
        failures.extend(f"{path}:missing" for path in sorted(remaining))
        rows.append((campaign, len(expected[campaign]), verified, len(failures), "VERIFIED" if not failures else "FAILED"))
        if failures:
            raise RuntimeError(f"archive file hash mismatch: {campaign}: {failures[:5]}")

    partial = CATALOG / "remote-archive-file-hash-validation.tsv.partial"
    with partial.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(("campaignId", "expectedFiles", "verifiedFiles", "failures", "status"))
        writer.writerows(rows)
    os.replace(partial, CATALOG / "remote-archive-file-hash-validation.tsv")
    (CATALOG / "REMOTE_ARCHIVE_FILE_HASHES_VERIFIED").write_text("verified=true\n", encoding="utf-8")


if __name__ == "__main__":
    main()
