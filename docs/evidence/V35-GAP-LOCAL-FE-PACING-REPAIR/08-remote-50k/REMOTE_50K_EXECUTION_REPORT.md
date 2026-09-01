# V35-LOCAL-FE-PACING 50K 远端执行与验收报告（Agent B）

- 日期：2026-08-31
- 训练机：`aic-inspur-home`（inspur-NP5570M5，OpenJDK 11.0.27，Ubuntu 20.04，32 核 / 125 GB 内存）
- 远端项目目录：`/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-50k-20260831`（唯一写入位置）
- 本地证据目录：`docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR/08-remote-50k/`
- 预注册输入：`07-50k-preregistration/staging-50k/`（只读，未改动）
- 执行脚本：`run-all-50k.sh`（冻结预注册版，SHA256 见下）
- 调度器：单实例，4 JVM 并行（`nice -n 10`、`-Xmx4g`），公平组间串行；未并发启动第二份调度器

## 1. 时间线（远端时间 +08:00）

| 时刻 | 事件 |
|---|---|
| 15:09:55 | SSH 连通性确认（SSH_OK, inspur-NP5570M5） |
| 15:10:00 | 创建远端目录（目录此前不存在，无覆盖风险） |
| 15:10–15:14 | `scp -r` 上传 staging-50k 全部 25 个文件（93 MB） |
| 15:15 | 远端逐文件 sha256sum 核验：25/25 OK |
| 15:17:49 | T2 只读预检（数据见第 3 节） |
| 15:18:08 | `nohup bash run-all-50k.sh` 启动（RESOURCE_PRECHECK 写入日志） |
| 15:18:46 | 组1 seed=20260907 × 50_2_3_1 四臂完成（C0 15:18:39 / C1 15:18:40 / C3 15:18:44 / C2 15:18:46，全部 exit=0） |
| 15:19:40 | 组2 seed=20260907 × 100_5_3_1 四臂完成（全部 exit=0） |
| 15:20:24 | 组3 seed=20260914 × 50_2_3_1 四臂完成（全部 exit=0） |
| 15:21:19 | 组4 seed=20260914 × 100_5_3_1 四臂完成（全部 exit=0）；日志出现 `ALL_16_RUNS_DONE` |
| 15:22 | 远端 `tar czf` 打包 results+logs（5,728,736 字节，SHA256 c1a01169cd7e0d40323b42877792d638686e2c96ea7fce97e998a4e6318a0e3e） |
| 15:24 | 回传本地 `08-remote-50k/sync/`，tar 包 SHA256 本地复算一致 |

运行总时长（调度器启动 → ALL_16_RUNS_DONE）：15:18:08 → 15:21:19 = 3 分 11 秒。
轮询方式：每 60–90 秒读取 `logs/run-all-50k.log`，直至 `ALL_16_RUNS_DONE`。

## 2. T1 逐文件 SHA 核验结果

manifest：`PREUPLOAD_SHA256.tsv`（25 个文件，路径相对 staging-50k 根）。
远端核验命令：`cd /home/inspur/aicomp/zhangbo-v35-local-fe-pacing-50k-20260831 && sha256sum` 逐文件对比。
结果：**25/25 OK，0 MISMATCH**。逐文件清单：

```
OK run-all-50k.sh
OK seed-20260907/bindings/100_5_3_1.binding.properties
OK seed-20260907/bindings/50_2_3_1.binding.properties
OK seed-20260907/inputs/java-jmetal58/EADHFSP/100_5_3_1.txt
OK seed-20260907/inputs/java-jmetal58/EADHFSP/50_2_3_1.txt
OK seed-20260907/inputs/java-jmetal58/fatigue-parameters/v1/100_5_3_1.fatigue.txt
OK seed-20260907/inputs/java-jmetal58/fatigue-parameters/v1/50_2_3_1.fatigue.txt
OK seed-20260907/inputs/java-jmetal58/instance-extensions/v1/100_5_3_1.setup.txt
OK seed-20260907/inputs/java-jmetal58/instance-extensions/v1/50_2_3_1.setup.txt
OK seed-20260907/jars/formal-algorithm-8DAD8F40.jar
OK seed-20260907/jars/jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar
OK seed-20260907/snapshots/100_5_3_1-seed-20260907.fourvec
OK seed-20260907/snapshots/50_2_3_1-seed-20260907.fourvec
OK seed-20260914/bindings/100_5_3_1.binding.properties
OK seed-20260914/bindings/50_2_3_1.binding.properties
OK seed-20260914/inputs/java-jmetal58/EADHFSP/100_5_3_1.txt
OK seed-20260914/inputs/java-jmetal58/EADHFSP/50_2_3_1.txt
OK seed-20260914/inputs/java-jmetal58/fatigue-parameters/v1/100_5_3_1.fatigue.txt
OK seed-20260914/inputs/java-jmetal58/fatigue-parameters/v1/50_2_3_1.fatigue.txt
OK seed-20260914/inputs/java-jmetal58/instance-extensions/v1/100_5_3_1.setup.txt
OK seed-20260914/inputs/java-jmetal58/instance-extensions/v1/50_2_3_1.setup.txt
OK seed-20260914/jars/formal-algorithm-8DAD8F40.jar
OK seed-20260914/jars/jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar
OK seed-20260914/snapshots/100_5_3_1-seed-20260914.fourvec
OK seed-20260914/snapshots/50_2_3_1-seed-20260914.fourvec
```

