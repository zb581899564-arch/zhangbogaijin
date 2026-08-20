#!/usr/bin/env bash
set -u

ROOT=/home/inspur/aicomp/zhangbo-java-p9-five-additional-500k-20260810
MAIN=org.uma.jmetal.runner.lc_psode.ZhangBoP9FiveSeedRunner
SLOT="$1"
CORES="$2"

cd "$ROOT"
mkdir -p "$ROOT/results" "$ROOT/logs"
date -Is > "$ROOT/logs/slot-${SLOT}.started-at.txt"
taskset -c "$CORES" java -Xmx4g -cp "$ROOT/app.jar" "$MAIN" \
  --seed-slot "$SLOT" --project-root "$ROOT" --output "$ROOT/results" \
  > "$ROOT/logs/slot-${SLOT}.console.log" 2>&1
STATUS=$?
printf '%s\n' "$STATUS" > "$ROOT/logs/slot-${SLOT}.exit-code.txt"
date -Is > "$ROOT/logs/slot-${SLOT}.finished-at.txt"
exit "$STATUS"
