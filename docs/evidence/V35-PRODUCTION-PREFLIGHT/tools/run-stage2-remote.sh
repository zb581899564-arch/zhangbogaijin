#!/usr/bin/env bash
# Stage2 Track C remote scheduler.  It is diagnostic-only: all workloads are
# explicitly marked non-formal and it never enumerates a paper matrix.
set -euo pipefail

ROOT="${1:?remote diagnostic root required}"
EXPECTED_JAR_SHA256="${2:?final freeze jar sha256 required}"
PHASE="${3:-all}" # preflight | benchmark | all
JAR="$ROOT/jmetal-exec-5.8-jar-with-dependencies.jar"
DRIVER_CLASSES="$ROOT/driver-classes"
PHASE_BUDGET_ADAPTER="$ROOT/v35_phase_budget_master_adapter.py"
PROJECT_ROOT="$ROOT/project"
EVIDENCE="$ROOT/evidence"
MAIN="V35ProductionPreflight"
DIAGNOSTIC_INSTANCE="20_2_3_1"
PREFLIGHT_SEED=20260828
BENCHMARK_SEED_BASE=2026082800
JAVA_BIN="${JAVA_BIN:-java}"

[[ "$PHASE" =~ ^(preflight|benchmark|all)$ ]] || {
  echo "unsupported phase=$PHASE" >&2; exit 64;
}
[[ -f "$JAR" && -d "$DRIVER_CLASSES" && -d "$PROJECT_ROOT/java-jmetal58" ]] || {
  echo "stage2 deployment incomplete under $ROOT" >&2; exit 65;
}
ACTUAL_JAR_SHA256="$(sha256sum "$JAR" | awk '{print $1}')"
[[ "$ACTUAL_JAR_SHA256" == "$EXPECTED_JAR_SHA256" ]] || {
  echo "jar SHA mismatch expected=$EXPECTED_JAR_SHA256 actual=$ACTUAL_JAR_SHA256" >&2; exit 66;
}
mkdir -p "$EVIDENCE/preflight" "$EVIDENCE/benchmark"

capture_host() {
  local target="$1"
  {
    echo "capturedAt=$(date -Is)"
    echo "hostname=$(hostname)"
    echo "nproc=$(nproc)"
    echo "jarSha256=$ACTUAL_JAR_SHA256"
    echo "java=$($JAVA_BIN -version 2>&1 | head -n 1)"
    free -b
    echo "--- swap ---"
    swapon --show 2>/dev/null || true
    echo "--- load ---"
    cat /proc/loadavg
    echo "--- vmstat ---"
    vmstat 1 3 2>/dev/null || true
  } >"$target"
}

run_one() {
  local output="$1" cpu="$2" seed="$3" max_fes="$4" purpose="$5"
  local evidence_out="$output/evidence"
  [[ ! -e "$output" ]] || { echo "refusing overwrite $output" >&2; return 70; }
  mkdir -p "$output"
  {
    echo "cpu=$cpu"
    echo "seed=$seed"
    echo "requestedFE=$max_fes"
    echo "purpose=$purpose"
    echo "startedAt=$(date -Is)"
    taskset -c "$cpu" true
  } >"$output/launch.properties"
  set +e
  /usr/bin/time -f 'elapsed_seconds=%e\nuser_seconds=%U\nsystem_seconds=%S\ncpu_percent=%P\nmax_rss_kb=%M' \
    -o "$output/resource-time.properties" \
    taskset -c "$cpu" "$JAVA_BIN" -Xms512m -Xmx4g \
      -Xlog:gc*:file="$output/gc.log":time,uptime,level,tags \
      -cp "$JAR:$DRIVER_CLASSES" "$MAIN" \
      --project-root "$PROJECT_ROOT" --output "$evidence_out" --arm A4 \
      --seed "$seed" --max-fes "$max_fes" --purpose "$purpose" \
      --jar-sha256 "$ACTUAL_JAR_SHA256" --freeze-binding FINAL_FREEZE_BOUND \
      >"$output/console.log" 2>&1
  local exit_code=$?
  set -e
  {
    echo "finishedAt=$(date -Is)"
    echo "exitCode=$exit_code"
    test -f "$evidence_out/preflight-gate.properties" && cat "$evidence_out/preflight-gate.properties" || true
  } >"$output/result.properties"
  [[ "$exit_code" -eq 0 ]] || return "$exit_code"
  grep -Fxq 'gateStatus=PASS' "$evidence_out/preflight-gate.properties" || return 71
  grep -Fxq "requestedFE=$max_fes" "$evidence_out/preflight-gate.properties" || return 72
  grep -Fxq 'phaseBoundAccepted=true' "$evidence_out/budget-termination.properties" || return 73
}

