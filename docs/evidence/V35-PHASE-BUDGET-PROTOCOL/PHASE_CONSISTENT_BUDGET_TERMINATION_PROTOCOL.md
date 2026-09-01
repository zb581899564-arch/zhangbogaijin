# V35 Stage2：Phase-Consistent Budget Termination 协议冻结

日期：2026-08-23  
协议版本：`v35-phase-consistent-budget-v1`  
术语：`PHASE_CONSISTENT_BUDGET_TERMINATION`

## 1. 已批准的预算含义

正式 `MaxFEs=500000` 是**允许的完整评价上限**，不是要求每条运行都恰好消费
500000 次评价的等式。冻结搜索保持一个完整 Q phase 不可拆分；不得开启 terminal
partial Q phase、补评价、改变局部 FE 配额、Q/LS 顺序或任何算法参数。

对每条正式运行，外部审计必须同时满足：

```text
0 < actualFE = decoderCalls <= requestedMaxFE
remainingFE = requestedMaxFE - actualFE
qPhaseFE = population × Q_Times = 100 × 50 = 5000
0 <= remainingFE < qPhaseFE
```

`remainingFE=0` 记为 `EXACT_MAX_FE`；其余合法情况记为
`PHASE_CONSISTENT_TAIL_STOP`。任一超预算、零 FE、decoder 不闭合或尾段不小于
一个完整 Q phase 都是 `INVALID`，不得进入参考前沿、指标或统计。

在正式 500k 条件下，该门蕴含 `utilizationRate > 99%`。同一
`(instance, seed)` 的 A0--A4 五臂组还必须共享实例、seed、初始四向量和
`requestedMaxFE`，并满足：

```text
max(actualFE) - min(actualFE) < 5000
```

## 2. 冻结身份与不变边界

本协议只增加**运行后只读分类与证据**，不修改冻结算法或重新构建 jar：

| 绑定项 | SHA-256 / 值 |
|---|---|
| Final tag | `v35-final-doe1-frozen` → `2b3316b21512ff9d1d7f3db972f016ba02edac6e` |
| Frozen fat jar | `8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9` |
| Frozen A4 config | `cff6bbca0a8357ae848e625710c0ba39a1c9419becd84ef4e95f8bb6f88db09e` |
| Source/config bundle | `ac92eda152348ce11861ec5c2f223e6a9c7643afd50cbaa5d48189d1fc41f0fd` |
| Population / Q_Times | `100 / 50` |
| qPhaseFE | `5000` |

正式 A0--A4 语义、实例、45×20 seed roster、900 共享初群 snapshot、FM3、单族、
序列无关 SUT、`ShiftMode=NONE`、`GLOBAL_ORIGINAL`、
`CA-TA-Lite → inherited LS`、P=5/G=5、rho=0 与方向教师池关闭均保持原冻结值。

## 3. 外部实现与记录

- `docs/evidence/V35-PRODUCTION-PREFLIGHT/tools/V35ProductionPreflight.java`
  在 `V35FairRunner.run(...)` 返回后分类预算，输出
  `budget-termination.properties`；它不接触算法对象的输入或状态。
- `tools/v35_phase_budget_master_adapter.py` 已实现预算分类、五臂组审计和汇总；它只可
  包裹具备显式 Master RunKey / snapshot 绑定接口的冻结渲染器。当前冻结树中该渲染器
  尚未交付，因此 adapter 不得被误作正式矩阵 launcher；这一 fail-closed 事实已登记在
  `06-formal-launch-readiness/FORMAL_MATRIX_BLOCKER.md`。
- 每个运行的 `budget-termination.properties` 必须包含
  `requestedMaxFE`、`actualFE`、`decoderCalls`、`remainingFE`、`qPhaseFE`、
  `utilizationRate`、`terminationKind`、`formalOuterCycles`、
  `formalQgRounds`、jar/config/snapshot hash。
- 组审计输出 `budget-utilization.csv` 与 `group-budget-audit.properties`。任何
  `INVALID` 组禁止进入 PFref、HV、IGD 与后续统计。

预算审计只在算法返回后读取 `RunRecord` 和已写出的证据，因而其开/关不可能改变
随机流、前沿、Q 表、动作轨迹、FE 或算法配置 hash；该不干预边界由代码结构和
分类器单元测试共同固定。

## 4. Gate3 重验与后续顺序

1. 用冻结 jar 在 `20_2_3_1`、seed `20260828`、population 100、50k 下，独立 JVM
   重跑 A0--A4；先验收共同初群、phase-bound、合法性、机制与证据哈希。
2. Gate3 五臂全部通过后，才在训练机执行冻结 jar 的 `4/8/12/16 JVM` 吞吐基准，
   决定 `FORMAL_MAX_PARALLEL`。
3. 只有预检、吞吐、正式 A0--A4 arm launcher 与显式 Master renderer 全部通过，外部
   adapter 才能调度 `A0--A4 × 45 instances × 20 seeds = 4500` 条 500k 运行。冻结 jar
   不能由 adapter 擅自补出缺失的 A1--A3 arm 实现。
4. 任何预检、吞吐或正式五臂组违反 phase-bound，立即标记该组 `INVALID` 并停止
   其后的指标/统计；不得以补评价或改变 phase 语义修复。

## 5. 历史证据隔离

此前 strict-exact 20k/50k/100k 记录保留为
`legacy_pre_phase_budget_protocol`。它们说明尾停的真实存在，但不再用
`requestedFE=actualFE` 否定冻结算法，更不进入本协议之后的性能或统计结果。

本协议不等于论文优越性结论，也不启动或完成正式统计；Gate3 与吞吐虽可接受，但在
正式 A0--A4 launcher / Master renderer 缺口闭合前，`formal_matrix_started=false`。
