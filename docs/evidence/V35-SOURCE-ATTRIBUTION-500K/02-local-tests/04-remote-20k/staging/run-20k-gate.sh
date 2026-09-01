#!/bin/bash
# V35 SOURCE-ATTRIBUTION Observer 20k OFF/ON gate (2 runs, serial).
set -uo pipefail
cd /home/inspur/aicomp/zhangbo-v35-source-attribution-observer-gate-20260901 || exit 1
mkdir -p results logs
CP="jars/jmetal-algorithm-5.8-V35-SOURCE-ATTRIBUTION-OBSERVER-V4.jar:jars/formal-algorithm-8DAD8F40.jar"
SNAP="snapshots/100_5_3_1-seed-20260901.fourvec"
for MODE in OFF ON; do
  TEL=$([ "$MODE" = "ON" ] && echo "ON" || echo "OFF")
  OUT="results/obs20k-$MODE"
  if [ -d "$OUT" ]; then echo "SKIP $OUT"; continue; fi
  echo "START $MODE $(date -Is)" | tee -a logs/run.log
  nice -n 10 java -Xms1g -Xmx4g -cp "$CP" \
    org.uma.jmetal.runner.lc_psode.V35ObserverGateRunner \
    --instance 100_5_3_1 --seed 20260901 --profile C0 --max-fes 20000 \
    --snapshot "$SNAP" --output "$OUT" --telemetry $TEL --checkpoints 5000,10000,15000 \
    > "logs/$MODE.log" 2>&1
  echo "END $MODE exit=$? $(date -Is)" | tee -a logs/run.log
done
echo "DONE $(date -Is)" | tee -a logs/run.log
