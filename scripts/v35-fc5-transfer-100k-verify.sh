#!/usr/bin/env bash
set -uo pipefail
R=/home/inspur/aicomp/zhangbo-v35-fc5-transfer-100k-20260825
echo "java_procs=$(ps -eo args | grep -c '[j]ava')"
echo "tmux_session_count=$(tmux ls 2>/dev/null | grep -c v35-fc5-transfer-100k)"
# expected 0 => session gone
echo "--- 5 root artifacts ---"
for f in FIRST_TIER_100K_TASKS.tsv FIRST_TIER_100K_ACCEPTANCE.csv FIRST_TIER_100K_ACCEPTANCE.properties RUNNER_SHA256.txt INPUT_SHA256.tsv; do
  if test -f "$R/$f"; then echo "OK $f"; else echo "MISSING $f"; fi
done
echo "--- 50k authoritative dir (reference) ---"
ls -ld /home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825
echo "--- 50k dir files unchanged? list key files ---"
ls -la /home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825 | head -20
