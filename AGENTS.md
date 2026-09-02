# 张博改进项目协作规则

本文件是 `E:\学习\李明哲-毕业材料\张博改进` 项目的最高优先级协作规则，适用于后续所有开发者、Codex 和其他 AI Agent，并对本目录及全部子目录生效。

项目最终目标是：

> 以李明哲第四章 HMOPSO-QGS 的论文语义和完整 Java 实验代码为来源，在隔离副本中建立不继承作者已确认缺陷的规范生产基线，并完成“序列无关设置时间下的动态疲劳解码”“Qp/Qg+DSCR+CFVF认知—社会全向量搜索”和“CA-TA-Lite五宏邻域局部搜索”三项正式研究机制，形成可追溯、可测试、可重复的 Java/jMetal 5.8 研究实现。产品族序列相关设置时间只保留占位，不在当前正式比较中启用。

“项目能够编译”“小算例能够运行”“单元测试通过”都不等于算法已经与论文对齐，更不等于论文级实验已经完成。

## 1. 工作范围与写入边界

- 项目根目录：`E:\学习\李明哲-毕业材料\张博改进`
- 唯一主路线图：`docs/ROADMAP.md`
- 后续所有代码、配置、测试、日志、报告和生成结果只能写入本项目目录。
- 本项目以 GitHub 仓库 `https://github.com/zb581899564-arch/zhangbogaijin.git`（分支 `main`）作为持久镜像：每次更改完成后必须在同一工作会话内 commit 并 push 到 GitHub（详见 §35 GitHub 镜像同步纪律）。
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

### 10.1 全流程强制留证与防重复实验（2026-08-24）

- **任何工作都必须留证**：包括但不限于公式核验、代码审计、缺陷复现与修复、单元/回归测试、
  编解码示例、单一变量实验、消融、参数筛选、烟测、性能测量、远程训练和正式统计。不得以
  “只是临时测试”“结果不好”或“运行失败”为由不登记。
- 启动任何运行前，必须先登记工作包ID、科学问题、唯一变量、对照臂、固定条件、实例、seed、
  population、MaxFEs、算法语义版本、配置哈希、源码或Jar哈希、初始种群哈希、预期输出目录和
  验收/停止条件；未完成预登记不得启动实验。
- 启动前必须查询`docs/PAPER_EVIDENCE_MASTER`、对应`docs/evidence`目录、训练机实验地图及归档
  总账。若已有实验的实例、seed、预算、算法语义、配置、源码/Jar和初始种群哈希全部一致，
  原则上必须复用并反向验收已有证据；确需重跑时必须登记原因和`sourceRunId`，禁止无说明重复实验。
- 运行全过程必须保留原始配置、控制台日志、状态、真实FE/Decoder调用、停止原因、前沿或输出、
  机制计数、异常与失败信息。失败、超时、部分完成和被否决分支同样必须保存真实已消耗预算，
  标记为`FAILED/PARTIAL/LEGACY_EXCLUDED`，不得覆盖、伪装成0 FE或静默删除。
- 每次运行结束后必须生成可审计清单，至少绑定：`runId/sourceRunId`、时间、主机、本地/远程/归档
  路径、实例与扩展数据哈希、配置与机制向量哈希、源码/Jar哈希、初始种群哈希、结果/前沿哈希、
  文件级SHA-256、论文用途等级、reference资格、已知限制和替代关系。
- 证据必须进入对应工作包目录，并同步登记到论文证据总账；聊天记录、截图、口头结论或仅存在于
  训练机的未登记目录均不构成验收证据。没有“输入→配置→执行→结果→哈希→结论”闭环的工作
  只能保持`in_progress/unverified`，不得标记完成或写入论文结论。
- 清理或替换实验数据前必须先完成归档、逐文件哈希复核和恢复路径登记；负结果和失败分支至少保留
  摘要、配置、日志、状态、关键原始输出及哈希，以便解释为何淘汰并防止未来重复走同一路线。
- 纯重复性复跑、跨JVM重放和修复后回归属于允许的重复实验，但必须明确标记目的，并与首次运行
  通过`sourceRunId`关联；不得把重复运行当成新的独立seed或增加统计样本量。

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

## 18. V35 FINAL Stage2 人工批准覆盖与生产门（2026-08-23）

用户已人工确认 `FC-6 CLOSED`、`DOE-1 development/held-out/parameter freeze ACCEPTED`，并固定
`FINAL_SEARCH_MIXTURE=[20,40,20,20]`。因此本文件中任何要求“FC-8/FC-9 通过后才可进入
V35 FINAL 正式路线”的历史规则，均保留为历史可追溯记录，但对 Stage2 标记为：

```text
FC-8 Champion Gate = SUPERSEDED_BY_FC6_AND_DOE1_EVIDENCE
FC-9 before formal experiment = SUPERSEDED_BY_FC6_AND_DOE1_EVIDENCE
```

Stage2 唯一的自动启动门是干净 Final source/Jar freeze、45实例×20seed×900共享初群 manifest、
A0--A4生产预检、冻结并发吞吐上限和语义身份复核。正式 roster 仅为 A0--A4，A0/A4 raw runs
同时复用作主两算法比较；禁止另建重复的 A0/A4 矩阵。

### 18.1 Phase-Consistent Budget Termination（2026-08-23）

用户已批准方案 C。正式 `MaxFEs=500000` 定义为最大允许的完整评价次数，而不是要求
`requestedFE=actualFE`。冻结 jar、Q/LS 时序、局部预算和算法参数一律不改；禁止 terminal
partial Q phase、补评价或任何“填满预算”的改动。每条正式运行必须满足：

```text
0 < actualFE = decoderCalls <= MaxFEs
remainingFE = MaxFEs - actualFE
qPhaseFE = population × Q_Times = 100 × 50 = 5000
0 <= remainingFE < qPhaseFE
```

运行后外部审计必须写入 `budget-termination.properties`，报告实际 FE、利用率、终止类型、
formal outer cycles/Q rounds 与 jar/config/snapshot hash；同一 `(instance,seed)` 的 A0--A4
五臂须共同初群且 `max(actualFE)-min(actualFE)<5000`。任一违反将使整组 `INVALID`，不得进入
PFref、指标或统计。完整协议见
`docs/evidence/V35-PHASE-BUDGET-PROTOCOL/PHASE_CONSISTENT_BUDGET_TERMINATION_PROTOCOL.md`；
旧 strict-exact 20k/50k/100k 证据只读保留为 `legacy_pre_phase_budget_protocol`。

2026-08-23 的 Gate3 与 4/8/12/16 JVM 吞吐已接受，容量上限固定为
`FORMAL_MAX_PARALLEL=16`。但这**不授权**启动 4500 条矩阵：冻结 jar 的
`ZhangBoV35FormalComparisonRunner`只支持两臂正式入口，当前没有同时满足 A0--A4、
900 snapshot、Master RunKey 与五臂组审计的 launcher/renderer。该缺口未以新的、版本化的
外部执行边界闭合前，必须保持 `formal_matrix_started=false`，不得以诊断 preflight 起点或
两臂 runner 替代五臂正式矩阵。详见
`docs/evidence/V35-PHASE-BUDGET-PROTOCOL/06-formal-launch-readiness/FORMAL_MATRIX_BLOCKER.md`。

### 18.2 Stage2 A0--A4 Master v2（2026-08-23）

D-089 的阻断项已由版本化外部执行链关闭。冻结算法 jar 仍为
`8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`，禁止重建或替换。
正式运行只能使用 `v35-final-a0-a4-master-v2`：每个公平组必须且只能包含 A0--A4 各一次，
绑定同一 instance、seed、snapshot、V35/P8 初群哈希与 Problem provenance；每个 RunKey 还必须
绑定 arm profile hash。调度上限固定为 15 JVM（3 个完整公平组），每个 wave 必须先通过五臂
预算及证据反向审计，方可启动下一 wave。完成结果恢复前也必须重验，不得仅凭目录存在跳过。

正式500k运行继续执行 `PHASE_CONSISTENT_BUDGET_TERMINATION`，每臂利用率须大于99%，组内
实际FE范围须小于5000。每组验收后允许把大型事件日志压缩为不可变归档，但只有归档和清单
SHA-256复核通过后才可移除未压缩日志。启动前服务器可用空间不得低于120GB。详细验收见
`docs/evidence/V35-STAGE2-MASTER-V2/STAGE2_P1_ACCEPTANCE_REPORT.md`。

首个正式 wave 已在2026-08-23通过三个完整五臂组审计，故当前
`formal_matrix_started=true`；这仅表示4500条运行在训练机后台执行。矩阵完成前继续禁止构造
最终PFref、计算论文统计、修改冻结算法或把`sampled/full reproduction`升级为true。

### 18.3 Stage2正式矩阵暂停与先导优先（2026-08-23）

用户已要求先用小范围证据判断机制和PDDR，再决定是否扩大。4500条Master已安全停止，当前
`formal_matrix_running=false, formal_matrix_paused=true`。只允许使用12个已验收五臂组的60条
配对运行进行先导分析；停止wave的孤立完成和partial结果不得混入。未经用户根据先导/PDDR
审计作出新决定，不得恢复Master、补跑剩余矩阵或修改冻结jar。证据目录为
`docs/evidence/V35-STAGE2-PILOT-A0-A4-20260823/`。

## 19. 论文证据总账与清理治理（2026-08-23）

- `docs/PAPER_EVIDENCE_MASTER/CURRENT_SCIENTIFIC_STATE.md`是当前运行状态、论文引用资格和清理
  判断的唯一事实入口；旧文档中的`RUNNING/BLOCKED`只能作为历史记录。
- 每个campaign、run和artifact必须进入总账，记录本地/训练机/归档路径、SHA-256、论文用途、
  reference资格、替代关系和恢复位置。
- 清理必须执行“删除前清单→归档→逐文件复算→确认无活动进程→绝对路径删除→恢复复验”。
- 当前或潜在论文原始数据至少保留两份；只有唯一副本、哈希不一致或归档不完整时必须跳过删除。
- 永不删除作者论文/Java/算例/结果、v2/v3.5来源、Final freeze、正式snapshot、I0/I1母表、
  ROADMAP历史决策或失败分支结论说明。
- Stage2的12个完整五臂组在A3/PDDR审计和新裁决前保持原始可读；8条孤立完成和7个partial
  只能作失败证据，禁止进入reference。
- 本证据整理工作包不授权恢复4500矩阵、修改冻结Jar、PDDR或正式参数。

### 19.1 总账归档与清理验收（2026-08-24）

- 证据总账已完成，入口固定为`docs/PAPER_EVIDENCE_MASTER/README_FIRST.md`；恢复和清理事实以
  `cleanup-execution.csv`、`artifact-ledger.tsv`和`remote-location-map.csv`为准。
