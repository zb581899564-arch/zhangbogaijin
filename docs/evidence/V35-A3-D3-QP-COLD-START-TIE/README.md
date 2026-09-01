# V35-A3-D3：Qp 冷启动贪心并列策略验证

本目录只验证 D2 中已经定位出的一个极小原因：Q0 的零 Q 表在贪心并列时稳定选中第一个合法动作
`KEEP`。它不是 DOE、正式消融或论文独立样本。

严格比较为：

```text
D1 = 谱系个人档案 + 固定 DIRECTIONAL pbest
Q0 = 同一档案 + Qp 四动作、零表贪心 FIRST_VALID、无 TD 学习
Q1 = 同一档案 + Qp 四动作、零表贪心 DIRECTIONAL_IF_TIED、无 TD 学习
```

唯一变量是**合法动作 Q 值并列时的贪心破平规则**。Q1继续使用同一epsilon探索、同一行动掩码、
同一`LEGACY_UNCLIPPED`奖励配置（但不结算奖励）、同步Qp/Qg时序及其余冻结边界。

输出不得修改冻结Jar、PDDR、DOE1、个人档案容量、双Q、奖励公式或正式矩阵。
