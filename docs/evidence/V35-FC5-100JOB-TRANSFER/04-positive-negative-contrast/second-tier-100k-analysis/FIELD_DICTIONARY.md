# FIELD_DICTIONARY — V35-FC5-T 100k 筛查字段语义（Luna C 独立核对）

本字典仅依据源码逐字段核对，不读取任何人的文字结论。字段语义以作者工程源码为准：

- `V35Fc5TransferAudit.java`（schema `FC5_100JOB_TRANSFER_V1`，版本常量同 runner）
- `V35FairRunner.java`（`runFc5TransferDiagnostic` / `writeRecord` / `RunRecord`）
- `ZhangBoV35Fc5TransferRunner.java`（replay launcher，只挂观察器）
- `ZhangBoCmaxAudit.java`（cmax-audit 两个 CSV）
- `ZhangBoQgController.java`（`fingerprint` 定义）

> 权威字段名以源码 `StringBuilder` 的 header 为准；本文“列号”按 header 顺序从 1 计。

---

## 0. 预注册运行集（来自 `ZhangBoV35Fc5TransferRunner.requireApprovedCase`）

FC5-T 只挂观察器，不改搜索语义。预注册对照集为：

| case | 实例 | seed 区间 | 允许 arm | 含义 |
|---|---|---|---|---|
| A0/A2 | `100_2_5_1`, `100_8_3_1` | 20260911–20260915 | A0, A2 | A2 相对 A0 的单变量 |
| A2/A4 | `100_2_4_1`, `100_5_3_1` | 20260901–20260905 | A2, A4 | A4 相对 A2 的单变量 |

- arm 枚举：`A0_BASELINE`→`V35_BASELINE`、`A2_CFVF`→`V35_A2`、`A4_BUDGET_AWARE_CATA`→`V35_FULL_POOL_OFF`。
- A4 = FULL 的*教师池关闭*消融分支：`qp/cfvf/qg/dscr/caTaLite` 全开，`directionalTeacherPool=false`。
- `max-fes` 只允许 2000/50000/100000/250000/500000。
- 100k 二档为 A2 vs A4 配对筛查；`seed-paired-contrast` 按同 seed 配 A2/A4。
- arm 判定字段在 `configuration.txt` 的 `arm=...`（A0/A2/A4）或 `status.properties` 的 `mode=...`（V35_BASELINE / V35_A2 / V35_FULL_POOL_OFF）。

---

## 1. `fc5-transfer-merge-rounds.csv`（`mergeRoundsCsv`）

header：`seed,cycle,fe,Nmerge,Nunique,Nnd,Roverflow`

| 列 | 类型 | 语义 |
|---|---|---|
| seed | long | 主 seed |
| cycle | int | 形式 PDDR 轮次序号（从 1 起） |
| fe | long | 本轮的完整评价数（真实 FE 计数值） |
| **Nmerge** | int | `pool.size()` —— 本轮送入 PDDR 选择器的合并池大小（候选数量） |
| **Nunique** | int | 依据 `[objective0,objective1,objective6]` 三元组去重后的唯一候选数 |
| **Nnd** | int | 在“唯一候选子集”上严格 Pareto 非支配（`[0,1,6]` 三目标，严格支配）的数量 |
| **Roverflow** | double | = `Nnd / TARGET_WORKING_POPULATION`，其中 `TARGET_WORKING_POPULATION=100`（工作种群恒 100） |

- `Roverflow = Nnd/100`（当分母非 0；代码对 `TARGET_WORKING_POPULATION==0` 兜底输出 NaN，但常量恒 100）。
- 定义（源码严格语义）：`dominates(a,b)` = 对 obj 0、1、6 均 `a<=b` 且至少一处 `a<b`。完全相等目标互相不支配；非支配计数只统计“唯一候选”，且要求未被池内任一其他唯一候选支配。

---

## 2. `fc5-transfer-windowed-merge-overflow.csv`（`windowedMergeCsv`）

Java 端按 `WINDOW_FE=50000` 分桶：`windowEnd = round(fe) 所在 50k 桶的终点`，`end=((max(1,fe)-1)/50000+1)*50000`。

header：`seed,windowEndFE,rounds,meanNmerge,meanNnd,maxNnd,meanRoverflow,maxRoverflow`

