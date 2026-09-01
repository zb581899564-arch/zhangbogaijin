#!/usr/bin/env python3
"""Delete only explicitly allowlisted expanded campaigns after archive verification."""

from __future__ import annotations

import csv
import os
import shutil
import argparse
from pathlib import Path


ROOT = Path("/home/inspur/aicomp").resolve()
CATALOG = ROOT / "zhangbo-v35-paper-evidence-catalog-20260823"
ARCHIVES = CATALOG / "archives"
ALLOWLIST = (
    "zhangbo-cmax-audit-20k-20260812",
    "zhangbo-fc-500k-20260817",
    "zhangbo-fc4-20260817",
    "zhangbo-fc6a1-20260819",
    "zhangbo-fc6b-region-20260820",
    "zhangbo-fc6b-region-20260820-r1",
    "zhangbo-fc6b-region-20260820-r2",
    "zhangbo-java-p9-decoder-timing-500k-20260811",
    "zhangbo-java-p9-five-additional-500k-20260810",
    "zhangbo-java-p9-pilot-20260810",
    "zhangbo-java-p9-single-500k-20260810",
    "zhangbo-p86-pair-100k-20260811",
    "zhangbo-runtime-audit-100k-20260810",
    "zhangbo-v35-doe1-20260820",
    "zhangbo-v35-doe1-heldout-20260822",
    "zhangbo-v35-p25a-main-variant-20260814",
    "zhangbo-v35-p25d-8alg-50k-20260815",
    "zhangbo-v35-stage2-phasebudget-20260823",
    "zhangbo-v35-stage2-phasebudget-20260823-r2",
)


def stats(root: Path) -> tuple[int, int]:
    count = 0
    total = 0
    for path in root.rglob("*"):
        if path.is_file():
            count += 1
            total += path.stat().st_size
    return count, total


def active_cwds() -> list[tuple[str, str]]:
    result = []
    for entry in Path("/proc").iterdir():
        if not entry.name.isdigit():
            continue
        try:
            cwd = os.readlink(entry / "cwd")
            cmdline = (entry / "cmdline").read_bytes().replace(b"\0", b" ").decode("utf-8", "replace")
            result.append((cwd, cmdline))
        except (FileNotFoundError, PermissionError, OSError):
            continue
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--execute", action="store_true")
    args = parser.parse_args()
    with (CATALOG / "remote-archive-manifest.tsv").open("r", encoding="utf-8", newline="") as stream:
        manifest = {row["campaignId"]: row for row in csv.DictReader(stream, delimiter="\t")}

    cwd_rows = active_cwds()
    planned = []
    for campaign in ALLOWLIST:
        source = (ROOT / campaign).resolve()
        if source.parent != ROOT or source.name != campaign:
            raise RuntimeError(f"unsafe resolved target: {source}")
        row = manifest.get(campaign)
        if row is None or row["status"] != "VERIFIED":
            raise RuntimeError(f"archive manifest is not verified: {campaign}")
        archive = ARCHIVES / f"{campaign}.tar.gz"
        if not archive.is_file() or archive.stat().st_size != int(row["archiveBytes"]):
            raise RuntimeError(f"verified archive missing or changed: {campaign}")
        if source.exists():
            if source.is_symlink() or not source.is_dir():
                raise RuntimeError(f"target is not a real directory: {source}")
            current = stats(source)
            expected = (int(row["fileCount"]), int(row["sourceBytes"]))
            if current != expected:
                raise RuntimeError(f"source drift before cleanup: {campaign}: {current} != {expected}")
            users = [(cwd, cmd) for cwd, cmd in cwd_rows if cwd == str(source) or cwd.startswith(str(source) + "/")]
            if users:
                raise RuntimeError(f"active process cwd blocks cleanup: {campaign}: {users}")
            planned.append((campaign, source, row, archive))

    if not args.execute:
        for campaign, source, row, archive in planned:
            print(f"DRY_RUN\t{campaign}\t{row['sourceBytes']}\t{archive}")
        print(f"planned={len(planned)}")
        return

    records = []
    for campaign, source, row, archive in planned:
        shutil.rmtree(source)
        if source.exists():
            raise RuntimeError(f"cleanup target still exists: {source}")
        records.append(
            (
                campaign,
                str(source),
                row["sourceBytes"],
                str(archive),
                row["archiveSha256"],
                "DELETED_AFTER_REMOTE_AND_G_ARCHIVE_VERIFICATION",
                "true",
            )
        )

    output = CATALOG / "remote-cleanup-execution.tsv.partial"
    with output.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(("campaignId", "path", "bytesBefore", "archivePath", "archiveSha256", "result", "recoverable"))
        writer.writerows(records)
    os.replace(output, CATALOG / "remote-cleanup-execution.tsv")


if __name__ == "__main__":
    main()
