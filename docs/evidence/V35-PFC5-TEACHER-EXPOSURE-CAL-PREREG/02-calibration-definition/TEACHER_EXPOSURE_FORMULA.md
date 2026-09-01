# Teacher Exposure 单旋钮数学定义

日期：2026-08-30　|　状态：预登记冻结，运行后不得修改
依据：`01-source-semantics/TEACHER_SELECTION_CALL_CHAIN.md` 的源码只读审计 + 既有遥测离线尺度分析

---

## 1. 可达范围（由源码结构直接推出，非选择）

| 选择面 | 候选集合基数 | 可否做集内改选 |
|---|---|---|
| Qg action 0（PREVIOUS_CACHE） | 1 | 否 |
| Qg action 1（HISTORICAL_CACHE） | 1 | 否 |
| **Qg action 2（GLOBAL_ARCHIVE_TOURNAMENT）** | **n（实测中位 147）** | **是** |
| Qp KEEP / DIRECTIONAL / EPSILON / COMPLEMENTARY | 各 1 | 否 |

按 §6.2「如果某Q动作不使用候选集合，不得强行套入」：

```ini
exposureAwareSelectionScope=QG_ACTION2_TOURNAMENT_ONLY
qpInScope=false
qgAction0InScope=false
qgAction1InScope=false
```

**Qp 明确不在范围内**，不是被遗漏，而是结构上不存在集内重选的自由度。

---

## 2. baseLoss：确认方向后再归一化

源码确认（`ZhangBoQgController.java:391-406`）：两个分支**都是越小越好**，即天然是 loss，无需符号转换。

| 子群 | 原始判据 | 量纲 | 实测跨度（500k 冻结 PFref） |
|---|---|---|---|
| G1_CMAX / G2_TEC / G3_TWC（边界） | 单目标原始值 | 物理量 | Cmax ≈ 728，TEC ≈ 18761，TWC ≈ 75079 |
| G4_BALANCED | `pddr = dominatedBy + 1/(dominates+1)` | 计数 | ≈ (0, n] |

**不归一化则 λ 在各子群上的实际杠杆相差两到三个数量级**，故必须在当前候选集合内归一化：

```text
rawLoss_i = compare 分支所用的原始标量（边界子群取该子群目标值，G4 取 pddr）

baseLoss_i = (rawLoss_i - min_rawLoss) / max(EPS, max_rawLoss - min_rawLoss)
            其中 min/max 取遍「本次比较所处的同一个候选集合」
            EPS = 1e-12（与 analyze_confirmation.py 的 EPS 一致）
```

**关键性质**：min-max 是严格单调变换，因此**在同一集合内的成对比较上，`baseLoss` 的比较结果与 `rawLoss` 完全一致**。又因锦标赛的两个选手同属 `pool ⊆ candidates`，故 λ=0 时归一化不改变任何一次比较结果 —— 这为 C0 精确等价提供了结构性保证（另一重保证是 λ=0 直接走原路径，见 §5）。

`rawLoss` 的 min/max 只依赖当前候选集合，**不读取未来信息、不读取未评价解**（LCS-02 已保证候选集合内全部已评价且目标有限）。

---

## 3. exposure：归一化口径的选择（由数据决定）

§6.2 建议式为：

```text
exposure_i = count_i / max(1, 当前controller全部teacher使用次数)
```

**实测（既有 50k ON 遥测，`telemetry-teacher-use-events.csv`，QG 锦标赛 295 次 / 237 个唯一教师）**：

```text
exposure_i ∈ [0.00338983, 0.01355932]
max |exposure_A − exposure_B| = 0.01016949
baseLoss 成对差距尺度（directionalRegret 中位） = 0.255070
λ=0.05 → 最大惩罚差 = 0.00051   （翻转中位差距需 > 0.255）
λ=0.30 → 最大惩罚差 = 0.00305   （同上）
翻转一次中位差距比较所需 λ ≈ 25.08
```

即：**按建议式，λ ∈ [0.05, 0.30] 的杠杆比所需值小约 84 倍，四个配置将行为上不可分辨**，Race 会以一个**机械性的** `NO_IMPROVING_CONFIGURATION` 收场——那不能证伪假设，只是旋钮没接上。

