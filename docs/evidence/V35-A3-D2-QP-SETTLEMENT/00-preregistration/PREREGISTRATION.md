# 预登记：V35-A3-D2 Qp 动作/TD 最小拆分

## 目的

既有 D0--D3 证据显示 D0→D1、D1→D2 都触发稳定退化门，而 D2→D3 未触发。此处不再混合修改个人档案、双Q或PDDR；仅将 D1→D2 拆为动作策略与学习两部分。

## 运行设计

新增且只新增：`Q0_QP_OBSERVE_ONLY × 20260822/20260823/20260824`，每条 50,000 FE、population=100、独立 JVM。

复用：`V35-A2-A3-DECOMPOSITION/04-runs/seed-*/D1_PA_DIRECTIONAL` 和 `D2_QP_SYNCHRONOUS`。

同一 seed 三臂必须具有相同的初始四向量哈希，且每条满足：

```text
status=COMPLETED
fullEvaluations=decoderCalls=50000
front non-empty and finite
illegalSolutions=duplicateEvaluations=exceptionalRepair=missingSource=0
```

## Q0 唯一差异

Q0 采用 `V35QpSettlementPolicy.OBSERVE_ONLY_ALL_CYCLES`：Qp 四动作与个人领导选择保持活动，谱系档案正常更新；`trainedTransitions=0`、`rewardSamples=0`、Q 表保持零表。同步 Qg/Qp 时序、未裁剪奖励配置、CFVF、DSCR、PDDR 和局部搜索边界与 D2 相同。

## 裁决

稳定退化门：至少 2/3 配对 seed 同时出现 `HV` 下降和 `IGD` 变差，并且中位 `ΔHV ≤ -2%` 或中位 `ΔIGD ≥ +10%`。

- D1→Q0 稳定退化、Q0→D2 未稳定退化：`QP_ACTION_POLICY_HARMFUL`。
- Q0→D2 稳定退化、D1→Q0 未稳定退化：`QP_TD_REWARD_HARMFUL`。
- 两段均稳定退化：`BOTH_QP_ACTION_AND_TD_HARMFUL`。
- 其余全部：`NON_ADDITIVE_OR_INCONCLUSIVE`。

共同 PFref 只能在 9 条最终前沿全部完成后一次性构造；两两独立 PFref 仅作敏感性表。结果不用于论文显著性结论。
