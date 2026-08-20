# V35-P25C BAL全开放三Seed验证报告

## 范围

本轮只验证P25B held-out失败后的安全正式语义，不重新校准压力阈值：

```text
instance=20_2_3_1
seeds=20260819,20260820,20260821
population=100
MaxFEs=100000
arms=A0,A4,A5
decoder=FM3
ShiftMode=NONE
familyMode=DEGENERATE_SINGLE_FAMILY
setupMode=SEQUENCE_INDEPENDENT
pressureClassifier=diagnostic_only
actualBottleneck=BAL
strictPressureMask=false
actionMask=N1|N2|N3|N4|N5
shadowEnabled=false
```

每条运行使用独立JVM；三个seed采用轮换arm顺序。9条运行均`COMPLETED`，每条FE精确为100000。同seed三臂的初始四向量种群哈希一致，非法Shift传播和CFVF repair均为0。

## 统一参考结果

参考前沿只在9条运行全部完成后，由全部最终前沿合并并严格Pareto过滤得到，规模为350。诊断判定为：

```text
A4_PREFERRED_SIGNAL
```

核心汇总：

| Arm | median HV | median IGD | median Spacing | median time (s) |
|---|---:|---:|---:|---:|
| A0 | 0.772928 | 0.135882 | 0.040086 | 3.495 |
| A4 | 0.853561 | 0.076575 | 0.036656 | 10.340 |
| A5 | 0.815153 | 0.095213 | 0.030077 | 13.565 |

- A4相对A0的HV胜出2/3，覆盖优势中位数为`+0.07524`；
- A5相对A0的HV胜出2/3，覆盖优势中位数为`+0.06503`；
- A5相对A4仅1/3 seed的HV胜或平；A5−A4 HV中位差为`−0.10710`，覆盖优势中位数为`−0.65221`；
- A4的中位运行时间约为A0的2.96倍，A5约为A4的1.31倍。

## 极值边界

三seed的极值中位数为：

| Arm | Cmax | TEC | TWC |
|---|---:|---:|---:|
| A0 | 194.21 | 8618.24 | 12584.35 |
| A4 | 198.54 | 8648.56 | 12692.71 |
| A5 | 194.83 | 8591.52 | 12858.53 |

这说明A4当前信号来自整张三目标前沿的HV、IGD和覆盖关系，而不是三个单目标极值全面领先。尤其Cmax极值没有形成稳定优势，不能写成“FULL在全部目标上优于基线”。

## 机制真实性

A4/A5每条运行均真实产生：

```text
CFVF offspring=20000
Qp actions=15000
archive insertions=400
CA-TA-Lite FE=748..845
DSCR dominatedTeacherUses=0
```

A5三个seed的方向教师池请求分别为266、224、164，且全部发生真实过滤，因此A5相对A4的回吐不是“教师池没有触发”。A0的CFVF/Qp/档案/CA-TA-Lite/教师池计数均为0，基线与创新链没有机制泄漏。

## 重放核验

独立JVM重跑`seed=20260819/A4`：

- `front.csv`、初始种群、配置、CA-TA-Lite事件、DSCR事件、教师使用、Cmax曲线/纪录和压力事件均字节级一致；
- 机制摘要去除真实计时字段后完全一致；
- 计时值按设计不要求字节级一致，也不参与算法决策。

因此本轮没有观察到同JVM历史或墙钟代价导致的动作漂移。

## 结论边界

当前最合理的论文主版本候选是A4，即关闭方向top-k教师池的完整创新链。方向教师池保留为可选Cmax导向模块，不应默认纳入FULL主版本。

本轮只有3个seed、单实例、100000 FE，属于主版本诊断门，不是显著性检验或论文最终实验。不得据此启动正式矩阵、宣称统计优越性或重新调压力分类阈值。

```text
bal_open_safe_semantics_validated=true
a4_preferred_engineering_signal=true
a5_directional_pool_default=false
formal_matrix_started=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```