- 训练机25个campaign均有G盘哈希匹配归档；19个历史/重试展开目录已安全删除，6个主线目录
  保持展开。任何后续清理必须继续执行相同的归档与绝对路径门。
- 冻结Jar SHA-256仍为
  `8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`；900份snapshot及4份
  manifest文件共904项复算失败为0。
- 4500矩阵仍为`formal_matrix_running=false, formal_matrix_paused=true`。不得因归档完成而自动
  恢复实验；先完成A3/PDDR纯观察审计并等待用户新裁决。

## 20. 非支配档案与前沿基数纪律（2026-08-24）

- 当前正式活动档案只能是`UNBOUNDED_FULL`；正式Runner不得暴露档案实验模式。
- `representative-front-k30`只用于论文绘图与甘特图选例，禁止进入搜索、PFref、HV、IGD、
  C-metric或任何主性能表。
- K25/K50只允许作等基数敏感性，必须与完整前沿主结果分表报告，不能替换原始front。
- ND1/ND2教师视图及ND3/ND4有界活动档案均为休眠候选；未经Gate A证据和用户独立批准，不得
  上传训练机、启动运行或写成已验证机制。
- 档案问题与PDDR必须分开研究；禁止借档案基数问题修改`GLOBAL_ORIGINAL`，也禁止借PDDR
  归因顺带裁剪档案。
- 任一活动档案语义变化都必须通过四配比DOE迁移门；触发交互/排序反转时须重做完整DOE，不能
  沿用`20/40/20/20`的旧结论直接升级。
- 观察钩子必须保持初群、动作/评价事件、Q表、FE与最终前沿不变；ND0中精确去重后的
  `decision-front`与`observed-full-front`不相等时立即停止。
- 4500正式矩阵继续暂停；本档案工作包不得恢复Master、构造正式PFref或计算论文统计。
- 禁止覆盖冻结Jar
  `8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`
  以及DOE1原始证据。新代码只可生成独立、可删除的实验构建产物。
- 完整协议、候选臂和升级门分别以`docs/V35_ND_ARCHIVE_PROTOCOL.md`、
  `docs/V35_ARCHIVE_EXPERIMENT_GUIDE.md`和ROADMAP D-094至D-096为准。

### 20.1 2026-08-24 Gate A与A2/A3收口

- 60条Stage2配对运行的等基数和leave-out审计未发现A4/A0排序反转；不得再把A4优势描述为
  “仅因输出点多”。完整decision-front继续用于主指标。
- ND0中`decision-front != observed-full-front`的候选生命周期差异已实证，Gate A为`BLOCKED`。
  禁止静默改用observed前沿，禁止启动ND1--ND4；教师方向遗憾只保留为待批准候选信号。
- PDDR当前裁决为`KEEP_GLOBAL_ORIGINAL`，专项证据已关闭。Stage2字段限制不是重新运行PDDR实验
  的授权；BP和Region-aware不得回主线。
- A2→A3曾暂定为`COMPOSITE_BLOCK_UNRESOLVED`；该历史判断已由20.4的D1→Q0→D2最小拆分
  收口，禁止再把“未裁剪奖励数值极值”直接写成唯一性能根因。
- 4500正式矩阵继续`formal_matrix_running=false`；冻结Jar、DOE配比、档案和PDDR均不得变化。

### 20.2 Qp方向奖励纠错边界（2026-08-24）

- `LEGACY_UNCLIPPED`继续是冻结Jar的历史行为；新增`V35_CLIPPED`只能由专用诊断配置显式创建，
  默认构造器、正式A3/A4和Master不得静默切换。
- v3.5公式一致性要求方向奖励裁剪到`[-1,1]`已由单元测试和三seed 50k诊断验证，但当前先导性能
  门拒绝晋升；不得因为“公式正确”就覆盖正式Jar，也不得因为旧行为短程更好而删除纠错证据。
- 本实验不是DOE；禁止在这三seed上继续搜索裁剪阈值、epsilon或奖励权重。
- 若要继续定位A2→A3，只允许另行批准的Qp/个人档案/双Q最小单变量设计；PDDR、档案基数、
  DOE1配比和局部搜索顺序继续冻结。

### 20.3 A2→A3最小因果拆分证据纪律（2026-08-24）

- `docs/evidence/V35-A2-A3-DECOMPOSITION/`是D0--D3因果拆分的唯一证据入口；它是诊断运行，
  不是DOE、正式消融或论文独立样本。D0/D3的重跑只能以`sourceRunId`与原A2/A3关联，绝不增加
  正式样本数。
- 固定四臂顺序为D0=A2控制、D1=谱系档案+确定性方向pbest、D2=D1+同步四动作Qp/未裁剪奖励、
  D3=D2+10%预热/P5-G5冻结。不得把D1、D2或D3映射为其它历史P8配置，或在其中夹带PDDR、档案基数、
  子群配比、局部搜索顺序、教师池、Shift或压力掩码变化。
- A2→A3根因只能按预注册稳定退化门分类；当多个相邻步骤均触发退化时，必须写
  `COMPOSITE_BLOCK_UNRESOLVED`，不得为了修复方便把责任单独归给Qp、个人档案或双Q。
- 所有后续单变量实验必须先写入预登记：固定机制向量、输入/初群哈希、sourceRunId、明确唯一变量、
  指标口径、停止门与排除规则；运行后必须保存配置、状态、前沿、完整事件、指标、根因裁决和文件级SHA-256。
- 事件流输出不得用固定布尔值冒充捕获状态；必须同时记录总事件数和保留事件数。发现历史元数据错误时，
  以旁路校正表登记并保留原始运行，不得重写搜索结果或静默覆盖旧证据。
- 本拆分已确认D3冻结时序没有通过稳定退化门；但它不构成保留或删除该机制的论文结论。正式Jar和
  4500矩阵继续冻结/暂停，任何奖励、档案或双Q修复均须独立用户批准。

### 20.4 Qp 动作策略与 TD 奖励最小拆分（2026-08-24）

- `docs/evidence/V35-A3-D2-QP-SETTLEMENT/`是D1→Q0→D2的唯一证据入口。`Q0_QP_OBSERVE_ONLY`
  与D2使用相同谱系档案和四动作Qp pbest，但所有周期只观察：动作与档案仍真实执行，奖励、TD
  transition和Q表写入为零。它不是DOE、正式消融或论文独立样本，正式A0--A4和冻结Jar必须拒绝
  该policy。
- D1/D2的新增遥测先经2k行为兼容门：初群、FE、评价轨迹、前沿、个人领导/Qp/dual-Q事件和
  profile一致；纳秒计时不属于该契约。Q0三条50k运行须关联同seed的D1/D2 `sourceRunId`，并保留
  预登记、配置、完整事件、统一及两两reference、指标与文件级SHA-256。
- 当前预注册裁决为`QP_ACTION_POLICY_HARMFUL`：D1→Q0在2/3 seed满足稳定退化门（中位
  ΔHV=-2.1951%，中位ΔIGD=+11.2588%）；Q0→D2只有1/3 seed同时变差，不满足TD奖励稳定退化门。
  Q0的零表在30,000次动作中有29,146次`KEEP`（97.15%），与代码的稳定贪心并列取首个合法
  动作一致。因此当前可归因的是“未学习时四动作Qp的实际pbest选择策略”，不是TD奖励学习或
  P5/G5冻结。
- 此结论不批准直接删除个人档案、替换Qp动作、裁剪奖励、调档案容量或改变PDDR。下一步若研究，
  必须单独预注册一个只改变Qp动作策略、保持D1及其余语义不变的最小修复臂；未获用户批准前，
  4500矩阵继续暂停，DOE1配比、PDDR和正式Jar继续冻结。

### 20.5 Qp 冷启动并列破平：已否定的最小修复假设（2026-08-24）

- `docs/evidence/V35-A3-D3-QP-COLD-START-TIE/`是唯一证据入口。Q1与Q0保持相同档案、四动作、
  同步时序和`OBSERVE_ONLY_ALL_CYCLES`，唯一变量是零表贪心同分且`DIRECTIONAL`合法时优先选
  `DIRECTIONAL`，而不是稳定选择第一个合法动作`KEEP`。正式A0--A4、默认构造器、冻结Jar和正式
  Runner必须拒绝该诊断配置。
- Q1三条50k记录确认动作分布实际变化：`DIRECTIONAL`由Q0的237/30000提高到2104/30000，且TD
  transition、奖励样本和Q表写入均为0；因此这不是“实现未触发”。
- 但Q0→Q1虽有2/3 seed同时改善，中位`ΔHV=+0.8850%`、`ΔIGD=-2.0515%`，未达到预注册改善门；
  D1→Q1则3/3同时退化，中位`ΔHV=-1.3788%`、`ΔIGD=+16.3038%`。裁决固定为
  `COLD_START_TIE_BREAK_NOT_CONFIRMED`：不得将该tie-break晋升为正式修复、重新解释A2→A3根因、
  重做DOE或重启4500矩阵。
- Qp的任何下一步必须另行用户批准并重新预注册；不得围绕Q1继续枚举tie-break优先级、探索率、
  奖励裁剪或档案容量以追逐有利结果。所有新的单变量诊断仍须保存预登记、配置/输入/初群哈希、
  状态、前沿、完整事件、指标、裁决和顶层文件级SHA-256。

### 20.6 A2→A3→A4整体机制门（2026-08-24）

- 已接受的 Stage2 12个完整五臂/500k公平组在`100_2_3_1`上直接给出：A2→A3中位
  `ΔHV=-16.24%`、`ΔIGD=-24.93%`，A3不是可单独宣称有效的增量；A3→A4中位
  `ΔHV=+22.82%`、`ΔIGD=+37.85%`，而直接A2→A4仍为`+8.46%/+14.01%`，说明CA-TA-Lite
  在当前单实例先导中不只恢复A3损失，还使完整A4超过A2。唯一可复算裁决在
  `docs/evidence/V35-A2-A3-A4-CHAIN-VERDICT/`。
- 该结论是`100_2_3_1 × 12 seed`的先导，不得写成多实例论文结论。A3只能作为A4耦合链的
  中间臂；在没有新的多实例整体门以前，禁止声称个人档案、Qp或双Q冻结具有独立正贡献。
- 下一步只能是预注册的A2对A4多实例整体确认，或明确停止A3组合并保留A2主线；不得继续调Qp
  的tie-break、探索率、奖励、档案容量，也不得以此修改PDDR、DOE配比或恢复4500矩阵。

### 20.7 A2/A4多实例确认与Final分支纪律（2026-08-24）

