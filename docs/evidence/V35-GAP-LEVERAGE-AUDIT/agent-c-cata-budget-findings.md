# Agent-C 取证报告：CA-TA / inherited LS 预算占用、贡献与可触达单旋钮（H_CATA_BUDGET_COORDINATION）

冻结 A4 = HMOPSO + CFVF + 双Q + CA-TA-Lite(N1–N5) + inherited LS + DYNAMIC_BETA 局部预算。
本报告全部数字来自下列只读文件与源码，无编造；每处给出 文件+字段/行 来源。

数据源：
- D1 = `docs/evidence/V35-PFC5-GAP-PROBE/04-v2-remote-500k-runs/sync/run-GAP500-A4-{50_2_3_1,100_5_3_1}-{20260827,20260906}/`（status.properties / ca-ta-lite-events.log / cmax-audit-curves.csv / cmax-audit-records.csv / bottleneck-pressure-events.csv / configuration.txt）
- D2 = `docs/evidence/V35-FC5-MIDHORIZON-250K/01-root-cause-analysis/remote-results/`（FC5_250K_ROOT_CAUSE_REPORT.md 等）；CA-TA 候选全链路遥测实际位于 `docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/10-real-20k-equivalence/runs/20k-{100_2_4_1,100_5_3_1}-20260901-A4-ON/telemetry-cata-contribution-{events,summary}.csv`（26 列；250k 目录无 cata 遥测文件，已核实 `find *cata*` 结果）
- S = `java-jmetal58/jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mypso/`（v35/ 与 zhangbo/ 及 ZhangBoMOHPSOQ.java）、`jmetal-exec/.../runner/lc_psode/`

---

## 1. 预算占用（D1，按 FE 归一，actualFE = fullEvaluations = 500000）

### 1.1 mechanismSummary 计数（status.properties 第 10 行 mechanismSummary 块）

| run | caTaLiteTest | caTaLiteApply | caTaLiteFE | 占比 | formalLocalFE | 占比 | 合计 | 合计占比 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| A4-50_2_3_1-20260827 | 13023 | 3314 | 16337 | 3.27% | 173563 | 34.71% | **189900** | **37.98%** |
| A4-50_2_3_1-20260906 | 10730 | 3842 | 14572 | 2.91% | 175328 | 35.07% | **189900** | **37.98%** |
| A4-100_5_3_1-20260827 | 10567 | 3894 | 14461 | 2.89% | 175439 | 35.09% | **189900** | **37.98%** |
| A4-100_5_3_1-20260906 | 10668 | 3852 | 14520 | 2.90% | 175380 | 35.08% | **189900** | **37.98%** |

核心结构事实：**四个 run 的 caTaLiteFE+formalLocalFE 恒等于 189900 FE（37.98%）**，与实例、seed 无关——局部搜索家族吃的是一条硬总量约束，CA-TA 只占其中 7.6–8.6%，inherited LS 占 91.4–92.4%。

总账恒等式（由 100_5_3_1-20260827 完整重建）：500000 = 100（初始种群）+ 310000（Q 相 62 周期 × 5000，=cfvfOffspring=310000）+ 189900（局部窗口）。

### 1.2 DYNAMIC_BETA 开窗机制的周期级验证（bottleneck-pressure-events.csv，62 周期×100 父代）

源码：`ZhangBoMOHPSOQ.java` L637-653 `beginLocalFeBudgetWindow()`：每外周期 Q 相结束后开窗，`B_L = floor(beta(u)/(1-beta(u)) * B_G)`，`beta(u)=betaMin+(betaMax-betaMin)*u^2`，u=开窗时 FE 进度；`V35LocalFeBudgetConfiguration.java` L48-60（同一公式）。A4 参数：betaMin=0.25、betaMax=0.65（configuration.txt L61-62；`V35FinalAblationProfile.java` L25-26 常量 LOCAL_FE_BETA_MIN/MAX，L108-111 注入）。

