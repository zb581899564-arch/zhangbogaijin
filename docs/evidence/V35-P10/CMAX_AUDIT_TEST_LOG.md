# V35-P10 Cmax audit integration log

## Scope

- 仅观察；不生成随机数、不评价候选、不改变PDDR/Qg/Qp/档案或FE。
- `V35FairRunner` 默认以1000 FE间隔挂接审计器。
- 历史 Shift-on/P9 审计与本记录隔离。

## Verification

`V35FairRunnerTest`（4 tests）通过；受控 FULL 小预算运行实际产生 `getCheckpoints()`，并输出 `candidateId,parentId,lineageId` 生命周期字段。Runner 同时输出 `cmax-audit-curves.csv`、`cmax-audit-records.csv` 和 `cmax-audit-summary.txt`。

当前尚未把 I1/20k 运行母表写入本目录，因此 P10 保持 `in_progress`，不回答历史 6750 FE 教师使用问题。

注意：当前审计器以“新 Cmax 纪录”作为候选生命周期索引；若某纪录后续被PDDR选中但没有新的更低 Cmax，仍需扩展到候选指纹全量索引后才能填充完整的PDDR/存活字段。这是下一步P10集成修复，不应把空字段解释为未发生PDDR。
