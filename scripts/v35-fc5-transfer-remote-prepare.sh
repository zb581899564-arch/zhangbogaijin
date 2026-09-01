#!/usr/bin/env bash
set -euo pipefail

ROOT=/home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825
SRC=/home/inspur/aicomp/zhangbo-v35-stage2-master-v2-20260823/input/java-project

for inst in 100_2_5_1 100_8_3_1 100_2_4_1 100_5_3_1; do
  cp -p "$SRC/EADHFSP/$inst.txt" "$ROOT/project-root/java-jmetal58/EADHFSP/$inst.txt"
  cp -p "$SRC/instance-extensions/v1/$inst.setup.txt" \
    "$ROOT/project-root/java-jmetal58/instance-extensions/v1/$inst.setup.txt"
  cp -p "$SRC/fatigue-parameters/v1/$inst.fatigue.txt" \
    "$ROOT/project-root/java-jmetal58/fatigue-parameters/v1/$inst.fatigue.txt"
  mkdir -p "$ROOT/input/snapshots/$inst"
done

SRC_A2A4=/home/inspur/aicomp/zhangbo-v35-a2-a4-confirmation-20260824/input/snapshots
for inst in 100_2_4_1 100_5_3_1; do
  for seed in 20260901 20260902 20260903; do
    cp -p "$SRC_A2A4/$inst/seed-$seed.fourvec" \
      "$ROOT/input/snapshots/$inst/seed-$seed.fourvec"
    cp -p "$SRC_A2A4/$inst/seed-$seed.fourvec.receipt.properties" \
      "$ROOT/input/snapshots/$inst/seed-$seed.fourvec.receipt.properties"
  done
done

SRC_A0A2=/home/inspur/aicomp/zhangbo-v35-a2-final-candidate-confirmation-20260825/run-r4/input/snapshots
for inst in 100_2_5_1 100_8_3_1; do
  for seed in 20260911 20260912 20260913; do
    cp -p "$SRC_A0A2/$inst/seed-$seed.fourvec" \
      "$ROOT/input/snapshots/$inst/seed-$seed.fourvec"
    cp -p "$SRC_A0A2/$inst/seed-$seed.fourvec.receipt.properties" \
      "$ROOT/input/snapshots/$inst/seed-$seed.fourvec.receipt.properties"
  done
done

sha256sum "$ROOT/bin/fc5-transfer-diagnostic.jar"
find "$ROOT/input/snapshots" -name '*.fourvec' -type f | sort | while read -r file; do
  receipt="$file.receipt.properties"
  expected=$(sed -n 's/^snapshotSha256=//p' "$receipt")
  actual=$(sha256sum "$file" | awk '{print $1}')
  test "$actual" = "$expected"
  printf '%s %s\n' "$actual" "${file#$ROOT/}"
done
