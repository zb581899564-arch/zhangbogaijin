# P6.1.1测试命令

所有命令在`java-jmetal58`执行，现有JDK 17仅作为构建运行时，Maven编译目标保持Java 8。

```powershell
mvn -pl jmetal-algorithm -am '-Dtest=ZhangBoEvaluatedPddrSelectorTest,ZhangBoPersonalArchiveTest,ZhangBoLineageCoordinatorTest,ZhangBoP62ConfigurationTest' '-DfailIfNoTests=false' '-Djacoco.skip=true' '-DargLine=' test
mvn -pl jmetal-exec -am '-Dtest=ZhangBoP6IntegrationSmokeTest' '-DfailIfNoTests=false' '-Djacoco.skip=true' '-DargLine=' test
mvn -pl jmetal-problem,jmetal-algorithm,jmetal-exec -am '-Dtest=Chapter4GoldenFixtureTest,DhhfspEncodingValidatorTest,DhhfspDecoderTest,FourVectorOperatorsTest,BaselineComponentsTest,OriginalNeighborhoodsTest,PublishedHmopsoQgsTest,ZhangBoFatigueModelTest,ZhangBoFatigueEvaluatorSyntheticTest,ZhangBoFatigueIntegrationTest,ZhangBoDirectDerivationSmokeTest,ZhangBoFatigueRunnerSmokeTest,ZhangBoQgControllerTest,ZhangBoCfvfUpdaterTest,ZhangBoEvaluatedPddrSelectorTest,ZhangBoPersonalArchiveTest,ZhangBoLineageCoordinatorTest,ZhangBoP62ConfigurationTest,ZhangBoP6IntegrationSmokeTest' '-DfailIfNoTests=false' '-Djacoco.skip=true' '-DargLine=' test
mvn '-DskipTests' '-Djacoco.skip=true' '-Dmaven.javadoc.skip=true' package
mvn '-Ddhfsp.data.dir=E:\学习\李明哲-毕业材料\张博改进\java-jmetal58\EADHFSP' '-Ddhfsp.fatigue.dir=E:\学习\李明哲-毕业材料\张博改进\java-jmetal58\fatigue-parameters\v1' '-Djacoco.skip=true' '-DargLine=--add-opens=java.base/java.lang=ALL-UNNAMED' test
```

