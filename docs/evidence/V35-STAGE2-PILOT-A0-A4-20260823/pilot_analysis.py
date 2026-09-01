#!/usr/bin/env python3
"""Analyze only the twelve fully accepted A0--A4 fairness groups.

This tool performs no algorithm evaluations and deliberately excludes the
interrupted seed-20260820..22 wave.
"""
import argparse
import bisect
import csv
import json
import math
import statistics
import zipfile
from collections import defaultdict
from pathlib import Path

import fc6_metrics as fm

ARMS = ("A0", "A1", "A2", "A3", "A4")
SEEDS = tuple(range(20260808, 20260820))


def fast_nondominated(points):
    """Exact 3-D minimization filter in O(n log n) for the pilot-sized union."""
    ordered = sorted({tuple(p[:3]) for p in points})
    if not ordered:
        return []
    ys = sorted({p[1] for p in ordered})
    y_index = {value: i + 1 for i, value in enumerate(ys)}
    tree = [float("inf")] * (len(ys) + 1)

    def query(index):
        value = float("inf")
        while index > 0:
            value = min(value, tree[index])
            index -= index & -index
        return value

    def update(index, value):
        while index < len(tree):
            tree[index] = min(tree[index], value)
            index += index & -index

    result = []
    for point in ordered:
        index = y_index[point[1]]
        if query(index) <= point[2] + fm.EPS:
            continue
        result.append(list(point))
        update(index, point[2])
    return result


# Preserve the accepted FC-6 metric definitions while replacing only its
# quadratic Pareto implementation with an equivalent scalable 3-D filter.
fm.nondominated = fast_nondominated


class KdNode:
    __slots__ = ("point", "index", "axis", "left", "right")

    def __init__(self, items, depth=0):
        axis = depth % 3
        items.sort(key=lambda item: item[0][axis])
        middle = len(items) // 2
        self.point, self.index = items[middle]
        self.axis = axis
        self.left = KdNode(items[:middle], depth + 1) if middle else None
        self.right = KdNode(items[middle + 1:], depth + 1) if middle + 1 < len(items) else None


def nearest_squared(node, target, skip_index=None, best=float("inf")):
    if node is None:
        return best
    if node.index != skip_index:
        value = sum((node.point[i] - target[i]) ** 2 for i in range(3))
        best = min(best, value)
    delta = target[node.axis] - node.point[node.axis]
    first, second = (node.left, node.right) if delta <= 0 else (node.right, node.left)
    best = nearest_squared(first, target, skip_index, best)
    if delta * delta <= best:
        best = nearest_squared(second, target, skip_index, best)
    return best


def fast_igd(approximation, reference):
    tree = KdNode([(list(point), index) for index, point in enumerate(approximation)])
    return sum(math.sqrt(nearest_squared(tree, point)) for point in reference) / len(reference)


def fast_spacing(points):
    if len(points) < 2:
        return 0.0
    tree = KdNode([(list(point), index) for index, point in enumerate(points)])
    distances = [math.sqrt(nearest_squared(tree, point, index)) for index, point in enumerate(points)]
    average = sum(distances) / len(distances)
    return math.sqrt(sum((value - average) ** 2 for value in distances) / len(distances))


def fast_coverage(left, right):
    if not right or not left:
        return 0.0
    ys = sorted({point[1] for point in left})
    tree = [float("inf")] * (len(ys) + 1)

    def update(index, value):
        while index < len(tree):
            tree[index] = min(tree[index], value)
            index += index & -index

    def query(index):
        value = float("inf")
        while index > 0:
            value = min(value, tree[index])
            index -= index & -index
        return value

    ordered_left = sorted(left, key=lambda p: p[0])
    ordered_right = sorted(right, key=lambda p: p[0])
    cursor = 0
    covered = 0
    for target in ordered_right:
        while cursor < len(ordered_left) and ordered_left[cursor][0] <= target[0] + fm.EPS:
            point = ordered_left[cursor]
            update(bisect.bisect_left(ys, point[1]) + 1, point[2])
            cursor += 1
        index = bisect.bisect_right(ys, target[1] + fm.EPS)
        if index and query(index) <= target[2] + fm.EPS:
            covered += 1
    return covered / len(right)


fm.igd = fast_igd
fm.spacing = fast_spacing
fm.coverage = fast_coverage


def median(values):
    return statistics.median(values)


def mean(values):
    return statistics.fmean(values)


def std(values):
    return statistics.stdev(values) if len(values) > 1 else 0.0


def rel(new, old, higher_better=True):
    if abs(old) <= 1e-12:
        return 0.0
    raw = (new - old) / old
    return raw if higher_better else -raw


def read_properties_text(text):
    out = {}
    for line in text.splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            out[key.strip()] = value.strip()
    return out


def read_properties(path):
    return read_properties_text(path.read_text(encoding="utf-8"))


