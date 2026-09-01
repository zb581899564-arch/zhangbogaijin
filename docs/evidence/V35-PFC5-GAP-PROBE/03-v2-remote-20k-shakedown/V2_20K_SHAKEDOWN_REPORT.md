# V2 训练机 20k 机制贯通报告（SHAKEDOWN_PASSED）

- 日期：2026-08-30
- 执行域：训练机 `/home/inspur/aicomp/zhangbo-v35-gap-probe-v2-20260830/20k-shakedown/`
- 配置：`50_2_3_1 × seed 20260827 × pop 100 × 20000 FE`，4 算法各 1 条独立 JVM
  （Java 11.0.27，-Xmx4g），同一快照（68c44eae…，V35 `a73e922f…` / P8 `219db415…` 四臂一致）
- 结论：**4/4 机制门全部 PASS**（`mechanism-gates.csv` 逐项），V2 20k 硬门通过，
  按用户授权直接进入 16×500k。

## 逐运行

| run | actualFE | decoderCalls | front | 机制门 |
|---|---:|---:|---:|---|
| GAP20K-A4 | 15258 | 15258 | 有 | PASS |
| GAP20K-A0 | 16673 | 16673 | 有 | PASS |
| GAP20K-SPEA2F | 20000 | 20000 | 100 | PASS |
| GAP20K-NSGA2F | 20000 | 20000 | 100 | PASS |

A4/A0 终止 = `PHASE_CONSISTENT_TAIL_STOP`（20k 预算下剩余 4742/3327 < 5000，
相位一致合法终止）；外部两臂精确闭合 20000。

## A4 机制门实测（全部真实触发）

```text
formalOuterCycles=2  formalQgRounds=100  qgSelections=400   pddrEvents=2
cfvfOffspring=10000  qpActions=8100      qpTransitions=4100
dualQWarmup=19       dualQP=41           dualQG=40
caTaLiteTest=447     caTaLiteApply=100
零项：cfvfRepairs=0  directionalPoolRequests=0  shadowEvaluations=0
      illegalSolutions=0  duplicateEvaluations=0  shift活动=0
```

## A0 机制门实测

```text
formalOuterCycles=1  formalQgRounds=50   qgSelections=200   pddrEvents=1
baselineUpdateEvents=5000（固定基线更新路径真实触发）
零项：qpActions=0  dualQ 全 0  cfvfOffspring=0  caTaLiteTest/Apply=0
      directionalPoolRequests=0  illegal/duplicate=0  shift活动=0
```

## 外部两臂

- SPEA2-F：tournament=19900 / crossover=19900 / mutation=19900；
  identityEvidence 含 strengthRawFitness=true、archive=true、
  environmentalSelection=SPEA2；forbiddenMechanismEvents=0。
- NSGA-II-F：tournament=19900 / crossover=9950 / mutation=19900；
  identityEvidence 含 binaryTournament、ranking、crowdingDistance、
  replacement=RankingAndCrowdingSelection；forbiddenMechanismEvents=0。

与 v1 的 2k 贯通（A0/A4 仅 100 FE、机制零触发）相比，本轮在真实机制层完成贯通；
20k 结果仍为 ENGINEERING_VALIDATION，不进入 reference 或论文性能表。
