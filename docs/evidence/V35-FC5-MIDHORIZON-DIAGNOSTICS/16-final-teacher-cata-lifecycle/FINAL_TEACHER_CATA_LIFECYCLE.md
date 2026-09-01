# V35-FC5-MIDHORIZON-DIAGNOSTICS-V3：Teacher / CA-TA 生命周期验收

状态：运行时字段完整性与生命周期语义已完成核验。报告中的 RIGHT_CENSORED_RUN_END 是明确的右删失枚举，不是缺失值替代。

## Teacher 运行证据

A4-20k-effective-20258-100_5_3_1-ON-final 产生 13600 条 teacher-use event：

- QG：600；QP：13000；
- qState 实际覆盖 0,1,2,3,4,5,6,7；
- qAction 实际包含 COMPLEMENTARY、DIRECTIONAL、EPSILON、GLOBAL_ARCHIVE_TOURNAMENT、HISTORICAL_CACHE、KEEP、PREVIOUS_CACHE；
- requesterSlot 为 1–4，requesterRole 覆盖 G1_CMAX、G4_BALANCED、G2_TEC、G3_TWC；
- teacherSource 实际包含 GLOBAL_ARCHIVE、HISTORICAL_CACHE、PERSONAL_ARCHIVE、PREVIOUS_CACHE；
- 13600 条记录的 offspringReason 均为 OBSERVED，没有 NOT_APPLICABLE、NOT_OBSERVED 或 UNOBSERVABLE 的 teacher-required 字段。

代表性 QG 行具有以下实际字段：qSystem=QG、scope=ALL_QG、qState=0、qAction=PREVIOUS_CACHE、requesterSlot=1、requesterRole=G1_CMAX、candidateViewSize=15、实际 eligible/selected directional score、真实 teacher fingerprint/objectives、真实 offspring fingerprint/objectives、offspringImproved=false、offspringReason=OBSERVED。Qp 路径同样写入完整 SelectionContext 与 branch-linked offspring backfill。

各长程 ON 运行的 teacher 行数与 contract 如下：

| run | teacher rows | Qg/Qp metadata + offspring | observer errors |
|---|---:|---|---:|
| A2-20k（两实例） | 200 / 200 | PASS（QP 按配置不适用） | 0 |
| A4-20k（两实例） | 13600 / 13600 | PASS | 0 |
| A2-50k | 400 | PASS（QP 按配置不适用） | 0 |
| A4-50k | 26300 | PASS | 0 |

teacherContract 在 A4 长程运行中为 qgObserved=true,qpObserved=true,metadataAndOffspringComplete=true,observerErrorsZero=true。

## CA-TA 生命周期证据

A4-20k-effective-20258-100_5_3_1-ON-final 产生 492 条稳定 ID 的 CA-TA event，summary 有 5 个窗口/宏邻域分组。实际结果分布为：

- result=ACCEPTED：162；
- result=NO_RECOVERY_GAIN：89；
- result=NOT_BETTER：241；
- 进入 merge pool：112；
- selectedByPddr=true：62；
- survivedNextGeneration=true：62，false：50。

每条记录都有 CATA-00000001 形式的稳定 eventId、generation/cycle、generatedFE/evaluatedFE、parent/candidate fingerprint、TEST/APPLY、accepted/result、merge pool、PDDR、personal/global archive、next-generation、later-teacher 与 later-improvement 字段。运行结束时尚不可观察的后续字段统一写为 RIGHT_CENSORED_RUN_END；该运行的 summary 为 rightCensoredFields>0 且 unobservableFields=0，所以 cataContractPass=true。

A4-20k 的另一实例有 539 条 CA-TA event；A4-50k 有 1394 条，均为稳定 ID、显式右删失、unobservableFields=0。A4-2k 因有效完整阶段预算内尚未产生 CA-TA candidate，故该短门的 cata lifecycle gate 保持失败，不被空表冒充通过。

结论：Qg/Qp 的 teacher metadata 与 offspring backfill 已从真实调用链观测；CA-TA 的全生命周期字段已从生成到可观测终点闭合，终点之后使用显式 right-censor，未使用隐式 NOT_OBSERVED 或空白占位。
