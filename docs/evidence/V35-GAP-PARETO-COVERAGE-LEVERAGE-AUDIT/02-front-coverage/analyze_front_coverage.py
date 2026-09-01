# -*- coding: utf-8 -*-
"""analyze_front_coverage.py — Agent B (V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT-V1, 02-front-coverage).

READ-ONLY audit (preregistration AUDIT_PREREGISTRATION.md, frozen 2026-08-31).
Inputs (never modified):
  16-remote-250k-runs/sync/seed-<S>/results/run-GAPL250K-<A>-<I>-<S>/   (18 runs)
      checkpoints/checkpoint-<FE>-decision-front.csv        (candidateFingerprint,Cmax,TEC,TWC)
      checkpoints/checkpoint-<FE>-observed-full-front.csv   (candidateFingerprint,Cmax,TEC,TWC)
      checkpoint-fronts.csv (combined; contains terminal-decision-front and
                             terminal-observed-full-front rows WITH fingerprints)
      front.csv (terminal decision front, Cmax,TEC,TWC only — no fingerprint)
      checkpoints/checkpoint-registry.csv, budget-termination.properties
  17-250k-reference-and-metrics/terminal-reference-fronts/PFref_terminal_<instance>.csv
Outputs (this directory):
  front-coverage-timeseries.csv
  observed-decision-gap.csv
  c0-vs-c3-hv-curve.csv
  (this script and README.md are themselves evidence)

Pipeline (frozen conventions):
  - Metric pipeline = fc6 'corrected': raw dedup -> raw nondominated -> min/max
    normalize from the instance reference WITHOUT clamp -> HV with reference box
    (1.1, 1.1, 1.1) (fc6.hypervolume; coordinates outside the box converge at the
    box boundary as part of the HV definition), IGD in normalized space.
  - Normalization reference: per instance, PFref_terminal (verified equal to the
    rebuild ND(union of all arms x seeds terminal decision fronts)); the SAME
    ideal/nadir is used for checkpoints and terminal, and as the IGD reference set.
  - Set operations are FINGERPRINT-level (real SHA-256 candidate fingerprints);
    ND verification is objective-triple-level (strict ND on unique triples).
  - Terminal decision front: taken from checkpoint-fronts.csv rows
    frontType=terminal-decision-front (these carry fingerprints) and verified
    content-identical to front.csv; fallback (only if verification failed) is
    front.csv with fingerprints resolved by exact decimal-string triple match
    against terminal-observed-full-front, unmatched triples getting a synthetic
    NOFP fingerprint.
"""
import csv
import io
import os
import sys
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
BASE = os.path.abspath(os.path.join(HERE, "..", ".."))  # ...\docs\evidence
sys.path.insert(0, r"E:\学习\李明哲-毕业材料\张博改进\scripts")
import fc6_metrics as fc6  # noqa: E402  (frozen metric pipeline)

SYNC = os.path.join(BASE, "V35-GAP-LOCAL-FE-PACING-REPAIR",
                    "16-remote-250k-runs", "sync")
PFREF_DIR = os.path.join(BASE, "V35-GAP-LOCAL-FE-PACING-REPAIR",
                         "17-250k-reference-and-metrics", "terminal-reference-fronts")

ARMS = ["C0", "C2", "C3"]
SEEDS = ["20260916", "20260917", "20260918"]
INSTANCES = [("50_2_3_1", "normal"), ("100_5_3_1", "difficult")]
CHECKPOINTS = [50000, 100000, 150000, 200000]
TERMINAL_FE = 250000  # requestedMaxFE=actualFE=250000 (budget-termination.properties)
ALL_CKPTS = CHECKPOINTS + [TERMINAL_FE]

EPS = 1e-12
HV_REF = (1.1, 1.1, 1.1)
MATERIAL_HV_RECOVERY = 0.02
MATERIAL_RATIO = 0.10


# ---------------------------------------------------------------- file readers
def read_fp_front(path):
    """Read candidateFingerprint,Cmax,TEC,TWC -> list of (fingerprint, [c,t,w])."""
    rows = []
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.lower().startswith("candidatefingerprint"):
                continue
            p = line.split(",")
            if len(p) < 4:
                continue
            rows.append((p[0], [float(p[1]), float(p[2]), float(p[3])], (p[1], p[2], p[3])))
    return rows


