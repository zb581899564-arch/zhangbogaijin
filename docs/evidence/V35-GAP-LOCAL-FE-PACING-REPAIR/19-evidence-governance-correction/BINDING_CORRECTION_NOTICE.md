# BINDING_CORRECTION_NOTICE — 250k 预登记清单漂移治理收口

- 日期：2026-08-31
- 性质：**证据治理收口（append-only）**。不重跑实验、不改科学输入、不改算法与预登记设计。
- 触发：V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT-V1 任务书 §二。

## 1. 事件还原

250k 执行期间（2026-08-31），首批 3 条臂（seed-20260916 × 50_2_3_1 的 C0/C2/C3）启动即失败（exit=1），异常为：

```
binding.setupFileSha256 expected=…a54df54(63 chars) actual=…a54df54e(64 chars)
```

根因：`15-250k-preregistration/artifact-binding.csv` 中 `setupFileSha256-50_2_3_1` 一行的手工转录丢失末位字符（63 位），由该 CSV 程序化生成的 6 份 binding 文件中 50_2_3_1 的三份继承了截断值。**实例 setup 文件本身从未改变**（修复前后该文件 SHA-256 均为 64 位真值 `…a54df54e`）。经用户授权，主Agent以真实文件哈希重新生成 6 份 binding 并推送远端，重试 1 次后 18/18 全部成功。

## 2. 漂移范围（与任务书核实清单一致，5 项）

`15-250k-preregistration/evidence-sha256.tsv`（修复前版本，44 项）中 5 项与当前文件不闭合：

1. `artifact-binding.csv`
2. `staging-250k/seed-20260916/bindings/50_2_3_1.binding.properties`
3. `staging-250k/seed-20260917/bindings/50_2_3_1.binding.properties`
4. `staging-250k/seed-20260918/bindings/50_2_3_1.binding.properties`
5. `staging-250k/upload-sha256.tsv`（上传清单随 binding 修复重新生成）

逐项前后哈希见 `preregistration-manifest-drift.csv` 与 `binding-hash-before-after.csv`。

## 3. 清单处理（原清单保留，不删除、不改写）

- `evidence-sha256.tsv`（修复前状态）：**原样保留**于 15 目录；它记录的是 binding 修复前的预登记状态，与上列 5 个被覆盖文件**不再闭合**——这是如实登记的漂移，不是清单错误。
- `evidence-sha256.pre-binding-correction.tsv`：上一行的副本（历史快照）。
- `evidence-sha256.post-binding-correction.tsv`：修复后 15 目录的新清单（45 项，含两个历史清单文件本身），反向复算 **missing=0 mismatch=0**。

## 4. 失败 attempt 证据的诚实限制

三条首次失败臂（GAPL250K-C0/C2/C3-50_2_3_1-20260916）的独立日志文件名与重试成功臂相同（`logs/{arm}-50_2_3_1-20260916.log`），重试覆盖了首次失败输出。**未伪造、未补写、未事后重建**任何失败 arm 日志。仍然保留的失败证据：

- 总日志 `sync/logs/run-all-250k.log`：首次 `START/END … exit=1` 三行与第二次 `ALL_18_RUNS_DONE` 完整可查；
- `REMOTE_250K_EXECUTION_REPORT.md`：失败堆栈、时间线、重试与授权记录；
- 失败属"启动即绑定校验拒绝"（零评估发生），不存在部分科学输出。

## 5. 机器状态

```ini
failedAttemptArmLogsPreserved=false
failedAttemptSummaryEvidencePreserved=true
scientificInputsChanged=false
algorithmChanged=false
preregisteredDesignChanged=false
scientificResultsAffected=false
preregisteredManifestEntriesDrifted=5
driftExplainedAndDocumented=true
postCorrectionManifestClosed=true
topLevelManifestRegenerated=true
evidencePackageFinalSignoff=true
```

## 6. 顶层清单重建

LOCAL-FE-PACING 证据树顶层 `evidence-sha256.tsv` 已在本收口中重新生成并反向复算（覆盖 19 目录与 pre/post 清单本身；见本目录 `evidence-sha256.tsv` 与顶层文件），全部闭合后方可置 `evidencePackageFinalSignoff=true`。
