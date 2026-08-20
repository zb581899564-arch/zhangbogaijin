# P3 原始解码优先验收报告

日期：2026-08-08  
状态：`completed`  
语义：`published_baseline`、`deterministic_canonical`、`author_actual`隔离  
评价预算：单元/黄金夹具内仅验证计数语义，未运行搜索实验

## 结论

P3已经完成论文规则解码、主动微调、约束保护右移、`Cmax/TEC/TWC`、可注入随机性、资源时间线、决策轨迹和jMetal 5.8问题适配。Fig.3完整编码得到三阶段20工序冻结轨迹；Fig.4只按可见结构独立保存，未被用来反向调参。

组件状态：

```text
decoder_engineering_validated=true
decoder_algorithm_aligned=true
engineering_validated=false
algorithm_aligned=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

这里的`decoder_algorithm_aligned=true`只表示解码组件逐规则映射到第四章来源，不表示原HMOPSO-QGS已闭环运行，更不表示论文实验复现。

## 实现范围

- 三种模式：`PUBLISHED_STOCHASTIC`、`DETERMINISTIC_CANONICAL`、诊断专用`AUTHOR_ACTUAL`；
- 第一阶段按JS位置访问并直接落实FA/MA/WA；
- 后续阶段ETC排序、FIFO破平、FAM、首轮工人无放回与后续最早可用工人；
- 初始追加式、机器—工人共同空档插入的主动微调、固定资源顺序的DAG右移；
- 工序、机器/工人时间线、随机/破平事件、约束报告和目标分解；
- `DhhfspProblem`固定三个目标，并只在成功`evaluate()`后计数一次；直接调用decoder不计评价；
- `AUTHOR_ACTUAL`通过隔离适配器调用未修改的`EDHHFSPW`，不接入生产Problem。

未实现：交叉、变异、粒子更新、HMOPSO、疲劳、CFVF、Qp、VNS或任何创新点。

## 黄金结果

Fig.3确定性最终结果：

```text
Cmax=60.68870523415978
processingEnergy=1982.7962256598619
standbyEnergy=28.636363636363647
TEC=2011.4325892962256
TWC=2602.9254079254083
rightShiftAccepted=true
```

右移相对微调保持两个工厂各自Cmax、资源顺序和TWC不变，TEC由`2023.106143015234`降到`2011.4325892962256`。完整三阶段数值与首位置人工核算见`P3_MANUAL_CALCULATION.md`。

## 测试与构建

| 门槛 | 结果 | 证据 |
|---|---|---|
| P3定向测试 | 10 tests，0 failures，0 errors | `maven-p3-targeted-tests.log` |
| `jmetal-problem`全部测试 | 21 tests，0 failures，0 errors | `maven-p3-problem-module-tests.log` |
| Java 8目标打包 | reactor 3/3 success | `maven-p3-package.log` |
| 根工程回归 | 651 tests，0 failures，3个P1既有errors，6 skipped | `maven-full-regression.log` |
| Java字节码 | 27个decoder class全部major version 52 | `P3_SCOPE_AND_BASELINE_AUDIT.txt` |
| 只读基线 | 1806文件，SHA差异0，可写文件0 | `P3_SCOPE_AND_BASELINE_AUDIT.txt` |
| 范围扫描 | 后续机制整词命中0 | `P3_SCOPE_AND_BASELINE_AUDIT.txt` |

根工程三个既有错误仍为：

1. `PMXCrossoverTest.shouldJMetalRandomGeneratorNotBeUsedWhenCustomRandomGeneratorProvided`
2. `PermutationSwapMutationTest.shouldJMetalRandomGeneratorNotBeUsedWhenCustomRandomGeneratorProvided`
3. `DefaultIntegerPermutationSolutionTest.shouldConstructorCreateAValidSolution`

三者都在作者`DefaultIntegerPermutationSolution`以`numberOfFactories=0`调用`Random.nextInt(0)`处报`bound must be positive`，名称、数量和原因与P1/P2完全一致；根reactor在`jmetal-core`停止，因此另行执行了`jmetal-problem`全部21项测试。

## 证据索引

- 来源映射：`P3_SOURCE_MAP.md`
- 三语义与作者差异：`P3_SEMANTIC_DIFFERENCES.md`
- 随机与破平：`P3_RANDOM_AND_TIE_EVENTS.md`
- 人工核算与目标：`P3_MANUAL_CALCULATION.md`
- 环境与命令：`P3_ENVIRONMENT_AND_COMMANDS.txt`
- 范围/基线审计：`P3_SCOPE_AND_BASELINE_AUDIT.txt`
- 源码与结果哈希：`P3_ARTIFACT_SHA256.csv`
- 三阶段完整轨迹：`java-jmetal58/jmetal-problem/src/main/resources/dfsp/chapter4/p3-fig3-*-deterministic.csv`

## 后续边界

P3通过后，路线图下一允许工作包为P4“原始算子与完整HMOPSO-QGS基线”。本工作包没有自动开始P4。四个全局验收标志继续为`false`，正式复现或实验矩阵仍需后续门槛及用户授权。

