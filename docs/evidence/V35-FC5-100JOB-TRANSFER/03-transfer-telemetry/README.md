# FC5-T 旁路遥测启动说明

状态：`FIRST_TIER_50K_ACCEPTED_PENDING_ANALYSIS`。本目录的 `telemetry-replay-registry.csv` 是首档
24条50k诊断重放的唯一清单。任务已于2026-08-25 18:41（Asia/Shanghai）在训练机独立目录启动并
完成：24/24运行、12/12配对组通过输入与证据验收。启动绑定、远端位置和验收摘要见
`REMOTE_LAUNCH_20260825.md`。本状态不表示H1已经成立，也不授权升级预算。

## 为什么需要重放

四个100-job正/负对照已有最终前沿、统一指标、预算/provenance和 Cmax 生命周期，却没有本审计
判定必需的合并池字段：`Nmerge`、精确去重 `Nunique`、严格非支配 `Nnd`、`Roverflow`，以及每轮
`E_C/E_E/E_W/E_B` 的 PDDR 槽位、后续教师使用和有效后代。因此不能从旧日志把 FC-5 机制写成已经
迁移确认。

## 旁路不变量

`FC5_100JOB_TRANSFER_V1` 只读取真实 `GLOBAL_ORIGINAL` PDDR 的同一合并候选池。它不向搜索路径
提供候选、分数、随机数、FE、Q 表、archive 或控制决策。启用前必须在同一输入上验证：

```text
initialPopulationHash
RNG event-consumption sequence
candidate fingerprint + (Cmax, TEC, TWC) sequence
PDDR selected identities and physical slots
Q-table state
FE
canonical-sorted final-front set
```

均与关闭观察时相同。禁止以带时间戳的原始 CSV 字节是否相同作为该验收的替代品。

## 启动顺序

1. 每个正/负对照只先运行注册表中的前三个配对 seed，预算为 50k。
2. 每个比较块完成后，先核验行为等价、输入/配置哈希、前沿、预算和遥测文件哈希。
3. 只有50k还未覆盖历史指标分叉区间，才将**该比较块**升至100k；再依次为250k和必要时500k。
4. 任一档足以接受或否定 H1 时停止该块；不可把“缺少结果”当成默认升级500k的理由。

结果只可写成：`FC5_TRANSFER_CONFIRMED`、`FC5_TRANSFER_NOT_CONFIRMED`或
`INSUFFICIENT_EVIDENCE`。即使确认，也只是可反驳的 root-cause candidate；修改PDDR仍需一份新预登记
的单变量修复实验。
