# 基于李明哲 HMOPSO-QGS 的 PF-SDST—动态疲劳、Qp/Qg—DSCR—CFVF 与 CA-TA-Lite-VNS 综合改进方案 v3.5

> **版本：** v3.5（Scope-Converged, Dominance-Safe Social Guidance）  
> **状态：** 方案冻结候选；本文是实现、单变量实验和论文写作的规范源，尚不代表代码已经实现或正式实验已经完成。  
> **建立日期：** 2026-08-12  
> **直接基座：** 李明哲第四章 HMOPSO-QGS 与本项目 `deterministic_canonical` Java/jMetal 5.8 生产线。  
> **逐节继承源：** `E:\学习\ziliao\HMOPSO_QGS_PF-SDST_疲劳_全向量双Q_轻量精确增量解码_RecoveryRescue_SE-FCRS_CA-TA-Lite_Cmax四阶段审计_综合改进方案_v3.4.md`。  
> **继承源 SHA-256：** `1398E78E923D6975E6284EE6FA3C35DD7028B96A61A9557A1BAC762E5AB95FCF`。  
> **关键收口：** PF-SDST 与动态疲劳保留；Qp/Qg、CFVF、CA-TA-Lite 保留；新增 DSCR；FCLS、FCRS、Recovery-Rescue、IncrementalReplay 退出正式主线；Temporal Shift 仅保留未来研究占位。  
> **实验硬边界：** 本文只定义实验，不授权自动启动多实例、多种子、20 次正式矩阵或上传任何结果。

---

# 0. 文档用途、权威边界与状态词

本文同时承担三种用途，但在章节上严格分开：

1. **Explanation：** 解释为什么 v3.5 要从 v3.4 收口，以及 DSCR 的证据链与设计理由；
2. **Reference：** 冻结模型、公式、时序、状态、动作、日志、统计和论文表述；
3. **Implementation specification：** 给 Java/jMetal 实现者提供可测试的接口、伪代码、失败条件和验收门。

全文状态词只有以下四种：

| 状态 | 精确定义 | 能否进入 v3.5 正式运行 |
|---|---|---:|
| `ACTIVE` | v3.5 主线必须执行的机制 | 是 |
| `INHERITED` | 从基座继承、继续执行，但不得冒充 v3.5 新创新 | 是 |
| `DISABLED` | 历史代码或证据可以保留，但 v3.5 默认配置与正式实验必须关闭 | 否 |
| `RESERVED` | 未来研究占位；当前没有足够的 genotype—phenotype 持久化语义，禁止实现成隐式行为 | 否 |

如果文字说明、旧配置、旧类名或历史实验标签与本表冲突，以本表和第 1 节的冻结矩阵为准。

---

# 1. v3.5 一页冻结结论

## 1.1 三项创新保持不变

v3.5 仍然只申报三项核心创新：

$$
\boxed{
\text{创新 1：PF-SDST + Dynamic Fatigue Model/Decoder}
}
$$

$$
\boxed{
\text{创新 2：Qp/Qg + DSCR + CFVF 认知—社会全向量全局搜索}
}
$$

$$
\boxed{
\text{创新 3：CA-TA-Lite-VNS}
}
$$

DSCR 归入创新 2，不另列“第四创新”。产品族、SDST、疲劳、PSO、Q-learning、VNS 等概念本身都不是本文首次提出；贡献在于针对当前继承问题构造的耦合模型和机制组合。

## 1.2 机制冻结矩阵

