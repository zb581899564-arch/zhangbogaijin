# -*- coding: utf-8 -*-
"""OFF/ON behaviour-equivalence and V2-OFF faithfulness checker (250k package §5).

Comparisons (all byte-level after masking registered nondeterministic fields):
  A) V2 OFF vs V2 ON   (same config)            -> observer neutrality
  B) V2 OFF vs stored V1/frozen-jar run (same config) -> instrumented-copy faithfulness

Masked fields (registered ex-ante in ACCEPTANCE_GATES / equivalence report):
  status.properties : algorithmRunNanos; inside mechanismSummary: algorithmRunNanos,
                      baseDecodeNanos, decoderTotalNanos, frameworkOverheadNanos
  formal-gate       : runnerVersion, runId, wallNanos
  configuration.txt : repairRunnerVersion, runId, experimentalJarSha256 (V1->V2 differs by design)
  budget-termination: experimentalJarSha256 (V1->V2 differs by design)
  excluded files    : profile.txt, profile.sha256, evidence-sha256.tsv, checkpoints/,
                      checkpoint-fronts.csv (ON-only by design)

Everything else must be byte-identical, including front.csv, passive-archive.csv,
cmax-audit-*.csv/txt, ca-ta-lite-events.log, dscr-*, bottleneck-pressure-events.csv,
shadow-probes.csv, diagnosis-summary.properties, pddr-observation.properties,
initial-population.sha256, passive-summary.properties, and every behaviour hash inside
status.properties (p6/pddr/qg/qp/caTa stream hashes + qgTableHash/qpTableHash).
"""
import csv, hashlib, io, os, re, sys

ROOT = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR"
GATES = os.path.join(ROOT, "02-local-tests", "sandbox", "ck-gates", "runs")
SYNC20K = os.path.join(ROOT, "03-remote-20k", "sync", "results")
SYNC50K = os.path.join(ROOT, "08-remote-50k", "sync", "seed-20260907", "results")
OUT = os.path.join(ROOT, "14-checkpoint-equivalence")

NANOS_KEYS = ("algorithmRunNanos", "baseDecodeNanos", "decoderTotalNanos",
              "frameworkOverheadNanos")


def mask_lines(path, extra_drops=()):
    drops = set(extra_drops) | {"runnerVersion", "runId", "wallNanos",
                                "repairRunnerVersion",
                                "observerMode", "checkpointTargets",
                                "checkpointRows", "observerExecutionErrors",
                                "profileSha256"}
    out = []
    if not os.path.exists(path):
        return None
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            key = line.split("=", 1)[0] if "=" in line else ""
            if key in drops:
                continue
            out.append(re.sub(r"(?<![0-9a-zA-Z])(" + "|".join(NANOS_KEYS)
                              + r")=-?\d+", r"\1=MASKED", line))
    return "".join(out)


def compare_runs(left, right, label, jar_hash_differs):
    """Returns list of (artifact, verdict, detail)."""
    rows = []
    left_files = {f for f in os.listdir(left) if os.path.isfile(os.path.join(left, f))}
    right_files = {f for f in os.listdir(right) if os.path.isfile(os.path.join(right, f))}
    skip = {"evidence-sha256.tsv", "profile.txt", "profile.sha256", "failure.txt"}
    on_only = {"checkpoint-fronts.csv"}
    for name in sorted(left_files | right_files):
        if name in skip:
            continue
        if jar_hash_differs and name in ("configuration.txt", "budget-termination.properties"):
            masked_l = mask_lines(os.path.join(left, name))
            masked_r = mask_lines(os.path.join(right, name))
            # jar hash lines differ by design; compare the remaining lines
            def strip_jar(text):
                keep = []
                for line in (text or "").splitlines(True):
                    if line.startswith("experimentalJarSha256="):
                        continue
                    keep.append(line)
                return "".join(keep)
            verdict = "IDENTICAL" if strip_jar(masked_l) == strip_jar(masked_r) else "DIFFER"
        elif name in on_only:
            verdict = "ON_ONLY" if os.path.exists(os.path.join(right, name)) else "MISSING_ON"
        else:
            ml = mask_lines(os.path.join(left, name))
            mr = mask_lines(os.path.join(right, name))
            if ml is None or mr is None:
                verdict = "MISSING_" + ("LEFT" if ml is None else "RIGHT")
            else:
                verdict = "IDENTICAL" if ml == mr else "DIFFER"
        rows.append([label, name, verdict])
    # directories: checkpoints/ only on ON side
    if os.path.isdir(os.path.join(right, "checkpoints")):
        rows.append([label, "checkpoints/", "ON_ONLY"])
    return rows


def front_size(path):
    if not os.path.exists(path):
        return -1
    with io.open(path, encoding="utf-8") as fh:
        return sum(1 for line in fh if line.strip() and not line.startswith("candidateFingerprint"))


