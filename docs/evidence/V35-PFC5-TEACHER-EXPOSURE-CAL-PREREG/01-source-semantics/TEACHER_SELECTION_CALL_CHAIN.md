# 教师选择源码只读语义审计

审计日期：2026-08-30
审计对象：`_isolated-v35-final-doe1-freeze-20260823/java-jmetal58/` —— 即冻结正式算法 Jar
`8dad8f40…d8b9` 的构建源码树（**不是**已被再次构建的 `张博改进/java-jmetal58/` 工作树）。
性质：**只读。未改任何源码，未编译，未运行，未消耗 FE。**
所有行号为实际打开文件所得，所有 SHA-256 为 `sha256sum` 实测。

---

## 1. 调用链

```text
ZhangBoMOHPSOQ (主循环, ZhangBoMOHPSOQ.java:2726)
  构造 candidates = copy(globallyOptimalIndividual)               :2726-2729
  （若为空，回退到 4 个子群解列表）                                  :2738-2743
  if isDscrEnabled(): candidates = applyV35Dscr(candidates)        :2745-2746
        └─ V35SocialKnowledgeSnapshot.fromEvaluatedSolutions(...)  :2780
        └─ zhangBoQgController.sanitizeTeacherCaches(...)          :2794-2797   ← DSCR 缓存清洗
        └─ 过滤被任一 snapshot teacher 严格支配的候选                 :2809-2839
  selectQgLeader(G1_CMAX / G4_BALANCED / G2_TEC / G3_TWC, ...)     :2754-2757
        └─ ZhangBoQgController.selectGreedy(group, candidates)     :2849  (Dual-Q P-block 时)
        └─ ZhangBoQgController.select(group, candidates)           :2850  (常规)
              ├─ random.nextDouble()           决定 exploit/explore  :70
              ├─ bestAction(...) 或 random.nextInt(0,2)  决定 action :73 / :76
              ├─ action==0 → copy(previous.get(group))              :81
              ├─ action==1 → copy(historical.get(group))            :84
              └─ action==2 → tournament(group, candidates, ...)     :87
                    └─ pool(group, candidates)                      :294 / :329-347
                    └─ random.nextInt(0, size-1) ×2                 :295-296
                    └─ compare(group, left, right, candidates)      :299 / :391-406
                          └─ 边界子群：单目标 raw 比较                :398-400
                          └─ G4_BALANCED：pddr(...) 比较            :402 / :408-423
                          └─ 平局 → fingerprint 字典序               :404
```

Qp 侧（另一独立调用链）：

```text
ZhangBoMOHPSOQ.java:3441
  └─ ZhangBoQpController.selectGroup(...)                    ZhangBoQpController.java:80/93
        └─ ZhangBoQpCandidateSelector.build(...)             ZhangBoQpCandidateSelector.java:29
              └─ directional / keep / epsilon / complementary  各自确定性 argmin
              └─ 构造 boolean[] mask（按 fingerprint 去重）        :50-58
        └─ ZhangBoArchiveEntry selected = candidates.get(action)  ZhangBoQpController.java:157
```

---

## 2. 九个问题的答复

### 2.1 Qg 的 teacher 候选集合如何形成

**候选集合 = DSCR 过滤后的全局档案副本。**

- 来源：`ZhangBoMOHPSOQ.java:2726-2729`，`candidates` 是 `globallyOptimalIndividual`（全局档案）的逐元素 `copy()`；为空时回退到四个子群解列表（`:2738-2743`）。
- DSCR 过滤：`applyV35Dscr` (`:2775-2840`) 用 `V35SocialKnowledgeSnapshot.fromEvaluatedSolutions(candidates)` 构造教师快照后，剔除**被任一快照教师严格支配**的候选（`:2818-2831`）。
- 合法性条件（来自 `V35SocialKnowledgeSnapshot.java:45-49`）：任一解若 `null` 或三个目标中有非有限值，**直接抛异常**。因此进入候选集合的解必然已评价且目标有限 —— 满足"不得读取未评价解"。
- 兜底：若过滤后为空，保留 `candidates.get(0)`（`:2836`）。

