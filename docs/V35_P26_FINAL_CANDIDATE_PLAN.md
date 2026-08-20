# v3.5-Final Candidate（V35-FC）研发方案

- 建立日期：2026-08-17
- 决策记录：`docs/ROADMAP.md` D-082；执行纪律：`AGENTS.md` 第 16 节
- 命名说明：本方案原文称 P26-x；因 `V35-P26..P28` 已分别用于统一 reference、论文图表与最终验收，工作包登记为 **`V35-FC-0..FC-9`**（Final Candidate）。
- 状态：`planning_completed`；FC-0 为下一可申请工作包（须用户批准）；`formal_matrix_started=false`。

---

## 1. 总原则

一句话核心原则：

> **保留三项创新，不增加第四项创新；从"堆机制"转向"提高每一次 FE 的有效性"。**

判定：算法思想不是完全不行，而是**搜索节奏、信用利用和 Cmax 链路尚未调顺**。

职责分工固定（论文叙事与代码职责均不得混淆）：

- FM3 告诉算法"真实调度是什么"；
- Qp/Qg + CFVF 负责"全局往哪里学"；
- CA-TA 负责"什么时候值得深挖、用什么邻域深挖"；
- Cmax 审计阻止再靠猜测修改算法。

三创新最终表述：

1. **Dynamic-fatigue-aware deterministic dual-resource decoding**（FM3，含 FM3-consistent 关键结构只读接口）；
2. **Cognitive-social full-vector dual-Q search**（Qp+Qg+DSCR+CFVF，贡献门控分块协调）；
3. **Budget-aware Test-and-Apply five-macro VNS**（阶段性 local FE 配额 × 低成本邻域试探 × 优秀邻域强化利用 × FM3 关键结构候选生成）。

## 2. 决策依据（实验事实链）

| 事实 | 证据 |
|---|---|
| 忠实口径 50k 下 A4 相对 QGS：HV +38.6%、IGD −35.7%（5/5 seed） | `V35-P25E-corrected-comparison/5seed-50k/` |
| LS=30 在 50k 吞 79% FE，仅 2 个外层循环；LS 30→2：HV +16~18%、IGD −27~31%（5/5 seed） | `budget-pacing/`（单变量 + 5seed-confirm） |
| 500k 论文口径：A4 与 QGS HV 均势（2:2），IGD/minTEC 4/4 领先，minCmax 2/4；预算结构相同（18 外层、local 81% vs 82%） | `500k-pair/P25E_500K_A4_VS_QGS.md` |
| dualQ 冻结使 Qg TD 学习量只有 QGS 的 56%（2000 vs 3600） | 同上机制对比表 |
| gb15 单实例全面改善但 100-job 崩塌（HV −11.4%、IGD +32.1%）→ 不转正 | `dualq-multi-instance/MULTI_INSTANCE_REPORT.md` |
| 严格瓶颈分类 held-out 门失败（覆盖率 33%、漏失 41%）→ BAL 全开放 | `V35-P25B-pressure-diagnosis/` |
| P25D 增强版对比污染 → 永久封死；P25E 忠实适配为唯一公平口径 | `V35-P25D-all-algorithms-50k-pilot/LEGACY_EXCLUSION.md` |

## 3. 工作包定义

### FC-0：A4-PREFINAL 存档

- **目标**：把当前 A4-gb5+LS30 正式配置物理归档为回退版本。
- **实施**：物化 FREEZE_MANIFEST（正式配置 canonicalText + 哈希、dualQ blockFrozen(0.10,5,5)、Table 9 参数、FM3/单族/序列无关/NONE 语义）+ source-sha256.csv（v35/zhangbo 生产源码）+ environment；2000 FE 重放一致门（固定 seed 三次重放 front 逐位一致）。
- **验收**：幂等契约（磁盘比对）；此后任何机制改动不得直接改生产默认，一律走 FC 包。
- **证据**：`docs/evidence/V35-P26/00_prefinal-archive/`。

### FC-1：FM3 一致关键结构

