# V35-FC5-MIDHORIZON-DIAGNOSTICS-V3.1 terminal checkpoint contract

状态：A 角色协议与测试草案；最终收口与真实运行结果见同目录的 `TERMINAL_CHECKPOINT_CONTRACT.md`。

## 1. 本轮范围

本协议只服务于最后一组独立 JVM 验证：

```text
arm=A4
population=100
requestedMaxFE=50000
instance=100_5_3_1
seed=20260901
telemetry=OFF/ON
```

单元测试使用内存中的合成状态和极小的 `PermutationSolution`，不是运行证据，不加载算例、不调用
`V35MidHorizonDiagnosticDriver.main()`，不创建 evidence run 目录，也不启动任何实验进程。

本轮明确不启动 2k、20k、A2、250k、正式矩阵或上传，不修改 formal frozen Jar。任何已有的其他规模、
其他 arm 或历史结果都不属于本契约的通过条件。

## 2. 终止状态机

```text
RUNNING
  -> TERMINAL_CANDIDATE
       只有算法在真实状态下完成一个完整原子边界后，才读取一次终止快照
  -> ACCEPTED
       所有门通过，才能发布终止快照/验收状态
  -> REJECTED
       任一门失败；不得降级为部分快照、合成前沿或“近似通过”
```

`TERMINAL_CANDIDATE` 必须同时携带：`actualFE`、`lastCompletedAtomicBoundaryFE`、终止类型、
三个分别命名的真实前沿，以及观察器错误计数。终止快照只能在同一真实算法状态、同一原子终止点
一次性采集 `workingPopulationND`、`decisionArchiveFront`、`observedFullFront`；观察不得修改 FE、
随机数、Q/PDDR、档案或任何决策状态。重复回调不得替换第一次已采集的快照。

## 3. 硬门与分类

定义：

```text
remaining = requestedMaxFE - actualFE
qPhaseFE = 5000
terminationKind = PHASE_CONSISTENT_BUDGET_TERMINATION
allowTerminalPartialFormalQPhase = false
```

`ACCEPTED` 当且仅当：

1. `0 < actualFE` 且 `actualFE <= requestedMaxFE`；
2. `0 <= remaining < qPhaseFE`；
3. `actualFE == lastCompletedAtomicBoundaryFE`；
4. `terminationKind` 完全等于 `PHASE_CONSISTENT_BUDGET_TERMINATION`；
5. `allowTerminalPartialFormalQPhase == false`；
6. 三个前沿分别存在、非空、所有目标有限，并且三个字段保持可区分；
7. `observerErrors == 0`；
8. 回调边界是 `V35CheckpointFrontObserver.ATOMIC_BOUNDARY`。

本轮允许的预算分类夹具只有两种合法数值：

| 情形 | `actualFE` | `remaining` | 结果 |
|---|---:|---:|---|
| exact budget | 50000 | 0 | `ACCEPTED` |
| 已知 A4 尾停 | 48269 | 1731 | `ACCEPTED` |

`remaining == 5000` 或更大、`actualFE > requestedMaxFE`、`actualFE != lastCompletedAtomicBoundaryFE`、
partial enabled、终止类型错误、边界错误、任一前沿缺失/为空/非有限/不可区分、观察器异常，均为
`REJECTED`。拒绝必须 fail-closed：不发布任何看似完整的三前沿终止证据。

## 4. B 的最小生产 API 对接约定

为保持改动小、避免 A 触碰生产源码，B 只需在同一 package 增加或等价暴露以下纯函数；可以是
`public`，也可以是同 package 可访问：

```java
public final class V35TerminalCheckpointContract {
  public enum Classification { ACCEPTED, REJECTED }

  public static Classification classify(
      long requestedMaxFE,
      long actualFE,
      long lastCompletedAtomicBoundaryFE,
      long qPhaseFE,
      boolean allowTerminalPartialFormalQPhase,
      String terminationKind,
      String checkpointBoundary,
      boolean workingPopulationNDComplete,
      boolean decisionArchiveFrontComplete,
      boolean observedFullFrontComplete,
      long observerErrors) {
    // pure, deterministic, no FE/RNG/Q/PDDR/decision side effects
  }
}
```

这里三个 `*Complete` 参数不是运行器自行填写的乐观标志；它们必须由同一原子终止快照的实际采集
结果计算，语义分别是“对应字段非空、有限、可序列化且保留自己的 frontType”。分类器不得构造
或修复前沿，也不得把一个字段复制给另外两个字段。

`classify` 必须返回 `Classification.REJECTED` 而不是吞掉非法状态后返回 `null`。异常也不得被解释
为通过；若 B 选择抛出异常而不是返回拒绝，A 测试会按失败处理，主 Agent 需要先完成接口一致化。

## 5. 现有观察器/遥测器测试要求

`V35Fc5MidHorizonDiagnosticsV31ContractTest` 还直接锁定以下行为：

- exact `50000` 和已知 `48269` 只能在 `ATOMIC_BOUNDARY` 下各采集一次三前沿；
- 缺失或空字段不能让其他两个字段以 `NONE` 伪装成完整终止快照；
- 非有限目标不能以 `NONE` 发布；
- 任一观察器异常必须计数，且异常后只能保留明确的 unavailable/failed-closed 状态；
- OFF 遥测在读取前沿参数之前短路，不构造证据、不消费随机数；
- `allowTerminalPartialFormalQPhase` 在遥测和终止分类两侧均保持关闭。

已有 `V35MidHorizonObserverRealTest` 中允许两个前沿为空的 V2 断言与本 V3.1 终止门不同，不能单独
替代本测试；若 B 采用事务式三前沿提交，主 Agent 需要在整合时同步处理该旧测试的语义冲突。

## 6. 验收边界

本文件和单元测试只证明协议分类及观察器 fail-closed 行为。它们不证明 A4 独立 JVM 已运行，
也不证明 ON/OFF 的真实算法行为等价。真实验证仍必须由主 Agent 按本轮唯一允许的
`A4/population=100/MaxFEs=50000/100_5_3_1/20260901` 组合，在新鲜独立 JVM 中另行执行并核对：

```text
actualFE <= requestedMaxFE
remaining = requestedMaxFE - actualFE
0 <= remaining < qPhaseFE
actualFE == lastCompletedAtomicBoundaryFE
terminationKind = PHASE_CONSISTENT_BUDGET_TERMINATION
allowTerminalPartialFormalQPhase = false
```

没有真实运行路径、真实终止快照和完整三前沿，不能把本测试的 `ACCEPTED` 夹具写入任何运行报告。
