# PERFORMANCE-SCREEN-50K — V35-LOCAL-FE-PACING (Agent C, Tasks B+C)

- 生成脚本: `10-performance-screen/performance_screen_50k.py`（全部数字由脚本从 16 条 run 文件计算）
- 指标实现: **import `scripts/fc6_metrics.py` corrected 管线**（raw 精确去重 → raw 严格 Pareto → 统一 min/max 归一（以 PFref 为 reference）→ **不 clamp**；HV 扫描线 rx=ry=rz=1.1；IGD 归一化空间，参考集=归一化 PFref）。唯一改动：PFref 已是非支配集，`nondominated(unique(PFref))==PFref`，故把参考端计算提升到循环外（逐 run 等价）
- 剂量门（Task A 输出 `09-dose-resolution/dose-resolution.csv`）: **DOSE_RESOLUTION_GATE=PASSED**

## 1. F_common 判定（§4-D1，从数据确认）

- 检查点存在性: FE=10000 presentIn=16/16; FE=20000 presentIn=16/16; FE=30000 presentIn=16/16; FE=40000 presentIn=16/16
- **F_common = 40000**（全部 16 条 run 在该预注册检查点均有 cmax-audit-curves.csv 行；最大共同检查点）
- 每条 run 在 F_common 的原始标量（bestCmaxGlobal/bestTECGlobal/bestTWCGlobal）见 `checkpoint-fronts-common/checkpoints-registry.csv`（64 行 run×checkpoint 注册表）与 `metrics-common.csv`
- 前沿级 HV/IGD 的共同FE比较按 D1 不创建（NOT_EXPORTED_BY_FROZEN_JAR）

## 2. 终态 reference（PFref_terminal）

- 50_2_3_1: ND(union of 8 terminal fronts) = 557 点（union 输入 2324 点；三目标精确去重 → 严格 Pareto）→ `reference-fronts-terminal/PFref_terminal_50_2_3_1.csv`
- 100_5_3_1: ND(union of 8 terminal fronts) = 242 点（union 输入 1093 点；三目标精确去重 → 严格 Pareto）→ `reference-fronts-terminal/PFref_terminal_100_5_3_1.csv`

## 3. 终态指标（corrected 管线，TERMINAL_PHASE_CONSISTENT_SECONDARY）

