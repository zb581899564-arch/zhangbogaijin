#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Build per-directory evidence ledgers and the remote path map."""
import hashlib
import os

BASE = os.path.dirname(os.path.abspath(__file__))
BASE = os.path.dirname(BASE)  # the script lives in tools/
REPO = os.path.dirname(os.path.dirname(os.path.dirname(BASE)))
R = "/home/inspur/aicomp/zhangbo-v35-gap-probe-v2-20260830"


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def ledger(root, exclude_self=True):
    entries = []
    for dirpath, dirnames, filenames in os.walk(root):
        for name in filenames:
            full = os.path.join(dirpath, name)
            rel = os.path.relpath(full, root).replace("\\", "/")
            if exclude_self and rel == "evidence-sha256.tsv":
                continue
            entries.append((rel, sha256_file(full), os.path.getsize(full)))
    entries.sort()
    out = os.path.join(root, "evidence-sha256.tsv")
    with open(out, "w", encoding="utf-8", newline="") as f:
        f.write("# per-directory evidence ledger; reverse-verify individually\n")
        f.write("sha256\tpath\tbytes\n")
        for rel, digest, size in entries:
            f.write("%s\t%s\t%d\n" % (digest, rel, size))
    return len(entries)


def main():
    counts = {}
    for d in ("03-v2-remote-20k-shakedown", "04-v2-remote-500k-runs",
              "05-v2-analysis", "06-v2-decision"):
        counts[d] = ledger(os.path.join(BASE, d))
    print(counts)

    # remote path map
    rows = [("inputs/java-jmetal58/EADHFSP/50_2_3_1.txt",
             "java-jmetal58/EADHFSP/50_2_3_1.txt"),
            ("inputs/java-jmetal58/EADHFSP/100_5_3_1.txt",
             "java-jmetal58/EADHFSP/100_5_3_1.txt"),
            ("inputs/java-jmetal58/instance-extensions/v1/50_2_3_1.setup.txt",
             "java-jmetal58/instance-extensions/v1/50_2_3_1.setup.txt"),
            ("inputs/java-jmetal58/instance-extensions/v1/100_5_3_1.setup.txt",
             "java-jmetal58/instance-extensions/v1/100_5_3_1.setup.txt"),
            ("inputs/java-jmetal58/fatigue-parameters/v1/50_2_3_1.fatigue.txt",
             "java-jmetal58/fatigue-parameters/v1/50_2_3_1.fatigue.txt"),
            ("inputs/java-jmetal58/fatigue-parameters/v1/100_5_3_1.fatigue.txt",
             "java-jmetal58/fatigue-parameters/v1/100_5_3_1.fatigue.txt"),
            ("snapshots/50_2_3_1-seed-20260827.fourvec",
             "docs/evidence/V35-FORMAL-MANIFEST/initial-populations/50_2_3_1/seed-20260827.fourvec"),
            ("snapshots/100_5_3_1-seed-20260827.fourvec",
             "docs/evidence/V35-FORMAL-MANIFEST/initial-populations/100_5_3_1/seed-20260827.fourvec"),
            ("snapshots/50_2_3_1-seed-20260906.fourvec",
             "docs/evidence/V35-PFC5-GAP-PROBE/tools/snapshots-local/50_2_3_1-seed-20260906.fourvec"),
            ("snapshots/100_5_3_1-seed-20260906.fourvec",
             "docs/evidence/V35-PFC5-GAP-PROBE/tools/snapshots-local/100_5_3_1-seed-20260906.fourvec"),
            ("jars/formal-algorithm-8DAD8F40.jar",
             "docs/evidence/V35-FC5-MIDHORIZON-250K/00-preregistration/runtime/formal-algorithm-8DAD8F40.jar"),
            ("jars/external-fair-baseline-comparison-966da3d2.jar",
             "docs/evidence/V35-EXTERNAL-BASELINE-PRODUCTION-PREFLIGHT/01-launcher-hardening/external-fair-baseline-comparison-preflight-966da3d2.jar"),
            ("jars/gap-probe-arm-launcher-v2.jar",
             "docs/evidence/V35-PFC5-GAP-PROBE/tools/gap-probe-arm-launcher-v2.jar")]
    for arm in ("A4", "A0"):
        for seed in ("20260827", "20260906"):
            rows.append(("20k-shakedown/run-GAP20K-%s" % arm,
                         "docs/evidence/V35-PFC5-GAP-PROBE/03-v2-remote-20k-shakedown/run-GAP20K-%s" % arm))
    for inst in ("50_2_3_1", "100_5_3_1"):
        for seed in ("20260827", "20260906"):
            for arm in ("A4", "A0", "SPEA2F", "NSGA2F"):
                rows.append(("500k-runs/run-GAP500-%s-%s-%s" % (arm, inst, seed),
                             "docs/evidence/V35-PFC5-GAP-PROBE/04-v2-remote-500k-runs/sync/run-GAP500-%s-%s-%s" % (arm, inst, seed)))
    rows.append(("plans/ (10 plan files)", ""))
    out = os.path.join(BASE, "04-v2-remote-500k-runs", "remote-path-map.csv")
    with open(out, "w", encoding="utf-8", newline="") as f:
        f.write("remotePath (under %s),localSourceOrSync\n" % R)
        for r, l in rows:
            f.write("%s,%s\n" % (r, l))
    print("remote-path-map rows:", len(rows))


if __name__ == "__main__":
    main()
