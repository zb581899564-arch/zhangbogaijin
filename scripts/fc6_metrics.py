#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""FC-6 metrics: faithful reimplementation of P8MetricCalculator semantics.

Reference convention (FC-2): per instance, reference front = nondominated union
of ALL fronts in the comparison set (e.g. 3 baseline + 3 BP-PDDR).  HV/IGD are
computed with min/max normalization against the reference and scanning-line
iterated hypervolume with rx=ry=rz=1.1 (exact port of P8MetricCalculator).

FC-6A-POST (2026-08-19) additions:
- --pipeline {old,corrected}: 'old' = historical pipeline (normalize with clamp
  to [0,1] FIRST, then nondominated filtering in normalized space) kept for
  provenance; 'corrected' (default) = raw-space dedup -> raw-space
  nondominated -> unified min/max from the reference -> normalize WITHOUT clamp
  (points may exceed [0,1]; the HV scanline still converges them at its own box
  boundary, IGD uses the normalized-space distance).  Rationale: clamping after
  normalization can flatten a dominated point onto a reference boundary and
  create artificial dominance, silently dropping real Pareto points
  (e.g. seed24 baseline 513 -> 389 under the old pipeline).
- --paired: per-seed paired deltas  deltaHV_s=(HV_BP_s-HV_BASE_s)/HV_BASE_s,
  deltaIGD_s=(IGD_BP_s-IGD_BASE_s)/IGD_BASE_s (negative = improvement).  Seeds
  are extracted from the label/文件名 by the first 8-digit run.
- --arm-stats: arm-level median/mean/std over HV / IGD / minCmax / minTEC /
  minTWC / front size / runtime; runtime is read from the aligned
  mechanism-summary.txt 'runNanos=' line (ns, converted to seconds).
- --json: machine-readable per-run dump (interface reserved for FC-8 paired
  Wilcoxon / effect size; with 3 seeds no significance claims are drawn here).

