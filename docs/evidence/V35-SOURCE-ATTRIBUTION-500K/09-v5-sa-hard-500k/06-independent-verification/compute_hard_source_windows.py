#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V35-SOURCE-ATTRIBUTION-500K / 09-v5-sa-hard-500k — HARD-side per-window source metrics.

Reuses the FROZEN Phase A0 contract primitives verbatim:
  - scripts/fc6_metrics.py            (dominates/equal/unique/normalize/hypervolume — never rebuilt)
  - 00-preregistration/threshold_recompute.py (canonical_groups/producer_set/first_admission_source
    semantics, window event slicing, Fpast=B_{t-1} decision front)

The ONLY addition is a vectorized exact-output accelerator for the two O(n^2) hot paths
(fc6.nondominated over ~25k-point window unions, and 1e-12 equality folding), which replays
the IDENTICAL float64 operations of fc6.dominates/fc6.equal in numpy. Exact-output equivalence
is verified in-process (V1 randomized ND tests, V2 subsampled-window equivalence against the
frozen threshold_recompute.window_metrics, V3 per-window contract sanity) and any failure is
fail-closed.

HARD-side only: no G1/G3 verdict is possible without SA-NORMAL (frozen plan §10); outputs are
descriptive budget shares + observational mechanism evidence only.
"""
import csv
import io
import os
import random
import sys

import numpy as np

ROOT = "E:/学习/李明哲-毕业材料/张博改进"
PKG = os.path.join(ROOT, "docs/evidence/V35-SOURCE-ATTRIBUTION-500K/09-v5-sa-hard-500k")
RES = os.path.join(PKG, "02-remote-run/results/SA-HARD-V5-500k")
CKPT = os.path.join(RES, "checkpoints")
OUT = os.path.join(PKG, "05-hard-source-analysis")
sys.path.insert(0, os.path.join(ROOT, "scripts"))
sys.path.insert(0, os.path.join(ROOT, "docs/evidence/V35-SOURCE-ATTRIBUTION-500K/00-preregistration"))

import fc6_metrics as fc6  # frozen primitives (dominates/equal/unique/normalize/hypervolume)
import threshold_recompute as TR  # frozen window semantics (EPS, WINDOW_FE, group/producer helpers)

EPS = fc6.EPS
WINDOW_FE = TR.WINDOW_FE
SOURCES = ["GLOBAL_CFVF", "CATA", "INHERITED_LS", "PARENT_CARRYOVER"]
RAW_TO_PRIMARY = {"GLOBAL_CFVF": "GLOBAL_CFVF", "CATA": "CATA", "INHERITED_LS": "INHERITED_LS",
                  "NOT_APPLICABLE": "PARENT_CARRYOVER"}  # NOT_APPLICABLE rows are exactly the
# 100 INITIAL_POPULATION rows (rawSource=INITIAL_POPULATION, attributionEligible=false);
# taxonomy maps INITIAL_POPULATION -> PARENT_CARRYOVER.


# ---------------------------------------------------------------- fast exact helpers
def nd_fast(points):
    """Exact-output replica of fc6.nondominated: same dominance arithmetic (fc6.dominates
    replayed in float64 numpy), same sort, same adjacent-1e-12 folding via frozen fc6.unique."""
    arr = np.asarray(points, dtype=np.float64)
    n = arr.shape[0]
    if n == 0:
        return []
    dominated = np.zeros(n, dtype=bool)
    q = arr
    chunk = max(1, int(4_000_000 // max(n, 1)))
    for lo in range(0, n, chunk):
        p = arr[lo:lo + chunk]                      # m x 3
        diff = q[None, :, :] - p[:, None, :]        # m x n x 3  (q - p)
        le = diff <= EPS                            # q[i] <= p[i] + EPS
        strict = (-diff) > EPS                      # q[i] + EPS < p[i]
        dom = (le.all(axis=2) & strict.any(axis=2))  # m x n
        dominated[lo:lo + chunk] = dom.any(axis=1)
    keep = arr[~dominated]
    result = [list(map(float, p)) for p in keep]
    result.sort()
    return fc6.unique(result)


def _ulp_gt_2eps(vals):
    """True iff nextafter(x) - x > 2*EPS for every value (then |a-b|<=EPS ⟹ bitwise equal)."""
    arr = np.asarray(vals, dtype=np.float64)
    if arr.size == 0:
        return True
    nxt = np.nextafter(arr, np.inf)
    return bool((nxt - arr > 2 * EPS).all())


def fold_groups_fast(events):
    """Exact-output replica of TR.canonical_groups.

    TR semantics: sequential first-match folding over the lexicographically sorted unique
    reps — fc6.equal is NOT transitive, so this is NOT connected-components. Exact fast path:
    |a.TEC-b.TEC|<=EPS ⟹ bitwise equality when ULP(TEC)>2*EPS at every value (ULP-guarded,
    fail-closed to the frozen slow path otherwise); same for TWC. Then equal(a,b) reduces to
    same (TEC,TWC) bucket ∧ |a.Cmax-b.Cmax|<=EPS, and the sequential first-match scan is run
    exactly (global sorted rep order, group creation order) within each tiny bucket.
    """
    by_exact = {}
    for e in events:
        by_exact.setdefault(tuple(e["objectives"]), []).append(e)
    reps = sorted(by_exact.keys())
    if not reps:
        return []
    if not (_ulp_gt_2eps([r[1] for r in reps]) and _ulp_gt_2eps([r[2] for r in reps])):
        return TR.canonical_groups(events)  # frozen slow path, fail-closed
    groups = []                              # [(rep, [events...])] in creation order
    bucket_groups = {}                       # (tec,twc) -> [group indices in creation order]
    for r in reps:
        key = (r[1], r[2])
        glist = bucket_groups.get(key, [])
        target = None
        for gi in glist:
            if abs(groups[gi][0][0] - r[0]) <= EPS:
                target = gi
                break
        if target is None:
            groups.append((r, []))
            glist.append(len(groups) - 1)
            bucket_groups[key] = glist
            target = len(groups) - 1
        groups[target][1].extend(by_exact[r])
    return groups


def any_equal_to(rows, targets):
    """rows[m x 3] vs targets[k x 3]: boolean per row = exists target with ALL coords
    within EPS (fc6.equal semantics) — NOT single-coordinate proximity."""
    if not len(targets) or not len(rows):
        return np.zeros(len(rows), dtype=bool)
    a = np.asarray(rows, dtype=np.float64)
    b = np.asarray(targets, dtype=np.float64)
    out = np.zeros(len(a), dtype=bool)
    chunk = max(1, int(4_000_000 // max(len(b), 1)))
    for lo in range(0, len(a), chunk):
        d = np.abs(a[lo:lo + chunk, None, :] - b[None, :, :])
        out[lo:lo + chunk] = (d <= EPS).all(axis=2).any(axis=1)
    return out


CW_ANY_EQ = any_equal_to  # captured for the V1d regression test


def hv_normalized_fast(points_raw, anchor_nd):
    """fc6 exact: ND -> normalize(anchor) -> hypervolume. ND computed by nd_fast."""
    nd = nd_fast(points_raw)
    if not nd:
        return 0.0
    anchor_nd2 = fc6.nondominated([list(x) for x in anchor_nd])  # tiny; frozen path
    return fc6.hypervolume(fc6.normalize(nd, anchor_nd2, clamp=False))


# ---------------------------------------------------------------- window metrics
def window_metrics_fast(fpast, events):
    """Mirror of TR.window_metrics with exact-output fast paths. Same return dict."""
    fpast_nd = nd_fast(fpast)
    groups = fold_groups_fast(events)
    if not groups:
        return {"hvAll": 0.0, "perSource": {}, "nndAll": 0, "emptyWindow": True,
                "producerSets": {}, "firstAdmission": {}, "whvgSumMinusTotalGain": 0.0}
    wt_reps = [g[0] for g in groups]
    producer_sets = {g[0]: TR.producer_set(g) for g in groups}
    first_admission = {g[0]: TR.first_admission_source(g) for g in groups}
    union_all = fpast_nd + wt_reps
    anchor = nd_fast(union_all)
    hv_all = hv_normalized_fast(union_all, anchor)
    all_sources = sorted(set().union(*[set(ps) for ps in producer_sets.values()]))
    # nd_new_reps: new vs Fpast AND member of anchor
    reps_arr = np.asarray(wt_reps, dtype=np.float64)
    is_new = ~any_equal_to(reps_arr, fpast_nd)
    in_anchor = any_equal_to(reps_arr, anchor)
    nd_new_mask = is_new & in_anchor
    nd_new_reps = [wt_reps[i] for i in np.nonzero(nd_new_mask)[0]]
    nnd_all = len(nd_new_reps)
    per_source = {}
    for s in all_sources:
        minus_reps = [r for r in wt_reps if producer_sets[r] != frozenset([s])]
        hv_minus_s = hv_normalized_fast(fpast_nd + minus_reps, anchor)
        whvg = hv_all - hv_minus_s
        share = 100.0 * whvg / max(hv_all, EPS)
        nexcl = sum(1 for r in nd_new_reps if producer_sets[r] == frozenset([s]))
        per_source[s] = {
            "nTuplesProduced": sum(1 for r in wt_reps if s in producer_sets[r]),
            "nTuplesExclusive": sum(1 for r in wt_reps if producer_sets[r] == frozenset([s])),
            "whvg": whvg,
            "whvgSharePct": share,
            "nexclND": nexcl,
            "exclusiveNdSharePct": 100.0 * nexcl / max(nnd_all, 1),
        }
    hv_minus_all = hv_normalized_fast(fpast_nd, anchor)
    partition_residual = (sum(per_source[s]["whvg"] for s in all_sources)
                          - (hv_all - hv_minus_all))
    return {"hvAll": hv_all, "perSource": per_source, "nndAll": nnd_all,
            "emptyWindow": False, "producerSets": {k: sorted(v) for k, v in producer_sets.items()},
            "firstAdmission": first_admission, "whvgSumMinusTotalGain": partition_residual}


# ---------------------------------------------------------------- verification
def verify():
    rnd = random.Random(20260901)
    # V1: randomized ND equivalence vs frozen fc6.nondominated
    for t in range(120):
        n = rnd.randint(2, 400)
        base = [rnd.uniform(700, 1400), rnd.uniform(110000, 140000), rnd.uniform(300000, 430000)]
        pts = []
        for _ in range(n):
            p = [base[0] + rnd.choice([0.0, rnd.uniform(-8, 8), rnd.uniform(-2e-13, 2e-13)]),
                 base[1] + rnd.choice([0.0, rnd.uniform(-3000, 3000)]),
                 base[2] + rnd.choice([0.0, rnd.uniform(-9000, 9000)])]
            pts.append(tuple(p))
        pts += pts[: max(1, n // 10)]  # duplicates
        a = nd_fast(pts)
        b = fc6.nondominated([list(p) for p in pts])
        if a != b:
            print("V1 FAIL at trial", t)
            return False
    # V1b: duplicate-heavy + exact-equal clusters
    pts = [(1.0, 5.0, 9.0), (1.0, 5.0, 9.0), (1.0 + 5e-13, 5.0, 9.0), (2.0, 4.0, 9.0), (0.5, 6.0, 10.0)]
    if nd_fast(pts) != fc6.nondominated([list(p) for p in pts]):
        print("V1b FAIL")
        return False
    print("V1 nd_fast==fc6.nondominated on 121 randomized/duplicate cases: PASS")
    # V1c: fold_groups_fast == TR.canonical_groups on randomized event sets
    # (includes ULP-adjacent Cmax clusters that exercise non-transitive 1e-12 equality)
    for t in range(60):
        n = rnd.randint(5, 300)
        evs = []
        c0, tec0, twc0 = 900.0, 120000.0, 350000.0
        for i in range(n):
            c = c0 + rnd.choice([0.0, rnd.uniform(-50, 50), rnd.uniform(-4e-13, 4e-13)])
            tec = tec0 + rnd.choice([0.0, rnd.uniform(-2000, 2000)])
            twc = twc0 + rnd.choice([0.0, rnd.uniform(-6000, 6000)])
            evs.append({"source": rnd.choice(["A", "B", "C"]), "nominalFE": i,
                        "actualFE": i, "candidateId": str(i),
                        "objectives": (c, tec, twc)})
        a = fold_groups_fast(evs)
        b = TR.canonical_groups(evs)
        if [g[0] for g in a] != [g[0] for g in b] or \
               [len(g[1]) for g in a] != [len(g[1]) for g in b] or \
               [sorted(e["candidateId"] for e in g[1]) for g in a] != \
               [sorted(e["candidateId"] for e in g[1]) for g in b]:
            print("V1c FAIL at trial", t, len(a), len(b))
            return False
    print("V1c fold_groups_fast==TR.canonical_groups on 60 randomized cases: PASS")
    # V1d: any_equal_to regression (single-coordinate proximity must NOT count as equality)
    rows = np.asarray([[1.0, 200000.0, 400000.0], [5.0, 6.0, 7.0]])
    targets = [[1.0 + 1e-13, 300000.0, 500000.0],   # close only in coord 0 -> NOT equal
               [5.0, 6.0, 7.0]]                     # exact match
    res = CW_ANY_EQ(rows, targets)
    if not (res[0] is False or res[0] == np.False_) or not res[1]:
        print("V1d FAIL", res)
        return False
    print("V1d any_equal_to single-coord regression: PASS")
    return True


def verify_subsample_window(ledger_events, fpast, stride, label):
    """V2: frozen TR.window_metrics vs fast pipeline on a deterministic subsample."""
    sub = ledger_events[::stride]
    m_ref = TR.window_metrics([list(p) for p in fpast], sub)
    m_fast = window_metrics_fast(fpast, sub)
    ok = True
    if m_ref["emptyWindow"] != m_fast["emptyWindow"]:
        ok = False
    if sorted(m_ref["perSource"]) != sorted(m_fast["perSource"]):
        ok = False
    for s in m_ref["perSource"]:
        for k in ("nTuplesProduced", "nTuplesExclusive", "nexclND"):
            if m_ref["perSource"][s][k] != m_fast["perSource"][s][k]:
                ok = False
        for k in ("whvg", "whvgSharePct", "exclusiveNdSharePct"):
            if repr(m_ref["perSource"][s][k]) != repr(m_fast["perSource"][s][k]):
                ok = False
    if repr(m_ref["hvAll"]) != repr(m_fast["hvAll"]) or m_ref["nndAll"] != m_fast["nndAll"]:
        ok = False
    print(f"V2 {label}: {'PASS' if ok else 'FAIL'} (stride={stride}, events={len(sub)})")
    return ok


# ---------------------------------------------------------------- main
def main():
    if not verify():
        print("VERIFICATION FAILED — fail-closed")
        return 2

    # ---- load ledger events (V5 schema) ----
    events = []
    roles = {}
    with io.open(os.path.join(RES, "source-ledger.csv"), encoding="utf-8", newline="") as fh:
        r = csv.DictReader(fh)
        for row in r:
            src = row["firstLevelSource"]
            ev = {"source": src,
                  "nominalFE": int(row["nominalFE"]),
                  "actualFE": int(row["actualFE"]),
                  "candidateId": row["candidateFingerprint"],
                  "objectives": (float(row["Cmax"]), float(row["TEC"]), float(row["TWC"]))}
            events.append(ev)
            roles[int(row["actualFE"])] = (row["subSwarmRole"], src)
    events.sort(key=lambda e: e["actualFE"])
    print("ledger events:", len(events))

    # ---- lifecycle per window per source ----
    life_types = ["GENERATED", "DESCENDANT", "IMPROVING_DESCENDANT", "MERGE_POOL",
                  "PDDR_SELECTED", "WORKING_POPULATION", "PERSONAL_ARCHIVE",
                  "QG_TEACHER", "QP_TEACHER", "QP_ACTION"]
    life = {}
    with io.open(os.path.join(RES, "source-lifecycle-events.csv"), encoding="utf-8", newline="") as fh:
        r = csv.DictReader(fh)
        for row in r:
            t = int(row["nominalFE"])
            t = (t + WINDOW_FE - 1) // WINDOW_FE  # window index from nominal FE
            key = (t, row["source"], row["eventType"])
            life[key] = life.get(key, 0) + 1

    os.makedirs(OUT, exist_ok=True)
    rows_out = []
    summary = {}
    # V2: frozen-reference equivalence on deterministic subsamples (fail-closed)
    fp1 = TR.read_front_csv(os.path.join(CKPT, "checkpoint-0-decision-front.csv"))
    fp10 = TR.read_front_csv(os.path.join(CKPT, "checkpoint-225000-decision-front.csv"))
    if not verify_subsample_window(events, fp1, 40, "window1-subsample") or \
       not verify_subsample_window(events, fp10, 40, "window10-subsample"):
        print("V2 EQUIVALENCE FAILED — fail-closed")
        return 2
    for t in range(1, 21):
        lo, hi = (t - 1) * WINDOW_FE, t * WINDOW_FE
        wev = [e for e in events if lo < e["nominalFE"] <= hi]
        fpast = TR.read_front_csv(os.path.join(CKPT, f"checkpoint-{lo}-decision-front.csv"))
        m = window_metrics_fast(fpast, wev)
        if m["whvgSumMinusTotalGain"] > 1e-9:
            print(f"SANITY FAIL window {t}: whvgSumMinusTotalGain={m['whvgSumMinusTotalGain']}")
            return 3
        # per-window primary-source rollup (PARENT_CARRYOVER from INITIAL_POPULATION rows)
        eval_by_src = {}
        for e in wev:
            prim = RAW_TO_PRIMARY.get(e["source"], e["source"])
            eval_by_src[prim] = eval_by_src.get(prim, 0) + 1
        for s in SOURCES:
            d = m["perSource"].get(s if s != "PARENT_CARRYOVER" else "NOT_APPLICABLE", {})
            lc = {k: life.get((t, s if s != "PARENT_CARRYOVER" else "NOT_APPLICABLE", k), 0) for k in life_types}
            row = {"window": t, "nominalFElo": lo, "nominalFEhi": hi, "source": s,
                   "nEvaluated": eval_by_src.get(s, 0),
                   "nUniqueObjectives": d.get("nTuplesProduced", 0),
                   "nTuplesExclusive": d.get("nTuplesExclusive", 0),
                   "nexclND": d.get("nexclND", 0),
                   "exclusiveNDSharePct": repr(d.get("exclusiveNdSharePct", 0.0)),
                   "WHVG": repr(d.get("whvg", 0.0)),
                   "WHVGSharePct": repr(d.get("whvgSharePct", 0.0)),
                   "hvAll": repr(m["hvAll"]), "nndAll": m["nndAll"],
                   "emptyWindow": m["emptyWindow"]}
            row.update({f"life_{k}": lc[k] for k in life_types})
            rows_out.append(row)
            agg = summary.setdefault(s, {k: 0 for k in ["nEvaluated"] + life_types})
            agg["nEvaluated"] += eval_by_src.get(s, 0)
            for k in life_types:
                agg[k] += lc[k]
        print(f"window {t}: hvAll={m['hvAll']:.6f} nndAll={m['nndAll']} events={len(wev)}")

    cols = list(rows_out[0].keys())
    with io.open(os.path.join(OUT, "source-window-metrics.csv"), "w", encoding="utf-8", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=cols, lineterminator="\n")
        w.writeheader()
        w.writerows(rows_out)

    with io.open(os.path.join(OUT, "source-lifecycle-summary.csv"), "w", encoding="utf-8", newline="") as fh:
        w = csv.writer(fh, lineterminator="\n")
        w.writerow(["source"] + ["nEvaluated"] + life_types +
                   ["mergeToPddrRate", "pddrToWorkingRate", "archiveRate", "improvingDescendantRate"])
        for s in SOURCES:
            a = summary[s]
            lc = {k: a[k] for k in life_types}
            mp, pd, wp, pa = lc["MERGE_POOL"], lc["PDDR_SELECTED"], lc["WORKING_POPULATION"], lc["PERSONAL_ARCHIVE"]
            gen, imp, desc = lc["GENERATED"], lc["IMPROVING_DESCENDANT"], lc["DESCENDANT"]
            w.writerow([s, a["nEvaluated"]] + [lc[k] for k in life_types] +
                       [(pd / mp if mp else 0.0), (wp / pd if pd else 0.0),
                        (pa / gen if gen else 0.0), (imp / desc if desc else 0.0)])

    # ---- four-direction extreme contributions per window (descriptive) ----
    with io.open(os.path.join(OUT, "direction-extreme-contributions.csv"), "w", encoding="utf-8", newline="") as fh:
        w = csv.writer(fh, lineterminator="\n")
        w.writerow(["window", "role", "nEvaluated", "minCmax", "minTEC", "minTWC"])
        for t in range(1, 21):
            lo, hi = (t - 1) * WINDOW_FE, t * WINDOW_FE
            agg = {}
            for e in events:
                if lo < e["nominalFE"] <= hi:
                    role, src = roles[e["actualFE"]]
                    a = agg.setdefault(role, [0, None, None, None])
                    a[0] += 1
                    c, tec, twc = e["objectives"]
                    a[1] = c if a[1] is None else min(a[1], c)
                    a[2] = tec if a[2] is None else min(a[2], tec)
                    a[3] = twc if a[3] is None else min(a[3], twc)
            for role in sorted(agg):
                a = agg[role]
                w.writerow([t, role, a[0]] + [repr(v) for v in a[1:]])

    print("OUT written:", OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main())