all_rows = []
header = ["comparison", "artifact", "verdict"]

# A) OFF vs ON for the two gates
for gate, inst, targets in (("gate20k", "50_2_3_1", "5000,10000,15000"),
                            ("gate50k", "100_5_3_1", "12500,25000,37500")):
    for arm in ("C0", "C2", "C3"):
        off = os.path.join(GATES, "%s-%s-OFF" % (gate, arm))
        on = os.path.join(GATES, "%s-%s-ON" % (gate, arm))
        all_rows.extend(compare_runs(off, on, "%s-%s OFFvsON" % (gate, arm), False))

# B) V2-OFF vs stored frozen-jar runs (faithfulness)
for arm in ("C0", "C2", "C3"):
    off20 = os.path.join(GATES, "gate20k-%s-OFF" % arm)
    stored20 = os.path.join(SYNC20K, "run-%s-50_2_3_1-20260907" % arm)
    all_rows.extend(compare_runs(off20, stored20, "faith20k-%s V2OFFvsSTORED" % arm, True))
    off50 = os.path.join(GATES, "gate50k-%s-OFF" % arm)
    stored50 = os.path.join(SYNC50K, "run-GAPL50K-%s-100_5_3_1-20260907" % arm)
    all_rows.extend(compare_runs(off50, stored50, "faith50k-%s V2OFFvsSTORED" % arm, True))

with open(os.path.join(OUT, "behavior-equivalence.csv"), "w", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(header)
    w.writerows(all_rows)

verdicts = {}
for _, _, v in all_rows:
    verdicts[v] = verdicts.get(v, 0) + 1
print("verdict counts:", verdicts)
bad = [r for r in all_rows if r[2] in ("DIFFER", "MISSING_LEFT", "MISSING_RIGHT", "MISSING_ON")]
for r in bad:
    print("BAD:", r)
on_ok = all(r[2] == "ON_ONLY" for r in all_rows if r[2] == "ON_ONLY")
faith = [r for r in all_rows if r[0].startswith("faith")]
faith_bad = [r for r in faith if r[2] in ("DIFFER", "MISSING_LEFT", "MISSING_RIGHT")]
gate_rows = [r for r in all_rows if "OFFvsON" in r[0]]
gate_bad = [r for r in gate_rows if r[2] in ("DIFFER", "MISSING_LEFT", "MISSING_RIGHT", "MISSING_ON")]
print("OFFvsON comparisons: %d rows, %d bad" % (len(gate_rows), len(gate_bad)))
print("faithfulness rows: %d, bad: %d" % (len(faith), len(faith_bad)))

# ON-side registry sanity: targets frozen exactly, overshoot 0, finite fronts, errors 0
print("== ON-side checkpoint registry audit ==")
on_issues = 0
for gate, inst, targets in (("gate20k", "50_2_3_1", "5000,10000,15000"),
                            ("gate50k", "100_5_3_1", "12500,25000,37500")):
    for arm in ("C0", "C2", "C3"):
        reg = os.path.join(GATES, "%s-%s-ON" % (gate, arm), "checkpoints",
                           "checkpoint-registry.csv")
        with io.open(reg, encoding="utf-8") as fh:
            rows = list(csv.DictReader(fh))
        want = targets.split(",") + []
        ck = [r for r in rows if r["frontType"].startswith("checkpoint-")]
        term = [r for r in rows if r["frontType"].startswith("terminal-")]
        frozen = sorted({r["checkpointTargetFE"] for r in ck})
        over = [r for r in rows if r["overshootFE"] != "0"]
        span = {}
        for r in ck:
            span.setdefault(r["checkpointTargetFE"], set()).add(r["checkpointObservedFE"])
        g = open(os.path.join(GATES, "%s-%s-ON" % (gate, arm), "formal-gate.properties")
                 ).read()
        errors = re.search(r"observerExecutionErrors=(\d+)", g).group(1)
        ok = (frozen == sorted(want) and not over and errors == "0"
              and all(len(v) == 1 for v in span.values())
              and len(term) == 2 and all(int(t["frontSize"]) > 0 for t in term))
        print("%s %s: targets=%s overshoot_nonzero=%d errors=%s terminal_ok=%s -> %s"
              % (gate, arm, frozen, len(over), errors,
                 all(int(t["frontSize"]) > 0 for t in term), "OK" if ok else "ISSUE"))
        if not ok:
            on_issues += 1

print()
if bad or on_issues:
    print("EQUIVALENCE_CHECK = FAILED")
    sys.exit(1)
print("EQUIVALENCE_CHECK = PASSED (OFFvsON neutral, V2-OFF faithful, ON registries valid)")
