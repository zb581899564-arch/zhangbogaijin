#!/usr/bin/env bash
set -euo pipefail

ROOT=/home/inspur/aicomp/zhangbo-java-p9-single-500k-20260810
OUT="$ROOT/results"
MAIN=org.uma.jmetal.runner.lc_psode.ZhangBoP9SingleComparisonRunner

cd "$ROOT"
mkdir -p "$OUT" "$ROOT/logs"
printf '%s\n' "$$" > "$ROOT/launcher.pid"
date -Is > "$ROOT/started-at.txt"

taskset -c 0-3 java -Xmx4g -cp "$ROOT/app.jar" "$MAIN" \
  --phase FULL --project-root "$ROOT" --output "$OUT" \
  > "$ROOT/logs/FULL-console.log" 2>&1

taskset -c 0-3 java -Xmx4g -cp "$ROOT/app.jar" "$MAIN" \
  --phase HMOPSO_QGS_F --project-root "$ROOT" --output "$OUT" \
  > "$ROOT/logs/HMOPSO-QGS-F-console.log" 2>&1

taskset -c 0-3 java -Xmx4g -cp "$ROOT/app.jar" "$MAIN" \
  --phase REPORT --project-root "$ROOT" --output "$OUT" \
  > "$ROOT/logs/REPORT-console.log" 2>&1

date -Is > "$ROOT/completed-at.txt"
