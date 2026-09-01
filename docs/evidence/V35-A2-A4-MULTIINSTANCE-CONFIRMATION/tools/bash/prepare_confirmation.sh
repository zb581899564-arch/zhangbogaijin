#!/usr/bin/env bash
# Prepare the pre-registered A2/A4 confirmation without touching the frozen Stage2 tree.
set -euo pipefail

if [[ "$#" != 2 ]]; then
  echo "usage: $0 <confirmation-root> <stage2-root>" >&2
  exit 64
fi

ROOT="$(realpath "$1")"
STAGE2="$(realpath "$2")"
JAR="$STAGE2/tool/frozen-algorithm.jar"
EXTERNAL_CLASSES="$STAGE2/tool/classes"
CONFIRMATION_CLASSES="$ROOT/tool/confirmation-classes"
JAVA_PROJECT="$STAGE2/input/java-project"
REGISTRY="$ROOT/input/00-preregistration/instance-seed-registry.csv"
INSTANCE_AUDIT="$ROOT/input/00-preregistration/instance-eligibility-audit.csv"
PROFILES="$ROOT/input/profile-registry.csv"
SNAPSHOTS="$ROOT/input/snapshots"
PLAN_ROOT="$ROOT/plans"
MANIFEST_ROOT="$ROOT/manifests"
OUTPUT_ROOT="$ROOT/results"
TOOLS="$ROOT/tools"
EXPECTED_JAR="8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9"

for item in "$JAR" "$EXTERNAL_CLASSES" "$CONFIRMATION_CLASSES" "$JAVA_PROJECT" "$REGISTRY" "$INSTANCE_AUDIT"; do
  [[ -e "$item" ]] || { echo "missing required input: $item" >&2; exit 65; }
done
[[ "$(sha256sum "$JAR" | awk '{print toupper($1)}')" == "$EXPECTED_JAR" ]] || {
  echo "frozen jar SHA-256 mismatch" >&2; exit 66;
}

python3 - "$INSTANCE_AUDIT" "$JAVA_PROJECT" <<'PY'
import csv, hashlib, pathlib, sys
audit, project = map(pathlib.Path, sys.argv[1:])
with audit.open(encoding="utf-8-sig", newline="") as f:
    rows = list(csv.DictReader(f))
if len(rows) != 6:
    raise SystemExit("expected six held-out instance audit rows")
for row in rows:
    path = project / "EADHFSP" / (row["instance"] + ".txt")
    actual = hashlib.sha256(path.read_bytes()).hexdigest().upper()
    expected = row["source_sha256"].upper()
    if actual != expected:
        raise SystemExit("instance SHA-256 mismatch: %s" % path)
print("INSTANCE_PROVENANCE_VERIFIED rows=6")
PY

mkdir -p "$ROOT/input" "$SNAPSHOTS" "$PLAN_ROOT" "$MANIFEST_ROOT" "$OUTPUT_ROOT" "$ROOT/status"

PROFILE_NEW="$ROOT/input/.profile-registry.new.csv"
java -cp "$CONFIRMATION_CLASSES:$EXTERNAL_CLASSES:$JAR" \
  org.uma.jmetal.runner.lc_psode.V35ProfileRegistryPrinter \
  --seeds 20260901,20260902,20260903,20260904,20260905 --max-fes 500000 --output "$PROFILE_NEW"
if [[ -e "$PROFILES" ]]; then
  cmp -- "$PROFILE_NEW" "$PROFILES" || { echo "profile registry drift" >&2; exit 67; }
  rm -- "$PROFILE_NEW"
else
  mv -- "$PROFILE_NEW" "$PROFILES"
fi

for instance in 20_2_4_1 20_5_3_1 50_2_4_1 50_5_3_1 100_2_4_1 100_5_3_1; do
  for seed in 20260901 20260902 20260903 20260904 20260905; do
    snapshot="$SNAPSHOTS/$instance/seed-$seed.fourvec"
    receipt="$snapshot.receipt.properties"
    if [[ -e "$snapshot" || -e "$receipt" ]]; then
      [[ -f "$snapshot" && -f "$receipt" ]] || { echo "partial snapshot: $snapshot" >&2; exit 68; }
      expected="$(awk -F= '$1=="snapshotSha256" {print $2}' "$receipt")"
      actual="$(sha256sum "$snapshot" | awk '{print tolower($1)}')"
      [[ "$actual" == "$expected" ]] || { echo "snapshot SHA-256 mismatch: $snapshot" >&2; exit 69; }
    else
      java -cp "$CONFIRMATION_CLASSES:$EXTERNAL_CLASSES:$JAR" \
        org.uma.jmetal.runner.lc_psode.V35ConfirmationSnapshotMaterializer \
        --project-root "$JAVA_PROJECT" --instance "$instance" --seed "$seed" --output "$snapshot"
    fi
  done
done

for item in "$MANIFEST_ROOT/first.json" "$MANIFEST_ROOT/remainder.json"; do
  [[ ! -e "$item" ]] || { echo "refusing to overwrite rendered manifest: $item" >&2; exit 70; }
done
python3 "$TOOLS/python/render_confirmation.py" \
  --registry "$REGISTRY" --java-project "$JAVA_PROJECT" --frozen-jar "$JAR" \
  --external-classes "$EXTERNAL_CLASSES" --confirmation-classes "$CONFIRMATION_CLASSES" \
  --snapshot-root "$SNAPSHOTS" --profiles "$PROFILES" --plan-root "$PLAN_ROOT/first" \
  --output-root "$OUTPUT_ROOT" --manifest "$MANIFEST_ROOT/first.json" --phase FIRST
python3 "$TOOLS/python/render_confirmation.py" \
  --registry "$REGISTRY" --java-project "$JAVA_PROJECT" --frozen-jar "$JAR" \
  --external-classes "$EXTERNAL_CLASSES" --confirmation-classes "$CONFIRMATION_CLASSES" \
  --snapshot-root "$SNAPSHOTS" --profiles "$PROFILES" --plan-root "$PLAN_ROOT/remainder" \
  --output-root "$OUTPUT_ROOT" --manifest "$MANIFEST_ROOT/remainder.json" --phase REMAINDER
python3 "$TOOLS/python/confirmation_master.py" --manifest "$MANIFEST_ROOT/first.json" \
  --progress "$ROOT/status/first-dry-run.csv" --groups-per-wave 1 --dry-run
python3 "$TOOLS/python/confirmation_master.py" --manifest "$MANIFEST_ROOT/remainder.json" \
  --progress "$ROOT/status/remainder-dry-run.csv" --groups-per-wave 8 --dry-run

cat > "$ROOT/status/preparation.properties" <<EOF
schema=v35-a2-a4-confirmation-preparation-v1
status=PREPARED
frozenJarSha256=$EXPECTED_JAR
snapshotCount=30
firstManifestRuns=2
remainderManifestRuns=58
formalMatrixTouched=false
EOF
echo "CONFIRMATION_PREPARATION_COMPLETED root=$ROOT"