- 下一算法实验只能按`docs/V35_A2_A4_MULTISCALE_CONFIRMATION_PROTOCOL.md`执行：六个从未用于当前
  有效开发/裁决总账的实例`20_2_4_1,20_5_3_1,50_2_4_1,50_5_3_1,100_2_4_1,100_5_3_1`、五个新seed
  `20260901..20260905`、A2/A4配对、各500k上限，共60条。不得替换、增删或看结果后重抽实例/seed。
- 所有60条必须使用Jar SHA-256
  `8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`；保持FM3、单族、序列无关SUT、
  `ShiftMode=NONE`、`GLOBAL_ORIGINAL`、`[20,40,20,20]`、当前LS顺序、P=5/G=5、rho=0与教师池关闭。
  未经用户新批准，不得改Qp、PDDR、Pacing、奖励、rho、CA-TA、压力掩码或局部搜索顺序。
- 每个`instance×seed`的A2/A4必须从同一独立快照起跑；运行接收、每实例reference构造与A4晋升/否决门
  完全以该协议为准。确认集只做候选判定，不进入正式PFref或论文统计。
- A4未通过时，裁决为`A4_NOT_PROMOTED`：停止救算法，A2成为主候选，旧A0--A4/4500 Master不得自动恢复。
  A4通过时也只授权Final freeze、production preflight和吞吐benchmark；4500条正式矩阵仍须用户单独批准。
- 任何后续运行、审计、诊断、预检、外部算法适配、模型实验或文档更新均须先建立证据目录，记录预登记、
  输入/配置/初群哈希、唯一runId和sourceRunId、执行状态、原始输出、分析脚本及文件级SHA-256。没有
  可反查证据链的结论不得用于路线决策或论文。

### 20.8 A4确认否决后的停止条件（2026-08-25）

- D-103确认已完成且裁决为`A4_NOT_PROMOTED`。60/60运行和30/30配对均有效，但100-job pooled
  HV/IGD门以及`100_5_3_1`单实例否决门失败；完整导入证据为
  `docs/evidence/V35-A2-A4-MULTIINSTANCE-CONFIRMATION/06-remote-analysis-import/`。
- A2是当前主候选，不等于已经完成Final freeze。后续只能先由用户批准并预注册新的A2 Final roster、
  对照范围与生产预检；不得自动把旧A0--A4/4500矩阵改名为A2矩阵或恢复运行。
- 不得在D-103的六实例/五seed确认集上调Qp、个人档案、双Q、CA-TA、PDDR、Pacing、rho、子群配比或
  压力掩码以挽救A4。若未来研究A4，必须使用新的未使用实例/seed和独立协议，且不得覆盖本否决结论。

### 20.9 A2主候选确认与Final roster纪律（2026-08-25）

- A2胜过A4的稳定性门不等于A2已经优于A0。唯一允许的下一主算法验证是
  `docs/V35_A2_FINAL_CANDIDATE_CONFIRMATION_PROTOCOL.md`：A0/A2、六个未使用的20/50/100-job实例、
  seed`20260911..20260915`、共60条500k配对运行。不得改用D-103的确认实例/seed，或将任何开发/确认数据
  重新包装为新held-out证据。
- 所有A0/A2运行必须使用冻结Jar `8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`、
  FM3、单族、序列无关SUT、Shift NONE、`[20,40,20,20]`、GLOBAL_ORIGINAL、当前LS顺序、P=5/G=5、rho=0、
  教师池关闭和phase-consistent预算。任何算法、PDDR、Qp、CA-TA、Pacing、rho或配比变化都会使本协议失效。
- A2只有通过协议的30/30配对、总体/实例/规模HV-IGD门、100-job否决门和TEC/TWC系统退化门，才获得
  `A2_FINAL_CANDIDATE_CONFIRMED`。失败即为`A2_NOT_PROMOTED`：停止Final freeze与正式矩阵，不重抽实例、
  不重跑以追逐结果，也不把A3/A4调参当作修复。
- 若A2通过，内部正式消融的建议roster是`A0 -> A1 -> A2`；A3/A4保留为负向/组合诊断，不得写成独立正贡献。
  即使A2通过，Final Freeze、production preflight、吞吐benchmark和任何正式矩阵仍须按对应协议与用户授权执行。

### 20.10 A2确认完成后的停止条件（2026-08-25）

- D-105的60条A0/A2独立确认已完成，裁决为`A2_NOT_PROMOTED`。总体配对中位`ΔCmax=-0.7410%`，
  且`100_8_3_1`触发100-job HV/IGD否决门；详情和可重算母表固定在
  `docs/evidence/V35-A2-FINAL-CANDIDATE-CONFIRMATION/05-decision/A2_NOT_PROMOTED_DECISION.md`。
- 当前不存在获准的V35 Final候选。禁止自动Final Freeze、production preflight、吞吐benchmark、A0--A4
  正式矩阵或外部算法正式对照；不得重抽实例/seed、重跑追逐结果，或通过改PDDR、Qp、CA-TA、Pacing、rho、
  子群配比来挽救A2/A4的否决结论。任何后续研究须由用户另行批准、预注册并使用新实例/seed。

### 20.11 FC5-T：100-job候选膨胀与利用断裂迁移审计（2026-08-25）

- 用户已批准的下一步是`FC5_100JOB_TRANSFER_V1`，不是把CFVF、Qp/双Q、CA-TA-Lite、FM3重新列为
  平权假设。首要问题仅为：FC-5已发现的“合并候选池ND膨胀 → GLOBAL_ORIGINAL PDDR压缩100工作槽位
  → 四方向代表未保留/未利用 → archive与working population脱节”是否在100-job退化实例迁移复现。
- 历史臂必须保持其冻结定义：A0/A2不得人工加入原本没有的Qp、双Q或CA-TA-Lite；A4已有的CFVF、双Q、
  CA-TA-Lite不得关闭。审计结论不得用作删除三项创新的依据；`GLOBAL_ORIGINAL`继续冻结，不得借本审计
  改PDDR、档案、DOE配比或局部搜索顺序。
- 先只读核验已有100-job正/负对照的字段。只有`Nmerge/Nunique/Nnd/Roverflow`、四方向代表生命周期与
  archive-working gap确实缺失时，才可用独立旁路Runner进行`50k → 100k → 250k → 必要时500k`逐级遥测。
  每一档必须先完成行为等价与证据验收；不得默认或批量启动500k。首档最多24条、均为预注册的50k观察重放。
- 本工作包的唯一证据入口是`docs/evidence/V35-FC5-100JOB-TRANSFER/`。所有遥测必须写入预登记、
  `sourceRunId`、输入/配置/初群哈希、完整原始输出、分析脚本、根因候选裁决和文件级SHA-256；无此证据链的
  结论不得驱动后续修复。H1成立只能写为`FC5_TRANSFER_CONFIRMED`（强机制迁移证据/root-cause candidate），
  仍须用另行预注册的单变量修复实验确认因果；H1不成立才依次解锁其它模块审查。

### 20.12 FC5-T 首档 50k 纠错与第二档 100k 筛查收口（2026-08-25）

- 首档 50k 分析已完成纠错（Luna A，只读复核原始数据）：PDDR 轮数=74（A0=14/A2=24/A4=36）、
  方向代表记录 296=74×4、archive–working gap 快照 74（71 个 cmaxGap=0、3 个非零）、
  唯一一次代表未保留=100_5_3_1/20260901/A4/E_E 5/6=0.8333；判据 2 仅 A2→A4 子块边界通过
  （ΔRoverflow=0.255≥0.25），A0→A2 子块 Δ=0.075 不通过，联合 H1 门不成立；
  裁决仍为 `INSUFFICIENT_EVIDENCE`，PDDR 仍 `KEEP_GLOBAL_ORIGINAL`。
  证据：`docs/evidence/V35-FC5-100JOB-TRANSFER/04-positive-negative-contrast/first-tier-50k-analysis/`。
- 第二档 100k 筛查（Luna B 执行 + Luna C 独立分析 + 主 Agent 审核）已收口，仅 6 条
  （100_5_3_1 × seed 20260901..03 × A2_CFVF/A4_BUDGET_AWARE_CATA，MaxFEs=100000）：
  全部 COMPLETED、illegal=0、dup=0、remainingFE<5000（A2≈96653–96680、A4=96025 为合法阶段一致尾停）、
  同 seed 两臂初群/snapshot/provenance 一致、evidence 反向复核 0 失败。100k 内全部 36 轮 PDDR
  Nnd∈[8,76] 从未 ≥90 或 >100（无候选池膨胀）；四方向代表 pool→next 保留率 100%（无丢失）；
  教师链路未断裂；A4 后半段 cmaxGap 转正峰值 3.65–5.94（相对 Cmax<1%）且 W2 教师曝光回落
  （total 23806→3693），但不满足情形 A（无 Nnd>100、无保留率下降）也不满足情形 B
  （cmaxGap 非零）→ 裁决 `FC5_TRANSFER_100K_INCONCLUSIVE`（情形 C）。
- 关键纪律：Nnd 增加（A4 Roverflow 0.38→0.71）但代表仍 100% 保留、教师仍正常利用 →
  “候选多”本身不是根因，**禁止据此修改 PDDR**。不自动升级 250k/500k；下一动作须用户决定并独立预注册。
  证据入口：`docs/evidence/V35-FC5-100JOB-TRANSFER/05-decision/FC5_TRANSFER_100K_DECISION.md` 与
  `.../second-tier-100k-analysis/`（evidence-sha256.tsv 175 项反向验证 0 失败）。

## 21. Post-FC5 Failure Replay、单旋钮校准与Final治理（2026-08-29）

本节是后续开发的当前最高优先级执行纪律。完整批准原文固定在
`docs/V35_POST_FC5_EXECUTION_MASTER_PLAN.md`；若早期路线建议与本节冲突，保留历史记录，但执行以本节和
后续明确的新决策为准。

### 21.1 当前科学状态与250k裁决

- 当前不存在获准的Final候选：`A2Promoted=false`、`A4Promoted=false`、
  `FinalCandidateApproved=false`、`FINAL_FROZEN=false`、`formalMatrix=PAUSED`。
- 12/12条FC5-T 250k诊断均完成并通过运行门，`actualFE=250000`。裁决固定为
  `FC5_TRANSFER_NOT_CONFIRMED_AT_250K`：最大严格`Nnd=92`，`Nnd>100`轮数为0；困难/正例中位
  `Roverflow=0.630/0.592`，差值仅`+0.038`；四方向pool→next最大差仅7.5个百分点；A4困难实例
  archive-working Cmax中位gap为0。不得继续把“ND overflow/PDDR容量不足”作为首要根因。
- 该否证不证明PDDR在所有情形最优，也不授权删除CFVF、Qp/Qg双Q或CA-TA-Lite。当前冻结：
  `PDDR=GLOBAL_ORIGINAL`、`CFVF=MANDATORY_FINAL_COMPONENT`、`DualQ=MANDATORY_FINAL_COMPONENT`、
  `CATA=MANDATORY_FINAL_COMPONENT`。
