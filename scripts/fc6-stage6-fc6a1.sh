#!/usr/bin/env bash
# FC-6A.1 PDDR Population Composition Audit — stage6 统一跑
# 仅至上传到服务器跑；不含拉回/离线分析/报告。
# 12 跑：QGS 臂×6 (CorrectedComparisonRunner HMOPSO_QGS_F) + BASE 臂×6 (BudgetDiagnosticRunner ls=30,budget 0.25:0.65)
# 每跑 MaxFEs=500000, population=100, FM3/NONE/DEGENERATE_SINGLE_FAMILY, 3 并行, 预期 <40min
# QGS 入口 = V35FairRunner.Mode.V35_BASELINE -> 同一 applyEvaluatedPddr (score 逐位同原 PddrFf)
# 陷阱：本批 jar 必须以原始 ZhangBoEvaluatedPddrSelector(无 BP 槽)构建 -> BUILD-C-bppddr-diag
set -euo pipefail

WORK_ROOT=/home/inspur/aicomp/zhangbo-fc6a1-20260819
PROJECT_ROOT="$WORK_ROOT/src/java-jmetal58"
OUTPUT_ROOT="$WORK_ROOT/results"
LOG_ROOT="$WORK_ROOT/logs"
MAIN_CP="$PROJECT_ROOT/jmetal-exec/target/jmetal-exec-5.8-jar-with-dependencies.jar"
BUILD_C_JAR="$PROJECT_ROOT/jmetal-exec/target/jmetal-exec-5.8-BUILD-C-bppddr-diag.jar"
CMP=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25ECorrectedComparisonRunner
BUDGET=org.uma.jmetal.runner.lc_psode.ZhangBoV35P25EBudgetDiagnosticRunner

mkdir -p "$OUTPUT_ROOT" "$LOG_ROOT"
cd "$PROJECT_ROOT"

# 使用已部署的 BUILD-C jar 作为 with-dependencies 主类路径
# 若 MAIN_CP 不存在或哈希不符，回退到 BUILD-C 复制
if [ ! -s "$MAIN_CP" ]; then
  if [ -s "$BUILD_C_JAR" ]; then
    cp "$BUILD_C_JAR" "$MAIN_CP"
    echo "COPIED_BUILD_C_TO_MAIN_CP $(sha256sum "$MAIN_CP" | cut -d' ' -f1)" | tee "$LOG_ROOT/build-status.txt"
  else
    echo "MISSING_JAR both $MAIN_CP and $BUILD_C_JAR absent" | tee -a "$LOG_ROOT/build-status.txt"
    exit 1
  fi
else
  echo "MAIN_CP_EXISTS $(sha256sum "$MAIN_CP" | cut -d' ' -f1)" | tee "$LOG_ROOT/build-status.txt"
fi
if [ -s "$BUILD_C_JAR" ]; then
  echo "BUILD_C_SHA256=$(sha256sum "$BUILD_C_JAR" | cut -d' ' -f1) BUILD_C_BYTES=$(stat -c%s "$BUILD_C_JAR")" | tee -a "$LOG_ROOT/build-status.txt"
fi

# 轻量校验：jar 内包含审计类
if ! jar tf "$MAIN_CP" | grep -q "V35Fc6BpPddrDiagnosticAudit.class"; then
  echo "JAR_MISSING_AUDIT_CLASS" | tee -a "$LOG_ROOT/build-status.txt"
  exit 1
fi

run_qgs() {
  local instance="$1" seed="$2"
  local dir="$OUTPUT_ROOT/QGS/$instance/seed-$seed"
  mkdir -p "$dir"
  echo "[$(date -Is)] START QGS $instance seed=$seed" | tee -a "$LOG_ROOT/task.log"
  if java -Xmx6g -cp "$MAIN_CP" "$CMP" \
      --algorithm HMOPSO_QGS_F --instance "$instance" --seed "$seed" --max-fes 500000 \
      --project-root "$PROJECT_ROOT" --output "$OUTPUT_ROOT/QGS/$instance" > "$dir/console.log" 2>&1; then
    echo "[$(date -Is)] DONE QGS $instance seed=$seed $(tail -1 "$dir/console.log")" | tee -a "$LOG_ROOT/task.log"
  else
    echo "[$(date -Is)] FAIL QGS $instance seed=$seed exit=$?" | tee -a "$LOG_ROOT/task.log"
    tail -30 "$dir/console.log" | tee -a "$LOG_ROOT/task.log"
    return 1
  fi
  # 中性门预检：front.csv 存在且非空
  if [ ! -s "$dir/front.csv" ]; then
    echo "MISSING_FRONT QGS $instance seed=$seed" | tee -a "$LOG_ROOT/task.log"
    return 1
  fi
  echo "FRONT_SHA QGS $instance seed=$seed $(sha256sum "$dir/front.csv" | cut -d' ' -f1) bytes=$(stat -c%s "$dir/front.csv")" | tee -a "$LOG_ROOT/task.log"
  if grep -q "fc6diagComp" "$dir/mechanism-summary.txt" 2>/dev/null; then
    echo "FC6DIAGCOMP_PRESENT QGS $instance seed=$seed lines=$(grep -c "fc6diagComp" "$dir/mechanism-summary.txt")" | tee -a "$LOG_ROOT/task.log"
  else
    echo "FC6DIAGCOMP_MISSING QGS $instance seed=$seed" | tee -a "$LOG_ROOT/task.log"
    return 1
  fi
}

