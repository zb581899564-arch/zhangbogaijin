#!/usr/bin/env python3
"""Render fail-closed A0/A2 final-candidate confirmation plans.

This tool never builds the algorithm and never creates a candidate population.  It only binds
pre-registered inputs to the immutable external Stage2 runner.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
from pathlib import Path

SCHEMA = "v35-a2-final-candidate-manifest-v1"
PLAN_SCHEMA = "v35-a2-final-candidate-confirmation-run-plan-v1"
PURPOSE = "FINAL_CANDIDATE_CONFIRMATION"
PUBLIC_TO_INTERNAL = {"A0_BASELINE": "A0", "A2_CFVF": "A2"}
ARMS = tuple(sorted(PUBLIC_TO_INTERNAL.values()))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def props(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith(("#", "!")):
            key, value = line.split("=", 1)
            if key in result:
                raise ValueError("duplicate key %s in %s" % (key, path))
            result[key] = value
    return result


def write_properties(path: Path, values: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join("%s=%s\n" % (key, values[key]) for key in sorted(values)), encoding="utf-8")


def identity_hash(run: dict[str, object]) -> str:
    fields = ("runId", "arm", "instance", "seed", "maxFEs", "jarSha256", "armProfileSha256",
              "snapshotSha256", "initialPopulationHashV35", "initialPopulationHashP8",
              "problemConfigurationSha256", "protocolVersion")
    raw = json.dumps({key: run[key] for key in fields}, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--registry", required=True, type=Path)
    parser.add_argument("--java-project", required=True, type=Path)
    parser.add_argument("--frozen-jar", required=True, type=Path)
    parser.add_argument("--external-classes", required=True, type=Path)
    parser.add_argument("--candidate-classes", required=True, type=Path)
    parser.add_argument("--candidate-source-root", required=True, type=Path)
    parser.add_argument("--runner-mode", required=True, choices=("compiled", "java-source"))
    parser.add_argument("--snapshot-root", required=True, type=Path)
    parser.add_argument("--profiles", required=True, type=Path)
    parser.add_argument("--plan-root", required=True, type=Path)
    parser.add_argument("--output-root", required=True, type=Path)
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--max-fes", required=True, type=int, choices=(2000, 500000))
    parser.add_argument("--pairs", required=True, help="comma-separated instance:seed pairs")
    parser.add_argument("--cpu-sets", default="0-1;2-3;4-5;6-7;8-9;10-11;12-13;14-15")
    args = parser.parse_args()

    registered = rows(args.registry)
    if len(registered) != 60:
        raise ValueError("registry must contain exactly 60 rows")
    requested = tuple(part.strip() for part in args.pairs.split(",") if part.strip())
    if not requested:
        raise ValueError("at least one pair is required")
    pair_set = {tuple(part.split(":", 1)) for part in requested}
    if any(len(pair) != 2 for pair in pair_set):
        raise ValueError("pair must be instance:seed")
    selected = [row for row in registered if (row["instance"], row["seed"]) in pair_set]
    if len(selected) != 2 * len(pair_set):
        raise ValueError("requested pair is not exactly present in registry")
    for row in selected:
        if row["arm"] not in PUBLIC_TO_INTERNAL or row["requestedMaxFE"] != "500000" or row["population"] != "100":
            raise ValueError("registry drift for %s" % row["runKey"])

    profile_rows = {(row["arm"], row["seed"]): row for row in rows(args.profiles)}
    jar_hash = sha256(args.frozen_jar)
    cpu_sets = [item.strip() for item in args.cpu_sets.split(";") if item.strip()]
    runs: list[dict[str, object]] = []
    for ordinal, row in enumerate(sorted(selected, key=lambda value: (value["instance"], int(value["seed"]), value["arm"]))):
        internal = PUBLIC_TO_INTERNAL[row["arm"]]
        instance, seed = row["instance"], int(row["seed"])
        snapshot = args.snapshot_root / instance / ("seed-%d.fourvec" % seed)
        receipt = props(snapshot.with_name(snapshot.name + ".receipt.properties"))
        profile = profile_rows.get((internal, str(seed)))
        if profile is None:
            raise ValueError("missing profile %s/%s" % (internal, seed))
        instance_path = args.java_project / "EADHFSP" / (instance + ".txt")
        setup = args.java_project / "instance-extensions" / "v1" / (instance + ".setup.txt")
        fatigue = args.java_project / "fatigue-parameters" / "v1" / (instance + ".fatigue.txt")
        for item in (snapshot, instance_path, setup, fatigue):
            if not item.is_file():
                raise ValueError("missing input %s" % item)
        kind = "PREFLIGHT" if args.max_fes == 2000 else PURPOSE
        run_id = "%s-%s" % (row["runKey"], kind)
        plan_path = args.plan_root / instance / ("seed-%d" % seed) / (internal + ".properties")
        output = args.output_root / instance / ("seed-%d" % seed) / internal
        plan = {
            "schema": PLAN_SCHEMA, "purpose": PURPOSE, "runId": run_id,
            "preRegisteredRunKey": row["runKey"], "preRegisteredArmLabel": row["arm"],
            "instance": instance, "arm": internal, "seed": seed, "population": 100, "maxFEs": args.max_fes,
            "frozenJar": args.frozen_jar.resolve().as_posix(), "frozenJarSha256": jar_hash,
            "instancePath": instance_path.resolve().as_posix(), "instanceSha256": sha256(instance_path),
            "setupPath": setup.resolve().as_posix(), "setupFileSha256": sha256(setup),
            "fatiguePath": fatigue.resolve().as_posix(), "fatigueFileSha256": sha256(fatigue),
            "setupConfigurationSha256": receipt["setupConfigurationSha256"],
            "fatigueConfigurationSha256": receipt["fatigueConfigurationSha256"],
            "problemConfigurationSha256": receipt["problemConfigurationSha256"],
            "snapshotPath": snapshot.resolve().as_posix(), "snapshotSha256": receipt["snapshotSha256"],
            "initialPopulationHashV35": receipt["initialPopulationHashV35"],
            "initialPopulationHashP8": receipt["initialPopulationHashP8"],
            "armProfileSha256": profile["profileSha256"], "runtimeConfigurationSha256": profile["runtimeConfigurationSha256"],
            "launcherAcceptanceOnly": "false", "includedInFormalStatistics": "false", "includedInReferenceFront": "false",
            "runKind": kind,
            "externalToolExecutionMode": "JAVA8_COMPILED" if args.runner_mode == "compiled" else "JAVA11_SOURCE_LAUNCHER",
        }
        write_properties(plan_path, plan)
        run: dict[str, object] = {
            "runId": run_id, "preRegisteredRunKey": row["runKey"], "preRegisteredArmLabel": row["arm"],
            "arm": internal, "instance": instance, "seed": seed, "maxFEs": args.max_fes, "jarSha256": jar_hash,
            "armProfileSha256": profile["profileSha256"], "snapshotSha256": receipt["snapshotSha256"],
            "initialPopulationHashV35": receipt["initialPopulationHashV35"], "initialPopulationHashP8": receipt["initialPopulationHashP8"],
            "problemConfigurationSha256": receipt["problemConfigurationSha256"],
            "protocolVersion": "v35-a2-final-candidate-confirmation-v1", "purpose": PURPOSE,
            "runKind": kind, "plan": plan_path.resolve().as_posix(), "output": output.resolve().as_posix(),
            "cpuSet": cpu_sets[ordinal % len(cpu_sets)],
        }
        run["runKey"] = identity_hash(run)
        if args.runner_mode == "compiled":
            run["command"] = ["taskset", "-c", str(run["cpuSet"]), "java", "-Xmx4g", "-cp",
                              "%s:%s:%s" % (args.candidate_classes.resolve().as_posix(), args.external_classes.resolve().as_posix(), args.frozen_jar.resolve().as_posix()),
                              "org.uma.jmetal.runner.lc_psode.ZhangBoV35A2FinalCandidateArmRunner", "--plan", str(run["plan"]), "--output", str(run["output"])]
        else:
            source = args.candidate_source_root / "org" / "uma" / "jmetal" / "runner" / "lc_psode" / "ZhangBoV35A2FinalCandidateArmRunner.java"
            if not source.is_file():
                raise ValueError("missing source launcher %s" % source)
            run["command"] = ["taskset", "-c", str(run["cpuSet"]), "java", "-Xmx4g", "-cp",
                              "%s:%s" % (args.external_classes.resolve().as_posix(), args.frozen_jar.resolve().as_posix()),
                              str(source.resolve()), "--plan", str(run["plan"]), "--output", str(run["output"])]
        runs.append(run)

    groups: dict[tuple[str, int], list[dict[str, object]]] = {}
    for run in runs:
        groups.setdefault((str(run["instance"]), int(run["seed"])), []).append(run)
    for key, pair in groups.items():
        if tuple(sorted(str(row["arm"]) for row in pair)) != ARMS:
            raise ValueError("incomplete A0/A2 pair %s" % (key,))
    if len({str(row["runId"]) for row in runs}) != len(runs) or len({str(row["runKey"]) for row in runs}) != len(runs):
        raise ValueError("duplicate run identity")
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_text(json.dumps({"schema": SCHEMA, "runKind": "PREFLIGHT" if args.max_fes == 2000 else PURPOSE,
                                         "externalToolExecutionMode": "JAVA8_COMPILED" if args.runner_mode == "compiled" else "JAVA11_SOURCE_LAUNCHER",
                                         "runs": runs, "preRegisteredRegistrySha256": sha256(args.registry)}, indent=2), encoding="utf-8")
    print("A2_FINAL_CANDIDATE_RENDERED runs=%d groups=%d maxFEs=%d" % (len(runs), len(groups), args.max_fes))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
