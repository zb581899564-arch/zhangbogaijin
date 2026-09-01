# CAL Repair Family 关闭裁决（REPAIR_FAMILY_NOT_PURSUED_STRUCTURAL_NO_LEVERAGE）

- 日期：2026-08-30
- 裁决：`PFC5-CAL=REPAIR_FAMILY_NOT_PURSUED_STRUCTURAL_NO_LEVERAGE`
- 性质：**纯 0-FE 文档裁决**。未修改算法、未删除任何创新、未消耗任何 FE、未上传任何文件、未运行 Race。
- 上游链：F1=`FAILURE_CLASS_REPRODUCED`（F1_DECISION.md）→ F2=`NOT_DEPLOYABLE_FIELDS_INSUFFICIENT`
  （CFVF 事件遥测缺失，阻断）→ `FC5=MECHANISM_UNRESOLVED` → 预登记
  `HYPOTHESIS_DRIVEN_TEACHER_EXPOSURE_CALIBRATION`（CAL_PREREGISTRATION.properties，
  `CAL_PREREGISTERED_NOT_IMPLEMENTED`）→ **本关闭裁决**。

## 1. 被关闭的对象（精确范围）

被关闭的是当前预登记的唯一注入点——

```text
Qg action-2 tournament exposure penalty（λ ∈ {0, 0.05, 0.15, 0.30}）
selectionScope = QG_ACTION2_TOURNAMENT_ONLY
```

**不是**：Teacher 集中假设本身、Dual-Q 机制、CFVF、CA-TA、PDDR、任何 A0–A4 冻结语义。

```ini
PFC5-CAL=REPAIR_FAMILY_NOT_PURSUED_STRUCTURAL_NO_LEVERAGE
calPreregistered=true
calImplemented=false
calUploaded=false
calStarted=false
raceStarted=false
teacherHypothesisRejected=false
dualQRejected=false
```

## 2. 为什么 Qp 与 Qg 缓存路径不可注入

（来源：`01-source-semantics/current-teacher-actions.csv`，逐方法行号审计；
`TEACHER_SELECTION_CALL_CHAIN.md`）

| 路径 | 源码事实 | 结论 |
|---|---|---|
| Qp KEEP | 当前 pbest 单解（build, L38-40） | 候选集基数=1，无重选空间 |
| Qp DIRECTIONAL | 个人档案 φ 的确定性 argmin（L64-74） | argmin 唯一固定身份，无选择自由度 |
| Qp EPSILON | ε-适应度确定性 argmin（L76-86） | 同上 |
| Qp COMPLEMENTARY | φ≤best+tol 子集内 min-cosine→max-spacing→fingerprint 确定性级联（L88-129） | 级联每级唯一，无自由度 |
| Qg action-0 PREVIOUS_CACHE | 每子群单例缓存 Map 读取（ZhangBoQgController.select L81） | 缓存基数=1 |
| Qg action-1 HISTORICAL_CACHE | 每子群单例历史缓存读取（L84） | 缓存基数=1 |
| Qg/Qp action 选择本身 | ε-greedy / mask 动作选择（L70/76、451/455） | 属 Q 语义，按主计划 §24 禁改 |

exposure-aware teacher identity selection 只能在"同一动作内、合法候选集 ≥2"的地方注入。
Qp 四个动作与 Qg 两个缓存动作的候选集全部为单例——**注入点在结构上不存在**。

## 3. 为什么 Qg action-2 锦标赛覆盖不足

50k ON 教师遥测（`100_5_3_1`/seed 20260901/A4，26,300 条 teacher 事件）离线尺度分析
（CAL_PREREGISTRATION_REPORT.md §4）：

```text
锦标赛路径占 Qg 选择   = 295 / 1200  = 24.58%（选择天花板）
占全部 teacher 事件    = 295 / 26300 = 1.12%（scopeCeilingOfAllTeacherSelections=0.01121673）
锦标赛路径自身分散度   = 295 次选择 / 237 个唯一教师，Hn = 0.985，top1 = 1.4%
真正集中的路径         = HISTORICAL top5 = 37.1%、PREVIOUS top5 = 27.9%（全部在不可注入的缓存路径上）
```

