# P9 HV/IGD reference与运行时间审计

## 审计结论

状态：`DONE_WITH_CONCERNS`。

1. 原P9单seed报告的IGD差距被reference构造明显放大。当前代码只合并该seed的`ZHANGBO-FULL`与`HMOPSO-QGS-F`前沿，再把其非支配集同时作为两者reference。这个指标适合单次诊断，不适合作为论文正式指标。
2. 把现有六个seed、两个算法的全部前沿先合并再严格Pareto过滤，并为全部运行冻结同一归一化边界后，FULL仍然明显占优；但IGD差距从原seed 20260808的约55倍下降到4.85倍，六seed范围为4.24–7.18倍。
3. 进一步执行leave-one-seed-out敏感性检查，即当前seed的FULL和BASE都不进入reference，FULL的IGD优势仍为约3.71–5.59倍。因此正向信号仍存在，但原来的“55倍”不能用于论文表述。
4. 35–39倍wall-clock不是疲劳解码本身造成的恒定倍数，而是FULL路径存在随运行长度增长的未计FE开销。100k FE隔离剖析中FULL为53.626秒、BASE为6.548秒，已是8.19倍；20k FE只有约3.5–3.8倍，500k FE扩大到约35–43倍，呈明显超线性。
5. JFR显示FULL的3589个CPU样本中，`ComparableTimSort`三个核心方法合计占56.7%。源码唯一会对不断增长的`Long`历史反复做自然排序的核心路径，是CA-TA统计每次生成`Snapshot`时重新复制和排序全部wall-clock/FE历史以计算中位数；而`best()`的比较器又会多次生成相同Snapshot。它是当前第一性能根因。

## Reference构造核查

当前实现位于`ZhangBoP9SingleComparisonRunner.runReport()`：只读取当前seed的两个front，执行`ND(FULL ∪ BASE)`后立即计算两者HV/IGD。`P8MetricCalculator.calculate()`又从传入reference单独计算min/max，因此不同seed报告使用不同归一化边界。

现有六seed、两算法共有5024个三目标点，均无重复。统一经验reference为：

- `PF_ref = ND(全部2算法 × 全部6次运行)`；
- reference大小：1151；
- reference目标下界：`(171.579497901, 8342.04566127, 12462.0762651)`；
- reference目标上界：`(445.870305857, 10990.1868641, 18995.6760991)`；
- FULL贡献1145点，BASE贡献6点，重合0点；FULL占99.48%。

这说明用户提出的reference泄漏担忧成立。由于当前只比较两个算法，而且FULL实际支配性很强，即便按“all algorithms, all runs”重建，经验PF仍会主要由FULL贡献。这本身不是计算错误，但意味着论文最终reference必须等所有正式对比算法和全部正式运行完成后一次性冻结，不能用当前两算法结果提前定稿。

| seed | FULL HV | BASE HV | FULL IGD | BASE IGD | BASE/FULL IGD |
|---:|---:|---:|---:|---:|---:|
| 20260808 | 0.956588 | 0.720456 | 0.031210 | 0.151240 | 4.85 |
| 20260809 | 0.936225 | 0.681284 | 0.028435 | 0.184136 | 6.48 |
| 20260810 | 0.937124 | 0.757306 | 0.032234 | 0.136715 | 4.24 |
| 20260811 | 0.942088 | 0.706396 | 0.034532 | 0.151580 | 4.39 |
| 20260812 | 0.937680 | 0.746395 | 0.032026 | 0.140611 | 4.39 |
| 20260813 | 0.974672 | 0.710222 | 0.022928 | 0.164546 | 7.18 |

leave-one-seed-out使用其他五个seed的两算法前沿作为reference，并继续使用六seed统一边界。FULL/BASE IGD倍率为4.09、4.82、3.83、4.26、3.71、5.59。当前seed不再能用自身点降低IGD后，FULL优势仍存在。

### 正式论文指标规则

每个实例必须执行两阶段指标流程：

1. 所有正式算法、所有正式run完成后，合并其最终非支配集并严格Pareto过滤，生成唯一`reference-front.csv`；
2. 从冻结集合生成唯一`normalization.properties`和固定HV reference point；
3. 对该实例所有算法和所有run只读取上述冻结文件计算HV、IGD和Spacing；
4. 保存算法集合、run集合、front哈希、reference哈希、边界和reference point，禁止单个算法或单次run重算；
5. 增加leave-one-run-out IGD作为敏感性审计，但不替代论文主指标。