### 2.2 Qp 的 pbest 候选集合如何形成

**Qp 不是"一个候选集合 + 选择"，而是"动作 → 唯一候选"的固定映射。**

`ZhangBoQpCandidateSelector.build()` (`:29-62`) 对个人档案条目按 fingerprint 排序后，为四个动作各算出**唯一一个**条目：

| 动作 | 候选 | 规则 | 行号 |
|---|---|---|---|
| `KEEP` | 当前已选 pbest（按 fingerprint 找回），找不到则用 directional | 直接取 | `:38-40` |
| `DIRECTIONAL` | `argmin φ`，平局取 fingerprint 小者 | 确定性 argmin | `:64-74` |
| `EPSILON` | `argmin ε-fitness`，平局取 fingerprint 小者 | 确定性 argmin | `:76-86` |
| `COMPLEMENTARY` | 在 `φ ≤ bestPhi + qualityTolerance` 的质量集合内，min cosine → max spacing → fingerprint | 确定性 argmin | `:88-129` |

随后 `mask` 按 fingerprint 去重（`:50-58`），并保证 `KEEP` 恒合法（`:59-61`）。

**结论：Qp 每个动作的合法候选集合均为单元素（或空）。不存在"动作已定、在集合内改选哪个 teacher"的自由度。**

### 2.3 DSCR 何时清洗缓存

**在候选集合构造之后、在 `selectQgLeader` 之前，且清洗先于支配过滤。**

- `ZhangBoMOHPSOQ.java:2794-2797`：`zhangBoQgController.sanitizeTeacherCaches(group, snapshot, v35DscrTeacherCache, v35DscrDecisionCycle, generationNumber(), fullEvaluationCount)`
- 实现：`ZhangBoQgController.sanitizeTeacherCaches` (`:242-254`) → 对 `previous`（`:249-250`）与 `historical`（`:251-252`）各调 `sanitizeOne`。
- `sanitizeOne` (`:349-371`)：取缓存解 → `V35DscrSanitizer.sanitize(role, before, snapshot)` → 若指纹变化，用快照中同指纹解替换（`:361-368`）。
- 顺序证据：清洗在 `:2788-2808` 循环内，支配过滤在 `:2809-2839`，`selectQgLeader` 在 `:2754-2757`（即 `applyV35Dscr` 返回之后）。**因此"DSCR 清洗后才计算候选"成立。**
- 注释佐证：`ZhangBoQgController.java:238-241` —— "Sanitizes ... This consumes no FE, no random event, and does not modify Q values or controller states."

### 2.4 各动作读取哪个集合

| 系统 | 动作（源码真实标识） | 读取的集合 | 行号 |
|---|---|---|---|
| Qg | `0`（遥测名 `PREVIOUS_CACHE`） | `previous` 单例缓存 | `:81` |
| Qg | `1`（遥测名 `HISTORICAL_CACHE`） | `historical` 单例缓存 | `:84` |
| Qg | `2`（遥测名 `GLOBAL_ARCHIVE_TOURNAMENT`） | `pool(group, candidates)` ⊆ 候选集合 | `:87`→`:294` |
| Qp | `KEEP` | 单元素（当前 pbest） | `ZhangBoQpCandidateSelector:38-40` |
| Qp | `DIRECTIONAL` | 单元素（argmin φ） | `:64-74` |
| Qp | `EPSILON` | 单元素（argmin ε-fitness） | `:76-86` |
| Qp | `COMPLEMENTARY` | 单元素（argmin cosine） | `:88-129` |

**Qg 只有 action 2 读取一个多元素候选集合。**

