# P4 原始算子与完整 HMOPSO-QGS 基线报告

日期：2026-08-08  
状态：`completed`  
目标语义：`published_baseline`

## 结论

P4已在P1复制出的李明哲jMetal 5.8工作工程中形成独立三目标闭环。原 `MOHPSOQ` 未修改；`EDHHFSPW` 保持P1路径兼容后的哈希，P4未再修改。只读基线1806个文件与P1清单逐文件复核，哈希差异0、可写文件0。

本阶段支持：

```text
baseline_engineering_validated=true
baseline_algorithm_aligned=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

这里的“算法对齐”仅指已批准的组件语义、短程闭环和固定种子工程证据，不代表论文正式规模复现。

## 已实现

- `HmopsoQgsConfiguration`冻结ESWA Table 9配置与2000 FE工程配置；`alpha=1.0`显式标记为`author_actual_compatibility`。
- `FourVectorOperators`实现Fig.5 JS交换序列，以及FA/MA/WA两点闭区间交叉、变异和最小资源修复，所有随机源通过`PseudoRandomGenerator`注入。
- `SubSwarmDecomposer`实现三个单目标边界群和一个PDDR-FF中心群；允许同一来源解跨群复制。
- `PddrFf`实现三目标严格Pareto支配及稳定指纹破平。
- `QGbestController`为每个子群维护`2×3`表，实现保持、历史最优、二元锦标赛三动作，奖励固定在局部搜索之前。
- `OriginalNeighborhoods`按总体v2编号实现关键工厂交换/插入和O1–O9；ESWA Table 8的MA/WA编号差异单独留证。
- `PublishedHmopsoQgs`实现初始化、M3分群、Q-gbest、全局更新、奖励、关键工厂搜索、固定O1–O9、合并、PDDR-FF保留、无容量个人非支配历史和统一FE计数。
- `CanonicalEadhfspInstanceLoader`读取`20_2_3_1.txt`的加工、机器、速度、功率原值，并按键控seed补全SUT及分阶段工人效率/成本；原工厂级工人数据不混入canonical实例。
- `P4HmopsoQgsSmokeRunner`执行获批2000 FE烟测并生成UTF-8轨迹、Q表、非支配集和SHA-256清单；500000 FE配置只序列化，未执行。

## 测试与构建

- P4定向测试：问题模块1项、算法模块10项，合计11项，`0 failures / 0 errors`。
- Fig.5交换序列及Fig.6六类固定事件全部精确通过；O1–O9和两类关键工厂操作均通过合法性测试。
- 短程闭环测试验证40 FE精确闭合、同seed字节一致、不同seed事件不同、Q奖励早于局部搜索。
- `mvn -pl jmetal-exec -am -DskipTests package`成功；抽查P4问题、算法、runner class均为major version 52（Java 8）。
- JDK 17兼容完整回归仍为651项、0 failures、3个P1已登记errors、6 skipped；三个错误签名不变。该旧回归在`jmetal-core`按预期失败后停止，P4模块通过独立定向测试覆盖。
- 旧`jmetal-algorithm`因TestNG依赖会让Surefire静默跳过JUnit 4；P4在该模块POM固定`surefire-junit4` provider，定向日志实际执行10项而非0项。
- 新P4主代码扫描：`fatigue/CFVF/Q-pbest/O10–O13/CA-TA`命中0，`new Random(`命中0。

日志：

- `maven-targeted-tests.log`
- `maven-package.log`
- `maven-full-regression-jdk17-compat.log`
- 首次未加JDK 17 `--add-opens` 的诊断日志：`maven-full-regression.log`

## 两组2000 FE烟测

两组实例均使用seed `20260808`，每组独立运行3次，每次恰好2000 FE。

| 实例 | 三次字节重放 | 轨迹SHA-256 | 结果SHA-256 | 非支配集数量 |
|---|---|---|---|---:|
| ESWA Fig.3黄金实例 | true | `42290524488295f990bea54a3572bb7b5609a7141cda7f1e57c5a35cd0d1d899` | `bf4bf8639b857a0ab0572e74b86bf8c392a2dce8be7b2aa8942675fbb3eae39f` | 70 |
| `20_2_3_1.txt` canonical | true | `3b5b939d7e3b254fb8f1518ae6661f1c0a30d3a735a04f8a929d2393a82e5c8f` | `0c117787f9bc3437ecaac469e12cbb9608d53f6119ed69c44478959c939ac8a7` | 32 |

每个run1均记录128次Q决策、64份子群快照和1686条局部搜索状态。黄金实例记录57次资源修复、769次接受、901次拒绝；真实实例记录330次修复、926次接受、744次拒绝。两者非法候选均为0，预算末尾各有16条显式停止记录。

完整输出及结果manifest位于`results/`子目录；工作工程中的`java-jmetal58/results/p4`与证据副本23个文件逐文件SHA-256一致。

## 未证明和边界

- 未运行Table 9的500000 FE正式预算。
- 未执行多实例、多种子统计、HV/IGD或与论文数值表的正式对比。
- `author_actual`仍只用于差异诊断；其七目标和两目标PDDR行为没有伪装成论文三目标基线。
- 未实现疲劳、CFVF、容量6个人档案、Q-pbest、双Q或O10–O13。

因此P4完成后下一允许工作包是P5疲劳解码，但不会自动开始。
