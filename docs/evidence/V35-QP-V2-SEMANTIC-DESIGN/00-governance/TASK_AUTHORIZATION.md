# V35-QP-V2-SEMANTIC-DESIGN（Phase B0.5）治理记录

工作包ID：`V35-QP-V2-SEMANTIC-DESIGN`（Phase B0.5：Qp-v2 单轴 K 语义设计与候选裁决）
执行日期：`2026-09-02`
性质：**0-FE 纯设计工作包**——只做科学定义、源码可行性审计与预注册设计，不写实现、不编译、不上传、不运行实验。

## 1. 上游状态（继承确认）

```ini
SOURCE_ATTRIBUTION=G4_NO_ACTIONABLE_LEVER
OLD_A4_DIAGNOSTIC_CLOSED=true
SOURCE_LEVER_CANDIDATE=NONE
QP_V2_SEMANTICS_UNDERDEFINED=true        # Phase B1 第一硬门裁决（D-116）
QP_V2_IMPLEMENTED=false
QP_V2_EXPERIMENT_STARTED=false
PHASE_B1_ENGINEERING_GATE=BLOCKED
```

本工作包是 D-116 登记的恢复路径第一步：由**用户以新任务书授权**补齐 K 语义预注册设计。
本包产物只是"候选算法定义 + 裁决 + 后续工程任务草案"，任何实现仍须用户单独批准（回到 Phase B1 流程）。

## 2. 任务边界（逐条登记）

1. 只允许科学定义、源码可行性审计、预注册设计；禁止实现/编译/上传/运行；
2. 必须先完成当前 Qp 调用链与语义合同的事实审计（§三清单）；
3. 设计 2–3 个候选 K 语义，每个 16 项定义 + 精确伪代码；
4. 每个候选给出 K=1 逐项还原证明；
5. 按十项标准裁决，产出五张 CSV；
6. 最终只能是 `SELECT_ONE` / `REJECT_ALL` / `NEEDS_EXPLICIT_USER_CHOICE` 之一；
7. 不得为推进路线强行选择；候选集合通常不足 2 或可触达率极低的候选必须淘汰。

## 3. 冻结边界（本包零改动声明）

```text
正式Jar = 8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9（实测复核一致）
FM3 / ShiftMode=NONE / single family / sequence-independent SUT
mixture=20/40/20/20 / PDDR=GLOBAL_ORIGINAL / CA-TA-Lite→inherited LS
双Q P5/G5 / rho=0 / Qp现有四动作 / Qp现有奖励 / 个人档案容量L=6 / CFVF / Qg / betaMax
```

本包未修改任何源码（源码审计全部只读）；未重开 PDDR修复 / local-FE pacing / teacher lambda /
source attribution扩大诊断 / 旧FC5候选膨胀路线。

## 4. 本包停止边界（最终状态）

```ini
newFEConsumed=0
newAlgorithmImplemented=false
experimentJarBuilt=false
remoteExperimentUploaded=false
QP_V2_EXPERIMENT_STARTED=false
QP_V2_250K_STARTED=false
DOE_AUTHORIZED=false
VALIDATION_AUTHORIZED=false
FORMAL_AUTHORIZED=false
formalMatrixRunning=false
formalJarChanged=false
PDDRChanged=false
CFVFChanged=false
DualQActionRewardChanged=false
CaTaChanged=false
```

## 5. 经验数据来源（只读复用，0 新 FE）

本包的可触达率量化全部来自**既有已验收遥测**，未运行任何新实验：

| 数据 | 来源 | 文件SHA-256（前16位） |
|---|---|---|
| A4 500k 真实Qp动作分布（HARD 100_5_3_1/20260901） | `V35-SOURCE-ATTRIBUTION-500K/09-v5-sa-hard-500k/02-remote-run/results/SA-HARD-V5-500k/source-lifecycle-events.csv` | `9a6c8360ebc7eaaa…`（与冷归档登记一致） |
| A4 500k 真实Qp动作分布（NORMAL 100_2_3_1/20260901） | `…/10-v5-sa-normal-500k/…/SA-NORMAL-V5-500k/source-lifecycle-events.csv` | `681745be1e44d2b3…` |
| A4 50k 档案规模/动作/教师集中度（hard 100_5_3_1/20260901） | `V35-FC5-MIDHORIZON-DIAGNOSTICS/26-final-runtime-jar-validation/A4-50k-ON-s20260901-121FBB49/telemetry-teacher-use-events.csv` | `b3adf3efcbd3224d…` |
| A4 20k 档案规模（hard 100_5_3_1/20260901） | `…/18-final-2k-20k-50k-gates/A4-20k-effective-20258-100_5_3_1-ON-final/telemetry-teacher-use-events.csv` | `59f2485632acecc3…` |
| A4 20k 档案规模/教师集中度（normal 100_2_4_1/20260901） | `…/18-final-2k-20k-50k-gates/A4-20k-effective-20258-100_2_4_1-ON-final/telemetry-teacher-use-events.csv` | `3838c3c4a6319eb1…` |
| D2/D3 50k 动作×档案规模（20-job 20_2_3_1×3seed×2arm） | `V35-A2-A3-DECOMPOSITION/04-runs/seed-*/{D2_QP_SYNCHRONOUS,D3_A3_BLOCK_FROZEN}/a2a3-personal-leader-events.csv` | 见各运行目录清单 |

注：SA 500k lifecycle 文件中 QP_ACTION/QP_TEACHER 事件为双写（543,600=2×271,800），比例统计不受影响；
QP层 relatedFingerprint 为占位常量，与 Phase A 登记的 `NOT_ATTRIBUTABLE_BY_FINGERPRINT_JOIN` 一致，
故身份集中度取自 FC5 教师遥测（teacherFingerprint 为真实四向量指纹）。
