# EVIDENCE_ERRATA_20260901 — V35-SOURCE-ATTRIBUTION-500K 最后一次0-FE证据勘误（append-only）

- 勘误日期：2026-09-01（Observer工程门收口后、SA-HARD 500k启动前）
- 性质：**0-FE证据勘误**。不修改源码、不重建Jar、不重跑2k/20k、不上传训练机、不启动500k。
- 涉及范围：`03-20k-off-on-gate`、`04-memory-preflight` 两包内的文档与汇总 + 三份清单重生成 + 本勘误。

## 1. 勘误事由

### 1.1 旧汇总文件误保留上一版20k内存数据

2026-09-01的20k OFF/ON工程门在同一远端目录共完成两次串行执行：

| 执行 | 时间（Asia/Shanghai） | Observer构建 | 结局 |
|---|---|---|---|
| 第1次 | 15:16:08 → 15:16:45 | 流式修正**前**（flushedEventLedger驻留`StringBuilder`） | 被取代 |
| 第2次（最终） | 16:23:06 → 16:23:44 | 真流式V4 Jar `78bf4d30…`（flush到磁盘临时文件） | **当前唯一有效证据** |

`run-acceptance.csv`（15:24:49写）与`REMOTE_20K_GATE_REPORT.md`（15:28:36写）在第1次执行后写就；
真流式修正、16:23重跑与16:29–16:31同步之后，这两份汇总**未随之重新生成**，误保留了第1次（被取代）
执行的内存实测值；16:35的`evidence-sha256.tsv`又将陈旧汇总原样哈希入册，使旧值进入证据链。

被误保留旧值与当前真实值（第2次/最终执行，`sync/results/*/memory-summary.properties`、
`formal-gate.properties`）对照：

| 字段 | 旧值（OFF） | 新值（OFF） | 旧值（ON） | 新值（ON） |
|---|---:|---:|---:|---:|
| heapUsedPeak | 1104635312 | **945796576** | 1075328608 | **940463104** |
| heapCommittedPeak | 1389363200 | **1227882496** | 1328545792 | **1204813824** |
| gcCollectionCount | 44 | **46** | 48 | **59** |
| gcCollectionTime | 498 | **490** | 533 | **591** |
| wallNanos | 16458137836 | **16667320483** | 19262986389 | **20767852780** |

派生量同步更正：observerOverheadHeapUsedPeakBytes `-29306704` → **`-5333472`**；
observerOverheadWallNanos `2804848553` → **`4100532297`**。
行为/预算字段（actualFE=15258、decoderCalls、remainingFE=4742、terminationKind、outerCycles=2、
ledgerRows=0/15258、status=COMPLETED、failures=NONE）两次执行一致，无需更正。

### 1.2 原始新运行文件始终正确

`sync/results/obs20k-OFF/memory-summary.properties`（heapUsedPeak=945796576）与
`sync/results/obs20k-ON/memory-summary.properties`（heapUsedPeak=940463104）自16:29–16:31同步落盘
起即为最终执行的真实值，从未被改动。`BEHAVIORAL_EQUIVALENCE_20K.md`、`OBSERVER_FREEZE.md`及内存门
计算（baseline=945796576 → estimated500kPeak=1241503200，ratio=0.289060 < 0.60 → PASS）也始终基于
正确值。错误只存在于两份汇总文档的内容层及其清单哈希——清单层本身从未失真（见§3）。

### 1.3 MEMORY_PREFLIGHT_20K.md 磁盘估算勘误

旧文写"source-ledger.csv 15258行≈1.6 MB → 500k≈50 MB 磁盘"，沿用了预登记估算口径而非实测。
实测（`sync/results/obs20k-ON/source-ledger.csv`）：**4,135,598 B（含307 B表头）/ 15,258 FE**，
bytesPerEvaluatedCandidate=(4,135,598−307)/15,258=**271.02 B/行**（落在预登记估算带144–354 B/行内，
相对登记值300 B/行偏差约−9.7%，未超20%阈值，`observer-memory-model.md` §2系数无需冻结更新）。
按线性磁盘估算：**500k ≈ 135,522,283 B ≈ 135.5 MB**。该值属于**磁盘容量**口径，不进入heap估算
——estimated500kPeak=1,241,503,200 B的分解模型不含任何磁盘账本项，勘误前后完全一致。
同文件"实测"表一并补齐ON侧heapUsedPeak实测值（940,463,104 B）、heapCommittedPeak与GC计数
（OFF 46次/490ms、ON 59次/591ms）。

## 2. 本次修改范围（仅文档与汇总）

