# -*- coding: utf-8 -*-
"""MAIN-AGENT INDEPENDENT VERIFICATION (PhaseA0-CORRECTION-V1 §九).

Independence rules:
  1. All EXPECTED values are literals from the test contract (hand-derived),
     written below as EXPECTED_* constants.  They are NOT derived by calling
     the implementation under test.
  2. The implementation (threshold_recompute.window_metrics /
     estimate_500k_peak) is imported and COMPARED against those literals.
  3. The HV numbers in T3/T4 expectations are hand-derived bounds: the exact
     HV values are implementation-tied (fc6 normalization), so the contract
     fixes SIGN/ORDERING/COUNTING properties (T1/T2 zero-ness, T3/T4
     positivity/ordering, exclusive counts) plus exact producerSet counts,
     which are semantics-level and implementation-independent.
  4. The manifest is re-verified independently.

Exit 0 iff every check passes.
"""
import csv, hashlib, io, json, os, random, sys

HERE = os.path.dirname(os.path.abspath(__file__))
PREREG = os.path.join(os.path.dirname(HERE), "00-preregistration")
sys.path.insert(0, PREREG)
import threshold_recompute as tr  # implementation under test

failures = []


def check(name, ok, detail=""):
    print(("PASS" if ok else "FAIL"), name, detail)
    if not ok:
        failures.append(name)


# ---- contract literals (hand-derived, NOT from the implementation) ----------
G, C, I = "GLOBAL_CFVF", "CATA", "INHERITED_LS"
FPAST = [(100.0, 500.0, 1000.0), (120.0, 450.0, 1100.0), (140.0, 500.0, 900.0)]
P_SHARED = (90.0, 480.0, 950.0)
P_EXCL_G = (85.0, 470.0, 940.0)   # dominates P_SHARED in union → only strict-ND new point
P_CATA = (110.0, 400.0, 1050.0)   # dominates (120,450,1100) in union
P_LS = (95.0, 490.0, 980.0)
EPS = 1e-12


def ev(src, cid, obj, fe=26001):
    return {"source": src, "nominalFE": fe, "actualFE": fe,
            "candidateId": cid, "objectives": obj}


# T1: GLOBAL and CATA generate the identical triple p.
#     producerSet(p) = {GLOBAL_CFVF, CATA};
#     removing either source leaves the other's copy → WHVG both 0;
#     p not exclusive to anyone → ExclusiveND both 0.
EXPECTED_T1 = {"producerSet": {G, C}, "whvgZero": {G, C}, "exclZero": {G, C}}

# T2: two points within 1e-12 (5e-13 apart) from different sources fold to ONE
#     canonical triple with the same producerSet → identical to T1.
EXPECTED_T2 = EXPECTED_T1

# T3: only GLOBAL generates a new strict-ND point.
#     WHVG_GLOBAL > 0; ExclusiveND_GLOBAL = 1; nndAll = 1; CATA all zero.
EXPECTED_T3 = {"whvgPositive": {G}, "excl": {G: 1, C: 0}, "nndAll": 1}

# T4: shared point (G+C) + GLOBAL-exclusive point.
#     shared point in NO ExclusiveND; GLOBAL gets exactly 1; CATA 0;
#     WHVG_CATA = 0 (its only event is the shared point);
#     WHVG_GLOBAL > 0 (its exclusive point is strict-ND new).
EXPECTED_T4 = {"excl": {G: 1, C: 0}, "whvgCataZero": True, "whvgGlobalPositive": True}

# T5: 20 random shuffles of a 3-source event set → producerSet, per-source
#     WHVG/ExclusiveND and nndAll all invariant.
EXPECTED_T5_SHUFFLES = 20

# T6: all triples single-source → counterfactual equals old semantics.
#     Hand-derived: window {P_SHARED, P_EXCL_G} from GLOBAL only; P_EXCL_G
#     dominates P_SHARED in the union → nndAll=1, ExclusiveND_GLOBAL=1;
#     WHVG_GLOBAL = HV(ND(Fpast∪W)) - HV(ND(Fpast)) > 0.
EXPECTED_T6 = {"excl": {G: 1}, "nndAll": 1, "whvgGlobalPositive": True}

# T8: memory formula contract (hand-derived unit cases).
B, GB = 2 * 1024 ** 3, 1024 ** 3
EXPECTED_T8 = {
    "baseline_only": B + max(0.20 * B, 256 * 1024 * 1024),   # = B + 0.2B (0.2B=400MiB>256MiB)
    "not_x25": 25 * B,
    "monotonic_caps": [0, 10 * 1024 * 1024, 50 * 1024 * 1024, 200 * 1024 * 1024],
    "gate_below_pass": 0.5999 * 10 * GB,
    "gate_equal_fail": 0.60 * 10 * GB,
    "gate_above_fail": 0.6001 * 10 * GB,
    "gate_heap": 10 * GB,
}


