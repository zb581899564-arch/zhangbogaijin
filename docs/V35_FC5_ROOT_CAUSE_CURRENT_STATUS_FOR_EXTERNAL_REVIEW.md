# V35 当前根因审查状态与外部 AI 交接说明

> 更新日期：2026-08-26  
> 用途：供其他 AI、研究协作者或审阅者快速了解当前做到哪一步、正在查什么根因、哪些结论已经成立、哪些尚未成立。  
> 本文是状态说明，不是新的实验结果，不替代各工作包原始证据与 SHA-256 清单。

## 1. 一句话结论

当前既没有获准的 V35 Final 算法，也没有证据支持立即修改 PDDR。正在调查的核心问题是：

> 为什么包含 CFVF、个人档案/Qp/双 Q 和 CA-TA-Lite 的完整 A4，在部分 100-job 实例的中后期反而落后于较简单的 A2；这种退化是否来自 FC-5 曾发现的“优秀候选被发现但没有在工作种群—教师—后代链路中持续利用”。

50k 与 100k 诊断尚未确认该机制。250k 根因实验尚未启动。目前只差修正诊断工具的 phase-consistent 终止快照协议，并用一对 A4 50k ON/OFF 运行完成最终工具验收。

## 2. 当前正式语义冻结

以下语义均未因本次根因审查改变：

```text
Decoder                    = FM3
ShiftMode                  = NONE
familyMode                 = DEGENERATE_SINGLE_FAMILY
setupMode                  = SEQUENCE_INDEPENDENT
search mixture             = [G1,G4,G2,G3]=[20,40,20,20]
PDDR                       = GLOBAL_ORIGINAL
local-search order         = CA-TA-Lite -> inherited LS
dual-Q schedule            = P=5 / G=5, rho=0
directional teacher pool   = OFF
active archive             = UNBOUNDED_FULL
formal matrix              = PAUSED
```

正式冻结 Jar SHA-256 仍为：

```text
8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
```

本轮遥测使用独立诊断 Jar，不覆盖正式冻结 Jar。

## 3. A0、A2、A4 分别是什么

本项目使用渐进消融臂。与当前问题直接相关的是：

| 臂 | 含义 | 当前用途 |
|---|---|---|
| `A0` | 规范 HMOPSO-QGS 公平基线 | 判断改进链是否整体优于规范基线 |
| `A2` | 在前置改进基础上启用 CFVF 的中间候选臂，不含 A4 后续完整机制 | A4 的直接简化对照 |
| `A4` | 完整候选链，包含 CFVF、个人档案/Qp、双 Q 与 budget-aware CA-TA-Lite | 原完整主候选，但已在多实例确认中被否决晋升 |

历史臂必须保持各自冻结定义。不能为了本次审查给 A0/A2 人为补入 Qp、双 Q 或 CA-TA，也不能关闭 A4 中已经存在的创新机制。

## 4. 为什么开始查根因

### 4.1 A2 对 A4 多实例确认

已完成：

```text
6 instances x 5 seeds x 2 arms = 60 runs at MaxFEs=500000
```

结果为：

```text
A4_NOT_PROMOTED
```

主要原因是 A4 在 100-job 汇总 HV/IGD 门及 `100_5_3_1` 单实例否决门失败。这个结果说明 A4 的完整创新链没有在未参与开发的多实例确认集上稳定成立。

### 4.2 A0 对 A2 多实例确认

随后又完成独立的 A0/A2 60 条配对确认。结果为：

```text
A2_NOT_PROMOTED
```

总体配对中位 `Delta Cmax=-0.7410%`，且 `100_8_3_1` 触发 100-job HV/IGD 否决门。因此当前既不能把 A4 冻结为 Final，也不能自动把 A2 政名为 Final。

### 4.3 当前真正的问题

当前不是为了删除 CFVF、双 Q 或 CA-TA-Lite，而是要解释：

```text
为什么这些机制在部分小/中规模或单实例上有改善信号，
但在某些100-job实例上会在中后期失去稳定优势？
```

## 5. FC-5 与 FC-6 已经做过什么

### 5.1 FC-5 的已知信号

FC-5 曾发现一种“利用断裂”现象：

```text
优秀候选已经生成
-> 进入 Merge Pool 或被档案观察
-> 未在后续 working population 中得到充分持续利用
-> 教师曝光和有效后代链可能断裂
```

FC-5 的结论不是“PDDR 程序有 bug”，而是“候选发现与后续利用之间可能脱节”。

### 5.2 FC-6 已否决的修法

FC-6 已经试过并否决：

- `ORDER_SWAP`：反转局部搜索顺序，因 IGD 门失败未转正；
- `BP_RESERVED_LEGACY`：历史 BP 预留路径退出主线；
- `REGION_AWARE`：在 20-job 与 100-job 均未通过，100-job出现严重 HV/IGD 退化。

