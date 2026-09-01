# V35-GAP-LOCAL-FE-PACING-REPAIR-V1 — 20k 机制门报告

- 日期：2026-08-31
- 裁决：**`20K_MECHANISM_GATE=PASSED`（10/10）**
- 执行域：训练机 `aic-inspur-home`（`/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-repair-20260831`，Java 11.0.27，`-Xmx4g`，`nice -n 10`，无GPU，每公平组5臂并行）
- 实现域：独立实验Jar + 薄Runner（Gap Probe V2 同款架构）；冻结正式Jar `8dad8f40…bad8b9` 全程未动（本批前后两次SHA核验一致）
- 单变量：`betaMax` ∈ {0.65(C0/REF), 0.55(C1), 0.45(C2), 0.35(C3)}，`betaMin=0.25` 冻结，`beta(u)=0.25+(betaMax−0.25)·u²`
- 最终状态：`selectedRepairFamily=LOCAL_FE_PACING`、`singleKnob=betaMax`、`rootCauseProven=false`、`repairImplemented=true`、`C0BehaviorEquivalent=true`、`feReallocationDemonstrated=true`、`20kMechanismGate=PASSED`

## 1. 运行矩阵（10条 = 2实例 × 5臂 × seed 20260907 × 20000 FE）

| run | instance | profile | betaMax | actualFE | remainingFE | terminationKind | front | 机制门 |
|---|---|---|---|---:|---:|---|---:|---|
| run-REF_A4-50_2_3_1 | 50_2_3_1 | REF_A4_FROZEN | 0.65 | 15258 | 4742 | PHASE_CONSISTENT_TAIL_STOP | 97 | PASS |
| run-C0-50_2_3_1 | 50_2_3_1 | C0_BETA_MAX_065 | 0.65 | 15258 | 4742 | PHASE_CONSISTENT_TAIL_STOP | 97 | PASS |
| run-C1-50_2_3_1 | 50_2_3_1 | C1_BETA_MAX_055 | 0.55 | 20000 | 0 | EXACT_MAX_FE | 117 | PASS |
| run-C2-50_2_3_1 | 50_2_3_1 | C2_BETA_MAX_045 | 0.45 | 20000 | 0 | EXACT_MAX_FE | 125 | PASS |
| run-C3-50_2_3_1 | 50_2_3_1 | C3_BETA_MAX_035 | 0.35 | 20000 | 0 | EXACT_MAX_FE | 122 | PASS |
| run-REF_A4-100_5_3_1 | 100_5_3_1 | REF_A4_FROZEN | 0.65 | 15258 | 4742 | PHASE_CONSISTENT_TAIL_STOP | 88 | PASS |
| run-C0-100_5_3_1 | 100_5_3_1 | C0_BETA_MAX_065 | 0.65 | 15258 | 4742 | PHASE_CONSISTENT_TAIL_STOP | 88 | PASS |
| run-C1-100_5_3_1 | 100_5_3_1 | C1_BETA_MAX_055 | 0.55 | 20000 | 0 | EXACT_MAX_FE | 103 | PASS |
| run-C2-100_5_3_1 | 100_5_3_1 | C2_BETA_MAX_045 | 0.45 | 20000 | 0 | EXACT_MAX_FE | 103 | PASS |
| run-C3-100_5_3_1 | 100_5_3_1 | C3_BETA_MAX_035 | 0.35 | 20000 | 0 | EXACT_MAX_FE | 104 | PASS |

全部满足：`0 < actualFE = decoderCalls ≤ 20000`、`remainingFE < 5000`、`illegalSolutions=0`、`duplicateEvaluations=0`、front非空有限、来源观测零丢失、零Shift活动。同实例五臂 max−min(actualFE)=4742 < 5000 ✓。

## 2. C0 行为等价（门1）— **成立**

REF_A4_FROZEN（冻结正式Jar的A4路径）vs C0（实验Jar重建路径），每实例逐字段：

| 字段 | 50_2_3_1 | 100_5_3_1 |
|---|---|---|
| front.csv SHA-256 | 逐字节一致 | 逐字节一致 |
| budget-termination.properties | 逐字节一致 | 逐字节一致 |
| initial-population.sha256 | 逐字节一致 | 逐字节一致 |
| 机制摘要行为字段（p6/qg/qp/pddr事件流哈希、outerCycles、qgRounds、cfvfOffspring、qpActions、caTaLiteFE、formalLocalFE、dscr含dtur=0、caTaEventStreamHash、qg/qpTableHash等） | 全部一致 | 全部一致 |
| 仅不同字段 | 4个墙钟纳秒字段 | 4个墙钟纳秒字段 |

