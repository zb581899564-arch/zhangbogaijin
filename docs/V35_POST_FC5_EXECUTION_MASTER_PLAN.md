# V35 后续开发执行总计划（治理冻结副本）

> **D-110 路线覆盖通知（2026-08-30）**：本文件继续作为 Failure Replay、FC5、Teacher Exposure 和
> 旧 Post-FC5 决策的历史冻结副本，但其中“Failure Replay → Single-Knob Teacher Exposure Calibration →
> Instance Race”为当前唯一开发主线的表述，已被
> [`V35_COMPETITIVE_SUPERIORITY_EXECUTION_ROADMAP.md`](V35_COMPETITIVE_SUPERIORITY_EXECUTION_ROADMAP.md)
> 取代。当前主线为 `Gap Probe → 单一repair family杠杆审计 → 分级开发 → DOE迁移 → Validation →
> Final Freeze → 正式消融与baseline比较`。历史证据和停止结论不删除；若与D-110冲突，以D-110和新路线为准。
> 本通知不授权任何实验。

> 状态：`ACTIVE_GOVERNANCE_PLAN`  
> 纳入日期：2026-08-29  
> 原始输入 SHA-256：`CA0A021C29096DE57CC30FC2C80742CB27DB203E98F8C1B59AF1AB11A7D3708E`  
> 说明：下文保存用户批准的执行原文；其强制约束同步写入根目录 `AGENTS.md`，阶段与决策同步写入 `docs/ROADMAP.md`。历史记录不删除，但若与本计划冲突，以本计划及后续明确的新决策为准。

---
# V35 后续开发执行总计划

## 基于三人共识冻结路线 + FC5-T 250k最新证据的正式执行版

---

# 0. 当前科学状态

当前没有获准的 V35 Final。

已知状态：

```ini
A2Promoted=false
A4Promoted=false

FinalCandidateApproved=false

PDDR=GLOBAL_ORIGINAL

CFVF=MANDATORY_FINAL_COMPONENT
DualQ=MANDATORY_FINAL_COMPONENT
CATA=MANDATORY_FINAL_COMPONENT

formalMatrix=PAUSED
FINAL_FROZEN=false
```

当前完整研究路线继续固定为：

$$
\boxed{
\text{Failure Replay}
\rightarrow
\text{Single-Knob Calibration}
\rightarrow
\text{Instance Race}
}
$$

不得因为后续某一次结果不理想重新设计新的大分支路线。

---

# 1. 最新 FC5-T 250k 证据的正式定位

最新250k正/负对照已经得到：

```text
FC5_TRANSFER_NOT_CONFIRMED_AT_250K
```

关键事实：

```text
max strict Nnd = 92
Nnd > 100 rounds = 0

hard median Roverflow = 0.630
positive median Roverflow = 0.592
delta = +0.038

A4 hard-case Cmax archive-working median gap = 0

A4 hard-case relative to A2 at 250k:
median ΔCmax = +2.50%
median ΔHV   = -8.29%
median ΔIGD  = -13.03%

HV/IGD worse = 3/3 seeds
```

因此当前必须正式记录：

```ini
FC5_H1A_ND_OVERFLOW=
NOT_SUPPORTED_THROUGH_250K

FC5_H1B_CLASSIC_UTILIZATION_BREAK=
NOT_CONFIRMED

PDDR_MODIFICATION_AUTHORIZED=
false
```

该结果的含义是：

> 原 FC-5 的“ND候选大量超过100 → PDDR被迫压缩 → 四方向代表利用断裂”不能解释目前 `100_5_3_1` 的250k退化。

但不得扩大解释为：

```text
PDDR永远没有问题
```

只能说：

```text
PDDR overflow / capacity-pressure mechanism
is no longer the primary hypothesis.
```

---

# 2. 当前根因候选优先级更新

基于目前全部证据，后续机制关注优先级调整为：

