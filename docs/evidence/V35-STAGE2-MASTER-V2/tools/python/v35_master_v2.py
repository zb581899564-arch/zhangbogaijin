#!/usr/bin/env python3
"""Fail-closed 15-JVM Stage2 master for complete A0--A4 fairness groups."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

SCHEMA = "v35-final-a0-a4-master-v2"
ARMS = ("A0", "A1", "A2", "A3", "A4")
RUNNER_VERSION = "v35-formal-a0-a4-external-runner-v1"
KEEP_READABLE = {
    "front.csv", "status.properties", "configuration.txt", "provenance.properties",
    "budget-termination.properties", "formal-gate.properties", "profile.txt",
    "profile.sha256", "initial-population.sha256", "evidence-sha256.tsv",
}
ARCHIVE_CANDIDATES = {
    "ca-ta-lite-events.log", "dscr-events.csv", "dscr-teacher-uses.csv",
    "bottleneck-pressure-events.csv", "shadow-probes.csv", "passive-archive.csv",
    "cmax-audit-curves.csv", "cmax-audit-records.csv", "cmax-audit-summary.txt",
}


class GateError(RuntimeError):
    pass


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def canonical_identity(run: dict) -> dict:
    return {key: run[key] for key in (
        "arm", "instance", "seed", "maxFEs", "jarSha256", "armProfileSha256",
        "snapshotSha256", "initialPopulationHashV35", "initialPopulationHashP8",
        "problemConfigurationSha256", "algorithmSemanticsVersion", "budgetProtocolVersion",
    )}


def run_key(run: dict) -> str:
    raw = json.dumps(canonical_identity(run), ensure_ascii=False, sort_keys=True,
                     separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


def read_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.lstrip().startswith(("#", "!")):
            continue
        if "=" not in line:
            raise GateError(f"invalid properties line in {path}: {line}")
        key, value = line.split("=", 1)
        if key in values:
            raise GateError(f"duplicate property {key} in {path}")
        values[key] = value
    return values


def verify_evidence_manifest(output: Path) -> None:
    manifest = output / "evidence-sha256.tsv"
    if not manifest.is_file():
        raise GateError(f"missing evidence manifest: {manifest}")
    with manifest.open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    seen: set[str] = set()
    for row in rows:
        rel = row.get("path", "")
        expected = row.get("sha256", "").lower()
        if not rel or rel in seen or rel == "evidence-sha256.tsv":
            raise GateError(f"invalid evidence row {rel!r} in {manifest}")
        seen.add(rel)
        target = (output / rel).resolve()
        if output.resolve() not in target.parents or not target.is_file():
            raise GateError(f"missing or escaping evidence path: {rel}")
        if sha256(target).lower() != expected:
            raise GateError(f"evidence hash mismatch: {target}")


def write_evidence_manifest(output: Path) -> None:
    manifest = output / "evidence-sha256.tsv"
    rows = []
    for path in sorted(p for p in output.rglob("*") if p.is_file() and p != manifest):
        rows.append(f"{path.relative_to(output).as_posix()}\t{sha256(path)}\n")
    manifest.write_text("path\tsha256\n" + "".join(rows), encoding="utf-8")


def audit_run(run: dict) -> dict:
    output = Path(run["output"])
    if not output.is_dir():
        raise GateError(f"missing completed output: {output}")
    verify_evidence_manifest(output)
    gate = read_properties(output / "formal-gate.properties")
    budget = read_properties(output / "budget-termination.properties")
    provenance = read_properties(output / "provenance.properties")
    status = read_properties(output / "status.properties")
    if gate.get("status") != "COMPLETED" or status.get("status") != "COMPLETED":
        raise GateError(f"run is not completed: {run['runId']}")
    exact = {
        "armProfileSha256": run["armProfileSha256"],
        "snapshotSha256": run["snapshotSha256"],
        "initialPopulationHashV35": run["initialPopulationHashV35"],
        "initialPopulationHashP8": run["initialPopulationHashP8"],
        "problemConfigurationSha256": run["problemConfigurationSha256"],
        "frozenJarSha256": run["jarSha256"],
    }
    for key, expected in exact.items():
        actual = provenance.get(key) or budget.get(key)
        if actual is None or actual.lower() != str(expected).lower():
            raise GateError(f"{run['runId']} {key} expected={expected} actual={actual}")
    requested = int(budget["requestedMaxFE"])
    actual = int(budget["actualFE"])
    decoder = int(budget["decoderCalls"])
    remaining = int(budget["remainingFE"])
    q_phase = int(budget["qPhaseFE"])
    utilization = float(budget["utilizationRate"])
    if requested != int(run["maxFEs"]) or not (0 < actual == decoder <= requested):
        raise GateError(f"FE closure failed: {run['runId']}")
    if not (0 <= remaining < q_phase == 5000):
        raise GateError(f"phase tail failed: {run['runId']}")
    if run.get("purpose") in {"FORMAL", "LAUNCHER_ACCEPTANCE"} and not utilization > 0.99:
        raise GateError(f"utilization <= 0.99: {run['runId']}")
    return {"runId": run["runId"], "arm": run["arm"], "actualFE": actual,
            "utilizationRate": utilization, "terminationKind": budget["terminationKind"]}


def group_runs(manifest: dict) -> dict[tuple[str, int], list[dict]]:
    groups: dict[tuple[str, int], list[dict]] = {}
    for run in manifest["runs"]:
        groups.setdefault((run["instance"], int(run["seed"])), []).append(run)
    return groups


def validate_group(key: tuple[str, int], runs: list[dict], audited: list[dict] | None = None) -> None:
    arms = [run["arm"] for run in runs]
    if sorted(arms) != list(ARMS) or len(set(arms)) != 5:
        raise GateError(f"group {key} does not contain exactly A0..A4: {arms}")
    same_fields = ("instance", "seed", "maxFEs", "jarSha256", "snapshotSha256",
                   "initialPopulationHashV35", "initialPopulationHashP8",
                   "problemConfigurationSha256", "algorithmSemanticsVersion", "budgetProtocolVersion")
    for field in same_fields:
        if len({str(run[field]).lower() for run in runs}) != 1:
            raise GateError(f"group {key} mismatched {field}")
    if audited:
        values = [int(row["actualFE"]) for row in audited]
        if max(values) - min(values) >= 5000:
            raise GateError(f"group {key} FE range is >= 5000: {values}")


def validate_manifest(manifest: dict, require_formal: bool = False) -> dict[tuple[str, int], list[dict]]:
    if manifest.get("schema") != SCHEMA:
        raise GateError(f"schema must be {SCHEMA}")
    runs = manifest.get("runs")
    if not isinstance(runs, list) or not runs:
        raise GateError("manifest runs must be a non-empty list")
    seen_run_ids: set[str] = set()
    seen_run_keys: set[str] = set()
    for run in runs:
        if run.get("arm") not in ARMS:
            raise GateError(f"unknown arm={run.get('arm')}")
        if run.get("runId") in seen_run_ids:
            raise GateError(f"duplicate runId={run.get('runId')}")
        seen_run_ids.add(run["runId"])
        calculated = run_key(run)
        if run.get("runKey") != calculated or calculated in seen_run_keys:
            raise GateError(f"invalid or duplicate RunKey for {run['runId']}")
        seen_run_keys.add(calculated)
        if not isinstance(run.get("command"), list) or not run["command"]:
            raise GateError(f"missing command for {run['runId']}")
        if "ZhangBoV35FormalAblationArmRunner" not in " ".join(run["command"]):
            raise GateError(f"wrong runner for {run['runId']}")
    groups = group_runs(manifest)
    for key, values in groups.items():
        validate_group(key, values)
    if require_formal:
        if len(runs) != 4500 or len(groups) != 900:
            raise GateError(f"formal manifest expected 4500 runs/900 groups, got {len(runs)}/{len(groups)}")
        if {run.get("purpose") for run in runs} != {"FORMAL"}:
            raise GateError("formal manifest contains non-formal purpose")
        expected_instances = {
            f"{jobs}_{stages}_{factories}_1"
            for jobs in (20, 50, 100, 150, 200)
            for stages in (2, 5, 8)
            for factories in (3, 4, 5)
        }
        expected_seeds = set(range(20260808, 20260828))
        actual_instances = {key[0] for key in groups}
        actual_seeds = {key[1] for key in groups}
        if actual_instances != expected_instances or actual_seeds != expected_seeds:
            raise GateError("formal instance/seed roster is not the frozen 45 x 20 cartesian product")
        if set(groups) != {(instance, seed) for instance in expected_instances for seed in expected_seeds}:
            raise GateError("formal fairness groups are not the complete frozen cartesian product")
    return groups


def disk_gate(path: Path, minimum_gb: float) -> None:
    free = shutil.disk_usage(path).free / (1024 ** 3)
    if free < minimum_gb:
        raise GateError(f"disk gate failed: {free:.2f} GB free < {minimum_gb:.2f} GB")


def archive_group(runs: list[dict]) -> None:
    for run in runs:
        output = Path(run["output"])
        candidates = [output / name for name in ARCHIVE_CANDIDATES if (output / name).is_file()]
        candidates = [path for path in candidates if path.name not in KEEP_READABLE and path.stat().st_size > 0]
        if not candidates:
            continue
        archive = output / "large-events.zip"
        tmp = output / ".partial-large-events.zip"
        with zipfile.ZipFile(tmp, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
            old_manifest = output / "evidence-sha256.tsv"
            if old_manifest.is_file():
                zf.write(old_manifest, arcname="prearchive-evidence-sha256.tsv")
            for path in sorted(candidates):
                zf.write(path, arcname=path.name)
        with zipfile.ZipFile(tmp, "r") as zf:
            expected_names = {p.name for p in candidates}
            if (output / "evidence-sha256.tsv").is_file():
                expected_names.add("prearchive-evidence-sha256.tsv")
            if zf.testzip() is not None or set(zf.namelist()) != expected_names:
                raise GateError(f"archive verification failed: {tmp}")
        os.replace(tmp, archive)
        (output / "large-events.zip.sha256").write_text(sha256(archive) + "  large-events.zip\n", encoding="utf-8")
        for path in candidates:
            path.unlink()
        write_evidence_manifest(output)


def run_one(run: dict) -> tuple[dict, int, str]:
    process = subprocess.run(run["command"], cwd=run.get("cwd") or None,
                             stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    log = Path(run["output"]).parent / ("console-" + run["runId"] + ".log")
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_text(process.stdout, encoding="utf-8")
    return run, process.returncode, process.stdout[-4000:]


def execute(manifest: dict, dry_run: bool, require_formal: bool) -> None:
    groups = validate_manifest(manifest, require_formal=require_formal)
    output_root = Path(manifest["outputRoot"])
    output_root.mkdir(parents=True, exist_ok=True)
    disk_gate(output_root, float(manifest.get("minimumFreeGB", 120)))
    ordered = sorted(groups.items())
    for wave_index in range(0, len(ordered), 3):
        wave = ordered[wave_index:wave_index + 3]
        wave_runs = [run for _, values in wave for run in sorted(values, key=lambda row: row["arm"])]
        if len(wave_runs) > 15:
            raise GateError("wave exceeds 15 JVM")
        if dry_run:
            continue
        pending_runs = []
        for run in wave_runs:
            output = Path(run["output"])
            if output.exists():
                # Resume is permitted only for a fully valid immutable output.
                # Invalid existing outputs are never overwritten or hidden.
                audit_run(run)
            else:
                pending_runs.append(run)
        with ThreadPoolExecutor(max_workers=15) as pool:
            futures = [pool.submit(run_one, run) for run in pending_runs]
            for future in as_completed(futures):
                run, code, tail = future.result()
                if code != 0:
                    raise GateError(f"run failed {run['runId']} exit={code}\n{tail}")
        for key, values in wave:
            audited = [audit_run(run) for run in values]
            validate_group(key, values, audited)
            archive_group(values)
            # The post-archive manifest is a new immutable current-state index;
            # re-audit it immediately so compression can never hide evidence loss.
            audited = [audit_run(run) for run in values]
            validate_group(key, values, audited)
        state = output_root / "master-state.json"
        state.write_text(json.dumps({"lastAcceptedWave": wave_index // 3,
                                     "acceptedGroups": min(wave_index + 3, len(ordered))},
                                    indent=2), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("validate", "dry-run", "run", "audit"))
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--formal", action="store_true")
    args = parser.parse_args()
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    groups = validate_manifest(manifest, require_formal=args.formal)
    if args.command == "validate":
        print(f"MASTER_V2_VALID runs={len(manifest['runs'])} groups={len(groups)}")
    elif args.command == "dry-run":
        execute(manifest, dry_run=True, require_formal=args.formal)
        print(f"MASTER_V2_DRY_RUN_OK processesStarted=0 runs={len(manifest['runs'])}")
    elif args.command == "audit":
        for key, runs in sorted(groups.items()):
            audited = [audit_run(run) for run in runs]
            validate_group(key, runs, audited)
        print(f"MASTER_V2_AUDIT_OK groups={len(groups)}")
    else:
        execute(manifest, dry_run=False, require_formal=args.formal)
        print(f"MASTER_V2_COMPLETED groups={len(groups)}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except GateError as error:
        print(f"MASTER_V2_BLOCKED {error}", file=sys.stderr)
        raise SystemExit(2)
