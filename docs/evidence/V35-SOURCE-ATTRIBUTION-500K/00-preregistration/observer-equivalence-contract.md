# Observer 20k OFF/ON 行为等价门合同（V35 SOURCE-ATTRIBUTION-500K Phase A0）

状态：`CONTRACT_FROZEN_CANDIDATE`（执行于未来 20k preflight；通过后才允许 500k）
实例/seed/arm：`100_5_3_1 / 20260901 / A4 / 20k`，同 JVM 参数（-Xmx4g）、同机、 observer OFF 与 ON 各一次。
唯一目的：证明观察器对搜索行为的零影响（neutrality），而非比较算法性能。

## 1. 比较字段清单（全部必须通过）

| # | 比较项 | 载体/字段 | 判定方式 |
|---|---|---|---|
| 1 | 初群 | `initial-population.sha256` / status.properties `initialPopulationHash` | hash 相等 |
| 2 | RNG/event 哈希 | status.properties `mechanismSummary` 内全部 `*EventStreamHash`（p6EventStreamHash、qgEventStreamHash、qpEventStreamHash、caTaEventStreamHash、pddrEventStreamHash）+ `decoderTiming.calls/decoderTotalNanos` 中 calls 部分 | hash 相等（nanos 字段掩码） |
| 3 | Qg/Qp 动作 | `qgSelections/qgTdUpdates/qpActions/qpTrainedTransitions` 计数 + dual-Q 事件序列 + `qgTableHash/qpTableHash` | 计数与 hash 均相等 |
| 4 | teacher identities | `dscr-teacher-uses.csv` 字节相等 + Qg/Qp teacher 序列摘要（observer 摘要与 OFF 侧重算摘要比对——OFF 侧由事件流重放重算） | 序列摘要相等 |
| 5 | CFVF fingerprints | `front.csv` + `checkpoint-fronts.csv` 内每行四向量指纹（由目标行交叉核验；指纹级比较以 ON 侧账本对 OFF 侧重算指纹） | 逐行相等 |
| 6 | 目标三元组 | `front.csv`（Cmax,TEC,TWC）逐行 | byte equality |
| 7 | PDDR survivors | status.properties `pddrEvents=…` 计数 + `pddrEventStreamHash` + 每 25k checkpoint 的 working population 行数与指纹摘要 | hash 相等 |
| 8 | CA-TA trace | `ca-ta-lite-events.log`、`bottleneck-pressure-events.csv`、`caTaLiteTest/caTaLiteApply/caTaLiteFE/formalLocalFE` 计数 | byte/hash 相等 |
| 9 | Q-table hashes | `qgTableHash`、`qpTableHash`（终局） | hash 相等 |
| 10 | actualFE | status.properties `fullEvaluations` / `actualFE` | 相等 |
| 11 | working population | 每 checkpoint 的种群指纹多集摘要（ON/OFF 同构导出） | multiset hash 相等 |
| 12 | decision-front | `front.csv` 终局 | byte equality（首选） |

判定优先级：**byte/hash equality 优先**。仅当掩码字段（§2）参与导致无法字节等价时，允许 canonical semantic equality，且必须逐字段书面说明规范化规则（排序、浮点格式、掩码项）——该说明本身是证据的一部分，事后不得改写。

## 2. 掩码字段（事先注册，不参与比较）

继承 V3 `equivalence_check_v3.py` 的 MASK_KEYS 并按 Phase A 调整：
`runnerVersion, runId, wallNanos, algorithmRunNanos, baseDecodeNanos, decoderTotalNanos, frameworkOverheadNanos, observerMode, observerSchemaVersion, checkpointTargets, checkpointRows, observerExecutionErrors, telemetryLedgerRows, telemetryPddrRounds, telemetryLedgerErrors, experimentalJarSha256, profileSha256, poolLevelAttribution`。
ON-only 产物（不要求 OFF 侧存在）：`source-ledger.csv`、`pddr-round-ledger.csv`、`checkpoints/`、`checkpoint-fronts.csv`、observer 采样 timeseries。
除掩码项外，一切产物必须 byte-identical（V3 先例口径）。

## 3. 失效判定

```ini
任一比较项系统性分叉（非掩码、非浮点打印噪声）
→ BEHAVIORAL_EQUIVALENCE_FAIL
→ SOURCE_ATTRIBUTION_NOT_AUTHORIZED
→ 停止：不进入 500k，修复仅限观察器代码，修复后重跑整个 20k 门
```

一次性/单点噪声（如文件时间戳、JVM 路径）必须先登记进掩码才可豁免；未登记的任何差异都算 FAIL。

## 4. 版本纪律

新增字段、来源标签、生命周期状态、buffer、hash 口径、上限值——任何一项变更即 `observerSchemaVersion` 递增，视为**新版本 observer**，必须完整重跑 20k OFF/ON 等价门 + 内存门，重新冻结。禁止"小改动免重跑"。

## 5. 执行产物

`observer-behavioral-equivalence.md`（比较矩阵+逐项 verdict+掩码清单+结论 properties：`BEHAVIORAL_EQUIVALENCE=PASS/FAIL`）+ `evidence-sha256.tsv` 登记。