```text
Tier 1:
Qp / Dual-Q teacher coordination

Tier 2:
CA-TA directional intensification

Tier 3:
CFVF × teacher interaction / full-vector amplification

Tier 4:
PDDR survivor lifecycle

Tier 5:
FM3 instance landscape
```

注意：

这只是诊断优先级，不代表已经确认：

```text
DualQ = root cause
```

也不得据此立即修改 Qp 或 CA-TA。

---

# 3. 一个非常重要的新机制假设

当前250k数据已经显示：

```text
hard case:
Cmax improves
but
HV / IGD deteriorate
```

而与此同时：

```text
direction representatives survive reasonably well
teacher utilization does not collapse
improving offspring still exist
```

因此当前更合理的候选机制不是：

> 搜索没有产生或没有利用好解。

而可能是：

> 搜索持续产生“改善”，但这些改善越来越集中到少数 trade-off 区域。

重点候选链：

```text
teacher identity concentration
        ↓
Qp/Qg反复利用少数teacher
        ↓
CFVF把teacher结构同时传播至
JS / FA / MA / WA
        ↓
CA-TA继续进行强局部开发
        ↓
局部/极值继续改善
        ↓
objective-space coverage收缩
        ↓
Cmax继续变好
但HV/IGD下降
```

该机制当前只能称：

```text
PRIMARY_MECHANISM_CANDIDATE
```

不得称：

```text
CONFIRMED_ROOT_CAUSE
```

---

# 4. 当前禁止事项

从现在开始，除非后续预注册 Gate 明确允许，否则禁止：

```text
修改 PDDR

恢复 REGION_AWARE
恢复 BP_RESERVED
加入 crowding distance
加入 NSGA-III / reference vectors
加入 fixed directional quota

修改 [20,40,20,20]

修改 rho=0

修改 P=5/G=5

修改 Pacing

关闭 CFVF

关闭 Qp / DualQ

关闭 CA-TA

改变 CA-TA -> inherited LS 顺序

重新启用 Cheap-Test

重新跑4500正式矩阵

新增无预注册250k/500k诊断分支
```

---

# 5. Phase 0：先完成治理和工具封板

## 5.1 四个0-FE冻结产物

必须完成并签字：

```text
historical-failure-seed-registry.csv

instance-exposure-role-registry.csv

baseline-fair-readiness.csv

FAILURE_REPLAY_REFERENCE_CONTRACT
```

---

## 5.2 Historical Failure Seed Registry

对象：

```text
instance = 100_5_3_1
historical A2/A4 500k confirmation seeds
```

必须包含：

```text
instance
seed

A2_HV
A4_HV
deltaHV

A2_IGD
A4_IGD
deltaIGD

A2_front_hash
A4_front_hash

initial_snapshot_exists
initial_snapshot_path
initial_snapshot_hash

historicalA2CheckpointFrontAvailable
historicalA2CheckpointFrontPath
historicalA2_raw_front_path

failureClass
caseEligible
```

failure replay case固定规则：

> 在满足历史 failure class 的 seed 中选择 seed ID 最小者。

禁止选择最差 seed。

选中：

```text
CASE_SELECTED_DIAGNOSTIC_ONLY=true
```

该seed禁止用于：

```text
Configuration Race
Validation
Formal
Final Test
```

---

# 6. Step 0：最后一次诊断工具验收

唯一批准的新工具计算：

```text
instance = 100_5_3_1
seed = 20260901

arm = A4

budget = 50k

telemetry OFF
telemetry ON
```

注意：

```text
20260901 = TOOL_ACCEPTANCE_SEED
```

不得自动成为 Failure Replay seed。

合法终止形式：

```text
nominalCheckpointFE = 50000

actualCheckpointFE =
lastCompletedAtomicBoundaryFE

checkpointKind =
PHASE_CONSISTENT_TERMINAL
```

禁止为了达到50000而执行 partial Q phase。

