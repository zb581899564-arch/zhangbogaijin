# Observer Schema 冻结候选稿（V35 SOURCE-ATTRIBUTION-500K Phase A0）

状态：`DESIGNED_NOT_IMPLEMENTED`（本文件只设计，不实现、不编译、不消耗FE）
版本：`1.0-draft`（冻结日期 2026-08-31；20k OFF/ON 等价门+内存门通过后置 `observerSchemaFrozen=true`）
上游合同：`source-taxonomy.csv`（四类一级来源，运行后不得增加第五类）、`source-call-chain.csv`（每个字段的真实观测点）
实现纪律继承：`V35EvaluationSourceContext.java`（ThreadLocal try/finally、不改RNG/FE/控制流）与 `V35SourceLedgerHook.java`（纯观察、错误计数不抛出、SHA-256指纹）。

## 0. 总原则

1. Observer 是独立旁路：永不进入搜索档案、不改 PDDR 输入、不选教师、不消耗随机数、不评估任何候选、不修改候选对象。
2. 候选级账本只保存有界标量与指纹；完整解对象、四向量数组、解码轨迹一律禁止（见§4）。
3. 一级标签只允许 `GLOBAL_CFVF / CATA / INHERITED_LS / PARENT_CARRYOVER`（评估侧枚举值到一级类的映射见 taxonomy；`SHADOW` 显式排除）。二级字段不得改变一级 G1–G4 裁决。
4. 所有新观测点必须复用已验证锚点（V3 的 11 处 patch，见 call-chain 的 v3PatchAnchor 列）；新增观测点 = 新 schema 版本，必须重跑 20k OFF/ON。

## 1. 候选级允许字段全集（每条已评估候选一行）

| 字段 | 类型/界限 | 真实事件来源（调用点） | 观测时机 | 备注 |
|---|---|---|---|---|
| candidateFingerprint | 64位hex（SHA-256 of `ZhangBoQgController.fingerprint`四向量文本，L444-448） | 全部评估入口（批窗口 fc52RecordEvaluated L1174-1177；单候选 L4780-4781；CA-TA-Lite L5121；legacy L4914；critical L5376/L5418；O1-O9 L5483；fixed VNS L5297） | 评估后observe时 | V3已验证先例（V35SourceLedgerHook.fingerprint） |
| source | 枚举：GLOBAL_CFVF/CATA_TEST/CATA_APPLY/INTER_FACTORY_LS/INTRA_FACTORY_VNS/INITIAL_POPULATION/FINAL_EVALUATE（评估侧）；PARENT（选择器侧，N_eval=0行） | L606/L682/L788/L4759/L5103/L4895/L5358/L5400/L5278/L5462 的 begin/end 窗口 | 评估后observe时 | 一级映射见 taxonomy |
| generation | long（generationNumber）+ int（formalBaselineOuterCycles）+ int（formalQRoundIndex） | 主循环 L672/L722/L814；fc52RecordEvaluated 传入 L1088-1089 | 评估后observe时 | 固定三个整数，无界问题 |
| actualFE | long（fullEvaluationCount 落账值） | 同上各评估入口；PreEvaluatedTag.evaluationOrdinal（L4788 等）交叉核验 | 评估后observe时 | PARENT行 N_eval=0，actualFE 记当轮 pool FE |
| Cmax, TEC, TWC | double×3（getObjective(0/1/6)） | 同上各评估入口 | 评估后observe时 | 目标三元组 |
| parentFingerprint | 64位hex 或 -1 | ZhangBoLineageTag 属性（V35SourceLedgerHook L67-69 先例）；账本内按 slot/lineage 匹配 | 评估后observe时（匹配于账本写入时） | INITIAL_POPULATION=-1 |
| teacherFingerprint | 64位hex 或空 | selectQgLeader L2842-2889（Selection.getLeader()）；Qp 教师 pendingQpSelections（settleQp L3497-3531 消费前快照） | teacher选择时（轮前快照）→账本回填 | 需新增只读快照钩子；观测点已核实 |
| enteredMergePool | bool | applyEvaluatedPddr L9295-9349（pool 组装顺序=selector输入序） | 进池时 | V3 P11 锚点 |
| selectedByPddr | bool | zhangBoEvaluatedPddrSelector.select 返回 L9350-9353；按 fingerprint multiset 回匹配（V3 onPddrRound 先例） | PDDR选择时 | selectedRank 一并记录 |
| enteredWorkingPopulation | bool | selected 重建 evaluatedOffspring/tempSwarm L9384-9416（GLOBAL_ORIGINAL 下 = selectedByPddr，仍单列以便未来模式变化） | PDDR选择时 | 与 selected 同点派生 |
| enteredPersonalArchive | bool | ZhangBoQpController.settle 内 archive.update 的 isInsertedEntrySurvived()（ZhangBoQpController.java L233-244）；先例 API：V35Fc52LifecycleAudit.observeArchiveAdd | settle时（评估后） | 需在 settle 链挂只读钩子；观测点已核实 |
| usedAsQgTeacher | bool | selectQgLeader 选中即标记（先例：v35CmaxLifecycleAudit.markTeacher L2868） | teacher选择时 | 按 fingerprint 注册表回填 |
| usedAsQpTeacher | bool | Qp 个人领导/方向选择（pendingPersonalLeaders L3483-3487；reconcilePopulation L9400-9403） | teacher选择时 | 同上模式 |
| generatedDescendant | bool（流式派生） | 账本自比较：存在 parentFingerprint==candidateFingerprint 的后行 | 窗口收盘时流式计算 | DERIVABLE_FROM_LEDGER |
| generatedImprovingDescendant | bool（流式派生） | 后行 accepted=true（L5131/L4921/L5368/L5410/L5487）或三目标占优 | 窗口收盘时流式计算 | 同上 |

