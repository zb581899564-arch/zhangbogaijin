#!/usr/bin/env python3
"""Offline, provenance-preserving V35 final-front analysis.

This module intentionally knows nothing about a runner or algorithm internals.  Its only inputs are
the final-front CSV files and a run-metadata CSV.  It is therefore safe to use after the formal matrix
has been frozen, but cannot be used to manufacture a result before raw fronts exist.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import sys
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Tuple


EPS = 1e-12
REFERENCE_POINT = (1.1, 1.1, 1.1)
REQUIRED_COLUMNS = (
    "run_id", "algorithm", "instance", "seed", "status", "config_hash", "budget",
    "initial_population_hash", "instance_sha256", "instance_extension_sha256",
    "fatigue_manifest_sha256", "decoder_mode", "family_mode", "setup_mode", "shift_mode",
    "objectives", "front_path", "wall_clock_ms", "cpu_time_ms",
)
SEMANTIC_CONTRACT = {
    "decoder_mode": "FM3",
    "family_mode": "DEGENERATE_SINGLE_FAMILY",
    "setup_mode": "SEQUENCE_INDEPENDENT",
    "shift_mode": "NONE",
    "objectives": "0|1|6",
}
POINT_COLUMNS = ("cmax", "tec", "twc")
STAT_METRICS = {
    "hv": 1.0,
    "igd": -1.0,
    "spacing": -1.0,
    "front_size": 1.0,
    "cmax_min": -1.0,
    "tec_min": -1.0,
    "twc_min": -1.0,
    "wall_clock_ms": -1.0,
    "cpu_time_ms": -1.0,
}


class AnalysisError(ValueError):
    """Raised for input that cannot support a traceable analysis."""


@dataclass(frozen=True)
class Run:
    run_id: str
    algorithm: str
    instance: str
    seed: str
    status: str
    config_hash: str
    budget: str
    initial_population_hash: str
    instance_sha256: str
    instance_extension_sha256: str
    fatigue_manifest_sha256: str
    decoder_mode: str
    family_mode: str
    setup_mode: str
    shift_mode: str
    objectives: str
    front_path: Path
    wall_clock_ms: float
    cpu_time_ms: float

    @property
    def run_key(self) -> Tuple[str, str, str, str, str]:
        return (self.algorithm, self.config_hash, self.instance, self.seed, self.budget)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def _float(value: str, label: str, allow_blank: bool = False) -> float:
    if value is None or value.strip() == "":
        if allow_blank:
            return math.nan
        raise AnalysisError("missing numeric value for " + label)
    try:
        parsed = float(value)
    except ValueError as error:
        raise AnalysisError("invalid numeric value for " + label + ": " + value) from error
    if not math.isfinite(parsed) or parsed < 0.0:
        raise AnalysisError("non-finite or negative value for " + label)
    return parsed


def _required(row: Dict[str, str], column: str, row_number: int) -> str:
    value = row.get(column, "")
    if value is None or not value.strip():
        raise AnalysisError("row {0}: missing {1}".format(row_number, column))
    return value.strip()


def read_manifest(path: Path) -> List[Run]:
    if not path.is_file():
        raise AnalysisError("metadata manifest does not exist: " + str(path))
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        headers = reader.fieldnames or []
        missing = [name for name in REQUIRED_COLUMNS if name not in headers]
        if missing:
            raise AnalysisError("metadata manifest missing columns: " + ", ".join(missing))
        rows = list(reader)
    if not rows:
        raise AnalysisError("metadata manifest has no physical runs; no analysis was produced")
    base = path.parent
    runs: List[Run] = []
    for index, row in enumerate(rows, start=2):
        front_text = _required(row, "front_path", index)
        front_path = Path(front_text)
        if not front_path.is_absolute():
            front_path = (base / front_path).resolve()
        runs.append(Run(
            run_id=_required(row, "run_id", index), algorithm=_required(row, "algorithm", index),
            instance=_required(row, "instance", index), seed=_required(row, "seed", index),
            status=_required(row, "status", index), config_hash=_required(row, "config_hash", index),
            budget=_required(row, "budget", index),
            initial_population_hash=_required(row, "initial_population_hash", index),
            instance_sha256=_required(row, "instance_sha256", index),
            instance_extension_sha256=_required(row, "instance_extension_sha256", index),
            fatigue_manifest_sha256=_required(row, "fatigue_manifest_sha256", index),
            decoder_mode=_required(row, "decoder_mode", index),
            family_mode=_required(row, "family_mode", index),
            setup_mode=_required(row, "setup_mode", index), shift_mode=_required(row, "shift_mode", index),
            objectives=_required(row, "objectives", index), front_path=front_path,
            wall_clock_ms=_float(row.get("wall_clock_ms", ""), "wall_clock_ms at row {0}".format(index), True),
            cpu_time_ms=_float(row.get("cpu_time_ms", ""), "cpu_time_ms at row {0}".format(index), True),
        ))
    return runs


def validate_runs(all_runs: Sequence[Run], algorithms: Sequence[str], formal: bool) -> List[Run]:
    selected_names = tuple(algorithms)
    if len(selected_names) < 2 or len(set(selected_names)) != len(selected_names):
        raise AnalysisError("--algorithms must give at least two unique, frozen algorithm identifiers")
    selected = [run for run in all_runs if run.algorithm in selected_names]
    if not selected:
        raise AnalysisError("the metadata has no rows for the selected algorithms")
    run_ids, run_keys = set(), set()
    for run in selected:
        if run.run_id in run_ids:
            raise AnalysisError("duplicate run_id: " + run.run_id)
        if run.run_key in run_keys:
            raise AnalysisError("duplicate RunKey: " + "|".join(run.run_key))
        run_ids.add(run.run_id)
        run_keys.add(run.run_key)
        if run.status != "COMPLETED":
            raise AnalysisError("selected run is not COMPLETED: " + run.run_id)
        for field, expected in SEMANTIC_CONTRACT.items():
            if getattr(run, field) != expected:
                raise AnalysisError("semantic drift in {0}: {1}={2}, expected {3}".format(
                    run.run_id, field, getattr(run, field), expected))
        if not run.front_path.is_file():
            raise AnalysisError("raw final front does not exist: " + str(run.front_path))
        if formal and (not math.isfinite(run.wall_clock_ms) or not math.isfinite(run.cpu_time_ms)):
            raise AnalysisError("formal analysis requires wall_clock_ms and cpu_time_ms: " + run.run_id)

    by_instance_seed: Dict[Tuple[str, str], List[Run]] = defaultdict(list)
    for run in selected:
        by_instance_seed[(run.instance, run.seed)].append(run)
    for block, block_runs in by_instance_seed.items():
        present = {run.algorithm for run in block_runs}
        if present != set(selected_names):
            raise AnalysisError("incomplete algorithm block for {0}/{1}: {2}".format(
                block[0], block[1], ",".join(sorted(present))))
        fairness_values = {
            (run.initial_population_hash, run.instance_sha256, run.instance_extension_sha256,
             run.fatigue_manifest_sha256, run.budget, run.decoder_mode, run.family_mode,
             run.setup_mode, run.shift_mode, run.objectives)
            for run in block_runs
        }
        if len(fairness_values) != 1:
            raise AnalysisError("fairness metadata mismatch for {0}/{1}".format(block[0], block[1]))

    instances = sorted({run.instance for run in selected})
    for instance in instances:
        seed_sets = {
            algorithm: {run.seed for run in selected if run.instance == instance and run.algorithm == algorithm}
            for algorithm in selected_names
        }
        if len({tuple(sorted(seeds)) for seeds in seed_sets.values()}) != 1:
            raise AnalysisError("algorithms do not share exactly the same seeds for instance " + instance)
        if formal and len(next(iter(seed_sets.values()))) != 20:
            raise AnalysisError("formal V35 analysis requires exactly 20 completed paired seeds for " + instance)
    return selected


def read_front(path: Path) -> List[Tuple[float, float, float]]:
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        headers = {name.lower().strip(): name for name in (reader.fieldnames or [])}
        missing = [name for name in POINT_COLUMNS if name not in headers]
        if missing:
            raise AnalysisError("front missing objective columns in {0}: {1}".format(path, ", ".join(missing)))
        points = []
        for number, row in enumerate(reader, start=2):
            values = []
            for name in POINT_COLUMNS:
                raw = row.get(headers[name], "")
                try:
                    value = float(raw)
                except (TypeError, ValueError) as error:
                    raise AnalysisError("invalid {0} at {1}:{2}".format(name, path, number)) from error
                if not math.isfinite(value):
                    raise AnalysisError("non-finite {0} at {1}:{2}".format(name, path, number))
                values.append(value)
            points.append(tuple(values))
    if not points:
        raise AnalysisError("raw final front is empty: " + str(path))
    return nondominated(points)


def equal(left: Sequence[float], right: Sequence[float]) -> bool:
    return all(abs(left[index] - right[index]) <= EPS for index in range(3))


def dominates(left: Sequence[float], right: Sequence[float]) -> bool:
    return all(left[index] <= right[index] + EPS for index in range(3)) and any(
        left[index] < right[index] - EPS for index in range(3))


def nondominated(points: Iterable[Sequence[float]]) -> List[Tuple[float, float, float]]:
    copied = [tuple(float(value) for value in point) for point in points]
    result = [candidate for candidate in copied if not any(
        other is not candidate and dominates(other, candidate) for other in copied)]
    result.sort()
    unique: List[Tuple[float, float, float]] = []
    for point in result:
        if not unique or not equal(unique[-1], point):
            unique.append(point)
    return unique


def bounds(reference: Sequence[Sequence[float]]) -> Tuple[Tuple[float, float, float], Tuple[float, float, float]]:
    if not reference:
        raise AnalysisError("PFref is empty")
    minimum = tuple(min(point[index] for point in reference) for index in range(3))
    maximum = tuple(max(point[index] for point in reference) for index in range(3))
    return minimum, maximum


def normalize(points: Iterable[Sequence[float]], minimum: Sequence[float], maximum: Sequence[float]) -> List[Tuple[float, float, float]]:
    normalized = []
    for point in points:
        values = []
        for index in range(3):
            value = (point[index] - minimum[index]) / max(EPS, maximum[index] - minimum[index])
            values.append(max(0.0, min(1.0, value)))
        normalized.append(tuple(values))
    return nondominated(normalized)


def hypervolume(points: Sequence[Sequence[float]]) -> float:
    """3-D minimisation HV, matching P8MetricCalculator's sliced (1.1,1.1,1.1) convention."""
    if not points:
        return 0.0
    rx, ry, rz = REFERENCE_POINT
    sorted_points = sorted(points, key=lambda point: point[0])
    volume, active, index = 0.0, [], 0
    while index < len(sorted_points):
        x = max(0.0, min(rx, sorted_points[index][0]))
        while index < len(sorted_points) and sorted_points[index][0] <= x + EPS:
            active.append(sorted_points[index])
            index += 1
        next_x = max(0.0, min(rx, sorted_points[index][0])) if index < len(sorted_points) else rx
        volume += max(0.0, next_x - x) * _union_yz(active, ry, rz)
    return max(0.0, volume)


