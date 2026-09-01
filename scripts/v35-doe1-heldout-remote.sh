#!/usr/bin/env bash
# Pre-registered V35-DOE-1 held-out confirmation: 60 isolated JVM runs.
# This script is intentionally self-contained so its SHA-256 is part of the evidence.
set -euo pipefail

ROOT="${1:?remote campaign root required}"
JAR="$ROOT/jmetal-exec-5.8-jar-with-dependencies.jar"
# A retry must never overwrite a failed attempt.  Callers may provide a
# separate output directory as the second argument; the historical default is
# retained for a first attempt.
OUT="${2:-$ROOT/output}"
MAIN="org.uma.jmetal.runner.lc_psode.ZhangBoV35Doe1ConfirmationRunner"
REPORT="org.uma.jmetal.runner.lc_psode.ZhangBoV35Doe1ConfirmationReportRunner"
JAR_SHA256="$(sha256sum "$JAR" | awk '{print $1}')"
SCRIPT_SHA256="$(sha256sum "$0" | awk '{print $1}')"
JAVA_RUNTIME="$(java -version 2>&1 | head -n 1)"
SEEDS=(20260901 20260902 20260903 20260904 20260905)
INSTANCES=(20_5_4_1 50_5_4_1 100_5_4_1)
CPU_SETS=(0-3 4-7 8-11 12-15 16-19)

mkdir -p "$OUT/logs"
{
  printf 'sha256\tpath\n'
  sha256sum "$JAR" "$0" "$ROOT"/EADHFSP/*_5_4_1.txt \
    "$ROOT"/instance-extensions/v1/*_5_4_1.setup.txt \
    "$ROOT"/fatigue-parameters/v1/*_5_4_1.fatigue.txt \
    | awk '{print $1 "\t" $2}'
} > "$OUT/deployment-checksums.tsv"
java -cp "$JAR" "$MAIN" --phase REGISTRY --project-root "$ROOT" --output "$OUT"

run_one() {
  local cpu="$1" arm="$2" instance="$3" seed="$4"
  local log="$OUT/logs/${arm}-${instance}-${seed}.log"
  taskset -c "$cpu" java -Xms512m -Xmx4g -cp "$JAR" "$MAIN" \
    --phase RUN --arm "$arm" --instance "$instance" --seed "$seed" \
    --project-root "$ROOT" --output "$OUT" >"$log" 2>&1
  local mixture
  case "$arm" in
    BASE) mixture="20_40_20_20";; T1) mixture="30_50_10_10";;
    T2) mixture="25_25_25_25";; T3) mixture="20_40_30_10";;
    *) exit 64;;
  esac
  local config="$OUT/runs/${arm}-${mixture}/${instance}/seed-${seed}/configuration.txt"
  printf '\nexecutionJarSha256=%s\nremoteScriptSha256=%s\nruntimeJava=%s\n' \
    "$JAR_SHA256" "$SCRIPT_SHA256" "$JAVA_RUNTIME" >> "$config"
}

run_seed_lane() {
  local lane="$1" seed="${SEEDS[$lane]}" cpu="${CPU_SETS[$lane]}"
  local rotations=(BASE T1 T2 T3)
  local arms=()
  for offset in 0 1 2 3; do arms+=("${rotations[$(((lane + offset) % 4))]}"); done
  for arm in "${arms[@]}"; do
    for instance in "${INSTANCES[@]}"; do run_one "$cpu" "$arm" "$instance" "$seed"; done
  done
}

pids=()
for lane in 0 1 2 3 4; do run_seed_lane "$lane" & pids+=("$!"); done
failed=0
for pid in "${pids[@]}"; do wait "$pid" || failed=1; done
if [[ "$failed" -ne 0 ]]; then
  printf 'HELDOUT_FAILED_AT=%s\n' "$(date -Is)" > "$OUT/FAILED.properties"
  exit 1
fi

java -Xms256m -Xmx2g -cp "$JAR" "$REPORT" --input "$OUT" --output "$OUT"
printf 'HELDOUT_COMPLETED_AT=%s\n' "$(date -Is)" > "$OUT/COMPLETED.properties"