| instance | profile | seed | HV | IGD | Spacing | C(f,PFref) | C(PFref,f) | frontSize | minCmax | minTEC | minTWC | actualFE | wall(s) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 50_2_3_1 | C0 | 20260907 | 0.9435 | 0.0704 | 0.0321 | 0.0108 | 1.0000 | 315 | 327.143 | 20987.0 | 33426.3 | 48269 | 29.6 |
| 50_2_3_1 | C1 | 20260907 | 0.9986 | 0.0438 | 0.0274 | 0.1508 | 1.0000 | 246 | 322.661 | 20952.7 | 33463.8 | 45359 | 30.6 |
| 50_2_3_1 | C2 | 20260907 | 0.9853 | 0.0547 | 0.0342 | 0.1149 | 1.0000 | 272 | 327.143 | 21071.9 | 33474.3 | 50000 | 36.8 |
| 50_2_3_1 | C3 | 20260907 | 1.0192 | 0.0340 | 0.0314 | 0.3196 | 1.0000 | 324 | 326.974 | 20789.6 | 33574.1 | 49036 | 34.9 |
| 100_5_3_1 | C0 | 20260907 | 0.9125 | 0.1484 | 0.0297 | 0.2107 | 1.0000 | 122 | 785.569 | 119448.3 | 323328.8 | 48269 | 49.2 |
| 100_5_3_1 | C1 | 20260907 | 0.7290 | 0.1585 | 0.0238 | 0.0331 | 1.0000 | 128 | 781.196 | 119822.3 | 330566.1 | 45359 | 46.0 |
| 100_5_3_1 | C2 | 20260907 | 0.7988 | 0.1249 | 0.0409 | 0.1405 | 1.0000 | 125 | 789.071 | 119843.0 | 328570.2 | 50000 | 53.6 |
| 100_5_3_1 | C3 | 20260907 | 0.8115 | 0.1315 | 0.0353 | 0.1281 | 1.0000 | 144 | 789.071 | 118837.6 | 328637.9 | 49036 | 53.6 |
| 50_2_3_1 | C0 | 20260914 | 0.7347 | 0.1733 | 0.0201 | 0.0790 | 1.0000 | 373 | 325.992 | 21025.2 | 35558.3 | 48269 | 35.1 |
| 50_2_3_1 | C1 | 20260914 | 0.7318 | 0.1368 | 0.0294 | 0.0718 | 1.0000 | 202 | 326.875 | 21110.8 | 36000.1 | 45359 | 30.3 |
| 50_2_3_1 | C2 | 20260914 | 0.8966 | 0.0732 | 0.0334 | 0.2424 | 1.0000 | 216 | 331.026 | 21150.1 | 34658.8 | 50000 | 35.5 |
| 50_2_3_1 | C3 | 20260914 | 0.7663 | 0.1364 | 0.0189 | 0.0108 | 1.0000 | 376 | 328.936 | 21064.7 | 35953.2 | 49036 | 42.5 |
| 100_5_3_1 | C0 | 20260914 | 0.6268 | 0.2120 | 0.0255 | 0.0000 | 1.0000 | 176 | 790.544 | 120083.4 | 330661.1 | 48269 | 52.0 |
| 100_5_3_1 | C1 | 20260914 | 0.7505 | 0.1816 | 0.0329 | 0.1901 | 1.0000 | 181 | 789.511 | 119528.2 | 324326.9 | 45359 | 48.1 |
| 100_5_3_1 | C2 | 20260914 | 0.8393 | 0.1389 | 0.0242 | 0.1983 | 1.0000 | 86 | 769.321 | 118945.3 | 329331.3 | 50000 | 55.0 |
| 100_5_3_1 | C3 | 20260914 | 0.7536 | 0.1627 | 0.0440 | 0.0992 | 1.0000 | 131 | 790.922 | 118167.1 | 326889.3 | 49036 | 53.7 |

（完整 20 列见 `metrics-terminal.csv`；wall = status.properties algorithmRunNanos）

### 3.1 紧凑表：2-seed 均值 HV/IGD（instance × profile）

| instance | C0 HV | C1 HV | C2 HV | C3 HV | C0 IGD | C1 IGD | C2 IGD | C3 IGD |
|---|---|---|---|---|---|---|---|---|
| 50_2_3_1 | 0.8391 | 0.8652 | 0.9410 | 0.8927 | 0.1219 | 0.0903 | 0.0640 | 0.0852 |
| 100_5_3_1 | 0.7696 | 0.7398 | 0.8190 | 0.7825 | 0.1802 | 0.1700 | 0.1319 | 0.1471 |

## 4. 配对响应（同 instance×seed，C0 为基准，正数=候选改善）

### 4.1 终态口径（2-seed 中位）

| instance | candidate | ΔHV | ΔIGD | ΔCmax | ΔTEC | ΔTWC |
|---|---|---|---|---|---|---|
| 50_2_3_1 | C1 | 2.72% | 29.45% | 0.55% | -0.12% | -0.68% |
| 50_2_3_1 | C2 | 13.23% | 40.05% | -0.77% | -0.50% | 1.19% |
| 50_2_3_1 | C3 | 6.16% | 36.50% | -0.43% | 0.38% | -0.78% |
| 100_5_3_1 | C1 | -0.19% | 3.77% | 0.34% | 0.07% | -0.16% |
| 100_5_3_1 | C2 | 10.72% | 25.13% | 1.12% | 0.31% | -0.61% |
| 100_5_3_1 | C3 | 4.58% | 17.30% | -0.25% | 1.05% | -0.25% |

### 4.2 common-FE 口径 @F_common=40000（2-seed 中位；bestCmax/bestTEC/bestTWCGlobal）