因此改用**同属无量纲、但杠杆可用的**分母：

```text
exposure_i = count_i / max(1, maxCount_in_controller)
             count_i         = 当前 controller 内该 teacher 累计被用作 teacher 的次数
             maxCount_...    = 同一 controller 内所有 teacher 的 count 最大值
             controller      = (qSystem=QG) × (ZhangBoSubSwarm)
```

两者对比（同一份实测数据）：

| 口径 | exposure 值域 | max Δexposure | λ=0.30 的最大惩罚差 | 能否翻转中位差距 |
|---|---|---|---|---|
| count / totalCount（建议式） | [0.0034, 0.0136] | 0.0102 | 0.0031 | 否（需 λ≈25） |
| **count / maxCount（采用）** | [0, 1]（实测 {0, .25, .5, .75, 1}） | 0.75 | 0.225 | 接近，可翻转中偏小差距 |

这是**归一化分母的替换，不是自创搜索机制**：比较判据、动作集合、候选集合、随机抽取次数、Q 表与奖励归属全部不变，只改动一个无量纲惩罚项的尺度。

---

## 4. 最终公式（冻结）

```text
adjustedLoss_i = baseLoss_i + lambda × exposure_i
选择：adjustedLoss 较小者
同分：沿用现有稳定指纹规则 fingerprint(left).compareTo(fingerprint(right))
```

约束（全部来自源码与 §6.2）：

```text
· baseLoss 的 min/max 只取当前候选集合，不跨时刻、不跨子群
· exposure 为 controller-local：(QG) × (子群) 各自独立计数
· Qp 与 Qg 绝不共享 exposure 计数器
· exposure 只由此前已真实发生的 teacher 使用事件更新（leader 确定之后递增）
· 不读取未来信息、不读取未评价解
· 不新增 teacher 候选、不删除合法候选
· 不改变动作合法掩码
· 惩罚不写回 Q 表，不改变 reward 归属
· previous / historical 缓存仍由 DSCR 按原规则清洗，清洗先于候选构造
```

---

## 5. C0 精确等价门（结构性 + 分支双重保证）

```java
if (lambda == 0) {
    return existingFrozenTeacherSelection(...);   // 原 compare()，零改动
} else {
    return exposureAwareSelection(...);           // adjustedLoss 成对比较
}
```

λ=0 分支**直接调用原有 `compare()`**，不做归一化、不加 0.0、不重排。因此 C0 与当前 A4 的下列逐项一致是**构造性保证**，而非"性能相近"：

```text
RNG consumption sequence   teacher identity sequence   Qg/Qp action sequence
Q table hashes             CFVF candidate sequence     PDDR selection sequence
CA-TA sequence             evaluation trace            actualFE
working population hash    canonical final front
```

---

## 6. λ 网格（固定 a priori，未调优）

```ini
lambdaGrid=0,0.05,0.15,0.30
gridLabel=FIXED_A_PRIORI_GRID
tunedOnResults=false
```

未声称经过性能调优。λ 取值在预登记阶段冻结，运行后不得修改。

**但必须同时记录三条由数据推出的限制**：

1. **结构天花板**：可改变的选择数上限 = QG 锦标赛占比 = **24.58%**（295/1200）；相对全部 teacher 使用事件仅 **1.12%**（295/26300）。
2. **因此 §8 的 C3 名义区间（35%–60%）不可达**。C1（5%–15%）可达，C2（15%–35%）仅在 15%–24.58% 子区间内可达。
3. **在 [0.05, 0.30] 上，预计改变比例落在 C1 带内或更低**（惩罚差 0.0375 / 0.1125 / 0.225 对比中位差距 0.255）。

故本 ladder 将被解释为 **「单调增强的分散压力」**，而不是名义的 C1/C2/C3 三段区间。若用户认为该杠杆不足以实现有效检验，应在实现前决定（见 `06-preregistration-decision/CAL_PREREGISTRATION_REPORT.md` 第 5 节），不得在运行后调 λ。
