# V35 Stage2 A0--A4 Final Semantic Review

审计日期：2026-08-23  
审计性质：只读代码与既有证据复核；未修改算法、配置或历史证据，未启动正式实验。  
审计对象：`V35FinalAblationProfile.VERSION=v35-final-a0-a4-ablation-v1`。

## 结论先行

```text
FINAL_A0_A4_SEMANTICS=ACCEPTED
A0_HMOPSO_QGS_F_IDENTITY=ALIGNED
A4_FROZEN_V35_FINAL_IDENTITY=ALIGNED
AUTHOR_ACTUAL_IN_FORMAL_ARMS=false
DIRECTIONAL_TEACHER_POOL_IN_A0_A4=false
FORMAL_DEPLOYMENT_CONFIGURATION_BINDING=BLOCKED_PENDING_MASTER_MANIFEST
```

前四项是语义结论：当前 A0--A4 profile 与 Stage2 已批准的 Final 主线一致；没有发现把 `author_actual` 混入 A0、把方向教师池或 Shift 混入 A4、或把 A4 错写成“只增加 CA-TA”的问题。

最后一项是部署门，不是 A0/A4 的机制差异。旧
`V35-FORMAL-EXPERIMENTS/00_protocol/frozen-configs/*.properties` 仍明确是
`frozen=false`、`runtime.configuration.sha256=UNFROZEN` 的历史模板。因此它们不能作为正式运行时绑定的证据。Stage2 MASTER Runner 必须对每个运行写入本报告定义的 arm canonical text/hash、Table-9 contract、正式实例/疲劳/SUT 哈希，以及已物化的 `(instance, seed)` 初始种群 snapshot hash；否则该次运行应 fail-closed。

## 1. 全臂共同、不可变的正式边界

| 维度 | 当前真实约束 | 审计结论 |
|---|---|---|
| 问题与目标 | 规范 `ZhangBoCanonicalProductionProblem`；FM3；主目标槽 `[0,1,6]` = Cmax/TEC/TWC | 五臂共同 |
| 产品族与设置 | `DEGENERATE_SINGLE_FAMILY`；`SEQUENCE_INDEPENDENT` | 五臂共同；不声称 PF-SDST |
| 时间移动 | `ShiftMode=NONE`；生产配置不暴露 Shift builder | 五臂共同；历史 FCLS/FCRS 不进入本审计 |
| 物理子群 | `[G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC]=[20,40,20,20]`（population=100） | 正式 Final mixture |
| PDDR | `GLOBAL_ORIGINAL`；严格三目标评价后环境选择 | 五臂共同；`BP_RESERVED_LEGACY`、`REGION_AWARE` 均不允许 |
| 局部搜索顺序 | `CA-TA-Lite -> inherited LS` | 顺序被冻结。A0--A3 的 CA-TA 被关闭，因此实际只执行 inherited LS；A4 才先执行 CA-TA-Lite |
| 原基线骨架 | Table-9 contract：`r1/r2<=0.6`，FA/MA/WA crossover=`0.2/0.5/0.5`，mutation=`0.08/0.15/0.25`，`Q_Times=50`，`LS_Times=30`，`gamma=epsilon=0.8` | 由 `setV35Configuration` 原子绑定，非旧巨型作者更新器 |
| Qg / PDDR | 原 Qg 与评价后 PDDR-FF 一直保留 | 五臂共同；它们不是新增创新开关 |
| 教师池 / rho | `directionalTeacherPool=false`；`rho=0` | 五臂共同；A5/教师池不在 A0--A4 阶梯 |

注意：`V35ProductionConfiguration.Builder` 的通用兼容默认仍是
`BP_RESERVED_LEGACY`，但 Final profile 对每一臂都显式写入
`GLOBAL_ORIGINAL`；不得用通用 builder 的默认值推断 Final 运行语义。

## 2. 臂身份、增量与因果问题

