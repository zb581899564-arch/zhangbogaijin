# 50K_PREREGISTRATION — V35-LOCAL-FE-PACING 50k 剂量分辨与性能筛查（冻结版）

- 日期：2026-08-31
- 状态：**FROZEN**（本文件在远端执行前冻结；此后不得修改门定义、指标口径或裁决规则。执行事实只写入 `08-remote-50k/`、分析只写入 `09/10/11/`。）
- 上游：`06-20k-scope-correction/20K_GATE_SCOPE_CORRECTION.md`（20k 剂量门 `NOT_RESOLVED`）、`05-gate-decision/NEXT_50K_PREREGISTRATION_DRAFT.md`（草案，其 §4 门定义被本文件取代）、`02-local-tests/LOCAL_PREGATE_50K_REPORT.md`（本地前置门 7/7 PASS）。
- 修复族正式名称：**`LOCAL_FE_PACING`**，单旋钮 `betaMax`（`CATA_BUDGET_REPAIR` 名称已作废）。
- 目的（按任务书 §2）：**50k 首先是剂量分辨门（DOSE_RESOLUTION），其次才是性能筛查（PERFORMANCE_SCREEN）**。剂量分辨失败 ⇒ 停止本 repair family，不调参、不寻找第五个 betaMax、不启动 250k。

## 1. 冻结科学语义（与 20k 完全一致，零改动）

FM3；ShiftMode=NONE；single family；sequence-independent SUT；mixture=20/40/20/20；PDDR=GLOBAL_ORIGINAL；LS order=CA-TA-Lite → inherited LS；Dual-Q=P5/G5；rho=0；directional teacher pool=OFF；active archive=UNBOUNDED_FULL；population=100；budget protocol=PHASE_CONSISTENT_BUDGET_TERMINATION（不用 partial Q phase 补齐预算）。CFVF、Dual-Q、CA-TA-Lite 三创新全程在环。唯一实验变量：`betaMax ∈ {0.65(C0), 0.55(C1), 0.45(C2), 0.35(C3)}`，`betaMin=0.25` 固定，`beta(u)=betaMin+(betaMax−betaMin)·u²`，`u=fullEvaluationCount/MaxFEs`（窗口开启时刻），`B_L(k)=floor(β(u)/(1−β(u))·B_G)`，每循环 Q 相位 B_G=5000，窗口硬顶=min(FE_open+B_L, MaxFEs)。

## 2. 冻结工件（运行前逐文件 SHA 复核）

| 工件 | SHA-256 | 说明 |
|---|---|---|
| formal-algorithm-8DAD8F40.jar | `8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9` | 冻结正式Jar，全程只读 |
| jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar | `a0788580684cff71ecc526e0f23d6e186dcd9131aad98776c25419378dc7331c` | 实验Jar，12类 major=52（本地逐类复核） |
| 50_2_3_1 实例/设置/疲劳 | `d08d6abc…` / `f9bde51a…` / `46192fe2…` | 与 20k binding 一致（输入链未变） |
| 100_5_3_1 实例/设置/疲劳 | `2e88fa97…` / `4b49b780…` / `cf611bfb…` | 同上 |
| 快照 50_2_3_1 × seed 20260907 | `79d1de2a1217f2632e0cc45cad1502c89390d5da8ea83527fe86e72d1190c187` | 已登记（20k 使用过，零FE物化+回读验证） |
| 快照 100_5_3_1 × seed 20260907 | `57ecc78628495c864abbe1d149d7b2e936a2e1d439d16b068de03e3c64dafec1` | 同上 |
| 快照 50_2_3_1 × seed 20260914 | `5722f3d5319ea31834b0b2f241668193318b23502444b899f3a8f861466df6db` | 2026-08-31 物化（零FE、同schema、reload校验），V35 `f80de22c…` / P8 `f667bae4…` |
| 快照 100_5_3_1 × seed 20260914 | `26e0258a4f406101f622336453fe99f3f0ec8575a24d52ee0e689656679cc3e6` | 同上，V35 `a20e8294…` / P8 `257eae21…` |

四个 profile 的 canonical text 哈希在同 seed 内互异（本地 2k 验证：C0 `d7e71435…`、C1 `10cd2c82…`、C2 `ddabb7b4…`、C3 `e2c915fd…`；实例不进入 canonical text 属冻结 Runner 设计，run 身份由输出目录与注册表承载）。binding 文件按 seed 分目录（Runner 以 `bindings/<instance>.binding.properties` 固定路径读取，故每 seed 一个工作目录，见 §8）。

