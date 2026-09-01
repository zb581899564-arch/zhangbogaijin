# 实验章节骨架与数据入口

> 本文档定义“未来可以怎样写实验”，不是“已经得到什么实验结果”。当前所有数字单元格、显著性符号、排名、胜平负和图形曲线均为空。

## 1. 当前实验状态

截至 2026-08-23，DOE-1 已关闭并冻结搜索期子群容量；但 `formal_matrix_started=false`，正式 20-seed 清单、正式算法集合和正式 reference 均未冻结。因此本章节当前只能保留协议、目录模板、字段契约与待办项。

Final 共同语义：

```text
FINAL_SEARCH_MIXTURE=[G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC]=[20,40,20,20]
FM3; DEGENERATE_SINGLE_FAMILY; SEQUENCE_INDEPENDENT; ShiftMode=NONE
GLOBAL_ORIGINAL; CA-TA-Lite -> inherited LS; A4-Pacing; P=5/G=5
rho=0; directionalTeacherPool=false; population=100; MaxFEs=500000
```

## 2. 实验问题与比较边界

### 2.1 两算法公平比较（未来 EXP-3）

比较对象只能是 A0 与经主版本门冻结的 `V35_MAIN`。A0 的论文表述固定为“规范、确定性、公平适配 HMOPSO-QGS 基线”，并且不是李明哲原始算法的直接可执行复现。候选 A4 目前不得提前写为最终 `V35_MAIN` 或 `FULL`。

同一个 `(instance, seed)` 对内，两个 arm 必须共享实例、SUT、疲劳参数、单族/序列无关设置、FM3、无 Shift、显式初始四向量种群、FE 预算与指标口径；不同 arm 使用独立 JVM、Problem、算法对象和输出目录。

### 2.2 疲劳机制验证（未来 EXP-4）

FM0–FM3 改变解码语义，必须执行固定染色体逐级解码、各模式独立搜索，以及 FM3 oracle 统一复评。疲劳指标是机制与风险诊断项，不增加第四目标。

### 2.3 合法搜索消融（未来 EXP-5）

消融只能沿 `A0 -> A1(+DSCR) -> A2(+CFVF) -> A3(+Qp/双Q协同) -> A4(+CA-TA-Lite)` 递进。任何正式 500000 FE 消融均需人工批准；当前不可将既有诊断或历史分支转写为该实验结果。

## 3. 原始运行与母表契约

### 3.1 目录模板（未来产物，不声称已存在）

```text
docs/evidence/V35-FORMAL-EXPERIMENTS/
  {package}/raw-runs/{RunKey}/
  08_reference_and_statistics/pfref/{instanceId}/
  08_reference_and_statistics/normalization/{instanceId}/
  08_reference_and_statistics/metric-tables/
  09_figures/
```

其中 `{package}` 对应 `01_main_variant_gate`、`02_five_scale_pilot`、`03_main_45x20`、`04_fatigue_validation`、`05_ablation`、`06_parameter_sensitivity` 或经批准的其他协议包。每一条物理运行必须有不可重复的 `RunKey=algorithm+config+instance+seed+budget`。

### 3.2 每个 raw run 的最低字段

| 字段 | 作用 | 未满足时的处理 |
|---|---|---|
| canonical configuration 与 SHA-256 | 证明运行时语义已冻结 | 不进入母表 |
| 实例、扩展、疲劳参数、源码哈希 | 证明共同问题接口 | 不进入母表 |
| seed 与初始种群 SHA-256 | 证明同 seed 公平配对 | 停止该配对 |
| status、真实 FE、停止原因 | 证明预算与失败保留 | 不填 0 或覆盖失败目录 |
| final front | 构造经验 `PFref` 的唯一原始来源 | 不计算正式指标 |
| 机制计数、Cmax 生命周期、计时摘要 | 机制/效率诊断 | 仅能标记缺失，不得补造 |
| console 日志与证据 SHA-256 清单 | 可复算性和审计 | 不作论文结论 |

### 3.3 经验 `PFref` 与指标脚本

每个实例只在已批准的参与算法集合和全部计划运行完成后构造一次：

\[
PF_{ref}^{(i)}=ND\left(\bigcup_a\bigcup_{s=1}^{20}PF_{i,a,s}\right).
\]

同一实例的全部算法使用同一归一化边界；退化范围为 `1e-12`，HV 参考点为归一化 `(1.1,1.1,1.1)`。Track D 必须在统计前提供不可变的指标脚本实际路径与 SHA-256；在此之前，下表内的 `[METRIC_SCRIPT_PATH_AND_SHA256]` 只能保留为占位符。

## 4. 论文表格占位与可追溯要求

