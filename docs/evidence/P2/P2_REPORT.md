# P2 论文算例与编码契约报告

## 结论

P2完成。第四章ESWA期刊版的表4、表5、Fig.3、Algorithm 2、Fig.5和Fig.6已经转成可追溯数据、Java 8四向量契约和固定测试。P2未实现解码、交叉、变异、修复、目标评价或任何创新机制。

本结论只证明论文黄金输入和编码契约已经建立，不表示解码或算法已经对齐，更不表示论文结果复现完成。

## 交付物

- Java领域类型：`java-jmetal58/jmetal-problem/src/main/java/org/uma/jmetal/problem/multiobjective/dfsp/model/`
- 黄金资源：`java-jmetal58/jmetal-problem/src/main/resources/dfsp/chapter4/`
- JUnit测试：`java-jmetal58/jmetal-problem/src/test/java/org/uma/jmetal/problem/multiobjective/dfsp/model/`
- 来源页渲染：`docs/evidence/P2/source-pages/`
- 来源映射：`P2_SOURCE_MAP.csv`
- 来源差异：`P2_SOURCE_DIFFERENCES.md`
- Algorithm 2语义：`P2_ALGORITHM2_SEMANTICS.md`
- Fig.5/Fig.6夹具：`P2_OPERATOR_FIXTURES.md`

## 来源核对

- ESWA PDF SHA-256：`BBBF3051E3B0B4F24A6B7FDC01DBAE7375D774E2467738A4DD8E49EAECACCF9D`。
- 学位论文 PDF SHA-256：`D835DCD5B15BF767F432F80835235BE740E7BD5BA0EF9DDB70437FB4EF91EC3A`。
- 已渲染并检查ESWA第7、8、9、12、14页及学位论文PDF第59–64页。
- 表4、表5和可见四向量逐值一致，没有`TODO_SOURCE_CONFIRMATION`。
- 学位论文第59页隐藏文本层曾抽取出另一组不可见数字；150 DPI渲染确认可见Fig.4-2与ESWA Fig.3一致，因此该数字串登记为文本层伪影而不是论文版本差异。
- Fig.6的特殊工人约束由学位论文正文明确：图例中工厂2第一阶段只有1名可选工人；它没有覆盖完整表5实例的2名工人。

## Java契约

- `DhhfspInstance`不可变保存加工/设置时间和机器/工人参数，所有数组输入输出均防御性复制。
- `DhhfspFourVectorSolution`复用jMetal 5.8的`PermutationSolution<Integer>`接口，显式维护`JS/FA/MA/WA`和三目标槽位。
- 论文资源保持1基编号，`Chapter4GoldenFixture`只在加载边界转换为0基运行态。
- `DhhfspEncodingValidator`检查完整排列、等长、空值、FA范围，以及由FA决定的第一阶段MA/WA合法域。
- `DhhfspEncodingCodec`使用固定UTF-8文本格式，显式记录`schemaVersion/semanticTag/indexBase`，拒绝重复键、缺失键、未知键和未知版本。
- `Chapter4OperatorFixtures`分别提供Fig.5八位置交换夹具和Fig.6六个五位置资源操作夹具，没有执行任何算子。
- `copy()`复制四向量、目标数组、属性容器及常用可变属性值；测试证明修改副本不会改变原解。

## 差异审计

相对P1最终工作副本，排除`target/results`后：

- 新增文件：12；
- 修改既有文件：0；
- 删除文件：0。

因此P2没有改动P1兼容修复、作者解码器、作者Solution、问题评价、算子或算法源码。只读基线的可写文件数仍为0。

## 构建与测试

### P2定向测试

执行结果：

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖内容：

- 表4、表5全部数值及维度；
- ESWA Fig.3的1基原值和0基运行态；
- JS完整排列及双向位置映射；
- 长度、空值、重复JS、非法FA/MA/WA；
- 防御性复制和Solution深复制；
- 0基/1基序列化往返及格式错误；
- Fig.5和Fig.6全部结构化输入、事件和预期结果。

日志：`maven-p2-targeted-tests.log`。

### 构建

`mvn -pl jmetal-problem -am -DskipTests package`成功。新增六个公开主类抽查均为class major version 52，即Java 8字节码。日志：`maven-p2-package.log`。

### 完整回归

按P1相同的JDK 17兼容参数和绝对`dhfsp.data.dir`运行根工程测试，结果仍为：

```text
Tests run: 651, Failures: 0, Errors: 3, Skipped: 6
```

三个错误名称、数量和原因与P1一致：作者`DefaultIntegerPermutationSolution`在通用mock问题的`numberOfFactories=0`下调用`Random.nextInt(0)`。P2没有修改该行为。根reactor因此在`jmetal-core`按预期非零退出，下游模块由P2定向测试单独验证。日志：`maven-full-regression.log`。

## 随机性与评价预算

```text
seed=20260808
random_source_invocations=0
complete_objective_evaluations=0
```

seed只作为工作包固定元数据保留；P2代码不创建或调用随机源。

## 未实现内容

- 新Solution尚未接入`EDHHFSPW.evaluate()`或作者HMOPSO-QGS主线；该接入属于P3及后续工作。
- Algorithm 2仅完成逐行语义和输入输出固化，没有执行第一阶段调度、ETC/FIFO/FAM、微调或右移。
- Fig.5/Fig.6只保存黄金事件，交叉、变异和修复实现属于P4。
- 没有计算`Cmax/TEC/TWC`，没有运行任何实验。

## 验收状态

```text
P2=completed
engineering_validated=false
algorithm_aligned=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

下一允许工作包为P3原始解码优先验收，必须由用户另行发起。
