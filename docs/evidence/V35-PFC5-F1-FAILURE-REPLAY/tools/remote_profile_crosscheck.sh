#!/usr/bin/env bash
# Zero-FE crosscheck of armProfileSha256 and runtimeConfigurationSha256 on the训练机.
set -euo pipefail
R=/home/inspur/aicomp/zhangbo-v35-pfc5-f1-20260829
mkdir -p "$R/tools"
cd "$R"
java -cp "$R/input/classes:$R/tools:$R/input/frozen-algorithm.jar" \
  org.uma.jmetal.runner.lc_psode.V35ProfileRegistryPrinter \
  --seeds 20260901 --max-fes 500000 \
  --output "$R/plans/profile-registry-recheck.csv"
echo "--- recheck output ---"
cat "$R/plans/profile-registry-recheck.csv"
