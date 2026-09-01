# V35 Stage2 Gate3：Phase-Consistent 50k 五臂重验

日期：2026-08-23  
状态：`ACCEPTED_PENDING_FULL_REGRESSION_AND_THROUGHPUT`  
协议：`v35-phase-consistent-budget-v1`

## 裁决范围

本报告只验收冻结 jar 在新的预算协议下的生产预检，**不是**500k 正式矩阵、性能比较、
参考前沿或论文优越性结论。算法没有修改、没有重建 jar，也没有启用 partial Q phase。

共同条件：

```text
instance=20_2_3_1
seed=20260828
population=100
requestedMaxFE=50000
decoder=FM3; family=DEGENERATE_SINGLE_FAMILY; setup=SEQUENCE_INDEPENDENT
ShiftMode=NONE; GLOBAL_ORIGINAL; CA-TA-Lite -> inherited LS
P=5/G=5; rho=0; directionalTeacherPool=false
```

冻结 jar SHA-256：
`8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9`。

## 预算与公平性结果

| Arm | actualFE = decoderCalls | remainingFE | 利用率 | 终止分类 | 前沿点数 |
|---|---:|---:|---:|---|---:|
| A0 | 50,000 | 0 | 100.000% | `EXACT_MAX_FE` | 69 |
| A1 | 50,000 | 0 | 100.000% | `EXACT_MAX_FE` | 100 |
| A2 | 50,000 | 0 | 100.000% | `EXACT_MAX_FE` | 90 |
| A3 | 50,000 | 0 | 100.000% | `EXACT_MAX_FE` | 117 |
| A4 | 48,269 | 1,731 | 96.538% | `PHASE_CONSISTENT_TAIL_STOP` | 304 |

全部五臂：

```text
status=COMPLETED
illegalSolutions=0
duplicateEvaluations=0
sharedInitialPopulationHash=9fb09e602019393d54f8083448839b9193a78afef2f122cb8d89f4511afc4466
max(actualFE)-min(actualFE)=1731 < qPhaseFE=5000
groupStatus=VALID
```

因此 A4 的尾段不是漏计或超预算：它为 `1731`，严格小于完整 Q phase 的 `5000`。50k
诊断预算本身不要求超过 99% 利用率；该下限只适用于正式 500k（尾段最多 4999）。

## 机制闭合

| 机制事件 | A0 | A1 | A2 | A3 | A4 |
|---|---:|---:|---:|---:|---:|
| Qg selections | 400 | 400 | 400 | 400 | 1,200 |
| PDDR events | 2 | 2 | 2 | 2 | 6 |
| DSCR teacher uses | 0 | 400 | 400 | 400 | 1,200 |
| CFVF offspring | 0 | 0 | 10,000 | 10,000 | 30,000 |
| Qp actions | 0 | 0 | 0 | 5,100 | 25,100 |
| archive insertions | 0 | 0 | 0 | 200 | 600 |
| CA-TA Test / Apply | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 | 1,040 / 373 |
| formal local FE | 39,900 | 39,900 | 39,900 | 39,900 | 16,756 |
| CFVF repairs | 0 | 0 | 0 | 0 | 0 |

关闭机制（方向教师池、shadow、Shift）均为零；每臂的外部 hard gate 均写为 `PASS`。

## 证据文件

- 每臂原子目录：`runs/A0_BASELINE` 至 `runs/A4_BUDGET_AWARE_CATA`；每个目录含
  `status.properties`、`configuration.txt`、`budget-termination.properties`、前沿、
  初群 hash、摘要和 SHA-256 清单。
- 五臂交叉审计：`group-budget-audit/budget-utilization.csv`、
  `group-budget-audit/group-budget-audit.properties` 与该目录的 `evidence-sha256.tsv`。
- 分类器测试：`tools/V35ProductionPreflightBudgetTest.java`。
- Master adapter 及组审计测试：`tools/v35_phase_budget_master_adapter.py`、
  `tools/test_v35_phase_budget_master_adapter.py`。

## 仍需关闭的门

冻结副本内的全量回归会重写历史证据目录；为保护已冻结证据，已停止该副本中由本轮启动的
长程回归，不把它误报为通过。随后在字节保真的隔离副本中完成了 `jmetal-problem` 67 项全绿，
以及 32 项与本协议直接相关的 V35 定向测试全绿。完整 205 项历史套件不适合作为可迁移 Gate3
门：其中 `V35Fc0/V35Fc2/V35P10.1` 将绝对 `projectRoot` 与历史输出正文一并哈希，在隔离
目录必然失败，即使源码、jar、输入与搜索行为未变。因此它们保留为历史工作树内的证据契约，
不替代本轮可移植的生产定向回归。

`jmetal-core` 的 3 个失败仍是 P1 登记的 `DefaultIntegerPermutationSolution` 缺省实例路径错误，
不在正式生产调用链，且冻结 jar 哈希保持不变。详见
`../04-regression/REGRESSION_SCOPE.md`。

隔离回归、Java 8 目标核验和冻结证据清单复核均已完成；训练机的 4/8/12/16 JVM 吞吐基准
可以开始部署。任何吞吐运行或后续正式五臂组若不满足本协议，立即 fail-closed。
