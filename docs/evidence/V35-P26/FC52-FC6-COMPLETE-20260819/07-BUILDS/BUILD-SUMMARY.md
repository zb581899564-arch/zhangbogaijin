# BUILD-SUMMARY：FC-5.2 → FC-6 构建与代码状态

生成：2026-08-19（本地 java-jmetal58 工作树 + 训练机部署）

## 1. 算法改动（FC-6 唯一算法 diff）

| 文件 | 改动 | 性质 |
|---|---|---|
| `jmetal-algorithm/.../zhangbo/ZhangBoEvaluatedPddrSelector.java` | 新增 `public static final int MAX_BOUNDARY_SLOTS=3` + `boundaryReservedIndices(pool, pddrScores)` + `lexicographicallyBetter(...)`；`select()` 改为"q==0 三向极值保留位先行 + 其余按 (score, originalOrder) 序填充" | **唯一算法改动**（零新增参数，不改 authorScores 公式） |
| `jmetal-algorithm/.../v35/V35Fc52LifecycleAudit.java` | R_retain 计数（`boundaryReservedIndexes` 镜像，独立实现）；每周期 archive 统计（`CycleArchiveStat`，submit/accept/reject/removed/存续尺寸） | 纯观察 |
| `jmetal-algorithm/.../mypso/ZhangBoMOHPSOQ.java` | `observeArchiveAdd` 的 cycle 参数由常数 `(int)formalBaselineOuterCycles+1` 修正为 `generationNumber()`（FC-6 诊断用，观察侧） | 纯观察/接线修正 |

未改：archive（`ZhangBoIncrementalParetoArchive` 一行未动）、PDDR 公式、VNS、Qg、Qp、CFVF、CA-TA、local acceptance、初始种群、随机序列、FINAL 输出。

## 2. 三个 jar（sha256 前 16 位）

| 构建 | 内容 | 校验和 | 用途 |
|---|---|---|---|
| BUILD-A | FC-5.2 最终版 + R_retain 观察（**无 BP-PDDR**，selector 为原始 select） | `54927ddf30e885aa` | Stage 1 死亡链验证 + 基线 R_retain |
| BUILD-B | = A + BP-PDDR（selector 改动） | `9b71d206159cc460` | Stage 2（20-job）/ Stage 3（100-job）验证 |
| BUILD-C | = B + 每周期 archive 观察 + archive-cycle 接线修正 | `c0014da4959cc0f4` | FC-6 根因诊断（运行中） |

构建命令：`mvn -pl jmetal-exec -am package -Dmaven.test.skip=true -Djacoco.skip=true -Dgpg.skip=true -Dmaven.javadoc.skip=true`（jmetal-algorithm 依赖 jmetal-problem 的 V35CmaxBestEver，必须 `-am`）。
主类：`org.uma.jmetal.runner.lc_psode.ZhangBoV35P25EBudgetDiagnosticRunner`

## 3. 单元测试（FC-6 新增/相关）

| 测试 | 断言 | 结果 |
|---|---|---|
| `V35Fc6BpPddrBoundaryTest`（5 例） | 三向极值保留、去重、被支配全局最小不保留、填充序 (score, originalOrder)、位移 ≤|E∖topK|、确定性、防御截断 | 5/5 绿 |
| `ZhangBoEvaluatedPddrSelectorTest`（4 例） | 既有行为在新规则下不回归（逐例验证，无需改动） | 4/4 绿 |
| `V35Fc52PddrSelectProbeTest` | score=1 探针 | 绿 |
| `V35Fc52LifecycleAuditTest` | OFF/ON/ON 三连跑 front hash + FE 一致（观察中性） | 绿（Build C 改动后复验仍绿） |
| mypso 区全量 205 例 | — | 203 绿；2 个历史锁红灯：`V35P101TeacherPoolVerificationTest`（pool-OFF 复现 P10.1 front——BP-PDDR 按设计改变 FULL 轨迹）、`V35P241FreezeRevisionTest`（P24.1 源码冻结——FC-5.2 起源码新增已过期）→ 计划 Stage 4 重冻结（保留旧值对照） |