`pool()` 的口径（`:329-347`）：当 `!directionalTeacherPool || !isBoundary(group)` 时**直接返回 `candidates`**；否则取该子群目标上最优的 `teacherPoolSize` 个。A4 配置 `directionalTeacherPool=false` → **pool ≡ candidates**。

### 2.5 比较器、方向分数、破平规则

比较器：`ZhangBoQgController.compare(group, left, right, reference)` (`:391-406`)。

```java
if (ZhangBoSubSwarmSemantics.isBoundary(group)) {
  comparison = Double.compare(
      left.getObjective(ZhangBoSubSwarmSemantics.objectiveIndex(group)),
      right.getObjective(ZhangBoSubSwarmSemantics.objectiveIndex(group)));
} else {
  comparison = Double.compare(pddr(left, reference), pddr(right, reference));
}
if (comparison == 0) comparison = fingerprint(left).compareTo(fingerprint(right));
```

- **优化方向：两个分支都是"越小越好"，即已经是 loss。**（调用点 `:299` 用 `compare(...) <= 0 ? left : right`，取较小者。）
- 分支一（G1_CMAX / G2_TEC / G3_TWC）：**原始单目标值**，未归一化，量纲差异巨大（Cmax 跨度约 728，TEC 约 18761，TWC 约 75079）。
- 分支二（G4_BALANCED）：`pddr = dominatedBy + 1.0/(dominates + 1.0)`（`:422`），其中 `dominates/dominatedBy` 为相对 `reference`（=候选集合）的支配计数（`:415-421`）。值域约 `(0, n]`（n = 候选集合大小），**亦越小越好**。
- **破平规则：稳定指纹字典序** `fingerprint(left).compareTo(fingerprint(right))`（`:404`）。指纹定义见 `:444-448`：
  `variables | variablesid | machineVector | variablesworker` 的字符串拼接 —— 是四向量基因型的纯函数。

### 2.6 teacher 选择是否消耗随机数

**消耗。** 三处：

| 位置 | 调用 | 用途 |
|---|---|---|
| `ZhangBoQgController.java:70` | `random.nextDouble()` | ε-greedy：决定 exploit / explore |
| `:76` | `random.nextInt(0, 2)` | explore 时均匀选动作 |
| `:295-296` | `random.nextInt(0, pool.size()-1)` ×2 | action 2 二元锦标赛抽两个候选 |

`Qp` 侧同样消耗：`ZhangBoQpController.java:451`（`random.nextDouble()`）、`:455`（`random.nextInt(0, valid.size()-1)`）。

**实现含义**：任何 exposure-aware 重选都**不得**引入或省略任何一次随机抽取，否则 RNG 序列错位，C0 等价门必然失败。

### 2.7 teacher 缓存何时更新

- `previous`：**每次 `select()` 无条件更新**为本次 leader（`ZhangBoQgController.java:89`）。
- `historical`：仅当 `compare(group, leader, best, candidates) < 0` 时更新为 leader（`:90-93`）。
- DSCR 替换（`sanitizeOne:367`）在清洗时**就地**改写缓存内容。
- 缓存写入点均在 leader 确定之后，即"使用之后、下一代选择之前"。

### 2.8 teacher exposure 当前是否已有计数

**有，但分两类，且均不参与选择。**

| 计数器 | 位置 | 粒度 | 覆盖范围 |
|---|---|---|---|
| `previousExposure` | `ZhangBoQgController.java:27` | `Map<ZhangBoSubSwarm, Long>` | 仅 action 0，按子群计数，**不按教师身份** |
| `historicalExposure` | `:28` | `Map<ZhangBoSubSwarm, Long>` | 仅 action 1，同上 |
| `selectionCount` | `:35` | 全局 long | 全部 Qg 选择次数 |
| `exposureByFingerprint` | `V35Fc6BpPddrDiagnosticAudit.java:79` | `LinkedHashMap<String, RescueExposure>` | **按教师身份**，含 `qgTeacherCount` 与 `qgByGroup[]`（`:391-392`）；属诊断审计层，纯观察 |

