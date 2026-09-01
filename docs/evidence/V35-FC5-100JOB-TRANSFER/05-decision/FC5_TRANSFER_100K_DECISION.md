# FC5_TRANSFER_100K_DECISION — V35-FC5-T 第二档 100k 筛查裁决

日期：2026-08-25
维护：主 Agent（本文件为 05-decision 正式裁决；Luna A/B/C 均已提交证据并经主 Agent 审核）
状态：`FC5_TRANSFER_100K_INCONCLUSIVE`（情形 C）

---

## 0. 预登记身份（固定，未按结果篡改）

| 比较块 | 退化实例 | seeds | arms | 历史依据 |
|---|---|---|---|---|
| A2→A4 | 100_5_3_1 | 20260901, 20260902, 20260903 | A2_CFVF, A4_BUDGET_AWARE_CATA | 50k 首档唯一边界信号（判据2 ΔRoverflow=0.255）；500k 历史确认退化实例（ΔHV=-12.96%, ΔIGD=-76.31%） |

预算：MaxFEs=100000，population=100，共 6 条物理运行。
窗口：W1=[0,50000]；W2=[50000,actualFE]，A4 的 W2 标记 `PARTIAL_SECOND_WINDOW`（actualFE=96025）。100k 为独立预算实验，不与 50k 拼接。

## 1. 运行与验收（Luna B，主 Agent 已独立复核）

- 6/6 `status=COMPLETED`、`illegalSolutions=0`、`duplicateEvaluations=0`、`front` 非空有限、26 项证据文件齐备、逐运行 evidence-sha256.tsv 反向复核 0 失败 → 全部 **PASS**（`FIRST_TIER_100K_ACCEPTANCE.properties` status=ACCEPTED）。
- actualFE：A2 = 96680/96672/96653（remaining 3320/3328/3347 < 5000，阶段一致尾停，合法，未补评价）；A4 = 96025×3（remaining 3975 < 5000）。同 seed 配对 FE 差 628–655 < 5000。
- 同 seed 两臂：initialPopulationHashV35/P8、snapshotSha256、instance/setup/fatigue provenance 全部一致；初群哈希与 50k 首档一致（如 20260901=179a82a3…）。
- 启动硬门 7 项全部 PASS；2k 探针标记 `PROBE_ONLY_NOT_IN_RESULTS` 未入统计；旧 50k 目录只读未动。

## 2. 核心观测（Luna C 独立分析，主 Agent 独立复核数据一致）

| 项 | A2 | A4 | 说明 |
|---|---|---|---|
| W1 maxNnd / medianNnd | 59 / 49 | 65 / 38 | A4 W1 较低 |
| W2 maxNnd / medianNnd | 70 / 60 | **76 / 71** | 均 <90，**从未 Nnd>100** |
| W1→W2 中位 Roverflow | 0.49→0.60 | 0.38→**0.71** | 上升但 Roverflow<1 |
| 四方向 pool→next 保留率 | 100% | 100%（W1、W2 全部） | **无代表丢失** |
| archive-working cmaxGap | 全程 0 | 后半段转正 3.65–5.94（相对 Cmax≈700+，<1%） | 无扩大趋势（峰值后回落/波动） |
| 首次代表损失 FE | ≈64472–64495（末期） | 13491（早） | 代表指纹周转，非方向代表丢失 |
| 教师曝光（total） | 68→42 | **23806→3693** | 改善后代 A4 7588→1245；链路未断裂（W2 仍有大量曝光与改善） |

## 3. 情形判定（预登记第八节）

- **情形 A（STRONG_SIGNAL）五条件**：① ≥2/3 seed W2 Nnd>100 → **False**（maxNnd=76）；② Roverflow 上升 → True；③ 保留率降≥20pp → **False**（0）；④ gap 于代表损失后扩大 → True（但幅度<6）；⑤ 教师曝光或改善后代同步下降 → True（2/3 seed）→ **A 不成立**（①③缺失）。
- **情形 B（NOT_CONFIRMED）四条件**：maxNnd<90 → True；保留率≥95% → True；cmaxGap≈0 无扩大 → **False**（A4 存在非零转正 3.65–5.94，且判定不满足"无扩大"）；教师链路未断裂 → True → **B 不全成立**。
- **情形 C（INCONCLUSIVE）**：A 不成立、B 不全成立 → **命中**，且明确属于预登记列举的"Nnd 增加（Roverflow 0.38→0.71）但代表仍 100% 保留、教师仍正常利用"情形。

