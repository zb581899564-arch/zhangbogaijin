# Qp-v2 单轴 K 语义来源核查报告（第一硬门）

核查日期：`2026-09-02`
性质：**0-FE 只读语义审计**。未修改任何源码，未编译，未运行实验，未消耗任何评价预算。
核查目的：回答任务书第三节七问，判定获批材料能否**唯一确定** Qp-v2 的 K 语义。

---

## 1. 核查的权威来源清单

| # | 来源 | 检索结果 |
|---|---|---|
| S1 | `docs/V35_SOURCE_ATTRIBUTION_500K_PHASE_A_PLAN.md` v1.0（三人共识冻结执行方案，2026-08-31冻结） | 全文246行仅 §8（L203）一处提及K轴；G2出口（L182）提及 `QP_CANDIDATE_SET_POLICY_V2` 名称 |
| S2 | `AGENTS.md`（1462行，含 §30.1/§30.5 G4条款与 §38 Phase A 收口） | 仅授权状态位 `QP_V2_AUTHORIZED=false` 与"须新的明确授权"条款；无K语义 |
| S3 | `docs/ROADMAP.md`（D-112…D-115，L2399–2616） | 5处 `QP_V2_AUTHORIZED=false`；无K语义 |
| S4 | `docs/PAPER_EVIDENCE_MASTER/CURRENT_SCIENTIFIC_STATE.md`（521行） | 无Qp-v2/K内容 |
| S5 | 三人共识证据目录 `docs/evidence/V35-SOURCE-ATTRIBUTION-500K/`（00-preregistration、09-v5-sa-hard-500k、10-v5-sa-normal-500k 的全部决策/预注册/报告文件） | 9处 `QP_V2_AUTHORIZED=false`；无K语义 |
| S6 | Qp现有源码 `java-jmetal58/.../zhangbo/ZhangBoQpCandidateSelector.java`（218行，全读） | 当前A4四动作→唯一候选的确定性映射（详见§3） |
| S7 | `docs/evidence/V35-PFC5-TEACHER-EXPOSURE-CAL-PREREG/01-source-semantics/TEACHER_SELECTION_CALL_CHAIN.md`（2026-08-30对冻结Jar源码树的只读审计） | 明确结论：Qp每个动作的合法候选集合均为单元素（或空），不存在"动作已定、在集合内改选"的自由度 |
| S8 | Git 全历史 `git log --all --grep` | 无任何 Qp-v2 实现提交 |
| S9 | 相邻但**不同轴**的历史机制（排除项）：A5 `directionalTeacherPool`（Qg action-2 top-k池，k=10，默认关闭，永久禁启清单）；Q1冷启动tie-break（动作破平，已否证，永久禁启清单）；Q0观察策略 | 均为Qg侧或Qp动作选择轴，且都在"永久不自动重启"清单内，**不是**K语义来源 |

## 2. 获批材料中关于K的全部原文

**唯一原文**（S1 §8，L203；亦是"Qp-v2仅保留预注册结构计划"（L197）所指的全部结构计划）：

> 若未来另行批准Qp-v2，唯一轴为`K=1,2,3,4`，且`K=1`必须精确等价current A4；不得同时调teacher lambda、
> PA size、tauQ或epsilon。单轴证明先4配置×normal/hard100×2seed×250k，再Top2×2实例×2seed×500k。
> 只有关闭原external gap至少50%，或进入最强external±15%竞争带且normal安全，才允许申请DOE。

**关联名称**（S1 §7 G2出口，L182；该出口路径从未触发——实际裁决为G4）：

> 未来唯一候选repair family为`QP_CANDIDATE_SET_POLICY_V2`，但`IMPLEMENTATION_AUTHORIZED=false`。

因此获批材料对K的全部确定内容为：

1. 轴取值：`K ∈ {1,2,3,4}`；
2. K=1 必须与 current A4 **精确等价**（这是要求，不是机制定义）；
3. 禁止同时调整：teacher lambda、PA size、tauQ、epsilon；
4. 证明协议：4配置×normal/hard100×2seed×250k → Top2×2实例×2seed×500k；
5. DOE申请条件（external gap关闭≥50% 或 ±15%竞争带且normal安全）；
6. repair family名称：`QP_CANDIDATE_SET_POLICY_V2`（G2从未触发，未展开）。

## 3. 当前A4的Qp语义基线（源码事实，供七问对照）

`ZhangBoQpCandidateSelector.build()`（S6，行号引自该文件）：

```text
输入：个人谱系档案条目 entries（容量L=6）、selectedFingerprint、group、current、gbest、bounds
L33-35  按 fingerprint 字典序稳定排序
L36     DIRECTIONAL = argmin φ(e)，平局取fingerprint小者        （L63-74）
L37-38  KEEP = find(selectedFingerprint)，缺失则回退DIRECTIONAL
L44     EPSILON = argmin ε-fitness(e)，平局取fingerprint小者     （L76-88）
L45-46  COMPLEMENTARY = 在质量集 {e: φ(e)≤bestφ+qualityTolerance} 内
        min cosine → max spacing → fingerprint小者；质量集<2或social范数≤ε时为null （L90-130）
L42-47  仅当 sorted.size()>1 时才提供 DIRECTIONAL/EPSILON/COMPLEMENTARY
L48-56  mask按fingerprint去重（与更早动作候选重复的动作置为非法）
L57-59  KEEP必须恒合法，否则抛IllegalStateException
```

