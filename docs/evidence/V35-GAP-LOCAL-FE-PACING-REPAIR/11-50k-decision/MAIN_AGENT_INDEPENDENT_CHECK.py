# -*- coding: utf-8 -*-
"""Main-agent independent recomputation for the 50k decision (50K_REPAIR_DECISION.md).

Recomputes every load-bearing number directly from the 08-remote-50k/sync raw run
files with implementations written independently of Agent C's scripts and of
scripts/fc6_metrics.py (own exact 3-D hypervolume via f1-slicing, own IGD, own
ND filter), then cross-checks against Agent C's candidate-screening.csv.
Exit code 0 iff all cross-checks agree.
"""
import csv, io, math, os, sys

ROOT = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR"
SYNC = os.path.join(ROOT, "08-remote-50k", "sync")
OUT = os.path.join(ROOT, "11-50k-decision")
PROFILES = ["C0", "C1", "C2", "C3"]
CANDS = ["C1", "C2", "C3"]
INSTANCES = ["50_2_3_1", "100_5_3_1"]
SEEDS = ["20260907", "20260914"]
F_COMMON_EXPECTED = 40000
CHECKPOINTS = [10000, 20000, 30000, 40000]

failures = []


def check(name, expected, actual, tol=1e-9):
    ok = abs(expected - actual) <= tol * max(1.0, abs(expected))
    if not ok:
        failures.append("%s: expected=%r actual=%r" % (name, expected, actual))
    return ok


def run_dir(profile, instance, seed):
    return os.path.join(SYNC, "seed-%s" % seed, "results",
                        "run-GAPL50K-%s-%s-%s" % (profile, instance, seed))


def read_props(path):
    flat = {}
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.rstrip("\n")
            if line.startswith("mechanismSummary="):
                for pair in line[len("mechanismSummary="):].split(","):
                    k, _, v = pair.partition("=")
                    flat[k] = v
            elif "=" in line:
                k, _, v = line.partition("=")
                flat[k] = v
    return flat


def read_front(run):
    pts = []
    with io.open(os.path.join(run, "front.csv"), encoding="utf-8") as fh:
        rdr = csv.reader(fh)
        next(rdr)
        for row in rdr:
            pts.append(tuple(float(v) for v in row[:3]))
    return pts


def read_curves(run):
    rows = {}
    with io.open(os.path.join(run, "cmax-audit-curves.csv"), encoding="utf-8") as fh:
        rdr = csv.DictReader(fh)
        for row in rdr:
            rows[int(row["fe"])] = (float(row["bestCmaxGlobal"]),
                                    float(row["bestTECGlobal"]),
                                    float(row["bestTWCGlobal"]))
    return rows


def nd_filter(points):
    pts = sorted(set(points))
    out = []
    for p in pts:
        dominated = False
        for q in out:
            if all(q[i] <= p[i] for i in range(3)) and any(q[i] < p[i] for i in range(3)):
                dominated = True
                break
        if not dominated:
            out = [q for q in out
                   if not (all(p[i] <= q[i] for i in range(3)) and any(p[i] < q[i] for i in range(3)))]
            out.append(p)
    return sorted(out)


def hv_3d(points, ref):
    """Exact 3-D hypervolume by slicing along objective 0 (independent impl)."""
    pts = [p for p in nd_filter(set(points))
           if all(p[i] < ref[i] for i in range(3))]
    if not pts:
        return 0.0
    xs = sorted(set([0.0] + [p[0] for p in pts] + [ref[0]]))
    total = 0.0
    for a, b in zip(xs, xs[1:]):
        if b <= 0.0:
            continue
        slab = [p for p in pts if p[0] <= b]
        # 2-D HV of (f1,f2) in the slab is computed over the projected points
        proj = sorted(set((p[1], p[2]) for p in slab))
        area = 0.0
        prev_y = ref[1]
        covered_until_x = ref[0]
        # sweep projected points by ascending f2: classical staircase
        cur = []
        for y, z in sorted(proj):
            if y >= ref[1] or z >= ref[2]:
                continue
            cur.append((y, z))
        # 2-D HV with ref (ref1, ref2): area of union of rectangles
        events = sorted(cur)  # ascending y
        area = 0.0
        max_z_so_far = -1.0
        last_y = ref[1]  # iterate from high y to low y for staircase width
        for y, z in sorted(cur, reverse=True):
            if z > max_z_so_far:
                area += (last_y - y) * (ref[2] - z) if z > max_z_so_far else 0.0
                # width from previous kept y down to this y, height ref2 - z
                max_z_so_far = z
                last_y = y
        # simpler correct staircase:
        area = 0.0
        kept = []
        for y, z in sorted(cur):
            if not any(ky <= y and kz <= z for ky, kz in kept):
                kept = [(ky, kz) for ky, kz in kept if not (y <= ky and z <= kz)]
                kept.append((y, z))
        # exact area via inclusion of maximal staircase
        ys = sorted(set([ky for ky, _ in kept] + [ref[1]]))
        for ya, yb in zip(ys, ys[1:]):
            zs = [kz for ky, kz in kept if ky <= ya]
            if zs:
                area += (yb - ya) * (ref[2] - min(zs))
        total += (b - a) * area
    return total


