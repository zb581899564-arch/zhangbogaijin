# FC-TIME-2 语义等价优化验收报告（2026-08-18）

> 阶段纪律：A1 → 验收 → A2 → 验收 → A3，每步独立实施与验收；禁止一次多改。所有改动只消除重复计算，不改任何决策、随机序、FE、candidate 数量。
> 基准：20_2_3_1 / 50k / seed 20260822 / PACING / 本地串行 JDK17（与 FC-TIME-1B 同机同口径）。
> 等价验收：每步 diff front.csv（224 点非支配集）逐位一致 + V35Fc1ModuleTimerTest（profile ON/OFF front hash+FE 一致）2/2 通过 + 全计数器（除新增指纹计数外）逐位一致。

## 总效果

| 版本 | 墙钟 runNanos | CFVF 总 | Og.Dscr | front | 等价门 |
|---|---:|---:|---:|---|---|
| FC-TIME-1B 基准 | 19.21s | 12.94s | 8.78s | — | — |
| +A1 fingerprint hoist | **9.97s（−48.1%）** | 4.51s | **0.57s（−93.6%）** | 逐位一致 ✓ | 2/2 ✓ |
| +A2 lightweight snapshot | 9.87s（−48.6%） | 4.33s | 0.51s | 逐位一致 ✓ | 2/2 ✓ |
| +A3 G4 range precompute | 10.00s | 4.34s | 0.52s | 逐位一致 ✓ | 2/2 ✓ |

## A1：Fingerprint Hoisting（主收益）

- **改动**：`applyV35Dscr` 过滤循环中 `fingerprint(solution)`（四向量 toString + machine copy 的纯函数）原本在**每个 (solution, teacher) 对内重建**；改为每个 solution 计算一次、teacher 循环内复用。
- **计数证据**：`fingerprintCallsBeforeEquivalent=5,299,454` → `fingerprintCallsActual=36,019`（−99.3%）；`fingerprintReuseCount=5,299,454`；`dominatesCalls` 不变（5,479,066，支配检查次数完全一致——逻辑没变，只是字符串构建少了）。
- **安全前提**：fingerprint 是纯函数（JS/FA/MA/WA 四向量），candidates 在本 Q round 内冻结（prepareOriginalQg 的 archive 拷贝层保证），teacher 侧指纹已在 snapshot 构建时缓存。
- **收益**：DSCR 29.3ms/call → 1.9ms/call；墙钟 −48%。

## A2：Lightweight Frozen Social Snapshot

- **改动**：`V35SocialKnowledgeSnapshot` 去掉两层冗余整解深拷贝——`fromEvaluatedSolutions` 的 `byFingerprint.put(fingerprint, copy(solution))` 改存引用；构造函数对 evaluatedSolutions 的再次深拷贝改浅拷贝 map。`solutionFor` 的 copy 保留（消费者 sanitizeOne 立即 copy 入缓存，安全阀）。
- **安全前提**（代码审查证据）：candidates 由 prepareOriginalQg 从 archive 拷贝后冻结；applyV35Dscr 只过滤不写原解；`solutionFor` 唯一消费者 `sanitizeOne`（ZhangBoQgController:362）对返回值立即 `copy(replacement)`，从不修改 snapshot 内对象。
- **收益**：DSCR 1.88→1.69ms/call（−10%）；消除每 Q round 2×A 次整解深拷贝。

## A3：G4 directionScore 范围预计算

- **改动**：`V35DscrSanitizer.sanitize` 的 G4 排序比较器原本对**每个 dominator 各扫一遍 snapshot** 求 min/max（O(T) per compare → O(T²)）；改为排序前一次扫描（相同迭代顺序 → 数值逐位相同 → 排序结果不变），比较器用 O(1) 重载。原 `directionScore(role, objectives, snapshot)` 保留（V35DscrTeacherCache:156 仍用它，次数少）。
- **收益**：50k 下 dominators 数量少，收益在噪声级（如实登记）；属正确降阶，无风险保留。

## 行为等价性全套证据

1. front.csv：A1/A2/A3 每步与 1B 基准 `diff` 逐位一致（224 点，Cmax/TEC/TWC 全同）。
2. `V35Fc1ModuleTimerTest`：profile ON/OFF 同 seed 同 FE front hash + FE 一致，2/2 通过（A1/A2/A3 后各跑一次）。
3. 计数器：`dscrCalls=300`、`dominatesCalls=5,479,066`、`archiveItemsVisited=216,114`、`pddrCalls=756`、粒子/子组/动作/合法性/冲突计数全部与基准逐位一致（diff 验证）——除新增 fingerprint 计数外无一变化。
4. 随机序：无任何随机调用改动（仅纯函数提前与复用）。

## 后续

- 50k 已收口（−48%）；500k 正式计时（FC-TIME-0 同机三臂 R1/R2/R）待跑，预计 DSCR 放大效应（archive 更大）下收益更显著。
- 候选剩余项（未做，等裁决）：prepareOriginalQg 的 archive 深拷贝（冻结语义必要层，保留）；DSCR 降频（改变刷新时机属第 2 类，须质量门 A/B）；`fingerprint` 的 int hash 化（用户禁止，除非证明无碰撞）。

## 证据文件

- `fc-time2-a1/pacing-50k/`、`fc-time2-a2/pacing-50k/`、`fc-time2-a3/pacing-50k/`（front.csv + mechanism-summary.txt）
- 基准：`fc-time1b-audit/pacing-50k/`
- 代码：`ZhangBoMOHPSOQ.applyV35Dscr`（A1）、`V35SocialKnowledgeSnapshot`（A2）、`V35DscrSanitizer`（A3）