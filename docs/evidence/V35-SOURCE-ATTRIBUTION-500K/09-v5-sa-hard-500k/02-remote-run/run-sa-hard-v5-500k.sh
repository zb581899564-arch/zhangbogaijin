#!/bin/bash
# V35-SOURCE-ATTRIBUTION-500K / 09-v5-sa-hard-500k — SA-HARD V5 500k
# campaign=V35-SOURCE-ATTRIBUTION-V5-SA-HARD-500K
# runKey=SA-HARD-V5 instance=100_5_3_1 seed=20260901 arm=A4(profile C0) MaxFEs=500000 observer=ON
# Single JVM, nice -n 10, -Xms1g -Xmx4g (heap must NOT be enlarged to mask observer problems).
# Classpath order: V5 observer jar FIRST, formal jar SECOND.
# Observer V5 (schema v35-source-attribution-observer-schema-v2) saves the terminal 500000
# snapshot itself; the 19 configured checkpoints cover the 25k nominal grid up to 475000.
# Atomic publish (.partial-<name> -> <name>) is performed inside V35ObserverGateRunner.
set -uo pipefail
cd /home/inspur/aicomp/zhangbo-v35-source-attribution-v5-sa-hard-500k-20260901 || exit 1
mkdir -p results logs

CP="jars/jmetal-algorithm-5.8-V35-SOURCE-ATTRIBUTION-OBSERVER-V5.jar:jars/formal-algorithm-8DAD8F40.jar"
SNAP="snapshots/100_5_3_1-seed-20260901.fourvec"
OUT="results/SA-HARD-V5-500k"
CKPT="25000,50000,75000,100000,125000,150000,175000,200000,225000,250000,275000,300000,325000,350000,375000,400000,425000,450000,475000"

{
  echo "campaign=V35-SOURCE-ATTRIBUTION-V5-SA-HARD-500K"
  echo "host=$(hostname)"
  echo "startIso=$(date -Is)"
  echo "javaVersion=$(java -version 2>&1 | head -1)"
  echo "osVersion=$(uname -a)"
  echo "nproc=$(nproc)"
  echo "niceLevel=10"
  echo "javaHeap=-Xms1g -Xmx4g"
  echo "classpathOrder=observerV5First,formalSecond"
  echo "observerSchema=v35-source-attribution-observer-schema-v2"
  echo "checkpointTargets=$CKPT"
  echo "terminalSnapshot=saved by runner separately (20 non-B0 = 19 checkpoints + 1 terminal; B0 separate)"
  echo "observerJarSha256=$(sha256sum jars/jmetal-algorithm-5.8-V35-SOURCE-ATTRIBUTION-OBSERVER-V5.jar | cut -d' ' -f1)"
  echo "formalJarSha256=$(sha256sum jars/formal-algorithm-8DAD8F40.jar | cut -d' ' -f1)"
  echo "snapshotSha256=$(sha256sum "$SNAP" | cut -d' ' -f1)"
  echo "instanceSha256=$(sha256sum inputs/java-jmetal58/EADHFSP/100_5_3_1.txt | cut -d' ' -f1)"
  echo "setupFileSha256=$(sha256sum inputs/java-jmetal58/instance-extensions/v1/100_5_3_1.setup.txt | cut -d' ' -f1)"
  echo "fatigueFileSha256=$(sha256sum inputs/java-jmetal58/fatigue-parameters/v1/100_5_3_1.fatigue.txt | cut -d' ' -f1)"
} > logs/launch-env.properties

if [ -d "$OUT" ]; then
  echo "REFUSING_TO_OVERWRITE $OUT $(date -Is)" | tee -a logs/run.log
  echo "2" > logs/exitcode.txt
  exit 2
fi

echo "START SA-HARD-V5 $(date -Is)" | tee -a logs/run.log
nice -n 10 java -Xms1g -Xmx4g -cp "$CP" \
  org.uma.jmetal.runner.lc_psode.V35ObserverGateRunner \
  --instance 100_5_3_1 --seed 20260901 --profile C0 --max-fes 500000 \
  --snapshot "$SNAP" --output "$OUT" --telemetry ON --checkpoints "$CKPT" \
  > "logs/SA-HARD-V5.log" 2>&1
RC=$?
echo "END SA-HARD-V5 exit=$RC $(date -Is)" | tee -a logs/run.log
echo "$RC" > logs/exitcode.txt
{
  echo "endIso=$(date -Is)"
  echo "processExitCode=$RC"
  echo "formalJarSha256AfterRun=$(sha256sum jars/formal-algorithm-8DAD8F40.jar | cut -d' ' -f1)"
  echo "observerJarSha256AfterRun=$(sha256sum jars/jmetal-algorithm-5.8-V35-SOURCE-ATTRIBUTION-OBSERVER-V5.jar | cut -d' ' -f1)"
  echo "logSizeBytes=$(stat -c %s logs/SA-HARD-V5.log)"
  echo "resultDirDiskBytes=$(du -sb "$OUT" | cut -f1)"
} > logs/run-closeout.properties
echo "DONE $(date -Is)" | tee -a logs/run.log
exit $RC
