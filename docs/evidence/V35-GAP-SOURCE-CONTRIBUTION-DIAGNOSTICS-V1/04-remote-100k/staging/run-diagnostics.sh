#!/bin/bash
# V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1: 6 x 100k diagnostic runs (C0, 2 instances, 3 seeds).
# Frozen prereg 00-preregistration/. Telemetry ON; classpath V3:FORMAL (shadowed engine).
set -uo pipefail
R=/home/inspur/aicomp/zhangbo-v35-source-diagnostics-20260831
cd "$R" || exit 1
mkdir -p logs
echo "RESOURCE_PRECHECK $(date -Is)" | tee -a logs/run-diagnostics.log
java -version 2>&1 | head -1 | tee -a logs/run-diagnostics.log
free -g | head -2 | tee -a logs/run-diagnostics.log
uptime | tee -a logs/run-diagnostics.log

run_one() {
  local seed="$1" inst="$2"
  local w="$R"
  local out="results/run-GAPLSRC-C0-${inst}-${seed}"
  local log="$R/logs/C0-${inst}-${seed}.log"
  if [ -d "$w/$out" ]; then echo "SKIP existing $out" | tee -a "$R/logs/run-diagnostics.log"; return 0; fi
  echo "START C0 $inst $seed $(date -Is)" | tee -a "$R/logs/run-diagnostics.log"
  ( cd "$w" && nice -n 10 java -Xmx4g \
      -cp "jars/jmetal-algorithm-5.8-V35-SOURCE-DIAGNOSTICS-V3.jar:jars/formal-algorithm-8DAD8F40.jar" \
      org.uma.jmetal.runner.lc_psode.V35SourceDiagnosticRunner \
      --instance "$inst" --seed "$seed" --profile C0 --max-fes 100000 \
      --snapshot "snapshots/${inst}-seed-${seed}.fourvec" --output "$out" \
      --telemetry ON --checkpoints 25000,50000,75000 ) > "$log" 2>&1
  echo "END C0 $inst $seed exit=$? $(date -Is)" | tee -a "$R/logs/run-diagnostics.log"
  return 0
}

# runner reads bindings/<instance>.binding.properties relative to CWD: run per seed dir
run_in_seed() {
  local seed="$1" inst="$2"
  local w="$R/seed-$seed"
  local out="results/run-GAPLSRC-C0-${inst}-${seed}"
  local log="$R/logs/C0-${inst}-${seed}.log"
  if [ -d "$w/$out" ]; then echo "SKIP existing $out" | tee -a "$R/logs/run-diagnostics.log"; return 0; fi
  echo "START C0 $inst $seed $(date -Is)" | tee -a "$R/logs/run-diagnostics.log"
  ( cd "$w" && nice -n 10 java -Xmx4g \
      -cp "jars/jmetal-algorithm-5.8-V35-SOURCE-DIAGNOSTICS-V3.jar:jars/formal-algorithm-8DAD8F40.jar" \
      org.uma.jmetal.runner.lc_psode.V35SourceDiagnosticRunner \
      --instance "$inst" --seed "$seed" --profile C0 --max-fes 100000 \
      --snapshot "snapshots/${inst}-seed-${seed}.fourvec" --output "$out" \
      --telemetry ON --checkpoints 25000,50000,75000 ) > "$log" 2>&1
  echo "END C0 $inst $seed exit=$? $(date -Is)" | tee -a "$R/logs/run-diagnostics.log"
  return 0
}

run_in_seed 20260919 50_2_3_1 &
run_in_seed 20260919 100_5_3_1 &
run_in_seed 20260920 50_2_3_1 &
run_in_seed 20260920 100_5_3_1 &
run_in_seed 20260921 50_2_3_1 &
run_in_seed 20260921 100_5_3_1 &
wait

echo "ALL_6_RUNS_DONE $(date -Is)" | tee -a logs/run-diagnostics.log
for d in seed-*/results/run-*; do
  g="$d/formal-gate.properties"
  if [ -f "$g" ]; then
    echo "$(basename "$d"): $(grep '^status=' "$g" | tr -d '\n') $(grep '^actualFE=' "$g" | tr -d '\n') $(grep '^failures=' "$g" | tr -d '\n')"
  else
    echo "$(basename "$d"): NO_GATE_FILE"
  fi
done | tee -a logs/run-diagnostics.log
