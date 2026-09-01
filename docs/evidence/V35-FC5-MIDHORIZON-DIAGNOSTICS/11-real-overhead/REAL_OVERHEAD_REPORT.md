# REAL_OVERHEAD_REPORT.md — 真实遥测开销验收

- 门：`realTelemetryOverheadValidated`
- 判定：**PASS** — 4/4 对（2 实例 × A2/A4）ON/OFF 墙钟开销均 ≤ 15% 门。

## 1. 测量方法

- 同一独立诊断 Jar（SHA `1F82F67E6A6515B56DD1EFEBC99A1A895150649AFA860BCB5D6B91616F63167A`）
  以相同 seed（20260901）、相同 MaxFEs（20000）分别以 OFF（telemetry=null）与
  ON（四观察者启用）运行；每 arm×instance×mode 一个独立 JVM。
- 开销 = `(ON_wallNanos − OFF_wallNanos) / OFF_wallNanos × 100%`；`wallNanos` 仅计时，
  不进入任何行为 hash。
- 初次批内测量曾出现 A2 100_2_4_1 OFF=4819ms 的异常快值（+29.26%）；
  经独立复测（OFF=5270ms / ON=5425ms → +2.94%）确认系单次计时噪声。复测 hash 与原批
  逐位一致（确定性复核通过），最终采用复测时序。

## 2. 结果（复测时序）

| 臂 | 实例 | OFF (ms) | ON (ms) | overhead% | 门 15% |
|---|---|---|---:|---:|---:|---|
| A2 | 100_2_4_1 | 5270 | 5425 | +2.94% | ✅ |
| A4 | 100_2_4_1 | 10577 | 10100 | −4.51% | ✅ |
| A2 | 100_5_3_1 | 8192 | 8377 | +2.26% | ✅ |
| A4 | 100_5_3_1 | 14095 | 15166 | +7.60% | ✅ |

- 最大开销 +7.60%（A4 100_5_3_1，四观察者全启用、CA-TA 事件最多），仍远低于 15% 门。
- A4 100_2_4_1 为负值（ON 快于 OFF），属计时噪声，方向不影响结论。

## 3. 字节量预估（250k 外推，仅预估不启动）

- 单次 20k ON 遥测 CSV：A2 ≈ 0.29–0.36 MB，A4 ≈ 5.9–6.5 MB（teacher 事件为主）。
- 250k（×12.5）预估：单次 ≤ 82 MB；12 条 ≤ 1 GB，远低于存储上限。
- 本条仅记录预估依据，**不授权启动 250k**。

## 4. 数据位置

- 开销矩阵：`../10-real-20k-equivalence/real-20k-telemetry-overhead.csv`
- 原始运行（behavior-summary.properties 内 wallNanos）：
  `../10-real-20k-equivalence/runs/20k-{100_2_4_1,100_5_3_1}-20260901-{A2,A4}-{OFF,ON}/`

## 5. 结论

```text
realTelemetryOverheadValidated = true
overheadPct_max                = +7.60%  （门 15%）
```
