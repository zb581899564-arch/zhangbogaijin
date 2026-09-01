# -*- coding: utf-8 -*-
"""Source-contribution diagnostics analysis (prereg §5/§6).

Inputs : 04-remote-100k/sync/seed-<S>/results/run-GAPLSRC-C0-<I>-<S>/
Outputs: 05-analysis/{source-contribution-matrix.csv, source-timing-windows.csv,
         teacher-join.csv, verdict-inputs.properties}

marginalHvContribution definition (prereg §5, registered): exact partition of
the terminal observed-front HV by exclusive volume of each final ND point
(x-sweep, 3-D, normalized by the front's own ideal/nadir, HV ref (1.1,1.1,1.1)),
attributed to the source label of that point's ledger row.  Sum over all
sources == HV(front).  valueEfficiency e(S) = HVShare(S) / evalShare(S).
"""
import csv, io, math, os, re, sys, collections

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SYNC = os.path.join(ROOT, "04-remote-100k", "sync")
SEEDS = ["20260919", "20260920", "20260921"]
INSTANCES = ["50_2_3_1", "100_5_3_1"]
WINDOWS = [(0, 25000), (25000, 50000), (50000, 75000), (75000, 100000)]

# budget-conservative source rollups (prereg §2 label mapping)
ROLLUP = {"INITIAL_POPULATION": "PARENT", "GLOBAL_CFVF": "CFVF(GLOBAL_Q)",
          "FINAL_EVALUATE": "FINAL_EVALUATE", "CATA_TEST": "CATA",
          "CATA_APPLY": "CATA", "INTER_FACTORY_LS": "INHERITED_LS",
          "INTRA_FACTORY_VNS": "INHERITED_LS", "UNSET": "UNSET"}


def run_dir(seed, inst):
    return os.path.join(SYNC, "seed-%s" % seed, "results",
                        "run-GAPLSRC-C0-%s-%s" % (inst, seed))


def sha_text(text):
    import hashlib
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def read_ledger(path):
    rows = []
    with io.open(path, encoding="utf-8") as fh:
        for r in csv.DictReader(fh):
            rows.append(r)
    return rows


def read_points(path):
    pts = set()
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            parts = line.strip().split(",")
            if len(parts) < 3 or parts[0].lower().startswith(("cmax", "candidate")):
                continue
            try:
                pts.add((float(parts[0]), float(parts[1]), float(parts[2])))
            except ValueError:
                continue
    return pts


def nd_filter(points):
    pts = sorted(points)
    out = []
    for p in pts:
        if any(all(q[i] <= p[i] for i in range(3)) and any(q[i] < p[i] for i in range(3))
               for q in out):
            continue
        out = [q for q in out
               if not (all(p[i] <= q[i] for i in range(3)) and any(p[i] < q[i] for i in range(3)))]
        out.append(p)
    return out


def hv_3d(points, ref=(1.1, 1.1, 1.1)):
    pts = [p for p in points if all(p[i] < ref[i] for i in range(3))]
    if not pts:
        return 0.0
    xs = sorted(set([0.0] + [p[0] for p in pts] + [ref[0]]))
    total = 0.0
    for a, b in zip(xs, xs[1:]):
        if b <= 0.0:
            continue
        slab = sorted(set((p[1], p[2]) for p in pts if p[0] <= b))
        kept = []
        for y, z in slab:
            if y >= ref[1] or z >= ref[2]:
                continue
            if not any(ky <= y and kz <= z for ky, kz in kept):
                kept = [(ky, kz) for ky, kz in kept if not (y <= ky and z <= kz)]
                kept.append((y, z))
        ys = sorted(set([ky for ky, _ in kept] + [ref[1]]))
        area = 0.0
        for ya, yb in zip(ys, ys[1:]):
            zs = [kz for ky, kz in kept if ky <= ya]
            if zs:
                area += (yb - ya) * (ref[2] - min(zs))
        total += (b - a) * area
    return total


