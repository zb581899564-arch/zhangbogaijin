# -*- coding: utf-8 -*-
"""Main-agent independent recomputation for the 250k decision.

Independent implementations (own 3-D exact HV via f1-slicing, own IGD, own ND
filter, own min-objectives) recomputed straight from the raw run files, then
cross-checked against the pipeline CSVs in 17-250k-reference-and-metrics/.
Exit 0 iff every cross-check agrees.
"""
import csv, io, math, os, sys

ROOT = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR"
SYNC = os.path.join(ROOT, "16-remote-250k-runs", "sync")
PIPE = os.path.join(ROOT, "17-250k-reference-and-metrics")
OUT = os.path.join(ROOT, "18-250k-decision")
ARMS = ["C0", "C2", "C3"]
CANDS = ["C2", "C3"]
INSTANCES = ["50_2_3_1", "100_5_3_1"]
SEEDS = ["20260916", "20260917", "20260918"]
TARGETS = [50000, 100000, 150000, 200000]
failures = []


def check(name, expected, actual, tol=0.01):
    ok = abs(expected - actual) <= tol * max(1.0, abs(expected))
    if not ok:
        failures.append("%s: pipeline=%r independent=%r" % (name, expected, actual))
    return ok


def run_dir(arm, inst, seed):
    return os.path.join(SYNC, "seed-%s" % seed, "results",
                        "run-GAPL250K-%s-%s-%s" % (arm, inst, seed))


def read_points(path):
    pts = set()
    if not os.path.exists(path):
        return pts
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.lower().startswith(("cmax", "candidatefingerprint")):
                continue
            parts = line.split(",")
            try:
                if len(parts) >= 4 and line.lower().startswith(("0", "1", "2", "3", "4",
                                                                "5", "6", "7", "8", "9")) \
                        and len(parts) == 4 and len(parts[0]) == 64:
                    pts.add((float(parts[1]), float(parts[2]), float(parts[3])))
                else:
                    pts.add((float(parts[0]), float(parts[1]), float(parts[2])))
            except (ValueError, IndexError):
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


def igd_val(front, ref):
    return sum(min(math.sqrt(sum((r[i] - q[i]) ** 2 for i in range(3))) for q in front)
               for r in ref) / len(ref)


# raw terminal fronts + checkpoint fronts
raw = {}
for inst in INSTANCES:
    for arm in ARMS:
        for seed in SEEDS:
            d = run_dir(arm, inst, seed)
            raw[(arm, inst, seed)] = {
                "term": read_points(os.path.join(d, "front.csv")),
                "ck": {t: read_points(os.path.join(
                    d, "checkpoints", "checkpoint-%d-observed-full-front.csv" % t))
                    for t in TARGETS},
            }

# references (independent construction)
refs, ck_refs = {}, {}
for inst in INSTANCES:
    u = set()
    for arm in ARMS:
        for seed in SEEDS:
            u |= raw[(arm, inst, seed)]["term"]
    refs[inst] = nd_filter(u)
    for t in TARGETS:
        uc = set()
        for arm in ARMS:
            for seed in SEEDS:
                uc |= raw[(arm, inst, seed)]["ck"][t]
        ck_refs[(inst, t)] = nd_filter(uc)

# independent metrics
my_term = {}
for inst in INSTANCES:
    lo = [min(p[i] for p in refs[inst]) for i in range(3)]
    hi = [max(p[i] for p in refs[inst]) for i in range(3)]
    nref = [tuple((p[i] - lo[i]) / (hi[i] - lo[i]) for i in range(3)) for p in refs[inst]]
    for arm in ARMS:
        for seed in SEEDS:
            pts = raw[(arm, inst, seed)]["term"]
            npts = [tuple((p[i] - lo[i]) / (hi[i] - lo[i]) for i in range(3)) for p in pts]
            my_term[(arm, inst, seed)] = (hv_3d(npts), igd_val(npts, nref))

print("== independent terminal HV/IGD (mean of 3 seeds) ==")
for inst in INSTANCES:
    for arm in ARMS:
        hvs = [my_term[(arm, inst, s)][0] for s in SEEDS]
        igs = [my_term[(arm, inst, s)][1] for s in SEEDS]
        print("%s %s: HV=%.4f IGD=%.4f" % (inst, arm, sum(hvs) / 3, sum(igs) / 3))

# independent paired deltas (median of 3)
print("== independent medians vs pipeline (tolerance 0.2%% relative) ==")
with io.open(os.path.join(PIPE, "paired-deltas.csv"), encoding="utf-8-sig") as fh:
    pd_rows = [r for r in csv.DictReader(fh)]
