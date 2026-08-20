# P8.3 CA-TA根因复现

日期：2026-08-10  
状态：confirmed_before_fix

## 根因

`ZhangBoCaTaController.decide()`把`K×nTest×applyMultiplier`作为当前Decision的`repetitions`返回；`ZhangBoMOHPSOQ.runCaTaLocalSearch()`随后在同一父粒子、同一`ZhangBoNeighborhoodRequest`和同一seed上循环该次数。因此Apply完整评价的是同一个确定性候选，而不是跨后续局部调用执行一次一个候选。

第二个偏差是`ZhangBoCaTaStatistics.best()`依次比较原始wall-clock中位数和FE中位数，没有实现总体v2的等权归一化`Cost`。

## 修复前源码SHA-256

```text
85165772b8b232bbc1e595094142f30da6fdf4859a15ca007b7d26b6a37c5354  ZhangBoCaTaController.java
c167c34fa34f2133856a9dafee177c893a3467aba9438c15f68e07c67e85a99c  ZhangBoCaTaStatistics.java
a7b1d16fe06fced1463a4680116f3f0c0ae256af0186978ae291a37bfa911d8e  ZhangBoNeighborhoodCandidateGateway.java
fdd1a97a4fcabeb4a6bae1d5b4d6bfe61c69111fab302b63e6b24d5c1fd2a85e  ZhangBoMOHPSOQ.java
```

## 失败测试

命令：

```text
mvn -q -pl jmetal-algorithm -am -Djacoco.skip=true -DfailIfNoTests=false -Dtest=ZhangBoCaTaControllerTest test
```

结果：`Tests run: 4, Failures: 2, Errors: 0, Skipped: 0`。

- Apply跨调用断言：期望`repetitions=1`，旧实现返回`2`。
- v2代价断言：等权归一化应选择`O2_JS_REVERSE`，旧实现按最小原始wall-clock选择`O3_JS_SWAP`。

该失败证明确认的是生产控制器根因，不是性能测量噪声。
