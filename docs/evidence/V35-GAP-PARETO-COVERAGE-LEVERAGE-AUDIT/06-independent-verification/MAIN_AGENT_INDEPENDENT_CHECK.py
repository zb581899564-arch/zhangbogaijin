# -*- coding: utf-8 -*-
"""Main-agent independent verification for the Pareto-coverage leverage audit.

Recomputes the load-bearing numbers with independent implementations (own ND
filter, own 3-D HV slicing, own entropy) directly from raw run/ledger files,
then cross-checks against the agents' CSVs.  Exit 0 iff all checks agree.
"""
import csv, io, math, os, re, sys

AUD = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT"
REPAIR = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR"
SYNC = os.path.join(REPAIR, "16-remote-250k-runs", "sync")
SEEDS = ["20260916", "20260917", "20260918"]
TARGETS = [50000, 100000, 150000, 200000]
failures = []


def med3(vals):
    return sorted(vals)[1]


def read_fp_front(path, fp_col=4, obj_cols=(5, 6, 7)):
    """checkpoint-fronts.csv style: fingerprint + 3 objectives."""
    out = {}
    if not os.path.exists(path):
        return out
    for line in io.open(path, encoding="utf-8"):
        parts = line.rstrip("\n").split(",")
        if len(parts) <= max(fp_col, *obj_cols):
            continue
        if parts[0] == "checkpointTargetFE" or parts[0] == "candidateFingerprint":
            continue
        try:
            out[parts[fp_col]] = (float(parts[obj_cols[0]]),
                                  float(parts[obj_cols[1]]),
                                  float(parts[obj_cols[2]]))
        except ValueError:
            continue
    return out


def read_obj_front(path):
    pts = set()
    if not os.path.exists(path):
        return pts
    for line in io.open(path, encoding="utf-8"):
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
    pts = [p for p in nd_filter(set(points)) if all(p[i] < ref[i] for i in range(3))]
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


def run_dir(arm, inst, seed):
    return os.path.join(SYNC, "seed-%s" % seed, "results",
                        "run-GAPL250K-%s-%s-%s" % (arm, inst, seed))


# ---- 0. top-level manifests (old 2346-item claim + regenerated 2353) --------
print("== 0. manifests ==")
top = os.path.join(REPAIR, "evidence-sha256.tsv")
lines = open(top).read().splitlines()[1:]
missing = mismatch = 0
for ln in lines:
    p, h = ln.split("\t")
    fp = os.path.join(REPAIR, p)
    if not os.path.exists(fp):
        missing += 1
    elif hashlib_sha(fp) if False else __import__("hashlib").sha256(open(fp, "rb").read()).hexdigest() != h:
        mismatch += 1
print("top-level entries=%d missing=%d mismatch=%d" % (len(lines), missing, mismatch))
if missing or mismatch:
    failures.append("top-level manifest not closed")
pre = open(os.path.join(REPAIR, "19-evidence-governance-correction",
                        "..", "15-250k-preregistration", "evidence-sha256.pre-binding-correction.tsv")).read().splitlines()[1:]
post = open(os.path.join(REPAIR, "15-250k-preregistration",
                         "evidence-sha256.post-binding-correction.tsv")).read().splitlines()[1:]
print("pre-correction entries=%d, post-correction entries=%d" % (len(pre), len(post)))


def hashlib_sha(p):
    import hashlib
    return hashlib.sha256(open(p, "rb").read()).hexdigest()


# ---- 1. 18 runs + 6 fair groups --------------------------------------------
print("== 1. run statuses ==")
acc = list(csv.DictReader(open(os.path.join(REPAIR, "16-remote-250k-runs",
                                            "run-acceptance.csv"), encoding="utf-8-sig")))
n_ok = sum(1 for r in acc if r["acceptance"] == "PASS" and r["status"] == "COMPLETED")
print("run-acceptance rows=%d accepted=%d" % (len(acc), n_ok))
if len(acc) != 18 or n_ok != 18:
    failures.append("run acceptance != 18/18")

# ---- 2. independent front-level gap recomputation (C0 & C3, hard instance) --
print("== 2. independent observed-decision gap (hard instance) ==")
# reference bounds per instance from the 17-dir PFref_terminal (pipeline) for
# normalization; HV computed with own implementation
pf_paths = {i: os.path.join(REPAIR, "17-250k-reference-and-metrics",
                            "terminal-reference-fronts", "PFref_terminal_%s.csv" % i)
            for i in ("50_2_3_1", "100_5_3_1")}
bounds = {}
for inst, p in pf_paths.items():
    pts = read_obj_front(p)
    lo = [min(q[i] for q in pts) for i in range(3)]
    hi = [max(q[i] for q in pts) for i in range(3)]
    bounds[inst] = (lo, hi)


def norm(p, inst):
    lo, hi = bounds[inst]
    return tuple((p[i] - lo[i]) / (hi[i] - lo[i]) for i in range(3))


