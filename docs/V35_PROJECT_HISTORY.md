# 张博改进 v3.5 完整历程与现状复盘

- 编制日期：2026-08-16
- 工程：`E:\学习\李明哲-毕业材料\张博改进`
- 本文档用途：从 v3.5 立项到当前（dualQ 多实例验证收尾）的完整、逐事件记录；供导师汇报、论文写作与后续决策引用。

---

## 0. 一句话现状

v3.5（DSCR + CFVF + Qp/Qg 双 Q + CA-TA-Lite-VNS + FM3 疲劳解码）在**忠实对比口径（P25E）**下被证明：50k 短预算显著优于作者基线 HMOPSO-QGS（HV +38.6%、IGD −35.7%，5/5 seed），500k 论文预算下与基线均势（IGD/TEC 4/4 领先）；此前"正式实验失败"（P25D，A4 排名 7/8）的根因是**六种对比算法被统一"增强版"引擎污染**，不是算法本身问题。P24.2 的 dualQ 参数校准（gb15）经多实例验证后**判定不转正**，正式配置维持 gb5 冻结值。

---

## 1. 背景

### 1.1 论文目标与本地复现

- 论文：李明哲第四章（ESWA），算法 HMOPSO-QGS（Q-learning 引导全局搜索的混合多目标粒子群优化）。
- 论文实验结论：HMOPSO-QGS 在 45 个实例上全面优于 7 个对比算法，IGD 改善 44–81%。
- 本地目标：在作者真实代码（jMetal 5.8、Java 8）上复现基线，并加入"张博改进"（v3.5）三个创新点，产出论文级对比证据。
- 问题域：DR-DHHFSP-ST（双资源分布式异构混合流水车间调度，含设置时间），三目标 `[Cmax, TEC, TWC]`（目标槽 0/1/6），单一产品族、序列无关 SUT、`ShiftMode=NONE`、四向量编码 `JS/FA/MA/WA`。

### 1.2 v3.5 三个创新点（用户明确"大体创新点不改"）

1. **动态疲劳解码（FM3）**：疲劳累积—自然恢复—加工时间反馈一体化解码；`λ/μ/r/δ/Fwarn/Fsafe` 按 45 实例键控生成；默认或 `r=0` 时严格回到作者评价体。
2. **认知—社会全向量搜索**：覆盖 `JS/FA/MA/WA` 四向量的 CFVF（全向量离散飞行）+ 双 Q-learning（Qg 社会引导 / Qp 认知引导，分块冻结协同）+ DSCR（动态社会教师选择）。
3. **CA-TA-Lite-VNS**：24 上下文 × N1–N5 宏邻域的轻量协同自适应 Tabu 搜索（Test-and-Apply），面向序列/机器/工人/设置/疲劳/平衡六类瓶颈。

---

## 2. 工程基线建设（2026-08-07 ~ 08-13，P0–P8.6）

严格按工作包逐层建设，每个包都有验收门和证据目录（`docs/evidence/`），全部保持"作者源码只读、零创新派生可回退"纪律。

