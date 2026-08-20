# P9 实验计划（FULL优先、两算法单次决策版）

版本：`v1.2`  
日期：`2026-08-10`  
状态：`single_comparison_completed_waiting_user_decision`

## 1. 当前最高优先级

当前不直接运行论文全部对比算法，不直接执行34项消融，也不直接启动45实例正式矩阵。

唯一最高优先任务是：

```text
先运行一次 ZHANGBO-FULL
→ 再运行一次 HMOPSO-QGS-F
→ 在完全相同的问题、实例、seed、初始种群和FE预算下比较
→ 根据差距决定是否继续扩大
```

执行结果（2026-08-10）：上述两次单次运行与诊断报告均已完成；当前停止扩大，等待用户决定。

这里：

- `ZHANGBO-FULL`包含疲劳解码、CFVF、容量6谱系档案、预热与分块冻结双Q、O1–O13及完整CA-TA（含FAT上下文）；
- `HMOPSO-QGS-F`保留李明哲HMOPSO-QGS搜索骨架，只适配到我们的规范FM3疲劳问题，不加入CFVF、Q-pbest、双Q冻结或CA-TA创新；
- 两者都使用规范生产解码和正确四向量身份映射，不使用作者已确认有缺陷的`author_actual`评价路径。

## 2. 第一轮单次对比设置

### 2.1 固定输入

- 实例：`EADHFSP/20_2_3_1.txt`；
- 实例类型：20工件、2阶段、3工厂；
- seed：`20260808`；
- 种群规模：`100`；
- 最大完整评价：`500000 FE`；
- 子群规模：ESWA Table 9的`M3={20,20,20,40}`；
- 同一实例扩展/SUT清单；
- 同一疲劳参数清单；
- 同一初始四向量种群及SHA-256；
- 同一三主目标：`Cmax/TEC/TWC`，对应七槽载体`objective[0]/[1]/[6]`；
- 同一完整评价计数器。

先选择`20_2_3_1`是因为它属于论文正式45实例、已完成规范解码和先导链路验收，能以较低成本发现500000 FE下的机制、预算或结果异常。

### 2.2 运行顺序

1. 先运行`ZHANGBO-FULL`一次；
2. FULL成功且证据完整后，再运行`HMOPSO-QGS-F`一次；
3. 两次运行不得并行，避免资源波动妨碍首次诊断；
4. 两次运行分别保存独立日志和结果，但共享输入及初始种群哈希；
5. 任一运行失败时停止，不继续扩大。

### 2.3 FULL固定机制

- 解码：`FM3 FATIGUE_AWARE_SELECTION`；
- 疲劳参数：`lambda∈U(0.01,0.03)`、`mu∈U(0.03,0.07)`、`r=0.30`；
- 阈值：`Fwarn=0.80`、`Fsafe=0.90`，只作诊断，不插入主动休息；
- 资源飞行：完整CFVF，`FMW/MW/M/W`耦合动作；
- CFVF参数：`c1R=c2R=0.4`、`omegaR=0.5`、`pExplore=0.05`；
- 个人档案：每谱系容量`6`；
- Qp：四动作、16状态，`alphaP=0.30`、`gammaP=0.80`，探索率从`0.30`线性降到`0.05`；
- Qg：三动作、两状态，`alphaG=1.0`、`gammaG=0.8`、`epsilonG=0.8`；
- 双Q：前10% FE预热，之后以`B=5`个外层代交替执行P-block/G-block；
- VNS：O1–O13；
- CA-TA：六类瓶颈、80% Need/20%探索、`nTest=1`、`applyMultiplier=1`、Apply探索率`0.10`、连续失败阈值3；
- O13只通过JS或WA形成自然恢复窗口，不使用休息基因。

### 2.4 李明哲基线固定机制

`HMOPSO-QGS-F`使用：