| 表 ID | 仅可回答的问题 | raw run 目录 | `PFref` / 边界 | 脚本 | 当前可写内容 |
|---|---|---|---|---|---|
| E-Tab-1 | 冻结语义、共同输入和公平约束是什么？ | 不适用；配置/manifest 哈希 | 不适用 | 不适用 | 已冻结事实 |
| E-Tab-2 | 每个 arm 是否完整、合法且 FE 闭合？ | `{package}/raw-runs/{RunKey}/` | 不适用 | `[RUN_AUDIT_SCRIPT_PATH_AND_SHA256]` | 字段表头，不能填结果 |
| E-Tab-3 | A0 与主版本的 HV/IGD/SP/C-metric 等为何？ | `03_main_45x20/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_SCRIPT_PATH_AND_SHA256]` | 禁止填数 |
| E-Tab-4 | FM0–FM3 在 FM3 oracle 下的机制/风险诊断为何？ | `04_fatigue_validation/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_SCRIPT_PATH_AND_SHA256]` | 禁止填数 |
| E-Tab-5 | A0–A4 递进消融是否形成稳定差异？ | `05_ablation/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_SCRIPT_PATH_AND_SHA256]` | 等批准和完整 raw runs |
| E-Tab-6 | 新机制参数的稳健性如何？ | `06_parameter_sensitivity/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` | `[SENSITIVITY_SCRIPT_PATH_AND_SHA256]` | 禁止填数 |

## 5. 论文图形占位与可追溯要求

| 图 ID | 目标 | raw run 目录 | `PFref` / 边界 | 脚本 | 当前状态 |
|---|---|---|---|---|---|
| E-Fig-1 | Final 方法、共同接口与 FE 边界 | 不适用；冻结配置与源码审计 | 不适用 | `[METHOD_FIGURE_SOURCE_PATH_AND_SHA256]` | 只能表达结构 |
| E-Fig-2 | RunKey 到 `PFref`、指标、统计的证据流 | 不适用；目录契约 | 不适用 | `[EVIDENCE_FLOW_SOURCE_PATH_AND_SHA256]` | 只能表达流程 |
| E-Fig-3 | HV/IGD 随 FE 的收敛检查点 | `02_five_scale_pilot/` 或 `03_main_45x20/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_AND_PLOT_SCRIPT_PATH_AND_SHA256]` | 禁止绘制数据 |
| E-Fig-4 | 20 次 HV/IGD 分布 | `03_main_45x20/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_AND_PLOT_SCRIPT_PATH_AND_SHA256]` | 禁止绘制数据 |
| E-Fig-5 | 五规模 50% 经验达到面及三个投影 | `03_main_45x20/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` | `[ATTAINMENT_PLOT_SCRIPT_PATH_AND_SHA256]` | 等全部 20 次 fronts |
| E-Fig-6 | Decoder/搜索控制/CA-TA-Lite 耗时分解 | raw run 的 timing 摘要 | 不适用 | `[TIMING_PLOT_SCRIPT_PATH_AND_SHA256]` | 不得使用历史未验收计时 |
| E-Fig-7 | 三目标与疲劳风险关系 | `03_main_45x20/` 与 `04_fatigue_validation/` raw runs | `08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_AND_PLOT_SCRIPT_PATH_AND_SHA256]` | 禁止绘制数据 |

对“非数据图”（E-Fig-1、E-Fig-2），raw run、`PFref` 和指标脚本明确不适用；它们必须分别以冻结配置/manifest 或目录契约为可核对来源。其他所有结果图表缺任一 raw run、`PFref`、归一化边界或脚本哈希时，均不得发布。

## 6. 统计文字模板（空证据时不得替换）

同 seed、同初始种群的两算法比较计划使用 Wilcoxon signed-rank；多算法比较计划使用 Friedman 检验和 Holm 校正，显著性水平为 `alpha=0.05`，并报告效应量与中位数/IQR、均值、标准差、胜/平/负。

可在证据冻结后使用的句式：

> 在 `[instance scope]` 上，基于 `[raw run manifest]`、`[PFref manifest]` 和 `[metric script SHA-256]` 重算后，`[algorithm X]` 相对 `[algorithm Y]` 的 `[metric]` 为 `[descriptive statistics]`；配对检验为 `[test output]`，效应量为 `[effect output]`。该结论的适用范围为 `[scope]`。

当前不得把方括号替换为数字、星号、排名或“显著优于”等措辞。

## 7. 当前缺失项与阻断条件

- EXP-1 所需的主版本冻结前置（包括 FC-8 Champion Gate）尚未完成；
- 正式 20 个 seed 清单、正式算法集合和 `V35_MAIN` 身份尚未冻结；
- EXP-2、EXP-3、EXP-4、EXP-5、EXP-6 等的完整 raw runs 尚未形成；
- 每实例经验 `PFref`、归一化边界、指标脚本路径/哈希和统计输出尚未形成；
- 正式运行计时母表、统一图表输入及最终审计尚未形成；
- 因而不得报告正式 A0/A4 优越、统计显著、完整复现或正式多算法比较结果。

## 来源

- [V35 Final 实验 DAG](../FINAL_EXPERIMENT_DAG.md)
- [V35 Final 实验状态](../FINAL_EXPERIMENT_STATUS.md)
- [v3.5 论文正式实验子路线图](../V35_FORMAL_EXPERIMENT_ROADMAP.md)