## 3. 运行矩阵（16 条，不得写成 24 条）

profiles C0–C3 × instances {50_2_3_1, 100_5_3_1} × seeds {20260907, 20260914}，population=100，MaxFEs=50000。**不运行 REF_A4**（C0≡A4 已由 20k 行为等价证明：front/预算/初群/机制摘要逐字段一致；本批继续绑定 C0 configuration hash 与双 Jar hash）。

每个 instance×seed 构成四臂公平组：同显式四向量 snapshot、同实例/SUT/疲劳、同 MaxFEs、同初群 V35/P8 双 hash、独立 JVM、独立 Problem、独立算法对象。

RunKey 与 registry：`run-registry-50k.csv`（16 行，RunKey=`GAPL50K-<P>-<I>-<S>`，唯一）。

## 4. 预登记偏差（ex-ante，运行前冻结；诚实边界，不做估算冒充）

**D1 — 前沿级统一FE检查点不可导出（`checkpointFrontExport=NOT_EXPORTED_BY_FROZEN_JAR`）。**
统一FE检查点处的完整三目标 front 需要运行中导出，而评估循环位于冻结正式Jar（V35FairRunner / 全部搜索引擎类均在 `8dad8f40…` 内，实验Jar仅含 12 个新增类）；按项目宪法冻结Jar不可改造。终态导出仅有：终态 front.csv、ND 过滤后的 passive-archive.csv（无 FE 序号）、以及 1k-FE 网格的标量审计曲线 cmax-audit-curves.csv（bestCmaxGlobal/bestTECGlobal/bestTWCGlobal 逐 1000-FE 桶）。因此：

- **主开发口径（统一实际FE检查点）实现为标量极值口径**：在各臂 cmax-audit-curves.csv 的 FE∈{10000,20000,30000,40000} 行比较 bestCmaxGlobal/bestTECGlobal/bestTWCGlobal。这些值来自真实评估序列的逐步最优记录，是**精确的共同FE比较**（不是事后插值、不是估算）。`F_common = 全部四臂都存在曲线行的最大预注册检查点`（从运行结果确认；预测为 40000，见 §5）。
- **次要口径（终态）**：各自终态 front（PHASE_CONSISTENT 终止的实际FE处）上的完整三目标指标（HV/IGD/Spacing/双向C/frontSize/minCmax/minTEC/minTWC），标注 `TERMINAL_PHASE_CONSISTENT_SECONDARY`。同组 actualFE 极差 <5000（§6 验收门）作为预算协变量如实随行报告。
- 前沿级 HV/IGD 的共同FE比较在本批**不创建**（不得用终态值、估算值或 best-Cmax 标量冒充）；已登记为 NOT_EXPORTED，留给 250k 批次若获准改造实验链时补齐。

**D2 — 分配上限无直接导出（`allocationAccounting=CLOSED_FORM_SCHEDULE_RECONSTRUCTION`）。**
`allocatedLocalFeHardLimit`（每窗口实际授予的硬顶）不在冻结导出内。以冻结源码公式的闭合调度模拟重建：给定 (betaMin, betaMax, MaxFEs) 确定性生成每窗口 (open, close, allocated=B_L 或截断)。该模型在 20k 全部 8 组 C0–C3 运行上**精确复现**导出的 (terminationKind, formalOuterCycles, totalLocalFE)（`04-mechanism-analysis/aggregate-gate.csv` scheduleValidation 8/8 PASS）。50k 每条运行必须重过同一验证；任何一条不一致 ⇒ 该 run 的分配指标降级为事件流重建（`ca-ta-lite-events.log` 的 fe 戳）并登记 `scheduleDeviation`，其剂量判定不得引用闭合模拟。

**D3 — 冻结 Runner 的 runId 前缀恒为 `GAPL20K-`。**
runId 前缀是冻结 Runner 的常量，无科学含义；50k 运行的身份以输出目录 `run-GAPL50K-<P>-<I>-<S>` 与注册表为准（`maxFEs=50000` 由 profile.txt / budget-termination.properties 承载）。

## 5. 闭合调度对 50k 的 ex-ante 预测（满耗假设；用于验证，不用于裁决）

