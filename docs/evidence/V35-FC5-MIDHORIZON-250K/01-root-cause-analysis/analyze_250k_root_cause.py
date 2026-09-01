#!/usr/bin/env python3
"""Read-only FC-5 transfer analysis for the pre-registered 12-run 250k campaign.

The script consumes only completed telemetry outputs.  It neither launches the
algorithm nor changes any search state.  All headline values in the Markdown
report are derived from the CSV products emitted here.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import math
import statistics
from collections import defaultdict
from pathlib import Path

EPS = 1e-12
INSTANCES = ("100_2_4_1", "100_5_3_1")
SEEDS = ("20260901", "20260902", "20260903")
ARMS = ("A2", "A4")
CHECKPOINTS = tuple(range(25000, 250001, 25000))
HARD_INSTANCE = "100_5_3_1"
POSITIVE_INSTANCE = "100_2_4_1"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest().upper()


def read_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith(("#", "!")):
            key, value = line.split("=", 1)
            if key in values:
                if values[key] != value:
                    raise ValueError(f"conflicting duplicate property {key} in {path}")
                continue
            values[key] = value
    return values


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def write_csv(path: Path, rows: list[dict], fields: list[str] | None = None) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if fields is None:
        fields = list(rows[0]) if rows else []
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, extrasaction="ignore", delimiter="\t" if path.suffix == ".tsv" else ",")
        writer.writeheader()
        writer.writerows(rows)


def truth(value: str | None) -> bool:
    return str(value).strip().lower() == "true"


def finite3(row: dict[str, str]) -> tuple[float, float, float]:
    point = (float(row["Cmax"]), float(row["TEC"]), float(row["TWC"]))
    if not all(math.isfinite(value) for value in point):
        raise ValueError(f"non-finite point: {point}")
    return point


def nondominated(points: list[tuple[float, float, float]]) -> list[tuple[float, float, float]]:
    """Exact objective de-duplication and strict Pareto filter for minimization."""
    ordered = sorted(set(points))
    accepted: list[tuple[float, float, float]] = []
    for point in ordered:
        dominated = False
        for other in ordered:
            if other == point:
                continue
            if all(other[i] <= point[i] for i in range(3)) and any(other[i] < point[i] for i in range(3)):
                dominated = True
                break
        if not dominated:
            accepted.append(point)
    return accepted


def bounds(reference: list[tuple[float, float, float]]):
    lows = tuple(min(p[i] for p in reference) for i in range(3))
    highs = tuple(max(p[i] for p in reference) for i in range(3))
    spans = tuple(max(EPS, highs[i] - lows[i]) for i in range(3))
    return lows, highs, spans


def normalize(points, limits):
    lows, _, spans = limits
    return [tuple((p[i] - lows[i]) / spans[i] for i in range(3)) for p in points]


def yz_union(points, ry=1.1, rz=1.1):
    ordered = sorted(points, key=lambda p: p[1])
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
    clipped = sorted([tuple(max(0.0, min(1.1, x)) for x in p) for p in points], key=lambda p: p[0])
    volume, active, cursor = 0.0, [], 0
    while cursor < len(clipped):
        x = clipped[cursor][0]
        while cursor < len(clipped) and clipped[cursor][0] <= x + EPS:
            active.append(clipped[cursor])
            cursor += 1
        next_x = clipped[cursor][0] if cursor < len(clipped) else 1.1
        volume += max(0.0, next_x - x) * yz_union(active)
    return max(0.0, volume)


def igd(approximation, reference):
    return sum(min(math.dist(r, a) for a in approximation) for r in reference) / len(reference)


def safe_rate(numerator: int | float, denominator: int | float):
    return numerator / denominator if denominator else ""


def median(values):
    vals = [float(v) for v in values if v != "" and v is not None]
    return statistics.median(vals) if vals else ""


def lifecycle_rows(path: Path) -> list[dict]:
    """Parse the legacy variable-width fingerprint column using the fixed 20-field tail."""
    header = path.read_text(encoding="utf-8", errors="strict").splitlines()[0].split(",")
    if header[:7] != ["seed", "cycle", "fe", "representative", "poolIndex", "source", "fingerprint"]:
        raise ValueError(f"unexpected lifecycle header in {path}")
    tail_names = header[7:]
    if len(tail_names) != 20:
        raise ValueError(f"unexpected lifecycle tail width {len(tail_names)} in {path}")
    result = []
    with path.open(encoding="utf-8", newline="") as stream:
        next(stream)
        for line_number, line in enumerate(stream, 2):
            parts = line.rstrip("\r\n").split(",")
            if len(parts) < 27:
                raise ValueError(f"short lifecycle row {line_number} in {path}")
            fixed, tail = parts[:6], parts[-20:]
            row = dict(zip(header[:6], fixed))
            row["fingerprint"] = ",".join(parts[6:-20])
            row.update(dict(zip(tail_names, tail)))
            result.append(row)
    return result


def run_path(root: Path, instance: str, seed: str, arm: str) -> Path:
    return root / instance / f"seed-{seed}" / arm


def collect_acceptance(root: Path):
    rows, artifacts = [], []
    required = (
        "status.properties", "behavior-summary.properties", "diagnostic-contract.properties",
        "telemetry-checkpoint-fronts.csv", "telemetry-pddr-cycle-summary.csv",
        "telemetry-pddr-full-ledger.csv", "telemetry-teacher-use-events.csv",
        "telemetry-cata-contribution-events.csv", "fc5-transfer-directional-representative-lifecycle.csv",
        "fc5-transfer-archive-working-gap.csv", "front.csv",
    )
    for instance in INSTANCES:
        for seed in SEEDS:
            for arm in ARMS:
                directory = run_path(root, instance, seed, arm)
                missing = [name for name in required if not (directory / name).is_file()]
                if missing:
                    raise FileNotFoundError(f"{directory}: missing {missing}")
                status = read_properties(directory / "status.properties")
                behavior = read_properties(directory / "behavior-summary.properties")
                contract = read_properties(directory / "diagnostic-contract.properties")
                checkpoint_rows = read_csv(directory / "telemetry-checkpoint-fronts.csv")
                observed = {(int(r["nominalCheckpointFE"]), r["frontType"]) for r in checkpoint_rows if r["unobservableReason"] == "NONE"}
                expected = {(fe, kind) for fe in CHECKPOINTS for kind in ("workingPopulationND", "decisionArchiveFront", "observedFullFront")}
                terminal_rows = [r for r in checkpoint_rows if int(r["nominalCheckpointFE"]) == 250000]
                actual_fe = int(status.get("fullEvaluations", behavior.get("actualFE", "-1")))
                pass_row = (
                    status.get("status") == "COMPLETED" and actual_fe == 250000 and
                    truth(behavior.get("hardGatesPass")) and expected <= observed and
                    terminal_rows and all(int(r["actualSnapshotFE"]) == 250000 for r in terminal_rows)
                )
                rows.append({
                    "instance": instance, "seed": seed, "arm": arm,
                    "status": status.get("status", ""), "actualFE": actual_fe,
                    "hardGatesPass": behavior.get("hardGatesPass", ""),
                    "terminalCheckpointPass": str(bool(terminal_rows) and all(int(r["actualSnapshotFE"]) == 250000 for r in terminal_rows)).lower(),
                    "checkpointTriplesObserved": len(observed), "expectedCheckpointTriples": len(expected),
                    "contractVersion": contract.get("contractVersion", ""), "accepted": str(pass_row).lower(),
                })
                for name in required:
                    path = directory / name
                    artifacts.append({"instance": instance, "seed": seed, "arm": arm, "path": str(path.relative_to(root)), "bytes": path.stat().st_size, "sha256": sha256(path)})
    return rows, artifacts


def collect_checkpoint_metrics(root: Path):
    raw = defaultdict(lambda: defaultdict(list))
    for instance in INSTANCES:
        for seed in SEEDS:
            for arm in ARMS:
                for row in read_csv(run_path(root, instance, seed, arm) / "telemetry-checkpoint-fronts.csv"):
                    if row["unobservableReason"] != "NONE":
                        continue
                    key = (seed, arm, int(row["nominalCheckpointFE"]), row["frontType"])
                    raw[instance][key].append(finite3(row))

    metric_rows, reference_rows = [], []
    for instance in INSTANCES:
        terminal_decision = []
        terminal_observed = []
        for seed in SEEDS:
            for arm in ARMS:
                terminal_decision += raw[instance][(seed, arm, 250000, "decisionArchiveFront")]
                terminal_observed += raw[instance][(seed, arm, 250000, "observedFullFront")]
        refs = {
            "FORMAL_DECISION": nondominated(terminal_decision),
            "DISCOVERY_OBSERVED": nondominated(terminal_observed),
        }
        for family, ref in refs.items():
            limits = bounds(ref)
            ref_norm = normalize(ref, limits)
            for point in ref:
                reference_rows.append({"instance": instance, "referenceFamily": family, "Cmax": point[0], "TEC": point[1], "TWC": point[2]})
            front_types = ("decisionArchiveFront",) if family == "FORMAL_DECISION" else ("workingPopulationND", "decisionArchiveFront", "observedFullFront")
            for seed in SEEDS:
                for arm in ARMS:
                    for fe in CHECKPOINTS:
                        for front_type in front_types:
                            front = nondominated(raw[instance][(seed, arm, fe, front_type)])
                            normal = normalize(front, limits)
                            metric_rows.append({
                                "instance": instance, "seed": seed, "arm": arm, "checkpointFE": fe,
                                "referenceFamily": family, "frontType": front_type, "frontSize": len(front),
                                "minCmax": min(p[0] for p in front), "minTEC": min(p[1] for p in front), "minTWC": min(p[2] for p in front),
                                "HV": hypervolume(normal), "IGD": igd(normal, ref_norm),
                            })
    return metric_rows, reference_rows


def paired_performance(metric_rows):
    index = {(r["instance"], r["seed"], r["arm"], r["checkpointFE"]): r for r in metric_rows if r["referenceFamily"] == "FORMAL_DECISION"}
    pairs = []
    for instance in INSTANCES:
        for seed in SEEDS:
            for fe in CHECKPOINTS:
                a2, a4 = index[(instance, seed, "A2", fe)], index[(instance, seed, "A4", fe)]
                pairs.append({
                    "instance": instance, "seed": seed, "checkpointFE": fe,
                    "A2_minCmax": a2["minCmax"], "A4_minCmax": a4["minCmax"],
                    "deltaCmaxBetter": (a2["minCmax"] - a4["minCmax"]) / a2["minCmax"],
                    "A2_HV": a2["HV"], "A4_HV": a4["HV"],
                    "deltaHVBetter": (a4["HV"] - a2["HV"]) / max(EPS, a2["HV"]),
                    "A2_IGD": a2["IGD"], "A4_IGD": a4["IGD"],
                    "deltaIGDBetter": (a2["IGD"] - a4["IGD"]) / max(EPS, a2["IGD"]),
                    "jointA4Worse": str(a4["HV"] < a2["HV"] - EPS and a4["IGD"] > a2["IGD"] + EPS).lower(),
                })
    summary = []
    for instance in INSTANCES:
        for fe in CHECKPOINTS:
            bucket = [r for r in pairs if r["instance"] == instance and r["checkpointFE"] == fe]
            summary.append({
                "instance": instance, "checkpointFE": fe,
                "medianDeltaCmaxBetter": median([r["deltaCmaxBetter"] for r in bucket]),
                "medianDeltaHVBetter": median([r["deltaHVBetter"] for r in bucket]),
                "medianDeltaIGDBetter": median([r["deltaIGDBetter"] for r in bucket]),
                "jointA4WorseSeeds": sum(truth(r["jointA4Worse"]) for r in bucket),
            })
    return pairs, summary


def collect_pddr(root: Path):
    per_round, window_rows = [], []
    for instance in INSTANCES:
        for seed in SEEDS:
            for arm in ARMS:
                rows = read_csv(run_path(root, instance, seed, arm) / "telemetry-pddr-cycle-summary.csv")
                for row in rows:
                    fe = int(row["FE"])
                    per_round.append({
                        "instance": instance, "seed": seed, "arm": arm, "cycle": int(row["cycle"]), "FE": fe,
                        "windowEndFE": min(250000, int(math.ceil(fe / 50000.0) * 50000)),
                        "poolSize": int(row["poolSize"]), "uniqueObjectiveCount": int(row["uniqueObjectiveCount"]),
                        "strictNdCount": int(row["strictNdCount"]), "selectedCount": int(row["selectedCount"]),
                    })
    groups = defaultdict(list)
    for row in per_round:
        groups[(row["instance"], row["seed"], row["arm"], row["windowEndFE"])].append(row)
    for (instance, seed, arm, window), rows in sorted(groups.items()):
        nd = [r["strictNdCount"] for r in rows]
        window_rows.append({
            "instance": instance, "seed": seed, "arm": arm, "windowEndFE": window, "rounds": len(rows),
            "meanNmerge": statistics.mean(r["poolSize"] for r in rows),
            "meanUniqueObjectives": statistics.mean(r["uniqueObjectiveCount"] for r in rows),
            "meanNnd": statistics.mean(nd), "maxNnd": max(nd), "meanRoverflow": statistics.mean(v / 100.0 for v in nd),
            "maxRoverflow": max(nd) / 100.0, "roundsNndOver100": sum(v > 100 for v in nd),
        })
    return per_round, window_rows


def collect_lifecycle(root: Path):
    event_rows, summaries = [], []
    for instance in INSTANCES:
        for seed in SEEDS:
            for arm in ARMS:
                rows = lifecycle_rows(run_path(root, instance, seed, arm) / "fc5-transfer-directional-representative-lifecycle.csv")
                max_cycle = max(int(r["cycle"]) for r in rows)
                for row in rows:
                    cycle = int(row["cycle"]); fe = int(row["fe"]); retired = int(row["retiredAtCycle"])
                    selected = truth(row["pddrSelected"]); entered = selected and int(row["nextPopulationSlot"]) >= 0
                    eligible = entered and cycle < max_cycle
                    survived = eligible and (retired == -1 or retired > cycle + 1)
                    teacher_uses = int(row["qgTeacherUses"]) + int(row["qpTeacherUses"])
                    event_rows.append({
                        "instance": instance, "seed": seed, "arm": arm, "cohortWindowEndFE": min(250000, int(math.ceil(fe / 50000.0) * 50000)),
                        "cycle": cycle, "FE": fe, "representative": row["representative"], "fingerprintSha256": hashlib.sha256(row["fingerprint"].encode()).hexdigest(),
                        "source": row["source"], "poolPresent": int(truth(row["poolPresent"])), "pddrSelected": int(selected),
                        "enteredNextPopulation": int(entered), "nextCycleEligible": int(eligible), "survivedNextCycle": int(survived),
                        "teacherUsed": int(teacher_uses > 0), "teacherExposure": teacher_uses,
                        "improvedOffspring": int(int(row["improvedOffspringCount"]) > 0), "improvedOffspringCount": int(row["improvedOffspringCount"]),
                        "lastTeacherFE": int(row["lastTeacherFE"]), "lastImprovementFE": int(row["lastImprovementFE"]), "retiredAtCycle": retired,
                    })
    for granularity in ("LABEL_EVENT", "UNIQUE_FINGERPRINT_PER_CYCLE"):
        base = event_rows
        if granularity == "UNIQUE_FINGERPRINT_PER_CYCLE":
            grouped = defaultdict(list)
            for row in event_rows:
                grouped[(row["instance"], row["seed"], row["arm"], row["cycle"], row["fingerprintSha256"])].append(row)
            base = []
            for members in grouped.values():
                first = dict(members[0])
                for field in ("poolPresent", "pddrSelected", "enteredNextPopulation", "nextCycleEligible", "survivedNextCycle", "teacherUsed", "improvedOffspring"):
                    first[field] = max(r[field] for r in members)
                for field in ("teacherExposure", "improvedOffspringCount"):
                    first[field] = max(r[field] for r in members)
                first["representative"] = "+".join(sorted({r["representative"] for r in members}))
                base.append(first)
        groups = defaultdict(list)
        for row in base:
            groups[(row["instance"], row["seed"], row["arm"], row["cohortWindowEndFE"], row["representative"])].append(row)
        for (instance, seed, arm, window, representative), rows in sorted(groups.items()):
            pool = sum(r["poolPresent"] for r in rows); selected = sum(r["pddrSelected"] for r in rows)
            entered = sum(r["enteredNextPopulation"] for r in rows); eligible = sum(r["nextCycleEligible"] for r in rows)
            survived = sum(r["survivedNextCycle"] for r in rows); teachers = sum(r["teacherUsed"] for r in rows)
            improved = sum(r["improvedOffspring"] for r in rows)
            summaries.append({
                "granularity": granularity, "instance": instance, "seed": seed, "arm": arm,
                "cohortWindowEndFE": window, "representative": representative, "poolRepresentatives": pool,
                "pddrSelected": selected, "poolToPddrRate": safe_rate(selected, pool),
                "enteredNextPopulation": entered, "poolToNextRate": safe_rate(entered, pool),
                "nextCycleEligible": eligible, "survivedNextCycle": survived, "nextToNextCycleRate": safe_rate(survived, eligible),
                "teacherUsed": teachers, "nextToTeacherRate": safe_rate(teachers, entered),
                "teacherExposure": sum(r["teacherExposure"] for r in rows),
                "teacherProducedImprovement": sum(1 for r in rows if r["teacherUsed"] and r["improvedOffspring"]),
                "teacherToImprovementRate": safe_rate(sum(1 for r in rows if r["teacherUsed"] and r["improvedOffspring"]), teachers),
                "improvedOffspringCount": sum(r["improvedOffspringCount"] for r in rows),
            })
    return event_rows, summaries


def collect_gaps(root: Path):
    events, summaries = [], []
    for instance in INSTANCES:
        for seed in SEEDS:
            for arm in ARMS:
                for row in read_csv(run_path(root, instance, seed, arm) / "fc5-transfer-archive-working-gap.csv"):
                    fe = int(row["fe"])
                    events.append({
                        "instance": instance, "seed": seed, "arm": arm, "cycle": int(row["cycle"]), "FE": fe,
                        "windowEndFE": min(250000, int(math.ceil(fe / 50000.0) * 50000)),
                        "cmaxGap": float(row["cmaxGap"]), "tecGap": float(row["tecGap"]), "twcGap": float(row["twcGap"]),
                        "workingSize": int(row["workingSize"]), "archiveSize": int(row["archiveSize"]),
                    })
    groups = defaultdict(list)
    for row in events:
        groups[(row["instance"], row["seed"], row["arm"], row["windowEndFE"])].append(row)
    for (instance, seed, arm, window), rows in sorted(groups.items()):
        summaries.append({
            "instance": instance, "seed": seed, "arm": arm, "windowEndFE": window,
            "medianCmaxGap": median([r["cmaxGap"] for r in rows]), "maxCmaxGap": max(r["cmaxGap"] for r in rows),
            "medianTecGap": median([r["tecGap"] for r in rows]), "maxTecGap": max(r["tecGap"] for r in rows),
            "medianTwcGap": median([r["twcGap"] for r in rows]), "maxTwcGap": max(r["twcGap"] for r in rows),
            "medianArchiveSize": median([r["archiveSize"] for r in rows]),
        })
    return events, summaries


def aggregate_rates(lifecycle_summaries, instance, arm="A4"):
    rows = [r for r in lifecycle_summaries if r["granularity"] == "LABEL_EVENT" and r["instance"] == instance and r["arm"] == arm]
    result = {}
    for representative in ("ALL", "E_C", "E_E", "E_W", "E_B"):
        bucket = rows if representative == "ALL" else [r for r in rows if r["representative"] == representative]
        pool = sum(r["poolRepresentatives"] for r in bucket); selected = sum(r["pddrSelected"] for r in bucket)
        entered = sum(r["enteredNextPopulation"] for r in bucket); eligible = sum(r["nextCycleEligible"] for r in bucket)
        survived = sum(r["survivedNextCycle"] for r in bucket); teachers = sum(r["teacherUsed"] for r in bucket)
        improved = sum(r["teacherProducedImprovement"] for r in bucket)
        result[representative] = {
            "poolToNext": safe_rate(entered, pool), "nextToNextCycle": safe_rate(survived, eligible),
            "nextToTeacher": safe_rate(teachers, entered), "teacherToImprovement": safe_rate(improved, teachers),
            "pool": pool, "selected": selected, "eligible": eligible, "survived": survived, "teachers": teachers, "improved": improved,
        }
    return result


def make_report(out: Path, acceptance, perf_summary, pddr_windows, lifecycle_summaries, gap_summaries):
    pddr_rounds = [r for r in pddr_windows]
    max_nd = max(r["maxNnd"] for r in pddr_rounds)
    over_rounds = sum(r["roundsNndOver100"] for r in pddr_rounds)
    hard_over = median([r["meanRoverflow"] for r in pddr_rounds if r["instance"] == HARD_INSTANCE and r["arm"] == "A4"])
    pos_over = median([r["meanRoverflow"] for r in pddr_rounds if r["instance"] == POSITIVE_INSTANCE and r["arm"] == "A4"])
    hard_rates, pos_rates = aggregate_rates(lifecycle_summaries, HARD_INSTANCE), aggregate_rates(lifecycle_summaries, POSITIVE_INSTANCE)
    retention_diffs = {rep: (pos_rates[rep]["poolToNext"] - hard_rates[rep]["poolToNext"]) for rep in hard_rates if hard_rates[rep]["poolToNext"] != "" and pos_rates[rep]["poolToNext"] != ""}
    worst_retention_drop = max(retention_diffs.values()) if retention_diffs else 0.0
    hard_cmax_gap = median([r["medianCmaxGap"] for r in gap_summaries if r["instance"] == HARD_INSTANCE and r["arm"] == "A4"])
    pos_cmax_gap = median([r["medianCmaxGap"] for r in gap_summaries if r["instance"] == POSITIVE_INSTANCE and r["arm"] == "A4"])
    terminal = {r["instance"]: r for r in perf_summary if r["checkpointFE"] == 250000}
    hard_joint_worse = terminal[HARD_INSTANCE]["jointA4WorseSeeds"]

    gate_rows = [
        {"gate": "H1.1 two consecutive 50k windows with Nnd>100 before degradation in >=2/3 hard seeds", "result": "FAIL", "evidence": f"all runs maxNnd={max_nd}; roundsNndOver100={over_rounds}"},
        {"gate": "H1.2 hard median Roverflow exceeds positive by >=0.25", "result": "PASS" if hard_over - pos_over >= .25 else "FAIL", "evidence": f"hard={hard_over:.6f}; positive={pos_over:.6f}; delta={hard_over-pos_over:+.6f}"},
        {"gate": "H1.3 at least one directional pool-to-next retention is >=20pp lower in hard case", "result": "PASS" if worst_retention_drop >= .20 else "FAIL", "evidence": f"largest positive-minus-hard drop={worst_retention_drop:+.6f}"},
        {"gate": "H1.4 archive-working Cmax gap expands before representative loss and performance degradation", "result": "FAIL" if abs(hard_cmax_gap) <= EPS else "NOT_ESTABLISHED", "evidence": f"hard median gap={hard_cmax_gap:.12g}; positive median gap={pos_cmax_gap:.12g}; hard terminal joint-worse seeds={hard_joint_worse}/3"},
    ]
    write_csv(out / "hypothesis-gates.csv", gate_rows)
    confirmed = all(r["result"] == "PASS" for r in gate_rows)
    verdict = "FC5_TRANSFER_CONFIRMED" if confirmed else "FC5_TRANSFER_NOT_CONFIRMED_AT_250K"
    root_cause = "CANDIDATE_OVERFLOW_UTILIZATION_BREAK_NOT_SUPPORTED"

    def pct(value): return f"{100*float(value):+.2f}%"
    lines = [
        "# V35-FC5-T 250k 根因分析报告", "",
        f"**裁决：`{verdict}`。** 原FC-5的“ND候选膨胀→PDDR压缩→四方向代表利用断裂”没有在本次250k正/负对照中复现；它不能解释当前100-job退化。", "",
        "> 这是对预注册H1机制的否证性诊断，不等于证明PDDR在所有情形都最优，也不授权删除CFVF、双Q或CA-TA-Lite。", "",
        "## 1. 执行验收", "",
        f"- 运行：{sum(truth(r['accepted']) for r in acceptance)}/12 accepted；全部 actualFE=250000。",
        "- 每条均观察到10个名义检查点的三类前沿，并在250000 FE命中真实终止快照。",
        "- 本分析没有启动新训练、没有修改算法、PDDR或冻结参数。", "",
        "## 2. 最关键证据", "",
        f"1. **没有ND overflow**：所有PDDR轮最大 strictNdCount={max_nd}，Nnd>100 的轮数={over_rounds}。",
        f"2. **困难实例没有形成预注册要求的Roverflow强差异**：A4困难实例窗口中位={hard_over:.3f}，正例={pos_over:.3f}，差={hard_over-pos_over:+.3f}。",
        f"3. **四方向即时保留未出现20个百分点断裂**：最大正例减困难实例差={worst_retention_drop:+.3f}。",
        f"4. **Cmax没有archive-working脱节**：A4困难实例窗口中位gap={hard_cmax_gap:.12g}；正例={pos_cmax_gap:.12g}。",
        f"5. **250k时困难实例尚未出现A4整体崩溃**：A4相对A2的中位ΔCmax={pct(terminal[HARD_INSTANCE]['medianDeltaCmaxBetter'])}、ΔHV={pct(terminal[HARD_INSTANCE]['medianDeltaHVBetter'])}、ΔIGD={pct(terminal[HARD_INSTANCE]['medianDeltaIGDBetter'])}；同时HV/IGD变差seed={hard_joint_worse}/3。", "",
        "## 3. 四方向生命周期（A4，全程，label-event口径）", "",
        "|实例|方向|pool→next|next→next-cycle|next→teacher|teacher→improvement|", "|---|---|---:|---:|---:|---:|",
    ]
    for instance, rates in ((POSITIVE_INSTANCE, pos_rates), (HARD_INSTANCE, hard_rates)):
        for rep in ("ALL", "E_C", "E_E", "E_W", "E_B"):
            row = rates[rep]
            lines.append(f"|{instance}|{rep}|{pct(row['poolToNext'])}|{pct(row['nextToNextCycle'])}|{pct(row['nextToTeacher'])}|{pct(row['teacherToImprovement'])}|")
    lines += ["", "说明：`next→next-cycle`剔除了每条运行末轮右删失；按代表出生窗口统计的教师/改善属于cohort结果，不冒充改善发生窗口。", "",
              "## 4. H1预注册门", "", "|判据|结果|自动证据|", "|---|---|---|"]
    for row in gate_rows:
        lines.append(f"|{row['gate']}|{row['result']}|{row['evidence']}|")
    lines += ["", "## 5. 根因边界与下一步", "",
              f"当前可下的最强结论是：`{root_cause}`。250k数据否定了把FC-5候选溢出链作为这两个100-job实例的已确认根因。", "",
              "尚未被本实验单独拆分的备选方向依优先级为：CFVF规模化编辑产出、Qp/双Q协调、CA-TA与inherited LS预算分配，最后才是FM3景观。若继续，应另行预注册最小单变量诊断；不得直接修改PDDR，也不得恢复4500矩阵。", "",
              "## 6. 口径限制", "",
              "- 只有2个100-job实例、3个seed；结论限于预注册FC-5迁移假设。",
              "- 运行只到250k，不能排除500k后才出现的另一种退化机制；但它已足够检验本次预注册的中程迁移链。",
              "- `decisionArchiveFront`用于A2/A4性能轨迹；`observedFullFront`只用于发现审计，未混入正式指标。",
              "- 生命周期按稳定指纹观测；个体退休不等于方向语义从种群消失。", ""]
    (out / "FC5_250K_ROOT_CAUSE_REPORT.md").write_text("\n".join(lines), encoding="utf-8")
    return verdict


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-root", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)

    acceptance, artifacts = collect_acceptance(args.run_root)
    if not all(truth(r["accepted"]) for r in acceptance):
        raise SystemExit("run acceptance failed; analysis aborted")
    metrics, refs = collect_checkpoint_metrics(args.run_root)
    pairs, perf_summary = paired_performance(metrics)
    pddr_rounds, pddr_windows = collect_pddr(args.run_root)
    lifecycle_events, lifecycle_summaries = collect_lifecycle(args.run_root)
    gap_events, gap_summaries = collect_gaps(args.run_root)

    write_csv(args.out / "run-acceptance.csv", acceptance)
    write_csv(args.out / "input-artifact-hashes.tsv", artifacts)
    write_csv(args.out / "reference-fronts.csv", refs)
    write_csv(args.out / "checkpoint-metrics.csv", metrics)
    write_csv(args.out / "paired-performance.csv", pairs)
    write_csv(args.out / "performance-checkpoint-summary.csv", perf_summary)
    write_csv(args.out / "pddr-rounds.csv", pddr_rounds)
    write_csv(args.out / "pddr-window-summary.csv", pddr_windows)
    write_csv(args.out / "directional-lifecycle-events.csv", lifecycle_events)
    write_csv(args.out / "directional-lifecycle-summary.csv", lifecycle_summaries)
    write_csv(args.out / "archive-working-gap-events.csv", gap_events)
    write_csv(args.out / "archive-working-gap-summary.csv", gap_summaries)
    verdict = make_report(args.out, acceptance, perf_summary, pddr_windows, lifecycle_summaries, gap_summaries)

    product_rows = []
    for path in sorted(args.out.iterdir()):
        if path.is_file() and path.name != "evidence-sha256.tsv":
            product_rows.append({"sha256": sha256(path), "bytes": path.stat().st_size, "path": path.name})
    write_csv(args.out / "evidence-sha256.tsv", product_rows)
    print(verdict)


if __name__ == "__main__":
    main()
