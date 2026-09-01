# Observer wall-clock 静态审计（V35 SOURCE-ATTRIBUTION-500K Phase A0）

状态：`AUDIT_COMPLETE`（0 FE 静态审计；源码=冻结树 `_isolated-v35-final-doe1-freeze-20260823`，逐行核对）
审计口径：每个机制回答两问——①是否读取真实墙钟（System.currentTimeMillis / System.nanoTime / new Random() 隐式墙钟种子 / Date/Instant）；②读取结果是否**影响搜索决策**（候选生成、动作选择、接受/拒绝、奖励、FE 分配、PDDR）。仅进入日志/遥测者标注 `RECORDING_ONLY`。

总判定先行：

```ini
wallClockInfluencesSearch = false   # 对冻结 A4 臂（及条件 A2 臂）成立
SCOPE_CAVEAT: legacy CA-TA（runCaTaLocalSearch，仅 CaTaLite 关闭时激活）的 v2Cost
              含 averageWallClockNanos 平局裁决——该路径 A4 不激活，见审计项4。
WALL_CLOCK_AUDIT_UNRESOLVED = false  # 无未决项，不触发 DO_NOT_RUN
```

全树扫描结论：`mypso` 算法树内 **零处** `System.currentTimeMillis`/`Date`/`Instant`；`System.nanoTime` 仅存在于 9 个文件且全部为遥测或 diagnostics（下文逐点列出）。唯一墙钟敏感点=legacy CA-TA credit 的第三字典序键（A4 不激活）。

## 逐项审计

### 1. CA-TA Test（seed 派生）
- 读取墙钟？**NO**。CA-TA-Lite：`attemptSeed = mixCaTaSeed(seed ^ generationNumber ^ parent.slot ^ decision.getEpoch() ^ decision.getCallOrdinal() ^ action.ordinal())`（ZhangBoMOHPSOQ.java **L5095-5097**）。`mixCaTaSeed`（**L5615-5621**）为纯 SplitMix64 位混合（xorshift×2 + 常量乘），输入全部是配置种子与计数器（generation/slot/epoch/callOrdinal/ordinal），**纯计数器，无墙钟**。legacy：`caTaRequestSeed`（**L5600-5613**）输入 = seed ^ DOMAIN_SEED ^ generation ^ slot ^ lineageId ^ context canonicalKey hash ^ contextEpoch ^ callOrdinal ^ id.number ^ repetition——同为纯计数器/常量。
- 影响搜索决策？不适用（无墙钟可影响）。候选生成本身由 `zhangBoCaTaRandom`（PseudoRandomGenerator，配置种子）驱动。
- 结论：**CLEAN**。

### 2. CA-TA Apply
- 读取墙钟？**YES（仅 diagnostics）**。`V35MacroCandidateGateway` 默认 NanoClock=`System.nanoTime()`（V35MacroCandidateGateway.java **L88**；调用点 **L117/L158-161/L169**），产出 `Attempt.elapsedNanos`；`ZhangBoNeighborhoodCandidateGateway` 同构（**L67-74**，result elapsed **L120/L145**）。
- 影响搜索决策？**NO**。elapsedNanos 唯一下游 = controller.record 的统计累计（见项4）；候选生成/评估/接受逻辑不读时间。生产代码从不调用注入口 `setCaTaNanoClock`（ZhangBoMOHPSOQ.java **L9523-9528**，注释明示 validation-only；Phase A 观察器不得调用）。
- 结论：**RECORDING_ONLY**。

### 3. Credit settlement（settleQp L3497 / settleOriginalQg L2891）
- 读取墙钟？**NO**。`settleOriginalQg`（**L2891-2939**）→ `ZhangBoQgController.settle/observeWithoutUpdate/settleWithScaledAlpha`（ZhangBoQgController.java **L131-150/L153-166/L175-199**）：reward = before/after 目标均值差的纯函数（`reward` **L206-224**），TD 更新 `old + alpha*(reward + gamma*max - old)`，无时间项。`settleQp`（**L3497-3531**）→ `ZhangBoQpController.settle`（ZhangBoQpController.java **L207-295**）：reward 分量 dominance/direction/archive/fatigue 全部由目标值、冻结 bounds、指纹派生；`firstEvaluationOrdinal` 为 FE 计数器非时间。
- 影响搜索决策？奖励影响 Q 表（搜索决策），但其计算**不含时间项**。
- 结论：**CLEAN**。

### 4. Action selection（v35CaTaLiteController / v35MacroCandidateGateway / legacy zhangBoCaTaController）
- 读取墙钟？**YES（legacy 路径）+ RECORDING_ONLY（A4 激活路径）**。
  - CA-TA-Lite（A4 激活）：`V35CaTaLiteController.decide`（V35CaTaLiteController.java **L87-150**）与打分 `best/compare/cost`（**L295-304/L306-321/L347-352**）只用 successes、average gain、**workUnits**（确定性：`parent.getNumberOfVariables()×(action.ordinal()+1)`，V35MacroCandidateGateway.java **L170**）、evaluations、calls、enum ordinal——`elapsedNanos` 虽被累计（record **L234**）但**从不参与打分**（源码注释 **L216**："Deterministic work units drive action selection; elapsed nanos are diagnostics only"）。topTwo/testShareExhausted/stagnation 全为计数器。→ **RECORDING_ONLY**。
  - **legacy CA-TA（A4 不激活）**：`ZhangBoCaTaController.decide`（ZhangBoCaTaController.java **L91-139**）→ `ZhangBoCaTaStatistics.best`（ZhangBoCaTaStatistics.java **L93-127**）：字典序 = successes → averageDirectionGain → **`v2Cost`（L175-178）= 0.5×averageWallClockNanos/(median+ε) + 0.5×averageEvaluations/(median+ε)** → calls → id。即第三平局键**读取真实墙钟并可能改变选中动作** → 该路径 `wallClockInfluencesSearch=true`。
