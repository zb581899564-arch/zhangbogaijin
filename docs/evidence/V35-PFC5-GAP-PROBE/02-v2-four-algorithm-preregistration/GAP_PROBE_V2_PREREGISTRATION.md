# V35-GAP-PROBE-V2 预登记（四算法，D-110 主线）

- 日期：2026-08-30
- 状态：`V2_PREREGISTERED`（用户已授权：训练机 20k 机制贯通硬门通过后直接启动 16×500k）
- 治理依据：D-110、AGENTS §22、`docs/V35_COMPETITIVE_SUPERIORITY_EXECUTION_ROADMAP.md` v1.0
- 取代：`00-preregistration-v1-3alg-superseded/`（三算法计划废止，见 SUPERSEDED 标记）

## 1. 固定实验矩阵

```text
algorithms = A4-Pacing, A0(HMOPSO-QGS-F), SPEA2-F, NSGA-II-F
instances  = 50_2_3_1 (DEVELOPMENT 50-job), 100_5_3_1 (registered hard)
seeds      = 20260827, 20260906
population = 100          MaxFEs = 500000
runs       = 4 × 2 × 2 = 16 条 500k（另有 4 条 20k 机制贯通）
execution  = 训练机独立目录 /home/inspur/aicomp/zhangbo-v35-gap-probe-v2-20260830
             每条独立 JVM / 独立 Problem / 独立算法对象；Java 11；-Xmx4g；禁 GPU
公平组     = 同 instance×seed 的 4 算法；每 wave 2 个完整公平组（8 JVM 并行）；
             不得拆散公平组；wall-clock 仅诊断
```

## 2. 算法绑定（详见 algorithm-registry.csv / artifact-binding.tsv）

```text
A4        冻结正式 Jar 8DAD8F40…（arm=A4_BUDGET_AWARE_CATA），薄启动器
          gap-probe-arm-launcher-v2.jar c8fb7e00…（--release 8，major 52；
          仅 ZhangBoV35FormalAblationArmRunner；算法类运行时全部来自冻结 Jar）
A0        同一冻结 Jar（arm=A0_BASELINE）；源码 PublishedHmopsoQgs.java 84c31eaf…
SPEA2-F   外部比较 Jar 966DA3D2…（OfficialJMetal58SPEA2 06a9ea82…；OFFICIAL_JMETAL_CORE）
NSGA-II-F 同一外部比较 Jar（OfficialJMetal58NSGAII 64d78c6c…；OFFICIAL_JMETAL_CORE）
工具身份勘误：v1 绑定表的 bb9d1ce3…（首版合并 Jar）与 f5de5272…（v1 探针+启动器
合并 Jar）均已由 v2 干净分离的启动器取代；不得再混淆。
```

NSGA-II-F 与 SPEA2-F 并行作为 external；P25E 5seed-50k 指标（SPEA2 medianHV
0.9103 > NSGA 0.8969 等）仅作开发背景，不构成任何选择或结论。
"后续最强 external"在 Gap 完成后按预注册规则确定（§6）。

## 3. 实例与 seed（措辞修正）

```text
50_2_3_1   DEVELOPMENT（Phase0 注册表最小字典序 50-job CONTAMINATED_DEVELOPMENT）
100_5_3_1  registered hard instance（CASE_SELECTED_DIAGNOSTIC_ONLY，本探针为其
           允许的诊断用途）
20260827   snapshot/materialization registered；performance-unexposed；
           never used in a completed scientific run（正式 manifest 池）
20260906   previously preregistered for a closed CAL route；no FE consumed；
           no performance observed；deterministically materialized in this
           campaign（canonical 工厂 + 回读校验）
两 seed 均为 DEVELOPMENT 用途，禁止用于未来 Validation 或 Final Test。
同一 instance×seed 四算法读取同一份显式四向量快照（逐字节同一文件）。
```

## 4. 共同条件与预算门（修正）