| profile | cycles | 终止 | finalFE | totalLocalFE | localFeShare |
|---|---:|---|---:|---:|---:|
| C0 (0.65) | 6 | TAIL | 48269 | 18169 | 0.3764 |
| C1 (0.55) | 6 | TAIL | 45359 | 15259 | 0.3364 |
| C2 (0.45) | 7 | EXACT | 50000 | 14900 | 0.2980 |
| C3 (0.35) | 7 | TAIL | 49036 | 13936 | 0.2842 |

预测：累计分配严格有序 18169>15259>14900>13936；localFeShare 相邻降幅 4.00/3.84/1.38 pp；outerCycles 非递减（6,6,7,7）；四臂均越过 FE=40000（F_common=40000 可用）；同组 actualFE 极差预测最大 4641<5000。每窗口匹配分配（6 个公共窗口）逐窗严格有序。**满耗假设若在 50k 破缺（窗口提前闭合），以 §4-D2 的降级规则处理。**

## 6. 运行验收门（每条必须全部满足，否则该 run 记 FAILED 并保留现场）

```
0 < actualFE = decoderCalls <= 50000
remainingFE < 5000
front finite and non-empty
illegalSolutions=0
duplicateEvaluations=0
unexplainedRepairs=0        （cfvfRepairs=0、directionalPoolRequests=0、shadow*=0）
lostProvenance=0            （sourceObservationLoss 不出现）
Shift activity=0            （leftShiftNanos=rightShiftNanos=0 等）
failures=NONE
scheduleValidation          （§4-D2：闭合调度 == 导出 kind/cycles/totalLocalFE）
```

每个四臂公平组：same instance / seed / snapshot / initial V35+P8 hashes / problem provenance；`max(actualFE)−min(actualFE) < 5000`。任一失败 ⇒ 停止上传后续分析（§8 停止条件）。

## 7. 剂量分辨门（先于一切性能分析）

记录字段（每 run）：allocatedLocalFeHardLimit（§4-D2 重建）、actualLocalFE、formalLocalFE、caTaLiteFE、localFeShare、globalPhaseFE、formalOuterCycles、formalQgRounds、cfvfOffspring、qgSelections、qpActions。

- **G1 结构门**：每 profile 运行时 `betaMax(C0)>C1>C2>C3` 且逐值匹配配置（profile.txt 读回）。
- **G2 分配门**：累计分配上限方向 C0>C1>C2>C3；同时报告三视图——总分配上限、每 outer cycle 平均分配上限、按相同进度 u∈{0.1..0.9} 对齐的理论分配上限。若累计档因 exact-stop 恒等式并列（20k 已登记的结构性机制），以每窗口匹配分配与 per-u 对齐视图共同裁决；三视图全部并列才算分配门失败。
- **G3 消费门**：用 localFeShare（不是只看 totalLocalFE）。四组全部 localFeShare(C0)>localFeShare(C3)；总体中位数 C0>C1≥C2≥C3；至少两个相邻档出现 ≥1 个百分点严格下降。totalLocalFE 梯度同时报告并标注 exact-stop 恒等式 caveat。
- **G4 行为门**：≥3/4 公平组中 outerCycles 或 cfvfOffspring 随 betaMax 降低呈非递减。

**失败裁决**：若仅 C0 与其余配置分开、而 C1/C2/C3 在消费口径（localFeShare 中位数）继续完全并列 ⇒ `DOSE_RESOLUTION_FAILED`，停止：不进行参数优选、不启动 250k、不寻找第五个 betaMax。

## 8. 执行协议

远端目录 `/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-50k-20260831`，按 seed 分工作目录（binding 固定路径约束）：

```
seed-20260907/{inputs/, bindings/, jars/, snapshots/, results/, logs/}
seed-20260914/{同构}
run-all-50k.sh
```

上传后逐文件核对（实验Jar、Runner 类所在Jar、实例/SUT/疲劳、快照、binding、run registry、本预登记）；启动前只读预检（CPU/内存/磁盘/Java/遗留进程）；每公平组最多 4 JVM 并行（`-Xmx4g`、`nice -n 10`、无GPU），组间串行；scheduler 在训练机本地，SSH 只负责上传/启动/检查；`.partial-*` → manifest 复核 → atomic move；失败 attempt 保留不覆盖；不删除远端原始结果；**16 条全部完成并验收后统一分析**。

## 9. 双口径指标与配对响应

