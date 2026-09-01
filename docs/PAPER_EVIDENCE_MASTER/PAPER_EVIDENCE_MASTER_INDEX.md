# P1--Stage2 论文证据总索引

## 1. 来源、基线与公式

| 阶段 | 内容 | 主要证据 | 论文用途 | 资格 |
|---|---|---|---|---|
| P1 | 作者源码、资料和哈希冻结 | `docs/evidence/P1/` | 数据与实现来源 | `MAIN_METHOD_EVIDENCE` |
| P2 | 四向量编码、黄金夹具 | `docs/evidence/P2/` | 编码章节和示例 | `MAIN_METHOD_EVIDENCE` |
| P3 | 论文解码oracle | `docs/evidence/P3/` | 公开公式共同语义核验 | `MAIN_METHOD_EVIDENCE` |
| P4/P4.1 | 论文验证线与作者派生线隔离 | `docs/evidence/P4*` | 基线来源和缺陷边界 | `REPRODUCIBILITY_ONLY` |
| P5/P5.1 | 疲劳累积、恢复、工时反馈 | `docs/evidence/P5*` | 第一创新公式 | `MAIN_METHOD_EVIDENCE` |
| P6.0--P6.5 | CFVF、Qp、双Q、子群语义 | `docs/evidence/P6*` | 第二创新结构 | `MAIN_METHOD_EVIDENCE` |
| P7.1/P7.2 | 旧O1--O13与CA-TA | `docs/evidence/P7*` | 历史机制来源，不作当前主线结果 | `LEGACY_EXCLUDED` |

## 2. 解码示例与历史移位线

| 证据 | 位置 | 用途 | 当前边界 |
|---|---|---|---|
| I0-v35 | `paper_evidence/I0-v35/` | FM3独立公式与Java逐字段对照 | 当前方法证据 |
| I1 | `paper_evidence/I1/` | 第四章10工件黄金示例、编码、甘特和疲劳解释 | 工程黄金示例 |
| I0/I1 Shift | `paper_evidence/I0/`、I1的09--11目录 | 历史FCLS/FCRS研究 | `LEGACY_EXCLUDED` |
| P8.1--P8.3 | `docs/evidence/P8.1`、`P8.3` | 规范解码、CA-TA纠错和性能工程 | 方法/工程审计 |
| P8.4--P8.6 | `docs/evidence/P8.4`、`P8.6` | 左移右移历史证据 | 不进当前正式路径 |

## 3. v3.5机制实现与审计

| 阶段 | 内容 | 论文用途 | 资格 |
|---|---|---|---|
| V35-P0--P4 | 单族占位、序列无关SUT、FM3、Shift冻结 | 模型与公平边界 | `MAIN_METHOD_EVIDENCE` |
| V35-P5/P7 | 规范基线、DSCR、CFVF、Qp/Qg | 第二创新实现 | `MAIN_METHOD_EVIDENCE` |
| V35-P8 | CA-TA-Lite五宏邻域 | 第三创新实现 | `MAIN_METHOD_EVIDENCE` |
| V35-P9--P19 | 公平Runner、教师、宏邻域、Cmax生命周期 | 机制与复现审计 | `REPRODUCIBILITY_ONLY` |
| V35-P21 | 六梯级消融先导 | 早期归因与预算修复 | `PILOT_DIAGNOSTIC` |
| V35-P22/P23 | 多实例pilot、精确前沿 | 正确性与尺度核验 | `REPRODUCIBILITY_ONLY` |
| V35-P24--P24.2 | 参数与最终冻结、dualQ gb15裁决 | 参数冻结过程 | `PAPER_PARAMETER_SELECTION` |

## 4. 算法比较、FC与DOE

| Campaign | 结论 | 论文资格 |
|---|---|---|
| P25A | 方向教师池未稳定优于A4 | `NEGATIVE_RESULT_APPENDIX` |
| P25B | 压力分类held-out门失败，保持BAL全开放 | `NEGATIVE_RESULT_APPENDIX` |
| P25D | 六算法被统一增强引擎污染 | `FORBIDDEN_FOR_PAPER_CLAIM` |
| P25E | 忠实适配的8算法50k先导 | `PILOT_DIAGNOSTIC` |
| FC-2 | Dynamic Local-FE Pacing转正 | `PAPER_PARAMETER_SELECTION` |
| FC-3 | Cheap-Test未转正 | `NEGATIVE_RESULT_APPENDIX` |
| FC-4 | Soft-freeze rho>0失败，保持rho=0 | `NEGATIVE_RESULT_APPENDIX` |
| FC-6A.4 | ORDER_SWAP因IGD门失败 | `NEGATIVE_RESULT_APPENDIX` |
| FC-6B | REGION_AWARE尤其在100-job失败 | `NEGATIVE_RESULT_APPENDIX` |
| DOE1 | 15点mixture开发+held-out确认 | `PAPER_PARAMETER_SELECTION` |
| DOE1最终 | T1/T2/T3均未过门，冻结20/40/20/20 | `PAPER_PARAMETER_SELECTION` |

## 5. Final与Stage2

| 证据 | 内容 | 当前状态 |
|---|---|---|
| `V35-FINAL-FREEZE` | Final源码、Jar、配置冻结 | 已完成 |
| `V35-FORMAL-MANIFEST` | 45实例、20 seeds、900共享初群 | 已完成 |
| `V35-PHASE-BUDGET-PROTOCOL` | phase-consistent MaxFEs协议 | 已完成 |
| `V35-REMOTE-BENCHMARK` | 4/8/12/16 JVM吞吐 | 已完成，16并行可用 |
| `V35-STAGE2-MASTER-V2` | 五臂Master执行链 | 已验收，但正式campaign已暂停 |
| `V35-STAGE2-PILOT-A0-A4-20260823` | 12组60条配对先导 | 当前最新机制证据 |

## 6. 当前下一步

只允许先做：

1. 使用已有日志完成A3与PDDR候选生命周期纯观察审计；
2. 若日志不足，再预注册小型、单变量、低预算实验；
3. 审计结论明确前，不恢复4500矩阵，不修改冻结Jar。

## 9. 非支配档案与论文代表前沿（2026-08-24）

- 当前正式搜索继续使用无界完整`decision-front`，PDDR仍为`GLOBAL_ORIGINAL`；两者均未改变。
- `observed-full-front`是纯观察候选发现前沿，只用于候选生命周期核对。
- K30只用于论文绘图和甘特图选例；K25/K50只用于等基数敏感性，均被代码级前沿类型表禁止进入主指标。
- ND1/ND2教师视图与ND3/ND4有界活动档案已经实现为休眠实验臂，但没有上传训练机、没有实验结果、没有升级为正式机制。
- 唯一证据入口为`docs/evidence/V35-ND-ARCHIVE/`；完整协议和升级门见
  `docs/V35_ND_ARCHIVE_PROTOCOL.md`与`docs/V35_ND_ARCHIVE_AND_CARDINALITY_ROADMAP.md`。