---

# 7. Step 0 行为等价验收

OFF / ON 至少要求：

```text
same initial snapshot

same actual FE

same phase boundaries

same RNG/event sequence

same Qg actions

same Qp actions

same Q-table hashes

same CFVF evaluated candidate sequence

same PDDR survivor sequence

same CA-TA action sequence

same working population hashes

same final/front hashes

same core-search-event hashes
```

并确认 telemetry observer 的 wall-clock overhead 不进入会影响 CA-TA credit 的真实计时域。

如果通过：

```ini
diagnosticToolingValidated=true
diagnosticToolingFrozen=true
```

此后诊断工具永久封板。

---

# 8. 完成 Phase 0 后单独预注册 F1

在以下全部完成前：

```text
4个0-FE治理产物完成

Step0工具验收完成

exact historical snapshot状态确认

Replay reference contract冻结
```

禁止启动 F1。

---

# 9. F1：500k Fresh OFF Failure Replay

默认：

```text
current frozen A4
× preregistered historical failure case
× exact historical initial snapshot
× 500k
× telemetry OFF
```

身份：

```text
Historical-state current-semantics failure replay
```

如果 exact snapshot 不存在，则降级：

```text
Current-semantics replay
using historical instance and seed
```

不得伪称 Historical-state。

---

# 10. F1必须 fresh

历史500k A4 run默认不得直接复用。

只有正式证明以下全部等价时才能复用：

```text
algorithm semantics

Jar/source identity

configuration

problem semantics

FM3

initial snapshot

random stream

FE contract

phase-consistent termination
```

否则：

$$
\boxed{F1=fresh\ 500k\ OFF}
$$

---

# 11. Failure Replay Reference Contract

F1/F2/F3及所有 checkpoint 必须共享固定：

```text
historical paired A2 raw front

empirical PFref

ideal

nadir

normalization

HV implementation

IGD implementation

objective order

failure threshold

common checkpoint alignment rule
```

禁止：

```text
50k一个PFref
100k再更新
250k再更新
500k又更新
```

同一个 replay 必须使用同一 PFref / ideal / nadir。

---

# 12. F1 Failure Reproduction Gate

F1只判断：

> 当前冻结A4是否仍进入历史 failure class。

比较：

```text
current fresh A4 @500k
vs
historical paired A2
```

重新按照 frozen replay reference contract 计算 HV/IGD。

Cmax：

```text
NOT part of failure reproduction hard gate
```

因为当前已多次发现：

```text
Cmax improves
while
HV / IGD deteriorate
```

---

# 13. F1 两种结果

## Case F1-PASS

若：

```text
current A4 again enters historical failure class
```

则：

```ini
F1=FAILURE_CLASS_REPRODUCED
```

进入F2。

---

## Case F1-FAIL

若：

```text
current A4 no longer enters historical failure class
```

则：

```ini
FC5_HISTORICAL_CASE=CLOSED
```

禁止继续声称：

> 当前算法仍患有历史 FC5 病。

此时不再运行 F2/F3。

直接进入：

```text
PROSPECTIVE_CURRENT_SEMANTICS_STABILITY
```

并按冻结默认路线考虑：

```text
Teacher Exposure Calibration
```

但只能称：

```text
HYPOTHESIS_DRIVEN_DEVELOPMENT_CALIBRATION
```

---

# 14. F2：500k Telemetry ON

只有F1 PASS才允许：

```text
same instance

same historical failure seed

same exact initial snapshot

same A4

same 500k

telemetry ON
```

必须同时通过：

```text
Outcome Equivalence
Behavioral Equivalence
```

---

# 15. Behavioral Equivalence 是硬门

必须比较：

```text
actual FE

phase boundaries

RNG/event stream

Qg action trace

Qp action trace

Q-table hashes

CFVF generation/evaluation trace

PDDR survivor trace

CA-TA action trace

working population hashes

front hashes

core event hashes
```

