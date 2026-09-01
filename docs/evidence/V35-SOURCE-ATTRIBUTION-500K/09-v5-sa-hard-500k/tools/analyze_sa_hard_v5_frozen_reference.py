#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V35-SOURCE-ATTRIBUTION-500K / SA-HARD 500k — HV/IGD under the FROZEN Reference Contract.

Calibre declaration (do not change without a new user decision)
--------------------------------------------------------------
* PRIMARY judgement pipeline (byte-for-byte the same calibre that produced the
  frozen historical-A2 anchors):

      raw finite front -> normalize by the FROZEN PFref -> hypervolume / igd

  No non-dominated filtering in the primary path.

* HV/IGD computed AFTER strict non-dominated filtering is reported as an
  ADDITIONAL DIAGNOSTIC ONLY and never feeds the reproduction verdict.

* All metric implementations are IMPORTED from
  ``V35-PFC5-PHASE0/tools/build_reference_contract.py`` (embedded exact copies of
  ``analyze_confirmation.py``, EPS=1e-12).  Nothing is re-implemented here.
  PFref / ideal / nadir / normalization / HV / IGD are BOUND, NEVER REBUILT.

* Gold self-check runs FIRST: if the frozen historical-A2 anchors cannot be
  reproduced to 1e-12, the script stops with REFERENCE_INVALID and never
  computes or interprets any SA-HARD number.

* Failure gate (frozen, post-hoc modification forbidden):
      deltaHV  = (HV_saHard  - HV_histA2)  / HV_histA2
      deltaIGD = (IGD_histA2 - IGD_saHard) / IGD_histA2      (positive = saHard better)
      REPRODUCED  <=>  deltaHV < -0.05  AND  deltaIGD < -0.20