随后 `ZhangBoQpController.select()`（据S7审计 §1/§2.6）：

```text
:451  random.nextDouble()          → ε-greedy 决定 exploit/explore
:455  random.nextInt(0, valid.size()-1)  → explore 时在合法动作内均匀选
选定的 action → candidates.get(action) = 唯一档案条目 → 认知领导(pbest)
```

**源码级结论**（S7原文）：*"Qp 不是'一个候选集合 + 选择'，而是'动作 → 唯一候选'的固定映射"；
"Qp 每个动作的合法候选集合均为单元素（或空）。不存在'动作已定、在集合内改选哪个 teacher'的自由度。"*

## 4. 七问逐项答复

### Q1：K究竟计数什么对象？

**UNDEFINED**。获批材料从未说明K计数的对象。现有代码中可设想的候选对象至少有三种互不等价的读法：
(a) 每个动作内部的top-K候选池（按该动作自身排序取前K）；(b) 个人档案（L=6）层面的K；(c) 领导槽位/锚点数。
没有任何获批文本在其中做出选择。G2出口（本应展开`QP_CANDIDATE_SET_POLICY_V2`的路径）从未触发。

### Q2：K在哪个现有合法候选集合内生效？

**UNDEFINED**。现有合法集合有三个：每动作单例候选映射、个人档案条目集（容量L=6）、
COMPLEMENTARY的质量集。获批材料未指定K作用于哪一个。

### Q3：K如何影响个人领导选择？

**UNDEFINED**。当前机制是"ε-greedy选动作→唯一候选"。K>1时：是动作内改为在K个候选中再选择？
选择规则是锦标赛（如Qg action-2）、确定性argmin、随机均匀还是轮转？是否保持四动作集合与mask语义？
均无定义。

### Q4：K是否改变Qp动作、奖励或档案容量？

**部分约束、实质UNDEFINED**。获批材料仅禁止"同时调"teacher lambda / PA size / tauQ / epsilon
（任务书冻结清单同向扩展）。但K本身是否改变四动作的锚点语义、mask去重规则或奖励计算，无任何定义。

### Q5：K=1为什么能够精确还原当前A4？

**UNDEFINED（机制层面）**。要求本身明确（"K=1必须精确等价current A4"），但还原机制未定义：
若读法为"每动作按其自身排序取top-1"，则恰好退化为当前argmin行为；若读法为"某个全局单一排序取top-1"
或"K个领导槽位取1"，则**不会**还原当前A4（四动作各自排序互不相同：φ、ε-fitness、cosine/spacing）。
等价性无法从获批材料推导，只能靠事后实现碰运气——这正是任务书禁止的"自行补一个看似合理的Top-K算法"。

### Q6：K>1如何选择候选以及如何稳定破平？

**UNDEFINED**。无选择规则、无破平规则。任务书自身亦要求"候选不足时的fallback必须来自预注册定义"——
而该预注册定义不存在（档案条目可能少于K、质量集可能少于K、动作候选可能因mask去重而缺失）。

### Q7：是否增加随机数消费？

**UNDEFINED**。当前Qp每次选择消耗1–2次随机抽取（Controller :451/:455）。若K>1引入池内选择
（例如仿Qg action-2的二元锦标赛），将新增`nextInt`消费并改变RNG序列；若为确定性取首，则不新增。
S7确立的RNG契约（"任何重选都不得引入或省略任何一次随机抽取"）只针对Qg注入点，对Qp-v2的K无对应条款。

## 5. 判定

```ini
QP_V2_SEMANTICS_UNDERDEFINED=true
```

获批材料唯一确定的只有：轴取值、K=1等价性**要求**、同时调整禁令、证明协议与DOE条件。
七个必需语义定义中，Q1/Q2/Q3/Q6/Q7完全缺失，Q4仅部分约束，Q5只有要求没有机制。
任何K的具体实现（包括"每动作top-K+确定性argmin"这一最自然的读法）都将是Agent的自行发明，
违反任务书第一硬门（"不得自行发明K语义……不得自行补一个看似合理的Top-K算法"）
与AGENTS.md §30.5 G4条款（G2路径从未触发展开该repair family）。

## 6. 恢复路径（供用户决策，非本工作包执行项）

若用户希望继续Phase B1，须先以**新的明确授权**冻结K语义预注册，至少补齐：
K计数对象与作用集合；K>1的选择与稳定破平规则；候选不足fallback；RNG消费契约；
K=1→当前A4的逐字段还原机制（证明而非假设）；mask/动作集合/奖励不变式。
这些定义一经用户批准即成为`V35QpV2Profile`的唯一依据，此后才允许实现、等价门与20k工程门。