| instance | candidate | ΔCmax | ΔTEC | ΔTWC |
|---|---|---|---|---|
| 50_2_3_1 | C1 | 1.04% | -0.12% | -0.53% |
| 50_2_3_1 | C2 | -0.28% | -0.50% | -0.11% |
| 50_2_3_1 | C3 | 0.04% | 0.38% | -0.63% |
| 100_5_3_1 | C1 | 0.34% | 0.13% | -0.23% |
| 100_5_3_1 | C2 | 1.12% | -0.23% | -0.79% |
| 100_5_3_1 | C3 | -0.79% | 0.93% | -0.34% |

（逐 seed 12 行 + 中位 6 行见 `paired-response.csv`；ΔHV/ΔIGD 仅终态口径，D1）

## 5. 性能筛查四门（§10）

### 门1 正常实例安全门（50_2_3_1，终态口径）

判据: median ΔHV ≥ −2% 且 median ΔIGD ≥ −10%；且无单 seed 同时 ΔHV<−5% 且 ΔIGD<−20%

- C1: median ΔHV=2.72% (≥−2%: Y), median ΔIGD=29.45% (≥−10%: Y); veto-seed 同时跌破: none → **PASS**
- C2: median ΔHV=13.23% (≥−2%: Y), median ΔIGD=40.05% (≥−10%: Y); veto-seed 同时跌破: none → **PASS**
- C3: median ΔHV=6.16% (≥−2%: Y), median ΔIGD=36.50% (≥−10%: Y); veto-seed 同时跌破: none → **PASS**

### 门2 困难实例改善门（100_5_3_1，终态口径）

判据: median ΔHV ≥ +2% 或 median ΔIGD ≥ +10%（至少一项）；另一项 ≥ −2%(HV)/−10%(IGD)

- C1: median ΔHV=-0.19% (≥+2%: N), median ΔIGD=3.77% (≥+10%: N); 另一项不恶化超限: HV≥−2% Y, IGD≥−10% Y → **FAIL**
- C2: median ΔHV=10.72% (≥+2%: Y), median ΔIGD=25.13% (≥+10%: Y); 另一项不恶化超限: HV≥−2% Y, IGD≥−10% Y → **PASS**
- C3: median ΔHV=4.58% (≥+2%: Y), median ΔIGD=17.30% (≥+10%: Y); 另一项不恶化超限: HV≥−2% Y, IGD≥−10% Y → **PASS**

### 门3 三目标保护门（common-FE 口径 @F_common=40000）

判据（预登记 §10 权威定义）: 不得两实例同一目标 median Δ 同时 < −2%；单实例 < −2% 单独标注

| candidate | ΔCmax(n/h) | ΔTEC(n/h) | ΔTWC(n/h) | 同时跌破 | <−2% 标注 | 门3 |
|---|---|---|---|---|---|---|
| C1 | 1.04% / 0.34% | -0.12% / 0.13% | -0.53% / -0.23% | no | none | **PASS** |
| C2 | -0.28% / 1.12% | -0.50% / -0.23% | -0.11% / -0.79% | no | none | **PASS** |
| C3 | 0.04% / -0.79% | 0.38% / 0.93% | -0.63% / -0.34% | no | none | **PASS** |

### 门4 双口径一致门

操作化（预登记语言为候选级单一方向"改善/不改善"，且 D1 下两口径唯一共同的维度是 Cmax/TEC/TWC）: 目标 o∈{Cmax,TEC,TWC}，pooled median（2 instance × 2 seed）终态口径符号 vs common-FE 口径符号；任一目标两口径均为非零且反号 ⇒ 该候选 BUDGET_SENSITIVITY_CONFLICT 出局（HV/IGD 无 common-FE 对应维度，不参与符号比较）

| candidate | Cmax term/common | TEC term/common | TWC term/common | 冲突 | 门4 |
|---|---|---|---|---|---|
| C1 | 0.34% / 0.63% | -0.07% / -0.02% | -0.68% / -0.53% | none | **PASS** |
| C2 | -0.22% / -0.22% | -0.37% / -0.50% | 0.13% / -0.11% | TWC term=+0.1293% common=-0.1059% | **FAIL** |
| C3 | -0.25% / -0.22% | 0.73% / 0.78% | -0.78% / -0.63% | none | **PASS** |

