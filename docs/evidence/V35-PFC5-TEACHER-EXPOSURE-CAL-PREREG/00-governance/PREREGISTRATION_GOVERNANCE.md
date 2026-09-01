# 治理与科学身份

工作包：`V35-PFC5-CAL-P0`　|　日期：2026-08-30

---

## 1. 前置状态确认（任务 §一）

七项逐项核对，全部一致，未触发「停止并报告」：

| 项 | 期望 | 实测 | 一致 |
|---|---|---|---|
| F1 | `FAILURE_CLASS_REPRODUCED` | `FAILURE_CLASS_REPRODUCED` | ✓ |
| F2 | `NOT_DEPLOYABLE_FIELDS_INSUFFICIENT` | 同 | ✓ |
| F3 | `BLOCKED` | `BLOCKED_BY_F1_F2`，`f3Started=false` | ✓ |
| FC5 | `MECHANISM_UNRESOLVED` | 同 | ✓ |
| PFC5-CAL | `ROUTE_OPEN_NOT_PREREGISTERED` | 同 | ✓ |
| formalMatrixRunning | `false` | `false`（`formalMatrix=PAUSED`） | ✓ |

数据来源：`V35-PFC5-F1-FAILURE-REPLAY/06-f1-decision/F1_DECISION.properties`、
`V35-PFC5-F2-DEPLOYABILITY-AUDIT/01-decision/F2_DEPLOYABILITY_DECISION.properties`、
`docs/ROADMAP.md` 状态表与 §13.2 冻结边界。

---

## 2. 科学身份（任务 §三）

```ini
experimentKind=HYPOTHESIS_DRIVEN_DEVELOPMENT_CALIBRATION
rootCauseConfirmed=false
teacherRootCauseConfirmed=false
formalAblation=false
DOE=false
paperStatisticalExperiment=false
```

### 研究假设（唯一许可表述）

> 在 PDDR 容量迁移假设未获 250k 支持、F1 又确定性复现 A4 覆盖退化，而 F2 无法完成全组件遥测的条件下，过度集中的教师身份暴露是当前最有根据、但尚未证实的可反驳发展假设。

### 禁止的表述（本工作包及后续均不得出现）

```text
DualQ 已被证明是根因
Teacher 已被证明是根因
CFVF 已被排除
CA-TA 已被排除
PDDR 在所有实例都没有问题
```

**明确声明**：`FC5=MECHANISM_UNRESOLVED` 的含义是「当前观测能力不足以定因」，**不等于**根因已排除 Teacher，也**不等于**根因是 Teacher。本 Calibration 是**假设检验**，不是已定因后的修复。

---

## 3. 措辞修正记录（任务 §二）

已在进入本工作包前完成，范围仅限 `V35-PFC5-F2-DEPLOYABILITY-AUDIT/`：

| 项 | 旧 | 新 |
|---|---|---|
| 表述 | 开启 telemetry 反而丢失 CFVF 可见性 | 诊断 runtime 缺少 CFVF 事件级遥测，仅保留有限聚合计数 |
| 键 | `telemetryOnLosesCfvfVisibility=true` | `cfvfEventTelemetryMissing=true` + `cfvfAggregateCountAvailable=true` |

**未改变**：`F2=NOT_DEPLOYABLE_FIELDS_INSUFFICIENT`、`blockingField=CFVF`、`FC5=MECHANISM_UNRESOLVED`，以及全部证据数值。

前后 SHA-256 与重建后的证据清单见
`V35-PFC5-F2-DEPLOYABILITY-AUDIT/01-decision/WORDING_REVISION_RECORD.md` 与同目录 `evidence-sha256.tsv`（6/6 闭合）。

---

## 4. 本工作包的禁止清单与自查

| 禁止项 | 自查结果 |
|---|---|
| 修改 Java 源码 | 未改（`source-hashes.tsv` 为实测，未写入任何文件） |
| 构建新算法 Jar | 未构建 |
| 实现 Teacher selector | 未实现，仅写边界与测试合同 |
| 运行 2k/20k/50k/250k/500k 实验 | 未运行 |
| 上传训练机 | 未上传 |
| 启动 F2/F3 | 未启动 |
| 启动 Configuration Race | 未启动（仅预登记 32 条 RunKey） |
| 修改 PDDR / CFVF / 双Q动作·奖励 / CA-TA | 未修改 |
| 恢复正式矩阵 | 未恢复 |

```ini
consumedFE=0
changedAlgorithm=false
uploadedAnything=false
```

---

## 5. 冻结不变量与 Calibration 的唯一自由度

Calibration 只可改变：

> Q 动作已经确定以后，在该动作原本合法的 teacher candidate set 中，选择哪个 teacher identity。

其余全部冻结（FM3、ShiftMode=NONE、single family、sequence-independent SUT、PDDR=GLOBAL_ORIGINAL、mixture=20/40/20/20、Pacing、CA-TA-Lite→inherited LS、CFVF 语义、Qg/Qp 动作集合、Q 状态、Q 奖励、P5/G5、warmup=10%、rho=0、个人档案容量=6、DSCR、CA-TA、inherited LS、方向教师池=OFF、population=100、objective slots=[0,1,6]）。

调用链保持：

```text
Q action → 该 action 原合法候选集合 → teacher identity selection → 实际 CFVF 行为 → reward 返回原 Q action
```
