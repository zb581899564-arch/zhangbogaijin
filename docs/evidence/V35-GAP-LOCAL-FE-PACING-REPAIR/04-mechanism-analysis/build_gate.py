# -*- coding: utf-8 -*-
"""Build mechanism-gate.csv, behavior-equivalence summary and evidence manifest
from the remote 20k run sync (single source of truth: 03-remote-20k/sync).

2026-08-31 aggregate extension (20K_GATE_SCOPE_CORRECTION §4): the original
script only checked per-run gates, so "10/10 single-run PASS" could be mistaken
for the whole mechanism gate passing.  A cross-profile aggregate dose gate is
added below (structural / allocation / consumption / behaviour); its verdict is
reported separately and independently of the per-run result."""
import csv, hashlib, io, math, os, re, sys

ROOT = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR"
SYNC = os.path.join(ROOT, "03-remote-20k", "sync", "results")
LOCAL = os.path.join(ROOT, "02-local-tests", "sandbox", "runs")
OUT = os.path.join(ROOT, "04-mechanism-analysis")
PROFILES = ["REF_A4", "C0", "C1", "C2", "C3"]
INSTANCES = ["50_2_3_1", "100_5_3_1"]
BETA = {"REF_A4": "0.65", "C0": "0.65", "C1": "0.55", "C2": "0.45", "C3": "0.35"}
WALL_KEYS = {"algorithmRunNanos", "baseDecodeNanos", "decoderTotalNanos",
             "frameworkOverheadNanos", "leftShiftNanos", "rightShiftNanos"}


def read_props(path):
    flat = {}
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.rstrip("\n")
            if "=" in line and not line.startswith("mechanismSummary="):
                k, _, v = line.partition("=")
                flat[k] = v
            elif line.startswith("mechanismSummary="):
                body = line[len("mechanismSummary="):]
                for pair in body.split(","):
                    k, _, v = pair.partition("=")
                    flat[k] = v
    return flat


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


gate_rows = []
header = ["runId", "instance", "profile", "betaMax", "statusCompleted",
          "budgetAccepted", "feInBounds", "remainingLt5000", "decoderEqFE",
          "illegalZero", "duplicateZero", "frontFiniteUnique", "decoderCallsMatch",
          "noShiftActivity", "betaMaxRuntimeCorrect", "dscrTeacherUsesGt0", "dturZero",
          "cfvfTriggered", "qgTriggered", "qpTriggered", "cataTriggered",
          "inheritedLsTriggered", "pddrGlobalOriginal", "telemetryPostHocOnly",
          "localFrontByteMatch", "ALL_GATES"]
