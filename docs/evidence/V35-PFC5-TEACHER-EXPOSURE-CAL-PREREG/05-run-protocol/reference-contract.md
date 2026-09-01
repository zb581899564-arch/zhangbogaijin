# Race 统一参考契约（预登记）

## 1. 构造时机与范围

**每个实例只能在 `4 configs × 2 seeds` 全部完成并通过验收后，构造一次** `PFref_cal(instance)`：

```text
PFref_cal(instance) = ND( C0 ∪ C1 ∪ C2 ∪ C3 的全部有效终态 front )
```

- 集合成员：该实例下 8 次运行（4 配置 × 2 seed）的全部有效终态前沿。
- `ND` = 严格非支配过滤。
- 每实例独立构造，**不得跨实例共用**。

## 2. 共享口径

同实例共享：

```text
ideal / nadir              由 PFref_cal(instance) 自身导出（各目标 min / max）
normalization              逐目标 (x - ideal) / (nadir - ideal)，span 下限 EPS=1e-12
HV reference               (1.1, 1.1, 1.1)
IGD 实现                   与 V35-PFC5-PHASE0/04-reference-contract 完全同一实现（analyze_confirmation.py 精确副本）
objective order            [Cmax, TEC, TWC]（目标槽位 [0,1,6]）
```

主判据 HV/IGD 的**口径必须与冻结 gold 锚点一致**：原始终态前沿 → 按 PFref 归一化 → HV/IGD，不做额外的严格非支配过滤。

## 3. 禁止

```text
· 每配置独立 reference
· 看结果后加入历史 front
· 不同实例共用 PFref
· 把诊断 case 的 reference（100_5_3_1 / seed 20260901 / PFref 757 点）用于 Race
· 把 F1 的新前沿加入 PFref_cal
· 运行中或运行后更新 ideal / nadir
```

## 4. 记录指标

每次运行记录：

```text
HV  IGD  Cmax  TEC  TWC  Spacing  frontSize  runtime
teacher concentration  teacher entropy  unique teacher count  teacher improvement/exposure
```

**参数选择只使用 HV 与 IGD。**
Cmax / TEC / TWC 只作破平与机制解释，不参与选择。

## 5. 与 F1 参考契约的关系

F1 使用的 `reference-contract.properties`（`ecdc5589…235f`，PFref 757 点）**仅服务于 100_5_3_1 / seed 20260901 的诊断 case**，其 `comparisonTarget` 为历史 A2。Race 使用**另行构造**的 `PFref_cal(instance)`，两者不得混用。
