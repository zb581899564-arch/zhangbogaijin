# 外部基线 Fair-Ready 最终裁决

- 日期：2026-08-30
- 机器可读：同目录 `readiness.properties`
- 上游依据：PFC5 Phase 0 基线审计（HMOPSO-QLS-F/MOEA/D/QMOEA 状态不变）、
  工作包 §十五 的十条 true 必要条件

## 裁决

```ini
NSGA_II_F_fairReady=true
SPEA2_F_fairReady=true
```

裁决不基于任何前沿外观——2k 前沿数值未进入任何门；十条必要条件全部由
来源/代码/行为证据支撑（证据索引见下）。

## 十条必要条件对照

| # | 条件 | NSGA-II-F | SPEA2-F | 证据 |
|---|---|---|---|---|
| 1 | OFFICIAL_JMETAL_CORE | true | true | GitHub tag jmetal-5.8（commit 831d62d0，API 验证）与隔离副本逐行 diff 零算法差异；m2 重建 jar 明确不作为证据；MIT 许可证同源（01-upstream-source/） |
| 2 | minimalProblemAdaptationOnly | true | true | adapter-contract.csv：单一 canonical 委托、显式初群队列、表示级 repair 仅限 7 槽视图（本链路不可达）；本轮零适配新增（复用 P25E 现件） |
| 3 | objectiveAdapter016 | true | true | THREE_OBJECTIVE 视图强制（引擎新门）；槽2=TWC 映射；槽2–5 读取 0 命中（扫描） |
| 4 | sameInitialPopulation | true | true | 4 条 run 同一 V35 初群哈希 `165ad2bc…`；公平性测试双臂 drain 哈希一致；快照=正式 manifest 同值 |
| 5 | exactDecoderAccounting | true | true | FE=decoderCalls=canonical counter=2000 精确闭合；duplicateEvaluations=0（预算守卫） |
| 6 | algorithmIdentityVerified | true | true | 身份测试 5/5：tournament/crossover/mutation/sort+crowding（NSGA-II）/strength+rawFitness+density+archive+truncation（SPEA2）计数全正；runner 落盘 event-summary.properties |
| 7 | forbiddenV35MechanismsAbsent | true | true | 静态扫描 91 项 0 违规；身份证据串无任何 V35 机制词；forbiddenMechanismEvents=0 |
| 8 | 2kSmokePassed | true | true | 4/4 COMPLETED，全部验收门通过（SMOKE_REPORT.md） |
| 9 | deterministicReplayPassed | true | true | 同 seed 独立 JVM ×2：front hash 与 FE 逐字节一致（双算法） |
| 10 | Java8Passed | true | true | 编译 target=1.8（major 52）不变；六模块打包成功 |

## 限制（不阻断 readiness，须随行）

1. **2k=ENGINEERING_SMOKE**：一切结果不得进入论文 reference、排名、优越性结论。
2. **首次正式使用前**：仍须按当时有效的实验子路线图完成 production preflight
   （更大烟测、预算协议对齐、独立 JVM/随机流环境登记）；本裁决不解锁任何
   50k+ 运行。
3. **确定性重放范围**：同一宿主机/JVM 家族下验证；跨机器重放未测（正式运行
   以 manifest+seed+快照哈希为准，不依赖跨机位重放）。
4. HMOPSO-QGS-F 维持 Phase 0 的 fairReady=true；HMOPSO-QLS-F
   （PENDING_SOURCE_VERIFICATION）、MOEA/D、QMOEA 状态不变。

## 边界确认

```text
formalV35JarChanged=false（8dad8f40… 重算不变）
diagnosticRuntimeJarChanged=false（121fbb49… 不变）
diagnosticBaseJarChanged=false（723d24ed… 不变）
PDDR/CFVF/DualQ/CA-TA changed=false
formalMatrixRunning=false
externalBaselineFormalRunsStarted=false
gapProbeStarted=false
validationStarted=false
finalCandidateSet=false
f2Started=false
f3Started=false
```
