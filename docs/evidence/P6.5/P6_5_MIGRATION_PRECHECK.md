# P6.5 子群语义迁移前检查

日期：2026-08-09  
状态：`in_progress`

## 已确认冲突

当前张博代码的角色标签为 `G1_CMAX/G2_CENTER/G3_TEC/G4_TWC`，与李明哲原始四子群职责不一致。目标语义固定改为：

`G1_CMAX → Cmax`、`G2_TEC → TEC`、`G3_TWC → TWC`、`G4_BALANCED → 平衡/PDDR`。

## 物理槽位保留

为保持默认 `author_actual` 行为，作者四个物理粒子槽位及更新顺序不重排：

`groupU1 → G1_CMAX`、`groupD3 → G2_TEC`、`groupUNew → G3_TWC`、`groupC2 → G4_BALANCED`。

## 迁移边界

- 原作者类、只读基线和历史证据不修改。
- P5解码、疲劳、编码黄金夹具不因子群迁移而失效。
- 旧Q表、VNS统计和subgroup-aware结果不自动迁移，统一标记为 `legacy_pre_subgroup_migration`。
- P6.5完成前不得开始P7.2 CA-TA主循环接入。
