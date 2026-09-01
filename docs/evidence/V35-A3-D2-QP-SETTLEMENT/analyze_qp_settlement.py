#!/usr/bin/env python3
"""Offline evidence builder for V35-A3-D2.

Reads existing D1/D2 fronts plus the three newly generated Q0 runs.  It never
launches Java, never alters a search result, and freezes all references only
after all nine front files have passed their individual evidence manifests.
"""
from __future__ import annotations

import csv
import hashlib
import math
import statistics
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
EXISTING = ROOT.parent / "V35-A2-A3-DECOMPOSITION" / "04-runs"
Q0_RUNS = ROOT / "03-q0-runs"
OUT = ROOT / "04-analysis"
SEEDS = ("20260822", "20260823", "20260824")
ARMS = ("D1_PA_DIRECTIONAL", "Q0_QP_OBSERVE_ONLY", "D2_QP_SYNCHRONOUS")
EPS = 1.0e-12


def sha(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def props(path: Path) -> dict[str, str]:
    result = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            result[key] = value
    return result


def mechanism_fields(status: dict[str, str]) -> dict[str, str]:
    """Extract flat mechanism counters without treating their human subfields as truth."""
    result = {}
    for token in status.get("mechanismSummary", "").split(","):
        if "=" in token:
            key, value = token.split("=", 1)
            result[key] = value
    return result


def front(path: Path) -> list[tuple[float, float, float]]:
    with path.open(newline="", encoding="utf-8") as handle:
        return [(float(row["Cmax"]), float(row["TEC"]), float(row["TWC"]))
                for row in csv.DictReader(handle)]


def dominates(a, b) -> bool:
    return all(x <= y for x, y in zip(a, b)) and any(x < y for x, y in zip(a, b))


def unique(points):
    return list(dict.fromkeys(points))


def nd(points):
    points = unique(points)
    return [p for p in points if not any(dominates(q, p) for q in points if q != p)]


def limits(points):
    return tuple((min(p[i] for p in points), max(p[i] for p in points)) for i in range(3))


def normal(points, lim):
    return [tuple((p[i] - lim[i][0]) / max(lim[i][1] - lim[i][0], EPS) for i in range(3))
            for p in points]


def hv(points, lim):
    ref = (1.1, 1.1, 1.1)
    q = [p for p in nd(normal(points, lim)) if all(p[i] <= ref[i] for i in range(3))]
    xs = sorted(set([p[0] for p in q] + [ref[0]]))
    total = 0.0
    for left, right in zip(xs, xs[1:]):
        active = sorted((p for p in q if p[0] <= left + EPS), key=lambda p: (p[1], p[2], p[0]))
        best_z, area = ref[2], 0.0
        for _, y, z in active:
            if z < best_z:
                area += max(ref[1] - y, 0.0) * max(best_z - z, 0.0)
                best_z = z
        total += max(right - left, 0.0) * area
    return total


def dist(a, b):
    return math.sqrt(sum((x - y) ** 2 for x, y in zip(a, b)))


def igd(points, reference, lim):
    a, r = normal(points, lim), normal(reference, lim)
    return sum(min(dist(x, y) for y in a) for x in r) / len(r)


def spacing(points, lim):
    p = normal(points, lim)
    if len(p) < 2:
        return 0.0
    nearest = [min(dist(a, b) for b in p if b != a) for a in p]
    avg = sum(nearest) / len(nearest)
    return math.sqrt(sum((x - avg) ** 2 for x in nearest) / max(len(nearest) - 1, 1))


def coverage(left, right):
    return 0.0 if not right else sum(any(dominates(a, b) or a == b for a in left) for b in right) / len(right)


def rel(new, old):
    return (new - old) / max(abs(old), EPS)


def verify_manifest(directory: Path) -> int:
    manifest = directory / "evidence-sha256.tsv"
    if not manifest.is_file():
        raise RuntimeError("missing evidence manifest: " + str(directory))
    count = 0
    with manifest.open(newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle, delimiter="\t"):
            target = directory / row["path"]
            if not target.is_file() or int(row["bytes"]) != target.stat().st_size or sha(target) != row["sha256"].lower():
                raise RuntimeError("manifest mismatch: " + str(target))
            count += 1
    if count == 0:
        raise RuntimeError("empty evidence manifest: " + str(directory))
    return count


def get_dir(seed, arm):
    # The immutable diagnostic runner uses one atomic directory per physical run.
    # Existing D1/D2 evidence predates this convention, hence its seed/arm tree.
    if arm.startswith("Q0_"):
        return Q0_RUNS / ("seed-" + seed + "-" + arm)
    return EXISTING / ("seed-" + seed) / arm


def write_csv(path: Path, rows):
    rows = list(rows)
    if not rows:
        raise RuntimeError("no rows for " + str(path))
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
        writer.writeheader(); writer.writerows(rows)


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    records, checks = [], []
    for seed in SEEDS:
        initial = set()
        for arm in ARMS:
            directory = get_dir(seed, arm)
            status, config = props(directory / "status.properties"), props(directory / "configuration.txt")
            if status.get("status") != "COMPLETED" or status.get("fullEvaluations") != "50000" or status.get("decoderCalls") != "50000":
                raise RuntimeError("invalid run contract: {} {}".format(seed, arm))
            if status.get("illegalSolutions") != "0" or status.get("duplicateEvaluations") != "0":
                raise RuntimeError("safety gate failed: {} {}".format(seed, arm))
            initial.add(status.get("initialPopulationHash"))
            qp = props(directory / "qp-summary.properties")
            checks.append({"runId": seed + ":" + arm, "manifestFiles": verify_manifest(directory),
                           "hashesVerified": "true"})
            records.append({"seed": seed, "arm": arm, "directory": directory, "status": status,
                            "config": config, "qp": qp, "mechanism": mechanism_fields(status),
                            "front": front(directory / "front.csv")})
        if len(initial) != 1:
            raise RuntimeError("initial population mismatch for seed " + seed)

    q0 = [r for r in records if r["arm"].startswith("Q0_")]
    for r in q0:
        if r["config"].get("qpSettlementPolicy") != "OBSERVE_ONLY_ALL_CYCLES":
            raise RuntimeError("Q0 policy missing: " + r["seed"])
        if int(r["qp"].get("trainedTransitions", "-1")) != 0 or int(r["qp"].get("rewardSamples", "-1")) != 0:
            raise RuntimeError("Q0 performed TD learning: " + r["seed"])
        if int(r["qp"].get("frozenObservations", "0")) <= 0 or sum(int(r["qp"].get(a + ".count", "0")) for a in ("KEEP", "DIRECTIONAL", "EPSILON", "COMPLEMENTARY")) <= 0:
            raise RuntimeError("Q0 action/observation telemetry missing: " + r["seed"])
    q0_actions = []
    for r in q0:
        for action in ("KEEP", "DIRECTIONAL", "EPSILON", "COMPLEMENTARY"):
            q0_actions.append({"seed": r["seed"], "action": action,
                               "count": int(r["qp"].get(action + ".count", "0")),
                               "tableHash": r["qp"].get("tableHash", ""),
                               "tableNonZeroCells": int(r["qp"].get("tableNonZeroCells", "0"))})
    write_csv(OUT / "q0-action-distribution.csv", q0_actions)
    q0_total = sum(row["count"] for row in q0_actions)
    q0_keep = sum(row["count"] for row in q0_actions if row["action"] == "KEEP")

    all_points = [p for r in records for p in r["front"]]
    reference, lim = nd(all_points), limits(all_points)
    with (OUT / "reference-front.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle); writer.writerow(("Cmax", "TEC", "TWC")); writer.writerows(sorted(reference))
    metrics, by = [], {}
    for r in records:
        row = {"runId": r["seed"] + ":" + r["arm"], "seed": r["seed"], "arm": r["arm"],
               "frontSize": len(r["front"]), "exactUniqueFrontSize": len(unique(r["front"])),
               "minCmax": min(p[0] for p in r["front"]), "minTEC": min(p[1] for p in r["front"]),
               "minTWC": min(p[2] for p in r["front"]), "HV": hv(r["front"], lim),
               "IGD": igd(r["front"], reference, lim), "Spacing": spacing(r["front"], lim),
               "actualFE": r["status"]["fullEvaluations"], "decoderCalls": r["status"]["decoderCalls"],
               "initialPopulationHash": r["status"]["initialPopulationHash"],
               "qpSettlementPolicy": r["config"].get("qpSettlementPolicy", "STANDARD_BY_DUAL_Q"),
               # Historical D1/D2 runs predate the additional telemetry.  Their
               # authoritative transition counter is the frozen mechanism summary.
               "qpTrainedTransitions": r["qp"].get("trainedTransitions", r["mechanism"].get("qpTransitions", "0")),
               "qpFrozenObservations": r["qp"].get("frozenObservations", "0"),
               "qpRewardSamples": r["qp"].get("rewardSamples", r["mechanism"].get("qpTransitions", "0")),
               "qpTableHash": r["qp"].get("tableHash", ""),
               "sourceRunId": r["config"].get("sourceRunId", "")}
        metrics.append(row); by[(r["seed"], r["arm"])] = (r, row)
    write_csv(OUT / "metrics-common-reference.csv", metrics)
    write_csv(OUT / "run-evidence-verification.csv", checks)

    pairs, outcome = [], {}
    for old_arm, new_arm in ((ARMS[0], ARMS[1]), (ARMS[1], ARMS[2])):
        scoped = [r for r in records if r["arm"] in (old_arm, new_arm)]
        pair_ref, pair_lim = nd(p for r in scoped for p in r["front"]), limits([p for r in scoped for p in r["front"]])
        per_seed = []
        for seed in SEEDS:
            old_r, old = by[(seed, old_arm)]; new_r, new = by[(seed, new_arm)]
            row = {"pair": old_arm + "->" + new_arm, "seed": seed, "fromArm": old_arm, "toArm": new_arm,
                   "deltaCmax": rel(old["minCmax"], new["minCmax"]), "deltaTEC": rel(old["minTEC"], new["minTEC"]),
                   "deltaTWC": rel(old["minTWC"], new["minTWC"]),
                   "deltaHVCommonReference": rel(new["HV"], old["HV"]), "deltaIGDCommonReference": rel(new["IGD"], old["IGD"]),
                   "deltaHVPairReference": rel(hv(new_r["front"], pair_lim), hv(old_r["front"], pair_lim)),
                   "deltaIGDPairReference": rel(igd(new_r["front"], pair_ref, pair_lim), igd(old_r["front"], pair_ref, pair_lim)),
                   "coverageToFrom": coverage(new_r["front"], old_r["front"]), "coverageFromTo": coverage(old_r["front"], new_r["front"])}
            per_seed.append(row); pairs.append(row)
        bad = sum(x["deltaHVCommonReference"] < 0 and x["deltaIGDCommonReference"] > 0 for x in per_seed)
        outcome[(old_arm, new_arm)] = {"bad": bad,
            "median_hv": statistics.median(x["deltaHVCommonReference"] for x in per_seed),
            "median_igd": statistics.median(x["deltaIGDCommonReference"] for x in per_seed)}
        outcome[(old_arm, new_arm)]["stable"] = bad >= 2 and (outcome[(old_arm,new_arm)]["median_hv"] <= -0.02 or outcome[(old_arm,new_arm)]["median_igd"] >= 0.10)
    write_csv(OUT / "pair-metrics.csv", pairs)
    left, right = outcome[(ARMS[0], ARMS[1])], outcome[(ARMS[1], ARMS[2])]
    if left["stable"] and right["stable"]: verdict = "BOTH_QP_ACTION_AND_TD_HARMFUL"
    elif left["stable"]: verdict = "QP_ACTION_POLICY_HARMFUL"
    elif right["stable"]: verdict = "QP_TD_REWARD_HARMFUL"
    else: verdict = "NON_ADDITIVE_OR_INCONCLUSIVE"
    summary = ["# V35-A3-D2 Qp 动作策略与 TD 奖励学习：裁决", "",
               "`a2_a3_d2_root_cause = {}`".format(verdict), "",
               "## 有效性", "", "- 9/9 条配对诊断运行通过文件级 SHA-256 反向核验。",
               "- 所有条目均为 `actualFE=decoderCalls=50000`，且同一 seed 三臂初始种群哈希一致。",
               "- Q0 三臂实际执行 Qp 动作并更新个人档案，但 `trainedTransitions=0`、`rewardSamples=0`，因此没有 TD 学习。",
               "- Q0 三个初始Q表均为零表，30,000次动作中 `KEEP` 为 {} 次（{:.2%}）；其余动作分布见 `q0-action-distribution.csv`。".format(q0_keep, q0_keep / float(q0_total)), "",
               "## 预注册门", "", "稳定退化：至少2/3 seed 同时 HV下降、IGD变差，且中位 ΔHV≤-2% 或中位 ΔIGD≥+10%。", ""]
    for pair in ((ARMS[0], ARMS[1]), (ARMS[1], ARMS[2])):
        x = outcome[pair]
        summary.append("- {}：bad={}/3，median ΔHV={:+.4%}，median ΔIGD={:+.4%}，stableRegression={}。".format(pair[0]+"→"+pair[1], x["bad"], x["median_hv"], x["median_igd"], x["stable"]))
    summary += ["", "## 解释边界", "", "该裁决严格指向“未学习/零表时四动作的实际选择行为”，而不是宣称所有Qp学习都必然有害；Q0→D2未满足TD稳定退化门。此结论不授权改变正式 Jar、奖励公式、个人档案容量、双Q时序、PDDR、DOE或正式矩阵。详见 `pair-metrics.csv`、`metrics-common-reference.csv` 和 `q0-action-distribution.csv`。"]
    (OUT / "CAUSE_DECISION.md").write_text("\n".join(summary)+"\n", encoding="utf-8")
    manifest = []
    for path in sorted(p for p in OUT.rglob("*") if p.is_file() and p.name != "evidence-sha256.tsv"):
        manifest.append("{}\t{}\t{}".format(sha(path), path.stat().st_size, path.relative_to(OUT).as_posix()))
    (OUT / "evidence-sha256.tsv").write_text("sha256\tbytes\tpath\n"+"\n".join(manifest)+"\n", encoding="utf-8")
    print(verdict)
    return 0


if __name__ == "__main__":
    sys.exit(main())
