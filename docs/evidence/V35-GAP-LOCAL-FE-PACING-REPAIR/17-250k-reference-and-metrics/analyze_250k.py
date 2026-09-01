# -*- coding: utf-8 -*-
"""250k reference/metrics/paired-delta pipeline (frozen prereg §7, task §8/§9/§10).

Reads  16-remote-250k-runs/sync/seed-<S>/results/run-GAPL250K-<A>-<I>-<S>/
Writes (this directory):
  terminal-reference-fronts/PFref_terminal_<instance>.csv
  checkpoint-reference-fronts/PFref_checkpoint_<instance>_<targetFE>.csv
  terminal-metrics.csv
  checkpoint-metrics.csv
  paired-deltas.csv
  candidate-decision.csv

frontType isolation (contract): terminal metrics use terminal-decision-front
(front.csv); checkpoint metrics use checkpoint-observed-full-front only; each
(instance, targetFE) gets its own reference.  Numbers are script-generated;
nothing hand-copied.
"""
import csv, io, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SYNC = os.path.join(ROOT, "16-remote-250k-runs", "sync")
sys.path.insert(0, r"E:\学习\李明哲-毕业材料\张博改进\scripts")
import fc6_metrics as fc6  # corrected pipeline: raw dedupe -> raw ND -> unclamped normalize -> HV(1.1^3)

ARMS = ["C0", "C2", "C3"]
CANDS = ["C2", "C3"]
INSTANCES = ["50_2_3_1", "100_5_3_1"]
SEEDS = ["20260916", "20260917", "20260918"]
TARGETS = [50000, 100000, 150000, 200000]


def run_dir(arm, inst, seed):
    return os.path.join(SYNC, "seed-%s" % seed, "results",
                        "run-GAPL250K-%s-%s-%s" % (arm, inst, seed))


def read_points(path, skip_header=True, cols=(0, 1, 2)):
    pts = []
    if not os.path.exists(path):
        return pts
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            if skip_header and line.lower().startswith(("cmax", "candidatefingerprint")):
                continue
            parts = line.split(",")
            try:
                pts.append([float(parts[cols[0]]), float(parts[cols[1]]), float(parts[cols[2]])])
            except (ValueError, IndexError):
                continue
    return pts


def read_props(path):
    flat = {}
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            if line.startswith("mechanismSummary="):
                for pair in line[len("mechanismSummary="):].rstrip().split(","):
                    k, _, v = pair.partition("=")
                    flat[k] = v
            elif "=" in line:
                k, _, v = line.rstrip("\n").partition("=")
                flat[k] = v
    return flat


def write_front(path, pts):
    with io.open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("Cmax,TEC,TWC\n")
        for p in sorted(pts):
            fh.write("%r,%r,%r\n" % (p[0], p[1], p[2]))


os.makedirs(os.path.join(HERE, "terminal-reference-fronts"), exist_ok=True)
os.makedirs(os.path.join(HERE, "checkpoint-reference-fronts"), exist_ok=True)

# ---- gather run data --------------------------------------------------------
data = {}
for inst in INSTANCES:
    for arm in ARMS:
        for seed in SEEDS:
            d = run_dir(arm, inst, seed)
            b = read_props(os.path.join(d, "budget-termination.properties"))
            g = read_props(os.path.join(d, "formal-gate.properties"))
            term_dec = read_points(os.path.join(d, "front.csv"))
            ck_obs = {t: read_points(os.path.join(d, "checkpoints",
                                                  "checkpoint-%d-observed-full-front.csv" % t),
                                     skip_header=True, cols=(1, 2, 3))
                      for t in TARGETS}
            ck_dec_sizes = {}
            reg = os.path.join(d, "checkpoints", "checkpoint-registry.csv")
            with io.open(reg, encoding="utf-8") as fh:
                for row in csv.DictReader(fh):
                    key = (int(row["checkpointTargetFE"]), row["frontType"])
                    ck_dec_sizes[key] = int(row["frontSize"])
            data[(arm, inst, seed)] = {
                "dir": d, "budget": b, "gate": g,
                "terminal": term_dec, "ck_obs": ck_obs,
                "ck_dec_sizes": ck_dec_sizes,
                "actualFE": int(b["actualFE"]),
                "kind": b["terminationKind"],
                "outerCycles": int(b["formalOuterCycles"]),
                "totalLocalFE": int(b["totalLocalFE"]),
                "share": float(b["localFeShare"]),
                "remaining": int(b["remainingFE"]),
                "util": float(b["utilizationRate"]),
                "runtimeNanos": int(g["wallNanos"]),
            }

