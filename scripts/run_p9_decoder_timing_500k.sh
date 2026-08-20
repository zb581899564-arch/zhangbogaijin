#!/usr/bin/env bash
set -euo pipefail

ROOT=/home/inspur/aicomp/zhangbo-java-p9-decoder-timing-500k-20260811
OUT="$ROOT/results"
MAIN=org.uma.jmetal.runner.lc_psode.ZhangBoP9SingleComparisonRunner

finish() {
  code=$?
  printf '%s\n' "$code" > "$ROOT/exit-code.txt"
  date -Is > "$ROOT/finished-at.txt"
}
trap finish EXIT

cd "$ROOT"
sha256sum -c inputs.sha256
mkdir -p "$OUT" "$ROOT/logs"
printf '%s\n' "$$" > "$ROOT/launcher.pid"
date -Is > "$ROOT/started-at.txt"

taskset -c 20-23 java -Xmx4g -cp "$ROOT/app.jar" "$MAIN" \
  --phase FULL --project-root "$ROOT" --output "$OUT" \
  > "$ROOT/logs/FULL-console.log" 2>&1

taskset -c 20-23 java -Xmx4g -cp "$ROOT/app.jar" "$MAIN" \
  --phase HMOPSO_QGS_F --project-root "$ROOT" --output "$OUT" \
  > "$ROOT/logs/HMOPSO-QGS-F-console.log" 2>&1

taskset -c 20-23 java -Xmx4g -cp "$ROOT/app.jar" "$MAIN" \
  --phase REPORT --project-root "$ROOT" --output "$OUT" \
  > "$ROOT/logs/REPORT-console.log" 2>&1

date -Is > "$ROOT/completed-at.txt"
