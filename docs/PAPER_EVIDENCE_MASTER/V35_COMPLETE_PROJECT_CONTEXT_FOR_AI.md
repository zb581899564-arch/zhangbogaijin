# V35 张博改进项目完整上下文总档案（供其他 AI 注入）

> 版本：2026-08-31  
> 项目根目录：`E:\学习\李明哲-毕业材料\张博改进`  
> 用途：把本文件整体注入新的 AI 对话，使其在提出后续路线前了解项目从来源核验、解码重建、三项创新、机制审计、参数实验、失败分支、正式实验准备到当前停止点的完整事实。  
> 重要：本文件是“上下文总档案”，不是新的实验授权。任何训练、上传、500k、多实例、DOE、Validation、Final Test 或正式矩阵仍需单独预登记和用户授权。

---

## 0. 新对话中的 AI 必须先遵守的阅读规则

1. **先看当前裁决，再看历史过程。** 本项目采用 append-only 证据治理，旧报告不会删除，但可能被后续勘误或否决。本文中“当前状态”和“最终裁决”优先于同一主题的早期信号。
2. **工程通过不等于科学通过。** `BUILD SUCCESS`、2k/20k贯通、运行完成、哈希闭合、机制触发，只证明相应工程门；它们不自动证明算法优越、机制因果、论文复现或 Final 资格。
3. **历史臂不得改写。** A0/A1/A2/A3/A4、P25D、P25E、FC、DOE、Stage2 等都有冻结语义。不得为了讲出单调故事给旧臂补机制、删机制或重新命名。
4. **所有新运行必须先回答：** `Which preregistered gate authorizes this run?`。没有唯一、已批准的 Gate 就是 `DO_NOT_RUN`。
5. **任何结论必须留证。** 至少保存预登记、输入/Jar/config/snapshot 哈希、runId/sourceRunId、原始输出、分析脚本、自动裁决和文件级 SHA-256。没有可反查证据链的结论不得驱动路线或进入论文。
6. **正式算法组件当前不得擅自删除：** FM3、CFVF、Qp/Qg 双Q和 CA-TA-Lite 是研究路线的目标组件；但现有数据尚未证明完整 A4 已达到 Final 资格。后续若要改变组件，必须另行提出科学上可反驳的单变量方案。
7. **不要把“超过所有 baseline”理解成每个实例、每个 seed 全胜。** 合法目标是实例级平均秩、HV/IGD总体质量、配对效应、显著性、规模稳定性和三目标极值安全性总体占优。

---

## 1. 一页式当前结论（截至 2026-08-31）

### 1.1 当前正式问题与算法语义

```text
decoderMode=FM3
familyMode=DEGENERATE_SINGLE_FAMILY
familyCount=1
setupMode=SEQUENCE_INDEPENDENT
ShiftMode=NONE
objectives=[0,1,6]=[Cmax,TEC,TWC]
population=100
MaxFEs=maximum allowed full evaluations
budgetProtocol=PHASE_CONSISTENT_BUDGET_TERMINATION

subSwarm physical/search order=[G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC]
subSwarm mixture=[20,40,20,20]
PDDR=GLOBAL_ORIGINAL
activeArchive=UNBOUNDED_FULL
localSearchOrder=CA-TA-Lite -> inherited LS
dualQ=10% warmup, P-block=5, G-block=5, rho=0
directionTeacherPool=OFF
pressureStrictMask=OFF; actual context fallback=BAL; N1-N5 all open
PF-SDST=OFF
```

正式冻结算法 Jar：

```text
SHA-256=8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
```

该 Jar 是历史 A0–A4/Stage2 的正式算法身份锚点。后来的诊断 Jar、外部算法比较 Jar、local-FE repair Jar 都是独立构建物，不得覆盖或冒充正式 Jar。

### 1.2 当前候选状态

```ini
A2Promoted=false
A4Promoted=false
FinalCandidateApproved=false
FINAL_FROZEN=false
formalMatrixRunning=false
formalMatrixPaused=true
sampledReproductionAccepted=false
fullReproductionAccepted=false
```

- A4 在 `100_2_3_1 × 12 seeds` 的 Stage2 先导中相对 A0/A2有正信号，但跨实例确认在 `100_5_3_1` 触发否决，故 `A4_NOT_PROMOTED`。
- A2 随后对 A0 的独立六实例确认也在 `100_8_3_1` 触发否决，故 `A2_NOT_PROMOTED`。
- 因此当前**没有获准的 Final 候选**，旧 A4 只能称 `A4_LEGACY` 或冻结候选，不能称 Final。

### 1.3 当前最重要的性能事实

四算法 Gap Probe V2（A4、A0/QGS、NSGA-II-F、SPEA2-F；两个实例、两个新 seed、16×500k）表明：

- `50_2_3_1`：A4总体领先或接近领先；
- 困难实例 `100_5_3_1`：A4和A0相对官方核 NSGA-II/SPEA2 出现严重 Pareto 覆盖退化；
- A4相对 SPEA2-F：HV gap `+63.5%`、IGD gap `+260.7%`；相对 NSGA-II-F：`+67.8%/+311.3%`；
- A4的 min Cmax 仍接近全场最优，问题集中在**前沿覆盖与收敛质量**，不是单目标 Cmax 全面崩坏；
- 总裁决 `GAP_GT_15`，`RED=false` 仅表示没有满足预登记的跨两个实例稳定红色条件，不表示算法合格或“绿色”。

### 1.4 当前关于 PDDR/非支配点多的结论

- `PDDR=GLOBAL_ORIGINAL` 保持不变；现有证据没有确认 PDDR 程序 bug。
- FC5 50k/100k/250k 均未复现“严格 ND 超过100→代表大量丢失→archive-working脱节”的完整链：250k 最大严格 `Nnd=92`，`Nnd>100`轮数0，困难/正例中位 `Roverflow` 只差 `+0.038`，四方向保留差有限，困难实例 Cmax archive-working gap 中位0。
- Pareto覆盖只读审计发现，observed-only候选虽存在，但在目标空间近冗余；并回决策前沿的潜在 HV 回收最大仅 `0.79%`，90个窗口无一达到2%。
- 因而当前观察更指向**生成侧多样性不足**，而不是“PDDR压缩掉大量有价值前沿”。这仍是观察性判断，不是已确认根因。
- BP_RESERVED、REGION_AWARE、ORDER_SWAP 均已失败；不得重新包装为新修复。

### 1.5 当前最近一次修复路线裁决

- 0-FE leverage audit 曾选择 `betaMax`（local-FE pacing上界）作为单旋钮：C0=.65、C1=.55、C2=.45、C3=.35。
- 20k证明实现和FE回流，50k证明剂量可分；50k勘误后 C2/C3进入250k。
- 250k中 C2失败，C3虽过终态门但在正常实例100k/150k检查点出现3/3一致实质退化，与终态小幅正向冲突。
- 最终：`LOCAL_FE_PACING repair family=PILOT_REJECTED`、`NO_REPAIR_CANDIDATE`；保持正式 `betaMax=0.65`，不跑500k，不换第五档继续调参。

### 1.6 当前真正停止点

最新 0-FE Pareto coverage leverage audit 裁决：

```ini
PARETO_COVERAGE_AUDIT=NO_ACTIONABLE_LEVER
rootCauseCandidate=NONE
newRepairImplemented=false
newExperimentStarted=false
DOEStarted=false
500kStarted=false
formalMatrixRunning=false
```

下一步不是自动跑实验。若继续，应先由新的规划对以下两条路线作选择：

1. **诊断路线：** 建立250k候选级 PDDR/来源归因遥测（含正常实例对照），只为补齐“哪个生成源导致中后段覆盖不足”；
2. **投稿路线：** 停止深挖根因，基于现有正负结果收缩算法主张，同时优先补齐外部作者算法 fair-ready、FM3独立模型实验和可发表统计设计。

