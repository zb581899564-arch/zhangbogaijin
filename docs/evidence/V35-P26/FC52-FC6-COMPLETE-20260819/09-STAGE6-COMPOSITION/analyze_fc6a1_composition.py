#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""FC-6A.1: PDDR population composition analysis.

Parses fc6diagComp lines from the 12 stage6 runs ({20,100}-job x {QGS,BASE} x 3
seeds) and produces the user-spec SS12 distribution table:

  per (algorithm, instance) x {mergePoolSize, N_ND, N_LT1, N_EQ1, N_GT1,
  selLT1, selEq1, selGt1, rejLT1, rejEq1} -> median / Q1 / Q3 / min / max
  plus R_C = N_LT1/100, R_B = N_EQ1/100, P(N_LT1>100), P(N_ND>100).

Usage:
  python analyze_fc6a1_composition.py <stage6-root> <out-dir>
    stage6-root: local dir holding <instance>/<arm>/seed-*/... (mirrors server
                 layout; QGS summaries live under runs/seed-*/HMOPSO_QGS_F/)
Output:
  <out-dir>/composition_per_round.csv   (all rounds, all 12 runs, raw)
  <out-dir>/composition_summary.csv     (SS12 aggregation table)
  <out-dir>/composition_by_seed.csv     (per-seed medians for drift check)
