#!/usr/bin/env bash
# Runs one existing V35-DOE1 preflight JVM on the training host.  This is a
# diagnostic-only launcher: it is deliberately capped at the already accepted
# 2k preflight request and cannot invoke the 500k confirmation entry point.
set -euo pipefail

ROOT="${1:?remote campaign root required}"
RUN_KEY="${2:?diagnostic RunKey required}"
LEVEL="${3:?parallelism level required}"
LANE="${4:?lane required}"
CPU="${5:?cpu id required}"

EXPECTED_JAR_SHA256="fc5fff0381ff5c200d35c16b8f310af8da796d2dc64a38962b29f641f3d29ef9"
JAR="$ROOT/jmetal-exec-5.8-jar-with-dependencies.jar"
MAIN="org.uma.jmetal.runner.lc_psode.ZhangBoV35Doe1MixtureRunner"
OUT="$ROOT/parallelism-benchmark-v35-final-freeze-20260822/runs/$RUN_KEY"

[[ "$LEVEL" =~ ^(4|8|12|16)$ ]] || { echo "unsupported parallelism level: $LEVEL" >&2; exit 64; }
[[ "$LANE" =~ ^[0-9]+$ && "$CPU" =~ ^[0-9]+$ ]] || { echo "lane/cpu must be integers" >&2; exit 64; }
[[ -f "$JAR" ]] || { echo "missing frozen DOE1 jar: $JAR" >&2; exit 65; }
[[ ! -e "$OUT" ]] || { echo "refusing to overwrite benchmark evidence: $OUT" >&2; exit 66; }

JAR_SHA256="$(sha256sum "$JAR" | awk '{print $1}')"
[[ "$JAR_SHA256" == "$EXPECTED_JAR_SHA256" ]] || {
  echo "frozen DOE1 jar hash mismatch: $JAR_SHA256" >&2; exit 67;
}

mkdir -p "$OUT"
START_NS="$(date +%s%N)"
set +e
/usr/bin/time -f 'elapsed_seconds=%e\nuser_seconds=%U\nsystem_seconds=%S\ncpu_percent=%P\nmax_rss_kb=%M' \
  -o "$OUT/resource-time.properties" \
  taskset -c "$CPU" java -Xms256m -Xmx2g -cp "$JAR" "$MAIN" \
  --phase RUN --treatment 8 --instance 20_5_4_1 --seed 20260901 --max-fes 2000 \
  --project-root "$ROOT" --output "$OUT" >"$OUT/console.log" 2>&1
EXIT_CODE=$?
set -e
FINISH_NS="$(date +%s%N)"
STATUS_FILE="$(find "$OUT" -name status.properties -type f -print -quit || true)"
FULL_EVALUATIONS=""
RUN_STATUS=""
if [[ -n "$STATUS_FILE" ]]; then
  FULL_EVALUATIONS="$(awk -F= '$1=="fullEvaluations" {print $2}' "$STATUS_FILE")"
  RUN_STATUS="$(awk -F= '$1=="status" {print $2}' "$STATUS_FILE")"
fi
{
  echo "benchmarkId=V35-FINAL-PARALLELISM-20260822"
  echo "safetyClass=short_benchmark"
  echo "runKey=$RUN_KEY"
  echo "parallelismLevel=$LEVEL"
  echo "lane=$LANE"
  echo "cpu=$CPU"
  echo "runner=$MAIN"
  echo "requestedMaxFEs=2000"
  echo "treatmentIndex=8"
  echo "mixture=20/40/20/20"
  echo "instance=20_5_4_1"
  echo "seed=20260901"
  echo "jarSha256=$JAR_SHA256"
  echo "runtimeJava=$(java -version 2>&1 | head -n 1)"
  echo "remoteStartedUnixNanos=$START_NS"
  echo "remoteFinishedUnixNanos=$FINISH_NS"
  echo "exitCode=$EXIT_CODE"
  echo "runnerStatus=$RUN_STATUS"
  echo "fullEvaluations=$FULL_EVALUATIONS"
  echo "statusFile=$STATUS_FILE"
} >"$OUT/benchmark.properties"

exit "$EXIT_CODE"
