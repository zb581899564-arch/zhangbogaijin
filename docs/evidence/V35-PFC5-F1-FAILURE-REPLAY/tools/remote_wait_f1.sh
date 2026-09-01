#!/usr/bin/env bash
# V35-PFC5-F1: wait for the single run to finish, then report.
# Polls every 30s up to 90 minutes. Never kills anything, never touches foreign processes.
R=/home/inspur/aicomp/zhangbo-v35-pfc5-f1-20260829
RUN_ID=V35PFC5F1-100_5_3_1-20260901-A4
OUT="$R/output/A4"
LOG="$R/logs/$RUN_ID.log"
JAVAPIDF="$R/pids/$RUN_ID.java.pid"
EXITF="$R/pids/$RUN_ID.exitcode"
MAX=180   # 180 * 30s = 90 minutes

JAVAP=$(cat "$JAVAPIDF" 2>/dev/null || echo 0)
for i in $(seq 1 $MAX); do
  if [ -f "$EXITF" ]; then
    break
  fi
  if ! kill -0 "$JAVAP" 2>/dev/null; then
    sleep 5
    [ -f "$EXITF" ] && break
  fi
  sleep 30
done

echo "=== wait loop ended after $((i*30))s ==="
if [ -f "$EXITF" ]; then
  echo "processExitCode=$(cat "$EXITF")"
else
  echo "processExitCode=UNKNOWN (timeout or pid file missing)"
fi
echo "stillAlive=$(kill -0 "$JAVAP" 2>/dev/null && echo yes || echo no)"
echo "outputExists=$([ -d "$OUT" ] && echo yes || echo no)"
echo "--- partial residue ---"
find "$R" -maxdepth 3 -name ".partial-*" -print
echo "--- log tail ---"
tail -n 30 "$LOG"
echo "--- output listing ---"
ls -la "$OUT" 2>/dev/null || echo "(no output dir)"
