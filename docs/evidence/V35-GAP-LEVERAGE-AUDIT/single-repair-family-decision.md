# 单一 Repair Family 裁决（SELECT_CATA_BUDGET_REPAIR）

- 日期：2026-08-30
- 裁决人：总控（三 Agent 并行取证 + 总控独立复核）
- 性质：0-FE 离线审计；只选择 repair family 与最小实验预登记，不实现、不运行

## 裁决

```ini
selectedRepairFamily=CATA_BUDGET
singleKnob=betaMax（局搜窗口份额上界；beta(u)=betaMin+(betaMax-betaMin)*u^2）
C0=0.65（当前冻结值，必须精确等价）
C1=0.55   C2=0.45   C3=0.35（同一语义轴：末端局搜窗口份额逐级压缩）
```

## 1. 三 Agent 证据 → 候选评估

| 候选 | 关键证据 | R1可达 | R2覆盖≥10% | R3方向一致 | 裁决 |
|---|---|---|---|---|---|
| H_CATA_BUDGET_COORDINATION | ①A4 结构化机制计数（outerCycles=62、qgSel=12400、cfvf=310000、qp=271800、formalLocalFE≈35%）在 50/100 实例**逐值相同**——调度对难度零自适应（Agent A §1，总控已独立复核）；②caTaLiteFE+formalLocalFE=189900 FE（37.98%）恒定，其中 u² 调度使 **62.3% 的局部预算落在 FE≥250000 之后**，而困难实例 best Cmax 在 129k–252k（25–50% 预算）后即停止改善，末端大窗 LS 接受率仅 **1.7–6.5%**（Agent C）；③A0 反事实：无 DYNAMIC_BETA 时 LS 吞 70–84% 预算——betaMax 是运行时已验证的预算阀门，且 `of(betaMin,betaMax)` 参数化 + `--local-fe-budget` CLI 通路现成（总控已核 V35FinalAblationProfile L26 与 Runner L248）；④250k 根因报告 §5 已将"CA-TA与inherited LS预算分配"列为下一优先方向 | **YES** | **37.98%（含 62.3% 尾段）** | **YES**（尾段 LS 死重 ↔ 覆盖停滞） | **SELECTED** |
| H_CFVF_QP_GUIDANCE | cfvfOffspring=62%、qpActions=54.4% 均为纯调度量且双实例相同；dtur=0 通道空；教师集中度确在 100-job 恶化（G1_CMAX top1 8.8%→21.5%/58.7%）但（Agent B）9 个注入点逐判：≥10% 覆盖率的旋钮全部位于冻结区（CFVF 变异率硬编码未暴露、Qp 动作分布属 Q 语义禁区、mixture 冻结、PA 不在引导链），唯一非禁区数值参数（COMPLEMENTARY qualityTolerance=0.15）的证据覆盖率不可证 | NO（无 ≥10% 非禁区旋钮） | — | 方向有支持（G1_CMAX 集中） | 排除（无可达杠杆；假设本身未被否证，登记为待观测） |
| H_CREDIT_TIMING | 无任何既有离线证据源能量化实例级奖励/更新时序差异；测量需新遥测工具版本（用户已禁止工具迭代），Q 奖励/状态冻结 | NO（离线不可测） | — | 未测 | 排除（证据不足） |
| NO_ACTIONABLE_LEVERAGE | 不适用——存在过 R1–R5 的候选 | — | — | — | 不选 |

## 2. 独立复核记录（总控）

- 亲测 `run-GAP500-A4-50_2_3_1-20260827` 与 `run-GAP500-A4-100_5_3_1-20260906` 的
  mechanismSummary：formalOuterCycles=62/62、qgSelections=12400/12400、
  qpActions=271800/271800、cfvfOffspring=310000/310000、formalLocalFE=175439/175380
  ——证实 Agent A 核心事实 1（调度实例盲）。
- 亲测 A0-100_5_3_1-20260906：formalOuterCycles=16、baselineUpdateEvents=80000、
  formalLocalFE=419900（84%）——证实反事实差异。
- 亲测 `V35FinalAblationProfile.java` L25-26（LOCAL_FE_BETA_MIN=0.25/MAX=0.65）、
  `V35LocalFeBudgetConfiguration` 头注（beta(u)=betaMin+(betaMax−betaMin)·u²，
  B_L=floor(β/(1−β)·B_G)）、`ZhangBoV35P25EBudgetDiagnosticRunner` L248
  `--local-fe-budget`——证实 Agent C 的注入点与 CLI 通路。

## 3. 方向性论证与诚实风险

- 论证：困难实例的 best Cmax 在 25–50% 预算后停止改善，而 u² 调度把 62.3% 的
  局部预算压进这段零改善尾段（末端窗 β→0.65 即 65% 给局搜），LS 尾段接受率
  1.7–6.5%——尾段局搜是死重，挤压了全局搜索（CFVF/Qg）的覆盖投入。
  压低 betaMax = 同一语义轴上逐级把尾段预算还给全局搜索。
- 风险（预登记时必须写明）：①50-job 上尾段 LS 仍有贡献（best 改善到 70–77%），
  压缩可能使正常实例轻微回退——50k 开发门包含正常实例不回退门；
  ②betaMax 改变将改变 canonical 配置哈希与随机流消费点之后的事件序列——
  这是 repair 的本体，不是等价性破坏；C0 必须证明与当前冻结 A4 逐位等价；
  ③FE/预算协议、PDDR、CFVF/双Q/CA-TA 语义、mixture、LS 顺序全部不动。
- 明确证伪：若 50k 开发门中 100_5_3_1 的 HV/IGD 相对 C0 无改善方向
  （median ΔHV≤0 且 ΔIGD≥0），或正常实例回退超过健壮性门，
  则 repair family 被证伪 → 记 REJECTED 并停止（不换轴、不调参续命）。

## 4. 最小实验预登记（只预登记，不实现不运行）

见 `proposed-development-run-registry.csv`：20k 机制门 8 条（C0–C3 × 2 实例 ×
seed 20260907；C0 必须与当前 A4 逐位等价）→ 50k 开发门 24 条（C0–C3 × 2 实例 ×
seed 20260907/20260914）→ 250k（32 条）仅在前两门通过后另行授权。
seed 规则：升序取未消耗 seed（20260827/20260906 已被 Gap Probe 消耗、
20260901 为 Failure Replay seed；20260907=CAL 池次小未用、20260914=全新），
快照按 V2 同款确定性物化并登记 SHA。

## 5. 排除路线确认

见 `historical-route-exclusion.csv`：BP-PDDR/ND-overflow/教师曝光 lambda/
Shift/压力 mask/A2 路线/Cheap-Test/LS 顺序/删创新/4500 矩阵——全部不得经
本 repair family 重新引入。
