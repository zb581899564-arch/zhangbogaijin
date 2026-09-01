# 算法 A（HMOPSO-QGS-F 公平适配基线）与算法 B（张博改进算法正式版 ZhangBo A4）逐流程代码级对比与 Makespan ($C_{\max}$) 差异机制分析报告

---

# 1. 对比对象与代码版本

本报告针对当前项目中的两个核心算法版本，进行严格的代码级逐流程追踪、执行时点对照与 $C_{\max}$（Makespan，最大完工时间）差异因果链剖析：

- **算法 A（Baseline）**：**HMOPSO-QGS-F 公平适配基线**
  - **运行模式**：`V35FairRunner.Mode.V35_BASELINE` / `Algorithm.HMOPSO_QGS_F`
  - **代码入口**：`org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ`（绑定 `formal-hmopso-qgs-v1` 运行时配置，`dscr=false, cfvf=false, qp=false, caTaLite=false, evaluatedPddr=false`）
  - **版本定性说明**：此版本并非李明哲论文原版带微调/右移/无疲劳的旧 Java 工程（`author_actual` 缺陷版本），而是李明哲 HMOPSO-QGS 在当前标准化生产环境下的**公平适配基线（`deterministic_canonical`）**。它与算法 B 处于完全相同的物理环境与模型约束之下。
  - **研发状态**：`engineering_validated=true`, `algorithm_aligned=true`, `sampled_reproduction_accepted=true`, `full_reproduction_accepted=false`（等待正式 45×20 矩阵）。
- **算法 B（ZhangBo A4 / A4-Pacing）**：**张博综合改进算法当前正式版本**
  - **运行模式**：`V35FairRunner.Mode.V35_FULL_POOL_OFF` / `Algorithm.ZHANGBO_A4`
  - **代码入口**：`org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ`（启用 `FM3` 动态疲劳解码、全向量 `CFVF`、`DSCR` 教师清洗、谱系档案 `Q-pbest`、`Budget-Aware CA-TA-Lite` 宏邻域与 `Local-FE Pacing` 动态配额，`evaluatedPddr=true`）
  - **研发状态**：`engineering_validated=true`, `algorithm_aligned=true`, `sampled_reproduction_accepted=in_progress`, `full_reproduction_accepted=false`（FC 候选阶段）。

### 严格的公平比较边界（已代码级锁死）
两个算法在所有正式对比实验中完全共享以下条件：
1. **测试算例与疲劳参数**：完全共享 `EADHFSP` 标准化扩展算例与确定性疲劳参数集（$\lambda, \mu, r, F_{\text{warn}}, F_{\text{safe}}$）。*注：疲劳参数为离散制造调度中的确定性计算抽象，用于构建标准化实验场景，不代表真实工人的生理参数*；
2. **问题与解码器**：共享 `ZhangBoCanonicalProductionProblem`，解码器固定为显式 `FM3`（两阶段工时非线性反馈与自然恢复模型），单产品族占位（`DEGENERATE_SINGLE_FAMILY`），序列无关设置时间（`SEQUENCE_INDEPENDENT`），移位机制完全冻结（`ShiftMode.NONE`）；
3. **非线性对数疲劳工时计算公式**（`ZhangBoFatigueModel.java:20`）：
   $$q(F) = 1 + \frac{r}{\ln 2}\ln(1+F),\quad PT = q(F) \cdot PT^0,\quad SET = q(F) \cdot SET^0$$
4. **初始种群**：同一 seed 下共享逐位哈希一致的初始四向量种群（`initialPopulationHash` 严格相同）；
5. **算力预算**：精确闭合于 500,000 FE，种群规模 $N=100$，三主优化目标固定为 $[0, 1, 6]$ 即 $(C_{\max}, TEC, TWC)$。

---

# 2. Algorithm A 完整流程

算法 A（HMOPSO-QGS-F 公平基线）的单次完整运行流程如下：

