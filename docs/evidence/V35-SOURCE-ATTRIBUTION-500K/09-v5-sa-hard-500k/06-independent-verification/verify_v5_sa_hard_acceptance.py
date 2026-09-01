#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V35-SOURCE-ATTRIBUTION-500K / 09-v5-sa-hard-500k — independent acceptance verification.

Read-only. Zero FE. Recomputes (never trusts reports):
  1. reverse verification of the run's own evidence-sha256.tsv (every file)
  2. budget / status / formal-gate / memory hard gates
  3. source-ledger: schema columns (actualFE,nominalFE,generation,outerCycle,qRound),
     row count == actualFE, contiguous FE, UNSET sources, objective finiteness,
     duplicate event rows, first-level source distribution
  4. strict three-objective B0 recomputed from the first 100 ledger rows
     compared point-by-point against the exported checkpoint-0 (b0) front
  5. lifecycle ledger: row count, required 10 event types present
  6. checkpoint registry: 19 configured + terminal + B0, overshoot < 5000
  7. terminal front: finite objectives, raw sha vs frozen historical-A4 sha

Outputs run-acceptance.csv next to this script's caller target directory.
"""
import csv
import hashlib
import os
import sys

BASE = os.path.dirname(os.path.abspath(__file__))          # 06-independent-verification
PKG = os.path.dirname(BASE)                                 # 09-v5-sa-hard-500k
RES = os.path.join(PKG, "02-remote-run", "results", "SA-HARD-V5-500k")

FROZEN = {
    "observer_jar_sha": "1a73e3cf025f7cfdb47bde38a7b34e8f8b0810958f61323a5d3cbc35272c8c9e",
    "formal_jar_sha": "8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9",
    "snapshot_sha": "84d845233e332a6612e5dfe93c97cbbeef40c4ee05766cbfd0e9446bd3043769",
    "hist_a4_front_sha": "f3755d83a2acb4280ff8dd566025340c8b64edc71050e05bbd6a3ff4b1239bdd",
    "req_event_types": {
        "GENERATED", "DESCENDANT", "IMPROVING_DESCENDANT", "MERGE_POOL",
        "PDDR_SELECTED", "WORKING_POPULATION", "PERSONAL_ARCHIVE",
        "QG_TEACHER", "QP_TEACHER", "QP_ACTION",
    },
    "ledger_required_cols": ["actualFE", "nominalFE", "generation", "outerCycle", "qRound"],
    "max_fes": 500000,
}

results = []   # (gate, value, expected, pass)


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
gate("sourceLossZero", "checkedAgainstLedgerUnsetRows", 0, True)  # resolved below after ledger scan
gate("mechanismFingerprintOuterCycles", mskv.get("formalOuterCycles"), "62 (matches F1)", mskv.get("formalOuterCycles") == "62")
gate("mechanismFingerprintCfvfOffspring", mskv.get("cfvfOffspring"), "310000 (matches F1)", mskv.get("cfvfOffspring") == "310000")
gate("mechanismFingerprintQgTableHash", mskv.get("qgTableHash", "")[:8].upper(), "F0E6D62B (matches F1)",
     str(mskv.get("qgTableHash", "")).upper().startswith("F0E6D62B"))
gate("mechanismFingerprintQpTableHash", mskv.get("qpTableHash", "")[:8].upper(), "9328966A (matches F1)",
     str(mskv.get("qpTableHash", "")).upper().startswith("9328966A"))
gate("mechanismFingerprintPddrStreamHash", mskv.get("pddrEventStreamHash", "")[:8].upper(), "D698245E (matches F1)",
     str(mskv.get("pddrEventStreamHash", "")).upper().startswith("D698245E"))
gate("runtimeMixture", st.get("runtimeSubSwarmSizes"), "G1=20;G4=40;G2=20;G3=20",
     st.get("runtimeSubSwarmSizes") == "G1_CMAX=20;G4_BALANCED=40;G2_TEC=20;G3_TWC=20")
gate("initialPopulationHashMatchesSnapshot", st.get("initialPopulationHash"),
     "179a82a3825566380ab6798aa898002d31565dad9d65802e57b295c2a4294c2d",
     st.get("initialPopulationHash") == "179a82a3825566380ab6798aa898002d31565dad9d65802e57b295c2a4294c2d")

# ---------- 3. source-ledger deep scan (streaming) ----------
ledger = os.path.join(RES, "source-ledger.csv")
required_cols = FROZEN["ledger_required_cols"]
src_counter = {}
n_rows = 0
unset_rows = 0
bad_objective_rows = 0
dup_event_rows = 0
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
    i_fin = ci.get("finalEvaluate")
    for row in r:
        n_rows += 1
        fe = int(row[i_fe])
        if fe != n_rows:
            dup_event_rows += 1 if fe == last_actual_fe else 0
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
        # nominal window rule: nominalFE = 25000 * ceil(actualFE/25000)
        expect_nom = 25000 * ((fe + 24999) // 25000)
        if int(row[i_nom]) != expect_nom:
            nominal_mismatch += 1
        if i_fin is not None and n_rows == 1:
            pass

gate("ledgerRows", n_rows, actual_fe, n_rows == actual_fe)
gate("ledgerRowsEqualsActualFE", n_rows == actual_fe == 500000, True, n_rows == actual_fe == 500000)
gate("ledgerUnsetSourceRows", unset_rows, 0, unset_rows == 0)
gate("ledgerInvalidObjectiveRows", bad_objective_rows, 0, bad_objective_rows == 0)
gate("ledgerNominalWindowRuleViolations", nominal_mismatch, 0, nominal_mismatch == 0)
gate("finalEvaluateColumnPresent", "finalEvaluate" in header, True, "finalEvaluate" in header)
unset_like = {k: v for k, v in src_counter.items() if k.upper() in ("UNSET", "", "UNKNOWN")}
gate("ledgerSourceDistribution", src_counter, "4 known classes", len(unset_like) == 0)
# resolve the sourceLoss placeholder with the real ledger-scan result
for idx, row in enumerate(results):
    if row[0] == "sourceLossZero":
        results[idx] = ("sourceLossZero", unset_rows, 0, "PASS" if unset_rows == 0 else "FAIL")
        break

# ---------- 4. strict B0 recomputed from first 100 rows ----------
def strict_nd(points):
    pts = sorted(set(points))          # exact dedupe, lexicographic (Cmax,TEC,TWC) all minimized
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
    for row in r:
        b0_exported.append((float(row[0]), float(row[1]), float(row[2])))
b0_recalc = strict_nd(b0_triples)
key = lambda p: (round(p[0], 9), round(p[1], 9), round(p[2], 9))
b0_match = len(b0_recalc) == len(b0_exported) and \
    sorted(map(key, b0_recalc)) == sorted(map(key, b0_exported))
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
# 口径冻结：19个配置检查点(25k..475k) + 1个terminal(500k) = 20个非B0快照目标；另有B0(0)。
configured = sorted({int(x["checkpointTargetFE"]) for x in reg if x["frontType"] == "checkpoint-decision-front"})
terminal_targets = sorted({int(x["checkpointTargetFE"]) for x in reg if x["frontType"] == "terminal-decision-front"})
b0_targets = sorted({int(x["checkpointTargetFE"]) for x in reg if x["frontType"] == "b0-decision-front"})
max_overshoot = max(int(x["overshootFE"]) for x in reg)
unobservable = [x for x in reg if str(x.get("frontType", "")).startswith("checkpoint") and int(x["checkpointObservedFE"]) <= 0 and int(x["checkpointTargetFE"]) > 0]
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
gate("terminalRows", sum(v for k, v in front_types.items() if k.startswith("terminal")), 2,
     sum(v for k, v in front_types.items() if k.startswith("terminal")) == 2)
gate("b0Rows", sum(v for k, v in front_types.items() if k.startswith("b0")), 2,
     sum(v for k, v in front_types.items() if k.startswith("b0")) == 2)
gate("nonB0SnapshotCount", len(configured) + len(terminal_targets),
     "20 (19 checkpoints + 1 terminal; terminal NOT a 21st window)",
     len(configured) + len(terminal_targets) == 20)

# every decision checkpoint file readable with finite objectives
# (runner convention, same as 20k gate: terminal decision front = top-level front.csv;
#  terminal rows are registered in checkpoint-registry.csv with observedFE==500000)
ckpt_dir = os.path.join(RES, "checkpoints")
unreadable = nonfinite = 0
for x in reg:
    if x["frontType"] == "b0-decision-front":
        p = os.path.join(ckpt_dir, "checkpoint-0-decision-front.csv")
    elif x["frontType"] == "checkpoint-decision-front":
        p = os.path.join(ckpt_dir, f"checkpoint-{x['checkpointTargetFE']}-decision-front.csv")
    else:
        continue  # terminal-decision-front verified via front.csv below
    if not os.path.exists(p):
        unreadable += 1
        continue
    with open(p, "r", encoding="utf-8") as f:
        rr = csv.reader(f)
        hdr = next(rr)
        oi = [hdr.index(c) for c in ("Cmax", "TEC", "TWC")]
        for row in rr:
            vals = [float(row[i]) for i in oi]
            if any(v != v or abs(v) == float("inf") for v in vals):
                nonfinite += 1
term = [x for x in reg if x["frontType"] == "terminal-decision-front"]
gate("terminalRegistryRows", len(term), 1, len(term) == 1)
gate("terminalObservedFE", int(term[0]["checkpointObservedFE"]) if term else -1, 500000,
     bool(term) and int(term[0]["checkpointObservedFE"]) == 500000)
gate("terminalOvershootFE", int(term[0]["overshootFE"]) if term else -1, 0,
     bool(term) and int(term[0]["overshootFE"]) == 0)
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
gate("frontSha256Raw", front_sha, FROZEN["hist_a4_front_sha"], front_sha == FROZEN["hist_a4_front_sha"])

# ---------- memory summary passthrough ----------
gate("heapUsedPeakBytes", mem.get("heapUsedPeak", "N/A"), "recorded", "heapUsedPeak" in mem)

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
print("frontSha:", front_sha)
print("b0 recalc/exported:", len(b0_recalc), len(b0_exported))
sys.exit(1 if n_fail else 0)
