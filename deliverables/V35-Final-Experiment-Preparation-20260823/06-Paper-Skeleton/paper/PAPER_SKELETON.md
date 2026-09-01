# v3.5 Final 论文骨架（非结果稿）

> 文档状态：`methods_and_experiment_skeleton_only`。本文件只固化论文结构、已冻结语义和未来证据入口；不包含正式实验数值、优越性结论、统计结论或完整复现声明。
>
> 最后核对：2026-08-23。当前仍为 `formal_matrix_started=false`、`sampled_reproduction_accepted=false`、`full_reproduction_accepted=false`。

## 暂定题目

**面向动态疲劳分布式混合流水车间调度的冻结协议研究：规范 HMOPSO-QGS 基线与候选 A4 的可追溯比较**

题目中的“比较”只描述拟执行的、受冻结协议约束的比较；在正式 raw fronts、统一经验 `PFref` 和统计验收齐备前，不改写为“优于”“有效”或同义结论。

## 摘要（待证据填充）

**研究问题。** `[待填：经来源与术语审计后的问题定义]`

**方法。** 本研究在固定的 FM3、单一产品族、序列无关设置时间和无 Shift 语义下，定义一条规范、确定性、公平适配的 HMOPSO-QGS 基线（A0）以及受主版本门约束的候选 A4-Pacing 配置。`[待填：仅可由冻结配置、源码审计和方法图支持的机制说明]`

**实验。** `[待填：仅在已批准的 raw run、实例/seed/初群哈希、FE 审计、经验 PFref 与冻结指标脚本齐备后填写]`

**结果。** `[禁止预填。仅可从 RESULTS_PLACEHOLDER_CONTRACT.md 所列冻结母表导入。]`

**结论。** `[禁止预填。须与统计结论、已知限制和复算证据逐项对应。]`

关键词：动态疲劳调度；distributed hybrid flow shop；HMOPSO-QGS；多目标优化；可追溯实验协议

## 1. 引言

### 1.1 研究背景与问题边界

- `[待填：来源论文和问题定义的可引用背景]`
- 本文当前仅讨论：FM3、`DEGENERATE_SINGLE_FAMILY`、`SEQUENCE_INDEPENDENT`、`ShiftMode=NONE` 与三目标 `[Cmax, TEC, TWC]` 的冻结语义。
- 不把该设置称为真实产品族序列相关设置时间（PF-SDST）实验，也不将疲劳参数表述为真实工人的精确生理测量。

### 1.2 研究目标与可检验问题

- RQ1（协议）：在相同实例、SUT、疲劳参数、初始四向量种群、seed、FE 预算与指标口径下，A0 与冻结后的主版本候选是否可被公平比较？
- RQ2（机制）：FM0–FM3 的差异是否能在统一 FM3 oracle 复评下被独立说明？
- RQ3（搜索）：A0–A4 的合法依赖链是否在人工批准后形成可审计的消融证据？

上述问题是实验设计占位，不含任何预设答案。

### 1.3 本文边界与非主张

- A4-Pacing 目前是 Final 主线中的候选配置；它尚不是已由主版本门确认的最终主算法。
- 不主张 A0 或 A4 在正式 A0/A4 比较中优越，不主张统计显著，也不主张完成抽样复现或完整复现。
- `REGION_AWARE`、`BP_RESERVED_LEGACY`、`ORDER_SWAP`、任意 active Shift、PF-SDST 与 `rho>0` 不属于 Final 主线结果或创新；如因溯源需要提及，只能标为隔离/拒绝的历史路径。

## 2. 问题、语义与方法

### 2.1 冻结问题语义

说明四向量 `JS/FA/MA/WA`、三目标 `[Cmax, TEC, TWC]`、实例级 `SUT[job][stage]` 与 FM3 统一解码边界。需要公式、数据字段或来源页码时保留 `[TODO_SOURCE_CONFIRMATION]`，不得猜测补全。

### 2.2 FM3 动态疲劳解码

说明已冻结的“序列无关设置时间下的动态疲劳、自然恢复与 setup/processing 两阶段一致解码”语义，以及它在公平比较中由所有算法共享的原因。这里不以任何数值效果证明 FM3 的收益。

### 2.3 A0：规范、确定性、公平适配 HMOPSO-QGS 基线