| 列 | 语义 |
|---|---|
| seed | 种子 |
| windowEndFE | 桶终点（50000 / 100000 / …） |
| rounds | 落入该桶的 PDDR 轮数 |
| meanNmerge / meanNnd | 桶内平均 |
| maxNnd | 桶内最大 Nnd |
| meanRoverflow / maxRoverflow | 桶内平均/最大 Roverflow |

> **注意**：Java 这一文件按“固定 50k 桶”聚合，第二桶标 `windowEndFE=100000`，**不代表**桶内实际有 50000 FE。分析侧须自行重定义 W1/W2（见 §8），不能把该文件当作 W2=[50000,actualFE] 口径，也不要把它冒充完整窗口。

---

## 3. `fc5-transfer-directional-representative-lifecycle.csv`（`representativesCsv`）

**header 共 27 列**（下文列号从 1 计），其中**第 7 列 `fingerprint` 含未加引号的逗号**，CSV 必须特殊解析：

> 解析规则：把整行按逗号 split；`head=tokens[0:6]`，`fingerprint=','.join(tokens[6:-20])`，`tail=tokens[-20:]`。即 **前 6 列 + 变宽 fingerprint + 后 20 列**。

| # | 列 | 语义 |
|---|---|---|
| 1 | seed | 种子 |
| 2 | cycle | 本轮（PDDR 轮次） |
| 3 | fe | 本轮 FE |
| 4 | **representative** | 方向角色：`E_C / E_E / E_W / E_B` |
| 5 | poolIndex | 该代表在池内的原始下标 |
| 6 | source | 代表来源（`ZhangBoEvaluatedPddrSelector.Source` 枚举名） |
| 7 | **fingerprint** | 多向量指纹，见 §7；含 `,` 与 `\|` |
| 8 | Cmax | objective[0]（最小化） |
| 9 | TEC | objective[1]（最小化） |
| 10 | TWC | objective[2]=objective[6]（最小化） |
| 11 | pddrScore | 分数 `= dominatedBy + 1/(dominates+1)` |
| 12 | pddrRank | 按 score 升序、同分按下标稳定排序后的名次（1=最好） |
| 13 | **poolPresent** | **恒为 true**（硬编码；观察器只在池内记录代表） |
| 14 | **pddrSelected** | 是否被 PDDR 选中进入下一个工作种群（true/false） |
| 15 | rejectReason | 未选中时为 `PDDR_SCORE_RANK_NOT_SELECTED`，否则 `SELECTED` |
| 16 | **nextPopulationSlot** | 若选中，进入下一代的物理槽位（1 起）；未选中为 -1 |
| 17 | **nextSemanticRole** | 槽位对应的子群语义角色：`G1_CMAX / G4_BALANCED / G2_TEC / G3_TWC`；未选中为 `NONE` |
| 18 | qgTeacherUses | 该代表被用作 Qg 教师（`QG`）的次数 |
| 19 | qpTeacherUses | 该代表被用作 Qp 教师（`QP`）的次数 |
| 20 | teacherUseCycles | 被用作教师的 cycle 列表（`;` 分隔） |
| 21 | **improvedOffspringCount** | 该代表作为教师产生的、按方向改善“代表方向”的后代数 |
| 22 | lastImprovementFE | 最后一次方向改善后代的 FE（-1 无） |
| 23 | lastImprovementTeacherKind | 最后一次改善的教师类型（QG/QP/NONE） |
| 24 | **lastImprovementRequestingRole** | 最后一次改善的请求子群角色（Gx_xx / UNASSIGNED / NONE） |
| 25 | lastTeacherFE | 最后一次被用作教师的 FE（-1 无） |
| 26 | lastTeacherRole | 最后一次教师用途对应的请求角色 |
| 27 | **retiredAtCycle** | 该代表从“存活工作种群”退休的 cycle（即其 fingerprint 不再被后续选中）；-1=直到观测结束仍存活 |

**方向改善判定**（`improvesRepresentativeDirection`）：
- `E_C`（Cmax 代表）：`child[0] < parent[0]`
- `E_E`（TEC 代表）：`child[1] < parent[1]`
- `E_W`（TWC 代表）：`child[2] < parent[2]`
- `E_B`（平衡代表）：三目标均不劣 + 至少一处严格更优