若：

```text
BehavioralEquivalence=false
```

立即：

```ini
FC5_MECHANISM=UNRESOLVED
```

并：

```text
禁止定义 t*
禁止用F2轨迹选择repair family
禁止使用F3形成因果链
```

之后只能进入 hypothesis-driven calibration。

---

# 16. F3触发规则

只有：

```text
F1 PASS

AND

F2 Behavioral Equivalence PASS

AND

historicalA2CheckpointFrontAvailable=false
```

三条同时成立时，自动启动：

```text
same instance
same seed
same snapshot

A2

500k

telemetry ON
```

F3不是额外探索实验。

它只是 A4 onset 分析缺少历史 A2 trajectory 时的必要 paired control。

---

# 17. Failure onset t*

正式定义：

$$
t^\*
$$

为：

> 最早一个A2/A4共同 phase-consistent checkpoint，使A4相对A2满足与F1终局完全相同的 failure criterion，并且下一个共同checkpoint仍然满足。

禁止用：

```text
第一次HV稍差
```

作为 onset。

必须：

```text
Fail(t*) = true
AND
Fail(next common checkpoint) = true
```

---

# 18. 结合250k结果，重点检查的时间区域

现有250k独立诊断已经看到：

```text
hard case:
HV / IGD worse = 3/3 seeds
```

因此F2/F3分析时应重点关注：

```text
approximately 100k -> 300k
```

但是：

> 不得因为现有250k结果，提前把250k定义成t*。

真正t*必须由Failure Replay自己的paired trajectory决定。

---

# 19. 只分析 t* 之前的机制异常

只有：

$$
t<t^\*
$$

稳定出现的异常才具有 root-cause-candidate 资格。

不得拿：

```text
HV已经崩以后才出现的异常
```

反过来当原因。

---

# 20. Root-Cause Competition

## H-Teacher

重点观察：

```text
Qp Top1 exposure

Qp Top5 exposure

Qg Top1 exposure

Qg Top5 exposure

controller-local teacher entropy

unique teacher count

improvement/exposure

teacher objective-region distribution
```

特别关注：

```text
少数teacher长期占据大量Qp/Qg exposure
```

是否先于：

```text
HV/IGD divergence
```

---

## H-CFVF

观察：

```text
normalized JS edit magnitude

normalized FA edit magnitude

normalized MA edit magnitude

normalized WA edit magnitude

pbest inheritance

gbest inheritance

FMW / MW / M / W action composition

cognitive-social inheritance balance
```

重点不是只看：

```text
edit多不多
```

而是看：

> 是否在 teacher concentration 后出现跨四向量的结构性放大。

---

## H-CATA

观察：

```text
Test FE

Apply FE

accepted candidate

macro neighborhood

directional improvement

Cmax contribution

TEC contribution

TWC contribution

Balanced contribution

objective-region concentration
```

重点判断：

> CA-TA 是否持续产生改善，但这些改善越来越集中在少数 trade-off 区域。

---

## H-PDDR

继续记录：

```text
pool -> PDDR

pool -> next

representative survival

archive-working divergence
```

但根据250k证据：

```text
PDDR priority = downgraded
```

只有F2明确出现：

```text
persistent survival anomaly before t*
```

才允许PDDR重新升级为repair candidate。

---

# 21. FC5结束后的Repair Family规则

每次只允许：

$$
\boxed{\text{一个repair family}}
$$

不得：

```text
同时改Teacher + PDDR + CA-TA
```

---

# 22. 若根因明确

如果出现：

```text
teacher anomaly
→ later t*
```

则：

```text
repairFamily=TEACHER
```

如果：

```text
CA-TA directional concentration
→ later t*
```

则：

```text
repairFamily=CATA
```

如果：

```text
CFVF amplification anomaly
→ later t*
```

则：

```text
repairFamily=CFVF
```