```text
Step A1: 注入初始种群并进行四向量评估
  ↓ (调用 FM3 解码器评估 N=100 个由 JS/FA/MA/WA 组成的四向量染色体，消耗 100 FE)
Step A2: 初始化粒子记忆与初始 Pareto 集合
  ↓ (初始化粒子个人历史 tempSwarm、计算初始非支配集 globallyOptimalIndividual)
Step A3: 外循环开始（判断剩余 FE 是否满足一次完整的 Q-phase 预算，即 50×100=5000 FE）
  ↓
Step A4: 种群分群（updateVelocity 贪心切分 4 个物理槽位）
  ↓ [Slot 1(G1_CMAX)=15, Slot 2(G4_BALANCED)=55, Slot 3(G2_TEC)=15, Slot 4(G3_TWC)=15]
Step A5: Q 学习社会领导者选择（Qg）
  ↓ (按当前状态从缓存中选择 gbest，未开启 DSCR，允许存在被严格支配的过期教师)
Step A6: 粒子更新（updatePosition）
  ↓ (采用原算法机制：JS 交换序列更新 + 资源层常规交叉与独立变异，未实现全向量显式联合引导)
Step A7: 全局后代评估（evaluateSwarm，消耗 100 FE）
  ↓
Step A8: 结算 Qg 奖励并进行 Q 表 TD 更新
  ↓
Step A9: 更新个人历史与非支配记忆
  ↓ (将当前后代压入 tempSwarm[k]，按三目标严格 Pareto 非支配规则截断)
Step A10: Q-round 循环（重复 Step A4~A9 共 Q_Times=50 轮，累计消耗 5000 FE）
  ↓
Step A11: 继承局部搜索（runFormalInheritedLocalSearch）
  ↓ (固定 LS_Times=30，依次对各子群执行关键工厂交换/插入及原版 O1~O9 算子，无动态预算硬顶)
Step A12: 全局 PDDR 环境选择（updateLeaders / applyEvaluatedPddr）
  ↓ (对本轮生成的所有后代与父代执行单标尺全局 PDDR 评分截断，直接保留前 100 席)
Step A13: 外部非支配档案增量更新（updateParticlesMemory）
  ↓ (将种群中的非支配解增量合并至 globallyOptimalIndividual)
Step A14: 判断终止条件（FE >= 500,000），若是则输出 final front，否则跳至 Step A3。
```

---

# 3. Algorithm B 完整流程

算法 B（张博改进算法正式版 ZhangBo A4）的单次完整运行流程如下：

```text
Step B1: 注入完全相同的初始种群并进行四向量评估
  ↓ (共享初始种群哈希，FM3 解码评估三目标，消耗 100 FE)
Step B2: 记忆初始化与谱系档案建立（zhangBoLineageCoordinator）
  ↓ (建立 100 个粒子的独立谱系，初始化容量 L=6 的个人非支配档案与冻结边界)
Step B3: 外循环开始（FC-5 Cmax 生命周期审计 beginCycle 与 per-cycle 模块耗时打点）
  ↓
Step B4: 种群分群（updateVelocity 贪心切分 4 个物理槽位）
  ↓ [Slot 1(G1_CMAX)=15, Slot 2(G4_BALANCED)=55, Slot 3(G2_TEC)=15, Slot 4(G3_TWC)=15]
Step B5: DSCR 教师缓存清洗与社会领导选择（Qg）
  ↓ (清除 previous/historical 缓存中被严格支配的陈旧教师，保证 gbest 纯净度)
Step B6: 认知领导选择（Qp，基于谱系档案与 16 状态 Q 表决策）
  ↓ (从个人谱系档案 L=6 中按 Cmax 方向锚点、收敛锚点或互补锚点选择 pbest)
Step B7: 全向量 CFVF 联合更新（updatePosition）
  ↓ (JS 交换序列 + 第一阶段显式 MA/WA 资源层 FMW/MW/M/W 联合演化，按工件身份重新定位)
Step B8: 全局后代 FM3 解码评估（evaluateSwarm，消耗 100 FE）
  ↓
Step B9: 双 Q 奖励结算与 TD 联合更新（settleOriginalQg + settleQp，分块冻结模式）
  ↓
Step B10: 更新粒子个人历史与谱系档案（zhangBoLineageCoordinator）
  ↓
Step B11: Q-round 循环（重复 Step B4~B10 共 50 轮，累计消耗 5000 FE）
  ↓
Step B12: 开启 Local-FE Pacing 动态预算配额窗口（beginLocalFeBudgetWindow）
  ↓ [B_L = ⌊β(u)/(1-β(u)) · B_G⌋，调度参数 β(u) 随进度由 βmin=0.25 动态升至 βmax=0.65]
Step B13: 真实代码调用第 1 步：CA-TA-Lite 宏邻域搜索（runV35CaTaLiteLocalSearch）
  ↓ (根据 (subSwarm, bottleneck) 上下文自适应调用 N1~N5 宏邻域，产生局部候选并预评价)
Step B14: 真实代码调用第 2 步：继承局部搜索（runFormalInheritedLocalSearch）
  ↓ (执行关键工厂工件重排，与 CA-TA-Lite 共享本轮 Local-FE Pacing 硬配额)
Step B15: 合并候选池物化（applyEvaluatedPddr）
  ↓ [Merge Pool = 100 父代 + 100 全局后代 + CA-TA 局部后代，池规模约 250~300]
Step B16: 评价后全局 PDDR 环境选择（zhangBoEvaluatedPddrSelector.select）
  ↓ (按全局 PDDR score = q + 1/(p+1) 严格排序取前 100 席，未入选解被淘汰)
Step B17: 谱系分裂、继承与淘汰（zhangBoLineageCoordinator.rebuild）
  ↓
Step B18: 外部非支配档案增量更新（updateParticlesMemory）
  ↓ (同步更新 globallyOptimalIndividual 与被动全生命周期观察档案)
Step B19: 判断终止条件（FE >= 500,000），若是则输出 final front，否则跳至 Step B3。
```