从 100_5_3_1-20260827 的 bottleneck-pressure-events.csv 按 generation 变化重建每周期局部消耗（下一周期 catStartFE − 本周期 catStartFE − 5000）：
- 周期22=1974，周期30=2317，周期40=3006，周期50=4320，周期55=5511，周期60=7621，周期61=8266，周期62=3840（被 maxFEs=500000 截断）。
- 与 beta(u)/(1-beta(u))×5000 逐周期吻合（如周期61：u=482894/500000=0.966 → beta=0.623 → 8267≈8266）。
- 局部(周期1..21)=37118，(22..61)=148942，(62)=3840，合计=189900（精确）；50_2_3_1-20260827 同口径 (22..61)=148946，结构相同 → **调度与实例无关**。
- u² 调度使窗口从头到尾放大 4.2 倍（1.97k→8.27k/周期）：**局部预算的 62.3%（118402/189900）落在 FE≥250000 之后**（周期37..62 之和，catStartFE≥256598）。

### 1.3 窗口内分配次序与 LS 需求

- 顺序冻结：`ZhangBoMOHPSOQ.java` L810-846，CATA_THEN_INHERITED 分支先 `runV35CaTaLiteLocalSearch` 后 `runFormalInheritedLocalSearch`，共享 `localFeHardLimit()`。
- LS 需求恒超窗口：每周期需求 = 2 inter-factory + 30 passes × 9 ops = 272 FE/父代 × 100 = 27200，而窗口仅 1974–8266 → **每周期窗口 100% 被吃满**，CA-TA 先取其 ~8%，LS 拿剩余 ~92% 并在 pass 循环里被 `localFeHardLimit()` 截断（`ZhangBoMOHPSOQ.java` L5811）。formalBaseline.localSearchTimes=30（configuration.txt L83）在 DYNAMIC_BETA 下永不触顶，故调 localSearchTimes 不改变 FE。

### 1.4 A0 反事实（无 DYNAMIC_BETA 的 legacy LS_Times=30）

- A0-100_5_3_1-20260827 status.properties：caTaLiteFE=0，formalLocalFE=419900（**84.0%**），fixedNeighborhoodEvents=417011。
- A0-50_2_3_1-20260827：formalLocalFE=347633（69.5%）。
- 结论：无硬窗时 inherited LS 会吞掉 70–84% 总预算；A4 的 DYNAMIC_BETA 正是把它压到 35%的既有机制——**betaMax 是已被运行时验证有效的预算阀门**。

### 1.5 decoder 时间分解（decoderTiming，status.properties L12）

- 两实例 baseDecodeNanos/decoderTotalNanos≈99.97%（如 100_5_3_1-20260827：31253724090/31282683552；leftShift/rightShift=0）——decoder 侧无局部搜索开销项，局部搜索的解码成本计入总墙钟。
- 墙钟不可靠（跨远程机器）：algorithmRunNanos 50-0827=495.3s / 100-0827=1002.0s / 50-0906=463.2s / 100-0906=821.8s；但 baseDecodeNanos 出现 50-0827=206.8s vs 100-0827=31.3s 的倒挂（0906 又反转）。**结论：FE 计数是唯一可靠货币，nanos 只作定性参考。**

### 1.6 困难实例的 LS 构造浪费（不计 FE，但计时间）

fixedNeighborhoodEvents（O1–O9 尝试事件，含 notApplicable）vs O1–O9 实际 FE（=formalLocalFE−12400 inter-factory）：100-job 269586 vs 163039 → **39.5% 尝试 notApplicable（0 FE）**；50-job 172324 vs 161163 → 6.5%。困难实例上 LS 近四成构造在不可行邻域上空转。

---

## 2. CA-TA 有效性

### 2.1 Test:Apply 比（D1 mechanismSummary）

