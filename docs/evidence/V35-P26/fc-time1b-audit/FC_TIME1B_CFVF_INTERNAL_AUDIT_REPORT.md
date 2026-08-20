# FC-TIME-1B：CFVF 内部审计报告（2026-08-18）

> 诊断任务（非算法改进）：只插计时/计数器，未改任何决策、缓存、随机序与 FE。行为等价门 `V35Fc1ModuleTimerTest`（front hash / FE / 事件流逐位一致）保持通过。
> 运行：20_2_3_1 / 50k FE / seed 20260822 / population=100 / 本地串行 JDK17 / A4-Legacy 与 A4-PACING 两臂。

## A. CFVF 15.37s（排序后实际 12.9s）内部时间分解（树状，非线性求和）

> 说明：module.TOTAL 含父子包含段（CFVF 主段含全部 CFVF.* 子段），报告按树状归因。pacing 墙钟 ≈ 20.0s。

```
pacing 50k 墙钟 ≈ 20.0s
├─ CFVF / updatePosition 全路径          12.94s (64.7%)   ← 每 Q round 一次 × 300
│  ├─ prepareDualQCoordination            0.05s (0.2%)
│  ├─ prepareOriginalQg                   8.92s (44.6%)   ← 老师准备（核心嫌疑段）
│  │  ├─ archive 深拷贝                   0.44s (2.2%)    300 calls × 平均 1.5ms
│  │  ├─ applyV35Dscr（DSCR 快照+过滤）   8.78s (43.9%)  ◀ 最大单块
│  │  └─ selectQgLeader ×4                0.10s (0.5%)
│  └─ updatePositionWithCfvf
│     ├─ Prep（lineage/Qp 选择/深拷贝）   1.32s (6.6%)
│     ├─ GroupUpdate（4×updateCfvfGroup） 1.55s (7.7%)   ← 真正"跟老师学"
│     │  └─ Cfvf.* 粒子级动作（合计）     0.42s (2.1%)   ← CFVF 本体
│     └─ TailPddr（PDDRFFselect）         1.10s (5.5%)
├─ Dominance（个人史剪枝）                2.57s (12.8%)
├─ Other（updateVelocity 组建/select 混段）1.37s (6.8%)
├─ FM3Decode                              0.65s (3.2%)
├─ InterFactoryLS                         0.50s (2.5%)
├─ Qp                                     0.49s (2.4%)
├─ CA-TA                                  0.26s (1.3%)
├─ PDDR_FF                                0.24s (1.2%)
├─ Archive                                0.06s (0.3%)
└─ Qg / copy / audit                     <0.1s
```

## B. 各子模块 total / calls / avg / items（pacing 50k）

| 模块 | total | calls | avg/call | 备注 |
|---|---:|---:|---:|---|
| CFVF（父段） | 12.94s | 300 | 43.1ms | — |
| ├ Og.ArchiveCopy | 0.44s | 300 | 1.5ms | 每 Q round 深拷贝全 archive |
| ├ **Og.Dscr** | **8.78s** | 300 | **29.3ms** | **最大单块**（legacy 11.8ms → 2.5×） |
| ├ Og.LeaderSelect | 0.10s | 300 | 0.3ms | |
| ├ Prep | 1.32s | 300 | 4.4ms | |
| ├ GroupUpdate | 1.55s | 300 | 5.2ms | |
| │ └ Cfvf.ValidateCopy | 0.046s | 30000 | 1.5µs | |
| │ └ Cfvf.JsChannel | 0.055s | 30000 | 1.8µs | |
| │ └ Cfvf.PbestDiff | 0.45s | 30000 | 15µs | |
| │ └ Cfvf.ResourceMerge | 0.056s | 30000 | 1.9µs | `conflictResolutionCount=600k`（20/粒子） |
| │ └ Cfvf.ActionApply | 0.125s | 30000 | 4.2µs | `resourceActionBuildCount=192k` |
| │ └ Cfvf.RepairLegality | 0.022s | 30000 | 0.7µs | `legalityCheckCount=30k` |
| │ └ Cfvf.TailHamming | 0.030s | 30000 | 1.0µs | |
| └ TailPddr | 1.10s | 300 | 3.7ms | 尾部 PDDRFFselect（O(S²)） |

