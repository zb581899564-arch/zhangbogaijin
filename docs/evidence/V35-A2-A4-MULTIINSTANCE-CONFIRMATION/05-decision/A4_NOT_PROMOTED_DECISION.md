# D-104：A4 未晋升为 Final 候选

日期：2026-08-25。该决策严格执行预注册的
[`V35_A2_A4_MULTISCALE_CONFIRMATION_PROTOCOL.md`](../../../V35_A2_A4_MULTISCALE_CONFIRMATION_PROTOCOL.md)。

## 验收事实

- 6个held-out实例、5个新seed、A2/A4配对，共60条500k运行全部完成；30个配对均有效。
- 每条均通过完整文件SHA-256、冻结Jar/provenance、共享四向量快照、有限前沿与
  phase-consistent budget复核。57条实际FE为500000；另3条A2运行分别为498241、495113、
  495556 FE，均为剩余FE小于5000、利用率大于99%的合法`PHASE_CONSISTENT_TAIL_STOP`，
  且其A2/A4配对FE差异均小于5000。
- 每个实例独立以其10条raw fronts构造`PFref_confirm`，未混入任何先导或开发reference。
- 关键100-job失败`100_5_3_1`已由既有FC-6指标实现在独立交叉复算中确认，reference大小为757，
  与主验收器的HV/IGD逐值一致。

## 裁决

虽然全部30个配对的总体中位为`ΔHV=+1.50%`、`ΔIGD=+7.24%`、`ΔCmax=+1.72%`，但：

- 100-job pooled中位为`ΔHV=-3.03%`、`ΔIGD=-8.02%`；
- `100_5_3_1`中位为`ΔHV=-12.96%`、`ΔIGD=-76.31%`，同时触发预注册100-job否决门。

故正式裁决为：`A4_NOT_PROMOTED`。

这不是“算法实现失败”或“结果无效”：运行与证据链均通过。它表示A4不能在当前冻结语义下被称为跨规模稳定的Final候选。A2成为当前主候选；任何新的Final roster、消融或外部算法比较都需要单独预注册和用户批准。旧4500条A0--A4 Master继续暂停，确认集不得用于正式PFref、统计显著性或论文优越性结论。

## 可复算输入

紧凑本地证据位于`../06-remote-analysis-import/`：

- `acceptance-run-audit.csv`：60条运行的接收账本；
- `reference-fronts/`：六个独立确认reference；
- `metrics.csv`与`paired-deltas.csv`：逐运行及逐配对指标；
- `instance-summary.csv`、`scale-summary.csv`与`promotion-decision.json`：预注册门和裁决；
- `evidence-sha256.tsv`：文件级完整性清单。