The OLD vs CORRECTED pipeline comparison must always be produced together; if
the corrected pipeline ever reverses a development conclusion, that is
reported immediately and separately.
"""
import argparse
import json
import math
import re
import sys
from collections import defaultdict

EPS = 1e-12
SEED_RE = re.compile(r"(\d{8})")


def read_front(path):
    points = []
    with open(path, "r", encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line or line.lower().startswith(("cmax", "c,")):
                continue
            parts = line.split(",")
            if len(parts) < 3:
                continue
            try:
                points.append([float(parts[0]), float(parts[1]), float(parts[2])])
            except ValueError:
                continue
    return points


def dominates(left, right):
    strict = False
    for i in range(3):
        if left[i] > right[i] + EPS:
            return False
        if left[i] + EPS < right[i]:
            strict = True
    return strict


def equal(a, b):
    return all(abs(a[i] - b[i]) <= EPS for i in range(3))


def unique(points):
    result = []
    for p in points:
        if not result or not equal(result[-1], p):
            result.append(p)
    return result


def nondominated(points):
    result = []
    for c in points:
        if not any(other is not c and dominates(other, c) for other in points):
            result.append(list(c))
    result.sort()
    return unique(result)


def normalize(points, reference, clamp=True):
    mins = [float("inf")] * 3
    maxs = [float("-inf")] * 3
    for p in reference:
        for i in range(3):
            mins[i] = min(mins[i], p[i])
            maxs[i] = max(maxs[i], p[i])
    out = []
    for p in points:
        if clamp:
            out.append([max(0.0, min(1.0, (p[i] - mins[i]) / max(EPS, maxs[i] - mins[i])))
                        for i in range(3)])
        else:
            out.append([(p[i] - mins[i]) / max(EPS, maxs[i] - mins[i])
                        for i in range(3)])
    return out


def union_yz(points, ry, rz):
    s = sorted(points, key=lambda p: p[1])
    area = 0.0
    min_z = rz
    idx = 0
    while idx < len(s):
        y = max(0.0, min(ry, s[idx][1]))
        while idx < len(s) and s[idx][1] <= y + EPS:
            min_z = min(min_z, max(0.0, min(rz, s[idx][2])))
            idx += 1
        next_y = max(y, min(ry, s[idx][1])) if idx < len(s) else ry
        area += max(0.0, next_y - y) * max(0.0, rz - min_z)
    return area


def hypervolume(points):
    """Scanning-line HV, rx=ry=rz=1.1.  Coordinates beyond the integration box
    are converged at the box boundary BEFORE the scan.  HV over the box
    [0,rx]x[0,ry]x[0,rz] treats points outside it as lying on the box surface,
    so this pre-convergence is part of the HV definition, NOT the
    topology-changing clamp that the corrected pipeline removed from the
    normalization/filter step.  Unclamped coords (>1.1) would otherwise stall
    the scan at a zero-width slab."""
    if not points:
        return 0.0
    rx = ry = rz = 1.1
    s = sorted([[max(0.0, min(rx, p[0])), max(0.0, min(ry, p[1])),
                 max(0.0, min(rz, p[2]))] for p in points], key=lambda p: p[0])
    volume = 0.0
    active = []
    idx = 0
    while idx < len(s):
        x = max(0.0, min(rx, s[idx][0]))
        while idx < len(s) and s[idx][0] <= x + EPS:
            active.append(s[idx])
            idx += 1
        next_x = max(x, min(rx, s[idx][0])) if idx < len(s) else rx
        volume += max(0.0, next_x - x) * union_yz(active, ry, rz)
    return max(0.0, volume)


def distance(a, b):
    return math.sqrt(sum((a[i] - b[i]) ** 2 for i in range(3)))


def igd(approximation, reference):
    return sum(min(distance(t, a) for a in approximation) for t in reference) / len(reference)


def spacing(points):
    if len(points) < 2:
        return 0.0
    d = [min(distance(points[i], points[j]) for j in range(len(points)) if j != i)
         for i in range(len(points))]
    mean = sum(d) / len(d)
    return math.sqrt(sum((v - mean) ** 2 for v in d) / len(d))


def coverage(left, right):
    if not right:
        return 0.0
    return sum(1 for t in right
               if any(dominates(c, t) or equal(c, t) for c in left)) / len(right)


def metrics(approximation, reference, pipeline):
    """Return metric dict for one approximation front against the raw
    (nondominated) reference, under the selected pipeline."""
    if pipeline == "corrected":
        a_raw = nondominated(unique(approximation))
        r_raw = nondominated(unique(reference))
        a = normalize(a_raw, r_raw, clamp=False)
        r = normalize(r_raw, r_raw, clamp=False)
        n = len(a)
    else:  # historical pipeline (normalize+clamp first, filter in norm space)
        a = nondominated(normalize(approximation, reference, clamp=True))
        r = nondominated(normalize(reference, reference, clamp=True))
        n = len(a)
    return {
        "hv": hypervolume(a),
        "igd": igd(a, r),
        "spacing": spacing(a),
        "cFwd": coverage(a, r),
        "cRev": coverage(r, a),
        "n": n,
    }


def min_objectives(points):
    raw = nondominated(unique(points))
    return {
        "minCmax": min(p[0] for p in raw),
        "minTEC": min(p[1] for p in raw),
        "minTWC": min(p[2] for p in raw),
        "rawN": len(raw),
    }


def runtime_seconds(summary_path):
    if not summary_path:
        return None
    try:
        with open(summary_path, "r", encoding="utf-8") as handle:
            for line in handle:
                line = line.strip()
                if line.startswith("runNanos="):
                    return float(line.split("=", 1)[1]) / 1e9
    except (IOError, ValueError):
        return None
    return None


def seed_of(label):
    match = SEED_RE.search(label)
    return match.group(1) if match else None


def arm_of(label, baseline_labels):
    return "baseline" if label in baseline_labels else "bp"


def median(values):
    vals = sorted(values)
    n = len(vals)
    if n == 0:
        return float("nan")
    if n % 2 == 1:
        return vals[n // 2]
    return (vals[n // 2 - 1] + vals[n // 2]) / 2.0


def mean(values):
    return sum(values) / len(values) if values else float("nan")


def std(values):
    if len(values) < 2:
        return 0.0
    m = mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / (len(values) - 1))


def main():
    parser = argparse.ArgumentParser(description="FC-6 metric table (FC-6A-POST)")
    parser.add_argument("--instance")
    parser.add_argument("--fronts", nargs="+", help="front.csv paths, one per run")
    parser.add_argument("--labels", nargs="+", help="labels aligned with fronts")
    parser.add_argument("--baseline-labels", nargs="+", help="labels that form the baseline arm")
    parser.add_argument("--summaries", nargs="+", default=[],
                        help="mechanism-summary.txt paths aligned with fronts (runtime only)")
    parser.add_argument("--pipeline", choices=("old", "corrected"), default="corrected",
                        help="metric pipeline (default corrected)")
    parser.add_argument("--both-pipelines", action="store_true",
                        help="compute old and corrected side by side and assert direction stability")
    parser.add_argument("--paired", action="store_true",
                        help="per-seed paired deltas between baseline and BP arms")
    parser.add_argument("--arm-stats", action="store_true",
                        help="arm-level median/mean/std across HV/IGD/Cmax/TEC/TWC/n/runtime")
    parser.add_argument("--json", help="write per-run dump to this path")
    args = parser.parse_args()

    if not args.labels or not args.fronts or len(args.labels) != len(args.fronts):
        parser.error("--labels and --fronts are required with equal length")
    base = set(args.baseline_labels or [])
    summaries = args.summaries or [None] * len(args.fronts)
    if len(summaries) != len(args.fronts):
        parser.error("--summaries must be aligned with --fronts")

    fronts = [(lab, read_front(path)) for lab, path in zip(args.labels, args.fronts)]
    ref = nondominated([p for _, f in fronts for p in f])

    rows = []
    for (lab, f), summary_path in zip(fronts, summaries):
        m = metrics(f, ref, args.pipeline)
        m.update(min_objectives(f))
        m["runtimeS"] = runtime_seconds(summary_path)
        m["seed"] = seed_of(lab)
        m["arm"] = arm_of(lab, base)
        rows.append((lab, m))
        print(f"{lab}\tn={m['n']}\tHV={m['hv']:.6f}\tIGD={m['igd']:.6f}\t"
              f"spacing={m['spacing']:.6f}\tcFwd={m['cFwd']:.6f}\tcRev={m['cRev']:.6f}\t"
              f"rawN={m['rawN']}\tminCmax={m['minCmax']:.2f}\tminTEC={m['minTEC']:.2f}\t"
              f"minTWC={m['minTWC']:.2f}")
    print(f"REFERENCE_SIZE={len(ref)}")
    print(f"PIPELINE={args.pipeline}")

    def avg(rows_, key, bucket):
        vals = [row[1][key] for row in rows_ if row[0] in bucket]
        return (sum(vals) / len(vals)) if vals else float("nan")

    for key in ("hv", "igd", "spacing"):
        b = avg(rows, key, base)
        print(f"BASE_AVG_{key}={b:.6f}")
    for lab, m in rows:
        if lab not in base:
            for key, thr_name, thr in (("hv", "HV_REG", -0.02), ("igd", "IGD_REG", 0.10),
                                       ("hv", "HV_VETO", -0.05), ("igd", "IGD_VETO", 0.10)):
                rel = (m[key] - avg(rows, key, base)) / max(EPS, avg(rows, key, base))
                fail = (thr_name == "HV_REG" and rel < thr) or (thr_name == "IGD_REG" and rel > thr) \
                    or (thr_name == "HV_VETO" and rel < thr) or (thr_name == "IGD_VETO" and rel > thr)
                print(f"{lab}_{thr_name}_{key}=rel{rel:+.4f} {'FAIL' if fail else 'pass'}")

    if args.both_pipelines:
        print("\n=== OLD vs CORRECTED PIPELINE COMPARISON ===")
        rows_old, rows_new = [], []
        for (lab, f), _ in zip(fronts, summaries):
            mo = metrics(f, ref, "old")
            mo.update(min_objectives(f))
            rows_old.append((lab, mo))
            mn = metrics(f, ref, "corrected")
            mn.update(min_objectives(f))
            rows_new.append((lab, mn))
        print(f"{'label':<28}{'n(old->new)':>20}{'HV(old->new)':>24}{'IGD(old->new)':>24}")
        for (lab, mo), (lab2, mn) in zip(rows_old, rows_new):
            print(f"{lab:<28}{mo['n']:>8d}->{mn['n']:<8d}{mo['hv']:.6f}->{mn['hv']:.6f}"
                  f"{mo['igd']:.6f}->{mn['igd']:.6f}")
        # direction stability: sign of (BP_arm - baseline_arm) median delta per metric
        def arm_median(rows_, bucket, key):
            vals = [r[1][key] for r in rows_ if r[0] in bucket]
            return median(vals)
        for key in ("hv", "igd"):
            old_med_base = arm_median(rows_old, base, key)
            old_med_bp = arm_median(rows_old, set(l for l, _ in rows_old) - base, key)
            new_med_base = arm_median(rows_new, base, key)
            new_med_bp = arm_median(rows_new, set(l for l, _ in rows_new) - base, key)
            # higher-is-better for HV, lower-is-better for IGD
            better = (lambda bp, b: bp > b) if key == "hv" else (lambda bp, b: bp < b)
            old_dir = "improve" if better(old_med_bp, old_med_base) else "degrade" \
                if better(old_med_base, old_med_bp) else "flat"
            new_dir = "improve" if better(new_med_bp, new_med_base) else "degrade" \
                if better(new_med_base, new_med_bp) else "flat"
            print(f"direction[{key}]: old base={old_med_base:.6f} bp={old_med_bp:.6f} -> {old_dir} | "
                  f"corrected base={new_med_base:.6f} bp={new_med_bp:.6f} -> {new_dir} | "
                  f"direction unchanged: {old_dir == new_dir}")

    if args.paired:
        print("\n=== PAIRED SEED DELTAS (per-seed baseline vs BP) ===")
        by_seed = defaultdict(dict)
        for lab, m in rows:
            if m["seed"]:
                by_seed[m["seed"]][m["arm"]] = (lab, m)
        header = (f"{'seed':<10}{'baseHV':>10}{'bpHV':>10}{'dHV':>10}{'baseIGD':>10}"
                  f"{'bpIGD':>10}{'dIGD':>10}{'baseCmax':>10}{'bpCmax':>10}{'dCmax':>10}")
        print(header)
        for seed in sorted(by_seed):
            arms = by_seed[seed]
            if "baseline" not in arms or "bp" not in arms:
                continue
            (bl, b), (pl, p) = arms["baseline"], arms["bp"]
            d_hv = (p["hv"] - b["hv"]) / max(EPS, b["hv"])
            d_igd = (p["igd"] - b["igd"]) / max(EPS, b["igd"])
            d_cmax = (p["minCmax"] - b["minCmax"]) / max(EPS, b["minCmax"])
            veto = "VETO" if (d_hv < -0.05 and d_igd > 0.20) else ""
            print(f"{seed:<10}{b['hv']:>10.6f}{p['hv']:>10.6f}{d_hv:>+10.4%}"
                  f"{b['igd']:>10.6f}{p['igd']:>10.6f}{d_igd:>+10.4%}"
                  f"{b['minCmax']:>10.2f}{p['minCmax']:>10.2f}{d_cmax:>+10.4%} {veto}")

    if args.arm_stats:
        print("\n=== ARM-LEVEL SUMMARY (median / mean / std) ===")
        arms = {"baseline": [r for r in rows if r[0] in base],
                "bp": [r for r in rows if r[0] not in base]}
        for key in ("hv", "igd", "minCmax", "minTEC", "minTWC", "n"):
            print(f"{key:<10}" + "".join(
                f"{name:<22}" + "".join(f"{fn([r[1][key] for r in bucket]):>12.4g}" for fn in
                                        (median, mean, std)) for name, bucket in arms.items()))
        for name, bucket in arms.items():
            nz = [r[1]["runtimeS"] for r in bucket if r[1]["runtimeS"] is not None]
            if nz:
                print(f"runtimeS[{name}] median={median(nz):.2f}s mean={mean(nz):.2f}s "
                      f"std={std(nz):.2f}s (n={len(nz)})")

    if args.json:
        dump = {"instance": args.instance, "pipeline": args.pipeline,
                "referenceSize": len(ref),
                "runs": [{"label": lab, "seed": m["seed"], "arm": m["arm"],
                          "hv": m["hv"], "igd": m["igd"], "spacing": m["spacing"],
                          "n": m["n"], "rawN": m["rawN"],
                          "minCmax": m["minCmax"], "minTEC": m["minTEC"], "minTWC": m["minTWC"],
                          "runtimeS": m["runtimeS"]} for lab, m in rows]}
        with open(args.json, "w", encoding="utf-8") as handle:
            json.dump(dump, handle, indent=2, ensure_ascii=False)
        print(f"\nJSON_WRITTEN={args.json}")


if __name__ == "__main__":
    main()