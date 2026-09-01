# FC-6A.3 实现一致性与候选池审计

状态：`engineering_smoke_validated`（2026-08-20）  
正式 500000 FE 顺序实验：`completed`；FC-6A.4 裁决保留当前顺序，详见
`../FC6A4-local-search-order/ORDER_DECISION.md`。

## 本次固定的候选池语义

- `PddrSelectionMode.GLOBAL_ORIGINAL`：仅按原始三目标 PDDR 的 `(score, originalOrder)` 排名前 100；这是 FC-6A.4 两臂唯一允许的模式。
- `PddrSelectionMode.BP_RESERVED_LEGACY`：只读历史兼容模式；FC-6 Runner 明确拒绝。
- `PddrSelectionMode.REGION_AWARE`：仅供 FC-6B，在 FC-6A.4 裁决确定局部搜索顺序后才允许启动。

每个父槽位在同一轮最多向 PDDR 提供一个最终局部承载解。一个后续局部动作覆盖已接受的先前局部候选时，先前候选记为 `superseded`，不再作为额外 PDDR 输入。这是候选池口径修正，不是新的搜索动作。

## 观察账本

`V35Fc6LocalCandidateAudit` 是纯观察组件。它按轮和来源记录：

- `PARENT`、`GLOBAL_Q_FINAL`；
- `CATA_TEST`、`CATA_APPLY`；
- `CRITICAL_SWAP`、`CRITICAL_INSERT`、`O1_O9`；
- 已评价、接受、覆盖、进入 PDDR、PDDR 选中/拒绝、来源 FE 与最佳生成 Cmax。

每次 PDDR 合并还记录精确 merge pool、原始 PDDR score/序号、是否选中和区域角色。审计关闭/开启的 2000 FE 重放已对比初始种群、FE、前沿、P6/PDDR/Qg/Qp 事件流及 Q 表哈希，均保持一致。

## 区域角色约定

FC-6B 固定物理槽位和容量：

| 物理槽位 | 语义角色 | 容量 |
|---|---:|---:|
| slot1 | `G1_CMAX` | 15 |
| slot2 | `G4_BALANCED` | 55 |
| slot3 | `G2_TEC` | 15 |
| slot4 | `G3_TWC` | 15 |

区域化选择只改变环境选择及下一轮粒子的槽位归属；Qg/Qp 规则不增加门控。跨区域教师曝光仅旁路记录。

## 已完成工程门

- `GLOBAL_ORIGINAL`、历史 BP 隔离和 `REGION_AWARE` 的单元测试；
- `CURRENT` 与 `ORDER_SWAP` 的 2000/6000 FE 配置隔离与可重放测试；
- `REGION_AWARE` 的 15/55/15/15 角色容量与下一轮槽位继承烟测；首次500k提交
  还捕获历史 `20/40/20/20` 运行时容量未重绑定的 fail-closed 缺陷，修复后20k回归
  与完整FC-6B重跑均通过容量门；
- 审计开/关行为等价测试。

这些测试不构成 500000 FE 的顺序裁决或区域化效果结论。
