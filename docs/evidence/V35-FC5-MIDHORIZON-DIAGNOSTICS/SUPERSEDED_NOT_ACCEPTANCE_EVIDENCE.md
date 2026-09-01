# SUPERSEDED_NOT_ACCEPTANCE_EVIDENCE

本标记用于防止旧遥测结果被误当作 V35-FC5-MIDHORIZON-DIAGNOSTICS-V3 的最终验收证据。以下内容保留用于历史追溯，但不参与本轮 acceptance gate：

- 00-preregistration 至 13-final-gate，以及 14-final-sequence-audit 下原有 run 子目录中的旧 V2、旧 fallback、旧摘要与旧 gate 结果；
- 18-final-2k-20k-50k-gates 中所有不以 -final 结尾的运行目录，包括 *-v1、A2-20k-ON、A2-20k-ON-v2、A2-20k-ON-v3、A2-20k-ON-v4、A4-20k-ON-v1 及旧 default-heap 试跑；
- 任何引用旧 diagnostic Jar SHA、pool ordinal、partial formal Q、placeholder teacher/CA-TA 字段或未完成 checkpoint 的文件。

本轮 acceptance evidence 仅限：

- 14-final-sequence-audit/FINAL_V3_SEQUENCE_AUDIT.md；
- 15-final-pddr-provenance；
- 16-final-teacher-cata-lifecycle；
- 17-final-checkpoint-budget；
- 18-final-2k-20k-50k-gates 下的 *-final 运行目录；
- 19-final-independent-verification；
- 20-final-decision。

旧结果没有被删除或覆盖；它们只是被明确标记为 SUPERSEDED_NOT_ACCEPTANCE_EVIDENCE。
