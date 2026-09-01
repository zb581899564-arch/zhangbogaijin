# GAP-PROBE 2k 工程贯通报告（ENGINEERING_SMOKE）

- 日期：2026-08-30
- 配置：`50_2_3_1 × seed 20260827 × population 100 × maxFEs 2000`（预登记锁定）
- 结果：**3/3 COMPLETED，SHAKEDOWN_PASSED**

## 逐门验收

| 门 | A4 | A0(HMOPSO-QGS-F) | SPEA2-F |
|---|---|---|---|
| 初群 V35 哈希 = a73e922f…f167 | ✓ | ✓ | ✓ |
| 初群 P8 哈希 = 219db415…1e51 | ✓ | ✓ | ✓ |
| status=COMPLETED / runner 门 failures | NONE | NONE | —（外部 runner 门） |
| actualFE=decoderCalls=fullEvaluations | 100 | 100 | 2000（精确） |
| 终止语义 | PHASE_CONSISTENT_TAIL_STOP（2k < 单个 Q phase 5000，零完整外循环适配；启动器门条款预写此情形） | 同左 | 精确预算闭合 remainingFE=0 |
| illegalSolutions / duplicateEvaluations | 0（runner 门强制） | 0 | 0 |
| front 非空有限 | 37 点 | 37 点 | 100 点 |
| 禁止机制 | runner 门全过（cfvfRepairs=0、directionalPool=0、shadow=0、shiftActivity=0） | 同左（qpActions=0、cfvfOffspring=0、caTaLite=0、dualQ=0） | forbiddenMechanismEvents=0；identityEvidence 无 V35 机制词 |
| 身份事件 | 接线级（子 Q 相位预算下机制正向门按设计延后至 500k FORMAL 门） | 同左 | crossover=1900/mutation=1900/tournament=3800 全 >0 |

## 语义说明（诚实记录）

A4/A0 在冻结相位一致预算协议下，2000 FE 不足以容纳一个完整 Q phase
（5000 FE），协议正确地在初始种群后执行 `PHASE_CONSISTENT_TAIL_STOP`
（actualFE=100=初始种群，`formalOuterCycles=0`）。这是冻结语义的正确行为，
不是缺陷：本贯通按启动器设计证明了**快照注入、解码器、输入哈希链与输出
工件**的接线正确性；机制级激活门（qgSelections>0、pddrEvents>0、双Q、
CA-TA 正向计数）按启动器条款延后至 500k FORMAL 运行时强制执行。
SPEA2-F 采用精确 FE 协议，2000 FE 全循环真实执行。

## 工件

每条 run 目录含 configuration.txt、source-provenance.properties、
initial-population.sha256、status.properties、front.csv、formal-gate.properties
（A4/A0）/ event-summary.properties（SPEA2-F）、budget-termination.properties、
manifest。2k 结果不进入 Gap reference、不进论文性能表。
