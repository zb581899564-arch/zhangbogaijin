#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Parse V35Fc6BpPddrDiagnosticAudit fc6Diag sections into compact CSV tables.

Reads the *.txt files produced from the gzipped mechanism-summary, emits:
  per-cycle geometry/rescue/exposure rollup (one CSV row per datum)
  rescue event detail (one row per rescue)
  rescue exposure summary (one row per rescued fingerprint)
"""
import csv
import glob
import os
import re
import sys

CYCLE_RE = re.compile(
    r"^fc6diagCycle (\d+)\s+fe=(\d+)\s+"
    r"popSize=(\d+)\s+popND=(\d+)\s+archSize=(\d+)\s+"
    r"popRanges=cmax\[([^,]+),([^,]+),([^\]]+)\],tec\[([^,]+),([^,]+),([^\]]+)\],twc\[([^,]+),([^,]+),([^\]]+)\]\s+"
    r"archRanges=cmax\[([^,]+),([^,]+),([^\]]+)\],tec\[([^,]+),([^,]+),([^\]]+)\],twc\[([^,]+),([^,]+),([^\]]+)\]\s+"
    r"rescues=(\d+)\(cmax=(\d+),tec=(\d+),twc=(\d+)\)\s+displaced=(\d+)\s+"
    r"teacherSel=(\d+)\s+cfvfGbestLearn=(\d+)")

RESCUE_RE = re.compile(
    r"^fc6diagRescue (\d+)\s+cycle=(\d+)\s+fe=(\d+)\s+role=([A-Z_]+)\s+"
    r"fp=([^ ]+)\s+Cmax=([^ ]+)\s+TEC=([^ ]+)\s+TWC=([^ ]+)\s+"
    r"q=(\d+)\s+p=(\d+)\s+score=([^ ]+)\s+origRank=(\d+)\s+slot=(\d+)\s+"
    r"lineage=(-?\d+)\s+dFp=([^ ]+)\s+dCmax=([^ ]+)\s+dTEC=([^ ]+)\s+dTWC=([^ ]+)\s+"
    r"dq=(\d+)\s+dp=(\d+)\s+dScore=([^ ]+)\s+dOrigRank=(-?\d+)")

EXPOSURE_RE = re.compile(
    r"^fc6diagExposure (\S+)\s+qg=(\d+)\(G1=(\d+),G2=(\d+),G3=(\d+),G4=(\d+)\)\s+"
    r"cfvfGbest=(\d+)\(G1=(\d+),G2=(\d+),G3=(\d+),G4=(\d+)\)\s+"
    r"cfvfPbest=(\d+)\s+descPresence=(\d+)\s+directPresence=(\d+)\s+"
    r"uniqueDescLineages=(\d+)\s+directLineages=(\d+)\s+"
    r"successPresence=(\d+)\s+nondomFinal=(\d+)")


def fields_of(line):
    """Split a tab-separated fc6diag line into an ordered key->value dict."""
    parts = line.split("\t")
    head = parts[0].split(" ", 1)
    out = {"_0": head[0], "_id": head[1] if len(head) > 1 else ""}
    for part in parts[1:]:
        if "=" in part:
            key, value = part.split("=", 1)
            out[key] = value
    return out


def parse_file(path, out_dir):
    tag = os.path.splitext(os.path.basename(path))[0]
    cycles, rescues, exposures, rollup = [], [], [], {}
    in_diag = False
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            line = line.rstrip("\n")
            if line == "fc6DiagBegin":
                in_diag = True
                continue
            if line == "fc6DiagEnd":
                break
            if not in_diag:
                continue
            if line.startswith("fc6diagRounds="):
                rollup["rounds"] = line.split("=", 1)[1]
            elif line.startswith("fc6diagBoundaryPool="):
                rollup["boundary"] = line.split("=", 1)[1]
            elif line.startswith("fc6diagActualRescues="):
                rollup["rescues"] = line.split("=", 1)[1]
            elif line.startswith("fc6diagDisplacements="):
                rollup["displaced"] = line.split("=", 1)[1]
            elif line.startswith("fc6diagCumulativeRescues="):
                rollup["cumrescues"] = line.split("=", 1)[1]
            elif line.startswith("fc6diagCycle "):
                f = fields_of(line)

                def triples(text):
                    out = []
                    for name in ("cmax", "tec", "twc"):
                        m = re.search(name + r"\[([^\]]+)\]", text)
                        out.extend(m.group(1).split(","))
                    return out

                pop = triples(f["popRanges"])
                arch = triples(f["archRanges"])
                res = re.match(r"(\d+)\(cmax=(\d+),tec=(\d+),twc=(\d+)\)",
                               f["rescues"])
                cycles.append((
                    f["_id"], f["fe"], f["popSize"], f["popND"], f["archSize"],
                    *pop, *arch,
                    res.group(1), res.group(2), res.group(3), res.group(4),
                    f["displaced"], f["teacherSel"], f["cfvfGbestLearn"]))
            elif line.startswith("fc6diagRescue "):
                f = fields_of(line)
                rescues.append((
                    f["_id"], f["cycle"], f["fe"], f["role"], f["fp"],
                    f["Cmax"], f["TEC"], f["TWC"], f["q"], f["p"], f["score"],
                    f["origRank"], f["slot"], f["lineage"],
                    f["dFp"], f["dCmax"], f["dTEC"], f["dTWC"],
                    f["dq"], f["dp"], f["dScore"], f["dOrigRank"]))
            elif line.startswith("fc6diagExposure "):
                f = fields_of(line)
                qg = re.findall(r"[-\d]+", f["qg"])
                gb = re.findall(r"[-\d]+", f["cfvfGbest"])
                exposures.append((
                    f["_id"], qg[0], qg[1], qg[2], qg[3], qg[4],
                    gb[0], gb[1], gb[2], gb[3], gb[4],
                    f["cfvfPbest"], f["descPresence"], f["directPresence"],
                    f["uniqueDescLineages"], f["directLineages"],
                    f["successPresence"], f["nondomFinal"]))
    with open(os.path.join(out_dir, tag + ".cycles.csv"), "w", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(["cycle", "fe", "popSize", "popND", "archSize",
                    "popMinCmax", "popMaxCmax", "popRngCmax",
                    "popMinTEC", "popMaxTEC", "popRngTEC",
                    "popMinTWC", "popMaxTWC", "popRngTWC",
                    "archMinCmax", "archMaxCmax", "archRngCmax",
                    "archMinTEC", "archMaxTEC", "archRngTEC",
                    "archMinTWC", "archMaxTWC", "archRngTWC",
                    "rescTotal", "rescCmax", "rescTec", "rescTwc", "displaced",
                    "teacherSel", "cfvfGbestLearn"])
        w.writerows(cycles)
    with open(os.path.join(out_dir, tag + ".rescues.csv"), "w", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(["id", "cycle", "fe", "role", "fp",
                    "cmax", "tec", "twc", "q", "p", "score",
                    "origRank", "slot", "lineage",
                    "dFp", "dCmax", "dTEC", "dTWC", "dq", "dp", "dScore", "dOrigRank"])
        w.writerows(rescues)
    with open(os.path.join(out_dir, tag + ".exposures.csv"), "w", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(["fp", "qg", "qgG1", "qgG2", "qgG3", "qgG4",
                    "cfvfGbest", "cbG1", "cbG2", "cbG3", "cbG4",
                    "cfvfPbest", "descPresence", "directPresence",
                    "uniqueDescLineages", "directLineages",
                    "successPresence", "nondomFinal"])
        w.writerows(exposures)
    return tag, rollup, len(cycles), len(rescues), len(exposures)


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else "."
    pattern = sys.argv[2] if len(sys.argv) > 2 else "*.txt"
    out_dir = os.path.join(root, "tables")
    os.makedirs(out_dir, exist_ok=True)
    for path in sorted(glob.glob(os.path.join(root, pattern))):
        tag, rollup, nc, nr, ne = parse_file(path, out_dir)
        print("%-24s cycles=%3d rescues=%3d expos=%3d  %s" % (
            tag, nc, nr, ne,
            " ; ".join("%s=%s" % (k, v) for k, v in rollup.items())))


if __name__ == "__main__":
    main()
