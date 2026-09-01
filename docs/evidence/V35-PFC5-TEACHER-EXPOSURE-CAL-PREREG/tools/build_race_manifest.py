#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V35-PFC5-CAL-P0: emit the preregistered Configuration Race manifest (32 RunKeys).

Zero FE. No algorithm run. Every hash field that can only be produced by the frozen
code is written as TO_BE_COMPUTED_BY_FROZEN_CODE, never guessed.
"""
import csv
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PREREG = os.path.dirname(HERE)
OUT = os.path.join(PREREG, "05-run-protocol", "configuration-race-manifest.csv")

CONFIGS = [
    ("C0_CURRENT", "0"),
    ("C1_WEAK", "0.05"),
    ("C2_MEDIUM", "0.15"),
    ("C3_STRONG", "0.30"),
]
INSTANCES = ["20_2_3_1", "50_2_3_1", "100_2_4_1", "100_8_3_1"]
SEEDS = ["20260906", "20260907"]

MAX_FES = 250000
BUDGET = "PHASE_CONSISTENT_BUDGET_TERMINATION"
TBD = "TO_BE_COMPUTED_BY_FROZEN_CODE"

FIELDS = [
    "runKey", "configuration", "lambda", "instance", "seed", "maxFEs",
    "budgetProtocol", "snapshotSha256", "initialPopulationHashV35",
    "initialPopulationHashP8", "algorithmJarSha256",
    "calibrationImplementationJarSha256", "profileHash",
    "problemConfigurationHash", "status",
]


def main():
    rows = []
    for instance in INSTANCES:
        for seed in SEEDS:
            for cfg, lam in CONFIGS:
                run_key = "V35CAL-%s-%s-%s" % (instance, seed, cfg)
                rows.append({
                    "runKey": run_key,
                    "configuration": cfg,
                    "lambda": lam,
                    "instance": instance,
                    "seed": seed,
                    "maxFEs": MAX_FES,
                    "budgetProtocol": BUDGET,
                    "snapshotSha256": TBD,
                    "initialPopulationHashV35": TBD,
                    "initialPopulationHashP8": TBD,
                    "algorithmJarSha256": TBD,
                    "calibrationImplementationJarSha256": TBD,
                    "profileHash": TBD,
                    "problemConfigurationHash": TBD,
                    "status": "PREREGISTERED_NOT_STARTED",
                })
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=FIELDS, lineterminator="\n")
        w.writeheader()
        w.writerows(rows)
    print("runKeys=%d" % len(rows))
    print("configs=%d instances=%d seeds=%d maxFEs=%d" % (
        len(CONFIGS), len(INSTANCES), len(SEEDS), MAX_FES))
    print("written=%s" % os.path.relpath(OUT, PREREG))
    return 0


if __name__ == "__main__":
    sys.exit(main())