def _union_yz(points: Sequence[Sequence[float]], ry: float, rz: float) -> float:
    sorted_points = sorted(points, key=lambda point: point[1])
    area, min_z, index = 0.0, rz, 0
    while index < len(sorted_points):
        y = max(0.0, min(ry, sorted_points[index][1]))
        while index < len(sorted_points) and sorted_points[index][1] <= y + EPS:
            min_z = min(min_z, max(0.0, min(rz, sorted_points[index][2])))
            index += 1
        next_y = max(0.0, min(ry, sorted_points[index][1])) if index < len(sorted_points) else ry
        area += max(0.0, next_y - y) * max(0.0, rz - min_z)
    return area


def distance(left: Sequence[float], right: Sequence[float]) -> float:
    return math.sqrt(sum((left[index] - right[index]) ** 2 for index in range(3)))


def igd(approximation: Sequence[Sequence[float]], reference: Sequence[Sequence[float]]) -> float:
    return sum(min(distance(target, point) for point in approximation) for target in reference) / len(reference)


def spacing(points: Sequence[Sequence[float]]) -> float:
    if len(points) < 2:
        return 0.0
    nearest = [min(distance(point, other) for other in points if other is not point) for point in points]
    mean = sum(nearest) / len(nearest)
    return math.sqrt(sum((value - mean) ** 2 for value in nearest) / len(nearest))


