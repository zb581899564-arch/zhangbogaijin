# HARD–NORMAL 分析报告（V35-SOURCE-ATTRIBUTION-500K / 10-v5-sa-normal-500k）

- 轨迹：HARD = `100_5_3_1/20260901/A4/500k/V5 ON`（D-114，已验收）；NORMAL = `100_2_3_1/20260901/A4/500k/V5 ON`（本包，已验收）。
- 口径来源（未重建、未改阈值）：Phase A0 冻结 `source-attribution-thresholds.json`（COUNTERFACTUAL_PRODUCER_SET、WHVGShare、ExclusiveNDShare、T_HV=2.0pp、T_ND=10.0pp、连续2窗）与 `performance-divergence-thresholds.json`（t_div fallback：HV 1.0pp AND IGD 10pp，连续2 checkpoint）。
- 指标实现：`scripts/fc6_metrics.py` + `V35-PFC5-PHASE0/tools/build_reference_contract.py`（冻结精确拷贝，禁止重建）。
- 窗口 ND/折叠用 numpy 逐算复刻 `fc6.dominates/equal`（同一 float64 运算），三重等价验证全 PASS（V1/V1c/V1d 随机与回归、V2 与冻结 `threshold_recompute.window_metrics` 子样本逐来源全等、V3 反事实单调界）。

## 1. 结论

```ini
SOURCE_ATTRIBUTION=G4_NO_ACTIONABLE_LEVER
OLD_A4_DIAGNOSTIC_CLOSED=true
G1_GLOBAL_CFVF=INSUFFICIENT
G3_CATA=NOT_TRIGGERED
SOURCE_LEVER_CANDIDATE=NONE
t_div=NOT_REACHED
SA_A2_CONDITIONAL_ELIGIBLE=false
```

Phase A 结束（G4出口）。A2 不具备资格。Phase B 及任何修复/DOE/Validation/Final/正式矩阵须新的明确授权。

## 2. Coverage divergence（`coverage-divergence.csv`）

逐 25k checkpoint（i=0..20，i=0=B0，i=20=terminal）计算 HARD 与 NORMAL 各自实例 PFref 归一化空间内的 HV/IGD、hvProgress、igdRelImp，以及 deficit（normal − hard，正值=hard更差）。

- **t_div=NOT_REACHED**：不存在 i 使 `deficitHV(i)≥1.0pp AND deficitIGD(i)≥10.0pp` 且 `lag(i+1)` 同样成立。逐 checkpoint 的 lag 全部为 False。
- 终态（i=20）：HARD HV=`0.5545772540415207`、IGD=`0.15898065502479636`（F1合同 PFref 757点归一化）；NORMAL HV=`0.6173682314632705`、IGD=`0.19910024655631323`（统一 PFref 1979点归一化）。**两者各自实例空间内，不跨实例直接比较绝对值**；t_div 只看各自空间内的 progress deficit。
- B0（i=0）：两 run 的 HV 均为 0（B0 100 点随机初始种群的严格 ND 前沿在 PFref 归一化空间中完全落在 HV 参考盒 (1.1,1.1,1.1) 之外），IGD 均很大（HARD 1.34 / NORMAL 1.88）。i=1 的 hvProgress 因基线≈0 而出现数值放大（dHVP≈-4.7e8），但该 checkpoint 的 lag 条件因 IGD 分量（3.06pp < 10.0pp）为 False，不影响 t_div 判定。后续 checkpoint（i≥2）数值稳定。
- reference 合同：HARD 用冻结 F1 合同（contract sha `ecdc5589…`，PFref sha `4dc85dd4…`，757点）；NORMAL 用冻结统一 reference front（sha `4b2c96b6…`，1979点）。两者 ideal/nadir 各自独立，`normalReferenceBackfillPolicy=FORBIDDEN`。

## 3. 窗口来源 deficit（`hard-normal-window-comparison.csv`）

