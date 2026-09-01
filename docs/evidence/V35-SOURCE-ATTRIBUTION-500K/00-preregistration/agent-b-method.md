# Agent B — 阈值与指标合同方法记录（V35 SOURCE-ATTRIBUTION-500K Phase A0）

- 日期：2026-08-31。性质：0-FE 预登记（只读取证 + 本目录三个产物写入）。
- 授权：`docs/V35_SOURCE_ATTRIBUTION_500K_PHASE_A_PLAN.md` §3.7/§3.8。未消耗 FE，未上传，未修改任何既有文件。
- 产物（本目录）：
  1. `source-attribution-thresholds.json` — 窗口来源贡献指标合同 + matched-window 可比性判定 + 阈值冻结
  2. `performance-divergence-thresholds.json` — 解释性 t_div 合同 + 历史充足性判定 + 阈值冻结
  3. `threshold_recompute.py` — 可重算脚本（本文件 §5）

## 1. 指标合同的关键冻结选择与理由（详见 JSON 字段级定义）

| 决策点 | 冻结值 | 理由 / 被拒绝的替代 |
|---|---|---|
| Fpast（既往前沿） | **B_{t−1} 边界 snapshot 的 decision front**（三元组级严格 ND） | G1/G3 与 t_div 全在 decision-front 指标上，单一对象防口径混用；observed full front 会把已被选择淘汰的候选计入覆盖。observed 变体仅可作次要诊断，不得用于门控、运行后不得切换 |
| Wt | 窗口 t 内按评估 nominalFE 归属的全部新生成三元组（含被 PDDR 淘汰者）；PARENT_CARRYOVER（N_eval=0）不进入 | 来源贡献问题关乎"生成"，与后续选择结果无关 |
| 归属 | 三元组级 FIRST_ADMISSION_WITHIN_WINDOW，tie-break (nominalFE, actualFE, candidateId, source) | 同一三元组可被多指纹/多次评估达成（02-front-coverage §5.1 已登记）；指纹级计数会高估新覆盖；first-admission 确定且行序无关 |
| ND | fc6 corrected：raw 1e-12 去重 → raw 严格 ND（epsilon-strict 支配）→ 归一化（不 clamp） | 沿用既有 HV/ND 管线，不重建（plan §3.6） |
| HV 归一化锚 | **每窗口 ND(Fpast∪Wt)（全集，含来源 s）的 ideal/nadir**；两项差值同一锚 | 剔除 s 不移动归一化，差值在同一空间精确；跨窗口比较走 share 空间，跨 checkpoint 轨迹比较走 run 级 Reference Contract（见 divergence JSON） |
| HV 参考 | (1.1,1.1,1.1)，fc6.hypervolume（最小化语义：前沿越好 HV 越大；例 (1,1,1)→0.001、(0,0,0)→1.331） | 与 fc6/P8 及 02-front-coverage 登记口径一致 |
| ExclusiveND 语义 | **(a) 严格 ND 新点的归属划分**（各来源 share 之和=100%）。拒绝任务文本字面口径 (b)"不被其他来源新点支配"（允许本来源内部被支配点计入）：(b) 与分母 NND_all 不构成划分、share 可加和>100%、且来源生成大量内部被支配点时会虚增其 share，反向掩盖 G1 要检测的贡献缺失，非保守 | 取对门控更保守且分子分母自洽的读法 |
| 符号方向 | deficit = normal − hard；正值 = hard 更差 | 消解 plan §3.7 原文 "hard-normal deficit" 表述歧义 |
| 单位 | 2.0/10.0/1.0/10.0 均为 percentage points（百分数之差），非相对百分比 | plan §3.7/§3.8 原文单位 |
| 连续性 | **同一指标**连续 2 窗/checkpoint（不允许两窗各用不同指标拼凑） | 比 "(A OR B) AND 连续" 更严格的预冻结读法 |
| FE 对齐 | 窗口按 nominal 25k 切；行同时记录 nominalFE+actualFE；跨边界候选按各自评估 nominalFE 逐个归属；Fpast 取边界 snapshot 记录态 | 历史 100k 运行实证 nominal↔actual 偏离（100000↔96025），按 actual 切窗会错位 |
| 空集/退化 | 空窗→0；锚某目标 range<1e-12→分母 max(range,1e-12) 并记 degenerate，照常计入连续计数（不得事后剔除），列 pipeline anomaly | 预冻结确定性，杜绝事后择优 |
| 比较容差 | deficit ≥ threshold − 1e-9（仅吸收 1-ulp 浮点误差；自检中数学上恰为 10.0pp 的差值实测算得 9.999999999999998） | 不改阈值本身，消解边界抖动 |

