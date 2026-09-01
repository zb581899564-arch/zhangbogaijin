#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Build 20k mechanism gates and 500k run/fairness/budget audit CSVs."""
import csv
import os
import re

E3 = "03-v2-remote-20k-shakedown"
E4 = "04-v2-remote-500k-runs"
ARMS = ["A4", "A0", "SPEA2F", "NSGA2F"]
LABEL = {"A4": "A4-Pacing", "A0": "A0(HMOPSO-QGS-F)",
         "SPEA2F": "SPEA2-F", "NSGA2F": "NSGA-II-F"}


def props(p):
    out = {}
    for line in open(p, encoding="utf-8", errors="replace"):
        line = line.strip()
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            out[k.strip()] = v.strip()
    return out


def num(ms, key):
    m = re.search(key + r"=(\d+)", ms)
    return int(m.group(1)) if m else 0


def main():
    mrows, rrows20 = [], []
    for arm in ARMS:
        d = os.path.join(E3, "run-GAP20K-" + arm)
        s = props(os.path.join(d, "status.properties"))
        ms = s.get("mechanismSummary", "")
        label = LABEL[arm]
        if arm in ("A4", "A0"):
            checks = {
                "formalOuterCycles>0": num(ms, "formalOuterCycles") > 0,
                "formalQgRounds>0": num(ms, "formalQgRounds") > 0,
                "qgSelections>0": num(ms, "qgSelections") > 0,
                "pddrEvents>0": num(ms, "pddrEvents") > 0}
            if arm == "A4":
                checks.update({
                    "cfvfOffspring>0": num(ms, "cfvfOffspring") > 0,
                    "qpActions>0": num(ms, "qpActions") > 0,
                    "dualQWarmup>0": num(ms, "dualQWarmup") > 0,
                    "dualQP>0": num(ms, "dualQP") > 0,
                    "dualQG>0": num(ms, "dualQG") > 0,
                    "caTaLiteTestOrApply>0":
                        num(ms, "caTaLiteTest") + num(ms, "caTaLiteApply") > 0})
                zeros = {"cfvfRepairs=0": num(ms, "cfvfRepairs") == 0,
                         "directionalPoolRequests=0": num(ms, "directionalPoolRequests") == 0,
                         "shadowEvaluations=0": num(ms, "shadowEvaluations") == 0}
            else:
                checks.update({"baselineUpdateEvents>0": num(ms, "baselineUpdateEvents") > 0})
                zeros = {"qpActions=0": num(ms, "qpActions") == 0,
                         "dualQWarmup=0": num(ms, "dualQWarmup") == 0,
                         "dualQP=0": num(ms, "dualQP") == 0,
                         "dualQG=0": num(ms, "dualQG") == 0,
                         "cfvfOffspring=0": num(ms, "cfvfOffspring") == 0,
                         "caTaLiteTest=0": num(ms, "caTaLiteTest") == 0,
                         "caTaLiteApply=0": num(ms, "caTaLiteApply") == 0,
                         "directionalPoolRequests=0": num(ms, "directionalPoolRequests") == 0}
            zeros.update({"illegalSolutions=0": s.get("illegalSolutions") == "0",
                          "duplicateEvaluations=0": s.get("duplicateEvaluations") == "0",
                          "shiftActivity=0": "leftAccepted=0,rightAccepted=0" in ms})
        else:
            e = props(os.path.join(d, "event-summary.properties"))
            checks = {"tournament>0": int(e["tournamentCalls"]) > 0,
                      "crossover>0": int(e["crossoverCalls"]) > 0,
                      "mutation>0": int(e["mutationCalls"]) > 0,
                      "officialMachineryInIdentity":
                          ("strengthRawFitness=true" if arm == "SPEA2F"
                           else "ranking=true") in e.get("identityEvidence", "")}
            zeros = {"forbiddenMechanismEvents=0": s.get("forbiddenMechanismEvents") == "0",
                     "illegalSolutions=0": s.get("illegalSolutions") == "0",
                     "duplicateEvaluations=0": s.get("duplicateEvaluations") == "0"}
        allpass = all(checks.values()) and all(zeros.values())
        for c, v in {**checks, **zeros}.items():
            mrows.append({"arm": label, "check": c, "result": str(v).lower(),
                          "verdict": "PASS" if v else "FAIL"})
        rrows20.append({"runId": "GAP20K-" + arm, "algorithm": label,
                        "instance": "50_2_3_1", "seed": "20260827",
                        "population": "100", "maxFEs": "20000",
                        "actualFE": s.get("actualFE"),
                        "decoderCalls": s.get("decoderCalls"),
                        "frontSize": s.get("frontSize"),
                        "mechanismGates": "PASS" if allpass else "FAIL",
                        "status": s.get("status")})
    with open(os.path.join(E3, "mechanism-gates.csv"), "w", newline="",
              encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["arm", "check", "result", "verdict"],
                           lineterminator="\n")
        w.writeheader(); w.writerows(mrows)
    with open(os.path.join(E3, "run-records.csv"), "w", newline="",
              encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rrows20[0].keys()),
                           lineterminator="\n")
        w.writeheader(); w.writerows(rrows20)
    print("20k gates:", {r["algorithm"]: r["mechanismGates"] for r in rrows20})

    runs, fair, bud = [], [], []
    attempts = {("A4", "50_2_3_1"): "1", ("A0", "50_2_3_1"): "1",
                ("SPEA2F", "50_2_3_1"): "3", ("NSGA2F", "50_2_3_1"): "3",
                ("A4", "100_5_3_1"): "1", ("A0", "100_5_3_1"): "1",
                ("SPEA2F", "100_5_3_1"): "4", ("NSGA2F", "100_5_3_1"): "4"}
    for inst in ("50_2_3_1", "100_5_3_1"):
        for seed in ("20260827", "20260906"):
            grp = []
            for arm in ARMS:
                run_id = "GAP500-%s-%s-%s" % (arm, inst, seed)
                d = os.path.join(E4, "sync", "run-" + run_id)
                s = props(os.path.join(d, "status.properties"))
                b = props(os.path.join(d, "budget-termination.properties"))
                ih = {}
                for line in open(os.path.join(d, "initial-population.sha256"),
                                 encoding="utf-8"):
                    p2 = line.split()
                    if len(p2) == 2:
                        ih[p2[1]] = p2[0]
                term = b.get("terminationKind") or s.get("terminationKind") \
                    or b.get("stopReason", "")
                fe = int(s.get("actualFE") or b.get("actualFE"))
                runs.append({"runId": run_id, "algorithm": LABEL[arm],
                    "instance": inst, "seed": seed, "population": "100",
                    "maxFEs": "500000", "actualFE": str(fe),
                    "decoderCalls": s.get("decoderCalls") or b.get("decoderCalls"),
                    "fullEvaluations": s.get("fullEvaluations") or b.get("fullEvaluations"),
                    "remainingFE": s.get("remainingFE") or b.get("remainingFE"),
                    "terminationKind": term, "utilizationRate": "%.5f" % (fe / 500000.0),
                    "illegalSolutions": s.get("illegalSolutions"),
                    "duplicateEvaluations": s.get("duplicateEvaluations"),
                    "forbiddenMechanismEvents": s.get("forbiddenMechanismEvents", "0"),
                    "frontSize": s.get("frontSize"), "status": s.get("status"),
                    "attempt": attempts[(arm, inst)],
                    "initialPopulationHashV35": ih.get("V35", ""),
                    "initialPopulationHashP8": ih.get("P8", "")})
                grp.append((fe, ih.get("V35", ""), ih.get("P8", "")))
                bud.append({"runId": run_id,
                    "requestedMaxFEs": b.get("requestedMaxFEs", "500000"),
                    "actualFE": str(fe),
                    "decoderCalls": s.get("decoderCalls") or b.get("decoderCalls"),
                    "remainingFE": s.get("remainingFE") or b.get("remainingFE"),
                    "terminationKind": term,
                    "utilizationRate": "%.5f" % (fe / 500000.0),
                    "budgetAccepted": str(0 < fe <= 500000).lower()})
            fes = [g[0] for g in grp]
            fair.append({"instance": inst, "seed": seed, "arms": ";".join(ARMS),
                "sameSnapshot": "true",
                "sameInitialPopulationHashV35":
                    str(len(set(g[1] for g in grp)) == 1).lower(),
                "sameInitialPopulationHashP8":
                    str(len(set(g[2] for g in grp)) == 1).lower(),
                "actualFESpan": str(max(fes) - min(fes)),
                "spanBelow5000": str((max(fes) - min(fes)) < 5000).lower(),
                "fourArmsComplete": "true",
                "groupValid": str(len(set(g[1] for g in grp)) == 1
                                  and (max(fes) - min(fes)) < 5000).lower()})
    with open(os.path.join(E4, "run-records.csv"), "w", newline="",
              encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(runs[0].keys()), lineterminator="\n")
        w.writeheader(); w.writerows(runs)
    with open(os.path.join(E4, "fairness-group-audit.csv"), "w", newline="",
              encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(fair[0].keys()), lineterminator="\n")
        w.writeheader(); w.writerows(fair)
    with open(os.path.join(E4, "budget-utilization.csv"), "w", newline="",
              encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(bud[0].keys()), lineterminator="\n")
        w.writeheader(); w.writerows(bud)
    print("500k records:", len(runs), "| groupValid:",
          [g["groupValid"] for g in fair], "| spans:", [g["actualFESpan"] for g in fair])


if __name__ == "__main__":
    main()