- **语义**：创新 1 向创新 3 提供调度信息的唯一正式接口。关键路径/关键块必须用 Decoder 实际得到的 actual start / actual completion / fatigue-adjusted actual processing time / 当前正式 setup 时间构造（`PT=q(F)·PT0` 之后的世界），不得再用基础时间 `PT0` 的 proxy。**只读真实 schedule，不写 start time**——绝不回到 FCLS/FCRS。
- **代码落点**（现状已确认）：
  - `V35MacroCandidateGateway.prepare(...)`（`jmetal-algorithm/.../v35/V35MacroCandidateGateway.java:75-106`）当前只接收静态实例数据 `ZhangBoFatigueInstanceData`，不接收评估结果；
  - N3 `structuralRelocation`（158-183 行）与 N4 `selectedResourcePosition`（220-236 行）当前用 `getStandardSetupTime/getStandardTime`（PT0 proxy）选关键作业/瓶颈位置；
  - O10 已有同源实现可复用：`ZhangBoNeighborhoodSuite.criticalBlock`（260-293 行）经 `ZhangBoCriticalDagAnalyzer.analyze`（28-81 行）读 FM3 实际轨迹（start/end/actualDuration、零时差关键块）。
- **实施**：`prepare` 增加 `ZhangBoFatigueEvaluationResult` 参数；N3/N4 内部路由改读真实轨迹（关键块 → N3 候选生成；瓶颈机器/工人 → N4 候选生成），复用 `ZhangBoCriticalDagAnalyzer`；prepare 在候选生成后仍走完整 FM3 重放评价。
- **验收**：I1 + `20_2_3_1` 2000 FE 语义审计（N3 候选确实来自疲劳调整后关键结构，逐候选登记来源）；四向量合法性 100%；FE 闭合；回归全绿。
- **先检查后改造**：第一刀是语义检查（audit 现状 N3/N4 命中率与 O10 关键块重合度），确认 PT0 proxy 与真实关键结构的偏差量级后再改。

### FC-2：Dynamic Local-FE Pacing（删除固定 LS_Times=30 的资源控制地位）

- **语义**：A4 的局部搜索强度不再由固定调用次数控制，而由**当前搜索进度的 FE 配额**控制：

  `u = FE / MaxFEs`，`β(u) = βmin + (βmax − βmin)·u²`（第一版候选 βmin=0.25、βmax=0.65，实验冻结）

  每个外层循环：设全局阶段消耗 `B_G` 个 FE，则本循环局部搜索硬预算

  `B_L = ⌊ β(u)/(1−β(u)) · B_G ⌋`，使 `B_L/(B_G+B_L) ≈ β(u)`

  inter-factory LS 与 CA-TA 共享 `B_L` 硬预算，谁都不能突破。前期 β≈25%（先把 Pareto Front 铺起来），后期最多 65%（充分 exploitation），不再达到现在动辄 79–82% 的 local 占比（除非以后实验证明需要）。50k 与 500k 天然使用同一套算法，只看 FE/MaxFEs。
- **代码落点**：
  - `ZhangBoMOHPSOQ.java:4702`：LS pass 循环 `for (pass < getLocalSearchTimes() && !budgetExhausted)` 改为预算驱动（budgetExhausted 判定用 `B_L` 而非仅 maxIterations）；
  - `ZhangBoMOHPSOQ.java:540-542`：外层进入条件补局部预算预留逻辑；
  - inter-factory swap/insert（4651-4657、4677-4683 行）与 `runV35CaTaLiteLocalSearch`（4327-4479 行）的 FE 计入同一 `B_L`；
  - 配置：`V35ProductionConfiguration` 新增可选 `localFeBudget`（null=旧 LS_Times 语义，默认不变、canonicalText/哈希不变）；`LS_Times=30` 保留为 A0/QGS-F 基线参数。
