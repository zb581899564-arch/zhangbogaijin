#!/usr/bin/env bash
# FC-6A.1 / Stage-6: PDDR Population Composition Audit (QGS vs A4-Pacing BASE)
# 12 runs: {20_2_3_1, 100_2_3_1} x {QGS, BASE} x {seed 20260822/23/24}
# - QGS arm: ZhangBoV35P25ECorrectedComparisonRunner --algorithm HMOPSO_QGS_F
#   (ZhangBoMOHPSOQ formal-baseline mode, SAME applyEvaluatedPddr entry)
# - BASE arm: ZhangBoV35P25EBudgetDiagnosticRunner, pacing params identical
#   to stage5 (ls=30, g-block=5, budget 0.25:0.65)
# Single jar BUILD-C3-COMP (original selector + composition audit, sha256
# 5233b690db12d7130549355228f4da026589f28759d702484ac65d178aaa3b4a).
# Neutrality gates checked after completion: 6 BASE fronts must match stage5
# C2-BASE sha256; QGS 100-job seed22 must match fc-time-sanity history.
set -euo pipefail

WORK_ROOT=/home/inspur/aicomp/zhangbo-fc6-20260818
PROJECT_ROOT=/home/inspur/aicomp/zhangbo-fc4-20260817/src/java-jmetal58
OUTPUT_ROOT="$WORK_ROOT/results/stage6-fc6a1"
LOG_ROOT="$WORK_ROOT/logs"
JAR="$WORK_ROOT/jars/jmetal-exec-5.8-BUILD-C3-COMP.jar"
MAIN_QGS=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25ECorrectedComparisonRunner
MAIN_BASE=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25EBudgetDiagnosticRunner

mkdir -p "$OUTPUT_ROOT" "$LOG_ROOT"
: > "$LOG_ROOT/stage6-task.log"

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
  echo "stage6 QGS $instance seed=$seed: $(tail -1 "$dir/console.log")" \
    >> "$LOG_ROOT/stage6-task.log"
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
  echo "stage6 BASE $instance seed=$seed: $(tail -1 "$dir/console.log")" \
    >> "$LOG_ROOT/stage6-task.log"
}
export -f run_qgs run_base
export MAIN_QGS MAIN_BASE PROJECT_ROOT OUTPUT_ROOT LOG_ROOT JAR

date -Is > "$LOG_ROOT/stage6-started.txt"

# batch 1: 100-job {QGS, BASE} x 3 seeds (fresh jobs file)
: > "$LOG_ROOT/stage6-jobs-100.txt"
for seed in 20260822 20260823 20260824; do
  echo "run_qgs 100_2_3_1 $seed" >> "$LOG_ROOT/stage6-jobs-100.txt"
  echo "run_base 100_2_3_1 $seed" >> "$LOG_ROOT/stage6-jobs-100.txt"
done
xargs -P 3 -a "$LOG_ROOT/stage6-jobs-100.txt" -d '\n' -I {} bash -c '{}'

# batch 2: 20-job {QGS, BASE} x 3 seeds (fresh jobs file)
: > "$LOG_ROOT/stage6-jobs-20.txt"
for seed in 20260822 20260823 20260824; do
  echo "run_qgs 20_2_3_1 $seed" >> "$LOG_ROOT/stage6-jobs-20.txt"
  echo "run_base 20_2_3_1 $seed" >> "$LOG_ROOT/stage6-jobs-20.txt"
done
xargs -P 3 -a "$LOG_ROOT/stage6-jobs-20.txt" -d '\n' -I {} bash -c '{}'

date -Is > "$LOG_ROOT/stage6-completed.txt"
echo "STAGE6_FC6A1_ALL_DONE"
