#!/usr/bin/env bash
# V35-FC5-T second-tier 100k acceptance (run-by-run + paired-seed) + remote artifact generation.
set -euo pipefail

ROOT=/home/inspur/aicomp/zhangbo-v35-fc5-transfer-100k-20260825
MAXFES=100000
QPHASE=5000   # population(100) x Q_Times(50)

python3 - "$ROOT" "$MAXFES" "$QPHASE" <<'PY'
from pathlib import Path
import csv
import math
import hashlib
import sys

root = Path(sys.argv[1])
maxfes = int(sys.argv[2])
qphase = int(sys.argv[3])

tasks = [line.split() for line in (root / "FIRST_TIER_100K_TASKS.tsv").read_text().splitlines() if line.strip()]
if len(tasks) != 6:
    raise SystemExit(f"expected exactly 6 registered runs (got {len(tasks)})")

REQUIRED = (
    "fc5-transfer-merge-rounds.csv",
    "fc5-transfer-windowed-merge-overflow.csv",
    "fc5-transfer-directional-representative-lifecycle.csv",
    "fc5-transfer-archive-working-gap.csv",
    "fc5-transfer-summary.properties",
    "cmax-audit-curves.csv",
)

def finite(cell):
    try:
        v = float(cell)
        return math.isfinite(v)
    except ValueError:
        return False

rows = []
pair = {}          # (instance, seed) -> set of (v35, p8, snapshot)
pair_provenance = {}  # (instance, seed) -> set of (instanceSha256, instanceExtensionSha256, fatigueConfigurationSha256)
failures = 0
verify_fail = 0

for instance, seed, arm, budget in tasks:
    budget = int(budget)
    run = root / "output" / "100k" / instance / f"seed-{seed}" / arm
    props = {}
    for line in (run / "status.properties").read_text().splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            props[k] = v
    initial = {}
    for line in (run / "initial-population.sha256").read_text().splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            initial[k] = v
    cfg = {}
    if (run / "configuration.txt").is_file():
        for line in (run / "configuration.txt").read_text().splitlines():
            if "=" in line:
                k, v = line.split("=", 1)
                cfg[k] = v

    # --- run-level gate ---
    status = props.get("status")
    actual = int(props.get("fullEvaluations", "-1"))
    decoder = int(props.get("decoderCalls", "-1"))
    remaining = maxfes - actual
    illegal = int(props.get("illegalSolutions", "-1"))
    duplicate = int(props.get("duplicateEvaluations", "-1"))
    ok = True
    msgs = []
    if status != "COMPLETED":
        ok = False; msgs.append(f"status={status}")
    if not (0 < actual <= maxfes):
        ok = False; msgs.append(f"actualFE out of range: {actual}")
    if decoder != actual:
        ok = False; msgs.append(f"decoderCalls != actualFE: {decoder} != {actual}")
    if not (0 <= remaining < qphase):
        ok = False; msgs.append(f"remainingFE={remaining} not in [0,{qphase})")
    if remaining < 0 or remaining >= 5000:
        ok = False; msgs.append(f"remainingFE={remaining} >= 5000")
    if illegal != 0:
        ok = False; msgs.append(f"illegalSolutions={illegal}")
    if duplicate != 0:
        ok = False; msgs.append(f"duplicateEvaluations={duplicate}")

    # front.csv non-empty & all finite
    front = run / "front.csv"
    front_rows = 0
    front_ok = True
    if not front.is_file() or front.stat().st_size == 0:
        front_ok = False; msgs.append("front.csv empty/missing")
    else:
        with front.open(newline="", encoding="utf-8") as h:
            rd = csv.reader(h)
            header = next(rd, None)
            for row in rd:
                if not row or all(c.strip() == "" for c in row):
                    continue
                front_rows += 1
                for cell in row:
                    if not finite(cell):
                        front_ok = False; msgs.append(f"front non-finite value: {cell}")
        if front_rows == 0:
            front_ok = False; msgs.append("front.csv has no data rows")
    if not front_ok:
        ok = False

    # required artifact files
    for req in REQUIRED:
        if not (run / req).is_file():
            ok = False; msgs.append(f"missing required {req}")

    # evidence-sha256.tsv reverse verification
    manifest = run / "evidence-sha256.tsv"
    ev_count = 0
    ev_mismatch = 0
    if not manifest.is_file():
        ok = False; msgs.append("missing evidence-sha256.tsv")
    else:
        with manifest.open(newline="", encoding="utf-8") as h:
            try:
                ev_list = list(csv.DictReader(h, delimiter="\t"))
            except Exception as e:
                ok = False; msgs.append(f"evidence manifest parse error: {e}")
                ev_list = []
        for item in ev_list:
            target = run / item["path"]
            ev_count += 1
            if not target.is_file():
                ev_mismatch += 1
                ok = False; msgs.append(f"evidence missing: {item['path']}")
                continue
            digest = hashlib.sha256(target.read_bytes()).hexdigest()
            size = target.stat().st_size
            if digest != item["sha256"] or size != int(item["bytes"]):
                ev_mismatch += 1
                ok = False; msgs.append(f"evidence mismatch: {item['path']}")

    # paired-seed hash collection
    key = (instance, seed)
    pair.setdefault(key, set()).add((initial.get("v35"), initial.get("p8"), initial.get("snapshot")))
    pair_provenance.setdefault(key, set()).add((
        cfg.get("instanceSha256"),
        cfg.get("instanceExtensionSha256"),
        cfg.get("fatigueConfigurationSha256"),
    ))

    if not ok:
        failures += 1
    verify_fail += ev_mismatch

    rows.append({
        "instance": instance,
        "seed": seed,
        "arm": arm,
        "budget": budget,
        "status": status,
        "actualFE": actual,
        "decoderCalls": decoder,
        "remainingFE": remaining,
        "illegalSolutions": illegal,
        "duplicateEvaluations": duplicate,
        "frontRows": front_rows,
        "frontFinite": str(front_ok).lower(),
        "initialPopulationHashV35": initial.get("v35", ""),
        "initialPopulationHashP8": initial.get("p8", ""),
        "snapshotSha256": initial.get("snapshot", ""),
        "instanceSha256": cfg.get("instanceSha256", ""),
        "instanceExtensionSha256": cfg.get("instanceExtensionSha256", ""),
        "fatigueConfigurationSha256": cfg.get("fatigueConfigurationSha256", ""),
        "evidenceFiles": ev_count,
        "evidenceMismatchFiles": ev_mismatch,
        "accept": "true" if ok else "false",
        "notes": ";".join(msgs),
    })

