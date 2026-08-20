# V35运行时审计与校正报告（2026-08-13）

## 结论

旧100k记录不能继续作为当前v3.5算法比较证据。旧实现同时存在事件统计口径、随机流、DSCR角色方向、CA-TA-Lite状态机、正式外循环、FE标记和双Q分块冻结粒度问题。上述问题均已修复并用当前语义重新运行20k/100k单seed诊断。

当前证据仍只属于工程诊断，不构成抽样复现或论文优越性结论。

## 已修问题

1. `qgEvents=4096`实为滚动日志保留上限。当前证据分开写入`p6EventsTotal`和`p6EventsRetained`。
2. 正式路径残留`JMetalRandom`全局状态。当前正式v3.5路径的索引随机和短生命周期`Random`均由运行实例seed派生；20k下验证FULL在同JVM中先跑/后跑基线不改变FE和最终前沿。
3. DSCR对四类子群统一按三目标字典序选老师。当前按角色选择严格支配者：G1最小Cmax、G2最小TEC、G3最小TWC、G4最小归一化最大偏差，再按指纹稳定破平。
4. CA-TA-Lite使用历史累计失败触发重测，导致达到3次失败后永久停留Test。当前只统计连续Apply失败；重测、掩码变化和Apply预算耗尽都会开启新的Test epoch及基线。
5. CA-TA-Lite合法mask过去只按上下文生成。当前先完成无FE preview，再把真实可构造动作交给状态机；同一preview复用于完整评价。
6. N4过去缺少自然恢复增益硬门，N5在JS动作成功后可能没有资源动作。当前N4只有自然恢复总时长严格增加才可接受；N5固定先做JS，再做资源动作，资源动作不可行时才退到工厂重分配。
7. CA-TA-Lite缺少正式代价信用。当前统计宏动作平均wall-clock和平均完整FE，并在成功次数、方向收益之后使用等权中位数归一化代价破平。
8. FULL过去走普通PSO循环，基线走`Q_Times=50/LS_Times=30`正式嵌套循环。当前两者共享同一正式外循环、相同Q轮数、PDDR时序、关键工厂搜索和O1-O9；FULL用CFVF替换原资源更新并叠加档案、Qp、DSCR和CA-TA-Lite。
9. 正式Q轮的新全局后代可能继承局部候选的预评价标记而被跳过。当前每轮全局更新后强制清除标记，FE分解重新闭合。
10. 正式FULL首次PDDR前的父代快照可能尚无谱系标签，且局部候选存在重复表示。当前在父代快照前初始化谱系，所有局部候选只通过pending映射进入共同PDDR。
11. 双Q分块冻结的块推进粒度错误：`decide()` 使用 `completedOuterGenerations`，在`Q_Times=50`的正式外循环下每50个Q轮才+1，导致20k/100k预算内`G_BLOCK`恒为0（实测20k: WARMUP=19/P=31/G=0；100k: WARMUP=50/P=150/G=0），FULL的Qg在预热后从未恢复学习，与基线（synchronous、Qg全程学习）不对称。当前新增按每次全局后代轮（每个Q round/每代）递增的`dualQRoundCounter`驱动块推进，块长5轮与AGENTS.md"P/G块长默认5代"对齐；修复后20k FULL为`WARMUP=19/P=16/G=15`，100k FULL为`WARMUP=50/P=75/G=75`，P/G真实交替。

## 当前FE闭合

100k基线：`100初始 + 200轮×100全局 + 79900继承局部 = 100000`。

100k FULL：`100初始 + 200轮×100 CFVF + 536 CA-TA-Lite + 79364继承局部 = 100000`。

两者均为4个正式外循环、200个Qg轮、4次评价后PDDR，初始种群哈希完全相同。FULL的dual-Q分块：`WARMUP=50 / P_BLOCK=75 / G_BLOCK=75`；基线为同步模式（dualQ全部为0）。

## 当前100k单seed诊断

实例`20_2_3_1`，seed`20260808`，population`100`，FM3，单族序列无关SUT，`ShiftMode=NONE`。

| 指标 | 校正基线 | FULL | FULL相对变化 |
|---|---:|---:|---:|
| 最小Cmax | 191.16991228544117 | 203.17341035581873 | +6.2775%（较差） |
| 最小TEC | 8651.554746588597 | 8571.108010911208 | -0.9297%（改善） |
| 最小TWC | 12890.749508822195 | 12881.17305355312 | -0.0743%（改善） |
| 最终前沿规模 | 84 | 147 | +75.00% |
| 本机单次algorithm.run | 3.68s | 11.38s | 3.09x |

G-block粒度修复后FULL的Qg在预热后恢复学习，单实例单seed下Cmax明显变差、TEC小幅改善、TWC持平，前沿多样性仍为正。当前结论是”多样性和TEC存在信号，但Cmax机制仍需V35-P10/P11审计”，不是全面优越信号；修复本身只保证分块冻结机制真实执行，不构成性能结论。

## 测试

v3.5定向回归：`mvn -q -pl jmetal-algorithm -am -Djacoco.skip=true -DfailIfNoTests=false -Dtest=V35*Test,ZhangBoDualQCoordinatorTest test`

结果：`Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`（含两个烟测新增的`dualQG>0`断言，证明G-block在20k/100k预算内真实执行）。

jmetal-exec P6集成回归（super路径）：`ZhangBoP6IntegrationSmokeTest`为`10/10`通过（super路径每代调用一次，粒度语义不变）。

模块全回归：`jmetal-problem`为`67/67`通过，`jmetal-algorithm`为`155/155`通过。`jmetal-core`在JDK 17加固定`--add-opens`后为648项通过、仅保留P1登记的3项`DefaultIntegerPermutationSolution`错误。六模块打包成功（跳过javadoc，JAVA_HOME环境缺javadoc命令），正式类字节码major version为52（Java 8）。

额外20k运行顺序隔离测试验证：同一FULL在干净状态与同JVM先执行基线后，状态均为`COMPLETED`，初始种群、FE和最终前沿一致；CA-TA动作级计数存在±2 FE以内的残余漂移，作为已知限制登记，不承诺动作级字节一致。

## 尚未证明

- 单实例单seed不能证明统计优势。
- 尚未执行QG0/QG1单变量配对，不能把当前变化归因于DSCR。
- 尚未完成Cmax教师曝光审计，不能针对Cmax调参或增加特权机制。
- 真实wall-clock参与CA-TA-Lite代价信用；跨机器动作日志不承诺字节级一致，正式实验需固定环境并独立重复。
- 不启动500000 FE、正式矩阵、PF-SDST或论文显著性统计。
