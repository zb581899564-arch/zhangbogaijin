# V35-P9 规范公平 Runner 自验收报告

日期：2026-08-12

## 结论

V35 规范生产链路通过本轮工程自验收，可以进行当前语义下的诊断性对照。该结论不等同于正式论文复现、统计显著性或算法优越性结论。

## 本轮发现并修复的问题

复跑同一实例、同一 seed、同一初始种群时，规范基线结果曾出现变化。根因是正式路径的共享 `select()` 仍调用全局 `JMetalRandom`，绕过了运行专属 seed 流。现已增加 formal-only `formalRandomInt()`：正式结构化基线路径改用 `zhangBoP6Random`，作者诊断路径仍保留原行为。

同时移除了 `prepareOriginalQg()` 中重复刷新 DSCR 教师缓存的逻辑，避免一次社会领导准备产生两次教师 exposure/替换统计。

## 验收证据

- `jmetal-problem`：67 tests，0 failures/errors。
- `jmetal-algorithm` V35/历史定向回归：95 tests，0 failures/errors。
- `jmetal-exec`：Maven Java 8 目标打包通过。
- 同 seed 基线重放：2 次，初始种群哈希、FE、最终前沿文本一致。
- 同 seed FULL 重放：2 次，初始种群哈希、FE、最终前沿文本一致。
- Shift：正式配置固定 `ShiftMode=NONE`。
- 设置语义：`DEGENERATE_SINGLE_FAMILY` + `SEQUENCE_INDEPENDENT`。
- 解码：`FM3`，正式目标适配 `[0,1,6]`。
- 20k smoke：baseline/FULL 均 `COMPLETED`，各 `FE=20000`，初始种群哈希一致，前沿非空。

## 20k 诊断性对照

实例 `20_2_3_1`，seed `20260808`，population `100`，预算 `20000 FE`。完整原始记录见 [V35_FAIR_COMPARISON_20K_20260808.txt](./V35_FAIR_COMPARISON_20K_20260808.txt)。本轮正式验收只采用状态、FE、共同初始种群哈希和机制事件作为门槛；前沿数值不作优越性结论，待统一 reference 和多 seed 方案批准后再汇总。

FULL 真实触发了 CFVF、Qg/Qp、谱系档案、DSCR、CA-TA-Lite Test/Apply；基线真实触发了结构化 Qg、PDDR、固定邻域局部评价。两者均无预算超限，且共享初始种群哈希。

## 100k 扩大诊断

在同一实例、同一 seed 和同一初始种群下又完成了 100k FE 诊断，原始记录见 [V35_FAIR_COMPARISON_100K_20260808.txt](./V35_FAIR_COMPARISON_100K_20260808.txt)。基线实际完成 100,000 FE，FULL 因完整外层代预算边界安全停止于 99,954 FE，未超预算。

| 配置 | 前沿数量 | Cmax 最小值 | TEC 最小值 | TWC 最小值 |
|---|---:|---:|---:|---:|
| V35_BASELINE | 61 | 196.8391 | 8909.7485 | 12955.8829 |
| V35_FULL | 236 | 193.2870 | 8580.5671 | 12394.3358 |

该单实例单 seed 结果中，FULL 的三个单目标极值分别改善约 1.80%、3.69% 和 4.33%；这只是扩大后的诊断信号，仍不等于多 seed 统计结论。

本次已将算法及 Decoder 分阶段记录接入 V35 Runner：100k 记录包含 `algorithmRunNanos`、成功 Decoder 调用数、基础解码耗时、左右移耗时、Decoder 总耗时、内部重传播数，以及 Qg/Qp、CFVF、档案、DSCR、CA-TA-Lite 事件摘要。当前正式 `ShiftMode=NONE`，因此左移/右移耗时和重传播数均为 0，这是配置结果，不是漏记。

## 保留边界

`A0_AUTHOR_DIAGNOSTIC`、历史 Shift 结果、P8/P9 旧实验和作者原始缺陷路径不进入当前参考前沿。仍保持 `pf_sdst_active_experiment=false`、`formal_matrix_started=false`、`sampled_reproduction_accepted=false`、`full_reproduction_accepted=false`。后续扩大多 seed、500000 FE 或正式矩阵必须另行批准。