- 50-0827 = 13023:3314 = 3.93:1（Test 占 CA-TA FE 79.7%）
- 50-0906 = 10730:3842 = 2.79:1（73.6%）
- 100-0827 = 10567:3894 = 2.71:1（73.1%）
- 100-0906 = 10668:3852 = 2.77:1（73.4%）
即 CA-TA 的 FE 约四分之三花在 Test（测后不落地）。Test 数由 `beginTestEpoch` 的 mask×nTest 决定（`V35CaTaLiteController.java` L282-293，nTest=1、mask≤5）。

### 2.2 Apply 接受率

500k 尾窗（ca-ta-lite-events.log 环形保留区，仅 100-job 两 run 含 v35Lite 事件；50-job 尾窗 4096 条全为 formalLocal）：
- 100-0827：action 事件 260 条，accepted=true 65（**25.0%**）；按宏 N1 13/58、N2 34/71、N3 8/22、N4 9/63、N5 1/46。
- 100-0906：238 条，accepted=true 56（**23.5%**）；N1 0/42、N2 31/73、N3 7/30、N4 12/53、N5 6/40。
- 决策 reason 分布（100-0827/0906）：APPLY_BEST 50/56、TEST 38/31、CONSECUTIVE_APPLY_FAILURE_RETEST 7/4、APPLY_EXPLORE 4/4、APPLY_HORIZON_COMPLETE_TEST 1/5；**无一条 RETEST_SUPPRESSED_TEST_SHARE_CAP**（testFeShareCap=1.0 从不触发，字段见 v35Lite decision 行）。
- bottleneck 字段全部为 BAL（FULL_MASK_AUDIT 全掩码诊断），故按 bottleneck 分组无区分度。

全 run 口径（D2 20k real A4-ON，telemetry-cata-contribution-events.csv 全程覆盖）：
- 100_2_4_1：evaluated=426，acceptedLocally=115（**27.0%**）；APPLY 68/165=41.2%，TEST 47/261=18.0%。
- 100_5_3_1：evaluated=473，acceptedLocally=156（**33.0%**）；APPLY 60/155=38.7%，TEST 96/318=30.2%。
- **困难实例局部接受率反而更高**，但下游（见 2.3）更差。

### 2.3 全链路存活率（D2 20k real A4-ON；summary CSV 同口径复核）

| 阶段 | 100_2_4_1（正常） | 100_5_3_1（困难） |
|---|---:|---:|
| evaluated | 426 | 473 |
| acceptedLocally | 115（27.0%） | 156（33.0%） |
| enteredMergePool | 115 | 156 |
| selectedByPddr | 62（**53.9%** of accepted） | 52（**33.3%** of accepted） |
| survivedNextGeneration | 全 NOT_OBSERVED（20k 遥测模式未观测） | 同左 |

困难实例上 CA-TA 接受后候选被 PDDR 丢弃的比例从 46.1% 升至 66.7%——**"接受得多、用得少"**，与"预算消耗多、有效贡献不足"假设一致。
按宏（generated→accepted→selectedByPddr）：100_2_4_1 N1 68→11→1、N2 58→14→4、N3 162→83→52、N4 79→7→5、**N5 59→0→0（全废）**；100_5_3_1 N1 71→18→1、N2 130→51→11、N3 99→54→25、N4 99→12→5、N5 74→21→10。

### 2.4 D2 250k 的情况（注意：250k 目录无 CA-TA 候选链遥测）

- `FC5_250K_ROOT_CAUSE_REPORT.md` §3 四方向生命周期：100_2_4_1 ALL 95.97/74.57/83.19/72.39% vs 100_5_3_1 ALL 94.89/82.80/87.25/74.68%——困难实例不差（H1 被否证）。
- 同报告 §2.5：250k 时 A4 相对 A2 困难实例中位 ΔCmax=+2.50%、ΔHV=−8.29%、ΔIGD=−13.03%，HV/IGD 变差 3/3 seeds。
- 同报告 §5 原文明确把 "**CA-TA与inherited LS预算分配**" 列为 H1 否证后优先级最高的未拆分方向——本审计即对该方向的量化。

---