- **实验**：`A4_old`(LS30) vs `A4_pace`：第一轮 10/20/50/100_2_3_1 × 50k × 3 paired seed（screening）；pacing 方向有效 → 500k × 3 paired seed。特别盯 100-job。
- **否决判据**：100-job 中位 HV −5% 或中位 IGD +10% 即否决（gb15 式大规模崩塌直接否决，不因 20-job 好看转正）。
- **为什么控制 FE 而非 LS 次数**：LS=10 在 10-job/100-job 及不同邻域上的实际评价代价不同；真正公平的资源单位是 FE。

### FC-3：Cheap-Test CA-TA

- **语义**：Test 真正 Lite：N1–N5 每宏邻域只产生 **1 个正式候选**（5 FE）；按原字典序（Accepted Success ↓ QGain ↓ Cost ↓ 确定性 tie-break）选 winner；仅当第一名与第二名几乎无法区分时 Top-2 one-extra-probe（各 +1 FE）。一次完整 Test 约 5~7 FE。Apply 阶段胜者 `N*` 连续获得后续局部预算（成功不回 N1）；仅连续失败 ≥ h 或 Apply 配额耗尽才 Re-test。硬规则：`FE_Test ≤ 20%·FE_local`——已达 20% 则不允许再昂贵 Re-test，优先当前 winner 或确定性 fallback。
- **现状与落点**：当前 `V35CaTaLiteController.decide`（82-126 行）nTest=1、multiplier=1，Test 轮已是一次返回整个 mask（K 个候选各 1 FE，`V35MacroCandidateGateway.evaluateOne` 恒 1 FE）——已接近 Lite 目标。本包补齐：(a) Top-2 难分加探（`beginTestEpoch` 207-217 行评分逻辑 + tie 判定阈值）；(b) `FE_Test ≤ 20%·FE_local` 硬门（Re-test 触发处，108-113 行）；(c) Apply 胜者持续语义确认（不成功即回 N1 的行为不得存在）。
- **验收**：Test FE 占比统计（对照 ≤20% 门）；20_2_3_1 50k × 3 paired seed A/B：Test FE 显著降低且 HV/IGD 不退。

### FC-4：贡献门控软冻结双 Q

- **语义**：双 Q 从"硬冻结"升级为"贡献门控软冻结"。P/G 块（B=5）与 10% 预热保留；**控制权仍分块**（论文叙事 block-coordinated cognitive-social dual Q-learning 不变）。变化只在冻结的定义：

  - P 块：Qp 主控制器（正常 ε-greedy、正常 α、正常 TD）；Qg 辅助——照常按当前状态查表选实际 gbest，但 **ε=0 纯 greedy**，学习率 `α_g^off = ρα·I_g^contrib`；
  - G 块：完全对称（Qg 正常；Qp ε=0、`α_p^off = ρα·I_p^contrib`）；
  - **I^contrib 定义**（贡献门控的严谨性所在）：本轮该粒子**至少实际执行了一个 gbest-derived（GBEST 或 BOTH 来源）CFVF 资源动作**则 I=1，否则 0。若 CFVF 因采样/冲突/合法性/裁剪没有执行任何 gbest 动作，这次 offspring 根本没有受到 gbest 的有效结构影响，不允许拿 reward 更新 Qg。
  - 效果：P 块 = Qp 主学习 + Qg 小幅吸收**有效**经验；G 块对称。相对纯 soft freeze（`α'=0.3α` 无条件）严谨得多。
