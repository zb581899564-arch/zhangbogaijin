# -*- coding: utf-8 -*-
"""V3 source-diagnostics OFF/ON equivalence + faithfulness checker.

A) V3 OFF vs V3 ON (same config)          -> observer/ledger neutrality
B) V3 OFF vs stored frozen-jar run        -> instrumented-copy faithfulness
Masked (registered ex-ante): runnerVersion/runId/wallNanos/observer fields
(observerMode, checkpointTargets, checkpointRows, observerExecutionErrors,
telemetryLedgerRows, telemetryPddrRounds, telemetryLedgerErrors)/
experimentalJarSha256 + derived profileSha256.  ON-only artifacts:
source-ledger.csv, pddr-round-ledger.csv, checkpoints/, checkpoint-fronts.csv.
Everything else must be byte-identical.
"""
import csv, hashlib, io, os, re, sys

AUD = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1"
GATES = os.path.join(AUD, "02-local-tests", "src-gates", "runs")
SYNC20K = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR\03-remote-20k\sync\results"
SYNC50K = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR\08-remote-50k\sync\seed-20260907\results"
OUT = os.path.join(AUD, "03-equivalence-gates")

MASK_KEYS = {"runnerVersion", "runId", "wallNanos", "repairRunnerVersion",
             "observerMode", "checkpointTargets", "checkpointRows",
             "observerExecutionErrors", "telemetryLedgerRows",
             "telemetryPddrRounds", "telemetryLedgerErrors", "profileSha256",
             "poolLevelAttribution"}
NANOS_KEYS = ("algorithmRunNanos", "baseDecodeNanos", "decoderTotalNanos",
              "frameworkOverheadNanos")
ON_ONLY = {"source-ledger.csv", "pddr-round-ledger.csv", "checkpoint-fronts.csv"}
SKIP = {"evidence-sha256.tsv", "profile.txt", "profile.sha256", "failure.txt"}


def mask_lines(path):
    out = []
    if not os.path.exists(path):
        return None
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            key = line.split("=", 1)[0] if "=" in line else ""
            if key in MASK_KEYS:
                continue
            out.append(re.sub(r"(" + "|".join(NANOS_KEYS) + r")=-?\d+", r"\1=MASKED", line))
    return "".join(out)


def compare(left, right, label, jar_differs):
    rows = []
    lf = {f for f in os.listdir(left) if os.path.isfile(os.path.join(left, f))}
    rf = {f for f in os.listdir(right) if os.path.isfile(os.path.join(right, f))}
    for name in sorted(lf | rf):
        if name in SKIP:
            continue
        lp, rp = os.path.join(left, name), os.path.join(right, name)
        if name in ON_ONLY:
            rows.append([label, name, "ON_ONLY" if os.path.exists(rp) else "MISSING_ON"])
            continue
        def masked(p):
            text = mask_lines(p)
            if text is None:
                return None
            if jar_differs and name in ("configuration.txt", "budget-termination.properties"):
                text = "".join(l for l in text.splitlines(True)
                               if not l.startswith("experimentalJarSha256="))
            return text
        ml, mr = masked(lp), masked(rp)
        if ml is None or mr is None:
            rows.append([label, name, "MISSING_" + ("LEFT" if ml is None else "RIGHT")])
        else:
            rows.append([label, name, "IDENTICAL" if ml == mr else "DIFFER"])
    if os.path.isdir(os.path.join(right, "checkpoints")):
        rows.append([label, "checkpoints/", "ON_ONLY"])
    return rows


rows = []
# A) OFF vs ON (C0, both instances, 2k + 20k)
for inst in ("50_2_3_1", "100_5_3_1"):
    for budget in ("2k", "20k"):
        off = os.path.join(GATES, "gate%s-%s-C0-OFF" % (budget, inst))
        on = os.path.join(GATES, "gate%s-%s-C0-ON" % (budget, inst))
        if os.path.isdir(on):
            rows.extend(compare(off, on, "%s-%s OFFvsON" % (budget, inst), False))
# B) faithfulness: V3-OFF vs stored frozen-jar runs
for arm in ("C0", "C2", "C3"):
    rows.extend(compare(
        os.path.join(GATES, "gate20k-50_2_3_1-%s-OFF" % arm),
        os.path.join(SYNC20K, "run-%s-50_2_3_1-20260907" % arm),
        "faith20k-%s V3OFFvsSTORED" % arm, True))
    rows.extend(compare(
        os.path.join(GATES, "gate50k-100_5_3_1-%s-OFF" % arm),
        os.path.join(SYNC50K, "run-GAPL50K-%s-100_5_3_1-20260907" % arm),
        "faith50k-%s V3OFFvsSTORED" % arm, True))

with open(os.path.join(OUT, "behavior-equivalence.csv"), "w", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(["comparison", "artifact", "verdict"])
    w.writerows(rows)

counts = {}
for _, _, v in rows:
    counts[v] = counts.get(v, 0) + 1
print("verdict counts:", counts)
bad = [r for r in rows if r[2] not in ("IDENTICAL", "ON_ONLY")]
for r in bad:
    print("BAD:", r)

# ON-side ledger audits
print("== ON-side ledger audits ==")
issues = 0
for inst, budget, fe_exp, rounds_exp in (("50_2_3_1", "20k", 15258, 2),
                                          ("100_5_3_1", "20k", 15258, 2),
                                          ("50_2_3_1", "2k", 100, 0),
                                          ("100_5_3_1", "2k", 100, 0)):
    p = os.path.join(GATES, "gate%s-%s-C0-ON" % (budget, inst), "source-ledger.csv")
    lrows = list(csv.DictReader(open(p, encoding="utf-8")))
    dist = {}
    for r in lrows:
        dist[r["source"]] = dist.get(r["source"], 0) + 1
    unset = dist.get("UNSET", 0)
    pp = os.path.join(GATES, "gate%s-%s-C0-ON" % (budget, inst), "pddr-round-ledger.csv")
    prows = list(csv.DictReader(open(pp, encoding="utf-8-sig")))
    rounds = len(set(r["cycle"] for r in prows))
    sel = sum(1 for r in prows if r["selectedByPddr"] == "true")
    ok = (len(lrows) == fe_exp and unset == 0 and rounds == rounds_exp
          and sel == rounds_exp * 100)
    print("%s %s: rows=%d dist=%s unset=%d pddrRounds=%d selected=%d -> %s"
          % (inst, budget, len(lrows), dist, unset, rounds, sel, "OK" if ok else "ISSUE"))
    if not ok:
        issues += 1

if bad or issues:
    print("EQUIVALENCE_CHECK_V3 = FAILED")
    sys.exit(1)
print("EQUIVALENCE_CHECK_V3 = PASSED")
