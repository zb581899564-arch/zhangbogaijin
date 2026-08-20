# P6.4 测试与构建摘要

日期：2026-08-09  
运行时：Oracle JDK 17.0.12  
编译目标：Java 8

## 结果

- P6.4调度、配置及控制器新增定向测试：17项，0 failures，0 errors；
- P2–P6.4定向回归：101项，0 failures，0 errors；
- `ZhangBoP6IntegrationSmokeTest`现有10项全部通过，其中P6.4包含2000 FE闭合测试及三次固定种群重放/异seed测试；
- P6.3同步入口定向烟测继续通过，旧工厂不写入`dualQ.*`配置字段；
- 六模块`mvn package`：BUILD SUCCESS；
- P6.4核心类字节码major version 52；
- 完整旧核心回归：651项、0 failures、3个P1既有errors、6 skipped，错误签名未变化。

## 关键命令

```text
mvn -pl jmetal-algorithm -am -Dtest=ZhangBoDualQCoordinatorTest,ZhangBoP62ConfigurationTest,ZhangBoQgControllerTest,ZhangBoQpControllerTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true test
mvn -pl jmetal-exec -am -Dtest=<P2-through-P6.4 directed set> -Dsurefire.failIfNoSpecifiedTests=false -Ddhfsp.data.dir=<workspace>/EADHFSP -Ddhfsp.fatigue.dir=<workspace>/fatigue-parameters/v1 -DargLine=--add-opens=java.base/java.lang=ALL-UNNAMED -Djacoco.skip=true test
mvn -Ddhfsp.data.dir=<workspace>/EADHFSP -Ddhfsp.fatigue.dir=<workspace>/fatigue-parameters/v1 -DargLine=--add-opens=java.base/java.lang=ALL-UNNAMED -Djacoco.skip=true test
mvn -DskipTests -Djacoco.skip=true -Dmaven.javadoc.skip=true package
```

完整`mvn test`按既有验收口径在`jmetal-core`的三项作者构造器错误处停止；后续张博模块通过独立定向回归验收，不修改这些P1已登记错误。