- 影响搜索决策？A4 激活路径 **NO**；legacy 路径 **YES（条件性：仅当 successes 与 gain 双平局时生效）**。
- 结论：A4/条件A2 走 `v35CaTaLiteEnabled` 路径（formal 基线 L763-778 只调 `runV35CaTaLiteLocalSearch`；legacy `runCaTaLocalSearch` 仅在 `isCaTaEnabled && !isV35CaTaLiteEnabled`（L4742-4747，v2 非基线路径）可达）→ **对 Phase A 两臂 NEUTRAL；legacy 路径登记为范围外 caveat**。Phase A 不得运行任何关闭 CaTaLite 的 arm（当前无此授权，天然满足）。

### 5. local-FE pacing（beginLocalFeBudgetWindow）
- 读取墙钟？**NO**。`beginLocalFeBudgetWindow`（ZhangBoMOHPSOQ.java **L583-594**）：`progress = fullEvaluationCount / maxIterations`（纯 FE 计数器）；`V35LocalFeBudgetConfiguration.localBudgetFor`（L56-60）= 纯解析公式 `floor(β/(1-β)·B_G)`，β=βMin+(βMax-βMin)·u²，u 即 progress 计数器。`localFeHardLimit`（L597-600）同纯。全部调用点（L4978/L5094/L5272/L5355/L5397/L5442）只用计数器比较。
- 影响搜索决策？是（FE 分配），但输入无时间项。
- 结论：**CLEAN**。

### 6. accept/reject（ZhangBoLocalSearchAcceptance + recoveryGain）
- 读取墙钟？**NO**。`accepts`（ZhangBoLocalSearchAcceptance.java **L14-36**）= 逐子群目标严格小于比较（G4 为任一目标改进）；`qualityGain`（**L38-49**）= 归一化 φ 差 + clip（EPSILON=1e-12）；输入仅目标值与冻结 bounds。`ZhangBoNaturalRecoveryGate.allows` 与 `ZhangBoPressureBottleneckClassifier`/`ZhangBoCaTaPhase.fromProgress`（phase 进度输入 = fullEvaluationCount/maxIterations 计数器，ZhangBoMOHPSOQ.java L5077）均无时间输入（相关文件 grep 零命中）。
- 影响搜索决策？是（接受/拒绝），纯目标值函数。
- 结论：**CLEAN（目标值纯函数）**。

### 7. Reward（ZhangBoQpController / zhangBoQgController 内 TD/reward 更新）
- 读取墙钟？**NO**。与项3 同链：两个 controller 文件 grep `nano|currentTimeMillis|wallClock` 除 ZhangBoCaTaStatistics（legacy 项4）外零命中。CFVF 通道核心 `ZhangBoCfvfUpdater` 的 nanoTime（**L33-78** 等）仅包裹 `V35ModuleTimer.record` 遥测；更新逻辑用 `random.nextDouble()`（种子化 PseudoRandomGenerator）。`authorRandom()`（ZhangBoMOHPSOQ.java **L8709-8723**）：formal baseline 下按 `seed ^ 常量 ^ ordinal·φ` 确定性派生（ordinal 为调用计数器）；**caveat**：非 replayable 且非 formal-baseline 的遗留路径返回 `new Random()`（JDK 隐式墙钟种子）——A4 为 formal baseline，不触该分支。
- 影响搜索决策？是（TD 更新），无时间项。
- 结论：**CLEAN**。

## 附加遥测点登记（RECORDING_ONLY，防误判）
- `V35ModuleTimer`（主循环 L605/L638/L651/L666/L674/L681/L698/L705/L712/L717/L734/L743/L757/L764/L773/L780/L787/L801/L808 全部 nanoTime 对）→ 仅产 `v35ModulePerCycleLines`（L829-856）遥测行，从不回读搜索。
- `V35FairRunner`/`V35P25DComparativeEngine`/p25e 引擎的 nanoTime → 运行器/对比引擎计时，不在 A4 决策路径。
- `V35ModuleTimer` 等遥测在 OFF/ON 等价门中按掩码（algorithmRunNanos 等 NANOS_KEYS）处理，与 V3 口径一致。

## 总结论

```ini
# 七项逐一判定
caTaTest.seed            = CLEAN (纯计数器 SplitMix64, L5095-5097/L5600-5613/L5615-5621)
caTaApply.elapsedNanos   = RECORDING_ONLY (L158-161; 下游不参与打分)
creditSettlement         = CLEAN (Qg L206-224 / Qp L277-289 纯目标函数)
actionSelection          = CLEAN_FOR_A4 (CaTaLite L295-352 无时间; legacy v2Cost L175-178 含 nanos → 范围外 caveat)
localFePacing            = CLEAN (u=FE/MaxFEs 纯计数器, L583-594)
acceptReject             = CLEAN (纯目标值函数, L14-49)
rewardTD                 = CLEAN (无时间项)
wallClockInfluencesSearch = false   # 冻结 A4 臂与条件 A2 臂
WALL_CLOCK_AUDIT_UNRESOLVED = false
# 执行含义（计划§3.5）：HARD/NORMAL 无需单JVM/CPU-affinity 强隔离，可走普通并发；
# 但 20k OFF/ON 等价门建议仍同机串行以消除负载噪声（等价门口径已含 nanos 掩码）。
# 运行期禁令：不得调用 setCaTaNanoClock(L9523)；不得关闭 v35CaTaLite（会激活 legacy 墙钟信用路径）。
```