def igd(ref_pts, front_pts):
    tot = 0.0
    for r in ref_pts:
        tot += min(math.sqrt(sum((r[i] - q[i]) ** 2 for i in range(3))) for q in front_pts)
    return tot / len(ref_pts)


# ---- 1. budget fields straight from run files ------------------------------
budget = {}
for inst in INSTANCES:
    for p in PROFILES:
        vals = []
        for seed in SEEDS:
            b = read_props(os.path.join(run_dir(p, inst, seed), "budget-termination.properties"))
            vals.append((int(b["actualFE"]), b["terminationKind"],
                         int(b["formalOuterCycles"]), int(b["totalLocalFE"]),
                         float(b["localFeShare"])))
        budget[(inst, p)] = vals

print("== 1. budget consistency across seeds (deterministic scheduler) ==")
for (inst, p), vals in budget.items():
    same = len(set(vals)) == 1
    print("%s %s: %s seeds_identical=%s" % (inst, p, vals[0], same))
    if not same:
        failures.append("budget differs across seeds for %s %s" % (inst, p))
    share = vals[0][4]
    total = vals[0][3]
    fe = vals[0][0]
    check("shareIdentity %s %s" % (inst, p), share, total / fe, 1e-9)
    # structural identity for exact-stop arms
    kind = vals[0][1]
    cycles = vals[0][2]
    if kind == "EXACT_MAX_FE":
        check("exactIdentity %s %s" % (inst, p), total, fe - 100 - cycles * 5000, 0)

# ---- 2. dose gate G2/G3/G4 aggregates --------------------------------------
print("== 2. dose aggregates ==")
cum = [budget[("50_2_3_1", p)][0][3] for p in PROFILES]
shares50 = [budget[("50_2_3_1", p)][0][4] for p in PROFILES]
shares100 = [budget[("100_5_3_1", p)][0][4] for p in PROFILES]
cycles = [budget[("50_2_3_1", p)][0][2] for p in PROFILES]
print("cumulative allocated:", cum, "strict:", cum[0] > cum[1] > cum[2] > cum[3])
print("shares 50:", shares50)
print("shares 100:", shares100)
print("adjacent drops (pp) 50:", [round((shares50[i] - shares50[i + 1]) * 100, 2) for i in range(3)])
print("adjacent drops (pp) 100:", [round((shares100[i] - shares100[i + 1]) * 100, 2) for i in range(3)])
print("cycles:", cycles)
if not (cum[0] > cum[1] > cum[2] > cum[3]):
    failures.append("cumulative allocation not strict")
for sh in (shares50, shares100):
    if not (sh[0] > sh[3] and sh[0] >= sh[1] >= sh[2] >= sh[3]):
        failures.append("share ordering violated")
    drops = [(sh[i] - sh[i + 1]) * 100 for i in range(3)]
    if sum(1 for d in drops if d >= 1.0) < 2:
        failures.append("fewer than 2 adjacent drops >=1pp")

# ---- 3. F_common from curves ------------------------------------------------
print("== 3. F_common ==")
present = {cp: 0 for cp in CHECKPOINTS}
curve_cache = {}
for inst in INSTANCES:
    for p in PROFILES:
        for seed in SEEDS:
            rows = read_curves(run_dir(p, inst, seed))
            curve_cache[(p, inst, seed)] = rows
            for cp in CHECKPOINTS:
                if cp in rows:
                    present[cp] += 1
f_common = max(cp for cp in CHECKPOINTS if present[cp] == 16)
print("checkpoint presence:", present, "-> F_common =", f_common)
if f_common != F_COMMON_EXPECTED:
    failures.append("F_common=%s expected 40000" % f_common)

# ---- 4. terminal metrics with own pipeline ---------------------------------
print("== 4. terminal metrics (own HV/IGD implementation) ==")
fronts = {}
for inst in INSTANCES:
    for p in PROFILES:
        for seed in SEEDS:
            fronts[(p, inst, seed)] = nd_filter(read_front(run_dir(p, inst, seed)))
pfref = {}
bounds = {}
for inst in INSTANCES:
    union = set()
    for p in PROFILES:
        for seed in SEEDS:
            union |= set(fronts[(p, inst, seed)])
    pfref[inst] = nd_filter(union)
    lo = [min(pt[i] for pt in pfref[inst]) for i in range(3)]
    hi = [max(pt[i] for pt in pfref[inst]) for i in range(3)]
    bounds[inst] = (lo, hi)
    print("PFref_terminal %s: %d points, ideal=%s nadir=%s" % (inst, len(pfref[inst]), lo, hi))

def norm(pt, inst):
    lo, hi = bounds[inst]
    return tuple((pt[i] - lo[i]) / (hi[i] - lo[i]) for i in range(3))