A0 定义为**规范、确定性、公平适配 HMOPSO-QGS 基线**：它与候选主算法共享 FM3、问题实例、单族/序列无关 SUT、`ShiftMode=NONE`、初始种群、seed、FE 预算、三目标和参考前沿构造口径。A0 **不是李明哲原始算法的直接可执行复现**；作者实际行为仅可作为隔离的 `A0_AUTHOR_DIAGNOSTIC` 诊断线，不能进入正式前沿、统计或论文结论。

### 2.4 候选 A4-Pacing 的方法位置

在 Final 冻结配置中，候选 A4-Pacing 使用现有的动态疲劳解码、认知—社会全向量双 Q 搜索和 CA-TA-Lite 机制链。冻结的控制条件包括：`GLOBAL_ORIGINAL` PDDR、`CA-TA-Lite -> inherited LS`、`P=5/G=5`、`rho=0`、方向教师池关闭与 `FINAL_SEARCH_MIXTURE=[20,40,20,20]`。本节只可报告机制接口和冻结状态；主版本门、消融和正式比较未完成前不得将其写为已证实的贡献。

### 2.5 隔离或拒绝路径

单列说明而不纳入方法结果：

- `ORDER_SWAP`：已作为历史单变量分支拒绝，不能作为 Final 方法。
- `REGION_AWARE`：已在 Final Candidate 的隔离分支中拒绝，不能作为正式创新、正式比较臂或结果来源。
- `BP_RESERVED_LEGACY`：只读历史兼容路径，不能进入 Final 主线。
- Shift 与 PF-SDST：当前分别为 `NONE` 和单族/序列无关占位；不产生正式实验结论。

## 3. 实验协议

本节由 [EXPERIMENT_SECTION_SKELETON.md](EXPERIMENT_SECTION_SKELETON.md) 展开。它先给出审计条件，再给出任何比较或统计文字。

### 3.1 共同输入与运行隔离

`RunKey=algorithm+config+instance+seed+budget`，同一 `(instance, seed)` 的对照共享显式初始种群；每个 arm 使用独立 JVM、Problem、算法对象与输出目录。

### 3.2 计划中的实验包

仅在相应前置门和人工授权满足后，按 EXP-1 至 EXP-10 的顺序写入。当前不得用计划矩阵替代已完成矩阵。

### 3.3 指标、经验参考前沿与统计

每个实例的 `PFref` 只能在冻结参与算法集和全部批准运行完成后构造；HV、IGD、SP、双向 C-metric、前沿规模、极值、疲劳诊断与效率指标都必须可由冻结的 raw fronts、边界和脚本重算。

## 4. 结果（严格占位）

本章的唯一填数入口是 [RESULTS_PLACEHOLDER_CONTRACT.md](RESULTS_PLACEHOLDER_CONTRACT.md)。当前必须显式写为：**无正式结果可报告。**

### 4.1 运行完整性与审计

`[待填：RunKey 完整性、配置/实例/初群哈希、真实 FE、异常与停止原因]`

### 4.2 两算法正式比较

`[待填：仅当 EXP-3 raw fronts、每实例经验 PFref、指标母表和配对统计均已冻结]`

### 4.3 疲劳机制验证

`[待填：仅当 EXP-4 的 FM0–FM3 搜索和 FM3 oracle 复评均完成]`

### 4.4 合法机制消融

`[待填：仅在人工批准后，且 A0→A1→A2→A3→A4 依赖链的 raw runs 齐备]`

### 4.5 稳健性、效率与限制

`[待填：仅从 EXP-6、运行计时母表和最终审计导入；不得以未过时间门的历史计时占位]`

## 5. 讨论

讨论须区分：工程诊断信号、经过配对统计的正式发现、无法确认的原因和外推限制。不得把单实例、单 seed、历史 Shift-on、旧压力语义、隔离分支或不完整 reference 当作论文级支持。

## 6. 结论

结论只能回收已经在第 4 节由冻结证据支持的发现，并同时报告：正式矩阵状态、可复算路径、适用边界和未完成事项。

## 附录与补充材料

- A. 冻结配置、源码/实例/扩展哈希与 seed 清单；
- B. 每个 RunKey 的原始运行索引和失败运行保留记录；
- C. 每实例 `PFref`、归一化边界、HV 参考点和指标脚本哈希；
- D. 45 实例逐行结果、配对统计和图表生成清单；
- E. 历史/拒绝路径的隔离说明（不与 Final 结果合表）。

