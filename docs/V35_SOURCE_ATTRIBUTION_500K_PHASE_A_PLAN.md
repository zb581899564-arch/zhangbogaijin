# V35 SOURCE-ATTRIBUTION-500K Phase A 冻结执行计划

版本：`1.0`  
冻结日期：`2026-08-31`  
来源：用户确认的“三人共识冻结执行方案 v1.0”  
状态：`PHASE_A_AUTHORIZED_ONLY`  
性质：`OBSERVATIONAL / READ_ONLY / DIAGNOSTIC_ONLY`

## 1. 授权边界

本计划只授权 Phase A 的顺序执行；不授权 Phase B 或任何修复、DOE、配置赛马、Validation、Final Freeze、正式比较和正式统计。

```ini
PHASE_A_AUTHORIZED_ONLY=true
DOE_AUTHORIZED=false
QP_V2_AUTHORIZED=false
CONFIG_RACE_AUTHORIZED=false
VALIDATION_AUTHORIZED=false
FORMAL_AUTHORIZED=false
```

此前 `algorithmOptimizationClosed=true` 继续有效，含义是禁止无证据继续调算法。Phase A 是一次有限的500k纵向诊断例外，不代表算法优化重新开放。Phase A最多产生 `SOURCE_LEVER_CANDIDATE`，不得产生 `ROOT_CAUSE_CONFIRMED`。

当前科学状态保持：

```ini
A2Promoted=false
A4Promoted=false
FinalCandidateApproved=false
FINAL_FROZEN=false
PDDR=GLOBAL_ORIGINAL
mixture=20,40,20,20
betaMax=0.65
PARETO_COVERAGE_AUDIT=NO_ACTIONABLE_LEVER
rootCauseCandidate=NONE
formalMatrixRunning=false
```

永久不自动重启：`REGION_AWARE`、`BP_RESERVED`、`ORDER_SWAP`、soft-freeze、gb15、Cheap-Test、A5 teacher pool、teacher-lambda、betaMax pacing、Qp cold-start tie、reward-clipping performance repair、DOE1 mixture。

## 2. 唯一科学问题

Phase A不研究“怎样把A4调好”，只回答：

> 在确定发生500k Pareto failure的A4轨迹中，相对正常100-job A4轨迹，哪种候选来源最早失去新的、非冗余的Pareto覆盖贡献？

使用500k的理由是：50k/100k未完整复现failure mechanism；250k未确认FC5旧PDDR/overflow链；`100_5_3_1/20260901/A4/500k`已确定性复现历史failure。250k只作为500k轨迹内checkpoint，不能据此把所有未来诊断默认升级为500k。

## 3. Phase A0：任何实现或运行前的0-FE预登记

### 3.1 NORMAL Control Resolver

NORMAL不能手工指定。必须从`instance-exposure-role-registry`和历史accepted run ledger自动解析，候选同时满足：

```text
jobScale=100
role=DEVELOPMENT
Current-A4历史语义下无failure veto
有accepted 500k evidence
有运行前可冻结的metric reference材料
不属于CASE_SELECTED_DIAGNOSTIC_ONLY/VALIDATION_RESERVED/FINAL_TEST_RESERVED
```

多候选时先选历史A4在HV和IGD同时positive/non-failure者；仍并列则按instance ID字典序最小。必须输出`normal-control-resolution.csv`，列出所有候选、证据和淘汰原因。无法合法解析即`DO_NOT_RUN`。

### 3.2 HARD病例

固定：

```text
instance=100_5_3_1
seed=20260901
arm=A4
role=CASE_SELECTED_DIAGNOSTIC_ONLY
```

该instance×seed永久禁止进入DOE、configuration、validation、formal comparison和final test。

### 3.3 一级来源分类

运行前冻结且运行后不得增加第五类：

```text
GLOBAL_CFVF
CATA
INHERITED_LS
PARENT_CARRYOVER
```

GLOBAL_CFVF允许二级解释字段：subSwarmRole、QgAction、Qg/QpTeacherHash、QpAction、JS/FA/MA/WA changed及各向量变更数。二级字段不得改变一级G1–G4裁决。

### 3.4 内存与观察数据最小化

