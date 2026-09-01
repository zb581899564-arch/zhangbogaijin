# V35 当前算法示意图说明

图文件：`V35_CURRENT_ALGORITHM_FLOW.svg`

## 图的语义范围

这张图对应当前工作副本的 V35-FC6 代码结构，不是旧 P8/P9 移位版本，也不是论文 oracle。图中固定写明：

- FM3 疲劳解码；
- `ShiftMode=NONE`；
- 单一产品族占位、序列无关 `SUT[job][stage]`；
- 正式三目标取作者七槽中的 `[0,1,6]`；
- 结构化 Qg/Qp、CFVF、谱系档案和 CA-TA-Lite；
- inherited local search 的 inter-factory exchange/insertion 与 O1–O9；
- 默认 PDDR 与 FC-6B `REGION_AWARE` 候选支分开标注。

## 与源码的对应关系

| 图中模块 | 当前代码依据 |
|---|---|
| FM3、ShiftMode、single-family、SUT | `V35ProductionConfiguration.java`、`ZhangBoCanonicalProductionProblem.java` |
| DSCR / Qg | `ZhangBoMOHPSOQ.java` 的 `prepareOriginalQg()`、`applyV35Dscr()`、`selectQgLeader()`；`ZhangBoQgController.java` |
| Qp / lineage archive | `ZhangBoMOHPSOQ.java` 的 `settleQp()`、`ZhangBoLineageCoordinator`、`ZhangBoPersonalLeaderDecision` |
| CFVF | `updatePositionWithCfvf()`、`ZhangBoCfvfUpdater.java` |
| FM3 decoder | `ZhangBoCanonicalProductionProblem.java`、`ZhangBoFatigueEvaluator.java` |
| CA-TA-Lite | `runV35CaTaLiteLocalSearch()`、`V35CaTaLiteController.java`、`V35MacroCandidateGateway.java` |
| inherited LS | `runFormalInheritedLocalSearch()`、`ZhangBoCriticalFactoryNeighborhoods`、O1–O9 gateway |
| PDDR | `ZhangBoEvaluatedPddrSelector.java`、`PddrSelectionMode.java` |
| FC-6B region branch | `PddrSelectionMode.REGION_AWARE`、`selectRegionAware()`，容量 `G1=15/G4=55/G2=15/G3=15` |
| audit / provenance | `V35FairRunner.java`、`V35CmaxLifecycleAudit.java`、`V35ModuleTimer.java` |

## 论文中建议的讲解顺序

1. 先从左侧的四个语义子群说明当前解集如何分工。
2. 再说明 DSCR 维护社会知识有效性，Qg/Qp 分别提供社会和谱系认知领导。
3. 用 CFVF 说明一个全局后代如何同时更新 JS、FA、MA、WA，且资源按工件身份对齐。
4. 用 FM3 解码器说明该后代只做一次完整评价，并产生 Cmax、TEC、TWC 与疲劳诊断。
5. 将 CA-TA-Lite 和 inherited local search 分为“探索/深挖”两条局部路径。
6. 最后说明 parent/global/local 三类已评价候选进入 PDDR；默认 PDDR 与 FC-6B 区域支不能在论文中混称为同一个已冻结版本。

## 当前状态提示

图中 `REGION_AWARE` 使用虚线语义说明，是 FC-6B 候选支。若正文只写当前安全主线，应只引用 `default: BP_RESERVED_LEGACY`；若正文写 FC-6B，则必须同时给出其独立配置哈希和对应实验证据。
