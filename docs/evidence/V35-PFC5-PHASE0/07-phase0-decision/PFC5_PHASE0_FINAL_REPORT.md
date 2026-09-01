PHASE0_PASSED_F1_PREREGISTERED

# PFC5 Phase 0 最终门禁报告

- 日期：2026-08-29
- 总控：单总控 Agent（ZCode），按用户批准执行包运行；AGENTS.md/ROADMAP.md/最终报告/
  evidence-sha256.tsv 仅由总控修改
- 消耗FE：0（全程零 FE；唯一网络动作为用户授权的只读证据拉取，已逐文件核验）
- 算法/PDDR/正式矩阵：零改动（`algorithmChanged=false`、`pddrChanged=false`、
  `formalMatrixRunning=false`）
- 机器可读状态：`PFC5_PHASE0_ACCEPTANCE.properties`；核验明细：`TEST_RESULTS.md`（12/12 PASS）

## 1. 历史失败 seed 选择结果

- 实例 `100_5_3_1`，A2 vs A4，500k，5 个历史配对 seed 全部登记
  （`01-historical-failure-case/historical-failure-seed-registry.csv`）。
- 冻结判据（否决门口径）：`deltaHV < −5% AND deltaIGD < −20%`；
  失败类 = {20260901, 20260904, 20260905}；20260902 未失败；20260903 双门均未越过
  （预登记初版曾误标 20260903 为 PARTIAL_HV_ONLY，脚本精确重算后已修正并留痕）。
- 选中 seed = **20260901**（最小 ID 规则）。如实登记：它同时是类内退化最重者与
  Step 0 工具验收 seed；选择按预冻结规则产生，非择差、非自动沿用工具身份。
- 案例与 seed 标记 `CASE_SELECTED_DIAGNOSTIC_ONLY`。
- `checkpointFrontAvailable=false`（历史 500k 无逐代前沿）→ 若 F1/F2 通过，F3 条件预判成立。

## 2. Snapshot 身份分类

```ini
historicalSnapshotIdentity=EXACT_HISTORICAL_SNAPSHOT_AVAILABLE
```

物理文件自训练机确认实验目录拉回，实测 SHA-256 `84d84523…3769` 与双臂 500k 运行前
独立写入的 provenance 锚点一致；V35 逻辑初群哈希 `179a82a3…4c2d` 与两臂 status、
50k/100k 转移运行全部一致；实例/SUT/疲劳/问题配置哈希与 45 实例正式 manifest 同值。
五环证据链详见 `SNAPSHOT_IDENTITY_DECISION.md`。

## 3. 实例角色登记情况

49 个实例（45 正式 roster + 4 试点）全部归类，互斥且经测试核验（TEST 第 3 项）：
诊断专属 1（100_5_3_1）、CONTAMINATED_DEVELOPMENT 17、VALIDATION_RESERVED 27、
LEGACY_EXCLUDED 4。物化清单（FORMAL-MANIFEST、Master 4500 计划登记）不计为暴露。
未知角色队列本轮为空；角色重分类须用户批准。

## 4. 外部基线 fair-ready 情况

- `fairReady=true`：HMOPSO-QGS-F（公平适配 runner + P8.3 性能门 + P25D 50k×5 seed，
  试点 mechanismSummary 证实 V35 机制全关）。
- `pending_gate`：NSGA-II-F、SPEA2-F（官方 jMetal 核 + 50k 试点在案，但须经 P25D
  引擎脱钩与上游登记后方可进论文 reference）。
- `NOT_READY`：HMOPSO-QLS-F（嵌于被禁用 P25D 引擎，PENDING_SOURCE_VERIFICATION）、
  MOEA/D（遗留问题类，未接公平链路）、QMOEA（无实现，既有登记缺口）。

## 5. Reference contract 是否冻结

已冻结（`FAILURE_REPLAY_REFERENCE_CONTRACT_V1`）：PFref=10 份历史 raw front 严格 ND
并集（757 点），与历史存档参考前沿集合完全一致、顺序无关重建逐位一致；ideal/nadir/
归一化/HV 参考点/实现（历史工具精确副本）全部绑定；失败门与 checkpoint 对齐规则一并
冻结。Gold 重算：HV 逐位一致、IGD 最大绝对差 1.665e-16（门 1e-12）→ PASS。
合同 SHA-256 `ecdc5589…235f`，PFref canonical `4dc85dd4…683da`。

## 6. Step 0 是否复用；是否产生新运行

```ini
STEP0=SATISFIED_AFTER_OFFLINE_RECONSTRUCTION
newStep0Runs=0
```

既有 121 runtime OFF/ON 证据经独立复核：29 个核心行为字段两侧全 EQUAL、终端快照
门 PASSED、三 Jar 封存副本逐一重算匹配。离线重建 working population（72 解，
canonical `8a9fd419…e43c`）与 front 哈希；工作树 Jar 漂移（a0a1e74d…/e5969803…）
登记为发现，权威实体绑定封存副本。**未重跑任何工具实验。**

## 7. 诊断 Jar 是否封板

已封板（`DIAGNOSTIC_TOOLING_FREEZE.md` + `diagnostic-freeze.properties`）：
`diagnosticToolingValidated=true`、`diagnosticToolingFrozen=true`、
`behavioralEquivalence=true`、`observerErrors=0`、`cataFullLifecycleValidated=false`
（原始输出，如实保留）。封板效力与升级通道按 §十一 执行。

## 8. F1 是否完成预登记；是否启动

```text
F1 预登记完整（06-f1-preregistration/ 五件：预登记正文、run contract、input manifest、
reference binding、stop gate），绑定冻结 Jar/实例/SUT/疲劳/profile/快照/初群哈希/
reference 合同/预算协议/环境留痕要求。
replayKind=HISTORICAL_STATE_FAILURE_REPLAY（精确快照在案）。
失败门、四值分流、F2/F3 未来门（仅文字，未建脚本）全部预注册。
```

## 9. 验证与纪律声明

- 核验套件 12/12 PASS（`TEST_RESULTS.md`）：CSV schema、seed 选择确定性、角色互斥、
  snapshot 双哈希、前沿有限性/去重/严格 ND 幂等、reference 顺序无关、HV/IGD gold
  重算、Jar SHA 反核、OFF/ON 行为复核、evidence 台账逐文件反算、文档路径存在性、
  AGENTS/ROADMAP 冻结边界一致性。
- `javaSourceChanged=false`、`buildNotRequired=true`（未改 Java 代码，未重跑六模块构建）。
- 证据台账：`evidence-sha256.tsv`（137 条，逐文件 SHA-256，可逐条反算；不含台账自身
  与两个自引用验证产物）。

## 10. 最终状态

```ini
phase0GovernanceComplete=true
historicalFailureSeedFrozen=true
historicalSnapshotIdentity=EXACT_HISTORICAL_SNAPSHOT_AVAILABLE
instanceRoleRegistryFrozen=true
baselineReadinessAudited=true
failureReplayReferenceFrozen=true
step0SatisfiedByExistingEvidence=true
newStep0Runs=0
diagnosticToolingFrozen=true
f1Preregistered=true
f1Started=false
f2Started=false
f3Started=false
algorithmChanged=false
pddrChanged=false
formalMatrixRunning=false
FINAL_FROZEN=false
```

ROADMAP 同步更新：`PFC5-1=COMPLETED`、`PFC5-2=SATISFIED_BY_EXISTING_EVIDENCE`、
`PFC5-F1=PREREGISTERED_NOT_STARTED`。

---

F1 has NOT started.
A separate user authorization is required to run F1.
