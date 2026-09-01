#!/usr/bin/env python3
"""Read-only audit of the archived Stage2 A2/A3 paired runs.

The script reads restored files and ZIP members only.  It never invokes Java,
Maven, SSH, or a remote runner.  All generated files are written below the
audit directory supplied by ``--output-dir``.
"""

from __future__ import annotations

import argparse
import csv
import io
import json
import math
import re
import statistics
import zipfile
from pathlib import Path
from typing import Dict, Iterable, List, Mapping, MutableMapping, Sequence


SEEDS = [f"202608{n:02d}" for n in range(8, 20)]
ARMS = ("A2", "A3")
INSTANCE = "100_2_3_1"
ARCHIVE_TOP = "zhangbo-v35-stage2-master-v2-20260823"
FORMAL_RUN_ROOT = (
    "results/formal-a0-a4-4500/100_2_3_1"
)


def read_lines(path: Path) -> List[str]:
    return path.read_text(encoding="utf-8-sig").splitlines()


def read_properties(path: Path) -> Dict[str, str]:
    result: Dict[str, str] = {}
    for raw in read_lines(path):
        line = raw.strip()
        if not line or line.startswith("#") or line.startswith("!"):
            continue
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def read_properties_text(text: str) -> Dict[str, str]:
    result: Dict[str, str] = {}
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or line.startswith("!"):
            continue
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def parse_mechanism(value: str) -> Dict[str, str]:
    """Parse comma-delimited mechanism summary without splitting dscr pipes."""
    result: Dict[str, str] = {}
    matches = list(re.finditer(r"(?:^|,)([A-Za-z][A-Za-z0-9]*)=", value))
    for index, match in enumerate(matches):
        start = match.end()
        end = matches[index + 1].start() if index + 1 < len(matches) else len(value)
        result[match.group(1)] = value[start:end].rstrip(",")
    return result


def parse_number(value: str):
    if value is None or value == "":
        return ""
    try:
        if re.fullmatch(r"[-+]?\d+", value):
            return int(value)
        if re.fullmatch(r"[-+]?(?:\d+\.\d*|\d*\.\d+|\d+)(?:[Ee][-+]?\d+)?", value):
            return float(value)
    except ValueError:
        pass
    return value


def truth(value: str) -> bool:
    return str(value).strip().lower() in {"true", "yes", "1"}


def number(properties: Mapping[str, str], key: str):
    return parse_number(properties.get(key, ""))


def bool_text(properties: Mapping[str, str], key: str) -> str:
    value = properties.get(key, "")
    return str(truth(value)).lower() if value != "" else ""


