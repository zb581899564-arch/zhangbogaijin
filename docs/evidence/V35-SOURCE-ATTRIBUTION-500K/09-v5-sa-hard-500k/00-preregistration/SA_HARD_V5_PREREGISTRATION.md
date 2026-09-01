# SA-HARD V5 500k 预注册（V35-SOURCE-ATTRIBUTION-500K / 09-v5-sa-hard-500k）

- 冻结日期：2026-09-01
- 授权链：`docs/V35_SOURCE_ATTRIBUTION_500K_PHASE_A_PLAN.md` v1.0（D-112）→ Phase A0（`00-preregistration/`，PHASE_A0_PREREGISTRATION_PASSED）→ V4 SA-HARD 独立验收纠正（`07-sa-hard-500k/INDEPENDENT_ACCEPTANCE_CORRECTION.md`）→ V5 工程门（`08-observer-v5-schema-correction/05-freeze-decision/V5_FINAL_DECISION.md`，D-113）。
- 本工作包唯一动作：使用冻结 Observer V5 重跑唯一一条 SA-HARD 500k。**不重新开发观察器，不自动启动 SA-NORMAL，不运行任何其他臂。**

## 1. 唯一获准运行

```ini
campaign=V35-SOURCE-ATTRIBUTION-V5-SA-HARD-500K
runKey=SA-HARD-V5
instance=100_5_3_1
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
role=CASE_SELECTED_DIAGNOSTIC_ONLY
```

训练机：

```ini
host=aic-inspur-home
remoteDir=/home/inspur/aicomp/zhangbo-v35-source-attribution-v5-sa-hard-500k-20260901
remoteDirPolicy=MUST_NOT_EXIST（存在即停止，不得覆盖）
execution=单JVM、nice -n 10、-Xms1g -Xmx4g、classpath顺序=V5观察器Jar在前、正式Jar在后
budgetProtocol=PHASE_CONSISTENT_BUDGET_TERMINATION
```

检查点：`25000,50000,...,475000`（19个配置检查点）；Runner 另行保存 terminal 500000 快照。
正确口径：**19个配置检查点 + 1个terminal = 20个非B0快照，另有B0**（checkpoint-0/b0-decision-front）。
terminal 不得重复计为第21个窗口。

## 2. 身份绑定（全部SHA-256于2026-09-01由sha256sum现场重算，非转录）

```ini
observerJarSha256=1a73e3cf025f7cfdb47bde38a7b34e8f8b0810958f61323a5d3cbc35272c8c9e
formalAlgorithmJarSha256=8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9
formalJarChanged=false
algorithmSemanticsChanged=false
snapshotSha256=84d845233e332a6612e5dfe93c97cbbeef40c4ee05766cbfd0e9446bd3043769
instanceSha256=2e88fa97a6f84af347a4603f04c387a65c8f9891bcab8ac6b70fdec622ea35cf
setupFileSha256=4b49b780f6ee887099574f9008bebdc106e9cf5808a11b5af97a5ee4512c1d90
fatigueFileSha256=cf611bfb3690d50f1b4dc8d6d6631dd9d04546d3ca4c4020cc9017475d4bf457
initialPopulationHashV35=179a82a3825566380ab6798aa898002d31565dad9d65802e57b295c2a4294c2d
initialPopulationHashP8=7c6f8b425f2781653ce9705b82050652f063b461b24c0f93d9486e2c686ca2d3
problemConfigurationSha256=892c7c3feddd09848bf35bac1a90a529153ad77b3cb712a36f357cd214cc79f4
profileKeyFields=arm=A4;betaMax=0.65;betaMin=0.25;pddrSelectionMode=GLOBAL_ORIGINAL;decoderMode=FM3;shiftMode=NONE;familyMode=DEGENERATE_SINGLE_FAMILY;setupMode=SEQUENCE_INDEPENDENT;population=100;maxFEs=500000;seed=20260901
```

快照物理副本：`docs/evidence/V35-PFC5-PHASE0/fetched-remote/snapshots/100_5_3_1/seed-20260901.fourvec`
（全库唯一物理副本，Failure Replay 同源）；staging 副本 `01-staging/snapshots/` 与其逐字节同哈希。

## 3. 运行前硬门（2026-09-01 现场核验记录）