---

## 2. 项目目标、来源和三条实现语义

### 2.1 最终研究目标

以李明哲第四章 HMOPSO-QGS 论文语义和完整 Java/jMetal 5.8 工程为来源，在隔离副本中建立不继承已确认作者缺陷的规范生产基线，并研究三类贡献：

1. 序列无关设置时间下的动态疲劳解码 FM3；
2. DSCR + Qg/Qp + 谱系个人档案 + CFVF 的认知—社会全向量搜索；
3. CA-TA-Lite 五宏邻域、Test/Apply/Re-test 和预算感知局部开发。

产品族序列相关设置时间只保留接口和退化占位；当前实验不得称为 PF-SDST。

### 2.2 来源优先级

算法语义冲突依次参考：

1. `E:\学习\ziliao\v3.5.md`；
2. 总体 v2 方案；
3. 双Q、疲劳、CA-TA 三份细节方案；
4. Java/jMetal 编解码优先实施方案。

原算法语义依次参考：

1. `E:\学习\eswa2026-最新李明哲第四.pdf`；
2. 学位论文第四章；
3. 作者当前 Java 实际行为。

论文与 Java 不一致时保留双线，不选择更有利的一条覆盖另一条。

### 2.3 六类语义标签

| 标签 | 含义 | 能否进入当前正式比较 |
|---|---|---:|
| `published_baseline` | 按论文公开规则重建 | 作为来源/验证语义 |
| `author_actual` | 作者 Java 的真实执行，包括缺陷与随机性 | 否，仅诊断 |
| `deterministic_canonical` | 修正身份映射、资源域和随机源后的规范HMOPSO-QGS | 是，A0母线 |
| `fatigue_improved` | 加入FM3疲劳累积、恢复和时长反馈 | 是 |
| `paper_verification_baseline` | P2–P4黄金算例与公式oracle | 否，不是生产入口 |
| `author_diagnostic` | 隔离复现作者缺陷 | 否 |

---

## 3. 基础问题建模与 FM0–FM3

### 3.1 四向量编码

每个解含等长四向量：

```text
JS：工件加工顺序
FA：工厂分配
MA：第一阶段机器分配
WA：第一阶段工人分配
```

向量位置不是工件编号。任何跨粒子资源复制必须通过 JS 逆映射按工件身份定位，不能把 position 当 job id。

### 3.2 当前三目标

七槽内部对象只将以下三槽作为正式优化目标：

```text
objective[0]=Cmax
objective[1]=TEC
objective[6]=TWC
```

外部比较适配器必须只暴露 `[0,1,6]`，不能让 NSGA-II/SPEA2 读取其它诊断槽。

### 3.3 FM3动态疲劳事件顺序

每道工序严格按：

```text
求最早开始时间
→ 根据空闲间隔进行自然恢复
→ 依据当前疲劳调整 setup 与 processing 时长
→ 计算完工
→ 累积疲劳
→ 更新机器/工人可用时间
```

基础时长：

```text
PT0=ST/(MS×WE)
SET0=SUT/WE
AT0=PT0+SET0
```

同一疲劳倍率同时作用于 PT 和 SET。禁止用 `0.1×ST` 冒充 SUT。第一阶段必须按工件身份读取显式 MA/WA；后续阶段按规范 ECT/FIFO/FAM 联合选择资源。

### 3.4 疲劳参数与指标

标准化场景一次性物化：

```text
lambda in U(0.01,0.03)
mu in U(0.03,0.07)
r=0.30
Fwarn=0.80
Fsafe=0.90
delta=r/(lambda ln2)  # 仅派生，不独立采样
```

记录 `Fmax/Favg/FE`、工人疲劳方差、高疲劳比例、最长连续工作时长、自然恢复总时长。它们是计算抽象，不得宣称为真实工人生理测量，也不构成第四目标。

### 3.5 黄金示例

- I1：第四章10工件工程黄金实例，承担四向量、甘特图、疲劳轨迹、CFVF和CA-TA讲解；
- I0-v35：独立公式重建与 Java 逐字段对照；曾比较370个工序字段和11个目标/疲劳字段，最大误差约 `3.55e-15`；
- I0/I1历史 Shift 轨迹只作教学/历史证据，不进入正式主线。

---

## 4. 第二创新：DSCR、Qg/Qp、谱系档案与 CFVF

### 4.1 四子群语义

逻辑角色：

```text
G1_CMAX
G2_TEC
G3_TWC
G4_BALANCED
```

物理槽映射固定：

```text
groupU1  -> G1_CMAX
groupD3  -> G2_TEC
groupUNew-> G3_TWC
groupC2  -> G4_BALANCED
```

物理执行/配置常写成 `[G1,G4,G2,G3]`，当前容量 `[20,40,20,20]`。所有目标索引和方向判断只能从集中语义映射读取，不得用 enum ordinal 或变量名猜测。

### 4.2 Qg与DSCR

- Qg保留李明哲 HMOPSO-QGS 的社会引导结构；
- DSCR只在 Qg 动作前清理 `previous/historical` 缓存中已被当前社会快照严格支配的老师；
- DSCR不增加动作、奖励或FE；完全同目标和互不支配候选不替换；
- 真实教师使用前要求 `dominatedTeacherUses=0`；指标为 DTUR 和 SCRR；
- 方向教师池 A5/top-k 后来没有稳定胜过 A4，正式默认关闭。

### 4.3 CFVF全向量更新

CFVF同时更新 JS/FA/MA/WA：

- JS继续使用交换序列；
- 资源层使用FMW/MW/M/W、资源惯性、合法探索及pbest/gbest冲突消解；
- JS变化后按工件身份重新定位资源；
- 只更新第一阶段显式MA/WA；
- repair仅为异常安全网，正式证据要求非法解、异常repair和重复评价为0。

### 4.4 谱系个人档案

每粒子谱系维护容量6的个人非支配档案，不是新的种群级全局档案。来源仅为本谱系已评价父代、全局后代和合法局部后代。更新使用严格三目标Pareto、近重复连通分量去重、方向锚点/epsilon锚点/最远点填充；只有目标距离极近才用疲劳风险破平。

PDDR后按谱系保留、分裂、退休或删除；换子群只更新标签，不清空档案。P6.2最初以影子模式验证行为不变。

### 4.5 Qp与双Q分块

Qp四动作：

1. KEEP：保持当前个人领导；
2. DIRECTIONAL：子群方向锚点；
3. EPSILON：加性epsilon收敛锚点；
4. COMPLEMENTARY：满足方向质量阈值的认知—社会互补锚点。

状态 `(Eg,Hi,Ri)` 共16类，每个子群共享 `16×4` Q表并使用可行动作掩码。奖励含Pareto支配、方向改善、档案贡献和疲劳风险；必须在局部搜索前结算，局部后代不能回写为本轮Qp奖励。

正式A3/A4采用：10% FE预热，随后P-block/G-block各5代、从P-block开始、`rho=0`。冻结方仍执行当前贪婪动作和刷新状态，但不提交TD。

### 4.6 已知负向证据

- A2→A3在Stage2先导中明显退化；
- 因果拆分显示“容量6档案+固定方向pbest”已伤害前沿，之后Qp动作/旧奖励又可能继续伤害；
- P5/G5时序本身没有通过稳定退化门；
- observe-only Qp显示未学习时动作分布几乎全KEEP；冷启动优先DIRECTIONAL虽改变动作但未达到改善门；
- 因此不能把Qp、个人档案或双Q写成已独立证明的正贡献，也不能直接把某一个单点认作唯一根因。

---

## 5. 第三创新：CA-TA-Lite

### 5.1 上下文与宏邻域

