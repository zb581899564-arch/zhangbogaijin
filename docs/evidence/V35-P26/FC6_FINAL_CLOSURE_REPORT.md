# V35-FC-6 收口总报告：候选池、局部搜索顺序与区域化生存选择

> 状态：`completed_rejected`（2026-08-20）  
> 主线结论：保持 `GLOBAL_ORIGINAL + CA-TA-Lite → inherited LS`。  
> 本文用途：汇总 FC-6 的预注册问题、实现、审计、500000 FE 结果、证据边界和后续冻结规则。它是工程验收记录，不是统计显著性报告，也不能作为论文优越性结论。

---

## 1. 一页结论

FC-5 的 Cmax 生命周期审计将短板定位在 **Survival（生存/环境选择）**，因此 FC-6 被严格限定为只测试一条 Survival 修复分支；不得同时增加教师门控、压力规则、Shift 或新搜索算子。FC-6 包含两个逐步问题：

1. **FC-6A.4：局部搜索顺序。** 在不改变候选、预算、PDDR 规则或教师规则的前提下，仅交换 `CA-TA-Lite` 与 inherited inter-factory/O1–O9 的调用顺序。
2. **FC-6B：区域化环境选择。** 只在 FC-6A.4 决出的顺序下，将原始全局 PDDR 与固定容量的 `REGION_AWARE` PDDR 对比。

两个问题都已完成 500000 FE 配对运行并被拒绝：

| 问题 | 数据 | 关键结果 | 预注册裁决 | 最终处理 |
|---|---|---|---|---|
| 局部搜索顺序 | `20_2_3_1` × 3 seed | 反序的中位最小 Cmax 改善 `6.8705%`，但中位 IGD 恶化 `11.6789%` | IGD 不得恶化超过 `10%` | **不转正**；保持当前顺序 |
| 区域化 PDDR | `20_2_3_1`、`100_2_3_1`，各 3 seed | 20-job：HV `-3.9689%`、IGD `+67.8729%`；100-job：HV `-22.7133%`、IGD `+371.7009%` | Cmax、HV、IGD 门及单 seed 灾难门 | **STOP_REVIEW**；不冻结 `REGION_AWARE` |

因此当前可继续使用的主线不是“区域化版本”，而是：

```text
FM3 + 单一退化产品族 + 序列无关 SUT + ShiftMode=NONE
+ A4-Pacing + GLOBAL_ORIGINAL PDDR
+ CA-TA-Lite → inherited inter-factory/O1–O9
```

`BP_RESERVED_LEGACY`、`ORDER_SWAP` 和 `REGION_AWARE` 仅保留为可追溯诊断分支，不得进入后续主版本、参考前沿或正式矩阵。

---

## 2. 问题来源、范围与不可变边界

### 2.1 FC-6 要回答的因果问题

当前正式循环的局部搜索原本是：

```text
全局 Q 阶段最终后代
→ CA-TA-Lite
→ inherited inter-factory exchange / insertion / O1–O9
→ PDDR
```

两段局部搜索共享同一 `localFeHardLimit()`。因此，先执行的一方可能占用局部 FE，使另一方较少或没有机会产生候选；这会影响 Cmax，却不等价于某个算子的固有质量更好。FC-6A.4 仅改变该执行顺序，目的是分离这个预算竞争效应。

同时，原始 PDDR 是全局候选池的选择器。FC-6B 检验：若按四个物理子群的既定容量保存候选，是否能改善 G1、G4、G2、G3 的生存多样性与 Cmax。该检验不改变 CFVF、Qg/Qp、DSCR、档案、疲劳解码或任何局部算子。

### 2.2 本轮共同冻结条件

除正在比较的单个字段外，所有运行共享下列条件：