Observer禁止无限保留完整Solution或JS/FA/MA/WA数组。候选级默认只保存：fingerprint、source、generation/FE、三目标、parent/teacher fingerprint、Merge/PDDR/working/PA/teacher/descendant/improving-descendant状态。仅允许有界ND sample和有界forensic reservoir。

500k前必须执行100-job/A4/20k observer ON内存preflight，输出：`heapUsedPeak`、`bytesPerEvaluatedCandidate`、`ledgerGrowthPer10kFE`、`estimated500kPeak`、`GC overhead`。硬门：

```text
estimated500kPeak < 0.60 × assignedJavaHeap
```

失败即`MEMORY_GATE_FAIL/500K_NOT_AUTHORIZED`。只能优化观察器存储，不能无限增加堆掩盖设计问题。

### 3.5 wall-clock正确性

静态审计CA-TA/Test-Apply/credit/action selection是否读取真实wall-clock。若`wallClockInfluencesSearch=true`，HARD/NORMAL必须单JVM、固定CPU affinity、相同CPU资源并隔离其它训练负载；否则可走普通并发。结论写入`observer-timing-neutrality-audit.md`。

### 3.6 Reference合同

HARD继续使用既有Failure Replay Reference Contract，PFref、ideal/nadir、HV/IGD实现、normalization及哈希均不得重建。NORMAL在运行前建立`NORMAL_SOURCE_REFERENCE_CONTRACT`，只能使用已accepted历史raw fronts；新source-attribution run不得进入NORMAL reference。

### 3.7 数值阈值

运行前冻结`source-attribution-thresholds.json`。窗口来源贡献：

```text
WHVG_s(t)=HV(ND(Fpast∪Wt))-HV(ND(Fpast∪Wt^-s))
WHVGShare_s(t)=100×WHVG_s(t)/max(HV(ND(Fpast∪Wt)),epsilon)
ExclusiveNDShare_s(t)=100×NexclusiveND_s/max(NND_all,1)
```

若无完全可比历史source telemetry，fallback为：

```text
hard-normal WHVGShare deficit >= 2.0 percentage points
OR ExclusiveNDShare deficit >= 10.0 percentage points
AND continuous for at least two 25k windows
```

若有可比历史波动，则阈值为`max(fallback, historical normal matched-window fluctuation P95)`。运行后禁止改阈值。

### 3.8 coverage divergence时间

只定义解释性`t_div`，不得称真正因果onset。每25k计算decision-front的HV progress和IGD improvement；最早同时明显落后且连续两个checkpoint保持的点为`t_div`。运行前冻结`performance-divergence-thresholds.json`；历史不足时fallback为HV窗口progress deficit≥1.0pp且IGD relative-improvement deficit≥10pp，连续两窗。

## 4. Source Observer工程门

独立构建`V35_SOURCE_ATTRIBUTION_OBSERVER.jar`，不得覆盖正式A4 Jar。固定`100_5_3_1/20260901/A4/20k`做OFF/ON等价，比较：初群、RNG/event、Qg/Qp动作、teacher identities、CFVF fingerprints、目标三元组、PDDR survivors、CA-TA trace、Q-table hashes、actualFE、working population和decision-front。

优先byte/hash equality；无法字节等价时必须说明canonical semantic equality原因。任一搜索行为系统性分叉即：

```text
BEHAVIORAL_EQUIVALENCE_FAIL
SOURCE_ATTRIBUTION_NOT_AUTHORIZED
```

20k等价门与内存门通过后设置`observerSchemaFrozen=true`、`observerJarFrozen=true`。新增字段、来源标签、生命周期、buffer或hash都视为新版本，必须重跑20k OFF/ON。

## 5. 500k执行矩阵与检查点

按顺序、不可并行越门：

1. `SA-HARD`：`100_5_3_1/20260901/A4/500k/observer ON`；终局必须仍满足历史FAIL class，否则`SOURCE_ATTRIBUTION_CASE_NOT_REPRODUCED`并停止。
2. `SA-NORMAL`：resolver选出的NORMAL/同seed/A4/500k/observer ON。
3. 只有G1成立时才允许条件A2：`100_5_3_1/20260901/A2/500k/observer ON`。

每25k nominal FE保存phase-consistent snapshot，记录nominal和actualFE、累计指标和25k窗口指标。

Phase A科学预算：A4 HARD+NORMAL=1.0M FE；条件A2最多+0.5M；另20k OFF/ON约0.04M。禁止增加第三实例、第二diagnostic seed或更多arms。