| 层 | 机制 | v3.5 状态 | 冻结说明 |
|---|---|---|---|
| 基座 | 四向量 `JS/FA/MA/WA` | `INHERITED` | 保持与李明哲第四章的编码身份一致 |
| 基座 | 四子群、原 Qg 三动作、PDDR-FF | `INHERITED` | DSCR 只维护 Qg cache，不改动作空间 |
| 模型 | 产品族 PF-SDST | `ACTIVE` | 保留序列相关设置时间 |
| 模型 | 动态疲劳、自然恢复、两阶段 setup-processing 传播 | `ACTIVE` | 保留统一确定性解码 |
| 模型 | 三目标 $C_{\max}/TEC/TWC$ | `INHERITED` | 不增加疲劳第四目标 |
| 全局 | 谱系个人非支配档案与 Qp | `ACTIVE` | 四动作、16 状态、容量 6 |
| 全局 | 原 Qg + DSCR | `ACTIVE` | DSCR 在 Qg action 前清理 `previous/historical` cache |
| 全局 | CFVF 全向量离散飞行 | `ACTIVE` | 社会与认知领导作用到 `JS/FA/MA/WA` |
| 全局 | Qp/Qg 分块冻结 | `ACTIVE` | 冻结学习，不冻结环境感知 |
| 局部 | CA-TA-Lite 五类宏邻域 | `ACTIVE` | `context=(group,bottleneck)` |
| 局部 | N5 Structural Recovery Restructuring | `ACTIVE` | 只能改 genotype，不直接平移 start time |
| 时间表 | FCLS | `DISABLED` | 不进入基础 Decoder，不进入 FULL v3.5 |
| 时间表 | FCRS / SE-FCRS / Recovery-Rescue | `DISABLED` | 不进入基础 Decoder，不进入 FULL v3.5 |
| 加速 | IncrementalReplay / 三资源 shift DAG | `DISABLED` | v3.5 不需要 shift candidate evaluator |
| 未来局部搜索 | Temporal N6 / Temporal Shift Module | `RESERVED` | 当前不定义可执行语义，不注册 CA-TA 动作 |
| Cmax 修补 | G1 intensification / Directional Elite / PDDR 改造 | `DISABLED` | 先做 QG0 vs QG1 单变量 DSCR 实验 |

## 1.3 v3.5 的默认执行身份

正式候选配置应至少写出：

```text
algorithmSemanticVersion=v3.5-dscr-scope-converged
decoderSemanticVersion=pf-sdst-fatigue-v3.5-no-shift
shiftMode=NONE
temporalShiftModule=RESERVED_DISABLED
fclsEnabled=false
fcrsEnabled=false
recoveryRescueEnabled=false
incrementalReplayEnabled=false
dscrMode=PRE_ACTION_DOMINANCE_SAFE
dscrSnapshotScope=FROZEN_QG_SOCIAL_CANDIDATES
caTaMode=LIGHTWEIGHT_MACRO_5
```

任一正式 v3.5 结果如果缺少上述状态记录，或出现非零 FCLS/FCRS/Temporal Shift 事件，不得标记为 v3.5。

---

# 2. 从 v3.4 到 v3.5：保留、删除与修正

## 2.1 保留

- 产品族驱动的 PF-SDST；
- setup 与 processing 两阶段疲劳传播；
- 工人自然恢复与疲劳反馈工时；
- 四子群固定语义；
- 谱系个人档案、Qp 四动作、Qp/Qg 分块冻结；
- CFVF 对全部显式向量的认知—社会引导；
- 轻量 CA-TA 的五类宏邻域、Test/Apply 和确定性内部路由；
- 评价后 PDDR-FF；
- 三主目标与人因辅助指标；
- Cmax 证据链、被动观测与严格分级报告。

## 2.2 从正式主线删除

v3.4 中以下长链不再进入 v3.5 正式算法：

```text
FCLS common-gap compaction
→ freeze CmaxStar
→ Recovery-Rescue SE-FCRS
→ exact affected-closure IncrementalReplay
→ FullReplay verification
```

删除理由不是“右移一定无效”，也不是否定现有 P8.6 工程成果，而是：

1. 当前 500k profiling 中基础 FM3 仅占约 `0.14%`，FCLS/FCRS 却占约 `32.85%/48.80%`；
2. FCRS 约 `63,884,116` 次完整重传播，约 `127.77` 次/FE，显著扩大工程复杂度；
3. 当前最直接的新证据指向 Qg stale social leader reuse，而不是 PDDR 或 shift；
4. shift 结果如果只改变 phenotype start time，而不改变 $X=(JS,FA,MA,WA)$，重新解码后无法遗传；
5. v3.5 的目标是先验证一个清晰、可归因的 DSCR 单变量，而不是同时替换 Decoder 和全局搜索。