# ---- references -------------------------------------------------------------
for inst in INSTANCES:
    union = []
    for arm in ARMS:
        for seed in SEEDS:
            union.extend(data[(arm, inst, seed)]["terminal"])
    pf = fc6.nondominated(fc6.unique(union))
    write_front(os.path.join(HERE, "terminal-reference-fronts",
                             "PFref_terminal_%s.csv" % inst), pf)
    for t in TARGETS:
        union_ck = []
        for arm in ARMS:
            for seed in SEEDS:
                union_ck.extend(data[(arm, inst, seed)]["ck_obs"][t])
        pf_ck = fc6.nondominated(fc6.unique(union_ck))
        write_front(os.path.join(HERE, "checkpoint-reference-fronts",
                                 "PFref_checkpoint_%s_%d.csv" % (inst, t)), pf_ck)

# ---- metrics ----------------------------------------------------------------
def metric_row(points, reference):
    m = fc6.metrics(points, reference, "corrected")
    mins = fc6.min_objectives(points)
    return m, mins


term_rows = []
refs = {inst: read_points(os.path.join(HERE, "terminal-reference-fronts",
                                       "PFref_terminal_%s.csv" % inst), skip_header=True)
        for inst in INSTANCES}
ck_refs = {(inst, t): read_points(os.path.join(HERE, "checkpoint-reference-fronts",
                                               "PFref_checkpoint_%s_%d.csv" % (inst, t)),
                                  skip_header=True)
           for inst in INSTANCES for t in TARGETS}

for inst in INSTANCES:
    for arm in ARMS:
        for seed in SEEDS:
            d = data[(arm, inst, seed)]
            m, mins = metric_row(d["terminal"], refs[inst])
            term_rows.append([inst, arm, seed, d["actualFE"], d["kind"], d["outerCycles"],
                              d["totalLocalFE"], round(d["share"], 6), len(d["terminal"]),
                              m["hv"], m["igd"], m["spacing"], m["cFwd"], m["cRev"],
                              mins["minCmax"], mins["minTEC"], mins["minTWC"], d["runtimeNanos"]])
term_header = ["instance", "arm", "seed", "actualFE", "terminationKind", "outerCycles",
               "totalLocalFE", "localFeShare", "frontSize", "HV", "IGD", "Spacing",
               "C_front_vs_PFref", "C_PFref_vs_front", "minCmax", "minTEC", "minTWC",
               "wallNanos"]
