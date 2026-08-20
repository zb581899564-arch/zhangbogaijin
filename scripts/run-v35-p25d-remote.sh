#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:?remote project root required}"
OUT="$ROOT/results"
APP="$ROOT/project/app.jar"
MAIN="org.uma.jmetal.runner.lc_psode.ZhangBoV35P25DRunner"
REPORT="org.uma.jmetal.runner.lc_psode.ZhangBoV35P25DReportRunner"
mkdir -p "$OUT/logs"

cpus=("0-3" "4-7" "8-11" "12-15" "16-19")

# Priority batch: all five Zhang-Bo A4 seeds must pass before any comparator starts.
pids=()
for slot in 1 2 3 4 5; do
  i=$((slot-1))
  taskset -c "${cpus[$i]}" java -Xmx4g -cp "$APP" "$MAIN" \
    --seed-slot "$slot" --algorithm ZHANGBO_A4 \
    --project-root "$ROOT/project" --output "$OUT" \
    >"$OUT/logs/slot-${slot}-ZHANGBO_A4.log" 2>&1 &
  pids+=("$!")
done
for pid in "${pids[@]}"; do wait "$pid"; done
for seed in 20260822 20260823 20260824 20260825 20260826; do
  grep -q '^status=COMPLETED' "$OUT/runs/seed-$seed/ZHANGBO_A4/status.properties"
  grep -q '^fullEvaluations=50000' "$OUT/runs/seed-$seed/ZHANGBO_A4/status.properties"
done
date -Iseconds > "$OUT/A4-batch-completed-at.txt"

algorithms=(HMOPSO_QGS_F HMOPSO_QLS_F MOPSO_F MOPSODS_DE_F MOHEADE_F NSGA_II_F SPEA2_F)
pids=()
for slot in 1 2 3 4 5; do
  i=$((slot-1))
  (
    for algorithm in "${algorithms[@]}"; do
      taskset -c "${cpus[$i]}" java -Xmx4g -cp "$APP" "$MAIN" \
        --seed-slot "$slot" --algorithm "$algorithm" \
        --project-root "$ROOT/project" --output "$OUT" \
        >"$OUT/logs/slot-${slot}-${algorithm}.log" 2>&1
    done
  ) &
  pids+=("$!")
done
for pid in "${pids[@]}"; do wait "$pid"; done

taskset -c 0-3 java -Xmx4g -cp "$APP" "$REPORT" --output "$OUT" \
  >"$OUT/logs/report.log" 2>&1
date -Iseconds > "$OUT/ALL-COMPLETED.txt"
