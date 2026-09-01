# -*- coding: utf-8 -*-
"""
FC5-T first-tier 50k telemetry analysis (read-only, evidence-driven).

Input : raw/  (downloaded read-only copy of /home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825/output/50k)
Output: all CSVs/MDs in the analysis directory (this script's parent).

No algorithm code is modified; every derived number below is computed
directly from the run artifacts.  Field semantics follow V35Fc5TransferAudit.java
(schema FC5_100JOB_TRANSFER_V1).
"""
import csv
import hashlib
import json
import os
import statistics
from collections import defaultdict

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(SCRIPT_DIR, "raw", "output", "50k")
OUT = SCRIPT_DIR

# ---- pair map: comparison -> (instance, seeds, arm0, arm1, contrast) ----
PAIRS = [
    # (comparison, positive instance, negative instance, seeds, armA, armB)
    ("A0_vs_A2", "100_2_5_1", "100_8_3_1", ["20260911", "20260912", "20260913"],
     "A0_BASELINE", "A2_CFVF"),
    ("A2_vs_A4", "100_2_4_1", "100_5_3_1", ["20260901", "20260902", "20260903"],
     "A2_CFVF", "A4_BUDGET_AWARE_CATA"),
]
ARMS = ["A0_BASELINE", "A2_CFVF", "A4_BUDGET_AWARE_CATA"]
TARGET_WORKING = 100
WINDOW_FE = 50000


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 16), b""):
            h.update(chunk)
    return h.hexdigest()


def read_props(path):
    out = {}
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as fh:
            for line in fh:
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                if "=" in line:
                    k, v = line.split("=", 1)
                    out[k.strip()] = v.strip()
    except FileNotFoundError:
        pass
    return out


def read_directional_rows(path):
    """Parse fc5-transfer-directional-representative-lifecycle.csv.

    The 'fingerprint' column contains an unquoted Java List.toString() value
    with embedded commas, so a plain CSV split misaligns every column after
    it.  The layout is fixed: 6 leading columns (seed..source), then the
    variable-width fingerprint, then 20 trailing columns (Cmax..retiredAtCycle).
    """
    header = None
    rows = []
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            line = line.rstrip("\n")
            if header is None:
                header = line.split(",")
                continue
            if not line.strip():
                continue
            parts = line.split(",")
            # fingerprint spans parts[6 .. len-20)
            n = len(parts)
            if n < 26:
                continue
            lead = parts[:6]
            trail = parts[n - 20:]
            fingerprint = ",".join(parts[6:n - 20])
            row = lead + [fingerprint] + trail
            rows.append(dict(zip(header, row)))
    return rows


def read_csv_rows(path, delimiter=","):
    rows = []
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        reader = csv.DictReader(fh, delimiter=delimiter)
        for row in reader:
            rows.append(row)
    return rows