| # | 硬门 | 结果 |
|---|---|---|
| 1 | V5 Jar SHA 完全匹配（实现目录与gate-workdir两份副本同哈希） | PASS（1a73e3cf…72c8c9e） |
| 2 | 正式 Jar SHA 完全匹配 | PASS（8dad8f40…bad8b9） |
| 3 | snapshot 物理 SHA 匹配（两份副本同哈希） | PASS（84d84523…043769） |
| 4 | V35/P8 初群逻辑哈希匹配（快照头字段与Phase A0绑定一致） | PASS（179a82a3…/7c6f8b42…） |
| 5 | instance/setup/fatigue 原始字节 SHA 匹配 | PASS（2e88fa97…/4b49b780…/cf611bfb…） |
| 6 | profile canonical 关键字段匹配（C0_BETA_MAX_065；GLOBAL_ORIGINAL；0.65/0.25；FM3/NONE） | PASS |
| 7 | 训练机可用空间 ≥10GB | PASS（/dev/sda2 可用246G） |
| 8 | 训练机不存在同名结果目录 | PASS（DIR_NOT_EXISTS，2026-09-01核验） |
| 9 | 无相关旧PID、无运行中formal matrix | PASS（pgrep java无运行进程；zhangbo-v35-stage2-master-v2-20260823/formal/PAUSED_BY_USER.properties 在位） |

任一项失败即停止；本表全部 PASS，无“修一下继续”情形。

## 4. Reference 与指标合同（不重建，只绑定）

- HARD reference：Failure Replay Reference Contract `docs/evidence/V35-PFC5-PHASE0/04-reference-contract/`
  （contract sha `ecdc5589…1235f`；PFref 757点 sha `4dc85dd4…683da`；gold anchors：historicalA2 HV
  `0.810244195451609` / IGD `0.057804242003353316`，实现=PHASE0 tools `build_reference_contract.py`
  精确拷贝，禁止重建）。失败类复现门沿用冻结判据：
  `deltaHV < -0.05 AND deltaIGD < -0.20`（相对 historicalA2 冻结锚）。
- 确定性参照：historicalA4 front sha `f3755d83…1239bdd`（非baseline，仅确定性参照）。
  V5为纯观察，规范排序后的终态前沿应与历史A4一致；不一致即
  `FAILURE_CLASS_REPRODUCTION=FAILED`，立即停止。
- 来源归因指标：严格复用 Phase A0 冻结 `source-attribution-thresholds.json`
  （COUNTERFACTUAL_PRODUCER_SET归属、WHVGShare、ExclusiveNDShare、fallback阈值 2.0pp/10.0pp/连续2窗）
  与 `performance-divergence-thresholds.json`（t_div fallback：HV 1.0pp AND IGD 10pp，连续2 checkpoint）。
  一级source四类：GLOBAL_CFVF/CATA/INHERITED_LS/PARENT_CARRYOVER；运行后不得增加第五类。
  FIRST_ADMISSION 仅 DESCRIPTIVE_ONLY，禁止进入任何门控；共享三目标点对任何单来源反事实贡献为0。

## 5. 运行后验收合同

工程门（全部必须满足）：`processExitCode=0`、`status=COMPLETED`、`failures=NONE`、
`0 < actualFE=decoderCalls<=500000`、`remainingFE=500000-actualFE∈[0,5000)`、`utilizationRate>0.99`、
front非空且三目标有限、`illegalSolutions=0`、`duplicateEvaluations=0`、`abnormalRepairs=0`、
`sourceLoss=0`、`observerExecutionErrors=0`、UNSET source rows=0、`sourceLedgerRows=actualFE`。

Observer V5 专用门：source-ledger 含 `actualFE,nominalFE,generation,outerCycle,qRound`；
source-lifecycle-events 含 GENERATED/DESCENDANT/IMPROVING_DESCENDANT/MERGE_POOL/PDDR_SELECTED/
WORKING_POPULATION/PERSONAL_ARCHIVE/QG_TEACHER/QP_TEACHER/QP_ACTION 十类。

附加验收：(1) 独立从source-ledger前100条评价重算严格三目标B0并与导出B0逐点一致；
(2) 每个nominal FE窗口checkpoint可读、目标有限；(3) checkpoint overshoot < 一个5000 FE原子phase；
(4) 逐文件反算运行自身evidence清单；(5) 记录实际heap峰值、GC、日志大小与磁盘占用；
(6) 正式Jar运行前后SHA保持不变。

## 6. 停止边界（无论结果如何）

```ini
SA_NORMAL_STARTED=false
SA_A2_CONDITIONAL_STARTED=false
SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
DOE_AUTHORIZED=false
QP_V2_AUTHORIZED=false
CONFIG_RACE_AUTHORIZED=false
VALIDATION_AUTHORIZED=false
formalMatrixRunning=false
PDDRChanged=false
CFVFChanged=false
DualQChanged=false
CaTaChanged=false
formalJarChanged=false
```

本包只产出HARD侧窗口证据，最多形成`SOURCE_LEVER_CANDIDATE`级观察材料；hard-normal差值门、
G1/G3最终裁决与t_div配对分析必须等待SA-NORMAL，本包不得宣布G1/G3成立。