- 当前最有根据但尚未确认的机制候选是：teacher identity concentration → Qp/Qg重复暴露 → CFVF四向量
  放大 → CA-TA定向强化 → 目标空间覆盖收缩。未通过Failure Replay前不得写成已证实因果。

### 21.2 禁止项与唯一主线

- 唯一主线为：`Failure Replay → Single-Knob Calibration → Instance Race → Validation → Final Freeze`。
- 禁止修改PDDR或启用`REGION_AWARE`、`BP_RESERVED`、crowding、NSGA-III/reference-vector或固定区域配额。
- 禁止调整mixture、Pacing、`rho`、P5/G5、Q状态/动作/奖励、个人档案容量、LS顺序；保持
  `CA-TA-Lite → inherited LS`。禁止Cheap-Test、4500矩阵和未预登记的250k/500k分支。
- A0/A2/A4必须保持各自冻结语义；不得给A0/A2补入原本没有的机制，也不得为诊断关闭A4中的CFVF、
  双Q或CA-TA-Lite。任何结论不得直接用于删除三项创新。
- 每次新计算必须回答`Which preregistered gate authorizes this run?`；没有唯一、已批准的Gate即
  `DO_NOT_RUN`。不得因“可能有用”自行增加实验。

### 21.3 Phase 0与工具封板

- 在任何新训练前，先完成四项0-FE治理物：`historical-failure-seed-registry.csv`、
  `instance-exposure-role-registry.csv`、`baseline-fair-readiness.csv`和
  `FAILURE_REPLAY_REFERENCE_CONTRACT`；必须登记历史失败seed的选择规则、snapshot物理路径/哈希、
  原始front、指标、front hash和历史A2 checkpoint可用性。
- 历史失败case固定为`100_5_3_1`；从满足历史failure class的seed中取最小seed ID，禁止选择最差seed。
  该case/seed仅用于诊断，不得进入Configuration Race、Validation、Formal或Final Test。
- Step 0为唯一A4 50k OFF/ON工具验收。执行前先审计现有
  `V35-FC5-MIDHORIZON-DIAGNOSTICS/26-final-runtime-jar-validation`是否已满足完全相同的合同；若满足则
  直接登记为已完成，禁止重复运行；只有身份或合同字段缺失时才允许补跑。
- 工具等价门必须比较初群、真实RNG消费、候选序列、Qg/Qp、Q表、CFVF、PDDR、CA-TA、working
  population、规范前沿和核心事件hash。遥测计时不得进入CA-TA信用。通过后诊断工具永久封板。

### 21.4 F1/F2/F3 Failure Replay门

- F1必须单独预注册：当前冻结A4、历史失败case、精确历史snapshot、500k、telemetry OFF。若精确snapshot
  不存在，只能称`CURRENT_SEMANTICS_REPLAY`，不得冒充历史状态复现。Cmax不属于failure复现硬门。
- F1若未复现历史failure class：写`FC5_HISTORICAL_CASE=CLOSED`，禁止F2/F3，转入
  `PROSPECTIVE_CURRENT_SEMANTICS_STABILITY`或假设驱动的Teacher Exposure Calibration。
- F1若复现，才允许F2：同实例/seed/snapshot/A4/500k、telemetry ON。F2须同时通过Outcome与完整
  Behavioral Equivalence；失败即`FC5_MECHANISM=UNRESOLVED`，禁止定义`t*`或用F2/F3构造因果链。
- 只有F1通过、F2行为等价通过且历史A2 checkpoint不可用时，才允许F3同snapshot的A2/500k/ON配对。
- F1/F2/F3与全部checkpoint必须共享一个冻结的empirical PFref、ideal/nadir、normalization、HV/IGD实现、
  目标顺序、失败阈值和共同checkpoint对齐规则；禁止按50k/100k/250k/500k更新reference。
- `t*`只可定义为最早的共同phase-consistent checkpoint：终局同一failure criterion在该点成立，并在下一
  共同checkpoint仍成立。只有`t<t*`的稳定异常具有root-cause-candidate资格。

### 21.5 根因竞争与单一repair family

- 优先级固定为：Teacher concentration → CFVF在teacher集中后的四向量放大 → CA-TA目标区域集中；PDDR
  降级为旁路观察，只有`t*`前出现持续survival anomaly才可重新成为repair候选；FM3最后审查。
- Teacher必须记录Qp/Qg Top1/Top5、controller-local entropy、unique teacher、improvement/exposure和
  objective-region；CFVF记录JS/FA/MA/WA归一化编辑与认知/社会继承；CA-TA记录Test/Apply FE、宏邻域、
  接受和Cmax/TEC/TWC/Balanced贡献。
- 每轮只允许一个repair family。若Failure Replay仍无法分辨，停止扩大根因诊断，不追加350k/400k或
  多seed机制体检，默认进入`HYPOTHESIS_DRIVEN_DEVELOPMENT_CALIBRATION`，不得声称DualQ根因已证实。

### 21.6 Teacher Exposure Calibration与Instance Race

- 校准唯一允许改变的是Q动作完成后、该动作原合法候选集合内的teacher identity selection；不得改变
  Q action/state/reward、P/G、rho、PA容量、CFVF、CA-TA或PDDR。
- 唯一旋钮为`lambda`：C0=0（当前）、C1弱、C2中、C3强；必须证明`lambda=0`与当前实现完全等价，
  包括RNG、teacher、动作、事件和轨迹。Qp/Qg使用各自controller-local exposure。
- Race固定为4配置×4个DEVELOPMENT实例（20、50、normal100、hard100）×2配对seed×250k；诊断选中
  的failure seed不得作为hard100 race seed。
- 先过100-job robustness gate：任一100-job实例两seed中位`ΔHV<-10%`或`ΔIGD<-20%`即淘汰；之后按
  四实例聚合的HV/IGD平均rank选Top2，Cmax/TEC/TWC只作破平和解释。空集规则按主计划预注册，不得放宽。
- Top2在normal100/hard100×2seed×500k决赛，选唯一`V35-R`后设置
  `INTERNAL_DEVELOPMENT_CLOSED=true`，停止继续调整lambda、mixture、Pacing、Q、PDDR、CFVF和CA-TA。

### 21.7 Validation、Final与正式实验

- Gap Probe与Failure Replay使用独立reference；只能输出`GAP_WITHIN_5/GAP_5_TO_15/GAP_GT_15`，稳定
  大于15%为RED，否则仅`NOT_RED`，禁止称GREEN。
- V35-R产生后才能使用未污染的`VALIDATION_RESERVED`实例进行50/100/150或200-job mini benchmark；
  所有外部算法须`fairReady=true`。首档1 seed只作Go/No-Go，不作论文显著性。
- Validation失败后若修改V35-R，该轮validation实例立即转为`CONTAMINATED_DEVELOPMENT`，不得再次用作holdout。
- Champion Gate通过后才可`FINAL_FROZEN=true`。Formal主比较先15实例×10seed，是否扩45实例和20seed由
  方差、置信区间、平均rank、效应量和功效决定；正式消融采用leave-one-component-out，不要求A0→A4单调。
- Final Freeze前禁止用anytime指标选repair或Race配置；Final Freeze后才可作为论文附加分析。

### 21.8 证据与资源纪律

- 每一工作包必须先建证据目录，保存预登记、输入/配置/snapshot/Jar哈希、runId/sourceRunId、原始输出、
  分析脚本、裁决和文件级SHA-256；任何结论必须可从原始数据自动重算。禁止手工抄写汇总数作为唯一证据。
- Failure Replay记录CPU affinity、JVM、heap、host、并发进程和wall-clock环境；F1/F2/F3同一计时域不得与
  大批baseline并发。若真实wall-clock参与CA-TA credit，按算法正确性问题处理。
- 正式矩阵继续暂停，直至Champion Gate、Final Freeze、production preflight和独立用户授权全部完成。

## 22. V35竞争优势、DOE迁移、Validation与正式实验治理（2026-08-30）

本节由D-110启用，覆盖第21节中“Failure Replay/Teacher Exposure Calibration是当前唯一开发主线”的
旧表述；第21节的历史证据、Failure Replay结论、PDDR降级和停止条件继续有效。完整执行路线见
`docs/V35_COMPETITIVE_SUPERIORITY_EXECUTION_ROADMAP.md`。

### 22.1 当前状态与唯一下一阶段

- 当前不存在获准Final：`A2Promoted=false`、`A4Promoted=false`、`FinalCandidateApproved=false`、
  `FINAL_FROZEN=false`。旧冻结A4只能称`A4_LEGACY`，不得覆盖或改名冒充Final。
- 当前唯一允许准备的工作包是`V35_GAP_PROBE_P0`：0-FE预登记、角色/seed/snapshot/Jar/reference合同和
  四算法集成贯通准备。它不授权16条500k，不授权上传或远端启动。
- Gap Probe固定四算法：A4-Pacing、HMOPSO-QGS-F、SPEA2-F、NSGA-II-F；固定一个DEVELOPMENT 50-job、
  `100_5_3_1`和两个paired seeds。禁止先看结果再挑“最强external”。
- Gap仅输出`GAP_WITHIN_5/GAP_5_TO_15/GAP_GT_15`；稳定大于15%为RED，否则仅NOT_RED，均不等于Final通过。

### 22.2 单一repair family与杠杆门

- Gap后先做0-FE leverage audit。Dual-Q→CFVF、CA-TA预算、Qp/CA-TA信用时序是候选域，不是三个可同时
  赛马的repair family。
- 每轮只能选择一个repair family；C0/C1/C2/C3必须是同一语义轴的当前/弱/中/强四档。禁止用四个标签
  包装四种不同机制改法。
- repair必须量化触发、teacher/action改变、CFVF offspring影响和FE覆盖；直接或可证明传播覆盖不足10%的
  旋钮默认在实现前停止。低频但高传播候选必须用真实调用链和事件证明，不能靠推测豁免。
- Teacher Exposure旧方案覆盖仅1.12%，已关闭，不得重新包装后启动；这不等于否决Teacher假设或Dual-Q。
- CFVF、Dual-Q、CA-TA-Lite仍是最终研究路线的强制保留组件；任何删除均需新的用户决策，不能由诊断代理自行执行。

### 22.3 分级开发与防止holdout污染

- 100k仅作cheap rejection：默认4配置×4 DEVELOPMENT实例×2seed，边界配置才可预登记补第三seed。
- Top2使用4实例×3seed×250k；500k development final使用Top2、QGS和Gap中按冻结规则确定的最强external。
- 实例角色必须互斥：`DEVELOPMENT`、`VALIDATION_RESERVED`、`FINAL_TEST_RESERVED`、`FORMAL_MAIN`、
  `CONTAMINATED_DEVELOPMENT`。Validation失败后若修改算法，该批立即转为污染开发集，禁止再次作holdout。