### 门4 敏感性备注（逐 instance 口径，2-seed 中位）

若改用更严的逐 instance 符号比较（每 instance 每目标两口径反号即冲突），翻位点如下（其余同号）：

| candidate | instance | 目标 | 终态 median | common-FE median | 反号? |
|---|---|---|---|---|---|
| C2 | 50_2_3_1 | TWC | 1.19% | -0.11% | YES |
| C2 | 100_5_3_1 | TEC | 0.31% | -0.23% | YES |
| C3 | 50_2_3_1 | Cmax | -0.43% | 0.04% | YES |

- C2 的冲突在逐 instance 口径下同样成立（50_2_3_1 TWC：终态 +1.19% vs common −0.11%；机理：其 seed-14 终态 minTWC 改善出现在 FE=40000 之后，C2 终态比 C0 多 1731 FE，属预算敏感型改善，正是本门要拦的情形）
- C3 在逐 instance 口径下会出现一处噪声级翻位（50_2_3_1 Cmax：终态 −0.43% vs common +0.04%，common 侧幅度 ≈0）；本报告采用候选级 pooled median 口径（对 seed 噪声更稳、与预登记"候选方向"单一表述一致）并如实登记该敏感性；任一口径下 C2 均因 TWC 冲突出局

## 6. 五维 Pareto 与候选筛选（§11）

五维（全部越大越好）: ΔHV_hard=100_5_3_1 终态 median ΔHV; ΔIGD_hard 同理; ΔCmax_all/ΔTEC_all/ΔTWC_all = 两实例 common-FE @F_common median 的最差值

| candidate | ΔHV_hard | ΔIGD_hard | ΔCmax_all | ΔTEC_all | ΔTWC_all | 被支配(全体) | 被支配(合格集) |
|---|---|---|---|---|---|---|---|
| C1 | -0.19% | 3.77% | 0.34% | -0.12% | -0.53% | NONE | NONE |
| C2 | 10.72% | 25.13% | -0.28% | -0.50% | -0.79% | NONE | NONE |
| C3 | 4.58% | 17.30% | -0.79% | 0.38% | -0.63% | NONE | NONE |

四门全过（合格）候选: C3

破平键值（顺序: 困难实例 common-FE median ΔCmax → ΔTEC → 正常实例终态 median ΔHV → ΔTWC → |betaMax−0.65|）:

- C1: tb1=0.34%, tb2=0.13%, tb3=2.72%, tb4=-0.68%, tb5=0.10
- C2: tb1=1.12%, tb2=-0.23%, tb3=13.23%, tb4=1.19%, tb5=0.20
- C3: tb1=-0.79%, tb2=0.93%, tb3=6.16%, tb4=-0.78%, tb5=0.30

- **保留候选: C3**（最多 2 个；破平键值排序后截断）
- **最终裁决（§11 六选一）: ONE_CANDIDATE_ADVANCES_TO_250K**
- 保留候选置 `250kEligible=true, 250kPreregistered=false, 250kStarted=false`（不自动启动 250k）

## 7. 状态块（机器可读）

```ini
[performance-screen-50k]
fCommon=40000
doseResolutionGate=PASSED
normalSafetyGate=C1=PASS;C2=PASS;C3=PASS
hardImprovementGate=C1=FAIL;C2=PASS;C3=PASS
tripleObjectiveGate=C1=PASS;C2=PASS;C3=PASS
dualCaliberGate=C1=PASS;C2=FAIL;C3=PASS
retainedCandidates=C3
budgetSensitivityConflict=true
fiveDimParetoDominated=C1:NONE;C2:NONE;C3:NONE
finalVerdict=ONE_CANDIDATE_ADVANCES_TO_250K
250kEligible=true
250kPreregistered=false
250kStarted=false
```
