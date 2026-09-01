# V35-A3-D2 实现、兼容与测试记录

## 修改边界

新增仅诊断用途的 `Q0_QP_OBSERVE_ONLY`：它与 D2 同样使用容量6谱系档案和 Qp 四动作，
但 `QpSettlementPolicy=OBSERVE_ONLY_ALL_CYCLES` 强制所有周期只观察。Qp 动作仍决定实际
pbest，档案仍更新；奖励、TD transition 与 Q 表写入均关闭。

该策略只能用于 `V35_DIAG_QP_OBSERVE_ONLY`。正式 A0--A4、默认构造器和正式 Runner 对任何
非标准 Qp settlement policy 均 fail-closed。默认 `STANDARD_BY_DUAL_Q` 不写入既有 canonical
text，因此冻结正式 profile 的文本与哈希语义不变。

## 运行时遥测

`qp-summary.properties`新增（或在历史运行中从冻结 mechanism summary 回读）以下证据：

- Qp动作计数和动作分布；
- `trainedTransitions`与`frozenObservations`；
- 奖励样本数、最小/最大值；
- Q表有限单元、非零单元、范围、绝对值熵和哈希。

该遥测只读取既有状态；不参与随机、评价、Q值、候选选择或行为哈希。

## 兼容预检

新增遥测后，D1/D2在seed `20260822`、请求2k FE下分别从现有固定初群重放。当前
phase-consistent预算在初始100次完整评价后停止；比较行为契约而非纳秒耗时。结果：

- 初始种群、FE、decoder calls、评价轨迹、最终前沿、profile哈希一致；
- personal-leader、Qp、dual-Q事件文件和Qp表/动作计数一致；
- 逐字段证据：`../02-compatibility-preflight/compatibility-checks.csv`；
- 结论：`../02-compatibility-preflight/COMPATIBILITY_REPORT.md`。

## 本地验证

在JDK 8目标下执行：

- `V35A2A3DecompositionConfigurationTest`：7项通过；
- `V35A2A3TelemetryEquivalenceTest`：2项通过；
- `ZhangBoQpControllerTest`：17项通过；
- 合计26项，0 failure、0 error；
- `jmetal-exec`依赖编译通过；新诊断Runner字节码 major version=52。

本目录的`Q0`三条50k物理运行、统一参考、裁决和逐文件哈希位于后续目录；它们不是论文独立样本，
不会改变冻结Jar、PDDR、DOE或正式矩阵。
