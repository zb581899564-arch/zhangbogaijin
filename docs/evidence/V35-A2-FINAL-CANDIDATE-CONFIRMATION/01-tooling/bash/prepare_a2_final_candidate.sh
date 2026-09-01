#!/usr/bin/env bash
# Prepare only the pre-registered A0/A2 candidate confirmation.  No formal matrix is touched.
set -euo pipefail

if [[ "$#" != 3 ]]; then
  echo "usage: $0 <candidate-root> <stage2-root> <max-fes>" >&2
  exit 64
fi

ROOT="$(realpath "$1")"
STAGE2="$(realpath "$2")"
MAX_FES="$3"
[[ "$MAX_FES" == 2000 || "$MAX_FES" == 500000 ]] || { echo "max-fes must be 2000 or 500000" >&2; exit 64; }

JAR="$STAGE2/tool/frozen-algorithm.jar"
EXTERNAL_CLASSES="$STAGE2/tool/classes"
CLASSES="$ROOT/tool/classes"
JAVA_PROJECT="$STAGE2/input/java-project"
REGISTRY="$ROOT/input/00-preregistration/instance-seed-registry.csv"
AUDIT="$ROOT/input/00-preregistration/instance-eligibility-audit.csv"
SNAPSHOTS="$ROOT/input/snapshots"
TOOLS="$ROOT/tools"
EXPECTED_JAR="8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9"
SOURCE_ROOT="$TOOLS/java/org/uma/jmetal/runner/lc_psode"
MATERIALIZER_SOURCE="$SOURCE_ROOT/V35A2FinalCandidateSnapshotMaterializer.java"
RUNNER_SOURCE="$SOURCE_ROOT/ZhangBoV35A2FinalCandidateArmRunner.java"
MATERIALIZER_CLASS="$CLASSES/org/uma/jmetal/runner/lc_psode/V35A2FinalCandidateSnapshotMaterializer.class"
RUNNER_CLASS="$CLASSES/org/uma/jmetal/runner/lc_psode/ZhangBoV35A2FinalCandidateArmRunner.class"

for item in "$JAR" "$EXTERNAL_CLASSES" "$JAVA_PROJECT" "$REGISTRY" "$AUDIT" "$MATERIALIZER_SOURCE" "$RUNNER_SOURCE"; do
  [[ -e "$item" ]] || { echo "missing required input: $item" >&2; exit 65; }
done
[[ "$(sha256sum "$JAR" | awk '{print toupper($1)}')" == "$EXPECTED_JAR" ]] || { echo "frozen jar SHA-256 mismatch" >&2; exit 66; }

if [[ -f "$MATERIALIZER_CLASS" && -f "$RUNNER_CLASS" ]]; then
  RUNNER_MODE="compiled"
  TOOL_MODE="JAVA8_COMPILED"
else
  RUNNER_MODE="java-source"
  TOOL_MODE="JAVA11_SOURCE_LAUNCHER"
fi

python3 - "$AUDIT" "$JAVA_PROJECT" <<'PY'
import csv, hashlib, pathlib, sys
audit, project = map(pathlib.Path, sys.argv[1:])
with audit.open(encoding="utf-8-sig", newline="") as stream:
    rows = list(csv.DictReader(stream))
if len(rows) != 6:
    raise SystemExit("expected six held-out audit rows")
for row in rows:
    instance = project / row["source_path"]
    setup = project / "instance-extensions" / "v1" / (row["instance"] + ".setup.txt")
    fatigue = project / "fatigue-parameters" / "v1" / (row["instance"] + ".fatigue.txt")
    for path, key in ((instance, "instance_sha256"), (setup, "setup_sha256"), (fatigue, "fatigue_sha256")):
        if hashlib.sha256(path.read_bytes()).hexdigest().upper() != row[key].upper():
            raise SystemExit("input SHA-256 mismatch: %s" % path)
print("A2_FINAL_CANDIDATE_INPUTS_VERIFIED rows=6")
PY

mkdir -p "$ROOT/input" "$SNAPSHOTS" "$ROOT/plans" "$ROOT/manifests" "$ROOT/results" "$ROOT/status"
PROFILES="$ROOT/input/profile-registry-${MAX_FES}.csv"
NEW_PROFILE="$ROOT/input/.profile-registry-${MAX_FES}.new.csv"
java -cp "$CLASSES:$EXTERNAL_CLASSES:$JAR" org.uma.jmetal.runner.lc_psode.V35ProfileRegistryPrinter \
  --seeds 20260911,20260912,20260913,20260914,20260915 --max-fes "$MAX_FES" --output "$NEW_PROFILE"
