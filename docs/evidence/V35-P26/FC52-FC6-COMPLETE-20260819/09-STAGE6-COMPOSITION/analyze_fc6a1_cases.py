#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""FC-6A.1 supplementary analyses on tables/composition_per_round.csv.

Produces:
  - case_classification.txt: rounds by user-spec Case A/B/C per arm/instance
  - eq1_fate.txt: boundary-isolated ND (score==1) selected-vs-rejected fate,
    squeeze-rule violation check, early/late phase drift
  - bp_counterfactual_check.txt: bp* columns vs orig sel* (must be 0 diff)
"""
import csv
import os
import sys
from collections import defaultdict

INT_COLS = ("cycle", "fe", "pool", "target", "nLT1", "nEq1", "nGt1", "nND",
            "selLT1", "selEq1", "selGt1", "rejLT1", "rejEq1", "rejGt1",
            "bpLT1", "bpEq1", "bpGt1")


def load(path):
    with open(path, encoding="utf-8") as fh:
        rows = list(csv.DictReader(fh))
    for r in rows:
        for k in INT_COLS:
            r[k] = int(r[k])
    return rows


def main():
    src = sys.argv[1]
    out_dir = sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)
    rows = load(src)
    groups = defaultdict(list)
    by_run = defaultdict(list)
    for r in rows:
        groups[(r["arm"], r["instance"])].append(r)
        by_run[(r["arm"], r["instance"], r["seed"])].append(r)

    with open(os.path.join(out_dir, "case_classification.txt"), "w",
              encoding="utf-8") as fh:
        fh.write("Case A: N_ND<=100 | Case B: N_ND>100 & N_LT1<=100 | "
                 "Case C: N_LT1>100\n\n")
        for key in sorted(groups):
            rs = groups[key]
            n = len(rs)
            a = sum(1 for r in rs if r["nND"] <= 100)
            c = sum(1 for r in rs if r["nLT1"] > 100)
            b = n - a - c
            fh.write("%s %s: rounds=%d  A=%d (%.1f%%)  B=%d (%.1f%%)  "
                     "C=%d (%.1f%%)\n"
                     % (key[0], key[1], n, a, 100 * a / n, b, 100 * b / n,
                        c, 100 * c / n))

    with open(os.path.join(out_dir, "eq1_fate.txt"), "w",
              encoding="utf-8") as fh:
        fh.write("Eq1 = boundary isolated nondominated (q=0,p=0,score==1.0)\n"
                 "LT1 = center strong nondominated (q=0,p>=1,score<1)\n\n")
        for key in sorted(groups):
            rs = groups[key]
            tot_eq1 = sum(r["nEq1"] for r in rs)
            sel_eq1 = sum(r["selEq1"] for r in rs)
            rej_eq1 = sum(r["rejEq1"] for r in rs)
            tot_lt1 = sum(r["nLT1"] for r in rs)
            rej_lt1 = sum(r["rejLT1"] for r in rs)
            fh.write("%s %s: Eq1 total=%d sel=%d rej=%d (rej %.1f%%) | "
                     "LT1 total=%d rej=%d (rej %.1f%%)\n"
                     % (key[0], key[1], tot_eq1, sel_eq1, rej_eq1,
                        100 * rej_eq1 / tot_eq1 if tot_eq1 else 0,
                        tot_lt1, rej_lt1,
                        100 * rej_lt1 / tot_lt1 if tot_lt1 else 0))
        fh.write("\nSqueeze rule (nLT1>=100 => all Eq1 rejected):\n")
        checked = viol = 0
        for r in rows:
            if r["nLT1"] >= 100 and r["nEq1"] > 0:
                checked += 1
                if r["selEq1"] != 0:
                    viol += 1
        fh.write("  rounds with nLT1>=100 & nEq1>0: %d, violations: %d\n"
                 % (checked, viol))
        fh.write("\nEarly vs late phase P(N_LT1>100) (per run, last third "
                 "vs earlier):\n")
        for key in sorted(groups):
            early_n = early_c = late_n = late_c = 0
            for (arm, inst, seed), rs in by_run.items():
                if (arm, inst) != key:
                    continue
                rs = sorted(rs, key=lambda x: x["cycle"])
                k = max(1, len(rs) // 3)
                for r in rs[:len(rs) - k]:
                    early_n += 1
                    early_c += 1 if r["nLT1"] > 100 else 0
                for r in rs[len(rs) - k:]:
                    late_n += 1
                    late_c += 1 if r["nLT1"] > 100 else 0
            fh.write("  %s %s: early=%.2f (%d rounds)  late=%.2f (%d rounds)\n"
                     % (key[0], key[1], early_c / early_n if early_n else 0,
                        early_n, late_c / late_n if late_n else 0, late_n))

    with open(os.path.join(out_dir, "bp_counterfactual_check.txt"), "w",
              encoding="utf-8") as fh:
        diff = sum(1 for r in rows if (r["bpLT1"], r["bpEq1"], r["bpGt1"])
                   != (r["selLT1"], r["selEq1"], r["selGt1"]))
        fh.write("rounds where BP counterfactual composition differs from "
                 "original selection: %d/%d\n" % (diff, len(rows)))

    for name in ("case_classification.txt", "eq1_fate.txt",
                 "bp_counterfactual_check.txt"):
        path = os.path.join(out_dir, name)
        print("=== %s ===" % name)
        with open(path, encoding="utf-8") as fh:
            print(fh.read())


if __name__ == "__main__":
    main()