| 臂 | 实际 Runner mode | 相对上一臂唯一批准增量 | 明确关闭项 | 要回答的因果问题 |
|---|---|---|---|---|
| A0 | `V35_BASELINE` | 无；规范、公平适配 HMOPSO-QGS-F | DSCR、CFVF、PA_i/Qp、双Q、CA-TA-Lite、动态 Local-FE、方向教师池 | 在共同 FM3/编码/预算下，规范的 HMOPSO-QGS 骨架能达到什么水平？ |
| A1 | `V35_QG1` | DSCR | CFVF、PA_i/Qp、双Q、CA-TA-Lite、动态 Local-FE、方向教师池 | 仅清理 Qg 实际社会教师缓存是否有贡献？ |
| A2 | `V35_A2` | CFVF 全向量认知--社会飞行 | PA_i/Qp、双Q、CA-TA-Lite、动态 Local-FE、方向教师池 | 用 CFVF 替代结构化基线更新后的贡献是什么？ |
| A3 | `V35_A3` | 容量 6 的 PA_i、Qp 与 P5/G5 硬冻结双Q | CA-TA-Lite、动态 Local-FE、方向教师池 | 谱系个人档案与认知领导、冻结双Q的联合贡献是什么？ |
| A4 | `V35_FULL_POOL_OFF` | CA-TA-Lite **和**动态 Local-FE Pacing | 方向教师池、Shift、PF-SDST、BP/Region PDDR | 第三创新的“预算感知 Test-and-Apply 局部搜索包”在 A3 之上的贡献是什么？ |

这是一条预注册的创新层级阶梯，不是任意 Boolean 组合。尤其 A4 的因果单元是
`BUDGET_AWARE_CATA_PACKAGE`：将其报告为“CA-TA 单独贡献”是不正确的。

## 3. A0 身份复核：规范 HMOPSO-QGS-F，而非 author_actual

### 已对齐的事实

1. A0 显式映射到 `V35FairRunner.Mode.V35_BASELINE`，并通过
   `ZhangBoGlobalSearchConfiguration.forV35` 选择
   `ParticleUpdateMode.PUBLISHED_BASELINE`、`GlobalLeaderMode.ORIGINAL_QG` 与
   `EnvironmentalSelectionMode.EVALUATED_PDDR`。
2. `ZhangBoMOHPSOQBuilder.setV35Configuration` 强制绑定
   `ZhangBoFormalHmopsoQgsConfiguration.table9()`，而不是调用原作者的
   `AUTHOR_UPDATE` 路径；正式配置同时固定 FM3、单族、序列无关设置和无 Shift。
3. `V35ProductionConfiguration` 明确拒绝 `ProductionDecodeMode.AUTHOR_DIAGNOSTIC`。
   因此 A0 不能静默退回 `author_actual` 解码或旧作者随机初始化。
4. A0 保留原 Qg、严格 PDDR、工厂间搜索与固定 O1--O9；2000 FE 机制预检实测
   `Qg=200`、`PDDR=1`、`baselineUpdate=500`、`inheritedLocalFE=1490`，且
   `CFVF=Qp=CA-TA=0`。

### A0 的退化/失败闭环

- 无 DSCR：Qg 使用原社会教师缓存，不实施清洗；这是 A0 的设计基线，不是故障。
- 无 CFVF：只走结构化 Fig.5/Fig.6 风格的规范基线更新。A2 以后该计数必须为零，
  否则表明混入两种全局更新。
- 无 PA_i/Qp/双Q：不得构造或继承 Qp archive/dual-Q 对象。
- 无 CA-TA-Lite：只允许 inherited inter-factory/O1--O9 局部搜索。
- 任何试图使用作者诊断解码、多产品族、序列相关设置、非 `NONE` Shift 或非全局 PDDR
  的运行，都不属于 A0，必须在 MASTER Runner 中拒绝。

## 4. A1--A4 的真实机制与退化行为

### A1 = A0 + DSCR

DSCR 在 Qg 选择前检查并清洗 Qg 实际 `previous/historical` 社会教师缓存；严格支配者存在时，
按子群方向替代，重复目标或互不支配解不替换。它不创建候选、不增加 FE、不更新 Q 表。

