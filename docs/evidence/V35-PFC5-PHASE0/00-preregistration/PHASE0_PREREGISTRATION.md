# V35-PFC5 Phase 0 预登记（治理冻结）

- 日期：2026-08-29
- 状态：`PREREGISTERED`
- 授权依据：`docs/ROADMAP.md` §13（PFC5-1 / PFC5-2 / PFC5-F1 仅预登记）、`AGENTS.md` §21、`docs/V35_POST_FC5_EXECUTION_MASTER_PLAN.md`
- 生成工具：总控 Agent（ZCode），人工批准执行包后按计划执行
- 消耗FE：0（全程零 FE；唯一网络动作为只读拉取历史证据文件）
- 改变算法：否
- 授权下一阶段：本阶段完成后，F1 仍需用户单独授权才能启动

## 1. 范围（唯一允许产出）

```text
PFC5-1  四项0-FE治理产物（历史失败seed登记、snapshot审计、实例角色登记、
        baseline readiness）+ Failure Replay Reference Contract
PFC5-2  既有121 runtime诊断证据的身份审计与工具封板（优先复用，禁止重跑）
PFC5-F1 仅完成预登记文档，不启动任何运行
```

明确禁止启动：F1/F2/F3 500k、Teacher Exposure Calibration、Configuration Race、
Gap Probe、Validation、4500 正式矩阵、任何未预登记实验。

## 2. 起点科学状态（冻结输入）

```ini
A2Promoted=false
A4Promoted=false
FinalCandidateApproved=false
FINAL_FROZEN=false
formalMatrix=PAUSED
PDDR=GLOBAL_ORIGINAL
CFVF=MANDATORY_FINAL_COMPONENT
DualQ=MANDATORY_FINAL_COMPONENT
CATA=MANDATORY_FINAL_COMPONENT
FC5_TRANSFER=NOT_CONFIRMED_AT_250K
PDDR_PRIORITY=DOWNGRADED_TO_OBSERVATION
```

## 3. 用户已批准的三项裁决（2026-08-29）

1. **F1 失败判据 = 否决门口径**：`deltaHV < -5% AND deltaIGD < -20%`（对配对历史 A2
   终态前沿、在本阶段冻结的 reference contract 口径下、500k 终态计算；Cmax 不作硬门；
   F1 只判终态前沿，不涉及逐 checkpoint 持续性）。该口径与
   `docs/V35_A2_A4_MULTISCALE_CONFIRMATION_PROTOCOL.md` §6 的 100-job 否决门一致，
   与历史裁决同一把尺。
2. **Failure Replay seed = 20260901**：按"失败类中最小 seed ID"规则选中。如实登记：
   该 seed 同时是本轮失败类中退化幅度最大的 seed，并且是 Step 0 工具验收 seed
   （主计划注明其"不得自动成为 replay seed"；本选择经由注册表规则产生，非自动沿用，
   该冲突与裁定过程记录于 HISTORICAL_FAILURE_CASE_REPORT.md）。
3. **授权 SSH 拉取**：从训练机 `/home/inspur/aicomp/zhangbo-v35-a2-a4-confirmation-20260824`
   只读拉取 100_5_3_1 的 10 份 500k raw front、对应 status 文件、以及 seed 20260901
   的共享初始快照。逐文件 SHA-256 必须与本地 `acceptance-run-audit.csv` /
   远端清单反向核对；连接失败或哈希不符时按 fail-closed 处理并转 BLOCKED 预案。

## 4. 关键输入文件（SHA-256 于预登记时计算）

