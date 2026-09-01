# 04-teacher-analysis — Agent C 产出（数据级对照，无最终假设裁决）

生成脚本：`generate_04_teacher_concentration.py`（Python 3.11，只读输入，全部数字由脚本生成）。

## 数据源（绝对路径）

`E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR\16-remote-250k-runs\sync\seed-{20260916,20260917,20260918}\results\run-GAPL250K-{C0,C2,C3}-{50_2_3_1,100_5_3_1}-<seed>\`
的 `dscr-teacher-uses.csv`（18 run 合计 122,400 行教师选择事件）与 `dscr-events.csv`（DSCR 方向缓存事件）。

## 口径（冻结）

- **teacher 身份** = `teacherId` 字段（六向量状态串）的 SHA-256 全文。这是真实教师状态指纹，不使用 poolOrdinal/index%4/文件序号。注意该身份是"状态快照"身份：教师粒子状态随代更新而改变，`uniqueTeacherCount` 计的是唯一教师状态数，是集中度的保守（偏高）口径。
- **FE 窗口**（左开右闭，首窗含 FE=0）：(0,50000], (50000,100000], (100000,150000], (150000,200000], (200000, terminal]。terminal=本 run 实际最大事件 FE（C0/C2=244135/244941，C3=248816；< 250000 因外层周期粒度提前停）。
- top1Share/top5Share：窗口内被选最多（前 5）教师状态的选择次数占比；normalizedEntropy = Shannon/log2(uniqueTeacherCount)（唯一教师 ≤1 时记 0）。
- `replacementRate` = dscr-events 中 `stale==true` 行占比——**18/18 run 与 status.properties mechanismSummary 的 `dscr.replacements` 逐一核对完全一致**；`teacherUses` 行数亦与 `dscr.teacherUses`/`qgSelections` 18/18 一致（C0=6200、C2=6800、C3=7200）。

## 覆盖范围限制

- dscr-teacher-uses 仅覆盖 **Qg 作用域**（教师选择）事件；**Qp 作用域的教师暴露在 250k 冻结 Jar 中未导出**（CSV 内 `qpScopeCoverage=NOT_EXPORTED`）。Qp 侧集中度仅有 50k 单 seed 参考：`V35-FC5-MIDHORIZON-DIAGNOSTICS\23-a4-50k-terminal-validation\A4-50k-ON-s20260901\telemetry-teacher-concentration.csv`（ALL_QP top1Share=0.169044，normalizedEntropy=0.600304；ALL_QG top1Share=0.055833，normalizedEntropy=0.834665）。
- A 源无 per-teacher `offspringImproved` → "教师暴露→后代有效率"链在 250k 为 **NOT_EXPORTED**；50k 单 seed 可在 C 源 `telemetry-teacher-use-events.csv`（含 offspringImproved、directionalRegret、requesterRole）交叉核验。

## 正常 vs 困难实例对照（C0=正式语义臂，3 seed 中位，数据级观察）

| 窗口 | 100_5_3_1(困难) top1 | 50_2_3_1(正常) top1 | Δ | 困难 normEntropy | 正常 normEntropy | Δ |
|---|---|---|---|---|---|---|
| (0,50000] | 0.0691 | 0.0507 | +1.8pp | 0.8125 | 0.8295 | -0.017 |
| (50000,100000] | 0.1764 | 0.1410 | +3.5pp | 0.6947 | 0.7154 | -0.021 |
| (100000,150000] | 0.2098 | 0.2259 | -1.6pp | 0.6776 | 0.6435 | +0.034 |
| (150000,200000] | 0.1789 | 0.2032 | -2.4pp | 0.6402 | 0.6578 | -0.018 |
| (200000,terminal] | 0.2313 | 0.2137 | +1.8pp | 0.6288 | 0.6688 | -0.040 |

- 两实例集中度都随预算上升（top1 从 ~5% 升至 ~21-23%，normalizedEntropy 从 ~0.82 降至 ~0.63-0.67），且窗口内方向占比稳定在 G1_CMAX≈0.4、G4_BALANCED≈0.3、G2/G3 各 ≈0.15（见 CSV split 列）。
- 困难实例多数窗口 top1Share 略高、normalizedEntropy 略低，但**最大差约 +3.5pp / -0.04**，未达到预注册 H3 门的"top1Share 高 ≥20pp 或归一化熵低 ≥0.20"的幅度量级（此为数据级陈述，H3 是否成立由主 Agent 依全部门条件裁决）。
- dominatedTeacherRatio 全部为 0（与 dscr.teacherUses=dominatedTeacherUses=0, dtur=0 一致）。
- meanDirectionScore / meanDominanceAge / replacementRate 的逐窗口值见 CSV（dominanceAge 全程均值≈0，即缓存刷新事件在发现支配当轮即处理）。
