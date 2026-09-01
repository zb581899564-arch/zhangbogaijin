# FIRST_TIER_50K_ANALYSIS_REPORT — FC5-T 首档 50k 遥测分析

日期：2026-08-25
分析性质：只读、证据驱动、行为中性旁路遥测分析（`FC5_100JOB_TRANSFER_V1`）
数据来源：`/home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825/output/50k`
（已只读打包下载至本目录 `raw/`，tar SHA-256=`303b9fc316e0425f162deb7fbf723ec7d579ca32863a737f5239273cd94ed6be`，与远端一致）
预登记：`../00-preregistration/FC5_100JOB_TRANSFER_PREREGISTRATION.md`
状态：`FIRST_TIER_50K_ANALYSIS_COMPLETE — INSUFFICIENT_EVIDENCE`

---

## 0. 一句话结论

**当前 50k 证据不支持"候选池非支配点膨胀 → PDDR 压缩 → 四方向代表利用断裂"机制在 100-job 退化实例上出现迁移信号；在可观测的 50k 窗口内，候选池从未膨胀（全部 74 轮 Nnd<100）、四方向代表仅一次未保留（E_E 5/6=0.8333，未形成 ≥20pp 系统性差异）、archive–working 的 Cmax 差距 71/74 快照为 0（仅 3 个非零且极小），且历史确认的 100-job 退化在 50k 的 Cmax 轨迹上未复现（方向甚至相反）。由于单条 50k 运行只有一个 50k 窗口（判据 1），且 gap 快照数 = PDDR 轮数（A4 达 6 个）只能观察单个窗口（判据 4），无法形成预登记要求的"连续两个 50k 窗口"证据，故最终裁决为 `INSUFFICIENT_EVIDENCE`，不得据此宣布 FC5 迁移根因成立。**

## 1. 数据与哈希验收（不信任已有摘要，逐条复核）

- 运行数：24/24（A0_BASELINE×6、A2_CFVF×12、A4_BUDGET_AWARE_CATA×6），与 `telemetry-replay-registry.csv` 一致。
- 状态复核（`run-acceptance-recheck.csv`）：24/24 条 `status=COMPLETED`；`decoderCalls=fullEvaluations`（A0/A2=50000，A4=48269）；`illegalSolutions=0`；`duplicateEvaluations=0`。
- A4 的 48269 FE 为冻结算法的阶段一致尾停（`PHASE_CONSISTENT_BUDGET_TERMINATION` 语义），非 FE 漏计；报告保留该差异，未补评价。
- 配对初群（`pair-initial-population-check.csv`）：12/12 配对组同 instance×seed 两臂 `initial-population.sha256`（v35 段）一致。
- 逐运行证据反向验证（对每条运行 `evidence-sha256.tsv` 中每个文件重新计算 SHA-256 与字节数）：24/24 运行 0 失败（`run-acceptance-recheck.csv` 的 `reverseFailures=0`）。
- 遥测完整性：每运行均含 `fc5-transfer-merge-rounds.csv`、`-windowed-merge-overflow.csv`、`-directional-representative-lifecycle.csv`、`-archive-working-gap.csv`、`fc5-transfer-summary.properties`、`cmax-audit-curves.csv` 与 `cmax-audit-records.csv`；全部 `observerErrors=0`（观察旁路未干扰搜索）。

## 2. 字段口径（详见 FIELD_DICTIONARY.md）

- `Nmerge`：合并池物理候选数；`Nunique`：按 `(Cmax,TEC,TWC)` 三目标精确去重；`Nnd`：Nunique 的严格三目标 Pareto 非支配点数；`Roverflow=Nnd/100`。全部与 `V35Fc5TransferAudit.java` 源码逐行核对，未按列名猜测。
- 方向代表：`E_C/E_E/E_W` = 池内 argmin(Cmax/TEC/TWC)，`E_B` = G4 归一化 Chebyshev φ 最小代表；`pddrSelected/nextPopulationSlot/nextSemanticRole` 为真实 PDDR 结果；`qgTeacherUses/qpTeacherUses` 为真实教师使用计数；`improvedOffspringCount` 为真实评价后方向严格改善子代数。
- 解析注意：`fingerprint` 列含未加引号逗号，已按固定 6+变宽+20 布局解析；`evidence-sha256.tsv` 为制表符分隔。