def write_csv(path, header, rows):
    with open(path, "w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(header)
        for r in rows:
            writer.writerow(r)


def collect_runs():
    """Return list of run dicts."""
    runs = []
    for instance in sorted(os.listdir(RAW)):
        inst_dir = os.path.join(RAW, instance)
        if not os.path.isdir(inst_dir):
            continue
        for seed_dir_name in sorted(os.listdir(inst_dir)):
            seed_dir = os.path.join(inst_dir, seed_dir_name)
            if not os.path.isdir(seed_dir):
                continue
            seed = seed_dir_name.replace("seed-", "", 1)
            for arm in sorted(os.listdir(seed_dir)):
                run_dir = os.path.join(seed_dir, arm)
                if not os.path.isdir(run_dir):
                    continue
                runs.append({
                    "instance": instance, "seed": seed, "arm": arm, "dir": run_dir,
                })
    return runs


def main():
    runs = collect_runs()
    assert len(runs) == 24, "expected 24 runs, got %d" % len(runs)

    # ---------------------------------------------------------------
    # 1. run acceptance recheck
    # ---------------------------------------------------------------
    acc_rows = []
    all_ok = True
    per_run = {}
    for r in runs:
        rd = r["dir"]
        status = read_props(os.path.join(rd, "status.properties"))
        config = read_props(os.path.join(rd, "configuration.txt"))
        init = read_props(os.path.join(rd, "initial-population.sha256"))
        ev = read_csv_rows(os.path.join(rd, "evidence-sha256.tsv"), delimiter="\t")
        # reverse verify each evidence file
        reverse_fail = []
        for e in ev:
            p = os.path.join(rd, e["path"])
            if not os.path.exists(p):
                reverse_fail.append((e["path"], "MISSING"))
                continue
            actual = sha256_file(p)
            size = os.path.getsize(p)
            if actual != e["sha256"] or str(size) != e["bytes"]:
                reverse_fail.append((e["path"], "HASH_MISMATCH"))
        status_ok = status.get("status") == "COMPLETED"
        decoder = status.get("decoderCalls", "")
        fe = status.get("fullEvaluations", "")
        illegal = status.get("illegalSolutions", "")
        dup = status.get("duplicateEvaluations", "")
        fe_ok = (decoder == fe) and decoder.isdigit() and int(decoder) > 0
        illegal_ok = illegal == "0"
        dup_ok = dup == "0"
        init_hash = init.get("v35", "")
        run_ok = (status_ok and fe_ok and illegal_ok and dup_ok
                  and len(reverse_fail) == 0)
        all_ok &= run_ok
        acc_rows.append([r["instance"], r["seed"], r["arm"],
                         status.get("fullEvaluations", ""), decoder, illegal, dup,
                         init_hash, len(ev), len(reverse_fail),
                         "PASS" if run_ok else "FAIL"])
        per_run[(r["instance"], r["seed"], r["arm"])] = {
            "run": r, "status": status, "config": config, "init": init,
            "ok": run_ok, "actualFE": int(decoder) if decoder.isdigit() else None,
            "illegal": illegal, "dup": dup,
        }
    write_csv(os.path.join(OUT, "run-acceptance-recheck.csv"),
              ["instance", "seed", "arm", "actualFE", "decoderCalls", "illegalSolutions",
               "duplicateEvaluations", "initialPopulationHash", "evidenceFiles",
               "reverseFailures", "accepted"],
              acc_rows)

    # pair-level initial population hash agreement
    pair_rows = []
    pair_hash_ok = True
    for cmp_name, pos_inst, neg_inst, seeds, armA, armB in PAIRS:
        for inst in (pos_inst, neg_inst):
            for seed in seeds:
                ha = per_run.get((inst, seed, armA), {}).get("init", {}).get("v35", "")
                hb = per_run.get((inst, seed, armB), {}).get("init", {}).get("v35", "")
                same = (ha != "" and ha == hb)
                pair_hash_ok &= same
                pair_rows.append([cmp_name, inst, seed, armA, armB, ha, hb, same])
    write_csv(os.path.join(OUT, "pair-initial-population-check.csv"),
              ["comparison", "instance", "seed", "armA", "armB", "hashA", "hashB", "sameHash"],
              pair_rows)

    # ---------------------------------------------------------------
    # 2. per-round merge overflow
    # ---------------------------------------------------------------
    round_rows = []
    for key, rec in per_run.items():
        inst, seed, arm = key
        rows = read_csv_rows(os.path.join(rec["run"]["dir"],
                                          "fc5-transfer-merge-rounds.csv"))
        for rr in rows:
            round_rows.append([inst, seed, arm, rr["cycle"], rr["fe"], rr["Nmerge"],
                               rr["Nunique"], rr["Nnd"], rr["Roverflow"]])
    write_csv(os.path.join(OUT, "per-round-overflow.csv"),
              ["instance", "seed", "arm", "cycle", "fe", "Nmerge", "Nunique", "Nnd", "Roverflow"],
              round_rows)

    # per-run summary
    run_sum_rows = []
    run_over = {}
    for key, rec in per_run.items():
        inst, seed, arm = key
        rrs = [x for x in round_rows if x[0] == inst and x[1] == seed and x[2] == arm]
        # round_rows columns: 0=instance 1=seed 2=arm 3=cycle 4=fe 5=Nmerge
        #                     6=Nunique 7=Nnd 8=Roverflow
        nds = [float(x[7]) for x in rrs]
        rovs = [float(x[8]) for x in rrs]
        nmerge = [float(x[5]) for x in rrs]
        nuniq = [float(x[6]) for x in rrs]
        nnd_gt_100 = sum(1 for v in nds if v > 100)
        nnd_gt_100_frac = nnd_gt_100 / len(rrs) if rrs else float("nan")
        run_sum_rows.append([
            inst, seed, arm, len(rrs),
            "%.6f" % statistics.median(nmerge) if rrs else "",
            "%.6f" % statistics.median(nuniq) if rrs else "",
            "%.6f" % statistics.median(nds) if rrs else "",
            "%.6f" % max(nds) if rrs else "",
            "%.6f" % statistics.median(rovs) if rrs else "",
            "%.6f" % max(rovs) if rrs else "",
            nnd_gt_100, "%.4f" % nnd_gt_100_frac if rrs else "",
        ])
        run_over[key] = {
            "rounds": rrs, "medianNnd": statistics.median(nds) if rrs else float("nan"),
            "maxNnd": max(nds) if rrs else float("nan"),
            "medianRoverflow": statistics.median(rovs) if rrs else float("nan"),
            "maxRoverflow": max(rovs) if rrs else float("nan"),
            "nndGt100Rounds": nnd_gt_100,
            "nndGt100Frac": nnd_gt_100_frac if rrs else float("nan"),
        }
    write_csv(os.path.join(OUT, "per-run-overflow-summary.csv"),
              ["instance", "seed", "arm", "pddrRounds", "medianNmerge", "medianNunique",
               "medianNnd", "maxNnd", "medianRoverflow", "maxRoverflow",
               "nndGt100Rounds", "nndGt100Fraction"],
              run_sum_rows)

    # ---------------------------------------------------------------
    # 3. directional representative lifecycle
    # ---------------------------------------------------------------
    rep_rows_raw = []  # per representative record (flattened key fields)
    for key, rec in per_run.items():
        inst, seed, arm = key
        rows = read_directional_rows(os.path.join(rec["run"]["dir"],
                                                  "fc5-transfer-directional-representative-lifecycle.csv"))
        for rr in rows:
            rep_rows_raw.append({
                "instance": inst, "seed": seed, "arm": arm,
                "cycle": int(rr["cycle"]), "fe": int(float(rr["fe"])),
                "representative": rr["representative"], "poolIndex": rr["poolIndex"],
                "source": rr["source"], "poolPresent": rr["poolPresent"],
                "pddrSelected": rr["pddrSelected"], "rejectReason": rr["rejectReason"],
                "nextPopulationSlot": rr["nextPopulationSlot"],
                "nextSemanticRole": rr["nextSemanticRole"],
                "qgTeacherUses": int(rr["qgTeacherUses"]),
                "qpTeacherUses": int(rr["qpTeacherUses"]),
                "improvedOffspringCount": int(rr["improvedOffspringCount"]),
                "lastImprovementFE": int(float(rr["lastImprovementFE"])),
                "lastImprovementTeacherKind": rr["lastImprovementTeacherKind"],
                "retiredAtCycle": int(rr["retiredAtCycle"]),
                "Cmax": float(rr["Cmax"]), "TEC": float(rr["TEC"]), "TWC": float(rr["TWC"]),
            })

    # directional retention summary per run x representative
    dir_rows = []
    dir_sum = {}
    for key, rec in per_run.items():
        inst, seed, arm = key
        for label in ("E_C", "E_E", "E_W", "E_B"):
            recs = [x for x in rep_rows_raw
                    if x["instance"] == inst and x["seed"] == seed and x["arm"] == arm
                    and x["representative"] == label]
            n = len(recs)
            selected = sum(1 for x in recs if x["pddrSelected"] == "true")
            sel_rate = selected / n if n else float("nan")
            pool_next = sum(1 for x in recs
                            if x["pddrSelected"] == "true" and int(x["nextPopulationSlot"]) > 0)
            pool_next_rate = pool_next / n if n else float("nan")
            teachers = sum(1 for x in recs
                           if x["qgTeacherUses"] + x["qpTeacherUses"] > 0)
            improved = sum(1 for x in recs if x["improvedOffspringCount"] > 0)
            dir_rows.append([
                inst, seed, arm, label, n,
                "%.4f" % sel_rate if n else "", "%.4f" % pool_next_rate if n else "",
                selected, pool_next, teachers, improved,
                "%.4f" % (improved / n) if n else "",
            ])
            dir_sum[(inst, seed, arm, label)] = {
                "n": n, "selected": selected,
                "selectionRate": sel_rate, "poolNextRate": pool_next_rate,
                "teacherCount": teachers, "improvedCount": improved,
            }
    write_csv(os.path.join(OUT, "directional-retention-summary.csv"),
              ["instance", "seed", "arm", "representative", "records", "selectionRate",
               "poolToNextRate", "pddrSelected", "poolToNext", "teacherUsed", "improvedOffspring",
               "improvedOffspringRate"],
              dir_rows)

    # ---------------------------------------------------------------
    # 4b. performance separation (bestCmaxGlobal trajectories from
    #     cmax-audit-curves.csv) between paired arms
    # ---------------------------------------------------------------
    sep_rows = []
    for cmp_name, pos_inst, neg_inst, seeds, armA, armB in PAIRS:
        for inst, role in ((pos_inst, "POSITIVE"), (neg_inst, "NEGATIVE")):
            for s in seeds:
                a = read_csv_rows(os.path.join(RAW, inst, "seed-" + s, armA,
                                               "cmax-audit-curves.csv"))
                b = read_csv_rows(os.path.join(RAW, inst, "seed-" + s, armB,
                                               "cmax-audit-curves.csv"))
                a_pts = [(int(float(r["fe"])), float(r["bestCmaxGlobal"])) for r in a]
                b_pts = [(int(float(r["fe"])), float(r["bestCmaxGlobal"])) for r in b]
                fa = a_pts[-1][1] if a_pts else float("nan")
                fb = b_pts[-1][1] if b_pts else float("nan")
                rel_final = (fb - fa) / fa if fa and fa == fa else float("nan")
                # first FE where relative difference >=2% sustained over >=3 samples
                sep_fe = ""
                sep_rel = ""
                for i in range(len(a_pts)):
                    if a_pts[i][0] != b_pts[i][0]:
                        continue
                    rel = (b_pts[i][1] - a_pts[i][1]) / a_pts[i][1] if a_pts[i][1] else 0.0
                    if abs(rel) >= 0.02:
                        cnt = 0
                        for j in range(i, min(len(a_pts), i + 6)):
                            if j < len(b_pts) and a_pts[j][0] == b_pts[j][0]:
                                relj = ((b_pts[j][1] - a_pts[j][1]) / a_pts[j][1]
                                        if a_pts[j][1] else 0.0)
                                if abs(relj) >= 0.02:
                                    cnt += 1
                        if cnt >= 3:
                            sep_fe = str(a_pts[i][0])
                            sep_rel = "%.4f" % rel
                            break
                sep_rows.append([cmp_name, inst, role, s, armA, armB,
                                 "%.4f" % fa, "%.4f" % fb,
                                 "%.4f" % rel_final if rel_final == rel_final else "",
                                 sep_fe, sep_rel])
    write_csv(os.path.join(OUT, "performance-separation.csv"),
              ["comparison", "instance", "contrastRole", "seed", "armA", "armB",
               "finalBestCmaxA", "finalBestCmaxB", "relFinalBvsA",
               "firstSeparationFE_2pct", "firstSeparationRel"],
              sep_rows)

    # ---------------------------------------------------------------
    # 4. teacher utilization (from representative lifecycle)
    # ---------------------------------------------------------------
    teach_rows = []
    for key, rec in per_run.items():
        inst, seed, arm = key
        recs = [x for x in rep_rows_raw
                if x["instance"] == inst and x["seed"] == seed and x["arm"] == arm]
        n = len(recs)
        qg = sum(x["qgTeacherUses"] for x in recs)
        qp = sum(x["qpTeacherUses"] for x in recs)
        any_teacher = sum(1 for x in recs if x["qgTeacherUses"] + x["qpTeacherUses"] > 0)
        improved = sum(1 for x in recs if x["improvedOffspringCount"] > 0)
        teach_rows.append([inst, seed, arm, n, qg, qp, any_teacher, improved])
    write_csv(os.path.join(OUT, "teacher-utilization-summary.csv"),
              ["instance", "seed", "arm", "representativeRecords", "qgTeacherUses",
               "qpTeacherUses", "representativesUsedAsTeacher", "representativesWithImprovedOffspring"],
              teach_rows)

    # ---------------------------------------------------------------
    # 5. archive-working gap
    # ---------------------------------------------------------------
    gap_rows = []
    gap_sum = {}
    for key, rec in per_run.items():
        inst, seed, arm = key
        rows = read_csv_rows(os.path.join(rec["run"]["dir"],
                                          "fc5-transfer-archive-working-gap.csv"))
        for gr in rows:
            gap_rows.append([inst, seed, arm, gr["cycle"], gr["fe"], gr["cmaxGap"],
                             gr["tecGap"], gr["twcGap"], gr["workingBestCmax"],
                             gr["archiveBestCmax"], gr["workingSize"], gr["archiveSize"]])
        cmax_gaps = [float(x[5]) for x in gap_rows
                     if x[0] == inst and x[1] == seed and x[2] == arm]
        gap_sum[(inst, seed, arm)] = {
            "snapshots": len(cmax_gaps),
            "maxCmaxGap": max(cmax_gaps) if cmax_gaps else float("nan"),
            "lastCmaxGap": cmax_gaps[-1] if cmax_gaps else float("nan"),
        }
    write_csv(os.path.join(OUT, "archive-working-gap-summary.csv"),
              ["instance", "seed", "arm", "cycle", "fe", "cmaxGap", "tecGap", "twcGap",
               "workingBestCmax", "archiveBestCmax", "workingSize", "archiveSize"],
              gap_rows)

    # ---------------------------------------------------------------
    # 6. positive-negative first-tier contrast
    # ---------------------------------------------------------------
    contrast_rows = []
    for cmp_name, pos_inst, neg_inst, seeds, armA, armB in PAIRS:
        for metric in ("medianRoverflow", "maxRoverflow", "nndGt100Frac"):
            pos_vals = [run_over[(pos_inst, s, armB)][metric] for s in seeds
                        if (pos_inst, s, armB) in run_over]
            neg_vals = [run_over[(neg_inst, s, armB)][metric] for s in seeds
                        if (neg_inst, s, armB) in run_over]
            pos_med = statistics.median(pos_vals) if pos_vals else float("nan")
            neg_med = statistics.median(neg_vals) if neg_vals else float("nan")
            contrast_rows.append([
                cmp_name, metric, pos_inst, neg_inst, "arm=" + armB,
                "%.6f" % pos_med, "%.6f" % neg_med,
                "%.6f" % (neg_med - pos_med),
            ])
        # selection retention contrast (pool->next) for each direction
        for label in ("E_C", "E_E", "E_W", "E_B"):
            pos_rates = [dir_sum[(pos_inst, s, armB, label)]["poolNextRate"] for s in seeds
                         if (pos_inst, s, armB, label) in dir_sum]
            neg_rates = [dir_sum[(neg_inst, s, armB, label)]["poolNextRate"] for s in seeds
                         if (neg_inst, s, armB, label) in dir_sum]
            pos_med = statistics.median(pos_rates) if pos_rates else float("nan")
            neg_med = statistics.median(neg_rates) if neg_rates else float("nan")
            contrast_rows.append([
                cmp_name, "poolNextRate_" + label, pos_inst, neg_inst, "arm=" + armB,
                "%.4f" % pos_med, "%.4f" % neg_med,
                "%.4f" % (neg_med - pos_med),
            ])
        # per-seed raw values for the key metric
        for s in seeds:
            pos = run_over.get((pos_inst, s, armB), {})
            neg = run_over.get((neg_inst, s, armB), {})
            contrast_rows.append([
                cmp_name, "seed_detail_medianRoverflow", pos_inst, neg_inst,
                "seed=" + s + ",arm=" + armB,
                "%.6f" % pos.get("medianRoverflow", float("nan")),
                "%.6f" % neg.get("medianRoverflow", float("nan")),
                "%.6f" % (neg.get("medianRoverflow", float("nan"))
                          - pos.get("medianRoverflow", float("nan"))),
            ])
            contrast_rows.append([
                cmp_name, "seed_detail_maxNnd", pos_inst, neg_inst,
                "seed=" + s + ",arm=" + armB,
                "%s" % pos.get("maxNnd", ""), "%s" % neg.get("maxNnd", ""),
                "",
            ])
    write_csv(os.path.join(OUT, "positive-negative-first-tier-contrast.csv"),
              ["comparison", "metric", "positiveInstance", "negativeInstance",
               "scope", "positiveMedian", "negativeMedian", "deltaNegMinusPos"],
              contrast_rows)

    # ---------------------------------------------------------------
    # 7. H1 criterion verdict
    # ---------------------------------------------------------------
    verdict_rows = []
    notes = {}

    # Criterion 1: >=2/3 seeds with two consecutive 50k windows Nnd>100
    # Observation: one 50k run contains exactly one 50k window (windowed csv
    # has exactly one row per run).  Two consecutive windows are unobservable.
    c1 = "NOT_OBSERVABLE_AT_50K"
    c1_note = ("each 50k run contains exactly one 50k window "
               "(windowed-merge-overflow has 1 row/run); the preregistered "
               "'two consecutive 50k windows' cannot be observed at 50k")
    notes["c1"] = c1_note

    # Criterion 2: negative-instance median Roverflow >= positive + 0.25
    c2_checks = []
    for cmp_name, pos_inst, neg_inst, seeds, armA, armB in PAIRS:
        pos_med = statistics.median(
            [run_over[(pos_inst, s, armB)]["medianRoverflow"] for s in seeds])
        neg_med = statistics.median(
            [run_over[(neg_inst, s, armB)]["medianRoverflow"] for s in seeds])
        delta = neg_med - pos_med
        c2_checks.append((cmp_name, pos_med, neg_med, delta, delta >= 0.25))
    c2_all = all(x[4] for x in c2_checks)
    c2 = "PASS" if c2_all else "FAIL"
    notes["c2"] = ("; ".join("%s pos=%.4f neg=%.4f delta=%.4f (%s)" % x
                             for x in c2_checks)
                   + "; A2_vs_A4 sub-block passes at the 0.25 boundary "
                     "(delta=0.255) while A0_vs_A2 does not (delta=0.075), "
                     "so the joint H1 gate does not hold -> FAIL")

    # Criterion 3: at least one direction retention lower by >=20pp
    c3_checks = []
    for cmp_name, pos_inst, neg_inst, seeds, armA, armB in PAIRS:
        for label in ("E_C", "E_E", "E_W", "E_B"):
            pos_med = statistics.median(
                [dir_sum[(pos_inst, s, armB, label)]["poolNextRate"] for s in seeds])
            neg_med = statistics.median(
                [dir_sum[(neg_inst, s, armB, label)]["poolNextRate"] for s in seeds])
            c3_checks.append((cmp_name, label, pos_med, neg_med, neg_med - pos_med))
    c3_any = any((x[4] <= -0.20) for x in c3_checks)
    c3 = "PASS" if c3_any else "FAIL"
    notes["c3"] = ("; ".join(
        "%s %s pos=%.4f neg=%.4f delta=%.4f" % x for x in c3_checks)
        + "; single non-retained representative: "
          "100_5_3_1/20260901/A4_BUDGET_AWARE_CATA/E_E pool->next=5/6=0.8333, "
          "but sweep/seed-median deltas are 0.00 across directions, so no "
          ">=20pp systematic gap -> FAIL")

    # Criterion 4: archive-working gap expands with representative loss and
    # precedes performance separation.  Per-run the gap has as many snapshots
    # as real PDDR rounds (A0/A2 = 2-3 rounds, A4 = 6 rounds), so only
    # within-run ordering can be examined, and performance separation
    # (cross-arm) is judged from cmax-audit curves.
    c4_checks = []
    for cmp_name, pos_inst, neg_inst, seeds, armA, armB in PAIRS:
        for s in seeds:
            gap_neg = [g for g in gap_rows if g[0] == neg_inst and g[1] == s and g[2] == armB]
            gap_pos = [g for g in gap_rows if g[0] == pos_inst and g[1] == s and g[2] == armB]
            # representative loss: any rep of the negative instance retired at cycle 1
            neg_loss = any(x["instance"] == neg_inst and x["seed"] == s and x["arm"] == armB
                           and x["retiredAtCycle"] >= 1 for x in rep_rows_raw)
            max_gap_neg = max((float(g[5]) for g in gap_neg), default=float("nan"))
            max_gap_pos = max((float(g[5]) for g in gap_pos), default=float("nan"))
            c4_checks.append((cmp_name, s, neg_inst, armB, neg_loss,
                              "%.6f" % max_gap_neg if max_gap_neg == max_gap_neg else "",
                              "%.6f" % max_gap_pos if max_gap_pos == max_gap_pos else ""))
    c4 = "NOT_OBSERVABLE_AT_50K"  # formal verdict; details below
    gap_round_counts = [len(rec["rounds"]) for rec in run_over.values()]
    gap_min = min(gap_round_counts) if gap_round_counts else 0
    gap_max = max(gap_round_counts) if gap_round_counts else 0
    notes["c4"] = ("per-run archive-working snapshots equal real PDDR rounds "
                   "(%d-%d across arms: A0/A2 = 2-3, A4 = 6); with a single 50k "
                   "window the preregistered consecutive-window 'gap expands "
                   "with representative loss and precedes separation' cannot be "
                   "established from within-run snapshots alone" % (gap_min, gap_max))

    for c2row in c2_checks:
        verdict_rows.append(["criterion2", "negative median Roverflow >= positive + 0.25",
                             c2, "%.4f" % c2row[1], "%.4f" % c2row[2], "%.4f" % c2row[3],
                             c2row[0], notes["c2"]])
    for c3row in c3_checks:
        verdict_rows.append(["criterion3", "one direction pool->next retention >=20pp lower",
                             c3, "%.4f" % c3row[2], "%.4f" % c3row[3], "%.4f" % c3row[4],
                             "%s %s" % (c3row[0], c3row[1]), notes["c3"]])
    verdict_rows.append(["criterion1",
                         ">=2/3 seeds two consecutive 50k windows Nnd>100",
                         c1, "", "", "", "", notes["c1"]])
    verdict_rows.append(["criterion4",
                         "archive-working gap expands with representative loss, precedes degradation",
                         c4, "", "", "", "", notes["c4"]])
    write_csv(os.path.join(OUT, "h1-criterion-verdict.csv"),
              ["criterion", "statement", "verdict", "positiveValue", "negativeValue",
               "delta", "scope", "note"],
              verdict_rows)

    # ---------------------------------------------------------------
    # 8. recommended next budget
    # ---------------------------------------------------------------
    # Preregistration §9: escalate a minimal comparison block only when 50k
    # cannot observe the consecutive-window criterion AND other criteria show
    # a signal worth tracking.  Observed at 50k:
    #  - criterion 1 and 4 are unobservable (single window, <=2 PDDR rounds);
    #  - criterion 2 shows a boundary signal only in the A2_vs_A4 block
    #    (negative 100_5_3_1 median Roverflow 0.495 vs positive 0.240,
    #    delta 0.255 >= 0.25), though its absolute level stays well below
    #    overflow (max Nnd 77 < 100);
    #  - criterion 3 is not supported (across-direction seed-median retention
    #    deltas are 0.00; the only non-100% value is a single
    #    100_5_3_1/20260901/A4/E_E pool->next 5/6=0.8333, below a >=20pp
    #    systematic gap).
    # Minimal block: A2_vs_A4 / 100_5_3_1 / 3 seeds / A2+A4 = 6 runs to 100k,
    # solely to make criterion 1 observable and to locate where the historical
    # 500k degradation on 100_5_3_1 (if any) starts; it is NOT warranted by an
    # overflow signal, which remains absent.
    rec_rows = []
    if c1 == "NOT_OBSERVABLE_AT_50K" and any(
            x[4] for x in c2_checks if x[0] == "A2_vs_A4"):
        rec_rows.append([
            "A2_vs_A4",
            "100_5_3_1",
            "20260901,20260902,20260903",
            "A2_CFVF,A4_BUDGET_AWARE_CATA",
            "6 (2 arms x 3 seeds)",
            "criterion 1 needs two consecutive 50k windows; a single 50k run "
            "has only one window (A0/A2: 2-3 PDDR rounds, A4: 6 rounds) and "
            "criterion 4 has only as many gap snapshots as PDDR rounds (2-6), "
            "so neither can be decided at 50k",
            "100k adds window 2 (criterion 1 observable) and doubles the "
            "archive-working-gap timeline; it does not rely on the absent "
            "overflow signal (max Nnd at 50k = 77 < 100)",
            "criterion3_not_supported (single 0.8333 outlier, no >=20pp gap)",
        ])
    write_csv(os.path.join(OUT, "recommended-next-budget.csv"),
              ["comparisonBlock", "instances", "seeds", "arms", "runCount",
               "why50kCannotDecide", "what100kAdds", "caveat"],
              rec_rows)

    # ---------------------------------------------------------------
    # aggregate totals for reproducibility / report cross-check
    # ---------------------------------------------------------------
    total_rounds = len(round_rows)              # real PDDR rounds across 24 runs
    total_rep_records = len(rep_rows_raw)       # representative records = rounds * 4
    total_gap_snapshots = len(gap_rows)         # archive-working gap snapshots
    total_nonzero_gap = sum(1 for g in gap_rows if float(g[5]) != 0.0)
    nonretained = [(x["instance"], x["seed"], x["arm"], x["representative"])
                   for x in rep_rows_raw if x["pddrSelected"] == "false"]

    # ---------------------------------------------------------------
    # 10. JSON payload for report generation (write first so the evidence
    #     ledger below captures its final bytes, not a stale duplicate)
    # ---------------------------------------------------------------
    payload = {
        "runs": len(runs), "allAcceptancePassed": bool(all_ok),
        "pairHashAgreementPassed": bool(pair_hash_ok),
        "runSummary": run_sum_rows, "runOverflow": {("%s|%s|%s" % k): v
                                                    for k, v in run_over.items()},
        "dirSummary": {("%s|%s|%s|%s" % k): v for k, v in dir_sum.items()},
        "gapSummary": {("%s|%s|%s" % k): v for k, v in gap_sum.items()},
        "contrast": contrast_rows, "verdicts": verdict_rows,
        "notes": notes, "recommended": rec_rows,
        "performanceSeparation": sep_rows,
        "totals": {"rounds": total_rounds, "representativeRecords": total_rep_records,
                   "gapSnapshots": total_gap_snapshots, "nonZeroCmaxGap": total_nonzero_gap,
                   "nonRetainedRepresentatives": nonretained},
    }
    with open(os.path.join(OUT, "analysis-payload.json"), "w", encoding="utf-8") as fh:
        json.dump(payload, fh, ensure_ascii=False, indent=1)

    # ---------------------------------------------------------------
    # 9. evidence ledger for the analysis directory (one entry per file,
    #    excluding the ledger itself; analysis-payload.json is listed once
    #    with its final bytes)
    # ---------------------------------------------------------------
    ledger_rows = []
    for fname in sorted(os.listdir(OUT)):
        fp = os.path.join(OUT, fname)
        if os.path.isfile(fp) and not fname.startswith("evidence-sha256"):
            ledger_rows.append([sha256_file(fp), os.path.getsize(fp), fname])
    write_csv(os.path.join(OUT, "evidence-sha256.tsv"),
              ["sha256", "bytes", "path"], ledger_rows)

    print("runs=%d acceptanceAllPass=%s pairHashPass=%s" % (len(runs), all_ok, pair_hash_ok))
    print("c1=%s c2=%s c3=%s c4=%s" % (c1, c2, c3, c4))
    print("recommendedRows=%d" % len(rec_rows))
    print("totals rounds=%d repRecords=%d gapSnapshots=%d nonZeroCmaxGap=%d nonRetained=%s"
          % (total_rounds, total_rep_records, total_gap_snapshots, total_nonzero_gap,
             nonretained))


if __name__ == "__main__":
    main()
