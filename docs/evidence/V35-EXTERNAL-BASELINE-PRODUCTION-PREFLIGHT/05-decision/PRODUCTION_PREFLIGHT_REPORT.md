# EXTERNAL BASELINE PRODUCTION PREFLIGHT REPORT（20K）

- 日期：2026-08-30
- 性质：`PRODUCTION_PREFLIGHT_ONLY / ENGINEERING_VALIDATION`——零科学性能结论
- 裁决：`EXTERNAL_BASELINE_PRODUCTION_PREFLIGHT=PASSED`（6/6 运行 + 9/9 故障门）

## 1. 6 条运行全部通过（逐门）

| runId | 算法 | 实例 | actualFE | frontSize | 身份事件 | 全部门 |
|---|---|---|---:|---:|---|---|
| PRE20-NSGAIIF-20_2_3_1 | NSGA-II-F | 20_2_3_1 | 20000 | 100 | 全真 | PASS |
| PRE20-NSGAIIF-50_2_3_1 | NSGA-II-F | 50_2_3_1 | 20000 | 100 | 全真 | PASS |
| PRE20-NSGAIIF-100_2_4_1 | NSGA-II-F | 100_2_4_1 | 20000 | 100 | 全真 | PASS |
| PRE20-SPEA2F-20_2_3_1 | SPEA2-F | 20_2_3_1 | 20000 | 100 | 全真 | PASS |
| PRE20-SPEA2F-50_2_3_1 | SPEA2-F | 50_2_3_1 | 20000 | 100 | 全真 | PASS |
| PRE20-SPEA2F-100_2_4_1 | SPEA2-F | 100_2_4_1 | 20000 | 100 | 全真 | PASS |

门明细（`preflight-acceptance.csv`）：status=COMPLETED、
actualFE=decoderCalls=fullEvaluations=20000、remainingFE=0、illegalSolutions=0、
duplicateEvaluations=0、unexplainedRepairs=0、forbiddenMechanismEvents=0、
front 非空且有限、身份事件全真。预算审计（`budget-audit.csv`）：
actualFE=decoderCalls=evaluationCounter=20000 六条全闭合，partialBatch=false。

## 2. 身份事件（`algorithm-identity-events.csv`，数值取自各 run 的 event-summary.properties）

- NSGA-II-F ×3：**crossover=9950、mutation=19900、tournament=19900**（tournament
  = 2×代数调用量含环境选择复用）；身份证据含 ranking=true、crowdingDistance=true、
  replacement=RankingAndCrowdingSelection（官方核路径真实触发）。
- SPEA2-F ×3：**crossover=19900、mutation=19900、tournament=39800**；身份证据含
  strengthRawFitness=true、archive=true、environmentalSelection=SPEA2（strength/
  raw fitness/density/截断在官方 EnvironmentalSelection+StrengthRawFitness 内执行）。
- 计数均为 runner 侧纯委托包装统计，官方核零插桩。
- 勘误：本报告初版误写为 9500/19000/19000 与 19000/19000/38000——那是按 2k smoke
  计数线性外推的估计值，不是读取 20k 运行文件的实际值；CSV 一直正确（直接取自
  运行产物），已按实际值更正。

## 3. 三组初始种群配对一致（`initial-population-pair-audit.csv`）

| 实例 | 快照 SHA（同） | V35 初群哈希（同） | P8 哈希（同） | 实例 SHA（同） | pairConsistent |
|---|---|---|---|---|---|
| 20_2_3_1 | b5f53f6e…5ae24 | 一致 | 一致 | 47d32d48…8a08 | true |
| 50_2_3_1 | 02fb3036…4c43a | 一致 | 一致 | d08d6abc…e787 | true |
| 100_2_4_1 | cfc45052…294b | 一致 | 一致 | 10b57d8c…b8d3 | true |

三份快照哈希与 `V35-FORMAL-MANIFEST` 登记值一致；objective 映射双臂均为
[0,1,6]=[Cmax,TEC,TWC]（THREE_OBJECTIVE 视图强制门之下）。

## 4. 输出原子化与故障注入全部通过

Runner 生产模式：`.partial-<runId>-<attempt>` → 清单逐文件 SHA 自校 →
`ATOMIC_MOVE` 终目录；终目录已存在/残留 partial 即 fail-closed；异常退出不可能
留下可误认的最终结果。外部 launcher 独立后验（重哈希清单+必备文件+状态门）。

故障注入 9/9 fail-closed（`failure-injection-results.csv`，均为真实执行而非推断）：

