#!/usr/bin/env bash
# FC-6A.4 only: paired order experiment.  This script intentionally does not
# launch Region-aware selection; FC-6B is unlocked only by the ORDER report.
set -euo pipefail

ROOT="${1:?remote root required}"
APP="$ROOT/project/app.jar"
PROJECT="$ROOT/project"
OUT="$ROOT/results"
RUNNER="org.uma.jmetal.runner.lc_psode.ZhangBoV35Fc6Runner"
REPORT="org.uma.jmetal.runner.lc_psode.ZhangBoV35Fc6ReportRunner"

cd "$ROOT"
sha256sum -c upload-sha256.tsv
mkdir -p "$OUT/logs"

if [[ -e "$OUT/ALL-COMPLETED.txt" ]]; then
  echo "FC-6A.4 already completed: $OUT" >&2
  exit 1
fi

run_arm() {
  local cpu="$1" phase="$2" seed="$3"
  taskset -c "$cpu" java -Xmx4g -cp "$APP" "$RUNNER" \
    --phase "$phase" --instance 20_2_3_1 --seed "$seed" \
    --project-root "$PROJECT" --output "$OUT"
}

# Each seed has two isolated JVMs.  The phase order is rotated only to avoid a
# persistent host-level warm-cache advantage; the scientific arm is phase.
(
  run_arm 0-3 ORDER_CURRENT 20260822
  run_arm 0-3 ORDER_SWAP 20260822
) >"$OUT/logs/seed-20260822.log" 2>&1 & pid22=$!
(
  run_arm 4-7 ORDER_SWAP 20260823
  run_arm 4-7 ORDER_CURRENT 20260823
) >"$OUT/logs/seed-20260823.log" 2>&1 & pid23=$!
(
  run_arm 8-11 ORDER_CURRENT 20260824
  run_arm 8-11 ORDER_SWAP 20260824
) >"$OUT/logs/seed-20260824.log" 2>&1 & pid24=$!

wait "$pid22"
wait "$pid23"
wait "$pid24"

for seed in 20260822 20260823 20260824; do
  for phase in order_current order_swap; do
    status="$OUT/runs/${phase}-20_2_3_1-seed-${seed}/status.properties"
    grep -qx 'status=COMPLETED' "$status"
    grep -qx 'fullEvaluations=500000' "$status"
  done
done

taskset -c 0-3 java -Xmx4g -cp "$APP" "$REPORT" --kind ORDER \
  --instance 20_2_3_1 --runs-root "$OUT" --output "$OUT/report" \
  >"$OUT/logs/report.log" 2>&1
date -Iseconds > "$OUT/ALL-COMPLETED.txt"
