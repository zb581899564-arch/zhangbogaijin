# 张博改进项目协作规则

本文件是 `E:\学习\李明哲-毕业材料\张博改进` 项目的最高优先级协作规则，适用于后续所有开发者、Codex 和其他 AI Agent，并对本目录及全部子目录生效。

项目最终目标是：

> 以李明哲第四章 HMOPSO-QGS 的论文语义和完整 Java 实验代码为来源，在隔离副本中建立不继承作者已确认缺陷的规范生产基线，并完成“序列无关设置时间下的动态疲劳解码”“Qp/Qg+DSCR+CFVF认知—社会全向量搜索”和“CA-TA-Lite五宏邻域局部搜索”三项正式研究机制，形成可追溯、可测试、可重复的 Java/jMetal 5.8 研究实现。产品族序列相关设置时间只保留占位，不在当前正式比较中启用。

“项目能够编译”“小算例能够运行”“单元测试通过”都不等于算法已经与论文对齐，更不等于论文级实验已经完成。

## 1. 工作范围与写入边界

- 项目根目录：`E:\学习\李明哲-毕业材料\张博改进`
- 唯一主路线图：`docs/ROADMAP.md`
- 后续所有代码、配置、测试、日志、报告和生成结果只能写入本项目目录。
- 可以只读访问父目录及 `E:\学习\ziliao` 中的论文、Markdown 方案、Java 原件、算例和作者结果。
- 禁止修改、删除、重命名、覆盖或“清理”以下原始材料：
  - 李明哲学位论文；
  - 第三章、第四章原 Java 工程；
  - 作者算例、历史输出和实验结果；
  - 两份期刊 PDF；
  - 用户提供的五份 Markdown 方案。
- 任何兼容修复、插桩、随机性控制或算法改进只能进入本项目中的隔离副本。

P0的“只建立治理文件”限制已经结束。当前允许范围以 `docs/ROADMAP.md` 登记的唯一在途工作包为准；不得越过工作包验收门提前实现后续机制。

## 2. 必读资料与优先级

开始任何后续任务前，依次阅读：

1. `AGENTS.md`
2. `docs/ROADMAP.md`
3. 与当前工作包直接相关的论文、方案和源代码
4. 当前工作包已有的来源清单、冲突登记、测试报告和 manifest

涉及V35-P25–P28、500000 FE、多实例、多seed、指标、统计或论文图表时，还必须完整阅读`docs/V35_FORMAL_EXPERIMENT_ROADMAP.md`。该文件是总路线图的实验子路线图；它不能覆盖`docs/ROADMAP.md`，但实验协议、矩阵、reference、统计和制图必须以它为唯一细则。

V35-P25E及后续论文算法对照必须执行“共享问题、隔离搜索机制”：只允许共享实例、四向量表示、规范FM3解码、ShiftMode.NONE、FE、初始种群和指标。禁止以统一重写的搜索引擎替代NSGA-II、SPEA2或论文作者算法；旧`V35P25DComparativeEngine`只作历史工程诊断，不得进入论文reference。官方jMetal核心须记录上游提交、许可证和差异，论文作者核心只允许Problem/Solution/初群/随机源/FE/日志白名单适配。

### 2.1 创新方案优先级

发生冲突时按以下顺序裁决：

1. `E:\学习\ziliao\v3.5.md`（当前正式主线）
2. `E:\学习\ziliao\HMOPSO_QGS_疲劳_全向量双Q_CA-TA-VNS_综合改进方案_v2.md`（v3.5未覆盖部分）
3. 三份细节方案：
   - `基于李明哲HMOPSO-QGS的多技能疲劳恢复与双Q引导改进方案.md`
   - `HMOPSO_Qpbest_认知社会双引导完整设计方案.md`
   - `李明哲第四章VNS改进方案_代价感知上下文自适应VNS.md`
4. 参考路线：`Codex_Java_jMetal_HMOPSO_QGS_编解码优先实施方案.md`

v3.5已明确关闭或后置的内容，不得从v2或旧细节方案重新引入；旧规则只能在历史证据段落中引用。

### 2.2 原算法语义优先级

第四章 HMOPSO-QGS 原始基线按以下顺序解释：

1. `E:\学习\eswa2026-最新李明哲第四.pdf`
2. `E:\学习\李明哲-毕业材料\3.毕业论文\104_2022930913_李明哲.pdf` 第四章
3. 李明哲第四章当前 Java 工作区的实际行为

`E:\学习\smejms-Zhang2026可能第三.pdf` 只用于理解第三章算法及其演进，不得覆盖第四章的编码、解码、工人分配、设置时间或算例定义。

论文与 Java 不一致时必须保留两条语义并登记差异，不得自行选择更有利的结果。

### 2.3 规范生产、作者诊断与论文验证三线

- 李明哲当前 Java 工程继续认定为完整实验来源，但其中已确认的 WA 全零、MA 未进入活动路径、固定资源域、静态共享状态和未控随机性不得再进入正式生产或消融路径。
- 原作者类、P1只读基线及P4.1机械派生证据保持不变；旧 `MOHPSOQ + EDHHFSPW` 行为只能通过 `A0_AUTHOR_DIAGNOSTIC` 显式入口运行，不参加正式消融、参考前沿或论文结论。
- 正式生产调用链使用张博专属 Builder/Algorithm 与独立 `ZhangBoCanonicalProductionProblem`；所有创新关闭时必须退化为 `deterministic_canonical` HMOPSO-QGS，而不是 `author_actual`。
- `deterministic_canonical` 必须保留论文基线的原 Q-gbest、评价后 PDDR-FF、原工厂间搜索、O1–O9和三主目标，同时使用正确的四向量身份映射、实例SUT、显式第一阶段MA/WA及可注入随机源。
- `PublishedHmopsoQgs/DhhfspProblem` 及P2–P4成果继续标记为 `paper_verification_baseline`，承担论文黄金算例、公式和算子oracle；P3固定时长微调/右移和P8.4疲劳移位均不得接入v3.5正式生产，统一保留为历史诊断。
- 正式Builder、P8注册表和实验Runner禁止选择 `AUTHOR_ACTUAL`、作者巨型更新器或 `DefaultIntegerPermutationSolution`；若需要作者缺陷诊断，必须进入独立目录并显式标记为不参与比较。

### 2.4 P8.5审计后的正式基线与证据边界

