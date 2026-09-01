# EQUIVALENCE_REPORT — 检查点观察器 OFF/ON 行为等价门（250k 包 §五）

- 日期：2026-08-31
- 执行域：本地工程门（非科学运行；确定性跨机先例：20k 批 `local20kFrontByteMatch=10/10`，本批 faithfulness 再次逐字节复现远端结果）
- 检查器：`equivalence_check.py`（掩码规则在文件头注册；掩码仅覆盖版本/运行ID/墙钟纳秒/观察器溯源字段/实验Jar哈希及其派生字段 profileSha256——全部为设计内差异，不含任何行为量）
- 数据：`behavior-equivalence.csv`（240 行）

## 1. 门定义与结果

### 20k 门（任务书 §五）
- instance=50_2_3_1，seed=20260907（已登记工程seed），arms=C0,C2,C3，MaxFEs=20000，OFF/ON，检查点目标{5000,10000,15000}（等比缩配，代码路径同 250k）。

### 50k 门（任务书 §五）
- instance=100_5_3_1，同 seed，同臂，MaxFEs=50000，OFF/ON，目标{12500,25000,37500}。

### 结果：全 PASS

```text
verdict counts: IDENTICAL=228, ON_ONLY=12, DIFFER=0
OFFvsON 比较 126 行，0 bad
faithfulness（V2-OFF vs 存储冻结运行）114 行，0 bad
ON 侧 6/6 run：4 检查点全部冻结于 target（overshoot=0）、terminal 双 front 有效、observerExecutionErrors=0
```

## 2. 任务书要求的等价项 → 覆盖证据映射

| 任务书要求 | OFF/ON 覆盖证据（逐字节一致项） |
|---|---|
| initialPopulationHash | initial-population.sha256 |
| actualFE / decoderCalls | formal-gate.properties（15258/20000 与 48269/50000/49036 全同） |
| RNG consumption hash | 注册为**行为迹复合哈希**：冻结Jar不导出显式RNG计数，等价性由全部可观测随机行为迹的一致性共同承载——p6/pddr/qg/qp/caTa 五条事件流哈希 + cmax-audit-records（候选级指纹序列）+ passive-archive（全部接纳解的非支配序）+ front.csv 全部逐字节一致 |
| candidate sequence hash | cmax-audit-records.csv（candidateId/lineage/fingerprint 序列）+ dscr-teacher-uses.csv + passive-archive.csv |
| PDDR selected identities | pddrEventStreamHash + archiveInsertions + passive-archive.csv |
| Qg/Qp action trace | qgEventStreamHash + qpEventStreamHash + qgSelections/qpActions/qpTransitions 计数 |
| Q-table hash | qgTableHash + qpTableHash |
| mechanism counts | mechanismSummary 全字段（teacherUses/dtur/dscr/双Q相位/局部FE记账等） |
| canonical terminal decision-front hash | front.csv 逐字节 |

ON 侧附加门：checkpointRows>0（实际=3检查点+2终端行，全部冻结）、frontsFinite=true（解析验证）、observerExecutionErrors=0。

## 3. 忠实性（V2-OFF ≡ 冻结Jar存储运行）

| 配置 | 存储来源 | actualFE 复现 | 结果 |
|---|---|---|---|
| 20k, 50_2_3_1, C0/C2/C3, seed 20260907 | `03-remote-20k/sync`（V1 jar，训练机） | 15258/20000/20000 | 逐掩码一致 |
| 50k, 100_5_3_1, C0/C2/C3, seed 20260907 | `08-remote-50k/sync`（V1 jar，训练机） | 48269/50000/49036 | 逐掩码一致 |

掩码=runnerVersion/runId/wallNanos/观察器溯源字段/experimentalJarSha256 及其派生 profileSha256（V1→V2 设计内差异）。其余全部逐字节一致——影子副本对冻结语义零漂移。

```ini
OFFON_20kGate=PASSED
OFFON_50kGate=PASSED
V2OFF_Faithfulness=PASSED
checkpointObserverValidated=true
250kReadyToRun=true
comparisonsTotal=240
identicalRows=228
onOnlyRows=12
differRows=0
```
