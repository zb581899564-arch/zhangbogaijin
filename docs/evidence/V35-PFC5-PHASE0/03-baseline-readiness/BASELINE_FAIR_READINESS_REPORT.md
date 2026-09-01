# PFC5-1D 外部基线 Fair-Ready 审计报告

- 生成时间：2026-08-29（UTC 10:57）
- 生成工具：`docs/evidence/V35-PFC5-PHASE0/tools/build_baseline_readiness.py`
  （所有 sourceSHA256 由脚本对当前源文件实测）
- 输出：`baseline-fair-readiness.csv`、`readiness-summary.json`
- 消耗FE：0；改变算法：否
- 判定纪律：`fairReady=true` 必须有实现 + 预算/解码器 + 运行证据三重支撑；
  仅为类名存在不计数；禁止用 P25D 增强比较器结果或连续 OMOPSO/SMPSO 冒充。

## 结论一览

| 算法 | fairReady | 状态 | 关键依据 / 阻断 |
|---|---|---|---|
| HMOPSO-QGS-F | **true** | 可用于 Gap Probe / Validation | 正式 fair-adaptation runner（`ZhangBoV35FormalComparisonRunner`，A0_HMOPSO_QGS_F_FAIR_ADAPTATION）；P8.3 性能门 20k/100k + P25D 50k 试点 ×5 seed 全 COMPLETED；试点 mechanismSummary 证实 V35 机制全关（qpActions=0、cfvfOffspring=0、caTaLiteTest=0、dscr=disabled、dualQ=0）；FM3/NONE/单家族/序列无关设置时间与冻结语义一致；确定性重放（deterministicReplay）尚未做过，不阻断 Gap Probe，Validation 前建议补 |
| NSGA-II-F | pending_gate | 实现与证据在，须先脱钩 P25D 引擎 | 官方 jMetal 5.8 核（`OfficialJMetal58NSGAII`）+ 50k 试点 ×5 seed；但当前经 `V35P25DComparativeEngine` 驱动，AGENTS 规定该引擎只作历史工程诊断、不得进论文 reference；须迁移到白名单 runner 并登记上游 jMetal 提交/许可证/差异 |
| SPEA2-F | pending_gate | 同上 | 同 NSGA-II-F（`OfficialJMetal58SPEA2`） |
| HMOPSO-QLS-F | false | PENDING_SOURCE_VERIFICATION | 算法体嵌在被禁用的 P25D 引擎内（枚举臂），无独立可审实现；对李论文第三章 QLS 语义的忠实性未验证 |
| MOEA/D-F | false | NOT_READY | 仅继承自第三章的 `MOEADRun`/`DHFSP_MOEAD` 遗留问题类；未接 FM3 共享解码器、无公平预算适配、未进 P25D 试点 roster |
| QMOEA | false | NOT_READY | 两个 Java 工程内零实现；ROADMAP/P9 计划登记的来源缺口（论文主对比硬门）；不在主计划 §33 Validation 算法清单内 |

## 说明

1. 当前唯一 `fairReady=true` 的外部基线是 **HMOPSO-QGS-F**——这足以支撑主计划 §31
   Gap Probe 的"1 strongest FAIR_READY external"占位及 §33 的基线对照需求；
   但 §33 Validation 要求 NSGA-II-F/SPEA2-F/(HMOPSO-QLS-F 或 MOEA/D-F) 全部
   fairReady=true，因此 **PFC5-VAL 之前必须完成 P25D 引擎脱钩与来源验证**，
   该工作属于后续工作包，不在本阶段授权内。
2. 所有"true (shared decoder)"类断言中，FM3/设置时间语义以共享问题路径为前提；
   每个算法在进入 Validation 前，仍须按 AGENTS §2（"共享问题、隔离搜索机制"）
   逐项复核 problemAdapter/objectiveMapping/随机源三列。
3. 本审计不修改任何算法源码；`V35P25DComparativeEngine` 的引用现状
   （ZhangBoV35P25DRunner、P25E corrected runner 及其测试）仅登记，不清理。
