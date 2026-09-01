# 方法草稿：v3.5 Final（仅限已冻结事实）

> 适用状态：`methods_draft_non_result`。本草稿可作为论文方法章节的起点，但不替代来源论文引用审计、源码审计或正式实验结果。

## 1. 研究对象与共同问题接口

本文采用四向量表示 `JS/FA/MA/WA` 描述候选调度，并在三目标槽位 `[0,1,6]` 上报告 `Cmax`、`TEC` 和 `TWC`。Final 比较的共同问题语义为：`decoderMode=FM3`、`familyMode=DEGENERATE_SINGLE_FAMILY`、`setupMode=SEQUENCE_INDEPENDENT`、`ShiftMode=NONE`、种群规模 `100` 和 `MaxFEs=500000`。

在该语义下，设置时间采用实例级 `SUT[job][stage]`；产品族为单一、零转移的退化占位。因此，本文不将当前设置称为 PF-SDST 或真实产品族切换实验。疲劳参数用于构造标准化、具有个体差异的计算场景，不被解释为真实工人的精确生理参数。

## 2. FM3 动态疲劳解码边界

FM3 是 Final 主线和公平比较的共同解码器。方法描述只保留已冻结的语义：在序列无关设置时间条件下处理动态疲劳累积、自然恢复以及 setup/processing 两阶段一致的工时反馈。调度轨迹应可记录工厂、阶段、工件、机器、工人、设置时间、加工时间、开始时间和结束时间，以支持对 `Cmax`、`TEC`、`TWC` 与疲劳诊断量的追溯。

本节不以任何实验数值推出 FM3 的性能收益。FM0–FM3 的机制比较须在批准后独立搜索，并将最终解统一交由 FM3 oracle 复评；在该实验完成前，不把不同解码语义下的 HV 直接解释为算法优劣。

## 3. 公平基线 A0

A0 定义为**规范、确定性、公平适配 HMOPSO-QGS 基线**。它使用稳定破平、可注入随机源、正确的四向量身份映射、实例 SUT 和显式第一阶段 MA/WA，并保留公平基线所需的原 Q-gbest、评价后严格 PDDR-FF、工厂间搜索、O1–O9 与三主目标。

为隔离搜索机制因素，A0 与候选主算法共享：实例与实例哈希、SUT、疲劳参数 manifest、FM3、单一产品族、序列无关设置时间、`ShiftMode=NONE`、同 seed 初始四向量种群、FE 预算、三目标适配器、被动评价档案和参考前沿构造方法。

**A0 不是李明哲原始算法的直接可执行复现。** 作者当前 Java 的实际行为只能经 `A0_AUTHOR_DIAGNOSTIC` 进入独立诊断，不参加正式前沿、统计或结论。故本文会将“规范公平基线”“作者实际诊断线”“论文验证基线”分开命名，绝不混用。

## 4. 候选 A4-Pacing 的冻结方法范围

当前 Final 语义把 A4-Pacing 作为候选，而非已确认的最终主算法。其可描述的既有机制范围为：

- FM3 动态疲劳解码；
- 在四个方向子群上工作的认知—社会全向量双 Q 搜索，其中包括原 Qg、Qp、DSCR 与 CFVF；
- CA-TA-Lite 的 N1–N5 宏邻域和 Test/Apply/Re-test 结构；
- `GLOBAL_ORIGINAL` PDDR 与 `CA-TA-Lite -> inherited LS` 调用顺序；
- A4 pacing、双 Q `P=5/G=5`、`rho=0`、方向教师池关闭，以及 `FINAL_SEARCH_MIXTURE=[20,40,20,20]`。

在冻结配置中，四个物理搜索子群容量按 `[G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC]=[20,40,20,20]` 使用。该数值来自 DOE-1 的已关闭确认阶段；它不是一个由论文结果反推的“最优容量”声明。

## 5. PDDR、局部搜索与评价预算的报告纪律

Final 主线使用 `GLOBAL_ORIGINAL` PDDR，并保持 `CA-TA-Lite -> inherited LS`。每次完整运行必须能审计：

```text
initial FE + global offspring FE + inherited local FE + CA-TA-Lite FE
= fullEvaluations <= MaxFEs
```

父代比较、PDDR、档案、DSCR、预测和内部比较不计完整评价；每个新候选最多完整评价一次。局部搜索候选须保留来源、父槽位、谱系和预评价标记。本文只会在 raw run 的真实 FE、异常、repair 和来源字段齐备时报告运行完整性。

## 6. 明确排除的历史或拒绝路径

下列内容不是 Final 方法、创新或结果来源：

- `REGION_AWARE`：Final Candidate 隔离分支已拒绝；
- `ORDER_SWAP`：隔离的局部搜索顺序分支已拒绝；
- `BP_RESERVED_LEGACY`：只读历史兼容路径；
- active Shift（包括 FCLS/FCRS）及 PF-SDST：当前 Final 语义分别永久关闭或仅保留接口占位；
- `rho>0` 与方向教师池：当前冻结配置中均关闭。

若在相关工作中引用这些路径，必须同时标记其“历史/隔离/拒绝”状态，且不得并入 Final `PFref`、指标、统计、图表或结论。

## 7. 可复算性与方法证据

每个正式物理运行预期以 `RunKey=algorithm+config+instance+seed+budget` 唯一标识，并保存 canonical configuration、实例/扩展/疲劳参数/源码哈希、seed 与初群哈希、真实 FE 与停止原因、最终 front、机制计数、计时摘要、控制台日志和证据 SHA-256 清单。方法图只表达冻结接口与 FE 边界；任何由运行数据驱动的图表都须遵守 [结果占位契约](RESULTS_PLACEHOLDER_CONTRACT.md)。

## 方法段落仍需补足的来源项

- `[TODO_SOURCE_CONFIRMATION]` 论文中的正式数学符号、定理性文字和具体页码引用；
- `[TODO_SOURCE_CONFIRMATION]` 可投稿版本所需的文献综述与相关工作比较；
- `[PENDING_FORMAL_EVIDENCE]` A4 是否通过主版本门并可正式命名为 `V35_MAIN`；
- `[PENDING_FORMAL_EVIDENCE]` 任何效率、质量、显著性或消融结论。

## 来源

- [V35 Final 实验 DAG](../FINAL_EXPERIMENT_DAG.md)
- [v3.5 论文正式实验子路线图](../V35_FORMAL_EXPERIMENT_ROADMAP.md)
- [DOE-1 参数冻结](../evidence/V35-DOE1-subgroup-mixture/07-parameter-freeze/FINAL_PARAMETER_FREEZE.md)
