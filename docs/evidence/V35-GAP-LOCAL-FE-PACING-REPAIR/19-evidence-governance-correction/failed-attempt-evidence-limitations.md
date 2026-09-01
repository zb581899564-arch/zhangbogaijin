# failed-attempt-evidence-limitations.md — 首次失败臂证据的诚实限制登记

## 事实

- 首次失败的 3 条臂：GAPL250K-C0/C2/C3-50_2_3_1-20260916（2026-08-31 17:21:59，exit=1，启动即绑定校验拒绝，零评估发生）。
- 失败原因：binding 文件 setupFileSha256 转录截断（63 位 vs 真值 64 位）。
- 修复后重试使用相同日志路径 `logs/{arm}-50_2_3_1-20260916.log`，**首次失败的独立日志被成功运行覆盖**。
- 未伪造、未补写、未事后重建任何失败 arm 日志。

## 仍保留的失败证据

1. `sync/logs/run-all-250k.log`：首次 `START/END … exit=1` 时间线三行完整保留（总日志按追加方式写入，未被重试覆盖）。
2. `sync/logs/` 中三条臂的重试成功日志（首次失败堆栈已不在，但失败异常文本已完整转录于 `REMOTE_250K_EXECUTION_REPORT.md` 的事件记录节与 `18-250k-decision/` 移交节）。
3. `REMOTE_250K_EXECUTION_REPORT.md`：失败原因、异常文本、授权与重试计数。

## 限制的影响评估

- 失败发生在任何科学评估之前（绑定校验在算法构造前），因此不存在"部分科学输出"丢失问题。
- 丢失的仅是失败 attempt 的独立 stdout/stderr 原始文件；其关键内容（异常类型与消息）已由总日志与执行报告双路径保存。

## 机器状态

```ini
failedAttemptArmLogsPreserved=false
failedAttemptSummaryEvidencePreserved=true
scientificInputsChanged=false
algorithmChanged=false
preregisteredDesignChanged=false
scientificResultsAffected=false
```