**“池→next 保留”**：一个代表被选中（`pddrSelected=true`，`nextPopulationSlot>0`）后，若其 `retiredAtCycle==-1` 或 `retiredAtCycle>cycle`，则算“保留进下一轮”。保留率 = 保留数 / 选中数（可到轮/到窗口/到方向）。

**“首次代表损失 FE”**：某方向的代表首次不再是“选中且保留”（即出现 `retiredAtCycle!=-1` 的首个退休）——把 `cycle→fe`（用 merge-rounds 或本文件建立映射）套到该退休 cycle 得到 FE。

---

## 4. `fc5-transfer-archive-working-gap.csv`（`archiveWorkingGapCsv`）

header：`seed,cycle,fe,workingBestCmax,archiveBestCmax,cmaxGap,workingBestTEC,archiveBestTEC,tecGap,workingBestTWC,archiveBestTWC,twcGap,workingSize,archiveSize`

| 列 | 语义 |
|---|---|
| seed/cycle/fe | 种子 / 轮 / FE |
| workingBestCmax | 工作种群 best（最小）Cmax |
| archiveBestCmax | 档案 best（最小）Cmax |
| **cmaxGap** | `= workingBestCmax - archiveBestCmax`（>0=工作比档案差） |
| workingBestTEC / archiveBestTEC / **tecGap** | 同上，TEC（objective[1]） |
| workingBestTWC / archiveBestTWC / **twcGap** | 同上，TWC（objective[2]） |
| workingSize / archiveSize | 两组解数量 |

- `best` = 最小（三个目标均最小化）；空集时输出 `NaN`。
- “首次 gap 扩大 FE”：观测到 `cmaxGap`（或 tecGap/twcGap）首次明显变差/扩大的 FE（见脚本阈值）。默认以 cmax 为主线。

---

## 5. `fc5-transfer-summary.properties`（`summaryProperties`）

```
schema=FC5_100JOB_TRANSFER_V1
seed=<long>
enabled=<boolean>
pddrRounds=<int>                                  # rounds 列表长度
representativeRecords=<int>                       # representatives 总条数
representativesSelected=<int>                     # pddrSelected 计数
representativesWithImprovedOffspring=<int>        # improvedOffspringCount>0 的代表数
archiveWorkingSnapshots=<int>                     # gaps 条数
observerErrors=<int>                              # 观察器吞掉的异常数（>0 说明观察不完整）
```

---

## 6. `cmax-audit-curves.csv` / `cmax-audit-records.csv`（`ZhangBoCmaxAudit`）

### 6.1 `cmax-audit-curves.csv`（`curvesCsv`）

header（15 列）：
`fe,bestCmaxGlobal,bestCmaxG1,currentBestCmaxG1,bestCmaxGenerated,bestCmaxGeneratedG1,bestCmaxSurvived,windowBestGenerated,windowBestSurvived,bestTECGlobal,bestTWCGlobal,bestTECGenerated,bestTWCGenerated`

按 `checkpoints` 每 1000 FE 输出一行；`best*`=历史最优，`current*`=当前，`windowBest*`=滑动窗口最优。

### 6.2 `cmax-audit-records.csv`（`recordsCsv`）

header（27 列）：
`candidateId,parentId,lineageId,generated,admitted,evaluation,generation,cmax,tec,twc,subSwarm,mechanism,operator,enteredCandidateSet,pddrRetained,personalArchive,globalArchive,nextRoundSurvival,g1SocialTeacherParticleUses,g1SocialTeacherGenerations,g1PersonalTeacherParticleUses,g1PersonalTeacherGenerations,firstTeacherFE,lastTeacherFE,firstTeacherGeneration,lastTeacherGeneration,fingerprintSha256`

- `enteredCandidateSet / pddrRetained / personalArchive / globalArchive / nextRoundSurvival` 为布尔（true/false）生存/准入标志。
- `g1SocialTeacher* / g1PersonalTeacher*` 代表 G1 代表被教师使用的次数/代数。
- 本筛查主要用于代表生存与教师曝光辅助；主口径仍是 §3/§4 的 FC5 文件。

---

## 7. fingerprint 构成（`ZhangBoQgController.fingerprint`）