def coverage(left: Sequence[Sequence[float]], right: Sequence[Sequence[float]]) -> float:
    if not right:
        return 0.0
    return sum(any(dominates(candidate, target) or equal(candidate, target) for candidate in left)
               for target in right) / float(len(right))


def metrics(front: Sequence[Sequence[float]], reference: Sequence[Sequence[float]],
            minimum: Sequence[float], maximum: Sequence[float]) -> Dict[str, float]:
    approximation = normalize(front, minimum, maximum)
    reference_normalized = normalize(reference, minimum, maximum)
    return {
        "hv": hypervolume(approximation),
        "igd": igd(approximation, reference_normalized),
        "spacing": spacing(approximation),
        "c_forward": coverage(approximation, reference_normalized),
        "c_reverse": coverage(reference_normalized, approximation),
        "front_size": float(len(front)),
        "cmax_min": min(point[0] for point in front), "cmax_max": max(point[0] for point in front),
        "tec_min": min(point[1] for point in front), "tec_max": max(point[1] for point in front),
        "twc_min": min(point[2] for point in front), "twc_max": max(point[2] for point in front),
    }


def _rank_abs(values: Sequence[float]) -> List[float]:
    ordered = sorted(enumerate(values), key=lambda pair: abs(pair[1]))
    ranks = [0.0] * len(values)
    start = 0
    while start < len(ordered):
        end = start + 1
        while end < len(ordered) and abs(abs(ordered[end][1]) - abs(ordered[start][1])) <= EPS:
            end += 1
        rank = (start + 1 + end) / 2.0
        for index in range(start, end):
            ranks[ordered[index][0]] = rank
        start = end
    return ranks


