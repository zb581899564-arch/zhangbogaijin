#!/usr/bin/env bash
set -uo pipefail

pilot_root="${1:-/home/inspur/aicomp/zhangbo-java-p9-pilot-20260810}"
output_root="${pilot_root}/results/p9-pilot-20260810"

cd "${pilot_root}"
export LC_ALL=C.UTF-8

{
  date -Is
  java -version 2>&1
  sha256sum app.jar EADHFSP/20_2_3_1.txt
} > results/preflight.txt

taskset -c 0-3 java -Xms512m -Xmx4g -cp app.jar \
  org.uma.jmetal.runner.lc_psode.ZhangBoP9PilotRunner "${output_root}" \
  > results/console.log 2>&1
pilot_status=$?

printf '%s\n' "${pilot_status}" > results/exit-code.txt
date -Is > results/finished-at.txt
exit "${pilot_status}"