这些数字来自旧 `fatigue-shift-v2-common-gap/LEFT_RIGHT` 的单实例单 seed 诊断，不得外推成普适性能结论。

## 2.3 新增与修正

v3.5 新增的唯一实质机制是：

$$
\boxed{
DSCR=Dominance\text{-}Safe\ Social\ Cache\ Refresh
}
$$

其核心不是“强制 G1 选择当前最小 Cmax”，而是：

> 在 Qg 选择动作之前，用本轮冻结的社会知识集合检查 `previous` 与 `historical` cache；只有当缓存领导被当前可见解严格 Pareto 支配时，才从支配它的集合中按子群方向选择替代者。

v3.5 同时修正三项规范：

- DSCR 发生在 Qg action **之前**，避免污染 `action → behavior → reward`；
- shift 未来占位必须先解决 genotype—phenotype persistence，不能简单改名为 N6；
- N5 只能通过 `JS/MA/WA` 的结构变化制造恢复，禁止直接修改 start time。

---

# 3. v3.5 的证据起点与论文边界

## 3.1 当前已直接观察到的事实

在 `20_2_3_1`、seed `20260808`、population `100`、`20000 FE` 的当前旧语义审计中，新 Cmax 纪录：

$$
x_{new}=(201.278740141651,10986.681,17109.382)
$$

严格支配旧社会老师：

$$
x_{old}=(205.902163389086,11378.794,18330.467)
$$

即：

$$
\boxed{x_{new}\prec x_{old}}
$$

但教师使用为：

| 项目 | $x_{new}$ | $x_{old}$ |
|---|---:|---:|
| 生成位置 | FE 6750 / generation 23 | 更早历史纪录 |
| G1 Qp 个人老师使用 | 15 粒子次 / 9 代 | 非本节重点 |
| G1 Qg 社会老师使用 | 0 | 680 粒子次 / 34 代 |
| 最后一次新纪录教学 | FE 13224 | 持续至 FE 16828 |

行为等价追踪证明前沿、Cmax 曲线、FE、Qg/Qp 表与机制事件流哈希一致，因此该教师指纹观测没有改变受审计运行的算法行为。

## 3.2 已证明与未证明

当前证据足以支持：

$$
\boxed{
\text{当前配置中直接观察到了 stale dominated social leader reuse。}
}
$$

当前证据不能提前支持：

$$
\boxed{
\text{DSCR 在无 shift 的 v3.5 中必然改善 }C_{\max}\text{ 或 HV。}
}
$$

原因是 v3.5 将 `ShiftMode=NONE`，搜索轨迹会变化；因此必须在 v3.5 新语义下重新执行 QG0/QG1 单变量实验。

## 3.3 正确论文措辞

禁止写：

> 李明哲的 Qg 有 bug，本文将其修复。

推荐写：

> 原社会领导保留机制能够提供搜索稳定性，但在动态多目标搜索中，历史领导可能随着新 Pareto 解出现而变为严格被支配状态。基于行为审计证据，本文在不改变 Qg 动作空间的前提下，引入 dominance-safe social cache maintenance，使历史社会经验得以保留，同时避免持续使用相对于当前社会知识集合已知严格劣质的缓存领导。

---

# 4. 必须继承的 HMOPSO-QGS 基座

## 4.1 四向量身份

粒子保持：

$$
\boxed{X_i=(JS_i,FA_i,MA_i,WA_i)}
$$

- `JS`：工件顺序；
- `FA`：工厂分配；
- `MA`：第一阶段机器分配；
- `WA`：第一阶段工人分配。

四条向量按**工件身份**对齐。任何资源学习、差异动作、交叉、修复和日志都不得把向量位置误当成工件编号。

## 4.2 四子群语义

正式语义锁死：

