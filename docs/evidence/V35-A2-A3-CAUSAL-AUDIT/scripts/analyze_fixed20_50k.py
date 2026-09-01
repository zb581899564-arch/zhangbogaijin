#!/usr/bin/env python3
"""Summarise the fixed 20-job A2/A3 run-end diagnostic evidence."""

from __future__ import annotations

import csv
import hashlib
import math
import re
import runpy
import statistics
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RUN_ROOT = ROOT / "local-50k-fixed20" / "raw"
OUTPUT = ROOT / "local-50k-fixed20"
SEEDS = (20260822, 20260823, 20260824)
ARMS = ("A2", "A3")
REWARD = re.compile(
    r"type=reward,.*?action=([^,]+),dom=([^,]+),direction=([^,]+),"
    r"archive=([^,]+),risk=([^,]+),total=([^,]+),"
)


def properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def front(path: Path) -> list[tuple[float, float, float]]:
    result = []
    with path.open(encoding="utf-8", newline="") as handle:
        for row in csv.DictReader(handle):
            result.append((float(row["Cmax"]), float(row["TEC"]), float(row["TWC"])))
    return result


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def verify_manifest(directory: Path) -> tuple[int, int]:
    manifest = directory / "evidence-sha256.tsv"
    checked = bad = 0
    for line in manifest.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        fields = line.split("\t")
        if fields[0].lower() == "sha256":
            continue
        if len(fields) != 3:
            raise RuntimeError(f"invalid manifest row in {manifest}: {line!r}")
        expected, _bytes, relative = fields
        target = directory / relative
        checked += 1
        if not target.is_file() or sha256_file(target).lower() != expected.lower():
            bad += 1
    return checked, bad


def median(values: list[float]) -> float:
    return statistics.median(values) if values else math.nan


