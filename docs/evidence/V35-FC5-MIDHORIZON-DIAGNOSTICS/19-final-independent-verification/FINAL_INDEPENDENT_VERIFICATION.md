# V35-FC5-MIDHORIZON-DIAGNOSTICS-V3：独立反向验收

本报告使用最终源码 Jar 与 18 号目录中的 final-suffixed ON/OFF 成对结果复核。最终 manifest 已覆盖 112 个 acceptance-evidence 文件，并在生成后完成逐文件长度与 SHA-256 反向校验。

## 12 项独立检查

| # | 检查 | 结果 |
|---:|---|---|
| 1 | RNG 来源为 actual jMetal random，而非 fallback | PASS |
| 2 | RNG streaming digest 非空且 draw count > 0 | PASS |
| 3 | JS/FA/MA/WA 四向量来自实际 pre-evaluation candidate，数量闭合到 decoderCalls | PASS |
| 4 | candidate identity 无 illegal solution、无 duplicate evaluation | PASS |
| 5 | PDDR 无 pool ordinal / index % 4，物理槽来自实际 sub-swarm 语义 | PASS |
| 6 | A4 lineage/parent 使用真实 tag 与真实 parent ID；A2 lineage 按配置显式不适用 | PASS |
| 7 | Qg/Qp required teacher fields 非 placeholder，offspring backfill 完整 | PASS |
| 8 | CA-TA 具有稳定 ID、完整 lifecycle 字段与显式 right-censor；unobservable fields 为 0 | PASS（A4-2k 的“无事件”短门除外） |
| 9 | checkpoint 使用 atomic boundary；20k 与 A2-50k 全部可观测 | PASS |
| 10 | formal budget phase-consistent，partial formal Q 被禁止 | PASS |
| 11 | ON/OFF 核心行为 hash 相等 | PASS（16/16 成对运行） |
| 12 | manifest reverse hash failures | PASS（0 failures） |

## ON/OFF 核心一致性与 overhead

每一对的 instance、seed、actualFE、decoderCalls、initialPopulationHash、evaluationTraceHash、qgTableHash、qpTableHash、pddrEventStreamHash 与 canonicalFrontHash 均相等。OFF 的 actual RNG/candidate digest 按设计为 NOT_APPLICABLE，不把关闭观测误当作 actual evidence。

| 成对运行 | ON wall | OFF wall | overhead |
|---|---:|---:|---:|
| A2 20k 100_2_4_1 | 11.910s | 10.465s | 13.82% |
| A2 20k 100_5_3_1 | 13.865s | 12.345s | 12.31% |
| A4 20k 100_2_4_1 | 16.887s | 15.643s | 7.95% |
| A4 20k 100_5_3_1 | 19.886s | 18.390s | 8.14% |
| A2 50k | 18.369s | 16.578s | 10.80% |
| A4 50k | 33.940s | 30.527s | 11.18% |

20k/50k 的完整链路 overhead 最大为 13.82%。2k 是已知 source-coverage 尚未闭合的短门，A2 为 14.77%，A4 为 17.54%，不用于替代完整链路 overhead gate。

## 输出规模

投影按任务规定的公式计算：

projected250kBytes = actualTelemetryBytes / actualFE × 250000 × 1.25

其中 checkpointBytes、pddrLedgerBytes、teacherBytes、cataBytes 分别包含该组件的 CSV 文件，sequenceAuditBytes 为 0（RNG/candidate 使用 streaming digest，不保留逐调用 audit 文件）。

| run | checkpointBytes | pddrLedgerBytes | teacherBytes | cataBytes | sequenceAuditBytes | actualTelemetryBytes | projected250kBytes |
|---|---:|---:|---:|---:|---:|---:|---:|
| A2-2k-effective-5100 | 34717 | 386958 | 639412 | 1036 | 0 | 1062123 | 65081066 |
| A4-2k-effective-5100 | 24566 | 369273 | 14890921 | 1036 | 0 | 15285796 | 936629657 |
| A2-20k-100_2_4_1 | 242570 | 493964 | 637709 | 1036 | 0 | 1375279 | 21488734 |
| A2-20k-100_5_3_1 | 310983 | 488217 | 638588 | 1036 | 0 | 1438824 | 22481625 |
| A4-20k-effective-20258-100_2_4_1 | 176587 | 1337934 | 42965777 | 1780332 | 0 | 46260630 | 713616688 |
| A4-20k-effective-20258-100_5_3_1 | 309528 | 1340521 | 43033235 | 1625273 | 0 | 46308557 | 714356011 |
| A2-50k | 543262 | 1022204 | 1270746 | 1036 | 0 | 2837248 | 17732800 |
| A4-50k | 598502 | 2837665 | 82994964 | 4591152 | 0 | 91022283 | 589290506 |
| aggregate | 2240715 | 8276736 | 187071352 | 8001937 | 0 | 205590740 | 3080677087 |

最大单运行投影为 936,629,657 bytes，低于 1 GB；8 个 ON 运行合计投影为 3,080,677,087 bytes，低于 15 GB。

## Manifest closure

20-final-decision/evidence-sha256.tsv 采用相对 evidence root 的路径，记录每个文件的 byte length 与 SHA-256；manifest 自身排除在 scope 外以避免循环。最终 reverse check 结果为 entries=112、reverseFailures=0。
