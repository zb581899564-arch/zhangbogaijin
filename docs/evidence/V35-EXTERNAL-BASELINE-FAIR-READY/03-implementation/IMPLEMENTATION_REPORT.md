# 外部基线 Fair-Ready 实现报告

- 日期：2026-08-30
- 工作包：V35-PFC5 阶段B（NSGA-II-F / SPEA2-F 可信化）
- 消耗FE：单元测试与 2k 贯通合计 4×2000 + 测试内若干千级评价，全部属于
  ENGINEERING_SMOKE/单测域；无任何 50k/250k/500k 科学实验
- 铁律核对：正式 V35 算法核心零改动；冻结 Jar 三件（8dad8f40 / 121fbb49 / 723d24ed）
  事后重算 SHA 全部不变；PDDR/CFVF/双Q/CA-TA 零改动

## 1. 改动清单（最小修改原则）

| 文件 | 类型 | 内容 |
|---|---|---|
| `jmetal-algorithm/…/p25e/V35P25EOfficialJMetalEngine.java` | 修改 | ① `run()` 强制 `ObjectiveView.THREE_OBJECTIVE`（堵 7 槽误用）；② 新增算子注入重载 `run(algorithm, problem, pop, maxFEs, seed, crossoverOverride, mutationOverride, selectionOverride)`——null 时回退原算子，搜索语义与 FE 合同不变；③ `domainSeed` 改 public（与 Runner 共享域分离种子）；④ 补 `SelectionOperator` 导入 |
| `jmetal-exec/…/lc_psode/ZhangBoV35ExternalFairBaselineRunner.java` | 新增 | 独立命名空间 fair-ready Runner（下节详述） |
| `jmetal-algorithm/src/test/…/p25e/V35ExternalBaselineRepresentationTest.java` | 新增 | 表示层 4 测试 |
| `jmetal-algorithm/src/test/…/p25e/V35ExternalBaselineIdentityTest.java` | 新增 | 身份门 5 测试 |
| `jmetal-algorithm/src/test/…/p25e/V35ExternalBaselineFairnessTest.java` | 新增 | 公平性 3 测试 |

未触碰：`OfficialJMetal58NSGAII/SPEA2`（隔离副本原样）、`V35FourVectorVariation`、
`V35ComparisonProblemAdapter`、`V35ComparisonSolution`、`V35ExactEvaluationBudget`、
正式 V35 算法核心、P8 组件、FM3 解码器、PDDR。

## 2. Runner 设计（ZhangBoV35ExternalFairBaselineRunner）

- 输入：`--algorithm {NSGA-II-F|SPEA2-F}`、`--instance`、`--seed`、`--population`、
  `--maxFEs`、`--snapshot`（v35-formal-initial-population-v1 四向量快照）、`--output`。
- 共享层：canonical FM3 问题（`ZhangBoCanonicalProblemLoader`，extensions/fatigue 目录，
  `ZhangBoShiftConfiguration.none()`）、`readSnapshot` 显式初群、
  `V35ComparisonProblemAdapter(THREE_OBJECTIVE, maxFEs)`。
- 隔离层：算子经 runner 侧纯委托包装（CountingCrossover/Mutation/Tournament）计数，
  官方核零插桩；初群经 `P8InitialPopulationProvider.copy` 深拷贝注入。
- 落盘：`configuration.txt`、`source-provenance.properties`、
  `initial-population.sha256`（V35/P8/snapshot-file 三哈希）、`status.properties`、
  `front.csv`、`event-summary.properties`（算子调用计数+身份证据）、
  `budget-termination.properties`、`stdout.log`、`stderr.log`、
  `evidence-sha256.tsv`（逐文件自清单）。

## 3. 构建与测试结果

- `mvn -pl jmetal-problem test`：**67/67 PASS**。
- `mvn -pl jmetal-algorithm -am test -Dtest="V35P25EFaithfulEnginesTest,V35ExternalBaseline*"`：
  P25E 回归 2/2 + 新增 **14/14 PASS**（代表 4 + 身份 5 + 公平 3 + 既有 2）。
- `mvn package -DskipTests`：六模块打包成功（Java 8 target=52 不变）。
- 比较适配构建物（独立名称）：
  `03-implementation/external-fair-baseline-comparison-585ca315.jar`（fat jar 终值
  SHA-256 `585ca315…`，模块 fat jar：core `3fd3b43d…`、problem `087a32e1…`、
  algorithm `89ecc98c…`、exec `585ca315…`）。
- 冻结 Jar 事后重算：`8dad8f40…`/`121fbb49…`/`723d24ed…` 全部不变。
- 注：本地单模块构建（不带 `-am`）会因 m2 里旧版 jmetal-core 编译失败——那是
  历史重建品；一切构建走 reactor（`-am`），与既有纪律一致。测试用
  `-Djacoco.skip=true`（JaCoCo 2.x agent 与 JDK17 不兼容，属历史环境问题）。

## 4. 随机源纪律

- `JMetalRandom.getInstance().setSeed(seed)` 每进程一次（官方核二元锦标赛候选抽取
  是唯一全局消费点）；交叉/变异用 `domainSeed(seed,1/2)` 域分离的
  `JavaRandomGenerator`（splitmix 混合）；初群工厂为确定性构造。
- 独立 JVM 重放（2k，同 seed 两次）front 哈希逐字节一致 → 随机流可重放实证。

## 5. 与禁止项的对照

静态扫描（`02-adapter-audit/forbidden-reference-scan.csv`，7 文件×13 检查=91 项）：
禁止引用（P25D 引擎、BaselineUpdater、CFVF、DSCR、CA-TA、PDDR、教师池、inherited LS、
O1-O13 网关、dualQ、个人档案）全部 0 命中；目标槽 2–5 读取 0 命中；槽 2–5 写入仅
登记两类合法形态（THREE_OBJECTIVE 的 TWC 映射 `canonical.getObjective(6)`→槽2、
7 槽垫片的 0.0 零填充——后者被引擎的 THREE_OBJECTIVE 强制门隔绝）；非 NONE Shift
0 命中。**overall=PASS。**
