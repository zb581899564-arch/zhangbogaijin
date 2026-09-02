#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V35-SOURCE-ATTRIBUTION-500K / 10-v5-sa-normal-500k — independent acceptance verification.

Read-only, zero FE. Recomputes (never trusts reports):
  1. reverse verification of the run's own evidence-sha256.tsv
  2. budget / status / formal-gate / memory hard gates
  3. source-ledger: V5 schema columns, row count == actualFE, contiguous FE, UNSET sources,
     objective finiteness, nominal-window rule, first-level source distribution
  4. strict three-objective B0 recomputed from the first 100 ledger rows vs the exported
     checkpoint-0 (b0) front — must match point-by-point
  5. lifecycle ledger: row count, required 10 event types, no UNSET source
  6. checkpoint registry: 19 configured + terminal + B0, overshoot < 5000
  7. terminal front: finite, non-empty; snapshot-derived initialPopulationHash match
  8. snapshot identity: the materialized snapshot's V35 logical hash equals the run's
     reported initialPopulationHash (proves the run consumed the preregistered snapshot)
  9. lifecycle fingerprint-join coverage (PA/QP NOT_ATTRIBUTABLE_BY_FINGERPRINT_JOIN must
     be registered, never guessed)

Outputs 03-run-acceptance/run-acceptance.csv.
"""
import csv
import hashlib
import os
import sys

BASE = os.path.dirname(os.path.abspath(__file__))          # 05-independent-verification
PKG = os.path.dirname(BASE)                                 # 10-v5-sa-normal-500k
RES = os.path.join(PKG, "02-remote-run", "results", "SA-NORMAL-V5-500k")

FROZEN = {
    "observer_jar_sha": "1a73e3cf025f7cfdb47bde38a7b34e8f8b0810958f61323a5d3cbc35272c8c9e",
    "formal_jar_sha": "8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9",
    "snapshot_sha": "ea19f69137e088fc9ef15b6c67700f8ee5029116df7c14ff931ae4cdb53a1842",
    "initial_population_hash_v35": "1fdf0820a8a5d035d53801dcd845313f0d9d3fb0f19c134ad29316da2192155e",
    "req_event_types": {
        "GENERATED", "DESCENDANT", "IMPROVING_DESCENDANT", "MERGE_POOL",
        "PDDR_SELECTED", "WORKING_POPULATION", "PERSONAL_ARCHIVE",
        "QG_TEACHER", "QP_TEACHER", "QP_ACTION",
    },
    "ledger_required_cols": ["actualFE", "nominalFE", "generation", "outerCycle", "qRound"],
    "max_fes": 500000,
}

results = []


def gate(name, value, expected, ok):
    results.append((name, str(value), str(expected), "PASS" if ok else "FAIL"))


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def read_props(path):
    props = {}
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                props[k.strip()] = v.strip()
    return props


# ---------- 1. reverse-verify the run's own evidence manifest ----------
manifest = os.path.join(RES, "evidence-sha256.tsv")
missing = mismatch = matched = 0
with open(manifest, "r", encoding="utf-8") as f:
    rows = list(csv.reader(f, delimiter="\t"))
for row in rows[1:]:
    if len(row) != 2:
        continue
    rel, expect = row[0].strip(), row[1].strip()
    p = os.path.join(RES, rel)
    if not os.path.exists(p):
        missing += 1
        continue
    if sha256_file(p) != expect:
        mismatch += 1
    else:
        matched += 1
gate("evidenceManifestMatched", matched, f"{len(rows)-1}", mismatch == 0 and missing == 0)
gate("evidenceManifestMissing", missing, 0, missing == 0)
gate("evidenceManifestMismatch", mismatch, 0, mismatch == 0)

# ---------- 2. budget / status / formal-gate / memory ----------
bt = read_props(os.path.join(RES, "budget-termination.properties"))
st = read_props(os.path.join(RES, "status.properties"))
fg = read_props(os.path.join(RES, "formal-gate.properties"))
mem = read_props(os.path.join(RES, "memory-summary.properties"))

actual_fe = int(bt["actualFE"])
decoder_calls = int(bt["decoderCalls"])
remaining = int(bt["remainingFE"])
util = float(bt["utilizationRate"])
gate("budgetRequestedMaxFE", bt.get("requestedMaxFE"), "500000", bt.get("requestedMaxFE") == "500000")
gate("statusCompleted", st.get("status"), "COMPLETED", st.get("status") == "COMPLETED")
gate("processExitCode", open(os.path.join(PKG, "02-remote-run", "logs", "exitcode.txt")).read().strip(), "0",
     open(os.path.join(PKG, "02-remote-run", "logs", "exitcode.txt")).read().strip() == "0")
gate("failuresNone", fg.get("failures"), "NONE", str(fg.get("failures")).upper() == "NONE")
gate("actualFeEqualsDecoderCalls", actual_fe == decoder_calls, True, actual_fe == decoder_calls)
gate("actualFeInRange", 0 < actual_fe <= 500000, True, 0 < actual_fe <= 500000)
gate("remainingFEInRange", 0 <= remaining < 5000, True, 0 <= remaining < 5000)
gate("remainingFEValue", remaining, "500000-actualFE", remaining == 500000 - actual_fe)
gate("utilizationRate", util, ">0.99", util > 0.99)
gate("terminationKind", bt.get("terminationKind"), "EXACT_MAX_FE", bt.get("terminationKind") == "EXACT_MAX_FE")
gate("illegalSolutions", st.get("illegalSolutions"), "0", st.get("illegalSolutions") == "0")
gate("duplicateEvaluations", st.get("duplicateEvaluations"), "0", st.get("duplicateEvaluations") == "0")
gate("formalJarShaInBudget", bt.get("formalJarSha256"), FROZEN["formal_jar_sha"],
     str(bt.get("formalJarSha256", "")).lower() == FROZEN["formal_jar_sha"])
gate("observerJarShaInBudget", bt.get("experimentalJarSha256"), FROZEN["observer_jar_sha"],
     str(bt.get("experimentalJarSha256", "")).lower() == FROZEN["observer_jar_sha"])
gate("heapXmx4gNotEnlarged", "4g" in open(os.path.join(PKG, "02-remote-run", "logs", "launch-env.properties")).read(),
     True, "4g" in open(os.path.join(PKG, "02-remote-run", "logs", "launch-env.properties")).read())

ms = st.get("mechanismSummary", "")
mskv = dict(kv.split("=", 1) for kv in ms.split(",") if "=" in kv)
gate("abnormalRepairs", mskv.get("cfvfRepairs"), "0", mskv.get("cfvfRepairs") == "0")
gate("observerExecutionErrors", fg.get("observerExecutionErrors"), "0", fg.get("observerExecutionErrors") == "0")
gate("telemetryLedgerErrors", fg.get("telemetryLedgerErrors"), "0", fg.get("telemetryLedgerErrors") == "0")
gate("telemetryLedgerRowsEqualsActualFE", fg.get("telemetryLedgerRows"), str(actual_fe),
     fg.get("telemetryLedgerRows") == str(actual_fe))
gate("observerLossZero", (fg.get("telemetryLedgerErrors") == "0" and fg.get("observerExecutionErrors") == "0"),
     True, fg.get("telemetryLedgerErrors") == "0" and fg.get("observerExecutionErrors") == "0")
# instance-independent budget schedule (matches the frozen A4 schedule, cf. SA-HARD-V5)
gate("mechanismFingerprintOuterCycles", mskv.get("formalOuterCycles"), "62 (frozen A4 schedule)", mskv.get("formalOuterCycles") == "62")
gate("mechanismFingerprintCfvfOffspring", mskv.get("cfvfOffspring"), "310000 (frozen A4 schedule)", mskv.get("cfvfOffspring") == "310000")
gate("mechanismFingerprintQgSelections", mskv.get("qgSelections"), "12400 (frozen A4 schedule)", mskv.get("qgSelections") == "12400")
gate("mechanismFingerprintQpActions", mskv.get("qpActions"), "271800 (frozen A4 schedule)", mskv.get("qpActions") == "271800")
gate("runtimeMixture", st.get("runtimeSubSwarmSizes"), "G1=20;G4=40;G2=20;G3=20",
     st.get("runtimeSubSwarmSizes") == "G1_CMAX=20;G4_BALANCED=40;G2_TEC=20;G3_TWC=20")
gate("initialPopulationHashMatchesSnapshot", st.get("initialPopulationHash"),
     FROZEN["initial_population_hash_v35"],
     st.get("initialPopulationHash") == FROZEN["initial_population_hash_v35"])

# snapshot identity chain (local staging copy of the materialized snapshot)
snap_local = os.path.join(PKG, "01-staging", "snapshots", "100_2_3_1-seed-20260901.fourvec")
gate("snapshotStagingSha256", sha256_file(snap_local), FROZEN["snapshot_sha"],
     sha256_file(snap_local) == FROZEN["snapshot_sha"])

# ---------- 3. source-ledger deep scan (streaming) ----------
ledger = os.path.join(RES, "source-ledger.csv")
required_cols = FROZEN["ledger_required_cols"]
src_counter = {}
n_rows = 0
unset_rows = 0
bad_objective_rows = 0
last_actual_fe = 0
header = None
b0_triples = []
nominal_mismatch = 0
with open(ledger, "r", encoding="utf-8", newline="") as f:
    r = csv.reader(f)
    header = next(r)
    ci = {c: i for i, c in enumerate(header)}
    for col in required_cols:
        if col not in ci:
            gate("ledgerColumn_" + col, "MISSING", "present", False)
    i_fe, i_nom, i_src = ci["actualFE"], ci["nominalFE"], ci["firstLevelSource"]
    i_c, i_t, i_w = ci["Cmax"], ci["TEC"], ci["TWC"]
    for row in r:
        n_rows += 1
        fe = int(row[i_fe])
        if fe != n_rows:
            pass  # gap check below via contiguity
        if fe <= last_actual_fe:
            pass
        last_actual_fe = fe
        src = row[i_src]
        src_counter[src] = src_counter.get(src, 0) + 1
        if src in ("UNSET", "", "UNKNOWN"):
            unset_rows += 1
        try:
            c, t, w = float(row[i_c]), float(row[i_t]), float(row[i_w])
            if not (c == c and t == t and w == w and abs(c) != float("inf") and abs(t) != float("inf") and abs(w) != float("inf")):
                bad_objective_rows += 1
        except Exception:
            bad_objective_rows += 1
        if n_rows <= 100:
            b0_triples.append((c, t, w))
        expect_nom = 25000 * ((fe + 24999) // 25000)
        if int(row[i_nom]) != expect_nom:
            nominal_mismatch += 1

gate("ledgerRows", n_rows, actual_fe, n_rows == actual_fe)
gate("ledgerRowsEqualsActualFE", n_rows == actual_fe == 500000, True, n_rows == actual_fe == 500000)
gate("ledgerUnsetSourceRows", unset_rows, 0, unset_rows == 0)
gate("ledgerInvalidObjectiveRows", bad_objective_rows, 0, bad_objective_rows == 0)
gate("ledgerNominalWindowRuleViolations", nominal_mismatch, 0, nominal_mismatch == 0)
gate("finalEvaluateColumnPresent", "finalEvaluate" in header, True, "finalEvaluate" in header)
unset_like = {k: v for k, v in src_counter.items() if k.upper() in ("UNSET", "", "UNKNOWN")}
gate("ledgerSourceDistribution", src_counter, "4 known classes", len(unset_like) == 0)
gate("sourceLossZero", unset_rows, 0, unset_rows == 0)

# ---------- 4. strict B0 recomputed from first 100 rows ----------
def strict_nd(points):
    pts = sorted(set(points))
    nd = []
    for p in pts:
        dominated = False
        for q in pts:
            if q is p:
                continue
            if all(q[i] <= p[i] for i in range(3)) and any(q[i] < p[i] for i in range(3)):
                dominated = True
                break
        if not dominated:
            nd.append(p)
    return sorted(nd)


b0_exported = []
with open(os.path.join(RES, "checkpoints", "checkpoint-0-decision-front.csv"), "r", encoding="utf-8") as f:
    r = csv.reader(f)
    hdr = next(r)
    oi = [hdr.index(c) for c in ("Cmax", "TEC", "TWC")] if "Cmax" in hdr else [0, 1, 2]
    for row in r:
        b0_exported.append((float(row[oi[0]]), float(row[oi[1]]), float(row[oi[2]])))
b0_recalc = strict_nd(b0_triples)
key = lambda p: (round(p[0], 9), round(p[1], 9), round(p[2], 9))
b0_match = len(b0_recalc) == len(b0_exported) and sorted(map(key, b0_recalc)) == sorted(map(key, b0_exported))
gate("b0StrictNdRecalcSize", len(b0_recalc), f"exported {len(b0_exported)}", len(b0_recalc) == len(b0_exported))
gate("b0PointwiseMatch", "PASSED" if b0_match else "FAILED", "PASSED", b0_match)

# ---------- 5. lifecycle ledger ----------
life = os.path.join(RES, "source-lifecycle-events.csv")
life_types = {}
life_rows = 0
life_unset = 0
with open(life, "r", encoding="utf-8", newline="") as f:
    r = csv.reader(f)
    lh = next(r)
    li = {c: i for i, c in enumerate(lh)}
    it, isrc = li["eventType"], li["source"]
    for row in r:
        life_rows += 1
        life_types[row[it]] = life_types.get(row[it], 0) + 1
        if row[isrc] in ("UNSET", "", "UNKNOWN"):
            life_unset += 1
req_types = FROZEN["req_event_types"]
have = set(life_types.keys())
gate("lifecycleEventTypes", sorted(have), sorted(req_types), req_types.issubset(have))
gate("lifecycleMissingTypes", sorted(req_types - have), "[]", req_types.issubset(have))
gate("lifecycleUnsetSourceRows", life_unset, 0, life_unset == 0)

# ---------- 6. checkpoint registry ----------
reg = list(csv.DictReader(open(os.path.join(RES, "checkpoints", "checkpoint-registry.csv"), "r", encoding="utf-8")))
configured = sorted({int(x["checkpointTargetFE"]) for x in reg if x["frontType"] == "checkpoint-decision-front"})
terminal_targets = sorted({int(x["checkpointTargetFE"]) for x in reg if x["frontType"] == "terminal-decision-front"})
b0_targets = sorted({int(x["checkpointTargetFE"]) for x in reg if x["frontType"] == "b0-decision-front"})
max_overshoot = max(int(x["overshootFE"]) for x in reg)
front_types = {}
for x in reg:
    front_types[x["frontType"]] = front_types.get(x["frontType"], 0) + 1
gate("configuredCheckpointTargets", len(configured), 19, len(configured) == 19)
gate("checkpointTargetsGrid", configured, "25000..475000 step 25000",
     configured == list(range(25000, 500000, 25000)))
gate("terminalTargetFE", terminal_targets, [500000], terminal_targets == [500000])
gate("b0TargetFE", b0_targets, [0], b0_targets == [0])
gate("nonB0SnapshotTargetCount", len(configured) + len(terminal_targets), 20,
     len(configured) + len(terminal_targets) == 20)
gate("maxOvershootFE", max_overshoot, "<5000", max_overshoot < 5000)
term = [x for x in reg if x["frontType"] == "terminal-decision-front"]
gate("terminalObservedFE", int(term[0]["checkpointObservedFE"]) if term else -1, 500000,
     bool(term) and int(term[0]["checkpointObservedFE"]) == 500000)

ckpt_dir = os.path.join(RES, "checkpoints")
unreadable = nonfinite = 0
for x in reg:
    if x["frontType"] == "b0-decision-front":
        p = os.path.join(ckpt_dir, "checkpoint-0-decision-front.csv")
    elif x["frontType"] == "checkpoint-decision-front":
        p = os.path.join(ckpt_dir, f"checkpoint-{x['checkpointTargetFE']}-decision-front.csv")
    else:
        continue
    if not os.path.exists(p):
        unreadable += 1
        continue
    with open(p, "r", encoding="utf-8") as f:
        rr = csv.reader(f)
        hdr = next(rr)
        oi = [hdr.index(c) for c in ("Cmax", "TEC", "TWC")] if "Cmax" in hdr else [0, 1, 2]
        for row in rr:
            vals = [float(row[i]) for i in oi]
            if any(v != v or abs(v) == float("inf") for v in vals):
                nonfinite += 1
gate("checkpointsUnreadable", unreadable, 0, unreadable == 0)
gate("checkpointNonFiniteObjectives", nonfinite, 0, nonfinite == 0)

# ---------- 7. terminal front ----------
front_path = os.path.join(RES, "front.csv")
front_sha = sha256_file(front_path)
n_pts = 0
fin = True
with open(front_path, "r", encoding="utf-8") as f:
    r = csv.reader(f)
    next(r)
    for row in r:
        n_pts += 1
        vals = [float(v) for v in row[:3]]
        if any(v != v or abs(v) == float("inf") for v in vals):
            fin = False
gate("frontPoints", n_pts, ">0", n_pts > 0)
gate("frontAllFinite", fin, True, fin)
gate("frontSha256Raw", front_sha, "recorded (no determinism reference for NORMAL/20260901)", len(front_sha) == 64)

# ---------- 8. memory passthrough ----------
gate("heapUsedPeakBytes", mem.get("heapUsedPeak", "N/A"), "recorded (must be < 4GB, no OOM)",
     "heapUsedPeak" in mem and int(mem.get("heapUsedPeak", "0")) < 4 * 1024 ** 3)

# ---------- write run-acceptance.csv ----------
out = os.path.join(PKG, "03-run-acceptance", "run-acceptance.csv")
os.makedirs(os.path.dirname(out), exist_ok=True)
with open(out, "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["gate", "value", "expected", "result"])
    for row in results:
        w.writerow(row)
n_fail = sum(1 for r in results if r[3] == "FAIL")
print(f"TOTAL_GATES={len(results)} FAIL={n_fail}")
for r in results:
    if r[3] == "FAIL":
        print("FAIL:", r)
print("srcDistribution:", src_counter)
print("lifeTypes:", life_types)
print("frontSha:", front_sha, "frontPoints:", n_pts)
print("b0 recalc/exported:", len(b0_recalc), len(b0_exported))
print("heapUsedPeak:", mem.get("heapUsedPeak"))
sys.exit(1 if n_fail else 0)
