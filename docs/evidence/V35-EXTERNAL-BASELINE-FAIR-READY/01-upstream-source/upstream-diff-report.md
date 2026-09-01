# 上游来源 Diff 报告（官方 jMetal 5.8 ↔ 本地隔离副本）

- 日期：2026-08-30
- 上游身份：GitHub tag `jmetal-5.8` = commit `831d62d0bbf384e1770efc1bb6eef69ce0ce75b9`
  （经 GitHub API 验证：tag 对象 `4552c65f…`，tagger ajnebro，2019-07-19；
  代码中 `V35P25EOfficialJMetalEngine.UPSTREAM_TAG/UPSTREAM_COMMIT` 与之一致）
- 许可证：MIT 式（官方 tag LICENSE.txt，SHA-256
  `153f8092342b46019a4f30c8eb04f8580f5ef5b664fd169ab141e7690d74f6d5`；项目根
  `java-jmetal58/LICENSE.txt` 内容相同）
- 决定性比对文件：`diff-copy-vs-github-nsgaii.txt`（180 行）、
  `diff-copy-vs-github-spea2.txt`（132 行）；GitHub 基准原文存于
  `official-jmetal58-github-{NSGAII,SPEA2}.java` 与 `official-jmetal58-github-deps/`

## 1. 关键来源声明（必须先读）

**Maven 本地库中的 `jmetal-*-5.8-sources.jar` 不是上游制品，不得作为官方来源证据。**
实证：Maven Central 从未发布 5.8（版本从 5.7 跳 5.9）；两 jar 的 MANIFEST 为
`Built-By: 33056 / Build-Jdk: 17.0.12`（本机构建）；jar 内含项目自有包
（`mypso/v35/p25e/official/`、`mypso/zhangbo/` 等）；jar 内 `nsgaii/NSGAII.java`、
`spea2/SPEA2.java` 是**改造版算法**（评估计数改写、reproduction 混入工厂/机器/工人
向量算子、构造器追加 6 参数等）。
因此官方性唯一有效依据 = **GitHub tag 原文 ↔ 本地隔离副本的逐行 diff**。

## 2. 隔离副本 ↔ GitHub 官方 5.8 逐行 diff 结论

| 算法 | 副本 | diff 规模 | 差异内容 |
|---|---|---|---|
| NSGA-II | `OfficialJMetal58NSGAII.java`（SHA `64d78c6c…`） | 2 hunks，+23/−80 | 仅 ① package 改为 `…mypso.v35.p25e.official`；② 类/构造器名 `NSGAII`→`OfficialJMetal58NSGAII`；③ javadoc 换成隔离说明；④ 纯排版（空行/续行折叠/缩进） |
| SPEA2 | `OfficialJMetal58SPEA2.java`（SHA `06a9ea82…`） | 5 hunks，+17/−55 | 同上四类 |

**算法语句零差异**：比较器、选择循环、reproduction（双亲选择+交叉+变异）、
evaluatePopulation、replacement、进度计数（NSGA-II `evaluations += offspringPopulationSize`；
SPEA2 `iterations=1 / iterations++`）、停止条件、`getResult()`（SPEA2 返回 archive）
全部逐行同构。上游 quirk（NSGA-II mating pool `population.get(i+j)`）按原样保真。

## 3. 运行期依赖核对（SPEA2/NSGA-II 实际解析到的类）

| 文件 | 与官方 5.8 比对 |
|---|---|
| `BinaryTournamentSelection.java`、`DominanceComparator.java` | 字节级相同 |
| `RankingAndCrowdingSelection.java`、`RankingAndCrowdingDistanceComparator.java`、`CrowdingDistance.java`、`EnvironmentalSelection.java`、`StrengthRawFitness.java`、`LocationAttribute.java`、`StrengthFitnessComparator.java` | 内容一致（仅换行符） |
| `SolutionListUtils.java` | 官方内容 − 1 个空行（无逻辑差异） |
| `AbstractGeneticAlgorithm.java`（本地 core） | 官方 + 2 个**未使用** import（惰性，不执行） |

两副本的 import 全部为官方 org.uma 类，**无任何自写副本类依赖**。

## 4. 框架层已知惰性差异（登记，不触碰副本逻辑）

- `AbstractEvolutionaryAlgorithm`：1 个未使用静态 import + `run()` 内 1 个死变量
  `int index = 0`（不执行算法逻辑）。
- `DominanceRanking`：1 个从未被调用的 `sleep()` 死方法。
- 算子接口扩展：`CrossoverOperator.getCrossoverProbabilityflag()`、
  `MutationOperator.getMutationProbabilityflag()`（仅加方法；官方副本只是实现该
  flag 返回，不改变搜索行为）。
- 以上均为**编译期牵连**，不构成算法改动；已在 `algorithm-source-map.csv` 登记。

## 5. 裁决

```ini
NSGA_II_F_sourceKind=OFFICIAL_JMETAL_CORE
SPEA2_F_sourceKind=OFFICIAL_JMETAL_CORE
upstreamIdentityResolved=true
evidenceBasis=github-tag-jmetal-5.8-commit-831d62d0-line-diff
```

本地重建的 m2 sources jar 不作为来源证据（本节 §1）；本结论不依赖它。
