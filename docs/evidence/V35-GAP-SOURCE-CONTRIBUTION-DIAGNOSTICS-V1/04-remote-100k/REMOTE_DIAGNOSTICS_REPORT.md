# REMOTE_DIAGNOSTICS_REPORT — V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1 / 04-remote-100k

- 执行日期：2026-08-31（本地时间）
- 训练机：`aic-inspur-home`（inspur-NP5570M5，Ubuntu 20.04，kernel 5.15.0-139，x86_64）
- 远端目录：`/home/inspur/aicomp/zhangbo-v35-source-diagnostics-20260831`（新建，未覆盖任何已有内容）
- 本地回传：`docs/evidence/V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1/04-remote-100k/sync/`
- 科学执行：全部 6 条运行在训练机完成（本机未执行任何科学计算）

## 1. 时间线（2026-08-31）

| 时刻 | 事件 |
|---|---|
| 22:25 | 远端目标目录创建（`ssh mkdir -p`） |
| 22:26–22:45 | T1 上传：`scp -r` staging 全部内容（186MB，52 文件） |
| 22:45:18 | T2 预检（java/nproc/内存/磁盘/负载/遗留进程） |
| 22:45:58 | T3 启动：`nohup bash run-diagnostics.sh`（PID 46061），6 JVM 并行（-Xmx4g，nice -n 10） |
| 22:47:01–22:48:04 | 各臂相继结束（全部 exit=0，首跑即成功，无需重试） |
| 22:48:04 | `ALL_6_RUNS_DONE`（总耗时约 2 分 06 秒） |
| 22:56 | 远端打包 results/ + logs/（tar.gz 40,943,524 B，sha256=9627858a19f58a258c2fbcc32bad8aff6dd32efa65f1eb994639351f9984433f） |
| 22:56–22:59 | 回传下载（sha256 核验一致）、解包到 `sync/`、本地结构核验（200 文件） |

单臂耗时（START→END，来自 `logs/run-diagnostics.log`）：50_2_3_1×20260919=63s；50_2_3_1×20260920=73s；50_2_3_1×20260921=83s；100_5_3_1×20260920=124s；100_5_3_1×20260921=125s；100_5_3_1×20260919=126s。

## 2. 预检（T2）

| 项 | 观察值 | 门限 | 结果 |
|---|---|---|---|
| java | OpenJDK 11.0.27 | — | PASS |
| nproc | 32 | — | PASS |
| 内存可用 | 119G（total 125G） | ≥40G | PASS |
| 磁盘 / | 247G 可用（71% 已用） | — | PASS |
| load average | 0.04 / 0.04 / 0.00 | ≤16 | PASS |
| 遗留 jmetal/runner 进程 | 0（pgrep 自匹配已排除） | 0 | PASS |
| 遗留 .partial-* | 0 | 0 | PASS |

## 3. SHA 核验（T1）

- 上传核验：`upload-sha256.tsv` 相对 staging 根逐文件 sha256 对照 —— **CHECKED=51 FAILURES=0**。
- 回传核验：远端 tarball sha256 与本地下载后 sha256 一致（`9627858a…84433f`）。
- 运行产物核验：每个 run 目录自带 `evidence-sha256.tsv`（31 项），远端逐文件复验 —— **6 个 run 全部 mismatches=0**。

## 4. 验收表（T4，全部数值读自远端 run 目录真实产物；回传后本地副本一致）

6 条运行核心值（6 条除 instance/seed 外完全一致）：

| runKey | actualFE | outerCycles | ledgerRows |
|---|---|---|---|
| run-GAPLSRC-C0-50_2_3_1-20260919 | 96025 | 12 | 96025 |
| run-GAPLSRC-C0-100_5_3_1-20260919 | 96025 | 12 | 96025 |
| run-GAPLSRC-C0-50_2_3_1-20260920 | 96025 | 12 | 96025 |
| run-GAPLSRC-C0-100_5_3_1-20260920 | 96025 | 12 | 96025 |
| run-GAPLSRC-C0-50_2_3_1-20260921 | 96025 | 12 | 96025 |
| run-GAPLSRC-C0-100_5_3_1-20260921 | 96025 | 12 | 96025 |

