#!/usr/bin/env python3
"""Read-only acceptance and metric analysis for the pre-registered A2/A4 confirmation.

This tool never calls the algorithm.  It first verifies all output evidence and
then freezes one empirical reference front per held-out instance.
"""
from __future__ import annotations

import argparse
import bisect
import csv
import hashlib
import json
import math
import statistics
from collections import defaultdict
from pathlib import Path

EPS = 1e-12
INTERNAL = {"A2_CFVF": "A2", "A4_BUDGET_AWARE_CATA": "A4"}
ARMS = ("A2", "A4")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def props(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith(("#", "!")):
            key, value = line.split("=", 1)
            if key in values:
                raise ValueError("duplicate property %s in %s" % (key, path))
            values[key] = value
    return values


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def write_csv(path: Path, rows: list[dict], fields: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def read_front(path: Path) -> list[tuple[float, float, float]]:
    points: list[tuple[float, float, float]] = []
    with path.open(encoding="utf-8") as stream:
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


def nondominated(points: list[tuple[float, float, float]]) -> list[tuple[float, float, float]]:
    """Exact strict 3-D minimization filter, with exact-objective de-duplication."""
    ordered = sorted(set(points))
    if not ordered:
        return []
    ys = sorted({point[1] for point in ordered})
    index_of = {value: index + 1 for index, value in enumerate(ys)}
    tree = [float("inf")] * (len(ys) + 1)

    def query(index: int) -> float:
        answer = float("inf")
        while index:
            answer = min(answer, tree[index])
            index -= index & -index
        return answer

    def update(index: int, value: float) -> None:
        while index < len(tree):
            tree[index] = min(tree[index], value)
            index += index & -index

    accepted: list[tuple[float, float, float]] = []
    for point in ordered:
        idx = index_of[point[1]]
        # Equality is not dominance; an exact duplicate was removed above.
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


def yz_union(points, ry=1.1, rz=1.1):
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
    ordered = sorted([tuple(max(0.0, min(1.1, value)) for value in point) for point in points], key=lambda point: point[0])
    volume, active, cursor = 0.0, [], 0
    while cursor < len(ordered):
        x = ordered[cursor][0]
        while cursor < len(ordered) and ordered[cursor][0] <= x + EPS:
            active.append(ordered[cursor])
            cursor += 1
        next_x = ordered[cursor][0] if cursor < len(ordered) else 1.1
        volume += max(0.0, next_x - x) * yz_union(active)
    return max(0.0, volume)


class KdNode:
    __slots__ = ("point", "index", "axis", "left", "right")

    def __init__(self, items, depth=0):
        self.axis = depth % 3
        items.sort(key=lambda item: item[0][self.axis])
        mid = len(items) // 2
        self.point, self.index = items[mid]
        self.left = KdNode(items[:mid], depth + 1) if mid else None
        self.right = KdNode(items[mid + 1:], depth + 1) if mid + 1 < len(items) else None


def nearest_squared(node, target, skip=None, best=float("inf")):
    if node is None:
        return best
    if node.index != skip:
        best = min(best, sum((node.point[i] - target[i]) ** 2 for i in range(3)))
    delta = target[node.axis] - node.point[node.axis]
    first, second = (node.left, node.right) if delta <= 0 else (node.right, node.left)
    best = nearest_squared(first, target, skip, best)
    return nearest_squared(second, target, skip, best) if delta * delta <= best else best


def igd(approximation, reference):
    tree = KdNode([(point, index) for index, point in enumerate(approximation)])
    return sum(math.sqrt(nearest_squared(tree, point)) for point in reference) / len(reference)


def igd_plus(approximation, reference):
    return sum(min(math.sqrt(sum(max(a[i] - r[i], 0.0) ** 2 for i in range(3))) for a in approximation) for r in reference) / len(reference)


def spacing(points):
    if len(points) < 2:
        return 0.0
    tree = KdNode([(point, index) for index, point in enumerate(points)])
    values = [math.sqrt(nearest_squared(tree, point, index)) for index, point in enumerate(points)]
    mean = sum(values) / len(values)
    return math.sqrt(sum((value - mean) ** 2 for value in values) / len(values))


def coverage(left, right):
    if not left or not right:
        return 0.0
    ys = sorted({point[1] for point in left})
    tree = [float("inf")] * (len(ys) + 1)

    def update(index, value):
        while index < len(tree):
            tree[index] = min(tree[index], value)
            index += index & -index

    def query(index):
        answer = float("inf")
        while index:
            answer = min(answer, tree[index])
            index -= index & -index
        return answer

    ordered_left, ordered_right, cursor, covered = sorted(left), sorted(right), 0, 0
    for target in ordered_right:
        while cursor < len(ordered_left) and ordered_left[cursor][0] <= target[0] + EPS:
            point = ordered_left[cursor]
            update(bisect.bisect_left(ys, point[1]) + 1, point[2])
            cursor += 1
        index = bisect.bisect_right(ys, target[1] + EPS)
        if index and query(index) <= target[2] + EPS:
            covered += 1
    return covered / len(right)


def median(values):
    return statistics.median(values)


def relative(numerator, denominator):
    if abs(denominator) <= EPS:
        raise ValueError("zero denominator in paired metric")
    return numerator / denominator


def verify_manifest(output: Path):
    manifest = output / "evidence-sha256.tsv"
    with manifest.open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    if not rows:
        raise ValueError("empty output evidence manifest %s" % output)
    for row in rows:
        candidate = (output / row["path"]).resolve()
        if output.resolve() not in candidate.parents or not candidate.is_file() or sha256(candidate).lower() != row["sha256"].lower():
            raise ValueError("evidence mismatch %s/%s" % (output, row["path"]))


def audit_run(run, results_root: Path):
    output = results_root / run["instance"] / ("seed-" + str(run["seed"])) / run["arm"]
    verify_manifest(output)
    plan = props(Path(run["plan"]))
    status, budget, provenance, gate, context = (props(output / name) for name in (
        "status.properties", "budget-termination.properties", "provenance.properties",
        "formal-gate.properties", "confirmation-context.properties"))
    if status.get("status") != "COMPLETED" or gate.get("status") != "COMPLETED":
        raise ValueError("incomplete run %s" % run["runId"])
    actual, decoder, requested = int(budget["actualFE"]), int(budget["decoderCalls"]), int(budget["requestedMaxFE"])
    remaining, qphase, utilization = int(budget["remainingFE"]), int(budget["qPhaseFE"]), float(budget["utilizationRate"])
    if not (0 < actual == decoder <= requested == 500000 and 0 <= remaining < qphase == 5000 and utilization > .99):
        raise ValueError("phase budget failure %s" % run["runId"])
    for key in ("illegalSolutions", "duplicateEvaluations"):
        if int(status[key]) != 0:
            raise ValueError("invalid evaluator counter %s/%s" % (run["runId"], key))
    if int(status.get("exceptionRepairs", "0")) != 0 or int(status.get("missingSources", "0")) != 0:
        raise ValueError("invalid repair/source counter %s" % run["runId"])
    if context.get("campaignPurpose") != "CONFIRMATION" or context.get("arm") != run["arm"] or context.get("runId") != run["runId"]:
        raise ValueError("confirmation context drift %s" % run["runId"])
    provenance_keys = {
        "frozenJarSha256": "jarSha256",
        "armProfileSha256": "armProfileSha256",
        "snapshotSha256": "snapshotSha256",
        "initialPopulationHashV35": "initialPopulationHashV35",
        "initialPopulationHashP8": "initialPopulationHashP8",
        "problemConfigurationSha256": "problemConfigurationSha256",
    }
    for evidence_key, manifest_key in provenance_keys.items():
        if provenance.get(evidence_key, "").lower() != run[manifest_key].lower():
            raise ValueError("provenance drift %s/%s" % (run["runId"], evidence_key))
    for key in ("runId", "preRegisteredRunKey", "preRegisteredArmLabel", "arm", "instance", "seed"):
        if context.get(key) != plan.get(key):
            raise ValueError("plan/context mismatch %s/%s" % (run["runId"], key))
    front = nondominated(read_front(output / "front.csv"))
    return {"output": output, "front": front, "actualFE": actual, "decoderCalls": decoder,
            "remainingFE": remaining, "utilization": utilization, "algorithmRunNanos": int(status["algorithmRunNanos"]),
            "frontRawCount": len(read_front(output / "front.csv")), "frontCount": len(front)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest-root", required=True, type=Path)
    parser.add_argument("--results-root", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    manifests = []
    for name, expected in (("first.json", 2), ("remainder.json", 58)):
        data = json.loads((args.manifest_root / name).read_text(encoding="utf-8"))
        if data.get("schema") != "v35-a2-a4-confirmation-manifest-v1" or len(data.get("runs", [])) != expected:
            raise ValueError("manifest drift %s" % name)
        manifests.extend(data["runs"])
    if len(manifests) != 60 or len({row["runId"] for row in manifests}) != 60:
        raise ValueError("expected 60 unique confirmation runs")

    records = {}
    audit_rows = []
    for run in sorted(manifests, key=lambda row: (row["instance"], int(row["seed"]), row["arm"])):
        record = audit_run(run, args.results_root)
        records[(run["instance"], run["seed"], run["arm"])] = record
        audit_rows.append({"runId": run["runId"], "instance": run["instance"], "seed": run["seed"], "arm": run["arm"],
                           "status": "ACCEPTED", "actualFE": record["actualFE"], "decoderCalls": record["decoderCalls"],
                           "remainingFE": record["remainingFE"], "utilizationRate": record["utilization"],
                           "frontRawCount": record["frontRawCount"], "frontNondominatedCount": record["frontCount"],
                           "algorithmRunSeconds": record["algorithmRunNanos"] / 1e9,
                           "frontSha256": sha256(record["output"] / "front.csv")})
    args.output.mkdir(parents=True, exist_ok=True)
    write_csv(args.output / "acceptance-run-audit.csv", audit_rows, list(audit_rows[0]))

    grouped: dict[tuple[str, str], list[dict]] = defaultdict(list)
    for run in manifests:
        grouped[(run["instance"], run["seed"])].append(run)
    if len(grouped) != 30:
        raise ValueError("expected 30 pairs")
    for key, pair in grouped.items():
        if sorted(row["arm"] for row in pair) != list(ARMS):
            raise ValueError("incomplete pair %s" % (key,))
        for field in ("snapshotSha256", "initialPopulationHashV35", "initialPopulationHashP8", "problemConfigurationSha256"):
            if pair[0][field] != pair[1][field]:
                raise ValueError("pair provenance mismatch %s/%s" % (key, field))
        values = [records[(row["instance"], row["seed"], row["arm"])]["actualFE"] for row in pair]
        if max(values) - min(values) >= 5000:
            raise ValueError("pair FE range >= 5000 %s" % (key,))

    reference_root = args.output / "reference-fronts"
    metric_rows, paired_rows, instance_rows, scale_rows = [], [], [], []
    by_instance = defaultdict(list)
    for run in manifests:
        by_instance[run["instance"]].append(run)
    for instance, runs in sorted(by_instance.items()):
        if len(runs) != 10:
            raise ValueError("instance roster incomplete %s" % instance)
        scale = instance.split("_", 1)[0]
        reference = nondominated([point for run in runs for point in records[(run["instance"], run["seed"], run["arm"])]["front"]])
        if not reference:
            raise ValueError("empty reference %s" % instance)
        reference_root.mkdir(parents=True, exist_ok=True)
        with (reference_root / (instance + ".csv")).open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream); writer.writerow(("Cmax", "TEC", "TWC")); writer.writerows(reference)
        ref_norm, lows, highs = normalize(reference, reference)
        by_seed_arm = {(run["seed"], run["arm"]): run for run in runs}
        for run in sorted(runs, key=lambda row: (int(row["seed"]), row["arm"])):
            rec = records[(run["instance"], run["seed"], run["arm"])]
            normal, _, _ = normalize(rec["front"], reference)
            metric_rows.append({"instance": instance, "scale": scale, "seed": run["seed"], "arm": run["arm"],
                                "hv": hypervolume(normal), "igd": igd(normal, ref_norm), "igdPlus": igd_plus(normal, ref_norm),
                                "spacing": spacing(normal), "frontSize": len(rec["front"]), "frontRawSize": rec["frontRawCount"],
                                "minCmax": min(point[0] for point in rec["front"]), "minTEC": min(point[1] for point in rec["front"]),
                                "minTWC": min(point[2] for point in rec["front"]), "actualFE": rec["actualFE"],
                                "utilizationRate": rec["utilization"], "algorithmRunSeconds": rec["algorithmRunNanos"] / 1e9,
                                "referenceSize": len(reference), "idealCmax": lows[0], "idealTEC": lows[1], "idealTWC": lows[2],
                                "nadirCmax": highs[0], "nadirTEC": highs[1], "nadirTWC": highs[2]})
        by_metrics = {(row["seed"], row["arm"]): row for row in metric_rows if row["instance"] == instance}
        deltas = []
        for seed in sorted({run["seed"] for run in runs}, key=int):
            a2, a4 = by_metrics[(seed, "A2")], by_metrics[(seed, "A4")]
            row = {"instance": instance, "scale": a2["scale"], "seed": seed,
                   "deltaCmax": relative(a2["minCmax"] - a4["minCmax"], a2["minCmax"]),
                   "deltaTEC": relative(a2["minTEC"] - a4["minTEC"], a2["minTEC"]),
                   "deltaTWC": relative(a2["minTWC"] - a4["minTWC"], a2["minTWC"]),
                   "deltaHV": relative(a4["hv"] - a2["hv"], a2["hv"]),
                   "deltaIGD": relative(a2["igd"] - a4["igd"], a2["igd"]),
                   "cA4A2": coverage(records[(instance, seed, "A4")]["front"], records[(instance, seed, "A2")]["front"]),
                   "cA2A4": coverage(records[(instance, seed, "A2")]["front"], records[(instance, seed, "A4")]["front"])}
            paired_rows.append(row); deltas.append(row)
        instance_rows.append({"instance": instance, "scale": deltas[0]["scale"], "pairs": len(deltas), "referenceSize": len(reference),
                              **{"median" + key[0].upper() + key[1:]: median([row[key] for row in deltas]) for key in ("deltaCmax", "deltaTEC", "deltaTWC", "deltaHV", "deltaIGD", "cA4A2", "cA2A4")}})

    all_deltas = paired_rows
    for scale in ("20", "50", "100"):
        bucket = [row for row in all_deltas if row["scale"] == scale]
        scale_rows.append({"scale": scale, "pairs": len(bucket),
                           **{"median" + key[0].upper() + key[1:]: median([row[key] for row in bucket]) for key in ("deltaCmax", "deltaTEC", "deltaTWC", "deltaHV", "deltaIGD")}})
    write_csv(args.output / "metrics.csv", metric_rows, list(metric_rows[0]))
    write_csv(args.output / "paired-deltas.csv", paired_rows, list(paired_rows[0]))
    write_csv(args.output / "instance-summary.csv", instance_rows, list(instance_rows[0]))
    write_csv(args.output / "scale-summary.csv", scale_rows, list(scale_rows[0]))

    med = {key: median([row[key] for row in all_deltas]) for key in ("deltaCmax", "deltaTEC", "deltaTWC", "deltaHV", "deltaIGD")}
    positive_instances = sum(row["medianDeltaHV"] >= 0 and row["medianDeltaIGD"] >= 0 for row in instance_rows)
    scale_pass = all(row["medianDeltaHV"] >= 0 and row["medianDeltaIGD"] >= 0 for row in scale_rows)
    hundreds = [row for row in all_deltas if row["scale"] == "100"]
    hundred_pass = median([row["deltaHV"] for row in hundreds]) >= 0 and median([row["deltaIGD"] for row in hundreds]) >= 0
    hundred_veto = any(row["scale"] == "100" and row["medianDeltaHV"] < -.05 and row["medianDeltaIGD"] < -.20 for row in instance_rows)
    all_tec_bad = all(row["medianDeltaTEC"] < -.02 for row in instance_rows)
    all_twc_bad = all(row["medianDeltaTWC"] < -.02 for row in instance_rows)
    gates = {
        "all30PairsValid": len(all_deltas) == 30,
        "overallMedianDeltaHVPositive": med["deltaHV"] > 0,
        "overallMedianDeltaIGDPositive": med["deltaIGD"] > 0,
        "overallMedianDeltaCmaxAtLeastMinus2Percent": med["deltaCmax"] >= -.02,
        "positiveHvIgdInstancesAtLeast4": positive_instances >= 4,
        "everyScaleMedianHvIgdNonnegative": scale_pass,
        "hundredJobPooledMedianHvIgdNonnegative": hundred_pass,
        "noSingleHundredJobVeto": not hundred_veto,
        "noSimultaneousTecAndTwcSystematicRegression": not (all_tec_bad and all_twc_bad),
    }
    decision = "A4_FINAL_CANDIDATE_CONFIRMED" if all(gates.values()) else "A4_NOT_PROMOTED"
    payload = {"schema": "v35-a2-a4-confirmation-analysis-v1", "acceptedRuns": len(audit_rows), "acceptedPairs": len(all_deltas),
               "overallMedians": med, "positiveHvIgdInstances": positive_instances, "gates": gates, "decision": decision}
    (args.output / "promotion-decision.json").write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    report = ["# V35 A2/A4 多实例确认：独立验收报告", "",
              "本报告只读取预注册的60条训练机输出；它未调用算法，也未混入任何开发、DOE或Stage2先导reference。", "",
              "## 运行完整性", "",
              "- 已验收运行：%d / 60；有效配对：%d / 30。" % (len(audit_rows), len(all_deltas)),
              "- 每条运行均通过文件级SHA-256、provenance、有限前沿、phase-consistent预算和共享快照复核。", "",
              "## 每实例参考前沿", "",
              "每个实例以A2/A4共10条raw front严格Pareto并集单独构造PFref；HV reference为归一化空间(1.1,1.1,1.1)。", "",
              "|Instance|Scale|PFref点数|median ΔHV|median ΔIGD|median ΔCmax|median ΔTEC|median ΔTWC|", "|---|---:|---:|---:|---:|---:|---:|---:|"]
    for row in instance_rows:
        report.append("|%s|%s|%d|%+.2f%%|%+.2f%%|%+.2f%%|%+.2f%%|%+.2f%%|" % (row["instance"], row["scale"], row["referenceSize"], 100*row["medianDeltaHV"], 100*row["medianDeltaIGD"], 100*row["medianDeltaCmax"], 100*row["medianDeltaTEC"], 100*row["medianDeltaTWC"]))
    report += ["", "## 预注册裁决", "", "|Gate|Result|", "|---|---|"]
    report += ["|%s|%s|" % (key, "PASS" if value else "FAIL") for key, value in gates.items()]
    report += ["", "**裁决：`%s`。**" % decision, "", "此裁决只决定A4是否进入Final freeze候选；不构成正式论文优越性或显著性结论。"]
    (args.output / "CONFIRMATION_ACCEPTANCE_REPORT.md").write_text("\n".join(report) + "\n", encoding="utf-8")

    files = sorted(path for path in args.output.rglob("*") if path.is_file() and path.name != "evidence-sha256.tsv")
    with (args.output / "evidence-sha256.tsv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t"); writer.writerow(("path", "sha256"))
        for path in files:
            writer.writerow((path.relative_to(args.output).as_posix(), sha256(path)))
    print(json.dumps(payload, ensure_ascii=False))


if __name__ == "__main__":
    main()
