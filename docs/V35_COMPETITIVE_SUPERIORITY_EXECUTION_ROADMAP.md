# V35 竞争优势、DOE 迁移、消融与正式实验执行路线

版本：`1.0`  
决策日期：`2026-08-30`  
状态：`ACTIVE_GOVERNANCE_PLAN / NO_RUN_AUTHORIZED`  
上位事实源：[`ROADMAP.md`](ROADMAP.md) 的 D-110  
历史计划：[`V35_POST_FC5_EXECUTION_MASTER_PLAN.md`](V35_POST_FC5_EXECUTION_MASTER_PLAN.md)

## 1. 目标与论文口径

本路线取代“继续把 FC5 根因追到底”作为当前开发主线。项目目标调整为：在公平问题接口、共同评价预算和未污染验证集上，形成一个保留 `CFVF + Dual-Q + CA-TA-Lite` 的 `V35-R`，并争取在正式统计中总体优于 HMOPSO-QGS-F、李明哲论文相关算法和经典多目标基线。

“超过全部 baseline”不解释为每个实例、每个 seed 都取胜，而按以下正式证据定义：

```text
HV / IGD(+) 的实例级平均秩总体第一；
相对各主要 baseline 的改善方向总体一致；
多数成对比较经 Holm 校正后显著；
paired effect size 为正；
20/50/100-job 等规模段不存在系统性 collapse；
三目标极值无不可接受的系统退化。
```

开发阶段可将 `win rate >= 70%` 作为目标，但不得把它变成反复调参直到越线的硬门。正式阶段只如实报告 win/tie/loss、平均秩、效应量和显著性。

## 2. 当前冻结状态

```ini
A2Promoted=false
A4Promoted=false
FinalCandidateApproved=false
FINAL_FROZEN=false
formalMatrixRunning=false
formalMatrixPaused=true

PDDR=GLOBAL_ORIGINAL
CFVF=MANDATORY_FINAL_COMPONENT
DualQ=MANDATORY_FINAL_COMPONENT
CATA=MANDATORY_FINAL_COMPONENT
mixture=20/40/20/20
localSearchOrder=CA-TA-Lite -> inherited LS
ShiftMode=NONE
```

DOE1 已完成 15-treatment development 和 held-out confirmation；没有新 treatment 达到预注册的 Cmax 改善门，因此当前配比继续冻结为 `20/40/20/20`。PDDR、FC5、Teacher Exposure、ORDER_SWAP、BP_RESERVED 和 REGION_AWARE 的历史结论保持不变，不因本路线重新打开。

## 3. 数据角色隔离

所有实例与 seed 必须在运行前进入且只能进入一种角色：

| 角色 | 用途 | 是否允许据其调算法 |
|---|---|---|
| `DEVELOPMENT` | Gap Probe、杠杆审计、C0--C3筛选、DOE迁移 | 是 |
| `VALIDATION_RESERVED` | Final Freeze 前的 Go/No-Go | 仅用于一次晋级裁决；失败后若改算法，该批立即污染 |
| `FINAL_TEST_RESERVED` | Final Freeze 后首次打开的正式Stage 1 | 否 |
| `FORMAL_MAIN` | 正式多实例统计 | 否 |
| `CONTAMINATED_DEVELOPMENT` | 曾被观察并用于修改算法的验证数据 | 只能回到开发用途 |

Validation 不是 Final Test。任何 Validation 失败后发生的算法修改，必须使用新的未污染 Validation 集；旧集改标为 `CONTAMINATED_DEVELOPMENT`。

## 4. 阶段 A：Gap Probe 竞争力摸底

### 4.1 参与算法

为避免在 SPEA2-F 与 NSGA-II-F 之间事后挑选对手，Gap Probe 固定包含：

```text
Current A4-Pacing
HMOPSO-QGS-F
SPEA2-F
NSGA-II-F
```

### 4.2 规模

```text
1个预登记的 DEVELOPMENT 50-job
100_5_3_1 hard case
2个预登记的新paired seeds
population=100
MaxFEs=500000
4 algorithms × 2 instances × 2 seeds = 16 runs
```

先完成 0-FE 预登记和同一 instance×seed 下四算法的短程工程贯通。未经用户对 16 条 500k 的单独批准，不得上传或启动。

### 4.3 指标与裁决

Gap Probe 使用独立的 `GAP_PROBE_REFERENCE_CONTRACT`。每个实例在全部8条运行结束后，合并四算法、两seed的原始前沿，精确去重、严格 Pareto 过滤，并冻结统一 normalization 和 HV reference。

只允许输出：

