# V35-PFC5-GAP-PROBE 预登记（第一阶段：0-FE 预登记 + 2k 工程贯通）

- 日期：2026-08-30
- 状态：`GAP_PREREGISTERED_NOT_STARTED`（12 条 500k 运行未启动，等待用户单独批准）
- 上游依据：`docs/V35_POST_FC5_EXECUTION_MASTER_PLAN.md` §31–32（Gap Probe：独立
  reference、三算法、2 实例 × 2 配对 seed × 500k、三档裁决、RED/NOT_RED）；
  `AGENTS.md` §21.7；外部基线 `NSGA_II_F/SPEA2_F=FAIR_READY` 与 20k 生产预检 PASSED
- 本阶段消耗FE：仅 2k 工程贯通 3×2000（ENGINEERING_SMOKE，不入任何 reference）

## 1. 参与算法（固定，详细绑定见 algorithm-selection-audit.csv / artifact-binding.tsv）

```text
A4  = Current A4-Pacing（冻结正式 Jar 8DAD8F40…，arm=A4_BUDGET_AWARE_CATA，
      经五臂快照绑定启动器 v35-formal-a0-a4-external-runner-v1 驱动；算法类全部
      运行时从冻结 Jar 加载，启动器为薄编译层，独立 Jar 2778e5b5…/f5de5272…）
A0  = HMOPSO-QGS-F（同一冻结 Jar 8DAD8F40…，arm=A0_BASELINE；
      源码 PublishedHmopsoQgs.java SHA 见绑定表）
SPEA2-F = 外部公平基线（比较 Jar 966DA3D2…，ZhangBoV35ExternalFairBaselineRunner；
      20k 生产预检 PASSED）
```

SPEA2-F 选定依据：P25E 5seed-50k 先导 `metrics-median.csv` 中 SPEA2_F
medianHV=0.9103 > NSGA_II_F 0.8969 且 medianIGD=0.0838 < 0.1000（两条件同时满足）。
该选择仅用于开发探针，不构成论文优越性结论；P25D/P25E 引擎历史限制照旧。

## 2. 实例（固定，禁止按性能择优）

```text
DEVELOPMENT 50-job = 50_2_3_1
  规则：Phase 0 instance-exposure-role-registry 中，取 job 数=50 且
  currentRole=CONTAMINATED_DEVELOPMENT 的实例按字典序最小者。
  候选 {50_2_3_1, 50_2_4_1, 50_2_5_1, 50_5_3_1, 50_5_4_1, 50_8_3_1} → 50_2_3_1。
hard（已登记困难实例）= 100_5_3_1（CASE_SELECTED_DIAGNOSTIC_ONLY，Gap Probe 是
  其角色允许的诊断用途；禁止另作他用）
禁止实例：一切 VALIDATION_RESERVED 实例。
```

## 3. 配对 seed（固定，规则先于结果）

规则：按数值升序取最小两个满足以下全部条件的 seed——
(i) 未出现在任何已执行运行记录（run 目录、status、metrics、run ledger）中；
(ii) 未用于 Failure Replay（排除 20260901）；(iii) 未用于任何 Validation；
(iv) 具备或可确定性物化两实例的显式四向量快照。
候选池 = 正式 manifest seeds（20260808..20260827，快照已物化）∪ CAL development
seed registry（20260906/20260907，CAL 关闭前从未实现、从未消耗）。

```text
seed1 = 20260827（正式 manifest 池最小干净 seed；全项目普查仅存在于快照物化清单）
seed2 = 20260906（CAL development 池最小干净 seed；快照由本工作包以
        canonical 工厂确定性物化并回读校验，工具与 SHA 见绑定表）
排除：20260808..20260826（P9/P25B/P25C/DOE1/P25D/工程冒烟等已消耗）、
20260901..20260905（A2/A4 确认+Failure Replay）、20260911..20260913（A0/A2 确认）。
```

同一 instance×seed 的三个算法读取同一份显式四向量快照（逐字节同一文件），
V35/P8 逻辑哈希见绑定表（20260827 快照逻辑哈希与正式 manifest 登记值交叉一致）。

## 4. 共同条件（冻结）

```ini
population=100
MaxFEs=500000
decoder=FM3
ShiftMode=NONE
familyMode=DEGENERATE_SINGLE_FAMILY
setupMode=SEQUENCE_INDEPENDENT
objectiveSlots=[0,1,6]
execution=independent JVM / independent Problem per run; sequential
budget=PHASE_CONSISTENT_BUDGET_TERMINATION（0 < actualFE=decoderCalls ≤ MaxFEs，
       remaining < 5000；禁止 partial Q phase 与补评价）
forbidden changes=A4/A0 算法语义、PDDR、CFVF、Dual-Q、CA-TA、子群比例 20/40/20/20、
       P5/G5、rho、LS 顺序、SPEA2-F 官方核与四向量算子
```

## 5. 阶段计划

```text
Phase 1（本阶段）：0-FE 预登记八件套 + 3 条 2k 工程贯通
  （50_2_3_1 × 20260827 × {A4, A0, SPEA2-F} 各 1 条；仅 ENGINEERING_SMOKE）
Phase 2（等待单独批准）：12 条 500k = 2 实例 × 2 seed × 3 算法，独立 JVM 顺序执行；
  预算约 12 × 500k FE（A4 约为基线数倍墙钟；SPEA2-F 最快）。
Phase 3：按 GAP_PROBE_REFERENCE_CONTRACT 构造逐实例 reference → 重算指标 →
  三档裁决 → RED/NOT_RED；不得自动进入 Validation 或 4500 矩阵。
```

## 6. 停止点

本预登记 + 2k 贯通完成后停止。12 条 500k 运行必须由用户在 run-registry.csv 之上
单独批准；不得自动上传或启动。
