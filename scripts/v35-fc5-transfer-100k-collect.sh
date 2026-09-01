#!/usr/bin/env bash
set -uo pipefail
R=/home/inspur/aicomp/zhangbo-v35-fc5-transfer-100k-20260825
echo "=== ROOT ARTIFACTS ==="
for f in FIRST_TIER_100K_TASKS.tsv FIRST_TIER_100K_ACCEPTANCE.csv FIRST_TIER_100K_ACCEPTANCE.properties RUNNER_SHA256.txt INPUT_SHA256.tsv; do
  if [[ -f "$R/$f" ]]; then
    printf '%s\t%s\t%s\n' "$f" "$(sha256sum "$R/$f" | awk '{print $1}')" "$(stat -c %s "$R/$f")"
  else
    echo "MISSING: $f"
  fi
done
echo "=== PER-RUN PROVENANCE (instance/seed : arm sourceRunId profileSha actualFE) ==="
for s in 20260901 20260902 20260903; do
  for a in A2_CFVF A4_BUDGET_AWARE_CATA; do
    d="$R/output/100k/100_5_3_1/seed-$s/$a"
    src=$(grep -E "^sourceRunId=" "$d/configuration.txt" 2>/dev/null | head -1 | cut -d= -f2)
    prof=$(grep -E "^profileSha256=" "$d/configuration.txt" 2>/dev/null | head -1 | cut -d= -f2)
    fe=$(grep -E "^fullEvaluations=" "$d/status.properties" 2>/dev/null | head -1 | cut -d= -f2)
    echo "100_5_3_1 $s $a : sourceRunId=$src profileSha=$prof actualFE=$fe"
  done
done
echo "=== OUTPUT TREE ==="
find "$R/output/100k" -maxdepth 3 -type d | sort
