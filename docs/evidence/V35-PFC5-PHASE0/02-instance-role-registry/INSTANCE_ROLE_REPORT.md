# PFC5-1C 实例角色登记报告

- 生成时间：2026-08-29（UTC 10:53）
- 生成工具：`docs/evidence/V35-PFC5-PHASE0/tools/build_instance_role_registry.py`
- 数据源：`V35-FORMAL-MANIFEST/FORMAL_INSTANCE_MANIFEST.csv`（45 实例 roster 及 SHA）、
  `docs/evidence/` 全部 89 个 campaign 目录的结构化 run 证据扫描（status.properties、
  run 台账 CSV、run 输出目录路径段）、`java-jmetal58/EADHFSP*.txt`（规模解析）、
  少量人工核实补充（P8/P8.3/P9/P25D 均在 20_2_3_1 上运行）
- 输出：`instance-exposure-role-registry.csv`、`role-registry-summary.json`
- 消耗FE：0；改变算法：否

## 普查结果

共登记 **49 个实例**（45 个正式 roster + 4 个试点目录实例）：

| 角色 | 数量 | 实例 |
|---|---:|---|
| CASE_SELECTED_DIAGNOSTIC_ONLY | 1 | 100_5_3_1 |
| CONTAMINATED_DEVELOPMENT | 17 | 20_2_3_1, 20_2_4_1, 20_2_5_1, 20_5_3_1, 20_5_4_1, 20_8_3_1, 50_2_3_1, 50_2_4_1, 50_2_5_1, 50_5_3_1, 50_5_4_1, 50_8_3_1, 100_2_3_1, 100_2_4_1, 100_2_5_1, 100_5_4_1, 100_8_3_1 |
| VALIDATION_RESERVED | 27 | 全部 150/200-job（12个）、100_5_5_1、100_8_4_1、100_8_5_1、20_5_5_1、20_8_4_1、20_8_5_1、50_5_5_1、50_8_4_1、50_8_5_1、20_8_4_1 等零暴露 roster 实例 |
| LEGACY_EXCLUDED | 4 | 10_2_3_1, 10_3_2_1, 3_2_2_1, 5_2_2_1（试点/接线专用，不在正式 roster） |

## 暴露判定方法（可复算）

`run 级暴露`只认结构化运行证据：`status.properties` 的 `instance=` 键、
表头含 `runId` 或（`seed`+`arm`/`status`）的台账 CSV、以及位于
`/results/`、`/runs/`、`/raw/`、`/output*`、`/04-development-runs/` 之下以实例 ID
为路径段的文件。**物化清单不算暴露**：`V35-FORMAL-MANIFEST/`（45×20 快照物化）与
`V35-MASTER-CAMPAIGN/01-registry/`（4500 计划 RunKey 登记）被显式排除——Master
矩阵只对 100_2_3_1 实际跑过 12 组先导，该实例已因真实 run 被标 CONTAMINATED_DEVELOPMENT。

主要暴露来源（与登记表 `historicalCampaigns` 列一一对应）：

- `V35-A2-A4-MULTIINSTANCE-CONFIRMATION`（=DOE-1 确认，60 条 500k run）：6 实例
- `V35-A2-FINAL-CANDIDATE-CONFIRMATION`（A0/A2 确认）：6 实例
- `V35-FC5-100JOB-TRANSFER`（50k/100k）、`V35-FC5-MIDHORIZON-250K`、
  `V35-FC5-MIDHORIZON-DIAGNOSTICS`（121 runtime/V31）：100_5_3_1、100_2_4_1
- `V35-STAGE2-PILOT-A0-A4-20260823`、`V35-STAGE2-MASTER-V2`（五臂先导）：100_2_3_1
- `V35-DOE1-subgroup-mixture`（真实 run 目录）：20/50/100_2_3_1、20/50/100_5_4_1
- P8/P8.1/P8.3 消融与性能门、P9 单次+5seed、`V35-P25D-all-algorithms-50k-pilot`：20_2_3_1

## 规则核查（与工作包硬规则逐条对照）

1. `100_5_3_1` = `CASE_SELECTED_DIAGNOSTIC_ONLY` ✓（未来只允许诊断用途，
   禁止 Race/Validation/Formal/Final/论文统计）。
2. 参与过 DOE、FC5、A2/A4 确认或算法决策的实例全部落入
   CONTAMINATED_DEVELOPMENT / CASE_SELECTED_DIAGNOSTIC_ONLY，
   无一被标 VALIDATION_RESERVED ✓。
3. VALIDATION_RESERVED 的 27 个实例均为"零 run 级暴露 + 完整 SHA 元数据"的
   roster 实例；`roleReason` 注明这是 2026-08-29 普查下的默认保留分配，
   重新分类须用户批准（普查法无法排除未电子化的口头决策，故保留复审口子）✓。
4. 未发现信息残缺到无法判定的实例（全部 49 个都有文件级证据），
   因此本轮 `UNKNOWN_NEEDS_REVIEW` 为空；若后续发现新暴露源，按规则改判并登记 ✓。

## 对后续工作包的含义

- Configuration Race 的 4 个 DEVELOPMENT 实例（20/50/normal100/hard100）应从
  CONTAMINATED_DEVELOPMENT 池选取（如 20_2_4_1、50_2_4_1、100_2_4_1/100_2_5_1/100_8_3_1
  作为 hard100 候选）；diagnostic seed 20260901 不得作为 hard100 Race seed。
- Validation Mini Benchmark 从 27 个 VALIDATION_RESERVED 中按 50/100/150-or-200
  各至少 1 个选取（首档 1 seed Go/No-Go）。
- 本登记表不授权任何运行；实例→用途的最终指派由对应预登记工作包提出、用户批准。