| 文件 | 操作 |
|---|---|
| `03-20k-off-on-gate/run-acceptance.csv` | 重新生成：内存/GC实测列改为最终执行真实值 |
| `03-20k-off-on-gate/REMOTE_20K_GATE_REPORT.md` | 重新生成：T3补记两次执行史、T4/机器可读摘要改为最终执行值、T5按当前56结果文件+4日志口径、Incidents登记本勘误事件 |
| `04-memory-preflight/MEMORY_PREFLIGHT_20K.md` | 修正：实测表补齐真实值；磁盘账本改为实测4,135,598 B→500k≈135.5 MB（磁盘容量口径，不入heap） |
| `03-20k-off-on-gate/evidence-sha256.tsv` | 重新生成（75包内条目+1条本勘误交叉绑定）并反向验证 |
| `04-memory-preflight/evidence-sha256.tsv` | 重新生成（1包内条目+1条本勘误交叉绑定）并反向验证 |
| `05-observer-freeze/evidence-sha256.tsv` | 重新生成（1包内条目+1条本勘误交叉绑定）并反向验证 |
| `EVIDENCE_ERRATA_20260901.md`（本文件） | 新增（append-only） |

## 3. 未修改内容（不可变项逐一复核）

- **源码**：未触碰任何Java/Python源文件；`01-observer-implementation`清单（61条，含src/、classes/、
  Jar与构建脚本）勘误前后反向验证均0/0。
- **Jar**：两个Jar未重建、未替换、字节不动。本次勘误本地重算SHA-256（复核位置：
  `03-20k-off-on-gate/staging/jars/`、`01-observer-implementation/`、`02-local-tests/src-gates/jars/`，
  所有副本逐位一致，且与最终执行`budget-termination.properties`登记值一致）：
  - observerJarSha256 = `78BF4D3016A612A9F3073CA00ABB94181EF4883B2838540AC9776B1EED046565`（=冻结值，未变）
  - formalJarSha256 = `8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`（=冻结值，未变）
- **运行数据**：`sync/`下全部60个运行产物（56结果+4日志）与`staging/`12文件零改动；勘误前基线反向
  验证确认全campaign七份清单（00:30、01:61、02:66、03:75、04:1、05:1、06:1条）全部missing=0/mismatch=0
  ——即清单层与原始运行产物从未失真，本次仅重哈希被修正的文档。
- **科学结论**：行为等价（12字节逐字节一致+2掩码等价+1测量only）、内存门PASS
  （1241503200 < 2576980377.6，ratio=0.289060 < 0.60）、FE闭合/终止/账本完整性结论全部不变；
  磁盘估算只影响磁盘容量规划口径，不影响heap门、行为等价或任何算法结论。
- **FE消耗**：本次勘误0 FE；未重跑2k/20k，未上传训练机，未启动500k。

## 4. 清单重生成与反向验证

三份清单按当前文件状态重新生成（被修正文档取新哈希；各附1条本勘误交叉绑定
`../EVIDENCE_ERRATA_20260901.md`，沿用00清单既有的跨目录绑定模式），并逐条反向验证
（文件存在性=missing，哈希复算=mismatch）：

```ini
03-20k-off-on-gate/evidence-sha256.tsv : entries=76 missing=0 mismatch=0
04-memory-preflight/evidence-sha256.tsv : entries=2 missing=0 mismatch=0
05-observer-freeze/evidence-sha256.tsv : entries=2 missing=0 mismatch=0
```

## 5. 复核基准（已验收事实复核，全部与产物一致）

```ini
observerJarSha256=78BF4D3016A612A9F3073CA00ABB94181EF4883B2838540AC9776B1EED046565
formalJarSha256=8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
actualFE_OFF=15258
actualFE_ON=15258
heapUsedPeak_OFF=945796576
heapUsedPeak_ON=940463104
estimated500kPeak=1241503200
estimatedPeakToHeapRatio=0.2890599892
memoryGatePassed=true
```

## 6. 勘误裁决（最终状态）

```ini
observerImplementationValidated=true
observerBehavioralEquivalent=true
memoryGateCalculationPassed=true
observerJarFrozen=true
evidencePackageFinalSignoff=true
sourceAttribution500kEligible=true
sourceAttribution500kStarted=false
SA_HARD_500K_STARTED=false
```

裁决含义：Observer工程门证据包经本次0-FE勘误后哈希闭合、汇总数值与原始运行产物逐项一致，
**具备SA-HARD 500k启动资格**；但500k仍未启动，须用户单独批准后按预注册冻结顺序执行
（SA-HARD → failure-class复现门 → SA-NORMAL → G1/G3分析 → 仅G1成立时SA-A2-CONDITIONAL → 强制停止）。

---

本文件为append-only：此后如再发现证据层错误，只允许新增勘误条目或新勘误文件，不得回改本文。
