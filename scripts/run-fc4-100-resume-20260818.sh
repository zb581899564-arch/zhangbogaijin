#!/usr/bin/env bash
# FC-4 rho 校准：重跑 100_2_3_1 全部 9 条（2026-08-17 掉线时未启动，空目录）
set -euo pipefail

WORK_ROOT=/home/inspur/aicomp/zhangbo-fc4-20260817
PROJECT_ROOT="$WORK_ROOT/src/java-jmetal58"
OUTPUT_ROOT="$WORK_ROOT/results"
LOG_ROOT="$WORK_ROOT/logs"
MAIN_CP="$PROJECT_ROOT/jmetal-exec/target/jmetal-exec-5.8-jar-with-dependencies.jar"
MAIN=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25EBudgetDiagnosticRunner

mkdir -p "$OUTPUT_ROOT" "$LOG_ROOT"
cd "$PROJECT_ROOT"

export PATH=/home/inspur/aicomp/zhangbo-v35-p25e-corrected-50k-20260815/tools/apache-maven-3.8.9/bin:$PATH
mvn -q -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true package > "$LOG_ROOT/build-resume-100.log" 2>&1
test -s "$MAIN_CP"
echo "BUILD_OK" > "$LOG_ROOT/build-resume-100-status.txt"

run_one() {
  local instance="$1" arm="$2" seed="$3" budget="$4" extra="$5" tag="$6"
  local dir="$OUTPUT_ROOT/$tag/$instance/$arm/seed-$seed"
  mkdir -p "$dir"
  java -Xmx6g -cp "$MAIN_CP" "$MAIN" \
    --project-root "$PROJECT_ROOT" --output "$dir" \
    --local-search-times 30 --seed "$seed" --max-fes "$budget" \
    --instance "$instance" $extra > "$dir/console.log" 2>&1
  echo "resume $tag $instance $arm seed=$seed: $(tail -1 "$dir/console.log")" >> "$LOG_ROOT/task-resume-100.log"
}

export -f run_one
export MAIN_CP MAIN PROJECT_ROOT OUTPUT_ROOT LOG_ROOT

: > "$LOG_ROOT/jobs-resume-100.txt"
date -Is > "$LOG_ROOT/resume-100-started-at.txt"

for rho in 0.1 0.2 0.3; do
  for seed in 20260822 20260823 20260824; do
    echo "run_one 100_2_3_1 rho$rho $seed 500000 \"--local-fe-budget 0.25:0.65 --soft-freeze-rho $rho\" fc4-rho" >> "$LOG_ROOT/jobs-resume-100.txt"
  done
done

xargs -P 8 -a "$LOG_ROOT/jobs-resume-100.txt" -d '\n' -I {} bash -c '{}'

date -Is > "$LOG_ROOT/resume-100-completed-at.txt"
echo "ALL_FC4_100_RESUMED_DONE" > "$LOG_ROOT/fc4-100-done.txt"