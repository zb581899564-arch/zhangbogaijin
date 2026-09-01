# Teacher Concentration Report — 教师垄断与利用率归一化

> 基于 `dscr-teacher-uses.csv` (ALL_QG_TEACHERS) 与 `directional lifecycle` 去重后 (DIRECTIONAL_REPRESENTATIVE_TEACHERS)，已对 (fingerprint,useCycle) 去重

## 1. 归一化口径

- exposures / PDDR cycle, exposures / 1000 FE, uniqueTeachers, Top1/Top5 share, Shannon entropy / normalizedEntropy, improvementEvents / exposure
- 数据见 `teacher-utilization-normalized.csv`，按 seed/arm/window 统计

## 2. 是否被少数解垄断？

- **ALL_QG_TEACHERS**：A4 W1 Top1 share 3-7%, W2 9-16% (seed20260901 W2 16.9%), Top5 14-60% (A4 W2 49-60%)。Normalized entropy 0.67-0.85，分布相对均匀，未垄断。
- **DIRECTIONAL_REPRESENTATIVE_TEACHERS**（去重后真实方向代表教师）：
  - A4 20260901 W1: 17 unique, Top1 73.8% (738%), Top5 97.9%, entropy 0.89/0.31 — **高度集中**，1个Cmax/W代表贡献了73%曝光
  - A4 20260901 W2: 14 unique, Top1 62.8%, entropy 1.06 — 仍集中
  - A4 20260903 W2: 15 unique, Top1 93.9%, entropy 0.34 — **极度集中**，单解垄断
  - A4 20260902 相对均匀：W1 Top1 20.8% entropy 2.21, W2 Top1 33.9% entropy 1.65

  **结论：方向代表教师存在局部垄断，尤其在W2。该现象与高频Qp使用同时出现，因果关系未验证（无单变量Qp因果证据），不能表述为“Qp放大垄断”。**

## 3. 利用率

- exposuresPerCycle (ALL_QG): A4 205/cycle (W1) vs 192/cycle (W2) — 基本持平，**未出现断裂**
- exposuresPer1000FE: A4 W1 28.8 vs W2 20.85 (A4) — W2因后期FE跨度小略降，但未断链
- directional exposuresPerCycle: A4 649 (W1) → 834 (W2) on 20260901 — 反而上升，说明代表仍在被高频使用

## 4. 改善事件

- directional lifecycle的 `improvedOffspringCount` 是**事件数**，非unique offspring。仅有lifetime总量，无window可分，故 `teacherToImprovementRate` 基于去重后：A4 W1 0.62-0.71 (15/24, 20/28), W2 0.46-0.76 (6/13,10/13)。**教师→改善转化率未崩**。

## 5. 重复计数风险已处理

同一fingerprint在多条历史Representative记录上同时累加的问题，已在correction脚本中用 `(fingerprint,useCycle)` 取max多重性去重。上表 uniqueTeachers 与 exposures 均为去重后下界估计。
