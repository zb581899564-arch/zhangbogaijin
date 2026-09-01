#!/usr/bin/env python3
"""Analyze the single-variable A3 Qp direction-reward clipping experiment."""

from __future__ import annotations

import csv
import hashlib
import math
import re
import runpy
import statistics
from pathlib import Path


ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parents[2]
SEEDS = (20260822, 20260823, 20260824)
ARMS = ("A3", "A3_CLIPPED")
A2_ROOT = PROJECT / "docs/evidence/V35-A2-A3-CAUSAL-AUDIT/local-50k-fixed20/raw/A2"
REWARD = re.compile(
    r"type=reward,.*?action=([^,]+),dom=([^,]+),direction=([^,]+),"
    r"archive=([^,]+),risk=([^,]+),total=([^,]+),"
)


def run_directory(seed: int, arm: str) -> Path:
    base = ROOT / ("preflight" if seed == 20260822 else "runs")
    return base / f"{arm}-seed-{seed}"


def read_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def read_front(path: Path) -> list[tuple[float, float, float]]:
    with path.open(encoding="utf-8", newline="") as handle:
        return [
            (float(row["Cmax"]), float(row["TEC"]), float(row["TWC"]))
            for row in csv.DictReader(handle)
        ]


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def verify_manifest(directory: Path) -> tuple[int, int]:
    checked = bad = 0
    for line in (directory / "evidence-sha256.tsv").read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("sha256\t"):
            continue
        expected, _size, relative = line.split("\t", 2)
        target = directory / relative
        checked += 1
        if not target.is_file() or sha256_file(target).lower() != expected.lower():
            bad += 1
    return checked, bad


def median(values: list[float]) -> float:
    return statistics.median(values) if values else math.nan


def c_metric(left: list[tuple[float, float, float]],
             right: list[tuple[float, float, float]]) -> float:
    if not right:
        return math.nan
    covered = 0
    for target in right:
        if any(all(source[i] <= target[i] for i in range(3)) for source in left):
            covered += 1
    return covered / len(right)