逐项验收（每条 run 均按此 7 组判据核对，6 条结果一致）：

| 判据 | 要求 | 观察值 | 结果 |
|---|---|---|---|
| formal-gate.status | COMPLETED | COMPLETED | PASS |
| formal-gate.failures | NONE | NONE | PASS |
| formal-gate.observerExecutionErrors | 0 | 0 | PASS |
| formal-gate.telemetryLedgerErrors | 0 | 0 | PASS |
| formal-gate.telemetryLedgerRows == actualFE | 相等 | 96025 == 96025 | PASS |
| formal-gate.telemetryPddrRounds == formalOuterCycles | 相等 | 12 == 12 | PASS |
| formal-gate.checkpointRows | 3 | 3 | PASS |
| formal-gate.actualFE == decoderCalls ≤ 100000 | 相等且≤100000 | 96025 == 96025 ≤ 100000 | PASS |
| budget.phaseBoundAccepted | true | true | PASS |
| budget.remainingFE | <5000 | 3975 | PASS |
| budget.utilizationRate | >0.98 | **0.960250000000** | **FAIL** |
| status.illegalSolutions | 0 | 0 | PASS |
| status.duplicateEvaluations | 0 | 0 | PASS |
| status.cfvfRepairs | 0 | 0 | PASS |
| status.directionalPoolRequests | 0 | 0 | PASS |
| status.shadowSamples | 0 | 0 | PASS |
| status.leftShiftNanos | 0 | 0 | PASS |
| status.rightShiftNanos | 0 | 0 | PASS |
| source-ledger.csv 行数 == actualFE | 相等 | 96025 == 96025 | PASS |
| source-ledger.csv source 列无 UNSET | 0 | 0（6 个合法来源值，见 §6） | PASS |
| pddr-round-ledger.csv 每 cycle 恰 100 行 selectedByPddr=true | 12 cycle × 100 | 每 run 12 个 cycle 均=100（合计 1200 true） | PASS |
| front.csv 非空 | >0 | 数据行 219–448（见 §6） | PASS |
| profile.txt betaMax | 0.650000 | localFeBudget.betaMax=0.650000（另有顶层 betaMax=0.65） | PASS |
| profile.txt betaMin | 0.250000 | localFeBudget.betaMin=0.250000（另有顶层 betaMin=0.25） | PASS |
| profile.txt maxFEs | 100000 | 100000 | PASS |

说明：`cfvfRepairs`/`directionalPoolRequests`/`shadowSamples` 位于 `status.properties` 的 `mechanismSummary=` 复合字段内；`leftShiftNanos`/`rightShiftNanos` 位于 `mechanismSummary=` 与 `decoderTiming=` 复合字段内（均为 0）。`status.properties` 顶层另有 illegalSolutions=0、duplicateEvaluations=0、stopReason=BUDGET_OR_NORMAL_STOP。

## 5. 异常与偏差（完整细节）

### 5.1 utilizationRate 门限偏差（唯一 FAIL 项；未重跑，理由见下）