def read_plain_front(path):
    """Read Cmax,TEC,TWC -> list of ([c,t,w], (c,t,w) decimal strings)."""
    rows = []
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.lower().startswith("cmax"):
                continue
            p = line.split(",")
            if len(p) < 3:
                continue
            rows.append(([float(p[0]), float(p[1]), float(p[2])], (p[0], p[1], p[2])))
    return rows


def read_registry(path):
    """checkpoint-registry.csv -> {targetFE: observedFE}."""
    obs = {}
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            p = line.strip().split(",")
            if len(p) < 3 or p[0] == "checkpointTargetFE":
                continue
            try:
                obs[int(p[0])] = int(p[1])
            except ValueError:
                continue
    return obs


def read_combined_terminal(path):
    """checkpoint-fronts.csv -> (terminal decision rows, terminal observed rows,
    {targetFE: observedFE} for terminal rows)."""
    dec, obs, fe = [], [], {}
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            p = line.strip().split(",")
            if len(p) < 8:
                continue
            if p[3] == "terminal-decision-front":
                dec.append((p[4], [float(p[5]), float(p[6]), float(p[7])], (p[5], p[6], p[7])))
                fe[int(p[0])] = int(p[1])
            elif p[3] == "terminal-observed-full-front":
                obs.append((p[4], [float(p[5]), float(p[6]), float(p[7])], (p[5], p[6], p[7])))
                fe[int(p[0])] = int(p[1])
    return dec, obs, fe


def run_dir(arm, inst, seed):
    return os.path.join(SYNC, "seed-%s" % seed, "results",
                        "run-GAPL250K-%s-%s-%s" % (arm, inst, seed))


# ------------------------------------------------------------------ core math
def nd_of(rows):
    """rows: list of (fp, triple, strtriple). Return (unique triples, ND triples)."""
    triples = []
    seen = set()
    for _, t, _ in rows:
        key = tuple(t)
        if key not in seen:
            seen.add(key)
            triples.append(t)
    return triples, fc6.nondominated([list(t) for t in triples])


def fp_sets(rows, nd_triples):
    """Map every ND triple to the set of fingerprints seen for it in `rows`.
    Returns (triple -> fp set for ND triples, all fp set of ND triples)."""
    by_triple = defaultdict(set)
    for fp, t, _ in rows:
        by_triple[tuple(t)].add(fp)
    nd_map = {tuple(t): by_triple[tuple(t)] for t in nd_triples}
    all_fp = set()
    for s in nd_map.values():
        all_fp |= s
    return nd_map, all_fp


def normalize_with(points, mins, maxs):
    scale = [max(EPS, maxs[i] - mins[i]) for i in range(3)]
    return [[(p[i] - mins[i]) / scale[i] for i in range(3)] for p in points]


class InstanceRef(object):
    def __init__(self, instance):
        path = os.path.join(PFREF_DIR, "PFref_terminal_%s.csv" % instance)
        pts = [list(r[0]) for r in read_plain_front(path)]
        mins = [min(p[i] for p in pts) for i in range(3)]
        maxs = [max(p[i] for p in pts) for i in range(3)]
        self.mins, self.maxs = mins, maxs
        self.norm_ref = normalize_with(pts, mins, maxs)
        self.size = len(pts)

    def norm(self, points):
        return normalize_with(points, self.mins, self.maxs)


def front_metrics(nd_triples, ref):
    """HV/IGD/min-objectives for one strict-ND triple set under the instance ref."""
    if not nd_triples:
        return {"hv": float("nan"), "igd": float("nan"),
                "minCmax": float("nan"), "minTEC": float("nan"), "minTWC": float("nan")}
    nrm = ref.norm(nd_triples)
    return {
        "hv": fc6.hypervolume(nrm),
        "igd": fc6.igd(nrm, ref.norm_ref),
        "minCmax": min(p[0] for p in nd_triples),
        "minTEC": min(p[1] for p in nd_triples),
        "minTWC": min(p[2] for p in nd_triples),
    }