def write_csv(path: Path, rows: list[dict[str, object]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)


def main() -> int:
    helper = PROJECT / "docs/evidence/V35-ND-ARCHIVE/01-offline-cardinality-audit/run_offline_cardinality_audit.py"
    metrics = runpy.run_path(str(helper))
    nondominated = metrics["nondominated"]
    calculate_metrics = metrics["calculate_metrics"]

    fronts: dict[tuple[int, str], list[tuple[float, float, float]]] = {}
    run_rows: list[dict[str, object]] = []
    reward_rows: list[dict[str, object]] = []
    checked_total = bad_total = 0
    for seed in SEEDS:
        for arm in ARMS:
            directory = run_directory(seed, arm)
            status = read_properties(directory / "status.properties")
            qp = read_properties(directory / "qp-summary.properties")
            values = read_front(directory / "front.csv")
            fronts[(seed, arm)] = values
            checked, bad = verify_manifest(directory)
            checked_total += checked
            bad_total += bad
            directions: list[float] = []
            totals: list[float] = []
            for line in (directory / "qp-events.log").read_text(encoding="utf-8").splitlines():
                match = REWARD.search(line)
                if match:
                    directions.append(float(match.group(3)))
                    totals.append(float(match.group(6)))
            reward_rows.append({
                "seed": seed,
                "arm": arm,
                "retainedRewardEvents": len(directions),
                "directionMin": min(directions),
                "directionMedian": median(directions),
                "directionMax": max(directions),
                "directionOutsideMinus1Plus1": sum(abs(value) > 1.0 + 1e-12 for value in directions),
                "directionBelowMinus1e6": sum(value < -1e6 for value in directions),
                "rewardMin": min(totals),
                "rewardMedian": median(totals),
                "rewardMax": max(totals),
            })
            run_rows.append({
                "seed": seed,
                "arm": arm,
                "status": status["status"],
                "fullEvaluations": int(status["fullEvaluations"]),
                "decoderCalls": int(status["decoderCalls"]),
                "illegalSolutions": int(status["illegalSolutions"]),
                "duplicateEvaluations": int(status["duplicateEvaluations"]),
                "initialPopulationHash": status["initialPopulationHash"],
                "frontSize": len(values),
                "minCmax": min(point[0] for point in values),
                "minTEC": min(point[1] for point in values),
                "minTWC": min(point[2] for point in values),
                "qpTableHash": qp.get("tableHash", ""),
                "manifestChecked": checked,
                "manifestFailures": bad,
            })

    if bad_total:
        raise RuntimeError(f"manifest failures: {bad_total}/{checked_total}")
    reference = nondominated(point for values in fronts.values() for point in values)
    with (ROOT / "reference-front.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(("Cmax", "TEC", "TWC"))
        writer.writerows(reference)

    by_key = {(int(row["seed"]), str(row["arm"])): row for row in run_rows}
    paired: list[dict[str, object]] = []
    for seed in SEEDS:
        legacy_front = fronts[(seed, "A3")]
        clipped_front = fronts[(seed, "A3_CLIPPED")]
        legacy_hv, legacy_igd = calculate_metrics(legacy_front, reference)
        clipped_hv, clipped_igd = calculate_metrics(clipped_front, reference)
        legacy = by_key[(seed, "A3")]
        clipped = by_key[(seed, "A3_CLIPPED")]
        paired.append({
            "seed": seed,
            "sameInitialPopulation": legacy["initialPopulationHash"] == clipped["initialPopulationHash"],
            "legacyHV": legacy_hv,
            "clippedHV": clipped_hv,
            "deltaHVRelative": (clipped_hv - legacy_hv) / legacy_hv,
            "legacyIGD": legacy_igd,
            "clippedIGD": clipped_igd,
            "igdImprovement": (legacy_igd - clipped_igd) / legacy_igd,
            "legacyMinCmax": legacy["minCmax"],
            "clippedMinCmax": clipped["minCmax"],
            "cmaxImprovement": (float(legacy["minCmax"]) - float(clipped["minCmax"])) / float(legacy["minCmax"]),
            "legacyMinTEC": legacy["minTEC"],
            "clippedMinTEC": clipped["minTEC"],
            "tecImprovement": (float(legacy["minTEC"]) - float(clipped["minTEC"])) / float(legacy["minTEC"]),
            "legacyMinTWC": legacy["minTWC"],
            "clippedMinTWC": clipped["minTWC"],
            "twcImprovement": (float(legacy["minTWC"]) - float(clipped["minTWC"])) / float(legacy["minTWC"]),
            "C_clipped_legacy": c_metric(clipped_front, legacy_front),
            "C_legacy_clipped": c_metric(legacy_front, clipped_front),
        })

    write_csv(ROOT / "run-summary.csv", run_rows)
    write_csv(ROOT / "qp-reward-summary.csv", reward_rows)
    write_csv(ROOT / "paired-metrics.csv", paired)

    # Secondary causal check: reuse the already verified same-seed A2 control;
    # this does not add training or mix it into the direct single-variable reference.
    a2_fronts = {seed: read_front(A2_ROOT / f"seed-{seed}" / "front.csv") for seed in SEEDS}
    a2_reference = nondominated(
        point
        for seed in SEEDS
        for values in (a2_fronts[seed], fronts[(seed, "A3_CLIPPED")])
        for point in values
    )
    with (ROOT / "a2-clipped-reference-front.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(("Cmax", "TEC", "TWC"))
        writer.writerows(a2_reference)
    a2_clipped: list[dict[str, object]] = []
    for seed in SEEDS:
        a2_status = read_properties(A2_ROOT / f"seed-{seed}" / "status.properties")
        clipped = by_key[(seed, "A3_CLIPPED")]
        a2_values = a2_fronts[seed]
        clipped_values = fronts[(seed, "A3_CLIPPED")]
        a2_hv, a2_igd = calculate_metrics(a2_values, a2_reference)
        clipped_hv, clipped_igd = calculate_metrics(clipped_values, a2_reference)
        a2_cmax = min(point[0] for point in a2_values)
        a2_tec = min(point[1] for point in a2_values)
        a2_twc = min(point[2] for point in a2_values)
        a2_clipped.append({
            "seed": seed,
            "sameInitialPopulation": a2_status["initialPopulationHash"] == clipped["initialPopulationHash"],
            "a2HV": a2_hv,
            "clippedHV": clipped_hv,
            "deltaHVRelative": (clipped_hv - a2_hv) / a2_hv,
            "a2IGD": a2_igd,
            "clippedIGD": clipped_igd,
            "igdImprovement": (a2_igd - clipped_igd) / a2_igd,
            "a2MinCmax": a2_cmax,
            "clippedMinCmax": clipped["minCmax"],
            "cmaxImprovement": (a2_cmax - float(clipped["minCmax"])) / a2_cmax,
            "a2MinTEC": a2_tec,
            "clippedMinTEC": clipped["minTEC"],
            "tecImprovement": (a2_tec - float(clipped["minTEC"])) / a2_tec,
            "a2MinTWC": a2_twc,
            "clippedMinTWC": clipped["minTWC"],
            "twcImprovement": (a2_twc - float(clipped["minTWC"])) / a2_twc,
        })
    write_csv(ROOT / "a2-clipped-paired-metrics.csv", a2_clipped)

    med_hv = median([float(row["deltaHVRelative"]) for row in paired])
    med_igd = median([float(row["igdImprovement"]) for row in paired])
    med_cmax = median([float(row["cmaxImprovement"]) for row in paired])
    med_tec = median([float(row["tecImprovement"]) for row in paired])
    med_twc = median([float(row["twcImprovement"]) for row in paired])
    hv_wins = sum(float(row["deltaHVRelative"]) > 0 for row in paired)
    igd_wins = sum(float(row["igdImprovement"]) > 0 for row in paired)
    clipped_outliers = sum(
        int(row["directionOutsideMinus1Plus1"])
        for row in reward_rows if row["arm"] == "A3_CLIPPED"
    )
    legacy_extreme = min(
        float(row["directionMin"]) for row in reward_rows if row["arm"] == "A3"
    )
    a2_med_hv = median([float(row["deltaHVRelative"]) for row in a2_clipped])
    a2_med_igd = median([float(row["igdImprovement"]) for row in a2_clipped])
    a2_med_cmax = median([float(row["cmaxImprovement"]) for row in a2_clipped])
    report = f"""# V35 Qp方向奖励裁剪单变量诊断报告

## 范围

这是三seed、50k FE的单变量工程诊断，不是DOE，也不是正式统计实验。两臂均为A3；唯一批准差异是旧`LEGACY_UNCLIPPED`与v3.5明文要求的`V35_CLIPPED`方向奖励。

## 正确性结果

- 6条运行全部精确完成50,000次Decoder调用，非法解与重复评价均为0。
- {len(SEEDS)}个同seed配对的初始种群哈希全部一致。
- 旧臂保留奖励事件的最小方向奖励为`{legacy_extreme:.12g}`。
- 裁剪臂落在`[-1,1]`外的保留事件数：`{clipped_outliers}`。
- 统一工程参考前沿点数：`{len(reference)}`。
- 逐运行清单复算文件：`{checked_total}`；失败：`{bad_total}`。

## 配对先导信号（裁剪臂相对旧臂）

- HV相对变化中位数：`{med_hv:.6%}`；胜出：`{hv_wins}/{len(SEEDS)}`。
- IGD改善中位数：`{med_igd:.6%}`；胜出：`{igd_wins}/{len(SEEDS)}`。
- Cmax改善中位数：`{med_cmax:.6%}`。
- TEC改善中位数：`{med_tec:.6%}`。
- TWC改善中位数：`{med_twc:.6%}`。

## A2与纠错A3的二级核对

另用独立`ND(A2 + A3_CLIPPED)`参考集，不与上面的A3单变量参考集混用：

- HV相对变化中位数：`{a2_med_hv:.6%}`。
- IGD改善中位数：`{a2_med_igd:.6%}`。
- Cmax改善中位数：`{a2_med_cmax:.6%}`。

## 裁决边界

数值缺陷已确认，v3.5裁剪实现也已通过机械正确性验收。但本先导门**拒绝晋升**：纠错A3没有保持旧A3信号，并且仍明显弱于A2。该实现保持默认关闭，只供诊断使用。这三组配对不授权替换冻结正式Jar、恢复正式矩阵或形成论文优越性结论。

```text
formulaMismatchConfirmed=true
clippedImplementationValidated=true
promotionDecision=REJECTED_PILOT_REGRESSION
formalJarReplaced=false
formalMatrixRunning=false
```
"""
    (ROOT / "QP_REWARD_CLIP_DIAGNOSTIC_REPORT.md").write_text(report, encoding="utf-8")

    manifest_rows = ["sha256\tbytes\tpath"]
    for path in sorted(ROOT.rglob("*")):
        relative = path.relative_to(ROOT)
        if (path.is_file() and path.name != "evidence-sha256.tsv"
                and not any(part.startswith(".partial-") for part in relative.parts)):
            manifest_rows.append(
                f"{sha256_file(path)}\t{path.stat().st_size}\t{relative.as_posix()}"
            )
    (ROOT / "evidence-sha256.tsv").write_text("\n".join(manifest_rows) + "\n", encoding="utf-8")
    print(f"reference_points={len(reference)}")
    print(f"median_delta_hv={med_hv}")
    print(f"median_igd_improvement={med_igd}")
    print(f"median_cmax_improvement={med_cmax}")
    print(f"a2_clipped_median_delta_hv={a2_med_hv}")
    print(f"a2_clipped_median_igd_improvement={a2_med_igd}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
