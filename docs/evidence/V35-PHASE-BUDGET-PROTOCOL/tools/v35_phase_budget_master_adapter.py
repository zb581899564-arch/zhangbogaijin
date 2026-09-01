#!/usr/bin/env python3
"""External Phase-Consistent Budget Termination adapter for frozen V35 runs.

This utility deliberately wraps, rather than edits, the frozen Stage2 master
renderer.  It adds only post-run evidence and fail-closed budget checks after
the Java runner has already returned.  It never changes a run command, jar,
configuration, population, random source, or algorithm decision.
"""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Mapping


PROTOCOL = "PHASE_CONSISTENT_BUDGET_TERMINATION"
VERSION = "v35-phase-consistent-budget-v1"
DEFAULT_POPULATION = 100
DEFAULT_Q_TIMES = 50


class PhaseBudgetError(RuntimeError):
    """The external budget protocol could not establish a valid run."""


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def parse_properties(path: Path) -> dict[str, str]:
    if not path.is_file():
        raise PhaseBudgetError("required properties file is missing: " + str(path))
    result: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise PhaseBudgetError("malformed properties line in " + str(path))
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def required_int(values: Mapping[str, str], key: str) -> int:
    try:
        return int(values[key])
    except (KeyError, ValueError) as error:
        raise PhaseBudgetError("missing or invalid integer " + key) from error


@dataclass(frozen=True)
class BudgetDecision:
    requested_max_fe: int
    actual_fe: int
    decoder_calls: int
    population: int
    q_times: int
    remaining_fe: int
    q_phase_fe: int
    utilization_rate: float
    termination_kind: str
    accepted: bool
    failure_reason: str


def classify_budget(requested_max_fe: int, actual_fe: int, decoder_calls: int,
                    population: int = DEFAULT_POPULATION,
                    q_times: int = DEFAULT_Q_TIMES) -> BudgetDecision:
    q_phase_fe = population * q_times
    remaining = requested_max_fe - actual_fe
    failure = "NONE"
    kind = "PHASE_CONSISTENT_TAIL_STOP"
    if requested_max_fe <= 0:
        failure = "REQUESTED_MAX_FE_NON_POSITIVE"
    elif population <= 0 or q_times <= 0:
        failure = "Q_PHASE_CONFIGURATION_NON_POSITIVE"
    elif actual_fe <= 0:
        failure = "ZERO_OR_NEGATIVE_ACTUAL_FE"
    elif decoder_calls != actual_fe:
        failure = "DECODER_CALLS_DO_NOT_CLOSE"
    elif actual_fe > requested_max_fe:
        failure = "OVER_MAX_FE"
    elif remaining == 0:
        kind = "EXACT_MAX_FE"
    elif remaining >= q_phase_fe:
        failure = "TAIL_NOT_SHORTER_THAN_Q_PHASE"
    if failure != "NONE":
        kind = "INVALID"
    return BudgetDecision(requested_max_fe, actual_fe, decoder_calls, population, q_times,
                          remaining, q_phase_fe,
                          0.0 if requested_max_fe <= 0 else actual_fe / requested_max_fe,
                          kind, failure == "NONE", failure)


def write_evidence_manifest(directory: Path) -> None:
    rows: list[tuple[str, str]] = []
    for path in sorted(directory.rglob("*")):
        if path.is_file() and path.name != "evidence-sha256.tsv":
            rows.append((path.relative_to(directory).as_posix(), sha256_file(path)))
    text = "path\tsha256\n" + "".join(path + "\t" + digest + "\n" for path, digest in rows)
    (directory / "evidence-sha256.tsv").write_text(text, encoding="utf-8")


def summarize_formal_rounds(status: Mapping[str, str]) -> tuple[int, int]:
    summary = status.get("mechanismSummary", "")
    values: dict[str, int] = {}
    for item in summary.split(","):
        if "=" not in item:
            continue
        key, raw = item.split("=", 1)
        try:
            values[key] = int(raw)
        except ValueError:
            continue
    return values.get("formalOuterCycles", 0), values.get("formalQgRounds", 0)


