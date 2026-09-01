#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Stage-by-stage debug: frozen TR.window_metrics vs fast pipeline on the V2 subsample."""
import csv
import io
import os
import sys

import numpy as np

ROOT = "E:/学习/李明哲-毕业材料/张博改进"
PKG = os.path.join(ROOT, "docs/evidence/V35-SOURCE-ATTRIBUTION-500K/09-v5-sa-hard-500k")
RES = os.path.join(PKG, "02-remote-run/results/SA-HARD-V5-500k")
CKPT = os.path.join(RES, "checkpoints")
sys.path.insert(0, os.path.join(ROOT, "scripts"))
sys.path.insert(0, os.path.join(ROOT, "docs/evidence/V35-SOURCE-ATTRIBUTION-500K/00-preregistration"))
sys.path.insert(0, os.path.join(PKG, "06-independent-verification"))

import fc6_metrics as fc6
import threshold_recompute as TR
import compute_hard_source_windows as CW

events = []
with io.open(os.path.join(RES, "source-ledger.csv"), encoding="utf-8", newline="") as fh:
    for row in csv.DictReader(fh):
        events.append({"source": row["firstLevelSource"],
                       "nominalFE": int(row["nominalFE"]),
                       "actualFE": int(row["actualFE"]),
                       "candidateId": row["candidateFingerprint"],
                       "objectives": (float(row["Cmax"]), float(row["TEC"]), float(row["TWC"]))})
events.sort(key=lambda e: e["actualFE"])
sub = events[::40]
fpast = TR.read_front_csv(os.path.join(CKPT, "checkpoint-0-decision-front.csv"))
print("fpast size:", len(fpast), "sub events:", len(sub))

g_ref = TR.canonical_groups(sub)
g_fast = CW.fold_groups_fast(sub)
print("groups: ref=%d fast=%d" % (len(g_ref), len(g_fast)))
r_ref = [tuple(g[0]) for g in g_ref]
r_fast = [tuple(g[0]) for g in g_fast]
if r_ref != r_fast:
    only_ref = [p for p in r_ref if p not in set(r_fast)]
    only_fast = [p for p in r_fast if p not in set(r_ref)]
    print("reps differ: only_ref=%d only_fast=%d" % (len(only_ref), len(only_fast)))
    for p in only_ref[:3]:
        print("  ref-only:", p)
    for p in only_fast[:3]:
        print("  fast-only:", p)
else:
    print("reps identical")
# producer sets
ps_ref = {tuple(g[0]): TR.producer_set(g) for g in g_ref}
ps_fast = {tuple(g[0]): TR.producer_set(g) for g in g_fast}
diff_ps = [k for k in ps_ref if ps_ref.get(k) != ps_fast.get(k)]
print("producer set diffs:", len(diff_ps), diff_ps[:3])

nd_ref_fp = fc6.nondominated([list(x) for x in fpast])
nd_fast_fp = CW.nd_fast(fpast)
print("fpast_nd: ref=%d fast=%d equal=%s" % (len(nd_ref_fp), len(nd_fast_fp), nd_ref_fp == nd_fast_fp))

union = nd_ref_fp + [list(g[0]) for g in g_ref]
nd_ref_u = fc6.nondominated([list(x) for x in union])
nd_fast_u = CW.nd_fast(union)
print("union nd: ref=%d fast=%d equal=%s" % (len(nd_ref_u), len(nd_fast_u), nd_ref_u == nd_fast_u))
if nd_ref_u != nd_fast_u:
    sref = sorted(map(tuple, nd_ref_u))
    sfst = sorted(map(tuple, nd_fast_u))
    only_ref = [p for p in sref if p not in set(sfst)]
    only_fast = [p for p in sfst if p not in set(sref)]
    print("  ref-only sample:", only_ref[:3])
    print("  fast-only sample:", only_fast[:3])

m_ref = TR.window_metrics([list(x) for x in fpast], sub)
m_fast = CW.window_metrics_fast(fpast, sub)
print("hvAll: ref=%r fast=%r" % (m_ref["hvAll"], m_fast["hvAll"]))
print("nndAll: ref=%d fast=%d" % (m_ref["nndAll"], m_fast["nndAll"]))
for s in sorted(set(m_ref["perSource"]) | set(m_fast["perSource"])):
    a = m_ref["perSource"].get(s, {})
    b = m_fast["perSource"].get(s, {})
    print("src", s)
    for k in ("nTuplesProduced", "nTuplesExclusive", "nexclND"):
        print("   %s ref=%s fast=%s %s" % (k, a.get(k), b.get(k), "OK" if a.get(k) == b.get(k) else "DIFF"))
    for k in ("whvg", "whvgSharePct", "exclusiveNdSharePct"):
        print("   %s ref=%r fast=%r %s" % (k, a.get(k), b.get(k), "OK" if repr(a.get(k)) == repr(b.get(k)) else "DIFF"))