def equal_point(a, b):
    return all(abs(a[i] - b[i]) <= fm.EPS for i in range(3))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-root", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    root = Path(args.run_root)
    out = Path(args.output)
    out.mkdir(parents=True, exist_ok=True)

    fronts = {}
    budgets = {}
    for seed in SEEDS:
        seed_name = f"seed-{seed}"
        for arm in ARMS:
            run = root / "100_2_3_1" / seed_name / arm
            status = read_properties(run / "status.properties")
            budget = read_properties(run / "budget-termination.properties")
            if status.get("status") != "COMPLETED":
                raise RuntimeError(f"not completed: {run}")
            if budget.get("phaseBoundAccepted") != "true":
                raise RuntimeError(f"budget gate failed: {run}")
            fronts[(seed, arm)] = fm.read_front(run / "front.csv")
            budgets[(seed, arm)] = budget

    all_points = [p for values in fronts.values() for p in values]
    reference = fm.nondominated(fm.unique(all_points))
    if not reference:
        raise RuntimeError("empty reference front")
    with (out / "reference-front.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(("Cmax", "TEC", "TWC"))
        writer.writerows(reference)

    rows = []
    for seed in SEEDS:
        for arm in ARMS:
            points = fronts[(seed, arm)]
            values = fm.metrics(points, reference, "corrected")
            values.update(fm.min_objectives(points))
            values.update({"seed": seed, "arm": arm,
                           "actualFE": int(budgets[(seed, arm)]["actualFE"]),
                           "utilization": float(budgets[(seed, arm)]["utilizationRate"])})
            rows.append(values)
    metric_fields = ("seed", "arm", "actualFE", "utilization", "hv", "igd", "spacing",
                     "cFwd", "cRev", "n", "rawN", "minCmax", "minTEC", "minTWC")
    with (out / "metrics.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=metric_fields)
        writer.writeheader()
        writer.writerows({k: row[k] for k in metric_fields} for row in rows)

    by_arm = defaultdict(list)
    for row in rows:
        by_arm[row["arm"]].append(row)
    summaries = []
    for arm in ARMS:
        bucket = by_arm[arm]
        arm_points = fm.nondominated(fm.unique([p for seed in SEEDS for p in fronts[(seed, arm)]]))
        contributed = sum(1 for ref in reference if any(equal_point(ref, p) for p in arm_points))
        summary = {"arm": arm, "runs": len(bucket), "referenceContribution": contributed}
        for key in ("hv", "igd", "minCmax", "minTEC", "minTWC", "rawN", "actualFE", "utilization"):
            values = [r[key] for r in bucket]
            summary[f"{key}Median"] = median(values)
            summary[f"{key}Mean"] = mean(values)
            summary[f"{key}Std"] = std(values)
        summaries.append(summary)
    with (out / "arm-summary.csv").open("w", encoding="utf-8", newline="") as handle:
        fields = list(summaries[0])
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader(); writer.writerows(summaries)

    index = {(r["seed"], r["arm"]): r for r in rows}
    paired_rows = []
    pair_summaries = []
    pairs = list(zip(ARMS, ARMS[1:])) + [("A0", "A4")]
    for left, right in pairs:
        pair_bucket = []
        for seed in SEEDS:
            a, b = index[(seed, left)], index[(seed, right)]
            row = {
                "seed": seed, "fromArm": left, "toArm": right,
                "deltaHV": rel(b["hv"], a["hv"], True),
                "deltaIGD": rel(b["igd"], a["igd"], False),
                "deltaCmax": rel(b["minCmax"], a["minCmax"], False),
                "deltaTEC": rel(b["minTEC"], a["minTEC"], False),
                "deltaTWC": rel(b["minTWC"], a["minTWC"], False),
            }
            paired_rows.append(row); pair_bucket.append(row)
        summary = {"fromArm": left, "toArm": right, "runs": len(pair_bucket)}
        for key in ("deltaHV", "deltaIGD", "deltaCmax", "deltaTEC", "deltaTWC"):
            values = [r[key] for r in pair_bucket]
            summary[f"{key}Median"] = median(values)
            summary[f"{key}Wins"] = sum(v > 1e-12 for v in values)
            summary[f"{key}Ties"] = sum(abs(v) <= 1e-12 for v in values)
            summary[f"{key}Losses"] = sum(v < -1e-12 for v in values)
        summary["catastrophicSeeds"] = sum(
            r["deltaHV"] < -0.05 and r["deltaIGD"] < -0.20 for r in pair_bucket)
        pair_summaries.append(summary)
    with (out / "paired-increments.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(paired_rows[0]))
        writer.writeheader(); writer.writerows(paired_rows)
    with (out / "paired-summary.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(pair_summaries[0]))
        writer.writeheader(); writer.writerows(pair_summaries)

    pddr_rows = []
    for seed in SEEDS:
        for arm in ARMS:
            archive = root / "100_2_3_1" / f"seed-{seed}" / arm / "large-events.zip"
            with zipfile.ZipFile(archive) as zf:
                summary = read_properties_text(zf.read("cmax-audit-summary.txt").decode("utf-8"))
            final_min = index[(seed, arm)]["minCmax"]
            global_best = float(summary["bestCmaxGlobal"])
            generated_best = float(summary["bestCmaxGenerated"])
            records = int(summary["recordCount"])
            retained = int(summary["pddrRetained"])
            pddr_rows.append({
                "seed": seed, "arm": arm, "finalFrontMinCmax": final_min,
                "auditBestGlobalCmax": global_best, "auditBestGeneratedCmax": generated_best,
                "generatedBetterThanGlobal": generated_best + 1e-12 < global_best,
                "generatedToGlobalGap": (global_best - generated_best) / max(1e-12, global_best),
                "recordCount": records, "pddrRetained": retained,
                "recordRetentionRate": retained / max(1, records),
                "globalArchive": int(summary["globalArchive"]),
                "personalArchive": int(summary["personalArchive"]),
            })
    with (out / "pddr-cmax-lifecycle.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(pddr_rows[0]))
        writer.writeheader(); writer.writerows(pddr_rows)
    pddr_summary = []
    for arm in ARMS:
        bucket = [r for r in pddr_rows if r["arm"] == arm]
        pddr_summary.append({
            "arm": arm, "runs": len(bucket),
            "generatedBetterThanGlobalRuns": sum(r["generatedBetterThanGlobal"] for r in bucket),
            "medianGeneratedToGlobalGap": median([r["generatedToGlobalGap"] for r in bucket]),
            "medianRecordRetentionRate": median([r["recordRetentionRate"] for r in bucket]),
            "medianRecordCount": median([r["recordCount"] for r in bucket]),
        })
    with (out / "pddr-summary.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(pddr_summary[0]))
        writer.writeheader(); writer.writerows(pddr_summary)

    data = {"referenceSize": len(reference), "acceptedRuns": len(rows),
            "acceptedGroups": len(SEEDS), "armSummaries": summaries,
            "pairSummaries": pair_summaries, "pddrSummaries": pddr_summary}
    (out / "pilot-summary.json").write_text(json.dumps(data, indent=2), encoding="utf-8")

    report = [
        "# A0--A4 500k 十二种子先导分析", "",
        "本报告只使用已完整通过五臂公平组审计的60条运行；中止wave的8条孤立完成结果和7个partial尝试均排除。", "",
        f"统一参考前沿大小：{len(reference)}。指标使用raw-space去重/严格Pareto过滤、统一归一化边界及(1.1,1.1,1.1) HV参考点。", "",
        "## Arm 汇总", "",
        "|Arm|HV median|IGD median|min Cmax median|min TEC median|min TWC median|PFref贡献|", "|---|---:|---:|---:|---:|---:|---:|",
    ]
    for s in summaries:
        report.append(f"|{s['arm']}|{s['hvMedian']:.6f}|{s['igdMedian']:.6f}|{s['minCmaxMedian']:.3f}|{s['minTECMedian']:.3f}|{s['minTWCMedian']:.3f}|{s['referenceContribution']}|")
    report += ["", "## 相邻机制增量（正值表示改善）", "",
               "|Pair|ΔHV median|ΔIGD median|ΔCmax median|ΔTEC median|ΔTWC median|HV W/T/L|IGD W/T/L|灾难seed|",
               "|---|---:|---:|---:|---:|---:|---:|---:|---:|"]
    for s in pair_summaries:
        report.append(f"|{s['fromArm']}→{s['toArm']}|{s['deltaHVMedian']:+.2%}|{s['deltaIGDMedian']:+.2%}|{s['deltaCmaxMedian']:+.2%}|{s['deltaTECMedian']:+.2%}|{s['deltaTWCMedian']:+.2%}|{s['deltaHVWins']}/{s['deltaHVTies']}/{s['deltaHVLosses']}|{s['deltaIGDWins']}/{s['deltaIGDTies']}/{s['deltaIGDLosses']}|{s['catastrophicSeeds']}|")
    report += ["", "## PDDR/Cmax生命周期旁路审计", "",
               "|Arm|生成Cmax记录优于最终global的runs|median gap|记录PDDR保留率median|",
               "|---|---:|---:|---:|"]
    for s in pddr_summary:
        report.append(f"|{s['arm']}|{s['generatedBetterThanGlobalRuns']}/{s['runs']}|{s['medianGeneratedToGlobalGap']:.2%}|{s['medianRecordRetentionRate']:.2%}|")
    report += ["", "## 解释边界", "",
               "- 本结果只覆盖一个100-job实例，属于机制先导，不是正式论文结论。",
               "- PDDR审计中的generated/global差距是需要进一步诊断的信号，不单独证明PDDR必须修改。",
               "- 是否更改PDDR应先进行纯观察生命周期审计或预注册的小型单变量实验，不能直接重启4500条矩阵。", ""]
    (out / "PILOT_REPORT.md").write_text("\n".join(report), encoding="utf-8")
    print(json.dumps(data, indent=2))


if __name__ == "__main__":
    main()
