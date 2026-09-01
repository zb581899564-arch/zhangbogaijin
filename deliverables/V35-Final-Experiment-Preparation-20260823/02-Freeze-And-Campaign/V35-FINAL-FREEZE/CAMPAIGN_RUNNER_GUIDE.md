# V35 Campaign Runner 使用与证据纪律

## 角色

`scripts/v35_campaign_runner.py` 是一个**调度与证据保存器**：它只执行经审查 manifest 中已经固定的命令，不生成算法参数、实例、seed 或机制开关。

RunKey 的恒等身份为：

```text
algorithm + configHash + instance + seed + budget
```

若短基准需要重复相同身份，必须显式提供独立 `isolationId`，不能静默覆盖前次运行。

## 状态与恢复

状态为 `PENDING`、`RUNNING`、`COMPLETED`、`FAILED` 或 `INVALID`。调度器：

- 原子写入 campaign state；
- 已完成 RunKey 只读跳过；
- 失败尝试保留在独立目录，可显式重试；
- 拒绝重复 RunKey 与 manifest 修改后的 resume；
- 以 `maxParallel` 启动独立进程/JVM；
- 默认拒绝 `safetyClass=formal`，除非命令明确传入 `--allow-formal`。

上述 `--allow-formal` 只是调度层的显式开关，不是科研授权。正式运行还必须由 `ZhangBoV35FormalComparisonRunner` 对 FC-8、EXP-1、实例/seed/初群、hash 与运行时公平契约重新验证。

## 已验证行为

`scripts/tests/test_v35_campaign_runner.py` 当前 4 项测试覆盖：重复 RunKey 拒绝、成功后跳过、失败保留并重试、formal 默认拒绝。

## 正式使用前的硬规则

1. 只使用新的、已批准的 final manifest；不得复用本目录中的 DOE-1 并发短预检 manifest。
2. 每个 arm 采用独立 JVM、Problem、算法对象与输出目录。
3. 每次 physical run 保存 canonical configuration、输入 provenance、初群哈希、真实 FE、停止原因、最终 raw front 与证据哈希。
4. 不由 scheduler 构造 `PFref`、HV、IGD 或统计；这些只能由 `tools/v35-analysis/` 在全体 raw fronts 齐备后一次性构造。
5. 任何失败记录永久保留；不允许覆盖或把重跑混作同一 physical run。
