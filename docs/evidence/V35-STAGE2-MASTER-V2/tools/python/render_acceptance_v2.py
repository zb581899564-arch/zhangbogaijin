#!/usr/bin/env python3
"""Render one five-arm snapshot-bound acceptance group (2k/50k/500k)."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
from pathlib import Path

from v35_master_v2 import ARMS, SCHEMA, run_key, validate_manifest


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def normalized_path(path: Path) -> str:
    return path.resolve().as_posix()


def properties(path: Path) -> dict[str, str]:
    return dict(line.split("=", 1) for line in path.read_text(encoding="utf-8").splitlines()
                if line and not line.startswith("#"))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--java-project", required=True, type=Path)
    parser.add_argument("--frozen-jar", required=True, type=Path)
    parser.add_argument("--external-classes", required=True, type=Path)
    parser.add_argument("--snapshot", required=True, type=Path)
    parser.add_argument("--snapshot-properties", required=True, type=Path)
    parser.add_argument("--profile-registry", required=True, type=Path)
    parser.add_argument("--max-fes", required=True, type=int)
    parser.add_argument("--purpose", choices=("SMOKE", "GATE3", "LAUNCHER_ACCEPTANCE"), required=True)
    parser.add_argument("--plan-root", required=True, type=Path)
    parser.add_argument("--output-root", required=True, type=Path)
    parser.add_argument("--manifest", required=True, type=Path)
    args = parser.parse_args()
    snapshot = properties(args.snapshot_properties)
    snapshot_header = properties(args.snapshot)
    with args.profile_registry.open(encoding="utf-8", newline="") as stream:
        profiles = {(row["arm"], int(row["seed"])): row for row in csv.DictReader(stream)}
    instance = snapshot.get("instance", "20_2_3_1")
    seed = int(snapshot["seed"])
    setup_path = args.java_project / "instance-extensions/v1" / f"{instance}.setup.txt"
    fatigue_path = args.java_project / "fatigue-parameters/v1" / f"{instance}.fatigue.txt"
    instance_path = args.java_project / "EADHFSP" / f"{instance}.txt"
    jar_hash = digest(args.frozen_jar)
    runs = []
    for arm in ARMS:
        profile = profiles[(arm, seed)]
        run_id = f"acceptance-{args.max_fes}-{instance}-s{seed}-{arm}"
        plan_path = args.plan_root / f"{arm}.properties"
        output = args.output_root / arm
        plan = {
            "schema": "v35-final-a0-a4-run-plan-v2", "purpose": args.purpose,
            "runId": run_id, "arm": arm, "seed": seed, "population": 100,
            "maxFEs": args.max_fes, "frozenJar": normalized_path(args.frozen_jar),
            "frozenJarSha256": jar_hash, "instancePath": normalized_path(instance_path),
            "setupPath": normalized_path(setup_path), "fatiguePath": normalized_path(fatigue_path),
            "instanceSha256": digest(instance_path), "setupFileSha256": digest(setup_path),
            "fatigueFileSha256": digest(fatigue_path),
            "setupConfigurationSha256": snapshot_header["SUTSHA256"],
            "fatigueConfigurationSha256": snapshot_header["fatigueParameterSHA256"],
            "problemConfigurationSha256": snapshot["problemConfigurationSha256"],
            "snapshotPath": normalized_path(args.snapshot), "snapshotSha256": digest(args.snapshot),
            "initialPopulationHashV35": snapshot["initialPopulationHashV35"],
            "initialPopulationHashP8": snapshot["initialPopulationHashP8"],
            "armProfileSha256": profile["profileSha256"],
            "runtimeConfigurationSha256": profile["runtimeConfigurationSha256"],
            "launcherAcceptanceOnly": str(args.purpose == "LAUNCHER_ACCEPTANCE").lower(),
            "includedInFormalStatistics": "false", "includedInReferenceFront": "false",
        }
        plan_path.parent.mkdir(parents=True, exist_ok=True)
        plan_path.write_text("".join(f"{key}={plan[key]}\n" for key in sorted(plan)),
                             encoding="utf-8")
        command = ["java", "-Xmx4g", "-cp", f"{args.external_classes}{os.pathsep}{args.frozen_jar}",
                   "org.uma.jmetal.runner.lc_psode.ZhangBoV35FormalAblationArmRunner",
                   "--plan", str(plan_path), "--output", str(output)]
        run = {
            "runId": run_id, "arm": arm, "instance": instance, "seed": seed,
            "maxFEs": args.max_fes, "jarSha256": jar_hash,
            "armProfileSha256": profile["profileSha256"],
            "snapshotSha256": digest(args.snapshot),
            "initialPopulationHashV35": snapshot["initialPopulationHashV35"],
            "initialPopulationHashP8": snapshot["initialPopulationHashP8"],
            "problemConfigurationSha256": snapshot["problemConfigurationSha256"],
            "algorithmSemanticsVersion": "v35-final-a0-a4-ablation-v1",
            "budgetProtocolVersion": "v35-phase-consistent-budget-v1",
            "purpose": args.purpose, "plan": str(plan_path), "output": str(output),
            "command": command,
        }
        run["runKey"] = run_key(run)
        runs.append(run)
    manifest = {"schema": SCHEMA, "outputRoot": str(args.output_root),
                "minimumFreeGB": 1, "runs": runs}
    validate_manifest(manifest)
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"ACCEPTANCE_MANIFEST_RENDERED maxFEs={args.max_fes} runs=5")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