def main() -> int:
    helper = ROOT.parent / "V35-ND-ARCHIVE" / "01-offline-cardinality-audit" / "run_offline_cardinality_audit.py"
    metrics = runpy.run_path(str(helper))
    nondominated = metrics["nondominated"]
    calculate_metrics = metrics["calculate_metrics"]

    rows: list[dict[str, object]] = []
    fronts: dict[tuple[int, str], list[tuple[float, float, float]]] = {}
    reward_rows: list[dict[str, object]] = []
    total_checked = total_bad = 0
    for seed in SEEDS:
        for arm in ARMS:
            directory = RUN_ROOT / arm / f"seed-{seed}"
            if not directory.is_dir():
                raise RuntimeError(f"missing diagnostic run: {directory}")
            status = properties(directory / "status.properties")
            qp = properties(directory / "qp-summary.properties")
            lineage = properties(directory / "lineage-summary.properties")
            dual = properties(directory / "dual-q-summary.properties")
            values = front(directory / "front.csv")
            fronts[(seed, arm)] = values
            checked, bad = verify_manifest(directory)
            total_checked += checked
            total_bad += bad
            rewards: list[float] = []
            directions: list[float] = []
            action_rewards: dict[str, list[float]] = {}
            for line in (directory / "qp-events.log").read_text(encoding="utf-8").splitlines():
                match = REWARD.search(line)
                if match:
                    action = match.group(1)
                    direction = float(match.group(3))
                    total = float(match.group(6))
                    directions.append(direction)
                    rewards.append(total)
                    action_rewards.setdefault(action, []).append(total)
            reward_rows.append({
                "seed": seed,
                "arm": arm,
                "retained_reward_events": len(rewards),
                "direction_min": min(directions) if directions else math.nan,
                "direction_median": median(directions),
                "direction_lt_minus_1": sum(value < -1.0 for value in directions),
                "direction_lt_minus_1e6": sum(value < -1.0e6 for value in directions),
                "reward_min": min(rewards) if rewards else math.nan,
                "reward_median": median(rewards),
            })
            rows.append({
                "seed": seed,
                "arm": arm,
                "status": status.get("status"),
                "full_evaluations": int(status.get("fullEvaluations", "-1")),
                "decoder_calls": int(status.get("decoderCalls", "-1")),
                "illegal_solutions": int(status.get("illegalSolutions", "-1")),
                "duplicate_evaluations": int(status.get("duplicateEvaluations", "-1")),
                "initial_population_hash": status.get("initialPopulationHash"),
                "evaluation_trace_hash": status.get("evaluationTraceHash"),
                "front_size": len(values),
                "min_cmax": min(point[0] for point in values),
                "min_tec": min(point[1] for point in values),
                "min_twc": min(point[2] for point in values),
                "qp_event_total": int(qp.get("eventCountTotal", "0")),
                "qp_event_retained": int(qp.get("eventsRetained", "0")),
                "qp_pbest_switches": int(qp.get("pbestSwitches", "0")),
                "qp_keep_count": int(qp.get("KEEP.count", "0")),
                "qp_directional_count": int(qp.get("DIRECTIONAL.count", "0")),
                "qp_epsilon_count": int(qp.get("EPSILON.count", "0")),
                "qp_complementary_count": int(qp.get("COMPLEMENTARY.count", "0")),
                "qp_keep_average_reward": float(qp.get("KEEP.averageReward", "0")),
                "qp_directional_average_reward": float(qp.get("DIRECTIONAL.averageReward", "0")),
                "qp_epsilon_average_reward": float(qp.get("EPSILON.averageReward", "0")),
                "qp_complementary_average_reward": float(qp.get("COMPLEMENTARY.averageReward", "0")),
                "archive_insertions": int(lineage.get("insertions", "0")),
                "archive_dominated_removals": int(lineage.get("dominatedRemovals", "0")),
                "archive_duplicate_removals": int(lineage.get("duplicateRemovals", "0")),
                "archive_truncations": int(lineage.get("truncations", "0")),
                "dual_q_warmup": int(dual.get("warmup", "0")),
                "dual_q_p_block": int(dual.get("pBlock", "0")),
                "dual_q_g_block": int(dual.get("gBlock", "0")),
                "manifest_files_checked": checked,
                "manifest_failures": bad,
            })

    reference = nondominated(point for values in fronts.values() for point in values)
    by_key = {(int(row["seed"]), str(row["arm"])): row for row in rows}
    paired: list[dict[str, object]] = []
    for seed in SEEDS:
        a2_hv, a2_igd = calculate_metrics(fronts[(seed, "A2")], reference)
        a3_hv, a3_igd = calculate_metrics(fronts[(seed, "A3")], reference)
        a2 = by_key[(seed, "A2")]
        a3 = by_key[(seed, "A3")]
        paired.append({
            "seed": seed,
            "same_initial_population": a2["initial_population_hash"] == a3["initial_population_hash"],
            "a2_hv": a2_hv,
            "a3_hv": a3_hv,
            "delta_hv": (a3_hv - a2_hv) / a2_hv,
            "a2_igd": a2_igd,
            "a3_igd": a3_igd,
            "igd_improvement": (a2_igd - a3_igd) / a2_igd,
            "a2_min_cmax": a2["min_cmax"],
            "a3_min_cmax": a3["min_cmax"],
            "cmax_improvement": (float(a2["min_cmax"]) - float(a3["min_cmax"])) / float(a2["min_cmax"]),
            "a2_min_tec": a2["min_tec"],
            "a3_min_tec": a3["min_tec"],
            "a2_min_twc": a2["min_twc"],
            "a3_min_twc": a3["min_twc"],
        })

    def write_csv(path: Path, data: list[dict[str, object]]) -> None:
        with path.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(data[0]))
            writer.writeheader()
            writer.writerows(data)

    write_csv(OUTPUT / "fixed20-run-summary.csv", rows)
    write_csv(OUTPUT / "fixed20-paired-metrics.csv", paired)
    write_csv(OUTPUT / "fixed20-qp-reward-anomalies.csv", reward_rows)
    if total_bad:
        raise RuntimeError(f"run manifest failures: {total_bad}/{total_checked}")
    print(f"runs={len(rows)} manifest_files_checked={total_checked} failures={total_bad}")
    print(f"reference_points={len(reference)}")
    print("median_delta_hv=" + str(median([float(row["delta_hv"]) for row in paired])))
    print("median_igd_improvement=" + str(median([float(row["igd_improvement"]) for row in paired])))
    print("median_cmax_improvement=" + str(median([float(row["cmax_improvement"]) for row in paired])))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