- 正式`B0/B1/HMOPSO-QGS-F`必须绑定`formal-hmopso-qgs-v1`运行时配置；配置文本中的`r1/r2`、FA/MA/WA交叉与变异率、`Q_Times`和`LS_Times`必须由同一不可变对象驱动实际循环，禁止只写日志而不执行。
- 正式基线使用结构化Fig.5/Fig.6更新器、原Qg、严格PDDR-FF、关键工厂交换/插入和O1–O9；不得重新启用作者巨型缺陷更新器。
- PDDR的支配关系必须是三主目标`[0,1,6]`上的严格Pareto支配；目标完全相同的解互不支配，再按稳定指纹或输入顺序破平。
- 34个论文消融标签允许出现跨矩阵的完全相同物理配置，但只有完整机制向量哈希相同才可复用，并必须登记别名来源；禁止为追求“每个标签都不同”而人为增加无科学意义的开关。
- CA-TA正式性能运行使用真实单调时钟；确定性时钟只用于语义回放、论文图和单元测试。真实时钟参与代价信用，因此不得宣称正式动作轨迹字节级重放。
- P8.5只完成工程审计、I1解释链、34标签开关审计和20k烟测；P8.6已完成共同空档移位历史线及I0新版粒子适用性冻结，但不属于v3.5正式生产。I0本人手算提交、v3.5当前语义pilot、500000 FE和正式统计矩阵仍是独立后续门。

## 3. 固定技术决策

- 实现语言：Java。
- 优化框架：复用原作者工程中的 jMetal `5.8`，不升级到 jMetal 7.x。
- Java 编译目标：保持原工程 Java `8` 兼容配置；优先使用本机现有 JDK/Maven 验证，不强制安装或切换到 JDK 21。
- 构建工具：沿用原工程 Maven 多模块结构，在工作副本中做最小兼容修复。
- 原作者工程必须先建立净化、只读、带 SHA-256 清单的 jMetal 5.8 快照。
- 实际开发工程必须由该快照复制产生，不得脱离作者代码另写一个无来源的新算法。
- 不引入 jMetal 7.x API、组件架构或与本项目无关的新框架模块。
- 路径必须项目相对化，禁止新增 `E:\DHFSP` 等作者机器硬编码路径。
- 优先进行最小、可验证的兼容整理；不得借机重构或修复与当前验收门无关的算法逻辑。
- 原版与改进版组件必须在同一工作工程中并存且可独立运行，至少包括原始/疲劳解码器、原始/CFVF更新器和原始/CA-TA-VNS；不得用改进实现覆盖唯一基线。

## 4. 语义标签

代码、配置、日志、报告和结果必须使用下列标签之一，不得混写：

- `published_baseline`：按第四章期刊论文和学位论文明确规则实现的基线。
- `author_actual`：作者当前 Java 源码的实际执行行为，包括已确认的缺陷或不受控随机性。
- `deterministic_canonical`：在公开规则基础上采用稳定破平和受控随机源的可重复基线，作为改进算法默认评价语义。
- `fatigue_improved`：加入总体 v2 规定的疲劳累积、自然恢复和加工时间反馈后的改进语义。
- `paper_verification_baseline`：P2–P4按论文重建的黄金测试和语义审计实现，不作为张博创新算法生产入口。
- `author_diagnostic`：隔离执行作者当前Java实际行为，只用于缺陷复现、来源追溯和兼容诊断。

若一个结果同时涉及多种语义，必须拆分为独立运行、独立目录和独立报告。

## 5. 强制开发顺序

旧P0–P8开发顺序只作为历史记录。v3.5正式实现严格遵循：

```text
V35-P0 源码、配置与历史证据冻结
→ V35-P1 原始基线重定级
→ V35-P2 产品族/设置时间占位契约
→ V35-P3 OperationTransitionKernel
→ V35-P4 序列无关设置时间下的动态疲劳Decoder
→ V35-P5 永久冻结Shift正式路径
→ V35-P6 v3.5公平基线与FULL
→ V35-P7 SocialKnowledgeSnapshot
→ V35-P8 DSCR pre-action sanitization
→ V35-P9 DSCR机制门与教师审计
→ V35-P10/P11 ShiftMode=NONE teacher audit与QG0/QG1
→ V35-P12 DSCR DTUR=0机制门
→ V35-P13–P16 CA-TA-Lite五宏邻域与确定性路由
→ V35-P17/P18 Passive Archive与Best-Ever追踪
→ V35-P19 Cmax生命周期审计
→ V35-P20 模型实验（真实PF-SDST需用户批准）
→ V35-P21 算法树消融
→ V35-P22–P28 pilot、精确核验、参数冻结、正式实验、统计与最终验收
```

前一个工作包未达到路线图验收门，不得开始后一个工作包。P4论文验证基线和P4.1作者Java直接派生基线均未冻结前，不得实现疲劳、CFVF、Qp或CA-TA-VNS。

当前 v3.5 主线状态（2026-08-15）：V35-P1--P4 已完成；V35-P5/P7/P8已完成运行时重新验收，V35-P9已完成单实例单seed 100k工程诊断；V35-P10--P19 全部收口（审计、top-k教师池、QG0/QG1、DSCR门、CA-TA-Lite五宏邻域、被动档案、三目标Best-Ever、生命周期审计）；**V35-P21--P24.1 已收口**（六梯级消融、10工件多实例pilot、3/5工件穷举精确前沿核验、最终参数冻结及A3历史值/JDK17回归命令修订，证据见`docs/evidence/V35-P21`至`V35-P24.1`）。P25A旧压力语义结果已隔离；P25B压力阈值held-out门失败，当前保持BAL全开放；P25C三seed/100k只形成`A4_PREFERRED_SIGNAL`工程信号，A4是主版本候选、A5教师池默认关闭。`docs/V35_FORMAL_EXPERIMENT_ROADMAP.md`已建立，但正式20次矩阵尚未启动。规范公平入口使用 `V35FairRunner`，严格固定
`FM3`、`DEGENERATE_SINGLE_FAMILY`、`SEQUENCE_INDEPENDENT`、`ShiftMode=NONE`
和共同初始种群，并要求主版本候选/基线共享Table-9正式外循环、Q/LS时序和FE闭合。DSCR按四个子群方向维护社会教师；CA-TA-Lite 使用独立
`V35MacroCandidateGateway` 的 N1--N5，禁止映射历史 O10--O13。500000 FE 单seed诊断已完成多轮（消融六梯级、三实例pilot、精确前沿核验、三实例预跑），均非统计性证据。**V35-FC-6 Survival 分支已于2026-08-20裁决结束：反转局部搜索顺序因IGD门失败不转正；`REGION_AWARE` PDDR在20-job与100-job均失败（100-job触发一票否决），主线保持`GLOBAL_ORIGINAL + CA-TA-Lite→inherited LS`。下一步必须先人工复核该拒绝结果，未经用户单独批准不得自动进入FC-7/FC-8、P25正式20次矩阵、P26统计或P20 PF-SDST。不得自动调压力阈值、修改教师池或扩大实验；任何机制/参数变更必须先更新冻结清单并全量回归。**

P6.3同步模式启用时的生产闭环每代执行顺序固定为：