| 字段 | 冻结值 |
|---|---|
| 实例 | `20_2_3_1`；FC-6B 另含 `100_2_3_1` |
| 种群 | `100`；物理槽位顺序固定为 `G1/G4/G2/G3` |
| 评价预算 | 每条独立 JVM 运行精确 `500000 FE` |
| 疲劳 | `FM3`（动态累积、自然恢复、工时反馈、疲劳感知选工） |
| 设置时间 | 实例既有 `SUT[job][stage]`，`SEQUENCE_INDEPENDENT` |
| 产品族 | `DEGENERATE_SINGLE_FAMILY` |
| 时间移动 | `ShiftMode=NONE` |
| 正式目标 | 七槽载体中的 `[0,1,6] = [Cmax, TEC, TWC]` |
| 公平性 | 同 seed 两臂显式生成相同初始四向量种群；哈希不一致即拒绝 |
| 参考集 | 每个对比组的全部完成运行结束后一次性合并前沿并严格 Pareto 过滤；不得按单臂自建参考集 |

这些限制的含义是：本报告只评价 **调用顺序** 和 **环境选择方式**。它不能证明疲劳、CFVF、双 Q 或 CA-TA-Lite 的论文级总体优越性。

---

## 3. 实现收口：从隐式行为到可审计配置

### 3.1 显式 PDDR 选择模式

新增 `PddrSelectionMode`，由 V35 Runner 写入 canonical configuration text、配置哈希、报告与来源证据：

| 模式 | 行为 | FC-6 可用性 |
|---|---|---|
| `GLOBAL_ORIGINAL` | 只按原始 `(score, originalOrder)` 稳定排序取前 100 | FC-6A.4 与 FC-6B 对照臂 |
| `BP_RESERVED_LEGACY` | 旧 BP 预留逻辑，只读历史兼容 | **拒绝进入 FC-6** |
| `REGION_AWARE` | 严格 PDDR score 后，按区域容量分配；未填满才回流全局原始排序 | 仅 FC-6B 实验臂 |

`REGION_AWARE` 的物理容量不是临时“偏好”，而是冻结的实验变量：

```text
G1_CMAX=15, G4_BALANCED=55, G2_TEC=15, G3_TWC=15
```

区域内的稳定排序固定为：

| 区域 | 名额 | 区域排序 |
|---|---:|---|
| `G1_CMAX` | 15 | `(Cmax, TEC, TWC, poolOrder)` |
| `G4_BALANCED` | 55 | `(PDDR score, poolOrder)` |
| `G2_TEC` | 15 | `(TEC, Cmax, TWC, poolOrder)` |
| `G3_TWC` | 15 | `(TWC, Cmax, TEC, poolOrder)` |

运行时也会同步重建四个物理槽位；因此“PDDR 选出了 15/55/15/15，但后续速度更新仍按 20/40/20/20”这一早期不一致已被容量硬门阻断并修复。

### 3.2 候选池与局部候选账本

FC-6A.3 新增纯观察账本，不改变 PDDR 排名、随机流、FE、Q 表或候选本身。每轮可区分：

```text
PARENT
GLOBAL_Q_FINAL
CATA_TEST / CATA_APPLY
CRITICAL_SWAP / CRITICAL_INSERT / O1–O9
```

各类别都会记录：已评价数、被接受数、被后续局部动作覆盖数、进入 PDDR 数、PDDR 选中/拒绝数及 FE。只有作为最终承载解保留的局部候选进入 merge pool；被拒绝或被后续局部动作覆盖的候选明确标为“不进入 PDDR”。`PendingCaTaLocalCandidate` 也增加了不可变局部来源字段。

审计打开/关闭已通过行为等价测试：前沿、FE、Q 表、随机事件与动作哈希一致。它只解释发生了什么，不参与选择。

### 3.3 局部搜索顺序开关

新增 `V35LocalSearchOrder`：

```text
CURRENT:    CA-TA-Lite → inherited LS
ORDER_SWAP: inherited LS → CA-TA-Lite
```

这不是新搜索机制；两个臂使用同一组 CA-TA-Lite、inter-factory 与 O1–O9 算子，唯一差异是共享 local-FE 配额的先后顺序。

### 3.4 旁路教师记录

Region-aware 实验只旁路记录 `crossRegionTeacherCount`、请求者角色、教师区域、教师目标及曝光次数；不新增教师门控。Qg/Qp 规则保持不变，只允许候选组成变化自然改变被选择的教师。

