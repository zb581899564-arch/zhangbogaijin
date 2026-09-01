# V35-FC5-MIDHORIZON-DIAGNOSTICS-V3：真实最终判定

判定：INCONCLUSIVE / FAIL-CLOSED。

## 统一状态

diagnosticToolingValidated=false
250kReadyForPreregistration=false
250kStarted=false
formalMatrixRunning=false
FC5=INCONCLUSIVE

algorithmDecisionSemanticsChanged=false
formalFrozenJarChanged=false
pddrDecisionChanged=false
diagnosticSourceChanged=true

trueRngSequenceAudit=true
trueGeneratedCandidateSequenceAudit=true
pddrPhysicalLifecycleValidated=true
teacherOutcomeLifecycleValidated=true
cataFullLifecycleValidated=false
midHorizonCheckpointCoverageValidated=false
formalBudgetSemanticMatch=true
evidenceHashesClosed=true

## 判定依据

已通过的部分：

- 最终源码 Jar 已固定为 723D24ED3021A01FACDA0231E3B142238E740FB18D025A4341748F2AF8D22E2F；
- actual jMetal RNG、actual generated candidate、JS/FA/MA/WA 四向量、streaming SHA-256 与 candidate count closure 已通过；
- PDDR parent/lineage/physical-slot provenance contract 已通过；
- Qg/Qp teacher metadata 与 offspring backfill 已通过；
- 长程 CA-TA 稳定 ID、生命周期字段、显式 right-censor 已通过；
- A2/A4 的完整 20k 配对与 A2 50k 的 checkpoint gate 已通过；
- ON/OFF 核心行为 hash 全部一致，20k/50k 完整链路的最大实测 overhead 为 13.82%；
- 按 actualTelemetryBytes / actualFE × 250000 × 1.25 计算，最大单运行日志投影为 936,629,657 bytes，8 个 ON 运行合计为 3,080,677,087 bytes，均在 1 GB/15 GB 边界内；
- 没有启动 250k、没有上传、没有运行 formal matrix。

在完整 20k/50k 验收组中保留的唯一最终硬门失败：

- A4 原始 50000 FE 运行在 phase-consistent termination 下于 actualFE=48269 停止；
- 名义 50000 FE atomic checkpoint 未到达，记录为 unobservableCheckpointCount=1、CHECKPOINT_NOT_REACHED:1；
- 其余 A4-50k lifecycle、provenance、teacher、candidate 与 observer-error 检查均通过，但 checkpoint gate 失败足以使总 gate 失败。

2k 的 5100 FE 与 A4 20k 的 20258 FE 是在执行前登记的 complete-phase fallback；2k 仍然只作为短门，因 source coverage（A2/A4）及 A4 CA-TA event 尚未闭合而不宣称全链路通过。没有使用任何未登记的 50k fallback。

## 封存边界

只将 14 号目录的 FINAL_V3_SEQUENCE_AUDIT.md、15–20 号目录中的 final-suffixed runs、最终报告、预登记预算记录、最终 diagnostic Jar 与 evidence-sha256.tsv 视为本轮 acceptance evidence。manifest 共 112 个条目，排除自身并已完成 reverseFailures=0 的逐文件校验。旧目录与旧源码输出见根目录的 SUPERSEDED_NOT_ACCEPTANCE_EVIDENCE.md，不得回填本轮 gate。

即使 evidence-sha256.tsv 的逐文件反向校验为零失败，本判定仍保持上述 fail-closed 状态；不得据此启动 250k 或 formal matrix。
