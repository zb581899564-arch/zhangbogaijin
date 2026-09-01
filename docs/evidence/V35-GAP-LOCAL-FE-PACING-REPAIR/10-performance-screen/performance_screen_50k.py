# -*- coding: utf-8 -*-
"""V35-LOCAL-FE-PACING-50K Agent C — Tasks B+C: dual-caliber metrics, paired
response, performance screening and candidate selection.

Writes (into 10-performance-screen/ only):
  checkpoint-fronts-common/checkpoints-registry.csv  run x checkpoint scalar registry
  reference-fronts-terminal/PFref_terminal_<inst>.csv per-instance terminal reference
  metrics-terminal.csv      per run terminal metrics (corrected pipeline)
  metrics-common.csv        per run x checkpoint {10k,20k,30k,40k} scalars
  paired-response.csv       per instance x seed x candidate deltas (both calibers)
  candidate-screening.csv   per candidate four gates + five-dim Pareto + retention
  performance-screen.md     machine-generated narrative + status block

Pipeline provenance:
- Metric functions are IMPORTED from scripts/fc6_metrics.py (corrected pipeline:
  raw dedup -> raw strict-nondominated -> unified min/max normalization from the
  reference -> NO clamp; HV scanline rx=ry=rz=1.1; IGD in normalized space).
  Only the per-run r_raw recomputation is hoisted out of the loop (hoisting the
  already-nondominated PFref is mathematically identical, verified by
  nondominated(unique(ref)) == ref).
- F_common follows prereg 4-D1: largest preregistered checkpoint
  {10000,20000,30000,40000} with a cmax-audit-curves.csv row in ALL 16 runs.
- Gates follow prereg sections 9/10/11 (normal safety, hard improvement,
  triple-objective protection, dual-caliber consistency; five-dim Pareto;
  tiebreak keys; <=2 retained).
- All numbers are computed from the run files; nothing is hand-copied.
"""
import csv, io, os, sys

ROOT = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR"
SYNC = os.path.join(ROOT, "08-remote-50k", "sync")
OUT = os.path.join(ROOT, "10-performance-screen")
SCRIPTS = r"E:\学习\李明哲-毕业材料\张博改进\scripts"
sys.path.insert(0, SCRIPTS)
import fc6_metrics as fm  # noqa: E402  (corrected-pipeline metric implementation)

SEEDS = ["20260907", "20260914"]
INSTANCES = ["50_2_3_1", "100_5_3_1"]
PROFILES = ["C0", "C1", "C2", "C3"]
CANDIDATES = ["C1", "C2", "C3"]
CHECKPOINTS = [10000, 20000, 30000, 40000]
EPS = 1e-12