# paired checks
pair_ok = True
for key, hashes in pair.items():
    if len(hashes) != 1:
        pair_ok = False
        print(f"PAIR_INIT_MISMATCH {key}: {hashes}", file=sys.stderr)
for key, prov in pair_provenance.items():
    if len(prov) != 1:
        pair_ok = False
        print(f"PAIR_PROVENANCE_MISMATCH {key}: {prov}", file=sys.stderr)

# within-pair FE spread per seed
fe_spread = {}
for r in rows:
    key = (r["instance"], r["seed"])
    fe_spread.setdefault(key, []).append(r["actualFE"])
spread_ok = True
for key, vals in fe_spread.items():
    if max(vals) - min(vals) >= qphase:
        spread_ok = False
        print(f"PAIR_FE_SPREAD_EXCEEDS_QPHASE {key}: {vals}", file=sys.stderr)

acceptance = root / "FIRST_TIER_100K_ACCEPTANCE.csv"
with acceptance.open("w", newline="", encoding="utf-8") as h:
    w = csv.DictWriter(h, fieldnames=list(rows[0]))
    w.writeheader()
    w.writerows(rows)

all_ok = (failures == 0 and pair_ok and spread_ok)
by_arm = {}
for r in rows:
    by_arm.setdefault(r["arm"], []).append(r["actualFE"])

summary = [
    f"status={'ACCEPTED' if all_ok else 'FAILED'}",
    "runs=6",
    "pairedGroups=3",
    f"runFailures={failures}",
    f"evidenceReverseVerified={'true' if verify_fail == 0 else 'false'}",
    f"evidenceMismatchFiles={verify_fail}",
    f"sameInitialPopulationWithinPair={'true' if pair_ok else 'false'}",
    f"sameProvenanceWithinPair={'true' if pair_ok else 'false'}",
    f"pairFESpreadBelowQPhase={'true' if spread_ok else 'false'}",
    "maxFEs=100000",
    "remainingFEThreshold=5000",
    "phaseBudgetProtocol=PHASE_CONSISTENT_BUDGET_TERMINATION",
    "autoEscalation=false",
]
for arm in sorted(by_arm):
    vals = by_arm[arm]
    summary.append(f"{arm}.runs={len(vals)}")
    summary.append(f"{arm}.actualFEMin={min(vals)}")
    summary.append(f"{arm}.actualFEMax={max(vals)}")
(root / "FIRST_TIER_100K_ACCEPTANCE.properties").write_text("\n".join(summary) + "\n", encoding="utf-8")

print("=== FIRST_TIER_100K_ACCEPTANCE.csv ===")
print(acceptance.read_text())
print("=== FIRST_TIER_100K_ACCEPTANCE.properties ===")
print((root / "FIRST_TIER_100K_ACCEPTANCE.properties").read_text())
if not all_ok:
    raise SystemExit(f"ACCEPTANCE FAILED: {failures} run failures, pair_ok={pair_ok}, spread_ok={spread_ok}")
print("ACCEPTANCE PASSED (all 6 runs).")
PY
