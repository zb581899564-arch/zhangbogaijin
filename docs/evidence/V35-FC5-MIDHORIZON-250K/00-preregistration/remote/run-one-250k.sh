#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 6 ]]; then
  echo "usage: run-one-250k.sh ROOT INSTANCE SEED ARM RUN_ID CPUSET" >&2
  exit 2
fi

ROOT="$1"
INSTANCE="$2"
SEED="$3"
ARM="$4"
RUN_ID="$5"
CPUSET="$6"

case "$INSTANCE" in
  100_2_4_1|100_5_3_1) ;;
  *) echo "unsupported instance: $INSTANCE" >&2; exit 3 ;;
esac
case "$SEED" in
  20260901|20260902|20260903) ;;
  *) echo "unsupported seed: $SEED" >&2; exit 3 ;;
esac
case "$ARM" in
  A2|A4) ;;
  *) echo "unsupported arm: $ARM" >&2; exit 3 ;;
esac

RUNTIME_JAR="$ROOT/00-preregistration/runtime/diagnostic-runtime-A0A1E74D.jar"
LAUNCHER_JAR="$ROOT/00-preregistration/tools/V35MidHorizon250kExternalRunner.jar"
PROJECT_ROOT="$ROOT/00-preregistration/inputs/java-project"
OUTPUT_DIR="$ROOT/output/250k/$INSTANCE/seed-$SEED/$ARM"
PID_FILE="$ROOT/pids/$RUN_ID.pid"

mkdir -p "$OUTPUT_DIR" "$ROOT/pids"
if [[ -e "$OUTPUT_DIR/behavior-summary.properties" || -e "$OUTPUT_DIR/status.properties" ]]; then
  echo "refusing to overwrite existing output: $OUTPUT_DIR" >&2
  exit 4
fi

echo "$$" > "$PID_FILE"
exec taskset -c "$CPUSET" java -Xmx4g \
  -cp "$LAUNCHER_JAR:$RUNTIME_JAR" \
  v35campaign.V35MidHorizon250kExternalRunner \
  --project-root "$PROJECT_ROOT" \
  --output "$OUTPUT_DIR" \
  --instance "$INSTANCE" \
  --seed "$SEED" \
  --arm "$ARM" \
  --max-fes 250000 \
  --telemetry ON \
  --run-id "$RUN_ID" \
  --jar-sha256 A0A1E74D00403CAC69FBC25B52AEAEB454A6CC2D9FA6BF2A1F6A0D12FFE15FF7 \
  --checkpoints 25000,50000,75000,100000,125000,150000,175000,200000,225000,250000
