# SOURCE_DIAGNOSTICS_PREREGISTRATION — V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1（冻结）

- 日期：2026-08-31
- 上游：campaign章程 `../V35-FINAL-COMPETITIVE-RECOVERY-CAMPAIGN/00-charter/CAMPAIGN_CHARTER.md`（P1）；
  杠杆审计 `../V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT/07-decision/FINAL_DECISION.properties`（NO_ACTIONABLE_LEVER；
  诊断能力缺口=250k候选级PDDR/来源归因遥测——本工作包即该缺口的100k档实现）。
- 目标：为每个被评估候选记录真实来源与全生命周期归宿，定位"生成侧候选质量/多样性不足"的具体来源。
- 性质：**诊断（ROOT_CANDIDATE），不是因果证明**。

## 1. 冻结科学语义（与正式线完全一致）

FM3；ShiftMode=NONE；single family；sequence-independent SUT；mixture=20/40/20/20；PDDR=GLOBAL_ORIGINAL；LS order=CA-TA-Lite→inherited LS；Dual-Q=P5/G5；rho=0；direction teacher pool=OFF；betaMax=0.65（仅C0）；population=100；PHASE_CONSISTENT_BUDGET_TERMINATION。诊断观察器纯观察：不入档案、不改PDDR输入/教师选择、不消耗随机数、不加FE、不改候选身份。

## 2. 诊断Jar V3（独立构建物；正式Jar字节不动）

影子面（全部从冻结tag `v35-final-doe1-frozen` 源码复制，classpath **V3:FORMAL** 优先加载）：

| 类 | 改动 |
|---|---|
| `ZhangBoMOHPSOQ`（影） | 9处observe站点传显式Source（2处批循环读窗口内current()；7处单候选传fc52同款字面量：GLOBAL_CFVF/CATA_TEST/CATA_APPLY/INTRA_FACTORY_VNS/INTER_FACTORY_LS×2/INTRA_FACTORY_VNS）；`observePassiveArchive` 签名加Source；PDDR选择钩子1行（`cmaxAudit.observePddrSelection` 后，isArmed()门控，纯观察池构成+选择结果） |
| `V35PassiveEvaluationArchive`（影） | `observeWithSource(solution, source)` = 原observeCore + 来源账本钩子 + 既有检查点钩子；`observe(solution)` 委托（兼容） |
| `V35FairRunner`（影） | 沿用V2的+2行（attach/recordTerminal） |
| `V35CheckpointObserverHook` | V2原样复用 |
| `V35SourceLedgerHook`（新） | 评估序账本（fingerprint/source/FE/三目标/parentLineageId）+ PDDR轮账本（池行：fingerprint/selectorSource/enteredMergePool；selected行：selectedByPddr/selectedRank/pddrScore）；指纹=SHA-256(ZhangBoQgController.fingerprint) |
| `V35SourceDiagnosticRunner`（新） | V2 runner改编 + `--telemetry OFF|ON`；导出 source-ledger.csv / pddr-round-ledger.csv / 检查点产物 |

`ZhangBoEvaluatedPddrSelector.Candidate.getPddrScore()` 仅selected行可用（池中未选中行无分数——登记 NOT_EXPORTED_AT_POOL_LEVEL）；selected行与池行经指纹多重集匹配（同指纹多候选时按序消耗）；teacherUsed 经 dscr-teacher-uses.csv 的teacherId文本离线SHA后与账本指纹连接（连接率如实报告，未命中=NOT_JOINABLE）；marginalHvContribution 离线重放（归档准入=弱支配+精确去重的目标值纯函数，HV参考(1.1,1.1,1.1)归一化）。

### 标签映射（冻结偏差登记）

| 任务书标签 | 账本实现 | 说明 |
|---|---|---|
| PARENT | INITIAL_POPULATION | 父代仅初群评估一次 |
| GLOBAL_Q 与 CFVF | GLOBAL_CFVF（Q轮元数据区分轮次） | A4下Q相offspring即CFVF offspring，同一评估事件 |
| QP_PERSONAL | **无评估行**（零FE） | Qp只结算/选领导不评估；仅作PDDR侧来源元数据 |
| CATA_TEST / CATA_APPLY | 同名 | 一致 |
| INHERITED_LS | INTER_FACTORY_LS 与 INTRA_FACTORY_VNS 两值（分析时合并报告） | 冻结枚举两个细分值 |
| （新增）FINAL_EVALUATE | 同名 | 每循环末补评批（量小单列） |

