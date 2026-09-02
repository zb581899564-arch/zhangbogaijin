# SA-NORMAL V5 500k 决策（V35-SOURCE-ATTRIBUTION-500K / 10-v5-sa-normal-500k）

**日期：** 2026-09-02
**授权链：** D-112 → Phase A0 → D-113 → D-114（SA-HARD-V5 完成）→ 用户本次明确授权唯一一条 SA-NORMAL 500k。
**本包唯一动作：** 一条 SA-NORMAL V5 500k + HARD–NORMAL 分析与 G1/G3 裁决。不重跑 SA-HARD；**不自动启动 A2**。

## 1. 裁决

```ini
SOURCE_ATTRIBUTION=G4_NO_ACTIONABLE_LEVER
OLD_A4_DIAGNOSTIC_CLOSED=true
G1_GLOBAL_CFVF=INSUFFICIENT
G3_CATA=NOT_TRIGGERED
SOURCE_LEVER_CANDIDATE=NONE
SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
SA_A2_CONDITIONAL_ELIGIBLE=false
SA_A2_CONDITIONAL_STARTED=false
```

## 2. 依据

### 运行与预算
- `processExitCode=0`、`status=COMPLETED`、`failures=NONE`。
- `actualFE=decoderCalls=500000`、`remainingFE=0`、`utilizationRate=1.0`、`EXACT_MAX_FE`。
- 单 JVM、`nice -n 10`、`-Xms1g -Xmx4g`（**未扩堆**），classpath=V5观察器Jar在前+正式Jar在后。
- 堆峰值 3,202,396,152 B（2.98 GB），< 4 GB，无 OOM。

### 身份
- observerJar=`1a73e3cf…72c8c9e`（V5 schema-v2，未重建）；formalJar=`8dad8f40…bad8b9`（未改，运行前后SHA一致）。
- snapshot=`ea19f691…3a1842`（0-FE 确定性物化，生成器同源性证明：同一生成器复现HARD快照逐字节一致）。
- `initialPopulationHash=1fdf0820…`（运行期报告与物化快照一致）。
- 实例/setup/fatigue SHA 与正式 manifest 一致。

### 验收
- 56/56 门 PASS（budget、integrity、V5五列+十类生命周期、B0独立重算5/5逐点一致、19+1检查点overshoot=0、Jar前后SHA不变、运行自身清单67项0缺0 mismatch、snapshot身份链闭合）。

### HARD–NORMAL 分析（冻结口径，不重建 reference/阈值）
- **t_div=NOT_REACHED**：HARD 相对 NORMAL 在 decision-front HV/IGD 上没有连续两 checkpoint 同时满足 lag 条件（HV progress deficit ≥1.0pp AND IGD rel-imp deficit ≥10pp）。终态 HARD HV=0.5546 vs NORMAL HV=0.6174（各自实例 PFref 归一化空间内），但逐 checkpoint 的 deficit 未形成持续覆盖发散。
- **G1=INSUFFICIENT**：GLOBAL_CFVF 的 WHVGShare deficit 在窗1–2 持续（fpw=1），ExclusiveNDShare deficit 在窗17–18 持续（fpw=17），但 t_div=NOT_REACHED → 时序前提（firstPersistentWindow ≤ t_div）不满足。survival anomaly 不竞争（mergeToPddr/pddrToWorking 差 <10pp）。
- **G3=NOT_TRIGGERED**：CATA 无任何 metric 持续 deficit；FE 占比 2.23%（NORMAL）/3.04%（HARD）< 5% 实质性门槛。
- **G4_NO_ACTIONABLE_LEVER**：G1/G3 均未触发 → 永久停止追 PDDR/pacing/teacher/source 扩大诊断。
- 描述性预算占比（CFVF 62%）继续**禁止**当作根因。

### 生命周期归属缺口（如实登记）
- MERGE_POOL/PDDR/WORKING/Qg teacher 可归属（>96% 解析）；PERSONAL_ARCHIVE/QP_TEACHER/QP_ACTION（1,321,618 行）主体指纹不在评价账本，标记 `NOT_ATTRIBUTABLE_BY_FINGERPRINT_JOIN`，未猜测/比例分摊/回填。

### attempt1 失败（0 FE，如实保留）
- 第1次启动因漏传 `bindings/100_2_3_1.binding.properties` 秒退（exit=1，IllegalArgumentException，发生在任何评价之前），0 FE 消耗、无结果目录。日志归档于 `02-remote-run/logs-attempt1/`。补齐绑定文件后第2次启动成功。

## 3. 停止边界（本包结束后保持）

```ini
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
```

Phase A 结束（G4 出口）；Phase B、Qp-v2、DOE、Validation、Final Freeze、正式矩阵均须新的明确授权。