```text
四子群划分
→ Qg选择社会领导
→ Qp基于谱系档案选择认知领导
→ CFVF全向量更新
→ 统一疲劳解码
→ 在任何历史更新或局部搜索前分别结算Qg奖励与Qp奖励，并批量提交Qp更新
→ 合并已评价全局后代与已评价父代
→ 评价后PDDR-FF真正替换主种群及对应作者历史
→ 更新作者个人历史与全局非支配集合
→ 按PDDR保留分支更新、分裂或删除谱系档案，并继承或回退个人领导
```

P6.4分块冻结模式的生产闭环保持同一评价/PDDR边界，但在每代领导选择前先按完整FE确定阶段：预热期Qg正常学习、Qp不产生动作或转移且pbest使用方向锚点；P-block冻结Qg的Q值并仅允许Qp学习；G-block冻结Qp的Q值并仅允许Qg学习。冻结方必须继续按当前状态贪婪执行动作并刷新环境状态，但不得累计奖励或提交TD。10%预热预算包含初始种群评价，边界必须按完整代向上取整，P/G块长默认5代且从P-block开始。同步模式不得增加P6.4配置字段。

Qp关闭时，P6.2档案仍是影子记忆，不得参与pbest选择、Qg、CFVF或评价预算；Qp启用时只能通过已验收的谱系档案动作选择认知领导，不得增加完整评价。禁止把后续工厂间搜索或VNS产生的改善反向记入本轮Qg/Qp全局搜索奖励。分块冻结只能表述为降低同步变化导致的非平稳性，不得称为完全因果隔离。

## 6. 编码与解码硬规则

- 第四章采用四向量 `JS/FA/MA/WA`，四个向量长度均等于工件数并按位置对齐。
- `MA` 和 `WA` 直接表示第一阶段资源；后续阶段按论文调度规则确定。
- 不得把“向量位置”误当作“工件编号”。跨粒子比较资源块时必须通过 `JS` 逆映射按工件身份定位。
- 用户本人手算I0及其移位链保留为历史教学证据：5工件、2工厂、2阶段，冻结输入为`paper_evidence/I0/01_input/i0_input.json`。I0的FCLS/FCRS程序结果不得进入v3.5正式Decoder、搜索、reference或论文主实验；提交本人手算副本前仍不得展示程序答案。
- I0在用户提交本人手算副本前只能提供输入、规则和空白表；禁止生成或展示Java解码答案、目标值、甘特图或任何可反推答案的程序结果。提交副本必须先只读冻结并记录SHA-256；P8.6统一解码语义已经冻结，提交后方可生成程序逐项对照。
- 第四章论文的2工厂、10工件、2阶段示例降为`Engineering Golden Instance I1`；Fig.3四向量仍为I1的`X0`。I1继续承担ESWA来源对齐、回归、机制链和工程图证据，但独立Python/Excel公式重建不得再称为“用户本人手算”。I0与I1必须使用不同ID、目录和语义标签，禁止相互复制结果。
- I1的当前生产主解码是显式FM3，退化对照是显式FM0；P3只作共同追加式论文oracle。P8.6的`fatigue-shift-v2-common-gap/LEFT_RIGHT`轨迹统一降为`legacy_shift_on`，不得与v3.5正式`ShiftMode=NONE`结果混写。
- 开始解码前必须逐值固化论文表4、表5、图3/学位论文图4-2和Algorithm 2；开始算子实现前必须固化图5、图6的父本、随机事件和预期结果。
- 任何看不清或无法从论文/Java唯一确认的数据必须标记 `TODO_SOURCE_CONFIRMATION` 并停止相应实现，禁止填入默认 `1.0` 或自行猜测。
- 后续阶段原始解码必须覆盖 `ECT`、`FIFO`、`FAM`、工人可用性、微调和右移。
- 解码必须输出逐工序轨迹，至少记录工厂、阶段、工件、机器、工人、设置时间、加工时间、开始时间和结束时间。
- 每次完整评价只能计入一次评价预算；局部搜索和 Test-and-Apply 的额外完整评价必须显式计数。

## 7. 三个创新点的边界

### 7.1 疲劳解码

- 实现动态疲劳累积、自然恢复、疲劳对加工时长的反馈以及疲劳感知工人选择。
- 严格遵循事件顺序：最早开始时间 → 恢复 → 调整时长 → 完工 → 累积疲劳 → 更新资源可用时间。
- 正式生产模式通过显式枚举选择，不得通过`r_k=0`隐式切换语义。`CANONICAL_NO_FATIGUE`仍读取实例级固化SUT并使用显式第一阶段MA/WA；疲劳模式统一使用`PT0=ST/(MS×WE)`、`SET0=SUT/WE`、`AT0=PT0+SET0`。同一疲劳倍率同时作用于PT和SET，禁止以`0.1×ST`冒充SUT。
- 非零疲劳路径的第一阶段必须按工件身份读取显式`MA/WA`；非法资源、SUT缺失或实例扩展哈希不符时拒绝评价，不得随机修复。
- 每个实例固定 `λ/μ/r/Fwarn/Fsafe`，其中 `δ=r/(λ ln2)` 只作为派生字段，不得独立配置或采样；所有算法和消融共享同一参数清单，候选评价期间不得重新采样。
- 正式标准化场景使用键控SHA-256一次性生成并物化：`λ∈U(0.01,0.03)`、`μ∈U(0.03,0.07)`、`r=0.30`、`Fwarn=0.80`、`Fsafe=0.90`。
- 上述参数是疲劳感知调度中的计算抽象，用于构造具有个体差异的标准化实验场景，不得表述为真实工人的精确生理参数。
- 必须计算并记录 `Fmax/Favg/FE`，并保留工人疲劳方差、高疲劳比例、最长连续工作时长和自然恢复总时长，供Qp风险奖励、FAT上下文、O13和实验诊断使用。
- 保留四向量和三个原始目标。
- 首版禁止加入多技能、主动休息、休息基因、第五染色体、第四疲劳目标、新的全局外部档案、LLM在线邻域设计或深度网络控制器。
- `AUTHOR_DIAGNOSTIC`与正式无疲劳模式必须是两个不可混用的入口；前者只复现P4.1作者行为，后者才是FM0/B0的正式比较基线。

### 7.2 全向量双 Q 引导