机制门：`teacherUses>0`、`validityChecks>0`、`dominatedTeacherUses=0`；DTUR 和 SCRR
必须从分子/分母日志重算。无严格支配者时缓存保留，这是合法 fallback，不是“DSCR 未触发”。

### A2 = A1 + CFVF

CFVF 在工件身份对齐的 JS/FA/MA/WA 四向量上生成全局后代。它**替代**而非叠加
`PUBLISHED_BASELINE` 更新；故 A2--A4 预期 `cfvfOffspring>0` 且
`baselineUpdateEvents=0`。资源修复是合法域保护，正式验收要求 `cfvfRepairs=0`。

### A3 = A2 + PA_i/Qp + P5/G5 双Q

A3 启用谱系个人档案、Qp 四动作和 10% FE 预热后的 P/G block hard-freeze：
`P=5`、`G=5`、`rho=0`。局部候选不得回写当轮 Qg/Qp 奖励，局部 FE 不推动 block 切换。
若某个 archive pbest 不再可解析，选择器回退到同一谱系 archive 的方向领导；掩码中不合法
动作不可被选中。预热阶段记录为 `dualQWarmup`，之后 P/G block 计数均应大于零。

### A4 = A3 + CA-TA-Lite + Dynamic Local-FE Pacing

A4 使用 24 个 `(subswarm role, bottleneck)` 上下文和 N1--N5 宏动作。正式诊断配置是
`fullMaskNoShadow`：不运行 shadow probe，也不声称严格压力分类已启用。每个全局后代先完成
唯一完整评价和 Q 奖励结算，再进入 CA-TA Test/Apply；局部候选最多完整评价一次，并与 inherited
LS 共同进入 PDDR。

动态预算为：

```text
beta(u) = 0.25 + (0.65 - 0.25) * u^2
B_L = floor(beta(u) / (1 - beta(u)) * B_G)
```

CA-TA-Lite 与 inherited LS 共享 `localFeHardLimit()`；不足完整 Q 批次或局部窗口时安全停止，
不生成半代。故 A4 必须按真实 Decoder calls 计费，不能强迫将小预算烟测填至名义上限。
在既有 2000 FE 预检中，A4 为 `1525` FE、`Test/Apply=25/15`，是安全停止的机制贯通证据，
不是同预算性能比较。

## 5. FE、候选来源与计数契约

| 项目 | 计入 FE | 不计入 FE / 必须保持 |
|---|---|---|
| 初始种群、全局 Q 后代、被接受的 local candidate 的唯一完整 FM3 decode | 是；唯一事实源是规范 decoder 成功调用数 | `successfulDecoderCalls == fullEvaluations` |
| Qg/Qp 的选择、奖励、Q 表更新，DSCR，PDDR，排序，archive 维护，preview/合法性判定 | 否 | 不得因观察或统计改变候选、随机流或 FE |
| CA-TA Test/Apply 与 inherited LS 的候选 | 候选真正完整评价时各计一次 | A4 两类局部搜索共享同一动态局部 FE 上限 |
| 内部 decoder 辅助/统计 | 否 | 无 Shift，因此 left/right shift 重算及耗时应为零 |

所有正式 arm 的硬门：`FE <= MaxFEs`、`decoderCalls=FE`、非法解/异常 repair/重复评价/来源丢失均为零，
前沿非空且三目标有限。A0--A3 的固定 `LS_Times=30` 是历史基线局部资源语义；A4 用动态窗口替换其
资源控制，因而不是“仅增加一个 CA-TA 调用”。

## 6. 随机性、公平初群与可重放边界

1. 每次 V35 run 在边界调用 `JMetalRandom.getInstance().setSeed(seed)`；张博全局、Qp、CA-TA
   分别从同一主 seed 或固定异或派生 seed 构造随机源。
2. 正式 Stage2 的公平性不依赖各 arm 自行 `createSolution()`：已接受的 Formal Manifest 物化了
   45×20=900 个 `(instance, seed)` 四向量初始种群 snapshot。每个 A0--A4 arm 必须读取同一个
   snapshot，并验证其 `initialPopulationSHA256`。
