# V35-P12 DSCR mechanism gate

日期：2026-08-13

本轮修正将 `V35DscrTeacherCache` 降为观察账本，Qg 的 `previous` 与 `historical` 才是实际教师缓存。每次 Qg 选择前冻结已评价社会快照，并按 G1/Cmax、G2/TEC、G3/TWC、G4/normalized-max-deviation 清洗实际缓存。

指标口径：

```text
DTUR = dominatedTeacherUses / teacherUses
SCRR = replacements / validityChecks
```

事件 CSV 包含 decisionCycle、generation、FE、cacheType、strict-dominator 计数、首次发现 FE、刷新 FE、dominanceAge 和 teacherExposure。20k QG0/QG1 单变量配对已完成：两条路径共享初始种群哈希；QG1 `teacherUses=200`、`dominatedTeacherUses=0`、`DTUR=0`、`validityChecks=392`、`replacements=48`、`SCRR=0.122448979592`，并生成独立properties摘要、事件表和teacher-use表。I1完整链路仍未完成，因此本工作包保持 `in_progress`。
