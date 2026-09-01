# V35 A2/A4 多实例确认：证据入口

状态：`COMPLETED_A4_NOT_PROMOTED`（2026-08-25）。

本目录只保存 [确认协议](../../V35_A2_A4_MULTISCALE_CONFIRMATION_PROTOCOL.md) 定义的 60 条 held-out 配对运行及其裁决证据。训练机独立目录为
`/home/inspur/aicomp/zhangbo-v35-a2-a4-confirmation-20260824`；冻结 Jar 未被修改，旧 4500 条
Master 仍保持暂停。首个 A2/A4 配对先通过后，fail-closed 调度器完成其余 58 条；60/60 均已通过
预算、provenance、前沿与文件级 SHA-256 验收。

## 已锁定范围

```text
instances = 20_2_4_1, 20_5_3_1, 50_2_4_1, 50_5_3_1, 100_2_4_1, 100_5_3_1
seeds = 20260901..20260905
arms = A2_CFVF, A4_BUDGET_AWARE_CATA
population = 100
requestedMaxFE = 500000
jarSha256 = 8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
```

训练开始前，Runner 必须物化并记录实例、SUT、疲劳配置、profile、快照和初群哈希；任何漂移 fail-closed。

## 预期文件

```text
00-preregistration/
01-snapshots/
02-raw-runs/
03-acceptance/
04-reference-and-metrics/
05-decision/
```

远端运行过程产生的 `status/`、30 份共享快照、两份 plan manifest、每条 raw output 与调度进度表均已
反向哈希复核。紧凑本地导入见 `06-remote-analysis-import/`：它包含每实例独立 reference、60条指标、
30条配对增量、裁决与 SHA-256 清单。结果为 `A4_NOT_PROMOTED`，因此确认集不进入正式 PFref 或论文统计，
也不授权救 A4 或恢复4500矩阵。

本目录生成的 confirmation reference 不得与开发、Stage2 单实例先导或将来正式矩阵 reference 混用。