$$
G_1\rightarrow C_{\max},\qquad
G_2\rightarrow TEC,\qquad
G_3\rightarrow TWC,\qquad
G_4\rightarrow Balanced
$$

代码身份：

```java
G1_CMAX
G2_TEC
G3_TWC
G4_BALANCED
```

禁止通过 `ordinal()`、物理数组顺序或旧变量名推断目标职责。

## 4.3 统一方向函数

三主目标：

$$
f_1=C_{\max},\qquad f_2=TEC,\qquad f_3=TWC
$$

在冻结参考集合上归一化：

$$
\bar f_m(x)=
\frac{f_m(x)-z_m^{min}}
{z_m^{max}-z_m^{min}+\varepsilon}
$$

方向评分：

$$
\phi_1(x)=\bar f_1(x),\quad
\phi_2(x)=\bar f_2(x),\quad
\phi_3(x)=\bar f_3(x)
$$

$$
\boxed{
\phi_4(x)=\max_{m\in\{1,2,3\}}\bar f_m(x)
}
$$

`directional pbest`、Qp 方向奖励、DSCR replacement、工厂方向压力和 CA-TA 子群收益必须共享同一语义源。

---

# 5. 创新 1：产品族 PF-SDST—动态疲劳统一模型

## 5.1 产品族与非对称换型矩阵

每个工件固定产品族：

$$
g_j\in\{1,\ldots,F\}
$$

每个阶段 $k$ 固定非对称转换矩阵：

$$
B^k=[B^k_{ab}],\qquad B^k_{aa}=0
$$

允许：

$$
B^k_{ab}\ne B^k_{ba}
$$

## 5.2 PF-SDST

保留李明哲原当前工件基础准备量 $SUT_{j,k}$，新增前驱产品族换型负担：

$$
\boxed{
SUT^{PF}_{f,k,m,i,j}
=SUT_{j,k}+B^k_{g_i,g_j}\eta_{f,k,m}
}
$$

其中：

- $i=Pred_M(j,k)$ 为机器直接前驱工件；
- $SUT_{j,k}$ 是当前工件自身基础准备量；
- $B^k_{g_i,g_j}$ 是产品族转换负担；
- $\eta_{f,k,m}$ 是可选机器换型因子。

虚拟首件前驱 0 的主定义：

$$
B^k_{0,g_j}=0
$$

当全部 $B^k_{ab}=0$ 时，严格退化到原 sequence-independent setup。

## 5.3 疲劳累积与恢复

工人疲劳状态：

$$
F_w(t)\in[0,1)
$$

工作 $d$ 时间后的累积：

$$
F^{out}=F^{in}+(1-F^{in})(1-e^{-\lambda_{f,w,k}d})
$$

休闲/等待 $r$ 时间后的自然恢复：

$$
F^{after}=F^{before}e^{-\mu_{f,w,k}r}
$$

正式标准化场景：

$$
\lambda_{f,w,k}\sim U(0.01,0.03),\qquad
\mu_{f,w,k}\sim U(0.03,0.07)
$$

但每个实例只采样一次并固化；同一实例、所有算法、所有 seed 必须读取同一参数清单。

## 5.4 疲劳工时倍率

$$
\boxed{
q_k(F)=1+\frac{r_k}{\ln2}\ln(1+F)
}
$$

主场景：

$$
r_k=0.30
$$

无疲劳工时反馈消融：$r_k=0$。严重场景 $r_k=0.70$ 仅作敏感性，不得与主结果混写。

## 5.5 setup-processing 两阶段传播

对工序 $O_{j,k}$：

$$
S^{set}_{j,k}=\max\{C^J_{j,k},C^M_{j,k},C^W_{j,k}\}
$$

$$
t^{rec}_{j,k}=\max(0,S^{set}_{j,k}-C^W_{j,k})
$$

$$
F^{set}_{j,k}=F^{before}_{j,k}e^{-\mu t^{rec}_{j,k}}
$$

$$
SET^0_{j,k}=\frac{SUT^{PF}_{f,k,m,i,j}}{WE_{f,w,k}}
$$