## 3. T2 启动前只读预检（15:17:49 采集）

| 项目 | 实测值 | 门槛 | 判定 |
|---|---|---|---|
| java -version | OpenJDK 11.0.27（build 11.0.27+6-post-Ubuntu-0ubuntu120.04） | 有 JVM | PASS |
| nproc | 32 | — | PASS |
| free -g | 总 125 G，已用 4 G，可用 119 G | 可用 ≥ 40 G | PASS |
| df -h /home/inspur | /dev/sda2 879 G，已用 587 G，可用 248 G（71%） | 有余量 | PASS |
| uptime/load | up 13 days，load 0.05 / 0.08 / 0.09 | load ≤ 16 | PASS |
| jmetal 遗留进程 | `ps -ef \| grep jmetal` = 0（grep -v grep） | 0 | PASS |
| 任意 java 进程 | 0 | — | PASS |

调度器自带 RESOURCE_PRECHECK（15:18:08）与上述一致（内存 119 G 可用、磁盘 248 G 可用、load 0.10/0.09/0.09）。

## 4. T3 执行记录

- 16 条运行全部首次启动成功：`START ... END ... exit=0`，**无任何重跑（每臂重跑次数 = 0）**，无 `SKIP existing`。
- `.partial-*` 残留清单：远端 `find . -name ".partial-*"` 计数 **0**（无失败臂、无残留）。
- 异常清单：无（无 exit≠0、无 MISMATCH、无资源告警、无第二调度器）。

## 5. T4 逐条验收表（值取自回传 run 目录文件；详单见 run-acceptance-50k.csv）

