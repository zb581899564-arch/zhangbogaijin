#!/bin/bash
set -euo pipefail
ROOT=/home/inspur/aicomp/zhangbo-v35-source-attribution-observer-v5-gate-20260901
cd "$ROOT"
mkdir -p results logs
CP="jars/jmetal-algorithm-5.8-V35-SOURCE-ATTRIBUTION-OBSERVER-V5.jar:jars/formal-algorithm-8DAD8F40.jar"
SNAP="snapshots/100_5_3_1-seed-20260901.fourvec"
for MODE in OFF ON; do
  OUT="results/obs20k-$MODE"
  if [ -e "$OUT" ]; then
    echo "REFUSE_EXISTING $OUT" >&2
    exit 30
  fi
  echo "START $MODE $(date -Is)" | tee -a logs/run.log
  set +e
  nice -n 10 java -Xms1g -Xmx4g -cp "$CP" \
    org.uma.jmetal.runner.lc_psode.V35ObserverGateRunner \
    --instance 100_5_3_1 --seed 20260901 --profile C0 --max-fes 20000 \
    --snapshot "$SNAP" --output "$OUT" --telemetry "$MODE" \
    --checkpoints 5000,10000,15000 \
    > "logs/$MODE.log" 2>&1
  RC=$?
  set -e
  echo "END $MODE exit=$RC $(date -Is)" | tee -a logs/run.log
  if [ "$RC" -ne 0 ]; then exit "$RC"; fi
done
echo "DONE $(date -Is)" | tee -a logs/run.log