| 场景 | 结果 |
|---|---|
| 错 Jar（换冻结正式 Jar 冒充比较 Jar） | FAIL_CLOSED_PRE（GATE_JAR_SHA_MISMATCH） |
| 错 snapshot（seed-20260823 冒充） | FAIL_CLOSED_PRE（GATE_SNAPSHOT_SHA_MISMATCH） |
| 错实例（20_2_3_1 文件冒充 50_2_3_1） | FAIL_CLOSED_PRE（GATE_INSTANCE_SHA_MISMATCH） |
| 重复 RunKey | FAIL_CLOSED_PRE（GATE_DUPLICATE_RUNKEY） |
| 已存在完成目录 | FAIL_CLOSED_PRE（GATE_FINAL_DIR_EXISTS） |
| 非法算法标签 | FAIL_CLOSED_PRE（GATE_ILLEGAL_ALGORITHM） |
| 进程中断（attempt 2：taskkill /F /T 树杀 @1.5s，100_2_4_1） | FAIL_CLOSED_INTERRUPTED；final_exists=false；partial `.partial-FI-interrupt-2/` 仅含 initial-population.sha256（无 manifest、无 status=COMPLETED），不可被误认为成功运行 |
| 缺 front | FAIL_CLOSED_VERIFY |
| evidence 清单被篡改 | FAIL_CLOSED_VERIFY（哈希不符被拒） |

**中断场景勘误（诚实记录）**：首次执行的 attempt 1 存在 harness 缺陷——`java`
经 Oracle javapath 启动器解析，`Popen.kill()` 只终止了启动器进程，真实 JVM 以
孤儿进程继续**跑完并成功落盘**（`.selftest/fi-interrupt/`，actualFE=20000、自带
清单），而 launcher 仍记录 INTERRUPTED——该 PASS 当时不受产物支持。复核发现后：
launcher kill 分支改为进程树终止（taskkill /F /T），首 attempt 产物原样归档为
`.selftest/fi-interrupt-first-attempt-kill-ineffective/`（缺陷证据），attempt 2
以真实树杀重执行并按上表成立。完整过程见
`04-acceptance/FAILURE_INJECTION_CORRECTION_LOG.md`；其余 8 项场景首次即真实验证。
注入产物隔离于 `03-runs/.selftest/`，不占 RunKey、不进注册表。

## 4b. 复核修正记录（2026-08-30，DOCUMENT_AND_LEDGER_CORRECTION_ONLY）

1. 测试总数勘误：14/14（12 项 ExternalBaseline 新增 + 2 项 P25E 回归），
   初版误写 16/16（把 P25E 分项计数与 surefire 总计重复相加）。
2. 身份事件计数勘误：见 §2（报告初版用 2k 外推值冒充 20k 实测值；
   CSV 始终正确）。
3. 顶层 evidence 清单勘误：初版 find 排除了所有名为 evidence-sha256.tsv 的文件，
   6 份 run 清单与 3 份 .selftest 清单（fi-interrupt×2 代、fi-missing、fi-tampered）
   共 9 份未入账；已改为仅排除顶层清单本身并全量反算（最终文件数见
   `PRODUCTION_PREFLIGHT_DECISION.properties: evidenceFiles`）。
4. 注册表收口：6 条 PRE20 行 outcome 由 LAUNCHED 更新为 ACCEPTED
   （= status COMPLETED 且逐门验收通过）；中断残留位置按真实路径
   （`03-runs/.selftest/`）表述。

## 5. 新旧比较 Jar SHA-256

```text
previous = 585ca3153136bee1c6bab89700563a64d9aab1f43815f8ae6bf822f01fcb93e6  (原样保留，未覆盖)
new      = 966da3d2d23842f4ea5892e8da57404c88b076be2f9fcb568b54953f525447d9
           (external-fair-baseline-comparison-preflight-966da3d2.jar；仅输出/隔离/幂等/证据差异)
frozen formal V35 jar 复算 = 8dad8f40…d8b9（不变）
```

## 6. 静态与测试基线（运行前复核）

- 静态扫描 91 项 0 违规（复用 fair-ready 工具）；
- V35ExternalBaselineRepresentation/Identity/Fairness + V35P25EFaithfulEngines =
  **14/14 PASS**（12 项 ExternalBaseline 新增 + 2 项 P25E 回归）；
- 三实例/三快照/extension/疲劳参数 SHA 与正式 manifest 逐一吻合（02-input-freeze）；
- Java 17.0.12 / class target 52 / 顺序单 JVM 执行。

## 7. 是否发现影响搜索语义的问题

**没有。** 全部发现均属 harness/文档域：快照门校验对象错误、中断定时、kill 信号
仅命中 Oracle javapath 启动器（树杀修复）、一处 Python/JVM API 混用、以及本报告
§4b 的三项文档/清单勘误——修复与更正均不触及搜索语义；冻结 Jar、官方隔离核心、
适配器、四向量算子、PDDR/CFVF/Dual-Q/CA-TA 复算/扫描全部不变。

## 8. 边界声明

```ini
externalBaselineFormalRunsStarted=false
gapProbeStarted=false
validationStarted=false
FinalCandidateApproved=false
formalMatrixRunning=false
algorithmChanged=false
PDDRChanged=false
CFVFChanged=false
DualQChanged=false
CaTaChanged=false
```

20k 结果只证明工程可生产性（原子输出、预算闭合、身份真实、可审计），
不得进入论文性能表，不构成任何算法优劣证据，不自动进入下一阶段。