| runKey（run-GAPL50K-…） | profile | instance | seed | actualFE | decoderCalls | remainingFE | terminationKind | outerCycles | totalLocalFE | formalLocalFE | caTaLiteFE | localFeShare | globalPhaseFE | frontSize | status/failures | acceptance |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| C0-50_2_3_1-20260907 | C0 | 50_2_3_1 | 20260907 | 48269 | 48269 | 1731 | PHASE_CONSISTENT_TAIL_STOP | 6 | 18169 | 16595 | 1574 | 0.376411361329 | 30100 | 315 | COMPLETED/NONE | PASS |
| C1-50_2_3_1-20260907 | C1 | 50_2_3_1 | 20260907 | 45359 | 45359 | 4641 | PHASE_CONSISTENT_TAIL_STOP | 6 | 15259 | 13823 | 1436 | 0.336405123570 | 30100 | 246 | COMPLETED/NONE | PASS |
| C2-50_2_3_1-20260907 | C2 | 50_2_3_1 | 20260907 | 50000 | 50000 | 0 | EXACT_MAX_FE | 7 | 14900 | 13119 | 1781 | 0.298000000000 | 35100 | 272 | COMPLETED/NONE | PASS |
| C3-50_2_3_1-20260907 | C3 | 50_2_3_1 | 20260907 | 49036 | 49036 | 964 | PHASE_CONSISTENT_TAIL_STOP | 7 | 13936 | 12165 | 1771 | 0.284199363733 | 35100 | 324 | COMPLETED/NONE | PASS |
| C0-100_5_3_1-20260907 | C0 | 100_5_3_1 | 20260907 | 48269 | 48269 | 1731 | PHASE_CONSISTENT_TAIL_STOP | 6 | 18169 | 16660 | 1509 | 0.376411361329 | 30100 | 122 | COMPLETED/NONE | PASS |
| C1-100_5_3_1-20260907 | C1 | 100_5_3_1 | 20260907 | 45359 | 45359 | 4641 | PHASE_CONSISTENT_TAIL_STOP | 6 | 15259 | 13819 | 1440 | 0.336405123570 | 30100 | 128 | COMPLETED/NONE | PASS |
| C2-100_5_3_1-20260907 | C2 | 100_5_3_1 | 20260907 | 50000 | 50000 | 0 | EXACT_MAX_FE | 7 | 14900 | 13255 | 1645 | 0.298000000000 | 35100 | 125 | COMPLETED/NONE | PASS |
| C3-100_5_3_1-20260907 | C3 | 100_5_3_1 | 20260907 | 49036 | 49036 | 964 | PHASE_CONSISTENT_TAIL_STOP | 7 | 13936 | 12263 | 1673 | 0.284199363733 | 35100 | 144 | COMPLETED/NONE | PASS |
| C0-50_2_3_1-20260914 | C0 | 50_2_3_1 | 20260914 | 48269 | 48269 | 1731 | PHASE_CONSISTENT_TAIL_STOP | 6 | 18169 | 16633 | 1536 | 0.376411361329 | 30100 | 373 | COMPLETED/NONE | PASS |
| C1-50_2_3_1-20260914 | C1 | 50_2_3_1 | 20260914 | 45359 | 45359 | 4641 | PHASE_CONSISTENT_TAIL_STOP | 6 | 15259 | 13769 | 1490 | 0.336405123570 | 30100 | 202 | COMPLETED/NONE | PASS |
| C2-50_2_3_1-20260914 | C2 | 50_2_3_1 | 20260914 | 50000 | 50000 | 0 | EXACT_MAX_FE | 7 | 14900 | 12986 | 1914 | 0.298000000000 | 35100 | 216 | COMPLETED/NONE | PASS |
| C3-50_2_3_1-20260914 | C3 | 50_2_3_1 | 20260914 | 49036 | 49036 | 964 | PHASE_CONSISTENT_TAIL_STOP | 7 | 13936 | 12010 | 1926 | 0.284199363733 | 35100 | 376 | COMPLETED/NONE | PASS |
| C0-100_5_3_1-20260914 | C0 | 100_5_3_1 | 20260914 | 48269 | 48269 | 1731 | PHASE_CONSISTENT_TAIL_STOP | 6 | 18169 | 16782 | 1387 | 0.376411361329 | 30100 | 176 | COMPLETED/NONE | PASS |
| C1-100_5_3_1-20260914 | C1 | 100_5_3_1 | 20260914 | 45359 | 45359 | 4641 | PHASE_CONSISTENT_TAIL_STOP | 6 | 15259 | 13966 | 1293 | 0.336405123570 | 30100 | 181 | COMPLETED/NONE | PASS |
| C2-100_5_3_1-20260914 | C2 | 100_5_3_1 | 20260914 | 50000 | 50000 | 0 | EXACT_MAX_FE | 7 | 14900 | 13218 | 1682 | 0.298000000000 | 35100 | 86 | COMPLETED/NONE | PASS |
| C3-100_5_3_1-20260914 | C3 | 100_5_3_1 | 20260914 | 49036 | 49036 | 964 | PHASE_CONSISTENT_TAIL_STOP | 7 | 13936 | 12241 | 1695 | 0.284199363733 | 35100 | 131 | COMPLETED/NONE | PASS |

机制零值与计数（16 条实测，取自 mechanismSummary；验收脚本已逐条断言）：illegalSolutions=0、duplicateEvaluations=0、cfvfRepairs=0、directionalPoolRequests=0、shadowSamples=0、shadowEvaluations=0、decoder leftShiftNanos=0、rightShiftNanos=0；计数项 C0/C1 = cfvfOffspring 30000、qgSelections 1200、qpActions 25100，C2/C3 = cfvfOffspring 35000、qgSelections 1400、qpActions 30100（qgSelections/qpActions 随 formalOuterCycles 线性：cycles×200 与 cycles×… 见各 run status.properties 原文）。

逐条检查项（1–6）16/16 全部满足：
1. formal-gate：status=COMPLETED、failures=NONE、actualFE=decoderCalls、0 < actualFE ≤ 50000。
2. budget-termination：phaseBoundAccepted=true、remainingFE < 5000（C2=0，其余 964–4641）、terminationKind ∈ {EXACT_MAX_FE, PHASE_CONSISTENT_TAIL_STOP}、localFeShare > 0、globalPhaseFE ∈ {30100 (C0/C1), 35100 (C2/C3)} > 0。
3. status.properties：illegalSolutions=0、duplicateEvaluations=0；mechanismSummary 中 cfvfRepairs=0、directionalPoolRequests=0、shadowSamples=0、shadowEvaluations=0；decoder 段 leftShiftNanos=0、rightShiftNanos=0。
4. front.csv 非空（数据行数 = formal-gate frontSize，16 条范围 86–376，另含 1 行表头）。
5. pddr-observation.properties：pddrSelectionMode=GLOBAL_ORIGINAL、observationMode=POST_HOC_PARSE_ONLY。
6. profile.txt：localFeBudget.betaMax 按 profile 匹配（C0=0.650000 / C1=0.550000 / C2=0.450000 / C3=0.350000）、betaMin=0.250000、maxFEs=50000。

