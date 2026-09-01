# V35-PFC5-F1 运行前预检审计报告

执行时间：2026-08-29 20:1x (UTC+8)
结论：**PRELAUNCH_AUDIT = PASS**，允许进入部署阶段。

---

## 1. Phase 0 证据复核（独立反向复算）

| 指标 | 结果 |
|---|---|
| 清单条目数 | **139** |
| matched | **139** |
| missing | **0** |
| mismatch | **0** |
| sizeMismatch | **0** |
| 判定 | **CLOSED** |

工具：`tools/verify_phase0_evidence.py`（本次新建，零 FE，不改算法）
明细：`01-prelaunch-audit/phase0-evidence-reverify.tsv`

补充说明：`evidence-sha256.tsv` 共 141 行 = 1 行 `#` 注释 + 1 行表头 + 139 行数据。Phase 0 报告正文写作 137，实测为 139，以**实测 139** 为准。该清单中**不含** `ROADMAP.md` 与 `AGENTS.md`（已 grep 确认），因此本次 §13.1 的文档性修正不会破坏 139/139 闭合。

---

## 2. 冻结身份复核

全部 11 项哈希均为**实测**，无一为推测或重新生成：

| 项 | SHA-256（小写） |
|---|---|
| formalAlgorithmJar | `8dad8f40…d8b9` |
| armProfile (A4/20260901/500000) | `5b3cc542…79d1` |
| runtimeConfiguration | `8c68f2a5…44b3` |
| instance `100_5_3_1` | `2e88fa97…35cf` |
| setup file (SUT) | `4b49b780…1d90` |
| setupConfiguration | `E7E9FF7F…58E1` |
| fatigue file | `cf611bfb…f457` |
| fatigueConfiguration | `81CAD959…67A1` |
| problemConfiguration | `892c7c3f…79f4` |
| historicalSnapshot（物理） | `84d84523…3769` |
| initialPopulationHash V35 | `179a82a3…4c2d` |
| initialPopulationHash P8 | `7c6f8b42…2d3` |
| reference contract | `ecdc5589…235f` |
| PFref canonical | `4dc85dd4…83da` |

`runtimeConfigurationSha256` 的独立来源为远端 `input/profile-registry.csv`（由冻结 Jar 的 `V35ProfileRegistryPrinter` 生成），并将在部署阶段用**零 FE 复算**再次交叉验证（见 `02-remote-deployment/`）。

快照头部自证身份（`seed-20260901.fourvec`）：

```text
schema=v35-formal-initial-population-v1
instanceId=100_5_3_1
instanceSHA256=2E88FA97…35CF
SUTSHA256=E7E9FF7F…58E1
fatigueParameterSHA256=81CAD959…67A1
problemConfigurationSHA256=892c7c3f…79f4
seed=20260901
population=100
decoderMode=FM3
familyMode=DEGENERATE_SINGLE_FAMILY
setupMode=SEQUENCE_INDEPENDENT
shiftMode=NONE
initialPopulationSHA256=179a82a3…4c2d
initialPopulationP8SHA256=7c6f8b42…2d3
```

与冻结期望值逐项一致。`shiftMode=NONE`、`decoderMode=FM3`、`setupMode=SEQUENCE_INDEPENDENT`、`familyMode=DEGENERATE_SINGLE_FAMILY` 均符合 F1 合同。

---

## 3. 算法配置固定项（来自冻结 profile，不重新生成）

```ini
arm=A4
preRegisteredArmLabel=A4_BUDGET_AWARE_CATA
pddrSelectionMode=GLOBAL_ORIGINAL
decoderMode=FM3
familyMode=DEGENERATE_SINGLE_FAMILY
setupMode=SEQUENCE_INDEPENDENT
shiftMode=NONE
directionalTeacherPool=false
softFreezeRho=0.0
dualQ.mode=BLOCK_FROZEN
dualQ.warmupRatio=0.10
dualQ.blockLength=5
dualQ.gBlockLength=5
dualQ.frozenSelectionPolicy=GREEDY
localSearchOrder=CA-TA-Lite -> inherited LS
a4CausalUnit=BUDGET_AWARE_CATA_PACKAGE
qg=true  qp=true  cfvf=true  caTaLite=true  dscr=true
```

