#!/usr/bin/env bash
# V35-FC-2 四规模 500k 验证 + FC-3 Cheap-Test A/B（训练机 2026-08-17）
set -euo pipefail

WORK_ROOT=/home/inspur/aicomp/zhangbo-fc-500k-20260817
PROJECT_ROOT="$WORK_ROOT/src/java-jmetal58"
OUTPUT_ROOT="$WORK_ROOT/results"
LOG_ROOT="$WORK_ROOT/logs"
MAIN_CP="$PROJECT_ROOT/jmetal-exec/target/jmetal-exec-5.8-jar-with-dependencies.jar"
MAIN=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25EBudgetDiagnosticRunner

mkdir -p "$OUTPUT_ROOT" "$LOG_ROOT"
cd "$PROJECT_ROOT"

# Incremental build (FC sources were already synced over the P25E tree).
export PATH=/home/inspur/aicomp/zhangbo-v35-p25e-corrected-50k-20260815/tools/apache-maven-3.8.9/bin:$PATH
mvn -q -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true package > "$LOG_ROOT/build.log" 2>&1
test -s "$MAIN_CP"
echo "BUILD_OK" > "$LOG_ROOT/build-status.txt"

run_one() {
  local instance="$1" arm="$2" seed="$3" budget="$4" extra="$5" tag="$6"
  local dir="$OUTPUT_ROOT/$tag/$instance/$arm/seed-$seed"
  mkdir -p "$dir"
  java -Xmx6g -cp "$MAIN_CP" "$MAIN" \
    --project-root "$PROJECT_ROOT" --output "$dir" \
    --local-search-times 30 --seed "$seed" --max-fes "$budget" \
    --instance "$instance" $extra > "$dir/console.log" 2>&1
  echo "$tag $instance $arm seed=$seed: $(tail -1 "$dir/console.log")" >> "$LOG_ROOT/task.log"
}

export -f run_one
export MAIN_CP MAIN PROJECT_ROOT OUTPUT_ROOT LOG_ROOT

: > "$LOG_ROOT/task.log"
date -Is > "$LOG_ROOT/started-at.txt"

# FC-2: 4 instances x legacy/pacing x 3 seeds, 500k.
for instance in 10_2_3_1 20_2_3_1 50_2_3_1 100_2_3_1; do
  for seed in 20260822 20260823 20260824; do
    for arm in legacy pacing; do
      extra=""
      if [ "$arm" = pacing ]; then extra="--local-fe-budget 0.25:0.65"; fi
      echo "run_one $instance $arm $seed 500000 \"$extra\" fc2-500k" >> "$LOG_ROOT/jobs.txt"
    done
  done
done

# FC-3: 20_2_3_1 x standard/cheap x 3 seeds, 50k.
for seed in 20260822 20260823 20260824; do
  for arm in standard cheap; do
    extra=""
    if [ "$arm" = cheap ]; then extra="--cheap-test true"; fi
    echo "run_one 20_2_3_1 $arm $seed 50000 \"$extra\" fc3-ab" >> "$LOG_ROOT/jobs.txt"
  done
done

xargs -P 8 -a "$LOG_ROOT/jobs.txt" -d '\n' -I {} bash -c '{}'

date -Is > "$LOG_ROOT/completed-at.txt"
echo "ALL_FC_DONE"