$$
SET_{j,k}=q_k(F^{set}_{j,k})SET^0_{j,k}
$$

$$
F^{mid}_{j,k}=F^{set}_{j,k}+(1-F^{set}_{j,k})(1-e^{-\lambda SET_{j,k}})
$$

$$
PT^0_{j,k}=\frac{ST_{j,k}}{MS_{f,k,m}WE_{f,w,k}}
$$

$$
PT_{j,k}=q_k(F^{mid}_{j,k})PT^0_{j,k}
$$

$$
C_{j,k}=S^{set}_{j,k}+SET_{j,k}+PT_{j,k}
$$

$$
F^{end}_{j,k}=F^{mid}_{j,k}+(1-F^{mid}_{j,k})(1-e^{-\lambda PT_{j,k}})
$$

完整因果链：

$$
\boxed{
Pred_M\rightarrow SUT^{PF}\rightarrow SET^0\rightarrow F^{set}
\rightarrow SET\rightarrow F^{mid}\rightarrow PT\rightarrow F^{end}
}
$$

## 5.6 v3.5 Decoder 唯一语义

基础解码必须是确定性的 `PF-SDST + Dynamic Fatigue Full Decode`：

```text
X=(JS,FA,MA,WA)
→ validate identity-aligned resource assignments
→ construct first-stage explicit MA/WA schedule
→ later stages choose legal (machine,worker) by full ECT preview
→ propagate PF-SDST, recovery, fatigue, setup and processing
→ compute Cmax/TEC/TWC and auxiliary diagnostics
→ return schedule
```

v3.5 在此处结束 Decoder。禁止继续调用 FCLS、FCRS、Temporal Shift 或 IncrementalReplay。

后续阶段对合法 $(m,w)\in\Omega_{j,k}$：

$$
(m^*,w^*)=\arg\min_{(m,w)\in\Omega_{j,k}}ECT_{j,k,m,w}
$$

同值按 `machine id → worker id` 稳定破平。

## 5.7 目标与辅助指标

优化目标保持：

$$
\boxed{\min(C_{\max},TEC,TWC)}
$$

其中：

$$
C_{\max}(x)=\max_j C_j(x)
$$

$$
TEC(x)=\sum \text{energy contribution},\qquad
TWC(x)=\sum \text{worker cost contribution}
$$

疲劳不是第四目标。辅助报告至少包括：

$$
F_{max},\quad F_{avg},\quad
FE_{fatigue}=\sum_w\int_0^{C_{\max}}\max(0,F_w(t)-F^{warn})dt
$$

以及 high-fatigue proportion、最长连续工作、自然恢复总时长和疲劳方差。默认 $F^{warn}=0.80$；$F^{safe}=0.90$ 只作可选诊断，不启用主动休息基因。

---

# 6. 创新 2：Qp/Qg—DSCR—CFVF 全向量认知—社会搜索

## 6.1 个人谱系档案与 Qp

每个粒子谱系维护：

$$
PA_i=\{p_{i,1},\ldots,p_{i,L}\},\qquad L=6
$$

更新只接受本谱系已经评价的父代、全局后代与局部后代：

$$
PA_i^{t+1}=ND(PA_i^t\cup\{X_i^{t+1}\})
$$

禁止把 global archive 解直接注入个人档案。

Qp 四动作：

$$
a_0^p=KEEP
$$

$$
a_1^p=SUBSWARM\ DIRECTION
$$

$$
a_2^p=INDICATOR/CONVERGENCE
$$

$$
a_3^p=COGNITIVE\text{-}SOCIAL\ COMPLEMENT
$$

Qp 选择的是策略，不是固定 archive 下标。

Qp 状态：

$$
s_i^p=(E_g,H_i,R_i),\qquad 4\times2\times2=16
$$

每个子群共享一张 $16\times4$ Q 表。互补质量门：

$$
\mathcal C_i=\{p\in PA_i:\phi_g(p)\le\phi_g^{best}+\tau_q\},
\qquad \tau_q=0.15
$$

奖励：

