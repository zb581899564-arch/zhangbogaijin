#!/usr/bin/env bash
# FC-6A.2 / Stage-7: Region x PDDR Composition Audit (QGS vs BASE)
# 12 runs: {20_2_3_1, 100_2_3_1} x {QGS, BASE} x {seed 20260822/23/24}
# Single jar BUILD-C4-REGION (ORIGINAL selector + region audit, sha256
# d5b21fdf4edd181490887f1cd501c751c3faf66e551ba0e06fbaeeeaa1f0646b).
# 174.44 probe auto-activates in 20-job seed22 runs (both arms).
# Neutrality gates: 6 BASE fronts must match stage5 C2-BASE sha256;
# QGS 100-job seed22 must match historical d193056a...
set -euo pipefail

WORK_ROOT=/home/inspur/aicomp/zhangbo-fc6-20260818
PROJECT_ROOT=/home/inspur/aicomp/zhangbo-fc4-20260817/src/java-jmetal58
OUTPUT_ROOT="$WORK_ROOT/results/stage7-fc6a2"
LOG_ROOT="$WORK_ROOT/logs"
JAR="$WORK_ROOT/jars/jmetal-exec-5.8-BUILD-C4-REGION.jar"
MAIN_QGS=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25ECorrectedComparisonRunner
MAIN_BASE=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25EBudgetDiagnosticRunner

mkdir -p "$OUTPUT_ROOT" "$LOG_ROOT"
: > "$LOG_ROOT/stage7-task.log"

run_qgs() {
  local instance="$1" seed="$2"
  local dir="$OUTPUT_ROOT/$instance/QGS/seed-$seed"
  mkdir -p "$dir"
  java -Xmx6g -cp "$JAR" "$MAIN_QGS" \
    --algorithm HMOPSO_QGS_F \
    --project-root "$PROJECT_ROOT" \
    --output "$dir" \
    --instance "$instance" \
    --seed "$seed" \
    --max-fes 500000 \
    > "$dir/console.log" 2>&1
  echo "stage7 QGS $instance seed=$seed: $(tail -1 "$dir/console.log")" \
    >> "$LOG_ROOT/stage7-task.log"
}

run_base() {
  local instance="$1" seed="$2"
  local dir="$OUTPUT_ROOT/$instance/BASE/seed-$seed"
  mkdir -p "$dir"
  java -Xmx6g -cp "$JAR" "$MAIN_BASE" \
    --project-root "$PROJECT_ROOT" --output "$dir" \
    --local-search-times 30 --seed "$seed" --max-fes 500000 \
    --instance "$instance" --local-fe-budget 0.25:0.65 \
    > "$dir/console.log" 2>&1
  echo "stage7 BASE $instance seed=$seed: $(tail -1 "$dir/console.log")" \
    >> "$LOG_ROOT/stage7-task.log"
}
export -f run_qgs run_base
export MAIN_QGS MAIN_BASE PROJECT_ROOT OUTPUT_ROOT LOG_ROOT JAR

date -Is > "$LOG_ROOT/stage7-started.txt"

: > "$LOG_ROOT/stage7-jobs-100.txt"
for seed in 20260822 20260823 20260824; do
  echo "run_qgs 100_2_3_1 $seed" >> "$LOG_ROOT/stage7-jobs-100.txt"
  echo "run_base 100_2_3_1 $seed" >> "$LOG_ROOT/stage7-jobs-100.txt"
done
xargs -P 3 -a "$LOG_ROOT/stage7-jobs-100.txt" -d '\n' -I {} bash -c '{}'

: > "$LOG_ROOT/stage7-jobs-20.txt"
for seed in 20260822 20260823 20260824; do
  echo "run_qgs 20_2_3_1 $seed" >> "$LOG_ROOT/stage7-jobs-20.txt"
  echo "run_base 20_2_3_1 $seed" >> "$LOG_ROOT/stage7-jobs-20.txt"
done
xargs -P 3 -a "$LOG_ROOT/stage7-jobs-20.txt" -d '\n' -I {} bash -c '{}'

date -Is > "$LOG_ROOT/stage7-completed.txt"
echo "STAGE7_FC6A2_ALL_DONE"
