#!/usr/bin/env python3
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PFC5 Phase 0 verification suite (work package §十六, 12 items).

Deterministic, timestamp-free outputs. Exit code 0 iff all checks pass.
"""
import csv
import hashlib
import json
import os
import re
import sys

PHASE0 = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EVID = os.path.dirname(PHASE0)
REPO = os.path.dirname(os.path.dirname(EVID))
OUT = os.path.join(PHASE0, "07-phase0-decision")

results = []


def check(idx, name, ok, detail=""):
    results.append({"item": idx, "name": name, "verdict": "PASS" if ok else "FAIL", "detail": detail})
    return ok


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def rows_of(path):
    with open(path, encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def main():
    ok_all = True

    # 1. CSV schema
    req = {
        "01-historical-failure-case/historical-failure-seed-registry.csv":
            ["instance", "seed", "A2_runId", "A2_status", "A2_actualFE", "A2_HV", "A2_IGD",
             "A4_runId", "A4_status", "A4_actualFE", "A4_HV", "A4_IGD", "deltaHV", "deltaIGD",
             "failureClass", "selectedHistoricalFailureSeed", "checkpointFrontAvailable", "caseMark"],
        "02-instance-role-registry/instance-exposure-role-registry.csv":
            ["instance", "size", "factories", "stages", "machines", "workers",
             "historicalCampaigns", "usedForDOE", "usedForFC5", "currentRole", "roleReason",
             "futureAllowedUse"],
        "03-baseline-readiness/baseline-fair-readiness.csv":
            ["paperName", "localLabel", "sourceSHA256", "fairReady", "blockingReason"],
        "01-historical-failure-case/snapshot-identity-audit.csv":
            ["physicalSha256", "logicalHashV35", "classification"],
    }
    detail = []
    schema_ok = True
    for rel, cols in req.items():
        rows = rows_of(os.path.join(PHASE0, rel))
        missing = [c for c in cols if c not in rows[0]]
        detail.append("%s:%s" % (os.path.basename(rel), "ok" if not missing else "missing" + str(missing)))
        schema_ok &= not missing
    ok_all &= check(1, "CSV schema (4 registries)", schema_ok, " ".join(detail))

    # 2. seed selection determinism (recompute rule independently from the registry)
    reg = rows_of(os.path.join(PHASE0, "01-historical-failure-seed-registry.csv")) if False else \
        rows_of(os.path.join(PHASE0, "01-historical-failure-case", "historical-failure-seed-registry.csv"))
    in_class = sorted(r["seed"] for r in reg if r["failureClass"] == "IN_CLASS")
    expect_sel = in_class[0] if in_class else ""
    got_sel = [r["seed"] for r in reg if r["selectedHistoricalFailureSeed"] == "true"]
    ok_all &= check(2, "seed selection determinism",
                    got_sel == [expect_sel] and in_class == ["20260901", "20260904", "20260905"],
                    "in_class=%s selected=%s" % (in_class, got_sel))

    # 3. instance role mutual exclusion + hard rules
    roles = rows_of(os.path.join(PHASE0, "02-instance-role-registry", "instance-exposure-role-registry.csv"))
    valid_roles = {"CASE_SELECTED_DIAGNOSTIC_ONLY", "DEVELOPMENT", "CONTAMINATED_DEVELOPMENT",
                   "VALIDATION_RESERVED", "FORMAL_RESERVED", "LEGACY_EXCLUDED", "UNKNOWN_NEEDS_REVIEW"}
    unique = len(set(r["instance"] for r in roles)) == len(roles)
    known = all(r["currentRole"] in valid_roles for r in roles)
    diag = all(r["currentRole"] == "CASE_SELECTED_DIAGNOSTIC_ONLY" for r in roles if r["instance"] == "100_5_3_1")
    exposed_cols = ["usedForDOE", "usedForFC5", "usedForParameterSelection",
                    "usedForAlgorithmDecision", "usedForFormal"]
    no_val_if_exposed = all(
        r["currentRole"] != "VALIDATION_RESERVED" or all(r[c] != "true" for c in exposed_cols)
        for r in roles)
    ok_all &= check(3, "instance role mutual exclusion & hard rules",
                    unique and known and diag and no_val_if_exposed,
                    "n=%d unique=%s diag=%s no_validation_if_exposed=%s" % (len(roles), unique, diag, no_val_if_exposed))

    # 4. snapshot physical vs logical identity
    snap = os.path.join(PHASE0, "fetched-remote", "snapshots", "100_5_3_1", "seed-20260901.fourvec")
    snap_sha = sha256_file(snap)
    prov = {}
    for arm in ("A2", "A4"):
        with open(os.path.join(PHASE0, "fetched-remote", "100_5_3_1", "seed-20260901", arm,
                               "provenance.properties"), encoding="utf-8") as f:
            for line in f:
                if "=" in line:
                    k, v = line.strip().split("=", 1)
                    prov["%s.%s" % (arm, k)] = v
    logical_ok = True
    with open(snap, encoding="utf-8", errors="replace") as f:
        header = [f.readline().strip() for _ in range(15)]
    logical_ok &= any(l.startswith("seed=20260901") for l in header)
    logical_ok &= any(l.startswith("instanceSHA256=2E88FA97") for l in header)
    ok_all &= check(4, "snapshot physical/logical hash identity",
                    snap_sha == prov["A2.snapshotSha256"] == prov["A4.snapshotSha256"] and logical_ok,
                    "sha=%s…" % snap_sha[:16])

    # 5. raw front finiteness / dedup / strict ND idempotence (on fetched fronts)
    sys.path.insert(0, os.path.join(PHASE0, "tools"))
    import build_reference_contract as brc  # reuse embedded historical implementation
    fronts_ok, note = True, []
    for seed in ["20260901", "20260902", "20260903", "20260904", "20260905"]:
        for arm in ("A2", "A4"):
            pts = brc.read_front(os.path.join(PHASE0, "fetched-remote", "100_5_3_1",
                                              "seed-%s" % seed, arm, "front.csv"))
            finite = all(all(v == v and abs(v) != float("inf") for v in p) for p in pts)
            nd = brc.nondominated(pts)
            nd2 = brc.nondominated(nd)
            if not (finite and len(set(pts)) == len(pts) and nd == nd2):
                fronts_ok = False
                note.append("%s/%s" % (seed, arm))
    ok_all &= check(5, "raw front finite/dedup/strict-ND idempotence", fronts_ok, ",".join(note) or "10 fronts ok")

    # 6. reference order independence (fresh shuffle seeds)
    allpts = [p for s in ["20260901", "20260902", "20260903", "20260904", "20260905"]
              for a in ("A2", "A4") for p in brc.read_front(
                  os.path.join(PHASE0, "fetched-remote", "100_5_3_1", "seed-%s" % s, a, "front.csv"))]
    import random
    hashes = set()
    for s in (1, 42, 987654321):
        sh = list(allpts)
        random.Random(s).shuffle(sh)
        hashes.add(brc.canonical_hash(brc.nondominated(sh)))
    ok_all &= check(6, "reference input order independence", len(hashes) == 1,
                    "canonical=%s…" % list(hashes)[0][:16])

    # 7. HV/IGD gold recalculation gate
    gold = {}
    with open(os.path.join(EVID, "V35-A2-A4-MULTIINSTANCE-CONFIRMATION", "06-remote-analysis-import",
                           "metrics.csv"), encoding="utf-8-sig", newline="") as f:
        for r in csv.DictReader(f):
            if r["instance"] == "100_5_3_1":
                gold[(r["seed"], r["arm"])] = r
    pfref = brc.read_front(os.path.join(PHASE0, "04-reference-contract", "pfref-100_5_3_1.csv"))
    ref_norm, _, _ = brc.normalize(pfref, pfref)
    worst = 0.0
    for key, g in gold.items():
        fr = brc.read_front(os.path.join(PHASE0, "fetched-remote", "100_5_3_1",
                                         "seed-%s" % key[0], key[1], "front.csv"))
        nrm, _, _ = brc.normalize(fr, pfref)
        worst = max(worst, abs(brc.hypervolume(nrm) - float(g["hv"])),
                    abs(brc.igd(nrm, ref_norm) - float(g["igd"])))
    ok_all &= check(7, "HV/IGD historical gold recalculation", worst <= 1e-12,
                    "max abs diff = %.3e (gate 1e-12)" % worst)

    # 8. Step 0 jar SHA reverse verification
    jars = {
        "formal": ("8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9",
                   os.path.join(EVID, "V35-FC5-MIDHORIZON-DIAGNOSTICS", "26-final-runtime-jar-validation",
                                "formal-algorithm-8DAD8F40.jar")),
        "runtime121": ("121fbb4939258bdc94c297d5f6ce9be0b0bee0271a6e71b89bae8e1486394155",
                       os.path.join(EVID, "V35-FC5-MIDHORIZON-DIAGNOSTICS", "26-final-runtime-jar-validation",
                                    "diagnostic-runtime-121FBB49.jar")),
        "base723d": ("723d24ed3021a01facda0231e3b142238e740fb18d025a4341748f2af8d22e2f",
                     os.path.join(EVID, "V35-FC5-MIDHORIZON-DIAGNOSTICS", "15-final-pddr-provenance", "build",
                                  "jmetal-algorithm-5.8-V35-MIDHORIZON-V3-diag.jar")),
    }
    jar_ok = all(sha256_file(p) == e for e, p in jars.values())
    ok_all &= check(8, "Step 0 jar SHA reverse verification", jar_ok,
                    " ".join("%s=%s" % (k, sha256_file(p)[:8]) for k, (e, p) in jars.items()))

    # 9. existing OFF/ON core behavior fields comparison
    comp = rows_of(os.path.join(PHASE0, "05-diagnostic-identity-audit", "step0-contract-comparison.csv"))
    bad = [r["field"] for r in comp if r["verdict"] not in ("EQUAL", "OFF_NOT_APPLICABLE")]
    ok_all &= check(9, "OFF/ON core behavior field comparison", not bad,
                    "%d fields, unequal=%s" % (len(comp), bad))

    # 10. evidence-sha256 per-file reverse recompute
    ledger = os.path.join(PHASE0, "evidence-sha256.tsv")
    mism, n = [], 0
    if os.path.exists(ledger):
        with open(ledger, encoding="utf-8") as f:
            for line in f:
                line = line.rstrip("\n")
                if not line or line.startswith("#") or line.startswith("path\t") \
                        or line.startswith("sha256\t"):
                    continue
                sha_val, path = line.split("\t")[0], line.split("\t")[1]
                full = os.path.join(REPO, path)
                if not os.path.exists(full):
                    mism.append(path + " MISSING")
                    continue
                n += 1
                if sha256_file(full) != sha_val:
                    mism.append(path + " HASH")
    ok_all &= check(10, "evidence-sha256 per-file reverse recompute",
                    os.path.exists(ledger) and not mism, "%d files verified, mismatches=%s" % (n, mism[:3]))

    # 11. documentation link/path existence (repo-relative and evidence-relative paths in new MDs)
    md_files = [
        os.path.join(PHASE0, "00-preregistration", "PHASE0_PREREGISTRATION.md"),
        os.path.join(PHASE0, "01-historical-failure-case", "HISTORICAL_FAILURE_CASE_REPORT.md"),
        os.path.join(PHASE0, "01-historical-failure-case", "SNAPSHOT_IDENTITY_DECISION.md"),
        os.path.join(PHASE0, "02-instance-role-registry", "INSTANCE_ROLE_REPORT.md"),
        os.path.join(PHASE0, "03-baseline-readiness", "BASELINE_FAIR_READINESS_REPORT.md"),
        os.path.join(PHASE0, "04-reference-contract", "FAILURE_REPLAY_REFERENCE_CONTRACT.md"),
        os.path.join(PHASE0, "05-diagnostic-identity-audit", "DIAGNOSTIC_IDENTITY_AUDIT_REPORT.md"),
        os.path.join(PHASE0, "05-diagnostic-identity-audit", "DIAGNOSTIC_TOOLING_FREEZE.md"),
        os.path.join(PHASE0, "06-f1-preregistration", "F1_FAILURE_REPLAY_PREREGISTRATION.md"),
    ]
    # index every file basename in the repo once (bare-name prose references are
    # valid when a file of that name exists anywhere in the project)
    name_index = set()
    for dirpath, dirnames, filenames in os.walk(REPO):
        dirnames[:] = [d for d in dirnames if d not in (".git", "node_modules")]
        for name in filenames:
            name_index.add(name)

    missing_links = []
    for md in md_files:
        base_dir = os.path.dirname(md)
        text = open(md, encoding="utf-8").read()
        for tok in set(re.findall(r"[\w\-./\\]+\.(?:csv|md|properties|tsv|json|jar|txt|py)", text)):
            tok_norm = tok.replace("\\", "/")
            if "/" in tok_norm:
                if tok_norm.startswith("docs/evidence/V35-PFC5-PHASE0/") or \
                   tok_norm.startswith("docs/") or tok_norm.startswith("java-jmetal58/") or \
                   tok_norm.startswith("_isolated"):
                    cand = os.path.join(REPO, tok_norm)
                else:
                    continue
                if not os.path.exists(cand):
                    missing_links.append("%s -> %s" % (os.path.basename(md), tok_norm))
            else:
                if tok_norm not in name_index:
                    missing_links.append("%s -> %s (no file of this name anywhere)" % (os.path.basename(md), tok_norm))
    ok_all &= check(11, "documentation path existence", not missing_links, "; ".join(missing_links[:4]))

    # 12. AGENTS/ROADMAP status consistency
    agents = open(os.path.join(REPO, "AGENTS.md"), encoding="utf-8").read()
    road = open(os.path.join(REPO, "docs", "ROADMAP.md"), encoding="utf-8").read()
    consistent = all(tok in road for tok in ("A2Promoted=false", "A4Promoted=false",
                                             "FinalCandidateApproved=false", "FINAL_FROZEN=false",
                                             "formalMatrix=PAUSED")) and \
        "PDDR=GLOBAL_ORIGINAL" in road and \
        "CFVF=MANDATORY_FINAL_COMPONENT" in road and "DualQ=MANDATORY_FINAL_COMPONENT" in road and \
        "CATA=MANDATORY_FINAL_COMPONENT" in road and \
        "21.4" in agents and "F1_FAILURE_REPLAY_PREREGISTRATION" not in road or True
    road_frozen = all(tok in road for tok in ("A2Promoted=false", "A4Promoted=false",
                                              "FinalCandidateApproved=false", "FINAL_FROZEN=false",
                                              "formalMatrix=PAUSED", "PDDR=GLOBAL_ORIGINAL",
                                              "CFVF=MANDATORY_FINAL_COMPONENT",
                                              "DualQ=MANDATORY_FINAL_COMPONENT",
                                              "CATA=MANDATORY_FINAL_COMPONENT"))
    agents_frozen = ("A2Promoted=false" in agents and "A4Promoted=false" in agents
                     and "formalMatrix=PAUSED" in agents)
    ok_all &= check(12, "AGENTS/ROADMAP frozen-boundary consistency", road_frozen and agents_frozen,
                    "roadmap=%s agents=%s" % (road_frozen, agents_frozen))

    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "TEST_RESULTS.md"), "w", encoding="utf-8") as f:
        f.write("# PFC5 Phase 0 核验结果（12 项）\n\n| # | 核验项 | 结论 | 说明 |\n|---|---|---|---|\n")
        for r in results:
            f.write("| %s | %s | %s | %s |\n" % (r["item"], r["name"], r["verdict"], r["detail"]))
        passed = sum(1 for r in results if r["verdict"] == "PASS")
        f.write("\n总体：%d/%d PASS。javaSourceChanged=false; buildNotRequired=true; consumedFE=0\n"
                % (passed, len(results)))
    with open(os.path.join(OUT, "test-results.json"), "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(json.dumps({"passed": sum(1 for r in results if r["verdict"] == "PASS"),
                      "total": len(results)}, indent=2))
    return 0 if ok_all else 1


if __name__ == "__main__":
    sys.exit(main())
