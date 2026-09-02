# 当前 Qp 语义合同（A4 冻结语义的规范化陈述）

日期：`2026-09-02`（Phase B0.5）。本文件把当前A4的Qp个人领导选择语义固化为合同，
作为 K=1 还原证明的规范基准。所有条目均直接来自源码（行号见 `CURRENT_QP_CALL_CHAIN.md`）。

## 1. 合同主体

**输入**（每次选择，每粒子）：
```text
E      = 谱系个人档案条目集（|E|≤6，互不支配，fingerprint唯一）
fp_req = 上一轮 selected pbest 的 fingerprint（经 ZhangBoQpLineageState 属性传递）
g      = 子群（G1_CMAX/G4_BALANCED/G2_TEC/G3_TWC）
cur    = 当前粒子（已评价父代）
gb     = 当前子群社会领导 gbest（已评价）
B      = 冻结归一化边界 ZhangBoArchiveBounds
mode   ∈ {EPSILON_GREEDY, GREEDY_FROZEN}（warmup 不进入本合同）
```

**规范语义**（当前A4）：
```text
sort(E)                     : 按 fingerprint 字典序
c[KEEP]         = find(E, fp_req) ?? argmin_φ(E)
c[DIRECTIONAL]  = argmin_{e∈E} (φ(e), fp(e))            [仅当|E|>1，否则非法]
c[EPSILON]      = argmin_{e∈E} (εfit(e), fp(e))          [仅当|E|>1，否则非法]
Q  = {e∈E : φ(e) ≤ min_φ + 0.15}
Q' = {e∈Q : ||dir(cur,e)|| > 1e-12}
c[COMPLEMENTARY] = argmin_{e∈Q'} (cos(e), -spacing(e), fp(e))
                  [仅当 |E|>1 ∧ |Q|≥2 ∧ ||dir(cur,gb)||>1e-12 ∧ |Q'|≥1，否则非法]

mask[a] = (c[a] 存在) ∧ (c[a].fp 未被更早动作占用)        [KEEP恒合法]
action  = mode分派（ε-greedy 或 冻结表贪婪；RNG合同见§2）
leader  = c[action]                                        ←确定性，无候选层随机性
```

**输出**：`Selection`（含 leader、mask、state、action、双方向分、档案快照等）→
CFVF认知引导 + Qp教师遥测 + settle结转（`find(E', leader.fp) ?? argmin_φ(E')`）。

## 2. RNG 合同（当前A4）

```text
每次EPSILON_GREEDY选择: nextDouble() ×1（恒定）
探索分支(draw<ε):      nextInt(0,|valid|-1) ×1
候选步:                零抽取
GREEDY_FROZEN/warmup:  零抽取
```
ε从0.30线性衰减至0.05（按FE进度）。

## 3. 不变量（K=1还原证明需逐条保持）

1. **四动作集合与语义**不变（KEEP/DIRECTIONAL/EPSILON/COMPLEMENTARY的锚点定义）；
2. **mask去重规则**不变（按动作序、fingerprint去重、KEEP恒合法）；
3. **动作选择**不变（ε-greedy/冻结贪婪、RNG次数与顺序不变）；
4. **候选步**在K=1时零RNG、identity=c[action]；
5. **奖励**不读取leader（只读 父/子/档案存活/疲劳），Q表更新由动作与奖励驱动；
6. **档案容量L=6**与更新规则不变；
7. **settle结转**规则不变（find-by-fingerprint，缺失→directional argmin）；
8. **PDDR/Qg/CFVF公式/CA-TA/FE**全部不变（CFVF只接收leader identity这一输入）；
9. **warmup**不进入K轴（pbest恒为directional argmin，跨K完全一致）。

## 4. 经验画像（既有遥测，0新FE；详见 `empirical/` 目录）

**真实A4 500k（V5 SA运行，100-job，动作分布）**：

| 运行 | KEEP | DIRECTIONAL | EPSILON | COMPLEMENTARY | 非KEEP |
|---|---|---|---|---|---|
| HARD 100_5_3_1/20260901 | 49.4% | 20.6% | 14.1% | 16.0% | **50.6%** |
| NORMAL 100_2_3_1/20260901 | 43.6% | 24.2% | 18.5% | 13.7% | **56.4%** |

（QP_ACTION事件=271,800/运行；lifecycle双写543,600不影响比例。）

**选择时档案规模（candidateViewSize，FC5教师遥测）**：

| 运行 | size=1 | size=2 | size=3 | size=4 | size=5 | ≥2合计 |
|---|---|---|---|---|---|---|
| A4 50k hard 100_5_3_1 | 40.05% | 24.05% | 14.33% | 14.45% | 7.12% | **59.95%** |
| A4 20k hard 100_5_3_1 | 89.08% | 10.92% | 0 | 0 | 0 | **10.92%** |
| A4 20k normal 100_2_4_1 | 91.40% | 8.60% | 0 | 0 | 0 | **8.60%** |
| D3 50k 20-job 20_2_3_1（3seed） | 55.33% | 44.67% | 0 | 0 | 0 | 44.67% |

（容量6从未触达；20-job D链与20k 100-job上档案≤2；50k hard上增长到3–5。）

**联合分布（A4 50k hard）**：非KEEP ∧ 档案≥2 = **33.60%**（8,434/25,100）；
directionalRegret>0 恰好等于 EPSILON+COMPLEMENTARY 选中数（4,496=3,626+870，
DIRECTIONAL选中regret恒为0）——遥测内部自洽。

**Qp教师身份集中度（teacherFingerprint真实四向量指纹）**：
- A4 50k hard：194个不同身份，top1=16.9%，**top5=54.7%**；
- A4 20k normal：117个，top1=26.7%，**top5=88.9%**。
- 教师事件占比：Qp 271,800 vs Qg 12,400（500k）→ **Qp占全部教师事件95.6%**。

## 5. 语义推论（对K轴设计直接有用）

1. **非KEEP动作被选中 ⟹ 档案≥2**（DIRECTIONAL/EPSILON/COMPLEMENTARY仅在|E|>1时存在）
   —— 动作分布即池可用的下界证据；
2. **KEEP候选天然单例**（当前领导唯一）——K对KEEP无作用空间，合同必须显式豁免；
3. **mask去重以K=1规范候选为基准**才可能跨K不变（否则K会改变动作合法性，污染动作层RNG）；
4. **奖励与leader解耦**（§3.5）——K只通过CFVF子代间接影响奖励，单一变量边界清晰；
5. **档案规模是实例与预算的函数**（hard 50k可达5；20k≤2）——K=3/4的池差异在20k工程门
   不可现场观测，需单测+更大预算遥测（B1预注册必须登记此限制）；
6. **身份集中度真实存在且Qp是主要来源**（top5=54.7%，Qp占95.6%事件）——
   AGENTS §21.1登记的"teacher identity concentration→重复暴露→CFVF放大→覆盖收缩"
   候选链在Qp侧有可注入的杠杆点，与已关闭的Qg侧（结构性无杠杆1.12%）不同。