- 保留原 HMOPSO-QGS 的四子群、PDDR-FF 和 Q-gbest。
- 张博创新链路的四子群语义唯一允许为`G1_CMAX/G2_TEC/G3_TWC/G4_BALANCED`；对应目标为`objective[0]/objective[1]/objective[6]/三目标平衡-PDDR`。
- 作者物理槽位不重排：`groupU1→G1_CMAX`、`groupD3→G2_TEC`、`groupUNew→G3_TWC`、`groupC2→G4_BALANCED`；物理执行顺序仍可为`G1/G4/G2/G3`。
- 目标索引、方向标量、Need权重、Qg/Qp方向、档案锚点、邻域接受和序列化必须统一通过`ZhangBoSubSwarmSemantics`取得；禁止用enum ordinal、裸`group==`或变量名推断目标职责。
- 新Q表按当前语义零初始化；迁移前Q表、VNS统计和子群感知结果只能标记为`legacy_pre_subgroup_migration`，不得自动迁移或作为当前证据。
- 作者Q代码存在但未进入P4.1冻结主循环；必须先以独立开关恢复原Q-gbest，再实施CFVF，禁止把两者效果混在同一消融增量中。
- P6模式固定为：`AUTHOR_ACTIVE + AUTHOR_UPDATE`（默认兼容）、`ORIGINAL_QG + AUTHOR_UPDATE`（B1Q）、`ORIGINAL_QG + CFVF`（B2）、`B2P=B2+EVALUATED_PDDR`、`B3=B2P+谱系个人档案影子记忆`。Qg或CFVF开启时必须使用显式启用、非零`r`的P5疲劳参数清单。
- 新增P6随机事件全部使用可注入`PseudoRandomGenerator`；作者更新器内部已有的未受控随机性继续标记为`author_actual`，不得据此声称完整Runner固定seed重放。
- CFVF 必须作用于全部显式向量，不能只更新 `JS`；`JS`继续使用交换序列，资源层使用 `FMW/MW/M/W`、资源惯性、合法探索和无固定覆盖偏置的pbest/gbest冲突消解。
- CFVF只更新第一阶段显式`MA/WA`，作者扩展WA后续阶段块保持不变；资源动作必须在JS更新后按工件身份重新定位。
- CFVF必须先在“原pbest + 原Q-gbest”条件下单独验收，repair只作为异常安全网并持续记录repair rate。
- `EVALUATED_PDDR`只能读取已经完成完整评价的父代和全局后代；候选顺序固定为全局后代在前、父代在后，同分保持稳定顺序，PDDR比较不消耗FE，返回结果必须真正替换主种群及其作者历史。
- 每个粒子谱系维护容量 `L=6` 的个人非支配档案，不得把它变成新的种群级外部档案。
- 档案只接收本谱系已评价父代、全局后代和未来接入的局部后代；全局非支配集合只用于冻结归一化边界，不得向个人档案注入解。
- 档案更新固定执行三目标严格Pareto过滤、近重复连通分量去重，再按子群方向锚点、加性epsilon锚点和最远点填充截断；只有归一化主目标距离不超过`1e-4`时才用等权`Fmax/FE`风险破平。
- PDDR后必须按谱系执行：一个分支保留时沿用ID，多个分支保留时退休旧ID并按来源与四向量指纹稳定分裂新ID，谱系淘汰则删除，换子群只更新子群标签且不清空档案。
- P6.2必须保持影子模式：启用前后的种群、最终非支配集、Qg表、CFVF事件和FE逐项一致；只有谱系与档案诊断允许新增。
- Qp四动作固定为：保持当前个人领导、子群方向锚点、加性epsilon收敛锚点、满足 `φg(p)≤φbest+τq` 的认知—社会互补锚点。
- Qp状态固定为 `(Eg,Hi,Ri)`：4类子群进化需求、2类个人停滞、2类认知—社会冗余，共16状态；每个子群共享一张 `16×4` Q表并使用动作可行性掩码。
- Qp奖励由Pareto支配、子群方向改善、个人档案贡献和疲劳风险改善组成，必须在任何局部搜索之前计算；局部搜索后代可进入谱系档案，但不能回写为本轮Qp奖励。
- Qp使用10%评价预算预热和 `B=5` 的P-block/G-block分块冻结；P-block只更新Qp，G-block只更新原Qg，冻结表仍用于动作执行。

### 7.3 CA-TA-Lite-VNS

- O1–O9只作为继承基座的低层动作来源；当前正式高层控制不再新增O10–O13，而采用N1–N5五类宏邻域。
- N1复用Insert/Swap/Reverse等基础JS移动；N2复用基础机器/工人资源移动。
- N3根据SEQ/SET/BAL瓶颈在critical、setup-edge和family source中确定性路由；N4根据WOR/MAC/SET/FAT/BAL确定性选择资源重分配方式。
- N5只通过JS/WA/必要MA的结构变化产生自然恢复窗口，不得引入主动休息编码或直接移动start time。
- P7.1的O1–O13结果只作历史独立邻域证据；v3.5生产主循环必须使用CA-TA-Lite宏邻域，不得把旧O10–O13矩阵或作者遗留空`perturbation()`伪装成当前高层局部搜索。
- 上下文固定为 `(g,b)`：四子群方向和`SEQ/MAC/WOR/SET/FAT/BAL`六类瓶颈；phase不进入经验表，stagnation只作为Re-test触发器。
- 工厂选择采用 80% 需求加权、20% 随机探索。
- 无效动作必须被掩码且不能计为一次失败。
- Test阶段所有有效邻域获得相同完整评价次数；Apply阶段集中调用优胜算子并保留小概率探索，连续失败达到阈值后返回Test阶段。
- 每个上下文和算子必须记录调用数、成功数、方向质量、运行时间和完整评价次数。
- 信用按成功率更高、方向质量更好、计算成本更低、历史调用更少的字典序比较；首版保持李明哲原接受准则。
- CA-TA只能在全局后代唯一完整评价及本轮Qg/Qp奖励结算后评价局部候选；局部候选必须携带来源、父槽位、谱系和预评价标记，外层评价不得重复计FE。
- 局部候选可以进入评价后PDDR、谱系档案和个人领导继承，但不得回写本轮Qg/Qp奖励；剩余预算不足完整全局后代批次时必须安全停止，不得生成半代。

## 8. 随机性与可重复性

- P4.1冻结的作者直接派生代码允许原样保留既有未受控随机性，但只属于`A0_AUTHOR_DIAGNOSTIC`；不得为了制造数值一致而改写该诊断线。
- P5及之后新增的随机事件必须来自可注入、可记录的随机源；禁止在新增算法、解码器和算子代码中临时调用无种子的 `new Random()`。
- `deterministic_canonical` 是张博正式生产与消融母线，使用稳定排序、明确资源编号破平、实例绑定资源域和可注入随机源。
- `author_actual` 仅作诊断；对作者内部不可控事件必须单独登记，不得进入固定seed重放或正式参考前沿。
- 每个运行必须记录主 seed、派生 seed、解码模式、实例哈希、配置哈希和源码哈希。
- 同配置、同 seed 的重复运行应产生相同染色体、目标值和关键轨迹；无法保证时必须登记原因。

## 9. 代码与数据修改规则