## 3. inherited LS 贡献与 cmax-audit 轨迹（定性+时点）

- cmax-audit-curves.csv 末次全局改善时点（100_5_3_1）：bestCmaxGlobal 20260827 止于 FE=252000、20260906 止于 FE=129000；bestTECGlobal 470000/495000；bestTWCGlobal 443000/470000。50_2_3_1-20260827 bestCmaxGlobal 止于 352000。
- 结合 1.2：**困难实例 bestCmax 在 FE≈129k–252k 后不再改善，而局部预算的 62.3%（≈118.4k FE）花在 FE≥250000 之后**（周期37..62）。Cmax 主峰停滞段恰是 u² 调度最慷慨的窗口段；TEC/TWC 末端仍有 best 点改善但与 HV/IGD 覆盖崩塌（§2.4）并存，说明末端 LS 产出的是 best 点微移而非前沿扩张。
- cmax-audit-records.csv 机制普查（500k，抽样台账）：50-0827 FIXED_VNS 39 条/pddrRetained 5；100-0827 32/9；100-0906 43/12；**四份 500k A4 台账中 CA_TA 机制记录为 0 条**——CA-TA 接受候选经 pendingCaTaLocalCandidates 以 INTRA_FACTORY_VNS 预评估标记回流（`ZhangBoMOHPSOQ.java` L5517-5525 区域），不作为 CA_TA 出现在台账。inherited LS 的 pddrRetained 5–12 条为局部搜索正面贡献的直接证据（量级小）。
- 尾窗 inherited LS 接受率（formalLocal 行）：50-0827 193/3644 applicable=5.3%（另有 1452 notApplicable）；50-0906 61/3613=1.7%（483 NA）；100-0827 209/3708=5.6%（28 NA）；100-0906 243/3758=6.5%——末端窗口大面积低产。

---

## 4. 可达性判定：结构可注入的单旋钮候选

禁区核对基线：LS 顺序 `V35LocalSearchOrder.CATA_THEN_INHERITED`（`V35FinalAblationProfile.java` L103 显式冻结；`V35ProductionConfiguration.java` L248-250 非默认值才写入 canonical text）；CA-TA 语义（Test/Apply 信用与选择规则，`V35CaTaLiteController`）；softFreezeRho=0.0（configuration.txt L26）；dualQ blockFrozen(0.10,5,5)（`V35FinalAblationProfile.java` L105-107）；subSwarmMixture 未启用。

### a. CA-TA Test 预算份额 testFeShareCap —— 可注入，预建旋钮
- 源码位置：`v35/V35CaTaLiteConfiguration.java` L20-26（字段注释：`FE_Test <= cap * FE_local`，默认 1.0 永不生效，**FC-3 候选 0.20**）、L56-58 `cheapTest()`=(1,1,0.10,3,true,0.20)；执行点 `v35/V35CaTaLiteController.java` L157-161 `testShareExhausted()` + L126-141（RETEST_SUPPRESSED_TEST_SHARE_CAP 路径，本次 500k 中零触发）。
- 注入点：`V35ProductionConfiguration.Builder.caTaLiteConfiguration(...)`（L336）→ `ZhangBoMOHPSOQ.java` L541-542 `new V35CaTaLiteController(getV35CaTaLiteConfiguration())`；**现成 CLI**：`jmetal-exec/.../ZhangBoV35P25EBudgetDiagnosticRunner.java` L83-85 `--cheap-test true`、L253-255 参数解析；canonical text 已内建记录（`V35ProductionConfiguration.java` L239-243）。
- 当前值 1.0 → 可调 0.20。影响：CA-TA Test 占其自身 FE 的 72.6-79.7%，压到 20% 释放约 8-11k FE（总预算 1.6-2.2%）回流给同窗 LS。**单独覆盖率 <10%**；零语义风险（FC-3 预注册候选、单变量、canonical 已支持）。