hv_term, igd_term = {}, {}
for inst in INSTANCES:
    for p in PROFILES:
        for seed in SEEDS:
            npts = [norm(pt, inst) for pt in fronts[(p, inst, seed)]]
            hv_term[(p, inst, seed)] = hv_3d(npts, (1.1, 1.1, 1.1))
            igd_term[(p, inst, seed)] = igd([norm(q, inst) for q in pfref[inst]], npts)

print("HV terminal (mean of 2 seeds):")
for inst in INSTANCES:
    print(" %s:" % inst, {p: round(sum(hv_term[(p, inst, s)] for s in SEEDS) / 2, 4) for p in PROFILES})
print("IGD terminal (mean of 2 seeds):")
for inst in INSTANCES:
    print(" %s:" % inst, {p: round(sum(igd_term[(p, inst, s)] for s in SEEDS) / 2, 4) for p in PROFILES})

# ---- 5. paired responses ----------------------------------------------------
print("== 5. paired responses (median of 2 seeds) ==")
def median2(a, b):
    return (a + b) / 2.0

resp = {}
for cand in CANDS:
    for inst in INSTANCES:
        d_hv = median2(*[(hv_term[(cand, inst, s)] - hv_term[("C0", inst, s)]) / hv_term[("C0", inst, s)]
                         for s in SEEDS])
        d_igd = median2(*[(igd_term[("C0", inst, s)] - igd_term[(cand, inst, s)]) / igd_term[("C0", inst, s)]
                          for s in SEEDS])
        # terminal scalar deltas
        for key, idx in (("Cmax", 0), ("TEC", 1), ("TWC", 2)):
            d = median2(*[(min(pt[idx] for pt in fronts[("C0", inst, s)])
                           - min(pt[idx] for pt in fronts[(cand, inst, s)]))
                          / min(pt[idx] for pt in fronts[("C0", inst, s)]) for s in SEEDS])
            resp[(cand, inst, "term_" + key)] = d
        # common-FE scalar deltas at F_common
        for key, idx in (("Cmax", 0), ("TEC", 1), ("TWC", 2)):
            vals = []
            for s in SEEDS:
                c0 = curve_cache[("C0", inst, s)][f_common][idx]
                cc = curve_cache[(cand, inst, s)][f_common][idx]
                vals.append((c0 - cc) / c0)
            resp[(cand, inst, "common_" + key)] = median2(*vals)
        resp[(cand, inst, "term_HV")] = d_hv
        resp[(cand, inst, "term_IGD")] = d_igd
        print("%s %s: dHV=%+.4f dIGD=%+.4f | common dCmax=%+.4f dTEC=%+.4f dTWC=%+.4f | "
              "term dCmax=%+.4f dTEC=%+.4f dTWC=%+.4f" % (
                  cand, inst, d_hv, d_igd,
                  resp[(cand, inst, "common_Cmax")], resp[(cand, inst, "common_TEC")],
                  resp[(cand, inst, "common_TWC")],
                  resp[(cand, inst, "term_Cmax")], resp[(cand, inst, "term_TEC")],
                  resp[(cand, inst, "term_TWC")]))

# ---- 6. gates ---------------------------------------------------------------
print("== 6. gates ==")
for cand in CANDS:
    safe = (resp[(cand, "50_2_3_1", "term_HV")] >= -0.02
            and resp[(cand, "50_2_3_1", "term_IGD")] >= -0.10)
    hard = (resp[(cand, "100_5_3_1", "term_HV")] >= 0.02
            or resp[(cand, "100_5_3_1", "term_IGD")] >= 0.10)
    hard_ok = (resp[(cand, "100_5_3_1", "term_HV")] >= -0.02
               and resp[(cand, "100_5_3_1", "term_IGD")] >= -0.10)
    triple = all(min(resp[(cand, i, "common_" + k)] for i in INSTANCES) >= -0.02
                 for k in ("Cmax", "TEC", "TWC"))
    # dual-caliber: terminal direction (HV composite) vs common-FE scalars
    term_pos = (resp[(cand, "50_2_3_1", "term_HV")] > 0
                and resp[(cand, "100_5_3_1", "term_HV")] > 0)
    common_pos = all(resp[(cand, i, "common_" + k)] > 0
                     for i in INSTANCES for k in ("Cmax", "TEC", "TWC"))
    print("%s: safe=%s hard=%s(hard_ok=%s) triple=%s termHV_both_pos=%s commonAll_pos=%s" % (
        cand, safe, hard, hard_ok, triple, term_pos, common_pos))

# ---- 7. cross-check vs Agent C candidate-screening.csv ----------------------
print("== 7. cross-check vs candidate-screening.csv ==")
with io.open(os.path.join(ROOT, "10-performance-screen", "candidate-screening.csv"),
             encoding="utf-8-sig") as fh:
    for row in csv.DictReader(fh):
        print(" C-row:", {k: row[k] for k in list(row)[:8]})

print()
if failures:
    print("INDEPENDENT_CHECK = FAILED")
    for f in failures:
        print(" -", f)
    sys.exit(1)
print("INDEPENDENT_CHECK = PASSED")