## 3. 四组对照（正例/退化例身份按预登记固定，未按 50k 数据篡改）

| 比较 | 正例 | 退化例 | 历史 500k 身份依据 |
|---|---|---|---|
| A0→A2 | 100_2_5_1（seeds 20260911-13） | 100_8_3_1（seeds 20260911-13） | A2 最终候选确认 |
| A2→A4 | 100_2_4_1（seeds 20260901-03） | 100_5_3_1（seeds 20260901-03） | A2↔A4 多实例确认 |

50k 内 Cmax 性能轨迹（`performance-separation.csv`，`bestCmaxGlobal` 每 1000 FE 采样）：

| 比较 | 实例 | 50k 终点 rel(A→B) 逐 seed | 50k 内方向 |
|---|---|---|---|
| A0→A2 | 100_2_5_1（正） | −4.5% / −0.7% / −10.6% | 3/3 A2 更好 |
| A0→A2 | 100_8_3_1（退） | −0.2% / −3.1% / −1.9% | **3/3 A2 更好（历史退化未在 50k 复现）** |
| A2→A4 | 100_2_4_1（正） | −2.7% / −4.0% / +1.6% | 2/3 A4 更好 |
| A2→A4 | 100_5_3_1（退） | −4.0% / −1.0% / −3.5% | **3/3 A4 更好（历史退化未在 50k 复现）** |

注：历史"退化"判据基于 HV/IGD（前沿质量），本表仅报告 Cmax 轨迹；两者口径不同，不作为对历史裁决的否定，仅用于说明 **50k 区间内未观察到与历史 100-job 退化一致的方向性信号**。

## 4. Nnd / Roverflow 时序（`per-round-overflow.csv`、`per-run-overflow-summary.csv`）

- 全部 24 条运行、共 74 个真实 PDDR 轮：`Nnd ∈ [8, 77]`，**无任何一轮 Nnd>100**；`Roverflow ∈ [0.08, 0.77]`，全部 <1。
- 每运行 PDDR 轮数：A0/A2 = 2 轮（个别 A0 运行 = 3 轮；cycle1 约 FE 30-32k，cycle2 = 50k），A4 = 6 轮（约每 6.8k 一轮）。
- `Nmerge` 全部在 235-300（100 父代 + 200 全局后代左右的物理池），`Nunique` 在 188-271，严格 ND 占比约 10%-30%。
- 结论：**FC-5 假设中的"候选池非支配点膨胀（Nnd 超过 100 工作槽位）"在 50k 预算内从未发生**——所有轮次的非支配点数量都远低于槽位数。

## 5. 四方向代表生命周期（`directional-retention-summary.csv`）

- 代表记录共 296 条（= 74 轮 × 4 方向）。
- `pddrSelected` 与 `pool→next population` 保留率：**仅 100_5_3_1/seed20260901/A4 的 E_E 为 0.8333（5/6，1 次未保留），其余全部为 1.0000**——即**仅一次未保留（E_E 5/6=0.8333），未形成 ≥20pp 系统性差异**。
- 方向改善后代：`improvedOffspringRate`（产生过方向严格改善子代的代表比例）在 A0/A2 约 0-100%（轮次少、噪声大），A4 约 0.5-1.0；A4 的 QP 教师曝光极高（单运行 qpTeacherUses 1023-7614），QG 教师曝光 17-375；A0/A2 无 QP（冻结边界），QG 曝光 0-46。
- 教师→有效后代链路存在：A4 在正例与退化例上都有大量"代表→教师→方向改善子代"事件（如 100_5_3_1/seed20260903/A4 E_W cycle4 improved=664，来源 QP），说明教师利用没有断裂。
- `retiredAtCycle`（代表指纹在下一轮未被保留）：A0/A2 因只有 2 轮，cycle1 代表大多在 cycle2 退休；A4 代表在 cycle3-6 陆续退休——这是"每轮重选稳定代表"的正常指纹周转，**不是方向代表丢失**（下一轮仍重新选出同方向代表，且除 E_E 一次外其余全部 100% 保留）。

## 6. archive–working 利用断裂（`archive-working-gap-summary.csv`）