上下文仍为 `4 subSwarm × 6 bottleneck = 24`。瓶颈标签为 `SEQ/MAC/WOR/SET/FAT/BAL`；BAL表示证据不足，不是第六种竞争压力。

五类宏邻域：

```text
N1：JS基础Insert/Swap/Reverse
N2：基础机器/工人资源移动
N3：按SEQ/SET/BAL在critical/setup-edge/family source中路由
N4：按WOR/MAC/SET/FAT/BAL选择资源重分配
N5：同时形成JS动作和资源动作，通过基因变化产生自然恢复
```

N3–N5是v3.5独立实现，不得映射为旧O10–O13或Shift动作。N5不得直接改start time。

### 5.2 Test/Apply/Re-test

- preview不消耗FE；
- Test评价合法宏动作，形成上下文动作价值；
- 高置信时可用严格mask，低置信BAL开放N1–N5；
- Apply使用冻结动作；连续失败、mask变化或预算条件触发Re-test；
- Qg/Qp奖励先于局部搜索结算，局部改善不反向污染全局搜索信用。

### 5.3 压力诊断P25B

曾实现目标工厂五压力：SEQ、MAC、WOR、SET、FAT，并用绝对强度与领先差距双门决定严格分类。Shadow反事实独立Problem、随机源和计数器，不进入正式FE、PDDR、档案或Q表。

但held-out的漏失率门失败，因此阈值未冻结。当前正式路径：

```text
pressureClassifier=diagnostic_only
actualBottleneck=BAL
strictPressureMask=false
enabled=N1-N5
shadow=false
```

不得继续用同一校准/held-out集调阈值，也不得声称瓶颈诊断已投入正式搜索。

---

## 6. PDDR、档案和四类前沿对象

### 6.1 GLOBAL_ORIGINAL PDDR

当前候选池固定为已评价全局后代在前、父代在后；严格三目标支配计算score，完全同目标互不支配，再按原始顺序稳定破平，选前100并真正替换工作种群及历史映射。PDDR不消耗FE。

### 6.2 四类前沿

| 对象 | 含义 | 搜索 | 主指标 |
|---|---|---:|---:|
| `decision-front` | 算法真实活动档案/getResult | 是 | 是 |
| `observed-full-front` | 旁路观察全部已评价候选的严格ND集合 | 否 | 仅审计 |
| `representative-front-k30` | 确定性30点代表集 | 否 | 否，仅绘图 |
| `sensitivity-front-k25/k50` | 固定基数敏感性 | 否 | 只作敏感性 |

K30不得进入搜索、PFref、HV、IGD或论文主表。ND1/ND2教师视图、ND3/ND4有界活动档案虽已本地实现，但从未远端启动、未晋升。

### 6.3 已否决的PDDR/档案修法

- `BP_RESERVED_LEGACY`：历史污染/预留方案，退出主线；
- `REGION_AWARE 15/55/15/15`：20/100-job均失败，100-job严重退化；
- `ORDER_SWAP`：inherited LS提前虽改善Cmax，但IGD越门；
- 有界档案/teacher view：休眠，未获实验资格；
- “非支配点多所以裁剪”：没有证据，禁止。

---

## 7. 预算、随机性和公平比较协议

### 7.1 Phase-consistent预算

`MaxFEs=500000`定义为最大允许完整评价数，不要求精确命中。一个完整Q phase：

```text
qPhaseFE=population×Q_Times=100×50=5000
0<actualFE=decoderCalls<=MaxFEs
0<=remainingFE<5000
utilizationRate>0.99  # 正式500k
```

剩余预算不足完整phase时停止，称 `PHASE_CONSISTENT_BUDGET_TERMINATION`；不允许partial Q phase或尾部补评价。每个公平组还要求五臂/多算法初群、实例、seed、provenance一致，actualFE跨度小于5000。

### 7.2 共同问题、独立搜索机制

共享：实例、JS/FA/MA/WA、FM3、SUT、疲劳参数、Shift NONE、目标[0,1,6]、初始种群、FE、指标、reference协议。  
不共享：更新、选择、档案、DE、分群、Q学习、局部搜索等搜索核心。

P25D因统一增强/重写六种比较算法而失去论文资格；P25E开始按官方jMetal或作者源码做最小Problem适配。

---

## 8. 从P0到P9的基础工程时间线

| 阶段 | 主要工作 | 当前地位 |
|---|---|---|
| P0 | 建立项目治理、原始材料只读边界、路线图与证据模板 | 有效治理 |
| P1 | 冻结作者源码/资料、SHA清单，建立隔离工作副本 | 来源证据 |
| P2 | 固化论文实例、表格、Fig.3四向量、身份映射和黄金夹具 | 方法证据 |
| P3 | 重建原始解码oracle，验证ECT/FIFO/FAM、微调和右移 | 论文验证线；Shift后被隔离 |
| P4 | 恢复论文算子和完整HMOPSO-QGS验证基线 | 论文验证线 |
| P4.1 | 从作者Java机械派生，登记WA全零、MA未活动、固定域、共享状态和随机性缺陷 | author diagnostic |
| P5 | 引入疲劳累积、恢复、工时反馈和疲劳工人选择 | 第一创新 |
| P5.1 | 修正生产SUT/MA/WA和合法资源域 | 正式FM3基础 |
| P6.0 | 独立恢复原Qg，避免与CFVF混合归因 | 第二创新基线 |
| P6.1 | CFVF全向量离散飞行 | 第二创新 |
| P6.1.1 | PDDR移动到完整评价之后并携带历史映射 | 正式环境选择 |
| P6.2 | 容量6谱系个人档案，先影子验证 | Qp基础 |
| P6.3 | Qp认知引导与同步双Q | 后续改为分块 |
| P6.4 | 10%预热、P5/G5分块冻结 | A3/A4历史语义 |
| P6.5 | 统一四子群角色与物理槽映射 | 正式语义 |
| P7.1 | 历史O10–O13邻域 | 后来被CA-TA N1–N5替代，legacy |
| P7.2 | 代价感知上下文自适应Test-and-Apply | CA-TA前身 |
| P8 | 集成、消融、黄金实例、共同初群与证据边界 | 工程整合 |
| P8.1 | 规范生产基线取代作者缺陷退化线 | A0母线 |
| P8.2 | I1工程黄金示例、独立公式重建 | 方法证据 |
| P8.3 | CA-TA Apply语义、真实时间/确定性代价代理等纠错 | 正式CA-TA基础 |
| P8.4–P8.6 | Shift/FCLS/FCRS历史研究 | `legacy_shift_on` |
| P9 | 早期500k、五seed、decoder计时和正式实验草案 | 历史诊断，不是当前正式统计 |

---

## 9. V35-P0到V35-P24：正式主线形成

### 9.1 V35-P0–P8

- P0冻结源码、配置、历史证据；
- P1重定级原始基线，正式生产不再退化到作者缺陷路径；
- P2建立单产品族/序列无关SUT占位契约；
- P3抽出OperationTransitionKernel；
- P4完成Shift NONE下FM3；
- P5永久冻结Shift正式路径；
- P6建立规范A0/FULL公平外循环；
- P7建立SocialKnowledgeSnapshot；
- P8完成DSCR pre-action sanitization。

### 9.2 V35-P9–P19

- P9做DSCR机制门和教师审计；
- P10/P10.1发现教师滞后、G1退化并试验方向top-k候选池；
- P11收口QG0/QG1单变量；
- P12证明DSCR后 `DTUR=0` 且无post-action override；
- P13–P16实现CA-TA-Lite 24上下文和N1–N5真实路由；
- P17/P18建立passive archive和三目标Best-Ever；
- P19追踪Cmax从Generation→Admission→Survival→Exploitation的生命周期。

### 9.3 V35-P21–P24

