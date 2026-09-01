#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PFC5-1C: census every DHHFSP instance across campaign evidence and freeze roles.

Run-level exposure is detected ONLY from structured run evidence:
  - status.properties files (instance= key)
  - ledger CSVs whose header contains runId, or (seed AND arm), or (seed AND status)
  - files whose PATH contains an instance-ID path segment under a run-ish root
    (/results/, /runs/, /raw/, /output*, /04-development-runs/)
Excluded (non-decisional materialization): V35-FORMAL-MANIFEST, V35-PFC5-PHASE0,
master-run-registry / paired-group-registry / any *template* file.

Roles (frozen rules):
  100_5_3_1                      -> CASE_SELECTED_DIAGNOSTIC_ONLY
  in 45-roster & run-exposed     -> CONTAMINATED_DEVELOPMENT
  in 45-roster & never run-used  -> VALIDATION_RESERVED (default holdout; census-dated)
  not in roster (pilot-only)     -> LEGACY_EXCLUDED
Zero FE. Read-only over all evidence.
"""
import csv
import hashlib
import json
import os
import re
import sys
from datetime import datetime, timezone

PHASE0 = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EVID = os.path.dirname(PHASE0)
REPO = os.path.dirname(os.path.dirname(EVID))
MANIFEST = os.path.join(EVID, "V35-FORMAL-MANIFEST", "FORMAL_INSTANCE_MANIFEST.csv")
OUT_DIR = os.path.join(PHASE0, "02-instance-role-registry")

ID_RE = re.compile(r"\b(\d{1,3}_\d_\d_\d)\b")
DIRSEG_RE = re.compile(r"(?:^|[/\\])(\d{1,3}_\d_\d_\d)(?:[/\\]|$)")
RUN_ROOTS = ("/results/", "/runs/", "/raw/", "/output", "/04-development-runs/", "\\results\\", "\\runs\\", "\\raw\\", "\\output")
EXCLUDE_DIRS = {"V35-PFC5-PHASE0", "V35-FORMAL-MANIFEST"}
EXCLUDE_NAME_PARTS = ("master-run-registry", "paired-group-registry", "template")
MAX_TEXT_BYTES = 4_000_000

# curated supplements (campaign -> instances), each verified by hand in Phase 0
SUPPLEMENT = {
    "P8": {"20_2_3_1"},
    "P8.1": {"20_2_3_1"},
    "P8.3": {"20_2_3_1"},
    "P9": {"20_2_3_1"},
    "V35-P25D-all-algorithms-50k-pilot": {"20_2_3_1"},
    "PF对比发放_20_2_3_1(repo-root)": {"20_2_3_1"},
}


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def read_instance_file(path):
    """Parse (jobs, factories, stages, machines, workers) from an EADHFSP instance file."""
    try:
        with open(path, encoding="utf-8", errors="replace") as f:
            lines = [ln.strip() for ln in f if ln.strip()]
        stages, factories, jobs = (int(x) for x in lines[0].split()[:3])
        machines = workers = None
        for i, ln in enumerate(lines):
            low = ln.lower()
            if low.startswith("number of machines"):
                total = 0
                for row in lines[i + 1:i + 1 + stages]:
                    total += sum(int(v) for v in row.split(","))
                machines = total
            if low.startswith("number of workers"):
                workers = sum(int(v) for v in lines[i + 1].split(","))
        return jobs, factories, stages, machines, workers
    except Exception:
        return None, None, None, None, None


def scan_campaign(campaign, root):
    found = set()
    hits = []
    for dirpath, dirnames, filenames in os.walk(root):
        rel = os.path.relpath(dirpath, EVID).replace("\\", "/")
        parts = rel.split("/")
        if any(part in EXCLUDE_NAME_PARTS for part in parts):
            continue
        # instance-id path segments under run-ish roots
        if any(marker in rel + "/" for marker in RUN_ROOTS):
            for part in parts:
                if ID_RE.fullmatch(part):
                    found.add(part)
        for name in filenames:
            if any(part in name for part in EXCLUDE_NAME_PARTS):
                continue
            full = os.path.join(dirpath, name)
            if name == "status.properties":
                try:
                    with open(full, encoding="utf-8", errors="replace") as f:
                        for line in f:
                            if line.lower().startswith("instance="):
                                found.add(line.split("=", 1)[1].strip().removesuffix(".txt"))
                                break
                except OSError:
                    pass
                continue
            if not name.lower().endswith(".csv"):
                continue
            try:
                with open(full, encoding="utf-8", errors="replace") as f:
                    header = f.readline()
                    if not ("runId" in header or ("seed" in header and "arm" in header)
                            or ("seed" in header and "status" in header)):
                        continue
                    col = None
                    cols = header.strip().split(",")
                    if "instance" in cols:
                        col = cols.index("instance")
                    elif "instanceId" in cols:
                        col = cols.index("instanceId")
                    rows = 0
                    for line in f:
                        rows += 1
                        if rows > 20000:
                            break
                        if col is not None:
                            m = ID_RE.search(line.split(",")[col] if col < len(line.split(",")) else "")
                        else:
                            m = ID_RE.search(line)
                        if m:
                            found.add(m.group(1))
            except OSError:
                pass
    return found


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    roster = {}
    with open(MANIFEST, encoding="utf-8-sig", newline="") as f:
        for row in csv.DictReader(f):
            roster[row["instanceId"]] = row

    all_ids = set(roster)
    # pilot instances present as files
    pilot_dir = os.path.join(REPO, "java-jmetal58", "EADHFSP-pilot", "EADHFSP")
    if os.path.isdir(pilot_dir):
        for name in os.listdir(pilot_dir):
            m = ID_RE.search(name)
            if m:
                all_ids.add(m.group(1))
    main_dir = os.path.join(REPO, "java-jmetal58", "EADHFSP")
    for name in os.listdir(main_dir):
        m = ID_RE.search(name)
        if m and name.endswith(".txt"):
            all_ids.add(m.group(1))

    campaigns = sorted(d for d in os.listdir(EVID)
                       if os.path.isdir(os.path.join(EVID, d)) and d not in EXCLUDE_DIRS)
    exposure = {}   # campaign -> set(ids)
    for c in campaigns:
        exposure[c] = scan_campaign(c, os.path.join(EVID, c))
    for c, ids in SUPPLEMENT.items():
        s = exposure.setdefault(c, set())
        s |= ids

    DOE_CAMPAIGNS = {"V35-A2-A4-MULTIINSTANCE-CONFIRMATION", "V35-DOE1-subgroup-mixture"}
    FC5_CAMPAIGNS = {c for c in exposure if c.startswith("V35-FC5")}
    PARAM_CAMPAIGNS = {"P8", "P8.1", "P8.3", "P9", "V35-P25D-all-algorithms-50k-pilot"}
    DECISION_CAMPAIGNS = {"V35-A2-A4-MULTIINSTANCE-CONFIRMATION",
                          "V35-A2-FINAL-CANDIDATE-CONFIRMATION",
                          "V35-STAGE2-PILOT-A0-A4-20260823", "V35-STAGE2-MASTER-V2"}
    FORMAL_CAMPAIGNS = {"V35-STAGE2-MASTER-V2", "V35-STAGE2-PILOT-A0-A4-20260823"}

    rows = []
    for inst in sorted(all_ids):
        exposed_in = sorted(c for c, ids in exposure.items() if inst in ids)
        in_roster = inst in roster
        jobs, factories, stages, machines, workers = read_instance_file(
            os.path.join(REPO, "java-jmetal58", "EADHFSP", "%s.txt" % inst))
        scale = roster.get(inst, {}).get("scale", "")
        if inst == "100_5_3_1":
            role = "CASE_SELECTED_DIAGNOSTIC_ONLY"
            reason = ("historical failure case selected by frozen minimum-ID rule "
                      "(00-preregistration/PHASE0_PREREGISTRATION.md); diagnostic use only")
        elif in_roster and exposed_in:
            role = "CONTAMINATED_DEVELOPMENT"
            reason = "run-level exposure in campaign evidence; eligible for development/Race, never validation"
        elif in_roster:
            role = "VALIDATION_RESERVED"
            reason = ("zero run-level exposure in 2026-08-29 census (formal-roster instance, "
                      "snapshot materialization only); default holdout assignment, "
                      "reclassification requires user approval")
        else:
            role = "LEGACY_EXCLUDED"
            reason = "pilot/wiring-only instance outside the 45-instance formal roster"

        rows.append({
            "instance": inst,
            "size": str(jobs) if jobs else "",
            "factories": str(factories) if factories else "",
            "stages": str(stages) if stages else "",
            "machines": str(machines) if machines else "",
            "workers": str(workers) if workers else "",
            "scale": scale,
            "in45Roster": str(in_roster).lower(),
            "historicalCampaigns": ";".join(exposed_in) if exposed_in else
                "V35-FORMAL-MANIFEST(materialization-only)",
            "usedForDOE": str(bool(set(exposed_in) & DOE_CAMPAIGNS)).lower(),
            "usedForFC5": str(bool(set(exposed_in) & FC5_CAMPAIGNS)).lower(),
            "usedForParameterSelection": str(bool(set(exposed_in) & PARAM_CAMPAIGNS)).lower(),
            "usedForAlgorithmDecision": str(bool(set(exposed_in) & DECISION_CAMPAIGNS)).lower(),
            "usedForValidation": "false",
            "usedForFormal": str(bool(set(exposed_in) & FORMAL_CAMPAIGNS)).lower(),
            "currentRole": role,
            "roleReason": reason,
            "sourcePaths": "docs/evidence census + java-jmetal58/EADHFSP/%s.txt" % inst,
            "sourceHashes": roster.get(inst, {}).get("instanceSHA256", ""),
            "futureAllowedUse": {
                "CASE_SELECTED_DIAGNOSTIC_ONLY": "diagnosis only; never Race/Validation/Formal/Final/paper statistics",
                "CONTAMINATED_DEVELOPMENT": "development incl. Configuration Race; never Validation holdout",
                "VALIDATION_RESERVED": "validation holdout only until user releases it",
                "LEGACY_EXCLUDED": "excluded from V35 experiments",
            }[role],
        })

    fieldnames = ["instance", "size", "factories", "stages", "machines", "workers", "scale",
                  "in45Roster", "historicalCampaigns", "usedForDOE", "usedForFC5",
                  "usedForParameterSelection", "usedForAlgorithmDecision", "usedForValidation",
                  "usedForFormal", "currentRole", "roleReason", "sourcePaths", "sourceHashes",
                  "futureAllowedUse"]
    out_csv = os.path.join(OUT_DIR, "instance-exposure-role-registry.csv")
    with open(out_csv, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames, lineterminator="\n")
        w.writeheader()
        w.writerows(rows)

    by_role = {}
    for r in rows:
        by_role.setdefault(r["currentRole"], []).append(r["instance"])
    summary = {
        "totalInstances": len(rows),
        "roleCounts": {k: len(v) for k, v in sorted(by_role.items())},
        "roles": by_role,
        "campaignsScanned": campaigns,
        "generatedAtUtc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "consumedFE": 0, "changedAlgorithm": False,
    }
    with open(os.path.join(OUT_DIR, "role-registry-summary.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2, ensure_ascii=False)
    print(json.dumps(summary, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
