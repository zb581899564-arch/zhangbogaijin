#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V35-SOURCE-ATTRIBUTION-500K / 09-v5-sa-hard-500k — lifecycle attribution by producing source.

Observation-layer fact (verified by direct inspection of source-lifecycle-events.csv):
only GENERATED / DESCENDANT / IMPROVING_DESCENDANT rows carry a real `source` column;
the utilization-layer event types (MERGE_POOL, PDDR_SELECTED, WORKING_POPULATION,
PERSONAL_ARCHIVE, QG_TEACHER, QP_TEACHER, QP_ACTION) carry source=NOT_APPLICABLE and
identify their subject by `subjectFingerprint`. Per-source utilization attribution therefore
requires joining subjectFingerprint -> source-ledger candidateFingerprint -> firstLevelSource.

This script:
  1. builds fingerprint -> firstLevelSource (unique mapping; AMBIGUOUS when a fingerprint is
     produced by more than one first-level source across the run — reported, never guessed);
  2. re-aggregates lifecycle events per 25k window per primary source (real source column for
     generation-layer events, fingerprint join for utilization-layer events);
  3. merges the corrected life_* columns into 05-hard-source-analysis/source-window-metrics.csv
     (the WHVG/WHVGShare/ExclusiveND columns were computed by compute_hard_source_windows.py and
     are NOT recomputed here) and rewrites source-lifecycle-summary.csv.

Zero FE, read-only on all frozen artifacts.
"""
import csv
import io
import os
import sys

ROOT = "E:/学习/李明哲-毕业材料/张博改进"
PKG = os.path.join(ROOT, "docs/evidence/V35-SOURCE-ATTRIBUTION-500K/09-v5-sa-hard-500k")
RES = os.path.join(PKG, "02-remote-run/results/SA-HARD-V5-500k")
OUT = os.path.join(PKG, "05-hard-source-analysis")
WINDOW_FE = 25000
RAW_TO_PRIMARY = {"GLOBAL_CFVF": "GLOBAL_CFVF", "CATA": "CATA",
                  "INHERITED_LS": "INHERITED_LS", "NOT_APPLICABLE": "PARENT_CARRYOVER"}
GEN_LAYER = {"GENERATED", "DESCENDANT", "IMPROVING_DESCENDANT"}
UTIL_LAYER = {"MERGE_POOL", "PDDR_SELECTED", "WORKING_POPULATION",
              "PERSONAL_ARCHIVE", "QG_TEACHER", "QP_TEACHER", "QP_ACTION"}
LIFE_TYPES = ["GENERATED", "DESCENDANT", "IMPROVING_DESCENDANT", "MERGE_POOL",
              "PDDR_SELECTED", "WORKING_POPULATION", "PERSONAL_ARCHIVE",
              "QG_TEACHER", "QP_TEACHER", "QP_ACTION"]
SOURCES = ["GLOBAL_CFVF", "CATA", "INHERITED_LS", "PARENT_CARRYOVER"]


def main():
    # 1. fingerprint -> source map
    fp_map = {}
    with io.open(os.path.join(RES, "source-ledger.csv"), encoding="utf-8", newline="") as fh:
        for row in csv.DictReader(fh):
            fp = row["candidateFingerprint"]
            src = row["firstLevelSource"]
            cur = fp_map.get(fp)
            if cur is None:
                fp_map[fp] = src
            elif cur != src:
                fp_map[fp] = "AMBIGUOUS"
    print("distinct fingerprints:", len(fp_map))
    from collections import Counter
    print("fingerprint source distribution:", Counter(fp_map.values()).most_common())

    # 2. lifecycle re-aggregation
    counts = {}          # (window, source, eventType) -> n
    stats = {"events": 0, "genLayer": 0, "utilLayer": 0,
             "utilResolved": 0, "utilAmbiguous": 0, "utilUnresolved": 0,
             "genLayerUnknownSource": 0}
    with io.open(os.path.join(RES, "source-lifecycle-events.csv"), encoding="utf-8", newline="") as fh:
        for row in csv.DictReader(fh):
            stats["events"] += 1
            et = row["eventType"]
            nom = int(row["nominalFE"])
            w = (nom + WINDOW_FE - 1) // WINDOW_FE
            if et in GEN_LAYER:
                stats["genLayer"] += 1
                src = row["source"]
                if src not in RAW_TO_PRIMARY:
                    stats["genLayerUnknownSource"] += 1
                    continue
                prim = RAW_TO_PRIMARY[src]
            else:
                stats["utilLayer"] += 1
                subj = row["subjectFingerprint"]
                src = fp_map.get(subj)
                if src is None:
                    stats["utilUnresolved"] += 1
                    continue
                if src == "AMBIGUOUS":
                    stats["utilAmbiguous"] += 1
                    continue
                stats["utilResolved"] += 1
                prim = RAW_TO_PRIMARY[src]
            counts[(w, prim, et)] = counts.get((w, prim, et), 0) + 1
    print("stats:", stats)

    # 3. merge into source-window-metrics.csv
    path = os.path.join(OUT, "source-window-metrics.csv")
    with io.open(path, encoding="utf-8", newline="") as fh:
        rows = list(csv.DictReader(fh))
        cols = list(rows[0].keys())
    for r in rows:
        w, s = int(r["window"]), r["source"]
        for et in LIFE_TYPES:
            r[f"life_{et}"] = str(counts.get((w, s, et), 0))
    with io.open(path, "w", encoding="utf-8", newline="") as fh:
        wcsv = csv.DictWriter(fh, fieldnames=cols, lineterminator="\n")
        wcsv.writeheader()
        wcsv.writerows(rows)

    # 4. lifecycle summary (run-level) with attribution coverage
    with io.open(os.path.join(OUT, "source-lifecycle-summary.csv"), "w", encoding="utf-8", newline="") as fh:
        wcsv = csv.writer(fh, lineterminator="\n")
        wcsv.writerow(["source"] + LIFE_TYPES +
                      ["mergeToPddrRate", "pddrToWorkingRate", "generatedToPddrRate",
                       "improvingDescendantRate", "teacherEventsPerGenerated"])
        for s in SOURCES:
            tot = {et: sum(counts.get((w, s, et), 0) for w in range(1, 21)) for et in LIFE_TYPES}
            g, mp, pd_, wp = tot["GENERATED"], tot["MERGE_POOL"], tot["PDDR_SELECTED"], tot["WORKING_POPULATION"]
            imp, desc = tot["IMPROVING_DESCENDANT"], tot["DESCENDANT"]
            teach = tot["QG_TEACHER"] + tot["QP_TEACHER"]
            wcsv.writerow([s] + [tot[et] for et in LIFE_TYPES] +
                          [(pd_ / mp if mp else 0.0), (wp / pd_ if pd_ else 0.0),
                           (pd_ / g if g else 0.0), (imp / desc if desc else 0.0),
                           (teach / g if g else 0.0)])
    with io.open(os.path.join(OUT, "lifecycle-attribution-coverage.properties"), "w", encoding="utf-8") as fh:
        for k, v in stats.items():
            fh.write("%s=%s\n" % (k, v))
        fh.write("attributionMethod=GEN-layer uses the real source column; UTIL-layer joins "
                 "subjectFingerprint -> source-ledger candidateFingerprint -> firstLevelSource\n")
        fh.write("ambiguousFingerprints=excluded and counted (never guessed)\n")
        fh.write("consumedFE=0\nchangedAlgorithm=false\n")
    print("merged:", path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
