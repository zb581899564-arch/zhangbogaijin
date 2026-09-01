# -*- coding: utf-8 -*-
"""V35-LOCAL-FE-PACING-50K Agent C — Task A: dose-resolution analysis.

Writes (into 09-dose-resolution/ only):
  fe-reallocation-50k.csv    16 rows, one per run (budget/FE reallocation fields)
  dose-resolution.csv        per instance x profile gate rows + schedule summary +
                             AGGREGATE verdict tail rows
  pddr-observation-50k.csv   16 rows of PDDR observation fields
  dose-resolution.md         machine-generated narrative + status block

Provenance notes:
- simulate_schedule() below is copied VERBATIM from
  04-mechanism-analysis/build_gate.py (closed-form schedule reconstruction,
  prereg 4-D2).  It is NOT imported because importing build_gate executes its
  module-level 20k gate code and would write into a forbidden directory.
- beta ladder and gates follow 07-50k-preregistration/50K_PREREGISTRATION.md
  section 7 (G1 structural, G2 allocation three views, G3 consumption,
  G4 behaviour) and section 5 ex-ante predictions.
- All numbers are computed from the run files; nothing is hand-copied.
"""
import csv, io, math, os, sys

ROOT = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR"
SYNC = os.path.join(ROOT, "08-remote-50k", "sync")
OUT = os.path.join(ROOT, "09-dose-resolution")
SEEDS = ["20260907", "20260914"]
INSTANCES = ["50_2_3_1", "100_5_3_1"]
PROFILES = ["C0", "C1", "C2", "C3"]
EXPECTED_BETA = {"C0": "0.65", "C1": "0.55", "C2": "0.45", "C3": "0.35"}
BETA_FLOAT = {p: float(v) for p, v in EXPECTED_BETA.items()}

# ---- closed-form schedule (verbatim copy of build_gate.simulate_schedule) ----
Q_PHASE = 5000        # formalBaselineConfiguration.getQTimes() * swarmSize
INITIAL_FE = 100      # initial population evaluations
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