- Validation固定先用未污染50/100/150或200-job各一例、V35-R/QGS/最强external、2seed×500k。
- Final Freeze后首次打开FINAL_TEST_RESERVED；其结果再差也不得改算法，只能继续预注册计划、停止扩大或收缩主张。

### 22.4 DOE迁移纪律

- DOE1及`20/40/20/20`继续有效，不因V35-R出现自动作废。
- 先做四配比×3实例×3seed×250k迁移门。原配比仍稳健第一即停止DOE。
- 只有一个challenger时，先做BASE vs challenger的18条500k确认；不得直接触发135+60条完整DOE。
- 只有多个配比广泛占优、BASE明显掉队、交互超过2个百分点或排名大面积反转，才可另行预注册完整DOE。
- DOE2 Pacing不自动启动。

### 22.5 消融与正式baseline纪律

- Final Freeze后正文主消融采用leave-one-component-out：FULL、FULL-CFVF、FULL-DualQ、FULL-CA-TA、
  FULL-FinalCoordination、QGS。每个移除臂先做依赖合法性审计；无合法反事实时按bundle移除并诚实名命。
- A0→A1→A2→A3→A4_LEGACY→V35_R_FINAL保留为开发历史/附录链，优先复用证据，不恢复旧4500矩阵，
  不要求逐臂单调改善。
- 正式消融首档6实例×10seed×6臂；是否扩9实例或20seed由方差、CI和论文需要决定。
- 正式baseline roster目标为QGS、QLS、MOPSO、MOPSODS-DE、MOHEADE、NSGA-II、SPEA2和V35-R；
  QMOEA无可信来源则保持PENDING，不得冒充。
- Formal Stage 1为8算法×9实例×5seed；Formal Main为8算法×45实例×10seed。是否补到20seed必须由
  预注册的功效、方差和排名稳定性门决定，禁止自动翻倍。

### 22.6 统计、证据与授权

- 每个实例等全部正式算法完成后才冻结共同PFref；Development、Gap、Validation、Final Test和Formal的
  reference完全隔离。
- 正式统计先在每个instance内聚合seed，再以instance为主要配对单元；使用Friedman、paired Wilcoxon、
  Holm和paired rank-biserial correlation。禁止把45×seed当成独立问题伪增样本量。
- 每次计算前必须回答`Which preregistered gate authorizes this run?`，并保存预登记、角色表、输入/Jar/
  config/snapshot哈希、原始输出、自动分析、裁决和文件级SHA-256。
- 当前保持：`gapProbeStarted=false`、`validationStarted=false`、`formalMatrixRunning=false`。任何500k、上传、
  DOE、Validation、Final Test、消融或正式矩阵均需用户针对对应工作包单独批准。


## 24. V35-GAP-LOCAL-FE-PACING-REPAIR 附加纪律（2026-08-31，追加）

1. 原杠杆裁决名 `SELECT_CATA_BUDGET_REPAIR` 已由
   `docs/evidence/V35-GAP-LEVERAGE-AUDIT/NAMING_AND_CAUSAL_BOUNDARY_CORRECTION.md`
   更正为 `SELECT_LOCAL_FE_PACING_REPAIR`；后续文件一律用新名，旧名仅存在于历史引用。
2. repair 单旋钮 `betaMax`（betaMin=0.25 冻结；C0=0.65/C1=0.55/C2=0.45/C3=0.35）；
   冻结正式Jar `8dad8f40…bad8b9` 不得覆盖或重建；repair 只能经独立实验Jar
   `jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar`（`a0788580…`）与
   `V35LocalFePacingRepairRunner` 的六flag CLI 运行；正式路径拒绝 C1–C3。
3. 20k 机制门已 PASSED（10/10，2026-08-31）：C0==REF_A4 行为逐位等价、FE回流成立、
   CFVF/Dual-Q/CA-TA 真实触发、PDDR 保持 GLOBAL_ORIGINAL、池级归因缺口如实登记
   （NOT_EXPORTED_BY_FROZEN_JAR）。
4. 50k 预登记为 **16 条**（4配置×2实例×2seed，seed 20260907/20260914）；
   旧"24条"计数作废。50k/250k/DOE/validation/正式矩阵均须用户逐项批准，
   不得自动启动；repair family 证伪则记 REJECTED 并停止，不换轴不调参续命。

## 25. V35-LOCAL-FE-PACING 50k 完成状态（2026-08-31，追加）

1. 20k 门范围更正已落盘：`docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR/06-20k-scope-correction/20K_GATE_SCOPE_CORRECTION.md`。
   `20kImplementationGate=PASSED` 仅指实现门；`strictPreregistered20kGate=NOT_FULLY_PASSED`、
   `doseResolutionAt20k=NOT_RESOLVED`（C1=C2=C3=4900 为 exact-stop 恒等式
   totalLocal=MaxFEs−globalPhaseFE 的结构性并列）。`build_gate.py` 已含跨配置聚合剂量门
   （20k 重跑输出 `AGGREGATE_DOSE_GATE_20K=NOT_RESOLVED`，exit 2），"10/10 单条通过"自此不得再等价"整门通过"。
2. 50k 已按冻结预登记 `07-50k-preregistration/50K_PREREGISTRATION.md` 完成：16/16 运行验收通过
   （训练机 `zhangbo-v35-local-fe-pacing-50k-20260831`，每臂 0 重跑），闭合调度预测 16/16 精确命中，
   4/4 公平组初群双 hash 一致、actualFE 极差 4641<5000。
3. 预登记偏差（运行前冻结）：前沿级共同FE检查点 `NOT_EXPORTED_BY_FROZEN_JAR`（评估循环在冻结正式Jar内），
   主口径=统一实际FE标量检查点（F_common=40000）；分配上限 `CLOSED_FORM_SCHEDULE_RECONSTRUCTION`
   （20k 验证 8/8、50k 验证 16/16）；池级 PDDR 归因继续 NOT_EXPORTED。
4. 剂量分辨 50k **PASSED**（G1–G4 全过；localFeShare 0.3764>0.3364>0.2980>0.2842，
   相邻降幅 4.00/3.84/1.38pp）；性能筛查唯一保留候选 **C3（betaMax=0.35）**；
   C2 触发 BUDGET_SENSITIVITY_CONFLICT（50 实例 TWC 终态 +1.19% vs common-FE −0.11% 符号翻转）出局；
   C1 困难实例无改善信号出局。最终裁决 `ONE_CANDIDATE_ADVANCES_TO_250K`。
5. 保持：`250kEligible=true`、`250kPreregistered=false`、`250kStarted=false`、`DOEStarted=false`、
   `validationStarted=false`、`FinalCandidateApproved=false`、`formalMatrixRunning=false`；
   PDDR/CFVF/Dual-Q/CA-TA/正式Jar/实验Jar 全部零改动。250k 须用户单独批准，不得自动启动。
6. 证据：`docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR/{06..11}-*/`，
   全树清单 `evidence-sha256.tsv`（1255 条目，checked=1255 missing=0 mismatch=0）。

## 26. 50k 候选裁决勘误（2026-08-31，追加）

1. `docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR/12-50k-decision-correction/` 以 append-only
   方式修正 50k 候选裁决：`C1Rejected=true` 维持；`C2EligibleFor250k=true`、
   `C3EligibleFor250k=true`；`50KDecision=TWO_CANDIDATES_ADVANCE_TO_250K`。
   原出局理由（C2 双口径 TWC 符号翻转）降级为 MINOR_FLUCTUATION：翻转幅度 ≈0.235pp
   （pooled +0.1293% vs −0.1059%），且原双口径为"终态完整前沿 HV/IGD vs 共同FE标量极值"
   的口径不对称比较（偏差 D1），淘汰证据不足。
2. 原因（教训登记）：预登记门在口径不对称未被消解前，不得以符号一致性作淘汰依据；
   后续门必须定义实质性阈值与 seed 一致性要求（已在 250k 任务书 §九 落实）。
3. 250k 实验臂固定为 C0/C2/C3；C1 不参与。250k 仍未启动（`250kStarted=false`）。

## 27. 250k 确认实验完成（2026-08-31，追加）

1. 50k 勘误已执行（§26）：C2/C3 双候选晋级并完成 250k 确认（18/18 运行，6/6 公平组，
   训练机 `zhangbo-v35-local-fe-pacing-250k-20260831`）。
2. 检查点观察器（V2 实验Jar `c2cf4294…`，正式Jar零改动）已通过 OFF/ON 等价门
   （20k/50k 两门 × C0/C2/C3，OFFvsON 126 行 0 DIFFER）与对冻结存储运行的忠实性门
   （114 行 0 DIFFER）；`checkpointObserverValidated=true`。检查点以逐次接纳冻结，
   observedFE==target、overshoot=0，四类 frontType 严格分列。
3. 250k 裁决：**NO_REPAIR_CANDIDATE**——C2 三门失败（安全 −3.19%、困难无信号、
   灾难门 2/3 seed）；C3 四门通过但检查点一致性门 CONFLICT（50_2_3_1 上 ck100000
   −6.87% / ck150000 −5.15%，3/3 seed 一致，终态仅 +0.19%，属实质性反转非
   MINOR_FLUCTUATION）。**LOCAL_FE_PACING repair family 按证伪条款 REJECTED**：
   不调参续命、不寻找第五个 betaMax；任何新修复方向须重新走杠杆审计流程。
4. 保持：`500kStarted=false`、`DOEStarted=false`、`validationStarted=false`、
   `FinalCandidateApproved=false`、`FINAL_FROZEN=false`、`formalMatrixRunning=false`；
   PDDR/CFVF/Dual-Q/CA-TA/mixture/正式Jar 零改动。
5. 证据：`docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR/{12..18}-*/`。

## 28. Pareto覆盖杠杆审计完成（2026-08-31，追加）

1. 证据治理收口完成：`docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR/19-evidence-governance-correction/`
   ——15目录5项清单漂移（setupFileSha256 63/64位转录截断的授权修复波及）已登记收口，
   pre/post清单闭合，LOCAL-FE-PACING顶层清单重建2353项0缺失0不匹配，
   `evidencePackageFinalSignoff=true`；失败arm日志被重试覆盖已如实登记
   （failedAttemptArmLogsPreserved=false，摘要证据保留）。