with open(os.path.join(HERE, "terminal-metrics.csv"), "w", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(term_header)
    w.writerows(term_rows)

ck_rows = []
for inst in INSTANCES:
    for arm in ARMS:
        for seed in SEEDS:
            d = data[(arm, inst, seed)]
            for t in TARGETS:
                pts = d["ck_obs"][t]
                m, mins = metric_row(pts, ck_refs[(inst, t)])
                ck_rows.append([inst, arm, seed, t, len(pts),
                                d["ck_dec_sizes"].get((t, "checkpoint-decision-front"), -1),
                                m["hv"], m["igd"], m["spacing"], m["cFwd"], m["cRev"],
                                mins["minCmax"], mins["minTEC"], mins["minTWC"]])
ck_header = ["instance", "arm", "seed", "checkpointTargetFE",
             "observedFullFrontSize", "decisionFrontSize", "HV", "IGD", "Spacing",
             "C_front_vs_PFref", "C_PFref_vs_front", "minCmax", "minTEC", "minTWC"]
with open(os.path.join(HERE, "checkpoint-metrics.csv"), "w", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(ck_header)
    w.writerows(ck_rows)

# ---- paired deltas ----------------------------------------------------------
def median3(vals):
    s = sorted(vals)
    return s[1]


delta_rows = []
term_medians = {}
ck_medians = {}
for inst in INSTANCES:
    for arm in CANDS:
        per_seed = []
        for seed in SEEDS:
            c0 = data[("C0", inst, seed)]
            ca = data[(arm, inst, seed)]
            hv0 = [r for r in term_rows if r[0] == inst and r[1] == "C0" and r[2] == seed][0][9]
            ig0 = [r for r in term_rows if r[0] == inst and r[1] == "C0" and r[2] == seed][0][10]
            hva = [r for r in term_rows if r[0] == inst and r[1] == arm and r[2] == seed][0][9]
            iga = [r for r in term_rows if r[0] == inst and r[1] == arm and r[2] == seed][0][10]
            m0 = fc6.min_objectives(c0["terminal"])
            ma = fc6.min_objectives(ca["terminal"])
            row = [inst, seed, arm,
                   (hva - hv0) / hv0, (ig0 - iga) / ig0,
                   (m0["minCmax"] - ma["minCmax"]) / m0["minCmax"],
                   (m0["minTEC"] - ma["minTEC"]) / m0["minTEC"],
                   (m0["minTWC"] - ma["minTWC"]) / m0["minTWC"]]
            for t in TARGETS:
                hv0c = [r for r in ck_rows if r[0] == inst and r[1] == "C0"
                        and r[2] == seed and r[3] == t][0][6]
                ig0c = [r for r in ck_rows if r[0] == inst and r[1] == "C0"
                        and r[2] == seed and r[3] == t][0][7]
                hvac = [r for r in ck_rows if r[0] == inst and r[1] == arm
                        and r[2] == seed and r[3] == t][0][6]
                iga_c = [r for r in ck_rows if r[0] == inst and r[1] == arm
                         and r[2] == seed and r[3] == t][0][7]
                row.append((hvac - hv0c) / hv0c)
                row.append((ig0c - iga_c) / ig0c)
            per_seed.append(row)
            delta_rows.append(row)
        med = [inst, "MEDIAN", arm]
        for i in range(3, len(per_seed[0])):
            med.append(median3([r[i] for r in per_seed]))
        delta_rows.append(med)
        term_medians[(arm, inst)] = med

pd_header = ["instance", "seed", "arm", "dHV_terminal", "dIGD_terminal",
             "dCmax_terminal", "dTEC_terminal", "dTWC_terminal"]
for t in TARGETS:
    pd_header += ["dHV_ck%d" % t, "dIGD_ck%d" % t]
with open(os.path.join(HERE, "paired-deltas.csv"), "w", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(pd_header)
    w.writerows(delta_rows)

# ---- gates (prereg §8) ------------------------------------------------------
COL = {name: idx for idx, name in enumerate(pd_header)}
TIDX = {t: (pd_header.index("dHV_ck%d" % t), pd_header.index("dIGD_ck%d" % t))
        for t in TARGETS}


def med(arm, inst, col):
    return term_medians[(arm, inst)][COL[col]]


decision_rows = []
for arm in CANDS:
    fails = []
    # normal safety
    if not (med(arm, "50_2_3_1", "dHV_terminal") >= -0.02
            and med(arm, "50_2_3_1", "dIGD_terminal") >= -0.10):
        fails.append("normalSafety")
    # hard improvement + other not beyond safety
    hv_h = med(arm, "100_5_3_1", "dHV_terminal")
    ig_h = med(arm, "100_5_3_1", "dIGD_terminal")
    if not ((hv_h >= 0.02 or ig_h >= 0.10) and hv_h >= -0.02 and ig_h >= -0.10):
        fails.append("hardImprovement")
    # single-seed disaster: >=2/3 seeds with dHV<-5% AND dIGD<-20% on any instance
    disaster = False
    for inst in INSTANCES:
        n = 0
        for seed in SEEDS:
            row = [r for r in delta_rows if r[0] == inst and r[1] == seed and r[2] == arm][0]
            if row[COL["dHV_terminal"]] < -0.05 and row[COL["dIGD_terminal"]] < -0.20:
                n += 1
        if n >= 2:
            disaster = True
    if disaster:
        fails.append("singleSeedDisaster")
    # triple objective: systematic = same instance >=2/3 seeds degrade >2%
    triple_detail = []
    for obj in ("dCmax_terminal", "dTEC_terminal", "dTWC_terminal"):
        for inst in INSTANCES:
            n = sum(1 for r in delta_rows
                    if r[0] == inst and r[1] in SEEDS and r[2] == arm
                    and r[COL[obj]] < -0.02)
            triple_detail.append("%s_%s=%d/3" % (obj, inst, n))
            if n >= 2:
                fails.append("tripleObjective:%s:%s" % (obj, inst))
    # checkpoint consistency: real conflict only if >=2 checkpoints opposite to
    # terminal direction AND magnitude HV>2% or IGD>10% AND >=2/3 seeds consistent
    conflicts = 0
    flip_notes = []
    for inst in INSTANCES:
        term_dir_hv = med(arm, inst, "dHV_terminal")
        for t in TARGETS:
            med_hv_ck = term_medians[(arm, inst)][TIDX[t][0]]
            opposite = (term_dir_hv >= 0 and med_hv_ck < 0) or (term_dir_hv < 0 and med_hv_ck >= 0)
            if opposite and abs(med_hv_ck) > 0.02:
                n_consistent = sum(1 for r in delta_rows
                                   if r[0] == inst and r[1] in SEEDS and r[2] == arm
                                   and ((term_dir_hv >= 0 and r[TIDX[t][0]] < 0)
                                        or (term_dir_hv < 0 and r[TIDX[t][0]] >= 0)))
                if n_consistent >= 2:
                    conflicts += 1
                    flip_notes.append("HV_ck%d_%s=%.4f" % (t, inst, med_hv_ck))
                else:
                    flip_notes.append("MINOR_FLUCTUATION_HV_ck%d_%s" % (t, inst))
            elif opposite:
                flip_notes.append("MINOR_FLUCTUATION_HV_ck%d_%s" % (t, inst))
    if conflicts >= 2:
        fails.append("checkpointConsistency:CONFLICT")
    gates = {
        "normalSafetyGate": "PASS" if "normalSafety" not in fails else "FAIL",
        "hardImprovementGate": "PASS" if "hardImprovement" not in fails else "FAIL",
        "singleSeedDisasterGate": "PASS" if not disaster else "FAIL",
        "tripleObjectiveGate": "PASS" if not any(f.startswith("tripleObjective") for f in fails) else "FAIL",
        "checkpointConsistencyGate": ("CONFLICT" if conflicts >= 2
                                      else "PASS(MINOR_FLUCTUATION_ONLY)"),
        "allGates": "PASS" if not fails else "FAIL",
    }
    five = [med(arm, "100_5_3_1", "dHV_terminal"), med(arm, "100_5_3_1", "dIGD_terminal"),
            min(med(arm, i, "dCmax_terminal") for i in INSTANCES),
            min(med(arm, i, "dTEC_terminal") for i in INSTANCES),
            min(med(arm, i, "dTWC_terminal") for i in INSTANCES)]
    decision_rows.append([arm, gates["normalSafetyGate"], gates["hardImprovementGate"],
                          gates["singleSeedDisasterGate"], gates["tripleObjectiveGate"],
                          gates["checkpointConsistencyGate"], gates["allGates"],
                          ";".join(fails) if fails else "NONE",
                          ";".join(flip_notes) if flip_notes else "NONE",
                          ";".join(triple_detail),
                          "%.6f,%.6f,%.6f,%.6f,%.6f" % tuple(five)])

with open(os.path.join(HERE, "candidate-decision.csv"), "w", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(["candidate", "normalSafetyGate", "hardImprovementGate",
                "singleSeedDisasterGate", "tripleObjectiveGate",
                "checkpointConsistencyGate", "allGates", "failedGates",
                "checkpointFlips", "tripleObjectiveDetail",
                "fiveDim[dHV_hard,dIGD_hard,dCmax_all,dTEC_all,dTWC_all]"])
    w.writerows(decision_rows)

# console summary
print("== terminal medians (dHV/dIGD vs C0) ==")
for arm in CANDS:
    for inst in INSTANCES:
        m = term_medians[(arm, inst)]
        print("%s %s: dHV=%+.4f dIGD=%+.4f dCmax=%+.4f dTEC=%+.4f dTWC=%+.4f"
              % (arm, inst, m[COL["dHV_terminal"]], m[COL["dIGD_terminal"]],
                 m[COL["dCmax_terminal"]], m[COL["dTEC_terminal"]], m[COL["dTWC_terminal"]]))
print("== checkpoint median dHV (3-seed) ==")
for arm in CANDS:
    for inst in INSTANCES:
        vals = ["%+ .4f" % term_medians[(arm, inst)][TIDX[t][0]] for t in TARGETS]
        print("%s %s ck dHV: %s" % (arm, inst, vals))
print("== gates ==")
for row in decision_rows:
    print(row[0], row[1:7], "failed:", row[7])
print("ANALYZE_250K DONE")
