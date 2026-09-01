#!/bin/bash
# V35-LOCAL-FE-PACING 50k dose-resolution & performance screen (16 runs, frozen prereg 2026-08-31).
# Fair group = one seed x one instance x 4 arms (C0..C3), max 4 parallel JVMs, groups sequential.
# Resource etiquette: nice -n 10, -Xmx4g, no GPU. Scheduler runs on the training machine.
set -uo pipefail
R=/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-50k-20260831
cd "$R" || exit 1
mkdir -p logs
echo "RESOURCE_PRECHECK $(date -Is)" | tee -a logs/run-all-50k.log
java -version 2>&1 | head -1 | tee -a logs/run-all-50k.log
free -g | head -2 | tee -a logs/run-all-50k.log
df -h /home/inspur | tail -1 | tee -a logs/run-all-50k.log
uptime | tee -a logs/run-all-50k.log

run_arm() {
  local seed="$1" inst="$2" profile="$3"
  local w="$R/seed-$seed"
  local out="results/run-GAPL50K-${profile}-${inst}-${seed}"
  local log="$R/logs/${profile}-${inst}-${seed}.log"
  if [ -d "$w/$out" ]; then echo "SKIP existing $out" | tee -a "$R/logs/run-all-50k.log"; return 0; fi
  echo "START $profile $inst $seed $(date -Is)" | tee -a "$R/logs/run-all-50k.log"
  ( cd "$w" && nice -n 10 java -Xmx4g \
      -cp "jars/formal-algorithm-8DAD8F40.jar:jars/jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar" \
      org.uma.jmetal.runner.lc_psode.V35LocalFePacingRepairRunner \
      --instance "$inst" --seed "$seed" --profile "$profile" --max-fes 50000 \
      --snapshot "snapshots/${inst}-seed-${seed}.fourvec" --output "$out" ) > "$log" 2>&1
  local rc=$?
  echo "END $profile $inst $seed exit=$rc $(date -Is)" | tee -a "$R/logs/run-all-50k.log"
  return 0
}

for seed in 20260907 20260914; do
  for inst in 50_2_3_1 100_5_3_1; do
    echo "=== FAIR GROUP seed=$seed inst=$inst ===" | tee -a logs/run-all-50k.log
    for profile in C0 C1 C2 C3; do
      run_arm "$seed" "$inst" "$profile" &
    done
    wait
  done
done

echo "ALL_16_RUNS_DONE $(date -Is)" | tee -a logs/run-all-50k.log
for d in seed-*/results/run-*; do
  g="$d/formal-gate.properties"
  if [ -f "$g" ]; then
    echo "$(basename "$d"): $(grep '^status=' "$g" | tr -d '\n') $(grep '^actualFE=' "$g" | tr -d '\n') $(grep '^failures=' "$g" | tr -d '\n')"
  else
    echo "$(basename "$d"): NO_GATE_FILE"
  fi
done | tee -a logs/run-all-50k.log
