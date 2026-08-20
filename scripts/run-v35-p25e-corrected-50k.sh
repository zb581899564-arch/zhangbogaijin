#!/usr/bin/env bash
set -euo pipefail

WORK_ROOT="${1:?work root required}"
STATUS_FILE="$WORK_ROOT/EXIT_CODE"
trap 'code=$?; printf "%s\n" "$code" > "$STATUS_FILE"' EXIT
printf "RUNNING\n" > "$STATUS_FILE"
export PATH="$WORK_ROOT/tools/apache-maven-3.8.9/bin:$PATH"
PROJECT_ROOT="$WORK_ROOT/src/java-jmetal58"
OUTPUT_ROOT="$WORK_ROOT/results"
LOG_ROOT="$WORK_ROOT/logs"
MAIN_CP="$PROJECT_ROOT/jmetal-exec/target/jmetal-exec-5.8-jar-with-dependencies.jar"

mkdir -p "$OUTPUT_ROOT" "$LOG_ROOT"
cd "$PROJECT_ROOT"

{
  date -Is
  java -version
  mvn -version
  sha256sum EADHFSP/20_2_3_1.txt \
    instance-extensions/v1/20_2_3_1.setup.txt \
    fatigue-parameters/v1/20_2_3_1.fatigue.txt
} > "$LOG_ROOT/preflight.log" 2>&1

mvn -q -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true package \
  > "$LOG_ROOT/build.log" 2>&1

test -s "$MAIN_CP"

algorithms=(
  ZHANGBO_A4
  HMOPSO_QGS_F
  HMOPSO_QLS_F
  MOPSO_F
  MOPSODS_DE_F
  MOHEADE_F
  NSGA_II_F
  SPEA2_F
)

for algorithm in "${algorithms[@]}"; do
  taskset -c 0-3 java -Xmx4g -cp "$MAIN_CP" \
    org.uma.jmetal.runner.lc_psode.ZhangBoV35P25ECorrectedComparisonRunner \
    --algorithm "$algorithm" \
    --project-root "$PROJECT_ROOT" \
    --output "$OUTPUT_ROOT" \
    > "$LOG_ROOT/${algorithm}.log" 2>&1
done

taskset -c 0-3 java -Xmx4g -cp "$MAIN_CP" \
  org.uma.jmetal.runner.lc_psode.ZhangBoV35P25ECorrectedReportRunner \
  --output "$OUTPUT_ROOT" \
  > "$LOG_ROOT/report.log" 2>&1

date -Is > "$WORK_ROOT/COMPLETED"