2. 只读审计（`docs/evidence/V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT/`，newFEConsumed=0）
   最终裁决 **NO_ACTIONABLE_LEVER**（无ROOT_CAUSE_CANDIDATE）：
   H1/H2=INSUFFICIENT_EVIDENCE（250k候选级PDDR遥测NOT_EXPORTED+front级反证：
   potentialHvRecovery≤0.79%且0/90行达2%门、困难vs正常ratio差−5.21pp方向相反、
   50_2_3_1候选级数据零命中；FC5-250K历史已否证溢出型利用断裂链）；
   H3=NOT_CONFIRMED（困难vs正常top1Share差+1.75pp≪20pp门）；
   H4=INSUFFICIENT_EVIDENCE（来源级ND/HV归因NOT_EXPORTED）。
3. 观察性结论（非因果确认）：覆盖崩塌指向生成侧多样性不足，而非保留侧压缩——
   被丢弃候选在目标空间近冗余。诊断能力缺口已登记：250k候选级PDDR/来源归因遥测
   （含正常实例对照）是未来任何根因闭环的前提，属诊断工作包而非修复杠杆。
4. 保持：`localFePacingRepairFamily=PILOT_REJECTED`、`betaMax=0.65`、`PDDR=GLOBAL_ORIGINAL`、
   `newRepairImplemented=false`、`newExperimentStarted=false`、`DOEStarted=false`、
   `500kStarted=false`、`FinalCandidateApproved=false`、`formalMatrixRunning=false`。

## 29. Campaign P1 来源贡献诊断完成（2026-08-31，追加）

1. 章程已立项：`docs/evidence/V35-FINAL-COMPETITIVE-RECOVERY-CAMPAIGN/00-charter/CAMPAIGN_CHARTER.md`
   （十二阶段路线图 + 全局停止条件：≤1修复族/≤1轮50k/≤1轮250k/≤1轮500k；Paper禁写清单生效）。
2. P1 诊断（`docs/evidence/V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1/`）：
   V3诊断Jar（`bbb9ccd6…`，影ZhangBoMOHPSOQ 11处纯观察patch+来源账本/PDDR轮账本；
   正式Jar字节不动；等价门190 IDENTICAL+16 ON_ONLY+0 DIFFER → sourceLedgerValidated=true）
   + 6×100k诊断运行（C0×2实例×seed20260919-21，全首跑成功）。
3. 裁决：**NO_SOURCE_LEVEL_FAILURE**——占评估量96.8%的CFVF（e=0.96/1.11）与inherited LS
   （e=1.15/0.86）价值效率与生成占比相称、逐seed健康；唯一低效（CATA e≈0.04）FE占比3.1%
   低于5%实质性门槛。生成侧无失效模块，无证据支持任何来源级修复族。
4. 按campaign P2规则：repairFamilyBudget=UNSPENT、**algorithmOptimizationClosed=true**
   （两轮独立证据链：预算侧LOCAL_FE_PACING否证 + 来源侧无失效）。等待用户复核后进入
   P5（Final对比：A2 vs A4 500k多实例）或直接冻结。
5. 保持：`newFEConsumed=0(诊断外)`、`PDDR=GLOBAL_ORIGINAL`、`betaMax=0.65`、
   `DOEStarted=false`、`500kStarted=false`、`FinalCandidateApproved=false`、
   `formalMatrixRunning=false`。执行事件：utilization>0.98阈值系250k档沿用的校准错误
   （100k尾停结构性上限0.95），实质判据全PASS，已如实登记。

## 30. SOURCE-ATTRIBUTION-500K Phase A 三人共识冻结纪律（2026-08-31，追加）

完整执行合同：`docs/V35_SOURCE_ATTRIBUTION_500K_PHASE_A_PLAN.md`。本节覆盖第29节中
“`algorithmOptimizationClosed=true`后直接进入Final对比或冻结”的下一步表述，但不推翻P1的
`NO_SOURCE_LEVEL_FAILURE`历史裁决。新授权只允许一次有限、只观察的500k纵向来源诊断；算法优化、
repair、DOE、Configuration Race、Validation、Final和Formal仍关闭。

### 30.1 当前授权与冻结状态

```ini
PHASE_A_AUTHORIZED_ONLY=true
DOE_AUTHORIZED=false
QP_V2_AUTHORIZED=false
CONFIG_RACE_AUTHORIZED=false
VALIDATION_AUTHORIZED=false
FORMAL_AUTHORIZED=false

A2Promoted=false
A4Promoted=false
FinalCandidateApproved=false
FINAL_FROZEN=false
PDDR=GLOBAL_ORIGINAL
mixture=20,40,20,20
betaMax=0.65
rootCauseCandidate=NONE
formalMatrixRunning=false
```

- `algorithmOptimizationClosed=true`继续禁止无证据调算法；Phase A是诊断例外，不代表优化重开。
- Phase A最多产生`SOURCE_LEVER_CANDIDATE`，不得写`ROOT_CAUSE_CONFIRMED`。
- 禁止重启REGION_AWARE、BP_RESERVED、ORDER_SWAP、soft-freeze、gb15、Cheap-Test、A5 teacher pool、
  teacher-lambda、betaMax pacing、Qp cold-start tie、reward-clipping performance repair和DOE1 mixture。

### 30.2 0-FE先决门

- NORMAL不得手工指定，必须由实例角色注册表与accepted ledger确定性解析；100-job、DEVELOPMENT、
  Current-A4无failure veto、有accepted 500k与可冻结reference，且不属于诊断/Validation/Final Test保留集。
  多候选先取HV/IGD同时non-failure，再取字典序最小；输出完整候选及淘汰理由。无法解析即`DO_NOT_RUN`。
- HARD固定`100_5_3_1/20260901/A4/CASE_SELECTED_DIAGNOSTIC_ONLY`，永久禁止进入DOE、配置、Validation、
  Formal和Final Test。
- 一级source只能为`GLOBAL_CFVF/CATA/INHERITED_LS/PARENT_CARRYOVER`；运行后不得增加第五类重分析。
- HARD继续用冻结Failure Replay reference；NORMAL reference只能用运行前accepted历史front，新run不得回灌。
- 运行前冻结WHVGShare/ExclusiveNDShare和`t_div`阈值。无可比历史时，source deficit fallback为2.0pp或
  10.0pp且连续两25k窗；performance fallback为HV progress deficit≥1.0pp且IGD improvement deficit≥10pp，
  连续两窗。运行后禁止改阈值。

### 30.3 Observer与500k硬门

- Observer必须独立Jar、只存fingerprint/source/FE/三目标和生命周期标志；禁止保留完整Solution或无限
  JS/FA/MA/WA数组。20k内存preflight要求`estimated500kPeak < 0.60×assignedJavaHeap`，失败不允许靠任意
  扩堆掩盖，只能优化observer存储。
- 必须静态确认真实wall-clock是否影响CA-TA决策。若影响，HARD/NORMAL须单JVM、固定CPU affinity、
  同资源且隔离其它训练负载。
- `100_5_3_1/20260901/A4/20k`执行Observer OFF/ON行为等价：初群、RNG、Qg/Qp、teacher、CFVF候选、
  目标、PDDR、CA-TA、Q表、FE、working population、decision-front全部相等。失败即停止。
- 等价与内存门通过后冻结schema/Jar；任何新增字段、来源标签、生命周期、buffer或hash均须重跑20k。

### 30.4 唯一运行矩阵与预算

顺序固定，前门未过不得启动后项：

1. A4 HARD 500k ON；终局必须复现历史failure class，否则停止；
2. resolver选出的A4 NORMAL 500k ON；
3. 仅G1成立时，允许同HARD case的A2 500k ON条件控制。

每25k nominal FE保存phase-consistent snapshot、累计和窗口指标。科学预算1.0M FE，条件A2后最多1.5M FE；
禁止新增第三实例、第二diagnostic seed或更多arms。

### 30.5 四类唯一出口

- `G1_GLOBAL_CFVF`：GLOBAL来源在hard-normal的WHVGShare或exclusiveND deficit连续两窗过冻结门，首次
  不晚于`t_div`，且不能被更强生存异常解释。
- `G2_QP_CFVF`：仅G1后运行A2；A4崩而A2不崩。只登记未来`QP_CANDIDATE_SET_POLICY_V2`候选，
  `IMPLEMENTATION_AUTHORIZED=false`。A2同样崩则归G1 common-CFVF。
- `G3_CATA`：GLOBAL未过G1、CATA deficit过门且消耗实质local FE。没有运行前已命名并可exact退化current
  A4的单轴时，`AUTHORIZED_REPAIR_FAMILY=NONE`，不得事后发明邻域比例/奖励/Apply修改。
- `G4_NO_ACTIONABLE_LEVER`：G1/G3均未过门，立即`OLD_A4_DIAGNOSTIC_CLOSED=true`，永久停止继续追PDDR、
  pacing、teacher exposure、source矩阵或盲目多参数DOE；Qp-v2只保留结构计划，不自动实现。

最终properties只能是G1、G2、G3或G4。Phase A结束必须向用户报告并停止；Phase B、任何repair、Qp-v2、
DOE、Validation、Final Freeze或正式实验均须新的明确授权。

## 30. SOURCE-ATTRIBUTION-500K Phase A0 完成（2026-08-31，追加）

1. 依据 `docs/V35_SOURCE_ATTRIBUTION_500K_PHASE_A_PLAN.md` v1.0（三文档SHA核验MATCH）完成
   Phase A0 0-FE预登记：`docs/evidence/V35-SOURCE-ATTRIBUTION-500K/00-preregistration/`（20文件，
   清单闭合0缺失0不匹配）。裁决：**PHASE_A0_PREREGISTRATION_PASSED**（无DO_NOT_RUN触发项）。
2. NORMAL Control规则化解析：**NORMAL=100_2_3_1**（CONTAMINATED_DEVELOPMENT=开发类；12条
   accepted A4 500k、A0→A4双正、raw fronts冷归档12/12哈希一致；9候选逐项淘汰记录在案；
   seed 20260901未消耗）。HARD=100_5_3_1/20260901/A4绑定闭合（快照84d84523…全库唯一物理副本）。
3. 阈值冻结：matched-window P95不可用（三份历史telemetry逐项NOT_COMPARABLE，未放宽标准）→
   fallback（WHVGShare deficit≥2.0pp OR ExclusiveNDShare deficit≥10.0pp，连续2窗）；
   t_div fallback（HV progress deficit≥1.0pp AND IGD rel-improvement≥10pp，连续2 checkpoint）；
   recompute脚本 --audit/--selftest 双PASS。四类一级来源映射至真实调用点（FINAL_EVALUATE并入
   GLOBAL_CFVF+二级护栏字段）；wallClockInfluencesSearch=false（A4/A2语义内，七项逐一审计）；
   内存模型流式设计+硬门 estimated500kPeak<0.60×heap。主Agent裁决：nominalFE派生列+B_0账本
   重构定义（附录冻结进 observer-schema.md）。
