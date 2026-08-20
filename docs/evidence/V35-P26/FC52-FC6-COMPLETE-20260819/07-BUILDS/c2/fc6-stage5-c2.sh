#!/usr/bin/env bash
# FC-6A-POST / Build-C2: BP-PDDR 稳定性诊断 (C2-BASE vs C2-BP)
# 12 跑: {100_2_3_1, 20_2_3_1} x {BASE, BP} x {seed 20260822/23/24}
# 500k FE, pacing 参数与 Stage-2/3 完全一致（--local-search-times 30
# --g-block-length 5 --local-fe-budget 0.25:0.65）。
# 修正 stage4 的 xargs jobs 文件追加 bug：每个 batch 用全新 jobs 文件。
set -euo pipefail

WORK_ROOT=/home/inspur/aicomp/zhangbo-fc6-20260818
MAIN=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25EBudgetDiagnosticRunner
PROJECT_ROOT=/home/inspur/aicomp/zhangbo-fc4-20260817/src/java-jmetal58
OUTPUT_ROOT="$WORK_ROOT/results/stage5-c2"
LOG_ROOT="$WORK_ROOT/logs"
BP_JAR="$WORK_ROOT/jars/jmetal-exec-5.8-BUILD-C2-BP-diag.jar"
BASE_JAR="$WORK_ROOT/jars/jmetal-exec-5.8-BUILD-C2-BASE-diag.jar"

mkdir -p "$OUTPUT_ROOT" "$LOG_ROOT"
: > "$LOG_ROOT/stage5-task.log"

run_one() {
  local instance="$1" arm="$2" seed="$3" jar="$4"
  local dir="$OUTPUT_ROOT/$instance/$arm/seed-$seed"
  mkdir -p "$dir"
  java -Xmx6g -cp "$jar" "$MAIN" \
    --project-root "$PROJECT_ROOT" --output "$dir" \
    --local-search-times 30 --seed "$seed" --max-fes 500000 \
    --instance "$instance" --local-fe-budget 0.25:0.65 \
    > "$dir/console.log" 2>&1
  echo "stage5 $instance $arm seed=$seed: $(tail -1 "$dir/console.log")" \
    >> "$LOG_ROOT/stage5-task.log"
}
export -f run_one
export MAIN PROJECT_ROOT OUTPUT_ROOT LOG_ROOT BP_JAR BASE_JAR

date -Is > "$LOG_ROOT/stage5-started.txt"

# batch 1: 100-job x {BASE,BP} x 3 seeds（全新 jobs 文件）
: > "$LOG_ROOT/stage5-jobs-100.txt"
for seed in 20260822 20260823 20260824; do
  echo "run_one 100_2_3_1 BASE $seed \$BASE_JAR" >> "$LOG_ROOT/stage5-jobs-100.txt"
  echo "run_one 100_2_3_1 BP $seed \$BP_JAR" >> "$LOG_ROOT/stage5-jobs-100.txt"
done
xargs -P 3 -a "$LOG_ROOT/stage5-jobs-100.txt" -d '\n' -I {} bash -c '{}'

# batch 2: 20-job x {BASE,BP} x 3 seeds（全新 jobs 文件，不追加到 batch1）
: > "$LOG_ROOT/stage5-jobs-20.txt"
for seed in 20260822 20260823 20260824; do
  echo "run_one 20_2_3_1 BASE $seed \$BASE_JAR" >> "$LOG_ROOT/stage5-jobs-20.txt"
  echo "run_one 20_2_3_1 BP $seed \$BP_JAR" >> "$LOG_ROOT/stage5-jobs-20.txt"
done
xargs -P 3 -a "$LOG_ROOT/stage5-jobs-20.txt" -d '\n' -I {} bash -c '{}'

date -Is > "$LOG_ROOT/stage5-completed.txt"
echo "STAGE5_C2_ALL_DONE"
