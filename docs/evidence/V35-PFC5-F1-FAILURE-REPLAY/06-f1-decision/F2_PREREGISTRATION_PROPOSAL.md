# F2 预登记建议

> **性质声明：本文档是建议，不是预登记，更不是启动。**
> `f2Eligible=true`、`f2Preregistered=false`、`f2Started=false`。
> F2 未启动、未上传、未创建任何任务。本文档不定义 `t*`，不下根因结论，不提出任何算法修改。

---

## 1. F2 为什么被解锁

`F1=FAILURE_CLASS_REPRODUCED` 成立，即：在**新鲜运行、精确历史快照、冻结算法 Jar、冻结 A4 profile、500000 FE、telemetry OFF** 的条件下，A4 的 500k 终态前沿相对历史 A2 配对基线满足

```text
deltaHV  = -0.31554307065117104  <  -0.05
deltaIGD = -1.7503285142217353   <  -0.20
```

按主计划预先规定的分支规则，F1 复现 failure class 是 F2 的**唯一**解锁条件。因此 F2 现在**具备资格（eligible）**，但按用户指令，F2 仍需用户单独授权并完成独立预登记后方可启动。

F2 的既定定位（照抄 `docs/ROADMAP.md` §13 状态表，未改动）：

```text
PFC5-F2  同case A4 500k ON   仅F1复现failure class后启动；行为等价失败即停止因果链
```

---

## 2. F2 建议冻结的合同（待用户确认后才算预登记）

F2 与 F1 必须构成**同实例、同 seed、同快照、同算法、同预算**的配对，唯一变量是 **telemetry 状态**。

| 项 | F1（已完成） | F2（建议） |
|---|---|---|
| instance | `100_5_3_1` | `100_5_3_1`（**同**） |
| seed | `20260901` | `20260901`（**同**） |
| 初始状态 | 精确历史快照 `84d84523…3769` | **同一**快照 `84d84523…3769`（**同**） |
| arm | `A4_BUDGET_AWARE_CATA` | `A4_BUDGET_AWARE_CATA`（**同**） |
| population | 100 | 100（**同**） |
| requestedMaxFEs | 500000 | 500000（**同**） |
| telemetry | **OFF** | **ON**（唯一变量） |
| 预算协议 | `PHASE_CONSISTENT_BUDGET_TERMINATION` | 同 |
| freshRunRequired | true | true（**不得复用 F1 运行**） |
| 正式算法 Jar | `8dad8f40…d8b9` | `8dad8f40…d8b9`（**同**） |
| armProfileSha256 | `5b3cc542…79d1` | `5b3cc542…79d1`（**同**） |
| runtimeConfigurationSha256 | `8c68f2a5…44b3` | 需另行确认（telemetry ON 对应的配置哈希可能不同，**必须由冻结 Jar 零 FE 复算后确定，不得推测**） |
| reference contract | `ecdc5589…235f` | **同**（不得更新） |
| PFref | `4dc85dd4…83da`（757 点） | **同**（不得加入任何新前沿） |
| 比较基线 | 历史 A2 | 历史 A2（**同**） |
| CPU 域 | 22-23 | 建议同 22-23，或经占用核查后另择不重叠核心 |

---

## 3. F2 需要预先解决的技术前提

1. **telemetry ON 的运行时入口**：F1 使用的冻结 Jar（8DAD8F40）**不含任何 telemetry 类**（`V35MidHorizonTelemetry`、`V35CheckpointFrontObserver` 等命中数均为 0），且其 `V35FairRunner` 只提供不含 telemetry 的 `run(...)` 重载。因此 telemetry ON **不能**由该冻结 Jar 直接提供。
2. 按 F1 的既有纪律，**禁止**为了开启 telemetry 而重建算法 Jar 或修改 `V35FairRunner`。
3. 因此 F2 预登记时必须先回答：telemetry ON 的运行时从何而来？
   - 若复用既有已封板的诊断 runtime（Step 0 身份链中的 `121FBB49` 诊断 runtime Jar / `723D24ED` 诊断 base Jar），则需先在预登记中论证其与冻结正式 Jar 的**行为等价性**，否则 F2 的 ON/OFF 对比不成立；
   - 若不存在行为等价的 telemetry ON 运行时，则 F2 在技术上不可执行，应在预登记阶段就判定为不可行，而不是带着不可比的运行时硬跑。

**这一点尚未解决，因此本文档明确不建议立即进入 F2 预登记。**

---

## 4. F2 必需的预登记五件套（对齐 F1 的规格）

1. 运行合同（`f2-run-contract.properties`）：预算协议、接收门、硬门
2. 输入清单（`f2-input-manifest.tsv`）：逐项 SHA-256，与 F1 逐条对照
3. 参考绑定（`f2-reference-binding.properties`）：明确复用 F1 同一冻结 contract 与 PFref，**禁止更新**
4. 停止门（`f2-stop-gate.properties`）：含「行为等价失败即停止因果链」
5. 预登记说明（`F2_FAILURE_REPLAY_PREREGISTRATION.md`）

---

## 5. 与 F3 的关系

按 ROADMAP §13 状态表，`PFC5-F3`（同 case A2 500k ON 配对）的启动条件是「F1、F2 通过且历史 A2 checkpoint 不可用」。F2 尚未预登记、尚未启动，因此：

```ini
f3Eligible=false
f3Started=false
```

本文档**不**创建 F3 任务，**不**为 F3 提出任何方案。

---

## 6. 状态与停止

```ini
f2Eligible=true
f2Preregistered=false
f2Started=false
f3Eligible=false
f3Started=false
```

F1 任务在此停止。是否进入 F2 预登记、以及如何处理第 3 节的 telemetry ON 运行时前提，需用户明确决定。
