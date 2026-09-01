# 名称与因果边界更正（NAMING_AND_CAUSAL_BOUNDARY_CORRECTION）

- 日期：2026-08-31
- 性质：对同目录 `single-repair-family-decision.md`（2026-08-30）裁决名称与因果边界的**追加更正**。原文件保留原样，不覆盖、不改写；本文件为唯一更正来源。
- 触发：总控任务书 `V35-GAP-LOCAL-FE-PACING-REPAIR-V1` §七。

## 1. 名称更正

```ini
oldLabel=SELECT_CATA_BUDGET_REPAIR
newLabel=SELECT_LOCAL_FE_PACING_REPAIR
semanticVersion=v35-local-fe-pacing-repair-v1
correctionType=NAMING_AND_CAUSAL_BOUNDARY
supersededLabelRetained=true
```

原裁决名 `SELECT_CATA_BUDGET_REPAIR`（CATA 预算修复）不准确，正式更正为
`SELECT_LOCAL_FE_PACING_REPAIR`（局部 FE 节奏修复）。后续所有工作包、配置、Jar、
报告与登记表一律使用新名称。

## 2. 因果边界更正（为什么改名）

原名称把杠杆归于 CA-TA 预算，但 0-FE 审计的实际量化边界是：

| 组件 | FE 占比（500k，A4，两实例逐值相同） | 受 betaMax 控制 |
|---|---|---|
| caTaLiteFE（CA-TA Test+Apply） | 14,461 FE ≈ 2.9%（50-job 实测 14,461/500,000） | 间接（共享同一硬预算窗口，但份额小） |
| formalLocalFE（inherited LS：关键交换/插入 + O1–O9） | 175,439/175,380 FE ≈ 35% | 直接（后期窗口主体） |
| 合计 | 189,900 FE ≈ 37.98% | — |
| 其中落在 FE≥250,000 之后的局部预算 | ≈62.3% | 直接 |

结论性边界：

1. **真正受 `betaMax` 主要控制的是后期 inherited LS 窗口**（约 35% FE），不是 CA-TA
   （约 3%）。把 repair 命名为 CATA_BUDGET 会把因果箭头画错。
2. `rootCauseProven=false`：现有证据（尾段 LS 接受率 1.7–6.5%、best Cmax 在
   129k–252k 后停止改善、62.3% 局部预算落在零改善尾段）只支持"后期局部窗口过大
   挤压全局覆盖投入"这一**可反驳假设**，尚未由实验证明因果。
3. `actionableLeverIdentified=true`：`betaMax` 是运行时已验证的预算阀门
   （`V35FinalAblationProfile` L26 → `V35LocalFeBudgetConfiguration.of()` →
   `V35ProductionConfiguration.localFeBudget()`），同一语义轴可单变量扫描。
4. `CaTaHarmfulNotClaimed=true`：现有证据没有证明 CA-TA 本身有害。FC-3 的教训
   （Cheap-Test 永久封禁：CA-TA Test 在贡献搜索）与 Gap Probe V2 中 A4 的竞争力
   均反对该主张。本 repair 只压缩**末端 inherited LS 窗口份额**，不触碰 CA-TA 参数。

## 3. 更正后的单变量定义（不变式）

```ini
selectedRepairFamily=LOCAL_FE_PACING
singleKnob=betaMax
formula=beta(u)=betaMin+(betaMax-betaMin)*u^2; B_L=floor(beta/(1-beta)*B_G)
betaMin=0.25 (frozen)
C0=0.65 (exact current A4, must be bit-equivalent)
C1=0.55
C2=0.45
C3=0.35
forbiddenToChange=CA-TA parameters, Cheap-Test, nTest, Apply multiplier, Qg/Qp
  rewards, Q-table parameters, P5/G5, rho, CFVF probabilities, PDDR, mixture,
  LS order, neighborhood set
```

## 4. 生效范围

- 本更正自 2026-08-31 起对 `V35-GAP-LOCAL-FE-PACING-REPAIR-V1` 及其后续
  （50k/250k/DOE/正式矩阵中与该 repair family 相关的部分）生效。
- 原裁决文件的候选评估表、排除路线（`historical-route-exclusion.csv`）、seed 规则、
  预登记结构**维持有效**；仅名称与因果归因按本文件更正。
- 原 50k 计数错误（24 条 = 4×2×3 的误写）在
  `05-gate-decision/NEXT_50K_PREREGISTRATION_DRAFT.md` 中按 4 配置 × 2 实例 ×
  2 seed = **16 条**更正；如需 24 条必须预注册第三个新 seed。
