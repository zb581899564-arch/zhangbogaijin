# V35 证据收口与 A2→A3 因果诊断最终裁决

日期：2026-08-24

## 最终状态

```text
pddr_current_decision = KEEP_GLOBAL_ORIGINAL
archive_cardinality_artifact = NOT_CONFIRMED
archive_gate_a = BLOCKED
a2_a3_root_cause = COMPOSITE_BLOCK_UNRESOLVED
a2_a3_strongest_fault_candidate = QP_SELECTION_OR_REWARD_NUMERICAL_INSTABILITY
nd1_nd4_started = false
pddr_experiment_started = false
formal_matrix_running = false
```

当前正式语义保持不变：

```text
PDDR = GLOBAL_ORIGINAL
archive = UNBOUNDED_FULL
mixture = 20/40/20/20
LS order = CA-TA-Lite → inherited LS
```

## 1. 输入证据完整性

从冷归档恢复12个完整五臂公平组，共60条Stage2配对运行。归档SHA-256为
`0202356F28C7013894FB14B7347EB77A66243AD9312139CD0FE2A62F24CAD5FB`；共复核6769个文件、
437543395字节。60条目标run完整，816个运行内证据清单条目全部反向通过。8条非配对完成和7条
partial尝试继续排除。

## 2. 前沿基数问题

完整decision-front中精确重复和被支配点均为0。归一化0.1%近重复率中位数仅0.2584%，最大
4.5714%，远低于20%门槛。固定K25/K50后A4相对A0的HV/IGD方向不反转；65种leave-one-run/
leave-one-arm reference也无反转。因此“A4优势只是因为输出点多或reference被自身污染”的解释
没有得到支持，裁决为 `archive_cardinality_artifact=NOT_CONFIRMED`。

完整前沿仍是主科学数据；K30只供绘图，K25/K50只作敏感性，不进入主指标。

## 3. ND0 Gate A

I1一条5k和20-job三条20k运行均完成，审计ON/OFF在2k回归中保持初群、评价轨迹、前沿、Q表、
事件流和FE一致。但四条运行均出现：

```text
exactDedup(decision-front) != exactDedup(observed-full-front)
```

原因是算法decision archive只接收进入既定生命周期的候选，而只读observer看见每个已评价候选。
这不是重复点污染，也不能通过静默切换论文指标来源来“修复”。三个20-job的档案+教师扫描时间仅
4.4%--4.9%，但教师方向遗憾中位数15.4%--22.3%、P95均超过100%。因最强等价门失败，Gate A
裁决为 `BLOCKED`；教师视图只能保留为将来候选，本轮不运行ND1--ND4。

## 4. PDDR收口

P6.1.1、FC5、FC6与Stage2共同证明：工程选择器已核验；`GLOBAL_ORIGINAL`确有偏向综合解并可能
放弃方向极值的结构性取舍，但BP与`REGION_AWARE`均在当前语义下造成质量退化，100-job尤其
严重。因此保持 `KEEP_GLOBAL_ORIGINAL`。Stage2候选级字段不足标记为
`EVIDENCE_FIELD_LIMITATION`，但不得再误写为“PDDR从未审计”。本轮没有启动新PDDR实验。

## 5. A2→A3

原Stage2 12-seed先导已经显示A3组合块非单调。补充的六条20-job/50k诊断全部公平闭合，A3相对
A2的三seed中位变化为：HV -9.51%，IGD改善 -73.74%，Cmax改善 -0.78%。个人档案没有容量截断；
双Q阶段计数稳定为49/26/25；预算、provenance、初群和PDDR一致。

Qp事件与源码直接显示方向奖励使用接近零的 `oldPhi` 作相对分母，产生最大约 `-3.33e10` 的极端
方向奖励，是确定存在的数值缺陷候选。但A3同时加入个人档案、Qp与block-frozen双Q，现有两臂
无法独立识别各自的性能贡献。因此最终根因必须保持 `COMPOSITE_BLOCK_UNRESOLVED`，并把
`QP_SELECTION_OR_REWARD_NUMERICAL_INSTABILITY`登记为最强、但尚未完成因果隔离的故障候选。

## 6. 下一步边界

当前不应恢复4500矩阵，也不应改档案或PDDR。若继续，只允许先设计Qp奖励尺度稳定化的最小
单变量实验；该计划必须重新冻结语义、证明观察/修复边界，并经用户批准后执行。没有批准前，
冻结Jar、DOE参数、PDDR、档案和正式矩阵状态均保持不变。

## 证据入口

- PDDR：`docs/evidence/V35-P26/PDDR_EVIDENCE_RESOLUTION.md`
- 离线基数：`docs/evidence/V35-ND-ARCHIVE/01-offline-cardinality-audit/audit-summary.md`
- ND0 Gate A：`docs/evidence/V35-ND-ARCHIVE/02-observation-tooling/GATE_A_REPORT.md`
- A2/A3归档审计：`docs/evidence/V35-A2-A3-CAUSAL-AUDIT/generated/A2_A3_CAUSAL_AUDIT_REPORT.md`
- A2/A3补充诊断：`docs/evidence/V35-A2-A3-CAUSAL-AUDIT/local-50k-fixed20/A2_A3_FIXED20_50K_DIAGNOSTIC_REPORT.md`
- 构建与证据验收：`docs/evidence/V35-ND-ARCHIVE/06-final-decision/BUILD_AND_EVIDENCE_VALIDATION.md`