def wilcoxon_signed_rank(improvements: Sequence[float]) -> Dict[str, float]:
    nonzero = [value for value in improvements if abs(value) > EPS]
    if not nonzero:
        return {"n": 0, "w_plus": 0.0, "w_minus": 0.0, "p_value": 1.0}
    ranks = _rank_abs(nonzero)
    w_plus = sum(rank for rank, value in zip(ranks, nonzero) if value > EPS)
    w_minus = sum(rank for rank, value in zip(ranks, nonzero) if value < -EPS)
    # Average tied ranks are half-integers; doubling makes an exact sign-sum DP integer-valued.
    scaled_ranks = [int(round(rank * 2.0)) for rank in ranks]
    observed = int(round(w_plus * 2.0))
    counts = {0: 1}
    for rank in scaled_ranks:
        updated = dict(counts)
        for subtotal, count in counts.items():
            updated[subtotal + rank] = updated.get(subtotal + rank, 0) + count
        counts = updated
    total = float(2 ** len(scaled_ranks))
    lower = sum(count for subtotal, count in counts.items() if subtotal <= observed) / total
    upper = sum(count for subtotal, count in counts.items() if subtotal >= observed) / total
    return {"n": len(nonzero), "w_plus": w_plus, "w_minus": w_minus,
            "p_value": min(1.0, 2.0 * min(lower, upper))}


def paired_effect(improvements: Sequence[float]) -> Dict[str, float]:
    wins = sum(value > EPS for value in improvements)
    losses = sum(value < -EPS for value in improvements)
    ties = len(improvements) - wins - losses
    if not improvements:
        return {"wins": 0, "ties": 0, "losses": 0, "a12": math.nan, "cliffs_delta": math.nan}
    total = float(len(improvements))
    return {"wins": wins, "ties": ties, "losses": losses,
            "a12": (wins + 0.5 * ties) / total, "cliffs_delta": (wins - losses) / total}


def holm(rows: List[Dict[str, object]]) -> None:
    by_metric: Dict[str, List[Dict[str, object]]] = defaultdict(list)
    for row in rows:
        by_metric[str(row["metric"])].append(row)
    for family in by_metric.values():
        ordered = sorted(family, key=lambda row: float(row["p_value"]))
        previous = 0.0
        count = len(ordered)
        for index, row in enumerate(ordered):
            adjusted = min(1.0, (count - index) * float(row["p_value"]))
            adjusted = max(previous, adjusted)
            row["holm_p_value"] = adjusted
            previous = adjusted