| 阶段 | 内容 | 关键产物 |
|---|---|---|
| P0 | 项目治理 | `AGENTS.md`、`docs/ROADMAP.md` |
| P1 | 作者快照与工作副本 | 1806 文件只读基线、SHA-256 溯源、消除模块循环与机器绝对路径 |
| P2 | 论文算例与编码契约 | ESWA 表4/表5/Fig.3 黄金夹具、四向量契约、Fig.5/Fig.6 算子夹具 |
| P3 | 原始解码优先 | 三语义隔离（`PUBLISHED_STOCHASTIC` / `AUTHOR_ACTUAL` / `DETERMINISTIC_CANONICAL`）、20 工序轨迹冻结（Cmax=60.6887…） |
| P4 | 原始算子与完整基线 | Fig.5/Fig.6 算子、M3 四子群、严格 PDDR-FF、三动作 Q-gbest、O1–O9、2000 FE 闭环 |
| P4.1 | 作者代码直接派生 | `ZhangBoEDHHFSPW/MOHPSOQ/Builder/Run` 零创新派生，规范化差异=0 |
| P5 | 疲劳解码创新 | 45 实例疲劳参数物化（`fatigue-parameters/v1`）、双路径硬门（r=0 与作者逐项一致） |
| P5.1 | 生产 SUT/MA 校正 | 45 份实例级 SUT（`instance-extensions/v1`）、PT/SET 疲劳时长分解、第一阶段显式 MA/WA |
| P6.0 | 原 Q-gbest 接入 | 每子群独立 2×3 Q 表、epsilon=0.8 等 |
| P6.1 | CFVF | FMW/MW/M/W 层级耦合动作、JS 逆映射按工件身份对齐、合法域构造为主 |
| P6.1.1 | 评价后 PDDR 校正 | 后代唯一评价后运行 PDDR，返回值真正替换种群与历史 |
| P6.2 | 谱系个人档案 | 容量 6、谱系继承/分裂/删除规则、影子模式 |
| P6.3 | Q-pbest | 四动作、16 状态、批内冻结批量 TD、局部搜索前奖励 |
| P6.4 | 分块冻结双 Q | 前 10% FE 预热（整代向上取整），P/G-block B=5 交替，冻结方只执行不学习 |
| P7.1 | O10–O13 新邻域 | 关键路径迁移、工人重分配、机器-工人联合重分配、自然恢复窗口 |
| P6.5 | 子群语义迁移 | 统一 G1_CMAX/G2_TEC/G3_TWC/G4_BALANCED，不重排作者物理槽位 |
| P7.2 | CA-TA | 六类瓶颈上下文、80/20 工厂选择、等预算 Test、代价感知 Apply（后经校正） |
| P8 | 34 标签消融 | 2 实例 × 3 种子 × 38 标签 = 228 条记录，全部 `COMPLETED` |
| P8.1 | 规范生产基线 | 作者缺陷路径隔离为 `A0_AUTHOR_DIAGNOSTIC`，204 条 P8-v3 记录 |
| P8.2 | I1 黄金示例 | 独立 Python 公式重建 1400 个工序字段通过 `1e-9` 门；I1 5000 FE 解释运行 |
| P8.3 | CA-TA 纠错与性能门 | Apply 改为跨父粒子逐候选执行、v2 代价信用、100k FULL/BASE 时间比 5.04× |
| P8.4/P8.6 | 疲劳一致左移/右移 | FCLS/FCRS 共同空档移位（common-gap）、I1/I0 图例门（FCLS 1/6、FCRS 1/41 接受） |
| P8.5 | 全链路算法审计 | 正式 B0/B1 绑定 Table 9 参数、严格 PDDR、CFVF 0.6 系数、34 开关审计 |

另有 **I0 本人手算门**（导师要求"作者本人手算小例子"）：5 工件 × 2 工厂 × 2 阶段空白表，等待用户手算提交后逐项核对；此前一直只发输入不发答案。

**早期正式诊断（P9 系列，现已标记 legacy）**：`20_2_3_1`、500k、6 seed（20260808–13）FULL vs HMOPSO-QGS-F 配对运行：6/6 `PROMISING_SIGNAL`，中位 Cmax/TEC/TWC 相对改善 −10.7%/−4.3%/−1.1%，但 wall-clock 约 39×；后因 P8.3 CA-TA 纠错等语义变更，该批结果隔离为 `legacy_pre_*`，不进入 v3.5 正式 reference。

---

## 3. 正式实验子路线（V35-P25 系列，2026-08-15 起）

`docs/V35_FORMAL_EXPERIMENT_ROADMAP.md` 锁定正式科学语义：

```text
decoderMode=FM3, familyMode=单族, setupMode=序列无关, shiftMode=NONE
objectives=[Cmax,TEC,TWC], population=100, MaxFEs=500000, runs=20（论文 30，用户定为 20）
```

正式参数继承 Table 9：LS_Times=30、Q_Times=50、r 上界 0.6、交叉 0.2/0.5/0.5、变异 0.08/0.15/0.25 等。`formal_matrix_started=false` 保持，任何正式矩阵仍需用户逐包批准。

### 3.1 P25A 主版本门（A0/A4/A5，5 seed）