只有：

```text
PDDR survival anomaly
→ later t*
```

才：

```text
repairFamily=PDDR
```

---

# 23. 如果FC5仍然UNRESOLVED

停止继续扩大根因诊断。

不再追加：

```text
350k
400k
第二个hard seed
第三个机制审查
```

默认进入：

$$
\boxed{\text{Teacher Exposure Calibration}}
$$

身份固定：

```text
HYPOTHESIS_DRIVEN_DEVELOPMENT_CALIBRATION
```

理由只能写：

> 当前历史和250k证据使 teacher over-exploitation 成为最有根据的发展假设。

不能写：

> 已证明 DualQ 是根因。

---

# 24. Teacher Exposure Calibration设计边界

只允许修改：

```text
teacher identity selection
```

禁止修改：

```text
Q action semantics

Q state

Q reward

P/G block

rho

PA capacity

CFVF semantics

CA-TA

PDDR
```

执行顺序固定：

```text
DSCR

↓

Q action

↓

该action原本合法的teacher candidate set

↓

exposure-aware teacher identity selection

↓

真实CFVF behavior

↓

reward返回原Q action
```

保证：

$$
\boxed{
action
\rightarrow
actual\ behavior
\rightarrow
reward
}
$$

---

# 25. Calibration唯一旋钮 λ

只允许：

```text
C0 = current / λ = 0

C1 = weak

C2 = medium

C3 = strong
```

必须：

$$
\boxed{\lambda=0\Rightarrow exact\ current}
$$

即：

```text
same RNG consumption

same teacher

same actions

same event stream

same trajectory
```

不得只是统计上接近。

Qp和Qg分别计算：

```text
controller-local exposure
```

禁止直接比较绝对use count。

---

# 26. Configuration Instance Race

如果进入calibration，使用：

```text
4 configs

×4 DEVELOPMENT instances

×2 paired seeds

×250k
```

实例必须包括：

```text
20-job DEVELOPMENT

50-job DEVELOPMENT

normal100 DEVELOPMENT

hard100 DEVELOPMENT
```

注意：

```text
CASE_SELECTED_DIAGNOSTIC_ONLY seed
```

禁止作为 hard100 Race seed。

---

# 27. Robustness Gate

基准固定：

```text
C0=CURRENT
```

不是A2、不是QGS。

对两个100-job DEVELOPMENT实例：

2 seeds先取median。

如果：

$$
median(\Delta HV)<-10\%
$$

或者：

$$
median(\Delta IGD)<-20\%
$$

直接淘汰配置。

原则：

$$
\boxed{\text{先不崩，再谈平均性能}}
$$

---

# 28. Rank Race

通过Robustness Gate的配置：

每个 instance 先聚合2 seeds。

计算：

```text
HV rank

IGD rank
```

跨4实例：

$$
Score=
\frac{
MeanRank_{HV}+MeanRank_{IGD}
}{2}
$$

Cmax/TEC/TWC只用于：

```text
tie-break
mechanism interpretation
extreme objective reporting
```

不得进入人工加权总分。

---

# 29. Race空集规则

预注册固定：

```text
4个过门
→ Top2

2个过门
→ 两个直接进500k

1个过门
→ 该配置 vs C0

0个过门
→ repair family FAIL
```

0个时禁止：

```text
放宽门

新增C4/C5

换另一repair family继续调

回退A2
```

---

# 30. Top2 500k决赛

```text
Top2

× normal100 DEVELOPMENT

× hard100 DEVELOPMENT

×2 paired seeds

×500k
```

重新构造统一reference。

选唯一：

```text
V35-R
```

随后：

```ini
INTERNAL_DEVELOPMENT_CLOSED=true
```

从这一刻开始禁止继续修改：

```text
lambda

mixture

Pacing

Q parameters

PDDR

CFVF

CA-TA
```

除非正式 Validation FAIL 后重新进入development。

