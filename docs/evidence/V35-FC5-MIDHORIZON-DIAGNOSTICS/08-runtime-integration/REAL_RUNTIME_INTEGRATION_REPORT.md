# REAL_RUNTIME_INTEGRATION_REPORT.md

- 工作包：`V35-FC5-MIDHORIZON-DIAGNOSTICS-V2`
- 门：`runtimeObserversWired`

## 1. 结论

```text
runtimeObserversWired = true
```

四个观察者已通过**真实遥测接线**接入冻结算法运行路径（ZhangBoMOHPSOQ 主循环），
全部为纯观察钩子；ON/OFF 行为等价已在真实 2k（4 JVM）与 20k（8 JVM）运行中逐位验证。

## 2. 接入位置（真实调用点，均带空安全 + isEnabled 守卫）

| 观察者 | 触发点 | 真实数据来源 |
|---|---|---|
| `V35CheckpointFrontObserver` | 每外周期结束 `onAtomicPhaseEnd`（:838） | workingPopulation / decisionArchive / passive archive snapshot |
| `V35FullPddrLedgerObserver` | 每次 PDDR 选择后 `onPddrRound`（:9579） | 真实 PDDR 候选池 `pddrAll`、来源 `fc5PddrSources`、选中 `selected` |
| `V35TeacherConcentrationObserver` | Qg/Qp 教师使用 `onTeacherUse`（:2939/:3364） | 真实 Qg 社会教师 / Qp 个人领导 |
| `V35CaTaContributionObserver` | CA-TA-Lite 候选评价 `onCaTaCandidate`（:5272） | 真实 Test/Apply 候选、接受结果 |

OFF 时 `V35MidHorizonTelemetry` 为 null，所有钩子在 `!= null` 处短路，零副作用。

## 3. 构造入口

- `V35FairRunner.runMidHorizonDiagnostic(...)`：telemetryOn=true 时构造 4 观察者 +
  coordinator；false 时传 null。
- `V35MidHorizonDiagnosticDriver.main(...)`：单 JVM 每 (arm, instance, mode)，
  加载 FM3 canonical problem、构建共同初群 100、调用 runner、写证据文件。

## 4. 构建产物

- 独立诊断 Jar：`build/jmetal-algorithm-5.8-V35-MIDHORIZON-V2-diag.jar`
- SHA-256：`1F82F67E6A6515B56DD1EFEBC99A1A895150649AFA860BCB5D6B91616F63167A`
- 冻结正式 Jar 未被修改/重建。

## 5. 验证闭环

真实 2k/20k OFF/ON 运行 → 行为 hash 一致 → 中间事件非零 → 开销 ≤15% →
确定性重放 → SHA 反向复核，全部通过（见 09/10/12 目录报告）。