- 运行：A0（基线）/A4（主版本）/A5（方向 top-k 教师池）× seed 20260809–13 × 500k。
- 结果：`DONE_WITH_CONCERNS`。统一 reference 1118 点中 A0 贡献 695（62.2%），A4 238、A5 185——reference 未被创新版主导，A4/A5 的 IGD 没有"自引自近"优势。
- A5 默认关闭（可选极值增强模块），A4 成为主版本候选。

### 3.2 P25B 压力分类诊断

- 实现压力诊断、BAL 置信回退、shadow 反事实审计、阈值选择。
- **held-out 门未通过** → `diagnosis_thresholds_frozen=false`；正式路径保持 `BAL` 全开放 + N1–N5 宏邻域，不启用单瓶颈严格掩码。

### 3.3 P25C

- `bal-open-100k` 工程诊断，完成 `engineering_diagnostic`，不构成正式结论。

---

## 4. 正式实验为何失败：P25D（2026-08-14 ~ 15）

### 4.1 现象

- 50k 预算、8 算法对比（A4 + 7 对比算法）下载验收后：**A4 排名 7/8**，NSGA-II/SPEA2 领先，A4 的 HV 大幅低于预期；同时出现大量 repair 事件（如 HMOPSO-QLS 50k 约 12505 次）。
- 与论文"HMOPSO-QGS 全面优于 7 算法、IGD 改善 44–81%"的结论明显不符。

### 4.2 根因：对比引擎"增强版"污染

- P25D 使用 `V35P25DComparativeEngine`：为让六种经典算法能跑通疲劳问题，**统一重写了它们的更新、环境选择和档案逻辑**，超出了"只共享问题、保留各自搜索机制"的公平适配边界。
- 后果：NSGA-II/SPEA2/MOPSO 等对比算法实际上运行的是"带张博增强逻辑"的变体，对比不再是"v3.5 vs 经典算法"，而是"增强版 v3.5 vs 增强版经典算法"。
- 用户自查道歉信确认了这一判断；我复核后认可：业界规范做法是**只为经典算法做最小必要适配（问题接口、表示、目标映射），绝不改写其搜索机制**。

### 4.3 数据证据

- 忠实适配后（P25E），MOPSO 的 HV 从 P25D 的约 0.94 掉到 0.05——P25D 里 MOPSO 的"好成绩"全部来自增强版重写，真实 MOPSO 在规范 FM3 问题上的水平远低于此。
- P25D 目录已隔离：`LEGACY_EXCLUSION.md` 声明 `legacy_enhanced_comparator_rewrite=true`、`valid_for_paper_comparison=false`、`excluded_from_p25e_reference_front=true`。

### 4.4 教训

公平对比的第一原则：**对比算法的搜索机制必须独立、忠实**，只允许共享问题/表示/预算/指标；任何"让算法跑得更好"的适配都会摧毁对比的公信力。P25D 的全部结论作废，不进 reference、不进论文。

---

## 5. 修正与改善：P25E 及后续诊断链（2026-08-15 ~ 16）

### 5.1 P25E 忠实适配（"改了一下"的第一步）

- 新 Runner：`ZhangBoV35P25ECorrectedComparisonRunner`。
- 六种对比算法身份重构：
  - 四个作者算法（HMOPSO-QLS-F、MOPSO-F、MOPSODS-DE-F、MOHEADE-F）→ **作者源码隔离副本**（仅类名/随机源/资源域/日志/插桩不同，`OFFICIAL_JMETAL_DIFF.md` 与 `IMPLEMENTATION_AUDIT.md` 留证）；
  - NSGA-II-F、SPEA2-F → **官方 jMetal 5.8 核心**（tag `jmetal-5.8`，commit `831d62d0`，仅包名/类名隔离，控制流保留）；
  - 全部六算法**静态拒绝引用** CFVF、Qp、DSCR、CA-TA-Lite、方向教师池和 `V35P25DComparativeEngine`。
- 八算法 2000 FE 身份/预算门通过（HMOPSO-QLS 因作者组结构安全停在 1950 FE）。

### 5.2 50k 纠正对比（5 seed × 8 算法 = 40 条）

统一 reference 333 点（40 条运行经验前沿），中位数：