$$
r_i^p=2r_i^{dom}+\widehat{\Delta\phi}_{i,g}
+0.5I_i^{PA}+0.25\widehat{\Delta Risk}_i
$$

奖励必须在任何局部搜索之前结算。

## 6.2 原 Qg 动作语义保持

原 Qg 的三动作继续为：

| 动作 | 行为语义 |
|---|---|
| $a_0^g$ | 使用 `previous` 缓存领导 |
| $a_1^g$ | 使用 `historical` 缓存领导 |
| $a_2^g$ | 从本轮当前社会候选集合进行锦标赛选择 |

DSCR 不增加动作、不改变 Q 表维度、不消耗随机数、不进行评价，也不把 action 0/1 偷换成 action 2。

## 6.3 为什么 DSCR 必须在 action 前

禁止流程：

```text
Qg chooses KEEP old leader
→ DSCR secretly replaces it
→ reward credits KEEP
```

这会破坏：

$$
\boxed{action\rightarrow behavior\rightarrow reward}
$$

正式流程：

```text
freeze social knowledge snapshot
→ DSCR sanitizes previous/historical cache
→ construct/refresh Qg state
→ Qg chooses action
→ execute exactly that action
→ CFVF
→ decode/evaluate
→ reward updates or observes Qg
```

因此 DSCR 是**社会知识缓存维护机制**，不是隐藏的 RL action。

## 6.4 冻结社会知识集合

定义本次 Qg 决策可见的冻结集合：

$$
\boxed{
A_t^{social}=\text{Qg 本轮决策时可见的冻结社会候选集合}
}
$$

V1 直接取完成本轮 archive/candidate update 后、进入 Qg 决策前，原 Qg action 2 实际可见的社会候选集合的不可变快照。

其边界必须同时满足：

1. 所有解已经完成三目标评价；
2. 使用与 Qg action 2 相同的候选来源，不扩大信息权限；
3. 快照在整轮所有 G1/G2/G3/G4 cache 清理和 leader query 期间冻结；
4. 本轮第一个粒子产生的新后代不能被同轮后续粒子的 DSCR 看见；
5. 任何 archive capacity/truncation 已在冻结前完成；
6. V1 不增加永久历史 dominance memory。

所以 DSCR 的保证只相对于当前 $A_t^{social}$，不是相对于算法历史上曾出现过但现已被截断的全部解。

## 6.5 严格支配集合

三目标最小化下，$x$ 严格 Pareto 支配 $L$ 当且仅当：

$$
x\prec L
\iff
\left(\forall m\in\{1,2,3\},f_m(x)\le f_m(L)\right)
\land
\left(\exists m,f_m(x)<f_m(L)\right)
$$

对每个缓存领导 $L$：

$$
\boxed{
D_t(L)=\{x\in A_t^{social}:x\prec L\}
}
$$

目标完全相同不构成严格支配；DSCR 不使用 epsilon dominance，不使用 PDDR score 代替 Pareto 支配。

## 6.6 replacement 规则

若：

$$
D_t(L)=\varnothing
$$

则：

