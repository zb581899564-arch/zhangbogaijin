# Phase B1 真实源码二次核对与事实核验报告（01-source-audit）

核对日期：`2026-09-02`
审计对象：真实源码（Java-jMetal 5.8 树）
核对结论：**`PHASE_B1_SOURCE_MATCH=true`**（全部 8 项事实与 B0.5 设计完全吻合，无任何隐式差异）。

---

## 一、八项核心事实逐条核对（含真实路径与精确行号）

### 1. 当前四动作规范候选如何生成
- **源码位置**：`java-jmetal58/.../zhangbo/ZhangBoQpCandidateSelector.java:25-61`
- **事实核验**：
  - `sorted` 在第 33-35 行按 `fingerprint` 字典序稳定排序；
  - `directional` 在第 36 行由 `directional(sorted, group, bounds)` 得到（内部第 63-74 行按 `phi asc, fingerprint asc` 破平）；
  - `keep` 在第 37-38 行由 `find(sorted, selectedFingerprint) ?? directional` 得到；
  - 当 `sorted.size() > 1` 时，第 40 行设 `DIRECTIONAL = directional`，第 41 行由 `epsilon(sorted, bounds)` 得到 `EPSILON`（内部第 76-88 行按 `epsilonFitness asc, fingerprint asc` 破平），第 42-45 行由 `complementary(...)` 得到 `COMPLEMENTARY`（内部第 90-130 行过滤退化方向并按 `cos asc, spacing desc, fingerprint asc` 破平；quality<2 或 socialNorm 退化返回 null）；
  - 只有当条目非 null 时才存入 `candidates` map。

### 2. 当前 mask 如何按动作顺序和指纹去重
- **源码位置**：`ZhangBoQpCandidateSelector.java:48-56`
- **事实核验**：
  - 严格按 `ZhangBoQpAction.values()`（即 `KEEP(0) -> DIRECTIONAL(1) -> EPSILON(2) -> COMPLEMENTARY(3)`）顺序迭代；
  - 若 `candidate != null` 且其指纹未在前面动作的 `seen` 集合中出现，则 `mask[action.ordinal()] = true` 并加入 `seen`；
  - `KEEP` 恒在第 57-59 行断言 `mask[KEEP.ordinal()] == true`，否则抛 `IllegalStateException`。

### 3. 当前动作选择消耗多少随机数
- **源码位置**：`ZhangBoQpController.java:470-486` (`selectAction`)
- **事实核验**：
  - 第 473 行：`double draw = random.nextDouble()` 恒定消耗 1 次；
  - 第 476-479 行：当且仅当 `draw < exploration`（探索分支）时，调用 `valid.get(random.nextInt(0, valid.size() - 1))` 额外消耗 1 次 `nextInt`；
  - 贪婪分支（第 481 行）调用 `selectConfiguredGreedyAction`，消耗 0 次随机数；
  - `GREEDY_FROZEN` 模式（第 155-156 行）直接调用 `selectGreedyAction(frozen[state], mask)`，全程消耗 0 次随机数。

### 4. 当前候选选择不消耗随机数
- **源码位置**：`ZhangBoQpController.java:160`
- **事实核验**：
  - 第 160 行：`ZhangBoArchiveEntry selected = value.candidates.get(action);`；
  - 此时动作 $a$ 确定后，直接从已构建好的 Map 中获取单例条目，**无任何 RNG 调用**。此行即为 $K$ 轴候选池介入的唯一合法锚点。

### 5. 当前 settle 与 reconcile fallback 规则
- **源码位置**：
  - Settle：`ZhangBoQpController.java:253-255`。在子代插入归档后，查询 `ZhangBoQpCandidateSelector.find(nextArchive, selection.selectedPbest.getFingerprint())`；若为 null，则回退为 `ZhangBoQpCandidateSelector.directional(nextArchive, group, bounds)`。
  - Reconcile：`ZhangBoQpController.java:352-369` (`reconcilePopulation`)。在 PDDR 分裂/重组后，查询 `find(memory.entries, requested)`；若缺失则设为 `directional(memory.entries, group, bounds).getFingerprint()`。
  - 均沿用既有 Directional argmin 回退，零新增复杂规则。

### 6. CFVF 在哪里读取 personal leader
- **源码位置**：`ZhangBoMOHPSOQ.java:3454-3464` (`updateCfvfGroup`)
- **事实核验**：
  - 第 3456 行：`ZhangBoQpController.Selection decision = selections.get(particleIndex);`
  - 第 3457 行：`PermutationSolution<Integer> personalLeader = decision.pbestSolution(current);`
  - 提取出个人领导的解实体后，直接传入 `cfvfUpdater.update(...)` 计算速度与位置差分。CFVF 公式本身不读取 Qp 内部状态，仅消费该解对象。

### 7. 奖励是否读取 leader identity
- **源码位置**：`ZhangBoQpController.java:294-295, 371-396` (`reward`)
- **事实核验**：
  - 奖励函数入参为：`ZhangBoArchiveEntry parent, ZhangBoArchiveEntry child, ZhangBoSubSwarm group, boolean archiveSurvived, ZhangBoArchiveBounds bounds`；
  - 计算分量包含：`dominance`（父子三目标支配关系）、`direction`（子代相对父代在子群方向标量 $\phi$ 上的改善度）、`archiveContribution`（子代是否存活于归档）、`fatigue`（子代疲劳惩罚）；
  - **完全不读取、不依赖所选取的 personal leader 解实体或指纹**。

### 8. 个人档案容量、去重与截断规则
- **源码位置**：`ZhangBoPersonalArchive.java:45-69` (`update`) 与 `ZhangBoPersonalArchiveConfiguration.java:17`
- **事实核验**：
  - 容量常数：`DEFAULT_CAPACITY = 6`；
  - 更新流程：`union(entries, child)` $\to$ 三目标严格 Pareto 过滤 $\to$ `duplicateEpsilon=1e-4` 连通分量近重复去重 $\to$ 超 6 截断（保留子群方向锚点与 $\epsilon$-fitness 锚点，最远点填充）。

---

## 二、核验结论

所有 8 个核心事实点与 Phase B0.5 设计完全吻合，不存在任何源码级歧义或隐式依赖。可以正式进入隔离实现阶段。