def write_budget_termination(output: Path, requested_max_fe: int,
                             expected_jar_sha256: str | None = None,
                             expected_config_sha256: str | None = None,
                             expected_snapshot_sha256: str | None = None,
                             population: int = DEFAULT_POPULATION,
                             q_times: int = DEFAULT_Q_TIMES) -> BudgetDecision:
    status = parse_properties(output / "status.properties")
    if status.get("status") != "COMPLETED":
        raise PhaseBudgetError("formal status is not COMPLETED")
    decision = classify_budget(requested_max_fe, required_int(status, "fullEvaluations"),
                               required_int(status, "decoderCalls"), population, q_times)
    outer_cycles, qg_rounds = summarize_formal_rounds(status)
    provenance = parse_properties(output / "provenance.properties")
    configuration = (output / "configuration.txt").read_text(encoding="utf-8")
    if "populationSize=100" not in configuration or "maxEvaluations={}".format(requested_max_fe) not in configuration:
        raise PhaseBudgetError("formal configuration does not bind expected population/max FE")
    if "formalBaseline.qTimes=50" not in configuration:
        raise PhaseBudgetError("formal configuration does not bind Table 9 qTimes=50")
    # Jar/config identity is already hash-bound by the frozen renderer's
    # manifest validation.  Older Java configuration text does not repeat
    # those digest literals, so this overlay records the independently
    # validated manifest identities rather than inventing a second text gate.
    recorded_snapshot = provenance.get("initialPopulationSnapshotSha256", "")
    if expected_snapshot_sha256 and recorded_snapshot.lower() != expected_snapshot_sha256.lower():
        raise PhaseBudgetError("initial-population snapshot SHA mismatch")
    text = "\n".join((
        "budgetProtocol=" + PROTOCOL,
        "budgetProtocolVersion=" + VERSION,
        "requestedMaxFE=" + str(decision.requested_max_fe),
        "actualFE=" + str(decision.actual_fe),
        "decoderCalls=" + str(decision.decoder_calls),
        "remainingFE=" + str(decision.remaining_fe),
        "population=" + str(decision.population),
        "qTimes=" + str(decision.q_times),
        "qPhaseFE=" + str(decision.q_phase_fe),
        "utilizationRate={:.12f}".format(decision.utilization_rate),
        "terminationKind=" + decision.termination_kind,
        "phaseBoundAccepted=" + str(decision.accepted).lower(),
        "phaseBoundFailure=" + decision.failure_reason,
        "formalOuterCycles=" + str(outer_cycles),
        "formalQgRounds=" + str(qg_rounds),
        "jarSha256=" + (expected_jar_sha256 or "UNBOUND"),
        "configSha256=" + (expected_config_sha256 or "UNBOUND"),
        "snapshotSha256=" + (expected_snapshot_sha256 or recorded_snapshot or "UNBOUND"),
        "",))
    (output / "budget-termination.properties").write_text(text, encoding="utf-8")
    write_evidence_manifest(output)
    if not decision.accepted:
        raise PhaseBudgetError("phase-bound budget gate failed: " + decision.failure_reason)
    return decision


def audit_group(entries: list[dict[str, Any]], report_directory: Path) -> dict[str, Any]:
    if len(entries) != 5:
        raise PhaseBudgetError("a formal fairness group must contain exactly five arms")
    requested = {entry["decision"].requested_max_fe for entry in entries}
    populations = {entry["decision"].population for entry in entries}
    phases = {entry["decision"].q_phase_fe for entry in entries}
    initial = {entry["initialPopulationHash"] for entry in entries}
    actual = [entry["decision"].actual_fe for entry in entries]
    accepted = all(entry["decision"].accepted for entry in entries)
    group_valid = (accepted and len(requested) == 1 and len(populations) == 1 and len(phases) == 1
                   and len(initial) == 1 and max(actual) - min(actual) < next(iter(phases)))
    reason = "NONE" if group_valid else "GROUP_PHASE_FAIRNESS_VIOLATION"
    report_directory.mkdir(parents=True, exist_ok=True)
    csv = ["arm,initialPopulationHash,actualFE,decoderCalls,remainingFE,qPhaseFE,utilizationRate,terminationKind,accepted"]
    for entry in sorted(entries, key=lambda value: value["arm"]):
        decision: BudgetDecision = entry["decision"]
        csv.append(",".join((entry["arm"], entry["initialPopulationHash"], str(decision.actual_fe),
            str(decision.decoder_calls), str(decision.remaining_fe), str(decision.q_phase_fe),
            "{:.12f}".format(decision.utilization_rate), decision.termination_kind,
            str(decision.accepted).lower())))
    (report_directory / "budget-utilization.csv").write_text("\n".join(csv) + "\n", encoding="utf-8")
    properties = "\n".join(("budgetProtocol=" + PROTOCOL, "budgetProtocolVersion=" + VERSION,
        "groupStatus=" + ("VALID" if group_valid else "INVALID"),
        "groupFailure=" + reason, "actualFERange=" + str(max(actual) - min(actual)),
        "qPhaseFE=" + str(next(iter(phases))), "sameInitialPopulation=" + str(len(initial) == 1).lower(), ""))
    (report_directory / "group-budget-audit.properties").write_text(properties, encoding="utf-8")
    write_evidence_manifest(report_directory)
    if not group_valid:
        raise PhaseBudgetError(reason)
    return {"groupStatus": "VALID", "actualFERange": max(actual) - min(actual),
            "qPhaseFE": next(iter(phases)), "members": len(entries)}