4. 未来RunKey冻结：SA-HARD→failure-class复现门→SA-NORMAL→(仅G1)SA-A2-CONDITIONAL→G1-G4→强制停止；
   禁止第三实例/第二诊断seed/其他arm。Observer未实现、未上传、未消耗FE。
5. 保持：`PHASE_A_AUTHORIZED_ONLY=true`、`algorithmOptimizationClosed=true`、
   `sourceAttribution500kStarted=false`、`DOE/QP_V2/CONFIG_RACE/VALIDATION/FORMAL_AUTHORIZED=false`、
   `formalJarChanged=false`、全部冻结语义未动。

## 31. Phase A0 修正完成（2026-09-01，追加）

1. Phase A0 初版被独立验收退回，两项阻断问题：A) 多来源重复目标点被 FIRST_ADMISSION
   错误归因（假 G1/G3 信号）；B) 内存外推公式 heapUsedPeak_OFF_20k×25 无效。
   状态曾置 NEEDS_REVISION（修正前快照：phase-a0-decision.pre-correction.properties /
   source-attribution-thresholds.pre-correction.json）。
2. 修正（PHASEA0-CORRECTION-V1）：归属规则=COUNTERFACTUAL_PRODUCER_SET
   （Wt^-s 仅剔除 producerSet=={s} 的三元组，共享点对任何单来源反事实贡献为0；
   first-admission 降级 DESCRIPTIVE_ONLY）；内存=分解模型（baseline 有界不×25，
   transient=max(实测delta, 有界cap+unflushed cap)，safety=max(20%,256MiB)，
   硬门<0.60×heap 严格小于，等于即fail-closed；20k无法证明基线有界→
   MEMORY_MODEL_INSUFFICIENT）。NORMAL 文字勘误两处（seed表述/100_2_4_1
   REFERENCE_MATERIAL_PARTIAL），不改变100_2_3_1选择。
3. 验证：T1–T8 开发者自测（threshold_recompute.py --selftest/--memory-selftest）与
   主Agent独立复核（期望值由测试合同显式给定）双路径全部PASS；清单27项0/0闭合。
4. 修正后状态：phaseA0Decision=PHASE_A0_PREREGISTRATION_PASSED、
   metricAttributionContractValidated=true、multiSourceCounterfactualSemanticsValidated=true、
   memoryPreflightModelValidated=true、memoryPreflightExecuted=false、memoryGatePassed=false、
   observerImplemented=false、newFEConsumed=0、remoteExperimentUploaded=false、
   sourceAttribution500kStarted=false。等待独立复验。

## 32. Phase A0 证据重包装（2026-09-01，追加）

独立复验意见：核心修正全部通过，但证据清单未闭合（phase-a0-correction-verification.md
未登记27/28；独立复核脚本未绑定SHA）。已完成0-FE重包装：顶层清单30行
（28内部证据文件+2条跨目录绑定：../06-independent-verification/{evidence-sha256.tsv,
main_agent_correction_verification.py}），06目录独立清单1项，双清单反算0缺失0不匹配；
独立复核脚本新增跨清单绑定自检并重跑PASS；
phaseA0Decision=PHASE_A0_PREREGISTRATION_PASSED（evidenceRepackComplete=true）。

## 33. SOURCE-ATTRIBUTION Observer实现与20k工程门完成（2026-09-01，追加）

1. V4观察器Jar `jmetal-algorithm-5.8-V35-SOURCE-ATTRIBUTION-OBSERVER-V4.jar` 已实现
   （43类Java 8 major=52；影ZhangBoMOHPSOQ 16处纯观察patch + 影QpController PA钩子 +
   有界流式观察器 + V35ObserverGateRunner含内存采样器/完整性门/原子输出）。
   classpath V4:FORMAL，冻结正式Jar `8dad8f40…` 逐字节不动。
2. 20k OFF/ON工程门（训练机 `zhangbo-v35-source-attribution-observer-gate-20260901`，
   100_5_3_1/20260901/A4/C0，串行）：
   行为等价 14/14行为产物逐字节一致（掩码=任务书§十四允许字段）；
   完整性 ledgerRows==actualFE==15258、UNSET=0、observerErrors=0、droppedEvents=0、
   checkpointRows=3、boundedCapacityViolations=0。
3. 内存门（分解模型，baseline不乘25）：baseline=1,104,635,312 B；ON峰值≤OFF峰值
   （observerMeasuredDelta=0）；estimated500kPeak=1,400,341,936 B；
   ratio=0.326 < 0.60 → memoryGatePassed=true。
4. 冻结：`observerSchemaFrozen=true`、`observerJarFrozen=true`
   （05-observer-freeze/OBSERVER_FREEZE.md）。
5. 状态：`sourceAttribution500kEligible=true`、`sourceAttribution500kStarted=false`、
   `SA_HARD_500K_STARTED=false`——500k须用户批准后按预登记§10顺序启动。
6. 构建事件：V3→V4转换脚本heredoc转义导致runner一度为0字节，最终以一次性生成脚本修复；
   2k/20k两轮暴露5处接线问题（countDataRows header、B_0内联捕获、writeMemorySummary
   guard位置与签名、disarm在gate读取前清空）全部修复并经OFF/ON确认。

## 34. Observer内存流式修正（2026-09-01，追加）

1. 独立验收退回：初版Observer的`flushedEventLedger`将全部flushed事件留在内存
   （StringBuilder持续追加），观察器内存并非有界，初版estimated500kPeak=1.40GB
   漏掉了完整ledger常驻；`memoryGatePassed=true`/`observerJarFrozen=true`暂不成立。
2. 修正（真流式）：flush到磁盘临时文件（createTempFile+BufferedWriter），
   内存仅持有有界未flush缓冲（≤25000行×1024B=25MB）；closeLedgerWriters先flush
   残留再关闭；Runner从磁盘复制ledger文件并从磁盘计数行数；PDDR账本同构流式。
3. 行为等价声明更正：**12文件逐字节一致 + 2文件掩码等价（configuration.txt/status.properties
   含观察器溯源字段）+ 1测量only（memory-summary）**，非初版"14/14逐字节"。
4. 重跑结果（V4 Jar SHA `78bf4d30…46565`）：本地2k OFF/ON PASS；远端20k OFF/ON PASS
   （14项比对通过）；内存门 estimated500kPeak=1,241,503,200 B、ratio=**0.2891**<0.60 → PASS。
5. 状态恢复：`memoryGatePassed=true`、`observerJarFrozen=true`、`observerSchemaFrozen=true`、
   `sourceAttribution500kEligible=true`、`sourceAttribution500kStarted=false`。
   冻结文档含完整Jar SHA。

## 35. GitHub 镜像同步纪律（2026-09-01，追加）

1. **唯一远端**：`origin = https://github.com/zb581899564-arch/zhangbogaijin.git`，分支 `main`。
   GitHub 镜像是本项目在"本地工作副本 + G盘冷归档"之外的第三份持久副本，用于灾难恢复与跨机恢复。
2. **强制同步规则**：每次更改（代码、配置、测试、文档、证据、清单、勘误、路线图决策，无论大小）
   完成后，**必须在同一工作会话内 `git commit` 并 `git push origin main`**。推送未确认成功
   （exit 0 且远端 ref 前移）前不得宣称"已同步/已保存"；只留本地 commit 而未推送视为同步未完成。
3. **提交纪律**：
   - 一次逻辑变更一个 commit，消息写明变更内容与原因；证据勘误、清单重生成、路线图裁决各自独立提交；
   - 提交前必须检查 `git status`，不得遗漏证据文件；严禁提交任何凭据、密钥、token 或
     `.git-credentials`（认证只走 credential helper，脚本不得硬编码密码）；
   - 大批积压变更允许合并为一次"全量同步"提交，但消息必须概括覆盖的工作包范围。
4. **纳入/排除边界（2026-09-01 起生效）**：
   - `docs/evidence/**` 下的冻结 Jar、class 与活动工作包小体积日志**纳入镜像**
     （见 `.gitignore` 证据例外段），保证各包 `evidence-sha256.tsv` 清单在仓库内可完整反向验证；
     相同 Jar 副本在 git 对象库自动去重为单一 blob；
   - `*.tar.gz`、`*.zip`、`*.7z` 等归档包、历史 campaign 大体积 `*.log`、`build/`、`target/`、
     `tmp/`、`.codex-temp*` 维持排除，按 §19 "两份副本"纪律保存在本地与 G 盘冷归档；
   - `.gitignore` 的证据纳入/排除边界变更本身必须单独 commit 并说明理由。
5. **GitHub 硬限制预案**：单文件必须 `<100 MB`（当前镜像最大文件 79.5 MB CSV / 46.3 MB Jar）。
   未来 500k 级遥测若产生 ≥100 MB 单文件：禁止强行入库，先压缩拆分或改为"归档+清单哈希登记"；
   确需 Git LFS 时必须先征得用户同意。
6. **推送失败处理**：网络/认证失败时保留本地 commit 并向用户如实报告，不得静默放弃；恢复后立即
   补推。远端出现非 fast-forward（他处有新提交）时先 `git pull --rebase` 复核无冲突再推。
7. **镜像基线**：2026-09-01 已完成全量现状同步（`c108f287` 文本/证据层 + `0449f765` 冻结
   Jar/class 层），此后每次更改按本节规则增量维护。

## 36. Source Observer V4退回与V5工程冻结（2026-09-01，追加）

1. V4 SA-HARD运行的冻结轨迹、500k预算、终态前沿和失败类复现有效；但V4缺少Phase A0合同要求的
   nominalFE/轮次上下文、真实生命周期账本、严格B0导出和真实Qp action，且parent向量查询键错误。
   因此V4不得用于来源根因结论，任何既有“CFVF占62%即根因”等表述均无效。
2. V5为独立诊断Jar，正式算法Jar仍为`8DAD8F40...BAD8B9`且未重建。V5修正：
   `actualFE+nominalFE+generation+outerCycle+qRound`、十类流式生命周期事件、严格B0、
   parentLineageId查询以及Qp真实动作事件。Observer事件时间戳不等于新增FE。
3. V5身份：schema=`v35-source-attribution-observer-schema-v2`，Jar SHA-256=
   `1A73E3CF025F7CFDB47BDE38A7B34E8F8B0810958F61323A5D3CBC35272C8C9E`，44类均Java major52。
4. 工程门：本地2k OFF/ON通过；训练机100_5_3_1/20260901/C0/20k OFF/ON均
   actualFE=decoderCalls=15258，14项行为文件逐字节一致，ledger=15258，lifecycle=72686，
   十类事件齐全，B0独立复算一致，内存预测ratio=0.3221<0.60。
