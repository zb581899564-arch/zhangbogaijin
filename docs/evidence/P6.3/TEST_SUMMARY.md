# P6.3 测试与构建摘要

日期：2026-08-09  
运行时：Oracle JDK 17.0.12  
编译目标：Java 8

## 结果

- Qp配置与控制器定向测试：19项，0 failures，0 errors；
- P2–P6.3定向回归：82项，0 failures，0 errors；
- `ZhangBoP6IntegrationSmokeTest`：8项，0 failures，0 errors；
- `20_2_3_1` P6.3烟测：100粒子、2000 FE，1900个CFVF后代，1900次评价后PDDR选择，非法解0，CFVF后置repair 0；四类Qp动作均大于0；
- 固定显式初始种群连续3次600 FE：Qp事件、最终Q表和最终结果字节级一致；Qp seed改为`20260809`后出现差异；
- 六模块`mvn package`：BUILD SUCCESS；
- Java字节码：Qp核心类major version 52；
- 完整旧核心回归：651项、0 failures、3个P1既有errors、6 skipped，错误签名仍是`PMXCrossoverTest`、`PermutationSwapMutationTest`和`DefaultIntegerPermutationSolutionTest`的`bound must be positive`。

## 关键命令

```text
mvn -pl jmetal-algorithm -am -Djacoco.skip=true -Dmaven.javadoc.skip=true -Dtest=ZhangBoQpControllerTest,ZhangBoP62ConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl jmetal-exec -am -Djacoco.skip=true -Dmaven.javadoc.skip=true -Dtest=<P2-through-P6.3 directed set> -Dsurefire.failIfNoSpecifiedTests=false test
mvn -Ddhfsp.data.dir=<workspace>/EADHFSP -Ddhfsp.fatigue.dir=<workspace>/fatigue-parameters/v1 -Djacoco.skip=true -DargLine=--add-opens=java.base/java.lang=ALL-UNNAMED test
mvn -DskipTests -Djacoco.skip=true -Dmaven.javadoc.skip=true package
```

第一次未加`--add-opens`的完整回归产生旧Mockito/CGLIB在JDK 17上的模块访问级联错误，不作为最终回归结论；最终兼容命令恢复为P6.2同一组3个已知错误。
