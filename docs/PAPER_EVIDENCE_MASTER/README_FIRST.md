# V35 论文证据总账：请先读本文件

更新时间：2026-08-30（Asia/Shanghai）  
状态：`CATALOG_ARCHIVED / FORMAL_CAMPAIGN_PAUSED / COMPETITIVE_ROUTE_ACTIVE`

## 一句话现状

当前不存在已批准的V35 Final：A4和A2均未通过跨实例晋升门，旧4500条A0--A4正式矩阵继续暂停。
FC5候选膨胀假设未在250k得到支持，Teacher Exposure修复因只覆盖1.12%的教师事件已关闭。当前活动路线已
转为“Gap Probe → 单一repair family杠杆审计 → 分级开发V35-R → DOE迁移 → 未污染Validation →
Final Freeze → 正式消融和baseline比较”。当前仅允许Gap Probe的0-FE预登记，不得恢复正式矩阵或修改算法。

## 阅读顺序

1. [`CURRENT_SCIENTIFIC_STATE.md`](CURRENT_SCIENTIFIC_STATE.md)：当前算法、参数和状态。
2. [`PAPER_EVIDENCE_MASTER_INDEX.md`](PAPER_EVIDENCE_MASTER_INDEX.md)：P1 到 Stage2 的完整证据路线。
3. [`PAPER_CHAPTER_SOURCE_GUIDE.md`](PAPER_CHAPTER_SOURCE_GUIDE.md)：写论文时每一节去哪里取数据。
4. [`CLAIM_EVIDENCE_MATRIX.md`](CLAIM_EVIDENCE_MATRIX.md)：哪些话可以写，哪些话不能写。
5. [`REMOTE_EXPERIMENT_MAP.md`](REMOTE_EXPERIMENT_MAP.md)：训练机每个实验目录是什么。
6. [`LEGACY_AND_NEGATIVE_RESULTS.md`](LEGACY_AND_NEGATIVE_RESULTS.md)：淘汰机制、失败实验和隔离边界。
7. [`CLEANUP_EXECUTION_REPORT.md`](CLEANUP_EXECUTION_REPORT.md)：归档、清理和恢复状态。
8. [`FINAL_CLEANUP_AND_RESTORE_REPORT.md`](FINAL_CLEANUP_AND_RESTORE_REPORT.md)：最终验收结论和恢复边界。
9. [`../V35_COMPETITIVE_SUPERIORITY_EXECUTION_ROADMAP.md`](../V35_COMPETITIVE_SUPERIORITY_EXECUTION_ROADMAP.md)：当前竞争优势、DOE迁移、消融和正式实验主路线。

冷归档与最终交付包位于：

```text
G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823
```

## 状态词

| 状态 | 论文含义 |
|---|---|
| `ENGINEERING_VALIDATED` | 工程实现和测试通过，可用于方法描述 |
| `PAPER_PARAMETER_SELECTION` | 可用于说明参数如何冻结，不是算法优越性证据 |
| `PILOT_PROMISING_SIGNAL` | 只表示先导信号，不得表述为统计显著 |
| `NEGATIVE_PARAMETER_DECISION` | 可用于附录或参数裁决说明 |
| `FORMAL_STATISTICAL_RESULT_PENDING` | 正式实验尚未完成 |
| `FORBIDDEN_FOR_PAPER_CLAIM` | 只能作历史审计，不得进入论文效果结论 |

## 唯一事实源优先级

```text
本目录 CURRENT_SCIENTIFIC_STATE.md
→ docs/ROADMAP.md 最新决策
→ 当前阶段正式报告
→ 历史工作包报告
```

旧文件即使仍写着 `RUNNING`、`BLOCKED` 或 `formal_matrix_started=true`，也不得覆盖这里记录的
`formalMatrixRunning=false / formalMatrixPaused=true`。
