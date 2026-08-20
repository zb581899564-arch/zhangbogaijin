#!/usr/bin/env bash
set -euo pipefail

experiment_root=/home/inspur/aicomp/zhangbo-p86-pair-100k-20260811
project_root="$experiment_root/project"
output_root="$experiment_root/results"
app="$project_root/app.jar"
runtime_main=org.uma.jmetal.runner.lc_psode.ZhangBoP83RuntimeGateRunner
report_main=org.uma.jmetal.runner.lc_psode.ZhangBoP9SingleComparisonRunner

mkdir -p "$output_root"
date -Is > "$experiment_root/started-at.txt"
sha256sum "$app" \
  "$project_root/EADHFSP/20_2_3_1.txt" \
  "$project_root/instance-extensions/v1/20_2_3_1.setup.txt" \
  "$project_root/instance-extensions/v1/MANIFEST.txt" \
  "$project_root/fatigue-parameters/v1/20_2_3_1.fatigue.txt" \
  "$project_root/fatigue-parameters/v1/MANIFEST.txt" \
  > "$experiment_root/input-sha256.txt"

/usr/bin/time -v taskset -c 20-23 java -Xmx4g -cp "$app" "$runtime_main" \
  FULL "$project_root" "$output_root" 100000 \
  > "$experiment_root/full-100k.console.log" 2>&1

/usr/bin/time -v taskset -c 20-23 java -Xmx4g -cp "$app" "$runtime_main" \
  HMOPSO_QGS_F "$project_root" "$output_root" 100000 \
  > "$experiment_root/base-100k.console.log" 2>&1

taskset -c 20-23 java -Xmx4g -cp "$app" "$report_main" \
  --phase REPORT --project-root "$project_root" --output "$output_root" \
  > "$experiment_root/report.console.log" 2>&1

date -Is > "$experiment_root/finished-at.txt"
printf 'COMPLETED\n' > "$experiment_root/status.txt"
