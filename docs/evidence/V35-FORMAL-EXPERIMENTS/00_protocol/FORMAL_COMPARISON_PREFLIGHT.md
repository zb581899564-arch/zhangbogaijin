# V35 vs HMOPSO-QGS-F 正式公平比较前置审计

审计日期：2026-08-23  
轨道：B（Formal V35 vs HMOPSO-QGS-F）  
结论：`BLOCKED_FAIL_CLOSED`；未启动任何 500k 正式运行。

## 已核实的当前边界

- 实验子路线图明确为 `EXP-1=blocked_by_FC-8`，而 `EXP-3` 的 45 实例 × 20 seed × 2 算法主比较还需 `FC-9` 启动门。
- `formal_20_seed_list_frozen=false`、`formal_algorithm_set_frozen=false`、`formal_reference_frozen=false`、`formal_matrix_started=false`。因此“20 seed”不是可直接代入的已冻结输入。
- `FINAL_EXPERIMENT_STATUS.md` 的 B 轨道记录为 `BLOCKED`、0 formal runs；`FINAL_EXPERIMENT_DAG.md` 也只允许先准备，不允许在冻结与矩阵审计缺失时启动原始前沿。
- FC-TIME 已通过不等于 FC-8 已通过。FC-7 与 FC-8 仍是未完成前置；当前不能把 A4 候选写成论文最终主算法。
- 当前可复用的底座是 `V35FairRunner`，但其注释明确它是工程公平桥接，而非正式矩阵启动器。新的门控 runner 只在所有外部冻结证据已存在时才会调用它。

## 矩阵是否已可唯一确定？

不能。

协议层的目标尺寸可以描述为 `45 instances × 20 seeds × 2 arms = 1800` 条独立物理运行，且每条为 `population=100`、`MaxFEs=500000`。但实际矩阵仍不能唯一生成，原因是：

| 必须冻结的输入 | 当前事实 | 结果 |
|---|---|---|
| FC-8 Champion Gate 与 EXP-1 主版本冻结 | 未闭合 | `V35_MAIN` 仍只是候选，不能指定正式机制哈希 |
| FC-9 与工作包/资源批准 | 未闭合 | 不允许开始 45×20 主比较 |
| 20 个正式 seed | `false` | 无法生成配对 RunKey |
| 正式算法集合/主版本名称 | `false` | 不能唯一绑定 MAIN 臂；A5 也不能被静默纳入 |
| 45 个实际文件名、实例/SUT/疲劳哈希 | 未形成冻结清单 | 文件漂移、缺失或重复无法被运行前拒绝 |
| 每 seed 的初始四向量种群双哈希 | 未物化 | 历史上 P8 与 V35FairRunner 存在不同哈希口径，必须同时锁定 |
| A0 与 MAIN 的运行时 canonical 配置哈希 | 未冻结 | 不能证明实际 Builder/Updater/外循环对应声明配置 |

所以 1800 只是已文档化的**目标协议规模**，不是可执行的正式矩阵，也不是任何结果或统计结论。

## 规范公平适配：A0 = HMOPSO-QGS-F

本轨道的 A0 名称、语义和限制固定如下：

```text
A0 / HMOPSO-QGS-F = 规范、确定性、无作者遗留缺陷的公平适配基线
```

它不是李明哲当前 Java 的 `author_actual` 原算法。它必须使用正式 `formal-hmopso-qgs-v1`/同等冻结运行时边界，保留原 Qg、严格 PDDR、基线更新、工厂间搜索和 O1–O9；但不得取得 DSCR、CFVF、Qp、谱系档案、双Q改进、CA-TA-Lite 或方向教师池等张博搜索增强。

两臂只能共享下列问题与实验边界，不能共享或重写彼此的搜索机制：

```text
instance + instance hash
SUT extension + fatigue-parameter manifest
FM3; DEGENERATE_SINGLE_FAMILY; SEQUENCE_INDEPENDENT; ShiftMode=NONE
objectives=[0,1,6]; population=100; MaxFEs=500000
seed + identical frozen initial JS/FA/MA/WA population
GLOBAL_ORIGINAL PDDR timing; CATA_THEN_INHERITED local-search order
BAL full-open diagnostic-only pressure mode; rho=0; directional teacher pool=false
passive evaluation archive and metric definition
```

`A0_AUTHOR_DIAGNOSTIC`、`AUTHOR_ACTUAL`、PF-SDST、任何 Shift、`REGION_AWARE`、`ORDER_SWAP`、`BP_RESERVED_LEGACY` 和旧压力结果全部是禁止输入。对比算法的来源适配白名单仅为 `Problem/Solution/initialPopulation/randomSource/FE/logging`；不得增强基线搜索。