| 算法 | medianHV | medianIGD | 排名 |
|---|---:|---:|---|
| SPEA2-F | 0.9103 | 0.0838 | 1 |
| NSGA-II-F | 0.8969 | 0.1000 | 2 |
| ZHANGBO_A4 | 0.6616 | 0.2063 | 3 |
| HMOPSO-QGS-F | 0.4772 | 0.3210 | 4 |
| MOPSODS-DE-F | 0.4292 | 0.3028 | 5 |
| MOHEADE-F | 0.1996 | 0.4703 | 6 |
| HMOPSO-QLS-F | 0.0837 | 0.6774 | 7 |
| MOPSO-F | 0.0602 | 0.7126 | 8 |

A4 仍落后 NSGA-II/SPEA2 约 30 个百分点——**即使忠实适配，50k 下仍输给官方算法**。但注意：A4 高于作者基线 QGS（+38.6% HV）。

### 5.3 repair 语义审计（回答"是不是适配作弊"）

- `V35P25ERepairAudit` ThreadLocal 审计钩子（默认 no-op，零开销），2000 FE 收集 4 个作者算法全部修复事件。
- 结论：**`repair_is_representation_adapter=true`，不破坏作者搜索语义**。全部修复是 MA/WA 的确定性 `floorMod(value,2)` 折叠（old∈{2,3}→new∈{0,1}），即作者算子产生的资源编号重新索引到当前实例第一阶段合法域（大小 2）；FA/JS 从不被修复；单位置、低频（0.009–0.37/评价）、随搜索时段均匀出现。
- 已知影响如实登记：作者原实例资源域比 `20_2_3_1` 大，适配压缩了其机器/工人选择自由度（4 选 1 → 2 选 1），因此作者算法绝对数值与论文不可直接对比（还需注明"资源域差异 + 疲劳模型"两个口径）。

### 5.4 5 seed 确认：创新点本身有效

50k 下 A4 vs QGS（作者基线）5 seed：**HV +38.6%、IGD −35.7%，5/5 seed 方向一致**。→ 在忠实口径下，v3.5 三创新点组合相对原作者基线确实有效；50k 输给 NSGA-II/SPEA2 不是创新点失效。

### 5.5 预算节奏诊断（找到 50k 落后的主因）

- 机制：继承局部搜索 `LS_Times=30`（Table 9 正式值）在 50k 下吞掉 **79% FE**，只剩 2 个外层循环、100 次 Q 学习、2 次 PDDR 环境选择——Q 学习和种群级进化几乎没机会起作用。
- 单变量实验（LS 30→10→5→2，其余机制与正式 A4 完全一致）：

| LS | local FE 占比 | outerCycles | HV | IGD |
|---|---:|---:|---:|---:|
| 30（正式） | 79.0% | 2 | 0.6213 | 0.2168 |
| 10 | 58.1% | 4 | 0.6842 | 0.1883 |
| 5 | 44.4% | 5 | 0.7463 | 0.1724 |
| 2 | 25.7% | 7 | 0.7804 | 0.1448 |

- 5 seed 确认：LS=5/LS=2 对 LS=30 **5/5 seed 全胜**（HV 中位 +16~18%、IGD −27~31%）。
- 结论：**预算节奏是 50k 下 A4 落后的真实且主要的结构性原因**；残余差距（LS=2 的 0.7804 vs NSGA-II 0.8969/SPEA2 0.9103，约 14–17%）提示短预算下机制收益上限有限。

### 5.6 500k 论文口径对照（A4 vs QGS，4 seed）

- HV 均势（2:2，其中 1 个 seed 差距 <0.001）、**IGD 4/4 领先、minTEC 4/4 领先**、minCmax 2/4（Cmax 仍是弱项）。
- 机制对比：500k 下 A4 与 QGS 预算结构几乎相同（18 外层循环、local 占比 81% vs 82%）——**均势不是预算节奏问题**；差异定位为 **dualQ 分块冻结削减 Qg 学习量**：A4 的 Qg TD=2000（冻结 G-block），QGS 全程学习 TD=3600。
- 解释：创新增益集中在短预算阶段（Qp 主导后期引导），长预算下基线 Qg 全程学习充分收敛而追平。