def fmt(v):
    if v is None:
        return ""
    if isinstance(v, float):
        if v != v:  # NaN
            return ""
        return repr(v)
    return str(v)


# ----------------------------------------------------------------- load a run
def load_run(arm, inst, seed, diag):
    d = run_dir(arm, inst, seed)
    reg = read_registry(os.path.join(d, "checkpoints", "checkpoint-registry.csv"))
    comb_dec, comb_obs, comb_fe = read_combined_terminal(os.path.join(d, "checkpoint-fronts.csv"))

    fronts = {}  # ckpt -> {"dec": rows, "obs": rows, "obsFE": int}
    for fe in CHECKPOINTS:
        fronts[fe] = {
            "dec": read_fp_front(os.path.join(d, "checkpoints",
                                              "checkpoint-%d-decision-front.csv" % fe)),
            "obs": read_fp_front(os.path.join(d, "checkpoints",
                                              "checkpoint-%d-observed-full-front.csv" % fe)),
            "obsFE": reg.get(fe, fe),
        }
    fronts[TERMINAL_FE] = {"obs": comb_obs, "obsFE": comb_fe.get(TERMINAL_FE, TERMINAL_FE)}

    # Terminal decision: combined-table rows (with fingerprints), verified vs front.csv
    front_csv = read_plain_front(os.path.join(d, "front.csv"))
    front_csv_triples = set(r[1] for r in front_csv)
    comb_dec_triples = set(r[2] for r in comb_dec)
    same_content = (len(front_csv) == len(comb_dec)
                    and front_csv_triples == comb_dec_triples)
    if same_content:
        fronts[TERMINAL_FE]["dec"] = comb_dec
        diag["terminal_dec_source"] = "checkpoint-fronts.csv:terminal-decision-front (verified == front.csv)"
    else:
        # fallback: front.csv + fingerprint resolved from terminal observed by exact string
        obs_map = {r[2]: r[0] for r in comb_obs}
        dec = []
        for t, s in front_csv:
            fp = obs_map.get(s, "NOFP|%s" % "|".join(s))
            dec.append((fp, t, s))
        fronts[TERMINAL_FE]["dec"] = dec
        diag["terminal_dec_source"] = (
            "front.csv + fingerprint resolution (WARNING: combined terminal-decision rows "
            "differ from front.csv: front.csv=%d rows, combined=%d rows)" % (len(front_csv), len(comb_dec)))
    return fronts


# ------------------------------------------------------- per-checkpoint analysis
def analyze_checkpoint(dec_rows, obs_rows, ref):
    """All front-level metrics for one (run, checkpoint)."""
    dec_unique, dec_nd = nd_of(dec_rows)
    obs_unique, obs_nd = nd_of(obs_rows)
    dec_map, dec_fps = fp_sets(dec_rows, dec_nd)
    obs_map, obs_fps = fp_sets(obs_rows, obs_nd)

    observed_only_fps = obs_fps - dec_fps
    decision_only_fps = dec_fps - obs_fps
    observed_nd_size = len(obs_fps)
    strict_nd_size = len(obs_nd)
    # observedOnly points: ND triples whose fingerprint set lies outside decision fps
    obs_only_triples = [t for t in obs_nd if not (obs_map[tuple(t)] & dec_fps)]

    # split of observedOnly fingerprints: does their objective triple already exist
    # on the decision front (objective-duplicate candidate) or is it objective-new?
    dec_triples = set(tuple(t) for t in dec_nd)
    fp_to_triple = {}
    for fp, t, _ in obs_rows:
        fp_to_triple[fp] = tuple(t)
    dup_obj = sum(1 for f in observed_only_fps if fp_to_triple[f] in dec_triples)
    new_obj = len(observed_only_fps) - dup_obj

    dec_m = front_metrics(dec_nd, ref)
    obs_m = front_metrics(obs_nd, ref)
    dec_nrm = ref.norm(dec_nd)
    obs_nrm = ref.norm(obs_nd)

    c_obs_dec = fc6.coverage(obs_nrm, dec_nrm)   # fraction of decision covered by observed
    c_dec_obs = fc6.coverage(dec_nrm, obs_nrm)   # fraction of observed covered by decision

    if obs_only_triples and dec_nrm:
        only_nrm = ref.norm(obs_only_triples)
        nn = sum(min(fc6.distance(p, q) for q in dec_nrm) for p in only_nrm) / len(only_nrm)
    elif not obs_only_triples:
        nn = 0.0
    else:
        nn = float("nan")

    return {
        "decisionFrontSize": len(dec_rows),
        "observedFullFrontSize": len(obs_rows),
        "exactDedupSize": len(obs_unique),
        "strictNdSize": strict_nd_size,
        "observedNdSize": observed_nd_size,
        "observedOnlyNdCount": len(observed_only_fps),
        "decisionOnlyNdCount": len(decision_only_fps),
        "observedOnlyRatio": (len(observed_only_fps) / observed_nd_size) if observed_nd_size else float("nan"),
        "obsOnlyDupOfDecisionObjectives": dup_obj,
        "obsOnlyObjectiveNew": new_obj,
        "C_observed_vs_decision": c_obs_dec,
        "C_decision_vs_observed": c_dec_obs,
        "normalizedNearestNeighborDistance": nn,
        "dec": dec_m, "obs": obs_m,
        "dec_nd": dec_nd, "obs_nd": obs_nd, "obs_only_triples": obs_only_triples,
        "dec_fps": dec_fps, "obs_fps": obs_fps,
    }


