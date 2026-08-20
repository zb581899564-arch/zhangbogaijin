#!/usr/bin/env bash
# FC-TIME Scaling Sanity Check: 100_2_3_1 / 500k / seed 20260822 / 同机串行。
# QGS 与优化后 Pacing 各 warm-up 1 + 正式 1；判定 R=T_Pacing/T_QGS。
set -euo pipefail

cd "E:\学习\李明哲-毕业材料\张博改进\java-jmetal58"
OUT=../docs/evidence/V35-P26/fc-time-sanity-100
mkdir -p "$OUT"
JAR=jmetal-exec/target/jmetal-exec-5.8-jar-with-dependencies.jar
BUDGET=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25EBudgetDiagnosticRunner
CMP=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25ECorrectedComparisonRunner
SEED=20260822
INST=100_2_3_1

measure() {
  local name="$1"; shift
  local dir="$OUT/$name"
  mkdir -p "$dir"
  for i in 0 1; do  # 0 = warm-up, 1 = formal
    local run_dir="$dir/run$i"
    mkdir -p "$run_dir"
    java -Xmx6g -cp "$JAR" "$@" --project-root . --output "$run_dir" \
      --seed "$SEED" --max-fes 500000 \
      > "$dir/console-$i.log" 2>&1 || { echo "$name run$i FAILED"; tail -3 "$dir/console-$i.log"; exit 1; }
    local nanos
    if [ -f "$run_dir/mechanism-summary.txt" ]; then
      nanos=$(grep '^runNanos=' "$run_dir/mechanism-summary.txt" | cut -d= -f2)
    elif [ -f "$run_dir/runs/seed-$SEED/HMOPSO_QGS_F/run-record.csv" ]; then
      nanos=$(tail -1 "$run_dir/runs/seed-$SEED/HMOPSO_QGS_F/run-record.csv" | cut -d, -f9)
    else
      nanos=""
    fi
    echo "$name run$i nanos=$nanos" | tee -a "$OUT/timing.log"
  done
}

echo "SANITY start $(date -Is)" | tee "$OUT/timing.log"
measure qgs "$CMP" --algorithm HMOPSO_QGS_F --instance "$INST"
measure pacing "$BUDGET" --local-search-times 30 --instance "$INST" --local-fe-budget 0.25:0.65
echo "SANITY done $(date -Is)" | tee -a "$OUT/timing.log"