### 5.7 P24.2 dualQ 参数级校准（用户选"2"，走冻结流程）

- 用户决策：不改创新点结构，只做 P/G-block 参数校准。
- 实施（向后兼容，默认行为不变）：`ZhangBoDualQCoordinationConfiguration` 新增 `gBlockLength`（默认=blockLength=5，等长时 blockIndex/offset 与历史公式逐位一致，单元测试钉住）；`ZhangBoDualQCoordinator` 支持不等长 P/G 块交替；`V35ProductionConfiguration` 可选 `dualQCoordination`（默认 null，哈希不变）；新增 3 项定向测试，17/17 通过。
- 500k 诊断（20_2_3_1，4 seed，统一 reference 881 点）：

| 配置 | medianHV | medianIGD | medianCmax | medianTEC | Qg TD |
|---|---:|---:|---:|---:|---:|
| A4-gb5（正式） | 0.8635 | 0.0825 | 191.70 | 8443.78 | 2000 |
| QGS 基线 | 0.8460 | 0.1412 | 189.49 | 8659.02 | 3600 |
| A4-gb10 | 0.8731 | 0.0781 | 189.42 | 8490.38 | 2520 |
| **A4-gb15** | **0.8738** | **0.0756** | **187.94** | **8381.27** | 2800 |

- gb15（G 块长 15）相对正式 A4：HV +1.2%、IGD −8.4%、Cmax −2.0%（全场最优）、TEC −0.7%（全场最优）；相对 QGS：HV +3.3%、IGD −46%——**首个"4 seed 中位数全面优于基线"的 500k 证据**。
- 冻结重建：V35-P24/P24.1 冻结物（FREEZE_MANIFEST + source-sha256.csv + 磁盘幂等契约）按当前源码树重建并通过；jmetal-algorithm 全量回归 262 项无新增失败（既有失败分类不变：V35P101/D-076、NSGAIIIT 上游、Mockito JDK17 需 `--add-opens`）。

### 5.8 多实例验证 gb15（2026-08-16，最终决策）

按承诺判据（"不要求每项都赢，要求没有实例明显退化"）执行 3 实例 × 2 配置 × 3 seed = **18 条 500k 完整运行**（10/50/100_2_3_1，覆盖小/中/大规模），20_2_3_1 用既有 4 seed 数据同口径补算。每实例用其全部 front 并集构造独立 reference（不同实例目标尺度不同，不共用），指标口径与 P8MetricCalculator 一致（精确扫描线 HV、归一化 [0,1]、参考点 (1.1,1.1,1.1)）。

gb15 相对 gb5 的中位数变化：

| 实例 | 规模 | HV | IGD | Cmax | TEC | 判定 |
|---|---:|---:|---:|---:|---:|---|
| 10_2_3_1 | 10 工件 | −1.9% | **−10.2%** | **−1.0%** | −0.4% | 方向一致 |
| 20_2_3_1 | 20 工件 | **+2.2%** | **−6.4%** | **−2.0%** | **−0.7%** | 全面改善 |
| 50_2_3_1 | 50 工件 | −0.9% | +0.0% | −0.1% | −0.1% | 打平 |
| 100_2_3_1 | 100 工件 | **−11.4%** | **+32.1%** | −2.6% | +1.6% | **明显退化** |

- **结论：gb15 不转正，正式配置维持 gb5。** 收益随实例规模呈倒 U 型：小实例 Q 表状态空间小、Qg 学习充分 → 收益真实；100 工件状态空间大、固定 2800 次 TD 覆盖不足 → 长 G 块把种群过久锁在全局方向、压制 P 块局部多样性（IGD +32% 即收敛不全面）。Cmax 在所有实例持平或改善佐证了"长 G 块强化 Cmax 方向"的机制定位。
- `gBlockLength` 保留为实验参数（`blockFrozen(warmup, blockLength, gBlockLength)` 重载），供论文消融或实例级调参，不进 Table-9 正式口径。P24.2 冻结闭环：参数校准结论是"维持冻结值不动"，无需重建冻结清单。

---

## 6. 当前状态

### 6.1 正式配置快照（Table-9 口径，A4）

