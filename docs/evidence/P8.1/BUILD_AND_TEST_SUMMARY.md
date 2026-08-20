# P8.1 构建与测试摘要

日期：`2026-08-10`

## 通过项

- `mvn -pl jmetal-problem -Djacoco.skip=true test`：46项，0 failures，0 errors。
- P8/双Q/基线/CA-TA定向测试：30项，0 failures，0 errors。
- `mvn -pl jmetal-exec -Djacoco.skip=true -DargLine=--add-opens=java.base/java.lang=ALL-UNNAMED test`：15项，0 failures，0 errors。
- 六模块`package`：成功；使用`-DskipTests -Djacoco.skip=true -Dgpg.skip=true -Dmaven.javadoc.skip=true`，保持Java 8编译目标。
- 普通FULL烟测：`fullFE=2000`、`caTaFE=1500`，Test/Apply/Event均非零。
- P8-v3：204/204 `COMPLETED`，FE范围1942–2000，非法解0，CFVF异常repair 0，初始四向量哈希无跨标签漂移。

## 保留的旧工程限制

- jMetal core在JDK 17兼容参数下仍为651项、0 failures、3个P1已登记errors、6 skipped；错误均来自旧`DefaultIntegerPermutationSolution`。
- 单独执行旧algorithm完整测试时，另暴露`NSGAIIBuilderTest`期望25000而作者源码默认3000的既有不一致。当前源码和测试与只读基线SHA-256完全相同，P8.1未引入也未修改该NSGA-II旁支；正式canonical HMOPSO-QGS不调用该路径。
- 旧Javadoc插件在未设置`JAVA_HOME`时找不到`javadoc`，因此工程打包显式跳过Javadoc附件；主源码编译与产物打包成功。

上述限制不得被写成P8.1新机制失败，也不得被用于宣称旧作者全工程已经净化。P8.1的“净化”限定为正式生产调用链不再使用这些缺陷类。
