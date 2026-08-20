#!/usr/bin/env bash
set -euo pipefail

root="/home/inspur/aicomp/zhangbo-v35-p25a-main-variant-20260814"
cd "$root"
sha256sum -c upload-sha256.tsv
mkdir -p logs results
chmod 0755 deployment/run-slot.sh deployment/wait-and-report.sh

if tmux has-session -t zhangbo-v35-p25a 2>/dev/null; then
  echo "tmux session already exists: zhangbo-v35-p25a" >&2
  exit 1
fi

tmux new-session -d -s zhangbo-v35-p25a -n seed1 \
  "bash '$root/deployment/run-slot.sh' 1 0-3 A0 A4 A5"
tmux new-window -t zhangbo-v35-p25a -n seed2 \
  "bash '$root/deployment/run-slot.sh' 2 4-7 A4 A5 A0"
tmux new-window -t zhangbo-v35-p25a -n seed3 \
  "bash '$root/deployment/run-slot.sh' 3 8-11 A5 A0 A4"
tmux new-window -t zhangbo-v35-p25a -n seed4 \
  "bash '$root/deployment/run-slot.sh' 4 12-15 A0 A5 A4"
tmux new-window -t zhangbo-v35-p25a -n seed5 \
  "bash '$root/deployment/run-slot.sh' 5 16-19 A4 A0 A5"
tmux new-window -t zhangbo-v35-p25a -n report \
  "bash '$root/deployment/wait-and-report.sh' |& tee '$root/logs/report-monitor.log'"
tmux list-windows -t zhangbo-v35-p25a