- `cmaxGap`（working−archive 最佳 Cmax）：74 个快照中 **71 个为 0**；仅 3 个非零且极小：100_2_5_1/A2/seed20260911 cycle1=3.9442、100_5_3_1/A4/seed20260901 cycle6=0.9411、100_8_3_1/A2/seed20260911 cycle2=4.2053。
- `tecGap/twcGap` 存在非零值（A4 更多，如 twcGap 最大 8355），但**无随轮次单调扩大趋势**，且与 Cmax 目标无关。
- 由于代表基本未丢失（仅一次未保留，§5），"差距随代表丢失扩大"的前提不成立；"差距先于性能退化"亦不可观测（50k 内性能退化未出现）。

## 7. H1 四项预注册判据逐条裁决（`h1-criterion-verdict.csv`）

| # | 判据 | 裁决 | 依据 |
|---|---|---|---|
| 1 | 退化实例 ≥2/3 seed 在性能分离前连续两个 50k 窗口 `Nnd>100` | **NOT_OBSERVABLE_AT_50K** | 每条 50k 运行恰好 1 个 50k 窗口（windowed CSV 每运行 1 行）；且 50k 内所有单轮 Nnd 均 <100，即使可观测也不满足 `Nnd>100` |
| 2 | 退化实例中位 Roverflow ≥ 正例 + 0.25 | **FAIL** | A0→A2：0.450 vs 0.375，Δ=0.075（不通过）；A2→A4：0.495 vs 0.240，Δ=0.255（边界通过，恰达 0.25）。仅 A2→A4 子块达标，A0→A2 子块不达标，故**联合 H1 门不成立**（整体仍 FAIL）；且绝对值 0.495 对应 Nnd≈50，远低于膨胀线 |
| 3 | ≥一种方向代表 pool→next 保留率低 ≥20pp | **FAIL** | 仅一次未保留（100_5_3_1/seed20260901/A4 的 E_E=5/6=0.8333），其余方向保留率均为 1.0000；该单次未保留未形成 ≥20pp 的系统性方向差（各方向 seed 中位差仍为 0.00）。无任何方向差 ≥0.20 |
| 4 | archive–working 差距随代表丢失扩大且先于性能退化 | **NOT_OBSERVABLE_AT_50K**（数据亦不支持） | 每运行 gap 快照数=PDDR 轮数（A0/A2=2-3、A4=6），仅覆盖单个 50k 窗口；代表基本未丢失（仅一次未保留）、cmaxGap 71/74 为 0、50k 内无性能退化 |

**只有 A2→A4 子块满足判据 2（Δ=0.255 边界通过），其余判据未获支持，联合 H1 门不成立**：判据 2 因 A0→A2 子块不通过而整体 FAIL；判据 3 未获支持（仅一次未保留，未形成 ≥20pp 系统性差异）；判据 1、4 因单个 50k 窗口不可观测。

## 8. 当前能够说什么、不能说什么

能够说（有 50k 证据支撑）：
1. 50k 内（74 轮 PDDR）候选池严格非支配点从未超过 100 槽位——FC-5 假设的"膨胀"未发生；
2. 50k 内四方向代表仅 1 次未保留（E_E 5/6=0.8333），其余全部被 PDDR 选中并进入下一代工作槽位——未形成"代表利用断裂"的系统性信号；
3. 50k 内 archive 与 working 的 Cmax 差距 71/74 快照为 0（仅 3 个非零且极小）——未形成"档案脱节"；
4. 50k 内 A0/A2、A2/A4 的历史 100-job 退化未在 Cmax 轨迹上复现（方向相反）；
5. 数据链路完整：24/24 运行、12/12 配对初群哈希一致、证据文件 SHA-256 反向验证全部通过。

不能说：
1. "FC5 迁移根因成立"（`FC5_TRANSFER_CONFIRMED` 明确不满足）；
2. "FC5 机制在任何预算下都不存在"——50k 只覆盖了历史 500k 确认中退化发生区间（如存在）之前的部分；判据 1/4 在 50k 不可观测，不能排除膨胀/断裂出现在 >50k 区间的可能性；
3. "机制与 100-job 退化无关"或"PDDR 无问题"——这不是本审计的结论，PDDR 的 `KEEP_GLOBAL_ORIGINAL` 裁决保持不变；
4. 任何关于 HV/IGD/正式统计的结论（本报告只分析遥测与 Cmax 轨迹，未重新构造 reference）。
5. 实例结构（机器/工人数）不构成任何因果解释。