def _ranks_descending(values: Dict[str, float], algorithms: Sequence[str]) -> Dict[str, float]:
    ordered = sorted(algorithms, key=lambda algorithm: (-values[algorithm], algorithm))
    ranks: Dict[str, float] = {}
    start = 0
    while start < len(ordered):
        end = start + 1
        while end < len(ordered) and abs(values[ordered[end]] - values[ordered[start]]) <= EPS:
            end += 1
        rank = (start + 1 + end) / 2.0
        for index in range(start, end):
            ranks[ordered[index]] = rank
        start = end
    return ranks


def _regularized_gamma_q(a: float, x: float) -> float:
    if x < 0.0 or a <= 0.0:
        return math.nan
    if x == 0.0:
        return 1.0
    gln = math.lgamma(a)
    if x < a + 1.0:
        term = 1.0 / a
        total = term
        ap = a
        for _ in range(1, 1000):
            ap += 1.0
            term *= x / ap
            total += term
            if abs(term) < abs(total) * 3e-14:
                break
        return 1.0 - total * math.exp(-x + a * math.log(x) - gln)
    b = x + 1.0 - a
    c = 1.0 / 1e-300
    d = 1.0 / b
    h = d
    for iteration in range(1, 1000):
        an = -iteration * (iteration - a)
        b += 2.0
        d = an * d + b
        if abs(d) < 1e-300:
            d = 1e-300
        c = b + an / c
        if abs(c) < 1e-300:
            c = 1e-300
        d = 1.0 / d
        delta = d * c
        h *= delta
        if abs(delta - 1.0) < 3e-14:
            break
    return h * math.exp(-x + a * math.log(x) - gln)


def friedman(blocks: Sequence[Dict[str, float]], algorithms: Sequence[str]) -> Dict[str, object]:
    count, size = len(blocks), len(algorithms)
    if count == 0:
        return {"n_blocks": 0, "chi_square": math.nan, "df": size - 1, "p_value": math.nan, "average_ranks": {}}
    rank_totals = {algorithm: 0.0 for algorithm in algorithms}
    tie_sum = 0.0
    for block in blocks:
        ranks = _ranks_descending(block, algorithms)
        for algorithm in algorithms:
            rank_totals[algorithm] += ranks[algorithm]
        groups: Dict[float, int] = defaultdict(int)
        for value in block.values():
            groups[round(value / EPS) if EPS else value] += 1
        tie_sum += sum(group ** 3 - group for group in groups.values() if group > 1)
    average = {algorithm: rank_totals[algorithm] / count for algorithm in algorithms}
    chi_square = (12.0 * count / (size * (size + 1.0)) *
                  sum(average[algorithm] ** 2 for algorithm in algorithms) - 3.0 * count * (size + 1.0))
    correction = 1.0 - tie_sum / (count * (size ** 3 - size)) if size > 1 else 1.0
    corrected = chi_square / correction if correction > EPS else 0.0
    return {"n_blocks": count, "chi_square": corrected, "df": size - 1,
            "p_value": _regularized_gamma_q((size - 1) / 2.0, corrected / 2.0), "average_ranks": average}


