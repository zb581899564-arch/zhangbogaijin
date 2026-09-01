#!/usr/bin/env python3
"""Build FC5-T's read-only 100-job background ledger from accepted evidence."""
from __future__ import annotations

import argparse
import csv
import hashlib
import statistics
from pathlib import Path


def read_csv(path: Path):
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def median(rows, key):
    values = [float(row[key]) for row in rows if row.get(key, "") not in ("", None)]
    return statistics.median(values) if values else ""


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-root", required=True, type=Path)
    args = parser.parse_args()
    root = args.project_root.resolve()
    evidence = root / "docs" / "evidence"
    output = evidence / "V35-FC5-100JOB-TRANSFER" / "01-existing-100job-background"
    output.mkdir(parents=True, exist_ok=True)

    sources = [
        ("V35-A2-FINAL-CANDIDATE-CONFIRMATION", "A0_vs_A2",
         evidence / "V35-A2-FINAL-CANDIDATE-CONFIRMATION" / "04-analysis" / "analysis" / "metrics.csv",
         {"100_2_5_1", "100_8_3_1"}),
        ("V35-A2-A4-MULTIINSTANCE-CONFIRMATION", "A2_vs_A4",
         evidence / "V35-A2-A4-MULTIINSTANCE-CONFIRMATION" / "06-remote-analysis-import" / "metrics.csv",
         {"100_2_4_1", "100_5_3_1"}),
        ("V35-STAGE2-PILOT-A0-A4-20260823", "Stage2_background",
         evidence / "V35-STAGE2-PILOT-A0-A4-20260823" / "results" / "metrics.csv",
         None),
    ]
    standardized = []
    for campaign, comparison, path, instances in sources:
        if not path.is_file():
            raise SystemExit("missing accepted evidence: {}".format(path))
        for row in read_csv(path):
            instance = row.get("instance", "100_2_3_1")
            if instances is not None and instance not in instances:
                continue
            standardized.append({
                "campaign": campaign,
                "comparison": comparison,
                "instance": instance,
                "seed": row.get("seed", ""),
                "arm": row.get("arm", ""),
                "actualFE": row.get("actualFE", ""),
                "utilizationRate": row.get("utilizationRate", row.get("utilization", "")),
                "HV": row.get("HV", row.get("hv", "")),
                "IGD": row.get("IGD", row.get("igd", "")),
                "Cmax": row.get("Cmax", row.get("minCmax", "")),
                "TEC": row.get("TEC", row.get("minTEC", "")),
                "TWC": row.get("TWC", row.get("minTWC", "")),
                "frontSize": row.get("frontSize", row.get("n", "")),
                "sourceFile": str(path.relative_to(root)).replace("\\", "/"),
            })

    raw_fields = list(standardized[0])
    with (output / "existing-100job-run-metrics.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=raw_fields)
        writer.writeheader(); writer.writerows(standardized)

    groups = {}
    for row in standardized:
        groups.setdefault((row["campaign"], row["comparison"], row["instance"], row["arm"]), []).append(row)
    fields = ["campaign", "comparison", "instance", "arm", "acceptedRuns", "medianActualFE",
              "medianUtilizationRate", "medianHV", "medianIGD", "medianCmax", "medianTEC",
              "medianTWC", "medianFrontSize", "telemetryAvailability"]
    summary = []
    for (campaign, comparison, instance, arm), rows in sorted(groups.items()):
        summary.append({
            "campaign": campaign, "comparison": comparison, "instance": instance, "arm": arm,
            "acceptedRuns": len(rows), "medianActualFE": median(rows, "actualFE"),
            "medianUtilizationRate": median(rows, "utilizationRate"), "medianHV": median(rows, "HV"),
            "medianIGD": median(rows, "IGD"), "medianCmax": median(rows, "Cmax"),
            "medianTEC": median(rows, "TEC"), "medianTWC": median(rows, "TWC"),
            "medianFrontSize": median(rows, "frontSize"),
            "telemetryAvailability": "Cmax_lifecycle_only;merge_pool_and_directional_lifecycle_absent",
        })
    with (output / "100job-background-summary.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader(); writer.writerows(summary)

    contrasts = [
        ("A0_vs_A2", "100_2_5_1", "POSITIVE", "A2 relative to A0",
         evidence / "V35-A2-FINAL-CANDIDATE-CONFIRMATION" / "04-analysis" / "analysis" / "instance-summary.csv"),
        ("A0_vs_A2", "100_8_3_1", "NEGATIVE", "A2 relative to A0",
         evidence / "V35-A2-FINAL-CANDIDATE-CONFIRMATION" / "04-analysis" / "analysis" / "instance-summary.csv"),
        ("A2_vs_A4", "100_2_4_1", "POSITIVE", "A4 relative to A2",
         evidence / "V35-A2-A4-MULTIINSTANCE-CONFIRMATION" / "06-remote-analysis-import" / "instance-summary.csv"),
        ("A2_vs_A4", "100_5_3_1", "NEGATIVE", "A4 relative to A2",
         evidence / "V35-A2-A4-MULTIINSTANCE-CONFIRMATION" / "06-remote-analysis-import" / "instance-summary.csv"),
    ]
    contrast_rows = []
    for comparison, instance, sign, direction, path in contrasts:
        record = next(row for row in read_csv(path) if row["instance"] == instance)
        contrast_rows.append({
            "comparison": comparison, "instance": instance, "contrastRole": sign,
            "treatmentDirection": direction, "pairedSeeds": record.get("pairs", "5"),
            "medianDeltaCmax": record["medianDeltaCmax"], "medianDeltaTEC": record["medianDeltaTEC"],
            "medianDeltaTWC": record["medianDeltaTWC"], "medianDeltaHV": record["medianDeltaHV"],
            "medianDeltaIGD": record["medianDeltaIGD"],
            "mergePoolTelemetry": "MISSING_REPLAY_REQUIRED",
            "rootCauseStatus": "CONTEXT_ONLY",
            "sourceFile": str(path.relative_to(root)).replace("\\", "/"),
        })
    with (output.parent / "04-positive-negative-contrast" / "positive-negative-contrast-matrix.csv").open(
            "w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(contrast_rows[0]))
        writer.writeheader(); writer.writerows(contrast_rows)

    # The first telemetry tier is intentionally capped at 24 runs: three
    # paired seeds for each of the four registered positive/negative 100-job
    # contrasts.  It is a registry, not a scheduler; no replay is started by
    # this script.
    registry = []
    seeds_by_comparison = {
        "A0_vs_A2": ("20260911", "20260912", "20260913"),
        "A2_vs_A4": ("20260901", "20260902", "20260903"),
    }
    arms_by_comparison = {
        "A0_vs_A2": ("A0_BASELINE", "A2_CFVF"),
        "A2_vs_A4": ("A2_CFVF", "A4_BUDGET_AWARE_CATA"),
    }
    for comparison, instance, role, direction, source in contrasts:
        for seed in seeds_by_comparison[comparison]:
            for arm in arms_by_comparison[comparison]:
                registry.append({
                    "diagnosticRunId": "FC5T-{}-{}-{}-{}".format(
                        comparison, instance, seed, arm),
                    "comparison": comparison,
                    "contrastRole": role,
                    "instance": instance,
                    "seed": seed,
                    "arm": arm,
                    "initialBudget": "50000",
                    "budgetEscalation": "50000->100000->250000->500000_if_required",
                    "sourceRunId": ("V35A2FINAL" if comparison == "A0_vs_A2"
                                      else "V35A2A4") + "-" + instance + "-" + seed
                                      + "-" + ("A0" if arm == "A0_BASELINE"
                                                 else "A4" if arm == "A4_BUDGET_AWARE_CATA"
                                                 else "A2"),
                    "telemetryMode": "FC5_100JOB_TRANSFER_V1_OBSERVER_ONLY",
                    "status": "PRE_REGISTERED_NOT_STARTED",
                    "startCondition": "core_merge_pool_fields_absent_in_historical_run",
                    "paperUse": "ROOT_CAUSE_DIAGNOSTIC_ONLY",
                })
    registry_path = output.parent / "03-transfer-telemetry" / "telemetry-replay-registry.csv"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    with registry_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(registry[0]))
        writer.writeheader(); writer.writerows(registry)

    snapshot_matrix = []
    for comparison, instance, role, direction, source in contrasts:
        seeds = seeds_by_comparison[comparison]
        campaign_dir = (evidence / "V35-A2-FINAL-CANDIDATE-CONFIRMATION"
                        if comparison == "A0_vs_A2"
                        else evidence / "V35-A2-A4-MULTIINSTANCE-CONFIRMATION")
        local_candidates = list(campaign_dir.rglob("*.fourvec"))
        present = []
        for seed in seeds:
            expected = "seed-{}.fourvec".format(seed)
            if any(path.name == expected and instance in path.parts for path in local_candidates):
                present.append(seed)
        snapshot_matrix.append({
            "comparison": comparison,
            "contrastRole": role,
            "instance": instance,
            "firstTierSeeds": ";".join(seeds),
            "localVerifiedSnapshotCount": len(present),
            "localVerifiedSeeds": ";".join(present) if present else "NONE",
            "requiredSnapshotRule": "original_confirmed_four_vector_snapshot_and_hash",
            "recoveryStatus": ("READY" if len(present) == len(seeds)
                               else "RECOVERY_REQUIRED_BEFORE_REPLAY"),
            "recoverySource": "original_remote_campaign_or_post_20260823_archive",
        })
    snapshot_path = output.parent / "03-transfer-telemetry" / "snapshot-recovery-matrix.csv"
    with snapshot_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(snapshot_matrix[0]))
        writer.writeheader(); writer.writerows(snapshot_matrix)

    # The manifest covers both evidence files and the small, explicitly named
    # observer implementation surface.  It intentionally excludes itself so a
    # second execution is stable.
    tracked = [
        path for path in (evidence / "V35-FC5-100JOB-TRANSFER").rglob("*")
        if path.is_file() and path.name != "evidence-sha256.tsv"
    ]
    tracked.extend([
        root / "java-jmetal58/jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/"
               "multiobjective/mypso/v35/V35Fc5TransferAudit.java",
        root / "java-jmetal58/jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/"
               "multiobjective/mypso/ZhangBoMOHPSOQ.java",
        root / "java-jmetal58/jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/"
               "multiobjective/mypso/v35/V35FairRunner.java",
        root / "java-jmetal58/jmetal-exec/src/main/java/org/uma/jmetal/runner/lc_psode/"
               "ZhangBoV35Fc5TransferRunner.java",
        root / "java-jmetal58/jmetal-algorithm/src/test/java/org/uma/jmetal/algorithm/"
               "multiobjective/mypso/zhangbo/V35Fc5TransferAuditTest.java",
        root / "java-jmetal58/jmetal-algorithm/src/test/java/org/uma/jmetal/algorithm/"
               "multiobjective/mypso/v35/V35Fc5TransferTelemetryEquivalenceTest.java",
    ])
    manifest = evidence / "V35-FC5-100JOB-TRANSFER" / "evidence-sha256.tsv"
    with manifest.open("w", encoding="utf-8", newline="") as handle:
        handle.write("sha256\tbytes\tpath\n")
        for path in sorted(tracked, key=lambda value: str(value).lower()):
            if not path.is_file():
                raise SystemExit("missing tracked FC5-T artifact: {}".format(path))
            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            try:
                label = path.relative_to(root).as_posix()
            except ValueError:
                label = path.as_posix()
            handle.write("{}\t{}\t{}\n".format(digest, path.stat().st_size, label))


if __name__ == "__main__":
    main()