## 表图占位总账

所有未来表图必须先登记在此表（或在实验章节中增加同字段条目）。`[METRIC_SCRIPT_PATH_AND_SHA256]` 是 Track D 冻结后才能替换的字面占位符，不能用临时脚本或手工数值代替。

| ID | 论文对象 | 允许的输入 raw run 目录 | `PFref` / 边界输入 | 指标或制图脚本 | 当前状态 |
|---|---|---|---|---|---|
| Tab-1 | Final 语义与共同输入 | 不适用；使用冻结配置与 manifest | 不适用 | 不适用；配置摘录须附哈希 | 可写事实，不填效果数值 |
| Tab-2 | 运行完整性与 FE 审计 | `docs/evidence/V35-FORMAL-EXPERIMENTS/{package}/raw-runs/{RunKey}/` | 不适用 | `[RUN_AUDIT_SCRIPT_PATH_AND_SHA256]` | 等 raw runs |
| Tab-3 | A0 vs 主版本质量指标 | `.../03_main_45x20/raw-runs/{RunKey}/` | `.../08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_SCRIPT_PATH_AND_SHA256]` | 禁止填数 |
| Tab-4 | FM0–FM3 机制验证 | `.../04_fatigue_validation/raw-runs/{RunKey}/` | `.../08_reference_and_statistics/pfref/{instanceId}/` 与 FM3 oracle 记录 | `[METRIC_SCRIPT_PATH_AND_SHA256]` | 禁止填数 |
| Tab-5 | A0–A4 合法消融 | `.../05_ablation/raw-runs/{RunKey}/` | `.../08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_SCRIPT_PATH_AND_SHA256]` | 等批准与 raw runs |
| Tab-6 | 参数稳健性 | `.../06_parameter_sensitivity/raw-runs/{RunKey}/` | `.../08_reference_and_statistics/pfref/{instanceId}/` | `[SENSITIVITY_SCRIPT_PATH_AND_SHA256]` | 禁止填数 |
| Fig-1 | Final 方法与 FE 边界 | 不适用；使用冻结配置、源码审计与事件定义 | 不适用 | `[METHOD_FIGURE_SCRIPT_OR_SOURCE_PATH_AND_SHA256]` | 可制图，不表达效果 |
| Fig-2 | 运行—PFref—指标证据链 | 不适用；使用本表的目录契约 | 不适用 | `[EVIDENCE_FLOW_FIGURE_SOURCE_PATH_AND_SHA256]` | 可制图，不表达效果 |
| Fig-3 | HV/IGD 收敛曲线 | `.../02_five_scale_pilot/` 或 `.../03_main_45x20/raw-runs/{RunKey}/` 的检查点 | `.../08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_AND_PLOT_SCRIPT_PATH_AND_SHA256]` | 禁止填数 |
| Fig-4 | 20 次 HV/IGD 分布 | `.../03_main_45x20/raw-runs/{RunKey}/` | `.../08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_AND_PLOT_SCRIPT_PATH_AND_SHA256]` | 禁止填数 |
| Fig-5 | 五规模 50% 经验达到面及投影 | `.../03_main_45x20/raw-runs/{RunKey}/` | `.../08_reference_and_statistics/pfref/{instanceId}/` | `[ATTAINMENT_PLOT_SCRIPT_PATH_AND_SHA256]` | 等 20 次 fronts |
| Fig-6 | 耗时分解 | 相应 raw run 的 timing 摘要 | 不适用 | `[TIMING_PLOT_SCRIPT_PATH_AND_SHA256]` | 禁止使用历史未验收计时 |
| Fig-7 | 目标与疲劳风险关系 | `.../03_main_45x20/` 与 `.../04_fatigue_validation/` raw runs | `.../08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_AND_PLOT_SCRIPT_PATH_AND_SHA256]` | 禁止填数 |

## 当前来源

- [V35 Final 实验 DAG](../FINAL_EXPERIMENT_DAG.md)
- [V35 Final 实验状态](../FINAL_EXPERIMENT_STATUS.md)
- [v3.5 论文正式实验子路线图](../V35_FORMAL_EXPERIMENT_ROADMAP.md)
- [DOE-1 参数冻结](../evidence/V35-DOE1-subgroup-mixture/07-parameter-freeze/FINAL_PARAMETER_FREEZE.md)