- 修改前先确认来源、当前工作包和最小验收标准。
- 优先做小范围、可验证的改动，不进行无关重构。
- 原始快照保持只读；任何兼容或改进代码进入独立工作工程。
- 发现作者代码缺陷时：保存原样证据、建立最小复现、登记影响、区分 `author_actual` 与论文语义，再决定生产实现。
- 科研参数若没有论文或总体方案依据，必须进入显式配置并标记为待参数实验，不得伪装成论文默认值。
- 不得通过修改结果文件、选择性删除实例或调整指标口径使结果接近论文。

## 10. 测试、证据与状态

每个工作包至少需要：

- 论文—方案—Java—实现来源映射；
- 固定输入和预期输出；
- 单元测试和必要的集成测试；
- 固定 seed 与随机事件记录；
- 精确评价预算；
- 构建、测试和运行日志；
- 配置、源码、实例和结果哈希；
- 证据路径和已知差异说明。

状态必须分别报告：

- `engineering_validated`：工程构建成功且基础测试通过。
- `algorithm_aligned`：核心算法组件和固定轨迹已与选定语义对齐。
- `sampled_reproduction_accepted`：经批准的小规模多实例、多种子验收通过。
- `full_reproduction_accepted`：正式矩阵、统计和证据链全部通过。

初始状态固定为：

