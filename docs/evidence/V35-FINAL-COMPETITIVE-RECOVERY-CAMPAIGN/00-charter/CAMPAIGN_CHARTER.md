# CAMPAIGN_CHARTER — V35 Final Competitive Recovery and Paper Campaign

- 日期：2026-08-31
- 性质：**收尾战役总章程**（治理路线图）。各阶段执行时仍需各自预登记（沿用既有工作包纪律）；本章程冻结路线、预算上限与停止条件。
- 目标：用一次完整漏斗完成 `定位生成侧问题 → 最多一个修复族 → 参数迁移确认 → Final候选裁决 → 正式消融与外部比较 → FM3模型实验 → 统计与论文`。
- 核心原则：**最多再允许一个新修复族**。若仍不能通过 250k/500k 门，停止调算法，冻结当前版本并诚实收缩论文结论，避免无限试错。

## 0. 起点（冻结事实）

```ini
PDDR=GLOBAL_ORIGINAL
mixture=20/40/20/20
betaMax=0.65
localFePacingRepairFamily=PILOT_REJECTED
paretoCoverageAudit=NO_ACTIONABLE_LEVER
FinalCandidateApproved=false
formalMatrixRunning=false
formalJarSha256=8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9
```

已排除路线（不得重复）：ORDER_SWAP、BP_RESERVED_LEGACY、REGION_AWARE、Teacher lambda、新 betaMax、压力掩码、Shift、直接裁剪非支配档案、无证据修改 PDDR。

当前唯一值得继续调查的方向：**生成侧候选质量/多样性不足**（尚不能判断来自 CFVF、Qp、CA-TA 还是复合耦合）。

## 1. 阶段路线图

| 阶段 | 内容 | 产出/门 |
|---|---|---|
| P1 | V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1：纯观察来源账本 → OFF/ON等价门 → 渐进诊断运行（6×100k，条件性+6×250k）→ 来源贡献裁决 | 诊断裁决六选一（ROOT_CANDIDATE性质） |
| P2 | 唯一修复族选择（依据P1裁决，用户复核后定方向）：CFVF→编辑幅度/变异触发/接受规则三选一；Qp→个人领导选择或Qp奖励二选一（复用A3-D证据）；CA-TA→调用治理（何时调用）；复合/无杠杆→停止修复 | 修复族预登记 |
| P3 | 修复实验漏斗：Gate1 20k工程门（control+repair×2实例×1seed）→ Gate2 50k筛查（≤24条，剂量预冻）→ Gate3 250k确认（≤18条，新seed）→ Gate4 500k多实例确认（60条=control+winner×6实例×5seed；20/50/100-job×2；100-job困难实例否决门）。500k失败 ⇒ repairRejected=true、stopAlgorithmOptimization=true，不再救第二次 | 逐门验收 |
| P4 | DOE迁移门：四点迁移检查（20/40/20/20、30/50/10/10、25/25/25/25、20/40/30/10）；仅当最优配比改变且改善>2%、或交互>2pp、或排序反转、或HV/IGD灾难门，才重做15-treatment DOE；否则沿用20/40/20/20。Pacing DOE不自动启动 | 迁移检查报告 |
| P5 | Final候选裁决：A2（CFVF主干）/A4（当前完整）/A4R（确认修复版）×6新实例×5新seed×500k。A4R晋级六条件（整体HV/IGD优于A2与A4；100-job无结构性退化；三目标无系统性崩塌；≥4/6实例获胜或实质持平；不依赖单seed或单困难实例；三项创新真实触发）。A4R失败→在A2与A4中选更稳健者，双Q与CA-TA仍可作研究机制讨论但不得虚构独立正贡献 | Final候选 |
| P6 | Final Freeze一次性冻结：source commit/tag、Jar SHA、canonical configuration、算法语义版本、FM3参数、PDDR、mixture、beta/pacing、实例清单、20正式seed、初群生成规则、输出schema、预算协议。此后任何结果难看都不能改算法 | freeze manifest |
| P7 | Production Preflight：20/50/100-job短程验证；1-2条完整500k长运行；消融臂公平审计；失败重启/原子输出/磁盘与日志压缩测试；4/8/12/16 JVM吞吐测试。调度器在训练机本地，SSH只启动与验收 | preflight报告 |
| P8 | 正式消融：结构按最终算法重新冻结（A0=HMOPSO-QGS-F基线，A1=+DSCR，A2=+CFVF，A3=+Qp/Dual-Q，A4=+CA-TA/Pacing；若有A4R且非CA-TA内部参数则加A5，否则吸收进A4）。先9实例×5seed确认臂定义合理，再批准45×20正式矩阵 | 消融先导→正式矩阵 |
| P9 | 外部算法比较：保底HMOPSO-QGS-F/NSGA-II-F/SPEA2-F，再从忠实适配且来源可信者中选2-3（HMOPSO-QLS-F/MOPSO-F/MOPSODS-DE-F/MOHEADE-F）。共享四向量初群/FM3/目标[0,1,6]/500k/20seed/统一实例/reference，不共享搜索机制。QMOEA维持PENDING_SOURCE_VERIFICATION | 外部比较 |
| P10 | FM3模型独立实验：FM0/FM1/FM2/FM3固定算法对比（Cmax/TEC/TWC/Fmax/Favg/疲劳暴露/恢复时间/加工时间膨胀），代表性20/50/100-job实例+独立协议与reference；λ/μ/r/Fwarn有限参数敏感性（少量预注册水平，禁止当算法调优） | 模型实验 |
| P11 | 统一指标与统计：PFref(instance)=ND(所有正式算法×所有正式run)；统一重算HV/IGD/IGD+/Spacing/C双向/极值/frontSize/runtime/FE利用率；统计以实例为主单位（每实例先汇总20 seed；两算法paired Wilcoxon；多算法Friedman+Holm；报告效应量与相对改善；禁止把20 seed当20个独立问题） | 统计包 |
| P12 | 论文：现在即可写 Problem formulation、FM3公式、四向量编码解码、DSCR/CFVF/Dual-Q/CA-TA结构、预算协议、可重复性与证据治理、失败路线与参数冻结方法。Results/统计表/最终结论等正式数据 | 论文稿 |

## 2. 全局停止条件（冻结）

```text
最多再允许1个修复族
最多1轮50k筛查
最多1轮250k确认
最多1轮500k确认
```

若仍失败：

```ini
algorithmOptimizationClosed=true
finalVersion=C0或A2/A4中最稳健者
```

然后直接进入正式对比、模型实验和论文写作。

## 3. Paper 禁写清单（至正式数据齐备）

不得写：①"显著优于所有算法"；②"100-job稳定领先"；③"Qp独立带来正贡献"；④"PDDR是根因"；⑤"Final算法已冻结"。

## 4. 批准粒度

P1 执行完毕（诊断裁决落盘）后**停止等用户复核**，再定 P2 修复族方向；P3 漏斗各门（20k/50k/250k/500k）逐门验收，500k 启动前需用户对 Gate3 结果复核。
