#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""FC-6A.2: Region x PDDR Composition & Probe Analysis.

Parses fc6diagRegion and fc6diagProbe lines from the 12 stage7 runs
({20,100}-job x {QGS,BASE} x 3 seeds) and generates:
1. region_per_round.csv: raw 474-round dataset with all regional breakdown
2. region_summary.csv: SS12 aggregation table with quantiles & ratios
3. region_by_seed.csv: per-seed aggregation to verify stability
4. rejected_nd_attribution.csv: attribution breakdown of all rejected q=0 solutions
5. probe_174_summary.csv: detailed results for the 174.44 counterfactual probe
6. Evaluation of Go / No-Go criteria (logged and structured)
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


def parse_summary_file(summary_path):
    region_rows = []
    probe_rows = []
    region_summary_data = None
    probe_summary_data = None

    if not os.path.isfile(summary_path):
        return region_rows, probe_rows, region_summary_data, probe_summary_data

    with open(summary_path, "r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            line_str = line.strip()
            if line_str.startswith("fc6diagRegion "):
                fields = dict(FIELD_RE.findall(line_str))
                if not fields:
                    continue
                cycle = int(line_str.split()[1])
                region_rows.append({
                    "cycle": cycle,
                    "fe": int(fields.get("fe", -1)),
                    "pool": int(fields.get("pool", -1)),
                    "target": int(fields.get("target", -1)),
                    "g1Lt1": int(fields.get("g1Lt1", 0)),
                    "g1Eq1": int(fields.get("g1Eq1", 0)),
                    "g2Lt1": int(fields.get("g2Lt1", 0)),
                    "g2Eq1": int(fields.get("g2Eq1", 0)),
                    "g3Lt1": int(fields.get("g3Lt1", 0)),
                    "g3Eq1": int(fields.get("g3Eq1", 0)),
                    "g4Lt1": int(fields.get("g4Lt1", 0)),
                    "g4Eq1": int(fields.get("g4Eq1", 0)),
                    "ovfLt1": int(fields.get("ovfLt1", 0)),
                    "ovfEq1": int(fields.get("ovfEq1", 0)),
                    "rejG1": int(fields.get("rejG1", 0)),
                    "rejG2": int(fields.get("rejG2", 0)),
                    "rejG3": int(fields.get("rejG3", 0)),
                    "rejG4": int(fields.get("rejG4", 0)),
                    "rejOvf": int(fields.get("rejOvf", 0)),
                    "absorbable": int(fields.get("absorbable", 0)),
                })
            elif line_str.startswith("fc6diagRegionSummary "):
                fields = dict(FIELD_RE.findall(line_str))
                region_summary_data = {
                    "rounds": int(fields.get("rounds", 0)),
                    "ovfRounds": int(fields.get("ovfRounds", 0)),
                    "totalRejNd": int(fields.get("totalRejNd", 0)),
                    "totalAbsorbable": int(fields.get("totalAbsorbable", 0)),
                    "totalOvf": int(fields.get("totalOvf", 0)),
                }
            elif line_str.startswith("fc6diagProbe "):
                parts = line_str.split("\t")
                cycle_part = parts[0].split()
                cycle = int(cycle_part[1]) if len(cycle_part) > 1 else -1
                fe_val = -1
                global_val = ""
                region_val = ""
                for p in parts[1:]:
                    if p.startswith("fe="):
                        fe_val = int(p.split("=")[1])
                    elif p.startswith("global="):
                        global_val = p.split("=")[1]
                    elif p.startswith("region="):
                        region_val = p.split("=")[1]
                probe_rows.append({
                    "cycle": cycle,
                    "fe": fe_val,
                    "global": global_val,
                    "region": region_val,
                })
            elif line_str.startswith("fc6diagProbeSummary "):
                fields = dict(FIELD_RE.findall(line_str))
                probe_summary_data = {
                    "rounds": int(fields.get("rounds", 0)),
                    "globalYes": int(fields.get("globalYes", 0)),
                    "regionYes": int(fields.get("regionYes", 0)),
                    "regionG1": int(fields.get("regionG1", 0)),
                }

    return region_rows, probe_rows, region_summary_data, probe_summary_data


def find_summary(stage_root, instance, arm, seed):
    base = os.path.join(stage_root, instance, arm, "seed-" + seed)
    if arm == "QGS":
        cand = os.path.join(base, "runs", "seed-" + seed, "HMOPSO_QGS_F", "mechanism-summary.txt")
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


def main():
    if len(sys.argv) < 3:
        stage_root = os.path.abspath("raw")
        out_dir = os.path.abspath("tables")
    else:
        stage_root = os.path.abspath(sys.argv[1])
        out_dir = os.path.abspath(sys.argv[2])

    os.makedirs(out_dir, exist_ok=True)
    print(f"[FC-6A.2 Analyze] stage_root={stage_root}, out_dir={out_dir}")

    all_per_round = []
    all_probes = []
    summary_by_run = {}

    for inst in INSTANCES:
        for arm in ARMS:
            for seed in SEEDS:
                path = find_summary(stage_root, inst, arm, seed)
                if not path:
                    print(f"[WARN] missing summary: {inst} {arm} {seed}")
                    continue
                r_rows, p_rows, r_sum, p_sum = parse_summary_file(path)
                key = (inst, arm, seed)
                summary_by_run[key] = {
                    "r_rows": r_rows,
                    "p_rows": p_rows,
                    "r_sum": r_sum,
                    "p_sum": p_sum,
                }

                for r in r_rows:
                    g1Total = r["g1Lt1"] + r["g1Eq1"]
                    g2Total = r["g2Lt1"] + r["g2Eq1"]
                    g3Total = r["g3Lt1"] + r["g3Eq1"]
                    g4Total = r["g4Lt1"] + r["g4Eq1"]
                    ovfTotal = r["ovfLt1"] + r["ovfEq1"]
                    ndTotal = g1Total + g2Total + g3Total + g4Total + ovfTotal
                    totalRej = r["rejG1"] + r["rejG2"] + r["rejG3"] + r["rejG4"] + r["rejOvf"]
                    
                    absorbRate = (r["absorbable"] / totalRej) if totalRej > 0 else float("nan")
                    ovfRate = (ovfTotal / ndTotal) if ndTotal > 0 else 0.0

                    row_dict = {
                        "instance": inst,
                        "arm": arm,
                        "seed": seed,
                        "cycle": r["cycle"],
                        "fe": r["fe"],
                        "pool": r["pool"],
                        "ndTotal": ndTotal,
                        "g1Lt1": r["g1Lt1"], "g1Eq1": r["g1Eq1"], "g1Total": g1Total,
                        "g2Lt1": r["g2Lt1"], "g2Eq1": r["g2Eq1"], "g2Total": g2Total,
                        "g3Lt1": r["g3Lt1"], "g3Eq1": r["g3Eq1"], "g3Total": g3Total,
                        "g4Lt1": r["g4Lt1"], "g4Eq1": r["g4Eq1"], "g4Total": g4Total,
                        "ovfLt1": r["ovfLt1"], "ovfEq1": r["ovfEq1"], "ovfTotal": ovfTotal,
                        "rejG1": r["rejG1"], "rejG2": r["rejG2"], "rejG3": r["rejG3"], "rejG4": r["rejG4"],
                        "rejOvf": r["rejOvf"], "rejTotal": totalRej,
                        "absorbable": r["absorbable"],
                        "absorbRate": absorbRate,
                        "ovfRate": ovfRate,
                    }
                    all_per_round.append(row_dict)

                for p in p_rows:
                    all_probes.append({
                        "instance": inst,
                        "arm": arm,
                        "seed": seed,
                        "cycle": p["cycle"],
                        "fe": p["fe"],
                        "global": p["global"],
                        "region": p["region"],
                    })

    # 1. Output region_per_round.csv
    per_round_path = os.path.join(out_dir, "region_per_round.csv")
    if all_per_round:
        with open(per_round_path, "w", newline="", encoding="utf-8") as fh:
            writer = csv.DictWriter(fh, fieldnames=list(all_per_round[0].keys()))
            writer.writeheader()
            writer.writerows(all_per_round)
        print(f"Wrote {len(all_per_round)} rounds to {per_round_path}")

    # 2. Output rejected_nd_attribution.csv
    # Aggregate attribution per (instance, arm)
    attr_path = os.path.join(out_dir, "rejected_nd_attribution.csv")
    attr_rows = []
    for inst in INSTANCES:
        for arm in ARMS:
            subset = [r for r in all_per_round if r["instance"] == inst and r["arm"] == arm]
            tot_rej = sum(r["rejTotal"] for r in subset)
            tot_rejG1 = sum(r["rejG1"] for r in subset)
            tot_rejG2 = sum(r["rejG2"] for r in subset)
            tot_rejG3 = sum(r["rejG3"] for r in subset)
            tot_rejG4 = sum(r["rejG4"] for r in subset)
            tot_rejOvf = sum(r["rejOvf"] for r in subset)
            tot_absorbable = sum(r["absorbable"] for r in subset)
            
            p_rejG1 = (tot_rejG1 / tot_rej * 100) if tot_rej > 0 else 0.0
            p_rejG2 = (tot_rejG2 / tot_rej * 100) if tot_rej > 0 else 0.0
            p_rejG3 = (tot_rejG3 / tot_rej * 100) if tot_rej > 0 else 0.0
            p_rejG4 = (tot_rejG4 / tot_rej * 100) if tot_rej > 0 else 0.0
            p_rejOvf = (tot_rejOvf / tot_rej * 100) if tot_rej > 0 else 0.0
            p_absorb = (tot_absorbable / tot_rej * 100) if tot_rej > 0 else 0.0

            # Medians across rounds with rejected ND
            rej_rounds = [r for r in subset if r["rejTotal"] > 0]
            med_absorb_rate = quantile(sorted([r["absorbRate"] for r in rej_rounds]), 0.5) * 100 if rej_rounds else 0.0

            attr_rows.append({
                "instance": inst,
                "arm": arm,
                "rounds": len(subset),
                "rejRounds": len(rej_rounds),
                "totalRejNd": tot_rej,
                "rejG1": tot_rejG1, "pctG1": f"{p_rejG1:.1f}%",
                "rejG2": tot_rejG2, "pctG2": f"{p_rejG2:.1f}%",
                "rejG3": tot_rejG3, "pctG3": f"{p_rejG3:.1f}%",
                "rejG4": tot_rejG4, "pctG4": f"{p_rejG4:.1f}%",
                "rejOvf": tot_rejOvf, "pctOvf": f"{p_rejOvf:.1f}%",
                "totalAbsorbable": tot_absorbable, "aggregateAbsorbPct": f"{p_absorb:.1f}%",
                "medianRoundAbsorbPct": f"{med_absorb_rate:.1f}%",
            })

    with open(attr_path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=list(attr_rows[0].keys()))
        writer.writeheader()
        writer.writerows(attr_rows)
    print(f"Wrote attribution table to {attr_path}")

    # 3. Output region_summary.csv (SS12 style)
    summary_path = os.path.join(out_dir, "region_summary.csv")
    metrics_to_stat = [
        "ndTotal", "g1Total", "g2Total", "g3Total", "g4Total", "ovfTotal",
        "rejTotal", "absorbable", "ovfRate"
    ]
    summary_rows = []
    for inst in INSTANCES:
        for arm in ARMS:
            subset = [r for r in all_per_round if r["instance"] == inst and r["arm"] == arm]
            for metric in metrics_to_stat:
                vals = [r[metric] for r in subset]
                st = stats_block(vals)
                summary_rows.append({
                    "instance": inst,
                    "arm": arm,
                    "metric": metric,
                    "median": f"{st['median']:.2f}",
                    "q1": f"{st['q1']:.2f}",
                    "q3": f"{st['q3']:.2f}",
                    "min": f"{st['min']:.2f}",
                    "max": f"{st['max']:.2f}",
                })

    with open(summary_path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=list(summary_rows[0].keys()))
        writer.writeheader()
        writer.writerows(summary_rows)
    print(f"Wrote summary metrics to {summary_path}")

    # 4. Output probe_174_summary.csv
    probe_path = os.path.join(out_dir, "probe_174_summary.csv")
    probe_summary_rows = []
    for inst in INSTANCES:
        for arm in ARMS:
            for seed in SEEDS:
                key = (inst, arm, seed)
                if key not in summary_by_run:
                    continue
                p_sum = summary_by_run[key]["p_sum"]
                if p_sum and p_sum["rounds"] > 0:
                    g_pct = p_sum["globalYes"] / p_sum["rounds"] * 100
                    r_pct = p_sum["regionYes"] / p_sum["rounds"] * 100
                    g1_pct = p_sum["regionG1"] / p_sum["rounds"] * 100
                    probe_summary_rows.append({
                        "instance": inst,
                        "arm": arm,
                        "seed": seed,
                        "rounds": p_sum["rounds"],
                        "globalYes": p_sum["globalYes"],
                        "globalYesPct": f"{g_pct:.1f}%",
                        "regionYes": p_sum["regionYes"],
                        "regionYesPct": f"{r_pct:.1f}%",
                        "regionG1": p_sum["regionG1"],
                        "regionG1Pct": f"{g1_pct:.1f}%",
                        "ratioRegionOverGlobal": f"{(r_pct / g_pct):.2f}x" if g_pct > 0 else "inf",
                    })

    if probe_summary_rows:
        with open(probe_path, "w", newline="", encoding="utf-8") as fh:
            writer = csv.DictWriter(fh, fieldnames=list(probe_summary_rows[0].keys()))
            writer.writeheader()
            writer.writerows(probe_summary_rows)
        print(f"Wrote probe summary to {probe_path}")

    # 5. Output region_by_seed.csv
    by_seed_path = os.path.join(out_dir, "region_by_seed.csv")
    by_seed_rows = []
    for inst in INSTANCES:
        for arm in ARMS:
            for seed in SEEDS:
                subset = [r for r in all_per_round if r["instance"] == inst and r["arm"] == arm and r["seed"] == seed]
                tot_rej = sum(r["rejTotal"] for r in subset)
                tot_abs = sum(r["absorbable"] for r in subset)
                tot_ovf = sum(r["rejOvf"] for r in subset)
                med_nd = quantile(sorted([r["ndTotal"] for r in subset]), 0.5)
                med_ovf_rate = quantile(sorted([r["ovfRate"] for r in subset]), 0.5) * 100
                abs_pct = (tot_abs / tot_rej * 100) if tot_rej > 0 else 0.0

                by_seed_rows.append({
                    "instance": inst,
                    "arm": arm,
                    "seed": seed,
                    "rounds": len(subset),
                    "medianNd": f"{med_nd:.1f}",
                    "totalRejNd": tot_rej,
                    "totalAbsorbable": tot_abs,
                    "absorbPct": f"{abs_pct:.1f}%",
                    "totalOvf": tot_ovf,
                    "medianOvfRate": f"{med_ovf_rate:.1f}%",
                })

    with open(by_seed_path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=list(by_seed_rows[0].keys()))
        writer.writeheader()
        writer.writerows(by_seed_rows)
    print(f"Wrote by_seed table to {by_seed_path}")

    # Print Key Decision Metrics to stdout
    print("\n" + "="*80)
    print("FC-6A.2 REGION AUDIT SUMMARY & GO/NO-GO EVALUATION")
    print("="*80)
    for r in attr_rows:
        print(f"[{r['arm']} {r['instance']}] Total Rej ND={r['totalRejNd']} | Absorbable={r['totalAbsorbable']} ({r['aggregateAbsorbPct']}) | Rej G1={r['rejG1']} G2={r['rejG2']} G3={r['rejG3']} G4={r['rejG4']} Ovf={r['rejOvf']}")
    print("-" * 80)
    for p in probe_summary_rows:
        print(f"[PROBE 174.44 {p['arm']} {p['instance']} seed{p['seed']}] GlobalYes={p['globalYesPct']} vs RegionYes={p['regionYesPct']} (G1={p['regionG1Pct']}) -> Ratio={p['ratioRegionOverGlobal']}")
    print("="*80 + "\n")


if __name__ == "__main__":
    main()
