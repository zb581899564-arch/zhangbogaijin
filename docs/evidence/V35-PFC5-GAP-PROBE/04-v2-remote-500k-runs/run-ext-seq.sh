#!/bin/bash
R=/home/inspur/aicomp/zhangbo-v35-gap-probe-v2-20260830
cd $R
for arm in SPEA2F NSGA2F; do
  for seed in 20260827 20260906; do
    if [ "$arm" = "SPEA2F" ]; then alg=SPEA2-F; else alg=NSGA-II-F; fi
    echo "START $arm $seed $(date +%H:%M:%S)"
    java -Xmx56g -cp $R/jars/external-fair-baseline-comparison-966da3d2.jar org.uma.jmetal.runner.lc_psode.ZhangBoV35ExternalFairBaselineRunner --algorithm $alg --instance $R/inputs/java-jmetal58/EADHFSP/100_5_3_1.txt --seed $seed --population 100 --maxFEs 500000 --snapshot $R/snapshots/100_5_3_1-seed-$seed.fourvec --run-id GAP500-$arm-100_5_3_1-$seed --attempt-id 3 --final-output $R/500k-runs/run-GAP500-$arm-100_5_3_1-$seed > $R/logs/gap500-$arm-100_5_3_1-$seed-attempt3.log 2>&1
    echo "END $arm $seed exit=$? $(date +%H:%M:%S)"
  done
done
echo ALL_DONE