- P21六梯级消融先导：仅用于早期归因和预算修复；
- P22做10工件多实例pilot；
- P23用3/5工件穷举精确前沿交叉核验；
- P24冻结第一版参数；P24.1修订A3历史值/JDK17回归；
- P24.2比较dualQ gb5/gb15，100-job表明gb15明显退化，最终保持P/G=5/5。

这些阶段建立了工程可运行和机制接线，但不等于45实例正式统计完成。

---

## 10. A0–A5定义

```text
A0 = 规范HMOPSO-QGS公平基线（FM3共享问题）
A1 = A0 + DSCR
A2 = A1 + CFVF
A3 = A2 + 谱系个人档案 + Qp + 双Q分块
A4 = A3 + budget-aware CA-TA-Lite
A5 = A4 + 方向top-k教师池（未转正，默认关闭）
```

这是开发历史链，不保证逐级单调。后来的正式消融计划已改为 Final 上下文下 leave-one-component-out，而不是强求 A0<A1<A2<A3<A4。

---

## 11. P25A–P25E与八算法比较

### P25A：主版本与教师池

旧压力语义下比较A0/A4/A5，后来统一标记 `legacy_pre_pressure_diagnosis`。教师池未稳定优于A4，A5关闭。

### P25B：压力诊断与shadow阈值

实现五压力、双置信门、shadow反事实隔离；held-out漏失率超过5%，因此 `diagnosis_thresholds_frozen=false`，正式保持BAL全开放。

### P25C：三seed 100k安全回退

得到 `A4_PREFERRED_SIGNAL`，但仅工程信号，Cmax/TEC/TWC极值未全面领先，不是论文结论。

### P25D：八算法五seed 50k

初版把六种基线统一放进增强比较器，搜索机制被重写/增强。结果保留但：

```text
legacy_enhanced_comparator_rewrite=true
valid_for_paper_comparison=false
```

不得进入新reference或论文优越性结论。

### P25E：忠实适配纠正

- 共享Problem/四向量/FM3/FE/初群，保留各算法搜索机制；
- NSGA-II/SPEA2使用隔离官方jMetal 5.8核心；
- MOPSO不得用连续OMOPSO/SMPSO冒充离散MOPSO；
- 作者算法只允许Problem、Solution、初群、随机源、FE和日志白名单适配；
- QMOEA因无可信源码保持 `PENDING_SOURCE_VERIFICATION`；
- 50k结果只作忠实适配先导，不是正式统计。

后来 NSGA-II-F、SPEA2-F 已完成独立fair-ready和20k production preflight；外部适配器在100-job 500k存在观测数据内存债，需要高堆或有界化观测层修复，但搜索语义本身未发现问题。

---

## 12. FC-2到FC-6：Final Candidate收口

| 分支 | 变量 | 裁决 |
|---|---|---|
| FC-2 | Dynamic local-FE pacing | 当时转正为A4-Pacing |
| FC-3 | Cheap-Test | 未转正 |
| FC-4 | soft-freeze `rho>0` | 100-job否决，保持rho=0 |
| FC-5 | 候选生成后是否被工作种群/教师利用 | 发现利用断裂线索，但不是PDDR程序bug |
| FC-6A.3 | PDDR模式与候选池审计 | 明确GLOBAL_ORIGINAL/BP/REGION三模式，隔离BP |
| FC-6A.4 | CURRENT vs ORDER_SWAP | Cmax改善但IGD越门，保持CA-TA→inherited LS |
| FC-6B | REGION_AWARE 15/55/15/15 | 20/100-job均失败，100-job一票否决 |

FC-6最终冻结：

```text
PDDR=GLOBAL_ORIGINAL
localSearchOrder=CA-TA-Lite -> inherited LS
BP_RESERVED_LEGACY=excluded
REGION_AWARE=rejected
```

注意：`15/55/15/15`只是失败的生存配额，不是搜索期四子群容量；搜索期基线始终是`20/40/20/20`。

---

## 13. DOE1四子群容量实验

### 13.1 设计

合法mixture：G1/G2/G3为10–30、G4为25–60、全部5的倍数、总和100，共111组合。15个treatment：

- 强制点：20/40/20/20、15/55/15/15、25/25/25/25；
- 12个确定性constrained D-optimal点；
- 二次Scheffé模型：4线性+6两两交互，共10列；
- 3实例×3seed×15=135条500k开发；
- 响应用相对基线的paired percentage change，避免100-job量级支配20-job；
- 设rank、condition number、adjusted R²、LOTO predicted R²模型充分性门；模型失配时按实测paired median选候选。

### 13.2 Held-out确认

新实例 `20_5_4_1/50_5_4_1/100_5_4_1`，seed `20260901..05`，baseline+最多三候选，最多60条500k；开发PFref与确认PFref完全分离。

### 13.3 结论

T1/T2/T3均未满足跨实例至少2%Cmax改善及HV/IGD安全门，因此：

```text
FINAL_SEARCH_MIXTURE=20/40/20/20
DOE1_COMPLETED=true
DOE2_PACING=not automatically started
```

DOE1的目标是判断是否值得替换基线，不是强行找到更好参数；“没有候选通过”是有效冻结结果。

---

## 14. Stage2冻结、Master与暂停

### 14.1 已完成的生产准备

- Final clean source/Jar/config冻结；
- 45实例×20seed=900份共同初群snapshot manifest；
- phase-consistent预算协议；
- 训练机4/8/12/16 JVM吞吐，16并行可用；
- 外置A0–A4 snapshot-bound Runner；
- Master renderer v2，计划4500物理运行=5臂×45实例×20seed；
- 每个公平组要求完整唯一A0–A4 roster、同snapshot、同problem provenance、FE跨度<5000。

### 14.2 为什么暂停4500矩阵

矩阵曾启动，但用户认为在机制可靠性和Final候选未确定前一次跑4500条风险过大，故暂停。当前：

```text
acceptedFairnessGroups=12
acceptedPairedRuns=60
excludedUnpairedCompleted=8
excludedPartialAttempts=7
formalPFrefGenerated=false
formalStatisticsGenerated=false
```

8条非配对完成和7条partial永远不得进入reference。暂停标记在训练机：

`/home/inspur/aicomp/zhangbo-v35-stage2-master-v2-20260823/formal/PAUSED_BY_USER.properties`

### 14.3 12组五臂先导结果（100_2_3_1）

统一pilot PFref 1979点。相邻中位变化：

| 对比 | ΔHV | ΔIGD | ΔCmax |
|---|---:|---:|---:|
| A0→A1 | -1.56% | +7.58% | +2.04% |
| A1→A2 | +29.90% | +6.88% | -1.32% |
| A2→A3 | -16.24% | -24.93% | +1.25% |
| A3→A4 | +22.82% | +37.85% | +1.31% |
| A0→A4 | +25.24% | +19.02% | +3.95% |

A4 vs A0还表现为TEC约+5.40%、TWC约-1.45%。这些数字只说明单实例机制链：A3负向、A4把整体拉回并超过A2/A0；不能外推为多实例Final结论。

---

## 15. 非支配基数、A2→A3及Qp诊断

### 15.1 基数敏感性

对60条Stage2配对前沿做精确去重、K25/K50、leave-one-run/arm-out等审计，A4/A0的排序没有因等基数而反转。因此“ A4仅因输出点多获得HV/IGD优势”未被确认。

但ND0观察到 `decision-front != observed-full-front`，说明候选生命周期仍有差异；这只阻止静默换front来源，不授权启动ND1–ND4。

### 15.2 D0–D3拆分

```text
D0=A2控制
D1=D0+容量6个人档案+确定性方向pbest
D2=D1+同步Qp四动作/旧未裁剪奖励
D3=D2+10%预热/P5-G5冻结（A3时序）
```

