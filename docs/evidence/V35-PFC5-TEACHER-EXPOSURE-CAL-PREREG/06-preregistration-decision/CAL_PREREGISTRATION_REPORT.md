# V35-PFC5-CAL-P0 预登记报告

日期：2026-08-30
**最终状态：`CAL_PREREGISTERED_NOT_IMPLEMENTED`**
**0 FE。未改源码、未构建 Jar、未实现 selector、未上传、未运行任何实验。**

---

## 1. 结论

```ini
CAL_PREREGISTRATION=CAL_PREREGISTERED_NOT_IMPLEMENTED
calPreregistered=true
calImplemented=false
calUploaded=false
calStarted=false
raceStarted=false
algorithmChanged=false
formalMatrixRunning=false
consumedFE=0
```

单旋钮 `lambda`、三组配置 `C1/C2/C3`、四个 Development 实例、两个 seed、32 条 RunKey、Race 协议、统一 reference 契约、Robustness Gate、Rank Race、空集规则、实现边界与 15 项测试合同**全部在预登记阶段冻结**。

---

## 2. 冻结内容一览

| 项 | 冻结值 | 证据 |
|---|---|---|
| 唯一旋钮 | `lambda`，网格 `[0, 0.05, 0.15, 0.30]`，`FIXED_A_PRIORI_GRID` | `02-calibration-definition/` |
| 注入范围 | **Qg action 2 锦标赛**（Qp 与 Qg action 0/1 结构上不可注入） | `01-source-semantics/` |
| baseLoss | 确认「越小越好」；当前候选集合内 min-max 归一化，EPS=1e-12 | `TEACHER_EXPOSURE_FORMULA.md` §2 |
| exposure | `count_i / max(1, maxCount_in_controller)`，controller-local | 同上 §3 |
| 判据 | `adjustedLoss = baseLoss + λ × exposure`，取较小；平局用现有稳定指纹 | 同上 §4 |
| C0 门 | `if (lambda==0) return existingFrozenTeacherSelection(...)` | 同上 §5 |
| 实例 | `20_2_3_1` / `50_2_3_1` / `100_2_4_1`(normal) / `100_8_3_1`(hard)；`100_5_3_1` 排除 | `03-development-design/` |
| seed | `20260906`、`20260907`（池中最小两个合法 seed） | 同上 |
| Race | 4 × 4 × 2 @ 250k = 32 次，独立 JVM、同快照、同预算协议 | `05-run-protocol/` |
| 参考 | 每实例在 8 次全部完成后构造一次 `PFref_cal(instance)` | `reference-contract.md` |
| 淘汰门 | 100-job 实例 `median(deltaHV) < -0.10` 或 `median(deltaIGD) < -0.20` | `failure-and-stop-rules.properties` |
| 排名 | `Score = (MeanRank_HV + MeanRank_IGD) / 2`，越小越好 | 同上 |

---

## 3. 源码审计的三条决定性发现

1. **Qp 与 Qg action 0/1 结构上不可注入。**
   Qg action 0/1 各读一个单例缓存；Qp 的 KEEP / DIRECTIONAL / EPSILON / COMPLEMENTARY 各自通过确定性 argmin（带指纹破平）映射到**唯一**候选。**这些路径没有"在集合内改选哪个 teacher"的自由度**，按 §6.2 不得强行套入。Qp 不在范围内不是遗漏，是结构使然，已明文记录。

2. **比较器天然是 loss（越小越好），但两个分支量纲不可比。**
   边界子群用原始单目标（Cmax 跨度约 728，TWC 约 75079），G4 用 PDDR（跨度 < n）。不归一化则 λ 在各子群上杠杆相差两三个数量级，故**必须**在当前候选集合内做 min-max 归一化。由于 min-max 单调、且锦标赛两个选手同属该集合，λ=0 时归一化不改变任何比较结果 —— 这为 C0 等价提供结构性保证（另一重保证是 λ=0 直接走原路径）。

