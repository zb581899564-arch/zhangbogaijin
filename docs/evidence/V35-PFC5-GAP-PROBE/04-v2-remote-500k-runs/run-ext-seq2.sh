#!/bin/bash
R=/home/inspur/aicomp/zhangbo-v35-gap-probe-v2-20260830
for pair in "NSGA2F:NSGA-II-F:20260827" "SPEA2F:SPEA2-F:20260906" "NSGA2F:NSGA-II-F:20260906"; do
  arm=$(echo $pair | cut -d: -f1)
  alg=$(echo $pair | cut -d: -f2)
  seed=$(echo $pair | cut -d: -f3)
  echo "START $arm $seed $(date +%H:%M:%S)"
  java -Xmx100g -cp $R/jars/external-fair-baseline-comparison-966da3d2.jar org.uma.jmetal.runner.lc_psode.ZhangBoV35ExternalFairBaselineRunner --algorithm $alg --instance $R/inputs/java-jmetal58/EADHFSP/100_5_3_1.txt --seed $seed --population 100 --maxFEs 500000 --snapshot $R/snapshots/100_5_3_1-seed-$seed.fourvec --run-id GAP500-$arm-100_5_3_1-$seed --attempt-id 4 --final-output $R/500k-runs/run-GAP500-$arm-100_5_3_1-$seed > $R/logs/gap500-$arm-100_5_3_1-$seed-attempt4.log 2>&1
  echo "END $arm $seed exit=$? $(date +%H:%M:%S)"
done
echo ALL_DONE
