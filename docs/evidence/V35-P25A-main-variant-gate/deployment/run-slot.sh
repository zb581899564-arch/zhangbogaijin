#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 4 ]]; then
  echo "usage: run-slot.sh <slot> <cpuset> <arm1> <arm2> [arm3]" >&2
  exit 2
fi

slot="$1"
cpuset="$2"
shift 2
root="/home/inspur/aicomp/zhangbo-v35-p25a-main-variant-20260814"
jar="$root/project/jmetal-exec-5.8-jar-with-dependencies.jar"
main="org.uma.jmetal.runner.lc_psode.ZhangBoV35P25ARunner"

for arm in "$@"; do
  log="$root/logs/seed-slot-${slot}-${arm}.log"
  echo "START slot=$slot arm=$arm cpuset=$cpuset at=$(date -Is)" | tee -a "$log"
  taskset -c "$cpuset" java -Xmx4g -cp "$jar" "$main" \
    --seed-slot "$slot" --arm "$arm" \
    --project-root "$root/project" --output "$root/results" \
    >>"$log" 2>&1
  echo "DONE slot=$slot arm=$arm at=$(date -Is)" | tee -a "$log"
done