---

## 4. FC-6A.3：实现一致性与候选池审计验收

### 4.1 已验证的实现契约

| 契约 | 验收结果 |
|---|---|
| V35 Runner 必须显式声明 PDDR 模式 | 通过；缺失模式或 `BP_RESERVED_LEGACY` 均 fail-closed |
| `GLOBAL_ORIGINAL` 仅用原始稳定排序 | 通过单元测试 |
| `REGION_AWARE` 的区域容量、回流和语义映射 | 通过单元/20k 贯通测试 |
| 局部候选的来源、覆盖及是否进入 PDDR | 已进入逐轮账本 |
| 审计 ON/OFF 不改变算法行为 | 通过重放等价测试 |
| 预评价候选不重复计 FE | 通过预算闭合测试 |

定向构建证据：

```text
ZhangBoV35Fc6RunnerTest                  5/5 通过
V35Fc6PddrSelectionModeTest              2/2 通过
V35 定向算法回归                         38/38 通过
```

其中 CA-TA 贯通烟测还验证 `fullFE=2000`、`caTaFE=1300`、`testCalls=936`、`applyCalls=364`、`events=1892`。这些数字是短程机制检查，不是本报告的性能结论。

完整实现映射与调用链见：[FC-6A.3 实现审计](FC6A3-implementation-audit/IMPLEMENTATION_AUDIT.md)。

---

## 5. FC-6A.4：局部搜索顺序单变量结果

### 5.1 预注册裁决规则

`ORDER_SWAP` 只有同时满足下列条件才可替换当前顺序：

1. 中位最小 Cmax 至少改善 `2%`；
2. 中位 HV 不低于当前臂 `2%`；
3. 中位 IGD 不恶化超过 `10%`；
4. 不存在任一 seed 同时出现 `HV < -5%` 且 `IGD > +20%` 的灾难组合。

本组使用 `20_2_3_1`、seed `20260822/20260823/20260824`，每 seed 的两臂独立 JVM 且初始种群哈希相同。六条运行全部 `COMPLETED`，均精确 `500000 FE`。

### 5.2 逐 seed 指标

| Seed | 顺序 | 前沿数 | HV | IGD | 最小 Cmax | 最小 TEC | 最小 TWC | 算法耗时（s） |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| 20260822 | CURRENT | 540 | 0.868230 | 0.042286 | 192.944793 | 8295.559812 | 12695.917532 | 352.615 |
| 20260822 | ORDER_SWAP | 603 | 0.869302 | 0.047225 | 170.932081 | 8308.596738 | 12570.698275 | 344.419 |
| 20260823 | CURRENT | 643 | 0.913044 | 0.032712 | 181.463681 | 8318.415992 | 12539.317170 | 284.365 |
| 20260823 | ORDER_SWAP | 700 | 0.905120 | 0.031672 | 168.996147 | 8386.831285 | 12566.167238 | 311.843 |
| 20260824 | CURRENT | 752 | 0.932378 | 0.019055 | 177.017999 | 8356.178041 | 12447.536542 | 328.559 |
| 20260824 | ORDER_SWAP | 596 | 0.905917 | 0.035589 | 192.407138 | 8338.070338 | 12343.485362 | 286.019 |

### 5.3 局部 FE 与候选生存账本摘要

| Seed | 顺序 | `FE_CA-TA` | `FE_inherited` | CA-TA 最佳生成 Cmax | inherited 最佳生成 Cmax | CA-TA 进入 PDDR | inherited 进入 PDDR | CA-TA 被选中 | inherited 被选中 |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 20260822 | CURRENT | 11607 | 178293 | 208.8375 | 183.9581 | 2683 | 770 | 229 | 90 |
| 20260822 | ORDER_SWAP | 0 | 189900 | — | 170.9321 | 0 | 747 | 0 | 110 |
| 20260823 | CURRENT | 15084 | 174816 | 181.5556 | 175.0817 | 2827 | 715 | 232 | 69 |
| 20260823 | ORDER_SWAP | 0 | 189900 | — | 168.9961 | 0 | 747 | 0 | 110 |
| 20260824 | CURRENT | 12426 | 177474 | 181.4756 | 174.9033 | 2476 | 647 | 144 | 75 |
| 20260824 | ORDER_SWAP | 0 | 189900 | — | 186.4140 | 0 | 803 | 0 | 143 |