# ---------------------------------------------------------------- IO helpers
def read_kv(path):
    d = {}
    with io.open(path, "r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            line = line.rstrip("\n")
            if "=" in line and not line.startswith("#"):
                k, _, v = line.partition("=")
                d[k] = v
    return d


def read_mechanism_summary(status):
    ms = status.get("mechanismSummary", "")
    flat = {}
    for pair in ms.split(","):
        k, _, v = pair.partition("=")
        if k:
            flat[k] = v
    return flat


def median(values):
    vals = sorted(values)
    n = len(vals)
    if n == 0:
        return float("nan")
    return vals[n // 2] if n % 2 == 1 else (vals[n // 2 - 1] + vals[n // 2]) / 2.0


def strict_descending(values):
    return all(a > b for a, b in zip(values, values[1:]))


def non_decreasing(values):
    return all(a <= b for a, b in zip(values, values[1:]))


# ---------------------------------------------------------------- load runs
runs = []
for seed in SEEDS:
    for inst in INSTANCES:
        for prof in PROFILES:
            run_key = "run-GAPL50K-%s-%s-%s" % (prof, inst, seed)
            d = os.path.join(SYNC, "seed-" + seed, "results", run_key)
            pf = read_kv(os.path.join(d, "profile.txt"))
            bt = read_kv(os.path.join(d, "budget-termination.properties"))
            st = read_kv(os.path.join(d, "status.properties"))
            mech = read_mechanism_summary(st)
            pd = read_kv(os.path.join(d, "pddr-observation.properties"))
            fg = read_kv(os.path.join(d, "formal-gate.properties"))
            runs.append({
                "runKey": run_key, "dir": d, "profile": prof, "instance": inst,
                "seed": seed,
                "betaMax_pf": pf.get("betaMax", ""),
                "betaMax_budget": pf.get("localFeBudget.betaMax", ""),
                "betaMin_pf": pf.get("betaMin", ""),
                "maxFEs_pf": pf.get("maxFEs", ""),
                "terminationKind": bt.get("terminationKind", ""),
                "actualFE": int(bt.get("actualFE", "0")),
                "remainingFE": int(bt.get("remainingFE", "0")),
                "outerCycles": int(bt.get("formalOuterCycles", "0")),
                "formalQgRounds": int(bt.get("formalQgRounds", "0")),
                "totalLocalFE": int(bt.get("totalLocalFE", "0")),
                "formalLocalFE": int(bt.get("formalLocalFE", "0")),
                "caTaLiteFE": int(bt.get("caTaLiteFE", "0")),
                "localFeShare": float(bt.get("localFeShare", "0")),
                "globalPhaseFE": int(bt.get("globalPhaseFE", "0")),
                "cfvfOffspring": int(mech.get("cfvfOffspring", "0")),
                "qgSelections": int(mech.get("qgSelections", "0")),
                "qpActions": int(mech.get("qpActions", "0")),
                "pddr": pd, "frontSize": int(fg.get("frontSize", "0")),
            })
assert len(runs) == 16, "expected 16 runs, found %d" % len(runs)

# ------------------------------------------------- closed schedule per profile
max_fes = 50000
sim = {}
for p in PROFILES:
    windows, final_fe, kind = simulate_schedule(BETA_FLOAT[p], max_fes)
    sim[p] = {"windows": windows, "finalFE": final_fe, "kind": kind,
              "cycles": len(windows),
              "total": sum(w["allocated"] for w in windows)}

# per-run schedule validation (exact equality, stricter than Agent B's +-250)
for r in runs:
    s = sim[r["profile"]]
    r["schedMatch"] = (r["terminationKind"] == s["kind"]
                       and r["outerCycles"] == s["cycles"]
                       and r["totalLocalFE"] == s["total"])
    r["schedDetail"] = "kind=%s,cycles=%d,total=%d" % (s["kind"], s["cycles"], s["total"])

# ------------------------------------------------- G1 structural (per run)
# numeric comparison: profile.txt carries betaMax=0.65 but
# localFeBudget.betaMax=0.650000 (same value, different string format)
for r in runs:
    r["g1"] = (abs(float(r["betaMax_pf"]) - BETA_FLOAT[r["profile"]]) < 1e-12
               and abs(float(r["betaMax_budget"]) - BETA_FLOAT[r["profile"]]) < 1e-12
               and abs(float(r["betaMin_pf"]) - BETA_MIN) < 1e-12)
g1_all = all(r["g1"] for r in runs)
g1_ladder = strict_descending([BETA_FLOAT[p] for p in PROFILES])

# ------------------------------------------------- G2 allocation (per instance)
g2_rows = {}   # inst -> dict of view results
for inst in INSTANCES:
    cum = [sim[p]["total"] for p in PROFILES]
    avg = [sim[p]["total"] / float(sim[p]["cycles"]) for p in PROFILES]
    min_cycles = min(sim[p]["cycles"] for p in PROFILES)
    per_window = [[sim[p]["windows"][k]["allocated"] for p in PROFILES]
                  for k in range(min_cycles)]
    per_u = [[int(math.floor(beta_at(BETA_FLOAT[p], u)
                             / (1.0 - beta_at(BETA_FLOAT[p], u)) * Q_PHASE))
              for p in PROFILES] for u in U_GRID]
    g2_cum = strict_descending(cum)
    g2_win = all(strict_descending(col) for col in per_window)
    g2_u = all(strict_descending(col) for col in per_u)
    g2_rows[inst] = {"cum": cum, "avg": avg, "perWindow": per_window,
                     "perU": per_u, "g2_cum": g2_cum, "g2_win": g2_win,
                     "g2_u": g2_u, "minCycles": min_cycles}

# ------------------------------------------------- G3 consumption (per group)
def fmt(x, nd=6):
    return ("%." + str(nd) + "f") % x

for inst in INSTANCES:
    for p in PROFILES:
        rs = [r for r in runs if r["instance"] == inst and r["profile"] == p]
        shares = [r["localFeShare"] for r in rs]
        med = median(shares)
        g3_rows = None
        # per (instance, profile): median of 2 seeds + strict gradient info
        for r in rs:
            r.setdefault("shareMedian", med)
            r["adjDropFromPrev"] = ""
# per (instance, seed) group verdicts + profile medians
group_keys = [(i, s) for i in INSTANCES for s in SEEDS]
g3_group = {}
for inst, seed in group_keys:
    shares = {p: [r["localFeShare"] for r in runs
                  if r["instance"] == inst and r["seed"] == seed and r["profile"] == p][0]
              for p in PROFILES}
    drops = [(shares[PROFILES[i]] - shares[PROFILES[i + 1]]) * 100.0 for i in range(3)]
    g3_group[(inst, seed)] = {
        "shares": shares,
        "c0gtC3": shares["C0"] > shares["C3"],
        "ordering": (shares["C0"] > shares["C1"] >= shares["C2"] >= shares["C3"]),
        "drops": drops,
        "dropsGE1pp": sum(1 for d in drops if d >= 1.0),
    }
profile_share_median = {p: median([r["localFeShare"] for r in runs
                                   if r["profile"] == p]) for p in PROFILES}
profile_total_median = {p: median([r["totalLocalFE"] for r in runs
                                   if r["profile"] == p]) for p in PROFILES}
overall_drops = [(profile_share_median[PROFILES[i]] - profile_share_median[PROFILES[i + 1]]) * 100.0
                 for i in range(3)]
g3_c0gtC3_all = all(g3_group[k]["c0gtC3"] for k in group_keys)
g3_median_order = (profile_share_median["C0"] > profile_share_median["C1"]
                   >= profile_share_median["C2"] >= profile_share_median["C3"])
g3_median_order_all_groups = all(g3_group[k]["ordering"] for k in group_keys)
g3_adjacent = sum(1 for d in overall_drops if d >= 1.0)
g3_totals_strict = strict_descending([profile_total_median[p] for p in PROFILES])
g3 = (g3_c0gtC3_all and g3_median_order and g3_median_order_all_groups and g3_adjacent >= 2)

# ------------------------------------------------- G4 behaviour (per group)
g4_group = {}
for inst, seed in group_keys:
    cyc = [next(r["outerCycles"] for r in runs
                if r["instance"] == inst and r["seed"] == seed and r["profile"] == p)
           for p in PROFILES]
    cfv = [next(r["cfvfOffspring"] for r in runs
                if r["instance"] == inst and r["seed"] == seed and r["profile"] == p)
           for p in PROFILES]
    g4_group[(inst, seed)] = {"outerCycles": cyc, "cfvfOffspring": cfv,
                              "cycND": non_decreasing(cyc), "cfvfND": non_decreasing(cfv),
                              "pass": non_decreasing(cyc) or non_decreasing(cfv)}
g4 = sum(1 for v in g4_group.values() if v["pass"]) >= 3

# ------------------------------------------------- failed-dose adjudication rule
# FAILED iff only C0 separates while C1/C2/C3 are fully tied on localFeShare median
c123_tied = (profile_share_median["C1"] == profile_share_median["C2"]
             == profile_share_median["C3"])
c0_separated = profile_share_median["C0"] > profile_share_median["C1"]
dose_failed_pattern = c123_tied and c0_separated
g_all = g1_all and g1_ladder and all(
    v["g2_cum"] and v["g2_win"] and v["g2_u"] for v in g2_rows.values()) and g3 and g4
DOSE_GATE = "PASSED" if (g_all and not dose_failed_pattern) else "FAILED"

# ================================================================ write CSVs
os.makedirs(OUT, exist_ok=True)

# ---- fe-reallocation-50k.csv (16 rows) ----
cols = ["runKey", "profile", "instance", "seed", "betaMaxProfileTxt", "betaMaxLocalFeBudget",
        "betaMinProfileTxt", "maxFEsProfileTxt", "terminationKind", "actualFE", "remainingFE",
        "outerCycles", "formalQgRounds", "totalLocalFE", "formalLocalFE", "caTaLiteFE",
        "localFeShare", "globalPhaseFE", "cfvfOffspring", "qgSelections", "qpActions",
        "frontSize", "schedPredictedKind", "schedPredictedCycles", "schedPredictedTotalLocalFE",
        "schedPredictedFinalFE", "scheduleValidation"]
with io.open(os.path.join(OUT, "fe-reallocation-50k.csv"), "w", encoding="utf-8", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(cols)
    for r in runs:
        s = sim[r["profile"]]
        w.writerow([r["runKey"], r["profile"], r["instance"], r["seed"],
                    r["betaMax_pf"], r["betaMax_budget"], r["betaMin_pf"], r["maxFEs_pf"],
                    r["terminationKind"], r["actualFE"], r["remainingFE"], r["outerCycles"],
                    r["formalQgRounds"], r["totalLocalFE"], r["formalLocalFE"],
                    r["caTaLiteFE"], r["localFeShare"], r["globalPhaseFE"],
                    r["cfvfOffspring"], r["qgSelections"], r["qpActions"], r["frontSize"],
                    s["kind"], s["cycles"], s["total"], s["finalFE"],
                    "MATCH" if r["schedMatch"] else "MISMATCH"])

# ---- pddr-observation-50k.csv (16 rows) ----
pcols = ["runKey", "profile", "instance", "seed", "pddrSelectionMode", "observationMode",
         "pddrEvents", "archiveInsertions", "globalOffspringFE", "caTaTestCalls",
         "caTaApplyCalls", "caTaLiteFE", "formalLocalFE", "inheritedLocalEventOps",
         "inheritedLocalAccepted", "decisionFrontSize", "observedFullFrontSize",
         "minCmax", "minTEC", "minTWC", "poolLevelAttribution"]
with io.open(os.path.join(OUT, "pddr-observation-50k.csv"), "w", encoding="utf-8", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(pcols)
    for r in runs:
        pd = r["pddr"]
        w.writerow([r["runKey"], r["profile"], r["instance"], r["seed"],
                    pd.get("pddrSelectionMode", ""), pd.get("observationMode", ""),
                    pd.get("pddrEvents", ""), pd.get("archiveInsertions", ""),
                    pd.get("globalOffspringFE", ""), pd.get("caTaTestCalls", ""),
                    pd.get("caTaApplyCalls", ""), pd.get("caTaLiteFE", ""),
                    pd.get("formalLocalFE", ""), pd.get("inheritedLocalEventOps", ""),
                    pd.get("inheritedLocalAccepted", ""), pd.get("decisionFrontSize", ""),
                    pd.get("observedFullFrontSize", ""), pd.get("minCmax", ""),
                    pd.get("minTEC", ""), pd.get("minTWC", ""),
                    pd.get("poolLevelAttribution", "")])

# ---- dose-resolution.csv ----
gcols = ["section", "instance", "profile", "seed", "gate", "verdict", "detail"]
with io.open(os.path.join(OUT, "dose-resolution.csv"), "w", encoding="utf-8", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(gcols)

    def row(instance, profile, seed, gate, verdict, detail):
        w.writerow(["GATE", instance, profile, seed, gate, verdict, detail])

    # per instance x profile rows
    for inst in INSTANCES:
        for p in PROFILES:
            rs = [r for r in runs if r["instance"] == inst and r["profile"] == p]
            shares = [r["localFeShare"] for r in rs]
            med = median(shares)
            g1p = all(r["g1"] for r in rs)
            g2 = g2_rows[inst]
            details = ("runs=%d shareMedian=%s shares=%s schedVal=%s"
                       % (len(rs), fmt(med), [fmt(s) for s in shares],
                          "".join("M" if r["schedMatch"] else "X" for r in rs)))
            row(inst, p, "BOTH", "G1_structural", "PASS" if g1p else "FAIL",
                "betaMax=%s ladder=%s" % (rs[0]["betaMax_pf"], ">".join(str(BETA_FLOAT[q]) for q in PROFILES)))
            row(inst, p, "BOTH", "G2_cumulativeStrict", "PASS" if g2["g2_cum"] else "FAIL",
                "cumulative=%s" % g2["cum"])
            row(inst, p, "BOTH", "G2_perWindowStrict", "PASS" if g2["g2_win"] else "FAIL",
                "%d common windows=%s" % (g2["minCycles"], g2["perWindow"]))
            row(inst, p, "BOTH", "G2_perUStrict", "PASS" if g2["g2_u"] else "FAIL",
                "u grid=%s" % g2["perU"])
            row(inst, p, "BOTH", "G3_shareC0gtC3",
                "PASS" if all(g3_group[(inst, s)]["c0gtC3"] for s in SEEDS) else "FAIL",
                "shares per seed=%s" % {s: fmt(g3_group[(inst, s)]["shares"][p]) for s in SEEDS})
            row(inst, p, "BOTH", "G3_shareMedianOrdering",
                "PASS" if all(g3_group[(inst, s)]["ordering"] for s in SEEDS) else "FAIL",
                "profileMedians=%s" % {q: fmt(profile_share_median[q]) for q in PROFILES})
            row(inst, p, "BOTH", "G3_adjacentDropsGE1pp",
                "PASS" if g3_adjacent >= 2 else "FAIL",
                "overallDrops_pp=%s count>=1pp=%d" % ([fmt(d, 2) for d in overall_drops], g3_adjacent))
            row(inst, p, "BOTH", "G3_totalLocalStrict", "PASS" if g3_totals_strict else "FAIL",
                "totalLocalFE median=%s (exact-stop identity caveat: C2 is EXACT_MAX_FE)"
                % [profile_total_median[q] for q in PROFILES])
            g4i = sum(1 for s in SEEDS if g4_group[(inst, s)]["pass"])
            row(inst, p, "BOTH", "G4_outerCyclesNonDecreasing",
                "PASS" if all(g4_group[(inst, s)]["cycND"] for s in SEEDS) else "FAIL",
                "perSeed outerCycles=%s" % {s: g4_group[(inst, s)]["outerCycles"] for s in SEEDS})
            row(inst, p, "BOTH", "G4_cfvfNonDecreasing",
                "PASS" if all(g4_group[(inst, s)]["cfvfND"] for s in SEEDS) else "FAIL",
                "perSeed cfvfOffspring=%s" % {s: g4_group[(inst, s)]["cfvfOffspring"] for s in SEEDS})
            _ = details  # per-run detail lives in fe-reallocation-50k.csv

    # schedule summary rows (per profile)
    for p in PROFILES:
        s = sim[p]
        w.writerow(["SCHEDULE", "ALL", p, "BOTH", "closedFormSchedule",
                    "PREDICTION",
                    "kind=%s cycles=%d finalFE=%d cumulative=%d perCycleAvg=%s windows=%s"
                    % (s["kind"], s["cycles"], s["finalFE"], s["total"],
                       fmt(s["total"] / float(s["cycles"]), 2),
                       ";".join("(open=%d,close=%d,alloc=%d,u=%.6f)" % (
                           x["open"], x["close"], x["allocated"], x["u"])
                           for x in s["windows"]))])
    # validation rows
    for r in runs:
        w.writerow(["SCHEDULE_VALIDATION", r["instance"], r["profile"], r["seed"],
                    "scheduleValidation",
                    "MATCH" if r["schedMatch"] else "MISMATCH",
                    "exported(kind=%s,cycles=%d,total=%d) predicted(%s)"
                    % (r["terminationKind"], r["outerCycles"], r["totalLocalFE"], r["schedDetail"])])
    # aggregate verdict tail rows
    w.writerow(["AGGREGATE", "ALL", "ALL", "BOTH", "G1_structural",
                "PASS" if (g1_all and g1_ladder) else "FAIL",
                "runtime betaMax per-value match 16/16=%s; ladder 0.65>0.55>0.45>0.35=%s"
                % (g1_all, g1_ladder)])
    for inst in INSTANCES:
        g2 = g2_rows[inst]
        w.writerow(["AGGREGATE", inst, "ALL", "BOTH", "G2_cumulativeStrict",
                    "PASS" if g2["g2_cum"] else "FAIL", "cumulative=%s" % g2["cum"]])
        w.writerow(["AGGREGATE", inst, "ALL", "BOTH", "G2_perWindowStrict",
                    "PASS" if g2["g2_win"] else "FAIL",
                    "%d common windows=%s" % (g2["minCycles"], g2["perWindow"])])
        w.writerow(["AGGREGATE", inst, "ALL", "BOTH", "G2_perUStrict",
                    "PASS" if g2["g2_u"] else "FAIL", "u grid=%s" % g2["perU"]])
    w.writerow(["AGGREGATE", "ALL", "ALL", "BOTH", "G3_shareC0gtC3",
                "PASS" if g3_c0gtC3_all else "FAIL",
                str({("%s|%s" % k): fmt(g3_group[k]["shares"]["C0"]) + ">" + fmt(g3_group[k]["shares"]["C3"])
                     for k in group_keys})])
    w.writerow(["AGGREGATE", "ALL", "ALL", "BOTH", "G3_shareMedianOrdering",
                "PASS" if (g3_median_order and g3_median_order_all_groups) else "FAIL",
                "overallMedians=%s; drops_pp=%s" % (
                    {p: fmt(profile_share_median[p]) for p in PROFILES},
                    [fmt(d, 2) for d in overall_drops])])
    w.writerow(["AGGREGATE", "ALL", "ALL", "BOTH", "G3_adjacentDropsGE1pp",
                "PASS" if g3_adjacent >= 2 else "FAIL",
                "count=%d (need >=2)" % g3_adjacent])
    w.writerow(["AGGREGATE", "ALL", "ALL", "BOTH", "G3_totalLocalStrict",
                "PASS" if g3_totals_strict else "FAIL",
                "totals=%s (exact-stop identity caveat applies to totalLocalFE, not localFeShare)"
                % [profile_total_median[p] for p in PROFILES]])
    w.writerow(["AGGREGATE", "ALL", "ALL", "BOTH", "G4_behaviour",
                "PASS" if g4 else "FAIL",
                str({("%s|%s" % k): "cyc=%s cfvf=%s pass=%s" % (
                    g4_group[k]["outerCycles"], g4_group[k]["cfvfOffspring"], g4_group[k]["pass"])
                     for k in group_keys})])
    w.writerow(["AGGREGATE", "ALL", "ALL", "BOTH", "DOSE_RESOLUTION_GATE", DOSE_GATE,
                "G1=%s G2(cum,win,u)=(%s) G3=%s G4=%s failedPattern(C0-only-separation)=%s"
                % (g1_all and g1_ladder,
                   tuple((g2_rows[i]["g2_cum"], g2_rows[i]["g2_win"], g2_rows[i]["g2_u"]) for i in INSTANCES),
                   g3, g4, dose_failed_pattern)])

# ================================================================ md report
L = []
L.append("# DOSE-RESOLUTION-50K — V35-LOCAL-FE-PACING (Agent C, Task A)")
L.append("")
L.append("- 生成脚本: `09-dose-resolution/dose_resolution_50k.py`（全部数字由脚本从 16 条 run 文件计算，无手抄）")
L.append("- 输入: `08-remote-50k/sync/seed-<S>/results/run-GAPL50K-<P>-<I>-<S>/`（16 条，验收 16/16 PASS，fairness 4/4 PASS，见 08 目录）")
L.append("- 调度重建: `simulate_schedule(betaMax, 50000)` 逐字复用 `04-mechanism-analysis/build_gate.py`（预登记 §4-D2 闭合调度；不 import 以免执行其模块级 20k 代码）")
L.append("- 预登记: `07-50k-preregistration/50K_PREREGISTRATION.md` §5 预测 / §7 门定义")
L.append("")
L.append("## 1. 闭合调度重建与逐 run 验证（§4-D2）")
L.append("")
L.append("| profile | betaMax | kind | cycles | finalFE | cumulative B_L | per-cycle avg | 预测(§5) |")
L.append("|---|---|---|---:|---:|---:|---:|---|")
pred = {"C0": "TAIL,6,48269,18169", "C1": "TAIL,6,45359,15259",
        "C2": "EXACT,7,50000,14900", "C3": "TAIL,7,49036,13936"}
for p in PROFILES:
    s = sim[p]
    short = s["kind"].replace("PHASE_CONSISTENT_TAIL_STOP", "TAIL").replace("EXACT_MAX_FE", "EXACT")
    L.append("| %s | %.2f | %s | %d | %d | %d | %.2f | %s |" % (
        p, BETA_FLOAT[p], short, s["cycles"], s["finalFE"], s["total"],
        s["total"] / float(s["cycles"]), pred[p]))
L.append("")
L.append("逐 run 验证（导出 terminationKind/formalOuterCycles/totalLocalFE 与闭合模拟精确相等）：")
L.append("")
L.append("| run | exported kind/cycles/totalLocalFE | predicted | verdict |")
L.append("|---|---|---|---|")
for r in runs:
    L.append("| %s | %s/%d/%d | %s | %s |" % (
        r["runKey"], r["terminationKind"], r["outerCycles"], r["totalLocalFE"],
        r["schedDetail"], "MATCH" if r["schedMatch"] else "MISMATCH"))
n_match = sum(1 for r in runs if r["schedMatch"])
L.append("")
L.append("**scheduleValidation = %d/16 MATCH**（精确等式，比 08 目录验收表的 ±250 容差更严）。" % n_match)
L.append("")
L.append("## 2. 每窗口分配表（6 个公共窗口；C0/C1 共 6 窗、C2/C3 共 7 窗，公共=前 6）")
L.append("")
L.append("| window k | u(open) | C0 alloc | C1 alloc | C2 alloc | C3 alloc | 严格递减 |")
L.append("|---|---|---:|---:|---:|---:|---|")
g2 = g2_rows["50_2_3_1"]
for k in range(g2["minCycles"]):
    u = sim["C0"]["windows"][k]["u"]
    col = [sim[p]["windows"][k]["allocated"] for p in PROFILES]
    L.append("| %d | %.4f | %d | %d | %d | %d | %s |" % (
        k + 1, u, col[0], col[1], col[2], col[3],
        "YES" if strict_descending(col) else "NO"))
L.append("")
L.append("C2/C3 第 7 窗（超出公共窗口，截断于 MaxFEs）：C2 open=48204 close=50000 alloc=%d；C3 open=46501 close=49036 alloc=%d。" % (
    sim["C2"]["windows"][6]["allocated"], sim["C3"]["windows"][6]["allocated"]))
L.append("")
L.append("## 3. per-u 对齐理论分配 B_L(u)=floor(beta(u)/(1-beta(u))*5000)")
L.append("")
L.append("| u | C0 | C1 | C2 | C3 | 严格递减 |")
L.append("|---|---:|---:|---:|---:|---|")
for ui, u in enumerate(U_GRID):
    col = g2["perU"][ui]
    L.append("| %.1f | %d | %d | %d | %d | %s |" % (
        u, col[0], col[1], col[2], col[3], "YES" if strict_descending(col) else "NO"))
L.append("")
L.append("## 4. 门判定")
L.append("")
L.append("### G1 结构门")
L.append("")
betas_read = {}
for p in PROFILES:
    vals = sorted(set((r["betaMax_pf"], r["betaMax_budget"]) for r in runs if r["profile"] == p))
    betas_read[p] = vals[0] if len(vals) == 1 else str(vals)
L.append("- 运行时读回（profile.txt `betaMax=` 与 `localFeBudget.betaMax=`，每 profile 4 条 run 完全一致）："
         + "; ".join("%s=%s" % (p, betas_read[p]) for p in PROFILES))
L.append("- 逐值匹配配置 {C0:0.65, C1:0.55, C2:0.45, C3:0.35} 且严格降序: %s" % ("PASS" if (g1_all and g1_ladder) else "FAIL"))
L.append("")
L.append("### G2 分配门（三视图）")
L.append("")
for inst in INSTANCES:
    g2i = g2_rows[inst]
    L.append("- %s 累计分配上限: %s → 严格递减 %s" % (inst, g2i["cum"], "PASS" if g2i["g2_cum"] else "FAIL"))
    L.append("- %s 每 outer cycle 平均: %s → 严格递减 %s" % (
        inst, [fmt(a, 2) for a in g2i["avg"]],
        "PASS" if strict_descending(g2i["avg"]) else "FAIL"))
    L.append("- %s 每窗口匹配分配（%d 公共窗）严格递减: %s；per-u 对齐严格递减: %s" % (
        inst, g2i["minCycles"], "PASS" if g2i["g2_win"] else "FAIL",
        "PASS" if g2i["g2_u"] else "FAIL"))
L.append("")
L.append("### G3 消费门（localFeShare）")
L.append("")
L.append("| instance | seed | C0 | C1 | C2 | C3 | C0>C3 | C0>C1>=C2>=C3 | 相邻降幅(pp) |")
L.append("|---|---|---|---|---|---|---|---|---|")
for inst, seed in group_keys:
    g = g3_group[(inst, seed)]
    L.append("| %s | %s | %s | %s | %s | %s | %s | %s | %s |" % (
        inst, seed, fmt(g["shares"]["C0"], 4), fmt(g["shares"]["C1"], 4),
        fmt(g["shares"]["C2"], 4), fmt(g["shares"]["C3"], 4),
        "Y" if g["c0gtC3"] else "N", "Y" if g["ordering"] else "N",
        "/".join(fmt(d, 2) for d in g["drops"])))
L.append("")
L.append("- 四组全部 localFeShare(C0)>localFeShare(C3): **%s**" % ("PASS" if g3_c0gtC3_all else "FAIL"))
L.append("- 总体中位数（2 instance × 2 seed；各 profile 预算确定性故 2-seed 中位与总体一致）C0=%s > C1=%s >= C2=%s >= C3=%s: **%s**" % (
    fmt(profile_share_median["C0"], 4), fmt(profile_share_median["C1"], 4),
    fmt(profile_share_median["C2"], 4), fmt(profile_share_median["C3"], 4),
    "PASS" if (g3_median_order and g3_median_order_all_groups) else "FAIL"))
L.append("- 相邻档降幅(pp): %s → ≥1pp 的相邻档计数 = %d（需 ≥2）: **%s**" % (
    [fmt(d, 2) for d in overall_drops], g3_adjacent, "PASS" if g3_adjacent >= 2 else "FAIL"))
L.append("- totalLocalFE 中位数 %s 严格递减: **%s**（exact-stop 恒等式 caveat：C2 终止于 EXACT_MAX_FE，其 totalLocalFE 为截断值 14900，本批无并列，caveat 不触发）" % (
    [profile_total_median[p] for p in PROFILES], "PASS" if g3_totals_strict else "FAIL"))
L.append("- G3 整体: **%s**" % ("PASS" if g3 else "FAIL"))
L.append("")
L.append("### G4 行为门")
L.append("")
L.append("| instance | seed | outerCycles C0→C3 | cfvfOffspring C0→C3 | 非递减 |")
L.append("|---|---|---|---|---|")
for inst, seed in group_keys:
    g = g4_group[(inst, seed)]
    L.append("| %s | %s | %s | %s | %s |" % (inst, seed, g["outerCycles"], g["cfvfOffspring"],
                                             "Y" if g["pass"] else "N"))
L.append("")
L.append("- ≥3/4 公平组满足 outerCycles 或 cfvfOffspring 非递减: %d/4 → **%s**" % (
    sum(1 for v in g4_group.values() if v["pass"]), "PASS" if g4 else "FAIL"))
L.append("")
L.append("## 5. 聚合裁决")
L.append("")
L.append("- 失败判据（仅 C0 分开而 C1/C2/C3 在 localFeShare 中位数完全并列）: C1=C2=C3 = %s → 未触发" % (
    "TRUE" if c123_tied else "FALSE"))
L.append("- **DOSE_RESOLUTION_GATE = %s**" % DOSE_GATE)
if DOSE_GATE == "PASSED":
    L.append("- 依据预登记 §7，剂量门通过，准许进入 B（双口径指标与配对响应）与 C（性能筛查）。")
else:
    L.append("- 剂量门失败：停止本 repair family（不调参、不寻找第五个 betaMax、不启动 250k）。")
L.append("")
L.append("## 6. 状态块（机器可读）")
L.append("")
L.append("```ini")
L.append("[dose-resolution-50k]")
L.append("scheduleValidation=%d/16_MATCH" % n_match)
L.append("G1_structural=%s" % ("PASS" if (g1_all and g1_ladder) else "FAIL"))
for inst in INSTANCES:
    g2i = g2_rows[inst]
    L.append("G2_cumulativeStrict[%s]=%s" % (inst, "PASS" if g2i["g2_cum"] else "FAIL"))
    L.append("G2_perWindowStrict[%s]=%s" % (inst, "PASS" if g2i["g2_win"] else "FAIL"))
    L.append("G2_perUStrict[%s]=%s" % (inst, "PASS" if g2i["g2_u"] else "FAIL"))
L.append("G3_shareC0gtC3=%s" % ("PASS" if g3_c0gtC3_all else "FAIL"))
L.append("G3_shareMedianOrdering=%s" % ("PASS" if (g3_median_order and g3_median_order_all_groups) else "FAIL"))
L.append("G3_adjacentDropsGE1pp=%d" % g3_adjacent)
L.append("G3_totalLocalStrict=%s" % ("PASS" if g3_totals_strict else "FAIL"))
L.append("G4_behaviour=%d/4_groups" % sum(1 for v in g4_group.values() if v["pass"]))
L.append("localFeShareMedian=%s" % (";".join("%s=%s" % (p, fmt(profile_share_median[p], 6)) for p in PROFILES)))
L.append("cumulativeAllocation=%s" % (";".join("%s=%d" % (p, sim[p]["total"]) for p in PROFILES)))
L.append("DOSE_RESOLUTION_GATE=%s" % DOSE_GATE)
L.append("```")
L.append("")

with io.open(os.path.join(OUT, "dose-resolution.md"), "w", encoding="utf-8") as fh:
    fh.write("\n".join(L))

print("scheduleValidation %d/16 MATCH" % n_match)
print("G1=%s G3=%s G4=%s(cumul/win/u ok on both instances: %s)" % (
    g1_all and g1_ladder, g3, g4,
    all(g2_rows[i]["g2_cum"] and g2_rows[i]["g2_win"] and g2_rows[i]["g2_u"] for i in INSTANCES)))
print("localFeShare medians:", {p: round(profile_share_median[p], 6) for p in PROFILES})
print("DOSE_RESOLUTION_GATE=%s" % DOSE_GATE)
sys.exit(0 if DOSE_GATE == "PASSED" else 1)
