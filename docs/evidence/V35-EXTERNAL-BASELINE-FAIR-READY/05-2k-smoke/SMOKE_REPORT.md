# 2k 工程贯通报告（ENGINEERING_SMOKE）

- 日期：2026-08-30
- 性质：**仅工程贯通**。不得进入论文 reference、性能排名或优越性结论。
- 消耗FE：4 runs × 2000 = 8000 decoder 调用（工程域）
- Runner：`ZhangBoV35ExternalFairBaselineRunner`（比较构建物
  `03-implementation/external-fair-baseline-comparison-585ca315.jar`）
- 固定配置（运行前锁定，未按性能挑选）：

```text
instance   = 20_2_3_1（CONTAMINATED_DEVELOPMENT，正式 roster 实例，
             instanceSha256=2e88fa97…5CF；instance-extension/fatigue 文件与
             45 实例正式 manifest 同源）
seed       = 20260822（P25D 50k 试点既用过的工程 seed；非验证保留 seed）
population = 100
MaxFEs     = 2000
snapshot   = docs/evidence/V35-FORMAL-MANIFEST/initial-populations/20_2_3_1/
             seed-20260822.fourvec
             （SHA b5f53f6e…，与 FORMAL_INITIAL_POPULATION_MANIFEST.csv 同值）
```

## 结果（4/4 COMPLETED）

| run | actualFE | front | canonicalFrontHash | crossover/mutation/tournament |
|---|---:|---:|---|---|
| NSGA-II-F r1 | 2000 | 65 | `22b2a082…7b19` | 950/1900/1900 |
| NSGA-II-F r2 | 2000 | 65 | `22b2a082…7b19`（=r1） | 950/1900/1900 |
| SPEA2-F r1 | 2000 | 100 | `68d1faf5…ad06` | 1900/1900/3800 |
| SPEA2-F r2 | 2000 | 100 | `68d1faf5…ad06`（=r1） | 1900/1900/3800 |

## 验收门逐项（工作包 §十三）

| 门 | 结果 |
|---|---|
| front 非空 | PASS（65/65/100/100） |
| 目标有限 | PASS（身份测试断言 + 结果值检查） |
| initialPopulationHash 一致 | PASS（4 条 run 同值 `165ad2bc…77f2`，且与历史 P25D 50k 试点的该实例同 seed 记录跨 campaign 吻合） |
| successfulDecoderCalls = fullEvaluations = actualFE | PASS（2000=2000=2000；canonical EvaluationCounter 同值） |
| actualFE ≤ 2000 | PASS（精确 2000） |
| illegalSolutions = 0 | PASS（4/4） |
| duplicateEvaluations = 0 | PASS（4/4，预算守卫全程无触发） |
| unexplainedRepairs = 0 | PASS（representationRepairs=0，算子自身维护合法性） |
| forbiddenMechanismEvents = 0 | PASS（4/4） |
| algorithmIdentityEvents 满足 | PASS（crossover/mutation/tournament 全部 >0；NSGA-II tournament=1900=2×代数、SPEA2=3800=4×代数，与各自选择结构相符） |
| 重放确定性 | PASS（同 seed 独立 JVM 两次：front hash 与 FE 完全一致，双算法皆然） |

## 快照身份

`seed-20260822.fourvec` 物理哈希 `b5f53f6e…5ae24` 与
`V35-FORMAL-MANIFEST/FORMAL_INITIAL_POPULATION_MANIFEST.csv` 登记值一致；
`initial-population.sha256` 内记录 V35/P8/snapshot-file 三重哈希。
同一快照喂给双臂（各经深拷贝注入），逐字节一致（见公平性测试）。

## 边界重申

本目录全部结果只证明：官方 jMetal 5.8 核在共享 V35 问题层上能被公平驱动、
预算精确闭合、行为可重放、无任何 V35 机制泄漏。**不构成任何算法优劣证据。**
