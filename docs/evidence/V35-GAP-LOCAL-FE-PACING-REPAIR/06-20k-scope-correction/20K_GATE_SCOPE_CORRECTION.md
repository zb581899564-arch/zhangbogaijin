# 20K_GATE_SCOPE_CORRECTION — 20k 机制门结论范围更正

- 日期：2026-08-31
- 性质：**范围更正（append-only）**。本文件追加并限定 `05-gate-decision/20K_MECHANISM_GATE_REPORT.md` 的结论范围；原报告全文保留，不删除、不改写。
- 触发依据：V35-LOCAL-FE-PACING-50K 任务书 §2。20k 十条运行（2 实例 × 5 臂 × seed 20260907 × 20000 FE）未满足预登记的完整剂量梯度，故原报告的整门 `20K_MECHANISM_GATE=PASSED` 不得再被引用为"机制门整体通过"，其有效范围收缩为实现门。

## 1. 更正事实

预登记的完整剂量梯度要求局部预算总量随 betaMax 单调严格下降：

```
totalLocalFE(C0) > totalLocalFE(C1) > totalLocalFE(C2) > totalLocalFE(C3)
```

20k 实测（`04-mechanism-analysis/fe-reallocation.csv`，两实例一致）：

| profile | betaMax | totalLocalFE | terminationKind |
|---|---:|---:|---|
| C0 | 0.65 | 5158 | PHASE_CONSISTENT_TAIL_STOP |
| C1 | 0.55 | 4900 | EXACT_MAX_FE |
| C2 | 0.45 | 4900 | EXACT_MAX_FE |
| C3 | 0.35 | 4900 | EXACT_MAX_FE |

**C1=C2=C3=4900，严格梯度在 C1–C3 三档完全并列 → 预登记梯度不满足。**

## 2. 结构性解释（20k 规模伪影的精确机制）

冻结源码（隔离 tag `v35-final-doe1-frozen`，`ZhangBoMOHPSOQ.beginLocalFeBudgetWindow` / `V35LocalFeBudgetConfiguration`）给出每外层循环的局部窗口调度：

```
u_k       = fullEvaluationCount / MaxFEs          （窗口开启时刻的全局进度）
B_L(k)    = floor( beta(u_k) / (1 - beta(u_k)) × B_G )，B_G = 每循环 Q 相位 FE
窗口硬顶  = min( FE_open(k) + B_L(k), MaxFEs )
循环启动  当且仅当 FE + qPhase(=5000) ≤ MaxFEs
```

由此，**EXACT_MAX_FE 终止的运行满足恒等式 `totalLocalFE = MaxFEs − globalPhaseFE`**，而 `globalPhaseFE = 100(初群) + outerCycles × 5000` 与 betaMax 无关（初群与 Q 相位长度固定）。20k 下 C1–C3 均为 3 循环 exact-stop：

```
totalLocalFE = 20000 − (100 + 3×5000) = 4900   （对 C1/C2/C3 同一恒等式）
```

即 exact-stop 运行的总量指标对 betaMax **结构性钝感**：释放的局部 FE 由最后一个窗口顶到 MaxFEs 吸收，掩盖了各档每窗口分配上限的差异。闭合公式调度模拟在全部 8 组 C0–C3 × 2 实例上精确复现导出 `totalLocalFE`（C0=5158：1906+3252；C1–C3=4900），并与事件流 `ca-ta-lite-events.log` 的每窗口关闭点一致（如 C3 w2 关闭预测 13818 vs 事件流 feMax 13816，±2）。每窗口分配上限本身严格有序（实例 50_2_3_1）：w1: C0 1906 > C1 1844 > C2 1784 > C3 1725；w2: C0 3252 > C1 2776 > C2 2360 > C3 1993。**该有序性不是 20k 预登记的剂量指标，不能追溯用于宣告 20k 剂量门通过**；它仅说明机制方向正确，并作为 50k 分配门的实现基础（见 `07-50k-preregistration/50K_PREREGISTRATION.md`）。

## 3. 更正后的机器状态

```ini
20kImplementationGate=PASSED
C0BehaviorEquivalent=true
feReallocationDemonstrated=true
doseResolutionAt20k=NOT_RESOLVED
strictPreregistered20kGate=NOT_FULLY_PASSED
50kPurpose=DOSE_RESOLUTION_AND_PERFORMANCE_SCREEN
20kRerunRequired=false
20kRunsRetained=true
repairFamilyNaming=LOCAL_FE_PACING
formerLabelRetired=CATA_BUDGET_REPAIR
```

逐项说明：

1. **20k 无需重跑**：十条运行的实现正确性（预算合法、机制真实触发、C0≡A4 行为等价、来源零丢失）不受本更正影响。
2. **实现正确**：`build_gate.py` 10/10 单条门全部通过（见原报告 §1、§8）。
3. **C1/C2/C3 在 20k 下不可区分**：预登记的总量梯度在 exact-stop 结构下不可分辨（§2 恒等式）。
4. **原报告结论范围收缩**：`20kMechanismGate=PASSED` 仅在其实现门范围内继续有效；任何"整个机制门通过"的表述以本文件的 `strictPreregistered20kGate=NOT_FULLY_PASSED` 为准。

## 4. 聚合门补丁（防止再次混淆）

`04-mechanism-analysis/build_gate.py` 原实现仅检查单条运行门（"10/10 单条通过"），无跨配置聚合校验。已按本更正扩展（同一文件内新增聚合段，原单条门逻辑不变）：

- 新增 `aggregate-gate.csv`：结构门（betaMax 逐值匹配）、分配门（闭合公式调度重建的每窗口与累计分配上限）、消费门（localFeShare / totalLocalFE 梯度）、行为门（outerCycles / cfvfOffspring 非递减）。
- 新增独立聚合裁决输出：20k 数据重跑输出 `DOSE_AGGREGATE_20K=NOT_RESOLVED`，退出码非 0——即修正后的门控下，20k 不再可能"单条全过 ⇒ 整门通过"。

## 5. 对 50k 的约束

- 50k 首要目的是**剂量分辨**（DOSE_RESOLUTION_AND_PERFORMANCE_SCREEN），其次才是性能筛查。
- 剂量分辨失败（仅 C0 与其余配置分开、C1–C3 继续并列）→ 裁决 `DOSE_RESOLUTION_FAILED`，停止本 repair family：不调参、不寻找第五个 betaMax、不启动 250k。
- 50k 的剂量门指标、双口径性能口径与预登记偏差，见 `07-50k-preregistration/50K_PREREGISTRATION.md`（运行前冻结）。

## 6. 命名更正登记

本 repair family 正式名称为 **`LOCAL_FE_PACING`**（单旋钮 `betaMax`）。此前 `CATA_BUDGET_REPAIR` 一名作废：现有证据（FE 回流至更多外层循环/CFVF/Qg/Qp，CA-TA-Lite Test/Apply 正常且 20k 中 C1–C3 的 caTaLiteFE 仅数百 FE 量级）指向 inherited LS 末端窗口的预算调度，而非 CA-TA 有害。历史文件中的旧名按原样保留，不再新增使用。