递增点：`select()` 中 action 0 → `increment(previousExposure, group)`（`:82`）；action 1 → `increment(historicalExposure, group)`（`:85`）；**action 2（锦标赛）不递增任何 exposure 计数器。**

注释佐证：`:41` —— "Observation-only counters; they never participate in action selection."

### 2.9 Qp 与 Qg 是否共享计数器

- **控制器自身的计数器彼此独立**：`previousExposure` / `historicalExposure` / `selectionCount` 均为 `ZhangBoQgController` 的实例字段，Qp 侧无读写路径。
- **诊断审计层的身份映射是共享容器、分立字段**：`V35Fc6BpPddrDiagnosticAudit.exposureByFingerprint`（`:79`）以 fingerprint 为键，**Qg 与 Qp 共用同一张表**，但分别写入 `qgTeacherCount`/`qgByGroup`（`:391-392`）与 `cfvfPbestCount`/`cfvfPbestByGroup`（`:416-417`）。即：**共享容器，分立计数。**

---

## 3. 候选合法性契约（全部排除条件）

| 条件 | 位置 | 效果 |
|---|---|---|
| 候选列表为 null 或空 | `ZhangBoQgController.requireCandidates` `:464-466` | 抛 `IllegalArgumentException` |
| 解为 null，或目标 0/1/6 非有限 | `V35SocialKnowledgeSnapshot.java:46-50` | 抛 `IllegalArgumentException` |
| 解被任一快照教师严格支配 | `ZhangBoMOHPSOQ.java:2826-2829` | 从 `filtered` 中剔除 |
| 过滤后为空 | `:2836` | 强制保留 `candidates.get(0)` |
| Qp：某动作的候选与更早动作的候选 fingerprint 重复 | `ZhangBoQpCandidateSelector.java:52-57` | `mask=false`，该动作非法 |
| Qp：`KEEP` 缺失 | `:59-61` | 抛 `IllegalStateException` |
| DSCR 替换目标在快照中不存在 | `ZhangBoQgController.java:363-366` | 抛 `IllegalStateException` |
| 选出的 Qg 教师被严格支配（DSCR 开启时） | `ZhangBoMOHPSOQ.java:2886` | 抛 `IllegalStateException` |

---

## 4. 对 Calibration 的三条硬约束（由源码直接推出）

1. **唯一可注入点是 Qg 的 action 2 二元锦标赛。**
   Qg action 0/1 读单例缓存；Qp 四个动作各自映射到唯一候选。**这两者都没有"在集合内改选哪个 teacher"的自由度**，按 §6.2「如果某Q动作不使用候选集合，不得强行套入」，不得强行套入。
2. **比较器天然是 loss（越小越好），但两个分支量纲不可比。**
   边界子群用原始目标（跨度 7×10² ~ 7.5×10⁴），G4 用 PDDR（跨度 < n）。因此 `baseLoss` **必须在当前候选集合内做 min-max 归一化**后才能与无量纲的 exposure 项相加，否则 λ 在不同子群上的实际杠杆相差两三个数量级。
3. **注入必须落在 `compare()` 的成对比较上，不能改成全局 argmin。**
   锦标赛每轮恰好消耗 2 次 `nextInt`（`:295-296`）并只比较两个元素。改成全池 argmin 会改变随机抽取次数与语义，直接违反 C0 精确等价门。正确做法是保持抽取与比较次数不变，只把比较判据换成 `adjustedLoss`。

---

## 5. 已核对的工作树差异提示

审计基于隔离冻结树。项目既有记录（`diagnostic-freeze.properties` / 封板令第 5 条）已声明工作树 `java-jmetal58` 在封板后被再次构建（jmetal-algorithm target 现为 `a0a1e74d…`）。本预登记的源码结论**只对 `8dad8f40…d8b9` 这一冻结实体负责**；未来实现必须重新指向冻结源码树，不得指向工作树。
