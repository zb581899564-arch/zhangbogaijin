# V35 Stage2 A2→A3 causal audit

审计时间：2026-08-24（本地只读分析）

## 范围与恢复

- G 盘归档：`G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\remote-campaigns\zhangbo-v35-stage2-master-v2-20260823.tar.gz`
- 恢复目标：`100_2_3_1`，seeds `20260808..20260819`，arms `A2/A3`。
- 恢复结果：24/24 个 run 目录，`large-events.zip` 24/24；另恢复 Stage2 pilot 聚合输出。
- 本报告由 `scripts/analyze_a2_a3.py` 生成；原始恢复文件未重写。

## 机制边界

A2/A3 共享 jar、snapshot、initial population、instance/setup/fatigue/problem hashes：配对 hash 不一致数为 `0`。两臂 profile 都是 `GLOBAL_ORIGINAL` PDDR、DSCR+CFVF+Qg、FM3、500000 FE；A2 为 `qp=false` 且 dual-Q 不适用，A3 为 `qp=true` 并强制 `BLOCK_FROZEN` dual-Q (`warmupRatio=0.1`, `blockLength=5`, `gBlockLength=5`, `GREEDY`)。因此 A2→A3 的高层消融是 PA_i+Qp，但运行时必须把 Qp 与 block-frozen 双 Q 一并审计；PDDR 不是本对比中发生变化的变量。

## 运行完整性

- status/formal gate 完整通过：`24/24`。
- exact max-FE 且 phase bound 无失败：`20/24`；phase-consistent tail stop 且 phase bound 无失败：`4/24`。两类都要保留，不能把 tail stop 误报为运行失败。
- 运行层面需保留的异常字段（illegal/duplicate、stopReason、frontSize）见 `run_inventory.csv`；不要用聚合 pilot FE 反推本归档 formal run 的预算状态。

## 12 seed pilot 结果

`pilot_a2_a3_pairs.csv` 是归档内 pilot 聚合的 A2→A3 配对表。描述性结果：

- ΔHV 中位数 `-0.162383`，负向 `7/12`；
- ΔIGD 中位数 `-0.249349`，负向 `8/12`；
- ΔCmax 中位数 `0.012524`。

这只能证明 A3 bundle 在本 pilot 中非单调，不能单独证明 Qp action、personal archive 截断或 dual-Q 调度中的某一个是根因。

## 机制计数与日志充分性

从 24 个 formal status 可直接看到 A2 的 Qp/档案计数为零，A3 的 Qp action、transition、archive insertion 和 Qg TD 更新均有记录。逐 seed 计数见 `mechanism_comparison.csv`；A3 的 Qp action 中位数为 `75000`，archive insertion 中位数为 `1700`，A2/A3 Qg TD 更新中位数分别为 `3200` / `1900`。

`large-events.zip` 只包含 PDDR/Cmax/DSCR/passive/shadow/pressure 类文件；24/24 有 `cmax-audit-records.csv`，但 0/24 有 Qp raw event、Q table dump 或 personal-archive update log。故：

- 预算、provenance、PDDR/DSCR/Cmax 生命周期的审计：日志足够；
- Qp state/action/reward、pbest fallback、personal archive dominated/duplicate/truncate 的因果归因：日志不足；只有 status 中的 count/hash，不能恢复事件顺序或奖励分量。

证据矩阵见 `evidence_matrix.csv`。

## 最小纯观察插桩点（本次未改源码）

源码检查确认 Qp 控制器本身已经在 `ZhangBoQpController.selectGroup` 记录 `select`，在 `settle` 记录 `reward`/`observeFrozen`，并公开 `getEvents()`、`getEventStreamHash()`、`getPbestSwitches()`、`getAverageReward()`、`getTableHash()`。`ZhangBoMOHPSOQ` 也公开 `getQpEvents()`；缺口在 `V35FairRunner`：当前 `mechanismSummary` 只写 Qp count/hash/tableHash，`writeRecord` 没有把 Qp event payload 写进 `large-events.zip`。