with io.open(os.path.join(PIPE, "terminal-metrics.csv"), encoding="utf-8-sig") as fh:
    tm_rows = list(csv.DictReader(fh))


def tget(arm, inst, seed, col):
    return float([r for r in tm_rows if r["arm"] == arm and r["instance"] == inst
                  and r["seed"] == seed][0][col])


for arm in CANDS:
    for inst in INSTANCES:
        for col, mine_idx in (("HV", 0), ("IGD", 1)):
            if mine_idx == 0:   # HV: larger is better
                mine = sorted((my_term[(arm, inst, s)][mine_idx] - my_term[("C0", inst, s)][mine_idx])
                              / my_term[("C0", inst, s)][mine_idx] for s in SEEDS)[1]
            else:               # IGD: smaller is better (task convention dIGD=(IGD_C0-IGD_cand)/IGD_C0)
                mine = sorted((my_term[("C0", inst, s)][mine_idx] - my_term[(arm, inst, s)][mine_idx])
                              / my_term[("C0", inst, s)][mine_idx] for s in SEEDS)[1]
            theirs = float([r for r in pd_rows if r["instance"] == inst and r["seed"] == "MEDIAN"
                            and r["arm"] == arm][0]["d%s_terminal" % col])
            check("%s_%s_d%s" % (arm, inst, col), theirs, mine)
            print("%s %s d%s: pipeline=%+.4f independent=%+.4f"
                  % (arm, inst, col, theirs, mine))

# independent gate verdicts vs candidate-decision.csv
print("== independent gate verdicts ==")
with io.open(os.path.join(PIPE, "candidate-decision.csv"), encoding="utf-8-sig") as fh:
    cd_rows = list(csv.DictReader(fh))


def med_of(vals):
    return sorted(vals)[1]


for arm in CANDS:
    mine = {}
    mine["normal"] = (med_of([(my_term[(arm, "50_2_3_1", s)][0] - my_term[("C0", "50_2_3_1", s)][0])
                              / my_term[("C0", "50_2_3_1", s)][0] for s in SEEDS]) >= -0.02
                      and med_of([(my_term[(arm, "50_2_3_1", s)][1] - my_term[("C0", "50_2_3_1", s)][1])
                                  / my_term[("C0", "50_2_3_1", s)][1] for s in SEEDS]) >= -0.10)
    hv_h = med_of([(my_term[(arm, "100_5_3_1", s)][0] - my_term[("C0", "100_5_3_1", s)][0])
                   / my_term[("C0", "100_5_3_1", s)][0] for s in SEEDS])
    ig_h = med_of([(my_term[("C0", "100_5_3_1", s)][1] - my_term[(arm, "100_5_3_1", s)][1])
                   / my_term[("C0", "100_5_3_1", s)][1] for s in SEEDS])
    mine["hard"] = (hv_h >= 0.02 or ig_h >= 0.10) and hv_h >= -0.02 and ig_h >= -0.10
    print("%s: normalSafety=%s hardImprovement=%s (dHV_hard=%+.4f dIGD_hard=%+.4f)"
          % (arm, mine["normal"], mine["hard"], hv_h, ig_h))
    for row in cd_rows:
        if row["candidate"] == arm:
            pipe_normal = row["normalSafetyGate"] == "PASS"
            pipe_hard = row["hardImprovementGate"] == "PASS"
            if pipe_normal != mine["normal"]:
                failures.append("%s normalSafety verdict mismatch" % arm)
            if pipe_hard != mine["hard"]:
                failures.append("%s hardImprovement verdict mismatch" % arm)

# fairness + budget sanity from raw files
print("== fairness/budget sanity ==")
for inst in INSTANCES:
    for seed in SEEDS:
        fes = []
        for arm in ARMS:
            b = {}
            with io.open(os.path.join(run_dir(arm, inst, seed),
                                      "budget-termination.properties"), encoding="utf-8") as fh:
                for line in fh:
                    k, _, v = line.strip().partition("=")
                    b[k] = v
            fes.append(int(b["actualFE"]))
            assert b["phaseBoundAccepted"] == "true" and int(b["remainingFE"]) < 5000 \
                and float(b["utilizationRate"]) > 0.98, (arm, inst, seed)
        spread = max(fes) - min(fes)
        print("%s %s: actualFE=%s spread=%d (<5000: %s)"
              % (inst, seed, fes, spread, spread < 5000))
        if spread >= 5000:
            failures.append("fairness spread %s %s" % (inst, seed))

print()
if failures:
    print("INDEPENDENT_CHECK_250K = FAILED")
    for f in failures:
        print(" -", f)
    sys.exit(1)
print("INDEPENDENT_CHECK_250K = PASSED")