## 9. 是否需要 100k

**不主动建议升级；不默认把 24 条全部升至 100k。**
预登记 §9 的升级条件是"50k 无法观察连续窗口（成立）**且**其他判据出现值得追踪的信号"。
判据 2 在 A2→A4 组出现边界信号（ΔRoverflow=0.255，恰达门槛）且判据 4 完全不可观测，构成"值得追踪"的最低条件；
但判据 3 未获支持（仅一次未保留，未形成 ≥20pp 系统性差异）、判据 2 的绝对值远低于膨胀线，故升级价值主要在于"使判据 1/4 可观测并定位历史退化起始 FE"，而非验证膨胀信号。
是否升级属用户决策；本分析不执行任何升级。

## 10. 最小升级清单（如用户批准，见 `recommended-next-budget.csv`）

| 项 | 值 |
|---|---|
| 比较块 | A2→A4 |
| 实例 | 100_5_3_1（唯一历史否决 + 唯一边界信号实例） |
| seed | 20260901, 20260902, 20260903（与首档一致，保持配对初群） |
| 臂 | A2_CFVF, A4_BUDGET_AWARE_CATA |
| 运行数 | 6（2 臂 × 3 seed），升至 100k |
| 解决的问题 | 判据 1（获得第二个 50k 窗口，使"连续两窗口 Nnd>100"可观测）；判据 4（gap 快照数近似翻倍：A0/A2 从 2-3 个、A4 从 6 个翻倍，代表周转与差距时序可查） |
| 明确边界 | 不依赖膨胀信号（50k 最大 Nnd=77）；不扩到 100_2_4_1/A0→A2 块；不改算法、PDDR、参数或冻结 Jar |

## 11. 冻结与边界声明

- PDDR 仍为 **`KEEP_GLOBAL_ORIGINAL`**，本分析未修改、未重放、未诊断性改动任何 PDDR 逻辑；
- 未修改 CFVF、Qp/双Q、CA-TA-Lite、FM3、DOE 参数、子群比例或局部搜索顺序；
- 未恢复 4500 正式矩阵，未启动 100k/250k/500k 重放；
- 未把 50k 结果写成正式论文优越性结论；
- 未重新包装 FC-6 已否决的 BP/Region-aware/顺序反转；
- 未因相关性宣称最终根因；本报告仅为 `ROOT_CAUSE_DIAGNOSTIC_ONLY` 证据。

## 最终裁决

```text
FC5_TRANSFER_STATUS = INSUFFICIENT_EVIDENCE
（50k 内无膨胀信号；四方向代表仅 1 次未保留（E_E 5/6=0.8333）；archive–working gap 仅 3/74 个快照非零且极小；判据 2 仅 A2→A4 子块边界通过（Δ=0.255）、A0→A2 子块不通过（Δ=0.075），联合 H1 门不成立；判据 3 未获支持；判据 1、4 因单个 50k 窗口不可观测）
PDDR_CURRENT_DECISION = KEEP_GLOBAL_ORIGINAL
NEXT_ALLOWED_ACTION = user decision on optional minimal A2_vs_A4/100_5_3_1 100k block; no automatic escalation
```

## 附：本目录产物清单

`FIELD_DICTIONARY.md`、`run-acceptance-recheck.csv`、`pair-initial-population-check.csv`、
`per-round-overflow.csv`、`per-run-overflow-summary.csv`、`directional-retention-summary.csv`、
`teacher-utilization-summary.csv`、`archive-working-gap-summary.csv`、`performance-separation.csv`、
`positive-negative-first-tier-contrast.csv`、`h1-criterion-verdict.csv`、
`recommended-next-budget.csv`、`analysis-payload.json`、`analyze_first_tier_50k.py`、
`raw/`（远端只读副本 + tar 包）、`evidence-sha256.tsv`（本目录全部产物的 SHA-256 清单）。

## 纠错记录（2026-08-25）

纠错原因：主 Agent 复核原始数据（`raw/output/50k`，24 条运行）后确认本报告与个别 CSV 存在统计笔误与表述错误。已逐项修正并保持其余内容不变；最终裁决（`INSUFFICIENT_EVIDENCE`、`KEEP_GLOBAL_ORIGINAL`）不变，本纠错不改变任何结论。