因此最小、行为等价的源码插桩应只把已有事件列表作为 sidecar 输出（例如 `qp-events.log` 与 count/hash summary），不改变 selection、settle、reward、archive 或 PDDR 分支。本次遵守“只负责新审计目录”，没有修改 `java-jmetal58` 源码；新目录内已加入 `diagnostic_record.py`、`qp_diagnostic_record.schema.json` 和 4 个本地单元测试，用于 future event export 的 fail-closed 解析。源码插桩尚未执行，故不存在新的算法行为可声称等价。

## 首先排查的变量与门

1. **配对保护门**：任何 frozen jar/snapshot/initial-population/problem hash 不一致立即停止；当前 `0` 个不一致。
2. **预算/终止门**：逐 run 检查 `actualFE`, `decoderCalls`, `terminationKind`, `phaseBoundFailure`, `formalGateFailures`；本归档先通过后再解释质量差异。
3. **dual-Q 调度门**：比较 A3 block-frozen warmup/P/G block 与 Qg TD 更新减少，确认是否改变了 Qg 学习/局部搜索阶段。
4. **Qp/PA_i 生命周期门**：需要 raw action、mask、selected pbest、fallback、archive update/truncate、reward components；当前缺失，不能宣称根因已定位。
5. **PDDR 共同路径门**：A2/A3 PDDR 都是 `GLOBAL_ORIGINAL`，只做共同候选保留损失审计，不将其误报为 A2→A3 的差异变量。

当前继续/停止判断：维持正式矩阵暂停；允许只读解析和本地日志诊断，不允许直接恢复远端 50k。若 raw Qp logs 无法取得，最多做 A2/A3 bundle 复现，不能完成内部机制因果分解。

## 待运行入口（本次未执行）

优先使用现有外部 launcher，单变量、低成本、人工批准后再运行。入口契约为：

```text
java -cp <frozen-classpath> org.uma.jmetal.runner.lc_psode.ZhangBoV35FormalAblationArmRunner --plan <plan.properties> --output <new-output-directory>
```

`plan.properties` 必须由既有 Stage2 manifest 生成，输出目录必须是新目录；不要覆盖归档或冻结 Jar。运行后只允许把输出交给本目录脚本复核：

```text
python scripts/analyze_a2_a3.py --restored-root <restored-root> --output-dir <audit-output>
```

以上命令仅写入本报告作为待运行入口，本次审计未启动 Java、Maven、SSH 或远端任务。

## 2026-08-24 补充诊断裁决

主Agent随后按批准范围完成了 `20_2_3_1 × A2/A3 × seeds 20260822..20260824 × 50000 FE`
六条纯观察运行。六条均精确闭合50000 FE，同seed初群一致，非法/重复评价为0；事件导出没有进入
搜索决策。详细证据见 `../local-50k-fixed20/A2_A3_FIXED20_50K_DIAGNOSTIC_REPORT.md`。

补充证据排除了预算/provenance、双Q阶段计数漂移和个人档案容量截断坍塌；同时直接发现Qp方向
奖励在 `oldPhi≈0` 时产生极端负值。由于A3仍是个人档案、Qp和block-frozen双Q的组合块，最终
因果分类按预注册停止条件保持：

```text
a2_a3_root_cause = COMPOSITE_BLOCK_UNRESOLVED
strongest_fault_candidate = QP_SELECTION_OR_REWARD_NUMERICAL_INSTABILITY
```

## 产物

- `run_inventory.csv`：24 个 run 的 profile/status/provenance/budget/formal gate/机制摘要。
- `profile_pair_comparison.csv`、`provenance_pair_check.csv`、`budget_pair_check.csv`：逐 seed 的差异与配对保护门。
- `mechanism_comparison.csv`：12 个 seed 的 A2/A3 机制计数与 pilot 增量。
- `pilot_metrics_a2_a3.csv`、`pilot_a2_a3_pairs.csv`：归档内 pilot 聚合和配对证据。
- `large_events_inventory.csv`：24 个 ZIP 的成员、Cmax/DSCR 行数及 Qp 原始日志缺口；`evidence_matrix.csv`：字段覆盖和缺口。
- `diagnostic_record.py`、`qp_diagnostic_record.schema.json`、`tests/test_diagnostic_record.py`：纯观察事件契约和本地 fail-closed 单元测试。
