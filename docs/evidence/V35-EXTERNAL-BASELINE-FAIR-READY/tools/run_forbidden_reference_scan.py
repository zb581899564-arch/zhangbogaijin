#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Static forbidden-reference scan for the external fair baselines.

Scans the two isolated official jMetal 5.8 copies, the engine and the external
fair-baseline runner. Strips // and /* */ comments and string/char literals
before matching, so documentation that ENUMERATES forbidden mechanisms is not
counted as a code reference. Also scans for objective slots 2..5 reads/writes
outside the adapter boundary and for Shift/local-search references.
Outputs 02-adapter-audit/forbidden-reference-scan.csv.
"""
import csv
import os
import re
import sys

PHASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(PHASE)))
ROOT = os.path.join(REPO, "java-jmetal58")

TARGETS = [
    "jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mypso/v35/p25e/official/OfficialJMetal58NSGAII.java",
    "jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mypso/v35/p25e/official/OfficialJMetal58SPEA2.java",
    "jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mypso/v35/p25e/V35P25EOfficialJMetalEngine.java",
    "jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mypso/v35/p25e/V35FourVectorVariation.java",
    "jmetal-problem/src/main/java/org/uma/jmetal/problem/multiobjective/dfsp/fatigue/V35ComparisonProblemAdapter.java",
    "jmetal-problem/src/main/java/org/uma/jmetal/problem/multiobjective/dfsp/fatigue/V35ComparisonSolution.java",
    "jmetal-exec/src/main/java/org/uma/jmetal/runner/lc_psode/ZhangBoV35ExternalFairBaselineRunner.java",
]

FORBIDDEN = [
    ("V35P25DComparativeEngine", r"V35P25DComparativeEngine"),
    ("ZhangBoBaselineUpdater", r"ZhangBoBaselineUpdater"),
    ("CFVF", r"\bCfvf\w*|CFVF"),
    ("DSCR", r"\bDscr\w*|DSCR"),
    ("CA-TA/CaTa", r"\bCaTa\w*|CA-TA"),
    ("PDDR", r"\bPddr\w*|PDDR"),
    ("directionalTeacherPool", r"directionalTeacherPool|TeacherPool"),
    ("inheritedLS", r"inheritedLocalSearch|InheritedLS"),
    ("O1-O13 gateways", r"V35MacroCandidateGateway|MacroNeighborhood"),
    ("dualQ", r"\bdualQ\b|DualQ"),
    ("personal archive", r"PersonalArchive|personalArchive"),
]

OBJECTIVE_SLOT_READS = r"getObjective\(\s*[2-5]\s*\)"
OBJECTIVE_SLOT_WRITES = re.compile(r"setObjective\(\s*[2-5]\s*,\s*([^)]*)\)")
# Allowed slot 2..5 writes: literal zero-fill ("0.0", the AUTHOR_SEVEN_SLOT shim,
# unreachable by the official cores because the engine enforces THREE_OBJECTIVE)
# and the TWC mapping "canonical.getObjective(6" into slot 2 of the three-objective
# view. Slot 2 IS TWC in that view, so this is the [0,1,6] mapping itself, not a
# diagnostic-slot read. Any other slot 2..5 write, and ANY slot 2..5 read, is a
# violation. (A negative lookahead after \\s* is unsafe here: backtracking can
# anchor the lookahead on the whitespace, so the value is captured and filtered.)
SHIFT = r"ShiftMode\.(?!NONE\b)\w+|ZhangBoFatigueShift"


def strip_comments_and_strings(text):
    text = re.sub(r"/\*.*?\*/", " ", text, flags=re.S)
    text = re.sub(r"//[^\n]*", " ", text)
    text = re.sub(r'"(?:\\.|[^"\\])*"', '""', text)
    text = re.sub(r"'(?:\\.|[^'\\])*'", "''", text)
    return text


def main():
    rows = []
    overall_pass = True
    for rel in TARGETS:
        path = os.path.join(ROOT, rel)
        raw = open(path, encoding="utf-8").read()
        code = strip_comments_and_strings(raw)
        for label, pattern in FORBIDDEN:
            hits = len(re.findall(pattern, code))
            verdict = "PASS" if hits == 0 else "VIOLATION"
            if hits:
                overall_pass = False
            rows.append({"scanTarget": os.path.basename(rel), "scope": "code",
                         "check": label, "hits": str(hits), "verdict": verdict})
        slot_hits = len(re.findall(OBJECTIVE_SLOT_READS, code))
        for m in OBJECTIVE_SLOT_WRITES.finditer(code):
            value = m.group(1).strip()
            if value == "0.0" or value.startswith("canonical.getObjective(6"):
                continue
            slot_hits += 1
        rows.append({"scanTarget": os.path.basename(rel), "scope": "code",
                     "check": "objective slots 2-5 reads/writes",
                     "hits": str(slot_hits),
                     "verdict": "PASS" if slot_hits == 0 else "VIOLATION"})
        if slot_hits:
            overall_pass = False
        shift_hits = len(re.findall(SHIFT, code))
        rows.append({"scanTarget": os.path.basename(rel), "scope": "code",
                     "check": "non-NONE shift usage",
                     "hits": str(shift_hits),
                     "verdict": "PASS" if shift_hits == 0 else "VIOLATION"})
        if shift_hits:
            overall_pass = False

    out = os.path.join(PHASE, "02-adapter-audit", "forbidden-reference-scan.csv")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["scanTarget", "scope", "check", "hits", "verdict"],
                           lineterminator="\n")
        w.writeheader()
        w.writerows(rows)
    violations = [r for r in rows if r["verdict"] != "PASS"]
    print("targets=%d checks=%d violations=%d overall=%s" % (
        len(TARGETS), len(rows), len(violations),
        "PASS" if overall_pass else "FAIL"))
    for r in violations:
        print(r)
    return 0 if overall_pass else 1


if __name__ == "__main__":
    sys.exit(main())
