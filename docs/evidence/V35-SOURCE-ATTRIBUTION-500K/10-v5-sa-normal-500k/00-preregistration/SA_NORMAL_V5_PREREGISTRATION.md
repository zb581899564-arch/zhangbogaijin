# SA-NORMAL V5 500k 预注册（V35-SOURCE-ATTRIBUTION-500K / 10-v5-sa-normal-500k）

- 冻结日期：2026-09-02
- 授权链：`docs/V35_SOURCE_ATTRIBUTION_500K_PHASE_A_PLAN.md` v1.0（D-112）→ Phase A0（PHASE_A0_PREREGISTRATION_PASSED）→ V5 工程门（D-113）→ V5 SA-HARD 500k 完成与验收（D-114）→ **用户本次明确授权唯一一条 SA-NORMAL 500k**。
- 本工作包唯一动作：一条 SA-NORMAL V5 500k + HARD–NORMAL 分析与 G1/G3 裁决。不重跑 SA-HARD；**不自动启动 A2 条件臂**（即使 G1 触发也只登记资格）。

## 1. 唯一获准运行

```ini
campaign=V35-SOURCE-ATTRIBUTION-V5-SA-NORMAL-500K
runKey=SA-NORMAL-V5
instance=100_2_3_1
seed=20260901
arm=A4
profile=C0_BETA_MAX_065
population=100
MaxFEs=500000
telemetry=ON
PDDR=GLOBAL_ORIGINAL
mixture=20,40,20,20
FM3=true
ShiftMode=NONE
singleFamily=true
sequenceIndependentSUT=true
observerSchema=v35-source-attribution-observer-schema-v2
role=CONTAMINATED_DEVELOPMENT_NORMAL_CONTROL
```

训练机：

```ini
host=aic-inspur-home
remoteDir=/home/inspur/aicomp/zhangbo-v35-source-attribution-v5-sa-normal-500k-20260902
remoteDirPolicy=MUST_NOT_EXIST（存在即停止，不得覆盖）
execution=单JVM、nice -n 10、-Xms1g -Xmx4g、classpath顺序=V5观察器Jar在前、正式Jar在后
budgetProtocol=PHASE_CONSISTENT_BUDGET_TERMINATION
```

资源纪律：HARD 实测堆峰值约 3.57GB（4GB 堆内）。**若 NORMAL 发生 OOM 立即停止并报告，禁止扩堆或修改观察器。**

检查点：`25000,50000,...,475000`（19 个配置检查点）；Runner 另存 terminal 500000 与 B0。
口径：**19 + 1 terminal = 20 个非 B0 快照，另有 B0**；terminal 不重复计为第 21 个窗口。

## 2. 身份绑定（2026-09-02 现场重算，非转录）

```ini
observerJarSha256=1a73e3cf025f7cfdb47bde38a7b34e8f8b0810958f61323a5d3cbc35272c8c9e
formalAlgorithmJarSha256=8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9
formalJarChanged=false
algorithmSemanticsChanged=false
instanceSha256=4fb1fad50b5c6f798580a37ff155eb9168932f1791c0262f37571d6ce91a39fa
setupFileSha256=617d92be0d95cfddbd35d6f936ea4bc2f6e99b74f4045927c770c639b800bc49
fatigueFileSha256=61c712f9cd721abc20ad81a1fa38409a9b0b2abbd8c7a464ffe60578c14c8c39
snapshotSha256=ea19f69137e088fc9ef15b6c67700f8ee5029116df7c14ff931ae4cdb53a1842
initialPopulationHashV35=1fdf0820a8a5d035d53801dcd845313f0d9d3fb0f19c134ad29316da2192155e
initialPopulationHashP8=fa5f2a5c01723f581c7c7ff7f183c1fad0c3955a61e407cd67c4192765be9548
setupConfigurationSha256=711B2A2E5088B2CC50FFFCFA0B92720A2922023BDD22065A756B9C24C6CF3B96
fatigueConfigurationSha256=01F4805BDD98902A8DC4EA02DC5A98D2FDEC5B2AE7F32C873062A238379592F4
problemConfigurationSha256=89cca2071a24160acbcec31b776236c1fa782e79943f3292701754f0d3925bb7
profileKeyFields=arm=A4;betaMax=0.65;betaMin=0.25;pddrSelectionMode=GLOBAL_ORIGINAL;decoderMode=FM3;shiftMode=NONE;familyMode=DEGENERATE_SINGLE_FAMILY;setupMode=SEQUENCE_INDEPENDENT;population=100;maxFEs=500000;seed=20260901
```

