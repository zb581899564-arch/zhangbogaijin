#!/usr/bin/env bash
# FC-TIME-0: 20_2_3_1 / 500k / seed 20260822 / 同机单 JVM 串行正式计时。
# 三臂：QGS(LegacyComparisonRunner) / Legacy(BudgetDiagnosticRunner 空budget) / Pacing(+local-fe-budget)。
# 每臂 warm-up 1 次（不记）+ 正式 3 次，取中位；输出 R1=Legacy/QGS、R2=Pacing/Legacy、R=Pacing/QGS。
set -euo pipefail

cd "E:\学习\李明哲-毕业材料\张博改进\java-jmetal58"
OUT=../docs/evidence/V35-P26/fc-time0
mkdir -p "$OUT"
JAR=jmetal-exec/target/jmetal-exec-5.8-jar-with-dependencies.jar
BUDGET=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25EBudgetDiagnosticRunner
CMP=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25ECorrectedComparisonRunner
SEED=20260822
INST=20_2_3_1

measure() {
  local name="$1"; shift
  local dir="$OUT/$name"
  mkdir -p "$dir"
  for i in 0 1 2 3; do  # 0 = warm-up, 1..3 = formal
    local tag="run$i"
    local run_dir="$dir/$tag"
    mkdir -p "$run_dir"
    java -Xmx6g -cp "$JAR" "$@" --project-root . --output "$run_dir" \
        --seed "$SEED" --max-fes 500000 \
        > "$dir/console-$tag.log" 2>&1 || { echo "$name $tag FAILED"; tail -3 "$dir/console-$tag.log"; exit 1; }
    local nanos
    if [ -f "$run_dir/mechanism-summary.txt" ]; then
      nanos=$(grep '^runNanos=' "$run_dir/mechanism-summary.txt" | cut -d= -f2)
    elif [ -f "$run_dir/run-record.csv" ]; then
      nanos=$(tail -1 "$run_dir/run-record.csv" | cut -d, -f9)
    else
      nanos=""
    fi
    echo "$name $tag nanos=$nanos" | tee -a "$OUT/timing.log"
  done
}

echo "FC-TIME-0 start $(date -Is)" | tee "$OUT/timing.log"
measure qgs  "$CMP" --algorithm HMOPSO_QGS_F
measure legacy "$BUDGET" --local-search-times 30 --instance "$INST"
measure pacing "$BUDGET" --local-search-times 30 --instance "$INST" --local-fe-budget 0.25:0.65
echo "FC-TIME-0 done $(date -Is)" | tee -a "$OUT/timing.log"