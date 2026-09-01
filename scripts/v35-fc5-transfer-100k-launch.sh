#!/usr/bin/env bash
# V35-FC5-T second-tier 100k screening experiment launcher.
# 6 independent JVM tasks, tmux session name: v35-fc5-transfer-100k.
# Usage:  bash v35-fc5-transfer-100k-launch.sh dry-run   # print everything, start nothing
#         bash v35-fc5-transfer-100k-launch.sh live      # actually launch in tmux
set -euo pipefail

ROOT=/home/inspur/aicomp/zhangbo-v35-fc5-transfer-100k-20260825
JAR="$ROOT/bin/fc5-transfer-diagnostic.jar"
PROJECT="$ROOT/project-root"
CLASS=org.uma.jmetal.runner.lc_psode.ZhangBoV35Fc5TransferRunner
MAXFES=100000
SESSION=v35-fc5-transfer-100k
TASKS="$ROOT/FIRST_TIER_100K_TASKS.tsv"
CPUSETS="0-3 4-7 8-11 12-15 16-19 20-23"

MODE="${1:-dry-run}"   # dry-run | live

# ---- build the 6-task roster: instance/seed/arm/budget ----
: > "$TASKS"
for seed in 20260901 20260902 20260903; do
  printf '100_5_3_1\t%s\tA2_CFVF\t100000\n' "$seed" >> "$TASKS"
  printf '100_5_3_1\t%s\tA4_BUDGET_AWARE_CATA\t100000\n' "$seed" >> "$TASKS"
done
task_count=$(grep -c . "$TASKS")
if [[ "$task_count" != "6" ]]; then
  echo "FATAL: roster is not exactly 6 (got $task_count)" >&2; exit 3
fi

# ---- refuse to run over an existing output tree ----
if [[ -d "$ROOT/output/100k" ]]; then
  echo "FATAL: output/100k already exists; refusing to overwrite." >&2; exit 4
fi

# ---- refuse to run if the tmux session already exists ----
if tmux ls 2>/dev/null | grep -q "$SESSION"; then
  echo "FATAL: tmux session '$SESSION' already exists." >&2; exit 5
fi

i=0
declare -a CMD_ARR=()
while IFS=$'\t' read -r instance seed arm budget; do
  [[ -z "$instance" ]] && continue
  cpuset=$(echo "$CPUSETS" | awk -v n="$i" '{print $(n+1)}')
  snapshot="$ROOT/input/snapshots/$instance/seed-$seed.fourvec"
  output="$ROOT/output/100k/$instance/seed-$seed/$arm"
  log="$ROOT/logs/100k-$instance-$seed-$arm.log"
  if [[ -e "$output" ]]; then
    echo "FATAL: refusing existing output: $output" >&2; exit 20
  fi
  sub="$ROOT/output/100k/$instance/seed-$seed"
  cmd="mkdir -p '$sub' \"$ROOT/logs\"; taskset -c '$cpuset' env CUDA_VISIBLE_DEVICES= java -Xmx4g -cp '$JAR' '$CLASS' --project-root '$PROJECT' --output '$output' --snapshot '$snapshot' --instance '$instance' --seed '$seed' --arm '$arm' --max-fes '$budget' > '$log' 2>&1"
  CMD_ARR+=("$cmd")
  echo "---- TASK $i ----"
  echo "cpuset=$cpuset  instance=$instance  seed=$seed  arm=$arm  budget=$budget"
  echo "output=$output"
  echo "log=$log"
  echo "cmd=$cmd"
  i=$((i+1))
done < "$TASKS"

if [[ "$MODE" != "live" ]]; then
  echo
  echo "=== DRY-RUN ONLY: $task_count tasks prepared, nothing started. Pass 'live' to launch. ==="
  exit 0
fi

# ---- live launch: one tmux window per task, all under the same session ----
echo "=== LIVE LAUNCH ==="
tmux new-session -d -s "$SESSION" -n "task-0" "${CMD_ARR[0]}"
for w in $(seq 1 $((task_count-1))); do
  tmux new-window -t "$SESSION" -n "task-$w" "${CMD_ARR[$w]}"
done
echo "tmux session '$SESSION' created with $task_count windows."
tmux ls | grep "$SESSION" || true