## 3. 初始种群快照（0-FE 确定性物化，非借用）

事实核验（2026-09-02）：本地与训练机均不存在 `100_2_3_1 × 20260901` 的已执行快照；正式 manifest 只覆盖
seeds 20260808..20260827；训练机确认实验（zhangbo-v35-a2-a4-confirmation-20260824）只有 6 个实例的快照，不含 100_2_3_1。

生成（全部 0 FE，正式Jar/算法未动）：

- 生成器：`org.uma.jmetal.runner.lc_psode.V35RepairSnapshotMaterializer`（jar sha
  `a0788580684cff71ecc526e0f23d6e186dcd9131aad98776c25419378dc7331c`，与 `jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar`
  证据包一致），classpath 上另置冻结正式 Jar `8dad8f40…`；命令与输出见 `01-staging/snapshot-provenance.properties`。
- **生成器同源性证明**：同一生成器对 `100_5_3_1/20260901` 的再物化与历史 HARD 快照**逐字节一致**
  （`84d84523…043769`）——即 NORMAL 快照与 HARD 快照出自同一条确定性生成规则。
- **确定性证明**：同一命令重放输出 byte-identical（物理 SHA `ea19f691…3a1842`）。
- 结构完整性（29/29 门，`05-independent-verification/verify_snapshot_provenance.py`）：
  100 粒子、JS/FA/MA/WA 各 100 行、长度=100、JS 均为 0..99 置换、资源值非负、头部绑定与
  `FORMAL_INSTANCE_MANIFEST` 逐项一致。
- 正式运行禁止 `problem.createSolution()`：运行由 `--snapshot` 注入（与 SA-HARD-V5 相同 Runner 语义）。
- 完整溯源：`01-staging/snapshot-provenance.properties` + `01-staging/snapshot-validation/`（重放与 HARD 再现副本）。

## 4. 运行前硬门（2026-09-02 现场核验记录）

| # | 硬门 | 结果 |
|---|---|---|
| 1 | V5 Jar SHA 完全匹配 | PASS（1a73e3cf…72c8c9e） |
| 2 | 正式 Jar SHA 完全匹配 | PASS（8dad8f40…bad8b9） |
| 3 | snapshot 物理 SHA（新生成，无既有副本可撞） | PASS（ea19f691…3a1842） |
| 4 | V35/P8 初群逻辑哈希（物化器报告 + 独立重放一致） | PASS（1fdf0820…/fa5f2a5c…） |
| 5 | instance/setup/fatigue 原始字节 SHA（对 FORMANIFEST） | PASS（4fb1fad5…/617d92be…/61c712f9…） |
| 6 | profile 关键字段（C0_BETA_MAX_065；GLOBAL_ORIGINAL；0.65/0.25；FM3/NONE） | PASS |
| 7 | 训练机可用空间 ≥10GB | PASS（运行前再核） |
| 8 | 训练机不存在同名结果目录 | PASS（运行前再核） |
| 9 | 无相关旧PID、无运行中formal matrix | PASS（运行前再核；Stage2 暂停标记在位） |
| 10 | NORMAL reference 材料冻结（12 accepted A4 fronts + 1979点统一front，sha 4b2c96b6…） | PASS |
| 11 | snapshot 生成 0 FE 且生成器同源性证明（HARD 快照逐字节复现） | PASS |

## 5. Reference 与指标合同（不重建，只绑定；禁回填）

- **HARD**：Failure Replay Reference Contract（contract sha `ecdc5589…1235f`；PFref 757点 `4dc85dd4…`；
  gold anchors HV `0.810244195451609` / IGD `0.057804242003353316`）——已用于 SA-HARD-V5（D-114）。