备注：formal-gate 内部 `runId=GAPL20K-…` 为冻结 formal jar 的内部命名（沿用 20k 版式），与本次 50k runKey 目录名无冲突；actualFE 等数值均为 50k 实测。

## 6. 闭合调度验证（检查项 7：满耗模型预测 vs 实测）

预测（预注册）→ 实测，16 条全部精确一致（scheduleMatch=MATCH 16/16）：

| profile | 预测 cycles/kind/totalLocalFE | 实测（4 条同 profile 全同） | finalFE 预测 vs 实测 | scheduleMatch |
|---|---|---|---|---|
| C0 | 6 / TAIL / ≈18169 | 6 / PHASE_CONSISTENT_TAIL_STOP / 18169 | ≈48269 vs 48269 | MATCH |
| C1 | 6 / TAIL / ≈15259 | 6 / PHASE_CONSISTENT_TAIL_STOP / 15259 | ≈45359 vs 45359 | MATCH |
| C2 | 7 / EXACT / ≈14900 | 7 / EXACT_MAX_FE / 14900 | =50000 vs 50000 | MATCH |
| C3 | 7 / TAIL / ≈13936 | 7 / PHASE_CONSISTENT_TAIL_STOP / 13936 | ≈49036 vs 49036 | MATCH |

闭合调度模型 16/16 与满耗预测完全一致（cycles、terminationKind、totalLocalFE 三元组逐条精确相等）。

## 7. 公平组审计（4 组，详见 fairness-group-audit.csv）

| instance | seed | actualFE 极差 | <5000 | V35/P8 初群 hash 四臂一致 | snapshotSha256（同组同文件） | verdict |
|---|---|---|---|---|---|---|
| 50_2_3_1 | 20260907 | 4641 | true | true（7a224435… / f1549b56…） | 79d1de2a…（=PREUPLOAD） | PASS |
| 100_5_3_1 | 20260907 | 4641 | true | true（91d71b10… / d087dc9a…） | 57ecc786…（=PREUPLOAD） | PASS |
| 50_2_3_1 | 20260914 | 4641 | true | true（f80de22c… / f667bae4…） | 5722f3d5…（=PREUPLOAD） | PASS |
| 100_5_3_1 | 20260914 | 4641 | true | true（a20e8294… / 257eae21…） | 26e0258a…（=PREUPLOAD） | PASS |

说明：snapshotSha256 引用冻结预注册 manifest（PREUPLOAD_SHA256.tsv）中 `snapshots/<inst>-seed-<S>.fourvec` 的 hash；同一公平组 4 臂在 run-all-50k.sh 中加载同一 snapshot 文件，初群 provenance（V35 + P8 双 hash）由各 run 目录 `initial-population.sha256` 逐臂读取后比对，组内四臂完全一致。

## 8. 回传与产出物清单

- `sync/seed-20260907/results/run-GAPL50K-…`（8 个 run 完整目录）
- `sync/seed-20260914/results/run-GAPL50K-…`（8 个 run 完整目录）
- `sync/logs/`（run-all-50k.log + 16 条单臂日志）
- `sync/…` 由远端 tar 包（SHA256 c1a01169cd7e0d40323b42877792d638686e2c96ea7fce97e998a4e6318a0e3e）解出，包 hash 本地复算一致
- `run-acceptance-50k.csv`（16 行 + 表头）
- `fairness-group-audit.csv`（4 行 + 表头）
- `REMOTE_50K_EXECUTION_REPORT.md`（本文件）
- `evidence-sha256.tsv`（08-remote-50k 整目录 SHA256 清单）
- `acceptance_check.py`（T4 验收与 CSV 生成脚本，供复现）

## 9. 结论

16/16 条运行完成并验收通过，4/4 公平组通过，闭合调度 16/16 与满耗预测一致，无重跑、无 .partial 残留、无异常。

```ini
runsCompleted=16/0
runsAccepted=16
fairGroupsPassed=4/4
executionVerdict=COMPLETED
```
