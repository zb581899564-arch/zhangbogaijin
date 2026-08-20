# P5 作者直接派生路径上的动态疲劳解码报告

日期：2026-08-08  
状态：`completed`  
生产语义：`fatigue_improved`  
退化语义：`author_actual`

## 结论

P5已在P4.1冻结的张博直接派生生产链上完成动态疲劳解码：

```text
ZhangBoMOHPSOQRun
→ ZhangBoMOHPSOQBuilder
→ ZhangBoMOHPSOQ
→ ZhangBoEDHHFSPW
→ ZhangBoFatigueEvaluator
```

默认构造器和所有`r_k=0`参数均直接执行原作者评价体；疲劳只有通过显式、与实例SHA-256匹配的参数清单才会启用。原作者`EDHHFSPW/MOHPSOQ/Builder/Runner`四个源文件SHA-256全部保持P4.1冻结值，P2–P4论文验证线也未被替换。

本阶段支持：

```text
fatigue_model_engineering_validated=true
fatigue_model_scheme_aligned=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

## 双路径硬门

- `new ZhangBoEDHHFSPW(...)`：原样进入作者评价体，不产生疲劳结果属性。
- 显式参数构造器或`withConfiguredFatigueParameters(...)`：先校验实例哈希和严格参数文件；任一`r_k>0`时进入疲劳评价器。
- `parameters.withZeroImpact()`：仍进入机械保留的作者评价体，不通过疲劳公式模拟退化。
- 七个目标槽保持作者结构：`0=Cmax`、`1=TEC`、`6=TWC`，`2–5`保留作者工厂索引含义。

`ZhangBoFatigueIntegrationTest`使用相同显式染色体比较默认路径和`r=0`路径，七目标及`time/jobEndPower/jobCost`三组内部矩阵逐项零容差相等，输入四向量和机器属性未被修改。

## 疲劳评价行为

疲劳路径保留作者JS/FA分厂顺序及最早可用机器选择。第一阶段由JS逆映射读取位置对齐WA；后续阶段只在当前工厂、阶段的合法工人集合内，按疲劳修正ECT选择工人，并以较小工人编号稳定破平。作者扩展WA中的后续阶段值不参与评价。

每道工序严格执行：

```text
前序完工与机器可用
→ 工人候选与工人可用
→ 最早开始
→ 自然恢复
→ 疲劳修正时长
→ 完工
→ 疲劳累积
→ 更新机器/工人状态
→ 记录轨迹
```

轨迹完整记录工件、阶段、工厂、机器、工人、前序/机器/工人可用时间、恢复时长、基础/实际时长、倍率、疲劳前后、能耗、成本及安全阈值事件。`Fsafe=0.90`只产生诊断，不插入休息、不改变可行性、不增加目标。

已实现的诊断为`Fmax/Favg/FE/Var(Fw)`、高疲劳比例、最长连续工作时长和自然恢复总时长。`FE`及高疲劳时长对工作、任务间恢复和末次任务到`Cmax`的尾部恢复使用解析积分；自然恢复总时长不计初始等待和尾部。

## 参数清单

- 目录：`java-jmetal58/fatigue-parameters/v1`
- 文件：45份实例清单加1份总manifest
- sampler：`sha256-keyed-u53-v1`
- seed：`20260808`
- `λ∈[0.01,0.03]`、`μ∈[0.03,0.07]`、`r=0.30`
- `δ=r/(λ ln2)`，不是独立参数
- `Fwarn=0.80`、`Fsafe=0.90`

严格UTF-8 codec拒绝缺字段、重复字段、未知schema、配置哈希错误、实例哈希错误、维度错误及非有限数值。参数审计见`PARAMETER_AUDIT.txt`，完整文件级映射见`java-jmetal58/fatigue-parameters/v1/MANIFEST.txt`。

论文表述边界见`PARAMETER_RATIONALE.md`：这些参数用于标准化异质实验场景，不代表真实工人的精确生理参数；真实数据拟合尚未进行。

## 作者兼容差异记录

作者`DefaultIntegerPermutationSolution`仍固定读取`150_8_5_1.txt`的资源域，可能为`20_2_3_1`等实例生成第一阶段非法WA。P5未修改该作者类；仅在疲劳显式启用的`ZhangBoEDHHFSPW.createSolution()`中，对新生成染色体按当前实例做最小合法域规范化。默认作者路径完全不变，外部传入的非法第一阶段WA仍由疲劳评价器直接拒绝，不进行隐式修复。

## 测试与构建

| 门槛 | 结果 | 证据 |
|---|---|---|
| P2/P3/P5问题定向回归 | 32 tests，0 failures，0 errors | `TEST_P2_P3_P5_PROBLEM_REACTOR.log` |
| P4算法验证线 | 1聚合测试通过 | `TEST_P4_ALGORITHM.log` |
| P4.1及P5 Runner | 4 tests，0 failures，0 errors | `TEST_P4_1_P5_RUNNERS.log` |
| 疲劳同输入重复评价 | 连续100次UTF-8结果字节级一致 | `ZhangBoFatigueIntegrationTest` |
| 45实例参数 | 维度、范围、派生δ、哈希及只读性通过 | `ZhangBoFatigueParameterCodecTest`、`PARAMETER_AUDIT.txt` |
| Java 8工程打包 | 6个reactor模块全部SUCCESS | `BUILD_PACKAGE_FINAL.log` |
| P5关键字节码 | 5个关键类major version 52 | `JAVA8_BYTECODE.csv` |
| 旧完整回归 | 651 tests，0 failures，3个P1既有errors，6 skipped | `TEST_FULL_REGRESSION_JDK17_COMPAT.log` |
| 作者源完整性 | 4/4冻结哈希不变 | `AUTHOR_SOURCE_INTEGRITY.csv` |
| 只读基线 | 1806文件，哈希差异0，可写文件0 | `BASELINE_INTEGRITY.txt` |
| 禁止机制扫描 | 0命中 | `FORBIDDEN_MECHANISM_SCAN.txt` |

完整旧回归的三个`bound must be positive`错误与P1–P4.1签名一致，根reactor仍在`jmetal-core`停止；P5新增测试均通过，没有新增失败。

验收期间三次因JDK 17模块参数或单模块依赖选择不正确而产生的诊断日志已在`SUPERSEDED_RUNS.md`中逐项标记，并分别指向最终替代证据；它们不参与上述最终结论。

## 可执行证据

`TRACE_20_2_3_1.txt`保存显式身份JS、轮转FA和合法第一阶段WA的40道工序确定性轨迹，包含七目标、七项疲劳指标和全部操作字段。P5 Runner以`20_2_3_1`、100粒子和100次初始评价贯通直接派生链；该烟测没有进入正式规模，也没有把作者算法内部未受控随机性描述为完整运行可重放。

## 明确未做

- 未实现CFVF、个人容量档案、Q-pbest、双Q、O10–O13或CA-TA-VNS；
- 未实现主动休息、多技能、第五染色体或第四目标；
- 未运行500000 FE、参数敏感性、正式多实例多种子或统计矩阵；
- 未进行真实工人生理参数拟合；
- 未设置`sampled_reproduction_accepted`或`full_reproduction_accepted`。

P5满足工程和方案对齐验收门。下一允许工作包为P6.1 CFVF独立验收，不自动开始。

P5源码、参数与证据的最终文件级SHA-256分别见`P5_SOURCE_SHA256.csv`和`P5_DELIVERABLE_SHA256.csv`。
