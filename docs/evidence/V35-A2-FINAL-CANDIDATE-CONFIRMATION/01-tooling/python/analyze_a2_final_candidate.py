#!/usr/bin/env python3
"""Read-only acceptance and decision analysis for the pre-registered A0/A2 campaign."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import statistics
from collections import defaultdict
from pathlib import Path

EPS = 1e-12
ARMS = ("A0", "A2")


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def props(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith(("#", "!")):
            key, value = line.split("=", 1)
            if key in values:
                raise ValueError("duplicate property %s in %s" % (key, path))
            values[key] = value
    return values


def write_csv(path: Path, rows: list[dict], fields: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader(); writer.writerows(rows)


def verify_manifest(root: Path) -> None:
    manifest = root / "evidence-sha256.tsv"
    if not manifest.is_file():
        raise ValueError("missing evidence manifest %s" % root)
    with manifest.open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    if not rows:
        raise ValueError("empty evidence manifest %s" % root)
    for row in rows:
        target = (root / row["path"]).resolve()
        if root.resolve() not in target.parents or not target.is_file() or sha256(target).lower() != row["sha256"].lower():
            raise ValueError("evidence mismatch %s/%s" % (root, row["path"]))


def read_front(path: Path) -> list[tuple[float, float, float]]:
    output: list[tuple[float, float, float]] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        cells = line.split(",")
        if len(cells) < 3 or cells[0].lower() == "cmax":
            continue
        point = tuple(float(value) for value in cells[:3])
        if not all(math.isfinite(value) for value in point):
            raise ValueError("non-finite point %s" % path)
        output.append(point)
    if not output:
        raise ValueError("empty front %s" % path)
    return output


def dominates(left, right) -> bool:
    return all(a <= b + EPS for a, b in zip(left, right)) and any(a < b - EPS for a, b in zip(left, right))


def nondominated(points: list[tuple[float, float, float]]) -> list[tuple[float, float, float]]:
    unique = sorted(set(points))
    return [point for point in unique if not any(other != point and dominates(other, point) for other in unique)]


def normalized(points, reference):
    lo = tuple(min(point[i] for point in reference) for i in range(3))
    hi = tuple(max(point[i] for point in reference) for i in range(3))
    span = tuple(max(EPS, hi[i] - lo[i]) for i in range(3))
    return [tuple((point[i] - lo[i]) / span[i] for i in range(3)) for point in points], lo, hi


def hv(points):
    # Exact 3-D minimization hypervolume for the fixed normalized point (1.1,1.1,1.1).
    ref = 1.1
    points = sorted({tuple(max(0.0, min(ref, value)) for value in point) for point in points})
    volume, active, index = 0.0, [], 0
    while index < len(points):
        x = points[index][0]
        while index < len(points) and points[index][0] <= x + EPS:
            active.append(points[index]); index += 1
        next_x = points[index][0] if index < len(points) else ref
        yz = 0.0
        min_z = ref
        ordered = sorted(active, key=lambda point: point[1])
        for cursor, point in enumerate(ordered):
            y = max(0.0, min(ref, point[1]))
            min_z = min(min_z, max(0.0, min(ref, point[2])))
            next_y = max(0.0, min(ref, ordered[cursor + 1][1])) if cursor + 1 < len(ordered) else ref
            yz += max(0.0, next_y - y) * max(0.0, ref - min_z)
        volume += max(0.0, next_x - x) * yz
    return max(0.0, volume)


def distance(left, right) -> float:
    return math.sqrt(sum((a - b) ** 2 for a, b in zip(left, right)))


def igd(approximation, reference) -> float:
    return sum(min(distance(point, candidate) for candidate in approximation) for point in reference) / len(reference)


def igd_plus(approximation, reference) -> float:
    return sum(min(math.sqrt(sum(max(a - r, 0.0) ** 2 for a, r in zip(candidate, point))) for candidate in approximation)
               for point in reference) / len(reference)


def spacing(points) -> float:
    if len(points) < 2:
        return 0.0
    nearest = [min(distance(point, other) for other in points if other != point) for point in points]
    mean = sum(nearest) / len(nearest)
    return math.sqrt(sum((value - mean) ** 2 for value in nearest) / len(nearest))


def coverage(left, right) -> float:
    return sum(any(dominates(candidate, target) for candidate in left) for target in right) / len(right) if right else 0.0


def relative(value, baseline) -> float:
    if abs(baseline) <= EPS:
        raise ValueError("zero baseline")
    return value / baseline


def audit(run: dict, results_root: Path) -> dict:
    root = results_root / run["instance"] / ("seed-" + str(run["seed"])) / run["arm"]
    verify_manifest(root)
    status, budget, provenance, gate, context = (props(root / name) for name in (
        "status.properties", "budget-termination.properties", "provenance.properties",
        "formal-gate.properties", "final-candidate-context.properties"))
    if status.get("status") != "COMPLETED" or gate.get("status") != "COMPLETED":
        raise ValueError("incomplete %s" % run["runId"])
    actual, decoder, requested = (int(budget[name]) for name in ("actualFE", "decoderCalls", "requestedMaxFE"))
    if not (0 < actual == decoder <= requested == 500000 and int(budget["remainingFE"]) < int(budget["qPhaseFE"]) == 5000 and float(budget["utilizationRate"]) > .99):
        raise ValueError("budget failure %s" % run["runId"])
    if int(status["illegalSolutions"]) or int(status["duplicateEvaluations"]) or int(status.get("exceptionRepairs", "0")) or int(status.get("missingSources", "0")):
        raise ValueError("invalid run counters %s" % run["runId"])
    if context.get("campaignPurpose") != "FINAL_CANDIDATE_CONFIRMATION" or context.get("runId") != run["runId"] or context.get("arm") != run["arm"]:
        raise ValueError("classification drift %s" % run["runId"])
    mapping = {"frozenJarSha256": "jarSha256", "armProfileSha256": "armProfileSha256", "snapshotSha256": "snapshotSha256",
               "initialPopulationHashV35": "initialPopulationHashV35", "initialPopulationHashP8": "initialPopulationHashP8",
               "problemConfigurationSha256": "problemConfigurationSha256"}
    for evidence_key, manifest_key in mapping.items():
        if provenance.get(evidence_key, "").lower() != str(run[manifest_key]).lower():
            raise ValueError("provenance drift %s/%s" % (run["runId"], evidence_key))
    raw = read_front(root / "front.csv")
    return {"root": root, "front": nondominated(raw), "rawSize": len(raw), "actualFE": actual,
            "utilization": float(budget["utilizationRate"]), "algorithmNanos": int(status["algorithmRunNanos"])}


def write_manifest(root: Path) -> None:
    rows = []
    for path in sorted(item for item in root.rglob("*") if item.is_file() and item.name != "evidence-sha256.tsv"):
        rows.append((path.relative_to(root).as_posix(), sha256(path)))
    with (root / "evidence-sha256.tsv").open("w", encoding="utf-8", newline="") as stream:
        stream.write("path\tsha256\n")
        for path, digest in rows:
            stream.write("%s\t%s\n" % (path, digest))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--results-root", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    runs = manifest.get("runs", [])
    if manifest.get("schema") != "v35-a2-final-candidate-manifest-v1" or len(runs) != 60 or len({row["runId"] for row in runs}) != 60:
        raise ValueError("expected exactly 60 unique confirmation runs")
    groups: dict[tuple[str, int], list[dict]] = defaultdict(list)
    for run in runs:
        if run["arm"] not in ARMS or run["maxFEs"] != 500000:
            raise ValueError("manifest arm/budget drift")
        groups[(run["instance"], int(run["seed"]))].append(run)
    if len(groups) != 30 or any(tuple(sorted(row["arm"] for row in pair)) != ARMS for pair in groups.values()):
        raise ValueError("pair roster drift")

    records, audit_rows = {}, []
    for run in sorted(runs, key=lambda row: (row["instance"], int(row["seed"]), row["arm"])):
        record = audit(run, args.results_root); records[(run["instance"], run["seed"], run["arm"])] = record
        audit_rows.append({"runId": run["runId"], "instance": run["instance"], "seed": run["seed"], "arm": run["arm"], "status": "ACCEPTED",
                           "actualFE": record["actualFE"], "utilizationRate": record["utilization"], "frontRawSize": record["rawSize"],
                           "frontNondominatedSize": len(record["front"]), "frontSha256": sha256(record["root"] / "front.csv")})
    args.output.mkdir(parents=True, exist_ok=True)
    write_csv(args.output / "acceptance-run-audit.csv", audit_rows, list(audit_rows[0]))

    metrics, deltas, instance_summary = [], [], []
    refs = args.output / "reference-fronts"; refs.mkdir(exist_ok=True)
    instance_runs: dict[str, list[dict]] = defaultdict(list)
    for run in runs: instance_runs[run["instance"]].append(run)
    for instance, group in sorted(instance_runs.items()):
        reference = nondominated([point for run in group for point in records[(run["instance"], run["seed"], run["arm"])]["front"]])
        if not reference:
            raise ValueError("empty reference %s" % instance)
        with (refs / (instance + ".csv")).open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream); writer.writerow(("Cmax", "TEC", "TWC")); writer.writerows(reference)
        ref_norm, lows, highs = normalized(reference, reference)
        per_seed = {}
        for run in sorted(group, key=lambda row: (int(row["seed"]), row["arm"])):
            record = records[(run["instance"], run["seed"], run["arm"])]
            norm, _, _ = normalized(record["front"], reference)
            entry = {"instance": instance, "scale": instance.split("_", 1)[0], "seed": run["seed"], "arm": run["arm"],
                     "HV": hv(norm), "IGD": igd(norm, ref_norm), "IGDplus": igd_plus(norm, ref_norm), "Spacing": spacing(norm),
                     "frontSize": len(record["front"]), "frontRawSize": record["rawSize"], "Cmax": min(point[0] for point in record["front"]),
                     "TEC": min(point[1] for point in record["front"]), "TWC": min(point[2] for point in record["front"]),
                     "actualFE": record["actualFE"], "utilizationRate": record["utilization"], "algorithmSeconds": record["algorithmNanos"] / 1e9,
                     "referenceSize": len(reference), "idealCmax": lows[0], "idealTEC": lows[1], "idealTWC": lows[2],
                     "nadirCmax": highs[0], "nadirTEC": highs[1], "nadirTWC": highs[2]}
            metrics.append(entry); per_seed[(run["seed"], run["arm"])] = (entry, record["front"])
        per_instance_deltas = []
        for seed in sorted({run["seed"] for run in group}, key=int):
            a0, f0 = per_seed[(seed, "A0")]; a2, f2 = per_seed[(seed, "A2")]
            delta = {"instance": instance, "scale": instance.split("_", 1)[0], "seed": seed,
                     "DeltaCmax": relative(a0["Cmax"] - a2["Cmax"], a0["Cmax"]), "DeltaTEC": relative(a0["TEC"] - a2["TEC"], a0["TEC"]),
                     "DeltaTWC": relative(a0["TWC"] - a2["TWC"], a0["TWC"]), "DeltaHV": relative(a2["HV"] - a0["HV"], a0["HV"]),
                     "DeltaIGD": relative(a0["IGD"] - a2["IGD"], a0["IGD"]), "C_A2_A0": coverage(f2, f0), "C_A0_A2": coverage(f0, f2)}
            deltas.append(delta); per_instance_deltas.append(delta)
        instance_summary.append({"instance": instance, "scale": instance.split("_", 1)[0], "referenceSize": len(reference),
                                 **{"median" + key: statistics.median(row[key] for row in per_instance_deltas) for key in ("DeltaCmax", "DeltaTEC", "DeltaTWC", "DeltaHV", "DeltaIGD")}})

    scale_summary = []
    for scale in ("20", "50", "100"):
        subset = [row for row in deltas if row["scale"] == scale]
        scale_summary.append({"scale": scale, **{"median" + key: statistics.median(row[key] for row in subset) for key in ("DeltaCmax", "DeltaTEC", "DeltaTWC", "DeltaHV", "DeltaIGD")}})
    write_csv(args.output / "metrics.csv", metrics, list(metrics[0]))
    write_csv(args.output / "paired-deltas.csv", deltas, list(deltas[0]))
    write_csv(args.output / "instance-summary.csv", instance_summary, list(instance_summary[0]))
    write_csv(args.output / "scale-summary.csv", scale_summary, list(scale_summary[0]))

    overall = {"median" + key: statistics.median(row[key] for row in deltas) for key in ("DeltaCmax", "DeltaTEC", "DeltaTWC", "DeltaHV", "DeltaIGD")}
    conditions = {
        "allPairsAccepted": len(audit_rows) == 60,
        "overallPositiveHvIgdAndNonnegativeCmax": overall["medianDeltaHV"] > 0 and overall["medianDeltaIGD"] > 0 and overall["medianDeltaCmax"] >= 0,
        "fourOfSixInstanceJointNonnegative": sum(row["medianDeltaHV"] >= 0 and row["medianDeltaIGD"] >= 0 for row in instance_summary) >= 4,
        "allScalePooledJointNonnegative": all(row["medianDeltaHV"] >= 0 and row["medianDeltaIGD"] >= 0 for row in scale_summary),
        "no100JobVeto": not any(row["scale"] == "100" and row["medianDeltaHV"] < -.05 and row["medianDeltaIGD"] < -.20 for row in instance_summary),
        "noTecTwcThreeScaleSystemicRegression": not (all(row["medianDeltaTEC"] < -.02 for row in scale_summary) or all(row["medianDeltaTWC"] < -.02 for row in scale_summary)),
    }
    decision = "A2_FINAL_CANDIDATE_CONFIRMED" if all(conditions.values()) else "A2_NOT_PROMOTED"
    result = {"decision": decision, "overall": overall, "conditions": conditions, "validPairs": len(groups), "validRuns": len(audit_rows),
              "protocol": "v35-a2-final-candidate-confirmation-v1"}
    (args.output / "promotion-decision.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    report = "# A2 主候选确认验收报告\n\n决策：`%s`。\n\n所有60条运行必须先通过文件、预算、provenance与配对审计；本报告只在60/60通过后产生。\n\n" % decision
    report += "总体配对中位：`DeltaCmax=%+.4f%%`、`DeltaTEC=%+.4f%%`、`DeltaTWC=%+.4f%%`、`DeltaHV=%+.4f%%`、`DeltaIGD=%+.4f%%`。\n\n" % tuple(100 * overall["median" + key] for key in ("DeltaCmax", "DeltaTEC", "DeltaTWC", "DeltaHV", "DeltaIGD"))
    report += "门：\n\n" + "\n".join("- %s: `%s`" % (key, value) for key, value in conditions.items()) + "\n"
    (args.output / "A2_FINAL_CANDIDATE_ACCEPTANCE_REPORT.md").write_text(report, encoding="utf-8")
    write_manifest(args.output)
    print("A2_FINAL_CANDIDATE_ANALYSIS_COMPLETED decision=%s acceptedRuns=%d" % (decision, len(audit_rows)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