与 F1 合同要求的 `PDDR=GLOBAL_ORIGINAL`、`ShiftMode=NONE`、`dualQ=BLOCK_FROZEN`、`warmup=0.10`、`P=5`、`G=5`、`rho=0.0`、`directionalTeacherPool=false` 逐项一致（`P=5`/`G=5` 即 `dualQ.blockLength=5`、`dualQ.gBlockLength=5`）。

---

## 4. 启动链路（未重建算法）

**结论：不需要重建 Jar，也不需要新写 launcher。**

- 冻结 Jar 位于工作树**之外**的隔离副本：
  `_isolated-v35-final-doe1-freeze-20260823/java-jmetal58/jmetal-exec/target/jmetal-exec-5.8-jar-with-dependencies.jar`
  （工作树 `张博改进/java-jmetal58/jmetal-exec/target/` 下那个是 `e5969803…`，已被排除未使用）
- 复用已验证的**外置** launcher：`org.uma.jmetal.runner.lc_psode.ZhangBoV35FormalAblationArmRunner`
  - 源码：`docs/evidence/V35-STAGE2-MASTER-V2/tools/java/...`（未改动）
  - 字节码：`docs/evidence/V35-STAGE2-MASTER-V2/build/classes/...`，SHA `3f35a72a…` / `998187ad…`，**major version = 52**
  - 冻结 Jar 内 `V35FairRunner.class` / `V35FinalAblationProfile.class` / `ZhangBoV35FormalInitialPopulationFreezeRunner.class` 同为 major 52，远端 Java 11 可加载
- 该 launcher **从不调用 `problem.createSolution()`**，只通过 `ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(...)` 读取精确历史快照，并强制校验快照不消耗 FE。

### telemetry = OFF 的三条独立证据

1. **结构性**：冻结 Jar 内 `V35MidHorizonTelemetry`、`V35CheckpointFrontObserver`、`V35FullPddrLedgerObserver`、`V35TeacherConcentrationObserver`、`V35CaTaContributionObserver` 命中数**全部为 0**。
2. **调用侧**：launcher 调用的是 `V35FairRunner.run(mode, problem, initial, maxFEs, seed, true, fullMaskNoShadow, false, configuration)` 9 参数重载，**无 telemetry 形参**。
3. **输出侧**：运行后 `03-raw-run/` 中不应出现任何 telemetry/checkpoint 文件，且 `configuration.txt` 中无 telemetry 键（见运行验收 C20）。

### 与历史运行的过程同一性

历史 A4 500k 那次运行**就是由这个 launcher 执行的**（`ZhangBoV35ConfirmationA2A4ArmRunner` 只是外壳，内部 `execute()` 直接委托给 `ZhangBoV35FormalAblationArmRunner`，且 executor purpose 就是 `LAUNCHER_ACCEPTANCE`）。因此：

- F1 使用 `purpose=LAUNCHER_ACCEPTANCE`、`schema=v35-final-a0-a4-run-plan-v2`，与历史执行器的门控语义**完全一致**（`strict99` 生效）。
- 历史 `formal-gate.properties` 记录 `failures=NONE`、`status=COMPLETED`、`actualFE=500000`、`decoderCalls=500000`、`frontSize=387`，`passive-summary.properties` 记录 `observedCount=500000` —— 说明 launcher 内置的全部 gate 项在 500k 上已实证通过，无未知 gate 风险。

**预期差异（不是异常）**：F1 不产生 `confirmation-context.properties`；F1 的 `configuration.txt` 写 `purpose=LAUNCHER_ACCEPTANCE` 而非被 wrapper 改写后的 `purpose=CONFIRMATION`；F1 的 `configuration.txt` 末尾没有 `confirmationWrapperVersion` 等三行。

---

## 5. 运行环境