5. 状态：`v5ObserverJarFrozen=true`、`sourceAttribution500kEligible=true`；但
   `correctedSaHard500kStarted=false`、`saNormalStarted=false`、`sourceAttributionRootCauseEstablished=false`。
   下一步必须单独批准V5 SA-HARD 500k；未验收该运行前不得启动SA-NORMAL或做根因裁决。

## 37. V5 SA-HARD 500k完成与来源归因边界（2026-09-01，追加）

1. 已按D-113资格执行**唯一一条**V5 SA-HARD 500k：`100_5_3_1/20260901/A4/C0_BETA_MAX_065/500k/observer ON`，
   训练机 `/home/inspur/aicomp/zhangbo-v35-source-attribution-v5-sa-hard-500k-20260901`（全新目录、单JVM、nice 10、
   `-Xms1g -Xmx4g`、classpath=V5观察器在前+正式Jar在后）。`exit=0`、`status=COMPLETED`、`actualFE=decoderCalls=500000`、
   `remainingFE=0`、`utilizationRate=1.0`、`EXACT_MAX_FE`；机制指纹（62外循环/12400 Qg/271800 Qp/310000 CFVF子代、
   `pddrEventStreamHash=d698245e…`、`qgTableHash=F0E6D62B…`、`qpTableHash=9328966A…`）与冻结F1逐项一致。
2. 运行验收61/61通过（预算、完整性、ledger=actualFE、UNSET=0、V5五列+十类生命周期事件、B0独立重算11/11逐点一致、
   19+1检查点overshoot=0、正式Jar前后SHA不变、运行自身清单67项0缺失0不匹配）。证据：
   `docs/evidence/V35-SOURCE-ATTRIBUTION-500K/09-v5-sa-hard-500k/`（110项包级清单闭合）。
3. 失败类复现门**PASSED**：终态前沿 `f3755d83…1239bdd` 与历史A4逐字节且规范排序一致；gold自检1e-12通过后
   HV=`0.5545772540415207`、IGD=`0.15898065502479636`，`deltaHV=-0.3155<-0.05`、`deltaIGD=-1.7503<-0.20`。
   reference、归一化边界与指标实现均沿用Phase A0冻结合同，未重建。
4. 本包**只能**产出HARD单侧窗口证据（20窗全部计算）。hard–normal deficit、`t_div`、G1/G3裁决均为
   `NOT_COMPUTABLE/UNDECIDED`，必须等待SA-NORMAL（`100_2_3_1/20260901/A4/500k/V5 ON`）。禁止用单侧轨迹自配对
   构造伪门，禁止宣布G1或G3成立。
5. 描述性预算占比（GLOBAL_CFVF 62.00%、INHERITED_LS 34.94%、CATA 3.04%、PARENT_CARRYOVER 0.02%）**不是**根因结论；
   任何"CFVF占62%即根因"的表述继续无效。窗口份额（`WHVGShare`/`ExclusiveNDShare`）是窗口内相对划分，
   其变化只描述来源间结构变化。
6. 登记偏差（不推翻验收）：(a) 堆峰值3.5666GB为20k分解模型预测（比值0.3221≈1.29GB）的约2.8倍，与V4同类（2.92倍），
   堆未扩、无OOM、缓存有界，登记为模型外推精度问题，**不授权**任何扩堆掩盖；
   (b) 生命周期利用层中 `PERSONAL_ARCHIVE`、`QP_TEACHER`、`QP_ACTION` 的主体指纹不在评价账本中
   （1,323,122行）无法按来源归属，登记为 `NOT_ATTRIBUTABLE_BY_FINGERPRINT_JOIN`，未猜测填补。
7. 证据治理：`source-ledger.csv`(165MB) 与 `source-lifecycle-events.csv`(448MB) 超过GitHub单文件100MB限制，
   按§35.5改为G盘冷归档+包级清单SHA登记（本地+训练机+冷归档三份），`.gitignore` 已单独登记排除。
8. 停止边界保持：`SA_NORMAL_STARTED=false`、`SA_A2_CONDITIONAL_STARTED=false`、
   `SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false`、`DOE/QP_V2/CONFIG_RACE/VALIDATION/FORMAL_AUTHORIZED=false`、
   `formalMatrixRunning=false`、`PDDR/CFVF/DualQ/CaTa/正式Jar全部零改动`。SA-NORMAL须用户另行批准。

## 38. SA-NORMAL V5 500k完成与Phase A G4收口（2026-09-02，追加）

1. 已执行唯一一条 SA-NORMAL V5 500k：`100_2_3_1/20260901/A4/C0_BETA_MAX_065/500k/observer V5 ON`。
   训练机全新目录 `zhangbo-v35-source-attribution-v5-sa-normal-500k-20260902`，单 JVM、nice 10、`-Xms1g -Xmx4g`、
   V5在前+正式Jar在后。`exit=0`、`COMPLETED`、`actualFE=500000`、`remainingFE=0`、`utilizationRate=1.0`、
   `EXACT_MAX_FE`。验收56/56通过。
2. **初始种群快照**：`100_2_3_1×20260901` 此前无任何已执行记录（正式 manifest 只覆盖 seeds 20260808..20260827），
   训练机亦无此快照。使用项目规范零-FE物化器 `V35RepairSnapshotMaterializer` 确定性物化
   （`ea19f691…3a1842`）。**生成器同源性证明**：同一生成器对 `100_5_3_1/20260901` 的再物化与历史 HARD 快照
   逐字节一致（`84d84523…`）→ NORMAL 与 HARD 快照出自同一条确定性生成规则。快照溯源29/29门通过。
3. attempt1 因漏传 `bindings/100_2_3_1.binding.properties` 秒退（exit=1，评价前，0 FE），日志归档 `logs-attempt1/`，
   补齐后 attempt2 成功。如实保留。
4. **HARD–NORMAL 分析（冻结Phase A0合同，不重建reference/阈值）**：
   - `t_div=NOT_REACHED`：HARD 相对 NORMAL 在 decision-front HV/IGD 上无连续两 checkpoint 同时满足 lag 条件。
   - `G1_GLOBAL_CFVF=INSUFFICIENT`：GLOBAL_CFVF 的 WHVGShare deficit 在窗1–2 持续（fpw=1）、ExNDShare deficit
     在窗17–18 持续（fpw=17），但 t_div=NOT_REACHED → 时序前提（firstPersistentWindow ≤ t_div）不满足。
     survival 异常不竞争（mergeToPddr/pddrToWorking 逐来源差 <10pp）。
   - `G3_CATA=NOT_TRIGGERED`：CATA 无任何 metric 持续 deficit；FE 占比 2.23%(N)/3.04%(H) < 5% 实质性门槛。
   - **`SOURCE_ATTRIBUTION=G4_NO_ACTIONABLE_LEVER`**；`OLD_A4_DIAGNOSTIC_CLOSED=true`；`SOURCE_LEVER_CANDIDATE=NONE`。
5. 限制（如实登记）：(a) B0 退化基线（HV_0=0）使 i=1 的 hvProgress 数值不稳定，但 lag 在 i=1 因 IGD 分量未过门
   而 False，不影响 t_div；(b) PA/QP 利用层中 `PERSONAL_ARCHIVE/QP_TEACHER/QP_ACTION` 主体指纹不在评价账本
   （NORMAL 1,321,618行 / HARD 1,323,122行）无法按来源归属，标记 `NOT_ATTRIBUTABLE_BY_FINGERPRINT_JOIN`。
6. 证据治理：10包121项清单0缺0 mismatch；两文件≥100MB G盘冷归档+SHA登记；`.gitignore`单独排除。
7. **Phase A 结束（G4出口）**。`SA_A2_CONDITIONAL_ELIGIBLE=false`、`SA_A2_CONDITIONAL_STARTED=false`。
   Phase B、Qp-v2、DOE、Configuration Race、Validation、Final Freeze、正式矩阵均须新的明确授权。
   永久停止追 PDDR/pacing/teacher exposure/source扩大诊断（G4 条款）。

## 39. V35-QP-V2-SINGLE-AXIS Phase B1：语义欠定义裁决（2026-09-02，追加）

1. 用户授权的 Phase B1 工作包（Qp-v2 单轴 K 语义冻结、隔离实现与 20k 工程门）已执行到**第一硬门即停止**。
   语义来源核查（9类来源全查：Phase A计划全文、AGENTS/ROADMAP/CURRENT_SCIENTIFIC_STATE、三人共识证据目录、
   Qp 源码 `ZhangBoQpCandidateSelector.java`、冻结Jar源码树审计
   `V35-PFC5-TEACHER-EXPOSURE-CAL-PREREG/01-source-semantics/TEACHER_SELECTION_CALL_CHAIN.md`、git全历史）
   确认：获批材料对 K 的全部定义只有 Phase A 计划 §8 一句话——轴取值 `K=1,2,3,4`、
   K=1 精确等价 current A4 的**要求**、"不得同时调 teacher lambda/PA size/tauQ/epsilon"禁令与
   证明协议；G2 出口虽命名 `QP_CANDIDATE_SET_POLICY_V2`，但该出口从未触发，无任何展开。
2. 七项必需语义定义中五项完全缺失（K计数对象、作用候选集合、K>1选择规则、稳定破平、
   候选不足fallback、RNG消费契约），K=1→A4 仅有要求无还原机制。当前 A4 的 Qp 是
   "动作→唯一候选"确定性映射（每动作 argmin + fingerprint 破平），不存在现成的多元素候选集；
   任何 Top-K 实现都是自行发明算法，违反第一硬门。
3. 裁决：`QP_V2_SEMANTICS_UNDERDEFINED=true`、`QP_V2_IMPLEMENTED=false`、
   `QP_V2_EXPERIMENT_STARTED=false`、`PHASE_B1_ENGINEERING_GATE=BLOCKED`
   （阻塞条件=K语义欠定义）、`QP_V2_250K_ELIGIBLE=false`。未实现任何 Profile/Runner/实验Jar，
   未运行任何实验（`newFEConsumed=0`），未启动 250k/DOE/Validation/正式矩阵。
   正式 Jar `8DAD8F40…BAD8B9` 前后实测一致；PDDR/CFVF/双Q动作奖励/CA-TA 零改动。
4. 远端同步：`origin/main` 已推进至 `051877aa`（=本地 main），上次登记的推送阻塞已解除。
   工作区遗留的未跟踪文件（`07-sa-hard-500k/` 下 8 项）只读登记不处置。
5. 重启条件：用户须先以新的明确授权冻结 K 语义预注册（补齐 8 项缺失定义：计数对象、作用集合、
   K>1 选择规则、破平规则、候选不足 fallback、RNG 契约、K=1 还原机制证明、动作/奖励/容量不变式），
   方可进入实现与等价门。证据：`docs/evidence/V35-QP-V2-SINGLE-AXIS/`（清单 4+1 项反向复算闭合）。