---

# 31. Gap Probe

Gap Probe允许作为并行工程分支准备，但不得干扰 Failure Replay CPU计时环境。

算法：

```text
Current A4-Pacing

HMOPSO-QGS-F

1 strongest FAIR_READY external
```

实例：

```text
1 DEVELOPMENT 50-job

100_5_3_1
```

2 paired seeds。

500k。

结果只输出：

```text
GAP_WITHIN_5

GAP_5_TO_15

GAP_GT_15
```

若稳定：

```text
>15%
```

则：

```text
RED
```

否则只允许：

```text
NOT_RED
```

禁止称GREEN。

---

# 32. Gap Probe必须独立reference

使用：

```text
GAP_PROBE_REFERENCE_CONTRACT
```

统一所有参与算法重新构造 empirical PFref。

禁止直接复用 Failure Replay reference。

历史run若想复用必须先做 semantic identity audit。

否则fresh500k。

---

# 33. Validation Mini Benchmark

V35-R产生后，首次使用：

```text
VALIDATION_RESERVED
```

至少：

```text
1 × 50-job

1 × 100-job

1 × 150/200-job
```

算法：

```text
V35-R

HMOPSO-QGS-F

NSGA-II-F

SPEA2-F

HMOPSO-QLS-F 或 MOEA/D-F
```

必须全部：

```text
fairReady=true
```

全部：

```text
500k
```

第一档：

```text
1 seed
```

仅作Go/No-Go。

不得写成论文正式显著性结果。

---

# 34. Champion Gate

目标不是每个cell全部第一。

要求：

```text
HV overall first tier

IGD overall first tier

no catastrophic large-instance collapse

clearly outperform HMOPSO-QGS-F

stable competitiveness against strongest external

Cmax / TEC / TWC multiple extreme directions competitive
```

理想目标：

```text
HV rank ≈ 1
IGD rank ≈ 1
```

但真正晋级标准是：

$$
\boxed{
\text{整体Pareto质量领先}
+
\text{稳定}
+
\text{统计可信}
}
$$

---

# 35. Validation FAIL规则

如果Validation FAIL后修改V35-R：

所有用于该轮Validation的实例立即：

```text
VALIDATION_RESERVED
→
CONTAMINATED_DEVELOPMENT
```

不得再次作为holdout。

必须更换新的 untouched validation instances。

---

# 36. Final Freeze

只有Champion Gate通过后：

```ini
FINAL_FROZEN=true
```

从这一刻开始：

$$
\boxed{\text{禁止再改算法}}
$$

后续实验只能产生论文证据。

不能根据正式结果再调参。

---

# 37. Formal Main Comparison

第一阶段建议：

```text
15 instances

×10 seeds

×all formal algorithms

×500k
```

全部算法seed数完全相同。

如果结果稳定，可扩：

```text
15 → 45 instances
```

是否从10补到20 seeds，根据：

```text
variance

confidence interval

average rank stability

effect size

statistical power
```

决定。

---

# 38. Formal Ablation

不再使用：

```text
必须 A0 < A1 < A2 < A3 < A4
```

这种单调要求。

采用：

```text
FULL V35-R

FULL - CFVF

FULL - DualQ

FULL - CA-TA

FULL - Stability Calibration

A0 / HMOPSO-QGS-F
```

即：

$$
\boxed{\text{leave-one-component-out}}
$$

回答：

> 在完整Final上下文中，去掉某项创新会怎样？

---

# 39. Anytime Metric

例如：

```text
FE@HV_target
```

只允许：

```text
FINAL_FROZEN
```

之后作为论文附加分析。

禁止用于：

```text
repair选择

Configuration Race

Robustness Gate
```

---

# 40. CPU/计算预算原则

整个development阶段严格采用：

```text
evidence gate
→ stop early when decision is available
```

Failure Replay典型最大：

