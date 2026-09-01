# V35-A3-D3 Qp冷启动并列策略：裁决

`cold_start_tie_verdict = COLD_START_TIE_BREAK_NOT_CONFIRMED`

## 有效性

- 9/9 D1/Q0/Q1记录通过各自文件级SHA-256清单。
- 同seed三臂初始种群哈希一致；所有条目`actualFE=decoderCalls=50000`，无非法解与重复评价。
- Q1已验证为`DIRECTIONAL_IF_TIED`，且`trainedTransitions=rewardSamples=0`。

## 预注册对照

- Q0_QP_OBSERVE_ONLY→Q1_QP_DIRECTIONAL_TIE：good=2/3，bad=1/3，median DeltaHV=+0.8850%，median DeltaIGD=-2.0515%。
- D1_PA_DIRECTIONAL→Q1_QP_DIRECTIONAL_TIE：good=0/3，bad=3/3，median DeltaHV=-1.3788%，median DeltaIGD=+16.3038%。

## 边界

本结果只评价零表贪心并列的破平规则；不修改奖励、TD、个人档案、双Q、PDDR、DOE或正式矩阵。详见`pair-metrics.csv`与`q-action-distribution.csv`。
