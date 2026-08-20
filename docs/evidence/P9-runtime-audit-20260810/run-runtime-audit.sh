#!/usr/bin/env bash
set -euo pipefail

audit_root=/home/inspur/aicomp/zhangbo-runtime-audit-100k-20260810
project_root=/home/inspur/aicomp/zhangbo-java-p9-five-additional-500k-20260810
main=org.uma.jmetal.runner.lc_psode.RuntimeAuditRunner
classpath="$audit_root/classes:$project_root/app.jar"

date -Is > "$audit_root/started-at.txt"
taskset -c 20-23 java -Xmx4g \
  -XX:StartFlightRecording="filename=$audit_root/full-100k.jfr,settings=profile,dumponexit=true" \
  -cp "$classpath" "$main" FULL "$project_root" "$audit_root/results" 100000 \
  > "$audit_root/full-100k.console.log" 2>&1
taskset -c 20-23 java -Xmx4g \
  -XX:StartFlightRecording="filename=$audit_root/base-100k.jfr,settings=profile,dumponexit=true" \
  -cp "$classpath" "$main" HMOPSO_QGS_F "$project_root" "$audit_root/results" 100000 \
  > "$audit_root/base-100k.console.log" 2>&1
date -Is > "$audit_root/finished-at.txt"
