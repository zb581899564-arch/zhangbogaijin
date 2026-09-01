#!/usr/bin/env bash
set -euo pipefail

ROOT=/home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825
JAR="$ROOT/bin/fc5-transfer-diagnostic.jar"
PROJECT="$ROOT/project-root"
CLASS=org.uma.jmetal.runner.lc_psode.ZhangBoV35Fc5TransferRunner
TASKS="$ROOT/first-tier-50k-tasks.tsv"

: > "$TASKS"
for instance in 100_2_5_1 100_8_3_1; do
  for seed in 20260911 20260912 20260913; do
    printf '%s\t%s\t%s\n' "$instance" "$seed" A0_BASELINE >> "$TASKS"
    printf '%s\t%s\t%s\n' "$instance" "$seed" A2_CFVF >> "$TASKS"
  done
done
for instance in 100_2_4_1 100_5_3_1; do
  for seed in 20260901 20260902 20260903; do
    printf '%s\t%s\t%s\n' "$instance" "$seed" A2_CFVF >> "$TASKS"
    printf '%s\t%s\t%s\n' "$instance" "$seed" A4_BUDGET_AWARE_CATA >> "$TASKS"
  done
done

run_one() {
  local instance=$1
  local seed=$2
  local arm=$3
  local snapshot="$ROOT/input/snapshots/$instance/seed-$seed.fourvec"
  local output="$ROOT/output/50k/$instance/seed-$seed/$arm"
  local log="$ROOT/logs/50k-$instance-$seed-$arm.log"
  if [[ -e "$output" ]]; then
    printf 'refusing existing output: %s\n' "$output" >&2
    return 20
  fi
  mkdir -p "$(dirname "$output")"
  java -Xmx4g -cp "$JAR" "$CLASS" \
    --project-root "$PROJECT" \
    --output "$output" \
    --snapshot "$snapshot" \
    --instance "$instance" \
    --seed "$seed" \
    --arm "$arm" \
    --max-fes 50000 > "$log" 2>&1
}
export ROOT JAR PROJECT CLASS
export -f run_one

xargs -P 12 -n 3 bash -c 'run_one "$1" "$2" "$3"' _ < "$TASKS"

python3 - "$ROOT" <<'PY'
from pathlib import Path
import sys

root = Path(sys.argv[1])
tasks = [line.split() for line in (root / "first-tier-50k-tasks.tsv").read_text().splitlines() if line.strip()]
if len(tasks) != 24:
    raise SystemExit("task roster is not exactly 24")
for instance, seed, arm in tasks:
    run = root / "output" / "50k" / instance / f"seed-{seed}" / arm
    props = {}
    for line in (run / "status.properties").read_text().splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            props[key] = value
    actual = int(props["fullEvaluations"])
    decoder = int(props["decoderCalls"])
    if props.get("status") != "COMPLETED" or not (0 < actual <= 50000) or decoder != actual:
        raise SystemExit(f"acceptance failed: {run}")
    if int(props.get("illegalSolutions", "-1")) != 0 or int(props.get("duplicateEvaluations", "-1")) != 0:
        raise SystemExit(f"solution accounting failed: {run}")
    for required in (
        "fc5-transfer-merge-rounds.csv",
        "fc5-transfer-windowed-merge-overflow.csv",
        "fc5-transfer-directional-representative-lifecycle.csv",
        "fc5-transfer-archive-working-gap.csv",
        "evidence-sha256.tsv",
    ):
        if not (run / required).is_file():
            raise SystemExit(f"missing {required}: {run}")
(root / "FIRST_TIER_50K_COMPLETED.properties").write_text(
    "status=COMPLETED\nruns=24\nmaxFEs=50000\nautoEscalation=false\n",
    encoding="utf-8",
)
PY
