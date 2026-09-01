#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PFC5-1A: build historical-failure-seed-registry.csv for 100_5_3_1 (A2 vs A4, 500k).

Read-only inputs:
  - V35-A2-A4-MULTIINSTANCE-CONFIRMATION/06-remote-analysis-import/{metrics,paired-deltas,acceptance-run-audit}.csv
  - V35-PFC5-PHASE0/fetched-remote/100_5_3_1/seed-*/{A2,A4}/{provenance.properties,status.properties,initial-population.sha256,profile.sha256}

Deterministic selection rule (frozen in 00-preregistration/PHASE0_PREREGISTRATION.md):
  failure class = (deltaHV < -0.05) AND (deltaIGD < -0.20); selected = min seed ID in class.

Writes:
  - 01-historical-failure-case/historical-failure-seed-registry.csv
  - 01-historical-failure-case/registry-summary.json  (machine-checkable, used by phase0 tests)

This tool performs no simulation and consumes no function evaluations.
"""
import csv
import json
import hashlib
import os
import sys
from datetime import datetime, timezone

PHASE0 = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EVID = os.path.dirname(PHASE0)
REPO = os.path.dirname(os.path.dirname(EVID))
CONF = os.path.join(EVID, "V35-A2-A4-MULTIINSTANCE-CONFIRMATION", "06-remote-analysis-import")
FETCH = os.path.join(PHASE0, "fetched-remote", "100_5_3_1")

INSTANCE = "100_5_3_1"
SEEDS = ["20260901", "20260902", "20260903", "20260904", "20260905"]
ARMS = ["A2", "A4"]
HV_GATE = -0.05
IGD_GATE = -0.20
OUT_DIR = os.path.join(PHASE0, "01-historical-failure-case")


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def read_props(path):
    out = {}
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.strip()
            if not line or "=" not in line or line.startswith("#"):
                continue
            k, v = line.split("=", 1)
            out[k.strip()] = v.strip()
    return out


def read_ledger(path):
    with open(path, "r", encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def local_front_rel(seed, arm):
    return "docs/evidence/V35-PFC5-PHASE0/fetched-remote/100_5_3_1/seed-%s/%s/front.csv" % (seed, arm)


def remote_front_rel(seed, arm):
    return "/home/inspur/aicomp/zhangbo-v35-a2-a4-confirmation-20260824/results/100_5_3_1/seed-%s/%s/front.csv" % (seed, arm)


def main():
    metrics = {(r["instance"], r["seed"], r["arm"]): r
               for r in read_ledger(os.path.join(CONF, "metrics.csv")) if r["instance"] == INSTANCE}
    audit = {(r["instance"], r["seed"], r["arm"]): r
             for r in read_ledger(os.path.join(CONF, "acceptance-run-audit.csv")) if r["instance"] == INSTANCE}
    deltas = {r["seed"]: r for r in read_ledger(os.path.join(CONF, "paired-deltas.csv"))
              if r["instance"] == INSTANCE}

    rows = []
    for seed in SEEDS:
        d = deltas[seed]
        dhv = float(d["deltaHV"])
        digd = float(d["deltaIGD"])
        if dhv < HV_GATE and digd < IGD_GATE:
            fclass = "IN_CLASS"
        elif dhv < HV_GATE:
            fclass = "PARTIAL_HV_ONLY"
        elif digd < IGD_GATE:
            fclass = "PARTIAL_IGD_ONLY"
        else:
            fclass = "NOT_IN_CLASS"
        row = {
            "instance": INSTANCE,
            "seed": seed,
            "failureClass": fclass,
            "deltaHV": d["deltaHV"],
            "deltaIGD": d["deltaIGD"],
            "deltaCmax": d["deltaCmax"],
            "hvGate": str(HV_GATE),
            "igdGate": str(IGD_GATE),
            "checkpointFrontAvailable": "false",
            "checkpointFrontEvidence": "V35-FC5-100JOB-TRANSFER/02-field-availability/existing-field-availability.csv: front=AVAILABLE(final only); no per-generation fronts recorded for 500k confirmation runs",
            "caseMark": "CASE_SELECTED_DIAGNOSTIC_ONLY",
        }
        for arm in ARMS:
            m = metrics[(INSTANCE, seed, arm)]
            a = audit[(INSTANCE, seed, arm)]
            run_dir = os.path.join(FETCH, "seed-%s" % seed, arm)
            prov = read_props(os.path.join(run_dir, "provenance.properties"))
            status = read_props(os.path.join(run_dir, "status.properties"))
            front_path = os.path.join(run_dir, "front.csv")
            local_sha = sha256_file(front_path)
            registered_sha = a["frontSha256"].strip().lower()
            front_match = "true" if local_sha == registered_sha else "false"
            p = arm.lower()
            row.update({
                "A%d_runId" % (2 if arm == "A2" else 4): a["runId"],
                "A%d_status" % (2 if arm == "A2" else 4): a["status"],
                "A%d_actualFE" % (2 if arm == "A2" else 4): a["actualFE"],
                "A%d_HV" % (2 if arm == "A2" else 4): m["hv"],
                "A%d_IGD" % (2 if arm == "A2" else 4): m["igd"],
                "A%d_frontLocalPath" % (2 if arm == "A2" else 4): local_front_rel(seed, arm),
                "A%d_frontRemotePath" % (2 if arm == "A2" else 4): remote_front_rel(seed, arm),
                "A%d_frontSha256" % (2 if arm == "A2" else 4): local_sha,
                "A%d_frontSha256MatchLedger" % (2 if arm == "A2" else 4): front_match,
                "A%d_snapshotPath" % (2 if arm == "A2" else 4):
                    "docs/evidence/V35-PFC5-PHASE0/fetched-remote/snapshots/100_5_3_1/seed-%s.fourvec" % seed,
                "A%d_snapshotSha256" % (2 if arm == "A2" else 4): prov["snapshotSha256"],
                "A%d_initialPopulationHashV35" % (2 if arm == "A2" else 4): prov["initialPopulationHashV35"],
                "A%d_initialPopulationHashP8" % (2 if arm == "A2" else 4): prov["initialPopulationHashP8"],
                "A%d_armProfileSha256" % (2 if arm == "A2" else 4): prov["armProfileSha256"],
                "A%d_formalAlgorithmJarSha256" % (2 if arm == "A2" else 4): prov["frozenJarSha256"],
                "A%d_problemConfigurationSha256" % (2 if arm == "A2" else 4): prov["problemConfigurationSha256"],
                "A%d_setupConfigurationSha256" % (2 if arm == "A2" else 4): prov["setupConfigurationSha256"],
                "A%d_fatigueConfigurationSha256" % (2 if arm == "A2" else 4): prov["fatigueConfigurationSha256"],
                "A%d_instanceSha256" % (2 if arm == "A2" else 4): prov["instanceSha256"],
                "A%d_decoderCalls" % (2 if arm == "A2" else 4): a["decoderCalls"],
            })
        rows.append(row)

    in_class = [r["seed"] for r in rows if r["failureClass"] == "IN_CLASS"]
    in_class.sort()
    selected = in_class[0] if in_class else ""
    for r in rows:
        r["eligibleHistoricalFailureSeed"] = "true" if r["failureClass"] == "IN_CLASS" else "false"
        if selected and r["seed"] == selected:
            r["selectedHistoricalFailureSeed"] = "true"
            r["selectionReason"] = (
                "smallest seed ID among IN_CLASS seeds (rule frozen in "
                "00-preregistration/PHASE0_PREREGISTRATION.md); NOTE: this seed is also the "
                "largest-degradation seed in the class and the Step-0 tool-acceptance seed; "
                "selection follows the minimum-ID rule, NOT worst-case or convenience picking"
            )
        else:
            r["selectedHistoricalFailureSeed"] = "false"
            r["selectionReason"] = ""

    os.makedirs(OUT_DIR, exist_ok=True)
    fieldnames = [
        "instance", "seed",
        "A2_runId", "A2_status", "A2_actualFE", "A2_HV", "A2_IGD",
        "A2_frontLocalPath", "A2_frontRemotePath", "A2_frontSha256", "A2_frontSha256MatchLedger",
        "A2_snapshotPath", "A2_snapshotSha256", "A2_initialPopulationHashV35", "A2_initialPopulationHashP8",
        "A2_armProfileSha256", "A2_formalAlgorithmJarSha256", "A2_problemConfigurationSha256",
        "A2_setupConfigurationSha256", "A2_fatigueConfigurationSha256", "A2_instanceSha256",
        "A2_decoderCalls",
        "A4_runId", "A4_status", "A4_actualFE", "A4_HV", "A4_IGD",
        "A4_frontLocalPath", "A4_frontRemotePath", "A4_frontSha256", "A4_frontSha256MatchLedger",
        "A4_snapshotPath", "A4_snapshotSha256", "A4_initialPopulationHashV35", "A4_initialPopulationHashP8",
        "A4_armProfileSha256", "A4_formalAlgorithmJarSha256", "A4_problemConfigurationSha256",
        "A4_setupConfigurationSha256", "A4_fatigueConfigurationSha256", "A4_instanceSha256",
        "A4_decoderCalls",
        "deltaHV", "deltaIGD", "deltaCmax", "hvGate", "igdGate",
        "failureClass", "eligibleHistoricalFailureSeed",
        "selectedHistoricalFailureSeed", "selectionReason",
        "checkpointFrontAvailable", "checkpointFrontEvidence", "caseMark",
    ]
    out_csv = os.path.join(OUT_DIR, "historical-failure-seed-registry.csv")
    with open(out_csv, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames, lineterminator="\n")
        w.writeheader()
        for r in rows:
            w.writerow(r)

    summary = {
        "instance": INSTANCE,
        "comparison": "A2_vs_A4",
        "budget": 500000,
        "seeds": SEEDS,
        "failureClassBySeed": {r["seed"]: r["failureClass"] for r in rows},
        "inClassSeeds": in_class,
        "selectedHistoricalFailureSeed": selected,
        "selectionRule": "min seed ID among IN_CLASS (deltaHV<%.2f AND deltaIGD<%.2f vs paired A2, 500k final fronts)" % (HV_GATE, IGD_GATE),
        "frontShaLedgerMatches": all(r["A2_frontSha256MatchLedger"] == "true" and
                                     r["A4_frontSha256MatchLedger"] == "true" for r in rows),
        "allRunsAcceptedFE500000": all(r["A2_status"] == "ACCEPTED" and r["A4_status"] == "ACCEPTED"
                                       and r["A2_actualFE"] == "500000" and r["A4_actualFE"] == "500000"
                                       for r in rows),
        "checkpointFrontAvailable": False,
        "generatedAtUtc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "consumedFE": 0,
        "changedAlgorithm": False,
    }
    with open(os.path.join(OUT_DIR, "registry-summary.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2, ensure_ascii=False)

    print(json.dumps(summary, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
