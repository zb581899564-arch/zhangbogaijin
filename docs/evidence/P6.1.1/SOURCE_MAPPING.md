# P6.1.1来源映射

| 实现 | 来源/约束 | 语义 |
|---|---|---|
| `ZhangBoEvaluatedPddrSelector` | 李明哲作者PDDR评分与用户批准的评价后时序 | `author_actual`评分，工程时序校正 |
| `ZhangBoMOHPSOQ.applyEvaluatedPddr` | P4.1直接派生主线与P6.1 CFVF评价链 | `fatigue_improved` |
| `EnvironmentalSelectionMode` | 用户批准的双模式兼容契约 | 默认保持`AUTHOR_PDDR_ACTIVE` |
| 候选记录与历史映射 | 用户批准的来源可追溯要求 | 后代在前、父代在后、稳定同分 |

