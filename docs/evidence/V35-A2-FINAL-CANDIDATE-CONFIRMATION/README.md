# V35 A2 主候选确认：证据入口

状态：`COMPLETED_A2_NOT_PROMOTED`。

本目录保存 A0 与 A2 的独立跨尺度主候选确认。其目的仅是决定 A2 是否有资格进行 Final Freeze；它不重跑、
不覆盖 D-103 的 A2/A4 结果，也不进入论文正式统计。

唯一协议为：[`../../V35_A2_FINAL_CANDIDATE_CONFIRMATION_PROTOCOL.md`](../../V35_A2_FINAL_CANDIDATE_CONFIRMATION_PROTOCOL.md)。

目录约定：

```text
00-preregistration/        固定实例、seed、配置与来源审计
01-tooling/                外置运行器和分析器（不改 frozen Jar）
02-preflight/              2k 接线验收
03-raw-runs/               60 条原始运行
04-analysis/               instance PFref、指标和配对门
05-decision/               A2_FINAL_CANDIDATE_CONFIRMED 或 A2_NOT_PROMOTED
```

2026-08-25执行完成：60/60条运行、30/30配对均通过接收门；预注册裁决为
`A2_NOT_PROMOTED`。紧凑分析母表位于`04-analysis/analysis/`，原始运行保留在训练机
`/home/inspur/aicomp/zhangbo-v35-a2-final-candidate-confirmation-20260825/run-r4/results/confirmation/`。
它们均不得进入正式论文PFref或统计。