破折号表示该顺序下 CA-TA-Lite 没有获得可用于完整评价的共享局部 FE；这正是本单变量实验要显式揭示的预算顺序效应，而不是缺失日志。

### 5.4 裁决与解释边界

`ORDER_SWAP` 的中位最小 Cmax 改善为 **`6.870539%`**，中位 HV 变化为 **`-0.867938%`**，但中位 IGD 变化为 **`+11.678867%`**，越过了预注册的 `+10%` IGD 上限。没有出现灾难 seed 组合，但这不能抵消 IGD 门失败。

结论：**保留 `CA-TA-Lite → inherited LS`。** 这不表示反序必然无价值，也不意味着 Cmax 不重要；它只表示在当前三目标、统一参考集和预先写定的转正规则下，不能用 Cmax 单项改善换取超过门槛的 IGD 退化。

完整报告、逐轮账本、前沿和冻结参考集见：[FC-6A.4 顺序裁决](FC6A4-local-search-order/ORDER_DECISION.md) 与 [A.4 结果目录](FC6A4-local-search-order/results/)。

---

## 6. FC-6B：区域化环境选择结果

### 6.1 预注册裁决规则

在 FC-6A.4 保留当前顺序后，FC-6B 只比较：

```text
GLOBAL_ORIGINAL  vs  REGION_AWARE(15/55/15/15)
```

`REGION_AWARE` 需要同时满足：中位最小 Cmax 改善至少 `2%`、HV 中位退化不超过 `2%`、IGD 中位退化不超过 `10%`，且不存在任一 seed `HV < -5%` 且 `IGD > +20%`。每个实例的全部六条运行完成后才冻结该实例的统一工程参考集。

本组共 12 条独立 JVM 运行（2 个实例 × 3 seed × 2 模式）。所有运行均：

```text
status=COMPLETED
fullEvaluations=500000
非法解=0
异常 repair=0
CFVF repair=0
```

六条 `REGION_AWARE` 运行的每个生存周期都经本地脚本复核为精确 `15/55/15/15`，不存在 BP 预留席位。

### 6.2 `20_2_3_1` 逐 seed 结果

| Seed | 模式 | 前沿数 | HV | IGD | Spacing | 最小 Cmax | 最小 TEC | 最小 TWC | 算法耗时（s） |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 20260822 | GLOBAL | 540 | 0.901924 | 0.048783 | 0.019708 | 192.944793 | 8295.559812 | 12695.917532 | 358.990 |
| 20260822 | REGION | 576 | 0.964425 | 0.032424 | 0.017499 | 170.085875 | 8390.801431 | 12536.970079 | 251.217 |
| 20260823 | GLOBAL | 643 | 0.952416 | 0.034921 | 0.015520 | 181.463681 | 8318.415992 | 12539.317170 | 292.564 |
| 20260823 | REGION | 359 | 0.908421 | 0.058622 | 0.017467 | 178.431375 | 8432.506036 | 12554.742740 | 218.873 |
| 20260824 | GLOBAL | 752 | 0.973046 | 0.021593 | 0.016482 | 177.017999 | 8356.178041 | 12447.536542 | 342.515 |
| 20260824 | REGION | 459 | 0.934427 | 0.044804 | 0.018118 | 177.934011 | 8436.389453 | 12507.521996 | 221.299 |

中位变化为：最小 Cmax **`+1.671026%`**、HV **`-3.968887%`**、IGD **`+67.872915%`**。虽然 seed `20260822` 的 REGION 结果较好，但其余两个 seed 的 HV/IGD 退化使中位门失败；该分支在 20-job 即不转正。

### 6.3 `100_2_3_1` 逐 seed 结果