- 与FULL相同的FM3疲劳问题、实例、SUT、参数、初始种群和目标；
- 李明哲HMOPSO-QGS规范搜索骨架；
- 原Q-gbest；
- 评价后PDDR-FF；
- 原工厂间搜索；
- 固定O1–O9；
- 原GA式FA/MA/WA更新，不使用CFVF；
- 不使用容量6谱系档案、Q-pbest、分块冻结双Q、O10–O13或CA-TA。

其Table 9参数固定为：

- `population=100`；
- `M3={20,20,20,40}`；
- `r1,r2~U[0,0.6]`；
- `lv2/lv3/lv4=0.2/0.5/0.5`；
- `mv2/mv3/mv4=0.08/0.15/0.25`；
- `Q_Times=50`；
- `LS_Times=30`；
- `gamma=0.8`；
- `epsilon=0.8`；
- `MaxFEs=500000`。

## 3. 首轮比较内容

单次运行不做显著性检验，也不能形成论文结论。本轮只回答“我们的FULL在同一问题上是否显示出值得继续的改善信号”。

比较以下内容：

### 3.1 结果质量

- 两个最终非支配集；
- `Cmax/TEC/TWC`的最小值、中位点和范围；
- 双向`C-metric`；
- 在两次结果合并形成的临时工程参考集上计算IGD、HV和Spacing；
- 非支配解数量。

上述参考集只用于单次诊断，不作为正式论文参考前沿。

### 3.2 疲劳和资源表现

- `Fmax/Favg/FE/Var(Fw)`；
- 高疲劳比例；
- 最长连续工作时长；
- 自然恢复总时长；
- 机器和工人负载不均衡；
- 是否通过更合理的MA/WA和JS产生恢复窗口。

### 3.3 机制和预算

- 实际完整评价数及停止原因；
- 非法解、异常repair、重复评价、来源丢失；
- CFVF四向量变化和资源动作比例；
- Qg/Qp动作、奖励和Q表变化；
- P/G-block阶段是否按外层代正确切换；
- CA-TA Test/Apply、上下文、FAT命中和局部FE；
- wall-clock和CPU time。

### 3.4 决策规则

首轮比较不设置“必须每个目标都更好”的不科学硬阈值，而按以下证据判断：

- 若FULL的非支配集明显覆盖基线、至少部分主要指标改善、机制事件真实发生且没有预算/合法性异常，则建议进入扩大验证；
- 若结果接近，先增加到3个seed复核随机波动，不立即启动45实例；
- 若FULL明显退化，先定位是疲劳模型、CFVF/双Q还是CA-TA造成，不继续扩大；
- 若出现非法解、repair、重复FE或机制空路径，视为工程失败，先修复。

## 4. 执行批次

### Batch 0：两算法正式单次Runner准备

- 建立只支持`ZHANGBO-FULL`和`HMOPSO-QGS-F`的P9比较Runner；
- 固定`20_2_3_1`、seed`20260808`、population 100、500000 FE；
- 验证共同初始种群、配置、实例、SUT、疲劳参数和源码哈希；
- 验证断点续跑、原子写入、失败记录和FE计数；
- 本批不运行优化实验。

### Batch 1：先跑ZHANGBO-FULL一次

- 训练机资源预检；
- 单JVM运行`ZHANGBO-FULL`；
- 验收结果、机制事件、FE、合法性和日志；
- 通过后才进入Batch 2。

### Batch 2：再跑HMOPSO-QGS-F一次

- 使用与Batch 1完全相同的输入和初始种群；
- 单JVM运行规范李明哲基线；
- 生成两算法单次比较报告。

### Batch 3：结果决策门

- 向用户报告两算法目标、Pareto关系、疲劳、机制和运行成本差异；
- 由用户决定：停止、增加3个seed、增加代表实例，或进入正式矩阵；
- 未经该决定不得自动扩大。

### Batch 4：可选小范围复核

仅在单次结果需要排除偶然性时执行：

- 保持同一实例，扩展到3个seed；或
- 选择小/中/大3–5个实例，每个3个seed；
- 仍然只比较FULL和HMOPSO-QGS-F。

