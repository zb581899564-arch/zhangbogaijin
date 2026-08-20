# V35-P0 基线冻结报告

## 冻结标识

- `snapshotTag`: `pre-validation-audited-20260810`
- `semanticMainline`: `v3.5`
- `familyMode`: `DEGENERATE_SINGLE_FAMILY`
- `familyCount`: `1`
- `setupMode`: `SEQUENCE_INDEPENDENT`
- `shiftMode`: `NONE`

## 范围

本快照包含当前张博改进工作副本中的源码、POM、资源、测试、配置和既有证据。`target`、`results`、`.git`、`.idea`、`.codex-temp`及日志文件排除。完整文件级 SHA-256 清单见 `source-sha256.csv`。

P8/P9、P8.4/P8.6移位结果仅作为历史证据保留，不进入v3.5当前参考前沿或算法结论。

## 当前结论

V35-P0冻结门通过：

- 当前输入、参数、配置和源码哈希已保存；
- v3.5退化产品族和序列无关设置时间语义已写入冻结清单；
- 正式Shift路径尚未完成代码级强制，列为V35-P3待办；
- 产品族/设置时间占位类型尚未完成，列为V35-P1/P2待办。

下一允许工作包：`V35-P1`。
