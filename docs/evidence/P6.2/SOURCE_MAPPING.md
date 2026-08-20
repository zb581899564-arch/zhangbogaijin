# P6.2来源映射

| 实现 | 方案要求 | 当前边界 |
|---|---|---|
| `ZhangBoArchiveEntry` | 四向量、三目标、疲劳风险与来源可追溯 | 不保存完整轨迹/甘特 |
| `ZhangBoArchiveBounds` | 每代冻结统一归一化尺度 | 全局集合只供边界，不注入个人档案 |
| `ZhangBoPersonalArchive` | 严格Pareto、近重复、容量6、方向/epsilon/多样性截断 | 影子记忆，不选pbest |
| `ZhangBoLineageTag/Memory/Coordinator` | 继承、分裂、删除、迁移与稳定ID | 当前生产只接父代和全局后代；局部来源由合成测试验收 |
| `ZhangBoGlobalSearchConfiguration` | B2P/B3独立开关与默认关闭兼容 | Q-pbest留到P6.3 |