def _area2d(skyline, ref=1.1):
    kept = []
    for y, z in sorted(skyline):
        if not any(ky <= y and kz <= z for ky, kz in kept):
            kept = [(ky, kz) for ky, kz in kept if not (y <= ky and z <= kz)]
            kept.append((y, z))
    ys = sorted(set([ky for ky, _ in kept] + [ref]))
    area = 0.0
    for ya, yb in zip(ys, ys[1:]):
        zs = [kz for ky, kz in kept if ky <= ya]
        if zs:
            area += (yb - ya) * (ref - min(zs))
    return area


def exclusive_attribution(front_norm):
    """Exact HV partition of a 3-D ND set: process points by ascending x;
    each point's 2-D exclusive staircase gain g_i applies over slabs
    [x_i, 1.1), so contribution(i) = g_i * (1.1 - x_i) and the contributions
    sum exactly to HV(front)."""
    pts = sorted(front_norm, key=lambda p: p[0])
    REF = 1.1
    contrib = {p: 0.0 for p in pts}
    active = []
    i = 0
    n = len(pts)
    while i < n:
        x = pts[i][0]
        j = i
        while j < n and pts[j][0] == x:
            j += 1
        for k in range(i, j):
            p = pts[k]
            y2, z2 = min(p[1], REF), min(p[2], REF)
            if y2 <= 0.0 or z2 <= 0.0:
                continue
            if not any(ky <= y2 and kz <= z2 for ky, kz in active):
                before = _area2d(active)
                after_active = [(ky, kz) for ky, kz in active
                                if not (y2 <= ky and z2 <= kz)]
                after_active.append((y2, z2))
                after = _area2d(after_active)
                contrib[p] = max(0.0, after - before) * (REF - x)
                active = after_active
        i = j
    return contrib


rows_out = []
window_rows = []
verdict = {}
for inst in INSTANCES:
    for seed in SEEDS:
        d = run_dir(seed, inst)
        ledger = read_ledger(os.path.join(d, "source-ledger.csv"))
        # terminal fronts
        obs_pts = read_points(os.path.join(d, "passive-archive.csv"))
        obs_nd = nd_filter(obs_pts)
        dec_pts = read_points(os.path.join(d, "front.csv"))
        lo = [min(p[i] for p in obs_nd) for i in range(3)]
        hi = [max(p[i] for p in obs_nd) for i in range(3)]

        def norm(p):
            return tuple((p[i] - lo[i]) / (hi[i] - lo[i]) for i in range(3))

        front_n = [norm(p) for p in obs_nd]
        hv_total = hv_3d(front_n)
        contrib = exclusive_attribution(front_n)
        # map objective triple -> source of its first ledger row (admission)
        triple_source = {}
        triple_fp = {}
        for r in ledger:
            key = (float(r["Cmax"]), float(r["TEC"]), float(r["TWC"]))
            if key not in triple_source:
                triple_source[key] = ROLLUP.get(r["source"], r["source"])
                triple_fp[key] = r["candidateFingerprint"]
        # per-source aggregates
        eval_count = collections.Counter()
        pool = collections.Counter()
        selected = collections.Counter()
        hv_share = collections.Counter()
        dec_contrib = collections.Counter()
        obs_contrib = collections.Counter()
        # ledger fingerprints for front membership
        fp_set_ledger = set(r["candidateFingerprint"] for r in ledger)
        for r in ledger:
            src = ROLLUP.get(r["source"], r["source"])
            eval_count[src] += 1
        prows = read_ledger(os.path.join(d, "pddr-round-ledger.csv"))
        for r in prows:
            src = ROLLUP.get(r["selectorSource"], r["selectorSource"])
            pool[src] += 1
            if r["selectedByPddr"] == "true":
                selected[src] += 1
        # front membership by fingerprint (terminal)
        dec_fp = set()
        for line in io.open(os.path.join(d, "front.csv"), encoding="utf-8"):
            parts = line.strip().split(",")
            if len(parts) >= 3 and not parts[0].lower().startswith(("cmax", "candidate")):
                dec_pts.add((float(parts[0]), float(parts[1]), float(parts[2])))
        # fingerprint membership from checkpoint-fronts terminal rows if present
        for p in obs_nd:
            src = triple_source.get(p, "UNKNOWN")
            hv_share[src] += contrib[norm(p)]
            obs_contrib[src] += 1
        # decision front membership: objective triple match
        for p in dec_pts:
            src = triple_source.get(p, "UNKNOWN")
            dec_contrib[src] += 1
        total_eval = sum(eval_count.values())
        for src in sorted(set(list(eval_count) + list(hv_share) + list(pool))):
            eshare = eval_count[src] / total_eval if total_eval else 0.0
            hvsh = (hv_share[src] / hv_total) if hv_total > 0 else 0.0
            e_val = (hvsh / eshare) if eshare > 0 else float("nan")
            rows_out.append([inst, seed, src, eval_count[src], round(eshare, 6),
                             pool[src], selected[src],
                             obs_contrib[src], dec_contrib[src],
                             round(hv_share[src], 8), round(hvsh, 6),
                             round(e_val, 6) if e_val == e_val else "INF_OR_NA"])
        # timing windows: per source evalShare + admitted share per window
        for (w0, w1) in WINDOWS:
            wc = collections.Counter()
            for r in ledger:
                fe = int(r["observedFE"])
                if w0 < fe <= w1:
                    wc[ROLLUP.get(r["source"], r["source"])] += 1
            tot_w = sum(wc.values())
            for src in sorted(wc):
                window_rows.append([inst, seed, "%d-%d" % (w0, w1), src,
                                    wc[src], round(wc[src] / tot_w, 6) if tot_w else 0.0])
        verdict.setdefault(inst, {})[seed] = {
            "hvTotal": hv_total,
            "eBySource": {src: ((hv_share[src] / hv_total) / (eval_count[src] / total_eval)
                                if eval_count[src] and hv_total > 0 else None)
                          for src in hv_share},
            "evalShareBySource": {src: eval_count[src] / total_eval for src in eval_count},
        }

