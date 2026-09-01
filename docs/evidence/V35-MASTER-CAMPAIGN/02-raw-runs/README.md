# 每条正式 raw run 的最小证据包

正式运行目录固定为 `02-raw-runs/<RunKey>/`，至少必须有：

```text
configuration.txt                 # arm canonical text，包含 ConfigSHA256
provenance.properties             # instance/SUT/fatigue/source/jar/snapshot 绑定
status.properties                 # COMPLETED/FAILED、真实 FE、停止原因
front.csv                         # 最终三目标 raw nondominated front
mechanism-summary.txt             # arm 应有与禁用的计数
initial-population.sha256         # V35 与 P8 两种口径
run-record.csv                    # FE/decoder/非法/重复/repair/source/runtime
console.log                       # 有限的运行控制台记录
evidence-sha256.tsv               # 当前运行目录的证据清单
```

`FORMAL_MINIMAL` 不保存海量逐周期 debug ledger。只有在运行前写入冻结审计清单的
`AUDIT_DETAILED` RunKey 才可额外保存周期事件；两种证据等级都不得改变随机流、FE 或
算法决策。

任何运行若发现 `actualFE != decoderCalls`、`actualFE > 500000`、非法/重复/非有限/异常
repair/来源丢失非零、Shift 活动、初群哈希或 provenance 不一致，必须写 FAILED/INVALID
证据，而非写 COMPLETED。
