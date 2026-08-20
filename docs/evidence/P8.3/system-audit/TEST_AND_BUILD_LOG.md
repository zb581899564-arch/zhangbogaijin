# P8.3测试与构建日志摘要

日期：2026-08-10

## 最终专项回归

```text
mvn -q -pl jmetal-algorithm,jmetal-exec -am -Djacoco.skip=true -DfailIfNoTests=false -Dtest=P8ExperimentRegistryTest,P8V3ExperimentRunnerTest,P8MetricCalculatorTest,P8InitialPopulationProviderTest,ZhangBoP8InitialPopulationProviderTest,ZhangBoP9SingleComparisonRunnerTest test
```

结果：

```text
P8 algorithm tests: 10 run, 0 failures, 0 errors
P8/P9 exec tests:   7 run, 0 failures, 0 errors
```

CA-TA、双Q、事件日志、增量Pareto和生产烟测最终定向结果：20项，0失败，0错误。`jmetal-problem`全模块结果：46项，0失败，0错误。

## 六模块打包

```text
mvn -q -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true package
```

结果：成功。第一次在PowerShell中未给带点号的属性加引号，被Maven解析成错误生命周期阶段；加引号后成功。这是命令行转义问题，不是源码或构建失败。

字节码检查：

```text
jmetal-algorithm/.../ZhangBoMOHPSOQ.class                     major=52
jmetal-exec/.../ZhangBoP83PerformanceSuiteRunner.class       major=52
```

## 旧核心回归边界

在JDK 17下加入：

```text
-DargLine=--add-opens=java.base/java.lang=ALL-UNNAMED
```

旧核心回归为651项、0失败、3错误、6跳过。3个错误与P1签名一致，来自作者默认构造器依赖模块工作目录下不存在的`EADHFSP/150_8_5_1.txt`，正式canonical生产路径不使用该构造器。