当前归一化实现会把超出reference边界的值裁剪到`[0,1]`。正式实现应确保冻结边界覆盖全部待比较结果，或取消上界裁剪并使用明确留裕量的固定HV reference point，避免较差点被裁剪后看起来更好。

## 运行时间根因

### 实测缩放

| 预算 | FULL | BASE | 倍率 | 说明 |
|---:|---:|---:|---:|---|
| 20k FE | 4.078–4.164 s | 1.085–1.180 s | 3.5–3.8x | 远端先导，同实例 |
| 100k FE | 53.626 s | 6.548 s | 8.19x | 隔离JFR，固定seed |
| 500k FE | 1068.963–1204.208 s | 27.651–30.359 s | 35–43x | 六seed正式诊断 |

### 根因一：CA-TA中位数反复全量排序

`ZhangBoCaTaStatistics.Bucket`保存每次调用的全部`wallClockNanos`和`fullEvaluations`。每次`Snapshot`都会复制整个历史并排序求中位数；`best()`对合法邻域排序时，比较器又会为左右邻域重复调用`snapshot()`。因此历史越长，单次动作选择越贵，累计成本接近超线性增长，而且这些排序不计入FE。

100k JFR CPU采样：

- `ComparableTimSort.mergeLo`：20.7%；
- `ComparableTimSort.mergeHi`：19.0%；
- `ComparableTimSort.binarySort`：17.0%；
- 三者合计：56.7%；
- `DhhfspFourVectorSolution.copyMutableValue/copyAttributes`直接叶子样本约10.4%，另有大量包含式复制调用；
- FULL发生20次young GC，BASE为7次，说明候选和历史复制也造成明显分配压力。

建议首先保持统计语义不变，将每个Bucket的中位数改成增量双堆或可删除/不可删除的在线中位数结构，并让一次`best()`先为每个合法邻域生成一次Snapshot再排序邻域。这个修改不改变邻域选择结果，只消除重复计算，预计是从35倍下降的最大单点收益。

### 根因二：CA-TA候选preview重复生成

每个全局后代先为合法掩码逐邻域执行`preview()`；真正Test/Apply时，`evaluateOne()`又重新执行同一邻域的`preview()`。O10–O13的preview会复制solution、重建关键DAG或回放预测，但不计FE。因此同一个父代/上下文/邻域候选至少可能生成两次。

建议为`parent fingerprint + context + neighborhood + seed-event`建立单代短生命周期缓存，掩码和评价共享同一不可变preview结果；上下文切换或父代变化立即失效。

### 根因三：复制、轨迹重建和事件字符串

FULL 100k产生20000个CFVF后代和79900个CA-TA局部评价；每个候选携带四向量及多层属性，深复制在JFR中占据显著样本。CA-TA还为每个后代重复计算瓶颈、Need和时间线，并保存大量完整事件字符串，而正式摘要最终只使用计数和哈希。

建议：

- 对同一已评价父代缓存trace派生的瓶颈与Need分量；
- 把运行期事件改成流式SHA-256 + 计数器 + 有界诊断ring buffer，完整事件只在debug模式保留；
- 增加`decoder / archive / PDDR / CA-TA preview / CA-TA evaluation / Q / logging`独立计时桶，后续每次正式运行直接输出成本分解。

### 次级风险：非支配历史的二次扫描

`updateParticlesMemory()`每代把候选加入无界`globallyOptimalIndividual`，随后用两层循环重新执行支配过滤；个人历史更新也有类似扫描。FULL约运行1000个外层代且最终front通常更大，BASE约545代。这不是本次JFR的第一热点，但在消除CA-TA排序后很可能成为下一瓶颈。

建议保持“无容量原始历史”的科研语义，先做精确的增量Pareto插入、指纹去重和被支配成员删除，不做容量截断；否则会改变算法搜索机制。

## 验收边界与下一步

本次没有修改生产算法，没有启动新的500k运行。100k JFR仅用于定位工程成本。

当前可以下结论：

- `FULL优势信号在统一reference和leave-one-seed-out下仍然存在`；
- `原55倍IGD不可作为正式论文数字`；
- `35–39倍运行时间主要包含可修复的未计FE统计/候选生成成本，不应直接视作算法理论复杂度`；
- `尚不能承诺优化后一定达到3–6倍，必须完成等价优化并以相同seed复跑100k和500k验证结果前沿、动作序列和评价预算不漂移`。

建议实施顺序：在线中位数/单次Snapshot → preview缓存 → trace/Need缓存 → 事件流式摘要 → 增量Pareto历史。每一步都必须先做固定事件字节级等价测试，再做100k性能门；只有达到目标后才重新跑500k和重建正式指标。