def _write_csv(path: Path, rows: Sequence[Dict[str, object]], fields: Sequence[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(fields), extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def _format_metrics(run: Run, values: Dict[str, float], front_hash: str) -> Dict[str, object]:
    return {
        "run_id": run.run_id, "algorithm": run.algorithm, "instance": run.instance, "seed": run.seed,
        "config_hash": run.config_hash, "budget": run.budget,
        "initial_population_hash": run.initial_population_hash, "front_path": str(run.front_path),
        "front_sha256": front_hash, "wall_clock_ms": run.wall_clock_ms, "cpu_time_ms": run.cpu_time_ms,
        **values,
    }


def compute(manifest_path: Path, output_dir: Path, algorithms: Sequence[str], control: str, formal: bool) -> Dict[str, object]:
    if control not in algorithms:
        raise AnalysisError("--control must be one of --algorithms")
    if output_dir.exists() and any(output_dir.iterdir()):
        raise AnalysisError("output directory must be new or empty; refusing to overwrite evidence: " + str(output_dir))
    all_runs = read_manifest(manifest_path)
    runs = validate_runs(all_runs, algorithms, formal)
    fronts = {run.run_id: read_front(run.front_path) for run in runs}
    front_hashes = {run.run_id: sha256_file(run.front_path) for run in runs}
    runs_by_instance: Dict[str, List[Run]] = defaultdict(list)
    for run in runs:
        runs_by_instance[run.instance].append(run)

    output_dir.mkdir(parents=True, exist_ok=True)
    reference_dir = output_dir / "reference-fronts"
    metric_rows: List[Dict[str, object]] = []
    pairwise_coverage_rows: List[Dict[str, object]] = []
    metric_values: Dict[str, Dict[str, float]] = {}
    normalized_fronts: Dict[str, List[Tuple[float, float, float]]] = {}
    reference_details = {}

    for instance, instance_runs in sorted(runs_by_instance.items()):
        reference = nondominated(point for run in instance_runs for point in fronts[run.run_id])
        minimum, maximum = bounds(reference)
        normalized_reference = normalize(reference, minimum, maximum)
        reference_path = reference_dir / (instance + ".csv")
        _write_csv(reference_path, [dict(zip(POINT_COLUMNS, point)) for point in reference], POINT_COLUMNS)
        reference_hash = sha256_file(reference_path)
        details = {
            "instance": instance, "pfref_size": len(reference), "minimum": minimum, "maximum": maximum,
            "normalization_epsilon": EPS, "hv_reference_point": REFERENCE_POINT,
            "pfref_sha256": reference_hash,
        }
        reference_details[instance] = details
        (reference_dir / (instance + ".json")).write_text(json.dumps(details, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        for run in instance_runs:
            values = metrics(fronts[run.run_id], reference, minimum, maximum)
            values["wall_clock_ms"] = run.wall_clock_ms
            values["cpu_time_ms"] = run.cpu_time_ms
            metric_values[run.run_id] = values
            normalized_fronts[run.run_id] = normalize(fronts[run.run_id], minimum, maximum)
            metric_rows.append(_format_metrics(run, values, front_hashes[run.run_id]))
        blocks: Dict[str, List[Run]] = defaultdict(list)
        for run in instance_runs:
            blocks[run.seed].append(run)
        for seed, block_runs in sorted(blocks.items()):
            by_algorithm = {run.algorithm: run for run in block_runs}
            for left in algorithms:
                for right in algorithms:
                    if left == right:
                        continue
                    left_run, right_run = by_algorithm[left], by_algorithm[right]
                    forward = coverage(normalized_fronts[left_run.run_id], normalized_fronts[right_run.run_id])
                    reverse = coverage(normalized_fronts[right_run.run_id], normalized_fronts[left_run.run_id])
                    pairwise_coverage_rows.append({
                        "instance": instance, "seed": seed, "left_algorithm": left, "right_algorithm": right,
                        "c_left_covers_right": forward, "c_right_covers_left": reverse,
                        "c_advantage": forward - reverse,
                    })

    metric_rows.sort(key=lambda row: (str(row["instance"]), str(row["seed"]), str(row["algorithm"])))
    _write_csv(output_dir / "per-run-metrics.csv", metric_rows, (
        "run_id", "algorithm", "instance", "seed", "config_hash", "budget", "initial_population_hash",
        "front_path", "front_sha256", "hv", "igd", "spacing", "c_forward", "c_reverse", "front_size",
        "cmax_min", "cmax_max", "tec_min", "tec_max", "twc_min", "twc_max", "wall_clock_ms", "cpu_time_ms",
    ))
    _write_csv(output_dir / "pairwise-coverage.csv", pairwise_coverage_rows, (
        "instance", "seed", "left_algorithm", "right_algorithm", "c_left_covers_right",
        "c_right_covers_left", "c_advantage",
    ))

    lookup = {(run.instance, run.seed, run.algorithm): run for run in runs}
    pairwise_rows: List[Dict[str, object]] = []
    candidates = [algorithm for algorithm in algorithms if algorithm != control]
    for instance in sorted(runs_by_instance):
        seeds = sorted({run.seed for run in runs_by_instance[instance]})
        for candidate in candidates:
            for metric, direction in STAT_METRICS.items():
                values = []
                for seed in seeds:
                    candidate_run = lookup[(instance, seed, candidate)]
                    control_run = lookup[(instance, seed, control)]
                    candidate_value = metric_values[candidate_run.run_id][metric]
                    control_value = metric_values[control_run.run_id][metric]
                    if math.isfinite(candidate_value) and math.isfinite(control_value):
                        values.append(direction * (candidate_value - control_value))
                if len(values) != len(seeds):
                    continue
                result, effect = wilcoxon_signed_rank(values), paired_effect(values)
                pairwise_rows.append({"instance": instance, "metric": metric, "candidate": candidate,
                                      "control": control, "n_pairs": len(values), **result, **effect})
            advantages = []
            coverage_index = {(row["instance"], row["seed"], row["left_algorithm"], row["right_algorithm"]): row
                              for row in pairwise_coverage_rows}
            for seed in seeds:
                advantages.append(float(coverage_index[(instance, seed, candidate, control)]["c_advantage"]))
            result, effect = wilcoxon_signed_rank(advantages), paired_effect(advantages)
            pairwise_rows.append({"instance": instance, "metric": "c_metric_advantage", "candidate": candidate,
                                  "control": control, "n_pairs": len(advantages), **result, **effect})
    holm(pairwise_rows)
    pairwise_rows.sort(key=lambda row: (str(row["metric"]), str(row["instance"]), str(row["candidate"])))
    _write_csv(output_dir / "statistics-pairwise.csv", pairwise_rows, (
        "instance", "metric", "candidate", "control", "n_pairs", "n", "w_plus", "w_minus", "p_value",
        "holm_p_value", "wins", "ties", "losses", "a12", "cliffs_delta",
    ))

    friedman_rows = []
    for metric, direction in STAT_METRICS.items():
        blocks = []
        for instance in sorted(runs_by_instance):
            for seed in sorted({run.seed for run in runs_by_instance[instance]}):
                block = {algorithm: direction * metric_values[lookup[(instance, seed, algorithm)].run_id][metric]
                         for algorithm in algorithms}
                if all(math.isfinite(value) for value in block.values()):
                    blocks.append(block)
        result = friedman(blocks, algorithms)
        friedman_rows.append({"metric": metric, **result,
                              "average_ranks": json.dumps(result["average_ranks"], sort_keys=True)})
    _write_csv(output_dir / "statistics-friedman.csv", friedman_rows,
               ("metric", "n_blocks", "chi_square", "df", "p_value", "average_ranks"))

    status = {
        "analysis_kind": "FORMAL_METRICS_READY_FOR_HUMAN_INTERPRETATION" if formal else "NON_FORMAL_DIAGNOSTIC_ONLY",
        "automatic_conclusions_emitted": False,
        "formal_matrix_claimed": bool(formal),
        "manifest_sha256": sha256_file(manifest_path),
        "algorithms": list(algorithms), "control": control, "instances": sorted(runs_by_instance),
        "run_count": len(runs), "reference_details": reference_details,
        "metric_convention": "all pairwise improvements/effects are candidate minus control after applying metric direction; positive is better",
        "holm_family": "all per-instance candidate-versus-control hypotheses for the same metric",
    }
    (output_dir / "analysis-status.json").write_text(json.dumps(status, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return status


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="V35 raw-front-only metric and statistics pipeline")
    parser.add_argument("--manifest", required=True, type=Path, help="run metadata CSV; never a derived result table")
    parser.add_argument("--output-dir", required=True, type=Path, help="new/empty analysis output directory")
    parser.add_argument("--algorithms", required=True, help="frozen comma-separated participant identifiers")
    parser.add_argument("--control", required=True, help="one identifier from --algorithms")
    parser.add_argument("--formal", action="store_true", help="enforce the exact 20-seed formal V35 gate")
    arguments = parser.parse_args(argv)
    try:
        algorithms = tuple(part.strip() for part in arguments.algorithms.split(",") if part.strip())
        status = compute(arguments.manifest.resolve(), arguments.output_dir.resolve(), algorithms,
                         arguments.control, arguments.formal)
    except AnalysisError as error:
        print("V35 analysis refused: " + str(error), file=sys.stderr)
        return 2
    print("V35 analysis complete: {0}; {1} raw runs; no automatic conclusion emitted.".format(
        status["analysis_kind"], status["run_count"]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
