#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""GAP-PROBE-V2 analysis: per-instance reference construction, uniform metric
recomputation, pairwise A4 lag gaps, three-band verdict, RED rule, and the
frozen strongest-external selection rule.

Implements the frozen GAP_PROBE_V2_REFERENCE_CONTRACT: metric implementation is
an exact copy of the historical acceptance analyzer (analyze_confirmation.py,
EPS=1e-12). Zero FE; pure post-processing of accepted raw fronts.
"""
import csv
import hashlib
import json
import math
import os
import random
import sys

EPS = 1e-12
HV_REF = 1.1
INSTANCE_DIR = {"50_2_3_1": "run-GAP500-%s-50_2_3_1-%s",
                "100_5_3_1": "run-GAP500-%s-100_5_3_1-%s"}
ARMS = ["A4", "A0", "SPEA2F", "NSGA2F"]
LABEL = {"A4": "A4-Pacing", "A0": "A0(HMOPSO-QGS-F)",
         "SPEA2F": "SPEA2-F", "NSGA2F": "NSGA-II-F"}
SEEDS = ["20260827", "20260906"]


def read_front(path):
    points = []
    with open(path, encoding="utf-8") as stream:
        for line in stream:
            fields = line.strip().split(",")
            if len(fields) < 3 or fields[0].lower() == "cmax":
                continue
            point = tuple(float(v) for v in fields[:3])
            if not all(math.isfinite(v) for v in point):
                raise ValueError("non-finite objective in %s" % path)
            points.append(point)
    if not points:
        raise ValueError("empty front %s" % path)
    return points


def nondominated(points):
    ordered = sorted(set(points))
    if not ordered:
        return []
    ys = sorted({p[1] for p in ordered})
    index_of = {v: i + 1 for i, v in enumerate(ys)}
    tree = [float("inf")] * (len(ys) + 1)

    def query(i):
        a = float("inf")
        while i:
            a = min(a, tree[i]); i -= i & -i
        return a

    def update(i, v):
        while i < len(tree):
            tree[i] = min(tree[i], v); i += i & -i

    accepted = []
    for p in ordered:
        idx = index_of[p[1]]
        if query(idx) <= p[2] + EPS:
            continue
        accepted.append(p)
        update(idx, p[2])
    return accepted


def normalize(points, reference):
    lows = [min(p[i] for p in reference) for i in range(3)]
    highs = [max(p[i] for p in reference) for i in range(3)]
    spans = [max(EPS, highs[i] - lows[i]) for i in range(3)]
    return [tuple((p[i] - lows[i]) / spans[i] for i in range(3)) for p in points], lows, highs


def yz_union(points, ry=HV_REF, rz=HV_REF):
    ordered = sorted(points, key=lambda p: p[1])
    area, min_z, cursor = 0.0, rz, 0
    while cursor < len(ordered):
        y = max(0.0, min(ry, ordered[cursor][1]))
        while cursor < len(ordered) and ordered[cursor][1] <= y + EPS:
            min_z = min(min_z, max(0.0, min(rz, ordered[cursor][2])))
            cursor += 1
        next_y = max(y, min(ry, ordered[cursor][1])) if cursor < len(ordered) else ry
        area += max(0.0, next_y - y) * max(0.0, rz - min_z)
    return area


def hypervolume(points):
    ordered = sorted([tuple(max(0.0, min(HV_REF, v)) for v in p) for p in points],
                     key=lambda p: p[0])
    volume, active, cursor = 0.0, [], 0
    while cursor < len(ordered):
        x = ordered[cursor][0]
        while cursor < len(ordered) and ordered[cursor][0] <= x + EPS:
            active.append(ordered[cursor]); cursor += 1
        next_x = ordered[cursor][0] if cursor < len(ordered) else HV_REF
        volume += max(0.0, next_x - x) * yz_union(active)
    return max(0.0, volume)


def igd(approx, reference):
    return sum(min(math.sqrt(sum((a[i] - r[i]) ** 2 for i in range(3)))
                   for a in approx) for r in reference) / len(reference)


def canonical_text(points):
    lines = ["Cmax,TEC,TWC"]
    for p in sorted(set(points)):
        lines.append(",".join("%.17g" % v for v in p))
    return "\n".join(lines) + "\n"


def canonical_hash(points):
    return hashlib.sha256(canonical_text(points).encode("utf-8")).hexdigest()


def analyze(runs_root, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    os.makedirs(os.path.join(out_dir, "reference-fronts"), exist_ok=True)
    metrics_rows, summary = [], {"instances": {}}
    for inst in ("50_2_3_1", "100_5_3_1"):
        fronts = {}
        for arm in ARMS:
            for seed in SEEDS:
                run_id = "GAP500-%s-%s-%s" % (arm, inst, seed)
                path = os.path.join(runs_root, "run-%s" % run_id, "front.csv")
                fronts[(arm, seed)] = read_front(path)
        all_points = [p for key in fronts for p in fronts[key]]
        pfref = nondominated(all_points)
        shuffled = list(all_points)
        random.Random(20260830).shuffle(shuffled)
        order_ok = canonical_hash(pfref) == canonical_hash(nondominated(shuffled))
        with open(os.path.join(out_dir, "reference-fronts",
                               "pfref-%s.csv" % inst), "w",
                  encoding="utf-8", newline="") as f:
            f.write(canonical_text(pfref))
        ref_norm, lows, highs = normalize(pfref, pfref)
        inst_metrics = {}
        for arm in ARMS:
            for seed in SEEDS:
                norm, _, _ = normalize(fronts[(arm, seed)], pfref)
                row = {"instance": inst, "seed": seed, "arm": LABEL[arm],
                       "frontSize": len(fronts[(arm, seed)]),
                       "hv": hypervolume(norm), "igd": igd(norm, ref_norm),
                       "minCmax": min(p[0] for p in fronts[(arm, seed)]),
                       "minTEC": min(p[1] for p in fronts[(arm, seed)]),
                       "minTWC": min(p[2] for p in fronts[(arm, seed)])}
                metrics_rows.append(row)
                inst_metrics[(arm, seed)] = row
        gaps, bands = [], []
        for comp, comp_label in (("A0", "A0(HMOPSO-QGS-F)"), ("SPEA2F", "SPEA2-F"),
                                 ("NSGA2F", "NSGA-II-F")):
            gh = [ (inst_metrics[(comp, s)]["hv"] - inst_metrics[("A4", s)]["hv"])
                   / inst_metrics[(comp, s)]["hv"] for s in SEEDS ]
            gi = [ (inst_metrics[("A4", s)]["igd"] - inst_metrics[(comp, s)]["igd"])
                   / inst_metrics[(comp, s)]["igd"] for s in SEEDS ]
            med_gh = (gh[0] + gh[1]) / 2.0
            med_gi = (gi[0] + gi[1]) / 2.0
            g = max(med_gh, med_gi)
            band = "GAP_WITHIN_5" if g <= 0.05 else (
                   "GAP_5_TO_15" if g <= 0.15 else "GAP_GT_15")
            bands.append(band)
            gaps.append({"instance": inst, "competitor": LABEL[comp],
                         "gapHV_s20260827": gh[0], "gapHV_s20260906": gh[1],
                         "medianGapHV": med_gh,
                         "gapIGD_s20260827": gi[0], "gapIGD_s20260906": gi[1],
                         "medianGapIGD": med_gi, "band": band,
                         "gapHV": gh, "gapIGD": gi})
        verdict_band = ("GAP_WITHIN_5" if all(b == "GAP_WITHIN_5" for b in bands)
                        else "GAP_5_TO_15" if all(b in ("GAP_WITHIN_5", "GAP_5_TO_15")
                                                  for b in bands)
                        else "GAP_GT_15")
        summary["instances"][inst] = {
            "pfrefPoints": len(pfref),
            "pfrefCanonicalSha256": canonical_hash(pfref),
            "orderIndependent": order_ok,
            "gaps": [{k: v for k, v in g.items() if k not in ("gapHV", "gapIGD")}
                     for g in gaps],
            "verdictBand": verdict_band}
        summary.setdefault("seedGaps", {})[inst] = {
            LABEL[c]: [{"seed": s, "gapHV":
                        (inst_metrics[(c, s)]["hv"] - inst_metrics[("A4", s)]["hv"])
                        / inst_metrics[(c, s)]["hv"],
                        "gapIGD":
                        (inst_metrics[("A4", s)]["igd"] - inst_metrics[(c, s)]["igd"])
                        / inst_metrics[(c, s)]["igd"]}
                       for s in SEEDS]
            for c in ("A0", "SPEA2F", "NSGA2F")}

    # RED rule: same competitor, same primary metric, >15% lag on BOTH instances AND BOTH seeds
    red = False
    red_detail = []
    for comp in ("A0(HMOPSO-QGS-F)", "SPEA2-F", "NSGA-II-F"):
        for metric in ("gapHV", "gapIGD"):
            per_inst = []
            for inst in ("50_2_3_1", "100_5_3_1"):
                vals = [g[metric] for g in summary["seedGaps"][inst][comp]]
                per_inst.append(all(v > 0.15 for v in vals))
            if all(per_inst):
                red = True
                red_detail.append({"competitor": comp, "metric": metric})
    summary["red"] = red
    summary["redDetail"] = red_detail
    overall = ("GAP_GT_15" if any(d["verdictBand"] == "GAP_GT_15"
                                  for d in summary["instances"].values())
               else "GAP_5_TO_15" if any(d["verdictBand"] == "GAP_5_TO_15"
                                         for d in summary["instances"].values())
               else "GAP_WITHIN_5")
    summary["overallBand"] = ("BLOCKED_REFERENCE_OR_RUNS" if red is None else overall)

    # strongest external: mean rank of HV and IGD across 2 instances x 2 seeds
    rank_acc = {LABEL[a]: {"hv": [], "igd": []}
                for a in ("A0", "SPEA2F", "NSGA2F")}
    for inst in ("50_2_3_1", "100_5_3_1"):
        for seed in SEEDS:
            for metric, key in (("hv", "hv"), ("igd", "igd")):
                ordered = sorted(
                    [((LABEL[a], seed), inst_metrics[(a, seed)][metric])
                     for a in ("A4", "A0", "SPEA2F", "NSGA2F")],
                    key=lambda kv: kv[1])
                for rank, (key_arm, _) in enumerate(ordered, 1):
                    if key_arm[0] in rank_acc:
                        rank_acc[key_arm[0]][key].append(rank)
    selection = []
    for arm_label, acc in rank_acc.items():
        hv_r = sum(acc["hv"]) / len(acc["hv"])
        igd_r = sum(acc["igd"]) / len(acc["igd"])
        selection.append({"arm": arm_label, "meanRankHV": hv_r,
                          "meanRankIGD": igd_r, "score": (hv_r + igd_r) / 2.0})
    selection.sort(key=lambda s: (s["score"], s["meanRankHV"], s["meanRankIGD"],
                                  s["arm"]))
    summary["externalSelection"] = selection
    summary["strongestExternal"] = selection[0]["arm"]

    with open(os.path.join(out_dir, "metrics.csv"), "w", encoding="utf-8",
              newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(metrics_rows[0].keys()),
                           lineterminator="\n")
        w.writeheader(); w.writerows(metrics_rows)
    gap_rows = []
    for inst in ("50_2_3_1", "100_5_3_1"):
        for g in summary["instances"][inst]["gaps"]:
            gap_rows.append(g)
    with open(os.path.join(out_dir, "gap-pairwise.csv"), "w", encoding="utf-8",
              newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(gap_rows[0].keys()),
                           lineterminator="\n")
        w.writeheader(); w.writerows(gap_rows)
    with open(os.path.join(out_dir, "external-rank-selection.csv"), "w",
              encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(selection[0].keys()),
                           lineterminator="\n")
        w.writeheader(); w.writerows(selection)
    with open(os.path.join(out_dir, "analysis-summary.json"), "w",
              encoding="utf-8") as f:
        json.dump(summary, f, indent=1, ensure_ascii=False)
    print(json.dumps(summary, indent=1, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(analyze(sys.argv[1], sys.argv[2]))