`C0_BEHAVIOR_EQUIVALENT=true`。跨机复现：本地20k与远端20k十组front SHA-256全部逐字节一致（`local20kFrontByteMatch=10/10`）。

## 3. betaMax 运行时读取（门2）

Runner 内部硬门断言 `configuration.getLocalFeBudget()` 的 `betaMin=0.25` 且 `betaMax == profile 值`（`failures=NONE` 传递）；五个profile的 `configurationSha256` 互异（registry），C0与REF的**运行时配置哈希相同**（设计使然，由门1实证）。

## 4. FE 回流（门3+4）— **成立（20k规模下带并列的诚实说明）**

| instance | profile | totalLocalFE | formalLocalFE | caTaLiteFE | localFeShare | outerCycles | qgRounds | cfvfOffspring | qgSel | qpActions |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 50_2_3_1 | C0 | 5158 | 4645 | 513 | 33.8% | 2 | 100 | 10000 | 400 | 8100 |
| 50_2_3_1 | C1 | 4900 | 4142 | 758 | 24.5% | 3 | 150 | 15000 | 600 | 13100 |
| 50_2_3_1 | C2 | 4900 | 4115 | 785 | 24.5% | 3 | 150 | 15000 | 600 | 13100 |
| 50_2_3_1 | C3 | 4900 | 4147 | 753 | 24.5% | 3 | 150 | 15000 | 600 | 13100 |
| 100_5_3_1 | C0 | 5158 | 4712 | 446 | 33.8% | 2 | 100 | 10000 | 400 | 8100 |
| 100_5_3_1 | C1 | 4900 | 4235 | 665 | 24.5% | 3 | 150 | 15000 | 600 | 13100 |
| 100_5_3_1 | C2 | 4900 | 4269 | 631 | 24.5% | 3 | 150 | 15000 | 600 | 13100 |
| 100_5_3_1 | C3 | 4900 | 4254 | 646 | 24.5% | 3 | 150 | 15000 | 600 | 13100 |

- `totalLocalFE`：C0 5158 → C1/C2/C3 4900（**严格下降发生在 C0→C1**，随后三档并列）。
- `formalOuterCycles`：2→3→3→3 非递减且至少一处严格增加 ✓。
- `cfvfOffspring`：10000→15000→15000→15000 非递减且严格增加 ✓；`qgSelections` 400→600、`qpActions` 8100→13100 同步增长。
- **`FE_REALLOCATION_DEMONSTRATED=true`**：释放的局部FE以更多外层Q循环+更多CFVF/Qg/Qp动作的形式回流。
- 诚实说明①：C1=C2=C3 的 `totalLocalFE` 并列是 **20k 规模伪影**——20k 只容纳 2–3 个外层循环，窗口预算是硬上限而非配额，LS 实际消耗由候选可构造性决定；C1–C3 的 `formalLocalFE`（4142/4115/4147 与 4235/4269/4254）在窗口内由接受率随机涨落决定，非单调。四档调度的真正分化需在 50k/250k（更多外层循环、u² 进入高段）检验。
- 诚实说明②：C1–C3 的 20000 为 EXACT_MAX_FE，C0/REF 为相位一致尾停（15258）——这是回流机制的直接体现（更高全局占比 → 循环边界对齐更早耗尽预算），不是预算门违规（两类终止均被 PHASE_CONSISTENT_BUDGET_TERMINATION 接受，且五臂 actualFE 极差 4742 < 5000）。

## 5. 机制真实触发（门5+6）

十次运行全部：`teacherUses>0、validityChecks>0、dominatedTeacherUses=0（dtur=0）`、`cfvfOffspring>0、cfvfRepairs=0`、`qgSelections>0`、`qpActions>0、qpTransitions>0`、`caTaLiteTest>0、caTaLiteApply>0`、`formalLocalFE>0`（O1–O9 inherited LS 事件流见各 run `ca-ta-lite-events.log`）。CFVF、Dual-Q（Qg+Qp）、CA-TA-Lite 三创新全部真实在环，无一被关闭或替代。

## 6. PDDR（门7）— `GLOBAL_ORIGINAL` 保持