### b. CA-TA 触发频率 / 80-20 工厂门 —— 不推荐
- 源码位置：`zhangbo/cata/ZhangBoCaTaConfiguration.java` L13 `DEFAULT_NEED_WEIGHTED_PROBABILITY = 0.80`，消费点 `ZhangBoMOHPSOQ.java` L5315-5319 `zhangBoFactoryNeedSelector.select(..., 0.80, ...)`。
- 该值是显式常量但**无私有构造之外的配置通路**（仅 standard()/disabled() 工厂，无 wither），改动必须改源码；且它改变的是工厂/诊断选择语义而非 FE 分配——触碰"CA-TA 语义不得改"禁区边缘。**不作为杠杆。**

### c. Apply 每次候选数上限（applyMultiplier / nTest）—— 可注入，低覆盖
- 源码位置：`v35/V35CaTaLiteController.java` L282-293 `remainingApply = mask.size() * nTest * applyMultiplier`（当前 5x1x1=5）；显式构造参数 `v35/V35CaTaLiteConfiguration.java` L28-31 与 L51-53 `standard()=(1,1,0.10,3)`；wither 预建 `zhangbo/cata/ZhangBoCaTaConfiguration.java` L57-60 `withTestAndApply(tests, multiplier)`。
- 注入点同 a（caTaLiteConfiguration 通路）。当前 1 → 可调 2。影响：CA-TA Apply FE 至多约 2 倍（总预算 2.9%→约 5%），同窗 LS 等量减少。**覆盖率 <10%，仅作组合项。**

### d. inherited LS FE 预算模式 betaMin/betaMax —— 可注入，主杠杆
- 源码位置：`v35/V35LocalFeBudgetConfiguration.java` L31-60（`of(betaMin,betaMax)`、`betaAt(u)=betaMin+(betaMax-betaMin)*u^2`、`localBudgetFor=floor(beta/(1-beta)*B_G)`）；A4 注入 `v35/V35FinalAblationProfile.java` L25-26 常量 0.25/0.65 + L108-111；runner 侧硬编码 `jmetal-exec/.../runner/lc_psode/ZhangBoV35FormalComparisonRunner.java` L207、`ZhangBoV35Fc6Runner.java` L172、`ZhangBoV35Doe1MixtureRunner.java` L123；**现成 CLI**：`ZhangBoV35P25EBudgetDiagnosticRunner.java` L245-250 `--local-fe-budget <betaMin>:<betaMax>`。
- 当前值 DYNAMIC_BETA(0.25,0.65)（configuration.txt L27,61-62）。可调：betaMax 0.65→0.45-0.50（或 betaMin 下调）。
- 影响：直接缩放 189900 FE（**37.98% 总预算**）的局部窗口；u² 调度使削减集中在末端大窗（周期 55-61 每窗 5.5k-8.3k FE，而困难实例 bestCmax 自 FE 约 129k-252k 后零改善）——砍 betaMax 牺牲的正是"末端低产窗"，且不触碰顺序/语义/rho/P5G5/mixture。A0 反事实（1.4 节）证明该阀门方向有效。**覆盖率 ≥10%：是（37.98% 全窗受控）。**
- 附注：`--local-search-times` 在 DYNAMIC_BETA 下不是有效 FE 旋钮（LS 需求 272 FE/父代 >> 窗口 20-83 FE/父代，30-pass 上限永不触顶，见 1.3）；`localFeBudget=null`（legacy）会让 LS 回到 70-84% 占用（A0 实测），方向相反，不可取。

---

## 5. 结论：H_CATA_BUDGET_COORDINATION 可达性裁决

**存在 ≥10% 覆盖率的可触达单旋钮。**