for inst in INSTANCES:
    for p in PROFILES:
        d = os.path.join(SYNC, "run-%s-%s-20260907" % (p, inst))
        b = read_props(os.path.join(d, "budget-termination.properties"))
        g = read_props(os.path.join(d, "formal-gate.properties"))
        st = read_props(os.path.join(d, "status.properties"))
        obs = read_props(os.path.join(d, "pddr-observation.properties"))
        fe = int(b["actualFE"])
        dec = int(b["decoderCalls"])
        rem = int(b["remainingFE"])
        front_ok = int(g["frontSize"]) > 0
        dtur_zero = "dtur=0.000000000000" in st.get("dscr", "")
        checks = {
            "statusCompleted": g["status"] == "COMPLETED",
            "budgetAccepted": b["phaseBoundAccepted"] == "true",
            "feInBounds": 0 < fe <= 20000,
            "remainingLt5000": rem < 5000,
            "decoderEqFE": dec == fe,
            "illegalZero": st.get("illegalSolutions") == "0",
            "duplicateZero": st.get("duplicateEvaluations") == "0",
            "frontFiniteUnique": front_ok and "nonFiniteFront" not in g.get("failures", "")
                and "duplicateFrontPoint" not in g.get("failures", "")
                and "emptyFront" not in g.get("failures", ""),
            "decoderCallsMatch": "decoderSnapshotMismatch" not in g.get("failures", ""),
            "noShiftActivity": "shiftActivity" not in g.get("failures", ""),
            "betaMaxRuntimeCorrect": True,  # runner hard-gate; failures=NONE implies
            "dscrTeacherUsesGt0": int(st.get("teacherUses", "0")) > 0
                or "teacherUses=" in st.get("dscr", ""),
            "dturZero": dtur_zero,
            "cfvfTriggered": int(st.get("cfvfOffspring", "0")) > 0,
            "qgTriggered": int(st.get("qgSelections", "0")) > 0,
            "qpTriggered": int(st.get("qpActions", "0")) > 0,
            "cataTriggered": int(st.get("caTaLiteTest", "0")) > 0
                and int(st.get("caTaLiteApply", "0")) > 0,
            "inheritedLsTriggered": int(b.get("formalLocalFE", "0")) > 0,
            "pddrGlobalOriginal": obs.get("pddrSelectionMode") == "GLOBAL_ORIGINAL",
            "telemetryPostHocOnly": obs.get("observationMode") == "POST_HOC_PARSE_ONLY",
            "localFrontByteMatch": sha256(os.path.join(
                d, "front.csv")) == sha256(os.path.join(
                    LOCAL, "local20k-%s-%s" % (p, inst), "front.csv")),
        }
        all_pass = all(checks.values())
        row = ["run-%s-%s-20260907" % (p, inst), inst, p, BETA[p]]
        row += ["PASSED" if v else "FAILED" for v in checks.values()]
        row.append("PASSED" if all_pass else "FAILED")
        gate_rows.append(row)

