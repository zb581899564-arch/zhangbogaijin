# V35-FC5-MIDHORIZON-DIAGNOSTICS-V3：最终 PDDR provenance 验收

状态：已完成实现与运行核验；本报告只覆盖诊断工具，不改变正式算法语义、参数或冻结 Jar。

## 固定对象

- Contract version：V35_MIDHORIZON_D_CONTRACT_V1
- seed：20260901
- diagnostic Jar SHA-256：723D24ED3021A01FACDA0231E3B142238E740FB18D025A4341748F2AF8D22E2F
- 诊断输入是实际运行中的 PermutationSolution，不是 PDDR pool ordinal、摘要或候选池索引。

## 已锁定的 provenance 规则

1. 候选在唯一的 pre-evaluation 边界被记录；candidateId、FE、generation、source、parentSlot、parentFingerprint、lineageId 与四个真实向量均来自该调用现场。
2. candidate digest 使用实际的 JS、FA、MA、WA 四向量，并使用 streaming SHA-256；候选数量必须闭合到实际 decoder evaluation call 数量。
3. PDDR 的 physicalSlotBefore 从候选实际携带的 ZhangBoSubSwarm 语义映射到 1–4，再由固定语义映射到 G1_CMAX、G4_BALANCED、G2_TEC、G3_TWC。不使用 poolOrdinal、sourceSlot 或 index % 4。
4. 本轮配置的物理容量顺序为 G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC；未显式覆写时使用 baseline 20,40,20,20。
5. A2 的 AUTHOR_HISTORY 配置不启用 lineage archive，因此 lineage 字段按“不适用”处理；A4 的 QP_FOUR_ACTIONS 路径使用真实 ZhangBoLineageTag。这不是用常量伪造通过条件。

## ON 运行结果

| run | actualFE / decoderCalls | RNG draws | candidates | PDDR rows | contract |
|---|---:|---:|---:|---:|---|
| A2-2k-effective-5100 | 5100 / 5100 | 719265 | 5100 | 200 | PASS |
| A4-2k-effective-5100 | 5100 / 5100 | 947872 | 5100 | 200 | PASS |
| A2-20k-100_2_4_1 | 20000 / 20000 | 1007192 | 20000 | 258 | PASS |
| A2-20k-100_5_3_1 | 20000 / 20000 | 719760 | 20000 | 255 | PASS |
| A4-20k-effective-20258-100_2_4_1 | 20258 / 20258 | 1260657 | 20258 | 731 | PASS |
| A4-20k-effective-20258-100_5_3_1 | 20258 / 20258 | 1498299 | 20258 | 730 | PASS |
| A2-50k | 50000 / 50000 | 1144370 | 50000 | 547 | PASS |
| A4-50k | 48269 / 48269 | 2149377 | 48269 | 1553 | PASS（独立的最终检查点门失败，见 17 号报告） |

八个 ON 运行的 rngConsumptionSequenceSource 均为 ACTUAL_JMETAL_RANDOM，rngHashSource 均为 ACTUAL_RANDOM_DRAWS；候选来源均为 ACTUAL_GENERATED_CANDIDATES，候选 hash 来源均为 ACTUAL_PRE_EVALUATION_CANDIDATES。每个运行的 candidate count 均等于 decoderCalls，sequenceIdentityPass=true。

## 反向抽样

在 A4-20k-effective-20258-100_5_3_1-ON-final 的 PDDR ledger 中，实际行包含：

- source=GLOBAL_OFFSPRING
- parentId=EVAL-4901、EVAL-4902 等真实父候选 ID
- parentSlot=0,1,2,...
- lineageId=81 等真实 lineage 值，parentLineageId=-1 表示该 global parent 没有可追溯的上游 lineage
- physicalSlotBefore=1 对应 semanticRoleBefore=G1_CMAX

同一运行的物理槽/语义组合计数为 1/G1_CMAX=164、2/G4_BALANCED=263、3/G2_TEC=164、4/G3_TWC=139，四类均来自 ledger 的实际候选字段。针对 PDDR adapter、full ledger、true sequence audit 与诊断 driver 的独立静态扫描未发现 index % 4、sourceSlotFor、CANDIDATE_DERIVED、SUMMARY_DERIVED 或 fallbackRng。

范围说明：v35 包中独立的 V35P25DComparativeEngine 仍保留其自身 local-candidate 的 index % 4；它没有被本次 V35MidHorizonDiagnosticDriver 调用，且不属于本轮 PDDR provenance 路径。为遵守“不改变正式算法语义”的边界，该比较引擎未被修改。

结论：PDDR provenance、父候选、lineage 与物理槽语义契约已完成实际链路验证；A2 的 lineage 不适用状态被显式保留，未被错误地当作真实 lineage 证据。