## 6. 候选生命周期和窗口输出

每个合法候选追踪：

```text
GENERATED→MERGE_POOL→PDDR_SELECTED→WORKING_POPULATION
→PERSONAL_ARCHIVE→QP/QG_TEACHER→DESCENDANT→IMPROVING_DESCENDANT
```

`PARENT_CARRYOVER`的`N_eval=0`，只进入生存/利用层。

每个25k窗口按一级source输出：N_evaluated、N_unique_objective、N_exclusive_ND、WHVG/WHVGShare及各生命周期转化率。GLOBAL_CFVF另按G1–G4、Qg/Qp action和四向量编辑类别出二级报告。

## 7. 唯一四类出口

### G1：GLOBAL/CFVF

GLOBAL_CFVF hard-normal的WHVGShare或exclusiveND deficit达到冻结阈值，连续至少两窗，首次持续异常不晚于`t_div`，且不能被更强的Merge→PDDR/working survival异常解释。输出`SOURCE_ATTRIBUTION=G1_GLOBAL_CFVF`。这只产生候选，不授权repair。

### G2：Qp↔CFVF

仅G1先成立才跑条件A2。若A4满足G1而A2不满足，输出`G2_QP_CFVF`；未来唯一候选repair family为`QP_CANDIDATE_SET_POLICY_V2`，但`IMPLEMENTATION_AUTHORIZED=false`。若A2也崩，输出G1 common-CFVF，未来第一候选轴才可为CFVF exploration intensity。

### G3：CA-TA

GLOBAL_CFVF不满足G1，且CATA deficit达到门、连续两窗、不晚于`t_div`、CA-TA仍消耗实质local FE，输出`G3_CATA`。若A0阶段没有已命名、理论明确且C0精确退化current A4的CA-TA单轴，则`AUTHORIZED_REPAIR_FAMILY=NONE`，不得事后发明邻域比例、奖励或Apply逻辑。

### G4：无可操作杠杆

G1/G3均未过门：

```ini
SOURCE_ATTRIBUTION=G4_NO_ACTIONABLE_LEVER
OLD_A4_DIAGNOSTIC_CLOSED=true
```

永久禁止继续追PDDR、pacing、teacher exposure、盲目多参数DOE或扩大source诊断。Qp-v2仅保留预注册结构计划，不自动实现、编译或运行。

最终properties只能是G1、G2、G3或G4，不得创建第五类。

## 8. Phase B以后仅为未授权脚手架

若未来另行批准Qp-v2，唯一轴为`K=1,2,3,4`，且`K=1`必须精确等价current A4；不得同时调teacher lambda、PA size、tauQ或epsilon。单轴证明先4配置×normal/hard100×2seed×250k，再Top2×2实例×2seed×500k。只有关闭原external gap至少50%，或进入最强external±15%竞争带且normal安全，才允许申请DOE。

Future DOE仅围绕已证明有效的单轴，3–4 factors、12–15 constrained D-optimal treatments；PredR²<0时禁止模型外推，改用paired empirical median、robustness和HV/IGD rank。

Validation必须使用未参与调参的数据；Champion Gate通过才`FINAL_FROZEN=true`。

## 9. Phase A交付物

```text
PREREGISTRATION.md
normal-control-resolution.csv
source-attribution-thresholds.json
performance-divergence-thresholds.json
observer-schema.md
observer-memory-preflight.csv
observer-behavioral-equivalence.md
sa-hard/
sa-normal/
sa-a2-conditional/
window-source-metrics.csv
source-lifecycle.csv
decision-front-trajectory.csv
SOURCE_ATTRIBUTION_FINAL_REPORT.md
SOURCE_ATTRIBUTION_FINAL_DECISION.properties
evidence-sha256.tsv
```

Phase A结束后必须向用户汇报并停止，不得自动进入任何修复实验。

## 10. 当前唯一执行顺序

```text
0-FE preregistration
→ independent bounded Source Observer
→ 20k OFF/ON equivalence + memory/timing gate
→ freeze observer
→ A4 HARD 500k
→ reproduce historical fail gate
→ A4 NORMAL 500k
→ G1/G3 analysis
→ conditional A2 hard 500k only if G1
→ one of G1/G2/G3/G4
→ Phase A END and mandatory user review
```
