#!/usr/bin/env python3
"""Render snapshot-bound A2/A4 confirmation plans without changing the frozen jar."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
from pathlib import Path

SCHEMA = "v35-a2-a4-confirmation-manifest-v1"
ARMS = ("A2", "A4")
FIRST_KEY = ("20_2_4_1", "20260901")
PUBLIC_TO_INTERNAL_ARM = {
    "A2_CFVF": "A2",
    "A4_BUDGET_AWARE_CATA": "A4",
}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def csv_rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith(("#", "!")):
            continue
        key, value = line.split("=", 1)
        if key in values:
            raise ValueError(f"duplicate key {key} in {path}")
        values[key] = value
    return values


def write_properties(path: Path, values: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(f"{key}={values[key]}\n" for key in sorted(values)), encoding="utf-8")


def run_key(run: dict) -> str:
    identity = {key: run[key] for key in (
        "runId", "arm", "instance", "seed", "maxFEs", "jarSha256", "armProfileSha256",
        "snapshotSha256", "initialPopulationHashV35", "initialPopulationHashP8",
        "problemConfigurationSha256", "protocolVersion",
    )}
    raw = json.dumps(identity, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


def check_groups(runs: list[dict], expected: int) -> None:
    if len(runs) != expected:
        raise ValueError(f"expected {expected} runs, got {len(runs)}")
    grouped: dict[tuple[str, str], list[dict]] = {}
    for run in runs:
        grouped.setdefault((run["instance"], str(run["seed"])), []).append(run)
    for key, pair in grouped.items():
        if len(pair) != 2 or tuple(sorted(row["arm"] for row in pair)) != ARMS:
            raise ValueError(f"incomplete A2/A4 pair {key}")
        fields = ("instance", "seed", "maxFEs", "jarSha256", "snapshotSha256",
                  "initialPopulationHashV35", "initialPopulationHashP8", "problemConfigurationSha256")
        for field in fields:
            if len({str(row[field]).lower() for row in pair}) != 1:
                raise ValueError(f"fairness mismatch {key}/{field}")
    if len({row["runId"] for row in runs}) != len(runs) or len({row["runKey"] for row in runs}) != len(runs):
        raise ValueError("duplicate run identity")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--registry", required=True, type=Path)
    parser.add_argument("--java-project", required=True, type=Path)
    parser.add_argument("--frozen-jar", required=True, type=Path)
    parser.add_argument("--external-classes", required=True, type=Path)
    parser.add_argument("--confirmation-classes", required=True, type=Path)
    parser.add_argument("--snapshot-root", required=True, type=Path)
    parser.add_argument("--profiles", required=True, type=Path)
    parser.add_argument("--plan-root", required=True, type=Path)
    parser.add_argument("--output-root", required=True, type=Path)
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--phase", choices=("FIRST", "REMAINDER", "ALL"), required=True)
    parser.add_argument(
        "--cpu-sets",
        default="0-1;2-3;4-5;6-7;8-9;10-11;12-13;14-15;16-17;18-19;20-21;22-23;24-25;26-27;28-29;30-31",
    )
    args = parser.parse_args()

    rows = csv_rows(args.registry)
    if len(rows) != 60:
        raise ValueError(f"registry must contain exactly 60 pre-registered runs, got {len(rows)}")
    selected = []
    for row in rows:
        if row.get("arm") not in PUBLIC_TO_INTERNAL_ARM:
            raise ValueError(f"unregistered public arm label {row.get('arm')}")
        if row.get("requestedMaxFE") != "500000" or row.get("population") != "100":
            raise ValueError(f"pre-registration budget/population drift {row.get('runKey')}")
        pair = (row["instance"], row["seed"])
        if args.phase == "FIRST" and pair != FIRST_KEY:
            continue
        if args.phase == "REMAINDER" and pair == FIRST_KEY:
            continue
        selected.append(row)
    expected = {"FIRST": 2, "REMAINDER": 58, "ALL": 60}[args.phase]
    if len(selected) != expected:
        raise ValueError(f"phase {args.phase} expected {expected} rows, got {len(selected)}")

    profile_rows = {(row["arm"], row["seed"]): row for row in csv_rows(args.profiles)}
    jar_hash = sha256(args.frozen_jar)
    cpu_sets = [part.strip() for part in args.cpu_sets.split(";") if part.strip()]
    runs: list[dict] = []
    for ordinal, row in enumerate(sorted(selected, key=lambda value: (value["instance"], int(value["seed"]), value["arm"]))):
        instance, seed = row["instance"], int(row["seed"])
        public_arm, arm = row["arm"], PUBLIC_TO_INTERNAL_ARM[row["arm"]]
        snapshot = args.snapshot_root / instance / f"seed-{seed}.fourvec"
        receipt = properties(snapshot.with_name(snapshot.name + ".receipt.properties"))
        profile = profile_rows.get((arm, str(seed)))
        if profile is None:
            raise ValueError(f"missing profile {arm}/{seed}")
        instance_path = args.java_project / "EADHFSP" / f"{instance}.txt"
        setup = args.java_project / "instance-extensions" / "v1" / f"{instance}.setup.txt"
        fatigue = args.java_project / "fatigue-parameters" / "v1" / f"{instance}.fatigue.txt"
        for path in (snapshot, instance_path, setup, fatigue):
            if not path.is_file():
                raise ValueError(f"missing input {path}")
        run_id = row["runKey"]
        plan_path = args.plan_root / instance / f"seed-{seed}" / f"{arm}.properties"
        output = args.output_root / instance / f"seed-{seed}" / arm
        plan = {
            "schema": "v35-a2-a4-confirmation-run-plan-v1", "purpose": "CONFIRMATION",
            "runId": run_id, "preRegisteredRunKey": row["runKey"],
            "preRegisteredArmLabel": public_arm,
            "instance": instance, "arm": arm, "seed": seed, "population": 100,
            "maxFEs": 500000, "frozenJar": args.frozen_jar.resolve().as_posix(), "frozenJarSha256": jar_hash,
            "instancePath": instance_path.resolve().as_posix(), "instanceSha256": sha256(instance_path),
            "setupPath": setup.resolve().as_posix(), "setupFileSha256": sha256(setup),
            "fatiguePath": fatigue.resolve().as_posix(), "fatigueFileSha256": sha256(fatigue),
            "setupConfigurationSha256": receipt["setupConfigurationSha256"],
            "fatigueConfigurationSha256": receipt["fatigueConfigurationSha256"],
            "problemConfigurationSha256": receipt["problemConfigurationSha256"],
            "snapshotPath": snapshot.resolve().as_posix(), "snapshotSha256": receipt["snapshotSha256"],
            "initialPopulationHashV35": receipt["initialPopulationHashV35"],
            "initialPopulationHashP8": receipt["initialPopulationHashP8"],
            "armProfileSha256": profile["profileSha256"],
            "runtimeConfigurationSha256": profile["runtimeConfigurationSha256"],
            "launcherAcceptanceOnly": "false", "includedInFormalStatistics": "false",
            "includedInReferenceFront": "false",
        }
        write_properties(plan_path, plan)
        run = {
            "runId": run_id, "preRegisteredRunKey": row["runKey"],
            "preRegisteredArmLabel": public_arm,
            "arm": arm, "instance": instance, "seed": seed, "maxFEs": 500000,
            "jarSha256": jar_hash, "armProfileSha256": profile["profileSha256"],
            "snapshotSha256": receipt["snapshotSha256"],
            "initialPopulationHashV35": receipt["initialPopulationHashV35"],
            "initialPopulationHashP8": receipt["initialPopulationHashP8"],
            "problemConfigurationSha256": receipt["problemConfigurationSha256"],
            "protocolVersion": "v35-a2-a4-heldout-confirmation-v1",
            "purpose": "CONFIRMATION", "plan": plan_path.resolve().as_posix(),
            "output": output.resolve().as_posix(), "cpuSet": cpu_sets[ordinal % len(cpu_sets)],
        }
        run["command"] = ["taskset", "-c", run["cpuSet"], "java", "-Xmx4g", "-cp",
            f"{args.confirmation_classes.resolve().as_posix()}:{args.external_classes.resolve().as_posix()}:{args.frozen_jar.resolve().as_posix()}",
            "org.uma.jmetal.runner.lc_psode.ZhangBoV35ConfirmationA2A4ArmRunner",
            "--plan", run["plan"], "--output", run["output"]]
        run["runKey"] = run_key(run)
        runs.append(run)
    check_groups(runs, expected)
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    manifest = {"schema": SCHEMA, "phase": args.phase, "outputRoot": args.output_root.resolve().as_posix(),
                "runs": runs, "preRegisteredRegistrySha256": sha256(args.registry)}
    args.manifest.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"CONFIRMATION_RENDERED phase={args.phase} runs={len(runs)} groups={len(runs)//2}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