所有 run 的 `pddrSelectionMode=GLOBAL_ORIGINAL`（runner 旁路文件记录 + 配置 canonical 文本双重确认）。评分公式、候选容量、选择顺序、区域配额、边界保留零改动。

## 7. PDDR 旁路观察（§16，观察范围诚实声明）

每 run 输出 `pddr-observation.properties`（汇总见 `pddr-observation.csv`）：per-source FE 记账（global offspring / CA-TA Test / CA-TA Apply / inherited LS 的 O1–O9 事件流逐事件计数与接受数）、PDDR 轮数（pddrEvents=2–3）、archive 存活插入（200/300 = outerCycles×100）、决策前沿规模与被动档案规模、三目标极值。

**局限（登记）**：池级 per-candidate 的 `enteredMergePool/selectedByPddr/rejectedByPddr/pddrSelectionRate/middleFrontCoverage` 需要池级组成遥测（FC-6A.1 的 `V35Fc6BpPddrDiagnosticAudit` 类），该类不在冻结正式Jar `8dad8f40…` 内；为其改造冻结Jar被 §4 禁止。本批按"不改冻结Jar"优先，池级归因登记为 `poolLevelAttribution=NOT_EXPORTED_BY_FROZEN_JAR`。20k 下成熟期挤压（N_<1>100）尚未进入稳态（仅 2–3 轮外层循环），池级观察的判读价值有限；50k 若获批准，建议在**不改冻结Jar**前提下评估是否追加纯观察旁路类（独立实验Jar内、`java.lang.instrument` 或显式 enable 钩子均不可用时，保持缺省并如实报告）。

## 8. 本地验证（§17 门，全部在远端执行前完成）

1. 五profile合法性与hash唯一 ✓（93/93 self-test checks）
2. betaMin=0.25 恒定 ✓
3. 正式构造器拒绝 C1–C3（单元断言）+ 冻结Jar结构上不含 repair 类 ✓
4. C0 canonical 科学参数 == 当前A4 ✓（配置哈希相等 + 行为等价）
5. 2000 FE 轻量贯通 ✓（REF/C0 双臂，快照/绑定/原子输出全链路）
6. telemetry 等价：PDDR 观察为**纯事后解析**（`POST_HOC_PARSE_ONLY`），算法内零遥测代码，结构上不可能改变行为；等价性由 REF/C0 front 逐字节一致背书 ✓
7. 随机流/候选序列/PDDR/规范front等价 ✓（门1）
8. 禁止模块静态扫描：实验源码无 `new Random()`、无 Shift/FCLS/FCRS、无 PDDR 改动、无 CA-TA/CFVF/Dual-Q 参数触碰 ✓
9. Java 8 六模块构建：实验Jar以 `javac --release 8` 独立构建，不触碰六模块构建产物 ✓
10. 新增类 major version=52（12个类逐个字节级校验）✓
11. 正式Jar SHA-256 前后一致 `8dad8f40…bad8b9` ✓
12. 证据清单逐文件反向复算：529 entries，checked=529 missing=0 mismatch=0 extra=0 ✓

## 9. 训练机资源与安全门

启动前预检：load 0.16、可用内存 119G、磁盘 248G、无遗留实验进程。执行期间仅 5 JVM×`-Xmx4g`×`nice -n 10`，总时长 ~28 秒（13:25:58–13:26:26），无GPU占用，未影响无关任务。失败attempt协议：`.partial-*` 保留 + `failure.txt` + 清单（本批无失败）。

## 10. 20k 阶段未做（按任务书 §19）

不选择最优 betaMax；不按单 seed HV/IGD 宣布性能；不构建论文 reference；不修改正式 A4；不进入 DOE。50k 为下一门，需用户批准。

## 11. 结论

```ini
selectedRepairFamily=LOCAL_FE_PACING
singleKnob=betaMax
rootCauseProven=false
repairImplemented=true
C0BehaviorEquivalent=true
feReallocationDemonstrated=true
20kMechanismGate=PASSED
50kPreregistered=true(draft_only)
50kStarted=false
250kApproved=false
DOEStarted=false
validationStarted=false
FinalCandidateApproved=false
formalMatrixRunning=false
PDDRChanged=false
CFVFChanged=false
DualQChanged=false
CaTaRemoved=false
formalJarChanged=false
```

按任务书 §10 第7步：**生成50k预登记草案后停止**，等待用户批准。
