#!/usr/bin/env python3
"""Create and verify full, campaign-scoped archives without deleting sources."""

from __future__ import annotations

import csv
import hashlib
import os
import subprocess
import tarfile
from pathlib import Path


ROOT = Path("/home/inspur/aicomp")
CATALOG = ROOT / "zhangbo-v35-paper-evidence-catalog-20260823"
ARCHIVES = CATALOG / "archives"
SUMMARY = CATALOG / "remote-campaign-summary.csv"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(8 * 1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest().upper()


def source_stats(root: Path) -> tuple[int, int]:
    count = 0
    total = 0
    for path in root.rglob("*"):
        if path.is_file():
            count += 1
            total += path.stat().st_size
    return count, total


def archive_stats(archive: Path, campaign: str) -> tuple[int, int]:
    count = 0
    total = 0
    prefix = campaign + "/"
    with tarfile.open(archive, "r:gz") as stream:
        for member in stream:
            normalized = member.name.replace("\\", "/")
            if normalized == campaign:
                continue
            if not normalized.startswith(prefix) or normalized.startswith("/") or "../" in normalized:
                raise RuntimeError(f"unsafe archive member: {member.name}")
            if member.isfile():
                count += 1
                total += member.size
    return count, total


def main() -> None:
    ARCHIVES.mkdir(parents=True, exist_ok=True)
    with SUMMARY.open("r", encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream))

    manifest = []
    for row in rows:
        campaign = row["campaignId"]
        source = ROOT / campaign
        if not source.is_dir():
            manifest.append((campaign, "MISSING", 0, 0, "", "", ""))
            continue

        expected_count, expected_bytes = source_stats(source)
        if expected_count != int(row["fileCount"]) or expected_bytes != int(row["totalBytes"]):
            raise RuntimeError(f"inventory drift before archive: {campaign}")

        target = ARCHIVES / f"{campaign}.tar.gz"
        partial = ARCHIVES / f".{campaign}.tar.gz.partial"
        if target.exists():
            archived_count, archived_bytes = archive_stats(target, campaign)
            if (archived_count, archived_bytes) != (expected_count, expected_bytes):
                raise RuntimeError(f"existing archive mismatch: {campaign}")
        else:
            partial.unlink(missing_ok=True)
            subprocess.run(
                ["tar", "-C", str(ROOT), "-czf", str(partial), campaign],
                check=True,
            )
            archived_count, archived_bytes = archive_stats(partial, campaign)
            if (archived_count, archived_bytes) != (expected_count, expected_bytes):
                raise RuntimeError(f"archive verification failed: {campaign}")
            os.replace(partial, target)

        manifest.append(
            (
                campaign,
                "VERIFIED",
                expected_count,
                expected_bytes,
                target.stat().st_size,
                sha256(target),
                str(target),
            )
        )

    tmp = CATALOG / "remote-archive-manifest.tsv.partial"
    with tmp.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(
            ("campaignId", "status", "fileCount", "sourceBytes", "archiveBytes", "archiveSha256", "archivePath")
        )
        writer.writerows(manifest)
    os.replace(tmp, CATALOG / "remote-archive-manifest.tsv")
    (CATALOG / "REMOTE_ARCHIVES_VERIFIED").write_text("verified=true\n", encoding="utf-8")


if __name__ == "__main__":
    main()
