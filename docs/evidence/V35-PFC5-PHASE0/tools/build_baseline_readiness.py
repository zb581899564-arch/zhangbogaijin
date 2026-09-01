#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PFC5-1D: build baseline-fair-readiness.csv with verified source hashes.

Honest states only: fairReady=true requires implementation + budget/decoder evidence;
everything else is NOT_READY / PENDING_SOURCE_VERIFIED with explicit blockingReason.
Zero FE; read-only.
"""
import csv
import hashlib
import json
import os
import sys
from datetime import datetime, timezone

PHASE0 = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(PHASE0)))
OUT_DIR = os.path.join(PHASE0, "03-baseline-readiness")

JA = "java-jmetal58/jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective"
JE = "java-jmetal58/jmetal-exec/src/main/java/org/uma/jmetal/runner/lc_psode"
JP = "java-jmetal58/jmetal-problem/src/main/java/org/uma/jmetal/problem/multiobjective"
PILOT = "docs/evidence/V35-P25D-all-algorithms-50k-pilot/download-20260815/results/runs"


def sha(rel):
    path = os.path.join(REPO, rel.replace("/", os.sep))
    if not os.path.exists(path):
        return "FILE_NOT_FOUND"
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


ROWS = [
    {
        "paperName": "HMOPSO-QGS-F (canonical fair baseline A0 / B0)",
        "localLabel": "HMOPSO_QGS_F",
        "sourceClass": "org.uma.jmetal.algorithm.multiobjective.hmopsoqgs.PublishedHmopsoQgs (+Builder, QGbestController)",
        "sourceKind": "project formal implementation (fair adaptation A0_HMOPSO_QGS_F_FAIR_ADAPTATION)",
        "sourceSHA256": sha("%s/hmopsoqgs/PublishedHmopsoQgs.java" % JA),
        "runner": "%s/ZhangBoV35FormalComparisonRunner.java" % JE,
        "problemAdapter": "EDHHFSPW canonical problem via formal comparison runner",
        "objectiveMapping": "[0,1,6]=[Cmax,TEC,TWC]",
        "initialPopulationInjection": "supported (shared four-vector snapshots; pilot initialPopulationHash recorded)",
        "randomSourceInjection": "verified in P8.5 audit (formal structured baseline; no global JMetalRandom leakage after v3.5 fix)",
        "exactDecoderBudget": "verified: decoder timing + FE ledger in P8.3 performance gates (20k/100k) and P25D 50k pilot",
        "phaseBudgetCompatibility": "PHASE_CONSISTENT_BUDGET_TERMINATION compatible (remainingFE<5000 window observed)",
        "FM3": "true", "ShiftMode": "NONE", "familyMode": "DEGENERATE_SINGLE_FAMILY", "setupMode": "SEQUENCE_INDEPENDENT",
        "forbiddenV35MechanismsAbsent": "true (pilot mechanismSummary: qpActions=0, cfvfOffspring=0, caTaLiteTest=0, dscr=disabled, dualQ=0)",
        "2kSmokeStatus": "COVERED_BY_P8_CHAIN",
        "50kPilotStatus": "COMPLETED x5 seeds (20260822..20260826), mode=V35_BASELINE, status=COMPLETED",
        "deterministicReplayStatus": "NOT_REPLAYED_YET",
        "fairReady": "true",
        "blockingReason": "",
    },
    {
        "paperName": "NSGA-II-F (official jMetal 5.8 core)",
        "localLabel": "NSGA_II_F",
        "sourceClass": "org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.official.OfficialJMetal58NSGAII",
        "sourceKind": "official jMetal core + whitelist adaptation; currently driven through V35P25DComparativeEngine",
        "sourceSHA256": sha("%s/mypso/v35/p25e/official/OfficialJMetal58NSGAII.java" % JA),
        "runner": "%s/ZhangBoV35P25DRunner.java (via V35P25DComparativeEngine)" % JE,
        "problemAdapter": "shared EDHHFSPW adapter via P25D engine",
        "objectiveMapping": "[0,1,6]=[Cmax,TEC,TWC]",
        "initialPopulationInjection": "shared snapshots injected in P25D pilot",
        "randomSourceInjection": "engine-level; per-arm seed handling to be re-audited before Validation",
        "exactDecoderBudget": "decoder timing recorded in pilot status",
        "phaseBudgetCompatibility": "assumed compatible; not phase-audited",
        "FM3": "true (shared decoder)", "ShiftMode": "NONE", "familyMode": "DEGENERATE_SINGLE_FAMILY", "setupMode": "SEQUENCE_INDEPENDENT",
        "forbiddenV35MechanismsAbsent": "true (no V35 mechanisms in official core)",
        "2kSmokeStatus": "NOT_RECORDED",
        "50kPilotStatus": "COMPLETED x5 seeds (P25D pilot)",
        "deterministicReplayStatus": "NOT_REPLAYED_YET",
        "fairReady": "pending_gate",
        "blockingReason": "AGENTS: V35P25DComparativeEngine is historical-engineering-only and must not enter paper reference; needs extraction onto a whitelisted runner + upstream jMetal commit/license record before Validation",
    },
    {
        "paperName": "SPEA2-F (official jMetal 5.8 core)",
        "localLabel": "SPEA2_F",
        "sourceClass": "org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.official.OfficialJMetal58SPEA2",
        "sourceKind": "official jMetal core + whitelist adaptation; currently driven through V35P25DComparativeEngine",
        "sourceSHA256": sha("%s/mypso/v35/p25e/official/OfficialJMetal58SPEA2.java" % JA),
        "runner": "%s/ZhangBoV35P25DRunner.java (via V35P25DComparativeEngine)" % JE,
        "problemAdapter": "shared EDHHFSPW adapter via P25D engine",
        "objectiveMapping": "[0,1,6]=[Cmax,TEC,TWC]",
        "initialPopulationInjection": "shared snapshots injected in P25D pilot",
        "randomSourceInjection": "engine-level; per-arm seed handling to be re-audited before Validation",
        "exactDecoderBudget": "decoder timing recorded in pilot status",
        "phaseBudgetCompatibility": "assumed compatible; not phase-audited",
        "FM3": "true (shared decoder)", "ShiftMode": "NONE", "familyMode": "DEGENERATE_SINGLE_FAMILY", "setupMode": "SEQUENCE_INDEPENDENT",
        "forbiddenV35MechanismsAbsent": "true (no V35 mechanisms in official core)",
        "2kSmokeStatus": "NOT_RECORDED",
        "50kPilotStatus": "COMPLETED x5 seeds (P25D pilot)",
        "deterministicReplayStatus": "NOT_REPLAYED_YET",
        "fairReady": "pending_gate",
        "blockingReason": "same P25D-engine restriction as NSGA-II-F",
    },
    {
        "paperName": "HMOPSO-QLS-F",
        "localLabel": "HMOPSO_QLS_F",
        "sourceClass": "embedded in V35P25DComparativeEngine (enum arm HMOPSO_QLS_F, Kind.PSO_QLS)",
        "sourceKind": "engine-embedded re-implementation; no standalone verified source",
        "sourceSHA256": sha("%s/mypso/v35/V35P25DComparativeEngine.java" % JA),
        "runner": "%s/ZhangBoV35P25DRunner.java (via V35P25DComparativeEngine)" % JE,
        "problemAdapter": "shared EDHHFSPW adapter via P25D engine",
        "objectiveMapping": "[0,1,6]=[Cmax,TEC,TWC]",
        "initialPopulationInjection": "shared snapshots injected in P25D pilot",
        "randomSourceInjection": "engine-level; NOT audited",
        "exactDecoderBudget": "decoder timing recorded in pilot status",
        "phaseBudgetCompatibility": "unknown",
        "FM3": "assumed true (shared decoder)", "ShiftMode": "NONE", "familyMode": "DEGENERATE_SINGLE_FAMILY", "setupMode": "SEQUENCE_INDEPENDENT",
        "forbiddenV35MechanismsAbsent": "assumed true; not audited per-mechanism",
        "2kSmokeStatus": "NOT_RECORDED",
        "50kPilotStatus": "COMPLETED x5 seeds (P25D pilot)",
        "deterministicReplayStatus": "NOT_REPLAYED_YET",
        "fairReady": "false",
        "blockingReason": "PENDING_SOURCE_VERIFICATION: algorithm lives inside the prohibited P25D comparative engine; paper-faithfulness (QLS semantics vs Li thesis ch.3) unverified",
    },
    {
        "paperName": "MOEA/D-F",
        "localLabel": "MOEAD (inherited chapter-3 code)",
        "sourceClass": "org.uma.jmetal.runner.lc_psode.MOEADRun + org.uma.jmetal.problem.multiobjective.dfsp.DHFSP_MOEAD",
        "sourceKind": "author-inherited jMetal code, not adapted to V35 fair harness",
        "sourceSHA256": sha("%s/dfsp/DHFSP_MOEAD.java" % JP),
        "runner": "%s/MOEADRun.java" % JE,
        "problemAdapter": "DHFSP_MOEAD (legacy chapter-3 problem class, not the canonical FM3 path)",
        "objectiveMapping": "unknown/legacy",
        "initialPopulationInjection": "not supported (evidence)",
        "randomSourceInjection": "not audited",
        "exactDecoderBudget": "not audited",
        "phaseBudgetCompatibility": "unknown",
        "FM3": "false (legacy problem path)", "ShiftMode": "unknown", "familyMode": "unknown", "setupMode": "unknown",
        "forbiddenV35MechanismsAbsent": "not audited",
        "2kSmokeStatus": "NOT_RECORDED",
        "50kPilotStatus": "NOT_RUN (absent from P25D pilot roster)",
        "deterministicReplayStatus": "NOT_REPLAYED_YET",
        "fairReady": "false",
        "blockingReason": "NOT_READY: legacy problem class, no shared-decoder/budget adaptation, no fair-run evidence",
    },
    {
        "paperName": "QMOEA",
        "localLabel": "QMOEA",
        "sourceClass": "ABSENT",
        "sourceKind": "no implementation in either Java project",
        "sourceSHA256": "FILE_NOT_FOUND",
        "runner": "",
        "problemAdapter": "", "objectiveMapping": "", "initialPopulationInjection": "no",
        "randomSourceInjection": "", "exactDecoderBudget": "", "phaseBudgetCompatibility": "",
        "FM3": "n/a", "ShiftMode": "n/a", "familyMode": "n/a", "setupMode": "n/a",
        "forbiddenV35MechanismsAbsent": "n/a",
        "2kSmokeStatus": "NOT_RUN", "50kPilotStatus": "NOT_RUN",
        "deterministicReplayStatus": "NOT_REPLAYED_YET",
        "fairReady": "false",
        "blockingReason": "NOT_READY: source gap registered in ROADMAP/P9 plan (paper main-comparison hard gate); outside master-plan §33 algorithm list",
    },
]

FIELDS = ["paperName", "localLabel", "sourceClass", "sourceKind", "sourceSHA256", "runner",
          "problemAdapter", "objectiveMapping", "initialPopulationInjection",
          "randomSourceInjection", "exactDecoderBudget", "phaseBudgetCompatibility",
          "FM3", "ShiftMode", "familyMode", "setupMode", "forbiddenV35MechanismsAbsent",
          "2kSmokeStatus", "50kPilotStatus", "deterministicReplayStatus", "fairReady",
          "blockingReason"]


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    out_csv = os.path.join(OUT_DIR, "baseline-fair-readiness.csv")
    with open(out_csv, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=FIELDS, lineterminator="\n")
        w.writeheader()
        for r in ROWS:
            w.writerow(r)
    ready = [r["localLabel"] for r in ROWS if r["fairReady"] == "true"]
    summary = {"fairReadyNow": ready,
               "pendingGate": [r["localLabel"] for r in ROWS if r["fairReady"] == "pending_gate"],
               "notReady": [r["localLabel"] for r in ROWS if r["fairReady"] == "false"],
               "generatedAtUtc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
               "consumedFE": 0, "changedAlgorithm": False}
    with open(os.path.join(OUT_DIR, "readiness-summary.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2)
    print(json.dumps(summary, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