```text
engineering_validated=false
algorithm_aligned=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

不得用“基本完成”“差不多一致”或“应该成功”替代明确状态。

## 11. 单个工作包完成定义

工作包只有同时满足以下条件才能标记为 `completed`：

- 路线图前置依赖全部完成；
- 计划交付物实际存在；
- 来源和语义已登记；
- 验收测试通过；
- 评价预算和随机性可审计；
- 已知差异、限制和未证明内容写入报告；
- 证据路径有效；
- `docs/ROADMAP.md` 已更新状态、日期和证据。

仅完成代码但缺少测试、来源或报告时，状态仍为 `in_progress`。

## 12. 路线图管理

- `docs/ROADMAP.md` 是唯一主计划，不另建相互矛盾的临时总计划。
- 后续每个任务必须引用工作包 ID。
- 开始任务时将状态改为 `in_progress`；只有满足完成定义后才能改为 `completed`。
- 新发现的关键冲突、风险或语义决定必须进入路线图决策记录，不能只留在聊天中。
- 不删除旧决策；修正时保留日期、原因和新证据。
- P9 正式实验必须获得用户单独批准，不能因前置测试通过而自动启动。
- P8.2之后的性能优化必须同时保持`action_trace_hash`、`front_hash`、`evaluation_trace_hash`和FE不变；任一漂移即拒绝优化并保留原验证版本。

## 13. 必须停止并询问用户的情况

- 需要修改、删除或覆盖原论文、原 Java、原算例或作者结果；
- 论文、总体方案和 Java 出现会改变生产语义的关键冲突；
- 需要猜测疲劳参数、算法参数、数据字段或缺失公式；
- 需要把 `author_actual` 缺陷重新引入生产默认行为，或需要正式生产调用作者遗留解码/更新器；
- 需要越过当前路线图验收门；
- 需要启动参数搜索、扩大实例/种子/评价预算或正式统计矩阵；
- 需要安装系统级软件、改变系统默认 JDK、升级 jMetal 主版本或使用额外计算资源；
- 验收失败但有人要求标记为成功。

遇到上述情况时，保留现有证据，说明冲突、可选语义、影响和推荐方案，等待用户决定。

## 14. v3.5正式主线覆盖修订（2026-08-12，最高优先级）

本节覆盖本文件前文与v3.5冲突的旧规则；`docs/ROADMAP.md`中的V35工作包是唯一执行顺序。

### 14.1 当前正式模型只启用退化设置语义

当前正式比较固定为：

```text
familyMode = DEGENERATE_SINGLE_FAMILY
familyCount = 1
familyOfJob[j] = 0
familyTransition[k][0][0] = 0
setupMode = SEQUENCE_INDEPENDENT
SUT = SUT[job][stage]
machineChangeoverFactor = 1
```

产品族和序列相关设置时间必须保留接口、字段、配置和退化测试，但当前不得随机生成多产品族、非对称转移矩阵、前驱工件相关SUT或机器换型因子。当前第一创新只能表述为“序列无关设置时间下的动态疲劳、自然恢复与setup/processing两阶段一致解码”，不得声称已完成PF-SDST序列相关实验。

推荐占位接口包括：

```text
ProductFamilyData
ProductFamilyAssignment
ProductFamilyTransitionMatrix
ProductFamilySetupModel
```

后续只有用户明确批准，才能把`DEGENERATE_SINGLE_FAMILY`切换为真实产品族实验配置。

### 14.2 Shift正式路径永久冻结

正式Builder、Problem、Algorithm、消融注册表和实验Runner必须固定：

```text
ShiftMode = NONE
```

以下内容不得进入当前正式执行路径：

```text
FCLS
FCRS
LEFT_ONLY
RIGHT_ONLY
LEFT_RIGHT
IncrementalReplay
ReleaseOverride
ReverseSlack
RecoveryRescue
TemporalShift
```

P8.6的`fatigue-shift-v2-common-gap`代码、I0/I1移位轨迹和相关P9计时只能标记为`legacy_shift_on`与`historical_diagnostic_only`。不得继续优化、重启或把移位结果纳入当前reference；任何重新启用都需要新的用户批准和新的工作包。

### 14.3 当前正式算法比较公平边界

张博FULL与李明哲公平基线必须共享：

```text
instance
SUT
familyMode
setupMode
FM3 fatigue decoder
ShiftMode=NONE
initial population
seed
FE budget
objectives=[0,1,6]
passive evaluation archive
reference construction
```

不得把产品族占位、SUT变化或Shift差异归因于搜索算法。当前不启动完整消融、PF-SDST真实启用或正式统计矩阵。

### 14.4 v3.5三项正式创新边界

1. 动态疲劳解码：累积、自然恢复、setup/processing两阶段工时反馈及后续联合机器—工人ECT。
2. 双Q全向量搜索：个人档案Qp、原Qg、DSCR和CFVF；DSCR只能在Qg动作前清理当前社会快照中已有严格支配的缓存老师，不增加动作、奖励或FE。
3. CA-TA-Lite：24类`(subSwarm,bottleneck)`上下文、N1–N5五类宏邻域、确定性内部路由、Test/Apply/Re-test；N5只能通过基因型变化形成自然恢复，不得直接改start time。

### 14.5 新增停止条件

- 未完成`V35-P0`源码/配置/证据冻结，不得修改v3.5生产代码。
- 未完成当前退化设置语义下的FULL与公平基线比较，不得启用真实PF-SDST。
- 未完成DSCR的DTUR、snapshot、strict-dominator和audit non-interference门，不得宣称第二创新完成。
- Cmax生命周期审计未定位Generation、Admission、Survival或Exploitation断点前，禁止新增Cmax特权、Elite保护、PDDR改权或新邻域。
- 不得把历史Shift-on结果、单实例诊断或旧P9报告称为v3.5正式复现。

### 14.6 v3.5正式公平运行硬门（2026-08-13）

- FULL与公平基线必须共享`formal-hmopso-qgs-v1`外循环、`Q_Times`、`LS_Times`、PDDR时序和继承局部搜索；不得再用普通PSO循环与正式嵌套循环直接比较。
- 每个新Q轮全局后代必须清除历史预评价标记并完整评价一次；必须满足`initial FE + global FE + CA-TA FE + inherited local FE = total FE`。
- 事件证据必须同时写入`totalCount`和`retainedCount`；禁止把容量4096的滚动窗口大小称为真实事件总数。
- DSCR替代老师必须按`G1:Cmax/G2:TEC/G3:TWC/G4:normalized max-deviation`方向选择，再以稳定指纹破平。
- CA-TA-Lite只能由连续Apply失败触发失败重测；历史累计失败不得造成永久Test。Test mask必须来自无FE preview后的真实可构造动作。
- N4候选必须严格增加任务间自然恢复；N5必须同时形成一个JS动作和一个资源动作，且不得调用任何Shift/ReleaseOverride逻辑。
- 当前100k单实例单seed只证明工程运行和诊断信号；Cmax仍略差时不得宣称FULL全面优于基线，也不得据此调参。

### 14.7 DSCR、宏邻域与Cmax审计硬门（2026-08-13）

- DSCR的唯一教师事实源是`ZhangBoQgController`的`previous`和`historical`缓存；旁路账本只做观察，不得驱动动作。
- DSCR指标固定为`DTUR=dominatedTeacherUses/teacherUses`、`SCRR=replacements/validityChecks`；事件必须同时保存decisionCycle、generation、FE、cacheType、strict-dominator计数、首次发现FE、刷新FE和dominanceAge。
- 只有严格支配者才能替换缓存教师；完全重复目标和互不支配候选不得替换。Qg进入CFVF前的真实teacher-use必须满足`dominatedTeacherUses=0`。
- v3.5的N3/N4/N5是独立宏邻域，不得把旧O10–O13或Shift代码作为实现。N3只移动按工件身份绑定的JS资源包；N4只改变第一阶段合法资源；N5必须同时产生JS和资源动作，并由完整解码证明自然恢复增加。
- Cmax审计是只观察旁路，默认每1000 FE输出Global/G1/Generated/Survived曲线以及候选、PDDR、档案、下一代存活和G1教师使用字段；审计开关不得改变行为哈希、FE或随机流。
- CA-TA-Lite动作选择使用确定性`workUnits`和完整评价次数的代价代理，真实`System.nanoTime()`只能作为诊断；不得再用墙钟参与正式动作选择。
- `V35FairRunner`提供`V35_QG0`/`V35_QG1`单变量配对：两者均启用原Qg，只有DSCR清洗开关不同；两条路径必须共享FM3、单族、序列无关SUT、ShiftMode=NONE和初始种群哈希。
- DSCR摘要必须以独立properties字段输出`teacherUses/dominatedTeacherUses/dtur/validityChecks/replacements/scrr`；失败运行必须保存问题评价器已消耗的真实FE，不得回写为0。
- 旧100k证据统一标记为`legacy_pre_dscr_macro_cmax_audit_fix`，本轮仅接受I1/20k机制重验；不得自动启动100k、500k、正式矩阵或多产品族实验。

### 14.8 CA-TA-Lite压力诊断与置信回退硬门（2026-08-14）

- 瓶颈诊断必须在Need模块选定目标工厂之后进行，只能读取该工厂已评价轨迹；禁止用整张调度的压力指导另一个工厂。
- 正式压力仅包含`SEQ/MAC/WOR/SET/FAT`五类，全部归一化到`[0,1]`；`BAL`不是竞争压力，只表示证据不足、数据非法或DAG不可计算时的fail-closed回退。
- 单瓶颈只有在`P1>=tauAbs`且`P1-P2>=tauGap`时成立；否则必须回退`BAL`并开放N1--N5。当前`SET`仅表示序列无关`SUT[job][stage]`压力，不得表述为PF-SDST证据。
- 阈值只能由I1和`20_2_3_1`的shadow反事实证据校准；shadow必须使用独立Problem、独立计数器和独立随机域，不进入主搜索FE、PDDR、档案、Q表、Cmax审计、正式前沿或Decoder计时。
- 阈值冻结门固定为`strictClassificationCoverage>=10%`且`missedPositiveBestRate<=5%`；校准集从Q50/Q60/Q70/Q80/Q90形成25组候选，held-out任一运行失败则保持BAL全开放。
- `System.nanoTime()`只允许记录诊断性能，不得参与压力分类、阈值选择或动作决策。Test/Apply/Re-test既有预算、连续失败和掩码变化语义不得因诊断升级而改变。
- 正在训练机执行的P25A使用冻结旧jar，禁止中途覆盖或修改；完成后统一标记`legacy_pre_pressure_diagnosis`，不得与新语义A4/A5前沿混合。
- 新的500000 FE A4/A5门、P24.2冻结或正式矩阵必须在held-out门通过后由用户另行批准。

### 14.9 P25C安全回退主版本边界（2026-08-15）

- P25B held-out漏失率未通过5%门，压力分类器只能作为诊断旁路；当前正式安全语义固定为`BAL`、N1--N5全开放、shadow关闭，禁止继续用同一校准/held-out数据调阈值。
- P25C三seed/100k只给出`A4_PREFERRED_SIGNAL`工程信号：A4的统一reference HV/IGD优于A0中位水平，但Cmax等单目标极值并未全面领先，不得称为算法统计优越。
- 方向top-k教师池在A5三seed均真实触发，但A5对A4仅1/3 HV胜或平，当前主版本候选为A4；教师池默认关闭，只保留为可选模块。
- P25C不构成正式矩阵、论文最终数字或抽样复现验收；任何500k、多实例、20次运行或显著性检验仍需用户另行批准。

## 15. 论文正式实验与制图规则（2026-08-15）

### 15.1 唯一实验子路线图

- `docs/V35_FORMAL_EXPERIMENT_ROADMAP.md`是V35-P25–P28唯一实验子路线图；旧`docs/P9_FORMAL_EXPERIMENT_PLAN.md`及P8/P9实验计划只作历史记录。
- 子路线图只定义执行协议，不自动授权运行。每个500000 FE、多实例、多seed、参数实验、消融或正式统计工作包仍须用户单独批准。
- 当前只完成`experiment_protocol_documented=true`；必须保持`formal_matrix_started=false`，直到用户明确批准EXP-1或后续工作包。

### 15.2 数据集和科学表述

- 正式矩阵沿用论文的`jobs={20,50,100,150,200}`、`stages={2,5,8}`、`factories={3,4,5}`共45实例结构。
- 数据必须表述为“基于李明哲EADHFSP基础实例构造的确定性标准化疲劳扩展测试集”，不得称为通用公开疲劳基准。
- 疲劳参数是制造调度中的计算抽象，不得表述为真实工人的精确生理参数。
- 当前产品族仍是单族零转移占位，设置时间仍是`SUT[job][stage]`；不得把当前实验描述为PF-SDST实验。

### 15.3 正式比较硬门

所有正式算法必须共享：

```text
instance and instance hashes
SUT and fatigue parameter manifest
FM3
DEGENERATE_SINGLE_FAMILY
SEQUENCE_INDEPENDENT
ShiftMode=NONE
initial population per seed
population=100
MaxFEs=500000
objectives=[0,1,6]
FE accounting
reference construction
```

- 正式重复次数固定为20，不照搬论文的30次。
- 20个seed必须在首个正式运行前冻结；同seed所有算法的初始四向量种群哈希必须相同。
- 当前安全语义为压力分类仅诊断、实际`BAL`全开放N1–N5、shadow关闭；P25B未冻结的阈值不得进入正式动作选择。
- 当前主版本仅是A4候选；A5方向教师池默认关闭并保留为可选模块。不得因名称`FULL`暗示A5必然最优。
- `A0_AUTHOR_DIAGNOSTIC`、历史Shift-on结果和旧压力语义结果不得进入正式reference。

### 15.4 Reference、指标和统计

- 每个实例的正式reference只能在参与算法和全部20次运行结束后一次构造：`PFref=ND(union of all participating algorithms and runs)`。
- 同一实例的全部算法共用同一归一化边界；HV参考点固定为归一化`(1.1,1.1,1.1)`。
- 禁止使用单次FULL+BASE union、每算法独立reference或运行中动态reference作为正式IGD/HV依据。
- 主指标固定为HV、IGD、SP、双向C-metric、非支配解数及Cmax/TEC/TWC极值；疲劳指标只作风险诊断，不增加第四目标。
- 同seed配对比较使用Wilcoxon signed-rank；多算法使用Friedman并进行Holm校正；`alpha=0.05`，同时报告效应量和中位数/IQR。
- 新增算法后，若正式参与算法集合发生变化，必须重新构造reference并重算全部指标。

### 15.5 论文图表来源纪律

- 学习ESWA论文Fig.1–14承担的论证功能，但不得复制其数据、点位、结论或造成混淆的视觉样式。
- I1统一用于四向量、甘特图、疲劳曲线、CFVF和CA-TA-Lite讲解；正式效果图使用冻结实验母表。
- 五规模50%经验达到面固定使用`20_2_5/50_2_5/100_2_5/150_2_5/200_2_5`，每个实例由20次最终前沿计算，输出三维面和三个二维投影。
- 所有图只读取冻结CSV/JSON，统一生成SVG、PDF和PNG；禁止在绘图软件中手工修改数值。
- 主算法、A0和外部算法在全部图中必须使用一致且色盲友好的颜色、标记和顺序。
- 参数主效应、收敛曲线、箱线图、达到面、统计排名、疲劳风险和运行时间分解必须分别标明数据工作包和配置哈希。

### 15.6 实验停止条件

出现下列任一情况必须停止并保留证据：

- 初始种群、实例、SUT、疲劳参数或配置哈希不一致；
- FE超预算、候选重复评价、非法解、异常repair或来源丢失非零；
- 正式路径出现Shift、多产品族或序列相关设置时间；
- DTUR非零，或声明开启的机制没有真实事件；
- 参与算法未冻结就生成正式reference；
- 根据正式测试结果临时改算法、参数、seed或删实例；
- 用户尚未批准相应实验工作包。

## 16. v3.5-Final Candidate（V35-FC）纪律（2026-08-17）

本节为当前最高优先级执行纪律；完整方案见`docs/V35_P26_FINAL_CANDIDATE_PLAN.md`，流水线登记与决策记录见`docs/ROADMAP.md` D-082。

### 16.1 总原则

- 三项创新冻结（FM3动态疲劳解码、认知—社会全向量双Q搜索、Budget-Aware CA-TA-Lite），**不增加第四项创新**。
- 核心原则从"增加更多聪明机制"改为"让已有三创新互相配合，且每一个FE尽可能产生价值"。
- 职责分工固定：FM3告诉算法"真实调度是什么"；Qp/Qg+CFVF负责"全局往哪里学"；CA-TA负责"什么时候值得深挖、用什么邻域深挖"；Cmax审计阻止靠猜测修改算法。

### 16.2 流水线与单变量纪律

- FC-0→FC-9严格顺序：A4-PREFINAL存档→FM3一致关键结构→Local-FE Pacing→Cheap-Test→软冻结ρ→Cmax/GIR审计→Cmax修复单支→最终消融→四规模Champion Gate→45×20启动门。
- 禁止跳包、禁止并行实施多个机制包；pacing（FC-2）与soft-freeze（FC-4）**不得同时首测**，必须先得到A4+Pacing稳定版本，再单变量对照A4+Pacing vs A4+Pacing+SoftFreeze。
- FC-6每次只允许实施一支Cmax修复分支（Generation/Admission/Survival/Exploitation四选一），依据FC-5审计证据选择；禁止同时加多种修复。
- 当前用户批准的 FC-6 仅为 **Survival** 支：先使用 `PddrSelectionMode.GLOBAL_ORIGINAL` 做 `CA-TA-Lite → inherited LS` 与反序的单变量配对；顺序裁决完成后，才可在同一顺序下比较 `GLOBAL_ORIGINAL` 与四方向对称 `REGION_AWARE`。`BP_RESERVED_LEGACY` 只读、不得进入 FC-6；区域容量固定 `G1=15/G4=55/G2=15/G3=15`，禁止叠加教师门控或新增搜索动作。
- FC-6 收口后 DOE-1 只允许改变搜索期物理子群容量，正式 PDDR 必须为 `GLOBAL_ORIGINAL`，正式顺序必须为 `CA-TA-Lite → inherited LS`。容量基线为 `[G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC]=[20,40,20,20]`；`15/55/15/15` 是历史容量控制点，不能被解释为 Region-aware 生存配额。DOE-1 的 `V35SubSwarmMixture`、111 点格点、15 点确定性设计和 135 条开发矩阵必须先通过预检；不得在预检前启动 500000 FE。
- DOE-1 的 held-out confirmation 已完成并冻结：T1=`30/50/10/10`、T2=`25/25/25/25`、T3=`20/40/30/10` 均未达到预注册的跨实例 `median ΔCmax >= +2%` 门；正式 `FINAL_SEARCH_MIXTURE` 保持 `[20,40,20,20]`。不得因 T1 接近门槛重新调容量或自动启动 DOE-2；后续仅能在此冻结值上进行经批准的消融或正式对照。
- FC-6 结果（2026-08-20）已收口：`ORDER_SWAP` 的中位最小Cmax虽改善6.87%，但IGD中位退化11.68%，故维持当前顺序；`REGION_AWARE`在20-job和100-job的HV/IGD门均失败，100-job同时触发一票否决。区域选择不得进入后续主版本或正式矩阵；保留`GLOBAL_ORIGINAL`，后续仅可在用户批准下进入人工复核或既定FC-7流程。唯一汇总入口为`docs/evidence/V35-P26/FC6_FINAL_CLOSURE_REPORT.md`；引用FC-6结果时必须同时保留其非统计性、失败分支隔离和禁止自动扩展的边界。
- 每个FC包的实验对照必须paired seed、共同初始种群、指标口径与P8MetricCalculator一致；诊断运行不得进入正式reference。

### 16.3 100-job一票否决

任何新参数或新机制转正的统一判据：四个代表规模（10/20/50/100）方向一致且**没有任何实例明显退化**。量化否决线：100-job中位HV降幅≥5%或中位IGD升幅≥10%。不得因20-job结果好看而转正（gb15教训，证据`docs/evidence/V35-P25E-corrected-comparison/dualq-multi-instance/MULTI_INSTANCE_REPORT.md`）。

### 16.4 参数地位变更

- `LS_Times=30`（Table 9）降级为**仅作者公平基线参数**（A0/HMOPSO-QGS-F等基线臂）；A4正式配置的局部搜索强度由β(u)=βmin+(βmax−βmin)·u²的local FE配额控制（B_L=⌊β/(1−β)·B_G⌋，inter-factory LS与CA-TA共享硬预算）。βmin=0.25/βmax=0.65为第一版候选，冻结前不得写入正式默认。
- 双Q块长固定P=5/G=5；`gBlockLength`保持实验参数地位，gb10/15/20路径永久关闭。
- ρ（软冻结系数）∈{0,0.1,0.2,0.3}为FC-4唯一可调新参数；ρ=0即当前硬冻结；全部候选失败则删除软冻结、维持硬冻结，不得引入其他冻结变体。
- CFVF四向量强度（JS/FA/MA/WA调整）与Sparse CFVF均为**条件分支**：只有FC-5的GIR/RecordContribution审计给出证据后才允许实施。

### 16.5 禁区清单（永久）

1. 不再试gb10/gb15/gb20块长；
2. 不重新上严格SEQ/MAC/WOR/FAT瓶颈掩码（P25B held-out已失败，BAL全开放为正式语义，压力分类仅诊断）；
3. 不重新启用FCLS/FCRS及任何Shift；
4. 不给DSCR强塞当前minCmax教师（DSCR唯一职责是Qg动作前的stale social cache有效性维护；Cmax教师覆盖问题归Qg修，不污染DSCR职责）；
5. 不人为修改CA-TA cost credit来救Cmax；
6. 不拍脑袋规定CFVF向量强度（先GIR审计后动）；
7. baseline永不增强：对比算法只允许问题接口适配，不得增强搜索机制（P25E原则，P25D路永久封死）；
8. 算法未通过FC-8 Champion Gate前不启动45×20正式矩阵（不烧算力）。

## 17. V3.5-FC-TIME 运行时间收口纪律（2026-08-18）

本节在 16 节基础上新增，完整方案见`docs/V35_FC_TIME_PLAN.md`，决策记录见`docs/ROADMAP.md` D-083。阶段插入位置：FC-4 与 FC-5 之间；下一可申请工作包 `V35-FC-T-0`。

### 17.1 核心原则

- 任务从"再提高 HV"转为**保住 FC-2 质量收益、把数十倍 CPU 时间砍到时间门内**。
- **只灭重复计算，不改任何算法决策**：Q/pacing/CA-TA/PDDR 选择结果/随机数调用顺序/FE 数一律不动。凡可能改变决策的改动必须走 FC 流水线，禁止借"优化"之名改行为。
- 不退回 PT0 proxy、不关 FM3 关键结构（FC-1 验收门不变）。

### 17.2 阶段顺序（D-083）

FC-TIME-0 正式计时（同机串行三臂 R1/R2/R）→ FC-TIME-1 模块耗时账（15 模块 + per-cycle，∑≥95% 门）→ FC-TIME-2 语义等价优化（A 四热点支配去重 → B FM3 critical DAG memo → C copy 精简，按 profiling 决定）→ 等价性验收 → Runtime Gate。禁止跳过探查直接改代码。

### 17.3 等价性验收（每项优化后必过）

同 seed（20260822）同 FE 重放，断言：front.csv sha256 逐位一致 + 外层/Qg/Qp 事件流哈希一致 + FE 一致；FC-TIME-2 落点加 500k 级逐位 front 比对。"结果一致但没测"不算数。

### 17.4 时间门（研发冻结标准）

| 档位 | 同机 Final/QGS | 动作 |
|---|---|---|
| 红线 | >10× | 不启动 45×20；继续瘦身或 FC-TIME-3（βmin 0.25/0.30/0.35 找拐点） |
| 可接受 | 5–8× | 保持 HV≈+10%/IGD 大幅下降/TEC/TWC 优势，可进 FC-5 |
| 理想 | 3–5× | 论文最舒服区间 |

时间门未过（>10×）不得宣称时间已收口，不得占位论文运行时间图。

### 17.5 四热点清单（FC-TIME-2-A 目标，勿扩散到无关代码）

`ZhangBoEvaluatedPddrSelector.authorScores`（:160-175）、`PDDRFFselect`（ZhangBoMOHPSOQ:7722-7731）、`select()` 全局锦标赛（:1260-1273）、`appendAndPrunePersonalHistories`（:8088-8097）——统一改"单遍双向 + j 从 i+1 起扫"，每次三目标比较同时结算两个方向；打分公式 `count2+1/(count1+1)` 提取共享实现保证两边一致。archive 已是增量式，不在此列。

### 17.6 悬置事项处置

- **Soft-Freeze（FC-4）**：**已裁决删除（2026-08-18）**。18/18 已测：20-job HV/Cmax/QgTD 改善但 IGD 9/9 退化；100-job HV 9/9 全输（−6.1~−44.1%，超否决线）、Cmax/TEC/TWC 无改善。判据全败 → 维持 ρ=0 硬冻结，ρ 参数封闭，不再开启。
- **Cheap-Test（FC-3）**：永久封禁。教训：CA-TA Test 不是纯开销，它在贡献搜索——**不要少 Test，要让 Test 算得更便宜**。