```
getVariables().toString() + '|' + getVariablesid().toString() + '|'
+ (copy(vars)).toString() + '|' + getVariablesworker().toString()
```

四段以 `|` 分隔，每段为 `List.toString()`（形如 `[1, 2, 3, …]`），因此 **fingerprint 恒含逗号**（含空格），不含引号。这就是 §3 需要变宽解析的根本原因。初始种群哈希（`initialHash`）= 逐 solution 的 fingerprint 以 `\n` 连接后 SHA-256。

---

## 8. W1 / W2 窗口定义（筛查侧，与 Java 固定 50k 桶不同）

> **名义窗口只随实际 FE 定义，不得把部分窗口冒充完整窗口。**

- **W1 = [0, 50000]**：`fe ∈ (0, 50000]`（全部轮次皆可稳定出现，默认视为观察完整）；`W1End=50000`。
- **W2 = [50000, actualFE]**：`fe ∈ (50000, actualFE]`；`W2End=actualFE`。
  - 因第 100k 实验采用 *Phase-Consistent Budget Termination*（`0 < actualFE = decoderCalls <= MaxFEs=100000`，且 `0 <= remainingFE < qPhaseFE=5000`），**实际 W2 跨度 `< 50000`**。
  - 因此运行中 `actualFE < 100000` 时（尤其 A4）**W2 必须标记 `PARTIAL_SECOND_WINDOW`**，不得冒充完整 50k 窗口，亦不得把 W1 与 W2 拼成 100k 连续轨迹（区分：100k 是独立预算实验，MaxFEs 影响 A4 预热/Pacing/预算调度）。
- 窗口聚合（`windowed-overflow.csv`）一律在**筛查侧**按 §8 重算，不复用 §2 的 Java 桶。

---

## 9. 运行级状态文件

### 9.1 `status.properties`（`writeRecord`）

`status`（COMPLETED/FAILED）、`mode`、`fullEvaluations`（= actualFE）、`decoderCalls`、`illegalSolutions`、`duplicateEvaluations`、`runtimeSubSwarmSizes`、`initialPopulationHash`、`evaluationTraceHash`、`stopReason`、`mechanismSummary`、`algorithmRunNanos`、`decoderTiming`。

### 9.2 `configuration.txt`（`configurationText + provenanceText`）

`runnerVersion`、`diagnosticKind=FC5_100JOB_TRANSFER_OBSERVER_ONLY`、`preRegistered=true`、`instance`、`seed`、`arm`、`population=100`、`maxFEs`、`sourceRunId`、`snapshotSha256`、`initialPopulationHashV35`、`initialPopulationHashP8`、`profileSha256`、`observerVersion`、`observerOnly=true`、`globalOriginalPddr=true`、`profileCanonicalBegin/End`，随后：
`instanceSha256`、`instanceExtensionSha256`、`fatigueConfigurationSha256`、`formalBaselineSha256`、`formalBaselineCanonicalBegin/End`。

### 9.3 `initial-population.sha256`

`v35=<initialHashV35>`、`p8=<initialHashP8>`、`snapshot=<snapshot 文件 SHA-256>`。

### 9.4 `evidence-sha256.tsv`（`writeHashes`）

**制表符分隔**，header：`sha256\tbytes\tpath`；`path` 为相对斜杠路径，`/` 分隔；**排除自身**。反向验证 = 逐行重读 `path`，重算 SHA-256 并比对 `bytes`（字节数）。

---

## 10. 验收口径（run-acceptance-recheck）

运行验收以 `ZhangBoV35Fc5TransferRunner.requireAccepted` + `telemetry-contract.properties` 为准：

- `status == COMPLETED`
- 前沿非空
- `illegalSolutions == 0`、`duplicateEvaluations == 0`
- `0 < fullEvaluations (=actualFE) <= maxFEs`
- `decoderCalls == fullEvaluations`
- `fc5TransferSummary` 非空
- 另外核对：`initial-population.sha256` 的 `v35` 与 `status.properties.initialPopulationHash` 一致；`configuration.txt` 的 `maxFEs`、`arm` 与预注册集匹配。

（“24→应为 6 条”为入口提示：raw 树可能含大量目录，筛查侧只应对**落在预注册 FC5 A2/A4 对照集且 maxFEs==100000** 的“运行目录”计数并成对。）