---

# 4. 两算法逐流程对齐总表

### 4.1 四子群“物理槽位”与“理论语义”严格对照表
为避免历史术语与代码槽位混淆，特建立物理实现与理论语义的唯一对照依据：

| 物理槽位 (Physical Slot) | 物理容量 (Size) | 语义枚举 (ZhangBoSubSwarm) | 目标职责 (Objective Index) | 理论语义名称 | 代码证据 |
|---|---:|---|---|---|---|
| **Slot 1 (groupU1)** | 15 | **`G1_CMAX`** | Index 0 ($C_{\max}$) | 完工时间极值群 | `ZhangBoSubSwarmSemantics.java:36` |
| **Slot 2 (groupC2)** | 55 | **`G4_BALANCED`** | Index -1 (综合 PDDR Score) | 中心综合平衡群 | `ZhangBoSubSwarmSemantics.java:36` |
| **Slot 3 (groupD3)** | 15 | **`G2_TEC`** | Index 1 ($TEC$) | 能源消耗极值群 | `ZhangBoSubSwarmSemantics.java:36` |
| **Slot 4 (groupUNew)** | 15 | **`G3_TWC`** | Index 6 ($TWC$) | 工人成本极值群 | `ZhangBoSubSwarmSemantics.java:36` |

> 📌 **核心说明**：
> 1. 代码中的物理数组顺序固定为 `[Slot 1, Slot 2, Slot 3, Slot 4]`（即 `[Cmax, Balanced, TEC, TWC]`）；
> 2. 算法内部所有高级组件（Qg/Qp 奖励、CA-TA 上下文路由、谱系档案投影）均已通过 `ZhangBoSubSwarmSemantics` 统一映射为标准理论语义（$G_1=C_{\max}, G_2=TEC, G_3=TWC, G_4=\text{Balanced}$）；
> 3. 在 FC-6A.2 审计报告中，`rejG2=0` 实质代表 **“Slot 2 对应的综合平衡群在全局 PDDR 竞争中 0 被拒”**。

### 4.2 两算法逐流程对比与对齐总表

> **图例说明**：
> - 🟢 `<span style="color:#228B22">相同</span>`：物理模型、解码环境与初始约束完全一致
> - 🔴 `<span style="color:#DC143C">不同</span>`：机制不同、分支控制不同
> - 🟠 `<span style="color:#FF8C00">重点差异 / 可能影响 Cmax</span>`：直接影响 Makespan 生成、存活与算力分配的核心差异
> - 🔵 `<span style="color:#1E90FF">工程等价 / 纯性能优化</span>`：算法逻辑等价，仅做运行时间优化与状态观察