| 文件 | SHA-256 |
|---|---|
| docs/V35_POST_FC5_EXECUTION_MASTER_PLAN.md | 1e91857f93f7685d481009dd91ab1f2dd96a4d684c1ad06aee23dd58beecb493 |
| AGENTS.md | 1826f314ed84b12d428017b1b3291774cc35a7d8b63df61402d5f559c7243512 |
| docs/ROADMAP.md | 58912a9a9aab0f5df5656feba6e79c59a7dd97e65c904e60ae8f125407e8cc1c |
| docs/evidence/V35-POST-FC5-GOVERNANCE/PLAN_INTEGRATION_REPORT.md | 5acb3d747967c88a00936ddf82e34ae258466b69dece6c066a56041e55bcbcc4 |
| docs/evidence/V35-FC5-MIDHORIZON-250K/01-root-cause-analysis/remote-results/FC5_250K_ROOT_CAUSE_REPORT.md | c1cd9365e639bed7503661ba7a68f1aacb6138605474fbe487fac4c1fb297889 |
| docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/25-v31-final-decision/V31_FINAL_DECISION.md | 7c8115ebd4237fc39774bb8929f9659fe34c39d44060cd34d1ff941c519a0f66 |
| docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/26-final-runtime-jar-validation/FINAL_121_RUNTIME_VERIFICATION.md | c021c0521ec80f698268d6f635b8041c148f0c70ea68d805da658109539700cf |
| V35-A2-A4-MULTIINSTANCE-CONFIRMATION/06-remote-analysis-import/metrics.csv | 2783adf9abe46a1172ab7319d3411acc3a23c79c8f3e02997e33954e3556476d |
| V35-A2-A4-MULTIINSTANCE-CONFIRMATION/06-remote-analysis-import/paired-deltas.csv | 04f36071cb771380a4bd86088576ce1ab7af24e29bcb74b6e95b8a373e00fc96 |
| V35-A2-A4-MULTIINSTANCE-CONFIRMATION/06-remote-analysis-import/acceptance-run-audit.csv | 5f60f25a6a363789b0ff1acbd251e7f6267bd35f81b51b568d4687b3937e465f |
| V35-A2-A4-MULTIINSTANCE-CONFIRMATION/00-preregistration/instance-seed-registry.csv | 2fdd1d3a2696fc108e361bcde7e6322dbf62ee3c469063963e72aab0f7b4f68c |
| V35-A2-A4-MULTIINSTANCE-CONFIRMATION/06-remote-analysis-import/reference-fronts/100_5_3_1.csv | a42d6f153366de4d0d422df4a025c995afdc36f8c0ca672a0179355d1c142ace |

## 5. 预注册失败类逐 seed 分类（按裁决 1 预先锁定）

> 注：下表初版由四舍五入口算值填写，其中 20260903 被误标为 PARTIAL_HV_ONLY；
> 脚本 `tools/build_failure_seed_registry.py` 按冻结门精确重算后修正为 NOT_IN_CLASS
> （ΔHV=-4.36% 未越过 -5% 门）。判定阈值与选择规则自始未变，选中 seed 不受影响。
> 修正记录于 2026-08-29，早于任何 F1 运行。

| seed | deltaHV | deltaIGD | failureClass |
|---|---|---|---|
| 20260901 | -0.3155430707 | -1.7503285142 | IN_CLASS |
| 20260902 | +0.0095710658 | +0.0291263301 | NOT_IN_CLASS |
| 20260903 | -0.0436166580 | -0.0691240769 | NOT_IN_CLASS（初版误标 PARTIAL_HV_ONLY，已修正） |
| 20260904 | -0.1296373342 | -0.7631458266 | IN_CLASS |
| 20260905 | -0.2855071648 | -1.7255116160 | IN_CLASS |

选中 seed：`20260901`（IN_CLASS 中最小 ID）。禁止在 F1 结果产出后更改 seed 或阈值。

## 6. 快照身份预判

本地 `V35-FORMAL-MANIFEST/initial-populations/100_5_3_1/` 仅含正式 seeds
20260808..20260827；确认 seeds 20260901..05 的快照本地 0 份物理文件（
`V35-FC5-100JOB-TRANSFER/03-transfer-telemetry/snapshot-recovery-matrix.csv` 判
`RECOVERY_REQUIRED_BEFORE_REPLAY`）。已知逻辑锚点：50k/100k 转移运行
（同实例同 seed）记录 `initialPopulationHash=179a82a3825566380ab6798aa898002d31565dad9d65802e57b295c2a4294c2d`。
最终分类以 S2（PFC5-1B）实测为准；禁止把"同 seed 重新生成"称为精确历史 snapshot。

## 7. 限制

- 本阶段零 FE；唯一写动作限于 `docs/evidence/V35-PFC5-PHASE0/`、`docs/ROADMAP.md`
  状态行、以及本目录内文件。
- 不修改算法源码、冻结 Jar、PDDR、CFVF、双Q、CA-TA、DOE 参数与任何历史证据目录。
- 若远端拉取失败：reference contract 判 BLOCKED，F1 预登记为不可运行草案，
  `PFC5_PHASE0=BLOCKED`，写明唯一阻断原因，等待用户单独授权补拉。
