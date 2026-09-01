#!/usr/bin/env python3
# V35-LOCAL-FE-PACING-50K T4 acceptance check + T5 CSV generation (Agent B).
import csv, os, re, sys, io

BASE = os.path.dirname(os.path.abspath(__file__))
SYNC = os.path.join(BASE, "sync")
SEEDS = ["20260907", "20260914"]
INSTS = ["50_2_3_1", "100_5_3_1"]
PROFILES = ["C0", "C1", "C2", "C3"]

BETA_MAX = {"C0": "0.650000", "C1": "0.550000", "C2": "0.450000", "C3": "0.350000"}
# Preregistered full-consumption schedule predictions (Agent A model).
PRED = {
    "C0": {"cycles": 6, "kind": "PHASE_CONSISTENT_TAIL_STOP", "finalFE": 48269, "totalLocalFE": 18169},
    "C1": {"cycles": 6, "kind": "PHASE_CONSISTENT_TAIL_STOP", "finalFE": 45359, "totalLocalFE": 15259},
    "C2": {"cycles": 7, "kind": "EXACT_MAX_FE", "finalFE": 50000, "totalLocalFE": 14900},
    "C3": {"cycles": 7, "kind": "PHASE_CONSISTENT_TAIL_STOP", "finalFE": 49036, "totalLocalFE": 13936},
}
# Snapshot hashes from PREUPLOAD_SHA256.tsv (frozen prereg).
SNAP = {
    ("50_2_3_1", "20260907"): "79d1de2a1217f2632e0cc45cad1502c89390d5da8ea83527fe86e72d1190c187",
    ("100_5_3_1", "20260907"): "57ecc78628495c864abbe1d149d7b2e936a2e1d439d16b068de03e3c64dafec1",
    ("50_2_3_1", "20260914"): "5722f3d5319ea31834b0b2f241668193318b23502444b899f3a8f861466df6db",
    ("100_5_3_1", "20260914"): "26e0258a4f406101f622336453fe99f3f0ec8575a24d52ee0e689656679cc3e6",
}