| # | 修正前 | 修正后 | 依据 |
|---|--------|--------|------|
| 1 | PDDR 总轮数 62 | 74 | `per-round-overflow.csv` 实为 74 行；24 条运行 `pddrRounds` 合计 74（A0=14、A2=24、A4=36；A0/A2 每运行 2 轮、个别 A0 运行 3 轮，A4 每运行 6 轮） |
| 2 | "296 条方向代表记录"但轮数表述为 62（不一致） | 296 条 = 74 轮 × 4 方向（一致） | `representativeRecords` 合计 296 = 74 × 4 |
| 3 | archive–working gap 快照 48 个、45 个为 0 | 74 个快照、71 个 cmaxGap=0、仅 3 个非零 | `archive-working-gap-summary.csv` 实为 74 行；非零为 100_2_5_1/20260911/A2_CFVF cycle1=3.9442、100_5_3_1/20260901/A4 cycle6=0.9411、100_8_3_1/20260911/A2_CFVF cycle2=4.2053 |
| 4/5 | "代表无一丢失 / 保留率全为 100%" | 仅一次未保留（100_5_3_1/20260901/A4 的 E_E=5/6=0.8333），未形成 ≥20pp 系统性差异 | 24 运行 × 4 方向中唯一 pool→next 保留率 <1.0 的记录 |
| 6 | 判据 2 被笼统指为不通过 | A2→A4 子块单独通过（0.495 vs 0.240，Δ=0.255≥0.25）；A0→A2 子块 Δ=0.075 不通过 → 联合 H1 门不成立（整体仍 FAIL） | `positive-negative-first-tier-contrast.csv` / `per-run-overflow-summary.csv` 中位 Roverflow |
| 7 | "没有任何一项判据获得支持" | 只有 A2→A4 子块满足判据 2（Δ=0.255 边界通过），其余判据未获支持，联合 H1 门不成立 | 同上 |

另修正：
- `h1-criterion-verdict.csv` 判据 4 备注原为 "only 2 PDDR rounds (<=2 gap snapshots)"，改为 "per-run archive-working snapshots = real PDDR rounds (2-6; A0/A2=2-3, A4=6)"；判据 2/3 备注补充 A2→A4 边界通过、唯一一次未保留（E_E 0.8333）。
- `FIELD_DICTIONARY.md` `pddrRounds` 一行由 "A0/A2=2、A4=6" 改为 "A0/A2 每运行 2 轮（个别 A0 运行 3 轮）、A4 每运行 6 轮（24 条合计 74 轮）"。
- `analyze_first_tier_50k.py`：修正判据 4 注释/备注（2 轮 → 2-6 轮）、判据 2/3 备注、section-8 注释与 `recommended-next-budget` 的 caveat（`criterion3_fully_negated` → `criterion3_not_supported (single 0.8333 outlier, no >=20pp gap)`）；修正 `evidence-sha256.tsv` 中 `analysis-payload.json` 重复/陈旧条目（先写 payload 再生成清单）；在 `totals` 中输出 `rounds/repRecords/gapSnapshots/nonZeroCmaxGap/nonRetained`。
- 复核后待厂商复算数字：74 轮、296 条记录、74 个 gap 快照（71 个为 0、3 个非零）、1 次未保留（E_E 5/6=0.8333）、判据 2 A2→A4 子块 Δ=0.255（边界通过）、A0→A2 子块 Δ=0.075（不通过），联合 H1 门不成立。

### 反向验证结果（evidence-sha256.tsv）

以下对 `evidence-sha256.tsv` 清单中每个文件重新计算 SHA-256 与字节数并与清单核对（`sha256\tbytes\tpath`）：

- 清单文件数：15（不含 `evidence-sha256.tsv` 自身，均为本目录产物）。
- 逐文件 SHA-256 + 字节数复算：**15 / 15 一致，失败 0**。
- 反向验证方法：每行读取 `path`，以 `hashlib.sha256` 重新计算，`bytes` 取 `os.path.getsize`，逐项与清单比对；任一不符即计为失败。
- 结论：本目录全部产物（含已纠错文件）的 SHA-256 与字节数与 `evidence-sha256.tsv` 完全一致，清单可信。

> 说明：`evidence-sha256.tsv` 自身不在清单内（自引用除外），`raw/` 为目录不参与清单，均符合原脚本清单语义。
