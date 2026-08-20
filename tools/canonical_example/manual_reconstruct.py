#!/usr/bin/env python3
"""Independent I1 decoder reconstruction.

This module deliberately does not import or invoke any Java decoder.  It reads
only the frozen I1 input tables and applies the published formulas and the
documented production resource-selection rules step by step.
"""

from __future__ import annotations

import argparse
import csv
import math
from pathlib import Path


EPS = 1.0e-12
TOLERANCE = 1.0e-9
NUMERIC_TRACE_FIELDS = (
    "predecessorCompletion", "machineAvailableBefore", "workerAvailableBefore",
    "start", "recoveryDuration", "fatigueBeforeRecovery", "fatigueAtStart",
    "ST", "SUT", "machineSpeed", "machinePower", "workerEfficiency", "workerCost",
    "lambda", "muCurrentStage", "recoveryMu", "r", "delta",
    "baseProcessing", "baseSetup", "baseAT", "fatigueMultiplier",
    "actualProcessing", "actualSetup", "actualAT", "end", "fatigueAfter",
    "energy", "cost",
)
IDENTITY_TRACE_FIELDS = ("sequence", "job", "stage", "factory", "machine", "worker")


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream))


def write_csv(path: Path, rows: list[dict[str, object]], fields: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def parse_vector(path: Path) -> dict[str, list[int]]:
    rows = read_csv(path)
    values = {row["vector"]: [int(value) for value in row["values"].split(",")] for row in rows}
    if set(values) != {"JS", "FA", "MA", "WA"}:
        raise ValueError(f"Expected exactly JS/FA/MA/WA in {path}")
    return values


def load_inputs(root: Path) -> dict[str, object]:
    input_dir = root / "01_input"
    job_rows = read_csv(input_dir / "job_stage_data.csv")
    machine_rows = read_csv(input_dir / "machine_data.csv")
    worker_rows = read_csv(input_dir / "worker_fatigue_data.csv")
    x0 = parse_vector(input_dir / "X0-zero-based.csv")
    jobs = 1 + max(int(row["job"]) for row in job_rows)
    stages = 1 + max(int(row["stage"]) for row in job_rows)
    factories = 1 + max(int(row["factory"]) for row in machine_rows)
    standard = {(int(r["job"]), int(r["stage"])): float(r["ST"]) for r in job_rows}
    setup = {(int(r["job"]), int(r["stage"])): float(r["SUT"]) for r in job_rows}
    speed = {(int(r["factory"]), int(r["stage"]), int(r["machine"])): float(r["speed"]) for r in machine_rows}
    power = {(int(r["factory"]), int(r["stage"]), int(r["machine"])): float(r["power"]) for r in machine_rows}
    efficiency: dict[tuple[int, int], float] = {}
    cost: dict[tuple[int, int], float] = {}
    lam: dict[tuple[int, int, int], float] = {}
    mu: dict[tuple[int, int, int], float] = {}
    maximum_increase: dict[int, float] = {}
    eligible: dict[tuple[int, int], list[int]] = {}
    for row in worker_rows:
        f, k, w = int(row["factory"]), int(row["stage"]), int(row["worker"])
        efficiency[(f, w)] = float(row["efficiency"])
        cost[(f, w)] = float(row["cost"])
        lam[(f, w, k)] = float(row["lambda"])
        mu[(f, w, k)] = float(row["mu"])
        maximum_increase[k] = float(row["r"])
        if row["eligible"].lower() == "true":
            eligible.setdefault((f, k), []).append(w)
    machine_counts: dict[tuple[int, int], int] = {}
    worker_counts: dict[int, int] = {}
    for f in range(factories):
        worker_counts[f] = 1 + max(w for ff, w in efficiency if ff == f)
        for k in range(stages):
            machine_counts[(f, k)] = 1 + max(m for ff, kk, m in speed if ff == f and kk == k)
    return dict(jobs=jobs, stages=stages, factories=factories, standard=standard,
                setup=setup, speed=speed, power=power, efficiency=efficiency,
                cost=cost, lam=lam, mu=mu, maximum_increase=maximum_increase,
                eligible=eligible, machine_counts=machine_counts,
                worker_counts=worker_counts, x0=x0)


def recover(fatigue: float, mu: float, idle: float) -> float:
    return max(0.0, min(math.nextafter(1.0, 0.0), fatigue * math.exp(-mu * idle)))


def multiplier(fatigue: float, maximum_increase: float) -> float:
    return 1.0 + maximum_increase / math.log(2.0) * math.log1p(fatigue)


def accumulate(fatigue: float, lam: float, duration: float) -> float:
    value = fatigue + (1.0 - fatigue) * (-math.expm1(-lam * duration))
    return max(0.0, min(math.nextafter(1.0, 0.0), value))


def excess_work(initial: float, lam: float, duration: float, threshold: float) -> float:
    if duration == 0.0:
        return 0.0
    crossing = 0.0
    if initial < threshold:
        crossing = -math.log((1.0 - threshold) / (1.0 - initial)) / lam
        if crossing >= duration:
            return 0.0
    return ((1.0 - threshold) * (duration - crossing)
            + (1.0 - initial) / lam * (math.exp(-lam * duration) - math.exp(-lam * crossing)))


def excess_recovery(initial: float, mu: float, duration: float, threshold: float) -> float:
    if duration == 0.0 or initial <= threshold:
        return 0.0
    above = min(duration, math.log(initial / threshold) / mu)
    return initial * (-math.expm1(-mu * above)) / mu - threshold * above


def time_above_work(initial: float, lam: float, duration: float, threshold: float) -> float:
    if initial >= threshold:
        return duration
    crossing = -math.log((1.0 - threshold) / (1.0 - initial)) / lam
    return max(0.0, duration - crossing)


def time_above_recovery(initial: float, mu: float, duration: float, threshold: float) -> float:
    if initial <= threshold:
        return 0.0
    return min(duration, math.log(initial / threshold) / mu)


def decode(data: dict[str, object], mode: str) -> tuple[list[dict[str, object]], dict[str, float]]:
    fatigue_enabled = mode == "FM3"
    recovery_enabled = mode == "FM3"
    fatigue_worker_selection = mode == "FM3"
    jobs, stages, factories = int(data["jobs"]), int(data["stages"]), int(data["factories"])
    x0: dict[str, list[int]] = data["x0"]  # type: ignore[assignment]
    position = {job: p for p, job in enumerate(x0["JS"])}
    factory_jobs = [[] for _ in range(factories)]
    for p, job in enumerate(x0["JS"]):
        factory_jobs[x0["FA"][p]].append(job)
    completion = [[0.0] * stages for _ in range(jobs)]
    machine_available = {(f, k, m): 0.0 for (f, k), count in data["machine_counts"].items() for m in range(count)}  # type: ignore[union-attr]
    machine_used = {key: False for key in machine_available}
    worker_available = {(f, w): 0.0 for f, count in data["worker_counts"].items() for w in range(count)}  # type: ignore[union-attr]
    worker_fatigue = {key: 0.0 for key in worker_available}
    worker_used = {key: False for key in worker_available}
    worker_last_stage = {key: -1 for key in worker_available}
    continuous = {key: 0.0 for key in worker_available}
    longest = {key: 0.0 for key in worker_available}
    factory_makespan = [0.0] * factories
    factory_energy = [0.0] * factories
    factory_cost = [0.0] * factories
    records: list[dict[str, object]] = []
    fatigue_sum = fatigue_max = excess = high_time = natural_recovery = 0.0
    safe_events = 0
    sequence = 0

    def candidate(f: int, k: int, job: int, machine: int, worker: int, predecessor: float) -> dict[str, float]:
        machine_before = machine_available[(f, k, machine)]
        worker_before = worker_available[(f, worker)]
        start = max(predecessor, machine_before, worker_before)
        fatigue_before = worker_fatigue[(f, worker)]
        recovery_duration = max(0.0, start - worker_before) if worker_used[(f, worker)] else 0.0
        recovery_stage = worker_last_stage[(f, worker)]
        recovery_mu = data["mu"][(f, worker, recovery_stage)] if recovery_stage >= 0 else data["mu"][(f, worker, k)]  # type: ignore[index]
        at_start = recover(fatigue_before, recovery_mu, recovery_duration) if recovery_enabled and recovery_duration > 0 else fatigue_before
        if not fatigue_enabled:
            at_start = 0.0
        worker_efficiency = data["efficiency"][(f, worker)]  # type: ignore[index]
        base_processing = data["standard"][(job, k)] / (data["speed"][(f, k, machine)] * worker_efficiency)  # type: ignore[index,operator]
        base_setup = data["setup"][(job, k)] / worker_efficiency  # type: ignore[index,operator]
        base_at = base_processing + base_setup
        factor = multiplier(at_start, data["maximum_increase"][k]) if fatigue_enabled else 1.0  # type: ignore[index]
        actual_processing = base_processing * factor
        actual_setup = base_setup * factor
        actual_at = actual_processing + actual_setup
        after = accumulate(at_start, data["lam"][(f, worker, k)], actual_at) if fatigue_enabled else 0.0  # type: ignore[index]
        return dict(start=start, recoveryDuration=recovery_duration, fatigueBeforeRecovery=fatigue_before,
                    fatigueAtStart=at_start, recoveryMu=recovery_mu, baseProcessing=base_processing,
                    baseSetup=base_setup, baseAT=base_at, fatigueMultiplier=factor,
                    actualProcessing=actual_processing, actualSetup=actual_setup,
                    actualAT=actual_at, end=start + actual_at, fatigueAfter=after,
                    baseEnd=start + base_at)

    for f in range(factories):
        order = list(factory_jobs[f])
        for k in range(stages):
            if k > 0:
                previous_rank = {job: rank for rank, job in enumerate(order)}
                order.sort(key=lambda job: (completion[job][k - 1], previous_rank[job], job))
            for ordinal, job in enumerate(order):
                if k == 0:
                    machine = x0["MA"][position[job]]
                elif ordinal == 0:
                    machine = 0
                else:
                    candidates_m = range(data["machine_counts"][(f, k)])  # type: ignore[index]
                    machine = min(candidates_m, key=lambda m: (machine_available[(f, k, m)], m))
                predecessor = 0.0 if k == 0 else completion[job][k - 1]
                if k == 0:
                    worker = x0["WA"][position[job]]
                    selected = candidate(f, k, job, machine, worker, predecessor)
                else:
                    possibilities = []
                    for possible_worker in data["eligible"][(f, k)]:  # type: ignore[index]
                        possible = candidate(f, k, job, machine, possible_worker, predecessor)
                        criterion = possible["end"] if fatigue_worker_selection else possible["baseEnd"]
                        possibilities.append((criterion, possible_worker, possible))
                    _, worker, selected = min(possibilities, key=lambda item: (item[0], item[1]))
                machine_before = machine_available[(f, k, machine)]
                worker_before = worker_available[(f, worker)]
                fatigue_before = worker_fatigue[(f, worker)]
                if recovery_enabled and worker_used[(f, worker)] and selected["start"] > worker_before:
                    gap = selected["start"] - worker_before
                    previous_mu = data["mu"][(f, worker, worker_last_stage[(f, worker)])]  # type: ignore[index]
                    excess += excess_recovery(fatigue_before, previous_mu, gap, 0.8)
                    high_time += time_above_recovery(fatigue_before, previous_mu, gap, 0.8)
                    natural_recovery += gap
                if fatigue_enabled:
                    current_lambda = data["lam"][(f, worker, k)]  # type: ignore[index]
                    excess += excess_work(selected["fatigueAtStart"], current_lambda, selected["actualAT"], 0.8)
                    high_time += time_above_work(selected["fatigueAtStart"], current_lambda, selected["actualAT"], 0.8)
                idle = max(0.0, selected["start"] - machine_before) if machine_used[(f, k, machine)] else 0.0
                energy = selected["actualAT"] * data["power"][(f, k, machine)] + idle  # type: ignore[index,operator]
                op_cost = (selected["actualAT"] + idle) * data["cost"][(f, worker)]  # type: ignore[index,operator]
                safe = fatigue_enabled and selected["fatigueAfter"] > 0.9
                safe_events += int(safe)
                if worker_used[(f, worker)] and abs(selected["start"] - worker_before) <= EPS:
                    continuous[(f, worker)] += selected["actualAT"]
                else:
                    continuous[(f, worker)] = selected["actualAT"]
                longest[(f, worker)] = max(longest[(f, worker)], continuous[(f, worker)])
                machine_available[(f, k, machine)] = selected["end"]
                machine_used[(f, k, machine)] = True
                worker_available[(f, worker)] = selected["end"]
                worker_fatigue[(f, worker)] = selected["fatigueAfter"]
                worker_used[(f, worker)] = True
                worker_last_stage[(f, worker)] = k
                completion[job][k] = selected["end"]
                factory_energy[f] += energy
                factory_cost[f] += op_cost
                fatigue_sum += selected["fatigueAfter"]
                fatigue_max = max(fatigue_max, selected["fatigueAfter"])
                records.append(dict(
                    sequence=sequence, job=job, stage=k, factory=f, machine=machine, worker=worker,
                    predecessorCompletion=predecessor, machineAvailableBefore=machine_before,
                    workerAvailableBefore=worker_before, start=selected["start"],
                    recoveryDuration=selected["recoveryDuration"], fatigueBeforeRecovery=fatigue_before,
                    fatigueAtStart=selected["fatigueAtStart"], ST=data["standard"][(job, k)],  # type: ignore[index]
                    SUT=data["setup"][(job, k)], machineSpeed=data["speed"][(f, k, machine)],  # type: ignore[index]
                    machinePower=data["power"][(f, k, machine)], workerEfficiency=data["efficiency"][(f, worker)],  # type: ignore[index]
                    workerCost=data["cost"][(f, worker)], **{"lambda": data["lam"][(f, worker, k)]},  # type: ignore[index]
                    muCurrentStage=data["mu"][(f, worker, k)], recoveryMu=selected["recoveryMu"],  # type: ignore[index]
                    r=data["maximum_increase"][k], delta=data["maximum_increase"][k] / (data["lam"][(f, worker, k)] * math.log(2.0)),  # type: ignore[index,operator]
                    baseProcessing=selected["baseProcessing"], baseSetup=selected["baseSetup"],
                    baseAT=selected["baseAT"], fatigueMultiplier=selected["fatigueMultiplier"],
                    actualProcessing=selected["actualProcessing"], actualSetup=selected["actualSetup"],
                    actualAT=selected["actualAT"], end=selected["end"], fatigueAfter=selected["fatigueAfter"],
                    energy=energy, cost=op_cost, safeThresholdExceeded=str(safe).lower()))
                sequence += 1

    makespan = 0.0
    for f in range(factories):
        factory_makespan[f] = max(completion[job][stages - 1] for job in factory_jobs[f])
        makespan = max(makespan, factory_makespan[f])
    final_values = []
    for f, count in data["worker_counts"].items():  # type: ignore[union-attr]
        for worker in range(count):
            final_fatigue = worker_fatigue[(f, worker)]
            if recovery_enabled and worker_used[(f, worker)] and makespan > worker_available[(f, worker)]:
                tail = makespan - worker_available[(f, worker)]
                tail_mu = data["mu"][(f, worker, worker_last_stage[(f, worker)])]  # type: ignore[index]
                excess += excess_recovery(final_fatigue, tail_mu, tail, 0.8)
                high_time += time_above_recovery(final_fatigue, tail_mu, tail, 0.8)
                final_fatigue = recover(final_fatigue, tail_mu, tail)
            final_values.append(final_fatigue)
    mean_final = sum(final_values) / len(final_values)
    variance = max(0.0, sum(value * value for value in final_values) / len(final_values) - mean_final * mean_final)
    objectives = dict(
        Cmax=makespan, TEC=sum(factory_energy), TWC=sum(factory_cost),
        Fmax=fatigue_max, Favg=fatigue_sum / len(records), FE=excess,
        VarFw=variance, highFatigueRatio=high_time / (len(final_values) * makespan),
        longestContinuousWork=max(longest.values()), totalNaturalRecovery=natural_recovery,
        safeEvents=float(safe_events))
    return records, objectives


def compare_trace(manual: list[dict[str, object]], program_path: Path, output: Path, mode: str) -> list[dict[str, object]]:
    program = read_csv(program_path)
    if len(manual) != len(program):
        raise AssertionError(f"{mode}: operation count differs {len(manual)} != {len(program)}")
    comparisons: list[dict[str, object]] = []
    failures = []
    for manual_row, program_row in zip(manual, program):
        sequence = int(manual_row["sequence"])
        for field in IDENTITY_TRACE_FIELDS:
            left, right = int(manual_row[field]), int(program_row[field])
            passed = left == right
            comparisons.append(dict(mode=mode, sequence=sequence, field=field,
                                    manualValue=left, programValue=right,
                                    absoluteError=abs(left - right), passed=str(passed).lower()))
            if not passed:
                failures.append((sequence, field, left, right))
        for field in NUMERIC_TRACE_FIELDS:
            left, right = float(manual_row[field]), float(program_row[field])
            error = abs(left - right)
            passed = error <= TOLERANCE
            comparisons.append(dict(mode=mode, sequence=sequence, field=field,
                                    manualValue=format(left, ".17g"), programValue=format(right, ".17g"),
                                    absoluteError=format(error, ".17g"), passed=str(passed).lower()))
            if not passed:
                failures.append((sequence, field, left, right))
    if failures:
        raise AssertionError(f"{mode}: trace comparison failures: {failures[:5]}")
    return comparisons


def compare_objectives(manual: dict[str, float], program_path: Path, mode: str) -> list[dict[str, object]]:
    program = {row["metric"]: float(row["value"]) for row in read_csv(program_path)}
    rows = []
    failures = []
    for metric, manual_value in manual.items():
        program_value = program[metric]
        error = abs(manual_value - program_value)
        passed = error <= TOLERANCE
        rows.append(dict(mode=mode, metric=metric, manualValue=format(manual_value, ".17g"),
                         programValue=format(program_value, ".17g"), absoluteError=format(error, ".17g"),
                         passed=str(passed).lower()))
        if not passed:
            failures.append((metric, manual_value, program_value))
    if failures:
        raise AssertionError(f"{mode}: objective comparison failures: {failures}")
    return rows


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence-root", required=True, type=Path)
    args = parser.parse_args()
    root = args.evidence_root.resolve()
    output = root / "03_manual_validation"
    data = load_inputs(root)
    all_trace_comparisons: list[dict[str, object]] = []
    all_objective_comparisons: list[dict[str, object]] = []
    trace_fields = list(IDENTITY_TRACE_FIELDS + NUMERIC_TRACE_FIELDS) + ["safeThresholdExceeded"]
    for mode, directory in (("FM3", "02_decoder_fm3"), ("FM0", "04_fm0_regression")):
        manual_trace, manual_objectives = decode(data, mode)
        write_csv(output / f"manual_trace_{mode.lower()}.csv", manual_trace, trace_fields)
        objective_rows = [dict(metric=metric, value=format(value, ".17g")) for metric, value in manual_objectives.items()]
        write_csv(output / f"manual_objectives_{mode.lower()}.csv", objective_rows, ["metric", "value"])
        all_trace_comparisons.extend(compare_trace(
            manual_trace, root / directory / "program_trace.csv", output, mode))
        all_objective_comparisons.extend(compare_objectives(
            manual_objectives, root / directory / "objective_breakdown.csv", mode))
    write_csv(output / "manual_vs_program.csv", all_trace_comparisons,
              ["mode", "sequence", "field", "manualValue", "programValue", "absoluteError", "passed"])
    write_csv(output / "manual_objectives_vs_program.csv", all_objective_comparisons,
              ["mode", "metric", "manualValue", "programValue", "absoluteError", "passed"])
    max_trace_error = max(float(row["absoluteError"]) for row in all_trace_comparisons)
    max_objective_error = max(float(row["absoluteError"]) for row in all_objective_comparisons)
    summary = (
        "manualDecoder=independent_python_reconstruction\n"
        "javaDecoderInvoked=false\n"
        f"operationsPerMode=20\n"
        f"traceComparisons={len(all_trace_comparisons)}\n"
        f"objectiveComparisons={len(all_objective_comparisons)}\n"
        f"tolerance={TOLERANCE:.1e}\n"
        f"maximumTraceAbsoluteError={max_trace_error:.17g}\n"
        f"maximumObjectiveAbsoluteError={max_objective_error:.17g}\n"
        "manual_decoder_validation_passed=true\n"
        "objective_reconstruction_passed=true\n"
    )
    (output / "validation_summary.properties").write_text(summary, encoding="utf-8", newline="\n")
    print(summary, end="")


if __name__ == "__main__":
    main()
