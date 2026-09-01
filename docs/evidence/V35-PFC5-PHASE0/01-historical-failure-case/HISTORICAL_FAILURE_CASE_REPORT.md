# PFC5-1A 历史失败案例报告（100_5_3_1，A2 vs A4，500k）

- 生成时间：2026-08-29（UTC 10:39）
- 生成工具：`docs/evidence/V35-PFC5-PHASE0/tools/build_failure_seed_registry.py`
- 输入：`V35-A2-A4-MULTIINSTANCE-CONFIRMATION/06-remote-analysis-import/{metrics.csv, paired-deltas.csv, acceptance-run-audit.csv}`、`V35-PFC5-PHASE0/fetched-remote/`（10 份 raw front + 每条 run 身份元数据）
- 输出：`historical-failure-seed-registry.csv`、`registry-summary.json`
- 消耗FE：0；改变算法：否
- 授权下一阶段：本产物为 F1 预登记的输入；F1 本身仍需用户单独授权

## 1. 案例固定

```text
instance   = 100_5_3_1
comparison = A2_CFVF vs A4_BUDGET_AWARE_CATA
budget     = 500000 FE（全部 10 条 run status=ACCEPTED、actualFE=500000）
campaign   = V35 A2/A4 多实例确认（2026-08-24，冻结 Jar 8dad8f40…d8b9）
```

失败判据（用户 2026-08-29 批准，预登记于 `00-preregistration/PHASE0_PREREGISTRATION.md`）：

```text
failure class ⇔ deltaHV < -5%  AND  deltaIGD < -20%
（对配对历史 A2 终态前沿、500k 终态计算；Cmax 不作硬门）
```

## 2. 逐 seed 结果（全部 5 个候选 seed，无遗漏）

| seed | A2 HV | A4 HV | deltaHV | A2 IGD | A4 IGD | deltaIGD | deltaCmax | failureClass |
|---|---|---|---|---|---|---|---|---|
| 20260901 | 0.810244195 | 0.554577254 | **-31.55%** | 0.057804242 | 0.158980655 | **-175.03%** | +3.91% | **IN_CLASS** |
| 20260902 | 0.640371631 | 0.646500670 | +0.96% | 0.125338852 | 0.121688192 | +2.91% | +1.65% | NOT_IN_CLASS |
| 20260903 | 0.782730694 | 0.748590597 | -4.36% | 0.088109733 | 0.094200237 | -6.91% | +2.21% | NOT_IN_CLASS |
| 20260904 | 0.789101606 | 0.686804577 | **-12.96%** | 0.065121277 | 0.114818307 | **-76.31%** | +2.14% | **IN_CLASS** |
| 20260905 | 0.821824235 | 0.587187528 | **-28.55%** | 0.050184439 | 0.136778270 | **-172.55%** | +1.80% | **IN_CLASS** |

5 seed 中位：deltaHV = -12.96%，deltaIGD = -76.31%——与 `05-decision/A4_NOT_PROMOTED_DECISION.md`
记录的 100-job 否决门触发值一致（该历史裁决即本判据的来源）。

Cmax 方向 A4 全部 5 seed 占优（deltaCmax 均 > 0）——失败完全集中在 HV/IGD，
这正是 F1 失败门排除 Cmax 的实证依据。

## 3. seed 选择（确定性规则，无人工挑选）

```text
IN_CLASS seeds = {20260901, 20260904, 20260905}（升序）
selected       = 20260901（最小 seed ID）
```

如实登记三项事实：

1. 20260901 同时是本轮失败类中**退化幅度最大**的 seed（ΔHV/ΔIGD 双最差）。
   它被选中是因为规则锁定"最小 seed ID"，不是因为择差——规则先于结果冻结。
2. 20260901 同时是 Step 0 的工具验收 seed（TOOL_ACCEPTANCE_SEED，仅用于 50k OFF/ON
   工具验证）。主计划注明它"不得自动成为 Failure Replay seed"；本次选择经注册表
   规则独立产生，非自动沿用。工具验收运行（50k，诊断 Jar）与 500k 确认运行
   （正式 Jar 8dad8f40）是不同身份、不同预算、不同目的的运行，无证据混用。
3. 该 seed 的历史暴露：50k/100k 转移诊断、250k 中程诊断均使用过它。这些是
   只读诊断暴露，不改变初始种群身份（`initialPopulationHashV35=179a82a3…4c2d`
   在所有运行中一致）。

选中案例与 seed 统一标记：`CASE_SELECTED_DIAGNOSTIC_ONLY`。
禁止用于：Configuration Race、Validation、Formal Experiment、Final Test、论文正式统计。
该 seed 亦不得作为 hard100 Race seed。

## 4. 数据完整性核验

- 10/10 raw front 本地 SHA-256 与 `acceptance-run-audit.csv` 登记值逐一一致
  （`registry-summary.json: frontShaLedgerMatches=true`）。
- 10/10 run 的每 run 自带 `evidence-sha256.tsv` 中 front.csv 哈希同值（三账本互证：
  远端 per-run 账本 = 本地验收账本 = 拉回文件实测值）。
- 双臂 provenance 记录同一快照 `snapshotSha256=84d84523…3769`、同一
  `initialPopulationHashV35=179a82a3…4c2d`（配对有效性）；arm profile 哈希各异
  （A2=`12db64fb…`、A4=`5b3cc542…`，符合预期）。
- `checkpointFrontAvailable=false`：历史 500k 运行只记录了终态前沿与 Cmax 生命周期，
  无逐代前沿（`V35-FC5-100JOB-TRANSFER/02-field-availability/existing-field-availability.csv`）。
  这将触发 F3 条件（若 F1/F2 通过）。

## 5. F2/F3 含义（仅登记，不授权）

- F2（若 F1 复现失败类）将使用 seed 20260901 + 快照 `84d84523…3769` + telemetry ON。
- F3 触发条件第三条"historical A2 checkpoint unavailable"按第 4 节证据预判成立，
  即 F3（A2/500k/ON 配对）几乎必然需要，预算按 1.5M FE 预留——本阶段不授权、不建脚本。
