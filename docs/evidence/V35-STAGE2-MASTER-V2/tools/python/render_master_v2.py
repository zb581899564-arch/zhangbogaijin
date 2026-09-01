#!/usr/bin/env python3
"""Render the immutable 4,500-run A0--A4 Stage2 manifest and per-run plans."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
from pathlib import Path

from v35_master_v2 import ARMS, SCHEMA, run_key, validate_manifest


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def normalized_path(path: Path) -> str:
    return path.resolve().as_posix()


def write_plan(path: Path, values: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = "".join(f"{key}={values[key]}\n" for key in sorted(values))
    path.write_text(text, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--instance-manifest", required=True, type=Path)
    parser.add_argument("--population-manifest", required=True, type=Path)
    parser.add_argument("--profile-registry", required=True, type=Path)
    parser.add_argument("--snapshot-root", required=True, type=Path)
    parser.add_argument("--java-project", required=True, type=Path)
    parser.add_argument("--frozen-jar", required=True, type=Path)
    parser.add_argument("--external-classes", required=True, type=Path)
    parser.add_argument("--output-root", required=True, type=Path)
    parser.add_argument("--plan-root", required=True, type=Path)
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--java-bin", default="java")
    parser.add_argument("--cpu-sets", default="")
    parser.add_argument("--max-fes", type=int, default=500000)
    parser.add_argument("--purpose", choices=("FORMAL", "LAUNCHER_ACCEPTANCE"), default="FORMAL")
    args = parser.parse_args()

    instance_rows = {row["instanceId"]: row for row in rows(args.instance_manifest)}
    population_rows = rows(args.population_manifest)
    profiles = {(row["arm"], int(row["seed"])): row for row in rows(args.profile_registry)}
    jar_hash = sha256(args.frozen_jar)
    cpu_sets = [value.strip() for value in args.cpu_sets.split(";") if value.strip()]
    runs: list[dict] = []
    ordinal = 0
    if len(instance_rows) != 45 or len(population_rows) != 900:
        raise ValueError(f"frozen manifests expected 45 instances/900 snapshots, got {len(instance_rows)}/{len(population_rows)}")
    seen_population_keys = set()
    for population in sorted(population_rows, key=lambda row: (row["instanceId"], int(row["seed"]))):
        instance_id = population["instanceId"]
        seed = int(population["seed"])
        if (instance_id, seed) in seen_population_keys:
            raise ValueError(f"duplicate frozen snapshot row: {instance_id}/{seed}")
        seen_population_keys.add((instance_id, seed))
        instance = instance_rows[instance_id]
        instance_path = args.java_project / "EADHFSP" / f"{instance_id}.txt"
        setup_path = args.java_project / "instance-extensions" / "v1" / f"{instance_id}.setup.txt"
        fatigue_path = args.java_project / "fatigue-parameters" / "v1" / f"{instance_id}.fatigue.txt"
        snapshot_path = args.snapshot_root / population["snapshotPath"]
        if sha256(instance_path).lower() != instance["instanceSHA256"].lower():
            raise ValueError(f"instance byte hash mismatch: {instance_path}")
        if sha256(snapshot_path).lower() != population["snapshotSHA256"].lower():
            raise ValueError(f"snapshot byte hash mismatch: {snapshot_path}")
        for field in ("instanceSHA256", "SUTSHA256", "fatigueParameterSHA256", "problemConfigurationSHA256"):
            if population[field].lower() != instance[field].lower():
                raise ValueError(f"population provenance mismatch {instance_id}/{seed}/{field}")
        for arm in ARMS:
            profile = profiles[(arm, seed)]
            run_id = f"{instance_id}-s{seed}-{arm}"
            output = args.output_root / instance_id / f"seed-{seed}" / arm
            plan_path = args.plan_root / instance_id / f"seed-{seed}" / f"{arm}.properties"
            plan = {
                "schema": "v35-final-a0-a4-run-plan-v2", "purpose": args.purpose,
                "runId": run_id, "arm": arm, "seed": seed, "population": 100,
                "maxFEs": args.max_fes, "frozenJar": normalized_path(args.frozen_jar),
                "frozenJarSha256": jar_hash, "instancePath": normalized_path(instance_path),
                "setupPath": normalized_path(setup_path), "fatiguePath": normalized_path(fatigue_path),
                "instanceSha256": instance["instanceSHA256"],
                "setupFileSha256": sha256(setup_path), "fatigueFileSha256": sha256(fatigue_path),
                "setupConfigurationSha256": instance["SUTSHA256"],
                "fatigueConfigurationSha256": instance["fatigueParameterSHA256"],
                "problemConfigurationSha256": instance["problemConfigurationSHA256"],
                "snapshotPath": normalized_path(snapshot_path), "snapshotSha256": population["snapshotSHA256"],
                "initialPopulationHashV35": population["initialPopulationSHA256"],
                "initialPopulationHashP8": population["initialPopulationP8SHA256"],
                "armProfileSha256": profile["profileSha256"],
                "runtimeConfigurationSha256": profile["runtimeConfigurationSha256"],
                "launcherAcceptanceOnly": str(args.purpose == "LAUNCHER_ACCEPTANCE").lower(),
                "includedInFormalStatistics": str(args.purpose == "FORMAL").lower(),
                "includedInReferenceFront": str(args.purpose == "FORMAL").lower(),
            }
            write_plan(plan_path, plan)
            command = [args.java_bin, "-Xmx4g", "-cp",
                       f"{args.external_classes}{os.pathsep}{args.frozen_jar}",
                       "org.uma.jmetal.runner.lc_psode.ZhangBoV35FormalAblationArmRunner",
                       "--plan", str(plan_path), "--output", str(output)]
            if cpu_sets:
                command = ["taskset", "-c", cpu_sets[ordinal % len(cpu_sets)]] + command
            run = {
                "runId": run_id, "arm": arm, "instance": instance_id, "seed": seed,
                "maxFEs": args.max_fes, "jarSha256": jar_hash,
                "armProfileSha256": profile["profileSha256"],
                "snapshotSha256": population["snapshotSHA256"],
                "initialPopulationHashV35": population["initialPopulationSHA256"],
                "initialPopulationHashP8": population["initialPopulationP8SHA256"],
                "problemConfigurationSha256": instance["problemConfigurationSHA256"],
                "algorithmSemanticsVersion": "v35-final-a0-a4-ablation-v1",
                "budgetProtocolVersion": "v35-phase-consistent-budget-v1",
                "purpose": args.purpose, "plan": str(plan_path), "output": str(output),
                "command": command,
            }
            run["runKey"] = run_key(run)
            runs.append(run)
            ordinal += 1
    manifest = {"schema": SCHEMA, "outputRoot": str(args.output_root),
                "minimumFreeGB": 120 if args.purpose == "FORMAL" else 1,
                "runs": runs}
    require_formal = args.purpose == "FORMAL"
    validate_manifest(manifest, require_formal=require_formal)
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"MASTER_V2_RENDERED runs={len(runs)} groups={len(runs)//5} manifest={args.manifest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
