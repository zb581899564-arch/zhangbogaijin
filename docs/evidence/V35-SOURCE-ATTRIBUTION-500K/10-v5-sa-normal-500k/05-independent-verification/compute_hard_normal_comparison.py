#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V35-SOURCE-ATTRIBUTION-500K / 10-v5-sa-normal-500k — HARD vs NORMAL comparison & G1/G3 verdict.

Zero FE, read-only on all frozen artifacts. Reuses the FROZEN Phase A0 contract primitives
(fc6_metrics + V35-PFC5-PHASE0/tools/build_reference_contract) — no metric rebuilt, no
threshold changed, no reference reconstructed.

Inputs (both already accepted):
  HARD  : 09-v5-sa-hard-500k/05-hard-source-analysis/source-window-metrics.csv  (life_* attributed)
          09-v5-sa-hard-500k/02-remote-run/results/SA-HARD-V5-500k/checkpoints/
          F1 reference contract (PFref 757 pts, ideal/nadir, gold anchors)
  NORMAL: 10-v5-sa-normal-500k/04-hard-normal-analysis/source-window-metrics.csv (life_* attributed)
          10-v5-sa-normal-500k/02-remote-run/results/SA-NORMAL-V5-500k/checkpoints/
          unified reference front (1979 pts; anti-backfill frozen)

Outputs (10-v5-sa-normal-500k/04-hard-normal-analysis/):
  hard-normal-window-comparison.csv   per (window, source): WHVG/ExclusiveND metrics + deficits + qualify flags
  source-survival-comparison.csv      per source: lifecycle totals + rates; PA/QP NOT_ATTRIBUTABLE registered
  coverage-divergence.csv             per checkpoint i=0..20: HV/IGD under each run's own PFref; t_div
  g1-g3-decision-matrix.csv           per source: firstPersistentWindow, timing, survival competition, FE materiality, verdict
  hard-normal-decision.properties     final G1/G3/SOURCE_LEVER_CANDIDATE + t_div + stop boundary