with open(os.path.join(HERE, "source-contribution-matrix.csv"), "w", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(["instance", "seed", "sourceRollup", "evalCount", "evalShare",
                "poolEntered", "selectedByPddr", "observedFrontCount",
                "decisionFrontCount", "marginalHvSum", "hvShare", "valueEfficiency"])
    w.writerows(rows_out)
with open(os.path.join(HERE, "source-timing-windows.csv"), "w", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(["instance", "seed", "feWindow", "sourceRollup", "evalCount", "evalShare"])
    w.writerows(window_rows)

# medians across seeds per instance
def median3(vals):
    vals = sorted(vals)
    n = len(vals)
    if n == 0:
        return None
    if n % 2 == 1:
        return vals[n // 2]
    return (vals[n // 2 - 1] + vals[n // 2]) / 2.0

print("== per-instance median valueEfficiency / evalShare by source ==")
summary = {}
for inst in INSTANCES:
    srcs = sorted(set(r[2] for r in rows_out if r[0] == inst))
    for src in srcs:
        es = [r[4] for r in rows_out if r[0] == inst and r[2] == src]
        vs = [r[11] for r in rows_out if r[0] == inst and r[2] == src]
        vs_num = [float(v) for v in vs if str(v) not in ("NA", "INF_OR_NA", "None", "")]
        summary[(inst, src)] = (median3(es) if es else None,
                                median3(vs_num) if len(vs_num) == 3 else None)
        print("%s %-18s evalShare=%.4f e(S)=%s" % (
            inst, src, summary[(inst, src)][0],
            "%.4f" % summary[(inst, src)][1] if summary[(inst, src)][1] is not None else "NA"))

with open(os.path.join(HERE, "verdict-inputs.properties"), "w", encoding="utf-8") as fh:
    fh.write("# script-generated verdict inputs (prereg §6)\n")
    for (inst, src), (es, e) in sorted(summary.items()):
        fh.write("evalShare.%s.%s=%s\n" % (inst, src.replace("(", "_").replace(")", ""),
                                           es))
        fh.write("valueEfficiency.%s.%s=%s\n" % (inst, src.replace("(", "_").replace(")", ""),
                                                 "NA" if e is None else round(e, 6)))
print("ANALYZE_SOURCE_DIAGNOSTICS DONE")