def run():
    # ---- T1 ----
    m = tr.window_metrics(FPAST, [ev(G, "a", P_SHARED), ev(C, "b", P_SHARED)])
    ps = m["producerSets"].get(P_SHARED)
    check("T1 producerSet == {GLOBAL_CFVF, CATA}", ps is not None and set(ps) == EXPECTED_T1["producerSet"], str(ps))
    check("T1 WHVG_GLOBAL == 0", m["perSource"][G]["whvg"] == 0.0, repr(m["perSource"][G]["whvg"]))
    check("T1 WHVG_CATA == 0", m["perSource"][C]["whvg"] == 0.0, repr(m["perSource"][C]["whvg"]))
    check("T1 ExclusiveND both == 0",
          m["perSource"][G]["nexclND"] == 0 and m["perSource"][C]["nexclND"] == 0)

    # ---- T2 ----
    m = tr.window_metrics(FPAST, [ev(G, "a", P_SHARED),
                                  ev(C, "b", tuple(v + 5e-13 for v in P_SHARED))])
    check("T2 epsilon-equal folds to 1 canonical triple", len(m["producerSets"]) == 1,
          str(len(m["producerSets"])))
    ps = list(m["producerSets"].values())[0]
    check("T2 producerSet == {GLOBAL_CFVF, CATA}", set(ps) == EXPECTED_T2["producerSet"], str(ps))
    check("T2 WHVG both == 0",
          m["perSource"][G]["whvg"] == 0.0 and m["perSource"][C]["whvg"] == 0.0)

    # ---- T3 ----
    m = tr.window_metrics(FPAST, [ev(G, "a", P_EXCL_G)], sources=[G, C])
    check("T3 WHVG_GLOBAL > 0", m["perSource"][G]["whvg"] > 0.0,
          repr(m["perSource"][G]["whvg"]))
    check("T3 ExclusiveND_GLOBAL == 1 and nndAll == 1",
          m["perSource"][G]["nexclND"] == EXPECTED_T3["excl"][G]
          and m["nndAll"] == EXPECTED_T3["nndAll"])
    check("T3 CATA all zero", m["perSource"][C]["whvg"] == 0.0
          and m["perSource"][C]["nexclND"] == 0)

    # ---- T4 ----
    m = tr.window_metrics(FPAST, [ev(G, "a", P_SHARED), ev(C, "b", P_SHARED),
                                  ev(G, "c", P_EXCL_G)], sources=[G, C])
    check("T4 ExclusiveND_GLOBAL == 1 (exclusive point only)",
          m["perSource"][G]["nexclND"] == EXPECTED_T4["excl"][G])
    check("T4 ExclusiveND_CATA == 0 (shared point excluded)",
          m["perSource"][C]["nexclND"] == EXPECTED_T4["excl"][C])
    check("T4 WHVG_CATA == 0 (no虚假贡献 from shared point)",
          m["perSource"][C]["whvg"] == 0.0)
    check("T4 WHVG_GLOBAL > 0 (exclusive point counts)",
          m["perSource"][G]["whvg"] > 0.0)

    # ---- T5 ----
    events = [ev(G, "a", P_SHARED, 26010), ev(C, "b", P_SHARED, 26020),
              ev(G, "c", P_EXCL_G, 26030), ev(C, "d", P_CATA, 26040),
              ev(I, "e", P_LS, 26050)]
    base = tr.window_metrics(FPAST, events)
    rng = random.Random(20260901)
    stable = True
    for i in range(EXPECTED_T5_SHUFFLES):
        s = list(events)
        rng.shuffle(s)
        m = tr.window_metrics(FPAST, s)
        if m["producerSets"] != base["producerSets"]:
            stable = False
            break
        if m["nndAll"] != base["nndAll"] or abs(m["hvAll"] - base["hvAll"]) > 1e-12:
            stable = False
            break
        for src in base["perSource"]:
            for k in ("whvg", "whvgSharePct", "nexclND", "exclusiveNdSharePct"):
                if abs(m["perSource"][src][k] - base["perSource"][src][k]) > 1e-12:
                    stable = False
                    break
    check("T5 20 random shuffles: producerSet/Wt/Wt^-s/WHVG/ExclusiveND invariant",
          stable)

    # ---- T6 ----
    m = tr.window_metrics(FPAST, [ev(G, "a", P_SHARED), ev(G, "c", P_EXCL_G)])
    check("T6 nndAll == 1 (P_EXCL_G dominates P_SHARED in union)",
          m["nndAll"] == EXPECTED_T6["nndAll"], str(m["nndAll"]))
    check("T6 ExclusiveND_GLOBAL == 1", m["perSource"][G]["nexclND"] == 1)
    check("T6 WHVG_GLOBAL > 0", m["perSource"][G]["whvg"] > 0.0)

    # ---- T8 ----
    e = tr.estimate_500k_peak(B, 0, 0, 0)
    check("T8 baseline-only == B + max(0.2B, 256MiB) (NOT x25)",
          abs(e - EXPECTED_T8["baseline_only"]) < 1e-6, "%r vs %r" % (e, EXPECTED_T8["baseline_only"]))
    check("T8 estimate far below deprecated x25 value",
          e < EXPECTED_T8["not_x25"], "%r vs %r" % (e, EXPECTED_T8["not_x25"]))
    prev = None
    mono = True
    for cap in EXPECTED_T8["monotonic_caps"]:
        e = tr.estimate_500k_peak(B, 0, cap, cap)
        if prev is not None and e < prev - 1e-6:
            mono = False
        prev = e
    check("T8 monotonic in buffer caps", mono)
    import inspect
    sig = inspect.signature(tr.estimate_500k_peak).parameters
    check("T8 no disk/ledger parameter in heap estimator",
          not any(("disk" in k or "ledger" in k) for k in sig))
    gp = tr.memory_gate_passes(EXPECTED_T8["gate_below_pass"], EXPECTED_T8["gate_heap"])
    ge = tr.memory_gate_passes(EXPECTED_T8["gate_equal_fail"], EXPECTED_T8["gate_heap"])
    ga = tr.memory_gate_passes(EXPECTED_T8["gate_above_fail"], EXPECTED_T8["gate_heap"])
    check("T8 gate: below 0.60 PASS", gp)
    check("T8 gate: == 0.60 FAIL-CLOSED", ge is False)
    check("T8 gate: above 0.60 FAIL-CLOSED", ga is False)

    # ---- manifest independent re-verification ----
    mf = os.path.join(PREREG, "evidence-sha256.tsv")
    lines = io.open(mf, encoding="utf-8").read().splitlines()[1:]
    missing = mismatch = 0
    for ln in lines:
        p, h = ln.split("\t")
        fp = os.path.join(PREREG, p)
        if not os.path.exists(fp):
            missing += 1
        elif hashlib.sha256(open(fp, "rb").read()).hexdigest() != h:
            mismatch += 1
    check("manifest reverse verification (0 missing, 0 mismatch)",
          missing == 0 and mismatch == 0,
          "entries=%d missing=%d mismatch=%d" % (len(lines), missing, mismatch))

    # ---- cross-manifest binding (evidence repack, 2026-09-01) ----
    # The 00-preregistration manifest must bind this directory's manifest and
    # this verification script by path+sha256; both must close here too.
    ext_rows = [ln for ln in lines if ln.split("	")[0].startswith("../06-independent-verification/")]
    check("cross-binding rows present in 00-prereg manifest", len(ext_rows) == 2,
          "%d rows" % len(ext_rows))
    own_manifest = os.path.join(PREREG, "..", "06-independent-verification",
                                "evidence-sha256.tsv")
    own_ok = os.path.exists(own_manifest)
    own_sha = hashlib.sha256(open(own_manifest, "rb").read()).hexdigest() if own_ok else "MISSING"
    bound = {ln.split("	")[0]: ln.split("	")[1] for ln in ext_rows}
    if own_ok:
        key = "../06-independent-verification/evidence-sha256.tsv"
        check("cross-binding: own manifest sha matches 00-prereg registration",
              bound.get(key) == own_sha,
              "registered=%s actual=%s" % (bound.get(key, "NONE")[:16], own_sha[:16]))
        script_fp = os.path.join(os.path.dirname(HERE), "06-independent-verification",
                                 "main_agent_correction_verification.py")
        script_sha = hashlib.sha256(open(script_fp, "rb").read()).hexdigest()
        key2 = "../06-independent-verification/main_agent_correction_verification.py"
        check("cross-binding: verification script sha matches registration",
              bound.get(key2) == script_sha,
              "registered=%s actual=%s" % (bound.get(key2, "NONE")[:16], script_sha[:16]))
        # own manifest internal closure
        own_lines = io.open(own_manifest, encoding="utf-8").read().splitlines()[1:]
        om_missing = om_mismatch = 0
        for ln in own_lines:
            pth, h = ln.split("	")
            fp = os.path.join(os.path.dirname(own_manifest), pth)
            if not os.path.exists(fp):
                om_missing += 1
            elif hashlib.sha256(open(fp, "rb").read()).hexdigest() != h:
                om_mismatch += 1
        check("own manifest reverse verification (0/0)",
              om_missing == 0 and om_mismatch == 0,
              "entries=%d missing=%d mismatch=%d" % (len(own_lines), om_missing, om_mismatch))

    # ---- first-admission restriction (contract) ----
    m = tr.window_metrics(FPAST, [ev(G, "a", P_SHARED, 26010), ev(C, "b", P_SHARED, 26020)])
    fa = m["firstAdmission"].get(P_SHARED)
    check("firstAdmission still recorded (descriptive) == GLOBAL (earliest event)",
          fa == G, str(fa))
    # and it must NOT leak into gating: WHVG_GLOBAL == 0 despite first-admission
    check("firstAdmission does not leak into WHVG gating",
          m["perSource"][G]["whvg"] == 0.0)

    print()
    if failures:
        print("INDEPENDENT_VERIFICATION = FAILED (%d)" % len(failures))
        return 1
    print("INDEPENDENT_VERIFICATION = PASSED (expectations given by test contract)")
    return 0


if __name__ == "__main__":
    sys.exit(run())
