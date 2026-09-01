# V2 独立复核报告（REAL）

- 工作包：`V35-FC5-MIDHORIZON-DIAGNOSTICS-V2`
- 目的：对 2k/20k 真实遥测证据做独立反向复核，确认不存在 stub、手造 CSV、
  空实现或时间戳污染。

## 1. Jar SHA 反向复核

- 声明 SHA-256：`1F82F67E6A6515B56DD1EFEBC99A1A895150649AFA860BCB5D6B91616F63167A`
- 实际 SHA-256（对 `08-runtime-integration/build/...-V2-diag.jar` 复算）：相同 ✅
- 冻结正式 Jar `8DAD8F40...` 未参与本次运行；本次运行仅使用上述独立诊断 Jar。

## 2. 主循环调用点复核（5 处，全部带空安全 + isEnabled 守卫）

| 调用点 | 位置 | 守卫 |
|---|---|---|
| `onAtomicPhaseEnd`（checkpoint） | ZhangBoMOHPSOQ:838 | `!= null && isEnabled()` |
| `onTeacherUse("QG", ...)` | :2939 | `!= null && isEnabled()` |
| `onTeacherUse("QP", ...)` | :3364 | `!= null && isEnabled()` |
| `onCaTaCandidate(...)` | :5272 | `!= null && isEnabled()` |
| `onPddrRound(...)` | :9579 | `!= null && isEnabled() && pddrAll != null` |

OFF 运行：telemetry 对象为 null（`runMidHorizonDiagnostic` 在 telemetryOn=false 时不构造），
所有调用点在 `!= null` 短路下完全跳过，零额外对象构造、零随机消耗、零 FE 变化。

## 3. 非空遥测复核（ON 运行全部四类 CSV 非空且含真实数据行）

- 2k A2/A4 ON：checkpoint-fronts / pddr-full-ledger / pddr-cycle-summary /
  teacher-use-events / teacher-concentration 全部含数据行；cata 两文件仅表头
  （A2/A4 在 2k 预算下无 CA-TA 事件，见 2k 报告第 5 节）。
- 20k A4 ON：checkpoint-fronts（4 名义全覆盖，overshoot≤2006）、
  pddr-full-ledger（718/723 行）、pddr-cycle-summary（3 周期）、
  teacher-use-events（12894/14185 行）、teacher-concentration（entropy 真实）、
  cata-contribution-events（226370 字节）+ summary（N1--N5 每宏真实计数）。
- 检查每行带 `generatedByRunId/sourceJarSha256/configurationHash/instanceHash/seed/arm`，
  无手工伪造特征。

## 4. 非 stub 复核

- 所有汇总（teacher 浓度 entropy、PDDR cutoff、CA-TA generated/accepted、
  checkpoint ND 过滤）均由观察者从真实运行状态现场计算，写入由运行生成的 CSV；
  不存在预置/手填数值。
- OFF 运行不生成任何 telemetry CSV（目录内只有 behavior-summary + canonical-front）。

## 5. 时间戳排除复核

- 行为 hash（initialPopulationHash / evaluationTraceHash / qgTableHash / qpTableHash /
  pddrEventStreamHash / canonicalFrontHash）全部由算法状态计算，不含 wall clock。
- `wallNanos` 仅单独记录用于开销门，**不参与任何 hash**。
- checkpoint/PDDR/teacher/cata CSV 行内无时间戳字段。

## 6. 确定性复核（同 seed 跨 JVM 重放）

对 20k 四对 OFF/ON 重新执行同一命令，对比
`canonicalFrontHash/evaluationTraceHash/qgTableHash/pddrEventStreamHash`：
全部 DETERMINISTIC。证明遥测接线不改变行为，且行为可逐位复现。

## 7. 结论

```text
observer 非空        = PASS
主循环调用点接线      = PASS（5 处，均守卫）
非 stub CSV          = PASS
SHA 反向复核          = PASS
时间戳不入 hash       = PASS
```

独立证据入口：
- `../09-real-2k-equivalence/`
- `../10-real-20k-equivalence/`
- `../evidence-sha256.tsv`（93 项，含每运行 behavior-summary / canonical-front / telemetry CSV）
