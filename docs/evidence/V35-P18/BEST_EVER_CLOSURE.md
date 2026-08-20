# V35-P18 收口报告：三目标 Best-Ever 追踪器（不拼接虚假解）

生成日期：2026-08-13
验收标准：三目标极值独立追踪，不拼接虚假解。
附：本报告同时纠正 V35-P18 状态表行与工作包行的口径错配（原状态表行登记的是
CA-TA 确定性代价代理证据，与行内验收标准不符——该证据保留为本目录 `DETERMINISTIC_COST.md`
的 CA-TA 代价附属证据，不再充当 P18 验收证据）。

## 1. 实现（扩展 ZhangBoCmaxAudit，用户批准方案）

- 生成级三目标极值：`bestGenerated`（Cmax，已有）/`bestTEC`/`bestTWC`——`observeGenerated`
  在 Cmax 纪录门**之前**对**全部**被观察候选更新（含非纪录候选）；
- 档案级极值：`currentGlobal`（已有）/`currentTECGlobal`/`currentTWCGlobal`（`refreshState`
  对全局档案求目标 0/1/6 最小值）；
- **来源指纹**：`bestCmaxSource`/`bestTECSource`/`bestTWCSource`——每个极值都是某个
  真实已评价解的三目标中的一维，且记录该解的四向量指纹；
- 导出：`curvesCsv` 尾部追加 `bestTECGlobal,bestTWCGlobal,bestTECGenerated,bestTWCGenerated`
  （不改既有列序，既有固定列解析测试不受影响）；`summaryText` 增六极值字段；schema 升
  `cmax-audit-v4-v35-lifecycle-three-objective`（旧 v3 证据保持历史，如实登记）。

## 2. "不拼接虚假解"保证

API 只暴露每目标独立标量 + 各自来源指纹；**不存在**任何把三个极值拼成一个伪解的出口。
测试钉子：三极值来自三个不同解时，三个来源指纹**两两互异**（`ZhangBoCmaxAuditTest` 2 项）。

## 3. 双实例证据（BEST_EVER_METRICS.csv）

| 指标 | 20k（20_2_3_1） | 5k（I1 10_2_2_1） |
|---|---|---|
| bestCmaxGenerated | 196.162206 | —（见母表） |
| bestTECGenerated | **8994.850**（生成级） | 1954.541 |
| bestTWCGenerated | **13335.856**（生成级） | 2666.791 |
| bestTECGlobal（档案级） | 9030.005 | 1954.541 |
| bestTWCGlobal（档案级） | 13492.398 | 2666.791 |
| 三来源指纹 | 均非空 ✅ | 均非空 ✅ |

值得注意的差异：20k 上**生成级 TEC 极值（8994.85）优于档案级（9030.0）**——部分历史
极值只存在于被淘汰候选而非全局档案，这正是独立 Best-Ever 追踪器相对"靠全局档案保存
极值"的价值；两口径分开报告，不做拼接。

## 4. 与 CFVF 观察缺口修复的联动

本轮修复（见 V35-P17 报告 §4 与 D-069）后，20k FULL 的纪录来源含 `CFVF/CFVF=6`，
"CFVF 零新纪录"（D-063/D-064）更正为观察缺口伪影。

## 5. 证据清单

| 文件 | 说明 |
|---|---|
| `BEST_EVER_METRICS.csv` | 双实例六极值 + 来源指纹 |
| `DETERMINISTIC_COST.md` | 既有 CA-TA 确定性代价证据（保留，不再充当 P18 验收证据） |
| V35-P17 目录三臂运行证据 | 审计器 v4 母表（curves 含新列、summary 含六极值） |