| Seed | 模式 | 前沿数 | HV | IGD | Spacing | 最小 Cmax | 最小 TEC | 最小 TWC | 算法耗时（s） |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 20260822 | GLOBAL | 321 | 0.621027 | 0.129855 | 0.017396 | 709.349403 | 48577.240966 | 90331.307136 | 587.854 |
| 20260822 | REGION | 166 | 0.660679 | 0.263441 | 0.028976 | 713.800565 | 47823.769827 | 94031.400624 | 409.990 |
| 20260823 | GLOBAL | 223 | 0.797890 | 0.054302 | 0.021092 | 701.926945 | 48065.080207 | 85305.753410 | 447.007 |
| 20260823 | REGION | 104 | 0.549277 | 0.256141 | 0.042337 | 721.777232 | 49292.411473 | 98297.290734 | 381.047 |
| 20260824 | GLOBAL | 584 | 0.774132 | 0.038241 | 0.016287 | 723.388627 | 48466.886626 | 88619.995295 | 540.686 |
| 20260824 | REGION | 124 | 0.598301 | 0.310886 | 0.027594 | 734.776958 | 48179.952357 | 97392.950882 | 399.558 |

中位变化为：最小 Cmax **`-1.574303%`**（即更差）、HV **`-22.713251%`**、IGD **`+371.700855%`**；并触发一票否决的灾难 seed 条件。该实例明确拒绝 `REGION_AWARE`。

### 6.4 FC-6B 裁决

**STOP_REVIEW：不冻结 `REGION_AWARE`，维持 `GLOBAL_ORIGINAL`。**

这是对当前冻结的 `15/55/15/15` 生存选择在两种代表规模上的实证结论；它不推出“所有区域化生存选择理论上均无效”。但 FC-6 的单支纪律禁止在失败后自动加入教师门控、压力掩码、额外局部搜索或改变容量来挽救该结果。若未来要重新研究此问题，必须先完成独立的人工复核与新的预注册计划。

完整结果见：[FC-6B 结果报告](FC6B-region-aware/FC6B_RESULT.md)、[最终 r3 远端结果包](FC6B-region-aware/remote-results-r3.tar.gz) 与其解压结果目录 `[remote-results-r3/results](FC6B-region-aware/remote-results-r3/results/)`。

---

## 7. 远端执行与证据完整性

### 7.1 正式使用的数据

FC-6 使用 18 条 500000 FE 物理运行：

```text
FC-6A.4：1 实例 × 3 seed × 2 顺序 = 6 条
FC-6B：2 实例 × 3 seed × 2 PDDR 模式 = 12 条
```

FC-6B 的唯一正式远端批次是：

```text
/home/inspur/aicomp/zhangbo-fc6b-region-20260820-r3
```

归档 `remote-results-r3.tar.gz` 的 SHA-256 为：

```text
60d70be77d64fd6d9aa091ad0106f6c9cbdac567f864f2b47e90d25db86b98ae
```

每个 r3 报告目录内的 `evidence-sha256.tsv` 均已在本地逐文件复核。运行配置中还保存实例、SUT、疲劳参数、初始种群、应用包与机制向量的来源绑定；因此前沿不可脱离对应配置单独解释。

### 7.2 被隔离的远端失败

以下失败被保留为部署/集成诊断，**未**混入任一正式参考前沿、指标或裁决：

| 批次 | 原因 | 处置 |
|---|---|---|
| 初始远端提交 | PDDR 已选区域容量正确，但运行时槽位仍为历史 `20/40/20/20`，容量硬门失败 | 修正运行时容量绑定，重建并测试 |
| r1 | 旧包/部署竞争导致 `ClassNotFoundException: ZhangBoDecoderTimingSnapshot` | 保留失败日志，不采纳 |
| r2 | 上传清单遗漏 `100_2_3_1.txt`，20-job 完成后无法执行 100-job | 视为部署清单不完整；不混入结果 |
| r3 | 全部输入、应用 SHA 与 12 条运行齐全 | **唯一正式 FC-6B 证据** |

这类隔离是证据边界的一部分：部分完成批次不能用来补足、优化或选择正式结果。

