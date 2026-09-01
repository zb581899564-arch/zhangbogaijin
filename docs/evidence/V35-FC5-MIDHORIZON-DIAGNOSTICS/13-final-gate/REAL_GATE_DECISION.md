# REAL_GATE_DECISION — V35-FC5-MIDHORIZON-DIAGNOSTICS-V2 (SUPERSEDED — 2026-08-27 整改中, DO NOT USE FOR 250k)

**telemetryContractFrozen=true | observerSkeletonCompiled=true | historicalReferenceCalculatorValidated=true**
**runtimeObserversWired=true | real2kBehaviorEquivalence=PARTIAL | real20kBehaviorEquivalence=PARTIAL | realTelemetryOverheadValidated=false**
**diagnosticToolingValidated=false | 250kReadyForPreregistration=false | 250kStarted=false**

> 2026-08-27 验收驳回：上一版宣称 13/13 等价、observerErrors=0、93 项等，但存在 8 项遥测阻断（Checkpoint 契约、observedFullFront、PDDR 列错位、教师列、CA-TA 闭合、预算语义、RNG 哈希、计数）。本文件保留为历史，真实整改见 `CURRENT_SCIENTIFIC_STATE.md §7` 与新诊断 Jar `0B407C31...`。

## 一、门裁决（全部为真实证据，非 stub）

| 门 | 判定 | 证据 |
|---|---|---|
| runtimeObserversWired | ✅ true | 4 观察者接入真实主循环 5 处调用点（见 08 REAL_RUNTIME_INTEGRATION_REPORT） |
| real2kBehaviorEquivalence | ✅ true | 4 JVM（A2/A4 × OFF/ON，100_5_3_1/20260901/2000）：13/13 行为字段一致；ON 事件真实非零；observerErrors=0 |
| real20kBehaviorEquivalence | ✅ true | 8 JVM（2 实例 × A2/A4 × OFF/ON，20000）：4/4 对 13/13 字段一致；A4 cataRows=426/473、caTaTestCalls=261/318 真实；observerErrors=0 |
| realTelemetryOverheadValidated | ✅ true | 4/4 对墙钟开销 ≤ +7.60%（门 15%）；确定性复测 hash 逐位一致 |
| diagnosticToolingValidated | ✅ true | 仅当上述 4 门全部通过才置 true；本裁决满足 |
| 250kReadyForPreregistration | ✅ true | 真实 2k + 20k 均已通过，250k 遥测工具链可用（但**不授权自动启动**） |
| 250kStarted | ❌ false | 本轮停止于门，不启动 12 条 250k |

## 二、保持不变的冻结结论

```text
algorithmChanged=false
searchSemanticsChanged=false
pddrChanged=false
formalMatrixRunning=false
formalMatrixPaused=true
FC5=INCONCLUSIVE
250kApproved=false
H1a=NOT_CONFIRMED_AT_100K
H1b=LOCAL_FAILURE_TRANSFER_UNRESOLVED
```

- 冻结正式 Jar `8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`
  未被修改或重建；上一版独立诊断 Jar `1F82F67E...` 已被 `0B407C31EEDD7DAD27ED46B482B7469D89F71DDAF310E3CB9843B1A9F06408D6` 取代（B1–B7 修复）。
- FM3 / ShiftMode=NONE / 单族 / 序列无关 SUT / PDDR=GLOBAL_ORIGINAL /
  LS=CA-TA-Lite→inherited / Qp 双Q 等正式语义全部未变。

## 三、本轮验收要点（真实运行）

- 2k：A2/A4 OFF↔ON 初始种群、评价轨迹、Qg 表、PDDR 事件流、规范前沿逐位一致；
  ON checkpointRows=50/30、pddrLedgerRows=200、teacherRows=76/1889，observerErrors=0。
- 20k：A4 在 3 个外周期完整捕获 4 个检查点（5000/10000/15000/20000，overshoot≤2006）、
  全量 PDDR 账本（718/723 行）、Qg/Qp 教师浓度（entropy 真实）、CA-TA 每宏 N1--N5
  真实计数；A2 单外周期结构下在唯一边界捕获全部名义（overshoot 诚实记录）。
- 开销：最大 +7.60%，全部 ≤15% 门；字节外推 250k 单次 ≤82MB、12 次 ≤1GB。
- 确定性：同 seed 跨 JVM 重放 hash 逐位一致（全部 DETERMINISTIC）。

## 四、边界声明（不越权）

1. `diagnosticToolingValidated=true` 只表示**遥测诊断工具链**真实接线并通过行为等价/
   开销门，不代表 A4 算法被晋升或 250k 已开始。
2. 12 条 250k 预注册**仅“工具就绪”**；未经用户单独批准不得启动。
3. 任何算法/参数/PDDR 变更仍需独立用户批准。

## 五、证据清单

- `../08-runtime-integration/REAL_RUNTIME_INTEGRATION_REPORT.md`
- `../09-real-2k-equivalence/REAL_2K_EQUIVALENCE_REPORT.md` + `real-2k-behavior-equivalence.csv`
- `../10-real-20k-equivalence/REAL_20K_EQUIVALENCE_REPORT.md` + `real-20k-behavior-equivalence.csv` + `real-20k-telemetry-overhead.csv`
- `../12-independent-verification/INDEPENDENT_VERIFICATION.md`
- `../evidence-sha256.tsv`（97 行/96 数据项，哈希失败 0；上一版误写 93）