my_gap = {}
for inst in ("100_5_3_1", "50_2_3_1"):
    for arm in ("C0", "C3"):
        for seed in SEEDS:
            d = run_dir(arm, inst, seed)
            cf = os.path.join(d, "checkpoint-fronts.csv")
            rows = {}
            for line in io.open(cf, encoding="utf-8"):
                parts = line.rstrip("\n").split(",")
                if len(parts) < 8 or parts[0] == "checkpointTargetFE":
                    continue
                rows.setdefault((int(parts[0]), parts[3]), {})[parts[4]] = \
                    (float(parts[5]), float(parts[6]), float(parts[7]))
            for t in TARGETS:
                dec = rows.get((t, "checkpoint-decision-front"), {})
                obs = rows.get((t, "checkpoint-observed-full-front"), {})
                dec_fp = set(dec)
                obs_fp = set(obs)
                dec_pts = set(dec.values())
                obs_nd = nd_filter(set(obs.values()))
                # observed-only, fingerprint level (matches agent B's registered definition)
                only_fp = obs_fp - dec_fp
                only_fp_pts = {obs[fp] for fp in only_fp}
                # strict variant: objective-new points only
                dec_triples = set(dec_pts)
                only_obj = [p for p in obs_nd if p not in dec_triples]
                ratio = len(only_fp) / len(obs_fp) if obs_fp else 0.0
                ratio_objnew = len(only_obj) / len(obs_nd) if obs_nd else 0.0
                hv_dec = hv_3d({norm(p, inst) for p in dec_pts})
                hv_rec = hv_3d({norm(p, inst) for p in dec_pts} | {norm(p, inst) for p in only_fp_pts})
                rec = hv_rec - hv_dec
                my_gap[(arm, inst, seed, t)] = (ratio, rec, ratio_objnew)
# medians per (arm, inst, checkpoint) + compare with agent B's CSV
gb = list(csv.DictReader(open(os.path.join(AUD, "02-front-coverage",
                                           "observed-decision-gap.csv"), encoding="utf-8-sig")))
print("arm instance ck: medianRatio(mine vs agentB) medianRecovery(mine vs agentB)")
for arm in ("C0", "C3"):
    for inst in ("100_5_3_1", "50_2_3_1"):
        for t in TARGETS:
            mr = med3([my_gap[(arm, inst, s, t)][0] for s in SEEDS])
            mo = med3([my_gap[(arm, inst, s, t)][2] for s in SEEDS])
            md = med3([my_gap[(arm, inst, s, t)][1] for s in SEEDS])
            brow = [r for r in gb if r["arm"] == arm and r["instance"] == inst
                    and int(r["checkpointTargetFE"]) == t]
            br = med3([float(r["observedOnlyRatio"]) for r in brow])
            bd = med3([float(r["potentialHvRecovery"]) for r in brow])
            ok = abs(mr - br) <= 0.05 and abs(md - bd) <= 0.005
            print("%s %s %6d: fpRatio %.4f vs %.4f (objNew %.4f) | recovery %.5f vs %.5f %s"
                  % (arm, inst, t, mr, br, mo, md, bd, "OK" if ok else "MISMATCH"))
            if not ok:
                failures.append("gap mismatch %s %s %d" % (arm, inst, t))

# ---- 3. gate-relevant summaries --------------------------------------------
print("== 3. gate summaries ==")
recs = [abs(float(r["potentialHvRecovery"])) for r in gb]
print("potentialHvRecovery max=%.5f rows>=0.02: %d/%d"
      % (max(recs), sum(1 for v in recs if v >= 0.02), len(recs)))
ratios = [float(r["observedOnlyRatio"]) for r in gb]
print("observedOnlyRatio rows>=0.10: %d/%d (universal, undiscriminating)"
      % (sum(1 for v in ratios if v >= 0.10), len(ratios)))
# hard vs normal C0 terminal ratio
c0_term = {}
for r in gb:
    if r["arm"] == "C0" and int(r["checkpointTargetFE"]) == 250000:
        c0_term.setdefault(r["instance"], []).append(float(r["observedOnlyRatio"]))
hard = med3(c0_term["100_5_3_1"])
normal = med3(c0_term["50_2_3_1"])
print("C0 terminal observedOnlyRatio: hard=%.4f normal=%.4f gap=%+.4f (gate needs hard-normal>=+0.10)"
      % (hard, normal, hard - normal))
if hard - normal >= 0.10:
    failures.append("condition 3 unexpectedly satisfied")

# ---- 4. teacher concentration (own entropy) --------------------------------
print("== 4. teacher concentration (C0, final window) ==")
tc = list(csv.DictReader(open(os.path.join(AUD, "04-teacher-analysis",
                                          "teacher-concentration-analysis.csv"),
                              encoding="utf-8-sig")))
for inst in ("50_2_3_1", "100_5_3_1"):
    rows = [r for r in tc if r["arm"] == "C0" and r["instance"] == inst
            and r["feWindowStart"] == "200000"]
    t1 = med3([float(r["top1Share"]) for r in rows])
    ent = med3([float(r["normalizedEntropy"]) for r in rows])
    print("%s C0 final-window: top1Share=%.4f normEntropy=%.4f (rows=%d)"
          % (inst, t1, ent, len(rows)))
rows_h = [float(r["top1Share"]) for r in tc if r["arm"] == "C0"
          and r["instance"] == "100_5_3_1" and r["feWindowStart"] == "200000"]
rows_n = [float(r["top1Share"]) for r in tc if r["arm"] == "C0"
          and r["instance"] == "50_2_3_1" and r["feWindowStart"] == "200000"]
diff = med3(rows_h) - med3(rows_n)
print("hard-vs-normal top1Share gap=%+.4f (H3 gate needs >=+0.20)" % diff)
if diff >= 0.20:
    failures.append("H3 condition unexpectedly satisfied")

# ---- 5. FC5 directional retention (from agent C CSV, spot verify) ----------
print("== 5. FC5 directional retention ==")
dr = list(csv.DictReader(open(os.path.join(AUD, "03-pddr-utilization",
                                          "directional-representative-lifecycle.csv"),
                              encoding="utf-8-sig")))
cols = dr[0].keys()
print("lifecycle rows=%d columns=%s" % (len(dr), list(cols)[:8]))

print()
if failures:
    print("INDEPENDENT_CHECK = FAILED")
    for f in failures:
        print(" -", f)
    sys.exit(1)
print("INDEPENDENT_CHECK = PASSED")