run_preflight() {
  local root="$EVIDENCE/preflight"
  [[ ! -e "$root/STARTED" ]] || { echo "preflight already started: $root" >&2; return 74; }
  [[ -f "$PHASE_BUDGET_ADAPTER" ]] || { echo "phase budget adapter missing: $PHASE_BUDGET_ADAPTER" >&2; return 75; }
  touch "$root/STARTED"
  capture_host "$root/host-before.txt"
  for arm in A0 A1 A2 A3 A4; do
    local output="$root/$arm"
    [[ ! -e "$output" ]] || { echo "refusing pre-existing arm evidence $output" >&2; return 75; }
    mkdir -p "$output"
    set +e
    /usr/bin/time -f 'elapsed_seconds=%e\nuser_seconds=%U\nsystem_seconds=%S\ncpu_percent=%P\nmax_rss_kb=%M' \
      -o "$output/resource-time.properties" \
      taskset -c 0 "$JAVA_BIN" -Xms512m -Xmx4g \
        -Xlog:gc*:file="$output/gc.log":time,uptime,level,tags \
        -cp "$JAR:$DRIVER_CLASSES" "$MAIN" \
        --project-root "$PROJECT_ROOT" --output "$output/evidence" --arm "$arm" \
        --seed "$PREFLIGHT_SEED" --max-fes 50000 --purpose PREFLIGHT \
        --jar-sha256 "$ACTUAL_JAR_SHA256" --freeze-binding FINAL_FREEZE_BOUND \
        >"$output/console.log" 2>&1
    local exit_code=$?
    set -e
    {
      echo "arm=$arm"
      echo "exitCode=$exit_code"
      test -f "$output/evidence/preflight-gate.properties" && cat "$output/evidence/preflight-gate.properties" || true
    } >"$output/result.properties"
    if [[ "$exit_code" -ne 0 ]] || ! grep -Fxq 'gateStatus=PASS' "$output/evidence/preflight-gate.properties"; then
      echo "PRECHECK_STOPPED_AT=$arm" >"$root/STOPPED.properties"
      capture_host "$root/host-after-failure.txt"
      return 76
    fi
  done
  awk -F= '/^initialPopulationHash=/{print $2}' "$root"/*/evidence/status.properties | sort -u \
    >"$root/initial-population-hashes.txt"
  [[ "$(wc -l < "$root/initial-population-hashes.txt")" -eq 1 ]] || {
    echo "INITIAL_POPULATION_HASH_DRIFT" >"$root/STOPPED.properties"; return 77;
  }
  python3 "$PHASE_BUDGET_ADAPTER" --audit-preflight-root "$root" \
    --group-report "$root/group-budget-audit" >"$root/group-budget-audit.json" || {
      echo "PHASE_BUDGET_GROUP_AUDIT_FAILED" >"$root/STOPPED.properties"; return 79;
    }
  capture_host "$root/host-after.txt"
  echo "PRECHECK_ACCEPTED_PENDING_INDEPENDENT_REVIEW" >"$root/COMPLETE.properties"
}

run_level() {
  local level="$1" root="$EVIDENCE/benchmark/L$1"
  [[ ! -e "$root" ]] || { echo "refusing pre-existing level evidence $root" >&2; return 78; }
  mkdir -p "$root"
  capture_host "$root/host-before.txt"
  local start_ns; start_ns="$(date +%s%N)"
  local pids=() lane
  for ((lane=0; lane<level; lane++)); do
    run_one "$root/run-$lane" "$lane" "$((BENCHMARK_SEED_BASE + level * 100 + lane))" \
      20000 THROUGHPUT &
    pids+=("$!")
  done
  local failures=0 pid
  for pid in "${pids[@]}"; do wait "$pid" || failures=$((failures + 1)); done
  local finish_ns; finish_ns="$(date +%s%N)"
  capture_host "$root/host-after.txt"
  local completed=0 actual_fe=0 file
  while IFS= read -r file; do
    if grep -Fxq 'gateStatus=PASS' "$file"; then
      completed=$((completed + 1))
      actual_fe=$((actual_fe + $(awk -F= '$1=="actualFE" {print $2}' "$file")))
    fi
  done < <(find "$root" -path '*/evidence/preflight-gate.properties' -type f | sort)
  local elapsed_ns=$((finish_ns - start_ns))
  awk -v level="$level" -v requested="$level" -v completed="$completed" -v failures="$failures" \
      -v fe="$actual_fe" -v start="$start_ns" -v finish="$finish_ns" -v elapsed="$elapsed_ns" \
      -v jar="$ACTUAL_JAR_SHA256" 'BEGIN {
        sec=elapsed/1000000000.0; runsHour=(sec>0 ? completed*3600.0/sec : 0); feHour=(sec>0 ? fe*3600.0/sec : 0);
        print "parallelism=" level "\nrequestedRuns=" requested "\ncompletedRuns=" completed "\nfailures=" failures \
          "\ntotalFE=" fe "\nwallSeconds=" sec "\nrunsPerHour=" runsHour "\nfePerHour=" feHour \
          "\njarSha256=" jar "\nstatus=" ((completed==requested && failures==0) ? "PASS_PENDING_REVIEW" : "FAIL")
      }' >"$root/level-summary.properties"
  [[ "$completed" -eq "$level" && "$failures" -eq 0 ]] || return 79
}

run_benchmark() {
  local level
  for level in 4 8 12 16; do run_level "$level"; done
  echo "BENCHMARK_COMPLETED_PENDING_INDEPENDENT_REVIEW" >"$EVIDENCE/benchmark/COMPLETE.properties"
}

case "$PHASE" in
  preflight) run_preflight ;;
  benchmark) run_benchmark ;;
  all) run_preflight && run_benchmark ;;
esac