**计数器全景（pacing 50k）**：`archiveScanCalls=1800`、`archiveItemsVisited=216,114`；`dscrCalls=300`、`socialCandidateBuildCalls=300`；`pddrCalls=756`、`pddrItemsVisited=89,962`；`dominatesCalls=5,478,066`（其中 DSCR 过滤 A×T 占大头）；`cfvfParticleUpdateCount=30,000`、`cfvfSubgroupUpdateCount=1,200`；`pbestDifferenceBuildCount=323,860`、`gbestDifferenceBuildCount=337,591`、`jsActionBuildCount=242,795`。

## C. 真实调用层级

```text
outer cycle (6)
 └─ × Q round (50，共 300)
     ├─ updatePosition → prepareDualQCoordination（1次）
     ├─ updatePosition → prepareOriginalQg（1次）
     │    ├─ archive copy：扫 globallyOptimalIndividual（A 个整解 copy）
     │    ├─ applyV35Dscr：fromEvaluatedSolutions 快照（O(A)）+ sanitize×4（O(T)×4）+ 过滤 A×T 严格支配
     │    └─ selectQgLeader ×4：QgController.select → tournament（G4 非边界走 pddr O(A) 评分）
     ├─ updatePosition → updatePositionWithCfvf
     │    ├─ Prep：lineage freezeBounds（O(A)）+ prepareQpSelections + 深拷贝
     │    ├─ updateCfvfGroup ×4（每组 25 粒子）
     │    │    └─ 每粒子：ZhangBoCfvfUpdater.update（O(jobs)，不扫 archive）
     │    └─ Tail：PDDRFFselect（O(S²=10,000 对)）
```

## D. DSCR 实际调用层级——与设计语义一致

- 设计期望：每 Q-round / subgroup 一次，不在 particle 内。
- **Actual：每 Q round 恰好 1 次**（`dscrCalls=300 = 300 Q rounds`），位于 `prepareOriginalQg` 内，在 4×selectQgLeader 之前只执行一次快照刷新（`socialCandidateBuildCalls=300`）；`updateCfvfGroup`（particle 级）不调 DSCR。
- **结论：层级正确，无粒子级重复**。问题不在层级，而在"每 Q round 全量重做 + 每次 O(A×T) 过滤"。

## E–G. 一个 Q round / particle / subgroup 内 archive 被扫描的遍数

| 作用域 | archive 扫描遍数 | 来源 |
|---|---|---|
| 每 Q round | **≈ 7 次**（总 O(A) 约 3×A + O(A×T) 过滤） | copy 1 + DSCR 快照 1 + sanitize×4（每遍 O(T)）+ pddr 评分（G4 每 compare O(A)，约 2–3 次） |
| 每 particle | **0 次** | CFVF 粒子更新不读 archive（只读传入的 pbest/gbest） |
| 每 subgroup | **0 次** | 同 particle |
| 每 outer cycle | **300×7 ≈ 2,100 次扫描、编辑 ~216k items** | `archiveItemsVisited=216,114` |

## H. archiveSize 增长时哪个子模块在膨胀（per-cycle，pacing）

| cycle | archiveSize | timePerQRound | Og.Dscr/call 趋势 | 全量 dominates/call |
|---|---:|---:|---:|---:|
| 1 | 56 | 21.2ms | ~10ms | ~6k |
| 2 | 95 | 23.9ms | ↑ | ↑ |
| 3 | 97 | 31.2ms | ↑ | ↑ |
| 4 | 139 | 33.6ms | ↑ | ↑ |
| 5 | 191 | 61.3ms | ↑ | ↑ |
| 6 | 224 | 87.6ms | ~29ms | ~18k |

**膨胀源：Og.Dscr 的 A×T 过滤**（`dominatesCalls` 5.7k→18.3k/call，随 archive 约线性 3 倍；T≈A 使过滤量 ≈ A²）+ Og.ArchiveCopy 的 O(A)。pddr（G4）仅 756 calls × 90k items，总量小。CFVF 本体各子段 calls 恒定（每粒子一次），单次成本不随 archive 变化。