def load_renderer(path: Path) -> Any:
    if not path.is_file():
        raise PhaseBudgetError("frozen master renderer is missing: " + str(path))
    spec = importlib.util.spec_from_file_location("v35_frozen_master_renderer", path)
    if spec is None or spec.loader is None:
        raise PhaseBudgetError("cannot load frozen master renderer")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def group_key(run: Mapping[str, Any]) -> tuple[str, int]:
    return str(run["instance"]), int(run["seed"])


def collect_group_entries(renderer: Any, state_dir: Path, runs: list[Mapping[str, Any]], jar_sha: str) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    for run in runs:
        run_key = renderer.run_key(run, jar_sha.lower())
        complete = json.loads((state_dir / "runs" / run_key / "completed.json").read_text(encoding="utf-8"))
        output = Path(complete["output"])
        values = parse_properties(output / "budget-termination.properties")
        decision = classify_budget(required_int(values, "requestedMaxFE"), required_int(values, "actualFE"),
                                   required_int(values, "decoderCalls"), required_int(values, "population"),
                                   required_int(values, "qTimes"))
        status = parse_properties(output / "status.properties")
        entries.append({"arm": str(run["arm"]), "decision": decision,
                        "initialPopulationHash": status["initialPopulationHash"]})
    return entries


def collect_preflight_entries(root: Path) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    for budget_path in sorted(root.rglob("budget-termination.properties")):
        output = budget_path.parent
        values = parse_properties(budget_path)
        status = parse_properties(output / "status.properties")
        gate_path = output / "preflight-gate.properties"
        gate = parse_properties(gate_path) if gate_path.is_file() else {}
        entries.append({"arm": values.get("arm") or gate.get("arm") or output.parent.name,
                        "decision": classify_budget(required_int(values, "requestedMaxFE"),
                                                    required_int(values, "actualFE"),
                                                    required_int(values, "decoderCalls"),
                                                    required_int(values, "population"),
                                                    required_int(values, "qTimes")),
                        "initialPopulationHash": status["initialPopulationHash"]})
    return entries


def instance_scale(instance: str) -> str:
    return instance.split("_", 1)[0] + "-job" if "_" in instance else "UNKNOWN"


def median(values: list[float]) -> float:
    ordered = sorted(values)
    midpoint = len(ordered) // 2
    return ordered[midpoint] if len(ordered) % 2 else (ordered[midpoint - 1] + ordered[midpoint]) / 2.0


def write_campaign_utilization(state_dir: Path) -> Path:
    rows: list[tuple[str, str, int, float, str]] = []
    for properties_path in sorted(state_dir.rglob("budget-termination.properties")):
        values = parse_properties(properties_path)
        output = properties_path.parent
        formal = parse_properties(output / "formal-gate.properties")
        provenance = parse_properties(output / "provenance.properties")
        arm = formal.get("arm", "UNKNOWN")
        instance = provenance.get("instanceId", "UNKNOWN")
        rows.append((arm, instance_scale(instance), required_int(values, "actualFE"),
                     float(values["utilizationRate"]), values["terminationKind"]))
    grouped: dict[tuple[str, str], list[tuple[int, float, str]]] = {}
    for arm, scale, actual, utilization, kind in rows:
        grouped.setdefault((arm, scale), []).append((actual, utilization, kind))
    output = state_dir / "budget-utilization-summary.csv"
    text = ["arm,instanceScale,runCount,actualFEMin,actualFEMedian,actualFEMax,utilizationMin,utilizationMedian,utilizationMax,exactCount,phaseTailCount"]
    for (arm, scale), values in sorted(grouped.items()):
        actual = [float(item[0]) for item in values]
        utilization = [item[1] for item in values]
        exact = sum(item[2] == "EXACT_MAX_FE" for item in values)
        phase_tail = sum(item[2] == "PHASE_CONSISTENT_TAIL_STOP" for item in values)
        text.append("{},{},{},{:.0f},{:.6f},{:.0f},{:.12f},{:.12f},{:.12f},{},{}".format(
            arm, scale, len(values), min(actual), median(actual), max(actual), min(utilization),
            median(utilization), max(utilization), exact, phase_tail))
    output.write_text("\n".join(text) + "\n", encoding="utf-8")
    return output


