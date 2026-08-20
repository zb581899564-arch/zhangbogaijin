# P6测试命令

所有命令在`java-jmetal58`执行，运行时使用现有JDK目录`E:\javavava`，编译目标仍为Java 8。

```powershell
mvn -pl jmetal-algorithm -am '-Dtest=ZhangBoQgControllerTest,ZhangBoCfvfUpdaterTest' '-DfailIfNoTests=false' '-Djacoco.skip=true' '-DargLine=' test
mvn -pl jmetal-exec -am '-Dtest=ZhangBoP6IntegrationSmokeTest' '-DfailIfNoTests=false' '-Djacoco.skip=true' '-DargLine=' test
mvn -pl jmetal-problem,jmetal-algorithm,jmetal-exec -am '-Dtest=Chapter4GoldenFixtureTest,DhhfspEncodingValidatorTest,DhhfspDecoderTest,FourVectorOperatorsTest,BaselineComponentsTest,OriginalNeighborhoodsTest,PublishedHmopsoQgsTest,ZhangBoFatigueModelTest,ZhangBoFatigueEvaluatorSyntheticTest,ZhangBoFatigueIntegrationTest,ZhangBoDirectDerivationSmokeTest,ZhangBoFatigueRunnerSmokeTest,ZhangBoQgControllerTest,ZhangBoCfvfUpdaterTest,ZhangBoP6IntegrationSmokeTest' '-DfailIfNoTests=false' '-Djacoco.skip=true' '-DargLine=' test
mvn '-DskipTests' '-Djacoco.skip=true' '-Dmaven.javadoc.skip=true' package
mvn '-Ddhfsp.data.dir=E:\学习\李明哲-毕业材料\张博改进\java-jmetal58\EADHFSP' '-Ddhfsp.fatigue.dir=E:\学习\李明哲-毕业材料\张博改进\java-jmetal58\fatigue-parameters\v1' '-Djacoco.skip=true' '-DargLine=--add-opens=java.base/java.lang=ALL-UNNAMED' test
```
