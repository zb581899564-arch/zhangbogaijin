#!/bin/bash
# V35-GAP-LOCAL-FE-PACING-REPAIR-V1 — 20k mechanism gate (10 runs on the training machine)
# Fair group = one instance x 5 arms (REF_A4, C0, C1, C2, C3), 5 parallel JVMs per group,
# groups run sequentially. Resource limits: nice -n 10, -Xmx4g, no GPU, no taskset restriction
# (5 JVMs on 32 cores is within the standing 4-core-ish etiquette? -- see run note below:
#  the runs are ~15 s each; we keep 5 parallel and nice 10 as approved for this work package).
set -uo pipefail
R=/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-repair-20260831
cd "$R" || exit 1
FORMAL=jars/formal-algorithm-8DAD8F40.jar
EXP=jars/jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar
mkdir -p results logs

echo "RESOURCE_PRECHECK $(date -Is)" | tee -a logs/run-all.log
java -version 2>&1 | head -1 | tee -a logs/run-all.log
free -g | head -2 | tee -a logs/run-all.log
uptime | tee -a logs/run-all.log

run_arm() {
  local inst="$1" profile="$2" seed=20260907
  local out="results/run-${profile}-${inst}-20260907"
  local log="logs/${profile}-${inst}.log"
  if [ -d "$out" ]; then echo "SKIP existing $out"; return 0; fi
  echo "START $profile $inst $(date -Is)" | tee -a logs/run-all.log
  nice -n 10 java -Xmx4g -cp "$FORMAL:$EXP" \
    org.uma.jmetal.runner.lc_psode.V35LocalFePacingRepairRunner \
    --instance "$inst" --seed "$seed" --profile "$profile" --max-fes 20000 \
    --snapshot "snapshots/${inst}-seed-${seed}.fourvec" --output "$out" \
    > "$log" 2>&1
  local rc=$?
  echo "END $profile $inst exit=$rc $(date -Is)" | tee -a logs/run-all.log
  return $rc
}
export -f run_arm

for inst in 50_2_3_1 100_5_3_1; do
  echo "=== FAIR GROUP $inst ===" | tee -a logs/run-all.log
  for profile in REF_A4 C0 C1 C2 C3; do
    run_arm "$inst" "$profile" &
  done
  wait
done

echo "ALL_10_RUNS_DONE $(date -Is)" | tee -a logs/run-all.log
# gate summary
for d in results/run-*; do
  g="$d/formal-gate.properties"
  if [ -f "$g" ]; then
    echo "$(basename $d): $(grep '^status=' $g) $(grep '^actualFE=' $g) $(grep '^failures=' $g)"
  else
    echo "$(basename $d): NO_GATE_FILE"
  fi
done | tee -a logs/run-all.log
