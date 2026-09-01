# V35-EXTERNAL-BASELINE-FAIR-READY 治理记录

- 日期：2026-08-30
- 工作包：V35-PFC5 阶段B（外部基线 Fair-Ready 收口）
- 上游裁决链：阶段A 同日完成 `PFC5-CAL=REPAIR_FAMILY_NOT_PURSUED_STRUCTURAL_NO_LEVERAGE`
  （`V35-PFC5-TEACHER-EXPOSURE-CAL-PREREG/07-closure/`）

## 范围与授权

本工作包只解决一个问题：NSGA-II-F 与 SPEA2-F 能否以可信的官方原始搜索核心，
在相同 V35 共享层上成为公平外部基线。允许动作：源码审计、最小适配、单元测试、
2k 工程贯通。禁止动作全部未发生：无 50k/250k/500k 科学实验、无训练机上传、
无外部算法正式比较、无 Configuration Race、无 F2/F3、无新 Teacher repair、
PDDR 零改动、正式矩阵零恢复。

## 共享层与隔离层（本包冻结表述）

共享（全部算法一致）：instance、四向量 JS/FA/MA/WA、FM3 decoder、ShiftMode=NONE、
DEGENERATE_SINGLE_FAMILY、sequence-independent SUT、目标槽 [0,1,6]、显式初群、
FE=成功 decoder 调用数、预算终止、指标口径。

隔离（本包两算法独有，不经任何共享代码路径）：选择、交叉、变异、适应度/排序、
环境选择、档案逻辑全部留在官方 jMetal 5.8 核内；runner 侧只做纯委托计数包装。

## P25D 隔离声明

```ini
legacy_enhanced_comparator_rewrite=true
valid_for_paper_comparison=false
```

`V35P25DComparativeEngine` 在本包两个算法的任何执行路径中引用数为 0（静态扫描）；
P25D 历史结果不进入新 reference、新指标、fairReady 证据或论文效果结论，
仅作历史错误路线说明。

## 产物索引

| 目录 | 内容 |
|---|---|
| 00-governance | 本文件 |
| 01-upstream-source | 来源映射、GitHub 5.8 基准原文（双算法+15 依赖）、决定性 diff、diff 报告 |
| 02-adapter-audit | adapter 契约、禁止引用静态扫描（91 项 0 违规） |
| 03-implementation | 实现报告、独立命名比较构建物（585ca315 jar） |
| 04-unit-tests | 14/14 新增 + 2/2 回归 + problem 67/67 |
| 05-2k-smoke | 4 条 run 全工件（configuration/provenance/初群哈希/status/front/events/budget/logs/manifest）+ 记录与报告 |
| 06-readiness-decision | 十条必要条件裁决 + readiness.properties |
| tools | 静态扫描工具（注释/字符串剥离后匹配） |

## 构建与身份

- Java 8 target（major 52）不变；六模块 reactor 打包成功。
- 比较适配构建物独立名称：`external-fair-baseline-comparison-585ca315.jar`。
- 冻结三 Jar（formal 8dad8f40 / runtime 121fbb49 / base 723d24ed）事后重算不变。
- 本包 javaSourceChanged=true（engine 防线+重载、新 Runner、新测试），
  均限于比较命名空间；正式 V35 算法核心零改动。
