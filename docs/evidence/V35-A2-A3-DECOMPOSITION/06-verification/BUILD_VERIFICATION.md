# 本地构建与定向回归记录

执行时间：2026-08-24（本地 Windows 环境）。

## 定向回归

```text
mvn.cmd -q -pl jmetal-algorithm \
  -Djacoco.skip=true \
  -Dtest=V35A2A3DecompositionConfigurationTest,V35A2A3TelemetryEquivalenceTest,V35ObservationOnOffEquivalenceTest \
  -DargLine=--add-opens=java.base/java.lang=ALL-UNNAMED test
```

结果：7 tests，0 failures，0 errors。完整控制台记录在`targeted-test.log`，退出状态在
`targeted-test-status.txt`。

覆盖的门：

- D1/D2/D3机制配置唯一性和非法组合拒绝；
- A3默认配置的canonical text不被诊断字段污染；
- A2→A3个人领导遥测开/关不改变初群、FE、评价轨迹、前沿及Q表哈希；
- 被动观察开/关行为等价；新事件捕获元数据在空流时真实写为`EMPTY`，不再固定写`false`。

## Java 8 构建物

```text
mvn.cmd -q -pl jmetal-exec -am -Dmaven.javadoc.skip=true -DskipTests package
```

结果：退出码0。`ZhangBoV35A2A3DecompositionRunner.class`的class major version为`52`，
即Java 8目标。机器可读状态在`java8-package-status.txt`。

这次构建只验证当前独立诊断源码；不覆盖、替换或重新声明冻结正式Jar。