```text
F1 0.5M

F2 0.5M

F3 0.5M if required
```

约：

$$
1.0\sim1.5M FE
$$

而不是再开24×500k。

Configuration Race：

$$
8M FE
$$

Top2：

$$
4M FE
$$

这些只在前一个Gate允许时发生。

---

# 41. CPU资源隔离

关键Failure Replay必须记录：

```text
CPU affinity

JVM version

heap

host

concurrent process list

wall-clock condition
```

F1/F2/F3期间禁止在同一CPU计时域大量并发baseline。

如果CA-TA真实wall-clock进入credit：

这属于：

```text
algorithm correctness
```

而非简单benchmark fairness。

---

# 42. Codex后续执行纪律

Codex不得自行：

```text
“觉得某个方向可能有用”
→ 增加一个实验
```

每一次新计算必须回答：

```text
Which preregistered gate authorizes this run?
```

如果没有明确Gate：

```text
DO_NOT_RUN
```

---

# 43. 当前立即执行顺序

从现在起严格按下面顺序：

```text
STEP A
吸收并冻结250k FC5-T否证结果
↓
标记FC5-H1 overflow链
NOT_SUPPORTED_THROUGH_250K

STEP B
完成四个0-FE治理产物
↓

STEP C
执行唯一A4 50k OFF/ON工具终验
↓
diagnosticToolingValidated=true
↓
工具永久封板

STEP D
单独预注册F1
↓

STEP E
fresh A4 500k OFF Failure Replay
↓

若F1失败复现：
    FC5 historical case CLOSED
    ↓
    hypothesis-driven teacher calibration

若F1成功复现：
    ↓
    F2 A4 500k ON
    ↓
    Behavioral Equivalence Gate

    若behavioral equivalence FAIL:
        FC5 mechanism unresolved
        ↓
        hypothesis-driven teacher calibration

    若PASS:
        ↓
        若无historical A2 checkpoint trajectory
        → F3 A2 500k ON

        ↓
        定义 t*
        ↓
        检查 t*之前最早异常

        ↓
        明确单一 repair family
        OR
        unresolved → teacher calibration

STEP F
C0/C1/C2/C3
Configuration Race @250k
↓

STEP G
Top2 @500k
↓
V35-R
↓
INTERNAL_DEVELOPMENT_CLOSED

STEP H
Validation Mini Benchmark
↓

STEP I
Champion Gate
↓

STEP J
FINAL_FROZEN
↓

STEP K
Formal Main + Formal Ablation
```

---

# 44. 最终总原则

后续研发必须遵守：

$$
\boxed{
\text{先复现真实失败}
\rightarrow
\text{再找失败前的异常}
\rightarrow
\text{只动一个旋钮}
\rightarrow
\text{多实例筛配置}
\rightarrow
\text{500k决赛}
\rightarrow
\text{未污染实例验证}
}
$$

而不是：

```text
看到一张图
→ 改算法
→ 再跑
→ 再改
```

当前250k结果的最大意义是：

$$
\boxed{
\text{不再继续浪费算力追逐 ND overflow / PDDR capacity hypothesis}
}
$$

后续最值得关注的是：

$$
\boxed{
\text{teacher coordination}
\rightarrow
\text{CFVF amplification}
\rightarrow
\text{CA-TA intensification}
}
$$

但在Failure Replay给出证据前，不得把这条链写成已经确认的因果根因。

最终研究方向始终保持：

$$
\boxed{
FM3
+
CFVF
+
Q^p/Q^g\text{双Q}
+
CA\text{-}TA\text{-Lite}
}
$$

目标不是“把所有机制研究到完全没有疑问”，而是：

$$
\boxed{
\text{尽快形成一个稳定、不崩、能明显超过QGS、
并对强external baseline具有第一梯队竞争力的V35-R}
}
$$

一旦达到Champion Gate，应立即 Final Freeze，停止继续追求开发集上的小幅提升。