| 来源 | metric | firstPersistentWindow | qualify两窗？ | 时序(firstPersistent ≤ t_div) | survival竞争 | 裁决 |
|---|---|---|---|---|---|---|
| GLOBAL_CFVF | WHVGShare | 1 (窗1–2 deficit ≥2.0pp) | ✓ | ✗ (t_div=NOT_REACHED) | ✗ (差<10pp) | INSUFFICIENT |
| GLOBAL_CFVF | ExclusiveNDShare | 17 (窗17–18 deficit ≥10.0pp) | ✓ | ✗ | ✗ | INSUFFICIENT |
| CATA | WHVGShare | NONE | — | — | — | NOT_TRIGGERED |
| CATA | ExclusiveNDShare | NONE | — | — | — | NOT_TRIGGERED |
| INHERITED_LS | both | NONE | — | — | — | N/A (非 G1/G3 候选) |
| PARENT_CARRYOVER | both | NONE | — | — | — | N/A |

**G1=INSUFFICIENT 的原因**：GLOBAL_CFVF 在窗口层确有 WHVG/ExND deficit 信号（NORMAL 的 GLOBAL_CFVF 窗口份额更高），但 G1 的冻结合同要求 deficit 信号 **不晚于 t_div**（即被 coverage divergence 锚定）。t_div=NOT_REACHED → 该锚定条件无法满足 → G1 不能触发。

**G3=NOT_TRIGGERED 的原因**：CATA 无任何 metric 持续 deficit；CATA FE 占比 2.23%（NORMAL）/3.04%（HARD）均低于 5% 实质性门槛。

## 4. 生命周期 survival 比较（`source-survival-comparison.csv`）

| 来源 | mergeToPddrRate H/N | pddrToWorkingRate H/N | survivalAnomalyCompetes |
|---|---|---|---|
| GLOBAL_CFVF | 39.7%/39.7% | 100%/100% | false |
| CATA | 30.7%/30.7% | 100%/100% | false |
| INHERITED_LS | 58.8%/58.8% | 100%/100% | false |

HARD 与 NORMAL 的 survival 转化率逐来源几乎相同 → survival 异常不竞争 → deficit 不能被更强的 PDDR/working survival 异常解释。但该排除成立的前提（t_div 锚定）本身失败，故不改变 G1=INSUFFICIENT 的裁决。

PA/QP 利用层事件中 `PERSONAL_ARCHIVE`（HARD 235922 / NORMAL 234418）、`QP_TEACHER`（各 543600）、`QP_ACTION`（各 543600）的主体指纹不在评价账本（HARD 1,323,122 / NORMAL 1,321,618 行），标记 `NOT_ATTRIBUTABLE_BY_FINGERPRINT_JOIN`，未猜测/比例分摊/回填。

## 5. 四方向极值贡献

`direction-extreme-contributions.csv`（NORMAL 侧）与 HARD 侧同名文件均按窗口 × subSwarmRole 输出 min Cmax/TEC/TWC。两 run 四方向均被真实评价；极值随窗口单调改善后收敛。描述性，未用于门控。

## 6. 禁写与限制

- **禁止**把 CFVF 62% 预算占比或任何窗口份额当作根因。
- **禁止**用 FIRST_ADMISSION 归因（仅 DESCRIPTIVE_ONLY）；共享点对任何单来源反事实贡献为 0。
- **禁止**新调阈值或重建 reference。
- **限制**：B0 退化基线（HV_0=0）使 i=1 的 hvProgress 数值不稳定，但 lag 在 i=1 因 IGD 分量未过门而 False，不影响 t_div 判定；该限制已如实登记。
- **限制**：PA/QP 生命周期归属缺口（~53% 事件无法按来源归属），如实登记，未猜测。

## 7. 停止边界

```ini
SA_A2_CONDITIONAL_STARTED=false
SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
DOE_AUTHORIZED=false
QP_V2_AUTHORIZED=false
CONFIG_RACE_AUTHORIZED=false
VALIDATION_AUTHORIZED=false
FORMAL_AUTHORIZED=false
formalMatrixRunning=false
PDDRChanged=false; CFVFChanged=false; DualQChanged=false; CaTaChanged=false; formalJarChanged=false
```