生命周期链：`GENERATED→MERGE_POOL→PDDR_SELECTED→WORKING_POPULATION→PERSONAL_ARCHIVE→QP/QG_TEACHER→DESCENDANT→IMPROVING_DESCENDANT`（计划§6），全部由上表字段表达；`PARENT_CARRYOVER` 行 N_eval=0，只可能有 MERGE_POOL/PDDR_SELECTED/WORKING/TEACHER/DESCENDANT 状态。

## 2. GLOBAL_CFVF 二级字段（不改一级裁决）

| 字段 | 真实事件来源 | 备注 |
|---|---|---|
| finalEvaluate | L788 窗口本身（taxonomy 决定：FINAL_EVALUATE 并入 GLOBAL_CFVF 时置 true） | A4 冻结语义下经验恒为 false/0 行；>0 即触发语义复核 |
| subSwarmRole | ZhangBoSubSwarm 解属性（L2900-2905 消费处可读；评估入口处可读） | G1_CMAX/G2_TEC/G3_TWC/G4_BALANCED |
| QgAction | pendingQgSelections（Selection.getAction()）；settleOriginalQg L2891-2939 消费前快照 | 轮级快照→按 group 回填 |
| QgTeacherHash | Selection.getLeader() 的 SHA-256(fingerprint)，截断16hex落账 | 同上 |
| QpTeacherHash | pendingQpSelections 的 selectedPbest fingerprint，截断16hex | 同上 |
| QpAction | Selection.getAction()（Qp 动作枚举） | 同上 |
| jsChanged/faChanged/maChanged/waChanged | 子代 vs parentFingerprint 对应行四向量逐段比较（fingerprint L444-448 四段） | 有界：4布尔 |
| jsDiffCount/faDiffCount/maDiffCount/waDiffCount | 逐段 Hamming/逐元素差计数 | 有界：4个long；禁止保存差集本身 |

## 3. 有界结构与上限（溢出策略一并冻结）

| 结构 | 建议上限 | 溢出策略 |
|---|---|---|
| 候选级账本（流式CSV） | 内存仅缓冲 1 个 25k 窗口；每窗口收盘即 flush+重置 | 永不整体驻留；flush 失败=observer error 计数（不中断搜索，沿用 V35SourceLedgerHook.fail 语义） |
| ND sample（每25k窗口） | 每窗口每一级来源 ≤512 行 | 超限用 observer 专用确定性 reservoir（独立于算法RNG，种子=runSeed^0xOBS1）均匀蓄水；丢弃仅计数 |
| forensic reservoir（每25k窗口） | 每窗口 ≤256 行（指纹+三目标+source+fe+parentFp+生命周期布尔） | 同上；forensic 行与 ND sample 不互斥 |
| 流式计数器 | 固定宽度 long（每来源×窗口：N_evaluated/N_unique_objective/N_exclusive_ND/生命周期转化计数） | 无溢出（long）；不做有界化 |
| 流式 SHA-256（事件流哈希：Qg/Qp动作序列、teacher序列、PDDR survivor序列） | 每窗口1个64hex摘要 | 摘要链跨窗口链接（前一窗口摘要进下一窗口种子），防篡改可复核 |
| pddr-round 账本 | 与V3同构（pool行/轮）；500k 预计 ≤20k 行 | 按窗口 flush |
| observer 错误日志 | 首错误+计数（errorCount/lastError 模式） | 不保留堆栈对象 |

## 4. 禁止项（冻结）

