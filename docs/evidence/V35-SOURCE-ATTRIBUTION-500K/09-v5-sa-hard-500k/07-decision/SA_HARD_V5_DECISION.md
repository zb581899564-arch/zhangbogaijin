# SA-HARD V5 500k 决策（V35-SOURCE-ATTRIBUTION-500K / 09-v5-sa-hard-500k）

**日期：** 2026-09-01
**授权链：** D-112（Phase A 计划 v1.0）→ Phase A0（PHASE_A0_PREREGISTRATION_PASSED）→ D-113（V4 来源合同退回、V5 工程门通过）→ 用户本次明确授权"用已冻结 Observer V5 重跑唯一一条 SA-HARD 500k"。
**本包唯一动作：** 一条 SA-HARD V5 500k；不重新开发观察器；**不自动启动 SA-NORMAL**。

## 1. 裁决

```ini
V5_SA_HARD_500K_STARTED=true
V5_SA_HARD_500K_COMPLETED=true
RUN_ACCEPTANCE=PASSED(61/61)
FAILURE_CLASS_REPRODUCTION=PASSED
HARD_WINDOW_EVIDENCE=COMPUTED(20/20)
HARD_NORMAL_DEFICIT=NOT_COMPUTABLE
SOURCE_LEVER_CANDIDATE=NONE
SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
```

## 2. 依据

| 判据 | 结果 | 证据 |
|---|---|---|
| 运行前硬门 9 项（Jar/Snapshot/输入/初群/profile/空间/目录/PID） | 全 PASS | `01-staging/artifact-binding.properties`、`00-preregistration/SA_HARD_V5_PREREGISTRATION.md` |
| processExitCode / status / failures | 0 / COMPLETED / NONE | `02-remote-run/logs/`、`03-run-acceptance/run-acceptance.csv` |
| 预算 `0<actualFE≤500000`、`remainingFE∈[0,5000)`、`utilizationRate>0.99` | 500000 / 0 / 1.000000 / EXACT_MAX_FE | `budget-termination.properties` |
| 完整性（illegal/dup/repair/sourceLoss/observerErrors/UNSET） | 全 0；`sourceLedgerRows=actualFE=500000` | run-acceptance.csv |
| Observer V5 专用门（ledger 五列 + lifecycle 十类事件） | 全齐 | run-acceptance.csv |
| B0 独立重算逐点一致 | 11/11 | run-acceptance.csv |
| 检查点口径（19+1 非B0 + B0；overshoot < 5000） | 19+1+1；overshoot=0 | `03-run-acceptance/checkpoint-registry.csv` |
| 正式 Jar 运行前后 SHA 不变 | 一致 | `logs/launch-env.properties` / `logs/run-closeout.properties` |
| 终态前沿与历史 A4 一致 | `frontSha256Raw=f3755d83…1239bdd`，规范排序亦一致 | `04-failure-reproduction/frozen-reference-analysis.properties` |
| 失败门（gold 自检 1e-12 通过后计算） | `deltaHV=-0.3155 < -0.05`、`deltaIGD=-1.7503 < -0.20` → 触发 | `04-failure-reproduction/failure-class-reproduction.csv` |

## 3. 未决（必须等待 SA-NORMAL）

`HARD_NORMAL_DEFICIT`、G1/G3、`t_div` 全部 `NOT_COMPUTABLE / UNDECIDED`。单条 HARD 轨迹只能给出 HARD 侧窗口证据
（`05-hard-source-analysis/`），不能完成 hard–normal 差值门，因此**不得**宣布 G1 或 G3 成立，
也**不得**把"GLOBAL_CFVF 占 62% 评价量"或任何窗口份额读作根因。

## 4. 登记偏差

1. **堆峰值外推偏差**：20k 分解模型预测 `estimated500kPeak` 比值 0.3221（≈1.29 GB），实测 3.5666 GB（约 2.8 倍）。
   与 V4 500k 同类偏差（2.92 倍）一致。堆按任务书固定 4 GB 且未扩堆，运行 `COMPLETED`、exit 0、观察器缓存有界，
   故登记为模型外推精度问题，**不**作为运行失败项，也**不**授权任何扩堆。
2. **生命周期归属缺口**：利用层事件中 `PERSONAL_ARCHIVE`、`QP_TEACHER`、`QP_ACTION` 的主体指纹不在评价账本中，
   无法按来源归属（共 1,323,122 行），已如实登记为 `NOT_ATTRIBUTABLE_BY_FINGERPRINT_JOIN`，未做任何猜测填补。
   需要新的观察钩子时属未来观察器工作包，须另行批准。

## 5. 停止边界（本包结束后保持）

```ini
SA_NORMAL_STARTED=false
SA_A2_CONDITIONAL_STARTED=false
SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
DOE_AUTHORIZED=false
QP_V2_AUTHORIZED=false
CONFIG_RACE_AUTHORIZED=false
VALIDATION_AUTHORIZED=false
FORMAL_AUTHORIZED=false
formalMatrixRunning=false
PDDRChanged=false
CFVFChanged=false
DualQChanged=false
CaTaChanged=false
formalJarChanged=false
algorithmSemanticsChanged=false
```

## 6. 下一步（须用户单独批准）

唯一在册的下一步是 `SA-NORMAL`：`100_2_3_1 / 20260901 / A4 / C0_BETA_MAX_065 / 500k / observer V5 ON`，
使用同冻结 Jar 与 V5 观察器、同一 25k 检查点网格，随后才能计算 hard–normal deficit、`t_div` 与 G1/G3。
本文件不授权该运行。