```text
decoderMode=FM3, ShiftMode=NONE, 单族, 序列无关 SUT, 目标 [0,1,6]
population=100, MaxFEs=500000, LS_Times=30, Q_Times=50
DSCR=true, CFVF=true, Qg=true, Qp=true, CA-TA-Lite=true
dualQ = blockFrozen(warmup=0.10, P-block=5, G-block=5)   ← P24.2 维持冻结
bottleneck = BAL 全开放, N1–N5, 方向教师池关闭（A5 可选）
对比算法 = P25E 忠实适配（作者隔离副本 4 + 官方 jMetal 5.8 2）
```

### 6.2 已证明 / 未证明

已证明（忠实口径、诊断级证据，非正式显著性）：
- v3.5 三创新点相对作者基线有效：50k HV +38.6%、IGD −35.7%（5/5 seed）；500k 下 IGD/TEC 4/4 领先、HV 均势。
- 50k 落后的主因是预算节奏（继承 LS=30 吞 79% FE），非机制缺陷。
- repair 是表示适配（floorMod 资源域折叠），非算法增强。
- gb15 参数校准收益是实例依赖的，100 工件明显退化，不能转正。

未证明 / 明确不做：
- `formal_matrix_started=false`：45 实例 × 20 runs 正式矩阵未启动，需用户批准。
- `sampled/full_reproduction_accepted=false`。
- I0 本人手算门未完成（等待用户提交手算表）。
- QMOEA 对比算法来源未核验（`PENDING_SOURCE_VERIFICATION`），不以近似类替代。
- Cmax 维度仍是弱项（500k 下 minCmax 2/4 胜）；疲劳指标不是每个 seed 都改善（早期 6 seed 诊断 5/6、4/6）。

### 6.3 证据索引

- P25D 失败与隔离：`docs/evidence/V35-P25D-all-algorithms-50k-pilot/`（`LEGACY_EXCLUSION.md`）
- P25E 适配审计：`docs/evidence/V35-P25E-corrected-comparison/IMPLEMENTATION_AUDIT.md`、`OFFICIAL_JMETAL_DIFF.md`
- repair 语义审计：`.../repair-audit/REPAIR_SEMANTIC_AUDIT.md`
- 50k 5 seed：`.../5seed-50k/`（`P25E_REPORT.md`、metrics.csv、metrics-median.csv）
- 预算节奏：`.../budget-pacing/`（单变量 + 5seed-confirm）
- 500k 对照：`.../500k-pair/P25E_500K_A4_VS_QGS.md`
- dualQ 校准：`docs/evidence/V35-P24.2/V35_P24_2_REPORT.md`、`.../dualq-gblock/`
- 多实例验证：`.../dualq-multi-instance/MULTI_INSTANCE_REPORT.md`（18 条运行 + 逐 seed 明细）
- 子路线图：`docs/V35_FORMAL_EXPERIMENT_ROADMAP.md`；主路线图：`docs/ROADMAP.md`（任务登记 D-081）

---

## 7. 复盘：五条教训

1. **对比公平性是第一公信力**：P25D 用"增强版"统一重写六种对比算法，直接导致 A4 排名 7/8 的假败局；任何对对比算法的机制改写都会让结果不可用。正确做法是隔离副本 + 官方核心 + 静态拒绝检查。
2. **预算节奏与机制强度要分开归因**：50k 下 A4 输给 NSGA-II/SPEA2，主因是继承局部搜索吞掉 79% FE 导致外层循环只有 2 次，而非创新机制无效；LS 30→2 后 HV +16~18% 且 5/5 seed 确认。
3. **短预算与长预算结论不可互推**：50k 下 A4 强于 QGS（机制强度），500k 下被追平（Qg 学习充分性）；论文口径（500k）与工程口径（50k）必须分开评估。
4. **单实例参数校准有陷阱**：gb15 在 20_2_3_1 上 4 seed 全面改善，多实例验证后 100 工件明显退化——转正前必须做多规模实例方向性验证。
5. **一切结论要有隔离留痕**：每个失败路径（P25D、legacy P9、legacy_pre_*）都有 `LEGACY_EXCLUSION.md` 式声明和哈希留证，任何阶段都可以回溯"哪个证据还有效、哪个已作废"。