| 序号 | 执行阶段 | 算法 A（HMOPSO-QGS-F 基线） | 算法 B（ZhangBo A4 正式版） | 状态标注 | 对 $C_{\max}$ 的潜在影响 | 代码证据（文件 / 类 / 方法 / 行号） |
|:---:|---|---|---|:---:|---|---|
| 1 | **初始种群生成** | 注入确定性初始四向量种群 | 注入完全相同的初始四向量种群 | <span style="color:#228B22">相同</span> | 无初始偏差（哈希逐位一致） | `V35FairRunner.java:157` |
| 2 | **目标解码模型** | FM3 两阶段工时疲劳非线性模型 | FM3 两阶段工时疲劳非线性模型 | <span style="color:#228B22">相同</span> | 评估环境完全相同 | `ZhangBoFatigueModel.java:20` |
| 3 | **四子群划分** | 贪心切分 15(Cmax)/55(综合)/15(TEC)/15(TWC) | 贪心切分 15(Cmax)/55(综合)/15(TEC)/15(TWC) | <span style="color:#228B22">相同</span> | 分群规则相同，但入选成员受前代环境选择影响 | `ZhangBoMOHPSOQ.java:1270-1360` |
| 4 | **教师缓存维护** | 直接使用缓存中的 gbest，未清洗严格支配解 | **DSCR 机制**：清洗缓存中被严格支配的陈旧教师 | <span style="color:#DC143C">不同</span> | 消除过期劣质教师对 G1 的误导 | `ZhangBoQgController.java:142` |
| 5 | **社会领导选择 (Qg)** | 原版 2 状态 Q-learning 决策 | 原版 2 状态 + 分块冻结（P/G-block=5） | <span style="color:#DC143C">不同</span> | 降低双 Q 同时更新时的非平稳性 | `ZhangBoQgController.java:210` |
| 6 | **认知领导选择 (Qp)** | 传统单粒子 pbest 标量记忆 | **Qp 谱系档案决策**：从 $L=6$ 档案中选择极值/收敛锚点 | <span style="color:#FF8C00">重点差异 / 可能影响 Cmax</span> | 允许粒子直接沿 $C_{\max}$ 极值方向进行认知学习 | `ZhangBoQpController.java:185` |
| 7 | **粒子位置更新** | JS 交换序列更新 + 资源层常规交叉/变异 | **全向量 CFVF**：JS/FA/MA/WA 四向量显式协同更新 | <span style="color:#FF8C00">重点差异 / 可能影响 Cmax</span> | **机制级影响**：协同排程与资源分派，降低瓶颈工序加工耗时 | `ZhangBoMOHPSOQ.java:656` |
| 8 | **全局后代评估** | Q-round 内每次更新后即时评估 | Q-round 内每次更新后即时评估 | <span style="color:#228B22">相同</span> | 评估时机与预算核算一致 | `ZhangBoMOHPSOQ.java:665` |
| 9 | **强化学习结算** | 仅结算 Qg 奖励 | 结算 Qg 奖励 + Qp 多目标与疲劳风险奖励 | <span style="color:#DC143C">不同</span> | 调节极值搜索与收敛强度的动态平衡 | `ZhangBoMOHPSOQ.java:687` |
| 10 | **局部搜索预算** | 固定每轮 30 次，无动态总预算约束 | **Local-FE Pacing**：$\beta \in [0.25, 0.65]$ 动态硬配额 | <span style="color:#FF8C00">重点差异 / 可能影响 Cmax</span> | **显著影响**：成熟期局部深挖算力大幅提升 | `ZhangBoMOHPSOQ.java:564` |
| 11 | **宏邻域局部搜索** | **无**（仅有原版 O1~O9 算子） | **CA-TA-Lite**：根据上下文自适应调用 N1~N5 宏邻域 | <span style="color:#FF8C00">重点差异 / 可能影响 Cmax</span> | **极显著影响**：定向针对关键路径与关键工厂重排 | `ZhangBoMOHPSOQ.java:735` |
| 12 | **继承局部搜索** | 顺序执行关键工厂工件重排与 O1~O9 | 顺序执行关键工厂工件重排（受 Pacing 预算硬约束） | <span style="color:#DC143C">不同</span> | 算法 B 在预算耗尽时安全截断 | `ZhangBoMOHPSOQ.java:742` |
| 13 | **局部搜索执行顺序** | 仅执行继承局部搜索 | **实际代码顺序：`CA-TA-Lite → Inherited LS`** | <span style="color:#DC143C">不同</span> | CA-TA-Lite 优先消耗配额并产生候选 | `ZhangBoMOHPSOQ.java:735-745` |
| 14 | **合并候选池构建** | 父代 (100) + 当代全局后代 (100) | 父代 (100) + 全局后代 (100) + **CA-TA 局部后代** | <span style="color:#FF8C00">重点差异 / 可能影响 Cmax</span> | 算法 B 池中汇集大量高质量 $C_{\max}$ 突破候选 | `ZhangBoMOHPSOQ.java:9092-9101` |
| 15 | **环境选择机制** | 原版全局单标尺 PDDR 评分截断 | 评价后全局单标尺 PDDR 评分截断（单遍双向加速） | <span style="color:#1E90FF">机制同构 / 工程等价</span> | 两者均采用 $\text{score} = q + \frac{1}{p+1}$ 全局排序 | `ZhangBoEvaluatedPddrSelector.java:160` |
| 16 | **极端解存活状态** | 候选池较小，挤压发生率相对较低 | **强生成侧注入大量综合解，极值解遭严重挤压** | <span style="color:#FF8C00">重点差异 / 可能影响 Cmax</span> | **核心痛点**：优质 $C_{\max}$ 解在选择阶段被误杀淘汰 | 见 FC-6A.1 与 FC-6A.2 审计实证 |
| 17 | **谱系记忆更新** | 仅维护简单的 $tempSwarm$ | **Lineage Coordinator** 维护谱系分裂、继承与删除 | <span style="color:#DC143C">不同</span> | 维系粒子在长周期搜索中的血统记忆 | `ZhangBoLineageCoordinator.java:112` |
| 18 | **外部非支配档案** | 增量 Pareto 档案过滤 | 增量 Pareto 档案过滤 + 被动全生命周期观察 | <span style="color:#1E90FF">工程等价 / 旁路观察</span> | 最终 Pareto 前沿提取逻辑一致 | `ZhangBoIncrementalParetoArchive.java:8606` |
| 19 | **终止与输出** | 精确 500,000 FE 闭合退出 | 精确 500,000 FE 闭合退出 | <span style="color:#228B22">相同</span> | 算力消耗严格一致 | `ZhangBoMOHPSOQ.java:603` |

