# P8.3 CA-TA语义纠错、时间优化与系统复核报告

日期：2026-08-10  
状态：`completed`

## 结论

P8.3完成了先纠错、后优化的闭环：

1. 复现并修复了Apply在同一父粒子上重复评价完全相同候选的问题；
2. Apply预算现在跨后续父粒子调用持续执行，每次调用最多评价一个候选；
3. 代价信用已按总体v2改为平均wall-clock与平均完整评价次数的等权中位数归一化；
4. 新语义版本固定为`cata-apply-v2`；
5. 统计历史、preview、Pareto历史和正式事件日志完成等价优化；
6. I1编解码、人工核算和图1–6未变化，图7–11按纠错后真实机制链重建；
7. 20k和100k真实性能门通过，100k FULL/BASE中位时间比为5.042241×。

## CA-TA状态机

Test阶段对当前合法邻域全部执行相同`nTest`次数，随后记录`remainingApplyCalls=K×nTest×applyMultiplier`。每次新的父粒子调用最多执行一个Apply候选；预算归零、合法掩码变化或连续失败3次时重新Test。正式随机键包含master seed、generation、parent slot、lineage、context epoch、call ordinal和neighborhood。

代价排序依次使用成功次数、平均方向收益、归一化平均代价、历史调用次数和邻域编号。局部候选不回写本轮Qg/Qp奖励，预评价标记阻止外层重复计FE。

## 行为与论文示例

纠错后I1运行在4999 FE停止，Qg、Qp、CFVF、档案、CA-TA和PDDR均真实触发。279个Apply决策全部满足一决策一候选。事件日志改为有界环形缓冲后，I1显式完整捕获的12个核心文件与纠错后冻结版本字节级一致。

FM3/FM0的20工序解码、1400个中间字段、22个目标/诊断字段及图1–6哈希保持不变。图7–11已根据纠错后的lineage 121完整一代证据重新生成。

## 性能

- 20k：FULL 2322 ms，BASE 591 ms，比值3.928934×；
- 100k：FULL 11698 ms，BASE 2320 ms，比值5.042241×；
- 所有计时运行均完成，FE精确闭合，非法解和异常repair为0；
- JFR指定四类热点合计19.47%，低于25%；
- 剩余首要热点为深复制31.79%，未通过收益门的实验改动已回退。

正式真实时钟会参与CA-TA代价信用，因此相同seed的wall-clock敏感路径可随固定环境中的微小抖动变化。确定性时钟用于I1可重放解释，真实时钟用于成本评估；论文必须分别说明。

## 验收状态

```text
cata_apply_semantics_validated=true
cata_cost_credit_v2_aligned=true
performance_optimization_behavior_preserved=true
runtime_100k_gate_passed=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
formal_matrix_started=false
```

本阶段没有运行500000 FE、六seed复验、完整消融或正式统计矩阵。旧P9六seed结果仅保留为`legacy_pre_cata_apply_fix`诊断，不进入纠错后的正式reference或论文统计。