## 4. 主 Agent 最终裁决

```text
FC5_TRANSFER_100K_STATUS = FC5_TRANSFER_100K_INCONCLUSIVE
（100k 内候选池从未膨胀：全部 36 个 PDDR 轮 Nnd∈[8,76]，无任何轮 ≥90 或 >100；
 四方向代表 pool→next 保留率 100%，无被挤出；教师链路未断裂；
 A4 存在轻微 archive-working cmaxGap 转正（峰值 3.65–5.94）与 W2 教师曝光回落（23806→3693），
 但幅度不足以构成"利用断裂"系统性证据，也不满足情形 A 的 Nnd>100 与保留率下降条件；
 情形 B 因 cmaxGap 非零不全成立 → 判情形 C）
PDDR_CURRENT_DECISION = KEEP_GLOBAL_ORIGINAL
NEXT_ALLOWED_ACTION = user decision; NO automatic 250k escalation
```

**关键结论（证据所及）**：
1. "候选池非支配点膨胀（Nnd>100）→ PDDR 压缩 → 四方向代表利用断裂"机制在 100k 预算内**未出现**：Nnd 从未超 90，代表从未丢失，教师利用持续存在。
2. **Nnd 增加但代表仍被保留、教师仍正常利用 → "候选多"本身不是根因，禁止据此修改 PDDR**（预登记第八节特别警告，本数据正是该情形）。
3. A4 在 100k 后半段出现 cmaxGap 转正与教师曝光回落的未解释信号，规模小（<1% Cmax），不足以定性，构成"需要更多证据"的理由之一。

## 5. 250k 建议（不自动运行）

- 建议字段：`INCONCLUSIVE_SEE_MORE` —— 100k 无法区分"轻微 gap/曝光回落信号"与噪声；如需继续，最小方案需**独立预注册**（如 A2→A4 / 100_5_3_1 3-seed 250k 块或扩 seed），并由用户批准。
- **不自动升级**：本轮结束即停，等待用户决定。禁止自动启动 250k/500k、禁止恢复 4500 矩阵。

## 6. 冻结与边界声明（不变）

- PDDR=GLOBAL_ORIGINAL；FM3；ShiftMode=NONE；单族；序列无关 SUT；子群配比 20/40/20/20；LS=CA-TA-Lite→inherited LS；方向教师池 OFF。
- 未修改 CFVF/Qp/双Q/CA-TA-Lite/FM3/DOE/rho/P5G5/Pacing；未给 A2 补 Qp/双Q/CA-TA；未用重新随机初始种群；未恢复 4500 矩阵；未把本轮写成论文正式优越性结果。
- 本裁决为单实例（100_5_3_1）×3 seed 的诊断性筛查结论，非统计性证据，不构成对 CFVF/Qp/CA-TA/FM3 的负面结论，更不授权删改它们。

## 7. 证据链

- 执行与验收：`../03-transfer-telemetry/second-tier-100k/REMOTE_LAUNCH_AND_ACCEPTANCE.md`、`run-registry.csv`、`remote-artifact-map.csv`（远端产物 SHA 双向一致）
- 分析：`../04-positive-negative-contrast/second-tier-100k-analysis/`（FIELD_DICTIONARY、run-acceptance-recheck、per-round-overflow、windowed-overflow、directional-lifecycle、teacher-utilization、archive-working-gap、seed-paired-contrast、h1-100k-screening-verdict、SECOND_TIER_100K_ANALYSIS_REPORT、recommended-next-action、evidence-sha256.tsv 175 项反向验证 0 失败）
- 50k 首档纠错：`../04-positive-negative-contrast/first-tier-50k-analysis/`（Luna A 已完成，INSUFFICIENT_EVIDENCE / KEEP_GLOBAL_ORIGINAL）
