#!/usr/bin/env python3
"""V35 campaign scheduler with evidence-preserving, process-isolated attempts.

This utility deliberately knows nothing about the optimisation algorithm.  It
only starts immutable command lines from a reviewed manifest and persists the
campaign state around each process.  In particular, it never synthesises a
configuration, seed, treatment, or command-line option.
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from pathlib import Path
from threading import Lock
from typing import Any, Dict, Iterable, List, Mapping, MutableMapping, Tuple


STATUSES = {"PENDING", "RUNNING", "COMPLETED", "FAILED", "INVALID"}
RETRYABLE = {"FAILED", "INVALID"}
REQUIRED_IDENTITY = ("algorithm", "configHash", "instance", "seed", "budget")
ALLOWED_SAFETY_CLASSES = {"diagnostic", "short_benchmark", "formal"}


class CampaignError(RuntimeError):
    """The manifest or a persisted campaign state violates the protocol."""


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat()


def canonical_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def atomic_json_write(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp-{}-{}".format(os.getpid(), time.time_ns()))
    temporary.write_text(canonical_json(value) + "\n", encoding="utf-8")
    os.replace(str(temporary), str(path))


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def identity_for(run: Mapping[str, Any]) -> Dict[str, Any]:
    missing = [field for field in REQUIRED_IDENTITY if field not in run]
    if missing:
        raise CampaignError("run is missing identity fields: {}".format(", ".join(missing)))
    identity = {field: run[field] for field in REQUIRED_IDENTITY}
    # A benchmark can deliberately repeat a short diagnostic command, but it
    # must state a distinct immutable isolation identity; ordinary campaigns
    # must leave this absent, so accidental physical duplicates are rejected.
    if "isolationId" in run:
        identity["isolationId"] = run["isolationId"]
    return identity


def run_key_for(run: Mapping[str, Any]) -> str:
    return sha256_text(canonical_json(identity_for(run)))


def command_hash(command: Iterable[str]) -> str:
    return sha256_text(canonical_json(list(command)))


def validate_manifest(manifest: Mapping[str, Any], allow_formal: bool = False) -> List[Dict[str, Any]]:
    if manifest.get("schemaVersion") != 1:
        raise CampaignError("schemaVersion=1 is required")
    if not isinstance(manifest.get("campaignId"), str) or not manifest["campaignId"].strip():
        raise CampaignError("non-empty campaignId is required")
    max_parallel = manifest.get("maxParallel")
    if not isinstance(max_parallel, int) or max_parallel < 1:
        raise CampaignError("maxParallel must be a positive integer")
    if (not isinstance(manifest.get("frozenBoundaryHash"), str)
            or len(manifest["frozenBoundaryHash"]) != 64
            or any(character not in "0123456789abcdefABCDEF" for character in manifest["frozenBoundaryHash"])):
        raise CampaignError("frozenBoundaryHash must be a SHA-256 hex string")
    if not isinstance(manifest.get("freezeEvidence"), str) or not manifest["freezeEvidence"].strip():
        raise CampaignError("freezeEvidence is required")
    runs = manifest.get("runs")
    if not isinstance(runs, list) or not runs:
        raise CampaignError("runs must be a non-empty list")

    normalized: List[Dict[str, Any]] = []
    seen: Dict[str, Dict[str, Any]] = {}
    diagnostic_limit = manifest.get("maxDiagnosticBudget")
    if diagnostic_limit is not None and (not isinstance(diagnostic_limit, int) or diagnostic_limit < 1):
        raise CampaignError("maxDiagnosticBudget must be a positive integer when present")

    for ordinal, item in enumerate(runs, start=1):
        if not isinstance(item, dict):
            raise CampaignError("run {} must be an object".format(ordinal))
        run = dict(item)
        identity = identity_for(run)
        safety_class = run.get("safetyClass")
        if safety_class not in ALLOWED_SAFETY_CLASSES:
            raise CampaignError("run {} has invalid safetyClass".format(ordinal))
        if safety_class == "formal" and not allow_formal:
            raise CampaignError("formal run {} rejected; explicit --allow-formal is required".format(ordinal))
        if safety_class == "short_benchmark":
            if not isinstance(run.get("benchmarkId"), str) or not run["benchmarkId"].strip():
                raise CampaignError("short_benchmark run {} requires benchmarkId".format(ordinal))
            if diagnostic_limit is not None and int(run["budget"]) > diagnostic_limit:
                raise CampaignError("short_benchmark run {} exceeds maxDiagnosticBudget".format(ordinal))
        command = run.get("command")
        if not isinstance(command, list) or not command or not all(isinstance(part, str) and part for part in command):
            raise CampaignError("run {} command must be a non-empty argv string list".format(ordinal))
        if not isinstance(run.get("maxAttempts", 1), int) or run.get("maxAttempts", 1) < 1:
            raise CampaignError("run {} maxAttempts must be a positive integer".format(ordinal))
        key = run_key_for(run)
        if key in seen:
            raise CampaignError(
                "duplicate RunKey {} for runs {} and {}; change neither treatment nor seed silently".format(
                    key, seen[key]["ordinal"], ordinal))
        frozen_hash = run.get("frozenBoundaryHash", manifest["frozenBoundaryHash"])
        if frozen_hash != manifest["frozenBoundaryHash"]:
            raise CampaignError("run {} frozenBoundaryHash differs from campaign".format(ordinal))
        run.update({
            "ordinal": ordinal,
            "runKey": key,
            "identity": identity,
            "commandHash": command_hash(command),
            "maxAttempts": run.get("maxAttempts", 1),
            "frozenBoundaryHash": frozen_hash,
        })
        seen[key] = run
        normalized.append(run)
    return normalized


@dataclass
class Paths:
    root: Path

    @property
    def state(self) -> Path:
        return self.root / "campaign-state.json"

    @property
    def lock(self) -> Path:
        return self.root / "campaign.lock"

    def run_dir(self, run_key: str) -> Path:
        return self.root / "runs" / run_key


class CampaignStore:
    def __init__(self, paths: Paths, manifest: Mapping[str, Any], runs: List[Dict[str, Any]]):
        self.paths = paths
        self.manifest = manifest
        self.runs = runs
        self.state: MutableMapping[str, Any] = {}
        self.mutex = Lock()

    def load_or_create(self) -> None:
        if self.paths.state.exists():
            state = read_json(self.paths.state)
            if state.get("campaignId") != self.manifest["campaignId"]:
                raise CampaignError("state belongs to another campaignId")
            if state.get("manifestHash") != sha256_text(canonical_json(self.manifest)):
                raise CampaignError("manifest changed after campaign state was created; create a new state directory")
            self.state = state
            return
        self.state = {
            "schemaVersion": 1,
            "campaignId": self.manifest["campaignId"],
            "manifestHash": sha256_text(canonical_json(self.manifest)),
            "frozenBoundaryHash": self.manifest["frozenBoundaryHash"],
            "createdAt": utc_now(),
            "runs": {},
        }
        for run in self.runs:
            self.state["runs"][run["runKey"]] = {
                "status": "PENDING",
                "identity": run["identity"],
                "commandHash": run["commandHash"],
                "attempts": [],
            }
        self.save()

    def save(self) -> None:
        with self.mutex:
            self.state["updatedAt"] = utc_now()
            atomic_json_write(self.paths.state, self.state)

    def verify_completed_markers(self) -> None:
        for run in self.runs:
            record = self.state["runs"][run["runKey"]]
            if record["status"] != "COMPLETED":
                continue
            marker = self.paths.run_dir(run["runKey"]) / "completed.json"
            if not marker.exists():
                record["status"] = "INVALID"
                record["invalidReason"] = "completed status has no atomic completion marker"
                continue
            try:
                completed = read_json(marker)
            except (OSError, ValueError) as error:
                record["status"] = "INVALID"
                record["invalidReason"] = "unreadable completion marker: {}".format(error)
                continue
            if completed.get("runKey") != run["runKey"] or completed.get("commandHash") != run["commandHash"]:
                record["status"] = "INVALID"
                record["invalidReason"] = "completion marker identity mismatch"
        self.save()


class CampaignLock:
    def __init__(self, path: Path):
        self.path = path
        self.fd: int | None = None

    def __enter__(self) -> "CampaignLock":
        self.path.parent.mkdir(parents=True, exist_ok=True)
        try:
            self.fd = os.open(str(self.path), os.O_CREAT | os.O_EXCL | os.O_WRONLY)
        except FileExistsError:
            raise CampaignError("campaign lock already exists: {}".format(self.path))
        os.write(self.fd, ("pid={} startedAt={}\n".format(os.getpid(), utc_now())).encode("utf-8"))
        return self

    def __exit__(self, exc_type: Any, exc: Any, traceback: Any) -> None:
        if self.fd is not None:
            os.close(self.fd)
        try:
            self.path.unlink()
        except FileNotFoundError:
            pass


def render_command(command: Iterable[str], run: Mapping[str, Any], attempt_dir: Path, run_dir: Path, attempt: int) -> List[str]:
    replacements = {
        "run_key": run["runKey"],
        "attempt": str(attempt),
        "attempt_dir": str(attempt_dir),
        "run_dir": str(run_dir),
    }
    try:
        return [part.format(**replacements) for part in command]
    except KeyError as error:
        raise CampaignError("unknown command placeholder {} for {}".format(error, run["runKey"]))


def recover_running_records(store: CampaignStore) -> None:
    for record in store.state["runs"].values():
        if record["status"] == "RUNNING":
            record["status"] = "INVALID"
            record["invalidReason"] = "orphaned RUNNING state found during resume; process ownership is not assumed"
    store.save()


def eligible_runs(store: CampaignStore, runs: List[Dict[str, Any]], retry_failed: bool) -> Tuple[List[Dict[str, Any]], int]:
    selected: List[Dict[str, Any]] = []
    skipped = 0
    for run in runs:
        record = store.state["runs"][run["runKey"]]
        status = record["status"]
        attempts = len(record["attempts"])
        if status == "COMPLETED":
            skipped += 1
        elif status == "PENDING":
            selected.append(run)
        elif status in RETRYABLE and retry_failed and attempts < run["maxAttempts"]:
            selected.append(run)
        elif status in RETRYABLE:
            skipped += 1
        else:
            raise CampaignError("unexpected persisted status {}".format(status))
    return selected, skipped


def execute_run(store: CampaignStore, run: Dict[str, Any]) -> Dict[str, Any]:
    record = store.state["runs"][run["runKey"]]
    attempt = len(record["attempts"]) + 1
    run_dir = store.paths.run_dir(run["runKey"])
    attempt_dir = run_dir / ("attempt-{:04d}".format(attempt))
    attempt_dir.mkdir(parents=True, exist_ok=False)
    command = render_command(run["command"], run, attempt_dir, run_dir, attempt)
    started = utc_now()
    attempt_record: Dict[str, Any] = {
        "attempt": attempt,
        "status": "RUNNING",
        "startedAt": started,
        "command": command,
        "commandHash": run["commandHash"],
        "outputDirectory": str(attempt_dir),
    }
    record["status"] = "RUNNING"
    record["attempts"].append(attempt_record)
    store.save()
    environment = os.environ.copy()
    environment.update({
        "V35_CAMPAIGN_RUN_KEY": run["runKey"],
        "V35_CAMPAIGN_ATTEMPT": str(attempt),
        "V35_CAMPAIGN_OUTPUT": str(attempt_dir),
    })
    with (attempt_dir / "stdout.log").open("wb") as stdout, (attempt_dir / "stderr.log").open("wb") as stderr:
        process = subprocess.Popen(command, cwd=str(Path.cwd()), env=environment, stdout=stdout, stderr=stderr)
        attempt_record["pid"] = process.pid
        store.save()
        exit_code = process.wait()
    finished = utc_now()
    attempt_record.update({"finishedAt": finished, "exitCode": exit_code})
    if exit_code == 0:
        completion = {
            "runKey": run["runKey"],
            "attempt": attempt,
            "commandHash": run["commandHash"],
            "completedAt": finished,
            "exitCode": exit_code,
            "outputDirectory": str(attempt_dir),
        }
        atomic_json_write(run_dir / "completed.json", completion)
        attempt_record["status"] = "COMPLETED"
        record["status"] = "COMPLETED"
    else:
        atomic_json_write(run_dir / ("failed-attempt-{:04d}.json".format(attempt)), {
            "runKey": run["runKey"], "attempt": attempt, "commandHash": run["commandHash"],
            "failedAt": finished, "exitCode": exit_code, "outputDirectory": str(attempt_dir),
        })
        attempt_record["status"] = "FAILED"
        record["status"] = "FAILED"
    store.save()
    return {"runKey": run["runKey"], "status": record["status"], "attempt": attempt, "exitCode": exit_code}


def run_campaign(manifest_path: Path, state_dir: Path | None, resume: bool, retry_failed: bool,
                 allow_formal: bool, dry_run: bool) -> Dict[str, Any]:
    manifest = read_json(manifest_path)
    runs = validate_manifest(manifest, allow_formal=allow_formal)
    paths = Paths(state_dir or manifest_path.parent / ("_campaign-state-" + manifest["campaignId"]))
    with CampaignLock(paths.lock):
        store = CampaignStore(paths, manifest, runs)
        store.load_or_create()
        store.verify_completed_markers()
        if resume:
            recover_running_records(store)
        selected, skipped = eligible_runs(store, runs, retry_failed)
        if dry_run:
            return {"campaignId": manifest["campaignId"], "planned": len(selected), "skipped": skipped,
                    "stateDirectory": str(paths.root), "dryRun": True}
        outcomes: List[Dict[str, Any]] = []
        with ThreadPoolExecutor(max_workers=manifest["maxParallel"]) as executor:
            futures = [executor.submit(execute_run, store, run) for run in selected]
            for future in as_completed(futures):
                outcomes.append(future.result())
        completed = sum(1 for outcome in outcomes if outcome["status"] == "COMPLETED")
        failed = sum(1 for outcome in outcomes if outcome["status"] != "COMPLETED")
        return {"campaignId": manifest["campaignId"], "started": len(selected), "completed": completed,
                "failed": failed, "skipped": skipped, "stateDirectory": str(paths.root), "dryRun": False}


def main(argv: List[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Run a frozen V35 campaign manifest")
    parser.add_argument("--manifest", required=True, type=Path, help="UTF-8 JSON campaign manifest")
    parser.add_argument("--state-dir", type=Path, help="state and attempt output directory")
    parser.add_argument("--resume", action="store_true", help="recover stale RUNNING records before scheduling")
    parser.add_argument("--retry-failed", action="store_true", help="retry FAILED or INVALID runs within maxAttempts")
    parser.add_argument("--allow-formal", action="store_true", help="explicitly permit manifest entries marked formal")
    parser.add_argument("--dry-run", action="store_true", help="validate and show scheduling result without starting processes")
    arguments = parser.parse_args(argv)
    try:
        result = run_campaign(arguments.manifest, arguments.state_dir, arguments.resume,
                              arguments.retry_failed, arguments.allow_formal, arguments.dry_run)
    except (CampaignError, OSError, subprocess.SubprocessError, ValueError) as error:
        print("V35_CAMPAIGN_ERROR: {}".format(error), file=sys.stderr)
        return 2
    print(canonical_json(result))
    return 0 if result.get("failed", 0) == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