- D0→D1：2/3 seed退化，中位HV -9.02%、IGD +102.56%；领导选择30,000次无fallback，故不是档案空；
- D1→D2：3/3退化，中位HV -4.32%、IGD +30.11%；
- D2→D3：未稳定退化，不能把P5/G5定为主因；
- 总体最初只能称复合块问题。

### 15.3 Q0/Q1最小诊断

- Q0保留Qp动作和pbest使用，但全程observe-only、不算奖励、不更新Q表；D1→Q0触发稳定退化，Q0→D2未稳定退化，裁决 `QP_ACTION_POLICY_HARMFUL`；
- Q0零表30,000动作中KEEP 29,146次（97.15%）；
- Q1只把冷启动并列优先改为DIRECTIONAL，动作真实改变，但改善未过门，裁决 `COLD_START_TIE_BREAK_NOT_CONFIRMED`；
- 奖励裁剪虽纠正数值尺度，但性能未晋升；不得覆盖正式Jar。

---

## 16. A2/A4和A0/A2跨实例确认

### 16.1 A2 vs A4（60×500k）

六实例：20/50/100-job各2个；seed `20260901..05`；30对配对。总体中位：

```text
ΔHV=+1.50%
ΔIGD=+7.24%
ΔCmax=+1.72%
```

但100-job pooled负向，`100_5_3_1`触发单实例否决，故：

```text
A4_NOT_PROMOTED
```

不得在该确认集上调Qp、CA-TA、PDDR、Pacing、rho、mixture或奖励来“救A4”。

### 16.2 A0 vs A2（60×500k）

使用另一套六实例和seed `20260911..15`。总体ΔCmax约 `-0.7410%`，仅3/6实例HV与IGD同时不负，`100_8_3_1`触发100-job否决：

```text
A2_NOT_PROMOTED
```

因此A2胜A4不等于A2胜A0；当前不存在Final。

---

## 17. FC5-T：候选膨胀/利用断裂迁移审计

### 17.1 假设链

```text
MergePool严格ND膨胀
→ PDDR压缩到100工作槽
→ 四方向代表未保留/未利用
→ archive-working脱节
→ 教师曝光和有效后代不足
→ 100-job前沿退化
```

FC5原结论是“利用断裂线索”，不是PDDR代码bug。FC6只否决三种修法，不是否决假设本身。

### 17.2 50k

纠错后：74个PDDR轮、296条方向代表、74个gap快照；仅一次代表保留5/6；联合H1门不成立，裁决 `INSUFFICIENT_EVIDENCE`。

### 17.3 100k

6条 `100_5_3_1 × 3seed × A2/A4`：Nnd范围8–76，无≥90或>100；代表保留率高，教师链未断；出现小于1%的A4后半段Cmax gap，但不足定因。裁决 `INCONCLUSIVE`。

### 17.4 250k

12条正/负对照全部通过：最大严格Nnd=92、无>100窗口；困难与正例Roverflow差+0.038；四方向保留差不大；archive-working Cmax gap中位0。裁决：

```text
FC5_TRANSFER_NOT_CONFIRMED_AT_250K
PDDR=KEEP_GLOBAL_ORIGINAL
```

这不是PDDR永远最优的证明，只是否决“overflow/capacity是当前首要根因”。

---

## 18. Failure Replay、Teacher Calibration与路线转向

历史失败case `100_5_3_1/seed 20260901/A4/500k` 被新鲜F1 replay确定性复现：A4前沿与历史逐字节相同，相对历史A2：

```text
HV=0.5545772540 vs 0.8102441955
IGD=0.1589806550 vs 0.0578042420
ΔHV=-31.55%
ΔIGD=-175.03%
```

F2遥测部署审计发现旧诊断schema对Teacher/PDDR较强、CA-TA部分、CFVF事件级不足，因此不能用它给完整根因定论。随后工具经历多轮真实接线、等价门和终止快照纠错；最终250k诊断能运行，但FC5仍未形成单一因果根因。

Teacher Exposure Calibration的源码审计发现：

- Qp四动作各映射唯一候选，结构上没有动作内候选重选空间；
- Qg action0/1也是单例；唯一多候选点为Qg action2二元锦标赛；
- 该点仅占全部教师事件约1.12%，自身已高度分散（Hn≈0.985、top1≈1.4%）；
- 真正集中的historical缓存是单例，无法注入；
- 因而lambda repair没有可达杠杆，32条Race在实现前关闭：`REPAIR_FAMILY_NOT_PURSUED_STRUCTURAL_NO_LEVERAGE`。

这只是否决该注入点，不是否决Teacher假设或Dual-Q。

---

## 19. 外部baseline fair-ready与production preflight

NSGA-II-F、SPEA2-F：

- 官方性绑定GitHub jMetal 5.8 tag/commit与MIT license；排除本机重建sources jar作为来源；
- 隔离核心仅包名/泛型接线差异，搜索算法语句不增强；
- 四向量Problem/算子最小适配；
- 静态扫描禁止引用P25D、CFVF、DSCR、CA-TA、PDDR等；
- 2k身份测试与20k production preflight通过；
- 20k真实身份计数：NSGA-II crossover 9950、mutation/tournament各19900；SPEA2 crossover/mutation各19900、tournament39800；
- 六条20k exact FE、初群配对、原子输出、清单与故障注入最终通过；
- 故障注入曾发现Windows kill只杀启动器不杀JVM，后改为进程树杀并保留首次缺陷证据。

这些只证明外部基线可用于正式实验，不证明其论文性能结论。

---

## 20. Gap Probe V2

### 20.1 设计

```text
algorithms=A4-Pacing,A0/HMOPSO-QGS-F,NSGA-II-F,SPEA2-F
instances=50_2_3_1,100_5_3_1
seeds=20260827,20260906
MaxFEs=500000
runs=16
```

4×20k机制贯通先通过；16×500k全部完成。外部臂在100-job因观测账本保留解码级对象先后以16/32/56g OOM，保留失败attempt后用100g堆完成；这属于外部适配器内存债，不是搜索机制变化。

### 20.2 结果

50-job：A4对A0 HV/IGD领先约50.5%/82.2%；对NSGA-II HV领先16%、IGD领先67.6%；对SPEA2 HV落后约4%但IGD领先38.5%。

100-job困难实例：A4相对SPEA2 HV/IGD落后63.5%/260.7%，相对NSGA-II落后67.8%/311.3%；A0也落后官方核。最强external按冻结联合均秩为SPEA2-F。

结论：小/中规模有竞争力，困难大规模出现严重覆盖崩塌；不能启动正式矩阵。

---

## 21. Local-FE Pacing repair完整链

### 21.1 杠杆选择

审计发现A4在正常/困难实例的机制计数几乎固定，局部搜索约占38% FE，且62.3%局部预算落在250k之后；困难实例的best Cmax早已停止改善。选择`betaMax`单旋钮，不改CA-TA语义：

```text
betaMin=0.25
C0=0.65 current
C1=0.55
C2=0.45
C3=0.35
```

正式Jar不动，使用独立repair Jar与专用Runner；C0必须与A4逐位等价。

### 21.2 20k

10/10单条实现门通过，C0==REF、FE回流和机制触发成立。但因exact-stop恒等式，C1/C2/C3 totalLocalFE均4900，严格剂量门后来勘误为 `NOT_RESOLVED`。

### 21.3 50k

16/16通过；localFeShare `.3764>.3364>.2980>.2842`。早期报告只让C3晋级，后发现C2被一个约0.235pp、且口径不对称的TWC符号翻转误杀，append-only勘误为C2/C3均进入250k。

### 21.4 250k

18/18、6公平组、checkpoint observer等价门通过。C2失败；C3终态四门通过但正常实例在100k/150k出现3/3 seed一致HV退化（-6.87%/-5.15%），终态仅+0.19%，触发检查点冲突。因此：

