# P8 集成、消融与工程验收报告

状态：`in_progress`  
日期：`2026-08-09`  
范围：仅 `张博改进` 的 Java/jMetal 5.8 工作副本；未运行 P9、未运行 `500000 FE`。

## 结论

P8 已建立统一实验注册表、确定性共同初始种群、运行记录、工程参考前沿和指标计算。`20_2_3_1` 上当前可真实暴露的 16 个配置标签均以三个批准 seed 完成，共 48 次成功运行；评价数介于 `1958` 与 `2000`，无非法解，CFVF 正常后置 repair 为 `0`。

P8 **不能标记 completed**：五组完整消融矩阵中仍有 18 个要求标签尚未以独立生产开关暴露；第四章 10 工件黄金夹具也不是作者直接派生路径可加载的 EADHFSP 源实例。因此它们均被显式记录为 `NOT_EXPOSED`，没有以近似组合替代。

这是一轮工程比较与机制审计，不是论文正式实验、抽样复现或完整复现。以下标志继续为 `false`：

```text
integration_engineering_validated=false
ablation_engineering_validated=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

## 固定实验口径

- 实例：`EADHFSP/20_2_3_1.txt`；实例 SHA-256 位于 `run-records.csv`。
- 对照黄金夹具：`chapter4-golden-10x2x2`，全条目均 `NOT_EXPOSED`，原因见下文。
- 种子：`20260808`、`20260809`、`20260810`。
- 每个配置：`population=100`、`MaxFEs=2000`。
- 物理子群槽位：`{20,40,20,20}`，语义映射版本由配置 canonical text 固化。
- 主比较目标：作者七槽位中的 `[0,1,6] = [Cmax, TEC, TWC]`，全部按最小化。
- 参考前沿：同一实例所有 `COMPLETED` 运行的最终非支配解合并并严格 Pareto 过滤后的工程参考集；它不是理论真实 Pareto 前沿。

## 注册矩阵与实际执行

注册表包含 34 个唯一标签：全向量 PSO 7 个、疲劳 4 个、Q-pbest 7 个、VNS 7 个、完整组合 9 个。

| 范围 | `COMPLETED` | `NOT_EXPOSED` | 说明 |
|---|---:|---:|---|
| `20_2_3_1` | 48 次（16 标签 × 3 seed） | 54 次（18 标签 × 3 seed） | 所有真实暴露配置均已运行 |
| 第四章黄金夹具 | 0 | 102 次（34 标签 × 3 seed） | 无作者兼容 EADHFSP 实例，拒绝合成或混用语义 |
| 总计 | 48 | 156 | `run-records.csv` 共 204 条 |

已真实运行的标签：

```text
FV0, FV-Full,
FM0, FM3,
QP0, QP6,
V0, V-Full,
B0, B1, B2, B3, B4, B5, B7, FULL
```

未暴露标签为 `FV1–FV5`、`FM1–FM2`、`QP1–QP5`、`V1–V5` 和 `B6`。每项缺口及原因已单独写入 `unsupported-combinations.csv`；它们没有被映射为任何其他版本。

## 预算、共同起点与重放边界

- 同一实例和 seed 的 34 个注册条目使用同一初始种群 SHA-256；每个 seed 均只有 1 个初始种群哈希。
- P8 的共同初始种群由显式 seed-keyed provider 生成并按当前实例的两阶段资源域合法化。该处理只作用于 P8 比较边界，未改动 P4.1 作者默认初始化或其零创新路径。
- 48 次完成运行均满足 `FE <= 2000`；CA-TA 配置因“预算不足完整代”安全停止，最低为 `1958 FE`，没有半评价候选或预算超支。
- 三次 `B5` 同 seed 重跑的最终前沿 SHA-256 一致，证明 P8 受控创新主线可重放。
- 三次 `B0` 同 seed 重跑的最终前沿 SHA-256 不一致。根因是作者活动更新路径保留了散落的 `new Random()`；这是 `author_actual` 的既有行为，不在 P8 中暗改。因此 P8 不声称作者基线端到端可由单一 seed 完全重放。

## 工程参考指标（完整组合序列）

以下为 `FULL` 矩阵下每标签三 seed 均值，仅用于工程观察，不做显著性检验或论文结论。

| 标签 | 平均 HV | 平均 IGD | 平均非支配解数 |
|---|---:|---:|---:|
| B0 | 0.261472 | 0.725544 | 14.67 |
| B1 | 0.317482 | 0.323554 | 49.67 |
| B2 | 0.454315 | 0.238935 | 60.00 |
| B3 | 0.476871 | 0.216890 | 76.00 |
| B4 | 0.495219 | 0.219375 | 61.67 |
| B5 | 0.441042 | 0.236401 | 67.67 |
| B7 | 0.384345 | 0.268036 | 71.67 |
| FULL | 0.384345 | 0.268036 | 71.67 |

`B7` 与 `FULL` 当前配置等价；保留两个标签是为了忠实登记计划中的两种命名位置，不能据此重复计入独立算法证据。

## 构建与回归

- 六模块 Maven `package` 成功；P8 registry、运行器类均为 Java 8 字节码（major version `52`）。
- P2–P7.2及P8定向回归共 50 项通过：问题模块 19 项、算法模块 17 项、运行器模块 14 项；其中包括黄金夹具、原始解码、疲劳、CFVF、CA-TA、作者派生、P6链路和P8初始群体测试。
- 根 `mvn test` 在 JDK 17 模块开放参数下执行到 `jmetal-core`：651 项、0 failures、3 errors、6 skipped。三项错误的测试签名与P1登记项相同：`PMXCrossoverTest`、`PermutationSwapMutationTest`、`DefaultIntegerPermutationSolutionTest` 的自定义随机源/构造器测试；根reactor因此未继续执行下游模块。P8定向下游回归已独立通过，未把这三项旧错误伪装为P8通过。

## 证据索引

- `matrix-registry.csv`：34 个标签、机制、配置键和暴露状态。
- `unsupported-combinations.csv`：全部未暴露条目及其具体缺口。
- `run-records.csv`：配置、实例、初始种群、FE、诊断和前沿规模。
- `metrics.csv`：HV、IGD、Spacing、双向 C-metric、最小三目标值。
- `reference-fronts/20_2_3_1.csv`：冻结的工程比较参考前沿。
- `runs/`：每次运行的前沿和 canonical configuration text。
- `input-source-manifest.tsv`：实例、疲劳参数、SUT扩展和P8相关源码的 SHA-256。
- `evidence-sha256.tsv`：P8证据目录中除自身外的文件 SHA-256 总清单。
- `p8-all-registry-run-final.log`：本报告引用的最终矩阵执行日志。

## 已知限制与下一门槛

1. P8 还缺少 18 个专项消融的独立生产开关；在它们真实实现、单独接入并重跑前，P8 不能通过完整消融验收。
2. 第四章黄金夹具保持 `published_baseline` 验证线，不能直接替代作者派生 EADHFSP 输入；需要另行建立经批准的作者兼容桥接，或保持本轮 `NOT_EXPOSED`。
3. 作者 `B0` 更新含未受控随机性；若未来需要严格随机重放，应单独制定兼容策略并重新验证“关闭创新严格退化”的硬门，不能在 P8 内静默替换。
4. P9 仍须用户单独批准实例集、种子数、评价预算、敏感性和统计矩阵。
