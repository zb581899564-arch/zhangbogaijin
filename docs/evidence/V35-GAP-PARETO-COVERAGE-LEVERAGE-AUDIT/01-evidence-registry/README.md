# 01-evidence-registry — 扫描方法与登记说明

- 日期：2026-08-31。登记人：Agent A（历史证据恢复与登记，只读审计）。`newFEConsumed=0`，未修改任何既有文件，未启动任何实验。
- 上游预登记：`../00-preregistration/AUDIT_PREREGISTRATION.md`（H1→H2→H3→H4 优先级冻结）。

## 扫描方法（全部路径由文件系统扫描定位，非凭记忆）

1. 以 `ls` 枚举 `docs/evidence/` 一级目录，按关键字 FC5/FC6/P26/GAP/PFC5/STAGE2/TRANSFER/LEVERAGE 收敛候选目录。
2. `find <dir> -type f` 列出全部文件，`head -2` 读取每个关键 CSV 的真实表头（field-availability 全部基于实际头部核验，不凭文档转述）。
3. 读取决策文件全文/关键节：FC5_250K_ROOT_CAUSE_REPORT.md、FC6_FINAL_CLOSURE_REPORT.md、STAGE3_BP_PDDR_100JOB_VETO.md、FC5_TRANSFER_100K_DECISION.md、REAL_FINAL_DECISION.md、CAL_REPAIR_FAMILY_CLOSURE.md、PILOT_DECISION.md、GAP_PROBE_V2_DECISION.md、module-leverage-matrix.csv、single-repair-family-decision.md、250K_REPAIR_DECISION.md。
4. `sha256sum` 计算全部 <20MB 关键文件真实 SHA-256；多文件结果目录（S21/S22/S31）写 `DIRECTORY`；三个未单独复算的索引/汇总文件在 sha 列写 `EVIDENCE_FIELD_LIMITATION`（文件存在、可随时复算）。

## 约定

- `sourcePath` 相对基准为 `E:\学习\李明哲-毕业材料\张博改进\`。
- `algorithmSemantics`：A4=frozen-formal，与 LOCAL-FE-PACING 的 C0 同语义（GLOBAL_ORIGINAL PDDR、mixture 20/40/20/20、FM3、ShiftNONE）；A2=A4 去 CA-TA-Lite；A0=无 DYNAMIC_BETA 基线；NSGA-II-F/SPEA2-F=忠实外部适配器。telemetryMode=ON 处均标注 telemetry equivalence verified/not verified（2k/20k/50k ON/OFF 行为 hash 等价已验证；诊断工具终局门 INCONCLUSIVE 见 S17，已如实标注）。
- `referenceEligible`：该数据源可进入假设裁决引用；`diagnosticOnly`：仅作机制/过程证据，不得进论文正式指标。
- 缺字段统一写 `NOT_EXPORTED`（冻结 Jar/管线未导出）、`NOT_OBSERVABLE`（该数据源无此观测）、`EVIDENCE_FIELD_LIMITATION`（检查点截断、右删失、FINGERPRINT_ONLY 等）。

## 关键缺口（供主 Agent 裁决前知悉）

- H1/H2 必需的候选级字段（enteredWorkingPopulation、enteredArchive、pddrRetained、selectedByPddr）在 **250k 预算运行上全部 NOT_EXPORTED**：候选级 PDDR ledger（含 lineageId/semanticRole/rejectionReason）本地仅存在于 2k/50k 诊断运行（S12/S14）。LOCAL-FE-PACING 250k 只有 checkpoint 前沿逐点指纹（S39），H1/H2 裁决门第 4 条只能用前沿指纹差分代理。
- H3 必需的逐事件教师序列（top1Share/归一化熵）在 250k 运行 NOT_EXPORTED；本地最优代理为 A4-50k-ON（S15，约 2.63 万事件）与 S16 聚合表。
- GAP-PROBE-V2 与 FC6 的 front.csv 仅含 Cmax,TEC,TWC 三列，无候选指纹（NOT_EXPORTED），覆盖差距只能按坐标分析。
- S11 为实现期 schema 样例（值为 FP_A4_001 演示），不得当真实运行数据引用。

## 文件

- `evidence-source-registry.csv`：40 数据源（含目录级 3 条）。
- `field-availability-matrix.csv`：23 个候选级/时序字段 × 可用位置。
- `historical-decision-map.csv`：10 条历史裁决（每条区分"否决的是修法不是机制"）。
- Agent A/B/C 详版取证见 `docs/evidence/V35-GAP-LEVERAGE-AUDIT/agent-*-findings.md`（本轮未逐字登记，已由 S32/S33 覆盖其结论）。
