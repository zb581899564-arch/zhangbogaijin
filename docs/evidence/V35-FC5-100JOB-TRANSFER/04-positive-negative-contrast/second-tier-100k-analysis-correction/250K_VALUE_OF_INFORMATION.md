# 250K Value of Information — 250k 能新增什么信息

**当前裁决：H1a NOT_CONFIRMED, H1b LOCAL_FAILURE_TRANSFER_UNRESOLVED, 250kApproved=false**

## 1. 现有100k已能回答的

- H1a：100k内无ND>100，已否定候选膨胀假说，不需250k。
- 局部拒绝：6条已观测，无需250k再证明“有拒绝”。

## 2. 现有500k历史不能回答的（但100k也不能）

- **时序对应**：500k确认实验（100_5_3_1, 757点reference）只有最终HV/IGD，无每PDDR cycle的checkpoint前沿，无法判断“PDDR拒绝是否发生在HV退化之前”。
- **字段缺失**：无完整MergePool分数表 → 无法计算真实cutoff score、替代者、等价非代表解；无checkpoint前沿 → 无法算生命周期HV/IGD。

## 3. 250k 若要批准，必须回答的二元问题

> **Q1：能否在250k内观察到代表拒绝、archive gap和checkpoint HV/IGD的明确先后关系？**（例如：A4在早期W2（cycle 6-9, FE 30k-65k）的 pool→PDDR是否稳定跌破85%且早于archive gap扩大，且该时序早于checkpoint HV/IGD退化）
> - 判定阈值：≥2/3 seed 同时 pool→PDDR <85% 且 firstGapExpandFE > firstRejectionFE，且HV/IGD退化在其后。
> - 干预：不改算法，仅延长预算至250k（MaxFEs=250000），其他 arm/window/mixture/LS/FM3/Shift 冻结。
> - 备注：500k历史无checkpoint HV轨迹，故不能表述为“早于500k历史HV退化区间”。

## 4. 为何现有500k不能替代250k

500k历史数据是**最终前沿**，无PDDR cycle日志；100k已有cycle日志但预算未覆盖到500k的phase 2-3的P/G冻结切换后期。250k能覆盖到 `W2_50K_100K` 之后的 `W3_100K_150K, W4_150K_250K` 早期中期。

## 5. 运行前置条件（必须已确认可记录）

1. 补全字段：`fullMergePoolScoreLedger` (或至少选中/未选中身份集合) + `checkpointFullFrontVectors` (每PDDR cycle 100个个体的3目标向量)
2. **轻量验证：2k/20k 验证（禁止直接500k影子）**：新字段落盘不改变行为哈希、FE、front SHA，front/Q/事件流逐位一致即可
3. 预注册：必须 **A2+A4配对臂 + 正例实例对照**（单A4-only 250k不能证明A2→A4退化或FC-5迁移），最小集如 1-2实例 × 3 seed × 2臂，明确二元问题与阈值（pool→PDDR是否早于gap扩大且与A2对照分离）

**当前：`nextExperiment=NOT_YET_PREREGISTERED`，仅保留 `250kApproved=false`。**

## 6. 若不补字段，即使跑250k仍是 NOT_COMPUTABLE

**结论：250k价值取决于先完成“2k/20k轻量字段可记录性”验证，而非直接跑500k。**