offspringImproved 在冻结正式路径无逐候选导出（仅FC5遥测线有）——登记 NOT_EXPORTED_FROZEN_PATH，以"该候选指纹是否出现在后续merge pool/前沿"等生存代理替代。

## 3. 等价门（本地，先于诊断运行；任一失败禁止上传）

1. 2k OFF/ON 与 20k OFF/ON（C0，两实例，seed 20260907）：13字段哈希一致（status/actualFE/decoderCalls/illegal/duplicate/initialPopulationHash/qgTableHash/qpTableHash/pddrEventStreamHash/canonicalFrontHash/frontSize/formalOuterCycles/评估trace）+ 全产物掩码逐字节比对（掩码=版本/runId/墙钟/观察器字段/jar哈希派生字段）+ observerErrors=0 + ON侧账本行数==actualFE。
2. V3-OFF 对已存50k运行（seed 20260907）忠实性（复用14号包掩码比对harness）。
3. 全部通过 ⇒ `sourceLedgerValidated=true`。

## 4. 渐进诊断运行（训练机）

- **第一档（必跑）**：C0 × {50_2_3_1, 100_5_3_1} × seed {20260919, 20260920, 20260921} × MaxFEs=100000 = **6条**，observer=ON。seed审计：20260916-18已被250k消耗；20260919/20从未使用；20260921仅被50k草案提及（提及不消耗）。
- **升级档（条件性）**：仅当目标差异在100k后才显现（预登记判据：两实例的来源贡献结构在50k与100k两个窗口间仍显著变化且未稳定）才追加对应6条250k；否则不升级。
- 执行协议：新campaign目录 `/home/inspur/aicomp/zhangbo-v35-source-diagnostics-20260831/`；逐文件SHA核验；每实例×seed组3 JVM、两组并行（≤6 JVM）；-Xmx4g、nice -n 10、无GPU；原子输出、失败保留；6条完成后统一分析。

## 5. 分析口径（脚本生成，禁手抄）

- 来源×结果矩阵（per instance × seed × source）：生成量（评估行占比）、进池率、selectedByPddr率、working-population进入率、observed-front贡献、decision-front贡献、marginalHvContribution分布（中位/前10%截断和）、终窗趋势；
- 困难vs正常实例对照（本设计两实例均有候选级数据——补上杠杆审计登记的最大缺口）；
- 时序：FE分窗（≤25k、25-50k、50-75k、75-100k）来源贡献演化。

## 6. 诊断裁决（六选一；预登记量化门，事后不得改）

设来源S在困难实例上的"价值效率" e(S) = (S的marginalHvContribution总和占全前沿HV比例) / (S的生成量占全部评估比例)：

1. `CFVF_GENERATION_COLLAPSE`：CFVF（GLOBAL_CFVF）生成占比最大（>60%评估行）且 e(CFVF) 终窗 < 0.3 且较早期窗口下降 ≥50%，≥2/3 seed、两实例一致；
2. `QP_PERSONAL_ARCHIVE_COLLAPSE`：Qp结算侧元数据显示personal leader更新停滞（dscr Qp表哈希早熟稳定 + personal teacher占比终窗 <10%），≥2/3 seed、两实例一致；
3. `CATA_LOW_VALUE_EXPLOITATION`：CATA_TEST+APPLY 合计 e(S) 终窗 < 0.15 且其FE占比 ≥5%，≥2/3 seed、两实例一致；
4. `COMPOSITE_GENERATION_BIAS`：≥2个来源同时满足各自价值效率显著低于其生成占比（e<0.5）且无单一主导来源；
5. `NO_SOURCE_LEVEL_FAILURE`：所有主要来源 e(S) ≥ 0.5 或贡献结构与生成占比相称（崩塌须由外部因素解释——登记并停）；
6. `INSUFFICIENT_EVIDENCE`：账本行数不足/连接率<80%/等价门失败导致无法评估。

所有裁决为 **ROOT_CANDIDATE（观察性）**，因果确认归第二阶段修复实验。

## 7. 停止点

诊断裁决落盘+治理更新后**立即停止**，等用户复核再选第二阶段修复族方向。本包不改PDDR/CFVF/Dual-Q/CA-TA语义，不启动修复实验。
