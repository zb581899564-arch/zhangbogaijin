#!/usr/bin/env bash
set -euo pipefail

root="/home/inspur/aicomp/zhangbo-v35-p25a-main-variant-20260814"
jar="$root/project/jmetal-exec-5.8-jar-with-dependencies.jar"
main="org.uma.jmetal.runner.lc_psode.ZhangBoV35P25AReportRunner"
seeds=(20260809 20260810 20260811 20260812 20260813)
arms=(A0 A4 A5)

while true; do
  complete=0
  for seed in "${seeds[@]}"; do
    for arm in "${arms[@]}"; do
      status="$root/results/runs/seed-$seed/$arm/status.properties"
      if [[ -f "$status" ]] && grep -qx 'status=COMPLETED' "$status"; then
        complete=$((complete + 1))
      fi
    done
  done
  echo "$(date -Is) completed=$complete/15"
  if [[ "$complete" -eq 15 ]]; then
    break
  fi
  if find "$root/results/runs" -type d -name 'failed-*' -print -quit 2>/dev/null | grep -q .; then
    echo "A failed arm exists; report is intentionally blocked." >&2
    exit 1
  fi
  sleep 30
done

java -Xmx4g -cp "$jar" "$main" \
  --runs-root "$root/results" --output "$root/results/report"