Zero FE. No algorithm / Jar / observer / PDDR / CFVF / CA-TA code touched.
"""
import ast
import csv
import hashlib
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SA = os.path.dirname(HERE)                       # .../07-sa-hard-500k
EVID = os.path.dirname(SA)                       # .../V35-SOURCE-ATTRIBUTION-500K
PHASE0 = os.path.join(EVID, "..", "V35-PFC5-PHASE0")
PHASE0 = os.path.normpath(PHASE0)
PHASE0_TOOLS = os.path.join(PHASE0, "tools")
CONTRACT_DIR = os.path.join(PHASE0, "04-reference-contract")

sys.path.insert(0, PHASE0_TOOLS)
import build_reference_contract as B  # noqa: E402  (exact copy reuse, never modified)

CONTRACT = os.path.join(CONTRACT_DIR, "reference-contract.properties")
PFREF = os.path.join(CONTRACT_DIR, "pfref-100_5_3_1.csv")
HIST_A2_FRONT = os.path.join(PHASE0, "fetched-remote", "100_5_3_1", "seed-20260901", "A2", "front.csv")
HIST_A4_FRONT = os.path.join(PHASE0, "fetched-remote", "100_5_3_1", "seed-20260901", "A4", "front.csv")

OUT_DIR = os.path.join(SA, "04-failure-reproduction", "frozen-reference-analysis")

# frozen constants (from reference-contract.properties + F1 decision)
CONTRACT_SHA = "ecdc5589ab4d36a028a0d53e9fcdbfc40ee1e04864df929c2e5c035b4481235f"
PFREF_SHA = "4dc85dd4fa3c7824ed2bf302b648355df796be7f15375db84047d23c4de683da"
GOLD_HV = 0.810244195451609          # HV_histA2  (frozen anchor)
GOLD_IGD = 0.057804242003353316      # IGD_histA2 (frozen anchor)
HIST_A2_FRONT_SHA = "75d8a44a71428274a591a1c6413ddac0cb7e7deb421da419a11b2d3196a204aa"
HIST_A4_FRONT_SHA = "f3755d83a2acb4280ff8dd566025340c8b64edc71050e05bbd6a3ff4b1239bdd"
ABS_TOL = 1e-12
REL_TOL = 1e-12
HV_GATE = -0.05
IGD_GATE = -0.20


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def load_contract():
    actual = sha256_file(CONTRACT)
    if actual != CONTRACT_SHA:
        return None, "contract sha256 mismatch expected=%s actual=%s" % (CONTRACT_SHA, actual)
    props = {}
    with open(CONTRACT, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            props[k] = v
    return props, None


def describe_front(raw):
    return {
        "rawFrontSize": len(raw),
        "finiteFrontSize": sum(1 for p in raw if all(v == v and abs(v) != float("inf") for v in p)),
        "exactDedupSize": len(set(raw)),
        "strictNdSize": len(B.nondominated(raw)),
        "canonicalFrontSha256": B.canonical_hash(raw),
    }


def write_csv(path, fieldnames, rows):
    with open(path, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames, lineterminator="\n")
        w.writeheader()
        for row in rows:
            w.writerow(row)


def emit(verdict, notes):
    print("verdict=%s" % verdict)
    for n in notes:
        print("note=%s" % n)


def main():
    front_path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(SA, "02-remote-run", "results", "SA-HARD-V5-500k", "front.csv")
    os.makedirs(OUT_DIR, exist_ok=True)
    note = []

    # --- 1. contract ------------------------------------------------------
    props, err = load_contract()
    if props is None:
        emit("REFERENCE_INVALID", ["contract: %s" % err])
        return 3
    ideal = ast.literal_eval(props["ideal"])
    nadir = ast.literal_eval(props["nadir"])
    print("contractSha256=%s" % CONTRACT_SHA)
    print("ideal=%s" % repr(ideal))
    print("nadir=%s" % repr(nadir))

    # --- 2. PFref ---------------------------------------------------------
    pfref = B.read_front(PFREF)
    pfref_hash = B.canonical_hash(pfref)
    if pfref_hash != PFREF_SHA:
        emit("REFERENCE_INVALID", ["pfref canonical sha256 mismatch: expected=%s actual=%s" % (PFREF_SHA, pfref_hash)])
        return 3
    print("pfrefPoints=%d" % len(pfref))
    print("pfrefCanonicalSha256=%s" % pfref_hash)

    ref_norm, lows, highs = B.normalize(pfref, pfref)
    if [float(v) for v in lows] != [float(v) for v in ideal] or [float(v) for v in highs] != [float(v) for v in nadir]:
        emit("REFERENCE_INVALID", ["ideal/nadir recomputed from PFref do not match the frozen contract values"])
        return 3

    # --- 3. GOLD SELF-CHECK (must pass before touching any SA-HARD number) -
    a2_sha = sha256_file(HIST_A2_FRONT)
    if a2_sha != HIST_A2_FRONT_SHA:
        emit("REFERENCE_INVALID", ["historical A2 front sha256 mismatch: expected=%s actual=%s" % (HIST_A2_FRONT_SHA, a2_sha)])
        return 3
    a2_raw = B.read_front(HIST_A2_FRONT)
    a2_norm, _, _ = B.normalize(a2_raw, pfref)
    gold_hv = B.hypervolume(a2_norm)
    gold_igd = B.igd(a2_norm, ref_norm)
    abs_hv = abs(gold_hv - GOLD_HV)
    rel_hv = abs_hv / max(abs(GOLD_HV), 1e-300)
    abs_igd = abs(gold_igd - GOLD_IGD)
    rel_igd = abs_igd / max(abs(GOLD_IGD), 1e-300)
    gold_ok = (abs_hv <= ABS_TOL and rel_hv <= REL_TOL and abs_igd <= ABS_TOL and rel_igd <= REL_TOL)
    print("goldHvRecomputed=%r" % gold_hv)
    print("goldIgdRecomputed=%r" % gold_igd)
    print("goldHvAbsDiff=%r" % abs_hv)
    print("goldIgdAbsDiff=%r" % abs_igd)
    print("goldSelfCheck=%s" % ("PASS" if gold_ok else "FAIL"))
    if not gold_ok:
        emit("REFERENCE_INVALID", ["gold self-check failed; refusing to compute or interpret SA-HARD metrics"])
        return 3

    # --- 4. SA-HARD front --------------------------------------------------
    if not os.path.isfile(front_path):
        emit("RUN_INVALID", ["SA-HARD front missing: %s" % front_path])
        return 4
    try:
        raw = B.read_front(front_path)
    except (ValueError, OSError) as exc:
        emit("RUN_INVALID", ["SA-HARD front unreadable/non-finite: %s" % exc])
        return 4
    if not raw:
        emit("RUN_INVALID", ["SA-HARD front empty"])
        return 4

    desc = describe_front(raw)
    for k in ("rawFrontSize", "finiteFrontSize", "exactDedupSize", "strictNdSize", "canonicalFrontSha256"):
        print("%s=%s" % (k, desc[k]))

    # primary pipeline (same calibre as the gold anchors)
    norm, _, _ = B.normalize(raw, pfref)
    hv = B.hypervolume(norm)
    igd = B.igd(norm, ref_norm)

    # additional diagnostic only
    nd = B.nondominated(raw)
    nd_norm, _, _ = B.normalize(nd, pfref)
    hv_nd = B.hypervolume(nd_norm)
    igd_nd = B.igd(nd_norm, ref_norm)

    min_cmax = min(p[0] for p in raw)
    min_tec = min(p[1] for p in raw)
    min_twc = min(p[2] for p in raw)

    delta_hv = (hv - GOLD_HV) / GOLD_HV
    delta_igd = (GOLD_IGD - igd) / GOLD_IGD
    delta_hv_nd = (hv_nd - GOLD_HV) / GOLD_HV
    delta_igd_nd = (GOLD_IGD - igd_nd) / GOLD_IGD

    triggered = (delta_hv < HV_GATE) and (delta_igd < IGD_GATE)
    verdict = "SA_HARD_FAILURE_CLASS_REPRODUCED" if triggered else "SOURCE_ATTRIBUTION_CASE_NOT_REPRODUCED"

    front_sha = sha256_file(front_path)
    hist_a4_sha = sha256_file(HIST_A4_FRONT) if os.path.isfile(HIST_A4_FRONT) else ""
    deterministic = bool(hist_a4_sha) and (front_sha == HIST_A4_FRONT_SHA)
    # canonical (order-independent) sorted-front comparison against historical A4
    canonical_match = ""
    if os.path.isfile(HIST_A4_FRONT):
        hist_a4_raw = B.read_front(HIST_A4_FRONT)
        canonical_match = str(B.canonical_hash(raw) == B.canonical_hash(hist_a4_raw)).lower()
    note.append("cmaxRole=NOT_A_HARD_GATE; reported for mechanism interpretation only")

    print("frontSourcePath=%s" % os.path.relpath(front_path, EVID).replace(os.sep, "/"))
    print("frontSha256Raw=%s" % front_sha)
    print("minCmax=%r" % min_cmax)
    print("minTEC=%r" % min_tec)
    print("minTWC=%r" % min_twc)
    print("hv=%r" % hv)
    print("igd=%r" % igd)
    print("deltaHV=%r" % delta_hv)
    print("deltaIGD=%r" % delta_igd)
    print("hvAfterStrictNd=%r" % hv_nd)
    print("igdAfterStrictNd=%r" % igd_nd)
    print("deltaHVAfterStrictNd=%r" % delta_hv_nd)
    print("deltaIGDAfterStrictNd=%r" % delta_igd_nd)
    print("failureGateHv=%r" % HV_GATE)
    print("failureGateIgd=%r" % IGD_GATE)
    print("failureGateTriggered=%s" % str(triggered).lower())
    print("determinismFrontMatchesHistoricalA4=%s" % str(deterministic).lower())
    print("canonicalFrontMatchesHistoricalA4=%s" % canonical_match)
    emit(verdict, note)

    # --- 5. outputs --------------------------------------------------------
    with open(os.path.join(OUT_DIR, "normalized-front.csv"), "w", encoding="utf-8", newline="") as f:
        f.write("Cmax,TEC,TWC\n")
        for point in sorted(set(norm)):
            f.write(",".join("%.17g" % v for v in point) + "\n")

    write_csv(
        os.path.join(OUT_DIR, "sa-hard-metrics.csv"),
        ["metric", "value"],
        [
            {"metric": "frontSourcePath", "value": os.path.relpath(front_path, EVID).replace(os.sep, "/")},
            {"metric": "frontSha256Raw", "value": front_sha},
            {"metric": "rawFrontSize", "value": desc["rawFrontSize"]},
            {"metric": "finiteFrontSize", "value": desc["finiteFrontSize"]},
            {"metric": "exactDedupSize", "value": desc["exactDedupSize"]},
            {"metric": "strictNdSize", "value": desc["strictNdSize"]},
            {"metric": "canonicalFrontSha256", "value": desc["canonicalFrontSha256"]},
            {"metric": "pipeline", "value": "rawFiniteFront -> normalize(frozen PFref) -> HV/IGD (same calibre as gold anchors)"},
            {"metric": "hv", "value": repr(hv)},
            {"metric": "igd", "value": repr(igd)},
            {"metric": "deltaHV", "value": repr(delta_hv)},
            {"metric": "deltaIGD", "value": repr(delta_igd)},
            {"metric": "minCmax", "value": repr(min_cmax)},
            {"metric": "minTEC", "value": repr(min_tec)},
            {"metric": "minTWC", "value": repr(min_twc)},
            {"metric": "hvAfterStrictNd", "value": repr(hv_nd)},
            {"metric": "igdAfterStrictNd", "value": repr(igd_nd)},
            {"metric": "deltaHVAfterStrictNd", "value": repr(delta_hv_nd)},
            {"metric": "deltaIGDAfterStrictNd", "value": repr(delta_igd_nd)},
            {"metric": "diagnosticOnly", "value": "hvAfterStrictNd/igdAfterStrictNd/delta*AfterStrictNd do NOT feed the verdict"},
            {"metric": "failureGateHv", "value": repr(HV_GATE)},
            {"metric": "failureGateIgd", "value": repr(IGD_GATE)},
            {"metric": "failureGateTriggered", "value": str(triggered).lower()},
            {"metric": "determinismFrontMatchesHistoricalA4", "value": str(deterministic).lower()},
            {"metric": "canonicalFrontMatchesHistoricalA4", "value": canonical_match},
            {"metric": "cmaxRole", "value": "NOT_A_HARD_GATE"},
            {"metric": "verdict", "value": verdict},
        ],
    )

    write_csv(
        os.path.join(SA, "failure-class-reproduction.csv"),
        ["role", "source", "frontSha256", "points", "hv", "igd", "deltaHV", "deltaIGD"],
        [
            {"role": "historicalA2", "source": "baseline (frozen anchor)",
             "frontSha256": HIST_A2_FRONT_SHA, "points": len(a2_raw),
             "hv": repr(GOLD_HV), "igd": repr(GOLD_IGD), "deltaHV": "0.0", "deltaIGD": "0.0"},
            {"role": "historicalA2Recomputed", "source": "gold self-check by this script",
             "frontSha256": a2_sha, "points": len(a2_raw),
             "hv": repr(gold_hv), "igd": repr(gold_igd),
             "deltaHV": repr((gold_hv - GOLD_HV) / GOLD_HV), "deltaIGD": repr((GOLD_IGD - gold_igd) / GOLD_IGD)},
            {"role": "saHardA4_500k_observerON", "source": os.path.relpath(front_path, EVID).replace(os.sep, "/"),
             "frontSha256": front_sha, "points": len(raw),
             "hv": repr(hv), "igd": repr(igd), "deltaHV": repr(delta_hv), "deltaIGD": repr(delta_igd)},
            {"role": "historicalA4", "source": "NOT A BASELINE - determinism reference only",
             "frontSha256": HIST_A4_FRONT_SHA,
             "points": len(B.read_front(HIST_A4_FRONT)) if os.path.isfile(HIST_A4_FRONT) else "",
             "hv": "", "igd": "", "deltaHV": "", "deltaIGD": ""},
        ],
    )

    with open(os.path.join(OUT_DIR, "frozen-reference-analysis.properties"), "w", encoding="utf-8") as f:
        f.write("referenceContractSha256=%s\n" % CONTRACT_SHA)
        f.write("pfrefCanonicalSha256=%s\n" % PFREF_SHA)
        f.write("pfrefPoints=%d\n" % len(pfref))
        f.write("goldSelfCheck=PASS\n")
        f.write("goldHvRecomputed=%r\n" % gold_hv)
        f.write("goldIgdRecomputed=%r\n" % gold_igd)
        f.write("goldHvAbsDiff=%r\n" % abs_hv)
        f.write("goldIgdAbsDiff=%r\n" % abs_igd)
        f.write("pipeline=rawFiniteFront -> normalize(frozen PFref) -> HV/IGD\n")
        f.write("rawFrontSize=%d\n" % desc["rawFrontSize"])
        f.write("finiteFrontSize=%d\n" % desc["finiteFrontSize"])
        f.write("exactDedupSize=%d\n" % desc["exactDedupSize"])
        f.write("strictNdSize=%d\n" % desc["strictNdSize"])
        f.write("canonicalFrontSha256=%s\n" % desc["canonicalFrontSha256"])
        f.write("frontSha256Raw=%s\n" % front_sha)
        f.write("hv=%r\n" % hv)
        f.write("igd=%r\n" % igd)
        f.write("deltaHV=%r\n" % delta_hv)
        f.write("deltaIGD=%r\n" % delta_igd)
        f.write("minCmax=%r\n" % min_cmax)
        f.write("minTEC=%r\n" % min_tec)
        f.write("minTWC=%r\n" % min_twc)
        f.write("hvAfterStrictNd=%r\n" % hv_nd)
        f.write("igdAfterStrictNd=%r\n" % igd_nd)
        f.write("failureGateHv=%r\n" % HV_GATE)
        f.write("failureGateIgd=%r\n" % IGD_GATE)
        f.write("failureGateTriggered=%s\n" % str(triggered).lower())
        f.write("determinismFrontMatchesHistoricalA4=%s\n" % str(deterministic).lower())
        f.write("canonicalFrontMatchesHistoricalA4=%s\n" % canonical_match)
        f.write("cmaxRole=NOT_A_HARD_GATE\n")
        f.write("verdict=%s\n" % verdict)
        f.write("consumedFE=0\n")
        f.write("changedAlgorithm=false\n")

    return 0


if __name__ == "__main__":
    sys.exit(main())