---

# 5. 关键差异表

为清晰界定算法机理改动与运行效率优化，特将全部差异解耦为“核心算法语义差异”与“纯工程实现差异”两类：

### 5.1 核心算法语义差异（改变搜索分布与 $C_{\max}$ 解的产生/存活）

| 差异编号 | 算法阶段 | 算法 A（Baseline） | 算法 B（ZhangBo A4） | 差异本质 | 对 $C_{\max}$ 的影响程度 | 影响路径与因果链 |
|:---:|---|---|---|---|:---:|---|
| **D1** | **粒子更新** | JS 交换序 + 常规资源交叉变异 | **全向量 CFVF 联合演化**（JS/FA/MA/WA） | 显式多向量联合引导 | **S 级（高）** | 协同平衡机器负荷与工人疲劳累积，降低工序等待时间，提升极值解生成率。 |
| **D2** | **局部搜索** | 仅原版 O1~O9 算子 | **CA-TA-Lite 宏邻域（N1~N5）** | 上下文自适应深挖 | **S 级（高）** | 专门针对 Critical Path 与瓶颈工厂工序进行重排，高频产生低 $C_{\max}$ 候选解。 |
| **D3** | **候选池生态** | Merge Pool 较小（约 200 解） | Merge Pool 较大（约 250~300 解）且富含强非支配解 | 候选池竞争结构改变 | **S 级（高）** | 相同全局 PDDR 规则下，池中高 $p$ 综合解暴增，反向加剧了对低 $p$ 的 $C_{\max}$ 极值解的淘汰挤压。 |
| **D4** | **局部 FE 调度** | 局部搜索轮次恒定（30 次） | **Local-FE Pacing**（$\beta: 0.25 \to 0.65$） | 动态算力重新分配 | **A 级（中高）** | 成熟期局部深挖算力上限提升至 65%（全生命周期实际局部 FE 占比约 35%），强化局部收敛。 |
| **D5** | **认知引导** | 单一 pbest 标量记忆 | **Qp 谱系档案（$L=6$）锚点引导** | 认知学习多点化 | **A 级（中高）** | 允许粒子跳出退化的单点 pbest，直接朝向历史极值或收敛锚点演化。 |
| **D6** | **教师治理** | 允许历史缓存存在过期教师 | **DSCR 严格支配清洗**（$\text{DTUR}=0$） | 社会领导纯净化 | **B 级（中）** | 保证 G1 子群学习的 gbest 领导者未被严格劣质化。 |

