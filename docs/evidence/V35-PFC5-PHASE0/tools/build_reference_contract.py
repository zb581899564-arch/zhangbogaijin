#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PFC5-1E: build the frozen FAILURE_REPLAY_REFERENCE_CONTRACT for 100_5_3_1.

Algorithm identity: this tool embeds an exact copy of the metric implementation used
by the historical A2/A4 confirmation acceptance run
(docs/evidence/V35-A2-A4-MULTIINSTANCE-CONFIRMATION/tools/python/analyze_confirmation.py,
EPS=1e-12): PFref = strict-nondominated union (exact dedup, equality is not dominance)
of the 10 raw fronts (A2/A4 x 5 seeds); fronts normalized by PFref per-objective
min/max; HV with reference point (1.1,1.1,1.1) in normalized space; IGD/IGD+ vs
normalized PFref. The original tool file SHA-256 is recorded in the contract.

Outputs (04-reference-contract/):
  pfref-100_5_3_1.csv          rebuilt PFref, canonical serialization
  historical-reference-inputs.csv
  gold-recalc-comparison.csv   recomputed HV/IGD vs historical metrics.csv values
  reference-contract.properties
  contract-summary.json

Zero FE. No algorithm code touched. Order-independence self-test included.
"""
import csv
import hashlib
import json
import math
import os
import random
import sys
from datetime import datetime, timezone

PHASE0 = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EVID = os.path.dirname(PHASE0)
REPO = os.path.dirname(os.path.dirname(EVID))
CONF = os.path.join(EVID, "V35-A2-A4-MULTIINSTANCE-CONFIRMATION", "06-remote-analysis-import")
FETCH = os.path.join(PHASE0, "fetched-remote", "100_5_3_1")
OUT = os.path.join(PHASE0, "04-reference-contract")

INSTANCE = "100_5_3_1"
SEEDS = ["20260901", "20260902", "20260903", "20260904", "20260905"]
ARMS = ["A2", "A4"]
EPS = 1e-12
HV_REF = 1.1
HV_GATE = -0.05
IGD_GATE = -0.20

ORIGINAL_TOOL = os.path.join(EVID, "V35-A2-A4-MULTIINSTANCE-CONFIRMATION",
                             "tools", "python", "analyze_confirmation.py")


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


# ---- exact copies of the historical implementation (analyze_confirmation.py) ----

def read_front(path):
    points = []
    with open(path, encoding="utf-8") as stream:
        for line in stream:
            fields = line.strip().split(",")
            if len(fields) < 3 or fields[0].lower() == "cmax":
                continue
            point = tuple(float(value) for value in fields[:3])
            if not all(math.isfinite(value) for value in point):
                raise ValueError("non-finite objective in %s" % path)
            points.append(point)
    if not points:
        raise ValueError("empty front %s" % path)
    return points


def nondominated(points):
    ordered = sorted(set(points))
    if not ordered:
        return []
    ys = sorted({point[1] for point in ordered})
    index_of = {value: index + 1 for index, value in enumerate(ys)}
    tree = [float("inf")] * (len(ys) + 1)

    def query(index):
        answer = float("inf")
        while index:
            answer = min(answer, tree[index])
            index -= index & -index
        return answer

    def update(index, value):
        while index < len(tree):
            tree[index] = min(tree[index], value)
            index += index & -index

    accepted = []
    for point in ordered:
        idx = index_of[point[1]]
        if query(idx) <= point[2] + EPS:
            continue
        accepted.append(point)
        update(idx, point[2])
    return accepted


def normalize(points, reference):
    lows = [min(point[i] for point in reference) for i in range(3)]
    highs = [max(point[i] for point in reference) for i in range(3)]
    spans = [max(EPS, highs[i] - lows[i]) for i in range(3)]
    return [tuple((point[i] - lows[i]) / spans[i] for i in range(3)) for point in points], lows, highs


def yz_union(points, ry=HV_REF, rz=HV_REF):
    ordered = sorted(points, key=lambda point: point[1])
    area, min_z, cursor = 0.0, rz, 0
    while cursor < len(ordered):
        y = max(0.0, min(ry, ordered[cursor][1]))
        while cursor < len(ordered) and ordered[cursor][1] <= y + EPS:
            min_z = min(min_z, max(0.0, min(rz, ordered[cursor][2])))
            cursor += 1
        next_y = max(y, min(ry, ordered[cursor][1])) if cursor < len(ordered) else ry
        area += max(0.0, next_y - y) * max(0.0, rz - min_z)
    return area


def hypervolume(points):
    ordered = sorted([tuple(max(0.0, min(HV_REF, value)) for value in point)
                      for point in points], key=lambda point: point[0])
    volume, active, cursor = 0.0, [], 0
    while cursor < len(ordered):
        x = ordered[cursor][0]
        while cursor < len(ordered) and ordered[cursor][0] <= x + EPS:
            active.append(ordered[cursor])
            cursor += 1
        next_x = ordered[cursor][0] if cursor < len(ordered) else HV_REF
        volume += max(0.0, next_x - x) * yz_union(active)
    return max(0.0, volume)


def igd(approximation, reference):
    return sum(min(math.sqrt(sum((a[i] - r[i]) ** 2 for i in range(3)))
                   for a in approximation) for r in reference) / len(reference)


# ---- canonical serialization (new, frozen here) ----

def canonical_text(points):
    lines = ["Cmax,TEC,TWC"]
    for point in sorted(set(points)):
        lines.append(",".join("%.17g" % value for value in point))
    return "\n".join(lines) + "\n"


def canonical_hash(points):
    return hashlib.sha256(canonical_text(points).encode("utf-8")).hexdigest()


def main():
    os.makedirs(OUT, exist_ok=True)

    fronts = {}
    inputs_rows = []
    for seed in SEEDS:
        for arm in ARMS:
            path = os.path.join(FETCH, "seed-%s" % seed, arm, "front.csv")
            fronts[(seed, arm)] = read_front(path)
            inputs_rows.append({
                "instance": INSTANCE, "seed": seed, "arm": arm,
                "localPath": os.path.relpath(path, REPO).replace("\\", "/"),
                "sha256": sha256_file(path),
                "points": len(fronts[(seed, arm)]),
            })

    # PFref rebuilt from raw fronts, plus order-independence self-test
    all_points = [p for seed in SEEDS for arm in ARMS for p in fronts[(seed, arm)]]
    pfref = nondominated(all_points)
    shuffled = list(all_points)
    random.Random(20260829).shuffle(shuffled)
    pfref_shuffled = nondominated(shuffled)
    order_independent = (set(pfref) == set(pfref_shuffled)
                         and canonical_hash(pfref) == canonical_hash(pfref_shuffled))

    saved_ref_path = os.path.join(CONF, "reference-fronts", "%s.csv" % INSTANCE)
    saved_ref = read_front(saved_ref_path)
    pfref_matches_saved = (set(pfref) == set(saved_ref))

    with open(os.path.join(OUT, "pfref-%s.csv" % INSTANCE), "w", encoding="utf-8", newline="") as f:
        f.write(canonical_text(pfref))

    ref_norm, lows, highs = normalize(pfref, pfref)
    pfref_hash = canonical_hash(pfref)

    # gold recalc vs historical metrics.csv
    gold = {}
    with open(os.path.join(CONF, "metrics.csv"), encoding="utf-8-sig", newline="") as f:
        for row in csv.DictReader(f):
            if row["instance"] == INSTANCE:
                gold[(row["seed"], row["arm"])] = row

    comparison_rows = []
    recomputed = {}
    max_abs_hv = max_rel_hv = max_abs_igd = max_rel_igd = 0.0
    for seed in SEEDS:
        for arm in ARMS:
            normal, _, _ = normalize(fronts[(seed, arm)], pfref)
            hv = hypervolume(normal)
            igd_v = igd(normal, ref_norm)
            recomputed[(seed, arm)] = (hv, igd_v)
            g = gold[(seed, arm)]
            for name, rec, goldv in (("hv", hv, float(g["hv"])), ("igd", igd_v, float(g["igd"]))):
                absd = abs(rec - goldv)
                reld = absd / max(abs(goldv), 1e-300)
                if name == "hv":
                    max_abs_hv = max(max_abs_hv, absd); max_rel_hv = max(max_rel_hv, reld)
                else:
                    max_abs_igd = max(max_abs_igd, absd); max_rel_igd = max(max_rel_igd, reld)
                comparison_rows.append({
                    "seed": seed, "arm": arm, "metric": name,
                    "historical": repr(goldv), "recomputed": repr(rec),
                    "absDiff": repr(absd), "relDiff": repr(reld),
                })

    with open(os.path.join(OUT, "historical-reference-inputs.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["instance", "seed", "arm", "localPath", "sha256", "points"],
                           lineterminator="\n")
        w.writeheader()
        w.writerows(inputs_rows)
    with open(os.path.join(OUT, "gold-recalc-comparison.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["seed", "arm", "metric", "historical", "recomputed",
                                          "absDiff", "relDiff"], lineterminator="\n")
        w.writeheader()
        w.writerows(comparison_rows)

    selected_seed = "20260901"
    hv_a2_hist, igd_a2_hist = recomputed[(selected_seed, "A2")]

    props = {
        "contractId": "FAILURE_REPLAY_REFERENCE_CONTRACT_V1",
        "instance": INSTANCE,
        "instanceSha256": "2e88fa97a6f84af347a4603f04c387a65c8f9891bcab8ac6b70fdec622ea35cf",
        "objectiveOrder": "[Cmax,TEC,TWC]",
        "objectiveSlots": "[0,1,6]",
        "pfrefConstruction": "strict-nondominated union (exact dedup, equality != dominance, EPS=1e-12) of the 10 historical raw fronts (A2/A4 x seeds 20260901..20260905), single construction, never updated",
        "pfrefPoints": str(len(pfref)),
        "pfrefCanonicalSha256": pfref_hash,
        "pfrefMatchesHistoricalSavedReference": str(pfref_matches_saved).lower(),
        "pfrefRebuildOrderIndependent": str(order_independent).lower(),
        "ideal": repr(lows),
        "nadir": repr(highs),
        "normalization": "per-objective (x - ideal)/(nadir - ideal), floor span at EPS=1e-12, ideals/nadirs from frozen PFref only",
        "hvReferencePoint": "(1.1,1.1,1.1) in normalized space; values clamped to [0,1.1]",
        "hvImplementation": "embedded exact copy of analyze_confirmation.py hypervolume (x-sweep + yz_union, EPS=1e-12)",
        "igdImplementation": "embedded exact copy of analyze_confirmation.py igd (mean min euclidean distance to normalized PFref)",
        "historicalToolSha256": sha256_file(ORIGINAL_TOOL),
        "failureGate": "deltaHV < -0.05 AND deltaIGD < -0.20 (joint), vs paired historical A2 under this contract",
        "deltaHVDefinition": "(HV_fresh - HV_histA2) / HV_histA2",
        "deltaIGDDefinition": "(IGD_histA2 - IGD_fresh) / IGD_histA2  (positive = fresh better)",
        "cmaxRole": "NOT a failure gate; reported for mechanism interpretation only",
        "comparisonTargetSeed": selected_seed,
        "comparisonTargetFrontSha256": sha256_file(os.path.join(FETCH, "seed-%s" % selected_seed, "A2", "front.csv")),
        "histA2HvRecomputed": repr(hv_a2_hist),
        "histA2IgdRecomputed": repr(igd_a2_hist),
        "roundingRule": "none; full double precision; strict comparisons",
        "missingDataRule": "empty/non-finite/unreadable fresh front => RUN_INVALID, never FAILURE",
        "checkpointAlignmentRule": "common checkpoints = nominal per completed formal outer cycle at phase-consistent atomic boundaries; actualFE = lastCompletedAtomicBoundaryFE; accepted iff 0 < actualFE <= MaxFEs and remainingFE < 5000; terminal kind = PHASE_CONSISTENT_TERMINAL",
        "referenceUpdatePolicy": "FORBIDDEN: no PFref/ideal/nadir updates at 50k/100k/250k/500k; one frozen basis for F1/F2/F3 and all checkpoints",
        "goldAbsToleranceHv": repr(max_abs_hv),
        "goldRelToleranceHv": repr(max_rel_hv),
        "goldAbsToleranceIgd": repr(max_abs_igd),
        "goldRelToleranceIgd": repr(max_rel_igd),
        "goldGateAbs": "1e-12",
        "goldGateRel": "1e-12",
        "goldGateVerdict": "PASS" if (max_abs_hv <= 1e-12 and max_rel_hv <= 1e-12
                                      and max_abs_igd <= 1e-12 and max_rel_igd <= 1e-12) else "FAIL",
        "generatedAtUtc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "consumedFE": "0",
        "changedAlgorithm": "false",
    }
    with open(os.path.join(OUT, "reference-contract.properties"), "w", encoding="utf-8") as f:
        for k, v in props.items():
            f.write("%s=%s\n" % (k, v))

    summary = {
        "pfrefPoints": len(pfref),
        "pfrefMatchesHistoricalSaved": pfref_matches_saved,
        "orderIndependent": order_independent,
        "maxAbsDiffHv": max_abs_hv, "maxRelDiffHv": max_rel_hv,
        "maxAbsDiffIgd": max_abs_igd, "maxRelDiffIgd": max_rel_igd,
        "histA2Hv": hv_a2_hist, "histA2Igd": igd_a2_hist,
        "pfrefCanonicalSha256": pfref_hash,
        "consumedFE": 0, "changedAlgorithm": False,
    }
    with open(os.path.join(OUT, "contract-summary.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2)
    print(json.dumps(summary, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