```text
GAP_WITHIN_5
GAP_5_TO_15
GAP_GT_15
```

稳定差距大于15%记为 `RED`；其他结果只能记为 `NOT_RED`，不能称为 GREEN、Final通过或论文优越性。

## 5. 阶段 B：0-FE 杠杆审计，只选一个 repair family

Gap Probe 后不得把 Dual-Q、CA-TA预算和二者结算三个不同修法一起赛马。先只读审计候选作用点：

1. `Dual-Q -> CFVF` 个人引导协调；
2. CA-TA 的预算、阶段和候选产出覆盖；
3. Qp 与 CA-TA 的信用、触发和结算耦合。

每个候选必须量化：

```text
触发事件数/合法事件数
实际改变teacher或动作的比例
实际影响CFVF offspring的比例
直接覆盖或可传播到的FE比例
与失败窗口的时序关系
```

只有满足以下任一条件的候选才可进入实验：

```text
直接触达至少10%的目标事件或完整评价；
或虽然直接频率较低，但源码与既有事件证明其结果会广播到至少10%的后续搜索。
```

像 Teacher Exposure 那样只覆盖全部教师事件 1.12%、且所达路径本身已高度分散的旋钮必须在实现前停止。

本阶段最终只允许选出一个 repair family。后续 `C0/C1/C2/C3` 必须是同一修订轴的四档强度：

```text
C0 = 当前A4精确控制
C1 = 弱
C2 = 中
C3 = 强
```

禁止 C1 改 Dual-Q、C2 改 CA-TA、C3 改 PDDR 这种异质赛马。

## 6. 阶段 C：V35-R 分级开发

### 6.1 100k fast reject

```text
4 configs × 4 DEVELOPMENT instances × 2 paired seeds × 100k
= 32 runs
```

100k 只负责淘汰明显失败配置，不负责选最终冠军。只有边界配置才允许按预登记补第三 seed。首先检查100-job collapse，再比较HV/IGD平均秩；Cmax、TEC、TWC作安全门和解释。

### 6.2 Top2 @250k

```text
2 configs × 4 instances × 3 paired seeds × 250k
= 24 runs
```

先过 robustness gate，再比较HV/IGD；任何复现旧A4困难实例 collapse 的配置直接淘汰。

### 6.3 500k development final

```text
Top2 configs
+ HMOPSO-QGS-F
+ Gap Probe中预登记规则选出的最强external
× 4 DEVELOPMENT instances
× 2 paired seeds
= 32 runs
```

本阶段只产生 `PROVISIONAL_V35_R`，不得设置 `FINAL_FROZEN=true`。若没有配置在不产生规模性collapse的前提下稳定优于当前A4和主要baseline，记 `NO_V35_R_CANDIDATE` 并停止继续盲调。

## 7. 阶段 D：DOE 迁移，不自动重做DOE1

### 7.1 DOE-M0

若V35-R没有改变会与子群容量产生交互的选择/协调语义，直接继承 DOE1 的 `20/40/20/20`，停止新DOE。

### 7.2 DOE-M1：四配比迁移门

```text
20/40/20/20
30/50/10/10
25/25/25/25
20/40/30/10

4 mixtures × 3 instances × 3 seeds × 250k
= 36 runs
```

若当前配比仍稳健第一，保持原DOE冻结。若只有一个 challenger 略优，不得立刻重跑完整DOE。

### 7.3 DOE-M1.5：单challenger确认

```text
BASE vs challenger
× 3 instances × 3 seeds × 500k
= 18 runs
```

只有challenger在500k仍达到预注册改善和安全门，才允许考虑替换配比。

### 7.4 DOE-M2：完整重做的唯一条件

只有出现以下任一情况才允许重做15-treatment DOE：

- BASE从第一明显跌至后列；
- 多个challenger同时稳定占优；
- mixture×instance交互范围超过2个百分点；
- 配比排名发生大面积反转。

完整DOE与held-out确认必须另行预注册、另行授权。DOE2 Pacing不自动启动。

## 8. 阶段 E：Validation Race

只使用未污染的 `VALIDATION_RESERVED`：

```text
1个50-job
1个100-job
1个150/200-job

PROVISIONAL_V35_R
HMOPSO-QGS-F
Gap Probe选出的最强external

3 algorithms × 3 instances × 2 seeds × 500k
= 18 runs
```

Validation 只作 Go/No-Go，不作论文显著性。如果结果非常稳定，可按预登记追加第二external或第三seed；不得看到结果后临时改变阈值。

Validation通过才允许进入 Final Freeze。Validation失败后若修改算法，本批实例立即改标 `CONTAMINATED_DEVELOPMENT`。

