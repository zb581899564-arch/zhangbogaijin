#!/usr/bin/env python3
"""Offline, deterministic analysis for the V35-A3-D3 cold-start tie diagnostic."""
from __future__ import annotations

import csv
import hashlib
import math
import statistics
from pathlib import Path

ROOT = Path(__file__).resolve().parent
D1 = ROOT.parent / "V35-A2-A3-DECOMPOSITION" / "04-runs"
Q0 = ROOT.parent / "V35-A3-D2-QP-SETTLEMENT" / "03-q0-runs"
Q1 = ROOT / "02-q1-runs"
OUT = ROOT / "03-analysis"
SEEDS = ("20260822", "20260823", "20260824")
ARMS = ("D1_PA_DIRECTIONAL", "Q0_QP_OBSERVE_ONLY", "Q1_QP_DIRECTIONAL_TIE")
EPS = 1e-12


def sha(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def props(path: Path) -> dict[str, str]:
    result = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            result[key] = value
    return result


def directory(seed: str, arm: str) -> Path:
    if arm == "D1_PA_DIRECTIONAL": return D1 / ("seed-" + seed) / arm
    if arm == "Q0_QP_OBSERVE_ONLY": return Q0 / ("seed-" + seed + "-" + arm)
    return Q1 / ("seed-" + seed + "-" + arm)


def points(path: Path) -> list[tuple[float, float, float]]:
    with path.open(newline="", encoding="utf-8") as stream:
        return [(float(row["Cmax"]), float(row["TEC"]), float(row["TWC"]))
                for row in csv.DictReader(stream)]


def dominates(left, right):
    return all(a <= b for a, b in zip(left, right)) and any(a < b for a, b in zip(left, right))


def unique(values): return list(dict.fromkeys(values))


def nd(values):
    values = unique(values)
    return [value for value in values if not any(dominates(other, value) for other in values if other != value)]


def limits(values):
    return tuple((min(value[i] for value in values), max(value[i] for value in values)) for i in range(3))


def normalized(value, lim):
    return tuple((value[i] - lim[i][0]) / max(lim[i][1] - lim[i][0], EPS) for i in range(3))


def hv(values, lim):
    # Exact 3-D union volume to the fixed normalized reference (1.1,1.1,1.1).
    values = nd(values)
    xs = sorted({normalized(value, lim)[0] for value in values} | {1.1})
    total = 0.0
    for index in range(len(xs) - 1):
        x = xs[index]
        width = xs[index + 1] - x
        active = [normalized(value, lim) for value in values if normalized(value, lim)[0] <= x]
        if not active: continue
        ys = sorted({value[1] for value in active} | {1.1})
        area = 0.0
        for yindex in range(len(ys) - 1):
            y = ys[yindex]
            height = ys[yindex + 1] - y
            zs = [value[2] for value in active if value[1] <= y]
            if zs: area += height * max(0.0, 1.1 - min(zs))
        total += width * area
    return total


def igd(values, reference, lim):
    candidate = [normalized(value, lim) for value in values]
    total = 0.0
    for reference_point in reference:
        point = normalized(reference_point, lim)
        total += min(math.sqrt(sum((a - b) ** 2 for a, b in zip(point, other))) for other in candidate)
    return total / len(reference)


def rel(old, new): return (new - old) / max(abs(old), EPS)


def verify_manifest(path: Path) -> int:
    rows = list(csv.DictReader((path / "evidence-sha256.tsv").open(encoding="utf-8"), delimiter="\t"))
    for row in rows:
        if sha(path / row["path"]) != row["sha256"]:
            raise RuntimeError("manifest mismatch: " + str(path / row["path"]))
    return len(rows)


def write_csv(path: Path, rows) -> None:
    rows = list(rows)
    with path.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0])); writer.writeheader(); writer.writerows(rows)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    records, checks, action_rows = [], [], []
    for seed in SEEDS:
        initial = set()
        for arm in ARMS:
            path = directory(seed, arm)
            status, config, qp = props(path / "status.properties"), props(path / "configuration.txt"), props(path / "qp-summary.properties")
            if status.get("status") != "COMPLETED" or status.get("fullEvaluations") != "50000" or status.get("decoderCalls") != "50000":
                raise RuntimeError("run contract: " + seed + ":" + arm)
            if status.get("illegalSolutions") != "0" or status.get("duplicateEvaluations") != "0":
                raise RuntimeError("safety contract: " + seed + ":" + arm)
            initial.add(status.get("initialPopulationHash"))
            checks.append({"runId": seed + ":" + arm, "manifestFiles": verify_manifest(path), "hashesVerified": "true"})
            record = {"seed": seed, "arm": arm, "path": path, "status": status, "config": config, "qp": qp,
                      "front": points(path / "front.csv")}
            records.append(record)
            if arm.startswith("Q"):
                for action in ("KEEP", "DIRECTIONAL", "EPSILON", "COMPLEMENTARY"):
                    action_rows.append({"seed": seed, "arm": arm, "action": action,
                                        "count": int(qp.get(action + ".count", "0")),
                                        "trainedTransitions": qp.get("trainedTransitions", "legacy"),
                                        "frozenObservations": qp.get("frozenObservations", "legacy"),
                                        "tableNonZeroCells": qp.get("tableNonZeroCells", "legacy")})
        if len(initial) != 1: raise RuntimeError("initial hash mismatch: " + seed)
    q1 = [r for r in records if r["arm"] == "Q1_QP_DIRECTIONAL_TIE"]
    for record in q1:
        if record["config"].get("qp.greedyTiePolicy") != "DIRECTIONAL_IF_TIED":
            raise RuntimeError("missing Q1 tie policy")
        if record["qp"].get("trainedTransitions") != "0" or record["qp"].get("rewardSamples") != "0":
            raise RuntimeError("Q1 unexpectedly learned")

    ref = nd(point for record in records for point in record["front"])
    lim = limits([point for record in records for point in record["front"]])
    with (OUT / "reference-front.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.writer(stream); writer.writerow(("Cmax", "TEC", "TWC")); writer.writerows(sorted(ref))
    metrics, indexed = [], {}
    for record in records:
        front = record["front"]
        row = {"runId": record["seed"] + ":" + record["arm"], "seed": record["seed"], "arm": record["arm"],
               "frontSize": len(front), "minCmax": min(value[0] for value in front),
               "minTEC": min(value[1] for value in front), "minTWC": min(value[2] for value in front),
               "HV": hv(front, lim), "IGD": igd(front, ref, lim), "actualFE": record["status"]["fullEvaluations"],
               "initialPopulationHash": record["status"]["initialPopulationHash"],
               "greedyTiePolicy": record["config"].get("qp.greedyTiePolicy", "FIRST_VALID")}
        metrics.append(row); indexed[(record["seed"], record["arm"])] = row
    write_csv(OUT / "metrics-common-reference.csv", metrics)
    write_csv(OUT / "run-evidence-verification.csv", checks)
    write_csv(OUT / "q-action-distribution.csv", action_rows)

    pairs, outcomes = [], {}
    for old_arm, new_arm in (("Q0_QP_OBSERVE_ONLY", "Q1_QP_DIRECTIONAL_TIE"),
                             ("D1_PA_DIRECTIONAL", "Q1_QP_DIRECTIONAL_TIE")):
        pair = []
        for seed in SEEDS:
            old, new = indexed[(seed, old_arm)], indexed[(seed, new_arm)]
            row = {"pair": old_arm + "->" + new_arm, "seed": seed, "fromArm": old_arm, "toArm": new_arm,
                   "deltaCmax": rel(old["minCmax"], new["minCmax"]), "deltaTEC": rel(old["minTEC"], new["minTEC"]),
                   "deltaTWC": rel(old["minTWC"], new["minTWC"]), "deltaHV": rel(old["HV"], new["HV"]),
                   "deltaIGD": rel(old["IGD"], new["IGD"])}
            pair.append(row); pairs.append(row)
        good = sum(row["deltaHV"] > 0 and row["deltaIGD"] < 0 for row in pair)
        bad = sum(row["deltaHV"] < 0 and row["deltaIGD"] > 0 for row in pair)
        outcomes[(old_arm, new_arm)] = {"good": good, "bad": bad,
            "medianHV": statistics.median(row["deltaHV"] for row in pair),
            "medianIGD": statistics.median(row["deltaIGD"] for row in pair)}
    write_csv(OUT / "pair-metrics.csv", pairs)
    rescue, versus_d1 = outcomes[("Q0_QP_OBSERVE_ONLY", "Q1_QP_DIRECTIONAL_TIE")], outcomes[("D1_PA_DIRECTIONAL", "Q1_QP_DIRECTIONAL_TIE")]
    stable_improvement = rescue["good"] >= 2 and (rescue["medianHV"] >= 0.02 or rescue["medianIGD"] <= -0.10)
    d1_regression = versus_d1["bad"] >= 2 and (versus_d1["medianHV"] <= -0.02 or versus_d1["medianIGD"] >= 0.10)
    verdict = "COLD_START_TIE_BREAK_CAUSAL" if stable_improvement and not d1_regression else (
        "PARTIAL_COLD_START_RESCUE" if stable_improvement else "COLD_START_TIE_BREAK_NOT_CONFIRMED")
    text = ["# V35-A3-D3 Qp冷启动并列策略：裁决", "", "`cold_start_tie_verdict = {}`".format(verdict), "",
            "## 有效性", "", "- 9/9 D1/Q0/Q1记录通过各自文件级SHA-256清单。",
            "- 同seed三臂初始种群哈希一致；所有条目`actualFE=decoderCalls=50000`，无非法解与重复评价。",
            "- Q1已验证为`DIRECTIONAL_IF_TIED`，且`trainedTransitions=rewardSamples=0`。", "",
            "## 预注册对照", ""]
    for key, result in outcomes.items():
        text.append("- {}→{}：good={}/3，bad={}/3，median DeltaHV={:+.4%}，median DeltaIGD={:+.4%}。".format(
            key[0], key[1], result["good"], result["bad"], result["medianHV"], result["medianIGD"]))
    text += ["", "## 边界", "", "本结果只评价零表贪心并列的破平规则；不修改奖励、TD、个人档案、双Q、PDDR、DOE或正式矩阵。详见`pair-metrics.csv`与`q-action-distribution.csv`。"]
    (OUT / "CAUSE_DECISION.md").write_text("\n".join(text) + "\n", encoding="utf-8")
    manifest = [(sha(path), path.stat().st_size, path.relative_to(OUT).as_posix())
                for path in sorted(OUT.rglob("*")) if path.is_file() and path.name != "evidence-sha256.tsv"]
    (OUT / "evidence-sha256.tsv").write_text("sha256\tbytes\tpath\n" + "".join("{}\t{}\t{}\n".format(*row) for row in manifest), encoding="utf-8")
    print(verdict)


if __name__ == "__main__":
    main()