## I. CFVF 真正 action generation 占比

`Cfvf.*` 八段合计 ≈ **2.1%**（0.42s / 20s）。**"跟老师学"本身极其便宜**；96% 的 CFVF 时间在老师准备与排序外围。

## J. legacy 对照（50k）

- legacy：CFVF 总 2.76s（27.5%），其中 Og.Dscr 1.18s（11.7%，11.8ms/call）、GroupUpdate 0.89s（8.9%）、LS 反而 2.9s（22.7%）。
- pacing 把 LS 截断（2.9s→0.5s），却把 Og.Dscr 从 1.18s 放大到 8.78s（×7.4）：**因为 pacing 把 2 cycle 变 6 cycle、每次 DSCR 的 archive 还更大**。pacing 的净代价 = DSCR 随 archive 增长的 O(A²) 放大。

## K. personal-history Dominance 13.1%（约 2.6–2.9s）初步说明

计时包裹为 `appendAndPrunePersonalHistories` 方法整段（每 Q round 一次 × 300）：逐粒子与其去重后个人史做全对严格支配剪枝（O(S×V²)，V≈个人史长度），另含 `select()` 全局锦标赛（在 updateVelocity 内，归 Other 6.8%）。**第二优先级，未进一步拆分**；量级与 CFVF 外围不可比，不阻塞。

## L. 最终结论

> **结论 A：CFVF 本体不重（action generation ≈ 2.1%），主要问题是 DSCR / social candidate preparation 在外围以"每 Q round 全量重做 + O(A×T) 过滤"的低效层级重复执行（Og.Dscr 43.9%，+准备链合计 ≈ 56%）。** 证据：dscrCalls=300=Q round 数、pddrItemsVisited 仅 90k 而 DSCR 过滤 dominates 5.48M、per-cycle 曲线与 archive 平方同步、CFVF 本体子段 calls 恒定不随 archive 变。

## M. 候选优化点（只提不实现，等待裁决）

1. **完全语义等价（第 1 类，可进 FC-TIME-2）**
   - a. `applyV35Dscr` 过滤循环：`fingerprint(solution)`（四向量 toString 拼接）在**每个 (solution,teacher) 对内重建**（A×T 次/调用）——移到外层循环前算一次，语义逐位相同（fingerprint 是纯函数）。预估省去大量字符串分配，占 DSCR 时间的大头之一。
   - b. `prepareOriginalQg` 的 archive 深拷贝（每 Q round A 个 copy，0.44s）改为**只读引用**（已验证 selectQgLeader/select/tournament/pddr 对 candidates 只读；applyV35Dscr 产出新 list 不写原解）。省 2.2% 且零风险。
   - c. `Og.Dscr` 可提前计算的项：`teacher.getFingerprint()` 已在 snapshot 内缓存，无需改。
2. **调整执行层级、理论语义等价（第 2 类，需实验验证质量）**
   - d. DSCR 快照/过滤从"每 Q round"降为"每 outer cycle"或"archive 变化时的脏标志复用"：改变刷新时机 → 输入相同但决策上下文不同，结果可能变，须跑 A/B（50k×3 seed 质量门）确认 HV/IGD/Cmax 不退。
   - e. `sanitizeTeacherCaches×4` 合并为一次全 roles 扫描（当前每 group 各自扫全部 snapshot teachers，=4×O(T)）。
3. **真正改变算法（第 3 类，不做）**
   - f. DSCR 过滤粒度放宽（只过滤被严格支配的、抽样 teachers）——改变 DSCR 语义，三创新冻结下禁止。

## 证据文件

- `fc-time1b-audit/pacing-50k/`、`fc-time1b-audit/legacy-50k/`：front.csv + mechanism-summary.txt（moduleTiming + perCycleTiming + counters）
- 代码：`V35ModuleTimer`（计时+计数，纯旁路）；插桩点全为只读
- 等价门：`V35Fc1ModuleTimerTest`（2/2 通过，profile ON/OFF front hash+FE 一致）