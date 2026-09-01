# 4500 条正式矩阵的启动就绪性复核（历史，已由 D-090 关闭）

> 状态：`SUPERSEDED_BY_STAGE2_MASTER_V2`。本文保留 D-089 时点证据。准确表述是旧
> renderer 已存在，但只支持双臂、串行和单一全局配置哈希；当时不存在的是满足 A0--A4
> 正式契约的 v2 launcher/renderer。D-090 已完成该外部执行链。

日期：2026-08-23  
结论：`BLOCKED_FORMAL_A0_A4_MASTER_LAUNCHER_GAP`  
`formal_matrix_started=false`

## 已接受的 Stage2 Gate

以下门已通过，且不会因为本结论被回滚：

| Gate | 状态 | 证据 |
|---|---|---|
| Final source / jar freeze | ACCEPTED | `v35-final-doe1-frozen`，jar `8dad...ad8b9` |
| 45×20 shared-start manifest | ACCEPTED | 45 实例、20 seed、900 snapshot |
| A0--A4 50k phase-bound Gate3 | ACCEPTED | `03-gate3-preflight/` 与 `05-remote-throughput/` |
| 4/8/12/16 JVM capacity | ACCEPTED | `FORMAL_MAX_PARALLEL=16` |
| A0--A4 final semantics | ACCEPTED | 已冻结机制 ladder |

## 阻断事实

正式矩阵的 roster 是 `A0,A1,A2,A3,A4`，即 4500 条物理运行。然而冻结 fat jar 中的
`ZhangBoV35FormalComparisonRunner`（版本 `v35-formal-comparison-gate-v2`）只接受：

```text
--arm HMOPSO_QGS_F | V35_MAIN
```

它可正确通过 `readSnapshot(...)` 读取冻结四向量起点，但并不支持 A1、A2、A3，也不能把
`A0--A4` 标签映射为五条正式机制阶梯。另一方面，当前外部
`V35ProductionPreflight` 能运行 A0--A4，却是诊断启动器：它会从 problem RNG 创建初始种群，
不读取 900 个正式 snapshot，故不能替代正式 launcher。

此外，Phase Budget adapter 已具备预算分类和五臂审计能力，但其计划中要求载入具有
`validate_manifest/run_key/output_fingerprint/run_campaign` 接口的冻结 Master renderer；当前
工作树中不存在该 renderer。通用 `scripts/v35_campaign_runner.py` 只负责进程调度和原子
attempt，不能自行渲染/验证 Master RunKey、snapshot 绑定或五臂组公平性。

因此，若现在直接开始 4500 条任务，将必然出现以下任一不允许的情况：

1. 只运行 A0/A4，违反已经批准的 A0--A4 消融矩阵；
2. 用诊断起点运行 A1--A3，违反 900 shared-start fairness contract；
3. 临时改写 frozen jar 或参数，违反本次方案 C 的冻结边界；
4. 绕过 Master RunKey / snapshot / phase-bound group audit。

任何一种都使正式数据不可用，故本阶段必须 fail-closed。

## 不做的事

- 不补评价、不启用 partial Q phase；
- 不重建、替换或修改 frozen fat jar；
- 不把 A0/A4 的两算法 run 误报为 A0--A4 消融矩阵；
- 不启动任何 500k formal raw run。

## 恢复启动所需的最小闭环

在新的、版本化的外部执行边界中提供并验证一个 **A0--A4 snapshot-bound formal arm launcher**
和一个 **Master renderer**。它们必须只调用冻结 jar 的公共 API，并逐条做到：

```text
readSnapshot(instance, seed)
→ verify V35/P8 initial hashes and provenance
→ bind frozen A0..A4 configuration / profile hash
→ execute one isolated JVM arm
→ write formal status, provenance and budget-termination evidence
→ atomically audit the complete five-arm group
```

该执行边界必须先以单个 `(instance,seed)` 的五臂 500k dry-run/acceptance 证明：所有实际 FE
满足 phase-bound，500k 利用率大于 99%，五臂 FE 范围小于 5000，且 snapshot/arm/provenance
无漂移。只有通过后才可重启“4500 条正式矩阵”这一授权步骤。