def run_grouped_campaign(manifest_path: Path, renderer_path: Path, state_dir: Path,
                         resume: bool, retry_failed: bool, dry_run: bool) -> dict[str, Any]:
    renderer = load_renderer(renderer_path)
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    normalized = renderer.validate_manifest(manifest)
    original_fingerprint = renderer.output_fingerprint

    def phase_fingerprint(output: Path) -> str:
        write_budget_termination(output, 500000, manifest["jarSha256"],
                                 manifest["frozenConfigSha256"])
        return original_fingerprint(output)

    renderer.output_fingerprint = phase_fingerprint
    grouped: dict[tuple[str, int], list[dict[str, Any]]] = {}
    for run in normalized:
        grouped.setdefault(group_key(run), []).append(run)
    group_state: list[dict[str, Any]] = []
    for (instance, seed), runs in sorted(grouped.items()):
        if len(runs) != 5:
            raise PhaseBudgetError("manifest group {} / {} does not contain five arms".format(instance, seed))
        subset = dict(manifest)
        subset["runs"] = runs
        group_id = "{}-{}".format(instance, seed)
        group_dir = state_dir / "groups" / group_id
        group_dir.mkdir(parents=True, exist_ok=True)
        subset_path = group_dir / "manifest.json"
        subset_path.write_text(json.dumps(subset, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n", encoding="utf-8")
        result = renderer.run_campaign(subset_path, group_dir / "master", resume, retry_failed, dry_run)
        record: dict[str, Any] = {"instance": instance, "seed": seed, "renderer": result}
        if not dry_run:
            if result["failed"] or result["completed"] + result["skipped"] != 5:
                record["budgetGroupStatus"] = "INVALID"
                group_state.append(record)
                raise PhaseBudgetError("frozen master did not complete group {} / {}".format(instance, seed))
            entries = collect_group_entries(renderer, group_dir / "master", runs, manifest["jarSha256"])
            record["budgetAudit"] = audit_group(entries, group_dir / "budget-audit")
            record["budgetGroupStatus"] = "VALID"
        group_state.append(record)
        (state_dir / "phase-budget-group-state.json").write_text(
            json.dumps(group_state, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    if not dry_run:
        write_campaign_utilization(state_dir)
    return {"protocol": VERSION, "groups": len(group_state), "groupState": str(state_dir / "phase-budget-group-state.json"),
            "dryRun": dry_run}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="V35 Phase-Consistent Budget Termination adapter")
    parser.add_argument("--manifest", type=Path)
    parser.add_argument("--frozen-master", type=Path)
    parser.add_argument("--state-dir", type=Path)
    parser.add_argument("--audit-preflight-root", type=Path,
                        help="audit five completed external-preflight outputs without running Java")
    parser.add_argument("--group-report", type=Path)
    parser.add_argument("--write-evidence-manifest", type=Path,
                        help="write a SHA-256 manifest for an existing evidence directory")
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--retry-failed", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.write_evidence_manifest:
            write_evidence_manifest(args.write_evidence_manifest)
            print(str(args.write_evidence_manifest / "evidence-sha256.tsv"))
            return 0
        if args.audit_preflight_root:
            if not args.group_report:
                raise PhaseBudgetError("--group-report is required with --audit-preflight-root")
            result = audit_group(collect_preflight_entries(args.audit_preflight_root), args.group_report)
            print(json.dumps(result, ensure_ascii=False, sort_keys=True))
            return 0
        if not args.manifest or not args.frozen_master or not args.state_dir:
            raise PhaseBudgetError("--manifest --frozen-master --state-dir are required for a master campaign")
        print(json.dumps(run_grouped_campaign(args.manifest, args.frozen_master, args.state_dir,
                                                args.resume, args.retry_failed, args.dry_run),
                         ensure_ascii=False, sort_keys=True))
        return 0
    except (PhaseBudgetError, OSError, ValueError, KeyError, RuntimeError) as error:
        print("V35_PHASE_BUDGET_ADAPTER_ERROR: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
