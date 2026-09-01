#!/usr/bin/env bash
set -euo pipefail

ROOT=/home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825

python3 - "$ROOT" <<'PY'
from pathlib import Path
import csv
import hashlib
import sys

root = Path(sys.argv[1])
tasks = [line.split() for line in (root / "first-tier-50k-tasks.tsv").read_text().splitlines() if line.strip()]
if len(tasks) != 24:
    raise SystemExit("expected exactly 24 registered runs")

rows = []
pair_hashes = {}
for instance, seed, arm in tasks:
    run = root / "output" / "50k" / instance / f"seed-{seed}" / arm
    props = {}
    for line in (run / "status.properties").read_text().splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            props[key] = value
    initial = {}
    for line in (run / "initial-population.sha256").read_text().splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            initial[key] = value
    if props.get("status") != "COMPLETED":
        raise SystemExit(f"not completed: {run}")
    actual = int(props["fullEvaluations"])
    decoder = int(props["decoderCalls"])
    if not (0 < actual <= 50000) or decoder != actual:
        raise SystemExit(f"FE closure failed: {run}")
    if int(props.get("illegalSolutions", "-1")) != 0 or int(props.get("duplicateEvaluations", "-1")) != 0:
        raise SystemExit(f"solution accounting failed: {run}")
    key = (instance, seed)
    pair_hashes.setdefault(key, set()).add((initial.get("v35"), initial.get("p8"), initial.get("snapshot")))

    manifest = run / "evidence-sha256.tsv"
    with manifest.open(newline="", encoding="utf-8") as handle:
        evidence = list(csv.DictReader(handle, delimiter="\t"))
    for item in evidence:
        target = run / item["path"]
        if not target.is_file():
            raise SystemExit(f"missing evidence file: {target}")
        digest = hashlib.sha256(target.read_bytes()).hexdigest()
        if digest != item["sha256"] or target.stat().st_size != int(item["bytes"]):
            raise SystemExit(f"evidence mismatch: {target}")
    rows.append({
        "instance": instance,
        "seed": seed,
        "arm": arm,
        "actualFE": actual,
        "decoderCalls": decoder,
        "initialPopulationHashV35": initial.get("v35", ""),
        "initialPopulationHashP8": initial.get("p8", ""),
        "snapshotSha256": initial.get("snapshot", ""),
        "frontSha256": hashlib.sha256((run / "front.csv").read_bytes()).hexdigest(),
        "evidenceFiles": len(evidence),
        "accepted": "true",
    })

for key, hashes in pair_hashes.items():
    if len(hashes) != 1:
        raise SystemExit(f"paired initial population mismatch: {key}")

acceptance = root / "FIRST_TIER_50K_ACCEPTANCE.csv"
with acceptance.open("w", newline="", encoding="utf-8") as handle:
    writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
    writer.writeheader()
    writer.writerows(rows)

by_arm = {}
for row in rows:
    by_arm.setdefault(row["arm"], []).append(row["actualFE"])
summary = [
    "status=ACCEPTED",
    "runs=24",
    "pairedGroups=12",
    "evidenceReverseVerified=true",
    "sameInitialPopulationWithinPair=true",
    "autoEscalation=false",
]
for arm in sorted(by_arm):
    values = by_arm[arm]
    summary.append(f"{arm}.runs={len(values)}")
    summary.append(f"{arm}.actualFEMin={min(values)}")
    summary.append(f"{arm}.actualFEMax={max(values)}")
(root / "FIRST_TIER_50K_ACCEPTANCE.properties").write_text("\n".join(summary) + "\n", encoding="utf-8")

for name in ("FIRST_TIER_50K_ACCEPTANCE.csv", "FIRST_TIER_50K_ACCEPTANCE.properties"):
    path = root / name
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    print(f"{digest}\t{path.stat().st_size}\t{name}")
PY
