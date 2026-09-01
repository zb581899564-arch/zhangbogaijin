#!/usr/bin/env python3
"""Fail-closed wave scheduler and pair audit for the A2/A4 confirmation only."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

ARMS = ("A2", "A4")


class GateError(RuntimeError):
    pass


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def props(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith(("#", "!")):
            key, value = line.split("=", 1)
            values[key] = value
    return values


def verify_manifest(output: Path) -> None:
    manifest = output / "evidence-sha256.tsv"
    if not manifest.is_file():
        raise GateError(f"missing evidence manifest {output}")
    with manifest.open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    if not rows:
        raise GateError(f"empty evidence manifest {output}")
    for row in rows:
        target = (output / row["path"]).resolve()
        if output.resolve() not in target.parents or not target.is_file() or sha256(target).lower() != row["sha256"].lower():
            raise GateError(f"evidence mismatch {output}/{row['path']}")


def audit(run: dict) -> dict:
    output = Path(run["output"])
    verify_manifest(output)
    gate, budget, provenance, status, context = (props(output / name) for name in (
        "formal-gate.properties", "budget-termination.properties", "provenance.properties",
        "status.properties", "confirmation-context.properties"))
    if gate.get("status") != "COMPLETED" or status.get("status") != "COMPLETED":
        raise GateError(f"incomplete {run['runId']}")
    if (context.get("campaignPurpose") != "CONFIRMATION" or context.get("arm") != run["arm"]
            or context.get("runId") != run["runId"]
            or context.get("preRegisteredRunKey") != run["preRegisteredRunKey"]
            or context.get("preRegisteredArmLabel") != run["preRegisteredArmLabel"]):
        raise GateError(f"classification drift {run['runId']}")
    actual, decoder, requested = int(budget["actualFE"]), int(budget["decoderCalls"]), int(budget["requestedMaxFE"])
    remaining, q_phase, utilization = int(budget["remainingFE"]), int(budget["qPhaseFE"]), float(budget["utilizationRate"])
    if not (0 < actual == decoder <= requested == 500000 and 0 <= remaining < q_phase == 5000 and utilization > .99):
        raise GateError(f"phase budget failure {run['runId']}")
    for key, expected in {"frozenJarSha256": run["jarSha256"], "armProfileSha256": run["armProfileSha256"],
                          "snapshotSha256": run["snapshotSha256"], "initialPopulationHashV35": run["initialPopulationHashV35"],
                          "initialPopulationHashP8": run["initialPopulationHashP8"],
                          "problemConfigurationSha256": run["problemConfigurationSha256"]}.items():
        if provenance.get(key, "").lower() != str(expected).lower():
            raise GateError(f"provenance mismatch {run['runId']}/{key}")
    return {"runId": run["runId"], "arm": run["arm"], "actualFE": actual, "utilizationRate": utilization,
            "status": "ACCEPTED"}


def groups(manifest: dict) -> list[tuple[tuple[str, int], list[dict]]]:
    if manifest.get("schema") != "v35-a2-a4-confirmation-manifest-v1":
        raise GateError("unexpected manifest schema")
    result: dict[tuple[str, int], list[dict]] = {}
    for run in manifest.get("runs", []):
        if run.get("arm") not in ARMS or run.get("purpose") != "CONFIRMATION":
            raise GateError(f"invalid run {run}")
        result.setdefault((run["instance"], int(run["seed"])), []).append(run)
    for key, pair in result.items():
        if tuple(sorted(row["arm"] for row in pair)) != ARMS:
            raise GateError(f"incomplete pair {key}")
    return sorted(result.items())


def run_one(run: dict) -> tuple[dict, int, str]:
    process = subprocess.run(run["command"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    log = Path(run["output"]).parent / f"console-{run['arm']}.log"
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_text(process.stdout, encoding="utf-8")
    return run, process.returncode, process.stdout[-4000:]


def write_progress(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fields = ("instance", "seed", "arm", "runId", "status", "actualFE", "utilizationRate", "detail")
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader(); writer.writerows(rows)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--progress", required=True, type=Path)
    parser.add_argument("--groups-per-wave", type=int, default=1)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    all_groups = groups(manifest)
    accepted: list[dict] = []
    for index in range(0, len(all_groups), args.groups_per_wave):
        wave = all_groups[index:index + args.groups_per_wave]
        runs = [run for _, pair in wave for run in pair]
        if args.dry_run:
            for run in runs:
                accepted.append({"instance": run["instance"], "seed": run["seed"], "arm": run["arm"],
                                 "runId": run["runId"], "status": "DRY_RUN", "actualFE": "", "utilizationRate": "", "detail": ""})
            continue
        with ThreadPoolExecutor(max_workers=len(runs)) as pool:
            futures = [pool.submit(run_one, run) for run in runs]
            completed = [future.result() for future in as_completed(futures)]
        failures = [(run, code, detail) for run, code, detail in completed if code != 0]
        if failures:
            for run, code, detail in failures:
                accepted.append({"instance": run["instance"], "seed": run["seed"], "arm": run["arm"], "runId": run["runId"],
                                 "status": "EXECUTION_FAILED", "actualFE": "", "utilizationRate": "", "detail": f"exit={code};{detail}"})
            write_progress(args.progress, accepted)
            raise GateError("wave execution failed; subsequent waves not started")
        for key, pair in wave:
            audited = [audit(run) for run in pair]
            values = [row["actualFE"] for row in audited]
            if max(values) - min(values) >= 5000:
                raise GateError(f"pair FE range >=5000 {key}/{values}")
            for run, record in zip(sorted(pair, key=lambda row: row["arm"]), sorted(audited, key=lambda row: row["arm"])):
                accepted.append({"instance": run["instance"], "seed": run["seed"], "arm": run["arm"], "runId": run["runId"],
                                 "status": record["status"], "actualFE": record["actualFE"],
                                 "utilizationRate": record["utilizationRate"], "detail": ""})
        write_progress(args.progress, accepted)
    write_progress(args.progress, accepted)
    print(f"CONFIRMATION_MASTER_COMPLETED runs={len(accepted)}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except GateError as error:
        print(f"CONFIRMATION_MASTER_BLOCKED {error}", file=sys.stderr)
        raise SystemExit(2)