```text
NO_REPAIR_CANDIDATE
LOCAL_FE_PACING=PILOT_REJECTED
betaMax remains 0.65
```

绑定文件曾把setup SHA抄成63位导致9条启动失败；修复只补齐期望哈希，科学输入字节不变。首次失败臂日志被重试覆盖，摘要保留并在治理报告中诚实登记。

---

## 22. 最新 Pareto Coverage Leverage Audit

这是纯只读0-FE审计，固定优先H1 PDDR/覆盖、H2生命周期、H3 Teacher、H4 CFVF/Qp来源。

结果：

- observed-only ratio在多数窗口存在，但指纹不同常对应相同目标三元组；
- potential HV recovery最大0.79%，0/90达到2%；
- 困难相对正常的ratio差反而-5.21pp；
- Teacher困难-正常top1Share仅+1.75pp，远低于20pp门；
- 250k候选级PDDR和CFVF/Qp来源级ND/HV贡献未导出，不能定因；
- 最终 `NO_ACTIONABLE_LEVER`，无root-cause candidate。

允许表述：现有观察更符合“生成侧中后段多样性不足”；禁止表述：已证明某个机制是根因、PDDR完全无关、Teacher/CFVF/Qp已排除。

---

## 23. 训练机实验地图

训练机：`aic-inspur-home`，根目录 `/home/inspur/aicomp`。主要目录：

| 目录 | 内容/状态 |
|---|---|
| `zhangbo-v35-p25e-corrected-50k-20260815` | 忠实八算法适配先导，展开保留 |
| `zhangbo-fc6-20260818` | FC-2至FC-6参数裁决权威源 |
| `zhangbo-fc6a4-order-20260820` | CURRENT vs ORDER_SWAP权威源 |
| `zhangbo-fc6b-region-20260820-r3` | Region-aware负结果权威源 |
| `zhangbo-v35-doe1-20260820` | DOE1开发，已归档后可清展开 |
| `zhangbo-v35-doe1-heldout-20260822` | DOE1 held-out确认 |
| `zhangbo-v35-stage2-phasebudget-20260823-r3` | Gate3/预算/并发权威源 |
| `zhangbo-v35-stage2-master-v2-20260823` | Stage2 12组60条先导与暂停矩阵 |
| `zhangbo-v35-gap-probe-v2-20260830` | 16×500k四算法Gap Probe |
| `zhangbo-v35-local-fe-pacing-50k-20260831` | 50k剂量筛查 |
| `zhangbo-v35-local-fe-pacing-250k-20260831` | 250k repair否决 |

历史P8/P9 Shift、P25D增强比较器、FC6B早期重试、phase-budget初版/r2等已归档或清理展开副本，不能用于当前论文效果结论。完整地图以 `docs/PAPER_EVIDENCE_MASTER/REMOTE_EXPERIMENT_MAP.md` 和 `remote-location-map.csv` 为准。

---

## 24. 证据资格与论文措辞

### 24.1 证据分类

| 类别 | 示例 |
|---|---|
| `MAIN_METHOD_EVIDENCE` | P1来源、四向量、FM3、机制代码与公式 |
| `PAPER_PARAMETER_SELECTION` | FC-2/4/6、DOE1及held-out |
| `PILOT_DIAGNOSTIC` | P25E、Stage2五臂、Gap Probe、修复筛查 |
| `NEGATIVE_RESULT_APPENDIX` | gb15、压力mask、ORDER_SWAP、REGION_AWARE、Qp诊断、pacing失败 |
| `REPRODUCIBILITY_ONLY` | 构建、预算、快照、吞吐、哈希和故障注入 |
| `LEGACY_EXCLUDED` | Shift-on、旧压力语义、旧P9、P25D |
| `TEMPORARY_OR_RETRY` | partial、失败attempt、重复上传 |

### 24.2 当前可以写

- FM3和I0/I1公式/逐字段工程验证；
- DSCR、CFVF、Qp/Qg、谱系档案、CA-TA已真实接入当前Java路径；
- DOE1没有找到稳定优于20/40/20/20的配比；
- A3单独负向，A4在某些实例/seed恢复甚至超过A2，但跨实例稳定性不足；
- 100-job困难实例存在可复现的Pareto覆盖崩塌；
- PDDR overflow假设未被250k支持；
- 多种修复（区域PDDR、顺序反转、teacher lambda、local-FE pacing）经预注册门被否决。

### 24.3 当前禁止写

- “V35在45实例显著优于所有baseline”；
- “A4已经Final”；
- “Qp或双Q已证明为独立正贡献”；
- “PDDR已确认是bug/根因”；
- “压力诊断已正式启用”；
- “当前是PF-SDST”；
- 用P25D、Shift-on、非配对Stage2或partial构建PFref；
- 把5seed/单实例/开发集结果当论文显著性结论。

---

## 25. 已归档的失败路线及其科学价值

| 路线 | 失败/停止原因 | 可保留价值 |
|---|---|---|
| gb15 | 100-job HV/IGD明显退化 | 说明保持P5/G5 |
| 方向top-k教师池 | A5未稳定优于A4 | 模块选择负结果 |
| 压力严格mask | held-out漏失率越门 | 说明BAL回退 |
| Cheap-Test | 未转正 | 局部搜索筛选 |
| soft-freeze rho>0 | 100-job否决 | 保持rho=0 |
| ORDER_SWAP | Cmax改善但IGD失败 | 多目标权衡示例 |
| BP_RESERVED | 污染/结构性失败 | 不回主线 |
| REGION_AWARE | 尤其100-job严重退化 | 不用固定区域生存配额 |
| Qp reward clipping | 公式正确但性能不晋升 | 公式/效果分离 |
| Qp cold-start tie | 动作改变但改善不过门 | 最小假设否证 |
| Teacher lambda | 注入覆盖约1.12%，无杠杆 | 实现前停止的范例 |
| Local-FE pacing | 50k有信号，250k检查点冲突 | 不靠终态小幅改善误判 |
| Archive K25/K50/K100/K200 | 未满足启动门 | 休眠而非失败 |

---

## 26. 后续完整路线（当前仅为建议框架，不自动授权）

### 26.1 先作战略选择

由于最新审计没有可操作杠杆，不能直接再开四档参数赛马。建议先在两条战略中选一条：

**路线A：最小诊断补洞。** 只实现纯观察的250k候选来源归因：每个生成源→merge→PDDR→working→teacher→offspring improvement，覆盖正常/困难实例、A0/A4或最终候选；行为等价通过后再决定是否存在单变量生成侧修复。无候选就停止。

**路线B：投稿优先。** 接受当前完整A4在困难规模不稳定，把失败写成负向消融；不再无限追根因，先完成FM3模型实验、剩余作者算法fair-ready、论文Methods/Problem/复现章节和一套规模可承受的正式对比设计。若必须保留三项创新，则将主张收缩为“组合框架及其适用边界”，而不是声称普遍支配。

### 26.2 如果未来产生新的V35-R候选

1. 只选一个repair family，C0–C3必须同一轴；
2. 100k fast reject；
3. Top2×4实例×3seed×250k；
4. Top2+QGS+最强external×4实例×2seed×500k；
5. 产生 `PROVISIONAL_V35_R` 后做DOE迁移门，而非自动重做DOE1；
6. 用未污染50/100/150或200-job做Validation；
7. Champion Gate通过才Final Freeze。

### 26.3 DOE迁移

先比较四配比：20/40/20/20、30/50/10/10、25/25/25/25、20/40/30/10。当前配比仍稳健第一即停止。只有多个challenger广泛占优、交互>2个百分点或大面积反转才重做15-treatment DOE。DOE2 Pacing不自动启动。

### 26.4 Final消融

Final Freeze后优先：

```text
FULL V35-R
FULL - CFVF
FULL - DualQ
FULL - CA-TA
FULL - Final Coordination
HMOPSO-QGS-F
```