| 项 | 值 |
|---|---|
| host | `inspur-NP5570M5` |
| CPU | Intel Xeon Silver 4215R @3.20GHz，32 逻辑核（2 座 × 8 核 × 2 线程） |
| NUMA | node0 = 0-7,16-23；node1 = 8-15,24-31 |
| CPU affinity | **22-23**（与历史 A4 500k 同一 cpuSet，同属 node0） |
| JVM | `/usr/bin/java`，OpenJDK **11.0.27** 2025-04-15 |
| JVM 参数 | `-Xmx4g`（不附加任何 GC / 编码 / locale 参数） |
| 可用内存 | 117 Gi（总 125 Gi） |
| 可用磁盘 | 249 G |
| loadavg | 0.37 / 0.09 / 0.02 |
| 并发 Java 进程 | **0** |
| 并发实验进程 | **0** |

核心 22-23 占用核查：仅有内核线程（`cpuhp/22`、`ksoftirqd/22`、`kworker/22:*` 等）与 0.0% 的空闲守护进程（`sshd`、`gsd-*`、`gvfs*`、`colord`、`acpid` 等），**无任何计算负载**。整机最重进程为 ToDesk 1.9%（core 25）、mongod 0.9%（core 7）、mysqld 0.2%（core 10），均不在 22-23。

**他人资产登记（严禁触碰）**：远端存在 4 个他人 tmux 会话 `fc6-stage1` ~ `fc6-stage4`（2026-08-19 建立）。本任务因此**不使用 tmux**，改用 `setsid + nohup + taskset`，且只管理本任务自身的 PID；不使用 `pkill`/`killall` 等模式匹配杀进程命令。

---

## 6. 预检清单逐项结果

见 `01-prelaunch-audit/prelaunch-input-audit.tsv`，共 **33 项**：

- C01 Phase 0 证据闭合
- C02–C10 算法 Jar / profile / 运行时配置 / 实例 / SUT / 疲劳 / problem 配置哈希
- C11–C13 快照物理 SHA 与逻辑初群哈希（V35 + P8）
- C14–C15 reference contract 与 PFref canonical SHA
- C16–C18 launcher 字节码 SHA 与 major version
- C19 seed / population / MaxFEs
- C20 telemetry = OFF
- C21–C24 远端磁盘 / JVM / JVM 参数 / CPU 亲和
- C25–C27 并发进程与核心占用
- C28–C30 输出目录为空、无同 runId 结果、无同名运行进程
- C31–C33 主机身份、内存、他人 tmux 会话登记

**全部 PASS。**（C04 `runtimeConfigurationSha256` 标记为 `PASS_PENDING_CROSSCHECK`，将在部署阶段用零 FE 复算闭合。）

---

## 7. 已知工程风险与对策

| 风险 | 对策 |
|---|---|
| 预创建输出目录会触发 launcher 的 `refusing overwrite` | 只创建父目录；启动脚本首行 `[ -e "$OUT" ] && exit 1` |
| plan 的 `schema`/`purpose` 若沿用历史值会直接 `IllegalArgumentException` | F1 plan 显式写 `schema=v35-final-a0-a4-run-plan-v2`、`purpose=LAUNCHER_ACCEPTANCE` |
| 运行输出约 87 MB（非 2 MB） | 下载按 87 MB 规划，用 `rsync -aP` 断点续传；远端原件下载后不删 |
| `.partial-*` 残留 | 启动前扫描；若有残留则保留并改用 `-r2` 后缀新 runId，不复用目录 |
| 运行约 16.5 分钟（历史 `algorithmRunNanos=989444694933`） | 轮询上限 60 分钟；超时不自动 kill，先记录环境再停下报告 |
| tmux 误伤他人 `fc6-*` 会话 | 不使用 tmux |

---

## 8. 判定

```ini
PRELAUNCH_AUDIT=PASS
phase0Evidence=CLOSED
F1=BLOCKED_PRELAUNCH 未触发
下一步=部署到远端并逐文件复核SHA
```
