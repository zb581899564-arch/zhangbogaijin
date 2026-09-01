#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V35-PFC5-F1 Step 4: run acceptance. Acceptance FIRST, metrics SECOND.

If any hard gate fails the verdict is RUN_INVALID and no HV/IGD may be computed
or interpreted. This script writes f1-run-acceptance.properties with
RUN_ACCEPTANCE=PASS|FAIL, which analyze_f1_frozen_reference.py checks before it
touches any F1 number.
"""
import csv
import math
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
F1 = os.path.dirname(HERE)
RAW = os.path.join(F1, "03-raw-run", "remote")
OUT = os.path.join(F1, "04-run-acceptance")

EXPECTED_JAR = "8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9"
EXPECTED_PROFILE = "5b3cc542dafc22c1a32f1c0994bae25ffef040f6bfdf2aa6090a42f86cfd79d1"
EXPECTED_SNAPSHOT = "84d845233e332a6612e5dfe93c97cbbeef40c4ee05766cbfd0e9446bd3043769"
EXPECTED_INIT_V35 = "179a82a3825566380ab6798aa898002d31565dad9d65802e57b295c2a4294c2d"
EXPECTED_INIT_P8 = "7c6f8b425f2781653ce9705b82050652f063b461b24c0f93d9486e2c686ca2d3"
EXPECTED_INSTANCE = "2e88fa97a6f84af347a4603f04c387a65c8f9891bcab8ac6b70fdec622ea35cf"
EXPECTED_SETUP_CONF = "E7E9FF7F646351FECB5801EC2EC177CEE2C00775173E4DE6841577695E8E58E1"
EXPECTED_FATIGUE_CONF = "81CAD959F27E461E41882E7353AC5F23574FA6DC50637F59E281B1E8788967A1"
EXPECTED_PROBLEM_CONF = "892c7c3feddd09848bf35bac1a90a529153ad77b3cb712a36f357cd214cc79f4"
REQUESTED_MAX_FE = 500000


def read_props(path):
    props = {}
    if not os.path.isfile(path):
        return props
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if "=" in line:
                k, v = line.split("=", 1)
                props[k.strip()] = v.strip()
    return props


def read_config_multiline(path):
    """configuration.txt is key=value but the profile block spans lines; read flat keys only."""
    return read_props(path)


def read_front(path):
    points = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            fields = line.strip().split(",")
            if len(fields) < 3 or fields[0].lower() == "cmax":
                continue
            points.append(tuple(float(v) for v in fields[:3]))
    return points


def mech_value(summary, key):
    if not summary:
        return None
    m = re.search(r"(?:^|[,|])" + re.escape(key) + r"=(-?\d+)", summary)
    return int(m.group(1)) if m else None


def main():
    raw = RAW if os.path.isdir(RAW) else os.path.join(F1, "03-raw-run")
    os.makedirs(OUT, exist_ok=True)

    status = read_props(os.path.join(raw, "status.properties"))
    budget = read_props(os.path.join(raw, "budget-termination.properties"))
    gate = read_props(os.path.join(raw, "formal-gate.properties"))
    prov = read_props(os.path.join(raw, "provenance.properties"))
    config = read_config_multiline(os.path.join(raw, "configuration.txt"))
    env = read_props(os.path.join(raw, "runtime-environment.properties"))
    passive = read_props(os.path.join(raw, "passive-summary.properties"))
    exitcode = read_props(os.path.join(raw, "exitcode.properties")).get("processExitCode", "MISSING")

    rows = []

    def check(gid, item, expected, actual, ok):
        rows.append({"gateId": gid, "item": item, "expected": str(expected),
                     "actual": str(actual), "verdict": "PASS" if ok else "FAIL"})
        return ok

    # --- exit code --------------------------------------------------------
    check("G01", "processExitCode", "0", exitcode, exitcode == "0")

    # --- status -----------------------------------------------------------
    st = status.get("status", "MISSING")
    check("G02", "status", "COMPLETED", st, st == "COMPLETED")

    # --- front ------------------------------------------------------------
    front_path = os.path.join(raw, "front.csv")
    front_ok = os.path.isfile(front_path)
    points = read_front(front_path) if front_ok else []
    check("G03", "frontExistsAndNonEmpty", ">0 points", len(points), front_ok and len(points) > 0)
    all_finite = bool(points) and all(all(math.isfinite(v) for v in p) for p in points)
    check("G04", "frontAllThreeObjectivesFinite", "true", all_finite, all_finite)

    # --- budget -----------------------------------------------------------
    actual_fe = status.get("fullEvaluations", "MISSING")
    decoder = status.get("decoderCalls", "MISSING")
    check("G05", "actualFE=decoderCalls", "equal", "fe=%s decoder=%s" % (actual_fe, decoder),
          actual_fe == decoder and actual_fe != "MISSING")
    try:
        fe_i = int(actual_fe)
    except ValueError:
        fe_i = -1
    check("G06", "actualFE<=requestedMaxFE", "<=%d" % REQUESTED_MAX_FE, fe_i, 0 < fe_i <= REQUESTED_MAX_FE)
    remaining = budget.get("remainingFE", "MISSING")
    try:
        rem_i = int(remaining)
    except ValueError:
        rem_i = 10 ** 9
    check("G07", "remainingFE<5000", "<5000", remaining, 0 <= rem_i < 5000)
    util = budget.get("utilizationRate", "MISSING")
    try:
        util_f = float(util)
    except ValueError:
        util_f = 0.0
    check("G08", "utilizationRate>0.99", ">0.99", util, util_f > 0.99)
    check("G09", "budgetTerminationKind", "EXACT_MAX_FE or PHASE_CONSISTENT_TAIL_STOP",
          budget.get("terminationKind", "MISSING"),
          budget.get("terminationKind", "") in ("EXACT_MAX_FE", "PHASE_CONSISTENT_TAIL_STOP"))

    # --- counters ---------------------------------------------------------
    check("G10", "illegalSolutions", "0", status.get("illegalSolutions", "MISSING"),
          status.get("illegalSolutions") == "0")
    check("G11", "duplicateEvaluations", "0", status.get("duplicateEvaluations", "MISSING"),
          status.get("duplicateEvaluations") == "0")

    mech = status.get("mechanismSummary", "")
    repairs = mech_value(mech, "cfvfRepairs")
    check("G12", "unexplainedRepairs (cfvfRepairs)", "0", repairs, repairs == 0)

    # sourceMissing: launcher checks passiveObservedCount == fullEvaluations as
    # "sourceObservationLoss"; the gate output is authoritative.
    gate_failures = gate.get("failures", "MISSING")
    no_loss = "sourceObservationLoss" not in gate_failures
    observed = passive.get("observedCount", "MISSING")
    observed_ok = (observed == str(actual_fe)) if observed != "MISSING" else no_loss
    check("G13", "sourceMissing (passiveObserved==fullEvaluations; gate free of sourceObservationLoss)",
          "0", "observedCount=%s gateFailures=%s" % (observed, gate_failures), no_loss and observed_ok)

    # --- launcher internal gate ------------------------------------------
    check("G14", "launcherFormalGateFailures", "NONE", gate_failures, gate_failures == "NONE")

    # --- identity / provenance -------------------------------------------
    check("G15", "formalJarSha256", EXPECTED_JAR, prov.get("frozenJarSha256", "MISSING"),
          prov.get("frozenJarSha256", "").lower() == EXPECTED_JAR)
    check("G16", "armProfileSha256", EXPECTED_PROFILE, prov.get("armProfileSha256", "MISSING"),
          prov.get("armProfileSha256", "").lower() == EXPECTED_PROFILE)
    check("G17", "snapshotSha256", EXPECTED_SNAPSHOT, prov.get("snapshotSha256", "MISSING"),
          prov.get("snapshotSha256", "").lower() == EXPECTED_SNAPSHOT)
    check("G18", "initialPopulationHashV35", EXPECTED_INIT_V35, prov.get("initialPopulationHashV35", "MISSING"),
          prov.get("initialPopulationHashV35", "").lower() == EXPECTED_INIT_V35)
    check("G19", "initialPopulationHashP8", EXPECTED_INIT_P8, prov.get("initialPopulationHashP8", "MISSING"),
          prov.get("initialPopulationHashP8", "").lower() == EXPECTED_INIT_P8)
    check("G20", "instanceSha256", EXPECTED_INSTANCE, prov.get("instanceSha256", "MISSING"),
          prov.get("instanceSha256", "").lower() == EXPECTED_INSTANCE)
    check("G21", "setupConfigurationSha256", EXPECTED_SETUP_CONF, prov.get("setupConfigurationSha256", "MISSING"),
          prov.get("setupConfigurationSha256", "").upper() == EXPECTED_SETUP_CONF)
    check("G22", "fatigueConfigurationSha256", EXPECTED_FATIGUE_CONF, prov.get("fatigueConfigurationSha256", "MISSING"),
          prov.get("fatigueConfigurationSha256", "").upper() == EXPECTED_FATIGUE_CONF)
    check("G23", "problemConfigurationSha256", EXPECTED_PROBLEM_CONF, prov.get("problemConfigurationSha256", "MISSING"),
          prov.get("problemConfigurationSha256", "").lower() == EXPECTED_PROBLEM_CONF)

    # --- semantic configuration ------------------------------------------
    telemetry_files = [f for f in os.listdir(raw)
                       if "telemetry" in f.lower() or "checkpoint" in f.lower()] if os.path.isdir(raw) else []
    cfg_has_telemetry = any("telemetry" in k.lower() for k in config)
    check("G24", "telemetry", "OFF (false)", "telemetryFiles=%d configHasTelemetryKey=%s" % (
        len(telemetry_files), cfg_has_telemetry),
        not telemetry_files and not cfg_has_telemetry)

    check("G25", "ShiftMode", "NONE", config.get("shiftMode", "MISSING"),
          config.get("shiftMode", "").upper() == "NONE")
    left_nanos = mech_value(mech, "leftShiftNanos")
    right_nanos = mech_value(mech, "rightShiftNanos")
    left_recomp = mech_value(mech, "leftRecomputations")
    right_recomp = mech_value(mech, "rightRecomputations")
    shift_quiet = (left_nanos == 0 and right_nanos == 0 and left_recomp == 0 and right_recomp == 0)
    check("G25b", "shiftRuntimeActivity", "0",
          "leftNanos=%s rightNanos=%s leftRecomp=%s rightRecomp=%s" % (
              left_nanos, right_nanos, left_recomp, right_recomp), shift_quiet)

    check("G26", "PDDR", "GLOBAL_ORIGINAL", config.get("pddrSelectionMode", "MISSING"),
          config.get("pddrSelectionMode", "").upper() == "GLOBAL_ORIGINAL")

    # --- run-contract identity -------------------------------------------
    check("G27", "seed", "20260901", config.get("seed", "MISSING"), config.get("seed") == "20260901")
    check("G28", "population", "100", config.get("population", "MISSING"), config.get("population") == "100")
    check("G29", "requestedMaxFE", "500000", budget.get("requestedMaxFE", "MISSING"),
          budget.get("requestedMaxFE") == "500000")
    check("G30", "arm", "A4", config.get("arm", "MISSING"), config.get("arm", "").upper() == "A4")
    check("G31", "runId", "V35PFC5F1-100_5_3_1-20260901-A4", config.get("runId", "MISSING"),
          config.get("runId") == "V35PFC5F1-100_5_3_1-20260901-A4")
    check("G32", "freshRun (snapshot-bound, not regenerated)", "snapshot-bound",
          "initial population read from frozen snapshot",
          prov.get("initialPopulationHashV35", "").lower() == EXPECTED_INIT_V35)

    failed = [r for r in rows if r["verdict"] == "FAIL"]
    passed = len(rows) - len(failed)
    verdict = "PASS" if not failed else "FAIL"

    with open(os.path.join(OUT, "run-acceptance.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["gateId", "item", "expected", "actual", "verdict"], lineterminator="\n")
        w.writeheader()
        w.writerows(rows)

    with open(os.path.join(OUT, "f1-run-acceptance.properties"), "w", encoding="utf-8") as f:
        f.write("RUN_ACCEPTANCE=%s\n" % verdict)
        f.write("gatesTotal=%d\n" % len(rows))
        f.write("gatesPassed=%d\n" % passed)
        f.write("gatesFailed=%d\n" % len(failed))
        f.write("failedGates=%s\n" % ("NONE" if not failed else ";".join(r["gateId"] for r in failed)))
        f.write("processExitCode=%s\n" % exitcode)
        f.write("status=%s\n" % st)
        f.write("requestedMaxFE=%s\n" % budget.get("requestedMaxFE", "MISSING"))
        f.write("actualFE=%s\n" % actual_fe)
        f.write("decoderCalls=%s\n" % decoder)
        f.write("remainingFE=%s\n" % remaining)
        f.write("qPhaseFE=%s\n" % budget.get("qPhaseFE", "MISSING"))
        f.write("utilizationRate=%s\n" % util)
        f.write("formalOuterCycles=%s\n" % budget.get("formalOuterCycles", "MISSING"))
        f.write("formalQgRounds=%s\n" % budget.get("formalQgRounds", "MISSING"))
        f.write("terminationKind=%s\n" % budget.get("terminationKind", "MISSING"))
        f.write("stopReason=%s\n" % status.get("stopReason", "MISSING"))
        f.write("frontPoints=%d\n" % len(points))
        f.write("telemetry=OFF\n")
        f.write("f1VerdictIfFail=RUN_INVALID\n")

    print("gatesTotal=%d" % len(rows))
    print("gatesPassed=%d" % passed)
    print("gatesFailed=%d" % len(failed))
    for r in failed:
        print("FAILED %s %s expected=%s actual=%s" % (r["gateId"], r["item"], r["expected"], r["actual"]))
    print("RUN_ACCEPTANCE=%s" % verdict)
    return 0 if verdict == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
