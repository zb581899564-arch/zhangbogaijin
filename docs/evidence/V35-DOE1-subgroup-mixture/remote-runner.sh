#!/usr/bin/env bash
set -u

# V35-DOE-1 remote campaign.  This script is uploaded with the fat jar and
# frozen instance resources; it does not alter the algorithm or construct a
# reference front.  A failed JVM is retained and the worker continues.
ROOT="${1:?remote project root}"
JAR="$ROOT/jmetal-exec-5.8-jar-with-dependencies.jar"
OUT="$ROOT/docs/evidence/V35-DOE1-subgroup-mixture/04-development-runs"
mkdir -p "$OUT/logs"
java -version 2>"$OUT/logs/java-version.txt"
sha256sum "$JAR" > "$OUT/logs/package.sha256"

instances=(20_2_3_1 50_2_3_1 100_2_3_1)
seeds=(20260822 20260823 20260824)
ranges=(0-3 4-7 8-11 12-15 16-19)

run_worker() {
  local worker="$1"; local cpu="${ranges[$worker]}"
  local log="$OUT/logs/worker-$worker.log"
  : > "$log"
  for ((t=worker; t<15; t+=5)); do
    for instance in "${instances[@]}"; do
      for seed in "${seeds[@]}"; do
        echo "START treatment=$t instance=$instance seed=$seed cpu=$cpu" >> "$log"
        taskset -c "$cpu" java -Xmx4g -XX:+UseG1GC -cp "$JAR" \
          org.uma.jmetal.runner.lc_psode.ZhangBoV35Doe1MixtureRunner \
          --phase RUN --project-root "$ROOT" --output "$OUT" \
          --treatment "$t" --instance "$instance" --seed "$seed" --max-fes 500000 \
          >> "$log" 2>&1
        rc=$?
        echo "END treatment=$t instance=$instance seed=$seed rc=$rc" >> "$log"
      done
    done
  done
}

for worker in 0 1 2 3 4; do run_worker "$worker" & done
wait

find "$OUT" -name status.properties -type f | sort > "$OUT/status-files.txt"
count=$(wc -l < "$OUT/status-files.txt")
completed=$(grep -h '^status=COMPLETED$' $(cat "$OUT/status-files.txt") 2>/dev/null | wc -l)
if [ "$count" -eq 135 ] && [ "$completed" -eq 135 ]; then
  printf 'status=COMPLETED\nruns=135\ncompleted=135\n' > "$OUT/campaign-complete.properties"
else
  printf 'status=INCOMPLETE\nstatusFiles=%s\ncompleted=%s\n' "$count" "$completed" > "$OUT/campaign-incomplete.properties"
fi
sha256sum "$OUT"/logs/* "$OUT"/campaign-*.properties 2>/dev/null > "$OUT/evidence-sha256.tsv" || true
