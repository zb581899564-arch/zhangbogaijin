# V35 A2→A3 固定20工件 50k 纯观察诊断报告

审计日期：2026-08-24  
实例：`20_2_3_1`  
种子：`20260822/20260823/20260824`  
臂：`A2_CFVF`、`A3_QP_PERSONAL_ARCHIVE`  
预算：每条 `MaxFEs=50000`，population=100

## 1. 裁决

```text
a2_a3_root_cause = COMPOSITE_BLOCK_UNRESOLVED
a2_a3_strongest_fault_candidate = QP_SELECTION_OR_REWARD_NUMERICAL_INSTABILITY
budget_or_provenance = RULED_OUT
dual_q_phase_skew = NOT_OBSERVED
personal_archive_capacity_collapse = NOT_OBSERVED
new_ablation_arm_started = false
```

本轮已经定位到一个可复现的 Qp 奖励数值不稳定：方向奖励使用

```text
(oldPhi-newPhi)/(abs(oldPhi)+epsilon)
```

当冻结归一化边界使 `oldPhi≈0`、而子代方向变差时，分母接近 epsilon，奖励可被放大到极端负值。
在 seed `20260822` 的保留事件中，最小 direction 为 `-3.330617191379857E10`；三个 seed 的
动作级平均奖励也出现 `-10^8` 至 `-10^9` 量级。该现象是直接的代码与事件证据，不是指标推断。

但 A3 相对 A2 同时加入个人档案、Qp 选择与 block-frozen 双Q时序。现有两臂不能分别估计这三项
对最终性能的独立因果效应。因此按照预注册停止条件，最终根因保持
`COMPOSITE_BLOCK_UNRESOLVED`；不能把“Qp奖励存在缺陷”偷换成“已单独证明全部A3退化由它造成”。

## 2. 公平性和预算门

- 六条运行全部 `COMPLETED`，每条 `fullEvaluations=decoderCalls=50000`。
- 非法解、重复评价均为0；每条运行的25个清单文件全部反向验证，合计150项、失败0。
- 同一seed的A2/A3初始种群SHA-256完全相同。
- 两臂实例、FM3、ShiftMode.NONE、PDDR=`GLOBAL_ORIGINAL`、provenance及外部预算口径相同。
- 因此 `BUDGET_OR_PROVENANCE` 被排除，PDDR也不是A2→A3发生变化的机制。

## 3. 三seed质量结果

统一参考前沿由六条最终完整前沿合并后严格Pareto过滤得到，共186点。

| seed | ΔHV(A3/A2) | IGD改善(A3相对A2) | Cmax改善 | 结论 |
|---|---:|---:|---:|---|
| 20260822 | -27.94% | -445.45% | +2.33% | Cmax极值改善，但整体前沿显著退化 |
| 20260823 | -9.51% | -43.47% | -0.78% | 全局质量退化 |
| 20260824 | -5.67% | -73.74% | -2.32% | 全局质量退化 |

中位数：`ΔHV=-9.51%`、`IGD改善=-73.74%`、`Cmax改善=-0.78%`。因此当前短程诊断支持
“A3组合块在这三个seed上不稳定且整体质量变差”，但不构成正式统计结论。

## 4. 机制排除与剩余解释

### 4.1 双Q阶段

三个A3运行的阶段计数完全一致：`warmup=49`、`P-block=26`、`G-block=25`。没有观察到
seed间或预算造成的阶段偏斜，故 `DUAL_Q_PHASE_SKEW` 不成立；这不等于证明block-frozen时序
本身无贡献，只说明它按配置执行且没有计数漂移。

### 4.2 个人档案

每条A3均有200次插入；容量截断均为0。支配移除分别为16/22/38，重复移除分别为50/43/36。
没有证据支持“容量6档案因持续截断而坍塌”。档案候选质量及其与Qp选择的耦合仍无法从两臂中
独立分离。

### 4.3 Qp选择与奖励

三个seed分别执行5100次Qp动作，pbest切换为1529/654/442次。保留日志中的700条奖励事件显示：

- direction中位数约为 `-0.238/-0.209/-0.191`；
- direction小于-1的事件数为153/125/87；
- seed20260822至少出现1条小于 `-1e6` 的极端方向奖励；
- 汇总平均值显示被滚动日志淘汰的更早事件中仍存在极端负值。

源代码位置为 `ZhangBoQpController.reward()`。当前归一化只在分母添加epsilon，没有对接近0的
基准值采用尺度稳定的对称归一化或有限界奖励，因此这是后续最小修复计划应优先处理的候选。

## 5. 停止边界

本轮不新增第三臂去拆分Qp/档案/双Q，不修改冻结Jar，不恢复4500正式矩阵。若用户批准下一阶段，
只能先提出“奖励尺度稳定化”的单变量设计，并必须证明修复前后除奖励数值外，预算、随机流和机制
开关均受控；是否需要进一步拆分个人档案与双Q，须由该最小实验结果另行决定。

母表：`fixed20-run-summary.csv`、`fixed20-paired-metrics.csv`、
`fixed20-qp-reward-anomalies.csv`；原始运行位于 `raw/A2` 与 `raw/A3`。

