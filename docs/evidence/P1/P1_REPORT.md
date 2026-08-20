# P1 原始代码快照与jMetal 5.8工作副本报告

## 结论

P1完成。只读基线和可修改工作副本均由作者当前dirty working tree建立；原源内容未变化。工作副本已经消除Maven模块循环、重复依赖和 `E:/DHFSP-4` 活跃路径，并在现有JDK 17上以Java 8目标完成六模块Maven打包。

本结论只表示P1工程基线建立成功，不表示编解码、算法或论文结果已经复现。

## 交付

- 只读基线：`baseline-li-jmetal58/`
- 工作工程：`java-jmetal58/`
- 来源、哈希、差异、构建和测试证据：`docs/evidence/P1/`

## 内容审计

- 来源纳入文件：1806；有效实例：45；总字节：210449564。
- 来源实施前后SHA-256差异：0。
- 来源实施前后Git porcelain状态差异：0。
- 来源与基线初始差异：0；基线与工作副本初始差异：0。
- 基线只读文件：1806/1806。
- 最终工作副本排除 `target/results` 后仍为1806项，与基线仅6项不同：2个POM和4个路径兼容Java文件。
- 工作副本中 `E:/DHFSP-4` 命中：0。
- 未修改解码规则、粒子更新、交叉、变异、局部搜索或任何创新机制。

## 兼容修改

1. 删除 `jmetal-algorithm` 对 `jmetal-exec` 的无效依赖；
2. 删除 `jmetal-exec` 中重复的 `jmetal-algorithm` 依赖；
3. `dhfsp.data.dir` 控制实例目录，默认 `EADHFSP`；
4. `dhfsp.output.dir` 控制输出根目录，默认 `results`；
5. `ALLAlgorithmRun` 自动创建 `data-result/50-percent/doe/indicators` 子目录；
6. `DefaultIntegerPermutationSolution` 仍读取作者原定的 `150_8_5_1.txt`，只改路径，不改语义。

## 构建和测试

- `mvn -DskipTests validate`：成功，六模块reactor顺序有效，不再存在循环或重复依赖警告。
- 首次 `package`：因进程未设置 `JAVA_HOME`，旧Javadoc插件找不到已存在的 `javadoc.exe`；证据保留。
- 仅在构建进程设置 `JAVA_HOME=E:\javavava` 后，`mvn -DskipTests package`：六模块全部成功。
- 生成的638个主class文件全部为major version 52，即Java 8字节码。
- 默认实例目录烟测：`EDHHFSPW(20,2,3,1)`成功读取，得到20个变量、7个作者实际目标槽位。
- 自定义输出目录烟测：四个子目录均成功创建。

## 已知测试限制

原 `mvn test` 被JaCoCo 0.7.7在JDK 17上的代理兼容问题阻断，测试数为0。使用 `-Djacoco.skip=true` 和JDK模块开放参数后，核心模块执行651项测试：0 failures、3 errors、6 skipped，即642项通过。

剩余3项为：

- `PMXCrossoverTest.shouldJMetalRandomGeneratorNotBeUsedWhenCustomRandomGeneratorProvided`
- `PermutationSwapMutationTest.shouldJMetalRandomGeneratorNotBeUsedWhenCustomRandomGeneratorProvided`
- `DefaultIntegerPermutationSolutionTest.shouldConstructorCreateAValidSolution`

三项均因作者改写的 `DefaultIntegerPermutationSolution` 在通用jMetal mock问题中取得 `numberOfFactories=0` 后调用 `Random.nextInt(0)`。这是现有 `author_actual` 语义问题；P1不修改算法源码来掩盖。下游模块主源码和测试源码已在成功的 `package` 中编译，但完整测试reactor会在core的这3项错误处停止。

另保留作者POM中TestNG动态 `RELEASE` 版本警告和Javadoc警告，本阶段不主动升级依赖。

## 验收状态

```text
engineering_validated=false
algorithm_aligned=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

下一阶段只能进入P2论文算例与编码契约，并需用户另行发起。
