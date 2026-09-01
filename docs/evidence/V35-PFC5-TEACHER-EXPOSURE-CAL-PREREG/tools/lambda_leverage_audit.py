#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V35-PFC5-CAL-P0: offline lambda-leverage audit using EXISTING teacher telemetry.

Zero FE. No algorithm run. Reads the frozen 121 runtime 50k ON telemetry that was
produced in 2026-08-26 (instance 100_5_3_1, seed 20260901, arm A4) and estimates:

  1. which Q actions expose a multi-candidate selection point at all;
  2. controller-local exposure concentration per path;
  3. the numerical leverage of the exposure term under the preregistered
     normalization  exposure_i = count_i / total_count_in_controller;
  4. whether the preregistered target flip-rate intervals are reachable.

Run:
  python tools/lambda_leverage_audit.py
"""
import collections
import csv
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PREREG = os.path.dirname(HERE)
TELEMETRY = os.path.normpath(os.path.join(
    PREREG, "..", "V35-FC5-MIDHORIZON-DIAGNOSTICS",
    "26-final-runtime-jar-validation",
    "A4-50k-ON-s20260901-121FBB49",
    "telemetry-teacher-use-events.csv"))

LAMBDA_GRID = [0.05, 0.15, 0.30]


def load():
    with open(TELEMETRY, encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def entropy_normalized(counts):
    total = sum(counts)
    if total <= 0 or len(counts) <= 1:
        return 0.0
    h = -sum((c / total) * math.log(c / total) for c in counts if c > 0)
    return h / math.log(len(counts))


def main():
    rows = load()
    qg = [r for r in rows if r["qSystem"] == "QG"]
    by_action = collections.defaultdict(list)
    for r in qg:
        by_action[r["qAction"]].append(r)

    print("telemetryRows=%d  qgRows=%d  qpRows=%d"
          % (len(rows), len(qg), len(rows) - len(qg)))

    # ---- 1. where can an exposure-aware re-selection physically act? -------
    print("\n=== QG action reachability ===")
    for action in sorted(by_action):
        n = len(by_action[action])
        print("  %-30s n=%4d  share_of_QG=%7.4f  share_of_ALL=%7.4f"
              % (action, n, n / len(qg), n / len(rows)))

    tournament = by_action.get("GLOBAL_ARCHIVE_TOURNAMENT", [])
    if not tournament:
        print("NO TOURNAMENT EVENTS -> cannot calibrate")
        return 1
    print("\nstructuralCeiling_changeableFractionOfQG   = %.6f" % (len(tournament) / len(qg)))
    print("structuralCeiling_changeableFractionOfAll  = %.6f" % (len(tournament) / len(rows)))

    # ---- 2. controller-local exposure concentration ------------------------
    print("\n=== controller-local exposure concentration ===")
    for label, subset in (("QG_ALL", qg), ("QG_TOURNAMENT", tournament),
                          ("QG_HISTORICAL_CACHE", by_action.get("HISTORICAL_CACHE", [])),
                          ("QG_PREVIOUS_CACHE", by_action.get("PREVIOUS_CACHE", []))):
        if not subset:
            continue
        c = collections.Counter(r["teacherFingerprint"] for r in subset)
        vals = sorted(c.values(), reverse=True)
        n = len(subset)
        print("  %-22s n=%4d uniq=%4d top1=%.4f top5=%.4f Hn=%.4f"
              % (label, n, len(c), vals[0] / n, sum(vals[:5]) / n,
                 entropy_normalized(vals)))

    # ---- 3. leverage of  exposure_i = count_i / total_count  ---------------
    print("\n=== exposure-term leverage under preregistered normalization ===")
    counts = collections.Counter(r["teacherFingerprint"] for r in tournament)
    n_total = len(tournament)
    max_count = max(counts.values())
    exposure_vals = {fp: c / n_total for fp, c in counts.items()}
    print("  tournamentEvents=%d uniqueTeachers=%d maxCount=%d meanCount=%.4f"
          % (n_total, len(counts), max_count, n_total / len(counts)))
    print("  exposure_i range            = [%.8f, %.8f]"
          % (min(exposure_vals.values()), max(exposure_vals.values())))
    print("  max |exposure_A - exposure_B| = %.8f"
          % (max(exposure_vals.values()) - min(exposure_vals.values())))

    # pairwise-gap scale of the base loss, proxied by directionalRegret
    regret = []
    for r in tournament:
        raw = r.get("directionalRegret", "")
        try:
            regret.append(float(raw))
        except ValueError:
            pass
    regret.sort()
    if regret:
        med = regret[len(regret) // 2]
        print("  directionalRegret (pool-spread proxy): median=%.6f p25=%.6f p75=%.6f"
              % (med, regret[len(regret) // 4], regret[3 * len(regret) // 4]))
    else:
        med = float("nan")
        print("  directionalRegret: no numeric values")

    print("\n  lambda x max|delta exposure|  vs  median base-loss gap = %.6f" % med)
    for lam in LAMBDA_GRID:
        reach = lam * (max(exposure_vals.values()) - min(exposure_vals.values()))
        print("    lambda=%.2f  ->  max penalty differential = %.8f   (needs > %.6f to flip a median gap)"
              % (lam, reach, med))
    needed = med / (max(exposure_vals.values()) - min(exposure_vals.values()))
    print("\n  lambda required to flip a MEDIAN-gap comparison = %.2f" % needed)

    # ---- 4. reachability of the preregistered target intervals -------------
    print("\n=== reachability of preregistered target flip-rate intervals ===")
    ceiling_qg = len(tournament) / len(qg)
    for label, lo, hi in (("C1", 0.05, 0.15), ("C2", 0.15, 0.35), ("C3", 0.35, 0.60)):
        reach = "REACHABLE" if ceiling_qg > lo else "UNREACHABLE"
        print("  %s target %.0f%%-%.0f%% -> ceiling %.2f%% -> %s"
              % (label, lo * 100, hi * 100, ceiling_qg * 100, reach))

    print("\nverdict=LEVERAGE_AUDIT_COMPLETED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