def csv_rows(path: Path) -> List[Dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def csv_rows_text(text: str) -> List[Dict[str, str]]:
    return list(csv.DictReader(io.StringIO(text)))


def zip_text(archive: zipfile.ZipFile, name: str) -> str:
    return archive.read(name).decode("utf-8-sig", errors="replace")


def inspect_large_events(path: Path) -> Dict[str, object]:
    result: Dict[str, object] = {
        "large_events_present": path.is_file(),
        "large_events_zip_bytes": path.stat().st_size if path.is_file() else "",
        "large_events_members": "",
        "qp_raw_events_present": False,
        "q_table_raw_present": False,
        "personal_archive_log_present": False,
        "cmax_records": "",
        "cmax_generated_rows": "",
        "cmax_entered_rows": "",
        "cmax_pddr_retained_rows": "",
        "cmax_personal_archive_rows": "",
        "cmax_global_archive_rows": "",
        "cmax_next_round_survival_rows": "",
        "passive_archive_rows": "",
        "dscr_event_rows": "",
        "dscr_teacher_use_rows": "",
        "cmax_best_generated": "",
        "cmax_best_global": "",
        "cmax_record_summary_json": "",
    }
    if not path.is_file():
        return result

    with zipfile.ZipFile(path) as archive:
        names = sorted(name for name in archive.namelist() if not name.endswith("/"))
        result["large_events_members"] = ";".join(names)
        result["qp_raw_events_present"] = any(
            re.search(r"(?i)(?:^|/)(?:qp|q[-_]?pbest|q[-_]?table|pbest[-_]?events?)", name)
            for name in names
        )
        result["q_table_raw_present"] = any(
            re.search(r"(?i)(?:q[-_]?table|q[-_]?values)", name) for name in names
        )
        result["personal_archive_log_present"] = any(
            re.search(r"(?i)(?:personal[-_]?archive|archive[-_]?updates?)", name)
            for name in names
        )

        if "cmax-audit-records.csv" in names:
            rows = csv_rows_text(zip_text(archive, "cmax-audit-records.csv"))
            result["cmax_records"] = len(rows)
            result["cmax_generated_rows"] = sum(truth(row.get("generated", "")) for row in rows)
            result["cmax_entered_rows"] = sum(truth(row.get("enteredCandidateSet", "")) for row in rows)
            result["cmax_pddr_retained_rows"] = sum(truth(row.get("pddrRetained", "")) for row in rows)
            result["cmax_personal_archive_rows"] = sum(truth(row.get("personalArchive", "")) for row in rows)
            result["cmax_global_archive_rows"] = sum(truth(row.get("globalArchive", "")) for row in rows)
            result["cmax_next_round_survival_rows"] = sum(
                str(row.get("nextRoundSurvival", "")).strip().lower() == "survived" for row in rows
            )
        if "passive-archive.csv" in names:
            result["passive_archive_rows"] = len(csv_rows_text(zip_text(archive, "passive-archive.csv")))
        if "dscr-events.csv" in names:
            result["dscr_event_rows"] = max(0, len(csv_rows_text(zip_text(archive, "dscr-events.csv"))))
        if "dscr-teacher-uses.csv" in names:
            result["dscr_teacher_use_rows"] = max(0, len(csv_rows_text(zip_text(archive, "dscr-teacher-uses.csv"))))
        if "cmax-audit-summary.txt" in names:
            summary = read_properties_text(zip_text(archive, "cmax-audit-summary.txt"))
            result["cmax_best_generated"] = parse_number(summary.get("bestCmaxGenerated", ""))
            result["cmax_best_global"] = parse_number(summary.get("bestCmaxGlobal", ""))
            result["cmax_record_summary_json"] = json.dumps(summary, ensure_ascii=False, sort_keys=True)
    return result


def run_dir(restored_root: Path, seed: str, arm: str) -> Path:
    return restored_root / ARCHIVE_TOP / FORMAL_RUN_ROOT / f"seed-{seed}" / arm


def run_record(restored_root: Path, seed: str, arm: str) -> Dict[str, object]:
    directory = run_dir(restored_root, seed, arm)
    profile_path = directory / "profile.txt"
    status_path = directory / "status.properties"
    provenance_path = directory / "provenance.properties"
    budget_path = directory / "budget-termination.properties"
    gate_path = directory / "formal-gate.properties"
    profile = read_properties(profile_path) if profile_path.is_file() else {}
    status = read_properties(status_path) if status_path.is_file() else {}
    provenance = read_properties(provenance_path) if provenance_path.is_file() else {}
    budget = read_properties(budget_path) if budget_path.is_file() else {}
    gate = read_properties(gate_path) if gate_path.is_file() else {}
    mechanism = parse_mechanism(status.get("mechanismSummary", ""))
    dscr = read_properties_text(mechanism.get("dscr", "").replace("|", "\n"))
    events = inspect_large_events(directory / "large-events.zip")

    row: Dict[str, object] = {
        "seed": seed,
        "arm": arm,
        "run_path": str(directory.relative_to(restored_root)).replace("\\", "/"),
        "run_files_present": all(path.is_file() for path in (profile_path, status_path, provenance_path, budget_path, gate_path)),
        "profile_json": json.dumps(profile, ensure_ascii=False, sort_keys=True),
        "provenance_json": json.dumps(provenance, ensure_ascii=False, sort_keys=True),
        "budget_json": json.dumps(budget, ensure_ascii=False, sort_keys=True),
        "status": status.get("status", ""),
        "mode": status.get("mode", ""),
        "fullEvaluations": number(status, "fullEvaluations"),
        "decoderCalls": number(status, "decoderCalls"),
        "illegalSolutions": number(status, "illegalSolutions"),
        "duplicateEvaluations": number(status, "duplicateEvaluations"),
        "initialPopulationHash": status.get("initialPopulationHash", ""),
        "stopReason": status.get("stopReason", ""),
        "profile_qp": profile.get("qp", ""),
        "profile_cfvf": profile.get("cfvf", ""),
        "profile_qg": profile.get("qg", ""),
        "profile_dscr": profile.get("dscr", ""),
        "profile_pddrSelectionMode": profile.get("pddrSelectionMode", ""),
        "profile_maxEvaluations": number(profile, "maxEvaluations"),
        "dualQ_mode": profile.get("dualQ.mode", "NOT_APPLICABLE"),
        "dualQ_warmupRatio": number(profile, "dualQ.warmupRatio"),
        "dualQ_blockLength": number(profile, "dualQ.blockLength"),
        "dualQ_gBlockLength": number(profile, "dualQ.gBlockLength"),
        "dualQ_frozenSelectionPolicy": profile.get("dualQ.frozenSelectionPolicy", "NOT_APPLICABLE"),
        "frozenJarSha256": provenance.get("frozenJarSha256", ""),
        "snapshotSha256": provenance.get("snapshotSha256", ""),
        "initialPopulationHashV35": provenance.get("initialPopulationHashV35", ""),
        "instanceSha256": provenance.get("instanceSha256", ""),
        "setupFileSha256": provenance.get("setupFileSha256", ""),
        "fatigueFileSha256": provenance.get("fatigueFileSha256", ""),
        "setupConfigurationSha256": provenance.get("setupConfigurationSha256", ""),
        "fatigueConfigurationSha256": provenance.get("fatigueConfigurationSha256", ""),
        "problemConfigurationSha256": provenance.get("problemConfigurationSha256", ""),
        "requestedMaxFE": number(budget, "requestedMaxFE"),
        "actualFE": number(budget, "actualFE"),
        "remainingFE": number(budget, "remainingFE"),
        "utilizationRate": number(budget, "utilizationRate"),
        "terminationKind": budget.get("terminationKind", ""),
        "phaseBoundAccepted": budget.get("phaseBoundAccepted", ""),
        "phaseBoundFailure": budget.get("phaseBoundFailure", ""),
        "formalOuterCycles": number(budget, "formalOuterCycles"),
        "formalQgRounds": number(budget, "formalQgRounds"),
        "formalGateStatus": gate.get("status", ""),
        "formalGateActualFE": number(gate, "actualFE"),
        "formalGateFrontSize": number(gate, "frontSize"),
        "includedInFormalStatistics": gate.get("includedInFormalStatistics", ""),
        "includedInReferenceFront": gate.get("includedInReferenceFront", ""),
        "formalGateFailures": gate.get("failures", ""),
        "p6EventsTotal": number(mechanism, "p6EventsTotal"),
        "p6EventsRetained": number(mechanism, "p6EventsRetained"),
        "fixedNeighborhoodEvents": number(mechanism, "fixedNeighborhoodEvents"),
        "pddrEvents": number(mechanism, "pddrEvents"),
        "qgSelections": number(mechanism, "qgSelections"),
        "qgTdUpdates": number(mechanism, "qgTdUpdates"),
        "qpActions": number(mechanism, "qpActions"),
        "qpTransitions": number(mechanism, "qpTransitions"),
        "cfvfOffspring": number(mechanism, "cfvfOffspring"),
        "cfvfRepairs": number(mechanism, "cfvfRepairs"),
        "archiveInsertions": number(mechanism, "archiveInsertions"),
        "formalLocalFE": number(mechanism, "formalLocalFE"),
        "dualQWarmup": number(mechanism, "dualQWarmup"),
        "dualQP": number(mechanism, "dualQP"),
        "dualQG": number(mechanism, "dualQG"),
        "algorithmRunNanos": number(mechanism, "algorithmRunNanos"),
        "dscrTeacherUses": number(dscr, "teacherUses"),
        "dscrDominatedTeacherUses": number(dscr, "dominatedTeacherUses"),
        "dscrValidityChecks": number(dscr, "validityChecks"),
        "dscrReplacements": number(dscr, "replacements"),
        "dscrScrr": parse_number(dscr.get("scrr", "")),
    }
    row.update(events)
    return row


def median(values: Iterable[float]):
    values = list(values)
    return statistics.median(values) if values else ""


def write_csv(path: Path, rows: Sequence[Mapping[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        path.write_text("\n", encoding="utf-8")
        return
    keys: List[str] = []
    for row in rows:
        for key in row:
            if key not in keys:
                keys.append(key)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=keys, extrasaction="ignore")
        writer.writeheader()
        writer.writerows({key: row.get(key, "") for key in keys} for row in rows)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--restored-root", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()

    restored_root = args.restored_root.resolve()
    output_dir = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    rows = [run_record(restored_root, seed, arm) for seed in SEEDS for arm in ARMS]
    write_csv(output_dir / "run_inventory.csv", rows)
    large_event_fields = (
        "seed", "arm", "run_path", "large_events_present", "large_events_zip_bytes",
        "large_events_members", "qp_raw_events_present", "q_table_raw_present",
        "personal_archive_log_present", "cmax_records", "cmax_generated_rows",
        "cmax_entered_rows", "cmax_pddr_retained_rows", "cmax_personal_archive_rows",
        "cmax_global_archive_rows", "cmax_next_round_survival_rows", "passive_archive_rows",
        "dscr_event_rows", "dscr_teacher_use_rows", "cmax_best_generated", "cmax_best_global",
    )
    write_csv(
        output_dir / "large_events_inventory.csv",
        [{key: row.get(key, "") for key in large_event_fields} for row in rows],
    )

    by_key = {(str(row["seed"]), str(row["arm"])): row for row in rows}
    profile_pair_rows: List[Dict[str, object]] = []
    provenance_pair_rows: List[Dict[str, object]] = []
    budget_pair_rows: List[Dict[str, object]] = []
    profile_fields = (
        "profile_pddrSelectionMode", "profile_dscr", "profile_cfvf", "profile_qg",
        "profile_qp", "dualQ_mode", "dualQ_warmupRatio", "dualQ_blockLength",
        "dualQ_gBlockLength", "dualQ_frozenSelectionPolicy",
    )
    provenance_fields = (
        "frozenJarSha256", "snapshotSha256", "initialPopulationHashV35", "instanceSha256",
        "setupFileSha256", "fatigueFileSha256", "setupConfigurationSha256",
        "fatigueConfigurationSha256", "problemConfigurationSha256",
    )
    for seed in SEEDS:
        a2 = by_key[(seed, "A2")]
        a3 = by_key[(seed, "A3")]
        profile_row: Dict[str, object] = {"seed": seed}
        for field in profile_fields:
            profile_row[f"A2_{field}"] = a2.get(field, "")
            profile_row[f"A3_{field}"] = a3.get(field, "")
            profile_row[f"same_{field}"] = a2.get(field, "") == a3.get(field, "")
        profile_row["changed_profile_fields"] = ";".join(
            field for field in profile_fields if a2.get(field, "") != a3.get(field, "")
        )
        profile_pair_rows.append(profile_row)

        provenance_row: Dict[str, object] = {"seed": seed}
        mismatches = []
        for field in provenance_fields:
            provenance_row[f"A2_{field}"] = a2.get(field, "")
            provenance_row[f"A3_{field}"] = a3.get(field, "")
            same = a2.get(field, "") == a3.get(field, "")
            provenance_row[f"same_{field}"] = same
            if not same:
                mismatches.append(field)
        provenance_row["all_shared_hashes_match"] = not mismatches
        provenance_row["mismatched_fields"] = ";".join(mismatches)
        provenance_pair_rows.append(provenance_row)

        budget_pair_rows.append(
            {
                "seed": seed,
                "A2_actualFE": a2.get("actualFE", ""),
                "A3_actualFE": a3.get("actualFE", ""),
                "A2_terminationKind": a2.get("terminationKind", ""),
                "A3_terminationKind": a3.get("terminationKind", ""),
                "A2_phaseBoundFailure": a2.get("phaseBoundFailure", ""),
                "A3_phaseBoundFailure": a3.get("phaseBoundFailure", ""),
                "A2_utilizationRate": a2.get("utilizationRate", ""),
                "A3_utilizationRate": a3.get("utilizationRate", ""),
                "delta_actualFE_A3_minus_A2": (
                    a3["actualFE"] - a2["actualFE"]
                    if isinstance(a2.get("actualFE"), (int, float))
                    and isinstance(a3.get("actualFE"), (int, float))
                    else ""
                ),
                "both_phase_bound_clean": a2.get("phaseBoundFailure") == "NONE"
                and a3.get("phaseBoundFailure") == "NONE",
            }
        )
    write_csv(output_dir / "profile_pair_comparison.csv", profile_pair_rows)
    write_csv(output_dir / "provenance_pair_check.csv", provenance_pair_rows)
    write_csv(output_dir / "budget_pair_check.csv", budget_pair_rows)
    pilot_dir = restored_root / ARCHIVE_TOP / "pilot-a0-a4" / "output"
    pilot_metrics = csv_rows(pilot_dir / "metrics.csv") if (pilot_dir / "metrics.csv").is_file() else []
    pilot_pairs = csv_rows(pilot_dir / "paired-increments.csv") if (pilot_dir / "paired-increments.csv").is_file() else []
    pilot_a2_a3 = [row for row in pilot_metrics if row.get("arm") in ARMS]
    write_csv(output_dir / "pilot_metrics_a2_a3.csv", pilot_a2_a3)
    a2_metrics = {row.get("seed"): row for row in pilot_a2_a3 if row.get("arm") == "A2"}
    a3_metrics = {row.get("seed"): row for row in pilot_a2_a3 if row.get("arm") == "A3"}
    pair_rows: List[Dict[str, object]] = []
    for pair in pilot_pairs:
        if pair.get("fromArm") != "A2" or pair.get("toArm") != "A3":
            continue
        seed = pair.get("seed", "")
        a2 = a2_metrics.get(seed, {})
        a3 = a3_metrics.get(seed, {})
        pair_rows.append(
            {
                "seed": seed,
                "deltaHV": parse_number(pair.get("deltaHV", "")),
                "deltaIGD": parse_number(pair.get("deltaIGD", "")),
                "deltaCmax": parse_number(pair.get("deltaCmax", "")),
                "deltaTEC": parse_number(pair.get("deltaTEC", "")),
                "deltaTWC": parse_number(pair.get("deltaTWC", "")),
                "pilot_A2_actualFE": parse_number(a2.get("actualFE", "")),
                "pilot_A3_actualFE": parse_number(a3.get("actualFE", "")),
                "pilot_A2_utilization": parse_number(a2.get("utilization", "")),
                "pilot_A3_utilization": parse_number(a3.get("utilization", "")),
                "pilot_A2_rawN": parse_number(a2.get("rawN", "")),
                "pilot_A3_rawN": parse_number(a3.get("rawN", "")),
            }
        )
    write_csv(output_dir / "pilot_a2_a3_pairs.csv", pair_rows)

    comparison: List[Dict[str, object]] = []
    for pair in pair_rows:
        seed = str(pair["seed"])
        a2 = by_key[(seed, "A2")]
        a3 = by_key[(seed, "A3")]
        record: Dict[str, object] = {"seed": seed}
        for key in (
            "deltaHV", "deltaIGD", "deltaCmax", "deltaTEC", "deltaTWC",
            "pilot_A2_actualFE", "pilot_A3_actualFE", "pilot_A3_utilization",
        ):
            record[key] = pair.get(key, "")
        for key in (
            "actualFE", "utilizationRate", "qgTdUpdates", "qpActions", "qpTransitions",
            "archiveInsertions", "formalOuterCycles", "formalQgRounds", "fixedNeighborhoodEvents",
            "formalLocalFE", "dualQWarmup", "dualQP", "dualQG", "dscrReplacements",
            "cmax_records", "cmax_pddr_retained_rows", "cmax_personal_archive_rows",
            "cmax_global_archive_rows", "qp_raw_events_present", "personal_archive_log_present",
        ):
            record[f"A2_{key}"] = a2.get(key, "")
            record[f"A3_{key}"] = a3.get(key, "")
            if isinstance(a2.get(key), (int, float)) and isinstance(a3.get(key), (int, float)):
                record[f"delta_{key}"] = a3[key] - a2[key]
        comparison.append(record)
    write_csv(output_dir / "mechanism_comparison.csv", comparison)

    matrix = []
    for label, predicate, required in (
        ("status.properties", lambda r: bool(r.get("status")), 24),
        ("profile.txt", lambda r: bool(r.get("profile_qp")), 24),
        ("provenance.properties", lambda r: bool(r.get("frozenJarSha256")), 24),
        ("budget-termination.properties", lambda r: bool(r.get("requestedMaxFE")), 24),
        ("large-events.zip", lambda r: truth(str(r.get("large_events_present", ""))), 24),
        ("cmax-audit-records.csv in large-events.zip", lambda r: bool(r.get("cmax_records")), 24),
        ("Qp raw event log in large-events.zip", lambda r: truth(str(r.get("qp_raw_events_present", ""))), 24),
        ("Q table raw dump in large-events.zip", lambda r: truth(str(r.get("q_table_raw_present", ""))), 24),
        ("personal archive update log in large-events.zip", lambda r: truth(str(r.get("personal_archive_log_present", ""))), 24),
    ):
        present = sum(bool(predicate(row)) for row in rows)
        matrix.append(
            {
                "artifact": label,
                "present_runs": present,
                "expected_runs": required,
                "coverage": present / required,
                "status": "AVAILABLE" if present == required else ("MISSING" if present == 0 else "PARTIAL"),
                "interpretation": (
                    "available for paired audit"
                    if present == required and "raw" not in label and "update log" not in label
                    else "not exported; hashes/counts cannot replace event payloads"
                    if present == 0 and ("raw" in label or "update log" in label)
                    else "inspect missing runs before causal claim"
                ),
            }
        )
    write_csv(output_dir / "evidence_matrix.csv", matrix)

    shared_fields = (
        "frozenJarSha256", "snapshotSha256", "initialPopulationHashV35", "instanceSha256",
        "setupFileSha256", "fatigueFileSha256", "setupConfigurationSha256",
        "fatigueConfigurationSha256", "problemConfigurationSha256",
    )
    shared_mismatch = []
    for seed in SEEDS:
        a2 = by_key[(seed, "A2")]
        a3 = by_key[(seed, "A3")]
        for field in shared_fields:
            if a2.get(field) != a3.get(field):
                shared_mismatch.append(f"{seed}:{field}")

    delta_hv = [float(row["deltaHV"]) for row in pair_rows]
    delta_igd = [float(row["deltaIGD"]) for row in pair_rows]
    delta_cmax = [float(row["deltaCmax"]) for row in pair_rows]
    negative_hv = sum(value < 0 for value in delta_hv)
    negative_igd = sum(value < 0 for value in delta_igd)
    exact_budget = sum(
        str(row.get("terminationKind")) == "EXACT_MAX_FE" and str(row.get("phaseBoundFailure")) == "NONE"
        for row in rows
    )
    tail_budget = sum(
        str(row.get("terminationKind")) == "PHASE_CONSISTENT_TAIL_STOP"
        and str(row.get("phaseBoundFailure")) == "NONE"
        for row in rows
    )
    gate_pass = sum(
        str(row.get("status")) == "COMPLETED"
        and str(row.get("formalGateStatus")) == "COMPLETED"
        and str(row.get("formalGateFailures")) == "NONE"
        and str(row.get("includedInFormalStatistics")) == "true"
        and str(row.get("includedInReferenceFront")) == "true"
        for row in rows
    )
    qg_a2 = [float(by_key[(seed, "A2")]["qgTdUpdates"]) for seed in SEEDS]
    qg_a3 = [float(by_key[(seed, "A3")]["qgTdUpdates"]) for seed in SEEDS]
    qp_a3 = [float(by_key[(seed, "A3")]["qpActions"]) for seed in SEEDS]
    archive_a3 = [float(by_key[(seed, "A3")]["archiveInsertions"]) for seed in SEEDS]
    report = f"""# V35 Stage2 A2→A3 causal audit

审计时间：2026-08-24（本地只读分析）

## 范围与恢复

- G 盘归档：`G:\\ResearchArchive\\ZhangBo-V35-Paper-Evidence-20260823\\remote-campaigns\\zhangbo-v35-stage2-master-v2-20260823.tar.gz`
- 恢复目标：`{INSTANCE}`，seeds `20260808..20260819`，arms `A2/A3`。
- 恢复结果：24/24 个 run 目录，`large-events.zip` 24/24；另恢复 Stage2 pilot 聚合输出。
- 本报告由 `scripts/analyze_a2_a3.py` 生成；原始恢复文件未重写。

## 机制边界

A2/A3 共享 jar、snapshot、initial population、instance/setup/fatigue/problem hashes：配对 hash 不一致数为 `{len(shared_mismatch)}`。两臂 profile 都是 `GLOBAL_ORIGINAL` PDDR、DSCR+CFVF+Qg、FM3、500000 FE；A2 为 `qp=false` 且 dual-Q 不适用，A3 为 `qp=true` 并强制 `BLOCK_FROZEN` dual-Q (`warmupRatio=0.1`, `blockLength=5`, `gBlockLength=5`, `GREEDY`)。因此 A2→A3 的高层消融是 PA_i+Qp，但运行时必须把 Qp 与 block-frozen 双 Q 一并审计；PDDR 不是本对比中发生变化的变量。

## 运行完整性

- status/formal gate 完整通过：`{gate_pass}/24`。
- exact max-FE 且 phase bound 无失败：`{exact_budget}/24`；phase-consistent tail stop 且 phase bound 无失败：`{tail_budget}/24`。两类都要保留，不能把 tail stop 误报为运行失败。
- 运行层面需保留的异常字段（illegal/duplicate、stopReason、frontSize）见 `run_inventory.csv`；不要用聚合 pilot FE 反推本归档 formal run 的预算状态。

## 12 seed pilot 结果

`pilot_a2_a3_pairs.csv` 是归档内 pilot 聚合的 A2→A3 配对表。描述性结果：

- ΔHV 中位数 `{median(delta_hv):.6f}`，负向 `{negative_hv}/12`；
- ΔIGD 中位数 `{median(delta_igd):.6f}`，负向 `{negative_igd}/12`；
- ΔCmax 中位数 `{median(delta_cmax):.6f}`。

这只能证明 A3 bundle 在本 pilot 中非单调，不能单独证明 Qp action、personal archive 截断或 dual-Q 调度中的某一个是根因。

## 机制计数与日志充分性

从 24 个 formal status 可直接看到 A2 的 Qp/档案计数为零，A3 的 Qp action、transition、archive insertion 和 Qg TD 更新均有记录。逐 seed 计数见 `mechanism_comparison.csv`；A3 的 Qp action 中位数为 `{median(qp_a3):.0f}`，archive insertion 中位数为 `{median(archive_a3):.0f}`，A2/A3 Qg TD 更新中位数分别为 `{median(qg_a2):.0f}` / `{median(qg_a3):.0f}`。

`large-events.zip` 只包含 PDDR/Cmax/DSCR/passive/shadow/pressure 类文件；24/24 有 `cmax-audit-records.csv`，但 0/24 有 Qp raw event、Q table dump 或 personal-archive update log。故：

- 预算、provenance、PDDR/DSCR/Cmax 生命周期的审计：日志足够；
- Qp state/action/reward、pbest fallback、personal archive dominated/duplicate/truncate 的因果归因：日志不足；只有 status 中的 count/hash，不能恢复事件顺序或奖励分量。

证据矩阵见 `evidence_matrix.csv`。

## 最小纯观察插桩点（本次未改源码）

源码检查确认 Qp 控制器本身已经在 `ZhangBoQpController.selectGroup` 记录 `select`，在 `settle` 记录 `reward`/`observeFrozen`，并公开 `getEvents()`、`getEventStreamHash()`、`getPbestSwitches()`、`getAverageReward()`、`getTableHash()`。`ZhangBoMOHPSOQ` 也公开 `getQpEvents()`；缺口在 `V35FairRunner`：当前 `mechanismSummary` 只写 Qp count/hash/tableHash，`writeRecord` 没有把 Qp event payload 写进 `large-events.zip`。

因此最小、行为等价的源码插桩应只把已有事件列表作为 sidecar 输出（例如 `qp-events.log` 与 count/hash summary），不改变 selection、settle、reward、archive 或 PDDR 分支。本次遵守“只负责新审计目录”，没有修改 `java-jmetal58` 源码；新目录内已加入 `diagnostic_record.py`、`qp_diagnostic_record.schema.json` 和 4 个本地单元测试，用于 future event export 的 fail-closed 解析。源码插桩尚未执行，故不存在新的算法行为可声称等价。

## 首先排查的变量与门

1. **配对保护门**：任何 frozen jar/snapshot/initial-population/problem hash 不一致立即停止；当前 `{len(shared_mismatch)}` 个不一致。
2. **预算/终止门**：逐 run 检查 `actualFE`, `decoderCalls`, `terminationKind`, `phaseBoundFailure`, `formalGateFailures`；本归档先通过后再解释质量差异。
3. **dual-Q 调度门**：比较 A3 block-frozen warmup/P/G block 与 Qg TD 更新减少，确认是否改变了 Qg 学习/局部搜索阶段。
4. **Qp/PA_i 生命周期门**：需要 raw action、mask、selected pbest、fallback、archive update/truncate、reward components；当前缺失，不能宣称根因已定位。
5. **PDDR 共同路径门**：A2/A3 PDDR 都是 `GLOBAL_ORIGINAL`，只做共同候选保留损失审计，不将其误报为 A2→A3 的差异变量。

当前继续/停止判断：维持正式矩阵暂停；允许只读解析和本地日志诊断，不允许直接恢复远端 50k。若 raw Qp logs 无法取得，最多做 A2/A3 bundle 复现，不能完成内部机制因果分解。

## 待运行入口（本次未执行）

优先使用现有外部 launcher，单变量、低成本、人工批准后再运行。入口契约为：

```text
java -cp <frozen-classpath> org.uma.jmetal.runner.lc_psode.ZhangBoV35FormalAblationArmRunner --plan <plan.properties> --output <new-output-directory>
```

`plan.properties` 必须由既有 Stage2 manifest 生成，输出目录必须是新目录；不要覆盖归档或冻结 Jar。运行后只允许把输出交给本目录脚本复核：

```text
python scripts/analyze_a2_a3.py --restored-root <restored-root> --output-dir <audit-output>
```

以上命令仅写入本报告作为待运行入口，本次审计未启动 Java、Maven、SSH 或远端任务。

## 产物

- `run_inventory.csv`：24 个 run 的 profile/status/provenance/budget/formal gate/机制摘要。
- `profile_pair_comparison.csv`、`provenance_pair_check.csv`、`budget_pair_check.csv`：逐 seed 的差异与配对保护门。
- `mechanism_comparison.csv`：12 个 seed 的 A2/A3 机制计数与 pilot 增量。
- `pilot_metrics_a2_a3.csv`、`pilot_a2_a3_pairs.csv`：归档内 pilot 聚合和配对证据。
- `large_events_inventory.csv`：24 个 ZIP 的成员、Cmax/DSCR 行数及 Qp 原始日志缺口；`evidence_matrix.csv`：字段覆盖和缺口。
- `diagnostic_record.py`、`qp_diagnostic_record.schema.json`、`tests/test_diagnostic_record.py`：纯观察事件契约和本地 fail-closed 单元测试。
"""
    (output_dir / "A2_A3_CAUSAL_AUDIT_REPORT.md").write_text(report, encoding="utf-8")

    manifest = [
        {
            "source_archive": "G:\\ResearchArchive\\ZhangBo-V35-Paper-Evidence-20260823\\remote-campaigns\\zhangbo-v35-stage2-master-v2-20260823.tar.gz",
            "restored_root": str(restored_root),
            "instance": INSTANCE,
            "seeds": ";".join(SEEDS),
            "arms": ";".join(ARMS),
            "run_count": len(rows),
            "pilot_metrics_rows": len(pilot_metrics),
            "remote_execution_started": "false",
            "frozen_jar_modified": "false",
            "pddr_modified": "false",
        }
    ]
    write_csv(output_dir / "restore_manifest.csv", manifest)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