if [[ -e "$PROFILES" ]]; then
  cmp -- "$NEW_PROFILE" "$PROFILES" || { echo "profile registry drift" >&2; exit 67; }
  rm -- "$NEW_PROFILE"
else
  mv -- "$NEW_PROFILE" "$PROFILES"
fi

for instance in 20_2_5_1 20_8_3_1 50_2_5_1 50_8_3_1 100_2_5_1 100_8_3_1; do
  for seed in 20260911 20260912 20260913 20260914 20260915; do
    snapshot="$SNAPSHOTS/$instance/seed-$seed.fourvec"
    receipt="$snapshot.receipt.properties"
    if [[ -e "$snapshot" || -e "$receipt" ]]; then
      [[ -f "$snapshot" && -f "$receipt" ]] || { echo "partial snapshot: $snapshot" >&2; exit 68; }
      expected="$(awk -F= '$1=="snapshotSha256" {print $2}' "$receipt")"
      actual="$(sha256sum "$snapshot" | awk '{print tolower($1)}')"
      [[ "$actual" == "$expected" ]] || { echo "snapshot SHA-256 mismatch: $snapshot" >&2; exit 69; }
    else
      if [[ "$RUNNER_MODE" == "compiled" ]]; then
        java -cp "$CLASSES:$EXTERNAL_CLASSES:$JAR" org.uma.jmetal.runner.lc_psode.V35A2FinalCandidateSnapshotMaterializer \
          --project-root "$JAVA_PROJECT" --instance "$instance" --seed "$seed" --output "$snapshot"
      else
        java -cp "$EXTERNAL_CLASSES:$JAR" "$MATERIALIZER_SOURCE" \
          --project-root "$JAVA_PROJECT" --instance "$instance" --seed "$seed" --output "$snapshot"
      fi
    fi
  done
done

if [[ "$MAX_FES" == 2000 ]]; then
  PAIRS="20_2_5_1:20260911,100_8_3_1:20260911"
  KIND="preflight"
else
  PAIRS="20_2_5_1:20260911,20_2_5_1:20260912,20_2_5_1:20260913,20_2_5_1:20260914,20_2_5_1:20260915,20_8_3_1:20260911,20_8_3_1:20260912,20_8_3_1:20260913,20_8_3_1:20260914,20_8_3_1:20260915,50_2_5_1:20260911,50_2_5_1:20260912,50_2_5_1:20260913,50_2_5_1:20260914,50_2_5_1:20260915,50_8_3_1:20260911,50_8_3_1:20260912,50_8_3_1:20260913,50_8_3_1:20260914,50_8_3_1:20260915,100_2_5_1:20260911,100_2_5_1:20260912,100_2_5_1:20260913,100_2_5_1:20260914,100_2_5_1:20260915,100_8_3_1:20260911,100_8_3_1:20260912,100_8_3_1:20260913,100_8_3_1:20260914,100_8_3_1:20260915"
  KIND="confirmation"
fi
MANIFEST="$ROOT/manifests/$KIND.json"
[[ ! -e "$MANIFEST" ]] || { echo "refusing to overwrite manifest: $MANIFEST" >&2; exit 70; }
python3 "$TOOLS/python/render_a2_final_candidate.py" --registry "$REGISTRY" --java-project "$JAVA_PROJECT" \
  --frozen-jar "$JAR" --external-classes "$EXTERNAL_CLASSES" --candidate-classes "$CLASSES" \
  --candidate-source-root "$TOOLS/java" --runner-mode "$RUNNER_MODE" \
  --snapshot-root "$SNAPSHOTS" --profiles "$PROFILES" --plan-root "$ROOT/plans/$KIND" \
  --output-root "$ROOT/results/$KIND" --manifest "$MANIFEST" --max-fes "$MAX_FES" --pairs "$PAIRS"
python3 "$TOOLS/python/a2_final_candidate_master.py" --manifest "$MANIFEST" \
  --progress "$ROOT/status/$KIND-dry-run.csv" --groups-per-wave 2 --dry-run
cat > "$ROOT/status/prepared-$KIND.properties" <<EOF
schema=v35-a2-final-candidate-preparation-v1
status=PREPARED
kind=$KIND
maxFEs=$MAX_FES
frozenJarSha256=$EXPECTED_JAR
formalMatrixTouched=false
externalToolExecutionMode=$TOOL_MODE
EOF
echo "A2_FINAL_CANDIDATE_PREPARED kind=$KIND root=$ROOT"