每个删项先做依赖合法性审计；无法单独关闭时按bundle删除并诚实名命。首档6实例×10seed×6臂=360条500k，只有方差/CI/审稿需要才扩。

### 26.5 正式baseline

目标roster：HMOPSO-QGS-F、HMOPSO-QLS-F、MOPSO-F、MOPSODS-DE-F、MOHEADE-F、NSGA-II-F、SPEA2-F、V35-R-Final。QMOEA继续PENDING。其余作者算法必须补齐与NSGA-II/SPEA2同等级的源码身份和20k production preflight。

建议Formal Stage 1：8算法×9实例×5seed=360条500k；通过后Formal Main：8×45×10=3600条。是否补20seed由方差、置信区间、排名稳定性与功效预注册决定，不自动翻倍。

### 26.6 统一reference与统计

每个实例等全部正式算法完成后再构造 `PFref=ND(all algorithms×all runs)`；统一ideal/nadir与HV参考点 `(1.1,1.1,1.1)`。报告HV、IGD/IGD+、双向C-metric、三目标极值；Spacing/frontSize/runtime/FE利用率作诊断。

统计先在每个instance内聚合seed，再以instance为主要配对单元；多算法Friedman、两两paired Wilcoxon、Holm、多报告paired rank-biserial effect size。不得把45×seed当成完全独立问题。

### 26.7 FM3独立模型实验

算法比较不能证明疲劳模型合理。另行固定搜索算法，比较FM0→FM1→FM2→FM3，报告Cmax/TEC/TWC、Fmax/Favg、暴露、恢复、实际工时、人员负荷和有限lambda/mu/r敏感性。该实验与算法消融分开。

---

## 27. 权威文档和证据入口

阅读顺序：

1. `AGENTS.md`——最高协作纪律；
2. `docs/ROADMAP.md`——append-only全部决策；
3. 本文件——跨阶段上下文；
4. `docs/PAPER_EVIDENCE_MASTER/CURRENT_SCIENTIFIC_STATE.md`——当前科学状态；
5. `docs/PAPER_EVIDENCE_MASTER/CLAIM_EVIDENCE_MATRIX.md`——claim资格；
6. `docs/PAPER_EVIDENCE_MASTER/REMOTE_EXPERIMENT_MAP.md`——训练机目录；
7. `docs/PAPER_EVIDENCE_MASTER/LEGACY_AND_NEGATIVE_RESULTS.md`——淘汰路线；
8. `docs/V35_COMPETITIVE_SUPERIORITY_EXECUTION_ROADMAP.md`——竞争优势/DOE/正式实验设计；
9. 各 `docs/evidence/<campaign>/` 原始证据与SHA清单。

主要证据目录：

```text
docs/evidence/P1 ... P9
docs/evidence/V35-P0 ... V35-P25E
docs/evidence/V35-P26
docs/evidence/V35-DOE1-subgroup-mixture
docs/evidence/V35-STAGE2-*
docs/evidence/V35-ND-ARCHIVE
docs/evidence/V35-A2-A3-DECOMPOSITION
docs/evidence/V35-A3-D2-QP-SETTLEMENT
docs/evidence/V35-A2-A4-MULTIINSTANCE-CONFIRMATION
docs/evidence/V35-A2-FINAL-CANDIDATE-CONFIRMATION
docs/evidence/V35-FC5-100JOB-TRANSFER
docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS
docs/evidence/V35-PFC5-GAP-PROBE
docs/evidence/V35-GAP-LEVERAGE-AUDIT
docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR
docs/evidence/V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT
docs/evidence/V35-EXTERNAL-BASELINE-FAIR-READY
docs/evidence/V35-EXTERNAL-BASELINE-PRODUCTION-PREFLIGHT
```

---

## 28. 给接手AI的具体任务要求

在提出下一阶段前，请先输出：

1. 你对当前“没有Final、A4大规模覆盖崩塌、PDDR overflow未证实、pacing修复被否、无actionable lever”的复述；
2. 你选择“继续最小诊断”还是“投稿优先”的理由；
3. 若提实验，写清唯一变量、实例角色、未使用seed、预算梯级、停止门、预期可证伪结论、运行数和证据目录；
4. 若提算法修改，说明为何不重复ORDER_SWAP/BP/REGION/Teacher-lambda/pacing/Qp-tie等已否决路线；
5. 若提DOE，说明为何触发DOE迁移门，而不是直接重做135+60；
6. 若提正式实验，先证明Final Freeze、外部算法fair-ready、production preflight和reference协议均已满足；
7. 明确哪些结论能写入论文，哪些只能写pilot/negative/engineering evidence。

不得只给“再调参数”“再跑更多seed”“换PDDR试试”这种没有预注册因果边界的建议。

---

## 29. 最终状态快照

```ini
formalAlgorithmJarSha256=8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
decoder=FM3
ShiftMode=NONE
familyMode=DEGENERATE_SINGLE_FAMILY
setupMode=SEQUENCE_INDEPENDENT
objectives=0,1,6
mixture=20,40,20,20
PDDR=GLOBAL_ORIGINAL
archive=UNBOUNDED_FULL
localSearchOrder=CATA_THEN_INHERITED
dualQ=P5_G5_RHO0
directionTeacherPool=false
pressureStrictMask=false
PFSDST=false

A2Promoted=false
A4Promoted=false
FinalCandidateApproved=false
FINAL_FROZEN=false
localFePacingRepairFamily=PILOT_REJECTED
paretoCoverageAudit=NO_ACTIONABLE_LEVER
rootCauseCandidate=NONE
newRepairImplemented=false
newExperimentStarted=false
DOEStarted=false
validationStarted=false
formalMatrixRunning=false
formalMatrixPaused=true
formalPFrefGenerated=false
formalStatisticsGenerated=false
sampledReproductionAccepted=false
fullReproductionAccepted=false
```

本档案的核心结论不是“项目失败”，而是：**模型与机制工程已经相当完整，证据治理也很强；但现有完整A4在困难100-job上存在可复现的Pareto覆盖问题，多个直觉修法均被科学否决，因此尚未达到Final与论文正式优越性门。下一步必须在“补最小诊断缺口”和“收缩主张、优先形成可投稿证据”之间作明确战略决策，不能继续无边界地加机制或调参数。**

---

## 30. 当前真实运行调用顺序与组件依赖

正式A4的概念链应按下列顺序理解，不能把组件当作互不相关的插件：

```text
实例/SUT/疲劳参数 + 共同四向量初群
→ FM3完整解码，得到Cmax/TEC/TWC和逐工序疲劳轨迹
→ 父代、谱系个人档案、Qp状态快照
→ 冻结当前Qg社会候选快照
→ DSCR清洗previous/historical缓存中被严格支配的教师
→ Qg选择社会leader
→ Qp从本谱系个人档案选择认知策略/leader
→ CFVF更新JS/FA/MA/WA
→ 每个新候选仅一次FM3完整评价
→ 在任何局部搜索之前结算Qg并批量结算Qp
→ 合并全局后代、父代及合法局部carrier
→ GLOBAL_ORIGINAL PDDR选择下一工作种群及对应历史
→ 谱系保留/分裂/删除/迁移，重建Qp pbest
→ 打开共享local-FE窗口
→ CA-TA-Lite Test/Apply/Re-test
→ inherited inter-factory / O1–O9局部搜索
→ 被动档案、Best-Ever和审计旁路更新
```

重要依赖：

- DSCR依赖Qg，但不改变Qg的状态/动作/奖励；
- Qp依赖谱系档案、PDDR、Qg和CFVF；
- A4中的CA-TA依赖A3链和FM3轨迹，不能随便构造“关Qp但保留原CA-TA”的伪反事实；
- CA-TA和inherited LS共用local-FE硬配额，顺序实验因此是有意义的单变量，但已经被FC6否决；
- local search结果可进入谱系档案，但不得回写为本轮Qg/Qp奖励；
- passive archive/observed front、PDDR工作种群和个人档案是三个不同对象，不能互换。