3. 同一 arm、同一 instance、同一 seed 必须在独立 JVM/独立 Problem 下重放；跨 arm 的动作轨迹
   **不应**相同，因为新增机制合法地消耗不同随机事件。公平条件是相同初群、问题、seed、FE 上限和
   共同机制边界，而非把不同算法的随机轨迹强行对齐。
4. 计时只作诊断，不能参与 CA-TA 动作选择；当前控制器以确定性 work units 作代价信用。

## 7. 预期机制计数（正式 run 的非零/为零门）

| 计数 | A0 | A1 | A2 | A3 | A4 |
|---|---:|---:|---:|---:|---:|
| `qgSelections`, `pddrEvents`, `formalQgRounds`, `fixedNeighborhoodEvents`, `formalLocalFE` | >0 | >0 | >0 | >0 | >0 |
| `baselineUpdateEvents` | >0 | >0 | 0 | 0 | 0 |
| `cfvfOffspring` | 0 | 0 | >0 | >0 | >0 |
| DSCR teacher uses / validity checks | 0 | >0 / >0 | >0 / >0 | >0 / >0 | >0 / >0 |
| DSCR dominated teacher uses | 0 | 0 | 0 | 0 | 0 |
| `qpActions`, `qpTransitions`, `archiveInsertions`, `dualQP`, `dualQG` | 0 | 0 | 0 | >0 | >0 |
| `caTaLiteTest`, `caTaLiteApply` | 0 | 0 | 0 | 0 | >0 |
| `directionalPoolRequests`, `directionalPoolFiltered`, shadow samples/evaluations | 0 | 0 | 0 | 0 | 0 |
| `cfvfRepairs` | 0 | 0 | 0 | 0 | 0 |

这些是机制贯通门，不是性能结论；某一臂的具体前沿大小、HV、IGD 或 Cmax 不应从本表推断。

## 8. 身份比对与 Stage2 放行条件

### A0 vs `HMOPSO-QGS-F`

**ALIGNED。** 名称 `HMOPSO-QGS-F` 在此仅表示规范、公平适配的基线：A0/
`V35_BASELINE`、Table-9 参数、FM3、共同四向量问题与严格真实 FE。它不表示、也不得替换为，
李明哲原始 `author_actual` 文件行为。

### A4 vs Frozen V35 FINAL

**ALIGNED（语义）。** A4/
`V35_FULL_POOL_OFF` 精确具有 Final Freeze 所列的：FM3、单族、序列无关 SUT、Shift NONE、
`GLOBAL_ORIGINAL`、`CA-TA-Lite -> inherited LS`、P5/G5/rho0、教师池关闭、20/40/20/20，
以及 A4-Pacing `beta=0.25->0.65`。

**尚不可将旧模板称为已部署冻结（阻断）。** 两个 `frozen-configs/*.properties` 文件仍含
`frozen=false` 和 `runtime.configuration.sha256=UNFROZEN`，因此未来 Master 运行必须覆盖/替换这些模板，
并将 `V35FinalAblationProfile.canonicalTextFor(...)` 与 SHA-256 逐 run 绑定。仅当该绑定、45 行实例
manifest、20 seeds 和 900 frozen snapshots 同时校验成功时，才可称 Stage2 的正式运行部署已冻结。

## 9. 已核验的证据范围与未作出的声明

已核验：profile 相邻差异、运行时 mode 映射、Table-9 绑定、FM3/no-shift/单族/序列无关问题门、
PDDR/order/teacher-pool 显式开关、2000 FE 机制贯通及其 FE/decoder 闭合。

未作声明：五臂性能优劣、统计显著性、论文最终优越性、PF-SDST 作用、压力诊断阈值作用、Shift 作用，
或任何尚未由 Stage2 Master campaign 写出的 500k 结果。

## 10. 审计锚点

本目录的 `evidence-sha256.tsv` 固定下列源码与既有证据的读取版本。对这些文件任一内容的后续修改，
都需要重新执行本语义审计；不得沿用本结论证明已变更的运行。