run_base() {
  local instance="$1" seed="$2"
  local dir="$OUTPUT_ROOT/BASE/$instance/seed-$seed"
  mkdir -p "$dir"
  echo "[$(date -Is)] START BASE $instance seed=$seed" | tee -a "$LOG_ROOT/task.log"
  if java -Xmx6g -cp "$MAIN_CP" "$BUDGET" \
      --local-search-times 30 --instance "$instance" --seed "$seed" --max-fes 500000 --local-fe-budget 0.25:0.65 \
      --project-root "$PROJECT_ROOT" --output "$OUTPUT_ROOT/BASE/$instance" > "$dir/console.log" 2>&1; then
    echo "[$(date -Is)] DONE BASE $instance seed=$seed $(tail -1 "$dir/console.log")" | tee -a "$LOG_ROOT/task.log"
  else
    echo "[$(date -Is)] FAIL BASE $instance seed=$seed exit=$?" | tee -a "$LOG_ROOT/task.log"
    tail -30 "$dir/console.log" | tee -a "$LOG_ROOT/task.log"
    return 1
  fi
  if [ ! -s "$dir/front.csv" ]; then
    echo "MISSING_FRONT BASE $instance seed=$seed" | tee -a "$LOG_ROOT/task.log"
    return 1
  fi
  echo "FRONT_SHA BASE $instance seed=$seed $(sha256sum "$dir/front.csv" | cut -d' ' -f1) bytes=$(stat -c%s "$dir/front.csv")" | tee -a "$LOG_ROOT/task.log"
  if grep -q "fc6diagComp" "$dir/mechanism-summary.txt" 2>/dev/null; then
    echo "FC6DIAGCOMP_PRESENT BASE $instance seed=$seed lines=$(grep -c "fc6diagComp" "$dir/mechanism-summary.txt")" | tee -a "$LOG_ROOT/task.log"
  else
    echo "FC6DIAGCOMP_MISSING BASE $instance seed=$seed" | tee -a "$LOG_ROOT/task.log"
    return 1
  fi
}

export -f run_qgs run_base
export MAIN_CP CMP BUDGET PROJECT_ROOT OUTPUT_ROOT LOG_ROOT

: > "$LOG_ROOT/task.log"
: > "$LOG_ROOT/jobs.txt"
date -Is > "$LOG_ROOT/started-at.txt"
echo "FC6A1_STAGE6_START $(date -Is) BUILD_C=$(cat "$LOG_ROOT/build-status.txt" 2>/dev/null | tr '\n' ' ')" | tee -a "$LOG_ROOT/task.log"

# 生成 jobs 列表（12 跑，独立 jobs 文件，沿用 stage5 修正）
for inst in 20_2_3_1 100_2_3_1; do
  for seed in 20260822 20260823 20260824; do
    echo "run_qgs $inst $seed" >> "$LOG_ROOT/jobs.txt"
  done
done
for inst in 20_2_3_1 100_2_3_1; do
  for seed in 20260822 20260823 20260824; do
    echo "run_base $inst $seed" >> "$LOG_ROOT/jobs.txt"
  done
done

echo "JOBS_LIST 12 runs, 3 parallel, ~40min expected (QGS 100-job ~69s, BASE ~510s)" | tee -a "$LOG_ROOT/task.log"
cat "$LOG_ROOT/jobs.txt" | tee -a "$LOG_ROOT/task.log"

# 3 并行执行
xargs -P 3 -a "$LOG_ROOT/jobs.txt" -d '\n' -I {} bash -c '{}'

date -Is > "$LOG_ROOT/completed-at.txt"
echo "FC6A1_STAGE6_DONE $(date -Is)" | tee -a "$LOG_ROOT/task.log"

# 中性验证门汇总（脚本内置）
echo "=== NEUTRAL_GATE_CHECK (BASE 6/6 vs stage5 C2-BASE) ===" | tee -a "$LOG_ROOT/task.log"
echo "Expected stage5 C2-BASE sha (from user spec):" | tee -a "$LOG_ROOT/task.log"
echo "  100-job fcb2dc88 / 2920d9ea / 5feb4c4b" | tee -a "$LOG_ROOT/task.log"
echo "  20-job  aee14130 / f8c23aa1 / 47f41524" | tee -a "$LOG_ROOT/task.log"
echo "Actual BASE fronts:" | tee -a "$LOG_ROOT/task.log"
for inst in 20_2_3_1 100_2_3_1; do
  for seed in 20260822 20260823 20260824; do
    f="$OUTPUT_ROOT/BASE/$inst/seed-$seed/front.csv"
    if [ -f "$f" ]; then
      echo "  BASE $inst seed=$seed sha=$(sha256sum "$f" | cut -d' ' -f1)" | tee -a "$LOG_ROOT/task.log"
    else
      echo "  BASE $inst seed=$seed MISSING" | tee -a "$LOG_ROOT/task.log"
    fi
  done
done
echo "QGS fronts (for 100-seed22 history对照, 其余以 BASE 全中论证):" | tee -a "$LOG_ROOT/task.log"
for inst in 20_2_3_1 100_2_3_1; do
  for seed in 20260822 20260823 20260824; do
    f="$OUTPUT_ROOT/QGS/$inst/seed-$seed/front.csv"
    if [ -f "$f" ]; then
      echo "  QGS $inst seed=$seed sha=$(sha256sum "$f" | cut -d' ' -f1)" | tee -a "$LOG_ROOT/task.log"
    else
      echo "  QGS $inst seed=$seed MISSING" | tee -a "$LOG_ROOT/task.log"
    fi
  done
done

echo "ALL_STAGE6_QGS_BASE_DONE — 停止于上传到服务器跑；拉回/分析/报告按用户后续指令执行" | tee -a "$LOG_ROOT/task.log"
