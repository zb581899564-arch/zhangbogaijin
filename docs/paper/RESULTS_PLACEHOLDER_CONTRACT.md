# 结果占位契约：禁止无证据填数

> 适用对象：论文第 4 节、摘要中的结果句、结论中的效果句、所有数据表、统计表与结果图。本契约优先于任何“先写一个看起来完整的结果章节”的需求。

## 1. 当前允许的唯一结果状态

当前可公开写入论文的状态句为：

> 正式实验矩阵尚未启动；正式 20-seed 清单、参与算法集合、每实例经验 `PFref`、冻结指标脚本和正式统计尚未形成。因此，本文当前不报告正式数值结果、统计显著性、算法优越性或完整复现结论。

这是状态说明，不是负面实验结论。不得将 DOE-1 的参数冻结、历史单实例/少 seed 工程诊断、历史 Shift-on、旧压力语义或隔离/拒绝分支转写为 Final 正式结果。

## 2. 结果单元的最小证据包

每一个数值、排名、箱线图点、显著性符号或结论句必须能够反向定位到下列全部项目：

1. `RunKey` 与原始目录：`docs/evidence/V35-FORMAL-EXPERIMENTS/{package}/raw-runs/{RunKey}/`；
2. canonical configuration、实例/扩展/疲劳参数/源码哈希；
3. seed 与同 `(instance, seed)` 的共同初始种群哈希；
4. status、真实 FE、停止原因、完整性/机制硬门字段；
5. raw final front；
6. 对应实例的 `PFref`、归一化边界和 HV 参考点文件；
7. 指标或统计脚本的实际路径、版本和 SHA-256；
8. 生成的冻结母表、输出文件哈希与图表脚本输入清单。

任何一项缺失，该单元只能保留为 `[PENDING_FORMAL_EVIDENCE]`，不能以估计、截图、手算修改、旧 output 或另一个算法的 reference 填补。

## 3. 表图写入门

| 对象 | raw run 必需目录 | `PFref` 必需目录 | 脚本必需记录 | 允许填数的门 |
|---|---|---|---|---|
| 主比较表/分布图 | `03_main_45x20/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_SCRIPT_PATH_AND_SHA256]` | 所有批准 arm 和 runs 完成，PFref/指标母表冻结 |
| 疲劳机制表/图 | `04_fatigue_validation/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` 与 FM3 oracle 记录 | `[METRIC_SCRIPT_PATH_AND_SHA256]` | FM0–FM3、oracle 复评、母表均完成 |
| 消融表/图 | `05_ablation/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` | `[METRIC_SCRIPT_PATH_AND_SHA256]` | 用户批准、合法依赖链、全部 raw runs 完成 |
| 参数图/表 | `06_parameter_sensitivity/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` | `[SENSITIVITY_SCRIPT_PATH_AND_SHA256]` | 设计、raw runs、主效应输入冻结 |
| 达到面 | `03_main_45x20/raw-runs/{RunKey}/` | `08_reference_and_statistics/pfref/{instanceId}/` | `[ATTAINMENT_PLOT_SCRIPT_PATH_AND_SHA256]` | 对应实例 20 次最终 fronts 完整 |
| 计时图 | 对应 raw run 的 timing 摘要 | 不适用 | `[TIMING_PLOT_SCRIPT_PATH_AND_SHA256]` | 同机环境、输入和时间门证据完整 |
| 方法/证据流图 | 不适用；冻结配置或目录契约 | 不适用 | 方法图源码路径与 SHA-256 | 只能表达流程，不能含效果数值 |

上述目录是未来证据的约定入口，不声称当前已存在。`[...PATH_AND_SHA256]` 只能由 Track D 或制图工作包的冻结输出替换。

## 4. 结论性语言门槛

| 拟写表述 | 至少需要的证据 | 当前状态 |
|---|---|---|
| “A0/A4 在某指标上优于对方” | 完整 raw fronts、共同 PFref、冻结指标母表及明确范围 | 不可写 |
| “统计显著” | 配对设计、冻结统计脚本、检验输出、效应量和多重比较处理 | 不可写 |
| “该机制有效” | 对应合法消融或机制实验、预先定义比较与审计字段 | 不可写 |
| “运行时间可接受/更快” | 受控同机计时、时间门和计时母表 | 不可写 |
| “完成抽样复现/完整复现” | 对应验收标志、完整实验/统计/证据链 | 不可写 |
| “DOE-1 冻结了 `[20,40,20,20]`” | DOE-1 参数冻结记录 | 可写，限于配置冻结事实 |
| “Final 路径关闭 Shift、单族、序列无关 SUT 等” | Final DAG 与冻结记录 | 可写，限于语义事实 |

## 5. 明确禁止的证据混入

以下材料不能进入 Final 结果表、`PFref`、指标、统计或图：

- `REGION_AWARE`、`ORDER_SWAP`、`BP_RESERVED_LEGACY` 的隔离/拒绝分支；
- active Shift、PF-SDST、`rho>0` 或方向教师池开启的非 Final 设置；
- `A0_AUTHOR_DIAGNOSTIC` 与其他 `author_actual` 诊断输出；
- 历史 Shift-on、旧压力语义、单实例或未批准扩展运行；
- 任何不共享共同初始种群、实例/参数哈希、FM3、单族/序列无关 SUT、`ShiftMode=NONE` 和 FE 口径的运行；
- 单次前沿、单算法 reference、运行中动态 reference 或手工改写的图表数值。

## 6. 数据填充流程

```text
批准的 raw runs 完整保留
  -> 按 RunKey 审计 config/hash/FE/合法性/机制门
  -> 冻结参与算法集合
  -> 每实例构造一次 pooled empirical PFref 与归一化边界
  -> 用冻结指标脚本重算母表
  -> 用冻结统计脚本与配对信息生成统计表
  -> 图表脚本读取母表并记录输入/输出 SHA-256
  -> 将可追溯的句子、表和图写入论文
```

在任一箭头前停止时，后续的论文位置保持占位，不作推断性补写。

## 7. 当前缺失清单

- `V35_MAIN` 最终身份和 EXP-1 主版本冻结；
- FC-8 Champion Gate 与进入正式矩阵的授权；
- 正式 20-seed 清单及所有对照 arm 的共同初始种群清单；
- EXP-2 至 EXP-7 所需 raw run 目录与原始 final fronts；
- 每实例经验 `PFref`、归一化边界、参考点和 SHA-256；
- Track D 冻结的指标/统计脚本实际路径、版本与 SHA-256；
- 配对检验、多算法检验、Holm 校正、效应量和描述统计输出；
- 计时母表、图表输入母表、图表脚本及最终证据审计。

因此，所有结果位置必须继续使用 `[PENDING_FORMAL_EVIDENCE]` 或第 1 节的状态句。

## 来源

- [V35 Final 实验 DAG](../FINAL_EXPERIMENT_DAG.md)
- [v3.5 论文正式实验子路线图](../V35_FORMAL_EXPERIMENT_ROADMAP.md)
- [DOE-1 参数冻结](../evidence/V35-DOE1-subgroup-mixture/07-parameter-freeze/FINAL_PARAMETER_FREEZE.md)