### Batch 5：最终正式两算法矩阵

只有前述决策门通过且用户批准后执行：

- 实例：论文45实例；
- 算法：`ZHANGBO-FULL`与`HMOPSO-QGS-F`；
- 每算法、每实例独立运行`20`次，不再使用30次；
- seeds：`20260808..20260827`；
- population：100；
- MaxFEs：500000。

规模为：

```text
2算法 × 45实例 × 20次 = 1800条正式运行
完整评价上限 = 900000000 FE
```

## 5. 其他论文算法

MOPSO-F、MOPSODS-DE-F、MOHEADE-F、NSGA-II-F、SPEA2-F、QMOEA-F和HMOPSO-QLS-F不再是当前前置任务。

- 是否增加其中部分算法，由FULL与HMOPSO-QGS-F的结果、论文写作需要和计算成本共同决定；
- 不要求一次性把论文8种算法全部适配；
- `QMOEA`来源尚未确认，但该问题不阻塞当前两算法单次比较或最终两算法矩阵；
- 任何后来加入的算法仍必须使用同一FM3问题、20个seed、共同初始种群和500000 FE。

## 6. 消融设置（最后执行）

消融仍保留P8-v3五组34项设计，但不在当前两算法实验前执行：

- 疲劳：`FM0–FM3`；
- 全向量：`FV0–FV5/FV-Full`；
- Q-pbest：`QP0–QP6`；
- VNS：`V0–V5/V-Full`；
- 完整组合：`B0–B7/FULL`。

执行原则：

1. 只有FULL与HMOPSO-QGS-F的正式比较值得继续时，才制定消融运行范围；
2. 优先运行能直接解释三个创新点的核心B链，不默认一开始就跑34项全矩阵；
3. 若最终批准完整消融，每个标签使用45实例、20个seed、500000 FE；
4. 不同解码语义不得混用参考前沿；
5. 机制向量完全相同的运行才可复用，并记录`sourceRunId`；
6. 消融最后执行，不得反过来阻塞我们自己的FULL结果。

完整34项若全部批准，其规模为：

```text
34标签 × 45实例 × 20次 = 30600条标签级运行
完整评价上限 = 15300000000 FE
```

该规模当前仅作远期上限，不代表已经批准执行。

## 7. 正式指标与统计

当运行次数扩大到20次后，正式报告包括：

- IGD、HV、Spacing、双向C-metric；
- 非支配解数量、CPU time和wall-clock；
- Cmax、TEC、TWC；
- Fmax、Favg、FE、Var(Fw)、高疲劳比例、最长连续工作和自然恢复；
- Wilcoxon比较；
- 多算法时才使用Friedman及Holm校正；
- 效应量和95%置信区间；
- `alpha=0.05`。

同一实例、同一解码语义的全部正式结果完成后才冻结参考前沿。单次和3-seed预检结果不得混入20次正式前沿。

## 8. 训练机与证据

- 使用项目专属目录和独立tmux会话；
- Java 11运行Java 8字节码，jMetal 5.8；
- 算法主要使用CPU，不占用GPU；
- 首次单次比较使用单JVM，避免并发噪声；
- 扩大时的并发数根据两次500000 FE实测耗时和内存决定；
- 每条运行保存配置、实例、SUT、疲劳参数、源码、初始种群和结果哈希；
- 失败记录保留真实已消耗FE，不覆盖、不伪装为0；
- 正式证据进入`docs/evidence/P9-formal/`。

## 9. 当前状态

```text
P9=planned_two_algorithm_single_run_first
batch0_runner_ready=false
full_single_run_completed=false
hmopso_qgs_f_single_run_completed=false
single_run_comparison_completed=false
formal_20_run_matrix_started=false
ablation_started=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

下一步只允许实施Batch 0。Batch 0准备度报告通过并得到用户批准后，先启动一次`ZHANGBO-FULL`；它通过后才运行一次`HMOPSO-QGS-F`。任何扩大都必须经过Batch 3结果决策门。
