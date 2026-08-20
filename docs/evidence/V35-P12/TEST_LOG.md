# V35-P12/P13/P18 targeted verification

日期：2026-08-13

```text
jmetal-algorithm targeted V35 suite: 32 tests, 0 failures, 0 errors
jmetal-problem full suite: 67 tests, 0 failures, 0 errors
jmetal-exec/jmetal-algorithm compile: SUCCESS
V35FairComparisonSmokeTest: baseline=COMPLETED,fe=20000; full=COMPLETED,fe=20000
QG0/QG1 pairing: both COMPLETED, fe=20000, initialPopulationHash identical; QG1 teacherUses=200,
dominatedTeacherUses=0, DTUR=0, validityChecks=392, replacements=48, SCRR=0.122448979592.
DSCR summary is exported as independent properties and teacher-use/events CSV files.
```

一次全模块算法回归共执行201项，其中V35/P8及历史张博定向类均通过；剩余失败为jMetal旧Mockito/JDK兼容错误及既有NSGA-II旧测试错误，不是本轮改动新增的V35失败签名。

本轮只做I1/20k级别机制验证，未启动100k、500k、正式矩阵或多产品族。
