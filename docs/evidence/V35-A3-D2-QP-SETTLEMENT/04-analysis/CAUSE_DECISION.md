# V35-A3-D2 Qp 动作策略与 TD 奖励学习：裁决

`a2_a3_d2_root_cause = QP_ACTION_POLICY_HARMFUL`

## 有效性

- 9/9 条配对诊断运行通过文件级 SHA-256 反向核验。
- 所有条目均为 `actualFE=decoderCalls=50000`，且同一 seed 三臂初始种群哈希一致。
- Q0 三臂实际执行 Qp 动作并更新个人档案，但 `trainedTransitions=0`、`rewardSamples=0`，因此没有 TD 学习。
- Q0 三个初始Q表均为零表，30,000次动作中 `KEEP` 为 29146 次（97.15%）；其余动作分布见 `q0-action-distribution.csv`。

## 预注册门

稳定退化：至少2/3 seed 同时 HV下降、IGD变差，且中位 ΔHV≤-2% 或中位 ΔIGD≥+10%。

- D1_PA_DIRECTIONAL→Q0_QP_OBSERVE_ONLY：bad=2/3，median ΔHV=-2.1951%，median ΔIGD=+11.2588%，stableRegression=True。
- Q0_QP_OBSERVE_ONLY→D2_QP_SYNCHRONOUS：bad=1/3，median ΔHV=+0.9718%，median ΔIGD=+5.4631%，stableRegression=False。

## 解释边界

该裁决严格指向“未学习/零表时四动作的实际选择行为”，而不是宣称所有Qp学习都必然有害；Q0→D2未满足TD稳定退化门。此结论不授权改变正式 Jar、奖励公式、个人档案容量、双Q时序、PDDR、DOE或正式矩阵。详见 `pair-metrics.csv`、`metrics-common-reference.csv` 和 `q0-action-distribution.csv`。
