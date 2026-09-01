#!/bin/bash
# V35-LOCAL-FE-PACING 250k C2/C3 confirmation with full-front checkpoints (18 runs).
# Frozen prereg 15-250k-preregistration/ (2026-08-31). Arms C0/C2/C3, seeds 20260916-18.
# Fair group = one seed x one instance x 3 arms; 2 groups in parallel (max 6 JVMs);
# nice -n 10, -Xmx4g, no GPU. CLASSPATH ORDER IS V2-FIRST (shadowed observer classes).
set -uo pipefail
R=/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-250k-20260831
cd "$R" || exit 1
mkdir -p logs
echo "RESOURCE_PRECHECK $(date -Is)" | tee -a logs/run-all-250k.log
java -version 2>&1 | head -1 | tee -a logs/run-all-250k.log
free -g | head -2 | tee -a logs/run-all-250k.log
df -h /home/inspur | tail -1 | tee -a logs/run-all-250k.log
uptime | tee -a logs/run-all-250k.log

run_arm() {
  local seed="$1" inst="$2" arm="$3"
  local w="$R/seed-$seed"
  local out="results/run-GAPL250K-${arm}-${inst}-${seed}"
  local log="$R/logs/${arm}-${inst}-${seed}.log"
  if [ -d "$w/$out" ]; then echo "SKIP existing $out" | tee -a "$R/logs/run-all-250k.log"; return 0; fi
  echo "START $arm $inst $seed $(date -Is)" | tee -a "$R/logs/run-all-250k.log"
  ( cd "$w" && nice -n 10 java -Xmx4g \
      -cp "jars/jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-CHECKPOINT-V2.jar:jars/formal-algorithm-8DAD8F40.jar" \
      org.uma.jmetal.runner.lc_psode.V35CheckpointRepairRunner \
      --instance "$inst" --seed "$seed" --profile "$arm" --max-fes 250000 \
      --snapshot "snapshots/${inst}-seed-${seed}.fourvec" --output "$out" --observer ON ) > "$log" 2>&1
  echo "END $arm $inst $seed exit=$? $(date -Is)" | tee -a "$R/logs/run-all-250k.log"
  return 0
}

run_group() {
  local seed="$1" inst="$2"
  echo "=== FAIR GROUP seed=$seed inst=$inst ===" | tee -a logs/run-all-250k.log
  for arm in C0 C2 C3; do
    run_arm "$seed" "$inst" "$arm" &
  done
  wait
}

# two fair groups in parallel at any time; groups 1+2 first, then 3+4, then 5+6
run_group 20260916 50_2_3_1 &
run_group 20260916 100_5_3_1 &
wait
run_group 20260917 50_2_3_1 &
run_group 20260917 100_5_3_1 &
wait
run_group 20260918 50_2_3_1 &
run_group 20260918 100_5_3_1 &
wait

echo "ALL_18_RUNS_DONE $(date -Is)" | tee -a logs/run-all-250k.log
for d in seed-*/results/run-*; do
  g="$d/formal-gate.properties"
  if [ -f "$g" ]; then
    echo "$(basename "$d"): $(grep '^status=' "$g" | tr -d '\n') $(grep '^actualFE=' "$g" | tr -d '\n') $(grep '^failures=' "$g" | tr -d '\n')"
  else
    echo "$(basename "$d"): NO_GATE_FILE"
  fi
done | tee -a logs/run-all-250k.log
