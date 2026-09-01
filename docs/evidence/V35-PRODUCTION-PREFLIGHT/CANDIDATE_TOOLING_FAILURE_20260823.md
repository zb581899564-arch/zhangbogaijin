# 候选工具链检查：A4 / 20k FE

这不是 final freeze 结果、不是生产预检结论，也不进入任何正式参考前沿或论文结果。

## 固定输入

```text
jarSha256 = 1320c7a9e55b9015752293f9c0ed9bb53aa678605c2f00f737b78c805e9b9872
freezeBinding = CANDIDATE_JAR_NOT_PRODUCTION_EVIDENCE
arm = A4
instance = 20_2_3_1
seed = 20260828
population = 100
requestedFE = 20000
```

## 观察

```text
algorithm status = COMPLETED
actualFE = 15258
decoderCalls = 15258
illegalSolutions = 0
duplicateEvaluations = 0
nonFiniteObjectives = 0
CFVF repairs = 0
Shift = NONE (all shift counters = 0)
```

DSCR、CFVF、Qp/PA_i、P5/G5、CA-TA-Lite Test/Apply 和 inherited local search 均产生非零事件；方向教师池、shadow 和 shift 均保持关闭。失败原因只有本阶段事先声明的 exact-FE 门：

```text
requestedFE != actualFE: 20000 != 15258
```

`status=COMPLETED` 与 `stopReason=BUDGET_OR_NORMAL_STOP` 表明这不是异常或非法解；它符合当前安全尾段“不生成半全局批次”的停止路径，但不满足本 Track 预注册的“每次诊断请求必须精确达到 FE”规则。

源码审计已定位到具体边界：A4 保留 Table-9 的 `Q_Times=50`，而 population 是 100，
故一个完整 Q phase 固定需要 5000 个全局 FE。`allowTerminalPartialFormalQPhase=false` 时，
外循环仅在剩余 FE 足以容纳完整 5000-FE Q phase 时进入；前两轮还消耗了共享 dynamic local-FE
窗口的 5158 次 CA-TA/inherited LS 评价，留下 4742 FE，因而第三轮不得进入。把它改成 partial Q
phase 虽可追近 20k，却会改变已冻结的 Q/LS 时序，故本 Track 不得这样处理。

## 处置

1. 已保留完整证据：`tooling-candidate-A4-20k/`。
2. 已停止候选 jar 的其余 A0--A4 和远端吞吐运行。
3. 不修改算法、预算、局部 FE、Q 次数或搜索逻辑。
4. Track A final jar 完成后，先以同一不变量重新验证。若 final jar 仍产生相同 non-exact tail，Track C 必须报告 `PREFLIGHT_BLOCKED_BY_EXACT_FE_GATE`，而不能把该 jar 标为通过。
