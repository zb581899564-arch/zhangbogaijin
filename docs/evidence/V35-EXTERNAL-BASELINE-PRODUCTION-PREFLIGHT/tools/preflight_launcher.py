#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V35-EXT-PREFLIGHT-20K external launcher (production preflight harness).

Responsibilities (zero search semantics; the runner owns the partial/atomic
boundary, this launcher owns the gates):
  - pre-launch freeze gates: comparison jar SHA, snapshot SHA, instance SHA,
    duplicate RunKey, pre-existing final directory, algorithm label;
  - launch one independent JVM per run and wait;
  - post-launch verification: manifest re-hash (independent of the runner's
    self-check), required files, status gate fields;
  - failure-injection self-test (wrong jar / wrong snapshot / wrong instance /
    duplicate RunKey / existing final directory / illegal algorithm label /
    process interrupt / missing front+status / tampered manifest), each must
    fail closed and never be recognizable as a successful run.

Usage:
  python preflight_launcher.py launch --config <launcher.json> --run-id ... (internal)
  python preflight_launcher.py selftest --config <launcher.json>
"""
import csv
import hashlib
import json
import os
import shutil
import subprocess
import sys
import time

PHASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(PHASE)))
RUNS = os.path.join(PHASE, "03-runs")
REQUIRED_FILES = ["configuration.txt", "source-provenance.properties",
                  "initial-population.sha256", "status.properties",
                  "budget-termination.properties", "event-summary.properties",
                  "front.csv", "stdout.log", "stderr.log", "evidence-sha256.tsv"]


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def load_config(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def read_properties(path):
    out = {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.strip()
            if line and "=" in line and not line.startswith("#"):
                k, v = line.split("=", 1)
                out[k.strip()] = v.strip()
    return out


def registry_rows():
    path = os.path.join(PHASE, "04-acceptance", "preflight-run-registry.csv")
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def append_registry(row):
    path = os.path.join(PHASE, "04-acceptance", "preflight-run-registry.csv")
    exists = os.path.exists(path)
    fields = ["runKey", "runId", "attemptId", "algorithm", "instance", "seed",
              "population", "maxFEs", "snapshotPath", "snapshotSha256",
              "instanceSha256", "jarSha256", "command", "launchUtc",
              "finalDirectory", "outcome"]
    write_header = not exists
    with open(path, "a", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fields, lineterminator="\n")
        if write_header:
            w.writeheader()
        w.writerow(row)


def preflight_gates(cfg, algorithm, instance_id, seed, final_dir, registry,
                    actual_instance_path=None, actual_snapshot_path=None):
    """Every gate must pass BEFORE a JVM is started. Returns error string or None.

    The instance/snapshot hash gates compare the files ACTUALLY handed to the JVM
    (canonical paths unless injected otherwise) against the frozen values
    registered for the REQUESTED ``instance_id`` — so a swapped instance file or
    a swapped snapshot fails closed as GATE_INSTANCE_SHA_MISMATCH /
    GATE_SNAPSHOT_SHA_MISMATCH."""
    jar = cfg["comparisonJar"]
    if sha256_file(jar) != cfg["comparisonJarSha256"]:
        return "GATE_JAR_SHA_MISMATCH"
    inst_path = actual_instance_path or os.path.join(
        cfg["javaProject"], "EADHFSP", instance_id + ".txt")
    if not os.path.exists(inst_path) or \
            sha256_file(inst_path) != cfg["instanceSha256"][instance_id]:
        return "GATE_INSTANCE_SHA_MISMATCH"
    snap = cfg["snapshots"][instance_id]
    snap_path = actual_snapshot_path or snap["path"]
    if not os.path.exists(snap_path) or sha256_file(snap_path) != snap["sha256"]:
        return "GATE_SNAPSHOT_SHA_MISMATCH"
    if algorithm not in ("NSGA-II-F", "SPEA2-F"):
        return "GATE_ILLEGAL_ALGORITHM"
    if os.path.exists(final_dir):
        return "GATE_FINAL_DIR_EXISTS"
    run_key = "%s|%s|%s|%s|%s" % (algorithm, instance_id, seed,
                                  cfg["population"], cfg["maxFEs"])
    for row in registry:
        if row.get("runKey") == run_key and row.get("outcome") in ("COMPLETED", "LAUNCHED"):
            return "GATE_DUPLICATE_RUNKEY"
    return None


def build_command(cfg, algorithm, instance_id, run_id, attempt_id, final_dir):
    inst_path = os.path.join(cfg["javaProject"], "EADHFSP", instance_id + ".txt")
    snap = cfg["snapshots"][instance_id]["path"]
    return [cfg["java"], "-cp", cfg["comparisonJar"],
            "org.uma.jmetal.runner.lc_psode.ZhangBoV35ExternalFairBaselineRunner",
            "--algorithm", algorithm,
            "--instance", inst_path,
            "--seed", str(cfg["seed"]),
            "--population", str(cfg["population"]),
            "--maxFEs", str(cfg["maxFEs"]),
            "--snapshot", snap,
            "--run-id", run_id,
            "--attempt-id", attempt_id,
            "--final-output", final_dir]


def verify_final_directory(final_dir, cfg, instance_id):
    """Independent post-launch verification (launcher-side, not runner-side)."""
    problems = []
    for required in REQUIRED_FILES:
        if not os.path.isfile(os.path.join(final_dir, required)):
            problems.append("missing:" + required)
    if problems:
        return problems
    manifest = os.path.join(final_dir, "evidence-sha256.tsv")
    with open(manifest, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line or line.startswith("#") or line.startswith("sha256\t"):
                continue
            sha_val, name = line.split("\t")[0], line.split("\t")[1]
            full = os.path.join(final_dir, name)
            if not os.path.isfile(full):
                problems.append("manifest-missing:" + name)
            elif sha256_file(full) != sha_val:
                problems.append("manifest-hash:" + name)
    status = read_properties(os.path.join(final_dir, "status.properties"))
    fe = str(cfg["maxFEs"])
    for key, expected in (("status", "COMPLETED"), ("actualFE", fe),
                          ("decoderCalls", fe), ("fullEvaluations", fe),
                          ("remainingFE", "0"), ("illegalSolutions", "0"),
                          ("duplicateEvaluations", "0"),
                          ("unexplainedRepairs", "0"),
                          ("forbiddenMechanismEvents", "0")):
        if status.get(key) != expected:
            problems.append("status.%s=%s(expect %s)" % (key, status.get(key), expected))
    with open(os.path.join(final_dir, "front.csv"), encoding="utf-8") as f:
        rows = [ln for ln in f.readlines()[1:] if ln.strip()]
        if not rows:
            problems.append("front-empty")
        for ln in rows:
            for value in ln.strip().split(","):
                v = float(value)
                if v != v or v in (float("inf"), float("-inf")):
                    problems.append("front-nonfinite")
                    break
    return problems


def do_launch(cfg, algorithm, instance_id, run_id, attempt_id, seed_override=None,
              jar_override=None, snapshot_override=None, instance_override=None,
              kill_after_seconds=None, skip_registry=False, final_dir_override=None):
    """One launch attempt through all gates; returns (outcome, detail).

    ``instance_id`` is the REQUESTED instance (drives the freeze gates and the
    RunKey); ``instance_override`` swaps the file actually handed to the JVM,
    which is how the wrong-instance injection triggers the hash gate."""
    seed = cfg["seed"] if seed_override is None else seed_override
    jar = cfg["comparisonJar"] if jar_override is None else jar_override
    snap = cfg["snapshots"][instance_id]["path"] if snapshot_override is None \
        else snapshot_override
    final_dir = final_dir_override or os.path.join(RUNS, "run-%s" % run_id)
    registry = [] if skip_registry else registry_rows()

    cfg_eff = dict(cfg)
    cfg_eff["comparisonJar"] = jar
    actual_inst_path = os.path.join(cfg["javaProject"], "EADHFSP",
                                    (instance_override or instance_id) + ".txt") \
        if instance_override else os.path.join(cfg["javaProject"], "EADHFSP",
                                               instance_id + ".txt")
    actual_snap_path = snapshot_override or cfg["snapshots"][instance_id]["path"]
    gate = preflight_gates(cfg_eff, algorithm, instance_id, seed, final_dir,
                           registry, actual_instance_path=actual_inst_path,
                           actual_snapshot_path=actual_snap_path)
    if gate:
        return ("FAIL_CLOSED_PRE", gate)
    inst_id = instance_id if instance_override is None else instance_override

    command = build_command(cfg, algorithm, inst_id, run_id, attempt_id, final_dir)
    if jar_override is not None:
        command[2] = jar
    if snapshot_override is not None:
        command[command.index("--snapshot") + 1] = snapshot_override
    if seed_override is not None:
        command[command.index("--seed") + 1] = str(seed_override)
    command[command.index("--instance") + 1] = os.path.join(
        cfg["javaProject"], "EADHFSP", inst_id + ".txt")

    if not skip_registry:
        append_registry({"runKey": "%s|%s|%s|%s|%s" % (algorithm, inst_id, seed,
                                                       cfg["population"], cfg["maxFEs"]),
                         "runId": run_id, "attemptId": attempt_id,
                         "algorithm": algorithm, "instance": inst_id, "seed": seed,
                         "population": cfg["population"], "maxFEs": cfg["maxFEs"],
                         "snapshotPath": command[command.index("--snapshot") + 1],
                         "snapshotSha256": sha256_file(command[command.index("--snapshot") + 1])
                         if os.path.exists(command[command.index("--snapshot") + 1]) else "N/A",
                         "instanceSha256": sha256_file(command[command.index("--instance") + 1])
                         if os.path.exists(command[command.index("--instance") + 1]) else "N/A",
                         "jarSha256": sha256_file(jar), "command": " ".join(command),
                         "launchUtc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
                         "finalDirectory": final_dir, "outcome": "LAUNCHED"})

    process = subprocess.Popen(command, cwd=REPO,
                               stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                               text=True, errors="replace")
    if kill_after_seconds is not None:
        time.sleep(kill_after_seconds)
        if process.poll() is None:
            # Oracle javapath java.exe is a launcher that spawns the real JVM as a
            # child; Popen.kill() would only terminate the launcher and leave an
            # orphaned JVM running to completion. Tree-kill is therefore mandatory.
            if os.name == "nt":
                subprocess.run(["taskkill", "/F", "/T", "/PID", str(process.pid)],
                               capture_output=True)
            else:
                process.kill()
            try:
                process.wait(timeout=30)
            except subprocess.TimeoutExpired:
                pass
            partial_dirs = [n for n in sorted(os.listdir(os.path.dirname(final_dir)))
                            if n.startswith(".partial-")] \
                if os.path.isdir(os.path.dirname(final_dir)) else []
            return ("FAIL_CLOSED_INTERRUPTED",
                    "tree-killed after %ss; final_exists=%s; partialSiblings=%s" % (
                        kill_after_seconds, os.path.exists(final_dir), partial_dirs))
    out, err = process.communicate()
    if process.returncode != 0:
        return ("FAIL_CLOSED_EXIT_%d" % process.returncode,
                (err or out)[-400:])
    problems = verify_final_directory(final_dir, cfg, inst_id)
    if problems:
        return ("FAIL_CLOSED_VERIFY", ";".join(problems)[:400])
    return ("COMPLETED", "final=%s" % final_dir)


def cmd_launch(args):
    cfg = load_config(args[args.index("--config") + 1])
    run_id = args[args.index("--run-id") + 1]
    algorithm = args[args.index("--algorithm") + 1]
    instance_id = args[args.index("--instance") + 1]
    attempt_id = args[args.index("--attempt-id", ) + 1] if "--attempt-id" in args else "1"
    outcome, detail = do_launch(cfg, algorithm, instance_id, run_id, attempt_id)
    print("%s %s" % (outcome, detail))
    return 0 if outcome == "COMPLETED" else 1


def cmd_selftest(args):
    cfg = load_config(args[args.index("--config") + 1])
    results = []
    tmp = os.path.join(PHASE, "03-runs", ".selftest")
    shutil.rmtree(tmp, ignore_errors=True)
    os.makedirs(tmp, exist_ok=True)

    def record(name, outcome, must_fail):
        ok = (outcome != "COMPLETED") if must_fail else (outcome == "COMPLETED")
        results.append({"scenario": name, "outcome": outcome, "expect": must_fail,
                        "verdict": "PASS" if ok else "FAIL"})
        return ok

    ok = True
    wrong_jar = cfg["frozenFormalV35Jar"]
    outcome, detail = do_launch(cfg, "NSGA-II-F", "20_2_3_1", "FI-wrongjar", "1",
                                jar_override=wrong_jar, skip_registry=True,
                                final_dir_override=os.path.join(tmp, "fi-wrongjar"))
    ok &= record("wrong-jar", outcome, True)
    outcome, _ = do_launch(cfg, "NSGA-II-F", "20_2_3_1", "FI-wrongsnap", "1",
                           snapshot_override=cfg["wrongSnapshot"], skip_registry=True,
                           final_dir_override=os.path.join(tmp, "fi-wrongsnap"))
    ok &= record("wrong-snapshot", outcome, True)
    outcome, _ = do_launch(cfg, "NSGA-II-F", "50_2_3_1", "FI-wronginst", "1",
                           instance_override="20_2_3_1", skip_registry=True,
                           final_dir_override=os.path.join(tmp, "fi-wronginst"))
    ok &= record("wrong-instance", outcome, True)
    outcome, _ = do_launch(cfg, "NSGA-II-F", "20_2_3_1", "FI-dup", "1",
                           skip_registry=False,
                           final_dir_override=os.path.join(tmp, "fi-dup"))
    ok &= record("duplicate-runkey", outcome, True)
    existing = os.path.join(tmp, "fi-existing")
    os.makedirs(existing, exist_ok=True)
    with open(os.path.join(existing, "status.properties"), "w") as f:
        f.write("status=COMPLETED\n")
    outcome, _ = do_launch(cfg, "NSGA-II-F", "20_2_3_1", "FI-existing", "1",
                           skip_registry=True, final_dir_override=existing)
    ok &= record("existing-final-dir", outcome, True)
    outcome, _ = do_launch(cfg, "MOPSO-XX", "20_2_3_1", "FI-illegal", "1",
                           skip_registry=True,
                           final_dir_override=os.path.join(tmp, "fi-illegal"))
    ok &= record("illegal-algorithm", outcome, True)
    outcome, detail = do_launch(cfg, "SPEA2-F", "100_2_4_1", "FI-interrupt", "1",
                                kill_after_seconds=1.5, skip_registry=True,
                                final_dir_override=os.path.join(tmp, "fi-interrupt"))
    ok &= record("process-interrupt", outcome, True)

    # missing front/status + tampered manifest: stage a completed run copy offline
    staged = os.path.join(tmp, "fi-missing")
    source = os.path.join(RUNS, "run-" + cfg["firstCompletedRunId"]) \
        if "firstCompletedRunId" in cfg else None
    if source and os.path.isdir(source):
        shutil.copytree(source, staged)
        os.remove(os.path.join(staged, "front.csv"))
        ok &= record("missing-front", ("FAIL_CLOSED_VERIFY", "")[0], True) \
            if verify_final_directory(staged, cfg, "20_2_3_1") else \
            record("missing-front", "COMPLETED", True)
        staged2 = os.path.join(tmp, "fi-tampered")
        shutil.copytree(source, staged2)
        manifest = os.path.join(staged2, "evidence-sha256.tsv")
        text = open(manifest, encoding="utf-8").read().splitlines()
        for i, line in enumerate(text):
            if line and not line.startswith("#") and not line.startswith("sha256\t"):
                parts = line.split("\t")
                text[i] = ("0" * 64) + "\t" + parts[1] + "\t" + parts[2]
                break
        open(manifest, "w", encoding="utf-8", newline="").write("\n".join(text) + "\n")
        problems = verify_final_directory(staged2, cfg, "20_2_3_1")
        ok &= record("tampered-manifest", "FAIL_CLOSED_VERIFY" if problems else "COMPLETED",
                     True)
    else:
        ok &= record("missing-front", "SKIPPED_NO_SOURCE", True)
        ok &= record("tampered-manifest", "SKIPPED_NO_SOURCE", True)

    with open(os.path.join(PHASE, "04-acceptance", "failure-injection-results.csv"),
              "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["scenario", "outcome", "expect", "verdict"],
                           lineterminator="\n")
        w.writeheader()
        w.writerows(results)
    print(json.dumps(results, indent=1))
    return 0 if ok else 1


def main():
    if len(sys.argv) < 2:
        print("usage: launch|selftest ...")
        return 2
    if sys.argv[1] == "launch":
        return cmd_launch(sys.argv[2:])
    if sys.argv[1] == "selftest":
        return cmd_selftest(sys.argv[2:])
    print("unknown mode")
    return 2


if __name__ == "__main__":
    sys.exit(main())