### 5.2 纯工程实现差异（不改变解与 $C_{\max}$，仅影响 CPU 运行时间）

| 差异编号 | 所属模块 | 工程优化实现 | 机制说明 | 对 $C_{\max}$ 的影响 |
|:---:|---|---|---|:---:|
| **E1** | **支配判断** | 单遍双向支配扫掠 (`O(N^2/2)`) | 提取共享打分公式，一次比较同时结算双方支配状态 | <span style="color:#1E90FF">纯工程优化（零算法影响）</span> |
| **E2** | **工时缓存** | FM3 关键结构 DAG Memo 缓存 | 缓存未发生变化的静态工序工时，消除冗余计算 | <span style="color:#1E90FF">纯工程优化（零算法影响）</span> |
| **E3** | **内存管理** | 集合与数组对象复用 | 减少 GC 开销与频繁的 Deep Copy | <span style="color:#1E90FF">纯工程优化（零算法影响）</span> |
| **E4** | **耗时打点** | `V35ModuleTimer` 全流程纳秒打点 | 纯观察性耗时统计，完全脱离随机数与解操作逻辑 | <span style="color:#1E90FF">纯工程优化（零算法影响）</span> |

---

# 6. Makespan差异机制分析

在相同的 500,000 FE 与 FM3 疲劳环境下，算法 A 与算法 B 的 $C_{\max}$ 指标表现差异可由以下三层由浅入深的因果链进行严密解释：

```text
【第一层：生成侧分布不同（Generative Distribution）】
  算法 B (CFVF + CA-TA-Lite) 协同调整工件排队与工人配置
  ↓ 改变搜索空间覆盖与瓶颈路径缩减能力
  在搜索过程中高频产生极低 Makespan 的理论突破候选（如 seed22 产生 174.4367 解）
  
【第二层：算力分配节奏不同（Computational Pacing）】
  算法 B (Local-FE Pacing) 动态调度算力 (调度上限 β 随进化进度从 0.25 升至 0.65)
  ↓ 改变探索与挖掘比例（全生命周期实际局部 FE 占比约 35%）
  后期局部搜索强度显著增强，使优质 Cmax 解被挖掘得更深

【第三层：环境选择存活筛选不同（Environmental Selection Filter - 核心症结）】
  两算法均采用全局单标尺 PDDR 打分规则：score = q + 1/(p+1)
  ↓ 面对截然不同的候选池生态
  算法 B 的强生成侧向池中注入了超额的高 p 综合平衡解 (N_<1 > 100 概率达 53%~85%)
  ↓ 发生结构性挤兑淘汰
  Cmax 极值解因 p 值天然偏低 (score ≈ 1.0)，在全局单标尺下被高 p 综合解 100% 误杀切除！
```