3. **注入必须落在成对比较上，不能改成全池 argmin。**
   锦标赛每轮恰好两次 `nextInt` 且只比较两个元素。改成 argmin 会改变随机抽取语义，直接违反 C0 门，且属「自创另一套搜索机制」。

---

## 4. 必须如实记录的杠杆限制（最重要的一节）

用既有 50k ON 教师遥测（`100_5_3_1` / seed 20260901 / A4，26,300 条）做离线尺度分析，得到三项限制：

### 4.1 结构天花板

```text
可改变的选择数上限 = QG 锦标赛占 QG 选择的比例 = 295 / 1200 = 24.58%
相对全部 teacher 使用事件                        = 295 / 26300 = 1.12%
```

**因此 §8 的 C3 名义区间（35%–60%）在数学上不可达。** C2（15%–35%）仅在 15%–24.58% 子区间内可达。

### 4.2 建议式 exposure 归一化没有杠杆

按 §6.2 建议式 `count / totalCount` 实测：

```text
exposure ∈ [0.0034, 0.0136]    max Δexposure = 0.0102
λ=0.30 → 最大惩罚差 0.0031     翻转一次中位差距比较需 > 0.255
→ 所需 λ ≈ 25.08（比最大提议值大 84 倍）
```

若照此实现，四个配置将**行为上不可分辨**，Race 会以**机械性的** `NO_IMPROVING_CONFIGURATION` 收场 —— 那不能证伪假设，只是旋钮没接上。

### 4.3 采用的替代归一化及其代价

改用 `count / maxCount`（仍为无量纲、`[0,1]`），λ=0.05/0.15/0.30 的最大惩罚差为 0.0375 / 0.1125 / 0.225，具备可用杠杆。但即便如此，**预计改变比例落在 C1 带内或更低**。

### 4.4 一个方向性的反直觉发现

遥测显示：**锦标赛路径本身已经高度分散**（295 次选择 / 237 个唯一教师，`Hn=0.985`，top1=1.4%）；真正集中的是**缓存路径**（HISTORICAL top5=37.1%、PREVIOUS top5=27.9%）。而缓存路径恰恰是结构上不可注入的。

即：**本旋钮唯一能够到的地方，正是暴露最不集中的地方。**

---

## 5. 需要用户决策的事项（实现前）

预登记已完成且不可回改，但基于第 4 节，建议在进入实现前确认路线。三个选项：

- **(i) 按本预登记实现并跑 Race。** 接受杠杆偏弱；若 2k 机制触发测试（第 12 项）不通过则应停止。若通过但 Race 结果为 C0 独胜，结论为 `NO_IMPROVING_CONFIGURATION` —— 但需注明该结论可能源于杠杆不足，而非假设为假。
- **(ii) 先解决杠杆问题再实现。** 例如改用「按 controller 内使用次数排序的分位暴露」等更强口径；这属于**设计变更，须重开预登记**，不得在实现阶段悄悄改。
- **(iii) 判定本 repair family 不值得 32 次运行**，直接记 `TEACHER_EXPOSURE_CALIBRATION=REPAIR_FAMILY_NOT_PURSUED`，转入其它候选方向。

按任务 §二十，**本工作包到此停止，不会自动进入实现阶段**。

---

## 6. 法律最终状态

```ini
CAL_PREREGISTERED_NOT_IMPLEMENTED
```

（另一个合法状态 `CAL_PREREGISTRATION_BLOCKED` 未触发：§6.2 的 `BLOCKED_SCORE_SEMANTICS` 针对的是「方向分数无法可靠归一化」，而本次审计确认分数方向明确、归一化可靠；限制出在 exposure 项的杠杆与可达范围，已在第 4 节如实记录而非隐瞒。）

---

## 7. 停止声明

```text
Teacher Exposure Calibration has NOT been implemented.
Configuration Race has NOT started.
No experiment was uploaded.
No FE was consumed.
Algorithm, PDDR, CFVF, Dual-Q actions/rewards and CA-TA were not changed.
```