def read_kv(path):
    d = {}
    with io.open(path, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.rstrip("\n")
            if "=" in line and not line.startswith("#"):
                k, v = line.split("=", 1)
                d[k] = v
    return d

def mech_get(ms, key):
    m = re.search(r"(?:^|,)" + re.escape(key) + r"=([^,]*)", ms or "")
    return m.group(1) if m else None

def dec_get(dec, key):
    m = re.search(r"(?:^|,)" + re.escape(key) + r"=([^,]*)", dec or "")
    return m.group(1) if m else None

def parse_init_pop(path):
    out = {}
    with io.open(path, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            parts = line.split()
            if len(parts) >= 2:
                out[parts[1]] = parts[0]
    return out

runs = []
for seed in SEEDS:
    for inst in INSTS:
        for prof in PROFILES:
            rk = "run-GAPL50K-%s-%s-%s" % (prof, inst, seed)
            d = os.path.join(SYNC, "seed-" + seed, "results", rk)
            if not os.path.isdir(d):
                runs.append({"runKey": rk, "profile": prof, "instance": inst, "seed": seed, "missing": True})
                continue
            fg = read_kv(os.path.join(d, "formal-gate.properties"))
            bt = read_kv(os.path.join(d, "budget-termination.properties"))
            st = read_kv(os.path.join(d, "status.properties"))
            pd = read_kv(os.path.join(d, "pddr-observation.properties"))
            pf = read_kv(os.path.join(d, "profile.txt"))
            ms = st.get("mechanismSummary", "")
            dec = st.get("decoderTiming", st.get("decoder", ""))
            with io.open(os.path.join(d, "front.csv"), "r", encoding="utf-8", errors="replace") as f:
                front_lines = [ln for ln in f.read().splitlines() if ln.strip()]
            ip = parse_init_pop(os.path.join(d, "initial-population.sha256"))

            r = {
                "runKey": rk, "profile": prof, "instance": inst, "seed": seed, "missing": False,
                "betaMax": pf.get("localFeBudget.betaMax", ""),
                "betaMin": pf.get("localFeBudget.betaMin", ""),
                "maxFEs_pf": pf.get("maxFEs", pf.get("maxEvaluations", "")),
                "actualFE": fg.get("actualFE", ""), "decoderCalls": fg.get("decoderCalls", ""),
                "frontSize": fg.get("frontSize", ""), "status": fg.get("status", ""),
                "failures": fg.get("failures", ""), "frontRows": len(front_lines),
                "remainingFE": bt.get("remainingFE", ""), "terminationKind": bt.get("terminationKind", ""),
                "outerCycles": bt.get("formalOuterCycles", ""), "totalLocalFE": bt.get("totalLocalFE", ""),
                "formalLocalFE": bt.get("formalLocalFE", ""), "caTaLiteFE": bt.get("caTaLiteFE", ""),
                "localFeShare": bt.get("localFeShare", ""), "globalPhaseFE": bt.get("globalPhaseFE", ""),
                "phaseBoundAccepted": bt.get("phaseBoundAccepted", ""),
                "illegalSolutions": st.get("illegalSolutions", ""), "duplicateEvaluations": st.get("duplicateEvaluations", ""),
                "cfvfRepairs": mech_get(ms, "cfvfRepairs"), "cfvfOffspring": mech_get(ms, "cfvfOffspring"),
                "directionalPoolRequests": mech_get(ms, "directionalPoolRequests"),
                "qgSelections": mech_get(ms, "qgSelections"), "qpActions": mech_get(ms, "qpActions"),
                "shadowSamples": mech_get(ms, "shadowSamples"), "shadowEvaluations": mech_get(ms, "shadowEvaluations"),
                "leftShiftNanos": dec_get(dec, "leftShiftNanos"), "rightShiftNanos": dec_get(dec, "rightShiftNanos"),
                "pddrSelectionMode": pd.get("pddrSelectionMode", ""), "observationMode": pd.get("observationMode", ""),
                "initV35": ip.get("V35", ""), "initP8": ip.get("P8", ""),
                "snapshotSha256": SNAP.get((inst, seed), ""),
                "checks": [], "fails": [],
            }

            # Check 1: formal-gate
            c1 = (r["status"] == "COMPLETED" and r["failures"] == "NONE"
                  and r["actualFE"] == r["decoderCalls"]
                  and r["actualFE"].isdigit() and 0 < int(r["actualFE"]) <= 50000)
            r["checks"].append(("formal_gate", c1))
            # Check 2: budget-termination
            c2 = (r["phaseBoundAccepted"] == "true" and r["remainingFE"].isdigit() and int(r["remainingFE"]) < 5000
                  and r["terminationKind"] in ("EXACT_MAX_FE", "PHASE_CONSISTENT_TAIL_STOP")
                  and r["localFeShare"] not in ("", None) and float(r["localFeShare"] or 0) > 0
                  and r["globalPhaseFE"].isdigit() and int(r["globalPhaseFE"]) > 0)
            r["checks"].append(("budget_termination", c2))
            # Check 3: status.properties mechanism zeroing
            c3 = (r["illegalSolutions"] == "0" and r["duplicateEvaluations"] == "0"
                  and r["cfvfRepairs"] == "0" and r["directionalPoolRequests"] == "0"
                  and r["shadowSamples"] == "0" and r["shadowEvaluations"] == "0"
                  and r["leftShiftNanos"] == "0" and r["rightShiftNanos"] == "0")
            r["checks"].append(("status_zeroing", c3))
            # Check 4: front.csv non-empty
            c4 = len(front_lines) > 0
            r["checks"].append(("front_nonempty", c4))
            # Check 5: pddr observation mode
            c5 = (r["pddrSelectionMode"] == "GLOBAL_ORIGINAL" and r["observationMode"] == "POST_HOC_PARSE_ONLY")
            r["checks"].append(("pddr_observation", c5))
            # Check 6: profile beta budget
            c6 = (r["betaMax"] == BETA_MAX[prof] and r["betaMin"] == "0.250000" and r["maxFEs_pf"] == "50000")
            r["checks"].append(("profile_beta", c6))
            # Check 7: schedule prediction
            p = PRED[prof]
            r["schedulePredictedCycles"] = str(p["cycles"])
            r["schedulePredictedKind"] = p["kind"]
            r["schedulePredictedTotalLocal"] = str(p["totalLocalFE"])
            tl = int(r["totalLocalFE"]) if r["totalLocalFE"].isdigit() else -1
            r["scheduleMatch"] = "MATCH" if (str(p["cycles"]) == r["outerCycles"]
                                             and p["kind"] == r["terminationKind"]
                                             and abs(tl - p["totalLocalFE"]) <= 250) else "MISMATCH"
            r["checks"].append(("schedule", r["scheduleMatch"] == "MATCH"))  # recorded, not gating
            r["fails"] = [name for name, ok in r["checks"] if not ok and name != "schedule"]
            r["acceptance"] = "PASS" if not r["fails"] else "FAIL"
            runs.append(r)

# ---- write run-acceptance-50k.csv ----
cols = ["runKey","profile","instance","seed","betaMax","actualFE","decoderCalls","remainingFE",
        "terminationKind","outerCycles","totalLocalFE","formalLocalFE","caTaLiteFE","localFeShare",
        "globalPhaseFE","cfvfOffspring","qgSelections","qpActions","frontSize","status","failures",
        "schedulePredictedCycles","schedulePredictedKind","schedulePredictedTotalLocal","scheduleMatch","acceptance"]
with io.open(os.path.join(BASE, "run-acceptance-50k.csv"), "w", encoding="utf-8", newline="") as f:
    w = csv.DictWriter(f, fieldnames=cols, extrasaction="ignore")
    w.writeheader()
    for r in runs:
        w.writerow({c: r.get(c, "") for c in cols})

# ---- fairness groups ----
groups = []
for seed in SEEDS:
    for inst in INSTS:
        arms = [r for r in runs if r["seed"] == seed and r["instance"] == inst]
        fes = [int(r["actualFE"]) for r in arms if not r.get("missing")]
        v35 = set(r["initV35"] for r in arms if not r.get("missing"))
        p8 = set(r["initP8"] for r in arms if not r.get("missing"))
        snap = set(r["snapshotSha256"] for r in arms)
        spread = (max(fes) - min(fes)) if len(fes) == 4 else -1
        spread_ok = 0 <= spread < 5000
        ident = (len(v35) == 1 and len(p8) == 1 and len(snap) == 1
                 and "" not in v35 and "" not in p8)
        g = {
            "instance": inst, "seed": seed,
            "snapshotSha256": list(snap)[0] if len(snap) == 1 else ";".join(sorted(snap)),
            "initialV35Hash": list(v35)[0] if len(v35) == 1 else ";".join(sorted(v35)),
            "initialP8Hash": list(p8)[0] if len(p8) == 1 else ";".join(sorted(p8)),
            "actualFESpread": spread, "spreadLt5000": str(spread_ok).lower(),
            "fourArmsIdenticalProvenance": str(ident).lower(),
            "verdict": "PASS" if (spread_ok and ident and len(fes) == 4) else "FAIL",
        }
        groups.append(g)

gcols = ["instance","seed","snapshotSha256","initialV35Hash","initialP8Hash","actualFESpread",
         "spreadLt5000","fourArmsIdenticalProvenance","verdict"]
with io.open(os.path.join(BASE, "fairness-group-audit.csv"), "w", encoding="utf-8", newline="") as f:
    w = csv.DictWriter(f, fieldnames=gcols)
    w.writeheader()
    for g in groups:
        w.writerow(g)

# ---- console detail ----
print("=== PER-RUN ===")
for r in runs:
    if r.get("missing"):
        print("%s MISSING" % r["runKey"]); continue
    print("%s acc=%s fails=%s sched=%s FE=%s kind=%s cyc=%s tlFE=%s share=%s" % (
        r["runKey"], r["acceptance"], r["fails"] or "-", r["scheduleMatch"], r["actualFE"],
        r["terminationKind"], r["outerCycles"], r["totalLocalFE"], r["localFeShare"]))
print("=== GROUPS ===")
for g in groups:
    print("%s seed=%s spread=%s spreadOK=%s ident=%s verdict=%s" % (
        g["instance"], g["seed"], g["actualFESpread"], g["spreadLt5000"], g["fourArmsIdenticalProvenance"], g["verdict"]))
n_pass = sum(1 for r in runs if r["acceptance"] == "PASS")
n_match = sum(1 for r in runs if r.get("scheduleMatch") == "MATCH")
g_pass = sum(1 for g in groups if g["verdict"] == "PASS")
print("SUMMARY runsAccepted=%d/16 scheduleMatch=%d/16 fairGroupsPassed=%d/4" % (n_pass, n_match, g_pass))