"""
import csv
import math
import os
import re
import sys

ARMS = ("QGS", "BASE")
SEEDS = ("20260822", "20260823", "20260824")
INSTANCES = ("100_2_3_1", "20_2_3_1")
FIELD_RE = re.compile(r"(\w+)=([-\d.]+)")


def parse_comp_lines(summary_path):
    rows = []
    with open(summary_path, "r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            if not line.startswith("fc6diagComp "):
                continue
            fields = dict(FIELD_RE.findall(line))
            if not fields:
                continue
            cycle = int(line.split()[1])
            rows.append({
                "cycle": cycle,
                "fe": int(fields.get("fe", -1)),
                "pool": int(fields.get("pool", -1)),
                "target": int(fields.get("target", -1)),
                "nLT1": int(fields.get("nLT1", -1)),
                "nEq1": int(fields.get("nEq1", -1)),
                "nGt1": int(fields.get("nGt1", -1)),
                "nND": int(fields.get("nND", -1)),
                "selLT1": int(fields.get("selLT1", -1)),
                "selEq1": int(fields.get("selEq1", -1)),
                "selGt1": int(fields.get("selGt1", -1)),
                "rejLT1": int(fields.get("rejLT1", -1)),
                "rejEq1": int(fields.get("rejEq1", -1)),
                "rejGt1": int(fields.get("rejGt1", -1)),
                "bpLT1": int(fields.get("bpLT1", -1)),
                "bpEq1": int(fields.get("bpEq1", -1)),
                "bpGt1": int(fields.get("bpGt1", -1)),
            })
    return rows


def find_summary(stage_root, instance, arm, seed):
    base = os.path.join(stage_root, instance, arm, "seed-" + seed)
    if arm == "QGS":
        cand = os.path.join(base, "runs", "seed-" + seed, "HMOPSO_QGS_F",
                            "mechanism-summary.txt")
    else:
        cand = os.path.join(base, "mechanism-summary.txt")
    return cand if os.path.isfile(cand) else None


def quantile(sorted_vals, q):
    if not sorted_vals:
        return float("nan")
    if len(sorted_vals) == 1:
        return float(sorted_vals[0])
    pos = (len(sorted_vals) - 1) * q
    lo = int(math.floor(pos))
    hi = int(math.ceil(pos))
    if lo == hi:
        return float(sorted_vals[lo])
    return sorted_vals[lo] + (sorted_vals[hi] - sorted_vals[lo]) * (pos - lo)


def stats_block(values):
    s = sorted(values)
    return {
        "median": quantile(s, 0.5),
        "q1": quantile(s, 0.25),
        "q3": quantile(s, 0.75),
        "min": float(s[0]) if s else float("nan"),
        "max": float(s[-1]) if s else float("nan"),
    }


def fmt(x):
    if isinstance(x, float):
        if math.isnan(x):
            return "NaN"
        return ("%.4f" % x).rstrip("0").rstrip(".") if abs(x) < 1000 else "%.1f" % x
    return str(x)


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(2)
    stage_root, out_dir = sys.argv[1], sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)

    all_rows = []
    per_run = {}
    missing = []
    for instance in INSTANCES:
        for arm in ARMS:
            for seed in SEEDS:
                summary = find_summary(stage_root, instance, arm, seed)
                if summary is None:
                    missing.append("%s/%s/seed-%s" % (instance, arm, seed))
                    continue
                rows = parse_comp_lines(summary)
                per_run[(instance, arm, seed)] = rows
                for row in rows:
                    flat = {"instance": instance, "arm": arm, "seed": seed}
                    flat.update(row)
                    all_rows.append(flat)

    if missing:
        print("MISSING RUNS: %s" % ", ".join(missing))
    if not all_rows:
        print("NO DATA PARSED")
        sys.exit(1)

    # ---- per-round raw CSV ----
    per_round_path = os.path.join(out_dir, "composition_per_round.csv")
    cols = ["instance", "arm", "seed", "cycle", "fe", "pool", "target",
            "nLT1", "nEq1", "nGt1", "nND", "selLT1", "selEq1", "selGt1",
            "rejLT1", "rejEq1", "rejGt1", "bpLT1", "bpEq1", "bpGt1"]
    with open(per_round_path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.writer(fh)
        writer.writerow(cols)
        for row in all_rows:
            writer.writerow([row[c] for c in cols])
    print("wrote %s (%d rounds)" % (per_round_path, len(all_rows)))

    # ---- SS12 summary table ----
    metrics = [("mergePoolSize", "pool"), ("N_ND", "nND"), ("N_LT1", "nLT1"),
               ("N_EQ1", "nEq1"), ("N_GT1", "nGt1"), ("selLT1", "selLT1"),
               ("selEq1", "selEq1"), ("selGt1", "selGt1"),
               ("rejLT1", "rejLT1"), ("rejEq1", "rejEq1")]
    summary_path = os.path.join(out_dir, "composition_summary.csv")
    with open(summary_path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.writer(fh)
        writer.writerow(["algorithm", "instance", "metric", "rounds",
                         "median", "Q1", "Q3", "min", "max"])
        for instance in INSTANCES:
            for arm in ARMS:
                runs = [r for (inst, a, s), rows in per_run.items()
                        if inst == instance and a == arm for r in rows]
                if not runs:
                    continue
                for label, key in metrics:
                    block = stats_block([r[key] for r in runs])
                    writer.writerow([arm, instance, label, len(runs),
                                     fmt(block["median"]), fmt(block["q1"]),
                                     fmt(block["q3"]), fmt(block["min"]),
                                     fmt(block["max"])])
                # ratios and exceedance probabilities
                rc = stats_block([r["nLT1"] / 100.0 for r in runs])
                writer.writerow([arm, instance, "R_C=N_LT1/100", len(runs),
                                 fmt(rc["median"]), fmt(rc["q1"]),
                                 fmt(rc["q3"]), fmt(rc["min"]), fmt(rc["max"])])
                rb = stats_block([r["nEq1"] / 100.0 for r in runs])
                writer.writerow([arm, instance, "R_B=N_EQ1/100", len(runs),
                                 fmt(rb["median"]), fmt(rb["q1"]),
                                 fmt(rb["q3"]), fmt(rb["min"]), fmt(rb["max"])])
                writer.writerow([arm, instance, "P(N_LT1>100)", len(runs),
                                 fmt(sum(1 for r in runs if r["nLT1"] > 100)
                                     / len(runs)), "", "", "", ""])
                writer.writerow([arm, instance, "P(N_ND>100)", len(runs),
                                 fmt(sum(1 for r in runs if r["nND"] > 100)
                                     / len(runs)), "", "", "", ""])
                writer.writerow([arm, instance, "P(pool>N)", len(runs),
                                 fmt(sum(1 for r in runs
                                         if r["pool"] > 2 * r["target"])
                                     / len(runs)), "", "", "", ""])
    print("wrote %s" % summary_path)

    # ---- per-seed medians (drift check) ----
    seed_path = os.path.join(out_dir, "composition_by_seed.csv")
    with open(seed_path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.writer(fh)
        writer.writerow(["algorithm", "instance", "seed", "rounds",
                         "pool_med", "nND_med", "nLT1_med", "nEq1_med",
                         "nGt1_med", "selLT1_med", "selEq1_med",
                         "rejEq1_med", "rejLT1_med"])
        for instance in INSTANCES:
            for arm in ARMS:
                for seed in SEEDS:
                    rows = per_run.get((instance, arm, seed), [])
                    if not rows:
                        continue
                    writer.writerow([arm, instance, seed, len(rows),
                                     fmt(quantile(sorted(r["pool"] for r in rows), .5)),
                                     fmt(quantile(sorted(r["nND"] for r in rows), .5)),
                                     fmt(quantile(sorted(r["nLT1"] for r in rows), .5)),
                                     fmt(quantile(sorted(r["nEq1"] for r in rows), .5)),
                                     fmt(quantile(sorted(r["nGt1"] for r in rows), .5)),
                                     fmt(quantile(sorted(r["selLT1"] for r in rows), .5)),
                                     fmt(quantile(sorted(r["selEq1"] for r in rows), .5)),
                                     fmt(quantile(sorted(r["rejEq1"] for r in rows), .5)),
                                     fmt(quantile(sorted(r["rejLT1"] for r in rows), .5))])
    print("wrote %s" % seed_path)

    # ---- console digest ----
    print("\n=== SS12 digest (median over all rounds x 3 seeds) ===")
    for instance in INSTANCES:
        for arm in ARMS:
            runs = [r for (inst, a, s), rows in per_run.items()
                    if inst == instance and a == arm for r in rows]
            if not runs:
                continue
            n = len(runs)
            med = {key: quantile(sorted(r[key] for r in runs), .5)
                   for _, key in metrics}
            p_lt1 = sum(1 for r in runs if r["nLT1"] > 100) / n
            p_nd = sum(1 for r in runs if r["nND"] > 100) / n
            print("%s %s: rounds=%d pool=%.0f nND=%.0f nLT1=%.0f nEq1=%.0f "
                  "nGt1=%.0f | selLT1=%.0f selEq1=%.0f rejLT1=%.0f rejEq1=%.0f "
                  "| P(nLT1>100)=%.3f P(nND>100)=%.3f"
                  % (instance, arm, n, med["pool"], med["nND"], med["nLT1"],
                     med["nEq1"], med["nGt1"], med["selLT1"], med["selEq1"],
                     med["rejLT1"], med["rejEq1"], p_lt1, p_nd))


if __name__ == "__main__":
    main()
