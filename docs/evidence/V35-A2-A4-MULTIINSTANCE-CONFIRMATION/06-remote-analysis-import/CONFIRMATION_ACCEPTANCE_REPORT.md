# V35 A2/A4 多实例确认：独立验收报告

本报告只读取预注册的60条训练机输出；它未调用算法，也未混入任何开发、DOE或Stage2先导reference。

## 运行完整性

- 已验收运行：60 / 60；有效配对：30 / 30。
- 每条运行均通过文件级SHA-256、provenance、有限前沿、phase-consistent预算和共享快照复核。

## 每实例参考前沿

每个实例以A2/A4共10条raw front严格Pareto并集单独构造PFref；HV reference为归一化空间(1.1,1.1,1.1)。

|Instance|Scale|PFref点数|median ΔHV|median ΔIGD|median ΔCmax|median ΔTEC|median ΔTWC|
|---|---:|---:|---:|---:|---:|---:|---:|
|100_2_4_1|100|785|+13.29%|+35.55%|+3.21%|-3.56%|+0.81%|
|100_5_3_1|100|757|-12.96%|-76.31%|+2.14%|-2.53%|-0.70%|
|20_2_4_1|20|873|+6.98%|+36.21%|+1.34%|+0.47%|+1.48%|
|20_5_3_1|20|470|+6.19%|+12.09%|+0.86%|-0.27%|+1.41%|
|50_2_4_1|50|2775|-4.09%|-10.39%|+7.87%|-2.96%|+1.52%|
|50_5_3_1|50|857|+2.62%|+18.55%|+0.86%|+0.68%|+2.99%|

## 预注册裁决

|Gate|Result|
|---|---|
|all30PairsValid|PASS|
|overallMedianDeltaHVPositive|PASS|
|overallMedianDeltaIGDPositive|PASS|
|overallMedianDeltaCmaxAtLeastMinus2Percent|PASS|
|positiveHvIgdInstancesAtLeast4|PASS|
|everyScaleMedianHvIgdNonnegative|FAIL|
|hundredJobPooledMedianHvIgdNonnegative|FAIL|
|noSingleHundredJobVeto|FAIL|
|noSimultaneousTecAndTwcSystematicRegression|PASS|

**裁决：`A4_NOT_PROMOTED`。**

此裁决只决定A4是否进入Final freeze候选；不构成正式论文优越性或显著性结论。