## 新 fail-closed 闸门

`ZhangBoV35FormalComparisonRunner` 是单臂、独立 JVM 的启动器；它没有批量调度器，也不会构造 `PFref`。每条调用必须以唯一 `RunKey` 的冻结计划启动，且只允许 `HMOPSO_QGS_F` 或已冻结的 `V35_MAIN` 臂。

执行前，它会拒绝下列任一条件：

1. `FC-8`、`EXP-1`、正式矩阵授权、20-seed 清单或算法 roster 未明确批准；
2. plan、source manifest、实例、SUT 扩展、疲劳参数、A0 配置、MAIN 配置任一 SHA-256 未冻结或不一致；
3. 计划未固定 FM3、单族、序列无关 SUT、Shift NONE、`[0,1,6]`、100 粒子、500000 FE、`GLOBAL_ORIGINAL`、既定局部搜索顺序、BAL 全开放、`rho=0`、`20/40/20/20` 或双Q `P=5/G=5`；
4. roster 没有同时登记 `HMOPSO_QGS_F=V35_BASELINE` 和一个已冻结 `V35_MAIN` 主版本；
5. 同一 seed 未具备 **V35 fingerprint hash** 与 **P8 SHA-256 hash** 两种初始种群锁定口径。

每次实际单臂运行还会在写出原始前沿前复核：实例/扩展/疲劳 provenance、运行时配置哈希、公平契约哈希、`0 < fullEvaluations=decoderCalls <= 500000`、非法解/重复完整评价为 0、无 Shift 活动、有限且非空且不含重复点的三目标前沿。若为避免半代或半局部窗口而安全停止，实际 FE 必须完整记录；它不能被伪装为满额运行。随后分别要求 A0 的基线机制、MAIN 的 DSCR/CFVF/Qp/档案/CA-TA-Lite 机制有真实事件，且 MAIN 的 `dominatedTeacherUses=0`。

这套约束不会更改任何算法、PDDR、解码器或 A0–A4 配置；它只拒绝没有完成冻结或公平证明的启动请求。

## 本轮生成的协议资产

| 路径 | 作用 | 当前状态 |
|---|---|---|
| `formal-comparison-plan.properties` | 单一 RunKey 的总闸门计划 | intentionally unfrozen |
| `formal-seed-list.properties` | 20 seed 的只读清单格式 | intentionally unfrozen，0 seed |
| `formal-algorithm-roster.properties` | 两臂身份与 MAIN 冻结位 | intentionally unfrozen |
| `source-manifest.tsv` | 源码/构建 provenance 清单格式 | intentionally unfrozen |
| `frozen-configs/*.properties` | A0、MAIN 的不可变运行时配置格式 | intentionally unfrozen |
| `run-registry.csv` | 只追加的 physical-run 注册表结构 | 仅表头，不含运行 |
| `fairness-contract-template.properties` | 双初群哈希与共享边界格式 | 无运行值 |
| `../03_main_45x20/raw-fronts/README.md` | 原始前沿存放纪律 | 无原始前沿 |

`08_reference_and_statistics/NO_PFREF_BEFORE_FREEZE.md` 明确禁止本轮创建最终参考前沿、HV/IGD、Wilcoxon 或任何论文统计结论。

## 仍需用户或 FC-8/FC-9 批准的事项

1. 人工复核 FC-6 拒绝结论，并决定是否进入 FC-7；不得跳过到 FC-8。
2. FC-8 Champion Gate 通过，并冻结最终 MAIN 版本（A4 或另一经批准候选）及其完整 canonical 配置哈希。
3. 冻结并签名 20 个 seed、算法 roster、45 实例清单、每 seed 的初始种群双哈希、实例/SUT/疲劳/源码 manifest。
4. FC-9、EXP-1 和正式 45×20 工作包的明确用户资源授权。仅在这之后可以把 `execution.authorized` 置为 true。
5. 如最终 MAIN 不是当前模板严格支持的 `A4_PACING_CANDIDATE`，必须先新增经过审计的明确适配，而不是放宽 runner 或把 A5 静默替换进去。

## 本轮边界结论

| 项目 | 结果 |
|---|---|
| 500k 正式比较 | 未启动 |
| 最终 PFref | 未创建 |
| 统计/论文优越性结论 | 未生成 |
| 算法、PDDR、解码、A0–A4 语义漂移 | 无 |
| 小预算接线验证 | 仅允许使用现有 `V35FairRunnerTest#baselineAndFullShareControlledStart`；其为 2×2000 FE 工程桥接测试，不是正式矩阵或 MAIN 冻结证据 |
