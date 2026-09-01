# 250K_REPAIR_DECISION — V35-LOCAL-FE-PACING C2/C3 双候选 250k 确认 最终裁决

- 日期：2026-08-31
- 裁决（预登记 §8/任务书 §十 四选一）：**`NO_REPAIR_CANDIDATE`**
- 含义：C2、C3 均未通过 250k 确认门；按既行证伪条款（single-repair-family-decision §3，50k 预登记引用）**LOCAL_FE_PACING repair family 在 250k 确认阶段被否证**——不调参、不寻找第五个 betaMax、不换轴续命；无候选进入 500k 多实例确认。
- 执行：18/18 运行 COMPLETED 且验收通过（`16-remote-250k-runs/`，训练机 `zhangbo-v35-local-fe-pacing-250k-20260831`）；6/6 公平组有效（actualFE 极差 997<5000，三臂初群双 hash 一致）；正式Jar `8dad8f40…bad8b9` 磁盘零改动；PDDR/CFVF/Dual-Q/CA-TA/mixture 冻结语义零改动。

## 1. 主Agent独立复算（`MAIN_AGENT_INDEPENDENT_CHECK_250K.py`，PASSED）

以独立实现（自有 3-D 精确 HV 切片法、IGD、ND 过滤）从原始 run 文件重算：终态 HV/IGD、配对 delta 中位数（与管线差 ≤0.1pp，其中 IGD 完全一致至 4 位小数）、公平组与预算合法性、gate 判定——与管线 `17-250k-reference-and-metrics/` 全部一致。复算过程抓出并修正了独立脚本自身的两处 ΔIGD 符号笔误（管线方向约定正确：正=候选改善）。

## 2. 逐候选裁决（预登记 §8 门，3-seed 中位）

### C2（betaMax=0.45）— **FAIL（三重失败）**

| 门 | 结果 | 数值 |
|---|---|---|
| 正常实例安全门 | **FAIL** | 50_2_3_1 median ΔHV = **−3.19%** < −2%（ΔIGD +15.92% 达标但需两项同时达标） |
| 困难实例改善门 | **FAIL** | 100_5_3_1 median ΔHV = −6.43%、ΔIGD = **−21.99%**（无任何改善信号） |
| 单seed灾难门 | **FAIL** | 100_5_3_1 上 2/3 seed 同时 ΔHV<−5% 且 ΔIGD<−20%（seed17: −7.50%/−21.99%；seed18: −6.43%/−32.65%） |
| 三目标保护门 | PASS | 无目标出现 ≥2/3 seed 退化 >2% |
| 检查点一致性门 | PASS(MINOR_FLUCTUATION_ONLY) | 无达幅反转 |

50k 时 C2 的优势（ΔHV_hard +10.72%、ΔIGD_hard +25.13%）在 250k **完全反转**（−6.43%、−21.99%）——低剂量预算在更长预算线上对困难实例崩溃。

### C3（betaMax=0.35）— **四门 PASS，检查点一致性门 CONFLICT → FAIL**

| 门 | 结果 | 数值 |
|---|---|---|
| 正常实例安全门 | PASS | 50_2_3_1 median ΔHV = +0.19%、ΔIGD = +2.86% |
| 困难实例改善门 | PASS | 100_5_3_1 median ΔIGD = **+12.26%** ≥ +10%（ΔHV −1.43% ≥ −2%） |
| 单seed灾难门 | PASS | 仅 1/3 seed 灾难（100_5_3_1 seed18: −13.26%/−37.18%），不足 2/3 |
| 三目标保护门 | PASS | 无系统性退化 |
| 检查点一致性门 | **CONFLICT** | 50_2_3_1 上 ≥2 个有效检查点方向与终态相反且幅度超阈：ck100000 median ΔHV = **−6.87%**、ck150000 = **−5.15%**（均 >2%），各 3/3 seed 一致；终态仅 +0.19% |

按预登记 §8（任务书 §九）的机械规则：≥2 有效检查点 ✓、方向相反 ✓、HV 反转幅度 >2% ✓、≥2/3 seed 一致 ✓ → **真实预算敏感性冲突成立**。这不是 50k 那类不足 1% 的标量符号翻转（该类按规则只记 MINOR_FLUCTUATION）——C3 在正常实例上的终态平价是**最后阶段才出现的**： ck50000 至 ck150000 期间持续落后 C0 约 5–7% HV，只在终点窗口（C3 以 EXACT_MAX_FE 收尾于 250000，第 36 个外循环的最后窗口顶格运行）追平。终态获益、过程不获益，属机制性的阶段敏感，非预算量差异（检查点比较在同 FE 下进行）。

## 3. 裁决逻辑

1. C2 出局（安全门+改善门+灾难门三重失败）。
2. C3 出局（检查点一致性门 CONFLICT——按本包预登记的实质性阈值判定，该阈值正是为取代 50k 被勘误的"口径不对称淘汰"而设，本轮以完整前沿对完整前沿、同 FE、多 seed 一性地满足）。
3. 无候选通过 → `NO_REPAIR_CANDIDATE`。
4. 附加诚实记录：250k 逐 seed 方差显著（如 100_5_3_1 C3 三 seed HV 0.621/0.722/0.569；C2 50_2_3_1 seed18 ΔIGD −24.56%），50k 开发筛查的两个正信号均未在 250k 复现——开发级 2-seed 筛查（50k）的信号强度不足以预测确认级结果，这与本包以"确认实验"收束该 family 的设计一致。

## 4. 最终机器状态

```ini
250kDecision=NO_REPAIR_CANDIDATE
C2Pass=false(normalSafety+hardImprovement+singleSeedDisaster FAIL)
C3Pass=false(checkpointConsistency CONFLICT, 4 gates PASS)
PROVISIONAL_250K_REPAIR_CANDIDATE=false
repairFamily LOCAL_FE_PACING=REJECTED_AT_250K_CONFIRMATION
parameterSearchFollowup=PROHIBITED(falsification clause)
fifthBetaMaxSearch=PROHIBITED
500kStarted=false
DOEStarted=false
validationStarted=false
FinalCandidateApproved=false
FINAL_FROZEN=false
formalMatrixRunning=false
PDDRChanged=false
CFVFChanged=false
DualQChanged=false
CaTaChanged=false
mixtureChanged=false
formalJarChanged=false
checkpointObserverValidated=true
50kDecision=TWO_CANDIDATES_ADVANCE_TO_250K(unchanged, executed)
```

## 5. 移交与停止

- 本包到 250k 分析与裁决完成即停止；不启动 500k、DOE、Validation、Final Freeze 或正式矩阵。
- 若未来需要新的修复方向，属新工作包，须重新走杠杆审计与单一 repair family 决策流程；不得以调参续命方式复活本 family。
- 执行事件记录：首批 3 臂因预登记 CSV 转录截断（setupFileSha256 少一位）启动失败，属工程绑定错误而非科学输入变更（实例/快照/公式哈希与预登记一致）；修复 binding 后重试 1 次成功，全程 18/18 最终 COMPLETED（详见 `16-remote-250k-runs/REMOTE_250K_EXECUTION_REPORT.md`）。
