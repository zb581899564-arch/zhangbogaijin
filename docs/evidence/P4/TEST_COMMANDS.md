# P4 验证命令

构建运行时：现有JDK 17（`JAVA_HOME=E:\javavava`）；Maven编译目标仍为Java 8。

```powershell
mvn -pl jmetal-problem,jmetal-algorithm -am '-Dtest=CanonicalEadhfspInstanceLoaderTest,FourVectorOperatorsTest,BaselineComponentsTest,OriginalNeighborhoodsTest,PublishedHmopsoQgsTest' '-DfailIfNoTests=false' '-Djacoco.skip=true' '-DargLine=' test
mvn -pl jmetal-exec -am -DskipTests package
java -Xmx2g '-Ddhfsp.data.dir=EADHFSP' '-Ddhfsp.output.dir=results' -cp 'jmetal-exec\target\jmetal-exec-5.8-jar-with-dependencies.jar' org.uma.jmetal.runner.multiobjective.P4HmopsoQgsSmokeRunner all
```

正式 `500000 FE` 配置只写入 `results/p4/table9-not-run.properties`，没有运行。