- **NORMAL**：Phase A0 冻结绑定（`00-preregistration/normal-reference-binding.properties`）：
  12 条 accepted A4 500k raw fronts（冷归档 `zhangbo-v35-stage2-master-v2-20260823.tar.gz`，整包 sha `0202356f…`）
  + 本地聚合 `docs/evidence/V35-STAGE2-PILOT-A0-A4-20260823/results/reference-front.csv`
  （1979 点，sha `4b2c96b643049a6df2ef1731c23adaa52a3eb88a4530b9cd95d7474f90882941`）。
  `normalReferenceBackfillPolicy=FORBIDDEN`：本 SA-NORMAL 运行的任何前沿/检查点不得回灌 NORMAL reference。
- t_div（`performance-divergence-thresholds.json` 冻结）：HV/IGD 由各自实例 PFref 归一化（HARD 用 F1 合同，
  NORMAL 用上述统一front）；fallback 阈值 HV progress deficit ≥1.0pp AND IGD rel-imp deficit ≥10pp，连续 2 checkpoint；
  B_0 为 i=1 差分基线。
- 窗口来源指标（`source-attribution-thresholds.json` 冻结）：COUNTERFACTUAL_PRODUCER_SET 归属、
  WHVGShare、ExclusiveNDShare、T_HV=2.0pp、T_ND=10.0pp、连续 2 窗；FIRST_ADMISSION 仅 DESCRIPTIVE_ONLY；
  共享三目标点对任何单来源反事实贡献为 0。
- 一级来源固定四类：GLOBAL_CFVF/CATA/INHERITED_LS/PARENT_CARRYOVER；运行后不得增加第五类。

## 6. 运行后验收合同

工程门（全部必须满足）：`processExitCode=0`、`status=COMPLETED`、`failures=NONE`、
`0<actualFE=decoderCalls<=500000`、`0<=remainingFE<5000`、`utilizationRate>0.99`、front 非空且三目标有限、
`illegalSolutions=0`、`duplicateEvaluations=0`、`abnormalRepairs=0`、`sourceLoss=0`、
`observerExecutionErrors=0`、`telemetryLedgerErrors=0`、UNSET source rows=0、`sourceLedgerRows=actualFE`。

V5 字段门：source-ledger 含 `actualFE,nominalFE,generation,outerCycle,qRound`；lifecycle 十类事件齐全。
独立重算 B0 与导出逐点一致；19 检查点 + terminal + B0，overshoot<5000；
PA/QP 无法通过 fingerprint 连接的事件继续标记 `NOT_ATTRIBUTABLE_BY_FINGERPRINT_JOIN`（禁止猜测/比例分摊/回填）。

## 7. HARD–NORMAL 分析与裁决（冻结规则）

对相同 25k 窗口输出：`hard-normal-window-comparison.csv`、`source-survival-comparison.csv`、
`coverage-divergence.csv`、`g1-g3-decision-matrix.csv`。

裁决仅允许：`G1_GLOBAL_CFVF=TRIGGERED/NOT_TRIGGERED/INSUFFICIENT`、`G3_CATA=…`、
`SOURCE_LEVER_CANDIDATE=GLOBAL_CFVF/CATA/NONE`。

- **G1**：GLOBAL_CFVF 的 hard–normal deficit（WHVGShare ≥2.0pp OR ExclusiveNDShare ≥10.0pp，同指标连续 ≥2 窗）
  越过冻结阈值，且 firstPersistentWindow ≤ t_div，且不能由更强的 PDDR/working survival 异常解释。
- **G3**：G1 未触发，且 CATA 满足同类持续门，且其 FE 达到实质性要求。
- 禁止把 CFVF 62% 预算占比当作根因；禁止 FIRST_ADMISSION 归因；禁止新调阈值；禁止重建 reference。
- 若 G1 触发：只登记 `SA_A2_CONDITIONAL_ELIGIBLE=true`，**不自动运行 A2**。

## 8. 停止边界（本包结束后保持）

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
