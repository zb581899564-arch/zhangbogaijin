# V35 Stage2 Gate3：冻结 A4 精确 FE 复验（历史隔离）

> 证据状态：`legacy_pre_phase_budget_protocol`。本报告准确保留此前严格
> `requestedFE=actualFE` 政策下的阻断事实；2026-08-23 用户已批准
> `v35-phase-consistent-budget-v1`，当前 Gate3 以
> `PHASE_CONSISTENT_BUDGET_TERMINATION` 重验为准，详见
> `docs/evidence/V35-PHASE-BUDGET-PROTOCOL/PHASE_CONSISTENT_BUDGET_TERMINATION_PROTOCOL.md`。

## 裁决

```text
Gate3 = BLOCKED
PREFLIGHT_ACCEPTED = false
REMOTE_THROUGHPUT_BENCHMARK_STARTED = false
FORMAL_MAX_PARALLEL = NOT_RECOMMENDED_UNTIL_GATE_POLICY_RESOLVED
```

这是一项非正式诊断复验，不构成 A0--A4 消融结论、算法性能结论或论文统计证据。

## 绑定的冻结物

```text
freezeTag = v35-final-doe1-frozen
freezeMetadataCommit = 2b3316b21512ff9d1d7f3db972f016ba02edac6e
freezeJarSha256 = 8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9
freezeJar = E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-freeze-20260823\java-jmetal58\jmetal-exec\target\jmetal-exec-5.8-jar-with-dependencies.jar
freezeBinding = FINAL_FREEZE_BOUND
```

## 合规 Gate3 复验（预注册 50k 与允许的 100k 扩展）

此前的 20k 工具链诊断只能说明“该冻结语义可能安全尾停”，不能替代 Track C
预注册的 `50000 FE` 生产预检。该 50k 复验及协议允许的 100k 扩展均使用同一冻结 jar、
同一非正式诊断输入；它们共同构成 Gate3 当前有效的预算裁决证据。

```text
arm = A4
instance = 20_2_3_1
seed = 20260828 (不属于正式 20260808..20260827 roster)
population = 100
requestedFE = 50000
actualFE = decoderCalls = 48269
status = COMPLETED
illegal / duplicate / nonfinite / repair = 0 / 0 / 0 / 0
frontSize = 304
```

所有 A4 必需机制在该运行中仍真实触发；Shift、方向教师池和 shadow 均为零。唯一
失败仍为严格预算等式：

```text
requestedFE = actualFE = decoderCalls
50000 != 48269
```

该运行的原子证据目录为
`final-freeze-A4-50k-20260828/`，其中包含 `preflight-gate.properties`、机制摘要、
前沿、初群哈希和 SHA-256 清单。因而 Gate3 的 `BLOCKED` 状态不是由非合规的 20k
诊断推断而来，而是由预注册 50k 复验直接证明。

100k 扩展复验也未通过严格等式：

```text
arm = A4
requestedFE = 100000
actualFE = decoderCalls = 96025
status = COMPLETED
illegal / duplicate / nonfinite / repair = 0 / 0 / 0 / 0
frontSize = 276
```

该扩展运行的 A4 机制同样真实触发，且其独立原子证据目录为
`final-freeze-A4-100k-20260828/`。因此在 Track C 允许的 50k/100k 诊断预算内，
不存在已证实可令 A4 精确闭合的候选；不能据此臆测一个对 A0--A4、45 实例和 20 seed
都严格闭合的共同正式预算。

## 历史 20k 工具链诊断（保留，不作 Gate3 裁决）

```text
arm = A4
instance = 20_2_3_1
seed = 20260828 (不属于正式 20260808..20260827 roster)
population = 100
requestedFE = 20000
decoder = FM3
family = DEGENERATE_SINGLE_FAMILY
setup = SEQUENCE_INDEPENDENT
shift = NONE
PDDR = GLOBAL_ORIGINAL
local-search order = CA-TA-Lite -> inherited LS
teacher pool = false
```

## 实测

| 项目 | 值 |
|---|---:|
| Algorithm status | `COMPLETED` |
| Requested FE | 20,000 |
| Actual FE | 15,258 |
| Decoder calls | 15,258 |
| Illegal solutions | 0 |
| Duplicate evaluations | 0 |
| Non-finite objectives | 0 |
| CFVF repairs | 0 |
| Wall time | 3.291 s |
| Algorithm time | 3.199 s |
| Front size | 138 |

启用机制均真实触发：Qg 400、PDDR 2、DSCR teacher-use 400、CFVF 10,000、Qp 8,100、
archive insertions 200、P5/G5 41/40、CA-TA Test/Apply 391/114、inherited local FE 4,653。
禁用机制未泄漏：directional teacher pool、shadow 和所有 shift 计数均为 0。

它同样不满足精确预算门：

```text
requestedFE = actualFE
20000 != 15258
```

该运行目录的 `evidence-sha256.tsv` 已复核：19 项、0 项不匹配。

## 可重复根因

冻结语义保留 `Q_Times=50` 和 population 100，因此一整个 outer Q phase 固定需要
5,000 次全局评价。A4 同时采用 `allowTerminalPartialFormalQPhase=false` 与 shared dynamic
local-FE window：在初始 100 FE、两个完整 Q phase（10,000 FE）及本轮 CA-TA/inherited LS
消耗 5,158 FE 后，剩余 4,742 FE 小于完整 Q phase 所需的 5,000 FE，因而安全停止。

改成 terminal partial phase 可以追近请求预算，但会改变被冻结的 Q/LS 时序，且违背本阶段
“不得修改算法、参数、局部预算、Q次数或搜索逻辑”的边界。故不得以修补方式让该门通过。

## 后续限制

按 Stage2 Track C 的“任一硬门失败立即停止”规则：

1. 不运行 A0--A3 的 50k 生产预检；
2. 不在训练机启动 4/8/12/16 JVM 远端吞吐 benchmark；
3. 不启动任何正式 500k 运行或 4,500 格矩阵；
4. 不给出 `FORMAL_MAX_PARALLEL` 数值。

在保持 strict exact-FE 政策的前提下，当前冻结语义没有可用的共同预算方案。若仍要求
`requestedFE=actualFE=decoderCalls`，必须另行批准新的算法/停止语义并重新冻结、重新验收；
不能通过选择未验证的预算数字、改局部预算或开启 partial Q phase 规避本门。未完成该治理
裁决前，Gate3 保持 `BLOCKED`。