因此当前继续保持：

```text
GLOBAL_ORIGINAL PDDR
CA-TA-Lite -> inherited LS
```

FC-6 只能证明上述三种修法失败，不能证明 FC-5 的“利用断裂”机制不存在。

## 6. 当前根因假设

当前第一优先假设为：

```text
ND候选逐渐增加
-> PDDR将候选压缩到100个工作槽位
-> Cmax/TEC/TWC/Balanced关键代表在后续周期中未持续保留或利用
-> archive 与 working population 发生脱节
-> 教师曝光或有效后代不足
-> A4在100-job中后期退化
```

观测链固定为：

```text
候选生成
-> 进入 Merge Pool
-> PDDR 是否选中
-> 下一代是否存活
-> 是否成为 Qg/Qp 教师
-> 是否产生目标改善后代
```

这只是 root-cause candidate。只有未来“只修这一环后困难实例恢复、正常实例不受伤”的单变量实验通过，才能称为因果根因。

## 7. 50k 与 100k 已得到什么

### 7.1 50k 第一档

50k 诊断没有观察到候选池膨胀：

- PDDR轮中的 `Nnd` 均小于100；
- 四方向代表在PDDR即时 pool-to-next 环节基本保留；
- archive-working Cmax gap 基本为0；
- 历史100-job退化在50k Cmax轨迹中尚未出现。

由于每条50k运行只有一个50k窗口，无法满足“连续两个窗口”迁移判据，裁决为：

```text
INSUFFICIENT_EVIDENCE
```

### 7.2 100k 第二档

100k 六条配对运行均有效，未发现 `Nnd>100`：

```text
max Nnd = 76
H1a ND_OVERFLOW = NOT_CONFIRMED_AT_100K
```

纠正分母后，A4生命周期数据为：

```text
next -> nextCycle      = 99/128 = 77.34%
next -> teacher        = 117/138 = 84.78%
teacher -> improvement = 77/117 = 65.81%
```

另观察到6条局部失败/拒绝事件。但当前数据缺少足够的正例对照、精确改善发生时序和完整checkpoint前沿，无法证明这些局部失败造成最终性能退化。

因此当前裁决为：

```text
H1a = NOT_CONFIRMED_AT_100K
H1b = LOCAL_FAILURE_EVENTS_OBSERVED_BUT_TRANSFER_UNRESOLVED
FC5_TRANSFER = INCONCLUSIVE
PDDR = KEEP_GLOBAL_ORIGINAL
```

不能根据“候选数量增加”或少数局部失败直接修改PDDR。

## 8. 为什么又开发诊断工具

100k数据缺少以下关键字段：

- 中程三类前沿真实快照；
- PDDR完整候选来源和物理槽位；
- Qg/Qp教师选择、方向遗憾和后代回填；
- CA-TA候选从生成到下一代/教师/改善的完整生命周期；
- 可证明遥测未改变随机流和搜索行为的真实序列哈希。

如果直接运行12条250k，即使得到性能数据，也可能无法解释退化发生在哪一环。因此先建设纯观察遥测工具。

早期V1/V2曾出现空Observer、placeholder字段、错误分母、假哈希和错误物理槽位，均已隔离为：

```text
SUPERSEDED_NOT_ACCEPTANCE_EVIDENCE
```

## 9. V3诊断工具当前状态

最新独立诊断 Jar：

```text
723D24ED3021A01FACDA0231E3B142238E740FB18D025A4341748F2AF8D22E2F
```

已通过的部分：

- 真实 jMetal RNG 消费序列；
- pre-evaluation 的真实 JS/FA/MA/WA 候选序列；
- candidate count 与 decoderCalls 闭合；
- PDDR真实 parent、lineage、物理槽位和语义角色；
- Qg/Qp教师元数据及后代回填；
- CA-TA长程生命周期和right-censor字段；
- ON/OFF核心行为哈希一致；
- 20k/50k完整链路最大实测遥测开销13.82%；
- 最终Manifest 112个文件，逐文件反算SHA-256为0失败。

当前状态仍为：

```ini
diagnosticToolingValidated=false
250kReadyForPreregistration=false
250kStarted=false
formalMatrixRunning=false
FC5=INCONCLUSIVE
```

## 10. 唯一剩余工具阻断

A4请求50k时，按冻结的phase-consistent预算语义合法停止于：

```text
requestedMaxFE = 50000
actualFE        = 48269
remainingFE     = 1731
qPhaseFE        = 5000
partial Q phase = disabled
```

因为剩余1731 FE不足一个完整5000-FE Q phase，算法不能继续，也不能为了凑50000而执行partial phase。

但当前遥测验收要求必须观测名义50000 FE原子快照，因此出现：

