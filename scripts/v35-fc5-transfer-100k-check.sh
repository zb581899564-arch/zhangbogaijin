#!/usr/bin/env bash
# Quick status monitor for the V35-FC5-T 100k experiment.
set -uo pipefail
R=/home/inspur/aicomp/zhangbo-v35-fc5-transfer-100k-20260825
echo "java_procs=$(ps -eo args | grep -c '[j]ava')"
echo "tmux: $(tmux ls 2>/dev/null | grep v35-fc5-transfer || echo none)"
for s in 20260901 20260902 20260903; do
  for a in A2_CFVF A4_BUDGET_AWARE_CATA; do
    f="$R/output/100k/100_5_3_1/seed-$s/$a/status.properties"
    if [[ -f "$f" ]]; then
      echo "== $s $a =="
      grep -E "status=|fullEvaluations=|decoderCalls=|illegalSolutions=|duplicateEvaluations=" "$f"
    else
      echo "== $s $a : no status yet (running) =="
    fi
  done
done
