# GAP-PROBE-V2 最终裁决

- 日期：2026-08-30
- 上游：D-110、AGENTS §22、V2 预登记（本目录上一级
  `02-v2-four-algorithm-preregistration/`）、用户执行授权（含 20k 通过后直启 500k）

## 1. 执行结果

```text
20k 机制贯通：4/4 PASS（SHAKEDOWN_PASSED，A4/A0 机制全真实触发）
500k 运行：  16/16 COMPLETED 且 ACCEPTED
公平组：     4/4 有效（同快照、同 V35/P8 初群哈希、同 provenance、
             actualFE 跨度 2267/0/0/0 全部 < 5000）
reference：  两实例 PFref 各自从 8 条 ACCEPTED raw front 构造，
             顺序无关自检通过
Gap 裁决：   50_2_3_1 = GAP_WITHIN_5（三对全部）
             100_5_3_1 = GAP_5_TO_15（vs A0）、GAP_GT_15 ×2（vs 官方核）
             总带宽 = GAP_GT_15
RED：        false（50-job 上任何主指标落后 ≤4.0%，种子级稳定性条件不成立）
最强 external：SPEA2-F（score 2.25；A0 2.50；NSGA-II-F 2.75）
```

## 2. 科学读法（边界内）

- 冻结 A4（A4_LEGACY）在 50-job 开发实例上**全面领先** A0 与 NSGA-II-F、
  与 SPEA2-F 的 HV 差距仅 4.0% 且 IGD 领先 38.5%。
- 在登记困难实例 100_5_3_1 上，A4 与 A0 相对两个官方经典核
  （NSGA-II-F/SPEA2-F）在 Pareto 覆盖质量（HV/IGD）上**存在大量差距**
  （HV 落后 63–68%、IGD 落后 260–311%），但三目标极值并未全面崩塌
  （minCmax 接近全场最优；minTEC/minTWC 差距 ~3%）——
  崩塌集中于覆盖质量而非极值方向，与 250k 诊断的"覆盖收缩"机制候选一致。
- A0 在 100_5_3_1 上 seed 间剧烈波动（gapHV −130.9% ↔ +44.1%），
  提示该实例上的双峰/不稳定行为是跨算法现象。
- 本裁决是**开发方向输入**：按 D-110 主线，下一步是 0-FE leverage audit 与
  单一 repair family 选择，目标即缩小困难实例上的覆盖差距。

## 3. 停止点与状态

```ini
gapProbeStarted=true
gapProbe500kCompleted=true
gapProbeVerdict=GAP_GT_15
gapProbeRed=false
repairFamilySelected=false
v35RProvisional=false
validationStarted=false
FinalCandidateApproved=false
FINAL_FROZEN=false
formalMatrixRunning=false
algorithmChanged=false
PDDRChanged=false
CFVFChanged=false
DualQChanged=false
CaTaChanged=false
```

不得自动进入 leverage audit、repair family 实现、DOE 迁移、Validation、
Final Freeze 或正式矩阵；这些均需用户新的明确决策。

## 4. 过程诚实记录（重试链与工具事件）

- 50-job 外部臂：attempt 1（-Xmx4g，4 条全部 OOM）→ attempt 2（12g，OOM）
  → attempt 3（16g，成功）。失败 partial 全部保留于远端 `.partial-*`。
- 100-job A4/A0：attempt 1（4g）一次成功（EXACT_MAX_FE）。
- 100-job 外部臂：attempt 1（16g OOM）→ attempt 2（32g OOM）→
  attempt 3（56g 串行 OOM）→ attempt 4（100g，成功；首条单跑 + 其余三条
  串行脚本 `run-ext-seq2.sh`）。根因：外部适配器路径按评价保留解码级
  观测数据（100-job 每评价 ~40-70KB，500k 需 >56g 堆），搜索语义零改动。
- 本地一次 seed 普查正则少一位、一次注册表哈希尾部手写错误，
  均在产出前自查发现并以机器重算替换（见 v1/PHASE1_DECISION 与
  V2 预登记）。
