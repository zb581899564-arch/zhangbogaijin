# PFC5-CAL 路线建议（假设驱动 Teacher Exposure Calibration）

> **性质声明：本文档是路线建议，不是预登记，更不是启动。**
> `calPreregistered=false`、`calStarted=false`。
> 未定义任何旋钮取值，未提出任何算法修改，未下根因结论，未消耗任何 FE。

---

## 1. 为什么转入这条路线

按用户预置判据 4：F2 字段不足时，**不继续反复改遥测工具**，按主路线将 FC5 记为机制未解析，转入假设驱动的 Teacher Exposure Calibration。

触发事实（详见 `F2_DEPLOYABILITY_AUDIT.md`）：

```ini
F2=NOT_DEPLOYABLE_FIELDS_INSUFFICIENT
blockingField=CFVF
FC5=MECHANISM_UNRESOLVED
```

注意 `MECHANISM_UNRESOLVED` 的准确含义：**不是**"机制已查明不是 Teacher"，也**不是**"根因是 Teacher"。而是：在冻结的诊断 schema 下，F2 无法为 A4 的失败机制提供可归因的观测（CFVF 占 500k 评价预算的 62%，却完全不可观测），因此**当前没有足够的观测能力去定因**。这正是"假设驱动"路线的适用前提——先提出可反驳的假设，用实验去检验，而不是继续在观测能力不足的通道上耗。

## 2. 主路线早已把这条线冻结为下一站

`docs/ROADMAP.md` D-109 节（2026-08-29）冻结的唯一主线：

```text
Failure Replay
→ Single-Knob Teacher Exposure Calibration（仅在Gate授权后）
→ Multi-instance Configuration Race
→ Top2 500k
```

且 `PFC5-CAL` 工作包的启动条件原文为：

> 根因明确指向 Teacher，**或 FC5 仍 unresolved 后走假设驱动路线**

后件已成立。因此本路线切换是**主计划内既定分支**，不是新开路线。

## 3. 一个重要的可行性事实

本次审计同时确认：在五个字段域中，**Teacher 域是诊断 runtime 覆盖最强的一域**——

- `telemetry-teacher-use-events.csv`：43 列，含 `teacherSource`、`cacheType`、`qSystem`、`qState`、`qAction`、`scope`、`directionalRegret`、`teacherFingerprint`、`offspringFingerprint`、`offspringImproved`
- `telemetry-teacher-concentration.csv`：`exposures`、`uniqueTeacherCount`、`top1Share`、`top5Share`、`shannonEntropy`、`normalizedEntropy`、`cyclesObserved`、`exposuresPerCycle`
- 契约层：`teacherContractPass=true`、`teacherOutcomeLifecycleValidated=true`

即：**冻结的观测能力恰好足以支撑它所转向的这条路线。** 这不是巧合——主路线在 D-109 把 Teacher 列为 Failure Replay 之后的下一站，而遥测的能力边界与之一致。

## 4. 背景信号（不是结论，只是既有记录）

以下来自既有决策记录，仅作为假设来源登记，**不构成根因判断**：

- D-107 描述的机制链末端为「继而缺少**教师暴露**与有效后代」。
- D-108 第二档 100k 记录：`A4 后半段 cmaxGap 转正峰值 3.65–5.94（相对 Cmax<1%）且 W2 **教师曝光回落（23806→3693）**`，但该档裁决为 `FC5_TRANSFER_100K_INCONCLUSIVE`（情形 C），未构成确认。
- D-109 第三档 250k：预注册的「ND 候选膨胀 → PDDR 容量压缩 → 四方向代表利用断裂」链条**未复现**，裁决 `FC5_TRANSFER_NOT_CONFIRMED_AT_250K`，PDDR 降级为旁路观察。

因此可形成的**可反驳假设**（待预登记时正式表述，本次不定稿）：A4 的终态退化可能经由"后段教师暴露回落 → 有效后代减少"这一路径，而与 PDDR 容量压缩无关。它需要独立预登记和独立的证伪条件，不能由现有记录推定。

## 5. PFC5-CAL 预登记需要什么（待用户授权后才展开）

| 项 | 说明 |
|---|---|
| 旋钮定义 | C0/C1/C2/C3 的具体含义与取值域。**本次不定义。** |
| 单旋钮纪律 | 每次只动一个 teacher exposure 旋钮；其余组件冻结 |
| 冻结边界遵守 | §13.2 禁止修改 PDDR、mixture、Pacing、rho、P5/G5、Q 状态/动作/奖励、个人档案容量、CFVF、CA-TA、LS 顺序。Teacher exposure 旋钮虽不在该清单内，但**任何算法改动都需独立预登记 + 用户明确批准** |
| 尺度与条数 | 需按项目既有纪律从最小档起步并逐级升级（参照 FC5-T 的 `50k → 100k → 250k → 必要时 500k` 升级范式），**不得直接跳到 500k 或正式矩阵** |
| 实例与 seed | 需明确；建议复用已暴露的困难/正例配对，但不在本文档内指定 |
| 判据与证伪条件 | 必须事前冻结，含"不成立即回退"的明确出口 |
| 观测通道 | 若需遥测，只能使用冻结的 7 类 schema；**CFVF 不可观测这一事实必须写进预登记的已知局限** |
| 与 F1 的关系 | F1 结论 `FAILURE_CLASS_REPRODUCED` 保持不变；CAL 若推进，`FC5_HISTORICAL_CASE` 仍为 `OPEN` 直到假设被检验 |

## 6. 不得做的事

```text
不得自动启动 PFC5-CAL
不得自动预登记 PFC5-CAL
不得定义 C0/C1/C2/C3 的具体取值
不得修改算法、PDDR、teacher 选择逻辑、CFVF、CA-TA
不得启动 Configuration Race / Gap Probe / Validation / 正式矩阵
不得为了补齐 CFVF 字段而迭代遥测工具（用户判据 4 已禁止）
```

## 7. 状态

```ini
F2=NOT_DEPLOYABLE_FIELDS_INSUFFICIENT
f2Preregistered=false
f2Started=false
FC5=MECHANISM_UNRESOLVED
nextRoute=HYPOTHESIS_DRIVEN_TEACHER_EXPOSURE_CALIBRATION
calPreregistered=false
calStarted=false
consumedFE=0
```

本任务到此停止，等待用户就是否授权 PFC5-CAL 预登记作出决定。
