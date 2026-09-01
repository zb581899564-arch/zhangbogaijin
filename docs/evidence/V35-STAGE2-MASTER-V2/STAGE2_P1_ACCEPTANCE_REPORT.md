# V35 Stage2-P1 A0--A4 正式启动链验收报告

日期：2026-08-23

## 验收结论

Stage2-P1 外部正式执行链已经闭合。冻结算法 jar 未修改，A0--A4 可通过公开 API、正式
snapshot 和独立 JVM 执行；Master v2 能验证 4500 条 RunKey、900 个五臂公平组，并以每 wave
15 个 JVM 调度。正式矩阵只允许在每个 wave 完成五臂组审计后继续。

## 冻结边界

```text
frozenJarSha256=8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
schema=v35-final-a0-a4-master-v2
budgetProtocol=v35-phase-consistent-budget-v1
population=100
MaxFEs=500000
qPhaseFE=5000
```

外置工具只做输入绑定、调用、证据落盘、预算和组级审计；不改变算法决策、随机流、FE、
前沿、Q 表或局部搜索顺序。正式 Runner 的源码中不调用 `problem.createSolution()`，运行时初群
完全来自冻结 snapshot。

## 本地验收

1. 外置 Java 源码使用 `javac --release 8` 编译，主类字节码 major version 为 52。
2. A0--A4 各 2000 FE 贯通，前沿非空、目标有限、来源闭合。
3. A0--A4 的 50000 FE Gate3 通过：

| Arm | actual FE | remaining FE | 结果 |
|---|---:|---:|---|
| A0 | 50000 | 0 | PASS |
| A1 | 50000 | 0 | PASS |
| A2 | 50000 | 0 | PASS |
| A3 | 50000 | 0 | PASS |
| A4 | 48269 | 1731 | 合法 phase-consistent 尾停 |

50k 只用于启动链重验；正式 500k 的利用率门仍严格为 `>0.99`。

## 远端非正式 500k 五臂验收组

```text
instance=20_2_3_1
seed=20260828
launcher_acceptance_only=true
included_in_formal_statistics=false
included_in_reference_front=false
```

| Arm | actual FE | remaining FE | utilization | 结果 |
|---|---:|---:|---:|---|
| A0 | 500000 | 0 | 100.0000% | PASS |
| A1 | 500000 | 0 | 100.0000% | PASS |
| A2 | 499724 | 276 | 99.9448% | PASS |
| A3 | 500000 | 0 | 100.0000% | PASS |
| A4 | 500000 | 0 | 100.0000% | PASS |

五臂实际 FE 范围为 `276 < 5000`。五臂共享同一实例、seed、snapshot、V35/P8 初群逻辑
哈希和 Problem provenance；非法解、重复评价、异常 repair、来源丢失均为 0。该组不进入
正式 900 个 snapshot、PFref 或论文统计。

## Master v2 验收

- 正式 manifest：4500 个唯一物理 RunKey。
- 公平组：900 个，每组恰好包含 A0、A1、A2、A3、A4 各一次。
- 设计：45 instances × 20 seeds × 5 arms。
- 正式 manifest SHA-256：
  `AB93D4AC6E8A5B71470D70EB7DB762F4563C7938DB32F8DD9C0B48985084B84E`。
- dry-run：通过，启动进程数为 0。
- 调度：3 fairness groups × 5 arms = 15 JVM/wave，保留 1 个已验收并发槽。
- 下一 wave 仅在当前三个公平组全部通过预算、provenance、roster 和 evidence 反向审计后启动。
- 已完成结果在恢复时先完整重验，再允许跳过；失败 attempt 永不覆盖。

## 状态边界

本报告验收的是启动链，不是论文结果。正式矩阵运行完成前不构造最终 PFref，不计算最终
HV/IGD/显著性，不改变算法参数，也不启动新机制研究。

## 正式启动记录

2026-08-23 20:11（Asia/Shanghai），正式 Master 已在训练机 tmux 会话
`zhangbo-v35-stage2-formal` 后台启动。首个 wave 的三个公平组、15 条运行已全部完成五臂预算、
provenance、roster、证据清单和归档审计：

| instance | seed | 五臂 actual FE | FE range |
|---|---:|---|---:|
| 100_2_3_1 | 20260808 | 500000/500000/500000/500000/500000 | 0 |
| 100_2_3_1 | 20260809 | 500000/500000/500000/495301/500000 | 4699 |
| 100_2_3_1 | 20260810 | 500000/500000/500000/495590/500000 | 4410 |

三组均严格满足 `range < 5000`，15 份大型事件日志均已生成并复核不可变归档。Master 状态为
`lastAcceptedWave=0, acceptedGroups=3`，随后才自动启动第二个15-JVM wave。因此
`formal_matrix_started=true`，但正式矩阵尚未完成，最终 PFref 与论文统计仍未开始。