### 关键证据与实证数据支撑：
1. **生成时点事实（FC-5.2 审计）**：
   - 算法 B 确实在运行中生成了极佳的 $C_{\max}$ 解（如 `20_2_3_1` seed22 的 **$C_{\max} = 174.4367$ 诞生于 $\text{FE} = 288,564$**，属于搜索中期的局部深挖产物；seed23 的 169.63 诞生于 $\text{FE} = 219,476$；seed24 的 191.21 诞生于 $\text{FE} = 496,557$）。这证实算法 B 的生成机制（CFVF + CA-TA-Lite）具备极高的极值探索效率。
2. **环境选择淘汰事实（FC-6A.1 & FC-6A.2 审计）**：
   - **成熟期常态挤兑**：BASE 20-job 中 85.5%、100-job 中 53.2% 的轮次出现中心强解超额（$N_{<1} > 100$）；
   - **174.44 反事实探针**：黄金解 174.44 在原版全局 PDDR 下的存活率仅为 **14.5%**（在 $\text{FE} > 70,000$ 后的成熟期连续数十轮被误杀淘汰）；而在 G1 区域分配机制下，其存活率达到 **100.0%**（提升 6.89 倍）；
   - **零综合解被拒**：在全 12 跑中，综合平衡群的被淘汰数恒为 **0**（`rejG2=0`），被全局 PDDR 切除的非支配解中 **72.2% ~ 84.4% 本来在 G1/G3/G4 方向配额中有空位**。

---

# 7. 差异嫌疑等级排序

| 等级 | 机制差异项 | 嫌疑原因与机制本质 | 代码证据 | 当前数据支撑状态 |
|:---:|---|---|---|---|
| **S 级**<br>(决定性影响) | **1. 全局单标尺 PDDR 环境选择淘汰** | 强生成侧产生的高 $p$ 综合解霸占全部席位，使优质 $C_{\max}$ 极值解在成熟期存活率暴跌至 14.5%。 | `ZhangBoEvaluatedPddrSelector.java:160` | **已证实**（FC-5.2 生命周期链、FC-6A.1 情况 C 判定、FC-6A.2 区域探针）。 |
| **S 级**<br>(决定性影响) | **2. CA-TA-Lite 宏邻域局部搜索** | N1/N3 专门针对 Critical Path 工序与瓶颈机器工人进行深度重排。 | `ZhangBoMOHPSOQ.java:735` | **机制支持**（FC-5.2 证明突破解均在局部搜索中物化；独立增益待单变量消融）。 |
| **S 级**<br>(决定性影响) | **3. 全向量 CFVF 协同更新** | 打破单向量孤立更新，显式引导机器分派与工人配置，从源头削减疲劳工时。 | `ZhangBoMOHPSOQ.java:656` | **机制支持**（受 GIR 证据支持；独立 $C_{\max}$ 贡献待单变量消融）。 |
| **A 级**<br>(强相关影响) | **4. Local-FE Pacing 动态预算** | 调度参数 $\beta$ 后期升至 0.65，显著增加成熟期局部搜索频次与挖掘深度。 | `ZhangBoMOHPSOQ.java:564` | **已证实**（FC-2 证明 20-job 500k $C_{\max}$ 中位数下降 1.4%：$191.1 \to 188.4$）。 |
| **A 级**<br>(强相关影响) | **5. Qp 谱系档案锚点引导** | 突破单点 pbest 局限，提供沿 $C_{\max}$ 极值方向的认知演化通道。 | `ZhangBoQpController.java:185` | **机制支持**（待严格 Qp-only 单变量消融进一步确认）。 |
| **B 级**<br>(中度影响) | **6. DSCR 教师缓存清洗** | 清洗被严格支配的陈旧教师，杜绝 G1 社会学习方向被劣质解污染。 | `ZhangBoQgController.java:142` | **已证实机制门**（$\text{DTUR}=0$，杜绝了过期教师使用；对 $C_{\max}$ 间接起效）。 |
| **C 级**<br>(无算法影响) | **7. 单遍支配、DAG 缓存与耗时打点** | 纯底层工程实现优化，仅缩减程序运行耗时。 | `V35ModuleTimer.java` | <span style="color:#1E90FF">已证实中性（解集零影响）</span> |