---

## 31. 核心参数与可核验身份

| 项目 | 当前值/状态 |
|---|---|
| 算法语义标签 | `v3.5-dscr-scope-converged`（历史正式语义标签） |
| 解码语义 | `pf-sdst-fatigue-v3.5-no-shift`，但正式仍是退化单族/序列无关setup，不可称真实PF-SDST |
| Qg | 每角色2状态×3动作：previous、historical、current tournament |
| Qp | 16状态×4动作；alpha=.30、gamma=.80、epsilon=.30→.05、tau=.15 |
| Qp奖励权重 | dominance/direction/archive/fatigue约`2/1/.5/.25`，历史A3/A4为LEGACY_UNCLIPPED |
| 个人档案 | capacity=6，normalization eps=1e-12，duplicate eps=1e-4，indicator kappa=.05 |
| 双Q | warmup=.10，P=5，G=5，rho=0 |
| DSCR | `PRE_ACTION_DOMINANCE_SAFE`、`FROZEN_QG_SOCIAL_CANDIDATES` |
| CA-TA | `LIGHTWEIGHT_MACRO_5`，Test=1，explore=.10，stagnation/retest=3 |
| 正式pacing | betaMin=.25，betaMax=.65，`beta(u)=.25+(.65-.25)u²` |
| PDDR | `GLOBAL_ORIGINAL`，三目标严格最小化 |
| 正式Jar | `8DAD8F40...B6BAD8B9` |
| local-FE实验Jar | `a0788580684cff71ecc526e0f23d6e186dcd9131aad98776c25419378dc7331c` |
| 250k checkpoint实验Jar | `c2cf429422b43c785509d94d7a35bfded7ab2d74e1b4054b620f972640135758` |
| 外部preflight Jar | `966da3d2d23842f4ea5892e8da57404c88b076be2f9fcb568b54953f525447d9` |

不要自行补写尚未在manifest冻结的A0–A4 runtime profile hash；历史审计曾明确某些master profile hash仍是runtime生成而非全局正式冻结字段。

---

## 32. 早期工程与先导的补充精确记录

### 32.1 P1来源快照

- 作者jMetal 5.8工作树复制为只读基线和可写副本；
- 约1806个文件、45实例进入来源清单；
- 原作者651个旧测试中存在3个由默认`numberOfFactories=0`导致的错误，诚实标记为author_actual，未伪称全绿；
- 项目保持Java 8六模块Maven结构。

### 32.2 P7/P8接线纠错

- P7.2初版144个上下文工程测试通过，但Test/Apply和逐后代接入不正确，旧证据隔离；
- corrected版形成228条工程记录，修正合法mask、全局后代Apply、预评价标记和FE；
- P8初版34标签仅16暴露，48条运行，18个`NOT_EXPOSED`，因此不能称P8完成；
- P8.1后形成34标签×2实例×3seed=204条工程记录；
- P8.3修复同一父粒子/候选重复评价和代价信用，所有旧P9结果标记`legacy_pre_cata_apply_fix`；
- 修复后20k FULL/BASE时间比约3.93×、100k约5.04×。

### 32.3 旧P9为何看起来非常强却不能使用

旧`20_2_3_1` 500k FULL vs QGS以及扩展6seed曾显示FULL在HV/IGD和三极值显著领先，但使用了后来被纠正的CA-TA、Shift/计时或基线语义；运行时间一度约39×。这些数据只能解释为何项目继续推进，不能进入当前reference或作为“已复现论文优势”。

### 32.4 P21–P23先导

- P21单实例单seed六梯级在D-074纠正预评价/FE后，大致显示DSCR +0.024 HV、CFVF -0.054、Qp +0.049、CA-TA +0.032、教师池 -0.024；这些相邻数值只作开发线索；
- P22三个10-job实例中FULL的HV比约1.001/0.998/1.056，Cmax均有信号；
- P23的3-job两臂都达到exact front，5-job FULL的IGD约改善17倍；属于小实例正确性/尺度证据，不能外推到100-job。

### 32.5 P25E先导

在忠实适配50k五seed中，A4相对QGS曾约HV +38.6%、IGD -35.7%，但仍落后官方NSGA-II/SPEA2。它同时说明“相对作者基线有改进信号”和“离强经典核仍有差距”，不应只截取前半句。

---

## 33. 当前文档冲突和引用优先级

由于项目坚持保留历史，以下旧状态会在文件中继续出现：

1. `CURRENT_SCIENTIFIC_STATE.md`前段和ROADMAP D-110曾写`gapProbeStarted=false`；后续append已记录Gap V2、local-FE 20k/50k/250k和Pareto审计，后者优先。
2. DOE1 README曾写开发/held-out未启动；权威事实是135条开发+最多60条held-out已完成，最终保持20/40/20/20。
3. `50K_REPAIR_DECISION.md`曾只让C3晋级；`12-50k-decision-correction`已改为C2/C3共同晋级，随后250k又把两者全部否决。
4. FC4早期状态写100-job进行中；最终18/18完成并拒绝rho>0。
5. P21原始A3数字被D-074预评价语义纠正；引用时用纠正后版本。
6. Stage2的`formalMatrixStartedHistorically=true`与`formalMatrixRunning=false`不矛盾：矩阵曾启动后暂停。
7. `REMOTE_EXPERIMENT_MAP.md`和早期campaign ledger截至8月23/24，可能缺8月30/31的新目录；Gap/local-FE最新路径以对应证据包和本文为准。

裁决优先级建议：

```text
最新FINAL_DECISION/勘误
→ CURRENT_SCIENTIFIC_STATE后续append
→ ROADMAP最新D记录
→ 对应campaign最终报告
→ 早期状态/启动报告/README
```

---

## 34. 训练机路径补充与数据保留纪律

较新的远端目录：

```text
/home/inspur/aicomp/zhangbo-v35-gap-probe-v2-20260830
/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-repair-20260831
/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-50k-20260831
/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-250k-20260831
```

`/home/inspur/aicomp/zhangbo-v35-source-diagnostics-20260831/`若存在，目前只能当预登记/计划位置，没有已接受运行和科学裁决，不能写成完成。

高价值原始结果至少双副本：G盘冷归档+训练机压缩/项目校验副本。任何缺文件、哈希不符或唯一副本不得删除。失败路径可压缩大型日志，但必须保留状态、配置、真实FE、错误、清单和恢复位置。

---

## 35. “三项创新必须保留”与科学证据之间的准确关系

用户的研究设计希望最终论文保留FM3、CFVF/双Q和CA-TA三项创新。这个目标不能被诊断代理自行删除，但也不能让实验结论失真：

- **FM3**已有较强方法/工程证据，但尚需独立模型实验说明建模影响；
- **CFVF**已真实接入且是四向量搜索核心，但A1→A2和A0→A2跨实例证据不支持简单宣称普遍正贡献；
- **Qp/双Q**已接入，但A3是明显负向中间臂，不能作为独立正贡献；若最终保留，更适合表述为完整协调框架的一部分，并由Final leave-one-out重新评价；
- **CA-TA-Lite**在单实例/部分规模能修复A3并改善Cmax，但A4在困难100-job覆盖崩塌；不能宣称普适提升；
- 正确的研究问题不是“怎样强迫每个模块都单调正向”，而是“完整Final中每个模块或依赖bundle的净贡献、适用边界和失败模式是什么”。

如果最终找不到同时保留三项且通过Champion Gate的V35-R，必须在论文主张、组件组合或投稿目标之间作明确选择，不能靠不断污染开发/验证集来追求预设结论。