## 9. 阶段 F：Final Freeze

冻结对象至少包括：

```text
commit/tag
formal Jar SHA-256
V35-R canonical configuration/hash
repair family与最终强度
mixture
CFVF / Dual-Q / CA-TA
PDDR / Pacing / local-search order
FM3 / SUT / fatigue configuration
instance role registry
formal seeds与snapshot生成规则
objective mapping [0,1,6]
budget protocol
output schema
reference/statistics protocol
```

旧版必须保留为 `A4_LEGACY`，最终版使用 `V35_R_FINAL`；禁止覆盖旧Jar或旧证据目录。

## 10. 阶段 G：正式消融

### 10.1 主正文消融：leave-one-component-out

建议臂：

```text
FULL V35-R
FULL - CFVF
FULL - DualQ
FULL - CA-TA
FULL - Final Coordination
HMOPSO-QGS-F
```

每个移除臂必须先通过依赖合法性审计。若关闭Dual-Q会使CA-TA语义失效，则不得伪造该臂，应将依赖模块作为bundle移除并如实命名。

首档：

```text
6 instances × 10 seeds × 6 arms × 500k
= 360 runs
```

只有方差、置信区间或审稿需求表明证据不足，才扩到9实例或补到20 seeds。

### 10.2 开发历史链

```text
A0 -> A1 -> A2 -> A3 -> A4_LEGACY -> V35_R_FINAL
```

该链用于机制演化图、负结果和附录。优先复用已验收证据，只在3--6个代表实例补缺；不得恢复旧4500条A0--A4矩阵。A3或A4_LEGACY出现负贡献时必须如实报告，不要求单调改善。

## 11. 阶段 H：外部算法与正式主比较

正式roster目标：

1. HMOPSO-QGS-F；
2. HMOPSO-QLS-F；
3. MOPSO-F；
4. MOPSODS-DE-F；
5. MOHEADE-F；
6. NSGA-II-F；
7. SPEA2-F；
8. V35-R-Final。

QMOEA保持 `PENDING_SOURCE_VERIFICATION`；无可信源码不得用相近类冒充。NSGA-II-F与SPEA2-F已通过production preflight，其余算法仍须完成同等级source identity、最小适配、2k身份测试和20k生产预检。

### 11.1 Formal Stage 1

Final Freeze后首次打开 `FINAL_TEST_RESERVED`：

```text
8 algorithms × 9 instances × 5 seeds × 500k
= 360 runs
```

这不是调参集。若结果不理想，只能继续预注册计划、停止扩大或收缩论文主张，不能修改算法。

### 11.2 Formal Main

```text
8 algorithms × 45 instances × 10 seeds × 500k
= 3600 runs
```

先以10 seeds形成正式主结果；是否对部分或全部比较扩到20 seeds，由预注册的方差、置信区间、排名稳定性与功效规则决定，不自动翻倍到7200条。

## 12. 统一指标与统计

每个实例必须等所有正式参与算法完成后，才构造统一 empirical PFref。开发、Gap、Validation、Final Test和Formal Main的reference不得混用。

主要指标：

```text
HV
IGD / IGD+
双向C-metric
min Cmax / min TEC / min TWC
Spacing（诊断）
frontSize（诊断）
runtime与FE utilization（诊断）
```

每个实例内先对seed聚合，再以实例作为主要配对统计单元：

```text
所有算法：Friedman
V35-R vs baseline：paired Wilcoxon
多重比较：Holm
效应量：paired rank-biserial correlation
```

方向统一为“V35-R更好时效应量为正”：HV取高为优；IGD、Cmax、TEC、TWC在统计输入前统一反向编码。

## 13. FM3独立模型实验

算法超过baseline不能证明FM3模型合理。另行固定搜索算法，比较 `FM0 -> FM1 -> FM2 -> FM3`，报告调度目标、疲劳暴露、自然恢复、实际工时、人员负荷及有限的参数敏感性。该模型实验不得与算法消融混为一张因果表。

## 14. 证据与停止条件

任何计算工作包必须先回答：

```text
Which preregistered gate authorizes this run?
```

每一工作包必须保存：预登记、角色注册表、输入/Jar/config/snapshot哈希、runId/sourceRunId、原始前沿、状态、日志、分析脚本、自动裁决和文件级SHA-256。任何未登记运行不得进入下一阶段的reference或选择。

当前唯一允许的下一步是：

```text
V35-GAP-PROBE-P0：0-FE预登记与四算法集成贯通准备
```

它不授权16条500k运行。所有大规模运行继续等待用户单独批准。

