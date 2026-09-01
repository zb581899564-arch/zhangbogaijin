#!/usr/bin/env bash
# V35-PFC5-F1: launch the single A4 / 100_5_3_1 / seed 20260901 / 500000 FE / telemetry OFF run.
# Owns ONLY its own PID/PGID/session. Never kills or pauses foreign processes.
# Does NOT use tmux (four foreign fc6-stage* sessions exist on this host).
set -euo pipefail

R=/home/inspur/aicomp/zhangbo-v35-pfc5-f1-20260829
RUN_ID=V35PFC5F1-100_5_3_1-20260901-A4
CPUSET=22-23
OUT="$R/output/A4"
LOG="$R/logs/$RUN_ID.log"
ENVF="$R/logs/$RUN_ID.env"
PIDF="$R/pids/$RUN_ID.pid"
JAVAPIDF="$R/pids/$RUN_ID.java.pid"
EXITF="$R/pids/$RUN_ID.exitcode"

command -v setsid >/dev/null || { echo "REFUSING: setsid missing"; exit 70; }
command -v taskset >/dev/null || { echo "REFUSING: taskset missing"; exit 70; }

# --- refuse to clobber anything -------------------------------------------
[ -e "$OUT" ] && { echo "REFUSING: output already exists: $OUT"; exit 71; }
[ -e "$PIDF" ] && { echo "REFUSING: pid file already exists: $PIDF"; exit 72; }
[ -e "$EXITF" ] && { echo "REFUSING: exitcode file already exists"; exit 72; }
find "$R" -maxdepth 3 -name ".partial-*" -print | grep . && { echo "REFUSING: partial residue present"; exit 73; }
pgrep -af "ZhangBoV35FormalAblationArmRunner" | grep -v pgrep && { echo "REFUSING: runner already active"; exit 74; } || true

mkdir -p "$R/output" "$R/logs" "$R/pids"

# --- record the launch environment (user requirement, section 六) ----------
export LC_ALL=C
{
  echo "runId=$RUN_ID"
  echo "f1Started=true"
  echo "host=$(hostname)"
  echo "user=$(whoami)"
  echo "cpuModel=$(lscpu | sed -n 's/^Model name: *//p' | head -1)"
  echo "logicalCpus=$(nproc)"
  echo "cpuAffinity=$CPUSET"
  echo "jvmPath=$(command -v java)"
  echo "jvmVersion=$(java -version 2>&1 | head -1)"
  echo "jvmArgs=-Xmx4g"
  echo "heap=-Xmx4g"
  echo "freeMemoryGB=$(free -g | awk '/^Mem:/{print $7}')"
  echo "freeDiskRoot=$(df -h / | awk 'NR==2{print $4}')"
  echo "loadAvg=$(cat /proc/loadavg)"
  echo "concurrentJavaProcesses=$(pgrep -c java 2>/dev/null || echo 0)"
  echo "concurrentExperimentProcesses=$(pgrep -af 'jmetal-exec|V35MidHorizon|ZhangBoV35' | grep -v pgrep | wc -l)"
  echo "wallClockStartUtc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "outputDirExistedBefore=false"
  echo "partialResidue=0"
  echo "isolation=setsid+nohup+taskset"
  echo "tmuxUsed=false"
  echo "telemetry=OFF"
  echo "requestedMaxFE=500000"
  echo "population=100"
  echo "seed=20260901"
  echo "instance=100_5_3_1"
  echo "arm=A4"
} > "$ENVF"

# --- launch in its own session, immune to SSH hangup, pinned to 22-23 ------
cd "$R"
setsid bash -c '
  echo $$ > "'"$PIDF"'"
  taskset -c '"$CPUSET"' java -Xmx4g \
    -cp "'"$R"'/input/classes:'"$R"'/input/frozen-algorithm.jar" \
    org.uma.jmetal.runner.lc_psode.ZhangBoV35FormalAblationArmRunner \
    --plan "'"$R"'/plans/F1.properties" \
    --output "'"$OUT"'" &
  JAVA=$!
  echo "$JAVA" > "'"$JAVAPIDF"'"
  wait "$JAVA"
  echo $? > "'"$EXITF"'"
' > "$LOG" 2>&1 < /dev/null &

sleep 5
SESS=$(cat "$PIDF" 2>/dev/null || echo 0)
JAVAP=$(cat "$JAVAPIDF" 2>/dev/null || echo 0)
echo "sessionLeaderPid=$SESS"
echo "javaPid=$JAVAP"
echo "alive=$(kill -0 "$JAVAP" 2>/dev/null && echo yes || echo no)"
ps -o pid,pgid,sid,psr,etime,cmd -p "$JAVAP"
echo "--- cmdline identity ---"
tr '\0' ' ' < "/proc/$JAVAP/cmdline"; echo
echo "--- taskset affinity mask ---"
taskset -p "$JAVAP"
echo "--- log so far ---"
tail -n 20 "$LOG"
