# HARD侧来源窗口分析报告（V35-SOURCE-ATTRIBUTION-500K / 09-v5-sa-hard-500k / SA-HARD-V5 500k）

- 轨迹：`100_5_3_1 / 20260901 / A4 / C0_BETA_MAX_065 / 500k / observer V5 ON`
- 口径来源（未重建、未改阈值）：Phase A0 冻结 `source-attribution-thresholds.json`
  （COUNTERFACTUAL_PRODUCER_SET 归属、`WHVGShare`、`ExclusiveNDShare`、fallback 阈值 2.0pp / 10.0pp / 连续 2 窗）
  与 `performance-divergence-thresholds.json`（`t_div` fallback：HV 1.0pp AND IGD 10pp）。
- 指标实现：`scripts/fc6_metrics.py` + `00-preregistration/threshold_recompute.py` 窗口语义（见 §5 等价验证）。
- 一级来源固定四类：`GLOBAL_CFVF / CATA / INHERITED_LS / PARENT_CARRYOVER`（运行后未增加第五类）。

## 0. 结论摘要（先给边界）

```ini
FAILURE_CLASS_REPRODUCTION=PASSED      # 见 04-failure-reproduction
RUN_ACCEPTANCE=PASSED(61/61)           # 见 03-run-acceptance
HARD_WINDOW_EVIDENCE=COMPUTED          # 20/20 窗口，见 §2–§4
HARD_NORMAL_DEFICIT=NOT_COMPUTABLE     # 缺 SA-NORMAL，见 §6
G1_GLOBAL_CFVF=UNDECIDED (needs SA-NORMAL)
G3_CATA=UNDECIDED (needs SA-NORMAL)
t_div=NOT_COMPUTABLE (needs SA-NORMAL)
SOURCE_LEVER_CANDIDATE=NONE            # 本包不设杠杆候选
SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
```

本包**只能**产出 HARD 单侧窗口证据；hard–normal 差值门、G1/G3 裁决和 `t_div` 配对分析必须等待 SA-NORMAL。
任何"CFVF 是根因"的表述在本包中**不被授权**。

---

## 1. 描述性预算占比（不是根因证据）

| 一级来源 | 全run评价量 | 占比 |
|---|---:|---:|
| GLOBAL_CFVF | 310000 | 62.00% |
| INHERITED_LS | 174702 | 34.94% |
| CATA | 15198 | 3.04% |
| PARENT_CARRYOVER（INITIAL_POPULATION 100 + PDDR parent 行 N_eval=0） | 100 | 0.02% |

逐窗口评价量（`source-window-metrics.csv`）：GLOBAL_CFVF 由窗1 的 19894（79.6%）单调降至窗18–20 的 10000（40.0%）；
INHERITED_LS 由 4253（17.0%）升至 14503（58.0%）；CATA 全程 465–1106（约 2–4%）。
该变化是冻结 `betaMax=0.65` 的 `β(u)=βmin+(βmax−βmin)u²` 局部预算配额的既定行为，不是本包新发现，也不构成根因证据。

> **禁写提示（任务书 §6）**：不得把"CFVF 占 62% 评价量"解释为 CFVF 根因。本节仅为预算描述。

## 2. 观察性机制证据（窗口级，因果未证）

数据来源：`source-window-metrics.csv`（20 窗 × 4 来源）。`WHVGShare`/`ExclusiveNDShare` 采用冻结反事实口径
（`Wt^-s` 仅剔除 `producerSet=={s}` 的三元组；共享点对任何单来源反事实贡献为 0；FIRST_ADMISSION 仅描述性，未进入任何门控）。

| 观察 | 证据 | 性质 |
|---|---|---|
| GLOBAL_CFVF 的边际 HV 贡献（WHVGShare）全程稳定在 11.6%–21.2%，**无逐窗崩塌** | 20 窗 `WHVGSharePct` 极差 11.62pp，末窗 13.14% 仍高于窗2 的 11.00% | 观察性 |
| GLOBAL_CFVF 独占新 ND 占比由早期 ~85–91% 降至后期 61–78%（窗19 最低 61.19%） | `exclusiveNDSharePct` 窗3 91.02% → 窗19 61.19% | 观察性（窗口内相对划分） |
| INHERITED_LS 独占新 ND 占比由早期 ~7–10% 升至后期 20–36%（窗19 36.32%） | 同上 | 观察性 |
| CATA 独占新 ND 占比全程 0–3.6%（20 窗合计 `nexclND=64`），WHVGShare ≈ 0 | `CATA` 行 | 观察性 |
| 单窗新非支配点数 `nndAll` 在 153–279 间波动，无单调枯竭 | `nndAll` 列 | 观察性 |
| PDDR→working population 转化 100%（每窗 selected==survived），merge→PDDR 转化：GLOBAL 39.7%、CATA 30.7%、INHERITED_LS 58.8% | `source-lifecycle-summary.csv`（按 fingerprint 连接归属，见 §5） | 观察性 |
| 每 generated 候选的 PDDR 入选率：CATA 7.40% > GLOBAL_CFVF 1.31% > INHERITED_LS 0.56% | 同上 | 观察性（单轨迹） |
| Qg 教师曝光构成：GLOBAL_CFVF 10303（83.1%）、INHERITED_LS 2032（16.4%）、CATA 11（0.09%），相对其预算占比（62%/35%/3%） disproportionate | 同上 | 观察性 |

**读法约束**：以上全部是**单轨迹内部**的窗口描述与转化率。窗口内 `ExclusiveNDShare` 是对当窗新 ND 点的划分（各来源之和 ≈100%），
其变化反映来源间的相对结构变化，**不等于**"某来源失效"。`WHVG` 的绝对量随窗口收敛自然下降，跨窗不可直接比较。

