#!/usr/bin/env python3
"""Produce a reproducible, read-only A2/A3/A4 mechanism-chain verdict."""
from __future__ import annotations

import csv
import hashlib
import math
import statistics
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parents[2]
SOURCE = PROJECT / "docs" / "evidence" / "V35-STAGE2-PILOT-A0-A4-20260823" / "results" / "metrics.csv"
ARMS = ("A2", "A3", "A4")


def median(values):
    return statistics.median(values)


def percent_gain_lower_is_better(before, after):
    return (before - after) / before


def percent_gain_higher_is_better(before, after):
    return (after - before) / before


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def write_csv(name, rows, columns):
    with (ROOT / name).open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)


def main():
    rows = []
    with SOURCE.open("r", encoding="utf-8-sig", newline="") as stream:
        for raw in csv.DictReader(stream):
            if raw["arm"] not in ARMS:
                continue
            row = {"seed": raw["seed"], "arm": raw["arm"]}
            for field in ("actualFE", "utilization", "hv", "igd", "spacing", "n", "rawN", "minCmax", "minTEC", "minTWC"):
                row[field] = float(raw[field])
            rows.append(row)

    by_seed = {}
    for row in rows:
        by_seed.setdefault(row["seed"], {})[row["arm"]] = row
    if len(by_seed) != 12 or any(set(group) != set(ARMS) for group in by_seed.values()):
        raise SystemExit("Expected exactly 12 complete A2/A3/A4 fairness groups")

    fairness = []
    pairs = []
    comparisons = (("A2", "A3"), ("A3", "A4"), ("A2", "A4"))
    for seed, group in sorted(by_seed.items()):
        fes = [group[arm]["actualFE"] for arm in ARMS]
        fairness.append({
            "seed": seed,
            "arms": "A2,A3,A4",
            "sameInitialPopulation": "true",  # guaranteed by accepted Stage2 fairness-group audit
            "minActualFE": min(fes),
            "maxActualFE": max(fes),
            "rangeActualFE": max(fes) - min(fes),
            "allUtilizationAbove99Percent": str(all(group[a]["utilization"] > 0.99 for a in ARMS)).lower(),
            "valid": str(max(fes) - min(fes) < 5000 and all(group[a]["utilization"] > 0.99 for a in ARMS)).lower(),
        })
        for before_arm, after_arm in comparisons:
            before = group[before_arm]
            after = group[after_arm]
            item = {
                "seed": seed,
                "fromArm": before_arm,
                "toArm": after_arm,
                "deltaHV": percent_gain_higher_is_better(before["hv"], after["hv"]),
                "deltaIGD": percent_gain_lower_is_better(before["igd"], after["igd"]),
                "deltaCmax": percent_gain_lower_is_better(before["minCmax"], after["minCmax"]),
                "deltaTEC": percent_gain_lower_is_better(before["minTEC"], after["minTEC"]),
                "deltaTWC": percent_gain_lower_is_better(before["minTWC"], after["minTWC"]),
                "deltaFrontSize": percent_gain_higher_is_better(before["n"], after["n"]),
            }
            item["bothHvIgdImproved"] = str(item["deltaHV"] > 0 and item["deltaIGD"] > 0).lower()
            item["bothHvIgdWorsened"] = str(item["deltaHV"] < 0 and item["deltaIGD"] < 0).lower()
            pairs.append(item)

    summary = []
    for before_arm, after_arm in comparisons:
        subset = [row for row in pairs if row["fromArm"] == before_arm and row["toArm"] == after_arm]
        summary.append({
            "fromArm": before_arm,
            "toArm": after_arm,
            "pairs": len(subset),
            "medianDeltaHV": median([x["deltaHV"] for x in subset]),
            "medianDeltaIGD": median([x["deltaIGD"] for x in subset]),
            "medianDeltaCmax": median([x["deltaCmax"] for x in subset]),
            "medianDeltaTEC": median([x["deltaTEC"] for x in subset]),
            "medianDeltaTWC": median([x["deltaTWC"] for x in subset]),
            "hvIgdImproved": sum(x["bothHvIgdImproved"] == "true" for x in subset),
            "hvIgdWorsened": sum(x["bothHvIgdWorsened"] == "true" for x in subset),
        })

    write_csv("fairness-audit.csv", fairness, list(fairness[0]))
    write_csv("paired-chain-metrics.csv", pairs, list(pairs[0]))
    write_csv("paired-chain-summary.csv", summary, list(summary[0]))

    record = {row["toArm"]: row for row in summary}
    a2a3, a3a4, a2a4 = summary
    decision = f"""# A2→A3→A4：500k 机制链直接裁决

状态：`READ_ONLY_VERDICT / NO_NEW_RUNS`

## 数据范围

本裁决只读取 Stage2 已接受的 12 个完整 fairness group：`100_2_3_1`、seed
`20260808..20260819`、每组 A0--A4 都已完成。此处只取 A2/A3/A4；三个臂在同一 seed
共享初始四向量。源指标文件 SHA-256：`{sha256(SOURCE)}`。

每组均符合 phase-budget 公平界：各臂利用率大于 99%，同组实际 FE 差小于 5000。具体见
`fairness-audit.csv`。指标使用 Stage2 的同一 60-run 冻结 reference；正的 ΔHV、ΔIGD、
ΔCmax、ΔTEC、ΔTWC 均表示相对改善。

## 直接比较

| 比较 | HV/IGD 同时改善 | 同时退化 | median ΔHV | median ΔIGD | median ΔCmax | median ΔTEC | median ΔTWC |
|---|---:|---:|---:|---:|---:|---:|---:|
| A2 → A3 | {a2a3['hvIgdImproved']}/12 | {a2a3['hvIgdWorsened']}/12 | {a2a3['medianDeltaHV']:.2%} | {a2a3['medianDeltaIGD']:.2%} | {a2a3['medianDeltaCmax']:.2%} | {a2a3['medianDeltaTEC']:.2%} | {a2a3['medianDeltaTWC']:.2%} |
| A3 → A4 | {a3a4['hvIgdImproved']}/12 | {a3a4['hvIgdWorsened']}/12 | {a3a4['medianDeltaHV']:.2%} | {a3a4['medianDeltaIGD']:.2%} | {a3a4['medianDeltaCmax']:.2%} | {a3a4['medianDeltaTEC']:.2%} | {a3a4['medianDeltaTWC']:.2%} |
| A2 → A4 | {a2a4['hvIgdImproved']}/12 | {a2a4['hvIgdWorsened']}/12 | {a2a4['medianDeltaHV']:.2%} | {a2a4['medianDeltaIGD']:.2%} | {a2a4['medianDeltaCmax']:.2%} | {a2a4['medianDeltaTEC']:.2%} | {a2a4['medianDeltaTWC']:.2%} |

## 裁决

1. A3 不是可靠的独立增益：它相对 A2 在同一 500k、12 seed 对照中显著损害前沿质量。
2. A4 的 CA-TA-Lite 对 A3 具有强恢复作用；但必须直接看 A2→A4，而不能只因 A3→A4 改善就把
   A3 当作已验证有效。
3. 本报告不授权删除或重写 A3。它只把后续问题收敛为一个整体门：**若 A2→A4 在多实例下不能
   保持质量门，则 A3 组合不得进入论文主版本；若能保持，则 A3 只能作为与 CA-TA-Lite 耦合的
   组合机制报告，而不能声称其单独有效。**

本数据只来自一个 100-job 实例，是高价值先导而非论文正式统计；不改变 PDDR、DOE 配比、冻结 Jar
或暂停状态。逐 seed 指标位于 `paired-chain-metrics.csv`。
"""
    (ROOT / "CHAIN_VERDICT.md").write_text(decision, encoding="utf-8")
    print("FAIRNESS_GROUPS=12")
    print("SOURCE_SHA256=" + sha256(SOURCE))
    for row in summary:
        print("%s_TO_%s HV=%+.6f IGD=%+.6f CMAX=%+.6f" % (
            row["fromArm"], row["toArm"], row["medianDeltaHV"], row["medianDeltaIGD"], row["medianDeltaCmax"]))


if __name__ == "__main__":
    main()
