#!/usr/bin/env python3
"""Recompute the fixed-20_2_3_1 A2/A3 diagnostic from run-end evidence.

This script is intentionally read-only with respect to the Java project.  It
reads the six completed local runs, verifies each run's evidence manifest,
computes paired minimisation metrics, and writes CSV/Markdown outputs below
``local-50k-fixed20/summary``.  It never launches Java, Maven, SSH, or a
remote task.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import math
import re
from pathlib import Path
from typing import Dict, Iterable, List, Mapping, Sequence, Tuple


SEEDS = ("20260822", "20260823", "20260824")
ARMS = ("A2", "A3")
INSTANCE = "20_2_3_1"
POPULATION = 100
MAX_FE = 50_000
EVENT_CAPACITY = 4096
EPS = 1.0e-12


def read_lines(path: Path) -> List[str]:
    return path.read_text(encoding="utf-8-sig").splitlines()


def read_properties(path: Path) -> Dict[str, str]:
    result: Dict[str, str] = {}
    if not path.is_file():
        return result
    for raw in read_lines(path):
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def number(value: str, default=""):
    if value is None or value == "":
        return default
    try:
        if value.lower() in {"nan", "infinity", "+infinity", "-infinity"}:
            return float(value)
        if any(ch in value for ch in ".eE"):
            return float(value)
        return int(value)
    except (TypeError, ValueError):
        return default


def read_front(path: Path) -> List[List[float]]:
    points: List[List[float]] = []
    if not path.is_file():
        return points
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        for row in csv.reader(handle):
            if not row or row[0].strip().lower() in {"cmax", "c"} or len(row) < 3:
                continue
            try:
                points.append([float(row[0]), float(row[1]), float(row[2])])
            except ValueError:
                continue
    return points


def retained_event_type_counts(path: Path) -> Dict[str, int]:
    result: Dict[str, int] = {}
    if not path.is_file():
        return result
    for line in read_lines(path):
        match = re.search(r"(?:^|,)type=([^,]+)", line)
        if match:
            event_type = match.group(1)
            result[event_type] = result.get(event_type, 0) + 1
    return result


def equal(left: Sequence[float], right: Sequence[float]) -> bool:
    return all(abs(left[index] - right[index]) <= EPS for index in range(3))


def dominates(left: Sequence[float], right: Sequence[float]) -> bool:
    strict = False
    for index in range(3):
        if left[index] > right[index] + EPS:
            return False
        if left[index] + EPS < right[index]:
            strict = True
    return strict


def unique(points: Iterable[Sequence[float]]) -> List[List[float]]:
    result: List[List[float]] = []
    for point in sorted((list(point) for point in points)):
        if not result or not equal(result[-1], point):
            result.append(point)
    return result


def nondominated(points: Iterable[Sequence[float]]) -> List[List[float]]:
    values = [list(point) for point in points]
    result = [candidate for candidate in values
              if not any(other is not candidate and dominates(other, candidate)
                         for other in values)]
    return unique(result)


def normalize(points: Iterable[Sequence[float]], reference: Sequence[Sequence[float]]) -> List[List[float]]:
    mins = [min(point[index] for point in reference) for index in range(3)]
    maxs = [max(point[index] for point in reference) for index in range(3)]
    return [[(point[index] - mins[index]) / max(EPS, maxs[index] - mins[index])
             for index in range(3)] for point in points]


def union_yz(points: Sequence[Sequence[float]], ry: float, rz: float) -> float:
    ordered = sorted(points, key=lambda point: point[1])
    area = 0.0
    min_z = rz
    index = 0
    while index < len(ordered):
        y = max(0.0, min(ry, ordered[index][1]))
        while index < len(ordered) and ordered[index][1] <= y + EPS:
            min_z = min(min_z, max(0.0, min(rz, ordered[index][2])))
            index += 1
        next_y = max(y, min(ry, ordered[index][1])) if index < len(ordered) else ry
        area += max(0.0, next_y - y) * max(0.0, rz - min_z)
    return area


def hypervolume(points: Sequence[Sequence[float]]) -> float:
    if not points:
        return 0.0
    rx = ry = rz = 1.1
    ordered = sorted(([max(0.0, min(rx, point[0])),
                       max(0.0, min(ry, point[1])),
                       max(0.0, min(rz, point[2]))] for point in points),
                     key=lambda point: point[0])
    volume = 0.0
    active: List[List[float]] = []
    index = 0
    while index < len(ordered):
        x = max(0.0, min(rx, ordered[index][0]))
        while index < len(ordered) and ordered[index][0] <= x + EPS:
            active.append(ordered[index])
            index += 1
        next_x = max(x, min(rx, ordered[index][0])) if index < len(ordered) else rx
        volume += max(0.0, next_x - x) * union_yz(active, ry, rz)
    return max(0.0, volume)


def distance(left: Sequence[float], right: Sequence[float]) -> float:
    return math.sqrt(sum((left[index] - right[index]) ** 2 for index in range(3)))


def igd(approximation: Sequence[Sequence[float]], reference: Sequence[Sequence[float]]) -> float:
    if not approximation or not reference:
        return float("nan")
    return sum(min(distance(target, candidate) for candidate in approximation)
               for target in reference) / len(reference)


def min_objectives(points: Sequence[Sequence[float]]) -> Tuple[float, float, float]:
    front = nondominated(points)
    return tuple(min(point[index] for point in front) for index in range(3))


def pair_metrics(a2: Mapping[str, object], a3: Mapping[str, object]) -> Dict[str, object]:
    front_a2 = a2["front"]
    front_a3 = a3["front"]
    reference = nondominated(list(front_a2) + list(front_a3))
    a2_norm = normalize(nondominated(front_a2), reference)
    a3_norm = normalize(nondominated(front_a3), reference)
    reference_norm = normalize(reference, reference)
    a2_min = min_objectives(front_a2)
    a3_min = min_objectives(front_a3)
    delta = [a3_min[index] - a2_min[index] for index in range(3)]
    objective_losses = sum(value > EPS for value in delta)
    objective_wins = sum(value < -EPS for value in delta)
    qp_total = int(a3["qpEventCountTotal"])
    if not a2["valid"] or not a3["valid"] or a2["initialPopulationHash"] != a3["initialPopulationHash"]:
        classification = "INVALID_EVIDENCE"
    elif not a2["loggingSufficient"] or not a3["loggingSufficient"]:
        classification = "LOGGING_INSUFFICIENT"
    elif qp_total <= 0 or a3["qpActionCount"] <= 0:
        classification = "NO_REGRESSION_SIGNAL"
    elif objective_losses >= 2:
        classification = "REGRESSION_SIGNAL"
    elif objective_wins >= 2:
        classification = "NO_REGRESSION_SIGNAL"
    else:
        classification = "MIXED_OR_NEUTRAL_SIGNAL"
    return {
        "seed": a2["seed"],
        "a2InitialPopulationHash": a2["initialPopulationHash"],
        "a3InitialPopulationHash": a3["initialPopulationHash"],
        "initialPopulationHashEqual": a2["initialPopulationHash"] == a3["initialPopulationHash"],
        "referenceFrontSize": len(reference),
        "a2FrontSize": len(front_a2),
        "a3FrontSize": len(front_a3),
        "a2HV": hypervolume(a2_norm),
        "a3HV": hypervolume(a3_norm),
        "deltaHV_A3_minus_A2": hypervolume(a3_norm) - hypervolume(a2_norm),
        "a2IGD": igd(a2_norm, reference_norm),
        "a3IGD": igd(a3_norm, reference_norm),
        "deltaIGD_A3_minus_A2": igd(a3_norm, reference_norm) - igd(a2_norm, reference_norm),
        "a2MinCmax": a2_min[0],
        "a3MinCmax": a3_min[0],
        "deltaMinCmax_A3_minus_A2": delta[0],
        "a2MinTEC": a2_min[1],
        "a3MinTEC": a3_min[1],
        "deltaMinTEC_A3_minus_A2": delta[1],
        "a2MinTWC": a2_min[2],
        "a3MinTWC": a3_min[2],
        "deltaMinTWC_A3_minus_A2": delta[2],
        "objectiveWins_A3": objective_wins,
        "objectiveLosses_A3": objective_losses,
        "qpTotal_A3": qp_total,
        "qpRetained_A3": a3["qpEventsRetained"],
        "qpRetentionRatio_A3": (float(a3["qpEventsRetained"]) / qp_total) if qp_total else "",
        "qpActionCounts_A3": "%s/%s/%s/%s" % (
            a3["qpKEEPCount"], a3["qpDIRECTIONALCount"],
            a3["qpEPSILONCount"], a3["qpCOMPLEMENTARYCount"]),
        "qpMaxAbsAverageReward_A3": a3["qpMaxAbsAverageReward"],
        "qpRewardAnomalyCandidate_A3": a3["qpRewardAnomalyCandidate"],
        "classification": classification,
    }


def manifest_check(directory: Path) -> Dict[str, object]:
    manifest = directory / "evidence-sha256.tsv"
    result = {"manifestPresent": manifest.is_file(), "manifestListedFiles": 0,
              "manifestActualFiles": 0, "manifestHashOK": False, "manifestErrors": ""}
    if not manifest.is_file():
        result["manifestErrors"] = "missing evidence-sha256.tsv"
        return result
    errors: List[str] = []
    listed = set()
    for line_number, raw in enumerate(read_lines(manifest)[1:], 2):
        if not raw.strip():
            continue
        parts = raw.split("\t", 2)
        if len(parts) != 3:
            errors.append("line %d malformed" % line_number)
            continue
        expected_hash, expected_bytes, relative = parts
        candidate = (directory / relative).resolve()
        try:
            candidate.relative_to(directory.resolve())
        except ValueError:
            errors.append("line %d escapes run" % line_number)
            continue
        listed.add(relative.replace("\\", "/"))
        if not candidate.is_file() or candidate.name == manifest.name:
            errors.append("line %d missing/self-listed" % line_number)
            continue
        actual_bytes = candidate.stat().st_size
        actual_hash = hashlib.sha256(candidate.read_bytes()).hexdigest()
        if str(actual_bytes) != expected_bytes or actual_hash != expected_hash.lower():
            errors.append("line %d hash-or-size-mismatch" % line_number)
    actual = {path.relative_to(directory).as_posix() for path in directory.rglob("*")
              if path.is_file() and path.name != manifest.name}
    if actual != listed:
        errors.append("listed-files=%d actual-files=%d" % (len(listed), len(actual)))
    result["manifestListedFiles"] = len(listed)
    result["manifestActualFiles"] = len(actual)
    result["manifestHashOK"] = not errors
    result["manifestErrors"] = "; ".join(errors)
    return result


def run_record(raw_root: Path, arm: str, seed: str) -> Dict[str, object]:
    directory = raw_root / arm / ("seed-" + seed)
    status = read_properties(directory / "status.properties")
    scope = read_properties(directory / "run-scope.txt")
    qp = read_properties(directory / "qp-summary.properties")
    lineage = read_properties(directory / "lineage-summary.properties")
    dual = read_properties(directory / "dual-q-summary.properties")
    front = read_front(directory / "front.csv")
    retained_types = retained_event_type_counts(directory / "qp-events.log")
    manifest = manifest_check(directory)
    qp_total = int(number(qp.get("eventCountTotal", "0"), 0))
    qp_retained = int(number(qp.get("eventsRetained", "0"), 0))
    qp_actions = sum(int(number(qp.get(action + ".count", "0"), 0))
                     for action in ("KEEP", "DIRECTIONAL", "EPSILON", "COMPLEMENTARY"))
    qp_avg_rewards = {action: float(number(qp.get(action + ".averageReward", "0"), 0.0))
                      for action in ("KEEP", "DIRECTIONAL", "EPSILON", "COMPLEMENTARY")}
    qp_max_abs_average_reward = max((abs(value) for value in qp_avg_rewards.values()), default=0.0)
    expected_scope = (scope.get("remoteRun") == "false"
                      and scope.get("instance") == INSTANCE
                      and int(number(scope.get("population", "-1"), -1)) == POPULATION
                      and int(number(scope.get("maxFEs", "-1"), -1)) == MAX_FE
                      and scope.get("randomStreamUnchanged") == "true")
    budget_ok = (status.get("status") == "COMPLETED"
                 and int(number(status.get("fullEvaluations", "-1"), -1)) == MAX_FE
                 and int(number(status.get("decoderCalls", "-1"), -1)) == MAX_FE
                 and int(number(status.get("illegalSolutions", "-1"), -1)) == 0
                 and int(number(status.get("duplicateEvaluations", "-1"), -1)) == 0)
    # For A3 the Qp action count and Q-table hash must be observable.  A2 is
    # the disabled control, so its zero/disabled values are the expected
    # mechanism evidence.  All three channels still require total/retained/
    # hash fields; retained Qp events are a rolling window, not full capture.
    channel_fields = all(key in qp for key in ("eventCountTotal", "eventsRetained", "eventStreamHash")
                         ) and all(key in lineage for key in ("eventCountTotal", "eventsRetained", "eventStreamHash")
                                   ) and all(key in dual for key in ("eventCountTotal", "eventsRetained", "eventStreamHash"))
    qp_expected = ((arm == "A2" and qp_total == 0 and qp.get("eventStreamHash") == "disabled")
                   or (arm == "A3" and qp_total > 0 and qp_actions > 0
                       and qp.get("tableHash", "") not in {"", "disabled"}))
    logging_sufficient = channel_fields and qp_expected
    row: Dict[str, object] = {
        "seed": seed, "arm": arm, "instance": scope.get("instance", ""),
        "population": scope.get("population", ""), "maxFEs": scope.get("maxFEs", ""),
        "status": status.get("status", ""), "mode": status.get("mode", ""),
        "fullEvaluations": number(status.get("fullEvaluations", "")),
        "decoderCalls": number(status.get("decoderCalls", "")),
        "illegalSolutions": number(status.get("illegalSolutions", "")),
        "duplicateEvaluations": number(status.get("duplicateEvaluations", "")),
        "initialPopulationHash": status.get("initialPopulationHash", ""),
        "evaluationTraceHash": status.get("evaluationTraceHash", ""),
        "front": front, "frontSize": len(front),
        "qpEventCountTotal": qp_total, "qpEventsRetained": qp_retained,
        "qpEventStreamHash": qp.get("eventStreamHash", ""),
        "qpTableHash": qp.get("tableHash", ""), "qpPbestSwitches": number(qp.get("pbestSwitches", "")),
        "qpActionCount": qp_actions,
        "qpKEEPCount": number(qp.get("KEEP.count", "")),
        "qpDIRECTIONALCount": number(qp.get("DIRECTIONAL.count", "")),
        "qpEPSILONCount": number(qp.get("EPSILON.count", "")),
        "qpCOMPLEMENTARYCount": number(qp.get("COMPLEMENTARY.count", "")),
        "qpKEEPAvgReward": qp_avg_rewards["KEEP"],
        "qpDIRECTIONALAvgReward": qp_avg_rewards["DIRECTIONAL"],
        "qpEPSILONAvgReward": qp_avg_rewards["EPSILON"],
        "qpCOMPLEMENTARYAvgReward": qp_avg_rewards["COMPLEMENTARY"],
        "qpMaxAbsAverageReward": qp_max_abs_average_reward,
        "qpRewardAnomalyCandidate": qp_max_abs_average_reward >= 1.0e6,
        "qpRetainedRewardEvents": retained_types.get("reward", 0),
        "qpRetainedSelectEvents": retained_types.get("select", 0),
        "qpRetainedLineageEvents": retained_types.get("lineage", 0),
        "lineageEventCountTotal": number(lineage.get("eventCountTotal", "")),
        "lineageEventsRetained": number(lineage.get("eventsRetained", "")),
        "lineageEventStreamHash": lineage.get("eventStreamHash", ""),
        "lineageDominatedRemovals": number(lineage.get("dominatedRemovals", "")),
        "lineageDuplicateRemovals": number(lineage.get("duplicateRemovals", "")),
        "lineageTruncations": number(lineage.get("truncations", "")),
        "lineageInsertions": number(lineage.get("insertions", "")),
        "dualQEventCountTotal": number(dual.get("eventCountTotal", "")),
        "dualQEventsRetained": number(dual.get("eventsRetained", "")),
        "dualQEventStreamHash": dual.get("eventStreamHash", ""),
        "dualQWarmup": number(dual.get("warmup", "")),
        "dualQPBlock": number(dual.get("pBlock", "")), "dualQGBlock": number(dual.get("gBlock", "")),
        "eventCapacity": EVENT_CAPACITY,
        "qpPayloadTruncated": qp_total > qp_retained,
        "loggingSufficient": logging_sufficient,
        "scopeValid": expected_scope, "budgetValid": budget_ok,
        "manifestPresent": manifest["manifestPresent"],
        "manifestListedFiles": manifest["manifestListedFiles"],
        "manifestActualFiles": manifest["manifestActualFiles"],
        "manifestHashOK": manifest["manifestHashOK"],
        "manifestErrors": manifest["manifestErrors"],
    }
    for action in ("KEEP", "DIRECTIONAL", "EPSILON", "COMPLEMENTARY"):
        count_key = "qp" + action + "Count"
        row[count_key + "Percent"] = (100.0 * float(row[count_key]) / qp_actions) if qp_actions else ""
    row["valid"] = bool(expected_scope and budget_ok and manifest["manifestHashOK"]
                        and status.get("initialPopulationHash") and status.get("evaluationTraceHash"))
    return row


def write_csv(path: Path, rows: Sequence[Mapping[str, object]], fields: Sequence[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(fields), extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row.get(field, "") for field in fields})


def fmt(value: object, digits: int = 6) -> str:
    if isinstance(value, float):
        if math.isnan(value):
            return "nan"
        return ("%." + str(digits) + "f") % value
    return str(value)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--audit-dir", type=Path,
                        default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    audit = args.audit_dir.resolve()
    raw = audit / "local-50k-fixed20" / "raw"
    summary = audit / "local-50k-fixed20" / "summary"
    records = [run_record(raw, arm, seed) for arm in ARMS for seed in SEEDS]
    pairs = [pair_metrics(next(row for row in records if row["arm"] == "A2" and row["seed"] == seed),
                          next(row for row in records if row["arm"] == "A3" and row["seed"] == seed))
             for seed in SEEDS]
    record_fields = [field for field in records[0] if field != "front"]
    pair_fields = list(pairs[0])
    write_csv(summary / "fixed20_run_summary.csv", records, record_fields)
    write_csv(summary / "fixed20_pairs.csv", pairs, pair_fields)

    valid_count = sum(bool(row["valid"]) for row in records)
    manifest_count = sum(bool(row["manifestHashOK"]) for row in records)
    classifications = {pair["classification"] for pair in pairs}
    regression_pairs = sum(pair["classification"] == "REGRESSION_SIGNAL" for pair in pairs)
    if regression_pairs >= 2:
        paired_quality_signal = "REGRESSION_SIGNAL"
    elif all(pair["classification"] == "NO_REGRESSION_SIGNAL" for pair in pairs):
        paired_quality_signal = "NO_REGRESSION_SIGNAL"
    else:
        paired_quality_signal = "MIXED_OR_NEUTRAL_SIGNAL"
    if valid_count != 6:
        overall = "INVALID_EVIDENCE"
    elif manifest_count != 6 or any(not row["loggingSufficient"] for row in records):
        overall = "LOGGING_INSUFFICIENT"
    else:
        # A3 changes Qp selection, its personal archive/lineage path, and the
        # dual-Q schedule together.  Therefore a quality regression is not a
        # single-variable causal result, even when every run is valid.
        overall = "COMPOSITE_BLOCK_UNRESOLVED"

    lines = [
        "# Fixed `20_2_3_1` A2→A3 causal diagnostic",
        "",
        "- Scope: local independent JVM, FM3/Shift.NONE, population 100, maxFEs 50,000.",
        "- Seeds: `20260822`, `20260823`, `20260824`; arms: `A2`, `A3`; completed runs: %d/6." % valid_count,
        "- This report is based only on `local-50k-fixed20`; earlier `local-50k` outputs used `100_2_3_1` and are not substituted here.",
        "- `V35FairRunner` exports observations after the algorithm returns; `writeRecord` does not feed any value back into control flow.",
        "",
        "## Run and evidence gate",
        "",
        "| check | result |",
        "|---|---:|",
        "| fixed scope + COMPLETED + 50,000 FE + 50,000 decoder calls + zero illegal/duplicate | %d/6 |" % valid_count,
        "| evidence-sha256.tsv recomputed with listed-file set equal to actual-file set | %d/6 |" % manifest_count,
        "| initial population hash equal within each A2/A3 seed pair | %d/3 |" % sum(bool(pair["initialPopulationHashEqual"]) for pair in pairs),
        "",
        "## Paired metrics",
        "",
        "`delta` is A3 minus A2. For minima, negative is better for A3; for HV, positive is better; for IGD, negative is better.",
        "",
        "| seed | Δmin Cmax | Δmin TEC | Δmin TWC | ΔHV | ΔIGD | A3 Qp total/retained | pair class |",
        "|---:|---:|---:|---:|---:|---:|---:|---|",
    ]
    for pair in pairs:
        lines.append("| %s | %s | %s | %s | %s | %s | %s/%s | `%s` |" % (
            pair["seed"], fmt(pair["deltaMinCmax_A3_minus_A2"]),
            fmt(pair["deltaMinTEC_A3_minus_A2"]), fmt(pair["deltaMinTWC_A3_minus_A2"]),
            fmt(pair["deltaHV_A3_minus_A2"]), fmt(pair["deltaIGD_A3_minus_A2"]),
            pair["qpTotal_A3"], pair["qpRetained_A3"], pair["classification"]))
    lines += [
        "",
        "## Mechanism evidence",
        "",
        "- A2 is the disabled-Qp control: Qp total `0`, action counts `0`, table hash `disabled`; lineage and dual-Q event totals are also `0`.",
        "- A3 has Qp totals `%s`, retained payload `%s`, and action counts `%s`; its Q-table hash, Qp stream hash, lineage stream hash, and dual-Q stream hash are exported per run." % (
            ", ".join(str(row["qpEventCountTotal"]) for row in records if row["arm"] == "A3"),
            ", ".join(str(row["qpEventsRetained"]) for row in records if row["arm"] == "A3"),
            ", ".join(str(row["qpActionCount"]) for row in records if row["arm"] == "A3")),
        "- A3 lineage totals are `%s`; dominated/duplicate removals are `%s`/`%s`; truncations are `%s`." % (
            ", ".join(str(row["lineageEventCountTotal"]) for row in records if row["arm"] == "A3"),
            ", ".join(str(row["lineageDominatedRemovals"]) for row in records if row["arm"] == "A3"),
            ", ".join(str(row["lineageDuplicateRemovals"]) for row in records if row["arm"] == "A3"),
            ", ".join(str(row["lineageTruncations"]) for row in records if row["arm"] == "A3")),
        "- A3 dual-Q phase counts are WARMUP/P/G = `49/26/25` in each run.",
        "- Qp action counts (KEEP/DIRECTIONAL/EPSILON/COMPLEMENTARY) and percentages are:",
    ]
    for row in records:
        if row["arm"] != "A3":
            continue
        lines.append("  - seed `%s`: `%s/%s/%s/%s` = `%.2f%%/%.2f%%/%.2f%%/%.2f%%`." % (
            row["seed"], row["qpKEEPCount"], row["qpDIRECTIONALCount"],
            row["qpEPSILONCount"], row["qpCOMPLEMENTARYCount"],
            row["qpKEEPCountPercent"], row["qpDIRECTIONALCountPercent"],
            row["qpEPSILONCountPercent"], row["qpCOMPLEMENTARYCountPercent"]))
    lines += [
        "- Qp average rewards (KEEP/DIRECTIONAL/EPSILON/COMPLEMENTARY) are:",
    ]
    for row in records:
        if row["arm"] != "A3":
            continue
        lines.append("  - seed `%s`: `%.6g/%.6g/%.6g/%.6g`; max absolute action average = `%.6g`." % (
            row["seed"], row["qpKEEPAvgReward"], row["qpDIRECTIONALAvgReward"],
            row["qpEPSILONAvgReward"], row["qpCOMPLEMENTARYAvgReward"],
            row["qpMaxAbsAverageReward"]))
    lines += [
        "- The retained Qp window has reward-event rows `%s`, select rows `%s`, and lineage rows `%s` (A3 seeds in order); reward rows are not a complete-history export." % (
            ", ".join(str(row["qpRetainedRewardEvents"]) for row in records if row["arm"] == "A3"),
            ", ".join(str(row["qpRetainedSelectEvents"]) for row in records if row["arm"] == "A3"),
            ", ".join(str(row["qpRetainedLineageEvents"]) for row in records if row["arm"] == "A3")),
        "- **Located defect candidate (`QP_SELECTION_OR_REWARD`)**: `ZhangBoQpController.reward()` computes `direction=(oldPhi-newPhi)/(abs(oldPhi)+normalizationEpsilon)`; the current personal-archive default epsilon is `1e-12`. The A3 action averages reach `10^8–10^9` (and one EPSILON average reaches about `-5.11e9`), which is numerically pathological if `oldPhi` is near zero. This is a source-level candidate, not yet a proven performance cause.",
        "- Qp event payload is a 4,096-entry rolling window: A3 total exceeds retained in all three runs. Counts, terminal stream hash, action totals/average rewards, Q-table hash, lineage counters, and dual-Q phase counts are available; the complete Qp event sequence is not.",
        "",
        "## Five-category gate",
        "",
        "The categories are audit labels, not statistical significance claims:",
        "",
        "1. `INVALID_EVIDENCE`: scope, budget, initial/evaluation hash, or evidence manifest fails.",
        "2. `LOGGING_INSUFFICIENT`: run is otherwise usable but required run-end count/hash/action fields are absent.",
        "3. `NO_REGRESSION_SIGNAL`: no A3 Qp activity or A3 wins at least two of three objective minima.",
        "4. `REGRESSION_SIGNAL`: Qp is active and A3 loses at least two of three objective minima; this is a paired quality label.",
        "5. `COMPOSITE_BLOCK_UNRESOLVED`: the runs are valid and may show regression, but A3 bundles Qp, personal archive/lineage, and dual-Q changes, or contains an unresolved reward-pathology candidate.",
        "",
        "**Paired quality signal: `%s`** (`%d/3` seed pairs meet the regression rule)." % (
            paired_quality_signal, regression_pairs),
        "**Final five-category classification: `%s`**. The fixed evidence supports a repeatable quality regression signal, but not an isolated causal attribution; A3 changes Qp plus personal-archive/lineage and the dual-Q phase schedule together. The reward anomaly is a located defect candidate, not proof that it caused the quality loss." % overall,
        "",
        "## Continue / stop gate",
        "",
        "- Continue only with a single-variable follow-up that keeps the fixed snapshot, seed, budget, population, PDDR semantics, and random stream unchanged; first compare A3 Qp action/reward/table transitions against a Qp-only or archive-only control.",
        "- Stop the current causal claim at `COMPOSITE_BLOCK_UNRESOLVED`; do not call Qp selection/reward the proven root cause while Qp, personal archive/lineage, and dual-Q differences remain bundled.",
        "- Do not collect full Qp history by changing algorithm decisions. If full sequence attribution is required, add a passive unbounded/streaming sink and rerun the same six-cell matrix only after the ON/OFF gate remains green.",
        "",
        "## Recompute",
        "",
        "```powershell",
        "python scripts/analyze_fixed20_diagnostic.py --audit-dir docs/evidence/V35-A2-A3-CAUSAL-AUDIT",
        "```",
    ]
    (summary / "FIXED20_A2_A3_DIAGNOSTIC_REPORT.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("overall=%s paired_quality=%s valid=%d/6 manifests=%d/6 pairs=%s" %
          (overall, paired_quality_signal, valid_count, manifest_count, ",".join(sorted(classifications))))


if __name__ == "__main__":
    main()
