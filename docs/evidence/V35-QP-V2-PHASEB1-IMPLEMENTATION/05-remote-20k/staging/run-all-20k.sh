#!/bin/bash
set -uo pipefail
R=/home/inspur/aicomp/zhangbo-v35-qp-v2-phaseb1-20260902
cd "$R" || exit 1
FORMAL=jars/formal-algorithm-8DAD8F40.jar
EXP=jars/jmetal-algorithm-5.8-V35-QP-V2-PHASEB1.jar
mkdir -p results logs

echo "=== RESOURCE_PRECHECK $(date -Is) ===" | tee -a logs/run-all.log
java -version 2>&1 | head -1 | tee -a logs/run-all.log
free -g | head -2 | tee -a logs/run-all.log
uptime | tee -a logs/run-all.log

run_arm() {
  local inst="$1" seed="$2" profile="$3"
  local out="results/run-${profile}-${inst}-${seed}"
  local log="logs/${profile}-${inst}-${seed}.log"
  if [ -d "$out" ]; then echo "SKIP existing $out"; return 0; fi
  echo "START $profile $inst seed=$seed $(date -Is)" | tee -a logs/run-all.log
  nice -n 10 java -Xmx4g -cp "$EXP:$FORMAL" \
    org.uma.jmetal.runner.lc_psode.V35QpV2ExperimentRunner \
    --instance "$inst" --seed "$seed" --profile "$profile" --max-fes 20000 \
    --snapshot "snapshots/${inst}-seed-${seed}.fourvec" --output "$out" \
    --telemetry ON \
    > "$log" 2>&1
  local rc=$?
  echo "END $profile $inst seed=$seed exit=$rc $(date -Is)" | tee -a logs/run-all.log
  return $rc
}
export -f run_arm

# 1. 20_2_3_1 seed 20260822 (5 arms)
echo "=== GROUP 1: 20_2_3_1 seed 20260822 ===" | tee -a logs/run-all.log
for profile in REF_A4 QP_V2_K1 QP_V2_K2 QP_V2_K3 QP_V2_K4; do
  run_arm "20_2_3_1" "20260822" "$profile" &
done
wait

# 2. 100_5_3_1 seed 20260901 (5 arms)
echo "=== GROUP 2: 100_5_3_1 seed 20260901 ===" | tee -a logs/run-all.log
for profile in REF_A4 QP_V2_K1 QP_V2_K2 QP_V2_K3 QP_V2_K4; do
  run_arm "100_5_3_1" "20260901" "$profile" &
done
wait

echo "=== ALL_10_RUNS_DONE $(date -Is) ===" | tee -a logs/run-all.log

for d in results/run-*; do
  sum="$d/summary.properties"
  if [ -f "$sum" ]; then
    echo "$(basename $d): $(grep '^profile=' $sum) $(grep '^actualFEs=' $sum) $(grep '^frontSize=' $sum) $(grep '^totalExtraRngDraws=' $sum) $(grep '^nonCanonicalSelections=' $sum)"
  fi
done