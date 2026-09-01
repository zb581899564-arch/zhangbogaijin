#!/usr/bin/env bash
# FC-6B only.  The order was frozen by FC-6A.4 as CATA_THEN_INHERITED.
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
  echo "FC-6B already completed: $OUT" >&2
  exit 1
fi

run_arm() {
  local cpu="$1" phase="$2" instance="$3" seed="$4"
  taskset -c "$cpu" java -Xmx4g -cp "$APP" "$RUNNER" \
    --phase "$phase" --instance "$instance" --seed "$seed" \
    --project-root "$PROJECT" --output "$OUT"
}

run_seed() {
  local cpu="$1" seed="$2"
  run_arm "$cpu" REGION_GLOBAL 20_2_3_1 "$seed"
  run_arm "$cpu" REGION_AWARE 20_2_3_1 "$seed"
  run_arm "$cpu" REGION_GLOBAL 100_2_3_1 "$seed"
  run_arm "$cpu" REGION_AWARE 100_2_3_1 "$seed"
}

run_seed 0-3 20260822 >"$OUT/logs/seed-20260822.log" 2>&1 & pid22=$!
run_seed 4-7 20260823 >"$OUT/logs/seed-20260823.log" 2>&1 & pid23=$!
run_seed 8-11 20260824 >"$OUT/logs/seed-20260824.log" 2>&1 & pid24=$!
wait "$pid22"
wait "$pid23"
wait "$pid24"

for instance in 20_2_3_1 100_2_3_1; do
  for seed in 20260822 20260823 20260824; do
    for phase in region_global region_aware; do
      status="$OUT/runs/${phase}-${instance}-seed-${seed}/status.properties"
      grep -qx 'status=COMPLETED' "$status"
      grep -qx 'fullEvaluations=500000' "$status"
    done
  done
done

taskset -c 0-3 java -Xmx4g -cp "$APP" "$REPORT" --kind REGION_CURRENT \
  --instance 20_2_3_1 --runs-root "$OUT" --output "$OUT/report-20" \
  >"$OUT/logs/report-20.log" 2>&1
taskset -c 0-3 java -Xmx4g -cp "$APP" "$REPORT" --kind REGION_CURRENT \
  --instance 100_2_3_1 --runs-root "$OUT" --output "$OUT/report-100" \
  >"$OUT/logs/report-100.log" 2>&1
date -Iseconds > "$OUT/ALL-COMPLETED.txt"