- 观察值：`budget-termination.properties: utilizationRate=0.960250000000`（判据要求 >0.98）。**6 条 run 完全相同**。
- 关联值（全部 6 条一致）：actualFE=96025、decoderCalls=96025、requestedMaxFE=100000、remainingFE=3975、qPhaseFE=5000、terminationKind=PHASE_CONSISTENT_TAIL_STOP、**phaseBoundAccepted=true、phaseBoundFailure=NONE**（引擎自身预算门接受该终止）。
- 结构性原因：`PHASE_CONSISTENT_BUDGET_TERMINATION` 在"下一全局相位单元放不进剩余预算"处停止，尾差 remainingFE 被协议结构性限定在 [0, qPhaseFE)=[0,5000)，故 utilizationRate 只能落在 (0.95, 1.0]。C0 相位表在 100k 处的边界**确定性地**落在 96025（FE 记账与种子无关；6 条种子轨迹不同而 FE 记账相同），utilizationRate=1−3975/100000=0.96025。
- 交叉证据：本地 20k 等价门运行（`02-local-tests/src-gates/runs/gate20k-*-C0-ON/budget-termination.properties`）同族停止：remainingFE=4742<5000、utilizationRate=0.7629 —— 即 C0 相位一致终止在任意已测预算下都不会达到 >0.98；该阈值疑似沿用 250k 档（~4000 尾差 → ~0.984）标定，未按 100k 重新标定。
- 预登记核查：`00-preregistration/SOURCE_DIAGNOSTICS_PREREGISTRATION.md` 未定义 utilizationRate 门限（仅冻结 `PHASE_CONSISTENT_BUDGET_TERMINATION` 语义）；>0.98 来自任务书验收清单。
- 未重跑理由：FE 记账由相位表确定性决定，重跑必然复现 96025/3975/0.96025；且 remainingFE<5000 与 phaseBoundAccepted=true 两项实质判据均 PASS。按"任何 FAIL 给完整细节"要求如实登记，交由裁决方裁定（改阈值/改标定/接受偏差）。

### 5.2 其他异常

- 无。6 条臂首跑全部 exit=0，无需重试（≤2 次重试机制未触发）；无 `.partial-*` 产物；无观察器错误；无账本错误。

## 6. 产物摘要（回传至 `sync/`）

- 结构：`sync/seed-<S>/results/run-GAPLSRC-C0-<inst>-<S>/`（6 个 run 目录，各含 31 项 `evidence-sha256.tsv` 清单、含 checkpoints/ 子目录共 32 个文件）+ `sync/logs/`（8 个文件：6 个单臂日志、run-diagnostics.log、nohup.out）。合计 200 文件。
- source-ledger.csv 来源值分布（6 run 合计 576150 数据行 = 6×96025）：GLOBAL_CFVF=360000，INTRA_FACTORY_VNS=195366，CATA_TEST=14260，CATA_APPLY=4052，INTER_FACTORY_LS=1872，INITIAL_POPULATION=600；无 UNSET。
- pddr-round-ledger.csv：每 run 12 cycle、每 cycle 恰 100 行 selectedByPddr=true（池行中 selectedByPddr=false 的未选中行亦如实记录）。
- front.csv 数据行：100_5_3_1×20260919=276，50_2_3_1×20260919=298，100_5_3_1×20260920=255，50_2_3_1×20260920=448，100_5_3_1×20260921=219，50_2_3_1×20260921=373。
- 检查点：每 run checkpoints/ 含 25000/50000/75000 三组 decision-front 与 observed-full-front + checkpoint-registry.csv（checkpointRows=3）。

## 7. .partial 清单

- 空。`find /home/inspur/aicomp/zhangbo-v35-source-diagnostics-20260831 -name ".partial-*"` = 0 项。

## 8. 机器可读块

```ini
[remote-diagnostics]
package=V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1
stage=04-remote-100k
runDate=2026-08-31
remoteHost=aic-inspur-home
remoteDir=/home/inspur/aicomp/zhangbo-v35-source-diagnostics-20260831
runsPlanned=6
runsCompleted=6
runsAccepted=0
executionVerdict=FAIL_UTILIZATION_RATE_GATE_STRUCTURAL
utilizationRateObserved=0.960250000000
utilizationRateRequired=>0.98
allOtherCriteria=PASS
uploadSha256Checked=51
uploadSha256Failures=0
runManifestChecks=6x31
runManifestMismatches=0
partialRuns=0
retryAttemptsUsed=0
observerExecutionErrors=0
telemetryLedgerErrors=0
syncBackFiles=200
```

注：`runsAccepted=0`/`executionVerdict=FAIL…` 仅反映 §5.1 的 utilizationRate 字面门限；其余全部判据（含全部科学完整性门）PASS。
