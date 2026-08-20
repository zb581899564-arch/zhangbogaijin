#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""FC-6A.1 PDDR Population Composition Audit — offline analysis.
Produces per-(algorithm,instance) §12 tables from fc6diagComp lines.
Input: 09-STAGE6-COMPOSITION/raw/**/(front.csv, mechanism-summary.txt)
Output: tables/*.csv + summary stats printed
"""
import re, csv, pathlib, statistics, collections

ROOT = pathlib.Path(__file__).parent
RAW = ROOT / "raw"
TABLES = ROOT / "tables"
TABLES.mkdir(parents=True, exist_ok=True)

COMP_RE = re.compile(
    r"fc6diagComp\s+(\d+)\s+fe=(\d+)\s+pool=(\d+)\s+target=(\d+)\s+"
    r"nLT1=(\d+)\s+nEq1=(\d+)\s+nGt1=(\d+)\s+nND=(\d+)\s+"
    r"selLT1=(\d+)\s+selEq1=(\d+)\s+selGt1=(\d+)\s+"
    r"rejLT1=(\d+)\s+rejEq1=(\d+)\s+rejGt1=(\d+)\s+"
    r"bpLT1=(\d+)\s+bpEq1=(\d+)\s+bpGt1=(\d+)"
)

def parse_file(path):
    rows = []
    text = path.read_text(encoding="utf-8", errors="replace")
    for m in COMP_RE.finditer(text):
        cycle, fe, pool, target, nLT1, nEq1, nGt1, nND, selLT1, selEq1, selGt1, rejLT1, rejEq1, rejGt1, bpLT1, bpEq1, bpGt1 = map(int, m.groups())
        rows.append({
            "cycle": cycle, "fe": fe, "pool": pool, "target": target,
            "nLT1": nLT1, "nEq1": nEq1, "nGt1": nGt1, "nND": nND,
            "selLT1": selLT1, "selEq1": selEq1, "selGt1": selGt1,
            "rejLT1": rejLT1, "rejEq1": rejEq1, "rejGt1": rejGt1,
            "bpLT1": bpLT1, "bpEq1": bpEq1, "bpGt1": bpGt1,
        })
    return rows

def q1_q3(arr):
    if not arr: return (None, None)
    s = sorted(arr)
    n = len(s)
    # linear interpolation percentile 25/75
    def perc(p):
        k = (n-1)*p/100
        f = int(k); c = min(f+1, n-1)
        d = k-f
        return s[f]*(1-d)+s[c]*d
    return perc(25), perc(75)

# Collect
records = []  # per-cycle
# QGS: raw/QGS/*/runs/seed-*/HMOPSO_QGS_F/mechanism-summary.txt
for p in (RAW / "QGS").rglob("mechanism-summary.txt"):
    # deduce instance and seed
    # path like .../QGS/20_2_3_1/runs/seed-20260822/...
    parts = p.parts
    # find instance
    inst = None; seed = None
    for i, part in enumerate(parts):
        if part in ("20_2_3_1", "100_2_3_1"):
            inst = part
            # next parts contain seed
            for pp in parts[i:]:
                if pp.startswith("seed-"):
                    seed = pp.replace("seed-","")
                    break
            break
    rows = parse_file(p)
    for r in rows:
        r["algorithm"] = "QGS"
        r["instance"] = inst
        r["seed"] = seed
        records.append(r)

# BASE: raw/BASE/*/seed-*/mechanism-summary.txt
for p in (RAW / "BASE").rglob("mechanism-summary.txt"):
    parts = p.parts
    inst = None; seed=None
    for i, part in enumerate(parts):
        if part in ("20_2_3_1", "100_2_3_1"):
            inst = part
            for pp in parts[i:]:
                if pp.startswith("seed-"):
                    seed = pp.replace("seed-","")
                    break
            break
    rows = parse_file(p)
    for r in rows:
        r["algorithm"] = "BASE"
        r["instance"] = inst
        r["seed"] = seed
        records.append(r)

print(f"Total cycles parsed: {len(records)}")
# group by (algorithm, instance)
groups = collections.defaultdict(list)
for r in records:
    groups[(r["algorithm"], r["instance"])].append(r)

