#!/usr/bin/env python3
"""Build a pre-cleanup, file-level inventory for every ZhangBo remote campaign."""

from __future__ import annotations

import csv
import hashlib
import os
from pathlib import Path
from typing import Iterable


ROOT = Path("/home/inspur/aicomp")
OUTPUT = ROOT / "zhangbo-v35-paper-evidence-catalog-20260823"
CAMPAIGNS = (
    "zhangbo-cmax-audit-20k-20260812",
    "zhangbo-fc-500k-20260817",
    "zhangbo-fc4-20260817",
    "zhangbo-fc6-20260818",
    "zhangbo-fc6a1-20260819",
    "zhangbo-fc6a4-order-20260820",
    "zhangbo-fc6b-region-20260820",
    "zhangbo-fc6b-region-20260820-r1",
    "zhangbo-fc6b-region-20260820-r2",
    "zhangbo-fc6b-region-20260820-r3",
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
    "zhangbo-v35-p25e-corrected-50k-20260815",
    "zhangbo-v35-stage2-master-v2-20260823",
    "zhangbo-v35-stage2-phasebudget-20260823",
    "zhangbo-v35-stage2-phasebudget-20260823-r2",
    "zhangbo-v35-stage2-phasebudget-20260823-r3",
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(8 * 1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest().upper()


def files(root: Path) -> Iterable[Path]:
    if not root.is_dir():
        return ()
    return sorted((p for p in root.rglob("*") if p.is_file()), key=lambda p: str(p))


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    artifact_tmp = OUTPUT / "remote-artifact-ledger.tsv.partial"
    summary_tmp = OUTPUT / "remote-campaign-summary.csv.partial"

    summaries = []
    with artifact_tmp.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(("host", "campaignId", "path", "bytes", "sha256"))
        for campaign in CAMPAIGNS:
            root = ROOT / campaign
            count = 0
            total = 0
            status = "PRESENT" if root.is_dir() else "MISSING"
            if root.is_dir():
                for path in files(root):
                    size = path.stat().st_size
                    writer.writerow(("aic-inspur-home", campaign, str(path), size, sha256(path)))
                    count += 1
                    total += size
            summaries.append((campaign, str(root), status, count, total))

    with summary_tmp.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("campaignId", "remotePath", "status", "fileCount", "totalBytes"))
        writer.writerows(summaries)

    os.replace(artifact_tmp, OUTPUT / "remote-artifact-ledger.tsv")
    os.replace(summary_tmp, OUTPUT / "remote-campaign-summary.csv")
    (OUTPUT / "INVENTORY_COMPLETE").write_text("complete=true\n", encoding="utf-8")


if __name__ == "__main__":
    main()
