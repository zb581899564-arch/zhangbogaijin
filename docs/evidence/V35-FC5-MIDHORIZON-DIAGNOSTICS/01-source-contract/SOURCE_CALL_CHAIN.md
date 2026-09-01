# SOURCE_CALL_CHAIN — V35 诊断遥测调用链（只读审计）

**审计范围：v35 冻结Jar 8DAD8F40 正式语义，行号基于当前工作树**

```
V35FairRunner.runFc5TransferDiagnostic
 ├─ ZhangBoMOHPSOQBuilder.build (mixture [20,40,20,20], LS order CA-TA→inherited, FM3, Shift=NONE)
 ├─ ZhangBoMOHPSOQ.run
 │   ├─ evaluateSwarm (FE原子增长点) → ZhangBoFatigueEvaluationResult [FM3 core]
 │   ├─ prepareOriginalQg → V35SocialKnowledgeSnapshot (Qg教师快照，全量拷贝冻结)
 │   ├─ applyV35Dscr (DSCR过滤，fingerprint hoisted A1) [观测点: teacherUses/dtur]
 │   ├─ settleOriginalQg / settleQp (Qg/Qp TD, P=5/G=5, warmup 10%)
 │   ├─ updatePositionWithCfvf (CFVF四向量, JS/FA/MA/WA)
 │   ├─ mergePool = parent(100) + offspring(100) → ZhangBoEvaluatedPddrSelector.GLOBAL_ORIGINAL.scores(Rank)
 │   ├─ V35Fc5TransferAudit.recordPddrRound (纯观察, 分数/排名/四代表捕获) [PDDR账本观测点]
 │   ├─ applyEvaluatedPddr → workingPopulation + archiveUpdate (decision/observed)
 │   ├─ runV35CaTaLiteLocalSearch (Test/Apply, N1-N5, 瓶颈) [CA-TA观测点]
 │   │   └─ V35MacroCandidateGateway (fingerprint, bottleneck)
 │   └─ observeCheckpoint (新增) → V35CheckpointFrontObserver.captureIfDue(actualFE)
 │       ├─ workingPopulationND (过滤后)
 │       ├─ decisionArchiveFront (ZhangBoIncrementalParetoArchive)
 │       └─ observedFullFront (V35PassiveEvaluationArchive, 只读)
 └─ V35CheckpointFrontObserver / V35FullPddrLedgerObserver / V35TeacherConcentrationObserver / V35CaTaContributionObserver (全部 observerErrors 计数, never throw)
```

**关键锚点：**
- FE原子边界：`ZhangBoMOHPSOQ.runFormalHmopsoQgsBaseline:575-657` 每qRound結束為原子階段
- PDDR：`ZhangBoEvaluatedPddrSelector.authorScores:160` 与 `PDDRFFselect:7722`
- Teacher：`ZhangBoQgController.sanitizeOne:362` 与 `ZhangBoDualQCoordinator:260`
- CA-TA：`V35CaTaLiteController:Test/Apply` 与 `V35MacroCandidateGateway:prepareWithEvaluation:259`

**保证：所有Observer仅读copy，never写回，不改排序/随机/FE。**
