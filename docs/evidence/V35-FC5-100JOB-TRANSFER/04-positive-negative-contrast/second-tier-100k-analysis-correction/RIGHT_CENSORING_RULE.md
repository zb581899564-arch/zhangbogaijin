# RIGHT_CENSORING_RULE — 方向代表下一轮存活的右删失处理

## 1. 为何需要

`retiredAtCycle` 由 `V35Fc5TransferAudit.retireMissing()` 在每轮用 `selectedFingerprints` 比对 `liveByFingerprint` 决定：
- 若指纹不在当轮选中集合中，则该指纹的所有历史记录被标记 `retiredAtCycle = 当前cycle` 并移出live。
- 若至观测结束仍在live，则 `retiredAtCycle = -1`。

对于最后一轮（A2: cycle 3 @ 96k, A4: cycle 12 @ 96025），**没有下一轮观察机会**：即使 `retiredAtCycle==-1`，也无法判断它在下一轮是否仍存活。

若把最后一轮计入 `next→nextCycle` 分母，会把“未观察”误判为“已失败”或“已存活”，引入偏差。

## 2. 规则

```
nextCycleEligible = enteredNextPopulation AND cycle < max_cycle_in_this_run
survivedNextCycle = nextCycleEligible AND (retiredAtCycle==-1 OR retiredAtCycle > cycle+1)
nextToNextCycleRate = survivedNextCycle / nextCycleEligible
```

- 最后一轮的 `enteredNext` 代表不进入 `nextCycleEligible`，其 `survivedNextCycle` 记为空（right-censored）。
- 统计粒度：DIRECTION_LABEL_EVENT 和 UNIQUE_DIRECTIONAL_REPRESENTATIVE 均执行此规则。

## 3. 本次数据中的实现（以 directional-retention-corrected.csv 自动统计为准）

- **A2**：max_cycle=3，每seed 12 label events，**每seed末轮4条 censored**，3 seed共 **12条** 已剔除 eligible 分母（eligible 24 = 36-12，survived 9，ALL 115/152含A2）。
- **A4**：max_cycle=12，每seed 48 label events，**末轮 censored 分seed为3/4/3条，共10条**（20260901:3，20260902:4，20260903:3），A4_ONLY eligible 128 = 144-16? 实际 entered 138中 eligible 128 = 138-10，survived 99；UNIQUE 127→99。
- **判定规则已在 `analyze_100k_correction.py:213-215` 实现：`nextCycleEligible = enteredNext AND cycle < max_cycle_in_this_run`**，末轮记为空（right-censored），不计入分母/分子。
- 聚合表 `directional-retention-corrected.csv` 的 `nextCycleEligible` 列即为分母，`survivedNextCycle` 为分子。

## 4. 源码依据

`V35Fc5TransferAudit.java:287-298 retireMissing`, `Representative.retiredAtCycle:528`, `analyze_100k_correction.py:213-215 transition_rows`。