# Per-group stats
summary_rows = []
for (alg, inst), lst in sorted(groups.items()):
    pools = [x["pool"] for x in lst]
    nNDs = [x["nND"] for x in lst]
    nLT1s = [x["nLT1"] for x in lst]
    nEq1s = [x["nEq1"] for x in lst]
    nGt1s = [x["nGt1"] for x in lst]
    selLT1s = [x["selLT1"] for x in lst]
    selEq1s = [x["selEq1"] for x in lst]
    selGt1s = [x["selGt1"] for x in lst]
    rejEq1s = [x["rejEq1"] for x in lst]
    rejLT1s = [x["rejLT1"] for x in lst]
    # derived
    RCs = [x["nLT1"]/100 for x in lst]
    RBs = [x["nEq1"]/100 for x in lst]
    p_lt1_gt100 = sum(1 for x in lst if x["nLT1"]>100)/len(lst) if lst else 0
    p_nd_gt100 = sum(1 for x in lst if x["nND"]>100)/len(lst) if lst else 0
    def stats(arr):
        med = statistics.median(arr) if arr else None
        q1, q3 = q1_q3(arr)
        return (med, q1, q3, min(arr) if arr else None, max(arr) if arr else None)
    for name, arr in [("pool",pools),("nND",nNDs),("nLT1",nLT1s),("nEq1",nEq1s),("nGt1",nGt1s),("selLT1",selLT1s),("selEq1",selEq1s),("rejEq1",rejEq1s),("RC",RCs),("RB",RBs)]:
        med,q1,q3,mi,ma = stats(arr)
        summary_rows.append([alg, inst, name, len(arr), med, q1, q3, mi, ma])
    summary_rows.append([alg, inst, "P_nLT1>100", len(lst), p_lt1_gt100, "", "", "", ""])
    summary_rows.append([alg, inst, "P_nND>100", len(lst), p_nd_gt100, "", "", "", ""])

# Write summary csv
with open(TABLES / "summary_by_group.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["algorithm","instance","metric","n","median","Q1","Q3","min","max"])
    w.writerows(summary_rows)

# Write per-cycle detail
with open(TABLES / "per_cycle_detail.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["algorithm","instance","seed","cycle","fe","pool","target","nLT1","nEq1","nGt1","nND","selLT1","selEq1","selGt1","rejLT1","rejEq1","rejGt1","bpLT1","bpEq1","bpGt1"])
    for r in sorted(records, key=lambda x:(x["algorithm"],x["instance"],x["seed"],x["cycle"])):
        w.writerow([r["algorithm"],r["instance"],r["seed"],r["cycle"],r["fe"],r["pool"],r["target"],r["nLT1"],r["nEq1"],r["nGt1"],r["nND"],r["selLT1"],r["selEq1"],r["selGt1"],r["rejLT1"],r["rejEq1"],r["rejGt1"],r["bpLT1"],r["bpEq1"],r["bpGt1"]])

# Print human-readable summary
for (alg,inst) in sorted(groups.keys()):
    lst = groups[(alg,inst)]
    print(f"\n=== {alg} {inst}  cycles={len(lst)} ===")
    for rname in ["pool","nND","nLT1","nEq1","nGt1","selLT1","selEq1","rejEq1"]:
        arr = [x[rname] for x in lst]
        med = statistics.median(arr)
        q1,q3 = q1_q3(arr)
        print(f"  {rname:8s} median={med:6.1f} Q1={q1:6.1f} Q3={q3:6.1f} min={min(arr):3d} max={max(arr):3d}")
    p_lt1 = sum(1 for x in lst if x["nLT1"]>100)/len(lst)
    p_nd = sum(1 for x in lst if x["nND"]>100)/len(lst)
    print(f"  P(nLT1>100)={p_lt1:.3f}  P(nND>100)={p_nd:.3f}  RC_median={statistics.median([x['nLT1']/100 for x in lst]):.3f}  RB_median={statistics.median([x['nEq1']/100 for x in lst]):.3f}")

print("\nWritten tables/per_cycle_detail.csv and tables/summary_by_group.csv")
