# P4.1 李明哲作者代码直接派生校正报告

日期：2026-08-08  
状态：`completed`  
生产退化语义：`author_actual`  
论文验证角色：`paper_verification_baseline`

## 结论

P4.1已从P1工作副本中的李明哲完整Java实验代码直接派生张博生产母线：

```text
ZhangBoHMOPSOQRun
→ ZhangBoMOHPSOQBuilder
→ ZhangBoMOHPSOQ
→ ZhangBoEDHHFSPW
```

四个作者源文件实施前后SHA-256完全不变；四个张博派生文件在撤销批准的类名、构造器名、类型引用和Runner算法名隔离后，与作者源逐字符一致。没有修改随机源、算法参数、编码、七目标槽位、评价、Q、局部搜索、PDDR、算子或个人历史流程。

本阶段支持：

```text
author_direct_derivation_validated=true
baseline_engineering_validated=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

其中`baseline_engineering_validated`仍只指P4论文验证基线，不能与本次作者直接派生状态混写。

## 直接派生映射

| 角色 | 作者源 | 张博派生源 | 允许差异 |
|---|---|---|---|
| 问题 | `EDHHFSPW.java` | `ZhangBoEDHHFSPW.java` | 类名、构造器名 |
| 算法 | `MOHPSOQ.java` | `ZhangBoMOHPSOQ.java` | 类名、构造器名、`EDHHFSPW`类型引用 |
| Builder | `MOHPSOQBuilder.java` | `ZhangBoMOHPSOQBuilder.java` | 类名、返回类型、构造类型引用 |
| Runner | `MOHPSOQRun.java` | `ZhangBoMOHPSOQRun.java` | 类名、Builder引用、算法标识`ZhangBo-MOHPSO-Q` |

精确原始/派生哈希见`SOURCE_DERIVATION_MANIFEST.csv`；规范化比较见`DERIVATION_NORMALIZED_DIFF.csv`。四组`normalized_equal`均为`True`。

## 双入口烟测

作者与张博路径均使用同一配置：

- 实例：`EADHFSP/20_2_3_1.txt`
- `swarmSize=100`
- 初始评价预算：`100`，不进入第二代
- `r=0.4`，JS交叉/变异`0.2/0.06`
- WA交叉/变异`0.4/0.1`
- MA交叉/变异`0.3/0.25`
- Q次数`40`，学习率/贪婪率`0.8/0.9`，局部搜索`40`
- 入口测试seed：`20260808`；作者代码内部仍保留未受控`Random/Collections.shuffle`

两条Runner均独立完成并产生七目标作者格式、四向量结果和非支配输出：作者入口输出43个解，张博入口输出34个解。数量不同是`author_actual`未受控随机性的可见证据，不属于派生差异，也没有被修改随机源来强行消除。

完整输出保存在`smoke-output/author`和`smoke-output/derived`，哈希见`SMOKE_OUTPUT_MANIFEST.csv`。`ZhangBoDirectDerivationSmokeTest`共3项，`0 failures / 0 errors`，覆盖算法闭环、结果形状、Runner签名及双Runner实际执行。

## 测试与构建

| 门槛 | 结果 | 证据 |
|---|---|---|
| 规范化源码差异 | 4/4完全一致 | `DERIVATION_NORMALIZED_DIFF.csv` |
| 作者源未改变 | 4/4 SHA-256一致 | `SOURCE_FREEZE_BEFORE.csv`、`SOURCE_DERIVATION_MANIFEST.csv` |
| 创新机制扫描 | 0命中 | `FORBIDDEN_MECHANISM_SCAN.txt` |
| P2–P4问题模块 | 22 tests，0 failures，0 errors | `TEST_P2_P3_P4_PROBLEM.log` |
| P4算法定向测试 | 10 tests，0 failures，0 errors | `TEST_P4_ALGORITHM_REACTOR.log` |
| P4.1直接派生/双Runner | 3 tests，0 failures，0 errors | `TEST_DIRECT_DERIVATION_RUNNERS.log` |
| Java 8打包 | 5模块全部SUCCESS | `BUILD_PACKAGE_FINAL.log` |
| 派生字节码 | 4类major version 52 | `JAVA8_BYTECODE.csv` |
| 旧完整回归 | 651 tests，0 failures，3个P1既有errors，6 skipped | `TEST_FULL_REGRESSION_FINAL.log` |
| 只读基线 | 1806文件，哈希差异0，可写文件0 | `BASELINE_INTEGRITY_CHECK.txt` |

本阶段37项治理、源码、测试和证据交付物的最终长度与SHA-256汇总在`P4_1_DELIVERABLE_SHA256.csv`。

旧完整回归的三项错误仍为：

1. `PMXCrossoverTest.shouldJMetalRandomGeneratorNotBeUsedWhenCustomRandomGeneratorProvided`
2. `PermutationSwapMutationTest.shouldJMetalRandomGeneratorNotBeUsedWhenCustomRandomGeneratorProvided`
3. `DefaultIntegerPermutationSolutionTest.shouldConstructorCreateAValidSolution`

三项均为P1登记的`bound must be positive`，数量和签名未变化；根reactor仍在`jmetal-core`停止，因此P2–P4与P4.1分别以定向测试覆盖。

## 冻结的后续边界

- P5疲劳只允许修改`ZhangBoEDHHFSPW`和张博专属辅助类；`δ=0`必须在相同输入和受控事件下退化到本次冻结行为。
- P6全向量与双Q只允许修改`ZhangBoMOHPSOQ/ZhangBoMOHPSOQBuilder`和张博专属组件。
- P7 CA-TA-VNS只允许修改张博派生搜索路径。
- `PublishedHmopsoQgs/DhhfspProblem`继续保留为`paper_verification_baseline`，只用于论文算例、公式和算子对照。
- 原作者类、只读基线、P2–P4成果和`ALLAlgorithmRun`均未修改或接入张博入口。
- 未实现疲劳、CFVF、Q-pbest、双Q、O10–O13或CA-TA；未执行500000 FE正式实验。

因此P4.1满足工程验收门，下一允许工作包为P5；P5不会在本阶段自动开始。