三重不足：

1. **覆盖率**：λ 旋钮最多触及 1.12% 的教师事件；预登记固定先验网格下 C3（35%–60%）
   数学不可达，C2 仅在 15%–24.58% 子区间可达（`targetC3Reachable=false`）。
2. **杠杆**：按预登记已否决的 `count/totalCount` 归一化实测，翻转一次中位差距需
   λ≈25.08，比最大提议值大 84 倍——四个配置将行为不可分辨。
3. **靶向错位**：唯一被集中的 HISTORICAL/PREVIOUS 缓存路径完全不受该旋钮控制。
   旋钮作用最力的地方恰好已经最分散（Hn=0.985）。

## 4. 为什么阴性 Race 不能证伪 Teacher 假设

Race 的推理结构是"λ 惩罚集中教师 → 若退化缓解则假设获得支持"。当前注入点下：

- 集中源（缓存路径，top5 合计 >60%）**不受处置**；
- 受处置的 1.12% 本身已接近最大熵，处置它几乎没有行为差异（§4.2 实测）；
- 因此预期的 Race 结局是**机械性的** `NO_IMPROVING_CONFIGURATION`（预登记
  `c0OnlySurvivorVerdict` 已预写该分支）——这一结局与"假设为假"和"旋钮没接上"
  两种解释**不可区分**。

按证据纪律，一个无法把"假设为假"与"处置无效"分开的阴性结果，不能作为对
Teacher 假设的检验。故 `teacherHypothesisRejected=false`、`dualQRejected=false`。

## 5. 为什么不值得运行 32 条 250k

- Race 规模 4 配置 × 4 实例 × 2 seed × 250k = **约 8M FE**（预登记
  estimated-compute-cost.md），是 F1+F2+F3 预算（≤1.5M）的 5 倍以上；
- 由 §3/§4，四个配置的轨迹差异预计落在噪声带内，Race 不可能产生
  决策相关信息；
- 全部先验证据指向机械性空集结局，而空集规则（主计划 §29）禁止放宽门、禁止换
  repair family 继续调——即花 8M FE 买一个预先可知的 `REPAIR_FAMILY=FAIL`；
- 唯一理性的动作是在实现前（0-FE）关闭该 repair family，把算力留给有杠杆的
  后续预登记。该决定与"先不崩，再谈平均性能"及"每次新计算必须回答
  Which preregistered gate authorizes this run?"的纪律一致：**没有任何 Gate
  能授权一个已知无杠杆的 8M FE 运行。**

## 6. 边界声明

```text
没有修改算法源码或冻结 Jar（javaSourceChanged=false）
没有删除 CFVF、CA-TA、Dual-Q、DSCR 或任何创新（它们保持 MANDATORY_FINAL_COMPONENT）
没有修改 PDDR（保持 GLOBAL_ORIGINAL）
没有消耗 FE（consumedFE=0）
没有上传/运行任何远端任务（calUploaded=false, raceStarted=false）
没有改变 F1/F2 裁决与 FC5=MECHANISM_UNRESOLVED 状态（FC5_HISTORICAL_CASE=OPEN）
```

## 7. 后续影响与待决

- `PFC5-RACE`、`PFC5-TOP2` 维持 `BLOCKED`（其前置 CAL 路线已关闭）。
- Teacher 集中假设仍是 `ROOT_CAUSE_CANDIDATE`（未证实也未否证）；FC5 机制保持
  UNRESOLVED。后续若重启该方向，需要一个新的、能触达缓存路径或以其它方式
  检验集中假设的预登记（例如缓存管理语义层面的单旋钮），且须用户单独批准；
  本裁决不预设该方向。
- 本关闭不打开 Gap Probe / Validation / Formal 中的任何一个。