```ini
decoder=FM3  ShiftMode=NONE  familyMode=DEGENERATE_SINGLE_FAMILY
setupMode=SEQUENCE_INDEPENDENT  objectiveSlots=[0,1,6]
budget=PHASE_CONSISTENT_BUDGET_TERMINATION（统一 MaxFEs=500000 上界）
A0/A4:  0 < actualFE=decoderCalls <= MaxFEs；remainingFE=MaxFEs-actualFE；
        0 <= remainingFE < 5000；terminationKind = EXACT_MAX_FE
        或 PHASE_CONSISTENT_TAIL_STOP（两种均合法）
外部:    通常精确闭合 500000；公平门同样使用 MaxFEs 上界
公平组:  同 instance×seed 四臂必须 same snapshot / same initialPopulationHashV35 /
         same initialPopulationHashP8 / same instance-SUT-fatigue provenance /
         max(actualFE)-min(actualFE) < 5000
禁止修改: A4/A0 语义、PDDR、CFVF、Dual-Q、CA-TA、mixture、P5/G5、rho、LS 顺序、
         两个官方核与四向量算子
```

## 5. 20k 机制贯通门（训练机，4 条，50_2_3_1 × 20260827）

四臂同快照。A4：formalOuterCycles>0、formalQgRounds>0、qgSelections>0、
pddrEvents>0、cfvfOffspring>0、qpActions>0、dualQ 相位计数>0、
caTaLiteTest 或 caTaLiteApply>0，且 illegal/duplicate/cfvfRepairs/Shift 活动/
directionalTeacherPool/REGION_AWARE/BP_RESERVED/shadowEvaluations 全 0。
A0：formalOuterCycles>0、formalQgRounds>0、qgSelections>0、pddrEvents>0、
baselineUpdateEvents>0（或固定 O1–O9 路径触发），qpActions=dualQ=cfvfOffspring=
caTaLite=0。SPEA2-F/NSGA-II-F：各自官方机制事件全真（strength/rawFitness/archive/
environmentalSelection；tournament/ranking/crowding/replacement），零 V35 机制。
共同门：front 非空有限、FE 闭合、remainingFE<5000、同组 actualFE 跨度<5000、
初群哈希四臂相同、来源缺失=0、独立 JVM、输出原子化、清单反向匹配。
任一失败 ⇒ `V2_SHAKEDOWN_FAILED`，保留证据并停止，不得启动 500k。

## 6. Gap 裁决与后续选择规则（预注册）

```text
比较对: A4 vs A0、A4 vs SPEA2-F、A4 vs NSGA-II-F
gapHV   = (HV_B − HV_A4)/HV_B      gapIGD = (IGD_A4 − IGD_B)/IGD_B（A4 落后为正）
聚合    = 每 (实例, 竞争者) 对两个 seed 取中位数；带宽按 max(medGapHV, medGapIGD)：
          ≤5% → GAP_WITHIN_5；≤15% → GAP_5_TO_15；>15% → GAP_GT_15
RED     = 仅当同一竞争者、同一主指标（HV 或 IGD）在两个实例和两个 seed 全部
          显示 A4 落后 >15%；否则 NOT_RED；禁止称 GREEN
最强 external（后续开发用）= 按两个实例 × 两个 seed 的 HV 与 IGD 平均秩联合
          选择（每 (instance,seed) 内按指标排名，四臂取平均秩最小者）；
          并列依次按 HV 平均秩、IGD 平均秩、算法标签字母序破平；
          不得凭单实例或单目标选择
缺数据  = 任何一条 500k 运行缺失/INVALID ⇒ verdict=BLOCKED_REFERENCE_OR_RUNS
```

## 7. 停止点

16×500k 完成验收 + reference + Gap 裁决后停止。不得自动进入 leverage audit、
repair family 实现、DOE 迁移、Validation、Final Freeze 或正式矩阵。