---

## 8. 当前验收状态与禁止性结论

### 8.1 可以确认的工程事实

```text
FC-6A.3 候选池审计已实现且为纯观察
FC-6A.4 已完成并保留 CURRENT 顺序
FC-6B 已完成并拒绝 REGION_AWARE
GLOBAL_ORIGINAL PDDR 是当前唯一冻结主线
BP_RESERVED_LEGACY 未进入本轮实验
所有正式 FC-6 运行 FE 闭合、前沿非空且无非法解/异常 repair
```

### 8.2 本报告不能声称的内容

本报告不得被解释为下列任何结论：

- `REGION_AWARE` 在所有任务、容量或算法中永远无效；
- 当前主线对李明哲算法或经典算法具有统计显著优越性；
- 18 条运行构成正式多实例、多 seed 论文实验；
- 已启用 PF-SDST、多产品族、序列相关设置时间、主动休息、多技能、第五染色体或第四目标；
- Shift 策略参与本轮比较；
- 可以自动启动 P25 正式 20 次矩阵、FC-7/FC-8 或新机制开发。

### 8.3 冻结状态

```text
V35-FC-6 = completed_rejected
formal_matrix_started = false
sampled_reproduction_accepted = false
full_reproduction_accepted = false
pf_sdst_active_experiment = false
shift_formal_path_frozen = true
```

下一步不是自动加修复，而是先人工复核 FC-6 的拒绝结果：确认是否接受“保持原始全局 PDDR 与当前局部搜索顺序”的主线。只有用户另行批准，才可进入既定 FC-7 流程或制定新的、单变量且预注册的生存选择研究计划。

---

## 9. 导航与证据索引

| 内容 | 位置 |
|---|---|
| FC-6A.3 实现、调用链与短程回归 | [FC6A3-implementation-audit/IMPLEMENTATION_AUDIT.md](FC6A3-implementation-audit/IMPLEMENTATION_AUDIT.md) |
| FC-6A.4 裁决 | [FC6A4-local-search-order/ORDER_DECISION.md](FC6A4-local-search-order/ORDER_DECISION.md) |
| FC-6A.4 逐 seed 前沿、账本、报告 | [FC6A4-local-search-order/results](FC6A4-local-search-order/results/) |
| FC-6B 最终裁决 | [FC6B-region-aware/FC6B_RESULT.md](FC6B-region-aware/FC6B_RESULT.md) |
| FC-6B 部署失败隔离 | [FC6B-region-aware/FAILURE_CAPACITY_GATE.md](FC6B-region-aware/FAILURE_CAPACITY_GATE.md) |
| FC-6B r3 远端完整结果 | [FC6B-region-aware/remote-results-r3/results](FC6B-region-aware/remote-results-r3/results/) |
| FC-6B 清单 | [FC6B-region-aware/evidence-sha256.tsv](FC6B-region-aware/evidence-sha256.tsv) |
| 路线图决策 | [docs/ROADMAP.md](../../ROADMAP.md) 的 D-084、D-085 |
| 运行与冻结规则 | [AGENTS.md](../../../AGENTS.md) 的 FC-6 条款 |

---

## 10. 复核清单

如需第三方或答辩前复核，按以下顺序即可独立核验本报告：

1. 对照 D-084 的预注册条件和本报告第 5、6 节的裁决门；
2. 校验每条 `status.properties` 为 `COMPLETED` 且 `fullEvaluations=500000`；
3. 校验同 seed 配对的 `initial-population.sha256` 一致；
4. 校验每条运行目录的 `evidence-sha256.tsv`；
5. 读取逐轮 local candidate ledger，核对来源 FE、进入 PDDR 与选中数；
6. 对 REGION 结果逐轮检查 `15/55/15/15` 且 BP 预留为零；
7. 按各组全部完成运行重建严格 Pareto 参考集和统一归一化指标；
8. 重新计算中位变化并检查预注册门是否通过。

只有以上证据链仍闭合时，才能复述“FC-6 已被检验但未转正”这一工程结论。
