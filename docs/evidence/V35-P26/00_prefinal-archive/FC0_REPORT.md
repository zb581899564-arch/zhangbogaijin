# V35-FC-0 A4-PREFINAL 存档

日期：2026-08-17。依据 D-082（v3.5-Final Candidate）：本存档是整个 FC 流水线（FC-1..FC-9）的回退锚点。存档语义 = 当前正式 A4：DSCR+CFVF+Qp+Qg+CA-TA-Lite、方向教师池关闭、BAL 全开放无 shadow、dualQ blockFrozen(0.10, 5, 5)、Table-9 LS_Times=30；无 local-FE 预算调度、无软冻结（两者分别由 FC-2/FC-4 引入）。

- `FREEZE_MANIFEST.txt`：语义版本 + 正式配置 canonicalText（哈希 116393b4e074c1918e1f0983adf32c9312ba439e9a8f99a7436ebf30d79b6e76）+ Table 9（哈希 8C2D808121E4A397A6C31FB82D440A5AB131315BBDECE1BAE0CA13F1706149D2）+ dualQ canonicalText；幂等契约：磁盘比对，漂移即失败
- `source-sha256.csv`：与 V35-P24 冻结相同的生产源码树（187 文件）——后续 FC 包引起的源码变化可用两份 CSV 的 diff 精确隔离
- `environment.txt`：运行环境
- 重放门：`20_2_3_1`、seed 20260808、20000 FE × 3 次连续重放，front SHA-256 = `c41c81332188d1c5dc5a8e29c0cbc9056a315b8a38b4383dfe26a7e2179dd528`，三次逐位一致

## 门

FC-0 之后任何机制改动不得直接改生产默认；必须通过对应 FC 工作包并保持向后兼容门（配置缺省 = 存档语义）。FC 流水线全部失败时回退到本存档（其 500k 已知表现：HV 均势、IGD/TEC 4/4 领先）。