$$
\boxed{L'_g=L_g}
$$

缓存即使不在当前 archive，只要未被当前冻结社会知识严格支配，仍可保留其探索价值。

若：

$$
D_t(L)\ne\varnothing
$$

则只能从 $D_t(L)$ 中替换：

$$
\boxed{
L'_g=\arg\min_{x\in D_t(L)}\phi_g(x)
}
$$

展开：

$$
L'_1=\arg\min_{x\in D_t(L)}C_{\max}(x)
$$

$$
L'_2=\arg\min_{x\in D_t(L)}TEC(x)
$$

$$
L'_3=\arg\min_{x\in D_t(L)}TWC(x)
$$

$$
L'_4=\arg\min_{x\in D_t(L)}\phi_4(x)
$$

G4 直接复用正式 Balanced 方向函数，禁止为 DSCR 再造 `max fatigue`、`G4Score2` 或新权重。

确定性破平：

```text
direction score
→ stable solution id
```

`solution id` 必须来自稳定、运行内唯一且不受对象内存地址影响的身份；必要时可用四向量规范指纹派生，但必须处理重复基因型的稳定序号。

## 6.7 清理对象和顺序

对每个子群的两个缓存分别清理：

```text
previous[group]
historical[group]
```

固定顺序：

```text
G1, G2, G3, G4
×
PREVIOUS, HISTORICAL
```

每次检查都只读同一个 $A_t^{social}$；前一个缓存的替换不得改变后一个缓存的候选集合。若两个 cache 最终选择同一解，允许保留，不强制多样化。

初始化规则：若某 cache 尚不存在，仍由原 Qg 初始化流程从 $A_t^{social}$ 选择；初始化不是 DSCR replacement，不计入 `SCRR`。

## 6.8 DSCR 两条性质

相对于当前冻结社会知识集合：

$$
\boxed{
\text{DSCR never replaces a cached leader unless a current visible solution strictly dominates it.}
}
$$

$$
\boxed{
\text{After sanitization, no cached social leader remains dominated by any solution in }A_t^{social}.
}
$$

DSCR **不保证**：

- cache 始终等于当前单目标极值；
- 201.279 必须成为 G1 leader；
- 历史上曾经被支配的 cache 永久失效；
- $C_{\max}$、HV 或 IGD+ 必然改善；
- Qg action 2 必然增加使用频率。

## 6.9 DSCR 伪代码

```text
function sanitizeAllCaches(snapshot A_social):
    assert A_social is immutable and fully evaluated

    for group in [G1, G2, G3, G4]:
        for slot in [PREVIOUS, HISTORICAL]:
            L = cache[group][slot]

            if L is absent:
                log NOT_INITIALIZED
                continue

            D = [x in A_social where strictlyDominates(x, L)]

            if D is empty:
                log VALID_KEEP
                continue

            replacement = stableArgMin(D, directionScore(group), solutionId)
            cache[group][slot] = deepCopy(replacement)

            log REPLACED with:
                generation, decisionCycle, FE,
                group, slot,
                oldId, oldObjectives,
                replacementId, replacementObjectives,
                dominatorCount,
                scoreName, oldFirstDominatedFE

    assert every present cache is nondominated by A_social
```

## 6.10 CFVF

资源包：

$$
R_{ij}=(FA_{ij},MA_{ij},WA_{ij})
$$

类别变量不做数值相减。资源差异动作按合法域分为：

$$
a_j^{FMW},\quad a_j^{MW},\quad a_j^M,\quad a_j^W
$$

两通道离散速度：

$$
V_i=(V_i^{JS},V_i^R)
$$

$$
V_{i,JS}^{t+1}
=\mathcal S_{\omega_{JS}}(V_{i,JS}^t)
\oplus\mathcal S_{c_1r_1}[\Delta_{JS}(P_i,X_i)]
\oplus\mathcal S_{c_2r_2}[\Delta_{JS}(G_g,X_i)]
$$

$$
V_{i,R}^{t+1}
=\mathcal S_{\omega_R}(V_{i,R}^t)
\oplus\mathcal S_{c_1r_1}[\Delta_R(P_i,X_i)]
\oplus\mathcal S_{c_2r_2}[\Delta_R(G_g,X_i)]
\oplus E_R
$$

当 pbest/gbest 对同一工件资源包冲突时，按 $c_1r_1$ 与 $c_2r_2$ 的相对强度抽样；`FMW > MW > M/W` 处理覆盖层级。所有动作应用前必须按当前 `JS/FA` 和实例资源域重新验证。

## 6.11 Qp/Qg 分块冻结

预热预算：

$$
0.10\times MaxFEs
$$

随后按 $B=5$ 代交替：

```text
P-block: Qp learn; Qg greedy execute + observe; no Qg TD
G-block: Qg learn; Qp greedy execute + observe; no Qp TD
```

冻结控制器仍要刷新状态、候选领导和实际动作。正式原则：

$$
\boxed{\text{freeze learning, not environmental awareness}}
$$