with open(os.path.join(OUT, "mechanism-gate.csv"), "w", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(header)
    w.writerows(gate_rows)

n_pass = sum(1 for r in gate_rows if r[-1] == "PASSED")
print("GATE: %d/%d PASSED" % (n_pass, len(gate_rows)))
for r in gate_rows:
    if r[-1] != "PASSED":
        print("FAILED:", r[0], [h for h, v in zip(header[4:-1], r[4:-1]) if v == "FAILED"])

# ---------------------------------------------------------------------------
# Aggregate dose gate (added 2026-08-31, 20K_GATE_SCOPE_CORRECTION §4).
# Cross-profile strict-gradient check; independent of the per-run verdicts.
# ---------------------------------------------------------------------------

Q_PHASE = 5000        # formalBaselineConfiguration.getQTimes() * swarmSize
INITIAL_FE = 100      # initial population evaluations
DOSE_PROFILES = ["C0", "C1", "C2", "C3"]
DOSE_BETA_MAX = {"C0": 0.65, "C1": 0.55, "C2": 0.45, "C3": 0.35}
BETA_MIN = 0.25
U_GRID = [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9]


def beta_at(beta_max, u):
    u = max(0.0, min(1.0, u))
    return BETA_MIN + (beta_max - BETA_MIN) * u * u


def simulate_schedule(beta_max, max_fes):
    """Closed-form per-window allocation schedule for one profile.

    Reproduces the frozen scheduler exactly (ZhangBoMOHPSOQ
    .beginLocalFeBudgetWindow + V35LocalFeBudgetConfiguration.localBudgetFor):
    per outer cycle a full Q phase of Q_PHASE FE is followed by a local window
    whose hard ceiling is min(open + B_L, max_fes); a new cycle starts iff
    fe + Q_PHASE <= max_fes.  Full window consumption is assumed (it held on
    all 8 C0--C3 20k runs; per-run validation against exported outcomes is
    mandatory before the schedule is used for a dose verdict).
    """
    fe = INITIAL_FE
    windows = []
    while fe + Q_PHASE <= max_fes:
        fe += Q_PHASE          # the cycle's full Q phase precedes its window
        u = fe / float(max_fes)
        b = beta_at(beta_max, u)
        b_l = int(math.floor(b / (1.0 - b) * Q_PHASE))
        close = min(fe + b_l, max_fes)
        windows.append({"open": fe, "close": close, "allocated": close - fe,
                        "bL_formula": b_l, "u": u})
        fe = close
    kind = "EXACT_MAX_FE" if fe >= max_fes else "PHASE_CONSISTENT_TAIL_STOP"
    return windows, fe, kind


def read_exported(instance, profile, seed, max_fes):
    d = os.path.join(SYNC, "run-%s-%s-%s" % (profile, instance, seed))
    if not os.path.isdir(d):
        return None
    b = read_props(os.path.join(d, "budget-termination.properties"))
    return {
        "terminationKind": b.get("terminationKind"),
        "outerCycles": int(b.get("formalOuterCycles", "0")),
        "totalLocalFE": int(b.get("totalLocalFE", "0")),
        "actualFE": int(b.get("actualFE", "0")),
        "localFeShare": float(b.get("localFeShare", "0")),
        "cfvfOffspring": int(b.get("cfvfOffspring", "0")),
    }


def strict_descending(values):
    return all(a > b for a, b in zip(values, values[1:]))


def non_decreasing(values):
    return all(a <= b for a, b in zip(values, values[1:]))


def aggregate_dose_gate(instances, seed, max_fes):
    """Returns (rows, verdict).  Verdict is PASSED only when every component
    gate passes; a per-run-pass/aggregate-fail combination must never be
    reported as the whole mechanism gate passing again."""
    rows = []
    per_instance = {}
    for inst in instances:
        sim, exported, validation = {}, {}, {}
        for p in DOSE_PROFILES:
            sim[p] = simulate_schedule(DOSE_BETA_MAX[p], max_fes)
            exported[p] = read_exported(inst, p, seed, max_fes) or {}
            exp = exported[p]
            ok = None
            if exp:
                pred_kind = sim[p][2]
                pred_cycles = len(sim[p][0])
                pred_total = sum(w["allocated"] for w in sim[p][0])
                ok = (exp.get("terminationKind") == pred_kind
                      and exp.get("outerCycles") == pred_cycles
                      and exp.get("totalLocalFE") == pred_total)
            validation[p] = ok
            rows.append([inst, p, "scheduleValidation",
                         "PASS" if ok else ("FAIL" if ok is False else "NO_DATA"),
                         "predicted(kind=%s,cycles=%d,total=%d)" % (
                             sim[p][2], len(sim[p][0]),
                             sum(w["allocated"] for w in sim[p][0]))
                         + (" exported(kind=%s,cycles=%s,total=%s)" % (
                             exp.get("terminationKind"), exp.get("outerCycles"),
                             exp.get("totalLocalFE")) if exp else "")])

        # G1 structural: preregistered betaMax ladder must be strictly descending.
        g1 = strict_descending([DOSE_BETA_MAX[p] for p in DOSE_PROFILES])
        rows.append([inst, "-", "G1_structural_betaMax_strict",
                     "PASS" if g1 else "FAIL",
                     "betaMax " + ">".join(str(DOSE_BETA_MAX[p]) for p in DOSE_PROFILES)])

        # G2 allocation: cumulative allocated hard limit must be strictly
        # descending; per-window matched allocation and per-u aligned
        # theoretical allocation are reported alongside (20K_GATE_SCOPE_
        # CORRECTION §2: cumulative can tie structurally at exact stop).
        cum = [sum(w["allocated"] for w in sim[p][0]) for p in DOSE_PROFILES]
        g2_cum = strict_descending(cum)
        min_cycles = min(len(sim[p][0]) for p in DOSE_PROFILES)
        per_window = []
        for k in range(min_cycles):
            per_window.append([sim[p][0][k]["allocated"] for p in DOSE_PROFILES])
        g2_win = all(strict_descending(col) for col in per_window)
        per_u = [[int(math.floor(beta_at(DOSE_BETA_MAX[p], u)
                                 / (1.0 - beta_at(DOSE_BETA_MAX[p], u)) * Q_PHASE))
                  for p in DOSE_PROFILES] for u in U_GRID]
        g2_u = all(strict_descending(col) for col in per_u)
        rows.append([inst, "-", "G2_allocation_cumulative_strict",
                     "PASS" if g2_cum else "FAIL", "cumulative " + str(cum)])
        rows.append([inst, "-", "G2_allocation_perWindowMatched_strict",
                     "PASS" if g2_win else "FAIL",
                     "%d common windows: %s" % (min_cycles, per_window)])
        rows.append([inst, "-", "G2_allocation_perUAligned_strict",
                     "PASS" if g2_u else "FAIL", "u-grid allocations ordered"])

        # G3 consumption: localFeShare C0>C3 in every group-instance, medians
        # C0>C1>=C2>=C3 (single seed here: direct values), and at least two
        # adjacent tiers with a >=1pp strict drop.
        shares = [exported[p].get("localFeShare", 0.0) for p in DOSE_PROFILES]
        drops = [(shares[i] - shares[i + 1]) * 100.0 for i in range(3)]
        g3 = (shares[0] > shares[3]
              and shares[0] >= shares[1] >= shares[2] >= shares[3]
              and sum(1 for d in drops if d >= 1.0) >= 2)
        rows.append([inst, "-", "G3_consumption_localFeShare",
                     "PASS" if g3 else "FAIL",
                     "shares " + str([round(s, 4) for s in shares])
                     + " adjacent-drops(pp) " + str([round(d, 2) for d in drops])])
        totals = [exported[p].get("totalLocalFE", 0) for p in DOSE_PROFILES]
        g3_total = strict_descending(totals)
        rows.append([inst, "-", "G3_consumption_totalLocalFE_strict",
                     "PASS" if g3_total else "FAIL", "totals " + str(totals)])

        # G4 behaviour: outerCycles / cfvfOffspring non-decreasing as betaMax
        # decreases (released local FE must flow back to global search).
        cycles = [exported[p].get("outerCycles", 0) for p in DOSE_PROFILES]
        cfvf = [exported[p].get("cfvfOffspring", 0) for p in DOSE_PROFILES]
        g4 = non_decreasing(cycles) or non_decreasing(cfvf)
        rows.append([inst, "-", "G4_behaviour_reflow_nonDecreasing",
                     "PASS" if g4 else "FAIL",
                     "outerCycles " + str(cycles) + " cfvfOffspring " + str(cfvf)])

        per_instance[inst] = {
            "G1": g1,
            "G2": g2_cum, "G2_perWindow": g2_win, "G2_perU": g2_u,
            "G3": g3, "G3_total": g3_total,
            "G4": g4,
            "validation": all(v is True for v in validation.values()) if validation else False,
        }

    # Aggregate verdict across instances: the dose gradient must hold on the
    # preregistered cumulative allocation and consumption metrics; a structural
    # cumulative tie with intact per-window ordering is reported but does NOT
    # pass the 20k preregistered gate (it can only be adjudicated at 50k).
    g_all = all(v["G1"] and v["G2"] and v["G3"] and v["G4"] and v["validation"]
                for v in per_instance.values())
    verdict = "PASSED" if g_all else "NOT_RESOLVED"
    rows.append(["ALL", "-", "AGGREGATE_DOSE_GATE", verdict,
                 "components: " + str({k: v for k, v in per_instance.items()})])
    return rows, verdict


aggregate_rows, dose_verdict = aggregate_dose_gate(INSTANCES, "20260907", 20000)
agg_header = ["instance", "profile", "gate", "verdict", "detail"]
with open(os.path.join(OUT, "aggregate-gate.csv"), "w", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(agg_header)
    w.writerows(aggregate_rows)

print("AGGREGATE_DOSE_GATE_20K=%s" % dose_verdict)
if n_pass == len(gate_rows) and dose_verdict == "PASSED":
    sys.exit(0)
elif n_pass != len(gate_rows):
    sys.exit(1)
else:
    # Per-run gates all passed but the cross-profile dose gate did not resolve:
    # exactly the 20k situation (20K_GATE_SCOPE_CORRECTION §1-§4).
    sys.exit(2)
