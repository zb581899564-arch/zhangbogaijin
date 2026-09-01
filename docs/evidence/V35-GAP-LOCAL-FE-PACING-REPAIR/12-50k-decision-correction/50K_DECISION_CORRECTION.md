# 50K_DECISION_CORRECTION — 50k 候选裁决勘误（append-only）

- 日期：2026-08-31
- 性质：**裁决勘误**。原裁决 `11-50k-decision/50K_REPAIR_DECISION.md` 的执行与剂量分辨部分全部维持有效；仅其"候选筛选"结论（C2 因双口径 TWC 符号翻转出局）被本文件取代。原文件保留，并在其末尾追加 supersession 注记。
- 触发依据：V35-GAP-LOCAL-FE-PACING-250K 任务书 §1。

## 1. 维持有效的部分（不因本勘误改变）

```ini
50kExecutionAccepted=true
doseResolution50k=PASSED
C1Rejected=true
scheduleValidation=16/16
fairGroupsPassed=4/4
formalJarChanged=false
PDDRChanged=false
CFVFChanged=false
DualQChanged=false
CaTaChanged=false
```

- 16/16 运行验收、剂量分辨四门（G1–G4）、闭合调度验证不受影响。
- **C1 的出局维持**：其失败原因是困难实例改善门（终态 median ΔHV_hard=−0.19% ≪ +2% 且 ΔIGD_hard=+3.77% ≪ +10%），与口径争议无关，为完整门失败。

## 2. 勘误理由

### 2.1 原双口径门比较的不是同一类对象

50k 冻结Jar无法导出共同FE处的完整前沿（偏差 D1：`checkpointFrontExport=NOT_EXPORTED_BY_FROZEN_JAR`），因此原"双口径一致门"实际比较的是：

```text
终态完整前沿的 HV/IGD   vs   共同FE处三个目标的标量极值（bestCmax/bestTEC/bestTWC @40000）
```

两套口径的统计对象不同（前沿分布 vs 单点极值），敏感度不同（TWC 极值对个别解敏感，前沿 HV 对整体分布敏感）。以标量极值的符号翻转否决基于完整前沿的晋级资格，**证据强度不足以支撑淘汰**。该口径不对称在 50k 预登记 §4-D1 已登记，但原裁决未将其反映到双口径门的证据权重上——这是裁决错误，不是执行错误。

### 2.2 翻转幅度远低于任何实质性阈值

C2 的 TWC 跨口径差异（pooled）：

```text
terminal ΔTWC        = +0.1293%
common-FE scalar ΔTWC = -0.1059%
幅度                  ≈ 0.235 个百分点
```

逐实例看：50_2_3_1 终态 +1.1930% vs common −0.1059%（唯一反号实例）；100_5_3_1 终态 −0.6094% vs common −0.7929%（同号）。反号幅度不足 1.3 个百分点、pooled 不足 0.24 个百分点，且无任何 2/3 seed 一致性支撑。按 250k 任务书 §九 的实质性阈值（HV>2% 或 IGD>10% 且 ≥2/3 seed 一致），该翻转属 **`MINOR_FLUCTUATION`**，不构成预算敏感性冲突。

### 2.3 C2 在主筛选指标上优于 C3

终态口径（完整前沿，主筛选指标）、两实例、两 seed 逐项：

| instance | 指标 | C2 | C3 | 优者 |
|---|---|---|---|---|
| 50_2_3_1 | HV（2-seed 均值） | 0.940959 | 0.892715 | C2 |
| 50_2_3_1 | IGD | 0.063961 | 0.085212 | C2 |
| 100_5_3_1 | HV | 0.819027 | 0.782528 | C2 |
| 100_5_3_1 | IGD | 0.131939 | 0.147129 | C2 |

median 配对响应同样：ΔHV_hard C2 +10.72% vs C3 +4.58%；ΔIGD_hard C2 +25.13% vs C3 +17.30%。C2 全面优于 C3（终态 HV/IGD），却被幅度不足 0.24pp 的标量符号翻转淘汰——淘汰错误。

## 3. 勘误后的机器状态

```ini
50kExecutionAccepted=true
doseResolution50k=PASSED
C1Rejected=true
C2EligibleFor250k=true
C3EligibleFor250k=true
50KDecision=TWO_CANDIDATES_ADVANCE_TO_250K
original50KDecision=ONE_CANDIDATE_ADVANCES_TO_250K(superseded)
c2BudgetSensitivityConflict=RETIRED_AS_MINOR_FLUCTUATION
caliberAsymmetryRegistered=true(D1)
supersessionPolicy=append_only
```

## 4. 对 250k 的约束

- 250k 实验臂固定为 **C0（对照）、C2（betaMax=0.45）、C3（betaMax=0.35）**；C1 不参与。
- 250k 引入真正的完整前沿检查点（`checkpoint-observed-full-front` / `checkpoint-decision-front`，独立实验构建物内纯观察器，正式Jar不动），使共同FE口径升级为前沿对前沿比较；裁决采用任务书 §九 的实质性阈值与 `MINOR_FLUCTUATION` 规则，禁止再用不足 1% 的单标量符号翻转淘汰候选。
- 三目标门同步收紧为"同一实例 ≥2/3 seed 退化 >2% 才构成系统性退化"。

## 5. 证据本底

- 原裁决（保留）：`../11-50k-decision/50K_REPAIR_DECISION.md`
- 配对响应数字源：`../10-performance-screen/paired-response.csv`（SEED 与 MEDIAN_2SEED 段）、`candidate-screening.csv`
- 终态指标源：`../10-performance-screen/metrics-terminal.csv`（主Agent独立复算 PASSED，见 `../11-50k-decision/MAIN_AGENT_INDEPENDENT_CHECK.py` 输出）
- 本目录 SHA 清单：`evidence-sha256.tsv`
