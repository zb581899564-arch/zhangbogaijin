# V35 A2/A4 确认后的执行路线

状态：`ALL_FINAL_CANDIDATES_NOT_PROMOTED; A4_NOT_PROMOTED + A2_NOT_PROMOTED (2026-08-25)`  
上游硬门：[A2 与 A4 多实例确认协议](V35_A2_A4_MULTISCALE_CONFIRMATION_PROTOCOL.md)

## 一条先验原则

先确认候选算法能否跨实例成立，再冻结和扩展实验。不得为了维护 A4 而先改 Qp、PDDR、档案、CA-TA-Lite、Pacing 或局部搜索顺序。

```text
A2/A4 60-run held-out confirmation
        |
        +-- A4_NOT_PROMOTED --> A2/A0 independent final-candidate confirmation; no rescue tuning
        |
        +-- A4_FINAL_CANDIDATE_CONFIRMED
               --> final freeze
               --> production preflight
               --> host throughput benchmark
               --> user approval
               --> A0-A4 master raw campaign
```

## 分支 A：A4 通过

### A.1 Final Freeze

不重新开发算法。以当前冻结 Jar、配置注册表和相同源码快照升格，产生新的 Final manifest，锁定：Jar SHA-256、A0--A4 canonical text/hash、FM3 参数、SUT、`ShiftMode=NONE`、`GLOBAL_ORIGINAL`、`[20,40,20,20]`、P=5/G=5、rho=0、45实例/20 seed、初群生成规则与输出 schema。

确认集的 60 条运行只用于候选判定，不进入最终正式 PFref 或论文统计。

### A.2 Production preflight

只验证执行链：代表性 20/50/100-job 短程运行、至少一条或两条完整 500k、raw front 保存、内存与日志、失败 attempt 处理，以及 A0--A4 同组公平审计。若 Jar、JVM 或宿主机条件相对于已有验收改变，必须重新做；完全相同的既有 Gate3/吞吐证据可作为历史依据但不能跳过当前 provenance 检查。

### A.3 服务器吞吐 benchmark

在训练机本地调度器测试 `4/8/12/16` 个独立 JVM。目标是最大化 `completed FE / wall-clock hour`，而不是单条任务的最短时间。评估 CPU、内存、GC 与 I/O，冻结 `FORMAL_MAX_PARALLEL`。SSH 只负责启动和检查，不能代替本地 scheduler。

### A.4 条件性 Master campaign

只有用户批准后启动：

```text
A0,A1,A2,A3,A4 x 45 instances x 20 seeds = 4500 raw runs
```

A0/A4 一次运行同时服务于主候选对比与渐进消融；不得先跑消融再重复跑 A0/A4。正式开始后不能因某实例表现不佳调整参数。此前 12 个 Stage2 公平组仅为先导，不能混入正式统计。

## 分支 B：A4 未通过

`A2/A0 主候选确认`已经完成，60条运行及30/30公平配对全部有效，但A2相对A0的总体`ΔCmax=-0.7410%`，
且`100_8_3_1`以`ΔHV=-22.3210%`、`ΔIGD=-40.2505%`触发100-job否决门。因此A2同样不能成为Final；
唯一裁决见
[`A2_NOT_PROMOTED_DECISION.md`](evidence/V35-A2-FINAL-CANDIDATE-CONFIRMATION/05-decision/A2_NOT_PROMOTED_DECISION.md)。

当前没有获准的主搜索Final候选。停止Final freeze、production preflight、吞吐benchmark和所有正式矩阵；不以
调Qp、PDDR、CA-TA或局部搜索来挽救。禁止把A2+CA-TA伪造为合法2×2因果臂；后续任何研究必须另行作出研究
决策、预注册并使用新实例/seed。在此分支中不得自动启动旧4500矩阵。

已发生的裁决：60条预注册确认运行全部有效，整体中位`ΔHV=+1.50%`、`ΔIGD=+7.24%`、
`ΔCmax=+1.72%`，但`100_5_3_1`的中位`ΔHV=-12.96%`、`ΔIGD=-76.31%`，并使100-job pooled
HV/IGD门失败。该事实触发预注册否决，不允许以总体正信号覆盖规模层级风险。唯一当前证据为
`docs/evidence/V35-A2-A4-MULTIINSTANCE-CONFIRMATION/06-remote-analysis-import/`。

## 与主算法选择分离的工作

### 外部算法

在 Final candidate 冻结后、且不污染 A0--A4 主矩阵的前提下，选择 2--4 个来源可审计的外部多目标/调度算法。共享实例、四向量表示、FM3 Decoder、三目标、预算、seed 和初群规则；保持各自的搜索更新、选择、档案与局部搜索。所有正式外部算法完成后再合并 PFref 并重算指标。

### FM3 模型实验

单独验证 `FM0 -> FM1 -> FM2 -> FM3` 对调度和人因指标的影响，固定搜索算法并报告 `Cmax/TEC/TWC`、Fmax/Favg、恢复时间和参数敏感性。A4 对 A0 的结果不能用于声称疲劳模型本身合理。

## 完成所有 raw runs 后的分析

每个实例聚合所有正式算法、全部正式 run 的 raw nondominated solutions，构造 pooled empirical reference front，统一理想/归一化边界和 HV reference point 后再重算 HV、IGD/IGD+、Spacing、双向 C-metric、三目标极值、front size、runtime 与 FE utilization。

统计时先在每个 instance 内计算每算法 20 seed 的中位数，再把 45 个 instance 作为主要配对单位。两算法使用 paired Wilcoxon 和效应量；多算法使用 Friedman + Holm。不得把同一实例的 20 个 seed 误作为 20 个独立调度问题。

## 论文叙事纪律

最终叙事由正式结果决定：FM3 是模型贡献；CFVF/DSCR 与认知—社会全向量搜索是搜索贡献；CA-TA/Pacing 是预算感知局部开发贡献。当前 A3 的独立正贡献尚未得到支持，除非正式消融改变该事实，否则不能把个人档案、Qp 或双Q冻结写成独立正增量。