---

# 8. 建议的最小验证实验

为验证上述因果链并解决 S 级瓶颈，建议进行以下单变量最小闭环实验：

### 建议实验：Region-aware Environmental Selection 隔离验证（FC-6B 候选）
1. **实验配置**：
   - 基础代码：张博 A4 正式版（`V35_FULL_POOL_OFF`）；
   - **唯一单变量改动**：将 `applyEvaluatedPddr` 从“全局 100 席统一截断”修改为“**对齐下游 15(Cmax)/55(综合)/15(TEC)/15(TWC) 的分群预分配选择**”；
   - 保持 CFVF、CA-TA-Lite、Pacing、DSCR、FM3 完全不变。
2. **测试规模**：`20_2_3_1` 与 `100_2_3_1`，种子 `20260822, 20260823, 20260824`，500,000 FE。
3. **预期验证目标**：
   - 观察 174.44 等优质 $C_{\max}$ 解是否能在最终 Pareto 前沿中稳定保留；
   - 检验种群中段是否保持稳定（解数量稳定在 500 左右，消除 seed24 式耦合失稳）；
   - 评估其对实际 HV、IGD 与 $C_{\max}$ 极值的最终提升效果。

---

# 9. 当前能够确定的结论

基于严密的代码级追溯与已落盘的审计实证数据，当前可完全确定的事实包括：

1. **公平性比较边界完全成立**：算法 A 与算法 B 共享完全相同的 FM3 解码、单产品族占位、`ShiftMode.NONE`、相同种子下的初始种群哈希以及严格 500,000 FE 预算。
2. **算法 A 并非仅更新作业排序**：原算法具备针对资源层的交叉、变异与邻域调整；算法 B 的核心进步在于 **CFVF 实现了 pbest/gbest 在 JS/FA/MA/WA 四向量上的显式协同引导**。
3. **局部搜索调用顺序确凿**：算法 B 在主循环中严格按照 **先 `CA-TA-Lite` 宏邻域搜索、后 `Inherited LS` 继承局部搜索** 的顺序执行，两者共享由 Pacing 动态决定的 Local-FE 硬预算。
4. **生成与存活的机制脱节是 $C_{\max}$ 核心瓶颈**：算法 B 的 CFVF 与 CA-TA-Lite 能够成功生成极低 $C_{\max}$ 突破解（如 174.4367 生成于 288,564 FE），但在全局单标尺 PDDR 环境选择中，这些解在成熟期由于 $p$ 值偏低而面临高达 85.5% 的误杀淘汰风险。
5. **区域吸收具有充分的数据依据**：FC-6A.2 反事实审计证实，被切除的优质方向解在 15/55/15/15 区域配额中具有 72.2%~84.4% 的天然容纳空间，且综合平衡群淘汰数为 0（`rejG2=0`）。
6. **工程级优化对解集绝对等价**：单遍双向支配、DAG Memo 缓存与模块耗时打点通过了严格的逐位 SHA-256 验证，证明其仅优化 CPU 时间，对优化解集分布与 $C_{\max}$ 无任何副作用。

---

# 10. 当前仍不能确定的问题

以下问题由于涉及尚未完成的单变量消融或后续闭环实验，当前仍需保持学术克制，待进一步验证：

1. **各单一创新模块的绝对独立增益**：CFVF、CA-TA-Lite 与 Qp 在完全剥离其他机制时的单变量独立贡献度（如纯 CFVF vs 纯 CA-TA），仍需依靠严格的单变量消融实验给出定量矩阵。
2. **Region-aware Selection 的实际闭环表现**：反事实审计（Counterfactual Audit）给出了充分的理论与数据依据，但其在实际闭环演化过程中对种群多样性、局部搜索反馈及最终多指标综合排名的影响，必须等待 FC-6B 真实运行结果。
3. **极端工件规模下的长程演化稳定性**：在更大规模实例（如 150/200 工件）下，随着搜索空间急剧膨胀，区域环境选择对三目标前沿延展度（Spacing/HV）与收敛速度的综合边界效应仍有待正式矩阵检验。
