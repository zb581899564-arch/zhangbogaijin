#!/usr/bin/env bash
# FC-6 Stage 1: Build A (FC-5.2 + R_retain observation, NO BP-PDDR)
# seed 22/23/24 death-chain verification + baseline R_retain
# instance 20_2_3_1 / 500k / pacing (local-fe-budget 0.25:0.65)
set -euo pipefail

WORK_ROOT=/home/inspur/aicomp/zhangbo-fc6-20260818
JAR="$WORK_ROOT/jars/jmetal-exec-5.8-BUILD-A-fc52-rretain.jar"
MAIN=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25EBudgetDiagnosticRunner
PROJECT_ROOT=/home/inspur/aicomp/zhangbo-fc4-20260817/src/java-jmetal58
OUTPUT_ROOT="$WORK_ROOT/results/stage1-fc5p2-baseline"
LOG_ROOT="$WORK_ROOT/logs"

mkdir -p "$OUTPUT_ROOT" "$LOG_ROOT"
: > "$LOG_ROOT/stage1-task.log"

run_one() {
  local seed="$1"
  local dir="$OUTPUT_ROOT/20_2_3_1/seed-$seed"
  mkdir -p "$dir"
  java -Xmx6g -cp "$JAR" "$MAIN" \
    --project-root "$PROJECT_ROOT" --output "$dir" \
    --local-search-times 30 --seed "$seed" --max-fes 500000 \
    --instance 20_2_3_1 --local-fe-budget 0.25:0.65 \
    > "$dir/console.log" 2>&1
  echo "stage1 seed=$seed: $(tail -1 "$dir/console.log")" >> "$LOG_ROOT/stage1-task.log"
}
export -f run_one
export JAR MAIN PROJECT_ROOT OUTPUT_ROOT LOG_ROOT

date -Is > "$LOG_ROOT/stage1-started.txt"
for seed in 20260822 20260823 20260824; do
  echo "run_one $seed" >> "$LOG_ROOT/stage1-jobs.txt"
done
xargs -P 3 -a "$LOG_ROOT/stage1-jobs.txt" -d '\n' -I {} bash -c '{}'
date -Is > "$LOG_ROOT/stage1-completed.txt"
echo "STAGE1_ALL_DONE"