## 2. matched-window 历史可比性判定（任务2）

可比性标准 C1–C6（同 instance / 同 arm A4 / 同 seed 20260901 / 同 budget 500k / 同冻结 observer schema（逐评估来源+三目标+指纹+nominalFE/actualFE）/ 同管线）全部满足才可用 P95；任一不满足即 `expectedMatched=false`，不得为用 P95 放宽标准。

**结论：`matchedWindowFluctuationAvailable=false`，启用 fallback。** 逐材料：

| 材料 | 判定 | 依据（一行） |
|---|---|---|
| M1 `V35-FC5-MIDHORIZON-250K/01-root-cause-analysis/remote-results/` | NOT_COMPARABLE | 12/12 run actualFE=**250000**（≠500k，contract V3_1）；原始 telemetry-pddr-full-ledger.csv **仅存远端**（本地 find 全树 0 个 *telemetry* 文件，input-artifact-hashes.tsv 只登记远端 sha），无法按本合同重算；seeds={20260901,02,03} 含 20260901（修正简报中"seed 20260902"的单一表述）；jar 身份本地不可核验 |
| M2 `V35-FC5-MIDHORIZON-DIAGNOSTICS/18-final-2k-20k-50k-gates/A4-50k-ON-final/telemetry-pddr-full-ledger.csv` | NOT_COMPARABLE | instance/arm/seed ✓（100_5_3_1 / A4 / 20260901）但 maxFEs=**50000**、actualFE=48269（≠500k）；ledger 为 **PDDR-pool 子集**（1553 行 ≪ ~48k 评估），objectives 列 **1375/1553 行=FINGERPRINT_ONLY**（无三目标），source 为生成机会标签（GLOBAL_OFFSPRING/PARENT/O1_O9/CRITICAL_INSERT…）而非冻结四类一级来源，**无 nominalFE 列**；jar 723D24ED…（≠冻结正式 jar 8dad8f40…） |
| M3 `V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1/04-remote-100k/sync/` | NOT_COMPARABLE | 逐评估账本 ✓（96025 行 source+三目标+指纹，25k checkpoint ✓）但 requestedMaxFE=**100000**、actualFE=**96025**（≠500k）；seeds=**20260919-21**（≠20260901）；arm=C0 跑在 GAPLSRC 诊断实验 jar（bbb9ccd6…）上；仅 observedFE 无 nominalFE；normal 侧实例 50_2_3_1 为 50-job，非 100-job NORMAL |

## 3. t_div 历史材料充足性（任务3b）

**结论：`historicalSufficiency.sufficient=false`，fallback 激活。** 逐项：

| 证据 | 一行结论 |
|---|---|
| H1 `V35-PFC5-PHASE0/fetched-remote/100_5_3_1/seed-{20260901..05}/{A2,A4}` | 500k（EXACT_MAX_FE，frozenJar 8dad8f40…）——历史 500k HARD 复现材料，但每 run 仅 10 个终态文件（front.csv 等），**无 checkpoints/ 目录、无逐 25k 前沿** |
| H2 `V35-PFC5-GAP-PROBE/04-v2-remote-500k-runs/sync/run-GAP500-*` | 500k 但仅终态 front.csv，无 checkpoints；normal 侧 50_2_3_1 为 50-job |
| H3 全库检索 | 不存在任何 **100_2_4_1 × (500k\|500000)** 路径（100-job NORMAL 候选实例无 500k A4 运行） |
| H4 P9/P21/P22/P23/P25E 500k 材料 | instanceSha256 均为 20_2_3_1（20-job，P9 与 P21 同 sha 47d32d48…）或外部算法对照（HMOPSO_QGS_F vs ZHANGBO_A4），非 100-job A4 HARD/NORMAL 成对 |
| H5 有 25k checkpoint 的历史材料 | 仅 FC5-250K（250k 预算、50k 间隔汇总粒度另计）与 V3 100k——预算均非 500k、无 NORMAL 500k 成对 |

冻结 t_div 阈值（fallback，即 plan §3.8 原文数值）：HV window progress deficit ≥ **1.0pp** AND IGD relative-improvement deficit ≥ **10pp**，连续 **2** 个 25k checkpoint 的最早点；lag 网格取两侧并集，任一侧/上一网格点缺失 → lag=false；t_div=NOT_REACHED 亦是登记结果。t_div 仅为**解释性 divergence 时间，非因果 onset**。

## 4. 冻结的关键值（速览）