注：全库 jmetal-algorithm 另有一批上游 jMetal 核心库测试在本机 JDK 下既有失败（operator/util 非法参数断言、mockito/cglib 环境错误），与 FC-6 改动无关（失败类不含 mypso/zhangbo/v35 任何文件）。

## 4. 已知观测口径差距

- fc52"出生"钩子漏接 `vnd()`/`factorySearch()`（工厂间变邻域）内 `evaluator.evaluate(current_pop1/pop1)`（ZhangBoMOHPSOQ L6438/6439/6454/6455/6902）：该路径经正式 problem.evaluate 被问题侧 `V35CmaxBestEver` 全量计数，但 fc52 不记录其出生/archive 事件（Stage 2 seed22 的 171.74 即此路径产出）。
- 影响：fc52 bestEver 对"VND 产出的最优解"欠计；基线三 seed 两侧观测器完全一致 → FC-5.2 死亡链结论（逐候选记录）不受影响。权威 best-evaluated Cmax = 问题侧 `bestCmaxEvaluatedOverall`（Stage 2：171.74/175.35/176.53；Stage 3：695.39/727.33/682.73）。
- P8/FC-2 指标口径的归一化夹取：参考面之外的原始点被压到 1.0 产生伪支配、部分点被滤除（两臂对称，判定公平；已核实 Stage 3 基线-24 掉 124/513、BP-24 掉 47/213）。
## 5. 后续构建（FC-6A-POST / FC-6A.1 追加，2026-08-19/20）

本地 jar 副本已随归档收拢至本目录（原 `张博改进/build-artifacts/` 已删除）：

| 构建 | 文件 | sha256 | 用途 |
|---|---|---|---|
| C2-BP | `c2/jmetal-exec-5.8-BUILD-C2-BP-diag.jar` | `29e2aa4b053e8c517db632b4f788c23d319afdc405c64d3b36b500b8ea3a6f6d` | stage5 BP 臂（BP selector + FC-6A-POST 诊断审计） |
| C2-BASE | `c2/jmetal-exec-5.8-BUILD-C2-BASE-diag.jar` | `67b91008a822078f1d2f4c58edcca4f21e3d48c9ab1e522f6855243c0d8e0a5c` | stage5 BASE 臂（原始 selector + 同审计；`-Dmaven.test.skip=true` 构建） |
| C3-COMP | `c3/jmetal-exec-5.8-BUILD-C3-COMP.jar` | `5233b690db12d7130549355228f4da026589f28759d702484ac65d178aaa3b4a` | stage6 FC-6A.1（**原始 selector** + 组成审计；selector 类字节码 `14040a20…` == BUILD-A 原版） |

脚本：`c2/fc6-stage5-c2.sh`（stage5 12 跑）、`c3/fc6-stage6-fc6a1.sh`（stage6 12 跑：QGS 臂走 `ZhangBoV35P25ECorrectedComparisonRunner --algorithm HMOPSO_QGS_F`，BASE 臂与 stage5 同参）。

C3-COMP 构建法：源码树 selector 临时换回原始版（BP 版备份于 `ZhangBoEvaluatedPddrSelector.java.bk-bp`，构建后已恢复 BP 版为工作树状态），`mvn -pl jmetal-exec -am package -Dmaven.test.skip=true -Djacoco.skip=true -Dgpg.skip=true -Dmaven.javadoc.skip=true`。

FC-6A.1 新增插桩（均纯观察）：`V35Fc6BpPddrDiagnosticAudit` 的 `fc6diagComp` per-round 组成行 + `fc6diagCompSummary` rollup；`ZhangBoV35P25ECorrectedComparisonRunner` 对 ZHANGBO_A4/HMOPSO_QGS_F 臂启用审计并把 `fc6DiagText()` 追加进 mechanism-summary.txt。

## 6. 已否决平行批（2026-08-19 晚，另一会话产物）

服务器 `/home/inspur/aicomp/zhangbo-fc6a1-20260819/` 的同名批次使用 BP 污染 jar（`12b83708…`，内含 `MAX_BOUNDARY_SLOTS` selector 字节码 `8e70ad91…`），front 与历史基线不符、轨迹第 2 轮起分歧、且 BASE 100-job seed22 不完整。数据已隔离至 `../09-STAGE6-COMPOSITION/rejected-parallel-batch/`（见其 WARNING.md），**勿引用**。
