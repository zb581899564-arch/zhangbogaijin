# CHECKPOINT_OBSERVER_REPORT — 纯观察完整前沿检查点观察器（250k 包 §四）

- 日期：2026-08-31
- 结论：**观察器已真实接线并通过行为等价门**（`checkpointObserverValidated=true`，证据见 `../14-checkpoint-equivalence/`）；正式Jar磁盘字节零改动（SHA 复核贯穿全程）。

## 1. 实现方式：独立实验构建物 V2（正式Jar不动）

评估循环位于冻结正式Jar（`V35FairRunner` + `ZhangBoMOHPSOQ` 全部搜索机制类）。为在不修改正式Jar文件的前提下获得检查点时刻的前沿快照，构建 V2 实验Jar `jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-CHECKPOINT-V2.jar`（18 类，全部 major=52，SHA `c2cf4294…35758`），内容：

| 类 | 来源 | 改动 |
|---|---|---|
| `V35FairRunner`（同FQN影子副本） | 隔离冻结 tag `v35-final-doe1-frozen` 源码 | +2 行：运行开始 `attach(canonical, algorithm)`、完成前 `recordTerminal(result, passiveArchive)`（未武装时均为 no-op） |
| `V35PassiveEvaluationArchive`（同FQN影子副本） | 同上 | +1 行：`observe()` 主体结束后调用 `V35CheckpointObserverHook.afterObserve(this)`（主体原样移入 `observeCore`，逐字节语义不变） |
| `V35CheckpointObserverHook`（新） | 本包 | 纯观察钩子：冻结检查点/终端快照（目标值、观测值、overshoot、四类frontType的 目标+三目标+候选指纹 CSV） |
| `V35CheckpointRepairRunner`（新） | V1 Runner 源码改编 | CLI 增 `--observer OFF|ON`（默认OFF）与 `--checkpoints`；导出 `checkpoints/` 与 `checkpoint-fronts.csv`；OFF 模式回放 V1 语义 |
| profile 类 ×2 | V1 同源 | 零改动（自测 93/93 PASS） |

加载方式：classpath **V2:FORMAL**（V2 优先），冻结正式Jar文件本身逐字节不动（本包全程 SHA 复核）。

## 2. 接线点与冻结规则

- **被动 observed-full-front**：冻结Jar本就在每次成功评估后将其接纳进被动档案（`observe()`，`passiveObservedCount==fullEvaluations` 由运行门强制）。钩子在每次接纳完成后检查：首次 `observedCount >= target` 即冻结——由于逐次接纳，**`checkpointObservedFE == target`、`overshootFE = 0`**（远严格于"原子阶段末尾观察、overshoot<5000"的兜底要求）。
- **决策前沿**：`ZhangBoMOHPSOQ.getResult()`（=`globallyOptimalIndividual` 引用）在冻结瞬间读取目标值并立即复制（无 mutation）；`evaluationCounterFE` 列记录批内评估计数器领先量（<100，信息字段）。
- **terminal 两种 frontType**：`recordTerminal` 在运行完成时从 `result` 与被动档案冻结（终态决策前沿规模与 `front.csv` 行数一致、终态观测前沿与 `passive-archive.csv` 行数一致，烟雾与等价门均验证）。
- **candidateFingerprint**：`ZhangBoQgController.fingerprint(solution)` 的 SHA-256（与初群哈希同源的规范向量文本；取十六进制避免 CSV 逗号冲突）。纯计算、无随机数。

## 3. 观察器禁止事项的实现证明

- 不入搜索档案/不改PDDR输入/不改教师选择：钩子只读 `getResult()` 与档案快照，无任何写回 API。
- 不消耗随机数：指纹用 `MessageDigest`；快照用 `copy()`/目标读取；无 `Random` 调用。
- 不增加FE：无任何 `evaluate` 调用。
- 不改候选身份或排序：只读复制。
- **经验证明**：OFF/ON 等价门（20k/50k 两门 × C0/C2/C3）全部行为产物逐字节一致，包括全部事件流哈希、Q表哈希、机制计数、候选审计记录、被动档案与终态前沿（`../14-checkpoint-equivalence/behavior-equivalence.csv`，OFFvsON 126 行 0 DIFFER）。

## 4. 导出物（每条 ON 运行）

```
checkpoints/checkpoint-registry.csv        # targetFE, observedFE, overshootFE, counterFE, frontType, frontSize
checkpoints/checkpoint-<FE>-decision-front.csv          # candidateFingerprint,Cmax,TEC,TWC
checkpoints/checkpoint-<FE>-observed-full-front.csv     # 同上
checkpoint-fronts.csv                      # 合并表：checkpointTargetFE, checkpointObservedFE, overshootFE, frontType, candidateFingerprint, Cmax, TEC, TWC
```

frontType 四分列（checkpoint-decision-front / checkpoint-observed-full-front / terminal-decision-front / terminal-observed-full-front），下游 reference 与指标按 `../15-250k-preregistration/checkpoint-reference-contract.md` 严格隔离。

## 5. 本地烟雾（开发记录）

烟雾阶段发现并修复两个实现问题（此为门控前置自测的价值记录）：
1. **classpath 顺序**：V2 必须置于 FORMAL 之前，否则冻结影子类不生效（表现为检查点零行、终端空）——修正为 V2:FORMAL 并写入运行脚本注释。
2. **指纹逗号**：原始向量指纹含逗号破坏 CSV 解析 → 改为 SHA-256 十六进制。
修复后烟雾（50_2_3_1, seed 20260907, C0, 20k, 目标{5000,10000,15000}）：检查点精确冻结（overshoot=0）、终态 97/149 与 front.csv/passive-archive 行数一致、OFF/ON 行为产物逐字节一致。失败 attempt 以 `.partial-*` 保留（协议验证）。

## 6. 等价门结果（详见 14）

- 20k 门（50_2_3_1, seed 20260907, C0/C2/C3, MaxFEs=20000, OFF/ON，目标{5000,10000,15000}）：PASS。
- 50k 门（100_5_3_1, 同 seed, 同臂, MaxFEs=50000, OFF/ON，目标{12500,25000,37500}）：PASS。
- 等价门预算小于 250k，按同一代码路径使用等比目标值（代码无差、仅配置数值不同）——已在预登记与等价报告中登记。
- V2-OFF 对存储冻结运行忠实性：20k（对 `03-remote-20k/sync`）与 50k（对 `08-remote-50k/sync`）同配置逐掩码字节一致（含跨机器确定性再次确认；actualFE 15258/20000 与 48269/50000/49036 完全复现）。

```ini
checkpointObserverImplemented=true
checkpointObserverWired=true
observerOFFON_EquivalenceGate20k=PASSED
observerOFFON_EquivalenceGate50k=PASSED
V2OFF_FaithfulnessToFrozenRuns=PASSED
checkpointRowsGate=4/4(per run)
overshootFE=0(all freezes)
observerExecutionErrors=0(all runs)
formalJarModified=false
checkpointObserverValidated=true
250kReadyToRun=true
```
