# V35-PFC5-F1 裁决

```ini
F1=FAILURE_CLASS_REPRODUCED
```

---

## 1. 裁决摘要

| 项 | 值 |
|---|---|
| runId | `V35PFC5F1-100_5_3_1-20260901-A4` |
| replayKind | `HISTORICAL_STATE_FAILURE_REPLAY` |
| 运行验收 | PASS（33/33 硬门） |
| processExitCode | 0 |
| requestedMaxFE / actualFE / decoderCalls | 500000 / **500000** / **500000** |
| remainingFE | **0** |
| utilizationRate | 1.000000000000 |
| terminationKind | `EXACT_MAX_FE` |
| 冻结 Jar | `8dad8f40…d8b9`（一致） |
| armProfile | `5b3cc542…79d1`（一致） |
| snapshot | `84d84523…3769`（一致） |
| 初群 V35 / P8 | `179a82a3…4c2d` / `7c6f8b42…2d3`（一致） |
| telemetry | OFF |
| **HV（F1 A4）** | **`0.5545772540415207`** |
| **IGD（F1 A4）** | **`0.15898065502479636`** |
| **HV（历史 A2）** | **`0.810244195451609`** |
| **IGD（历史 A2）** | **`0.057804242003353316`** |
| **ΔHV** | **`-0.31554307065117104`** |
| **ΔIGD** | **`-1.7503285142217353`** |
| 失败门 | ΔHV < −0.05 **且** ΔIGD < −0.20 → **触发** |
| **F1 裁决** | **`FAILURE_CLASS_REPRODUCED`** |

minCmax = `755.144349612787`，minTEC = `113858.2152135067`，minTWC = `307754.57119086105`。Cmax 按合同为 `NOT_A_HARD_GATE`，仅作机制解读参考，不参与裁决。

F1 只判断 500k 终态前沿，不判断 checkpoint 持续性。

---

## 2. 裁决依据

冻结合同规定的四类结论只能取其一。本次：

- 运行硬门 33/33 通过 → 不是 `RUN_INVALID`
- 参考合同与 PFref 哈希一致、gold 自检绝对偏差 0.0 → 不是 `REFERENCE_INVALID`
- ΔHV 与 ΔIGD 同时低于失败门 → **是 `FAILURE_CLASS_REPRODUCED`**，不是 `FAILURE_CLASS_NOT_REPRODUCED`

判据与用户指令、与 `reference-contract.properties` 的机器可读定义**符号一致**，无需以合同修正用户指令中的符号表达。

---

## 3. 附带的确定性记录（不改变裁决）

F1 终态前沿与历史 A4 运行的终态前沿**逐字节相同**（`f3755d83…`，387 点），机制计数（62 外循环、3100 Qg 轮次、qgSelections=12400、qpActions=271800、cfvfOffspring=310000、caTaLiteTest=11502、teacherUses=12400、全部事件流哈希）亦逐项一致。

这说明该失败类在当前冻结代码 + 冻结输入 + 冻结快照下是**确定性可复现**的，不是环境噪声。**但本节不构成裁决依据**——裁决基线自始至终是历史 A2，且 F1 是新鲜运行（`freshRunRequired=true`、`reuseHistoricalRunAllowed=false`），所有 500000 FE 均为本次实际消耗。

---

## 4. 分支纪律

按用户预先规定的分支规则，`FAILURE_CLASS_REPRODUCED` 分支下：

```ini
f2Eligible=true
f2Preregistered=false
f2Started=false
f3Eligible=false
f3Started=false
FC5_HISTORICAL_CASE=OPEN
```

已做的（仅建议，非启动）：

- 生成 F2 预登记建议 → `06-f1-decision/F2_PREREGISTRATION_PROPOSAL.md`
- 说明 F2 为什么被解锁
- 列出 F2 所需的同实例 / seed / snapshot / A4 / 500k / telemetry ON 合同

未做且不会做：

```text
未自动启动 F2
未自动上传 F2
未创建 F3 任务
未定义 t*
未下根因结论
未修改算法、PDDR、Qp/Qg、CFVF、CA-TA、FM3、V35FairRunner、ZhangBoMOHPSOQ
未重建算法 Jar
未启动 Teacher Exposure Calibration / Configuration Race / Gap Probe / Validation / 正式矩阵
未跑任何额外 seed 或实例
```

---

## 5. 停止声明

```text
F2 has NOT started.
F3 has NOT started.
Formal matrix is NOT running.
Algorithm and PDDR were NOT changed.
```

本任务到此停止，等待用户下一步明确授权。