- Reference（每实例、每口径独立）：`PFref_terminal(instance) = ND(C0∪C1∪C2∪C3, 全部2 seed 终态front)`；前沿级 `PFref_common` 按 D1 不创建。流程：原始三目标精确去重 → 严格 Pareto 过滤 → 统一 ideal/nadir → HV reference=(1.1,1.1,1.1)。
- 指标输出：HV、IGD、Spacing、双向 C-metric、frontSize、minCmax、minTEC、minTWC、actualFE、wall-clock。筛选只使用 HV/IGD/Cmax/TEC/TWC；Spacing/frontSize/runtime 仅诊断。
- 配对响应（同 instance×seed 内 C0 为基准，正数=候选改善）：ΔHV、ΔIGD、ΔCmax、ΔTEC、ΔTWC。common-FE 口径的 ΔCmax/ΔTEC/ΔTWC 由标量检查点（F_common 处 bestCmax/bestTEC/bestTWC）计算；ΔHV/ΔIGD 仅终态口径（D1）。两口径分别报告。

## 10. 性能筛查门（2-seed 开发筛查，不做统计显著性结论）

- **正常实例安全门（50_2_3_1）**：候选不得出现 median ΔHV < −2% 或 median ΔIGD < −10%（终态口径）；且不得有单 seed 同时 ΔHV < −5% 且 ΔIGD < −20%。
- **困难实例改善信号（100_5_3_1）**：候选至少满足一项 median ΔHV ≥ +2% 或 median ΔIGD ≥ +10%；同时另一项不得恶化超过 HV −2% / IGD −10%。
- **三目标保护门**：不得在两个实例上同时出现 median ΔCmax/ΔTEC/ΔTWC < −2%（common-FE 标量口径）；任一目标系统性退化必须报告。
- **双口径一致门**：共同FE标量口径（Cmax/TEC/TWC）与终态口径（HV/IGD 等）必须指向同一候选方向；相反 ⇒ `BUDGET_SENSITIVITY_CONFLICT`，不晋级。

## 11. 候选筛选与裁决

先过 DOSE_RESOLUTION_GATE，再过 §10 四门。合格配置按五维严格 Pareto 过滤：`ΔHV_hard`（终态）、`ΔIGD_hard`（终态）、`ΔCmax_all`、`ΔTEC_all`、`ΔTWC_all`（common-FE 标量 @F_common）。最多保留两个候选。破平顺序：困难实例 common-FE 的 median ΔCmax → median ΔTEC → 正常实例 median ΔHV（终态）→ ΔTWC → betaMax 较接近当前值(0.65)者优先。无候选通过 ⇒ `LOCAL_FE_PACING_REPAIR_NOT_SUPPORTED_AT_50K`，停止本 repair family。

最终裁决六选一：`DOSE_RESOLUTION_FAILED` / `LOCAL_FE_PACING_REPAIR_NOT_SUPPORTED_AT_50K` / `ONE_CANDIDATE_ADVANCES_TO_250K` / `TWO_CANDIDATES_ADVANCE_TO_250K` / `BUDGET_SENSITIVITY_CONFLICT` / `EXECUTION_OR_EVIDENCE_FAILURE`。即使候选通过也只置 `250kEligible=true, 250kPreregistered=false, 250kStarted=false`，不自动启动 250k。

## 12. PDDR 观察边界

PDDR 保持 GLOBAL_ORIGINAL 零改动。记录：pddrEvents、archiveSurvival（archiveInsertions）、candidate source FE 记账、CA-TA Test/Apply counts、inherited LS counts、decisionFront、observedFullFront、三目标极值。池级归因保持 `poolLevelAttribution=NOT_EXPORTED_BY_FROZEN_JAR`（冻结Jar无池级遥测，不得以估算值或 Merge Pool 序号冒充）。本阶段不修改 PDDR、不重启 BP/Region-aware/边界配额。

## 13. 证据目录与停止条件

继续写入 `docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR/`：`07-50k-preregistration/`（本文件+registry+binding+预上传清单）、`08-remote-50k/`、`09-dose-resolution/`、`10-performance-screen/`、`11-50k-decision/`，产出物按任务书 §15 清单；数字一律由脚本从 CSV 生成。停止条件：任何 FE/来源/合法性违约、验收门失败、scheduleValidation 失败且事件流降级仍不可判读 ⇒ `EXECUTION_OR_EVIDENCE_FAILURE` 并停机；历史文件与失败结果全部保留。