- **代码落点**：I_contrib 直接复用现有机制——`ZhangBoCfvfUpdater`（81-92 行）已在动作实际写入 offspring 时计数 `gbestInherited++`（GBEST 与 BOTH 均计入），每粒子每代的 `ZhangBoCfvfDiagnostics.gbestInherited>0` 即 I_g=1；`gbestInherited`/`pbestInherited` 对称可得 I_p。新增：(a) 学习门控：冻结方 TD 提交处按 `ρα·I_contrib` 缩放（Qg/Qp 批量 TD 提交点）；(b) 冻结方动作选择 ε=0（`ZhangBoGlobalSearchConfiguration` 的探索率读取处按块相位门控）；(c) `ZhangBoDualQCoordinationConfiguration` 新增 `softFreezeRho`（默认 0.0 = 硬冻结，canonicalText 兼容）。
- **参数校准**：ρ ∈ {0, 0.1, 0.2, 0.3}，ρ=0 即当前硬冻结。**不做 gb10/15/20**（已被多实例实验判死）。
- **单变量纪律（强制）**：必须在 FC-2 稳定版本上测试——`A4+Pacing` vs `A4+Pacing+SoftFreeze`，否则无法区分 pacing 与 soft-Q 的贡献。
- **实验**：先只跑 `20_2_3_1`（gb15 获益明显处）和 `100_2_3_1`（gb15 崩掉处）× 500k × 3 paired seed。转正条件：20 工件不比硬冻结差、100 工件无 diversity collapse、Qg TD 有效增加、HV/IGD 不退化、Cmax 改善——全部满足才进 10/50 确认。
- **回退**：都不行则**删除软冻结，继续 P5/G5 硬冻结**。这一点必须有勇气。

### FC-5：Cmax Audit + CFVF GIR 审计（只观察，不改算法）

- **纪律**：v3.5-Final **不预设 Cmax 为什么弱**。先启动四段审计 `Generation → Admission → Survival → Exploitation`，按证据只走一个分支。
- **实施**：
  - 四段审计：复用/扩展 V35-P19 Cmax 生命周期审计与 P9.1 只观察审计（均为已验证旁路），补齐 Admission（局部接受拒绝率）与 Exploitation（教师使用覆盖）分段字段；行为哈希不变门（挂/不挂逐位一致）。
  - **GIR 审计**：`GIR_{JS,FA,MA,WA} × {pbest,gbest} × {G1,G2,G3,G4}` 八组基因继承率。现状 `ZhangBoCfvfDiagnostics.sourceCounts` 是全局粒度，需按子群与向量维度拆分（`ZhangBoResourceAction` 已携带向量与 Source，扩展计数键即可）。
  - **RecordContribution**：新 Cmax record 出现之前，究竟是哪类 CFVF 修改（向量×来源×子群）起主要作用——每条新 record 回看其谱系最近 k 次 CFVF 修改并归因。
- **产物**：审计母表 CSV + 判定报告（四段断点定位 + GIR 分布 + RecordContribution 归因）。
- **意义**：以后数据若显示如 `G1: JS=45%, FA=30%, MA=20%, WA=5%`，才有证据做 G1-specific vector intensity；Sparse CFVF（规模相关修改上限，n↑ 时修改比例下降）同为**条件分支**，仅当 100-job audit 证明 CFVF 单次改太多导致 diversity 坍缩才启用。

### FC-6：Cmax 修复分支（按审计只选一支）

| 分支 | 审计证据 | 修复动作 | 明确不动 |
|---|---|---|---|
| A: Generation Gap | 根本产生不了好 Cmax | 查 `G1 Qg/Qp → CFVF → N3/O10` 链，看 RecordContribution 哪类修改最易产生新 record，只强化该环节 | 不碰 PDDR |
| B: Admission Gap | 好 Cmax 产生了但被局部接受拒绝 | G1 directional acceptance | 不动 DSCR |
| C: Survival Gap | 好解进 merge pool 却死在 PDDR | **四方向对称精英保留**：`e1=argmin Cmax`、`e2=argmin TEC`、`e3=argmin TWC`、`e4=argmin φ4`，去重后先保住方向锚点再 PDDR 填充——不是特权保护 Cmax，四方向完全对称 | 不引入非对称特权 |
| D: Exploitation Gap | 好解活着进 archive 但没人学 | G1 teacher exposure（归 **Qg** 修，如 G1 current extreme coverage） | 不污染 DSCR 职责 |

- **纪律**：不允许同时加三种修复；每支实施后独立验收（paired seed + 行为哈希可控变化登记）。

### FC-7：最终消融