1. 完整 Solution 对象或其引用驻留（含 defensive copy 长期保存）。
2. 全量 JS/FA/MA/WA 数组或其差集文本（只允许 changed 布尔+计数；ND/forensic 行也只存指纹+三目标）。
3. 完整解码轨迹、FM3 操作序列、关键 DAG（ZhangBoFatigueEvaluationResult 及其操作记录）。
4. 无界对象图：按候选数增长的一切非流式结构（V3 的整账 StringBuilder 在 500k 属违规规模，本 schema 改为逐窗流式）。
5. 读取或缓存算法侧 Random/PseudoRandomGenerator 状态；读取 System.nanoTime/nanoTime 后参与任何输出排序或采样决策（observer 自身时钟只允许测自身开销且不落账本行序）。
6. 五类来源标签、来源标签重解释、或把 SHADOW 计入任何一级类。

## 5. 版本纪律

任何字段增删、标签增删、上限修改、哈希口径修改 → `observerSchemaVersion` 递增 → 20k OFF/ON 等价门与内存门全部重跑 → 重新 `observerSchemaFrozen=true`。

---

## Main-Agent Resolution (Phase A0, 2026-08-31) — Agent B 跨文件缺口裁决

Agent B（窗口指标合同）标记：窗口切分依赖逐候选 `nominalFE` 与 `B_0`（初始种群后 decision front）快照，schema 草案未登记。主Agent裁决如下（冻结，Observer 实现阶段必须遵守）：

1. **`nominalFE` 列（派生列，必须输出）**：`nominalFE = 25000 × ceil(actualFE / 25000)`，即该候选归属的 25k nominal 窗口右端点。依据：PHASE_CONSISTENT_BUDGET_TERMINATION 下 actualFE 单调且在正常运行中与 nominal 同步推进（尾停差 <5000），ceiling 归窗无歧义；该列由 Observer 在写账本行时直接计算（零成本派生，不新增观测点）。WHVG/ExclusiveNDShare 的窗口切分一律使用该列。
2. **`B_0` 定义（无需新钩子）**：`B_0 = ND(账本中 source=INITIAL_POPULATION 的全部行)`——由账本离线精确重构（初始种群恰为账本前 100 行，V3 实测已验证该语义）。B_t（t≥1）= 25k checkpoint 的 phase-consistent decision-front snapshot（既有 checkpoint observer 能力，目标集 {25000,…,500000}）。
3. 该裁决不改四类一级来源分类、不改任何行为语义；Observer 实现阶段若未输出 `nominalFE` 列即 schema 违规。


---

## PHASEA0-CORRECTION-V1（2026-09-01）——多来源反事实语义修正（阻断问题A）

独立验收退回：初版合同的 FIRST_ADMISSION_WITHIN_WINDOW 归属把多来源重复目标点错误归属给单一来源（最小反例：GLOBAL_CFVF 与 CATA 先后生成完全相同的三元组 p，删除任一来源的事件后另一来源仍保留 p，反事实 WHVG 双方应为 0；初版却把全部贡献归给最先生成者，产生假的 G1/G3 信号）。

### 冻结修正（Observer schema 层）

1. **ledger 必须保留每条已评价候选事件的真实一级来源标签**；Observer 写入阶段**禁止**按目标三元组只保留第一来源（V3 式逐评估账本结构本身满足，本条为显式禁令）。
2. **三元组去重与 producerSet 构造发生在离线分析层**（threshold_recompute.py `canonical_groups` + `producer_set`），不在 Observer 内执行。
3. 反事实语义（与 source-attribution-thresholds.json attributionRule 完全一致）：
   - 归属规则 = `COUNTERFACTUAL_PRODUCER_SET`；
   - `Wt^-s = uniqueObjectiveTriples({e ∈ E_t | e.source != s})`——仅当某三元组的 `producerSet(p) == {s}`（全部事件来自 s）时才从 Wt 中剔除；多来源共享的三元组必须保留；
   - `multiSourceDuplicateRule = SHARED_POINTS_CONTRIBUTE_TO_NO_SINGLE_SOURCE`：producerSet 含 ≥2 来源的点不计入任何来源的 ExclusiveND，且单来源反事实中该点不消失；
   - `ExclusiveND`: `p ∈ ND(Fpast∪Wt) ∧ p ∉ Fpast ∧ producerSet(p) == {s}`；
   - `firstAdmissionScope = DESCRIPTIVE_ONLY`：firstProducerSource/ActualFE/CandidateId 仅用于描述性时序报告/候选出生顺序解释/非门控诊断，**严禁**进入 WHVG/WHVGShare/ExclusiveND/ExclusiveNDShare/G1/G3 门控。
4. 一级来源仍固定四类 `GLOBAL_CFVF / CATA / INHERITED_LS / PARENT_CARRYOVER`，不新增第五类。
5. 本修正不改变：nominalFE 派生列与 B_0 定义（上一节 Main-Agent Resolution）、四类映射、内存硬门、wall-clock 审计结论。