"""
import csv
import io
import os
import sys

ROOT = "E:/学习/李明哲-毕业材料/张博改进"
EVID = os.path.join(ROOT, "docs/evidence/V35-SOURCE-ATTRIBUTION-500K")
HARD = os.path.join(EVID, "09-v5-sa-hard-500k")
NORM = os.path.join(EVID, "10-v5-sa-normal-500k")
OUT = os.path.join(NORM, "04-hard-normal-analysis")
PHASE0 = os.path.normpath(os.path.join(EVID, "..", "V35-PFC5-PHASE0"))
sys.path.insert(0, os.path.join(ROOT, "scripts"))
sys.path.insert(0, os.path.join(PHASE0, "tools"))
import fc6_metrics as fc6  # frozen primitives
import build_reference_contract as B  # frozen exact copy (read_front/normalize/hypervolume/igd/nondominated)

EPS = fc6.EPS
WINDOW_FE = 25000
SOURCES = ["GLOBAL_CFVF", "CATA", "INHERITED_LS", "PARENT_CARRYOVER"]
T_HV = 2.0   # WHVGShare deficit threshold (pp), frozen Phase A0 fallback
T_ND = 10.0  # ExclusiveNDShare deficit threshold (pp), frozen Phase A0 fallback
CONSEC = 2
T_DIV_HV = 1.0    # hvProgress deficit (pp), frozen
T_DIV_IGD = 10.0  # igdRelImp deficit (pp), frozen
CATA_MATERIALITY_FE_SHARE = 0.05  # frozen materiality gate (P1 campaign, 5%)

# frozen reference materials
HARD_CONTRACT = os.path.join(PHASE0, "04-reference-contract", "reference-contract.properties")
HARD_PFREF = os.path.join(PHASE0, "04-reference-contract", "pfref-100_5_3_1.csv")
NORMAL_PFREF = os.path.join(ROOT, "docs/evidence/V35-STAGE2-PILOT-A0-A4-20260823",
                           "results/reference-front.csv")
HARD_CKPT = os.path.join(HARD, "02-remote-run/results/SA-HARD-V5-500k/checkpoints")
HARD_FRONT = os.path.join(HARD, "02-remote-run/results/SA-HARD-V5-500k/front.csv")
NORM_CKPT = os.path.join(NORM, "02-remote-run/results/SA-NORMAL-V5-500k/checkpoints")
NORM_FRONT = os.path.join(NORM, "02-remote-run/results/SA-NORMAL-V5-500k/front.csv")


def read_props(path):
    p = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                p[k.strip()] = v.strip()
    return p


def hv_igd(front_raw, pfref_nd_raw, ref_norm):
    """fc6 corrected: front ND -> normalize by the frozen PFref ideal/nadir (no clamp) -> HV/IGD.
    pfref_nd_raw = raw strict-ND of the frozen PFref (its min/max = ideal/nadir, never rebuilt);
    ref_norm = fc6.normalize(pfref_nd_raw, pfref_nd_raw, clamp=False) for the IGD reference set."""
    nd = fc6.nondominated([list(x) for x in front_raw])
    if not nd:
        return 0.0, 0.0
    a_norm = fc6.normalize(nd, pfref_nd_raw, clamp=False)
    return fc6.hypervolume(a_norm), fc6.igd(a_norm, ref_norm)


def load_reference(pfref_path):
    pfref = B.read_front(pfref_path)
    pfref_nd = fc6.nondominated([list(x) for x in pfref])           # raw strict-ND (ideal/nadir source)
    ref_norm = fc6.normalize(pfref_nd, pfref_nd, clamp=False)      # IGD reference set
    return pfref, pfref_nd, ref_norm


def front_for_checkpoint(ckpt_dir, top_front, i):
    """i=0 -> B0; i=1..19 -> checkpoint i*25000; i=20 -> terminal (top-level front.csv)."""
    if i == 0:
        p = os.path.join(ckpt_dir, "checkpoint-0-decision-front.csv")
    elif i == 20:
        p = top_front
    else:
        p = os.path.join(ckpt_dir, f"checkpoint-{i * WINDOW_FE}-decision-front.csv")
    if not os.path.exists(p):
        return None
    rows = []
    with open(p, encoding="utf-8") as f:
        r = csv.reader(f)
        hdr = next(r)
        oi = [hdr.index(c) for c in ("Cmax", "TEC", "TWC")] if "Cmax" in hdr else [0, 1, 2]
        for row in r:
            rows.append((float(row[oi[0]]), float(row[oi[1]]), float(row[oi[2]])))
    return rows


def main():
    os.makedirs(OUT, exist_ok=True)

    # ---- reference contracts (frozen, not rebuilt) ----
    hprops = read_props(HARD_CONTRACT)
    h_pfref, h_pfref_nd, h_ref_norm = load_reference(HARD_PFREF)
    n_pfref, n_pfref_nd, n_ref_norm = load_reference(NORMAL_PFREF)
    h_points = len(h_pfref)
    n_points = len(n_pfref)

    # ---- coverage divergence (per checkpoint HV/IGD + t_div) ----
    ckpt_rows = []
    h_prev = n_prev = None
    lag_flags = []
    for i in range(0, 21):
        nom_fe = i * WINDOW_FE
        h_front = front_for_checkpoint(HARD_CKPT, HARD_FRONT, i)
        n_front = front_for_checkpoint(NORM_CKPT, NORM_FRONT, i)
        h_hv, h_igd = hv_igd(h_front, h_pfref_nd, h_ref_norm) if h_front else (None, None)
        n_hv, n_igd = hv_igd(n_front, n_pfref_nd, n_ref_norm) if n_front else (None, None)
        # hvProgress / igdRelImp (baseline i=0 = B0)
        if i == 0:
            h_hv0, h_igd0, n_hv0, n_igd0 = h_hv, h_igd, n_hv, n_igd
        h_hvp = 100.0 * (h_hv - h_prev[0]) / max(h_prev[0], EPS) if (i > 0 and h_prev and h_prev[0] is not None and h_hv is not None) else None
        h_igdr = 100.0 * (h_prev[1] - h_igd) / max(h_prev[1], EPS) if (i > 0 and h_prev and h_prev[1] is not None and h_igd is not None) else None
        n_hvp = 100.0 * (n_hv - n_prev[0]) / max(n_prev[0], EPS) if (i > 0 and n_prev and n_prev[0] is not None and n_hv is not None) else None
        n_igdr = 100.0 * (n_prev[1] - n_igd) / max(n_prev[1], EPS) if (i > 0 and n_prev and n_prev[1] is not None and n_igd is not None) else None
        dHVP = (n_hvp - h_hvp) if (n_hvp is not None and h_hvp is not None) else None
        dIGDR = (n_igdr - h_igdr) if (n_igdr is not None and h_igdr is not None) else None
        lag = (dHVP is not None and dIGDR is not None and dHVP >= T_DIV_HV and dIGDR >= T_DIV_IGD)
        lag_flags.append(lag if i > 0 else False)
        ckpt_rows.append({
            "checkpointIndex": i, "nominalFE": nom_fe,
            "hardHV": repr(h_hv) if h_hv is not None else "", "hardIGD": repr(h_igd) if h_igd is not None else "",
            "normalHV": repr(n_hv) if n_hv is not None else "", "normalIGD": repr(n_igd) if n_igd is not None else "",
            "hardHvProgressPct": repr(h_hvp) if h_hvp is not None else "",
            "hardIgdRelImpPct": repr(h_igdr) if h_igdr is not None else "",
            "normalHvProgressPct": repr(n_hvp) if n_hvp is not None else "",
            "normalIgdRelImpPct": repr(n_igdr) if n_igdr is not None else "",
            "deficitHvProgressPp": repr(dHVP) if dHVP is not None else "",
            "deficitIgdRelImpPp": repr(dIGDR) if dIGDR is not None else "",
            "lagConditionMet": str(lag) if i > 0 else "baseline",
        })
        h_prev = (h_hv, h_igd)
        n_prev = (n_hv, n_igd)
    # t_div = earliest i with lag(i) and lag(i+1), i in 1..19
    t_div = None
    for i in range(1, 20):
        if lag_flags[i] and lag_flags[i + 1] if i + 1 < len(lag_flags) else False:
            t_div = i
            break
    t_div_str = str(t_div) if t_div is not None else "NOT_REACHED"

    cols = list(ckpt_rows[0].keys()) + ["t_div_note"]
    with io.open(os.path.join(OUT, "coverage-divergence.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=cols, lineterminator="\n")
        w.writeheader()
        for r in ckpt_rows:
            r["t_div_note"] = (f"t_div={t_div_str} (earliest i with lag(i)&lag(i+1))" if r["checkpointIndex"] == 1 else "")
            w.writerow(r)

    # ---- window metrics comparison ----
    hw = list(csv.DictReader(io.open(os.path.join(HARD, "05-hard-source-analysis", "source-window-metrics.csv"), encoding="utf-8")))
    nw = list(csv.DictReader(io.open(os.path.join(OUT, "source-window-metrics.csv"), encoding="utf-8")))
    h_by = {(int(r["window"]), r["source"]): r for r in hw}
    n_by = {(int(r["window"]), r["source"]): r for r in nw}
    comp_rows = []
    # qualify tracking for firstPersistentWindow
    qual = {("WHVGShare", s): [] for s in SOURCES}
    qual.update({("ExclusiveNDShare", s): [] for s in SOURCES})
    for t in range(1, 21):
        for s in SOURCES:
            h = h_by.get((t, s), {})
            n = n_by.get((t, s), {})
            def f(d, k, default="0"):
                return float(d.get(k, default)) if d.get(k, default) not in ("", None) else 0.0
            d_whvg = f(n, "WHVGSharePct") - f(h, "WHVGSharePct")
            d_exnd = f(n, "exclusiveNDSharePct") - f(h, "exclusiveNDSharePct")
            q_hv = d_whvg >= T_HV
            q_nd = d_exnd >= T_ND
            qual[("WHVGShare", s)].append(q_hv)
            qual[("ExclusiveNDShare", s)].append(q_nd)
            comp_rows.append({
                "window": t, "source": s,
                "hard_nEvaluated": h.get("nEvaluated", ""), "normal_nEvaluated": n.get("nEvaluated", ""),
                "hard_nUniqueObjectives": h.get("nUniqueObjectives", ""), "normal_nUniqueObjectives": n.get("nUniqueObjectives", ""),
                "hard_nTuplesExclusive": h.get("nTuplesExclusive", ""), "normal_nTuplesExclusive": n.get("nTuplesExclusive", ""),
                "hard_nexclND": h.get("nexclND", ""), "normal_nexclND": n.get("nexclND", ""),
                "hard_exclusiveNDSharePct": h.get("exclusiveNDSharePct", ""), "normal_exclusiveNDSharePct": n.get("exclusiveNDSharePct", ""),
                "deficit_exclusiveNDSharePp": repr(d_exnd),
                "hard_WHVGSharePct": h.get("WHVGSharePct", ""), "normal_WHVGSharePct": n.get("WHVGSharePct", ""),
                "deficit_WHVGSharePp": repr(d_whvg),
                "qualify_WHVGShare": str(q_hv), "qualify_ExclusiveNDShare": str(q_nd),
            })
    with io.open(os.path.join(OUT, "hard-normal-window-comparison.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(comp_rows[0].keys()), lineterminator="\n")
        w.writeheader()
        w.writerows(comp_rows)

    # firstPersistentWindow per (metric, source): earliest t with qualify[t] and qualify[t+1]
    fpw = {}
    for (metric, s), flags in qual.items():
        first = None
        for t in range(len(flags) - 1):
            if flags[t] and flags[t + 1]:
                first = t + 1
                break
        fpw[(metric, s)] = first

    # ---- survival comparison ----
    hsum = list(csv.DictReader(io.open(os.path.join(HARD, "05-hard-source-analysis", "source-lifecycle-summary.csv"), encoding="utf-8")))
    nsum = list(csv.DictReader(io.open(os.path.join(OUT, "source-lifecycle-summary.csv"), encoding="utf-8")))
    hsum_by = {r["source"]: r for r in hsum}
    nsum_by = {r["source"]: r for r in nsum}
    surv_rows = []
    for s in SOURCES:
        h = hsum_by.get(s, {})
        n = nsum_by.get(s, {})
        surv_rows.append({
            "source": s,
            "hard_nEvaluated": h.get("nEvaluated", ""), "normal_nEvaluated": n.get("nEvaluated", ""),
            "hard_MERGE_POOL": h.get("MERGE_POOL", ""), "normal_MERGE_POOL": n.get("MERGE_POOL", ""),
            "hard_PDDR_SELECTED": h.get("PDDR_SELECTED", ""), "normal_PDDR_SELECTED": n.get("PDDR_SELECTED", ""),
            "hard_WORKING_POPULATION": h.get("WORKING_POPULATION", ""), "normal_WORKING_POPULATION": n.get("WORKING_POPULATION", ""),
            "hard_QG_TEACHER": h.get("QG_TEACHER", ""), "normal_QG_TEACHER": n.get("QG_TEACHER", ""),
            "hard_PERSONAL_ARCHIVE": h.get("PERSONAL_ARCHIVE", ""), "normal_PERSONAL_ARCHIVE": n.get("PERSONAL_ARCHIVE", ""),
            "hard_IMPROVING_DESCENDANT": h.get("IMPROVING_DESCENDANT", ""), "normal_IMPROVING_DESCENDANT": n.get("IMPROVING_DESCENDANT", ""),
            "hard_mergeToPddrRate": h.get("mergeToPddrRate", ""), "normal_mergeToPddrRate": n.get("mergeToPddrRate", ""),
            "hard_pddrToWorkingRate": h.get("pddrToWorkingRate", ""), "normal_pddrToWorkingRate": n.get("pddrToWorkingRate", ""),
            "hard_generatedToPddrRate": h.get("generatedToPddrRate", ""), "normal_generatedToPddrRate": n.get("generatedToPddrRate", ""),
            "hard_teacherEventsPerGenerated": h.get("teacherEventsPerGenerated", ""), "normal_teacherEventsPerGenerated": n.get("teacherEventsPerGenerated", ""),
            "PA_QP_attribution": "NOT_ATTRIBUTABLE_BY_FINGERPRINT_JOIN for PERSONAL_ARCHIVE/QP_TEACHER/QP_ACTION (registered, never guessed)",
        })
    with io.open(os.path.join(OUT, "source-survival-comparison.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(surv_rows[0].keys()), lineterminator="\n")
        w.writeheader()
        w.writerows(surv_rows)

    # ---- G1/G3 decision matrix ----
    def first_persistent(metric, s):
        v = fpw.get((metric, s))
        return v if v is not None else "NONE"

    t_div_int = t_div if t_div is not None else None
    decision_rows = []
    for s in SOURCES:
        fpw_hv = first_persistent("WHVGShare", s)
        fpw_nd = first_persistent("ExclusiveNDShare", s)
        timing_ok_hv = (isinstance(fpw_hv, int) and t_div_int is not None and fpw_hv <= t_div_int)
        timing_ok_nd = (isinstance(fpw_nd, int) and t_div_int is not None and fpw_nd <= t_div_int)
        # survival competition: G1 requires the deficit NOT be explainable by a stronger
        # PDDR/working survival anomaly. Operationalize: hard vs normal mergeToPddr/pddrToWorking
        # are similar (retention not the differentiator) => survival explanation fails => attribution stands.
        h = hsum_by.get(s, {})
        n = nsum_by.get(s, {})
        h_mp = float(h.get("mergeToPddrRate", "0") or 0)
        n_mp = float(n.get("mergeToPddrRate", "0") or 0)
        h_pw = float(h.get("pddrToWorkingRate", "0") or 0)
        n_pw = float(n.get("pddrToWorkingRate", "0") or 0)
        survival_anomaly_competes = (abs(n_mp - h_mp) >= 10.0 or abs(n_pw - h_pw) >= 10.0)
        # FE materiality for G3 (CATA must consume substantial local FE; 5% gate)
        h_fe = int(h.get("nEvaluated", "0") or 0)
        n_fe = int(n.get("nEvaluated", "0") or 0)
        cata_material = (s == "CATA" and (h_fe / 500000 >= CATA_MATERIALITY_FE_SHARE or n_fe / 500000 >= CATA_MATERIALITY_FE_SHARE))
        # verdict per source
        if s == "GLOBAL_CFVF":
            triggered = (isinstance(fpw_hv, int) or isinstance(fpw_nd, int)) and timing_ok_hv and not survival_anomaly_competes
            verdict = "TRIGGERED" if triggered else ("INSUFFICIENT" if (isinstance(fpw_hv, int) or isinstance(fpw_nd, int)) else "NOT_TRIGGERED")
        elif s == "CATA":
            triggered = (isinstance(fpw_hv, int) or isinstance(fpw_nd, int)) and timing_ok_hv and not survival_anomaly_competes and cata_material
            verdict = "TRIGGERED" if triggered else ("INSUFFICIENT" if (isinstance(fpw_hv, int) or isinstance(fpw_nd, int)) else "NOT_TRIGGERED")
        else:
            verdict = "N/A (not a G1/G3 lever candidate)"
        decision_rows.append({
            "source": s,
            "firstPersistentWindow_WHVGShare": fpw_hv,
            "firstPersistentWindow_ExclusiveNDShare": fpw_nd,
            "t_div": t_div_str,
            "timingOk_firstPersistent_le_t_div": str(timing_ok_hv or timing_ok_nd),
            "survivalAnomalyCompetes": str(survival_anomaly_competes),
            "cataFEMaterialityMet": str(cata_material),
            "verdict": verdict,
        })
    with io.open(os.path.join(OUT, "g1-g3-decision-matrix.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(decision_rows[0].keys()), lineterminator="\n")
        w.writeheader()
        w.writerows(decision_rows)

    # ---- final decision properties ----
    g1_row = next(r for r in decision_rows if r["source"] == "GLOBAL_CFVF")
    g3_row = next(r for r in decision_rows if r["source"] == "CATA")
    g1 = "TRIGGERED" if g1_row["verdict"] == "TRIGGERED" else ("INSUFFICIENT" if "INSUFFICIENT" in g1_row["verdict"] else "NOT_TRIGGERED")
    g3 = "TRIGGERED" if g3_row["verdict"] == "TRIGGERED" else ("INSUFFICIENT" if "INSUFFICIENT" in g3_row["verdict"] else "NOT_TRIGGERED")
    lever = "GLOBAL_CFVF" if g1 == "TRIGGERED" else ("CATA" if g3 == "TRIGGERED" else "NONE")
    a2_eligible = "true" if g1 == "TRIGGERED" else "false"
    with io.open(os.path.join(OUT, "hard-normal-decision.properties"), "w", encoding="utf-8") as f:
        f.write("# V35-SOURCE-ATTRIBUTION-500K / 10-v5-sa-normal-500k HARD-NORMAL decision (frozen Phase A0 contract)\n")
        f.write(f"hardReferenceContract={read_props(HARD_CONTRACT).get('contractSha256', '') or os.path.basename(HARD_CONTRACT)}\n")
        f.write(f"hardPfrefPoints={h_points}\n")
        f.write(f"normalReferenceFrontPoints={n_points}\n")
        f.write(f"t_div={t_div_str}\n")
        f.write(f"G1_GLOBAL_CFVF={g1}\n")
        f.write(f"G3_CATA={g3}\n")
        f.write(f"SOURCE_LEVER_CANDIDATE={lever}\n")
        f.write(f"SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false\n")
        f.write(f"SA_A2_CONDITIONAL_ELIGIBLE={a2_eligible}\n")
        f.write(f"SA_A2_CONDITIONAL_STARTED=false\n")
        f.write("T_HV=2.0\nT_ND=10.0\nconsecutiveWindows=2\n")
        f.write("tDivHvProgressDeficitPp=1.0\ntDivIgdRelativeImprovementDeficitPp=10.0\n")
        f.write("firstAdmissionPolicy=DESCRIPTIVE_ONLY (never gating)\n")
        f.write("multiSourceCounterfactual=SHARED_POINTS_CONTRIBUTE_TO_NO_SINGLE_SOURCE\n")
        f.write("normalReferenceBackfillPolicy=FORBIDDEN\n")
        f.write("DOE_AUTHORIZED=false\nQP_V2_AUTHORIZED=false\nCONFIG_RACE_AUTHORIZED=false\n")
        f.write("VALIDATION_AUTHORIZED=false\nFORMAL_AUTHORIZED=false\nformalMatrixRunning=false\n")
        f.write("PDDRChanged=false\nCFVFChanged=false\nDualQChanged=false\nCaTaChanged=false\nformalJarChanged=false\n")
        f.write("consumedFE=0 (this analysis)\n")
    print(f"t_div={t_div_str} G1={g1} G3={g3} SOURCE_LEVER_CANDIDATE={lever} A2_eligible={a2_eligible}")
    for r in decision_rows:
        print(f"  {r['source']}: fpw_WHVG={r['firstPersistentWindow_WHVGShare']} fpw_ExND={r['firstPersistentWindow_ExclusiveNDShare']} verdict={r['verdict']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