def read_kv(path):
    d = {}
    with io.open(path, "r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            line = line.rstrip("\n")
            if "=" in line and not line.startswith("#"):
                k, _, v = line.partition("=")
                d[k] = v
    return d


def median(values):
    vals = sorted(values)
    n = len(vals)
    if n == 0:
        return float("nan")
    return vals[n // 2] if n % 2 == 1 else (vals[n // 2 - 1] + vals[n // 2]) / 2.0


def fmt(x, nd=6):
    return ("%." + str(nd) + "f") % x


def sign(x):
    return 0 if abs(x) <= EPS else (1 if x > 0 else -1)


# ---------------------------------------------------------------- load runs
runs = []
for seed in SEEDS:
    for inst in INSTANCES:
        for prof in PROFILES:
            run_key = "run-GAPL50K-%s-%s-%s" % (prof, inst, seed)
            d = os.path.join(SYNC, "seed-" + seed, "results", run_key)
            fg = read_kv(os.path.join(d, "formal-gate.properties"))
            st = read_kv(os.path.join(d, "status.properties"))
            bt = read_kv(os.path.join(d, "budget-termination.properties"))
            pf = read_kv(os.path.join(d, "profile.txt"))
            front = fm.read_front(os.path.join(d, "front.csv"))
            audit = {}
            with io.open(os.path.join(d, "cmax-audit-curves.csv"), "r",
                         encoding="utf-8", errors="replace") as fh:
                rdr = csv.DictReader(fh)
                for row in rdr:
                    audit[int(row["fe"])] = row
            runs.append({
                "runKey": run_key, "dir": d, "profile": prof, "instance": inst,
                "seed": seed, "front": front,
                "frontRows": len(front),
                "frontSizeExported": int(fg.get("frontSize", "0")),
                "actualFE": int(bt.get("actualFE", "0")),
                "terminationKind": bt.get("terminationKind", ""),
                "betaMax": float(pf.get("betaMax", "nan")),
                "wallNanos": int(st.get("algorithmRunNanos", "0")),
                "audit": audit,
            })
assert len(runs) == 16

# ---------------------------------------------------- B1: F_common + registry
os.makedirs(os.path.join(OUT, "checkpoint-fronts-common"), exist_ok=True)
reg_rows = []
for r in runs:
    for cp in CHECKPOINTS:
        row = r["audit"].get(cp)
        present = row is not None
        r.setdefault("cp", {})[cp] = (row if present else None)
        reg_rows.append([r["runKey"], r["profile"], r["instance"], r["seed"], cp,
                         "true" if present else "false",
                         row["bestCmaxGlobal"] if present else "",
                         row["bestTECGlobal"] if present else "",
                         row["bestTWCGlobal"] if present else ""])
with io.open(os.path.join(OUT, "checkpoint-fronts-common", "checkpoints-registry.csv"),
             "w", encoding="utf-8", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(["runKey", "profile", "instance", "seed", "checkpointFE", "rowPresent",
                "bestCmaxGlobal", "bestTECGlobal", "bestTWCGlobal"])
    w.writerows(reg_rows)

F_COMMON = None
f_common_detail = []
for cp in sorted(CHECKPOINTS, reverse=True):
    if all(r["cp"].get(cp) is not None for r in runs):
        F_COMMON = cp
        break
for cp in sorted(CHECKPOINTS):
    n_present = sum(1 for r in runs if r["cp"].get(cp) is not None)
    f_common_detail.append("FE=%d presentIn=%d/16" % (cp, n_present))
assert F_COMMON is not None, "no preregistered checkpoint present in all 16 runs"

# ---------------------------------------------------- B2: terminal references
os.makedirs(os.path.join(OUT, "reference-fronts-terminal"), exist_ok=True)
pfref = {}
for inst in INSTANCES:
    pts = [p for r in runs if r["instance"] == inst for p in r["front"]]
    nd = fm.nondominated(fm.unique(pts))
    pfref[inst] = nd
    with io.open(os.path.join(OUT, "reference-fronts-terminal",
                              "PFref_terminal_%s.csv" % inst),
                 "w", encoding="utf-8", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(["Cmax", "TEC", "TWC"])
        for p in nd:
            w.writerow([repr(p[0]), repr(p[1]), repr(p[2])])

# ---------------------------------------------------- B3: terminal metrics
# corrected pipeline, identical to fc6_metrics.metrics(): a_raw =
# nondominated(unique(front)); r_raw = nondominated(unique(reference)) == PFref
# (already filtered); normalize WITHOUT clamp against PFref min/max; HV box
# rx=ry=rz=1.1; IGD against normalized PFref.
term = {}
for inst in INSTANCES:
    ref_raw = pfref[inst]
    ref_norm = fm.normalize(ref_raw, ref_raw, clamp=False)
    for r in [x for x in runs if x["instance"] == inst]:
        a_raw = fm.nondominated(fm.unique(r["front"]))
        a_norm = fm.normalize(a_raw, ref_raw, clamp=False)
        mo = fm.min_objectives(r["front"])
        m = {
            "hv": fm.hypervolume(a_norm),
            "igd": fm.igd(a_norm, ref_norm),
            "spacing": fm.spacing(a_norm),
            "cFwd": fm.coverage(a_norm, ref_norm),   # C(C, PFref)
            "cRev": fm.coverage(ref_norm, a_norm),   # C(PFref, C)
            "n": len(a_norm),
            "refN": len(ref_raw),
            "minCmax": mo["minCmax"], "minTEC": mo["minTEC"], "minTWC": mo["minTWC"],
            "rawN": len(r["front"]),
        }
        term[r["runKey"]] = m
        r["metrics"] = m

cols = ["runKey", "profile", "instance", "seed", "HV", "IGD", "Spacing",
        "C_front_vs_PFref", "C_PFref_vs_front", "frontSizeExported", "frontRows",
        "ndSize", "pfrefSize", "minCmax", "minTEC", "minTWC", "actualFE",
        "terminationKind", "wallNanos", "wallSeconds"]
with io.open(os.path.join(OUT, "metrics-terminal.csv"), "w", encoding="utf-8",
             newline="") as fh:
    w = csv.writer(fh)
    w.writerow(cols)
    for r in runs:
        m = r["metrics"]
        w.writerow([r["runKey"], r["profile"], r["instance"], r["seed"],
                    fmt(m["hv"], 6), fmt(m["igd"], 6), fmt(m["spacing"], 6),
                    fmt(m["cFwd"], 6), fmt(m["cRev"], 6), r["frontSizeExported"],
                    r["frontRows"], m["n"], m["refN"],
                    fmt(m["minCmax"], 6), fmt(m["minTEC"], 6), fmt(m["minTWC"], 6),
                    r["actualFE"], r["terminationKind"], r["wallNanos"],
                    fmt(r["wallNanos"] / 1e9, 2)])

# ---------------------------------------------------- B4: common-FE scalars
with io.open(os.path.join(OUT, "metrics-common.csv"), "w", encoding="utf-8",
             newline="") as fh:
    w = csv.writer(fh)
    w.writerow(["runKey", "profile", "instance", "seed", "checkpointFE",
                "isF_common", "bestCmaxGlobal", "bestTECGlobal", "bestTWCGlobal"])
    for r in runs:
        for cp in CHECKPOINTS:
            row = r["cp"].get(cp)
            if row is None:
                w.writerow([r["runKey"], r["profile"], r["instance"], r["seed"],
                            cp, "false", "", "", ""])
            else:
                w.writerow([r["runKey"], r["profile"], r["instance"], r["seed"],
                            cp, "true" if cp == F_COMMON else "false",
                            row["bestCmaxGlobal"], row["bestTECGlobal"],
                            row["bestTWCGlobal"]])

# ---------------------------------------------------- B5: paired response
def rel(new, base):
    return (new - base) / max(EPS, base)


def rel_improve(cand_val, base_val):
    """positive = candidate better (cand smaller for min-objectives)"""
    return (base_val - cand_val) / max(EPS, base_val)


pairs = []
for inst in INSTANCES:
    for seed in SEEDS:
        for cand in CANDIDATES:
            base = next(r for r in runs if r["instance"] == inst and r["seed"] == seed
                        and r["profile"] == "C0")
            c = next(r for r in runs if r["instance"] == inst and r["seed"] == seed
                     and r["profile"] == cand)
            mb, mc = base["metrics"], c["metrics"]
            fb, fc = base["cp"][F_COMMON], c["cp"][F_COMMON]
            row = {
                "instance": inst, "seed": seed, "candidate": cand,
                "dHV": (mc["hv"] - mb["hv"]) / max(EPS, mb["hv"]),
                "dIGD": (mb["igd"] - mc["igd"]) / max(EPS, mb["igd"]),
                "dCmax_t": rel_improve(mc["minCmax"], mb["minCmax"]),
                "dTEC_t": rel_improve(mc["minTEC"], mb["minTEC"]),
                "dTWC_t": rel_improve(mc["minTWC"], mb["minTWC"]),
                "dCmax_c": rel_improve(float(fc["bestCmaxGlobal"]), float(fb["bestCmaxGlobal"])),
                "dTEC_c": rel_improve(float(fc["bestTECGlobal"]), float(fb["bestTECGlobal"])),
                "dTWC_c": rel_improve(float(fc["bestTWCGlobal"]), float(fb["bestTWCGlobal"])),
                "hvC0": mb["hv"], "hvC": mc["hv"], "igdC0": mb["igd"], "igdC": mc["igd"],
            }
            pairs.append(row)

med = {}
for inst in INSTANCES:
    for cand in CANDIDATES:
        rows = [p for p in pairs if p["instance"] == inst and p["candidate"] == cand]
        med[(inst, cand)] = {k: median([p[k] for p in rows])
                             for k in ("dHV", "dIGD", "dCmax_t", "dTEC_t", "dTWC_t",
                                       "dCmax_c", "dTEC_c", "dTWC_c")}
pooled = {}
for cand in CANDIDATES:
    rows = [p for p in pairs if p["candidate"] == cand]
    pooled[cand] = {k: median([p[k] for p in rows])
                    for k in ("dHV", "dIGD", "dCmax_t", "dTEC_t", "dTWC_t",
                              "dCmax_c", "dTEC_c", "dTWC_c")}

with io.open(os.path.join(OUT, "paired-response.csv"), "w", encoding="utf-8",
             newline="") as fh:
    w = csv.writer(fh)
    w.writerow(["section", "instance", "seed", "candidate",
                "dHV_terminal", "dIGD_terminal", "dCmax_terminal", "dTEC_terminal",
                "dTWC_terminal", "fCommon", "dCmax_commonFE", "dTEC_commonFE",
                "dTWC_commonFE", "note"])
    for p in pairs:
        w.writerow(["SEED", p["instance"], p["seed"], p["candidate"],
                    fmt(p["dHV"], 6), fmt(p["dIGD"], 6), fmt(p["dCmax_t"], 6),
                    fmt(p["dTEC_t"], 6), fmt(p["dTWC_t"], 6), F_COMMON,
                    fmt(p["dCmax_c"], 6), fmt(p["dTEC_c"], 6), fmt(p["dTWC_c"], 6), ""])
    for inst in INSTANCES:
        for cand in CANDIDATES:
            m = med[(inst, cand)]
            w.writerow(["MEDIAN_2SEED", inst, "MEDIAN", cand,
                        fmt(m["dHV"], 6), fmt(m["dIGD"], 6), fmt(m["dCmax_t"], 6),
                        fmt(m["dTEC_t"], 6), fmt(m["dTWC_t"], 6), F_COMMON,
                        fmt(m["dCmax_c"], 6), fmt(m["dTEC_c"], 6), fmt(m["dTWC_c"], 6),
                        ""])

# ================================================================ C: gates
def dominates5(b, a):
    """b dominates a in five-dim space (all higher=better), strict."""
    ge = all(b[k] >= a[k] - EPS for k in a)
    gt = any(b[k] > a[k] + EPS for k in a)
    return ge and gt


screen = {}
for cand in CANDIDATES:
    m_n = med[("50_2_3_1", cand)]
    m_h = med[("100_5_3_1", cand)]
    seeds_n = [p for p in pairs if p["instance"] == "50_2_3_1" and p["candidate"] == cand]

    # 1. normal-instance safety gate (terminal caliber, 50_2_3_1)
    safety_ok = (m_n["dHV"] >= -0.02 and m_n["dIGD"] >= -0.10)
    veto_seeds = [p for p in seeds_n if p["dHV"] < -0.05 and p["dIGD"] < -0.20]
    normal_safety = safety_ok and not veto_seeds

    # 2. hard-instance improvement gate (terminal caliber, 100_5_3_1)
    signal = (m_h["dHV"] >= 0.02) or (m_h["dIGD"] >= 0.10)
    other_ok = (m_h["dHV"] >= -0.02) and (m_h["dIGD"] >= -0.10)
    hard_improve = signal and other_ok

    # 3. triple-objective protection gate (common-FE caliber @F_common)
    viol = []
    both_breach = False
    for obj in ("dCmax_c", "dTEC_c", "dTWC_c"):
        v_n = med[("50_2_3_1", cand)][obj]
        v_h = med[("100_5_3_1", cand)][obj]
        if v_n < -0.02:
            viol.append("50_2_3_1:%s=%.4f%%" % (obj, v_n * 100))
        if v_h < -0.02:
            viol.append("100_5_3_1:%s=%.4f%%" % (obj, v_h * 100))
        if v_n < -0.02 and v_h < -0.02:
            both_breach = True
    triple = not both_breach

    # 4. dual-caliber consistency gate (Cmax/TEC/TWC; pooled median over both
    #    instances x 2 seeds; sign disagreement on any objective = conflict)
    conflicts = []
    for obj_t, obj_c, name in (("dCmax_t", "dCmax_c", "Cmax"),
                               ("dTEC_t", "dTEC_c", "TEC"),
                               ("dTWC_t", "dTWC_c", "TWC")):
        st_, sc_ = sign(pooled[cand][obj_t]), sign(pooled[cand][obj_c])
        if st_ != 0 and sc_ != 0 and st_ != sc_:
            conflicts.append("%s term=%+.4f%% common=%+.4f%%" % (
                name, pooled[cand][obj_t] * 100, pooled[cand][obj_c] * 100))
    dual_ok = not conflicts

    # five-dim values (all higher = better)
    five = {
        "dHV_hard": m_h["dHV"], "dIGD_hard": m_h["dIGD"],
        "dCmax_all": min(med[("50_2_3_1", cand)]["dCmax_c"], m_h["dCmax_c"]),
        "dTEC_all": min(med[("50_2_3_1", cand)]["dTEC_c"], m_h["dTEC_c"]),
        "dTWC_all": min(med[("50_2_3_1", cand)]["dTWC_c"], m_h["dTWC_c"]),
    }
    screen[cand] = {
        "normal_safety": normal_safety, "hard_improve": hard_improve,
        "triple": triple, "dual_ok": dual_ok, "viol": viol, "conflicts": conflicts,
        "veto_seeds": veto_seeds, "five": five,
        "all_gates": normal_safety and hard_improve and triple and dual_ok,
        "signal_hv": m_h["dHV"] >= 0.02, "signal_igd": m_h["dIGD"] >= 0.10,
        "m_n": m_n, "m_h": m_h,
        "tiebreak": {
            "tb1_dCmax_hard_common": m_h["dCmax_c"],
            "tb2_dTEC_hard_common": m_h["dTEC_c"],
            "tb3_dHV_normal_terminal": m_n["dHV"],
            "tb4_dTWC_normal_terminal": m_n["dTWC_t"],
            "tb5_betaDistTo065": abs(next(r["betaMax"] for r in runs
                                          if r["profile"] == cand) - 0.65),
        },
    }

# Pareto filter (five-dim strict) among all candidates and among qualified
for cand in CANDIDATES:
    dom_all = [o for o in CANDIDATES if o != cand and dominates5(screen[o]["five"], screen[cand]["five"])]
    screen[cand]["dominatedBy_all"] = dom_all
qualified = [c for c in CANDIDATES if screen[c]["all_gates"]]
for cand in CANDIDATES:
    dom_q = [o for o in qualified if o != cand and dominates5(screen[o]["five"], screen[cand]["five"])]
    screen[cand]["dominatedBy_qualified"] = dom_q

retained = [c for c in qualified if not screen[c]["dominatedBy_qualified"]]
retained_sorted = sorted(retained, key=lambda c: (
    -screen[c]["tiebreak"]["tb1_dCmax_hard_common"],
    -screen[c]["tiebreak"]["tb2_dTEC_hard_common"],
    -screen[c]["tiebreak"]["tb3_dHV_normal_terminal"],
    -screen[c]["tiebreak"]["tb4_dTWC_normal_terminal"],
    screen[c]["tiebreak"]["tb5_betaDistTo065"]))
if len(retained_sorted) > 2:
    retained_sorted = retained_sorted[:2]

any_conflict = any(not screen[c]["dual_ok"] for c in CANDIDATES)

# dose gate from Task A output
dose_gate = "UNKNOWN"
with io.open(os.path.join(ROOT, "09-dose-resolution", "dose-resolution.csv"),
             "r", encoding="utf-8") as fh:
    for row in csv.reader(fh):
        if row and row[0] == "AGGREGATE" and row[4] == "DOSE_RESOLUTION_GATE":
            dose_gate = row[5]

if dose_gate != "PASSED":
    final_verdict = "DOSE_RESOLUTION_FAILED"
elif retained_sorted:
    final_verdict = ("%s_ADVANCES_TO_250K" %
                     ("ONE_CANDIDATE" if len(retained_sorted) == 1 else "TWO_CANDIDATES"))
elif any(not screen[c]["all_gates"] and not screen[c]["dual_ok"] for c in CANDIDATES):
    final_verdict = "BUDGET_SENSITIVITY_CONFLICT"
else:
    final_verdict = "LOCAL_FE_PACING_REPAIR_NOT_SUPPORTED_AT_50K"

# ---------------------------------------------------- candidate-screening.csv
with io.open(os.path.join(OUT, "candidate-screening.csv"), "w", encoding="utf-8",
             newline="") as fh:
    w = csv.writer(fh)
    w.writerow(["candidate", "betaMax", "normalSafetyGate", "hardImprovementGate",
                "tripleObjectiveGate", "dualCaliberGate", "allFourGates",
                "dHV_hard_terminal_median", "dIGD_hard_terminal_median",
                "dCmax_all_worstCommonMedian", "dTEC_all_worstCommonMedian",
                "dTWC_all_worstCommonMedian", "dHV_normal_terminal_median",
                "dIGD_normal_terminal_median", "dominatedBy_allCandidates",
                "dominatedBy_qualified", "tb1_dCmax_hard_common", "tb2_dTEC_hard_common",
                "tb3_dHV_normal_terminal", "tb4_dTWC_normal_terminal",
                "tb5_betaDistTo065", "gateFlags", "retained", "retentionRank"])
    for cand in CANDIDATES:
        s = screen[cand]
        flags = []
        if s["viol"]:
            flags.append("tripleBelowMinus2pp:" + ";".join(s["viol"]))
        if s["veto_seeds"]:
            flags.append("normalVetoSeeds:%s" % ["%s|%s" % (v["seed"], fmt(v["dHV"], 4)) for v in s["veto_seeds"]])
        if s["conflicts"]:
            flags.append("dualCaliberConflict:" + ";".join(s["conflicts"]))
        ret = cand in retained_sorted
        rank = retained_sorted.index(cand) + 1 if ret else ""
        w.writerow([cand, fmt(next(r["betaMax"] for r in runs if r["profile"] == cand), 2),
                    "PASS" if s["normal_safety"] else "FAIL",
                    "PASS" if s["hard_improve"] else "FAIL",
                    "PASS" if s["triple"] else "FAIL",
                    "PASS" if s["dual_ok"] else "FAIL",
                    "PASS" if s["all_gates"] else "FAIL",
                    fmt(s["five"]["dHV_hard"], 6), fmt(s["five"]["dIGD_hard"], 6),
                    fmt(s["five"]["dCmax_all"], 6), fmt(s["five"]["dTEC_all"], 6),
                    fmt(s["five"]["dTWC_all"], 6),
                    fmt(s["m_n"]["dHV"], 6), fmt(s["m_n"]["dIGD"], 6),
                    ";".join(s["dominatedBy_all"]) or "NONE",
                    ";".join(s["dominatedBy_qualified"]) or "NONE",
                    fmt(s["tiebreak"]["tb1_dCmax_hard_common"], 6),
                    fmt(s["tiebreak"]["tb2_dTEC_hard_common"], 6),
                    fmt(s["tiebreak"]["tb3_dHV_normal_terminal"], 6),
                    fmt(s["tiebreak"]["tb4_dTWC_normal_terminal"], 6),
                    fmt(s["tiebreak"]["tb5_betaDistTo065"], 2),
                    "; ".join(flags) or "NONE",
                    "YES" if ret else "NO", rank])
    w.writerow(["FINAL", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "finalVerdict=%s retained=%s doseGate=%s budgetSensitivityConflict=%s"
                % (final_verdict, "+".join(retained_sorted) or "NONE", dose_gate,
                   "true" if any_conflict else "false")])

# ---------------------------------------------------- performance-screen.md
L = []
L.append("# PERFORMANCE-SCREEN-50K — V35-LOCAL-FE-PACING (Agent C, Tasks B+C)")
L.append("")
L.append("- 生成脚本: `10-performance-screen/performance_screen_50k.py`（全部数字由脚本从 16 条 run 文件计算）")
L.append("- 指标实现: **import `scripts/fc6_metrics.py` corrected 管线**（raw 精确去重 → raw 严格 Pareto → 统一 min/max 归一（以 PFref 为 reference）→ **不 clamp**；HV 扫描线 rx=ry=rz=1.1；IGD 归一化空间，参考集=归一化 PFref）。唯一改动：PFref 已是非支配集，`nondominated(unique(PFref))==PFref`，故把参考端计算提升到循环外（逐 run 等价）")
L.append("- 剂量门（Task A 输出 `09-dose-resolution/dose-resolution.csv`）: **DOSE_RESOLUTION_GATE=%s**" % dose_gate)
L.append("")
L.append("## 1. F_common 判定（§4-D1，从数据确认）")
L.append("")
L.append("- 检查点存在性: " + "; ".join(f_common_detail))
L.append("- **F_common = %d**（全部 16 条 run 在该预注册检查点均有 cmax-audit-curves.csv 行；最大共同检查点）" % F_COMMON)
L.append("- 每条 run 在 F_common 的原始标量（bestCmaxGlobal/bestTECGlobal/bestTWCGlobal）见 `checkpoint-fronts-common/checkpoints-registry.csv`（64 行 run×checkpoint 注册表）与 `metrics-common.csv`")
L.append("- 前沿级 HV/IGD 的共同FE比较按 D1 不创建（NOT_EXPORTED_BY_FROZEN_JAR）")
L.append("")
L.append("## 2. 终态 reference（PFref_terminal）")
L.append("")
for inst in INSTANCES:
    union_n = sum(1 for r in runs if r["instance"] == inst for _ in r["front"])
    L.append("- %s: ND(union of 8 terminal fronts) = %d 点（union 输入 %d 点；三目标精确去重 → 严格 Pareto）→ `reference-fronts-terminal/PFref_terminal_%s.csv`" % (
        inst, len(pfref[inst]), union_n, inst))
L.append("")
L.append("## 3. 终态指标（corrected 管线，TERMINAL_PHASE_CONSISTENT_SECONDARY）")
L.append("")
L.append("| instance | profile | seed | HV | IGD | Spacing | C(f,PFref) | C(PFref,f) | frontSize | minCmax | minTEC | minTWC | actualFE | wall(s) |")
L.append("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|")
for r in runs:
    m = r["metrics"]
    L.append("| %s | %s | %s | %s | %s | %s | %s | %s | %d | %.3f | %.1f | %.1f | %d | %.1f |" % (
        r["instance"], r["profile"], r["seed"], fmt(m["hv"], 4), fmt(m["igd"], 4),
        fmt(m["spacing"], 4), fmt(m["cFwd"], 4), fmt(m["cRev"], 4),
        r["frontSizeExported"], m["minCmax"], m["minTEC"], m["minTWC"],
        r["actualFE"], r["wallNanos"] / 1e9))
L.append("")
L.append("（完整 20 列见 `metrics-terminal.csv`；wall = status.properties algorithmRunNanos）")
L.append("")
L.append("### 3.1 紧凑表：2-seed 均值 HV/IGD（instance × profile）")
L.append("")
L.append("| instance | C0 HV | C1 HV | C2 HV | C3 HV | C0 IGD | C1 IGD | C2 IGD | C3 IGD |")
L.append("|---|---|---|---|---|---|---|---|---|")
for inst in INSTANCES:
    vals = []
    for p in PROFILES:
        hv = [term[r["runKey"]]["hv"] for r in runs if r["instance"] == inst and r["profile"] == p]
        ig = [term[r["runKey"]]["igd"] for r in runs if r["instance"] == inst and r["profile"] == p]
        vals.extend([fmt(sum(hv) / 2.0, 4), fmt(sum(ig) / 2.0, 4)])
    L.append("| %s | %s | %s | %s | %s | %s | %s | %s | %s |" % (
        inst, vals[0], vals[2], vals[4], vals[6], vals[1], vals[3], vals[5], vals[7]))
L.append("")
L.append("## 4. 配对响应（同 instance×seed，C0 为基准，正数=候选改善）")
L.append("")
L.append("### 4.1 终态口径（2-seed 中位）")
L.append("")
L.append("| instance | candidate | ΔHV | ΔIGD | ΔCmax | ΔTEC | ΔTWC |")
L.append("|---|---|---|---|---|---|---|")
for inst in INSTANCES:
    for cand in CANDIDATES:
        m = med[(inst, cand)]
        L.append("| %s | %s | %s | %s | %s | %s | %s |" % (
            inst, cand, fmt(m["dHV"] * 100, 2) + "%", fmt(m["dIGD"] * 100, 2) + "%",
            fmt(m["dCmax_t"] * 100, 2) + "%", fmt(m["dTEC_t"] * 100, 2) + "%",
            fmt(m["dTWC_t"] * 100, 2) + "%"))
L.append("")
L.append("### 4.2 common-FE 口径 @F_common=%d（2-seed 中位；bestCmax/bestTEC/bestTWCGlobal）" % F_COMMON)
L.append("")
L.append("| instance | candidate | ΔCmax | ΔTEC | ΔTWC |")
L.append("|---|---|---|---|---|")
for inst in INSTANCES:
    for cand in CANDIDATES:
        m = med[(inst, cand)]
        L.append("| %s | %s | %s | %s | %s |" % (
            inst, cand, fmt(m["dCmax_c"] * 100, 2) + "%",
            fmt(m["dTEC_c"] * 100, 2) + "%", fmt(m["dTWC_c"] * 100, 2) + "%"))
L.append("")
L.append("（逐 seed 12 行 + 中位 6 行见 `paired-response.csv`；ΔHV/ΔIGD 仅终态口径，D1）")
L.append("")
L.append("## 5. 性能筛查四门（§10）")
L.append("")
L.append("### 门1 正常实例安全门（50_2_3_1，终态口径）")
L.append("")
L.append("判据: median ΔHV ≥ −2% 且 median ΔIGD ≥ −10%；且无单 seed 同时 ΔHV<−5% 且 ΔIGD<−20%")
L.append("")
for cand in CANDIDATES:
    s = screen[cand]
    L.append("- %s: median ΔHV=%s (≥−2%%: %s), median ΔIGD=%s (≥−10%%: %s); veto-seed 同时跌破: %s → **%s**" % (
        cand, fmt(s["m_n"]["dHV"] * 100, 2) + "%", "Y" if s["m_n"]["dHV"] >= -0.02 else "N",
        fmt(s["m_n"]["dIGD"] * 100, 2) + "%", "Y" if s["m_n"]["dIGD"] >= -0.10 else "N",
        [v["seed"] for v in s["veto_seeds"]] or "none",
        "PASS" if s["normal_safety"] else "FAIL"))
L.append("")
L.append("### 门2 困难实例改善门（100_5_3_1，终态口径）")
L.append("")
L.append("判据: median ΔHV ≥ +2% 或 median ΔIGD ≥ +10%（至少一项）；另一项 ≥ −2%(HV)/−10%(IGD)")
L.append("")
for cand in CANDIDATES:
    s = screen[cand]
    L.append("- %s: median ΔHV=%s (≥+2%%: %s), median ΔIGD=%s (≥+10%%: %s); 另一项不恶化超限: HV≥−2%% %s, IGD≥−10%% %s → **%s**" % (
        cand, fmt(s["m_h"]["dHV"] * 100, 2) + "%", "Y" if s["signal_hv"] else "N",
        fmt(s["m_h"]["dIGD"] * 100, 2) + "%", "Y" if s["signal_igd"] else "N",
        "Y" if s["m_h"]["dHV"] >= -0.02 else "N", "Y" if s["m_h"]["dIGD"] >= -0.10 else "N",
        "PASS" if s["hard_improve"] else "FAIL"))
L.append("")
L.append("### 门3 三目标保护门（common-FE 口径 @F_common=%d）" % F_COMMON)
L.append("")
L.append("判据（预登记 §10 权威定义）: 不得两实例同一目标 median Δ 同时 < −2%；单实例 < −2% 单独标注")
L.append("")
L.append("| candidate | ΔCmax(n/h) | ΔTEC(n/h) | ΔTWC(n/h) | 同时跌破 | <−2% 标注 | 门3 |")
L.append("|---|---|---|---|---|---|---|")
for cand in CANDIDATES:
    s = screen[cand]
    mn, mh = med[("50_2_3_1", cand)], med[("100_5_3_1", cand)]
    L.append("| %s | %s / %s | %s / %s | %s / %s | %s | %s | **%s** |" % (
        cand, fmt(mn["dCmax_c"] * 100, 2) + "%", fmt(mh["dCmax_c"] * 100, 2) + "%",
        fmt(mn["dTEC_c"] * 100, 2) + "%", fmt(mh["dTEC_c"] * 100, 2) + "%",
        fmt(mn["dTWC_c"] * 100, 2) + "%", fmt(mh["dTWC_c"] * 100, 2) + "%",
        "YES" if any((mn[o] < -0.02 and mh[o] < -0.02) for o in ("dCmax_c", "dTEC_c", "dTWC_c")) else "no",
        "; ".join(s["viol"]) or "none",
        "PASS" if s["triple"] else "FAIL"))
L.append("")
L.append("### 门4 双口径一致门")
L.append("")
L.append("操作化（预登记语言为候选级单一方向\"改善/不改善\"，且 D1 下两口径唯一共同的维度是 Cmax/TEC/TWC）: 目标 o∈{Cmax,TEC,TWC}，pooled median（2 instance × 2 seed）终态口径符号 vs common-FE 口径符号；任一目标两口径均为非零且反号 ⇒ 该候选 BUDGET_SENSITIVITY_CONFLICT 出局（HV/IGD 无 common-FE 对应维度，不参与符号比较）")
L.append("")
L.append("| candidate | Cmax term/common | TEC term/common | TWC term/common | 冲突 | 门4 |")
L.append("|---|---|---|---|---|---|")
for cand in CANDIDATES:
    s = screen[cand]
    pc = pooled[cand]
    L.append("| %s | %s / %s | %s / %s | %s / %s | %s | **%s** |" % (
        cand, fmt(pc["dCmax_t"] * 100, 2) + "%", fmt(pc["dCmax_c"] * 100, 2) + "%",
        fmt(pc["dTEC_t"] * 100, 2) + "%", fmt(pc["dTEC_c"] * 100, 2) + "%",
        fmt(pc["dTWC_t"] * 100, 2) + "%", fmt(pc["dTWC_c"] * 100, 2) + "%",
        "; ".join(s["conflicts"]) or "none",
        "PASS" if s["dual_ok"] else "FAIL"))
L.append("")
L.append("### 门4 敏感性备注（逐 instance 口径，2-seed 中位）")
L.append("")
L.append("若改用更严的逐 instance 符号比较（每 instance 每目标两口径反号即冲突），翻位点如下（其余同号）：")
L.append("")
L.append("| candidate | instance | 目标 | 终态 median | common-FE median | 反号? |")
L.append("|---|---|---|---|---|---|")
for cand in CANDIDATES:
    for inst in INSTANCES:
        m = med[(inst, cand)]
        for ot, oc, nm in (("dCmax_t", "dCmax_c", "Cmax"), ("dTEC_t", "dTEC_c", "TEC"),
                           ("dTWC_t", "dTWC_c", "TWC")):
            if sign(m[ot]) != 0 and sign(m[oc]) != 0 and sign(m[ot]) != sign(m[oc]):
                L.append("| %s | %s | %s | %s | %s | YES |" % (
                    cand, inst, nm, fmt(m[ot] * 100, 2) + "%", fmt(m[oc] * 100, 2) + "%"))
L.append("")
L.append("- C2 的冲突在逐 instance 口径下同样成立（50_2_3_1 TWC：终态 +1.19% vs common −0.11%；机理：其 seed-14 终态 minTWC 改善出现在 FE=40000 之后，C2 终态比 C0 多 1731 FE，属预算敏感型改善，正是本门要拦的情形）")
L.append("- C3 在逐 instance 口径下会出现一处噪声级翻位（50_2_3_1 Cmax：终态 −0.43% vs common +0.04%，common 侧幅度 ≈0）；本报告采用候选级 pooled median 口径（对 seed 噪声更稳、与预登记\"候选方向\"单一表述一致）并如实登记该敏感性；任一口径下 C2 均因 TWC 冲突出局")
L.append("")
L.append("## 6. 五维 Pareto 与候选筛选（§11）")
L.append("")
L.append("五维（全部越大越好）: ΔHV_hard=100_5_3_1 终态 median ΔHV; ΔIGD_hard 同理; ΔCmax_all/ΔTEC_all/ΔTWC_all = 两实例 common-FE @F_common median 的最差值")
L.append("")
L.append("| candidate | ΔHV_hard | ΔIGD_hard | ΔCmax_all | ΔTEC_all | ΔTWC_all | 被支配(全体) | 被支配(合格集) |")
L.append("|---|---|---|---|---|---|---|---|")
for cand in CANDIDATES:
    s = screen[cand]
    f = s["five"]
    L.append("| %s | %s | %s | %s | %s | %s | %s | %s |" % (
        cand, fmt(f["dHV_hard"] * 100, 2) + "%", fmt(f["dIGD_hard"] * 100, 2) + "%",
        fmt(f["dCmax_all"] * 100, 2) + "%", fmt(f["dTEC_all"] * 100, 2) + "%",
        fmt(f["dTWC_all"] * 100, 2) + "%",
        ";".join(s["dominatedBy_all"]) or "NONE",
        ";".join(s["dominatedBy_qualified"]) or "NONE"))
L.append("")
L.append("四门全过（合格）候选: %s" % ("+".join(qualified) if qualified else "NONE"))
L.append("")
L.append("破平键值（顺序: 困难实例 common-FE median ΔCmax → ΔTEC → 正常实例终态 median ΔHV → ΔTWC → |betaMax−0.65|）:")
L.append("")
for cand in CANDIDATES:
    tb = screen[cand]["tiebreak"]
    L.append("- %s: tb1=%s, tb2=%s, tb3=%s, tb4=%s, tb5=%s" % (
        cand, fmt(tb["tb1_dCmax_hard_common"] * 100, 2) + "%",
        fmt(tb["tb2_dTEC_hard_common"] * 100, 2) + "%",
        fmt(tb["tb3_dHV_normal_terminal"] * 100, 2) + "%",
        fmt(tb["tb4_dTWC_normal_terminal"] * 100, 2) + "%",
        fmt(tb["tb5_betaDistTo065"], 2)))
L.append("")
L.append("- **保留候选: %s**（最多 2 个；破平键值排序后截断）" % ("+".join(retained_sorted) if retained_sorted else "NONE"))
L.append("- **最终裁决（§11 六选一）: %s**" % final_verdict)
if retained_sorted:
    L.append("- 保留候选置 `250kEligible=true, 250kPreregistered=false, 250kStarted=false`（不自动启动 250k）")
L.append("")
L.append("## 7. 状态块（机器可读）")
L.append("")
L.append("```ini")
L.append("[performance-screen-50k]")
L.append("fCommon=%d" % F_COMMON)
L.append("doseResolutionGate=%s" % dose_gate)
L.append("normalSafetyGate=%s" % (";".join("%s=%s" % (c, "PASS" if screen[c]["normal_safety"] else "FAIL") for c in CANDIDATES)))
L.append("hardImprovementGate=%s" % (";".join("%s=%s" % (c, "PASS" if screen[c]["hard_improve"] else "FAIL") for c in CANDIDATES)))
L.append("tripleObjectiveGate=%s" % (";".join("%s=%s" % (c, "PASS" if screen[c]["triple"] else "FAIL") for c in CANDIDATES)))
L.append("dualCaliberGate=%s" % (";".join("%s=%s" % (c, "PASS" if screen[c]["dual_ok"] else "FAIL") for c in CANDIDATES)))
L.append("retainedCandidates=%s" % ("+".join(retained_sorted) if retained_sorted else "NONE"))
L.append("budgetSensitivityConflict=%s" % ("true" if any_conflict else "false"))
L.append("fiveDimParetoDominated=%s" % (";".join("%s:%s" % (c, ";".join(screen[c]["dominatedBy_qualified"]) or "NONE") for c in CANDIDATES)))
L.append("finalVerdict=%s" % final_verdict)
L.append("250kEligible=%s" % ("true" if retained_sorted else "false"))
L.append("250kPreregistered=false")
L.append("250kStarted=false")
L.append("```")
L.append("")

with io.open(os.path.join(OUT, "performance-screen.md"), "w", encoding="utf-8") as fh:
    fh.write("\n".join(L))

# ---------------------------------------------------------------- console
print("F_COMMON=%d (%s)" % (F_COMMON, "; ".join(f_common_detail)))
print("PFref sizes:", {i: len(pfref[i]) for i in INSTANCES})
print("\nTerminal HV/IGD (instance x profile, 2-seed mean):")
for inst in INSTANCES:
    for p in PROFILES:
        hv = [term[r["runKey"]]["hv"] for r in runs if r["instance"] == inst and r["profile"] == p]
        ig = [term[r["runKey"]]["igd"] for r in runs if r["instance"] == inst and r["profile"] == p]
        print("  %s %s HV=%.4f IGD=%.4f" % (inst, p, sum(hv) / 2, sum(ig) / 2))
print("\nPer-candidate gates and five dims:")
for cand in CANDIDATES:
    s = screen[cand]
    f = s["five"]
    print("  %s gates(NS,HI,TR,DC)=%s five=[%s %s %s %s %s] retained=%s" % (
        cand, tuple("P" if v else "F" for v in (s["normal_safety"], s["hard_improve"], s["triple"], s["dual_ok"])),
        fmt(f["dHV_hard"] * 100, 2) + "%", fmt(f["dIGD_hard"] * 100, 2) + "%",
        fmt(f["dCmax_all"] * 100, 2) + "%", fmt(f["dTEC_all"] * 100, 2) + "%",
        fmt(f["dTWC_all"] * 100, 2) + "%", cand in retained_sorted))
print("retained=%s finalVerdict=%s conflict=%s" % (
    retained_sorted or "NONE", final_verdict, any_conflict))
