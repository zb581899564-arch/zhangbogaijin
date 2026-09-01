#!/usr/bin/env bash
# V35-FC5-T second-tier 100k screening experiment: input preparation + hard-gate verification.
# Authoritative inputs are copied from the read-only 50k directory.
set -euo pipefail

ROOT=/home/inspur/aicomp/zhangbo-v35-fc5-transfer-100k-20260825
SRC=/home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825        # READ-ONLY authoritative 50k dir
INST=100_5_3_1
SEEDS="20260901 20260902 20260903"
JAR="$SRC/bin/fc5-transfer-diagnostic.jar"
EXPECTED_JAR_SHA=E59698030AF2215994D4FD179AA2B1F26787A0F1239628543339477E119FA8B5

# ---- refuse if root already exists (never disturb an existing experiment) ----
if [[ -e "$ROOT" ]]; then
  echo "FATAL: new root already exists: $ROOT" >&2
  exit 3
fi

mkdir -p \
  "$ROOT/bin" \
  "$ROOT/project-root/java-jmetal58/EADHFSP" \
  "$ROOT/project-root/java-jmetal58/instance-extensions/v1" \
  "$ROOT/project-root/java-jmetal58/fatigue-parameters/v1" \
  "$ROOT/input/snapshots/$INST" \
  "$ROOT/logs"

# ---- 1) diagnostic jar ----
cp -p "$JAR" "$ROOT/bin/fc5-transfer-diagnostic.jar"

# ---- 2) instance / setup / fatigue (EADHFSP data) ----
cp -p "$SRC/project-root/java-jmetal58/EADHFSP/$INST.txt" \
  "$ROOT/project-root/java-jmetal58/EADHFSP/$INST.txt"
cp -p "$SRC/project-root/java-jmetal58/instance-extensions/v1/$INST.setup.txt" \
  "$ROOT/project-root/java-jmetal58/instance-extensions/v1/$INST.setup.txt"
cp -p "$SRC/project-root/java-jmetal58/fatigue-parameters/v1/$INST.fatigue.txt" \
  "$ROOT/project-root/java-jmetal58/fatigue-parameters/v1/$INST.fatigue.txt"

# ---- 3) per-seed .fourvec snapshots + receipts (one snapshot per seed shared by A2/A4) ----
for s in $SEEDS; do
  cp -p "$SRC/input/snapshots/$INST/seed-$s.fourvec" \
    "$ROOT/input/snapshots/$INST/seed-$s.fourvec"
  cp -p "$SRC/input/snapshots/$INST/seed-$s.fourvec.receipt.properties" \
    "$ROOT/input/snapshots/$INST/seed-$s.fourvec.receipt.properties"
done

# ---- hard gate: jar sha must match the expected diagnostic jar ----
JAR_SHA=$(sha256sum "$ROOT/bin/fc5-transfer-diagnostic.jar" | awk '{print $1}')
if [[ "${JAR_SHA^^}" != "$EXPECTED_JAR_SHA" ]]; then
  echo "FATAL: jar sha mismatch: $JAR_SHA" >&2
  exit 4
fi

# ---- write RUNNER_SHA256.txt (jar only) ----
printf 'sha256=%s\npath=%s\nbytes=%s\n' "$JAR_SHA" "$ROOT/bin/fc5-transfer-diagnostic.jar" \
  "$(stat -c %s "$ROOT/bin/fc5-transfer-diagnostic.jar")" > "$ROOT/RUNNER_SHA256.txt"

# ---- write INPUT_SHA256.tsv (path<TAB>sha256<TAB>bytes, relative path) ----
: > "$ROOT/INPUT_SHA256.tsv"
{
  for f in \
    "project-root/java-jmetal58/EADHFSP/$INST.txt" \
    "project-root/java-jmetal58/instance-extensions/v1/$INST.setup.txt" \
    "project-root/java-jmetal58/fatigue-parameters/v1/$INST.fatigue.txt"; do
    printf '%s\t%s\t%s\n' "$f" "$(sha256sum "$ROOT/$f" | awk '{print $1}')" "$(stat -c %s "$ROOT/$f")"
  done
  for s in $SEEDS; do
    for suffix in "" ".receipt.properties"; do
      f="input/snapshots/$INST/seed-$s.fourvec$suffix"
      printf '%s\t%s\t%s\n' "$f" "$(sha256sum "$ROOT/$f" | awk '{print $1}')" "$(stat -c %s "$ROOT/$f")"
    done
  done
} >> "$ROOT/INPUT_SHA256.tsv"

# ---- hard gate: each snapshot sha must equal its receipt's snapshotSha256 ----
for s in $SEEDS; do
  snap="$ROOT/input/snapshots/$INST/seed-$s.fourvec"
  receipt="$snap.receipt.properties"
  expected=$(sed -n 's/^snapshotSha256=//p' "$receipt")
  actual=$(sha256sum "$snap" | awk '{print $1}')
  if [[ "$actual" != "$expected" ]]; then
    echo "FATAL: snapshot sha mismatch for seed $s: got $actual expected $expected" >&2
    exit 5
  fi
done

# ---- hard gate: exactly one .fourvec snapshot per seed for this instance ----
for s in $SEEDS; do
  n=$(find "$ROOT/input/snapshots/$INST" -name "seed-$s.fourvec" | wc -l)
  if [[ "$n" != "1" ]]; then
    echo "FATAL: expected exactly 1 snapshot for seed $s, found $n" >&2
    exit 6
  fi
done

echo "=== PREPARE COMPLETE ==="
echo "ROOT=$ROOT"
echo "--- RUNNER_SHA256.txt ---"
cat "$ROOT/RUNNER_SHA256.txt"
echo "--- INPUT_SHA256.tsv ---"
cat "$ROOT/INPUT_SHA256.tsv"
