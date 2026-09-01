# V35-A3-D2：Qp 动作策略与 TD 奖励学习最小拆分诊断

本目录是对既有 `V35-A2-A3-DECOMPOSITION` 的**单变量追加诊断**，不是 DOE、正式消融或论文独立样本。

固定边界为 FM3、`ShiftMode=NONE`、单一产品族、序列无关 SUT、`[20,40,20,20]`、`GLOBAL_ORIGINAL` PDDR、`CA-TA-Lite → inherited LS`、方向教师池关闭。不会修改冻结正式 Jar、DOE 参数、PDDR 或暂停中的正式矩阵。

严格链条为：

```text
D1：谱系个人档案 + 确定性方向 pbest，Qp关闭
→ Q0：同一档案 + Qp四动作实际选 pbest，所有周期 OBSERVE_ONLY
→ D2：同一四动作 + LEGACY_UNCLIPPED 奖励和 TD 更新
```

`Q0` 仍更新谱系档案、仍实际使用 Qp 选出的个人领导；仅禁止奖励计算、TD transition 和 Q 表更新。D1/D2 只复用既有三 seed 50k 运行，新增物理运行仅为 Q0 三条。

目录：

- `00-preregistration/`：固定设计、裁决门和来源关联；
- `01-implementation/`：实现与测试证据；
- `02-compatibility-preflight/`：D1/D2 2k 行为兼容验证；
- `03-q0-runs/`：Q0 三条新 50k 运行；
- `04-analysis/`：统一/两两参考、指标与唯一裁决；
- `05-verification/`：SHA-256 与反向验收结果。

任何结论仅可使用 `04-analysis/CAUSE_DECISION.md` 中预注册的四类之一；不得由本目录直接修改正式算法或恢复正式矩阵。