```text
CHECKPOINT_NOT_REACHED
RUN_END_NO_ATOMIC_SNAPSHOT
unobservableCheckpointCount=1
```

这不是算法崩溃，也不是PDDR、双Q或CA-TA失败，而是“名义检查点协议”与“phase-consistent预算协议”冲突。

## 11. 已确定的下一步

只修终止快照协议：

```text
nominalCheckpointFE = 50000
actualCheckpointFE  = 48269
checkpointKind      = PHASE_CONSISTENT_TERMINAL
```

必须证明 `actualFE=lastCompletedAtomicBoundaryFE`，并在该真实终止点保存：

```text
workingPopulationND
decisionArchiveFront
observedFullFront
```

然后只重跑一对：

```text
100_5_3_1 / seed 20260901 / A4 / 50k / telemetry OFF+ON
```

不再重跑2k、20k、A2，也不运行250k。

只有这对运行通过行为等价和终止快照门，才允许设置：

```ini
diagnosticToolingValidated=true
250kReadyForPreregistration=true
```

这里的`ReadyForPreregistration`只表示可以撰写下一阶段预注册，不表示250k已经批准。

## 12. 后续可能的250k实验，但尚未批准

若工具最终通过，预期250k诊断设计为：

```text
instances = 100_2_4_1（正例）, 100_5_3_1（困难例）
seeds     = 20260901..20260903
arms      = A2, A4
budget    = MaxFEs 250000, phase-consistent termination
total     = 2 x 3 x 2 = 12 runs
```

250k的目的不是重新比较谁的最终性能更好，而是观察退化形成前后的中程链路：

```text
PDDR候选
-> 工作种群
-> 教师曝光
-> 改善后代
-> checkpoint HV/IGD/Cmax变化
```

250k是否启动仍需用户另行批准。

## 13. 当前禁止事项

在新证据出现前禁止：

- 修改PDDR或恢复BP/Region-aware；
- 删除或关闭CFVF、个人档案/Qp、双Q、CA-TA-Lite；
- 调整`20/40/20/20`、Pacing、奖励、rho或局部搜索顺序；
- 修改正式冻结Jar；
- 启动250k、500k补充实验或4500正式矩阵；
- 把50k/100k相关性写成最终因果根因；
- 把诊断工具通过写成A4算法晋升。

## 14. 希望外部AI重点评议的问题

1. 将phase-consistent真实终止点作为名义MaxFEs的terminal checkpoint，是否是正确且不改变算法语义的做法？
2. 当前50k/100k证据是否足以否定“早期ND overflow”，但仍不足以否定100k以后出现的利用断裂？
3. 如果250k观察到代表即时保留正常、但next-cycle/teacher/improvement链逐渐下降，最小单变量修复应作用在哪一层？
4. 如何在不引入crowding、reference vector或区域配额的前提下，验证working-population利用断裂？
5. 若250k仍不能确认FC-5迁移，下一优先级是否应依次为CFVF规模编辑、Qp/双Q协调、CA-TA/继承局部搜索预算，再到FM3景观？

## 15. 主要证据入口

项目根目录：

```text
E:\学习\李明哲-毕业材料\张博改进
```

主要文件：

- `docs/evidence/V35-A2-A4-MULTIINSTANCE-CONFIRMATION/`
- `docs/evidence/V35-A2-FINAL-CANDIDATE-CONFIRMATION/`
- `docs/evidence/V35-P26/FC6A4-local-search-order/`
- `docs/evidence/V35-P26/FC6B-region-aware/`
- `docs/evidence/V35-FC5-100JOB-TRANSFER/`
- `docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/20-final-decision/REAL_FINAL_DECISION.md`
- `docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/19-final-independent-verification/FINAL_INDEPENDENT_VERIFICATION.md`
- `docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/17-final-checkpoint-budget/FINAL_CHECKPOINT_BUDGET_ACCEPTANCE.md`
- `docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/20-final-decision/evidence-sha256.tsv`
- `docs/PAPER_EVIDENCE_MASTER/CURRENT_SCIENTIFIC_STATE.md`
- `docs/ROADMAP.md`

## 16. 当前最终状态

```ini
finalCandidateApproved=false
A4Promoted=false
A2Promoted=false
pddrCurrentDecision=KEEP_GLOBAL_ORIGINAL
FC5=INCONCLUSIVE
diagnosticToolingValidated=false
250kReadyForPreregistration=false
250kStarted=false
formalMatrixRunning=false
formalMatrixPaused=true
algorithmDecisionSemanticsChanged=false
formalFrozenJarChanged=false
```

任何外部建议都应以这些冻结边界为前提，不能通过重新抽实例、重调参数或删除创新机制绕过既有否决证据。