```text
source-attribution-thresholds.json:
  matchedWindowFluctuationAvailable = false（M1/M2/M3 全部 NOT_COMPARABLE，p95=null）
  mode = FALLBACK
  whvgShareDeficitPp = 2.0
  exclusiveNdShareDeficitPp = 10.0
  consecutiveWindows = 2（同一指标、同一来源、网格相邻）
  epsilon = 1e-12（share 分母 max(HV_all, 1e-12)，与 fc6 EPS 一致）
  归属 = 三元组级 first-admission；ND = fc6 corrected；HV 锚 = 每窗口 ND(Fpast∪Wt)
  deficit = normal − hard（正值=hard 更差）；单位 = percentage points

performance-divergence-thresholds.json:
  historicalSufficiency.sufficient = false → mode = FALLBACK
  hvProgressDeficitPp = 1.0
  igdRelativeImprovementDeficitPp = 10.0
  consecutiveCheckpoints = 2
  归一化 = 各实例 Reference Contract PFref_terminal（run 级统一，不 clamp），
  IGD = fc6.igd（归一化空间欧氏，对归一化完整 PFref）
  thresholdComparisonEpsilon = 1e-9（两文件一致）
```

## 5. 可重算脚本

`docs/evidence/V35-SOURCE-ATTRIBUTION-500K/00-preregistration/threshold_recompute.py`（Python 3 标准库；HV/IGD/ND 原语直接 `import scripts/fc6_metrics.py`，不重建）：

```bash
python threshold_recompute.py --audit      # 从登记输入重算可比性/充足性判定，与冻结 JSON 逐项比对（只读）
python threshold_recompute.py --selftest   # 行序无关性（5 次随机打乱）+ HV/去重/归属/t_div 健全性
python threshold_recompute.py --windows --ledger <PhaseA_ledger.csv> --fpast <B_{t-1}_front.csv> --window t
python threshold_recompute.py --gate --ledger-normal ... --ledger-hard ... --fpast-normal t:path ... --window ...
python threshold_recompute.py --divergence --traj-hard <trajectory.csv> --traj-normal <trajectory.csv>
```

- `--windows/--gate` 输入列要求：ledger `source,nominalFE,actualFE,candidateId,Cmax,TEC,TWC`；front CSV 三列目标；trajectory CSV `nominalFE,hv,igd`（含 nominalFE=0 基线行）。
- 确定性：三元组按字典序排序后分组/归属（tie-break 链见 JSON），不依赖 CSV 行序；`--selftest` 以 5 次随机打乱验证逐位一致。
- 2026-08-31 实测：`--selftest` **PASS**（12 项断言）；`--audit` **PASS**（判定与冻结 JSON 10/10 MATCH，M1/M2/M3 证据字段全部复核）。

## 6. 可能影响后续门的发现（如实登记）

1. **nominalFE 逐行字段是硬性 schema 要求**：三份历史材料均无逐候选 nominalFE（M2/M3 只有 actualFE/observedFE）。Phase A observer schema（§4 冻结前）必须提供逐候选 nominalFE+actualFE 成对字段与 **B_0（初始种群后、窗口 1 前）snapshot**，否则窗口指标按本合同不可计算——应在 20k OFF/ON 等价门暴露，禁止事后以 actualFE 近似。
2. **与 observer-schema.md（Agent A 草案，2026-08-31 并行产出）的跨文件缺口**：该草案候选级字段只登记 `actualFE`，未见逐候选 `nominalFE` 与 B_0 snapshot 条目；且其账本按 25k 窗口 flush（窗口边界定义未在该文件出现）。按本合同，需在 schema 冻结前补两处：(a) 逐候选 nominalFE（预算计数器落账值）；(b) B_0 边界 snapshot（初始种群 decision front）。若不采纳，窗口指标按本合同不可计算，属 §4 schema 门失败路径——此为 **潜在 DO_NOT_RUN 触发点**，需在 schema 冻结前由负责方裁决（本 Agent 不修改他人产物）。
3. **PDDR-pool 型 ledger（M2 形态）不满足来源归因需求**：objectives=FINGERPRINT_ONLY、生成机会标签、仅池内候选。Phase A ledger 必须逐评估事件含三目标值。
2. **PDDR-pool 型 ledger（M2 形态）不满足来源归因需求**：objectives=FINGERPRINT_ONLY、生成机会标签、仅池内候选。Phase A ledger 必须逐评估事件含三目标值。
3. **500k 历史 HARD（PFC5-PHASE0 seed-20260901/A4）只有终态 front**：plan §5 的"每 25k phase-consistent snapshot"在 Phase A 是新增观测需求，无历史先例可复用轨迹。
4. 无任何发现触发 DO_NOT_RUN（本阶段判定均为"历史材料不可用→启用预注册 fallback"，属 plan §3.7/§3.8 明文路径）。