def potential_hv_recovery(res, ref):
    """HV(decision union observedOnly strictly-ND points) - HV(decision), normalized space.
    Candidates are first excluded when their fingerprint OR their exact objective
    triple matches the decision front."""
    dec_nd = res["dec_nd"]
    dec_triples = set(tuple(t) for t in dec_nd)
    cands = [t for t in res["obs_nd"]
             if tuple(t) not in dec_triples
             and not (res["obs_map_tuple"][tuple(t)] & res["dec_fps"])] \
        if "obs_map_tuple" in res else None
    if cands is None:
        return 0.0, 0
    if not cands:
        return 0.0, 0
    union = [list(t) for t in dec_triples] + [list(t) for t in cands]
    nd_union = fc6.nondominated(union)
    recovered = [t for t in nd_union if tuple(t) not in dec_triples]
    if not recovered:
        return 0.0, 0
    hv_before = fc6.hypervolume(ref.norm(dec_nd))
    hv_after = fc6.hypervolume(ref.norm(nd_union))
    return hv_after - hv_before, len(recovered)


# ------------------------------------------------------------------------ main
def main():
    diag = {"terminal_dec_source": None, "fp_multi_triple": 0, "dec_not_nd": 0,
            "strict_vs_observed_mismatch": 0, "dec_not_nd_details": []}
    refs = {}
    print("=== PFref_terminal verification (rebuild ND(union arms x seeds terminal decision)) ===")
    for inst, kind in INSTANCES:
        union = []
        for a in ARMS:
            for s in SEEDS:
                union += [list(r[0]) for r in
                          read_plain_front(os.path.join(run_dir(a, inst, s), "front.csv"))]
        rebuilt = fc6.nondominated(union)
        refs[inst] = InstanceRef(inst)
        path = os.path.join(PFREF_DIR, "PFref_terminal_%s.csv" % inst)
        filed = [list(r[0]) for r in read_plain_front(path)]
        filed_nd = fc6.nondominated(filed)
        match = (len(rebuilt) == len(filed_nd)
                 and all(fc6.equal(x, y) for x, y in zip(rebuilt, filed_nd)))
        print("%s (%s): PFref size=%d, rebuilt ND size=%d, match=%s, "
              "ideal=%s, nadir=%s" % (inst, kind, len(filed), len(rebuilt), match,
                                      refs[inst].mins, refs[inst].maxs))
        if not match:
            print("  WARNING: PFref_terminal file != rebuild; using FILE for normalization "
                  "(registered in README)")

    timeseries = []
    gaps = []
    results = {}  # (inst, seed, arm, ckpt) -> res

    for inst, kind in INSTANCES:
        ref = refs[inst]
        for seed in SEEDS:
            for arm in ARMS:
                fronts = load_run(arm, inst, seed, diag)
                for ckpt in ALL_CKPTS:
                    dec_rows, obs_rows = fronts[ckpt]["dec"], fronts[ckpt]["obs"]
                    res = analyze_checkpoint(dec_rows, obs_rows, ref)
                    # fingerprint -> triple consistency across this checkpoint's rows
                    fp_map = defaultdict(set)
                    for fp, t, _ in dec_rows + obs_rows:
                        fp_map[fp].add(tuple(t))
                    if any(len(v) > 1 for v in fp_map.values()):
                        diag["fp_multi_triple"] += 1
                    if res["strictNdSize"] != res["observedNdSize"]:
                        diag["strict_vs_observed_mismatch"] += 1
                    # decision front ND check (informational)
                    if res["decisionFrontSize"] != len(res["dec_nd"]):
                        diag["dec_not_nd"] += 1
                        diag["dec_not_nd_details"].append(
                            "%s %s seed%s ckpt%d: rows=%d uniqueTriples=%d strictND=%d" %
                            (inst, arm, seed, ckpt, res["decisionFrontSize"],
                             res["decisionFrontSize"], len(res["dec_nd"])))
                    res["obs_map_tuple"] = fp_sets(obs_rows, res["obs_nd"])[0]
                    recovery, n_recovered = potential_hv_recovery(res, ref)
                    res["potentialHvRecovery"] = recovery
                    res["recoveredCount"] = n_recovered
                    material = (recovery >= MATERIAL_HV_RECOVERY - EPS
                                or res["observedOnlyRatio"] >= MATERIAL_RATIO - EPS)
                    res["gapIsMaterial"] = material
                    results[(inst, seed, arm, ckpt)] = res

                    front_type_dec = ("terminal-decision-front" if ckpt == TERMINAL_FE
                                      else "checkpoint-decision-front")
                    front_type_obs = ("terminal-observed-full-front" if ckpt == TERMINAL_FE
                                      else "checkpoint-observed-full-front")
                    base = {
                        "instance": inst, "seed": seed, "arm": arm,
                        "checkpointTargetFE": ckpt,
                        "checkpointObservedFE": fronts[ckpt]["obsFE"],
                        "decisionFrontSize": res["decisionFrontSize"],
                        "observedFullFrontSize": res["observedFullFrontSize"],
                        "exactDedupSize": res["exactDedupSize"],
                        "strictNdSize": res["strictNdSize"],
                        "observedNdSize": res["observedNdSize"],
                        "observedOnlyNdCount": res["observedOnlyNdCount"],
                        "decisionOnlyNdCount": res["decisionOnlyNdCount"],
                        "observedOnlyRatio": res["observedOnlyRatio"],
                        "C_observed_vs_decision": res["C_observed_vs_decision"],
                        "C_decision_vs_observed": res["C_decision_vs_observed"],
                        "normalizedNearestNeighborDistance": res["normalizedNearestNeighborDistance"],
                        "HV_decision": res["dec"]["hv"], "HV_observed": res["obs"]["hv"],
                        "IGD_decision": res["dec"]["igd"], "IGD_observed": res["obs"]["igd"],
                    }
                    row_dec = dict(base); row_dec["frontType"] = front_type_dec
                    row_dec["minCmax"], row_dec["minTEC"], row_dec["minTWC"] = \
                        res["dec"]["minCmax"], res["dec"]["minTEC"], res["dec"]["minTWC"]
                    row_obs = dict(base); row_obs["frontType"] = front_type_obs
                    row_obs["minCmax"], row_obs["minTEC"], row_obs["minTWC"] = \
                        res["obs"]["minCmax"], res["obs"]["minTEC"], res["obs"]["minTWC"]
                    timeseries.extend([row_dec, row_obs])

                    gaps.append({
                        "instance": inst, "seed": seed, "arm": arm,
                        "checkpointTargetFE": ckpt,
                        "observedOnlyNdCount": res["observedOnlyNdCount"],
                        "observedOnlyRatio": res["observedOnlyRatio"],
                        "potentialHvRecovery": res["potentialHvRecovery"],
                        "gapIsMaterial": material,
                    })

    # ---------------------------------------------------------------- write CSVs
    ts_cols = ["instance", "seed", "arm", "checkpointTargetFE", "checkpointObservedFE",
               "frontType", "decisionFrontSize", "observedFullFrontSize", "exactDedupSize",
               "strictNdSize", "observedNdSize", "observedOnlyNdCount", "decisionOnlyNdCount",
               "observedOnlyRatio", "C_observed_vs_decision", "C_decision_vs_observed",
               "normalizedNearestNeighborDistance", "minCmax", "minTEC", "minTWC",
               "HV_decision", "HV_observed", "IGD_decision", "IGD_observed"]
    with io.open(os.path.join(HERE, "front-coverage-timeseries.csv"), "w",
                 encoding="utf-8", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(ts_cols)
        for row in timeseries:
            w.writerow([fmt(row[c]) for c in ts_cols])

    gap_cols = ["instance", "seed", "arm", "checkpointTargetFE", "observedOnlyNdCount",
                "observedOnlyRatio", "potentialHvRecovery", "gapIsMaterial"]
    with io.open(os.path.join(HERE, "observed-decision-gap.csv"), "w",
                 encoding="utf-8", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(gap_cols)
        for row in gaps:
            w.writerow([fmt(row[c]) for c in gap_cols])

    # c0-vs-c3-hv-curve.csv: instance x checkpoint wide, arms side by side, 3-seed means
    hv_cols = ["instance", "checkpointTargetFE"]
    for m in ["meanHV_decision", "meanHV_observed", "meanObservedOnlyRatio",
              "meanPotentialHvRecovery", "meanMinCmax_decision", "meanMinCmax_observed"]:
        for a in ARMS:
            hv_cols.append("%s_%s" % (m, a))
    curve_rows = []
    for inst, kind in INSTANCES:
        for ckpt in ALL_CKPTS:
            row = {"instance": inst, "checkpointTargetFE": ckpt}
            for m, getter in (
                    ("meanHV_decision", lambda r: r["dec"]["hv"]),
                    ("meanHV_observed", lambda r: r["obs"]["hv"]),
                    ("meanObservedOnlyRatio", lambda r: r["observedOnlyRatio"]),
                    ("meanPotentialHvRecovery", lambda r: r["potentialHvRecovery"]),
                    ("meanMinCmax_decision", lambda r: r["dec"]["minCmax"]),
                    ("meanMinCmax_observed", lambda r: r["obs"]["minCmax"])):
                for a in ARMS:
                    vals = [getter(results[(inst, s, a, ckpt)]) for s in SEEDS]
                    row["%s_%s" % (m, a)] = sum(vals) / len(vals)
            curve_rows.append(row)
    with io.open(os.path.join(HERE, "c0-vs-c3-hv-curve.csv"), "w",
                 encoding="utf-8", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(hv_cols)
        for row in curve_rows:
            w.writerow([fmt(row[c]) for c in hv_cols])

    # ------------------------------------------------------------- diagnostics
    print("\n=== Global data-quality diagnostics ===")
    print("terminal decision source: %s" % diag["terminal_dec_source"])
    print("checkpoints where some fingerprint maps to >1 distinct objective triple: %d"
          % diag["fp_multi_triple"])
    print("checkpoints where decision front as shipped is NOT strictly ND: %d"
          % diag["dec_not_nd"])
    for d in diag["dec_not_nd_details"]:
        print("   %s" % d)
    all_res = list(results.values())
    print("max potentialHvRecovery over all rows: %.6f ; rows with recovery>=0.02: %d ; "
          "rows with observedOnlyRatio>=0.10: %d (of %d)" %
          (max(r["potentialHvRecovery"] for r in all_res),
           sum(1 for r in all_res if r["potentialHvRecovery"] >= MATERIAL_HV_RECOVERY - EPS),
           sum(1 for r in all_res if r["observedOnlyRatio"] >= MATERIAL_RATIO - EPS),
           len(all_res)))
    print("checkpoints where strictNdSize != observedNdSize (fingerprint-vs-objective "
          "inconsistency): %d (of %d)" % (diag["strict_vs_observed_mismatch"],
                                          len(INSTANCES) * len(SEEDS) * len(ARMS) * len(ALL_CKPTS)))

    # ------------------------------------------------- per-instance C0 vs C3 table
    print("\n=== C0 vs C3 coverage-gap comparison (median over 3 seeds; "
          "ratio=observedOnlyRatio, rec=potentialHvRecovery) ===")
    for inst, kind in INSTANCES:
        print("-- %s (%s) --" % (inst, kind))
        print("%-10s %-4s %-10s %-10s %-10s %-10s %-8s %-8s" %
              ("ckpt", "arm", "ratio_med", "rec_med", "dupObj_med", "objNew_med", "", ""))
        for ckpt in ALL_CKPTS:
            for arm in ARMS:
                rat = [results[(inst, s, arm, ckpt)]["observedOnlyRatio"] for s in SEEDS]
                rec = [results[(inst, s, arm, ckpt)]["potentialHvRecovery"] for s in SEEDS]
                dup = [results[(inst, s, arm, ckpt)]["obsOnlyDupOfDecisionObjectives"] for s in SEEDS]
                new = [results[(inst, s, arm, ckpt)]["obsOnlyObjectiveNew"] for s in SEEDS]
                n_mat = sum(1 for s in SEEDS if results[(inst, s, arm, ckpt)]["gapIsMaterial"])
                print("%-10d %-4s %-10.6f %-10.6f %-10.1f %-10.1f materialSeeds=%d/3"
                      % (ckpt, arm, fc6.median(rat), fc6.median(rec),
                         fc6.median(dup), fc6.median(new), n_mat))
            r0 = [results[(inst, s, "C0", ckpt)]["observedOnlyRatio"] for s in SEEDS]
            r3 = [results[(inst, s, "C3", ckpt)]["observedOnlyRatio"] for s in SEEDS]
            p0 = [results[(inst, s, "C0", ckpt)]["potentialHvRecovery"] for s in SEEDS]
            p3 = [results[(inst, s, "C3", ckpt)]["potentialHvRecovery"] for s in SEEDS]
            print("    C0-C3 delta (median): ratio %+.6f, rec %+.6f"
                  % (fc6.median(r0) - fc6.median(r3), fc6.median(p0) - fc6.median(p3)))

        print("-- earliest material checkpoint (>=2/3 seeds material) per arm --")
        for arm in ARMS:
            first = None
            for ckpt in ALL_CKPTS:
                n_mat = sum(1 for s in SEEDS if results[(inst, s, arm, ckpt)]["gapIsMaterial"])
                if n_mat >= 2:
                    first = ckpt
                    break
            # median-based onset
            first_med = None
            for ckpt in ALL_CKPTS:
                rat = fc6.median([results[(inst, s, arm, ckpt)]["observedOnlyRatio"] for s in SEEDS])
                rec = fc6.median([results[(inst, s, arm, ckpt)]["potentialHvRecovery"] for s in SEEDS])
                if rat >= MATERIAL_RATIO - EPS or rec >= MATERIAL_HV_RECOVERY - EPS:
                    first_med = ckpt
                    break
            print("  %-4s first-material(2/3 seeds)=%s   first-material(median)=%s"
                  % (arm, first, first_med))

        print("-- HV_decision median trajectory (context) --")
        for arm in ARMS:
            traj = ["%d:%.4f" % (ckpt, fc6.median([results[(inst, s, arm, ckpt)]["dec"]["hv"]
                                                   for s in SEEDS]))
                    for ckpt in ALL_CKPTS]
            print("  %-4s %s" % (arm, "  ".join(traj)))

    print("\nOutputs written to %s" % HERE)
    print("rows: front-coverage-timeseries.csv=%d, observed-decision-gap.csv=%d, "
          "c0-vs-c3-hv-curve.csv=%d" % (len(timeseries), len(gaps), len(curve_rows)))


if __name__ == "__main__":
    main()