主旋钮（K1）：**DYNAMIC_BETA betaMax（连带 betaMin）**
- 定义：末端外周期局部 FE 窗口占比上限，`B_L=floor(beta(u)/(1-beta(u))*B_G)`，u^2 前向加载。
- 注入点：`V35FinalAblationProfile.java` L26 `LOCAL_FE_BETA_MAX`（或 P25E 诊断 runner `--local-fe-budget 0.25:0.50`）。
- 当前值 0.65 → 建议试验值 0.45-0.50。
- 覆盖率：支配 189900 FE = 37.98% 总预算；其中 62.3% 落在困难实例 bestCmax 零改善段（FE≥250k）之后。
- 证据链：窗口 100% 被 LS 吃满（需求 27200 >> 窗口 1.97k-8.27k/周期）；LS 尾窗接受率仅 1.7-6.5%；CA-TA 仅占窗内 7.6-8.6%——**困难实例的局部预算错配主体是 inherited LS 的末端大窗，而非 CA-TA**。
- 风险：改 beta 即改 A4 canonical hash（预期内，需重新冻结记录）；对 50_2_3_1 也有影响（其 bestCmax 改善延至 352000，缓冲略多）；不动任何禁区（顺序/CA-TA语义/rho/P5G5/mixture）。

辅助旋钮（K2，与 K1 正交可组合）：**testFeShareCap 1.0→0.20（cheapTest）**——预建 FC-3 候选、CLI 现成、零语义风险；单项覆盖约 1.6-2.2% 总预算，用于压缩 CA-TA 的 73-80% Test 占比。
不推荐：b（80/20 工厂门，语义禁区+无配置通路）、c（applyMultiplier，覆盖<10%）、`--local-search-times`（DYNAMIC_BETA 下无效）。

对假设本身的修正：H_CATA_BUDGET_COORDINATION 若表述为"CA-TA 与 LS **合计**预算过多"不准确——两者合计恰好锁定 189900 FE；准确表述是"**inherited LS 在 u² 调度下的末端窗口分配过多而边际产出趋零，CA-TA 仅是窗内小头（约 8%）**"。K1（betaMax）正面作用于该机制。

---

## 6. 关键数字清单（供总控复核）

1. caTaLiteFE+formalLocalFE = **189900/500000 = 37.98%**，四个 A4 run 恒等（各自 status.properties mechanismSummary）。
2. CA-TA 占总预算 **2.89-3.27%**；Test 占 CA-TA 自身 **72.6-79.7%**；Test:Apply 约 2.71-3.93:1。
3. inherited LS 占总预算 **34.71-35.09%**；无预算上限时（A0）达 **69.5-84.0%**（A0-50=347633、A0-100=419900）。
4. 每周期局部窗口 1974→8266 FE（周期 22→61，u² 调度），LS 需求 27200/周期，窗口 100% 吃满；周期 62 被 maxFEs 截断为 3840。
5. 局部预算 **62.3%（118402 FE）落在 FE≥250000 之后**；100_5_3_1 bestCmaxGlobal 末次改善 FE=252000（0827）/129000（0906）。
6. CA-TA Apply 尾窗接受率 25.0%/23.5%（100-job 两 run）；20k 全链路：接受率 27.0%（正常）/33.0%（困难），接受→selectedByPddr 存活 **53.9% vs 33.3%**；100_2_4_1 的 N5 全程 59 评估 0 接受。
7. inherited LS 尾窗接受率 1.7-6.5%；100-job LS 尝试 **39.5% notApplicable**（269586 事件 vs 163039 FE）。
8. 500k cmax-audit-records：FIXED_VNS 32-43 条/pddrRetained 5-12；**CA_TA 机制记录 0 条**。
9. 250k（D2 报告）：A4 vs A2 困难实例 ΔHV=-8.29%、ΔIGD=-13.03%（3/3 seeds 差）；报告 §5 原文将"CA-TA与inherited LS预算分配"列为下一优先方向。
10. 旋钮现状：betaMin=0.25/betaMax=0.65（DYNAMIC_BETA，configuration.txt L61-62）；testFeShareCap=1.0、nTest=1、applyMultiplier=1（standard()）；needWeightedProbability=0.80；CLI 注入通路 `--local-fe-budget`/`--cheap-test` 已存在于 ZhangBoV35P25EBudgetDiagnosticRunner。