## 3. 四方向极值贡献

`direction-extreme-contributions.csv` 按窗口 × `subSwarmRole`（G1_CMAX/G4_BALANCED/G2_TEC/G3_TWC）输出评价量与
`minCmax / minTEC / minTWC`。四方向代表在 20 个窗口中均被真实评价（无零计数方向），极值随窗口单调改善后收敛。
该文件为描述性极值轨迹，未用于任何门控。

## 4. 生命周期账本（V5 真实事件）

`source-lifecycle-events.csv` = 2,430,744 行，十类事件齐全（GENERATED 500000、QP_ACTION 543600、QP_TEACHER 543600、
DESCENDANT 493233、PERSONAL_ARCHIVE 235922、IMPROVING_DESCENDANT 73874、MERGE_POOL 15715、QG_TEACHER 12400、
PDDR_SELECTED 6200、WORKING_POPULATION 6200），来源无 UNSET。事件 FE 只是观察时间戳，不构成新增评价。

## 5. 计算方法、等价验证与已知缺口

- **冻结实现复用**：HV / ND / 归一化 / equal 一律来自 `scripts/fc6_metrics.py`；窗口切分、producerSet、反事实剔除语义
  与 `threshold_recompute.py` 一致。
- **向量化加速（唯一新增工程手段，非语义重建）**：25k 点级窗口 union 的 `nondominated` 与 1e-12 折叠在原始 O(n²) Python
  下不可行，故以 float64 numpy **逐算复刻** `fc6.dominates` / `fc6.equal` 的同一浮点运算，并对两处非传递性语义
  （`fc6.equal` 非传递 → 采用 TR 的"首个匹配组"顺序折叠而非连通分量；`any_equal_to` 必须"同一点全坐标接近"而非"某点某坐标接近"）
  做了回归测试。
- **等价验证（运行于 `06-independent-verification/compute_hard_source_windows.py`，全部 PASS）**：
  - V1：`nd_fast == fc6.nondominated`，121 组随机/重复/ULP 邻接用例；
  - V1c：`fold_groups_fast == TR.canonical_groups`，60 组随机用例（含 ULP 邻接 Cmax 簇）；
  - V1d：`any_equal_to` 单坐标接近回归；
  - V2：冻结 `TR.window_metrics` 与快路径在窗1/窗10 确定性子样本（stride=40，12500 事件）上**逐来源逐值全等**；
  - V3：每窗 `whvgSumMinusTotalGain ≤ 1e-9`（冻结合同自带的反事实单调界）。
- **生命周期归属缺口（如实登记，未猜测）**：lifecycle 中只有 GENERATED/DESCENDANT/IMPROVING_DESCENDANT 携带真实来源列；
  利用层事件按 `subjectFingerprint → source-ledger.candidateFingerprint → firstLevelSource` 连接。
  解析结果（`lifecycle-attribution-coverage.properties`）：
  - `MERGE_POOL` 15662/15715 可归属（53 个 fingerprint 多来源歧义，已排除未猜测）；
  - `PDDR_SELECTED` / `WORKING_POPULATION` 各 6167/6200 可归属（33 歧义）；
  - `QG_TEACHER` 12352/12400 可归属（48 歧义）；
  - `PERSONAL_ARCHIVE`（235922）、`QP_TEACHER`（543600）、`QP_ACTION`（543600）**主体指纹不在评价账本中，无法按来源归属**。
  因此"teacher exposure"本报告只报告 Qg 侧；Qp 侧教师曝光、个人档案贡献在本包为 **NOT_ATTRIBUTABLE_BY_FINGERPRINT_JOIN**，
  需要新的观察钩子（属未来观察器工作包，不属本包授权）。

## 6. 必须等待 SA-NORMAL 的量（本包未计算/不可计算）

| 量 | 状态 | 原因 |
|---|---|---|
| hard–normal `WHVGShare` deficit（门 2.0pp，连续 2 窗） | **NOT_COMPUTABLE** | 需要 `100_2_3_1/20260901/A4/500k/V5 ON` 的窗口指标与 HARD 配对 |
| hard–normal `ExclusiveNDShare` deficit（门 10.0pp，连续 2 窗） | **NOT_COMPUTABLE** | 同上 |
| `t_div`（HV progress deficit ≥1.0pp AND IGD rel-imp deficit ≥10pp，连续 2 checkpoint） | **NOT_COMPUTABLE** | 需要两条 decision-front 逐 25k 轨迹配对 |
| G1（GLOBAL_CFVF）/ G3（CATA）裁决 | **UNDECIDED** | 二者均以上述 deficit 与 `t_div` 为前提 |
| G2（Qp↔CFVF） | 不适用 | 仅 G1 成立后才考虑条件臂 A2 |

`decision-front` 的逐窗口 HV/IGD 本包已可计算（19 检查点 + terminal + B0 齐备），但**只有单侧**，
不能与自身配对形成 deficit；为避免"拿 HARD 自己当对照"的伪门，本包不计算单侧 `t_div` 代理值。

## 7. 停止边界（本包维持）

```ini
SA_NORMAL_STARTED=false
SA_A2_CONDITIONAL_STARTED=false
SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
DOE_AUTHORIZED=false
QP_V2_AUTHORIZED=false
CONFIG_RACE_AUTHORIZED=false
VALIDATION_AUTHORIZED=false
formalMatrixRunning=false
PDDRChanged=false
CFVFChanged=false
DualQChanged=false
CaTaChanged=false
formalJarChanged=false
newFEConsumed=500000 (本包唯一获准运行 SA-HARD-V5)
```