- DSCR D0/D1（开/关）+ CFVF/Qp/CA-TA + `localFeBudget`（FC-2）/`softFreezeRho`（FC-4）开关，对齐 EXP-5 合法依赖链（`caTaLite ⇒ qp ∧ dscr` 等既有偏序继续成立）。
- 决定各模块是否真的保留：任何模块在消融中无正贡献即按证据降级/移除，不得"因为实现了所以保留"。

### FC-8：四规模 Champion Gate（= EXP-1 主版本冻结门）

- **设计**：10/20/50/100 × {50k, 500k} × 5 paired seed，比较 9 算法：Final A4、HMOPSO-QGS-F、HMOPSO-QLS-F、MOPSO-F、MOPSODS-DE-F、MOHEADE-F、NSGA-II-F、SPEA2-F、QMOEA（来源验证完成后）。
- **硬目标**：HV/IGD 为主要总体第一梯队（最好总体第一）；Cmax 不再是结构性短板；100-job 无明显退化。
- **公平边界**：P25E 原则永远不能破——baseline 只允许问题接口适配，不允许搜索机制增强；P25D 那条路永久封死。
- 四个规模、500k 下都稳定 → 才启动 45×20；否则**不烧算力**。

### FC-9：45×20 正式矩阵启动门

- FC-8 通过后授权 EXP-3（对应 V35-P25）：45 实例 × 20 seed 正式矩阵，协议完全按 `V35_FORMAL_EXPERIMENT_ROADMAP.md` 执行（统一 reference 一次构造、Wilcoxon signed-rank + Holm、达到面等）。

## 4. 最终候选算法结构（一张图）

```text
Population(t)
  → FM3 deterministic decode
  → G1/G2/G3/G4
  → freeze social snapshot
  → DSCR（只维护 stale social cache）
  → Qp + Qg（P5/G5 分块：主控制器正常学习；辅控制器 ε=0 greedy + 贡献门控软 TD ρα·I_contrib）
  → CFVF（四向量认知—社会全向量飞行）
  → FM3 decode → Q reward → Personal Archive
  → 计算当前 local FE 配额 β(FE/MaxFEs)，B_L=⌊β/(1−β)·B_G⌋
  → Inter-factory LS（共享 B_L）
  → BAL-open N1–N5 CA-TA（Cheap Test 5~7 FE，FE_Test≤20%·FE_local；Apply winner 连续深挖）
  → Merge Pool → PDDR-FF
  → Archive / lineage / passive audit
  → Population(t+1)
```

FM3 真实关键结构持续提供给 N3（及 O10 语义链）；DSCR 继续保留但**绝不扩权**（不把当前 minCmax 塞进候选教师——A5 已证明简单增强方向教师池未必有效；Cmax teacher coverage 有问题修 Qg，不污染 DSCR）。

## 5. 禁区清单（永久）

1. 不继续 gb10/15/20；
2. 不重新上严格 SEQ/MAC/WOR/FAT mask；
3. 不重新启用 FCLS/FCRS；
4. 不给 DSCR 强塞 minCmax leader；
5. 不人为修改 CA-TA 的 cost credit 来救 Cmax；
6. 不现在拍脑袋规定 JS 弱、MA/WA 强（先 GIR 审计）；
7. 不给 baseline 使用任何 v3.5 增强组件；
8. 不在算法还没最终冻结时启动 45×20。

## 6. 风险与回退

- 每个机制包失败即回退到上一稳定版本；全流水线失败时回退版本 = FC-0 的 A4-PREFINAL 存档（gb5+LS30，其 500k 表现：HV 均势、IGD/TEC 4/4 领先——即使零改进，也有一份可辩护的论文底线）。
- 所有诊断运行不进正式 reference；每包 paired seed + 共同初始种群 + P8MetricCalculator 口径；行为哈希可控变化逐包登记。
- 外部方法学定位（如实表述）：PSO 自适应参数与多算子优化的 adaptive operator selection 文献只能证明"动态协调搜索强度"方向合理，**不能证明本项目具体 (ρ, β) 参数正确**——参数必须由本流水线实验冻结。
